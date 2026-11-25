# unicodegen (AI용)

## 목적

Unicode 13.0 UCD의 범주·Script·Script_Extensions·이진 속성 구간을 제품 정규식 엔진의 Java 표로 만든다.
계획 항목: P28-05.

## 입력과 출력

입력 디렉터리에는 `UnicodeData.txt`, `Scripts.txt`, `ScriptExtensions.txt`, `PropList.txt`,
`DerivedCoreProperties.txt`, `PropertyValueAliases.txt`가 있어야 한다.
출력은 `UnicodePropertyData.java`이며 엔진 패키지 `org.duckdns.hjow.swingwebpanel.js.regexp`에 둔다.
판본은 Unicode 13.0.0, 원본은 `https://www.unicode.org/Public/13.0.0/ucd/`, 조회일 2026-11-25이다.
JAR 용량 제한을 위해 Emoji 13 속성 표는 의도적으로 생성하지 않는다.

## 빌드와 실행

- Java 6 소스: `tools/unicodegen/src/unicodegen/UnicodeGen.java`
- 빌드: `tools/unicodegen/build.sh` 또는 `tools\unicodegen\build.bat`
- 실행: `tools/unicodegen/run.sh <ucd-directory> <output-java>`
- 종료 코드 0 성공, 1 사용법 오류, 2 입력/출력 오류.
- 실제 실행 JRE: JDK 6u45 (`java version "1.6.0_45"`).

제품과 시험은 이 도구 클래스를 사용하지 않는다. 생성 소스만 제품 JAR에 들어간다.

