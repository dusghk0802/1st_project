package project.dto;

/**
 * 커뮤니티 댓글(Comment) 데이터를 전달하는 통합 DTO 클래스
 * userId / authorId, author / authorNickname 상호 호환성을 완벽히 지원합니다.
 */
public class Comment {
    private long commentId;
    private long postId;
    private long userId;             // 작성자 회원 번호 (authorId와 동일)
    private Long parentCommentId;    // 대댓글용 부모 댓글 ID (null 허용)
    private String author;          // 작성자 닉네임 (authorNickname과 동일)
    private String authorNickname;   // 작성자 닉네임 (JOIN 조회용)
    private String content;          // 댓글 내용
    private String status;           // 상태 (ACTIVE, DELETED 등)
    private String createdAt;        // 작성 일시

    // ==========================================
    // 1. 기본 생성자
    // ==========================================
    public Comment() {
        this.status = "ACTIVE";
    }

    // ==========================================
    // 2. 루트 댓글 생성자 (postId, userId, content)
    // ==========================================
    public Comment(long postId, long userId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.parentCommentId = null;
        this.status = "ACTIVE";
    }

    // ==========================================
    // 3. 대댓글(답글) 생성자 (postId, userId, parentCommentId, content)
    // ==========================================
    public Comment(long postId, long userId, Long parentCommentId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.status = "ACTIVE";
    }

    // ==========================================
    // Getter & Setter (authorId 상호 호환 지원)
    // ==========================================
    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    // --- User ID & Author ID 완벽 연동 ---
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAuthorId() {
        return userId; // ✨ getAuthorId 호출 시 userId 반환
    }

    public void setAuthorId(long authorId) {
        this.userId = authorId; // ✨ setAuthorId 호출 시 userId에 저장
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    // --- Author & Nickname 완벽 연동 ---
    public String getAuthor() {
        return author != null ? author : authorNickname;
    }

    public void setAuthor(String author) {
        this.author = author;
        this.authorNickname = author;
    }

    public String getAuthorNickname() {
        return authorNickname != null ? authorNickname : author;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
        this.author = authorNickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}