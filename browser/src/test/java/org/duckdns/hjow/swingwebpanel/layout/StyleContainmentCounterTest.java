package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of style containment (<code>contain: style</code>, plan item P10-08, CSS Containment Level 2, section 3.3, https://www.w3.org/TR/css-contain-2/#containment-style): the
 * <code>counter-increment</code> and <code>counter-set</code> inside the subtree of the element are scoped to it. The first use of a counter in the subtree starts a new
 * counter at 0 whatever the outside has, the increments do not leak out, and <code>counter()</code> and <code>counters()</code> still see the counters of the outside.
 * <p>
 * 스타일 격리(<code>contain: style</code>, 계획 P10-08, CSS Containment Level 2 3.3 절) 시험: 요소의 하위 트리 안의 <code>counter-increment</code> 와 <code>counter-set</code> 은 그 하위
 * 트리로 한정된다. 하위 트리에서 카운터를 처음 쓰면 바깥이 어떻든 0 에서 시작하는 새 카운터가 만들어지고, 증가는 바깥으로 새지 않으며, <code>counter()</code> 와 <code>counters()</code>
 * 는 여전히 바깥의 카운터를 본다.
 */
public class StyleContainmentCounterTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StyleContainmentCounterTest() {
    }

    /**
     * Returns the text of the first line of an element.
     * <p>
     * 요소의 첫 줄의 텍스트를 반환한다.
     *
     * @param r  the layout / 레이아웃
     * @param id the id / id
     * @return the text / 텍스트
     */
    private static String firstLine(LayoutResult r, String id) {
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, id));
        Check.isFalse(lines.isEmpty(), "#" + id + " has a line");
        return LayoutTestSupport.lineText(lines.get(0));
    }

    /**
     * The example of the specification: the element increments <code>n</code> itself (not scoped) and its <code>::after</code> increments it inside the subtree, so that
     * a new counter nests in the one of the element: <code>1</code> before, <code>1.2</code> after.
     * <p>
     * 규격의 예: 요소가 <code>n</code> 을 스스로 증가시키고(한정되지 않음) 그 <code>::after</code> 는 하위 트리 안에서 증가시키므로 요소의 카운터 안에 새 카운터가 중첩된다.
     * 앞은 <code>1</code>, 뒤는 <code>1.2</code>.
     */
    public void testSpecificationExample() {
        LayoutResult r = LayoutTestSupport.layout("<style>#a{contain:style;counter-increment:n;margin:0} #a::before,#a::after{content:counters(n,'.') ' '} #a::after{counter-increment:n 2}</style>"
                + "<div id=a></div>", 400);
        Check.equal("1 1.2", firstLine(r, "a").trim(), "1 before the increment of ::after and 1.2 after it");
    }

    /**
     * An increment inside the subtree starts a new counter at 0 even though the outside has one, and it does not change the outer counter: after the element the outer
     * counter has its old value. Without containment the outer counter is incremented.
     * <p>
     * 하위 트리 안의 증가는 바깥에 카운터가 있어도 0 에서 시작하는 새 카운터를 만들고 바깥 카운터를 바꾸지 않는다: 요소 뒤에서 바깥 카운터는 이전 값이다. 격리가 없으면 바깥 카운터가 증가한다.
     */
    public void testIncrementsDoNotLeakOut() {
        String css = "<style>body{counter-reset:n 5} p{margin:0} .c{contain:style} i{counter-increment:n} p::before{content:counter(n)}</style>";
        LayoutResult free = LayoutTestSupport.layout(css + "<div><i></i><i></i></div><p id=a></p>", 400);
        Check.equal("7", firstLine(free, "a"), "without containment the outer counter counts to 7");
        LayoutResult contained = LayoutTestSupport.layout(css + "<div class=c><i></i><i></i></div><p id=a></p>", 400);
        Check.equal("5", firstLine(contained, "a"), "with containment the outer counter is unchanged after the element");
    }

    /**
     * Inside the element the counter starts at 0 and counts up from there (the outer value is not the start); the increments of one name go on in that new counter, and
     * <code>counter()</code> reads the outer counter where the subtree has not started a counter of that name.
     * <p>
     * 요소 안에서 카운터는 0 에서 시작해 올라간다(바깥 값이 시작점이 아니다). 안에서 리셋한 카운터는 평소처럼 동작하고, 하위 트리가 그 이름의 카운터를 시작하지 않은 곳에서
     * <code>counter()</code> 는 바깥 카운터를 읽는다.
     */
    public void testInnerCounterStartsAtZero() {
        String css = "<style>body{counter-reset:n 5 m 7} p{margin:0} .c{contain:style} i{counter-increment:n} b{counter-increment:m}"
                + "p::before{content:counter(n) '/' counter(m)}</style>";
        LayoutResult r = LayoutTestSupport.layout(css + "<div class=c><i></i><p id=a></p><i></i><p id=b></p></div>", 400);
        Check.equal("1/7", firstLine(r, "a"), "the first increment inside makes a new n at 1 (not 6); counter(m) still reads the outer m");
        Check.equal("2/7", firstLine(r, "b"), "the second increment inside counts on");
        String reset = "<style>p{margin:0} .c{contain:style} i{counter-increment:n} p::before{content:counter(n)} body{counter-reset:n 5}</style>";
        LayoutResult s = LayoutTestSupport.layout(reset + "<p id=o></p><div class=c style='counter-reset:n 10'><i></i><p id=a></p></div>", 400);
        Check.equal("5", firstLine(s, "o"), "before the element the outer counter shows 5");
        Check.equal("1", firstLine(s, "a"), "even a counter that the element resets itself is outside: the first increment inside starts a new counter at 0");
    }

    /**
     * The counter properties of a pseudo-element apply before its own <code>content</code> is evaluated (CSS Lists 3, 4.4 and the example of CSS Containment 2):
     * <code>::before { counter-increment: n; content: counter(n) }</code> shows the incremented value.
     * <p>
     * 의사 요소의 카운터 속성은 자신의 <code>content</code> 를 평가하기 전에 적용된다(CSS Lists 3 4.4 와 CSS Containment 2 의 예): <code>::before { counter-increment: n; content: counter(n) }</code>
     * 은 증가한 값을 보인다.
     */
    public void testPseudoCountersApplyBeforeItsContent() {
        LayoutResult r = LayoutTestSupport.layout("<style>body{counter-reset:n} p{margin:0} p::before{counter-increment:n;content:counter(n)}</style><p id=a></p><p id=b></p>", 400);
        Check.equal("1", firstLine(r, "a"), "the first pseudo-element shows 1");
        Check.equal("2", firstLine(r, "b"), "the second shows 2");
    }

    /**
     * Without style containment the counters of an element behave as before (a regression check of the heading counters of the layout tests of generated content).
     * <p>
     * 스타일 격리가 없으면 요소의 카운터는 이전처럼 동작한다(생성 콘텐츠 레이아웃 시험의 제목 카운터를 확인하는 회귀 시험).
     */
    public void testNoContainmentIsUnchanged() {
        LayoutResult r = LayoutTestSupport.layout("<style>body{counter-reset:sec} h2{counter-increment:sec} h2::before{content:counter(sec) '. '}</style><h2 id=a>A</h2><h2 id=b>B</h2>", 400);
        Check.equal("1. A", firstLine(r, "a"), "first");
        Check.equal("2. B", firstLine(r, "b"), "second");
    }
}
