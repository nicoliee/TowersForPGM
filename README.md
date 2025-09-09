# TowersForPGM
![PGM](https://img.shields.io/badge/requires-PGM-orange)

**TowersForPGM** is a comprehensive Minecraft plugin designed specifically for **PGM** servers that enhances competitive gameplay with advanced features for tournaments, drafts, ranked matches, and match statistics.

## 🎯 Map Requirements

This plugin has specific map requirements:

- ✅ **Exactly 2 teams** per map
- ✅ Teams must be named **"red"** and **"blue"**

## 🚀 Key Features

### 🎭 Draft System
Complete captain-based draft system with intelligent player suggestions and customizable picking orders.

### ⏱️ Preparation Time
Configurable preparation phases with protected regions, timers, and potion effects to ensure fair match starts.

### 🏆 Ranked Matches
Full ranked matchmaking system with ELO ratings, queue management, and automatic match creation.

### 📊 Statistics Tracking
Comprehensive player statistics with support for multiple tournaments and leaderboards.

### 🔄 Auto-Refill System
Automatic inventory refilling system for seamless gameplay experience.

### 🤖 Discord Integration
Integration with MatchBot for Discord notifications and player management.

## 📋 Requirements

- **PGM Plugin** (Required) - Core functionality dependency
- **MatchBot Plugin** (Optional) - For Discord integration features
- **MySQL Database** (Optional) - Required only for ranked matches and statistics

> **Note:** The database is completely optional. Without it, you can still use the draft system, preparation time, and other features. However, **ranked matches and statistics tracking require a database connection**.

## 🛠️ Installation

1. Download the latest release of TowersForPGM
2. Place the JAR file in your server's `plugins` folder
3. Ensure PGM is installed and running
4. Start your server to generate configuration files
5. Configure the database settings in `config.yml` (if using ranked features)
6. Restart the server

## ⚙️ Configuration

### Database Setup (Optional)
```yaml
database:
  enabled: false  # Set to true for ranked features
  host: "localhost"
  port: 3306
  name: "your_database"
  user: "your_username"
  password: "your_password"
```

### Draft Configuration
```yaml
draft:
  suggestions: true         # Captain suggestions
  timer: true              # Pick timer
  secondPickBalance: true  # Balance for odd drafts
  order: "ABBAAB"         # Pick order pattern
  minOrder: 8             # Minimum players for order
```

### Ranked System
```yaml
rankeds:
  size: 8                 # Players per ranked match
  order: "ABBAAB"        # Draft order for rankeds
  tables: ["RankedT1"]   # Database tables
  maps: ["Mini Towers:TE"] # Available maps
```

## 🎮 Commands

### Core Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/captains <nick1> <nick2>` | Start draft with two captains | `towers.captains` |
| `/add <player>` | Add player to draft pool | `towers.captains` |
| `/remove <player>` | Remove player from draft pool | `towers.captains` |
| `/pick <player>` | Pick a player during draft | Default |
| `/ready` | Mark yourself as ready | Default |

### Ranked System

| Command | Description | Permission |
|---------|-------------|------------|
| `/ranked join` | Join ranked queue | Default |
| `/ranked leave` | Leave ranked queue | Default |
| `/ranked list` | Show queue status | Default |
| `/elo [player]` | Show ELO rating | Default |
| `/forfeit` | Forfeit current match | Default |

### Statistics

| Command | Description | Permission |
|---------|-------------|------------|
| `/stat [player] [table]` | Show player statistics | `towersstats.use` |
| `/top <category> [amount] [table]` | Show leaderboards | `towersstats.use` |

### Administration

| Command | Description | Permission |
|---------|-------------|------------|
| `/towers` | Main admin command | `towers.admin` |
| `/preparationTime <on\|off>` | Toggle preparation time | `towers.admin` |
| `/cancelMatch` | Cancel current match | `towers.admin` |
| `/tag` | Tag players for Discord | `towers.admin` |

### Advanced Configuration

Use `/towers help` for detailed configuration options:
- `/towers draft` - Draft system settings
- `/towers preparation` - Preparation time configuration  
- `/towers stats` - Statistics and table management

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `towers.admin` | Full administrative access | `op` |
| `towers.captains` | Draft management capabilities | `op` |
| `towers.developer` | Debug message access | `op` |
| `towersstats.use` | View player statistics | `true` |

## 🎯 Usage Examples

### Starting a Draft
```
/captains Player1 Player2
/add Player3
/add Player4
/add Player5
/add Player6
# Captains will then use /pick to select their teams
```

### Joining Ranked Queue
```
/ranked join
# Wait for 8 players, then automatic draft begins
```

### Viewing Statistics
```
/stat Player1           # Show Player1's stats
/top kills 10 TorneoT1  # Page 10 of top killers in TorneoT1 table
/elo Player1            # Show Player1's ELO rating
```

## 🤝 Integration

### MatchBot Integration
Configure Discord integration in `config.yml`:
```yaml
matchbot:
  discordChannel: "your-channel-id"
  rankedRoleId: "your-role-id"
```

### Database Tables
The plugin automatically manages database tables for different tournaments and contexts. Tables can be created and managed through in-game commands.
