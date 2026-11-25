package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.StreamingWebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;

/**
 * A test page for the web request APIs: a real {@link PageScripts} over a small document, a manual clock, and a fake
 * {@link WebRequestSource} whose requests are recorded and answered by the test itself, on the test thread, so that every order of events
 * is deterministic. {@link #run()} lets the page process what was queued. Console output is recorded.
 * <p>
 * 웹 요청 API 시험용 페이지: 작은 문서 위의 실제 {@link PageScripts}, 수동 시계, 요청을 기록하고 시험이 직접 시험 스레드에서 응답하는 가짜
 * {@link WebRequestSource}. 모든 이벤트 순서가 결정적이다. {@link #run()} 은 줄 선 일을 페이지가 처리하게 한다. 콘솔 출력은 기록된다.
 */
final class WebFixture {

    /**
     * A request that the page started and the way to answer it.
     * <p>
     * 페이지가 시작한 요청과 그 응답 방법.
     */
    static final class Started implements WebRequestSource.Call {
        /** The request. / 요청. */
        final WebRequest request;

        /** The receiver. / 받는 쪽. */
        final WebRequestSource.Callback callback;

        /** True after cancel. / 취소된 뒤에는 true. */
        boolean cancelled;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param request  the request / 요청
         * @param callback the receiver / 받는 쪽
         */
        Started(WebRequest request, WebRequestSource.Callback callback) {
            this.request = request;
            this.callback = callback;
        }

        /**
         * Records the cancellation.
         * <p>
         * 취소를 기록한다.
         */
        public void cancel() {
            cancelled = true;
        }
    }

    /**
     * A started stream that the test answers by hand (the streaming counterpart of {@link Started}).
     * <p>
     * 시험이 직접 답하는 시작된 스트림({@link Started} 의 스트리밍 짝).
     */
    static final class StreamStarted implements WebRequestSource.Call {
        /** The request. / 요청. */
        final WebRequest request;

        /** The receiver of the stream. / 스트림을 받는 쪽. */
        final StreamingWebRequestSource.Listener listener;

        /** True after the page cancelled the stream. / 페이지가 스트림을 취소했으면 true. */
        boolean cancelled;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param request  the request / 요청
         * @param listener the receiver / 받는 쪽
         */
        StreamStarted(WebRequest request, StreamingWebRequestSource.Listener listener) {
            this.request = request;
            this.listener = listener;
        }

        /**
         * Records the cancellation.
         * <p>
         * 취소를 기록한다.
         */
        public void cancel() {
            cancelled = true;
        }
    }

    /**
     * The environment of the test page.
     * <p>
     * 시험 페이지의 환경.
     */
    static final class Env extends HostEnvironment {
        /** The manual clock. / 수동 시계. */
        final PageFixture.ManualScheduler scheduler = new PageFixture.ManualScheduler();

        /** The requests started so far. / 지금까지 시작한 요청. */
        final List<Started> started = new ArrayList<Started>();

        /** The streams started so far. / 지금까지 시작한 스트림. */
        final List<StreamStarted> streams = new ArrayList<StreamStarted>();

        /** False to simulate a request service that cannot stream. / 스트리밍할 수 없는 요청 서비스를 흉내 내려면 false. */
        boolean streaming = true;

        /** False to simulate a host without a request service. / 요청 서비스가 없는 호스트를 흉내 내려면 false. */
        boolean hasSource = true;

        /** False to switch scripts off. / 스크립트를 끄려면 false. */
        boolean scripting = true;

        /** Whether the test page has the isolated capability. / 시험 페이지에 격리 권한이 있는지 여부. */
        boolean isolated;

        /** Returns the mocked isolation capability. / 시험용 격리 권한을 반환한다. */
        public boolean isCrossOriginIsolated() {
            return isolated;
        }

        /**
         * Returns the manual scheduler.
         * <p>
         * 수동 스케줄러를 반환한다.
         *
         * @return the scheduler / 스케줄러
         */
        public Scheduler getScheduler() {
            return scheduler;
        }

