const CONTEXT_PATH = (() => {
    const path = window.location.pathname;
    if (path === '/' || !path.includes('/')) return '';
    if (path.endsWith('/')) return path.slice(0, -1);
    return path.substring(0, path.lastIndexOf('/'));
})();

function apiUrl(path) {
    return `${CONTEXT_PATH}${path}`;
}

async function api(path, options = {}) {
    const config = {credentials: 'same-origin', ...options};
    if (config.body && !(config.body instanceof FormData)) {
        config.headers = {'Content-Type': 'application/json', ...(config.headers || {})};
    }
    const response = await fetch(apiUrl(path), config);
    let payload;
    try {
        payload = await response.json();
    } catch (_) {
        throw new Error(`서버 응답을 읽을 수 없습니다. HTTP ${response.status}`);
    }
    if (!response.ok || payload.success === false) {
        const error = new Error(payload.message || '요청 처리에 실패했습니다.');
        error.code = payload.errorCode;
        error.status = response.status;
        throw error;
    }
    return payload.data;
}

function won(value) {
    return `${Math.round(Number(value || 0)).toLocaleString('ko-KR')}원`;
}

function numberOnly(value) {
    return Math.round(Number(value || 0)).toLocaleString('ko-KR');
}

function formatDate(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
    return date.toLocaleString('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
}

function escapeHtml(value) {
    return String(value ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

function query(name, fallback = null) {
    const value = new URLSearchParams(location.search).get(name);
    return value === null || value === '' ? fallback : value;
}

function merchantDefaultAmount(merchant) {
    const code = merchant?.categoryCode;
    if (code === 'FOOD') return merchant?.merchantName?.includes('커피') || merchant?.brandName?.includes('스타벅스') ? 4800 : 18000;
    if (code === 'MART') return 24000;
    if (code === 'CULTURE') return 15000;
    if (code === 'MEDICAL') return 12000;
    if (code === 'FUEL') return 50000;
    return 10000;
}

function showToast(message, type = 'info') {
    document.querySelectorAll('.toast').forEach(el => el.remove());
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 220);
    }, 2500);
}

function setButtonLoading(button, loading, text = '처리 중...') {
    if (!button) return;
    if (loading) {
        button.dataset.originalText = button.innerHTML;
        button.innerHTML = text;
        button.disabled = true;
    } else {
        button.innerHTML = button.dataset.originalText || button.innerHTML;
        button.disabled = false;
    }
}

function benefitLabel(payment) {
    const discount = Number(payment.discountAmount || 0);
    const reward = Number(payment.rewardAmount || 0);
    if (discount > 0) return `-${won(discount)} 할인`;
    if (reward > 0) return `+${won(reward)} 적립`;
    return '혜택 없음';
}

function navIcon(name) {
    const icons = {
        home: '<svg viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5v8a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>',
        nearby: '<svg viewBox="0 0 24 24"><path d="M12 21s7-6 7-12a7 7 0 1 0-14 0c0 6 7 12 7 12z"/><circle cx="12" cy="9" r="2.5"/></svg>',
        pay: '<svg viewBox="0 0 24 24"><path d="M4 4h4v4H4zM16 4h4v4h-4zM4 16h4v4H4zM11 4h2v3h-2zM11 9h3v2h-3zM16 11h4v2h-4zM10 14h3v3h-3zM15 15h2v2h-2zM18 17h2v3h-3v-2h1zM11 19h4v2h-4z"/></svg>',
        cards: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 9h18"/></svg>'
    };
    return icons[name] || '';
}

function renderBottomNav(active) {
    const nav = document.getElementById('bottomNav');
    if (!nav) return;
    const items = [
        ['home', 'index.html', '홈'], ['nearby', 'nearby.html', '주변'], ['pay', 'pay.html', '결제'], ['cards', 'cards.html', '카드']
    ];
    nav.innerHTML = items.map(([key, href, label]) => `<a class="bottom-nav-item ${active === key ? 'active' : ''}" href="${href}">
    ${navIcon(key)}<span>${label}</span></a>`).join('');
}

function pageReady(active) {
    renderBottomNav(active);
    document.body.classList.add('ready');
}
