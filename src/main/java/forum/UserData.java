package forum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;



@SuppressWarnings("unused")
public class UserData {

    private int id;
    private String about;
    private String email;
    private String fullname;
    private String nickname;

    @JsonCreator
    UserData(
            @JsonProperty("about") String about,
            @JsonProperty("email") String email,
            @JsonProperty("fullname") String fullname,
            @JsonProperty("nickname") String nickname) {
        this.about = about;
        this.email = email;
        this.fullname = fullname;
        this.nickname = nickname;
    }

    UserData(@NotNull User user) {
        this.about = user.getAbout();
        this.email = user.getEmail();
        this.fullname = user.getFullname();
        this.nickname = user.getNickname();
    }

    public String getAbout() {
        return about;
    }

    public String getEmail() {
        return email;
    }

    public String getFullname() {
        return fullname;
    }

    public String getNickname() {
        return nickname;
    }

}
