package project.dao;

import project.common.DBConnection;
import project.dto.ReportDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자(Admin) 데이터베이스 접근 객체 (DAO)
 * DB 컬럼 기준: reporter_id 사용
 */
public class AdminDAO {

    public int getTotalPostCount() {
        String sql = "SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("전체 게시글 수 조회 오류: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalCommentCount() {
        String sql = "SELECT COUNT(*) FROM comments WHERE status = 'PUBLISHED'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("전체 댓글 수 조회 오류: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("총 회원 수 조회 오류: " + e.getMessage());
        }
        return 0;
    }

    public int getPendingReportCount() {
        String sql = "SELECT COUNT(*) FROM reports WHERE status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("미처리 신고 건수 조회 오류: " + e.getMessage());
        }
        return 0;
    }

    public boolean deletePostByAdmin(long postId) {
        String sql = "UPDATE posts SET status = 'DELETED' WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("관리자 게시글 강제 삭제 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCommentByAdmin(long commentId) {
        String sql = "UPDATE comments SET status = 'DELETED' WHERE comment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("관리자 댓글 강제 삭제 오류: " + e.getMessage());
            return false;
        }
    }

    public List<ReportDTO> getAllReports() {
        List<ReportDTO> reportList = new ArrayList<>();
        String sql = "SELECT r.report_id, r.target_type, r.target_id, r.reporter_id, r.reason, r.status, r.created_at, u.nickname " +
                "FROM reports r " +
                "JOIN users u ON r.reporter_id = u.user_id " +
                "ORDER BY r.report_id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reportList.add(mapResultSetToReportDTO(rs));
            }
        } catch (SQLException e) {
            System.err.println("신고 목록 조회 중 SQL 오류: " + e.getMessage());
        }
        return reportList;
    }

    /**
     * 신고 필터링 페이징 조회 (reporter_id 컬럼 사용)
     */
    public List<ReportDTO> getFilteredReports(String status, String targetType, int offset, int limit) {
        List<ReportDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT r.report_id, r.target_type, r.target_id, r.reporter_id, r.reason, r.status, r.created_at, u.nickname ")
                .append("FROM reports r ")
                .append("JOIN users u ON r.reporter_id = u.user_id WHERE 1=1 ");

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND UPPER(r.status) = UPPER('").append(status.trim()).append("') ");
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            sql.append("AND UPPER(r.target_type) = UPPER('").append(targetType.trim()).append("') ");
        }
        sql.append("ORDER BY r.report_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setInt(1, offset);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReportDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("신고 필터 조회 오류: " + e.getMessage());
        }
        return list;
    }

    public boolean updateReportStatus(long reportId, String reportStatus) {
        String sql = "UPDATE reports SET status = ? WHERE report_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reportStatus);
            pstmt.setLong(2, reportId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("신고 상태 업데이트 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTargetStatus(String targetType, long targetId, String targetStatus) {
        String tableName = "POST".equalsIgnoreCase(targetType) ? "posts" : "comments";
        String idColumn = "POST".equalsIgnoreCase(targetType) ? "post_id" : "comment_id";
        String sql = "UPDATE " + tableName + " SET status = ? WHERE " + idColumn + " = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetStatus);
            pstmt.setLong(2, targetId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("신고 대상 상태 업데이트 오류: " + e.getMessage());
            return false;
        }
    }

    private ReportDTO mapResultSetToReportDTO(ResultSet rs) throws SQLException {
        ReportDTO dto = new ReportDTO();
        dto.setReportId(rs.getLong("report_id"));
        dto.setTargetType(rs.getString("target_type"));
        dto.setTargetId(rs.getLong("target_id"));
        dto.setUserId(rs.getLong("reporter_id"));
        dto.setReporterId(rs.getLong("reporter_id"));
        dto.setReason(rs.getString("reason"));
        dto.setStatus(rs.getString("status"));
        dto.setReporterNickname(rs.getString("nickname"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            String dateStr = ts.toString();
            if (dateStr.contains(".")) {
                dateStr = dateStr.substring(0, dateStr.lastIndexOf('.'));
            }
            dto.setCreatedAt(dateStr);
        } else {
            dto.setCreatedAt("");
        }

        return dto;
    }
    /**
     * [관리자 회원 관리] 전체 회원 목록 조회 (페이징 지원)
     */
    public List<project.dto.User> getAllUsers(int offset, int limit) {
        List<project.dto.User> list = new ArrayList<>();
        String sql = "SELECT user_id, login_id, nickname, email, role, status, created_at " +
                "FROM users ORDER BY user_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, offset);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    project.dto.User user = new project.dto.User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setNickname(rs.getString("nickname"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    list.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 관리자 회원 목록 조회 오류: " + e.getMessage());
        }
        return list;
    }

    /**
     * [관리자 회원 관리] 회원 계정 상태 변경 (ACTIVE, SUSPENDED 등)
     */
    public boolean updateUserStatus(long userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setLong(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 회원 상태 변경 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * [관리자 회원 관리] 회원 강제 탈퇴
     */
    public boolean deleteUserByAdmin(long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 관리자 회원 삭제 오류: " + e.getMessage());
            return false;
        }
    }
}
