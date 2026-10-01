# 쿠루 · 건국대 공강메이트

건국대학교 GLOCAL 학생의 시간표와 학교 소식을 챙기는 캐릭터 중심 Android 에이전트입니다. Muse와 Dots의 동반자 경험을 참고하되, 캐릭터와 화면은 직접 만든 디자인입니다.

## 앱 흐름

1. 회원가입 또는 로그인. Supabase 확인 메일 승인 후 로그인합니다.
2. 과목명, 과목코드, 학과, 교수명으로 서버 과목을 검색해 수강 시간표를 선택합니다.
3. 쿠루 메인화면에서 현재 수업·다음 수업·공강을 확인합니다. 에이전트가 자동으로 공강 계획과 예정 소식을 살펴봅니다.
4. 제안을 수락하면 내 계획에 저장하고 공모전 준비안·신청 서류 체크리스트·과제 개요 등을 받습니다.
5. 준비안을 복사하고, 다음 수업 이동 여유를 남긴 집중 타이머를 시작하거나 완료한 계획을 표시합니다.
6. 학교 소식 탭에서 공모전·장학금·학사 일정을 보고 원문을 열 수 있습니다.

## Supabase 연결

기존 프로젝트 `gbveuxpgwooarmbzlhlz`를 사용합니다.

- Auth: 이메일 가입, 로그인, 로그아웃, 세션 갱신
- Data API: `course_catalog`, `course_sessions`, `contests`, `scholarships`, `academic_events`
- 기존 `free-time-agent`: 시간표, 이동시간, 식사, 수업 제약을 계산하는 공강 계획
- 새 `campus-companion`: 기존 공강 계획을 사용하고 학교 소식·선택 과목·사용자 할 일을 함께 고려하여 제안과 준비안을 생성
- 기존 자동 수집 작업 유지: 공모전/장학금 6시간마다, 학사 일정 매일
- 앱이 화면에 있는 동안 소식은 15분마다 확인하며, 계획은 15분마다 또는 수업 경계가 바뀔 때 갱신합니다. 앱을 다시 열면 시간을 즉시 비교합니다.

새 함수 소스는 `supabase/functions/campus-companion/`에 있습니다. 기존 함수·테이블·수집 예약 작업은 변경하지 않습니다. 서버의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용하며 비밀 키는 앱에 넣지 않습니다. AI 호출에 실패하면 기본 시간표 계획과 기본 준비안을 명시하여 반환합니다.

## 저장과 범위

로그인 토큰은 Android Keystore AES-GCM으로 암호화해 저장합니다. 비밀번호는 저장하지 않습니다. 로그인, 시간표, 계정별 계획과 대화는 앱 재시작 후 복원됩니다. 시간표는 로그인 사용자 JWT로 `user_courses`에 저장하고 로그인 시 복원합니다. 저장 완료 전에는 추천을 시작하지 않습니다. 계획·대화는 사용자 ID로 분리한 기기 로컬 저장입니다. 집중 타이머는 앱 재시작 시 종료됩니다.

수업시간이 서버에 없는 과목은 자동 공강 계산에 사용할 수 없으므로 선택을 막고 안내합니다. 시간이 겹치는 과목은 분반을 확인해야 진행할 수 있습니다. 과제는 사용자가 추가한 할 일과 free-time-agent v17이 조회한 사용자 TLS 과제을 고려합니다. 에이전트가 실제 교수의 과제를 자동 수집하거나 제출하지는 않습니다.

AI는 준비 개요와 체크리스트 등 초안을 만들며 공모전/장학금 신청이나 과제 제출을 대신 수행하지 않습니다. 신청 자격과 마감 시각은 원문 확인이 필요합니다. 현재 데이터에 미래 마감의 소식이 없으면 빈 상태를 표시하며 임의의 소식을 만들지 않습니다.

## 실행

Android Studio에서 `settings.gradle.kts`가 있는 폴더를 엽니다. JDK 17, Android SDK 35, API 26 이상 기기가 필요합니다. Gradle 동기화 후 앱을 실행하세요.

다른 프로젝트로 연결할 때는 `local.properties`의 기존 `sdk.dir`을 유지하고 추가합니다:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR_PUBLIC_KEY
```

패키지: `com.gonggangmate.fresh`. 앱 이름: `쿠루 · 공강메이트`, 버전 `2.0-kuru`.

v17 연동: `user_courses` 조회·추가·삭제, 저장 후 재조회 검증, `data_sources` 표시를 지원합니다. campus-companion 소스는 저장된 과목 자동 조회도 지원하며 서버에 별도 배포해야 적용됩니다. 동료 검증용 항목은 `VERIFICATION.md`에 정리했습니다.

참고: [Muse](https://ai.meta.com/muse/), [Dots](https://chatgpt.com/features/dots/).
