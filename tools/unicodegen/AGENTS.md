# unicodegen (AI용)

## 목적

Unicode 18.0.0 UCD의 일반 범주·Script·Script_Extensions·이진 속성과 대소문자 매핑을 ECMAScript 식별자, 문자열 변환 및 정규식 엔진이 공유하는 표로 만든다.
계획 항목은 P29-02-g이다.

## 입력과 출력

입력 디렉터리에는 `ReadMe.txt`, `UnicodeData.txt`, `Scripts.txt`, `ScriptExtensions.txt`, `PropList.txt`,
`DerivedCoreProperties.txt`, `DerivedNormalizationProps.txt`, `DerivedBinaryProperties.txt`, `PropertyAliases.txt`,
`PropertyValueAliases.txt`, `SpecialCasing.txt`, `CaseFolding.txt`를 둔다. 배포본의 `extracted/DerivedBinaryProperties.txt`를 같은 디렉터리에 놓는다.
출력 `UnicodePropertyData.java`는 엔진의 `org.duckdns.hjow.swingwebpanel.js.text` 패키지에 둔다.
Unicode 18.0.0 배포본을 2026-11-26에 조회했다. 원본은 [Unicode Character Database](https://www.unicode.org/Public/18.0.0/ucd/)에 있다.
ECMA-262 제17판 표에서 허용하는 속성과 정확한 별칭만 생성한다. 이모지 전용 속성에는 EXC-15가 적용된다.
대소문자 자료에는 로케일 독립 전체 매핑과 단순 접기(C, S 상태)를 넣는다. 어말 시그마의 문맥 판정은 엔진에서 처리한다.

## 빌드와 실행

- Java 6 소스: `tools/unicodegen/src/unicodegen/UnicodeGen.java`
- 빌드: `tools/unicodegen/build.sh` 또는 `tools\unicodegen\build.bat`
- 실행: `tools/unicodegen/run.sh <ucd-directory> <output-java>`
- 종료 코드 0 성공, 1 사용법 오류, 2 입력/출력 오류.
- 실제 실행 JRE: JDK 6u45 (`java version "1.6.0_45"`).

제품과 시험은 이 도구 클래스를 사용하지 않는다. 생성 소스만 제품 JAR에 들어간다.

