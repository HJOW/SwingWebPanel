package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.selector.ElementState;
import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.css.selector.PseudoClasses;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorMatcher;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * HTML5 2014 section 10.3.2 body link/vlink/alink hints: state, cascade, mutation invalidation and rendered colors (P07-07-b-1).
 * <p>HTML5 2014 10.3.2의 body link/vlink/alink 힌트: 상태·캐스케이드·변경 무효화·실제 색 그리기 시험.
 */
public class LegacyLinkHintsTest {
    /** Parsed document owned by the test. / 시험이 소유하는 파싱 문서. */
    private Document document;
    /** CSS engine released after each case. / 각 사례 뒤 해제하는 CSS 엔진. */
    private CssEngine css;
    /** Test-controlled dynamic link state. / 시험이 제어하는 동적 링크 상태. */
    private LinkState state;

    /** Creates the suite. / 시험 스위트를 만든다. */
    public LegacyLinkHintsTest() {
    }

    /** A deliberately broad state provider, testing that only hyperlinks match :visited. / 하이퍼링크만 :visited에 일치하는지 시험하도록 넓게 답하는 상태 제공자. */
    private static final class LinkState implements ElementState {
        /** Current visited flag. / 현재 방문 표시. */
        boolean visited;
        /** Current active flag. / 현재 활성 표시. */
        boolean active;
        /** Creates unvisited/inactive state. / 미방문·비활성 상태를 만든다. */
        LinkState() {
        }
        /** Answers link states for all elements; the matcher must still enforce hyperlink eligibility. / 모든 요소에 상태를 답하며 매처가 하이퍼링크 적격성을 확인해야 한다.
         * @param element candidate / 후보
         * @param pseudo pseudo-class ID / 의사 클래스 ID
         * @return tri-state answer / 세 상태 응답 */
        public int get(Element element, int pseudo) {
            if (pseudo == PseudoClasses.VISITED) {
                return visited ? TRUE : FALSE;
            }
            if (pseudo == PseudoClasses.ACTIVE) {
                return active ? TRUE : FALSE;
            }
            return UNKNOWN;
        }
    }

    /**
     * Parses real HTML and installs the actual CSS engine with dynamic state.
     * <p>실제 HTML을 파싱하고 동적 상태와 실제 CSS 엔진을 설치한다.
     * @param bodyAttributes body attributes / body 속성
     * @param markup body content / 본문 내용
     * @param rules author stylesheet / 작성자 시트
     */
    private void init(String bodyAttributes, String markup, String rules) {
        document = HtmlParser.parseDocument("<!doctype html><html><head><style>" + rules + "</style></head><body "
                + bodyAttributes + ">" + markup + "</body></html>", "http://legacy.test/", null, true);
        state = new LinkState();
        css = new CssEngine(document, null, new MediaEnvironment(200, 120));
        css.loadStyleSheets();
        css.getResolver().setElementState(state);
    }

    /** Releases document listeners after each test. / 각 시험 후 문서 리스너를 해제한다. */
    public void tearDown() {
        if (css != null) {
            css.dispose();
        }
    }

    /** Returns an element's computed color. / 요소의 계산 색을 반환한다.
     * @param id element ID / 요소 ID
     * @return RGB text / RGB 문자열 */
    private String color(String id) {
        return css.getComputedStyle(document.getElementById(id)).getPropertyValue("color");
    }

    /**
     * Unvisited, visited and active hints replace one another and inherited child color follows state invalidation.
     * <p>미방문·방문·활성 힌트가 서로 대체되고 자식의 상속 색도 상태 무효화를 따른다.
     */
    public void testStatesAndInheritance() {
        init("text='#123456' link=red vlink=green alink=blue", "<a id=a href=one><span id=child>text</span></a><a id=plain>plain</a>", "");
        Check.equal("rgb(255, 0, 0)", color("a"), "unvisited hint");
        Check.equal("rgb(255, 0, 0)", color("child"), "child inherits link color");
        Check.equal("rgb(18, 52, 86)", color("plain"), "anchor without href uses body text color");
        state.visited = true;
        css.getResolver().stateChanged(document.getElementById("a"));
        Check.equal("rgb(0, 128, 0)", color("a"), "visited hint");
        Check.equal("rgb(0, 128, 0)", color("child"), "cached descendant invalidated");
        state.active = true;
        css.getResolver().stateChanged(document.getElementById("a"));
        Check.equal("rgb(0, 0, 255)", color("a"), "active hint wins");
        state.active = false;
        css.getResolver().stateChanged(document.getElementById("a"));
        Check.equal("rgb(0, 128, 0)", color("a"), "active release restores visited hint");
    }

