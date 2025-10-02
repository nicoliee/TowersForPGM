# ⏱️ Preparation Time - TowersForPGM

Preparation time system where if you define a region, they won't be able to build or remove blocks over that region.

**Effects during preparation**: Regeneration, haste 1 are granted and a message in the hotbar shows remaining time.

> [!IMPORTANT]  
> **Automatic start**: Preparation time starts automatically when a match begins

---

## Configuration in `config.yml`

```yaml
# Preparation time configuration
preparationTime:
  enabled: true
  maps:
    Mini Towers:TE:   # Map name
      P1: -9, 0, -16  # Minimum coordinate of protected region
      P2: 37, 45, 20  # Maximum coordinate of protected region
      Timer: 4        # Preparation time in minutes
      Haste: 3        # Haste time in minutes
```

### Configuration description

| Option | Type | Description |
|--------|------|-------------|
| `preparationTime` | `object` | Main system configuration |
| `├── enabled` | `true/false` | Activates or deactivates preparation time system when starting match |
| `└── maps` | `object` | List of maps configured for preparation time |
| `    └── [Map Name]` | `String` | Specific map configuration (e.g.: "Mini Towers:TE") |
| `        ├── P1` | `x, y, z` | Minimum coordinate of protected region (x, y, z) |
| `        ├── P2` | `x, y, z` | Maximum coordinate of protected region (x, y, z) |
| `        ├── Timer` | `number` | Preparation time duration in minutes |
| `        └── Haste` | `number` | Speed effect duration in minutes |

---

## Available Commands

### Required Permissions
> [!IMPORTANT]  
> **Required permission**: `towers.admin / op` - Only administrators can configure preparation time

### Base Command
```
/towers preparation
```

### Arguments and Subcommands
### Maps
| Command | Description | Usage |
|---------|-------------|-------|
| `add` | **Add map** | Adds a map with default configuration |
| `remove` | **Remove map** | Removes the map from preparation list |

**Usage:**
```
/towers preparation add
/towers preparation remove
```

---

### Region Configuration

> **Note:** The region defines the area where you **CANNOT** build during preparation time.

#### Region Commands

| Command | Description | Format |
|---------|-------------|---------|
| `min <x> <y> <z>` | **Minimum coordinates** | Defines the lower point of the region |
| `max <x> <y> <z>` | **Maximum coordinates** | Defines the upper point of the region |

**Example:**
```
/towers preparation min -10 0 -20
/towers preparation max 40 50 25
```

> [!WARNING]  
> **Limitation**: This configuration only handles **one region** per map. For complex maps requiring multiple protected regions, consider using the map's XML configuration.

---

### Timer Configuration

| Command | Description | Restrictions |
|---------|-------------|---------------|
| `timer <num>` | **Preparation time** | Duration in minutes |
| `haste <num>` | **Haste time** | Speed effect duration |

**Examples:**
```
/towers preparation timer 5
/towers preparation haste 3
```

## Active Effects During Preparation

| Effect | Duration | Description |
|--------|----------|-------------|
| **Regeneration** | Entire preparation time | Automatic health regeneration |
| **Haste** | Configurable | Mining speed effect |
| **Notification** | Entire time | Hotbar message with remaining time |

---

## 🔗 Related Links

- ⚔️ [Draft System](Draft.md)
- 🤖 [MatchBot System](Matchbot.md)
- 🔄 [Refill System](Refill.md)
- 📈 [Statistics](Stats.md)