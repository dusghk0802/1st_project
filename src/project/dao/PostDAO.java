package project.dao;

import project.common.DBConnection;
import project.dto.Post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시판(Post) 데이터베이스 접근 객체 (DAO)
 * DB 컬럼 기준: author_id 사용, movie_id 컬럼 미포함 구조
 */
public class PostDAO {

    /**
     * 1. 신규 게시글 등록
     */
    public boolean createPost(Post post) {
        String sql = "INSERT INTO posts (board_id, author_id, title, content, view_count, like_count, status) " +
                "VALUES (?, ?, ?, ?, 0, 0, 'PUBLISHED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, post.getBoardId() > 0 ? post.getBoardId() : 1L);
            pstmt.setLong(2, post.getUserId() > 0 ? post.getUserId() : post.getAuthorId());
            pstmt.setString(3, post.getTitle());
            pstmt.setString(4, post.getContent());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시글 등록 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 2. 전체 게시글 목록 조회
     */
    public List<Post> getAllPosts() {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT p.post_id, p.board_id, p.author_id, p.title, p.content, " +
                "p.view_count, p.like_count, p.status, p.created_at, u.nickname, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count " +
                "FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.status = 'PUBLISHED' " +
                "ORDER BY p.post_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ 게시글 목록 조회 중 SQL 오류: " + e.getMessage());
        }
        return list;
    }

    /** Selected board feed used by the category navigation. */
    public List<Post> getPostsByBoardId(long boardId) {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT p.post_id, p.board_id, p.author_id, p.title, p.content, " +
                "p.view_count, p.like_count, p.status, p.created_at, u.nickname, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count " +
                "FROM posts p JOIN users u ON p.author_id=u.user_id " +
                "WHERE p.status='PUBLISHED' AND p.board_id=? ORDER BY p.post_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, boardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) {
            System.err.println("Board post lookup failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * 3. 게시글 상세 보기 (조회수 1 증가 포함)
     */
    public Post getPostById(long postId, Long viewerId) {
        if (viewerId == null) {
            increaseViewCount(postId);
        } else {
            increaseViewCountOnce(postId, viewerId);
        }

        String sql = "SELECT p.post_id, p.board_id, p.author_id, p.title, p.content, " +
                "p.view_count, p.like_count, p.status, p.created_at, u.nickname, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count " +
                "FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.post_id = ? AND p.status = 'PUBLISHED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPost(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 게시글 상세 조회 중 SQL 오류: " + e.getMessage());
        }
        return null;
    }

    private void increaseViewCount(long postId) {
        String sql = "UPDATE posts SET view_count = view_count + 1 WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("조회수 증가 실패: " + e.getMessage());
        }
    }

    /**
     * 4. 게시글 수정 (작성자 본인 검증)
     */
    public boolean updatePost(long postId, long userId, String title, String content) {
        String sql = "UPDATE posts SET title = ?, content = ? WHERE post_id = ? AND author_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setLong(3, postId);
            pstmt.setLong(4, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시글 수정 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 5. 게시글 삭제 (작성자 본인 검증)
     */
    public boolean deletePost(long postId, long userId) {
        String sql = "DELETE FROM posts WHERE post_id = ? AND author_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);
            pstmt.setLong(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시글 삭제 중 SQL 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✨ 6. 게시글 검색 및 페이징
     */
    public List<Post> searchPosts(String keyword, int offset, int limit) {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT p.post_id, p.board_id, p.author_id, p.title, p.content, " +
                "p.view_count, p.like_count, p.status, p.created_at, u.nickname, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count " +
                "FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.status = 'PUBLISHED' AND (p.title LIKE ? OR p.content LIKE ?) " +
                "ORDER BY p.post_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setInt(3, offset);
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPost(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 게시글 검색 중 SQL 오류: " + e.getMessage());
        }
        return list;
    }

    /**
     * 7. 게시글 좋아요(추천) 토글 기능
     */
    public boolean toggleLike(long postId, long userId) {
        String checkSql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND user_id = ?";
        String insertSql = "INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)";
        String deleteSql = "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
        String updatePostLikeCountSql = "UPDATE posts SET like_count = " +
                "(SELECT COUNT(*) FROM post_likes WHERE post_id = ?) WHERE post_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            boolean isLiked = false;

            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setLong(1, postId);
                pstmt.setLong(2, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        isLiked = true;
                    }
                }
            }

            if (isLiked) {
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setLong(1, postId);
                    pstmt.setLong(2, userId);
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setLong(1, postId);
                    pstmt.setLong(2, userId);
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updatePostLikeCountSql)) {
                pstmt.setLong(1, postId);
                pstmt.setLong(2, postId);
                pstmt.executeUpdate();
            }

            return !isLiked;

        } catch (SQLException e) {
            System.err.println("❌ 좋아요 토글 처리 중 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean isLiked(long postId, long userId) {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /** Counts one view per logged-in user for each post. */
    private void increaseViewCountOnce(long postId, long userId) {
        String insert = "INSERT INTO post_views (post_id, user_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insert)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, userId);
            if (pstmt.executeUpdate() == 1) {
                increaseViewCount(postId);
            }
        } catch (SQLException ignored) {
            // The primary-key duplicate means this member already opened this post.
        }
    }

    public int getLikeCount(long postId) {
        String sql = "SELECT like_count FROM posts WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * 8. [마이페이지 전용] 내가 작성한 게시글 목록 조회
     */
    public List<Post> getPostsByUserId(long userId) {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT p.post_id, p.board_id, p.author_id, p.title, p.content, " +
                "p.view_count, p.like_count, p.status, p.created_at, u.nickname, " +
                "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count " +
                "FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.author_id = ? " +
                "ORDER BY p.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPost(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 내가 쓴 글 목록 조회 중 오류: " + e.getMessage());
        }
        return list;
    }

    /**
     * ResultSet ➔ Post DTO 매핑 헬퍼 메서드
     */
    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getLong("post_id"));
        post.setBoardId(rs.getLong("board_id"));
        post.setUserId(rs.getLong("author_id"));
        post.setAuthorId(rs.getLong("author_id"));
        post.setTitle(rs.getString("title"));
        post.setContent(rs.getString("content"));
        post.setViewCount(rs.getInt("view_count"));
        post.setLikeCount(rs.getInt("like_count"));
        post.setStatus(rs.getString("status"));
        post.setAuthorNickname(rs.getString("nickname"));

        try {
            post.setCommentCount(rs.getInt("comment_count"));
        } catch (SQLException ignored) {}

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            String dateStr = timestamp.toString();
            if (dateStr.contains(".")) {
                dateStr = dateStr.substring(0, dateStr.lastIndexOf('.'));
            }
            post.setCreatedAt(dateStr);
        } else {
            post.setCreatedAt("");
        }

        return post;
    }
}
