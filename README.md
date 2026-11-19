# SwingWebPanel


## 소개

Java 의 Swing 용 웹 브라우저 컴포넌트입니다.
일부 에셋을 제외하면 순수 자바로 구현되어 있습니다.
Java 6 이상 버전을 모두 지원합니다.
    
우리 프로젝트에서는 AI를 적극 사용합니다.

## 소개 (AI 작성)

Java Swing 에서 쓰는 웹 브라우저 컴포넌트입니다. HTML 파서, CSS 처리기, 레이아웃/렌더링 엔진, JavaScript 엔진(ES2015 대상), DOM, 이미지/오디오 디코더, HTTP/HTTPS/FTP 요청 계층을
**순수 Java 로 직접 구현**했고 외부 라이브러리, 기존 JavaScript 엔진(Rhino 등), JavaFX WebView, 네이티브 코드(JNI 등), OS 브라우저 실행에 의존하지 않습니다.

> **현재 상태: 부분 구현입니다.** HTML5/CSS3/ES6 전체를 지원한다고 주장하지 않습니다. 기능별 지원 상태와 근거, 남은 차이는 [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md),
> 진행과 검증 결과는 [docs/PROGRESS.md](docs/PROGRESS.md), 계획은 [AGENTS.md](AGENTS.md) 의 "개발 계획"에 있습니다.

## 요구 사항

- Java 6 이상. 제품 코드는 Java 6 문법/API 만 쓰며 JRE 6 에서 검증합니다(`C:\Program Files\Java\jdk1.6.0_45`, 다른 경로는 환경 변수 `JAVA6_HOME`).
- JRE 6u45 의 JSSE 는 TLS 1.2 와 클라이언트 SNI 를 지원하지 않아 현대 HTTPS 사이트에 연결하지 못할 수 있습니다. 그 경우 HTTPS 의 연결/TLS 실패에 한해 같은 주소로 HTTP 접근을 한 번 시도합니다
  (더 새로운 JRE 에서는 그 JRE 가 지원하는 프로토콜을 씁니다).

## 빌드와 시험

| 하는 일 | 명령(sh) | 명령(Windows) |
| --- | --- | --- |
| 제품 JAR 만들기(`build/swingwebpanel.jar`) | `./build.sh` | `build.bat` |
| 전체 시험 | `./test.sh` | `test.bat` |
| 예제 실행 | `examples/run.sh BlankPageExample [URL]` | `examples\run.bat BlankPageExample [URL]` |

