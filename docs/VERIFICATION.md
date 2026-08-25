# 기능 점검 결과

이 문서는 저장소를 실제로 기동해 확인한 결과입니다. 수치는 자동 점검 스크립트의 출력이며,
화면은 같은 시점에 Chrome 으로 촬영했습니다(`docs/screenshots/`).

## 점검 환경

| 항목 | 값 |
|---|---|
| 점검일 | 2026-08-22 |
| OS | Windows 11 |
| JDK | 17 (Temurin 17.0.17+10) |
| Maven | 3.9.9 |
| Node.js | 22.14.0 / npm 10.9.2 |
| DB | 내장 HSQLDB (시드 자동 적재) |
| 브라우저 | Chrome (headless, 1440x900) |

## 결과 요약

**18 / 18 통과**

| 결과 | 구분 | 항목 | 상세 |
|---|---|---|---|
| PASS | API | OpenAPI 문서 | HTTP 200, 경로 73 개 |
| PASS | API | 다국어 — 화면 문구 영어 지원 | 251/254 키 (99%), 번들 ko 1885 / en 1812 |
| PASS | 보안 | 비로그인 관리자 API 차단 | resultCode 403 |
| PASS | 인증 | 관리자 로그인 | resultCode 200 |
| PASS | 인증 | HttpOnly 쿠키 발급 | HttpOnly=True |
| PASS | 인증 | 응답 본문에 토큰 없음 | 본문 토큰 미포함 |
| PASS | 인증 | 현재 사용자 조회 | name=관리자 roles=ROLE_ADMIN |
| PASS | API | 메인 구성 | HTTP 200 / resultCode 200 |
| PASS | API | 게시판 마스터 | HTTP 200 / resultCode 200 |
| PASS | API | FAQ | HTTP 200 / resultCode 200 |
| PASS | API | Q&A | HTTP 200 / resultCode 200 |
| PASS | API | 설문 | HTTP 200 / resultCode 200 |
| PASS | API | 약관 | HTTP 200 / resultCode 200 |
| PASS | API | 회원 관리 | HTTP 200 / resultCode 200 |
| PASS | API | 권한 | HTTP 200 / resultCode 200 |
| PASS | 회원 | 회원가입 | resultCode 200 |
| PASS | 회원 | 가입 직후 로그인 | resultCode 200 |
| PASS | 회원 | 사용자 구분 오선택 거부 | USR 구분 -> resultCode 300 |

## 재현 방법

README 의 "빠른 시작" 대로 기동한 뒤, 아래를 확인하면 같은 결과를 얻습니다.

```bash
mvn test                       # 단위·통합 테스트
curl http://localhost:18090/api/v3/api-docs   # OpenAPI 문서
curl http://localhost:18090/api/i18n/ko       # 메시지 번들
```

