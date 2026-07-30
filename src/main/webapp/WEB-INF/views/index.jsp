<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
    <title>BenePay 홈</title>
    <link rel="stylesheet" href="assets/app.css">
</head>
<body>
<div class="app-shell">
    <header class="page-header">
        <div class="page-title">홈</div>
        <div class="header-actions">
            <button class="icon-button" aria-label="알림">♧</button>
            <div class="mini-grid">▦</div>
        </div>
    </header>
    <main class="content">
        <section class="greeting"><h1>안녕하세요, <span id="userName">사용자</span>님!</h1>
            <p>오늘도 스마트한 소비를 시작해보세요.</p></section>
        <section class="section">
            <div class="section-title-row">
                <div class="section-title">내 카드 현황</div>
            </div>
            <div id="primaryCard" class="surface primary-card">
                <div class="loading">카드 정보를 불러오는 중...</div>
            </div>
        </section>
        <section class="section home-grid">
            <a id="quickPay" class="quick-pay" href="pay.html">
                <div>
                    <div style="font-weight:900">간편 결제</div>
                    <div id="quickMerchant" class="sub hidden"></div>
                </div>
                <div>
                    <div style="font-size:22px;margin-bottom:5px">⌗</div>
                    <div class="go">지금 결제 →</div>
                </div>
            </a>
            <a class="surface benefit-card" href="history.html">
                <div style="font-weight:900;font-size:14px">이번 달 혜택</div>
                <div class="gift">♧</div>
                <div id="monthlyBenefit" class="benefit-value">0원</div>
                <div class="benefit-caption">할인 및 적립 포함</div>
            </a>
        </section>
        <section class="section">
            <div class="section-title-row">
                <div class="section-title">최근 결제 내역</div>
                <a class="section-link" href="history.html">전체보기</a></div>
            <div id="recentPayments" class="surface payment-list">
                <div class="loading">결제 내역을 불러오는 중...</div>
            </div>
        </section>
    </main>
    <nav id="bottomNav" class="bottom-nav"></nav>
</div>
<script src="assets/common.js"></script>
<script>
    (async function () {
        try {
            const data = await api('/api/v1/home');
            document.getElementById('userName').textContent = data.userName || '사용자';
            renderPrimary(data.primaryCard);
            document.getElementById('monthlyBenefit').textContent = won(data.monthlyBenefit);
            renderQuick(data.quickMerchant);
            renderRecent(data.recentPayments || []);
        } catch (e) {
            showToast(e.message, 'error');
        }
        pageReady('home');
    })();

    function renderPrimary(card) {
        const el = document.getElementById('primaryCard');
        if (!card) {
            el.innerHTML = '<div class="empty-state">등록된 카드가 없습니다.</div>';
            return;
        }
        const target = Number(card.targetAmount || 0), spent = Number(card.spentAmount || 0);
        const progress = target > 0 ? Math.min(100, spent / target * 100) : 100;
        el.innerHTML = `<div class="card-head"><div><span class="tag">주 사용 카드</span><div class="card-name">${escapeHtml(card.cardName)}</div><div class="card-meta">${escapeHtml(card.cardCompanyName)} · ${escapeHtml(card.cardLast4)}</div></div><div class="bank-card-icon"></div></div>
  <div class="status-line"><span class="${card.benefitEligible ? 'success' : 'danger'}">${card.benefitEligible ? '전월 실적 충족' : '전월 실적 미달'}</span><span style="color:var(--muted)">실적 총액까지 ${won(card.remainingAmount)}</span></div>
  <div class="progress"><span style="width:${progress}%"></span></div><div class="progress-caption">목표 ${won(card.targetAmount)}</div>`;
    }

    function renderQuick(merchant) {
        const link = document.getElementById('quickPay'), label = document.getElementById('quickMerchant');
        link.href = 'pay.html';
        label.textContent = '';
        label.classList.add('hidden');
        if (!merchant) return;
        label.textContent = merchant.merchantName;
        label.classList.remove('hidden');
        link.href = `pay.html?merchantId=${merchant.merchantId}&amount=${merchantDefaultAmount(merchant)}`;
    }

    function renderRecent(items) {
        const el = document.getElementById('recentPayments');
        if (!items.length) {
            el.innerHTML = '<div class="empty-state">최근 결제 내역이 없습니다.</div>';
            return;
        }
        el.innerHTML = items.map(p => `<div class="payment-row"><div class="payment-main"><span>${escapeHtml(p.merchantName)}</span><span>${won(p.finalAmount)}</span></div><div class="payment-sub"><span>${formatDate(p.paymentTime)} | ${escapeHtml(p.cardName)}</span><span class="payment-benefit">${benefitLabel(p)}</span></div></div>`).join('');
    }
</script>
</body>
</html>
