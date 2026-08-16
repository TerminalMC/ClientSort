/**
 * @typedef {import('@octokit/rest').Octokit} Octokit
 * @typedef {import('@actions/github').context} Context
 * @typedef {import('@actions/core')} Core
 * @typedef {import('@octokit/openapi-types').components['schemas']['release']} Release
 * @typedef {import('@octokit/openapi-types').components['schemas']['tag']} Tag
 * @typedef {import('@octokit/openapi-types').components['schemas']['pull-request']} PullRequest
 * @typedef {import('@octokit/openapi-types').components['schemas']['commit-comparison']} CommitComparison
 */

/**
 * If there is a tag associated with the current context, scans the commits since the previous tag
 * for the same loader, parses the commit messages to identify merged PRs, and adds a comment on
 * each PR with a link to the tag.
 *
 * If no previous tag could be found, assumes all closed PRs are fair game.
 *
 * This would nominally be run by a workflow triggered on release published, but as that doesn't
 * work if the release was created by a workflow using the default secrets.GITHUB_TOKEN, it can
 * also be run without release context, for example within a release workflow after tag creation.
 *
 * https://github.com/actions/github-script
 * https://octokit.github.io/rest.js
 * https://docs.github.com/en/rest
 *
 * @param {Object} args
 * @param {Octokit} args.github
 * @param {Context} args.context
 * @param {typeof import('@actions/core')} args.core
 */
