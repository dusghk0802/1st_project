package project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import project.common.DBConnection;
import project.security.PasswordHasher;

/**
 * 자유게시판 데모 글 생성기 (개발/시연용).
 * 실행 순서: BoardDemoSeeder 먼저 → 이 생성기. 템플릿 글 90개를 먼저 저장하고 예시 글 10개를 마지막에 저장해서
 * 예시 글이 목록 맨 위(post_id가 가장 큼)에 오게 한다. 실제 회원 글은 건드리지 않는다.
 * 인자 없이 실행: 생성 (이미 있으면 건너뜀) / --clean: demo_ 회원과 그 글·댓글·좋아요 삭제
 */
public final class MovieCommunitySeeder {
    private static final int TEMPLATE_POSTS = 90;
    private static final long SEED = 20261020L;

    /** 아이디, 닉네임, 즐겨 쓰는 장르 */
    private static final String[][] DEMO_USERS = {
        {"demo_cine01", "주말시네마", "드라마"},
        {"demo_cine02", "SF덕후", "SF"},
        {"demo_cine03", "공포는못참지", "공포"},
        {"demo_cine04", "영화관죽돌이", "액션"},
        {"demo_cine05", "평점도둑", "스릴러"},
        {"demo_cine06", "팝콘한통", "코미디"},
    };

    /** 목록 맨 위에 보일 예시 글. 이 배열 순서대로 위에서부터 보인다. {제목, 연결할 영화 제목(없으면 null), 내용} */
    private static final String[][] EXAMPLES = {
        {"인터스텔라를 다시 본 이유", "인터스텔라",
            "처음 봤을 때는 블랙홀 장면이랑 음악에 압도돼서 이야기를 반쯤 놓쳤던 것 같아요.\n\n이번에 다시 보니 쿠퍼와 머피의 관계가 훨씬 크게 다가오더라고요. 시간이 다르게 흐르는 행성에서 돌아왔을 때 쌓여 있던 영상 메시지 장면은 두 번째인데도 울컥했습니다.\n\n과학 설정을 다 이해하지 못해도 결국 가족 이야기라서 오래 남는 영화 같아요. 다시 보신 분들은 어떤 장면이 새롭게 보이셨나요?"},
        {"주말 SF 영화 추천 부탁드려요", null,
            "이번 주말에 SF 영화를 몰아서 보려고 하는데 뭘 볼지 고민이에요.\n\n우주 배경도 좋고, 미래 사회 이야기도 좋아요. 너무 어려운 설정보다는 몰입해서 볼 수 있는 작품이면 좋겠습니다.\n\n여러분이 재밌게 본 SF 영화 한 편씩 추천해 주세요!"},
        {"오펜하이머 해석과 감상", "오펜하이머",
            "세 시간이 길게 느껴지지 않을 정도로 몰입해서 봤어요.\n\n컬러와 흑백이 오가는 구성이 처음엔 헷갈렸는데, 오펜하이머 본인의 시점과 바깥에서 그를 바라보는 시점을 나눈 거라고 생각하니 정리가 되더라고요. 트리니티 실험 장면에서 소리가 사라지는 연출은 극장에서 보길 잘했다 싶었습니다.\n\n마지막 대화 장면을 어떻게 해석하셨는지 궁금해요."},
        {"비 오는 날 보기 좋은 영화", null,
            "요즘 비가 자주 와서 집에서 조용히 영화 보는 날이 많아졌어요.\n\n저는 이런 날엔 잔잔한 드라마나 로맨스를 찾게 되더라고요. 창밖 빗소리랑 같이 들으면 분위기가 두 배가 되는 것 같아요.\n\n비 오는 날 꺼내 보는 영화가 있으면 같이 공유해요."},
        {"그래비티의 우주 연출", null,
            "그래비티는 이야기보다 연출로 기억되는 영화 같아요.\n\n오프닝의 긴 롱테이크, 소리 없는 우주에서 들리는 숨소리, 돌아가는 시점 같은 장면들이 진짜 우주에 떠 있는 느낌을 줬습니다. 극장에서 3D로 봤을 때 멀미가 날 정도였어요.\n\n우주 연출이 인상적이었던 다른 영화도 추천해 주세요."},
        {"영화 OST 추천", null,
            "영화를 보고 나서 OST를 계속 찾아 듣게 되는 경우가 있잖아요.\n\n저는 장면이 바로 떠오르는 음악을 특히 좋아해요. 출퇴근길에 들으면 그 영화를 다시 보는 기분이 들더라고요.\n\n여러분이 플레이리스트에 넣어 둔 영화 음악은 뭔가요?"},
        {"엔딩이 기억나는 영화", null,
            "영화를 다 보고 나서 한참 동안 자리에서 못 일어났던 적 있으신가요?\n\n반전이 있는 엔딩도 좋고, 여운이 길게 남는 열린 결말도 좋아요. 엔딩 하나 때문에 영화 전체가 다르게 보이는 경우도 있고요.\n\n스포일러가 될 수 있으니 영화 제목만 적고, 이야기는 댓글에서 조심스럽게 나눠 봐요."},
        {"넷플릭스 주말 추천", null,
            "주말에 넷플릭스로 볼 영화를 고르다가 시간을 다 보내는 편이에요.\n\n가볍게 보기 좋은 코미디, 몰입감 있는 스릴러, 가족이랑 보기 좋은 애니메이션까지 장르 상관없이 추천받고 싶어요.\n\n요즘 보신 것 중에 괜찮았던 작품 알려 주세요."},
        {"인생 영화 한 편만 고른다면", null,
            "딱 한 편만 고르라고 하면 정말 어렵죠.\n\n저는 볼 때마다 다른 장면이 눈에 들어오는 영화를 인생 영화로 꼽게 되더라고요. 나이에 따라 다르게 보이는 영화도 있고요.\n\n여러분의 인생 영화와 그 이유를 한 줄로 남겨 주세요."},
        {"영화관에서 보면 좋은 작품", null,
            "어떤 영화는 집에서 보면 절반밖에 못 느끼는 것 같아요.\n\n큰 화면과 사운드가 필요한 영화, 다른 관객들과 같이 웃고 놀라야 재밌는 영화가 있잖아요. 저는 우주나 전쟁을 다룬 영화는 꼭 극장에서 보려고 해요.\n\n극장에서 봐서 다행이었던 영화가 있으면 추천해 주세요."},
    };

