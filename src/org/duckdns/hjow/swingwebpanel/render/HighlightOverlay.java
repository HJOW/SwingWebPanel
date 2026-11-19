package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Highlight;
import org.duckdns.hjow.swingwebpanel.dom.HighlightRegistry;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.StaticRange;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.layout.StyleSource;

/**
 * The custom highlights of a text fragment (CSS Custom Highlight API Level 1 sections 4.2.4 and 4.2.5, CSS Pseudo-Elements 4 section 3.6): for every
 * custom highlight registered in the {@link HighlightRegistry} of the document, the parts of the text node of the fragment that its ranges cover (collapsed
 * ranges, ranges of another document and invalid static ranges are ignored; overlapping ranges of one highlight are one area), with the style of
 * <code>::highlight(name)</code> of the parent element of the text. The layers are ordered by priority and then by registration (a later one is above), and
 * each highlight that is registered under several names is painted once per name.
 * <p>
 * The painter uses the layers like this: the backgrounds (<code>background-color</code>) are painted lowest layer first under the text; the text is painted
 * in its own color outside the highlighted parts, and inside each part in the color and with the decoration lines of the topmost layer that covers it (a
 * <code>color</code> of <code>currentcolor</code> keeps the color of the layer below, finally the color of the text).
 * <p>
 * 텍스트 조각의 사용자 정의 하이라이트(CSS Custom Highlight API Level 1 4.2.4, 4.2.5 절, CSS Pseudo-Elements 4 3.6 절): 문서의 {@link HighlightRegistry} 에 등록된 사용자
 * 정의 하이라이트마다, 그 범위가 덮는 조각의 텍스트 노드 부분(접힌 범위, 다른 문서의 범위, 올바르지 않은 정적 범위는 무시하고, 한 하이라이트의 겹치는 범위는 한 영역)과 텍스트의 부모
 * 요소의 <code>::highlight(이름)</code> 스타일. 층은 우선순위, 그다음 등록 순서로 정렬하며(나중 것이 위), 여러 이름으로 등록한 하이라이트는 이름마다 한 번씩 그린다.
 * <p>
 * 그리는 쪽은 층을 이렇게 쓴다: 배경(<code>background-color</code>)은 가장 아래 층부터 텍스트 밑에 그리고, 텍스트는 하이라이트되지 않은 부분은 자신의 색으로, 각 부분 안은 그
 * 부분을 덮는 가장 위 층의 색과 장식선으로 그린다(<code>color</code> 가 <code>currentcolor</code> 이면 아래 층의 색을, 마지막에는 텍스트의 색을 쓴다).
 */
final class HighlightOverlay {

    /**
     * One highlight layer over a text fragment: its style and the parts of the text node it covers.
     * <p>
     * 텍스트 조각 위의 하이라이트 층 하나: 스타일과 덮는 텍스트 노드의 부분.
     */
    static final class Layer {
        /** The style of <code>::highlight(name)</code>. <code>::highlight(이름)</code> 의 스타일. */
        final ComputedStyle style;
        /** The covered parts {from, to} in the data of the text node, sorted and disjoint. 텍스트 노드 데이터에서 덮는 부분 {시작, 끝}(정렬되고 겹치지 않음). */
        final List<int[]> spans;

        /**
         * Creates a layer.
         * <p>
         * 층을 만든다.
         *
         * @param style the style / 스타일
         * @param spans the parts / 부분
         */
        Layer(ComputedStyle style, List<int[]> spans) {
            this.style = style;
            this.spans = spans;
        }
    }

    /**
     * Not instantiable; the class only offers static methods.
     * <p>
     * 인스턴스를 만들지 않는다. 정적 메서드만 제공한다.
     */
    private HighlightOverlay() {
    }

