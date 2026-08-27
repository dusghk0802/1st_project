package project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import project.common.DBConnection;

/** Adds idempotent, movie-focused sample posts to notice and movie-recommendation boards. */
public final class BoardDemoSeeder {
    private static final String[] NOTICE_TITLES = {
        "[공지] CINEHUB 영화 추천 게시판 이용 안내", "[공지] 스포일러 표기 기준 안내", "[공지] 영화 리뷰 작성 시 유의사항",
        "[공지] 주간 인기 영화 집계 기준", "[공지] 신고 및 게시글 숨김 처리 안내", "[공지] 좋은 영화 대화 문화를 함께 만들어요",
        "[공지] 영화 포스터와 외부 링크 이용 안내", "[공지] 추천 글 제목 작성 방법", "[공지] 커뮤니티 운영 정책 업데이트", "[공지] 신규 회원 환영 인사"
    };
    private static final String[] MOVIE_TITLES = {
        "비 오는 날 보기 좋은 감성 영화 추천", "주말에 몰아보기 좋은 SF 영화", "엔딩이 오래 기억나는 영화", "혼자 조용히 보기 좋은 영화",
        "가족과 함께 보기 좋은 작품", "영화 입문자에게 추천하는 명작", "우주를 배경으로 한 영화 추천", "OST가 특히 좋은 영화",
        "긴 러닝타임이 아깝지 않은 영화", "다시 보고 싶은 한국 영화"
    };

    public static void main(String[] args) throws Exception {
        try (Connection connection = DBConnection.getConnection(); Statement statement = connection.createStatement()) {
            long authorId;
            try (ResultSet result = statement.executeQuery("SELECT MIN(user_id) FROM users WHERE status='ACTIVE'")) {
                result.next();
                authorId = result.getLong(1);
            }
            if (authorId == 0) throw new IllegalStateException("활성 사용자가 없습니다.");
            insertPosts(connection, 1, authorId, NOTICE_TITLES, "CINEHUB 운영 안내입니다. 영화를 좋아하는 모든 이용자가 편안하게 대화할 수 있도록 내용을 확인해 주세요.");
            insertPosts(connection, 3, authorId, MOVIE_TITLES, "이 영화는 분위기와 장면, 음악이 특히 인상적이었습니다. 비슷한 작품이나 여러분의 추천도 댓글로 알려 주세요.");
        }
    }

    private static void insertPosts(Connection connection, long boardId, long authorId, String[] titles, String content) throws Exception {
        try (PreparedStatement exists = connection.prepareStatement("SELECT COUNT(*) FROM posts WHERE board_id=? AND title=?");
             PreparedStatement insert = connection.prepareStatement("INSERT INTO posts(board_id,author_id,title,content,view_count,like_count,status) VALUES(?,?,?,?,0,0,'PUBLISHED')")) {
            for (String title : titles) {
                exists.setLong(1, boardId);
                exists.setString(2, title);
                try (ResultSet result = exists.executeQuery()) {
                    result.next();
                    if (result.getInt(1) > 0) continue;
                }
                insert.setLong(1, boardId);
                insert.setLong(2, authorId);
                insert.setString(3, title);
                insert.setString(4, content);
                insert.executeUpdate();
            }
        }
    }
}
