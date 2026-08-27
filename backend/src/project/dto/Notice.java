package project.dto;

public class Notice {
    private long noticeId;
    private String title;
    private String content;
    private boolean pinned;
    private String createdAt;

    public Notice() {}

    public Notice(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

    public Notice(long noticeId, String title, String content, boolean pinned, String createdAt) {
        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.createdAt = createdAt;
    }

    public long getNoticeId() { return noticeId; }
    public void setNoticeId(long noticeId) { this.noticeId = noticeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}