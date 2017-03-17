package forum;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kate on 12.03.17.
 */
public class Thread {
    private int id;
    private String author;
    private Timestamp created;
    private String forum;
    private String message;
    private String slug;
    private String title;
    private int votes;
    //private static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSX");
    //private static SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
    public String getSlug() {
        return slug;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    public String getAuthor() {
        return author;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }
    public Timestamp getCreated() {
        return created;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }
    public String getForum() {
        return forum;
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }
    public int getVotes() {
        return votes;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
