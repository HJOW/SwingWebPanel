# SwingWebPanel

한국어 · [English](README.md)

## 소개

Java 의 Swing 용 웹 브라우저 컴포넌트입니다.    
일부 에셋을 제외하면 순수 자바로 구현되어 있습니다.
```java
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
public class JavaBrowser extends JFrame {
    private SwingWebPanel web;
    public JavaBrowser() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());
        
        web = new SwingWebPanel();
        add(new JScrollPane(web));
        
        web.setPage("https://www.w3.org/");
    }
    public static void main(String[] args) {
        new JavaBrowser().setVisible(true);
    }
}
```
Java 6 이상 버전을 모두 지원합니다.
    
Maven 을 통한 의존성 주입이 가능합니다.    
SwingWebPanel 컴포넌트 사용 시 다음 사항을 dependencies 에 넣습니다.
```xml
<dependency>
    <groupId>org.duckdns.hjow</groupId>
    <artifactId>swingwebpanel</artifactId>
    <version>0.1.2</version>
</dependency>
```
만약 Swing 컴포넌트가 아닌 스크립트 엔진으로만 사용한다면 다음과 같이 사용하면 됩니다.
```xml
<dependency>
    <groupId>org.duckdns.hjow</groupId>
    <artifactId>swingwebpanel-js</artifactId>
    <version>0.1.2</version>
</dependency>
```
    
오픈소스 라이센스에 따라, 이 컴포넌트를 사용함에 따른 결과는 사용자 책임입니다.    
프로젝트 특성상 보안 취약점 이슈가 자주 발생할 수 있으므로,    
되도록이면 자체 개발한 홈페이지 혹은 프로젝트 내장 웹페이지 출력 용도로만 사용하는 것을 권장합니다.
    
우리 프로젝트에서는 AI를 적극 사용하여 개발하고 있습니다.
    
## 소개 (AI 작성)

Java Swing 에서 쓰는 웹 브라우저 컴포넌트입니다. HTML 파서, CSS 처리기, 레이아웃/렌더링 엔진, JavaScript 엔진(ECMA-262 제17판, ECMAScript 2026 기준), DOM, 이미지/오디오 디코더, HTTP/HTTPS/FTP 요청 계층을
**순수 Java 로 직접 구현**했고 외부 라이브러리, 기존 JavaScript 엔진(Rhino 등), JavaFX WebView, 네이티브 코드(JNI 등), OS 브라우저 실행에 의존하지 않습니다.

HTML·CSS·JavaScript의 지원 범위와 브라우저 호환성 차이는 [기능 표](docs/FEATURE_MATRIX.md)에 있습니다.
JavaScript는 이전 판본과 ES2026 기능을 함께 검증합니다. 프로젝트의 검증 결과와 작업 계획은 [진행 기록](docs/PROGRESS.md)과 [개발 계획](AGENTS.md)에 있습니다.

## 요구 사항

- Java 6 이상. 제품 코드는 Java 6 문법/API 만 쓰며 JRE 6 에서 검증합니다(`C:\Program Files\Java\jdk1.6.0_45`, 다른 경로는 환경 변수 `JAVA6_HOME`).
- JRE 6u45의 JSSE는 TLS 1.0까지 지원하며 클라이언트 SNI가 없습니다. 선택 모듈로 Java 6·7에서 TLS 1.3과 1.2를 사용할 수 있습니다.
  (더 새로운 JRE 에서는 그 JRE 가 지원하는 프로토콜을 씁니다).

## 빌드와 시험

저장소 루트에서 Maven 3.8.8 이상을 JDK 8 이상으로 실행합니다(`JAVA_HOME`). 컴파일과 회귀 시험에는 별도로 설치한 JDK 6을 사용합니다.
기본 경로는 Windows의 `C:\Program Files\Java\jdk1.6.0_45`와 Linux의 `/usr/lib/jvm/java-6-openjdk`입니다.
설치 경로가 다르면 `JAVA6_HOME`을 지정합니다. 빌드는 JDK 6의 `javac`와 `java`를 확인하고 잘못된 경로 또는 버전에서 실패합니다.

| 하는 일 | 명령 |
| --- | --- |
| 전체 시험과 패키징 | `mvn verify` |
| JAR만 만들기 | `mvn -Dmaven.test.skip=true package` |
| 엔진 시험만 | `mvn -pl engine test` |
| 이름에 `BrowserJarsTest`가 들어간 시험 실행 | `mvn "-Dtest.filter=BrowserJarsTest" test` |
| 오프라인 검증 | 플러그인을 한 번 받은 뒤 `mvn -o verify` |
| 로컬 Maven 저장소에 설치 | `mvn install` |

`build.sh`/`build.bat`, `test.sh`/`test.bat`, `engine/build.*`와 `engine/test.*`도 Maven을 호출합니다. 시험 스크립트는 기존 `--filter 문자열`을 받습니다.
예제 실행은 `examples/run.sh BlankPageExample` 또는 `examples\run.bat BlankPageExample`을 사용합니다. [예제 목록](examples/README.md)

Eclipse에서는 저장소 루트를 **Existing Maven Projects**로 가져오면 부모 POM과 세 모듈을 선택할 수 있습니다.

### Javadoc 생성

Windows 명령 프롬프트에서 `javadoc.bat`, POSIX 셸에서 `sh javadoc.sh`를 실행합니다. JDK 6이 기본 경로에 없으면 `JAVA6_HOME`을 지정합니다. Maven 빌드는 필요하지 않습니다.

