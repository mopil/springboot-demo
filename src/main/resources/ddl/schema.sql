-- =========================================================
-- DDL 기록 파일
-- DB 테이블 생성/변경이 생기면 반드시 이 파일에 누적 기록한다.
-- 형식: 날짜 + 변경 내용 주석 후 DDL 추가 (기존 기록은 수정하지 않는다)
-- local/test는 ddl-auto=create-drop이 스키마를 만들지만, 기록은 여기가 기준이다.
-- =========================================================

-- 2026-07-24: sample 테이블 생성 (소프트딜리트: deleted_at)
CREATE TABLE sample (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    memo       VARCHAR(500) NULL,
    deleted_at DATETIME NULL
);

-- 2026-07-24: BaseEntity 공통 컬럼 추가 (created_at, updated_at — JPA Auditing)
ALTER TABLE sample ADD COLUMN created_at DATETIME NOT NULL;
ALTER TABLE sample ADD COLUMN updated_at DATETIME NOT NULL;

-- 2026-07-24: 중복 처리 가드 컬럼 추가 (BaseIdempotencyEntity — idempotency_key 유니크)
-- 기존 행이 있는 운영 DB 적용 순서: nullable 추가 → 백필 → 제약 부여 (NOT NULL 즉시 부여는 기존 행에서 실패)
ALTER TABLE sample ADD COLUMN idempotency_key VARCHAR(36) NULL;
UPDATE sample SET idempotency_key = UUID() WHERE idempotency_key IS NULL;
ALTER TABLE sample MODIFY COLUMN idempotency_key VARCHAR(36) NOT NULL;
ALTER TABLE sample ADD CONSTRAINT uk_sample_idempotency_key UNIQUE (idempotency_key);
