package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.js.host.structured.HostMessageValues;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;

/**
 * What a page needs from the component around it: the user agent, how to fetch scripts, timers, cookies, storage, the viewport size, and
 * the hooks for navigation and repaint requests. Every method has a harmless default, so a test or an embedder overrides only what it
 * provides. The component creates one environment per page.
 * <p>
 * 페이지가 주변 컴포넌트에 필요로 하는 것: User-Agent, 스크립트를 가져오는 방법, 타이머, 쿠키, 저장소, 뷰포트 크기, 이동과 다시 그리기 요청용 훅.
 * 모든 메서드에 무해한 기본값이 있으므로 시험이나 임베더는 제공하는 것만 재정의한다. 컴포넌트가 페이지마다 환경 하나를 만든다.
 */
public abstract class HostEnvironment {

    /** Default duration of one browser script task in milliseconds. / 브라우저 스크립트 작업 하나의 기본 제한(밀리초). */
    public static final long DEFAULT_TASK_TIME_LIMIT_MILLIS = 10000L;

    /** Shared memory cluster for a standalone host environment. / 독립 호스트 환경의 공유 메모리 클러스터. */
    private final Object sharedMemoryCluster = new Object();

    /**
     * Creates the environment.
     * <p>
     * 환경을 만든다.
     */
    protected HostEnvironment() {
    }

    /**
     * Returns the stable cluster token for the component that owns this environment.
     * <p>
     * 이 환경을 소유한 컴포넌트의 고정 클러스터 토큰을 반환한다.
     *
     * @return standalone identity token / 독립 식별 토큰
     */
    public Object getSharedMemoryCluster() {
        return sharedMemoryCluster;
    }

    /**
     * Reports whether this document is allowed to use the isolated agent capabilities.
     * <p>
     * 이 문서가 격리된 에이전트의 권한을 사용할 수 있는지 알려 준다.
     *
     * @return true for an isolated document / 격리된 문서이면 true
     */
    public boolean isCrossOriginIsolated() {
        return false;
    }

    /**
     * Returns the policy inherited by requests and used to check worker responses.
     * <p>
     * 요청이 물려받고 워커 응답 검사에 사용하는 정책을 반환한다.
     *
     * @return the document embedder policy / 문서의 삽입 정책
     */
    public CrossOriginPolicy.Embedder getEmbedderPolicy() {
        return CrossOriginPolicy.Embedder.UNSAFE_NONE;
    }

    /**
     * Returns the user agent that <code>navigator.userAgent</code> reports.
     * <p>
     * <code>navigator.userAgent</code> 가 알리는 User-Agent 를 반환한다.
     *
     * @return the user agent / User-Agent
     */
    public String getUserAgent() {
        return "swingwebpanel/1.0";
    }

    /**
     * Returns the browsing context of the document: the top-level document of the component or a nested context of a frame. The scripts use
     * it for <code>parent</code>, <code>top</code>, <code>frames</code> and <code>frameElement</code>; the default is null, a document that
     * stands alone (it is its own parent and top).
     * <p>
     * 문서의 브라우징 문맥을 반환한다: 컴포넌트의 최상위 문서 또는 프레임의 중첩 문맥. 스크립트가 <code>parent</code>, <code>top</code>,
     * <code>frames</code>, <code>frameElement</code> 에 쓴다. 기본값은 null 이며 혼자 있는 문서(자기가 부모이자 최상위)이다.
     *
     * @return the context or null / 문맥 또는 null
     */
    public BrowsingContext getBrowsingContext() {
        return null;
    }

    /**
     * Returns the Java host message origin when this document belongs to a messaging component.
     * <p>
     * 이 문서가 메시지를 쓰는 컴포넌트에 있으면 Java 호스트 메시지의 오리진을 반환한다.
     *
     * @return host origin or null / 호스트 오리진 또는 null
     */
    public String getHostMessageOrigin() {
        return null;
    }

    /**
     * Offers a script message to the Java host.
     * <p>
     * Java 호스트에 스크립트 메시지를 전달한다.
     *
     * @param data copied Java data / 복사한 Java 데이터
     */
    public void publishHostMessage(HostMessageValues.JavaMessage data) {
    }

