# CONTENT-P17 작업 기록 (MIME 분류 P17-01, 일반 텍스트 P17-02, XHTML P17-05, 오류 페이지 P17-06)

작업자: CONTENT-P17. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/content/**`(markdown 제외), `tests/src/org/duckdns/hjow/swingwebpanel/content/**`(markdown 제외), `tests/resources/content/**`, 이 문서.
core 는 건드리지 않았다. 아래 "조정자가 core 에서 할 일" 에 연결 방법을 적었다.

## 2026-11-16 · 콘텐츠 계층의 순수 부분 (P17-01, P17-02, P17-05, P17-06)

현재 작업 번호: P17-01 / P17-02 / P17-05 / P17-06 의 콘텐츠 계층 구현과 시험(컴포넌트 연결 전)
다음 작업 번호: 조정자의 core 연결(아래), 그 뒤 P17-04/P17-07/P17-08 의 통합 확인

### 완료한 내용

| 항목 | 클래스 | 내용 |
| --- | --- | --- |
| P17-01 | `content.ContentType` | WHATWG MIME Sniffing "parse a MIME type": type/subtype 소문자, 모든 매개변수(순서 유지, 같은 이름은 첫 것), 따옴표 값과 백슬래시 이스케이프, 공백/대소문자 규칙, 깨진 값은 `isValid()==false` + `getMime()==""`(= 타입 없음). `getSuffix()`, `isXml()`, `isJson()`, `toString()`(정규 형태) |
| P17-01 | `ContentKind`, `ContentDecision`, `ContentClassifier` | 응답/`setContent` 의 처리기 판정: HTML, XHTML, PLAIN_TEXT, MARKDOWN, IMAGE, AUDIO, VIDEO(대체 표시), DOWNLOAD. `classifyResponse(header, attachment, head)`, `classifySetContent(mime)`/`requireSetContentMime`(IAE), `kindOfMime`, `sniff(byte[])` |
| P17-02 | `content.text.PlainTextDocumentBuilder` | 텍스트를 `html>head+body>pre[style="word-wrap: break-word; white-space: pre-wrap;"]>텍스트노드 하나` 로. 텍스트는 어떤 것도 해석하지 않는다. CRLF/CR→LF, NUL→U+FFFD(HTML 표준의 텍스트 파일 입력 전처리). 문서 contentType 은 `text/plain`(또는 지정한 타입) |
| P17-05 | `content.xhtml.XhtmlParser`(+`XhtmlBuilder`, `XhtmlEntities`, `XmlProlog`, `XmlDecoder`, `XmlText`, `XhtmlLimits`, `XhtmlResult`, `XmlParseError`) | `application/xhtml+xml` → 자체 DOM (아래 정책) |
| P17-06 | `content.error.ErrorPageBuilder`(+`ErrorPage`, `ErrorKind`) | HTTP 4xx/5xx, DNS/연결/시간 초과/TLS/리디렉션/크기/잘림, 차단 스킴(400), classpath 누락(404), FTP 실패(404), 잘못된 주소, CORS, XML 오류의 HTML 원문 + 사실들. 모든 바깥 값 이스케이프 |

### 정책(문서화한 결정)

**MIME 분류 (P17-01 의 "없거나 미지원일 때의 표시 정책")**

- `Content-Disposition: attachment` → 항상 다운로드(`Reason.ATTACHMENT`). 타입이 없어도 마찬가지.
- 없거나 깨진 `Content-Type` 은 오류가 아니다. 앞 1445 바이트로 판별한다(`ContentClassifier.sniff`; WHATWG "unknown MIME type" 알고리즘의 축소판: HTML 태그 17종 패턴, `<?xml`,
  PDF/PS, BOM, GIF/PNG/JPEG/BMP/WebP/ICO/AVIF, WAVE/AIFF/AU/MP3(ID3)/Ogg/FLAC/MIDI/MP4/M4A/WebM, gzip/ZIP/RAR 시그니처, 마지막으로 제어 바이트가 있으면 `application/octet-stream`,
  없으면 `text/plain`). UTF-8 BOM 뒤의 HTML 도 HTML 로 본다.
- `text/html`=HTML, `application/xhtml+xml`=XHTML, `text/markdown`·`text/x-markdown`=Markdown, `text/plain`=PLAIN_TEXT.
- 그 밖의 `text/*`, JavaScript 타입, `application/json`/`+json`, `application/xml`/`+xml`(RSS, Atom 포함)은 **텍스트로 소스를 보여 준다**(PLAIN_TEXT).
  단 Chromium 이 텍스트로 보이지 않는 `text/*` 목록(`kUnsupportedTextTypes`: calendar, vcard, csv, rtf, tsv 등 23종)은 다운로드이다.
  출처: `https://raw.githubusercontent.com/chromium/chromium/main/third_party/blink/common/mime_util/mime_util.cc` (조회일 2026-11-16, main 의 당시 판. 목록과 "text/* 는 지원, 목록은 제외" 규칙을
  우리 표로 옮겼을 뿐 C++ 코드를 옮기지 않았다).
- 루트 요소가 XHTML 네임스페이스의 `html` 인 `application/xml`/`text/xml` 본문은 XHTML 이다(브라우저와 같다). 본문 앞부분(DOCTYPE 뒤 루트까지)이 필요하다.
- `image/*`(svg 포함)=IMAGE, `audio/*`·`application/ogg`=AUDIO, `video/*`=VIDEO(미지원 비디오 대체 표시). 그 밖은 모두 DOWNLOAD.
- `setContent` 의 MIME: 매개변수 제거 + 대소문자 무시 후 `text/html`, `text/plain`, `text/markdown`, `application/xhtml+xml` 만 허용. null/빈 값/그 밖은 `IllegalArgumentException`(다운로드 아님). `text/x-markdown` 은 `setContent` 에서는 거부한다(API 계약 5.10 의 네 가지 문구 그대로).

**일반 텍스트 (P17-02)**: Chrome 과 같은 모양(`pre` 의 `white-space: pre-wrap; word-wrap: break-word`)이라 긴 줄이 창 너비에서 줄바꿈된다. `<title>` 없음. 원문(`getContent()`)은 변환하지 않는다.
`text/css` 등 다른 텍스트 타입도 같은 빌더를 쓰며 `document.contentType` 만 다르다.

**XHTML (P17-05)**

- Java 표준 XML 파서(`javax.xml.parsers.SAXParserFactory`, JRE 6 의 JAXP)를 공개 API 로만 쓰고 자체 DOM 으로 변환한다. 오류 복구 없음: 첫 well-formed 오류가 `XhtmlResult.getError()`(`XmlParseError`: 종류, 메시지, 줄, 열, 소스 줄)가 되며
  HTML 파서로 되돌아가지 않는다. 부분 트리는 `getDocument()` 에 남지만 쓰지 않는 것이 규칙이다.
- **텍스트 밖은 읽지 않는다.** 4중 방어: (1) DOCTYPE 의 외부 식별자(PUBLIC/SYSTEM)를 파서가 보기 전에 텍스트에서 제거한다(`XmlProlog` 로 DOCTYPE 을 읽고 내부 서브셋만 남긴 DOCTYPE 으로 다시 씀. DOM 의 DOCTYPE 노드는 원래 식별자를
  유지하고 줄 수는 그대로 맞춘다. 줄/열 보정 있음), (2) `external-general-entities`/`external-parameter-entities`/`load-external-dtd` 기능 끔, (3) XInclude 끔, (4) 엔티티 해석기는 무엇이 와도 빈 소스를 주고 기록한다.
  선언되지 않았거나 외부인 엔티티 참조는 파서가 조용히 버리므로(JDK 6/8 확인) `DeclHandler`+`LexicalHandler` 로 직접 검출해 **오류**로 만든다.
  문서가 지정한 외부 식별자는 `XhtmlResult.getRefusedReferences()` 에 기록된다(읽지는 않음).
- 이름 있는 참조(`&nbsp;`): DOCTYPE 이 XHTML/HTML/MathML/SVG 의 알려진 W3C DTD 이면(공개 식별자 `-//W3C//DTD XHTML|HTML|MathML|SVG...` 또는 www.w3.org 의 `xhtml*/html*/mathml*/svg*` 시스템 식별자)
  문서에 나오는 HTML 문자 참조 표(`html.NamedCharacterReferences`)의 이름을 내부 엔티티로 선언해 준다(XHTML 1.0 의 엔티티 파일 셋의 상위 집합. 문서가 직접 선언한 것이 우선). 그 밖의 문서는 미리 정의된 5개 외의 이름이 오류이다.
- 한도(`XhtmlLimits`): 입력 16 Mi 문자, 깊이 512, 노드 100 만, 요소당 속성 4096, 펼친 텍스트 32 Mi 문자, 엔티티 참조 50 만, 시간 30 초. 초과는 `LIMIT_EXCEEDED`. JDK 의 기본 한도(JDK 6u45 는 엔티티 6만 4천 번)는
  `secure-processing=false` 로 풀어 두었다(`&nbsp;` 가 10 만 번 있는 페이지가 열려야 하므로). JDK 8+ 에서는 `jdk.xml` 속성도 풀어 보며 모르는 JRE 는 건너뛴다.
- 인코딩(`XmlDecoder`): BOM > HTTP charset(알려진 라벨) > BOM 없는 UTF-16 자동 판별 > XML 선언 `encoding` > UTF-8. 라벨은 Encoding 표준 표(`html.EncodingLabels`, `euc-kr`/`windows-949` 등). 잘못된 바이트는 U+FFFD 로 바꾸지 않고
  **ENCODING 오류**(줄/열)이다. ASCII 호환 바이트에 UTF-16 선언이 붙으면 UTF-8 로 본다. 문자열 입력(`setContent`)은 선언의 인코딩을 무시한다.
- DOM: `Document.createXmlDocument("application/xhtml+xml")`(HTML 문서 아님: 이름은 대소문자 구분), no-quirks, 요소는 `Document.createElementInternal(ns, prefix, local)` — HTML 파서와 같은 `Element`.
  속성은 `setAttributeNS`(xmlns 선언은 XMLNS 네임스페이스 속성). CDATA→`CDATASection`, PI→`ProcessingInstruction`(`xml-stylesheet` 포함, 해석은 안 함), 주석, DOCTYPE 노드. XML 선언은 노드가 아님.
  `script` 는 `ParserNodeData.scriptFlags(...).setParserInserted(true)`, `template` 의 자식은 template contents 로.
  HTML 파서와 같은 마크업의 트리가 같음을 시험으로 확인(`testSameTreeAsHtmlParser`).
- 리스너: `XhtmlParser.setListener(ParserListener)` — HTML 파서와 같은 통지(`documentModeDetermined`, `elementInserted/Closed`, `scriptReady`, `styleReady`, `stylesheetFound`, `imageFound`, `baseHrefChanged`, `metaRefresh`, `titleReady`)를
  문서 순서로 호출한다. 단 XML 해석은 중단할 수 없으므로 `ParserControl.pause()` 와 `insertAtInsertionPoint` 는 무시된다(스크립트가 해석을 붙잡지 못한다. 외부 스크립트는 해석이 끝난 뒤 처리해야 한다).

**오류 페이지 (P17-06)**: `<title>` = 제목줄(`404 Not Found`), `h1`, 설명, 주소(다시 시도 링크 `<a href>`, 읽을 수 있는 스킴이고 공백/제어 문자가 없고 8192 자 이하일 때만. 아니면 텍스트만),
상세는 `pre.swp-detail`(XML 오류의 줄 번호와 소스 줄과 `---^`). 모든 바깥 값은 `ErrorPageBuilder.escape`(`& < > " '`, 제어 문자, C1, 양방향 서식 문자는 U+FFFD)로 이스케이프. 표시하는 주소는 2000 자에서 자른다.
제목/설명은 `core.ErrorPages.describe` 와 글자 그대로 같다("Connection failed" 등; `NetworkIntegrationTest` 가 그 문자열을 검사한다). 차단 스킴은 상태 400, 없는 classpath/FTP/NOT_FOUND 는 404.
`body` 에 `class="swp-error swp-error-<kind>"`.

