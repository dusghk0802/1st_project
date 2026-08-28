Attribute VB_Name = "CINEHUB_Presentation"
Option Explicit

Private Const NAVY As Long = &H38241A
Private Const BLUE As Long = &HDF6C2D
Private Const PINK As Long = &H7A4BE9
Private Const INK As Long = &H352218
Private Const MUTED As Long = &H8A7061
Private Const WHITE As Long = &HFFFFFF
Private Const LINE As Long = &HEEE1D9

Public Sub CreateCinehubPresentation()
    Dim p As Presentation
    Set p = Presentations.Add
    p.PageSetup.SlideSize = ppSlideSizeOnScreen16x9
    
    Cover p
    Purpose p
    Architecture p
    HybridRecommendation p
    Features p
    Erd p
    Stack p
    Team p
    Closing p
    
    MsgBox "CINEHUB 발표 자료가 생성되었습니다. 저장(Ctrl+S)하세요.", vbInformation
End Sub

Private Function NewSlide(ByVal p As Presentation, ByVal title As String, ByVal subtitle As String) As Slide
    Dim s As Slide
    Set s = p.Slides.Add(p.Slides.Count + 1, ppLayoutBlank)
    Block s, 0, 0, 960, 540, WHITE
    TextBox s, title, 42, 42, 870, 40, 27, INK, True
    TextBox s, subtitle, 42, 91, 870, 24, 13, MUTED, False
    Block s, 42, 128, 876, 1, LINE
    TextBox s, "CINEHUB  |  " & Format$(p.Slides.Count, "00"), 42, 506, 876, 14, 8, MUTED, False, ppAlignRight
    Set NewSlide = s
End Function

Private Sub Block(ByVal s As Slide, ByVal x As Single, ByVal y As Single, ByVal w As Single, ByVal h As Single, ByVal color As Long)
    With s.Shapes.AddShape(msoShapeRectangle, x, y, w, h)
        .Fill.ForeColor.RGB = color
        .Line.Visible = msoFalse
    End With
End Sub

Private Sub TextBox(ByVal s As Slide, ByVal value As String, ByVal x As Single, ByVal y As Single, ByVal w As Single, ByVal h As Single, ByVal fontSize As Single, ByVal color As Long, ByVal isBold As Boolean, Optional ByVal align As PpParagraphAlignment = ppAlignLeft)
    With s.Shapes.AddTextbox(msoTextOrientationHorizontal, x, y, w, h).TextFrame.TextRange
        .Text = value
        .Font.Name = "맑은 고딕"
        .Font.Size = fontSize
        .Font.Color.RGB = color
        .Font.Bold = isBold
        .ParagraphFormat.Alignment = align
    End With
End Sub

Private Sub Card(ByVal s As Slide, ByVal x As Single, ByVal y As Single, ByVal title As String, ByVal body As String, ByVal accent As Long)
    With s.Shapes.AddShape(msoShapeRoundedRectangle, x, y, 270, 125)
        .Fill.ForeColor.RGB = WHITE
        .Line.ForeColor.RGB = LINE
    End With
    Block s, x, y, 5, 125, accent
    TextBox s, title, x + 18, y + 17, 230, 23, 16, INK, True
    TextBox s, body, x + 18, y + 52, 230, 56, 12, MUTED, False
End Sub

Private Sub Cover(ByVal p As Presentation)
    Dim s As Slide
    Set s = p.Slides.Add(1, ppLayoutBlank)
    Block s, 0, 0, 960, 540, NAVY
    Block s, 42, 42, 68, 4, PINK
    TextBox s, "CINEHUB", 42, 105, 800, 55, 42, WHITE, True
    TextBox s, "AI 기반 영화 탐색 커뮤니티", 42, 173, 760, 28, 21, RGB(248, 230, 220), False
    TextBox s, "DB · RAG · OpenAI를 결합한 자연어 영화 추천과 커뮤니티 서비스", 42, 220, 850, 25, 15, RGB(222, 193, 175), False
    TextBox s, "PROJECT PRESENTATION  |  2026", 42, 382, 500, 16, 10, RGB(222, 193, 175), True
End Sub

Private Sub Purpose(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "영화를 찾는 경험을 더 자연스럽게", "제목 검색을 넘어, 상황과 감정으로 작품을 탐색합니다.")
    Card s, 42, 175, "문제", "자연어로 표현한 취향은 단순 제목·장르 검색만으로 찾기 어렵습니다.", PINK
    Card s, 345, 175, "해결", "Oracle와 Qdrant로 후보를 만들고 OpenAI가 추천 이유를 정리합니다.", BLUE
    Card s, 648, 175, "확장", "영화 상세와 커뮤니티, 신고·관리자 기능을 하나로 연결합니다.", RGB(111, 166, 46)
End Sub

Private Sub Architecture(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "서비스 아키텍처", "Java 서버를 중심으로 데이터 저장·벡터 검색·AI 판단을 역할별로 분리했습니다.")
    Card s, 42, 190, "Browser", "HTML · CSS · JavaScript", PINK
    Card s, 345, 190, "Java Server", "HttpServer · 세션 · CSRF", BLUE
    Card s, 648, 190, "Oracle XE", "영화·회원·커뮤니티", RGB(111, 166, 46)
    TextBox s, "Application Layer: Java 17 · JDK HttpServer · JDBC", 42, 380, 620, 24, 16, INK, True
    TextBox s, "세션 인증 · 역할 기반 권한 · CSRF 토큰 · 입력값 이스케이프", 42, 418, 720, 24, 16, INK, True
    TextBox s, "Data & AI Layer: Oracle XE · TMDB Batch · Qdrant · OpenAI", 42, 456, 760, 24, 16, INK, True
