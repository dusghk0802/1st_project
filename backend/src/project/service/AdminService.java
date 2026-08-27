package project.service;

import project.dao.AdminDAO;

/**
 * 관리자 페이지 신고 처리 비즈니스 로직 서비스
 * DB 체크 제약조건(CHK_REPORTS_STATUS) 호환: RESOLVED, REJECTED
 */
public class AdminService {

    private final AdminDAO adminDAO;

    public AdminService() {
        this.adminDAO = new AdminDAO();
    }

    public AdminService(AdminDAO adminDAO) {
        this.adminDAO = adminDAO;
    }

    /**
     * 관리자 신고 처리 (블라인드 또는 반려)
     *
     * @param reportId   신고 접수 번호
     * @param targetType 신고 대상 ("POST" 또는 "COMMENT")
     * @param targetId   신고 대상 ID (post_id 또는 comment_id)
     * @param action     관리자 조치 ("BLIND" 또는 "DISMISS" / "REJECT")
     * @return 처리 성공 여부
     */
    public boolean processReport(long reportId, String targetType, long targetId, String action) {
        if (action == null || targetType == null) {
            return false;
        }

        if ("BLIND".equalsIgnoreCase(action)) {
            // 1. 블라인드 처리: 원본 글/댓글 숨김('BLIND') 및 신고 상태 처리완료('RESOLVED')
            boolean targetUpdated = adminDAO.updateTargetStatus(targetType, targetId, "BLIND");
            boolean reportUpdated = adminDAO.updateReportStatus(reportId, "RESOLVED");
            return targetUpdated && reportUpdated;

        } else if ("DISMISS".equalsIgnoreCase(action) || "REJECT".equalsIgnoreCase(action)) {
            // 2. 반려 처리: 원본 글/댓글 정상 복구('ACTIVE') 및 신고 상태 기각('REJECTED')
            boolean targetUpdated = adminDAO.updateTargetStatus(targetType, targetId, "ACTIVE");
            boolean reportUpdated = adminDAO.updateReportStatus(reportId, "REJECTED");
            return targetUpdated && reportUpdated;
        }

        return false;
    }
}