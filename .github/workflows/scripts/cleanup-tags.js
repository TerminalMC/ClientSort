/*
If there are no other releases associated with the tag of the deleted release, deletes the tag.

This is intended to be run by a workflow triggered on release deleted.
 */
module.exports = async ({github, context, core}) => {
    const {owner, repo} = context.repo;

    if (!context.payload || !context.payload.release) {
        console.warn("Release context not found");
        return;
    }

    const releaseTag = context.payload.release.tag_name;

    console.log(`Checking remaining releases for tag: ${releaseTag}`);

    // Fetch all releases
    const releases = await github.paginate(
            github.rest.repos.listReleases,
            {
                owner,
                repo,
                per_page: 100
            }
    );

    // Find any release still using this tag
    const existingRelease = releases.find(r => r.tag_name === releaseTag);
    if (existingRelease) {
        console.log(`Tag is still used by release ${existingRelease.name}.`);
        return;
    }

    // No releases; delete the tag
    console.log(`No releases found. Deleting tag...`);
    try {
        await github.rest.git.deleteRef({
            owner,
            repo,
            ref: `tags/${releaseTag}`
        });

        console.log(`Deleted tag: ${releaseTag}`);
    } catch (err) {
        console.log(`Failed to delete tag: ${err.message}`);
        core.setFailed(err.message);
    }
}
