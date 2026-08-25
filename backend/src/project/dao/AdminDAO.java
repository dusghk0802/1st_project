package project.dao;

import project.common.DBConnection;
import java.sql.*;

public class AdminDAO {

    // 1. 관리자 통계 조회: 전체 게시글 수 확인
    public int getTotalPostCount() {
        String sql = "SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("전체 게시글 수 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // 2. 관리자 통계 조회: 전체 댓글 수 확인
    public int getTotalCommentCount() {
        String sql = "SELECT COUNT(*) FROM comments WHERE status = 'PUBLISHED'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("전체 댓글 수 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // 3. 관리자 권한으로 게시글 강제 삭제 (Soft Delete)
    public boolean deletePostByAdmin(long postId) {
        String sql = "UPDATE posts SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE post_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);
            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("관리자 게시글 강제 삭제 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 4. 관리자 권한으로 댓글 강제 삭제 (Soft Delete)
    public boolean deleteCommentByAdmin(long commentId) {
        String sql = "UPDATE comments SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE comment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, commentId);
            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("관리자 댓글 강제 삭제 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}