    /**
     * Returns the source of external scripts, or null if scripts cannot be fetched (they then fail with an <code>error</code> event).
     * <p>
     * 외부 스크립트의 공급원을 반환한다. 가져올 수 없으면 null 이며 스크립트는 <code>error</code> 이벤트로 실패한다.
     *
     * @return the source or null / 공급원 또는 null
     */
    public ScriptSource getScriptSource() {
        return null;
    }

    /**
     * Tells whether the document counts as visible to the user (<code>document.visibilityState</code> is <code>visible</code>, <code>document.hidden</code> is false): the
     * component that shows it is on the screen. The default is true.
     * <p>
     * 문서가 사용자에게 보이는 것으로 치는지 알려 준다(<code>document.visibilityState</code> 가 <code>visible</code>, <code>document.hidden</code> 이 false): 문서를 보여 주는
     * 컴포넌트가 화면에 있다. 기본값은 true.
     *
     * @return true while visible / 보이는 동안 true
     */
    public boolean isDocumentVisible() {
        return true;
    }

    /**
     * Tells what kind of navigation made the page (Navigation Timing: the <code>type</code> of the navigation entry): <code>navigate</code>, <code>reload</code>
     * or <code>back_forward</code>. The default is <code>navigate</code>.
     * <p>
     * 페이지를 만든 이동의 종류를 알려 준다(Navigation Timing 탐색 항목의 <code>type</code>): <code>navigate</code>, <code>reload</code>, <code>back_forward</code>.
     * 기본값은 <code>navigate</code>.
     *
     * @return the type / 종류
     */
    public String getNavigationType() {
        return "navigate";
    }

    /**
     * Tells whether the host has a network connection (<code>navigator.onLine</code>); the host reports a change through the
     * <code>online</code>/<code>offline</code> events of the window. The default is true.
     * <p>
     * 호스트에 네트워크 연결이 있는지 알려 준다(<code>navigator.onLine</code>). 호스트는 변화를 window 의 <code>online</code>/<code>offline</code> 이벤트로 알린다.
     * 기본값은 true.
     *
     * @return true while online / 연결된 동안 true
     */
    public boolean isOnline() {
        return true;
    }

    /**
     * Returns the pointer capture service, or null if the host has no pointer (<code>setPointerCapture</code> then fails with a
     * <code>NotFoundError</code> for every pointer id).
     * <p>
     * 포인터 캡처 서비스를 반환한다. 호스트에 포인터가 없으면 null 이며 <code>setPointerCapture</code> 는 모든 포인터 ID 에 대해
     * <code>NotFoundError</code> 로 실패한다.
     *
     * @return the service or null / 서비스 또는 null
     */
    public PointerService getPointerService() {
        return null;
    }

    /**
     * Returns the service for <code>XMLHttpRequest</code> and <code>fetch()</code>, or null if the page cannot make requests (they then
     * fail like a network error).
     * <p>
     * <code>XMLHttpRequest</code> 와 <code>fetch()</code> 용 서비스를 반환한다. 페이지가 요청을 할 수 없으면 null 이며 요청은 네트워크 오류처럼 실패한다.
     *
     * @return the service or null / 서비스 또는 null
     */
    public WebRequestSource getWebRequestSource() {
        return null;
    }

    /**
     * Returns the service that loads the images of <code>img</code> elements, or null if the page cannot load images (their
     * <code>load</code> and <code>error</code> events never fire and <code>complete</code> stays false).
     * <p>
     * <code>img</code> 요소의 이미지를 로드하는 서비스를 반환한다. 페이지가 이미지를 로드할 수 없으면 null 이며 <code>load</code>, <code>error</code>
     * 이벤트가 발생하지 않고 <code>complete</code> 는 false 로 남는다.
     *
     * @return the service or null / 서비스 또는 null
     */
    public ImageRequestSource getImageRequestSource() {
        return null;
    }

    /**
     * Returns the service that fetches the resources of <code>audio</code> and <code>video</code> elements, or null if the page cannot load
     * media (the elements then fail with an <code>error</code> event, as for an unsupported resource).
     * <p>
     * <code>audio</code> 와 <code>video</code> 요소의 리소스를 가져오는 서비스를 반환한다. 페이지가 미디어를 로드할 수 없으면 null 이며 요소는 지원하지
     * 않는 리소스처럼 <code>error</code> 이벤트로 실패한다.
     *
     * @return the service or null / 서비스 또는 null
     */
    public MediaRequestSource getMediaRequestSource() {
        return null;
    }

