package forum;


import com.fasterxml.jackson.annotation.JsonCreator;

public class User {
    private int id;
    private String about;
    private String email;
    private String fullname;
    private String nickname;

    public User() {

    }

    public User(int id, String about, String name, String email, String fullname, String nickname) {
        this.id = id;
        this.about = about;
        this.email = email;
        this.fullname = fullname;
        this.nickname = nickname;
    }

    public int getId() {return this.id;}
    public void setId(int id) {this.id = id;}

    public void setAbout(String about) {this.about = about;}
    public String getAbout() {return this.about;}

    public void setEmail(String email) {this.email = email;}
    public String getEmail() {return this.email;}

    public void setFullname(String fullname) {this.fullname = fullname;}
    public String getFullname() {return this.fullname;}

    public void setNickname(String nickname) {this.nickname = nickname;}
    public String getNickname() {return this.nickname;}
}