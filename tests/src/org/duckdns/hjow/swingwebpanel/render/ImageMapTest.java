package org.duckdns.hjow.swingwebpanel.render;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the client-side image maps and the server-side image maps (plan item P07-07-c, LEG-07, HTML5 REC 2014-10-28 section 4.7.13 and
 * section 4.5.1): the lookup of the <code>map</code> of a <code>usemap</code>, the list of integers of <code>coords</code>, the shapes,
 * their layering, the hit test of the image that gives the point to the <code>area</code> that covers it (also under transforms and in the
 * scrolled fixed layer), and the suffix of the server-side map.
 * <p>
 * 클라이언트 쪽과 서버 쪽 이미지 맵 시험(계획 P07-07-c, LEG-07, HTML5 REC 2014-10-28 §4.7.13, §4.5.1): <code>usemap</code> 의 <code>map</code> 찾기,
 * <code>coords</code> 의 정수 목록, 모양, 겹침, 점을 덮는 <code>area</code> 에 점을 주는 이미지의 히트 테스트(변환과 스크롤된 fixed 층 포함), 서버 쪽 맵의 접미사.
 */
public class ImageMapTest {

    /** The base URL of the pages. / 페이지의 기준 URL. */
    private static final String BASE = "http://t.test/dir/page.html";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageMapTest() {
    }

    /**
     * Parses a body into a document.
     * <p>
     * body 를 문서로 해석한다.
     *
     * @param body the markup / 마크업
     * @return the document / 문서
     */
    private static Document doc(String body) {
        return HtmlParser.parseDocument("<!doctype html><body>" + body, BASE, null, false);
    }

