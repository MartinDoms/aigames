-- Create sequence for short code generation
CREATE SEQUENCE IF NOT EXISTS lobby_short_code_seq;

-- Add short_code column to lobbies table if it doesn't exist
ALTER TABLE lobbies ADD COLUMN IF NOT EXISTS short_code VARCHAR(10) UNIQUE;
ALTER TABLE lobbies ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Create function to generate obfuscated short codes
CREATE OR REPLACE FUNCTION generate_lobby_short_code()
RETURNS TEXT AS $$
DECLARE
  id INT;
  salt INT := 982451653; -- A large prime number as salt
  short_code TEXT;
  obfuscated_id INT;
BEGIN
  SELECT nextval('lobby_short_code_seq') INTO id;

  -- Obfuscate the sequential ID with XOR and bit shifting for non-linearity
  obfuscated_id := (id # salt) << 5 | (id >> 3);

  -- Convert to base36 (alphanumeric)
  short_code := '';
  WHILE obfuscated_id > 0 LOOP
    short_code := substring('0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ' FROM (obfuscated_id % 36 + 1) FOR 1) || short_code;
    obfuscated_id := obfuscated_id / 36;
  END LOOP;

  -- Ensure minimum length of 6 characters
  WHILE length(short_code) < 6 LOOP
    short_code := '0' || short_code;
  END LOOP;

  RETURN short_code;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically generate short_code on insert if not provided
CREATE OR REPLACE FUNCTION set_lobby_short_code()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.short_code IS NULL THEN
    NEW.short_code := generate_lobby_short_code();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add trigger to lobbies table
DROP TRIGGER IF EXISTS ensure_lobby_short_code ON lobbies;
CREATE TRIGGER ensure_lobby_short_code
BEFORE INSERT ON lobbies
FOR EACH ROW
EXECUTE FUNCTION set_lobby_short_code();

-- Update existing records to have short_codes if they don't already
UPDATE lobbies
SET short_code = generate_lobby_short_code()
WHERE short_code IS NULL;