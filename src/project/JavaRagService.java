package project;

import project.common.DBConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Java-only OpenAI embedding and Qdrant REST client. No Python vector service is used. */
public final class JavaRagService {
    private static final String COLLECTION="oracle_movie_semantic";
    private static final String QDRANT=env("QDRANT_URL","http://127.0.0.1:6333");
    private static final String KEY=env("OPENAI_API_KEY","");
    private static final HttpClient HTTP=HttpClient.newHttpClient();
    public boolean configured(){return !KEY.isBlank();}
    public List<Long> search(String text) throws Exception { if(!configured())return List.of(); return qdrantSearch(embed(List.of(text)).get(0)); }
    public int syncAll() throws Exception {
        if(!configured()) throw new IllegalStateException("OPENAI_API_KEY is required"); ensureCollection(); int total=0;
        try(Connection c=DBConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT movie_id,title,original_title,DBMS_LOB.SUBSTR(overview,8000,1) FROM movies ORDER BY movie_id"); ResultSet r=s.executeQuery()){
            List<Long> ids=new ArrayList<>(); List<String> texts=new ArrayList<>();
            while(r.next()){ids.add(r.getLong(1));texts.add(join(r.getString(2),r.getString(3),r.getString(4)));if(ids.size()==50){upsert(ids,embed(texts));total+=ids.size();ids.clear();texts.clear();}}
            if(!ids.isEmpty()){upsert(ids,embed(texts));total+=ids.size();}
        } return total;
    }
    private void ensureCollection() throws Exception { request("PUT",QDRANT+"/collections/"+COLLECTION,"{\"vectors\":{\"size\":1536,\"distance\":\"Cosine\"}}"); }
    private List<float[]> embed(List<String> inputs) throws Exception {
        StringBuilder body=new StringBuilder("{\"model\":\"text-embedding-3-small\",\"input\":[");for(int i=0;i<inputs.size();i++){if(i>0)body.append(',');body.append('"').append(json(inputs.get(i))).append('"');}body.append("]}");
        HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/embeddings")).header("Authorization","Bearer "+KEY).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        String result=HTTP.send(req,HttpResponse.BodyHandlers.ofString()).body(); List<float[]> vectors=new ArrayList<>();int at=0;while((at=result.indexOf("\"embedding\":[",at))>=0){int start=result.indexOf('[',at+12),depth=0,end=start;for(;end<result.length();end++){char ch=result.charAt(end);if(ch=='[')depth++;if(ch==']'&&--depth==0)break;}String[] values=result.substring(start+1,end).split(",");float[] vector=new float[values.length];for(int i=0;i<values.length;i++)vector[i]=Float.parseFloat(values[i]);vectors.add(vector);at=end+1;}if(vectors.size()!=inputs.size())throw new IllegalStateException("embedding response invalid");return vectors;
    }
    private void upsert(List<Long> ids,List<float[]> vectors)throws Exception{StringBuilder b=new StringBuilder("{\"points\":[");for(int i=0;i<ids.size();i++){if(i>0)b.append(',');b.append("{\"id\":").append(ids.get(i)).append(",\"vector\":[");for(int j=0;j<vectors.get(i).length;j++){if(j>0)b.append(',');b.append(vectors.get(i)[j]);}b.append("]}");}request("PUT",QDRANT+"/collections/"+COLLECTION+"/points?wait=true",b.append("]}").toString());}
    private List<Long> qdrantSearch(float[] vector)throws Exception{StringBuilder b=new StringBuilder("{\"query\":[");for(int i=0;i<vector.length;i++){if(i>0)b.append(',');b.append(vector[i]);}String result=request("POST",QDRANT+"/collections/"+COLLECTION+"/points/query",b.append("],\"limit\":8}").toString());List<Long> ids=new ArrayList<>();Matcher m=Pattern.compile("\\\"id\\\":(\\d+)").matcher(result);while(m.find())ids.add(Long.parseLong(m.group(1)));return ids;}
    private static String request(String method,String url,String body)throws Exception{HttpRequest req=HttpRequest.newBuilder(URI.create(url)).header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body)).build();HttpResponse<String> response=HTTP.send(req,HttpResponse.BodyHandlers.ofString());if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Qdrant error "+response.statusCode()+": "+response.body());return response.body();}
    private static String join(String...parts){StringBuilder b=new StringBuilder();for(String p:parts)if(p!=null&&!p.isBlank())b.append(p).append('\n');return b.toString();}
    private static String json(String text){return text.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
    private static String env(String key,String fallback){String value=System.getenv(key);return value==null||value.isBlank()?fallback:value;}
}
