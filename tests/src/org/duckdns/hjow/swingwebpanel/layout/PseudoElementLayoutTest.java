package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the generated content and the pseudo-elements (plan item P10-04, CSS Lists Level 3 counters, CSS Pseudo-Elements Level 4):
 * <code>counter-reset</code>, <code>counter-increment</code>, <code>counter-set</code> with <code>counter()</code> and
 * <code>counters()</code> in <code>::before</code> and <code>::after</code> (scopes of siblings and nested elements, counter styles,
 * counters in a flex container), <code>::first-letter</code> (the letter with its punctuation, the style, the float drop cap, the source
 * offsets of the rest of the text), and <code>::marker</code> (the color and the content of list markers).
 * <p>
 * 생성 콘텐츠와 의사 요소 시험(계획 P10-04, CSS Lists Level 3 카운터, CSS Pseudo-Elements Level 4): <code>::before</code>, <code>::after</code> 의
 * <code>counter-reset</code>, <code>counter-increment</code>, <code>counter-set</code> 과 <code>counter()</code>, <code>counters()</code>(형제와 중첩 요소의 범위, 카운터 스타일,
 * 플렉스 컨테이너 안의 카운터), <code>::first-letter</code>(문장 부호가 붙은 글자, 스타일, float 드롭 캡, 나머지 텍스트의 원본 위치), <code>::marker</code>(목록 표식의 색과 content).
 */
public class PseudoElementLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PseudoElementLayoutTest() {
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
     * A section counter and a nested subsection counter number the headings (the classic <code>1.</code>, <code>1.1</code>).
     * <p>
     * 절 카운터와 중첩된 소절 카운터가 제목에 번호를 매긴다(전형적인 <code>1.</code>, <code>1.1</code>).
     */
    public void testHeadingCounters() {
        LayoutResult r = LayoutTestSupport.layout("<style>body{counter-reset:sec} h2{counter-increment:sec;counter-reset:sub} h3{counter-increment:sub}"
                + "h2::before{content:counter(sec) '. '} h3::before{content:counter(sec) '.' counter(sub) ' '}</style>"
                + "<h2 id=a>A</h2><h3 id=a1>x</h3><h3 id=a2>y</h3><h2 id=b>B</h2><h3 id=b1>z</h3>", 400);
        Check.equal("1. A", firstLine(r, "a"), "first section");
        Check.equal("1.1 x", firstLine(r, "a1"), "first subsection");
        Check.equal("1.2 y", firstLine(r, "a2"), "second subsection");
        Check.equal("2. B", firstLine(r, "b"), "second section: the sub counter was reset");
        Check.equal("2.1 z", firstLine(r, "b1"), "its first subsection");
    }

    /**
     * <code>counters()</code> joins the values of nested counters with the separator; an element that has no counter shows 0; the
     * counters of the children of an element are out of scope after it.
     * <p>
     * <code>counters()</code> 는 중첩된 카운터의 값을 구분자로 잇는다. 카운터가 없는 요소는 0 을 보이고, 요소의 자식이 만든 카운터는 그 요소 뒤에서 범위를 벗어난다.
     */
    public void testNestedCountersAndScope() {
        LayoutResult r = LayoutTestSupport.layout("<style>ol,ul{counter-reset:item} li{counter-increment:item} li::before{content:counters(item,'.') ' '}"
                + "#x::before{content:counter(inner)} #y::before{content:counter(sib)}</style>"
                + "<ol><li id=l1><span>a</span><ol><li id=l11>b</li><li id=l12>c</li></ol></li><li id=l2>d</li></ol>"
                + "<div><i style='counter-reset:inner 5'>.</i></div><p id=x>q</p>"
                + "<div style='counter-reset:sib 7'></div><p id=y>r</p>", 400);
        Check.equal("1 a", firstLine(r, "l1"), "outer item 1");
        Check.equal("1.1 b", firstLine(r, "l11"), "nested item 1.1");
        Check.equal("1.2 c", firstLine(r, "l12"), "nested item 1.2");
        Check.equal("2 d", firstLine(r, "l2"), "outer item 2: the nested counter is out of scope");
        Check.equal("0q", firstLine(r, "x"), "a counter that was reset in a child of a sibling is not in scope: 0");
        Check.equal("7r", firstLine(r, "y"), "a counter that was reset by a preceding sibling is in scope for the following siblings");
    }

    /**
     * <code>counter-set</code>, a negative increment, counter styles and <code>::after</code>.
     * <p>
     * <code>counter-set</code>, 음수 증가, 카운터 스타일, <code>::after</code>.
     */
    public void testCounterSetAndStyles() {
        LayoutResult r = LayoutTestSupport.layout("<style>#a{counter-set:c 3} #a::before{content:counter(c,upper-roman)} #a::after{content:counter(c,lower-alpha)}"
                + "#b{counter-reset:d 10;counter-increment:d -4} #b::before{content:counter(d) counter(d,decimal-leading-zero)}</style>"
                + "<p id=a>x</p><p id=b>y</p>", 400);
        Check.equal("IIIxc", firstLine(r, "a"), "counter-set 3: upper-roman before the text, lower-alpha after it");
        Check.equal("606y", firstLine(r, "b"), "reset to 10, incremented by -4: 6, and in decimal-leading-zero 06");
    }