    /**
     * Returns the highlight layers of a text fragment, lowest first; empty when the document has no registered highlight that covers the fragment.
     * <p>
     * 텍스트 조각의 하이라이트 층을 가장 아래 것부터 반환한다. 문서에 조각을 덮는 등록된 하이라이트가 없으면 비어 있다.
     *
     * @param result the layout (its style source gives the highlight styles) / 레이아웃(스타일 공급자가 하이라이트 스타일을 준다)
     * @param doc    the document or null / 문서 또는 null
     * @param t      the TEXT box / TEXT 상자
     * @return the layers / 층
     */
    static List<Layer> layersOf(LayoutResult result, Document doc, LayoutBox t) {
        HighlightRegistry registry = doc == null ? null : doc.peekHighlights();
        StyleSource styles = result == null ? null : result.getStyles();
        if (registry == null || registry.getSize() == 0 || styles == null || !(t.getNode() instanceof Text)) {
            return Collections.emptyList();
        }
        Text node = (Text) t.getNode();
        Node parent = node.getParentNode();
        if (!(parent instanceof Element) || !node.isConnected()) {
            return Collections.emptyList();
        }
        final List<Map.Entry<String, Highlight>> entries = registry.getEntries();
        List<Integer> order = new ArrayList<Integer>();
        // 등록 순서의 번호를 모아 우선순위로 안정 정렬한다(같으면 등록 순서).
        for (int i = 0; i < entries.size(); i++) {
            order.add(Integer.valueOf(i));
        }
        Collections.sort(order, new Comparator<Integer>() {
            /**
             * Compares two entries by priority (ties keep the registration order).
             * <p>
             * 두 항목을 우선순위로 비교한다(같으면 등록 순서를 지킨다).
             *
             * @param a an index / 번호
             * @param b another index / 다른 번호
             * @return the order / 순서
             */
            public int compare(Integer a, Integer b) {
                int pa = entries.get(a.intValue()).getValue().getPriority();
                int pb = entries.get(b.intValue()).getValue().getPriority();
                return pa < pb ? -1 : (pa > pb ? 1 : a.intValue() - b.intValue());
            }
        });
        int from = t.getSourceStart();
        int to = t.getSourceEnd();
        List<Layer> out = new ArrayList<Layer>();
        // 아래 층부터 하이라이트마다 이 조각에서 덮는 부분을 모은다.
        for (int k = 0; k < order.size(); k++) {
            Map.Entry<String, Highlight> e = entries.get(order.get(k).intValue());
            List<int[]> spans = spans(e.getValue(), node, doc, from, to);
            if (!spans.isEmpty()) {
                out.add(new Layer(styles.getHighlightStyle((Element) parent, e.getKey()), spans));
            }
        }
        return out;
    }

