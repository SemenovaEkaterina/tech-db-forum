package forum;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


@RestController
public class ForumApi {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping(path = "api/user/{nickname}/profile", produces = "application/json")
    public ResponseEntity getUser(@PathVariable String nickname) {

        List<User> users = jdbcTemplate.query("SELECT * FROM forum_user WHERE nickname=?::citext;",
                new Object[]{nickname},
                new BeanPropertyRowMapper(User.class));

        if(users.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        return ResponseEntity.ok(new UserData(users.get(0)));
    }

    @PostMapping(path = "/api/user/{nickname}/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity createUser(@RequestBody UserData body, @PathVariable String nickname) {

        try {
            jdbcTemplate.update("INSERT INTO forum_user (about, email, fullname, nickname) VALUES (?, ?, ?, ?::citext);",
                    new Object[]{body.getAbout(), body.getEmail(), body.getFullname(), nickname},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});


            List<User> users = jdbcTemplate.query("SELECT * FROM forum_user f WHERE f.nickname=?::citext;",
                    new Object[]{nickname},
                    new BeanPropertyRowMapper(User.class));

            return ResponseEntity.status(201)
                    .body(new UserData(users.get(0)));
        }
        catch (DuplicateKeyException e) {
            List<User> users = jdbcTemplate.query("SELECT * FROM forum_user f WHERE f.nickname::citext=?::citext OR f.email::citext=?::citext;",
                    new Object[]{nickname, body.getEmail()},
                    new BeanPropertyRowMapper(User.class));

            return ResponseEntity.status(409).body(users);
        }
    }


