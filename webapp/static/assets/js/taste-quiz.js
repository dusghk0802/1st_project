function bindTasteQuiz() {
  var start = document.getElementById('tasteStartButton');
  if (!start) return;
  var choices = [], current = 0, likes = [], dislikes = [], unknowns = [];

  function card(movie) {
    var poster = movie.poster
      ? '<img src="' + safe(movie.poster) + '" alt="' + safe(movie.title) + '">'
      : '<div class="taste-poster-empty">NO POSTER</div>';
    return '<article class="taste-movie">' + poster
      + '<h2>' + safe(movie.title) + '</h2><p class="muted">평점 ' + Number(movie.voteAverage || 0).toFixed(1) + '</p>'
      + '<div><button type="button" data-rate="like" data-id="' + movie.id + '">♥ 좋아요</button>'
      + '<button type="button" data-rate="dislike" data-id="' + movie.id + '">싫어요</button>'
      + '<button type="button" data-rate="unknown" data-id="' + movie.id + '">잘 모르겠어요</button></div></article>';
  }

  function render() {
    var choice = choices[current], box = document.getElementById('tastePair'), progress = document.getElementById('tasteProgress');
    if (!choice) { progress.textContent = '선택 완료! GPT가 당신의 취향에 맞는 영화를 고르는 중입니다.'; box.innerHTML = ''; recommend(); return; }
    progress.textContent = (current + 1) + ' / ' + choices.length;
    box.innerHTML = '<section class="taste-pair"><p class="muted">이 영화를 어떻게 느끼시나요?</p><div class="taste-movies taste-movies-single">' + card(choice.movie) + '</div></section>';
    Array.prototype.forEach.call(box.querySelectorAll('[data-rate]'), function (button) {
      button.onclick = function () {
        if (button.disabled) return;
        var id = Number(button.getAttribute('data-id'));
        var rate = button.getAttribute('data-rate');
        (rate === 'like' ? likes : rate === 'dislike' ? dislikes : unknowns).push(id);
        button.parentElement.querySelectorAll('button').forEach(function (item) { item.disabled = true; });
        button.classList.add('selected');
        setTimeout(function () { current++; render(); }, 260);
      };
    });
  }

  function recommend() {
    fetch('/api/taste-quiz/recommend', {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'likes=' + encodeURIComponent(likes.join(',')) + '&dislikes=' + encodeURIComponent(dislikes.join(',')) + '&unknowns=' + encodeURIComponent(unknowns.join(','))
    }).then(function (response) { return response.json(); }).then(function (data) {
      var result = document.getElementById('tasteResult'), movie = (data.results || [])[0];
      result.style.display = 'block';
      if (data.error || !movie) { result.innerHTML = '<p class="muted">' + safe(data.error || '추천 결과를 찾지 못했습니다.') + '</p>'; return; }
      result.innerHTML = '<article class="post-card taste-result-card">' + (movie.poster ? '<img src="' + safe(movie.poster) + '" alt="' + safe(movie.title) + '">' : '')
        + '<div><span class="muted">GPT TASTE PICK</span><h2>' + safe(movie.title) + '</h2><p>' + safe(movie.reason || '선택한 취향을 바탕으로 추천한 작품입니다.') + '</p>'
        + '<a class="btn-main balance-detail-link" href="/movies/' + movie.id + '">영화 상세·한줄평 보기</a></div></article>';
    }).catch(function () { document.getElementById('tasteResult').innerHTML = '<p class="muted">추천을 불러오지 못했습니다.</p>'; });
  }

  start.onclick = function () {
    start.disabled = true; document.getElementById('tasteProgress').textContent = '장르별 영화를 준비하는 중입니다.';
    fetch('/api/taste-quiz/pairs').then(function (response) { return response.json(); }).then(function (data) {
      choices = [];
      (data.genres || []).forEach(function (genre) { (genre.movies || []).forEach(function (movie) { choices.push({ genre: genre.name, movie: movie }); }); });
      if (!choices.length) { document.getElementById('tasteProgress').textContent = '준비된 영화가 부족합니다.'; start.disabled = false; return; }
      document.getElementById('tasteStart').style.display = 'none'; render();
    }).catch(function () { document.getElementById('tasteProgress').textContent = '테스트를 시작하지 못했습니다.'; start.disabled = false; });
  };
}
