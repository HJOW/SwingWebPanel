package org.duckdns.hjow.swingwebpanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.duckdns.hjow.swingwebpanel.core.BrowserSession;
import org.duckdns.hjow.swingwebpanel.core.ConsoleDispatcher;
import org.duckdns.hjow.swingwebpanel.core.WebMessageDispatcher;
import org.duckdns.hjow.swingwebpanel.core.ClipboardSink;
import org.duckdns.hjow.swingwebpanel.core.ControlOverlays;
import org.duckdns.hjow.swingwebpanel.core.DocumentLoop;
import org.duckdns.hjow.swingwebpanel.core.OpenRequest;
import org.duckdns.hjow.swingwebpanel.core.WindowOpener;
import org.duckdns.hjow.swingwebpanel.core.FontDefaults;
import org.duckdns.hjow.swingwebpanel.core.FormSubmitSink;
import org.duckdns.hjow.swingwebpanel.core.UploadChooser;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.download.DownloadControl;
import org.duckdns.hjow.swingwebpanel.download.DownloadHandle;
import org.duckdns.hjow.swingwebpanel.download.DownloadRequest;
import org.duckdns.hjow.swingwebpanel.download.DownloadStatusSink;
import org.duckdns.hjow.swingwebpanel.download.SwingDownloadDialog;
import org.duckdns.hjow.swingwebpanel.download.SwingDownloadHandler;
import org.duckdns.hjow.swingwebpanel.core.NetworkPageLoader;
import org.duckdns.hjow.swingwebpanel.core.ParsingPageLoader;
import org.duckdns.hjow.swingwebpanel.core.HyperlinkDispatcher;
import org.duckdns.hjow.swingwebpanel.core.PageLoader;
import org.duckdns.hjow.swingwebpanel.core.PageSnapshot;
import org.duckdns.hjow.swingwebpanel.core.PageView;
import org.duckdns.hjow.swingwebpanel.core.PageInput;
import org.duckdns.hjow.swingwebpanel.core.PageKeyboard;
import org.duckdns.hjow.swingwebpanel.core.PageZoom;
import org.duckdns.hjow.swingwebpanel.core.SessionListener;
import org.duckdns.hjow.swingwebpanel.core.ViewMetrics;
import org.duckdns.hjow.swingwebpanel.core.ZoomControl;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.JavaSoundOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.SystemOutputFactory;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.HttpCache;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.SslConfigurer;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;

/**
 * A web browser component for Swing, written in pure Java 6 without external libraries. Put it in a window directly, or wrap it
 * in a <code>JScrollPane</code> to get scrolling: <code>new JScrollPane(new SwingWebPanel())</code>; the component never creates
 * a scroll pane of its own.
 * <p>
 * Basic use: <code>panel.setPage("http://example.com/")</code> starts an asynchronous navigation and returns at once;
 * <code>panel.setContent("&lt;h1&gt;Hi&lt;/h1&gt;", "text/html")</code> shows content you already have. Query methods return the last
 * <em>confirmed</em> state, so while a page is loading they still describe the previous page. All public methods can be
 * called from the event dispatch thread or from any other thread; property changes ("page", "statusText", "progress",
 * "title") are announced on the event dispatch thread. Call {@link #dispose()} when the component is no longer needed. The
 * full contract of every method is in <code>docs/API_CONTRACT.md</code>.
 * <p>
 * Scrolling policy (<code>Scrollable</code>): the content tracks the viewport width as long as the laid-out content can fit
 * (its minimum width is not larger than the viewport); only when the page needs more width than the viewport offers does the
 * view keep its own width and a horizontal scroll bar appear. The view tracks the viewport height when the content is shorter than
 * the viewport, otherwise it scrolls vertically. One scroll step is the line height reported by the layout engine (16 pixels
 * until one exists); a block step is the visible size minus one step.
 * <p>
 * Swing 용 웹 브라우저 컴포넌트. 외부 라이브러리 없이 순수 Java 6 로 작성했다. 창에 직접 넣거나 <code>JScrollPane</code> 으로 감싸
 * 스크롤을 얻는다: <code>new JScrollPane(new SwingWebPanel())</code>. 컴포넌트가 자체 스크롤 창을 만들지는 않는다.
 * <p>
 * 기본 사용: <code>panel.setPage("http://example.com/")</code> 는 비동기 이동을 시작하고 즉시 반환한다.
 * <code>panel.setContent("&lt;h1&gt;Hi&lt;/h1&gt;", "text/html")</code> 은 이미 가진 콘텐츠를 보여 준다. 조회 메서드는 마지막
 * <em>확정된</em> 상태를 반환하므로 로딩 중에는 이전 페이지를 설명한다. 모든 공개 메서드는 이벤트 디스패치 스레드나 다른 어떤
 * 스레드에서도 호출할 수 있고, 속성 변경("page", "statusText", "progress", "title")은 이벤트 디스패치 스레드에서 통지한다.
 * 컴포넌트가 더 필요 없으면 {@link #dispose()} 를 호출한다. 모든 메서드의 상세 계약은 <code>docs/API_CONTRACT.md</code> 에 있다.
 * <p>
 * 스크롤 정책(<code>Scrollable</code>): 배치된 콘텐츠가 들어갈 수 있는 동안(최소 폭이 뷰포트보다 크지 않은 동안)에는 콘텐츠가 뷰포트
 * 폭을 따라간다. 페이지가 뷰포트보다 넓은 폭을 필요로 할 때만 자기 폭을 유지하며 가로 스크롤 막대가 나타난다. 콘텐츠가 뷰포트보다 낮으면
 * 뷰포트 높이를 따라가고, 아니면 세로로 스크롤한다. 스크롤 한 단위는 레이아웃 엔진이 알려 주는 줄 높이(엔진이 생기기 전에는 16 픽셀)이고,
 * 블록 단위는 보이는 크기에서 한 단위를 뺀 값이다.
 */
public class SwingWebPanel extends JPanel implements Scrollable {

    /**
     * Serialization version id (the component is not meant to be serialized).
     * <p>
     * 직렬화 버전 식별자(이 컴포넌트는 직렬화를 전제로 하지 않는다).
     */
    private static final long serialVersionUID = 1L;

    /**
     * Name of the property that announces the URL of the confirmed page.
     * <p>
     * 확정된 페이지의 URL 변경을 알리는 속성 이름.
     */
    public static final String PROPERTY_PAGE = "page";

    /** Origin of messages from the Java host. / Java 호스트에서 온 메시지의 오리진. */
    public static final String HOST_ORIGIN = "swingwebpanel://host";

    /**
     * Name of the property that announces the status bar text.
     * <p>
     * 상태바 메시지 변경을 알리는 속성 이름.
     */
    public static final String PROPERTY_STATUS_TEXT = "statusText";

    /**
     * Name of the property that announces the progress value.
     * <p>
     * 진행률 변경을 알리는 속성 이름.
     */
    public static final String PROPERTY_PROGRESS = "progress";

    /**
     * Name of the property that announces the document title.
     * <p>
     * 문서 제목 변경을 알리는 속성 이름.
     */
    public static final String PROPERTY_TITLE = "title";

    /**
     * Name of the property that announces the page zoom factor ({@link #setZoom(double)}); the values are <code>Double</code>.
     * <p>
     * 페이지 확대 배율({@link #setZoom(double)}) 변경을 알리는 속성 이름. 값은 <code>Double</code> 이다.
     */
    public static final String PROPERTY_ZOOM = "zoom";

    /**
     * Name of the property that announces a change of {@link #setRootWindowClosingEnabled(boolean)}; the values are <code>Boolean</code>.
     * <p>
     * {@link #setRootWindowClosingEnabled(boolean)} 의 변경을 알리는 속성 이름. 값은 <code>Boolean</code> 이다.
     */
    public static final String PROPERTY_ROOT_WINDOW_CLOSING_ENABLED = "rootWindowClosingEnabled";

    /**
     * Name of the property that announces a change of the default fonts ({@link #setDefaultFontFamily(String, String)},
     * {@link #setDefaultFontSize(int)}, {@link #setDefaultMonospaceFontSize(int)}); the values are one-line descriptions (<code>String</code>).
     * <p>
     * 기본 글꼴({@link #setDefaultFontFamily(String, String)}, {@link #setDefaultFontSize(int)}, {@link #setDefaultMonospaceFontSize(int)})의
     * 변경을 알리는 속성 이름. 값은 한 줄 설명(<code>String</code>)이다.
     */
    public static final String PROPERTY_DEFAULT_FONTS = "defaultFonts";

    /**
     * Name of the property that announces a change of {@link #setPopupBlockingEnabled(boolean)}; the values are <code>Boolean</code>.
     * <p>
     * {@link #setPopupBlockingEnabled(boolean)} 의 변경을 알리는 속성 이름. 값은 <code>Boolean</code> 이다.
     */
    public static final String PROPERTY_POPUP_BLOCKING_ENABLED = "popupBlockingEnabled";

    /**
     * Name of the property that announces a new result of the search in the page ({@link #find(String, boolean, boolean)}); the values
     * are {@link FindResult} objects.
     * <p>
     * 페이지 안 찾기({@link #find(String, boolean, boolean)})의 새 결과를 알리는 속성 이름. 값은 {@link FindResult} 객체이다.
     */
    public static final String PROPERTY_FIND_RESULT = "findResult";

    /** Kind of default font: the font of content whose author named none. / 기본 글꼴의 종류: 작성자가 글꼴을 지정하지 않은 내용의 글꼴. */
    public static final String FONT_STANDARD = "standard";

    /** Kind of default font: the font behind the generic name <code>serif</code>. / 기본 글꼴의 종류: 일반 이름 <code>serif</code> 뒤의 글꼴. */
    public static final String FONT_SERIF = "serif";

    /** Kind of default font: the font behind the generic name <code>sans-serif</code>. / 기본 글꼴의 종류: 일반 이름 <code>sans-serif</code> 뒤의 글꼴. */
    public static final String FONT_SANS_SERIF = "sans-serif";

    /** Kind of default font: the font behind the generic name <code>monospace</code>. / 기본 글꼴의 종류: 일반 이름 <code>monospace</code> 뒤의 글꼴. */
    public static final String FONT_MONOSPACE = "monospace";

    /**
     * Preferred width of an empty page in pixels.
     * <p>
     * 빈 페이지의 선호 폭(픽셀).
     */
    private static final int DEFAULT_WIDTH = 640;

    /**
     * Preferred height of an empty page in pixels.
     * <p>
     * 빈 페이지의 선호 높이(픽셀).
     */
    private static final int DEFAULT_HEIGHT = 480;

    /**
     * All state and behavior of the component.
     * <p>
     * 컴포넌트의 모든 상태와 동작.
     */
    private final transient BrowserSession session;

    /**
     * Hyperlink listeners of the host.
     * <p>
     * 호스트의 하이퍼링크 리스너.
     */
    private final transient HyperlinkDispatcher links = new HyperlinkDispatcher();

    /**
     * Console listeners of the host.
     * <p>
     * 호스트의 콘솔 리스너.
     */
    private final transient ConsoleDispatcher consoles = new ConsoleDispatcher(this);

    /** Host web message registry and delivery queue. / 호스트 웹 메시지 등록부와 전달 큐. */
    private final transient WebMessageDispatcher webMessages = new WebMessageDispatcher(this, consoles);

    /**
     * Size information from the layout engine.
     * <p>
     * 레이아웃 엔진이 알려 주는 크기 정보.
     */
    private final transient ViewMetrics metrics = new ViewMetrics();

    /** Document-thread layout and EDT drawing bridge. / 문서 스레드 배치와 EDT 그리기를 잇는 뷰. */
    private final transient PageView view;

    /** Download manager and popups of this component. / 이 컴포넌트의 다운로드 관리자와 팝업. */
    private final transient SwingDownloadHandler downloads;

    /** Internal keyboard and focus input. / 내부 키보드와 포커스 입력. */
    private final transient PageKeyboard keyboard;

    /** The search in the page. / 페이지 안 찾기. */
    private final transient org.duckdns.hjow.swingwebpanel.core.PageFind finder;

    /** Internal pointer input, separate from user hyperlink listeners. / 사용자 하이퍼링크 리스너와 독립된 내부 포인터 입력. */
    private final transient PageInput input;

    /** Internal text input of input methods (Hangul and other composed text) into editing hosts. / 입력기(한글 등 조합하는 글)가 편집 호스트에 하는 내부 텍스트 입력. */
    private final transient org.duckdns.hjow.swingwebpanel.core.PageComposition composition;

    /** The Swing editors, bubbles and file chooser of the form controls of the page. 페이지의 폼 컨트롤을 위한 Swing 편집기, 말풍선, 파일 선택기. */
    private final transient ControlOverlays controls;

    /**
     * Cookies of this component (shared with the network page loader): a store of its own, or the store of the {@link CookieJar} that
     * the host passed.
     * <p>
     * 이 컴포넌트의 쿠키(네트워크 페이지 로더와 공유한다): 자기 저장소, 또는 호스트가 넘긴 {@link CookieJar} 의 저장소.
     */
    private final transient CookieStore cookies;

    /** The component whose page opened the window of this component, or null (plan item P31-08). / 페이지가 이 컴포넌트의 창을 연 컴포넌트. 없으면 null(계획 P31-08). */
    private final transient SwingWebPanel creator;

    /** Closes the window that shows this component after a script closed it; set by the default popup window, or null. / 스크립트가 닫은 뒤 이 컴포넌트를 보이는 창을 닫는다. 기본 팝업 창이 정한다. 없으면 null. */
    private transient volatile Runnable windowCloser;

    /**
     * What a component needs to become the window that a page of another component opens (extension, API contract 5.28): it is passed
     * to {@link SwingWebPanel#createPopupPanel(PopupSeed)} and from there to the constructor
     * {@link SwingWebPanel#SwingWebPanel(PopupSeed)}. A seed can be used for one component only.
     * <p>
     * 컴포넌트가 다른 컴포넌트의 페이지가 연 창이 되는 데 필요한 것(확장, API 계약 5.28): {@link SwingWebPanel#createPopupPanel(PopupSeed)} 에
     * 넘겨지고 거기서 생성자 {@link SwingWebPanel#SwingWebPanel(PopupSeed)} 로 넘긴다. 씨앗 하나는 컴포넌트 하나에만 쓸 수 있다.
     */
    public static final class PopupSeed {

        /** The component whose page opens the window. / 페이지가 창을 여는 컴포넌트. */
        private final SwingWebPanel creator;

        /** The document loop that the new window shares with its opener, or null. / 새 창이 여는 창과 함께 쓰는 문서 루프. 없으면 null. */
        private final DocumentLoop loop;

        /** True once a component was created from the seed. / 씨앗으로 컴포넌트를 만든 뒤 true. */
        private boolean used;

        /**
         * Creates a seed; only the component does this.
         * <p>
         * 씨앗을 만든다. 컴포넌트만 만든다.
         *
         * @param creator the component whose page opens the window / 페이지가 창을 여는 컴포넌트
         * @param loop    the document loop to share, or null / 함께 쓸 문서 루프 또는 null
         */
        private PopupSeed(SwingWebPanel creator, DocumentLoop loop) {
            this.creator = creator;
            this.loop = loop;
        }

        /**
         * Marks the seed as used.
         * <p>
         * 씨앗을 쓴 것으로 표시한다.
         *
         * @return this seed / 이 씨앗
         * @throws IllegalStateException if a component was already created from it / 이미 컴포넌트를 만든 씨앗이면
         */
        private synchronized PopupSeed take() {
            if (used) {
                throw new IllegalStateException("a popup seed can be used for one component only");
            }
            used = true;
            return this;
        }
    }

    /** The jar that the host passed, or null when the component has a store of its own. / 호스트가 넘긴 저장소. 컴포넌트가 자기 저장소를 가지면 null. */
    private final transient CookieJar cookieJar;

    /** Fetcher and cache shared by main documents and subresources. / 주 문서와 하위 리소스가 공유하는 가져오기 객체와 캐시. */
    private final transient ResourceFetcher fetcher;

    /**
     * Listeners this component registered on itself; they are removed at dispose.
     * <p>
     * 이 컴포넌트가 자신에게 등록한 내부 리스너. dispose 때 제거한다.
     */
    private final transient Set<Object> internalListeners = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    /** The page zoom factor (1 for 100 percent), see {@link #setZoom(double)}. / 페이지 확대 배율(1 이면 100 %). {@link #setZoom(double)} 참조. */
    private volatile double zoom = 1.0;
    /** The component x that the zoom keeps fixed (set by Ctrl+wheel), or -1 for the top left of the viewport; EDT only. / 확대가 고정해 두는 컴포넌트 x(Ctrl+휠이 정함). 뷰포트 왼쪽 위이면 -1. EDT 에서만 쓴다. */
    private transient int zoomAnchorX = -1;
    /** The component y that the zoom keeps fixed, or -1; EDT only. / 확대가 고정해 두는 컴포넌트 y. 없으면 -1. EDT 에서만 쓴다. */
    private transient int zoomAnchorY = -1;
    /** What the last zoom keeps in place until the layout of the new size is known: {document x, document y, x in viewport, y in viewport}, or null; EDT only. / 마지막 확대가 새 크기의 레이아웃을 알 때까지 제자리에 두는 것: {문서 x, 문서 y, 뷰포트 안 x, 뷰포트 안 y}. 없으면 null. EDT 에서만 쓴다. */
    private transient double[] zoomKeep;
    /** Viewport currently observed on the EDT. / EDT에서 현재 관찰하는 뷰포트. */
    private transient JViewport observedViewport;
    /** The viewport width that the pages were told about last (-1 before the first report); EDT only. / 페이지에 마지막으로 알린 뷰포트 폭(첫 보고 전에는 -1). EDT 에서만 쓴다. */
    private transient int reportedWidth = -1;
    /** The viewport height that the pages were told about last (-1 before the first report); EDT only. / 페이지에 마지막으로 알린 뷰포트 높이(첫 보고 전에는 -1). EDT 에서만 쓴다. */
    private transient int reportedHeight = -1;
    /** The horizontal scroll position that the pages were told about last; EDT only. / 페이지에 마지막으로 알린 가로 스크롤 위치. EDT 에서만 쓴다. */
    private transient int reportedX = -1;
    /** The vertical scroll position that the pages were told about last; EDT only. / 페이지에 마지막으로 알린 세로 스크롤 위치. EDT 에서만 쓴다. */
    private transient int reportedY = -1;
    /** Copies scroll position/extent changes without reading Swing from a script task. / 스크립트 작업에서 Swing을 읽지 않고 스크롤 위치·크기 변경을 복사한다. */
    private final transient ChangeListener viewportListener = new ChangeListener() {
        /** Refreshes the immutable viewport data on the EDT. / EDT에서 불변 뷰포트 데이터를 갱신한다.
         * @param event viewport change / 뷰포트 변경 */
        public void stateChanged(ChangeEvent event) {
            updateViewport();
        }
    };

    /**
     * Set once {@link #dispose()} ran.
     * <p>
     * {@link #dispose()} 가 실행되면 true.
     */
    private volatile boolean disposed;

    /**
     * Last viewport width for which {@link #viewportWidthChanged(int)} was called.
     * <p>
     * {@link #viewportWidthChanged(int)} 를 마지막으로 호출한 폭.
     */
    private int lastWidth = -1;

    /**
     * Creates a component that shows the blank page.
     * <p>
     * 빈 페이지를 보여 주는 컴포넌트를 만든다.
     */
    public SwingWebPanel() {
        this((PageLoader) null, null);
    }

