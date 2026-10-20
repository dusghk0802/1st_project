// 모든 페이지 공통 상단 메뉴바: 로그인 상태 표시, 현재 메뉴 밑줄, 로그아웃.
// 로그인 상태는 서버가 HTML에 미리 반영하고, 여기서는 세션 변경 시 보정만 한다.
(function () {
    var aliases = { '/post/': '/posts', '/category/': '/posts', '/new-post': '/posts', '/edit-post/': '/posts', '/user/': '/mypage', '/movies/': '/movies' };

    function byId(id) { return document.getElementById(id); }
    function show(id, display) { var el = byId(id); if (el) el.style.display = display; }

    if (typeof window.logoutNow !== 'function') {
        window.logoutNow = function () {
            var m = document.cookie.match(/(?:^|; )CINEHUB_CSRF=([^;]*)/);
            fetch('/api/auth/logout', { method: 'POST', headers: { 'X-CSRF-Token': m ? decodeURIComponent(m[1]) : '' } })
                .then(function (r) { if (!r.ok) throw new Error('logout failed'); location.href = '/'; })
                .catch(function () { alert('로그아웃에 실패했습니다. 다시 시도해 주세요.'); });
        };
    }

    // GitHub Pages 시연용 페이지처럼 사이트가 /저장소이름/ 아래에 있을 때 그 앞부분을 떼고 비교한다
    var base = window.__CINEHUB_BASE || '/';
    function stripBase(p) { return base !== '/' && p.indexOf(base) === 0 ? '/' + p.slice(base.length) : p; }

    function markActive() {
        var path = stripBase(location.pathname);
        Object.keys(aliases).forEach(function (prefix) { if (path.indexOf(prefix) === 0) path = aliases[prefix]; });
        var best = null, bestLength = -1;
        document.querySelectorAll('.main-menu > li > a').forEach(function (link) {
            var href = stripBase(link.getAttribute('href'));
            var match = href === '/' ? path === '/' : path === href || path.indexOf(href + '/') === 0;
            if (match && href.length > bestLength) { best = link; bestLength = href.length; }
        });
        document.querySelectorAll('.main-menu > li.active').forEach(function (li) { li.classList.remove('active'); });
        if (best) {
            best.parentElement.classList.add('active');
            // 좁은 화면에서 메뉴가 가로 스크롤될 때 현재 메뉴가 보이도록 가운데로 이동
            var nav = document.querySelector('.main-header .main-nav');
            if (nav && nav.scrollWidth > nav.clientWidth) {
                var navBox = nav.getBoundingClientRect(), itemBox = best.getBoundingClientRect();
                nav.scrollLeft += (itemBox.left + itemBox.width / 2) - (navBox.left + navBox.width / 2);
            }
        }
    }

    function applySession(u) {
        if (!u) return;
        document.body.classList.add('logged-in');
        show('navLogin', 'none'); show('navRegister', 'none');
        show('navUser', 'inline-block'); show('navLogout', 'inline-block');
        show('navNotifications', 'list-item');
        if (byId('navMyPage')) byId('navMyPage').textContent = u.nickname + '님';
        if (u.role === 'ADMIN') show('navAdmin', 'list-item');
        // 읽지 않은 알림 수 배지 (서버가 그린 값을 최신으로 맞춤)
        fetch('/api/notifications/count').then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
            if (!d) return;
            var a = document.querySelector('#navNotifications > a');
            if (!a) return;
            var b = a.querySelector('.nav-badge');
            if (d.unread > 0) { if (!b) { b = document.createElement('span'); b.className = 'nav-badge'; a.appendChild(b); } b.textContent = d.unread > 99 ? '99+' : d.unread; }
            else if (b) b.remove();
        }).catch(function () { });
    }

    // 닉네임 버튼: 누르면 마이페이지·로그아웃 드롭다운을 열고 닫는다
    function bindUserMenu() {
        var menu = byId('navUser'), toggle = byId('navMyPage');
        if (!menu || !toggle) return;
        toggle.setAttribute('role', 'button');
        toggle.setAttribute('aria-haspopup', 'true');
        toggle.setAttribute('aria-expanded', 'false');
        function setOpen(open) {
            menu.classList.toggle('open', open);
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        }
        toggle.addEventListener('click', function (e) { e.preventDefault(); setOpen(!menu.classList.contains('open')); });
        document.addEventListener('click', function (e) { if (!menu.contains(e.target)) setOpen(false); });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape') setOpen(false); });
    }

    function init() {
        markActive();
        bindUserMenu();
        fetch('/api/me').then(function (r) { return r.ok ? r.json() : null; }).then(applySession).catch(function () { });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
