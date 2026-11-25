# unicodegen

ECMAScript 식별자, 문자열 대소문자 변환, 정규식이 공유하는 Unicode 18.0.0 범위와 매핑을 Java 소스로 생성합니다.

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
정규식 이모지 전용 속성에는 프로젝트의 예외 EXC-15가 적용됩니다.

