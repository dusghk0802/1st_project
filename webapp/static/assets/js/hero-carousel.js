// 홈 메인 배너 캐러셀: 기존 CINEHUB 배너 다음에 TMDB 인기 영화 배경 이미지를 자동으로 넘겨 보여 준다.
(function () {
    var INTERVAL = 5000;
    var IMAGE_BASE = 'https://image.tmdb.org/t/p/w1280';

    var root = document.getElementById('heroCarousel');
    if (!root) return;
    var dots = root.querySelector('.hero-dots');
    var slides = [], current = 0, timer = null, hovering = false;

    function escapeHtml(v) { var d = document.createElement('div'); d.textContent = v || ''; return d.innerHTML; }

    function show(index) {
        if (!slides.length) return;
        current = (index + slides.length) % slides.length;
        slides.forEach(function (slide, i) { slide.classList.toggle('is-active', i === current); });
        dots.querySelectorAll('button').forEach(function (dot, i) {
            dot.classList.toggle('is-active', i === current);
            dot.setAttribute('aria-selected', i === current ? 'true' : 'false');
        });
    }

    function start() { stop(); if (slides.length > 1 && !hovering) timer = setInterval(function () { show(current + 1); }, INTERVAL); }
    function stop() { if (timer) { clearInterval(timer); timer = null; } }
    function go(index) { show(index); start(); }

    function buildDots() {
        dots.innerHTML = '';
        slides.forEach(function (_, i) {
            var dot = document.createElement('button');
            dot.type = 'button';
            dot.setAttribute('role', 'tab');
            dot.setAttribute('aria-label', (i + 1) + '번째 배너');
            dot.addEventListener('click', function () { go(i); });
            dots.appendChild(dot);
        });
        root.classList.toggle('has-many', slides.length > 1);
    }

    function addMovieSlide(movie) {
        var slide = document.createElement('a');
        slide.className = 'hero-slide hero-slide-movie';
        slide.href = '/movies/' + movie.id;
        slide.style.backgroundImage = "url('" + IMAGE_BASE + movie.backdrop + "')";
        slide.innerHTML = "<div class='hero-caption'><span class='hero-label'>HOT</span><h1>" + escapeHtml(movie.title) + "</h1></div>";
        root.insertBefore(slide, root.querySelector('.hero-prev'));
        slides.push(slide);
    }

    slides = Array.prototype.slice.call(root.querySelectorAll('.hero-slide'));
    root.querySelector('.hero-prev').addEventListener('click', function () { go(current - 1); });
    root.querySelector('.hero-next').addEventListener('click', function () { go(current + 1); });
    root.addEventListener('mouseenter', function () { hovering = true; stop(); });
    root.addEventListener('mouseleave', function () { hovering = false; start(); });
    var touchX = null;
    root.addEventListener('touchstart', function (e) { touchX = e.touches[0].clientX; stop(); }, { passive: true });
    root.addEventListener('touchend', function (e) {
        if (touchX === null) return;
        var dx = e.changedTouches[0].clientX - touchX; touchX = null;
        if (Math.abs(dx) > 40) go(current + (dx < 0 ? 1 : -1)); else start();
    });
    document.addEventListener('visibilitychange', function () { if (document.hidden) stop(); else start(); });

    fetch('/api/home/banners').then(function (r) { return r.ok ? r.json() : { results: [] }; }).then(function (d) {
        (d.results || []).forEach(function (movie) { if (movie.backdrop) addMovieSlide(movie); });
    }).catch(function () { }).then(function () { buildDots(); show(0); start(); });
})();
