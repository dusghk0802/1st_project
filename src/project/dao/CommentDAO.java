package project.dao;

import project.common.DBConnection;
import project.dto.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 및 대댓글(Comment) 데이터베이스 접근 객체 (DAO)
 * DB 컬럼명: author_id 기준
 */
public class CommentDAO {

    /**
     * 1. 댓글 / 대댓글 등록
     */
    public boolean createComment(Comment comment) {
        String sql = "INSERT INTO comments (post_id, author_id, parent_comment_id, content, status) " +
                "VALUES (?, ?, ?, ?, 'PUBLISHED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, comment.getPostId());
            pstmt.setLong(2, comment.getUserId() > 0 ? comment.getUserId() : comment.getAuthorId());

            if (comment.getParentCommentId() != null && comment.getParentCommentId() > 0) {
                pstmt.setLong(3, comment.getParentCommentId());
            } else {
                pstmt.setNull(3, Types.BIGINT);
            }

            pstmt.setString(4, comment.getContent());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 댓글 등록 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 2. 특정 게시글의 전체 댓글 목록 조회 (작성자 닉네임 포함)
     */
    public List<Comment> getCommentsByPostId(long postId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.comment_id, c.post_id, c.author_id, c.parent_comment_id, " +
                "c.content, c.status, c.created_at, u.nickname " +
                "FROM comments c " +
                "JOIN users u ON c.author_id = u.user_id " +
                "WHERE c.post_id = ? AND c.status = 'PUBLISHED' " +
                "ORDER BY COALESCE(c.parent_comment_id, c.comment_id) ASC, c.comment_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToComment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 게시글 댓글 목록 조회 중 SQL 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 3. 댓글 단건 조회
     */
    public Comment getCommentById(long commentId) {
        String sql = "SELECT c.comment_id, c.post_id, c.author_id, c.parent_comment_id, " +
                "c.content, c.status, c.created_at, u.nickname " +
                "FROM comments c " +
                "JOIN users u ON c.author_id = u.user_id " +
                "WHERE c.comment_id = ? AND c.status = 'PUBLISHED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, commentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToComment(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 댓글 단건 조회 중 오류: " + e.getMessage());
        }
        return null;
    }

    /**
     * 4. 댓글 삭제 (작성자 본인 검증)
     */
    public boolean deleteComment(long commentId, long userId) {
        String sql = "UPDATE comments SET status = 'DELETED' WHERE comment_id = ? AND author_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, commentId);
            pstmt.setLong(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 댓글 삭제 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 5. [마이페이지 전용] 내가 작성한 댓글 목록 조회
     */
    public List<Comment> getCommentsByUserId(long userId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.comment_id, c.post_id, c.author_id, c.parent_comment_id, " +
                "c.content, c.status, c.created_at, u.nickname " +
                "FROM comments c " +
                "JOIN users u ON c.author_id = u.user_id " +
                "WHERE c.author_id = ? " +
                "ORDER BY c.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToComment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 내가 쓴 댓글 목록 조회 중 오류: " + e.getMessage());
        }
        return list;
    }

    private Comment mapResultSetToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setCommentId(rs.getLong("comment_id"));
        comment.setPostId(rs.getLong("post_id"));
        comment.setUserId(rs.getLong("author_id"));
        comment.setAuthorId(rs.getLong("author_id"));

        long parentId = rs.getLong("parent_comment_id");
        comment.setParentCommentId(rs.wasNull() ? null : parentId);

        comment.setContent(rs.getString("content"));
        comment.setStatus(rs.getString("status"));
        comment.setAuthorNickname(rs.getString("nickname"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            String dateStr = timestamp.toString();
            if (dateStr.contains(".")) {
                dateStr = dateStr.substring(0, dateStr.lastIndexOf('.'));
            }
            comment.setCreatedAt(dateStr);
        } else {
            comment.setCreatedAt("");
        }

        return comment;
    }
}
