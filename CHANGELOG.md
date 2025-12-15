# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2025-12-15

### Added
- **Shared Health System** - Server-wide damage sharing (all players share damage)
- **Shared Health Death Pact** - If ANY player takes lethal damage, ALL players die
- **Shared Health Totem Logic** - Multiple totem scenarios:
  - Totem Saves All ON: Any totem saves everyone
  - Totem Saves All OFF: Only totem holders survive
- **Multi-totem Support** - Handles multiple totem holders with appropriate notifications
- **Soul Link Totem** - New craftable item required for manual soul linking
  - Recipe: Amethyst Shards + Eye of Enders + Totem of Undying (yields 2)
  - Prevents conflicts with other mods using shift+right-click on players
- **Mutual Exclusivity** - Shared Health overrides Soul Link when enabled (existing links preserved)
- **Single-Player Freeze System** - New immersive death handling for single-player:
  - Full-screen overlay with countdown timer and animated effects
  - Complete player immobilization (movement, actions, interactions blocked)
  - Damage immunity while frozen (prevents death loops)
  - Auto-respawn when timer expires
  - Toggleable via config (Single Player Enabled option)

### Changed
- **Cloth Config UI** - Replaced broken sliders with reliable text fields
- **Config Tooltips** - Enhanced with multi-line explanations, ranges, and color-coded warnings
- **Fallback Config UI** - Added missing sliders (Ban Multiplier, PvP/PvE Multipliers, Soul Link Damage Share, Shared Health Damage)
- **Fallback Tooltips** - Updated all tooltips to match Cloth Config quality with ranges and helpful info
- **Soul Link Damage Share** - Now explicitly documented as NON-LETHAL only
- **Death Pact Clarification** - Lethal damage triggers instant death regardless of damage share %
- **Soul Link Manual Mode** - Now requires holding Soul Link Totem (avoids mod conflicts)

### Fixed
- **Shared Health Damage Calculation** - Fixed missing /100.0 in percentage calculation
- **Duplicate Soul Link Messages** - Added cooldown to prevent double message spam when shift+right-clicking
- **Soul Link Totem Save (TotemSavesPartner=OFF)** - Fixed scenarios where:
  - Player with totem now survives even if partner (no totem) takes lethal damage
  - Player without totem now dies if partner (with totem) takes lethal damage
- **Resurrection Ritual Formatting** - Fixed message box exceeding chat width
- **Resurrection Ritual Colors** - "Ban tier preserved" now red, "freed from void" now dark purple
- **Login Message** - Now correctly mentions Soul Link Totem for manual linking
- **Single-Player Config Sync** - Disabling mod now immediately clears ban and removes damage immunity
- **Single-Player Enabled Config** - Now properly synced to server so damage immunity respects config changes

### Technical
- Added SharedHealthMixin for server-wide death pact handling
- Simplified SharedHealthHandler to only handle non-lethal damage sharing
- Added interaction cooldown system to SoulLinkEventHandler
- Updated config documentation in README

## [1.1.0] - 2025-12-13

### Added
- **Manual Soul Linking** - Shift+right-click another player to request a soul link (requires mutual consent)
- **Pending Link Requests** - Both players must accept for a soul link to form
- **Soul Link Status Messages** - Context-aware feedback when interacting with linked/unlinked players
- **Config Validation** - Automatic clamping of config values to valid slider ranges
- **Network Packet System** - Client-server communication for keybind actions

### Fixed
- **Single-Player Death Handling** - Now properly saves world and returns to title screen instead of allowing respawn
- **Soul Link Totem Protection** - Totem of Undying now correctly saves your partner from death
- **Stack Overflow Crash** - Fixed infinite recursion in cross-handler damage sharing
- **Command Permissions** - All commands now properly require OP Level 4
- **Fallback Config Keybinds Button** - Now navigates directly to KeybindsScreen
- **Max Ban Tier Slider** - Now uses -1 = Infinite (matching Cloth Config behavior)
- **Config Slider Corruption** - Values outside slider ranges no longer break the UI

### Changed
- **Soul Link Manual Mode** - Now requires mutual consent (both players must shift+click each other)
- **Damage Share Tracking** - Added DamageShareTracker utility to prevent recursive damage loops
- **SimpleFallbackConfigScreen** - Complete overhaul with improved slider handling and navigation

### Technical
- Added LivingEntityMixin for soul link totem interception
- Added DamageShareTracker utility class
- Added ModNetworking for client-server packet handling
- Improved SoulLinkManager with pending request system

## [1.0.0] - 2025-12-13

### Added
- Progressive ban system with configurable tier-based duration
- Soul Link feature for player pairing with shared damage and death pact
- Mercy Cooldown system rewarding active gameplay with tier reduction
- Nemesis Multiplier for differentiated PvP/PvE ban penalties
- Altar of Resurrection endgame ritual with Resurrection Totem crafting
- Ghost Echo cosmetic effects on player death
- Full command suite for ban management and soul link administration
- JSON configuration with in-game editing support
- ModMenu integration with optional Cloth Config support
- Fallback configuration screen for vanilla-compatible setup

### Technical
- Multi-version support: Minecraft 1.21.9, 1.21.10, 1.21.11
- Built with Stonecutter for separate version-specific JARs
- Requires Fabric Loader 0.18.0+
- Java 21 required
