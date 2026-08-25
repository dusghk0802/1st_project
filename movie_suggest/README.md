# CineMatch

Java 11 movie search server with MySQL, TMDB fallback, and a local Qdrant semantic-search service.

## Required services

- MySQL 8: `movie_community_db`
- Qdrant: `http://127.0.0.1:6333`
- Python packages: `fastembed`, `qdrant-client`, `pymysql`

Set `DB_PASSWORD` and `TMDB_ACCESS_TOKEN` as environment variables. `TMDB_ACCESS_TOKEN` is needed only when a locally missing movie must be fetched from TMDB.

## Start

```powershell
$env:DB_PASSWORD = 'your_password'
python vector_service.py

javac -encoding UTF-8 -cp ".\lib\mysql-connector-j-9.3.0.jar" -d out src\community\Database.java src\community\SimpleJson.java src\community\TmdbClient.java src\community\TmdbOnDemandImporter.java src\community\MovieSearchServer.java
java -cp "out;lib\mysql-connector-j-9.3.0.jar" community.MovieSearchServer 8089
```

Open `http://127.0.0.1:8089`.
