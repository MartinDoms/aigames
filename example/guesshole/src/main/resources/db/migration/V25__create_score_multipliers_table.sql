-- Alter the guesses table to add base_score
ALTER TABLE guesses
ADD COLUMN base_score INTEGER;

-- Create score_multipliers table for storing badge information
CREATE TABLE score_multipliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guess_id UUID NOT NULL REFERENCES guesses(id) ON DELETE CASCADE,
    multiplier_type VARCHAR(50) NOT NULL,
    multiplier_value FLOAT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    tooltip TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for quick lookup
CREATE INDEX idx_score_multipliers_guess_id ON score_multipliers(guess_id);