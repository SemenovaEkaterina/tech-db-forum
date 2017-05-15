CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS forum_user (
    id SERIAL PRIMARY KEY,
    about character varying NOT NULL,
    email citext NOT NULL UNIQUE,
    fullname character varying NOT NULL,
    nickname citext NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS forum_forum (
    id SERIAL PRIMARY KEY,
    slug citext NOT NULL UNIQUE,
    forum_user integer NOT NULL REFERENCES forum_user(id),
    title character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS forum_thread (
    author integer NOT NULL REFERENCES forum_user(id),
    created timestamp with time zone,
    forum integer NOT NULL REFERENCES forum_forum(id),
    id SERIAL PRIMARY KEY,
    message character varying NOT NULL,
    title character varying NOT NULL,
    slug citext UNIQUE
);

CREATE TABLE IF NOT EXISTS forum_vote (
    nickname integer NOT NULL REFERENCES forum_user(id),
    voice integer NOT NULL,
    thread integer NOT NULL REFERENCES forum_thread(id),
    CONSTRAINT unique_forum_vote UNIQUE(nickname, thread)
);

CREATE TABLE IF NOT EXISTS forum_post (
    id SERIAL PRIMARY KEY,
    author integer NOT NULL REFERENCES forum_user(id),
    created timestamp with time zone,
    forum integer NOT NULL REFERENCES forum_forum(id),
    isedited boolean DEFAULT false NOT NULL,
    message character varying NOT NULL,
    parent integer DEFAULT 0 REFERENCES forum_post(id),
    thread integer NOT NULL REFERENCES forum_thread(id)
);

CREATE INDEX IF NOT EXISTS idx_forum_thread_id ON forum_thread(id);
CREATE INDEX IF NOT EXISTS idx_forum_forum_id_slug ON forum_forum(id, citext(slug));