### 변경 파일

`src/org/duckdns/hjow/swingwebpanel/content/`: `ContentType.java`, `ContentKind.java`, `ContentDecision.java`, `ContentClassifier.java`;
`content/text/`: `PlainTextDocumentBuilder.java`, `package-info.java`;
`content/xhtml/`: `XhtmlParser.java`, `XhtmlBuilder.java`, `XhtmlEntities.java`, `XhtmlLimits.java`, `XhtmlResult.java`, `XmlDecoder.java`, `XmlText.java`, `XmlParseError.java`, `XmlProlog.java`, `package-info.java`;
`content/error/`: `ErrorPageBuilder.java`, `ErrorPage.java`, `ErrorKind.java`, `package-info.java`;
`tests/src/org/duckdns/hjow/swingwebpanel/content/`: `ContentTypeTest.java`, `ContentClassifierTest.java`, `ContentDocumentsStyleTest.java`;
`content/text/PlainTextDocumentBuilderTest.java`; `content/xhtml/`: `XhtmlParserTest.java`, `XhtmlSecurityTest.java`, `XmlDecoderTest.java`, `XmlPrologTest.java`, `XhtmlTreeDump.java`(보조); `content/error/ErrorPageBuilderTest.java`.

### 검증 결과

- JDK 6u45 (`C:\Program Files\Java\jdk1.6.0_45`): `scripts/pkgtest.sh cont "content html dom net css" "content" --filter org.duckdns.hjow.swingwebpanel.content.` → **80 passed / 0 failed / 0 skipped**
  (이 작업의 시험 62건 + 기존 Markdown 시험 18건). 시험 62건: `ContentTypeTest` 6, `ContentClassifierTest` 7, `ContentDocumentsStyleTest` 3, `PlainTextDocumentBuilderTest` 5, `XhtmlParserTest` 12, `XhtmlSecurityTest` 9,
  `XmlDecoderTest` 6, `XmlPrologTest` 4, `ErrorPageBuilderTest` 10. 마지막 수정(`XhtmlParser` 의 JAXP 속성 값만 바뀜) 뒤에는 XHTML 패키지 31건을 다시 돌렸다: `... --filter org.duckdns.hjow.swingwebpanel.content.x` → JDK 6 31/0/0.
