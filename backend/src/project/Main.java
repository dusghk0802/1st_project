package project;

import project.dao.*;
import project.dto.Comment;
import project.dto.Post;
import project.dto.User;
import project.service.LoginResult;
import project.service.LoginService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        PostDAO postDAO = new PostDAO();
        LoginService loginService = new LoginService(userDAO);

        String testId = "moviestar";
        String testNick = "시네필";
        String testPassword = "secure123!";

        // ==========================================
        // 1. 회원가입 및 로그인 인증 테스트
        // ==========================================
        System.out.println("=== 1. 회원 및 인증 테스트 ===");
        if (userDAO.isLoginIdDuplicate(testId)) {
            System.out.println("❌ 이미 사용 중인 아이디입니다. 기존 계정으로 로그인을 진행합니다.");
        } else {
            System.out.println("✅ 사용 가능한 아이디입니다. 회원가입을 진행합니다.");
            User newUser = new User(testId, testPassword, testNick, "star@movie.com", "USER");
            userDAO.registerUser(newUser);
        }

        LoginResult loginResult = loginService.authenticate(testId, testPassword);
        User loginUser = null;

        if (loginResult.isSuccess()) {
            loginUser = loginResult.getUser();
            System.out.println("🔓 로그인 성공! 사용자 번호(ID): " + loginUser.getUserId() + " | 닉네임: " + loginUser.getNickname());
        } else {
            System.out.println("🔒 로그인 실패: " + loginResult.getMessage());
            return;
        }

        // ==========================================
        // 2. 커뮤니티 게시글 등록 및 목록 조회
        // ==========================================
        System.out.println("\n=== 2. 커뮤니티 게시글 테스트 ===");
        Post newPost = new Post(1L, loginUser.getUserId(), "인셉션 정말 명작이네요!", "꿈 안에서 꿈을 꾸는 연출이 대단했습니다.");
        boolean isCreated = postDAO.createPost(newPost);

        if (isCreated) {
            System.out.println("📝 게시글 작성 성공!");
        }

        List<Post> posts = postDAO.getAllPosts();
        System.out.println("\n--- [ 영화 커뮤니티 게시글 목록 ] ---");
        for (Post p : posts) {
            System.out.println("글번호: " + p.getPostId() +
                    " | 제목: " + p.getTitle() +
                    " | 작성자: " + p.getAuthorNickname() +
                    " | 댓글수: " + p.getCommentCount() +
                    " | 조회수: " + p.getViewCount());
        }

        // DB에 존재하는 첫 번째 게시글 ID를 타겟으로 지정 (글이 없을 경우 기본 1L)
        long targetPostId = posts.isEmpty() ? 1L : posts.get(0).getPostId();
        long currentUserId = loginUser.getUserId();

        // ==========================================
        // 3. 게시글 상세 보기 (조회수 증가)
        // ==========================================
        System.out.println("\n=== 3. 게시글 상세 보기 테스트 (글번호: " + targetPostId + ") ===");
        Post detailPost = postDAO.getPostById(targetPostId);
        if (detailPost != null) {
            System.out.println("----------------------------------------");
            System.out.println("제목: " + detailPost.getTitle());
            System.out.println("작성자: " + detailPost.getAuthorNickname() + " | 조회수: " + detailPost.getViewCount() + " | 추천수: " + detailPost.getLikeCount());
            System.out.println("작성일: " + detailPost.getCreatedAt());
            System.out.println("----------------------------------------");
            System.out.println("[본문]\n" + detailPost.getContent());
            System.out.println("----------------------------------------");
        } else {
            System.out.println("❌ 해당 게시글을 찾을 수 없습니다.");
        }

        // ==========================================
        // 4. 댓글 및 대댓글 작성 테스트
        // ==========================================
        System.out.println("\n=== 4. 댓글 및 대댓글 테스트 ===");
        CommentDAO commentDAO = new CommentDAO();

        Comment rootComment = new Comment(targetPostId, currentUserId, "정말 공감되는 리뷰네요! 잘 읽었습니다.");
        commentDAO.createComment(rootComment);
        System.out.println("💬 루트 댓글 작성 완료!");

        List<Comment> comments = commentDAO.getCommentsByPostId(targetPostId);
        if (!comments.isEmpty()) {
            long firstCommentId = comments.get(0).getCommentId();
            Comment replyComment = new Comment(targetPostId, currentUserId, firstCommentId, "저도 그렇게 생각합니다 ㅎㅎ");
            commentDAO.createComment(replyComment);
            System.out.println("↳ 💬 대댓글(답글) 작성 완료!");
        }

        System.out.println("\n--- [ 게시글 댓글 & 대댓글 목록 ] ---");
        List<Comment> finalComments = commentDAO.getCommentsByPostId(targetPostId);
        for (Comment c : finalComments) {
            String prefix = (c.getParentCommentId() == null) ? "[댓글] " : "  ㄴ [답글] ";
            System.out.println(prefix + c.getAuthorNickname() + ": " + c.getContent() + " (" + c.getCreatedAt() + ")");
        }

        // ==========================================
        // 5. 게시글 검색 및 페이징 테스트
        // ==========================================
        System.out.println("\n=== 5. 게시글 검색 및 페이징 테스트 ===");
        String keyword = "인셉션";
        List<Post> searchResults = postDAO.searchPosts(keyword, 0, 10);
        System.out.println("🔍 '" + keyword + "' 검색 결과 (" + searchResults.size() + "건):");
        for (Post p : searchResults) {
            System.out.println("글번호: " + p.getPostId() + " | 제목: " + p.getTitle() + " | 작성자: " + p.getAuthorNickname());
        }

        // ==========================================
        // 6. 게시글 좋아요(추천) 토글 테스트 ✨ (삭제 전에 실행!)
        // ==========================================
        System.out.println("\n=== 6. 게시글 좋아요 기능 테스트 == / 글번호: " + targetPostId);

        // 1회차: 좋아요 추가
        boolean isLiked1 = postDAO.toggleLike(targetPostId, currentUserId);
        System.out.println("❤️ 1차 토글 결과: " + (isLiked1 ? "좋아요 등록됨!" : "좋아요 취소됨!"));

        Post likedPost1 = postDAO.getPostById(targetPostId);
        if (likedPost1 != null) {
            System.out.println("  현재 추천수(like_count): " + likedPost1.getLikeCount());
        }

        // 2회차: 좋아요 취소
        boolean isLiked2 = postDAO.toggleLike(targetPostId, currentUserId);
        System.out.println("🤍 2차 토글 결과: " + (isLiked2 ? "좋아요 등록됨!" : "좋아요 취소됨!"));

        Post likedPost2 = postDAO.getPostById(targetPostId);
        if (likedPost2 != null) {
            System.out.println("  현재 추천수(like_count): " + likedPost2.getLikeCount());
        }

        // ==========================================
        // 7. 관리자 기능 테스트
        // ==========================================
        System.out.println("\n=== 7. 관리자 페이지 및 기능 테스트 ===");
        AdminDAO adminDAO = new AdminDAO();
        int totalPosts = adminDAO.getTotalPostCount();
        int totalComments = adminDAO.getTotalCommentCount();
        System.out.println("📊 [관리자 통계] 총 게시물 수: " + totalPosts + "개 | 총 댓글 수: " + totalComments + "개");

        // ==========================================
        // 8. 신고 기능 테스트
        // ==========================================
        System.out.println("\n=== 8. 신고 기능 테스트 ===");
        ReportDAO reportDAO = new ReportDAO();
        boolean isPostReported = reportDAO.reportTarget("POST", targetPostId, currentUserId, "테스트 신고 사유입니다.");
        System.out.println(isPostReported ? "🚨 신고 접수 성공!" : "ℹ️ 이미 접수되었거나 신고 처리 완료");

        // ==========================================
        // 9. 게시글 수정 및 삭제 테스트 (마지막 정리 단계)
        // ==========================================
        System.out.println("\n=== 9. 게시글 수정 및 삭제 테스트 ===");
        boolean isUpdated = postDAO.updatePost(targetPostId, currentUserId,
                "인셉션 수정된 제목입니다!", "본문 내용도 수정되었습니다.");
        if (isUpdated) {
            System.out.println("✏️ 게시글 수정 성공!");
        }

        // 테스트 글 삭제 진행
        boolean isDeleted = postDAO.deletePost(targetPostId, currentUserId);
        if (isDeleted) {
            System.out.println("🗑️ " + targetPostId + "번 게시글 삭제 성공!");
        } else {
            System.out.println("❌ 게시글 삭제 실패...");
        }

        System.out.println("\n==================================================");
        System.out.println("🏁 Main 전체 시나리오 통합 테스트 완료!");
        System.out.println("==================================================");
    }
}