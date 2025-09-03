-- Add location_point_id column to guesses table
ALTER TABLE guesses ADD COLUMN location_point_id BIGINT;

-- Add a foreign key constraint referencing the location_points table
ALTER TABLE guesses
    ADD CONSTRAINT fk_guesses_location_point
    FOREIGN KEY (location_point_id)
    REFERENCES location_points(id);

-- Add an index for faster lookups
CREATE INDEX idx_guesses_location_point_id ON guesses(location_point_id);