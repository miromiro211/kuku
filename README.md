# KUKU · 건국대 공강메이트

**팀명: 컴파일 에러**  
**프로젝트명: KUKU**

KUKU는 건국대학교 GLOCAL캠퍼스 학생의 **시간표, 공강 시간, 학교 소식**을 관리해 주는 캐릭터 중심 Android AI 에이전트입니다.

학생의 시간표를 기반으로 현재 수업과 다음 수업을 확인하고, 공강 시간에 할 수 있는 활동을 AI가 제안합니다. 또한 공모전, 장학금, 학사 일정과 같은 학교 정보를 함께 확인하여 필요한 준비 사항과 체크리스트를 제공합니다.

## 주요 기능

- 이메일 회원가입 및 로그인
- 서버 과목 검색 및 개인 시간표 등록
- 현재 수업 / 다음 수업 / 공강 시간 확인
- AI 기반 공강 활동 및 일정 추천
- 공모전·장학금·학사 일정 확인
- 공모전 준비안, 신청 서류 체크리스트, 과제 개요 생성
- 개인 할 일 및 계획 저장
- 집중 타이머 기능
- 앱 재실행 시 로그인 및 시간표 복원

## 주요 사용 기술

### Android
- Kotlin
- Jetpack Compose
- Android Studio
- Android Keystore
- AES-GCM

### Backend / Database
- Supabase
- Supabase Auth
- PostgreSQL
- Supabase Data API
- Supabase Edge Functions

### AI / Agent
- OpenAI API
- `free-time-agent`
  - 시간표, 이동시간, 식사, 수업 제약을 고려한 공강 계획 생성
- `campus-companion`
  - 공강 계획과 학교 소식, 선택 과목, 사용자 할 일을 종합해 맞춤 제안 및 준비안 생성

### 주요 데이터
- `course_catalog`
- `course_sessions`
- `user_courses`
- `contests`
- `scholarships`
- `academic_events`

## 실행 방법

### 1. 프로젝트 열기

Android Studio에서 `settings.gradle.kts`가 있는 KUKU 프로젝트 폴더를 엽니다.

### 2. 개발 환경 확인

다음 환경이 필요합니다.

- JDK 17
- Android SDK 35
- Android API 26 이상 기기 또는 에뮬레이터

### 3. Supabase 설정

`local.properties`의 기존 `sdk.dir`은 유지하고 다음 값을 추가합니다.

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR_PUBLIC_KEY
```

### 4. 실행

Gradle Sync를 진행한 뒤 Android Studio에서 앱을 실행합니다.

회원가입을 하는 경우 이메일로 전송된 Supabase 인증 메일을 승인한 뒤 로그인합니다.

## 프로젝트 정보

- **프로젝트명:** KUKU
- **팀명:** 컴파일 에러
- **앱 이름:** 쿠루 · 공강메이트
- **Package:** `com.gonggangmate.fresh`
- **Version:** `2.0-kuru`
- **Supabase Project:** `gbveuxpgwooarmbzlhlz`

## 보안 및 데이터 처리

로그인 토큰은 Android Keystore와 AES-GCM을 이용해 암호화하여 저장하며 비밀번호는 기기에 저장하지 않습니다.

OpenAI API의 비밀 키는 Android 앱에 포함하지 않고 Supabase 서버의 `OPENAI_API_KEY`, `OPENAI_MODEL` 환경 변수를 사용합니다.

시간표는 로그인한 사용자의 JWT를 이용해 `user_courses`에 저장되며, 사용자별 계획과 대화 데이터도 계정별로 분리하여 관리합니다.

AI는 공모전 준비안, 체크리스트, 과제 개요 등의 **초안 작성을 지원**하지만 실제 공모전·장학금 신청이나 과제 제출을 대신 수행하지 않습니다.
