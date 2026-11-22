# Maven 전환 기록 (P23)

## P23-01 도입 방식 조사와 결정 (2026-11-22)

### 1. 확인한 사실 (이 PC, `tools/webfetch` 로 실제 접속)

`https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml`(Maven Central, HTTPS 전용)에 실행 JRE 별로 접속했다(조회 2026-11-22).

| 실행 JRE | 결과 |
| --- | --- |
| JDK 6u45 (`jdk1.6.0_45`) | **실패**: `SSLHandshakeException: Received fatal alert: handshake_failure` (TLS 1.2 미지원) |
| JDK 7u80 (`jdk1.7.0_80`) | **실패**: `PKIX path building failed` (오래된 신뢰 저장소) |
| JDK 8u202 (`jdk1.8.0_202`) | 접속 성공(본문을 받음) |

따라서 **Maven 자체를 JDK 6 으로 실행하는 방식(마지막 지원판 3.2.5)은 쓸 수 없다**: Maven Central 에서 플러그인과 의존성을 받지 못한다. Maven 은 JDK 8 이상으로 실행하고, 컴파일과 시험 실행만 JDK 6 으로 한다.

### 2. 판본과 요구 JDK (maven.apache.org / mojohaus.org 조회 2026-11-22)

| 도구 | 판본 | 실행에 필요한 것 |
| --- | --- | --- |
| Apache Maven | **3.10.1**(최신, 이전 안정 3.9.16) | JDK 8 이상으로 실행. "toolchains 로 1.3 이상의 다른 JDK 를 대상으로 빌드할 수 있다" |
| maven-compiler-plugin | 3.16.0 | Maven 3.6.3, JDK 1.8 |
| maven-toolchains-plugin | 3.3.0 | Maven 3.6.3, JDK 1.8 |
| maven-jar-plugin | 3.5.1 | Maven 3.6.3, JDK 1.8 |
| maven-resources-plugin | 3.5.0 | Maven 3.6.3, JDK 1.8 |
| maven-shade-plugin(합친 단일 JAR) | 3.6.2 | Maven 3.6.3, JDK 1.8 |
| exec-maven-plugin(자체 시험 실행기) | 3.7.0 | Maven 3.9.0, JDK 1.8 |

(판본은 조회 시점의 최신이다. 처음 빌드할 때 `mvn help:effective-pom` 으로 실제 해석된 판본을 확인해 POM 에 고정한다.)

### 3. 결정

