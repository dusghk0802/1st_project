package project.service;

import project.dao.CommentDAO;
import project.dao.PostDAO;
import project.dao.UserDAO;
import project.dto.Comment;
import project.dto.Post;
import project.dto.User;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 마이페이지 비즈니스 로직을 처리하는 서비스 클래스
 */
public class MyPageService {

    private final UserDAO userDAO;
    private final PostDAO postDAO;
    private final CommentDAO commentDAO;

    // 닉네임 및 이메일 유효성 검사 정규표현식
    private static final Pattern NICK_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]{2,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PW_LENGTH = 8;
    private static final int MAX_PW_LENGTH = 20;

    // 기본 생성자
    public MyPageService() {
        this.userDAO = new UserDAO();
        this.postDAO = new PostDAO();
        this.commentDAO = new CommentDAO();
    }

    // 의존성 주입용 생성자
    public MyPageService(UserDAO userDAO, PostDAO postDAO, CommentDAO commentDAO) {
        this.userDAO = userDAO;
        this.postDAO = postDAO;
        this.commentDAO = commentDAO;
    }

    /**
     * 1. 내 프로필 정보 단건 조회
     */
    public User getMyProfile(long userId) {
        return userDAO.findByUserId(userId);
    }

    /**
     * 2. 프로필 수정 (닉네임 및 이메일 유효성 검사 + 중복 체크)
     */
    public String updateProfile(long userId, String newNickname, String newEmail) {
        if (newNickname == null || !NICK_PATTERN.matcher(newNickname.trim()).matches()) {
            return "닉네임은 한글, 영문, 숫자 2~10자로 입력해야 합니다.";
        }
        if (newEmail == null || !EMAIL_PATTERN.matcher(newEmail.trim()).matches()) {
            return "올바른 이메일 형식을 입력해 주세요.";
        }

        User currentUser = userDAO.findByUserId(userId);
        if (currentUser == null) {
            return "존재하지 않는 회원입니다.";
        }

        // 닉네임을 변경하는 경우에만 중복 검사 실행
        if (!newNickname.trim().equals(currentUser.getNickname())) {
            if (userDAO.isNicknameDuplicate(newNickname.trim())) {
                return "이미 사용 중인 닉네임입니다.";
            }
        }

        boolean success = userDAO.updateProfile(userId, newNickname.trim(), newEmail.trim());
        return success ? "SUCCESS" : "프로필 수정 처리에 실패했습니다.";
    }

    /**
     * 3. 비밀번호 변경 (현재 비밀번호 일치 확인 + 새 비밀번호 길이 및 확인 일치 검증)
     */
    public String changePassword(long userId, String currentPassword, String newPassword, String confirmNewPassword) {
        if (currentPassword == null || currentPassword.isEmpty() ||
                newPassword == null || newPassword.isEmpty()) {
            return "현재 비밀번호와 새 비밀번호를 모두 입력해 주세요.";
        }

        if (newPassword.length() < MIN_PW_LENGTH || newPassword.length() > MAX_PW_LENGTH) {
            return "새 비밀번호는 8~20자로 입력해야 합니다.";
        }

        if (!newPassword.equals(confirmNewPassword)) {
            return "새 비밀번호 확인이 일치하지 않습니다.";
        }

        User user = userDAO.findByUserId(userId);
        if (user == null) {
            return "존재하지 않는 회원입니다.";
        }

        // 현재 비밀번호와 일치하는지 대조
        if (!currentPassword.equals(user.getPasswordHash())) {
            return "현재 비밀번호가 일치하지 않습니다.";
        }

        boolean success = userDAO.updatePassword(userId, newPassword);
        return success ? "SUCCESS" : "비밀번호 변경 처리에 실패했습니다.";
    }

    /**
     * 4. 내가 작성한 게시글 목록 조회
     */
    public List<Post> getMyPosts(long userId) {
        return postDAO.getPostsByUserId(userId);
    }

    /**
     * 5. 내가 작성한 댓글 목록 조회
     */
    public List<Comment> getMyComments(long userId) {
        return commentDAO.getCommentsByUserId(userId);
    }
}