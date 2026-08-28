/**
 * CINEHUB 프로젝트 Google Slides 자동 생성 스크립트
 *
 * Google Apps Script에서 실행:
 * 1) script.google.com → 새 프로젝트
 * 2) 이 파일 전체 붙여넣기
 * 3) createCinehubPresentation() 실행
 */

const COLORS = {
  NAVY: '#1A2438',
  BLUE: '#2D6CDF',
  PINK: '#E94B7A',
  GREEN: '#2EA66F',
  INK: '#1E293B',
  MUTED: '#64748B',
  WHITE: '#FFFFFF',
  LINE: '#E2E8F0',
  CARD: '#F8FAFC',
  PEACH: '#DEC1AF'
};

function createCinehubPresentation() {
  const presentation = SlidesApp.create('CINEHUB - 영화 추천 & 커뮤니티 플랫폼 발표자료');
  const initialSlide = presentation.getSlides()[0];

  createCover(presentation);
  createPurpose(presentation);
  createArchitecture(presentation);
  createHybridRecommendation(presentation);
  createFeatures(presentation);
  createErd(presentation);
  createStack(presentation);
  createTeam(presentation);
  createClosing(presentation);

  initialSlide.remove();
  Logger.log('CINEHUB 발표자료 생성 완료: ' + presentation.getUrl());
}

function createCover(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);
  slide.getBackground().setSolidFill(COLORS.NAVY);

  addBlock(slide, 42, 42, 70, 4, COLORS.PINK);
  addText(slide, 'AI MOVIE COMMUNITY PLATFORM', 42, 92, 650, 22, {
    fontSize: 12, color: COLORS.PINK, bold: true
  });
  addText(slide, 'CINEHUB', 42, 124, 650, 58, {
    fontSize: 46, color: COLORS.WHITE, bold: true
  });
  addText(slide, 'DB · RAG · OpenAI를 결합한 자연어 영화 추천과 커뮤니티 서비스', 42, 202, 650, 28, {
    fontSize: 15, color: COLORS.PEACH
  });
  addText(slide, '2026.08 | 팀 CINEHUB', 42, 338, 650, 20, {
    fontSize: 11, color: COLORS.PEACH
  });
}

function createPurpose(presentation) {
  const slide = newSlide(
    presentation,
    '01. 프로젝트 개요',
    '제목 검색을 넘어, 상황과 감정으로 영화를 탐색하는 경험을 만듭니다.'
  );
  const cards = [
    ['문제 정의', '자연어로 표현한 취향은 제목·장르 중심의 단순 검색만으로 찾기 어렵습니다.', COLORS.PINK],
    ['해결 방안', 'Oracle 영화 데이터와 Qdrant 시맨틱 검색으로 후보를 만들고 OpenAI가 추천 이유를 정리합니다.', COLORS.BLUE],
    ['목표 가치', '영화 탐색, 상세 정보, 감상 공유, 댓글과 운영 기능을 하나의 커뮤니티로 연결합니다.', COLORS.GREEN]
  ];
  cards.forEach((item, index) => {
    const x = 42 + index * 292;
    addCard(slide, x, 142, 270, 160, item[2]);
    addText(slide, item[0], x + 18, 164, 230, 24, { fontSize: 14, color: COLORS.INK, bold: true });
    addText(slide, item[1], x + 18, 207, 230, 74, { fontSize: 11, color: COLORS.MUTED });
  });
}

