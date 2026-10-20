package project;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import project.common.DBConnection;
import project.security.PasswordHasher;

/**
 * GitHub Pages용 시연(읽기 전용) 사이트 생성기.
 * 서버를 같은 프로세스에서 띄운 뒤 화면 HTML과 API 응답(JSON)을 파일로 저장한다. 화면의 fetch는 저장된 JSON을 읽도록 바꾸고,
 * 로그인·글쓰기 같은 쓰기 기능은 "데모에서는 사용할 수 없음"으로 응답한다.
 * 사용: java project.StaticSiteExporter [출력폴더=docs] [기본경로=/1st_project/]
 */
public final class StaticSiteExporter {
    private static final int PORT = 18089;
    private static final String ORIGIN = "http://127.0.0.1:" + PORT;
    private static final String VIEWER_LOGIN = "demoviewer";
    /** 영화 추천 페이지에서 미리 저장해 둘 예시 검색어 */
    private static final String[] SEARCH_EXAMPLES = {"우주", "사랑", "좀비", "마블", "비 오는 날"};
    /** 출력 폴더 안에서 이 생성기가 만드는 항목. 다시 만들 때 이것만 지운다 (docs의 다른 문서는 보존). */
    private static final String[] GENERATED = {"index.html", "404.html", ".nojekyll", "about-us", "movies", "posts", "post", "category",
        "balance-game", "taste-quiz", "login", "register", "static", "api"};
    /** 따옴표로 시작하는 루트 경로. 앞에 +가 붙은 문자열('/api/posts/'+id+'/comments'의 뒷부분)은 경로 중간이라 제외한다. */
    private static final Pattern ROOT_PATH = Pattern.compile("(?<!\\+\\s?)(['\"])/(?=[A-Za-z_])");

    private final Path out;
    private final String base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient member = HttpClient.newBuilder().cookieHandler(cookies).build();
    private final HttpClient guest = HttpClient.newHttpClient();
    private final Set<Long> movieIds = new LinkedHashSet<>();
    private int pages, jsons;

