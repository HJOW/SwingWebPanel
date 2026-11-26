package org.duckdns.hjow.swingwebpanel.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;

/**
 * Collects the layout text read by <code>HTMLElement.innerText</code> (HTML §3.2.7).
 * <p>
 * HTML §3.2.7의 <code>HTMLElement.innerText</code>가 읽는 배치 텍스트를 모은다.
 */
public final class RenderedText {

    /** Orders a text node's layout fragments by source offset. / 텍스트 노드의 배치 조각을 원문 위치 순으로 정렬한다. */
    private static final Comparator<LayoutBox> CONTENT_ORDER = new Comparator<LayoutBox>() {
        /** Compares starting offsets. / 시작 위치를 비교한다. */
        public int compare(LayoutBox a, LayoutBox b) {
            return a.getSourceStart() - b.getSourceStart();
        }
    };

    /** Boxes from the current synchronous document layout. / 현재 문서의 동기 배치에서 얻은 상자. */
    private final LayoutResult layout;
    /** Current styles for nodes with no own principal box. / 주 상자가 없는 노드의 현재 계산 스타일. */
    private final StyleResolver styles;
    /** Collected characters. / 모은 문자. */
    private final StringBuilder output = new StringBuilder();
    /** The largest pending required break, emitted only between nonempty items. / 비어 있지 않은 항목 사이에 출력할 최대 필수 줄바꿈 수. */
    private int breaks;
    /** A collapsed source space pending before the next inline item. / 다음 인라인 항목 전에 남겨 둔 접힌 원문 공백. */
    private boolean space;

    /**
     * Creates a collector for one layout. / 한 배치 결과용 수집기를 만든다.
     * @param layout the current layout / 현재 배치
     * @param styles the active style resolver / 활성 스타일 리졸버
     */
    private RenderedText(LayoutResult layout, StyleResolver styles) {
        this.layout = layout;
        this.styles = styles;
    }

    /**
     * Reads the element's rendered text, falling back to DOM text when its own box is absent.
     * <p>
     * 요소의 렌더링 텍스트를 읽으며 자기 상자가 없으면 DOM 텍스트를 읽는다.
     * @param element the queried HTML element / 조회할 HTML 요소
     * @param layout the current layout / 현재 배치
     * @param styles the active style resolver / 활성 스타일 리졸버
     * @return the text of the element / 요소의 텍스트
     */
    public static String get(Element element, LayoutResult layout, StyleResolver styles) {
        RenderedText collector = new RenderedText(layout, styles);
        LayoutBox box = collector.boxOf(element);
        if (box == null) {
            return element.getTextContent();
        }
        if (html(element, "select")) {
            collector.options(element);
            return collector.output.toString();
        }
        return box.getType() == LayoutBox.Type.REPLACED ? "" : collector.children(element);
    }

