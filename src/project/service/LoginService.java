package project.service;

import project.dao.UserDAO;
import project.dto.User;

import java.util.regex.Pattern;
import project.security.PasswordHasher;

/**
 * 로그인 요청값 유효성 검사 및 DB 인증을 수행하는 서비스 클래스
 */
public class LoginService {

    private final UserDAO userDAO;

    // 회원가입 규칙과 동일한 아이디 정규식 (영문, 숫자 4~12자)
    // Oracle allows 30 characters and the registration form accepts up to that size.
    // Login must use the same rule; otherwise a successfully created account can never log in.
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,30}$");
    private static final int MIN_PW_LENGTH = 8;
    private static final int MAX_PW_LENGTH = 128;

    public LoginService() {
        this.userDAO = new UserDAO();
    }

    public LoginService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * 로그인 유효성 검사 및 계정 인증 메서드
     *
     * @param loginId  사용자가 입력한 아이디
     * @param password 사용자가 입력한 비밀번호
     * @return 검증 및 인증 결과(LoginResult)
     */
    public LoginResult authenticate(String loginId, String password) {

        // 1단계: 필수 입력값 누락 검사 (null 또는 공백)
        if (loginId == null || loginId.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return LoginResult.fail(LoginStatus.EMPTY_INPUT);
        }

        String trimmedId = loginId.trim();

        // 2단계: 아이디 형식 유효성 검사
        if (!ID_PATTERN.matcher(trimmedId).matches()) {
            return LoginResult.fail(LoginStatus.INVALID_ID_FORMAT);
        }

        // 3단계: 비밀번호 길이 유효성 검사
        if (password.length() < MIN_PW_LENGTH || password.length() > MAX_PW_LENGTH) {
            return LoginResult.fail(LoginStatus.INVALID_PASSWORD_FORMAT);
        }

        // 4단계: DB 조회 및 비밀번호 일치 확인
        try {
            User user = userDAO.findByLoginId(trimmedId);

            // 사용자가 존재하지 않는 경우
            if (user == null) {
                return LoginResult.fail(LoginStatus.USER_NOT_FOUND);
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                return LoginResult.fail(LoginStatus.USER_NOT_FOUND);
            }

            // 비밀번호 불일치 검사
            if (!PasswordHasher.matches(password, user.getPasswordHash())) {
                return LoginResult.fail(LoginStatus.WRONG_PASSWORD);
            }

            // 모든 검증 통과 -> 로그인 성공
            return LoginResult.success(user);

        } catch (Exception e) {
            System.err.println("로그인 처리 중 오류 발생: " + e.getMessage());
            return LoginResult.fail(LoginStatus.DB_ERROR);
        }
    }

}
