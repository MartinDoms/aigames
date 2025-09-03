CREATE TABLE countries (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255),
  code VARCHAR(3),
  geom GEOMETRY(MULTIPOLYGON, 4326)
);

CREATE TABLE cities (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255),
  country_code VARCHAR(3),
  geom GEOMETRY(MULTIPOLYGON, 4326)
);

CREATE INDEX countries_geom_idx ON countries USING GIST(geom);
CREATE INDEX cities_geom_idx ON cities USING GIST(geom);