생성이 끝나면 `docs/javadoc/engine/index.html`, `docs/javadoc/tls13/index.html`, `docs/javadoc/browser/index.html`, `docs/javadoc/examples/index.html`, `docs/javadoc/desktopapp/index.html`을 열 수 있습니다.

### JAR 구성

이 체크아웃의 Maven 버전은 `0.1.2`입니다. `mvn install` 후 브라우저와 엔진을 함께 쓰려면 `org.duckdns.hjow:swingwebpanel:0.1.2`를,
엔진만 쓰려면 `org.duckdns.hjow:swingwebpanel-js:0.1.2`를 의존성에 추가합니다.

Maven 없이 실행하는 애플리케이션은 **`browser/target/swingwebpanel.jar` 하나를 클래스패스에** 두면 됩니다.

| 파일 | 내용 |
| --- | --- |
| `browser/target/swingwebpanel.jar` | 브라우저와 JavaScript 엔진의 합본 |
| `browser/target/swingwebpanel-browser.jar` | 브라우저. 실행 시 엔진 JAR 필요 |
| `engine/target/swingwebpanel-js.jar` | 독립 JavaScript 엔진과 `javax.script` 제공자 |

예제는 `examples/target/`에 별도로 빌드됩니다. SwingWebPanel 로 만든 웹 브라우저 앱(탭, 북마크, 설정, 찾기, 인쇄 등)은 `desktopapp/target/swingwebpanel-desktopapp.jar`로 빌드되며 `java -jar`로 실행합니다([desktopapp/README.md](desktopapp/README.md)). Maven 플러그인은 빌드에만 쓰이며 제품에 외부 런타임 라이브러리가 추가되지 않습니다.

### Maven Central 배포

저장소 루트에서 다음 명령을 실행하면 `release` 프로필로 소스·Javadoc JAR를 만들고 GPG 서명 후 Central에 게시합니다.

```sh
mvn -Prelease clean deploy
```

