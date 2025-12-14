# Simple Death Bans

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9--1.21.11-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.1.0-orange)](https://modrinth.com/project/simpledeathbans)

A hardcore survival Fabric mod featuring a progressive banning system with Soul Links, Mercy Cooldowns, and Resurrection Rituals.

**Author:** Partacus-SPQR  
**Source:** [GitHub](https://github.com/Partacus-SPQR/SimpleDeathBans)  
**Download:** [Modrinth](https://modrinth.com/project/simpledeathbans)

## Features

### 1. Progressive Ban System
- Death results in a temporary server ban
- Ban time increases with each death (tier system)
- Base ban time: 1 minute × tier × multiplier
- Maximum tier configurable (default: 10, or -1 for infinite)

### 2. The Soul Link ("Soulbound")
- **Togglable** (OP Level 4 only, default: OFF)
- **Automatic Mode**: Players are automatically paired with "Soul Partners" on login
- **Manual Mode**: Players can craft a **Soul Link Totem** and shift+right-click another player
  - Requires **mutual consent** - both players must shift+right-click each other
- Damage sharing: When Player A takes damage, Player B takes the same damage (configurable)
- **Death Pact**: If one partner dies, the other dies instantly
- **Soul Link Totem Protection**: If you die while holding a Soul Link Totem, your partner is saved instead of dying
- Both players hear the Wither spawn sound and receive the message "Your soul has been severed"

#### Soul Link Totem Recipe (Shapeless):
```
[Amethyst Shard] [Amethyst Shard]
[  Echo Shard ] [Amethyst Shard]
[Amethyst Shard]
```

### 3. The Mercy Cooldown (Survival Reward)
- Reduces ban tier over time for active players
- Prevents AFK farming with activity checks every 15 minutes
- Requirements: Move 50+ blocks OR interact with 20+ blocks
- After 24 hours of active playtime without deaths, tier decreases by 1
- Reward sound and message: "Your past sins are forgotten"

### 4. The Nemesis Multiplier (PvP Adjustment)
- PvP deaths: 0.5× ban time multiplier (less punishing)
- PvE deaths: 1.0× ban time multiplier
- Handles indirect kills (knockback into lava, etc.)

### 5. The Altar of Resurrection (Endgame Ritual)
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

### 6. The Ghost Echo (Immersion)
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
    "baseBanMinutes": 1,
    "banMultiplierPercent": 100,
    "maxBanTier": 10,
    "enableSoulLink": false,
    "soulLinkDamageSharePercent": 100,
    "soulLinkTotemSavesPartner": true,
    "mercyPlaytimeHours": 24,
    "mercyMovementBlocks": 50,
    "mercyBlockInteractions": 20,
    "pvpBanMultiplierPercent": 50,
    "pveBanMultiplierPercent": 100,
    "enableGhostEcho": true,
    "enableResurrectionAltar": true
}
```

**Note:** Multipliers are now stored as integer percentages (100 = 100%, 50 = 50%, etc.)

## Dependencies

**Required:**
- Fabric Loader 0.18.0+
- Minecraft 1.21.9, 1.21.10, or 1.21.11
- Fabric API

**Optional (Recommended):**
- Cloth Config ≥20.0.0 (enhanced config screen)
- ModMenu ≥17.0.0 (mod list integration)

## Installation

1. Install Fabric Loader and Fabric API
2. Download SimpleDeathBans jar
3. Place in `mods` folder
4. (Optional) Install Cloth Config and ModMenu for better config experience

## Single-Player Note

This mod works in single-player worlds with modified behavior:
- **Death Handling** - On death, the world saves and you're returned to the title screen (simulates ban)
- **Progressive Bans, Mercy Cooldown, Ghost Echo** - Work as expected
- **Soul Link** - No effect (requires multiple players)
- **Altar of Resurrection** - Can be completed solo, but cannot unban yourself while banned

Use `/sdb unban <name>` with cheats enabled to self-unban in single-player.

## Building

```bash
./gradlew build
```

Output jar will be in `build/libs/`

## License

MIT License - See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

**Made by Partacus-SPQR**
