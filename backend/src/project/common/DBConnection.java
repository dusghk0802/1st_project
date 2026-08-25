package project.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";

    // 💡 아래의 USER와 PASSWORD를 본인의 오라클 계정 정보로 정확히 수정하세요!
    private static final String USER = "c##project"; // 예: c##scott 또는 본인 계정명
    private static final String PASSWORD = "1234"; // 본인 계정 비밀번호

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("Oracle JDBC 드라이버를 찾을 수 없습니다.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("데이터베이스 연결 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return conn;
    }
}