업로드 없이 서명된 배포 번들만 만들려면 `mvn -Prelease "-Dcentral.skipUpload=true" clean deploy`를 실행합니다.
Central Portal 토큰과 서명 키 설정은 [배포 상세](docs/progress/maven.md#maven-central-배포)를 확인하세요.

## 사용법

`SwingWebPanel` 은 `JPanel` 이며 `Scrollable` 을 구현합니다. 스크롤은 호출자가 `JScrollPane` 으로 감싸서 얻습니다(내부에 다른 스크롤 창을 만들지 않습니다).
주 패키지는 `org.duckdns.hjow.swingwebpanel` 입니다(이전 이름 `swingwebpanel` 에서 변경되었으므로 기존 코드는 `import` 를 바꿔야 합니다).

```java
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

SwingWebPanel panel = new SwingWebPanel();
panel.addPropertyChangeListener(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent e) {
        // "page", "statusText", "progress", "title" 속성의 변경이 EDT 에서 통지된다.
        System.out.println(e.getPropertyName() + " = " + e.getNewValue());
    }
});
frame.add(new JScrollPane(panel));
panel.setPage("http://example.com/");     // 비동기: 곧바로 돌아오고 읽기는 백그라운드에서 진행된다.
...
panel.dispose();                          // 창을 닫을 때 반드시 호출한다.
```

모든 공개 메서드는 EDT 와 일반 스레드에서 호출할 수 있습니다. 입력은 호출 시점에 검증하고(잘못된 인수는 `IllegalArgumentException`), 화면 변경은 EDT 에서 일어나며,
네트워크와 긴 파싱/스크립트 실행은 EDT 를 막지 않습니다. 조회 메서드는 마지막으로 확정된 상태의 복사본이나 불변 값을 돌려줍니다.

### 공개 메서드

| 메서드 | 설명 |
| --- | --- |
| `setPage(String url)` / `getPage()` | 주소로 비동기 이동하고 확정된 주소를 조회합니다. HTTP, HTTPS, 익명 FTP, `classpath://`, 로컬 `file:`, 등록된 `blob:` 및 `about:blank`를 지원합니다. `setContent`로 표시한 문서의 주소는 `about:text`입니다. |
| `setContent(String content, String contentType)` / `getContent()` | `text/html`, `text/plain`, `text/markdown`, `application/xhtml+xml` 를 지원한다. `getContent()`는 DOM 변경이나 Markdown 변환과 관계없이 **받은 원문**을 돌려줍니다. |
| `back()`, `back(int n)`, `forward()`, `forward(int n)`, `getHistories()` | 이력 이동(정확히 n 칸 갈 수 없으면 아무 일도 하지 않음)과 이력 URL 의 `Vector<String>` 복사본. |
| `refresh()`, `refresh(boolean force)` | 새로고침. `force` 는 이 컴포넌트가 읽는 주 문서와 하위 리소스의 캐시를 우회한다. |
| `setUserAgent(String)` / `getUserAgent()` | 기본값 `swingwebpanel/1.0`. |
| `addHeaders(Map<String,String>)` / `getHeaders()` | 모든 요청에 붙는 추가 헤더(이름은 대소문자 무시, 기존 값을 덮어씀, `User-Agent` 는 받지 않고 조회에서도 제외). |
| `getCookies()` | 이 컴포넌트의 만료되지 않은 쿠키 전부(`name=value; name=value`). 쿠키 저장소는 기본적으로 컴포넌트별이다. |
| `new SwingWebPanel(CookieJar)`, `getCookieJar()`, `clearCookies()` | 여러 컴포넌트가 쿠키를 공유하게 합니다. `CookieJar` 하나를 만들어 각 컴포넌트의 생성자에 넘기면 한 컴포넌트의 로그인이 나머지에서도 유효합니다(브라우저의 탭). 공유 저장소는 컴포넌트를 폐기해도 남습니다. `clearCookies()` 와 `CookieJar.clear()` 는 쿠키를 지웁니다(`clean()` 은 쿠키를 유지합니다). |
| `copy()`, `cut()`, `paste()`, `selectAll()` | 메뉴 항목처럼 호스트가 실행하는 편집 명령입니다. 해당 단축키(Ctrl+C/X/V/A)와 같은 일을 하며 페이지에 클립보드 이벤트는 가고 키 이벤트는 가지 않습니다. 컴포넌트가 스스로 처리하는 이 키들과 확대 키는 소비하므로 호스트의 메뉴 가속 키가 같은 키로 다시 실행되지 않습니다. |
| `find(String, boolean, boolean)`, `clearFind()`, `getFindResult()` | 페이지 안 찾기입니다. 표시되는 글에서 모든 일치를 강조하고 현재 일치로 스크롤하며, 같은 글로 다시 부르면 다음·이전 일치로 갑니다. 결과(`FindResult`: 개수와 현재 번호)는 `findResult` 속성으로 알립니다. |
| `createPrintout(PageFormat, boolean, PrintoutListener)` | 보이는 페이지를 인쇄용으로 배치해 `PagePrintout`(`java.awt.print.Pageable`)을 만들어 줍니다. `@media print`, `@page` 의 크기·여백, 쪽 나눔을 따릅니다. 프린터 선택과 인쇄 작업(`PrinterJob`)은 호스트가 합니다. 페이지의 `window.print()` 는 재정의할 수 있는 `printRequested()` 로 옵니다(기본은 아무것도 하지 않음). |
| `setDefaultFontFamily(String, String)`, `setDefaultFontSize(int)`, `setDefaultMonospaceFontSize(int)` | 페이지가 글꼴을 정하지 않은 글에 쓰는 컴포넌트별 기본 글꼴과 크기입니다. |
| `setPopupBlockingEnabled(boolean)` / `isPopupBlockingEnabled()` | 팝업 차단(기본 켬): 사용자의 조작 없이 부른 `window.open` 을 막습니다. 허용된 창은 기본 팝업 창(`JDialog`)에 열리고, `openPopupWindow`·`openNewWindow`·`popupBlocked` 를 재정의해 탭이나 자기 창에 보일 수 있습니다. 여러 컴포넌트 중 하나만 닫을 때는 `disposeKeepingStorage()` 를 씁니다. |
| `setJavascriptEnabled(boolean)` / `isJavascriptEnabled()` | 기본 켜짐. 끄면 스크립트, 인라인 핸들러, 타이머가 멈추고 HTML/CSS/링크/폼은 계속 동작한다. 다시 켜도 이미 중단한 페이지의 스크립트를 되살리지 않으며 다음 이동부터 실행한다. |
| `getStatusText()`, `getProgress()` | 상태바 메시지와 진행률(아래 표). |
| `addHyperlinkListener`, `removeHyperlinkListener`, `removeHyperlinkEvents()` | `javax.swing.event.HyperlinkListener` 로 링크 진입/이탈/활성화를 받는다. 통지를 모두 제거해도 링크 이동 자체는 계속 동작한다. |
| `addConsoleListener`, `removeConsoleListener`, `getConsoleListeners()` | 페이지 스크립트의 `console.log`/`console.error` 등 콘솔 출력을 `ConsoleListener` 로 받는다(아래 "콘솔 출력 받기"). `clean()` 은 유지하고 `dispose()` 는 제거한다. |
| `postMessage(Object[,String])`, `addWebMessageListener`, `removeWebMessageListener`, `getWebMessageListeners()` | 현재 최상위 문서에 복사한 값을 보내고, 페이지의 호스트 메시지를 EDT에서 받는다. |
| `setRootWindowClosingEnabled(boolean)` / `isRootWindowClosingEnabled()` | 팝업이 아닌 본 창의 페이지가 `window.close()`로 창을 닫는 것을 허용합니다. 기본은 꺼짐(무시)이고, 켜면 기본 동작은 `System.exit(0)`으로 프로그램을 끝내는 것입니다(아래 "페이지가 창을 닫게 하기"). |
| `getLocalStorage()`, `getSessionStorage()` | Web Storage 항목의 읽기 전용 복사본(`StorageEntry`: origin, key, value). |
| `clean()` | 진행 중인 작업을 취소하고 `about:blank` 로 돌아가며 캐시, sessionStorage, **프로세스 공용 localStorage 전체**를 비운다(다른 컴포넌트도 영향을 받는다). User-Agent, 추가 헤더, 쿠키, JavaScript 설정, 사용자 리스너는 유지한다. |
| `dispose()` | `clean()` 후 이 컴포넌트에 등록된 모든 사용자 리스너와 내부 자원(타이머, 하위 작업, 미디어, 다운로드)을 해제한다. 반복 호출은 안전하다. 이후 변경 메서드는 `IllegalStateException`, 조회는 빈 상태를 돌려준다. |
| 그 밖 | `getTitle()`(스크립트가 `document.title` 을 바꾸면 따라 바뀌고 `title` 속성으로 알립니다), `getMimeType()`, `getHistoryIndex()`, `stop()`, `isDisposed()` |

생성 직후 상태: 주소 `about:blank`, 원문 빈 문자열, 이력 `about:blank` 하나, 추가 헤더/쿠키 없음, JavaScript 켜짐, 진행률 100.

### 상태와 진행률

| 상황 | `getStatusText()` | `getProgress()` |
| --- | --- | --- |
| 초기, `clean()` 뒤 | `""` | 100 |
| 이동 시작 | `Loading <URL>` | 0 |
| 본문 수신 중 | `Loading <URL>` | 길이를 알면 1~50, 모르면 **-1**(불확정) |
| 페이지 확정 뒤 리소스/스크립트를 기다리는 동안 | `Loading resources (n/m)` | 50~94 |
| 완료(모든 필수 하위 작업이 성공하거나 실패해 끝남) | `Done` | 100 |
| 오류 페이지(404, 500, 연결 실패 등) | `Error 404` 등 | 100 |

링크 위에 마우스를 올리면 그 주소가 상태 메시지에 보입니다.

### 이동, 이력, 폼

- 이동은 비동기입니다. 로딩 중에는 `getPage()`/`getContent()` 가 이전에 확정된 페이지를 돌려주고, 새 페이지가 확정되면 URL, 원문, 문서, 이력 위치가 한꺼번에 바뀝니다.
  새 이동이 시작되면 이전 요청은 취소되고 늦게 도착한 응답은 무시됩니다. 네트워크 실패는 예외가 아니라 오류 페이지가 됩니다.
- 리디렉션은 최종 주소 한 건만 이력에 남고, 새로고침과 이력 이동은 항목을 추가하지 않습니다. 뒤로 간 뒤 새 이동을 하면 앞쪽 이력이 지워집니다.
- 폼의 `GET` 은 쿼리가 붙은 주소로의 일반 이동입니다. `POST` 에 응답한 페이지는 이력에서 **폼을 다시 보내지 않고 저장한 원문으로** 보여 주며, 새로고침(과 그 응답을 파일로 저장해야 할 때)은
  `protected boolean confirmResubmit(String url)` 로 사용자에게 묻습니다(기본은 확인 대화상자, 헤드리스에서는 아니오). 자세한 정책은 [docs/API_CONTRACT.md](docs/API_CONTRACT.md) 5.8.

### 로컬 파일과 객체 URL

Java에서 로컬 파일을 열 때는 `panel.setPage(new File(path).toURI().toASCIIString())`을 사용합니다. 로컬 문서는 상대 경로와 `fetch`/XHR로 다른 로컬 파일을 읽을 수 있습니다. HTTP 리디렉션, 원격 문서와 불투명 오리진의 문서, UNC 주소는 로컬 파일에 접근할 수 없습니다.

페이지와 워커가 `URL.createObjectURL`로 등록한 `blob:` 주소는 같은 컴포넌트에서 열 수 있습니다. 하위 리소스, 프레임, Fetch/XHR 및 링크 다운로드는 생성자의 오리진을 따릅니다. `URL.revokeObjectURL` 호출이나 생성한 문맥의 종료로 등록이 해제됩니다. 이미 시작된 다운로드는 확보한 바이트를 사용합니다. [주소 정책](docs/API_CONTRACT.md#6-허용-스킴-정책-p01-12)

### HTTPS와 SSL 재정의

HTTPS는 기본으로 JRE의 신뢰 저장소와 호스트 이름 검증을 씁니다. `configureSsl`을 재정의하면 해당 연결의 신뢰 저장소나 호스트 이름 검증을 정할 수 있습니다.

```java
SwingWebPanel panel = new SwingWebPanel() {
    protected void configureSsl(HttpsURLConnection connection) throws IOException {
        super.configureSsl(connection);           // 기본: 이 JRE 가 지원하는 프로토콜 선택
        connection.setSSLSocketFactory(myFactory); // 이 컴포넌트의 이 연결에만 적용
    }
};
```

Java 6·7에서 선택 모듈을 쓰려면 클래스패스에 `tls13/target/swingwebpanel-tls13.jar`를 넣고 페이지를 열기 전에 켭니다. 데스크톱 앱은 모듈을 포함하며 Java 6·7에서 자동으로 켭니다.

```java
SwingWebPanel panel = new SwingWebPanel();
if (SwingWebPanel.isTls13ModuleAvailable()) {
    panel.setTls13ModuleEnabled(true);
}
panel.setPage("https://example.com/");
```

모듈이 협상 실패를 보고하면 같은 HTTPS 주소를 JRE JSSE로 한 번 더 시도합니다. 최종 연결/TLS 실패 뒤에는 기존 조건에 따라 HTTP로 한 번 접근합니다. 이때 Secure 쿠키는 보내지 않습니다. HTTP 404/500으로 응답이 끝났으면 TLS 재시도가 없습니다.

선택 TLS 모듈은 외부 보안 감사를 받지 않았습니다. P-256·P-384 키 교환은 연산 시간이 일정하지 않으며 인증서 폐기를 확인하지 않습니다. 내장 Mozilla 루트는 2026-11-28에 고정한 CCADB 자료입니다. 보안이 중요한 환경에서는 유지보수되는 JRE를 사용하세요. [모듈 사용법과 한계](tls13/README.md)

페이지의 `XMLHttpRequest`/`fetch` 는 컴포넌트와 같은 요청 계층(User-Agent, 추가 헤더, 쿠키, SSL 재정의, 스킴 제한)을 지나며 교차 오리진 요청은 CORS 를 따릅니다: 단순하지 않은 요청은 `OPTIONS` 사전 요청으로 서버의 허락을 먼저 받습니다. JRE 의 `HttpURLConnection` 은 `Origin` 같은 제한 헤더를 버리므로 그 헤더를 실어야 하는 요청(교차 오리진 요청, 사전 요청, 오리진이 있는 POST/PUT/PATCH/DELETE)은 소켓 기반 연결로 보내며 위의 `configureSsl` 재정의가 그 연결에도 똑같이 적용됩니다. 단 JVM 프록시(`http.proxyHost` 등)가 적용되는 환경에서는 소켓 연결이 프록시를 쓰지 못하므로 JRE 연결을 쓰고 그 헤더는 사라집니다.

### 웹 API와 스크립트 작업 제한

`AbortSignal`로 이벤트 리스너를 등록해 취소할 수 있습니다. Fetch 응답은 `body.getReader()`와 비동기 반복을 지원합니다. 클래스패스에서 읽는 Fetch 스트림 한 건은 32 MiB까지 제공하고, `arrayBuffer()` 등 일괄 소비에는 기존 16 MiB 한도가 적용됩니다.

`navigator.clipboard.readText()`와 `writeText()`는 HTTPS, 루프백 또는 호스트가 제공한 클래스패스 문서에서 일반 텍스트를 다룹니다. 텍스트를 쓰려면 신뢰 사용자 입력이 필요합니다. 읽기는 Ctrl+V 또는 Shift+Insert에서 허용합니다. [기능 표](docs/FEATURE_MATRIX.md)

스크립트 작업의 기본 실행 시간 한도는 10초입니다. 호스트가 제공하는 특정 앱에 더 긴 초기화 시간이 필요하면 `protected long getScriptTaskTimeLimitMillis(String url)`을 재정의할 수 있습니다. 로더가 최상위 문서의 최종 URL로 한 번 호출하며, 하위 프레임에는 기본값이 적용됩니다. [스크립트 계약](docs/API_CONTRACT.md#54-setjavascriptenabledboolean-c-js--p15-10)

### 파일 다운로드

링크가 `Content-Disposition: attachment` 응답이나 보여 줄 수 없는 형식(zip 등)을 가리키거나 같은 오리진의 `<a download>` 이면 페이지를 바꾸지 않고 **Swing 다운로드 팝업**(`JDialog` 와 `JFileChooser`)이
열립니다. 파일 이름/출처/저장 위치/진행률(크기를 모르면 불확정)/취소/결과를 보여 주며, 전송은 백그라운드 스레드에서 하고 컴포넌트와 같은 요청 계층(User-Agent, 헤더, 쿠키, SSL 확장 지점)을 씁니다.
같은 오리진의 `blob:` 링크는 DOM에 붙이지 않은 앵커의 `click()`으로도 저장할 수 있습니다. 헤드리스 환경에서는 다운로드를 취소하고 상태 메시지로 알립니다. 호스트가 직접 UI 를 만들려면 `protected DownloadHandle createDownloadPopup(DownloadRequest, DownloadControl)` 을,
저장 위치를 정하려면 `protected File getDownloadDirectory()` 를 재정의합니다(예: [DownloadPopupExample](examples/src/main/java/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java)).

### 오디오와 비디오

`<audio>` 는 PCM/WAV/AIFF/AU/MP3/AAC/FLAC/Ogg Vorbis/Opus(WebM 안의 Vorbis·Opus 포함)를 순수 Java 디코더로 재생하며 Java Sound 로 출력합니다(`protected AudioOutputFactory createAudioOutputFactory()` 를 재정의해
출력을 바꿀 수 있고, 장치가 없으면 `error` 이벤트가 됩니다). `<video>` 는 **재생하지 않으며** 요소의 자리에 미지원 비디오임을 표시합니다(`canPlayType` 은 항상 `""`).
이미지/오디오 주소에 접근하지 못하면 그 자리에 미지원 URL 과 `alt` 가 표시됩니다.

Tab으로 `<audio controls>`에 포커스를 옮깁니다. Space나 Enter로 재생과 일시정지를 전환하고, 왼쪽과 오른쪽 화살표로 5초씩 탐색합니다. 위와 아래 화살표로 음량을 10%씩 조절하고 M으로 음소거를 전환합니다.

**Web Audio API(P25, 청취 확인 전)**: `AudioContext`/`OfflineAudioContext` 와 오디오 노드 그래프(원천, 게인, 지연, 필터, 웨이브셰이퍼, 컨볼버, 컴프레서, 애널라이저, 패너와 HRTF, `ScriptProcessorNode`(폐기 예정 API 의 호환 구현, 지연 2 블록)와 `<audio>` 요소의 `createMediaElementSource`(같은 오리진만, 비디오는 지원하지 않음) 등)를 순수 Java 로 구현합니다. `AudioContext` 는 Java Sound 기본 믹서로 소리를 내고 장치가 없으면 소리 없이 시간만 흐릅니다(예외 없음). 호스트는 `protected createWebAudioOutputFactory()` 를 재정의해 소리를 다른 곳으로 보내거나(`<audio>` 요소는 `createAudioOutputFactory()` 가 따로 있습니다) `null` 을 반환해 끌 수 있습니다. **소리가 실제 장치에서 나는지는 자동 시험으로 확인하지 못했습니다**(가짜 장치로 PCM 과 상태를 검증). `decodeAudioData` 는 위 디코더를 재사용하며 컨텍스트의 표본율로 변환합니다. `audioWorklet.addModule()` 로 `AudioWorkletProcessor` 모듈을 불러와 `AudioWorkletNode` 로 쓸 수 있습니다(프로세서는 렌더링 스레드에서 도는 인터프리터 코드라서 단순한 프로세서 일곱 개 안팎이 48 kHz 실시간의 한계이고, 스크립트가 바쁘면 그 구간은 무음이며 콘솔에 경고합니다). 구현 상태는 [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md) 의 Web Audio 절에 있습니다. `MediaStream` 계열(마이크 등 장치 입력)은 지원하지 않습니다.

### 콘솔 출력 받기

페이지 스크립트가 `console.log`, `console.error` 등으로 쓰는 메시지를 호스트가 받으려면 `ConsoleListener` 를 등록합니다. 리스너는 **이벤트 디스패치 스레드**에서, 메시지가 쓰인 순서대로 호출되므로 Swing 컴포넌트를 바로 갱신해도 됩니다.

```java
panel.addConsoleListener(new ConsoleListener() {
    public void consoleMessage(ConsoleEvent e) {
        // 수준(LOG/INFO/WARN/ERROR/DEBUG), 서식을 적용한 메시지, 쓴 주체(문서/프레임/Worker), 위치
        System.out.println("[" + e.getLevel() + "] " + e.getMessage()
                + " (" + e.getScriptUrl() + ":" + e.getLineNumber() + ")");
    }
});
```

- `ConsoleEvent` 는 수준, 메시지(`%s %d %o` 같은 서식 문자열을 적용한 것), 서식 적용 전 인수 문자열, 메서드 이름, 종류(`CONSOLE_CALL`, `DIALOG`, `UNCAUGHT_EXCEPTION`, `UNHANDLED_REJECTION`, `SCRIPT_ABORTED`, `HOST_ERROR`, `MESSAGES_DROPPED`), 쓴 주체, 메시지를 쓴 문서 URL, 스크립트 URL과 줄/열을 가집니다. 문서 URL 은 쓴 시점에 고정되므로 이동 뒤에 도착한 메시지도 어느 페이지의 것인지 알 수 있고, 떠난 페이지의 늦은 메시지는 전달되지 않습니다.
- 잡히지 않은 예외와 처리되지 않은 promise 거부도 `ERROR` 수준으로 알립니다(`console.error` 호출과는 `getKind()` 로 구분). `alert/confirm/prompt` 는 대화 상자를 띄우지 않고 `DIALOG` 종류로 알립니다.
- 리스너가 느려 1000개 넘게 쌓이면 그 뒤 메시지는 버리고, 큐가 비면 `MESSAGES_DROPPED` 이벤트 하나가 버린 개수를 알립니다. 스크립트 실행은 리스너를 기다리지 않습니다.
- **리스너가 하나도 없으면 콘솔 출력은 버려집니다**(표준 출력에 쓰지 않습니다). 예제: `examples/run.sh ConsoleExample`. 자세한 규칙은 `docs/API_CONTRACT.md` 5.20 절.

### Java와 페이지 사이에 메시지 보내기

Java의 `postMessage(message, targetOrigin)`는 대상 오리진과 일치하는 최상위 문서에 `MessageEvent`를 보냅니다. 문서가 리스너를 등록한 뒤 먼저 준비 메시지를 보내면 Java에서 응답을 시작할 수 있습니다. 페이지는 `window.opener.postMessage(data, "swingwebpanel://host")`로 전송하고, 받은 메시지는 `event.source.postMessage(reply, event.origin)`로 답할 수 있습니다.

```java
panel.addWebMessageListener(new WebMessageListener() {
    public void messageReceived(WebMessageEvent event) {
        if (!"https://app.example".equals(event.getOrigin())
                || event.getSourceKind() != WebMessageEvent.SourceKind.DOCUMENT) {
            return;
        }
        if ("ready".equals(event.getData())) {
            event.reply("hello");
        }
    }
});
```

네트워크 페이지나 그 하위 프레임에서 메시지가 올 수 있으므로 오리진과 데이터 형식을 먼저 확인하세요. Java가 특정 사이트에 값을 보낼 때는 해당 절대 오리진을 지정합니다. `reply`는 이벤트가 기록한 원래 문서에만 응답합니다. [값 변환과 수명 계약](docs/API_CONTRACT.md#522-java-호스트와-페이지-스크립트-간-메시지-p27)

Windows에서는 `examples\run.bat MessageExample`로 클래스패스 왕복 예제를 열 수 있습니다. `--smoke`를 붙이면 화면 없이 메시지를 주고받습니다. [예제 목록](examples/README.md)

### 페이지가 창을 닫게 하기

기본적으로 본 창의 페이지가 부른 `window.close()`는 무시되고 페이지의 콘솔에 경고만 남습니다. `setRootWindowClosingEnabled(true)`로 허용하면, 최상위 문서가 `window.close()`를 부를 때 호출한 스크립트 작업이 끝난 직후 EDT에서 `closeRootWindow()`가 한 번 호출됩니다. 기본 구현은 `System.exit(0)`으로 프로그램을 끝냅니다.

```java
SwingWebPanel panel = new SwingWebPanel() {
    protected void closeRootWindow() {
        frame.dispose();   // 프로그램을 끝내지 않고 이 창만 닫는다
        dispose();
    }
};
panel.setRootWindowClosingEnabled(true);
```

**주의:** 이 설정을 켜면 보여 주는 페이지가 프로그램을 종료할 수 있습니다. 내용을 직접 통제하는 페이지에서만 켜세요. 다른 오리진의 프레임과 `allow-top-navigation` 없는 샌드박스 프레임이 부른 `top.close()`는 설정을 켜도 무시됩니다. 닫기가 수행되기 전에 Java에서 `setPage`, `clean()` 등으로 컴포넌트를 조작하면 닫기가 취소됩니다. [닫기 조건과 취소 계약](docs/API_CONTRACT.md#523-확장-메서드-본-창-닫기-허용-p30-확장이며-agentsmd-의-공개-메서드-목록에는-없다)

### 독립 JavaScript 엔진과 `javax.script`

`swingwebpanel-js.jar` 하나를 클래스패스에 두고 Java 6 이상에서 실행할 수 있습니다. 지원 기능에는 JSON 모듈과 import attributes, 가변·공유 버퍼, `Atomics.waitAsync`, `Float16Array`와 `Uint8Array`의 Base64·16진 변환, 이터레이터 헬퍼, 정규식 `/v`, Unicode 18 정규화가 포함됩니다. 정규식의 이모지 전용 속성은 미지원이며 `Intl`은 제공하지 않습니다. `localeCompare`와 `toLocaleString` 계열은 ECMA-262의 기본 동작을 사용합니다. [기능 표 7절](docs/FEATURE_MATRIX.md#7-ecmascript-es2026-목표-및-이전-버전-호환-p00-04-p28-p29)

```java
import org.duckdns.hjow.swingwebpanel.js.embed.*;

JsEngine engine = new JsEngine();
engine.getLimits().setMaxTimeMillis(2000);           // 믿을 수 없는 코드는 시간 제한을 둔다
engine.put("name", "world");
Object r = engine.eval("`hello ${name}`.toUpperCase()");    // "HELLO WORLD"
engine.eval("function twice(x) { return x * 2; }");
Object d = engine.call("twice", Integer.valueOf(21));       // Double 42.0
engine.putFunction("add", new JsHostFunction() {            // 스크립트가 Java 코드에 닿는 유일한 길
    public Object call(Object thisValue, Object[] args) {
        return Double.valueOf(((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue());
    }
});
```

- **값 변환**: Java→스크립트는 `null`, `JsEngine.UNDEFINED`, `Boolean`, 모든 `Number`(기본적으로 Number, `BigInteger`는 정밀도를 유지하는 BigInt), 문자열류/`Character`/`Enum`, `Map`(복사), `Collection`/배열(복사), `JsHostFunction`, 핸들만 받고 **그 밖의 Java 객체는 `IllegalArgumentException`** 으로 거부합니다(스크립트에 Java 객체, 클래스, 메서드가 노출되지 않습니다). 스크립트→Java 는 Number가 **항상 `Double`**, BigInt가 `java.math.BigInteger`, undefined 는 `JsEngine.UNDEFINED`, 배열은 살아 있는 `List`, 객체는 살아 있는 `Map`, 함수는 `JsCallable` 입니다. 복사본은 `toPlain` 으로 얻습니다.
- 형식화 배열과 버퍼는 `JsObjectHandle`로 반환됩니다. 현재 바이트의 독립 복사본을 얻으려면 `TypedArrays.copyBytes(handle.getJsObject())`를 호출합니다. 분리된 버퍼나 범위를 벗어난 뷰의 복사 결과는 빈 배열입니다.
- 독립 모드에는 기능 표에 있는 ECMAScript 내장 객체와 `console`, `print`가 있습니다. 브라우저 객체와 타이머는 제공하지 않습니다. Promise 작업은 최상위 호출 끝에서 실행되며, 다른 스레드에서 들어온 작업은 `runJobs()`로 처리할 수 있습니다.
- **오류**: 구문/런타임 오류와 `throw` 는 이름, 메시지, 스크립트 이름, 줄, 열을 가진 `JsScriptError` 로 던져지고, 제한/취소로 멈춘 스크립트는 `ScriptAbortedException` 입니다.
- **스레드**: 엔진과 핸들은 어느 스레드에서나 쓸 수 있고 연산은 한 번에 하나씩 실행되며, `cancel()` 만 잠금 없이 아무 스레드에서나 호출할 수 있습니다.
- 예제는 `examples/run.sh EngineExample`로 실행합니다. 실행 클래스패스에는 예제 클래스와 엔진 JAR를 둡니다.

엔진 JAR는 Java Scripting API(JSR 223)의 `META-INF/services/javax.script.ScriptEngineFactory`에 등록됩니다. `getLanguageVersion()` 및 `ScriptEngine.LANGUAGE_VERSION`은 대상 ECMA-262 판본인 `"17"`을 반환합니다.

```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("swingwebpanel-js");   // 고유한 이름
engine.put("x", 20);
Object r = engine.eval("x + 22");                    // Double 42.0
Invocable inv = (Invocable) engine;                  // Compilable 도 구현
```

- **이름 충돌에 주의하세요.** JRE 6~7 은 Rhino, JRE 8~14 는 Nashorn 을 `javascript`/`js`/`ECMAScript` 이름과 확장자 `js`, MIME `application/javascript` 로 이미 등록하고 있습니다. 이 엔진이 그 이름을 가로채면 기존 프로그램이 조용히 다른 엔진을 받게 되므로 **고유한 식별자만 등록합니다**: 이름 `swingwebpanel-js`(와 `swingwebpanel-javascript`), 확장자 `swjs`, MIME `application/x-swingwebpanel-javascript`. 일반 이름이 꼭 필요하면(예: 자체 엔진이 없는 Java 15 이상) JVM 옵션 `-Dswingwebpanel.js.genericNames=true` 로 일반 이름도 등록할 수 있습니다(`ScriptEngineManager` 를 만들기 전에 지정. 그때 `getEngineByName("javascript")` 가 어느 엔진을 돌려주는지는 JRE 의 검색 순서에 따릅니다).
- `ENGINE_SCOPE` 바인딩은 전역 객체의 살아 있는 보기입니다(`put`/`get`/`var`). `eval(script, bindings)` 로 다른 바인딩을 주면 그 바인딩마다 독립된 전역 스코프를 가집니다. `GLOBAL_SCOPE` 는 읽기 전용 대체 조회입니다. `print` 와 `console` 은 컨텍스트의 `getWriter()`/`getErrorWriter()` 로 갑니다. `ScriptException` 에는 파일 이름(`ScriptEngine.FILENAME`), 줄, 열이 있습니다. `THREADING` 은 `MULTITHREADED` 입니다.
- 스크립트가 Java 코드를 부르게 하려면 바인딩에 `JsHostFunction` 을 넣습니다(다른 Java 객체는 거부됩니다). 실행 제한과 취소는 `((JsScriptEngine) engine).getJsEngine()` 으로 얻은 `JsEngine` 에서 지정합니다.
- JSON 모듈을 쓰려면 `JsEngine.setModuleLoader`에 `ModuleLoader.WithAttributes` 구현을 등록합니다. `import('./data.json', { with: { type: 'json' } })`의 경로와 타입은 이 로더로 전달됩니다.
- 예제는 `examples/run.sh script.ScriptEngineExample`로 실행합니다.

### 저장소, 쿠키, 수명

- `sessionStorage` 는 컴포넌트 하나마다 메모리에 있고(오리진별), 컴포넌트가 수거되면 함께 사라집니다.
- `localStorage` 는 같은 JVM 프로세스의 컴포넌트가 공유하되 오리진별로 분리하며 임시 폴더의 프로세스별 파일에 저장합니다(자동 삭제는 OS 의 임시 파일 정리에 맡김). 파일이 사라지면 그냥 비어 있는 것으로 보고,
  파싱에 실패하면 초기화한 뒤 씁니다. `clean()` 은 이 공용 `localStorage` 를 비웁니다.

### 지원하지 않는 것(명시적 예외)

- **비디오 재생**, **3D 가속**, **WebGL/WebGPU**: `canvas.getContext('webgl' 등)` 은 `null` 을 반환하고 관련 전역 객체는 노출하지 않습니다. 2D 캔버스는 지원 대상입니다.
- `mailto:`, `javascript:`, `data:`, `jar:` 등 허용 목록에 없는 스킴, 외부 플러그인이 필요한 Flash·Java 애플릿 등의 기능, CSS 3D 변환, 이모지 렌더링 및 이모지 전용 정규식 속성은 지원하지 않습니다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | 요구사항, 주의사항, 단계별 개발 계획과 완료 체크 |
| [docs/API_CONTRACT.md](docs/API_CONTRACT.md) | 공개 메서드별 입력/반환/예외/상태 변화와 정책 |
| [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md) | 기능별 규격 판본, 구현 클래스, 검증 사례, 지원 상태와 제약 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 패키지 구성, 데이터 흐름, 캐시/수명/진행률 구조 |
| [docs/PROGRESS.md](docs/PROGRESS.md) | 진행 기록, 검증 결과, 미해결 사항, 다음 작업 |
| [docs/progress/netstorm.md](docs/progress/netstorm.md) | NetStorm 샘플 구동과 JDK별 계측 결과 |

## 제3자 자료와 고지

- **HRTF 자료(Web Audio `PannerNode` 의 HRTF 모드)**: MIT KEMAR 더미 헤드 측정 자료(compact 세트), Bill Gardner 와 Keith Martin, MIT Media Lab Machine Listening Group, 1994, <https://sound.media.mit.edu/resources/KEMAR.html>. Copyright 1994 by the MIT Media Laboratory. 자료를 쓸 때 저자를 표기하면 사용 제한 없이 무료로 쓸 수 있다는 조건으로 제공된 자료이며, 이 프로젝트는 그것을 `tools/hrtfgen` 으로 변환한 자원 `media/webaudio/dsp/kemar-compact.hrtf` 를 제품 JAR 에 넣습니다(원본 compact.zip 은 시험의 독립 기준으로 `browser/src/test/resources/kemar/` 에만 있습니다).
- **DynamicsCompressorNode**: Chromium(Blink)의 압축기 설계를 Java 로 옮겼으므로 해당 클래스(`DynamicsCompressorKernel.java`)에 Chromium 의 BSD 3-clause 고지를 그대로 유지합니다.

- **Public Suffix List**와 **Unicode 데이터**의 라이선스는 각 자료와 함께 배포합니다. [Public Suffix List 고지](browser/src/main/resources/org/duckdns/hjow/swingwebpanel/net/public_suffix_list.dat), [Unicode 라이선스](engine/src/main/resources/META-INF/LICENSE-unicode.txt)

## License

TLS 모듈과 데스크톱 앱은 CCADB에서 받은 Mozilla 루트 인증서 자료를 CDLA-Permissive-2.0으로 함께 배포합니다. [CCADB 출처 고지와 라이선스](tls13/src/main/resources/META-INF/LICENSE-ccadb.txt)

The bundled Public Suffix List is distributed under the Mozilla Public License 2.0. Its original notice and revision are preserved in the data file. [Public Suffix List](https://publicsuffix.org/list/)

Copyright 2026 HJOW

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