module.exports = async ({github, context, core}) => {
    const {owner, repo} = context.repo;

    //
    // Functions
    //

    /**
     * @returns {Promise<Release[]>}
     */
    async function getAllReleases() {
        // https://docs.github.com/en/rest/releases/releases#list-releases
        return await github.paginate(github.rest.repos.listReleases, {
            owner,
            repo,
            per_page: 100,
        });
    }

    /**
     * @returns {Promise<Tag[]>}
     */
    async function getAllTags() {
        // https://docs.github.com/rest/repos/repos#list-repository-tags
        return await github.paginate(github.rest.repos.listTags, {
            owner,
            repo,
            per_page: 100,
        });
    }

    /**
     * @param {Tag[]} allTags
     * @param {string} loaderName
     * @param {string} releaseTagName
     * @returns {Promise<Tag | undefined>}
     */
    async function getPreviousTag(allTags, loaderName, releaseTagName) {
        return allTags.find(t => {
            if (t.name === releaseTagName)
                return false;
            return getLoaderName(t.name) === loaderName;
        });
    }

    /**
     * @param {Tag} previousTag
     * @param {string} releaseTagName
     * @returns {Promise<CommitComparison>}
     */
    async function getComparison(previousTag, releaseTagName) {
        // https://docs.github.com/en/rest/commits/commits#compare-two-commits
        return (await github.paginate(github.rest.repos.compareCommitsWithBasehead, {
            owner,
            repo,
            basehead: `${previousTag.name}...${releaseTagName}`,
            per_page: 100,
        }))[0];
    }

    /**
     * @param {CommitComparison} comparison
     * @returns {Set<number>}
     */
    function getPullNumbers(comparison) {
        const mergeRegex = /Merge pull request #(\d+)/i;
        /** @type {Set<number>} */
        const pullNumbers = new Set();

        for (const commit of comparison.commits) {
            const match = commit.commit.message.match(mergeRegex);
            if (match) {
                pullNumbers.add(Number(match[1]));
            }
        }

        return pullNumbers;
    }

    /**
     * @returns {Set<number>}
     */
    async function getAllClosedPullNumbers() {
        /** @type {Set<number>} */
        const pullNumbers = new Set();

        /** @type {PullRequest[]} */
        const pulls = await github.paginate(github.rest.pulls.list, {
            owner,
            repo,
            state: 'closed',
            per_page: 100,
        });
        for (const pull of pulls) {
            pullNumbers.add(pull.number);
        }

        return pullNumbers;
    }

    /**
     * @param {number} pullNumber
     * @param {Set<string>} releaseTagNames
     * @returns {Promise<void>}
     */
    async function commentOnPulls(pullNumber, releaseTagNames) {
        const plural = releaseTagNames.size > 1 ? 's' : '';
        let body = `This PR has been released in the following version${plural}:`;
        for (const releaseTagName of releaseTagNames) {
            const encodedName = encodeURIComponent(releaseTagName);
            const url = `https://github.com/${owner}/${repo}/releases/tag/${encodedName}`;
            body += `\n- [\`${releaseTagName}\`](${url})`;
        }

        // https://docs.github.com/rest/issues/comments#create-an-issue-comment
        await github.rest.issues.createComment({
            owner,
            repo,
            issue_number: pullNumber,
            body: body
        });
        console.log(`Commented on PR #${pullNumber}`);
    }

    /**
     * @param {string} tagName
     * @returns {string}
     */
    function getLoaderName(tagName) {
        // tag names follow the format `v<major>.<minor>.<patch>+<mc-version>-<loader>`
        return tagName.substring(tagName.lastIndexOf('-') + 1).toLowerCase();
    }

    //
    // Control flow
    //

    const scanMonths = Number(process.env.SCAN_MONTHS ?? `0`)
    if (!Number.isInteger(scanMonths)) {
        console.log(`Env SCAN_MONTHS (${process.env.SCAN_MONTHS}) is not an integer: aborting`);
        return;
    } else if (scanMonths < 1) {
        console.log(`Env SCAN_MONTHS (${scanMonths}) is less than one: aborting)`)
        return;
    }

    const allReleases = await getAllReleases();
    console.log(`Found ${allReleases.length} total releases`);
    if (allReleases.length > 0) {
        console.log(`Newest release tag is ${allReleases[0].tag_name}`);
    }

    const allTags = await getAllTags();
    console.log(`Found ${allTags.length} total tags`);

    const cutoff = new Date(new Date().setMonth(new Date().getMonth() - scanMonths));

    const recentReleases = new Map(allReleases
            .filter((release) => release.published_at && new Date(release.published_at).getTime() >= cutoff)
            .map((release) => [release.tag_name, release])
    );
    console.log(`Filtered ${recentReleases.size} recent releases from the last ${scanMonths} months`);
    if (recentReleases.length > 0) {
        console.log(`Oldest recent release tag is ${recentReleases[recentReleases.length - 1].tag_name}`);
    }

    let recentTags = allTags
            .filter((tag) => recentReleases.has(tag.name))
            .sort((a, b) => {
                // newest first
                const releaseA = recentReleases.get(a.name);
                const releaseB = recentReleases.get(b.name);
                return (new Date(releaseB.published_at).getTime() - new Date(releaseA.published_at).getTime());
            });
    console.log(`Filtered ${recentTags.length} recent tags`);
    if (recentTags.length > 0) {
        console.log(`Newest recent tag is ${recentTags[0].name}`);
        console.log(`Oldest recent tag is ${recentTags[recentTags.length - 1].name}`);
    }

    const releaseTagNames = [];
    if (context.payload && context.payload.release) {
        // release context; use the release tag
        console.log(`Found release context: ${context.payload.release}`);
        releaseTagNames.push(context.payload.release.tag_name);
    } else {
        // other context; iterate all tags with matching SHA
        console.log(`No release context; searching tags`)
        for (const tag of recentTags) {
            if (tag.commit.sha === process.env.GITHUB_SHA) {
                console.log(`Found tag with matching SHA: ${tag.name}`);
                releaseTagNames.push(tag.name);
            }
        }
    }
    recentTags = recentTags.filter((tag) => !releaseTagNames.includes(tag.name));
    console.log(`Refined ${recentTags.length} recent tags`);
    if (recentTags.length > 0) {
        console.log(`Newest recent tag is ${recentTags[0].name}`);
        console.log(`Oldest recent tag is ${recentTags[recentTags.length - 1].name}`);
    }

    if (releaseTagNames.length === 0) {
        console.error(`Failed to find a release tag name`);
        return;
    }

    /** @type {Map<number,Set<string>>} */
    const pullTags = new Map();

    for (const releaseTagName of releaseTagNames) {
        console.log(`Found release tag name: ${releaseTagName}`);

        const loaderName = getLoaderName(releaseTagName);
        console.log(`Extracted loader name: ${loaderName}`);

        /** @type {Set<number>} */
        let pullNumbers;

        const previousTag = await getPreviousTag(recentTags, loaderName, releaseTagName);
        if (previousTag) {
            console.log(`Found previous tag: ${previousTag.name}`);

            const comparison = await getComparison(previousTag, releaseTagName);
            console.log(`Found ${comparison.commits.length} commits between tag ${previousTag.name} and ${releaseTagName}`);

            pullNumbers = getPullNumbers(comparison);
            console.log(`Found ${pullNumbers.size} PRs from merge commit messages: ${[...pullNumbers]}`);
        } else {
            console.log(`Could not find a previous tag for loader ${loaderName} and release tag name ${releaseTagName}`);

            pullNumbers = await getAllClosedPullNumbers();
            console.log(`Found ${pullNumbers.size} closed PRs: ${[...pullNumbers]}`);
        }

        for (const pullNumber of pullNumbers) {
            let tags = pullTags.get(pullNumber);
            if (!tags) {
                tags = new Set();
                pullTags.set(pullNumber, tags);
            }
            tags.add(releaseTagName)
        }
    }

    for (const [pullNumber, tagNames] of pullTags) {
        await commentOnPulls(pullNumber, tagNames)
    }

    console.log(`Finished`);
}
