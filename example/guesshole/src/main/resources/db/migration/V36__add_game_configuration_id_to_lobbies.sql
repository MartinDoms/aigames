ALTER TABLE lobbies ADD COLUMN game_configuration_id UUID;

-- Add a foreign key constraint referencing the location_points table
ALTER TABLE lobbies
    ADD CONSTRAINT fk_lobbies_game_configuration
    FOREIGN KEY (game_configuration_id)
    REFERENCES game_configurations(id);

-- Add an index for faster lookups
CREATE INDEX idx_lobbies_game_configuration_id ON lobbies(game_configuration_id);