    /**
     * Author rules and inline declarations outrank hints of specificity zero, even for active links.
     * <p>작성자 규칙과 인라인 선언이 활성 링크에서도 구체성 0의 힌트보다 우선한다.
     */
    public void testAuthorCascadeWins() {
        init("link=red vlink=green alink=blue", "<a id=a href=one>one</a><a id=b href=two style='color:#112233'>two</a>", "a{color:purple}");
        Check.equal("rgb(128, 0, 128)", color("a"), "author type rule beats hint");
        Check.equal("rgb(17, 34, 51)", color("b"), "inline declaration wins");
        state.visited = true;
        state.active = true;
        css.getResolver().stateChanged(null);
        Check.equal("rgb(128, 0, 128)", color("a"), "author rule still wins active hint");
        Check.equal("rgb(17, 34, 51)", color("b"), "inline still wins");
    }

    /**
     * Changing body colors or replacing the body invalidates links outside the body as well as descendants.
     * <p>본문 색 변경이나 본문 교체가 후손뿐 아니라 본문 밖 링크도 무효화한다.
     */
    public void testBodyMutationAndReplacement() {
        init("link=red", "<a id=a href=one>one</a>", "");
        Element headLink = document.createElement("a");
        headLink.setAttribute("href", "head");
        headLink.setAttribute("id", "head-link");
        document.getElementsByTagName("head").item(0).appendChild(headLink);
        Check.equal("rgb(255, 0, 0)", color("head-link"), "hint applies throughout the document");
        Element body = (Element) document.getElementsByTagName("body").item(0);
        body.setAttribute("link", "blue");
        Check.equal("rgb(0, 0, 255)", color("head-link"), "body mutation invalidates head link");
        Check.equal("rgb(0, 0, 255)", color("a"), "body descendant invalidated");
        Element replacement = document.createElement("body");
        replacement.setAttribute("link", "magenta");
        document.getDocumentElement().replaceChild(replacement, body);
        Check.equal("rgb(255, 0, 255)", color("head-link"), "new body provides hint");
        replacement.removeAttribute("link");
        Check.equal("rgb(0, 0, 238)", color("head-link"), "attribute removal restores UA color");
    }

    /**
     * Empty/transparent legacy colors are ignored; :link and :visited are disjoint and never match non-links.
     * <p>빈 값·transparent 이전 색은 무시하며 :link와 :visited는 서로 배타적이고 비링크에는 일치하지 않는다.
     * @throws Exception on selector parse failure / 선택자 파싱 실패 시
     */
    public void testIgnoredHintsAndStateSelectors() throws Exception {
        init("link=transparent vlink=''", "<a id=a href=one>one</a><a id=plain>plain</a><p id=p>paragraph</p>", "");
        Check.equal("rgb(0, 0, 238)", color("a"), "invalid hint leaves UA link color");
        state.visited = true;
        css.getResolver().stateChanged(null);
        Check.equal("rgb(85, 26, 139)", color("a"), "invalid visited hint leaves UA visited color");
        MatchContext context = new MatchContext(null, state);
        Check.isTrue(!SelectorMatcher.matches(SelectorParser.parse(":link", null), document.getElementById("a"), context), "visited link is not :link");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse(":any-link", null), document.getElementById("a"), context), "visited is :any-link");
        Check.isTrue(!SelectorMatcher.matches(SelectorParser.parse(":visited", null), document.getElementById("p"), context), "non-link is not visited");
        Check.isTrue(!SelectorMatcher.matches(SelectorParser.parse(":visited", null), document.getElementById("plain"), context), "anchor without href is not visited");
    }

    /**
     * CurrentColor backgrounds prove the hint reaches actual rendered pixels without depending on font rasterization.
     * <p>currentColor 배경으로 글꼴 픽셀 모양에 의존하지 않고 힌트가 실제 그림에 도달하는지 확인한다.
     */
    public void testRenderedHintColors() {
        init("link=red vlink=green alink=blue", "<a id=a href=one></a>",
                "html,body{margin:0;padding:0}a{display:block;width:80px;height:60px;background:currentColor}");
        Check.equal(0xff0000, paintedColor(), "rendered unvisited color");
        state.visited = true;
        css.getResolver().stateChanged(null);
        Check.equal(0x008000, paintedColor(), "rendered visited color");
        state.active = true;
        css.getResolver().stateChanged(null);
        Check.equal(0x0000ff, paintedColor(), "rendered active color");
    }

    /** Renders and samples a point inside the empty anchor. / 빈 앵커 내부 점을 렌더링하고 확인한다.
     * @return sampled RGB / 샘플 RGB */
    private int paintedColor() {
        RenderedPage page = PageRenderer.render(document, css, null, 200, 120);
        BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            page.paint(graphics, null, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(20, 20) & 0xffffff;
    }
}
