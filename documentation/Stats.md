# Statistics System - TowersForPGM

> [!IMPORTANT]  
> **Configurable database**: Supports MySQL and SQLite according to configuration. **Required permission**: `towers.admin`

> [!NOTE]  
> **Dynamic tables**: Create multiple tables for different types of matches, assign by map

> [!TIP]  
> 🤖 **MatchBot Integration**: Additional `/stats` and `/top` commands in Discord
---
## Configuration

### Initial
1. **Database**: Configure MySQL/SQLite in `config.yml`
2. **Tables**: `/towers stats add <table>` for each type
3. **Default**: `/towers stats default <table>`

### By Maps
1. Go to map → `/towers stats addmap <table>`

### Temporary  
1. `/towers stats addtemporary <table>` → `/towers stats removetemporary`

> [!TIP]
> **Priority**: Temporary → Map → Default

> [!NOTE]  
> **Configuration finished**: Following these steps you would already have everything configured to send statistics to the corresponding table depending on the map, but if you want to see the yml configuration it will be below


---
## Configuration `config.yml`

```yaml
database:
  enabled: false        # true=MySQL | false=SQLite
  host: "localhost"     # MySQL host
  port: 3306           # MySQL port  
  name: "torneodb"     # Database name
  user: "root"         # MySQL user
  password: "password" # MySQL password
  tables: [Amistoso, TorneoT1]  # Available tables
  defaultTable: Amistoso        # Default table
```

---

## Commands `/towers stats`

### General Management
| Command | Function |
|---------|---------|
| `list` | List all available tables |

### Tables
| Command | Function |
|---------|---------|
| `add <table>` | Create new table |
| `remove <table>` | Delete existing table |
| `default <table>` | Set default table |

### Maps
| Command | Function |
|---------|---------|
| `addmap <table>` | Assign table to current map |
| `removemap` | Remove map assignment |

### Temporary
| Command | Function |
|---------|---------|
| `addtemporary <table>` | Temporary table for session |
| `removetemporary` | Remove temporary configuration |

---
## Related Links

- 📝 [Draft System](Draft.md)
- ⏱️ [Preparation Time](Preparation%20Time.md)
- 🔄 [Refill System](Refill.md)
- 🤖 [MatchBot System](Matchbot.md)