    /**
     * Returns the factory that opens the sound outputs for the media elements of the page, or null if there is no sound output (the elements can
     * load and seek but <code>play()</code> then fails with an <code>error</code> event).
     * <p>
     * 페이지의 미디어 요소를 위한 소리 출력을 여는 팩터리를 반환한다. 소리 출력이 없으면 null 이며 요소는 로드하고 탐색할 수 있지만
     * <code>play()</code> 는 <code>error</code> 이벤트로 실패한다.
     *
     * @return the factory or null / 팩터리 또는 null
     */
    public AudioOutputFactory getAudioOutputFactory() {
        return null;
    }

    /**
     * Returns the factory that opens the outputs of the real-time <code>AudioContext</code> objects of the page (Web Audio, plan item P25-07), or null for the system output
     * (Java Sound, with a silent output that keeps the time running when there is no device or it cannot be opened). It is the Web Audio counterpart of
     * {@link #getAudioOutputFactory()}: the two are chosen independently, because the outputs differ in kind (a Web Audio output takes floating point audio and blocks until the device has
     * room, the output of a media element takes 16 bit PCM and never blocks).
     * <p>
     * 페이지의 실시간 <code>AudioContext</code> 의 출력을 여는 팩터리를 반환한다(Web Audio, 계획 P25-07). 없으면 null 이며 시스템 출력(Java Sound. 장치가 없거나 열 수 없으면 시간을 계속
     * 흐르게 하는 무음 출력)을 쓴다. {@link #getAudioOutputFactory()} 의 Web Audio 짝이다: 둘은 따로 고른다. 출력의 성격이 다르기 때문이다(Web Audio 출력은 부동 소수점 오디오를 받고
     * 장치에 빈 곳이 생길 때까지 기다리며, 미디어 요소의 출력은 16비트 PCM 을 받고 기다리지 않는다).
     *
     * @return the factory or null for the system output / 팩터리. 시스템 출력이면 null
     */
    public OutputFactory getWebAudioOutputFactory() {
        return null;
    }

    /**
     * Tells the component what a media element found out about a resource, so that the renderer can show it: the resource can be played
     * (<code>error</code> is null) or cannot (a failed fetch, an unsupported or damaged format). Called on the thread that owns the DOM; the
     * default does nothing.
     * <p>
     * 미디어 요소가 리소스에 대해 알아낸 것을 컴포넌트에 알려 렌더러가 보여 줄 수 있게 한다: 리소스를 재생할 수 있거나(<code>error</code> 가 null)
     * 재생할 수 없다(가져오기 실패, 지원하지 않거나 손상된 형식). DOM 을 소유한 스레드에서 호출되며 기본은 아무것도 하지 않는다.
     *
     * @param absoluteUrl the absolute URL of the resource / 리소스의 절대 URL
     * @param error       null if it can be played, else a short description of the failure / 재생할 수 있으면 null, 아니면 실패의 짧은 설명
     */
    public void mediaStateChanged(String absoluteUrl, String error) {
    }

    /**
     * Returns the state and pixels of an image that the page already requested (for the <code>drawImage()</code> of a canvas), without
     * starting a request. Called on the thread that owns the DOM; the default knows no images.
     * <p>
     * 페이지가 이미 요청한 이미지의 상태와 화소를 요청을 시작하지 않고 반환한다(캔버스의 <code>drawImage()</code> 용). DOM 을 소유한 스레드에서 호출되며
     * 기본은 이미지를 모른다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @return the image, or null if it was never requested / 이미지. 요청한 적이 없으면 null
     */
    public org.duckdns.hjow.swingwebpanel.render.ImageResource getLoadedImage(String absoluteUrl) {
        return null;
    }