    /**
     * Visits descendants in DOM order without recursing on the Java stack. / 자손을 DOM 순서로 방문한다. Java 스택에서 재귀를 쓰지 않는다.
     * @param root the rendered element / 렌더링된 요소
     * @return the collected text / 모은 텍스트
     */
    private String children(Element root) {
        Node node = root.getFirstChild();
        // 요소 진입·이탈에서 필요한 구분자를 모으고 실제 텍스트는 원문 순서로 읽는다.
        while (node != null) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (enter(element)) {
                    if (node.hasChildNodes()) {
                        node = node.getFirstChild();
                        continue;
                    }
                    leave(element);
                }
            } else if (node instanceof Text) {
                text((Text) node);
            }
            // 다음 형제가 없으면 이미 방문한 부모를 닫으면서 루트까지 되돌아간다.
            while (node.getNextSibling() == null) {
                node = node.getParentNode();
                if (node == root) {
                    return output.toString();
                }
                leave((Element) node);
            }
            node = node.getNextSibling();
        }
        return output.toString();
    }

    /**
     * Adds a leading boundary and chooses whether to visit descendants. / 시작 구분자를 기록하고 자손 방문 여부를 정한다.
     * @param element the element being entered / 방문을 시작하는 요소
     * @return whether to visit the element's descendants / 자손을 방문할지 여부
     */
    private boolean enter(Element element) {
        LayoutBox box = boxOf(element);
        ComputedStyle style = box == null ? styles.getComputedStyle(element) : box.getStyle();
        // display:none 하위는 CSS 상자를 생성하지 않는다. visibility:hidden의 자손은 다시 visible이 될 수 있다.
        if ("none".equals(style.display())) {
            return false;
        }
        if (html(element, "br")) {
            if (visible(style) && !"contents".equals(style.display())) {
                append("\n");
            }
            return false;
        }
        if (box == null) {
            return "contents".equals(style.display());
        }
        if (visible(style)) {
            require(boundary(element, box));
        }
        if (html(element, "select")) {
            options(element);
            if (visible(style)) {
                require(boundary(element, box));
            }
            return false;
        }
        // 치환 요소의 텍스트는 일반 CSS 자식 상자로 배치되지 않는다.
        return box.getType() != LayoutBox.Type.REPLACED;
    }

    /**
     * Collects the virtual option boxes of a select or its groups. / select와 optgroup의 가상 option 상자를 모은다.
     * @param control a select or optgroup / select 또는 optgroup
     */
    private void options(Element control) {
        // 폼 컨트롤은 단일 배치 상자로 그려지지만 innerText에서는 표시되는 옵션을 별도로 수집한다.
        for (Node node = control.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            ComputedStyle style = styles.getComputedStyle(element);
            if ("none".equals(style.display())) {
                continue;
            }
            if (node instanceof HtmlOptionElement && visible(style)) {
                require(1);
                append(((HtmlOptionElement) node).getText());
                require(1);
            } else if (html(control, "select") && html(element, "optgroup")) {
                options(element);
            }
        }
    }

    /**
     * Queues a trailing boundary or adds the separator of a table part. / 끝 구분자를 예약하거나 표 부분의 구분자를 더한다.
     * @param element the element just visited / 방문을 마친 요소
     */
    private void leave(Element element) {
        LayoutBox box = boxOf(element);
        if (box == null || !visible(box.getStyle())) {
            return;
        }
        if (box.getType() == LayoutBox.Type.TABLE_CELL && followedBy(box, LayoutBox.Type.TABLE_CELL)) {
            append("\t");
        } else if (box.getType() == LayoutBox.Type.TABLE_ROW && (followedBy(box, LayoutBox.Type.TABLE_ROW)
                || followedBy(box.getParent(), LayoutBox.Type.TABLE_ROW_GROUP))) {
            append("\n");
        }
        require(boundary(element, box));
    }

    /**
     * Adds the visible fragments of a text node in source order. / 텍스트 노드의 보이는 조각을 원문 순서로 더한다.
     * @param text the text node / 텍스트 노드
     */
    private void text(Text text) {
        List<LayoutBox> boxes = layout.getBoxes(text);
        if (boxes.size() > 1) {
            boxes = new ArrayList<LayoutBox>(boxes);
            Collections.sort(boxes, CONTENT_ORDER);
        }
        String source = text.getData();
        ComputedStyle style = styles.getComputedStyle((Element) text.getParentNode());
        if (!visible(style)) {
            return;
        }
        String whiteSpace = style.keyword(Prop.WHITE_SPACE);
        boolean keepLines = "pre".equals(whiteSpace) || "pre-wrap".equals(whiteSpace)
                || "pre-line".equals(whiteSpace) || "break-spaces".equals(whiteSpace);
        int end = 0;
        // 배치 조각의 변환된 문자열을 사용하고 그 사이에서 접히거나 줄을 나눈 원문 공백을 확인한다.
        for (LayoutBox box : boxes) {
            if (box.getType() == LayoutBox.Type.TEXT && box.getPseudo() == null && visible(box.getStyle()) && box.getText() != null
                    && box.getSourceStart() < box.getSourceEnd()) {
                gap(source, end, box.getSourceStart(), keepLines);
                append(fragmentText(box, source));
                end = box.getSourceEnd();
            }
        }
        gap(source, end, source.length(), keepLines);
    }

    /**
     * Restores source soft hyphens whose glyphs were replaced for painting. / 그리기용 글리프로 바뀐 원문 소프트 하이픈을 복원한다.
     * @param box the text fragment / 텍스트 조각
     * @param source the original node text / 노드의 원문
     * @return the logical text of the fragment / 조각의 논리적 텍스트
     */
    private static String fragmentText(LayoutBox box, String source) {
        String text = box.getText();
        int start = box.getSourceStart();
        int end = box.getSourceEnd();
        if (end > source.length() || end - start != text.length()) {
            return text;
        }
        StringBuilder restored = null;
        // 원문과 같은 길이의 조각에서는 문자 위치로 소프트 하이픈을 되살린다.
        for (int i = start; i < end; i++) {
            if (source.charAt(i) == 0x00ad) {
                if (restored == null) {
                    restored = new StringBuilder(text);
                }
                restored.setCharAt(i - start, (char) 0x00ad);
            }
        }
        return restored == null ? text : restored.toString();
    }

    /**
     * Recovers white space omitted at a line boundary in layout. / 배치의 줄 경계에서 빠진 공백을 복원한다.
     * @param text the original text / 원문
     * @param start first source offset / 시작 위치
     * @param end exclusive source offset / 끝 위치. 문자는 포함하지 않는다
     * @param keepLines whether a source newline is significant / 원문 개행을 보존할지 여부
     */
    private void gap(String text, int start, int end, boolean keepLines) {
        // 세그먼트에 표시되지 않은 원문 공백과 명시 개행만 읽는다.
        for (int i = start; i < end && i < text.length(); i++) {
            char c = text.charAt(i);
            if (keepLines && (c == '\n' || c == '\r')) {
                append("\n");
                if (c == '\r' && i + 1 < end && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (isCollapsible(c)) {
                space = true;
            } else if (c == 0x00ad) {
                append(String.valueOf(c));
            }
        }
    }

    /**
     * Adds a nonempty item with its pending separators. / 비어 있지 않은 항목과 앞에 예약한 구분자를 더한다.
     * @param item the item's characters / 항목의 문자열
     */
    private void append(String item) {
        if (item.length() == 0) {
            return;
        }
        if (output.length() != 0) {
            // 필수 줄바꿈은 이미 모은 내용과 다음 항목 사이에서만 출력한다.
            for (int i = 0; i < breaks; i++) {
                output.append('\n');
            }
            if (breaks == 0 && space && !isCollapsible(output.charAt(output.length() - 1)) && !isCollapsible(item.charAt(0))) {
                output.append(' ');
            }
        }
        breaks = 0;
        space = false;
        output.append(item);
    }

    /**
     * Queues the strongest required boundary. / 가장 많은 필수 줄바꿈을 예약한다.
     * @param count number of required line breaks / 필수 줄바꿈 수
     */
    private void require(int count) {
        breaks = Math.max(breaks, count);
        if (count != 0) {
            space = false;
        }
    }

    /**
     * Finds a non-generated box owned by an element. / 요소가 소유한 생성 콘텐츠가 아닌 상자를 찾는다.
     * @param element the element / 요소
     * @return the first principal box, or null / 첫 주 상자. 없으면 null
     */
    private LayoutBox boxOf(Element element) {
        // 의사 요소와 목록 표식은 대상 요소가 만든 주 상자가 아니다.
        for (LayoutBox box : layout.getBoxes(element)) {
            if (box.getPseudo() == null && box.getType() != LayoutBox.Type.MARKER && box.getType() != LayoutBox.Type.TEXT) {
                return box;
            }
        }
        return null;
    }

    /**
     * Checks for a following sibling with the given box type. / 지정한 종류의 뒤 형제 상자가 있는지 확인한다.
     * @param box the box before the siblings / 형제 앞 상자
     * @param kind sought box type / 찾는 상자 종류
     * @return whether a matching sibling follows / 조건에 맞는 뒤 형제가 있는지 여부
     */
    private static boolean followedBy(LayoutBox box, LayoutBox.Type kind) {
        if (box == null || box.getParent() == null) {
            return false;
        }
        List<LayoutBox> siblings = box.getParent().getChildren();
        // 마지막 상자부터 현재 상자까지 거꾸로 훑어 뒤에 같은 종류가 있는지 확인한다.
        for (int i = siblings.size() - 1; i >= 0; i--) {
            LayoutBox sibling = siblings.get(i);
            if (sibling == box) {
                return false;
            }
            if (sibling.getType() == kind) {
                return true;
            }
        }
        return false;
    }

    /** Returns the paragraph or block boundary strength. / 단락 또는 블록의 필수 줄바꿈 수를 반환한다. */
    private static int boundary(Element element, LayoutBox box) {
        if (html(element, "p")) {
            return 2;
        }
        LayoutBox.Type kind = box.getType();
        return kind == LayoutBox.Type.BLOCK || kind == LayoutBox.Type.TABLE_CAPTION
                || ((kind == LayoutBox.Type.REPLACED || kind == LayoutBox.Type.TABLE) && !box.isInlineLevel()) ? 1 : 0;
    }

    /** Checks the local name in the HTML namespace. / HTML 네임스페이스에서 로컬 이름을 확인한다. */
    private static boolean html(Element element, String localName) {
        return localName.equals(element.getLocalName()) && Namespaces.HTML.equals(element.getNamespaceURI());
    }

    /** Checks computed visibility. / 계산된 가시성을 확인한다. */
    private static boolean visible(ComputedStyle style) {
        return style == null || "visible".equals(style.keyword(Prop.VISIBILITY));
    }

    /** Identifies CSS collapsible document whitespace. / CSS에서 접히는 문서 공백을 판별한다. */
    private static boolean isCollapsible(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }
}
