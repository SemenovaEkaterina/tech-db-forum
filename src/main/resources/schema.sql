CREATE EXTENSION IF NOT EXISTS citext;
SET synchronous_commit TO OFF;

/*DROP TABLE IF EXISTS forum_user CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_user (
    id SERIAL PRIMARY KEY,
    about character varying NOT NULL,
    email citext NOT NULL UNIQUE,
    fullname character varying NOT NULL,
    nickname citext NOT NULL UNIQUE COLLATE "C"
);

/*DROP TABLE IF EXISTS forum_forum CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_forum (
    id SERIAL PRIMARY KEY,
    slug citext NOT NULL UNIQUE,
    forum_user integer NOT NULL REFERENCES forum_user(id),
    title character varying NOT NULL,
    threads integer DEFAULT 0,
    posts integer DEFAULT 0
);

/*DROP TABLE IF EXISTS forum_thread CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_thread (
    author integer NOT NULL REFERENCES forum_user(id),
    created timestamp with time zone,
    forum integer NOT NULL REFERENCES forum_forum(id),
    id SERIAL PRIMARY KEY,
    message character varying NOT NULL,
    title character varying NOT NULL,
    slug citext UNIQUE,
    votes integer DEFAULT 0
);

/*DROP TABLE IF EXISTS forum_vote CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_vote (
    nickname integer NOT NULL REFERENCES forum_user(id),
    voice integer NOT NULL,
    thread integer NOT NULL REFERENCES forum_thread(id),
    CONSTRAINT unique_forum_vote UNIQUE(nickname, thread)
);

/*DROP TABLE IF EXISTS forum_post CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_post (
    id SERIAL PRIMARY KEY,
    author integer NOT NULL REFERENCES forum_user(id),
    created timestamp with time zone,
    forum integer NOT NULL REFERENCES forum_forum(id),
    isedited boolean DEFAULT false NOT NULL,
    message character varying NOT NULL,
    parent integer DEFAULT 0 REFERENCES forum_post(id),
    path integer[],
    thread integer NOT NULL REFERENCES forum_thread(id)
);

/*DROP TABLE IF EXISTS forum_f_u CASCADE;*/
CREATE TABLE IF NOT EXISTS forum_f_u (
    forum_id integer NOT NULL REFERENCES forum_forum(id),
    user_id integer NOT NULL REFERENCES forum_user(id),
    CONSTRAINT unique_forum_user UNIQUE(forum_id, user_id)
);

CREATE OR REPLACE FUNCTION new_thread() RETURNS TRIGGER AS '
    BEGIN
    UPDATE forum_forum SET threads = threads + 1 WHERE NEW.forum=forum_forum.id;
    INSERT INTO forum_f_u (forum_id, user_id) VALUES (NEW.forum, NEW.author) ON CONFLICT (forum_id, user_id) DO NOTHING;
    RETURN NEW;
    END
    ' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION new_post() RETURNS TRIGGER AS '
    BEGIN
    UPDATE forum_forum SET posts = posts + 1 WHERE NEW.forum=forum_forum.id;
    INSERT INTO forum_f_u (forum_id, user_id) VALUES (NEW.forum, NEW.author) ON CONFLICT (forum_id, user_id) DO NOTHING;
    RETURN NEW;
    END
    ' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION new_vote() RETURNS TRIGGER AS '
    BEGIN
    UPDATE forum_thread SET votes = votes + NEW.voice WHERE NEW.thread=forum_thread.id;
    RETURN NEW;
    END
    ' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_vote() RETURNS TRIGGER AS '
    BEGIN
    UPDATE forum_thread SET votes = votes - OLD.voice + NEW.voice WHERE NEW.thread=forum_thread.id;
    RETURN NEW;
    END
    ' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_stat_th ON forum_thread;
DROP TRIGGER IF EXISTS update_stat_p ON forum_post;
DROP TRIGGER IF EXISTS update_stat_v ON forum_vote;
DROP TRIGGER IF EXISTS update_stat_v_u ON forum_vote;

CREATE TRIGGER update_stat_th
    AFTER INSERT ON forum_thread
    FOR EACH ROW
    EXECUTE PROCEDURE new_thread();

CREATE TRIGGER update_stat_p
    AFTER INSERT ON forum_post
    FOR EACH ROW
    EXECUTE PROCEDURE new_post();

CREATE TRIGGER update_stat_v
    AFTER INSERT ON forum_vote
    FOR EACH ROW
    EXECUTE PROCEDURE new_vote();

CREATE TRIGGER update_stat_v_u
    AFTER UPDATE ON forum_vote
    FOR EACH ROW
    EXECUTE PROCEDURE update_vote();

CREATE INDEX IF NOT EXISTS idx_forum_thread_id_forum ON forum_thread(id);
CREATE INDEX IF NOT EXISTS idx_forum_user_nickname_id ON forum_user(citext(nickname), id);
CREATE INDEX IF NOT EXISTS idx_forum_vote_thread_voice ON forum_vote(thread, voice);
CREATE INDEX IF NOT EXISTS idx_forum_user_id_nickname ON forum_user(id, citext(nickname));


CREATE INDEX IF NOT EXISTS idx_forum_forum_id_slug ON forum_forum(id, citext(slug));
CREATE INDEX IF NOT EXISTS idx_forum_post_thread_author ON forum_post(thread, author);

CREATE INDEX IF NOT EXISTS idx_forum_thread_author_forum ON forum_thread(author, forum);

CREATE INDEX IF NOT EXISTS idx_forum_post_path1 ON forum_post((path[1]));
CREATE INDEX IF NOT EXISTS idx_forum_thread_slug_id ON forum_thread(citext(slug), id);

CREATE INDEX IF NOT EXISTS idx_forum_post_parent_thread_id ON forum_post(parent, thread, id);
CREATE INDEX IF NOT EXISTS idx_forum_post_thread_id ON forum_post(thread, id);

CREATE INDEX IF NOT EXISTS idx_forum_forum_slug_id ON forum_forum(citext(slug), id);


