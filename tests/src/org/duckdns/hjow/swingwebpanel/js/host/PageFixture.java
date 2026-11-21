package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;

/**
 * A test fixture that loads an HTML page the way the component does: a {@link PageScripts}, a parse with the script listener,
 * {@link PageScripts#completeParsing()}, with a manual clock, script sources from a map, recorded console output, navigation requests and
 * switchable scripting. Time only moves when {@link #advance(long)} is called.
 * <p>
 * 컴포넌트가 하는 방식대로 HTML 페이지를 로드하는 시험 도구: {@link PageScripts}, 스크립트 리스너와 함께 해석, {@link PageScripts#completeParsing()}.
 * 수동 시계, 맵에서 가져오는 스크립트 원문, 기록되는 콘솔 출력과 이동 요청, 켜고 끌 수 있는 스크립트 실행을 갖는다. 시간은
 * {@link #advance(long)} 을 부를 때만 흐른다.
 */
public final class PageFixture {

    /**
     * A task of the manual scheduler.
     * <p>
     * 수동 스케줄러의 작업.
     */
    private static final class Task implements Comparable<Task>, Scheduler.Handle {
        /**
         * The due time.
         * <p>
         * 실행 시각.
         */
        long due;
        /**
         * The creation order for equal times.
         * <p>
         * 시각이 같을 때의 생성 순서.
         */
        long seq;
        /**
         * The work.
         * <p>
         * 할 일.
         */
        Runnable work;
        /**
         * True after cancel.
         * <p>
         * 취소 뒤에는 true.
         */
        volatile boolean cancelled;

        /**
         * Cancels the task.
         * <p>
         * 작업을 취소한다.
         */
        public void cancel() {
            cancelled = true;
        }

        /**
         * Orders by time, then by creation.
         * <p>
         * 시각, 생성 순서로 정렬한다.
         *
         * @param o the other task / 다른 작업
         * @return comparison / 비교 결과
         */
        public int compareTo(Task o) {
            return due != o.due ? (due < o.due ? -1 : 1) : (seq < o.seq ? -1 : seq > o.seq ? 1 : 0);
        }
    }

    /**
     * The manual clock and task queue.
     * <p>
     * 수동 시계와 작업 대기열.
     */
    public static final class ManualScheduler implements Scheduler {
        /**
         * The current time.
         * <p>
         * 현재 시각.
         */
        long now = 1000000;
        /**
         * The next sequence number.
         * <p>
         * 다음 순서 번호.
         */
        long seq;
        /**
         * The pending tasks.
         * <p>
         * 대기 중인 작업.
         */
        final PriorityQueue<Task> queue = new PriorityQueue<Task>();

        /**
         * Returns the manual time.
         * <p>
         * 수동 시각을 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public synchronized long now() {
            return now;
        }

        /**
         * Queues a task.
         * <p>
         * 작업을 대기열에 넣는다.
         *
         * @param task    the task / 작업
         * @param delayMs the delay / 지연
         * @return the handle / 핸들
         */
        public Handle post(Runnable task, long delayMs) {
            Task t = new Task();
            // 플레이어와 가져오기 스레드도 작업을 올리므로 대기열은 잠금 아래에서만 만진다.
            synchronized (this) {
                t.due = now + delayMs;
                t.seq = seq++;
                t.work = task;
                queue.add(t);
            }
            return t;
        }

        /**
         * Moves the time forward and runs every task that becomes due, in order.
         * <p>
         * 시간을 앞으로 보내며 실행 시각이 된 작업을 순서대로 모두 실행한다.
         *
         * @param ms the milliseconds / 밀리초
         */
        public void advance(long ms) {
            long end;
            synchronized (this) {
                end = now + ms;
            }
            // 기한이 끝 시각 이내인 작업을 하나씩 꺼내 시계를 그 시각으로 옮기며 실행한다. 작업은 잠금 밖에서 실행한다(작업이 다시 올릴 수 있다).
            for (;;) {
                Task t;
                synchronized (this) {
                    if (queue.isEmpty() || queue.peek().due > end) {
                        break;
                    }
                    t = queue.poll();
                    if (t.cancelled) {
                        continue;
                    }
                    now = Math.max(now, t.due);
                }
                t.work.run();
            }
            synchronized (this) {
                now = end;
            }
        }

