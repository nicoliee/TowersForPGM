# Draft System - TowersForPGM

> [!IMPORTANT]  
> **Important Notice**: Once a match with draft has started, no one else will be able to join teams (unless an administrator forces someone to a team using the command `/team force <nick> <team>`)

> [!WARNING]  
> **Map Requirement**: The map where a draft is conducted must have 2 teams called "Red" or "Blue" (you can change team names in the map's xml file)
---
# **Draft Process:**
1. 🎲 Captains are selected (random or forced)
2. 🔄 Captains pick by turns according to configured order
3. ✅ When finished, they have 90 seconds to use `/ready`
4. 🚀 The match begins

---

## **Configuration in `config.yml`**


| Option | Type | Description |
|--------|------|-------------|
| `suggestions` | `true/false` | Captains receive player suggestions to pick |
| `timer` | `true/false` | Activates time limit for captains to pick |
| `secondPickBalance` | `true/false` | If draft is odd, second captain will have one more player |
| `order` | `string` | Pick order (A = First Captain, B = Second Captain) |
| `minOrder` | `number` | Minimum number of players to apply specified order |

---

## Available Commands

### Commands for Administrators

#### Prerequisites
- [x] Have **OP** permissions or `towers.captains` permission
- [x] Minimum **4 people** online

#### 🔧 Available commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/captains <Nick1> <Nick2> [force]` | **Start a draft** | Specify 2 online players as captains. Use `force` to maintain given order |
| `/add <nick>` | **Add player** | Add a player to available list (after starting draft) |
| `/del <nick>` | **Remove player** | Remove a player from available list (after starting draft) |


---

### Commands for Captains

#### Available commands

| Command | Description | Details |
|---------|-------------|---------|
| `/pick [nick]` | **Pick player** | Without nick: opens interactive GUI (everyone can use it)<br>With nick: directly picks the player |
| `/ready` | **Confirm ready** | Reduces startup countdown from 90s to 5s if both captains are ready |
---


## 🔗 Related Links

- 🤖 [MatchBot System](Matchbot.md)
- ⏱️ [Preparation Time](Preparation%20Time.md)
- 🔄 [Refill System](Refill.md)
- 📈 [Statistics](Stats.md)
