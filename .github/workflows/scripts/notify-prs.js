/*
If there are one or more tags associated with the current context, scans the commits since the last
corresponding tag to find PRs first included in the new tag, and adds a comment to each with a link.

This would nominally be run by a workflow triggered on release published, but as that doesn't work
if the release was created by a workflow using the default secrets.GITHUB_TOKEN, it can also be
run without release context, for example within a release workflow after tag creation.
 */
module.exports = async ({github, context, core}) => {
    const {owner, repo} = context.repo;

    function getLoader(tagName) {
        return tagName.substring(tagName.lastIndexOf('-') + 1).toLowerCase();
    }

    async function notifyTag(releaseName, allTags) {
        const releaseLoader = getLoader(releaseName);
        console.log(`Parsed loader: ${releaseLoader}`);

        // Find the most recent previous tag for the same loader
        const previousTag = allTags.find(t => {
            if (t.name === releaseName)
                return false;
            return getLoader(t.name) === releaseLoader;
        })

        let pulls;
        if (previousTag) {
            // Fetch up to 100 commits between the two tags
            console.log(`Found previous tag: ${previousTag.name}`);
            const res = await github.rest.repos.compareCommitsWithBasehead({
                owner,
                repo,
                basehead: `${previousTag.name}...${releaseName}`,
                per_page: 100,
            });
            commits = res.data.commits;
            console.log(`Found ${commits.length} commits`);

            for (const commit of commits) {
                const res = await github.rest.repos.listPullRequestsAssociatedWithCommit({
                    owner,
                    repo,
                    commit_sha: commit.sha,
                });
                pulls = res.data;
            }
            console.log(`Found ${pulls.length} associated PRs`);
        } else {
            console.log(`No previous tag found; fetching all PRs`)
            const res = await github.rest.pulls.list({
                owner,
                repo,
                state: 'closed',
                per_page: 100,
            });
            pulls = res.data;
            console.log(`Found ${pulls.length} total PRs`);
        }

        // Comment on each PR
        const encodedName = encodeURIComponent(releaseName);
        const url = `https://github.com/${owner}/${repo}/releases/tag/${encodedName}`;
        for (const pull of pulls) {
            await github.rest.issues.createComment({
                owner,
                repo,
                issue_number: pull.number,
                body: `This PR has been released in version [\`${releaseName}\`](${url})`
            });
            console.log(`Commented on PR #${pull.number}`);
        }
    }

    // Fetch all tags
    const allTags = await github.paginate(github.rest.repos.listTags, {
        owner,
        repo,
        per_page: 100,
    });
    console.log(`Found ${allTags.length} total tags`);

    if (context.payload && context.payload.release) {
        // Release context; use the release tag
        console.log(`Found release context: ${context.payload.release}`);
        await notifyTag(context.payload.release.tag_name, allTags);
    } else {
        // Other context; iterate all tags with matching SHA
        console.log(`No release context; searching tags`)
        for (const tag of allTags) {
            if (tag.commit.sha === process.env.GITHUB_SHA) {
                console.log(`Found tag with matching SHA: ${tag.name}`);
                await notifyTag(tag.name, allTags);
            }
        }
    }
}
