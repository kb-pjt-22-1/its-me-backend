<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta
            name="viewport"
            content="width=device-width, initial-scale=1, maximum-scale=1"
    >

    <title>BenePay 결제 내역</title>

    <link rel="stylesheet" href="assets/app.css">

    <style>
        body {
            background: #ecebe7;
        }

        .app-shell {
            background: #f8f7f4;
        }

        /* 상단 헤더 */
        .history-header {
            position: sticky;
            top: 0;
            z-index: 30;
            height: 63px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: rgba(255, 255, 255, 0.97);
            border-bottom: 1px solid #e8e5df;
        }

        .history-header-title {
            font-size: 18px;
            font-weight: 900;
            letter-spacing: -0.4px;
        }

        .history-back-button {
            position: absolute;
            left: 15px;
            width: 38px;
            height: 38px;
            border: 0;
            background: transparent;
            border-radius: 50%;
            font-size: 31px;
            font-weight: 300;
            line-height: 1;
        }

        .history-grid-button {
            position: absolute;
            right: 16px;
            width: 33px;
            height: 33px;
            border: 1px solid #e5e1db;
            border-radius: 10px;
            background: #ffffff;
            color: #99938a;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.07);
            font-size: 17px;
        }

        .history-content {
            padding: 0 18px 100px;
        }

        /* 월 선택 */
        .month-selector {
            height: 64px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 28px;
        }

        .month-move-button {
            width: 36px;
            height: 36px;
            border: 0;
            background: transparent;
            color: #26231f;
            font-size: 28px;
            font-weight: 300;
        }

        .month-move-button:disabled {
            color: #ddd9d2;
            cursor: default;
        }

        .selected-month {
            min-width: 115px;
            text-align: center;
            font-size: 17px;
            font-weight: 900;
        }

        /* 월 결제 요약 */
        .monthly-summary {
            position: relative;
            overflow: hidden;
            min-height: 107px;
            display: grid;
            grid-template-columns: 1.35fr 1fr;
            margin-bottom: 26px;
            border-radius: 20px;
            background: #555148;
            color: #ffffff;
            box-shadow: 0 8px 22px rgba(68, 62, 52, 0.12);
        }

        .monthly-summary::after {
            content: "";
            position: absolute;
            width: 140px;
            height: 140px;
            right: -43px;
            top: -45px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.055);
        }

        .summary-column {
            position: relative;
            z-index: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 20px 21px;
        }

        .summary-column:last-child {
            align-items: flex-end;
            text-align: right;
        }

        .summary-label {
            margin-bottom: 10px;
            color: #c4beb1;
            font-size: 12px;
        }

        .summary-amount {
            font-size: 25px;
            font-weight: 900;
            letter-spacing: -0.6px;
        }

        .summary-benefit {
            color: #ffb800;
            font-size: 22px;
        }

        /* 날짜별 목록 */
        .payment-date-group {
            margin-bottom: 21px;
        }

        .payment-date-label {
            margin: 0 2px 8px;
            color: #8d867b;
            font-size: 12px;
            font-weight: 800;
        }

        .payment-group-card {
            overflow: hidden;
            border-radius: 18px;
            background: #ffffff;
            box-shadow: 0 5px 17px rgba(50, 45, 35, 0.055);
        }

        .payment-history-row {
            width: 100%;
            min-height: 70px;
            display: grid;
            grid-template-columns: 45px minmax(0, 1fr) auto 13px;
            gap: 10px;
            align-items: center;
            padding: 12px 15px;
            border: 0;
            border-bottom: 1px solid #ece9e3;
            background: #ffffff;
            text-align: left;
        }

        .payment-history-row:last-child {
            border-bottom: 0;
        }

        .payment-history-row:active {
            background: #faf9f7;
        }

        .merchant-icon {
            width: 43px;
            height: 43px;
            display: grid;
            place-items: center;
            border-radius: 12px;
            background: #f7f6f3;
            color: #5f5a51;
            font-size: 21px;
        }

        .payment-info {
            min-width: 0;
        }

        .payment-merchant-name {
            overflow: hidden;
            color: #161512;
            font-size: 14px;
            font-weight: 900;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .payment-card-info {
            overflow: hidden;
            margin-top: 6px;
            color: #948e84;
            font-size: 11px;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .payment-price-area {
            min-width: 91px;
            text-align: right;
        }

        .payment-final-amount {
            color: #171611;
            font-size: 13px;
            font-weight: 900;
            white-space: nowrap;
        }

        .payment-benefit-text {
            min-height: 15px;
            margin-top: 5px;
            color: #ffad00;
            font-size: 10px;
            font-weight: 800;
            white-space: nowrap;
        }

        .payment-failed-text {
            color: #df5549;
        }

        .payment-arrow {
            color: #aaa49b;
            font-size: 19px;
            font-weight: 300;
        }

        /* 로딩 및 빈 화면 */
        .history-message {
            padding: 70px 15px;
            color: #99938a;
            text-align: center;
            font-size: 13px;
        }

        .history-message-icon {
            margin-bottom: 12px;
            font-size: 34px;
        }
    </style>
</head>

<body>

<div class="app-shell">

    <header class="history-header">

        <button
                type="button"
                class="history-back-button"
                onclick="goBack()"
                aria-label="뒤로가기"
        >
            ‹
        </button>

        <div class="history-header-title">
            결제 내역
        </div>

        <button
                type="button"
                class="history-grid-button"
                aria-label="전체 메뉴"
        >
            ▦
        </button>

    </header>


    <main class="history-content">

        <section class="month-selector">

            <button
                    id="previousMonthButton"
                    type="button"
                    class="month-move-button"
                    aria-label="이전 달"
            >
                ‹
            </button>

            <strong
                    id="selectedMonthText"
                    class="selected-month"
            >
                -
            </strong>

            <button
                    id="nextMonthButton"
                    type="button"
                    class="month-move-button"
                    aria-label="다음 달"
            >
                ›
            </button>

        </section>


        <section class="monthly-summary">

            <div class="summary-column">
                <span
                        id="totalPaymentLabel"
                        class="summary-label"
                >
                    이번 달 총 결제
                </span>

                <strong
                        id="monthlyPaymentAmount"
                        class="summary-amount"
                >
                    0원
                </strong>
            </div>

            <div class="summary-column">
                <span class="summary-label">
                    받은 혜택
                </span>

                <strong
                        id="monthlyBenefitAmount"
                        class="summary-amount summary-benefit"
                >
                    0원
                </strong>
            </div>

        </section>


        <section id="historyList">

            <div class="history-message">
                결제 내역을 불러오는 중입니다.
            </div>

        </section>

    </main>


    <nav
            id="bottomNav"
            class="bottom-nav"
    ></nav>

</div>


<script src="assets/common.js"></script>

<script>
    const historyState = {
        payments: [],
        selectedDate: new Date(
            new Date().getFullYear(),
            new Date().getMonth(),
            1
        )
    };


    document.addEventListener(
        'DOMContentLoaded',
        initializeHistory
    );


    async function initializeHistory() {
        pageReady('home');

        document.getElementById(
            'previousMonthButton'
        ).addEventListener(
            'click',
            () => moveMonth(-1)
        );

        document.getElementById(
            'nextMonthButton'
        ).addEventListener(
            'click',
            () => moveMonth(1)
        );

        document.getElementById(
            'historyList'
        ).addEventListener(
            'click',
            handlePaymentClick
        );

        updateMonthSelector();
        await loadPaymentHistory();
    }


    async function loadPaymentHistory() {
        const historyList =
            document.getElementById('historyList');

        historyList.innerHTML = `
        <div class="history-message">
            결제 내역을 불러오는 중입니다.
        </div>
    `;

        try {
            const yearMonth =
                createYearMonthValue();

            const response =
                await api(
                    `/api/v1/payments?yearMonth=${
                        encodeURIComponent(yearMonth)
                    }`
                );

            /*
             * 서버 응답 구조:
             * {
             *   yearMonth,
             *   totalPaymentAmount,
             *   totalBenefitAmount,
             *   payments: []
             * }
             */
            historyState.payments =
                response &&
                Array.isArray(response.payments)
                    ? response.payments
                    : [];

            renderHistory();

        } catch (error) {
            console.error(
                '결제 내역 조회 실패:',
                error
            );

            historyList.innerHTML = `
            <div class="history-message">
                <div class="history-message-icon">!</div>
                결제 내역을 불러오지 못했습니다.<br>
                ${escapeHtml(error.message)}
            </div>
        `;
        }
    }


    function renderHistory() {
        updateMonthSelector();

        const selectedPayments =
            historyState.payments
                .filter(payment => {
                    const paymentDate =
                        parsePaymentDate(
                            payment.paymentTime
                        );

                    return (
                        paymentDate &&
                        paymentDate.getFullYear() ===
                        historyState.selectedDate.getFullYear() &&
                        paymentDate.getMonth() ===
                        historyState.selectedDate.getMonth()
                    );
                })
                .sort((a, b) => {
                    return (
                        parsePaymentDate(b.paymentTime) -
                        parsePaymentDate(a.paymentTime)
                    );
                });

        renderSummary(selectedPayments);
        renderPaymentGroups(selectedPayments);
    }


    function renderSummary(payments) {
        const approvedPayments =
            payments.filter(payment =>
                payment.paymentStatus ===
                'APPROVED'
            );

        const totalPayment =
            approvedPayments.reduce(
                (sum, payment) =>
                    sum +
                    Number(
                        payment.finalAmount || 0
                    ),
                0
            );

        const totalBenefit =
            approvedPayments.reduce(
                (sum, payment) =>
                    sum +
                    Number(
                        payment.discountAmount || 0
                    ) +
                    Number(
                        payment.rewardAmount || 0
                    ),
                0
            );

        const month =
            historyState.selectedDate.getMonth() +
            1;

        document.getElementById(
            'totalPaymentLabel'
        ).textContent =
            `${month}월 총 결제`;

        document.getElementById(
            'monthlyPaymentAmount'
        ).textContent =
            won(totalPayment);

        document.getElementById(
            'monthlyBenefitAmount'
        ).textContent =
            won(totalBenefit);
    }


    function renderPaymentGroups(payments) {
        const historyList =
            document.getElementById(
                'historyList'
            );

        if (payments.length === 0) {
            historyList.innerHTML = `
                <div class="history-message">
                    <div class="history-message-icon">🧾</div>
                    선택한 달의 결제 내역이 없습니다.
                </div>
            `;

            return;
        }

        const groupedPayments = {};

        payments.forEach(payment => {
            const date =
                parsePaymentDate(
                    payment.paymentTime
                );

            const key =
                createDateKey(date);

            if (!groupedPayments[key]) {
                groupedPayments[key] = [];
            }

            groupedPayments[key].push(payment);
        });

        historyList.innerHTML =
            Object.entries(groupedPayments)
                .map(([dateKey, group]) => {
                    return `
                        <section class="payment-date-group">

                            <div class="payment-date-label">
                                ${createDateLabel(dateKey)}
                            </div>

                            <div class="payment-group-card">
                                ${group
                        .map(
                            createPaymentRow
                        )
                        .join('')}
                            </div>

                        </section>
                    `;
                })
                .join('');
    }


    function createPaymentRow(payment) {
        const merchant =
            getMerchantInformation(payment);

        const approved =
            payment.paymentStatus ===
            'APPROVED';

        const canceled =
            payment.paymentStatus ===
            'CANCELED';

        const time =
            createTimeText(
                payment.paymentTime
            );

        const cardText =
            payment.cardName ||
            payment.cardCompanyName ||
            '등록 카드';

        const amount =
            Number(
                payment.finalAmount || 0
            );

        let benefitText = '';

        if (!approved) {
            benefitText =
                canceled
                    ? '결제 취소'
                    : '결제 실패';
        } else {
            benefitText =
                createBenefitText(payment);
        }

        return `
            <button
                    type="button"
                    class="payment-history-row"
                    data-payment-id="${payment.paymentId}"
            >

                <span class="merchant-icon">
                    ${merchant.icon}
                </span>

                <span class="payment-info">

                    <span class="payment-merchant-name">
                        ${escapeHtml(
            payment.merchantName ||
            '가맹점'
        )}
                    </span>

                    <span class="payment-card-info">
                        ${escapeHtml(time)}
                        ·
                        ${escapeHtml(cardText)}
                    </span>

                </span>

                <span class="payment-price-area">

                    <span class="payment-final-amount">
                        ${approved ? '-' : ''}
                        ${won(amount)}
                    </span>

                    <span class="
                        payment-benefit-text
                        ${approved
            ? ''
            : 'payment-failed-text'}
                    ">
                        ${escapeHtml(benefitText)}
                    </span>

                </span>

                <span class="payment-arrow">
                    ›
                </span>

            </button>
        `;
    }


    function createBenefitText(payment) {
        const discount =
            Number(
                payment.discountAmount || 0
            );

        const reward =
            Number(
                payment.rewardAmount || 0
            );

        if (discount > 0 && reward > 0) {
            return (
                `할인 ${won(discount)} · ` +
                `적립 ${won(reward)}`
            );
        }

        if (discount > 0) {
            return `할인 ${won(discount)}`;
        }

        if (reward > 0) {
            return `적립 ${won(reward)}`;
        }

        return '';
    }


    function handlePaymentClick(event) {
        const row =
            event.target.closest(
                '[data-payment-id]'
            );

        if (!row) {
            return;
        }

        const paymentId =
            String(
                row.dataset.paymentId
            );

        const payment =
            historyState.payments.find(
                item =>
                    String(item.paymentId) ===
                    paymentId
            );

        if (!payment) {
            showToast(
                '결제 정보를 찾을 수 없습니다.',
                'error'
            );

            return;
        }

        sessionStorage.setItem(
            'selectedPayment',
            JSON.stringify(payment)
        );

        location.href =
            `payment-detail.html?id=${
                encodeURIComponent(paymentId)
            }`;
    }


    async function moveMonth(amount) {
        const nextDate =
            new Date(
                historyState.selectedDate.getFullYear(),
                historyState.selectedDate.getMonth() + amount,
                1
            );

        const currentMonth =
            new Date(
                new Date().getFullYear(),
                new Date().getMonth(),
                1
            );

        /*
         * 현재 월보다 미래로 이동하지 못하게 처리
         */
        if (nextDate > currentMonth) {
            return;
        }

        historyState.selectedDate =
            nextDate;

        updateMonthSelector();

        /*
         * 월이 바뀔 때 해당 월의 데이터를 서버에서 다시 조회
         */
        await loadPaymentHistory();
    }


    function createYearMonthValue() {
        const year =
            historyState.selectedDate.getFullYear();

        const month =
            String(
                historyState.selectedDate.getMonth() + 1
            ).padStart(2, '0');

        return `${year}-${month}`;
    }


    function updateMonthSelector() {
        const year =
            historyState.selectedDate.getFullYear();

        const month =
            historyState.selectedDate.getMonth() +
            1;

        document.getElementById(
            'selectedMonthText'
        ).textContent =
            `${year}년 ${month}월`;

        const currentMonth =
            new Date(
                new Date().getFullYear(),
                new Date().getMonth(),
                1
            );

        document.getElementById(
            'nextMonthButton'
        ).disabled =
            historyState.selectedDate >=
            currentMonth;
    }


    function parsePaymentDate(value) {
        if (!value) {
            return null;
        }

        const date =
            new Date(value);

        return Number.isNaN(
            date.getTime()
        )
            ? null
            : date;
    }


    function createDateKey(date) {
        const year =
            date.getFullYear();

        const month =
            String(
                date.getMonth() + 1
            ).padStart(2, '0');

        const day =
            String(
                date.getDate()
            ).padStart(2, '0');

        return `${year}-${month}-${day}`;
    }


    function createDateLabel(dateKey) {
        const [year, month, day] =
            dateKey
                .split('-')
                .map(Number);

        const date =
            new Date(
                year,
                month - 1,
                day
            );

        const weekdayNames = [
            '일',
            '월',
            '화',
            '수',
            '목',
            '금',
            '토'
        ];

        return (
            `${month}월 ${day}일 ` +
            `(${weekdayNames[date.getDay()]})`
        );
    }


    function createTimeText(value) {
        const date =
            parsePaymentDate(value);

        if (!date) {
            return '-';
        }

        const hour =
            String(
                date.getHours()
            ).padStart(2, '0');

        const minute =
            String(
                date.getMinutes()
            ).padStart(2, '0');

        return `${hour}:${minute}`;
    }


    function getMerchantInformation(payment) {
        const name =
            String(
                payment.merchantName || ''
            ).toLowerCase();

        if (
            name.includes('커피') ||
            name.includes('카페') ||
            name.includes('스타벅스')
        ) {
            return {
                icon: '☕',
                category: '카페'
            };
        }

        if (
            name.includes('gs25') ||
            name.includes('cu') ||
            name.includes('세븐') ||
            name.includes('이마트')
        ) {
            return {
                icon: '▣',
                category: '편의점'
            };
        }

        if (
            name.includes('식탁') ||
            name.includes('식당') ||
            name.includes('스토리아') ||
            name.includes('레스토랑')
        ) {
            return {
                icon: '♜',
                category: '음식점'
            };
        }

        if (
            name.includes('지하철') ||
            name.includes('버스') ||
            name.includes('택시')
        ) {
            return {
                icon: '▤',
                category: '교통'
            };
        }

        if (
            name.includes('병원') ||
            name.includes('약국')
        ) {
            return {
                icon: '✚',
                category: '병원/약국'
            };
        }

        if (
            name.includes('cgv') ||
            name.includes('영화')
        ) {
            return {
                icon: '▣',
                category: '문화'
            };
        }

        return {
            icon: '▢',
            category: '일반'
        };
    }


    function goBack() {
        if (history.length > 1) {
            history.back();
            return;
        }

        location.href =
            'index.html';
    }
</script>

</body>
</html>