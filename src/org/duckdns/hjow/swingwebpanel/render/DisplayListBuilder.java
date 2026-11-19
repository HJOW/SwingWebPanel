package org.duckdns.hjow.swingwebpanel.render;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.ClipPaths;
import org.duckdns.hjow.swingwebpanel.layout.Effects;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.layout.PaintWalker;
import org.duckdns.hjow.swingwebpanel.layout.StickyConstraint;
import org.duckdns.hjow.swingwebpanel.layout.TextEmphasis;
import org.duckdns.hjow.swingwebpanel.layout.TableBorder;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgReference;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Turns a {@link LayoutResult} into a {@link DisplayList} (plan items P09-04, P09-05, P10-01). The commands follow the painting order of
 * CSS 2.1 Appendix E, which {@link PaintWalker} walks and reports to this class: first the canvas background (the background of the root
 * element, or of <code>body</code> when the root has none, covering the whole canvas), then for every stacking context its own
 * background and border, the contexts with a negative <code>z-index</code>, the backgrounds and borders of the in-flow block-level
 * boxes, the floats, the inline content (inline backgrounds and borders, text, list markers, replaced content), the positioned boxes and
 * the contexts with <code>z-index</code> 0 or auto and finally the contexts with a positive <code>z-index</code>. A float, an inline
 * block and a positioned box are painted as a whole. Groups carry what the painter must apply to a part of the list: a clip to the padding
 * box of a box with <code>overflow</code> other than visible (or to <code>clip: rect()</code>), an opacity, and the fixed group of a
 * <code>position:fixed</code> box that is not scrolled. A group with an opacity of 0 produces no commands. Images that cannot be shown get
 * a {@link DisplayCommands.Placeholder}: the unsupported URL and the alt text, the unsupported-video notice, or an audio bar. P10-04: the
 * decoration of a box is painted in the order outer shadows, background color, background layers (images and gradients,
 * {@link BackgroundLayers}), inset shadows, border; boxes with <code>border-radius</code> use rounded shapes ({@link RoundedBox}) for the
 * background, the layers, the border and the clip of <code>overflow</code>.
 * <p>
 * {@link LayoutResult} 를 {@link DisplayList} 로 만든다(계획 P09-04, P09-05, P10-01). 명령은 {@link PaintWalker} 가 훑어 이 클래스에 알려 주는 CSS 2.1 부록 E 의
 * 그리기 순서를 따른다: 먼저 캔버스 배경(루트 요소의 배경, 없으면 <code>body</code> 의 배경이 캔버스 전체를 덮음), 그다음 쌓임 맥락마다 자신의 배경과 border,
 * <code>z-index</code> 가 음수인 맥락, 정상 흐름 블록 수준 상자의 배경과 border, float, 인라인 콘텐츠(인라인 배경과 border, 텍스트, 목록 표식, 치환 콘텐츠),
 * 위치 지정 상자와 <code>z-index</code> 가 0 이거나 auto 인 맥락, 마지막으로 <code>z-index</code> 가 양수인 맥락. float, inline-block, 위치 지정 상자는 통째로
 * 그린다. 그룹은 페인터가 리스트의 일부에 적용할 것을 담는다: <code>overflow</code> 가 visible 이 아닌 상자의 padding box 로(또는 <code>clip: rect()</code> 로)
 * 자르기, 불투명도, 스크롤되지 않는 <code>position:fixed</code> 상자의 fixed 그룹. 불투명도 0 인 그룹은 명령을 만들지 않는다. 보여 줄 수 없는 이미지는
 * {@link DisplayCommands.Placeholder} 가 된다(지원하지 않는 URL과 alt, 지원하지 않는 비디오 안내, 오디오 막대). P10-04: 상자의 장식은 바깥 그림자, 배경색,
 * 배경 레이어(이미지와 그라디언트, {@link BackgroundLayers}), 안쪽 그림자, border 순서로 그리며 <code>border-radius</code> 가 있는 상자는 배경, 레이어, border,
 * <code>overflow</code> 클립에 둥근 도형({@link RoundedBox})을 쓴다.
 */
final class DisplayListBuilder implements PaintWalker.Visitor, GeneratedImages.ElementPictures {

    /**
     * The notice that is shown instead of a plug-in (<code>embed</code>, or an <code>object</code> without fallback content).
     * <p>
     * 플러그인(<code>embed</code>, 또는 대체 내용이 없는 <code>object</code>) 대신 보여 주는 안내.
     */
    static final String UNSUPPORTED_PLUGIN = "지원하지 않는 플러그인입니다 (Plug-in is not supported)";

    /**
     * The notice that is shown instead of a video.
     * <p>
     * 비디오 대신 보여 주는 안내.
     */
    static final String UNSUPPORTED_VIDEO = "지원하지 않는 비디오입니다 (Video is not supported)";

    /**
     * The prefix of the line that names a URL that cannot be used.
     * <p>
     * 사용할 수 없는 URL 을 알리는 줄의 앞부분.
     */
    static final String UNSUPPORTED_URL = "지원하지 않는 URL (Unsupported URL): ";

    /**
     * The layout to paint.
     * <p>
     * 그릴 레이아웃.
     */
    private final LayoutResult result;

    /**
     * The image states, may be null.
     * <p>
     * 이미지 상태. null 일 수 있다.
     */
    private final ImageSource images;

    /**
     * The document, for the base URL.
     * <p>
     * 기준 URL 을 위한 문서.
     */
    private final Document doc;

    /**
     * The commands built so far.
     * <p>
     * 지금까지 만든 명령.
     */
    private final List<DisplayCommand> out = new ArrayList<DisplayCommand>();

    /**
     * Marks in {@link #open} a group that was left out because the output was suppressed.
     * <p>
     * 출력이 억제되어 생략한 그룹을 {@link #open} 에서 표시한다.
     */
    private static final int SKIPPED = -2;

    /**
     * Marks in {@link #open} an opacity group of a fully transparent box (it suppresses the output).
     * <p>
     * 완전히 투명한 상자의 불투명도 그룹을 {@link #open} 에서 표시한다(출력을 억제한다).
     */
    private static final int HIDDEN = -1;

    /**
     * The start positions of the groups that are open, innermost last (or {@link #SKIPPED}/{@link #HIDDEN}).
     * <p>
     * 열려 있는 그룹의 시작 위치. 가장 안쪽이 마지막이다({@link #SKIPPED}/{@link #HIDDEN} 일 수 있음).
     */
    private final List<Integer> open = new ArrayList<Integer>();

    /**
     * Greater than 0 while the output is suppressed (inside a fully transparent box).
     * <p>
     * 출력이 억제되는 동안(완전히 투명한 상자 안) 0 보다 크다.
     */
    private int suppressed;

    /**
     * The opaque color that every text command gets while the text mask of <code>background-clip: text</code> is built (see {@link #textMask}); 0 while
     * the page text is painted.
     * <p>
     * <code>background-clip: text</code> 의 텍스트 마스크를 만드는 동안 모든 텍스트 명령이 받는 불투명한 색({@link #textMask} 참조). 페이지 텍스트를 그리는 동안은 0.
     */
    private int maskColor;

    /**
     * The box whose background has been painted on the canvas (its own background is skipped), or null.
     * <p>
     * 배경을 캔버스에 이미 그린 상자(자신의 배경은 건너뛴다). 없으면 null.
     */
    private LayoutBox canvasSource;

    /**
     * Creates a builder.
     * <p>
     * 빌더를 만든다.
     *
     * @param result the layout / 레이아웃
     * @param images image states or null / 이미지 상태 또는 null
     */
    private DisplayListBuilder(LayoutResult result, ImageSource images) {
        this(result, images, new ArrayList<Node>());
    }

    /**
     * Creates a builder that paints the image of <code>element()</code>, knowing the nodes that the pictures being built depend on.
     * <p>
     * <code>element()</code> 의 이미지를 그리는 빌더를 만든다. 만들고 있는 그림이 의존하는 노드를 안다.
     *
     * @param result the layout / 레이아웃
     * @param images image states or null / 이미지 상태 또는 null
     * @param chain  the referrers and the referenced elements of the pictures being built, outermost first / 만들고 있는 그림의 참조자와 참조된 요소(바깥쪽부터)
     */
    private DisplayListBuilder(LayoutResult result, ImageSource images, List<Node> chain) {
        this.result = result;
        this.images = images;
        Node n = result.getRoot() == null ? null : result.getRoot().getNode();
        this.doc = n == null ? null : (n instanceof Document ? (Document) n : n.getOwnerDocument());
        this.layers = new BackgroundLayers(images, doc);
        this.chain = chain;
        this.layers.setElementPictures(this);
    }

    /**
     * The referrers and the referenced elements of the <code>element()</code> pictures that this builder paints inside (empty for the page itself): a
     * reference to an ancestor or self of any of them is a cycle (CSS Images 4 section 2.7.3).
     * <p>
     * 이 빌더가 그 안을 그리고 있는 <code>element()</code> 그림의 참조자와 참조된 요소(페이지 자체이면 비어 있음): 이들 중 어느 것의 조상이나 자신을 가리키면 순환이다
     * (CSS Images 4 2.7.3 절).
     */
    private final List<Node> chain;

    /**
     * The deepest nesting of <code>element()</code> pictures (a picture of an element whose decoration uses another picture, and so on).
     * <p>
     * <code>element()</code> 그림의 최대 중첩 깊이(장식이 다른 그림을 쓰는 요소의 그림 등).
     */
    private static final int MAX_PICTURE_NESTING = 8;

