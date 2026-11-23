package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;

/**
 * Client-side image maps of HTML (HTML5 REC 2014-10-28 section 4.7.13, https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html#image-maps,
 * retrieved 2026-11-17): an <code>img</code> with a <code>usemap</code> attribute is associated with the <code>map</code> element that the
 * attribute names, and the <code>area</code> elements of the map are shapes that are layered on the image in reverse tree order (the
 * first <code>area</code> in tree order is the top-most shape). A point of the image belongs to the first shape that contains it, also
 * when that shape has no <code>href</code> (a dead area catches the point, so the image or the link around the image is not used). The
 * coordinates of the shapes are in CSS pixels of the displayed image, so they are not affected by the zoom and by transforms. This class
 * only reads the DOM and computes; it is thread-confined to its caller like the DOM.
 * <p>
 * HTML 의 클라이언트 쪽 이미지 맵(HTML5 REC 2014-10-28 §4.7.13): <code>usemap</code> 속성이 있는 <code>img</code> 는 속성이 가리키는 <code>map</code> 요소와 연결되고,
 * 맵의 <code>area</code> 요소는 트리 순서의 역순으로 이미지 위에 겹쳐지는 모양이다(트리 순서의 첫 <code>area</code> 가 맨 위 모양). 이미지의 한 점은 그 점을 포함하는
 * 첫 모양에 속하며, 그 모양에 <code>href</code> 가 없어도 마찬가지이다(죽은 영역이 점을 잡으므로 이미지나 이미지를 감싼 링크는 쓰이지 않는다). 모양의 좌표는 표시된
 * 이미지의 CSS 픽셀이므로 확대와 변환의 영향을 받지 않는다. 이 클래스는 DOM 을 읽고 계산만 하며 DOM 처럼 호출자의 스레드에 묶인다.
 */
public final class ImageMaps {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ImageMaps() {
    }

    /**
     * Finds the <code>map</code> element that a <code>usemap</code> value refers to: the rules for parsing a hash-name reference of
     * HTML5 2014 section 2.4.9 return the first <code>map</code> in tree order that has an <code>id</code> equal to the text after the
     * first "#" or a <code>name</code> that matches it without regard to case. A value without "#" or with "#" at its end names no map.
     * <p>
     * <code>usemap</code> 값이 가리키는 <code>map</code> 요소를 찾는다: HTML5 2014 §2.4.9 의 해시 이름 참조 해석 규칙은 첫 "#" 뒤의 문자열과 같은 <code>id</code> 를
     * 가졌거나 대소문자를 구분하지 않고 같은 <code>name</code> 을 가진 트리 순서의 첫 <code>map</code> 을 돌려준다. "#" 이 없거나 맨 끝에 있는 값은 맵을 가리키지 않는다.
     *
     * @param doc    the document / 문서
     * @param useMap the value of the <code>usemap</code> attribute, may be null / <code>usemap</code> 속성 값. null 일 수 있다
     * @return the map element, or null / map 요소. 없으면 null
     */
    public static Element findMap(Document doc, String useMap) {
        return ElementCollections.findMap(doc, useMap);
    }

    /**
     * Returns the <code>area</code> elements of a map in tree order.
     * <p>
     * 맵의 <code>area</code> 요소를 트리 순서로 반환한다.
     *
     * @param map the map element / map 요소
     * @return the areas, never null / area 요소들. null 이 아니다
     */
    public static List<Element> areasOf(Element map) {
        List<Element> out = new ArrayList<Element>();
        ElementCollection all = map.getElementsByTagName("area");
        // 후손 area 를 트리 순서로 모은다.
        for (int i = 0; i < all.getLength(); i++) {
            out.add(all.item(i));
        }
        return out;
    }

    /**
     * Finds the <code>area</code> of the map of an image that is at a point of the image: the first area in tree order whose shape
     * contains the point (the shapes are layered in reverse tree order, so that one is the top-most).
     * <p>
     * 이미지의 맵에서 이미지의 한 점에 있는 <code>area</code> 를 찾는다: 모양이 점을 포함하는 트리 순서의 첫 area(모양은 트리 순서의 역순으로 겹쳐지므로 그것이 맨 위이다).
     *
     * @param img the <code>img</code> element / <code>img</code> 요소
     * @param x   x in CSS pixels from the left edge of the displayed image / 표시된 이미지의 왼쪽 가장자리에서 CSS 픽셀 x
     * @param y   y in CSS pixels from the top edge of the displayed image / 표시된 이미지의 위쪽 가장자리에서 CSS 픽셀 y
     * @param w   the displayed width of the image / 표시된 이미지의 너비
     * @param h   the displayed height of the image / 표시된 이미지의 높이
     * @return the area, or null if the image has no map or no shape covers the point / area. 맵이 없거나 점을 덮는 모양이 없으면 null
     */
    public static Element areaAt(Element img, double x, double y, double w, double h) {
        String useMap = img.getAttribute("usemap");
        if (useMap == null || useMap.length() == 0) {
            return null;
        }
        Element map = findMap(img.getOwnerDocument(), useMap);
        if (map == null) {
            return null;
        }
        List<Element> areas = areasOf(map);
        // 트리 순서의 첫 area 가 맨 위 모양이므로 앞에서부터 점을 포함하는 첫 모양을 찾는다.
        for (int i = 0; i < areas.size(); i++) {
            if (contains(areas.get(i), x, y, w, h)) {
                return areas.get(i);
            }
        }
        return null;
    }

