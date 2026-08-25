-- Movie Community database schema based on the supplied DBML ERD.
-- Target: MySQL 8.0+ / UTF-8
CREATE DATABASE IF NOT EXISTS movie_community_db DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE movie_community_db;

CREATE TABLE users (
 user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, login_id VARCHAR(30) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 nickname VARCHAR(30) NOT NULL UNIQUE, email VARCHAR(255) NOT NULL UNIQUE, role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
 status ENUM('ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE login_logs (
 log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, user_id BIGINT UNSIGNED NOT NULL, ip_address VARCHAR(45) NOT NULL, user_agent VARCHAR(255),
 status ENUM('SUCCESS','FAILED') NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_login_logs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX ix_login_logs_user_created (user_id, created_at), INDEX ix_login_logs_ip_created (ip_address, created_at)
);
CREATE TABLE admin_action_logs (
 log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, admin_id BIGINT UNSIGNED NOT NULL, action_type VARCHAR(50) NOT NULL,
 target_type ENUM('USER','POST','COMMENT') NOT NULL, target_id BIGINT UNSIGNED NOT NULL, reason VARCHAR(255), ip_address VARCHAR(45) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_admin_logs_admin FOREIGN KEY (admin_id) REFERENCES users(user_id),
 INDEX ix_admin_logs_admin_created (admin_id, created_at), INDEX ix_admin_logs_target (target_type, target_id)
);
CREATE TABLE reports (
 report_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, reporter_id BIGINT UNSIGNED NOT NULL, target_type ENUM('POST','COMMENT','USER') NOT NULL,
 target_id BIGINT UNSIGNED NOT NULL, reason VARCHAR(255) NOT NULL, status ENUM('PENDING','REVIEWED','RESOLVED','REJECTED') NOT NULL DEFAULT 'PENDING',
 resolved_by BIGINT UNSIGNED, resolved_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(user_id),
 CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by) REFERENCES users(user_id),
 INDEX ix_reports_target (target_type, target_id), INDEX ix_reports_status_created (status, created_at)
);

CREATE TABLE movies (
 movie_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, tmdb_id BIGINT UNSIGNED NOT NULL UNIQUE, title VARCHAR(255) NOT NULL, original_title VARCHAR(255),
 title_en VARCHAR(255), overview TEXT, overview_en TEXT, runtime INT UNSIGNED, release_date DATE, poster_path VARCHAR(255), poster_local_path VARCHAR(500), backdrop_path VARCHAR(255),
 vote_average DECIMAL(3,1) NOT NULL DEFAULT 0.0, vote_count INT UNSIGNED NOT NULL DEFAULT 0, popularity DECIMAL(10,3) NOT NULL DEFAULT 0.0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX ix_movies_title (title), INDEX ix_movies_release_date (release_date), INDEX ix_movies_popularity (popularity)
);
CREATE TABLE genres (genre_id INT UNSIGNED PRIMARY KEY, name VARCHAR(50) NOT NULL);
CREATE TABLE movie_genres (
 movie_id BIGINT UNSIGNED NOT NULL, genre_id INT UNSIGNED NOT NULL, PRIMARY KEY (movie_id, genre_id),
 CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
 CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE CASCADE
);
CREATE TABLE movie_likes (
 movie_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (movie_id, user_id),
 CONSTRAINT fk_movie_likes_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
 CONSTRAINT fk_movie_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- TMDB에서 수집한 리뷰 원문과, 선택적으로 OpenAI가 만든 한국어 검색 보조 문장.
-- 벡터 DB에는 review_id와 semantic_text(없으면 content)를 payload로 함께 저장한다.
CREATE TABLE movie_reviews (
 review_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 movie_id BIGINT UNSIGNED NOT NULL,
 tmdb_review_id VARCHAR(80) NOT NULL,
 author VARCHAR(255),
 language_code VARCHAR(10),
 content MEDIUMTEXT NOT NULL,
 content_en MEDIUMTEXT,
 semantic_text TEXT,
 source_created_at DATETIME NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_movie_reviews_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
 UNIQUE KEY uk_movie_reviews_tmdb_id (tmdb_review_id),
 INDEX ix_movie_reviews_movie (movie_id),
 INDEX ix_movie_reviews_language (language_code)
);

CREATE TABLE boards (board_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE, description VARCHAR(255), sort_order INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE categories (
 category_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, board_id BIGINT UNSIGNED NOT NULL, name VARCHAR(50) NOT NULL, sort_order INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_categories_board FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE, UNIQUE KEY uk_categories_board_name (board_id, name)
);
CREATE TABLE posts (
 post_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, board_id BIGINT UNSIGNED NOT NULL, category_id BIGINT UNSIGNED, author_id BIGINT UNSIGNED NOT NULL, movie_id BIGINT UNSIGNED,
 title VARCHAR(200) NOT NULL, content TEXT NOT NULL, view_count INT UNSIGNED NOT NULL DEFAULT 0, like_count INT UNSIGNED NOT NULL DEFAULT 0, comment_count INT UNSIGNED NOT NULL DEFAULT 0,
 status ENUM('PUBLISHED','HIDDEN','DELETED') NOT NULL DEFAULT 'PUBLISHED', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_posts_board FOREIGN KEY (board_id) REFERENCES boards(board_id), CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
 CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(user_id), CONSTRAINT fk_posts_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE SET NULL,
 INDEX ix_posts_board_created (board_id, created_at), INDEX ix_posts_author_created (author_id, created_at), INDEX ix_posts_category_created (category_id, created_at), INDEX ix_posts_movie_created (movie_id, created_at)
);
CREATE TABLE comments (
 comment_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, post_id BIGINT UNSIGNED NOT NULL, author_id BIGINT UNSIGNED NOT NULL, parent_comment_id BIGINT UNSIGNED,
 content TEXT NOT NULL, status ENUM('PUBLISHED','HIDDEN','DELETED') NOT NULL DEFAULT 'PUBLISHED', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE, CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(user_id),
 CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(comment_id) ON DELETE CASCADE, INDEX ix_comments_post_parent_created (post_id, parent_comment_id, created_at)
);
CREATE TABLE post_likes (
 post_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (post_id, user_id),
 CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE, CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

INSERT INTO boards (name, description, sort_order) VALUES
 ('자유게시판','자유롭게 이야기하는 공간',1), ('영화 리뷰','영화 감상과 리뷰를 공유하는 공간',2), ('영화 질문','영화 관련 질문을 남기는 공간',3);