function createArchitecture(presentation) {
  const slide = newSlide(
    presentation,
    '02. 시스템 아키텍처',
    'Java HttpServer를 중심으로 데이터 저장·벡터 검색·AI 판단을 역할별로 분리했습니다.'
  );
  const layers = [
    ['Presentation Layer', 'HTML5 · CSS3 · Vanilla JavaScript', '화면 렌더링, fetch 기반 요청, 반응형 커뮤니티 UI'],
    ['Application Layer', 'Java 17 · JDK HttpServer · JDBC', '세션 인증, 역할 기반 권한, CSRF 토큰, 입력값 이스케이프'],
    ['Data & AI Layer', 'Oracle XE · TMDB · Qdrant · OpenAI', '영화·커뮤니티 저장, 시맨틱 검색, 최종 추천과 이유 생성']
  ];
  layers.forEach((layer, index) => {
    const y = 126 + index * 92;
    addCard(slide, 42, y, 876, 72, index === 1 ? COLORS.PINK : COLORS.BLUE);
    addText(slide, layer[0], 62, y + 12, 220, 20, { fontSize: 13, color: COLORS.BLUE, bold: true });
    addText(slide, layer[1], 62, y + 38, 260, 18, { fontSize: 10, color: COLORS.MUTED });
    addText(slide, layer[2], 350, y + 27, 535, 22, { fontSize: 11, color: COLORS.INK });
  });
}

function createHybridRecommendation(presentation) {
  const slide = newSlide(
    presentation,
    '03. 하이브리드 추천 엔진',
    'DB와 RAG가 후보를 찾고, OpenAI가 후보 중 한 편과 추천 이유를 선택합니다.'
  );
  const blocks = [
    ['1. 후보 수집', 'Oracle 키워드 검색과 Qdrant 줄거리 시맨틱 검색 결과를 결합해 최대 10편의 후보를 구성합니다.', COLORS.BLUE],
    ['2. 최종 추천', 'OpenAI Responses API가 후보 중 한 편을 고르고, 사용자의 자연어 요청에 맞춘 한국어 추천 이유를 생성합니다.', COLORS.PINK],
    ['3. 안전한 폴백', '외부 AI 호출에 실패해도 DB·RAG 후보 결과를 반환합니다. 랜덤 추천도 DB 후보에서 선택합니다.', COLORS.GREEN]
  ];
  blocks.forEach((block, index) => {
    const x = 42 + index * 292;
    addCard(slide, x, 145, 270, 180, block[2]);
    addText(slide, block[0], x + 18, 168, 230, 24, { fontSize: 14, color: COLORS.INK, bold: true });
    addText(slide, block[1], x + 18, 211, 230, 92, { fontSize: 10.5, color: COLORS.MUTED });
  });
  addText(slide, '자연어 질의  →  DB + RAG 후보군 생성  →  OpenAI 최종 선택 + 추천 이유', 42, 374, 850, 25, {
    fontSize: 17, color: COLORS.INK, bold: true
  });
}

function createFeatures(presentation) {
  const slide = newSlide(
    presentation,
    '04. 핵심 기능',
    '영화 탐색부터 커뮤니티와 관리자 운영까지의 흐름을 제공합니다.'
  );
  const items = [
    ['영화 탐색', '자연어 영화 검색 · 랜덤 추천 · 영화 상세 페이지', COLORS.BLUE],
    ['커뮤니티', '공지사항 · 자유게시판 · 영화 추천 게시판 · 댓글 · 좋아요', COLORS.PINK],
    ['개인화', '회원별 게시글 조회수 1회 집계 · 알림 · 마이페이지', COLORS.GREEN],
    ['관리자 기능', '회원 상태 관리 · 신고 처리 · 게시글 숨김 · 운영 로그', COLORS.PINK],
    ['보안', '세션 기반 인증 · 역할 권한 · CSRF · 차단 회원 로그인 제한', COLORS.BLUE],
    ['데이터 파이프라인', 'TMDB 영화 데이터 수집 · Oracle 저장 · Qdrant 벡터 인덱스', COLORS.GREEN]
  ];
  items.forEach((item, index) => {
    const col = index % 3;
    const row = Math.floor(index / 3);
    const x = 42 + col * 292;
    const y = 132 + row * 132;
    addCard(slide, x, y, 270, 108, item[2]);
    addText(slide, item[0], x + 18, y + 16, 230, 22, { fontSize: 13, color: COLORS.INK, bold: true });
    addText(slide, item[1], x + 18, y + 48, 230, 42, { fontSize: 10, color: COLORS.MUTED });
  });
}

