# 공강메이트 · 모아의 방

Kotlin + Jetpack Compose로 만든 새로운 Android 네이티브 데모 프로젝트입니다.
토킹톰 참고 이미지의 ‘중앙 캐릭터와 주변 행동 버튼’ 구성을 바탕으로,
직접 그린 보라색 고양이 모아와 부드러운 캠퍼스 라운지를 구현했습니다.
참고 이미지나 토킹톰 캐릭터 파일은 포함하지 않았습니다.

## 바로 열기

1. ZIP을 압축 해제합니다.
2. Android Studio의 **Open**에서 `settings.gradle.kts`가 있는 `GonggangMate` 폴더를 선택합니다. `app` 폴더만 열지 마세요.
3. Gradle 동기화를 기다립니다. 처음에는 인터넷 연결이 필요합니다.
4. SDK 설치 안내가 나오면 Android SDK Platform 35와 Build Tools 35.0.0을 설치합니다.
5. **Settings → Build, Execution, Deployment → Build Tools → Gradle**에서 Gradle JDK를 **17**로 선택합니다. Android Studio가 제공하는 JDK 17을 사용해도 됩니다.
6. 상단 실행 구성을 **app**, 기기를 API 26 이상의 에뮬레이터나 휴대폰으로 선택하고 ▶ Run을 누릅니다.

기존 레포나 브랜치를 바꿀 필요가 없습니다. `com.gonggangmate.fresh`라는 별도 앱 ID로 설치됩니다.
로그인, Supabase 키, API 키, localhost 설정이 필요하지 않습니다.
`local.properties`는 Android Studio가 현재 컴퓨터에 맞게 생성합니다.

명령어로 빌드하려면 Android SDK 설정과 JDK 17이 있는 환경에서:

```powershell
# Windows PowerShell · 프로젝트 폴더에서
.\gradlew.bat assembleDebug
```

```bash
# macOS / Linux · 프로젝트 폴더에서
chmod +x gradlew
./gradlew assembleDebug
```

빌드 후 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 2분 시연 순서

1. 홈에서 모아를 눌러 인사합니다. 눈깜빡임, 숨쉬기, 꼬리 움직임은 계속 재생됩니다.
2. 팔레트 버튼으로 연두색 스카프를 바꿉니다.
3. 상단 공강 설정에서 **60분 / 전체**를 선택합니다.
4. 추천 탭에서 ‘수업 전, 커피 한 잔’을 열고 **데모로 참여하기**를 누릅니다.
5. 약속 탭에서 저장된 모임을 확인하고 모임 채팅을 엽니다.
6. 메시지를 보내 자동 안내 응답을 확인합니다.
7. 모아와 대화 화면에서 **‘30분 산책할래’**라고 보냅니다. 시간과 관심사가 바뀌고 추천이 갱신됩니다.
8. 앱을 종료하고 다시 실행합니다. 설정, 참여한 데모 약속, 대화, 스카프 상태가 유지됩니다.
9. 약속 상세에서 취소하거나 내 정보에서 데모 데이터를 초기화할 수 있습니다.

## 구현 범위

- 캐릭터가 중심인 홈과 애니메이션 Canvas 일러스트
- 공강 시간·관심사 설정, 추천 이유와 소요 시간 표시
- 개인 프로필 대신 함께 할 활동을 보여주는 모임 카드
- 모임 상세, 데모 참여와 취소, 내 약속
- 규칙 기반 모아 대화: 숫자 시간과 활동 키워드로 추천 조건 갱신
- 모임별 로컬 채팅, 예시 자동 응답
- SharedPreferences에 JSON으로 상태 저장, 최근 100개 메시지 유지
- 하단 5개 탭, 시스템 뒤로 가기, 채팅 키보드 여백 처리
- Android Studio 미리보기: `ui/MateApp.kt`의 `HomePreview`

모아의 추천 조건은 **활동 시간 + 왕복 도보 시간 + 수업 전 여유 5분 ≤ 공강 시간**입니다.
추천 목록은 실제 일정 데이터가 아닌 고정된 예시 모임이며 동시에 여러 모임을 저장할 수 있습니다.
시간·장소·인원은 시연용 데이터입니다. 저장된 약속에는 실제 예약 시각이나 만료 기능이 없습니다.

**실제 AI 모델, 로그인, Supabase, 실제 사용자 매칭, 실시간 메시지 전송, 푸시 알림은 포함되지 않습니다.**
모아의 답변은 규칙으로 생성되며 모임 채팅은 자동 안내가 응답합니다.
실서비스로 연결하려면 인증·예약·채팅 서버와 AI 호출 계층을 별도로 구현해야 합니다.

## 소스 위치

| 파일 | 역할 |
|---|---|
| `MainActivity.kt` | 앱 진입점 |
| `Models.kt` | 예시 모임, 데이터 모델, 오프라인 추천 정책 |
| `MateViewModel.kt` | 상태 관리, 참여·취소·채팅·기기 저장 |
| `ui/MateApp.kt` | 홈·추천·약속·채팅·설정 화면 |
| `ui/MoaScene.kt` | 원본 고양이와 방 일러스트, 캐릭터 애니메이션 |
| `ui/Theme.kt` | 라일락·민트 색상 테마 |

## 빌드 구성 및 확인 상태

AGP 8.9.2 / Gradle 8.11.1 / Kotlin 2.1.20 / Compose BOM 2025.04.01 / JDK 17.
공식 Gradle Wrapper JAR과 Unix·Windows 실행 스크립트가 포함되어 있습니다.

제작 환경에는 Android SDK가 없으며 Gradle 다운로드가 DNS 제한으로 실패했습니다.
따라서 APK 빌드, 에뮬레이터 실행, 화면 렌더링 검증은 완료하지 못했습니다.
소스 구문 및 XML·압축파일 구성 점검 결과는 `VERIFICATION.md`를 참고하세요.

공식 구성 참고:
- https://developer.android.com/build/releases/agp-8-9-0-release-notes
- https://kotlinlang.org/docs/compose-compiler-migration-guide.html

Gradle Wrapper 바이너리는 Gradle v8.11.1 저장소에서 가져왔으며 Apache License 2.0이 적용됩니다.
- https://github.com/gradle/gradle/blob/v8.11.1/gradle/wrapper/gradle-wrapper.jar
- https://www.apache.org/licenses/LICENSE-2.0
