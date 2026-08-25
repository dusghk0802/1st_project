package project.dto;

import java.sql.Timestamp;

public class Comment {
    private long commentId;
    private long postId;
    private long authorId;
    private Long parentCommentId; // null일 수 있으므로 Long 객체 사용
    private String content;
    private String status; // 'PUBLISHED', 'HIDDEN', 'DELETED'
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 조인해서 작성자 닉네임을 보여주기 위한 필드
    private String authorNickname;

    public Comment() {}

    // 댓글 작성용 생성자 (루트 댓글용)
    public Comment(long postId, long authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.parentCommentId = null;
    }

    // 대댓글 작성용 생성자 (부모 댓글 ID 포함)
    public Comment(long postId, long authorId, Long parentCommentId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.content = content;
    }

    // Getter & Setter
    public long getCommentId() { return commentId; }
    public void setCommentId(long commentId) { this.commentId = commentId; }
    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }
}