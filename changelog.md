# Changelog

## 3.102.5

- Fixed creative sort order sporadically misplacing items
- Fixed creative search order not working when optimization was switched off

## 3.102.4

- Reset `mc` version counter to 1 at mc1.0.0
- Fixed ratelimit executor preventing shutdown (TauCu)
- Added a command to open the config screen

## 3.2.3

- Fixed a potential compat issue caused by removal of action buttons from old screens
- Removed refmap usages

## 3.2.2

- Updated Russian translation (rfin0)

## 3.1.1

- Added automatic conversion for intermediary class policies

## 3.0.0

- Re-enabled config screen
- Fixed built-in resourcepack loading on NeoForge

## 3.0.0-beta.2

- Updated to mc26.1
- Temporarily disabled config screen
- Mod versioning scheme is now `major.mc.minor`:
  - `major` is incremented on 'significant' feature changes, or breaking API changes (if
    applicable).
  - `mc` is never reset, and is incremented on every MC release, irrespective of whether a mod
    update was required.
  - `minor` is reset when `major` is changed, and is incremented on every update that does not
    change either of the previous two numbers.
