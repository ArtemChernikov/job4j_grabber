CREATE TABLE IF NOT EXISTS posts(
id serial PRIMARY KEY,
name text,
text text,
link text UNIQUE,
created timestamp
);