- 보조(JRE 6 검증이 아님): 같은 클래스 파일을 JDK 8u202 와 Zulu 25 에서 실행해도 XHTML 31건 통과(JDK 8 은 속성 값 때문에 한 번 실패해서 `Integer.MAX_VALUE` 로 고침. 수정 뒤 JDK 6/8/25 모두 31/0/0). 전체 80건도 수정 전 코드로 JDK 6 80/0, JDK 25 80/0 이었다.
- 린트: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/content tests/src/org/duckdns/hjow/swingwebpanel/content` → 0 위반(46 파일).
- 시험이 실제로 증명하는 것:
  - **외부 접근 차단**: 로컬 `TestHttpServer` 의 요청 카운터가 0 임을 확인(외부 DTD `SYSTEM`/`PUBLIC`, 외부 일반 엔티티, 외부 매개변수 엔티티, 매개변수 엔티티로 원격 DTD 조각을 읽는 blind XXE, XInclude).
    `file:` 표식 파일(`TOPSECRET_LOCAL_FILE`)과 로컬 DTD 파일(`LOADED_FROM_DTD_FILE`)이 DOM/오류에 없음. 해석기가 모든 요청에 빈 소스를 주고 기록함(`XhtmlBuilder.resolveEntity` 직접 호출).
  - **폭탄**: billion-laughs(9단계×10) → `LIMIT_EXCEEDED`(1초 안), 빈 엔티티 2^30 번 확장 → `LIMIT_EXCEEDED`, 5000자 엔티티를 2만 번 → `LIMIT_EXCEEDED`. 깊이/노드/속성/입력 크기/텍스트/엔티티 수/시간(1 ms) 한도 각각 확인.
    `&nbsp;` 10 만 개 페이지는 정상 해석(JDK 기본 한도를 푼 것을 확인).
  - **구조화 오류**: 잘못된 문서 21종(줄 번호 확인), DOCTYPE 을 다시 쓴 뒤에도 원문 기준 줄/열, 한 줄에 DOCTYPE 이 있는 경우의 열 보정, 인코딩 오류는 `ENCODING` 종류와 줄/열.
  - **인코딩**: BOM(UTF-8/16LE/16BE), BOM 없는 UTF-16 자동 판별, XML 선언의 EUC-KR/ISO-8859-1, HTTP charset 이 선언보다 우선, 모르는 라벨 무시, 기본 UTF-8, 잘못된 UTF-8/EUC-KR 은 U+FFFD 가 아니라 오류.
  - **DOM 동일성**: 같은 마크업을 HTML 파서와 XHTML 파서로 만든 트리가 xmlns 속성을 뺀 덤프에서 같음, script 플래그, template contents, 리스너 통지 37개 이벤트의 순서.
  - **CSS 연결**: XHTML 문서의 요소가 HTML 과 같은 사용자 에이전트 스타일(h1/p 블록, head/style/title 숨김, pre, body margin, h1 크기)을 받고 `style` 요소 규칙이 적용되며 대문자 `P` 는 받지 않음, 일반 텍스트 `pre` 는 `white-space: pre-wrap`/`overflow-wrap: break-word`, 오류 페이지 스타일 수집.
  - **오류 페이지 이스케이프**: 적대적인 주소/이유 문구/상세(`"><script>`, `onerror`, `</title>`)를 HTML 파서로 다시 해석해 script/img/이벤트 속성 요소가 없고 href 가 주소와 같음. 차단 스킴/공백/제어 문자/8192자 초과 주소는 링크 없음.

