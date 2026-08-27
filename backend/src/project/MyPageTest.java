package project;

import project.dto.Comment;
import project.dto.Post;
import project.dto.User;
import project.service.MyPageService;

import java.util.List;

/**
 * 마이페이지(MyPage) 백엔드 비즈니스 로직 단위 테스트 클래스
 */
public class MyPageTest {

    public static void main(String[] args) {
        // 마이페이지 비즈니스 로직을 처리하는 서비스 인스턴스 생성
        MyPageService myPageService = new MyPageService();

        // 💡 테스트할 회원의 user_id (현재 DB의 users 테이블에 존재하는 번호로 설정)
        long targetUserId = 1L;

        System.out.println("==================================================");
        System.out.println("👤 [마이페이지 백엔드 기능 단위 테스트 시작]");
        System.out.println("==================================================");

        // ==========================================
        // 1. 내 프로필 조회 테스트
        // ==========================================
        System.out.println("\n▶ 1. 내 프로필 정보 조회");
        User myProfile = myPageService.getMyProfile(targetUserId);

        if (myProfile != null) {
            System.out.println("  ✅ 조회 성공!");
            System.out.println("  - 회원번호(PK) : " + myProfile.getUserId());
            System.out.println("  - 로그인 아이디 : " + myProfile.getLoginId());
            System.out.println("  - 현재 닉네임   : " + myProfile.getNickname());
            System.out.println("  - 이메일 주소   : " + myProfile.getEmail());
            System.out.println("  - 계정 권한     : " + myProfile.getRole());
            System.out.println("  - 계정 상태     : " + myProfile.getStatus());
        } else {
            System.out.println("  ❌ 조회 실패: " + targetUserId + "번 회원이 데이터베이스에 존재하지 않습니다.");
            System.out.println("  (DB에 존재하는 user_id 번호로 targetUserId 값을 변경해 주세요.)");
            return; // 회원이 없으면 이후 수정/조회 테스트 진행 불가하므로 종료
        }

        // ==========================================
        // 2. 프로필 수정 테스트 (닉네임, 이메일)
        // ==========================================
        System.out.println("\n▶ 2. 프로필 정보 수정 테스트 (닉네임 / 이메일)");
        String newNickname = "시네필수정";
        String newEmail = "updated_star@movie.com";

        String updateResult = myPageService.updateProfile(targetUserId, newNickname, newEmail);
        if ("SUCCESS".equals(updateResult)) {
            System.out.println("  ✅ 프로필 수정 성공!");
            User updatedUser = myPageService.getMyProfile(targetUserId);
            System.out.println("  - 변경된 닉네임: " + updatedUser.getNickname());
            System.out.println("  - 변경된 이메일: " + updatedUser.getEmail());
        } else {
            System.out.println("  ❌ 프로필 수정 실패 사유: " + updateResult);
        }

        // ==========================================
        // 3. 비밀번호 변경 테스트
        // ==========================================
        System.out.println("\n▶ 3. 비밀번호 변경 테스트");
        String currentPw = "secure123!";       // 현재 비밀번호
        String newPw = "newSecure456!";        // 변경할 새 비밀번호
        String confirmNewPw = "newSecure456!"; // 새 비밀번호 확인

        // 3-1. 비밀번호 변경 시도
        String pwChangeResult = myPageService.changePassword(targetUserId, currentPw, newPw, confirmNewPw);
        if ("SUCCESS".equals(pwChangeResult)) {
            System.out.println("  ✅ 새 비밀번호로 변경 성공!");

            // 3-2. 다음 테스트 및 일관성을 위해 원래 비밀번호로 원복 복구
            myPageService.changePassword(targetUserId, newPw, currentPw, currentPw);
            System.out.println("  🔄 다음 테스트를 위해 기존 비밀번호(" + currentPw + ")로 원복 완료");
        } else {
            System.out.println("  ❌ 비밀번호 변경 실패 사유: " + pwChangeResult);
        }

        // ==========================================
        // 4. 내가 작성한 게시글 목록 조회 테스트
        // ==========================================
        System.out.println("\n▶ 4. 내가 작성한 게시글 목록 조회");
        List<Post> myPosts = myPageService.getMyPosts(targetUserId);

        System.out.println("  📋 총 작성한 게시글 수: " + myPosts.size() + "건");
        if (myPosts.isEmpty()) {
            System.out.println("  (작성한 게시글이 없습니다.)");
        } else {
            for (Post p : myPosts) {
                System.out.println("  - [글번호: " + p.getPostId() + "] 제목: " + p.getTitle() +
                        " | 조회수: " + p.getViewCount() +
                        " | 추천수: " + p.getLikeCount() +
                        " | 작성일: " + p.getCreatedAt());
            }
        }

        // ==========================================
        // 5. 내가 작성한 댓글 목록 조회 테스트
        // ==========================================
        System.out.println("\n▶ 5. 내가 작성한 댓글 목록 조회");
        List<Comment> myComments = myPageService.getMyComments(targetUserId);

        System.out.println("  💬 총 작성한 댓글 수: " + myComments.size() + "건");
        if (myComments.isEmpty()) {
            System.out.println("  (작성한 댓글이 없습니다.)");
        } else {
            for (Comment c : myComments) {
                String typePrefix = (c.getParentCommentId() == null) ? "[댓글]" : "  ㄴ[답글]";
                System.out.println("  - " + typePrefix + " [댓글번호: " + c.getCommentId() + " ➔ 원본글: #" + c.getPostId() + "] 내용: \"" +
                        c.getContent() + "\" (" + c.getCreatedAt() + ")");
            }
        }

        System.out.println("\n==================================================");
        System.out.println("🏁 [마이페이지 모든 기능 단위 테스트 완료]");
        System.out.println("==================================================");
    }
}