    /**
     * Creates a component that shows the blank page and keeps its cookies in a jar that it shares with the other components created
     * with the same jar (extension, API contract 5.24): a cookie that a page of one of them sets is sent by the requests of all of
     * them. Disposing this component does not clear the jar. Everything else (history, cache, session storage) stays separate.
     * <p>
     * 빈 페이지를 보여 주며, 같은 저장소로 만든 다른 컴포넌트와 공유하는 저장소에 쿠키를 두는 컴포넌트를 만든다(확장, API 계약 5.24): 그중 한
     * 컴포넌트의 페이지가 설정한 쿠키를 모든 컴포넌트의 요청이 보낸다. 이 컴포넌트를 폐기해도 저장소는 비워지지 않는다. 그 밖의 것(이력, 캐시,
     * sessionStorage)은 따로이다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * CookieJar jar = new CookieJar();
     * SwingWebPanel tab1 = new SwingWebPanel(jar);
     * SwingWebPanel tab2 = new SwingWebPanel(jar);
     * </pre>
     *
     * @param jar the shared cookie jar / 공유할 쿠키 저장소
     * @throws IllegalArgumentException if the jar is null / 저장소가 null 이면
     */
    public SwingWebPanel(CookieJar jar) {
        this((PageLoader) null, requireJar(jar));
    }

    /**
     * Rejects a missing jar before the component is built.
     * <p>
     * 컴포넌트를 만들기 전에 없는 저장소를 거부한다.
     *
     * @param jar the jar / 저장소
     * @return the jar / 저장소
     */
    private static CookieJar requireJar(CookieJar jar) {
        if (jar == null) {
            throw new IllegalArgumentException("jar must not be null");
        }
        return jar;
    }

    /**
     * Creates a component that loads pages with the given loader. Meant for subclasses, hosts and tests that replace the page
     * loading pipeline.
     * <p>
     * 주어진 로더로 페이지를 읽는 컴포넌트를 만든다. 페이지 로딩 파이프라인을 바꾸려는 하위 클래스, 호스트, 시험용이다.
     *
     * @param loader page loader, or null for the standard loader that uses the network layer / 페이지 로더. 네트워크 계층을 쓰는 표준 로더이면 null
     */
    protected SwingWebPanel(PageLoader loader) {
        this(loader, null);
    }

    /**
     * Creates the component of a window that a page of another component opens (extension, API contract 5.28). A subclass that wants
     * its own class for such windows overrides {@link #createPopupPanel(PopupSeed)} to call a constructor that passes the seed here.
     * The component shares the cookies of the component that opened it and starts with its default fonts. Unlike the other
     * constructors this one may run on a thread other than the event dispatch thread (the thread of the script that calls
     * <code>window.open</code>), so a subclass constructor must not show anything or wait for the event dispatch thread.
     * <p>
     * 다른 컴포넌트의 페이지가 연 창의 컴포넌트를 만든다(확장, API 계약 5.28). 그런 창에 자기 클래스를 쓰려는 하위 클래스는
     * {@link #createPopupPanel(PopupSeed)} 를 재정의하여, 씨앗을 여기로 넘기는 생성자를 부른다. 컴포넌트는 자신을 연 컴포넌트의 쿠키를 함께 쓰고
     * 그 기본 글꼴로 시작한다. 다른 생성자와 달리 이 생성자는 이벤트 디스패치 스레드가 아닌 스레드(<code>window.open</code> 을 부른 스크립트의
     * 스레드)에서 실행될 수 있으므로, 하위 클래스의 생성자는 아무것도 보이거나 이벤트 디스패치 스레드를 기다리면 안 된다.
     *
     * @param seed the seed that {@link #createPopupPanel(PopupSeed)} received / {@link #createPopupPanel(PopupSeed)} 가 받은 씨앗
     * @throws IllegalArgumentException if the seed is null / 씨앗이 null 이면
     * @throws IllegalStateException    if the seed was used already / 이미 쓴 씨앗이면
     */
    protected SwingWebPanel(PopupSeed seed) {
        this(null, null, requireSeed(seed).take());
    }

    /**
     * Rejects a missing seed before the component is built.
     * <p>
     * 컴포넌트를 만들기 전에 없는 씨앗을 거부한다.
     *
     * @param seed the seed / 씨앗
     * @return the seed / 씨앗
     */
    private static PopupSeed requireSeed(PopupSeed seed) {
        if (seed == null) {
            throw new IllegalArgumentException("seed must not be null");
        }
        return seed;
    }

    /**
     * Creates the component with a page loader and a cookie jar.
     * <p>
     * 페이지 로더와 쿠키 저장소로 컴포넌트를 만든다.
     *
     * @param loader page loader, or null for the standard loader / 페이지 로더. 표준 로더이면 null
     * @param jar    the shared cookie jar, or null for a store of its own / 공유 쿠키 저장소. 자기 저장소를 쓰면 null
     */
    private SwingWebPanel(PageLoader loader, CookieJar jar) {
        this(loader, jar, null);
    }

    /**
     * Creates the component with a page loader, a cookie jar and, for a window that a page opened, the seed of that window.
     * <p>
     * 페이지 로더, 쿠키 저장소, 그리고 페이지가 연 창이면 그 창의 씨앗으로 컴포넌트를 만든다.
     *
     * @param loader page loader, or null for the standard loader / 페이지 로더. 표준 로더이면 null
     * @param jar    the shared cookie jar, or null for a store of its own / 공유 쿠키 저장소. 자기 저장소를 쓰면 null
     * @param seed   the seed of a window that a page opened, or null / 페이지가 연 창의 씨앗. 없으면 null
     */
    private SwingWebPanel(PageLoader loader, CookieJar jar, PopupSeed seed) {
        super(null);
        creator = seed == null ? null : seed.creator;
        // 페이지가 연 창은 자신을 연 컴포넌트의 쿠키를 함께 쓴다(같은 브라우저의 창이다).
        cookieJar = creator != null ? creator.cookieJar : jar;
        cookies = creator != null ? creator.cookies : jar == null ? new CookieStore() : jar.store();
        setOpaque(true);
        setBackground(java.awt.Color.WHITE);
        setFocusable(true);
        fetcher = createFetcher();
        PageLoader effective = loader == null ? createNetworkLoader() : loader;
        session = new BrowserSession(effective, new Observer(), cookies, org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager.getInstance(),
                seed == null ? null : seed.loop);
        // 공유 저장소는 이 컴포넌트를 폐기해도 비우지 않는다. 여는 컴포넌트와 함께 쓰는 저장소도 그렇다.
        session.setCookiesShared(jar != null || creator != null);
        session.setWindowOpener(new WindowOpener() {
            /**
             * Creates the component of the window of a <code>window.open</code> call and asks the host, later on the event dispatch
             * thread, to show it.
             * <p>
             * <code>window.open</code> 호출의 창의 컴포넌트를 만들고, 나중에 이벤트 디스패치 스레드에서 호스트에 그것을 보이도록 요청한다.
             *
             * @param request what the script asked for / 스크립트가 요청한 것
             * @param shared  the document loop to share, or null / 함께 쓸 문서 루프 또는 null
             * @return the session of the new window, or null / 새 창의 세션. 없으면 null
             */
            public BrowserSession openFromScript(OpenRequest request, DocumentLoop shared) {
                final SwingWebPanel child = newPopupPanel(shared, request);
                if (child == null) {
                    return null;
                }
                final WindowRequest asked = new WindowRequest(SwingWebPanel.this, request, child);
                SwingUtilities.invokeLater(new Runnable() {
                    /** Lets the host show the window; a host that fails gets the window closed. / 호스트가 창을 보이게 한다. 실패한 호스트의 창은 닫는다. */
                    public void run() {
                        if (child.isDisposed()) {
                            return;
                        }
                        try {
                            openPopupWindow(asked);
                        } catch (RuntimeException e) {
                            asked.reject();
                        }
                    }
                });
                return child.session;
            }

            /**
             * Asks the overridable method whether windows can be shown.
             * <p>
             * 재정의할 수 있는 메서드에 창을 보일 수 있는지 묻는다.
             *
             * @return true if a window can be shown / 창을 보일 수 있으면 true
             */
            public boolean canOpenWindows() {
                return !disposed && canOpenPopupWindows();
            }

            /**
             * Offers the request to the host on the event dispatch thread and runs the fallback if the host does not take it.
             * <p>
             * 이벤트 디스패치 스레드에서 요청을 호스트에 제안하고, 호스트가 받지 않으면 대체 동작을 실행한다.
             *
             * @param request  what was asked for / 요청한 것
             * @param fallback what to do otherwise, or null / 받지 않으면 할 일 또는 null
             */
            public void openFromLink(final OpenRequest request, final Runnable fallback) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** Asks the host. / 호스트에 묻는다. */
                    public void run() {
                        if (disposed) {
                            return;
                        }
                        WindowRequest asked = new WindowRequest(SwingWebPanel.this, request, null);
                        boolean taken;
                        try {
                            taken = openNewWindow(asked);
                        } catch (RuntimeException e) {
                            taken = false;
                        }
                        // 호스트가 받지 않았으면 대체 동작을 한다. 팝업 차단이 막은 요청은 대체 동작도 하지 않는다.
                        if (!taken && !asked.isBlocked()) {
                            asked.reject();
                            if (fallback != null) {
                                fallback.run();
                            }
                        }
                    }
                });
            }

