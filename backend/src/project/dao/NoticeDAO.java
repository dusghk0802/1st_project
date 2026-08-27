package project.dao;

import project.common.DBConnection;
import project.dto.Notice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoticeDAO {

    public boolean createNotice(Notice notice) {
        String sql = "INSERT INTO notices (title, content, is_pinned) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notice.getTitle());
            pstmt.setString(2, notice.getContent());
            pstmt.setString(3, notice.isPinned() ? "Y" : "N");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 공지사항 등록 오류: " + e.getMessage());
            return false;
        }
    }

    public List<Notice> getNotices(int offset, int limit) {
        List<Notice> list = new ArrayList<>();
        String sql = "SELECT notice_id, title, content, is_pinned, created_at " +
                "FROM notices " +
                "ORDER BY is_pinned DESC, notice_id DESC " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, offset);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Notice notice = new Notice();
                    notice.setNoticeId(rs.getLong("notice_id"));
                    notice.setTitle(rs.getString("title"));
                    notice.setContent(rs.getString("content"));
                    notice.setPinned("Y".equalsIgnoreCase(rs.getString("is_pinned")));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        String s = ts.toString();
                        notice.setCreatedAt(s.contains(".") ? s.substring(0, s.lastIndexOf('.')) : s);
                    } else {
                        notice.setCreatedAt("");
                    }
                    list.add(notice);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 공지사항 목록 조회 오류: " + e.getMessage());
        }
        return list;
    }

    public boolean updateNotice(Notice notice) {
        String sql = "UPDATE notices SET title = ?, content = ?, is_pinned = ? WHERE notice_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notice.getTitle());
            pstmt.setString(2, notice.getContent());
            pstmt.setString(3, notice.isPinned() ? "Y" : "N");
            pstmt.setLong(4, notice.getNoticeId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 공지사항 수정 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteNotice(long noticeId) {
        String sql = "DELETE FROM notices WHERE notice_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, noticeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 공지사항 삭제 오류: " + e.getMessage());
            return false;
        }
    }
    /**
     * 공지사항 상세 단건 조회
     */
    public Notice getNoticeById(long noticeId) {
        String sql = "SELECT notice_id, title, content, is_pinned, created_at FROM notices WHERE notice_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, noticeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Notice notice = new Notice();
                    notice.setNoticeId(rs.getLong("notice_id"));
                    notice.setTitle(rs.getString("title"));
                    notice.setContent(rs.getString("content"));
                    notice.setPinned("Y".equalsIgnoreCase(rs.getString("is_pinned")));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        String s = ts.toString();
                        notice.setCreatedAt(s.contains(".") ? s.substring(0, s.lastIndexOf('.')) : s);
                    } else {
                        notice.setCreatedAt("");
                    }
                    return notice;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 공지사항 단건 조회 오류: " + e.getMessage());
        }
        return null;
    }
}