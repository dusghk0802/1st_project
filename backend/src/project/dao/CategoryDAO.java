package project.dao;

import project.common.DBConnection;
import project.dto.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> getCategoriesByBoardId(long boardId) {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, board_id, category_name, sort_order FROM categories WHERE board_id = ? ORDER BY sort_order ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, boardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(
                            rs.getLong("category_id"),
                            rs.getLong("board_id"),
                            rs.getString("category_name"),
                            rs.getInt("sort_order")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 카테고리 목록 조회 오류: " + e.getMessage());
        }
        return list;
    }

    public boolean createCategory(Category category) {
        String sql = "INSERT INTO categories (board_id, category_name, sort_order) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, category.getBoardId());
            pstmt.setString(2, category.getCategoryName());
            pstmt.setInt(3, category.getSortOrder());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 카테고리 생성 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET board_id = ?, category_name = ?, sort_order = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, category.getBoardId());
            pstmt.setString(2, category.getCategoryName());
            pstmt.setInt(3, category.getSortOrder());
            pstmt.setLong(4, category.getCategoryId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 카테고리 수정 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCategory(long categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, categoryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 카테고리 삭제 오류: " + e.getMessage());
            return false;
        }
    }
}