package project.dao;

import project.common.DBConnection;
import project.dto.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

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

    public User login(String loginId, String password) {
        String sql = "SELECT * FROM users WHERE login_id = ? AND password_hash = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loginId);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setNickname(rs.getString("nickname"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.out.println("로그인 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

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
}