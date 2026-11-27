package org.duckdns.hjow.swingwebpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The default fonts of a component (API contract 5.27, cases C-FONT-1 to C-FONT-5): the default font size and the size keywords, the
 * standard family for content without an author font, the families behind the generic names, the size of the monospace elements, and
 * the rules of the setting itself (arguments, events, other components, frames, <code>clean()</code>). The pages report what scripts
 * measure through their title, which they refresh with a timer, so that a change of a live page is seen too.
 * <p>
 * 컴포넌트의 기본 글꼴(API 계약 5.27, 사례 C-FONT-1 ~ C-FONT-5): 기본 글꼴 크기와 크기 키워드, 작성자 글꼴이 없는 내용의 표준 패밀리, 일반 이름
 * 뒤의 패밀리, 고정폭 요소의 크기, 그리고 설정 자체의 규칙(인수, 이벤트, 다른 컴포넌트, 프레임, <code>clean()</code>). 페이지는 스크립트가 잰 것을
 * 제목으로 알리고 타이머로 제목을 갱신하므로, 살아 있는 페이지의 변경도 볼 수 있다.
 */
public class DefaultFontsTest {

    /** A family that every JRE has and that is clearly narrower or wider than the serif font: the logical monospaced font. / 모든 JRE 에 있고 serif 글꼴과 폭이 분명히 다른 패밀리: 논리 고정폭 글꼴. */
    private static final String MONO = "Monospaced";

    /** The script that writes its measurements into the title again and again. / 잰 것을 제목에 반복해서 적는 스크립트. */
    private static final String REPORT = "<script>function size(id) { return getComputedStyle(document.getElementById(id)).fontSize; }"
            + "function width(id) { return Math.round(document.getElementById(id).getBoundingClientRect().width * 10) / 10; }"
            + "function family(id) { return getComputedStyle(document.getElementById(id)).fontFamily; }"
            + "setInterval(function () { document.title = report(); }, 30);</" + "script>";

    /** The component of the case. / 사례의 컴포넌트. */
    private SwingWebPanel panel;

    /** The property changes that were announced. / 알려진 속성 변경. */
    private final List<String> changes = new ArrayList<String>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DefaultFontsTest() {
    }

