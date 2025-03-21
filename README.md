# TowersForPGM Plugin README

## Description

**TowersForPGM** is a Minecraft plugin that extends the functionality of the **PGM** plugin by adding new features such as:

1. **Draft System**: Allows for a player draft before starting a match, where captains can pick their teams.
2. **Preparation Time**: Provides configurable preparation time with regions, timers, and potion effects.
3. **ScoreBox Statistics**: Tracks player statistics specifically for the **ScoreBox** game mode. (More game modes to be added in the future.) These statistics are stored in a MySQL database.
## Requirements

**TowersForPGM** requires the **PGM** plugin to function.
## Commands

Below is a list of the available commands in **TowersForPGM**:

- **/towersForPGM**: Main command for TowersForPGM. 
- **/captains <nick1> <nick2>**: Starts the draft with two captains.
- **/add <nick>**: Adds a player to the draft pool.
- **/remove <nick>**: Removes a player from the draft pool.
- **/pick <player>**: Picks a player for the draft.
- **/region <add|delete|list|timer|haste> [value]**: Manages regions and timers, including potion effects.
- **/preparationTime <on|off|enable|disable>**: Enables or disables preparation time before a match.
- **/table <add|delete|list> [name]**: Manages tournament tables.
- **/setTable <name>**: Sets the table where statistics are recorded.
- **/stat <table> [player]**: Displays statistics for a specific player or table.
- **/top <category> [amount] [table]**: Shows the best statistics for a player or table.

## Permissions

Here are the available permissions for **TowersForPGM**:

- **towers.admin**: Allows the user to manage and change the plugin's settings. (Default: `op`)
- **towersstats.use**: Allows the user to view player statistics. (Default: `true`)

## Configuration

The plugin offers a variety of configurable options related to the draft system, preparation time, and statistics. Administrators should review and adjust these settings to suit their tournament needs.