    @PostMapping(path = "/api/user/{nickname}/profile", consumes = "application/json", produces="application/json")
    public ResponseEntity changeUser(@RequestBody UserData body, @PathVariable String nickname) {


        try {
            List<User> users = jdbcTemplate.query("UPDATE forum_user SET email = COALESCE(?, email), about = COALESCE(?, about), fullname = COALESCE(?, fullname) WHERE nickname=?::citext RETURNING *;",
                    new Object[]{body.getEmail(), body.getAbout(), body.getFullname(), nickname},
                    new BeanPropertyRowMapper(User.class));

            if (users.isEmpty()) {
                return ResponseEntity.status(404).body("Not found");
            }
            return ResponseEntity.ok(new UserData(users.get(0)));
        }
        catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body("Such data already exists");
        }

    }

    @PostMapping(path = "/api/forum/create", consumes = "application/json", produces="application/json")
    public ResponseEntity createForum(@RequestBody ForumData body) {

        try {
            List<Forum> forums = jdbcTemplate.query("WITH req as (SELECT * FROM forum_user WHERE nickname=?::citext) INSERT INTO forum_forum as f (slug, title, forum_user) VALUES (?, ?, (SELECT id FROM req)) RETURNING f.id, f.slug, f.title, (SELECT nickname FROM req) as user;",
                    new Object[]{body.getUser(), body.getSlug(), body.getTitle()},
                    new BeanPropertyRowMapper(Forum.class));

            return ResponseEntity.status(201)
                    .body(new ForumData(forums.get(0)));
        }
        catch (DuplicateKeyException e) {
            List<Forum> forums = jdbcTemplate.query("SELECT f.id, f.slug, f.title, u.nickname as user FROM forum_forum f JOIN forum_user u ON(f.forum_user=u.id) WHERE f.slug=?::citext;",
                    new Object[]{body.getSlug()},
                    new BeanPropertyRowMapper(Forum.class));
            return ResponseEntity.status(409)
                    .body(new ForumData(forums.get(0)));

        }
        catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }
    }

    @GetMapping(path = "/api/forum/{slug}/details", produces="application/json")
    public ResponseEntity getForum(@PathVariable String slug) {
        List<Forum> forums = jdbcTemplate.query("WITH req1 as (\n" +
                        "    SELECT COUNT(t.id) as threads\n" +
                        "    FROM forum_forum f\n" +
                        "    JOIN forum_thread t ON(t.forum=f.id)\n " +
                        "    WHERE f.slug=?::citext " +
                        "),\n" +
                        "    req2 as (\n" +
                        "    SELECT COUNT(p.id) as posts\n" +
                        "    FROM forum_forum f\n" +
                        "    JOIN forum_post p ON(p.forum=f.id)\n" +
                        "    WHERE f.slug=?::citext " +
                        "    )\n" +
                        "SELECT f.id, f.slug, f.title, u.nickname as user, (SELECT threads from req1), (SELECT posts FROM req2)\n" +
                        "FROM forum_forum f \n" +
                        "JOIN forum_user u ON(f.forum_user=u.id)\n" +
                        "WHERE slug=?::citext;",
                new Object[]{slug, slug, slug},
                new BeanPropertyRowMapper(Forum.class));

        if (forums.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        return ResponseEntity.status(200)
                .body(new ForumData(forums.get(0)));
    }

    @PostMapping(path = "/api/forum/{slug}/create", produces="application/json")
    public ResponseEntity createThread(@RequestBody ThreadData body, @PathVariable String slug) {

        try {
            List<Thread> threads = jdbcTemplate.query("WITH req1 as (SELECT * FROM forum_user WHERE nickname=?::citext),\n" +
                            "    req2 as (SELECT * FROM forum_forum WHERE slug=?::citext)\n" +
                            "INSERT INTO forum_thread as f (author, created, forum, message, title, slug)\n" +
                            "    VALUES ((SELECT id FROM req1), ?, (SELECT id FROM req2), ?, ?, ?)\n" +
                            "RETURNING id, (SELECT nickname as author FROM req1), created, (SELECT slug as forum FROM req2), message, title, slug, 0;",
                    new Object[]{body.getAuthor(), slug, body.getDateCreated(), body.getMessage(), body.getTitle(), body.getSlug()},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR},
                    new BeanPropertyRowMapper(Thread.class));

            return ResponseEntity.status(201)
                    .body(new ThreadData(threads.get(0)));
        }
        catch (DuplicateKeyException e){
            List<Thread> threads = jdbcTemplate.query("SELECT t.*, u.nickname as author, f.slug as forum FROM forum_thread t" +
                            " JOIN forum_user u ON (t.author=u.id) JOIN forum_forum f ON(t.forum=f.id) " +
                            "WHERE t.slug=?::citext",
                    new Object[]{body.getSlug()},
                    new int[] {Types.VARCHAR},
                    new BeanPropertyRowMapper(Thread.class));
            return ResponseEntity.status(409)
                    .body(new ThreadData(threads.get(0)));
        }

        catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }
    }


    @GetMapping(path = "/api/forum/{slug}/threads", produces="application/json")
    public ResponseEntity getThreads(@PathVariable String slug, @RequestParam(value="limit", required = false) Integer limit,
                                     @RequestParam(value="since", required = false) String since, @RequestParam(value="desc", required = false) Boolean desc) throws ParseException {

        List<Forum> forums = jdbcTemplate.query("SELECT * FROM forum_forum f WHERE f.slug=?::citext",
                new Object[]{slug},
                new int[] {Types.VARCHAR},
                new BeanPropertyRowMapper(Forum.class));

        if (forums.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }


        Timestamp sinceT = new Timestamp(0);
        if (since != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            java.util.Date parsedDate = fmt.parse(since);
            sinceT = new Timestamp(parsedDate.getTime());
        }


        String sql = "SELECT u.nickname as author, t.created, f.slug as forum, t.id, t.message, t.slug, t.title " +
                "FROM forum_thread t JOIN forum_forum f ON (t.forum=f.id) JOIN forum_user u ON (t.author=u.id) " +
                "WHERE f.slug=?::citext ";

        if (since != null) {
            if(desc != null && desc) {
                sql += " AND t.created <= '" + since.toString() + "' ";
            }
            else {
                sql += " AND t.created >= '" + since.toString() + "' ";
            }
        }

        sql += "ORDER BY t.created";

        if (desc != null && desc == true) {
            sql += " DESC";
        }
        if (limit != null) {
            sql += " LIMIT " + limit.toString();
        }

        sql += ";";

        List<Thread> threads = jdbcTemplate.query(sql,
                new Object[]{slug},
                new int[] {Types.VARCHAR},
                new BeanPropertyRowMapper(Thread.class));


        ThreadData threadDatas[] = new ThreadData[threads.size()];

        for (int i = 0; i < threads.size(); i++) {
            threadDatas[i] = new ThreadData(threads.get(i));
        }

        return ResponseEntity.status(200)
                .body(threadDatas);
    }

    @Transactional
    @PostMapping(path = "/api/thread/{slug_or_id}/create", produces="application/json")
    public ResponseEntity createPost(@RequestBody String body, @PathVariable String slug_or_id) throws IOException{

        ObjectMapper mapper = new ObjectMapper();

        List<PostData> request = mapper.readValue(body, new TypeReference<List<PostData>>(){});
        List<Post> posts = new ArrayList<Post>();
        List<Post> curPosts = new ArrayList<Post>();

        Integer id;

        if (slug_or_id.matches("\\d+")) {
            id = Integer.parseInt(slug_or_id);
            slug_or_id = null;
        }
        else {
            id = null;
        }

        if (request.isEmpty()) {
            Integer count = (int)jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_thread WHERE id=? OR slug=?::citext;",
                    new Object[] {id, slug_or_id},
                    Integer.class);

            if (count.equals(0)) {
                return ResponseEntity.status(404)
                        .body(null);
            }

        }

        java.sql.Timestamp now_t = new java.sql.Timestamp(new java.util.Date().getTime());

        for (int i = 0; i < request.size(); i++) {

            try {



                curPosts = jdbcTemplate.query("INSERT INTO forum_post as p1 (author, forum, message, parent, thread, isedited, created)\n" +
                                "SELECT DISTINCT u.id AS author, f.id AS forum, ?, p.id as parent, (CASE WHEN ? is null THEN t.id ELSE p.thread END) AS thread, FALSE, ?::TIMESTAMP" +
                                " FROM forum_thread t\n" +
                                "LEFT JOIN forum_forum f ON (t.forum = f.id)\n" +
                                "LEFT JOIN forum_post p ON (p.id=? AND p.thread=t.id),\n" +
                                "forum_user u\n" +
                                "WHERE (u.nickname = ?::citext OR u.nickname IS NULL) AND\n" +
                                "(t.id = ? OR t.slug = ?::citext OR t.id IS NULL)\n" +
                                "GROUP BY 1, 2, 3, 4, 5, 6\n" +
                                "RETURNING p1.id, (SELECT nickname FROM forum_user WHERE id=p1.author) as author, p1.created, (SELECT slug FROM forum_forum WHERE id=p1.forum) as forum, p1.message, p1.parent, p1.thread;",
                        new Object[]{request.get(i).getMessage(),request.get(i).getParent(), now_t, request.get(i).getParent(), request.get(i).getAuthor(), id, slug_or_id},
                        new int[] {Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.VARCHAR},
                        new BeanPropertyRowMapper(Post.class));

                if (curPosts.isEmpty()) {
                    return ResponseEntity.status(404)
                            .body(null);
                }
                posts.add(curPosts.get(0));
            }
            catch (DataIntegrityViolationException e) {
                return ResponseEntity.status(409)
                        .body(null);
            }

        }



        PostData postDatas[] = new PostData[posts.size()];

        for (int i = 0; i < posts.size(); i++) {
            postDatas[i] = new PostData(posts.get(i));
        }

        return ResponseEntity.status(201)
                .body(postDatas);
    }

    @PostMapping(path = "/api/thread/{slug_or_id}/vote", consumes = "application/json", produces="application/json")
    public ResponseEntity vote(@RequestBody VoteData body, @PathVariable String slug_or_id) {

        Integer id;

        if (slug_or_id.matches("\\d+")) {
            id = Integer.parseInt(slug_or_id);
            slug_or_id = null;
        }
        else {
            id = null;
        }

        try {
            jdbcTemplate.query("WITH req1 as (SELECT u.id as id FROM forum_user u WHERE u.nickname=?::citext), " +
                            "req2 as (SELECT t.id as id FROM forum_thread t WHERE t.id = ? OR slug=?::citext)" +
                            " INSERT INTO forum_vote as v (nickname, voice, thread)" +
                            " VALUES ((SELECT id FROM req1), ?, (SELECT id FROM req2))" +
                            " ON CONFLICT (nickname, thread)" +
                            " DO UPDATE SET voice = ?" +
                            " RETURNING *;",
                    new Object[]{body.getNickname(), id, slug_or_id, body.getVoice(), body.getVoice()},
                    new BeanPropertyRowMapper(Vote.class));

            List<Thread> threads =  jdbcTemplate.query("SELECT u.nickname as author, t.created, f.slug as forum, t.id, t.message, t.slug, t.title, SUM(COALESCE(v.voice, 0)) as votes" +
                            " FROM forum_thread t" +
                            " JOIN forum_user u ON(u.id=t.author)" +
                            " JOIN forum_forum f ON(f.id=t.forum)" +
                            " LEFT JOIN forum_vote v ON (t.id=v.thread)" +
                            " WHERE t. id=? OR t.slug=?::citext" +
                            " GROUP BY 1,2,3,4,5,6;",
                    new Object[]{id, slug_or_id},
                    new BeanPropertyRowMapper(Thread.class));

            return ResponseEntity.status(200)
                    .body(new ThreadData(threads.get(0)));
        }
        catch (DuplicateKeyException e) {
            return ResponseEntity.status(409)
                    .body(null);

        }
        catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

    }

    @GetMapping(path = "/api/thread/{slug_or_id}/details", produces="application/json")
    public ResponseEntity getThread(@PathVariable String slug_or_id) {

        Integer id;

        if (slug_or_id.matches("\\d+")) {
            id = Integer.parseInt(slug_or_id);
            slug_or_id = null;
        }
        else {
            id = null;
        }

        List<Thread> threads =  jdbcTemplate.query("SELECT u.nickname as author, t.created, f.slug as forum, t.id, t.message, t.slug, t.title, SUM(COALESCE(v.voice, 0)) as votes" +
                        " FROM forum_thread t" +
                        " JOIN forum_user u ON(u.id=t.author)" +
                        " JOIN forum_forum f ON(f.id=t.forum)" +
                        " LEFT JOIN forum_vote v ON (t.id=v.thread)" +
                        " WHERE t.id=? OR t.slug=?::citext" +
                        " GROUP BY 1,2,3,4,5,6;",
                new Object[]{id, slug_or_id},
                new BeanPropertyRowMapper(Thread.class));

        if (threads.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not Found");
        }

        return ResponseEntity.status(200)
                .body(new ThreadData(threads.get(0)));
    }


    @GetMapping(path = "/api/thread/{slug_or_id}/posts", produces="application/json")
    public ResponseEntity getPosts(@PathVariable String slug_or_id, @RequestParam(value="limit", required = false) Integer limit,
                                   @RequestParam(value="marker", required = false) String marker, @RequestParam(value="desc", required = false) Boolean desc,
                                   @RequestParam(value="sort", required = false) String sort) {
        Integer id;

        if (slug_or_id.matches("\\d+")) {
            id = Integer.parseInt(slug_or_id);
            slug_or_id = null;
        }
        else {
            id = null;
        }

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_thread WHERE id=? OR slug=?::citext;",
                new Object[] {id, slug_or_id},
                Integer.class);

        if (count.equals(0)) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        String sql = "";

        if (sort == null) {
            sort = "flat";
        }

        if (sort != null && sort.equals("tree")) {
            sql = "WITH RECURSIVE tree (path, author, created, forum, id, isEdited, message, parent, thread) AS (\n" +
                    "        SELECT array[p.id], u.nickname as author, p.created, f.slug as forum, p.id, p.isEdited, p.message, p.parent, p.thread\n" +
                    "        FROM forum_post p\n" +
                    "        JOIN forum_user u ON(p.author=u.id)\n" +
                    "        JOIN forum_forum f ON(f.id=p.forum)\n" +
                    "        JOIN forum_thread t ON(t.id=p.thread)\n" +
                    "        WHERE (t.id=? OR t.slug=?::citext) AND p.parent ISNULL\n" +
                    "    UNION ALL\n" +
                    "        SELECT array_append(rt.path, p2.id), u2.nickname as author, p2.created, f2.slug as forum, p2.id, p2.isEdited, p2.message, p2.parent, p2.thread\n" +
                    "        FROM forum_post p2\n" +
                    "        JOIN forum_user u2 ON(p2.author=u2.id)\n" +
                    "        JOIN forum_forum f2 ON(f2.id=p2.forum)\n" +
                    "        JOIN forum_thread t2 ON(t2.id=p2.thread)\n" +
                    "        JOIN tree rt ON (rt.id=p2.parent)\n" +
                    ")\n" +
                    "SELECT id, path, author, created, forum, isEdited, message, parent, thread\n" +
                    "FROM tree\n";


            if (marker != null) {
                if (desc != null && desc) {
                    sql += " WHERE path < '" + marker.toString() + "' ";
                } else {
                    sql += " WHERE path > '" + marker.toString() + "' ";
                }
            }

            sql += " ORDER BY path";

            if (desc != null && desc) {
                sql += " DESC";
            }

            sql += " ,1";

            if (desc != null && desc == true) {
                sql += " DESC";
            }

            if (limit != null) {
                sql += " LIMIT " + limit.toString();
            }

            sql += ";";
        }

        if (sort != null && sort.equals("parent_tree")) {

            sql = "WITH RECURSIVE tree (path, author, created, forum, id, isEdited, message, parent, thread) AS (\n" +
                    "        SELECT array[p.id], u.nickname as author, p.created, f.slug as forum, p.id, p.isEdited, p.message, p.parent, p.thread\n" +
                    "        FROM forum_post p\n" +
                    "        JOIN forum_user u ON(p.author=u.id)\n" +
                    "        JOIN forum_forum f ON(f.id=p.forum)\n" +
                    "        JOIN forum_thread t ON(t.id=p.thread)\n" +
                    "        WHERE (t.id=? OR t.slug=?::citext) AND p.parent ISNULL\n" +
                    "    UNION ALL\n" +
                    "        SELECT array_append(rt.path, p2.id), u2.nickname as author, p2.created, f2.slug as forum, p2.id, p2.isEdited, p2.message, p2.parent, p2.thread\n" +
                    "        FROM forum_post p2\n" +
                    "        JOIN forum_user u2 ON(p2.author=u2.id)\n" +
                    "        JOIN forum_forum f2 ON(f2.id=p2.forum)\n" +
                    "        JOIN forum_thread t2 ON(t2.id=p2.thread)\n" +
                    "        JOIN tree rt ON (rt.id=p2.parent)\n" +
                    ")\n" +
                    "SELECT id, path, author, created, forum, isEdited, message, parent, thread, dense_rank() OVER (ORDER BY path[1]";


            if (desc != null && desc) {
                sql += " DESC";
            }
            sql += ") AS rank FROM tree\n";

            if (marker != null) {
                if (desc != null && desc) {
                    sql += " WHERE path < '" + marker.toString() + "' ";
                } else {
                    sql += " WHERE path > '" + marker.toString() + "' ";
                }
            }

            sql += " ORDER BY 2";

            if (desc != null && desc == true) {
                sql += " DESC";
            }

            sql += " ,1";

            if (desc != null && desc == true) {
                sql += " DESC";
            }


            if (limit != null) {
                sql = "SELECT * FROM (" + sql + ") as req  WHERE rank <= " + limit.toString();
            }
            sql += ";";

        }

        if (sort != null && sort.equals("flat")) {
            sql = "SELECT u.nickname as author, p.created, f.slug as forum, p.id, p.isEdited, p.message, p.parent, p.thread" +
                    " FROM forum_post p" +
                    " JOIN forum_user u ON(p.author=u.id)" +
                    " JOIN forum_forum f ON(f.id=p.forum)" +
                    " JOIN forum_thread t ON(t.id=p.thread)" +
                    " WHERE (t.id=? OR t.slug=?::citext)";

            if (marker != null) {
                if (desc != null && desc) {
                    sql += " AND p.id < " + marker.toString() + " ";
                } else {
                    sql += " AND p.id > " + marker.toString() + " ";
                }
            }

            sql += " ORDER BY 2";

            if (desc != null && desc == true) {
                sql += " DESC";
            }

            sql += ", 4";

            if (desc != null && desc == true) {
                sql += " DESC";
            }

            if (limit != null) {
                sql += " LIMIT " + limit.toString();
            }

            sql += ";";
        }

        List<Post> posts = jdbcTemplate.query(sql,
                new Object[]{id, slug_or_id},
                new BeanPropertyRowMapper(Post.class));


        PostData postDatas[] = new PostData[posts.size()];

        for (int i = 0; i < posts.size(); i++) {
            postDatas[i] = new PostData(posts.get(i));
        }

        if (!posts.isEmpty()) {
            if (sort.equals("flat")) {
                marker = Integer.toString(postDatas[posts.size() - 1].getId());
            }
            if (sort.equals("tree")) {
                marker = postDatas[posts.size() - 1].getPath();
            }
            if (sort.equals("parent_tree")) {
                marker = postDatas[posts.size() - 1].getPath();
            }
        }

        return ResponseEntity.status(200)
                .body(new PostListData(marker, postDatas));
    }


    @PostMapping(path = "/api/service/clear")
    public ResponseEntity clear() {
        jdbcTemplate.update("TRUNCATE TABLE forum_user CASCADE;",
                new Object[]{});

        return ResponseEntity.status(200)
                .body(null);
    }

    @PostMapping(path = "/api/thread/{slug_or_id}/details", consumes="application/json",produces="application/json")
    public ResponseEntity changeThread(@RequestBody ThreadData body, @PathVariable String slug_or_id) {

        Integer id;

        if (slug_or_id.matches("\\d+")) {
            id = Integer.parseInt(slug_or_id);
            slug_or_id = null;
        }
        else {
            id = null;
        }

        List<Thread> threads = jdbcTemplate.query("UPDATE forum_thread t1\n" +
                        "SET message = COALESCE(?, t.message), title = COALESCE(?, t.title)\n" +
                        "FROM forum_thread t\n" +
                        "JOIN forum_forum f ON(f.id=t.forum)\n" +
                        "JOIN forum_user u ON(u.id=t.author)\n" +
                        "WHERE t.id=? OR t.slug=?::citext\n" +
                        "RETURNING u.nickname as author, t.created, f.slug as forum, t.id, t1.message, t.slug, t1.title;",
                new Object[]{body.getMessage(), body.getTitle(), id, slug_or_id},
                new BeanPropertyRowMapper(Thread.class));

        if (threads.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        return ResponseEntity.status(200)
                .body(new ThreadData(threads.get(0)));

    }

    @GetMapping(path = "/api/forum/{slug}/users", produces="application/json")
    public ResponseEntity getUsers(@PathVariable String slug, @RequestParam(value="limit", required = false) Integer limit,
                                   @RequestParam(value="since", required = false) String since, @RequestParam(value="desc", required = false) Boolean desc) {

        List<Forum> forums = jdbcTemplate.query("SELECT * FROM forum_forum WHERE slug=?::citext",
                new Object[]{slug},
                new BeanPropertyRowMapper(Forum.class));

        if (forums.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        String sql = "SELECT * FROM (\n" +
                "    SELECT DISTINCT u.*\n" +
                "    FROM forum_user u\n" +
                "        JOIN forum_thread t ON (t.author = u.id)\n" +
                "        JOIN forum_forum f ON (f.id = t.forum)\n" +
                "    WHERE f.slug = ?::citext\n" +
                "    UNION\n" +
                "    SELECT DISTINCT u.*\n" +
                "    FROM forum_user u\n" +
                "        JOIN forum_post p ON (p.author = u.id)\n" +
                "        JOIN forum_forum f ON (f.id = p.forum)\n" +
                "    WHERE f.slug = ?::citext\n" +
                ") as req\n";

        if (since != null) {
            if (desc != null && desc) {
                sql += " WHERE nickname < '" + since + "'\n";
            }
            else {
                sql += " WHERE nickname > '" + since + "'\n";
            }
        }

        sql += "ORDER BY nickname";

        if (desc != null && desc) {
            sql += " DESC";
        }

        if (limit != null) {
            sql += " LIMIT " + limit.toString();
        }

        sql += ";";

        List<User> users = jdbcTemplate.query(sql,
                new Object[]{slug, slug},
                new BeanPropertyRowMapper(User.class));

        return ResponseEntity.status(200)
                .body(users);
    }

    @GetMapping(path = "/api/post/{id}/details", produces="application/json")
    public ResponseEntity getPost(@PathVariable Integer id, @RequestParam(value = "related", required = false) String related) {

        DetailPostData detailPostData = new DetailPostData();

        String sql = "SELECT u.nickname as author, p.created, f.slug as forum, p.id, p.isEdited, p.message, p.thread FROM forum_post p\n" +
                "JOIN forum_user u ON(u.id=p.author)\n" +
                "JOIN forum_forum f ON(f.id=p.forum)\n" +
                "WHERE p.id=?;";

        List<Post> posts =  jdbcTemplate.query(sql,
                new Object[]{id},
                new BeanPropertyRowMapper(Post.class));

        if (posts.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        detailPostData.setPost(new PostData(posts.get(0)));

        if (related != null && related.contains("user")) {
            List<User> users = jdbcTemplate.query("SELECT u.about, u.email, u.fullname, u.nickname" +
                            " FROM forum_user u" +
                            " JOIN forum_post p ON(p.author=u.id)" +
                            " WHERE p.id=?",
                    new Object[]{id},
                    new BeanPropertyRowMapper(User.class));


            detailPostData.setAuthor(new UserData(users.get(0)));
        }

        if (related != null && related.contains("thread")) {
            List<Thread> threads = jdbcTemplate.query("SELECT u.nickname as author, t.created, f.slug as forum, t.id, t.message, t.title, t.slug" +
                            " FROM forum_thread t" +
                            " JOIN forum_user u ON(t.author=u.id)" +
                            " JOIN forum_forum f ON(f.id=t.forum)" +
                            " JOIN forum_post p ON(p.thread=t.id)" +
                            " WHERE p.id=?",
                    new Object[]{id},
                    new BeanPropertyRowMapper(Thread.class));


            detailPostData.setThread(new ThreadData(threads.get(0)));

        }

        if (related != null && related.contains("forum")) {
            sql = "WITH req1 as (\n" +
                    "    SELECT COUNT(t.id) as threads\n" +
                    "    FROM forum_forum f\n" +
                    "    JOIN forum_thread t ON(t.forum=f.id)\n " +
                    "    WHERE f.slug=?::citext " +
                    "),\n" +
                    "    req2 as (\n" +
                    "    SELECT COUNT(p.id) as posts\n" +
                    "    FROM forum_forum f\n" +
                    "    JOIN forum_post p ON(p.forum=f.id)\n" +
                    "    WHERE f.slug=?::citext " +
                    "    )\n" +
                    "SELECT f.id, f.slug, f.title, u.nickname as user, (SELECT threads from req1), (SELECT posts FROM req2)\n" +
                    "FROM forum_forum f \n" +
                    "JOIN forum_user u ON(f.forum_user=u.id)\n" +
                    "WHERE slug=?::citext;";
            List<Forum> forums = jdbcTemplate.query(sql,
                    new Object[]{posts.get(0).getForum(), posts.get(0).getForum(), posts.get(0).getForum()},
                    new BeanPropertyRowMapper(Forum.class));


            detailPostData.setForum(new ForumData(forums.get(0)));

        }


        return ResponseEntity.status(200)
                .body(detailPostData);
    }

    @PostMapping(path = "/api/post/{id}/details", produces="application/json")
    public ResponseEntity changePost(@PathVariable Integer id, @RequestBody PostData body) {

        String sql = "UPDATE forum_post p1\n" +
                "SET message=COALESCE(?, p.message), isedited=(CASE WHEN ?::citext<>p.message THEN true ELSE false END)\n" +
                "FROM forum_post p\n" +
                "JOIN forum_user u ON(u.id=p.author)\n" +
                "JOIN forum_forum f ON(f.id=p.forum)\n" +
                "WHERE p.id=?\n" +
                "RETURNING u.nickname as author, p.created, f.slug as forum, p.id, p1.isedited, p1.message, p.thread;";


        List<Post> posts = jdbcTemplate.query(sql,
                new Object[]{body.getMessage(), body.getMessage(), id},
                new BeanPropertyRowMapper(Post.class));

        if (posts.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Not found");
        }

        return ResponseEntity.status(200)
                .body(new PostData(posts.get(0)));
    }

    @GetMapping(path = "/api/service/status", produces="application/json")
    public ResponseEntity getStatus() {
        Status status = new Status();

        status.setForum(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_forum;", new Object[] {}, Integer.class));
        status.setPost(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_post;", new Object[] {}, Integer.class));
        status.setThread(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_thread;", new Object[] {}, Integer.class));
        status.setUser(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM forum_user;", new Object[] {}, Integer.class));

        return ResponseEntity.status(200)
                .body(status);

    }


}
