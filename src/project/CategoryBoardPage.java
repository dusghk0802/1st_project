package project;

/** Category page with a direct return and the same paging behavior as the full board list. */
final class CategoryBoardPage {
    private CategoryBoardPage() {}

    static String render(long boardId) {
        return "<!doctype html><html lang='ko'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>게시판 | CINEHUB</title>"
            + "<link rel='stylesheet' href='/static/assets/css/style.css'><script src='/static/assets/js/site-header.js' defer></script><script src='/static/assets/js/pagination.js'></script><link rel='stylesheet' href='/static/assets/css/widgets.css'><link rel='stylesheet' href='/static/assets/css/responsive.css'><link rel='stylesheet' href='/static/assets/css/post-layout.css'></head><body class='logged-in'>"
            + ""+CinehubServer.siteHeader()+""
            + "<main><div class='container'><a class='board-back' href='/posts'>← 전체 게시판</a><div class='widget-header-3'><h1 class='widget-title' id='boardTitle'>게시판</h1></div><nav class='pagination pagination-top' id='postPaginationTop'></nav><div id='posts'><p class='muted'>게시글을 불러오는 중입니다.</p></div><nav class='pagination' id='postPagination'></nav></div></main><footer class='main-footer'><div class='container'><p class='text-center text-muted'>CINEHUB · 영화 탐색 커뮤니티</p></div></footer>"
            + "<script>function safe(v){var d=document.createElement('div');d.textContent=v||'';return d.innerHTML;}var id=" + boardId + ";fetch('/api/boards').then(function(r){return r.json();}).then(function(d){var b=(d.boards||[]).filter(function(x){return x.id===id;})[0];if(b)document.getElementById('boardTitle').textContent=b.name;});fetch('/api/posts?boardId='+id).then(function(r){return r.json();}).then(function(d){var a=d.posts||[],page=1,draw=function(){var pages=Math.max(1,Math.ceil(a.length/10));document.getElementById('posts').innerHTML=a.slice((page-1)*10,page*10).map(function(p){return '<a class=\"post-card\" href=\"/post/'+p.id+'\"><h3>'+safe(p.title)+'</h3><p>'+safe(p.content).slice(0,100)+'</p><small>'+safe(p.author)+' · '+safe(p.createdAt)+'</small></a>';}).join('')||'<p class=\"muted\">이 게시판에는 아직 게시글이 없습니다.</p>';['postPaginationTop','postPagination'].forEach(function(navId){renderPagination(document.getElementById(navId),page,pages,function(n){page=n;draw();window.scrollTo({top:0,behavior:'smooth'});});});};draw();});</script></body></html>";
    }
}
