# TowersForPGM

## Description

**TowersForPGM** is a Minecraft plugin that extends the functionality of the **PGM** plugin by adding new competitive and administrative features, such as:

1. **Draft System**: Allows for a player draft before starting a match, where captains can pick their teams.
2. **Preparation Time**: Provides configurable preparation time with region boundaries, timers, and potion effects.
3. **ScoreBox Statistics**: Tracks player statistics specifically for the **ScoreBox** game mode, with data stored in a MySQL database. (More game modes will be added in the future.)

## Requirements

- **PGM** plugin is required for **TowersForPGM** to function properly.

## Commands

Below is a list of available commands in **TowersForPGM**:

### Main Command

- **/towers**: Central command for managing the draft, preparation, and statistics systems.

  - **/towers draft suggestions**
  - **/towers draft timer**
  - **/towers draft private <true|false>**
  
  - **/towers preparation add**
  - **/towers preparation remove**
  - **/towers preparation max <x> <y> <z>**
  - **/towers preparation min <x> <y> <z>**
  - **/towers preparation timer <minutes>**
  - **/towers preparation haste <minutes>**
  - **/towers preparation list**
  
  - **/towers stats toggle**
  - **/towers stats default <table>**
  - **/towers stats add <table>**
  - **/towers stats remove <table>**
  - **/towers stats list**
  - **/towers stats addMap <table>**
  - **/towers stats removeMap <table>**
  
  - **/towers help <draft|preparation|stats>**: Shows help information for subcommands.

### Other Commands

- **/captains <nick1> <nick2>**: Starts a draft session with two captains.
- **/add <nick>**: Adds a player to the draft pool.
- **/remove <nick>**: Removes a player from the draft pool.
- **/pick <player>**: Picks a player during the draft.
- **/region <add|delete|list|timer|haste> [value]**: Manages draft/preparation regions and related timers/effects.
- **/preparationTime <on|off|enable|disable>**: Enables or disables preparation time before a match.
- **/table <add|delete|list> [name]**: Manages tournament tables.
- **/setTable <name>**: Sets the active table for tracking statistics.
- **/stat <table> [player]**: Displays statistics for a specific player or table.
- **/top <category> [amount] [table]**: Shows leaderboard-style statistics.

## Permissions

| Permission             | Description                                          | Default |
|------------------------|------------------------------------------------------|---------|
| `towers.admin`         | Full access to all plugin configuration and commands | `op`    |
| `towersstats.use`      | Allows viewing of player statistics                  | `true`  |

## Configuration

All configuration is handled via the `/towers` command system. Use `/towers help` to view available options. There is **no longer a separate `/config` command**—all settings are now dynamically managed via in-game commands for ease of use during live events or setup.