        /**
         * Returns the number of pending (not cancelled) tasks.
         * <p>
         * 대기 중인(취소되지 않은) 작업 수를 반환한다.
         *
         * @return count / 개수
         */
        public synchronized int pending() {
            int n = 0;
            // 취소되지 않은 작업만 센다.
            for (Task t : queue) {
                if (!t.cancelled) {
                    n++;
                }
            }
            return n;
        }
    }

    /**
     * The environment of the fixture.
     * <p>
     * 시험 도구의 환경.
     */
    public static final class Env extends HostEnvironment {
        /**
         * The scheduler.
         * <p>
         * 스케줄러.
         */
        public final ManualScheduler scheduler = new ManualScheduler();
        /**
         * Script sources by URL.
         * <p>
         * URL 별 스크립트 원문.
         */
        public final Map<String, String> scripts = new HashMap<String, String>();
        /**
         * Navigation requests as <code>url</code> or <code>url (replace)</code>.
         * <p>
         * 이동 요청(<code>url</code> 또는 <code>url (replace)</code>).
         */
        public final List<String> navigations = new ArrayList<String>();
        /**
         * Host calls such as <code>reload</code> and <code>go(-1)</code>.
         * <p>
         * <code>reload</code>, <code>go(-1)</code> 같은 호스트 호출.
         */
        public final List<String> calls = new ArrayList<String>();
        /**
         * The cookie store or null.
         * <p>
         * 쿠키 저장소 또는 null.
         */
        public CookieStore cookies;
        /**
         * The localStorage manager or null.
         * <p>
         * localStorage 관리자 또는 null.
         */
        public LocalStorageManager local;
        /**
         * The sessionStorage or null.
         * <p>
         * sessionStorage 또는 null.
         */
        public SessionStorage session;
        /**
         * The scripting flag.
         * <p>
         * 스크립트 사용 플래그.
         */
        public boolean scripting = true;
        /**
         * The time limit of a script task.
         * <p>
         * 스크립트 작업의 시간 제한.
         */
        public long timeLimit = 10000;
        /**
         * The style resolver for getComputedStyle, or null.
         * <p>
         * getComputedStyle 용 스타일 리졸버. 없으면 null.
         */
        public StyleResolver resolver;
        /**
         * Number of documentChanged calls.
         * <p>
         * documentChanged 호출 수.
         */
        public int changes;
        /**
         * The image loading service, or null (the page then cannot load images).
         * <p>
         * 이미지 로딩 서비스. 없으면 null(페이지가 이미지를 로드할 수 없다).
         */
        public ImageRequestSource images;

        /**
         * The pointer capture service, or null (the page then has no active pointer).
         * <p>
         * 포인터 캡처 서비스. 없으면 null(페이지에 활성 포인터가 없다).
         */
        public PointerService pointers;

        /**
         * Whether the document counts as visible (the host's answer for <code>document.visibilityState</code>).
         * <p>
         * 문서를 보이는 것으로 치는지(<code>document.visibilityState</code> 에 대한 호스트의 답).
         */
        public volatile boolean visible = true;

        /**
         * Whether the host is online (the answer for <code>navigator.onLine</code>).
         * <p>
         * 호스트가 연결되어 있는지(<code>navigator.onLine</code> 에 대한 답).
         */
        public volatile boolean online = true;

        /**
         * Returns the visibility that the test set.
         * <p>
         * 시험이 정한 가시성을 반환한다.
         *
         * @return true while visible / 보이는 동안 true
         */
        public boolean isDocumentVisible() {
            return visible;
        }