    /**
     * Renders a page with a loaded image of 20 x 10 pixels (<code>ok.png</code>).
     * <p>
     * 20 x 10 화소로 읽은 이미지(<code>ok.png</code>)가 있는 페이지를 렌더링한다.
     *
     * @param body the body markup / body 마크업
     * @return the page / 페이지
     */
    private static RenderedPage page(String body) {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/ok.png",
                ImageResource.loaded(RenderTestSupport.solid(20, 10, 0xFFFF0000)));
        Document d = HtmlParser.parseDocument("<!doctype html><style>html,body{margin:0;padding:0} body{font:10px monospace;line-height:normal}</style><body>" + body,
                BASE, null, false);
        CssEngine css = new CssEngine(d, null, new MediaEnvironment(400, 300));
        css.loadStyleSheets();
        RenderTestSupport.lastDocument = d;
        return PageRenderer.render(d, css, images, 400, 300);
    }

    /**
     * The map of a <code>usemap</code> is the first map in tree order whose id equals the name after the "#" or whose name equals it
     * without regard to case; values without a name after the "#" name no map.
     * <p>
     * <code>usemap</code> 의 맵은 "#" 뒤의 이름과 id 가 같거나 name 이 대소문자 무시로 같은 트리 순서의 첫 맵이다. "#" 뒤에 이름이 없는 값은 맵을 가리키지 않는다.
     */
    public void testFindMap() {
        Document d = doc("<map id=a name=other></map><map name=Shapes id=s1></map><map name=shapes id=s2></map><map id=x></map>");
        Check.equal("a", ImageMaps.findMap(d, "#a").getAttribute("id"), "by id");
        Check.equal("s1", ImageMaps.findMap(d, "#shapes").getAttribute("id"), "by name, case-insensitive, the first in tree order");
        Check.equal("s1", ImageMaps.findMap(d, "#Shapes").getAttribute("id"), "by name, the same case");
        Check.equal("a", ImageMaps.findMap(d, "#other").getAttribute("id"), "by the name of the first map");
        Check.equal("x", ImageMaps.findMap(d, "other.html#x").getAttribute("id"), "the text after the first # (the part before it is not looked at)");
        Check.isNull(ImageMaps.findMap(d, "#A"), "an id is compared case-sensitively");
        Check.isNull(ImageMaps.findMap(d, "shapes"), "no # names no map");
        Check.isNull(ImageMaps.findMap(d, "#"), "a # at the end names no map");
        Check.isNull(ImageMaps.findMap(d, ""), "the empty value");
        Check.isNull(ImageMaps.findMap(d, null), "null");
        Check.isNull(ImageMaps.findMap(d, "#none"), "no such map");
    }

    /**
     * The list of integers of HTML5 2014 section 2.4.4.5: separators, signs, text after a number and the field without digits.
     * <p>
     * HTML5 2014 §2.4.4.5 의 정수 목록: 구분자, 부호, 숫자 뒤의 글자, 숫자 없는 필드.
     */
    public void testParseIntegers() {
        Check.equal("[1, 2, 3, 4]", java.util.Arrays.toString(ImageMaps.parseIntegers("1,2,3,4")), "commas");
        Check.equal("[1, 2, 3]", java.util.Arrays.toString(ImageMaps.parseIntegers(" 1 2 ; 3 ")), "spaces and semicolons");
        Check.equal("[-5, 7]", java.util.Arrays.toString(ImageMaps.parseIntegers("-5,7")), "a negative number");
        Check.equal("[10, 20]", java.util.Arrays.toString(ImageMaps.parseIntegers("10px,20.5")), "the text after a number ends the number");
        Check.equal("[1, 2]", java.util.Arrays.toString(ImageMaps.parseIntegers("1,2,x,4")), "a field without digits ends the list");
        Check.equal("[1, 3]", java.util.Arrays.toString(ImageMaps.parseIntegers("1,,3")), "an empty field is skipped like a separator");
        Check.equal("[]", java.util.Arrays.toString(ImageMaps.parseIntegers("")), "empty");
        Check.equal("[]", java.util.Arrays.toString(ImageMaps.parseIntegers(null)), "null");
        Check.equal("[0]", java.util.Arrays.toString(ImageMaps.parseIntegers("0")), "zero");
        Check.equal("[2147483647]", java.util.Arrays.toString(ImageMaps.parseIntegers("99999999999")), "a huge number saturates");
    }

    /**
     * The shapes, each state with its rules for too few and too many items.
     * <p>
     * 모양: 각 상태와 항목이 모자라거나 넘칠 때의 규칙.
     */
    public void testShapes() {
        Document d = doc("<map name=m>"
                + "<area id=r shape=rect coords='10,10,50,30'>"
                + "<area id=rs shape=rectangle coords='50,30,10,10'>"
                + "<area id=rd coords='0,0,5,5,99,99'>"
                + "<area id=rf shape=rect coords='1,2,3'>"
                + "<area id=c shape=circle coords='50,50,20'>"
                + "<area id=cc shape=circ coords='50,50,0'>"
                + "<area id=c0 shape=circle coords='50,50'>"
                + "<area id=p shape=poly coords='0,0,40,0,40,40,0,40'>"
                + "<area id=p5 shape=polygon coords='0,0,40,0,40,40,0,40,7'>"
                + "<area id=pf shape=poly coords='0,0,40,0,40'>"
                + "<area id=df shape=default>"
                + "<area id=star shape=poly coords='50,0,20,95,95,35,5,35,80,95'>"
                + "<area id=bogus shape=bogus coords='0,0,9,9'>"
                + "</map>");
        Document dd = d;
        Check.isTrue(ImageMaps.contains(dd.getElementById("r"), 10, 10, 100, 100), "rect: the left top corner belongs to it");
        Check.isFalse(ImageMaps.contains(dd.getElementById("r"), 50, 20, 100, 100), "rect: the right edge does not");
        Check.isFalse(ImageMaps.contains(dd.getElementById("r"), 20, 30, 100, 100), "rect: the bottom edge does not");
        Check.isTrue(ImageMaps.contains(dd.getElementById("r"), 49.9, 29.9, 100, 100), "rect: inside");
        Check.isTrue(ImageMaps.contains(dd.getElementById("rs"), 20, 20, 100, 100), "rect with its corners in the wrong order is swapped");
        Check.isTrue(ImageMaps.contains(dd.getElementById("rd"), 3, 3, 100, 100), "the shape attribute is optional (rect), the items past the fourth are dropped");
        Check.isFalse(ImageMaps.contains(dd.getElementById("rd"), 50, 50, 100, 100), "the dropped items do not make a bigger rectangle");
        Check.isFalse(ImageMaps.contains(dd.getElementById("rf"), 2, 2, 100, 100), "rect with three items is empty");
        Check.isTrue(ImageMaps.contains(dd.getElementById("c"), 50, 70, 100, 100), "circle: the edge belongs to it");
        Check.isFalse(ImageMaps.contains(dd.getElementById("c"), 50, 71, 100, 100), "circle: outside");
        Check.isTrue(ImageMaps.contains(dd.getElementById("c"), 60, 60, 100, 100), "circle: inside");
        Check.isFalse(ImageMaps.contains(dd.getElementById("c"), 70, 70, 100, 100), "circle: the corner of the bounding box is outside");
        Check.isFalse(ImageMaps.contains(dd.getElementById("cc"), 50, 50, 100, 100), "circle with radius zero is empty");
        Check.isFalse(ImageMaps.contains(dd.getElementById("c0"), 50, 50, 100, 100), "circle with two items is empty");
        Check.isTrue(ImageMaps.contains(dd.getElementById("p"), 20, 20, 100, 100), "polygon: inside");
        Check.isFalse(ImageMaps.contains(dd.getElementById("p"), 60, 20, 100, 100), "polygon: outside");
        Check.isTrue(ImageMaps.contains(dd.getElementById("p5"), 20, 20, 100, 100), "polygon: the odd last item is dropped");
        Check.isFalse(ImageMaps.contains(dd.getElementById("pf"), 20, 20, 100, 100), "polygon with fewer than six items is empty");
        Check.isTrue(ImageMaps.contains(dd.getElementById("df"), 99, 99, 100, 100), "default: the whole image");
        Check.isFalse(ImageMaps.contains(dd.getElementById("df"), 100, 50, 100, 100), "default: not past the right edge");
        Check.isFalse(ImageMaps.contains(dd.getElementById("df"), -1, 0, 100, 100), "default: not before the left edge");
        // 별 모양은 짝홀 규칙이므로 가운데의 오각형은 구멍이다.
        Check.isFalse(ImageMaps.contains(dd.getElementById("star"), 50, 50, 100, 100), "polygon: the center of the pentagram is a hole by the even-odd rule");
        Check.isTrue(ImageMaps.contains(dd.getElementById("star"), 50, 15, 100, 100), "polygon: a tip of the star");
        Check.isTrue(ImageMaps.contains(dd.getElementById("bogus"), 4, 4, 100, 100), "an unknown shape is the rectangle state");
    }

    /**
     * The hit test gives a point of the image to the first area in tree order that covers it (the top-most shape); an area without
     * <code>href</code> catches the point, and a point outside every shape stays with the image and with the link around it.
     * <p>
     * 히트 테스트는 이미지의 점을 그 점을 덮는 트리 순서의 첫 area(맨 위 모양)에 준다. <code>href</code> 가 없는 area 도 점을 잡고, 모든 모양 밖의 점은 이미지와 이미지를
     * 감싼 링크에 남는다.
     */
    public void testHitTestUsesTheTopMostShape() {
        RenderedPage p = page("<img id=i src='ok.png' usemap='#m' style='position:absolute;left:30px;top:20px;width:100px;height:60px'>"
                + "<map name=m>"
                + "<area id=hole shape=rect coords='40,20,60,40'>"
                + "<area id=big shape=rect coords='20,10,80,50' href='big.html'>"
                + "<area id=ball shape=circle coords='20,40,10' href='ball.html'>"
                + "<area id=all shape=default href='all.html'>"
                + "</map>"
                + "<a id=w href='around.html' style='position:absolute;left:200px;top:0'><img id=j src='ok.png' usemap='#m' style='width:100px;height:60px'></a>");
        HitTester t = new HitTester(p.getLayout());
        // 이미지의 왼쪽 위가 (30, 20)이므로 이미지 좌표 (x, y)는 문서 좌표 (30 + x, 20 + y)이다.
        HitResult h = t.hit(30 + 50, 20 + 30);
        Check.equal("hole", h.getElement().getAttribute("id"), "the first area in tree order is the top-most: the hole");
        Check.isNull(h.getLink(), "the dead area is not a link");
        Check.isNull(h.getHref(), "and has no address");
        h = t.hit(30 + 30, 20 + 20);
        Check.equal("big", h.getElement().getAttribute("id"), "the rectangle");
        Check.equal("big", h.getLink().getAttribute("id"), "its link");
        Check.equal("http://t.test/dir/big.html", h.getHref(), "the address is resolved against the document");
        h = t.hit(30 + 15, 20 + 40);
        Check.equal("ball", h.getElement().getAttribute("id"), "the circle (the big rectangle starts at x = 20)");
        h = t.hit(30 + 90, 20 + 55);
        Check.equal("all", h.getElement().getAttribute("id"), "the default area catches the rest of the image");
        Check.equal("http://t.test/dir/all.html", h.getHref(), "its address");
        h = t.hit(0, 0);
        Check.isTrue(h.getElement() == null || !"area".equals(h.getElement().getLocalName()), "outside the image no area is hit");
    }

    /**
     * A point that no shape covers goes to the image and to the link around it; an image without a map or with a missing map is a plain
     * image.
     * <p>
     * 어떤 모양도 덮지 않는 점은 이미지와 그것을 감싼 링크로 간다. 맵이 없거나 맵을 못 찾는 이미지는 일반 이미지이다.
     */
    public void testPointOutsideEveryShapeStaysWithTheImage() {
        RenderedPage p = page("<a id=w href='around.html'><img id=i src='ok.png' usemap='#m' style='width:100px;height:60px'></a>"
                + "<map name=m><area id=r shape=rect coords='0,0,10,10' href='r.html'></map>"
                + "<a id=w2 href='around2.html' style='position:absolute;left:200px;top:0'><img id=j src='ok.png' usemap='#nothing' style='width:100px;height:60px'></a>");
        HitTester t = new HitTester(p.getLayout());
        HitResult h = t.hit(5, 5);
        Check.equal("r", h.getElement().getAttribute("id"), "inside the shape");
        Check.equal("http://t.test/dir/r.html", h.getHref(), "the area link wins over the link around the image");
        h = t.hit(50, 30);
        Check.equal("i", h.getElement().getAttribute("id"), "outside every shape: the image");
        Check.equal("w", h.getLink().getAttribute("id"), "and the link around it");
        h = t.hit(250, 30);
        Check.equal("j", h.getElement().getAttribute("id"), "a missing map: the image");
        Check.equal("w2", h.getLink().getAttribute("id"), "and the link around it");
    }

    /**
     * The coordinates of an image map are those of the displayed image: they follow the width and the height of the image (not its
     * natural size) and are not changed by a transform; the border and the padding of the image are not part of it.
     * <p>
     * 이미지 맵의 좌표는 표시된 이미지의 것이다: 이미지의 자연 크기가 아니라 너비와 높이를 따르고 변환이 바꾸지 않으며 이미지의 border 와 padding 은 포함하지 않는다.
     */
    public void testCoordinatesAreInDisplayedPixels() {
        String map = "<map name=m><area id=r shape=rect coords='50,0,100,50' href='r.html'></map>";
        RenderedPage p = page("<img id=i src='ok.png' usemap='#m' style='position:absolute;left:0;top:0;width:100px;height:50px;transform:scale(2);transform-origin:0 0'>" + map);
        HitTester t = new HitTester(p.getLayout());
        Check.equal("r", t.hit(150, 20).getElement().getAttribute("id"), "the scaled image: (75, 10) in image pixels is inside the rectangle");
        Check.equal("i", t.hit(80, 20).getElement().getAttribute("id"), "the scaled image: (40, 10) in image pixels is not");
        RenderedPage q = page("<img id=i src='ok.png' usemap='#m' style='position:absolute;left:10px;top:10px;width:100px;height:50px;border:5px solid red;padding:3px'>" + map);
        t = new HitTester(q.getLayout());
        // 내용 상자의 왼쪽 위는 (10 + 5 + 3, 10 + 5 + 3)이다.
        Check.equal("r", t.hit(18 + 60, 18 + 10).getElement().getAttribute("id"), "the origin is the content box: (60, 10) is inside");
        Check.equal("i", t.hit(18 + 40, 18 + 10).getElement().getAttribute("id"), "(40, 10) is not");
        Check.equal("i", t.hit(10 + 2, 10 + 2).getElement().getAttribute("id"), "the border area is not on the image map");
    }

    /**
     * The suffix of a server-side image map: <code>?x,y</code> with the integers measured from the border box of the image, only for an
     * <code>img</code> with <code>ismap</code> in an <code>a</code> link.
     * <p>
     * 서버 쪽 이미지 맵의 접미사: 이미지 border box 에서 잰 정수로 된 <code>?x,y</code> 이며 <code>a</code> 링크 안의 <code>ismap</code> 이 있는 <code>img</code> 에만 붙는다.
     */
    public void testServerSideSuffix() {
        RenderedPage p = page("<a id=a href='cgi.map'><img id=i src='ok.png' ismap style='position:absolute;left:30px;top:20px;width:100px;height:60px;border:2px solid red'></a>"
                + "<a id=b href='cgi.map'><img id=n src='ok.png' style='position:absolute;left:300px;top:0;width:10px;height:10px'></a>");
        HitTester t = new HitTester(p.getLayout());
        Document d = RenderTestSupport.lastDocument;
        HitResult h = t.hit(30 + 12.7, 20 + 7.2);
        Check.equal("?12,7", ImageMaps.serverSideSuffix(h.getBox(), h.getElement(), h.getLink(), 30 + 12.7, 20 + 7.2, 0, 0), "from the border edge, truncated");
        Check.equal("?0,0", ImageMaps.serverSideSuffix(h.getBox(), h.getElement(), h.getLink(), 30, 20, 0, 0), "the top left corner");
        h = t.hit(305, 5);
        Check.equal("", ImageMaps.serverSideSuffix(h.getBox(), h.getElement(), h.getLink(), 305, 5, 0, 0), "an image without ismap");
        Check.equal("", ImageMaps.serverSideSuffix(null, d.getElementById("i"), d.getElementById("a"), 0, 0, 0, 0), "no box");
        Check.equal("", ImageMaps.serverSideSuffix(h.getBox(), d.getElementById("i"), null, 0, 0, 0, 0), "not in a link");
    }

    /**
     * The areas of a map in tree order, with the areas nested in other elements.
     * <p>
     * 맵의 area 를 트리 순서로. 다른 요소 안에 있는 area 도 포함한다.
     */
    public void testAreasInTreeOrder() {
        Document d = doc("<map name=m><area id=a1><p><area id=a2></p><area id=a3></map><area id=out>");
        List<Element> areas = ImageMaps.areasOf(d.getElementsByTagName("map").item(0));
        Check.equal(3, areas.size(), "three areas, the one outside is not a part of the map");
        Check.equal("a1", areas.get(0).getAttribute("id"), "first");
        Check.equal("a2", areas.get(1).getAttribute("id"), "the nested area");
        Check.equal("a3", areas.get(2).getAttribute("id"), "last");
    }
}
