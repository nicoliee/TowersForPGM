# Refill System - TowersForPGM

> [!IMPORTANT]  
> **Configuration without match**: It's important to configure chests while there is NO match in progress

> [!WARNING]  
> **Apply changes**: When finished configuring chests, you must cycle to the same map again to apply the effects

---

## System Operation

The refill system **automatically refills chests every 60 seconds** starting when the match begins. Chests are refilled with the content they had before the match started.

---

## Available Commands

### Required Permissions
> [!IMPORTANT]  
> **Required permission**: `towers.admin` or **OP** - Only administrators can configure the refill system

### Base Command
```
/towers refill
```

### Arguments and Subcommands

| Command | Description | Usage |
|---------|-------------|-------|
| `add` | **Add chest** | Adds a chest to the refill chest list |
| `remove` | **Remove chest** | Removes the chest from the refill list |

**Usage:**
```
/towers refill add
/towers refill remove
```

---

## Configuration Process

### Step by Step

1. **Position yourself**: Stand on the chest you want to refill
2. **Execute command**: Use `/towers refill add` to add it to the list
3. **Repeat**: Repeat the process for all necessary chests
4. **Apply changes**: Cycle to the same map again for changes to take effect

### Special Considerations

| Situation | Required Action |
|-----------|------------------|
| **Double chests** | You must execute the command for both blocks of the double chest |
| **Remove chest** | Stand on the chest and use `/towers refill remove` |
| **Apply changes** | Mandatory to cycle the map after configuration |


---

## Related Links

- ⚔️ [Draft System](Draft.md)
- ⏱️ [Preparation Time](Preparation%20Time.md)
- 🤖 [MatchBot System](Matchbot.md)
- 📈 [Statistics](Stats.md)