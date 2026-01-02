# Simple Death Bans

A survival/SMP Fabric mod featuring a progressive banning system with Soul Links, Mercy Cooldowns, and Resurrection Rituals.

## Features

### Progressive Ban System
- Death = temporary server ban with increasing duration
- Ban time scales with tier: base × tier × multiplier
- Configurable max tier (default: unlimited)

### Soul Link (Partner System)
- **Optional** - Disabled by default
- Pair with another player as "Soul Partners"
- **Damage Sharing**: Partner takes shared damage (configurable %)
- **Death Pact**: Lethal damage kills both instantly
- **Totem Protection**: Configurable totem save behavior

#### Soul Link Totem Recipe:
```
[Amethyst Shard] [Eye of Ender] [Amethyst Shard]
[  Eye of Ender ] [Totem of Undying] [  Eye of Ender ]
[Amethyst Shard] [Eye of Ender] [Amethyst Shard]
```

#### Void Crystal Totem (Sever Link):
```
[End Crystal]    [    Empty    ]   [End Crystal]
[    Empty   ] [Soul Link Totem] [    Empty   ]
[End Crystal]    [    Empty    ]   [End Crystal]
```
*Warning: Severing increases your ban tier!*

### Shared Health (Server-Wide)
- **Optional** - Disabled by default
- ALL players share damage and death
- One totem can save everyone (configurable)

### Mercy Cooldown
- Reduces ban tier over time for active players
- 24 hours of active playtime = -1 tier
- Prevents AFK farming with activity checks

### Nemesis Multiplier
- PvP deaths: 50% ban time (less punishing)
- PvE deaths: 100% ban time

### Altar of Resurrection
The ultimate endgame feature to unban a banned player.

#### Resurrection Totem Recipe:
```
[Totem of Undying] [Nether Star] [Totem of Undying]
[  Nether Star  ] [Heavy Core] [  Nether Star  ]
[Totem of Undying] [Nether Star] [Totem of Undying]
```

#### Requirements:
- Fully powered **Netherite Beacon** (164 Netherite Blocks!)
- **ALL online players** must participate
- Sneak + Right-click beacon to commit

### Ghost Echo
- Lightning strikes at death location
- Custom death message: *"[Player] has been lost to the void for [X] minutes"*

### Single-Player Mode
- Death freezes you with countdown overlay instead of kicking
- Damage immunity while frozen
- Auto-respawn when timer expires

## Commands

All commands require **OP Level 4**. Alias: `/sdb`

| Command | Description |
|---------|-------------|
| `/sdb reload` | Reload configuration |
| `/sdb settier <player> <tier>` | Set player's ban tier |
| `/sdb gettier <player>` | Get player's ban tier |
| `/sdb unban <player>` | Remove active ban |
| `/sdb clearbans` | Clear all bans |
| `/sdb listbans` | List banned players |
| `/sdb soullink set <p1> <p2>` | Link two players |
| `/sdb soullink clear <player>` | Remove soul link |
| `/sdb soullink status <player>` | Check soul link |

## Dependencies

**Required:**
- Fabric Loader ≥0.18.2
- Fabric API

**Optional (Recommended):**
- Cloth Config - Enhanced config screen
- ModMenu - Easy config access

## Version Compatibility

| Minecraft | Fabric API | Cloth Config | ModMenu |
|-----------|------------|--------------|---------|
| **1.21.11** | ≥0.139.5 | ≥21.11.150 | ≥17.0.0 |
| 1.21.10 | ≥0.138.3 | ≥20.0.149 | ≥16.0.0 |
| 1.21.9 | ≥0.134.0 | ≥20.0.149 | ≥16.0.0 |

## Author's Note