    /**
     * Returns the state and the pixels of an image that an element with a <code>crossorigin</code> attribute already requested in CORS mode, without
     * starting a request. The default gives the plain image of {@link #getLoadedImage(String)}.
     * <p>
     * <code>crossorigin</code> 속성이 있는 요소가 이미 CORS 모드로 요청한 이미지의 상태와 화소를 요청을 시작하지 않고 반환한다. 기본은
     * {@link #getLoadedImage(String)} 의 일반 이미지를 준다.
     *
     * @param absoluteUrl the absolute URL of the image / 이미지의 절대 URL
     * @param cors        <code>anonymous</code>, <code>use-credentials</code> or null for a plain image / <code>anonymous</code>, <code>use-credentials</code>. 일반 이미지는 null
     * @return the image, or null if it was never requested / 이미지. 요청한 적이 없으면 null
     */
    public org.duckdns.hjow.swingwebpanel.render.ImageResource getLoadedImage(String absoluteUrl, String cors) {
        return getLoadedImage(absoluteUrl);
    }

    /**
     * Tells the component that the picture of a canvas changed, so that the renderer paints it again (without laying the page out again).
     * It only posts work and may be called while the canvas is locked; the default does nothing.
     * <p>
     * 캔버스의 그림이 바뀌었음을 컴포넌트에 알려 렌더러가 (페이지를 다시 배치하지 않고) 다시 그리게 한다. 작업을 올리기만 하며 캔버스가 잠긴 채 불릴 수
     * 있다. 기본은 아무것도 하지 않는다.
     *
     * @param canvas the <code>canvas</code> element / <code>canvas</code> 요소
     */
    public void canvasChanged(Element canvas) {
    }

    /**
     * Tells the component that what the control bar of an <code>audio</code> element shows changed (the position, play or pause, the
     * volume), so that the renderer paints it again without laying the page out again. It only posts work; the default does nothing.
     * <p>
     * <code>audio</code> 요소의 컨트롤 막대가 보여 주는 것(위치, 재생이나 일시정지, 음량)이 바뀌었음을 컴포넌트에 알려, 렌더러가 페이지를 다시 배치하지
     * 않고 다시 그리게 한다. 작업을 올리기만 하며 기본은 아무것도 하지 않는다.
     *
     * @param media the <code>audio</code> element / <code>audio</code> 요소
     */
    public void mediaControlsChanged(Element media) {
    }

    /**
     * Returns the scheduler for timers and animation frames; null means that timers never fire.
     * <p>
     * 타이머와 애니메이션 프레임용 스케줄러를 반환한다. null 이면 타이머가 절대 실행되지 않는다.
     *
     * @return the scheduler or null / 스케줄러 또는 null
     */
    public Scheduler getScheduler() {
        return null;
    }

    /**
     * Returns the component's object URL scope for this document if it has one.
     * <p>
     * 컴포넌트가 이 문서의 객체 URL 범위를 제공하면 반환한다.
     *
     * @return the document scope or null / 문서 범위 또는 null
     */
    public BlobUrlStore.Scope getBlobUrlScope() {
        return null;
    }

    /**
     * Receives the result of an asynchronous clipboard text operation on the document thread.
     * <p>
     * 비동기 클립보드 텍스트 작업 결과를 문서 스레드에서 받는다.
     */
    public interface ClipboardResult {
        /**
         * Reports the text or the unavailability of the clipboard.
         * <p>
         * 텍스트 또는 클립보드를 사용할 수 없음을 알린다.
         *
         * @param text text for a read, or empty for a successful write / 읽은 텍스트. 쓰기에 성공하면 빈 문자열
         * @param unavailable true when the host could not access the clipboard / 호스트가 클립보드에 접근하지 못하면 true
         */
        void complete(String text, boolean unavailable);
    }

    /**
     * Requests access to plain clipboard text after the caller checked user activation.
     * <p>
     * 호출자가 사용자 활성화를 검증한 뒤 클립보드의 일반 텍스트 접근을 요청한다.
     *
     * @param write text to write, or null for a read / 쓸 텍스트. 읽으려면 null
     * @param result completion on the document thread / 문서 스레드에서 받을 완료 결과
     */
    public void requestClipboardText(String write, ClipboardResult result) {
        result.complete("", true);
    }