    private StaticSiteExporter(Path out, String base) { this.out = out; this.base = base; }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs").toAbsolutePath().normalize();
        String base = args.length > 1 ? args[1] : "/1st_project/";
        if (!base.startsWith("/")) base = "/" + base;
        if (!base.endsWith("/")) base = base + "/";
        try {
            new StaticSiteExporter(out, base).run();
            System.exit(0);
        } catch (Exception error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws Exception {
        String password = prepareViewer();
        CinehubServer.main(new String[]{String.valueOf(PORT)});
        login(password);
        cleanOutput();

        String publicHeader = extractHeader(new String(get(guest, "/").body(), StandardCharsets.UTF_8));

        // ---- API 데이터 (JSON) ----
        saveJson("/api/boards");
        saveJson("/api/home/banners");
        collectIds(saveJson("/api/movies/popular"));
        collectIds(saveJson("/api/home/banners"));
        collectIds(saveJson("/api/movies/random"));
        for (String q : SEARCH_EXAMPLES) collectIds(saveJson("/api/movies/search?q=" + encodeURIComponent(q)));
        String posts = saveJson("/api/posts");
        for (Long board : idsOf(saveJson("/api/boards"), "id")) saveJson("/api/posts?boardId=" + board);
        List<Long> postIds = idsOf(posts, "id");
        for (Long id : postIds) { saveJson("/api/posts/" + id); saveJson("/api/posts/" + id + "/comments"); }
        String balance = saveJson("/api/balance-game/start");
        List<Long> balanceIds = idsOf(balance, "id");
        movieIds.addAll(balanceIds);
        for (Long id : balanceIds) saveJson("/api/balance-game/" + id + "/similar");
        String pairs = saveJson("/api/taste-quiz/pairs");
        saveTasteRecommendation(pairs);
        for (Long id : movieIds) {
            saveJson("/api/movies/" + id);
            saveJson("/api/movies/" + id + "/comments");
            saveJson("/api/movies/" + id + "/trailer");
            String comments = saveJson("/api/movies/" + id + "/comments?page=1");
            int total = intOf(comments, "totalPages");
            for (int p = 2; p <= total; p++) saveJson("/api/movies/" + id + "/comments?page=" + p);
        }

        // ---- 화면 (HTML) ----
        for (String page : new String[]{"/", "/about-us", "/movies", "/posts", "/balance-game", "/taste-quiz", "/login", "/register"}) savePage(page, publicHeader);
        for (Long board : idsOf(saveJson("/api/boards"), "id")) savePage("/category/" + board, publicHeader);
        for (Long id : postIds) savePage("/post/" + id, publicHeader);
        for (Long id : movieIds) savePage("/movies/" + id, publicHeader);

        copyStatic();
        write(out.resolve(".nojekyll"), "");
        write(out.resolve("404.html"), notFoundPage());
        System.out.println("데모 사이트 생성 완료: 화면 " + pages + "개, 데이터 " + jsons + "개 → " + out);
        System.out.println("GitHub Pages 주소(기본 경로 " + base + "): 저장소 Settings → Pages에서 main 브랜치 /docs 선택");
    }

    // ---------- 준비 ----------

    /** 화면을 저장할 때 쓸 데모 계정. 실행할 때마다 새 무작위 비밀번호로 바꿔서 비밀번호를 어디에도 저장하지 않는다. */
    private static String prepareViewer() throws Exception {
        String password = "Demo-" + UUID.randomUUID();
        String hash = PasswordHasher.hash(password);
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement update = c.prepareStatement("UPDATE users SET password_hash=?, status='ACTIVE' WHERE login_id=?")) {
                update.setString(1, hash);
                update.setString(2, VIEWER_LOGIN);
                if (update.executeUpdate() == 0) {
                    try (PreparedStatement insert = c.prepareStatement("INSERT INTO users(login_id,password_hash,nickname,email,role,status) VALUES(?,?,?,?,'USER','ACTIVE')")) {
                        insert.setString(1, VIEWER_LOGIN);
                        insert.setString(2, hash);
                        insert.setString(3, "데모방문자");
                        insert.setString(4, VIEWER_LOGIN + "@demo.cinehub.test");
                        insert.executeUpdate();
                    }
                }
            }
        }
        return password;
    }

    private void login(String password) throws Exception {
        String body = "loginId=" + encodeURIComponent(VIEWER_LOGIN) + "&password=" + encodeURIComponent(password);
        HttpResponse<byte[]> r = member.send(HttpRequest.newBuilder(URI.create(ORIGIN + "/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new IllegalStateException("데모 계정 로그인 실패: " + r.statusCode());
    }

    private void cleanOutput() throws IOException {
        Files.createDirectories(out);
        for (String name : GENERATED) {
            Path p = out.resolve(name);
            if (!Files.exists(p)) continue;
            try (Stream<Path> walk = Files.walk(p)) {
                for (Path x : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(x);
            }
        }
    }

    // ---------- JSON ----------

    private String saveJson(String apiPath) throws Exception {
        HttpResponse<byte[]> r = get(member, apiPath);
        Path file = out.resolve(jsonFile(apiPath));
        Files.createDirectories(file.getParent());
        Files.write(file, r.body());
        jsons++;
        return new String(r.body(), StandardCharsets.UTF_8);
    }

    private void saveTasteRecommendation(String pairs) throws Exception {
        // 장르 번호("id":878,"name")는 빼고 영화 번호("id":382,"title")만 고른다
        List<Long> ids = new ArrayList<>();
        Matcher movie = Pattern.compile("\"id\":(\\d+),\"title\"").matcher(pairs);
        while (movie.find()) ids.add(Long.parseLong(movie.group(1)));
        List<String> likes = new ArrayList<>(), dislikes = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) { if (i % 3 == 0) likes.add(String.valueOf(ids.get(i))); else if (i % 3 == 1) dislikes.add(String.valueOf(ids.get(i))); }
        String body = "likes=" + encodeURIComponent(String.join(",", likes)) + "&dislikes=" + encodeURIComponent(String.join(",", dislikes)) + "&unknowns=";
        String csrf = cookies.getCookieStore().getCookies().stream().filter(c -> "CINEHUB_CSRF".equals(c.getName())).map(HttpCookie::getValue).findFirst().orElse("");
        HttpResponse<byte[]> r = member.send(HttpRequest.newBuilder(URI.create(ORIGIN + "/api/taste-quiz/recommend"))
                .header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-Token", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
        Path file = out.resolve(jsonFile("/api/taste-quiz/recommend"));
        Files.createDirectories(file.getParent());
        Files.write(file, r.body());
        jsons++;
    }

    /** 브라우저 쪽 fetch 대체 코드(shim)와 똑같은 규칙으로 API 주소를 파일 경로로 바꾼다. */
    static String jsonFile(String apiPath) {
        int q = apiPath.indexOf('?');
        String path = q < 0 ? apiPath : apiPath.substring(0, q), query = q < 0 ? "" : apiPath.substring(q + 1);
        String key = path.substring("/api/".length()).replaceAll("/+$", "");
        if (!query.isEmpty()) key += "__" + query.replace('%', '_').replace('&', '-').replace('=', '.');
        return "api/" + key + ".json";
    }

    private void collectIds(String json) { movieIds.addAll(idsOf(json, "id")); }

    private static List<Long> idsOf(String json, String field) {
        List<Long> ids = new ArrayList<>();
        Matcher m = Pattern.compile("\"" + field + "\":(\\d+)").matcher(json);
        while (m.find()) { long id = Long.parseLong(m.group(1)); if (!ids.contains(id)) ids.add(id); }
        return ids;
    }

    private static int intOf(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    // ---------- HTML ----------

    private void savePage(String path, String publicHeader) throws Exception {
        HttpResponse<byte[]> r = get(member, path);
        if (r.statusCode() != 200) { System.out.println("건너뜀 " + path + " (" + r.statusCode() + ")"); return; }
        String html = new String(r.body(), StandardCharsets.UTF_8);
        html = html.replaceFirst("(?s)<header class='main-header.*?</header>", Matcher.quoteReplacement(publicHeader));
        html = html.replace("<body class='logged-in'>", "<body>");
        html = rewriteRootPaths(html);
        html = html.replaceFirst("<meta charset='utf-8'>", Matcher.quoteReplacement("<meta charset='utf-8'>" + shim()));
        Path file = path.equals("/") ? out.resolve("index.html") : out.resolve(path.substring(1)).resolve("index.html");
        write(file, html);
        pages++;
    }

    private static String extractHeader(String html) {
        Matcher m = Pattern.compile("(?s)<header class='main-header.*?</header>").matcher(html);
        if (!m.find()) throw new IllegalStateException("메뉴바를 찾지 못했습니다.");
        return m.group();
    }

    /** '/posts', "/static/..." 같은 사이트 루트 경로를 GitHub Pages 기본 경로(/저장소이름/)로 바꾼다. */
    private String rewriteRootPaths(String text) {
        text = text.replace("href='/'", "href='" + base + "'").replace("href=\"/\"", "href=\"" + base + "\"");
        return ROOT_PATH.matcher(text).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + base));
    }

    private void copyStatic() throws IOException {
        Path src = Path.of("webapp", "static").toAbsolutePath();
        try (Stream<Path> walk = Files.walk(src)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                Path target = out.resolve("static").resolve(src.relativize(p).toString());
                Files.createDirectories(target.getParent());
                String name = p.getFileName().toString();
                boolean text = name.endsWith(".css") || (name.endsWith(".js") && !name.endsWith(".min.js"));
                // site-header.js는 window.__CINEHUB_BASE로 직접 경로를 맞추므로 바꾸지 않는다
                if (text && !name.equals("site-header.js") && !p.toString().contains("vendor")) write(target, rewriteRootPaths(Files.readString(p, StandardCharsets.UTF_8)));
                else Files.copy(p, target);
            }
        }
    }

    /** 모든 화면 맨 앞에 넣는 코드: fetch를 저장된 JSON 읽기로 바꾸고, 쓰기 기능은 막고, 데모 안내를 띄운다. */
    private String shim() {
        String examples = String.join("|", SEARCH_EXAMPLES);
        return "<script>window.__CINEHUB_BASE='" + base + "';window.__CINEHUB_DEMO=true;(function(){"
            + "var B=window.__CINEHUB_BASE,of=window.fetch.bind(window),EX='" + examples + "'.split('|');"
            + "function json(o,s){return Promise.resolve(new Response(JSON.stringify(o),{status:s,headers:{'Content-Type':'application/json'}}));}"
            + "function toast(t){var d=document.createElement('div');d.textContent=t;d.style.cssText='position:fixed;left:50%;bottom:28px;transform:translateX(-50%);z-index:9999;background:rgba(23,24,27,.92);color:#fff;padding:12px 18px;border-radius:8px;font-size:14px;box-shadow:0 8px 24px rgba(0,0,0,.25)';document.body.appendChild(d);setTimeout(function(){d.remove();},2600);}"
            + "function file(u){var a=new URL(u,location.href);if(a.origin!==location.origin)return null;var p=a.pathname;if(p.indexOf(B)===0)p='/'+p.slice(B.length);if(p.indexOf('/api/')!==0)return null;"
            + "var k=p.slice(5).replace(/\\/+$/,'');if(a.search.length>1)k+='__'+a.search.slice(1).replace(/%/g,'_').replace(/&/g,'-').replace(/=/g,'.');return {path:p,url:B+'api/'+k+'.json'};}"
            + "window.fetch=function(u,o){var f=file(typeof u==='string'?u:u.url);if(!f)return of(u,o);var m=((o&&o.method)||'GET').toUpperCase();"
            + "if(f.path==='/api/me')return json({error:'login required'},401);"
            + "if(m!=='GET'){if(f.path==='/api/taste-quiz/recommend')return of(f.url);toast('시연용 페이지라 로그인·글쓰기 같은 기능은 사용할 수 없어요.');return json({error:'시연용 페이지에서는 사용할 수 없는 기능이에요.'},403);}"
            + "return of(f.url).then(function(r){if(r.ok)return r;if(f.path==='/api/movies/search')return json({results:[],message:'시연용 페이지에서는 예시 검색어만 검색할 수 있어요: '+EX.join(', ')},200);return json({error:'시연용 데이터가 없어요.'},404);});};"
            + "document.addEventListener('DOMContentLoaded',function(){var main=document.querySelector('main');if(main){var n=document.createElement('div');"
            + "n.style.cssText='width:min(1100px,calc(100% - 22px));margin:-12px auto 22px;padding:10px 16px;border-radius:8px;background:#fff5f6;border:1px solid #f3c7cc;color:#7a1f28;font-size:14px;box-sizing:border-box';"
            + "n.textContent='CINEHUB 시연용 페이지입니다. 화면은 실제 데이터로 둘러볼 수 있지만 로그인·글쓰기·좋아요·AI 자유 검색은 동작하지 않아요.';"
            + "var hero=document.getElementById('heroCarousel');if(hero)hero.insertAdjacentElement('afterend',n);else main.insertBefore(n,main.firstChild);}"
            + "var q=document.getElementById('movieQuery');if(q){var box=document.createElement('div');box.style.cssText='margin-top:12px;display:flex;flex-wrap:wrap;gap:6px;align-items:center;font-size:14px';"
            + "box.innerHTML='<span class=\"muted\">예시 검색어</span>';EX.forEach(function(w){var b=document.createElement('button');b.type='button';b.className='btn-main';b.style.cssText='padding:4px 12px;margin:0';b.textContent=w;"
            + "b.onclick=function(){q.value=w;var s=document.getElementById('movieSearchButton');if(s)s.click();};box.appendChild(b);});(q.closest('.movie-search-bar')||q).insertAdjacentElement('afterend',box);}"
            + "document.querySelectorAll('form').forEach(function(f){f.addEventListener('submit',function(){toast('시연용 페이지라 로그인·회원가입·글쓰기는 사용할 수 없어요.');},true);});});"
            + "})();</script>";
    }

    private String notFoundPage() {
        return "<!doctype html><html lang='ko'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>CINEHUB 시연용 페이지</title>"
            + "<link rel='stylesheet' href='" + base + "static/assets/css/style.css'></head><body><main><div class='container'><section class='hero'>"
            + "<span class='muted'>CINEHUB DEMO</span><h1>시연용 페이지에 없는 화면이에요</h1><p class='muted'>로그인이 필요한 화면이나 글쓰기·마이페이지는 시연용 페이지에 포함되지 않았어요.</p>"
            + "<a class='btn-main' href='" + base + "'>홈으로 가기</a></section></div></main></body></html>";
    }

    // ---------- 공통 ----------

    private static HttpResponse<byte[]> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(ORIGIN + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String encodeURIComponent(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
