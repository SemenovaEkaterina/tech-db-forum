package forum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kate on 12.03.17.
 */


public class ThreadData {
    private int id;
    private String author;
    private Timestamp created;
    private String forum;
    private String message;
    private String slug;
    private String title;
    private int votes;

    @JsonCreator
    ThreadData(
            @JsonProperty("author") String author,
            @JsonProperty("created") String created,
            @JsonProperty("message") String message,
            @JsonProperty("title") String title) throws ParseException{
        this.author = author;

        if (created != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            Date parsedDate = fmt.parse(created);
            this.created = new Timestamp(parsedDate.getTime());
        }
        this.message = message;
        this.title = title;
    }

    ThreadData(@NotNull Thread thread) {
        this.author = thread.getAuthor();
        this.created = thread.getCreated();
        this.message = thread.getMessage();
        this.forum = thread.getForum();
        this.id = thread.getId();
        this.title = thread.getTitle();
        this.slug = thread.getSlug();
        this.votes = thread.getVotes();
    }

    public String getMessage() {
        return message;
    }

    public int getVotes() {
        return votes;
    }

    public String getForum() {
        return forum;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getCreated() {
        if (created == null) {
            return "";
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return fmt.format(created);
    }

    public Timestamp getDateCreated() {
        return created;
    }

    public int getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }
}
