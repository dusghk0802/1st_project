package project.dao;

import project.common.DBConnection;
import java.sql.*;

public class ReportDAO {

    // 1. 중복 신고 여부 확인 (정식 스키마: reporter_id 사용)
    public boolean hasUserReported(String targetType, long targetId, long reporterId) {
        String sql = "SELECT COUNT(*) FROM reports WHERE target_type = ? AND target_id = ? AND reporter_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, targetType);
            pstmt.setLong(2, targetId);
            pstmt.setLong(3, reporterId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("신고 중복 확인 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 2. 신고 등록 (정식 스키마 반영)
    public boolean reportTarget(String targetType, long targetId, long reporterId, String reason) {
        if (hasUserReported(targetType, targetId, reporterId)) {
            System.out.println("⚠️ 이미 신고한 대상입니다.");
            return false;
        }

        String sql = "INSERT INTO reports (reporter_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, reporterId);
            pstmt.setString(2, targetType);
            pstmt.setLong(3, targetId);
            pstmt.setString(4, reason);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("신고 등록 중 SQL 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}