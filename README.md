# CINEHUB

> 영화 탐색, AI 의미 검색(RAG), 커뮤니티를 하나로 연결한 순수 Java 기반 웹 애플리케이션

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Oracle](https://img.shields.io/badge/Database-Oracle%20XE-F80000?logo=oracle&logoColor=white)
![Qdrant](https://img.shields.io/badge/Vector%20DB-Qdrant-DC244C)
![License](https://img.shields.io/badge/Backend-Java%20HttpServer-1f2937)

## 소개

CINEHUB는 영화 정보를 검색하고, 자연어 문장으로 분위기에 맞는 영화를 찾으며, 영화 이야기를 나누는 커뮤니티입니다. Spring Boot 없이 JDK의 `HttpServer`, JDBC, Vanilla JavaScript로 구성했습니다.

## 주요 기능

- 회원가입, 로그인/로그아웃, 프로필·비밀번호 변경, 회원탈퇴
- 계정 상태 관리: 활성, 정지, 탈퇴 계정의 세션·로그인 차단
- 공지사항, 자유게시판, 영화 추천 게시판
- 게시글 작성·수정·삭제, 조회수, 사용자별 좋아요, 댓글, 신고
- 알림 센터와 관리자 대시보드
- 관리자 회원 정지, 게시글 숨김, 신고 처리, 로그인/관리 감사 로그
- TMDB 기반 영화 데이터, 포스터, 인기 TOP 5, 랜덤 추천, 영화 상세
- OpenAI Embeddings + Qdrant + Oracle 키워드 검색을 결합한 영화 RAG 검색

## 기술 구성

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17+, JDK `HttpServer` |
| Frontend | HTML, CSS, Vanilla JavaScript |
| RDBMS | Oracle XE, JDBC |
| Vector DB | Qdrant |
| AI | OpenAI Embeddings API |
| Movie Data | TMDB API |

## 서비스 구조

서비스 구조도와 RAG 검색 흐름은 [SERVICE_ARCHITECTURE.md](docs/SERVICE_ARCHITECTURE.md)에서 확인할 수 있습니다.

## ERD

[ERD.dbml](docs/ERD.dbml)은 현재 Oracle 스키마를 기준으로 작성한 dbdiagram.io용 코드입니다.

1. [dbdiagram.io](https://dbdiagram.io/)를 엽니다.
2. [ERD.dbml](docs/ERD.dbml)의 전체 내용을 붙여 넣습니다.
3. 생성된 ERD를 PNG 또는 PDF로 내보낼 수 있습니다.

## 실행 전 준비

- JDK 17 이상
- Oracle XE 및 Oracle JDBC Driver (`ojdbc11`)
- Qdrant (`http://127.0.0.1:6333` 기본값)
- 환경 변수 `DB_PASSWORD`, `OPENAI_API_KEY`

라이브러리 JAR 파일은 용량과 라이선스 관리상 Git에 포함하지 않습니다. 아래 파일을 `lib/`에 준비합니다.

```text
lib/ojdbc11-23.5.0.24.07.jar
lib/slf4j-api-2.0.13.jar
lib/slf4j-simple-2.0.13.jar
```

Windows PowerShell에서 환경 변수를 설정합니다.

```powershell
[Environment]::SetEnvironmentVariable('DB_PASSWORD', 'Oracle_비밀번호', 'User')
[Environment]::SetEnvironmentVariable('OPENAI_API_KEY', 'OpenAI_API_키', 'User')
```

## 실행

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1 -OraclePassword "Oracle_비밀번호"
```

실행 후 브라우저에서 `http://127.0.0.1:8088`로 접속합니다.

## 프로젝트 구조

```text
src/project/
  CinehubServer.java       HTTP 라우팅, API, 세션, CSRF
  dao/                     Oracle JDBC DAO
  service/                 로그인 검증과 비밀번호 해싱
  JavaRagService.java      OpenAI·Qdrant REST 기반 RAG
  *Seeder.java             초기 커뮤니티 데이터 생성
webapp/
  db/miniproject.sql       Oracle 기본 스키마
  static/assets/           CINEHUB 화면 정적 자산
docs/
  ERD.dbml                 dbdiagram.io ERD 코드
  SERVICE_ARCHITECTURE.md  서비스 구조도와 RAG 흐름
run.ps1                    컴파일·서버 실행 스크립트
```

## 보안 주의

`.env`, API 키, Oracle 비밀번호, 로그, DB 파일, 컴파일 결과물은 Git에 올리지 않습니다. 외부 공개 시에는 Oracle(1521)과 Qdrant(6333)를 인터넷에 직접 노출하지 마세요.