        /**
         * Returns the network state that the test set.
         * <p>
         * 시험이 정한 네트워크 상태를 반환한다.
         *
         * @return true while online / 연결된 동안 true
         */
        public boolean isOnline() {
            return online;
        }

        /**
         * Returns the fake pointer capture service.
         * <p>
         * 가짜 포인터 캡처 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public PointerService getPointerService() {
            return pointers;
        }

        /**
         * The media request service, or null (the page then cannot load audio or video).
         * <p>
         * 미디어 요청 서비스. 없으면 null(페이지가 오디오와 비디오를 로드할 수 없다).
         */
        public MediaRequestSource media;

        /**
         * The factory of the sound outputs, or null (the page then has no sound output).
         * <p>
         * 소리 출력의 팩터리. 없으면 null(페이지에 소리 출력이 없다).
         */
        public AudioOutputFactory outputs;

        /**
         * The media states that the page reported, as <code>url=ok</code> or <code>url=reason</code>.
         * <p>
         * 페이지가 알린 미디어 상태(<code>url=ok</code> 또는 <code>url=이유</code>).
         */
        public final List<String> mediaStates = new ArrayList<String>();

        /**
         * The viewport width that the page sees (a test may change it before it reports the change).
         * <p>
         * 페이지가 보는 뷰포트 폭(시험이 바꾸고 변화를 알릴 수 있다).
         */
        public volatile int viewportWidth = 800;

        /**
         * The viewport height that the page sees.
         * <p>
         * 페이지가 보는 뷰포트 높이.
         */
        public volatile int viewportHeight = 600;

        /**
         * Returns {@link #viewportWidth}.
         * <p>
         * {@link #viewportWidth} 를 반환한다.
         *
         * @return the width / 폭
         */
        public int getViewportWidth() {
            return viewportWidth;
        }

        /**
         * Returns {@link #viewportHeight}.
         * <p>
         * {@link #viewportHeight} 를 반환한다.
         *
         * @return the height / 높이
         */
        public int getViewportHeight() {
            return viewportHeight;
        }

        /**
         * The horizontal scroll position that the page sees; {@link #scrollTo} moves it at once (the component does it later).
         * <p>
         * 페이지가 보는 가로 스크롤 위치. {@link #scrollTo} 가 즉시 옮긴다(컴포넌트는 나중에 한다).
         */
        public volatile int scrollX;

        /**
         * The vertical scroll position that the page sees.
         * <p>
         * 페이지가 보는 세로 스크롤 위치.
         */
        public volatile int scrollY;

        /**
         * The scroll requests of the page as <code>x,y</code>, in order.
         * <p>
         * 페이지의 스크롤 요청(<code>x,y</code>), 순서대로.
         */
        public final List<String> scrolls = new ArrayList<String>();

        /**
         * The border boxes that the layout hook reports, by element id, as x, y, width, height in viewport pixels.
         * <p>
         * 레이아웃 훅이 알려 주는 border 상자. 요소 id 별, 뷰포트 픽셀의 x, y, 폭, 높이.
         */
        public final Map<String, double[]> bounds = new HashMap<String, double[]>();

        /**
         * Returns {@link #scrollX}.
         * <p>
         * {@link #scrollX} 를 반환한다.
         *
         * @return the position / 위치
         */
        public int getScrollX() {
            return scrollX;
        }

        /**
         * Returns {@link #scrollY}.
         * <p>
         * {@link #scrollY} 를 반환한다.
         *
         * @return the position / 위치
         */
        public int getScrollY() {
            return scrollY;
        }

        /**
         * Records the request and moves the position at once.
         * <p>
         * 요청을 기록하고 위치를 즉시 옮긴다.
         *
         * @param x the x / x
         * @param y the y / y
         */
        public void scrollTo(double x, double y) {
            scrolls.add(Math.round(x) + "," + Math.round(y));
            scrollX = (int) Math.round(Math.max(0, x));
            scrollY = (int) Math.round(Math.max(0, y));
        }