function createErd(presentation) {
  const slide = newSlide(
    presentation,
    '05. 데이터 모델링 (ERD)',
    '영화·커뮤니티·운영 데이터를 분리하고 관계를 통해 연결했습니다.'
  );
  const tables = [
    ['USERS', '회원 식별자, password_hash, 닉네임, 권한(ROLE), 상태(STATUS)'],
    ['MOVIES', '내부 movie_id PK, TMDB tmdb_id UNIQUE, 제목, 줄거리, 포스터, 평점'],
    ['POSTS / COMMENTS', '게시글·댓글·좋아요·회원별 조회수, parent_comment_id 관계'],
    ['REPORTS / ADMIN_LOGS', '신고 상태, 관리자 처리 이력, 로그인 및 운영 감사 로그'],
    ['MOVIE_GENRES / MOVIE_LIKES', '영화-장르 다대다 관계와 회원 영화 찜 상태']
  ];
  tables.forEach((table, index) => {
    const y = 125 + index * 53;
    addCard(slide, 42, y, 876, 42, index % 2 === 0 ? COLORS.BLUE : COLORS.PINK);
    addText(slide, table[0], 62, y + 11, 260, 18, { fontSize: 11, color: COLORS.INK, bold: true });
    addText(slide, table[1], 330, y + 11, 555, 18, { fontSize: 10, color: COLORS.MUTED });
  });
  addText(slide, 'ERD 이미지: docs/erd.png  |  DBML 원본: docs/ERD.dbml', 42, 414, 840, 18, {
    fontSize: 10, color: COLORS.MUTED
  });
}

function createStack(presentation) {
  const slide = newSlide(
    presentation,
    '06. 기술 스택',
    '현재 구현에 사용 중인 기술만 정리했습니다.'
  );
  const stacks = [
    ['Backend', 'Java 17 · JDK HttpServer · JDBC'],
    ['Authentication & Security', '세션 기반 인증 · 역할 권한 · CSRF 토큰 · 입력값 이스케이프'],
    ['Database', 'Oracle XE · JDBC'],
    ['AI & Search', 'Qdrant Vector DB · RAG 시맨틱 검색 · OpenAI Responses API'],
    ['Data Pipeline', 'TMDB API · 영화 메타데이터 수집 · Qdrant 인덱싱'],
    ['Frontend & Delivery', 'HTML5 · CSS3 · Vanilla JavaScript · GitHub · README · DBML']
  ];
  stacks.forEach((stack, index) => {
    const y = 118 + index * 48;
    addCard(slide, 42, y, 876, 38, index % 2 === 0 ? COLORS.BLUE : COLORS.PINK);
    addText(slide, stack[0], 62, y + 10, 245, 18, { fontSize: 11, color: COLORS.BLUE, bold: true });
    addText(slide, stack[1], 325, y + 10, 560, 18, { fontSize: 10.5, color: COLORS.INK });
  });
}

function createTeam(presentation) {
  const slide = newSlide(
    presentation,
    '07. 팀 역할 및 WBS 마일스톤',
    '설계부터 구현·테스트까지 역할을 나누고 기능을 통합했습니다.'
  );
  const stages = [
    ['1단계', '기획 & 설계', '요구사항 정의, UI/UX 와이어프레임, Oracle DBML·ERD 설계'],
    ['2단계', '백엔드 & AI 개발', 'Java 세션·권한·CSRF, TMDB 수집, Qdrant RAG, OpenAI 추천 연동'],
    ['3단계', '프론트엔드 연동', 'HTML/CSS/JavaScript 화면, 영화 검색·커뮤니티·관리자 UI 연결'],
    ['4단계', '테스트 & 최적화', '기능 QA, 권한 검증, 데이터·추천 응답 점검, 발표 자료 정리']
  ];
  stages.forEach((stage, index) => {
    const y = 132 + index * 63;
    addCard(slide, 42, y, 876, 50, index % 2 === 0 ? COLORS.PINK : COLORS.BLUE);
    addText(slide, stage[0], 62, y + 15, 60, 18, { fontSize: 11, color: COLORS.PINK, bold: true });
    addText(slide, stage[1], 140, y + 15, 170, 18, { fontSize: 11, color: COLORS.INK, bold: true });
    addText(slide, stage[2], 325, y + 15, 550, 18, { fontSize: 10, color: COLORS.MUTED });
  });
}

