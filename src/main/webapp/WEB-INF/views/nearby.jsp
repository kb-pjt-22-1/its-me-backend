<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
    <title>BenePay 주변</title>
    <link rel="stylesheet" href="assets/app.css">
</head>
<body>
<div class="app-shell map-page">
    <div class="map-top">
        <div style="display:flex;justify-content:space-between;align-items:center">
            <div class="map-title">주변</div>
            <div class="header-actions">
                <button class="icon-button">♧</button>
                <button class="icon-button">▮</button>
                <div class="mini-grid">▦</div>
            </div>
        </div>
        <div class="search-box"><span>⌕</span><input id="searchInput" placeholder="매장명 또는 카테고리 검색"></div>
        <div id="categoryBar" class="category-scroll"></div>
    </div>
    <div id="mapCanvas" class="map-canvas">
        <div class="map-block" style="left:2%;top:8%;width:21%;height:33%"></div>
        <div class="map-block" style="left:30%;top:2%;width:22%;height:24%"></div>
        <div class="map-block" style="left:58%;top:8%;width:37%;height:30%"></div>
        <div class="map-block" style="left:2%;top:50%;width:20%;height:25%"></div>
        <div class="map-block" style="left:29%;top:47%;width:25%;height:35%"></div>
        <div class="map-block" style="left:61%;top:51%;width:34%;height:24%"></div>
        <div class="user-dot"></div>
        <button class="location-button">➤</button>
    </div>
    <section class="store-sheet">
        <div class="sheet-handle"></div>
        <div class="sheet-meta"><span>현재 위치 기준</span><span id="storeCount">0곳　 거리순 ↕</span></div>
        <div class="sheet-title">반경 1km 내 제휴 매장</div>
        <div id="storeList" class="store-list">
            <div class="loading">주변 매장을 불러오는 중...</div>
        </div>
    </section>
    <nav id="bottomNav" class="bottom-nav"></nav>