    private static final String[] OPENINGS = {
        "지난 주말에 드디어 봤어요.", "미루고 미루다가 이제야 봤네요.", "친구 추천으로 보게 됐어요.",
        "퇴근하고 조용히 혼자 봤습니다.", "극장에서 보고 왔어요.", "OTT에 올라왔길래 바로 틀었어요."
    };
    private static final String[] IMPRESSIONS = {
        "생각보다 훨씬 몰입해서 봤어요.", "중반부터 이야기가 확 살아나더라고요.", "배우들 연기가 특히 좋았습니다.",
        "음악이랑 화면이 정말 잘 어울렸어요.", "기대를 크게 안 했는데 의외로 재밌었어요.", "보고 나서 여운이 꽤 오래 남았어요.",
        "호불호는 갈릴 것 같은데 저는 좋았어요.", "러닝타임이 짧게 느껴질 정도였어요."
    };
    private static final String[] CLOSINGS = {
        "보신 분들은 어떠셨나요?", "비슷한 분위기의 영화 있으면 추천해 주세요.", "다른 분들 감상도 궁금해요.",
        "아직 안 보신 분들께 추천합니다.", "댓글로 같이 이야기 나눠요."
    };
    private static final String[] COMMENTS = {
        "저도 봤어요! 공감합니다.", "이 영화 아직 못 봤는데 이번 주말에 봐야겠네요.", "저는 결말이 조금 아쉬웠어요.",
        "글 보니까 다시 보고 싶어지네요.", "추천 감사합니다, 리스트에 추가했어요.", "그 장면 저도 기억나요!",
        "음악이 정말 좋았죠.", "저랑 생각이 똑같으시네요.", "저는 오히려 초반이 더 좋았어요.", "극장에서 봤으면 더 좋았을 것 같아요.",
        "평점보다 훨씬 재밌게 봤어요.", "비슷한 영화로 하나 더 추천드리면 좋겠네요.", "배우 연기 진짜 좋았어요.",
        "두 번 보니까 더 좋더라고요.", "저는 무서워서 반쯤 가리고 봤어요 ㅋㅋ", "좋은 글 감사합니다!", "이번 주에 꼭 볼게요.",
        "스포 없이 써 주셔서 감사해요."
    };