1. **Maven 은 JDK 8(`jdk1.8.0_202`, 또는 그 이상)로 실행**하고 `JAVA_HOME` 은 그 JDK 를 가리킨다. 제품은 JDK 6 으로 컴파일한다.
2. **컴파일은 toolchains 로 JDK 6 의 `javac`**: `maven-toolchains-plugin`(`toolchain` 목표, `jdk` 버전 `1.6`)이 `~/.m2/toolchains.xml` 의 JDK 6 정의를 찾고, `maven-compiler-plugin` 이 그 `javac` 로 `-encoding UTF-8 -source 1.6 -target 1.6 -g` 컴파일한다. JDK 8 의 `javac -source 6 -target 6` 으로는 JRE 6 API 호환이 증명되지 않으므로(클래스 라이브러리가 JDK 8 것) 쓰지 않는다. `toolchains.xml` 샘플은 저장소에 두고(`docs/` 또는 모듈 루트) 사용자가 경로를 고친다.
3. **JDK 6 을 찾지 못하면 다른 JDK 로 조용히 대체하지 않고 빌드가 오류로 끝난다**: toolchains 가 일치하는 정의가 없으면 실패하는 기본 동작을 그대로 쓴다(`<failOnNoMatch>` 성격). 다른 JDK 로 시험하려면 `JAVA6_HOME`/toolchains 를 명시적으로 바꾸고, 그 결과는 JRE 6 검증으로 기록하지 않는다.
4. **시험은 자체 실행기 `testing.TestRunner`**(외부 시험 라이브러리 없음)를 `exec-maven-plugin`(`exec` 목표, `verify` 단계)으로 **JDK 6 의 `java`** 에서 실행한다(`<executable>${jdk6.home}/bin/java</executable>`; surefire 는 JDK 6 지원 여부와 우리 실행기 때문에 쓰지 않는다). 실행기 인수(`--filter`)는 Maven 속성(`-Dtest.filter=...`)으로 넘긴다. JVM 옵션은 지금 스크립트와 같게 `-XX:MaxPermSize=256m`(시험 JVM 에만), 엔진 단계는 `-Djava.awt.headless=true` 이다. 실패는 종료 코드로 빌드 실패가 된다.
5. **모듈 구성**: P26 이 `engine/` 을 이미 따로 떼어 두었으므로 처음부터 부모 POM(`packaging=pom`) + 하위 모듈로 간다. `swingwebpanel-js`(`engine/`) → `swingwebpanel`(브라우저, 엔진에 의존) → `swingwebpanel-examples`(제품 JAR 에 넣지 않음). 엔진 모듈의 `dependencies` 는 비워 두어(외부 라이브러리 없음, 브라우저 의존 금지) 경계를 빌드로 강제한다. 대응표(`engine/src`→`src/main/java` 등)는 `docs/progress/engine.md` D 절. 엔진 시험 실행기(`testing.*`)는 엔진 모듈의 test-jar 로 내보내 브라우저 모듈이 `test` 범위로 쓴다.
6. **합친 단일 JAR**(`swingwebpanel.jar`, 기존 사용자가 JAR 하나만 클래스패스에 두는 방식)은 `maven-shade-plugin`(또는 assembly)으로 두 모듈을 합쳐 만들고 이름은 P23-03 에서 확정한다(Maven 산출물 이름 `swingwebpanel` 은 브라우저만이므로 합친 JAR 에는 `swingwebpanel-all` 같은 분류자를 쓰는 안이 유력). 서비스 파일(`META-INF/services/javax.script.ScriptEngineFactory`)은 합칠 때 보존한다.
7. **제품 POM 의 런타임 `dependencies` 는 비운다**. Maven 플러그인은 빌드 도구일 뿐 제품 런타임 의존성이 아니다. `tools/` 의 도구는 reactor 에서 제외하고 각자의 sh/bat 빌드를 유지한다.
8. **오프라인 재현**: 플러그인을 한 번 받은 뒤 `mvn -o` 로 빌드되는지 P23-07 에서 확인한다.

### 4. 지금 막혀 있는 것 (2026-11-22)

이 PC 에는 **Maven 이 설치되어 있지 않다**(`mvn` 이 PATH 에 없고 `~/.m2/repository` 도 비어 있다). 사용자가 직접 설치하기로 했다(2026-11-22). P23-02 이후는 실제 `mvn` 으로 빌드·시험해야 검증할 수 있으므로 설치되기 전에는 진행하지 않는다(검증 없이 POM 만 쓰고 완료로 표시하지 않는다). 설치할 때 필요한 것: Apache Maven 3.9 이상(JDK 8 이상으로 실행), `JAVA_HOME` 은 JDK 8, `~/.m2/toolchains.xml` 에 JDK 6u45 정의(`<jdk><version>1.6</version></jdk>`, `jdkHome` = `C:\Program Files\Java\jdk1.6.0_45`).

### 5. 재개 절차 (Maven 이 설치된 뒤)

1. `mvn -v`(JDK 8 로 실행되는지), `mvn help:effective-settings` 로 환경을 확인한다.
2. P23-02: `git mv` 로 `src`→`src/main/java` 등을 옮기고(엔진 모듈 `engine/` 은 그대로 모듈 디렉터리), 부모 POM 과 모듈 POM 을 만든다(P23-03).
3. P23-04: 시험 실행 연결, P23-05: 기존 스크립트 정리, P23-06: 도구·문서의 경로 갱신, P23-07: 전환 전 `build.sh` 결과와 JAR 항목·시험 수 비교.