    /**
     * Creates the component.
     * <p>
     * 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        panel = create();
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_DEFAULT_FONTS, new PropertyChangeListener() {
            /** Remembers the change and the thread. / 변경과 스레드를 기억한다. */
            public void propertyChange(PropertyChangeEvent e) {
                synchronized (changes) {
                    changes.add(e.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : "@other"));
                }
            }
        });
    }

    /**
     * Creates a component with a size on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 크기가 있는 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private static SwingWebPanel create() throws Exception {
        final SwingWebPanel[] made = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                made[0] = new SwingWebPanel();
                made[0].setSize(600, 400);
            }
        });
        return made[0];
    }

    /**
     * Disposes the component.
     * <p>
     * 컴포넌트를 폐기한다.
     */
    public void tearDown() {
        panel.dispose();
    }

    /**
     * Waits until the title of a component is the expected text.
     * <p>
     * 컴포넌트의 제목이 기대한 글이 될 때까지 기다린다.
     *
     * @param p        the component / 컴포넌트
     * @param expected the expected title / 기대하는 제목
     * @param message  what the title means / 제목이 뜻하는 것
     * @throws Exception on failure / 실패 시
     */
    private static void awaitTitle(SwingWebPanel p, String expected, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 제목이 될 때까지 짧게 쉬며 기다린다.
        while (!expected.equals(p.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(expected, p.getTitle(), message);
    }

    /**
     * Waits until the title of a component starts with a marker and returns the title.
     * <p>
     * 컴포넌트의 제목이 표시로 시작할 때까지 기다리고 그 제목을 반환한다.
     *
     * @param p      the component / 컴포넌트
     * @param marker the beginning of the title / 제목의 앞부분
     * @return the title / 제목
     * @throws Exception on failure / 실패 시
     */
    private static String awaitMarked(SwingWebPanel p, String marker) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 제목이 표시로 시작할 때까지 짧게 쉬며 기다린다.
        while (!p.getTitle().startsWith(marker) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(p.getTitle().startsWith(marker), "the page reported: " + p.getTitle());
        return p.getTitle();
    }

    /**
     * C-FONT-1: the default size is the size of <code>medium</code>; the other keywords scale with it, the root starts from it (so a
     * percentage or rem follows it), and a live page is styled again when the size changes.
     * <p>
     * C-FONT-1: 기본 크기는 <code>medium</code> 의 크기이다. 다른 키워드는 이에 비례하고, 루트가 여기서 시작하며(그래서 백분율과 rem 이 따라간다),
     * 크기가 바뀌면 살아 있는 페이지의 스타일을 다시 계산한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FONT_1_DefaultSize() throws Exception {
        Check.equal(16, panel.getDefaultFontSize(), "the built-in default size");
        String page = "<!doctype html><title>-</title><style>#m { font-size: medium } #s { font-size: small } #x { font-size: x-large }"
                + " #r { font-size: 2rem } #f { font-size: 10px }</style>"
                + "<p id=p>plain</p><p id=m>medium</p><p id=s>small</p><p id=x>x-large</p><p id=r>rem</p><p id=f>fixed</p><h1 id=h>heading</h1>"
                + REPORT + "<script>function report() { return [size('p'), size('m'), size('s'), size('x'), size('r'), size('f'), size('h')].join(' '); }</" + "script>";
        panel.setContent(page, "text/html");
        awaitTitle(panel, "16px 16px 13px 24px 32px 10px 32px", "the built-in sizes");
        panel.setDefaultFontSize(20);
        Check.equal(20, panel.getDefaultFontSize(), "the size that was set");
        awaitTitle(panel, "20px 20px 16.25px 30px 40px 10px 40px", "a live page follows the new default size");
        panel.setContent(page.replace("<style>", "<style>html { font-size: 50% } "), "text/html");
        awaitTitle(panel, "10px 20px 16.25px 30px 20px 10px 20px", "a percentage on the root is taken from the default size; keywords are not");
        panel.setDefaultFontSize(16);
        awaitTitle(panel, "8px 16px 13px 24px 16px 10px 16px", "back to the built-in size");
    }

    /**
     * C-FONT-2: the standard family is the font of content whose author named none; it is laid out with that font, author rules win
     * over it, and removing it brings the built-in font back.
     * <p>
     * C-FONT-2: 표준 패밀리는 작성자가 글꼴을 지정하지 않은 내용의 글꼴이다. 그 글꼴로 배치하고, 작성자 규칙이 우선하며, 지우면 내장 글꼴로 돌아간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FONT_2_StandardFamily() throws Exception {
        Check.equal("", panel.getDefaultFontFamily(SwingWebPanel.FONT_STANDARD), "no standard family at first");
        String text = "iiiiiiiiiiWWWWWWWWWW";
        String page = "<!doctype html><title>-</title><style>span { white-space: nowrap } #mono { font-family: " + MONO + " }"
                + " #author { font-family: serif }</style>"
                + "<span id=plain>" + text + "</span><br><span id=mono>" + text + "</span><br><span id=author>" + text + "</span>"
                + REPORT + "<script>function report() { return 'w:' + [width('plain') === width('mono'), width('author') === width('mono'),"
                + " family('plain').indexOf('" + MONO + "') >= 0, family('author')].join(' '); }</" + "script>";
        panel.setContent(page, "text/html");
        awaitTitle(panel, "w:false false false serif", "without a standard family the plain text is in the serif font");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, "  " + MONO + " ");
        Check.equal(MONO, panel.getDefaultFontFamily(SwingWebPanel.FONT_STANDARD), "the trimmed name is kept");
        awaitTitle(panel, "w:true false true serif", "plain text is laid out with the standard family; the author font stays");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, null);
        awaitTitle(panel, "w:false false false serif", "removing the standard family brings the built-in font back");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, "No Such Font Family 12345");
        awaitTitle(panel, "w:false false false serif", "a family that is not installed changes nothing in the layout");
    }

    /**
     * C-FONT-3: the families behind the generic names are used wherever a style names <code>serif</code>, <code>sans-serif</code> or
     * <code>monospace</code>, also as the last name of an author list; a family that is not installed is ignored.
     * <p>
     * C-FONT-3: 일반 이름 뒤의 패밀리는 스타일이 <code>serif</code>, <code>sans-serif</code>, <code>monospace</code> 를 쓰는 모든 곳에 쓰이고,
     * 작성자 목록의 마지막 이름일 때도 쓰인다. 설치되지 않은 패밀리는 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FONT_3_GenericFamilies() throws Exception {
        String text = "iiiiiiiiiiWWWWWWWWWW";
        String page = "<!doctype html><title>-</title><style>span { white-space: nowrap } #mono { font-family: " + MONO + " }"
                + " #serif { font-family: serif } #sans { font-family: 'No Such Font 1', sans-serif } #fixed { font-family: monospace }</style>"
                + "<span id=mono>" + text + "</span><br><span id=serif>" + text + "</span><br><span id=sans>" + text + "</span><br>"
                + "<span id=fixed>" + text + "</span><br><span id=plain>" + text + "</span>"
                + REPORT + "<script>function report() { var m = width('mono'); return 'g:' + [width('serif') === m, width('sans') === m,"
                + " width('fixed') === m, width('plain') === m].join(' '); }</" + "script>";
        panel.setContent(page, "text/html");
        awaitTitle(panel, "g:false false true false", "the built-in generic fonts");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_SERIF, MONO);
        awaitTitle(panel, "g:true false true true", "serif stands for the chosen family, also for the initial font of plain text");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_SANS_SERIF, MONO);
        awaitTitle(panel, "g:true true true true", "sans-serif at the end of an author list stands for the chosen family");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_SERIF, "");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_SANS_SERIF, "No Such Font Family 12345");
        panel.setDefaultFontFamily(SwingWebPanel.FONT_MONOSPACE, "Serif");
        awaitTitle(panel, "g:false false false false", "monospace can stand for another family; a missing family is ignored");
        Check.equal("Serif", panel.getDefaultFontFamily(SwingWebPanel.FONT_MONOSPACE), "the family of a kind");
    }

    /**
     * C-FONT-4: the size of the monospace elements is given for text of the default size, scales with the surrounding text, is not
     * applied twice to nested elements and loses against author rules; 0 gives the size of the surrounding text.
     * <p>
     * C-FONT-4: 고정폭 요소의 크기는 기본 크기의 글을 기준으로 주고, 주변 글에 비례하며, 중첩된 요소에 두 번 적용되지 않고, 작성자 규칙에 진다.
     * 0 이면 주변 글의 크기이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FONT_4_MonospaceSize() throws Exception {
        Check.equal(0, panel.getDefaultMonospaceFontSize(), "no monospace size at first");
        String page = "<!doctype html><title>-</title><style>#author { font-size: 10px }</style>"
                + "<p><code id=c>code</code> <kbd id=k>kbd</kbd> <code id=author>author</code></p><pre id=pre>pre <code id=nested>nested</code></pre>"
                + "<h1><code id=h>heading</code></h1><textarea id=ta></textarea>"
                + REPORT + "<script>function report() { return [size('c'), size('k'), size('pre'), size('nested'), size('h'), size('author')].join(' '); }</" + "script>";
        panel.setContent(page, "text/html");
        awaitTitle(panel, "16px 16px 16px 16px 32px 10px", "without the setting monospace elements have the size of their surroundings");
        panel.setDefaultMonospaceFontSize(13);
        Check.equal(13, panel.getDefaultMonospaceFontSize(), "the size that was set");
        awaitTitle(panel, "13px 13px 13px 13px 26px 10px", "13 for 16: scaled in a heading, not applied twice when nested, author rule wins");
        panel.setDefaultFontSize(20);
        awaitTitle(panel, "13px 13px 13px 13px 26px 10px", "the monospace size is given for text of the default size");
        panel.setDefaultMonospaceFontSize(0);
        awaitTitle(panel, "20px 20px 20px 20px 40px 10px", "0 gives the size of the surrounding text");
    }

    /**
     * C-FONT-5: wrong arguments are refused, changes are announced once on the event dispatch thread, the setting stays through
     * <code>clean()</code> and for later pages, reaches frames, does not reach another component, and a disposed component refuses it.
     * <p>
     * C-FONT-5: 잘못된 인수는 거부하고, 변경은 이벤트 디스패치 스레드에서 한 번 알리며, 설정은 <code>clean()</code> 뒤와 이후의 페이지에 유지되고,
     * 프레임에 닿고, 다른 컴포넌트에는 닿지 않으며, 폐기된 컴포넌트는 설정을 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_FONT_5_RulesOfTheSetting() throws Exception {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Uses an unknown kind. / 모르는 종류를 쓴다. */
            public void run() {
                panel.setDefaultFontFamily("cursive", MONO);
            }
        }, "an unknown kind");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Reads an unknown kind. / 모르는 종류를 읽는다. */
            public void run() {
                panel.getDefaultFontFamily(null);
            }
        }, "an unknown kind when reading");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Uses a name with a line break. / 줄 바꿈이 든 이름을 쓴다. */
            public void run() {
                panel.setDefaultFontFamily(SwingWebPanel.FONT_SERIF, "Bad\nName");
            }
        }, "a control character in the name");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Uses a size that is too small. / 너무 작은 크기를 쓴다. */
            public void run() {
                panel.setDefaultFontSize(5);
            }
        }, "a default size below the range");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Uses a size that is too large. / 너무 큰 크기를 쓴다. */
            public void run() {
                panel.setDefaultFontSize(73);
            }
        }, "a default size above the range");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Uses a monospace size that is too small. / 너무 작은 고정폭 크기를 쓴다. */
            public void run() {
                panel.setDefaultMonospaceFontSize(3);
            }
        }, "a monospace size outside the range");
        panel.setDefaultFontSize(16);
        panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, "");
        panel.setDefaultFontSize(24);
        panel.setDefaultFontSize(24);
        panel.setDefaultFontFamily(SwingWebPanel.FONT_STANDARD, "A \"quoted\\name\"");
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Lets the queued announcements run. / 대기 중인 알림이 실행되게 한다. */
            public void run() {
            }
        });
        synchronized (changes) {
            Check.equal(2, changes.size(), "one announcement per real change: " + changes);
            Check.contains(changes.get(0), "size=24", "the new value describes the defaults");
            Check.isTrue(changes.get(0).indexOf("@other") < 0 && changes.get(1).indexOf("@other") < 0, "announced on the event dispatch thread");
        }
        SwingWebPanel other = create();
        TestHttpServer server = new TestHttpServer();
        try {
            String page = "<!doctype html><title>-</title><p id=p>x</p>" + REPORT
                    + "<script>function report() { var f = document.querySelector('iframe');"
                    + " var inner = f && f.contentDocument && f.contentDocument.getElementById('q');"
                    + " return 's:' + size('p') + ' ' + (inner ? f.contentWindow.getComputedStyle(inner).fontSize + ' ' + getComputedStyle(inner).fontSize : 'none'); }</" + "script>"
                    + "<iframe src='/inner'></iframe>";
            server.routeFixed("/outer", TestHttpServer.Response.html(page));
            server.routeFixed("/inner", TestHttpServer.Response.html("<!doctype html><p id=q>inner</p>"));
            server.start();
            panel.setPage(server.url("/outer"));
            other.setPage(server.url("/outer"));
            awaitTitle(panel, "s:24px 24px 16px", "the page and its frame use the default size (the window of another document answers with the initial style)");
            awaitTitle(other, "s:16px 16px 16px", "another component keeps its own defaults");
            panel.clean();
            Check.equal(24, panel.getDefaultFontSize(), "clean() keeps the default fonts");
            panel.setPage(server.url("/outer"));
            awaitTitle(panel, "s:24px 24px 16px", "a page after clean() uses them");
            panel.setDefaultFontSize(18);
            Check.isTrue(awaitMarked(panel, "s:18px").equals("s:18px 18px 16px"), "a change reaches the frame of a live page: " + panel.getTitle());
        } finally {
            other.dispose();
            server.stop();
        }
        final SwingWebPanel gone = create();
        gone.dispose();
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Changes a disposed component. / 폐기된 컴포넌트를 바꾼다. */
            public void run() {
                gone.setDefaultFontSize(20);
            }
        }, "a disposed component refuses the setting");
        Check.equal(16, gone.getDefaultFontSize(), "but still answers the query");
    }
}
