package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;

/**
 * The layout engine (plan items P09-01, P09-02, P09-03): turns a DOM document and the computed styles into a tree of boxes with
 * coordinates, see {@link LayoutBox} and {@link LayoutResult}. The engine is stateless; every call builds a new result from scratch
 * (a full relayout), so a width change is handled by calling it again with the new viewport width. The caller (the document thread)
 * decides when to relayout: only when the viewport width, the DOM, the styles or the replaced content changed, never from inside a layout
 * callback, so relayout cannot loop.
 * <p>
 * Threading: the call reads the DOM and the styles and must run on the thread that owns the document (it blocks the caller). The work is
 * done on a helper thread with a large stack, because block nesting up to {@link BoxBuilder#MAX_DEPTH} levels is laid out recursively; the
 * helper never writes to the document. The returned {@link LayoutResult} is immutable by convention and may be read on any thread.
 * Layout cannot be cancelled while it runs.
 * <p>
 * Floats, <code>clear</code>, relative, absolute and fixed positioning, stacking order and clipping are implemented (P10-01);
 * <code>position:sticky</code> is laid out like <code>position:relative</code> without an offset (no scroll-dependent shift). Tables
 * (P10-02), flex containers, multi-column containers, grid containers and vertical writing modes (P10-03, see {@link FlexLayouter},
 * {@link MulticolLayouter}, {@link GridLayouter} and {@link OrthogonalLayouter}) are implemented.
 * <p>
 * 레이아웃 엔진(계획 P09-01, P09-02, P09-03): DOM 문서와 계산 스타일을 좌표를 가진 상자 트리로 바꾼다({@link LayoutBox}, {@link LayoutResult} 참고).
 * 엔진은 상태가 없고 호출마다 새 결과를 처음부터 만든다(전체 재배치). 너비 변화는 새 뷰포트 너비로 다시 호출해서 처리한다. 재배치 시점은 호출자(문서 스레드)가
 * 정한다: 뷰포트 너비, DOM, 스타일, 치환 콘텐츠가 바뀌었을 때만 하며 레이아웃 콜백 안에서는 하지 않으므로 재배치가 반복되지 않는다.
 * <p>
 * 스레드: 호출은 DOM 과 스타일을 읽으며 문서를 소유한 스레드에서 실행해야 한다(호출자를 막는다). {@link BoxBuilder#MAX_DEPTH} 단계까지의 블록 중첩을 재귀로
 * 배치하므로 큰 스택을 가진 보조 스레드에서 작업하며 보조 스레드는 문서에 쓰지 않는다. 반환된 {@link LayoutResult} 는 관례상 불변이며 어느 스레드에서나
 * 읽을 수 있다. 실행 중인 레이아웃은 취소할 수 없다.
 * <p>
 * float, <code>clear</code>, 상대/절대/고정 배치, 쌓임 순서, 자르기를 구현했다(P10-01). <code>position:sticky</code> 는 오프셋이 없는
 * <code>position:relative</code> 처럼 배치한다(스크롤에 따라 움직이지 않음). 표(P10-02), 플렉스 컨테이너, 다단 컨테이너, 그리드 컨테이너, 세로 쓰기 모드(P10-03, {@link FlexLayouter},
 * {@link MulticolLayouter}, {@link GridLayouter}, {@link OrthogonalLayouter} 참조)는 구현했다.
 */
public final class LayoutEngine {

    /** Stack size of the layout helper thread: 64 MiB (reserved lazily by the VM). 레이아웃 보조 스레드의 스택 크기: 64 MiB(VM 이 필요할 때 확보). */
    private static final long STACK_SIZE = 64L * 1024 * 1024;

    /**
     * Private constructor: static entry point only.
     * <p>
     * 비공개 생성자: 정적 진입점만 있다.
     */
    private LayoutEngine() {
    }

