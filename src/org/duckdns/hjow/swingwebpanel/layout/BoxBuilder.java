package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;

/**
 * Builds the box tree from the DOM (plan item P09-01): <code>display:none</code> elements are skipped, <code>display:contents</code>
 * elements contribute only their children, block-level and inline-level content are separated by anonymous block boxes, inline content is
 * flattened into {@link InlineItem}s, <code>::before</code>/<code>::after</code> content and list markers are generated and replaced
 * elements become {@link LayoutBox.Type#REPLACED} boxes. A block-level box inside an inline element splits the inline element around it.
 * Nesting deeper than {@link #MAX_DEPTH} levels is not built (the deeper content is dropped), so very deep documents cannot overflow the
 * Java stack. Tables (P10-02) get anonymous table parts, flex containers (P10-03) get flex items (blockified children, anonymous items
 * for text, children sorted by <code>order</code>) and multi-column containers are marked (children with <code>column-span:all</code>
 * split them into sections) and grid containers get grid items exactly like flex containers get flex items.
 * <p>
 * DOM 에서 상자 트리를 만든다(계획 P09-01): <code>display:none</code> 요소는 건너뛰고, <code>display:contents</code> 요소는 자식만 기여하며, 블록 수준과
 * 인라인 수준 콘텐츠는 익명 블록 상자로 나누고, 인라인 콘텐츠는 {@link InlineItem} 으로 펼치며, <code>::before</code>/<code>::after</code> 콘텐츠와
 * 목록 표식을 생성하고, 치환 요소는 {@link LayoutBox.Type#REPLACED} 상자가 된다. 인라인 요소 안의 블록 수준 상자는 인라인 요소를 둘로 나눈다.
 * {@link #MAX_DEPTH} 단계보다 깊은 중첩은 만들지 않으므로(더 깊은 콘텐츠는 버림) 아주 깊은 문서도 Java 스택을 넘치게 하지 않는다.
 * 표(P10-02)는 익명 표 부품을 얻고 플렉스 컨테이너(P10-03)는 플렉스 항목(블록화한 자식, 텍스트의 익명 항목, <code>order</code> 로 정렬한 자식)을 얻으며
 * 다단 컨테이너(P10-03)는 표시하며(<code>column-span:all</code> 자식이 구간을 나눈다) 그리드 컨테이너는 플렉스 컨테이너가 플렉스 항목을 얻는 것처럼 그리드 항목을 얻는다.
 */
final class BoxBuilder {

    /** Maximum nesting depth of boxes that is built. 만드는 상자 중첩의 최대 깊이. */
    static final int MAX_DEPTH = 512;

    /** Source of styles. 스타일 공급자. */
    private final StyleSource styles;
    /** Source of replaced content, may be null. 치환 콘텐츠 공급자(null 가능). */
    private final ReplacedResolver resolver;
    /** List counters by parent element: next number and step. 부모 요소별 목록 카운터: 다음 번호와 증분. */
    private final Map<Node, int[]> counters = new IdentityHashMap<Node, int[]>();
    /** The counter styles of the document: the predefined ones and the <code>@counter-style</code> definitions. 문서의 카운터 스타일: 미리 정의된 것과 <code>@counter-style</code> 정의. */
    private final CounterStyles counterStyles;
    /** The generated content and the CSS counters (P10-04). 생성 콘텐츠와 CSS 카운터(P10-04). */
    private final GeneratedContent gc;
    /** Sequence counter that numbers the boxes in tree order. 상자를 트리 순서로 매기는 번호 계수기. */
    private int seq;
    /** The nearest positioned ancestor box while the tree is built (null: none, the initial containing block applies). 트리를 만드는 동안 가장 가까운 위치 지정 조상 상자(null: 없음, 초기 포함 블록). */
    private LayoutBox cb;
    /** The nearest transformed ancestor box that is being filled: the containing block of fixed boxes inside it (P10-06). 채우는 중인 가장 가까운 변환된 조상 상자: 그 안의 fixed 상자의 포함 블록(P10-06). */
    private LayoutBox tfCb;
    /** Absolutely positioned boxes whose containing block is the initial containing block or the viewport. 포함 블록이 초기 포함 블록이나 뷰포트인 절대 배치 상자. */
    private final List<LayoutBox> icbList = new ArrayList<LayoutBox>();
    /** The rendered elements of the top layer of the document in order (CSS Positioned Layout 4 section 3), or null. 문서의 최상위 계층에서 그려지는 요소(순서대로, CSS Positioned Layout 4 3 절). 없으면 null. */
    private List<Element> topLayer;
    /** True while a top layer element is added at the end of the root (it is skipped where it is in the tree). 최상위 계층 요소를 루트 끝에 더하는 동안 true(트리의 제자리에서는 건너뛴다). */
    private boolean addingTopLayer;

    /**
     * Creates a builder.
     * <p>
     * 빌더를 만든다.
     *
     * @param styles   style source / 스타일 공급자
     * @param resolver replaced content source or null / 치환 콘텐츠 공급자 또는 null
     */
    BoxBuilder(StyleSource styles, ReplacedResolver resolver) {
        this.styles = styles;
        this.resolver = resolver;
        this.counterStyles = CounterStyles.of(styles);
        this.gc = new GeneratedContent(counterStyles);
    }

    /**
     * Returns the external content of a replaced element from the resolver, for the picture as stored in the file if the style is
     * <code>image-orientation: none</code> and the content has an orientation of its metadata applied (CSS Images 3, section 5.1).
     * <p>
     * 치환 요소의 외부 콘텐츠를 리졸버에서 받아 반환한다. 스타일이 <code>image-orientation: none</code> 이고 콘텐츠에 메타데이터의 방향이 적용되어
     * 있으면 파일에 저장된 그대로의 그림의 콘텐츠를 반환한다(CSS Images 3, 5.1절).
     *
     * @param e the replaced element / 치환 요소
     * @param s its style / 요소의 스타일
     * @return the content, or null if nothing is known / 콘텐츠. 알려진 것이 없으면 null
     */
    private ReplacedContent replacedContent(Element e, ComputedStyle s) {
        ReplacedContent content = resolver == null ? null : resolver.getReplacedContent(e);
        if (content instanceof OrientedContent && s.isKeyword(Prop.IMAGE_ORIENTATION, "none")) {
            return ((OrientedContent) content).withoutOrientation();
        }
        return content;
    }

    /**
     * Returns the absolutely positioned boxes whose containing block is the initial containing block (or, for fixed boxes, the viewport).
     * <p>
     * 포함 블록이 초기 포함 블록(fixed 상자는 뷰포트)인 절대 배치 상자를 반환한다.
     *
     * @return the boxes in source order / 소스 순서의 상자
     */
    List<LayoutBox> getInitialAbsolute() {
        return icbList;
    }

    /**
     * Returns the float side of a style (<code>left</code>, <code>right</code>; <code>inline-start</code> and <code>inline-end</code> follow
     * the left-to-right direction).
     * <p>
     * 스타일의 float 방향을 반환한다(<code>left</code>, <code>right</code>. <code>inline-start</code>, <code>inline-end</code> 는 왼쪽에서 오른쪽 방향을 따른다).
     *
     * @param s the style / 스타일
     * @return <code>LayoutBox.FLOAT_*</code> / <code>LayoutBox.FLOAT_*</code>
     */
    static int floatSideOf(ComputedStyle s) {
        String f = Css.kw(s, Prop.FLOAT);
        if ("left".equals(f) || "inline-start".equals(f)) {
            return LayoutBox.FLOAT_LEFT;
        }
        if ("right".equals(f) || "inline-end".equals(f)) {
            return LayoutBox.FLOAT_RIGHT;
        }
        return LayoutBox.FLOAT_NONE;
    }

    /**
     * Returns the position kind of a style.
     * <p>
     * 스타일의 위치 종류를 반환한다.
     *
     * @param s the style / 스타일
     * @return <code>LayoutBox.POS_*</code> / <code>LayoutBox.POS_*</code>
     */
    static int positionKindOf(ComputedStyle s) {
        String p = s.position();
        if ("relative".equals(p)) {
            return LayoutBox.POS_RELATIVE;
        }
        if ("absolute".equals(p)) {
            return LayoutBox.POS_ABSOLUTE;
        }
        if ("fixed".equals(p)) {
            return LayoutBox.POS_FIXED;
        }
        if ("sticky".equals(p)) {
            return LayoutBox.POS_STICKY;
        }
        return LayoutBox.POS_STATIC;
    }

