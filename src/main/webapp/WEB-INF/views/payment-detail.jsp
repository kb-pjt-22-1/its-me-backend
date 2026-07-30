<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">

    <meta
            name="viewport"
            content="width=device-width, initial-scale=1, maximum-scale=1"
    >

    <title>BenePay 결제 상세</title>

    <link rel="stylesheet" href="assets/app.css">

    <style>
        body {
            background: #ecebe7;
        }

        .detail-shell {
            padding-bottom: 0;
            background: #f8f7f4;
        }

        /* 헤더 */
        .detail-header {
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

        .detail-header-title {
            font-size: 18px;
            font-weight: 900;
            letter-spacing: -0.4px;
        }

        .detail-back-button {
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

        .detail-grid-button {
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

        .detail-content {
            padding: 21px 20px 50px;
        }

        .detail-card {
            overflow: hidden;
            border-radius: 20px;
            background: #ffffff;
            box-shadow: 0 6px 20px rgba(50, 45, 35, 0.07);
        }

        /* 가맹점 정보 */
        .detail-merchant-header {
            display: grid;
            grid-template-columns: 51px minmax(0, 1fr);
            gap: 15px;
            align-items: center;
            padding: 21px;
            border-bottom: 1px solid #eae7e1;
        }

        .detail-merchant-icon {
            width: 51px;
            height: 51px;
            display: grid;
            place-items: center;
            border-radius: 14px;
            background: #f7f6f3;
            color: #5e594f;
            font-size: 23px;
        }

        .detail-merchant-name {
            margin: 0;
            color: #171612;
            font-size: 18px;
            font-weight: 900;
            letter-spacing: -0.4px;
        }

        .detail-category {
            margin-top: 7px;
            color: #999187;
            font-size: 12px;
        }

        /* 상세 정보 행 */
        .detail-information {
            margin: 0;
        }

        .detail-row {
            min-height: 52px;
            display: grid;
            grid-template-columns: 95px minmax(0, 1fr);
            gap: 15px;
            align-items: center;
            padding: 13px 20px;
            border-bottom: 1px solid #eae7e1;
        }

        .detail-row:last-child {
            border-bottom: 0;
        }

        .detail-label {
            color: #999187;
            font-size: 13px;
        }

        .detail-value {
            min-width: 0;
            color: #171612;
            font-size: 13px;
            text-align: right;
            word-break: keep-all;
        }

        .detail-value.amount {
            font-weight: 900;
        }

        .detail-value.benefit {
            color: #ffad00;
            font-weight: 900;
        }

        .detail-value.success {
            color: #00a978;
            font-weight: 900;
        }

        .detail-value.failed {
            color: #df5549;
            font-weight: 900;
        }

        .detail-message {
            padding: 80px 15px;
            color: #99938a;
            text-align: center;
            font-size: 13px;
        }
    </style>
</head>

<body>

<div class="app-shell detail-shell">

    <header class="detail-header">

        <button
                type="button"
                class="detail-back-button"
                onclick="goBack()"
                aria-label="뒤로가기"
        >
            ‹
        </button>

        <div class="detail-header-title">
            결제 상세
        </div>

        <button
                type="button"
                class="detail-grid-button"
                aria-label="전체 메뉴"
        >
            ▦
        </button>

    </header>


    <main
            id="detailContent"
            class="detail-content"
    >

        <div class="detail-message">
            결제 상세 정보를 불러오는 중입니다.
        </div>

    </main>

</div>


<script src="assets/common.js"></script>

<script>
    document.addEventListener(
        'DOMContentLoaded',
        initializePaymentDetail
    );


    async function initializePaymentDetail() {
        pageReady('home');

        const paymentId =
            new URLSearchParams(
                location.search
            ).get('id');

        try {
            let payment =
                readPaymentFromStorage(
                    paymentId
                );

            /*
             * 상세 페이지 주소로 바로 들어온 경우
             * 전체 목록 API에서 해당 결제를 다시 찾는다.
             */
            if (!payment) {
                if (!paymentId) {
                    throw new Error(
                        '결제 ID가 없습니다.'
                    );
                }

                /*
                 * 결제 상세 API 직접 호출
                 */
                payment =
                    await api(
                        `/api/v1/payments/${
                            encodeURIComponent(paymentId)
                        }`
                    );
            }

            if (!payment) {
                throw new Error(
                    '결제 정보를 찾을 수 없습니다.'
                );
            }

            renderPaymentDetail(payment);

        } catch (error) {
            console.error(error);

            document.getElementById(
                'detailContent'
            ).innerHTML = `
                <div class="detail-message">
                    결제 상세 정보를 불러오지 못했습니다.<br>
                    ${escapeHtml(error.message)}
                </div>
            `;
        }
    }


    function readPaymentFromStorage(paymentId) {
        try {
            const saved =
                sessionStorage.getItem(
                    'selectedPayment'
                );

            if (!saved) {
                return null;
            }

            const payment =
                JSON.parse(saved);

            if (
                paymentId &&
                String(payment.paymentId) !==
                String(paymentId)
            ) {
                return null;
            }

            return payment;

        } catch (error) {
            return null;
        }
    }


    function renderPaymentDetail(payment) {
        const merchant =
            getMerchantInformation(payment);

        const status =
            createStatusInformation(
                payment.paymentStatus
            );

        const discountAmount =
            Number(
                payment.discountAmount || 0
            );

        const rewardAmount =
            Number(
                payment.rewardAmount || 0
            );

        const benefitRows = [];

        if (discountAmount > 0) {
            benefitRows.push(`
                <div class="detail-row">
                    <div class="detail-label">
                        할인 금액
                    </div>

                    <div class="detail-value benefit">
                        ${won(discountAmount)}
                    </div>
                </div>
            `);
        }

        if (rewardAmount > 0) {
            benefitRows.push(`
                <div class="detail-row">
                    <div class="detail-label">
                        적립 금액
                    </div>

                    <div class="detail-value benefit">
                        ${won(rewardAmount)}
                    </div>
                </div>
            `);
        }

        if (
            discountAmount === 0 &&
            rewardAmount === 0
        ) {
            benefitRows.push(`
                <div class="detail-row">
                    <div class="detail-label">
                        받은 혜택
                    </div>

                    <div class="detail-value">
                        없음
                    </div>
                </div>
            `);
        }

        const failureRow =
            payment.failureReason
                ? `
                    <div class="detail-row">
                        <div class="detail-label">
                            실패 사유
                        </div>

                        <div class="detail-value failed">
                            ${escapeHtml(
                    payment.failureReason
                )}
                        </div>
                    </div>
                `
                : '';

        const approvalCodeRow =
            payment.approvalCode
                ? `
                    <div class="detail-row">
                        <div class="detail-label">
                            승인 번호
                        </div>

                        <div class="detail-value">
                            ${escapeHtml(
                    payment.approvalCode
                )}
                        </div>
                    </div>
                `
                : '';

        document.getElementById(
            'detailContent'
        ).innerHTML = `
            <section class="detail-card">

                <header class="detail-merchant-header">

                    <div class="detail-merchant-icon">
                        ${merchant.icon}
                    </div>

                    <div>
                        <h1 class="detail-merchant-name">
                            ${escapeHtml(
            payment.merchantName ||
            '가맹점'
        )}
                        </h1>

                        <div class="detail-category">
                            ${escapeHtml(
            merchant.category
        )}
                        </div>
                    </div>

                </header>


                <div class="detail-information">

                    <div class="detail-row">
                        <div class="detail-label">
                            결제 일시
                        </div>

                        <div class="detail-value">
                            ${escapeHtml(
            createDateTimeText(
                payment.paymentTime
            )
        )}
                        </div>
                    </div>


                    <div class="detail-row">
                        <div class="detail-label">
                            매장명
                        </div>

                        <div class="detail-value">
                            ${escapeHtml(
            payment.merchantName ||
            '-'
        )}
                        </div>
                    </div>


                    <div class="detail-row">
                        <div class="detail-label">
                            결제 금액
                        </div>

                        <div class="detail-value amount">
                            ${
            payment.paymentStatus ===
            'APPROVED'
                ? '-'
                : ''
        }${won(
            payment.finalAmount
        )}
                        </div>
                    </div>


                    <div class="detail-row">
                        <div class="detail-label">
                            사용 카드
                        </div>

                        <div class="detail-value">
                            ${escapeHtml(
            createCardText(payment)
        )}
                        </div>
                    </div>


                    ${benefitRows.join('')}


                    <div class="detail-row">
                        <div class="detail-label">
                            승인 상태
                        </div>

                        <div class="
                            detail-value
                            ${status.className}
                        ">
                            ${status.text}
                        </div>
                    </div>


                    ${approvalCodeRow}

                    ${failureRow}

                </div>

            </section>
        `;
    }


    function createCardText(payment) {
        let text =
            payment.cardName ||
            payment.cardCompanyName ||
            '등록 카드';

        if (payment.cardLast4) {
            text +=
                ` · ${payment.cardLast4}`;
        }

        return text;
    }


    function createStatusInformation(status) {
        if (status === 'APPROVED') {
            return {
                text: '결제 완료',
                className: 'success'
            };
        }

        if (status === 'CANCELED') {
            return {
                text: '결제 취소',
                className: 'failed'
            };
        }

        return {
            text: '결제 실패',
            className: 'failed'
        };
    }


    function createDateTimeText(value) {
        if (!value) {
            return '-';
        }

        const date =
            new Date(value);

        if (
            Number.isNaN(
                date.getTime()
            )
        ) {
            return String(value)
                .replace('T', ' ');
        }

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

        const hour =
            String(
                date.getHours()
            ).padStart(2, '0');

        const minute =
            String(
                date.getMinutes()
            ).padStart(2, '0');

        return (
            `${year}.${month}.${day} ` +
            `${hour}:${minute}`
        );
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
        location.href =
            'history.html';
    }
</script>

</body>
</html>