    /**
     * Returns the cookie store of the component, or null for no cookies.
     * <p>
     * 컴포넌트의 쿠키 저장소를 반환한다. 쿠키가 없으면 null.
     *
     * @return the store or null / 저장소 또는 null
     */
    public CookieStore getCookies() {
        return null;
    }

    /**
     * Returns the process-wide localStorage, or null if the page has none.
     * <p>
     * 프로세스 공용 localStorage 를 반환한다. 페이지에 없으면 null.
     *
     * @return the manager or null / 관리자 또는 null
     */
    public LocalStorageManager getLocalStorage() {
        return null;
    }

    /**
     * Returns the sessionStorage of the component, or null if the page has none.
     * <p>
     * 컴포넌트의 sessionStorage 를 반환한다. 페이지에 없으면 null.
     *
     * @return the storage or null / 저장소 또는 null
     */
    public SessionStorage getSessionStorage() {
        return null;
    }

    /**
     * Returns the viewport width in pixels (<code>innerWidth</code>).
     * <p>
     * 뷰포트 폭(픽셀)을 반환한다(<code>innerWidth</code>).
     *
     * @return the width / 폭
     */
    public int getViewportWidth() {
        return 800;
    }

    /**
     * Returns the viewport height in pixels (<code>innerHeight</code>).
     * <p>
     * 뷰포트 높이(픽셀)를 반환한다(<code>innerHeight</code>).
     *
     * @return the height / 높이
     */
    public int getViewportHeight() {
        return 600;
    }

    /**
     * Tells whether scripts run (the component's <code>setJavascriptEnabled</code>).
     * <p>
     * 스크립트를 실행하는지 알려 준다(컴포넌트의 <code>setJavascriptEnabled</code>).
     *
     * @return true if scripts run / 스크립트를 실행하면 true
     */
    public boolean isScriptingEnabled() {
        return true;
    }

    /**
     * Returns the time limit of one script task in milliseconds (0 for none); a script that runs longer is aborted.
     * <p>
     * 스크립트 작업 하나의 시간 제한(밀리초)을 반환한다(0 이면 없음). 더 오래 실행되는 스크립트는 중단된다.
     *
     * @return the limit / 제한
     */
    public long getTaskTimeLimitMillis() {
        return DEFAULT_TASK_TIME_LIMIT_MILLIS;
    }

    /**
     * Returns the URL of the referring page (<code>document.referrer</code>).
     * <p>
     * 참조 페이지 URL 을 반환한다(<code>document.referrer</code>).
     *
     * @return the URL or "" / URL 또는 ""
     */
    public String getReferrer() {
        return "";
    }

    /**
     * Asks the component to navigate (<code>location.href = ...</code>, <code>location.assign</code>, <code>replace</code>); the target is
     * checked by the component's navigation policy, never by the page.
     * <p>
     * 컴포넌트에 이동을 요청한다(<code>location.href = ...</code>, <code>location.assign</code>, <code>replace</code>). 대상은 페이지가 아니라
     * 컴포넌트의 이동 정책이 검사한다.
     *
     * @param url     the absolute URL / 절대 URL
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    public void navigate(String url, boolean replace) {
    }

    /**
     * Performs the object URL download action of an uncancelled synthetic anchor click.
     * <p>
     * 취소하지 않은 합성 앵커 클릭의 객체 URL 다운로드 기본 동작을 수행한다.
     *
     * @param link the anchor element / 앵커 요소
     */
    public void activateObjectDownload(org.duckdns.hjow.swingwebpanel.dom.Element link) {
        // 독립 스크립트 호스트에는 다운로드 기능이 없다.
    }

    /**
     * Asks the component to reload the page.
     * <p>
     * 컴포넌트에 페이지 새로고침을 요청한다.
     */
    public void reload() {
    }

    /**
     * Asks the component to move in the history (<code>history.go</code>, <code>back</code>, <code>forward</code>).
     * <p>
     * 컴포넌트에 이력 이동을 요청한다(<code>history.go</code>, <code>back</code>, <code>forward</code>).
     *
     * @param delta the number of steps, negative for back / 이동 칸 수(뒤로는 음수)
     */
    public void historyGo(int delta) {
    }