    /**
     * Tells whether the shape of an <code>area</code> contains a point of the image (HTML5 2014 section 4.7.13.2): the shape is
     * empty if the <code>coords</code> list has fewer items than its state needs, an odd last item of a polygon and the items past the
     * ones that a state uses are dropped, a rectangle with its corners in the wrong order is swapped, and a circle needs a positive
     * radius. The edges of a rectangle belong to it on the left and the top, the points of a circle include its edge, and a polygon uses
     * the even-odd rule.
     * <p>
     * <code>area</code> 의 모양이 이미지의 한 점을 포함하는지 알려 준다(HTML5 2014 §4.7.13.2): <code>coords</code> 목록의 항목이 상태에 필요한 수보다 적으면 모양이 비어
     * 있고, 다각형의 홀수 마지막 항목과 상태가 쓰는 것을 넘는 항목은 버리며, 모서리 순서가 틀린 직사각형은 서로 바꾸고, 원은 양의 반지름이 필요하다. 직사각형은 왼쪽과 위쪽
     * 가장자리를 포함하고 원은 가장자리를 포함하며 다각형은 짝홀 규칙을 쓴다.
     *
     * @param area the <code>area</code> element / <code>area</code> 요소
     * @param x    x in CSS pixels from the left edge of the displayed image / 표시된 이미지의 왼쪽 가장자리에서 CSS 픽셀 x
     * @param y    y in CSS pixels from the top edge of the displayed image / 표시된 이미지의 위쪽 가장자리에서 CSS 픽셀 y
     * @param w    the displayed width of the image / 표시된 이미지의 너비
     * @param h    the displayed height of the image / 표시된 이미지의 높이
     * @return true if the point is inside the shape / 점이 모양 안이면 true
     */
    public static boolean contains(Element area, double x, double y, double w, double h) {
        String shape = area.getAttribute("shape");
        String kind = shape == null ? "rect" : shape.toLowerCase(java.util.Locale.ENGLISH);
        int[] c = parseIntegers(area.getAttribute("coords"));
        if ("default".equals(kind)) {
            // 기본 상태의 모양은 이미지 전체를 덮는 직사각형이다.
            return x >= 0 && x < w && y >= 0 && y < h;
        }
        if ("circle".equals(kind) || "circ".equals(kind)) {
            if (c.length < 3 || c[2] <= 0) {
                return false;
            }
            double dx = x - c[0];
            double dy = y - c[1];
            return dx * dx + dy * dy <= (double) c[2] * c[2];
        }
        if ("poly".equals(kind) || "polygon".equals(kind)) {
            if (c.length < 6) {
                return false;
            }
            int n = c.length - (c.length % 2);
            Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            path.moveTo(c[0], c[1]);
            // 꼭짓점을 차례로 잇고 닫는다.
            for (int i = 2; i < n; i += 2) {
                path.lineTo(c[i], c[i + 1]);
            }
            path.closePath();
            Shape s = path;
            return s.contains(x, y);
        }
        // 그 밖에는(rect, rectangle, 없는 값, 알 수 없는 값) 직사각형 상태이다.
        if (c.length < 4) {
            return false;
        }
        int x1 = Math.min(c[0], c[2]);
        int x2 = Math.max(c[0], c[2]);
        int y1 = Math.min(c[1], c[3]);
        int y2 = Math.max(c[1], c[3]);
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    /**
     * Parses a list of integers (HTML5 2014 section 2.4.4.5, the rules that the <code>coords</code> attribute uses): numbers are
     * separated by commas, semicolons and spaces, a "-" in front of the digits negates the number, characters that are not digits
     * after a number end that number, and a field without digits ends the list.
     * <p>
     * 정수 목록을 해석한다(HTML5 2014 §2.4.4.5, <code>coords</code> 속성이 쓰는 규칙): 숫자는 쉼표, 세미콜론, 공백으로 구분하고, 숫자 앞의 "-" 는 음수로 만들며, 숫자 뒤의
     * 숫자가 아닌 글자는 그 숫자를 끝내고, 숫자가 없는 필드는 목록을 끝낸다.
     *
     * @param s the attribute value, may be null / 속성 값. null 일 수 있다
     * @return the numbers, empty for null and for an empty value / 숫자들. null 이거나 빈 값이면 빈 배열
     */
    static int[] parseIntegers(String s) {
        List<Integer> numbers = new ArrayList<Integer>();
        if (s == null) {
            return new int[0];
        }
        int pos = 0;
        int n = s.length();
        // 구분자와 숫자를 번갈아 읽는다.
        while (true) {
            // 구분자 하나를 건너뛴다.
            if (pos < n && isSeparator(s.charAt(pos))) {
                pos++;
            }
            if (pos >= n) {
                break;
            }
            if (isSeparator(s.charAt(pos))) {
                continue;
            }
            boolean negated = false;
            long value = 0;
            boolean started = false;
            boolean gotNumber = false;
            boolean finished = false;
            boolean ended = false;
            // 한 필드를 구분자나 끝까지 읽는다.
            while (pos < n) {
                char ch = s.charAt(pos);
                if (ch == '-') {
                    if (gotNumber) {
                        finished = true;
                    }
                    if (!finished) {
                        negated = !started;
                        started = true;
                    }
                } else if (ch >= '0' && ch <= '9') {
                    if (!finished) {
                        value = Math.min(value * 10 + (ch - '0'), Integer.MAX_VALUE);
                        started = true;
                        gotNumber = true;
                    }
                } else if (isSeparator(ch)) {
                    if (!gotNumber) {
                        // 숫자가 없는 필드는 목록을 끝낸다("1,2,x,4" 처럼).
                        return toArray(numbers);
                    }
                    ended = true;
                    break;
                } else if (!Character.isLetter(ch) && ch < 128) {
                    // 알파벳이 아닌 그 밖의 ASCII 글자: 숫자 뒤이면 숫자를 끝내고 아니면 음수 표시를 취소한다.
                    if (gotNumber) {
                        finished = true;
                    }
                    if (!finished) {
                        negated = false;
                    }
                } else if (!finished) {
                    negated = false;
                }
                pos++;
            }
            if (!ended && !gotNumber) {
                break;
            }
            numbers.add(Integer.valueOf((int) (negated ? -value : value)));
            if (!ended) {
                break;
            }
        }
        return toArray(numbers);
    }

    /**
     * Tells whether a character separates the numbers of a list of integers.
     * <p>
     * 글자가 정수 목록의 숫자를 구분하는지 알려 준다.
     *
     * @param ch the character / 글자
     * @return true for a space, a comma and a semicolon / 공백, 쉼표, 세미콜론이면 true
     */
    private static boolean isSeparator(char ch) {
        return ch == ' ' || ch == ',' || ch == ';';
    }

    /**
     * Copies a list of numbers into an array.
     * <p>
     * 숫자 목록을 배열로 복사한다.
     *
     * @param list the list / 목록
     * @return the array / 배열
     */
    private static int[] toArray(List<Integer> list) {
        int[] out = new int[list.size()];
        // 항목을 하나씩 옮긴다.
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i).intValue();
        }
        return out;
    }

    /**
     * Finds the <code>area</code> of the image map of an image that covers a point of the page. The point is moved into the coordinate
     * system of the image (see {@link #localPoint}) and the content box of the image is the origin, because the coordinates of an image
     * map are in CSS pixels of the displayed image.
     * <p>
     * 이미지 맵이 있는 이미지에서 페이지의 한 점을 덮는 <code>area</code> 를 찾는다. 점을 이미지의 좌표계로 옮기고({@link #localPoint} 참조) 이미지 맵 좌표는 표시된 이미지의
     * CSS 픽셀이므로 이미지의 content box 를 원점으로 삼는다.
     *
     * @param box     the replaced box of the image / 이미지의 치환 상자
     * @param img     the <code>img</code> element / <code>img</code> 요소
     * @param docX    x in document coordinates / 문서 좌표 x
     * @param docY    y in document coordinates / 문서 좌표 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @return the area, or null if no shape covers the point / area. 점을 덮는 모양이 없으면 null
     */
    public static Element areaAt(LayoutBox box, Element img, double docX, double docY, int scrollX, int scrollY) {
        double[] p = localPoint(box, docX, docY, scrollX, scrollY);
        if (p == null) {
            return null;
        }
        return areaAt(img, p[0] - box.getContentX(), p[1] - box.getContentY(), box.getContentWidth(), box.getContentHeight());
    }

    /**
     * Moves a point of the page into the coordinate system of a box: the scroll offset is subtracted if the box is inside a fixed box (as
     * the hit test does), and the transforms of the box and its ancestors are undone, so zoom and transforms do not change what a
     * point of the displayed image means.
     * <p>
     * 페이지의 한 점을 상자의 좌표계로 옮긴다: 상자가 fixed 상자 안이면 (히트 테스트처럼) 스크롤 오프셋을 빼고 상자와 조상의 변환을 되돌리므로 확대와 변환은 표시된 이미지의
     * 점이 뜻하는 바를 바꾸지 않는다.
     *
     * @param box     the box / 상자
     * @param docX    x in document coordinates / 문서 좌표 x
     * @param docY    y in document coordinates / 문서 좌표 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @return the point {x, y} in the untransformed document coordinates of the box, or null if the transform cannot be inverted / 변환하지 않은 상자의 문서 좌표의 점 {x, y}. 변환을
     *         되돌릴 수 없으면 null
     */
    public static double[] localPoint(LayoutBox box, double docX, double docY, int scrollX, int scrollY) {
        double px = docX;
        double py = docY;
        // 상자나 조상이 뷰포트에 고정되어 있으면 점은 스크롤을 뺀 값과 비교한다.
        for (LayoutBox a = box; a != null; a = a.getParent()) {
            if (a.isFixedPosition()) {
                px -= scrollX;
                py -= scrollY;
                break;
            }
        }
        java.awt.geom.AffineTransform full = box.getFullTransform();
        if (full != null) {
            try {
                java.awt.geom.Point2D p = full.inverseTransform(new java.awt.geom.Point2D.Double(px, py), null);
                px = p.getX();
                py = p.getY();
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                return null;
            }
        }
        return new double[] { px, py };
    }

    /**
     * Returns the hyperlink suffix of a server-side image map (HTML5 2014 section 4.5.1, the activation behavior of <code>a</code>): if the
     * click went to an <code>img</code> with an <code>ismap</code> attribute inside an <code>a</code> link, the address that is followed
     * gets <code>?x,y</code> appended, with the distance in CSS pixels from the left and top edge of the border of the image to the
     * click as integers (a point in the part of the image that has no such distance is zero).
     * <p>
     * 서버 쪽 이미지 맵의 하이퍼링크 접미사를 반환한다(HTML5 2014 §4.5.1, <code>a</code> 의 활성화 동작): <code>a</code> 링크 안의 <code>ismap</code> 속성이 있는
     * <code>img</code> 로 클릭이 갔으면 따라가는 주소 뒤에 <code>?x,y</code> 가 붙는다. 값은 이미지 border 의 왼쪽과 위쪽 가장자리에서 클릭까지의 CSS 픽셀 거리이며 정수이다
     * (그런 거리가 없는 부분의 점은 0).
     *
     * @param hit     the box of the element that was hit / 맞은 요소의 상자
     * @param target  the element that received the click / 클릭을 받은 요소
     * @param link    the link that is followed / 따라가는 링크
     * @param docX    x of the click in document coordinates / 클릭의 문서 좌표 x
     * @param docY    y of the click in document coordinates / 클릭의 문서 좌표 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @return the suffix such as <code>?12,34</code>, or the empty string if the click is not on a server-side image map / <code>?12,34</code> 같은 접미사. 서버 쪽 이미지 맵 위의
     *         클릭이 아니면 빈 문자열
     */
    public static String serverSideSuffix(LayoutBox hit, Element target, Element link, double docX, double docY, int scrollX, int scrollY) {
        if (hit == null || target == null || link == null || !"img".equals(target.getLocalName()) || !target.hasAttribute("ismap")
                || !"a".equals(link.getLocalName())) {
            return "";
        }
        double[] p = localPoint(hit, docX, docY, scrollX, scrollY);
        if (p == null) {
            return "?0,0";
        }
        long x = (long) Math.floor(p[0] - hit.getX());
        long y = (long) Math.floor(p[1] - hit.getY());
        return "?" + Math.max(0, x) + "," + Math.max(0, y);
    }

    /**
     * Tells whether an element is an <code>img</code> in the HTML namespace that has a <code>usemap</code> attribute.
     * <p>
     * 요소가 <code>usemap</code> 속성이 있는 HTML 네임스페이스의 <code>img</code> 인지 알려 준다.
     *
     * @param e the element, may be null / 요소. null 일 수 있다
     * @return true if it may have an image map / 이미지 맵이 있을 수 있으면 true
     */
    static boolean hasMap(Node e) {
        return e instanceof Element && "img".equals(((Element) e).getLocalName()) && ((Element) e).hasAttribute("usemap");
    }
}
