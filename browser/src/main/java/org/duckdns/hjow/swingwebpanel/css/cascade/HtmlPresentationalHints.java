package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorMatcher;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.values.NamedColors;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The presentational hints of HTML (the "non-CSS presentational hints" of the HTML Standard and the long-standing browser behaviour for old pages):
 * <code>bgcolor</code>, <code>background</code>, <code>text</code>, margins of <code>body</code>; <code>border</code>, <code>cellpadding</code>,
 * <code>cellspacing</code>, <code>width</code>, <code>height</code>, <code>align</code>, <code>valign</code>, <code>nowrap</code> of tables and cells;
 * <code>align</code> of block elements; <code>width</code>, <code>height</code>, <code>hspace</code>, <code>vspace</code>, <code>border</code>,
 * <code>align</code> of images and objects; <code>color</code>, <code>face</code>, <code>size</code> of <code>font</code>; <code>clear</code> of
 * <code>br</code>; <code>type</code> of lists; <code>align</code>, <code>size</code>, <code>width</code>, <code>noshade</code> of <code>hr</code>;
 * <code>frameborder</code> of frames. Colors follow the legacy color parsing rules. The link colors of <code>body</code> (<code>link</code>,
 * <code>vlink</code>, <code>alink</code>) apply to matching links through the resolver's dynamic state context.
 * <p>
 * HTML 의 표현 힌트(HTML 표준의 "CSS 가 아닌 표현 힌트"와 오래된 페이지를 위한 브라우저의 오랜 동작): <code>body</code> 의 <code>bgcolor</code>, <code>background</code>,
 * <code>text</code>, 여백, 표와 셀의 <code>border</code>, <code>cellpadding</code>, <code>cellspacing</code>, <code>width</code>, <code>height</code>, <code>align</code>,
 * <code>valign</code>, <code>nowrap</code>, 블록 요소의 <code>align</code>, 이미지와 객체의 <code>width</code>, <code>height</code>, <code>hspace</code>, <code>vspace</code>,
 * <code>border</code>, <code>align</code>, <code>font</code> 의 <code>color</code>, <code>face</code>, <code>size</code>, <code>br</code> 의 <code>clear</code>, 목록의
 * <code>type</code>, <code>hr</code> 의 <code>align</code>, <code>size</code>, <code>width</code>, <code>noshade</code>, 프레임의 <code>frameborder</code>. 색은 이전 색
 * 파싱 규칙을 따른다. <code>body</code>의 링크 색은 리졸버의 동적 상태 문맥을 통해 일치하는 링크에 적용한다.
 */
public final class HtmlPresentationalHints implements StatefulPresentationalHints {

    /** Unvisited-link selector; HTML5 2014 rendering section 10.3.2. / HTML5 2014 렌더링 10.3.2의 미방문 링크 선택자. */
    private static final SelectorList LINK = stateSelector(":link");
    /** Visited-link selector. / 방문한 링크 선택자. */
    private static final SelectorList VISITED = stateSelector(":visited");
    /** Active-link state selector. / 활성 링크 상태 선택자. */
    private static final SelectorList ACTIVE = stateSelector(":active");

