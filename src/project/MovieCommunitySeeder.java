package project;

import java.sql.*;
import project.common.DBConnection;

/** Seeds the Oracle community with movie-focused sample posts for development/demo use. */
public final class MovieCommunitySeeder {
  public static void main(String[] args) throws Exception {
    String[] titles={"인터스텔라를 다시 본 이유","주말 SF 영화 추천 부탁드려요","오펜하이머 해석과 감상","비 오는 날 보기 좋은 영화","그래비티의 우주 연출","영화 OST 추천","엔딩이 기억나는 영화","넷플릭스 주말 추천","인생 영화 한 편만 고른다면","영화관에서 보면 좋은 작품"};
    try(Connection c=DBConnection.getConnection(); Statement s=c.createStatement()){
      long author; try(ResultSet r=s.executeQuery("SELECT MIN(user_id) FROM users WHERE status='ACTIVE'")){r.next();author=r.getLong(1);} if(author==0) throw new IllegalStateException("active user required");
      s.executeUpdate("UPDATE posts SET title='영화 이야기 #' || post_id, content='좋아하는 영화의 장면과 감상, 추천을 자유롭게 나누어 주세요. 어떤 장면이 가장 기억에 남았는지도 함께 이야기해 봅시다.', board_id=2 WHERE status='PUBLISHED'");
      try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM posts WHERE title=?"); PreparedStatement ins=c.prepareStatement("INSERT INTO posts(board_id,author_id,title,content,view_count,like_count,status) VALUES(?,?,?, ?,0,0,'PUBLISHED')")){
        for(int i=1;i<=100;i++){String title=titles[(i-1)%titles.length]+" #"+i;q.setString(1,title);try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)>0)continue;}ins.setLong(1,2);ins.setLong(2,author);ins.setString(3,title);ins.setString(4,"영화 #"+i+"에 대한 감상입니다. 인상 깊었던 장면과 추천 이유를 함께 나눠 주세요. 다른 관점의 해석도 환영합니다.");ins.executeUpdate();}
      }
    }
  }
}
