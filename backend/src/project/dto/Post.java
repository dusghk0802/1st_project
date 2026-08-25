package project.dto;

import java.sql.Timestamp;

public class Post {
    private long postId;
    private long boardId;
    private long authorId;
    private String title;
    private String content;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private String status; // 'PUBLISHED', 'HIDDEN', 'DELETED'
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 조인해서 작성자 닉네임을 함께 보여줄 때 유용한 필드 (선택)
    private String authorNickname;

    public Post() {}

    // 게시글 작성용 생성자
    public Post(long boardId, long authorId, String title, String content) {
        this.boardId = boardId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }

    // Getter & Setter
    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }
    public long getBoardId() { return boardId; }
    public void setBoardId(long boardId) { this.boardId = boardId; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }
}