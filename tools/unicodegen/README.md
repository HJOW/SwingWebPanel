# unicodegen

정규식의 `\p{...}` / `\P{...}` 속성에 사용할 Unicode 13.0 표를 Java 소스로 만드는 도구입니다.
Unicode 13.0.0 UCD 파일을 입력으로 받아 제품 실행 중 파일 접근이나 JRE의 유니코드 판본에 의존하지 않는 구간 표를 생성합니다.

## 빌드

```bat
tools\unicodegen\build.bat
```

JDK 6 경로는 `JAVA6_HOME`으로 바꿀 수 있으며 기본값은 `C:\Program Files\Java\jdk1.6.0_45`입니다.

## 실행

```bat
tools\unicodegen\run.bat C:\path\to\ucd13 engine\src\main\java\org\duckdns\hjow\swingwebpanel\js\regexp\UnicodePropertyData.java
```

입력 디렉터리에는 위 `AGENTS.md`에 적은 UCD 파일 6개가 필요합니다. JAR 크기를 제한하기 위해 Emoji 13 속성은 생성하지 않습니다.

