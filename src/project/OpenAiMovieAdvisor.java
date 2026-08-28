package project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Uses the OpenAI Responses API to turn natural-language requests into movie search terms. */
public final class OpenAiMovieAdvisor {
    public static final class Recommendation {
        private final long movieId;
        private final String reason;

        public Recommendation(long movieId, String reason) {
            this.movieId = movieId;
            this.reason = reason;
        }

        public long getMovieId() {
            return movieId;
        }

        public String getReason() {
            return reason;
        }
    }

    /** A title selected by the model before its public metadata is fetched from TMDB. */
    public static final class TitleRecommendation {
        private final String title;
        private final String reason;

        public TitleRecommendation(String title, String reason) {
            this.title = title;
            this.reason = reason;
        }

        public String getTitle() { return title; }
        public String getReason() { return reason; }
    }
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private final String apiKey = env("OPENAI_API_KEY", "");
    private final String model = env("OPENAI_MOVIE_MODEL", "gpt-4o-mini");

    public boolean configured() {
        return !apiKey.isBlank();
    }

    public String searchTerms(String request) throws Exception {
        String prompt = "You are a movie search assistant. Convert this Korean movie request into 3 to 6 short "
                + "Korean or English search keywords for a local movie catalog. Return only comma-separated keywords. "
                + "Request: " + request;
        return responseText(prompt);
    }

    public Recommendation chooseFromCandidates(String request, List<Long> ids, List<String> titles) throws Exception {
        StringBuilder candidates = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            candidates.append(ids.get(i)).append('|').append(titles.get(i)).append('\n');
        }
        String prompt = "A user wants this: " + request + "\n"
                + "Pick the best one movie from the following candidate catalog. "
                + "Return only: numeric_id|one concise Korean recommendation reason.\n" + candidates;
        String response = responseText(prompt);
        Matcher match = Pattern.compile("\\d+").matcher(response);
        if (!match.find()) throw new IllegalStateException("OpenAI did not return a movie id");
        long id = Long.parseLong(match.group());
        String reason = response.substring(match.end()).replaceFirst("^[|\\s:.-]+", "").trim();
        return new Recommendation(ids.contains(id) ? id : ids.get(0), reason);
    }

    /** Lets GPT select a movie from broad film knowledge; the caller resolves public metadata through TMDB. */
    public TitleRecommendation recommendFromTaste(String tasteContext) throws Exception {
        String prompt = "Recommend exactly one real, feature-length movie for this viewer. "
                + "Taste context: " + tasteContext + "\n"
                + "Consider story, theme, conflict, mood, and emotional arc. Avoid films similar to the disliked examples. "
                + "Return only: official movie title|exactly two Korean sentences. Sentence one must connect a liked story or mood to the choice. "
                + "Sentence two must cite a concrete story, emotional, or atmosphere element from the choice. Do not use generic wording.";
        String response = responseText(prompt).trim();
        int divider = response.indexOf('|');
        if (divider < 1) throw new IllegalStateException("OpenAI did not return a movie title");
        String title = response.substring(0, divider).trim();
        String reason = response.substring(divider + 1).trim();
        if (title.isBlank() || reason.isBlank()) throw new IllegalStateException("OpenAI returned an incomplete recommendation");
        return new TitleRecommendation(title, reason);
    }

    /** Chooses from candidates using titles and synopses, so preference recommendations consider story as well as genre. */
    public Recommendation chooseFromDetailedCandidates(String request, List<Long> ids, List<String> titles, List<String> overviews) throws Exception {
        StringBuilder candidates = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            String overview = i < overviews.size() ? overviews.get(i) : "";
            if (overview.length() > 350) overview = overview.substring(0, 350);
            candidates.append(ids.get(i)).append('|').append(titles.get(i)).append(" | 줄거리: ").append(overview).append('\n');
        }
        String prompt = "사용자의 영화 취향 정보입니다. " + request + "\n"
                + "제목뿐 아니라 줄거리의 갈등, 분위기, 소재, 감정선을 함께 비교해 후보 중 가장 어울리는 한 편을 선택하세요. "
                + "싫어한 영화와 비슷한 줄거리나 소재는 피하세요. "
                + "Return only: numeric_id|exactly two Korean sentences. The first sentence must connect a selected story, theme, or mood preference to this movie. The second sentence must name a specific plot, emotional, or atmosphere element in this movie. Never use generic wording such as '오늘의 추천입니다'.\n" + candidates;
        String response = responseText(prompt);
        Matcher match = Pattern.compile("\\d+").matcher(response);
        if (!match.find()) throw new IllegalStateException("OpenAI did not return a movie id");
        long id = Long.parseLong(match.group());
        String reason = response.substring(match.end()).replaceFirst("^[|\\s:.-]+", "").trim();
        return new Recommendation(ids.contains(id) ? id : ids.get(0), reason);
    }

    public String explainSearch(String request, List<String> titles) throws Exception {
        String prompt = "A user searched for movies with this request: " + request + "\n"
                + "These are the matched movie titles: " + String.join(", ", titles) + "\n"
                + "Write one concise Korean sentence explaining why these titles fit the request. "
                + "Return only that sentence.";
        return responseText(prompt);
    }

    /** Returns a concise, Korean reason for every candidate shown in a random recommendation set. */
    public Map<Long, String> explainCandidates(List<Long> ids, List<String> titles) throws Exception {
        StringBuilder candidates = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            candidates.append(ids.get(i)).append('|').append(titles.get(i)).append('\n');
        }
        String prompt = "These are curated movie candidates. For every line, write one concise Korean reason "
                + "why a general viewer could enjoy it today. Return exactly one line per candidate as "
                + "numeric_id|Korean reason. Do not omit any id.\n" + candidates;
        Map<Long, String> reasons = new LinkedHashMap<>();
        for (String line : responseText(prompt).split("\\r?\\n")) {
            int divider = line.indexOf('|');
            if (divider <= 0) continue;
            try {
                long id = Long.parseLong(line.substring(0, divider).trim());
                String reason = line.substring(divider + 1).trim();
                if (ids.contains(id) && !reason.isBlank()) reasons.put(id, reason);
            } catch (NumberFormatException ignored) {
                // Ignore a malformed line and use the local fallback below.
            }
        }
        return reasons;
    }

    private String responseText(String input) throws Exception {
        if (!configured()) throw new IllegalStateException("OPENAI_API_KEY is not configured");
        String body = "{\"model\":\"" + json(model) + "\",\"store\":false,\"input\":\"" + json(input) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI error " + response.statusCode());
        }
        Matcher matcher = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\\\.|[^\\\"])*)\\\"").matcher(response.body());
        String text = "";
        while (matcher.find()) text = unescape(matcher.group(1));
        if (text.isBlank()) throw new IllegalStateException("OpenAI response was empty");
        return text.trim();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                if (next == 'n') out.append('\n');
                else if (next == 't') out.append('\t');
                else if (next == 'u' && i + 4 < value.length()) {
                    String hex = value.substring(i + 1, i + 5);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    } catch (NumberFormatException invalidUnicode) {
                        out.append('u');
                    }
                } else out.append(next);
            } else out.append(c);
        }
        return out.toString();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
