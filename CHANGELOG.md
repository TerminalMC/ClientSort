# Changelog

This project uses the versioning scheme `<major>.<minecraft>.<minor>[-<alpha|beta>.<build>]`,
according to the following rules:

- `major` is incremented on backwards-incompatible API changes, or major feature changes under the
  project owner's interpretation of the word.
- `minecraft` denotes the Minecraft version that the release is built against, and is initialized to
  `1` for Minecraft `v1.0.0` and incremented by `1` for each release version thereafter.
- `minor` is initialized to `0` on the first release for any Minecraft version, is reset to `0` when
  `major` is incremented, and is incremented by `1` on any other change.
- `-<alpha|beta>.build` is used to indicate that a version should be considered less stable and less
  well-tested than normal. `build` is reset to `0` when changing from `alpha` to `beta` and is
  incremented by `1` on each release.

Unreleased changes should be listed in the "Unreleased" changelog entry. Immediately prior to a
release, the "Unreleased" header should be replaced with the release version and the release date
(UTC), and a new "Unreleased" entry should be created above the renamed entry.

Each changelog entry should be populated with entries in the following order:

- Security updates, in any form.
- Minecraft target version updates, in the form "Updated to mc\<version>"
- Additions, in the form "Added \<feature>".
- Changes, in any form (e.g., "\<feature> now \<new behavior>").
- Deprecations, in the form "Deprecated \<API surface>".
- Removals, in the form "Removed \<feature>".
- Fixes, in the form "Fixed an issue \<causing> \<problem description>".
- Translation updates, in the form "Updated \<language> translation".

Additional notes:

- The first line of a changelist entry should be a concise, single-sentence summary. If additional
  information is to be provided, nested bullets should be used.
- If a changelist entry is associated with an external contribution, the contributor's username and
  the contribution number in parentheses should be appended to the summary.
  - e.g., "Added an option to do something (someone) (#23)."
- If a changelist entry is associated with a ticket, the ticket number in parentheses should be
  appended to the summary.
  - e.g., "Fixed an issue causing entities to disappear (#45)."
- The changelog entry for the first beta release following an alpha series, and for the first full
  release following an alpha/beta series, should include two top-level bullets: "Changes since last
  \<alpha|beta>" and "Changes since last full release".
- If a release is yanked, the changelog entry should not be removed but should have "[YANKED]"
  appended to the header.

___

## Unreleased

## 3.103.2-beta.2 [2026-08-17]

- Added a button to the editor screen to open the main config menu.
- Fixed an issue causing server-side sorting to sometimes misplace the last item.
- Fixed an issue causing the ItemLocks bypass key to not function.
- Fixed an issue causing titled policies to be removed on config save.
- Fixed some minor issues with policy splitting and slot testing.

## 3.103.2-beta.1 [2026-08-15]

- Added support for Locked in Slots.
- Added a screen-class blacklist to allow disabling buttons on certain modded screens.
- Fixed a crash when pressing `Open Editor` key if also bound to `Drop Selected Item`.
- Fixed a crash when parsing a class policy containing a number over the integer limit.
- Fixed an issue causing certain items with data to always be sorted to the end.
- Fixed an issue causing server-accelerated ops to not trigger refresh events.
- Fixed an issue causing sorting to include dedicated slots.

## 3.103.1 [2026-07-04]

- Fixed an issue causing creative sort order to sporadically misplace items.
- Fixed an issue causing creative search order to not work when optimization was switched off.

## 3.103.0 [2026-06-19]

- Updated to mc26.2
