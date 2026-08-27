package project;

import project.dao.AdminDAO;
import project.dao.PostDAO;
import project.dao.ReportDAO;
import project.dto.Post;
import project.dto.ReportDTO;
import project.service.AdminService;

import java.util.List;

/**
 * 관리자 신고 관리(블라인드 및 반려) 단위 테스트
 */
public class AdminReportTest {

    public static void main(String[] args) {
        AdminService adminService = new AdminService();
        AdminDAO adminDAO = new AdminDAO();
        PostDAO postDAO = new PostDAO();
        ReportDAO reportDAO = new ReportDAO();

        long testUserId = 1L;

        System.out.println("==================================================");
        System.out.println("🛡️ [관리자 신고 관리 - 블라인드 / 반려 단위 테스트 시작]");
        System.out.println("==================================================");

        // 0. 임시 데이터 생성
        System.out.println("\n▶ 0. 테스트용 임시 게시글 및 신고 데이터 생성");
        Post testPost = new Post(1L, testUserId, "신고 관리 테스트용 게시글", "블라인드 및 반려 테스트 본문입니다.");
        postDAO.createPost(testPost);

        List<Post> currentPosts = postDAO.getAllPosts();
        if (currentPosts.isEmpty()) {
            System.out.println("❌ 게시글 등록 실패로 테스트를 중단합니다.");
            return;
        }
        long dynamicPostId = currentPosts.get(0).getPostId();
        System.out.println("  - 테스트용 게시글 생성 완료 (글번호: #" + dynamicPostId + ")");

        reportDAO.reportTarget("POST", dynamicPostId, testUserId, "부적절한 홍보성 게시글입니다.");

        List<ReportDTO> reportList = adminDAO.getAllReports();
        long dynamicReportId = 0L;
        for (ReportDTO r : reportList) {
            if ("POST".equals(r.getTargetType()) && r.getTargetId() == dynamicPostId) {
                dynamicReportId = r.getReportId();
                break;
            }
        }
        if (dynamicReportId == 0L) {
            dynamicReportId = reportList.isEmpty() ? 1L : reportList.get(0).getReportId();
        }
        System.out.println("  - 테스트용 신고 접수 완료 (신고번호: #" + dynamicReportId + ")");

        // 1. 블라인드 처리 테스트
        System.out.println("\n▶ 1. #" + dynamicPostId + "번 게시글 '블라인드(BLIND)' 처리 시도");
        boolean isBlindSuccess = adminService.processReport(dynamicReportId, "POST", dynamicPostId, "BLIND");
        System.out.println("  처리 결과: " + (isBlindSuccess ? "✅ 블라인드 처리 성공" : "❌ 처리 실패"));

        List<Post> activePostsAfterBlind = postDAO.getAllPosts();
        boolean existsInListAfterBlind = activePostsAfterBlind.stream().anyMatch(p -> p.getPostId() == dynamicPostId);
        System.out.println("  - 일반 게시판 목록 노출 여부: " + (existsInListAfterBlind ? "⚠️ 여전히 노출됨 (오류)" : "🔒 숨김 처리됨 (정상)"));

        // 2. 반려 처리 테스트
        System.out.println("\n▶ 2. #" + dynamicPostId + "번 게시글 신고 '반려(DISMISS)' 처리 (게시글 복구)");
        boolean isDismissSuccess = adminService.processReport(dynamicReportId, "POST", dynamicPostId, "DISMISS");
        System.out.println("  처리 결과: " + (isDismissSuccess ? "✅ 반려 및 게시글 복구 성공" : "❌ 처리 실패"));

        List<Post> activePostsAfterDismiss = postDAO.getAllPosts();
        boolean existsInListAfterDismiss = activePostsAfterDismiss.stream().anyMatch(p -> p.getPostId() == dynamicPostId);
        System.out.println("  - 일반 게시판 목록 노출 여부: " + (existsInListAfterDismiss ? "🔓 정상 복구되어 노출됨 (성공)" : "❌ 노출 안 됨 (오류)"));

        // 3. 테스트 데이터 정리
        postDAO.deletePost(dynamicPostId, testUserId);

        System.out.println("\n==================================================");
        System.out.println("🏁 관리자 신고 관리 단위 테스트 완료!");
        System.out.println("==================================================");
    }
}