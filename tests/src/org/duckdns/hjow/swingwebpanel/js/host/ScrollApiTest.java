package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the scrolling API of the page (plan items P20-01, P20-12): the arguments of <code>scroll</code>, <code>scrollTo</code> and
 * <code>scrollBy</code> of the window and of the scrolling element, <code>scrollX</code>/<code>scrollY</code>, <code>scrollIntoView</code> with its
 * alignments and <code>document.scrollingElement</code>. The environment of the fixture records the requests; the real viewport is tested in the
 * component test.
 * <p>
 * 페이지의 스크롤 API 시험(계획 P20-01, P20-12): window 와 스크롤 요소의 <code>scroll</code>, <code>scrollTo</code>, <code>scrollBy</code> 인수,
 * <code>scrollX</code>/<code>scrollY</code>, 정렬이 있는 <code>scrollIntoView</code>, <code>document.scrollingElement</code>. 시험 도구의 환경이 요청을
 * 기록하며 실제 뷰포트는 컴포넌트 시험에서 확인한다.
 */
public class ScrollApiTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScrollApiTest() {
    }

    /**
     * The window methods: a pair of numbers, an options object, relative scrolling and the replacement of values that are not finite.
     * <p>
     * window 메서드: 숫자 쌍, 옵션 객체, 상대 스크롤, 유한하지 않은 값의 대체.
     */
    public void testWindowScroll() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        f.scripts.evaluate("scrollTo(10, 200); scroll({top: 50}); scrollBy(5, -20); scrollBy({left: 3}); scrollTo(NaN, Infinity); scrollTo();", "t");
        Check.equal("[10,200, 10,50, 15,30, 18,30, 0,0]", f.env.scrolls.toString(), "the requests: absolute, options with a missing axis, relative, not finite, no arguments");
        f.env.scrollX = 7;
        f.env.scrollY = 90;
        Check.equal("7,90,7,90", f.scripts.evaluate("[scrollX, scrollY, pageXOffset, pageYOffset].join()", "t"), "the position that the environment reports");
    }

    /**
     * The scrolling element is the root element in standards mode and <code>body</code> in quirks mode; only it scrolls the viewport.
     * <p>
     * 스크롤 요소는 표준 모드에서 루트 요소이고 호환(quirks) 모드에서 <code>body</code> 이다. 그것만 뷰포트를 스크롤한다.
     */
    public void testScrollingElement() {
        PageFixture f = new PageFixture().load("<!doctype html><body><p id=p>x</p>");
        f.env.scrollY = 40;
        Check.equal("true,40,0", f.scripts.evaluate("var h = document.documentElement; [document.scrollingElement === h, h.scrollTop, "
                + "document.body.scrollTop].join()", "t"), "standards mode: the root element");
        f.scripts.evaluate("document.documentElement.scrollTop = 300; document.getElementById('p').scrollTop = 5; document.documentElement.scrollLeft = 8;"
                + "document.documentElement.scrollBy(1, 2);", "t");
        Check.equal("[0,300, 8,300, 9,302]", f.env.scrolls.toString(), "only the root element scrolls the viewport");
        PageFixture q = new PageFixture().load("<body><p id=p>x</p>");
        q.env.scrollY = 25;
        Check.equal("true,25,0", q.scripts.evaluate("[document.scrollingElement === document.body, document.body.scrollTop, "
                + "document.documentElement.scrollTop].join()", "t"), "quirks mode: the body element");
    }

    /**
     * <code>scrollIntoView</code>: <code>start</code> (the default and <code>true</code>), <code>end</code> (<code>false</code>),
     * <code>center</code> and <code>nearest</code>.
     * <p>
     * <code>scrollIntoView</code>: <code>start</code>(기본과 <code>true</code>), <code>end</code>(<code>false</code>), <code>center</code>,
     * <code>nearest</code>.
     */
    public void testScrollIntoView() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        // 뷰포트는 800x600 이고 스크롤 위치는 (0, 100), 요소는 뷰포트 좌표 (20, 300) 크기 60x40 이다.
        f.env.bounds.put("a", new double[] { 20, 300, 60, 40 });
        String[] calls = { "a.scrollIntoView()", "a.scrollIntoView(true)", "a.scrollIntoView(false)",
            "a.scrollIntoView({block: 'center', inline: 'center'})", "a.scrollIntoView({block: 'nearest'})",
            "a.scrollIntoView({block: 'end', inline: 'start'})" };
        String[] expected = { "0,400", "0,400", "0,-160", "-350,120", "0,100", "20,-160" };
        String[] why = { "the default is start: the top of the element at the top of the viewport (100 + 300)", "true is start",
            "false is end: the bottom at the bottom (100 + 300 + 40 - 600)", "center on both axes", "nearest: visible already, nothing moves",
            "end on the block axis and start on the inline axis" };
        // 호출마다 스크롤 위치를 처음으로 되돌리고 요청이 기대한 위치인지 확인한다.
        for (int i = 0; i < calls.length; i++) {
            f.env.scrolls.clear();
            f.env.scrollX = 0;
            f.env.scrollY = 100;
            f.scripts.evaluate("var a = document.getElementById('a'); " + calls[i], "t");
            Check.equal(expected[i], f.env.scrolls.size() == 1 ? f.env.scrolls.get(0) : f.env.scrolls.toString(), why[i]);
        }
        // 요소가 뷰포트 위쪽에 가려져 있으면 nearest 는 시작에 맞춘다.
        f.env.bounds.put("a", new double[] { 20, -50, 60, 40 });
        f.env.scrolls.clear();
        f.env.scrollX = 0;
        f.env.scrollY = 100;
        f.scripts.evaluate("document.getElementById('a').scrollIntoView({block: 'nearest'})", "t");
        Check.equal("0,50", f.env.scrolls.get(0), "nearest: an element above the viewport is put at the top (100 - 50)");
    }
}
