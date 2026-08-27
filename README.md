# CINEHUB

영화 검색, AI 기반 추천, 영화 커뮤니티를 하나로 묶은 순수 Java 웹 애플리케이션입니다.

Spring Boot 없이 Java `HttpServer`로 동작하며, Oracle XE에는 회원·게시글·영화 데이터를 저장하고 Qdrant와 OpenAI 임베딩을 활용해 자연어 영화 검색(RAG)을 제공합니다.

## 주요 기능

- 회원가입, 로그인, 프로필·비밀번호 변경
- 공지사항·자유게시판·영화 추천 게시판
- 게시글 작성·수정·삭제, 댓글, 신고
- 관리자 신고 처리, 회원 상태 관리, 게시글 숨김
- 알림 조회 및 문의 접수
- TMDB 영화 포스터·줄거리 데이터 기반 영화 검색
- Qdrant 벡터 검색과 키워드 검색을 결합한 영화 RAG
- 인기 영화 TOP 5, 랜덤 영화 추천, 영화 상세 화면

## 기술 구성

| 구분 | 사용 기술 |
| --- | --- |
| Backend | Java 17+, JDK `HttpServer` |
| Database | Oracle XE / JDBC |
| Vector DB | Qdrant |
| AI | OpenAI Embeddings |
| Movie data | TMDB |
| Frontend | HTML, CSS, Vanilla JavaScript |

## 실행 전 준비

1. Oracle XE를 설치하고 실행합니다.
2. Qdrant를 `http://127.0.0.1:6333`에서 실행합니다.
3. 아래 환경 변수를 설정합니다.

```powershell
[Environment]::SetEnvironmentVariable('DB_PASSWORD', 'Oracle 비밀번호', 'User')
[Environment]::SetEnvironmentVariable('OPENAI_API_KEY', 'OpenAI API 키', 'User')
```

필요한 Oracle JDBC 드라이버(`ojdbc11`)와 SLF4J JAR 파일을 `lib/` 폴더에 둡니다. 이 라이브러리 파일과 개인 키·로그·데이터베이스 파일은 저장소에 올리지 않습니다.

## 실행

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

실행 후 브라우저에서 `http://127.0.0.1:8088`로 접속합니다.

## 프로젝트 구조

```text
src/project/                 Java HTTP 서버, Oracle DAO, 보안, RAG 구현
src/project/*Seeder.java     게시판 예시 데이터 생성 도구
webapp/static/assets/        CINEHUB 화면 스타일과 정적 자산
run.ps1                      컴파일과 서버 실행 스크립트
```

## 보안 주의사항

- API 키·DB 비밀번호는 코드나 Git에 저장하지 않습니다.
- 외부 공개 시에는 8088 포트만 방화벽/공유기에서 열고 Oracle(1521)과 Qdrant(6333)는 외부에 노출하지 않습니다.
