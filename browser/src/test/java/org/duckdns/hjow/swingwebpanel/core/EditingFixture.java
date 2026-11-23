package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * What the integration tests of editing and drag and drop by the user share (plan item P20-06): a component that reads the clipboard from a
 * variable and records what is copied, the test page with an editing host that reports its events, its HTML and the selection to the
 * <code>sessionStorage</code> of the page, and the helpers that send real AWT key, input method and mouse events to the component (key
 * events go to the registered key listeners directly, because AWT sends a key event that is dispatched to a component that has no native
 * focus to the focus owner), wait for the asynchronous results of the document queue and read the painted pixels.
 * <p>
 * 사용자의 편집과 끌어다 놓기의 통합 시험이 공유하는 것(계획 P20-06): 클립보드를 변수에서 읽고 복사한 것을 기록하는 컴포넌트, 편집 호스트를 가졌고 자기 이벤트,
 * HTML, 선택을 페이지의 <code>sessionStorage</code> 로 알리는 시험 페이지, 컴포넌트에 실제 AWT 키, 입력기, 마우스 이벤트를 보내는 도우미(키 이벤트는 등록된
 * 키 리스너를 직접 부른다. 네이티브 포커스가 없는 컴포넌트로 전달한 키 이벤트는 AWT 가 포커스 소유자에게 돌려보내기 때문이다), 문서 큐의 비동기 결과를 기다리는
 * 것, 그려진 픽셀을 읽는 것.
 */
final class EditingFixture {

    /**
     * The longest time that a wait lasts in milliseconds (a cold JVM needs a long time for the first page).
     * <p>
     * 기다림의 최대 시간(밀리초). 차가운 JVM 은 첫 페이지에 오래 걸린다.
     */
    static final long WAIT_MS = 40000;