    /**
     * The counters of a flex container's pseudo-elements work like those of a block.
     * <p>
     * 플렉스 컨테이너의 의사 요소의 카운터는 블록의 것과 같다.
     */
    public void testCountersInFlexContainer() {
        LayoutResult r = LayoutTestSupport.layout("<style>.f{display:flex;counter-increment:n} .f::before{content:counter(n)}</style>"
                + "<div class=f id=f1><span>a</span></div><div class=f id=f2><span>b</span></div>", 400);
        Check.equal("1", LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(r, "f1")).get(0)), "first flex container");
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "f2"), LayoutBox.Type.TEXT, t);
        Check.equal("2", t.get(0).getText(), "second flex container");
    }

    /**
     * <code>::first-letter</code> styles the first letter with the style of the pseudo-element; the punctuation around it belongs to it;
     * the rest of the text keeps the style of the paragraph and its source offsets.
     * <p>
     * <code>::first-letter</code> 는 첫 글자를 의사 요소의 스타일로 꾸민다. 글자를 둘러싼 문장 부호도 포함된다. 나머지 텍스트는 문단의 스타일과 원본 위치를 유지한다.
     */
    public void testFirstLetter() {
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-letter{color:#f00;font-size:200%}</style><p id=p>Hello world</p><p id=q>\"Quoted\" text</p>", 400);
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "p"), LayoutBox.Type.TEXT, t);
        Check.equal("H", t.get(0).getText(), "the first letter is a fragment of its own");
        Check.equal(0xFFFF0000, t.get(0).getStyle().color(Prop.COLOR), "in the color of ::first-letter");
        Check.near(20, t.get(0).getFont().getSize2D(), 0.01, "twice the font size");
        Check.near(10, t.get(1).getFont().getSize2D(), 0.01, "the rest keeps the font size");
        Check.equal(0, t.get(0).getSourceStart(), "the letter starts at offset 0 of the text node");
        Check.equal("ello ", t.get(1).getText(), "the rest starts after the letter (up to the next break opportunity)");
        Check.equal(1, t.get(1).getSourceStart(), "and its source offset skips the letter");
        Check.near(t.get(0).getX() + t.get(0).getWidth(), t.get(1).getX(), 0.5, "the rest follows the letter on the line");
        List<LayoutBox> q = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "q"), LayoutBox.Type.TEXT, q);
        Check.equal("\"Q", q.get(0).getText(), "the opening quote belongs to the first letter");
        Check.equal(0xFFFF0000, q.get(0).getStyle().color(Prop.COLOR), "and is styled with it");
    }

    /**
     * A floated <code>::first-letter</code> is a drop cap: the first letter is a float and the text flows beside it.
     * <p>
     * float 인 <code>::first-letter</code> 는 드롭 캡이다: 첫 글자는 float 이고 텍스트는 그 옆으로 흐른다.
     */
    public void testFirstLetterDropCap() {
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-letter{float:left;font-size:300%;margin-right:2px}</style><p id=p>Drop cap text that goes on</p>", 400);
        LayoutBox p = LayoutTestSupport.box(r, "p");
        LayoutBox cap = null;
        // 문단의 자식 중 첫 글자 float 를 찾는다.
        for (int i = 0; i < p.getChildren().size(); i++) {
            if ("first-letter".equals(p.getChildren().get(i).getPseudo())) {
                cap = p.getChildren().get(i);
            }
        }
        Check.notNull(cap, "the first letter is a float box of its own");
        Check.isTrue(cap.isFloat(), "it floats");
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(p, LayoutBox.Type.TEXT, t);
        Check.equal("D", t.get(0).getText(), "the letter is text of the float");
        Check.isTrue(t.get(1).getX() >= cap.getX() + cap.getWidth() - 0.01, "the rest of the first line is right of the float: " + t.get(1).getX() + " vs " + (cap.getX() + cap.getWidth()));
    }

    /**
     * Without a first letter (only white space) or when the first thing is an atomic box, nothing is styled.
     * <p>
     * 첫 글자가 없거나(공백뿐) 처음이 원자적 상자이면 아무것도 꾸미지 않는다.
     */
    public void testFirstLetterNotFound() {
        LayoutResult r = LayoutTestSupport.layout("<style>p::first-letter{color:#f00}</style><p id=p><span style='display:inline-block;width:5px;height:5px'></span>Hello</p>", 400);
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "p"), LayoutBox.Type.TEXT, t);
        Check.equal("Hello", t.get(0).getText(), "text after an inline-block is not the first letter");
        Check.isTrue(t.get(0).getStyle().color(Prop.COLOR) != 0xFFFF0000, "and is not styled");
    }

    /**
     * <code>::marker</code> styles the marker of a list item: the color and the <code>content</code> replace the default marker.
     * <p>
     * <code>::marker</code> 는 목록 항목의 표식을 꾸민다: 색과 <code>content</code> 가 기본 표식을 대체한다.
     */
    public void testMarker() {
        LayoutResult r = LayoutTestSupport.layout("<style>li::marker{color:#f00} #b::marker{content:'>> '} #c{list-style-position:inside} #c::marker{color:#00f}</style>"
                + "<ul><li id=a>a</li><li id=b>b</li><li id=c>c</li></ul>", 400);
        List<LayoutBox> ma = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "a"), LayoutBox.Type.MARKER, ma);
        Check.equal(1, ma.size(), "one marker");
        Check.equal(0xFFFF0000, ma.get(0).getStyle().color(Prop.COLOR), "the marker is red");
        List<LayoutBox> mb = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "b"), LayoutBox.Type.MARKER, mb);
        Check.equal(">> ", mb.get(0).getText(), "content replaces the marker text");
        List<LayoutBox> tc = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, "c"), LayoutBox.Type.TEXT, tc);
        Check.equal(0xFF0000FF, tc.get(0).getStyle().color(Prop.COLOR), "the inside marker is text in the color of ::marker");
    }
}
