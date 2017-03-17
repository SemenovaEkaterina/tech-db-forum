package forum;

public class DetailPostData {
    private PostData post;
    private UserData author;
    private ThreadData thread;
    private ForumData forum;

    public PostData getPost() {
        return post;
    }

    public void setPost(PostData post) {
        this.post = post;
    }

    public UserData getAuthor() {
        return author;
    }

    public void setAuthor(UserData author) {
        this.author = author;
    }

    public ThreadData getThread() {
        return thread;
    }

    public void setThread(ThreadData thread) {
        this.thread = thread;
    }

    public ForumData getForum() {
        return forum;
    }

    public void setForum(ForumData forum) {
        this.forum = forum;
    }
}
