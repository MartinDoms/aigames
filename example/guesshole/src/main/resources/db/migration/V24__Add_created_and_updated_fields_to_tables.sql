-- First, create the function to automatically update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Add columns to game_instance
ALTER TABLE game_instance
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to game_instance
DROP TRIGGER IF EXISTS update_game_instance_updated_at ON game_instance;
CREATE TRIGGER update_game_instance_updated_at
    BEFORE UPDATE ON game_instance
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add columns to game_instance_player
ALTER TABLE game_instance_player
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to game_instance_player
DROP TRIGGER IF EXISTS update_game_instance_player_updated_at ON game_instance_player;
CREATE TRIGGER update_game_instance_player_updated_at
    BEFORE UPDATE ON game_instance_player
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add columns to game_state
ALTER TABLE game_state
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to game_state
DROP TRIGGER IF EXISTS update_game_state_updated_at ON game_state;
CREATE TRIGGER update_game_state_updated_at
    BEFORE UPDATE ON game_state
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add only updated_at to lobbies (already has created_at)
ALTER TABLE lobbies
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to lobbies
DROP TRIGGER IF EXISTS update_lobbies_updated_at ON lobbies;
CREATE TRIGGER update_lobbies_updated_at
    BEFORE UPDATE ON lobbies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add columns to players
ALTER TABLE players
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to players
DROP TRIGGER IF EXISTS update_players_updated_at ON players;
CREATE TRIGGER update_players_updated_at
    BEFORE UPDATE ON players
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add columns to round
ALTER TABLE round
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

-- Add trigger to round
DROP TRIGGER IF EXISTS update_round_updated_at ON round;
CREATE TRIGGER update_round_updated_at
    BEFORE UPDATE ON round
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();