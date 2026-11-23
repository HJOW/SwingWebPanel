package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of what a page shows in the place of an <code>img</code> that has no usable picture (plan item P07-07-b-11, HTML5 REC 2014-10-28
 * section 10.4.2): an image without dimensions is a non-replaced inline element with the text that it stands for, an image with
 * dimensions keeps its box with the same text in it, in quirks mode and in standards mode alike (the project shows the unsupported URL at
 * the place of the image, plan item P18-06), and the text works as a link in an <code>a</code> element.
 * <p>
 * 쓸 수 있는 그림이 없는 <code>img</code> 자리에 페이지가 무엇을 보여 주는지 확인한다(계획 P07-07-b-11, HTML5 REC 2014-10-28 §10.4.2): 크기가 없는 이미지는 그것이 대신하는
 * 텍스트를 가진 비치환 인라인 요소이고, 크기가 있는 이미지는 호환 모드와 표준 모드 모두에서 상자를 유지하며 같은 텍스트가 들어간다(프로젝트는 이미지 자리에 미지원 URL 을
 * 보여 준다, 계획 P18-06). 텍스트는 <code>a</code> 요소 안에서 링크로 동작한다.
 */
public class BrokenImageFallbackTest {

    /** The base URL of the pages. / 페이지의 기준 URL. */
    private static final String BASE = "http://t.test/dir/page.html";

    /** The unsupported URL notice for the failed image. / 읽지 못한 이미지의 미지원 URL 안내. */
    private static final String NOTICE = "[" + DisplayListBuilder.UNSUPPORTED_URL + "http://t.test/dir/bad.png]";

    /** The viewport size. / 뷰포트 크기. */
    private static final int W = 300;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrokenImageFallbackTest() {
    }

