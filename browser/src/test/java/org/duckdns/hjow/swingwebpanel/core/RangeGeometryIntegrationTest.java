package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.stored;

import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the geometry that scripts read from ranges and elements with the real component (plan item P20-03): the rectangles of a range
 * are the real rectangles of the selected characters (not the boxes of the elements), they are real <code>DOMRect</code> and
 * <code>DOMRectList</code> objects, <code>Selection.toString()</code> gives the text that copying gives, and the geometry interfaces
 * <code>DOMRect</code>, <code>DOMRectReadOnly</code>, <code>DOMPoint</code> work as constructors.
 * <p>
 * 실제 컴포넌트로 스크립트가 범위와 요소에서 읽는 기하의 시험(계획 P20-03): 범위의 사각형은 요소의 상자가 아니라 선택된 글자의 실제 사각형이고, 진짜
 * <code>DOMRect</code> 와 <code>DOMRectList</code> 객체이며, <code>Selection.toString()</code> 은 복사했을 때의 텍스트를 주고, 기하 인터페이스
 * <code>DOMRect</code>, <code>DOMRectReadOnly</code>, <code>DOMPoint</code> 가 생성자로 동작한다.
 */
public class RangeGeometryIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RangeGeometryIntegrationTest() {
    }

    /**
     * Runs a script on the standard test page and returns what it stored under the key <code>geo</code>.
     * <p>
     * 표준 시험 페이지에서 스크립트를 실행하고 <code>geo</code> 키로 저장한 결과를 반환한다.
     *
     * @param script the script, which calls <code>sessionStorage.setItem('geo', ...)</code> / <code>sessionStorage.setItem('geo', ...)</code> 를 부르는 스크립트
     * @param rects  receives the rectangles of the three spans of the page / 페이지의 span 세 개의 사각형을 받는다
     * @return the stored result / 저장된 결과
     * @throws Exception on failure / 실패 시
     */
    private static String run(String script, Rects[] rects) throws Exception {
        Panel panel = create(400, 300);
        try {
            rects[0] = open(panel, page(script));
            long deadline = System.currentTimeMillis() + 15000;
            // 스크립트가 sessionStorage 에 결과를 쓸 때까지 반복해서 확인한다.
            while (stored(panel, "geo").length() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            return stored(panel, "geo");
        } finally {
            close(panel);
        }
    }

    /**
     * A range inside one text node reports the rectangle of exactly its characters (one <code>DOMRect</code>), not the box of the element
     * that holds the text.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRangeRectangleIsTheRectangleOfTheCharacters() throws Exception {
        Rects[] rects = new Rects[1];
        String result = run("var t = document.getElementById('a').firstChild; var rg = document.createRange(); rg.setStart(t, 2); rg.setEnd(t, 7);"
                + " var b = rg.getBoundingClientRect(); var l = rg.getClientRects(); var s = document.getElementById('a').getBoundingClientRect();"
                + " sessionStorage.setItem('geo', [b instanceof DOMRect, l instanceof DOMRectList, l.length, b.left, b.width, s.left, s.width, b.top, b.height, s.top, s.height,"
                + " l[0] instanceof DOMRect, l.item(0) === l[0], b.right === b.left + b.width, b.bottom === b.top + b.height].join(','));", rects);
        String[] p = result.split(",");
        Check.equal("true", p[0], "the bounding rectangle is a DOMRect");
        Check.equal("true", p[1], "the client rectangles are a DOMRectList");
        Check.equal("1", p[2], "one rectangle for one line");
        double adv = Double.parseDouble(p[6]) / 11;
        Check.near(Double.parseDouble(p[5]) + 2 * adv, Double.parseDouble(p[3]), 0.6, "starts before the third character");
        Check.near(5 * adv, Double.parseDouble(p[4]), 0.8, "five characters wide, not the width of the whole span");
        Check.near(Double.parseDouble(p[9]), Double.parseDouble(p[7]), 1.5, "the top of the line");
        Check.near(Double.parseDouble(p[10]), Double.parseDouble(p[8]), 1.5, "as high as the span");
        Check.equal("true", p[11], "the items of the list are DOMRect objects");
        Check.equal("true", p[12], "item(0) is the same object as [0]");
        Check.equal("true", p[13], "right = left + width");
        Check.equal("true", p[14], "bottom = top + height");
    }

    /**
     * A range from one paragraph into the next reports the rectangle of the tail of the first, then the head of the second; the bounding
     * rectangle is their union; a collapsed range reports the caret place.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRangeOverTwoBlocksAndCollapsedRange() throws Exception {
        Rects[] rects = new Rects[1];
        String result = run("var a = document.getElementById('a').firstChild; var b = document.getElementById('b').firstChild;"
                + " var rg = document.createRange(); rg.setStart(a, 6); rg.setEnd(b, 3); var l = rg.getClientRects(); var u = rg.getBoundingClientRect();"
                + " var c = document.createRange(); c.setStart(a, 4); c.collapse(true); var cl = c.getClientRects();"
                + " sessionStorage.setItem('geo', [l.length, l[0].left, l[0].width, l[1].left, l[1].width, l[0].top, l[1].top, u.left, u.right, u.top, u.bottom,"
                + " cl.length, cl[0].width, cl[0].left].join(','));", rects);
        String[] p = result.split(",");
        double adv = rects[0].advance(0, 11);
        double left = rects[0].left(0);
        Check.equal("2", p[0], "two rectangles: the tail of the first block and the head of the second");
        Check.near(left + 6 * adv, Double.parseDouble(p[1]), 0.8, "the first one starts before 'world'");
        Check.near(5 * adv, Double.parseDouble(p[2]), 0.8, "'world' is five characters");
        Check.near(rects[0].left(1), Double.parseDouble(p[3]), 0.8, "the second starts at the start of the second block");
        Check.near(3 * adv, Double.parseDouble(p[4]), 0.8, "three characters of 'second'");
        Check.isTrue(Double.parseDouble(p[6]) > Double.parseDouble(p[5]) + 5, "the second is below the first");
        Check.near(Math.min(Double.parseDouble(p[1]), Double.parseDouble(p[3])), Double.parseDouble(p[7]), 0.01, "the union starts at the leftmost edge");
        Check.near(Double.parseDouble(p[5]), Double.parseDouble(p[9]), 0.01, "the union starts at the top of the first");
        Check.equal("1", p[11], "a collapsed range reports one rectangle");
        Check.equal("0", p[12], "of zero width");
        Check.near(left + 4 * adv, Double.parseDouble(p[13]), 0.8, "at the caret");
    }

    /**
     * An element that wraps over lines reports one rectangle per line box from <code>getClientRects()</code>, and
     * <code>getBoundingClientRect()</code> is a <code>DOMRect</code> that is a new object on every call.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testElementClientRectsOnePerLine() throws Exception {
        Rects[] rects = new Rects[1];
        String result = run("var d = document.createElement('div'); d.style.cssText = 'width:60px'; d.innerHTML = '<b id=w>aaa bbb ccc ddd eee fff</b>';"
                + " document.body.appendChild(d); var w = document.getElementById('w'); var l = w.getClientRects(); var u = w.getBoundingClientRect();"
                + " var n = document.createElement('i'); n.style.display = 'none'; document.body.appendChild(n);"
                + " sessionStorage.setItem('geo', [l.length > 1, l[0].top < l[1].top, u.height >= l[1].bottom - l[0].top - 1, w.getBoundingClientRect() !== u,"
                + " n.getClientRects().length, n.getBoundingClientRect().width].join(','));", rects);
        Check.equal("true,true,true,true,0,0", result, "per-line rectangles, a new DOMRect every call, none for display:none");
    }

    /**
     * The geometry interfaces work as constructors: the edges follow the origin and a negative size, the read-only variant has no setters,
     * <code>fromRect()</code>, <code>toJSON()</code> and the points.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGeometryInterfaces() throws Exception {
        Rects[] rects = new Rects[1];
        String result = run("var rc = new DOMRect(10, 20, -4, 6); var ro = new DOMRectReadOnly(1, 2, 3, 4); rc.x = 50;"
                + " var out = [rc.x, rc.y, rc.width, rc.height, rc.left, rc.right, rc.top, rc.bottom, rc instanceof DOMRectReadOnly, ro instanceof DOMRect,"
                + " Object.getPrototypeOf(DOMRect) === DOMRectReadOnly];"
                + " try { 'use strict'; ro.x = 9; } catch (e) { out.push('threw'); } out.push(ro.x);"
                + " var f = DOMRect.fromRect({ x: 1, y: 2, width: 3, height: 4 }); out.push(f.right, f.bottom, f instanceof DOMRect);"
                + " var j = JSON.parse(JSON.stringify(new DOMRect(1, 2, 3, 4))); out.push(j.x, j.width, j.right, j.bottom);"
                + " var p = new DOMPoint(1, 2); out.push(p.x, p.y, p.z, p.w, p instanceof DOMPointReadOnly); p.w = 5; out.push(p.w);"
                + " var q = DOMPoint.fromPoint({ x: 7, y: 8, z: 9, w: 2 }); out.push(q.x, q.y, q.z, q.w);"
                + " var z = new DOMRect(); out.push(z.x, z.width);"
                + " sessionStorage.setItem('geo', out.join(','));", rects);
        Check.equal("50,20,-4,6,46,50,20,26,true,false,true," + "9,".substring(0, 0) + "1,4,6,true,1,3,4,6,1,2,0,1,true,5,7,8,9,2,0,0",
                result.replace("threw,", ""), "rectangle edges, read-only variant, fromRect, toJSON, points");
    }

    /**
     * <code>Selection.toString()</code> gives the text that copying gives: blocks are separated by an empty line, text that cannot be
     * selected and the text of scripts are left out.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSelectionToStringIsTheRenderedText() throws Exception {
        Rects[] rects = new Rects[1];
        String result = run("var s = window.getSelection(); s.selectAllChildren(document.body);"
                + " var all = s.toString(); var rg = document.createRange(); rg.selectNodeContents(document.body);"
                + " sessionStorage.setItem('geo', JSON.stringify([all, rg.toString().indexOf('setTimeout') >= 0, all.indexOf('setTimeout') >= 0, all.indexOf('locked')]));", rects);
        Check.contains(result, "hello world\\n\\nsecond line", "rendered text with the blocks separated");
        Check.contains(result, "true,false,-1", "Range.toString keeps the raw data (script text), Selection.toString leaves out script and user-select:none text");
    }
}
