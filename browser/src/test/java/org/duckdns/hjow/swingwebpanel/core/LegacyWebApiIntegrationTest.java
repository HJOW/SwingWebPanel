package org.duckdns.hjow.swingwebpanel.core;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the layout dependent members of {@link org.duckdns.hjow.swingwebpanel.js.host.LegacyWebApi} in a real component (plan item P20-01):
 * <code>clientTop</code>, <code>clientLeft</code>, <code>offsetParent</code> and <code>document.elementsFromPoint</code>.
 * <p>
 * 실제 컴포넌트에서 {@link org.duckdns.hjow.swingwebpanel.js.host.LegacyWebApi} 의 레이아웃에 의존하는 멤버의 시험(계획 P20-01): <code>clientTop</code>,
 * <code>clientLeft</code>, <code>offsetParent</code>, <code>document.elementsFromPoint</code>.
 */
public class LegacyWebApiIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyWebApiIntegrationTest() {
    }

    /**
     * Finds the session of a component by reflection (the product has no testing API).
     * <p>
     * 컴포넌트의 세션을 리플렉션으로 찾는다(제품에는 시험용 API 가 없다).
     *
     * @param panel the component / 컴포넌트
     * @return the session / 세션
     * @throws Exception on failure / 실패 시
     */
    private static BrowserSession session(SwingWebPanel panel) throws Exception {
        Field field = SwingWebPanel.class.getDeclaredField("session");
        field.setAccessible(true);
        return (BrowserSession) field.get(panel);
    }

    /**
     * Evaluates a script on the document queue of the page and returns its completion value.
     * <p>
     * 페이지의 문서 큐에서 스크립트를 평가하고 완료 값을 반환한다.
     *
     * @param session the session / 세션
     * @param code    the script / 스크립트
     * @return the result / 결과
     * @throws Exception on failure / 실패 시
     */
    private static String evaluate(final BrowserSession session, final String code) throws Exception {
        final String[] result = new String[1];
        final Throwable[] failure = new Throwable[1];
        final CountDownLatch done = new CountDownLatch(1);
        session.submitDocumentTask(new Runnable() {
            /**
             * Runs the script in the realm of the confirmed page.
             * <p>
             * 확정된 페이지의 realm 에서 스크립트를 실행한다.
             */
            public void run() {
                try {
                    Field field = HtmlPageDocument.class.getDeclaredField("runtime");
                    field.setAccessible(true);
                    PageRuntime runtime = (PageRuntime) field.get(session.getSnapshot().getDocument());
                    result[0] = runtime.scripts().evaluate(code, "legacy-test");
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    done.countDown();
                }
            }
        });
        Check.isTrue(done.await(5, TimeUnit.SECONDS), "the query completed");
        Check.isNull(failure[0], "the query failed: " + failure[0]);
        return result[0];
    }

    /**
     * Creates a component of 300 x 180 pixels on the EDT.
     * <p>
     * EDT 에서 300 x 180 화소의 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private static SwingWebPanel sizedPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Loads the test page and returns the session.
     * <p>
     * 시험 페이지를 읽고 세션을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the session / 세션
     * @throws Exception on failure / 실패 시
     */
    private static BrowserSession load(SwingWebPanel panel) throws Exception {
        BrowserSession state = session(panel);
        panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}"
                + "#outer{position:relative;left:20px;top:10px;width:100px;height:80px;border:4px solid red;border-left-width:6px}"
                + "#inner{width:30px;height:20px;background:#00f}#plain{width:10px;height:10px}#fixed{position:fixed;left:0;top:100px;width:5px;height:5px}"
                + "#none{display:none}</style><div id=outer><div id=inner></div></div><p id=plain></p><div id=fixed></div><div id=none></div>"
                + "<table id=tb><tr><td id=td><span id=sp>x</span></td></tr></table>", "text/html");
        TestSupport.settle(state);
        return state;
    }

    /**
     * <code>clientTop</code> and <code>clientLeft</code> are the border widths of the element, 0 without a box.
     * <p>
     * <code>clientTop</code> 과 <code>clientLeft</code> 는 요소의 테두리 두께이고 상자가 없으면 0 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClientTopAndLeft() throws Exception {
        SwingWebPanel panel = sizedPanel();
        try {
            BrowserSession state = load(panel);
            Check.equal("4|6|0|0|0", evaluate(state, "var g = function (id) { return document.getElementById(id); };"
                    + "[g('outer').clientTop, g('outer').clientLeft, g('inner').clientTop, g('none').clientLeft, g('sp').clientTop].join('|')"),
                    "the border widths, 0 for an element without border, without a box and inline");
        } finally {
            panel.dispose();
        }
    }

    /**
     * <code>offsetParent</code> follows the CSSOM View algorithm: positioned ancestors, table parts for static elements, the body, and null for
     * the root, the body, fixed and not rendered elements.
     * <p>
     * <code>offsetParent</code> 가 CSSOM View 알고리즘을 따른다: 위치 지정 조상, static 요소의 표 부분, body, 그리고 루트, body, fixed, 그려지지 않는 요소는 null.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOffsetParent() throws Exception {
        SwingWebPanel panel = sizedPanel();
        try {
            BrowserSession state = load(panel);
            Check.equal("outer|BODY|BODY|null|null|null|null|td", evaluate(state, "var g = function (id) { return document.getElementById(id); };"
                    + "var n = function (e) { return e === null ? 'null' : (e.id || e.tagName); };"
                    + "[n(g('inner').offsetParent), n(g('outer').offsetParent), n(g('plain').offsetParent), n(document.body.offsetParent), "
                    + "n(document.documentElement.offsetParent), n(g('fixed').offsetParent), n(g('none').offsetParent), n(g('sp').offsetParent)].join('|')"),
                    "the offset parents");
        } finally {
            panel.dispose();
        }
    }

    /**
     * <code>document.elementsFromPoint</code> lists the topmost element and its ancestors, and nothing outside the viewport.
     * <p>
     * <code>document.elementsFromPoint</code> 가 최상위 요소와 조상을 나열하고 뷰포트 밖이면 아무것도 나열하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testElementsFromPoint() throws Exception {
        SwingWebPanel panel = sizedPanel();
        try {
            BrowserSession state = load(panel);
            Check.equal("inner,outer,BODY,HTML", evaluate(state, "document.elementsFromPoint(30, 20).map(function (e) { return e.id || e.tagName; }).join()"),
                    "the stack at a point inside the inner box");
            Check.equal("0|0", evaluate(state, "document.elementsFromPoint(-1, 5).length + '|' + document.elementsFromPoint(5000, 5).length"),
                    "outside the viewport");
            Check.equal("TypeError", evaluate(state, "try { document.elementsFromPoint(NaN, 1); 'no error' } catch (e) { e.name }"), "a coordinate that is not finite");
        } finally {
            panel.dispose();
        }
    }
}