    /**
     * Applies <code>history.pushState</code> or <code>replaceState</code> of the page: the history entry of the page takes the new URL and
     * the serialized state object, without a request and without a navigation (the page itself has already changed its own document URL and
     * its <code>history.state</code>). The default does nothing.
     * <p>
     * 페이지의 <code>history.pushState</code>, <code>replaceState</code> 를 적용한다: 페이지의 이력 항목이 요청이나 이동 없이 새 URL과 직렬화된 상태
     * 객체를 갖는다(페이지는 이미 자기 문서 URL과 <code>history.state</code> 를 바꾸었다). 기본은 아무것도 하지 않는다.
     *
     * @param state   the serialized state object, or null / 직렬화된 상태 객체. 없으면 null
     * @param url     the new absolute URL (of the origin of the document) / 새 절대 URL(문서와 같은 오리진)
     * @param replace true to replace the current entry, false to add one / 현재 항목을 바꾸려면 true, 추가하려면 false
     */
    public void updateHistoryState(ClonedValue state, String url, boolean replace) {
    }

    /**
     * Returns the number of entries of the session history (<code>history.length</code>); the default is 1, the page alone.
     * <p>
     * 세션 이력의 항목 수를 반환한다(<code>history.length</code>). 기본값은 페이지 하나뿐인 1.
     *
     * @return the number of entries / 항목 수
     */
    public int getHistoryLength() {
        return 1;
    }

    /**
     * Called after the document changed so that the component can schedule layout and repaint; may be called often, so it must be cheap.
     * <p>
     * 문서가 바뀐 뒤에 호출되어 컴포넌트가 배치와 다시 그리기를 예약할 수 있게 한다. 자주 호출될 수 있으므로 가벼워야 한다.
     */
    public void documentChanged() {
    }

    /**
     * Called after a script animation (Web Animations, <code>element.animate()</code>) changed the animated style of the page, so that the component
     * renders again. The default asks for a new rendering through {@link #canvasChanged(Element)}, which renders the page and ticks its animation
     * engine; the engine reports the changed styles of the script animations, so the page is laid out again. Cheap, may be called often.
     * <p>
     * 스크립트 애니메이션(Web Animations, <code>element.animate()</code>)이 페이지의 애니메이션 스타일을 바꾼 뒤 컴포넌트가 다시 렌더링하도록 불린다. 기본 구현은
     * {@link #canvasChanged(Element)} 로 새 렌더링을 요청하며, 그 렌더링이 애니메이션 엔진을 진행하고 엔진이 스크립트 애니메이션의 바뀐 스타일을 알려 페이지가 다시
     * 배치된다. 가벼우며 자주 불릴 수 있다.
     */
    public void animationsChanged() {
        canvasChanged(null);
    }

    /**
     * Tells whether sub-resources that the component started for this page (stylesheets and images) are still loading. The window
     * <code>load</code> event waits for them; the default is false, for hosts that load nothing.
     * <p>
     * 컴포넌트가 이 페이지를 위해 시작한 하위 리소스(스타일시트와 이미지)가 아직 로딩 중인지 알려 준다. window <code>load</code> 이벤트는 이것을
     * 기다린다. 기본값은 false 이며 아무것도 로드하지 않는 호스트를 위한 것이다.
     *
     * @return true while one is loading / 로딩 중인 것이 있으면 true
     */
    public boolean hasPendingResources() {
        return false;
    }

    /**
     * Called once, on the thread that owns the DOM, after the window <code>load</code> event of the page was fired. The component uses it to
     * finish the progress of the load.
     * <p>
     * 페이지의 window <code>load</code> 이벤트가 발생한 뒤 DOM 을 소유한 스레드에서 한 번 호출된다. 컴포넌트는 이것으로 로딩 진행률을 마무리한다.
     */
    public void pageLoaded() {
    }

    /**
     * Waits for preceding parser-blocking styles before a classic parser/deferred script. Called on the loader owner,
     * before the script task/time limit begins; async and dynamically inserted scripts do not call this hook.
     * <p>일반 파서·defer 스크립트 전에 앞선 파서 차단 스타일을 기다린다. 스크립트 작업·시간 제한 시작 전 로더 소유자에서 호출하며
     * async와 동적 삽입 스크립트는 이 훅을 호출하지 않는다.
     * @param script parser-created script about to run / 실행 직전 파서 생성 스크립트
     */
    public void prepareParserScript(Element script) {
    }