    /**
     * Parses a fixed selector once, treating a programmer typo as initialization failure.
     * <p>고정 선택자를 한 번 파싱하며 프로그래머의 오타는 초기화 실패로 처리한다.
     * @param selector fixed selector text / 고정 선택자 문자열
     * @return parsed selector / 파싱한 선택자
     */
    private static SelectorList stateSelector(String selector) {
        try {
            return SelectorParser.parse(selector, null);
        } catch (SelectorSyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates the hints.
     * <p>
     * 힌트를 만든다.
     */
    public HtmlPresentationalHints() {
    }

    /**
     * Builds the declarations of an element.
     * <p>
     * 요소의 선언을 만든다.
     *
     * @param e    the element / 요소
     * @param base the base URL or null / 기준 URL 또는 null
     * @return the declaration text or null / 선언 문자열 또는 null
     */
    public String hintsFor(Element e, String base) {
        return hintsFor(e, base, new MatchContext());
    }

    /**
     * Builds hints with the shared dynamic match context, including body link colors and attribute-free center elements.
     * HTML5 2014 section 10.3.3 assigns center alignment to the author hint origin, rather than the UA sheet.
     * <p>본문 링크 색과 속성 없는 center 요소까지 공용 동적 매칭 문맥으로 힌트를 만든다.
     * HTML5 2014 §10.3.3에 따라 center 정렬은 UA 시트가 아닌 작성자 표현 힌트 출처에 속한다.
     * @param e styled element / 스타일을 계산할 요소
     * @param base document base URL / 문서 기준 URL
     * @param context resolver's match context / 리졸버 매칭 문맥
     * @return declarations or null / 선언 또는 null
     */
    public String hintsFor(Element e, String base, MatchContext context) {
        if (!Namespaces.HTML.equals(e.getNamespaceURI())) {
            return null;
        }
        String n = Namespaces.asciiLower(e.getLocalName());
        String attributes = attributeHints(e, n, base, context);
        // 호환 모드의 여백 병합 규칙(HTML5 2014 10.3.10)은 구조에 따른 힌트이며 속성 힌트 앞에 둔다.
        String quirks = QuirkHints.margins(e, n);
        if (quirks == null) {
            return attributes;
        }
        return attributes == null ? quirks : quirks + attributes;
    }

    /**
     * Builds the hints that come from the attributes of an element (and from its fixed role such as <code>center</code>).
     * <p>
     * 요소의 속성(그리고 <code>center</code> 같은 고정된 역할)에서 나오는 힌트를 만든다.
     *
     * @param e       styled element / 스타일을 계산할 요소
     * @param n       lower-case local name / 소문자 지역 이름
     * @param base    document base URL / 문서 기준 URL
     * @param context resolver's match context / 리졸버 매칭 문맥
     * @return declarations or null / 선언 또는 null
     */
    private String attributeHints(Element e, String n, String base, MatchContext context) {
        // center는 속성이 없어도 표현 힌트를 제공하며 일반 사용자 CSS보다 우선한다.
        if (n.equals("center")) {
            return "text-align: center;";
        }
        // 속성이 없는 요소는 힌트가 없다. 다만 표 부품은 표의 border/rules/cellpadding 힌트를 받고, 프레임 안 문서의 body 는 컨테이너 프레임 요소의 여백 속성을 물려받을 수 있다.
        if (!e.hasAttributes() && !isTablePart(n)) {
            Document owner = e.getOwnerDocument();
            if (owner == null || owner.getContainerFrame() == null || !"body".equals(n)) {
                return null;
            }
        }
        StringBuilder sb = new StringBuilder();
        if ((n.equals("a") || n.equals("area")) && e.hasAttribute("href")) {
            linkColors(e, sb, context);
        } else if (n.equals("body")) {
            body(e, sb, base);
        } else if (n.equals("frameset")) {
            frameset(e, sb);
        } else if (n.equals("frame")) {
            frame(e, sb);
        } else if (n.equals("table")) {
            TableHints.table(e, sb);
        } else if (n.equals("td") || n.equals("th")) {
            TableHints.cell(e, sb);
        } else if (n.equals("tr")) {
            TableHints.row(e, sb);
        } else if (n.equals("thead") || n.equals("tbody") || n.equals("tfoot")) {
            TableHints.section(e, sb);
        } else if (n.equals("caption")) {
            TableHints.caption(e, sb);
        } else if (n.equals("col") || n.equals("colgroup")) {
            TableHints.column(e, sb);
        } else if (n.equals("img") || n.equals("object") || n.equals("embed") || n.equals("applet") || n.equals("iframe") || n.equals("video")
                || n.equals("canvas")
                || (n.equals("input") && LegacyAttributes.keyword(e, "type", "image") != null)) {
            replaced(e, n, sb);
        } else if (n.equals("font")) {
            font(e, sb);
        } else if (n.equals("br")) {
            clear(e, sb);
        } else if (n.equals("hr")) {
            hr(e, sb);
        } else if (n.equals("marquee")) {
            marquee(e, sb);
        } else if (n.equals("pre")) {
            // pre[wrap] 은 줄을 접는 서식 있는 텍스트이다(HTML5 2014 10.3.3).
            if (e.hasAttribute("wrap")) {
                LegacyAttributes.add(sb, "white-space", "pre-wrap");
            }
        } else if (n.equals("ol") || n.equals("ul") || n.equals("li")) {
            listType(e, n, sb);
        } else if (n.equals("div")) {
            // div 는 center 와 middle 이 모두 글을 가운데 정렬한다.
            LegacyAttributes.textAlign(e, sb, true, false);
        } else if (n.equals("p") || n.equals("h1") || n.equals("h2") || n.equals("h3") || n.equals("h4") || n.equals("h5") || n.equals("h6")) {
            LegacyAttributes.textAlign(e, sb, false, false);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Tells whether a lower-case local name is a table part that takes hints from its table: a caption, a column group, a row group, a row
     * or a cell.
     * <p>
     * 소문자 지역 이름이 표에서 힌트를 받는 표 부품(캡션, 열 그룹, 행 그룹, 행, 셀)인지 알려 준다.
     *
     * @param n the name / 이름
     * @return true for a table part / 표 부품이면 true
     */
    private static boolean isTablePart(String n) {
        return n.equals("td") || n.equals("th") || n.equals("tr") || n.equals("thead") || n.equals("tbody") || n.equals("tfoot")
                || n.equals("caption") || n.equals("colgroup");
    }

    /**
     * Applies link/vlink/alink from the first body child of the document element. Every declaration remains an author hint
     * with specificity zero; active color wins over the visited/unvisited hint, while author CSS can override both.
     * Source: https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#the-page (2014-10-28, retrieved 2026-11-16).
     * <p>문서 요소의 첫 body 자식에서 link/vlink/alink를 적용한다. 구체성 0의 작성자 힌트이며 활성 색이 방문 여부 색보다 우선하고 작성자 CSS는 둘 다 덮어쓴다.
     * @param link candidate hyperlink / 후보 하이퍼링크
     * @param output declaration output / 선언 출력
     * @param context dynamic match context / 동적 매칭 문맥
     */
    private static void linkColors(Element link, StringBuilder output, MatchContext context) {
        Element root = link.getOwnerDocument().getDocumentElement();
        if (root == null) {
            return;
        }
        Element body = null;
        // 중첩되거나 다른 네임스페이스에 있는 body는 문서의 본문으로 사용하지 않는다.
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && Namespaces.HTML.equals(((Element) child).getNamespaceURI())
                    && "body".equals(((Element) child).getLocalName())) {
                body = (Element) child;
                break;
            }
        }
        if (body == null) {
            return;
        }
        boolean visited = SelectorMatcher.matches(VISITED, link, context);
        boolean unvisited = SelectorMatcher.matches(LINK, link, context);
        if (!visited && !unvisited) {
            return;
        }
        LegacyAttributes.color(body, visited ? "vlink" : "link", "color", output);
        if (SelectorMatcher.matches(ACTIVE, link, context)) {
            LegacyAttributes.color(body, "alink", "color", output);
        }
    }

    /**
     * Parses HTML5 2014 section 2.4.6 legacy colors: CSS3 SVG names, <code>#rgb</code>, or components derived from arbitrary text.
     * Empty original input and transparent are errors; whitespace-only input produces black. Only HTML ASCII whitespace is stripped.
     * Source: https://www.w3.org/TR/2014/REC-html5-20141028/infrastructure.html#rules-for-parsing-a-legacy-color-value (retrieved 2026-11-17).
     * <p>
     * HTML5 2014 §2.4.6의 CSS3 SVG 색 이름·<code>#rgb</code>·임의 문자열 성분 해석을 적용한다.
     * 빈 원문과 transparent는 실패하고 공백만 있으면 검정이다. HTML ASCII 공백만 제거한다.
     *
     * @param value the attribute value / 속성 값
     * @return a CSS color, or null for missing/empty original input or transparent / CSS 색. 속성 부재·빈 원문·transparent이면 null
     */
    static String legacyColor(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        int start = 0;
        int end = value.length();
        // HTML이 정한 다섯 ASCII 공백만 제거한다. NBSP·수직 탭 등은 색 입력에 남긴다.
        while (start < end && Namespaces.isAsciiWhitespace(value.charAt(start))) {
            start++;
        }
        // 빈 원문 검사는 공백 제거 전에 하므로 공백만 있는 입력도 일반 성분 해석을 거친다.
        while (end > start && Namespaces.isAsciiWhitespace(value.charAt(end - 1))) {
            end--;
        }
        String s = Namespaces.asciiLower(value.substring(start, end));
        if (s.equals("transparent")) {
            return null;
        }
        // 공유 CSS4 색 표의 rebeccapurple은 기준 판본의 CSS3 SVG 색 이름이 아니다.
        if (NamedColors.lookup(s) >= 0 && !s.equals("rebeccapurple")) {
            return s;
        }
        if (s.length() == 4 && s.charAt(0) == '#' && isHex(s.charAt(1)) && isHex(s.charAt(2)) && isHex(s.charAt(3))) {
            return "#" + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2) + s.charAt(3) + s.charAt(3);
        }
        if (s.length() > 128) {
            s = s.substring(0, 128);
        }
        if (s.length() > 0 && s.charAt(0) == '#') {
            s = s.substring(1);
        }
        StringBuilder h = new StringBuilder();
        // 16진수가 아닌 UTF-16 단위를 0으로 바꾼다. 보충 문자의 서로게이트 쌍도 규격대로 00이 된다.
        for (int i = 0; i < s.length(); i++) {
            h.append(isHex(s.charAt(i)) ? s.charAt(i) : '0');
        }
        // 길이가 0 이 아닌 3 의 배수가 되도록 0 을 덧붙인다.
        while (h.length() == 0 || h.length() % 3 != 0) {
            h.append('0');
        }
        int len = h.length() / 3;
        String[] c = {h.substring(0, len), h.substring(len, 2 * len), h.substring(2 * len)};
        if (len > 8) {
            // 성분이 8 자리보다 길면 앞쪽을 잘라 낸다.
            for (int i = 0; i < 3; i++) {
                c[i] = c[i].substring(len - 8);
            }
            len = 8;
        }
        // 세 성분이 모두 0 으로 시작하고 길이가 2 보다 크면 앞의 0 을 하나씩 뺀다.
        while (len > 2 && c[0].charAt(0) == '0' && c[1].charAt(0) == '0' && c[2].charAt(0) == '0') {
            for (int i = 0; i < 3; i++) {
                c[i] = c[i].substring(1);
            }
            len--;
        }
        // 한 자리 성분은 0~15의 정수이므로 앞에 0을 붙이고 두 자리보다 긴 성분만 자른다.
        return len == 1 ? "#0" + c[0] + "0" + c[1] + "0" + c[2]
                : "#" + c[0].substring(0, 2) + c[1].substring(0, 2) + c[2].substring(0, 2);
    }

    /**
     * Tells whether a character is a hexadecimal digit.
     * <p>
     * 문자가 16진수 숫자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for hex digits / 16진수 숫자이면 true
     */
    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Adds the hints of the <code>body</code> element.
     * <p>
     * <code>body</code> 요소의 힌트를 더한다.
     *
     * @param e    the element / 요소
     * @param sb   the output / 출력
     * @param base the base URL / 기준 URL
     */
    private static void body(Element e, StringBuilder sb, String base) {
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        LegacyAttributes.color(e, "text", "color", sb);
        LegacyAttributes.background(e, sb);
        bodyMargin(e, "marginwidth", "leftmargin", "margin-left", sb);
        bodyMargin(e, "marginwidth", "rightmargin", "margin-right", sb);
        bodyMargin(e, "marginheight", "topmargin", "margin-top", sb);
        bodyMargin(e, "marginheight", "bottommargin", "margin-bottom", sb);
    }

    /**
     * Maps the first existing body margin attribute. An invalid primary value must not fall through to the secondary one;
     * no hint is emitted, leaving the UA default of 8px or a user/author override. Frame-container inheritance is P20-02.
     * <p>처음 존재하는 body 여백 속성을 대응시킨다. 잘못된 상위 값은 하위 속성으로 내려가지 않고 힌트를 생략하여 UA 기본 8px 또는 사용자·작성자 재정의를 따른다.
     * 컨테이너 프레임 속성 상속은 P20-02에서 연결한다.
     * @param body body element / 본문 요소
     * @param primary preferred attribute / 우선 속성
     * @param secondary fallback only when primary is absent / 우선 속성이 없을 때만 쓰는 속성
     * @param property CSS margin property / CSS 여백 속성
     * @param output declaration output / 선언 출력
     */
    private static void bodyMargin(Element body, String primary, String secondary, String property, StringBuilder output) {
        String raw = body.getAttribute(primary);
        // 본문에 속성이 없으면 문서를 담은 frame/iframe 요소의 같은 속성이 다음 순서이다(HTML5 REC 2014-10-28 10.3.2).
        if (raw == null) {
            Document owner = body.getOwnerDocument();
            Element container = owner == null ? null : owner.getContainerFrame();
            raw = container == null ? null : container.getAttribute(primary);
        }
        if (raw == null) {
            raw = body.getAttribute(secondary);
        }
        String length = LegacyAttributes.pixels(raw);
        if (length != null) {
            LegacyAttributes.add(output, property, length);
        }
    }

    /**
     * The thickness of the border between the frames of a frameset when the page does not say (browsers use 6 pixels).
     * <p>
     * 페이지가 정하지 않았을 때 frameset 의 프레임 사이 경계선의 두께(브라우저는 6 픽셀을 쓴다).
     */
    private static final int FRAMESET_BORDER = 6;

    /**
     * Maps the attributes of a <code>frameset</code>: <code>cols</code> and <code>rows</code> become the track lists of the grid that lays
     * the cells out (HTML5 REC 2014-10-28 10.6; see {@link FramesetTracks} for the differences), and the color of the borders, which is
     * also what an empty cell shows, is the <code>bordercolor</code> of the element or of its parent frameset (gray). The borders themselves
     * belong to the frames ({@link #frame}).
     * <p>
     * <code>frameset</code> 의 속성을 대응시킨다: <code>cols</code>, <code>rows</code> 는 칸을 배치하는 그리드의 트랙 목록이 되고(HTML5 REC 2014-10-28 10.6,
     * 차이는 {@link FramesetTracks} 참조), 빈 칸이 보여 주기도 하는 경계선 색은 요소 또는 부모 frameset 의 <code>bordercolor</code>(회색)이다. 경계선
     * 자체는 프레임의 것이다({@link #frame}).
     *
     * @param e  the frameset / frameset
     * @param sb the output / 출력
     */
    private static void frameset(Element e, StringBuilder sb) {
        LegacyAttributes.add(sb, "grid-template-columns", FramesetTracks.toTracks(e.getAttribute("cols")));
        LegacyAttributes.add(sb, "grid-template-rows", FramesetTracks.toTracks(e.getAttribute("rows")));
        LegacyAttributes.add(sb, "background-color", framesetColor(e));
    }

    /**
     * Maps the attributes of a <code>frame</code>: when the frame has a border (HTML5 REC 2014-10-28 10.6) its cell gets an inner border of
     * half the thickness that the frameset says, in the frame border color, so that two neighbours show the whole thickness between them.
     * <p>
     * <code>frame</code> 의 속성을 대응시킨다: 프레임에 경계선이 있으면(HTML5 REC 2014-10-28 10.6) 칸 안쪽에 frameset 이 정한 두께의 절반으로 프레임 경계선 색의
     * 경계선을 그려, 이웃한 두 프레임 사이에 전체 두께가 보이게 한다.
     *
     * @param e  the frame / frame
     * @param sb the output / 출력
     */
    private static void frame(Element e, StringBuilder sb) {
        Node parent = e.getParentNode();
        if (!(parent instanceof Element) || !"frameset".equals(((Element) parent).getLocalName()) || !hasFrameBorder(e)) {
            return;
        }
        int thickness = FRAMESET_BORDER;
        Node n = parent;
        // 가장 가까운 frameset 부터 올라가며 처음 나오는 border(없으면 framespacing)를 두께로 쓴다.
        while (n instanceof Element && "frameset".equals(((Element) n).getLocalName())) {
            int own = intValue(((Element) n).getAttribute("border"));
            if (own < 0) {
                own = intValue(((Element) n).getAttribute("framespacing"));
            }
            if (own >= 0) {
                thickness = own;
                break;
            }
            n = n.getParentNode();
        }
        String own = legacyColor(e.getAttribute("bordercolor"));
        LegacyAttributes.add(sb, "border-width", (thickness / 2.0) + "px");
        LegacyAttributes.add(sb, "border-style", "solid");
        LegacyAttributes.add(sb, "border-color", own != null ? own : framesetColor((Element) parent));
    }

    /**
     * Parses a legacy non-negative integer from its leading digits (used for the border thickness of framesets).
     * <p>
     * 앞쪽 숫자로 이전 방식의 음이 아닌 정수를 읽는다(frameset 의 경계선 두께에 쓴다).
     *
     * @param s the attribute value / 속성 값
     * @return the integer or -1 if invalid / 정수. 올바르지 않으면 -1
     */
    private static int intValue(String s) {
        if (s == null) {
            return -1;
        }
        String t = s.trim();
        int i = 0;
        // 앞쪽 숫자만 읽는다(아홉 자리까지).
        while (i < t.length() && i < 9 && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
        }
        return i == 0 ? -1 : Integer.parseInt(t.substring(0, i));
    }

    /**
     * Returns the frame border color of a frameset: its <code>bordercolor</code> if it can be read, else that of the parent frameset, else
     * gray (HTML5 REC 2014-10-28 10.6).
     * <p>
     * frameset 의 프레임 경계선 색을 반환한다: 읽을 수 있는 <code>bordercolor</code>, 없으면 부모 frameset 의 것, 그것도 없으면 회색(HTML5 REC
     * 2014-10-28 10.6).
     *
     * @param e the frameset / frameset
     * @return a CSS color / CSS 색
     */
    private static String framesetColor(Element e) {
        Node n = e;
        // 자기 속성부터 부모 frameset 으로 올라가며 처음 읽을 수 있는 bordercolor 를 찾는다.
        while (n instanceof Element && "frameset".equals(((Element) n).getLocalName())) {
            String color = legacyColor(((Element) n).getAttribute("bordercolor"));
            if (color != null) {
                return color;
            }
            n = n.getParentNode();
        }
        return "gray";
    }

    /**
     * Tells whether a frame has a border (HTML5 REC 2014-10-28 10.6): <code>frameborder</code> that starts with 1, y or Y says yes, any other
     * value says no, and without the attribute the parent frameset decides (a frameset without a parent frameset has one).
     * <p>
     * 프레임에 경계선이 있는지 알려 준다(HTML5 REC 2014-10-28 10.6): 1, y, Y 로 시작하는 <code>frameborder</code> 는 있음, 다른 값은 없음이고, 속성이 없으면
     * 부모 frameset 이 정한다(부모 frameset 이 없으면 있음).
     *
     * @param e the frame or frameset / frame 또는 frameset
     * @return true if it has a border / 경계선이 있으면 true
     */
    private static boolean hasFrameBorder(Element e) {
        String fb = e.getAttribute("frameborder");
        if (fb != null) {
            return fb.length() > 0 && (fb.charAt(0) == '1' || fb.charAt(0) == 'y' || fb.charAt(0) == 'Y');
        }
        Node parent = e.getParentNode();
        return parent instanceof Element && "frameset".equals(((Element) parent).getLocalName()) ? hasFrameBorder((Element) parent) : true;
    }

    /**
     * Parses an HTML non-negative integer as a finite CSS pixel value: ASCII whitespace, an optional sign, then the longest
     * ASCII digit prefix. Negative zero is zero. Values outside finite double range are not emitted as CSS.
     * Source: HTML5 2014 sections 2.4.4.1/2.4.4.2 and 10.2 (retrieved 2026-11-16).
     * <p>HTML의 음이 아닌 정수를 유한 CSS 픽셀 값으로 읽는다. ASCII 공백·선택적 부호·가장 긴 ASCII 숫자 접두사를 처리하며 음의 0도 0이다.
     * 유한 double 범위 밖 값은 CSS로 내보내지 않는다. 출처는 HTML5 2014 2.4.4.1/2.4.4.2 및 10.2이다.
     * @param raw raw attribute or null / 원래 속성 또는 null
     * @return pixel length or null on failure / 픽셀 길이 또는 실패 시 null
     */
    private static String nonNegativePixels(String raw) {
        if (raw == null) {
            return null;
        }
        int position = 0;
        // HTML이 정한 ASCII 공백만 건너뛰며 NBSP 등은 숫자 접두사로 허용하지 않는다.
        while (position < raw.length() && Namespaces.isAsciiWhitespace(raw.charAt(position))) {
            position++;
        }
        boolean negative = position < raw.length() && raw.charAt(position) == '-';
        if (position < raw.length() && (negative || raw.charAt(position) == '+')) {
            position++;
        }
        int start = position;
        double value = 0;
        // 선행 0도 모두 읽어 고정 자릿수 잘림으로 숫자가 달라지지 않게 한다.
        while (position < raw.length() && raw.charAt(position) >= '0' && raw.charAt(position) <= '9') {
            value = value * 10 + (raw.charAt(position++) - '0');
        }
        if (position == start || Double.isInfinite(value) || (negative && value != 0)) {
            return null;
        }
        return Double.toString(value) + "px";
    }

    /**
     * Adds the hints of the embedded and replaced elements (HTML5 2014 section 10.4.3): <code>width</code> and <code>height</code> of
     * <code>applet</code>, <code>embed</code>, <code>iframe</code>, <code>object</code>, <code>video</code>, <code>img</code>, image
     * buttons and (a project addition) <code>canvas</code>; for <code>applet</code>, <code>embed</code>, <code>iframe</code>,
     * <code>img</code>, <code>object</code> and image buttons also <code>hspace</code> and <code>vspace</code> (the horizontal and vertical
     * margins, a length or a percentage), <code>align</code> and the <code>frameborder</code> of <code>iframe</code>; and for
     * <code>img</code>, <code>object</code> and image buttons a <code>border</code> greater than zero (a solid border of that width).
     * The <code>align</code> values <code>center</code> and <code>middle</code> put the vertical middle of the element on the baseline of
     * the parent (the non-standard keyword <code>-webkit-baseline-middle</code> that the engine supports for this purpose), whereas
     * <code>absmiddle</code> and <code>abscenter</code> use <code>vertical-align: middle</code>.
     * <p>
     * 삽입/치환 요소의 힌트를 더한다(HTML5 2014 §10.4.3): <code>applet</code>, <code>embed</code>, <code>iframe</code>, <code>object</code>, <code>video</code>,
     * <code>img</code>, 이미지 버튼과 (프로젝트 추가) <code>canvas</code> 의 <code>width</code>, <code>height</code>. <code>applet</code>, <code>embed</code>,
     * <code>iframe</code>, <code>img</code>, <code>object</code>, 이미지 버튼은 <code>hspace</code>, <code>vspace</code>(가로/세로 margin, 길이 또는 백분율),
     * <code>align</code>, <code>iframe</code> 의 <code>frameborder</code> 도 있고, <code>img</code>, <code>object</code>, 이미지 버튼은 0 보다 큰
     * <code>border</code>(그 두께의 실선 border)도 있다. <code>align</code> 의 <code>center</code>, <code>middle</code> 은 요소의 세로 가운데를 부모의 기준선에 두며
     * (엔진이 이 목적으로 지원하는 비표준 키워드 <code>-webkit-baseline-middle</code>), <code>absmiddle</code>, <code>abscenter</code> 는 <code>vertical-align: middle</code> 이다.
     *
     * @param e  the element / 요소
     * @param n  the lower-case tag name / 소문자 태그 이름
     * @param sb the output / 출력
     */
    private static void replaced(Element e, String n, StringBuilder sb) {
        LegacyAttributes.dimension(e, "width", "width", sb);
        LegacyAttributes.dimension(e, "height", "height", sb);
        // video 와 canvas 는 크기만 대응하고 나머지 속성은 절이 말하지 않는다.
        if (n.equals("video") || n.equals("canvas")) {
            return;
        }
        LegacyAttributes.dimension(e, "hspace", "margin-left", sb);
        LegacyAttributes.dimension(e, "hspace", "margin-right", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-top", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-bottom", sb);
        double border = LegacyAttributes.nonNegativeInteger(e.getAttribute("border"));
        // 0 보다 큰 border 는 두께와 실선 스타일이 된다(오류와 0 은 효과가 없다).
        if (border > 0 && (n.equals("img") || n.equals("object") || n.equals("input"))) {
            LegacyAttributes.add(sb, "border-width", LegacyAttributes.px(border));
            LegacyAttributes.add(sb, "border-style", "solid");
        }
        if (n.equals("iframe") && LegacyAttributes.keyword(e, "frameborder", "0", "no") != null) {
            LegacyAttributes.add(sb, "border", "none");
        }
        String a = LegacyAttributes.keyword(e, "align", "left", "right", "top", "baseline", "texttop", "absmiddle", "abscenter", "bottom", "center",
                "middle");
        if (a == null) {
            return;
        }
        // 정렬 키워드를 float 또는 세로 정렬로 대응한다.
        if (a.equals("left") || a.equals("right")) {
            LegacyAttributes.add(sb, "float", a);
        } else if (a.equals("texttop")) {
            LegacyAttributes.add(sb, "vertical-align", "text-top");
        } else if (a.equals("absmiddle") || a.equals("abscenter")) {
            LegacyAttributes.add(sb, "vertical-align", "middle");
        } else if (a.equals("center") || a.equals("middle")) {
            LegacyAttributes.add(sb, "vertical-align", "-webkit-baseline-middle");
        } else {
            LegacyAttributes.add(sb, "vertical-align", a);
        }
    }

    /**
     * Adds independent color, font-family and legacy font-size hints (HTML5 2014 section 10.3.4).
     * Parses the entire face value before serializing it, so comments or delimiters cannot affect adjacent hints.
     * Source: https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#phrasing-content-0 (retrieved 2026-11-17).
     * <p>
     * <code>font</code>의 색·글꼴 목록·크기 힌트를 독립적으로 더한다. face 전체를 먼저 파싱·직렬화하여 주석·구분자가 이웃 힌트에 영향을 주지 않게 한다.
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    private static void font(Element e, StringBuilder sb) {
        LegacyAttributes.color(e, "color", "color", sb);
        String face = e.getAttribute("face");
        if (face != null) {
            Map<Integer, CssValue> parsed = new HashMap<Integer, CssValue>();
            // 속성은 선언문이 아닌 단일 CSS 값이다. 인용된 구분자는 허용하고 추가 선언은 거부한다.
            if (PropertyRegistry.getInstance().parse("font-family", face, parsed) == PropertyRegistry.OK) {
                LegacyAttributes.add(sb, "font-family", parsed.get(Integer.valueOf(Prop.FONT_FAMILY)).toCssText());
            }
        }
        String size = e.getAttribute("size");
        if (size == null) {
            return;
        }
        int position = 0;
        // 부호 전의 HTML ASCII 공백만 건너뛰며 부호 뒤 공백은 숫자 수집을 실패시킨다.
        while (position < size.length() && Namespaces.isAsciiWhitespace(size.charAt(position))) {
            position++;
        }
        int mode = 0;
        if (position < size.length() && (size.charAt(position) == '+' || size.charAt(position) == '-')) {
            mode = size.charAt(position++) == '+' ? 1 : -1;
        }
        int digits = position;
        int level = 0;
        // 7 이상은 최종 범위 제한 결과가 같으므로 포화시켜 임의 길이 정수의 오버플로를 피한다.
        while (position < size.length() && size.charAt(position) >= '0' && size.charAt(position) <= '9') {
            level = Math.min(7, level * 10 + size.charAt(position++) - '0');
        }
        if (position == digits) {
            return;
        }
        // 상대 크기는 부모 글꼴이나 basefont가 아닌 고정 기준 3에 더하거나 뺀다.
        if (mode != 0) {
            level = 3 + mode * level;
        }
        String[] names = {"x-small", "x-small", "small", "medium", "large", "x-large", "xx-large", "xxx-large"};
        level = Math.max(1, Math.min(7, level));
        LegacyAttributes.add(sb, "font-size", names[level]);
    }

    /**
     * Adds the hints of the <code>marquee</code> element (HTML5 2014 section 10.5.10): <code>bgcolor</code> (the background color),
     * <code>width</code> and <code>height</code> (dimensions), <code>hspace</code> and <code>vspace</code> (the horizontal and vertical
     * margins) and the natural height of 200 pixels of a marquee whose <code>direction</code> is <code>up</code> or <code>down</code>
     * (a valid <code>height</code> attribute overrides it). The engine shows the box without animation (project policy).
     * <p>
     * <code>marquee</code> 요소의 힌트를 더한다(HTML5 2014 §10.5.10): <code>bgcolor</code>(배경색), <code>width</code>, <code>height</code>(차원 값),
     * <code>hspace</code>, <code>vspace</code>(가로/세로 margin)와 <code>direction</code> 이 <code>up</code> 이나 <code>down</code> 인 marquee 의 자연 높이 200 화소
     * (올바른 <code>height</code> 속성이 이를 덮어쓴다). 엔진은 애니메이션 없이 상자만 보여 준다(프로젝트 정책).
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    private static void marquee(Element e, StringBuilder sb) {
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        String direction = LegacyAttributes.keyword(e, "direction", "left", "right", "up", "down");
        // 세로 방향 marquee 의 자연 높이는 200 화소이며 height 속성이 있으면 아래 선언이 덮어쓴다.
        if ("up".equals(direction) || "down".equals(direction)) {
            LegacyAttributes.add(sb, "height", "200px");
        }
        LegacyAttributes.dimension(e, "width", "width", sb);
        LegacyAttributes.dimension(e, "height", "height", sb);
        LegacyAttributes.dimension(e, "hspace", "margin-left", sb);
        LegacyAttributes.dimension(e, "hspace", "margin-right", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-top", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-bottom", sb);
    }

    /**
     * Adds the hints of the <code>br</code> element: <code>clear</code> with the values <code>left</code>, <code>right</code>,
     * <code>all</code> (as <code>both</code>) and <code>both</code> (HTML5 2014 section 10.3.4).
     * <p>
     * <code>br</code> 요소의 힌트를 더한다: <code>clear</code> 의 <code>left</code>, <code>right</code>, <code>all</code>(<code>both</code> 로), <code>both</code>
     * 값(HTML5 2014 §10.3.4).
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    private static void clear(Element e, StringBuilder sb) {
        String c = LegacyAttributes.keyword(e, "clear", "left", "right", "all", "both");
        if (c != null) {
            LegacyAttributes.add(sb, "clear", c.equals("all") ? "both" : c);
        }
    }

    /**
     * Adds the hints of the <code>hr</code> element (HTML5 2014 section 10.3.12): <code>width</code>, <code>align</code> (the margins),
     * <code>color</code> (the text color, which is the default border color) and <code>size</code>. An <code>hr</code> with a
     * <code>color</code> or a <code>noshade</code> attribute (present, even when empty) has a solid border, and its <code>size</code>
     * divided by two is the width of the four borders. Otherwise a <code>size</code> of one removes the bottom border and a larger one
     * gives a content height of the size minus two.
     * <p>
     * <code>hr</code> 요소의 힌트를 더한다(HTML5 2014 §10.3.12): <code>width</code>, <code>align</code>(margin), <code>color</code>(글자색이며 기본 border 색),
     * <code>size</code>. <code>color</code> 나 <code>noshade</code> 속성이 있는(비어 있어도) <code>hr</code> 은 실선 border 를 가지며 <code>size</code> 의 절반이
     * 네 border 의 두께이다. 그렇지 않으면 <code>size</code> 1 은 아래 border 를 없애고 더 큰 값은 size 에서 2 를 뺀 내용 높이를 준다.
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    private static void hr(Element e, StringBuilder sb) {
        LegacyAttributes.dimension(e, "width", "width", sb);
        String a = LegacyAttributes.keyword(e, "align", "left", "right", "center");
        // align 은 좌우 margin 으로 가로줄의 위치를 정한다.
        if ("left".equals(a)) {
            LegacyAttributes.add(sb, "margin-left", "0");
            LegacyAttributes.add(sb, "margin-right", "auto");
        } else if ("right".equals(a)) {
            LegacyAttributes.add(sb, "margin-left", "auto");
            LegacyAttributes.add(sb, "margin-right", "0");
        } else if ("center".equals(a)) {
            LegacyAttributes.add(sb, "margin-left", "auto");
            LegacyAttributes.add(sb, "margin-right", "auto");
        }
        boolean flat = e.hasAttribute("color") || e.hasAttribute("noshade");
        if (flat) {
            LegacyAttributes.add(sb, "border-style", "solid");
        }
        double size = LegacyAttributes.nonNegativeInteger(e.getAttribute("size"));
        if (size >= 0) {
            if (flat) {
                LegacyAttributes.add(sb, "border-width", LegacyAttributes.px(size / 2));
            } else if (size == 1) {
                LegacyAttributes.add(sb, "border-bottom-width", "0");
            } else if (size > 1) {
                LegacyAttributes.add(sb, "height", LegacyAttributes.px(size - 2));
            }
        }
        LegacyAttributes.color(e, "color", "color", sb);
    }

    /**
     * Adds the <code>list-style-type</code> of a <code>type</code> attribute (HTML5 2014 section 10.3.8). The presentational hints are
     * <code>ol[type=1|a|A|i|I]</code> (case-sensitive), <code>ul[type=disc|circle|square i]</code> and the union of both for
     * <code>li</code>: an <code>li</code> follows its own <code>type</code> whatever its parent is, and a value that its list kind does
     * not know has no effect.
     * <p>
     * <code>type</code> 속성의 <code>list-style-type</code> 을 더한다(HTML5 2014 §10.3.8). 표현 힌트는 <code>ol[type=1|a|A|i|I]</code>(대소문자 구분),
     * <code>ul[type=disc|circle|square i]</code> 와 <code>li</code> 에 대한 둘의 합집합이다: <code>li</code> 는 부모가 무엇이든 자신의
     * <code>type</code> 을 따르고, 해당 목록 종류가 모르는 값은 효과가 없다.
     *
     * @param e  the element / 요소
     * @param n  the lower-case tag name / 소문자 태그 이름
     * @param sb the output / 출력
     */
    private static void listType(Element e, String n, StringBuilder sb) {
        String t = e.getAttribute("type");
        if (t == null || t.length() == 0) {
            return;
        }
        boolean item = n.equals("li");
        String style = null;
        // 번호 목록과 li 는 대소문자를 구분하는 한 글자 값을 따른다.
        if (item || n.equals("ol")) {
            if (t.equals("1")) {
                style = "decimal";
            } else if (t.equals("a")) {
                style = "lower-alpha";
            } else if (t.equals("A")) {
                style = "upper-alpha";
            } else if (t.equals("i")) {
                style = "lower-roman";
            } else if (t.equals("I")) {
                style = "upper-roman";
            }
        }
        // 글머리 기호 목록과 li 는 ASCII 대소문자를 구분하지 않는 이름을 따른다.
        if (style == null && (item || n.equals("ul"))) {
            String l = Namespaces.asciiLower(t);
            if (l.equals("disc") || l.equals("circle") || l.equals("square")) {
                style = l;
            }
        }
        if (style != null) {
            LegacyAttributes.add(sb, "list-style-type", style);
        }
    }
}
