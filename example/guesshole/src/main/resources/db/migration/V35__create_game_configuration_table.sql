CREATE TABLE game_configurations (
  id uuid DEFAULT public.uuid_generate_v4() PRIMARY KEY,
  game_type character varying(255) NOT NULL,
  num_rounds integer NULL,
  round_length_seconds integer NULL,
  geography_type character varying(255) NULL,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);
