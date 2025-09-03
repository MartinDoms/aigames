ALTER TABLE round ADD COLUMN location_point_id BIGINT;

-- Add a foreign key constraint referencing the location_points table
ALTER TABLE round
    ADD CONSTRAINT fk_round_location_point
    FOREIGN KEY (location_point_id)
    REFERENCES location_points(id);

-- Add an index for faster lookups
CREATE INDEX idx_round_location_point_id ON round(location_point_id);