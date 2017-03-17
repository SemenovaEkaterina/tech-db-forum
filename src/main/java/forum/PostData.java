package forum;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.criteria.CriteriaBuilder;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostData {
    private String author;
    private Timestamp created;
    private String forum;
    private int id;
    private boolean isEdited;
    private String message;
    private Integer parent;
    private int thread;
    private String path;

    @JsonCreator
    PostData(
            @JsonProperty("author") String author,
            @JsonProperty("message") String message,
            @JsonProperty("parent") Integer parent,
            @JsonProperty("created") String created) throws ParseException {
        this.author = author;
        this.message = message;
        this.parent = parent;
        if (created != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            Date parsedDate = fmt.parse(created);
            this.created = new Timestamp(parsedDate.getTime());
        }
    }

    PostData(@NotNull Post post) {
        this.author = post.getAuthor();
        this.created = post.getCreated();
        this.forum = post.getForum();
        this.id = post.getId();
        this.isEdited = post.getIsEdited();
        this.message = post.getMessage();
        if (post.getParent() == null) {
            this.parent = 0;
        }
        else {
            this.parent = post.getParent();
        }
        this.thread = post.getThread();
        this.path = post.getPath();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreated() {
        if (created == null) {
            return "";
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return fmt.format(created);
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public String getForum() {
        return forum;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
