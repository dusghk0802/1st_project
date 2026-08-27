package project.service;

import project.dto.User;

/**
 * 로그인 인증 처리 결과를 담아 반환하는 클래스
 */
public class LoginResult {
    private final boolean success;
    private final LoginStatus status;
    private final User user;

    private LoginResult(boolean success, LoginStatus status, User user) {
        this.success = success;
        this.status = status;
        this.user = user;
    }

    // 성공 시 호출하는 팩토리 메서드
    public static LoginResult success(User user) {
        return new LoginResult(true, LoginStatus.SUCCESS, user);
    }

    // 실패 시 호출하는 팩토리 메서드
    public static LoginResult fail(LoginStatus status) {
        return new LoginResult(false, status, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return status.getMessage();
    }

    public User getUser() {
        return user;
    }
}