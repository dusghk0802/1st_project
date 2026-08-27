package project.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-SHA256 password storage for new Java accounts. Legacy Flask hashes remain verifiable. */
public final class PasswordHasher {
    private static final int ITERATIONS=310000, BYTES=32;
    private PasswordHasher() { }
    public static String hash(String password) {
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2-sha256$"+ITERATIONS+"$"+hex(salt)+"$"+hex(derive(password,salt,ITERATIONS));
    }
    public static boolean matches(String password,String saved) {
        try {
            if(saved.startsWith("pbkdf2-sha256$")){String[] p=saved.split("\\$");return MessageDigest.isEqual(derive(password,unhex(p[2]),Integer.parseInt(p[1])),unhex(p[3]));}
            if(saved.startsWith("pbkdf2:sha256:")){String[] p=saved.split("\\$",3);int rounds=Integer.parseInt(p[0].substring(14));return MessageDigest.isEqual(derive(password,p[1].getBytes(StandardCharsets.UTF_8),rounds),unhex(p[2]));}
            return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),saved.getBytes(StandardCharsets.UTF_8));
        } catch(Exception bad){return false;}
    }
    private static byte[] derive(String pass,byte[] salt,int rounds){try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(pass.toCharArray(),salt,rounds,BYTES*8)).getEncoded();}catch(Exception e){throw new IllegalStateException(e);}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}
    private static byte[] unhex(String s){byte[] out=new byte[s.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return out;}
}
