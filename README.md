# TowersForPGM
![PGM](https://img.shields.io/badge/requires-PGM-orange)

**TowersForPGM** adds essential competitive features to PGM servers. Main focus: **ranked matches with comprehensive statistics**. Also includes optional per-map features for specific scenarios.

> **Important:** Maps must have exactly **2 teams**

**Background:** Originally developed for "The Towers Champagne" community, inspired by "Godzilla Rankeds" and "Stratus Network" competitive systems.

> [!NOTE]  
> **Getting Started**: Start with `Rankeds` and `Stats` as core features. Other features are optional.

## What This Plugin Adds

- 🏆 **Ranked System + Statistics** - ELO-based ranking with detailed player stats
- 📊 **Advanced Analytics** - Multi-tournament/season tracking with leaderboards
- ⚔️ **Draft System** - Captain-based team selection with balancing
- ⏱️ **Preparation Phase** - Optional pre-match setup with protected regions
- 🔄 **Chest Refills** - Automatic equipment restocking during matches
- 🤖 **Discord Integration** - Match notifications and stats via MatchBot (optional)

---

## Core Features

### Statistics & Ranked System
- **Individual/Team Performance** - Kills, deaths, objectives, win rates, trends
- **ELO Rating** - Skill-based matchmaking for fair games
- **Queue Management** - Automated match creation when players join
- **Tournament Tables** - Separate stats for different competitions/seasons
- **Map Pool Control** - Admin-defined ranked-eligible maps
- **Flexible Team Formation** - Auto-balance or captain-based drafts

### Discord Integration (MatchBot)
- **Live Match Updates** - Auto embeds when ranked matches start/end
- **Discord Commands** - `/stats <player>` and `/top <category>` in Discord
- **Role Notifications** - Tag roles for ranked match announcements
- **Real-time Access** - View stats/leaderboards without joining server

**Setup:** Install MatchBot → TowersForPGM auto-detects → Configure channel/role IDs

## Other Features
- **Draft System** - Captain selection with GUI/suggestions. Auto in ranked, manual with `/captains`
- **Preparation Phase** - Pre-match setup time with protected regions/timers per-map
- **Chest Refills** - 60s auto-restocking. Fast setup: Stand on chests + admin commands to setup

## Commands

| User Type | Commands | Description | Access |
|-----------|----------|-------------|--------|
| **Players** | `/ranked join` | Join ranked queue | When ranked enabled |
| | `/pick [player]` | Draft selection/GUI | During drafts (captains pick) |
| | `/ready` | Ready up faster | During drafts (captains) |
| | `/tag` | Discord notification | Ranked maps |
| | `/forfeit` | Team surrender | Ranked matches |
| **Organizers** | `/captains <nick1> <nick2>` | Start draft | `towers.captains` |
| | `/add/remove <player>` | Manage draft pool | `towers.captains` |
| **Admins** | `/towers` | Configuration access | `towers.admin` |
| | `/cancelMatch` | Cancel match (no stats) | `towers.admin` |

## Setup & Configuration

**Installation:** Install PGM → Add TowersForPGM.jar → Restart → Configure

**Core Setup:**
- **Ranked**: [System Guide](documentation/Rankeds.md) - Competitive matches with ELO
- **Statistics**: [Setup Guide](documentation/Stats.md) - Database and analytics
- **Discord**: [Integration Guide](documentation/Matchbot.md) - MatchBot features

**Optional:** [Draft](documentation/Draft.md) • [Preparation Time](documentation/Preparation%20Time.md) • [Chest Refills](documentation/Refill.md)

---

**This plugin is perfect For:** Competitive communitys • Tournament Organizers.