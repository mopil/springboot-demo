---
name: commit-pr
description: 변경사항을 커밋하고 푸시한 뒤 PR을 연다. /commit-pr로 호출. 커밋 타입은 feat/fix/refactor/chore 4개만 사용. PR body에는 tech-spec 링크와 변경 요약을 포함한다.
---

# commit-pr — 커밋 · 푸시 · PR 생성

## 사전 확인

1. git 저장소가 아니면 멈추고 사용자에게 확인 (`git init` + 원격 주소 필요).
2. 현재 브랜치가 `main`/`master`면 **새 브랜치를 먼저 생성**한다: `<type>/<kebab-case-요약>` (예: `feat/order-api`).
3. `git status`로 변경 파일을 확인하고, 빌드 산출물·임시 파일은 스테이징하지 않는다.

## 커밋 규칙

- 커밋 메시지 타입은 **feat / fix / refactor / chore 4개만** 사용한다:
  - `feat`: 기능 추가 · 요구사항 구현
  - `fix`: 버그 수정
  - `refactor`: 동작 변경 없는 구조 개선
  - `chore`: 설정, 의존성, 문서, 빌드 등
- 형식:
  ```
  <type>: <한 줄 제목 (한국어, 50자 이내)>

  - 주요 변경 bullet
  - ...

  Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>
  ```
- 성격이 다른 변경(예: feat + chore)이 섞여 있으면 커밋을 분리한다.

## 푸시 & PR

1. `git push -u origin <branch>`
2. `gh pr create`로 PR 생성. **title은 커밋 타입 규칙과 동일** (`<type>: <제목>`).
3. **PR body 형식** (필수):
   ```markdown
   ## Tech Spec
   - <`.claude/tech-spec/`의 해당 문서 경로(저장소 링크)>   ← 없으면 사용자에게 확인 후 진행

   ## 변경 요약
   - <무엇을 왜 바꿨는지 bullet 요약>

   ## 검증
   - ktlintCheck / test / koverVerify 결과

   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   ```
4. 생성된 PR URL을 보고한다.

## 주의

- 코드리뷰(P1 = 0) + `ktlintFormat ktlintCheck test koverVerify` 통과 전에는 실행하지 않는다.
- force push 금지. 이미 열린 PR이 있으면 새 커밋만 푸시한다.
