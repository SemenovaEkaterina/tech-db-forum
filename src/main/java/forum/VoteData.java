package forum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;


public class VoteData {
    private String nickname;
    private Integer voice;

    @JsonCreator
    VoteData(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("voice") Integer voice) {
        this.nickname = nickname;
        this.voice = voice;

    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getVoice() {
        return voice;
    }

    public void setVoice(Integer voice) {
        this.voice = voice;
    }
}
