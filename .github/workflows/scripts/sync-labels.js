/*
Synchronizes the repo's labels with a definition file.

This is intended to be run by workflow triggered manually or on a schedule.
 */
module.exports = async ({github, context, core}) => {
    const fs = require('fs');
    const {owner, repo} = context.repo;

    // Parse the standard labels
    const standardLabels = JSON.parse(fs.readFileSync('./labels.json', 'utf8'));

    // Fetch the existing labels
    // https://docs.github.com/en/rest/issues/labels#list-labels-for-a-repository
    const existingLabels = await github.paginate(github.rest.issues.listLabelsForRepo, {
        owner,
        repo,
        per_page: 100
    });

    // Process the standard labels
    for (const standardLabel of standardLabels) {
        const name = standardLabel.name;
        if (!name) {
            console.error('Skipping label with no name');
            continue;
        }

        const color = (standardLabel.color || 'ededed').replace(/^#/, '').toLowerCase();
        const description = standardLabel.description || '';
        const aliases = standardLabel.aliases || [];
        const shouldDelete = standardLabel.delete === true;

        // Initially assume we'll need to create it
        let create = !shouldDelete;

        // Iterate over existing labels, reversed to allow removal
        for (let i = existingLabels.length - 1; i >= 0; i--) {
            const existingLabel = existingLabels[i];
            const existingName = existingLabel.name;

            // Check for a match against the name or any alias
            let matched = false;
            if (existingName === name) {
                matched = true;
                // label exists: no need to create
                create = false;
                console.log(`Found an existing label matching name '${name}'`)
            } else {
                if (aliases.includes(existingName)) {
                    matched = true;
                    console.log(`Found an existing label matching alias '${existingName}' of name '${name}'`)
                }
            }

            if (matched) {
                // Match found: delete or update

                if (shouldDelete) {
                    // Delete
                    // https://docs.github.com/en/rest/issues/labels#delete-a-label
                    console.log(`Deleting label: '${existingName}'`);
                    try {
                        await github.rest.issues.deleteLabel({owner, repo, name: existingName});
                    } catch (error) {
                        console.error(`Failed to delete '${existingName}': ${error.message}`);
                    }
                } else {
                    // Update
                    // https://docs.github.com/en/rest/issues/labels#update-a-label
                    const rename = existingName !== name;
                    const recolor = existingLabel.color !== color;
                    const redesc = existingLabel.description !== description;
                    if (rename || recolor || redesc) {
                        console.log(`Updating label: '${existingName}'${rename ? ' -> ' + name : ''}`);
                        try {
                            await github.rest.issues.updateLabel({
                                owner,
                                repo,
                                name: existingName,
                                new_name: rename ? name : undefined,
                                color,
                                description
                            });
                        } catch (error) {
                            console.error(`Failed to update '${existingName}': ${error.message}`);
                        }
                    } else {
                        console.log(`No change for label: '${existingName}'`)
                    }
                }

                // Remove from the list so it doesn't get deleted or updated again
                existingLabels.splice(i, 1);
            }
        }

        if (create) {
            // Label doesn't exist yet: create it
            // https://docs.github.com/en/rest/issues/labels#create-a-label
            console.log(`Creating label: '${name}'`);
            try {
                await github.rest.issues.createLabel({
                    owner,
                    repo,
                    name,
                    color,
                    description
                });
                existingLabels.push(name);
            } catch (error) {
                console.error(`Failed to create '${name}': ${error.message}`);
            }
        }
    }

    // At this point we've deleted or updated every existing label that matched a
    // standard name or alias, so the existing list now only contains custom labels.
    if (process.env.ALLOW_CUSTOM !== 'true') {
        console.log(`Cleaning up ${existingLabels.length} custom label(s)`)
        for (const existingLabel of existingLabels) {
            const existingName = existingLabel.name;
            // Delete
            // https://docs.github.com/en/rest/issues/labels#delete-a-label
            console.log(`Deleting label: '${existingName}'`);
            try {
                await github.rest.issues.deleteLabel({owner, repo, name: existingName});
            } catch (error) {
                console.error(`Failed to delete '${existingName}': ${error.message}`);
            }
        }
    }
}