### 조정자가 core 에서 할 일 (core 는 건드리지 않았음)

1. **오류 페이지**: `core.ErrorPages` 를 `ErrorPageBuilder` 로 바꾼다.
   - `ErrorPages.page(url, status, title, message)` → `ErrorPage p = ErrorPageBuilder.build(ErrorKind.X, url, status, title, message, "")`
   - `NetworkPageLoader.toSnapshot` 의 `ErrorPages.describe(failure, msg)`+`page(...)` → `ErrorPageBuilder.forFailure(r.getFailureKind(), url, r.getFailureMessage())` (NOT_FOUND/FTP_FAILURE 는 404, BLOCKED_SCHEME 은 400 이 자동).
   - 본문 없는 HTTP 오류 → `ErrorPageBuilder.http(finalUrl, status, r.getReason())`.
   - 스냅샷: `PageSnapshot.error(url, p.getHtml(), p.getMimeType(), p.getStatus()).withTitle(p.getHeading())`.
   - `ErrorPages.reasonOf(int)` 는 `ErrorPageBuilder.reasonPhrase(int)`. 클래스패스 누락은 `ErrorPageBuilder.classpathMissing(url)`, 차단 스킴은 `blockedScheme(url)`.
2. **응답 분류**: `NetworkPageLoader.isDisplayable/sniff/decodeBody/reasonFor` 를 `ContentClassifier.classifyResponse(contentTypeHeader, head.isAttachment(), firstBytes(≥ 4096))` 로 대체한다.
   `decision.getKind()` 로 HTML/XHTML/PLAIN_TEXT/MARKDOWN → 텍스트 로더, IMAGE/AUDIO/VIDEO → 감싸기/대체 문서, DOWNLOAD → `DownloadRequiredException(reason 은 decision.getReason())`.
   스냅샷의 MIME 은 `decision.getDisplayMime()` 를 쓰면 `ParsingPageLoader.attach` 의 정확 일치 분기를 그대로 쓸 수 있다(text/css → text/plain).
   `BrowserSession.CONTENT_TYPES`/`setContent` 검증은 `ContentClassifier.requireSetContentMime(mime)` 로 바꾼다(같은 규칙. 반환값이 정규화된 MIME).
