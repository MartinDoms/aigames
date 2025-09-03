-- Drop existing trigger first
DROP TRIGGER IF EXISTS ensure_lobby_short_code ON lobbies;

-- Improve the short code generation function with better randomization
CREATE OR REPLACE FUNCTION generate_lobby_short_code()
RETURNS TEXT AS $$
DECLARE
  id BIGINT;
  salt BIGINT := 982451653; -- A large prime number as salt
  random_part INT;
  short_code TEXT := '';
  obfuscated_id BIGINT;
  char_pos INT;
  chars TEXT := '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  attempts INT := 0;
  max_attempts INT := 10;
  exists_already BOOLEAN;
BEGIN
  -- Get next sequence value
  SELECT nextval('lobby_short_code_seq') INTO id;

  -- Loop to handle the rare case of collisions
  LOOP
    -- Reset short_code for each attempt
    short_code := '';

    -- Get a random value between 0 and 999999
    SELECT floor(random() * 1000000)::int INTO random_part;

    -- Combine sequence ID with random value
    obfuscated_id := (id * 1000000 + random_part);

    -- Additional obfuscation - XOR with salt and bit shifting
    obfuscated_id := (obfuscated_id # salt) # (random_part << 5);

    -- Convert to base36 (alphanumeric)
    WHILE obfuscated_id > 0 LOOP
      char_pos := (obfuscated_id % 36) + 1;
      short_code := substr(chars, char_pos, 1) || short_code;
      obfuscated_id := obfuscated_id / 36;
    END LOOP;

    -- Ensure minimum length of 6 characters with more randomness
    WHILE length(short_code) < 6 LOOP
      char_pos := (floor(random() * 36) + 1)::int;
      short_code := substr(chars, char_pos, 1) || short_code;
    END LOOP;

    -- Check if this short code already exists
    EXECUTE 'SELECT EXISTS(SELECT 1 FROM lobbies WHERE short_code = $1)'
    INTO exists_already
    USING short_code;

    -- Exit loop if unique or too many attempts
    EXIT WHEN NOT exists_already OR attempts >= max_attempts;
    attempts := attempts + 1;
  END LOOP;

  -- If we couldn't find a unique short code after max attempts, add a timestamp to ensure uniqueness
  IF exists_already THEN
    short_code := short_code || substr(to_char(CURRENT_TIMESTAMP, 'SSMS'), 1, 2);
  END IF;

  RETURN short_code;
END;
$$ LANGUAGE plpgsql;

-- Re-create the trigger with the improved function
CREATE OR REPLACE FUNCTION set_lobby_short_code()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.short_code IS NULL THEN
    NEW.short_code := generate_lobby_short_code();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ensure_lobby_short_code
BEFORE INSERT ON lobbies
FOR EACH ROW
EXECUTE FUNCTION set_lobby_short_code();

-- Regenerate all existing short codes to use the improved algorithm
UPDATE lobbies
SET short_code = generate_lobby_short_code()
WHERE short_code IS NOT NULL;

-- Add a unique constraint if it doesn't already exist
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'lobbies_short_code_key' AND conrelid = 'lobbies'::regclass
  ) THEN
    ALTER TABLE lobbies ADD CONSTRAINT lobbies_short_code_key UNIQUE (short_code);
  END IF;
END
$$;