package project.dao;

import project.common.DBConnection;
import project.dto.Board;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시판(Board) 데이터베이스 접근 객체 (DAO)
 * DB 컬럼 기준: board_id, name, description, sort_order
 */
public class BoardDAO {

    /**
     * 1. 전체 게시판 목록 조회 (정렬 순서 기준)
     */
    public List<Board> getAllBoards() {
        List<Board> list = new ArrayList<>();
        String sql = "SELECT board_id, name, description, sort_order FROM boards ORDER BY sort_order ASC, board_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Board(
                        rs.getLong("board_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("sort_order")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ 게시판 목록 조회 오류: " + e.getMessage());
        }
        return list;
    }

    /**
     * 2. 신규 게시판 추가
     */
    public boolean createBoard(Board board) {
        String sql = "INSERT INTO boards (name, description, sort_order) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, board.getBoardName());
            pstmt.setString(2, board.getBoardDescription());
            pstmt.setInt(3, board.getSortOrder());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시판 추가 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 3. 게시판 정보 수정
     */
    public boolean updateBoard(Board board) {
        String sql = "UPDATE boards SET name = ?, description = ?, sort_order = ? WHERE board_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, board.getBoardName());
            pstmt.setString(2, board.getBoardDescription());
            pstmt.setInt(3, board.getSortOrder());
            pstmt.setLong(4, board.getBoardId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시판 수정 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 4. 게시판 삭제
     */
    public boolean deleteBoard(long boardId) {
        String sql = "DELETE FROM boards WHERE board_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, boardId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 게시판 삭제 오류: " + e.getMessage());
            return false;
        }
    }
}