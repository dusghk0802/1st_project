(() => {
  const query = document.getElementById('movie-query');
  const results = document.getElementById('movie-results');
  const status = document.getElementById('movie-status');
  const escapeHtml = (value) => String(value || '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const poster = (id) => `/movies/poster/${id}`;

  function movieCard(movie) {
    return `<article class="movie-card" data-id="${movie.id}">
      <button type="button" class="movie-card-button" aria-label="${escapeHtml(movie.title)} 상세 보기">
        <img src="${poster(movie.id)}" alt="${escapeHtml(movie.title)} 포스터" loading="lazy">
        <div class="movie-card-copy"><span>${escapeHtml(movie.en)}</span><h3>${escapeHtml(movie.title)}</h3><p>${escapeHtml(movie.overview || '줄거리 정보가 없습니다.')}</p><b>상세 보기 <i>→</i></b></div>
      </button></article>`;
  }

  function renderCards(items) {
    results.innerHTML = items.map(movieCard).join('');
    results.querySelectorAll('.movie-card-button').forEach(button => button.addEventListener('click', () => loadDetail(button.closest('.movie-card').dataset.id)));
  }

  async function getJson(url) {
    const response = await fetch(url, { cache: 'no-store' });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || '요청을 처리하지 못했습니다.');
    return data;
  }

  async function search() {
    const text = query.value.trim();
    if (!text) { status.textContent = '검색어를 입력해 주세요.'; query.focus(); return; }
    status.textContent = '영화를 찾고 있습니다…'; results.innerHTML = '';
    try {
      const data = await getJson(`/movies/search?q=${encodeURIComponent(text)}`);
      if (!data.results || !data.results.length) { status.textContent = '검색 결과가 없습니다. 다른 표현으로 검색해 보세요.'; return; }
      status.textContent = data.imported
        ? `TMDB에서 영화를 가져와 저장했습니다. 관련 영화 ${data.results.length}개를 함께 보여드립니다.`
        : `${data.results.length}개의 영화를 찾았습니다.`;
      renderCards(data.results);
    } catch (error) { status.textContent = error.message; }
  }

  async function randomMovie() {
    status.textContent = '오늘의 영화를 고르는 중…'; results.innerHTML = '';
    try { const movie = await getJson('/movies/random'); status.textContent = '오늘의 랜덤 추천입니다.'; renderCards([movie]); }
    catch (error) { status.textContent = error.message; }
  }

  async function loadDetail(id) {
    status.textContent = '영화 정보를 불러오는 중…'; results.innerHTML = '';
    try {
      const movie = await getJson(`/movies/detail/${id}`);
      const similar = movie.similar || [];
      results.innerHTML = `<article class="movie-detail"><button class="movie-back" type="button">← 검색 결과로</button><div class="movie-detail-main"><img src="${poster(movie.id)}" alt="${escapeHtml(movie.title)} 포스터"><div><span class="movie-eyebrow">FILM DETAIL</span><h2>${escapeHtml(movie.title)}</h2><h3>${escapeHtml(movie.en)}</h3><p class="movie-meta">개봉 ${escapeHtml(movie.release || '정보 없음')} · 평점 ${escapeHtml(movie.voteAverage || '-')} (${escapeHtml(movie.voteCount || 0)}명)</p><p>${escapeHtml(movie.overview || '줄거리 정보가 없습니다.')}</p></div></div><div class="movie-similar"><h3>이 영화와 비슷한 영화</h3><div class="movie-grid">${similar.length ? similar.map(movieCard).join('') : '<p>추천할 유사 영화가 아직 없습니다.</p>'}</div></div></article>`;
      status.textContent = '영화 상세 정보';
      results.querySelector('.movie-back').addEventListener('click', () => { results.innerHTML = ''; status.textContent = '제목 또는 줄거리를 검색해 보세요.'; });
      results.querySelectorAll('.movie-card-button').forEach(button => button.addEventListener('click', () => loadDetail(button.closest('.movie-card').dataset.id)));
    } catch (error) { status.textContent = error.message; }
  }

  document.getElementById('movie-search-button').addEventListener('click', search);
  document.getElementById('movie-random-button').addEventListener('click', randomMovie);
  query.addEventListener('keydown', event => { if (event.key === 'Enter') search(); });
})();