3. **일반 텍스트**: `ParsingPageLoader.plainTextDocument` 를 `PlainTextDocumentBuilder.build(text, url, mime)` 로 바꾸고 반환된 문서에 `HtmlMarkupSupport`/`CssSelectorEngine.install` 만 설치한다
   (`populate(Document, text)` 로 미리 서비스를 설치한 문서를 채워도 된다).
4. **XHTML**: `ParsingPageLoader.attach` 에서 `application/xhtml+xml`:
   ```java
   XhtmlParser p = new XhtmlParser();                       // 또는 new XhtmlParser(preparedXmlDocument)
   Document doc = p.getDocument();  doc.setMarkupSupport(new HtmlMarkupSupport());  CssSelectorEngine.install(doc);
   p.setListener(<HTML 과 같은 ParserListenerAdapter: styleReady/stylesheetFound/titleReady/scriptReady>);
   XhtmlResult r = p.parse(bytesOrNull != null ? p.parse(bytes, charsetLabel, url) : p.parse(sourceText, url));
   if (!r.isWellFormed()) → ErrorPage ep = ErrorPageBuilder.xmlParseError(url, r.getError());   // 표시는 ep.getHtml() 을 일반 HTML 경로로, getContent() 는 r.getSource()(수신 원문)
   ```
   - 네트워크 본문의 디코딩은 `NetworkPageLoader.decodeBody` 의 `EncodingSniffer`(HTML 규칙) 대신 `XmlDecoder.decode(body, charset)` 를 쓴다(`XmlText.getText()` 가 수신 원문. `hasError()` 이면 `ENCODING` 오류).
     `XhtmlParser.parse(byte[], charset, url)` 는 내부에서 같은 디코딩을 하므로, 원문을 `getContent()` 로 보관하려면 `r.getSource()` 를 쓰면 된다.
   - `scriptReady` 에서 외부 스크립트가 `pause()` 를 호출해도 XHTML 해석은 멈추지 않는다 → 외부 스크립트를 해석 뒤에 실행하는 경로(defer 와 같은 취급)가 필요하다(PageScripts 쪽 협의).
   - XHTML 문서의 `innerHTML`/`outerHTML` 은 지금 HTML 구문(`HtmlMarkupSupport`)을 쓴다. XML 구문(XMLSerializer, XML 조각 파싱)은 P20-04 에서 필요하다.
