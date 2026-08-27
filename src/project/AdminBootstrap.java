package project;
import project.common.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Lists existing admins and creates one only when ADMIN_LOGIN/ADMIN_PASSWORD are supplied. */
public final class AdminBootstrap {
    public static void main(String[] args) throws Exception {
        String login=System.getenv("ADMIN_LOGIN"), password=System.getenv("ADMIN_PASSWORD");
        try(Connection c=DBConnection.getConnection()){
            if(login!=null&&!login.isBlank()&&password!=null&&!password.isBlank()){
                try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM users WHERE login_id=?")){q.setString(1,login);try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)==0){try(PreparedStatement s=c.prepareStatement("INSERT INTO users(login_id,password_hash,nickname,email,role,status) VALUES(?,?,?,?, 'ADMIN','ACTIVE')")){s.setString(1,login);s.setString(2,password);s.setString(3,"관리자");s.setString(4,login+"@local.admin");s.executeUpdate();}}}}
                try(PreparedStatement s=c.prepareStatement("UPDATE users SET role='ADMIN',status='ACTIVE' WHERE login_id=?")){s.setString(1,login);s.executeUpdate();}
            }
            try(PreparedStatement s=c.prepareStatement("SELECT login_id,nickname FROM users WHERE role='ADMIN' ORDER BY user_id");ResultSet r=s.executeQuery()){while(r.next())System.out.println("admin="+r.getString(1)+" ("+r.getString(2)+")");}
        }
    }
}
