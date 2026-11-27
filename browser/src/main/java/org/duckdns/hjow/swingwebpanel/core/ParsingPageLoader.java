package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.content.error.ErrorPage;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPageBuilder;
import org.duckdns.hjow.swingwebpanel.content.markdown.MarkdownConverter;
import org.duckdns.hjow.swingwebpanel.content.text.PlainTextDocumentBuilder;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlResult;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.html.ParserListenerAdapter;
import org.duckdns.hjow.swingwebpanel.html.ParserControl;
import org.duckdns.hjow.swingwebpanel.html.ParserListener;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * A page loader that completes the snapshots of another loader with a DOM tree (plan items P07, P17-02, P17-04): <code>text/html</code> source
 * is parsed by the HTML parser, <code>text/markdown</code> is converted to HTML first and then parsed, and <code>text/plain</code> becomes a
 * document with one <code>pre</code> element. The text kept in the snapshot (the value of <code>getContent()</code>) is never changed; the
 * page title comes from the parsed document. <code>application/xhtml+xml</code> is parsed by the XML parser (a document that is not well-formed becomes an error page). Other types (images are
 * wrapped into HTML by the network loader beforehand) keep their snapshot without a document. The DOM is built on the loading thread and handed over together with
 * the snapshot, so nothing else touches it before the document thread confirms the page. Cache clearing is forwarded to the wrapped loader.
 * <p>
 * 다른 로더의 스냅샷에 DOM 트리를 붙여 완성하는 페이지 로더(계획 P07, P17-02, P17-04): <code>text/html</code> 원문은 HTML 파서로 해석하고,
 * <code>text/markdown</code> 은 먼저 HTML 로 바꾼 뒤 해석하며, <code>text/plain</code> 은 <code>pre</code> 요소 하나를 가진 문서가 된다.
 * 스냅샷에 보관한 원문(<code>getContent()</code> 값)은 바꾸지 않으며 페이지 제목은 해석한 문서에서 얻는다. <code>application/xhtml+xml</code> 은 XML 파서로 해석한다(well-formed 가 아니면 오류 페이지가 된다). 그 밖의 형식(이미지는
 * 네트워크 로더가 미리 HTML 로 감싼다)은 문서 없이 스냅샷을 그대로 둔다. DOM 은 로딩 스레드에서 만들어 스냅샷과 함께 넘기므로
 * 문서 스레드가 페이지를 확정하기 전에는 아무도 건드리지 않는다. 캐시 삭제는 감싼 로더로 전달한다.
 */
public final class ParsingPageLoader implements PageLoader, CacheOwner {

    /**
     * The loader whose snapshots are completed.
     * <p>
     * 스냅샷을 완성할 대상 로더.
     */
    private final PageLoader delegate;

    /** Bound component state, published before the first navigation. / 첫 이동 전에 게시하는 연결된 컴포넌트 상태. */
    private volatile ScriptServices services;

    /** Immutable script dependencies without a construction cycle. / 생성 순환 없는 불변 스크립트 의존성. */
    private static final class ScriptServices {
        /** Owning session. / 소유 세션. */
        final BrowserSession session;
        /** Common request layer. / 공통 요청 계층. */
        final ResourceFetcher fetcher;
        /** Component cookies. / 컴포넌트 쿠키. */
        final CookieStore cookies;
        /** Presentation queries after ownership handoff. / 소유권 인계 뒤 표현 조회. */
        final PageView view;
        /** The loader that owns these services, which loads the documents of frames. / 이 서비스를 가진 로더. 프레임의 문서를 로드한다. */
        final ParsingPageLoader loader;
        /** Captures component dependencies. / 컴포넌트 의존성을 보관한다.
         * @param session owner / 소유자
         * @param fetcher common fetcher / 공통 가져오기 객체
         * @param cookies cookie jar / 쿠키 저장소
         * @param view presentation hooks / 표현 훅
         * @param loader the owning loader / 이 서비스를 가진 로더 */
        ScriptServices(BrowserSession session, ResourceFetcher fetcher, CookieStore cookies, PageView view, ParsingPageLoader loader) {
            this.session = session;
            this.fetcher = fetcher;
            this.cookies = cookies;
            this.view = view;
            this.loader = loader;
        }
    }