        /**
         * Returns the box that a test registered for the id of the element, or zeros.
         * <p>
         * 시험이 요소의 id 로 등록한 상자를 반환한다. 없으면 0.
         *
         * @param element the element / 요소
         * @return x, y, width, height / x, y, 폭, 높이
         */
        public double[] getElementBounds(org.duckdns.hjow.swingwebpanel.dom.Element element) {
            double[] b = bounds.get(element.getAttribute("id"));
            return b == null ? new double[4] : b.clone();
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
         * Returns the script source backed by the map.
         * <p>
         * 맵을 쓰는 스크립트 공급원을 반환한다.
         *
         * @return the source / 공급원
         */
        public ScriptSource getScriptSource() {
            return new ScriptSource() {
                /**
                 * Reads a script from the map.
                 * <p>
                 * 맵에서 스크립트를 읽는다.
                 *
                 * @param url the URL / URL
                 * @return the text / 텍스트
                 * @throws IOException if the URL is unknown / URL 을 모르면
                 */
                public String fetch(String url) throws IOException {
                    String s = scripts.get(url);
                    if (s == null) {
                        throw new IOException("404 " + url);
                    }
                    return s;
                }
            };
        }

        /**
         * Returns the cookie store.
         * <p>
         * 쿠키 저장소를 반환한다.
         *
         * @return the store / 저장소
         */
        public CookieStore getCookies() {
            return cookies;
        }

        /**
         * Returns the localStorage.
         * <p>
         * localStorage 를 반환한다.
         *
         * @return the manager / 관리자
         */
        public LocalStorageManager getLocalStorage() {
            return local;
        }

        /**
         * Returns the sessionStorage.
         * <p>
         * sessionStorage 를 반환한다.
         *
         * @return the storage / 저장소
         */
        public SessionStorage getSessionStorage() {
            return session;
        }

        /**
         * Returns the scripting flag.
         * <p>
         * 스크립트 사용 플래그를 반환한다.
         *
         * @return the flag / 플래그
         */
        public boolean isScriptingEnabled() {
            return scripting;
        }

        /**
         * Returns the time limit.
         * <p>
         * 시간 제한을 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public long getTaskTimeLimitMillis() {
            return timeLimit;
        }

        /**
         * Records a navigation.
         * <p>
         * 이동을 기록한다.
         *
         * @param url     the URL / URL
         * @param replace the replace flag / 바꾸기 플래그
         */
        public void navigate(String url, boolean replace) {
            navigations.add(replace ? url + " (replace)" : url);
        }

        /**
         * Records a reload.
         * <p>
         * 새로고침을 기록한다.
         */
        public void reload() {
            calls.add("reload");
        }

        /**
         * Records a history move.
         * <p>
         * 이력 이동을 기록한다.
         *
         * @param delta the steps / 칸 수
         */
        public void historyGo(int delta) {
            calls.add("go(" + delta + ")");
        }

        /**
         * Returns the image loading service.
         * <p>
         * 이미지 로딩 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public ImageRequestSource getImageRequestSource() {
            return images;
        }

        /**
         * Records a media state.
         * <p>
         * 미디어 상태를 기록한다.
         *
         * @param absoluteUrl the URL / URL
         * @param error       null if playable / 재생할 수 있으면 null
         */
        public void mediaStateChanged(String absoluteUrl, String error) {
            mediaStates.add(absoluteUrl + "=" + (error == null ? "ok" : error));
        }

        /**
         * The request service of the page (used by the requests of text tracks, fetch and so on), or null for none.
         * <p>
         * 페이지의 요청 서비스(텍스트 트랙, fetch 등의 요청이 쓴다). 없으면 null.
         */
        public WebRequestSource web;

        /**
         * Returns the request service of the page.
         * <p>
         * 페이지의 요청 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public WebRequestSource getWebRequestSource() {
            return web;
        }

        /**
         * Returns the media request service.
         * <p>
         * 미디어 요청 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public MediaRequestSource getMediaRequestSource() {
            return media;
        }

        /**
         * Returns the factory of the sound outputs.
         * <p>
         * 소리 출력의 팩터리를 반환한다.
         *
         * @return the factory or null / 팩터리 또는 null
         */
        public AudioOutputFactory getAudioOutputFactory() {
            return outputs;
        }

        /**
         * The factory of the outputs of the real-time Web Audio contexts of the page: silent outputs by default, so that a test of a page never makes a sound or opens a device; a test that
         * wants to see the audio that a context plays sets a recording factory.
         * <p>
         * 페이지의 실시간 Web Audio 컨텍스트의 출력 팩터리: 기본은 무음 출력이므로 페이지 시험이 소리를 내거나 장치를 열지 않는다. 컨텍스트가 재생하는 오디오를 보려는 시험은 기록하는
         * 팩터리를 정한다.
         */
        public org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory webAudioOutputs = org.duckdns.hjow.swingwebpanel.media.webaudio.OutputRenderDriver.silentOutputs();

        /**
         * Returns the factory of the outputs of the real-time Web Audio contexts.
         * <p>
         * 실시간 Web Audio 컨텍스트의 출력 팩터리를 반환한다.
         *
         * @return the factory / 팩터리
         */
        public org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory getWebAudioOutputFactory() {
            return webAudioOutputs;
        }

        /**
         * Returns the style resolver.
         * <p>
         * 스타일 리졸버를 반환한다.
         *
         * @return the resolver or null / 리졸버 또는 null
         */
        public StyleResolver getStyleResolver() {
            return cssEngine != null ? getCssEngine().getResolver() : resolver;
        }

        /**
         * The CSS engine of the page for the CSSOM tests (the test creates it after loading and calls <code>loadStyleSheets()</code>), or null.
         * <p>
         * CSSOM 시험을 위한 페이지의 CSS 엔진(시험이 로드한 뒤 만들고 <code>loadStyleSheets()</code> 를 부른다). 없으면 null.
         */
        public org.duckdns.hjow.swingwebpanel.css.CssEngine cssEngine;

        /**
         * Returns the CSS engine with the pending changes of the style elements applied, as the component does.
         * <p>
         * 컴포넌트처럼 style 요소의 대기 중인 변경을 적용한 CSS 엔진을 반환한다.
         *
         * @return the engine or null / 엔진 또는 null
         */
        public org.duckdns.hjow.swingwebpanel.css.CssEngine getCssEngine() {
            if (cssEngine != null) {
                cssEngine.refreshIfNeeded();
            }
            return cssEngine;
        }

        /**
         * Counts the change notifications.
         * <p>
         * 변경 통지를 센다.
         */
        public void documentChanged() {
            changes++;
        }

        /**
         * The images that the page may read back for a canvas, by absolute URL (a test puts decoded images here).
         * <p>
         * 페이지가 캔버스를 위해 읽을 수 있는 이미지(절대 URL별). 시험이 디코딩한 이미지를 넣는다.
         */
        public final Map<String, org.duckdns.hjow.swingwebpanel.render.ImageResource> loadedImages = new HashMap<String, org.duckdns.hjow.swingwebpanel.render.ImageResource>();

        /**
         * Number of canvasChanged calls.
         * <p>
         * canvasChanged 호출 수.
         */
        public int canvasChanges;

        /**
         * Returns an image that the test registered.
         * <p>
         * 시험이 등록한 이미지를 반환한다.
         *
         * @param absoluteUrl the URL / URL
         * @return the image or null / 이미지 또는 null
         */
        public org.duckdns.hjow.swingwebpanel.render.ImageResource getLoadedImage(String absoluteUrl) {
            return loadedImages.get(absoluteUrl);
        }

        /**
         * Counts the canvas change notifications.
         * <p>
         * 캔버스 변경 통지를 센다.
         *
         * @param canvas the canvas element / 캔버스 요소
         */
        public void canvasChanged(org.duckdns.hjow.swingwebpanel.dom.Element canvas) {
            canvasChanges++;
        }
    }

