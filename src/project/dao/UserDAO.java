package project.dao;

import project.common.DBConnection;
import project.dto.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /**
     * 회원가입 데이터 저장
     */
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (login_id, password_hash, nickname, email, role, status) " +
                "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getLoginId());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getNickname());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getRole());

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("회원가입 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 로그인 검증용 단건 조회 (아이디 기준 조회)
     */
    public User findByLoginId(String loginId) {
        String sql = "SELECT * FROM users WHERE login_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setNickname(rs.getString("nickname"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.out.println("회원 조회 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 아이디 중복 확인
     */
    public boolean isLoginIdDuplicate(String loginId) {
        String sql = "SELECT COUNT(*) FROM users WHERE login_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("아이디 중복 확인 오류: " + e.getMessage());
        }
        return true;
    }

    /**
     * 닉네임 중복 확인
     */
    public boolean isNicknameDuplicate(String nickname) {
        String sql = "SELECT COUNT(*) FROM users WHERE nickname = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("닉네임 중복 확인 오류: " + e.getMessage());
        }
        return true;
    }
    /**
     * [마이페이지] 회원 고유 번호(PK)로 회원 정보 조회
     */
    public User findByUserId(long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setNickname(rs.getString("nickname"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.out.println("회원 번호 조회 중 SQL 오류: " + e.getMessage());
        }
        return null;
    }

    /**
     * [마이페이지] 닉네임 및 이메일 정보 수정
     */
    public boolean updateProfile(long userId, String newNickname, String newEmail) {
        String sql = "UPDATE users SET nickname = ?, email = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newNickname);
            pstmt.setString(2, newEmail);
            pstmt.setLong(3, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("프로필 수정 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * [마이페이지] 비밀번호 변경
     */
    public boolean updatePassword(long userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPassword);
            pstmt.setLong(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("비밀번호 변경 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }
    /** Keeps authored content and moderation history intact while disabling the account. */
    public boolean withdrawUser(long userId) {
        String sql = "UPDATE users SET status = 'WITHDRAWN' WHERE user_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Account withdrawal failed: " + e.getMessage());
            return false;
        }
    }
}
