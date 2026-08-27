package project;

import project.dao.AdminDAO;
import project.dao.BoardDAO;
import project.dao.CategoryDAO;
import project.dao.NoticeDAO;
import project.dto.Board;
import project.dto.Category;
import project.dto.Notice;
import project.dto.ReportDTO;

import java.util.List;

public class AdminFeatureTest {

    public static void main(String[] args) {
        AdminDAO adminDAO = new AdminDAO();
        BoardDAO boardDAO = new BoardDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        NoticeDAO noticeDAO = new NoticeDAO();

        System.out.println("==================================================");
        System.out.println("🛠️ [관리자 신규 기능 통합 단위 테스트 시작]");
        System.out.println("==================================================");

        // 1. 대시보드 통계 수치 조회
        System.out.println("\n▶ 1. 대시보드 요약 통계 조회");
        int totalUsers = adminDAO.getTotalUserCount();
        int totalPosts = adminDAO.getTotalPostCount();
        int pendingReports = adminDAO.getPendingReportCount();
        System.out.println("  - 총 회원 수: " + totalUsers + "명");
        System.out.println("  - 총 게시글 수: " + totalPosts + "개");
        System.out.println("  - 미처리 신고 건수: " + pendingReports + "건");

        // 2. 게시판 & 카테고리 관리 CRUD 테스트
        System.out.println("\n▶ 2. 게시판 및 카테고리 CRUD 테스트");
        Board newBoard = new Board("이벤트게시판", "진행 중인 이벤트를 안내하는 게시판", 99);
        boolean isBoardCreated = boardDAO.createBoard(newBoard);
        System.out.println("  - 게시판 추가 결과: " + (isBoardCreated ? "✅ 성공" : "❌ 실패"));

        List<Board> boards = boardDAO.getAllBoards();
        long createdBoardId = 0L;
        for (Board b : boards) {
            if ("이벤트게시판".equals(b.getBoardName())) {
                createdBoardId = b.getBoardId();
                break;
            }
        }
        System.out.println("  - 신규 생성된 게시판 ID: #" + createdBoardId);

        if (createdBoardId > 0) {
            Category newCategory = new Category(createdBoardId, "진행중이벤트", 1);
            boolean isCategoryCreated = categoryDAO.createCategory(newCategory);
            System.out.println("  - 카테고리 추가 결과: " + (isCategoryCreated ? "✅ 성공" : "❌ 실패"));

            List<Category> categories = categoryDAO.getCategoriesByBoardId(createdBoardId);
            System.out.println("  - 해당 게시판의 카테고리 수: " + categories.size() + "개");

            // 카테고리 정리
            if (!categories.isEmpty()) {
                categoryDAO.deleteCategory(categories.get(0).getCategoryId());
            }
            // 신규 게시판 삭제 (기본 1번 게시판이 아닌 방금 만든 게시판만 삭제)
            boardDAO.deleteBoard(createdBoardId);
        }

        // 3. 공지사항 관리 CRUD 테스트
        System.out.println("\n▶ 3. 공지사항 등록 및 목록 조회 테스트");
        Notice testNotice = new Notice("8월 서버 점검 안내", "<p>8월 30일 새벽 점검이 진행됩니다.</p>", true);
        boolean isNoticeCreated = noticeDAO.createNotice(testNotice);
        System.out.println("  - 공지사항 등록 결과: " + (isNoticeCreated ? "✅ 성공" : "❌ 실패"));

        List<Notice> notices = noticeDAO.getNotices(0, 5);
        System.out.println("  - 공지사항 목록 조회: " + notices.size() + "건 조회됨");
        for (Notice n : notices) {
            System.out.println("    [#" + n.getNoticeId() + "] " + (n.isPinned() ? "📌[고정] " : "") + n.getTitle() + " (" + n.getCreatedAt() + ")");
        }

        if (!notices.isEmpty()) {
            noticeDAO.deleteNotice(notices.get(0).getNoticeId());
        }

        // 4. 신고 목록 필터링 조회 테스트
        System.out.println("\n▶ 4. 신고 목록 조건별 필터링 조회 테스트");
        List<ReportDTO> filteredReports = adminDAO.getFilteredReports("PENDING", "POST", 0, 5);
        System.out.println("  - 대기 중인 게시글 신고 건수: " + filteredReports.size() + "건");

        System.out.println("\n==================================================");
        System.out.println("🏁 관리자 신규 기능 통합 테스트 완료!");
        System.out.println("==================================================");
    }
}