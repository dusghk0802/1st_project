package project.dto;

/**
 * 신고(Report) 데이터를 전달하는 통합 DTO 클래스
 * userId / reporterId 간의 상호 호환성을 완벽히 지원합니다.
 */
public class ReportDTO {
    private long reportId;
    private String targetType;       // 신고 대상 유형: POST, COMMENT, USER 등
    private long targetId;           // 대상 식별 번호 (post_id, comment_id 등)
    private long userId;             // 신고자 회원 번호 (reporterId와 동일)
    private String reason;           // 신고 사유
    private String status;           // 처리 상태: PENDING, RESOLVED, REJECTED 등
    private String createdAt;        // 접수 일시
    private String reporterNickname; // 신고자 닉네임 (JOIN 조회용)

    // ==========================================
    // 1. 기본 생성자
    // ==========================================
    public ReportDTO() {
        this.status = "PENDING";
    }

    // ==========================================
    // 2. 신고 등록용 생성자
    // ==========================================
    public ReportDTO(String targetType, long targetId, long userId, String reason) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.userId = userId;
        this.reason = reason;
        this.status = "PENDING";
    }

    // ==========================================
    // 3. 전체 필드 생성자
    // ==========================================
    public ReportDTO(long reportId, String targetType, long targetId, long userId,
                     String reason, String status, String createdAt, String reporterNickname) {
        this.reportId = reportId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.userId = userId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.reporterNickname = reporterNickname;
    }

    // ==========================================
    // Getter & Setter (userId & reporterId 상호 연동)
    // ==========================================
    public long getReportId() {
        return reportId;
    }

    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    // --- userId & reporterId 양방향 호환 ---
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getReporterId() {
        return userId; // reporterId 호출 시 userId 반환
    }

    public void setReporterId(long reporterId) {
        this.userId = reporterId; // reporterId 세팅 시 userId에 대입
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public String getReporterNickname() {
        return reporterNickname;
    }

    public void setReporterNickname(String reporterNickname) {
        this.reporterNickname = reporterNickname;
    }
}