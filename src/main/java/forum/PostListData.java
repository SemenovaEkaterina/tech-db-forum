package forum;

import javax.validation.constraints.NotNull;
import java.util.List;

public class PostListData {

    private String marker;
    private PostData posts[];

    PostListData(@NotNull String marker, PostData posts[]) {
        this.marker = marker;
        this.posts = posts;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public PostData[] getPosts() {
        return posts;
    }

    public void setPosts(PostData posts[]) {
        this.posts = posts;
    }
}
