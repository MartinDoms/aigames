-- Initial schema for AI Games platform

-- Games table: stores information about each game type
CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Game sessions table: stores individual game sessions/attempts
CREATE TABLE game_sessions (
    id SERIAL PRIMARY KEY,
    game_id INTEGER NOT NULL REFERENCES games(id),
    player_name VARCHAR(255),
    score INTEGER DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    session_data JSONB, -- Store game-specific data (current state, answers, etc.)
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_game_sessions_game_id FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Daily challenges table: for games that have daily elements
CREATE TABLE daily_challenges (
    id SERIAL PRIMARY KEY,
    game_id INTEGER NOT NULL REFERENCES games(id),
    challenge_date DATE NOT NULL,
    challenge_data JSONB NOT NULL, -- Store the daily puzzle/challenge
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, challenge_date),
    CONSTRAINT fk_daily_challenges_game_id FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Daily results table: track user performance on daily challenges
CREATE TABLE daily_results (
    id SERIAL PRIMARY KEY,
    daily_challenge_id INTEGER NOT NULL REFERENCES daily_challenges(id),
    player_name VARCHAR(255),
    score INTEGER DEFAULT 0,
    attempts INTEGER DEFAULT 1,
    is_solved BOOLEAN NOT NULL DEFAULT false,
    result_data JSONB, -- Store attempt details
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_results_challenge_id FOREIGN KEY (daily_challenge_id) REFERENCES daily_challenges(id)
);

-- Create indexes for better performance
CREATE INDEX idx_game_sessions_game_id ON game_sessions(game_id);
CREATE INDEX idx_game_sessions_started_at ON game_sessions(started_at);
CREATE INDEX idx_daily_challenges_game_date ON daily_challenges(game_id, challenge_date);
CREATE INDEX idx_daily_results_challenge_id ON daily_results(daily_challenge_id);
CREATE INDEX idx_daily_results_completed_at ON daily_results(completed_at);

-- Insert initial game types
INSERT INTO games (name, display_name, description, category) VALUES 
    ('word-ladder', 'Word Ladder', 'Transform one word into another by changing one letter at a time', 'word'),
    ('number-sequence', 'Number Sequence', 'Find the pattern and complete the number sequence', 'number'),
    ('geography-quiz', 'Geography Quiz', 'Test your knowledge of world geography', 'geography');