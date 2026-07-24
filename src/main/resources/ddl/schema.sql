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