    /**
     * Renders a page.
     * <p>
     * 페이지를 렌더링한다.
     *
     * @param doctype the text before the markup (a DOCTYPE or nothing) / 마크업 앞의 문자열(문서 형식 또는 없음)
     * @param body    the body markup / body 마크업
     * @return the rendered page / 렌더링한 페이지
     */
    private static RenderedPage page(String doctype, String body) {
        RenderTestSupport.FixedImages images = new RenderTestSupport.FixedImages().put("http://t.test/dir/bad.png", ImageResource.failed("404"))
                .put("http://t.test/dir/wait.png", ImageResource.PENDING)
                .put("http://t.test/dir/ok.png", ImageResource.loaded(RenderTestSupport.solid(20, 10, 0xFFFF0000)));
        Document doc = HtmlParser.parseDocument(doctype + "<style>html,body{margin:0;padding:0} body{font:10px monospace;line-height:normal}</style><body>" + body, BASE, null,
                false);
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(W, 100));
        css.loadStyleSheets();
        RenderedPage p = PageRenderer.render(doc, css, images, W, 100);
        RenderTestSupport.lastDocument = doc;
        return p;
    }

    /**
     * Returns the text of the boxes of an element, in order.
     * <p>
     * 요소의 상자들의 텍스트를 순서대로 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id of the element / 요소의 id
     * @return the text / 텍스트
     */
    private static String textOf(RenderedPage p, String id) {
        Element e = RenderTestSupport.lastDocument.getElementById(id);
        StringBuilder sb = new StringBuilder();
        collect(p.getLayout().getRoot(), e, sb);
        return sb.toString();
    }

    /**
     * Appends the text of the TEXT boxes that belong to an element.
     * <p>
     * 요소에 속한 TEXT 상자의 텍스트를 덧붙인다.
     *
     * @param b  the box to search / 찾을 상자
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    private static void collect(LayoutBox b, Element e, StringBuilder sb) {
        if (b.getType() == LayoutBox.Type.TEXT && b.getNode() == e && b.getText() != null) {
            sb.append(b.getText());
        }
        // 자식 상자를 순서대로 훑는다.
        for (LayoutBox c : b.getChildren()) {
            collect(c, e, sb);
        }
    }

    /**
     * Returns the types of the boxes of an element.
     * <p>
     * 요소의 상자들의 종류를 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id / id
     * @return the types / 종류
     */
    private static List<LayoutBox.Type> typesOf(RenderedPage p, String id) {
        List<LayoutBox.Type> out = new ArrayList<LayoutBox.Type>();
        // 요소에 속한 상자의 종류를 모은다.
        for (LayoutBox b : p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById(id))) {
            out.add(b.getType());
        }
        return out;
    }

    /**
     * Counts the placeholder commands of a page.
     * <p>
     * 페이지의 대체 그림 명령 수를 센다.
     *
     * @param p the page / 페이지
     * @return the number / 개수
     */
    private static int placeholders(RenderedPage p) {
        int n = 0;
        DisplayList l = p.getDisplayList();
        // 그리기 명령을 훑으며 대체 그림을 센다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getType() == DisplayCommand.Type.PLACEHOLDER) {
                n++;
            }
        }
        return n;
    }

    /**
     * A failed image without dimensions is an inline element that shows the unsupported URL, with the alt text before it if there is
     * one: in standards mode and in quirks mode alike.
     * <p>
     * 크기가 없는 읽지 못한 이미지는 지원하지 않는 URL 을 보여 주는 인라인 요소이고, alt 텍스트가 있으면 그 앞에 둔다. 표준 모드와 호환 모드가 같다.
     */
    public void testFailedImageWithoutSizeIsInlineText() {
        String[] doctypes = { "<!doctype html>", "" };
        // 표준 모드와 호환 모드에서 같은 결과를 확인한다.
        for (int m = 0; m < doctypes.length; m++) {
            String mode = m == 0 ? "standards" : "quirks";
            RenderedPage p = page(doctypes[m], "<p>a<img id=i src='bad.png' alt='Logo'>b</p><p>c<img id=j src='bad.png'>d</p><p>e<img id=k src='bad.png' alt=''>f</p>");
            Check.equal("Logo " + NOTICE, textOf(p, "i"), mode + ": the alt text and the notice");
            Check.equal(NOTICE, textOf(p, "j"), mode + ": the notice only, without alt");
            Check.equal(NOTICE, textOf(p, "k"), mode + ": an empty alt shows the notice too (the project requirement)");
            Check.isFalse(typesOf(p, "i").contains(LayoutBox.Type.REPLACED), mode + ": not a replaced box");
            Check.isTrue(typesOf(p, "i").contains(LayoutBox.Type.INLINE), mode + ": an inline box");
            Check.equal(0, placeholders(p), mode + ": no placeholder frame is painted");
            BufferedImage img = RenderTestSupport.paint(p, W, 100);
            Check.isTrue(RenderTestSupport.ink(img, 0, 0, W, 30) > 50, mode + ": the text is painted");
        }
    }

    /**
     * An image without a source shows the alt text only; an image without anything to show, and a pending or a loaded image, stay replaced
     * boxes (zero, zero, and the natural size).
     * <p>
     * 소스가 없는 이미지는 alt 텍스트만 보여 준다. 보여 줄 것이 없는 이미지와 읽는 중이거나 읽은 이미지는 치환 상자로 남는다(크기 0, 0, 자연 크기).
     */
    public void testOtherStatesStayReplaced() {
        RenderedPage p = page("<!doctype html>", "<p>a<img id=i alt='only alt'>b</p><p>c<img id=j>d</p><p>e<img id=k src='wait.png' alt='soon'>f</p>"
                + "<p>g<img id=l src='ok.png' alt='no'>h</p><p>i<img id=m src='unknown.png' alt='x'>j</p>");
        Check.equal("only alt", textOf(p, "i"), "no source: the alt text");
        Check.equal("", textOf(p, "j"), "no source and no alt: nothing");
        Check.isTrue(typesOf(p, "j").contains(LayoutBox.Type.REPLACED), "no source and no alt: the replaced box of size zero");
        Check.equal("", textOf(p, "k"), "pending: nothing");
        Check.isTrue(typesOf(p, "k").contains(LayoutBox.Type.REPLACED), "pending: a replaced box");
        Check.equal("", textOf(p, "l"), "loaded: no text");
        LayoutBox ok = p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("l")).get(0);
        Check.near(20, ok.getWidth(), 0.01, "loaded: natural width");
        Check.equal("", textOf(p, "m"), "unknown (not requested yet): nothing");
    }

    /**
     * An image with dimensions keeps its box and the painter fills it with the unsupported URL and the alt text, in quirks mode (which the
     * specification asks for), in standards mode (the project keeps it for the alt text too) and for the dimensions that CSS gives.
     * <p>
     * 크기가 있는 이미지는 상자를 유지하고 그리는 쪽이 지원하지 않는 URL 과 alt 텍스트로 채운다. 규격이 요구하는 호환 모드, 프로젝트가 alt 텍스트가 있어도 유지하는 표준 모드,
     * CSS 가 크기를 주는 경우가 모두 같다.
     */
    public void testFailedImageWithSizeKeepsItsBox() {
        String[] doctypes = { "", "<!doctype html>" };
        // 호환 모드와 표준 모드에서 같은 결과를 확인한다.
        for (int m = 0; m < doctypes.length; m++) {
            String mode = m == 0 ? "quirks" : "standards";
            RenderedPage p = page(doctypes[m], "<p><img id=i src='bad.png' alt='Logo' width=120 height=30></p><p><img id=j src='bad.png' style='width:90px;height:20px'></p>"
                    + "<p><img id=k src='bad.png' alt='x' width=50></p>");
            Check.isTrue(typesOf(p, "i").contains(LayoutBox.Type.REPLACED), mode + ": attributes: the box");
            LayoutBox i = p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("i")).get(0);
            Check.near(120, i.getWidth(), 0.01, mode + ": width");
            Check.near(30, i.getHeight(), 0.01, mode + ": height");
            Check.isTrue(typesOf(p, "j").contains(LayoutBox.Type.REPLACED), mode + ": CSS: the box");
            Check.equal("", textOf(p, "i"), mode + ": the text is painted by the placeholder, not laid out");
            Check.equal(2, placeholders(p), mode + ": two placeholder frames");
            Check.equal("x " + NOTICE, textOf(p, "k"), mode + ": only one dimension: inline text");
        }
    }

    /**
     * The text of a broken image in a link is a link: a click on the text follows the link, and the text of an image that is not broken
     * does not change anything.
     * <p>
     * 링크 안 깨진 이미지의 텍스트는 링크이다: 텍스트를 누르면 링크를 따라간다.
     */
    public void testTextOfBrokenImageIsALink() {
        RenderedPage p = page("<!doctype html>", "<a id=a href='next.html'><img id=i src='bad.png' alt='Logo'></a>");
        HitTester hit = new HitTester(p.getLayout());
        HitResult r = hit.hit(10, 5);
        Check.notNull(r.getLink(), "the text is inside the link");
        Check.equal("http://t.test/dir/next.html", r.getHref(), "the link target");
        Check.equal("i", r.getElement().getAttribute("id"), "the element is the image");
    }
}