    /**
     * Reads the source of a frame with the wrapped loader (network, class path, error pages). Call it on a loader thread; the DOM is built
     * later on the document thread with {@link #attachFrame}, so that all documents of a page are parsed and scripted on one thread.
     * <p>
     * 감싼 로더로 프레임의 원문을 읽는다(네트워크, 클래스패스, 오류 페이지). 읽기 스레드에서 부른다. DOM 은 나중에 문서 스레드에서
     * {@link #attachFrame} 로 만들므로 한 페이지의 모든 문서가 한 스레드에서 파싱되고 스크립트가 실행된다.
     *
     * @param request the request of the frame / 프레임의 요청
     * @return the snapshot without a document / 문서가 없는 스냅샷
     * @throws Exception if the wrapped loader fails (a {@link DownloadRequiredException} when the response has to be saved) / 감싼 로더가 실패하면
     *                   (응답을 저장해야 하면 {@link DownloadRequiredException})
     */
    PageSnapshot loadFrameSource(NavigationRequest request) throws Exception {
        return delegate.load(request);
    }

    /**
     * Builds the DOM, the presentation and the script host of a frame from its snapshot. Call it on the document thread.
     * <p>
     * 프레임의 스냅샷으로 DOM, 표현 상태, 스크립트 호스트를 만든다. 문서 스레드에서 부른다.
     *
     * @param snapshot the source of the frame / 프레임의 원문
     * @param request  the request of the frame / 프레임의 요청
     * @return the snapshot with a document (unchanged if the type has none) / 문서가 붙은 스냅샷(문서가 없는 형식이면 그대로)
     */
    PageSnapshot attachFrame(PageSnapshot snapshot, NavigationRequest request) {
        return attach(snapshot, request.isJavascriptEnabled(), new PageRequestContext(request, snapshot.getCrossOriginPolicy()), services, request);
    }

    /**
     * Builds the DOM, the presentation and the script host of the first empty document of a window that a page opened (plan item
     * P31-08-b). It runs on the thread of the script that opened the window, without any loading.
     * <p>
     * 페이지가 연 창의 처음 빈 문서의 DOM, 표현 상태, 스크립트 호스트를 만든다(계획 P31-08-b). 창을 연 스크립트의 스레드에서 실행되며 읽기는
     * 하지 않는다.
     *
     * @param snapshot the empty source / 빈 원문
     * @param request  the request of the window, with the origin to inherit / 물려받을 오리진이 든 창의 요청
     * @return the snapshot with a document / 문서가 붙은 스냅샷
     */
    PageSnapshot attachInitial(PageSnapshot snapshot, NavigationRequest request) {
        return attach(snapshot, request.isJavascriptEnabled(), new PageRequestContext(request, snapshot.getCrossOriginPolicy()), services, request);
    }