5. `docs/FEATURE_MATRIX.md`: text/plain·XHTML·오류 페이지·MIME 분류 행을 "부분 구현(콘텐츠 계층 검증 완료, 컴포넌트 연결 전)" 으로. `docs/ARCHITECTURE.md` 의 `content.ContentHandler`/`content.XhtmlParser` 표기는
   실제 클래스(`content.ContentClassifier`, `content.xhtml.XhtmlParser`)로.

### 확인하지 못한 것 / 한계 (정직한 기록)

- core 에 연결하지 않았으므로 실제 컴포넌트에서 XHTML 페이지를 열어 보는 통합, `getContent()` 원문 보존, 이력 복원, 오류 페이지 화면 표시는 확인하지 못했다. 스타일 시험(`ContentDocumentsStyleTest`)이 CSS 엔진까지의 경로만 확인한다(레이아웃/그리기는 확인하지 않음).
- 속성 값 안의 선언되지 않은 엔티티 참조: XML 파서가 조용히 버리는 경우가 있으나, 선언 검출은 콘텐츠의 참조에만 동작한다. 외부 DTD 를 가진 문서는 DOCTYPE 다시 쓰기 덕분에 속성에서도 오류가 되지만,
  내부 서브셋에 매개변수 엔티티 참조(`%name;`)가 있고 그 선언이 외부에 있는 드문 문서는 속성 안의 미선언 엔티티가 조용히 사라질 수 있다.
- `xml-stylesheet` 처리 명령은 DOM 노드로만 남기고 스타일시트로 해석하지 않는다(CSS 계층 몫). `xml:base`/`xml:space` 는 해석하지 않는다.
- JRE 6 의 JAXP 메시지는 로케일에 따라 번역되어 `XmlParseError.getMessage()` 는 영어가 아닐 수 있다(JDK 8 에서 한국어 확인). 시험은 메시지 글자에 의존하지 않는다.
- JRE 6 에서만 검증했다. 보조로 JDK 8(30건 통과)과 JDK 25 에서 같은 시험을 돌려 JDK 25 의 기본 한도(깊이 100, 엔티티 2500)를 푸는 속성을 추가했다(JDK 6 이외의 결과는 JRE 6 검증이 아니다).
- `ContentClassifier.sniff` 의 "서버가 바이너리를 `text/plain` 으로 보낸 경우(text or binary)"와 `X-Content-Type-Options: nosniff` 는 구현하지 않았다.
- P18-01 의 이미지/오디오 형식 판별(시그니처로 형식 확인)은 하지 않았다. 여기의 시그니처는 타입이 아예 없는 응답을 IMAGE/AUDIO/DOWNLOAD 로 보내는 용도뿐이다.

### 체크박스 후보

콘텐츠 계층에서 구현과 시험이 끝난 부분(전체 항목은 core 연결이 있어야 체크할 수 있어 **P17-0x 체크박스는 아직 후보가 아님**):

- P17-01: MIME/Content-Type 값 해석기, 분류, `setContent` MIME 검사 — 연결 부분(네트워크 응답과 `setContent` 가 같은 경로를 쓰는 것)이 남음.
- P17-02: 일반 텍스트 DOM 빌더 — 화면 표시(레이아웃)와 스크립트 미실행의 컴포넌트 수준 확인이 남음.
- P17-05: XHTML 파서 전체(구문, 네임스페이스, 대소문자, 인코딩, 엔티티, 보안, 한도, 구조화 오류) — `application/xhtml+xml` 의 컴포넌트 연결(원문, 오류 화면, 스크립트)이 남음.
- P17-06: 오류 페이지 생성기 — 4xx/5xx/DNS/연결/차단 스킴/classpath/FTP 의 실제 흐름에서 이 생성기를 쓰는 연결(`core.ErrorPages` 대체)이 남음.
