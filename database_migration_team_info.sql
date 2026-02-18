-- Migration script to add team information and enhanced player statistics to match history
-- This script is safe to run multiple times (uses IF NOT EXISTS where supported)

-- ==================== MYSQL MIGRATION ====================
-- Step 1: Rename old columns to new naming convention (if they exist)
-- Uncomment and run these if you have the old camelCase columns:
ALTER TABLE match_players_history CHANGE COLUMN damageDone damage_done DOUBLE DEFAULT 0;
ALTER TABLE match_players_history CHANGE COLUMN damageTaken damage_taken DOUBLE DEFAULT 0;

-- Step 2: Add all new columns to match_players_history
ALTER TABLE match_players_history 
-- Team information
ADD COLUMN IF NOT EXISTS team_name VARCHAR(64),
ADD COLUMN IF NOT EXISTS team_color_hex VARCHAR(16),
ADD COLUMN IF NOT EXISTS team_score INT,

-- ELO tracking
ADD COLUMN IF NOT EXISTS elo_before INT,

-- K/D stats
ADD COLUMN IF NOT EXISTS killstreak INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS max_killstreak INT DEFAULT 0,

-- Bow stats
ADD COLUMN IF NOT EXISTS longest_bow_kill INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS bow_damage DOUBLE DEFAULT 0,
ADD COLUMN IF NOT EXISTS bow_damage_taken DOUBLE DEFAULT 0,
ADD COLUMN IF NOT EXISTS shots_taken INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS shots_hit INT DEFAULT 0,

-- Damage stats (only add if you didn't rename above)
ADD COLUMN IF NOT EXISTS damage_done DOUBLE DEFAULT 0,
ADD COLUMN IF NOT EXISTS damage_taken DOUBLE DEFAULT 0,

-- Objective stats
ADD COLUMN IF NOT EXISTS destroyable_pieces_broken INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS monuments_destroyed INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS flags_captured INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS flag_pickups INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS cores_leaked INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS wools_captured INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS wools_touched INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS longest_flag_hold_millis BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;


-- ==================== SQLITE MIGRATION ====================
-- SQLite doesn't support RENAME COLUMN until SQLite 3.25.0, so we need to migrate data manually

-- Step 1: Check if old columns exist and create new table with correct schema
-- IMPORTANT: Backup your database before running this migration!

-- Create new table with all correct columns
CREATE TABLE IF NOT EXISTS match_players_history_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id TEXT NOT NULL,
    username TEXT NOT NULL,
    -- K/D stats
    kills INTEGER DEFAULT 0,
    deaths INTEGER DEFAULT 0,
    assists INTEGER DEFAULT 0,
    killstreak INTEGER DEFAULT 0,
    max_killstreak INTEGER DEFAULT 0,
    -- Bow stats
    longest_bow_kill INTEGER DEFAULT 0,
    bow_damage REAL DEFAULT 0,
    bow_damage_taken REAL DEFAULT 0,
    shots_taken INTEGER DEFAULT 0,
    shots_hit INTEGER DEFAULT 0,
    -- Damage stats (NEW NAMES)
    damage_done REAL DEFAULT 0,
    damage_taken REAL DEFAULT 0,
    -- Objective stats
    destroyable_pieces_broken INTEGER DEFAULT 0,
    monuments_destroyed INTEGER DEFAULT 0,
    flags_captured INTEGER DEFAULT 0,
    flag_pickups INTEGER DEFAULT 0,
    cores_leaked INTEGER DEFAULT 0,
    wools_captured INTEGER DEFAULT 0,
    wools_touched INTEGER DEFAULT 0,
    longest_flag_hold_millis INTEGER DEFAULT 0,
    points INTEGER DEFAULT 0,
    -- Match info
    win INTEGER DEFAULT 0,
    game INTEGER DEFAULT 1,
    winstreak_delta INTEGER DEFAULT 0,
    elo_delta INTEGER DEFAULT 0,
    maxElo_after INTEGER DEFAULT 0,
    -- Team info
    team_name TEXT,
    team_color_hex TEXT,
    team_score INTEGER,
    elo_before INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches_history(match_id) ON DELETE CASCADE
);

-- Step 2: Copy data from old table to new table
-- This handles both old column names (damageDone/damageTaken) and new ones (damage_done/damage_taken)
INSERT INTO match_players_history_new (
    id, match_id, username, kills, deaths, assists,
    damage_done, damage_taken, points, win, game,
    winstreak_delta, elo_delta, maxElo_after
)
SELECT 
    id, match_id, username, kills, deaths, assists,
    COALESCE(damageDone, damage_done, 0) as damage_done,
    COALESCE(damageTaken, damage_taken, 0) as damage_taken,
    points, win, game, winstreak_delta, elo_delta, maxElo_after
FROM match_players_history;

-- Step 3: Drop old table and rename new table
DROP TABLE match_players_history;
ALTER TABLE match_players_history_new RENAME TO match_players_history;

-- Step 4: Recreate indexes
CREATE INDEX IF NOT EXISTS idx_match_players_match ON match_players_history(match_id);
CREATE INDEX IF NOT EXISTS idx_match_players_user ON match_players_history(username);
CREATE INDEX IF NOT EXISTS idx_team_name ON match_players_history(team_name);
CREATE INDEX IF NOT EXISTS idx_elo_before ON match_players_history(elo_before);

-- ELO tracking
-- ALTER TABLE match_players_history ADD COLUMN elo_before INTEGER;

-- K/D stats
-- ALTER TABLE match_players_history ADD COLUMN killstreak INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN max_killstreak INTEGER DEFAULT 0;

-- Bow stats
-- ALTER TABLE match_players_history ADD COLUMN longest_bow_kill INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN bow_damage REAL DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN bow_damage_taken REAL DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN shots_taken INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN shots_hit INTEGER DEFAULT 0;

-- Damage stats (si tienes damageDone/damageTaken, SQLite no soporta RENAME, deberás recrear la tabla)
-- ALTER TABLE match_players_history ADD COLUMN damage_done REAL DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN damage_taken REAL DEFAULT 0;

-- Objective stats
-- ALTER TABLE match_players_history ADD COLUMN destroyable_pieces_broken INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN monuments_destroyed INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN flags_captured INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN flag_pickups INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN cores_leaked INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN wools_captured INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN wools_touched INTEGER DEFAULT 0;
-- ALTER TABLE match_players_history ADD COLUMN longest_flag_hold_millis INTEGER DEFAULT 0;

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_team_name ON match_players_history(team_name);
CREATE INDEX IF NOT EXISTS idx_elo_before ON match_players_history(elo_before);
CREATE INDEX IF NOT EXISTS idx_match_team ON match_players_history(match_id, team_name);
