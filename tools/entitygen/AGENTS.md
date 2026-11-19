# entitygen (AI용 문서)

## 목적

규격 자료(JSON)에서 제품 소스의 표를 만들어 내는 개발 보조 도구이다. 제품 기능이 아니며 제품 코드(`src`)와 제품 JAR은 이 도구의 클래스에 의존하지 않는다.
계획 항목: P07-04(이름 있는 문자 참조 표를 규격 자료에서 생성하고 도구를 `tools/`에 둔다), P07-01(인코딩 라벨 표).

## 입력과 출력

- 입력 1: `entities.json` — HTML 표준의 이름 있는 문자 참조 표 (`https://html.spec.whatwg.org/entities.json`).
  키는 `"&AElig"`, `"&AElig;"` 처럼 `&` 로 시작하고, 값에 `codepoints`(정수 배열)와 `characters` 가 있다. 이 도구는 `codepoints` 만 쓴다.
- 입력 2: `encodings.json` — Encoding 표준의 인코딩 이름/라벨 표 (`https://encoding.spec.whatwg.org/encodings.json`).
  그룹 배열 안에 `encodings` 배열이 있고 각 항목에 `name` 과 `labels` 가 있다.
- 각 입력 옆에 webfetch 도구가 쓴 `<입력>.meta` 파일이 있으면 `source-url`, `retrieved`, `last-modified`, `etag` 줄을 생성 소스의 Javadoc 에 기록한다.
  (webfetch 의 stderr 출력을 `entities.json.meta` 로 저장해 둔다. 이 저장소에서는 `build/specs/html/` 에 있다.)
- 출력(`출력디렉터리`):
  - `NamedCharacterData.java` — `org.duckdns.hjow.swingwebpanel.html` 패키지의 패키지 비공개 클래스. `CHUNKS` 문자열 배열. 항목은 공백으로 구분한
    `이름:코드포인트,코드포인트` (이름은 `&` 를 뺀 것, 세미콜론 유무 그대로 — 2231 항목).
  - `EncodingLabelData.java` — `CHUNKS` 문자열 배열. 항목은 `|` 로 구분한 `표준이름=라벨,라벨,...` (40 인코딩).
  - 한 상수의 길이는 6000 글자 이하로 나눈다(클래스 파일 상수 한계 65535 바이트). 제품 빌드(`scripts/pkgtest.sh`)는 자바가 아닌 자원을 복사하지 않으므로
    표를 자원이 아니라 자바 소스로 만든다.

## 빌드와 실행

- 소스: `tools/entitygen/src/entitygen/` (`EntityGen.java`, `MiniJson.java`; Java 6 문법, 외부 라이브러리 없음)
- 빌드: `tools/entitygen/build.sh` 또는 `tools\entitygen\build.bat` (JDK 6 필요. `JAVA6_HOME`, 기본 `C:\Program Files\Java\jdk1.6.0_45`. 못 찾으면 종료 코드 1)
- 실행: `tools/entitygen/run.sh entities.json encodings.json src/org/duckdns/hjow/swingwebpanel/html`
  또는 `tools\entitygen\run.bat ...`. 실행 JRE 는 `JAVA6_HOME` (없으면 기본 JDK 6 경로).
- 종료 코드: 0 성공 / 1 사용법 오류 / 2 출력 디렉터리 오류. 입력 파일이 잘못되면 예외 스택과 함께 0 이 아닌 코드로 끝난다.

## 제약

- 생성 파일은 직접 고치지 않는다. 규격 판본이 바뀌면 입력을 다시 받아(`tools/webfetch`) 이 도구를 다시 실행한다.
- 이 도구는 JSON 을 자체 `MiniJson` 으로 읽는다(외부 라이브러리 금지). `entities.json` 의 `characters` 필드의 `\uXXXX` 이스케이프는 해석하지만 쓰지는 않는다.
- 이 개발 환경의 Bash 입력은 `\\` 를 `\` 로 바꾸므로 소스를 고칠 때 Write/Edit 도구를 쓴다(sourcecheck 도구 문서의 주의 참조).
- 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45` (sh). bat 은 같은 방식이나 이 환경에서는 실행해 보지 못했다.

## 마지막으로 생성한 판본

- `entities.json`: source-url `https://html.spec.whatwg.org/entities.json`, retrieved 2026-11-16, last-modified Wed, 12 Nov 2025 00:20:03 GMT, etag `6913d2b3-239e9`, 2231 항목.
- `encodings.json`: source-url `https://encoding.spec.whatwg.org/encodings.json`, retrieved 2026-11-16, last-modified Mon, 19 Oct 2026 23:30:10 GMT, etag `6ad6a802-22d1`, 40 인코딩.
