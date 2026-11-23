package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Maps points to page content and content to rectangles (plan item P09-06), on top of the immutable {@link LayoutResult}. The topmost box at
 * a point is the last box in painting order that contains it (CSS 2.1 Appendix E: floats and positioned boxes with a higher
 * <code>z-index</code> are found before what they cover; boxes whose overflow clips are not found outside the clip). A hit reports the element, the nearest enclosing link with its absolute URL, and over text the text node with the character
 * offset that the caret would take. The conversions between component (screen) and document coordinates are the exact inverse of what
 * {@link PagePainter} applies: <code>screen = (document - scroll) * scale</code>. The geometry helpers are the basis of the JavaScript
 * <code>getBoundingClientRect</code>/<code>getClientRects</code>. All methods only read immutable data and may be called from any thread.
 * <p>
 * 점을 페이지 콘텐츠로, 콘텐츠를 사각형으로 대응시킨다(계획 P09-06). 불변 {@link LayoutResult} 위에서 동작한다. 점 위의 맨 위 상자는 점을 포함하는 그리기 순서의
 * 마지막 상자이다(CSS 2.1 부록 E: float 와 더 높은 <code>z-index</code> 의 위치 지정 상자가 덮고 있는 것보다 먼저 찾아지며, overflow 로 자르는 상자는 자르기 밖에서 찾지
 * 않는다). 히트 결과는 요소, 가장 가까운 링크와
 * 절대 URL, 텍스트 위라면 캐럿이 놓일 문자 오프셋을 가진 텍스트 노드를 알려 준다. 컴포넌트(화면) 좌표와 문서 좌표의 변환은 {@link PagePainter} 가
 * 적용하는 것의 정확한 역이다: <code>화면 = (문서 − 스크롤) × 배율</code>. 기하 도우미는 JavaScript <code>getBoundingClientRect</code>/
 * <code>getClientRects</code> 의 바탕이다. 모든 메서드는 불변 데이터만 읽으므로 어느 스레드에서나 부를 수 있다.
 */
public final class HitTester {

    /**
     * The layout that is queried.
     * <p>
     * 질의 대상 레이아웃.
     */
    private final LayoutResult layout;