function createClosing(presentation) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);
  slide.getBackground().setSolidFill(COLORS.NAVY);
  addBlock(slide, 42, 42, 70, 4, COLORS.PINK);
  addText(slide, 'CINEHUB', 42, 112, 700, 55, { fontSize: 42, color: COLORS.WHITE, bold: true });
  addText(slide, '더 잘 찾고, 더 깊게 나누는 영화 커뮤니티', 42, 184, 800, 28, {
    fontSize: 20, color: COLORS.PEACH
  });
  addText(slide, '한 줄 회고', 42, 294, 240, 22, { fontSize: 14, color: COLORS.PEACH, bold: true });
  addText(slide, '“서로 다른 역할의 결과를 하나의 사용자 경험으로 연결하며, 데이터와 AI를 실제 서비스로 완성하는 과정을 배웠습니다.”', 42, 340, 820, 60, {
    fontSize: 17, color: COLORS.WHITE, bold: true
  });
  addText(slide, 'THANK YOU  |  Q&A', 42, 462, 720, 22, { fontSize: 12, color: COLORS.PEACH, bold: true });
}

function newSlide(presentation, title, subtitle) {
  const slide = presentation.appendSlide(SlidesApp.PredefinedLayout.BLANK);
  slide.getBackground().setSolidFill(COLORS.WHITE);
  addText(slide, title, 42, 30, 876, 30, { fontSize: 18, color: COLORS.INK, bold: true });
  addText(slide, subtitle, 42, 62, 876, 20, { fontSize: 10, color: COLORS.MUTED });
  addBlock(slide, 42, 92, 876, 1, COLORS.LINE);
  addText(slide, 'CINEHUB  |  ' + String(presentation.getSlides().length).padStart(2, '0'), 42, 500, 876, 12, {
    fontSize: 8, color: COLORS.MUTED, align: SlidesApp.ParagraphAlignment.RIGHT
  });
  return slide;
}

function addText(slide, text, x, y, width, height, options) {
  const shape = slide.insertTextBox(text, x, y, width, height);
  const range = shape.getText();
  const style = range.getTextStyle();
  style.setFontFamily('Noto Sans KR');
  style.setFontSize(options.fontSize || 12);
  style.setForegroundColor(options.color || COLORS.INK);
  style.setBold(Boolean(options.bold));
  if (options.align) {
    range.getParagraphs().forEach(function(paragraph) {
      paragraph.getRange().getParagraphStyle().setParagraphAlignment(options.align);
    });
  }
  return shape;
}

function addBlock(slide, x, y, width, height, color) {
  const shape = slide.insertShape(SlidesApp.ShapeType.RECTANGLE, x, y, width, height);
  shape.getFill().setSolidFill(color);
  shape.getBorder().setTransparent();
  return shape;
}

function addCard(slide, x, y, width, height, accent) {
  const shape = slide.insertShape(SlidesApp.ShapeType.ROUND_RECTANGLE, x, y, width, height);
  shape.getFill().setSolidFill(COLORS.CARD);
  shape.getBorder().getLineFill().setSolidFill(COLORS.LINE);
  shape.getBorder().setWeight(1);
  addBlock(slide, x, y, 5, height, accent);
  return shape;
}