JDK 6 를 찾지 못하면 다른 JDK 로 조용히 대체하지 않고 오류로 종료합니다(다른 JDK 는 `SWP_ALLOW_NON_JDK6=1` 로 명시했을 때만 쓰며 그 결과는 JRE 6 검증이 아닙니다).
예제는 [examples/README.md](examples/README.md) 를 보세요.

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
| `setPage(String url)` / `getPage()` | 주소로 이동(비동기)/현재 확정된 주소. 허용 스킴은 `http://`, `https://`, `ftp://`(익명)이고 `classpath://` 는 클래스패스의 HTML 을, `about:blank` 는 빈 페이지를 연다. `mailto:`, `file:`, `data:`, `javascript:` 등은 차단한다. `setContent` 로 채웠으면 `getPage()` 는 `about:text`. |
| `setContent(String content, String contentType)` / `getContent()` | `text/html`, `text/plain`, `text/markdown`, `application/xhtml+xml` 를 지원한다. `getContent()` 는 **화면에 보이는 내용이 아니라 받은 원문**(스크립트가 DOM 을 바꿔도, Markdown 이 HTML 로 변환돼도 원문 그대로)을 돌려준다. |
| `back()`, `back(int n)`, `forward()`, `forward(int n)`, `getHistories()` | 이력 이동(정확히 n 칸 갈 수 없으면 아무 일도 하지 않음)과 이력 URL 의 `Vector<String>` 복사본. |
| `refresh()`, `refresh(boolean force)` | 새로고침. `force` 는 이 컴포넌트가 읽는 주 문서와 하위 리소스의 캐시를 우회한다. |
| `setUserAgent(String)` / `getUserAgent()` | 기본값 `swingwebpanel/1.0`. |
| `addHeaders(Map<String,String>)` / `getHeaders()` | 모든 요청에 붙는 추가 헤더(이름은 대소문자 무시, 기존 값을 덮어씀, `User-Agent` 는 받지 않고 조회에서도 제외). |
| `getCookies()` | 이 컴포넌트의 만료되지 않은 쿠키 전부(`name=value; name=value`). 쿠키 저장소는 컴포넌트별이다. |
| `setJavascriptEnabled(boolean)` / `isJavascriptEnabled()` | 기본 켜짐. 끄면 스크립트, 인라인 핸들러, 타이머가 멈추고 HTML/CSS/링크/폼은 계속 동작한다. 다시 켜도 이미 중단한 페이지의 스크립트를 되살리지 않으며 다음 이동부터 실행한다. |
| `getStatusText()`, `getProgress()` | 상태바 메시지와 진행률(아래 표). |
| `addHyperlinkListener`, `removeHyperlinkListener`, `removeHyperlinkEvents()` | `javax.swing.event.HyperlinkListener` 로 링크 진입/이탈/활성화를 받는다. 통지를 모두 제거해도 링크 이동 자체는 계속 동작한다. |
| `getLocalStorage()`, `getSessionStorage()` | Web Storage 항목의 읽기 전용 복사본(`StorageEntry`: origin, key, value). |
| `clean()` | 진행 중인 작업을 취소하고 `about:blank` 로 돌아가며 캐시, sessionStorage, **프로세스 공용 localStorage 전체**를 비운다(다른 컴포넌트도 영향을 받는다). User-Agent, 추가 헤더, 쿠키, JavaScript 설정, 사용자 리스너는 유지한다. |
| `dispose()` | `clean()` 후 이 컴포넌트에 등록된 모든 사용자 리스너와 내부 자원(타이머, 하위 작업, 미디어, 다운로드)을 해제한다. 반복 호출은 안전하다. 이후 변경 메서드는 `IllegalStateException`, 조회는 빈 상태를 돌려준다. |
| 그 밖 | `getTitle()`, `getMimeType()`, `getHistoryIndex()`, `stop()`, `isDisposed()` |

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

### HTTPS 와 SSL 재정의

HTTPS 는 JRE 의 기본 신뢰 저장소와 호스트 이름 검증을 씁니다(JVM 전역 SSL 설정은 바꾸지 않습니다). 연결마다 호출되는 아래 메서드를 **재정의**하면 그 연결에만 신뢰 저장소나 호스트 검증을 바꿀 수 있습니다.

```java
SwingWebPanel panel = new SwingWebPanel() {
    protected void configureSsl(HttpsURLConnection connection) throws IOException {
        super.configureSsl(connection);           // 기본: 이 JRE 가 지원하는 프로토콜 선택
        connection.setSSLSocketFactory(myFactory); // 이 컴포넌트의 이 연결에만 적용
    }
};
```

HTTPS 의 연결/TLS 처리 실패일 때만 같은 주소의 HTTP 접근을 한 번 시도합니다(HTTP 404/500 응답은 실패로 보지 않습니다). 이때 Secure 쿠키는 보내지 않습니다.

페이지의 `XMLHttpRequest`/`fetch` 는 컴포넌트와 같은 요청 계층(User-Agent, 추가 헤더, 쿠키, SSL 재정의, 스킴 제한)을 지나며 교차 오리진 요청은 CORS 를 따릅니다: 단순하지 않은 요청은 `OPTIONS` 사전 요청으로 서버의 허락을 먼저 받습니다. JRE 의 `HttpURLConnection` 은 `Origin` 같은 제한 헤더를 버리므로 그 헤더를 실어야 하는 요청(교차 오리진 요청, 사전 요청, 오리진이 있는 POST/PUT/PATCH/DELETE)은 소켓 기반 연결로 보내며 위의 `configureSsl` 재정의가 그 연결에도 똑같이 적용됩니다. 단 JVM 프록시(`http.proxyHost` 등)가 적용되는 환경에서는 소켓 연결이 프록시를 쓰지 못하므로 JRE 연결을 쓰고 그 헤더는 사라집니다.

