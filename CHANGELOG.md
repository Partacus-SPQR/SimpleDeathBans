# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2025-12-15

### Added
- **Shared Health System** - Server-wide damage sharing where all players share damage
- **Shared Health Death Pact** - Lethal damage to any player kills all players instantly
- **Shared Health Totem Logic** - Configurable totem behavior (save all vs save self only)
- **Soul Link Totem** - New craftable item required for manual soul linking
  - Recipe: Amethyst Shards + Eyes of Ender + Totem of Undying
  - Prevents conflicts with other mods using shift+right-click
- **Void Crystal Totem** - New item to sever soul links with penalties
  - Recipe: End Crystals + Soul Link Totem
  - Increases ban tier and applies cooldowns
- **Single-Player Freeze System** - Immersive death handling for single-player worlds
  - Full-screen overlay with animated countdown timer
  - Complete player immobilization with damage immunity
  - Auto-respawn when ban timer expires
  - Toggleable via config

### Changed
- **Config UI** - Replaced broken sliders with reliable text fields in Cloth Config
- **Config Tooltips** - Enhanced with multi-line explanations and color-coded warnings
- **Soul Link** - Manual mode now requires holding Soul Link Totem
- **Mutual Exclusivity** - Shared Health overrides Soul Link when enabled

### Fixed
- **Shared Health Damage Calculation** - Corrected percentage calculation
- **Duplicate Soul Link Messages** - Added cooldown to prevent spam
- **Soul Link Totem Protection** - Fixed all totem save scenarios
- **Resurrection Ritual** - Fixed message formatting and colors
- **Config Permission Check** - Non-operators can now view but not modify settings
- **Single-Player Config Sync** - Disabling mod immediately clears ban state

### Technical
- Multi-version support via Stonecutter (1.21.9, 1.21.10, 1.21.11)
- Version-specific item models for cross-version compatibility
- Added SharedHealthMixin for server-wide death handling

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