    /**
     * Returns the style resolver of the page (the component creates a CSS engine per page), or null if styles are not computed; used by
     * <code>getComputedStyle</code>.
     * <p>
     * 페이지의 스타일 리졸버를 반환한다(컴포넌트가 페이지마다 CSS 엔진을 만든다). 스타일을 계산하지 않으면 null. <code>getComputedStyle</code> 이 쓴다.
     *
     * @return the resolver or null / 리졸버 또는 null
     */
    public StyleResolver getStyleResolver() {
        return null;
    }

    /**
     * Returns the CSS engine of the page, the owner of the style sheets that the resolver uses (the component creates one per page); the engine has
     * the pending changes of the document applied (a <code>style</code> element that was added is collected). The CSSOM of the page
     * (<code>document.styleSheets</code>, the rules, <code>adoptedStyleSheets</code>) works on the sheets of this engine, so a change that a script makes
     * to a rule changes the styles that the resolver computes. Null if styles are not computed.
     * <p>
     * 페이지의 CSS 엔진을 반환한다. 리졸버가 쓰는 스타일 시트의 소유자이다(컴포넌트가 페이지마다 만든다). 엔진에는 문서의 대기 중인 변경이 적용되어 있다(추가된
     * <code>style</code> 요소를 수집한다). 페이지의 CSSOM(<code>document.styleSheets</code>, 규칙, <code>adoptedStyleSheets</code>)은 이 엔진의 시트에서 일하므로
     * 스크립트가 규칙에 한 변경이 리졸버가 계산하는 스타일을 바꾼다. 스타일을 계산하지 않으면 null.
     *
     * @return the engine or null / 엔진 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.css.CssEngine getCssEngine() {
        return null;
    }

    /**
     * Returns the border box of an element in viewport pixels as <code>{x, y, width, height}</code> (the layout hook for
     * <code>getBoundingClientRect</code> and the <code>offset*</code> sizes); all zero until layout exists.
     * <p>
     * 요소의 테두리 상자를 뷰포트 픽셀의 <code>{x, y, width, height}</code> 로 반환한다(<code>getBoundingClientRect</code> 와 <code>offset*</code> 크기의
     * 레이아웃 훅). 레이아웃이 생기기 전에는 모두 0.
     *
     * @param element the element / 요소
     * @return four numbers / 숫자 네 개
     */
    public double[] getElementBounds(Element element) {
        return new double[4];
    }

    /**
     * Returns the client rectangles of a range in viewport pixels, each as <code>{x, y, width, height}</code> (the layout hook for
     * <code>Range.getClientRects</code>): the real geometry of the selected characters, one rectangle per line, and the border boxes of
     * the elements that the range selects. Null means that the host has no layout, and the caller falls back to an approximation from
     * element boxes.
     * <p>
     * 범위의 클라이언트 사각형을 뷰포트 픽셀의 <code>{x, y, 폭, 높이}</code> 로 반환한다(<code>Range.getClientRects</code> 의 레이아웃 훅): 선택된 글자의
     * 실제 기하(줄마다 하나)와 범위가 선택한 요소의 border box. null 이면 호스트에 레이아웃이 없는 것이며 호출자는 요소 상자로 근사한다.
     *
     * @param range the range / 범위
     * @return the rectangles or null / 사각형 목록. 없으면 null
     */
    public double[][] getRangeRects(org.duckdns.hjow.swingwebpanel.dom.Range range) {
        return null;
    }

    /**
     * Returns the text of a range the way copying it gives it (rendered text: collapsed white space, line feeds between blocks), the
     * meaning of <code>Selection.toString()</code>; null if the host has no layout.
     * <p>
     * 범위를 복사했을 때 나오는 텍스트(렌더링된 텍스트: 접힌 공백, 블록 사이의 줄바꿈)를 반환한다. <code>Selection.toString()</code> 의 뜻이다. 호스트에
     * 레이아웃이 없으면 null.
     *
     * @param range the range / 범위
     * @return the text or null / 텍스트. 없으면 null
     */
    public String getRangeText(org.duckdns.hjow.swingwebpanel.dom.Range range) {
        return null;
    }