    private final Random random = new Random(SEED);
    private final Map<Long, Integer> movieUses = new HashMap<>();
    private final Map<String, Long> users = new LinkedHashMap<>();
    private final List<Movie> movies = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        boolean clean = args.length > 0 && "--clean".equals(args[0]);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (clean) new MovieCommunitySeeder().clean(c); else new MovieCommunitySeeder().seed(c);
                c.commit();
            } catch (Exception error) {
                c.rollback();
                throw error;
            }
        }
    }

    private void seed(Connection c) throws Exception {
        long freeBoard = boardId(c, "자유게시판");
        if (count(c, "SELECT COUNT(*) FROM posts WHERE board_id IN (SELECT board_id FROM boards WHERE name IN ('공지사항','영화 추천'))") == 0) {
            throw new IllegalStateException("BoardDemoSeeder를 먼저 실행해 주세요. (공지·영화 추천 글이 예시 글보다 아래에 오도록 하기 위한 순서)");
        }
        if (count(c, "SELECT COUNT(*) FROM posts p JOIN users u ON u.user_id=p.author_id WHERE u.login_id LIKE 'demo\\_%' ESCAPE '\\'") > 0) {
            System.out.println("이미 자유게시판 데모 글이 있습니다. 다시 만들려면 --clean으로 지운 뒤 실행하세요.");
            return;
        }
        ensureUsers(c);
        loadMovies(c);

        // 1) 템플릿 글 90개: 60일 전 ~ 3일 전, 시간 순서대로 저장
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime from = now.minusDays(60), to = now.minusDays(3);
        long span = java.time.Duration.between(from, to).toMinutes();
        List<Long> offsets = new ArrayList<>();
        for (int i = 0; i < TEMPLATE_POSTS; i++) offsets.add((long) (random.nextDouble() * span));
        Collections.sort(offsets);
        java.util.Set<String> usedTitles = new java.util.HashSet<>(), usedContents = new java.util.HashSet<>();
        for (String[] e : EXAMPLES) usedTitles.add(e[0]);
        for (int i = 0; i < TEMPLATE_POSTS; i++) {
            Draft d = templatePost(i);
            for (int attempt = 0; attempt < 12 && (usedTitles.contains(d.title) || usedContents.contains(d.content)); attempt++) d = templatePost(i + attempt + 1);
            usedTitles.add(d.title);
            usedContents.add(d.content);
            insertPost(c, freeBoard, d, from.plusMinutes(offsets.get(i)), false);
        }

        // 2) 예시 글 10개: 마지막에 저장 → 번호가 가장 커서 맨 위. 배열의 마지막 글부터 거꾸로 저장해 첫 글이 맨 위에 오게 함
        LocalDateTime exampleTime = now.minusDays(2);
        for (int i = EXAMPLES.length - 1; i >= 0; i--) {
            Draft d = new Draft(EXAMPLES[i][0], EXAMPLES[i][2], EXAMPLES[i][1] == null ? null : movieIdByTitle(c, EXAMPLES[i][1]), null);
            exampleTime = exampleTime.plusMinutes(150 + random.nextInt(120));
            insertPost(c, freeBoard, d, exampleTime.isAfter(now) ? now.minusMinutes(i) : exampleTime, true);
        }

        System.out.println("자유게시판 데모 글 " + (TEMPLATE_POSTS + EXAMPLES.length) + "개 생성 완료 (예시 " + EXAMPLES.length + "개는 목록 맨 위)");
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT b.name, COUNT(p.post_id) FROM boards b LEFT JOIN posts p ON p.board_id=b.board_id GROUP BY b.name, b.sort_order ORDER BY b.sort_order")) {
            while (r.next()) System.out.println("  " + r.getString(1) + ": " + r.getInt(2) + "개");
        }
    }

    private void clean(Connection c) throws SQLException {
        String demoUsers = "SELECT user_id FROM users WHERE login_id LIKE 'demo\\_%' ESCAPE '\\'";
        String demoPosts = "SELECT post_id FROM posts WHERE author_id IN (" + demoUsers + ")";
        int posts = count(c, "SELECT COUNT(*) FROM (" + demoPosts + ")");
        try (Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM post_likes WHERE user_id IN (" + demoUsers + ") OR post_id IN (" + demoPosts + ")");
            s.executeUpdate("DELETE FROM comments WHERE author_id IN (" + demoUsers + ") OR post_id IN (" + demoPosts + ")");
            for (String sql : new String[]{"DELETE FROM notifications WHERE user_id IN (" + demoUsers + ")", "DELETE FROM login_logs WHERE user_id IN (" + demoUsers + ")"}) {
                try { s.executeUpdate(sql); } catch (SQLException missing) { if (missing.getErrorCode() != 942 && missing.getErrorCode() != 904) throw missing; }
            }
            s.executeUpdate("DELETE FROM posts WHERE author_id IN (" + demoUsers + ")");
            int removedUsers = s.executeUpdate("DELETE FROM users WHERE login_id LIKE 'demo\\_%' ESCAPE '\\'");
            System.out.println("데모 데이터 삭제 완료: 글 " + posts + "개, 데모 회원 " + removedUsers + "명 (댓글·좋아요 포함)");
        }
    }

    // ---------- 템플릿 글 ----------

    private Draft templatePost(int index) {
        switch (index % 8) {
            case 0: { Movie m = pick(null); return new Draft(choose(m.title + " 보고 왔어요", m.title + " 후기 (스포 없음)", m.title + " 짧은 감상"),
                    paragraph(choose(OPENINGS), info(m), choose(IMPRESSIONS) + " 평점 " + m.rating() + "점이 납득이 가는 영화였어요.", choose(CLOSINGS)), m.id, m.genre); }
            case 1: { Movie m = pick(null); return new Draft(choose(m.title + " 보신 분 계신가요?", m.title + " 볼까 말까 고민 중이에요", m.title + " 어떤가요?"),
                    paragraph((m.year == null ? "" : m.year + "년에 나온 ") + m.genre + " 영화라고 해서 관심이 생겼어요.", "줄거리를 보니 \"" + snippet(m.overview) + "\" 이런 내용이더라고요.",
                            "평점은 " + m.rating() + "점인데 실제로 보신 분들 생각이 궁금합니다.", "스포 없이 짧게라도 알려 주시면 감사하겠습니다!"), m.id, m.genre); }
            case 2: { Movie a = pick(null), b = pick(a.genre); if (b.id == a.id) b = pick(null);
                return new Draft(a.title + " vs " + b.title + ", 뭐가 더 좋았나요",
                    paragraph("둘 다 " + a.genre + " 장르라 비교하면서 봤어요.", a.title + " 평점 " + a.rating() + "점, " + b.title + " 평점 " + b.rating() + "점이더라고요.",
                            choose("저는 " + a.title + " 쪽이 더 기억에 남았어요.", "저는 " + b.title + " 쪽이 더 취향이었어요.", "둘 다 장점이 달라서 고르기가 어렵네요."),
                            "여러분은 어느 쪽이 더 좋으셨나요?"), a.id, a.genre); }
            case 3: { Movie m = pick(null); List<Movie> same = sameGenre(m.genre, 3);
                StringBuilder list = new StringBuilder(); for (Movie x : same) list.append("- ").append(x.title).append(x.year == null ? "" : " (" + x.year + ")").append('\n');
                return new Draft(choose(m.genre + " 영화 입문작 추천 부탁드려요", m.genre + " 장르 좋아하시는 분들 추천해 주세요", "요즘 " + m.genre + " 영화에 빠졌어요"),
                    paragraph("최근에 " + m.genre + " 영화를 몇 편 보고 나서 이 장르가 좋아졌어요.", "지금까지 본 건 이 정도예요.\n" + list.toString().trim(),
                            "이 다음으로 볼 만한 작품 추천해 주시면 감사하겠습니다."), m.id, m.genre); }
            case 4: { Movie m = pick(null); return new Draft(choose(m.title + " 두 번째 보니 다르네요", m.title + " 재관람 후기", "오랜만에 " + m.title + " 다시 봤어요"),
                    paragraph("처음 볼 때는 이야기 따라가기 바빴는데 다시 보니 디테일이 눈에 들어오더라고요.", info(m), choose(IMPRESSIONS), "다시 보면 더 좋은 영화 있으면 알려 주세요."), m.id, m.genre); }
            case 5: { Movie m = pick(null); return new Draft("평점 " + m.rating() + "점인 " + m.title + ", 기대만큼이었나요?",
                    paragraph("TMDB 평점 " + m.rating() + "점, 투표 수가 " + String.format("%,d", m.votes) + "개나 되길래 기대하고 봤어요.",
                            choose("평점만큼 좋았어요.", "저는 평점보다 조금 아쉬웠어요.", "평점보다 훨씬 재밌게 봤어요."), info(m), "평점이랑 실제 감상이 다르게 느껴진 영화 있으셨나요?"), m.id, m.genre); }
            case 6: { Movie m = pickWithYear(); List<Movie> sameYear = sameYear(m.year, 3);
                StringBuilder list = new StringBuilder(); for (Movie x : sameYear) list.append("- ").append(x.title).append(" (").append(x.genre).append(")\n");
                return new Draft(m.year + "년 영화 중에 최고는?",
                    paragraph(m.year + "년에 나온 영화들을 정리해 보다가 궁금해졌어요.", "제가 기억나는 건 이 정도예요.\n" + list.toString().trim(), "여러분이 꼽는 " + m.year + "년 최고의 영화는 뭔가요?"), m.id, m.genre); }
            default: { List<Movie> three = new ArrayList<>(); for (int i = 0; i < 3; i++) three.add(pick(null));
                StringBuilder list = new StringBuilder(); for (Movie x : three) list.append("- ").append(x.title).append(" (").append(x.genre).append(")\n");
                return new Draft(choose("이번 주말 몰아볼 영화 정했어요", "주말 영화 리스트 공유해요", "이번 주 영화 계획", "연휴에 볼 영화 골라 봤어요",
                        "요즘 보려고 찜해 둔 영화들", "밤새 볼 영화 순서 추천해 주세요", "다음 주 영화 모임 후보작", "이 세 편 중에 뭐부터 볼까요?"),
                    paragraph(choose("이번 주말은 집에서 영화만 볼 생각이에요.", "오랜만에 쉬는 주말이라 영화 몰아보기 하려고요."), "장르를 섞어서 골라 봤어요.\n" + list.toString().trim(),
                            "순서는 어떻게 보면 좋을까요? 같이 볼 만한 영화도 추천해 주세요."), three.get(0).id, three.get(0).genre); }
        }
    }

    private Movie pick(String genre) {
        List<Movie> pool = new ArrayList<>();
        for (Movie m : movies) if ((genre == null || genre.equals(m.genre)) && movieUses.getOrDefault(m.id, 0) < 2) pool.add(m);
        if (pool.isEmpty()) pool = movies;
        Movie m = pool.get(random.nextInt(Math.min(pool.size(), 150)));
        movieUses.merge(m.id, 1, Integer::sum);
        return m;
    }

    private Movie pickWithYear() {
        for (int i = 0; i < 20; i++) { Movie m = pick(null); if (m.year != null) return m; }
        return pick(null);
    }

    private List<Movie> sameGenre(String genre, int n) {
        List<Movie> pool = new ArrayList<>();
        for (Movie m : movies) if (genre.equals(m.genre)) pool.add(m);
        Collections.shuffle(pool, random);
        return pool.subList(0, Math.min(n, pool.size()));
    }

    private List<Movie> sameYear(String year, int n) {
        List<Movie> pool = new ArrayList<>();
        for (Movie m : movies) if (year != null && year.equals(m.year)) pool.add(m);
        Collections.shuffle(pool, random);
        return pool.subList(0, Math.min(n, pool.size()));
    }

    private static String info(Movie m) {
        return (m.year == null ? "" : m.year + "년 ") + m.genre + " 영화인데, " + snippet(m.overview);
    }

    private static String snippet(String overview) {
        String text = overview == null ? "" : overview.replaceAll("\\s+", " ").trim();
        int dot = text.indexOf(". ");
        if (dot > 20 && dot < 90) return text.substring(0, dot + 1);
        return text.length() > 80 ? text.substring(0, 80).trim() + "…" : text;
    }

    private static String paragraph(String... parts) {
        StringBuilder out = new StringBuilder();
        for (String p : parts) { if (p == null || p.isBlank()) continue; if (out.length() > 0) out.append("\n\n"); out.append(p); }
        return out.toString();
    }

    private String choose(String... options) { return options[random.nextInt(options.length)]; }

    // ---------- DB ----------

    private void insertPost(Connection c, long boardId, Draft d, LocalDateTime createdAt, boolean example) throws SQLException {
        long author = authorFor(d.genre);
        List<Long> others = new ArrayList<>(users.values());
        others.remove(author);
        Collections.shuffle(others, random);
        int comments = random.nextInt(5);
        int likes = random.nextInt(others.size() + 1);
        int views = (example ? 200 + random.nextInt(600) : 15 + random.nextInt(380)) + likes * 20 + comments * 15;
        long postId;
        try (PreparedStatement s = c.prepareStatement("INSERT INTO posts(board_id,author_id,movie_id,title,content,view_count,like_count,comment_count,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,'PUBLISHED',?,?)", new String[]{"post_id"})) {
            s.setLong(1, boardId);
            s.setLong(2, author);
            if (d.movieId == null) s.setNull(3, java.sql.Types.NUMERIC); else s.setLong(3, d.movieId);
            s.setString(4, d.title.length() > 200 ? d.title.substring(0, 200) : d.title);
            s.setString(5, d.content);
            s.setInt(6, views);
            s.setInt(7, likes);
            s.setInt(8, comments);
            s.setTimestamp(9, Timestamp.valueOf(createdAt));
            s.setTimestamp(10, Timestamp.valueOf(createdAt));
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { keys.next(); postId = keys.getLong(1); }
        }
        try (PreparedStatement s = c.prepareStatement("INSERT INTO comments(post_id,author_id,content,status,created_at,updated_at) VALUES(?,?,?,'PUBLISHED',?,?)")) {
            LocalDateTime at = createdAt;
            for (int i = 0; i < comments; i++) {
                at = at.plusMinutes(10 + random.nextInt(300));
                s.setLong(1, postId);
                s.setLong(2, others.get(i % others.size()));
                s.setString(3, choose(COMMENTS));
                s.setTimestamp(4, Timestamp.valueOf(at));
                s.setTimestamp(5, Timestamp.valueOf(at));
                s.executeUpdate();
            }
        }
        try (PreparedStatement s = c.prepareStatement("INSERT INTO post_likes(post_id,user_id,created_at) VALUES(?,?,?)")) {
            for (int i = 0; i < likes; i++) {
                s.setLong(1, postId);
                s.setLong(2, others.get(i));
                s.setTimestamp(3, Timestamp.valueOf(createdAt.plusMinutes(5 + i * 37L)));
                s.executeUpdate();
            }
        }
    }

    private long authorFor(String genre) {
        if (genre != null && random.nextInt(10) < 7) {
            for (String[] u : DEMO_USERS) if (genre.equals(u[2])) return users.get(u[0]);
        }
        List<Long> ids = new ArrayList<>(users.values());
        return ids.get(random.nextInt(ids.size()));
    }

    private void ensureUsers(Connection c) throws SQLException {
        try (PreparedStatement find = c.prepareStatement("SELECT user_id FROM users WHERE login_id=?");
             PreparedStatement insert = c.prepareStatement("INSERT INTO users(login_id,password_hash,nickname,email,role,status) VALUES(?,?,?,?,'USER','ACTIVE')", new String[]{"user_id"})) {
            for (String[] u : DEMO_USERS) {
                find.setString(1, u[0]);
                try (ResultSet r = find.executeQuery()) { if (r.next()) { users.put(u[0], r.getLong(1)); continue; } }
                insert.setString(1, u[0]);
                insert.setString(2, PasswordHasher.hash(UUID.randomUUID().toString()));
                insert.setString(3, u[1]);
                insert.setString(4, u[0] + "@demo.cinehub.test");
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) { keys.next(); users.put(u[0], keys.getLong(1)); }
            }
        }
    }

    private void loadMovies(Connection c) throws SQLException {
        String sql = "SELECT m.movie_id, m.title, TO_CHAR(m.release_date,'YYYY'), m.vote_average, m.vote_count, DBMS_LOB.SUBSTR(m.overview,400,1),"
                + " (SELECT LISTAGG(g.name, '|') WITHIN GROUP (ORDER BY g.genre_id) FROM movie_genres mg JOIN genres g ON g.genre_id=mg.genre_id WHERE mg.movie_id=m.movie_id)"
                + " FROM movies m WHERE m.overview IS NOT NULL AND DBMS_LOB.GETLENGTH(m.overview) > 30 AND m.vote_count >= ? ORDER BY m.popularity DESC";
        for (int minVotes : new int[]{300, 50, 0}) {
            movies.clear();
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setInt(1, minVotes);
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) {
                        String genre = primaryGenre(r.getString(7));
                        if (genre == null) continue;
                        movies.add(new Movie(r.getLong(1), r.getString(2), r.getString(3), r.getDouble(4), r.getInt(5), r.getString(6), genre));
                    }
                }
            }
            if (movies.size() >= 120) break;
        }
        if (movies.isEmpty()) throw new IllegalStateException("영화 데이터가 없습니다. TMDB 영화 채우기를 먼저 실행하세요.");
    }

    /** 여러 장르 중 영화를 가장 잘 설명하는 장르 하나 (애니메이션·공포처럼 성격이 뚜렷한 장르 우선) */
    private static final String[] GENRE_PRIORITY = {"애니메이션", "다큐멘터리", "공포", "음악", "서부", "전쟁", "역사", "로맨스", "SF", "판타지",
        "미스터리", "범죄", "스릴러", "코미디", "가족", "액션", "모험", "드라마", "TV 영화"};

    private static String primaryGenre(String joined) {
        if (joined == null) return null;
        List<String> genres = java.util.Arrays.asList(joined.split("\\|"));
        for (String g : GENRE_PRIORITY) if (genres.contains(g)) return g;
        return genres.get(0);
    }

    private static Long movieIdByTitle(Connection c, String title) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT movie_id FROM movies WHERE title=? ORDER BY popularity DESC FETCH FIRST 1 ROWS ONLY")) {
            s.setString(1, title);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : null; }
        }
    }

    private static long boardId(Connection c, String name) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT board_id FROM boards WHERE name=?")) {
            s.setString(1, name);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException(name + " 게시판이 없습니다. OracleSchemaSetup을 먼저 실행하세요.");
                return r.getLong(1);
            }
        }
    }

    private static int count(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    private static final class Movie {
        final long id; final String title, year, overview, genre; final double vote; final int votes;
        Movie(long id, String title, String year, double vote, int votes, String overview, String genre) {
            this.id = id; this.title = title; this.year = year; this.vote = vote; this.votes = votes; this.overview = overview; this.genre = genre;
        }
        String rating() { return String.format("%.1f", vote); }
    }

    private static final class Draft {
        final String title, content, genre; final Long movieId;
        Draft(String title, String content, Long movieId, String genre) { this.title = title; this.content = content; this.movieId = movieId; this.genre = genre; }
    }
}
