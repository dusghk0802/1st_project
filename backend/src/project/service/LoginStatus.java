package project.service;

/**
 * 로그인 검증 결과 상태를 정의하는 Enum
 */
public enum LoginStatus {
    SUCCESS("로그인에 성공했습니다."),
    EMPTY_INPUT("아이디와 비밀번호를 모두 입력해 주세요."),
    INVALID_ID_FORMAT("아이디는 영문과 숫자 조합 4~12자여야 합니다."),
    INVALID_PASSWORD_FORMAT("비밀번호는 8~20자여야 합니다."),
    USER_NOT_FOUND("존재하지 않는 아이디입니다."),
    WRONG_PASSWORD("비밀번호가 일치하지 않습니다."),
    DB_ERROR("데이터베이스 처리 중 오류가 발생했습니다.");

    private final String message;

    LoginStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}