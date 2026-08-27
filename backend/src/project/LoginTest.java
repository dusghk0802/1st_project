package project;

import project.service.LoginResult;
import project.service.LoginService;

public class LoginTest {

    public static void main(String[] args) {
        // 로그인 비즈니스 로직 서비스 인스턴스 생성
        LoginService loginService = new LoginService();

        System.out.println("==================================================");
        System.out.println("🧪 [로그인 비즈니스 로직 단위 테스트 시작]");
        System.out.println("==================================================");

        // [테스트 1] 공백 및 빈 값 입력 검증
        runTestCase(loginService, "1. 빈 값 입력 테스트", "", "");

        // [테스트 2] 아이디 형식 오류 검증 (4자 미만 입력)
        runTestCase(loginService, "2. 아이디 형식 오류 테스트 (3글자)", "abc", "password123!");

        // [테스트 3] 비밀번호 길이 오류 검증 (8자 미만 입력)
        runTestCase(loginService, "3. 비밀번호 길이 오류 테스트 (4글자)", "testuser1", "1234");

        // [테스트 4] 데이터베이스에 존재하지 않는 아이디 검증
        runTestCase(loginService, "4. 미존재 아이디 테스트", "unknown9999", "password123!");

        // [테스트 5] DB에 등록된 아이디지만 비밀번호가 틀린 경우
        // (앞서 회원가입했던 실제 아이디가 있다면 아래 첫 번째 인자에 넣어보세요!)
        runTestCase(loginService, "5. 비밀번호 불일치 테스트", "admin", "wrongpassword123!");

        System.out.println("\n==================================================");
        System.out.println("🏁 모든 테스트 시나리오 검증 완료!");
        System.out.println("==================================================");
    }

    /**
     * 개별 테스트 케이스를 실행하고 결과를 예쁘게 출력해 주는 헬퍼 메서드
     */
    private static void runTestCase(LoginService service, String testTitle, String id, String pw) {
        System.out.println("\n▶ [" + testTitle + "]");
        System.out.println("  입력 데이터 -> 아이디: \"" + id + "\", 비밀번호: \"" + pw + "\"");

        // LoginService의 검증 메서드 호출
        LoginResult result = service.authenticate(id, pw);

        // 결과에 따른 출력 분기
        if (result.isSuccess()) {
            System.out.println("  결과: ✅ 성공!");
            System.out.println("  메시지: " + result.getMessage());
            System.out.println("  사용자 닉네임: " + result.getUser().getNickname());
            System.out.println("  사용자 권한: " + result.getUser().getRole());
        } else {
            System.out.println("  결과: ❌ 차단/실패 (정상적으로 검증됨)");
            System.out.println("  상태 코드: " + result.getStatus());
            System.out.println("  안내 메시지: " + result.getMessage());
        }
    }
}