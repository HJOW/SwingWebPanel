# sourcecheck (AI용 문서)

## 목적

프로젝트 규칙(`AGENTS.md` "주의사항")을 기계적으로 검사하는 개발 보조 도구이다. 제품 코드가 아니며 제품 JAR/테스트는 이 도구에 의존하지 않는다.
계획 항목: P02-07(인코딩), P02-08(Javadoc/주석), P02-09(금지 API), P22-08(최종 검사).

## 검사 항목 (규칙 ID)

| 그룹(`--only`) | 규칙 ID | 내용 |
| --- | --- | --- |
| encoding | `utf8`, `bom`, `replacement-char`, `invisible-char`, `sh-crlf` | java/md/sh 는 엄격한 UTF-8(BOM 없음), U+FFFD 금지, 보이지 않는 문자(NBSP, 폭 없는 문자, U+2028/2029, BOM 등)가 java/sh 에 원문자로 있으면 위반, sh 는 LF 만 |
| encoding | `bat-utf8`, `bat-ms949`, `bat-crlf` | bat 는 MS949(UTF-8 한국어가 섞이면 위반), CRLF 줄 끝 |
| forbidden | `forbidden-api`, `forbidden-engine`, `forbidden-process`, `forbidden-native`, `forbidden-render` | `com.sun`/`sun`, JS 엔진(Rhino/Nashorn/Graal/javax.script/JavaFX), `Runtime.exec`/`ProcessBuilder`/`Desktop`, `native`/`System.load*`, Swing 내장 HTML 렌더러 |
| forbidden | `native-artifact` | `.dll .so .dylib .c .cpp .h .jar .class ...` 파일이 저장소에 있으면 위반(`target`, `build`, `bin`, `.git`은 제외) |
| forbidden | `java6-api`, `java6-syntax` | 람다, 메서드 참조, 다이아몬드, try-with-resources, 다중 catch, 이진/밑줄 리터럴, Java 7+ 클래스/메서드(`java.nio.file`, `Objects`, `Integer.compare` 등). 빠른 점검일 뿐이며 JDK 6 컴파일이 실제 증명이다 |
| forbidden | `default-charset` | `FileReader/FileWriter`, 인수 1개인 `InputStreamReader/OutputStreamWriter`, 인수 없는 `getBytes()` (플랫폼 기본 인코딩 의존) |
| javadoc | `javadoc`, `javadoc-lang` | 모든 클래스/메서드/필드/상수(비공개, enum 상수, package-info 포함, 익명 클래스 본문 제외)에 Javadoc 이 있고 영어(2단어 이상)와 한국어(2글자 이상)가 모두 있어야 함 |
| loops | `loop-comment` | 모든 `for/while/do` 반복문의 같은 줄/앞 2줄/본문 첫 줄 근처에 한글이 든 일반 주석(Javadoc 제외) |

예외: 줄에 `sourcecheck:allow` 가 있으면 그 줄의 금지 API/문자셋 규칙은 건너뛴다(정당한 사유를 주석으로 남길 것).
브라우저와 엔진의 `src/test/resources/` 아래 파일에는 네이티브 산출물 검사만 적용한다. 인코딩이 다른 시험 입력을 담을 수 있다.

## 빌드와 실행

- 빌드: `tools/sourcecheck/build.sh` 또는 `tools\sourcecheck\build.bat` (JDK 6, `JAVA6_HOME`, 못 찾으면 종료 코드 1)
- 실행: `tools/sourcecheck/run.sh [옵션] [경로...]` 또는 `tools\sourcecheck\run.bat ...` — 항상 **저장소 루트를 기준**으로 실행한다.
- 옵션: `--root DIR`, `--only encoding,forbidden,javadoc,loops`, `--max N`(출력 줄 수, 기본 200), `--selftest`
- 기본 경로: `browser engine examples desktopapp tools scripts docs AGENTS.md build.sh build.bat test.sh test.bat`
- 출력: `경로:줄: [규칙] 메시지` 와 마지막 요약 줄 `N violation(s) in M file(s) checked`
- 종료 코드: 0 위반 없음 / 1 위반 있음 / 2 사용법·입출력 오류. `--selftest` 는 불량 샘플이 종료 코드 1, 양호 샘플이 0 인지 검증해 0/1 반환.

## 구조

`SourceCheck`(진입점, 파일 순회), `JavaScanner`(주석/문자열 제거 토큰화), `EncodingChecks`, `ForbiddenChecks`, `DocChecks`(멤버 순회형 Javadoc/반복문 검사), `Violation`, `SelfTest`.
`DocChecks` 는 완전한 Java 파서가 아니라 멤버 머리부를 분류하고 중괄호로 본문을 건너뛰는 근사 검사기이다. 오탐이 있으면 규칙을 고치지 말고 코드 쪽을 맞추거나 도구를 개선한다.

## 제약

- Java 6 문법/API 만 사용. 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45` (sh, bat 모두).
- 주의(도구 환경): 이 개발 환경의 Write/Edit/Bash 입력은 `\uXXXX`(서로게이트 제외)를 실제 문자로 바꾸어 저장한다.
  그래서 특수 문자는 소스에 `(char) 0x2028` 처럼 적고, Java 문자열에 JS/정규식의 유니코드 이스케이프 문자열을 담을 때는 `"\\u0041"` 로 적는다.
  `invisible-char` 규칙이 이 문제를 잡는다.
