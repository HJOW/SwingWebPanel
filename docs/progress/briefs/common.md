## 공통 규칙 (모든 작업자)

프로젝트: SwingWebPanel — 외부 라이브러리/네이티브 코드/Rhino 없이 순수 Java(6 문법/API)로 만든 Swing 웹 브라우저 컴포넌트.
- 작업 시작 전에 읽을 것: `AGENTS.md`(저장소 루트; "주의사항"과 "공통 작업 규칙"), `docs/PROGRESS.md`("다음 세션 인수인계" 5~6절, 도구 사용 메모), 소유 영역의 `docs/progress/<영역>.md`.
- 코드 규칙: Java 6 (`javac -source 1.6 -target 1.6`; 람다/다이아몬드/try-with-resources/String switch/Java 7+ API 금지), UTF-8, 모든 클래스/메서드/필드/상수에 **영어+한국어 Javadoc**(해당 객체의 설명과 사용법), 반복문·중요 분기에 **한국어 주석**.
  `com.sun`/`sun` 패키지, 외부 JAR, 네이티브/JNI, `Runtime.exec` 금지. 점검: `sh tools/sourcecheck/run.sh <변경 경로>` → 0 위반이어야 한다.
- 검증은 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`, 환경 변수 `JAVA6_HOME`)로만 한다. 기본 `java`(JDK 25)로 시험 결과를 주장하지 않는다.
  시험 실행: `sh scripts/pkgtest.sh <이름> "<src 패키지>" "<tests 패키지>" --filter <접두사>` 또는 `scripts/snaptest.sh`(HEAD 스냅샷에 소유 경로만 덮어써서 다른 작업자의 미완성 코드와 격리). 오래 걸리는 것은 `run_in_background`.
  Bash 도구에서 중첩 heredoc/`python -` 은 멈출 수 있으니 긴 스크립트는 Write 도구로 파일을 만든 뒤 실행한다. 한국어가 든 perl 스크립트는 `use utf8;`.
- 문서: `docs/progress/<영역>.md` 에만 기록한다(변경 파일, 검증 명령과 실제 결과, 미해결 사항, 다음 작업, 조정자 요청, 체크박스 후보). `AGENTS.md`, `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `scripts/**` 는 **조정자만** 고친다(필요하면 "조정자 요청"에 적는다).
  작업 중인 영역 밖의 파일은 가산적으로 최소 수정만 한다.
- commit: 작은 단위로 자주 한다(사용 한도 중단에 대비). 자기 경로만 `git add -- <경로>` 후 `git commit -m "<메시지>" -- <경로>`. **메시지는 한 줄**, 형식 `YYYYMMDD-NNN 작업번호 한국어 요약`(날짜 20261117, 번호는 소유 대역에서 1씩, 추가 줄/트레일러 없음). push 하지 않는다.
- 정직: 컴파일/시험을 직접 돌린 결과만 "통과"로 적는다. 돌리지 않은 것은 "미확인"으로 적는다. 구현 한계는 숨기지 않고 문서에 적는다. 기능을 완료로 표시하는 것은 조정자의 몫이다(체크박스 후보만 제안).
- 동시에 도는 작업자는 최대 2명이고 4코어 PC 이므로 시험은 필요한 범위(`--filter`)만 돌린다.
