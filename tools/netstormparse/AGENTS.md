# netstormparse (AI용 문서)

## 목적

NetstormReborn의 외부 소스와 webpack 번들을 SwingWebPanel 자체 JavaScript 파서로 읽는 P28-02-f 개발 도구이다.
제품 JAR나 시험은 이 도구에 의존하지 않으며 게임 자산을 이 저장소로 복사하지 않는다.

## 빌드와 실행

- JDK 6(`JAVA6_HOME`)과 먼저 빌드한 `engine/target/swingwebpanel-js.jar`가 필요하다.
- 빌드: `tools/netstormparse/build.bat` 또는 `tools/netstormparse/build.sh`
- 실행: `tools/netstormparse/run.bat "H:\Workspace\git\NetstormReborn\webpj\www"`

도구는 번들을 Script로, `debug.html`의 inline module과 `dist` 밖 모든 `.js` 파일을 Module로 파싱한다.
모듈의 상대 static import도 실제 파일에 연결되는지 검사한다.

## 출력과 종료 코드

출력은 파싱할 파일 수, 문자 수, 모듈 수, static import 수, 실패 파일별 위치와 최종 `... syntax error(s), ... missing module(s)` 요약이다.
종료 코드: 0 성공 / 1 구문 오류 또는 없는 모듈 / 2 입력·파일·사용법 오류.

## 제약

- Java 6 문법/API만 사용하고 UTF-8로 읽는다.
- 실행 의미, DOM, 렌더링, 오디오, 게임 자산 적재는 검사하지 않는다.
- `Parser.parseScript`/`parseModule`의 실행 제한을 적용하지 않는다.
- 실제로 실행한 JRE: JDK 6u45 `1.6.0_45`(P28-02-f).
- JRE 6의 기본 인코딩을 쓰지 않는다.

