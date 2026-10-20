// 게시글 목록 페이지 번호: 5개씩 묶어서 보여 주고, 앞뒤 묶음은 ‹ › 화살표로 이동한다.
// 사용법: renderPagination(navElement, 현재페이지(1부터), 전체페이지수, function (새페이지) { ... })
(function () {
    var GROUP_SIZE = 5;

    window.renderPagination = function (nav, page, pages, onChange) {
        if (!nav) return;
        pages = Math.max(1, pages);
        var groupStart = Math.floor((page - 1) / GROUP_SIZE) * GROUP_SIZE + 1;
        var groupEnd = Math.min(pages, groupStart + GROUP_SIZE - 1);
        var html = '';

        html += "<button type='button' class='page-arrow' data-page='" + (groupStart - 1) + "' aria-label='이전 페이지 묶음'" + (groupStart === 1 ? ' disabled' : '') + ">&lsaquo;</button>";
        for (var n = groupStart; n <= groupEnd; n++) {
            html += "<button type='button' class='page-number" + (n === page ? " active' aria-current='page'" : "'") + " data-page='" + n + "'>" + n + "</button>";
        }
        html += "<button type='button' class='page-arrow' data-page='" + (groupEnd + 1) + "' aria-label='다음 페이지 묶음'" + (groupEnd === pages ? ' disabled' : '') + ">&rsaquo;</button>";

        nav.innerHTML = html;
        Array.prototype.forEach.call(nav.querySelectorAll('button'), function (button) {
            button.onclick = function () {
                var target = Number(button.getAttribute('data-page'));
                if (!button.disabled && target >= 1 && target <= pages && target !== page) onChange(target);
            };
        });
    };
})();