        /**
         * Returns the fake request service.
         * <p>
         * 가짜 요청 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public WebRequestSource getWebRequestSource() {
            if (!hasSource) {
                return null;
            }
            if (streaming) {
                return new StreamingSource();
            }
            return new WebRequestSource() {
                public Call start(WebRequest request, Callback callback) {
                    Started s = new Started(request, callback);
                    started.add(s);
                    return s;
                }
            };
        }

        /**
         * A request service that also streams; it records what the page starts.
         * <p>
         * 스트리밍도 하는 요청 서비스. 페이지가 시작하는 것을 기록한다.
         */
        private final class StreamingSource implements WebRequestSource, StreamingWebRequestSource {
            /**
             * Records an ordinary request.
             * <p>
             * 보통 요청을 기록한다.
             *
             * @param request  the request / 요청
             * @param callback the receiver / 받는 쪽
             * @return the handle / 핸들
             */
            public Call start(WebRequest request, Callback callback) {
                Started s = new Started(request, callback);
                started.add(s);
                return s;
            }

            /**
             * Records a stream.
             * <p>
             * 스트림을 기록한다.
             *
             * @param request  the request / 요청
             * @param listener the receiver / 받는 쪽
             * @return the handle / 핸들
             */
            public Call startStream(WebRequest request, StreamingWebRequestSource.Listener listener) {
                StreamStarted s = new StreamStarted(request, listener);
                streams.add(s);
                return s;
            }
        }

        /**
         * Returns the scripting flag.
         * <p>
         * 스크립트 사용 표시를 반환한다.
         *
         * @return true if scripts run / 스크립트를 실행하면 true
         */
        public boolean isScriptingEnabled() {
            return scripting;
        }
    }

    /** The environment. / 환경. */
    final Env env = new Env();

    /** The page scripts. / 페이지 스크립트. */
    final PageScripts scripts;

    /** The console lines of the page. / 페이지의 콘솔 줄. */
    final List<String> console = new ArrayList<String>();

    /**
     * Loads a page.
     * <p>
     * 페이지를 로드한다.
     *
     * @param url  the page URL / 페이지 URL
     * @param html the markup / 마크업
     */
    WebFixture(String url, String html) {
        this(url, html, false);
    }

    /**
     * Loads a page with the specified isolation capability.
     * <p>
     * 지정한 격리 권한으로 페이지를 로드한다.
     *
     * @param url page URL / 페이지 URL
     * @param html page markup / 페이지 마크업
     * @param isolated whether shared memory is available / 공유 메모리를 사용할 수 있는지 여부
     */
    private WebFixture(String url, String html, boolean isolated) {
        env.isolated = isolated;
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setUrl(url);
        scripts = new PageScripts(doc, env);
        scripts.getRealm().setConsole(new ConsoleHook() {
            public void message(String level, String text) {
                console.add(level + ": " + text);
            }
        });
        HtmlParser parser = new HtmlParser(doc, html);
        parser.setScripting(true);
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
        run();
    }

    /**
     * Loads an empty page at the usual test address.
     * <p>
     * 보통의 시험 주소의 빈 페이지를 로드한다.
     */
    WebFixture() {
        this(false);
    }

    /** Loads the default page with the specified capability. / 지정한 권한으로 기본 페이지를 로드한다. */
    WebFixture(boolean isolated) {
        this("http://example.test/dir/page.html", "<html><body><div id=out></div></body></html>", isolated);
    }

    /**
     * Lets the page process everything that was queued (and the timers that are due now).
     * <p>
     * 줄 선 모든 일(과 지금 기한이 된 타이머)을 페이지가 처리하게 한다.
     */
    void run() {
        env.scheduler.advance(0);
    }

