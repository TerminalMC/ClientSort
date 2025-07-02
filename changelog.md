# Changelog

## 2.0.0-beta.5

- Improved compatibility with Neo/Forge modded containers extending ItemStackHandler
- Fixed button layout class-name validator preventing config save after removing a mod with a
  configured layout
- Added a support workaround for supermartijn642corelib
- Improved compatibility with sophisticatedcore
- Added full inheritance checking for layout key classes
- Updated Russian translation (rfin0)

## 2.0.0-beta.4

- Fixed an issue breaking button editor on modded inventory screens
- Slightly improved slot placement on scrolling inventories

## 2.0.0-beta.3

- Fixed a crash when returning to editor from selector with all buttons disabled

## 2.0.0-beta.2

- Added a global config toggle button to the button selector screen
- Fixed buttons staying highlighted after completion of an action
- Replaced status button with right-click to toggle individual status in editor
- Moved GUI editor instructions to a tooltip
- Fixed layout key split option allowing invalid layout creation
- Fixed layout class indicator
- Fixed a bug breaking deletion of layout keys
- Fixed an issue with button generation in modded inventories

## 2.0.0-beta.1

Note: `v2.x.x` versions are not compatible with `v1.x.x` config files, or vice versa.

- Moved stack collection to server when using server-accelerated sort
- Fixed player inventory sorting in creative mode
- Added support for stack fill and transfer operations
- Added GUI buttons (off by default) as an alternative to using keybinds

## 1.3.3

- Fixed serverside sorting with certain storage mods

## 1.3.2

- Fixed a crash on NeoForge introduced in v1.3.1

## 1.3.1

- Improved EMI compatibility

## 1.3.0

- Enabled server accelerated sorting

## 1.2.0-beta.1

- Fixed version metadata
- Fixed a bug causing multiplayer sort rate to be used in singleplayer
- Added optional sorting sounds

## 1.1.3-beta.1

- Removed a guard condition preventing compatibility with Traveler's Backpack

## 1.1.2

- Updated Russian translation (rfin0)

## 1.1.1

- Fixed keybind translation

## 1.1.0

- Updated bundle handling
- Added Ukrainian translation (ttrafford7)
- Added support for ItemLocks
- Added an option to change behavior of extra slots (e.g. offhand) when sorting
