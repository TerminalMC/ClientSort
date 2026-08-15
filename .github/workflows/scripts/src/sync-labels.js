/**
 * @typedef {import('@octokit/rest').Octokit} Octokit
 * @typedef {import('@actions/github').context} Context
 * @typedef {import('@actions/core')} Core
 * @typedef {import('@octokit/openapi-types').components['schemas']['label']} Label
 */
/**
 * @typedef {Object} LabelSpec
 * @property {string | undefined} name
 * @property {string | undefined} color
 * @property {string | undefined} description
 * @property {string[] | undefined} aliases
 * @property {boolean | undefined} delete
 */

/**
 * Synchronizes the repo's labels with a definition file.
 *
 * At runtime, the definition `labels.json` file must be available in the script execution
 * directory. Assuming this script is invoked from a workflow, this can be achieved by committing
 * the `labels.json` file into the `.github/workflows` directory, copying it to that directory
 * before executing this script, or downloading it from a remote location.
 *
 * This is intended to be run by a workflow triggered on push of the `labels.json` file or, if
 * using a remote file, on a schedule.
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
    const fs = require('fs');
    const {owner, repo} = context.repo;

    //
    // Functions
    //

    /**
     * @returns {Promise<Label[]>}
     */
    async function getAllLabels() {
        // https://docs.github.com/en/rest/issues/labels#list-labels-for-a-repository
        return await github.paginate(github.rest.issues.listLabelsForRepo, {
            owner,
            repo,
            per_page: 100,
        });
    }

    /**
     * @param {string} name
     * @param {string} color
     * @param {string} description
     * @returns {Promise<Label>}
     */
    async function createLabel(name, color, description) {
        // https://docs.github.com/en/rest/issues/labels#create-a-label
        const {data: labelData} = await github.rest.issues.createLabel({
            owner,
            repo,
            name,
            color,
            description
        });
        return labelData;
    }

    //
    // Control flow
    //

    /** @type {LabelSpec[]} */
    const definedLabels = JSON.parse(fs.readFileSync('./labels.json', 'utf8'));
    const existingLabels = await getAllLabels();

    for (const definedLabel of definedLabels) {
        const name = definedLabel.name;
        if (!name) {
            console.error('Skipping label with no name');
            continue;
        }

        const color = (definedLabel.color || 'ededed').replace(/^#/, '').toLowerCase();
        const description = definedLabel.description || '';
        const aliases = definedLabel.aliases || [];
        const shouldDelete = definedLabel.delete === true;

        //  initially assume we'll need to create it
        let create = !shouldDelete;

        // iterate over existing labels, reversed to allow removal
        for (let i = existingLabels.length - 1; i >= 0; i--) {
            const existingLabel = existingLabels[i];
            const existingName = existingLabel.name;

            // check for a match against the name or any alias
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
                // match found: delete or update

                if (shouldDelete) {
                    // delete
                    // https://docs.github.com/en/rest/issues/labels#delete-a-label
                    console.log(`Deleting label: '${existingName}'`);
                    try {
                        await github.rest.issues.deleteLabel({owner, repo, name: existingName});
                    } catch (error) {
                        console.error(`Failed to delete '${existingName}': ${error.message}`);
                    }
                } else {
                    // update
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

                // remove from the list so it doesn't get deleted or updated again
                existingLabels.splice(i, 1);
            }
        }

        if (create) {
            // label doesn't exist yet: create it
            // https://docs.github.com/en/rest/issues/labels#create-a-label
            console.log(`Creating label: '${name}'`);
            try {
                existingLabels.push(await createLabel(name, color, description));
            } catch (error) {
                console.error(`Failed to create '${name}': ${error.message}`);
            }
        }
    }

    // At this point we've deleted or updated every existing label that matched a
    // defined name or alias, so the 'existing' list now only contains custom labels,
    // which can be removed.
    if (process.env.ALLOW_CUSTOM !== 'true') {
        console.log(`Cleaning up ${existingLabels.length} custom label(s)`)
        for (const existingLabel of existingLabels) {
            const existingName = existingLabel.name;
            // delete
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
