# TowersForPGM
![PGM](https://img.shields.io/badge/requires-PGM-orange)

**TowersForPGM** is a comprehensive Minecraft plugin designed specifically for **PGM** servers that enhances competitive gameplay with advanced features for tournaments, drafts, ranked matches, and match statistics.

- - -
## 🎯 Map Requirements

This plugin has specific map requirements:

- ✅ **Exactly 2 teams** per map
- ✅ Teams must be named **"red"** and **"blue"**
- - -
## 🚀 Key Features

- **Draft System:** Captain-based drafts with smart player suggestions and flexible pick orders.
- **Preparation Phase:** Customizable pre-game timers, protected regions, and potion effects for fair starts.
- **Ranked Matches:** ELO-based matchmaking, queue management, and automated match setup.
- **Stats Tracking:** Tracks player stats across tournaments with leaderboards.
- **Chest Refills:** Define chests to auto-refill every 60 seconds during matches.
- **Discord Integration:** With MatchBot, get Discord embeds for ranked matches and use `/stats` and `/top` to view stats in Discord.

## 📋 Requirements

- **PGM Plugin** (Required) - Core functionality dependency
- **MatchBot Plugin** (Optional) - For Discord integration features
> **Note:** You can use a MySQL database if you have one available. If not, the plugin will automatically create and use a local SQLite database.

## 🛠️ Installation

1. Download the latest release of TowersForPGM
2. Place the JAR file in your server's `plugins` folder
3. Ensure PGM is installed and running
4. Start your server to generate configuration files
5. Configure the database settings in `config.yml`
6. Restart the server

## ⚙️ Configuration

### Database Setup (Optional)
```yaml
# Configuración de la base de datos
database:
  enabled: false  # (true = MySQL | false = SQLite)
  # Configuración de MySQL
  host: "localhost"
  port: 3306
  name: "torneodb"
  user: "root" 
  password: "password" 
  tables:         # Tablas disponibles en la base de datos
    - Amistoso
    - TorneoT1
  defaultTable: Amistoso  # Tabla por defecto
  matchbot:
    tables: # Tablas que se usarán en los comandos de Discord
      - Amistoso
      - TorneoT1
      - RankedT1
```

### Draft Configuration
```yaml
# Configuración del sistema de draft (selección de jugadores)
draft:
  suggestions: true    # Los capitanes recibirán sugerencias de jugadores para elegir
  timer: true         # Los capitanes tendrán un tiempo límite para elegir
  secondPickBalance: true # Si es true y el draft es impar, el segundo capitán tendrá un jugador más
  order: "ABBAAB"     # Orden de elección: A = Primer Capitán, B = Segundo Capitán
  minOrder: 8              # Número mínimo de jugadores para aplicar el orden
```

### Ranked System
```yaml
# Configuración de Rankeds (Es necesario tener una base de Datos)
rankeds:
  size: 8          # Número de jugadores
  order: "ABBAAB"  # Orden de elección para rankeds
  matchmaking: false # Si es true, los equipos se balancearán automáticamente, si es false, los capitanes elegirán a los jugadores
  tables:              # Tablas disponibles para rankeds
    - RankedT1
  defaultTable: RankedT1  # Tabla por defecto para rankeds
  maps: # Lista de mapas disponibles para rankeds
    - Mini Towers:TE
  matchbot: # Bot que envía mensajes al canal de Discord (es necesario tener el plugin MatchBot configurado)
    discordChannel: "" # Canal de Discord donde se enviarán los mensajes
    rankedRoleId: "" # ID del rol a tagear cunado se use /tag
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
| `/forfeit` | Forfeit current match | Default |
| `/tag` | Tag players for Discord | Default |



### Administration

| Command | Description | Permission |
|---------|-------------|------------|
| `/towers` | Main admin command | `towers.admin` |
| `/cancelMatch` | Cancel current match | `towers.admin` |


> **Note:** You can also use these commands to configure and manage the plugin:
>
> Use `/towers help` to see detailed configuration options:
> - `/towers draft` - Draft system configuration
> - `/towers preparation` - Preparation time configuration  
> - `/towers stats` - Statistics and table management

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `towers.admin` | Full administrative access | `op` |
| `towers.captains` | Draft management capabilities | `op` |
| `towers.developer` | Debug message access | `op` |

## 🤝 MatchBot Integration

## 🤝 MatchBot Integration
MatchBot integration is optional, but it allows you to view your statistics directly on Discord using the `/stats` and `/top` commands.  
Additionally, the plugin can automatically create Discord embeds when ranked matches start and end, showing every tracked stat for every player in the configured channel.

### MatchBot Configuration

- The tables defined in `database.matchbot.tables` are used to display statistics in the `/top` and `/stats` Discord commands. Only data stored in these tables will appear in those commands.

  ```yaml
  database:
    matchbot:
      tables:
        - Amistoso
        - TorneoT1
        - RankedT1
  ```

- The value of `rankeds.matchbot.discordChannel` specifies the Discord channel where embeds will be automatically sent when ranked matches start or end.

  ```yaml
  rankeds:
    matchbot:
      discordChannel: "your-discord-channel-id"
  ```

- The option `rankeds.matchbot.rankedRole` lets you define the Discord role that will be mentioned (tagged) when using the `/tag` command.

  ```yaml
  rankeds:
    matchbot:
      rankedRole: "your-discord-role-id"
  ```

Make sure to configure these values correctly in your `config.yml` file for the integration to work as expected.

### Database Tables
The plugin automatically manages database tables for different tournaments and contexts. Tables can be created and managed through in-game via `/towers` command.