    /**
     * Returns the picture of <code>element(#id)</code> (CSS Images 4 section 2.7): a rendered element is painted with its descendants as a stacking context
     * (its transform, fixed position and sticky offset ignored) over a transparent canvas whose edges are those of its decorated bounding box (the border boxes
     * of its fragments grown by the <code>border-image-outset</code>); an element that is not rendered gives the picture of its paint source (an
     * <code>img</code> with a loaded image, a <code>canvas</code>); anything else, a missing element and a reference that makes a cycle (the element is an
     * ancestor or self of the referrer or of a node whose picture is being built) are invalid images.
     * <p>
     * <code>element(#id)</code> 의 그림을 반환한다(CSS Images 4 2.7 절): 그려지는 요소는 자손과 함께 쌓임 맥락으로(자신의 변환, 고정 배치, sticky 오프셋은 무시) 장식된
     * 경계 상자(조각들의 border box 를 <code>border-image-outset</code> 만큼 늘린 것)를 가장자리로 하는 투명한 캔버스에 그린다. 그려지지 않는 요소는 그림 원본(이미지를 불러온
     * <code>img</code>, <code>canvas</code>)의 그림이다. 그 밖의 경우, 없는 요소, 순환을 만드는 참조(요소가 참조자나 그림을 만들고 있는 노드의 조상이거나 자신)는 잘못된
     * 이미지이다.
     *
     * @param id       the id / id
     * @param referrer the node whose style uses the function / 함수를 쓰는 스타일의 노드
     * @return the picture or null / 그림 또는 null
     */
    public GeneratedImages.Picture elementPicture(String id, Node referrer) {
        if (doc == null || chain.size() >= MAX_PICTURE_NESTING * 2) {
            return null;
        }
        Element target = doc.getElementById(id);
        if (target == null || isAncestorOrSelf(target, referrer)) {
            return null;
        }
        // 그리고 있는 그림의 노드 중 어느 것의 조상이거나 자신이면 순환이다.
        for (int i = 0; i < chain.size(); i++) {
            if (isAncestorOrSelf(target, chain.get(i))) {
                return null;
            }
        }
        List<LayoutBox> boxes = result.getBoxes(target);
        if (boxes == null || boxes.isEmpty()) {
            return paintSource(target);
        }
        List<Node> next = new ArrayList<Node>(chain);
        next.add(referrer);
        next.add(target);
        DisplayListBuilder sub = new DisplayListBuilder(result, images, next);
        LayoutBox principal = boxes.get(0);
        java.awt.geom.Rectangle2D bounds = null;
        // 조각마다 border box 를 border-image-outset 만큼 늘려 합친다.
        for (int i = 0; i < boxes.size(); i++) {
            LayoutBox b = boxes.get(i);
            if (b.getType() == LayoutBox.Type.MARKER) {
                continue;
            }
            double[] o = BorderImageLayer.outsetOf(b, b.getStyle(), BorderImageLayer.BORDER);
            if (o == null) {
                o = new double[4];
            }
            java.awt.geom.Rectangle2D r = new java.awt.geom.Rectangle2D.Double(b.getX() - o[3], b.getY() - o[0], b.getWidth() + o[1] + o[3], b.getHeight() + o[0] + o[2]);
            bounds = bounds == null ? r : bounds.createUnion(r);
        }
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return null;
        }
        if (principal.getType() == LayoutBox.Type.INLINE) {
            sub.inlinePicture(boxes);
        } else {
            PaintWalker.walkAsImage(principal, sub);
        }
        return new GeneratedImages.ElementPicture(sub.out, bounds);
    }

    /**
     * Paints the fragments of an inline element for its <code>element()</code> picture: the decoration of every fragment, then the text boxes of its lines that
     * belong to the element.
     * <p>
     * 인라인 요소의 <code>element()</code> 그림을 위해 조각을 그린다: 조각마다 장식, 그다음 그 줄에서 요소에 속하는 텍스트 상자.
     *
     * @param fragments the fragments of the element / 요소의 조각
     */
    private void inlinePicture(List<LayoutBox> fragments) {
        // 조각마다 장식과 같은 줄의 요소 텍스트를 그린다.
        for (int i = 0; i < fragments.size(); i++) {
            LayoutBox f = fragments.get(i);
            decorate(f);
            LayoutBox line = f.getParent();
            List<LayoutBox> siblings = line == null ? new ArrayList<LayoutBox>() : line.getChildren();
            // 같은 줄에서 이 조각의 요소에 속하는 텍스트 상자를 찾는다.
            for (int k = 0; k < siblings.size(); k++) {
                LayoutBox c = siblings.get(k);
                if (c.getType() == LayoutBox.Type.TEXT && f.isTextOfInline(c)) {
                    paintText(c);
                }
            }
        }
    }

    /**
     * Returns the picture of the paint source of an element that is not rendered (CSS Images 4 section 2.7.1): an <code>img</code> whose image has arrived, or
     * a <code>canvas</code> with a picture; null for any other element.
     * <p>
     * 그려지지 않는 요소의 그림 원본의 그림을 반환한다(CSS Images 4 2.7.1 절): 이미지가 도착한 <code>img</code>, 그림이 있는 <code>canvas</code>. 다른 요소이면 null.
     *
     * @param e the element / 요소
     * @return the picture or null / 그림 또는 null
     */
    private GeneratedImages.Picture paintSource(Element e) {
        String name = e.getLocalName();
        if ("img".equals(name)) {
            String src = e.getAttribute("src");
            if (src == null || src.length() == 0 || images == null) {
                return null;
            }
            ImageResource r = images.getImage(resolve(src));
            return r == null || r.getState() != ImageResource.State.LOADED || r.getImage() == null ? null : new GeneratedImages.RasterPicture(r.getImage(), 1, null, false);
        }
        if ("canvas".equals(name) && images instanceof CanvasSource) {
            org.duckdns.hjow.swingwebpanel.media.image.DecodedImage picture = ((CanvasSource) images).getCanvasImage(e);
            return picture == null ? null : new GeneratedImages.RasterPicture(picture, 1, null, false);
        }
        return null;
    }

    /**
     * Tells whether a node is an ancestor of another node or the node itself.
     * <p>
     * 노드가 다른 노드의 조상이거나 그 노드 자신인지 알려 준다.
     *
     * @param ancestor the possible ancestor / 조상일 수 있는 노드
     * @param node     the node, may be null / 노드. null 가능
     * @return true if so / 그러면 true
     */
    private static boolean isAncestorOrSelf(Node ancestor, Node node) {
        // 노드에서 부모를 따라 올라가며 찾는다.
        for (Node n = node; n != null; n = n.getParentNode()) {
            if (n == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * The builder of the background layers (images and gradients).
     * <p>
     * 배경 레이어(이미지와 그라디언트)를 만드는 빌더.
     */
    private final BackgroundLayers layers;

    /** The SVG references that the boxes of a document make, by document (see {@link SvgReferences}). 문서마다 상자가 만드는 SVG 참조({@link SvgReferences} 참조). */
    private final java.util.Map<Document, SvgReferences> svgReferences = new java.util.IdentityHashMap<Document, SvgReferences>();

    /**
     * Builds the display list of a layout.
     * <p>
     * 레이아웃의 디스플레이 리스트를 만든다.
     *
     * @param result the layout / 레이아웃
     * @param images image and media states, may be null (every image is pending) / 이미지와 미디어 상태. null 이면 모든 이미지가 진행 중
     * @return the display list / 디스플레이 리스트
     */
    static DisplayList build(LayoutResult result, ImageSource images) {
        DisplayListBuilder b = new DisplayListBuilder(result, images);
        b.run();
        double w = Math.max(result.getContentWidth(), result.getViewportWidth());
        double h = Math.max(result.getContentHeight(), result.getViewportHeight());
        return new DisplayList(b.out, w, h, result.getViewportWidth(), result.getViewportHeight());
    }

    /**
     * Builds all commands.
     * <p>
     * 모든 명령을 만든다.
     */
    private void run() {
        LayoutBox root = result.getRoot();
        if (root == null) {
            return;
        }
        paintCanvasBackground(root);
        PaintWalker.walk(root, this);
    }

    /**
     * Paints the background of the canvas: the root element's, or the first <code>body</code> box's when the root has no background.
     * <p>
     * 캔버스의 배경을 그린다: 루트 요소의 배경, 루트에 배경이 없으면 첫 <code>body</code> 상자의 배경.
     *
     * @param root the root box / 루트 상자
     */
    private void paintCanvasBackground(LayoutBox root) {
        LayoutBox source = null;
        if (hasBackground(root.getStyle())) {
            source = root;
        } else {
            // 루트에 배경이 없으면 body 상자를 찾아 그 배경을 캔버스에 쓴다.
            for (LayoutBox c : root.getChildren()) {
                if (c.getNode() instanceof Element && "body".equals(((Element) c.getNode()).getLocalName()) && hasBackground(c.getStyle())) {
                    source = c;
                    break;
                }
            }
        }
        if (source == null) {
            // 배경이 없어도 어두운 색 구성표의 루트이면 캔버스는 Canvas 시스템 색(어두운 값)으로 칠한다(CSS Color Adjustment 1).
            if (root.getStyle() != null && root.getStyle().usesDarkScheme()) {
                emit(new DisplayCommands.FillRect(0, 0, Math.max(result.getContentWidth(), result.getViewportWidth()),
                        Math.max(result.getContentHeight(), result.getViewportHeight()), 0xFF000000 | org.duckdns.hjow.swingwebpanel.css.values.NamedColors.lookupSystemDark("canvas"),
                        root.getNode()));
            }
            return;
        }
        canvasSource = source;
        double w = Math.max(result.getContentWidth(), result.getViewportWidth());
        double h = Math.max(result.getContentHeight(), result.getViewportHeight());
        ComputedStyle s = source.getStyle();
        int color = s.color(Prop.BACKGROUND_COLOR);
        if ((color >>> 24) != 0) {
            emit(new DisplayCommands.FillRect(0, 0, w, h, color, source.getNode()));
        }
        // 캔버스의 배경 레이어는 캔버스 전체를 위치 기준과 클립으로 쓴다.
        List<DisplayCommand> cmds = new ArrayList<DisplayCommand>();
        layers.build(cmds, s, new RoundedBox(0, 0, w, h, new double[8]), new double[4], new double[4], true, source.getNode());
        // 만든 명령을 차례로 더한다.
        for (int i = 0; i < cmds.size(); i++) {
            emit(cmds.get(i));
        }
    }

    /**
     * Tells whether a style paints a background (a color with alpha or an image).
     * <p>
     * 스타일이 배경(알파가 있는 색 또는 이미지)을 그리는지 알려 준다.
     *
     * @param s the style or null / 스타일 또는 null
     * @return true if there is a background / 배경이 있으면 true
     */
    private static boolean hasBackground(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        if ((s.color(Prop.BACKGROUND_COLOR) >>> 24) != 0) {
            return true;
        }
        return BackgroundLayers.hasLayers(s);
    }

    /**
     * Tells whether a box is painted (the computed visibility is not hidden or collapse).
     * <p>
     * 상자를 그리는지 알려 준다(계산된 visibility 가 hidden 이나 collapse 가 아님).
     *
     * @param s the style or null / 스타일 또는 null
     * @return true if visible / 보이면 true
     */
    private static boolean visible(ComputedStyle s) {
        return s == null || (!s.isKeyword(Prop.VISIBILITY, "hidden") && !s.isKeyword(Prop.VISIBILITY, "collapse"));
    }

    /**
     * Paints the content of a box that the walker reports: a text fragment, a list marker or a replaced box.
     * <p>
     * 순회기가 알리는 상자의 내용을 그린다: 텍스트 조각, 목록 표식, 치환 상자.
     *
     * @param b the box / 상자
     */
    public void content(LayoutBox b) {
        LayoutBox.Type t = b.getType();
        if (t == LayoutBox.Type.TEXT) {
            paintText(b);
        } else if (t == LayoutBox.Type.MARKER) {
            paintMarker(b);
        } else if (t == LayoutBox.Type.REPLACED) {
            paintReplaced(b);
        }
    }

    /**
     * Adds a command unless the output is suppressed (inside a group with an opacity of 0 nothing is painted).
     * <p>
     * 출력이 억제되지 않았으면 명령을 더한다(불투명도 0 인 그룹 안에서는 아무것도 그리지 않는다).
     *
     * @param c the command / 명령
     */
    private void emit(DisplayCommand c) {
        if (suppressed == 0) {
            out.add(c);
        }
    }

    /**
     * Opens a group: adds its start command and remembers where it is. Inside a suppressed part no command is added.
     * <p>
     * 그룹을 연다: 시작 명령을 더하고 위치를 기억한다. 억제된 부분 안에서는 명령을 더하지 않는다.
     *
     * @param type  start type / 시작 종류
     * @param x     left / 왼쪽
     * @param y     top / 위쪽
     * @param w     width / 너비
     * @param h     height / 높이
     * @param alpha opacity / 불투명도
     * @param node  DOM node or null / DOM 노드 또는 null
     */
    private void openGroup(DisplayCommand.Type type, double x, double y, double w, double h, float alpha, Node node) {
        openGroup(type, x, y, w, h, alpha, BlendModes.NORMAL, node);
    }

    /**
     * Opens a group with a blend mode: adds its start command and remembers where it is. Inside a suppressed part no command is added.
     * <p>
     * 혼합 모드가 있는 그룹을 연다: 시작 명령을 더하고 위치를 기억한다. 억제된 부분 안에서는 명령을 더하지 않는다.
     *
     * @param type  start type / 시작 종류
     * @param x     left / 왼쪽
     * @param y     top / 위쪽
     * @param w     width / 너비
     * @param h     height / 높이
     * @param alpha opacity / 불투명도
     * @param blend blend mode constant / 혼합 모드 상수
     * @param node  DOM node or null / DOM 노드 또는 null
     */
    private void openGroup(DisplayCommand.Type type, double x, double y, double w, double h, float alpha, int blend, Node node) {
        if (suppressed > 0) {
            open.add(Integer.valueOf(SKIPPED));
            return;
        }
        open.add(Integer.valueOf(out.size()));
        out.add(new DisplayCommands.Group(type, x, y, w, h, alpha, blend, node));
    }

    /**
     * Closes the innermost group: adds its end command and links the start command to it.
     * <p>
     * 가장 안쪽 그룹을 닫는다: 끝 명령을 더하고 시작 명령과 연결한다.
     *
     * @param type end type / 끝 종류
     * @param node DOM node or null / DOM 노드 또는 null
     */
    private void closeGroup(DisplayCommand.Type type, Node node) {
        int start = open.remove(open.size() - 1).intValue();
        if (start < 0) {
            return;
        }
        DisplayCommand s = out.get(start);
        java.awt.geom.Rectangle2D b = s.getBounds();
        out.add(new DisplayCommands.Group(type, b.getX(), b.getY(), b.getWidth(), b.getHeight(), 1f, node));
        s.groupEnd = out.size() - 1;
    }

    /**
     * Starts a clip group.
     * <p>
     * 자르기 그룹을 시작한다.
     *
     * @param owner the clipping box / 자르는 상자
     * @param x     left / 왼쪽
     * @param y     top / 위쪽
     * @param w     width / 너비
     * @param h     height / 높이
     */
    public void beginClip(LayoutBox owner, double x, double y, double w, double h) {
        ComputedStyle s = owner.getStyle();
        // border-radius 가 있는 상자가 padding box 로 자를 때는 둥근 padding 가장자리로 자른다(CSS Backgrounds and Borders 5.2).
        if (s != null && suppressed == 0) {
            RoundedBox border = RoundedBox.ofBorderBox(s, owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
            if (border.isRounded()) {
                RoundedBox pad = border.inset(owner.getBorderTop(), owner.getBorderRight(), owner.getBorderBottom(), owner.getBorderLeft());
                if (Math.abs(pad.x - x) < 0.5 && Math.abs(pad.y - y) < 0.5 && Math.abs(pad.w - w) < 0.5 && Math.abs(pad.h - h) < 0.5) {
                    open.add(Integer.valueOf(out.size()));
                    out.add(new BoxCommands.RoundedClip(pad.toShape(), owner.getNode()));
                    return;
                }
            }
        }
        openGroup(DisplayCommand.Type.CLIP_START, x, y, w, h, 1f, owner.getNode());
    }

    /**
     * Ends a clip group.
     * <p>
     * 자르기 그룹을 끝낸다.
     *
     * @param owner the clipping box / 자르는 상자
     */
    public void endClip(LayoutBox owner) {
        closeGroup(DisplayCommand.Type.CLIP_END, owner.getNode());
    }

    /**
     * Starts an opacity group; a box that is fully transparent paints nothing (neither do its descendants).
     * <p>
     * 불투명도 그룹을 시작한다. 완전히 투명한 상자는 아무것도 그리지 않는다(자손도 마찬가지).
     *
     * @param box     the box / 상자
     * @param opacity the opacity / 불투명도
     */
    public void beginOpacity(LayoutBox box, double opacity) {
        if (opacity <= 0) {
            open.add(Integer.valueOf(HIDDEN));
            suppressed++;
            return;
        }
        double[] b = subtreeBounds(box);
        // 혼합 모드가 있으면 그룹을 그릴 때 배경과 혼합한다(CSS Compositing 1).
        openGroup(DisplayCommand.Type.OPACITY_START, b[0], b[1], b[2] - b[0], b[3] - b[1], (float) opacity, BlendModes.modeOf(Effects.blendMode(box.getStyle())),
                box.getNode());
    }

    /**
     * Starts the effect group of a box: its content is painted into a buffer that gets the filter functions (and the mask) applied.
     * <p>
     * 상자의 효과 그룹을 시작한다: 내용을 버퍼에 그린 뒤 필터 함수(와 마스크)를 입힌다.
     *
     * @param box the box with a filter or a mask / 필터나 마스크가 있는 상자
     */
    public void beginEffects(LayoutBox box) {
        if (suppressed > 0) {
            open.add(Integer.valueOf(SKIPPED));
            return;
        }
        double[] b = subtreeBounds(box);
        open.add(Integer.valueOf(out.size()));
        FilterSpec backdrop = FilterSpec.ofBackdrop(box.getStyle());
        // 배경 필터는 요소의 border box(둥근 모서리 포함)만 바꾼다.
        java.awt.Shape backdropShape = backdrop == null ? null : RoundedBox.ofBorderBox(box.getStyle(), box.getX(), box.getY(), box.getWidth(), box.getHeight()).toShape();
        // clip-path: url(#id) 는 같은 문서의 SVG clipPath 요소로 버퍼를 자른다(없는 요소이면 아무것도 자르지 않는다).
        String clipId = ClipPaths.reference(box.getStyle());
        SvgReferences refs = referencesOf(box);
        SvgReference clipRef = clipId == null || refs == null ? null : refs.clipPath(clipId);
        java.awt.geom.Rectangle2D border = new java.awt.geom.Rectangle2D.Double(box.getX(), box.getY(), box.getWidth(), box.getHeight());
        out.add(new EffectGroup(b[0], b[1], b[2] - b[0], b[3] - b[1], filterOf(box), MaskLayers.of(box, layers, refs), backdrop, backdropShape, border,
                clipRef, clipRef == null ? null : clipId, box.getNode()));
    }

    /**
     * Returns the SVG references of the document of a box (they are read once for every document of this build).
     * <p>
     * 상자의 문서의 SVG 참조를 반환한다(이번 빌드의 문서마다 한 번 읽는다).
     *
     * @param box the box / 상자
     * @return the finder, or null for a box without a DOM node / 찾는 쪽. DOM 노드가 없는 상자이면 null
     */
    private SvgReferences referencesOf(LayoutBox box) {
        Document owner = box.getNode() == null ? null : box.getNode().getOwnerDocument();
        if (owner == null) {
            return null;
        }
        SvgReferences refs = svgReferences.get(owner);
        if (refs == null) {
            refs = new SvgReferences(owner);
            svgReferences.put(owner, refs);
        }
        return refs;
    }

    /**
     * Returns the filter functions of a box with the references to SVG <code>filter</code> elements resolved (null if it has no filter or a reference
     * names no filter of the document).
     * <p>
     * 상자의 필터 함수를 SVG <code>filter</code> 요소 참조를 푼 채로 반환한다(필터가 없거나 참조가 문서에 없는 필터를 가리키면 null).
     *
     * @param box the box / 상자
     * @return the filter functions or null / 필터 함수. 없으면 null
     */
    private FilterSpec filterOf(LayoutBox box) {
        FilterSpec f = FilterSpec.of(box.getStyle());
        if (f == null || !f.hasReferences()) {
            return f;
        }
        return f.resolve(referencesOf(box), box.getWidth(), box.getHeight());
    }

    /**
     * Ends the effect group of a box.
     * <p>
     * 상자의 효과 그룹을 끝낸다.
     *
     * @param box the box / 상자
     */
    public void endEffects(LayoutBox box) {
        closeGroup(DisplayCommand.Type.EFFECT_END, box.getNode());
    }

    /**
     * Ends an opacity group.
     * <p>
     * 불투명도 그룹을 끝낸다.
     *
     * @param box the box / 상자
     */
    public void endOpacity(LayoutBox box) {
        if (open.get(open.size() - 1).intValue() == HIDDEN) {
            open.remove(open.size() - 1);
            suppressed--;
            return;
        }
        closeGroup(DisplayCommand.Type.OPACITY_END, box.getNode());
    }

    /**
     * Starts the group of a fixed box.
     * <p>
     * fixed 상자의 그룹을 시작한다.
     *
     * @param box the fixed box / fixed 상자
     */
    public void beginFixed(LayoutBox box) {
        double[] b = subtreeBounds(box);
        // 변환된 fixed 상자의 그룹 영역은 변환한 뒤의 영역이다.
        if (box.getTransform() != null) {
            java.awt.geom.Rectangle2D r = box.getTransform().createTransformedShape(new java.awt.geom.Rectangle2D.Double(b[0], b[1], b[2] - b[0], b[3] - b[1]))
                    .getBounds2D();
            b = new double[] { r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY() };
        }
        openGroup(DisplayCommand.Type.FIXED_START, b[0], b[1], b[2] - b[0], b[3] - b[1], 1f, box.getNode());
    }

    /**
     * Ends the group of a fixed box.
     * <p>
     * fixed 상자의 그룹을 끝낸다.
     *
     * @param box the fixed box / fixed 상자
     */
    public void endFixed(LayoutBox box) {
        closeGroup(DisplayCommand.Type.FIXED_END, box.getNode());
    }

    /**
     * Starts the group of a sticky box: the painter shifts the commands inside by the amount of the constraint for the scroll position.
     * <p>
     * sticky 상자의 그룹을 시작한다: 페이지를 그릴 때 스크롤 위치에 대한 제약의 이동량만큼 안쪽 명령을 옮긴다.
     *
     * @param box        the sticky box / sticky 상자
     * @param constraint its constraint / 제약
     */
    public void beginSticky(LayoutBox box, StickyConstraint constraint) {
        if (suppressed > 0) {
            open.add(Integer.valueOf(SKIPPED));
            return;
        }
        double[] b = subtreeBounds(box);
        open.add(Integer.valueOf(out.size()));
        out.add(new StickyGroup(b[0], b[1], b[2] - b[0], b[3] - b[1], constraint, box.getNode()));
    }

    /**
     * Ends the group of a sticky box.
     * <p>
     * sticky 상자의 그룹을 끝낸다.
     *
     * @param box the sticky box / sticky 상자
     */
    public void endSticky(LayoutBox box) {
        closeGroup(DisplayCommand.Type.STICKY_END, box.getNode());
    }

    /**
     * Starts the group of a transformed box (P10-06). The bounds of the group are those of the content after the transform; a group of
     * a box that is not visible (suppressed output) is skipped like every group.
     * <p>
     * 변환된 상자의 그룹을 시작한다(P10-06). 그룹의 영역은 변환한 뒤 내용의 영역이다. 보이지 않는 상자(출력 억제)의 그룹은 다른 그룹처럼 건너뛴다.
     *
     * @param box       the transformed box / 변환된 상자
     * @param transform its transform / 변환
     */
    public void beginTransform(LayoutBox box, java.awt.geom.AffineTransform transform) {
        if (suppressed > 0) {
            open.add(Integer.valueOf(SKIPPED));
            return;
        }
        double[] b = subtreeBounds(box);
        java.awt.geom.Rectangle2D r = transform.createTransformedShape(new java.awt.geom.Rectangle2D.Double(b[0], b[1], b[2] - b[0], b[3] - b[1]))
                .getBounds2D();
        open.add(Integer.valueOf(out.size()));
        out.add(new TransformGroup(r.getX(), r.getY(), r.getWidth(), r.getHeight(), transform, box.getNode()));
    }

    /**
     * Ends the group of a transformed box.
     * <p>
     * 변환된 상자의 그룹을 끝낸다.
     *
     * @param box the transformed box / 변환된 상자
     */
    public void endTransform(LayoutBox box) {
        closeGroup(DisplayCommand.Type.TRANSFORM_END, box.getNode());
    }

    /**
     * Starts the clip group of the <code>clip-path</code> shape of a box (P10-08); the group is a clip like that of a rounded box.
     * <p>
     * 상자의 <code>clip-path</code> 모양의 자르기 그룹을 시작한다(P10-08). 그룹은 둥근 상자의 것과 같은 자르기이다.
     *
     * @param box   the box / 상자
     * @param shape the shape / 모양
     */
    public void beginClipPath(LayoutBox box, java.awt.Shape shape) {
        if (suppressed > 0) {
            open.add(Integer.valueOf(SKIPPED));
            return;
        }
        open.add(Integer.valueOf(out.size()));
        out.add(new BoxCommands.RoundedClip(shape, box.getNode()));
    }

    /**
     * Ends the clip group of a <code>clip-path</code>.
     * <p>
     * <code>clip-path</code> 의 자르기 그룹을 끝낸다.
     *
     * @param box the box / 상자
     */
    public void endClipPath(LayoutBox box) {
        closeGroup(DisplayCommand.Type.CLIP_END, box.getNode());
    }

    /**
     * Computes the bounds of a box and all its descendants (border boxes).
     * <p>
     * 상자와 모든 자손의 경계(테두리 상자)를 계산한다.
     *
     * @param c the box / 상자
     * @return {minX, minY, maxX, maxY} / {minX, minY, maxX, maxY}
     */
    private double[] subtreeBounds(LayoutBox c) {
        double[] b = { c.getX(), c.getY(), c.getX() + c.getWidth(), c.getY() + c.getHeight() };
        grow(c, b);
        expand(c, b);
        growFilter(c, b);
        return b;
    }

    /**
     * Grows bounds by the area that the filter functions of a box reach beyond its content (blurs and drop shadows).
     * <p>
     * 상자의 필터 함수가 내용 밖으로 닿는 영역만큼 경계를 넓힌다(흐림과 drop shadow).
     *
     * @param c the box / 상자
     * @param b the bounds that are grown / 넓힐 경계
     */
    private void growFilter(LayoutBox c, double[] b) {
        FilterSpec f = filterOf(c);
        if (f == null) {
            return;
        }
        double[] e = f.extent();
        b[0] -= e[0];
        b[1] -= e[1];
        b[2] += e[2];
        b[3] += e[3];
    }

    /**
     * Grows bounds by the outer shadows of a box (they reach beyond the border box).
     * <p>
     * 상자의 바깥 그림자만큼 경계를 넓힌다(그림자는 border box 밖으로 뻗는다).
     *
     * @param c the box / 상자
     * @param b the bounds that are grown / 넓힐 경계
     */
    private static void grow(LayoutBox c, double[] b) {
        // border-image 의 영역은 outset 만큼 border box 밖으로 뻗는다(CSS Backgrounds 3 5.4).
        double[] o = BorderImageLayer.outsetOf(c, c.getStyle(), BorderImageLayer.BORDER);
        if (o != null) {
            b[0] = Math.min(b[0], c.getX() - o[3]);
            b[1] = Math.min(b[1], c.getY() - o[0]);
            b[2] = Math.max(b[2], c.getX() + c.getWidth() + o[1]);
            b[3] = Math.max(b[3], c.getY() + c.getHeight() + o[2]);
        }
        double[] e = BoxShadows.extent(c.getStyle());
        if (e != null) {
            b[0] = Math.min(b[0], c.getX() - e[0]);
            b[1] = Math.min(b[1], c.getY() - e[1]);
            b[2] = Math.max(b[2], c.getX() + c.getWidth() + e[2]);
            b[3] = Math.max(b[3], c.getY() + c.getHeight() + e[3]);
        }
    }

    /**
     * Grows bounds to include the descendants of a box.
     * <p>
     * 상자의 자손을 포함하도록 경계를 넓힌다.
     *
     * @param c the box / 상자
     * @param b the bounds that are grown / 넓힐 경계
     */
    private void expand(LayoutBox c, double[] b) {
        // 모든 자손의 테두리 상자를 경계에 포함시킨다(변환된 자손은 변환한 영역을 포함시킨다).
        for (LayoutBox k : c.getChildren()) {
            if (k.getTransform() != null || FilterSpec.has(k.getStyle())) {
                double[] kb = { k.getX(), k.getY(), k.getX() + k.getWidth(), k.getY() + k.getHeight() };
                grow(k, kb);
                expand(k, kb);
                growFilter(k, kb);
                java.awt.geom.Rectangle2D r = new java.awt.geom.Rectangle2D.Double(kb[0], kb[1], kb[2] - kb[0], kb[3] - kb[1]);
                // 변환된 자손은 변환한 뒤의 영역을 경계에 포함시킨다.
                if (k.getTransform() != null) {
                    r = k.getTransform().createTransformedShape(r).getBounds2D();
                }
                b[0] = Math.min(b[0], r.getMinX());
                b[1] = Math.min(b[1], r.getMinY());
                b[2] = Math.max(b[2], r.getMaxX());
                b[3] = Math.max(b[3], r.getMaxY());
                continue;
            }
            b[0] = Math.min(b[0], k.getX());
            b[1] = Math.min(b[1], k.getY());
            b[2] = Math.max(b[2], k.getX() + k.getWidth());
            b[3] = Math.max(b[3], k.getY() + k.getHeight());
            grow(k, b);
            expand(k, b);
        }
    }

    /**
     * Paints the background, border and outline of a box.
     * <p>
     * 상자의 배경, 경계선, 아웃라인을 그린다.
     *
     * @param b the box / 상자
     */
    public void decorate(LayoutBox b) {
        ComputedStyle s = b.getStyle();
        if (s == null || !visible(s) || b.getNode() == null) {
            return;
        }
        if (b.isHiddenEmptyCell()) {
            // empty-cells:hide 인 빈 셀은 배경도 border 도 그리지 않는다.
            return;
        }
        double x = b.getX();
        double y = b.getY();
        double w = b.getWidth();
        double h = b.getHeight();
        if (b.getType() == LayoutBox.Type.TABLE) {
            // 표의 배경과 border 는 위아래 caption 을 뺀 격자에 속한다.
            double[] g = b.getGridRectangle();
            x = g[0];
            y = g[1];
            w = g[2];
            h = g[3];
        }
        RoundedBox outer = RoundedBox.ofBorderBox(s, x, y, w, h);
        double[] borders = { b.getBorderTop(), b.getBorderRight(), b.getBorderBottom(), b.getBorderLeft() };
        double[] paddings = { b.getPaddingTop(), b.getPaddingRight(), b.getPaddingBottom(), b.getPaddingLeft() };
        List<DisplayCommand> cmds = new ArrayList<DisplayCommand>();
        // 1. 바깥 그림자(배경보다 먼저, 상자 바깥으로만 그린다).
        BoxShadows.addOuter(cmds, s, outer, b.getNode());
        if (b != canvasSource) {
            int count = BackgroundLayers.layers(s.get(Prop.BACKGROUND_IMAGE)).size();
            // 배경색은 마지막 레이어의 background-clip 으로 자른다(CSS Backgrounds and Borders 3.7).
            CssValue clipValue = BackgroundLayers.item(s, Prop.BACKGROUND_CLIP, Math.max(0, count - 1));
            String clip = clipValue instanceof CssKeyword ? ((CssKeyword) clipValue).getName() : "border-box";
            RoundedBox cb = outer;
            if ("padding-box".equals(clip) || "content-box".equals(clip)) {
                cb = outer.inset(borders[0], borders[1], borders[2], borders[3]);
                if ("content-box".equals(clip)) {
                    cb = cb.inset(paddings[0], paddings[1], paddings[2], paddings[3]);
                }
            }
            int color = s.color(Prop.BACKGROUND_COLOR);
            DisplayCommand fill = null;
            if ((color >>> 24) != 0 && cb.w > 0 && cb.h > 0) {
                if (cb.isRounded()) {
                    fill = new BoxCommands.ShapeFill(cb.toShape(), color, b.getNode());
                } else {
                    fill = new DisplayCommands.FillRect(cb.x, cb.y, cb.w, cb.h, color, b.getNode());
                }
            }
            List<DisplayCommand> textMask = null;
            // 배경색이 글자로 자르는 레이어(background-clip: text)에 속하면 색도 글리프 안에만 남긴다.
            if (fill != null && "text".equals(clip)) {
                textMask = textMask(b);
                fill = new TextClipBackground(java.util.Collections.singletonList(fill), textMask, b.getNode());
            }
            // 2. 배경 레이어(이미지, 그라디언트). local 배경을 위해 스크롤하는 상자는 콘텐츠의 범위를 안다.
            List<DisplayCommand> layerCmds = new ArrayList<DisplayCommand>();
            List<Integer> layerIdx = new ArrayList<Integer>();
            double[] scrollExtent = b.isClipsContent() && BackgroundLayers.usesLocal(s) ? contentExtent(b) : null;
            layers.build(layerCmds, s, outer, borders, paddings, false, b.getNode(), BackgroundLayers.BACKGROUND, layerIdx, scrollExtent);
            // 글자로 자르는 레이어는 글리프 안에만 남도록 텍스트 마스크와 함께 감싼다.
            for (int j = 0; j < layerCmds.size(); j++) {
                if ("text".equals(BackgroundLayers.clipOf(s, layerIdx.get(j).intValue()))) {
                    if (textMask == null) {
                        textMask = textMask(b);
                    }
                    layerCmds.set(j, new TextClipBackground(java.util.Collections.singletonList(layerCmds.get(j)), textMask, b.getNode()));
                }
            }
            int[] modes = new int[layerCmds.size() + 1];
            boolean blended = false;
            // 레이어마다 background-blend-mode 의 혼합 모드를 정한다(배경색은 가장 아래이므로 normal).
            for (int j = 0; j < layerCmds.size(); j++) {
                CssValue bm = BackgroundLayers.item(s, Prop.BACKGROUND_BLEND_MODE, layerIdx.get(j).intValue());
                modes[j + 1] = bm instanceof CssKeyword ? BlendModes.modeOf(((CssKeyword) bm).getName()) : BlendModes.NORMAL;
                blended |= modes[j + 1] != BlendModes.NORMAL;
            }
            if (blended) {
                // 혼합하는 레이어가 있으면 배경색과 레이어를 자신의 버퍼에 그려 서로 혼합한 뒤 한 번에 그린다.
                List<DisplayCommand> parts = new ArrayList<DisplayCommand>();
                parts.add(fill != null ? fill : new DisplayCommands.FillRect(cb.x, cb.y, 0, 0, 0, b.getNode()));
                parts.addAll(layerCmds);
                cmds.add(new BlendedBackground(parts, modes, b.getNode()));
            } else {
                if (fill != null) {
                    cmds.add(fill);
                }
                cmds.addAll(layerCmds);
            }
        }
        // 3. 안쪽 그림자(배경 위, border 아래).
        BoxShadows.addInset(cmds, s, outer.inset(borders[0], borders[1], borders[2], borders[3]), b.getNode());
        // 만든 명령을 차례로 더한다.
        for (int i = 0; i < cmds.size(); i++) {
            emit(cmds.get(i));
        }
        if (!b.isCollapsedBorderModel()) {
            // border-image 가 쓸 수 있으면(원본이 none 이 아니고 이미지가 도착했으면) border 스타일 대신 아홉 조각 이미지를 그린다(CSS Backgrounds 3 5.1). border-radius 로 자르지 않는다(4.3).
            BorderImageLayer image = BorderImageLayer.of(b, x, y, w, h, s, layers, BorderImageLayer.BORDER);
            if (image != null) {
                emit(image);
            } else {
                border(b, s, x, y, w, h, outer);
            }
        }
        outline(b, s, x, y, w, h);
    }

    /**
     * Builds the commands of the text of a box in an opaque color, for <code>background-clip: text</code>: the background is painted only where this
     * text has opaque pixels. The text of a block-level box is that of all its descendants; the text of an inline element is the text boxes of its line
     * that belong to the element. The commands are made by the same code as the page text, so the glyphs, the spacing and the order are the same, and
     * they are taken out of the output again (the page text is painted by its own commands later).
     * <p>
     * 상자의 텍스트를 불투명한 색의 명령으로 만든다(<code>background-clip: text</code> 용): 배경은 이 텍스트에 불투명한 화소가 있는 곳에만 그려진다. 블록 수준 상자의
     * 텍스트는 모든 자손의 텍스트이고, 인라인 요소의 텍스트는 그 줄에서 요소에 속하는 텍스트 상자이다. 명령은 페이지 텍스트와 같은 코드로 만들므로 글리프, 간격, 순서가 같으며,
     * 출력에서는 다시 뺀다(페이지 텍스트는 나중에 자신의 명령이 그린다).
     *
     * @param b the box whose background is clipped / 배경을 자르는 상자
     * @return the text commands / 텍스트 명령
     */
    private List<DisplayCommand> textMask(LayoutBox b) {
        int from = out.size();
        int saved = maskColor;
        maskColor = 0xFF000000;
        try {
            if (b.getType() == LayoutBox.Type.INLINE) {
                LayoutBox line = b.getParent();
                List<LayoutBox> siblings = line == null ? new ArrayList<LayoutBox>() : line.getChildren();
                // 같은 줄에서 이 인라인 요소에 속하는 텍스트 상자를 찾는다.
                for (int i = 0; i < siblings.size(); i++) {
                    LayoutBox c = siblings.get(i);
                    if (c.getType() == LayoutBox.Type.TEXT && b.isTextOfInline(c)) {
                        paintText(c);
                    }
                }
            } else {
                ArrayList<LayoutBox> stack = new ArrayList<LayoutBox>();
                stack.add(b);
                // 명시적 스택으로 자손을 문서 순서로 훑으며 텍스트 상자를 만난다.
                while (!stack.isEmpty()) {
                    LayoutBox c = stack.remove(stack.size() - 1);
                    if (c != b && c.getType() == LayoutBox.Type.TEXT) {
                        paintText(c);
                    }
                    List<LayoutBox> kids = c.getChildren();
                    // 자식을 거꾸로 쌓아 문서 순서로 처리한다.
                    for (int i = kids.size() - 1; i >= 0; i--) {
                        stack.add(kids.get(i));
                    }
                }
            }
        } finally {
            maskColor = saved;
        }
        List<DisplayCommand> added = out.subList(from, out.size());
        List<DisplayCommand> result = new ArrayList<DisplayCommand>(added);
        added.clear();
        return result;
    }

    /**
     * Computes the right and bottom edge of the content of a box that clips its content, in document coordinates (the padding at the end sides is
     * included): the scrollable overflow area that a <code>background-attachment: local</code> background is positioned in.
     * <p>
     * 내용을 자르는 상자의 콘텐츠의 오른쪽과 아래쪽 가장자리를 문서 좌표로 계산한다(끝 쪽 padding 포함): <code>background-attachment: local</code> 배경이 위치하는
     * 스크롤 가능한 넘침 영역이다.
     *
     * @param b the box / 상자
     * @return {right, bottom} / {오른쪽, 아래쪽}
     */
    private static double[] contentExtent(LayoutBox b) {
        double right = b.getX() + b.getWidth() - b.getBorderRight();
        double bottom = b.getY() + b.getHeight() - b.getBorderBottom();
        ArrayList<LayoutBox> stack = new ArrayList<LayoutBox>();
        stack.addAll(b.getChildren());
        // 명시적 스택으로 자손을 훑으며 가장 오른쪽과 가장 아래 가장자리를 찾는다(고정 배치 상자는 스크롤 콘텐츠가 아니다).
        while (!stack.isEmpty()) {
            LayoutBox c = stack.remove(stack.size() - 1);
            if (c.isFixedPosition()) {
                continue;
            }
            right = Math.max(right, c.getX() + c.getWidth() + c.getMarginRight() + b.getPaddingRight());
            bottom = Math.max(bottom, c.getY() + c.getHeight() + c.getMarginBottom() + b.getPaddingBottom());
            stack.addAll(c.getChildren());
        }
        return new double[] { right, bottom };
    }

    /**
     * Paints the collapsed borders of a table: every segment is a filled strip in the style and color of the winning border.
     * <p>
     * 표의 합쳐진 border 를 그린다: 선분마다 이긴 border 의 스타일과 색으로 채운 띠이다.
     *
     * @param table the table box / 표 상자
     */
    public void tableBorders(LayoutBox table) {
        if (!visible(table.getStyle())) {
            return;
        }
        List<TableBorder> segments = table.getCollapsedBorders();
        // 선분마다 가로이면 위쪽 변, 세로이면 왼쪽 변 하나로 이루어진 border 명령을 만든다.
        for (int i = 0; i < segments.size(); i++) {
            TableBorder t = segments.get(i);
            double th = t.getThickness();
            double[] widths = t.isHorizontal() ? new double[] { th, 0, 0, 0 } : new double[] { 0, 0, 0, th };
            String[] styles = t.isHorizontal() ? new String[] { t.getStyle(), "none", "none", "none" } : new String[] { "none", "none", "none", t.getStyle() };
            int[] colors = { t.getColor(), t.getColor(), t.getColor(), t.getColor() };
            emit(new DisplayCommands.Border(t.getX(), t.getY(), t.getWidth(), t.getHeight(), widths, styles, colors, table.getNode()));
        }
    }

    /**
     * Paints the column rules of a multi-column container: every rule is a left border of the width, style and color of
     * <code>column-rule</code>.
     * <p>
     * 다단 컨테이너의 열 구분선을 그린다: 구분선마다 <code>column-rule</code> 의 너비, 스타일, 색을 가진 왼쪽 border 이다.
     *
     * @param container the multi-column container / 다단 컨테이너
     */
    public void columnRules(LayoutBox container) {
        ComputedStyle s = container.getStyle();
        if (s == null || !visible(s)) {
            return;
        }
        String style = s.keyword(Prop.COLUMN_RULE_STYLE);
        int color = s.color(Prop.COLUMN_RULE_COLOR);
        if (!BorderPainter.isVisibleStyle(style) || (color >>> 24) == 0) {
            return;
        }
        double[][] rules = container.getColumnRules();
        // 구분선마다 왼쪽 변 하나로 이루어진 border 명령을 만든다.
        for (int i = 0; i < rules.length; i++) {
            double[] r = rules[i];
            double[] widths = { 0, 0, 0, r[2] };
            String[] styles = { "none", "none", "none", style };
            int[] colors = { color, color, color, color };
            emit(new DisplayCommands.Border(r[0], r[1], r[2], r[3], widths, styles, colors, container.getNode()));
        }
    }

    /**
     * Paints the resize handle of a box with <code>resize</code> (CSS Basic User Interface 4 section 6.1) in the corner of its padding box.
     * <p>
     * <code>resize</code> 가 있는 상자의 크기 조절 핸들을 padding box 모서리에 그린다(CSS Basic User Interface 4 6.1 절).
     *
     * @param box the resizable box / 크기를 바꿀 수 있는 상자
     */
    public void resizer(LayoutBox box) {
        ComputedStyle s = box.getStyle();
        double[] h = org.duckdns.hjow.swingwebpanel.layout.Resizing.handle(box);
        if (s == null || h == null || !visible(s)) {
            return;
        }
        emit(new DisplayCommands.Resizer(h[0], h[1], h[2], box.getNode()));
    }

    /**
     * Returns the first whitespace separated token of a text.
     * <p>
     * 텍스트의 첫 공백 구분 토큰을 반환한다.
     *
     * @param t the text / 텍스트
     * @return the first token / 첫 토큰
     */
    private static String firstToken(String t) {
        String s = t.trim();
        int i = s.indexOf(' ');
        return i < 0 ? s : s.substring(0, i);
    }

    /**
     * Adds the border command of a box if any side is visible.
     * <p>
     * 어느 변이든 보이면 상자의 경계선 명령을 더한다.
     *
     * @param b the box / 상자
     * @param s the style / 스타일
     * @param x left of the border box / 테두리 상자의 왼쪽
     * @param y top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     * @param outer the rounded border edge of the box (a border with rounded corners uses it) / 상자의 둥근 border 가장자리(둥근 모서리 border 가 쓴다)
     */
    private void border(LayoutBox b, ComputedStyle s, double x, double y, double w, double h, RoundedBox outer) {
        double[] widths = { b.getBorderTop(), b.getBorderRight(), b.getBorderBottom(), b.getBorderLeft() };
        String[] styles = { s.keyword(Prop.BORDER_TOP_STYLE), s.keyword(Prop.BORDER_RIGHT_STYLE), s.keyword(Prop.BORDER_BOTTOM_STYLE),
            s.keyword(Prop.BORDER_LEFT_STYLE) };
        int[] colors = { s.color(Prop.BORDER_TOP_COLOR), s.color(Prop.BORDER_RIGHT_COLOR), s.color(Prop.BORDER_BOTTOM_COLOR),
            s.color(Prop.BORDER_LEFT_COLOR) };
        boolean any = false;
        // 보이는 변이 하나라도 있는지 확인한다.
        for (int i = 0; i < 4; i++) {
            if (widths[i] > 0 && BorderPainter.isVisibleStyle(styles[i]) && (colors[i] >>> 24) != 0) {
                any = true;
            }
        }
        if (any && outer.isRounded()) {
            // 둥근 모서리 border 는 둥근 고리로 그린다(fieldset 의 legend 로 끊긴 위쪽 경계선과 둥근 모서리의 조합은 지원하지 않는다).
            emit(new BoxCommands.RoundedBorder(outer, widths, styles, colors, b.getNode()));
        } else if (any) {
            DisplayCommand border = new DisplayCommands.Border(x, y, w, h, widths, styles, colors, b.getNode());
            LayoutBox legend = b.getRenderedLegend();
            double gapStart = legend == null ? 0 : Math.max(x, legend.getX());
            double gapEnd = legend == null ? 0 : Math.min(x + w, legend.getX() + legend.getWidth());
            if (legend == null || widths[0] <= 0 || gapEnd <= gapStart) {
                emit(border);
                return;
            }
            // fieldset 의 위쪽 경계선은 legend 가 놓인 구간에서 끊긴다: 위쪽 경계선이 없는 경계선 하나와, legend 양옆의 위쪽 경계선 조각 둘로 나눠 그린다.
            double[] noTop = { 0, widths[1], widths[2], widths[3] };
            double[] topOnly = { widths[0], 0, 0, 0 };
            emit(new DisplayCommands.Border(x, y, w, h, noTop, styles, colors, b.getNode()));
            if (gapStart > x) {
                emit(new DisplayCommands.Border(x, y, gapStart - x, widths[0], topOnly, styles, colors, b.getNode()));
            }
            if (gapEnd < x + w) {
                emit(new DisplayCommands.Border(gapEnd, y, x + w - gapEnd, widths[0], topOnly, styles, colors, b.getNode()));
            }
        }
    }

    /**
     * Adds the outline of a box (painted right after the border; an approximation of the topmost outline layer).
     * <p>
     * 상자의 아웃라인을 더한다(경계선 바로 뒤에 그린다. 가장 위 아웃라인 계층의 근사).
     *
     * @param b the box / 상자
     * @param s the style / 스타일
     * @param x left of the border box / 테두리 상자의 왼쪽
     * @param y top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     */
    private void outline(LayoutBox b, ComputedStyle s, double x, double y, double w, double h) {
        String style = s.keyword(Prop.OUTLINE_STYLE);
        double ow = s.px(Prop.OUTLINE_WIDTH);
        int color = s.color(Prop.OUTLINE_COLOR);
        if (!BorderPainter.isVisibleStyle(style) || Double.isNaN(ow) || ow <= 0 || (color >>> 24) == 0) {
            return;
        }
        double off = s.px(Prop.OUTLINE_OFFSET);
        if (Double.isNaN(off)) {
            off = 0;
        }
        double d = ow + off;
        double[] widths = { ow, ow, ow, ow };
        String[] styles = { style, style, style, style };
        int[] colors = { color, color, color, color };
        emit(new DisplayCommands.Border(x - d, y - d, w + 2 * d, h + 2 * d, widths, styles, colors, b.getNode()));
    }

    /**
     * Resolves a URL against the base URL of the document.
     * <p>
     * URL 을 문서의 기준 URL 로 해석한다.
     *
     * @param ref the reference / 참조
     * @return the absolute URL or the reference itself if it cannot be resolved / 절대 URL. 해석할 수 없으면 참조 그대로
     */
    private String resolve(String ref) {
        String base = doc == null ? null : doc.getBaseUri();
        if (base == null || base.length() == 0 || "about:blank".equals(base) || "about:text".equals(base)) {
            return ref;
        }
        try {
            return UrlUtil.resolve(base, ref);
        } catch (IllegalArgumentException e) {
            return ref;
        }
    }

    /**
     * Paints a text fragment.
     * <p>
     * 텍스트 조각을 그린다.
     *
     * @param t the TEXT box / TEXT 상자
     */
    private void paintText(LayoutBox t) {
        ComputedStyle s = t.getStyle();
        String text = t.getText();
        if (text == null || text.length() == 0 || t.getFont() == null || !visible(s)) {
            return;
        }
        // 웹 글꼴의 block 기간(font-display)에는 대체 글꼴의 자리만 차지하고 글자, 장식, 강조점을 그리지 않는다(CSS Fonts 4 4.8).
        if (t.isFontBlocked()) {
            return;
        }
        int color = maskColor != 0 ? maskColor : (s == null ? 0xFF000000 : s.color(Prop.COLOR));
        int decoColor = color;
        int decoStyle = TextRenderer.STYLE_SOLID;
        double thickness = Double.NaN;
        double wordSpacing = 0;
        if (s != null) {
            int dc = s.color(Prop.TEXT_DECORATION_COLOR);
            if ((dc >>> 24) != 0 && maskColor == 0) {
                decoColor = dc;
            }
            String st = s.keyword(Prop.TEXT_DECORATION_STYLE);
            if ("double".equals(st)) {
                decoStyle = TextRenderer.STYLE_DOUBLE;
            } else if ("dotted".equals(st)) {
                decoStyle = TextRenderer.STYLE_DOTTED;
            } else if ("dashed".equals(st)) {
                decoStyle = TextRenderer.STYLE_DASHED;
            } else if ("wavy".equals(st)) {
                decoStyle = TextRenderer.STYLE_WAVY;
            }
            thickness = s.px(Prop.TEXT_DECORATION_THICKNESS);
            double ws = s.px(Prop.WORD_SPACING);
            wordSpacing = Double.isNaN(ws) ? 0 : ws;
        }
        if (t.getTextRotation() != 0) {
            // 똑바로 서거나 조합한 글리프(P10-03-e)는 따로 그리고 나머지는 눕힌 텍스트로 그린다.
            DisplayCommand upright = VerticalTextCommands.create(t, text, color, t.getTextDecoration(), decoStyle, decoColor, thickness);
            if (upright != null) {
                emit(upright);
            } else {
                emitRotated(t, text, color, wordSpacing, t.getTextDecoration(), decoStyle, decoColor, thickness);
            }
            // 세로 조판 모드(vertical-rl, vertical-lr)의 강조점은 글리프의 오른쪽(또는 왼쪽)에 똑바로 선다. 눕힌 조각의 시계 반대 방향 쓰기(sideways-lr)는 그리지 않는다.
            TextEmphasis verticalEmphasis = t.getTextRotation() == 1 ? TextEmphasis.of(s) : null;
            if (verticalEmphasis != null && verticalEmphasis.isVertical()) {
                emit(new VerticalTextCommands.EmphasisColumn(t, text, verticalEmphasis, wordSpacing, t.getNode()));
            }
            return;
        }
        DisplayCommands.Text textCommand = new DisplayCommands.Text(text, t.getFont(), t.getX(), t.getBaseline(), t.getWidth(), t.getAscent(), t.getDescent(),
                color, t.getLetterSpacing(), wordSpacing, t.getBidiLevel(), t.getTextDecoration(), decoStyle, decoColor, thickness, t.getNode());
        // 밑줄과 윗줄의 거리, 위치, 글리프 건너뛰기(text-underline-offset, text-underline-position, text-decoration-skip-ink)를 명령에 알린다.
        if (s != null && (t.getTextDecoration() & 3) != 0) {
            double offset = s.resolve(Prop.TEXT_UNDERLINE_OFFSET, t.getFont().getSize2D());
            String position = s.get(Prop.TEXT_UNDERLINE_POSITION) == null ? "" : s.get(Prop.TEXT_UNDERLINE_POSITION).toCssText();
            boolean skipInk = !"none".equals(s.keyword(Prop.TEXT_DECORATION_SKIP_INK));
            textCommand.withUnderlineOptions(offset, position.indexOf("under") >= 0, t.getDescent(), skipInk && maskColor == 0);
        }
        // 사용자 정의 하이라이트(CSS Custom Highlight API)가 덮는 부분은 배경을 깔고 그 하이라이트의 색과 장식으로 다시 그린다.
        List<HighlightOverlay.Layer> highlights = maskColor == 0 ? HighlightOverlay.layersOf(result, doc, t) : java.util.Collections.<HighlightOverlay.Layer>emptyList();
        if (highlights.isEmpty()) {
            emit(textCommand);
        } else {
            emitHighlighted(t, text, textCommand, highlights, color, wordSpacing, decoStyle, decoColor, thickness);
        }
        // 강조점(text-emphasis)은 텍스트 위나 아래에 글자마다 그린다. 점의 공간은 배치가 줄 높이에 더해 두었다.
        TextEmphasis emphasis = TextEmphasis.of(s);
        if (emphasis != null) {
            emit(new EmphasisMarks(text, t.getFont(), emphasis, new double[] { t.getX(), t.getY(), t.getWidth(), t.getHeight() }, t.getBaseline(),
                    t.getAscent(), t.getDescent(), t.getLetterSpacing(), wordSpacing, t.getBidiLevel(), t.getNode()));
        }
    }

    /**
     * Adds the commands of a text fragment that custom highlights cover (CSS Custom Highlight API 1 section 4.2.4, CSS Pseudo-Elements 4 section 3.6): first
     * the <code>background-color</code> of every layer over its parts (lowest layer first), then the text, painted in its own color outside the highlighted
     * parts and, inside a part, with the <code>color</code> and the <code>text-decoration</code> of the topmost layer that covers it (a layer whose color is
     * <code>currentcolor</code> keeps the color of the layer below).
     * <p>
     * 사용자 정의 하이라이트가 덮는 텍스트 조각의 명령을 더한다(CSS Custom Highlight API 1 4.2.4 절, CSS Pseudo-Elements 4 3.6 절): 먼저 층마다 그 부분 위에
     * <code>background-color</code> 를 (아래 층부터) 깔고, 그다음 텍스트를 하이라이트되지 않은 부분은 자신의 색으로, 부분 안은 그것을 덮는 가장 위 층의 <code>color</code> 와
     * <code>text-decoration</code> 으로 그린다(색이 <code>currentcolor</code> 인 층은 아래 층의 색을 쓴다).
     *
     * @param t           the TEXT box / TEXT 상자
     * @param text        the text / 텍스트
     * @param base        the command of the text in its own color / 자신의 색의 텍스트 명령
     * @param layers      the highlight layers, lowest first / 하이라이트 층(아래 것부터)
     * @param color       the color of the text / 텍스트의 색
     * @param wordSpacing the word spacing / 단어 간격
     * @param decoStyle   the decoration style of the text / 텍스트의 장식 스타일
     * @param decoColor   the decoration color of the text / 텍스트의 장식 색
     * @param thickness   the decoration thickness or NaN / 장식 두께 또는 NaN
     */
    private void emitHighlighted(LayoutBox t, String text, DisplayCommands.Text base, List<HighlightOverlay.Layer> layers, int color, double wordSpacing,
            int decoStyle, int decoColor, double thickness) {
        // 1. 배경: 아래 층부터 부분마다 배경색을 칠한다.
        for (int i = 0; i < layers.size(); i++) {
            HighlightOverlay.Layer l = layers.get(i);
            int bg = HighlightOverlay.colorOf(l.style, Prop.BACKGROUND_COLOR, HighlightOverlay.colorOf(l.style, Prop.COLOR, color));
            if ((bg >>> 24) == 0) {
                continue;
            }
            // 덮는 부분마다 사각형을 칠한다.
            for (int k = 0; k < l.spans.size(); k++) {
                java.awt.geom.Rectangle2D r = TextSelections.fragmentRect(t, l.spans.get(k)[0], l.spans.get(k)[1]);
                emit(new DisplayCommands.FillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), bg, t.getNode()));
            }
        }
        // 2. 텍스트: 부분의 경계로 텍스트를 잘게 나누고, 조각마다 덮는 층을 아래에서 위로 적용해 색(currentcolor 는 아래 것을 유지)과 장식을 정한다.
        java.util.TreeSet<Integer> cuts = new java.util.TreeSet<Integer>();
        // 모든 층의 부분 경계를 모은다.
        for (int i = 0; i < layers.size(); i++) {
            for (int k = 0; k < layers.get(i).spans.size(); k++) {
                cuts.add(Integer.valueOf(layers.get(i).spans.get(k)[0]));
                cuts.add(Integer.valueOf(layers.get(i).spans.get(k)[1]));
            }
        }
        java.util.LinkedHashMap<String, java.awt.geom.Area> groups = new java.util.LinkedHashMap<String, java.awt.geom.Area>();
        java.util.Map<String, DisplayCommand> groupText = new java.util.HashMap<String, DisplayCommand>();
        Integer prev = null;
        // 이웃한 경계 사이의 조각마다 그것을 덮는 층으로 모양을 정한다.
        for (Integer cut : cuts) {
            if (prev != null) {
                int a = prev.intValue();
                int b = cut.intValue();
                int c = color;
                int bits = 0;
                int dStyle = decoStyle;
                int dColor = decoColor;
                double dThick = thickness;
                boolean any = false;
                // 아래 층부터 이 조각을 덮는 층을 적용한다(위 층이 이긴다).
                for (int i = 0; i < layers.size(); i++) {
                    HighlightOverlay.Layer l = layers.get(i);
                    if (!covers(l.spans, a, b)) {
                        continue;
                    }
                    any = true;
                    c = HighlightOverlay.colorOf(l.style, Prop.COLOR, c);
                    int lb = HighlightOverlay.decorationOf(l.style);
                    if (lb != 0) {
                        bits = lb;
                        String st = l.style.keyword(Prop.TEXT_DECORATION_STYLE);
                        dStyle = "double".equals(st) ? TextRenderer.STYLE_DOUBLE : "dotted".equals(st) ? TextRenderer.STYLE_DOTTED : "dashed".equals(st)
                                ? TextRenderer.STYLE_DASHED : "wavy".equals(st) ? TextRenderer.STYLE_WAVY : TextRenderer.STYLE_SOLID;
                        dColor = HighlightOverlay.colorOf(l.style, Prop.TEXT_DECORATION_COLOR, c);
                        dThick = l.style.px(Prop.TEXT_DECORATION_THICKNESS);
                    }
                }
                if (any) {
                    int allBits = t.getTextDecoration() | bits;
                    int lineColor = bits != 0 ? dColor : (t.getTextDecoration() != 0 ? decoColor : c);
                    String key = c + "/" + allBits + "/" + dStyle + "/" + lineColor + "/" + dThick;
                    java.awt.geom.Area area = groups.get(key);
                    if (area == null) {
                        area = new java.awt.geom.Area();
                        groups.put(key, area);
                        groupText.put(key, new DisplayCommands.Text(text, t.getFont(), t.getX(), t.getBaseline(), t.getWidth(), t.getAscent(), t.getDescent(), c,
                                t.getLetterSpacing(), wordSpacing, t.getBidiLevel(), allBits, dStyle, lineColor, dThick, t.getNode()));
                    }
                    area.add(new java.awt.geom.Area(TextSelections.fragmentRect(t, a, b)));
                }
            }
            prev = cut;
        }
        List<java.awt.Shape> areas = new ArrayList<java.awt.Shape>();
        List<DisplayCommand> texts = new ArrayList<DisplayCommand>();
        // 모양이 같은 조각을 하나의 영역으로 묶어 그 텍스트와 함께 넘긴다.
        for (java.util.Map.Entry<String, java.awt.geom.Area> g : groups.entrySet()) {
            areas.add(g.getValue());
            texts.add(groupText.get(g.getKey()));
        }
        emit(new HighlightOverlay.Painted(base, areas, texts, t.getNode()));
    }

    /**
     * Tells whether sorted disjoint parts cover a range completely.
     * <p>
     * 정렬되고 겹치지 않는 부분들이 범위를 완전히 덮는지 알려 준다.
     *
     * @param spans the parts / 부분
     * @param a     the start / 시작
     * @param b     the end / 끝
     * @return true if one part contains the range / 한 부분이 범위를 포함하면 true
     */
    private static boolean covers(List<int[]> spans, int a, int b) {
        // 범위를 포함하는 부분을 찾는다.
        for (int i = 0; i < spans.size(); i++) {
            if (spans.get(i)[0] <= a && spans.get(i)[1] >= b) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the command that paints a text fragment of vertical text: the glyphs are rotated by a quarter turn, the baseline is a vertical
     * line inside the box (to the right of the descent side for the clockwise turn) and the text starts at the top of the box (at the
     * bottom for the counter-clockwise turn).
     * <p>
     * 세로 텍스트의 텍스트 조각을 그리는 명령을 더한다: 글리프는 사분원만큼 회전하고 기준선은 상자 안의 세로선이며(시계 방향이면 descent 쪽 오른쪽) 텍스트는 상자 위쪽에서
     * 시작한다(시계 반대 방향이면 아래쪽에서).
     *
     * @param t         the TEXT or MARKER box / TEXT 또는 MARKER 상자
     * @param text      the text / 텍스트
     * @param color     ARGB color / ARGB 색
     * @param wordSpacing word spacing / 단어 간격
     * @param decoration  decoration bits / 장식 비트
     * @param decoStyle   decoration style / 장식 스타일
     * @param decoColor   decoration color / 장식 색
     * @param thickness   decoration thickness or NaN / 장식 두께 또는 NaN
     */
    private void emitRotated(LayoutBox t, String text, int color, double wordSpacing, int decoration, int decoStyle, int decoColor,
            double thickness) {
        boolean cw = t.getTextRotation() == 1;
        double ox = cw ? t.getX() + t.getDescent() : t.getX() + t.getAscent();
        double oy = cw ? t.getY() : t.getY() + t.getHeight();
        double[] bounds = { t.getX(), t.getY(), t.getWidth(), t.getHeight() };
        emit(new DisplayCommands.RotatedText(text, t.getFont(), bounds, ox, oy, cw, t.getTextAdvance(), t.getAscent(), color,
                t.getLetterSpacing(), wordSpacing, t.getBidiLevel(), decoration, decoStyle, decoColor, thickness, t.getNode()));
    }

    /**
     * Paints a list marker: bullets as shapes, other markers as text.
     * <p>
     * 목록 표식을 그린다: 불릿은 모양으로, 그 밖의 표식은 텍스트로.
     *
     * @param m the MARKER box / MARKER 상자
     */
    private void paintMarker(LayoutBox m) {
        ComputedStyle s = m.getStyle();
        // list-style-image 의 표식은 이미지를 표식 상자의 왼쪽(공백 앞)에 그린다. 그림을 쓸 수 없으면 아무것도 그리지 않는다.
        if (m.getMarkerImage() != null) {
            GeneratedImages.Picture p = visible(s) ? layers.anyPicture(m.getMarkerImage(), m.getNode(), s) : null;
            double em = s == null ? 16 : s.fontSizePx();
            if (p != null) {
                double iw = p.hasNaturalSize() ? p.naturalWidth() : em;
                double ih = p.hasNaturalSize() ? p.naturalHeight() : em;
                emit(new GeneratedImages.Draw(p, m.getX(), m.getY() + m.getHeight() - ih, iw, ih, m.getNode()));
            }
            return;
        }
        String text = m.getText();
        if (text == null || text.length() == 0 || m.getFont() == null || !visible(s)) {
            return;
        }
        int color = s == null ? 0xFF000000 : s.color(Prop.COLOR);
        String trimmed = text.trim();
        int shape = -1;
        if ("•".equals(trimmed)) {
            shape = DisplayCommands.SHAPE_DISC;
        } else if ("◦".equals(trimmed)) {
            shape = DisplayCommands.SHAPE_CIRCLE;
        } else if ("▪".equals(trimmed)) {
            shape = DisplayCommands.SHAPE_SQUARE;
        }
        double fontSize = m.getFont().getSize2D();
        if (m.getTextRotation() != 0) {
            emitRotated(m, text, color, 0, 0, TextRenderer.STYLE_SOLID, color, Double.NaN);
            return;
        }
        if (shape >= 0) {
            double size = Math.max(4, Math.round(fontSize * 0.35));
            double bulletAdvance = TextRenderer.advance(m.getFont(), trimmed);
            double cx = m.getX() + bulletAdvance / 2;
            double cy = m.getBaseline() - fontSize * 0.32;
            emit(new DisplayCommands.MarkerShape(Math.round(cx - size / 2), Math.round(cy - size / 2), size, shape, color, m.getNode()));
            return;
        }
        emit(new DisplayCommands.Text(text, m.getFont(), m.getX(), m.getBaseline(), m.getWidth(), m.getAscent(), m.getDescent(), color, 0, 0,
                0, 0, TextRenderer.STYLE_SOLID, color, Double.NaN, m.getNode()));
    }

    /**
     * Paints the content of a replaced box: an image, or the replacement drawing for images, audio and video that cannot be shown.
     * <p>
     * 치환 상자의 내용을 그린다: 이미지, 또는 보여 줄 수 없는 이미지/오디오/비디오의 대체 그림.
     *
     * @param c the REPLACED box / REPLACED 상자
     */
    private void paintReplaced(LayoutBox c) {
        if (!(c.getNode() instanceof Element) || !visible(c.getStyle())) {
            return;
        }
        Element e = (Element) c.getNode();
        String tag = e.getLocalName() == null ? "" : e.getLocalName().toLowerCase();
        double x = c.getContentX();
        double y = c.getContentY();
        double w = c.getContentWidth();
        double h = c.getContentHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        ControlSnapshot control = ControlPainter.snapshot(e, c);
        if (control != null) {
            // 폼 컨트롤은 상태를 스냅샷으로 떠서 그리기 명령 하나로 낸다(배경과 경계선은 상자 그리기가 이미 냈다).
            emit(new ControlCommand(control));
            return;
        }
        if ("img".equals(tag)) {
            paintImageElement(e, c, x, y, w, h);
        } else if ("video".equals(tag)) {
            emit(new DisplayCommands.Placeholder(x, y, w, h, new String[] { UNSUPPORTED_VIDEO }, DisplayCommands.KIND_VIDEO, e));
        } else if ("audio".equals(tag)) {
            paintAudioElement(e, x, y, w, h);
        } else if ("canvas".equals(tag) && images instanceof CanvasSource) {
            // 캔버스는 스크립트가 그린 비트맵의 불변 스냅샷을 상자 크기로 그린다. 아직 그린 것이 없으면 투명하다.
            org.duckdns.hjow.swingwebpanel.media.image.DecodedImage picture = ((CanvasSource) images).getCanvasImage(e);
            if (picture != null) {
                emit(new DisplayCommands.Image(x, y, w, h, picture, e));
            }
        } else if ("object".equals(tag) || "embed".equals(tag)) {
            // 플러그인과 중첩 문서는 실행하지 않는다: 안내와 주소를 보인다(object 의 대체 내용이 있으면 레이아웃이 치환 상자를 만들지 않으므로 여기에 오지 않는다).
            String target = e.getAttribute("embed".equals(tag) ? "src" : "data");
            if (target == null || target.trim().length() == 0) {
                emit(new DisplayCommands.Placeholder(x, y, w, h, new String[] { UNSUPPORTED_PLUGIN }, DisplayCommands.KIND_BROKEN, e));
            } else {
                emit(new DisplayCommands.Placeholder(x, y, w, h, new String[] { UNSUPPORTED_PLUGIN, resolve(target.trim()) }, DisplayCommands.KIND_BROKEN, e));
            }
        } else if ("svg".equals(tag) && InlineSvg.isRoot(e)) {
            // 인라인 SVG 는 하위 트리를 상자 크기의 이미지로 래스터화해서 그린다(그릴 수 없으면 아무것도 내지 않는다).
            org.duckdns.hjow.swingwebpanel.media.image.DecodedImage drawing = InlineSvg.rasterize(e, Math.max(1, (int) Math.round(w)), Math.max(1, (int) Math.round(h)));
            if (drawing != null) {
                emit(new DisplayCommands.Image(x, y, w, h, drawing, e));
            }
        } else if (("iframe".equals(tag) || "frame".equals(tag)) && images instanceof FrameSource) {
            // 중첩된 브라우징 문맥은 프레임 소유자가 이 상자 크기로 배치한 불변 페이지를 상자 안에 클립해서 그린다.
            FramePicture frame = ((FrameSource) images).getFrame(e, w, h);
            if (frame != null) {
                emit(new FrameCommand(x, y, w, h, frame, e));
            }
        }
    }

    /**
     * Paints an <code>img</code> element.
     * <p>
     * <code>img</code> 요소를 그린다.
     *
     * @param e the element / 요소
     * @param c the box / 상자
     * @param x content left / 콘텐츠 왼쪽
     * @param y content top / 콘텐츠 위쪽
     * @param w content width / 콘텐츠 너비
     * @param h content height / 콘텐츠 높이
     */
    private void paintImageElement(Element e, LayoutBox c, double x, double y, double w, double h) {
        // srcset/sizes/picture 를 반영한 실제 소스를 고른다(레이아웃의 ImageReplacedResolver 와 같은 환경).
        ImageSelection.Choice choice = ImageSelection.select(e, new MediaEnvironment(result.getViewportWidth(), result.getViewportHeight()));
        String src = choice == null ? null : choice.getUrl();
        String alt = e.getAttribute("alt") == null ? "" : e.getAttribute("alt");
        if (src == null || src.trim().length() == 0) {
            if (alt.length() > 0) {
                emit(new DisplayCommands.Placeholder(x, y, w, h, new String[] { alt }, DisplayCommands.KIND_BROKEN, e));
            }
            return;
        }
        String url = resolve(src.trim());
        // crossorigin 속성이 있는 요소는 CORS 모드로 가져온 이미지를 그린다.
        ImageResource r = ImageLookup.of(images, e, url);
        if (r == null || r.getState() == ImageResource.State.PENDING) {
            return;
        }
        if (r.getState() == ImageResource.State.LOADED && r.getImage() != null) {
            // image-orientation: none 이면 EXIF 등의 방향을 적용하지 않은 저장된 그대로의 이미지를 그린다(CSS Images 3 5.1절).
            boolean stored = c.getStyle() != null && c.getStyle().isKeyword(Prop.IMAGE_ORIENTATION, "none");
            emit(new DisplayCommands.Image(x, y, w, h, stored ? r.getImage().unoriented() : r.getImage(), e));
            return;
        }
        emit(new DisplayCommands.Placeholder(x, y, w, h, failureLines(url, alt), DisplayCommands.KIND_BROKEN, e));
    }

    /**
     * Builds the lines of a failed resource: the unsupported URL and, if present, the alt text.
     * <p>
     * 실패한 리소스의 줄을 만든다: 지원하지 않는 URL 과, 있으면 alt 텍스트.
     *
     * @param url the URL / URL
     * @param alt the alt text or the empty string / alt 텍스트 또는 빈 문자열
     * @return the lines / 줄
     */
    private static String[] failureLines(String url, String alt) {
        if (alt.length() > 0) {
            return new String[] { UNSUPPORTED_URL + url, alt };
        }
        return new String[] { UNSUPPORTED_URL + url };
    }

    /**
     * Paints an <code>audio</code> element: a failure replacement, or the control bar when the element has <code>controls</code>.
     * <p>
     * <code>audio</code> 요소를 그린다: 실패 대체 그림, 또는 <code>controls</code> 가 있으면 컨트롤 막대.
     *
     * @param e the element / 요소
     * @param x content left / 콘텐츠 왼쪽
     * @param y content top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     */
    private void paintAudioElement(Element e, double x, double y, double w, double h) {
        String alt = e.getAttribute("alt") == null ? "" : e.getAttribute("alt");
        String src = e.getAttribute("src");
        // src 속성이 없으면 source 자식의 src 를 찾는다.
        if (src == null || src.trim().length() == 0) {
            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && "source".equals(((Element) n).getLocalName()) && ((Element) n).getAttribute("src") != null) {
                    src = ((Element) n).getAttribute("src");
                    break;
                }
            }
        }
        if (src == null || src.trim().length() == 0) {
            if (alt.length() > 0) {
                emit(new DisplayCommands.Placeholder(x, y, w, h, new String[] { alt }, DisplayCommands.KIND_BROKEN, e));
            }
            return;
        }
        String url = resolve(src.trim());
        ImageResource r = images == null ? null : images.getMedia(url);
        if (r != null && r.getState() == ImageResource.State.FAILED) {
            emit(new DisplayCommands.Placeholder(x, y, w, h, failureLines(url, alt), DisplayCommands.KIND_BROKEN, e));
        } else if (e.hasAttribute("controls")) {
            // 컨트롤 막대는 스크립트 호스트가 알려 주는 요소의 상태(재생 여부, 위치, 길이, 음량)를 보여 준다.
            MediaControlState state = images instanceof MediaControlSource ? ((MediaControlSource) images).getMediaControls(e) : null;
            emit(new DisplayCommands.Placeholder(x, y, w, h, new String[0], DisplayCommands.KIND_AUDIO, e, state));
        }
    }
}
