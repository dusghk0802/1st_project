(function () {
  function safeText(value) {
    var node = document.createElement('div');
    node.textContent = value || '';
    return node.innerHTML;
  }

  function paging(page, pages) {
    if (typeof window.controls === 'function') {
      window.controls('reportPaging', page, pages, function (nextPage) {
        window.reportPage = nextPage;
        window.renderReports();
      });
    }
  }

  window.renderReports = function () {
    var query = (document.getElementById('reportQuery').value || '').toLowerCase();
    var reports = window.reportData || [];
    var filtered = query ? reports.filter(function (report) {
      return (report.type + ' ' + report.reason + ' ' + report.reporter).toLowerCase().indexOf(query) >= 0;
    }) : reports;
    var pages = Math.max(1, Math.ceil(filtered.length / 5));
    if (window.reportPage >= pages) window.reportPage = 0;
    var items = filtered.slice(window.reportPage * 5, window.reportPage * 5 + 5);
    document.getElementById('reports').innerHTML = items.map(function (report) {
      var isPending = report.status === 'PENDING';
      var action = isPending
        ? '<button class="btn-main" onclick="resolveReport(' + report.id + ',\'RESOLVED\')">처리 및 숨김</button>'
          + '<button class="btn-main" onclick="resolveReport(' + report.id + ',\'REJECTED\')">반려</button>'
        : '<span class="muted">' + (report.status === 'RESOLVED' ? '처리 완료 · 대상 숨김' : '반려됨 · 원본 유지') + '</span>';
      return '<article class="post-card admin-report-row"><b>' + safeText(report.type) + ' #' + report.targetId + '</b>'
        + '<p>신고 사유: ' + safeText(report.reason) + '</p><small class="muted">신고자 ' + safeText(report.reporter) + ' · ' + safeText(report.status) + '</small>'
        + '<div class="report-actions">' + action + '</div></article>';
    }).join('') || '<p class="muted">신고가 없습니다.</p>';
    paging(window.reportPage, pages);
  };

  window.resolveReport = function (id, status) {
    var question = status === 'RESOLVED'
      ? '신고 대상 콘텐츠를 숨김 처리할까요?'
      : '신고를 반려하고 원본을 유지할까요?';
    if (!window.confirm(question)) return;
    window.fetch('/1st_project/api/admin/reports/' + id, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': window.csrf() },
      body: 'status=' + status
    }).then(function (response) { return response.json(); }).then(function (data) {
      if (data.error) { window.alert(data.error); return; }
      window.alert(data.message || '신고 처리 상태가 변경되었습니다.');
      window.loadReports();
      window.loadStats();
      window.loadPosts();
    }).catch(function () { window.alert('신고를 처리하지 못했습니다. 다시 시도해 주세요.'); });
  };

  window.setTimeout(function () {
    if (typeof window.loadReports === 'function') window.loadReports();
  }, 0);
}());
