# Changelog

## 2.1.7

- Fixed a crash when performing server-accelerated ops on Forge 1.20.1 introduced in v2.1.6

## 2.1.6

- Added save-state after server-accelerated operations
- Disabled operations and buttons in spectator mode
- Added an option to show button tooltips

## 2.1.5

- Improved platform service loader
- Added locked-slot compatibility with Stack to Nearby Chests
- Removed compile-time dependency on ItemLocks
- Disabled validation on dedicated server installation by default

## 2.1.4

- Improved compatibility with modded bundles

## 2.1.3

- Improved compatibility with Accessories mod
- Added an option to anchor buttons to the left side of the inventory

## 2.1.2

- Fixed Simplified Chinese translation (Xinyang-Gao)

## 2.1.1

- Updated Simplified Chinese translation (Xinyang-Gao)

## 2.1.0

Note: this version includes networking changes, servers and clients must be upgraded together.

- Added a per-policy option to automatically trigger a single operation when opening an inventory

## 2.0.3

- Fixed a compatibility issue with creative search introduced in v2.0.1

## 2.0.2

- Fixed hotbar and extra slot scoping being overridden by smart grouping
- Fixed trigger buttons not appearing on initial inventory open in creative mode

## 2.0.1

- Updated Russian translation (rfin0)
- Fixed creative search (TauCu)

## 2.0.0

- Increased interaction interval limit to 1000ms
- Fixed item type validator also checking stack size
- Fixed buttons showing stacked on selector screen when disabled
- Enabled buttons by default
- Disabled stack-fill button on player inventory by default
- Added Simplified Chinese translation (Xinyang-Gao)
- Improved in-game configuration instructions
- Removed the option to enable serverside validation in singleplayer
  - Manually-added serverside policies still apply

## 2.0.0-beta.20

- Improved button reference slot selection algorithm
- Improved slot collection heuristics
- Improved compatibility with Traveler's Backpack
  - Tool and upgrade slots will no longer be affected by operations on the main inventory
- Fixed a minor issue with policy saving when first enabling buttons on a new inventory type
- Fixed buttons disappearing on screen resize on Fabric
- Fixed buttons disappearing on creative inventory tab switch
- Disabled operations in extra inventory slots by default
- Disabled operations in certain inventories from the following mods by default:
  - Traveler's Backpack
  - Tom's Simple Storage Mod
- Added several serverside config options for op validation
  - Validation is now disabled in singleplayer by default
  - Item count validation now tolerates a difference of 32 by default

## 2.0.0-beta.19

- Fixed a threading issue with server-acceleration payload handling
- Enabled ops in donkey and mule inventories by default

## 2.0.0-beta.18

- Added a built-in dark-mode resource pack

## 2.0.0-beta.17

- Fixed an issue with sorting using an outdated reference snapshot on certain versions
- Added an option to disable validation of server-accelerated operation results

## 2.0.0-beta.16

- Fixed an issue causing locked slots to apply at the wrong position when switching inventory types
- Added a workaround for a crash when attempting to operate on containerless modded slots
- Increased warning display time to 5 seconds
- Added automatic update information to server class policy config

## 2.0.0-beta.15

Note: this beta version includes networking changes, servers and clients must be upgraded together.

- Improved communication of server-accelerated operation failure
- Added an option to fall back to client operations when a server-accelerated operation fails

## 2.0.0-beta.14

- Added support for configuring specific items to always be sorted to the start or end

## 2.0.0-beta.13

- Added a new operation 'Transfer Matching', which only transfers item types that already exist in
  the destination inventory
- Added options to configure type-matching

## 2.0.0-beta.12

- Fixed button status not saving when changed via editor screen

## 2.0.0-beta.11

Warning: this beta version includes breaking changes to mod config, and downgrading to previous
versions will result in data loss.

- Fixed an issue with serialization of defaulted offset values
- Added support for configuring keybinds via mod options
- Added an option to isolate mod keybinds from Minecraft keybinds
- Redesigned policy system and layout data-string configuration
- Added support for ignoring specific slots when performing operations
- Improved robustness of config deserializer
- Fixed an issue with stack collection in creative client sorting

## 2.0.0-beta.10

- Re-enabled serverside loading on Fabric
- Updated Russian translation (rfin0)

## 2.0.0-beta.9

- Fixed another issue causing items to be added to bundles
- Fixed detection of bundle variants (>= mc1.21.2)

## 2.0.0-beta.8

- Fixed an issue causing items to be added to bundles
- Prevented running multiple client-side operations simultaneously
- Reduced redundant interactions when sorting client-side in creative-mode

## 2.0.0-beta.7

- Added serverside detection for invalid inventory state during an operation
- Added serverside class-policy configuration with automatic blacklisting of inventories causing
  invalid state
- Added clientside class-policy configuration to manually disable operations
- Disabled all operations on Create Toolboxes by default

Changes over v2.0.0-beta.6

- Fixed command registration breaking other mods
- Fixed an inconsistency with inventory updates when using client creative operations

## 2.0.0-beta.6

- Added serverside detection for invalid inventory state during an operation
- Added serverside class-policy configuration with automatic blacklisting of inventories causing
  invalid state
- Added clientside class-policy configuration to manually disable operations
- Disabled all operations on Create Toolboxes by default

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