    /**
     * Creates the decorator.
     * <p>
     * 장식 로더를 만든다.
     *
     * @param delegate the wrapped loader, not null / 감쌀 로더(null 불가)
     */
    public ParsingPageLoader(PageLoader delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

    /**
     * Loads a page with the wrapped loader and attaches the DOM tree.
     * <p>
     * 감싼 로더로 페이지를 읽고 DOM 트리를 붙인다.
     *
     * @param request what to load / 읽을 대상
     * @return the snapshot, with a document for the supported types / 스냅샷. 지원하는 형식에는 문서가 붙어 있다
     * @throws Exception if the wrapped loader fails / 감싼 로더가 실패하면
     */
    public PageSnapshot load(NavigationRequest request) throws Exception {
        PageSnapshot snapshot = delegate.load(request);
        ScriptServices bound = services;
        // 문서를 만들기 전에 응답의 opener 정책을 적용한다: 새 문서의 window.opener 가 그 결과를 본다.
        if (bound != null && snapshot != null) {
            bound.session.applyOpenerPolicy(request, snapshot);
        }
        PageSnapshot result = attach(snapshot, request.isJavascriptEnabled(), new PageRequestContext(request, snapshot.getCrossOriginPolicy()), services,
                request);
        if (request.getCancelToken().isCancelled()) {
            result.dispose();
            request.getCancelToken().throwIfCancelled();
        }
        return result;
    }

    /**
     * Binds standard component script services once, before any loads. Standalone decorators remain parse-only until bound.
     * <p>로딩 전에 표준 컴포넌트 스크립트 서비스를 한 번 연결한다. 독립 장식 로더는 연결 전까지 파싱만 한다.
     * @param session owning session / 소유 세션
     * @param fetcher common resource fetcher / 공통 리소스 가져오기 객체
     * @param cookies component cookie jar / 컴포넌트 쿠키 저장소
     * @param view page presentation / 페이지 표현
     * @throws IllegalArgumentException for null services / 서비스가 null이면
     * @throws IllegalStateException if already bound / 이미 연결되었으면
     */
    public synchronized void bindScripts(BrowserSession session, ResourceFetcher fetcher, CookieStore cookies, PageView view) {
        if (session == null || fetcher == null || cookies == null || view == null) {
            throw new IllegalArgumentException("script services must not be null");
        }
        if (services != null) {
            throw new IllegalStateException("script services already bound");
        }
        services = new ScriptServices(session, fetcher, cookies, view, this);
        session.bindBlobUrls(fetcher.getBlobUrlStore());
    }

    /**
     * Forwards the cache clearing to the wrapped loader when it owns caches.
     * <p>
     * 감싼 로더가 캐시를 가지고 있으면 캐시 삭제를 전달한다.
     */
    public void clearCache() {
        if (delegate instanceof CacheOwner) {
            ((CacheOwner) delegate).clearCache();
        }
    }

    /**
     * Attaches a DOM tree to a snapshot according to its MIME type.
     * <p>
     * MIME 타입에 따라 스냅샷에 DOM 트리를 붙인다.
     *
     * @param snapshot  the snapshot / 스냅샷
     * @param scripting whether JavaScript is enabled (affects how <code>noscript</code> is parsed) / JavaScript 활성 여부(<code>noscript</code> 해석에 영향)
     * @return the snapshot with a document, or the same snapshot if its type is not handled / 문서가 붙은 스냅샷. 처리하지 않는 형식이면 같은 스냅샷
     */
    static PageSnapshot attach(PageSnapshot snapshot, boolean scripting) {
        return attach(snapshot, scripting, new PageRequestContext(null, snapshot.getCrossOriginPolicy()), null, null);
    }

    /**
     * Attaches the parsed tree and copied request settings without retaining a navigation or history chain.
     * <p>이동이나 이력 연결을 붙잡지 않고 파싱한 트리와 복사한 요청 설정을 붙인다.
     * @param snapshot input snapshot / 입력 스냅샷
     * @param scripting parser scripting flag / 파서의 스크립트 설정
     * @param requests copied resource settings / 복사한 리소스 설정
     * @param services component services, null for standalone parsing / 컴포넌트 서비스이며 독립 파싱이면 null
     * @param request navigation settings, null for standalone parsing / 이동 설정이며 독립 파싱이면 null
     * @return snapshot with DOM / DOM이 있는 스냅샷
     */
    private static PageSnapshot attach(PageSnapshot snapshot, boolean scripting, PageRequestContext requests,
            ScriptServices services, NavigationRequest request) {
        if (snapshot.getDocument() != null) {
            return snapshot;
        }
        String mime = snapshot.getMimeType() == null ? "" : snapshot.getMimeType().toLowerCase();
        String content = snapshot.getContent() == null ? "" : snapshot.getContent();
        String url = snapshot.getBaseUrl() != null ? snapshot.getBaseUrl() : snapshot.getUrl();
        final String[] title = new String[1];
        HtmlPageDocument document;
        if ("text/html".equals(mime)) {
            document = parseHtml(content, url, snapshot.getEncoding(), scripting, title, services, request, requests);
        } else if ("text/markdown".equals(mime)) {
            // 원문은 그대로 두고 변환한 HTML 로만 문서를 만든다.
            document = parseHtml(MarkdownConverter.toHtmlDocument(content, null), url, snapshot.getEncoding(), scripting, title, services, request,
                    requests);
        } else if ("text/plain".equals(mime)) {
            document = new HtmlPageDocument(plainTextDocument(content, url), requests);
        } else if ("application/xhtml+xml".equals(mime)) {
            document = parseXhtml(content, url, snapshot.getEncoding(), scripting, title, services, request, requests);
        } else {
            return snapshot;
        }
        PageSnapshot result = snapshot.withDocument(document);
        if (title[0] != null && title[0].length() > 0 && (snapshot.getTitle() == null || snapshot.getTitle().length() == 0)) {
            result = result.withTitle(title[0]);
        }
        return result;
    }

    /**
     * Builds a tree from markup with a given parser, with the services of this component installed (selector engine, markup support).
     * <p>
     * 주어진 파서로 마크업에서 트리를 만들며 이 컴포넌트의 서비스(선택자 엔진, 마크업 지원)를 설치한다.
     */
    private interface MarkupSource {
        /**
         * Parses the markup into the tree, calling the listener while it does.
         * <p>
         * 마크업을 해석해 트리를 만들며 그 동안 리스너를 호출한다.
         *
         * @param listener the listener for scripts, styles and the title / 스크립트, 스타일, 제목을 위한 리스너
         * @return false if the markup could not be parsed into a tree (XHTML that is not well-formed) / 마크업을 트리로 해석하지 못했으면(well-formed 가
         *         아닌 XHTML) false
         */
        boolean parse(ParserListener listener);
    }

    /**
     * Parses HTML into a document with the services of this component installed (selector engine, markup support).
     * <p>
     * HTML 을 해석해 이 컴포넌트의 서비스(선택자 엔진, 마크업 지원)가 설치된 문서를 만든다.
     *
     * @param source    HTML source / HTML 원문
     * @param url       document URL / 문서 URL
     * @param encoding  name of the character encoding of the source / 원문의 문자 인코딩 이름
     * @param scripting JavaScript enabled flag / JavaScript 활성 여부
     * @param title     one-element array that receives the document title / 문서 제목을 받는 원소 하나짜리 배열
     * @param services optional component services / 선택적 컴포넌트 서비스
     * @param request optional navigation settings / 선택적 이동 설정
     * @param requests copied resource settings / 복사한 리소스 설정
     * @return the document with an optional script host / 선택적 스크립트 호스트를 가진 문서
     */
    private static HtmlPageDocument parseHtml(final String source, String url, String encoding, final boolean scripting, final String[] title,
            final ScriptServices services, NavigationRequest request, PageRequestContext requests) {
        final Document doc = Document.createHtmlDocument();
        return parseMarkup(doc, new MarkupSource() {
            /**
             * Runs the HTML parser.
             * <p>
             * HTML 파서를 실행한다.
             *
             * @param listener the listener / 리스너
             * @return always true: HTML always yields a tree / 항상 true. HTML 은 언제나 트리가 된다
             */
            public boolean parse(ParserListener listener) {
                HtmlParser parser = new HtmlParser(doc, source);
                parser.setScripting(scripting);
                parser.setListener(listener);
                parser.run();
                return true;
            }
        }, url, encoding, scripting, title, services, request, requests);
    }

    /**
     * Parses an XHTML document with the XML parser (plan item P17-05). The result is a document in XML style (case-sensitive names, no
     * error recovery); a document that is not well-formed gives an error page instead of a tree: the page shows the position of the error,
     * and the received text stays the source of the page.
     * <p>
     * XHTML 문서를 XML 파서로 해석한다(계획 P17-05). 결과는 XML 방식의 문서(대소문자를 구분하는 이름, 오류 복구 없음)이며, well-formed 가 아닌 문서는
     * 트리 대신 오류 페이지를 만든다. 페이지는 오류 위치를 보여 주고 수신 원문은 페이지의 원문으로 남는다.
     *
     * @param source    XHTML source / XHTML 원문
     * @param url       document URL / 문서 URL
     * @param encoding  name of the character encoding of the source / 원문의 문자 인코딩 이름
     * @param scripting JavaScript enabled flag / JavaScript 활성 여부
     * @param title     one-element array that receives the document title / 문서 제목을 받는 원소 하나짜리 배열
     * @param services  optional component services / 선택적 컴포넌트 서비스
     * @param request   optional navigation settings / 선택적 이동 설정
     * @param requests  copied resource settings / 복사한 리소스 설정
     * @return the document, or the document of an error page / 문서. well-formed 가 아니면 오류 페이지의 문서
     */
    private static HtmlPageDocument parseXhtml(final String source, final String url, String encoding, boolean scripting, final String[] title,
            ScriptServices services, NavigationRequest request, PageRequestContext requests) {
        final Document doc = Document.createXmlDocument(XhtmlParser.CONTENT_TYPE);
        final XhtmlResult[] result = new XhtmlResult[1];
        HtmlPageDocument parsed = parseMarkup(doc, new MarkupSource() {
            /**
             * Runs the XML parser.
             * <p>
             * XML 파서를 실행한다.
             *
             * @param listener the listener / 리스너
             * @return true if the document is well-formed / well-formed 이면 true
             */
            public boolean parse(ParserListener listener) {
                XhtmlParser parser = new XhtmlParser(doc);
                parser.setListener(listener);
                result[0] = parser.parse(source, url);
                return result[0].isWellFormed();
            }
        }, url, encoding, scripting, title, services, request, requests);
        if (parsed != null) {
            return parsed;
        }
        // 구문 오류가 있는 XHTML 은 HTML 로 복구하지 않고 오류 위치를 보여 주는 페이지를 만든다. 오류 페이지의 스크립트는 실행하지 않는다.
        ErrorPage page = ErrorPageBuilder.xmlParseError(url, result[0].getError());
        title[0] = page.getHeading();
        return parseHtml(page.getHtml(), url, encoding, false, new String[1], services, request, requests);
    }

    /**
     * Runs a markup parser into a prepared document with the page runtime installed first (see {@link #parseHtml}).
     * <p>
     * 페이지 런타임을 먼저 설치한 뒤 마크업 파서를 준비된 문서로 실행한다({@link #parseHtml} 참조).
     *
     * @param doc       the new, empty document / 새로 만든 빈 문서
     * @param markup    the parser / 파서
     * @param url       document URL / 문서 URL
     * @param encoding  name of the character encoding of the source / 원문의 문자 인코딩 이름
     * @param scripting JavaScript enabled flag / JavaScript 활성 여부
     * @param title     one-element array that receives the document title / 문서 제목을 받는 원소 하나짜리 배열
     * @param services  optional component services / 선택적 컴포넌트 서비스
     * @param request   optional navigation settings / 선택적 이동 설정
     * @param requests  copied resource settings / 복사한 리소스 설정
     * @return the document, or null if the markup does not make a tree / 문서. 마크업이 트리가 되지 않으면 null
     */
    private static HtmlPageDocument parseMarkup(Document doc, MarkupSource markup, String url, String encoding, boolean scripting, final String[] title,
            final ScriptServices services, NavigationRequest request, PageRequestContext requests) {
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setScriptingEnabled(scripting);
        // 문서의 문자 인코딩은 수신 바이트를 디코딩한 인코딩이다. 파싱 중 실행되는 외부 일반 스크립트가 이것을 물려받는다.
        doc.setCharacterSet(encoding);
        if (url != null) {
            doc.setUrl(url);
        }
        // 등록되지 않은 객체와 접근이 거부된 파일의 오류 문서는 주소 문자열에 적힌 오리진을 획득하지 않는다.
        String protocol = UrlUtil.getScheme(url);
        if ("blob".equals(protocol) && (request == null || request.getBlobUrlEntry() == null)
                || UrlUtil.SCHEME_FILE.equals(protocol) && request != null && request.getFrame() != null
                        && (request.getFileFrameInitiator() == null || !UrlUtil.SCHEME_FILE.equals(request.getFileFrameInitiator().getScheme()))) {
            doc.setOriginUrl("about:blank");
        }
        // 프레임의 문서는 주소, 기준 URL, 오리진을 프레임이 정한다(about:blank/srcdoc 은 생성자의 것을 물려받고 샌드박스는 오리진이 불투명).
        final FrameContext frame = request == null ? null : request.getFrame();
        if (frame != null) {
            frame.prepareDocument(doc);
        }
        // 페이지가 연 창의 처음 빈 문서는 창을 연 문서의 오리진과 기준 주소를 물려받는다.
        if (frame == null && request != null && request.getInheritedOrigin() != null) {
            doc.setUrl("about:blank");
            doc.setOriginUrl(request.getInheritedOrigin());
            if (request.getInheritedBase() != null) {
                doc.setBaseUri(request.getInheritedBase());
            }
        }
        final PagePresentation presentation = services == null ? null : services.view.createPresentation(doc, requests);
        if (presentation != null) {
            // 폼 컨트롤의 호스트는 파싱 전에 설치한다: 파싱 중 스크립트가 폼을 조작해도 상태 변경이 페이지에 전달된다.
            doc.setFormHost(services.view.createFormHost(doc, presentation, frame));
            // 선택의 변경 통지도 파싱 전에 연결한다: 스크립트와 사용자가 같은 Selection 을 쓰고 selectionchange 와 강조가 한곳에서 일어난다.
            doc.setSelectionListener(services.view.createSelectionListener());
        }
        final PageRuntime runtime = !scripting || services == null ? null
                : new PageRuntime(services.session, services.fetcher, services.cookies, doc, request, requests, services.view, presentation);
        // 프레임의 문서는 파싱하는 동안에도 자기 window 를 가진다. 프레임의 문맥이 알아야 parent.frames[0] 같은 참조가 자기 자신을 가리킨다.
        if (frame != null && runtime != null) {
            frame.parsingStarted(doc, runtime.scripts());
        }
        if (presentation != null) {
            presentation.bindCancellation(request.getCancelToken());
            // 이 문서의 iframe/frame 요소를 중첩 브라우징 문맥으로 연결한다. 확정되기 전에는 요소만 관찰한다.
            presentation.setFrameSet(FrameSet.create(services.session, services.loader, services.view, request, doc, presentation, runtime, frame));
        }
        final ParserListener scripts = runtime == null ? null : runtime.scripts().createParserListener();
        // 첫 번째로 해석되는 meta http-equiv=refresh 만 쓴다(HTML5 2014 §4.2.5.3: 이미 처리한 refresh 가 있으면 뒤의 것은 무시한다).
        final MetaRefresh[] refresh = new MetaRefresh[1];
        ParserListener listener = new ParserListenerAdapter() {
            /** Starts a parser stylesheet without blocking HTML tokenization. / HTML 토큰화를 막지 않고 파서 스타일시트를 시작한다.
             * @param style completed style element / 완성된 style 요소 */
            public void styleReady(Element style) {
                if (presentation != null) {
                    presentation.parserStyleSheet(style, services.view.viewportWidth(), services.view.viewportHeight());
                }
            }

            /** Starts a parser link and records its initial blocking conditions. / 파서 link를 시작하고 초기 차단 조건을 기록한다.
             * @param link parser-created stylesheet link / 파서 생성 스타일시트 link */
            public void stylesheetFound(Element link) {
                if (presentation != null) {
                    presentation.parserStyleSheet(link, services.view.viewportWidth(), services.view.viewportHeight());
                }
            }

            /** Forwards parser-inserted scripts at their insertion point. / 파서 삽입 스크립트를 해당 삽입 지점에서 전달한다.
             * @param script completed element / 완성된 요소
             * @param control parser insertion control / 파서 삽입 제어 */
            public void scriptReady(Element script, ParserControl control) {
                if (scripts != null) {
                    scripts.scriptReady(script, control);
                }
            }
            /**
             * Remembers the first valid timed redirect of a <code>meta</code> element.
             * <p>
             * <code>meta</code> 요소의 첫 번째 올바른 예약된 이동을 기억한다.
             *
             * @param content the value of the <code>content</code> attribute / <code>content</code> 속성 값
             */
            public void metaRefresh(String content) {
                if (refresh[0] == null) {
                    refresh[0] = MetaRefresh.parse(content);
                }
            }

            /**
             * Remembers the title of the document.
             * <p>
             * 문서의 제목을 기억한다.
             *
             * @param t the title text / 제목 문자열
             */
            public void titleReady(String t) {
                if (title[0] == null) {
                    title[0] = t;
                }
            }
        };
        boolean complete = false;
        try {
            if (runtime != null) {
                runtime.scripts().parsingStarted();
            }
            boolean tree = markup.parse(listener);
            if (!tree) {
                return null;
            }
            if (runtime != null) {
                runtime.scripts().completeParsing();
            }
            complete = true;
            PageAudio audio = null;
            // 스크립트 호스트가 없는 문서에는 프레임의 제한과 함께 마크업의 오디오를 준비한다.
            if (runtime == null && services != null) {
                audio = PageAudio.prepare(services.session, services.view, presentation, frame, doc);
            }
            HtmlPageDocument page = new HtmlPageDocument(doc, requests, runtime, presentation, audio);
            // 문서에 meta refresh 가 있으면 페이지가 확정될 때 시작하도록 맡긴다(JavaScript 설정과 무관하다).
            if (refresh[0] != null && services != null) {
                page.setMetaRefresh(refresh[0], services.session, frame);
            }
            return page;
        } finally {
            if (presentation != null) {
                presentation.parsingFinished();
            }
            if (runtime != null) {
                runtime.parsingFinished();
            }
            // 예외로 버려진 파싱도 타이머·네트워크·세션 등록을 반드시 해제한다.
            if (!complete && runtime != null) {
                runtime.close();
            }
            if (!complete && presentation != null) {
                presentation.retire();
            }
        }
    }

    /**
     * Builds the document of a plain text page: the text is shown as it is inside one <code>pre</code> element, never as markup.
     * <p>
     * 일반 텍스트 페이지의 문서를 만든다: 텍스트는 마크업으로 해석되지 않고 <code>pre</code> 요소 하나 안에 그대로 보인다.
     *
     * @param text the text / 텍스트
     * @param url  document URL / 문서 URL
     * @return the document / 문서
     */
    private static Document plainTextDocument(String text, String url) {
        Document doc = PlainTextDocumentBuilder.build(text, url);
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        return doc;
    }
}