    /**
     * Reads the rendered text of an element, or null without a layout. / 요소의 렌더링 텍스트를 읽는다. 배치가 없으면 null을 반환한다.
     * @param element the element whose descendants are read / 자손을 읽을 요소
     * @return the text or null / 텍스트 또는 null
     */
    public String getInnerText(Element element) {
        return null;
    }

    /**
     * Returns the border boxes of an element in viewport pixels, one <code>{x, y, width, height}</code> per box (the layout hook for
     * <code>getClientRects</code>; an inline element that wraps has one per line). Null means that the host has no layout.
     * <p>
     * 요소의 border box 를 뷰포트 픽셀의 상자마다 하나씩 <code>{x, y, 폭, 높이}</code> 로 반환한다(<code>getClientRects</code> 의 레이아웃 훅. 줄바꿈된
     * 인라인 요소는 줄마다 하나). null 이면 호스트에 레이아웃이 없다.
     *
     * @param element the element / 요소
     * @return the rectangles or null / 사각형 목록. 없으면 null
     */
    public double[][] getElementRects(Element element) {
        return null;
    }

    /**
     * Returns the topmost element at a point of the viewport in viewport pixels (the layout hook for <code>document.elementFromPoint</code>),
     * or null when the point is outside the viewport or no layout exists.
     * <p>
     * 뷰포트 픽셀 좌표의 점에 있는 최상위 요소를 반환한다(<code>document.elementFromPoint</code> 의 레이아웃 훅). 점이 뷰포트 밖이거나 레이아웃이
     * 없으면 null.
     *
     * @param x the x of the point / 점의 x
     * @param y the y of the point / 점의 y
     * @return the element or null / 요소 또는 null
     */
    public Element getElementAt(double x, double y) {
        return null;
    }

    /**
     * Tells whether a media query (<code>window.matchMedia</code>) matches the page now. The default evaluates it against the size of the
     * viewport with the default environment (screen, 24 bit colour, 1 dppx).
     * <p>
     * 미디어 질의(<code>window.matchMedia</code>)가 지금 페이지에 일치하는지 알려 준다. 기본 구현은 기본 환경(screen, 24비트 색, 1 dppx)과 뷰포트
     * 크기로 평가한다.
     *
     * @param query the media query list text / 미디어 질의 목록 문자열
     * @return true if it matches / 일치하면 true
     */
    public boolean matchesMedia(String query) {
        MediaEnvironment env = new MediaEnvironment(getViewportWidth(), getViewportHeight());
        return MediaQueryList.parse(query).matches(env);
    }

    /**
     * Returns the horizontal scroll position of the viewport in document pixels (<code>scrollX</code>); 0 without a scrolling viewport.
     * <p>
     * 뷰포트의 가로 스크롤 위치를 문서 픽셀로 반환한다(<code>scrollX</code>). 스크롤하는 뷰포트가 없으면 0.
     *
     * @return the position / 위치
     */
    public int getScrollX() {
        return 0;
    }

    /**
     * Returns the vertical scroll position of the viewport in document pixels (<code>scrollY</code>); 0 without a scrolling viewport.
     * <p>
     * 뷰포트의 세로 스크롤 위치를 문서 픽셀로 반환한다(<code>scrollY</code>). 스크롤하는 뷰포트가 없으면 0.
     *
     * @return the position / 위치
     */
    public int getScrollY() {
        return 0;
    }

    /**
     * Asks the component to scroll the viewport so that a point of the document is at its top left (<code>scrollTo</code>); the position is
     * clamped to the content and nothing happens without a scrolling viewport. The scroll itself happens later on the event dispatch thread.
     * <p>
     * 문서의 한 점이 뷰포트의 왼쪽 위에 오도록 스크롤하라고 컴포넌트에 요청한다(<code>scrollTo</code>). 위치는 내용 범위로 제한되며 스크롤하는
     * 뷰포트가 없으면 아무 일도 없다. 실제 스크롤은 나중에 이벤트 디스패치 스레드에서 일어난다.
     *
     * @param x the x in document pixels / 문서 픽셀의 x
     * @param y the y in document pixels / 문서 픽셀의 y
     */
    public void scrollTo(double x, double y) {
    }
}
