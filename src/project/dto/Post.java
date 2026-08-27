package project.dto;

/**
 * 커뮤니티 게시글(Post) 데이터를 전달하는 통합 DTO 클래스
 * 작성자 정보, 게시판 정보, 댓글 수(commentCount)를 완벽히 지원합니다.
 */
public class Post {
    private long postId;
    private long boardId;           // 게시판 식별자 (예: 1=자유게시판, 2=영화토론 등)
    private long movieId;           // 영화 번호 (PK)
    private long userId;            // 작성자 회원 번호 (PK / authorId와 동일)
    private String author;          // 작성자 닉네임 (authorNickname과 동일)
    private String authorNickname;  // 작성자 닉네임 (JOIN 조회용)
    private String title;           // 글 제목
    private String content;         // 글 내용
    private int viewCount;          // 조회수
    private int likeCount;          // 추천/좋아요 수
    private int commentCount;       // ✨ 해당 글에 달린 댓글 수
    private String status;          // 상태 (ACTIVE, DELETED 등)
    private String createdAt;       // 작성일시

    // ==========================================================
    // 1. 기본 생성자
    // ==========================================================
    public Post() {
        this.status = "ACTIVE";
        this.commentCount = 0;
    }

    // ==========================================================
    // 2. 글 작성용 생성자 모음 (오버로딩)
    // ==========================================================

    // [게시판/영화 ID, 작성자 ID, 제목, 본문]
    public Post(long boardId, long userId, String title, String content) {
        this.boardId = boardId;
        this.movieId = boardId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.status = "ACTIVE";
        this.commentCount = 0;
    }

    // [작성자 ID, 제목, 본문]
    public Post(long userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.status = "ACTIVE";
        this.commentCount = 0;
    }

    // [제목, 본문, 작성자 닉네임]
    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.authorNickname = author;
        this.status = "ACTIVE";
        this.commentCount = 0;
    }

    // ==========================================================
    // 3. 목록 및 상세 조회용 생성자 모음
    // ==========================================================

    // [글번호, 제목, 작성자, 조회수, 작성일]
    public Post(long postId, String title, String author, int viewCount, String createdAt) {
        this.postId = postId;
        this.title = title;
        this.author = author;
        this.authorNickname = author;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.status = "ACTIVE";
        this.commentCount = 0;
    }

    // [전체 필드 초기화 생성자]
    public Post(long postId, long boardId, long movieId, long userId, String author,
                String title, String content, int viewCount, int likeCount, int commentCount, String status, String createdAt) {
        this.postId = postId;
        this.boardId = boardId;
        this.movieId = movieId;
        this.userId = userId;
        this.author = author;
        this.authorNickname = author;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ==========================================================
    // Getter & Setter
    // ==========================================================
    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getBoardId() {
        return boardId;
    }

    public void setBoardId(long boardId) {
        this.boardId = boardId;
    }

    public long getMovieId() {
        return movieId;
    }

    public void setMovieId(long movieId) {
        this.movieId = movieId;
    }

    // --- User ID & Author ID 상호 연동 ---
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAuthorId() {
        return userId;
    }

    public void setAuthorId(long authorId) {
        this.userId = authorId;
    }

    // --- Author & Nickname 상호 연동 ---
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

    // --- 일반 필드 Getter/Setter ---
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    // ✨ 댓글 수 Getter & Setter
    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
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