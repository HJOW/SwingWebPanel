# webfetch (AI용 문서)

## 목적

웹 페이지/규격 문서/Chromium 소스 원문을 조회하는 개발 보조 도구이다. 제품 기능이 아니며
제품 코드(`src`)와 제품 JAR은 이 도구의 클래스에 의존하지 않는다.
공통 작업 규칙: 도구로 얻은 규격 발췌에는 원본 URL, 판본/리비전, 조회 날짜를 함께 기록한다.
이 도구는 `# source-url`, `# final-url`, `# retrieved`, `# jre` 등을 표준 오류(stderr)에 출력하므로
그 값을 그대로 문서에 옮긴다.

## 빌드와 실행

- 소스: `tools/webfetch/src/webfetch/WebFetch.java` (Java 6 문법, 외부 라이브러리 없음)
- 빌드: `tools/webfetch/build.sh` 또는 `tools\webfetch\build.bat`
  (JDK 6 필요. `JAVA6_HOME` 으로 경로 변경, 기본 `C:\Program Files\Java\jdk1.6.0_45`. 못 찾으면 종료 코드 1)
- 실행: `tools/webfetch/run.sh [옵션] URL` 또는 `tools\webfetch\run.bat [옵션] URL`
  - 실행 JRE 는 `WEBFETCH_JAVA_HOME` > `JAVA6_HOME` > 기본 JDK 6 순으로 선택한다.
  - **HTTPS 는 반드시 `WEBFETCH_JAVA_HOME` 에 새 JRE 를 지정한다.** (아래 "확인된 사실")

## 옵션

| 옵션 | 설명 |
| --- | --- |
| `--out FILE` | 본문을 파일로 저장(없으면 stdout 으로 원시 바이트 출력) |
| `--text` | HTML 태그/script/style 제거 후 UTF-8 평문 출력 |
| `--base64` | 본문을 Base64 로 해독 (Gitiles `?format=TEXT` 용) |
| `--quiet` | 출처 메타데이터(stderr) 출력 생략 |
| `--timeout MS` | 연결/읽기 제한 시간 (기본 30000) |
| `--max-bytes N` | 최대 본문 크기 (기본 32MB, 초과 시 네트워크 실패 처리) |
| `--user-agent UA` | User-Agent (기본 `swingwebpanel-webfetch/1.0`) |
| `--header "Name: value"` | 요청 헤더 추가 (여러 번 지정 가능) |

## 종료 코드

0 성공 / 1 사용법 오류 / 2 네트워크·TLS 실패 / 3 HTTP 오류 상태(400 이상)

## 입력/출력 형식

- 입력: 명령행 옵션과 URL 하나(http 또는 https).
- 출력: stdout = 본문, stderr = `# key: value` 형식의 출처 메타데이터와 오류 메시지.
- 리디렉션은 최대 10회 직접 따라간다(http↔https 전환 허용).

## 제약

- Java 6 문법/API 만 사용. `java.util.Base64` 같은 Java 7+ API 는 쓰지 않는다(Base64 직접 구현).
- 인증서 검증을 끄는 옵션은 의도적으로 제공하지 않는다.

## 확인된 사실 (조회 날짜 2026-11-16)

- JRE `1.6.0_45`(JDK 6u45)에서 `https://www.w3.org/TR/2014/REC-html5-20141028/` 조회:
  `javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure` (종료 코드 2).
  `http://example.com/` 같은 평문 HTTP 는 정상(종료 코드 0).
- JRE `Zulu25.28+85-CA (openjdk 25)`에서 같은 HTTPS URL 조회 성공(상태 200).
- 결론: 도구 클래스는 JDK 6 으로 컴파일한 class 버전 50 이며, HTTPS 조회 시에는 더 새 JRE 로 실행한다.
  이 사실은 제품 P04/P22-07 의 "JRE 6 TLS 지원 범위" 기록과 연결된다.
- 실제로 실행해 본 JRE: 1.6.0_45(HTTP 만 성공), openjdk 25 Zulu25.28+85-CA(HTTP/HTTPS 성공).
- 조회 성공 확인(2026-11-16, 새 JRE): w3.org 의 HTML5 REC / CSS 스냅샷 / HTML 4.01 / XHTML 1.0 / CSS2 / SVG / PNG,
  rfc-editor.org 의 RFC, whatwg.org 의 DOM/HTML/XHR/Fetch/Encoding, spec.commonmark.org, raw.githubusercontent.com(Chromium 미러).
- 알려진 실패: `https://262.ecma-international.org/6.0/` 은 이 도구의 기본 User-Agent 로 HTTP 403 을 반환했다
  (2026-11-16). 접근 제한을 우회하기 위한 User-Agent 위장은 하지 않는다. 필요하면 사용자가 허용 경로를 지정한다.
- 주의: W3C 의 `2dcontext`, `webstorage` 주소는 WHATWG HTML Living Standard 로 리디렉션된다.

## 사용 예

```
set WEBFETCH_JAVA_HOME=C:\Program Files\Java\zuu25
tools\webfetch\run.bat --text https://www.w3.org/TR/2014/REC-html5-20141028/
tools\webfetch\run.bat --base64 --out build\x.txt "https://chromium.googlesource.com/chromium/src/+/refs/heads/main/README.md?format=TEXT"
```