    /**
     * Creates a hit tester.
     * <p>
     * 히트 테스터를 만든다.
     *
     * @param layout the layout, not null / 레이아웃(null 불가)
     */
    public HitTester(LayoutResult layout) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        this.layout = layout;
    }

    /**
     * Converts a screen x to a document x.
     * <p>
     * 화면 x 를 문서 x 로 바꾼다.
     *
     * @param screenX x in component coordinates / 컴포넌트 좌표의 x
     * @param scrollX horizontal scroll offset in document px / 문서 px 단위 가로 스크롤 오프셋
     * @param scale   the zoom factor / 확대 배율
     * @return x in document coordinates / 문서 좌표의 x
     */
    public static double toDocumentX(double screenX, int scrollX, double scale) {
        return screenX / scale + scrollX;
    }

    /**
     * Converts a screen y to a document y.
     * <p>
     * 화면 y 를 문서 y 로 바꾼다.
     *
     * @param screenY y in component coordinates / 컴포넌트 좌표의 y
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @param scale   the zoom factor / 확대 배율
     * @return y in document coordinates / 문서 좌표의 y
     */
    public static double toDocumentY(double screenY, int scrollY, double scale) {
        return screenY / scale + scrollY;
    }

    /**
     * Converts a document x to a screen x.
     * <p>
     * 문서 x 를 화면 x 로 바꾼다.
     *
     * @param docX    x in document coordinates / 문서 좌표의 x
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scale   the zoom factor / 확대 배율
     * @return x in component coordinates / 컴포넌트 좌표의 x
     */
    public static double toScreenX(double docX, int scrollX, double scale) {
        return (docX - scrollX) * scale;
    }

    /**
     * Converts a document y to a screen y.
     * <p>
     * 문서 y 를 화면 y 로 바꾼다.
     *
     * @param docY    y in document coordinates / 문서 좌표의 y
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @param scale   the zoom factor / 확대 배율
     * @return y in component coordinates / 컴포넌트 좌표의 y
     */
    public static double toScreenY(double docY, int scrollY, double scale) {
        return (docY - scrollY) * scale;
    }

    /**
     * Finds what is at a point of the document.
     * <p>
     * 문서의 한 점에 무엇이 있는지 찾는다.
     *
     * @param docX x in document coordinates / 문서 좌표의 x
     * @param docY y in document coordinates / 문서 좌표의 y
     * @return the result (its box is null outside the document) / 결과(문서 밖이면 상자가 null)
     */
    public HitResult hit(double docX, double docY) {
        return hit(docX, docY, 0, 0);
    }

    /**
     * Finds what is at a point of the document while the page is scrolled: boxes of <code>position:fixed</code> stay at the same place of
     * the viewport, so the point is compared with them after the scroll offset is subtracted.
     * <p>
     * 페이지가 스크롤된 상태에서 문서의 한 점에 무엇이 있는지 찾는다: <code>position:fixed</code> 상자는 뷰포트의 같은 자리에 머무르므로 스크롤 오프셋을 뺀 점을
     * 그 상자와 비교한다.
     *
     * @param docX    x in document coordinates / 문서 좌표의 x
     * @param docY    y in document coordinates / 문서 좌표의 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @return the result (its box is null outside the document) / 결과(문서 밖이면 상자가 null)
     */
    public HitResult hit(double docX, double docY, int scrollX, int scrollY) {
        LayoutBox box = layout.boxAt(docX, docY, scrollX, scrollY);
        if (box == null) {
            return new HitResult(null, null, null, null, null, -1);
        }
        Node node = null;
        LayoutBox b = box;
        // 노드가 없는 상자(줄, 익명 상자)는 부모 상자에서 노드를 찾는다.
        while (b != null && b.getNode() == null) {
            b = b.getParent();
        }
        if (b != null) {
            node = b.getNode();
        }
        Element element = node instanceof Element ? (Element) node : (node == null ? null : node.getParentElement());
        Node textNode = null;
        int offset = -1;
        if (box.getType() == LayoutBox.Type.TEXT && node != null && !(node instanceof Element)) {
            textNode = node;
            // 회전한 텍스트는 줄 방향 거리(위쪽에서 시작하면 y 거리, 아래쪽에서 시작하면 반전한 y 거리)를 x 거리처럼 넘긴다.
            double along = box.getTextRotation() == 0 ? docX - box.getX() : (box.getTextRotation() == 1 ? docY - box.getY() : box.getY() + box.getHeight() - docY);
            offset = textOffset(box, box.getX() + along);
        }
        Element link = null;
        Element area = null;
        // 이미지 맵이 있는 img 위의 점은 그 점을 덮는 첫 area 가 받는다(HTML5 2014 §4.7.13.2: 이벤트는 맨 위 모양에, 없으면 img 에 간다).
        if (box.getType() == LayoutBox.Type.REPLACED && ImageMaps.hasMap(element)) {
            area = ImageMaps.areaAt(box, element, docX, docY, scrollX, scrollY);
        }
        if (area != null) {
            // 모양은 img 에서 상속하는 독립된 요소처럼 동작한다: href 가 없는 area(죽은 영역)는 링크가 아니며 img 를 감싼 링크로 올라가지 않는다.
            element = area;
            link = area.hasAttribute("href") ? area : null;
        } else {
            // 요소에서 문서 쪽으로 올라가며 href 가 있는 a/area 를 찾는다.
            for (Element e = element; e != null; e = e.getParentElement()) {
                String name = e.getLocalName();
                if (("a".equals(name) || "area".equals(name)) && e.hasAttribute("href")) {
                    link = e;
                    break;
                }
            }
        }
        String href = null;
        if (link != null) {
            href = resolve(link.getOwnerDocument(), link.getAttribute("href"));
        }
        return new HitResult(box, element, link, href, textNode, offset);
    }

    /**
     * Resolves a link target against the base URL of the document.
     * <p>
     * 링크 대상을 문서의 기준 URL 로 해석한다.
     *
     * @param doc  the document / 문서
     * @param href the attribute value / 속성 값
     * @return the absolute URL, or the value itself if it cannot be resolved / 절대 URL. 해석할 수 없으면 값 그대로
     */
    private static String resolve(Document doc, String href) {
        String base = doc == null ? null : doc.getBaseUri();
        if (base == null || base.length() == 0 || "about:blank".equals(base) || "about:text".equals(base)) {
            return href;
        }
        try {
            return UrlUtil.resolve(base, href.trim());
        } catch (IllegalArgumentException e) {
            return href;
        }
    }

    /**
     * Computes the offset in the data of the text node for a point over a text fragment: the caret position nearest to the point.
     * <p>
     * 텍스트 조각 위의 점에 대해 텍스트 노드 데이터의 오프셋을 계산한다: 점에 가장 가까운 캐럿 위치.
     *
     * @param t    the TEXT box / TEXT 상자
     * @param docX x in document coordinates / 문서 좌표의 x
     * @return the offset in the node data / 노드 데이터 안의 오프셋
     */
    static int textOffset(LayoutBox t, double docX) {
        String s = t.getText() == null ? "" : t.getText();
        int n = s.length();
        int start = t.getSourceStart();
        int end = t.getSourceEnd();
        double advance = t.getTextAdvance();
        if (n == 0 || advance <= 0) {
            return start;
        }
        double rel = docX - t.getX();
        if ((t.getBidiLevel() & 1) == 1) {
            rel = advance - rel;
        }
        int idx;
        if (rel <= 0) {
            idx = 0;
        } else if (rel >= advance) {
            idx = n;
        } else if (t.getGlyphMode() == LayoutBox.GLYPHS_COMBINED) {
            // 조합한 묶음은 한 글자처럼 앞 절반이면 처음, 뒤 절반이면 끝이다.
            idx = rel * 2 < advance ? 0 : n;
        } else if (t.getGlyphMode() == LayoutBox.GLYPHS_UPRIGHT) {
            // 똑바로 선 글자는 클러스터마다 같은 크기의 칸이므로 가장 가까운 칸 경계를 고른다.
            int[] cl = t.getClusterOffsets();
            int cn = cl.length - 1;
            int k = (int) Math.round(rel / (advance / cn));
            idx = cl[Math.max(0, Math.min(cn, k))];
        } else if (TextRenderer.isComplex(s) || t.getFont() == null) {
            idx = (int) Math.round(n * rel / advance);
        } else {
            idx = 0;
            double prev = 0;
            int i = 0;
            // 클러스터 경계마다 앞부분의 advance 를 재서 점이 속한 구간의 가까운 쪽을 고른다.
            while (i < n) {
                int next = TextRenderer.clusterEnd(s, i);
                double adv = TextRenderer.advance(t.getFont(), s.substring(0, next)) + t.getLetterSpacing() * next;
                if (rel < adv) {
                    idx = rel - prev < adv - rel ? i : next;
                    break;
                }
                prev = adv;
                i = next;
                idx = n;
            }
        }
        if (end - start == n) {
            return start + idx;
        }
        return start + (int) Math.round((double) idx * (end - start) / n);
    }

    /**
     * Returns the bounding box of a node: the union of the border boxes of its layout boxes (for text: its fragments).
     * <p>
     * 노드의 경계 상자를 반환한다: 레이아웃 상자들의 테두리 상자의 합집합(텍스트는 그 조각들).
     *
     * @param node the node / 노드
     * @return the rectangle in document coordinates, or null if the node has no box / 문서 좌표의 사각형. 상자가 없으면 null
     */
    public Rectangle2D getBoundingBox(Node node) {
        Rectangle2D r = null;
        // 노드의 모든 상자를 하나로 합친다.
        for (Rectangle2D c : getClientRects(node)) {
            r = r == null ? c : r.createUnion(c);
        }
        return r;
    }

    /**
     * Returns the rectangles of a node, one per box (an inline element that wraps over lines gives one per line).
     * <p>
     * 노드의 사각형을 상자마다 하나씩 반환한다(여러 줄에 걸친 인라인 요소는 줄마다 하나).
     *
     * @param node the node / 노드
     * @return the rectangles in document coordinates, empty if the node has no box / 문서 좌표의 사각형. 상자가 없으면 빈 목록
     */
    public List<Rectangle2D> getClientRects(Node node) {
        List<Rectangle2D> list = new ArrayList<Rectangle2D>();
        // 노드에 속한 상자마다 테두리 상자를 사각형으로 만든다(생성 콘텐츠 표식은 제외한다).
        for (LayoutBox b : layout.getBoxes(node)) {
            if (b.getType() == LayoutBox.Type.MARKER || b.getPseudo() != null) {
                continue;
            }
            Rectangle2D r = new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            // 변환된 상자(와 변환된 조상 안의 상자)는 변환한 사각형의 경계 상자를 알린다(getBoundingClientRect, P10-06).
            java.awt.geom.AffineTransform full = b.getFullTransform();
            if (full != null) {
                r = full.createTransformedShape(r).getBounds2D();
            }
            list.add(r);
        }
        return list;
    }
}
