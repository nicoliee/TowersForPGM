# MatchBot System - TowersForPGM

> [!WARNING]  
> **Prior configuration required**: This system is optional and requires that you have MatchBot configured and working correctly

> [!NOTE]  
> **Configuration access**: You will only be able to access these configurations once the plugin has detected MatchBot working

---

## Configuration in `matchbot.yml`

```yaml
tables:
  - Amistoso
  - TorneoT1
  - RankedT1
discordChannel: ""  # Discord channel where messages will be sent
rankedRoleId: ""    # Role ID to tag when using /tag
```

### Configuration description

| Option | Type | Description |
|--------|------|-------------|
| `tables` | `array` | List of statistics tables available for consultation |
| `discordChannel` | `string` | Discord channel ID where messages will be sent |
| `rankedRoleId` | `string` | Discord role ID to mention in ranked matches |

---

## Ranked System

### Notification Command
```
/tag
```

### Operation
The `/tag` command works when:
- Current map belongs to the list of maps enabled for ranked
- Sends an embed to Discord mentioning the role configured in `rankedRoleId`
- Notifies about an available ranked match

> [!NOTE]  
> **Requirements to work**: The `/tag` command will not work unless you have these 3 requirements configured: **valid map** (must be in the ranked maps list), **configured role** (`rankedRoleId` with valid ID) and **configured channel** (`discordChannel` configured correctly).

---

## Discord Commands

### Available Commands

| Command | Description | Parameters |
|---------|-------------|------------|
| `/stats <nick> [table]` | **Check statistics** | `nick`: Player name<br>`table`: Specific table (optional) |
| `/top <stat> [table]` | **View ranking** | `stat`: Statistic to check<br>`table`: Specific table (optional) |

---

## Discord Configuration
Before starting in discord do the following:
Settings -> Advanced -> Verify that developer mode is enabled
1. **Channel**: Use developer mode to copy channel ID
2. **Role**: Copy ID of the role that will be mentioned in rankeds
---

## Troubleshooting

| Problem | Possible Cause | Solution |
|----------|---------------|----------|
| **Configuration doesn't appear** | MatchBot not detected | Verify MatchBot installation and operation |
| **Command /tag doesn't work** | Map is not ranked | Verify ranked maps list |
| **Messages don't arrive** | Channel misconfigured | Verify channel ID and bot permissions |
| **Role not mentioned** | Incorrect role ID | Verify role ID and permissions |

---

## Related Links

- 📊 [Statistics System](Stats.md)
- ⚔️ [Draft System](Draft.md)
- ⏱️ [Preparation Time](Preparation%20Time.md)
- 🔄 [Refill System](Refill.md)

