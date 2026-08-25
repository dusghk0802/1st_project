package project.dao;

import project.common.DBConnection;
import project.dto.Post;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    // 1. 게시글 작성 메서드 (Create)
    public boolean createPost(Post post) {
        String sql = "INSERT INTO posts (board_id, author_id, title, content, status) " +
                "VALUES (?, ?, ?, ?, 'PUBLISHED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, post.getBoardId());
            pstmt.setLong(2, post.getAuthorId());
            pstmt.setString(3, post.getTitle());
            pstmt.setString(4, post.getContent());

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("게시글 작성 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 2. 전체 게시글 목록 조회 메서드 (Read)
    public List<Post> getAllPosts() {
        List<Post> postList = new ArrayList<>();

        String sql = "SELECT p.*, u.nickname FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.status = 'PUBLISHED' " +
                "ORDER BY p.post_id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Post post = new Post();
                post.setPostId(rs.getLong("post_id"));
                post.setBoardId(rs.getLong("board_id"));
                post.setAuthorId(rs.getLong("author_id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setViewCount(rs.getInt("view_count"));
                post.setLikeCount(rs.getInt("like_count"));
                post.setCommentCount(rs.getInt("comment_count"));
                post.setStatus(rs.getString("status"));
                post.setCreatedAt(rs.getTimestamp("created_at"));
                post.setAuthorNickname(rs.getString("nickname")); // 조인된 닉네임 세팅

                postList.add(post);
            }
        } catch (SQLException e) {
            System.out.println("게시글 목록 조회 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return postList;
    }
    // 3. 게시글 조회수 증가 메서드 (Update)
    public void increaseViewCount(long postId) {
        String sql = "UPDATE posts SET view_count = view_count + 1 WHERE post_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("조회수 증가 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 4. 게시글 상세 조회 메서드 (Read - 단건 조회)
    public Post getPostById(long postId) {
        // 상세 페이지 진입 시 조회수를 먼저 1 증가시킨 후 데이터를 가져옵니다.
        increaseViewCount(postId);

        String sql = "SELECT p.*, u.nickname FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.post_id = ? AND p.status = 'PUBLISHED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Post post = new Post();
                    post.setPostId(rs.getLong("post_id"));
                    post.setBoardId(rs.getLong("board_id"));
                    post.setAuthorId(rs.getLong("author_id"));
                    post.setTitle(rs.getString("title"));
                    post.setContent(rs.getString("content"));
                    post.setViewCount(rs.getInt("view_count"));
                    post.setLikeCount(rs.getInt("like_count"));
                    post.setCommentCount(rs.getInt("comment_count"));
                    post.setStatus(rs.getString("status"));
                    post.setCreatedAt(rs.getTimestamp("created_at"));
                    post.setUpdatedAt(rs.getTimestamp("updated_at"));
                    post.setAuthorNickname(rs.getString("nickname"));
                    return post;
                }
            }
        } catch (SQLException e) {
            System.out.println("게시글 상세 조회 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // 해당하는 글이 없거나 삭제된 경우
    }
    // 5. 게시글 수정 메서드 (Update)
    // 보안을 위해 글 번호(postId)와 작성자 번호(authorId)가 모두 일치할 때만 수정되도록 합니다.
    public boolean updatePost(long postId, long authorId, String newTitle, String newContent) {
        String sql = "UPDATE posts SET title = ?, content = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE post_id = ? AND author_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newTitle);
            pstmt.setString(2, newContent);
            pstmt.setLong(3, postId);
            pstmt.setLong(4, authorId);

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("게시글 수정 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 6. 게시글 삭제 메서드 (Delete 또는 상태 변경)
    // 여기서는 완전히 삭제하는 대신 status를 'DELETED'로 바꾸는 논리적 삭제(Soft Delete) 방식으로 구현합니다.
    public boolean deletePost(long postId, long authorId) {
        String sql = "UPDATE posts SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE post_id = ? AND author_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, postId);
            pstmt.setLong(2, authorId);

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("게시글 삭제 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // 7. 게시글 검색 및 페이징 조회 메서드 (Read)
    // keyword: 검색어, offset: 건너뛸 갯수(예: 0번부터 시작), limit: 한 페이지에 보여줄 갯수(예: 10개)
    public List<Post> searchPosts(String keyword, int offset, int limit) {
        List<Post> postList = new ArrayList<>();

        // 제목이나 내용에 키워드가 포함된 활성 상태의 게시글을 최신순으로 조회하면서 페이징 처리
        // LIKE 함수 사용하여 특정단어 검색 가능
        String sql = "SELECT p.*, u.nickname FROM posts p " +
                "JOIN users u ON p.author_id = u.user_id " +
                "WHERE p.status = 'PUBLISHED' AND (p.title LIKE ? OR p.content LIKE ?) " +
                "ORDER BY p.post_id DESC " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setInt(3, offset);
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Post post = new Post();
                    post.setPostId(rs.getLong("post_id"));
                    post.setBoardId(rs.getLong("board_id"));
                    post.setAuthorId(rs.getLong("author_id"));
                    post.setTitle(rs.getString("title"));
                    post.setContent(rs.getString("content"));
                    post.setViewCount(rs.getInt("view_count"));
                    post.setLikeCount(rs.getInt("like_count"));
                    post.setCommentCount(rs.getInt("comment_count"));
                    post.setStatus(rs.getString("status"));
                    post.setCreatedAt(rs.getTimestamp("created_at"));
                    post.setAuthorNickname(rs.getString("nickname"));

                    postList.add(post);
                }
            }
        } catch (SQLException e) {
            System.out.println("게시글 검색 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return postList;
    }
    // 8. 게시글 좋아요 여부 확인 메서드
    public boolean checkUserLiked(long postId, long userId) {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 9. 게시글 좋아요 토글 메서드 (누르지 않았다면 추가 후 like_count + 1, 이미 눌렀다면 삭제 후 like_count - 1)
    public boolean toggleLike(long postId, long userId) {
        boolean hasLiked = checkUserLiked(postId, userId);

        String sql = "";
        String updateCountSql = "";

        if (!hasLiked) {
            // 좋아요 누르지 않은 상태 -> 추가
            sql = "INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)";
            updateCountSql = "UPDATE posts SET like_count = like_count + 1 WHERE post_id = ?";
        } else {
            // 이미 누른 상태 -> 취소
            sql = "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
            updateCountSql = "UPDATE posts SET like_count = like_count - 1 WHERE post_id = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmtCount = conn.prepareStatement(updateCountSql)) {

            // 트랜잭션 관리를 위해 수동 커밋 모드로 설정할 수도 있으나 간단하게 순차 실행합니다.
            pstmt.setLong(1, postId);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                pstmtCount.setLong(1, postId);
                pstmtCount.executeUpdate();
                return !hasLiked; // true면 좋아요 추가됨, false면 좋아요 취소됨
            }

        } catch (SQLException e) {
            System.out.println("좋아요 처리 중 SQL 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}