    /**
     * The template of the test page. The script reports to the sessionStorage: <code>rects</code> (the rectangle of the elements
     * <code>ed</code>, <code>ro</code> and <code>box</code>, then <code>ready</code>), <code>html</code> (the HTML of the host after every
     * event), <code>log</code> (the events that reached the host, as <code>type:key</code>, <code>type:inputType:data</code> or
     * <code>type:data</code>, separated by spaces), <code>sel</code> (<code>anchor,focus</code> as offsets in the text of the host) and
     * <code>markers</code> (the number of <code>keyup</code> events of F12 that the document saw: they tell that the document queue
     * handled everything before them). <code>%EXTRA%</code> is replaced by more script of a case, <code>%BODY%</code> by the body.
     * <p>
     * 시험 페이지의 틀. 스크립트는 sessionStorage 로 알린다: <code>rects</code>(<code>ed</code>, <code>ro</code>, <code>box</code> 요소의 사각형), <code>html</code>(이벤트마다
     * 호스트의 HTML), <code>log</code>(호스트에 닿은 이벤트: <code>type:key</code>, <code>type:inputType:data</code>, <code>type:data</code>. 공백으로 구분),
     * <code>sel</code>(호스트 텍스트의 오프셋 <code>anchor,focus</code>), <code>markers</code>(문서가 본 F12 의 <code>keyup</code> 수. 이것 앞의 모든 것을 문서 큐가 처리했다는
     * 뜻이다). <code>%EXTRA%</code> 는 사례의 추가 스크립트로, <code>%BODY%</code> 는 본문으로 바뀐다.
     */
    private static final String TEMPLATE = "<!doctype html><style>html,body{margin:0;padding:0} body{font:16px monospace;line-height:24px}"
            + " #ed{width:320px;margin:0;padding:0} #ro{margin-top:20px} #box{width:60px;height:30px;background:#123456} p{margin:0}</style>"
            + "%BODY%"
            + "<script>setTimeout(function () {"
            + " var ed = document.getElementById('ed'); var log = []; var markers = 0;"
            + " function put(k, v) { sessionStorage.setItem(k, v); }"
            + " function pos(n, o) { var r = document.createRange(); r.setStart(ed, 0); r.setEnd(n, o); return r.toString().length; }"
            + " function sel() { var s = window.getSelection(); if (!s.anchorNode || !ed.contains(s.anchorNode) || !ed.contains(s.focusNode)) { return 'none'; }"
            + "   return pos(s.anchorNode, s.anchorOffset) + ',' + pos(s.focusNode, s.focusOffset); }"
            + " function note(e) {"
            + "   var d = e.type;"
            + "   if (e.type.indexOf('key') === 0) { d += ':' + e.key; }"
            + "   else if (e.type === 'beforeinput' || e.type === 'input') { d += ':' + e.inputType + ':' + e.data + (e.isComposing ? ':c' : ''); }"
            + "   else if (e.type.indexOf('composition') === 0) { d += ':' + e.data; }"
            + "   else if (e.type === 'paste' || e.type === 'cut' || e.type === 'copy') { d += ':' + (e.clipboardData ? e.clipboardData.getData('text/plain') : ''); }"
            + "   log.push(d); put('log', log.join(' ')); put('html', ed.innerHTML); put('text', ed.textContent.replace(/\\u00a0/g, ' ')); put('sel', sel()); }"
            + " var types = ['keydown', 'keypress', 'keyup', 'beforeinput', 'input', 'compositionstart', 'compositionupdate', 'compositionend', 'cut', 'copy', 'paste'];"
            + " for (var i = 0; i < types.length; i++) { ed.addEventListener(types[i], note); }"
            + " document.addEventListener('selectionchange', function () { put('sel', sel()); });"
            + " document.addEventListener('keyup', function (e) { if (e.key === 'F12') { markers++; var ro = document.getElementById('ro'); if (ro) { put('ro', ro.innerHTML); } put('markers', String(markers)); } });"
            + " function r(id) { var e = document.getElementById(id); if (!e) { return '0,0,0,0'; } var b = e.getBoundingClientRect(); return [b.left, b.top, b.width, b.height].join(','); }"
            + " put('html', ed.innerHTML); put('sel', sel());"
            + " %EXTRA%"
            + " var tr = document.createRange(); tr.selectNodeContents(ed); put('textw', String(tr.getBoundingClientRect().width));"
            + " var all = document.querySelectorAll('[id]'); for (var k = 0; k < all.length; k++) { put('rect:' + all[k].id, r(all[k].id)); }"
            + " put('rects', r('ed') + ';' + r('ro') + ';' + r('box'));"
            + " }, 0);</script>";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private EditingFixture() {
    }

    /**
     * Returns the test page with an editing host with the text <code>hello world</code> followed by a locked block and a box.
     * <p>
     * 텍스트 <code>hello world</code> 를 가진 편집 호스트와 편집할 수 없는 블록, 상자가 있는 시험 페이지를 반환한다.
     *
     * @param extra more script that runs once the page is shown, or an empty text / 페이지가 보인 뒤 한 번 실행되는 추가 스크립트. 없으면 빈 문자열
     * @return the page / 페이지
     */
    static String page(String extra) {
        return pageWith("<div id=ed contenteditable=true>hello world</div><div id=ro>read only</div><div id=box></div>", extra);
    }

    /**
     * Returns the test page with a given body.
     * <p>
     * 본문을 지정한 시험 페이지를 반환한다.
     *
     * @param body  the body markup / 본문 마크업
     * @param extra more script that runs once the page is shown, or an empty text / 페이지가 보인 뒤 한 번 실행되는 추가 스크립트. 없으면 빈 문자열
     * @return the page / 페이지
     */
    static String pageWith(String body, String extra) {
        return TEMPLATE.replace("%BODY%", body).replace("%EXTRA%", extra);
    }

    /**
     * The data that a paste reads from the clipboard: a text and optional markup.
     * <p>
     * 붙여넣기가 클립보드에서 읽는 데이터: 텍스트와 선택적인 마크업.
     */
    static final class Clip implements Transferable {
        /** The text. 텍스트. */
        final String text;
        /** The markup or null. 마크업. 없으면 null. */
        final String html;
        /** The flavor of the markup. 마크업의 형식. */
        private final DataFlavor htmlFlavor;