    /**
     * Returns the parts of a text node between two offsets that the ranges of a highlight cover, merged into disjoint sorted parts.
     * <p>
     * 하이라이트의 범위가 두 오프셋 사이의 텍스트 노드에서 덮는 부분을, 겹치지 않게 합쳐 정렬해 반환한다.
     *
     * @param h    the highlight / 하이라이트
     * @param node the text node / 텍스트 노드
     * @param doc  the document / 문서
     * @param from the start of the fragment in the data / 데이터에서 조각의 시작
     * @param to   the end of the fragment in the data / 데이터에서 조각의 끝
     * @return the parts / 부분
     */
    static List<int[]> spans(Highlight h, Text node, Document doc, int from, int to) {
        List<int[]> raw = new ArrayList<int[]>();
        List<Object> ranges = h.getRanges();
        int length = ((CharacterData) node).getLength();
        // 범위마다 텍스트 노드와 겹치는 부분을 구한다.
        for (int i = 0; i < ranges.size(); i++) {
            Object r = ranges.get(i);
            Node sc;
            int so;
            Node ec;
            int eo;
            if (r instanceof Range) {
                Range lr = (Range) r;
                sc = lr.getStartContainer();
                so = lr.getStartOffset();
                ec = lr.getEndContainer();
                eo = lr.getEndOffset();
            } else {
                StaticRange st = (StaticRange) r;
                sc = st.getStartContainer();
                so = st.getStartOffset();
                ec = st.getEndContainer();
                eo = st.getEndOffset();
                // 올바르지 않은 정적 범위(오프셋이 노드 길이를 넘음, 시작이 끝 뒤)는 무시한다(5.2 절).
                if (so > lengthOf(sc) || eo > lengthOf(ec) || Range.comparePointOrder(sc, so, ec, eo) > 0) {
                    continue;
                }
            }
            // 다른 문서의 노드를 가리키거나 접힌 범위는 그리지 않는다(4.2.4, 5.2 절).
            if (!inDocument(sc, doc) || !inDocument(ec, doc) || (sc == ec && so == eo)) {
                continue;
            }
            int a = sc == node ? so : (Range.comparePointOrder(sc, so, node, 0) <= 0 ? 0 : length + 1);
            int b = ec == node ? eo : (Range.comparePointOrder(ec, eo, node, length) >= 0 ? length : -1);
            a = Math.max(a, from);
            b = Math.min(b, to);
            if (a < b) {
                raw.add(new int[] { a, b });
            }
        }
        Collections.sort(raw, new Comparator<int[]>() {
            /**
             * Orders parts by their start.
             * <p>
             * 부분을 시작으로 정렬한다.
             *
             * @param x a part / 부분
             * @param y another part / 다른 부분
             * @return the order / 순서
             */
            public int compare(int[] x, int[] y) {
                return x[0] - y[0];
            }
        });
        List<int[]> merged = new ArrayList<int[]>();
        // 겹치거나 맞닿는 부분을 하나로 합친다(한 하이라이트의 겹치는 범위는 하나의 범위처럼 그린다).
        for (int i = 0; i < raw.size(); i++) {
            int[] s = raw.get(i);
            if (!merged.isEmpty() && s[0] <= merged.get(merged.size() - 1)[1]) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], s[1]);
            } else {
                merged.add(new int[] { s[0], s[1] });
            }
        }
        return merged;
    }

    /**
     * Returns the length of a node as a boundary point container (the data length of character data, else the number of children).
     * <p>
     * 경계점 컨테이너로서 노드의 길이를 반환한다(문자 데이터는 데이터 길이, 아니면 자식 수).
     *
     * @param n the node / 노드
     * @return the length / 길이
     */
    private static int lengthOf(Node n) {
        if (n instanceof CharacterData) {
            return ((CharacterData) n).getLength();
        }
        int c = 0;
        // 자식을 센다.
        for (Node k = n.getFirstChild(); k != null; k = k.getNextSibling()) {
            c++;
        }
        return c;
    }

    /**
     * Tells whether a node belongs to a document and is connected to it.
     * <p>
     * 노드가 문서에 속하고 연결되어 있는지 알려 준다.
     *
     * @param n   the node / 노드
     * @param doc the document / 문서
     * @return true if so / 그러면 true
     */
    private static boolean inDocument(Node n, Document doc) {
        return n != null && (n == doc || (n.getOwnerDocument() == doc && n.isConnected()));
    }

    /**
     * Returns the decoration bits that a highlight style adds (<code>text-decoration-line</code>).
     * <p>
     * 하이라이트 스타일이 더하는 장식 비트(<code>text-decoration-line</code>)를 반환한다.
     *
     * @param s the style / 스타일
     * @return the bits of {@link LayoutBox#DECORATION_UNDERLINE} and the others / {@link LayoutBox#DECORATION_UNDERLINE} 등의 비트
     */
    static int decorationOf(ComputedStyle s) {
        CssValue v = s.get(Prop.TEXT_DECORATION_LINE);
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : Collections.singletonList(v);
        int bits = 0;
        // 키워드마다 해당하는 비트를 켠다.
        for (int i = 0; i < items.size(); i++) {
            CssValue k = items.get(i);
            if (k == null) {
                continue;
            }
            if (k.isKeyword("underline")) {
                bits |= LayoutBox.DECORATION_UNDERLINE;
            } else if (k.isKeyword("overline")) {
                bits |= LayoutBox.DECORATION_OVERLINE;
            } else if (k.isKeyword("line-through")) {
                bits |= LayoutBox.DECORATION_LINE_THROUGH;
            }
        }
        return bits;
    }

    /**
     * Returns the color of a highlight property, resolving <code>currentcolor</code> to a given color.
     * <p>
     * 하이라이트 속성의 색을 반환하며 <code>currentcolor</code> 는 주어진 색으로 푼다.
     *
     * @param s            the style / 스타일
     * @param id           the property / 속성
     * @param currentColor the color that <code>currentcolor</code> stands for / <code>currentcolor</code> 가 뜻하는 색
     * @return the ARGB color / ARGB 색
     */
    static int colorOf(ComputedStyle s, int id, int currentColor) {
        CssValue v = s.get(id);
        if (v == null || v.isKeyword("currentcolor")) {
            return currentColor;
        }
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssColor) {
            return ((org.duckdns.hjow.swingwebpanel.css.values.CssColor) v).getArgb();
        }
        return currentColor;
    }

    /**
     * The text of a fragment painted with its highlights: the base text outside the highlighted parts and, inside each part, the text in the color and the
     * decorations of the topmost layer.
     * <p>
     * 하이라이트와 함께 그리는 조각의 텍스트: 하이라이트된 부분 밖은 원래 텍스트, 각 부분 안은 가장 위 층의 색과 장식의 텍스트.
     */
    static final class Painted extends DisplayCommand {
        /** The text in its own color. 자신의 색의 텍스트. */
        private final DisplayCommand base;
        /** The highlighted parts. 하이라이트된 부분. */
        private final List<Shape> areas;
        /** The text of each part. 부분마다의 텍스트. */
        private final List<DisplayCommand> texts;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param base  the text in its own color / 자신의 색의 텍스트
         * @param areas the highlighted parts / 하이라이트된 부분
         * @param texts the text of each part / 부분마다의 텍스트
         * @param node  the DOM node / DOM 노드
         */
        Painted(DisplayCommand base, List<Shape> areas, List<DisplayCommand> texts, Node node) {
            super(Type.TEXT, base.getBounds().getX(), base.getBounds().getY(), base.getBounds().getWidth(), base.getBounds().getHeight(), node);
            this.base = base;
            this.areas = areas;
            this.texts = texts;
        }

        /**
         * Paints the base text clipped outside the parts, then every part clipped to its area.
         * <p>
         * 원래 텍스트를 부분 밖으로 잘라 그리고, 부분마다 그 영역으로 잘라 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Shape old = g.getClip();
            try {
                Rectangle2D b = getBounds();
                Area outside = new Area(new Rectangle2D.Double(b.getX() - 4, b.getY() - 4, b.getWidth() + 8, b.getHeight() + 8));
                // 하이라이트된 부분을 빼서 원래 텍스트가 그려질 영역을 만든다.
                for (int i = 0; i < areas.size(); i++) {
                    outside.subtract(new Area(areas.get(i)));
                }
                g.clip(outside);
                base.paint(ctx);
                // 부분마다 그 영역 안에서 하이라이트의 텍스트를 그린다.
                for (int i = 0; i < areas.size(); i++) {
                    g.setClip(old);
                    g.clip(areas.get(i));
                    texts.get(i).paint(ctx);
                }
            } finally {
                g.setClip(old);
            }
        }
    }
}
