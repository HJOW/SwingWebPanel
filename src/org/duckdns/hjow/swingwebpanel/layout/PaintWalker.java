package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * Walks the box tree in the painting order of CSS 2.1 Appendix E (plan item P10-01) and reports what is to be painted to a
 * {@link Visitor}. The renderer builds its display list from this walk and the hit test uses the same walk in reverse, so what is on top
 * on the screen is also what a click finds.
 * <p>
 * A <b>stacking context</b> (the root, a positioned box with an integer <code>z-index</code>, a fixed box, a box with
 * <code>opacity</code> below 1, a box with a transform; see {@link LayoutBox#isStackingContext()}) paints in this order: its own
 * background and border, the stacking contexts of its descendants with a negative <code>z-index</code> (in order of <code>z-index</code>,
 * then tree order), the backgrounds and borders of the in-flow block-level descendants (in tree order), the floats (each as a whole, in
 * tree order), the inline content of the in-flow descendants (lines, text, list markers, replaced content, inline blocks as a whole), the
 * positioned descendants with <code>z-index</code> auto or 0 and the contexts with 0 (in tree order) and last the contexts with a
 * positive <code>z-index</code>. A float, an inline block and a positioned box with <code>z-index:auto</code> is painted <i>as a whole</i>
 * as if it formed a stacking context, except that its positioned descendants and the contexts inside it belong to the enclosing
 * stacking context, which paints them in its own order. <code>overflow</code> other than visible clips the descendants whose containing
 * block chain contains the box (an absolutely positioned box is not clipped by an ancestor that is not its containing block);
 * <code>clip: rect()</code> clips an absolutely positioned box itself; <code>position:fixed</code> boxes are reported inside a fixed group
 * so that the renderer can keep them in place when the page scrolls.
 * <p>
 * 상자 트리를 CSS 2.1 부록 E 의 그리기 순서로 훑으며 그릴 것을 {@link Visitor} 에 알린다(계획 P10-01). 렌더러는 이 순회로 디스플레이 리스트를 만들고
 * 히트 테스트는 같은 순회를 거꾸로 쓰므로 화면에서 맨 위에 있는 것을 클릭도 찾는다.
 * <p>
 * <b>쌓임 맥락</b>(루트, 정수 <code>z-index</code> 를 가진 위치 지정 상자, fixed 상자, <code>opacity</code> 가 1 보다 작은 상자, transform 이 있는 상자.
 * {@link LayoutBox#isStackingContext()} 참조)은 이 순서로 그린다: 자신의 배경과 border, <code>z-index</code> 가 음수인 자손 맥락(<code>z-index</code>, 그다음 트리
 * 순서), 정상 흐름 블록 수준 자손의 배경과 border(트리 순서), float(각각 통째로, 트리 순서), 정상 흐름 자손의 인라인 콘텐츠(줄, 텍스트, 목록 표식, 치환 콘텐츠,
 * inline-block 은 통째로), <code>z-index</code> 가 auto 이거나 0 인 위치 지정 자손과 0 인 맥락(트리 순서), 마지막으로 <code>z-index</code> 가 양수인 맥락.
 * float, inline-block, <code>z-index:auto</code> 인 위치 지정 상자는 쌓임 맥락을 만드는 것처럼 <i>통째로</i> 그리되 그 안의 위치 지정 자손과 맥락은 둘러싼 쌓임
 * 맥락에 속하며 그 맥락이 자기 순서로 그린다. <code>overflow</code> 가 visible 이 아니면 포함 블록 사슬에 그 상자가 있는 자손을 자른다(절대 배치 상자는 자신의
 * 포함 블록이 아닌 조상에게는 잘리지 않는다). <code>clip: rect()</code> 는 절대 배치 상자 자신을 자른다. <code>position:fixed</code> 상자는 fixed 그룹 안에서
 * 알려 주므로 렌더러가 페이지가 스크롤되어도 제자리에 둘 수 있다.
 */
public final class PaintWalker {

    /**
     * Receives the painting events of {@link PaintWalker}. Every box that has a border box is reported once with {@link #decorate}
     * (including line boxes and anonymous boxes, which the renderer ignores but the hit test records); the text, markers and replaced
     * content follow with {@link #content}.
     * <p>
     * {@link PaintWalker} 의 그리기 이벤트를 받는다. border box 를 가진 모든 상자는 {@link #decorate} 로 한 번 알려진다(줄 상자와 익명 상자 포함. 렌더러는 무시하지만
     * 히트 테스트는 기록한다). 텍스트, 표식, 치환 콘텐츠는 {@link #content} 로 이어진다.
     */
    public interface Visitor {
        /**
         * Reports the background, border and outline of a box (the box is a block, an inline fragment, a line, an anonymous block or a
         * replaced box).
         * <p>
         * 상자의 배경, border, outline 을 알린다(상자는 블록, 인라인 조각, 줄, 익명 블록, 치환 상자 중 하나).
         *
         * @param box the box / 상자
         */
        void decorate(LayoutBox box);

        /**
         * Reports the content of a box: a text fragment, a list marker or the picture of a replaced box.
         * <p>
         * 상자의 내용을 알린다: 텍스트 조각, 목록 표식, 치환 상자의 그림.
         *
         * @param box the box / 상자
         */
        void content(LayoutBox box);

        /**
         * Starts a clip to a rectangle in document coordinates; everything until the matching {@link #endClip} is painted inside it.
         * <p>
         * 문서 좌표 사각형으로 자르기를 시작한다. 짝이 되는 {@link #endClip} 까지 그려지는 모든 것이 그 안에 그려진다.
         *
         * @param owner the box that clips / 자르는 상자
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param w     width / 너비
         * @param h     height / 높이
         */
        void beginClip(LayoutBox owner, double x, double y, double w, double h);

        /**
         * Ends the clip started by the matching {@link #beginClip}.
         * <p>
         * 짝이 되는 {@link #beginClip} 이 시작한 자르기를 끝낸다.
         *
         * @param owner the box that clips / 자르는 상자
         */
        void endClip(LayoutBox owner);

        /**
         * Starts a group that is painted with an opacity below 1.
         * <p>
         * 1 보다 작은 불투명도로 그리는 그룹을 시작한다.
         *
         * @param box     the stacking context box / 쌓임 맥락 상자
         * @param opacity the opacity between 0 and 1 / 0~1 의 불투명도
         */
        void beginOpacity(LayoutBox box, double opacity);

        /**
         * Ends the group started by {@link #beginOpacity}.
         * <p>
         * {@link #beginOpacity} 가 시작한 그룹을 끝낸다.
         *
         * @param box the stacking context box / 쌓임 맥락 상자
         */
        void endOpacity(LayoutBox box);

        /**
         * Starts a group of a fixed box: its coordinates are those at scroll offset 0 and stay on the same place of the viewport.
         * <p>
         * fixed 상자의 그룹을 시작한다. 좌표는 스크롤 오프셋 0 일 때의 것이며 뷰포트의 같은 자리에 머문다.
         *
         * @param box the fixed box / fixed 상자
         */
        void beginFixed(LayoutBox box);

        /**
         * Ends the group started by {@link #beginFixed}.
         * <p>
         * {@link #beginFixed} 가 시작한 그룹을 끝낸다.
         *
         * @param box the fixed box / fixed 상자
         */
        void endFixed(LayoutBox box);

        /**
         * Starts the group of a sticky box (see {@link StickyConstraint}): everything until the matching {@link #endSticky} is painted, and
         * hit, shifted by the amount that the constraint gives for the viewport while the page is scrolled.
         * <p>
         * sticky 상자의 그룹을 시작한다({@link StickyConstraint} 참조): 짝이 되는 {@link #endSticky} 까지의 모든 것은 페이지가 스크롤되는 동안 제약이 뷰포트에 대해 정하는
         * 만큼 옮겨져 그려지고 히트된다.
         *
         * @param box        the sticky box / sticky 상자
         * @param constraint its constraint / 제약
         */
        void beginSticky(LayoutBox box, StickyConstraint constraint);

        /**
         * Ends the group started by {@link #beginSticky}.
         * <p>
         * {@link #beginSticky} 가 시작한 그룹을 끝낸다.
         *
         * @param box the sticky box / sticky 상자
         */
        void endSticky(LayoutBox box);

        /**
         * Starts a group that is painted with the 2D transform of a box (P10-06): everything until the matching {@link #endTransform} is
         * painted in the local (laid-out) coordinates of the box and mapped by the transform.
         * <p>
         * 상자의 2D 변환을 적용해 그리는 그룹을 시작한다(P10-06): 짝이 되는 {@link #endTransform} 까지의 모든 것은 상자의 지역(배치된) 좌표로 그려지고 변환으로 옮겨진다.
         *
         * @param box       the transformed stacking context box / 변환된 쌓임 맥락 상자
         * @param transform the transform of the box in the coordinates of its parent / 부모 좌표로 나타낸 상자의 변환
         */
        void beginTransform(LayoutBox box, java.awt.geom.AffineTransform transform);

        /**
         * Ends the group started by {@link #beginTransform}.
         * <p>
         * {@link #beginTransform} 이 시작한 그룹을 끝낸다.
         *
         * @param box the transformed stacking context box / 변환된 쌓임 맥락 상자
         */
        void endTransform(LayoutBox box);

        /**
         * Starts a group that is clipped to the shape of <code>clip-path</code> (P10-08): everything until the matching
         * {@link #endClipPath} is painted and hit only inside the shape.
         * <p>
         * <code>clip-path</code> 의 모양으로 자르는 그룹을 시작한다(P10-08): 짝이 되는 {@link #endClipPath} 까지의 모든 것은 모양 안에서만 그려지고 히트된다.
         *
         * @param box   the box with the clip path / clip-path 를 가진 상자
         * @param shape the shape in the coordinates of the box (document coordinates before the transform of the box) / 상자의 좌표로 나타낸 모양(상자의 변환 전 문서 좌표)
         */
        void beginClipPath(LayoutBox box, java.awt.Shape shape);

        /**
         * Ends the group started by {@link #beginClipPath}.
         * <p>
         * {@link #beginClipPath} 가 시작한 그룹을 끝낸다.
         *
         * @param box the box with the clip path / clip-path 를 가진 상자
         */
        void endClipPath(LayoutBox box);

        /**
         * Starts a group whose content is painted into a buffer that gets the effects of the box applied before it is drawn: the filter
         * functions of <code>filter</code> and then the mask of <code>mask-image</code> (CSS Filter Effects 1 and CSS Masking 1; the
         * order of the compositing model is filter, clip, mask, opacity). Everything until the matching {@link #endEffects} belongs to the
         * group.
         * <p>
         * 상자의 효과를 입히는 버퍼에 내용을 그리는 그룹을 시작한다: <code>filter</code> 의 필터 함수, 그다음 <code>mask-image</code> 의 마스크(CSS Filter Effects 1,
         * CSS Masking 1. 합성 모델의 순서는 필터, 자르기, 마스크, 불투명도이다). 짝이 되는 {@link #endEffects} 까지의 모든 것이 그룹에 속한다.
         *
         * @param box the box with a filter or a mask / 필터나 마스크가 있는 상자
         */
        void beginEffects(LayoutBox box);

        /**
         * Ends the group started by {@link #beginEffects}.
         * <p>
         * {@link #beginEffects} 가 시작한 그룹을 끝낸다.
         *
         * @param box the box with a filter or a mask / 필터나 마스크가 있는 상자
         */
        void endEffects(LayoutBox box);

        /**
         * Reports the resize handle of a box with <code>resize</code> (see {@link Resizing}); it is painted above the content of the box, in
         * the corner of its padding box, after the clip of the padding box ended.
         * <p>
         * <code>resize</code> 가 있는 상자의 크기 조절 핸들을 알린다({@link Resizing} 참조). 상자 내용 위의 padding box 모서리에, padding box 자르기가 끝난 뒤에 그린다.
         *
         * @param box the resizable box / 크기를 바꿀 수 있는 상자
         */
        void resizer(LayoutBox box);

        /**
         * Reports the collapsed borders of a table (<code>border-collapse:collapse</code>), painted after the backgrounds of the
         * cells and above them (CSS 2.1 17.6.2); see {@link LayoutBox#getCollapsedBorders()}.
         * <p>
         * 표의 합쳐진 border(<code>border-collapse:collapse</code>)를 알린다. 셀 배경 뒤에 그 위로 그린다(CSS 2.1 17.6.2).
         * {@link LayoutBox#getCollapsedBorders()} 참조.
         *
         * @param table the table box / 표 상자
         */
        void tableBorders(LayoutBox table);

        /**
         * Reports the column rules of a multi-column container (see {@link LayoutBox#getColumnRules()}), painted right after the
         * background and the border of the container and below its content.
         * <p>
         * 다단 컨테이너의 열 구분선을 알린다({@link LayoutBox#getColumnRules()} 참조). 컨테이너의 배경과 border 바로 뒤에, 내용 아래에 그린다.
         *
         * @param container the multi-column container / 다단 컨테이너
         */
        void columnRules(LayoutBox container);
    }

    /**
     * A box that is painted in the z-order of a stacking context together with the clips of the ancestors that clip it.
     * <p>
     * 쌓임 맥락의 z 순서로 그려지는 상자와 그것을 자르는 조상.
     */
    private static final class Item {
        /** The box. 상자. */
        final LayoutBox box;
        /** The boxes that clip it, innermost first. 그것을 자르는 상자(안쪽이 먼저). */
        final List<LayoutBox> clips;

        /**
         * Creates an item.
         * <p>
         * 항목을 만든다.
         *
         * @param box   the box / 상자
         * @param clips the clipping boxes / 자르는 상자
         */
        Item(LayoutBox box, List<LayoutBox> clips) {
            this.box = box;
            this.clips = clips;
        }

        /**
         * Returns the sort key: the z-index of a context, 0 for the others.
         * <p>
         * 정렬 키를 반환한다: 맥락은 z-index, 나머지는 0.
         *
         * @return the z-index / z-index
         */
        int z() {
            // 최상위 계층의 상자는 가장 높은 z-index 의 맥락보다도 위에 그린다(CSS Positioned Layout 4 3 절).
            if (box.topLayer) {
                return Integer.MAX_VALUE;
            }
            return box.stackingContext && box.hasZ ? box.zIndex : 0;
        }
    }

    /** Orders items by z-index, then by tree order. z-index 순서, 그다음 트리 순서로 정렬한다. */
    private static final Comparator<Item> Z_ORDER = new Comparator<Item>() {
        /**
         * Compares two items.
         * <p>
         * 항목 둘을 비교한다.
         *
         * @param a first / 첫째
         * @param b second / 둘째
         * @return the order / 순서
         */
        public int compare(Item a, Item b) {
            int za = a.z();
            int zb = b.z();
            if (za != zb) {
                return za < zb ? -1 : 1;
            }
            return a.box.order < b.box.order ? -1 : (a.box.order == b.box.order ? 0 : 1);
        }
    };

    /** The receiver of the events. 이벤트를 받는 쪽. */
    private final Visitor v;

    /**
     * The stacking contexts that are painted as a group (a buffer of their own) because a box inside them has a blend mode: a
     * stacking context isolates what is inside it, so the blend sees only the content of that context (CSS Compositing 1 section 3).
     * <p>
     * 안쪽 상자에 혼합 모드가 있어서 그룹(자신의 버퍼)으로 그리는 쌓임 맥락: 쌓임 맥락은 그 안을 격리하므로 혼합은 그 맥락의 내용만 본다(CSS Compositing 1 3 절).
     */
    private final java.util.Set<LayoutBox> isolated = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<LayoutBox, Boolean>());

    /**
     * Creates a walker.
     * <p>
     * 순회기를 만든다.
     *
     * @param v the visitor / 방문자
     */
    private PaintWalker(Visitor v) {
        this.v = v;
    }

    /**
     * Walks a box tree in painting order.
     * <p>
     * 상자 트리를 그리기 순서로 훑는다.
     *
     * @param root the root box / 루트 상자
     * @param v    the visitor / 방문자
     */
    public static void walk(LayoutBox root, Visitor v) {
        if (root == null) {
            return;
        }
        PaintWalker w = new PaintWalker(v);
        w.markIsolation(root);
        w.context(root);
    }

    /**
     * Walks a box and its descendants in painting order as the image of <code>element()</code> (CSS Images 4 section 2.7): the box is painted as a stacking
     * context, and its own transform, fixed positioning and sticky offset are ignored (the transforms of its ancestors are not walked at all).
     * <p>
     * 상자와 자손을 <code>element()</code> 의 이미지로서 그리기 순서로 훑는다(CSS Images 4 2.7 절): 상자를 쌓임 맥락으로 그리며, 상자 자신의 변환, 고정 배치, sticky 오프셋은
     * 무시한다(조상의 변환은 아예 훑지 않는다).
     *
     * @param box the box / 상자
     * @param v   the visitor / 방문자
     */
    public static void walkAsImage(LayoutBox box, Visitor v) {
        if (box == null) {
            return;
        }
        PaintWalker w = new PaintWalker(v);
        w.imageRoot = box;
        w.markIsolation(box);
        w.context(box);
    }

    /**
     * The box that is walked as the image of <code>element()</code> (its own placement is ignored), or null.
     * <p>
     * <code>element()</code> 의 이미지로서 훑는 상자(자신의 배치는 무시한다). 없으면 null.
     */
    private LayoutBox imageRoot;

    /**
     * Finds the stacking contexts that have to be painted as a group because a box below them blends (<code>mix-blend-mode</code>):
     * the nearest stacking context above such a box, unless it is the root (the root blends with the canvas that is painted before).
     * An explicit stack walks the tree, so the depth does not matter.
     * <p>
     * 아래에 혼합하는 상자(<code>mix-blend-mode</code>)가 있어서 그룹으로 그려야 하는 쌓임 맥락을 찾는다: 그런 상자 위의 가장 가까운 쌓임 맥락(루트는 제외. 루트는 먼저
     * 그려진 캔버스와 혼합한다). 명시적 스택으로 트리를 훑으므로 깊이는 상관없다.
     *
     * @param root the root box / 루트 상자
     */
    private void markIsolation(LayoutBox root) {
        java.util.ArrayList<LayoutBox[]> stack = new java.util.ArrayList<LayoutBox[]>();
        stack.add(new LayoutBox[] { root, null });
        // 상자를 하나씩 꺼내며 가장 가까운 조상 쌓임 맥락과 함께 자식을 쌓는다.
        while (!stack.isEmpty()) {
            LayoutBox[] e = stack.remove(stack.size() - 1);
            LayoutBox b = e[0];
            LayoutBox anc = e[1];
            if (anc != null && Effects.blends(b.style)) {
                isolated.add(anc);
            }
            // 루트의 자식에게는 조상 맥락이 없다(루트는 격리하지 않는다). 그 밖에는 상자 자신이 맥락이면 그것이 조상이다.
            LayoutBox next = b == root ? null : (b.stackingContext ? b : anc);
            for (int i = 0; i < b.children.size(); i++) {
                stack.add(new LayoutBox[] { b.children.get(i), next });
            }
        }
    }

    /**
     * Returns the opacity of a box.
     * <p>
     * 상자의 불투명도를 반환한다.
     *
     * @param b the box / 상자
     * @return opacity between 0 and 1 / 0~1 의 불투명도
     */
    public static double opacityOf(LayoutBox b) {
        if (b.style == null) {
            return 1;
        }
        double o = b.style.number(Prop.OPACITY);
        return Double.isNaN(o) ? 1 : Math.max(0, Math.min(1, o));
    }

    /**
     * Tells whether a box clips its content to its padding box.
     * <p>
     * 상자가 내용을 padding box 로 자르는지 알려 준다.
     *
     * @param b the box / 상자
     * @return true if it clips / 자르면 true
     */
    private static boolean clips(LayoutBox b) {
        return b.clipsContent && b.type != LayoutBox.Type.REPLACED;
    }

    /**
     * Starts the clip of a box to its padding box.
     * <p>
     * 상자의 padding box 로 자르기를 시작한다.
     *
     * @param b the box / 상자
     */
    private void beginPadding(LayoutBox b) {
        v.beginClip(b, b.x + b.borderLeft, b.y + b.borderTop, Math.max(0, b.width - b.borderLeft - b.borderRight),
                Math.max(0, b.height - b.borderTop - b.borderBottom));
    }

    /**
     * Starts the clip of <code>clip: rect()</code> of a box, if it has one.
     * <p>
     * 상자에 <code>clip: rect()</code> 가 있으면 그 자르기를 시작한다.
     *
     * @param b the box / 상자
     * @return true if a clip was started / 자르기를 시작했으면 true
     */
    private boolean beginRect(LayoutBox b) {
        double[] r = b.clipRect == null ? null : b.getClipRectangle();
        if (r == null) {
            return false;
        }
        v.beginClip(b, r[0], r[1], r[2], r[3]);
        return true;
    }

    /**
     * Tells whether a box takes part in the normal flow as a block-level box that is painted by its parent's passes: a block, an
     * anonymous block or a block-level replaced box that is neither a float, nor positioned, nor a stacking context.
     * <p>
     * 상자가 부모의 단계에서 그려지는 정상 흐름의 블록 수준 상자인지 알려 준다: float 도, 위치 지정도, 쌓임 맥락도 아닌 블록, 익명 블록, 블록 수준 치환 상자.
     *
     * @param c the box / 상자
     * @return true for in-flow blocks / 정상 흐름 블록이면 true
     */
    private static boolean inFlowBlock(LayoutBox c) {
        LayoutBox.Type t = c.type;
        boolean tablePart = t == LayoutBox.Type.TABLE_CAPTION || t == LayoutBox.Type.TABLE_ROW_GROUP || t == LayoutBox.Type.TABLE_ROW
                || t == LayoutBox.Type.TABLE_CELL || t == LayoutBox.Type.TABLE_COLUMN_GROUP || t == LayoutBox.Type.TABLE_COLUMN
                || (t == LayoutBox.Type.TABLE && !c.inlineLevel);
        if (t != LayoutBox.Type.BLOCK && t != LayoutBox.Type.ANONYMOUS_BLOCK && !tablePart && !(t == LayoutBox.Type.REPLACED && !c.inlineLevel)) {
            return false;
        }
        return c.floatSide == LayoutBox.FLOAT_NONE && c.posKind == LayoutBox.POS_STATIC && !c.stackingContext && !c.flexItem && !c.gridItem;
    }

    /**
     * Paints a stacking context.
     * <p>
     * 쌓임 맥락을 그린다.
     *
     * @param c the box that forms the context / 맥락을 만드는 상자
     */
    private void context(LayoutBox c) {
        // element() 의 이미지로 그리는 상자는 자신의 고정 배치, sticky, 변환을 무시한다(CSS Images 4 2.7 절).
        boolean asImage = c == imageRoot;
        boolean fixed = !asImage && c.isFixedPosition();
        double op = opacityOf(c);
        if (fixed) {
            v.beginFixed(c);
        }
        // sticky 상자는 스크롤에 따라 옮겨지는 그룹 안에서 그린다(변환은 그 안쪽에서 적용된다).
        boolean sticky = !asImage && c.sticky != null;
        if (sticky) {
            v.beginSticky(c, c.sticky);
        }
        // 변환은 상자와 그 내용 전체(불투명도 그룹, 자르기 포함)를 감싼다(CSS Transforms 1 13 절).
        boolean transformed = !asImage && c.transform != null;
        if (transformed) {
            v.beginTransform(c, c.transform);
        }
        // 불투명도가 1 보다 작거나 혼합 모드가 있거나 안쪽 상자가 혼합하는 쌓임 맥락은 자신의 버퍼에 그린 뒤 한 번 합성한다.
        boolean group = op < 1 || Effects.blends(c.style) || isolated.contains(c);
        if (group) {
            v.beginOpacity(c, op);
        }
        // clip-path 는 상자와 그 내용 전체를 모양으로 자른다(P10-08). 쓸 수 없는 값은 아무것도 자르지 않는다.
        java.awt.Shape clipPath = ClipPaths.has(c.style) ? ClipPaths.shapeOf(c) : null;
        if (clipPath != null) {
            v.beginClipPath(c, clipPath);
        }
        // 필터와 마스크는 clip-path 안쪽에서 적용한다(합성 모델의 순서: 필터, 자르기, 마스크, 불투명도).
        boolean effects = FilterSpec.has(c.style) || Effects.hasMask(c.style) || FilterSpec.hasBackdrop(c.style) || ClipPaths.reference(c.style) != null;
        if (effects) {
            v.beginEffects(c);
        }
        boolean rect = beginRect(c);
        decor(c);
        boolean clip = clips(c);
        if (clip) {
            beginPadding(c);
        }
        List<Item> items = new ArrayList<Item>();
        collect(c, c, items);
        List<Item> neg = new ArrayList<Item>();
        List<Item> zero = new ArrayList<Item>();
        List<Item> pos = new ArrayList<Item>();
        // 항목을 z-index 가 음수, 0 이나 auto, 양수로 나눈다.
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            int z = it.z();
            (z < 0 ? neg : (z > 0 ? pos : zero)).add(it);
        }
        Collections.sort(neg, Z_ORDER);
        Collections.sort(zero, Z_ORDER);
        Collections.sort(pos, Z_ORDER);
        paintItems(neg);
        body(c);
        paintItems(zero);
        paintItems(pos);
        if (clip) {
            v.endClip(c);
            reportResizer(c);
        }
        if (rect) {
            v.endClip(c);
        }
        if (effects) {
            v.endEffects(c);
        }
        if (clipPath != null) {
            v.endClipPath(c);
        }
        if (group) {
            v.endOpacity(c);
        }
        if (transformed) {
            v.endTransform(c);
        }
        if (sticky) {
            v.endSticky(c);
        }
        if (fixed) {
            v.endFixed(c);
        }
    }

    /**
     * Reports the decoration of a block-level box (background, border, outline) and, for a multi-column container, its column rules.
     * <p>
     * 블록 수준 상자의 장식(배경, border, outline)을 알리고 다단 컨테이너이면 열 구분선도 알린다.
     *
     * @param b the box / 상자
     */
    private void decor(LayoutBox b) {
        v.decorate(b);
        if (b.columnRules != null) {
            v.columnRules(b);
        }
    }

    /**
     * Paints the in-flow content of a box: block backgrounds, floats and inline content.
     * <p>
     * 상자의 정상 흐름 콘텐츠를 그린다: 블록 배경, float, 인라인 콘텐츠.
     *
     * @param c the box / 상자
     */
    private void body(LayoutBox c) {
        if (c.type == LayoutBox.Type.REPLACED) {
            v.content(c);
            return;
        }
        flowBackgrounds(c);
        if (c.type == LayoutBox.Type.TABLE) {
            v.tableBorders(c);
        }
        floats(c);
        inlineFlow(c);
    }

    /**
     * Paints a box as a whole as if it formed a stacking context (a float, an inline block, a positioned box with
     * <code>z-index:auto</code>): its decoration, then its in-flow content. Its positioned descendants are not painted here.
     * <p>
     * 쌓임 맥락을 만드는 것처럼 상자를 통째로 그린다(float, inline-block, <code>z-index:auto</code> 인 위치 지정 상자): 장식, 그다음 정상 흐름 콘텐츠.
     * 그 안의 위치 지정 자손은 여기서 그리지 않는다.
     *
     * @param u the box / 상자
     */
    private void atomic(LayoutBox u) {
        boolean rect = beginRect(u);
        decor(u);
        boolean clip = clips(u);
        if (clip) {
            beginPadding(u);
        }
        body(u);
        if (clip) {
            v.endClip(u);
            reportResizer(u);
        }
        if (rect) {
            v.endClip(u);
        }
    }

    /**
     * Reports the resize handle of a box that clips its content, if it has one.
     * <p>
     * 내용을 자르는 상자에 크기 조절 핸들이 있으면 알린다.
     *
     * @param b the box / 상자
     */
    private void reportResizer(LayoutBox b) {
        // 핸들은 padding box 자르기가 끝난 뒤에 내용 위에 그린다.
        if (Resizing.handle(b) != null) {
            v.resizer(b);
        }
    }

    /**
     * Paints the items of the z-order of a context.
     * <p>
     * 맥락의 z 순서 항목을 그린다.
     *
     * @param items the sorted items / 정렬된 항목
     */
    private void paintItems(List<Item> items) {
        // 항목마다 자르는 조상의 자르기(바깥쪽부터) 안에서 맥락이면 맥락으로, 아니면 통째로 그린다.
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            // 바깥쪽 자르기부터 시작한다.
            for (int k = it.clips.size() - 1; k >= 0; k--) {
                beginPadding(it.clips.get(k));
            }
            if (it.box.stackingContext) {
                context(it.box);
            } else {
                atomic(it.box);
            }
            // 안쪽 자르기부터 끝낸다.
            for (int k = 0; k < it.clips.size(); k++) {
                v.endClip(it.clips.get(k));
            }
        }
    }

    /**
     * Collects the positioned descendants and the stacking contexts below a box that belong to the context of <code>ctx</code>: every
     * positioned box and every stacking context, not looking inside a stacking context (it paints its own contents) but looking inside
     * positioned boxes that are not contexts (their positioned descendants belong to this context too).
     * <p>
     * 상자 아래에서 <code>ctx</code> 맥락에 속하는 위치 지정 자손과 쌓임 맥락을 모은다: 모든 위치 지정 상자와 쌓임 맥락. 쌓임 맥락 안은 들여다보지 않지만(스스로
     * 자기 내용을 그림) 맥락이 아닌 위치 지정 상자 안은 들여다본다(그 안의 위치 지정 자손도 이 맥락에 속함).
     *
     * @param p   the box whose descendants are visited / 자손을 훑을 상자
     * @param ctx the stacking context box / 쌓임 맥락 상자
     * @param out receives the items in tree order / 트리 순서로 항목을 받는다
     */
    private void collect(LayoutBox p, LayoutBox ctx, List<Item> out) {
        // 자식을 차례로 보며 위치 지정 상자나 맥락을 항목으로 모은다.
        for (int i = 0; i < p.children.size(); i++) {
            LayoutBox c = p.children.get(i);
            if (c.stackingContext || c.posKind != LayoutBox.POS_STATIC) {
                out.add(new Item(c, clipChain(c, ctx)));
                if (!c.stackingContext) {
                    collect(c, ctx, out);
                }
                continue;
            }
            collect(c, ctx, out);
        }
    }

    /**
     * Returns the boxes that clip a positioned box below a stacking context: the clipping boxes on the chain of containing blocks
     * (an absolutely positioned box has its containing block as the next link) between the box and the context, innermost first. The
     * context itself is not part of the chain, because its clip surrounds the whole paint of the context.
     * <p>
     * 쌓임 맥락 아래 위치 지정 상자를 자르는 상자를 반환한다: 상자와 맥락 사이의 포함 블록 사슬(절대 배치 상자는 다음 고리가 포함 블록)에서 자르는 상자들이며 안쪽이
     * 먼저다. 맥락 자신은 사슬에 넣지 않는다. 맥락의 자르기가 맥락의 모든 그리기를 감싸기 때문이다.
     *
     * @param c   the box / 상자
     * @param ctx the stacking context box / 쌓임 맥락 상자
     * @return the clipping boxes / 자르는 상자
     */
    private static List<LayoutBox> clipChain(LayoutBox c, LayoutBox ctx) {
        List<LayoutBox> out = new ArrayList<LayoutBox>(2);
        LayoutBox x = c.getContainingBlock();
        // 포함 블록 사슬을 따라 맥락까지 올라가며 자르는 상자를 모은다(트리 밖으로 나가면 멈춘다).
        for (int guard = 0; x != null && x != ctx && guard < 4096; guard++) {
            if (!isInside(x, ctx)) {
                break;
            }
            if (clips(x)) {
                out.add(x);
            }
            x = x.getContainingBlock();
        }
        return out;
    }

    /**
     * Tells whether a box is a descendant of another box.
     * <p>
     * 상자가 다른 상자의 자손인지 알려 준다.
     *
     * @param x        the box / 상자
     * @param ancestor the possible ancestor / 조상일 수 있는 상자
     * @return true if x is below ancestor / x 가 ancestor 아래이면 true
     */
    private static boolean isInside(LayoutBox x, LayoutBox ancestor) {
        // 부모를 따라 올라가며 조상을 찾는다.
        for (LayoutBox p = x.parent; p != null; p = p.parent) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Paints the backgrounds and borders of the in-flow block-level descendants in tree order (step 4 of Appendix E); a box that clips
     * clips its descendants.
     * <p>
     * 정상 흐름 블록 수준 자손의 배경과 border 를 트리 순서로 그린다(부록 E 의 4 단계). 자르는 상자는 자손을 자른다.
     *
     * @param parent the box whose children are visited / 자식을 훑을 상자
     */
    private void flowBackgrounds(LayoutBox parent) {
        // 자식 중 정상 흐름 블록을 차례로 장식하고 그 안으로 내려간다.
        for (int i = 0; i < parent.children.size(); i++) {
            LayoutBox c = parent.children.get(i);
            if (!inFlowBlock(c)) {
                continue;
            }
            decor(c);
            if (c.type == LayoutBox.Type.REPLACED) {
                continue;
            }
            boolean clip = clips(c);
            if (clip) {
                beginPadding(c);
            }
            flowBackgrounds(c);
            if (c.type == LayoutBox.Type.TABLE) {
                v.tableBorders(c);
            }
            if (clip) {
                v.endClip(c);
            }
        }
    }

    /**
     * Paints the floats among the descendants of a box in tree order (step 5), each as a whole.
     * <p>
     * 상자의 자손 중 float 를 트리 순서로 각각 통째로 그린다(5 단계).
     *
     * @param parent the box whose flow is searched / 흐름을 찾을 상자
     */
    private void floats(LayoutBox parent) {
        List<Item> found = new ArrayList<Item>();
        findFloats(parent, new ArrayList<LayoutBox>(), found);
        // 찾은 float 를 트리 순서로 그린다(각 float 는 흐름 안의 자르는 조상 안에서).
        for (int i = 0; i < found.size(); i++) {
            Item it = found.get(i);
            // 바깥쪽 자르기부터 시작한다.
            for (int k = it.clips.size() - 1; k >= 0; k--) {
                beginPadding(it.clips.get(k));
            }
            atomic(it.box);
            // 안쪽 자르기부터 끝낸다.
            for (int k = 0; k < it.clips.size(); k++) {
                v.endClip(it.clips.get(k));
            }
        }
    }

    /**
     * Finds the floats in the in-flow part of a subtree.
     * <p>
     * 하위 트리의 정상 흐름 부분에서 float 를 찾는다.
     *
     * @param p     the box / 상자
     * @param clips the clipping boxes above p, outermost first / p 위의 자르는 상자(바깥쪽이 먼저)
     * @param out   receives the floats / float 를 받는다
     */
    private void findFloats(LayoutBox p, List<LayoutBox> clips, List<Item> out) {
        // 자식 중 float 를 모으고 정상 흐름 블록 안으로 내려간다.
        for (int i = 0; i < p.children.size(); i++) {
            LayoutBox c = p.children.get(i);
            if (c.floatSide != LayoutBox.FLOAT_NONE && c.posKind == LayoutBox.POS_STATIC && !c.stackingContext
                    && (c.type == LayoutBox.Type.BLOCK || c.type == LayoutBox.Type.REPLACED || c.type == LayoutBox.Type.TABLE)) {
                List<LayoutBox> inner = new ArrayList<LayoutBox>(clips.size());
                // 가장 안쪽이 먼저 오도록 거꾸로 복사한다.
                for (int k = clips.size() - 1; k >= 0; k--) {
                    inner.add(clips.get(k));
                }
                out.add(new Item(c, inner));
            } else if (inFlowBlock(c) && c.type != LayoutBox.Type.REPLACED) {
                boolean clip = clips(c);
                if (clip) {
                    clips.add(c);
                }
                findFloats(c, clips, out);
                if (clip) {
                    clips.remove(clips.size() - 1);
                }
            }
        }
    }

    /**
     * Paints the inline content of the in-flow descendants in tree order (step 7).
     * <p>
     * 정상 흐름 자손의 인라인 콘텐츠를 트리 순서로 그린다(7 단계).
     *
     * @param parent the box whose children are visited / 자식을 훑을 상자
     */
    private void inlineFlow(LayoutBox parent) {
        // 자식을 차례로 보며 줄, 표식, 치환 콘텐츠를 그리고 정상 흐름 블록 안으로 내려간다.
        for (int i = 0; i < parent.children.size(); i++) {
            LayoutBox c = parent.children.get(i);
            LayoutBox.Type t = c.type;
            if (t == LayoutBox.Type.LINE) {
                line(c);
            } else if (t == LayoutBox.Type.MARKER) {
                v.content(c);
            } else if ((c.flexItem || c.gridItem) && c.posKind == LayoutBox.POS_STATIC && !c.stackingContext) {
                // 플렉스 항목은 inline-block 처럼 통째로 그린다(순서는 order 를 반영한 자식 순서).
                atomic(c);
            } else if (inFlowBlock(c)) {
                if (t == LayoutBox.Type.REPLACED) {
                    v.content(c);
                    continue;
                }
                boolean clip = clips(c);
                if (clip) {
                    beginPadding(c);
                }
                inlineFlow(c);
                if (clip) {
                    v.endClip(c);
                    reportResizer(c);
                }
            }
        }
    }

    /**
     * Paints one line: the inline element fragments first (backgrounds and borders), then text, markers and atomic boxes in visual order.
     * <p>
     * 줄 하나를 그린다: 먼저 인라인 요소 조각(배경과 border), 그다음 텍스트, 표식, 원자적 상자를 시각 순서로.
     *
     * @param line the line box / 줄 상자
     */
    private void line(LayoutBox line) {
        v.decorate(line);
        // 먼저 인라인 요소 조각의 배경과 border 를 그린다.
        for (int i = 0; i < line.children.size(); i++) {
            LayoutBox c = line.children.get(i);
            if (c.type == LayoutBox.Type.INLINE) {
                v.decorate(c);
            }
        }
        // 그다음 텍스트, 표식, 원자적 상자를 시각 순서로 그린다.
        for (int i = 0; i < line.children.size(); i++) {
            LayoutBox c = line.children.get(i);
            LayoutBox.Type t = c.type;
            if (t == LayoutBox.Type.TEXT || t == LayoutBox.Type.MARKER) {
                v.content(c);
            } else if ((t == LayoutBox.Type.INLINE_BLOCK || t == LayoutBox.Type.REPLACED || t == LayoutBox.Type.TABLE)
                    && c.posKind == LayoutBox.POS_STATIC
                    && !c.stackingContext) {
                atomic(c);
            }
        }
    }
}
