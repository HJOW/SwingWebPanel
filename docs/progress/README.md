# 병렬 작업 규칙 (docs/progress)

여러 작업자(사람/AI 세션/하위 에이전트)가 같은 작업 트리에서 동시에 일할 때의 규칙이다. 조정자는 메인 세션이다.

## 소유와 수정 범위

- 각 작업자는 **자기에게 배정된 경로만** 수정한다(배정 목록은 지시문에 있음).
- 다음 파일은 조정자만 수정한다: `AGENTS.md`(체크박스 포함), `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`,
  `docs/API_CONTRACT.md`, 루트 `build.*`/`test.*`, `scripts/`, `.gitignore`, `.classpath`. 필요한 변경은 작업자의 진행 기록에 "조정자 요청"으로 적는다.
- 다른 작업자의 패키지 코드를 고쳐야 하면 고치지 말고 요청으로 남긴다.

## 빌드와 시험

- 작업자는 `./build.sh`, `./test.sh` 대신(다른 작업자의 미완성 코드 때문에 깨질 수 있음)
  `scripts/pkgtest.sh <이름> "<src 하위 패키지들>" "<tests 하위 패키지들>" [TestRunner 인수]` 를 쓴다.
  출력은 `build/pkg-<이름>/` 이며 JDK 6 만 사용한다(`JAVA6_HOME`, 못 찾으면 오류).
- 전체 통합 확인(`./test.sh`)은 조정자가 한다. 조정자는 자기 실행 때 `BUILD_DIR=build/main` 을 쓴다.

## 코드 규칙 (AGENTS.md "주의사항" 요약)

- Java 6 문법/API 만(람다, 다이아몬드, try-with-resources, 다중 catch, 문자열 switch, `java.nio.file`, `StandardCharsets`, `Objects` 등 금지).
- `com.sun.*`/`sun.*`, 외부 라이브러리, 네이티브 코드, `Runtime.exec`/`ProcessBuilder` 금지.
- 모든 클래스/메서드/필드/상수(비공개와 enum 상수 포함, 익명 클래스 본문 제외)에 **영어와 한국어** Javadoc. 반복문과 중요한 분기문에 **한국어 주석**.
- 소스/문서는 UTF-8, bat 는 MS949(ASCII 만 쓰는 것을 권장).

## commit

- 한 줄 메시지: `YYYYMMDD-NNN <작업번호> 요약` (날짜는 `date +%Y%m%d`, NNN 은 그날 마지막 번호+1; `git log --format=%s` 로 확인).
  본문/추가 줄/Co-Authored-By 줄을 쓰지 않는다. push 하지 않는다.
- **자기 경로만** commit 한다: `git add -- <경로들>` 후 `git commit -q -m "..." -- <경로들>`. `index.lock` 충돌 시 몇 초 뒤 재시도한다.
- 작업 번호 하나를 끝냈을 때, 검증 가능한 중간 상태에 도달했을 때마다 commit 한다. 검증이 실패한 상태는 진행 기록의 미해결 사항에 적는다.

## 진행 기록 (`docs/progress/<영역>.md`)

작업 단위마다 **맨 위에** 항목을 추가한다: `현재 작업 번호`, `완료한 내용`, `변경 파일`, `검증 결과`(실제 실행한 명령/환경/통과·실패 수),
`미해결 사항`, `다음 작업 번호`, `조정자 요청`, 그리고 **체크박스 후보**(구현과 검증이 모두 끝난 계획 항목 번호). 
구현만 했거나 검증하지 못한 항목은 체크박스 후보에 넣지 않는다. 조정자가 이 후보를 AGENTS.md 에 반영한다.

## 정직성

- 검증하지 않은 것을 완료로 쓰지 않는다. 실패하는 사례, 건너뛴 사례(이유 포함), 부분 구현을 그대로 기록한다.
- 부분 구현을 "ES2026/HTML5/CSS3 전체 지원"으로 표시하지 않는다.