        /**
         * Creates the data.
         * <p>
         * 데이터를 만든다.
         *
         * @param text the text / 텍스트
         * @param html the markup or null / 마크업. 없으면 null
         */
        Clip(String text, String html) {
            this.text = text;
            this.html = html;
            DataFlavor f;
            try {
                f = new DataFlavor("text/html;class=java.lang.String");
            } catch (ClassNotFoundException e) {
                f = DataFlavor.stringFlavor;
            }
            htmlFlavor = f;
        }

        /**
         * Returns the flavors.
         * <p>
         * 형식을 반환한다.
         *
         * @return the flavors / 형식
         */
        public DataFlavor[] getTransferDataFlavors() {
            return html == null ? new DataFlavor[] { DataFlavor.stringFlavor } : new DataFlavor[] { DataFlavor.stringFlavor, htmlFlavor };
        }

        /**
         * Tells whether a flavor is offered.
         * <p>
         * 형식을 제공하는지 알려 준다.
         *
         * @param flavor the flavor / 형식
         * @return true if offered / 제공하면 true
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor) || (html != null && htmlFlavor.equals(flavor));
        }

        /**
         * Returns the data of a flavor.
         * <p>
         * 형식의 데이터를 반환한다.
         *
         * @param flavor the flavor / 형식
         * @return the data / 데이터
         * @throws UnsupportedFlavorException if not offered / 제공하지 않으면
         * @throws IOException never / 발생하지 않는다
         */
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (DataFlavor.stringFlavor.equals(flavor)) {
                return text;
            }
            if (html != null && htmlFlavor.equals(flavor)) {
                return html;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * A component that serves the pages of the cases over HTTP, reads the clipboard from a variable and records what is copied.
     * <p>
     * 사례의 페이지를 HTTP 로 주고, 클립보드를 변수에서 읽고, 복사한 것을 기록하는 컴포넌트.
     */
    static class Panel extends SwingWebPanel {
        /** Serialization identifier. / 직렬화 식별자. */
        private static final long serialVersionUID = 1L;
        /** The copied texts. 복사된 텍스트. */
        final List<String> texts = Collections.synchronizedList(new ArrayList<String>());
        /** The copied markup (null entries for none). 복사된 마크업(없으면 null). */
        final List<String> markups = Collections.synchronizedList(new ArrayList<String>());
        /** What a paste reads, or null for an empty clipboard. 붙여넣기가 읽는 것. 빈 클립보드이면 null. */
        volatile Transferable clipboard;
        /** True to fail the read of the clipboard. 클립보드 읽기를 실패시키려면 true. */
        volatile boolean clipboardBroken;
        /** What the component switched the input methods to, in order (null while the constructor of the base class runs). 컴포넌트가 입력기를 바꾼 값들(기반 클래스 생성자가 도는 동안은 null). */
        volatile List<Boolean> methods;
        /** The server that gives the pages of the cases. 사례의 페이지를 주는 서버. */
        private TestHttpServer server;
        /** The number of pages that were shown. 보여 준 페이지 수. */
        private int pages;

        /**
         * Creates the component.
         * <p>
         * 컴포넌트를 만든다.
         */
        Panel() {
            methods = Collections.synchronizedList(new ArrayList<Boolean>());
        }

        /**
         * Records the switching of the input methods.
         * <p>
         * 입력기를 바꾼 것을 기록한다.
         *
         * @param enable true to turn them on / 켜려면 true
         */
        public void enableInputMethods(boolean enable) {
            List<Boolean> list = methods;
            if (list != null) {
                list.add(Boolean.valueOf(enable));
            }
            super.enableInputMethods(enable);
        }

        /**
         * Starts the navigation to a page that the test server gives (it starts the server at the first call).
         * <p>
         * 시험 서버가 주는 페이지로의 이동을 시작한다(첫 호출에서 서버를 시작한다).
         *
         * @param html the page / 페이지
         * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
         */
        void show(String html) throws Exception {
            if (server == null) {
                server = new TestHttpServer();
                server.start();
            }
            pages++;
            String path = "/page" + pages;
            server.routeFixed(path, TestHttpServer.Response.html(html));
            setPage(server.url(path));
        }

        /**
         * Stops the server of the cases.
         * <p>
         * 사례의 서버를 멈춘다.
         */
        void stopServer() {
            if (server != null) {
                server.stop();
                server = null;
            }
        }

        /**
         * Records the copy.
         * <p>
         * 복사를 기록한다.
         *
         * @param plainText    the text / 텍스트
         * @param htmlFragment the markup or null / 마크업. 없으면 null
         */
        protected void copyToClipboard(String plainText, String htmlFragment) throws Exception {
            texts.add(plainText);
            markups.add(htmlFragment);
        }

        /**
         * Gives the clipboard variable.
         * <p>
         * 클립보드 변수를 준다.
         *
         * @return the data or null / 데이터. 없으면 null
         * @throws Exception when the clipboard is set to be broken / 클립보드가 고장으로 정해졌으면
         */
        protected Transferable readClipboard() throws Exception {
            if (clipboardBroken) {
                throw new IOException("the clipboard is broken");
            }
            return clipboard;
        }
    }

    /**
     * Creates a component of a given size on the EDT.
     * <p>
     * EDT 에서 주어진 크기의 컴포넌트를 만든다.
     *
     * @param width  width / 너비
     * @param height height / 높이
     * @return the component / 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static Panel create(final int width, final int height) throws Exception {
        final Panel[] result = new Panel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                result[0] = new Panel();
                result[0].setSize(width, height);
            }
        });
        return result[0];
    }

    /**
     * Returns an item of the sessionStorage of the component, or an empty text.
     * <p>
     * 컴포넌트의 sessionStorage 항목을 반환한다. 없으면 빈 문자열.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     */
    static String stored(SwingWebPanel panel, String key) {
        // 컴포넌트의 sessionStorage 항목 중 키가 같은 것을 찾는다.
        for (StorageEntry e : panel.getSessionStorage()) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return "";
    }

    /**
     * The rectangles of the elements of the test page that the script reported: x, y, width, height in CSS pixels.
     * <p>
     * 스크립트가 알린 시험 페이지 요소의 사각형: x, y, 너비, 높이(CSS 픽셀).
     */
    static final class Rects {
        /** The rectangles of ed, ro and box. ed, ro, box 의 사각형. */
        final double[][] r = new double[3][];

        /**
         * Parses the report.
         * <p>
         * 알림을 해석한다.
         *
         * @param report the report / 알림
         */
        Rects(String report) {
            String[] parts = report.split(";");
            // 요소마다 네 수를 해석한다.
            for (int i = 0; i < 3; i++) {
                String[] n = parts[i].split(",");
                r[i] = new double[] { Double.parseDouble(n[0]), Double.parseDouble(n[1]), Double.parseDouble(n[2]), Double.parseDouble(n[3]) };
            }
        }

        /**
         * Returns the left edge of an element.
         * <p>
         * 요소의 왼쪽 가장자리를 반환한다.
         *
         * @param i the element (0 ed, 1 ro, 2 box) / 요소
         * @return x / x
         */
        double left(int i) {
            return r[i][0];
        }

        /**
         * Returns the y of the middle of the first line of an element.
         * <p>
         * 요소의 첫 줄 가운데 y 를 반환한다.
         *
         * @param i the element / 요소
         * @return y / y
         */
        double middle(int i) {
            return r[i][1] + 12;
        }

        /**
         * Returns the width of a character of the text <code>hello world</code> (the font is monospace).
         * <p>
         * 텍스트 <code>hello world</code> 의 글자 하나의 너비를 반환한다(글꼴이 고정폭이다).
         *
         * @return the width / 너비
         */
        double advance() {
            return textWidth / 11;
        }

        /** The width of the text "hello world" in the host. 호스트의 텍스트 "hello world" 의 너비. */
        double textWidth;
    }

    /**
     * Shows a page, waits until the script reported the rectangles, and gives the host the keyboard focus of the window (as if the component
     * had it).
     * <p>
     * 페이지를 보여 주고 스크립트가 사각형을 알릴 때까지 기다린 뒤 호스트에 창의 키보드 포커스를 준다(컴포넌트가 가진 것처럼).
     *
     * @param panel the component / 컴포넌트
     * @param html  the page / 페이지
     * @return the rectangles / 사각형
     * @throws Exception on failure / 실패 시
     */
    static Rects open(Panel panel, String html) throws Exception {
        panel.show(html);
        long deadline = System.currentTimeMillis() + WAIT_MS;
        // 스크립트가 사각형을 알릴 때까지 sessionStorage 의 rects 항목을 반복해서 확인한다.
        while (stored(panel, "rects").length() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.isTrue(stored(panel, "rects").length() > 0, "the page reported the rectangles of its elements");
        TestSupport.flushEdt();
        gainFocus(panel);
        Rects rects = new Rects(stored(panel, "rects"));
        rects.textWidth = Double.parseDouble(stored(panel, "textw"));
        return rects;
    }

    /**
     * Returns the rectangle of an element of the page that has an <code>id</code>, as the script reported it when the page was shown: x, y,
     * width and height in CSS pixels.
     * <p>
     * 페이지가 보일 때 스크립트가 알린 대로 <code>id</code> 가 있는 요소의 사각형을 반환한다: x, y, 너비, 높이(CSS 픽셀).
     *
     * @param panel the component / 컴포넌트
     * @param id    the id of the element / 요소의 id
     * @return {x, y, width, height} / {x, y, 너비, 높이}
     */
    static double[] rect(SwingWebPanel panel, String id) {
        String[] n = stored(panel, "rect:" + id).split(",");
        return new double[] { Double.parseDouble(n[0]), Double.parseDouble(n[1]), Double.parseDouble(n[2]), Double.parseDouble(n[3]) };
    }

    /**
     * Returns the x of the middle of an element.
     * <p>
     * 요소의 가운데 x 를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param id    the id / id
     * @return x / x
     */
    static double midX(SwingWebPanel panel, String id) {
        double[] r = rect(panel, id);
        return r[0] + r[2] / 2;
    }

    /**
     * Returns the y of the middle of an element.
     * <p>
     * 요소의 가운데 y 를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param id    the id / id
     * @return y / y
     */
    static double midY(SwingWebPanel panel, String id) {
        double[] r = rect(panel, id);
        return r[1] + r[3] / 2;
    }

    /**
     * Tells the component's focus listeners that the component gained the keyboard focus (the test component is not shown in a window).
     * <p>
     * 컴포넌트의 포커스 리스너에 컴포넌트가 키보드 포커스를 얻었다고 알린다(시험 컴포넌트는 창에 보이지 않는다).
     *
     * @param panel the component / 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void gainFocus(final SwingWebPanel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Notifies the listeners. / 리스너에 알린다. */
            public void run() {
                FocusEvent event = new FocusEvent(panel, FocusEvent.FOCUS_GAINED);
                // 등록된 포커스 리스너를 직접 부른다.
                for (java.awt.event.FocusListener l : panel.getFocusListeners()) {
                    l.focusGained(event);
                }
            }
        });
    }

