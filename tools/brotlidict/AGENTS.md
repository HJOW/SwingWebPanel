# brotlidict (AI용 문서)

## 목적

RFC 7932(Brotli 압축 데이터 형식)의 텍스트에서 Brotli 디코더가 쓰는 표를 자바 소스 하나(`BrotliData.java`)로 만들어 내는 개발 보조 도구이다.
제품 기능이 아니며 제품 코드(`src`)와 제품 JAR은 이 도구의 클래스에 의존하지 않는다(만들어진 `BrotliData.java` 만 제품 소스이다).
계획 항목: P10-08-e(WOFF2 웹 글꼴: Brotli 디코더에 정적 사전 122,784 바이트가 필요하다).

## 입력과 출력

- 입력: `rfc7932.txt` — `https://www.rfc-editor.org/rfc/rfc7932.txt` 의 텍스트(`tools/webfetch` 로 받는다). 입력 옆에 webfetch 가 쓴 `rfc7932.txt.meta`
  (`# source-url`, `# retrieved`, `# etag` 줄)가 있으면 생성 소스의 Javadoc 에 출처로 기록한다.
- 출력(`출력디렉터리`): `BrotliData.java` — `org.duckdns.hjow.swingwebpanel.media.font` 패키지의 패키지 비공개 클래스. 다음을 Base64 문자열 상수로 담는다.
  - `DICTIONARY`: 정적 사전 122,784 바이트(부록 A 의 16진 덤프), 8192 글자 조각의 배열.
  - `NDBITS`: 사전의 길이별 비트 깊이(8 절) 25 개.
  - `TRANSFORMS`: 단어 변환 121 개(부록 B). 변환마다 접두사 바이트, 0, 기본 변환 번호(0 Identity, 1 FermentFirst, 2 FermentAll, 3~11 OmitFirst1~9, 12~20 OmitLast1~9), 접미사 바이트, 0. 648 바이트.
  - `CONTEXT_LUTS`: 문맥 조회표 Lut0, Lut1, Lut2(7.1 절) 각 256 바이트, 모두 768 바이트.
- **검증**: RFC 가 적은 길이와 CRC-32 와 도구가 읽은 표를 대조한다: 사전 122,784 바이트와 `0x5136cb04`, 변환 648 바이트와 `0x3d965f81`, 조회표 `0x8e91efb7`, `0xd01a32f4`, `0x0dd7a0d6`.
  하나라도 다르면 예외로 끝나(종료 코드 0 이 아님) 파일을 만들지 않는다. 변환 번호가 0 부터 차례대로가 아니거나 121 개가 아니어도 같다.

## 빌드와 실행

- 소스: `tools/brotlidict/src/brotlidict/BrotliDict.java` (Java 6 문법, 외부 라이브러리 없음)
- 빌드: `tools/brotlidict/build.sh` 또는 `tools\brotlidict\build.bat` (JDK 6 필요. `JAVA6_HOME`, 기본 `C:\Program Files\Java\jdk1.6.0_45`. 못 찾으면 종료 코드 1)
- 실행: `tools/brotlidict/run.sh build/rfc7932.txt browser/src/main/java/org/duckdns/hjow/swingwebpanel/media/font` 또는 `tools\brotlidict\run.bat ...`
- 종료 코드: 0 성공 / 1 사용법 오류 / 2 출력 디렉터리 오류 / 그 밖의 0 이 아닌 값은 입력 검증 실패(예외 스택).

## 제약

- 생성 파일은 직접 고치지 않는다. 규격 판본이 바뀌면 입력을 다시 받아(`tools/webfetch`) 이 도구를 다시 실행한다.
- 입력은 ISO-8859-1 로 읽는다(RFC 텍스트는 ASCII 이다). 표 추출은 RFC 의 쪽 머리말/꼬리말 줄을 건너뛰는 정규식에 의존하므로 판본이 바뀌면 CRC 검증이 먼저 알려 준다.
- 이 개발 환경의 Bash 입력은 `\\` 를 `\` 로 바꾸므로 소스를 고칠 때 Write/Edit 도구를 쓴다.
- 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45` (sh). bat 은 같은 방식이나 이 환경에서는 실행해 보지 못했다.

## 마지막으로 생성한 판본

- `rfc7932.txt`: source-url `https://www.rfc-editor.org/rfc/rfc7932.txt`, retrieved 2026-11-18, etag `W/"1d5372459b2a6c5336defa4ec6da5e72"`, 389,979 바이트.
  모든 CRC-32 가 맞았다(2026-11-18, 이 PC, JDK 6u45).
