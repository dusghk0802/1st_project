package community;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Simple browser server for the movie records currently stored in MySQL. */
public final class MovieSearchServer {
    public static void main(String[] args) throws IOException {
        MovieSearchServer app = new MovieSearchServer();
        int port = args.length == 0 ? 8082 : Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", app::route);
        server.start();
        System.out.println("http://127.0.0.1:" + port);
    }
    private void route(HttpExchange e) throws IOException {
        if ("/api/search".equals(e.getRequestURI().getPath())) { search(e); return; }
        if ("/api/random".equals(e.getRequestURI().getPath())) { random(e); return; }
        if ("/api/movie".equals(e.getRequestURI().getPath())) { detail(e); return; }
        if ("/poster".equals(e.getRequestURI().getPath())) { poster(e); return; }
        send(e, 200, "text/html", pageWithSimilarMovies());
    }
    private void random(HttpExchange e) throws IOException {
        try (Connection c=Database.connect(); PreparedStatement s=c.prepareStatement("SELECT movie_id,title,COALESCE(title_en,''),COALESCE(overview,'') FROM movies ORDER BY RAND() LIMIT 1"); ResultSet r=s.executeQuery()) {
            if (!r.next()) { send(e,404,"application/json","{\"error\":\"no movies\"}"); return; }
            Movie m=new Movie(r.getLong(1),r.getString(2),r.getString(3),r.getString(4));
            send(e,200,"application/json",movieJson(m));
        } catch(Exception x) { send(e,500,"application/json","{\"error\":\"random recommendation failed\"}"); }
    }
    private void detail(HttpExchange e) throws IOException {
        try {
            long id=Long.parseLong(param(e,"id")); MovieDetail movie;
            String sql="SELECT movie_id,title,COALESCE(title_en,''),COALESCE(overview,''),COALESCE(overview_en,''),COALESCE(release_date,''),vote_average,vote_count FROM movies WHERE movie_id=?";
            try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next()){send(e,404,"application/json","{\"error\":\"movie not found\"}");return;}movie=new MovieDetail(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getDouble(7),r.getInt(8));}}
            StringBuilder json=new StringBuilder("{\"id\":").append(movie.id).append(",\"title\":\"").append(esc(movie.title)).append("\",\"en\":\"").append(esc(movie.en)).append("\",\"overview\":\"").append(esc(movie.overview)).append("\",\"overviewEn\":\"").append(esc(movie.overviewEn)).append("\",\"release\":\"").append(esc(movie.release)).append("\",\"voteAverage\":").append(movie.voteAverage).append(",\"voteCount\":").append(movie.voteCount).append(",\"reviews\":[");
            try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement("SELECT COALESCE(author,''),content,COALESCE(content_en,'') FROM movie_reviews WHERE movie_id=? ORDER BY updated_at DESC LIMIT 10")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){int n=0;while(r.next()){if(n++>0)json.append(',');json.append("{\"author\":\"").append(esc(r.getString(1))).append("\",\"content\":\"").append(esc(r.getString(2))).append("\",\"contentEn\":\"").append(esc(r.getString(3))).append("\"}");}}}
            List<Long> similarIds=semanticIds(movie.overview+" "+movie.overviewEn); similarIds.removeIf(value -> value == movie.id);
            List<Movie> similar=findByIds(similarIds.size()>5?similarIds.subList(0,5):similarIds);
            json.append("],\"similar\":[");
            for(int i=0;i<similar.size();i++){if(i>0)json.append(',');json.append(movieJson(similar.get(i)));}
            send(e,200,"application/json",json.append("]}").toString());
        } catch(Exception x) { send(e,500,"application/json","{\"error\":\"movie detail failed\"}"); }
    }
    private void search(HttpExchange e) throws IOException {
        String q = param(e, "q").trim(); if (q.isEmpty()) { send(e, 400, "application/json", "{\"error\":\"query required\"}"); return; }
        try {
            List<Movie> found = findByKeyword(q);
            if (found.isEmpty()) {
                try { long imported = new TmdbOnDemandImporter().importFirst(q); if (imported > 0) { indexVector(imported); found = findByIds(java.util.List.of(imported)); } }
                catch (Exception ignored) { /* TMDB may not have a title match for a plot-only sentence. */ }
            }
            if (found.isEmpty()) found = findByIds(semanticIds(q));
            StringBuilder json=new StringBuilder("{\"results\":[");
            for(int i=0;i<found.size();i++){ Movie m=found.get(i); if(i>0)json.append(','); json.append("{\"id\":").append(m.id).append(",\"title\":\"").append(esc(m.title)).append("\",\"en\":\"").append(esc(m.en)).append("\",\"overview\":\"").append(esc(m.overview)).append("\"}"); }
            send(e,200,"application/json",json.append("]}").toString());
        } catch(Exception x) { send(e,500,"application/json","{\"error\":\"database search failed\"}"); }
    }
    private List<Movie> findByKeyword(String query) throws Exception {
        String like = "%" + query + "%"; List<Movie> found = new ArrayList<>();
        String sql = "SELECT movie_id,title,COALESCE(title_en,''),COALESCE(overview,'') FROM movies WHERE title LIKE ? OR title_en LIKE ? OR overview LIKE ? OR overview_en LIKE ? ORDER BY popularity DESC LIMIT 10";
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) { for (int i=1;i<=4;i++) s.setString(i, like); try (ResultSet r=s.executeQuery()) { while(r.next()) found.add(new Movie(r.getLong(1),r.getString(2),r.getString(3),r.getString(4))); } }
        return found;
    }
    private List<Long> semanticIds(String query) {
        try { String encoded=URLEncoder.encode(query,StandardCharsets.UTF_8); HttpResponse<String> response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8090/search?q="+encoded)).GET().build(),HttpResponse.BodyHandlers.ofString()); java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\d+").matcher(response.body());List<Long>ids=new ArrayList<>();while(m.find())ids.add(Long.parseLong(m.group()));return ids; } catch(Exception ignored) { return java.util.Collections.emptyList(); }
    }
    private void indexVector(long id) { try { HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8090/index/movie")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"movie_id\":"+id+"}")).build(),HttpResponse.BodyHandlers.discarding()); } catch(Exception ignored) { } }
    private List<Movie> findByIds(List<Long> ids) throws Exception { if(ids.isEmpty())return java.util.Collections.emptyList();StringBuilder marks=new StringBuilder();for(int i=0;i<ids.size();i++){if(i>0)marks.append(',');marks.append('?');}java.util.Map<Long,Movie> map=new java.util.HashMap<>();try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement("SELECT movie_id,title,COALESCE(title_en,''),COALESCE(overview,'') FROM movies WHERE movie_id IN ("+marks+")")){for(int i=0;i<ids.size();i++)s.setLong(i+1,ids.get(i));try(ResultSet r=s.executeQuery()){while(r.next())map.put(r.getLong(1),new Movie(r.getLong(1),r.getString(2),r.getString(3),r.getString(4)));}}List<Movie>result=new ArrayList<>();for(Long id:ids)if(map.containsKey(id))result.add(map.get(id));return result; }
    private void poster(HttpExchange e) throws IOException {
        try {
            long id=Long.parseLong(param(e,"id")); String path="", posterPath="";
            try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement("SELECT COALESCE(poster_local_path,''),COALESCE(poster_path,'') FROM movies WHERE movie_id=?")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(r.next()){path=r.getString(1);posterPath=r.getString(2);}}}
            if(!path.isBlank() && Files.isRegularFile(Path.of(path))){byte[] b=Files.readAllBytes(Path.of(path));e.getResponseHeaders().set("Content-Type","image/jpeg");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();return;}
            String remote=TmdbClient.posterUrl(posterPath);if(remote!=null){e.getResponseHeaders().set("Location",remote);e.sendResponseHeaders(302,-1);e.close();return;}
            throw new IOException();
        } catch(Exception x) { send(e,404,"text/plain","poster not found"); }
    }
    private static String param(HttpExchange e,String key){String q=e.getRequestURI().getRawQuery();if(q==null)return "";for(String p:q.split("&")){int n=p.indexOf('=');if(n>0&&p.substring(0,n).equals(key))return URLDecoder.decode(p.substring(n+1),StandardCharsets.UTF_8);}return "";}
    private static String esc(String s){return (s==null?"":s).replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
    private static String movieJson(Movie m){return "{\"id\":"+m.id+",\"title\":\""+esc(m.title)+"\",\"en\":\""+esc(m.en)+"\",\"overview\":\""+esc(m.overview)+"\"}";}
    private static String pageWithSimilarMovies(){return PAGE3.replace("<h2>Reviews</h2>","<h2>비슷한 영화</h2>'+cards(m.similar||[])+'<h2>리뷰</h2>")
        .replace("CineMatch","영화추천").replace("Click a movie card to see its full details.","영화 카드를 누르면 상세 정보를 볼 수 있습니다.")
        .replace("e.g. action, love, space","예: 액션, 사랑, 우주").replace(">Search</button>",">검색</button>")
        .replace("Random movie","랜덤 영화").replace("Search for a movie or choose a random recommendation.","영화를 검색하거나 랜덤 영화를 추천받으세요.")
        .replace("Searching...","검색 중...").replace("No result found.","검색 결과가 없습니다.").replace("Choosing a movie...","랜덤 추천 중...")
        .replace("Loading details...","상세 정보를 불러오는 중...").replace(">Back</button>",">뒤로 가기</button>")
        .replace("Release:","개봉일:").replace("Rating:","평점:").replace("votes","명 참여")
        .replace(">Overview</h3>",">줄거리</h3>").replace(">English overview</h3>",">영문 줄거리</h3>")
        .replace("No saved reviews yet.","저장된 리뷰가 없습니다.").replace("Anonymous","익명");}
    private static void send(HttpExchange e,int status,String type,String body)throws IOException{byte[]b=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type",type+";charset=utf-8");e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();}
    private static final class Movie{final long id;final String title,en,overview;Movie(long id,String title,String en,String overview){this.id=id;this.title=title;this.en=en;this.overview=overview;}}
    private static final class MovieDetail{final long id;final String title,en,overview,overviewEn,release;final double voteAverage;final int voteCount;MovieDetail(long id,String title,String en,String overview,String overviewEn,String release,double voteAverage,int voteCount){this.id=id;this.title=title;this.en=en;this.overview=overview;this.overviewEn=overviewEn;this.release=release;this.voteAverage=voteAverage;this.voteCount=voteCount;}}
    private static final String PAGE3="<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>CineMatch</title>"
        +"<style>body{margin:0;background:#f5f7fc;color:#16213c;font:16px Arial}.hero{padding:55px 20px;text-align:center;color:#fff;background:linear-gradient(120deg,#3e4ec4,#9a63ee)}h1{font-size:42px;margin:0 0 10px}.search{margin:25px auto;max-width:720px;display:flex;gap:10px}input{flex:1;padding:16px;border:0;border-radius:12px;font-size:16px}button{border:0;border-radius:12px;background:#ff6b5f;color:#fff;padding:12px 22px;font-weight:bold;cursor:pointer}.random{background:#27366e}.wrap{max-width:1080px;margin:32px auto;padding:0 20px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:16px}.card{overflow:hidden;background:#fff;border-radius:14px;box-shadow:0 8px 25px #24345c22;cursor:pointer}.card img{width:100%;height:310px;object-fit:cover;background:#e5e9f4}.card div{padding:14px}.card h2{font-size:18px;margin:0}.card p{color:#69758f;font-size:14px;line-height:1.5}.empty{text-align:center;color:#69758f;padding:50px}.detail{background:#fff;border-radius:18px;padding:25px;box-shadow:0 8px 25px #24345c22}.detailHead{display:grid;grid-template-columns:240px 1fr;gap:26px}.detailHead img{width:240px;border-radius:12px}.meta{color:#56627d}.review{border-top:1px solid #e5e9f4;padding:14px 0;line-height:1.55}</style>"
        +"<section class=hero><h1>CineMatch</h1><p>Click a movie card to see its full details.</p><div class=search><input id=q placeholder='e.g. action, love, space'><button onclick=go()>Search</button><button class=random onclick=randomMovie()>Random movie</button></div></section><main class=wrap><div id=out class=empty>Search for a movie or choose a random recommendation.</div></main>"
        +"<script>function safe(s){return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}function cards(a){var x='';for(var i=0;i<a.length;i++){var m=a[i];x+='<article class=card onclick=detail('+m.id+')><img src=/poster?id='+m.id+'><div><h2>'+safe(m.title)+'</h2><small>'+safe(m.en)+'</small><p>'+safe(m.overview)+'</p></div></article>'}return x}async function go(){var q=document.querySelector('#q').value.trim(),o=document.querySelector('#out');if(!q)return;o.className='empty';o.textContent='Searching...';var d=await fetch('/api/search?q='+encodeURIComponent(q)).then(function(r){return r.json()});if(!d.results.length){o.textContent='No result found.';return}o.className='grid';o.innerHTML=cards(d.results)}async function randomMovie(){var o=document.querySelector('#out');o.className='empty';o.textContent='Choosing a movie...';var m=await fetch('/api/random').then(function(r){return r.json()});o.className='grid';o.innerHTML=cards([m])}async function detail(id){var o=document.querySelector('#out');o.className='empty';o.textContent='Loading details...';var m=await fetch('/api/movie?id='+id).then(function(r){return r.json()});var reviews='';if(m.reviews.length){for(var i=0;i<m.reviews.length;i++){var r=m.reviews[i];reviews+='<div class=review><b>'+safe(r.author||'Anonymous')+'</b><p>'+safe(r.content)+'</p><p class=meta>'+safe(r.contentEn)+'</p></div>'}}else{reviews='<p class=meta>No saved reviews yet.</p>'}o.className='detail';o.innerHTML='<button onclick=location.reload()>Back</button><div class=detailHead><img src=/poster?id='+m.id+'><div><h1>'+safe(m.title)+'</h1><h3>'+safe(m.en)+'</h3><p class=meta>Release: '+safe(m.release)+' · Rating: '+m.voteAverage+' ('+m.voteCount+' votes)</p><h3>Overview</h3><p>'+safe(m.overview)+'</p><h3>English overview</h3><p class=meta>'+safe(m.overviewEn)+'</p></div></div><h2>Reviews</h2>'+reviews}document.querySelector('#q').addEventListener('keydown',function(e){if(e.key==='Enter')go()})</script>";
    private static final String PAGE2="<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>CineMatch</title>"
        +"<style>body{margin:0;background:#f5f7fc;color:#16213c;font:16px Arial}.hero{padding:55px 20px;text-align:center;color:#fff;background:linear-gradient(120deg,#3e4ec4,#9a63ee)}h1{font-size:42px;margin:0 0 10px}.search{margin:25px auto;max-width:720px;display:flex;gap:10px}input{flex:1;padding:16px;border:0;border-radius:12px;font-size:16px}button{border:0;border-radius:12px;background:#ff6b5f;color:#fff;padding:0 22px;font-weight:bold;cursor:pointer}.random{background:#27366e}.wrap{max-width:1080px;margin:32px auto;padding:0 20px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:16px}.card{overflow:hidden;background:#fff;border-radius:14px;box-shadow:0 8px 25px #24345c22;cursor:pointer}.card img{width:100%;height:310px;object-fit:cover;background:#e5e9f4}.card div{padding:14px}.card h2{font-size:18px;margin:0}.card p{color:#69758f;font-size:14px;line-height:1.5}.empty{text-align:center;color:#69758f;padding:50px}.detail{background:#fff;border-radius:18px;padding:25px;box-shadow:0 8px 25px #24345c22}.detailHead{display:grid;grid-template-columns:240px 1fr;gap:26px}.detailHead img{width:240px;border-radius:12px}.meta{color:#56627d}.review{border-top:1px solid #e5e9f4;padding:14px 0;line-height:1.55}@media(max-width:650px){.detailHead{grid-template-columns:1fr}.detailHead img{width:180px}}</style>"
        +"<section class=hero><h1>CineMatch</h1><p>Click a movie card to see its full details.</p><div class=search><input id=q placeholder='e.g. action, love, space'><button onclick=go()>Search</button><button class=random onclick=randomMovie()>Random movie</button></div></section><main class=wrap><div id=out class=empty>Search for a movie or choose a random recommendation.</div></main>"
        +"<script>function safe(s){return String(s||'').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')}function cards(a){return a.map(m=>`<article class=card onclick='detail(${m.id})'><img src='/poster?id=${m.id}'><div><h2>${safe(m.title)}</h2><small>${safe(m.en)}</small><p>${safe(m.overview)}</p></div></article>`).join('')}async function go(){let q=document.querySelector('#q').value.trim(),o=document.querySelector('#out');if(!q)return;o.className='empty';o.textContent='Searching...';let d=await fetch('/api/search?q='+encodeURIComponent(q)).then(r=>r.json());if(!d.results.length){o.textContent='No result found.';return}o.className='grid';o.innerHTML=cards(d.results)}async function randomMovie(){let o=document.querySelector('#out');o.className='empty';o.textContent='Choosing a movie...';let m=await fetch('/api/random').then(r=>r.json());o.className='grid';o.innerHTML=cards([m])}async function detail(id){let o=document.querySelector('#out');o.className='empty';o.textContent='Loading details...';let m=await fetch('/api/movie?id='+id).then(r=>r.json());let reviews=m.reviews.length?m.reviews.map(r=>`<div class=review><b>${safe(r.author||'Anonymous')}</b><p>${safe(r.content)}</p>${r.contentEn?`<p class=meta>${safe(r.contentEn)}</p>`:''}</div>`).join(''):'<p class=meta>No saved reviews yet.</p>';o.className='detail';o.innerHTML=`<button onclick='location.reload()'>Back</button><div class=detailHead><img src='/poster?id=${m.id}'><div><h1>${safe(m.title)}</h1><h3>${safe(m.en)}</h3><p class=meta>Release: ${safe(m.release)} · Rating: ${m.voteAverage} (${m.voteCount} votes)</p><h3>Overview</h3><p>${safe(m.overview)||'No Korean overview.'}</p><h3>English overview</h3><p class=meta>${safe(m.overviewEn)||'No English overview.'}</p></div></div><h2>Reviews</h2>${reviews}`)}document.querySelector('#q').addEventListener('keydown',e=>{if(e.key==='Enter')go()})</script>";
    private static final String PAGE="<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>CineMatch</title><style>body{margin:0;background:#f5f7fc;color:#16213c;font:16px Arial}.hero{padding:65px 20px;text-align:center;color:white;background:linear-gradient(120deg,#3e4ec4,#9a63ee)}h1{font-size:44px;margin:0 0 12px}.search{margin:25px auto;max-width:680px;display:flex;gap:10px}input{flex:1;padding:16px;border:0;border-radius:12px;font-size:16px}button{border:0;border-radius:12px;background:#ff6b5f;color:white;padding:0 24px;font-weight:bold;cursor:pointer}.random{background:#27366e}.wrap{max-width:1050px;margin:34px auto;padding:0 20px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:16px}.card{overflow:hidden;background:white;border-radius:14px;box-shadow:0 8px 25px #24345c22}.card img{width:100%;height:310px;object-fit:cover;background:#e5e9f4}.card div{padding:14px}.card h2{font-size:18px;margin:0}.card p{color:#69758f;font-size:14px;line-height:1.5}.empty{text-align:center;color:#69758f;padding:50px}</style><section class=hero><h1>CineMatch</h1><p>MySQL에 저장된 영화 제목과 한글·영문 줄거리를 검색합니다.</p><div class=search><input id=q placeholder='예: action, love, space'><button onclick=go()>검색</button><button class=random onclick=randomMovie()>오늘의 랜덤 영화</button></div></section><main class=wrap><div id=out class=empty>검색어를 입력하거나 랜덤 영화를 추천받으세요.</div></main><script>function cards(a){return a.map(m=>'<article class=card><img src=/poster?id='+m.id+'><div><h2>'+m.title+'</h2><small>'+m.en+'</small><p>'+m.overview+'</p></div></article>').join('')}async function go(){let q=document.querySelector('#q').value.trim(),o=document.querySelector('#out');if(!q)return;o.className='empty';o.textContent='검색 중...';let d=await fetch('/api/search?q='+encodeURIComponent(q)).then(r=>r.json());if(!d.results.length){o.textContent='결과가 없습니다.';return}o.className='grid';o.innerHTML=cards(d.results)}async function randomMovie(){let o=document.querySelector('#out');o.className='empty';o.textContent='랜덤 추천 중...';let m=await fetch('/api/random').then(r=>r.json());o.className='grid';o.innerHTML=cards([m])}document.querySelector('#q').addEventListener('keydown',e=>{if(e.key==='Enter')go()})</script>";
}
