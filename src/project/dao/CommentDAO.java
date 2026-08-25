package project.dao;

import project.common.DBConnection;
import project.dto.Comment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    // 1. 댓글 작성 메서드 (루트 댓글 또는 대댓글 공용)
    public boolean createComment(Comment comment) {
        String sql = "INSERT INTO comments (post_id, author_id, parent_comment_id, content, status) " +
                "VALUES (?, ?, ?, ?, 'PUBLISHED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, comment.getPostId());
            pstmt.setLong(2, comment.getAuthorId());

            // parent_comment_id가 null일 경우 오라클에 맞게 처리
            if (comment.getParentCommentId() == null) {
                pstmt.setNull(3, Types.NULL);
            } else {
                pstmt.setLong(3, comment.getParentCommentId());
            }

            pstmt.setString(4, comment.getContent());

            int result = pstmt.executeUpdate();

            // 댓글 작성 성공 시 posts 테이블의 comment_count(댓글 수)도 1 증가시킵니다.
            if (result > 0) {
                increaseCommentCount(conn, comment.getPostId());
                return true;
            }

        } catch (SQLException e) {
            System.out.println("댓글 작성 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 보조 메서드: 게시글의 댓글 수 증가
    private void increaseCommentCount(Connection conn, long postId) {
        String sql = "UPDATE posts SET comment_count = comment_count + 1 WHERE post_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 2. 특정 게시글의 댓글 목록 조회 메서드
    public List<Comment> getCommentsByPostId(long postId) {
        List<Comment> commentList = new ArrayList<>();

        String sql = "SELECT c.*, u.nickname FROM comments c " +
                "JOIN users u ON c.author_id = u.user_id " +
                "WHERE c.post_id = ? AND c.status = 'PUBLISHED' " +
                "ORDER BY NVL(c.parent_comment_id, c.comment_id) ASC, c.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Comment comment = new Comment();
                    comment.setCommentId(rs.getLong("comment_id"));
                    comment.setPostId(rs.getLong("post_id"));
                    comment.setAuthorId(rs.getLong("author_id"));

                    long parentId = rs.getLong("parent_comment_id");
                    if (!rs.wasNull()) {
                        comment.setParentCommentId(parentId);
                    }

                    comment.setContent(rs.getString("content"));
                    comment.setStatus(rs.getString("status"));
                    comment.setCreatedAt(rs.getTimestamp("created_at"));
                    comment.setAuthorNickname(rs.getString("nickname"));

                    commentList.add(comment);
                }
            }
        } catch (SQLException e) {
            System.out.println("댓글 목록 조회 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return commentList;
    }
}