End Sub

Private Sub HybridRecommendation(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "DB + RAG로 후보를 찾고, AI가 한 편을 고릅니다", "검색 품질과 추천 설명력을 함께 확보하는 2단계 구조입니다.")
    Card s, 42, 175, "① 후보 수집", "Oracle 키워드 검색 · Qdrant 줄거리 시맨틱 검색 · 최대 10편 후보 구성", BLUE
    Card s, 345, 175, "② 최종 추천", "OpenAI Responses API · 후보 중 1편 선택 · 한국어 추천 이유", PINK
    Card s, 648, 175, "③ 안전한 응답", "API 실패 시 후보 결과로 폴백 · 랜덤은 30편 후보에서 AI가 선택", RGB(111, 166, 46)
    TextBox s, "자연어 질의  →  후보군 생성  →  한 편 추천 + 이유", 42, 405, 760, 28, 20, INK, True
End Sub

Private Sub Features(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "CINEHUB 핵심 기능", "탐색, 기록, 운영을 하나의 서비스에서 제공합니다.")
    Card s, 42, 165, "영화 탐색", "자연어 검색 · 랜덤 추천 · 영화 상세", BLUE
    Card s, 345, 165, "커뮤니티", "공지·자유·영화추천 게시판 · 댓글·좋아요", PINK
    Card s, 648, 165, "개인화", "회원별 조회수 1회 집계 · 알림 · 마이페이지", RGB(111, 166, 46)
    Card s, 42, 315, "관리자", "회원 상태 관리 · 신고 처리 · 운영 로그", PINK
    Card s, 345, 315, "보안", "세션 기반 인증·권한 · CSRF · 차단 회원 제한", BLUE
    Card s, 648, 315, "데이터", "TMDB · Oracle · Qdrant 인덱스", RGB(111, 166, 46)
End Sub

Private Sub Erd(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "ERD: 영화·커뮤니티·운영 데이터를 분리해 연결", "핵심 엔터티는 사용자, 영화, 게시글이며 활동과 운영 이력을 별도로 보관합니다.")
    Card s, 42, 175, "회원·보안", "users · login_logs · post_views · notifications", BLUE
    Card s, 345, 175, "커뮤니티·운영", "boards · posts · comments · likes · reports · admin_action_logs", PINK
    Card s, 648, 175, "영화 데이터", "movies · genres · movie_genres · movie_likes", RGB(111, 166, 46)
    TextBox s, "ERD 이미지: docs/erd.png", 42, 405, 400, 25, 16, MUTED, True
    TextBox s, "DBML 원본: docs/ERD.dbml", 42, 440, 400, 25, 16, MUTED, True
End Sub

Private Sub Stack(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "프로젝트 구현 기술", "가벼운 Java HTTP 서버 기반으로 Oracle·Qdrant·OpenAI를 통합했습니다.")
    Card s, 42, 175, "Backend", "Java 17 · JDK HttpServer · JDBC", BLUE
    Card s, 345, 175, "Authentication", "세션 기반 인증 · 역할 권한 · CSRF", PINK
    Card s, 648, 175, "AI / Search", "Qdrant · OpenAI Responses API", RGB(111, 166, 46)
    Card s, 42, 325, "Data", "TMDB API", PINK
    Card s, 345, 325, "Frontend", "HTML · CSS · JavaScript", BLUE
    Card s, 648, 325, "Delivery", "GitHub · README · ERD · 구조 문서", RGB(111, 166, 46)
End Sub

Private Sub Team(ByVal p As Presentation)
    Dim s As Slide: Set s = NewSlide(p, "역할을 나누고, 하나의 영화 경험으로 연결", "기획·AI·프론트엔드·DB·백엔드가 유기적으로 맞물리도록 협업했습니다.")
    Card s, 42, 165, "노건우 | 팀장 · AI", "WBS 관리 · TMDB 데이터 파이프라인 · Qdrant 추천 API", PINK
    Card s, 345, 165, "이연화 | Frontend · DB", "Oracle 모델링·ERD · UI/UX · 메인·영화·커뮤니티 UI", BLUE
    Card s, 648, 165, "조문희 | Backend · API", "인증·인가 · 게시글·댓글 API · 관리자 회원·신고 API", RGB(111, 166, 46)
End Sub

Private Sub Closing(ByVal p As Presentation)
    Dim s As Slide
    Set s = p.Slides.Add(p.Slides.Count + 1, ppLayoutBlank)
    Block s, 0, 0, 960, 540, NAVY
    Block s, 42, 42, 68, 4, PINK
    TextBox s, "CINEHUB", 42, 105, 800, 55, 40, WHITE, True
    TextBox s, "더 잘 찾고, 더 깊게 나누는 영화 커뮤니티", 42, 173, 800, 28, 20, RGB(248, 230, 220), False
    TextBox s, "한 줄 회고", 42, 298, 250, 24, 15, RGB(222, 193, 175), True
    TextBox s, "“서로 다른 역할의 결과를 하나의 사용자 경험으로 연결하며, 데이터와 AI를 실제 서비스로 완성하는 과정을 배웠습니다.”", 42, 340, 830, 60, 18, WHITE, True
    TextBox s, "THANK YOU", 42, 462, 760, 20, 11, RGB(222, 193, 175), True
End Sub