    /**
     * Sets the properties that belong to positioning and stacking of a box built from an element: the creation number, the float side
     * (only for boxes that really float), the position kind, <code>z-index</code>, the stacking context flag and <code>clip: rect()</code>.
     * <p>
     * 요소에서 만든 상자의 위치 지정과 쌓임에 속하는 속성을 정한다: 생성 번호, float 방향(실제로 뜨는 상자만), 위치 종류, <code>z-index</code>,
     * 쌓임 맥락 표시, <code>clip: rect()</code>.
     *
     * @param b    the box / 상자
     * @param s    its style / 스타일
     * @param root true for the root element / 루트 요소이면 true
     */
    private void initPosition(LayoutBox b, ComputedStyle s, boolean root) {
        b.order = ++seq;
        b.posKind = positionKindOf(s);
        b.floatSide = b.posKind == LayoutBox.POS_ABSOLUTE || b.posKind == LayoutBox.POS_FIXED ? LayoutBox.FLOAT_NONE : floatSideOf(s);
        // 모양이 이미지인 float 는 이미지의 알파를 읽을 공급자를 가진다(CSS Shapes 1 4 절). 공급자가 없는 리졸버에서는 none 처럼 다룬다.
        if (b.floatSide != LayoutBox.FLOAT_NONE && resolver instanceof ShapeImageSource && FloatShapes.isImage(s.get(Prop.SHAPE_OUTSIDE))) {
            b.shapeSource = (ShapeImageSource) resolver;
        }
        double z = s.number(Prop.Z_INDEX);
        b.hasZ = b.posKind != LayoutBox.POS_STATIC && !Double.isNaN(z);
        b.zIndex = b.hasZ ? (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(z))) : 0;
        double op = s.number(Prop.OPACITY);
        // 필터, 혼합 모드, isolation, 마스크가 있는 상자도 opacity 처럼 쌓임 맥락이다(CSS Filter Effects 1, Compositing 1, Masking 1).
        // fixed 와 sticky 상자는 항상 쌓임 맥락을 만든다(CSS Positioned Layout 3: 스크롤에 따라 옮겨지는 상자는 안의 위치 지정 자손도 함께 옮겨야 한다).
        b.stackingContext = root || b.posKind == LayoutBox.POS_FIXED || b.posKind == LayoutBox.POS_STICKY || (b.hasZ) || (!Double.isNaN(op) && op < 1) || Css.hasTransform(s)
                || ClipPaths.has(s) || Effects.makesStackingContext(s) || Containment.layoutOrPaint(s);
        if (b.posKind == LayoutBox.POS_ABSOLUTE || b.posKind == LayoutBox.POS_FIXED) {
            b.clipRect = clipRectOf(s);
        }
    }

    /**
     * Reads <code>clip: rect(top, right, bottom, left)</code> of a style.
     * <p>
     * 스타일의 <code>clip: rect(위, 오른쪽, 아래, 왼쪽)</code> 를 읽는다.
     *
     * @param s the style / 스타일
     * @return {top, right, bottom, left} with NaN for <code>auto</code>, or null if the value is <code>auto</code> / auto 는 NaN 인 {위, 오른쪽, 아래, 왼쪽}. 값이 auto 이면 null
     */
    private static double[] clipRectOf(ComputedStyle s) {
        CssValue v = s.get(Prop.CLIP);
        if (!(v instanceof CssFunctionValue) || !((CssFunctionValue) v).isNamed("rect")) {
            return null;
        }
        List<CssValue> a = ((CssFunctionValue) v).getArguments();
        if (a.size() != 4) {
            return null;
        }
        double[] r = new double[4];
        // 네 값(위, 오른쪽, 아래, 왼쪽)을 길이로 읽고 auto 는 NaN 으로 둔다.
        for (int i = 0; i < 4; i++) {
            CssValue x = a.get(i);
            r[i] = x instanceof CssLength ? ((CssLength) x).getValue() : Double.NaN;
        }
        return r;
    }

    /**
     * The state while the children of one block container are collected.
     * <p>
     * 블록 컨테이너 하나의 자식을 모으는 동안의 상태.
     */
    private final class Ctx {
        /** The container being filled. 채우는 중인 컨테이너. */
        final LayoutBox box;
        /** Block-level children (including anonymous blocks). 블록 수준 자식(익명 블록 포함). */
        final List<LayoutBox> blocks = new ArrayList<LayoutBox>();
        /** The inline items collected since the last block-level child. 마지막 블록 수준 자식 이후 모은 인라인 항목. */
        List<InlineItem> run = new ArrayList<InlineItem>();
        /** The open inline elements (outermost first). 열려 있는 인라인 요소(바깥쪽이 먼저). */
        final List<LayoutBox> open = new ArrayList<LayoutBox>();
        /** Decoration bits of the container. 컨테이너의 장식 비트. */
        final int deco;
        /** Internal table parts that were found outside of a table and wait for the anonymous table around them. 표 밖에서 만나 둘러쌀 익명 표를 기다리는 표 내부 부품. */
        final List<Node> pendingTable = new ArrayList<Node>();
        /** Nesting depth of the pending table parts. 대기 중인 표 부품의 중첩 깊이. */
        int pendingDepth;
        /** The style of <code>::first-letter</code> while the first letter of the container has not been found, else null (P10-04). 컨테이너의 첫 글자를 찾기 전의 <code>::first-letter</code> 스타일. 찾은 뒤에는 null(P10-04). */
        ComputedStyle firstLetter;
        /** The element that <code>::first-letter</code> belongs to. <code>::first-letter</code> 가 속한 요소. */
        Element firstLetterOwner;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param box the container / 컨테이너
         */
        Ctx(LayoutBox box) {
            this.box = box;
            this.deco = box.decoration;
        }

        /**
         * Returns the style that applies to text at this point.
         * <p>
         * 이 지점의 텍스트에 적용되는 스타일을 반환한다.
         *
         * @return style / 스타일
         */
        ComputedStyle style() {
            return open.isEmpty() ? box.style : open.get(open.size() - 1).style;
        }

        /**
         * Returns the innermost open inline element or null.
         * <p>
         * 가장 안쪽의 열린 인라인 요소를 반환한다. 없으면 null.
         *
         * @return the element box or null / 요소 상자 또는 null
         */
        LayoutBox current() {
            return open.isEmpty() ? null : open.get(open.size() - 1);
        }

        /**
         * Returns the decoration bits that apply at this point.
         * <p>
         * 이 지점에 적용되는 장식 비트를 반환한다.
         *
         * @return bits / 비트
         */
        int decoration() {
            return open.isEmpty() ? deco : open.get(open.size() - 1).decoration;
        }

        /**
         * Adds a piece of text.
         * <p>
         * 텍스트 조각을 더한다.
         *
         * @param text raw text / 원문
         * @param node source node / 원본 노드
         */
        void addText(String text, Node node) {
            if (firstLetter != null && splitFirstLetter(text, node)) {
                return;
            }
            run.add(InlineItem.text(text, node, style(), current(), decoration()));
        }

        /**
         * Splits the first letter off the text if it is the first text of the container and a <code>::first-letter</code> style exists:
         * the text before it, the letter unit in its own inline element (or float) with the style of the pseudo-element, the rest.
         * <p>
         * 컨테이너의 첫 텍스트이고 <code>::first-letter</code> 스타일이 있으면 첫 글자를 떼어 낸다: 그 앞의 텍스트, 의사 요소의 스타일을 가진 자기만의 인라인 요소(또는 float)
         * 안의 글자 단위, 나머지.
         *
         * @param text raw text / 원문
         * @param node source node / 원본 노드
         * @return true if the text was added (split) / 텍스트를 (나누어) 더했으면 true
         */
        boolean splitFirstLetter(String text, Node node) {
            int[] r = GeneratedContent.firstLetter(text);
            if (r == null) {
                return false;
            }
            // initial-letter 가 있으면 첫 글자를 드롭 캡 상자로 만드는 스타일로 바꾼다(CSS Inline 3).
            ComputedStyle fl = InitialLetter.adjust(firstLetter, styles.getStyle(firstLetterOwner));
            firstLetter = null;
            if (r[0] > 0) {
                InlineItem lead = InlineItem.text(text.substring(0, r[0]), node, style(), current(), decoration());
                run.add(lead);
            }
            String unit = text.substring(r[0], r[1]);
            LayoutBox proto = new LayoutBox(LayoutBox.Type.INLINE, firstLetterOwner, fl);
            proto.pseudo = "first-letter";
            if (Css.isInline(fl.display())) {
                proto.inlineLevel = true;
                proto.inlineParent = current();
                proto.decoration = decoration() | Css.decoration(fl);
                InlineItem o = new InlineItem(InlineItem.Kind.OPEN);
                o.box = proto;
                o.edge = true;
                run.add(o);
                InlineItem t = InlineItem.text(unit, node, fl, proto, proto.decoration);
                t.srcOffset = r[0];
                run.add(t);
                InlineItem cl = new InlineItem(InlineItem.Kind.CLOSE);
                cl.box = proto;
                cl.edge = true;
                run.add(cl);
            } else {
                // float 인 첫 글자(드롭 캡)는 float 상자 안의 텍스트이다.
                LayoutBox fb = new LayoutBox(LayoutBox.Type.BLOCK, firstLetterOwner, fl);
                fb.pseudo = "first-letter";
                fb.bfcRoot = true;
                fb.decoration = decoration() | Css.decoration(fl);
                fb.items = new ArrayList<InlineItem>();
                InlineItem t = InlineItem.text(unit, node, fl, null, fb.decoration);
                t.srcOffset = r[0];
                fb.items.add(t);
                initPosition(fb, fl, false);
                if (fb.floatSide != LayoutBox.FLOAT_NONE || fb.isAbsolutelyPositioned()) {
                    fb.decoration = 0;
                    addOutOfFlow(this, fb);
                } else {
                    addBlock(fb);
                }
            }
            if (r[1] < text.length()) {
                InlineItem rest = InlineItem.text(text.substring(r[1]), node, style(), current(), decoration());
                rest.srcOffset = r[1];
                run.add(rest);
            }
            return true;
        }

        /**
         * Adds an atomic inline-level box.
         * <p>
         * 원자적 인라인 수준 상자를 더한다.
         *
         * @param b the box / 상자
         */
        void addAtomic(LayoutBox b) {
            firstLetter = null;
            InlineItem i = new InlineItem(InlineItem.Kind.ATOMIC);
            i.box = b;
            i.owner = current();
            run.add(i);
        }

        /**
         * Builds the anonymous table around the table parts that were collected outside of a table and adds it as a block-level child.
         * <p>
         * 표 밖에서 모은 표 부품을 감싸는 익명 표를 만들어 블록 수준 자식으로 더한다.
         */
        void flushTable() {
            if (pendingTable.isEmpty()) {
                return;
            }
            List<Node> nodes = new ArrayList<Node>(pendingTable);
            pendingTable.clear();
            addBlock(buildTable(null, box.style, nodes, pendingDepth, false));
        }

        /**
         * Adds a block-level child, splitting the open inline elements around it.
         * <p>
         * 블록 수준 자식을 더하며 열린 인라인 요소를 그 둘레에서 나눈다.
         *
         * @param b the block-level box / 블록 수준 상자
         */
        void addBlock(LayoutBox b) {
            firstLetter = null;
            List<LayoutBox> reopen = new ArrayList<LayoutBox>(open);
            // 열린 인라인 요소를 안쪽부터 닫는다(끝 가장자리는 나눈 쪽에 주지 않는다).
            for (int i = reopen.size() - 1; i >= 0; i--) {
                InlineItem c = new InlineItem(InlineItem.Kind.CLOSE);
                c.box = reopen.get(i);
                run.add(c);
            }
            flush();
            blocks.add(b);
            // 블록 뒤에서 같은 인라인 요소를 바깥쪽부터 다시 연다.
            for (int i = 0; i < reopen.size(); i++) {
                InlineItem o = new InlineItem(InlineItem.Kind.OPEN);
                o.box = reopen.get(i);
                run.add(o);
            }
        }

        /**
         * Wraps the collected inline items in an anonymous block if they have content.
         * <p>
         * 모은 인라인 항목에 내용이 있으면 익명 블록으로 감싼다.
         */
        void flush() {
            if (hasContent(run)) {
                LayoutBox anon = new LayoutBox(LayoutBox.Type.ANONYMOUS_BLOCK, null, box.style);
                anon.items = run;
                anon.decoration = box.decoration;
                blocks.add(anon);
            }
            run = new ArrayList<InlineItem>();
        }

        /**
         * Finishes the container: its inline items or its block children are set.
         * <p>
         * 컨테이너를 마친다: 인라인 항목 또는 블록 자식이 정해진다.
         */
        void finish() {
            flushTable();
            if (blocks.isEmpty()) {
                box.items = run;
                return;
            }
            flush();
            // 모은 블록 수준 자식을 컨테이너에 붙인다.
            for (int i = 0; i < blocks.size(); i++) {
                box.add(blocks.get(i));
            }
        }
    }

    /**
     * Tells whether inline items contain anything that produces a line: non-collapsible text, visible text, an atomic box or a break.
     * <p>
     * 인라인 항목에 줄을 만드는 것(접히지 않는 텍스트, 보이는 텍스트, 원자적 상자, 줄바꿈)이 있는지 알려 준다.
     *
     * @param run the items / 항목
     * @return true if content / 내용이 있으면 true
     */
    static boolean hasContent(List<InlineItem> run) {
        // 항목을 차례로 보며 줄을 만드는 항목이 하나라도 있는지 확인한다.
        for (int i = 0; i < run.size(); i++) {
            InlineItem it = run.get(i);
            if (it.kind == InlineItem.Kind.ATOMIC || it.kind == InlineItem.Kind.BREAK || it.kind == InlineItem.Kind.FLOAT
                    || it.kind == InlineItem.Kind.ABS) {
                return true;
            }
            if (it.kind == InlineItem.Kind.TEXT && it.text.length() > 0) {
                if (!Css.collapsesSpaces(it.style)) {
                    return true;
                }
                // 접히는 공백만 있는 텍스트는 내용으로 치지 않는다.
                for (int k = 0; k < it.text.length(); k++) {
                    char c = it.text.charAt(k);
                    if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Builds the box tree of a document.
     * <p>
     * 문서의 상자 트리를 만든다.
     *
     * @param doc the document / 문서
     * @return the root box (the box of the root element) / 루트 상자(루트 요소의 상자)
     */
    LayoutBox build(Document doc) {
        Element root = doc.getDocumentElement();
        ComputedStyle s = root == null ? ComputedStyle.initial() : styles.getStyle(root);
        if (root == null || "none".equals(s.display())) {
            LayoutBox r = new LayoutBox(LayoutBox.Type.ANONYMOUS_BLOCK, doc, s);
            r.items = new ArrayList<InlineItem>();
            r.bfcRoot = true;
            return r;
        }
        topLayer = renderedTopLayer(doc);
        LayoutBox b = block(root, s, 1, 0, true);
        b.bfcRoot = true;
        b.stackingContext = true;
        assignFrames(b, Css.FRAME_H, 0);
        return b;
    }

    /**
     * Assigns the writing mode frame to every box of the tree (top down). A box has the frame of its containing block unless its writing
     * mode is perpendicular to it: then it is an orthogonal flow root with its own frame and bfc root. Inline elements and replaced
     * elements never start a flow of their own; tables and their rows are always horizontal; an absolutely positioned box is compared
     * with the frame of its containing block.
     * <p>
     * 트리의 모든 상자에 쓰기 모드 프레임을 정해 준다(위에서 아래로). 상자는 쓰기 모드가 포함 블록과 수직이 아니면 포함 블록의 프레임을 갖는다. 수직이면 자신의 프레임을 가진
     * 직교 흐름 루트이며 BFC 루트이다. 인라인 요소와 치환 요소는 자신의 흐름을 시작하지 않고, 표와 행은 언제나 가로이며, 절대 배치 상자는 포함 블록의 프레임과 비교한다.
     *
     * @param b           the box / 상자
     * @param parentFrame the frame of its parent / 부모의 프레임
     * @param depth       nesting depth / 중첩 깊이
     */
    private void assignFrames(LayoutBox b, int parentFrame, int depth) {
        int own = Css.frameOf(b.style);
        LayoutBox.Type t = b.type;
        if (t == LayoutBox.Type.TABLE || t == LayoutBox.Type.TABLE_ROW_GROUP || t == LayoutBox.Type.TABLE_ROW
                || t == LayoutBox.Type.TABLE_COLUMN_GROUP || t == LayoutBox.Type.TABLE_COLUMN) {
            own = Css.FRAME_H;
        }
        int f;
        if (t == LayoutBox.Type.INLINE || t == LayoutBox.Type.REPLACED || (own == Css.FRAME_H) == (parentFrame == Css.FRAME_H)) {
            f = parentFrame;
        } else {
            f = own;
        }
        b.frame = f;
        b.parentFrame = parentFrame;
        b.orthRoot = f != parentFrame;
        if (b.isAbsolutelyPositioned()) {
            // 절대 배치 상자는 포함 블록의 프레임과 비교한다(포함 블록은 이미 프레임이 정해진 조상이다).
            int cbFrame = b.absCb == null ? Css.FRAME_H : b.absCb.frame;
            b.parentFrame = cbFrame;
            b.orthRoot = f != cbFrame;
        }
        if (b.orthRoot) {
            b.bfcRoot = true;
        }
        if (depth >= MAX_DEPTH + 8) {
            return;
        }
        // 블록 자식과 인라인 항목이 가진 상자의 프레임을 정한다.
        for (int i = 0; i < b.children.size(); i++) {
            assignFrames(b.children.get(i), f, depth + 1);
        }
        if (b.items != null) {
            for (int i = 0; i < b.items.size(); i++) {
                InlineItem it = b.items.get(i);
                // OPEN 과 CLOSE 는 같은 상자를 가리키므로 OPEN 과 원자적 상자, float, 절대 배치 상자만 처리한다.
                if (it.box != null && it.kind != InlineItem.Kind.CLOSE) {
                    assignFrames(it.box, f, depth + 1);
                }
            }
        }
    }

    /**
     * Builds a block-level box of an element.
     * <p>
     * 요소의 블록 수준 상자를 만든다.
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param depth nesting depth / 중첩 깊이
     * @param deco  decoration bits of the parent / 부모의 장식 비트
     * @param root  true for the root element / 루트 요소이면 true
     * @return the box / 상자
     */
    private LayoutBox block(Element e, ComputedStyle s, int depth, int deco, boolean root) {
        String disp = s.display();
        if (!root && ("table".equals(disp) || "inline-table".equals(disp))) {
            List<Node> kids = new ArrayList<Node>();
            if (depth < MAX_DEPTH) {
                flatten(e, kids, depth);
            }
            return buildTable(e, s, kids, depth, "inline-table".equals(disp));
        }
        LayoutBox b = new LayoutBox(LayoutBox.Type.BLOCK, e, s);
        initPosition(b, s, root);
        b.decoration = deco | Css.decoration(s);
        b.clipsContent = Css.clips(s);
        b.bfcRoot = root || isBfcRoot(s);
        fillPositioned(b, e, depth, "list-item".equals(s.display()));
        FirstLine.propagate(b);
        ControlSizes.markLegend(b, e);
        return b;
    }

    /**
     * Fills a box with the content of its element; a positioned box is the containing block of the absolutely positioned boxes inside
     * it while its content is built.
     * <p>
     * 상자를 요소의 내용으로 채운다. 위치 지정 상자는 내용을 만드는 동안 그 안의 절대 배치 상자의 포함 블록이 된다.
     *
     * @param b        the box / 상자
     * @param e        the element / 요소
     * @param depth    nesting depth / 중첩 깊이
     * @param listItem true for a list item / 목록 항목이면 true
     */
    private void fillPositioned(LayoutBox b, Element e, int depth, boolean listItem) {
        LayoutBox saved = cb;
        LayoutBox savedTf = tfCb;
        // 변환된 상자는 절대 배치와 fixed 자손의 포함 블록이다(CSS Transforms 1 2 절).
        // 필터가 있는 상자도 절대 배치와 fixed 자손의 포함 블록이다(CSS Filter Effects 1 5.1 절).
        boolean transformed = Css.hasTransform(b.style) || FilterSpec.has(b.style) || FilterSpec.hasBackdrop(b.style) || Containment.layoutOrPaint(b.style);
        if (b.posKind != LayoutBox.POS_STATIC || transformed) {
            cb = b;
        }
        if (transformed) {
            tfCb = b;
        }
        fill(b, e, depth, listItem);
        cb = saved;
        tfCb = savedTf;
    }

    /**
     * Registers a float or an absolutely positioned box as an out-of-flow item of the content that is being collected. An absolutely
     * positioned box is also recorded with its containing block, which lays it out when it is finished.
     * <p>
     * float 나 절대 배치 상자를 모으는 중인 콘텐츠의 흐름 밖 항목으로 등록한다. 절대 배치 상자는 포함 블록에도 기록되며 포함 블록이 끝날 때 배치한다.
     *
     * @param c  collection state / 모으는 상태
     * @param ob the box (already blockified) / 상자(이미 블록화됨)
     */
    private void addOutOfFlow(Ctx c, LayoutBox ob) {
        ob.inlineLevel = false;
        boolean abs = ob.posKind == LayoutBox.POS_ABSOLUTE || ob.posKind == LayoutBox.POS_FIXED;
        InlineItem it = new InlineItem(abs ? InlineItem.Kind.ABS : InlineItem.Kind.FLOAT);
        it.box = ob;
        it.owner = c.current();
        c.run.add(it);
        if (abs) {
            registerAbsolute(ob);
        }
    }

    /**
     * Tells whether a style makes its box the root of a block formatting context.
     * <p>
     * 스타일이 상자를 블록 서식 문맥의 루트로 만드는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true for a BFC root / BFC 루트이면 true
     */
    private static boolean isBfcRoot(ComputedStyle s) {
        String d = s.display();
        String pos = s.position();
        return Css.clips(s) || !"none".equals(Css.kw(s, Prop.FLOAT)) && Css.kw(s, Prop.FLOAT).length() > 0 || "absolute".equals(pos)
                || "fixed".equals(pos) || "flow-root".equals(d) || d.startsWith("table") || d.indexOf("flex") >= 0 || d.indexOf("grid") >= 0
                || "inline-block".equals(d) || Containment.layoutOrPaint(s);
    }

    /**
     * Fills a block container with the content of its element.
     * <p>
     * 블록 컨테이너를 요소의 내용으로 채운다.
     *
     * @param b        the container / 컨테이너
     * @param e        the element / 요소
     * @param depth    nesting depth / 중첩 깊이
     * @param listItem true for a list item / 목록 항목이면 true
     */
    private void fill(LayoutBox b, Element e, int depth, boolean listItem) {
        if (isFlexDisplay(b.style.display()) || isGridDisplay(b.style.display())) {
            // 플렉스/그리드 컨테이너의 자식은 플렉스/그리드 항목이 된다(P10-03).
            fillFlex(b, e, depth);
            return;
        }
        Ctx c = new Ctx(b);
        // ::first-letter 는 블록 컨테이너의 첫 줄의 첫 글자를 꾸민다(P10-04).
        c.firstLetter = styles.getPseudoStyle(e, "first-letter");
        c.firstLetterOwner = e;
        // ::first-line 은 블록 컨테이너의 첫 줄을 꾸민다(P10-04-f). 내용이 블록 자식으로 시작하면 block() 이 첫 자식에게 넘긴다.
        b.firstLineStyle = styles.getPseudoStyle(e, "first-line");
        b.firstLineBase = b.firstLineStyle == null ? null : b.style;
        if (listItem) {
            b.listOrdinal = ordinal(e);
            String marker = counterStyles.marker(b.style.get(Prop.LIST_STYLE_TYPE), b.listOrdinal);
            // ::marker 는 표식의 색, 글꼴과 content 를 정한다(P10-04).
            ComputedStyle ms = styles.getPseudoStyle(e, "marker");
            if (ms != null) {
                b.markerStyle = ms;
                String mt = gc.text(ms, e);
                if (mt != null) {
                    marker = mt.length() == 0 ? null : mt;
                }
            }
            boolean inside = "inside".equals(Css.kw(b.style, Prop.LIST_STYLE_POSITION));
            // list-style-image 가 쓸 수 있는 이미지이면 바깥쪽 표식은 list-style-type 과 상관없이 그 이미지이다(CSS Lists 3 3.3 절). ::marker 의 content 가 정해지면 그것이 이긴다.
            CssValue image = b.style.get(Prop.LIST_STYLE_IMAGE);
            double[] imageSize = null;
            if (!inside && image != null && !image.isKeyword("none") && resolver instanceof MarkerImageSource && (ms == null || gc.text(ms, e) == null)) {
                imageSize = ((MarkerImageSource) resolver).markerImageSize(image, e);
            }
            if (imageSize != null) {
                b.markerText = marker != null ? marker : "";
                b.markerImage = image;
                b.markerImageSize = imageSize;
            } else if (marker != null) {
                if (inside) {
                    InlineItem m = InlineItem.text(marker, e, ms != null ? ms : b.style, null, b.decoration);
                    m.marker = true;
                    c.run.add(m);
                } else {
                    b.markerText = marker;
                }
            }
        }
        // content-visibility: hidden 이면 내용(과 ::before/::after)의 상자를 만들지 않는다(CSS Containment 2).
        boolean skip = Containment.skipsContents(b.style);
        if (!skip) {
            pseudo(c, e, "before", depth);
        }
        if (depth < MAX_DEPTH && !skip) {
            // 요소의 자식을 차례로 처리한다.
            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                add(c, n, depth);
            }
        }
        if (!skip) {
            pseudo(c, e, "after", depth);
        }
        // 최상위 계층의 요소와 그 ::backdrop 은 루트 요소의 형제처럼 문서의 끝에 그린다(CSS Positioned Layout 4 3.1 절).
        if (topLayer != null && e.getParentNode() == e.getOwnerDocument()) {
            addTopLayer(c, depth);
        }
        c.finish();
        if (isMulticolStyle(b.style) && b.type != LayoutBox.Type.REPLACED) {
            // 다단 컨테이너는 독립된 블록 서식 문맥이며 column-span:all 인 자식은 열 구간을 나눈다(P10-03).
            b.multicol = true;
            b.bfcRoot = true;
            if (b.items == null) {
                groupColumnSections(b);
            }
        }
    }

    /**
     * Tells whether a style makes a multi-column container (<code>column-count</code> or <code>column-width</code> is not
     * <code>auto</code>).
     * <p>
     * 스타일이 다단 컨테이너를 만드는지(<code>column-count</code> 나 <code>column-width</code> 가 <code>auto</code> 가 아닌지) 알려 준다.
     *
     * @param s the style / 스타일
     * @return true for multi-column containers / 다단 컨테이너이면 true
     */
    static boolean isMulticolStyle(ComputedStyle s) {
        return s != null && (!s.isAuto(Prop.COLUMN_COUNT) || !s.isAuto(Prop.COLUMN_WIDTH));
    }

    /**
     * Tells whether a box of a multi-column container spans all the columns (<code>column-span:all</code> on an in-flow block-level
     * child).
     * <p>
     * 다단 컨테이너의 자식 상자가 모든 열에 걸치는지(정상 흐름 블록 수준 자식의 <code>column-span:all</code>) 알려 준다.
     *
     * @param k the child box / 자식 상자
     * @return true for spanning boxes / 걸치는 상자이면 true
     */
    private static boolean isSpanner(LayoutBox k) {
        return k.style != null && k.type != LayoutBox.Type.ANONYMOUS_BLOCK && k.floatSide == LayoutBox.FLOAT_NONE
                && k.posKind != LayoutBox.POS_ABSOLUTE && k.posKind != LayoutBox.POS_FIXED && "all".equals(Css.kw(k.style, Prop.COLUMN_SPAN));
    }

    /**
     * Splits the block children of a multi-column container that has a spanning child into column sections: the runs of children between
     * the spanning boxes are wrapped into anonymous multi-column boxes (each is balanced and cut into columns on its own) and the
     * container itself becomes an ordinary block.
     * <p>
     * 모든 열에 걸치는 자식이 있는 다단 컨테이너의 블록 자식을 열 구간으로 나눈다: 걸치는 상자 사이의 자식들은 익명 다단 상자로 감싸고(각각 따로 균형을 맞춰 열로
     * 자른다) 컨테이너 자신은 보통의 블록이 된다.
     *
     * @param b the container / 컨테이너
     */
    private void groupColumnSections(LayoutBox b) {
        boolean any = false;
        // 걸치는 자식이 있는지 확인한다.
        for (int i = 0; i < b.children.size(); i++) {
            if (isSpanner(b.children.get(i))) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        List<LayoutBox> old = new ArrayList<LayoutBox>(b.children);
        b.children.clear();
        LayoutBox section = null;
        // 자식을 차례로 보며 걸치는 상자는 그대로 두고 나머지는 구간에 모은다.
        for (int i = 0; i < old.size(); i++) {
            LayoutBox k = old.get(i);
            if (isSpanner(k)) {
                section = null;
                b.add(k);
                continue;
            }
            if (section == null) {
                section = new LayoutBox(LayoutBox.Type.ANONYMOUS_BLOCK, null, b.style);
                section.order = ++seq;
                section.multicol = true;
                section.bfcRoot = true;
                b.add(section);
            }
            section.add(k);
        }
        b.multicol = false;
    }

    /**
     * Returns the number of a list item: the <code>value</code> attribute, or the next number of its parent's counter
     * (<code>ol start</code> and <code>reversed</code> are honored).
     * <p>
     * 목록 항목의 번호를 반환한다: <code>value</code> 속성, 아니면 부모 카운터의 다음 번호(<code>ol start</code>, <code>reversed</code> 반영).
     *
     * @param li the list item element / 목록 항목 요소
     * @return the number / 번호
     */
    private int ordinal(Element li) {
        Node p = li.getParentNode();
        int[] c = counters.get(p);
        if (c == null) {
            c = new int[] { 1, 1 };
            if (p instanceof Element && "ol".equals(((Element) p).getLocalName())) {
                Element ol = (Element) p;
                boolean reversed = ol.hasAttribute("reversed");
                int count = 0;
                // reversed 목록은 항목 수에서 시작하므로 li 자식을 센다.
                for (Node n = ol.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (n instanceof Element && "li".equals(((Element) n).getLocalName())) {
                        count++;
                    }
                }
                c[1] = reversed ? -1 : 1;
                c[0] = parseInt(ol.getAttribute("start"), reversed ? count : 1);
            }
            counters.put(p, c);
        }
        int v = parseInt(li.getAttribute("value"), c[0]);
        c[0] = v + c[1];
        return v;
    }

    /**
     * Parses an integer attribute value with the "rules for parsing integers" (HTML5 2014 section 2.4.4.1): leading ASCII white space,
     * an optional <code>+</code> or <code>-</code> sign, then digits; characters after the digits are ignored. Text without digits or
     * a number that does not fit an <code>int</code> gives the default.
     * <p>
     * 정수 속성 값을 "정수 해석 규칙"(HTML5 2014 §2.4.4.1)으로 해석한다: 앞쪽 ASCII 공백, 선택적 <code>+</code>/<code>-</code> 부호, 숫자 순이며 숫자 뒤
     * 문자는 무시한다. 숫자가 없거나 <code>int</code> 에 들어가지 않으면 기본값이다.
     *
     * @param s   the text or null / 텍스트 또는 null
     * @param def the default / 기본값
     * @return the number or the default / 숫자 또는 기본값
     */
    static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        int i = 0;
        // 앞쪽 ASCII 공백(공백, 탭, 줄 바꿈, 쓰기 쪽 넘김, 캐리지 리턴)을 건너뛴다.
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\n' || s.charAt(i) == '\f' || s.charAt(i) == '\r')) {
            i++;
        }
        boolean negative = false;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            negative = s.charAt(i) == '-';
            i++;
        }
        int start = i;
        long v = 0;
        // 연속한 숫자를 읽고 int 범위를 넘으면 기본값으로 돌아간다.
        while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            v = v * 10 + (s.charAt(i) - '0');
            if (v > Integer.MAX_VALUE) {
                return def;
            }
            i++;
        }
        if (i == start) {
            return def;
        }
        return (int) (negative ? -v : v);
    }

    /**
     * Tells whether a node is content of a closed <code>details</code> element that is not shown: every child (text included) except the
     * first <code>summary</code> element child. An open <code>details</code> shows all of its children.
     * <p>
     * 노드가 보이지 않는 닫힌 <code>details</code> 요소의 내용인지 알려 준다: 첫 <code>summary</code> 요소 자식을 뺀 모든 자식(텍스트 포함). 열린
     * <code>details</code> 는 모든 자식을 보인다.
     *
     * @param n the node / 노드
     * @return true if the node is hidden / 보이지 않으면 true
     */
    static boolean isHiddenDetailsChild(Node n) {
        Node p = n.getParentNode();
        if (!(p instanceof Element)) {
            return false;
        }
        Element d = (Element) p;
        if (!isHtml(d) || !"details".equals(d.getLocalName()) || d.hasAttribute("open")) {
            return false;
        }
        // details 의 자식 중 첫 summary 요소만 보인다.
        for (Node ch = d.getFirstChild(); ch != null; ch = ch.getNextSibling()) {
            if (ch instanceof Element && isHtml((Element) ch) && "summary".equals(((Element) ch).getLocalName())) {
                return ch != n;
            }
        }
        return true;
    }

    /**
     * Adds a DOM node to the content of a container.
     * <p>
     * DOM 노드를 컨테이너의 내용에 더한다.
     *
     * @param c     collection state / 모으는 상태
     * @param n     the node / 노드
     * @param depth depth of the parent / 부모의 깊이
     */
    private void add(Ctx c, Node n, int depth) {
        // 닫힌 details 는 첫 summary 이외의 자식(텍스트 포함)을 보이지 않는다(HTML 표준 15.5.4 details 와 summary 렌더링).
        if (isHiddenDetailsChild(n)) {
            return;
        }
        if (n instanceof Text) {
            String data = ((Text) n).getData();
            if (!c.pendingTable.isEmpty()) {
                // 표 부품 사이의 공백은 버리고 다른 텍스트가 오면 모아 둔 표를 먼저 내보낸다.
                if (isBlankText(n)) {
                    return;
                }
                c.flushTable();
            }
            if (data.length() > 0) {
                c.addText(data, n);
            }
            return;
        }
        if (!(n instanceof Element)) {
            return;
        }
        Element e = (Element) n;
        // 최상위 계층의 요소는 트리의 제자리가 아니라 루트의 끝에서 만든다.
        if (topLayer != null && !addingTopLayer && topLayer.contains(e)) {
            return;
        }
        ComputedStyle s = styles.getStyle(e);
        String d = s.display();
        if ("none".equals(d)) {
            return;
        }
        if (isTableInternal(d)) {
            // 표 밖에서 만난 표 내부 부품은 모아 두었다가 익명 표로 감싼다.
            c.pendingTable.add(e);
            c.pendingDepth = depth;
            return;
        }
        if (!"contents".equals(d)) {
            c.flushTable();
        }
        if ("contents".equals(d)) {
            if (depth < MAX_DEPTH) {
                // display:contents 요소는 상자 없이 자식만 같은 문맥에 더한다.
                for (Node ch = e.getFirstChild(); ch != null; ch = ch.getNextSibling()) {
                    add(c, ch, depth + 1);
                }
            }
            return;
        }
        String tag = isHtml(e) ? e.getLocalName() : ("svg".equals(e.getLocalName()) ? "svg" : "");
        if ("br".equals(tag)) {
            InlineItem br = new InlineItem(InlineItem.Kind.BREAK);
            br.node = e;
            // br 의 계산 스타일(clear 속성)을 줄바꿈 뒤의 float 비움에 쓰도록 남긴다.
            br.style = s;
            c.run.add(br);
            return;
        }
        if ("wbr".equals(tag)) {
            // wbr의 계산 스타일(nobr wbr 포함)을 유지하며 글리프나 빈 줄을 만들지 않는다.
            InlineItem opportunity = new InlineItem(InlineItem.Kind.OPPORTUNITY);
            opportunity.node = e;
            opportunity.style = s;
            c.run.add(opportunity);
            return;
        }
        if ("input".equals(tag) && "hidden".equalsIgnoreCase(e.getAttribute("type"))) {
            return;
        }
        int pk = positionKindOf(s);
        boolean outOfFlow = pk == LayoutBox.POS_ABSOLUTE || pk == LayoutBox.POS_FIXED || floatSideOf(s) != LayoutBox.FLOAT_NONE;
        // 깨졌고 자기 크기가 없는 인라인 이미지는 크기 0 치환 상자 대신 대체 텍스트를 내용으로 하는 비치환 인라인 요소이다(HTML5 2014 §10.4.2).
        String fallbackText = !outOfFlow && Css.isInline(d) ? brokenImageText(e, tag, s) : null;
        if (fallbackText != null) {
            LayoutBox proto = new LayoutBox(LayoutBox.Type.INLINE, e, s);
            proto.inlineLevel = true;
            proto.inlineParent = c.current();
            proto.decoration = c.decoration() | Css.decoration(s);
            InlineItem o = new InlineItem(InlineItem.Kind.OPEN);
            o.box = proto;
            o.edge = true;
            c.run.add(o);
            c.open.add(proto);
            c.addText(fallbackText, e);
            c.open.remove(c.open.size() - 1);
            InlineItem cl = new InlineItem(InlineItem.Kind.CLOSE);
            cl.box = proto;
            cl.edge = true;
            c.run.add(cl);
            return;
        }
        if (Replaced.isReplaced(e, tag)) {
            LayoutBox r = new LayoutBox(LayoutBox.Type.REPLACED, e, s);
            initPosition(r, s, false);
            r.replaced = replacedContent(e, s);
            ControlSizes.prepare(r, e, s);
            // 텍스트 필드와 텍스트 영역은 ::placeholder 의 스타일(글자색)로 placeholder 를 그린다.
            if ("input".equals(tag) || "textarea".equals(tag)) {
                r.placeholderStyle = styles.getPseudoStyle(e, "placeholder");
            }
            // 파일 컨트롤의 단추는 ::file-selector-button 의 스타일로 그린다(CSS Pseudo-Elements 4 5.1 절). 작성자가 정하지 않은 속성은 그리는 쪽의 모양을 쓴다.
            if ("input".equals(tag) && "file".equalsIgnoreCase(e.getAttribute("type") == null ? "" : e.getAttribute("type").trim())) {
                r.fileButtonStyle = styles.getPseudoStyle(e, "file-selector-button");
                r.fileButtonDeclared = r.fileButtonStyle == null ? null : styles.getPseudoDeclared(e, "file-selector-button");
            }
            r.clipsContent = Css.clips(s);
            r.bfcRoot = true;
            r.inlineLevel = !outOfFlow && (Css.isInline(d) || Css.isAtomicInline(d));
            if (outOfFlow) {
                // float 나 절대 배치 치환 요소는 흐름 밖 항목으로 등록한다.
                addOutOfFlow(c, r);
            } else if (r.inlineLevel) {
                c.addAtomic(r);
            } else {
                c.addBlock(r);
            }
            return;
        }
        if (outOfFlow) {
            // float 와 절대 배치 요소는 블록화한 상자를 만들어 흐름 밖 항목으로 등록한다(텍스트 장식은 전달되지 않는다).
            addOutOfFlow(c, block(e, s, depth + 1, 0, false));
            return;
        }
        if ("inline-table".equals(d)) {
            LayoutBox it = block(e, s, depth + 1, 0, false);
            it.inlineLevel = true;
            c.addAtomic(it);
            return;
        }
        if (Css.isAtomicInline(d)) {
            LayoutBox ib = new LayoutBox(LayoutBox.Type.INLINE_BLOCK, e, s);
            initPosition(ib, s, false);
            ib.inlineLevel = true;
            ib.bfcRoot = true;
            ib.decoration = Css.decoration(s);
            ib.clipsContent = Css.clips(s);
            fillPositioned(ib, e, depth + 1, false);
            c.addAtomic(ib);
            return;
        }
        if (Css.isInline(d)) {
            LayoutBox proto = new LayoutBox(LayoutBox.Type.INLINE, e, s);
            proto.inlineLevel = true;
            proto.inlineParent = c.current();
            proto.decoration = c.decoration() | Css.decoration(s);
            InlineItem o = new InlineItem(InlineItem.Kind.OPEN);
            o.box = proto;
            o.edge = true;
            c.run.add(o);
            c.open.add(proto);
            pseudo(c, e, "before", depth);
            if (depth < MAX_DEPTH) {
                // 인라인 요소의 자식을 같은 문맥에 더한다.
                for (Node ch = e.getFirstChild(); ch != null; ch = ch.getNextSibling()) {
                    add(c, ch, depth + 1);
                }
            }
            pseudo(c, e, "after", depth);
            c.open.remove(c.open.size() - 1);
            InlineItem cl = new InlineItem(InlineItem.Kind.CLOSE);
            cl.box = proto;
            cl.edge = true;
            c.run.add(cl);
            return;
        }
        // 그 밖의 display 값(block, list-item, table, flex ...)은 블록 수준 상자로 다룬다.
        c.addBlock(block(e, s, depth + 1, c.decoration(), false));
    }

    /**
     * Returns the text that replaces a broken <code>img</code> that has no dimensions of its own (HTML5 REC 2014-10-28 section 10.4.2,
     * https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#images, retrieved 2026-11-17): such an image is not a replaced element
     * but a non-replaced inline element whose content is the text that it stands for. An image whose <code>width</code> and
     * <code>height</code> are both specified (by the attributes or by CSS) keeps its replaced box, which the painter fills with the same
     * text; the specification asks for that in quirks mode or if the image has no <code>alt</code> attribute, and the project keeps it
     * in the standards mode too, because the unsupported URL has to be shown at the place of the image (plan item P18-06).
     * <p>
     * 자기 크기가 없는 깨진 <code>img</code> 를 대신하는 텍스트를 반환한다(HTML5 REC 2014-10-28 §10.4.2): 그런 이미지는 치환 요소가 아니라 그것이 대신하는 텍스트를 내용으로 하는
     * 비치환 인라인 요소이다. <code>width</code> 와 <code>height</code> 가 둘 다 (속성이나 CSS 로) 정해진 이미지는 치환 상자를 유지하며 그리는 쪽이 같은 텍스트로 채운다.
     * 규격은 호환 모드이거나 <code>alt</code> 속성이 없을 때 그렇게 하라고 하는데, 프로젝트는 이미지 자리에 미지원 URL 을 보여야 하므로(계획 P18-06) 표준 모드에서도 유지한다.
     *
     * @param e   the element / 요소
     * @param tag its lower-case tag name / 소문자 태그 이름
     * @param s   its computed style / 계산 스타일
     * @return the text, or null if the element is not a broken image of this kind / 텍스트. 이 종류의 깨진 이미지가 아니면 null
     */
    private String brokenImageText(Element e, String tag, ComputedStyle s) {
        if (!"img".equals(tag) || !(resolver instanceof ReplacedFallback)) {
            return null;
        }
        // 두 크기가 모두 정해져 있으면 크기가 있는 치환 상자를 유지한다.
        if (!s.isAuto(Prop.WIDTH) && !s.isAuto(Prop.HEIGHT)) {
            return null;
        }
        return ((ReplacedFallback) resolver).getFallbackText(e);
    }

    // ---------------------------------------------------------------------------------------------- flexbox (P10-03)

    /**
     * Tells whether a <code>display</code> value makes a flex container (<code>flex</code> and <code>inline-flex</code>, with the
     * <code>-webkit-</code> aliases).
     * <p>
     * <code>display</code> 값이 플렉스 컨테이너(<code>flex</code>, <code>inline-flex</code>와 <code>-webkit-</code> 별칭)를 만드는지 알려 준다.
     *
     * @param d the display value / display 값
     * @return true for flex containers / 플렉스 컨테이너이면 true
     */
    static boolean isFlexDisplay(String d) {
        return "flex".equals(d) || "inline-flex".equals(d) || "-webkit-flex".equals(d) || "-webkit-inline-flex".equals(d);
    }

    /**
     * Tells whether a <code>display</code> value makes a grid container (<code>grid</code> and <code>inline-grid</code>).
     * <p>
     * <code>display</code> 값이 그리드 컨테이너(<code>grid</code>, <code>inline-grid</code>)를 만드는지 알려 준다.
     *
     * @param d the display value / display 값
     * @return true for grid containers / 그리드 컨테이너이면 true
     */
    static boolean isGridDisplay(String d) {
        return "grid".equals(d) || "inline-grid".equals(d);
    }

    /**
     * Marks a box as a flex item or a grid item of its container.
     * <p>
     * 상자를 컨테이너의 플렉스 항목이나 그리드 항목으로 표시한다.
     *
     * @param container the flex or grid container / 플렉스 또는 그리드 컨테이너
     * @param item      the item box / 항목 상자
     */
    private static void markItem(LayoutBox container, LayoutBox item) {
        if (container.gridContainer) {
            item.gridItem = true;
        } else {
            item.flexItem = true;
        }
    }

    /**
     * Registers an absolutely positioned box with its containing block (the nearest positioned ancestor box, or the initial containing
     * block), which lays it out when it is finished.
     * <p>
     * 절대 배치 상자를 포함 블록(가장 가까운 위치 지정 조상 상자, 없으면 초기 포함 블록)에 등록한다. 포함 블록이 끝날 때 배치한다.
     *
     * @param ob the absolutely positioned or fixed box / 절대 배치 또는 fixed 상자
     */
    private void registerAbsolute(LayoutBox ob) {
        // fixed 상자의 포함 블록은 뷰포트이지만 변환된 조상이 있으면 그 조상이다.
        ob.absCb = ob.posKind == LayoutBox.POS_FIXED ? tfCb : cb;
        // 포함 블록이 없으면 초기 포함 블록 목록에, 있으면 그 상자의 목록에 등록한다.
        if (ob.absCb == null) {
            icbList.add(ob);
        } else {
            if (ob.absCb.absList == null) {
                ob.absCb.absList = new ArrayList<LayoutBox>();
            }
            ob.absCb.absList.add(ob);
        }
    }

    /**
     * Fills a flex container: each in-flow child element, each pseudo-element and each run of non-blank text becomes a flex item (a
     * blockified box that starts its own block formatting context), children that are absolutely positioned or fixed are out-of-flow
     * boxes, and <code>display:none</code> children are skipped. The items are sorted by <code>order</code> (CSS Flexbox 4.2): the
     * children list of the container is the order-modified document order, which is also the painting order.
     * <p>
     * 플렉스 컨테이너를 채운다: 정상 흐름의 자식 요소, 의사 요소, 공백이 아닌 텍스트 덩어리마다 플렉스 항목(자신의 블록 서식 문맥을 시작하는 블록화된 상자)이
     * 되고, 절대 배치나 fixed 자식은 흐름 밖 상자이며, <code>display:none</code> 자식은 건너뛴다. 항목은 <code>order</code> 로 정렬한다(CSS Flexbox 4.2):
     * 컨테이너의 자식 목록이 order 를 반영한 문서 순서이며 그리기 순서이기도 하다.
     *
     * @param b     the container / 컨테이너
     * @param e     its element / 요소
     * @param depth nesting depth / 중첩 깊이
     */
    private void fillFlex(LayoutBox b, Element e, int depth) {
        if (isGridDisplay(b.style.display())) {
            b.gridContainer = true;
        } else {
            b.flexContainer = true;
        }
        List<LayoutBox> items = new ArrayList<LayoutBox>();
        List<LayoutBox> outOfFlow = new ArrayList<LayoutBox>();
        boolean skip = Containment.skipsContents(b.style);
        if (!skip) {
            flexPseudo(b, e, "before", items, outOfFlow);
        }
        if (depth < MAX_DEPTH && !skip) {
            List<Node> run = new ArrayList<Node>();
            flexChildren(b, e, depth, items, outOfFlow, run);
            flushFlexText(b, run, items);
        }
        if (!skip) {
            flexPseudo(b, e, "after", items, outOfFlow);
        }
        orderFlexItems(items);
        // 정렬한 항목을 컨테이너의 자식으로 붙이고 흐름 밖 상자를 뒤에 붙인다.
        for (int i = 0; i < items.size(); i++) {
            b.add(items.get(i));
        }
        // 흐름 밖 상자를 항목 뒤에 붙인다.
        for (int i = 0; i < outOfFlow.size(); i++) {
            b.add(outOfFlow.get(i));
        }
    }

    /**
     * Collects the flex items among the children of a node (the container, or a <code>display:contents</code> child whose children take
     * its place).
     * <p>
     * 노드(컨테이너, 또는 자식이 그 자리를 대신하는 <code>display:contents</code> 자식)의 자식 중에서 플렉스 항목을 모은다.
     *
     * @param b       the container / 컨테이너
     * @param parent  the node whose children are visited / 자식을 훑을 노드
     * @param depth   nesting depth / 중첩 깊이
     * @param items   receives the items / 항목을 받는다
     * @param oof     receives the out-of-flow boxes / 흐름 밖 상자를 받는다
     * @param run     the text nodes that wait for their anonymous item / 익명 항목을 기다리는 텍스트 노드
     */
    private void flexChildren(LayoutBox b, Node parent, int depth, List<LayoutBox> items, List<LayoutBox> oof, List<Node> run) {
        // 자식을 차례로 보며 텍스트는 모으고 요소는 항목으로 만든다.
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Text) {
                if (((Text) n).getData().length() > 0) {
                    run.add(n);
                }
                continue;
            }
            if (!(n instanceof Element)) {
                continue;
            }
            Element ce = (Element) n;
            ComputedStyle s = styles.getStyle(ce);
            String d = s.display();
            if ("none".equals(d)) {
                continue;
            }
            if ("contents".equals(d)) {
                if (depth < MAX_DEPTH) {
                    flexChildren(b, ce, depth + 1, items, oof, run);
                }
                continue;
            }
            String tag = isHtml(ce) ? ce.getLocalName() : ("svg".equals(ce.getLocalName()) ? "svg" : "");
            if ("br".equals(tag) || "wbr".equals(tag) || ("input".equals(tag) && "hidden".equalsIgnoreCase(ce.getAttribute("type")))) {
                continue;
            }
            flushFlexText(b, run, items);
            LayoutBox box = flexItemBox(b, ce, s, tag, depth);
            if (box.isAbsolutelyPositioned()) {
                registerAbsolute(box);
                oof.add(box);
            } else {
                items.add(box);
            }
        }
    }

    /**
     * Builds the box of a child element of a flex container, blockified. The box is marked as a flex item unless it is absolutely
     * positioned (a float is no float inside a flex container; <code>z-index</code> applies to flex items even if they are not positioned).
     * <p>
     * 플렉스 컨테이너의 자식 요소의 상자를 블록화해서 만든다. 절대 배치가 아니면 플렉스 항목으로 표시한다(플렉스 컨테이너 안에서 float 는 float 가 아니며
     * <code>z-index</code> 는 위치 지정이 없는 플렉스 항목에도 적용된다).
     *
     * @param b     the container / 컨테이너
     * @param ce    the child element / 자식 요소
     * @param s     its style / 스타일
     * @param tag   the HTML tag name or "" / HTML 태그 이름 또는 ""
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox flexItemBox(LayoutBox b, Element ce, ComputedStyle s, String tag, int depth) {
        LayoutBox box;
        if (Replaced.isReplaced(ce, tag)) {
            box = new LayoutBox(LayoutBox.Type.REPLACED, ce, s);
            initPosition(box, s, false);
            box.replaced = replacedContent(ce, s);
            ControlSizes.prepare(box, ce, s);
            box.clipsContent = Css.clips(s);
            box.bfcRoot = true;
        } else {
            box = block(ce, s, depth + 1, b.decoration, false);
        }
        box.inlineLevel = false;
        if (box.isAbsolutelyPositioned()) {
            box.floatSide = LayoutBox.FLOAT_NONE;
            return box;
        }
        box.floatSide = LayoutBox.FLOAT_NONE;
        markItem(b, box);
        box.bfcRoot = true;
        double z = s.number(Prop.Z_INDEX);
        // 위치 지정이 없는 플렉스 항목에도 z-index 는 쌓임 맥락을 만든다.
        if (!Double.isNaN(z)) {
            box.hasZ = true;
            box.zIndex = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(z)));
            box.stackingContext = true;
        }
        return box;
    }

    /**
     * Turns the collected run of text nodes into an anonymous flex item unless the text is all white space (then it is not rendered).
     * <p>
     * 모은 텍스트 노드 덩어리를 익명 플렉스 항목으로 만든다. 텍스트가 모두 공백이면 만들지 않는다(그려지지 않음).
     *
     * @param b     the container / 컨테이너
     * @param run   the text nodes (cleared) / 텍스트 노드(비워진다)
     * @param items receives the item / 항목을 받는다
     */
    private void flushFlexText(LayoutBox b, List<Node> run, List<LayoutBox> items) {
        boolean blank = true;
        // 공백이 아닌 글자가 있는 노드가 하나라도 있는지 확인한다.
        for (int i = 0; i < run.size(); i++) {
            if (!isBlankText(run.get(i))) {
                blank = false;
                break;
            }
        }
        if (!blank) {
            LayoutBox anon = new LayoutBox(LayoutBox.Type.ANONYMOUS_BLOCK, null, b.style);
            anon.order = ++seq;
            anon.decoration = b.decoration;
            anon.bfcRoot = true;
            markItem(b, anon);
            Ctx c = new Ctx(anon);
            // 텍스트 노드를 차례로 익명 항목의 인라인 콘텐츠에 더한다.
            for (int i = 0; i < run.size(); i++) {
                c.addText(((Text) run.get(i)).getData(), run.get(i));
            }
            c.finish();
            items.add(anon);
        }
        run.clear();
    }

    /**
     * Generates the <code>::before</code> or <code>::after</code> box of a flex container as a flex item (or as an out-of-flow box).
     * <p>
     * 플렉스 컨테이너의 <code>::before</code> 또는 <code>::after</code> 상자를 플렉스 항목(또는 흐름 밖 상자)으로 만든다.
     *
     * @param b     the container / 컨테이너
     * @param e     the originating element / 기원 요소
     * @param name  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @param items receives the item / 항목을 받는다
     * @param oof   receives out-of-flow boxes / 흐름 밖 상자를 받는다
     */
    private void flexPseudo(LayoutBox b, Element e, String name, List<LayoutBox> items, List<LayoutBox> oof) {
        // 요소의 카운터는 ::before 직전에 적용하고 ::after 뒤에 범위를 닫는다(P10-04).
        if ("before".equals(name)) {
            gc.enter(e, styles.getStyle(e));
        }
        flexPseudoContent(b, e, name, items, oof);
        if ("after".equals(name)) {
            gc.exit(e);
        }
    }

    /**
     * Adds the item of a generated content of a flex or grid container.
     * <p>
     * 플렉스나 그리드 컨테이너의 생성 콘텐츠 항목을 더한다.
     *
     * @param b     the container / 컨테이너
     * @param e     the originating element / 기원 요소
     * @param name  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @param items receives the item / 항목을 받는다
     * @param oof   receives out-of-flow boxes / 흐름 밖 상자를 받는다
     */
    private void flexPseudoContent(LayoutBox b, Element e, String name, List<LayoutBox> items, List<LayoutBox> oof) {
        ComputedStyle ps = styles.getPseudoStyle(e, name);
        if (ps == null) {
            return;
        }
        String text = gc.text(ps, e);
        if (text == null || "none".equals(ps.display())) {
            return;
        }
        // 의사 요소의 카운터 속성은 자신의 content 를 평가하기 전에 적용한다(CSS Lists 3 4.4). 그래서 증가한 값으로 content 를 다시 평가한다.
        gc.applyPseudo(ps, e);
        text = gc.text(ps, e);
        LayoutBox pb = new LayoutBox(LayoutBox.Type.BLOCK, e, ps);
        pb.pseudo = name;
        initPosition(pb, ps, false);
        pb.bfcRoot = true;
        pb.clipsContent = Css.clips(ps);
        pb.decoration = b.decoration | Css.decoration(ps);
        pb.floatSide = LayoutBox.FLOAT_NONE;
        pb.items = new ArrayList<InlineItem>();
        if (text.length() > 0) {
            pb.items.add(InlineItem.text(text, e, ps, null, pb.decoration));
        }
        if (pb.isAbsolutelyPositioned()) {
            pb.decoration = 0;
            registerAbsolute(pb);
            oof.add(pb);
            return;
        }
        markItem(b, pb);
        double z = ps.number(Prop.Z_INDEX);
        if (!Double.isNaN(z)) {
            pb.hasZ = true;
            pb.zIndex = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(z)));
            pb.stackingContext = true;
        }
        items.add(pb);
    }

    /**
     * Returns the integer <code>order</code> of a flex item (0 for anonymous items).
     * <p>
     * 플렉스 항목의 정수 <code>order</code> 를 반환한다(익명 항목은 0).
     *
     * @param it the item / 항목
     * @return the order value / order 값
     */
    private static int orderOf(LayoutBox it) {
        if (it.type == LayoutBox.Type.ANONYMOUS_BLOCK || it.style == null) {
            return 0;
        }
        double v = it.style.number(Prop.ORDER);
        return Double.isNaN(v) ? 0 : (int) Math.max(-1000000, Math.min(1000000, Math.round(v)));
    }

    /**
     * Sorts the flex items by <code>order</code> (stable: ties keep the document order). The creation numbers of the boxes inside the
     * items are permuted along with the items, so that boxes that are painted in tree order are ordered like the order-modified document
     * order but keep their place relative to the boxes outside the container.
     * <p>
     * 플렉스 항목을 <code>order</code> 로 정렬한다(안정 정렬: 같으면 문서 순서 유지). 항목 안 상자들의 생성 번호도 항목과 함께 치환되므로 트리 순서로 그리는 상자는
     * order 를 반영한 문서 순서대로 정해지고 컨테이너 밖 상자에 대한 상대 위치는 그대로이다.
     *
     * @param items the items (sorted in place) / 항목(제자리 정렬)
     */
    private static void orderFlexItems(List<LayoutBox> items) {
        boolean any = false;
        // order 가 0 이 아닌 항목이 있는지 확인한다.
        for (int i = 0; i < items.size(); i++) {
            if (orderOf(items.get(i)) != 0) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        List<LayoutBox> before = new ArrayList<LayoutBox>(items);
        java.util.Collections.sort(items, new java.util.Comparator<LayoutBox>() {
            /**
             * Compares two items by their order value.
             * <p>
             * 두 항목을 order 값으로 비교한다.
             *
             * @param a first / 첫째
             * @param c second / 둘째
             * @return the comparison / 비교 결과
             */
            public int compare(LayoutBox a, LayoutBox c) {
                int x = orderOf(a);
                int y = orderOf(c);
                return x < y ? -1 : (x == y ? 0 : 1);
            }
        });
        List<LayoutBox> dom = new ArrayList<LayoutBox>();
        Map<LayoutBox, Boolean> seen = new IdentityHashMap<LayoutBox, Boolean>();
        // 원래 문서 순서로 모든 상자의 생성 번호를 모은다.
        for (int i = 0; i < before.size(); i++) {
            collectBoxes(before.get(i), dom, seen);
        }
        int[] nums = new int[dom.size()];
        // 번호를 배열에 옮겨 둔다.
        for (int i = 0; i < nums.length; i++) {
            nums[i] = dom.get(i).order;
        }
        List<LayoutBox> now = new ArrayList<LayoutBox>();
        seen = new IdentityHashMap<LayoutBox, Boolean>();
        // 정렬한 순서로 상자를 훑으며 같은 번호 집합을 순서대로 다시 매긴다.
        for (int i = 0; i < items.size(); i++) {
            collectBoxes(items.get(i), now, seen);
        }
        // 새 순서의 상자에 번호를 차례로 준다.
        for (int i = 0; i < now.size() && i < nums.length; i++) {
            now.get(i).order = nums[i];
        }
    }

    /**
     * Collects the boxes below a box (and the box) that have a creation number, in tree order, including the boxes that are still inline
     * items (atomic boxes, floats, absolutely positioned boxes).
     * <p>
     * 상자(와 그 아래)에서 생성 번호가 있는 상자를 트리 순서로 모은다. 아직 인라인 항목인 상자(원자적 상자, float, 절대 배치 상자)도 포함한다.
     *
     * @param b    the box / 상자
     * @param out  receives the boxes / 상자를 받는다
     * @param seen the boxes already collected / 이미 모은 상자
     */
    private static void collectBoxes(LayoutBox b, List<LayoutBox> out, Map<LayoutBox, Boolean> seen) {
        if (seen.put(b, Boolean.TRUE) != null) {
            return;
        }
        if (b.order > 0) {
            out.add(b);
        }
        // 블록 자식을 차례로 훑는다.
        for (int i = 0; i < b.children.size(); i++) {
            collectBoxes(b.children.get(i), out, seen);
        }
        // 인라인 항목이 가진 상자를 차례로 훑는다.
        if (b.items != null) {
            for (int i = 0; i < b.items.size(); i++) {
                if (b.items.get(i).box != null) {
                    collectBoxes(b.items.get(i).box, out, seen);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------- tables (P10-02)

    /**
     * Tells whether a <code>display</code> value names an internal part of a table (a row group, row, cell, column, column group or
     * caption). Such an element that is not inside a table gets an anonymous table around it (CSS 2.1 17.2.1).
     * <p>
     * <code>display</code> 값이 표의 내부 부품(행 그룹, 행, 셀, 열, 열 그룹, caption)인지 알려 준다. 표 밖에 있는 그런 요소는 익명 표로 감싼다(CSS 2.1 17.2.1).
     *
     * @param d the display value / display 값
     * @return true for internal table parts / 표 내부 부품이면 true
     */
    static boolean isTableInternal(String d) {
        return "table-row-group".equals(d) || "table-header-group".equals(d) || "table-footer-group".equals(d) || "table-row".equals(d)
                || "table-cell".equals(d) || "table-column-group".equals(d) || "table-column".equals(d) || "table-caption".equals(d);
    }

    /**
     * Collects the element and text children of a node for a table: <code>display:none</code> children are dropped and the children
     * of <code>display:contents</code> elements take their place.
     * <p>
     * 표를 위해 노드의 요소와 텍스트 자식을 모은다: <code>display:none</code> 자식은 버리고 <code>display:contents</code> 요소의 자식이 그 자리에 들어온다.
     *
     * @param parent the node / 노드
     * @param out    receives the nodes / 노드를 받는다
     * @param depth  nesting depth / 중첩 깊이
     */
    private void flatten(Node parent, List<Node> out, int depth) {
        // 자식을 차례로 보며 보이지 않는 요소는 버리고 display:contents 요소는 자식으로 펼친다.
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                String d = styles.getStyle((Element) n).display();
                if ("none".equals(d)) {
                    continue;
                }
                if ("contents".equals(d)) {
                    if (depth < MAX_DEPTH) {
                        flatten(n, out, depth + 1);
                    }
                    continue;
                }
                out.add(n);
            } else if (n instanceof Text) {
                out.add(n);
            }
        }
    }

    /**
     * Tells whether a node is a text node that only holds white space (it is dropped between the parts of a table).
     * <p>
     * 노드가 공백만 가진 텍스트 노드인지 알려 준다(표의 부품 사이에서는 버린다).
     *
     * @param n the node / 노드
     * @return true for blank text / 공백뿐인 텍스트이면 true
     */
    private static boolean isBlankText(Node n) {
        if (!(n instanceof Text)) {
            return false;
        }
        String d = ((Text) n).getData();
        // 문자를 하나씩 보며 공백이 아닌 글자가 있는지 확인한다.
        for (int i = 0; i < d.length(); i++) {
            char ch = d.charAt(i);
            if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f') {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a positive integer attribute of an HTML table element (<code>colspan</code>, <code>rowspan</code>, <code>span</code>)
     * following the HTML rules: a missing or invalid value gives the default and values are limited.
     * <p>
     * HTML 표 요소의 양의 정수 속성(<code>colspan</code>, <code>rowspan</code>, <code>span</code>)을 HTML 규칙에 따라 읽는다: 없거나 잘못된 값은 기본값이고 값은 제한된다.
     *
     * @param e    the element or null / 요소 또는 null
     * @param name the attribute name / 속성 이름
     * @param def  the default / 기본값
     * @param min  the smallest value / 가장 작은 값
     * @param max  the largest value / 가장 큰 값
     * @return the value / 값
     */
    private static int intAttribute(Element e, String name, int def, int min, int max) {
        if (e == null || !isHtml(e)) {
            return def;
        }
        String v = e.getAttribute(name);
        if (v == null) {
            return def;
        }
        String t = v.trim();
        int i = 0;
        // 앞쪽의 숫자만 읽는다(HTML 의 음이 아닌 정수 해석).
        while (i < t.length() && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
        }
        if (i == 0) {
            return def;
        }
        try {
            long n = Long.parseLong(t.substring(0, Math.min(i, 9)));
            return (int) Math.max(min, Math.min(max, n));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Builds a table box from the nodes that make up its content (CSS 2.1 17.2.1): captions, column groups and columns, row groups, rows
     * and cells are built as boxes, and whatever is missing in between is created as anonymous boxes: loose rows and cells go into an
     * anonymous row group, loose cells and other content into an anonymous row and an anonymous cell. The first header group is placed
     * before and the first footer group after all other row groups.
     * <p>
     * 표의 내용을 이루는 노드로 표 상자를 만든다(CSS 2.1 17.2.1): caption, 열 그룹과 열, 행 그룹, 행, 셀은 상자로 만들고 그 사이에 빠진 것은 익명 상자로 만든다:
     * 홀로 있는 행과 셀은 익명 행 그룹에, 홀로 있는 셀과 다른 콘텐츠는 익명 행과 익명 셀에 넣는다. 첫 헤더 그룹은 다른 모든 행 그룹 앞에, 첫 푸터 그룹은 뒤에 놓는다.
     *
     * @param te     the table element or null for an anonymous table / 표 요소. 익명 표이면 null
     * @param ts     the style (of the table, or of the parent for an anonymous table) / 스타일(표의 것, 익명 표는 부모의 것)
     * @param kids   the content nodes / 내용 노드
     * @param depth  nesting depth / 중첩 깊이
     * @param inline true for <code>display:inline-table</code> / <code>display:inline-table</code> 이면 true
     * @return the table box / 표 상자
     */
    private LayoutBox buildTable(Element te, ComputedStyle ts, List<Node> kids, int depth, boolean inline) {
        LayoutBox t = new LayoutBox(LayoutBox.Type.TABLE, te, ts);
        if (te != null) {
            initPosition(t, ts, false);
            t.clipsContent = Css.clips(ts);
        } else {
            t.order = ++seq;
        }
        t.bfcRoot = true;
        t.inlineLevel = inline;
        LayoutBox saved = cb;
        LayoutBox savedTf = tfCb;
        boolean transformedTable = te != null && (Css.hasTransform(ts) || FilterSpec.has(ts) || FilterSpec.hasBackdrop(ts) || Containment.layoutOrPaint(ts));
        if (t.posKind != LayoutBox.POS_STATIC || transformedTable) {
            cb = t;
        }
        if (transformedTable) {
            tfCb = t;
        }
        List<LayoutBox> captions = new ArrayList<LayoutBox>();
        List<LayoutBox> columns = new ArrayList<LayoutBox>();
        List<LayoutBox> groups = new ArrayList<LayoutBox>();
        LayoutBox header = null;
        LayoutBox footer = null;
        LayoutBox anonGroup = null;
        LayoutBox anonRow = null;
        List<Node> run = new ArrayList<Node>();
        // 자식을 차례로 보며 부품은 상자로, 홀로 있는 것은 익명 상자 안에 모은다.
        for (int i = 0; i < kids.size(); i++) {
            Node n = kids.get(i);
            String d = n instanceof Element ? styles.getStyle((Element) n).display() : "";
            if (n instanceof Text || !(isTableInternal(d))) {
                if (!isBlankText(n)) {
                    run.add(n);
                }
                continue;
            }
            Element e = (Element) n;
            ComputedStyle s = styles.getStyle(e);
            if ("table-cell".equals(d)) {
                if (anonGroup == null) {
                    anonGroup = anonymous(LayoutBox.Type.TABLE_ROW_GROUP, t.style);
                    groups.add(anonGroup);
                }
                if (anonRow == null) {
                    anonRow = anonymous(LayoutBox.Type.TABLE_ROW, t.style);
                    anonGroup.add(anonRow);
                }
                flushRun(anonRow, run, depth);
                anonRow.add(buildCell(e, s, depth));
                continue;
            }
            // 셀이 아닌 부품을 만나면 모아 둔 콘텐츠를 익명 셀로 내보낸다.
            if (!run.isEmpty()) {
                if (anonGroup == null) {
                    anonGroup = anonymous(LayoutBox.Type.TABLE_ROW_GROUP, t.style);
                    groups.add(anonGroup);
                }
                if (anonRow == null) {
                    anonRow = anonymous(LayoutBox.Type.TABLE_ROW, t.style);
                    anonGroup.add(anonRow);
                }
                flushRun(anonRow, run, depth);
            }
            if ("table-row".equals(d)) {
                if (anonGroup == null) {
                    anonGroup = anonymous(LayoutBox.Type.TABLE_ROW_GROUP, t.style);
                    groups.add(anonGroup);
                }
                anonRow = null;
                anonGroup.add(buildRow(e, s, depth));
                continue;
            }
            anonGroup = null;
            anonRow = null;
            if ("table-caption".equals(d)) {
                captions.add(buildCaption(e, s, depth));
            } else if ("table-column-group".equals(d) || "table-column".equals(d)) {
                columns.add(buildColumn(e, s, d, depth));
            } else {
                LayoutBox g = buildRowGroup(e, s, depth);
                groups.add(g);
                if (header == null && "table-header-group".equals(d)) {
                    header = g;
                } else if (footer == null && "table-footer-group".equals(d)) {
                    footer = g;
                }
            }
        }
        if (!run.isEmpty()) {
            if (anonGroup == null) {
                anonGroup = anonymous(LayoutBox.Type.TABLE_ROW_GROUP, t.style);
                groups.add(anonGroup);
            }
            if (anonRow == null) {
                anonRow = anonymous(LayoutBox.Type.TABLE_ROW, t.style);
                anonGroup.add(anonRow);
            }
            flushRun(anonRow, run, depth);
        }
        // 첫 헤더 그룹은 맨 앞에, 첫 푸터 그룹은 맨 뒤에 놓는다.
        if (header != null) {
            groups.remove(header);
            groups.add(0, header);
        }
        if (footer != null) {
            groups.remove(footer);
            groups.add(footer);
        }
        // 자식 순서는 그리기 순서이다: caption, 열 그룹과 열, 행 그룹.
        for (int i = 0; i < captions.size(); i++) {
            t.add(captions.get(i));
        }
        // 열 그룹과 열 상자를 표의 자식으로 붙인다.
        for (int i = 0; i < columns.size(); i++) {
            t.add(columns.get(i));
        }
        // 행 그룹 상자를 표시 순서대로 표의 자식으로 붙인다.
        for (int i = 0; i < groups.size(); i++) {
            t.add(groups.get(i));
        }
        if (te != null && isCollapsingTable(te, captions, columns, groups)) {
            // 호환 모드의 "collapsing table quirk"(호환 모드 표준 3.10): 행 그룹, 캡션, 열이 있는 열 그룹이 없는 표는 높이 0, border-style none 이다.
            ComputedStyle flat = ts.with(Prop.HEIGHT, CssLength.px(0));
            flat = flat.with(Prop.BORDER_TOP_STYLE, CssKeyword.NONE).with(Prop.BORDER_RIGHT_STYLE, CssKeyword.NONE);
            t.style = flat.with(Prop.BORDER_BOTTOM_STYLE, CssKeyword.NONE).with(Prop.BORDER_LEFT_STYLE, CssKeyword.NONE);
        }
        cb = saved;
        tfCb = savedTf;
        return t;
    }

    /**
     * Tells whether a table is a "collapsing table" of quirks mode: the document is in quirks mode and the table has no row group box,
     * no caption box and no column group box that has a column (Quirks Mode Standard 3.10).
     * <p>
     * 표가 호환 모드의 "collapsing table" 인지 알려 준다: 문서가 호환 모드이고 표에 행 그룹 상자, 캡션 상자, 열이 있는 열 그룹 상자가 없다(호환 모드 표준 3.10).
     *
     * @param te       the table element / 표 요소
     * @param captions the caption boxes / 캡션 상자
     * @param columns  the column and column group boxes / 열과 열 그룹 상자
     * @param groups   the row group boxes / 행 그룹 상자
     * @return true for a collapsing table / collapsing table 이면 true
     */
    private static boolean isCollapsingTable(Element te, List<LayoutBox> captions, List<LayoutBox> columns, List<LayoutBox> groups) {
        Document d = te.getOwnerDocument();
        if (d == null || d.getCompatibilityMode() != Document.QUIRKS_MODE || !captions.isEmpty() || !groups.isEmpty()) {
            return false;
        }
        // 열이 있는 열 그룹(colSpan 이 0 으로 표시됨)이 하나라도 있으면 collapsing table 이 아니다.
        for (int i = 0; i < columns.size(); i++) {
            LayoutBox c = columns.get(i);
            if (c.type == LayoutBox.Type.TABLE_COLUMN_GROUP && c.colSpan == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates an anonymous table part.
     * <p>
     * 익명 표 부품을 만든다.
     *
     * @param type  the kind of part / 부품 종류
     * @param style the style of the parent / 부모의 스타일
     * @return the box / 상자
     */
    private LayoutBox anonymous(LayoutBox.Type type, ComputedStyle style) {
        LayoutBox b = new LayoutBox(type, null, style);
        b.order = ++seq;
        return b;
    }

    /**
     * Turns the collected loose content into an anonymous cell of a row.
     * <p>
     * 모아 둔 홀로 있는 콘텐츠를 행의 익명 셀로 만든다.
     *
     * @param row   the row / 행
     * @param run   the content nodes (cleared) / 콘텐츠 노드(비워진다)
     * @param depth nesting depth / 중첩 깊이
     */
    private void flushRun(LayoutBox row, List<Node> run, int depth) {
        if (run.isEmpty()) {
            return;
        }
        LayoutBox cell = anonymous(LayoutBox.Type.TABLE_CELL, row.style);
        cell.bfcRoot = true;
        fillNodes(cell, run, depth + 1);
        row.add(cell);
        run.clear();
    }

    /**
     * Fills a container with the content of a list of sibling nodes.
     * <p>
     * 형제 노드 목록의 내용으로 컨테이너를 채운다.
     *
     * @param b     the container / 컨테이너
     * @param nodes the nodes / 노드
     * @param depth nesting depth / 중첩 깊이
     */
    private void fillNodes(LayoutBox b, List<Node> nodes, int depth) {
        Ctx c = new Ctx(b);
        // 노드를 차례로 컨테이너의 내용에 더한다.
        for (int i = 0; i < nodes.size(); i++) {
            add(c, nodes.get(i), depth);
        }
        c.finish();
    }

    /**
     * Builds a table cell box.
     * <p>
     * 표 셀 상자를 만든다.
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox buildCell(Element e, ComputedStyle s, int depth) {
        LayoutBox cell = new LayoutBox(LayoutBox.Type.TABLE_CELL, e, s);
        initPosition(cell, s, false);
        cell.floatSide = LayoutBox.FLOAT_NONE;
        cell.bfcRoot = true;
        cell.decoration = Css.decoration(s);
        cell.clipsContent = Css.clips(s);
        cell.colSpan = intAttribute(e, "colspan", 1, 1, 1000);
        cell.rowSpan = intAttribute(e, "rowspan", 1, 0, 65534);
        fillPositioned(cell, e, depth + 1, false);
        return cell;
    }

    /**
     * Builds a table row box with its cells (loose content between the cells goes into anonymous cells).
     * <p>
     * 표 행 상자를 셀과 함께 만든다(셀 사이의 홀로 있는 콘텐츠는 익명 셀에 들어간다).
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox buildRow(Element e, ComputedStyle s, int depth) {
        LayoutBox row = new LayoutBox(LayoutBox.Type.TABLE_ROW, e, s);
        initPosition(row, s, false);
        row.floatSide = LayoutBox.FLOAT_NONE;
        List<Node> kids = new ArrayList<Node>();
        if (depth < MAX_DEPTH) {
            flatten(e, kids, depth);
        }
        List<Node> run = new ArrayList<Node>();
        // 자식 중 셀은 상자로 만들고 나머지는 모아 익명 셀로 만든다.
        for (int i = 0; i < kids.size(); i++) {
            Node n = kids.get(i);
            if (n instanceof Element && "table-cell".equals(styles.getStyle((Element) n).display())) {
                flushRun(row, run, depth);
                row.add(buildCell((Element) n, styles.getStyle((Element) n), depth));
            } else if (!isBlankText(n)) {
                run.add(n);
            }
        }
        flushRun(row, run, depth);
        return row;
    }

    /**
     * Builds a row group box with its rows (loose cells and other content go into anonymous rows).
     * <p>
     * 행 그룹 상자를 행과 함께 만든다(홀로 있는 셀과 다른 콘텐츠는 익명 행에 들어간다).
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox buildRowGroup(Element e, ComputedStyle s, int depth) {
        LayoutBox g = new LayoutBox(LayoutBox.Type.TABLE_ROW_GROUP, e, s);
        initPosition(g, s, false);
        g.floatSide = LayoutBox.FLOAT_NONE;
        List<Node> kids = new ArrayList<Node>();
        if (depth < MAX_DEPTH) {
            flatten(e, kids, depth);
        }
        LayoutBox anonRow = null;
        List<Node> run = new ArrayList<Node>();
        // 자식 중 행은 상자로 만들고 셀과 다른 콘텐츠는 익명 행의 익명 셀로 모은다.
        for (int i = 0; i < kids.size(); i++) {
            Node n = kids.get(i);
            String d = n instanceof Element ? styles.getStyle((Element) n).display() : "";
            if ("table-row".equals(d)) {
                if (anonRow != null) {
                    flushRun(anonRow, run, depth);
                } else if (!run.isEmpty()) {
                    anonRow = anonymous(LayoutBox.Type.TABLE_ROW, g.style);
                    g.add(anonRow);
                    flushRun(anonRow, run, depth);
                }
                anonRow = null;
                g.add(buildRow((Element) n, styles.getStyle((Element) n), depth));
            } else if ("table-cell".equals(d)) {
                if (anonRow == null) {
                    anonRow = anonymous(LayoutBox.Type.TABLE_ROW, g.style);
                    g.add(anonRow);
                }
                flushRun(anonRow, run, depth);
                anonRow.add(buildCell((Element) n, styles.getStyle((Element) n), depth));
            } else if (!isBlankText(n) && !isTableInternal(d)) {
                run.add(n);
            }
        }
        if (!run.isEmpty()) {
            if (anonRow == null) {
                anonRow = anonymous(LayoutBox.Type.TABLE_ROW, g.style);
                g.add(anonRow);
            }
            flushRun(anonRow, run, depth);
        }
        return g;
    }

    /**
     * Builds a table caption box.
     * <p>
     * 표 caption 상자를 만든다.
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox buildCaption(Element e, ComputedStyle s, int depth) {
        LayoutBox cap = new LayoutBox(LayoutBox.Type.TABLE_CAPTION, e, s);
        initPosition(cap, s, false);
        cap.floatSide = LayoutBox.FLOAT_NONE;
        cap.bfcRoot = true;
        cap.decoration = Css.decoration(s);
        cap.clipsContent = Css.clips(s);
        fillPositioned(cap, e, depth + 1, false);
        return cap;
    }

    /**
     * Builds a column or a column group box (a column group holds its <code>col</code> children, or spans the columns of its
     * <code>span</code> attribute if it has none).
     * <p>
     * 열 또는 열 그룹 상자를 만든다(열 그룹은 <code>col</code> 자식을 가지며 없으면 <code>span</code> 속성의 열 수를 차지한다).
     *
     * @param e     the element / 요소
     * @param s     its style / 스타일
     * @param d     its display value / display 값
     * @param depth nesting depth / 중첩 깊이
     * @return the box / 상자
     */
    private LayoutBox buildColumn(Element e, ComputedStyle s, String d, int depth) {
        boolean group = "table-column-group".equals(d);
        LayoutBox c = new LayoutBox(group ? LayoutBox.Type.TABLE_COLUMN_GROUP : LayoutBox.Type.TABLE_COLUMN, e, s);
        c.order = ++seq;
        c.colSpan = intAttribute(e, "span", 1, 1, 1000);
        if (group) {
            List<Node> kids = new ArrayList<Node>();
            if (depth < MAX_DEPTH) {
                flatten(e, kids, depth);
            }
            boolean any = false;
            // 열 그룹의 자식 중 열만 상자로 만든다.
            for (int i = 0; i < kids.size(); i++) {
                Node n = kids.get(i);
                if (n instanceof Element && "table-column".equals(styles.getStyle((Element) n).display())) {
                    c.add(buildColumn((Element) n, styles.getStyle((Element) n), "table-column", depth));
                    any = true;
                }
            }
            if (any) {
                c.colSpan = 0;
            }
        }
        return c;
    }

    /**
     * Returns the elements of the top layer of a document that are rendered (CSS Positioned Layout 4 section 3.1: an element whose inclusive ancestor has
     * <code>display: none</code> is not rendered), or null if there is none.
     * <p>
     * 문서의 최상위 계층에서 그려지는 요소를 반환한다(CSS Positioned Layout 4 3.1 절: 자신이나 조상이 <code>display: none</code> 인 요소는 그려지지 않는다). 없으면 null.
     *
     * @param doc the document / 문서
     * @return the elements in order, or null / 순서대로의 요소. 없으면 null
     */
    private List<Element> renderedTopLayer(Document doc) {
        List<Element> all = doc.getTopLayer();
        List<Element> out = new ArrayList<Element>();
        // 요소마다 자신과 조상이 모두 표시되는지 확인한다.
        for (int i = 0; i < all.size(); i++) {
            boolean shown = true;
            // 요소에서 루트까지 올라가며 display:none 을 찾는다.
            for (Element a = all.get(i); a != null && shown; a = a.getParentElement()) {
                shown = !"none".equals(styles.getStyle(a).display());
            }
            if (shown) {
                out.add(all.get(i));
            }
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * Adds the top layer to the end of the root element (CSS Positioned Layout 4 sections 3.1 and 3.2): for every element in order, its
     * <code>::backdrop</code> (when a rule targets it and its <code>display</code> and <code>content</code> are not <code>none</code>) and then the element
     * itself. Both are stacking contexts that are painted above everything else of the document, in this order.
     * <p>
     * 루트 요소의 끝에 최상위 계층을 더한다(CSS Positioned Layout 4 3.1, 3.2 절): 요소마다 순서대로 그 <code>::backdrop</code>(규칙이 가리키고 <code>display</code> 와
     * <code>content</code> 가 <code>none</code> 이 아닐 때), 그다음 요소 자신. 둘 다 문서의 다른 모든 것 위에 이 순서로 그려지는 쌓임 맥락이다.
     *
     * @param c     the collection state of the root / 루트의 모으는 상태
     * @param depth the depth / 깊이
     */
    private void addTopLayer(Ctx c, int depth) {
        // 최상위 계층의 요소마다 배경막과 요소의 상자를 만든다.
        for (int i = 0; i < topLayer.size(); i++) {
            Element t = topLayer.get(i);
            ComputedStyle bs = styles.getPseudoStyle(t, "backdrop");
            if (bs != null && !"none".equals(bs.display()) && !bs.isKeyword(Prop.CONTENT, "none")) {
                LayoutBox bd = new LayoutBox(LayoutBox.Type.BLOCK, t, bs);
                bd.pseudo = "backdrop";
                bd.items = new ArrayList<InlineItem>();
                bd.bfcRoot = true;
                initPosition(bd, bs, false);
                bd.topLayer = true;
                bd.stackingContext = true;
                if (bd.isAbsolutelyPositioned()) {
                    addOutOfFlow(c, bd);
                } else {
                    c.addBlock(bd);
                }
            }
            int from = c.run.size();
            addingTopLayer = true;
            try {
                add(c, t, depth + 1);
            } finally {
                addingTopLayer = false;
            }
            // 방금 만든 요소의 상자를 최상위 계층의 쌓임 맥락으로 표시한다.
            for (int k = from; k < c.run.size(); k++) {
                LayoutBox made = c.run.get(k).box;
                if (made != null && made.node == t) {
                    made.topLayer = true;
                    made.stackingContext = true;
                }
            }
        }
    }

    /**
     * Generates the content of <code>::before</code> or <code>::after</code> of an element.
     * <p>
     * 요소의 <code>::before</code> 또는 <code>::after</code> 콘텐츠를 생성한다.
     *
     * @param c     collection state / 모으는 상태
     * @param e     the originating element / 기원 요소
     * @param name  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @param depth depth / 깊이
     */
    private void pseudo(Ctx c, Element e, String name, int depth) {
        // 요소의 카운터는 ::before 직전에 적용하고 ::after 뒤에 범위를 닫는다(P10-04).
        if ("before".equals(name)) {
            gc.enter(e, styles.getStyle(e));
        }
        pseudoContent(c, e, name, depth);
        if ("after".equals(name)) {
            gc.exit(e);
        }
    }

    /**
     * Adds the generated content of a pseudo-element to the container being collected.
     * <p>
     * 의사 요소의 생성 콘텐츠를 모으는 중인 컨테이너에 더한다.
     *
     * @param c     collection state / 모으는 상태
     * @param e     the originating element / 기원 요소
     * @param name  <code>before</code> or <code>after</code> / <code>before</code> 또는 <code>after</code>
     * @param depth depth / 깊이
     */
    private void pseudoContent(Ctx c, Element e, String name, int depth) {
        c.flushTable();
        ComputedStyle ps = styles.getPseudoStyle(e, name);
        if (ps == null) {
            return;
        }
        String text = gc.text(ps, e);
        String d = ps.display();
        if (text == null || "none".equals(d)) {
            return;
        }
        // 의사 요소의 카운터 속성은 자신의 content 를 평가하기 전에 적용한다(CSS Lists 3 4.4). 그래서 증가한 값으로 content 를 다시 평가한다.
        gc.applyPseudo(ps, e);
        text = gc.text(ps, e);
        if (Css.isInline(d)) {
            LayoutBox proto = new LayoutBox(LayoutBox.Type.INLINE, e, ps);
            proto.pseudo = name;
            proto.inlineLevel = true;
            proto.inlineParent = c.current();
            proto.decoration = c.decoration() | Css.decoration(ps);
            InlineItem o = new InlineItem(InlineItem.Kind.OPEN);
            o.box = proto;
            o.edge = true;
            c.run.add(o);
            c.open.add(proto);
            if (text.length() > 0) {
                c.addText(text, e);
            }
            c.open.remove(c.open.size() - 1);
            InlineItem cl = new InlineItem(InlineItem.Kind.CLOSE);
            cl.box = proto;
            cl.edge = true;
            c.run.add(cl);
            return;
        }
        boolean atomic = Css.isAtomicInline(d);
        LayoutBox b = new LayoutBox(atomic ? LayoutBox.Type.INLINE_BLOCK : LayoutBox.Type.BLOCK, e, ps);
        b.pseudo = name;
        b.inlineLevel = atomic;
        b.bfcRoot = atomic || isBfcRoot(ps);
        b.clipsContent = Css.clips(ps);
        b.decoration = c.decoration() | Css.decoration(ps);
        b.items = new ArrayList<InlineItem>();
        if (text.length() > 0) {
            b.items.add(InlineItem.text(text, e, ps, null, b.decoration));
        }
        initPosition(b, ps, false);
        if (b.floatSide != LayoutBox.FLOAT_NONE || b.isAbsolutelyPositioned()) {
            // float 나 절대 배치인 생성 콘텐츠는 흐름 밖 항목으로 등록한다.
            b.decoration = 0;
            addOutOfFlow(c, b);
        } else if (atomic) {
            c.addAtomic(b);
        } else {
            c.addBlock(b);
        }
    }

    /**
     * Tells whether an element belongs to the HTML namespace.
     * <p>
     * 요소가 HTML 네임스페이스에 속하는지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for HTML elements / HTML 요소이면 true
     */
    private static boolean isHtml(Element e) {
        return Namespaces.HTML.equals(e.getNamespaceURI()) || e.getNamespaceURI() == null;
    }

    /**
     * Lower-cases a tag name without locale surprises.
     * <p>
     * 로케일 문제 없이 태그 이름을 소문자로 바꾼다.
     *
     * @param s the name / 이름
     * @return lower-case name / 소문자 이름
     */
    static String lower(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }
}
