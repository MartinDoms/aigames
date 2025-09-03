-- V1_5__Add_Kicked_Flag_To_Players.sql

-- Add kicked column to players table
ALTER TABLE players ADD COLUMN kicked BOOLEAN NOT NULL DEFAULT FALSE;

-- Create an index for faster lookup of players in a lobby that aren't kicked
CREATE INDEX idx_players_lobby_kicked ON players(lobby_id, kicked);

-- Comment explaining the migration
COMMENT ON COLUMN players.kicked IS 'Flag indicating if a player has been kicked from their lobby';