    /**
     * Tells the component's focus listeners that the component lost the keyboard focus (to a component that is not part of it).
     * <p>
     * 컴포넌트의 포커스 리스너에 컴포넌트가 키보드 포커스를 잃었다고(자기 일부가 아닌 컴포넌트로) 알린다.
     *
     * @param panel the component / 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void loseFocus(final SwingWebPanel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Notifies the listeners. / 리스너에 알린다. */
            public void run() {
                FocusEvent event = new FocusEvent(panel, FocusEvent.FOCUS_LOST);
                // 등록된 포커스 리스너를 직접 부른다.
                for (java.awt.event.FocusListener l : panel.getFocusListeners()) {
                    l.focusLost(event);
                }
            }
        });
    }

    /**
     * Waits until an item of the sessionStorage of the page has a value.
     * <p>
     * 페이지의 sessionStorage 항목이 어떤 값이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param key      the key / 키
     * @param expected the value / 값
     * @throws Exception on interruption / 인터럽트 시
     */
    static void await(SwingWebPanel panel, String key, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        // 문서 큐의 작업이 끝나 스크립트가 알릴 때까지 항목을 반복해서 확인한다.
        while (System.currentTimeMillis() < deadline) {
            if (expected.equals(stored(panel, key))) {
                return;
            }
            Thread.sleep(10);
        }
        Check.equal(expected, stored(panel, key), "the page should report '" + key + "'");
    }

    /**
     * Waits until an item of the sessionStorage of the page contains a text (for events that arrive while a drag runs, when the key events
     * of {@link #drain} are suppressed).
     * <p>
     * 페이지의 sessionStorage 항목이 어떤 텍스트를 포함할 때까지 기다린다(끌기가 도는 동안은 {@link #drain} 의 키 이벤트가 막히므로 그동안 오는 이벤트용).
     *
     * @param panel    the component / 컴포넌트
     * @param key      the key / 키
     * @param fragment the text that the value must contain / 값이 포함해야 하는 텍스트
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitContains(SwingWebPanel panel, String key, String fragment) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        // 문서 큐의 작업이 끝나 스크립트가 알릴 때까지 항목을 반복해서 확인한다.
        while (System.currentTimeMillis() < deadline) {
            if (stored(panel, key).indexOf(fragment) >= 0) {
                return;
            }
            Thread.sleep(10);
        }
        Check.contains(stored(panel, key), fragment, "the page should report '" + key + "' with this text");
    }

    /**
     * Waits until the HTML of the editing host is a given text.
     * <p>
     * 편집 호스트의 HTML 이 주어진 텍스트가 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param expected the HTML / HTML
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitHtml(SwingWebPanel panel, String expected) throws Exception {
        await(panel, "html", expected);
    }

    /**
     * Waits until the selection in the host is <code>anchor,focus</code> (offsets in its text).
     * <p>
     * 호스트의 선택이 <code>anchor,focus</code>(텍스트의 오프셋)가 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param expected the offsets / 오프셋
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitSel(SwingWebPanel panel, String expected) throws Exception {
        await(panel, "sel", expected);
    }

    /**
     * Waits until everything that was sent before was handled by the document queue (a key F12 is sent and its <code>keyup</code> is awaited).
     * <p>
     * 이전에 보낸 모든 것을 문서 큐가 처리할 때까지 기다린다(F12 키를 보내고 그 <code>keyup</code> 을 기다린다).
     *
     * @param panel the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    static void drain(SwingWebPanel panel) throws Exception {
        String before = stored(panel, "markers");
        int n = before.length() == 0 ? 0 : Integer.parseInt(before);
        tap(panel, KeyEvent.VK_F12, KeyEvent.CHAR_UNDEFINED, 0);
        await(panel, "markers", String.valueOf(n + 1));
        TestSupport.flushEdt();
    }

    /**
     * Returns the events that reached the host so far (the <code>log</code> of the page).
     * <p>
     * 지금까지 호스트에 닿은 이벤트(페이지의 <code>log</code>)를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the log / 로그
     */
    static String log(SwingWebPanel panel) {
        return stored(panel, "log");
    }

    /**
     * Dispatches a key event to the key listeners on the EDT.
     * <p>
     * EDT 에서 키 이벤트를 키 리스너에 전달한다.
     *
     * @param panel     the component / 컴포넌트
     * @param id        <code>KEY_PRESSED</code>, <code>KEY_TYPED</code> or <code>KEY_RELEASED</code>
     * @param keyCode   the AWT key code / AWT 키 코드
     * @param keyChar   the character / 문자
     * @param modifiers the extended modifiers / 확장 보조 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void keyEvent(final SwingWebPanel panel, final int id, final int keyCode, final char keyChar, final int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                KeyEvent event = new KeyEvent(panel, id, System.currentTimeMillis(), modifiers, id == KeyEvent.KEY_TYPED ? KeyEvent.VK_UNDEFINED : keyCode,
                        keyChar);
                // 등록된 키 리스너를 직접 부른다(AWT 는 포커스가 없는 컴포넌트의 키 이벤트를 포커스 소유자에게 돌려보낸다).
                for (KeyListener l : panel.getKeyListeners()) {
                    if (id == KeyEvent.KEY_PRESSED) {
                        l.keyPressed(event);
                    } else if (id == KeyEvent.KEY_TYPED) {
                        l.keyTyped(event);
                    } else {
                        l.keyReleased(event);
                    }
                }
            }
        });
    }

    /**
     * Presses and releases a key that gives no character (an arrow key, Backspace, a shortcut): <code>KEY_PRESSED</code> and
     * <code>KEY_RELEASED</code>.
     * <p>
     * 글자를 만들지 않는 키(화살표, Backspace, 단축키)를 누르고 뗀다: <code>KEY_PRESSED</code> 와 <code>KEY_RELEASED</code>.
     *
     * @param panel     the component / 컴포넌트
     * @param keyCode   the AWT key code / AWT 키 코드
     * @param keyChar   the character that the platform reports for the key press (a control character for Ctrl+letter) / 플랫폼이 키 누름에 보고하는 문자
     * @param modifiers the extended modifiers / 확장 보조 값
     * @throws Exception on failure / 실패 시
     */
    static void tap(SwingWebPanel panel, int keyCode, char keyChar, int modifiers) throws Exception {
        keyEvent(panel, KeyEvent.KEY_PRESSED, keyCode, keyChar, modifiers);
        keyEvent(panel, KeyEvent.KEY_RELEASED, keyCode, keyChar, modifiers);
    }

    /**
     * Types a text as a keyboard does: for every character <code>KEY_PRESSED</code>, <code>KEY_TYPED</code> and <code>KEY_RELEASED</code>
     * (a line feed is the Enter key).
     * <p>
     * 키보드가 하는 대로 텍스트를 입력한다: 글자마다 <code>KEY_PRESSED</code>, <code>KEY_TYPED</code>, <code>KEY_RELEASED</code>(줄바꿈은 Enter 키).
     *
     * @param panel the component / 컴포넌트
     * @param text  the text / 텍스트
     * @throws Exception on failure / 실패 시
     */
    static void type(SwingWebPanel panel, String text) throws Exception {
        // 글자마다 누름, 입력, 뗌을 보낸다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int code = c == '\n' ? KeyEvent.VK_ENTER : c == ' ' ? KeyEvent.VK_SPACE : Character.isLetter(c) ? Character.toUpperCase(c) : KeyEvent.VK_UNDEFINED;
            int mod = Character.isUpperCase(c) ? InputEvent.SHIFT_DOWN_MASK : 0;
            keyEvent(panel, KeyEvent.KEY_PRESSED, code, c, mod);
            keyEvent(panel, KeyEvent.KEY_TYPED, code, c, mod);
            keyEvent(panel, KeyEvent.KEY_RELEASED, code, c, mod);
        }
    }

    /**
     * Sends an input method event the way an input method does: the committed text and the text that is still composed.
     * <p>
     * 입력기가 하는 대로 입력기 이벤트를 보낸다: 확정된 텍스트와 아직 조합 중인 텍스트.
     *
     * @param panel     the component / 컴포넌트
     * @param committed the committed text / 확정된 텍스트
     * @param composing the text that is still composed (empty for none) / 아직 조합 중인 텍스트. 없으면 빈 문자열
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void inputMethod(final SwingWebPanel panel, final String committed, final String composing) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                String all = committed + composing;
                AttributedString text = new AttributedString(all);
                InputMethodEvent event = new InputMethodEvent(panel, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                        all.length() == 0 ? null : text.getIterator(), committed.length(), null, null);
                // 등록된 입력기 리스너를 직접 부른다.
                for (InputMethodListener l : panel.getInputMethodListeners()) {
                    l.inputMethodTextChanged(event);
                }
            }
        });
    }

    /**
     * Dispatches a mouse event on the EDT.
     * <p>
     * EDT 에서 마우스 이벤트를 전달한다.
     *
     * @param panel      the component / 컴포넌트
     * @param id         the AWT event id / AWT 이벤트 ID
     * @param x          component x / 컴포넌트 x
     * @param y          component y / 컴포넌트 y
     * @param clickCount the click count / 클릭 횟수
     * @param modifiers  the extended modifiers / 확장 보조 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void mouse(final SwingWebPanel panel, final int id, final double x, final double y, final int clickCount, final int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, id, System.currentTimeMillis(), modifiers, (int) Math.round(x), (int) Math.round(y), clickCount,
                        false, id == MouseEvent.MOUSE_MOVED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Presses and releases the main button at a point (a click).
     * <p>
     * 한 점에서 주 버튼을 누르고 뗀다(클릭).
     *
     * @param panel      the component / 컴포넌트
     * @param x          component x / 컴포넌트 x
     * @param y          component y / 컴포넌트 y
     * @param clickCount the click count / 클릭 횟수
     * @param shift      true to hold Shift / Shift 를 누르고 있으면 true
     * @throws Exception on failure / 실패 시
     */
    static void click(SwingWebPanel panel, double x, double y, int clickCount, boolean shift) throws Exception {
        int down = InputEvent.BUTTON1_DOWN_MASK | (shift ? InputEvent.SHIFT_DOWN_MASK : 0);
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y, clickCount, down);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y, clickCount, shift ? InputEvent.SHIFT_DOWN_MASK : 0);
    }

    /**
     * Presses the main button, drags to another point and releases (all in component coordinates).
     * <p>
     * 주 버튼을 누르고 다른 점까지 끌고 뗀다(모두 컴포넌트 좌표).
     *
     * @param panel the component / 컴포넌트
     * @param x1    the x of the press / 누른 곳의 x
     * @param y1    the y of the press / 누른 곳의 y
     * @param x2    the x of the release / 뗀 곳의 x
     * @param y2    the y of the release / 뗀 곳의 y
     * @throws Exception on failure / 실패 시
     */
    static void drag(SwingWebPanel panel, double x1, double y1, double x2, double y2) throws Exception {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x1, y1, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_DRAGGED, (x1 + x2) / 2, (y1 + y2) / 2, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_DRAGGED, x2, y2, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x2, y2, 1, 0);
    }

    /**
     * Paints the component into an image on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그린다.
     *
     * @param panel the component / 컴포넌트
     * @return the image / 이미지
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static BufferedImage paint(final SwingWebPanel panel) throws Exception {
        final BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints without a native window. / 네이티브 창 없이 그린다. */
            public void run() {
                BufferedImage image = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                result[0] = image;
            }
        });
        return result[0];
    }

    /**
     * Closes a component and its server.
     * <p>
     * 컴포넌트와 서버를 닫는다.
     *
     * @param panel the component / 컴포넌트
     */
    static void close(SwingWebPanel panel) {
        panel.dispose();
        if (panel instanceof Panel) {
            ((Panel) panel).stopServer();
        }
        TestSupport.flushEdt();
    }

    /**
     * Returns a transferable with only a text.
     * <p>
     * 텍스트만 가진 전달 객체를 반환한다.
     *
     * @param text the text / 텍스트
     * @return the data / 데이터
     */
    static Transferable text(String text) {
        return new StringSelection(text);
    }
}
