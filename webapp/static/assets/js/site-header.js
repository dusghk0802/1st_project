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

    function markActive() {
        var path = location.pathname;
        Object.keys(aliases).forEach(function (prefix) { if (path.indexOf(prefix) === 0) path = aliases[prefix]; });
        var best = null, bestLength = -1;
        document.querySelectorAll('.main-menu > li > a').forEach(function (link) {
            var href = link.getAttribute('href');
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
    }

    function init() {
        markActive();
        fetch('/api/me').then(function (r) { return r.ok ? r.json() : null; }).then(applySession).catch(function () { });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
