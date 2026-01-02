# Simple Death Bans

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9--1.21.11-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.2.1-orange)](https://modrinth.com/project/simpledeathbans)

A hardcore survival Fabric mod featuring a progressive banning system with Soul Links, Mercy Cooldowns, and Resurrection Rituals.

**Author:** Partacus-SPQR  
**Source:** [GitHub](https://github.com/Partacus-SPQR/SimpleDeathBans)  
**Download:** [Modrinth](https://modrinth.com/project/simpledeathbans)

## Version Compatibility

| Minecraft | Mod Version | Fabric Loader | Fabric API | Cloth Config | ModMenu |
|-----------|-------------|---------------|------------|--------------|---------|
| **1.21.11** *(Primary)* | 1.2.1 | ≥0.18.2 | ≥0.139.5 | ≥21.11.150 | ≥17.0.0 |
| 1.21.10 | 1.2.1 | ≥0.18.2 | ≥0.138.3 | ≥20.0.149 | ≥16.0.0 |
| 1.21.9 | 1.2.1 | ≥0.18.2 | ≥0.134.0 | ≥20.0.149 | ≥16.0.0 |

> **Note:** Cloth Config and ModMenu are optional but recommended for the best configuration experience.

## Features

### 1. Progressive Ban System
- Death results in a temporary server ban
- Ban time increases with each death (tier system)
- Base ban time: 1 minute × tier × multiplier
- Maximum tier configurable (default: 10, or -1 for infinite)

### 2. The Soul Link ("Soulbound")
- **Togglable** (OP Level 4 only, default: OFF)
- **Automatic Mode**: Players are automatically paired with "Soul Partners" on login
- **Manual Mode**: Hold a **Soul Link Totem** and shift+right-click another player to request a soul link
  - Requires **mutual consent** - both players must hold the totem and shift+right-click each other
  - Using an item prevents conflicts with other mods that use shift+right-click on players
- **Damage Sharing**: When Player A takes damage, Player B takes the same damage (configurable %)
  - **Important**: Damage share % only affects NON-LETHAL damage
  - Example: 50% damage share = Partner takes half damage on every hit
- **Death Pact**: If one partner takes LETHAL damage, both die instantly
  - Death Pact is NOT affected by damage share % - lethal = instant death for both
- **Totem of Undying Protection**:
  - **Totem Saves Partner ON**: Any totem saves BOTH players
  - **Totem Saves Partner OFF**: Totem only saves the holder, partner dies
  - If BOTH have totems: Both totems consumed, both survive

#### Soul Link Totem Recipe (3×3 Shaped):
```
[Amethyst Shard] [Eye of Ender] [Amethyst Shard]
[  Eye of Ender ] [Totem of Undying] [  Eye of Ender ]
[Amethyst Shard] [Eye of Ender] [Amethyst Shard]
```
*Yields 1 Soul Link Totem*

#### Void Crystal Totem (Soul Link Severance)
- **Purpose**: Sever your current soul link
- **Usage**: Right-click while holding to break your soul bond
- **Penalty**: Increases your ban tier by 1 (configurable)
- **Cooldowns Applied**:
  - You cannot form a new link for 30 minutes (configurable)
  - Cannot re-link with your ex-partner for 24 hours (configurable)
- **Visual**: Has enchantment glint to indicate power

#### Void Crystal Totem Recipe (3×3 Shaped):
```
[End Crystal]    [    Empty    ]   [End Crystal]
[    Empty   ] [Soul Link Totem] [    Empty   ]
[End Crystal]    [    Empty    ]   [End Crystal]
```
*Yields 1 Void Crystal Totem*

### 3. Shared Health (Server-Wide Health Pool)
- **Togglable** (OP Level 4 only, default: OFF)
- **Server-Wide Damage**: When ANY player takes damage, ALL players take damage
- **Damage Share %**: Controls how much damage others receive (default 100% = 1:1)
  - **Important**: Damage share % only affects NON-LETHAL damage
- **Death Pact**: If ANY player takes LETHAL damage, ALL players die instantly
  - Death Pact is NOT affected by damage share % - lethal = instant death for everyone

#### Totem Saves All = ENABLED (default):
- **One player has totem**: That player's totem saves EVERYONE
  - Notification: `"§k><§r [Player]'s totem has saved everyone from the void! §k><§r"`
- **Multiple players have totems**: ALL totems consumed, everyone saved
  - Notification: `"§k><§r Multiple people have saved others from the void! §k><§r"`
- **No one has totem**: ALL players die with default ban logic

#### Totem Saves All = DISABLED:
- **Players WITH totems**: Survive (their totem is consumed)
- **Players WITHOUT totems**: Die with default ban logic
  - Single survivor: `"§k><§r [Player] is the only one to survive from the void! §k><§r"`
  - Multiple survivors: `"§k><§r Multiple people have survived the voids grasp! §k><§r"`

### 4. The Mercy Cooldown (Survival Reward)
- Reduces ban tier over time for active players
- Prevents AFK farming with activity checks every 15 minutes
- Requirements: Move 50+ blocks OR interact with 20+ blocks
- After 24 hours of active playtime without deaths, tier decreases by 1
- Reward sound and message: "Your past sins are forgotten"

### 5. The Nemesis Multiplier (PvP Adjustment)
- PvP deaths: 0.5× ban time multiplier (less punishing)
- PvE deaths: 1.0× ban time multiplier
- Handles indirect kills (knockback into lava, etc.)

### 6. The Altar of Resurrection (Endgame Ritual)
- **The ultimate endgame feature** to unban a friend
- Requires a **Resurrection Totem** and server-wide consensus

#### Resurrection Totem Recipe (3×3 Shaped):
```
[Totem of Undying] [Nether Star] [Totem of Undying]
[  Nether Star  ] [Heavy Core] [  Nether Star  ]
[Totem of Undying] [Nether Star] [Totem of Undying]
```

#### Altar Requirements:
1. **Fully Powered Beacon** with a complete **Netherite Base**
   - 4 layers of Netherite Blocks (164 total blocks!)
   - Layer 1: 3×3 (9 blocks)
   - Layer 2: 5×5 (25 blocks)
   - Layer 3: 7×7 (49 blocks)
   - Layer 4: 9×9 (81 blocks)

2. **Server Consensus Required**
   - ALL online players must participate
   - Each player must Sneak + Right-click the Beacon while holding the totem
   - Ritual cannot be completed until everyone commits

#### How to Perform the Ritual:
1. Build a fully powered Netherite beacon pyramid
2. **Initiator** (player holding Resurrection Totem): Sneak + Right-click the Beacon
   - Broadcasts: `"[Player] has initiated the Altar of Resurrection! [1/X] players have committed."`
3. All other online players: Sneak + Right-click the Beacon to commit (no totem needed!)
4. When all players have committed:
   - **Totem of Undying activation particles** burst on ALL participating players
   - Totem activation sound plays at each player's location
   - Lightning strikes dramatically at the beacon
   - A **random banned player** is unbanned
   - **Ban tier is PRESERVED** (they keep their punishment level!)
   - **Only the initiator's totem is consumed** (one totem per ritual)

#### Ritual Cancellation:
- If any committed player disconnects, the ritual is cancelled
- Ritual times out after 5 minutes

### 7. The Ghost Echo (Immersion)
- When a player dies and is banned:
  - Cosmetic lightning bolt spawns at death location
  - Custom death message (gray, italic): "[Player] has been lost to the void for [X] minutes"

## Commands

All commands require **Operator Level 4**.

| Command | Description |
|---------|-------------|
| `/simpledeathbans reload` | Reload configuration |
| `/simpledeathbans settier <player> <tier>` | Set player's ban tier |
| `/simpledeathbans gettier <player>` | Get player's current ban tier |
| `/simpledeathbans unban <player>` | Remove active ban (keeps tier) |
| `/simpledeathbans clearbans` | Clear all active bans |
| `/simpledeathbans listbans` | List all currently banned players |
| `/simpledeathbans soullink set <player1> <player2>` | Manually link two players |
| `/simpledeathbans soullink clear <player>` | Remove a player's soul link |
| `/simpledeathbans soullink status <player>` | Check a player's soul link |

Alias: `/sdb` can be used instead of `/simpledeathbans`

## Configuration

Config file: `config/simpledeathbans.json`

```json
{
    "enableDeathBans": true,
    "baseBanMinutes": 1,
    "banMultiplierPercent": 100,
    "maxBanTier": -1,
    "exponentialBanMode": false,
    "enableSoulLink": false,
    "soulLinkShareHunger": false,
    "soulLinkDamageSharePercent": 100,
    "soulLinkRandomPartner": true,
    "soulLinkTotemSavesPartner": true,
    "soulLinkSeverCooldownMinutes": 30,
    "soulLinkSeverBanTierIncrease": 1,
    "soulLinkExPartnerCooldownHours": 24,
    "soulLinkRandomReassignCooldownHours": 12,
    "soulLinkRandomAssignCheckIntervalMinutes": 60,
    "soulLinkCompassMaxUses": 10,
    "soulLinkCompassCooldownMinutes": 10,
    "enableSharedHealth": false,
    "sharedHealthDamagePercent": 100,
    "sharedHealthTotemSavesAll": true,
    "sharedHealthShareHunger": false,
    "enableMercyCooldown": true,
    "mercyPlaytimeHours": 24,
    "mercyMovementBlocks": 50,
    "mercyBlockInteractions": 20,
    "mercyCheckIntervalMinutes": 15,
    "pvpBanMultiplierPercent": 50,
    "pveBanMultiplierPercent": 100,
    "enableGhostEcho": true,
    "enableResurrectionAltar": true
}
```

### Configuration Reference

| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| `enableDeathBans` | true/false | true | Master switch to enable/disable death bans |
| `baseBanMinutes` | 1-60 | 1 | Base ban duration per tier (minutes) |
| `banMultiplierPercent` | 10-1000 | 100 | Global ban time multiplier (100 = 1x) |
| `maxBanTier` | -1 to 100 | -1 | Maximum ban tier (-1 = infinite) |
| `exponentialBanMode` | true/false | false | Use doubling formula (1,2,4,8...) vs linear |
| `enableSoulLink` | true/false | false | Enable Soul Link partner system |
| `soulLinkDamageSharePercent` | 0-200 | 100 | Damage shared to partner (non-lethal only) |
| `soulLinkRandomPartner` | true/false | true | Auto-pair vs manual Soul Link Totem |
| `soulLinkTotemSavesPartner` | true/false | true | Totem of Undying saves both partners |
| `soulLinkShareHunger` | true/false | false | Share hunger loss with soul-linked partner |
| `soulLinkSeverCooldownMinutes` | 0-120 | 30 | Cooldown after breaking a soul link |
| `soulLinkSeverBanTierIncrease` | 0-10 | 1 | Ban tier penalty for severing |
| `soulLinkExPartnerCooldownHours` | 0-168 | 24 | Cooldown before re-linking with ex-partner |
| `soulLinkRandomReassignCooldownHours` | 0-72 | 12 | Grace period before auto-reassignment |
| `soulLinkRandomAssignCheckIntervalMinutes` | 1-1440 | 60 | How often to check for unlinked players |
| `soulLinkCompassMaxUses` | 1-100 | 10 | Tracking uses per Soul Link Totem |
| `soulLinkCompassCooldownMinutes` | 0-60 | 10 | Cooldown between tracking uses |
| `enableSharedHealth` | true/false | false | Enable server-wide health sharing |
| `sharedHealthDamagePercent` | 0-200 | 100 | Damage shared to all players (non-lethal only) |
| `sharedHealthTotemSavesAll` | true/false | true | One totem saves everyone |
| `sharedHealthShareHunger` | true/false | false | Share hunger loss with all players |
| `enableMercyCooldown` | true/false | true | Enable ban tier reduction over time |
| `mercyPlaytimeHours` | 1-168 | 24 | Active hours needed to reduce tier |
| `mercyMovementBlocks` | 0-500 | 50 | Blocks moved per check to count as active |
| `mercyBlockInteractions` | 0-200 | 20 | Interactions per check to count as active |
| `mercyCheckIntervalMinutes` | 1-60 | 15 | Minutes between activity checks |
| `pvpBanMultiplierPercent` | 0-500 | 50 | Ban modifier for PvP deaths |
| `pveBanMultiplierPercent` | 0-500 | 100 | Ban modifier for PvE deaths |
| `enableGhostEcho` | true/false | true | Lightning + death message on ban |
| `enableResurrectionAltar` | true/false | true | Enable Resurrection Altar feature |

**Note:** 
- Multipliers are stored as integer percentages (100 = 100%, 50 = 50%, etc.)
- Damage share percentages only affect NON-LETHAL damage
- Lethal damage triggers Death Pact (instant death) regardless of percentage

## Dependencies

### Required
- **Fabric Loader** ≥0.18.2
- **Fabric API** (version depends on Minecraft version - see compatibility table above)

### Optional (Recommended)
- **Cloth Config** - Enhanced configuration screen with sliders and tooltips
- **ModMenu** - Mod list integration for easy config access

> **Tip:** Without Cloth Config, a built-in fallback config screen is provided with full functionality.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version
2. Download the matching [Fabric API](https://modrinth.com/mod/fabric-api) version
3. Download the SimpleDeathBans JAR for your Minecraft version
4. Place all JARs in your `mods` folder
5. (Optional) Install Cloth Config and ModMenu for enhanced config experience

## Single-Player Mode

This mod includes a dedicated **Single-Player Mode** (default: ON) with modified behavior:

### How It Works
- **Death Freezes You** - On death, instead of kicking you out, you're frozen in place with a full-screen overlay
- **Countdown Timer** - Visual timer shows remaining ban time with animated effects
- **Damage Immunity** - You cannot be hurt while frozen (prevents death loops)
- **Complete Immobilization** - Movement, jumping, attacking, item use, block breaking/placing all disabled
- **Auto-Unfreeze** - When timer expires, you automatically respawn and can play normally

### Single-Player Config Option
- **Enabled (default)**: Uses the freeze system described above
- **Disabled**: Returns to title screen on death (original behavior)
- Access via Mod Menu → Simple Death Bans → Single Player section

### Features That Work Normally
- **Progressive Bans** - Ban tier increases with each death
- **Mercy Cooldown** - Active playtime reduces your ban tier
- **Ghost Echo** - Lightning and death messages still appear

### Features Requiring Multiplayer
- **Soul Link** - No effect (requires multiple players)
- **Shared Health** - No effect (requires multiple players)
- **Altar of Resurrection** - Can be completed solo, but cannot unban yourself while banned

Use `/sdb unban <name>` with cheats enabled to self-unban in single-player.

## Building from Source

```bash
# Build all versions
./gradlew build

# Build specific version
./gradlew :1.21.11:build
./gradlew :1.21.10:build
./gradlew :1.21.9:build
```

Output JARs will be in:
- `versions/1.21.11/build/libs/simpledeathbans-1.2.1+1.21.11.jar`
- `versions/1.21.10/build/libs/simpledeathbans-1.2.1+1.21.10.jar`
- `versions/1.21.9/build/libs/simpledeathbans-1.2.1+1.21.9.jar`

## License

MIT License - See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

**Made by Partacus-SPQR**
