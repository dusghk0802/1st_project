package project;

import project.dao.AdminDAO;
import project.dao.CommentDAO;
import project.dao.PostDAO;
import project.dao.UserDAO;
import project.dto.Comment;
import project.dto.Post;
import project.dto.User;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        PostDAO postDAO = new PostDAO();

        String testId = "moviestar";
        String testNick = "시네필";

        System.out.println("=== 1. 회원 및 인증 테스트 ===");
        if (userDAO.isLoginIdDuplicate(testId)) {
            System.out.println("❌ 이미 사용 중인 아이디입니다. 기존 계정으로 로그인을 진행합니다.");
        } else {
            System.out.println("✅ 사용 가능한 아이디입니다. 회원가입을 진행합니다.");
            User newUser = new User(testId, "secure123!", testNick, "star@movie.com", "USER");
            userDAO.registerUser(newUser);
        }

        // 로그인하여 실제 user_id(PK) 가져오기
        User loginUser = userDAO.login(testId, "secure123!");
        if (loginUser != null) {
            System.out.println("🔓 로그인 성공! 사용자 번호(ID): " + loginUser.getUserId() + " | 닉네임: " + loginUser.getNickname());

            System.out.println("\n=== 2. 커뮤니티 게시글 테스트 ===");
            // 방금 로그인한 유저의 ID(loginUser.getUserId())를 작성자 번호로 사용합니다!
            Post newPost = new Post(1L, loginUser.getUserId(), "인셉션 정말 명작이네요!", "꿈 안에서 꿈을 꾸는 연출이 대단했습니다.");
            boolean isCreated = postDAO.createPost(newPost);

            if (isCreated) {
                System.out.println("📝 게시글 작성 성공!");
            }

            // 게시글 목록 조회
            List<Post> posts = postDAO.getAllPosts();
            System.out.println("\n--- [ 영화 커뮤니티 게시글 목록 ] ---");
            for (Post p : posts) {
                System.out.println("글번호: " + p.getPostId() +
                        " | 제목: " + p.getTitle() +
                        " | 작성자: " + p.getAuthorNickname() +
                        " | 조회수: " + p.getViewCount());
            }
        } else {
            System.out.println("🔒 로그인 실패...");
        }
        // 3. 게시글 상세 조회 및 조회수 증가 테스트
        long targetPostId = 1L; // 테스트할 글 번호 (DB 상황에 맞게 조절 가능)
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
            System.out.println("❌ 해당 게시글을 찾을 수 없거나 삭제된 글입니다.");
        }
        // 4. 댓글 및 대댓글 테스트 추가
        CommentDAO commentDAO = new CommentDAO();

        // 4-1. 루트 댓글 작성 (1번 글에, 로그인한 유저가 작성)
        Comment rootComment = new Comment(targetPostId, loginUser.getUserId(), "정말 공감되는 리뷰네요! 잘 읽었습니다.");
        commentDAO.createComment(rootComment);
        System.out.println("💬 루트 댓글 작성 완료!");

        // 방금 작성한 댓글의 ID를 가져오거나 테스트를 위해, 목록을 조회해서 대댓글을 달아볼 수 있습니다.
        // 여기서는 테스트로 목록을 조회해 봅니다.
        List<Comment> comments = commentDAO.getCommentsByPostId(targetPostId);
        if (!comments.isEmpty()) {
            long firstCommentId = comments.get(0).getCommentId();

            // 4-2. 대댓글(답글) 작성 (방금 달린 첫 번째 댓글에 답글 달기)
            Comment replyComment = new Comment(targetPostId, loginUser.getUserId(), firstCommentId, "저도 그렇게 생각합니다 ㅎㅎ");
            commentDAO.createComment(replyComment);
            System.out.println("↳ 💬 대댓글(답글) 작성 완료!");
        }

        // 4-3. 최종 댓글 목록 출력
        System.out.println("\n--- [ 게시글 댓글 & 대댓글 목록 ] ---");
        List<Comment> finalComments = commentDAO.getCommentsByPostId(targetPostId);
        for (Comment c : finalComments) {
            String prefix = (c.getParentCommentId() == null) ? "[댓글] " : "  ㄴ [답글] ";
            System.out.println(prefix + c.getAuthorNickname() + ": " + c.getContent() + " (" + c.getCreatedAt() + ")");
        }
        // 5. 게시글 수정 및 삭제 테스트
        System.out.println("\n=== 5. 게시글 수정 및 삭제 테스트 ===");
        targetPostId = 1L; // 테스트할 글 번호

        // 5-1. 게시글 수정 테스트 (작성자 본인인 경우)
        boolean isUpdated = postDAO.updatePost(targetPostId, loginUser.getUserId(),
                "인셉션 수정된 제목입니다!", "본문 내용도 수정되었습니다.");
        if (isUpdated) {
            System.out.println("✏️ 게시글 수정 성공!");

            // 수정된 내용 확인
            Post updatedPost = postDAO.getPostById(targetPostId);
            if (updatedPost != null) {
                System.out.println("수정된 제목: " + updatedPost.getTitle());
            }
        } else {
            System.out.println("❌ 게시글 수정 실패 (권한이 없거나 글이 존재하지 않습니다.)");
        }

        // 5-2. 게시글 삭제 테스트
        boolean isDeleted = postDAO.deletePost(targetPostId, loginUser.getUserId());
        if (isDeleted) {
            System.out.println("🗑️ 게시글 삭제 성공!");
        } else {
            System.out.println("❌ 게시글 삭제 실패...");
        }
        // 6. 게시글 검색 및 페이징 테스트
        System.out.println("\n=== 6. 게시글 검색 및 페이징 테스트 ===");
        String keyword = "인셉션"; // 검색할 키워드
        int pageOffset = 0;   // 첫 번째 페이지 (0개 건너뜀)
        int pageLimit = 10;   // 10개씩 보기

        List<Post> searchResults = postDAO.searchPosts(keyword, pageOffset, pageLimit);
        System.out.println("🔍 '" + keyword + "' 검색 결과 (" + searchResults.size() + "건):");
        for (Post p : searchResults) {
            System.out.println("글번호: " + p.getPostId() + " | 제목: " + p.getTitle() + " | 작성자: " + p.getAuthorNickname());
        }
        // 7. 게시글 좋아요(추천) 토글 테스트
        System.out.println("\n=== 7. 게시글 좋아요 기능 테스트 == / 글번호: 1");
        targetPostId = 1L;
        long currentUserId = loginUser.getUserId();

        // 첫 번째 호출: 좋아요 추가
        boolean isLiked1 = postDAO.toggleLike(targetPostId, currentUserId);
        System.out.println("❤️ 좋아요 상태: " + (isLiked1 ? "좋아요 등록됨!" : "좋아요 취소됨!"));

        // 게시글 정보 다시 조회해서 like_count 확인
        Post likedPost = postDAO.getPostById(targetPostId);
        if (likedPost != null) {
            System.out.println("현재 게시글 추천수(like_count): " + likedPost.getLikeCount());
        }

        // 두 번째 호출: 좋아요 취소 (토글 기능 확인)
        boolean isLiked2 = postDAO.toggleLike(targetPostId, currentUserId);
        System.out.println("🤍 좋아요 상태: " + (isLiked2 ? "좋아요 등록됨!" : "좋아요 취소됨!"));

        // 8. 관리자 기능 테스트
        System.out.println("\n=== 8. 관리자 페이지 및 기능 테스트 ===");
        AdminDAO adminDAO = new AdminDAO();

        // 8-1. 시스템 통계 확인 (전체 게시물 수, 전체 댓글 수)
        int totalPosts = adminDAO.getTotalPostCount();
        int totalComments = adminDAO.getTotalCommentCount();
        System.out.println("📊 [관리자 통계 현황]");
        System.out.println("- 현재 게시된 총 게시물 수: " + totalPosts + "개");
        System.out.println("- 현재 등록된 총 댓글 수: " + totalComments + "개");

        // 8-2. 관리자 권한으로 특정 게시글 강제 삭제 테스트 (예: 2번 글)
        long targetPostIdForAdmin = 2L;
        boolean isPostDeletedByAdmin = adminDAO.deletePostByAdmin(targetPostIdForAdmin);
        if (isPostDeletedByAdmin) {
            System.out.println("🛡️ [관리자] " + targetPostIdForAdmin + "번 게시글 강제 삭제 완료!");
        }

        // 8-3. 관리자 권한으로 특정 댓글 강제 삭제 테스트 (예: 1번 댓글)
        long targetCommentIdForAdmin = 1L;
        boolean isCommentDeletedByAdmin = adminDAO.deleteCommentByAdmin(targetCommentIdForAdmin);
        if (isCommentDeletedByAdmin) {
            System.out.println("🛡️ [관리자] " + targetCommentIdForAdmin + "번 댓글 강제 삭제 완료!");
        }
    }


}