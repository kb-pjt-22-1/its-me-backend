# BenePay DB 적용 순서

## 새로 설치

`benepay-mockup-integrated.sql`을 실행합니다.

## 기존 DB 업그레이드

기존 결제 테이블이 있는 상태에서 `full-feature-migration.sql`을 실행합니다.

이 마이그레이션은 카드 상태, 결제 취소 사유, 일반 간편결제 토큰, 카드 실적 전달 Outbox와 중복 처리 방지 테이블을 추가합니다.
