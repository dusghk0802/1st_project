"""Local multilingual embedding + Qdrant service used by the Java web server."""
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import pymysql
from fastembed import TextEmbedding
from qdrant_client import QdrantClient, models

COLLECTION = "movie_semantic_search"
MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
MODEL_CACHE = r"E:\test\embedding-models"
embedding = TextEmbedding(model_name=MODEL_NAME, cache_dir=MODEL_CACHE)
qdrant = QdrantClient(host="127.0.0.1", port=6333)


def database():
    return pymysql.connect(host="127.0.0.1", port=3306, user=os.getenv("DB_USER", "root"),
                           password=os.getenv("DB_PASSWORD", ""), database="movie_community_db",
                           charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor)


def ensure_collection():
    if not qdrant.collection_exists(COLLECTION):
        qdrant.create_collection(COLLECTION, vectors_config=models.VectorParams(size=384, distance=models.Distance.COSINE))


def movie_text(row):
    return "\n".join(value for value in [row.get("title"), row.get("title_en"), row.get("overview"), row.get("overview_en")] if value)


def index_movie(movie_id):
    with database() as conn, conn.cursor() as cur:
        cur.execute("SELECT movie_id,tmdb_id,title,title_en,overview,overview_en,poster_local_path FROM movies WHERE movie_id=%s", (movie_id,))
        row = cur.fetchone()
    if not row:
        return False
    vector = next(embedding.embed([movie_text(row)])).tolist()
    qdrant.upsert(COLLECTION, points=[models.PointStruct(id=int(row["movie_id"]), vector=vector, payload={
        "movie_id": int(row["movie_id"]), "tmdb_id": int(row["tmdb_id"]), "title": row["title"], "title_en": row["title_en"] or ""
    })])
    return True


def sync_all():
    ensure_collection()
    with database() as conn, conn.cursor() as cur:
        cur.execute("SELECT movie_id,tmdb_id,title,title_en,overview,overview_en FROM movies")
        rows = cur.fetchall()
    points = []
    for row, vector in zip(rows, embedding.embed([movie_text(row) for row in rows])):
        points.append(models.PointStruct(id=int(row["movie_id"]), vector=vector.tolist(), payload={
            "movie_id": int(row["movie_id"]), "tmdb_id": int(row["tmdb_id"]), "title": row["title"], "title_en": row["title_en"] or ""
        }))
    if points:
        qdrant.upsert(COLLECTION, points=points)
    return len(points)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        try:
            path, _, query = self.path.partition("?")
            args = dict(item.split("=", 1) for item in query.split("&") if "=" in item)
            if path == "/health": return self.reply(200, {"status": "ok", "collection": COLLECTION})
            if path == "/search":
                from urllib.parse import unquote_plus
                text = unquote_plus(args.get("q", "")).strip()
                if not text: return self.reply(400, {"error": "query required"})
                ensure_collection()
                vector = next(embedding.embed([text])).tolist()
                points = qdrant.query_points(COLLECTION, query=vector, limit=6, with_payload=True).points
                return self.reply(200, {"ids": [int(point.id) for point in points]})
            self.reply(404, {"error": "not found"})
        except Exception as error:
            self.reply(500, {"error": str(error)})

    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", "0")); payload = json.loads(self.rfile.read(length) or b"{}")
            if self.path == "/sync": return self.reply(200, {"indexed": sync_all()})
            if self.path == "/index/movie":
                ok = index_movie(int(payload["movie_id"]))
                return self.reply(200, {"indexed": ok})
            self.reply(404, {"error": "not found"})
        except Exception as error:
            self.reply(500, {"error": str(error)})

    def reply(self, status, data):
        encoded = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status); self.send_header("Content-Type", "application/json; charset=utf-8"); self.send_header("Content-Length", str(len(encoded))); self.end_headers(); self.wfile.write(encoded)
    def log_message(self, *_): pass


if __name__ == "__main__":
    ensure_collection()
    ThreadingHTTPServer(("127.0.0.1", 8090), Handler).serve_forever()