    /**
     * Moves the manual clock and lets the page process what becomes due.
     * <p>
     * 수동 시계를 옮기고 기한이 된 일을 페이지가 처리하게 한다.
     *
     * @param ms the milliseconds / 밀리초
     */
    void advance(long ms) {
        env.scheduler.advance(ms);
    }

    /**
     * Runs script text as a page task and returns its completion value as a string (null if it threw).
     * <p>
     * 스크립트 텍스트를 페이지 작업으로 실행하고 완료 값을 문자열로 반환한다(던졌으면 null).
     *
     * @param code the script / 스크립트
     * @return the value or null / 값 또는 null
     */
    String js(String code) {
        return scripts.evaluate(code, "test");
    }

    /**
     * Answers the request with the given index with a complete response, on the test thread, then lets the page process it.
     * <p>
     * 주어진 순번의 요청에 완전한 응답으로 시험 스레드에서 응답한 뒤 페이지가 처리하게 한다.
     *
     * @param index    the index of the request / 요청의 순번
     * @param response the response / 응답
     */
    void answer(int index, WebResponse response) {
        env.started.get(index).callback.onComplete(response);
        run();
    }

    /**
     * Lets stream number <code>index</code> answer with a response head and runs the page tasks.
     * <p>
     * <code>index</code> 번째 스트림이 응답 머리로 답하게 하고 페이지 작업을 실행한다.
     *
     * @param index  the stream / 스트림
     * @param status the status code / 상태 코드
     * @param type   the Content-Type or null / Content-Type 또는 null
     * @return what the listener answered: true if it wants the body / 받는 쪽의 답: 본문을 원하면 true
     */
    boolean streamHead(int index, int status, String type) {
        WebResponse head = response(status, type, "");
        head.setUrl("http://example.test/dir/stream");
        boolean wanted = env.streams.get(index).listener.onHeaders(head);
        run();
        return wanted;
    }

    /**
     * Sends a piece of the body of stream number <code>index</code> (UTF-8) and runs the page tasks.
     * <p>
     * <code>index</code> 번째 스트림의 본문 조각을 보내고(UTF-8) 페이지 작업을 실행한다.
     *
     * @param index the stream / 스트림
     * @param text  the piece / 조각
     */
    void streamData(int index, String text) {
        byte[] b = Encodings.utf8(text);
        env.streams.get(index).listener.onData(b, b.length);
        run();
    }

    /**
     * Ends stream number <code>index</code> and runs the page tasks.
     * <p>
     * <code>index</code> 번째 스트림을 끝내고 페이지 작업을 실행한다.
     *
     * @param index the stream / 스트림
     * @param end   the end / 끝
     */
    void streamEnd(int index, WebResponse end) {
        env.streams.get(index).listener.onEnd(end);
        run();
    }

    /**
     * Creates an ordinary response.
     * <p>
     * 일반적인 응답을 만든다.
     *
     * @param status the status / 상태
     * @param type   the Content-Type / Content-Type
     * @param body   the body text (UTF-8) / 본문 텍스트(UTF-8)
     * @return the response / 응답
     */
    static WebResponse response(int status, String type, String body) {
        WebResponse r = new WebResponse(WebResponse.TYPE_BASIC);
        r.setStatus(status, status == 200 ? "OK" : "Status " + status);
        r.setUrl("http://example.test/dir/data");
        byte[] bytes = Encodings.utf8(body);
        r.setBody(bytes);
        r.setDeclaredLength(bytes.length);
        if (type != null) {
            r.addHeader("Content-Type", type);
        }
        r.addHeader("Content-Length", String.valueOf(bytes.length));
        return r;
    }

    /**
     * Returns the console lines joined with a bar.
     * <p>
     * 콘솔 줄을 세로줄로 이어서 반환한다.
     *
     * @return the lines / 줄들
     */
    String log() {
        StringBuilder sb = new StringBuilder();
        // 기록한 콘솔 줄을 세로줄로 잇는다.
        for (int i = 0; i < console.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(console.get(i));
        }
        return sb.toString();
    }
}