            /**
             * Tells the host on the event dispatch thread that a window was blocked.
             * <p>
             * 창을 차단했음을 이벤트 디스패치 스레드에서 호스트에 알린다.
             *
             * @param request what was asked for / 요청한 것
             */
            public void popupBlocked(final OpenRequest request) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** Tells the host. / 호스트에 알린다. */
                    public void run() {
                        if (!disposed) {
                            SwingWebPanel.this.popupBlocked(new WindowRequest(SwingWebPanel.this, request, null));
                        }
                    }
                });
            }
        });
        session.setConsoleDispatcher(consoles);
        session.setWebMessageDispatcher(webMessages);
        session.setPrintHandler(new Runnable() {
            /** Offers a print request of a script to the host (event dispatch thread). / 스크립트의 인쇄 요청을 호스트에 제안한다(이벤트 디스패치 스레드). */
            public void run() {
                if (!disposed) {
                    printRequested();
                }
            }
        });
        session.setWindowCloseHandler(new org.duckdns.hjow.swingwebpanel.core.WindowCloseHandler() {
            /**
             * Closes the window of this component for a script, on the event dispatch thread.
             * <p>
             * 스크립트를 위해 이 컴포넌트의 창을 이벤트 디스패치 스레드에서 닫는다.
             *
             * @param createdByWebContent true if web content created the window / 웹 콘텐츠가 창을 만들었으면 true
             */
            public void closeWindow(boolean createdByWebContent) {
                // 호스트가 만든 창만 본 창 닫기 정책(기본은 프로그램 종료)을 쓴다. 웹 콘텐츠가 만든 창은 그 창을 담은 쪽이 닫는다.
                if (disposed) {
                    return;
                }
                if (createdByWebContent) {
                    closeOpenedWindow();
                } else {
                    closeRootWindow();
                }
            }
        });
        controls = new ControlOverlays(this);
        view = new PageView(session, metrics, new PageView.HostWithScriptLimits() {
            /** Publishes new scroll dimensions and drawing on the EDT. / EDT에서 새 스크롤 크기와 그림을 게시한다. */
            public void viewChanged() {
                revalidate();
                repaint();
                // 새 렌더링에서 컨트롤이 놓인 자리에 편집기를 다시 놓는다.
                controls.layout(SwingWebPanel.this.view.getRenderedPage());
                // 확대한 직후라면 새 크기가 알려졌으므로 기준점을 다시 제자리에 둔다.
                keepZoomPoint(true);
            }

            /** Reads the top-level document's host policy. / 최상위 문서의 호스트 정책을 읽는다. */
            public long scriptTaskTimeLimitMillis(String url) {
                return SwingWebPanel.this.getScriptTaskTimeLimitMillis(url);
            }
        }, fetcher, cookies);
        view.setAudioOutputFactory(new AudioOutputFactory() {
            /** The factory that the overridable method chose, asked at the first use. / 재정의할 수 있는 메서드가 고른 팩터리. 처음 쓸 때 묻는다. */
            private AudioOutputFactory chosen;

            /** Opens an output with the factory of this component. / 이 컴포넌트의 팩터리로 출력을 연다.
             * @param sampleRate frames per second / 초당 프레임 수
             * @param channels 1 or 2 / 1 또는 2
             * @return the output / 출력
             * @throws MediaException ACCESS_FAILED without a factory or a device / 팩터리나 장치가 없으면 ACCESS_FAILED */
            public AudioOutput open(int sampleRate, int channels) throws MediaException {
                AudioOutputFactory factory;
                synchronized (this) {
                    if (chosen == null) {
                        chosen = SwingWebPanel.this.createAudioOutputFactory();
                    }
                    factory = chosen;
                }
                if (factory == null) {
                    throw new MediaException(MediaException.Kind.ACCESS_FAILED, "this component has no audio output");
                }
                return factory.open(sampleRate, channels);
            }
        });
        view.setWebAudioOutputFactory(new OutputFactory() {
            /** The factory that the overridable method chose, asked at the first use. / 재정의할 수 있는 메서드가 고른 팩터리. 처음 쓸 때 묻는다. */
            private OutputFactory chosen;

            /** True once the overridable method was asked. / 재정의할 수 있는 메서드에 물었으면 true. */
            private boolean asked;

            /** Opens an output with the factory of this component. / 이 컴포넌트의 팩터리로 출력을 연다.
             * @param sampleRate frames per second / 초당 프레임 수
             * @param channels the number of channels / 채널 수
             * @param bufferFrames the buffer in frames / 버퍼(프레임)
             * @return the output / 출력
             * @throws IOException if there is no factory or the output cannot be opened / 팩터리가 없거나 출력을 열 수 없으면 */
            public org.duckdns.hjow.swingwebpanel.media.webaudio.output.AudioOutput open(int sampleRate, int channels, int bufferFrames) throws IOException {
                OutputFactory factory;
                synchronized (this) {
                    if (!asked) {
                        chosen = SwingWebPanel.this.createWebAudioOutputFactory();
                        asked = true;
                    }
                    factory = chosen;
                }
                // 팩터리가 없으면(null) 소리 출력이 없는 것이므로 열 수 없다고 알린다. 렌더링 드라이버가 소리 없이 시간만 흐르는 출력으로 대신한다.
                if (factory == null) {
                    throw new IOException("this component has no Web Audio output");
                }
                return factory.open(sampleRate, channels, bufferFrames);
            }
        });
        if (effective instanceof ParsingPageLoader) {
            ((ParsingPageLoader) effective).bindScripts(session, fetcher, cookies, view);
        }
        // 페이지가 연 창은 자신을 연 컴포넌트의 기본 글꼴로 시작한다.
        if (creator != null) {
            view.setFontDefaults(creator.view.getFontDefaults());
        }
        downloads = new SwingDownloadHandler(this, fetcher, cookies, new DownloadStatusSink() {
            /** Shows a download message in the status text. / 다운로드 메시지를 상태 메시지에 보인다.
             * @param message the message / 메시지 */
            public void downloadStatus(String message) {
                session.setStatusMessage(message);
            }
        }) {
            /** Lets this component's overridable method create the popup. / 이 컴포넌트의 재정의 가능한 메서드가 팝업을 만들게 한다.
             * @param request what is downloaded / 내려받는 대상
             * @param control how the popup cancels or changes the target / 팝업이 취소하거나 대상을 바꾸는 방법
             * @return the popup / 팝업 */
            protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
                return SwingWebPanel.this.createDownloadPopup(request, control);
            }

            /** Lets this component's overridable method choose the directory. / 이 컴포넌트의 재정의 가능한 메서드가 디렉터리를 고르게 한다.
             * @return the directory / 디렉터리 */
            protected java.io.File getDefaultDirectory() {
                java.io.File chosen = SwingWebPanel.this.getDownloadDirectory();
                return chosen != null ? chosen : super.getDefaultDirectory();
            }
        };
        session.setDownloadHandler(downloads);
        session.setResubmitConfirmer(new org.duckdns.hjow.swingwebpanel.core.ResubmitConfirmer() {
            /** Lets this component's overridable method ask the user. / 이 컴포넌트의 재정의 가능한 메서드가 사용자에게 묻게 한다.
             * @param url the address that the form was sent to / 폼을 보낸 주소
             * @return true to send the form again / 폼을 다시 보내려면 true */
            public boolean confirmResubmit(String url) {
                return SwingWebPanel.this.confirmResubmit(url);
            }
        });
        input = new PageInput(this, session, view, links);
        internalListeners.add(input);
        addMouseListener(input);
        addMouseMotionListener(input);
        addMouseWheelListener(input);
        keyboard = new PageKeyboard(this, session, view, input);
        finder = new org.duckdns.hjow.swingwebpanel.core.PageFind(this, session, view, new org.duckdns.hjow.swingwebpanel.core.PageFind.Listener() {
            /** Announces the new result as a property change (event dispatch thread). / 새 결과를 속성 변경으로 알린다(이벤트 디스패치 스레드).
             * @param result the new result / 새 결과 */
            public void findResultChanged(FindResult result) {
                FindResult old = announcedFind;
                announcedFind = result;
                firePropertyChange(PROPERTY_FIND_RESULT, old, result);
            }
        });
        view.setFind(finder);
        internalListeners.add(keyboard);
        addKeyListener(keyboard);
        addFocusListener(keyboard);
        // 입력기(한글 등)의 텍스트는 편집 호스트에 조합되는 대로 들어간다. 편집 호스트가 포커스를 가지기 전에는 입력기를 꺼 둔다.
        composition = new org.duckdns.hjow.swingwebpanel.core.PageComposition(this, session, view, keyboard, input.getEditing(), input.getSelection());
        internalListeners.add(composition);
        addInputMethodListener(composition);
        input.setPressHook(new Runnable() {
            /** Ends a running composition before a press moves the caret. / 누름이 캐럿을 옮기기 전에 진행 중인 조합을 끝낸다. */
            public void run() {
                composition.finish();
            }
        });
        input.getSelection().setInputMethodSwitch(new org.duckdns.hjow.swingwebpanel.core.PageSelection.InputMethodSwitch() {
            /** Turns the input methods of the component on or off. / 컴포넌트의 입력기를 켜거나 끈다.
             * @param on true to turn them on / 켜려면 true */
            public void setEnabled(boolean on) {
                SwingWebPanel.this.enableInputMethods(on);
            }
        });
        enableInputMethods(false);
        // 텍스트 선택으로 끌 때 보이는 영역 밖이면 뷰포트가 따라 스크롤되고, 복사한 텍스트는 이 컴포넌트의 클립보드 메서드로 간다.
        setAutoscrolls(true);
        ZoomControl zoomControl = new ZoomControl() {
            /** Returns the zoom factor. / 확대 배율을 반환한다.
             * @return the factor / 배율 */
            public double getZoom() {
                return zoom;
            }

            /** Sets the zoom factor. / 확대 배율을 정한다.
             * @param factor the factor / 배율 */
            public void setZoom(double factor) {
                SwingWebPanel.this.setZoom(factor);
            }

            /** Remembers the place that the zoom keeps fixed. / 확대가 고정해 두는 곳을 기억한다.
             * @param x component x / 컴포넌트 x
             * @param y component y / 컴포넌트 y */
            public void setZoomAnchor(int x, int y) {
                zoomAnchorX = x;
                zoomAnchorY = y;
            }
        };
        input.setZoomControl(zoomControl);
        keyboard.setZoomControl(zoomControl);
        // 페이지 본문의 복사, 편집 호스트의 붙여넣기, 폼 컨트롤의 편집기가 같은 클립보드 메서드(재정의할 수 있다)를 쓴다.
        ClipboardSink clipboardSink = new ClipboardSink() {
            /** Passes the copied text to the overridable method. / 복사한 텍스트를 재정의할 수 있는 메서드에 넘긴다.
             * @param plainText the text / 텍스트
             * @param htmlFragment the markup or null / 마크업. 없으면 null
             * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면 */
            public void write(String plainText, String htmlFragment) throws Exception {
                SwingWebPanel.this.copyToClipboard(plainText, htmlFragment);
            }
        };
        org.duckdns.hjow.swingwebpanel.core.ClipboardSource clipboardSource = new org.duckdns.hjow.swingwebpanel.core.ClipboardSource() {
            /** Passes the read to the overridable method. / 읽기를 재정의할 수 있는 메서드에 넘긴다.
             * @return the clipboard contents or null / 클립보드 내용. 없으면 null
             * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면 */
            public java.awt.datatransfer.Transferable read() throws Exception {
                return SwingWebPanel.this.readClipboard();
            }
        };
        input.getSelection().setClipboard(clipboardSink);
        input.getEditing().setClipboardSource(clipboardSource);
        controls.setClipboard(clipboardSink, clipboardSource);
        session.setClipboard(clipboardSink, clipboardSource);
        controls.setKeyboard(keyboard);
        view.setControlOverlays(controls);
        view.setFormSubmitSink(new FormSubmitSink() {
            /** Passes the submission to the overridable method. / 제출을 재정의할 수 있는 메서드에 넘긴다.
             * @param submission what to send / 보낼 내용 */
            public void submit(FormSubmission submission) {
                SwingWebPanel.this.submitForm(submission);
            }
        });
        controls.setUploadChooser(new UploadChooser() {
            /** Passes the question to the overridable method. / 질문을 재정의할 수 있는 메서드에 넘긴다.
             * @param multiple several files? / 여러 파일 여부
             * @param accept accept attribute / accept 속성
             * @return the files or null / 파일 또는 null */
            public File[] choose(boolean multiple, String accept) {
                return SwingWebPanel.this.chooseUploadFiles(multiple, accept);
            }
        });
        MouseListener pressRecorder = controls.getMouseListener();
        internalListeners.add(pressRecorder);
        addMouseListener(pressRecorder);
        ComponentListener resizeListener = new ComponentAdapter() {
            /**
             * Tells the layout side that the viewport width changed.
             * <p>
             * 뷰포트 폭이 바뀌었음을 레이아웃 쪽에 알린다.
             *
             * @param e resize event / 크기 변경 이벤트
             */
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                if (w != lastWidth) {
                    lastWidth = w;
                    viewportWidthChanged(w);
                } else {
                    updateViewport();
                }
            }
        };
        internalListeners.add(resizeListener);
        addComponentListener(resizeListener);
        HierarchyBoundsListener boundsListener = new HierarchyBoundsAdapter() {
            /** Updates the extent even when overflowing content keeps its own width. / 넘치는 내용이 자기 폭을 유지해도 뷰포트 범위를 갱신한다.
             * @param event ancestor resize event / 조상 크기 변경 이벤트 */
            public void ancestorResized(HierarchyEvent event) {
                updateViewport();
            }
        };
        internalListeners.add(boundsListener);
        addHierarchyBoundsListener(boundsListener);
        HierarchyListener parentListener = new HierarchyListener() {
            /** Updates the extent after attachment to a viewport. / 뷰포트에 부착된 뒤 범위를 갱신한다.
             * @param event hierarchy event / 계층 이벤트 */
            public void hierarchyChanged(HierarchyEvent event) {
                updateViewport();
                // 화면에 나타나거나 사라지면 문서의 가시성(document.visibilityState)이 바뀐다.
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    session.setDocumentVisible(isShowing());
                }
            }
        };
        internalListeners.add(parentListener);
        addHierarchyListener(parentListener);
    }

    /**
     * Receives the submission of a form of the page (the user pressed a submit button or Enter, or a script submitted the form, and the form
     * passed validation). The default hands it to the browser session when it can navigate with it; a subclass or a test overrides it to
     * observe or to replace the navigation. It is called on the document thread of the page and must not block it.
     * <p>
     * 페이지의 폼 제출을 받는다(사용자가 제출 단추나 Enter 를 눌렀거나 스크립트가 폼을 제출했고 폼이 검증을 통과했다). 기본은 브라우저 세션이 그것으로 이동할 수
     * 있으면 세션에 넘긴다. 하위 클래스나 시험은 이것을 재정의하여 이동을 관찰하거나 대신한다. 페이지의 문서 스레드에서 호출되며 이 스레드를 막으면 안 된다.
     *
     * @param submission the method, URL, body and target of the submission / 제출의 메서드, URL, 본문, 대상
     */
    protected void submitForm(FormSubmission submission) {
        Object target = session;
        if (target instanceof FormSubmitSink) {
            ((FormSubmitSink) target).submit(submission);
        } else {
            session.setStatusMessage("Form submission is not supported: " + submission.getUrl());
        }
    }

    /**
     * Asks the user for the files of a file upload control. The default shows a Swing file chooser (nothing without a screen); a host or a
     * test overrides it to choose files without a dialog. It is called on the event dispatch thread; return null if the user cancelled.
     * <p>
     * 파일 업로드 컨트롤의 파일을 사용자에게 묻는다. 기본은 Swing 파일 선택기를 보여 준다(화면이 없으면 아무것도 하지 않는다). 호스트나 시험은 이것을 재정의하여
     * 대화상자 없이 파일을 고른다. 이벤트 디스패치 스레드에서 호출되며, 사용자가 취소했으면 null 을 반환한다.
     *
     * @param multiple true if the control accepts several files / 컨트롤이 여러 파일을 받으면 true
     * @param accept   the <code>accept</code> attribute of the control / 컨트롤의 <code>accept</code> 속성
     * @return the chosen files or null / 고른 파일 또는 null
     */
    protected File[] chooseUploadFiles(boolean multiple, String accept) {
        return controls.showFileChooser(multiple, accept);
    }

    /**
     * Tells Swing that the children of this component (the editors of the form controls and the validation bubble) may overlap each other and
     * the page, so the page is repainted under them.
     * <p>
     * 이 컴포넌트의 자식(폼 컨트롤의 편집기와 검증 말풍선)이 서로 그리고 페이지와 겹칠 수 있음을 Swing 에 알려, 그 아래의 페이지가 다시 그려지게 한다.
     *
     * @return true if there are no children / 자식이 없으면 true
     */
    public boolean isOptimizedDrawingEnabled() {
        return getComponentCount() == 0;
    }

    /**
     * Creates the popup that shows one download
 (a navigation whose response is an attachment, a file that cannot be shown, or a link
     * with the <code>download</code> attribute). The default is a non-modal dialog whose parent is the window of this component, with the
     * file name, the source, the target (changeable through a file chooser), a progress bar (indeterminate when the size is unknown), a
     * cancel button and the result. A host or a test overrides it to show its own popup or to observe the download. It is called on the
     * EDT; the transfer itself runs on a background thread of the download manager.
     * <p>
     * 다운로드 하나를 보여 주는 팝업을 만든다(응답이 첨부 파일이거나 보여 줄 수 없는 파일인 이동, <code>download</code> 속성이 있는 링크). 기본은 이 컴포넌트의 창을 부모로
     * 하는 비모달 대화상자이며 파일 이름, 출처, 저장 위치(파일 선택기로 바꿀 수 있음), 진행 막대(크기를 모르면 불확정), 취소 단추, 결과를 보여 준다.
     * 호스트와 시험은 이것을 재정의하여 자신의 팝업을 보여 주거나 다운로드를 관찰한다. EDT 에서 호출되며 전송은 다운로드 관리자의 백그라운드
     * 스레드에서 실행된다.
     *
     * @param request what is downloaded / 내려받는 대상
     * @param control how the popup can cancel the download or change its target / 팝업이 다운로드를 취소하거나 대상을 바꾸는 방법
     * @return the popup / 팝업
     */
    protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {
        return new SwingDownloadDialog(SwingUtilities.getWindowAncestor(this), request, control);
    }

    /**
     * Asks the user whether the form that produced the shown page may be sent again. It is asked when the page is refreshed (or its
     * response has to be saved as a file), because a page that a form submission (<code>POST</code>) produced is never sent again
     * silently; showing it again from the history uses the stored source and does not ask. The default shows a confirmation dialog whose
     * parent is the window of this component; in a headless environment it answers no. A host or a test overrides it. It is called on the
     * EDT.
     * <p>
     * 지금 보이는 페이지를 만든 폼을 다시 보내도 되는지 사용자에게 묻는다. 폼 제출(<code>POST</code>)이 만든 페이지는 조용히 다시 보내지 않으므로 새로고침할
     * 때(또는 응답을 파일로 저장해야 할 때) 묻는다. 이력에서 다시 보여 줄 때는 저장한 원문을 쓰므로 묻지 않는다. 기본은 이 컴포넌트의 창을 부모로 하는 확인
     * 대화상자를 보이며, 헤드리스 환경에서는 아니오로 답한다. 호스트와 시험이 재정의한다. EDT 에서 호출된다.
     *
     * @param url the address that the form was sent to / 폼을 보낸 주소
     * @return true to send the form again / 폼을 다시 보내려면 true
     */
    protected boolean confirmResubmit(String url) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return false;
        }
        int answer = javax.swing.JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                "The page was made by sending a form to\n" + url + "\nSend the form data again?", "Confirm form resubmission",
                javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        return answer == javax.swing.JOptionPane.YES_OPTION;
    }

    /**
     * Creates the factory that opens the sound outputs of the <code>audio</code> elements of the pages (plan item P18-08). The default is the
     * default mixer of Java Sound ({@link JavaSoundOutputFactory}); a machine without an audio device makes <code>play()</code> fail with an
     * <code>error</code> event. A host overrides it to send the sound elsewhere, a test to record the PCM that would be played. It is asked
     * once, when the first element starts to play, and may return null for no sound.
     * <p>
     * 페이지의 <code>audio</code> 요소의 소리 출력을 여는 팩터리를 만든다(계획 P18-08). 기본은 Java Sound 의 기본 믹서({@link JavaSoundOutputFactory})이며,
     * 오디오 장치가 없는 컴퓨터에서는 <code>play()</code> 가 <code>error</code> 이벤트로 실패한다. 호스트는 소리를 다른 곳으로 보내려고, 시험은 재생되었을
     * PCM 을 기록하려고 재정의한다. 첫 요소가 재생을 시작할 때 한 번 묻으며, 소리가 없으면 null 을 반환해도 된다.
     *
     * @return the factory, or null for no sound output / 팩터리. 소리 출력이 없으면 null
     */
    protected AudioOutputFactory createAudioOutputFactory() {
        // 페이지가 연 창은 자신을 연 컴포넌트가 고른 출력을 쓴다.
        return creator != null ? creator.createAudioOutputFactory() : new JavaSoundOutputFactory();
    }

    /**
     * Selects the script task wall-clock limit for the final URL of a top-level document. A host can override this method for a known
     * application with longer initialization. Called on a loader thread before any scripts run. The default is 10 seconds and a nonpositive
     * override uses that same default. Frames have the default limit. The step limit and cancellation still apply.
     * <p>
     * 최상위 문서의 최종 URL에 스크립트 작업의 경과 시간 한도를 지정한다. 초기화가 긴 특정 애플리케이션을 위해 호스트에서 재정의할 수 있다.
     * 스크립트가 실행되기 전에 로더 스레드에서 호출한다. 기본값은 10초이고 재정의가 0 이하를 반환할 때도 기본값을 쓴다.
     * 프레임은 기본 한도를 사용한다. 단계 제한과 취소는 계속 적용된다.
     *
     * @param url final address of the top-level document / 최상위 문서의 최종 주소
     * @return the limit in milliseconds / 시간 한도(밀리초)
     */
    protected long getScriptTaskTimeLimitMillis(String url) {
        // 페이지가 연 창은 자신을 연 컴포넌트의 한도를 따른다.
        if (creator != null) {
            return creator.getScriptTaskTimeLimitMillis(url);
        }
        return org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment.DEFAULT_TASK_TIME_LIMIT_MILLIS;
    }

    /**
     * Creates the factory that opens the outputs of the real-time <code>AudioContext</code> objects of the pages (Web Audio, plan item P25-07). The default is the default mixer of
     * Java Sound ({@link SystemOutputFactory}); a context whose output cannot be opened (no sound device, or the device is busy) still runs, silently, and keeps the time of the context
     * going at real speed. A host overrides it to send the sound elsewhere (or to switch Web Audio off by returning null, which makes every context play silently), a test to record the audio
     * that would be played. It is independent of {@link #createAudioOutputFactory()}, which serves the <code>audio</code> elements: a host that wants to redirect all sound overrides both.
     * The factory is asked once, when the first real-time context starts to run; each segment of a context (the time between a <code>resume()</code> and the next <code>suspend()</code>)
     * opens its own output, on the rendering thread of the context.
     * <p>
     * 페이지의 실시간 <code>AudioContext</code> 의 출력을 여는 팩터리를 만든다(Web Audio, 계획 P25-07). 기본은 Java Sound 의 기본 믹서({@link SystemOutputFactory})이다. 출력을 열 수 없는
     * 컨텍스트(소리 장치가 없거나 장치가 사용 중)도 소리 없이 돌며 컨텍스트의 시간을 실제 속도로 흐르게 한다. 호스트는 소리를 다른 곳으로 보내려고(또는 null 을 반환해 Web Audio 를 꺼서 모든
     * 컨텍스트가 소리 없이 돌게 하려고), 시험은 재생되었을 오디오를 기록하려고 재정의한다. <code>audio</code> 요소를 위한 {@link #createAudioOutputFactory()} 와 독립이다: 모든 소리를
     * 돌리려는 호스트는 둘 다 재정의한다. 팩터리는 첫 실시간 컨텍스트가 돌기 시작할 때 한 번 묻는다. 컨텍스트의 구간(<code>resume()</code> 에서 다음 <code>suspend()</code> 까지의 시간)마다
     * 컨텍스트의 렌더링 스레드에서 자기 출력을 연다.
     *
     * @return the factory, or null for no Web Audio output / 팩터리. Web Audio 출력이 없으면 null
     */
    protected OutputFactory createWebAudioOutputFactory() {
        // 페이지가 연 창은 자신을 연 컴포넌트가 고른 출력을 쓴다.
        return creator != null ? creator.createWebAudioOutputFactory() : new SystemOutputFactory();
    }

    /**
     * Returns the directory in which downloads are saved first (the user can still choose another file in the download popup). The default
     * is null, which means the Downloads folder of the user if it exists and the home directory otherwise. A host or a test overrides it.
     * <p>
     * 다운로드를 처음 저장하는 디렉터리를 반환한다(사용자는 다운로드 팝업에서 다른 파일을 고를 수 있다). 기본은 null 이며, 사용자의 Downloads 폴더가
     * 있으면 그것, 없으면 홈 디렉터리를 뜻한다. 호스트와 시험이 재정의한다.
     *
     * @return the directory or null / 디렉터리. 없으면 null
     */
    protected java.io.File getDownloadDirectory() {
        // 페이지가 연 창은 자신을 연 컴포넌트의 다운로드 폴더를 쓴다.
        return creator != null ? creator.getDownloadDirectory() : null;
    }

    /**
     * Creates the standard page loader: network fetching with this component's cookies, and the SSL extension point
     * {@link #configureSsl(HttpsURLConnection)} applied to every HTTPS connection. Classpath addresses are read with the context class
     * loader of the creating thread (or the loader of this library if there is none).
     * <p>
     * 표준 페이지 로더를 만든다: 이 컴포넌트의 쿠키를 쓰는 네트워크 가져오기이며, 모든 HTTPS 연결에 SSL 확장 지점
     * {@link #configureSsl(HttpsURLConnection)} 이 적용된다. 클래스패스 주소는 생성한 스레드의 컨텍스트 클래스 로더(없으면 이 라이브러리의
     * 로더)로 읽는다.
     *
     * @return the loader / 로더
     */
    private PageLoader createNetworkLoader() {
        return new ParsingPageLoader(new NetworkPageLoader(fetcher, cookies));
    }

    /**
     * Creates this component's request layer for every resource purpose and the protected SSL hook.
     * <p>모든 리소스 목적과 protected SSL 훅을 위한 컴포넌트별 요청 계층을 만든다.
     * @return fetcher with a private HTTP cache / 독립 HTTP 캐시가 있는 가져오기 객체
     */
    private ResourceFetcher createFetcher() {
        SslConfigurer ssl = new SslConfigurer() {
            /**
             * Forwards to the overridable method of the component.
             * <p>
             * 컴포넌트의 재정의 가능한 메서드로 넘긴다.
             *
             * @param connection HTTPS connection about to connect / 곧 연결할 HTTPS 연결
             * @throws IOException to abort / 중단하려면 던진다
             */
            public void configureSsl(HttpsURLConnection connection) throws IOException {
                SwingWebPanel.this.configureSsl(connection);
            }
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = SwingWebPanel.class.getClassLoader();
        }
        ResourceFetcher resources = new ResourceFetcher(ssl, FetchLimits.DEFAULT, cl);
        // 컴포넌트마다 자신의 HTTP 캐시를 쓴다(다른 컴포넌트와 응답을 공유하지 않는다).
        resources.setCache(new HttpCache());
        return resources;
    }

    /**
     * Bridges session notifications (already on the EDT) to Swing property changes and repaints.
     * <p>
     * 세션 통지(이미 EDT 에서 호출됨)를 Swing 속성 변경과 다시 그리기로 이어 주는 다리.
     */
    private final class Observer implements SessionListener {
        /**
         * Revalidates and repaints after a page was confirmed.
         * <p>
         * 페이지가 확정된 뒤 다시 배치하고 그린다.
         *
         * @param oldUrl   previous URL / 이전 URL
         * @param snapshot confirmed page / 확정된 페이지
         */
        public void pageConfirmed(String oldUrl, PageSnapshot snapshot) {
            if (!disposed) {
                input.reset();
                updateViewport();
                view.pageConfirmed(snapshot);
            }
        }

        /**
         * Scrolls to the target of a fragment after a navigation inside the page, when the component sits in a viewport.
         * <p>
         * 페이지 안의 이동 뒤 컴포넌트가 뷰포트 안에 있으면 프래그먼트의 대상으로 스크롤한다.
         *
         * @param oldUrl   previous URL / 이전 URL
         * @param snapshot the page / 페이지
         * @param target   x, y, width, height of the target in document coordinates, or null / 대상의 x, y, 폭, 높이(문서 좌표). 없으면 null
         */
        public void fragmentChanged(String oldUrl, PageSnapshot snapshot, double[] target) {
            if (disposed || target == null) {
                return;
            }
            // 대상의 위쪽이 뷰포트 맨 위에 오게 하되 가로 위치는 그대로 둔다.
            scrollViewport(Double.NaN, target[1]);
        }

        /**
         * Scrolls the viewport for a script (<code>window.scrollTo</code> and the like), when the component sits in a viewport.
         * <p>
         * 컴포넌트가 뷰포트 안에 있으면 스크립트(<code>window.scrollTo</code> 등)를 위해 뷰포트를 스크롤한다.
         *
         * @param x the x in document pixels / 문서 픽셀의 x
         * @param y the y in document pixels / 문서 픽셀의 y
         */
        public void scrollRequested(double x, double y) {
            if (!disposed) {
                scrollViewport(x, y);
            }
        }

        /**
         * Announces the "page" property change.
         * <p>
         * "page" 속성 변경을 알린다.
         *
         * @param oldUrl previous URL / 이전 URL
         * @param newUrl new URL / 새 URL
         */
        public void pageChanged(String oldUrl, String newUrl) {
            if (!disposed) {
                firePropertyChange(PROPERTY_PAGE, oldUrl, newUrl);
            }
        }

        /**
         * Announces the "title" property change.
         * <p>
         * "title" 속성 변경을 알린다.
         *
         * @param oldTitle previous title / 이전 제목
         * @param newTitle new title / 새 제목
         */
        public void titleChanged(String oldTitle, String newTitle) {
            if (!disposed) {
                firePropertyChange(PROPERTY_TITLE, oldTitle, newTitle);
            }
        }

        /**
         * Announces the "statusText" property change.
         * <p>
         * "statusText" 속성 변경을 알린다.
         *
         * @param oldText previous text / 이전 메시지
         * @param newText new text / 새 메시지
         */
        public void statusTextChanged(String oldText, String newText) {
            if (!disposed) {
                firePropertyChange(PROPERTY_STATUS_TEXT, oldText, newText);
            }
        }

        /**
         * Announces the "progress" property change.
         * <p>
         * "progress" 속성 변경을 알린다.
         *
         * @param oldValue previous progress / 이전 진행률
         * @param newValue new progress / 새 진행률
         */
        public void progressChanged(int oldValue, int newValue) {
            if (!disposed) {
                firePropertyChange(PROPERTY_PROGRESS, Integer.valueOf(oldValue), Integer.valueOf(newValue));
            }
        }
    }

    /**
     * Throws if the component was disposed.
     * <p>
     * 컴포넌트가 폐기되었으면 예외를 던진다.
     *
     * @throws IllegalStateException if disposed / 폐기되었을 때
     */
    private void checkAlive() {
        if (disposed) {
            throw new IllegalStateException("component is disposed");
        }
    }

    /**
     * Sets the User-Agent header value used by all later requests. Without a call the value is "swingwebpanel/1.0".
     * <p>
     * 이후 모든 요청에 쓰는 User-Agent 헤더 값을 지정한다. 호출하지 않으면 "swingwebpanel/1.0" 이다.
     *
     * @param agent user agent text: not null, not blank, no CR/LF / User-Agent 문자열(null, 공백뿐, CR/LF 포함 불가)
     * @throws IllegalArgumentException for an invalid value / 잘못된 값일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setUserAgent(String agent) {
        session.setUserAgent(agent);
    }

    /**
     * Returns the User-Agent value.
     * <p>
     * User-Agent 값을 반환한다.
     *
     * @return user agent text / User-Agent 문자열
     */
    public String getUserAgent() {
        return session.getUserAgent();
    }

    /**
     * Starts loading an address and returns immediately. Supported: http, https, ftp (anonymous), <code>classpath://path</code>
     * and <code>about:blank</code>. Addresses with other schemes are shown as an error page without any connection. While the page
     * loads, the query methods still return the previous page.
     * <p>
     * 주소 읽기를 시작하고 즉시 반환한다. 지원: http, https, ftp(익명), <code>classpath://경로</code>, <code>about:blank</code>.
     * 다른 스킴의 주소는 연결 없이 오류 페이지로 보여 준다. 페이지를 읽는 동안 조회 메서드는 계속 이전 페이지를 반환한다.
     *
     * @param url absolute address / 절대 주소
     * @throws IllegalArgumentException for null, blank, relative or unparsable addresses and for about:text / null, 공백, 상대 주소,
     *                                  해석할 수 없는 주소, about:text 일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setPage(String url) {
        session.setPage(url);
    }

    /**
     * Posts a copied value to the current top-level script document with the specified target origin.
     * Call from any thread. Register the page listener first, for example by waiting for a ready message from the page.
     * Use the expected absolute target URL when sending data that belongs to a specific origin.
     * <p>
     * 복사한 값을 지정한 대상 오리진의 현재 최상위 스크립트 문서에 보낸다. 어느 스레드에서나 호출할 수 있다.
     * 페이지에서 준비 메시지를 받은 뒤 호출하면 리스너 등록을 확인할 수 있다. 특정 오리진의 데이터에는 대상 절대 주소를 지정한다.
     *
     * @param message plain Java data to copy / 복사할 Java 데이터
     * @param targetOrigin an absolute URL or <code>"*"</code> / 절대 주소 또는 <code>"*"</code>
     * @throws IllegalArgumentException when the origin or message is invalid / 주소나 값이 잘못되었으면
     * @throws IllegalStateException when the panel has been disposed / 컴포넌트를 폐기했으면
     */
    public void postMessage(Object message, String targetOrigin) {
        checkAlive();
        session.postMessage(message, targetOrigin);
    }

    /**
     * Posts to the current top-level document using the wildcard target origin.
     * <p>
     * 와일드카드 대상 오리진으로 현재 최상위 문서에 보낸다.
     *
     * @param message plain Java data to copy / 복사할 Java 데이터
     */
    public void postMessage(Object message) {
        postMessage(message, "*");
    }

    /**
     * Returns the URL of the confirmed page: the final URL after redirects, "about:blank" for the empty page and "about:text"
     * for content given with {@link #setContent(String, String)}.
     * <p>
     * 확정된 페이지의 URL 을 반환한다: 리디렉션 이후 최종 URL, 빈 페이지는 "about:blank",
     * {@link #setContent(String, String)} 로 넣은 콘텐츠는 "about:text".
     *
     * @return URL / URL
     */
    public String getPage() {
        return session.getPage();
    }

    /**
     * Turns JavaScript on or off (default: on). A change takes effect with the next content setting, navigation or refresh;
     * a page whose scripts were stopped is not re-run automatically.
     * <p>
     * JavaScript 를 켜거나 끈다(기본: 켬). 변경은 다음 콘텐츠 설정, 이동, 새로고침부터 효과가 있고, 이미 중단된 페이지의 스크립트를
     * 자동으로 다시 실행하지는 않는다.
     *
     * @param enabled true to enable / 켜려면 true
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setJavascriptEnabled(boolean enabled) {
        if (enabled) {
            session.setJavascriptEnabled(true);
        } else {
            webMessages.beginReset();
            try {
                session.setJavascriptEnabled(false);
            } finally {
                webMessages.endReset();
            }
        }
    }

    /**
     * Tells whether JavaScript is enabled (extension method).
     * <p>
     * JavaScript 가 켜져 있는지 알려 준다(확장 메서드).
     *
     * @return true if enabled / 켜져 있으면 true
     */
    public boolean isJavascriptEnabled() {
        return session.isJavascriptEnabled();
    }

    /**
     * Allows or forbids the page to close the window of this component with <code>window.close()</code> (extension method, API contract
     * 5.23; default: forbidden). When it is allowed and the top-level document, not a popup, calls <code>window.close()</code>, the
     * component calls {@link #closeRootWindow()} on the event dispatch thread right after the calling script task ended; by default that
     * ends the program with <code>System.exit(0)</code>. A frame of another origin and a script of another window cannot close the
     * window. Forbidding it again cancels a closing that has not been performed yet. The value survives {@link #clean()} and a change is
     * announced as the {@link #PROPERTY_ROOT_WINDOW_CLOSING_ENABLED} property on the event dispatch thread. It may be called from any
     * thread.
     * <p>
     * <b>Security:</b> with this setting on, the page that is shown can end the program. Turn it on only for pages whose content the host
     * controls.
     * <p>
     * 페이지가 <code>window.close()</code> 로 이 컴포넌트의 창을 닫는 것을 허용하거나 막는다(확장 메서드, API 계약 5.23. 기본: 막음). 허용했을 때
     * 팝업이 아닌 최상위 문서가 <code>window.close()</code> 를 부르면, 컴포넌트는 호출한 스크립트 작업이 끝난 직후 이벤트 디스패치 스레드에서
     * {@link #closeRootWindow()} 를 부른다. 기본 동작은 <code>System.exit(0)</code> 으로 프로그램을 끝내는 것이다. 다른 오리진의 프레임과 다른 창의
     * 스크립트는 이 창을 닫을 수 없다. 다시 막으면 아직 수행하지 않은 닫기를 취소한다. 값은 {@link #clean()} 뒤에도 유지되고, 변경은
     * {@link #PROPERTY_ROOT_WINDOW_CLOSING_ENABLED} 속성으로 이벤트 디스패치 스레드에서 알린다. 어느 스레드에서나 부를 수 있다.
     * <p>
     * <b>보안:</b> 이 설정을 켜면 보여 주는 페이지가 프로그램을 끝낼 수 있다. 호스트가 내용을 통제하는 페이지에서만 켠다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * panel.setRootWindowClosingEnabled(true);
     * panel.setPage("classpath://app/index.html"); // a button of the page may now call window.close()
     * </pre>
     *
     * @param enabled true to allow / 허용하려면 true
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setRootWindowClosingEnabled(final boolean enabled) {
        checkAlive();
        final boolean old = session.setRootWindowClosingEnabled(enabled);
        if (old == enabled) {
            return;
        }
        // 속성 변경은 EDT 에서 알린다.
        if (SwingUtilities.isEventDispatchThread()) {
            firePropertyChange(PROPERTY_ROOT_WINDOW_CLOSING_ENABLED, old, enabled);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Announces the new value on the event dispatch thread. / 이벤트 디스패치 스레드에서 새 값을 알린다. */
                public void run() {
                    firePropertyChange(PROPERTY_ROOT_WINDOW_CLOSING_ENABLED, old, enabled);
                }
            });
        }
    }

    /**
     * Tells whether the page may close the window of this component with <code>window.close()</code> (extension method, API contract
     * 5.23). After {@link #dispose()} it returns the last value.
     * <p>
     * 페이지가 <code>window.close()</code> 로 이 컴포넌트의 창을 닫을 수 있는지 알려 준다(확장 메서드, API 계약 5.23). {@link #dispose()} 뒤에는
     * 마지막 값을 반환한다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean isRootWindowClosingEnabled() {
        return session.isRootWindowClosingEnabled();
    }

    /**
     * Closes the window of this component because the page called <code>window.close()</code> while
     * {@link #setRootWindowClosingEnabled(boolean)} allowed it (API contract 5.23). It is called once per accepted call, on the event
     * dispatch thread, after the unloading events of the page ran. The default implementation ends the program with
     * <code>System.exit(0)</code>. A host that must not end (for example a browser that only closes the tab or the window that holds this
     * component) overrides it; when the overriding method returns, the component and its page stay as they are and
     * <code>window.closed</code> stays true until the host disposes the component or navigates it.
     * <p>
     * {@link #setRootWindowClosingEnabled(boolean)} 가 허용한 동안 페이지가 <code>window.close()</code> 를 불렀으므로 이 컴포넌트의 창을 닫는다(API
     * 계약 5.23). 받아들인 호출마다 한 번, 페이지의 언로드 이벤트가 실행된 뒤 이벤트 디스패치 스레드에서 호출된다. 기본 구현은
     * <code>System.exit(0)</code> 으로 프로그램을 끝낸다. 끝나면 안 되는 호스트(예: 이 컴포넌트를 담은 탭이나 창만 닫는 브라우저)는 재정의한다.
     * 재정의한 메서드가 반환하면 컴포넌트와 페이지는 그대로 남고, 호스트가 컴포넌트를 폐기하거나 이동시킬 때까지 <code>window.closed</code> 는
     * true 로 남는다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * SwingWebPanel panel = new SwingWebPanel() {
     *     protected void closeRootWindow() {
     *         frame.dispose(); // close only this window instead of ending the program
     *         dispose();
     *     }
     * };
     * </pre>
     */
    protected void closeRootWindow() {
        System.exit(0);
    }

    /**
     * Turns the popup blocker of this component on or off (extension, API contract 5.28). While it is on, which it is at first, a
     * window that a page asks for with <code>window.open</code> is opened only if the user did something in the page shortly before (a
     * press of a mouse button or a key, within about five seconds), and one such action opens one window; a form that a script submits
     * into a new window follows the same rule. A blocked window is reported to {@link #popupBlocked(WindowRequest)} and
     * <code>window.open</code> returns null. While it is off, every such window is opened. Links that the user clicks are never
     * blocked. A window that a page opens takes the setting of its opener. The setting stays through {@link #clean()}.
     * <p>
     * 이 컴포넌트의 팝업 차단을 켜거나 끈다(확장, API 계약 5.28). 켜져 있는 동안(처음에 켜져 있다) 페이지가 <code>window.open</code> 으로 요청한
     * 창은 사용자가 직전에 페이지에서 무언가 했을 때만(마우스 단추나 키를 누름, 약 5초 안) 열리고, 그런 조작 한 번이 창 하나를 연다. 스크립트가
     * 새 창으로 제출한 폼도 같은 규칙을 따른다. 차단한 창은 {@link #popupBlocked(WindowRequest)} 에 알리고 <code>window.open</code> 은 null 을
     * 돌려준다. 꺼져 있으면 그런 창을 모두 연다. 사용자가 누른 링크는 차단하지 않는다. 페이지가 연 창은 여는 창의 설정을 이어받는다. 설정은
     * {@link #clean()} 뒤에도 유지된다.
     *
     * @param enabled true to block / 차단하려면 true
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setPopupBlockingEnabled(final boolean enabled) {
        checkAlive();
        if (!session.setPopupBlockingEnabled(enabled)) {
            return;
        }
        final Boolean old = Boolean.valueOf(!enabled);
        if (SwingUtilities.isEventDispatchThread()) {
            firePropertyChange(PROPERTY_POPUP_BLOCKING_ENABLED, old, Boolean.valueOf(enabled));
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Announces the change on the EDT. / EDT 에서 변경을 알린다. */
                public void run() {
                    firePropertyChange(PROPERTY_POPUP_BLOCKING_ENABLED, old, Boolean.valueOf(enabled));
                }
            });
        }
    }

    /**
     * Tells whether the popup blocker of this component is on (extension, API contract 5.28).
     * <p>
     * 이 컴포넌트의 팝업 차단이 켜져 있는지 알려 준다(확장, API 계약 5.28).
     *
     * @return true if windows that a page asks for without an action of the user are blocked; true at first / 페이지가 사용자의 조작 없이
     *         요청한 창을 차단하면 true. 처음에 true
     */
    public boolean isPopupBlockingEnabled() {
        return session.isPopupBlockingEnabled();
    }

    /**
     * Creates the component of a window for a page of this component (plan item P31-08): asks the overridable factory, checks that the
     * result really was made from the seed, and gives it the size that the page asked for.
     * <p>
     * 이 컴포넌트의 페이지를 위한 창의 컴포넌트를 만든다(계획 P31-08): 재정의할 수 있는 팩터리에 묻고, 결과가 정말 그 씨앗으로 만든 것인지
     * 확인하고, 페이지가 요청한 크기를 준다.
     *
     * @param shared  the document loop that the window shares with its opener, or null / 창이 여는 창과 함께 쓰는 문서 루프 또는 null
     * @param request what the page asked for / 페이지가 요청한 것
     * @return the component, or null if none could be created / 컴포넌트. 만들 수 없었으면 null
     */
    private SwingWebPanel newPopupPanel(DocumentLoop shared, OpenRequest request) {
        if (disposed) {
            return null;
        }
        PopupSeed seed = new PopupSeed(this, shared);
        SwingWebPanel child;
        try {
            child = createPopupPanel(seed);
        } catch (RuntimeException e) {
            child = null;
        }
        // 씨앗으로 만들지 않은 컴포넌트는 여는 창과 이어져 있지 않으므로 쓸 수 없다.
        if (child == null || child.creator != this || !seed.used) {
            if (child != null && child != this) {
                child.disposeKeepingStorage();
            }
            return null;
        }
        int w = request.getWidth() > 0 ? request.getWidth() : DEFAULT_WIDTH;
        int h = request.getHeight() > 0 ? request.getHeight() : DEFAULT_HEIGHT;
        // 창이 보이기 전에도 페이지가 요청한 크기의 뷰포트를 본다(innerWidth 등).
        child.setSize(w, h);
        child.view.setViewport(w, h);
        return child;
    }

    /**
     * Creates the component of a new window for a link, a form or a choice of the user, and starts it; called by
     * {@link WindowRequest#getPanel()} on the event dispatch thread.
     * <p>
     * 링크, 폼, 사용자의 선택을 위한 새 창의 컴포넌트를 만들고 시작한다. {@link WindowRequest#getPanel()} 이 이벤트 디스패치 스레드에서 부른다.
     *
     * @param request what was asked for / 요청한 것
     * @return the component, or null if none could be created / 컴포넌트. 만들 수 없었으면 null
     */
    SwingWebPanel createLinkedPanel(OpenRequest request) {
        SwingWebPanel child = newPopupPanel(null, request);
        if (child != null) {
            child.session.startAsOpenedWindow(session, request, false);
        }
        return child;
    }

    /**
     * Creates the component of a new window that a page of this component opens (extension point, API contract 5.28). The default
     * creates a plain {@link SwingWebPanel}. A subclass overrides it to use its own class:
     * <code>return new MyPanel(seed);</code>, where the constructor of <code>MyPanel</code> calls <code>super(seed)</code>. For
     * <code>window.open</code> it is called on the thread that runs the calling script, not on the event dispatch thread: it must
     * only construct the component and return. The new window is shown later through {@link #openPopupWindow(WindowRequest)} or
     * {@link #openNewWindow(WindowRequest)}.
     * <p>
     * 이 컴포넌트의 페이지가 연 새 창의 컴포넌트를 만든다(확장 지점, API 계약 5.28). 기본 구현은 보통의 {@link SwingWebPanel} 을 만든다. 하위
     * 클래스는 자기 클래스를 쓰려고 재정의한다: <code>return new MyPanel(seed);</code>. 이때 <code>MyPanel</code> 의 생성자가
     * <code>super(seed)</code> 를 부른다. <code>window.open</code> 에서는 이벤트 디스패치 스레드가 아니라 호출한 스크립트를 실행하는 스레드에서
     * 불리므로, 컴포넌트를 만들어 돌려주기만 해야 한다. 새 창은 나중에 {@link #openPopupWindow(WindowRequest)} 나
     * {@link #openNewWindow(WindowRequest)} 를 거쳐 보인다.
     *
     * @param seed the seed to pass to the constructor / 생성자에 넘길 씨앗
     * @return the component, or null to open no window / 컴포넌트. 창을 열지 않으려면 null
     */
    protected SwingWebPanel createPopupPanel(PopupSeed seed) {
        return new SwingWebPanel(seed);
    }

    /**
     * Tells whether this component can show windows that pages open with <code>window.open</code> (extension point, API contract
     * 5.28). The default says yes when the JVM has a display. If it says no, <code>window.open</code> returns null for a target that
     * needs a new window, as it did before windows were supported. It is called on the thread that runs the calling script and must
     * return at once. A window that a page opened asks the component that opened it.
     * <p>
     * 이 컴포넌트가 페이지가 <code>window.open</code> 으로 연 창을 보일 수 있는지 알려 준다(확장 지점, API 계약 5.28). 기본 구현은 JVM 에
     * 화면이 있으면 그렇다고 답한다. 아니라고 답하면, 창을 지원하기 전과 같이 새 창이 필요한 대상에 <code>window.open</code> 이 null 을
     * 돌려준다. 호출한 스크립트를 실행하는 스레드에서 불리며 곧바로 돌아와야 한다. 페이지가 연 창은 자신을 연 컴포넌트에 묻는다.
     *
     * @return true if a window can be shown / 창을 보일 수 있으면 true
     */
    protected boolean canOpenPopupWindows() {
        if (creator != null) {
            return creator.canOpenPopupWindows();
        }
        return !java.awt.GraphicsEnvironment.isHeadless();
    }

    /**
     * Shows the window that a script opened with <code>window.open</code> (extension point, API contract 5.28). It is called on the
     * event dispatch thread shortly after the call of the script; the component of the window is {@link WindowRequest#getPanel()}.
     * The default shows it in a window of its own: a modeless dialog that belongs to the window of this component, with the size and
     * place that the page asked for, the title of the page as its title, and a scroll pane around the component. The dialog is closed
     * when the user closes it, when a script closes the window, and together with the window that it belongs to; the component is
     * then disposed with {@link #disposeKeepingStorage()}. A host overrides the method to show the component elsewhere (a tab, a
     * frame of its own) and then disposes it itself, or calls {@link WindowRequest#reject()} to open nothing. A window that a page
     * opened passes the request to the component that opened it.
     * <p>
     * 스크립트가 <code>window.open</code> 으로 연 창을 보인다(확장 지점, API 계약 5.28). 스크립트의 호출 직후에 이벤트 디스패치 스레드에서
     * 불리며, 창의 컴포넌트는 {@link WindowRequest#getPanel()} 이다. 기본 구현은 그것을 자기 창에 보인다: 이 컴포넌트의 창에 속한 비모달 대화
     * 상자이며, 페이지가 요청한 크기와 위치를 갖고, 페이지의 제목이 창의 제목이며, 컴포넌트를 스크롤 창으로 감싼다. 대화 상자는 사용자가 닫을
     * 때, 스크립트가 창을 닫을 때, 자신이 속한 창이 닫힐 때 함께 닫히고, 그때 컴포넌트를 {@link #disposeKeepingStorage()} 로 폐기한다. 호스트는
     * 메서드를 재정의하여 컴포넌트를 다른 곳(탭, 자기 프레임)에 보이고 폐기도 직접 하거나, {@link WindowRequest#reject()} 를 불러 아무것도 열지
     * 않는다. 페이지가 연 창은 요청을 자신을 연 컴포넌트에 넘긴다.
     *
     * @param request the window to show / 보일 창
     */
    protected void openPopupWindow(WindowRequest request) {
        if (creator != null) {
            creator.openPopupWindow(request);
            return;
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            request.reject();
            return;
        }
        PopupDialog.show(this, request);
    }

    /**
     * Offers a new window that a link, a form or the user asked for (extension point, API contract 5.28): the target of a link or a
     * form names no existing window ({@link WindowRequest#LINK}), or the user chose to open a link in a new tab or window with Ctrl,
     * Shift or the middle button ({@link WindowRequest#USER}). It is called on the event dispatch thread. The default returns false,
     * and the page then behaves as it does without new windows: the link or form navigates the context that it is in, and a click
     * with a modifier is an ordinary click. A host that opens such windows shows {@link WindowRequest#getPanel()} (or opens
     * {@link WindowRequest#getUrl()} in a component of its own) and returns true. A window that a page opened passes the request to
     * the component that opened it.
     * <p>
     * 링크, 폼, 사용자가 요청한 새 창을 제안한다(확장 지점, API 계약 5.28): 링크나 폼의 대상이 기존 창을 가리키지 않거나
     * ({@link WindowRequest#LINK}), 사용자가 Ctrl, Shift, 가운데 단추로 링크를 새 탭이나 창에서 열기로 골랐다({@link WindowRequest#USER}).
     * 이벤트 디스패치 스레드에서 불린다. 기본 구현은 false 를 반환하고, 그러면 페이지는 새 창이 없을 때처럼 동작한다: 링크나 폼은 자기가 있는
     * 문맥을 이동시키고, 보조 키를 누른 클릭은 보통의 클릭이다. 그런 창을 여는 호스트는 {@link WindowRequest#getPanel()} 을 보이고(또는
     * {@link WindowRequest#getUrl()} 을 자기 컴포넌트에서 열고) true 를 반환한다. 페이지가 연 창은 요청을 자신을 연 컴포넌트에 넘긴다.
     *
     * @param request the window that was asked for / 요청한 창
     * @return true if the host opened the window (or decided to open nothing) / 호스트가 창을 열었으면(또는 아무것도 열지 않기로 했으면) true
     */
    protected boolean openNewWindow(WindowRequest request) {
        return creator != null && creator.openNewWindow(request);
    }

    /**
     * Receives a window that was not opened because the popup blocker is on and the page asked without an action of the user
     * (extension point, API contract 5.28). It is called on the event dispatch thread. The default shows a message in the status text
     * and tells the console listeners a warning. A host overrides it to tell the user and to offer to open the address after all
     * (which then is a window without a connection to the page that asked). A window that a page opened passes the request to the
     * component that opened it.
     * <p>
     * 팝업 차단이 켜져 있고 페이지가 사용자의 조작 없이 요청했기 때문에 열지 않은 창을 받는다(확장 지점, API 계약 5.28). 이벤트 디스패치
     * 스레드에서 불린다. 기본 구현은 상태 문구에 메시지를 보이고 콘솔 리스너에 경고를 알린다. 호스트는 이것을 재정의하여 사용자에게 알리고, 그
     * 주소를 그래도 열 수 있게 제안한다(그렇게 연 창은 요청한 페이지와 관계가 없는 창이다). 페이지가 연 창은 요청을 자신을 연 컴포넌트에 넘긴다.
     *
     * @param request the window that was blocked; it has no component / 차단한 창. 컴포넌트가 없다
     */
    protected void popupBlocked(WindowRequest request) {
        if (creator != null) {
            creator.popupBlocked(request);
            return;
        }
        String what = request.getUrl().length() == 0 ? "about:blank" : request.getUrl();
        session.setStatusMessage("Popup blocked: " + what);
        consoles.publish(ConsoleEvent.Level.WARN, ConsoleEvent.Kind.HOST_ERROR, "open", "Popup blocked: " + what
                + " (the page asked for a window without an action of the user)", new ArrayList<String>(), ConsoleEvent.Origin.DOCUMENT,
                request.getDocumentUrl(), "", 0, 0);
    }

    /**
     * Closes the window that shows this component after a script of its page closed it with <code>window.close()</code> (extension
     * point, API contract 5.28). It is called on the event dispatch thread, and only for a component that a page opened (a script
     * closes the window of any other component through {@link #closeRootWindow()}, and only if the host allowed it). The default
     * closes the popup window that the default {@link #openPopupWindow(WindowRequest)} created; a component that the host placed
     * elsewhere is only disposed with {@link #disposeKeepingStorage()}. A host that shows such components overrides the method to
     * remove the tab or close the frame.
     * <p>
     * 페이지의 스크립트가 <code>window.close()</code> 로 닫은 뒤, 이 컴포넌트를 보이는 창을 닫는다(확장 지점, API 계약 5.28). 이벤트 디스패치
     * 스레드에서 불리며, 페이지가 연 컴포넌트에만 불린다(그 밖의 컴포넌트의 창은 스크립트가 {@link #closeRootWindow()} 를 거쳐, 호스트가 허용했을
     * 때만 닫는다). 기본 구현은 기본 {@link #openPopupWindow(WindowRequest)} 가 만든 팝업 창을 닫는다. 호스트가 다른 곳에 둔 컴포넌트는
     * {@link #disposeKeepingStorage()} 로 폐기만 한다. 그런 컴포넌트를 보이는 호스트는 메서드를 재정의하여 탭을 없애거나 프레임을 닫는다.
     */
    protected void closeOpenedWindow() {
        Runnable closer = windowCloser;
        if (closer != null) {
            closer.run();
        } else {
            disposeKeepingStorage();
        }
    }

    /**
     * Sets what closes the window that shows this component when a script closes it; used by the default popup window.
     * <p>
     * 스크립트가 닫을 때 이 컴포넌트를 보이는 창을 닫는 것을 정한다. 기본 팝업 창이 쓴다.
     *
     * @param closer the action, or null / 동작 또는 null
     */
    void setWindowCloser(Runnable closer) {
        windowCloser = closer;
    }

    /**
     * Goes back one entry in the history; does nothing if there is no previous entry.
     * <p>
     * 이력을 한 칸 뒤로 이동한다. 이전 항목이 없으면 아무 동작도 하지 않는다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void back() {
        session.back(1);
    }

    /**
     * Goes back n entries; does nothing if n is not positive or if the history does not reach back exactly n entries.
     * <p>
     * 이력을 n 칸 뒤로 이동한다. n 이 양수가 아니거나 이력이 정확히 n 칸 뒤까지 없으면 아무 동작도 하지 않는다.
     *
     * @param n number of entries / 칸 수
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void back(int n) {
        session.back(n);
    }

    /**
     * Goes forward one entry in the history; does nothing if there is no next entry.
     * <p>
     * 이력을 한 칸 앞으로 이동한다. 다음 항목이 없으면 아무 동작도 하지 않는다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void forward() {
        session.forward(1);
    }

    /**
     * Goes forward n entries; does nothing if n is not positive or if the history does not reach forward exactly n entries.
     * <p>
     * 이력을 n 칸 앞으로 이동한다. n 이 양수가 아니거나 이력이 정확히 n 칸 앞까지 없으면 아무 동작도 하지 않는다.
     *
     * @param n number of entries / 칸 수
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void forward(int n) {
        session.forward(n);
    }

    /**
     * Returns a copy of the history: the URL of every confirmed main-document navigation, oldest first.
     * <p>
     * 이력의 복사본을 반환한다: 확정된 주 문서 이동의 URL 목록(오래된 것이 앞).
     *
     * @return history URLs / 이력 URL 목록
     */
    public Vector<String> getHistories() {
        return session.getHistories();
    }

    /**
     * Returns the current position in the history, 0 based (extension method).
     * <p>
     * 이력에서의 현재 위치(0 기반)를 반환한다(확장 메서드).
     *
     * @return position / 위치
     */
    public int getHistoryIndex() {
        return session.getHistoryIndex();
    }

    /**
     * Reloads the current page, respecting caches.
     * <p>
     * 캐시 정책을 따라 현재 페이지를 다시 읽는다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void refresh() {
        session.refresh(false);
    }

    /**
     * Reloads the current page. With force, the page and its sub-resources bypass this component's caches.
     * <p>
     * 현재 페이지를 다시 읽는다. force 이면 페이지와 하위 리소스가 이 컴포넌트의 캐시를 우회한다.
     *
     * @param force true to bypass caches / 캐시를 우회하려면 true
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void refresh(boolean force) {
        session.refresh(force);
    }

    /**
     * Cancels the running navigation, if any (extension method).
     * <p>
     * 진행 중인 이동이 있으면 취소한다(확장 메서드).
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void stop() {
        session.stop();
    }

    /**
     * Returns the source text the browser received for the confirmed page, unchanged by scripts or DOM edits. For content
     * given with setContent it is exactly the given string; for the blank page it is the empty string.
     * <p>
     * 확정된 페이지에 대해 브라우저가 받은 원문을 반환한다. 스크립트나 DOM 변경의 영향을 받지 않는다. setContent 로 넣은
     * 콘텐츠는 입력 문자열 그대로이고 빈 페이지는 빈 문자열이다.
     *
     * @return source text / 원문
     */
    public String getContent() {
        return session.getContent();
    }

    /**
     * Returns the MIME type of the confirmed page, for example "text/html" (extension method).
     * <p>
     * 확정된 페이지의 MIME 타입(예: "text/html")을 반환한다(확장 메서드).
     *
     * @return MIME type / MIME 타입
     */
    public String getMimeType() {
        return session.getMimeType();
    }

    /**
     * Shows content you already have. The page URL becomes "about:text" and the history gets a new entry.
     * Supported types: text/html, text/plain, text/markdown, application/xhtml+xml (parameters such as charset and letter case
     * are ignored).
     * <p>
     * 이미 가진 콘텐츠를 보여 준다. 페이지 URL 은 "about:text" 가 되고 이력에 새 항목이 생긴다.
     * 지원 타입: text/html, text/plain, text/markdown, application/xhtml+xml(charset 같은 매개변수와 대소문자는 무시).
     *
     * @param content     source text, may be empty / 원문(빈 문자열 허용)
     * @param contentType MIME type / MIME 타입
     * @throws IllegalArgumentException if a value is null or the type is not supported / 값이 null 이거나 지원하지 않는 타입일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setContent(String content, String contentType) {
        session.setContent(content, contentType);
    }

    /**
     * Returns the title of the confirmed page, or an empty string (extension method).
     * <p>
     * 확정된 페이지의 제목을 반환한다. 없으면 빈 문자열이다(확장 메서드).
     *
     * @return title / 제목
     */
    public String getTitle() {
        return session.getTitle();
    }

    /**
     * Returns the text of the status bar, for example "Loading http://..." or "Done".
     * <p>
     * 상태바 메시지를 반환한다. 예: "Loading http://..." 또는 "Done".
     *
     * @return status text / 상태 메시지
     */
    public String getStatusText() {
        return session.getStatusText();
    }

    /**
     * Returns the progress of loading: 0 to 100, or -1 while the total amount of work is unknown. A value of 100 means that
     * the page and its required sub-work finished (successfully or not), not merely that a connection was made.
     * <p>
     * 로딩 진행률을 반환한다: 0~100, 전체 작업량을 모르는 동안은 -1. 100 은 연결이 되었다는 뜻이 아니라 페이지와 필수 하위 작업이
     * (성공하든 실패하든) 끝났다는 뜻이다.
     *
     * @return progress / 진행률
     */
    public int getProgress() {
        return session.getProgress();
    }

    /**
     * Registers a hyperlink listener. Registering the same listener twice notifies it twice.
     * <p>
     * 하이퍼링크 리스너를 등록한다. 같은 리스너를 두 번 등록하면 두 번 통지한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void addHyperlinkListener(HyperlinkListener listener) {
        checkAlive();
        links.add(listener);
    }

    /**
     * Removes one registration of a hyperlink listener; unknown listeners are ignored.
     * <p>
     * 하이퍼링크 리스너 등록 하나를 제거한다. 등록되지 않은 리스너는 무시한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     */
    public void removeHyperlinkListener(HyperlinkListener listener) {
        links.remove(listener);
    }

    /**
     * Removes all hyperlink listeners registered through this component. Following links by clicking keeps working.
     * <p>
     * 이 컴포넌트에 등록된 모든 하이퍼링크 리스너를 제거한다. 클릭으로 링크를 따라가는 동작은 계속 작동한다.
     */
    public void removeHyperlinkEvents() {
        links.clear();
    }

    /**
     * Registers a console listener. It receives what the scripts of the pages write with <code>console.log</code>,
     * <code>console.error</code> and the other console methods (of the main document, its frames and its workers) and the problems the
     * browser reports for the scripts, on the event dispatch thread. Registering the same listener twice notifies it twice. Without a
     * listener the console output is discarded. See {@link ConsoleListener} and {@link ConsoleEvent}.
     * <p>
     * 콘솔 리스너를 등록한다. 페이지 스크립트가 <code>console.log</code>, <code>console.error</code> 등 콘솔 메서드로 쓰는 것(주 문서, 그
     * 프레임, 그 worker)과 브라우저가 스크립트에 대해 보고하는 문제를 이벤트 디스패치 스레드에서 받는다. 같은 리스너를 두 번 등록하면 두 번
     * 통지한다. 리스너가 없으면 콘솔 출력은 버려진다. {@link ConsoleListener}, {@link ConsoleEvent} 참조.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void addConsoleListener(ConsoleListener listener) {
        checkAlive();
        consoles.add(listener);
    }

    /**
     * Removes one registration of a console listener; unknown listeners are ignored.
     * <p>
     * 콘솔 리스너 등록 하나를 제거한다. 등록되지 않은 리스너는 무시한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     * @throws IllegalArgumentException if the listener is null / 리스너가 null 일 때
     */
    public void removeConsoleListener(ConsoleListener listener) {
        consoles.remove(listener);
    }

    /**
     * Returns the registered console listeners in registration order (a copy; empty after {@link #dispose()}).
     * <p>
     * 등록된 콘솔 리스너를 등록 순서대로 반환한다(복사본. {@link #dispose()} 뒤에는 비어 있다).
     *
     * @return the listeners / 리스너
     */
    public ConsoleListener[] getConsoleListeners() {
        return consoles.getListeners();
    }

    /**
     * Registers a page message listener. Call from any thread.
     * Messages are delivered on the EDT. Validate the sender origin and shape of the data.
     * <p>
     * 페이지 메시지 리스너를 등록한다. 어느 스레드에서나 호출할 수 있다.
     * EDT에서 메시지를 받는다. 송신 오리진과 데이터 형식을 검사한다.
     *
     * @param listener non-null listener / null이 아닌 리스너
     * @throws IllegalArgumentException for a null listener / 리스너가 null이면
     * @throws IllegalStateException when disposed / 폐기했으면
     */
    public void addWebMessageListener(WebMessageListener listener) {
        checkAlive();
        webMessages.add(listener);
    }

    /**
     * Removes one registration of a web message listener.
     * <p>
     * 웹 메시지 리스너의 등록 하나를 제거한다.
     *
     * @param listener non-null listener / null이 아닌 리스너
     * @throws IllegalArgumentException for a null listener / 리스너가 null이면
     * @throws IllegalStateException when disposed / 폐기했으면
     */
    public void removeWebMessageListener(WebMessageListener listener) {
        checkAlive();
        webMessages.remove(listener);
    }

    /**
     * Returns a copy of web message listeners in registration order.
     * <p>
     * 등록 순서로 웹 메시지 리스너 배열의 사본을 반환한다.
     *
     * @return registered listeners, empty after disposal / 등록한 리스너. 폐기 후에는 빈 배열
     */
    public WebMessageListener[] getWebMessageListeners() {
        return webMessages.getListeners();
    }

    /**
     * Cancels all work, shows the blank page and resets the history to a single "about:blank" entry. Also clears this
     * component's caches and sessionStorage and the process-wide localStorage, which also empties the localStorage seen by
     * other components in the same JVM. User agent, extra headers, cookies, the JavaScript setting and listeners are kept.
     * <p>
     * 모든 작업을 취소하고 빈 페이지를 보여 주며 이력을 "about:blank" 하나로 초기화한다. 이 컴포넌트의 캐시와 sessionStorage,
     * 그리고 프로세스 공용 localStorage 도 비우므로 같은 JVM 의 다른 컴포넌트가 보는 localStorage 도 비워진다.
     * User-Agent, 추가 헤더, 쿠키, JavaScript 설정, 리스너는 유지한다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void clean() {
        checkAlive();
        webMessages.beginReset();
        try {
            input.reset();
            downloads.cancelAll();
            session.clean();
            view.clear();
            fetcher.getCache().clear();
        } finally {
            webMessages.endReset();
        }
    }

    /**
     * Releases everything: does what {@link #clean()} does, then removes all listeners registered on this component, stops
     * background work and forgets headers. Calling it again does nothing. Afterwards, changing methods throw
     * IllegalStateException and query methods return the empty state.
     * <p>
     * 모든 것을 해제한다: {@link #clean()} 이 하는 일을 한 뒤 이 컴포넌트에 등록된 모든 리스너를 제거하고 백그라운드 작업을 멈추며
     * 헤더를 잊는다. 다시 호출해도 아무 일도 없다. 이후 변경 메서드는 IllegalStateException 을 던지고 조회 메서드는 빈 상태를 반환한다.
     */
    public void dispose() {
        dispose(false);
    }

    /**
     * Disposes the component like {@link #dispose()} but leaves the process-wide <code>localStorage</code> as it is (extension, API
     * contract 5.28). {@link #dispose()} clears it, as the contract of the component says, which also empties it for every other
     * component of the process; a host that shows several components side by side (tabs, popup windows) closes one of them with this
     * method so that the others keep their stored data. Everything else is as in {@link #dispose()}: the page, the history, the
     * session storage, the cache of the component, the listeners. Calling it again, or after {@link #dispose()}, does nothing.
     * <p>
     * {@link #dispose()} 처럼 컴포넌트를 폐기하되 프로세스 공용 <code>localStorage</code> 는 그대로 둔다(확장, API 계약 5.28). {@link #dispose()}
     * 는 컴포넌트의 계약대로 그것을 비우는데, 그러면 프로세스의 다른 모든 컴포넌트에서도 비워진다. 여러 컴포넌트를 나란히 보이는 호스트(탭, 팝업
     * 창)는 그중 하나를 이 메서드로 닫아 나머지가 저장 데이터를 유지하게 한다. 그 밖의 것은 {@link #dispose()} 와 같다: 페이지, 이력,
     * sessionStorage, 컴포넌트의 캐시, 리스너. 다시 부르거나 {@link #dispose()} 뒤에 불러도 아무 일도 없다.
     */
    public void disposeKeepingStorage() {
        dispose(true);
    }

    /**
     * Disposes the component.
     * <p>
     * 컴포넌트를 폐기한다.
     *
     * @param keepLocalStorage true to leave the process-wide localStorage untouched / 프로세스 공용 localStorage 를 건드리지 않으려면 true
     */
    private void dispose(boolean keepLocalStorage) {
        synchronized (internalListeners) {
            if (disposed) {
                return;
            }
            disposed = true;
        }
        webMessages.close();
        input.dispose();
        keyboard.dispose();
        finder.dispose();
        composition.dispose();
        downloads.dispose();
        session.dispose(keepLocalStorage);
        view.dispose();
        fetcher.getCache().clear();
        links.clear();
        // 콘솔 리스너와 아직 전달하지 못한 메시지도 버린다. 폐기 뒤에 도착하는 메시지는 무시된다.
        consoles.close();
        removeAttachedListeners();
        if (SwingUtilities.isEventDispatchThread()) {
            detachViewport();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Removes the external viewport connection on its owning EDT. / 소유 EDT에서 외부 뷰포트 연결을 제거한다. */
                public void run() {
                    detachViewport();
                }
            });
        }
    }

    /**
     * Tells whether {@link #dispose()} ran (extension method).
     * <p>
     * {@link #dispose()} 가 실행되었는지 알려 준다(확장 메서드).
     *
     * @return true if disposed / 폐기되었으면 true
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Adds extra request headers (for example Accept-Language) that are sent with every request of this component.
     * Names are case-insensitive and a later value replaces an earlier one; other headers stay. The User-Agent cannot be
     * changed here (use {@link #setUserAgent(String)}); an entry with that name is ignored.
     * <p>
     * 이 컴포넌트의 모든 요청에 실을 추가 요청 헤더(예: Accept-Language)를 지정한다. 이름은 대소문자를 구분하지 않으며 나중 값이
     * 이전 값을 덮어쓰고 다른 헤더는 유지된다. User-Agent 는 여기서 바꿀 수 없으며({@link #setUserAgent(String)} 사용) 그 이름의
     * 항목은 무시한다.
     *
     * @param headers header names and values: no nulls, no CR/LF / 헤더 이름과 값(null, CR/LF 불가)
     * @throws IllegalArgumentException for a null map or invalid entries (nothing is applied) / null 맵이나 잘못된 항목일 때(아무것도 적용하지 않음)
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void addHeaders(Map<String, String> headers) {
        session.addHeaders(headers);
    }

    /**
     * Returns a copy of the extra headers (without the User-Agent).
     * <p>
     * 추가 헤더의 복사본을 반환한다(User-Agent 제외).
     *
     * @return headers / 헤더
     */
    public Map<String, String> getHeaders() {
        return session.getHeaders();
    }

    /**
     * Returns all cookies of this component as one string "name=value; name=value". The string is empty while no cookie was
     * stored. Expired cookies are excluded; cookies with the same name at different domains or paths are preserved.
     * <p>
     * 이 컴포넌트의 모든 쿠키를 "name=value; name=value" 한 문자열로 반환한다. 저장된 쿠키가 없으면 빈 문자열이다.
     * 만료된 쿠키는 제외하며 도메인이나 경로가 다른 동명 쿠키도 보존한다.
     *
     * @return cookies / 쿠키
     */
    public String getCookies() {
        return session.getCookies();
    }

    /**
     * Returns the cookie jar that this component was created with (extension, API contract 5.24).
     * <p>
     * 이 컴포넌트를 만들 때 넘긴 쿠키 저장소를 반환한다(확장, API 계약 5.24).
     *
     * @return the shared jar, or null if the component has a cookie store of its own / 공유 저장소. 컴포넌트가 자기 쿠키 저장소를 가지면 null
     */
    public CookieJar getCookieJar() {
        return cookieJar;
    }

    /**
     * Removes all cookies of this component (extension, API contract 5.24); {@link #clean()} keeps them. If the component shares a
     * {@link CookieJar}, the cookies of the jar are removed for all components that share it. The page that is shown keeps running.
     * <p>
     * 이 컴포넌트의 모든 쿠키를 지운다(확장, API 계약 5.24). {@link #clean()} 은 쿠키를 유지한다. 컴포넌트가 {@link CookieJar} 를 공유하면 그
     * 저장소의 쿠키가 공유하는 모든 컴포넌트에서 지워진다. 보이는 페이지는 계속 실행된다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void clearCookies() {
        checkAlive();
        cookies.clear();
    }

    /**
     * Returns a read-only copy of all localStorage items of all origins, shared by the components of this JVM process.

     * <p>
     * 이 JVM 프로세스의 컴포넌트가 공유하는 모든 오리진의 localStorage 항목을 읽기 전용 복사본으로 반환한다.

     *
     * @return copy of the items / 항목 복사본
     */
    public Collection<StorageEntry> getLocalStorage() {
        return session.getLocalStorage();
    }

    /**
     * Returns a read-only copy of all sessionStorage items of this component (all origins).

     * <p>
     * 이 컴포넌트의 모든 sessionStorage 항목(모든 오리진)을 읽기 전용 복사본으로 반환한다.

     *
     * @return copy of the items / 항목 복사본
     */
    public Collection<StorageEntry> getSessionStorage() {
        return session.getSessionStorage();
    }

    /**
     * Tells the pages whether the host has a network connection (extension method): <code>navigator.onLine</code> follows it and the pages hear
     * <code>online</code> or <code>offline</code> at their window when it changes. The component cannot detect the network state, so the host that knows it
     * (a network monitor of the application) calls this. The default is true; a call that does not change the state does nothing.
     * <p>
     * 호스트에 네트워크 연결이 있는지 페이지에 알린다(확장 메서드): <code>navigator.onLine</code> 이 이를 따르고 상태가 바뀌면 페이지가 window 에서 <code>online</code> 또는
     * <code>offline</code> 을 듣는다. 컴포넌트는 네트워크 상태를 알아낼 수 없으므로 아는 쪽(응용 프로그램의 네트워크 감시기)이 이 메서드를 부른다. 기본값은
     * true 이며 상태가 바뀌지 않는 호출은 아무것도 하지 않는다.
     *
     * @param online true if the host is online / 호스트가 연결되어 있으면 true
     */
    public void setOnline(boolean online) {
        session.setOnline(online);
    }

    /**
     * Tells whether the host said that it has a network connection (extension method, see {@link #setOnline(boolean)}).
     * <p>
     * 호스트가 네트워크 연결이 있다고 했는지 알려 준다(확장 메서드, {@link #setOnline(boolean)} 참조).
     *
     * @return true while online (the default) / 연결된 동안 true(기본값)
     */
    public boolean isOnline() {
        return session.isOnline();
    }

    /**
     * Returns the page zoom factor (extension method): 1 shows the page at 100 percent, 2 at 200 percent.
     * <p>
     * 페이지 확대 배율을 반환한다(확장 메서드): 1 은 100 %, 2 는 200 % 로 보여 준다.
     *
     * @return the factor, from 0.25 to 5.0 / 배율. 0.25 부터 5.0 까지
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * Zooms the page (extension method; the public methods of the component are not changed by it): the page is laid out for a viewport that
     * is as many times smaller as the factor says (so media queries, percentage widths and line breaks follow what the user sees), and the
     * drawing, the mouse coordinates, the scroll size, the selection highlight and the editors of the form controls use the same factor. A
     * factor outside 0.25 to 5.0 is cut to the nearest end of the range. Ctrl+mouse wheel and Ctrl+plus, Ctrl+minus, Ctrl+0 call it too.
     * The change is applied on the event dispatch thread (a call from another thread is queued) and announced as the
     * {@link #PROPERTY_ZOOM} property change. The zoom is a setting of the component: it stays when another page is shown, and neither
     * {@link #clean()} nor a page changes it.
     * <p>
     * 페이지를 확대한다(확장 메서드. 컴포넌트의 공개 메서드는 이것으로 바뀌지 않는다): 페이지는 배율만큼 작은 뷰포트에 맞춰 배치되고(그래서 미디어 질의, 백분율 폭,
     * 줄바꿈이 사용자가 보는 것을 따른다) 그리기, 마우스 좌표, 스크롤 크기, 선택 강조, 폼 컨트롤의 편집기가 같은 배율을 쓴다. 0.25 ~ 5.0 밖의 배율은 범위의 가까운
     * 끝으로 맞춘다. Ctrl+마우스 휠과 Ctrl+플러스, Ctrl+마이너스, Ctrl+0 도 이것을 부른다. 변경은 이벤트 디스패치 스레드에서 적용되며(다른 스레드의 호출은
     * 큐에 들어간다) {@link #PROPERTY_ZOOM} 속성 변경으로 알린다. 확대는 컴포넌트의 설정이다: 다른 페이지를 보여 줘도 유지되며 {@link #clean()} 도 페이지도 바꾸지 않는다.
     *
     * @param factor the factor, a finite positive number / 배율. 유한한 양수
     * @throws IllegalArgumentException for NaN, infinite or not positive numbers / NaN, 무한, 양수가 아닌 수일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setZoom(double factor) {
        checkAlive();
        final double target = PageZoom.clamp(factor);
        final double old = zoom;
        zoom = target;
        if (SwingUtilities.isEventDispatchThread()) {
            applyZoom(old, target);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Applies the zoom on the EDT. / EDT 에서 확대를 적용한다. */
                public void run() {
                    if (!disposed) {
                        applyZoom(old, target);
                    }
                }
            });
        }
    }

    /**
     * Applies a new zoom factor on the EDT: the view, the editors and the scroll size change, the viewport keeps the point that the user
     * zoomed around (the anchor of Ctrl+wheel, else the top left corner), and the property change is announced.
     * <p>
     * 새 확대 배율을 EDT 에서 적용한다: 뷰, 편집기, 스크롤 크기가 바뀌고, 뷰포트는 사용자가 확대의 기준으로 삼은 점(Ctrl+휠의 기준점, 없으면 왼쪽 위 모서리)을
     * 그대로 유지하며, 속성 변경을 알린다.
     *
     * @param old    the factor before / 이전 배율
     * @param target the new factor / 새 배율
     */
    private void applyZoom(double old, double target) {
        JViewport viewport = getParent() instanceof JViewport ? (JViewport) getParent() : null;
        Point position = viewport == null ? new Point() : viewport.getViewPosition();
        int fx = zoomAnchorX >= 0 ? Math.max(0, zoomAnchorX - position.x) : 0;
        int fy = zoomAnchorY >= 0 ? Math.max(0, zoomAnchorY - position.y) : 0;
        zoomAnchorX = -1;
        zoomAnchorY = -1;
        view.setZoom(target);
        controls.setZoom(target);
        // 기준점 아래에 있던 문서의 점을 기억해 새 배율에서 같은 뷰포트 위치에 오게 한다.
        zoomKeep = new double[] { (position.x + fx) / old, (position.y + fy) / old, fx, fy };
        updateViewport();
        // 확대는 CSS 뷰포트 크기를 바꾸므로 스크립트에 resize 를 알리고 미디어 질의를 다시 평가하게 한다.
        session.viewportChanged(true, false);
        revalidate();
        repaint();
        controls.layout(view.getRenderedPage());
        keepZoomPoint(false);
        firePropertyChange(PROPERTY_ZOOM, Double.valueOf(old), Double.valueOf(target));
    }

    /**
     * Moves the viewport so that the document point that the last zoom keeps is at its place in the viewport again. It is called right
     * after the zoom (the size of the content is still the old one, scaled), and once more when the layout of the new viewport size is
     * published; after that call the point is forgotten.
     * <p>
     * 마지막 확대가 유지하려는 문서의 점이 다시 뷰포트 안의 제자리에 오도록 뷰포트를 움직인다. 확대 직후(내용 크기는 아직 이전 크기를 확대한 것이다)와 새 뷰포트
     * 크기의 레이아웃이 게시될 때 한 번 더 불리며, 그 호출 뒤에는 그 점을 잊는다.
     *
     * @param last true for the call after the new layout; the point is forgotten afterwards / 새 레이아웃 뒤의 호출이면 true. 그 뒤에 점을 잊는다
     */
    private void keepZoomPoint(boolean last) {
        double[] keep = zoomKeep;
        if (keep == null) {
            return;
        }
        scrollViewportTo((int) Math.round(keep[0] * zoom - keep[2]), (int) Math.round(keep[1] * zoom - keep[3]));
        if (last) {
            zoomKeep = null;
        }
    }

    /**
     * Sets one of the default font families of this component (extension method, API contract 5.27). The kind
     * {@link #FONT_STANDARD} is the font of content whose author named no font; {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF} and
     * {@link #FONT_MONOSPACE} are the fonts behind the generic names of CSS. A family that is not installed is ignored when text is
     * laid out (the built-in font of the kind is used). Fonts that the author names, and web fonts, are not affected. The pages that
     * are shown (frames included) are laid out again; the setting stays for later pages and through {@link #clean()}, and it does not
     * reach other components.
     * <p>
     * 이 컴포넌트의 기본 글꼴 패밀리 가운데 하나를 정한다(확장 메서드, API 계약 5.27). 종류 {@link #FONT_STANDARD} 는 작성자가 글꼴을 지정하지
     * 않은 내용의 글꼴이고, {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF}, {@link #FONT_MONOSPACE} 는 CSS 일반 이름 뒤의 글꼴이다. 설치되지
     * 않은 패밀리는 텍스트를 배치할 때 무시한다(그 종류의 내장 글꼴을 쓴다). 작성자가 지정한 글꼴과 웹 글꼴에는 영향이 없다. 보이는
     * 페이지(프레임 포함)를 다시 배치한다. 설정은 이후의 페이지와 {@link #clean()} 뒤에도 유지되며 다른 컴포넌트에는 닿지 않는다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, "Malgun Gothic");
     * panel.setDefaultFontSize(18);
     * </pre>
     *
     * @param kind   {@link #FONT_STANDARD}, {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF} or {@link #FONT_MONOSPACE} /
     *               {@link #FONT_STANDARD}, {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF}, {@link #FONT_MONOSPACE} 중 하나
     * @param family the font family name; null or empty for the built-in font / 글꼴 패밀리 이름. 내장 글꼴이면 null 이나 빈 문자열
     * @throws IllegalArgumentException for an unknown kind, or a name with control characters / 모르는 종류이거나 이름에 제어 문자가 있을 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setDefaultFontFamily(String kind, String family) {
        checkAlive();
        String name = family == null ? "" : family.trim();
        // 제어 문자가 든 이름은 글꼴 이름이 아니다.
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) < ' ' || name.charAt(i) == 0x7F) {
                throw new IllegalArgumentException("a font family name must not contain control characters");
            }
        }
        FontDefaults old;
        FontDefaults next;
        synchronized (fontLock) {
            old = view.getFontDefaults();
            next = old.withFamily(kind, name);
            if (next == null) {
                throw new IllegalArgumentException("unknown kind of default font: " + kind);
            }
            view.setFontDefaults(next);
        }
        announceFonts(old, next);
    }

    /**
     * Returns one of the default font families of this component (extension method, API contract 5.27).
     * <p>
     * 이 컴포넌트의 기본 글꼴 패밀리 가운데 하나를 반환한다(확장 메서드, API 계약 5.27).
     *
     * @param kind {@link #FONT_STANDARD}, {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF} or {@link #FONT_MONOSPACE} /
     *             {@link #FONT_STANDARD}, {@link #FONT_SERIF}, {@link #FONT_SANS_SERIF}, {@link #FONT_MONOSPACE} 중 하나
     * @return the family that the host set, or the empty string for the built-in font / 호스트가 정한 패밀리. 내장 글꼴이면 빈 문자열
     * @throws IllegalArgumentException for an unknown kind / 모르는 종류일 때
     */
    public String getDefaultFontFamily(String kind) {
        String family = view.getFontDefaults().getFamily(kind);
        if (family == null) {
            throw new IllegalArgumentException("unknown kind of default font: " + kind);
        }
        return family;
    }

    /**
     * Sets the default font size of this component (extension method, API contract 5.27): the size of <code>font-size: medium</code>, in
     * CSS pixels. The other size keywords scale with it, and content without an author font size is shown in this size. The built-in
     * value is 16.
     * <p>
     * 이 컴포넌트의 기본 글꼴 크기를 정한다(확장 메서드, API 계약 5.27): <code>font-size: medium</code> 의 크기(CSS 픽셀)이다. 다른 크기 키워드는
     * 이것에 비례하고, 작성자가 글꼴 크기를 지정하지 않은 내용은 이 크기로 보인다. 내장 값은 16 이다.
     *
     * @param px the size, from 6 to 72 / 크기. 6 부터 72 까지
     * @throws IllegalArgumentException for a size outside the range / 범위 밖의 크기일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setDefaultFontSize(int px) {
        checkAlive();
        if (px < FontDefaults.MIN_SIZE || px > FontDefaults.MAX_SIZE) {
            throw new IllegalArgumentException("the default font size must be from " + FontDefaults.MIN_SIZE + " to " + FontDefaults.MAX_SIZE + ": " + px);
        }
        FontDefaults old;
        FontDefaults next;
        synchronized (fontLock) {
            old = view.getFontDefaults();
            next = old.withSizes(px, old.getMonospaceSize());
            view.setFontDefaults(next);
        }
        announceFonts(old, next);
    }

    /**
     * Returns the default font size of this component (extension method, API contract 5.27).
     * <p>
     * 이 컴포넌트의 기본 글꼴 크기를 반환한다(확장 메서드, API 계약 5.27).
     *
     * @return the size of <code>font-size: medium</code> in CSS pixels; 16 unless the host set another / <code>font-size: medium</code> 의
     *         크기(CSS 픽셀). 호스트가 바꾸지 않았으면 16
     */
    public int getDefaultFontSize() {
        return view.getFontDefaults().getSize();
    }

    /**
     * Sets the font size of the monospace elements (<code>code</code>, <code>kbd</code>, <code>samp</code>, <code>tt</code>,
     * <code>pre</code> and the old <code>listing</code>, <code>xmp</code>, <code>plaintext</code>) of this component (extension method,
     * API contract 5.27), as browsers have a separate default size for fixed-width text. The size is given for text of the default
     * font size and scales with the surrounding text (inside a heading it grows with the heading). 0, the built-in value, shows these
     * elements in the size of the surrounding text. Author rules for the font size of these elements win.
     * <p>
     * 이 컴포넌트에서 고정폭 요소(<code>code</code>, <code>kbd</code>, <code>samp</code>, <code>tt</code>, <code>pre</code>, 그리고 옛
     * <code>listing</code>, <code>xmp</code>, <code>plaintext</code>)의 글꼴 크기를 정한다(확장 메서드, API 계약 5.27). 브라우저가 고정폭 글에
     * 따로 기본 크기를 두는 것과 같다. 크기는 기본 글꼴 크기의 글을 기준으로 주며 주변 글에 비례한다(제목 안에서는 제목과 함께 커진다). 내장 값인
     * 0 이면 이 요소들을 주변 글의 크기로 보인다. 이 요소들의 글꼴 크기를 정한 작성자 규칙이 우선한다.
     *
     * @param px the size from 6 to 72, or 0 for the size of the surrounding text / 6 부터 72 까지의 크기. 주변 글의 크기를 쓰면 0
     * @throws IllegalArgumentException for a size outside the range / 범위 밖의 크기일 때
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void setDefaultMonospaceFontSize(int px) {
        checkAlive();
        if (px != 0 && (px < FontDefaults.MIN_SIZE || px > FontDefaults.MAX_SIZE)) {
            throw new IllegalArgumentException("the monospace font size must be 0 or from " + FontDefaults.MIN_SIZE + " to " + FontDefaults.MAX_SIZE + ": " + px);
        }
        FontDefaults old;
        FontDefaults next;
        synchronized (fontLock) {
            old = view.getFontDefaults();
            next = old.withSizes(old.getSize(), px);
            view.setFontDefaults(next);
        }
        announceFonts(old, next);
    }

    /**
     * Returns the font size of the monospace elements of this component (extension method, API contract 5.27).
     * <p>
     * 이 컴포넌트에서 고정폭 요소의 글꼴 크기를 반환한다(확장 메서드, API 계약 5.27).
     *
     * @return the size in CSS pixels, or 0 for the size of the surrounding text / 크기(CSS 픽셀). 주변 글의 크기를 쓰면 0
     */
    public int getDefaultMonospaceFontSize() {
        return view.getFontDefaults().getMonospaceSize();
    }

    /** Guards the read-change-write of the default fonts against calls from several threads. / 여러 스레드의 호출로부터 기본 글꼴의 읽고-바꾸고-쓰기를 보호한다. */
    private final transient Object fontLock = new Object();

    /**
     * Announces a change of the default fonts as the {@link #PROPERTY_DEFAULT_FONTS} property change on the event dispatch thread.
     * <p>
     * 기본 글꼴의 변경을 {@link #PROPERTY_DEFAULT_FONTS} 속성 변경으로 이벤트 디스패치 스레드에서 알린다.
     *
     * @param old  the defaults before / 이전 기본값
     * @param next the defaults now / 지금의 기본값
     */
    private void announceFonts(final FontDefaults old, final FontDefaults next) {
        if (old.equals(next)) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            firePropertyChange(PROPERTY_DEFAULT_FONTS, old.toString(), next.toString());
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Announces the change on the EDT. / EDT 에서 변경을 알린다. */
                public void run() {
                    firePropertyChange(PROPERTY_DEFAULT_FONTS, old.toString(), next.toString());
                }
            });
        }
    }

    /**
     * Copies the selection of the page to the clipboard (extension method), as Ctrl+C does: the <code>copy</code> event is sent to the
     * page first, and a listener may cancel it or put its own data on the clipboard. If a text control of a form has the focus, the
     * selection of that control is copied. The page gets no key events. The command is run on the event dispatch thread (a call from
     * another thread is queued) and the page part of it on the document queue, so the clipboard may change shortly after the call returns.
     * <p>
     * 페이지의 선택을 Ctrl+C 와 같게 클립보드에 복사한다(확장 메서드): 먼저 <code>copy</code> 이벤트를 페이지에 보내며, 리스너는 이를 취소하거나 자기 데이터를
     * 클립보드에 넣을 수 있다. 폼의 텍스트 컨트롤에 포커스가 있으면 그 컨트롤의 선택을 복사한다. 페이지에 키 이벤트는 가지 않는다. 명령은 이벤트 디스패치
     * 스레드에서 실행되고(다른 스레드의 호출은 큐에 들어간다) 그 가운데 페이지 쪽 일은 문서 큐에서 실행되므로, 클립보드는 호출이 돌아온 조금 뒤에 바뀔 수 있다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void copy() {
        editCommand(PageKeyboard.COMMAND_COPY);
    }

    /**
     * Cuts the selection of an editable place (a text control of a form, <code>contenteditable</code>, design mode) to the clipboard
     * (extension method), as Ctrl+X does: the <code>cut</code> event is sent to the page first. Nothing happens where the page cannot be
     * edited. Threads as for {@link #copy()}.
     * <p>
     * 편집 가능한 곳(폼의 텍스트 컨트롤, <code>contenteditable</code>, 디자인 모드)의 선택을 Ctrl+X 와 같게 클립보드로 잘라낸다(확장 메서드): 먼저
     * <code>cut</code> 이벤트를 페이지에 보낸다. 편집할 수 없는 곳에서는 아무 일도 없다. 스레드는 {@link #copy()} 와 같다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void cut() {
        editCommand(PageKeyboard.COMMAND_CUT);
    }

    /**
     * Pastes the clipboard into the editable place that has the focus (extension method), as Ctrl+V does: the <code>paste</code> event is
     * sent to the page first, and unless a listener cancels it the data is inserted. Where the page cannot be edited only the event is
     * sent. The clipboard is read with {@link #readClipboard()}. Threads as for {@link #copy()}.
     * <p>
     * 포커스를 가진 편집 가능한 곳에 Ctrl+V 와 같게 클립보드를 붙여넣는다(확장 메서드): 먼저 <code>paste</code> 이벤트를 페이지에 보내고, 리스너가 취소하지 않으면
     * 데이터를 넣는다. 편집할 수 없는 곳에서는 이벤트만 간다. 클립보드는 {@link #readClipboard()} 로 읽는다. 스레드는 {@link #copy()} 와 같다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void paste() {
        editCommand(PageKeyboard.COMMAND_PASTE);
    }

    /**
     * Selects everything (extension method), as Ctrl+A does: all the text of the text control or of the editable place that has the
     * focus, else all the content of the page. Threads as for {@link #copy()}.
     * <p>
     * Ctrl+A 와 같게 모두 선택한다(확장 메서드): 포커스를 가진 텍스트 컨트롤이나 편집 가능한 곳의 글 전체, 그런 곳이 아니면 페이지의 모든 내용. 스레드는
     * {@link #copy()} 와 같다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void selectAll() {
        editCommand(PageKeyboard.COMMAND_SELECT_ALL);
    }

    /**
     * Runs an edit command on the event dispatch thread (now if the caller is on it, else queued).
     * <p>
     * 편집 명령을 이벤트 디스패치 스레드에서 실행한다(호출한 쪽이 그 스레드이면 바로, 아니면 큐에 넣는다).
     *
     * @param command one of the command constants of {@link PageKeyboard} / {@link PageKeyboard} 의 명령 상수 가운데 하나
     */
    private void editCommand(final String command) {
        checkAlive();
        if (SwingUtilities.isEventDispatchThread()) {
            runEditCommand(command);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                /** Runs the command on the EDT. / EDT 에서 명령을 실행한다. */
                public void run() {
                    if (!disposed) {
                        runEditCommand(command);
                    }
                }
            });
        }
    }

    /**
     * Runs an edit command (event dispatch thread): the editor of the focused text control handles it if there is one, else the page.
     * <p>
     * 편집 명령을 실행한다(이벤트 디스패치 스레드): 포커스를 가진 텍스트 컨트롤의 편집기가 있으면 그것이, 없으면 페이지가 처리한다.
     *
     * @param command one of the command constants of {@link PageKeyboard} / {@link PageKeyboard} 의 명령 상수 가운데 하나
     */
    private void runEditCommand(String command) {
        javax.swing.text.JTextComponent editor = controls.getActiveEditor();
        // 폼의 텍스트 컨트롤 편집기가 열려 있으면 그 편집기의 명령을 쓴다(클립보드 이벤트는 편집기가 페이지를 거쳐 보낸다).
        if (editor != null) {
            if (PageKeyboard.COMMAND_COPY.equals(command)) {
                editor.copy();
            } else if (PageKeyboard.COMMAND_CUT.equals(command)) {
                editor.cut();
            } else if (PageKeyboard.COMMAND_PASTE.equals(command)) {
                editor.paste();
            } else {
                editor.selectAll();
            }
            return;
        }
        keyboard.editCommand(command);
    }

    /**
     * Lays the page that is shown out for printing and hands the result, a {@link PagePrintout}, to a listener on the event dispatch
     * thread (extension method, API contract 5.30). The printout implements <code>java.awt.print.Pageable</code> and
     * <code>Printable</code>, so the host gives it to a <code>PrinterJob</code> or paints its pages into a preview. The component
     * itself never talks to a printer and shows no dialog.
     * <p>
     * The layout is made for the <code>print</code> medium (<code>@media print</code> applies, <code>@media screen</code> does not) at
     * the width of the page area: the paper that the host passes, with its imageable area as the default margins, changed by the
     * <code>@page</code> rules of the document (<code>size</code>, <code>margin</code>, with <code>:first</code>, <code>:left</code>
     * and <code>:right</code>). Pages are divided at forced breaks (<code>break-before</code>, <code>break-after</code> and the
     * <code>page-break-*</code> aliases) and otherwise so that lines, images and table rows are not cut and
     * <code>break-inside: avoid</code>, <code>orphans</code> and <code>widows</code> are respected where the page height allows it.
     * Before the layout the page hears <code>beforeprint</code> and after it <code>afterprint</code>; what a listener of the first
     * changes in the document is printed. What the component shows on the screen, its scroll position and its zoom are not changed,
     * and the zoom does not affect the printout.
     * <p>
     * 보이는 페이지를 인쇄용으로 배치하고 결과인 {@link PagePrintout} 을 이벤트 디스패치 스레드에서 리스너에 넘긴다(확장 메서드, API 계약
     * 5.30). 인쇄물은 <code>java.awt.print.Pageable</code> 과 <code>Printable</code> 을 구현하므로, 호스트는 그것을 <code>PrinterJob</code> 에
     * 넘기거나 그 쪽들을 미리 보기에 그린다. 컴포넌트 자신은 프린터와 통신하지 않고 대화 상자도 보이지 않는다.
     * <p>
     * 배치는 <code>print</code> 매체용이며(<code>@media print</code> 는 적용되고 <code>@media screen</code> 은 적용되지 않는다) 쪽 영역의 폭으로
     * 만든다: 호스트가 넘긴 용지와 그 인쇄 가능 영역(기본 여백)을 문서의 <code>@page</code> 규칙(<code>size</code>, <code>margin</code>,
     * <code>:first</code>·<code>:left</code>·<code>:right</code>)으로 바꾼 것이다. 쪽은 강제 나눔(<code>break-before</code>,
     * <code>break-after</code>, 별칭 <code>page-break-*</code>)에서 나누고, 그 밖에는 줄, 이미지, 표의 행을 가르지 않으며 쪽 높이가 허락하는 한
     * <code>break-inside: avoid</code>, <code>orphans</code>, <code>widows</code> 를 지킨다. 배치 전에 페이지가 <code>beforeprint</code> 를,
     * 뒤에 <code>afterprint</code> 를 듣는다. 앞의 것의 리스너가 문서에서 바꾼 것이 인쇄된다. 컴포넌트가 화면에 보이는 것, 스크롤 위치, 확대
     * 비율은 바뀌지 않으며, 확대 비율은 인쇄물에 영향을 주지 않는다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * PageFormat paper = PrinterJob.getPrinterJob().defaultPage();
     * panel.createPrintout(paper, false, new PrintoutListener() {
     *     public void printoutReady(PagePrintout printout) {
     *         // printout.getNumberOfPages(), printout.print(graphics, null, 0) ...
     *     }
     * });
     * </pre>
     *
     * @param paper            the paper to print on: its size, orientation and imageable area (it is copied) / 인쇄할 용지: 크기, 방향,
     *                         인쇄 가능 영역(복사한다)
     * @param printBackgrounds true to print background colors, background images and box shadows; false leaves them out except where
     *                         the page asks for them with <code>print-color-adjust: exact</code> / 배경색, 배경 이미지, 상자 그림자를
     *                         인쇄하려면 true. false 이면 페이지가 <code>print-color-adjust: exact</code> 로 요청한 곳 말고는 뺀다
     * @param listener         the receiver of the printout / 인쇄물을 받는 쪽
     * @throws IllegalArgumentException if the paper or the listener is null / 용지나 리스너가 null 이면
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void createPrintout(java.awt.print.PageFormat paper, final boolean printBackgrounds, final PrintoutListener listener) {
        checkAlive();
        if (paper == null || listener == null) {
            throw new IllegalArgumentException("paper and listener must not be null");
        }
        final String title = getTitle();
        final String url = getPage();
        view.print(paper, printBackgrounds, new PageView.PrintCallback() {
            /** Wraps the layout for the host. / 호스트를 위해 레이아웃을 감싼다.
             * @param layout the layout or null / 레이아웃 또는 null */
            public void printReady(org.duckdns.hjow.swingwebpanel.core.PrintLayout layout) {
                listener.printoutReady(layout == null ? null : new PagePrintout(layout, title, url, printBackgrounds));
            }
        });
    }

    /**
     * Receives the request of a script of the page to print it: <code>window.print()</code> (extension point, API contract 5.30). It is
     * called on the event dispatch thread shortly after the call of the script, which does not wait. The default does nothing, as
     * before printing was supported: the component has no print dialog of its own. A host that prints overrides it, typically to do
     * what its own print command does (ask for the printer, call
     * {@link #createPrintout(java.awt.print.PageFormat, boolean, PrintoutListener)}, print).
     * <p>
     * 페이지의 스크립트가 인쇄해 달라고 한 요청을 받는다: <code>window.print()</code>(확장 지점, API 계약 5.30). 스크립트의 호출 직후에 이벤트
     * 디스패치 스레드에서 불리며, 스크립트는 기다리지 않는다. 기본 구현은 인쇄를 지원하기 전과 같이 아무것도 하지 않는다: 컴포넌트에는 자체 인쇄
     * 대화 상자가 없다. 인쇄하는 호스트는 이것을 재정의하여 보통 자기 인쇄 명령이 하는 일(프린터를 묻고
     * {@link #createPrintout(java.awt.print.PageFormat, boolean, PrintoutListener)} 를 부르고 인쇄한다)을 한다.
     */
    protected void printRequested() {
        return;
    }

    /** The result of the search that was announced last; event dispatch thread only. / 마지막으로 알린 찾기 결과. 이벤트 디스패치 스레드에서만 쓴다. */
    private transient FindResult announcedFind = FindResult.NONE;

    /**
     * Searches the text of the page that is shown (extension method, API contract 5.29). A call with a new text starts a search: every
     * place of the page where the text is shown is highlighted, the first one that is not above the visible area becomes the current
     * match, drawn in another color, and the view scrolls to it. A call with the text of the active search goes to the next match
     * (<code>forward</code>) or the previous one, going around at the ends. While the user types, each call with the longer text keeps
     * the current match where it started if it still matches. The result (count and number of the current match) is announced as the
     * {@link #PROPERTY_FIND_RESULT} property on the event dispatch thread and returned by {@link #getFindResult()}; the call itself
     * returns at once and may come from any thread.
     * <p>
     * The search sees the text as it is displayed: white space is collapsed, a phrase is found across a line break and across inline
     * elements, but not across paragraphs or table cells; hidden text is not found. Text in form controls and in frames is not
     * searched. While a search is active it follows the page: after a change of the page the highlights and the count are updated.
     * Showing another page ends the search. An empty text is the same as {@link #clearFind()}.
     * <p>
     * 보이는 페이지의 글을 찾는다(확장 메서드, API 계약 5.29). 새 글로 부르면 찾기를 시작한다: 그 글이 보이는 페이지의 모든 곳을 강조하고, 보이는
     * 영역보다 위에 있지 않은 첫 일치가 현재 일치가 되어 다른 색으로 그려지며, 뷰가 그곳으로 스크롤한다. 활성 찾기의 글로 부르면 다음
     * 일치(<code>forward</code>)나 이전 일치로 가고 끝에서는 반대쪽 끝으로 돈다. 사용자가 입력하는 동안 더 긴 글로 부를 때마다, 현재 일치가 여전히
     * 일치하면 그 자리를 유지한다. 결과(개수와 현재 일치의 번호)는 {@link #PROPERTY_FIND_RESULT} 속성으로 이벤트 디스패치 스레드에서 알리고
     * {@link #getFindResult()} 가 반환한다. 호출 자체는 곧바로 돌아오며 어느 스레드에서나 부를 수 있다.
     * <p>
     * 찾기는 표시되는 그대로의 글을 본다: 공백은 접혀 있고, 줄 바꿈과 인라인 요소에 걸친 구절은 찾지만 문단이나 표 셀에 걸친 것은 찾지 않으며,
     * 숨겨진 글은 찾지 않는다. 폼 컨트롤과 프레임 안의 글은 찾지 않는다. 찾기가 활성인 동안에는 페이지를 따라간다: 페이지가 바뀌면 강조와 개수를
     * 갱신한다. 다른 페이지를 보이면 찾기는 끝난다. 빈 글은 {@link #clearFind()} 와 같다.
     * <p>
     * 사용 예 / Example:
     * <pre>
     * panel.find("license", false, true);   // start, or go to the next match
     * panel.find("license", false, false);  // go to the previous match
     * panel.clearFind();                    // remove the highlights
     * </pre>
     *
     * @param text      the text to search for / 찾을 글
     * @param matchCase true to tell upper and lower case apart / 대소문자를 구분하려면 true
     * @param forward   true to go forward, false to go backward / 앞으로 가려면 true, 뒤로 가려면 false
     * @throws IllegalArgumentException if the text is null / 글이 null 이면
     * @throws IllegalStateException    if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void find(String text, boolean matchCase, boolean forward) {
        checkAlive();
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        finder.find(text, matchCase, forward);
    }

    /**
     * Ends the search in the page (extension method, API contract 5.29): the highlights are removed and the result becomes
     * {@link FindResult#NONE}. It does nothing when no search is active. It may be called from any thread.
     * <p>
     * 페이지 안 찾기를 끝낸다(확장 메서드, API 계약 5.29): 강조를 지우고 결과는 {@link FindResult#NONE} 이 된다. 활성 찾기가 없으면 아무 일도
     * 없다. 어느 스레드에서나 부를 수 있다.
     *
     * @throws IllegalStateException if the component was disposed / 컴포넌트가 폐기되었을 때
     */
    public void clearFind() {
        checkAlive();
        finder.clear();
    }

    /**
     * Returns the last result of the search in the page (extension method, API contract 5.29).
     * <p>
     * 페이지 안 찾기의 마지막 결과를 반환한다(확장 메서드, API 계약 5.29).
     *
     * @return the result; {@link FindResult#NONE} while nothing is searched for and after {@link #dispose()} / 결과. 아무것도 찾지 않는
     *         동안과 {@link #dispose()} 뒤에는 {@link FindResult#NONE}
     */
    public FindResult getFindResult() {
        return finder.getResult();
    }

    /**
     * Puts the copied text on the clipboard. The default writes <code>text/plain</code> (and <code>text/html</code> if there is markup) to the
     * system clipboard; a host that keeps its own clipboard, or a test, overrides it. It is called on the event dispatch thread after the
     * user copied the selection (Ctrl+C or Ctrl+Insert) and no listener of the <code>copy</code> event cancelled it. An exception (no screen,
     * the clipboard is not available) is not shown to the user except as the status text.
     * <p>
     * 복사한 텍스트를 클립보드에 넣는다. 기본은 <code>text/plain</code>(마크업이 있으면 <code>text/html</code> 도)을 시스템 클립보드에 쓴다. 자체 클립보드를 가진
     * 호스트나 시험은 이것을 재정의한다. 사용자가 선택을 복사하고(Ctrl+C 또는 Ctrl+Insert) <code>copy</code> 이벤트의 어떤 리스너도 취소하지 않았을 때 이벤트
     * 디스패치 스레드에서 호출된다. 예외(화면 없음, 클립보드를 쓸 수 없음)는 상태 메시지로만 알린다.
     *
     * @param plainText    the text / 텍스트
     * @param htmlFragment the markup of the selection, or null / 선택의 마크업. 없으면 null
     * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면
     */
    protected void copyToClipboard(String plainText, String htmlFragment) throws Exception {
        java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new CopiedText(plainText, htmlFragment), null);
    }

    /**
     * Reads the clipboard for a paste into an editable place of the page (<code>contenteditable</code> or design mode). The default reads
     * the system clipboard; a host that keeps its own clipboard, or a test, overrides it. It is called on the event dispatch thread when the
     * user pastes (Ctrl+V or Shift+Insert); the <code>text/plain</code> and <code>text/html</code> data it offers become the
     * <code>clipboardData</code> of the <code>paste</code> event. An exception (no screen, the clipboard is not available) is not shown to
     * the user except as the status text, and nothing is pasted.
     * <p>
     * 페이지의 편집 가능한 곳(<code>contenteditable</code> 이나 디자인 모드)에 붙여넣기 위해 클립보드를 읽는다. 기본은 시스템 클립보드를 읽는다. 자체 클립보드를
     * 가진 호스트나 시험은 이것을 재정의한다. 사용자가 붙여넣을 때(Ctrl+V 또는 Shift+Insert) 이벤트 디스패치 스레드에서 호출되며, 이것이 주는
     * <code>text/plain</code> 과 <code>text/html</code> 데이터가 <code>paste</code> 이벤트의 <code>clipboardData</code> 가 된다. 예외(화면 없음, 클립보드를
     * 쓸 수 없음)는 상태 메시지로만 알리고 아무것도 붙여넣지 않는다.
     *
     * @return the contents of the clipboard, or null if it is empty / 클립보드의 내용. 비었으면 null
     * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면
     */
    protected java.awt.datatransfer.Transferable readClipboard() throws Exception {
        return java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    }

    /**
     * Returns what an input method asks the component while the user composes text in an editing host of the page (Korean, Japanese and
     * other languages): where the caret is, so that the window of the candidates opens beside it. The composed text itself arrives as
     * <code>InputMethodEvent</code> and is shown in the page as it is composed. Form controls have editors of their own and answer for
     * themselves.
     * <p>
     * 사용자가 페이지의 편집 호스트에서 텍스트를 조합할 때(한국어, 일본어 등) 입력기가 컴포넌트에 묻는 것에 답하는 객체를 반환한다: 캐럿이 어디 있는지(후보 창이
     * 그 옆에 열린다). 조합하는 텍스트 자체는 <code>InputMethodEvent</code> 로 도착하며 조합되는 대로 페이지에 보인다. 폼 컨트롤은 자기 편집기가 있고 스스로 답한다.
     *
     * @return the requests object / 질의 객체
     */
    public java.awt.im.InputMethodRequests getInputMethodRequests() {
        return composition == null ? null : composition.getRequests();
    }

    /**
     * The data of a copy: the text as a string, and the markup as <code>text/html</code> if there is any.
     * <p>
     * 복사의 데이터: 문자열인 텍스트와, 있으면 <code>text/html</code> 인 마크업.
     */
    private static final class CopiedText implements java.awt.datatransfer.Transferable {
        /** The html flavor (the string representation of <code>text/html</code>). html 형식(<code>text/html</code> 의 문자열 표현). */
        private static final java.awt.datatransfer.DataFlavor HTML = htmlFlavor();
        /** The text. 텍스트. */
        private final String text;
        /** The markup, or null. 마크업. 없으면 null. */
        private final String html;

        /**
         * Creates the data.
         * <p>
         * 데이터를 만든다.
         *
         * @param text the text / 텍스트
         * @param html the markup or null / 마크업. 없으면 null
         */
        CopiedText(String text, String html) {
            this.text = text;
            this.html = html;
        }

        /**
         * Returns the flavor of <code>text/html</code> as a string, or null if the JRE cannot make it.
         * <p>
         * 문자열인 <code>text/html</code> 형식을 반환한다. JRE 가 만들 수 없으면 null.
         *
         * @return the flavor or null / 형식. 없으면 null
         */
        private static java.awt.datatransfer.DataFlavor htmlFlavor() {
            try {
                return new java.awt.datatransfer.DataFlavor("text/html;class=java.lang.String");
            } catch (ClassNotFoundException e) {
                return null;
            } catch (RuntimeException e) {
                return null;
            }
        }

        /**
         * Returns the flavors that this data has.
         * <p>
         * 이 데이터가 가진 형식을 반환한다.
         *
         * @return the flavors / 형식
         */
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            if (html != null && HTML != null) {
                return new java.awt.datatransfer.DataFlavor[] { java.awt.datatransfer.DataFlavor.stringFlavor, HTML };
            }
            return new java.awt.datatransfer.DataFlavor[] { java.awt.datatransfer.DataFlavor.stringFlavor };
        }

        /**
         * Tells whether a flavor is supported.
         * <p>
         * 형식을 지원하는지 알려 준다.
         *
         * @param flavor the flavor / 형식
         * @return true if supported / 지원하면 true
         */
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            return java.awt.datatransfer.DataFlavor.stringFlavor.equals(flavor) || (html != null && HTML != null && HTML.equals(flavor));
        }

        /**
         * Returns the data in a flavor.
         * <p>
         * 형식에 맞는 데이터를 반환한다.
         *
         * @param flavor the flavor / 형식
         * @return the string / 문자열
         * @throws java.awt.datatransfer.UnsupportedFlavorException for another flavor / 다른 형식이면
         */
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException {
            if (java.awt.datatransfer.DataFlavor.stringFlavor.equals(flavor)) {
                return text;
            }
            if (html != null && HTML != null && HTML.equals(flavor)) {
                return html;
            }
            throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
        }
    }

    /**
     * Extension point for SSL (TLS) setup. It is called for every HTTPS connection before it connects. The default
     * implementation does nothing, so the JRE's default trust store and host name verification apply. Override it to
     * install a custom <code>SSLSocketFactory</code> or <code>HostnameVerifier</code> for this connection only; never change
     * JVM-wide settings here. If connecting over HTTPS fails, the browser tries plain HTTP once for the same address.
     * <p>
     * SSL(TLS) 설정을 위한 확장 지점. 모든 HTTPS 연결이 연결하기 전에 호출한다. 기본 구현은 아무것도 하지 않으므로 JRE 의 기본 신뢰 저장소와
     * 호스트 이름 검증이 적용된다. 이 연결에만 쓸 <code>SSLSocketFactory</code> 나 <code>HostnameVerifier</code> 를 지정하려면
     * 재정의한다. 여기서 JVM 전역 설정을 바꾸지 않는다. HTTPS 연결이 실패하면 브라우저는 같은 주소로 평문 HTTP 를 한 번 시도한다.
     *
     * @param connection the HTTPS connection about to be opened / 곧 여는 HTTPS 연결
     * @throws IOException to abort the connection / 연결을 중단하려면 던진다
     */
    protected void configureSsl(HttpsURLConnection connection) throws IOException {
        // 페이지가 연 창은 자신을 연 컴포넌트의 SSL 설정을 따른다(호스트가 좁혀 둔 설정을 팝업이 벗어나지 않게 한다).
        if (creator != null) {
            creator.configureSsl(connection);
            return;
        }
        // 기본 동작: JRE 기본 신뢰 저장소와 호스트 이름 검증을 쓰고, 연결별로 이 JRE 가 지원하는 프로토콜만 고른다(전역 설정은 바꾸지 않는다).
        DefaultSslConfigurer.INSTANCE.configureSsl(connection);
    }

    /**
     * Called on the event dispatch thread when the component's width changed. The default updates the viewport extent and
     * schedules a coalesced layout on the document thread.
     * <p>
     * 컴포넌트 폭이 바뀌면 이벤트 디스패치 스레드에서 호출되어 레이아웃 쪽이 페이지를 다시 배치할 수 있게 한다.
     * 기본 구현은 뷰포트 범위를 갱신하고 문서 스레드에 재배치를 합쳐 예약한다.
     *
     * @param width new width in pixels / 새 폭(픽셀)
     */
    protected void viewportWidthChanged(int width) {
        updateViewport();
    }

    /**
     * Moves the viewport that holds this component so that a point of the document is at its top left, clamped to the content. A coordinate
     * that is <code>NaN</code> keeps the current position of that axis. Nothing happens if the component is not inside a viewport. Call it
     * on the EDT.
     * <p>
     * 이 컴포넌트를 담은 뷰포트를 문서의 한 점이 왼쪽 위에 오도록 내용 범위 안에서 움직인다. <code>NaN</code> 인 좌표는 그 축의 현재 위치를 유지한다.
     * 컴포넌트가 뷰포트 안에 없으면 아무 일도 없다. EDT 에서 부른다.
     *
     * @param x the x in document pixels, or NaN / 문서 픽셀의 x. 또는 NaN
     * @param y the y in document pixels, or NaN / 문서 픽셀의 y. 또는 NaN
     */
    private void scrollViewport(double x, double y) {
        // 문서 픽셀(CSS 픽셀)을 컴포넌트 픽셀로 바꾼다(값이 없는 축은 그대로 NaN 으로 둔다).
        double z = zoom;
        scrollViewportAt(Double.isNaN(x) ? x : x * z, Double.isNaN(y) ? y : y * z);
    }

    /**
     * Moves the viewport to a position in component pixels, clamped to the content (see {@link #scrollViewport(double, double)}).
     * <p>
     * 뷰포트를 컴포넌트 픽셀 위치로 옮긴다. 내용 범위 안으로 제한한다({@link #scrollViewport(double, double)} 참조).
     *
     * @param x the x in component pixels / 컴포넌트 픽셀의 x
     * @param y the y in component pixels / 컴포넌트 픽셀의 y
     */
    private void scrollViewportTo(int x, int y) {
        scrollViewportAt(x, y);
    }

    /**
     * Moves the viewport to a position in component pixels; a coordinate that is NaN keeps that axis where it is.
     * <p>
     * 뷰포트를 컴포넌트 픽셀 위치로 옮긴다. NaN 인 좌표는 그 축을 그대로 둔다.
     *
     * @param x the x in component pixels, or NaN / 컴포넌트 픽셀의 x. 또는 NaN
     * @param y the y in component pixels, or NaN / 컴포넌트 픽셀의 y. 또는 NaN
     */
    private void scrollViewportAt(double x, double y) {
        java.awt.Container parent = javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, this);
        if (!(parent instanceof javax.swing.JViewport)) {
            return;
        }
        javax.swing.JViewport viewport = (javax.swing.JViewport) parent;
        int maxY = Math.max(0, Math.max(getHeight(), getPreferredSize().height) - viewport.getExtentSize().height);
        int maxX = Math.max(0, Math.max(getWidth(), getPreferredSize().width) - viewport.getExtentSize().width);
        java.awt.Point now = viewport.getViewPosition();
        // 내용 범위를 벗어나지 않게 제한하고, 값이 없는 축은 현재 위치를 쓴다.
        int nx = Double.isNaN(x) ? now.x : (int) Math.max(0, Math.min(maxX, Math.round(x)));
        int ny = Double.isNaN(y) ? now.y : (int) Math.max(0, Math.min(maxY, Math.round(y)));
        viewport.setViewPosition(new java.awt.Point(Math.min(maxX, nx), ny));
    }

    /** Uses the viewport extent so horizontal overflow does not hide later viewport changes. / 가로 넘침이 이후 크기 변경을 가리지 않도록 뷰포트 범위를 쓴다. */
    private void updateViewport() {
        if (view == null || disposed) {
            return;
        }
        JViewport viewport = getParent() instanceof JViewport ? (JViewport) getParent() : null;
        if (observedViewport != viewport) {
            detachViewport();
            observedViewport = viewport;
            if (viewport != null) {
                viewport.addChangeListener(viewportListener);
            }
        }
        Dimension extent = viewport == null ? getSize() : viewport.getExtentSize();
        Point position = viewport == null ? new Point() : viewport.getViewPosition();
        view.setViewportPosition(position.x, position.y);
        view.setViewport(extent.width, extent.height);
        // 이전 보고와 비교해 크기나 스크롤 위치가 바뀐 경우에만 페이지에 resize/scroll 을 알린다(첫 호출은 기준값만 기록한다).
        boolean first = reportedWidth < 0;
        boolean resized = !first && (extent.width != reportedWidth || extent.height != reportedHeight);
        boolean scrolled = !first && (position.x != reportedX || position.y != reportedY);
        reportedWidth = extent.width;
        reportedHeight = extent.height;
        reportedX = position.x;
        reportedY = position.y;
        if (resized || scrolled) {
            session.viewportChanged(resized, scrolled);
        }
        // fixed/sticky 상자는 스크롤 위치에 따라 그려지는 자리가 달라지므로, JViewport 가 픽셀을 복사해 스크롤했어도 뷰포트 전체를 다시 그린다.
        if (scrolled) {
            org.duckdns.hjow.swingwebpanel.render.RenderedPage shown = view.getRenderedPage();
            if (shown != null && shown.getDisplayList().isScrollDependent()) {
                repaint();
            }
        }
    }

    /** Releases only this component's external viewport listener on the EDT. / EDT에서 이 컴포넌트의 외부 뷰포트 리스너만 해제한다. */
    private void detachViewport() {
        if (observedViewport != null) {
            observedViewport.removeChangeListener(viewportListener);
            observedViewport = null;
        }
    }

    /**
     * Paints the Swing background and the immutable commands produced by the document thread.
     * <p>
     * Swing 배경과 문서 스레드가 만든 불변 그림 명령을 그린다.
     *
     * @param g graphics context / 그래픽 컨텍스트
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D copy = (Graphics2D) g.create();
        try {
            paintPage(copy);
        } finally {
            copy.dispose();
        }
    }

    /**
     * Hook for the rendering engine: paints the latest layout of the confirmed page.
     * <p>
     * 렌더링 엔진을 위한 확장 지점: 확정된 페이지의 최신 배치를 그린다.
     *
     * @param g2 graphics context, disposed by the caller / 그래픽 컨텍스트(호출자가 해제)
     */
    protected void paintPage(Graphics2D g2) {
        if (view != null) {
            view.paint(g2);
        }
    }

    /**
     * Returns the size information object that the layout engine updates (for the rendering code of this library).
     * <p>
     * 레이아웃 엔진이 갱신하는 크기 정보 객체를 반환한다(이 라이브러리의 렌더링 코드용).
     *
     * @return metrics / 크기 정보
     */
    protected final ViewMetrics getViewMetrics() {
        return metrics;
    }

    /**
     * Returns the preferred size: the laid-out content size, or 640x480 for an empty page.
     * <p>
     * 선호 크기를 반환한다: 배치된 콘텐츠 크기이며 빈 페이지는 640x480 이다.
     *
     * @return preferred size / 선호 크기
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        double z = zoom;
        // 레이아웃이 알려 준 CSS 픽셀 크기에 확대 배율을 곱해 컴포넌트 픽셀 크기로 바꾼다.
        return new Dimension(Math.max((int) Math.ceil(metrics.getMinWidth() * z), DEFAULT_WIDTH),
                Math.max((int) Math.ceil(metrics.getHeight() * z), DEFAULT_HEIGHT));
    }

    /**
     * Returns the preferred size of the viewport when this component is the view of a scroll pane.
     * <p>
     * 이 컴포넌트가 스크롤 창의 뷰일 때 뷰포트의 선호 크기를 반환한다.
     *
     * @return viewport size / 뷰포트 크기
     */
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Returns the distance of one scroll step: the line height reported by the layout engine.
     * <p>
     * 스크롤 한 단위의 거리를 반환한다: 레이아웃 엔진이 알려 준 줄 높이.
     *
     * @param visibleRect visible part / 보이는 영역
     * @param orientation SwingConstants.VERTICAL or HORIZONTAL / SwingConstants.VERTICAL 또는 HORIZONTAL
     * @param direction   negative up/left, positive down/right / 음수는 위/왼쪽, 양수는 아래/오른쪽
     * @return distance in pixels / 거리(픽셀)
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return unitStep();
    }

    /**
     * Returns one scroll step in component pixels: the line height of the layout (CSS pixels) times the zoom factor.
     * <p>
     * 컴포넌트 픽셀 단위의 스크롤 한 단위를 반환한다: 레이아웃의 줄 높이(CSS 픽셀)에 확대 배율을 곱한 값.
     *
     * @return the step, at least 1 / 단위. 1 이상
     */
    private int unitStep() {
        return Math.max(1, (int) Math.round(metrics.getUnitIncrement() * zoom));
    }

    /**
     * Returns the distance of one block (page) scroll: the visible size minus one step.
     * <p>
     * 블록(페이지) 스크롤 한 번의 거리를 반환한다: 보이는 크기에서 한 단위를 뺀 값.
     *
     * @param visibleRect visible part / 보이는 영역
     * @param orientation SwingConstants.VERTICAL or HORIZONTAL / SwingConstants.VERTICAL 또는 HORIZONTAL
     * @param direction   negative up/left, positive down/right / 음수는 위/왼쪽, 양수는 아래/오른쪽
     * @return distance in pixels / 거리(픽셀)
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        int visible = orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        return Math.max(unitStep(), visible - unitStep());
    }

    /**
     * Tells whether the view width follows the viewport width: true while the laid-out content fits into the viewport.
     * <p>
     * 뷰 폭이 뷰포트 폭을 따라가는지 알려 준다: 배치된 콘텐츠가 뷰포트에 들어가는 동안 true.
     *
     * @return true to track the viewport width / 뷰포트 폭을 따라가려면 true
     */
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getWidth() >= Math.ceil(metrics.getMinWidth() * zoom);
        }
        return false;
    }

    /**
     * Tells whether the view height follows the viewport height: true while the content is shorter than the viewport.
     * <p>
     * 뷰 높이가 뷰포트 높이를 따라가는지 알려 준다: 콘텐츠가 뷰포트보다 낮은 동안 true.
     *
     * @return true to track the viewport height / 뷰포트 높이를 따라가려면 true
     */
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getHeight() > Math.ceil(metrics.getHeight() * zoom);
        }
        return false;
    }

    /**
     * Removes the listeners that were registered on this component: our own internal ones and every listener added by the host.
     * Listeners that belong to Swing itself (the look and feel's connections) are left alone.
     * <p>
     * 이 컴포넌트에 등록된 리스너를 제거한다: 우리 내부 리스너와 호스트가 추가한 모든 리스너. Swing 자체의 연결(look and feel)은 건드리지 않는다.
     */
    private void removeAttachedListeners() {
        List<Object> all = new ArrayList<Object>();
        all.addAll(Arrays.asList((Object[]) getComponentListeners()));
        all.addAll(Arrays.asList((Object[]) getFocusListeners()));
        all.addAll(Arrays.asList((Object[]) getHierarchyListeners()));
        all.addAll(Arrays.asList((Object[]) getHierarchyBoundsListeners()));
        all.addAll(Arrays.asList((Object[]) getKeyListeners()));
        all.addAll(Arrays.asList((Object[]) getMouseListeners()));
        all.addAll(Arrays.asList((Object[]) getMouseMotionListeners()));
        all.addAll(Arrays.asList((Object[]) getMouseWheelListeners()));
        all.addAll(Arrays.asList((Object[]) getInputMethodListeners()));
        all.addAll(Arrays.asList((Object[]) getPropertyChangeListeners()));
        all.addAll(Arrays.asList((Object[]) getVetoableChangeListeners()));
        all.addAll(Arrays.asList((Object[]) getAncestorListeners()));
        // 등록된 리스너를 모두 훑으며, 구현한 종류마다 제거 대상이면 제거한다(등록하지 않은 종류의 제거는 영향이 없다).
        for (Object l : all) {
            if (!shouldRemove(l)) {
                continue;
            }
            if (l instanceof ComponentListener) {
                removeComponentListener((ComponentListener) l);
            }
            if (l instanceof FocusListener) {
                removeFocusListener((FocusListener) l);
            }
            if (l instanceof HierarchyListener) {
                removeHierarchyListener((HierarchyListener) l);
            }
            if (l instanceof HierarchyBoundsListener) {
                removeHierarchyBoundsListener((HierarchyBoundsListener) l);
            }
            if (l instanceof KeyListener) {
                removeKeyListener((KeyListener) l);
            }
            if (l instanceof MouseListener) {
                removeMouseListener((MouseListener) l);
            }
            if (l instanceof MouseMotionListener) {
                removeMouseMotionListener((MouseMotionListener) l);
            }
            if (l instanceof MouseWheelListener) {
                removeMouseWheelListener((MouseWheelListener) l);
            }
            if (l instanceof InputMethodListener) {
                removeInputMethodListener((InputMethodListener) l);
            }
            if (l instanceof PropertyChangeListener) {
                removePropertyChangeListener((PropertyChangeListener) l);
            }
            if (l instanceof VetoableChangeListener) {
                removeVetoableChangeListener((VetoableChangeListener) l);
            }
            if (l instanceof AncestorListener) {
                removeAncestorListener((AncestorListener) l);
            }
        }
    }

    /**
     * Decides whether a registered listener must be removed at dispose: our own internal listeners and host listeners go,
     * listeners owned by Swing/AWT classes stay.
     * <p>
     * 등록된 리스너를 dispose 때 제거해야 하는지 판단한다: 우리 내부 리스너와 호스트 리스너는 제거하고 Swing/AWT 클래스가 가진 리스너는 둔다.
     *
     * @param listener registered listener / 등록된 리스너
     * @return true if it must be removed / 제거해야 하면 true
     */
    private boolean shouldRemove(Object listener) {
        Object l = listener;
        if (l instanceof PropertyChangeListenerProxy) {
            l = ((PropertyChangeListenerProxy) l).getListener();
        }
        if (internalListeners.contains(l)) {
            return true;
        }
        String name = l.getClass().getName();
        boolean owned = name.startsWith("javax.swing.") || name.startsWith("java.awt.") || name.startsWith("sun.")
                || name.startsWith("com.sun.");
        return !owned;
    }
}