</div>
<script src="assets/common.js"></script>
<script>
    const state = {merchants: [], bookmarks: new Set(), category: 'ALL', search: '', selected: null};
    const categories = [['ALL', '전체'], ['FOOD', '한식·카페'], ['MART', '편의점'], ['MEDICAL', '병원'], ['FUEL', '주유소'], ['CULTURE', '문화']];
    (async function () {
        try {
            const [merchants, bookmarks] = await Promise.all([api('/api/v1/merchants'), api('/api/v1/bookmarks')]);
            state.merchants = merchants;
            bookmarks.forEach(m => state.bookmarks.add(Number(m.merchantId)));
            state.selected = merchants[0]?.merchantId || null;
            renderCategories();
            renderAll();
        } catch (e) {
            showToast(e.message, 'error');
        }
        pageReady('nearby');
    })();

    function renderCategories() {
        document.getElementById('categoryBar').innerHTML = categories.map(([code, name]) => `<button class="category-chip ${state.category === code ? 'active' : ''}" data-code="${code}">${name}</button>`).join('');
        document.querySelectorAll('.category-chip').forEach(b => b.onclick = () => {
            state.category = b.dataset.code;
            renderCategories();
            renderAll();
        });
    }

    function filtered() {
        const q = state.search.trim().toLowerCase();
        return state.merchants.filter(m => (state.category === 'ALL' || m.categoryCode === state.category) && (!q || `${m.merchantName} ${m.categoryName} ${m.address}`.toLowerCase().includes(q)));
    }

    function renderAll() {
        const list = filtered();
        document.getElementById('storeCount').textContent = `${list.length}곳　 거리순 ↕`;
        renderMarkers(list);
        renderStores(list);
    }

    function renderMarkers(list) {
        const canvas = document.getElementById('mapCanvas');
        canvas.querySelectorAll('.map-marker').forEach(el => el.remove());
        if (!list.length) return;
        const lats = list.map(m => Number(m.latitude)), lngs = list.map(m => Number(m.longitude));
        const minLat = Math.min(...lats), maxLat = Math.max(...lats), minLng = Math.min(...lngs),
            maxLng = Math.max(...lngs);
        list.forEach((m, i) => {
            const x = maxLng === minLng ? 50 : 15 + (Number(m.longitude) - minLng) / (maxLng - minLng) * 70;
            const y = maxLat === minLat ? 50 : 18 + (maxLat - Number(m.latitude)) / (maxLat - minLat) * 62;
            const btn = document.createElement('button');
            btn.className = `map-marker ${Number(state.selected) === Number(m.merchantId) ? 'active' : ''}`;
            btn.style.left = x + '%';
            btn.style.top = y + '%';
            btn.innerHTML = '⌖';
            btn.title = m.merchantName;
            btn.onclick = () => {
                state.selected = m.merchantId;
                renderAll();
                document.getElementById(`store-${m.merchantId}`)?.scrollIntoView({behavior: 'smooth', block: 'center'});
            };
            canvas.appendChild(btn);
        });
    }

    function categoryIcon(code) {
        return code === 'FOOD' ? '♨' : code === 'MART' ? '▣' : code === 'MEDICAL' ? '✚' : code === 'FUEL' ? '⛽' : code === 'CULTURE' ? '◈' : '●';
    }

    function benefitText(m) {
        if (m.categoryCode === 'FOOD') return m.merchantName.includes('커피') || m.brandName?.includes('스타벅스') ? '등록 10% 할인' : '등록 5% 캐시백';
        if (m.categoryCode === 'MART') return '기본 3% 적립';
        return '보유 카드 혜택 확인';
    }

    function renderStores(list) {
        const el = document.getElementById('storeList');
        if (!list.length) {
            el.innerHTML = '<div class="empty-state">조건에 맞는 매장이 없습니다.</div>';
            return;
        }
        el.innerHTML = list.map((m, i) => `<article id="store-${m.merchantId}" class="store-card ${Number(state.selected) === Number(m.merchantId) ? 'highlight' : ''}" data-id="${m.merchantId}"><div class="store-icon">${categoryIcon(m.categoryCode)}</div><div><div class="store-name">${escapeHtml(m.merchantName)}</div><div class="store-meta">${escapeHtml(m.categoryName)} · ${80 + i * 70}m · ★ ${(4.6 + (i % 2) * .1).toFixed(1)}</div><span class="store-benefit">${benefitText(m)}</span><a class="store-pay" href="pay.html?merchantId=${m.merchantId}&amount=${merchantDefaultAmount(m)}">바로 결제하기 →</a></div><button class="bookmark-btn ${state.bookmarks.has(Number(m.merchantId)) ? 'active' : ''}" data-bookmark="${m.merchantId}">${state.bookmarks.has(Number(m.merchantId)) ? '▮' : '▯'}</button></article>`).join('');
        document.querySelectorAll('[data-bookmark]').forEach(btn => btn.onclick = async (e) => {
            e.preventDefault();
            e.stopPropagation();
            await toggleBookmark(Number(btn.dataset.bookmark));
        });
        document.querySelectorAll('.store-card').forEach(card => card.onclick = e => {
            if (e.target.closest('a,button')) return;
            state.selected = Number(card.dataset.id);
            renderAll();
        });
    }

    async function toggleBookmark(id) {
        try {
            if (state.bookmarks.has(id)) {
                await api(`/api/v1/bookmarks/${id}`, {method: 'DELETE'});
                state.bookmarks.delete(id);
                showToast('북마크를 해제했어요.');
            } else {
                await api('/api/v1/bookmarks', {method: 'POST', body: JSON.stringify({merchantId: id})});
                state.bookmarks.add(id);
                showToast('매장을 저장했어요. 홈 간편결제에서 바로 이용할 수 있어요.', 'success');
            }
            renderAll();
        } catch (e) {
            showToast(e.message, 'error');
        }
    }

    document.getElementById('searchInput').addEventListener('input', e => {
        state.search = e.target.value;
        renderAll();
    });
</script>
</body>
</html>
