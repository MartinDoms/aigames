-- Repeatable migration to associate round templates with location points
-- Will run every time it changes and after all versioned migrations
-- v5

-- Create a function to get or create a location point
CREATE OR REPLACE FUNCTION get_or_create_location_point(p_longitude numeric, p_latitude numeric)
RETURNS bigint AS $$
DECLARE
    v_location_id bigint;
    v_geom geometry;
    v_admin_level integer;
    v_gid_0 varchar;
    v_name_0 varchar;
    v_type_0 varchar;
    v_gid_1 varchar;
    v_name_1 varchar;
    v_type_1 varchar;
    v_gid_2 varchar;
    v_name_2 varchar;
    v_type_2 varchar;
    v_gid_3 varchar;
    v_name_3 varchar;
    v_type_3 varchar;
    v_gid_4 varchar;
    v_name_4 varchar;
    v_type_4 varchar;
    v_gid_5 varchar;
    v_name_5 varchar;
    v_type_5 varchar;
BEGIN
    -- Check if the point is null
    IF p_longitude IS NULL OR p_latitude IS NULL THEN
        RETURN NULL;
    END IF;

    -- Create a point geometry
    v_geom := ST_SetSRID(ST_Point(p_longitude, p_latitude), 4326);

    -- Check if we already have a location point for these coordinates
    SELECT id INTO v_location_id FROM location_points
    WHERE latitude = p_latitude AND longitude = p_longitude
    LIMIT 1;

    -- If we found an existing location point, return it
    IF v_location_id IS NOT NULL THEN
        RETURN v_location_id;
    END IF;

    -- Try to find administrative info for this point
    -- Start with a CTE to find the most specific admin level
    WITH location AS (
        SELECT
            gb.*,
            CASE
                WHEN gb.name_5 IS NOT NULL AND gb.name_5 <> '' THEN 5
                WHEN gb.name_4 IS NOT NULL AND gb.name_4 <> '' THEN 4
                WHEN gb.name_3 IS NOT NULL AND gb.name_3 <> '' THEN 3
                WHEN gb.name_2 IS NOT NULL AND gb.name_2 <> '' THEN 2
                WHEN gb.name_1 IS NOT NULL AND gb.name_1 <> '' THEN 1
                ELSE 0
            END AS admin_level
        FROM
            gadm_boundaries gb
        WHERE
            ST_Contains(gb.geom, v_geom)
        ORDER BY
            admin_level DESC
        LIMIT 1
    )
    SELECT
        l.admin_level,
        l.gid_0, l.name_0, 'Country',
        l.gid_1, l.name_1, l.engtype_1,
        l.gid_2, l.name_2, l.engtype_2,
        l.gid_3, l.name_3, l.engtype_3,
        l.gid_4, l.name_4, l.engtype_4,
        l.gid_5, l.name_5, l.engtype_5
    INTO
        v_admin_level,
        v_gid_0, v_name_0, v_type_0,
        v_gid_1, v_name_1, v_type_1,
        v_gid_2, v_name_2, v_type_2,
        v_gid_3, v_name_3, v_type_3,
        v_gid_4, v_name_4, v_type_4,
        v_gid_5, v_name_5, v_type_5
    FROM location l;

    -- If no administrative boundary found, set defaults for oceans/uninhabited areas
    IF v_admin_level IS NULL THEN
        v_admin_level := 0;
        v_gid_0 := 'INTL.WATERS';
        v_name_0 := 'International Waters';
        v_type_0 := 'Ocean/Uninhabited';
    END IF;

    -- Insert the new location point
    INSERT INTO location_points (
        latitude, longitude,
        admin0_type, admin0_name, gid0,
        admin1_type, admin1_name, gid1,
        admin2_type, admin2_name, gid2,
        admin3_type, admin3_name, gid3,
        admin4_type, admin4_name, gid4,
        admin5_type, admin5_name, gid5,
        created_at, updated_at
    ) VALUES (
        p_latitude, p_longitude,
        v_type_0, v_name_0, v_gid_0,
        v_type_1, v_name_1, v_gid_1,
        v_type_2, v_name_2, v_gid_2,
        v_type_3, v_name_3, v_gid_3,
        v_type_4, v_name_4, v_gid_4,
        v_type_5, v_name_5, v_gid_5,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
    RETURNING id INTO v_location_id;

    RETURN v_location_id;
END;
$$ LANGUAGE plpgsql;

-- Process all round templates that don't have a location point yet

DO $$
DECLARE
    v_count int := 0;
    v_processed int := 0;
    v_skipped int := 0;
BEGIN
    -- Count how many we need to process
    SELECT COUNT(*) INTO v_count
    FROM round_template
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

    -- Log the start
    RAISE NOTICE 'Starting to process % round templates without location points.', v_count;

    -- Update each round template
    UPDATE round_template
    SET location_point_id = get_or_create_location_point(longitude, latitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

    GET DIAGNOSTICS v_processed = ROW_COUNT;


    -- Log completion
    RAISE NOTICE 'Processed % round templates, 0 were skipped due to null coordinates',
        v_processed;
END $$;

-- Clean up the function when done
DROP FUNCTION IF EXISTS get_or_create_location_point(numeric, numeric);