    /**
     * Lays out a document.
     * <p>
     * 문서를 배치한다.
     *
     * @param doc            the document / 문서
     * @param styles         source of computed styles / 계산 스타일 공급자
     * @param replaced       source of replaced content (may be null) / 치환 콘텐츠 공급자(null 가능)
     * @param viewportWidth  width of the viewport in CSS pixels (the width of the initial containing block) / CSS 픽셀 뷰포트 너비(초기 포함 블록의 너비)
     * @param viewportHeight height of the viewport (the base of percentage heights of the root) / 뷰포트 높이(루트의 백분율 높이 기준)
     * @return the result / 결과
     * @throws IllegalArgumentException if doc or styles is null / doc 이나 styles 가 null 이면
     */
    public static LayoutResult layout(final Document doc, final StyleSource styles, final ReplacedResolver replaced, final double viewportWidth,
            final double viewportHeight) {
        return layout(doc, styles, replaced, viewportWidth, viewportHeight, null);
    }

    /**
     * Lays out a document with the web fonts of the page (P10-05): the families that <code>@font-face</code> rules declare are looked up
     * in <code>webFonts</code> before the installed fonts, for the time of this layout only.
     * <p>
     * 페이지의 웹 글꼴로 문서를 배치한다(P10-05): <code>@font-face</code> 규칙이 선언한 패밀리는 이 레이아웃 동안만 설치된 글꼴보다 먼저 <code>webFonts</code> 에서 찾는다.
     *
     * @param doc            the document / 문서
     * @param styles         source of computed styles / 계산 스타일 공급자
     * @param replaced       source of replaced content (may be null) / 치환 콘텐츠 공급자(null 가능)
     * @param viewportWidth  width of the viewport in CSS pixels / CSS 픽셀 뷰포트 너비
     * @param viewportHeight height of the viewport / 뷰포트 높이
     * @param webFonts       the web fonts of the page, may be null / 페이지의 웹 글꼴. null 가능
     * @return the result / 결과
     * @throws IllegalArgumentException if doc or styles is null / doc 이나 styles 가 null 이면
     */
    public static LayoutResult layout(final Document doc, final StyleSource styles, final ReplacedResolver replaced, final double viewportWidth,
            final double viewportHeight, final WebFontSource webFonts) {
        if (doc == null || styles == null) {
            throw new IllegalArgumentException("document and styles must not be null");
        }
        final double vw = Double.isNaN(viewportWidth) ? 0 : Math.max(0, viewportWidth);
        final double vh = Double.isNaN(viewportHeight) ? 0 : Math.max(0, viewportHeight);
        final LayoutResult[] result = new LayoutResult[1];
        final Throwable[] failure = new Throwable[1];
        Thread t = new Thread(null, new Runnable() {
            /**
             * Runs the layout on the helper thread.
             * <p>
             * 보조 스레드에서 레이아웃을 실행한다.
             */
            public void run() {
                try {
                    FontResolver.setWebFonts(webFonts);
                    // 웹 글꼴은 페이지의 @font-feature-values 로 font-variant-alternates 의 이름을 푼다(CSS Fonts 4 6.9 절).
                    if (webFonts instanceof WebFontSet && styles instanceof CssStyleSource) {
                        ((WebFontSet) webFonts).setFeatureValues(((CssStyleSource) styles).getFontFeatureValues());
                    }
                    result[0] = layoutWithContainers(doc, styles, replaced, vw, vh);
                    // contain-intrinsic-size: auto 인 요소의 크기를 기억한다(CSS Box Sizing 4 5.2.1 절).
                    Containment.rememberSizes(doc, result[0]);
                } catch (Throwable e) {
                    failure[0] = e;
                } finally {
                    FontResolver.setWebFonts(null);
                }
            }
        }, "swingwebpanel-layout", STACK_SIZE);
        t.setDaemon(true);
        t.start();
        boolean interrupted = false;
        // 보조 스레드가 끝날 때까지 기다린다(인터럽트는 기억했다가 끝난 뒤 다시 건다).
        while (true) {
            try {
                t.join();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (failure[0] instanceof RuntimeException) {
            throw (RuntimeException) failure[0];
        }
        if (failure[0] instanceof Error) {
            throw (Error) failure[0];
        }
        return result[0];
    }

    /** The number of extra layout passes allowed when container query results change the styles. 컨테이너 질의 결과가 스타일을 바꿀 때 허용하는 추가 레이아웃 횟수. */
    private static final int MAX_CONTAINER_PASSES = 4;

    /**
     * Lays out the document and, if the style source evaluates container queries, repeats the layout while the measured sizes of the query containers
     * change the styles (a container query depends on the layout of its container, so the first pass uses the sizes of the previous layout and the
     * passes that follow settle it; at most {@link #MAX_CONTAINER_PASSES} extra passes, as a cyclic dependency never settles).
     * <p>
     * 문서를 배치하고, 스타일 공급자가 컨테이너 질의를 평가하면 잰 질의 컨테이너 크기가 스타일을 바꾸는 동안 레이아웃을 되풀이한다(컨테이너 질의는 컨테이너의 레이아웃에 달려
     * 있으므로 첫 배치는 이전 레이아웃의 크기를 쓰고 뒤따르는 배치가 안정시킨다. 순환 의존은 안정되지 않으므로 추가 배치는 최대 {@link #MAX_CONTAINER_PASSES} 번).
     *
     * @param doc      the document / 문서
     * @param styles   styles / 스타일
     * @param replaced replaced content or null / 치환 콘텐츠 또는 null
     * @param vw       viewport width / 뷰포트 너비
     * @param vh       viewport height / 뷰포트 높이
     * @return the result of the last pass / 마지막 배치의 결과
     */
    private static LayoutResult layoutWithContainers(Document doc, StyleSource styles, ReplacedResolver replaced, double vw, double vh) {
        LayoutResult r = doLayout(doc, styles, replaced, vw, vh);
        if (!(styles instanceof ContainerSource)) {
            return r;
        }
        ContainerSource cs = (ContainerSource) styles;
        // 컨테이너 크기가 바뀌어 스타일이 달라졌으면 다시 배치한다.
        for (int pass = 0; pass < MAX_CONTAINER_PASSES && cs.usesContainerQueries(); pass++) {
            if (!cs.updateContainerSizes(measureContainers(r.getRoot()))) {
                break;
            }
            r = doLayout(doc, styles, replaced, vw, vh);
        }
        return r;
    }

    /**
     * Measures the query containers of a layout: the content box size of every block-level box whose element has a <code>container-type</code>.
     * <p>
     * 레이아웃의 질의 컨테이너를 잰다: 요소에 <code>container-type</code> 이 있는 모든 블록 수준 상자의 content box 크기.
     *
     * @param root the root box / 루트 상자
     * @return the sizes {width, height} by element / 요소별 크기 {너비, 높이}
     */
    private static java.util.Map<org.duckdns.hjow.swingwebpanel.dom.Element, double[]> measureContainers(LayoutBox root) {
        java.util.Map<org.duckdns.hjow.swingwebpanel.dom.Element, double[]> out = new java.util.IdentityHashMap<org.duckdns.hjow.swingwebpanel.dom.Element, double[]>();
        java.util.ArrayList<LayoutBox> stack = new java.util.ArrayList<LayoutBox>();
        stack.add(root);
        // 명시적 스택으로 모든 상자를 훑으며 컨테이너 요소의 첫 상자를 기록한다.
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            boolean boxy = b.type == LayoutBox.Type.BLOCK || b.type == LayoutBox.Type.INLINE_BLOCK || b.type == LayoutBox.Type.TABLE;
            if (boxy && b.pseudo == null && b.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && Containment.isContainer(b.style)
                    && !out.containsKey(b.node)) {
                double w = Math.max(0, b.width - b.borderLeft - b.borderRight - b.paddingLeft - b.paddingRight);
                double h = Math.max(0, b.height - b.borderTop - b.borderBottom - b.paddingTop - b.paddingBottom);
                out.put((org.duckdns.hjow.swingwebpanel.dom.Element) b.node, new double[] {w, h});
            }
            stack.addAll(b.children);
        }
        return out;
    }

    /**
     * Does the layout on the current thread.
     * <p>
     * 현재 스레드에서 레이아웃을 한다.
     *
     * @param doc      the document / 문서
     * @param styles   styles / 스타일
     * @param replaced replaced content or null / 치환 콘텐츠 또는 null
     * @param vw       viewport width / 뷰포트 너비
     * @param vh       viewport height / 뷰포트 높이
     * @return the result / 결과
     */
    private static LayoutResult doLayout(Document doc, StyleSource styles, ReplacedResolver replaced, double vw, double vh) {
        BoxBuilder builder = new BoxBuilder(styles, replaced);
        LayoutBox root = builder.build(doc);
        BlockLayouter bl = new BlockLayouter(new TextMeasure());
        bl.viewportW = vw;
        bl.viewportH = vh;
        bl.positionTry = styles instanceof PositionTrySource ? (PositionTrySource) styles : null;
        bl.lineQuirks =doc.getCompatibilityMode() != Document.NO_QUIRKS_MODE;
        bl.quirksMode = doc.getCompatibilityMode() == Document.QUIRKS_MODE;
        // 호환 모드에서는 html 과 body 가 뷰포트를 채운다(호환 모드 표준 3.6, 3.7): 배치 전에 표시해 최소 블록 크기로 반영한다.
        if (doc.getCompatibilityMode() == Document.QUIRKS_MODE) {
            markQuirkFill(doc, root);
        }
        bl.layoutBlockLevel(root, vw, vh);
        root.lx = root.marginLeft;
        root.ly = root.marginTop;
        if (root.frame == Css.FRAME_VRL && root.orthRoot && root.width + root.marginLeft < vw) {
            // 세로쓰기(vertical-rl) 루트 요소는 첫 줄이 오른쪽에서 시작하므로 뷰포트보다 좁으면 오른쪽 가장자리에 붙인다.
            root.lx = vw - root.width;
        }
        // 포함 블록이 초기 포함 블록(뷰포트)인 절대 배치 상자는 루트의 위치가 정해진 뒤에 배치한다.
        bl.absolute.layoutInitial(builder.getInitialAbsolute(), vw, vh);
        assign(root, 0, 0, vw);
        applyTransforms(root);
        applySticky(root, vw, vh);
        LayoutResult laid = new LayoutResult(root, vw, vh);
        laid.setStyles(styles);
        return laid;
    }

    /**
     * Marks the boxes of the "html element fills the viewport" and "body element fills the html element" quirks of quirks mode (Quirks
     * Mode Standard 3.6 and 3.7, https://quirks.spec.whatwg.org/, retrieved 2026-11-17): the <code>html</code> element gets
     * {@link LayoutBox#quirkFill} 1 and the block box of the <code>body</code> element (not positioned absolutely or fixed, not
     * floated, block-level) gets 2. The layout raises the minimum block size of such a box if its block size is <code>auto</code>
     * (see {@link BlockLayouter#minHeight}), in the horizontal and in the vertical writing modes, before the content is laid out, so the
     * descendants that depend on the filled size see it.
     * <p>
     * 호환 모드의 "html 요소가 뷰포트를 채운다"와 "body 요소가 html 요소를 채운다" quirk(호환 모드 표준 3.6, 3.7)의 상자를 표시한다: <code>html</code> 요소는
     * {@link LayoutBox#quirkFill} 1, <code>body</code> 요소의 블록 상자(절대/고정 배치도 float 도 아니고 블록 수준)는 2 를 받는다. 레이아웃은 블록 크기가
     * <code>auto</code> 인 그런 상자의 최소 블록 크기를({@link BlockLayouter#minHeight} 참조) 내용을 배치하기 전에 가로쓰기와 세로쓰기에서 올리므로 채운 크기에 의존하는
     * 자손도 그 크기를 본다.
     *
     * @param doc  the document / 문서
     * @param root the root box / 루트 상자
     */
    private static void markQuirkFill(Document doc, LayoutBox root) {
        if (root.node != doc.getDocumentElement() || root.style == null || !"html".equals(doc.getDocumentElement().getLocalName())) {
            return;
        }
        root.quirkFill = 1;
        // 루트의 자식 중 body 요소의 블록 상자를 찾는다.
        for (int i = 0; i < root.children.size(); i++) {
            LayoutBox c = root.children.get(i);
            if (c.type == LayoutBox.Type.BLOCK && c.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && "body".equals(((org.duckdns.hjow.swingwebpanel.dom.Element) c.node).getLocalName())) {
                if (c.style != null && !c.isAbsolutelyPositioned() && !c.isFixedPosition() && !c.isFloat() && !c.inlineLevel) {
                    c.quirkFill = 2;
                }
                return;
            }
        }
    }

    /**
     * Gives every transformed box its 2D transform (P10-06, CSS Transforms Level 1): the <code>transform</code> of its style around the
     * <code>transform-origin</code> in document coordinates. A transform changes neither the layout nor the position of the other boxes,
     * so it is computed from the final geometry after {@link #assign}. Only the boxes that CSS lets transform have one: blocks, inline
     * blocks, replaced boxes, tables and table cells, rows, row groups and captions of an element (not text, lines, inline elements and
     * anonymous boxes).
     * <p>
     * 변환된 모든 상자에 2D 변환을 준다(P10-06, CSS Transforms Level 1): 스타일의 <code>transform</code> 을 <code>transform-origin</code> 둘레에 적용한 문서 좌표의 변환.
     * 변환은 배치도 다른 상자의 위치도 바꾸지 않으므로 {@link #assign} 뒤의 최종 기하에서 계산한다. CSS 가 변환을 허용하는 상자만 가진다: 요소의 블록, inline-block, 치환 상자,
     * 표와 셀, 행, 행 그룹, caption(텍스트, 줄, 인라인 요소, 익명 상자는 제외).
     *
     * @param root the root box / 루트 상자
     */
    private static void applyTransforms(LayoutBox root) {
        java.util.ArrayList<LayoutBox> stack = new java.util.ArrayList<LayoutBox>();
        stack.add(root);
        // 명시적 스택으로 모든 상자를 훑는다(깊이 제한이 없다).
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            LayoutBox.Type t = b.type;
            boolean transformable = b.node != null && (t == LayoutBox.Type.BLOCK || t == LayoutBox.Type.INLINE_BLOCK || t == LayoutBox.Type.REPLACED
                    || t == LayoutBox.Type.TABLE || t == LayoutBox.Type.TABLE_CELL || t == LayoutBox.Type.TABLE_ROW
                    || t == LayoutBox.Type.TABLE_ROW_GROUP || t == LayoutBox.Type.TABLE_CAPTION);
            if (transformable && Css.hasTransform(b.style)) {
                // 오프셋 경로(CSS Motion Path 1)가 있는 상자는 오프셋 변환이 transform 과 함께 변환 순서에 들어간다.
                b.transform = MotionPath.has(b.style) ? MotionPath.boxTransform(b) : org.duckdns.hjow.swingwebpanel.css.anim.Transforms.boxTransform(b.style, b.x, b.y, b.width, b.height);
            }
            stack.addAll(b.children);
        }
    }