    /**
     * The environment.
     * <p>
     * 환경.
     */
    public final Env env = new Env();

    /**
     * The console lines as <code>level: text</code>.
     * <p>
     * 콘솔 줄(<code>level: text</code>).
     */
    public final List<String> console = new ArrayList<String>();

    /**
     * The document.
     * <p>
     * 문서.
     */
    public Document document;

    /**
     * The scripts of the page.
     * <p>
     * 페이지의 스크립트.
     */
    public PageScripts scripts;

    /**
     * Creates the fixture and its (not yet used) environment.
     * <p>
     * 시험 도구와 아직 쓰지 않은 환경을 만든다.
     */
    public PageFixture() {
    }

    /**
     * Loads a page.
     * <p>
     * 페이지를 로드한다.
     *
     * @param url  the document URL / 문서 URL
     * @param html the source / 원문
     * @return this fixture / 이 시험 도구
     */
    public PageFixture load(String url, String html) {
        start(url, html);
        settle();
        return this;
    }

    /**
     * Loads a page without waiting for the document to complete (for pages that wait for images that the test completes later).
     * <p>
     * 문서가 완료되기를 기다리지 않고 페이지를 로드한다(시험이 나중에 완료시키는 이미지를 기다리는 페이지용).
     *
     * @param url  the document URL / 문서 URL
     * @param html the source / 원문
     * @return this fixture / 이 시험 도구
     */
    public PageFixture start(String url, String html) {
        document = Document.createHtmlDocument();
        document.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(document);
        document.setUrl(url);
        scripts = new PageScripts(document, env);
        scripts.getRealm().setConsole(new ConsoleHook() {
            /**
             * Records a console line.
             * <p>
             * 콘솔 줄을 기록한다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                console.add(level + ": " + text);
            }
        });
        HtmlParser parser = new HtmlParser(document, html);
        parser.setScripting(env.isScriptingEnabled());
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
        return this;
    }

    /**
     * Waits for the scripts that finish loading after the parser (async and dynamically inserted ones). The component runs them on its
     * document queue; the fixture runs the manual scheduler instead, so the page is complete when this returns, as with a real page.
     * <p>
     * 파서가 끝난 뒤에 로딩을 마치는 스크립트(async, 동적으로 삽입한 스크립트)를 기다린다. 컴포넌트는 이를 문서 큐에서 실행하며, 시험 도구는 수동
     * 스케줄러를 돌려 이 메서드가 돌아올 때 실제 페이지처럼 문서가 완료된 상태가 되게 한다.
     */
    public void settle() {
        long end = System.currentTimeMillis() + 10000;
        // 문서가 완료 상태가 될 때까지 큐를 돌리며 짧게 쉰다. 폐기되었거나 스크립트가 꺼져 있으면 기다리지 않는다.
        while (!"complete".equals(scripts.getReadyState()) && !scripts.isDisposed() && env.isScriptingEnabled()
                && System.currentTimeMillis() < end) {
            env.scheduler.advance(0);
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Loads a page with the default URL.
     * <p>
     * 기본 URL 로 페이지를 로드한다.
     *
     * @param html the source / 원문
     * @return this fixture / 이 시험 도구
     */
    public PageFixture load(String html) {
        return load("http://example.test/dir/page.html", html);
    }

    /**
     * Moves the manual clock.
     * <p>
     * 수동 시계를 움직인다.
     *
     * @param ms the milliseconds / 밀리초
     */
    public void advance(long ms) {
        env.scheduler.advance(ms);
    }

    /**
     * Returns the console lines without the level, joined by commas (only lines of level log).
     * <p>
     * 수준을 뺀 콘솔 줄을 쉼표로 이어 반환한다(log 수준만).
     *
     * @return the joined text / 이어 붙인 문자열
     */
    public String log() {
        StringBuilder sb = new StringBuilder();
        // log 수준의 줄만 쉼표로 이어 붙인다.
        for (String line : console) {
            if (line.startsWith("log: ")) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(line.substring(5));
            }
        }
        return sb.toString();
    }
}
