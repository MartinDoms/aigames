-- Add location_point_id column to round_template table
ALTER TABLE round_template ADD COLUMN location_point_id BIGINT;

-- Add a foreign key constraint referencing the location_points table
ALTER TABLE round_template
    ADD CONSTRAINT fk_round_template_location_point
    FOREIGN KEY (location_point_id)
    REFERENCES location_points(id);

-- Add an index for faster lookups
CREATE INDEX idx_round_template_location_point_id ON round_template(location_point_id);