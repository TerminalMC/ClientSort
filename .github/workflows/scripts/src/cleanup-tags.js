/**
 * @typedef {import('@octokit/rest').Octokit} Octokit
 * @typedef {import('@actions/github').context} Context
 * @typedef {import('@octokit/openapi-types').components['schemas']['release']} Release
 */

/**
 * If there are no other releases associated with the tag of the deleted release, deletes the tag.
 *
 * This is intended to be run by a workflow triggered on release deleted.
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
        // https://docs.github.com/rest/releases/releases#list-releases
        return await github.paginate(github.rest.repos.listReleases, {
            owner,
            repo,
            per_page: 100,
        });
    }

    /**
     * @param {string} tagName
     * @returns {Promise<void>}
     */
    async function deleteTag(tagName) {
        // https://docs.github.com/rest/git/refs#delete-a-reference
        await github.rest.git.deleteRef({
            owner,
            repo,
            ref: `tags/${tagName}`,
        });
    }

    //
    // Control flow
    //

    if (!context.payload || !context.payload.release) {
        console.error("Failed to find a release context");
        return;
    }

    const releaseTagName = context.payload.release.tag_name;

    console.log(`Checking remaining releases for tag: ${releaseTagName}`);

    const releases = await getAllReleases();

    const existingRelease = releases.find(r => r.tag_name === releaseTagName);
    if (existingRelease) {
        console.log(`Tag is still used by release: ${existingRelease.name}`);
        return;
    }

    console.log(`No releases found. Deleting tag...`);
    try {
        await deleteTag(releaseTagName);
        console.log(`Deleted tag: ${releaseTagName}`);
    } catch (err) {
        console.log(`Failed to delete tag: ${err.message}`);
        core.setFailed(err.message);
    }
}