    /**
     * Gives every <code>position:sticky</code> box that can be shifted its {@link StickyConstraint} (CSS Positioned Layout 3 section 3.4):
     * the margin box, the content box of its containing block and the insets in px, from the final geometry after {@link #assign}. Percentage
     * insets refer to the viewport (the scrollport). The layout is not changed; the renderer shifts the painting and the hit test.
     * <p>
     * 옮길 수 있는 모든 <code>position:sticky</code> 상자에 {@link StickyConstraint} 를 준다(CSS Positioned Layout 3 3.4 절): {@link #assign} 뒤의 최종 기하에서
     * 마진 상자, 포함 블록의 콘텐츠 상자, px 오프셋을 계산한다. 백분율 오프셋은 뷰포트(스크롤포트)를 기준으로 한다. 레이아웃은 바뀌지 않으며 렌더러가 그리기와 히트 테스트를 옮긴다.
     *
     * @param root the root box / 루트 상자
     * @param vw   viewport width / 뷰포트 너비
     * @param vh   viewport height / 뷰포트 높이
     */
    private static void applySticky(LayoutBox root, double vw, double vh) {
        java.util.ArrayList<LayoutBox> stack = new java.util.ArrayList<LayoutBox>();
        stack.add(root);
        // 명시적 스택으로 모든 상자를 훑으며 sticky 상자의 제약을 만든다(깊이 제한이 없다).
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            if (b.posKind == LayoutBox.POS_STICKY) {
                b.sticky = StickyConstraint.of(b, vw, vh);
            }
            stack.addAll(b.children);
        }
    }

    /**
     * Returns the offset of <code>position:relative</code> of a style: <code>left</code> wins over <code>right</code> and
     * <code>top</code> over <code>bottom</code> (CSS 2.1 9.4.3, left-to-right); percentages of <code>left</code>/<code>right</code> refer to the
     * width of the containing block, those of <code>top</code>/<code>bottom</code> are taken as <code>auto</code> because the height of the
     * containing block is not known here.
     * <p>
     * 스타일의 <code>position:relative</code> 오프셋을 반환한다: <code>left</code> 가 <code>right</code> 보다, <code>top</code> 이 <code>bottom</code> 보다
     * 우선한다(CSS 2.1 9.4.3, 왼쪽에서 오른쪽). <code>left</code>/<code>right</code> 의 백분율은 포함 블록의 너비를 참조하고 <code>top</code>/<code>bottom</code> 의
     * 백분율은 여기서 포함 블록의 높이를 모르므로 <code>auto</code> 로 본다.
     *
     * @param s  the style / 스타일
     * @param pw width of the containing block / 포함 블록의 너비
     * @return {dx, dy} / {dx, dy}
     */
    private static double[] relativeOffset(org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle s, double pw) {
        double l = s.resolve(Prop.LEFT, pw);
        double r = s.resolve(Prop.RIGHT, pw);
        double t = s.resolve(Prop.TOP, Double.NaN);
        double bt = s.resolve(Prop.BOTTOM, Double.NaN);
        return new double[] { !Double.isNaN(l) ? l : (!Double.isNaN(r) ? -r : 0), !Double.isNaN(t) ? t : (!Double.isNaN(bt) ? -bt : 0) };
    }

    /**
     * Converts the parent-relative coordinates of a box tree to document coordinates (explicit stack, so any depth works), applies
     * <code>position:relative</code> offsets to block-level and atomic boxes and turns local baselines into absolute ones.
     * <p>
     * 상자 트리의 부모 기준 좌표를 문서 좌표로 바꾼다(명시적 스택이므로 깊이에 상관없다). 블록 수준 상자와 원자적 상자에는
     * <code>position:relative</code> 오프셋을 적용하고 지역 기준선을 절대 기준선으로 바꾼다.
     *
     * @param root  the root / 루트
     * @param ox    x of the origin / 원점 x
     * @param oy    y of the origin / 원점 y
     * @param rootW width of the containing block of the root / 루트의 포함 블록 너비
     */
    private static void assign(LayoutBox root, double ox, double oy, double rootW) {
        java.util.ArrayList<Object[]> stack = new java.util.ArrayList<Object[]>();
        stack.add(new Object[] { root, Double.valueOf(ox), Double.valueOf(oy), Double.valueOf(rootW) });
        // 상자를 하나씩 꺼내 부모의 절대 위치에 자기 위치를 더하고 자식을 쌓는다.
        while (!stack.isEmpty()) {
            Object[] e = stack.remove(stack.size() - 1);
            LayoutBox b = (LayoutBox) e[0];
            double px = ((Double) e[1]).doubleValue();
            double py = ((Double) e[2]).doubleValue();
            double pw = ((Double) e[3]).doubleValue();
            double dx = 0;
            double dy = 0;
            boolean movable = b.type == LayoutBox.Type.BLOCK || b.type == LayoutBox.Type.INLINE_BLOCK || b.type == LayoutBox.Type.REPLACED
                    || b.type == LayoutBox.Type.TABLE || b.type == LayoutBox.Type.TABLE_CELL;
            if (movable && b.style != null && "relative".equals(b.style.position())) {
                double[] o = relativeOffset(b.style, pw);
                dx = o[0];
                dy = o[1];
            }
            // 인라인 요소가 상대 배치이면 그 안의 텍스트와 상자, 그 요소의 조각이 함께 옮겨진다.
            for (LayoutBox o = b.inlineOwner; o != null; o = o.inlineParent) {
                if (o.style != null && "relative".equals(o.style.position())) {
                    double[] r = relativeOffset(o.style, pw);
                    dx += r[0];
                    dy += r[1];
                }
            }
            b.relativeX = dx;
            b.relativeY = dy;
            b.x = px + b.lx + dx;
            b.y = py + b.ly + dy;
            if (!Double.isNaN(b.baseline)) {
                b.baseline = b.y + b.baseline;
            }
            // 자식은 거꾸로 쌓아 문서 순서로 처리한다.
            for (int i = b.children.size() - 1; i >= 0; i--) {
                stack.add(new Object[] { b.children.get(i), Double.valueOf(b.x), Double.valueOf(b.y), Double.valueOf(b.width) });
            }
        }
    }
}
