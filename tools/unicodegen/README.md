# unicodegen

ECMAScript 식별자·정규식의 Unicode 범위, 문자열 대소문자 변환과 정규화에 필요한 Unicode 18.0.0 매핑을 Java 소스로 생성합니다.

## 빌드

```bat
tools\unicodegen\build.bat
```

JDK 6 경로는 `JAVA6_HOME`으로 바꿀 수 있으며 기본값은 `C:\Program Files\Java\jdk1.6.0_45`입니다.

## 실행

```bat
tools\unicodegen\run.bat C:\path\to\ucd18 engine\src\main\java\org\duckdns\hjow\swingwebpanel\js\text\UnicodePropertyData.java
```

입력 디렉터리에 [AGENTS.md](AGENTS.md)에 열거한 Unicode 18.0.0 파일을 모읍니다. 결과를 위 경로에 두고 엔진을 빌드합니다.

정규화의 원본 적합성 자료를 모두 검사하려면 같은 판본의 `NormalizationTest.txt`와 JDK 6으로 컴파일한 시험 클래스가 필요합니다. 전체 실행 명령은 [AGENTS.md](AGENTS.md#정규화-적합성-시험)에 있습니다.
정규식 이모지 전용 속성에는 프로젝트의 예외 EXC-15가 적용됩니다.

