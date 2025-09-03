-- This table stores hierarchical administrative boundary information for geographic points

CREATE TABLE location_points (
    id BIGSERIAL PRIMARY KEY,

    -- Geographic coordinates
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    -- Administrative level 0 (Country)
    admin0_type VARCHAR(50),
    admin0_name VARCHAR(100),
    gid0 VARCHAR(20),

    -- Administrative level 1 (State/Province)
    admin1_type VARCHAR(50),
    admin1_name VARCHAR(100),
    gid1 VARCHAR(20),

    -- Administrative level 2 (County/District)
    admin2_type VARCHAR(50),
    admin2_name VARCHAR(100),
    gid2 VARCHAR(20),

    -- Administrative level 3
    admin3_type VARCHAR(50),
    admin3_name VARCHAR(100),
    gid3 VARCHAR(20),

    -- Administrative level 4
    admin4_type VARCHAR(50),
    admin4_name VARCHAR(100),
    gid4 VARCHAR(20),

    -- Administrative level 5
    admin5_type VARCHAR(50),
    admin5_name VARCHAR(100),
    gid5 VARCHAR(20),

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX idx_location_points_coords ON location_points (latitude, longitude);
CREATE INDEX idx_location_points_admin0 ON location_points (admin0_name);
CREATE INDEX idx_location_points_admin1 ON location_points (admin1_name);
CREATE INDEX idx_location_points_admin2 ON location_points (admin2_name);
CREATE INDEX idx_location_points_gid0 ON location_points (gid0);
CREATE INDEX idx_location_points_gid1 ON location_points (gid1);
CREATE INDEX idx_location_points_gid2 ON location_points (gid2);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create a trigger to automatically update the updated_at column
CREATE TRIGGER update_location_points_modtime
BEFORE UPDATE ON location_points
FOR EACH ROW
EXECUTE FUNCTION update_modified_column();