### 파일 다운로드

링크가 `Content-Disposition: attachment` 응답이나 보여 줄 수 없는 형식(zip 등)을 가리키거나 같은 오리진의 `<a download>` 이면 페이지를 바꾸지 않고 **Swing 다운로드 팝업**(`JDialog` 와 `JFileChooser`)이
열립니다. 파일 이름/출처/저장 위치/진행률(크기를 모르면 불확정)/취소/결과를 보여 주며, 전송은 백그라운드 스레드에서 하고 컴포넌트와 같은 요청 계층(User-Agent, 헤더, 쿠키, SSL 확장 지점)을 씁니다.
헤드리스 환경에서는 다운로드를 취소하고 상태 메시지로 알립니다. 호스트가 직접 UI 를 만들려면 `protected DownloadHandle createDownloadPopup(DownloadRequest, DownloadControl)` 을,
저장 위치를 정하려면 `protected File getDownloadDirectory()` 를 재정의합니다(예: [DownloadPopupExample](examples/src/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java)).

### 오디오와 비디오

`<audio>` 는 PCM/WAV/AIFF/AU/MP3/AAC/FLAC/Ogg Vorbis/Opus(WebM 안의 Vorbis·Opus 포함)를 순수 Java 디코더로 재생하며 Java Sound 로 출력합니다(`protected AudioOutputFactory createAudioOutputFactory()` 를 재정의해
출력을 바꿀 수 있고, 장치가 없으면 `error` 이벤트가 됩니다). `<video>` 는 **재생하지 않으며** 요소의 자리에 미지원 비디오임을 표시하는 것이 목표입니다(`canPlayType` 은 항상 `""`).
이미지/오디오 주소에 접근하지 못하면 그 자리에 미지원 URL 과 `alt` 가 표시됩니다.

### 저장소, 쿠키, 수명

- `sessionStorage` 는 컴포넌트 하나마다 메모리에 있고(오리진별), 컴포넌트가 수거되면 함께 사라집니다.
- `localStorage` 는 같은 JVM 프로세스의 컴포넌트가 공유하되 오리진별로 분리하며 임시 폴더의 프로세스별 파일에 저장합니다(자동 삭제는 OS 의 임시 파일 정리에 맡김). 파일이 사라지면 그냥 비어 있는 것으로 보고,
  파싱에 실패하면 초기화한 뒤 씁니다. `clean()` 은 이 공용 `localStorage` 를 비웁니다.

### 지원하지 않는 것(명시적 예외)

- **비디오 재생**, **3D 가속**, **WebGL/WebGPU**: `canvas.getContext('webgl' 등)` 은 `null` 을 반환하고 관련 전역 객체는 노출하지 않습니다. 2D 캔버스는 지원 대상입니다.
- `http://`, `https://`, `ftp://` 외 스킴(`mailto:` 등), 외부 플러그인이 필요한 기능(Flash, Java 애플릿 등: 대체 내용을 표시), 3D 변환.

## 문서

| 문서 | 내용 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | 요구사항, 주의사항, 단계별 개발 계획과 완료 체크 |
| [docs/API_CONTRACT.md](docs/API_CONTRACT.md) | 공개 메서드별 입력/반환/예외/상태 변화와 정책 |
| [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md) | 기능별 규격 판본, 구현 클래스, 검증 사례, 지원 상태와 제약 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 패키지 구성, 데이터 흐름, 캐시/수명/진행률 구조 |
| [docs/PROGRESS.md](docs/PROGRESS.md) | 진행 기록, 검증 결과, 미해결 사항, 다음 작업 |

## License

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