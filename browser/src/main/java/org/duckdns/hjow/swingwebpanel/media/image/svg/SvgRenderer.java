package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;

/**
 * Draws an SVG document with Java2D (plan item P18-04). One renderer object serves one drawing run: it walks the element tree, computes the
 * style of every element ({@link SvgStyle}), builds shapes ({@link SvgPath}), paints fills and strokes (solid colours, gradients and
 * patterns from {@link SvgPaintServers}), draws text ({@link SvgText}), markers ({@link SvgMarkers}) and embedded images, handles
 * <code>use</code>, <code>symbol</code>, nested <code>svg</code>, <code>switch</code>, and applies the group effects (opacity, clip paths,
 * masks, filters from {@link SvgFilters}, blend modes) through transparent layers. Only java.awt public API is used (no 3D acceleration, no
 * change of JVM-wide Java2D settings: rendering hints are set on the graphics objects of this run only). A budget of element visits and
 * layer depth, and the thread interrupt flag, stop hostile documents.
 * <p>
 * Java2D 로 SVG 문서를 그린다(계획 P18-04). 렌더러 객체 하나가 그리기 한 번을 맡는다: 요소 트리를 훑고, 요소마다 스타일({@link SvgStyle})을 계산하고,
 * 도형({@link SvgPath})을 만들고, 채우기와 선(단색, {@link SvgPaintServers} 의 그라디언트와 패턴)을 칠하고, 문자({@link SvgText}), 마커
 * ({@link SvgMarkers}), 포함된 이미지를 그리며, <code>use</code>, <code>symbol</code>, 중첩 <code>svg</code>, <code>switch</code> 를 처리하고,
 * 그룹 효과(투명도, 클립 경로, 마스크, {@link SvgFilters} 의 필터, 혼합 모드)를 투명 레이어로 적용한다. java.awt 공개 API 만 쓴다(3D 가속 없음, JVM
 * 전체의 Java2D 설정은 바꾸지 않으며 렌더링 힌트는 이번 그리기의 그래픽스 객체에만 지정한다). 요소 방문 수와 레이어 깊이의 예산과 스레드
 * 인터럽트 플래그가 악의적인 문서를 막는다.
 */
final class SvgRenderer {

    /**
     * Maximum number of element visits in one drawing run (includes the repeated visits of <code>use</code>).
     * <p>
     * 그리기 한 번에서 요소 방문의 최대 횟수(<code>use</code> 의 반복 방문 포함).
     */
    static final long MAX_VISITS = 2000000L;

    /**
     * Maximum nesting of transparent layers.
     * <p>
     * 투명 레이어의 최대 중첩.
     */
    static final int MAX_LAYER_DEPTH = 24;

    /**
     * Maximum depth of <code>use</code> and other element references while drawing.
     * <p>
     * 그리는 동안 <code>use</code> 와 기타 요소 참조의 최대 깊이.
     */
    static final int MAX_REFERENCE_DEPTH = 48;

    /**
     * Thrown inside a run to stop it; carries the media exception to report.
     * <p>
     * 그리기를 중단하기 위해 내부에서 던진다. 보고할 미디어 예외를 담고 있다.
     */
    static final class Abort extends RuntimeException {

        /**
         * Serialization version id.
         * <p>
         * 직렬화 버전 식별자.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The exception to report.
         * <p>
         * 보고할 예외.
         */
        final MediaException media;

        /**
         * Creates the abort signal.
         * <p>
         * 중단 신호를 만든다.
         *
         * @param media exception to report / 보고할 예외
         */
        Abort(MediaException media) {
            super(media.getMessage());
            this.media = media;
        }
    }

    /**
     * One transparent layer: an image, its graphics, and the graphics that it will be drawn onto.
     * <p>
     * 투명 레이어 하나: 이미지, 그 그래픽스, 그리고 나중에 그려질 부모 그래픽스.
     */
    static final class Layer {

        /**
         * The layer image (premultiplied ARGB).
         * <p>
         * 레이어 이미지(알파 미리 곱한 ARGB).
         */
        BufferedImage img;

        /**
         * The graphics of the layer image.
         * <p>
         * 레이어 이미지의 그래픽스.
         */
        Graphics2D g;

        /**
         * The graphics that was current before the layer began.
         * <p>
         * 레이어가 시작되기 전의 현재 그래픽스.
         */
        Graphics2D parentG;

        /**
         * The image that was current before the layer began.
         * <p>
         * 레이어가 시작되기 전의 현재 이미지.
         */
        BufferedImage parentImg;

        /**
         * The transform (user space to device) at the moment the layer began.
         * <p>
         * 레이어가 시작된 순간의 변환(사용자 공간 → 장치).
         */
        AffineTransform ctm;
    }

    /**
     * The document being drawn.
     * <p>
     * 그리는 문서.
     */
    final SvgImage doc;

    /**
     * The final image.
     * <p>
     * 최종 이미지.
     */
    private final BufferedImage canvas;

    /**
     * The width of the canvas in pixels.
     * <p>
     * 캔버스 너비(픽셀).
     */
    final int canvasW;

    /**
     * The height of the canvas in pixels.
     * <p>
     * 캔버스 높이(픽셀).
     */
    final int canvasH;

    /**
     * The graphics that currently receives the drawing (the canvas or a layer).
     * <p>
     * 현재 그리기를 받는 그래픽스(캔버스 또는 레이어).
     */
    Graphics2D g;

    /**
     * The image behind {@link #g}.
     * <p>
     * {@link #g} 의 이미지.
     */
    BufferedImage curImg;

    /**
     * The width of the viewport in effect (base of percentages along x).
     * <p>
     * 적용 중인 뷰포트 너비(x 방향 백분율의 기준).
     */
    double vpW;

    /**
     * The height of the viewport in effect.
     * <p>
     * 적용 중인 뷰포트 높이.
     */
    double vpH;

    /**
     * The number of element visits so far.
     * <p>
     * 지금까지의 요소 방문 수.
     */
    private long visits;

    /**
     * The number of layers in use.
     * <p>
     * 사용 중인 레이어 수.
     */
    private int layerDepth;

    /**
     * The elements that are being instantiated through <code>use</code>, patterns, markers, clip paths, masks and filters (cycle guard).
     * <p>
     * <code>use</code>, 패턴, 마커, 클립 경로, 마스크, 필터로 인스턴스화되고 있는 요소(순환 방지).
     */
    final Set<SvgElement> active = new HashSet<SvgElement>();

    /**
     * The factor that single-paint shapes multiply into their paint alpha instead of drawing a layer.
     * <p>
     * 단일 칠 도형이 레이어를 쓰는 대신 칠의 알파에 곱하는 계수.
     */
    double alphaScale = 1;

    /**
     * The fill that <code>context-fill</code> refers to (inside markers), or null.
     * <p>
     * (마커 안에서) <code>context-fill</code> 이 가리키는 채우기. 없으면 null.
     */
    SvgPaint contextFill;

    /**
     * The stroke that <code>context-stroke</code> refers to, or null.
     * <p>
     * <code>context-stroke</code> 가 가리키는 선. 없으면 null.
     */
    SvgPaint contextStroke;

    /**
     * Creates a renderer that draws a document into an image.
     * <p>
     * 문서를 이미지에 그리는 렌더러를 만든다.
     *
     * @param doc    the document / 문서
     * @param canvas the target image (premultiplied ARGB) / 대상 이미지(알파 미리 곱한 ARGB)
     */
    SvgRenderer(SvgImage doc, BufferedImage canvas) {
        this.doc = doc;
        this.canvas = canvas;
        this.canvasW = canvas.getWidth();
        this.canvasH = canvas.getHeight();
    }

    /**
     * Returns the limits of the run.
     * <p>
     * 이번 그리기의 제한을 반환한다.
     *
     * @return the limits / 제한
     */
    DecodeLimits limits() {
        return doc.limits;
    }

    /**
     * Creates a graphics object for an image with the quality hints of this renderer.
     * <p>
     * 이 렌더러의 품질 힌트를 지정한 이미지용 그래픽스 객체를 만든다.
     *
     * @param img the image / 이미지
     * @return the graphics / 그래픽스
     */
    Graphics2D createGraphics(BufferedImage img) {
        Graphics2D gg = img.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        gg.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        return gg;
    }

    /**
     * Counts one element visit and stops the run if the budget is used up or the thread was interrupted.
     * <p>
     * 요소 방문을 하나 세고 예산을 다 썼거나 스레드가 인터럽트되었으면 그리기를 중단한다.
     */
    void tick() {
        visits++;
        if (visits > MAX_VISITS) {
            throw new Abort(new MediaException(MediaException.Kind.TOO_LARGE, "the SVG document is too complex to draw"));
        }
        if ((visits & 255) == 0 && Thread.currentThread().isInterrupted()) {
            throw new Abort(new MediaException(MediaException.Kind.CANCELLED, "decoding was cancelled"));
        }
    }

    /**
     * Draws the whole document into the canvas. Errors of the limits and the interrupt are thrown as {@link MediaException}.
     * <p>
     * 문서 전체를 캔버스에 그린다. 제한 초과와 인터럽트는 {@link MediaException} 으로 던진다.
     *
     * @throws MediaException TOO_LARGE or CANCELLED / TOO_LARGE 또는 CANCELLED
     */
    void render() throws MediaException {
        g = createGraphics(canvas);
        curImg = canvas;
        try {
            SvgElement root = doc.root;
            double nw = doc.getNaturalWidth();
            double nh = doc.getNaturalHeight();
            double[] vb = doc.viewBox;
            double bx = vb != null ? vb[0] : 0;
            double by = vb != null ? vb[1] : 0;
            double bw = vb != null ? vb[2] : nw;
            double bh = vb != null ? vb[3] : nh;
            String par = root.attr("preserveAspectRatio");
            AffineTransform view = viewBoxTransform(bx, by, bw, bh, par, canvasW, canvasH);
            vpW = bw;
            vpH = bh;
            g.setClip(0, 0, canvasW, canvasH);
            SvgStyle base = new SvgStyle(bw, bh);
            SvgStyle rootStyle = new SvgStyle(base, root, bw, bh);
            paintRootBackground(rootStyle);
            g.transform(view);
            if (rootStyle.displayed() && conditionsPass(root)) {
                renderStyled(root, rootStyle, true);
            }
        } catch (Abort a) {
            throw a.media;
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to draw the SVG image");
        } finally {
            g.dispose();
        }
    }

    /**
     * Paints the <code>background-color</code> (or a colour <code>background</code>) of the root element over the whole canvas.
     * <p>
     * 루트 요소의 <code>background-color</code>(또는 색인 <code>background</code>)를 캔버스 전체에 칠한다.
     *
     * @param st the style of the root element / 루트 요소의 스타일
     */
    private void paintRootBackground(SvgStyle st) {
        String bg = st.own("background-color");
        if (bg == null) {
            bg = st.own("background");
        }
        if (bg == null) {
            return;
        }
        Integer c = SvgColors.parse(bg);
        if (c == null || (c.intValue() >>> 24) == 0) {
            return;
        }
        g.setColor(new Color(c.intValue(), true));
        g.fillRect(0, 0, canvasW, canvasH);
    }

    /**
     * Computes the transform that maps a view box onto a viewport of the given size (<code>preserveAspectRatio</code> rules).
     * <p>
     * viewBox 를 지정한 크기의 뷰포트에 대응시키는 변환을 계산한다(<code>preserveAspectRatio</code> 규칙).
     *
     * @param vbx  view box x / viewBox x
     * @param vby  view box y / viewBox y
     * @param vbw  view box width / viewBox 너비
     * @param vbh  view box height / viewBox 높이
     * @param par  the <code>preserveAspectRatio</code> value or null / <code>preserveAspectRatio</code> 값 또는 null
     * @param w    viewport width / 뷰포트 너비
     * @param h    viewport height / 뷰포트 높이
     * @return the transform / 변환
     */
    static AffineTransform viewBoxTransform(double vbx, double vby, double vbw, double vbh, String par, double w, double h) {
        String[] p = parseAspect(par);
        double sx = w / vbw;
        double sy = h / vbh;
        if ("none".equals(p[0])) {
            return new AffineTransform(sx, 0, 0, sy, -vbx * sx, -vby * sy);
        }
        double s = "slice".equals(p[1]) ? Math.max(sx, sy) : Math.min(sx, sy);
        double tx = -vbx * s;
        double ty = -vby * s;
        String align = p[0];
        // 정렬에 따라 남는 공간을 나눈다(Min 은 0, Mid 는 절반, Max 는 전부).
        if (align.indexOf("xMid") >= 0) {
            tx += (w - vbw * s) / 2;
        } else if (align.indexOf("xMax") >= 0) {
            tx += w - vbw * s;
        }
        if (align.indexOf("YMid") >= 0) {
            ty += (h - vbh * s) / 2;
        } else if (align.indexOf("YMax") >= 0) {
            ty += h - vbh * s;
        }
        return new AffineTransform(s, 0, 0, s, tx, ty);
    }

    /**
     * Parses a <code>preserveAspectRatio</code> value.
     * <p>
     * <code>preserveAspectRatio</code> 값을 해석한다.
     *
     * @param par the value or null / 값 또는 null
     * @return {alignment, meetOrSlice}; the defaults are <code>xMidYMid</code> and <code>meet</code> / {정렬, meet/slice}. 기본은 <code>xMidYMid</code>, <code>meet</code>
     */
    static String[] parseAspect(String par) {
        String align = "xMidYMid";
        String mode = "meet";
        if (par != null) {
            String[] t = par.trim().split("\\s+");
            int i = 0;
            if (i < t.length && "defer".equals(t[i])) {
                i++;
            }
            if (i < t.length && ("none".equals(t[i]) || t[i].matches("x(Min|Mid|Max)Y(Min|Mid|Max)"))) {
                align = t[i];
                i++;
                if (i < t.length && ("meet".equals(t[i]) || "slice".equals(t[i]))) {
                    mode = t[i];
                }
            }
        }
        return new String[] { align, mode };
    }

    /**
     * Looks up an element of the document by an id reference such as <code>#name</code> or <code>name</code>.
     * <p>
     * <code>#name</code> 이나 <code>name</code> 같은 id 참조로 문서의 요소를 찾는다.
     *
     * @param ref the reference / 참조
     * @return the element or null / 요소 또는 null
     */
    SvgElement lookup(String ref) {
        if (ref == null) {
            return null;
        }
        String id = ref.trim();
        if (id.startsWith("#")) {
            id = id.substring(1);
        } else if (id.indexOf('#') >= 0) {
            // 다른 문서를 가리키는 참조는 따르지 않는다.
            return null;
        }
        return doc.ids.get(id);
    }

    /**
     * Tells whether an element name is drawn directly when it appears in the tree (as opposed to definitions and non-graphical elements).
     * <p>
     * 요소 이름이 트리에 나타났을 때 직접 그려지는 것인지(정의나 그리지 않는 요소가 아닌지) 알려 준다.
     *
     * @param n the local name / 지역 이름
     * @return true if drawn / 그려지면 true
     */
    static boolean isGraphicalName(String n) {
        return "g".equals(n) || "svg".equals(n) || "a".equals(n) || "switch".equals(n) || "use".equals(n) || "rect".equals(n)
                || "circle".equals(n) || "ellipse".equals(n) || "line".equals(n) || "polyline".equals(n) || "polygon".equals(n)
                || "path".equals(n) || "text".equals(n) || "image".equals(n);
    }

    /**
     * Draws an element given the style of its parent.
     * <p>
     * 부모 스타일을 받아 요소를 그린다.
     *
     * @param e      the element / 요소
     * @param parent the parent style / 부모 스타일
     */
    void renderElement(SvgElement e, SvgStyle parent) {
        if (!e.isSvg() || !isGraphicalName(e.name)) {
            return;
        }
        tick();
        SvgStyle st = new SvgStyle(parent, e, vpW, vpH);
        if (!st.displayed() || !conditionsPass(e)) {
            return;
        }
        renderStyled(e, st, false);
    }

    /**
     * Tells whether the conditional processing attributes (<code>systemLanguage</code>, <code>requiredExtensions</code>) let the element
     * be drawn. The user language is the default locale of the JVM.
     * <p>
     * 조건부 처리 속성(<code>systemLanguage</code>, <code>requiredExtensions</code>)이 요소를 그리도록 허용하는지 알려 준다. 사용자 언어는 JVM 의 기본 로케일이다.
     *
     * @param e the element / 요소
     * @return true if the element may be drawn / 요소를 그려도 되면 true
     */
    boolean conditionsPass(SvgElement e) {
        if (e.attrs.containsKey("requiredExtensions")) {
            // 알 수 없는 확장이므로 조건을 통과하지 못한다.
            return false;
        }
        String sl = e.attrs.get("systemLanguage");
        if (sl != null) {
            String[] langs = sl.split(",");
            String user = userLanguage().toLowerCase(Locale.ENGLISH);
            boolean ok = false;
            // 목록의 언어 중 사용자 언어와 같거나 그 하위 언어(en-US 와 en)이면 통과한다.
            for (int i = 0; i < langs.length; i++) {
                String l = langs[i].trim().toLowerCase(Locale.ENGLISH);
                if (l.length() > 0 && (l.equals(user) || user.startsWith(l + "-") || l.startsWith(user + "-"))) {
                    ok = true;
                }
            }
            return ok;
        }
        return true;
    }

    /**
     * Returns the language tag that <code>systemLanguage</code> is compared with: the default locale of the JVM (for example
     * <code>en-US</code>).
     * <p>
     * <code>systemLanguage</code> 와 비교할 언어 태그를 반환한다: JVM 의 기본 로케일(예: <code>en-US</code>).
     *
     * @return the language tag / 언어 태그
     */
    static String userLanguage() {
        Locale l = Locale.getDefault();
        String c = l.getCountry();
        return c.length() == 0 ? l.getLanguage() : l.getLanguage() + "-" + c;
    }

    /**
     * Draws an element whose style is known: applies its transform and group effects and draws its content.
     * <p>
     * 스타일을 아는 요소를 그린다. 변환과 그룹 효과를 적용하고 내용을 그린다.
     *
     * @param e         the element / 요소
     * @param st        the style / 스타일
     * @param outermost true for the root svg element / 루트 svg 요소이면 true
     */
    void renderStyled(SvgElement e, SvgStyle st, boolean outermost) {
        AffineTransform saved = g.getTransform();
        double savedAlpha = alphaScale;
        try {
            AffineTransform t = transformOf(e, st);
            if (t == null) {
                return;
            }
            g.transform(t);
            Effects fx = effectsOf(e, st);
            if (!fx.needsLayer()) {
                renderContent(e, st, outermost);
            } else if (fx.onlyOpacity() && canScaleAlpha(e, st)) {
                alphaScale *= fx.opacity;
                renderContent(e, st, outermost);
            } else if (layerDepth >= MAX_LAYER_DEPTH) {
                // 레이어가 너무 깊으면 효과 없이 그린다.
                renderContent(e, st, outermost);
            } else {
                renderLayered(e, st, fx, outermost);
            }
        } finally {
            alphaScale = savedAlpha;
            g.setTransform(saved);
        }
    }

    /**
     * Tells whether an opacity can be applied by scaling the paint alpha: the element paints at most one of fill and stroke, so the result
     * is the same as drawing it into a layer.
     * <p>
     * 투명도를 칠의 알파에 곱해 적용해도 되는지 알려 준다: 요소가 채우기와 선 중 많아야 하나만 칠하므로 레이어에 그린 것과 결과가 같다.
     *
     * @param e  the element / 요소
     * @param st the style / 스타일
     * @return true if the alpha can be scaled / 알파를 곱해도 되면 true
     */
    private boolean canScaleAlpha(SvgElement e, SvgStyle st) {
        String n = e.name;
        boolean shape = "rect".equals(n) || "circle".equals(n) || "ellipse".equals(n) || "line".equals(n) || "polyline".equals(n)
                || "polygon".equals(n) || "path".equals(n);
        if (shape) {
            if (st.get("marker-start") != null || st.get("marker-mid") != null || st.get("marker-end") != null) {
                String ms = st.get("marker-start");
                String mm = st.get("marker-mid");
                String me = st.get("marker-end");
                if ((ms != null && !"none".equalsIgnoreCase(ms)) || (mm != null && !"none".equalsIgnoreCase(mm))
                        || (me != null && !"none".equalsIgnoreCase(me))) {
                    return false;
                }
            }
            SvgPaint fill = st.paint("fill");
            SvgPaint stroke = st.paint("stroke");
            SvgLength sw = st.length("stroke-width");
            boolean strokes = stroke.kind != SvgPaint.NONE && (sw == null || sw.value > 0);
            boolean fills = fill.kind != SvgPaint.NONE;
            return !(strokes && fills);
        }
        return "image".equals(n);
    }

    /**
     * The group effects of an element.
     * <p>
     * 요소의 그룹 효과.
     */
    static final class Effects {

        /**
         * The group opacity.
         * <p>
         * 그룹 투명도.
         */
        double opacity = 1;

        /**
         * The clip path element or null.
         * <p>
         * 클립 경로 요소. 없으면 null.
         */
        SvgElement clip;

        /**
         * The mask element or null.
         * <p>
         * 마스크 요소. 없으면 null.
         */
        SvgElement mask;

        /**
         * The filter elements (in order) and the CSS filter functions, or null.
         * <p>
         * 필터 요소(순서대로)와 CSS 필터 함수. 없으면 null.
         */
        List<Object> filters;

        /**
         * The blend mode keyword (lower case) or null for normal.
         * <p>
         * 혼합 모드 키워드(소문자). 보통이면 null.
         */
        String blend;

        /**
         * Tells whether any effect needs the element to be drawn into a layer.
         * <p>
         * 효과가 하나라도 있어 요소를 레이어에 그려야 하는지 알려 준다.
         *
         * @return true if a layer is needed / 레이어가 필요하면 true
         */
        boolean needsLayer() {
            return opacity < 1 || clip != null || mask != null || filters != null || blend != null;
        }

        /**
         * Tells whether opacity is the only effect.
         * <p>
         * 효과가 투명도뿐인지 알려 준다.
         *
         * @return true if only the opacity is set / 투명도만 있으면 true
         */
        boolean onlyOpacity() {
            return clip == null && mask == null && filters == null && blend == null;
        }
    }

    /**
     * Reads the group effects of an element from its style.
     * <p>
     * 요소의 스타일에서 그룹 효과를 읽는다.
     *
     * @param e  the element / 요소
     * @param st the style / 스타일
     * @return the effects / 효과
     */
    private Effects effectsOf(SvgElement e, SvgStyle st) {
        Effects fx = new Effects();
        fx.opacity = st.opacity("opacity", 1);
        String clipId = st.urlProperty("clip-path");
        if (clipId != null) {
            SvgElement c = doc.ids.get(clipId);
            if (c != null && c.is("clipPath")) {
                fx.clip = c;
            }
        }
        String maskId = st.urlProperty("mask");
        if (maskId != null) {
            SvgElement m = doc.ids.get(maskId);
            if (m != null && m.is("mask")) {
                fx.mask = m;
            }
        }
        String filter = st.own("filter");
        if (filter != null && !"none".equalsIgnoreCase(filter)) {
            fx.filters = SvgFilters.parseFilterList(this, filter, st);
            if (fx.filters != null && fx.filters.isEmpty()) {
                fx.filters = null;
            }
        }
        String blend = st.own("mix-blend-mode");
        if (blend != null && !"normal".equalsIgnoreCase(blend)) {
            fx.blend = blend.toLowerCase(Locale.ENGLISH);
        }
        return fx;
    }

    /**
     * Computes the transform of an element from the CSS <code>transform</code> property or the <code>transform</code> attribute, including
     * the <code>transform-origin</code> of the CSS form. An invalid value counts as no transform.
     * <p>
     * 요소의 변환을 CSS <code>transform</code> 속성이나 <code>transform</code> 속성에서 계산한다. CSS 형식의 <code>transform-origin</code> 을
     * 포함한다. 올바르지 않은 값은 변환이 없는 것으로 본다.
     *
     * @param e  the element / 요소
     * @param st the style / 스타일
     * @return the transform, or null if it is not invertible (the element is not drawn) / 변환. 역변환이 없으면 null(요소를 그리지 않음)
     */
    AffineTransform transformOf(SvgElement e, SvgStyle st) {
        AffineTransform t = null;
        String css = e.declared == null ? null : e.declared.get("transform");
        if (css != null && !"none".equalsIgnoreCase(css.trim())) {
            double[] origin = transformOrigin(e, st);
            t = SvgTransforms.parse(css, true, origin[2], origin[3]);
            if (t != null && (origin[0] != 0 || origin[1] != 0)) {
                AffineTransform full = AffineTransform.getTranslateInstance(origin[0], origin[1]);
                full.concatenate(t);
                full.translate(-origin[0], -origin[1]);
                t = full;
            }
        } else if (css == null) {
            String attr = e.attrs.get("transform");
            if (attr != null) {
                t = SvgTransforms.parse(attr, false, 0, 0);
            }
        }
        if (t == null) {
            return new AffineTransform();
        }
        double det = t.getDeterminant();
        if (det == 0 || Double.isNaN(det) || Double.isInfinite(det)) {
            return null;
        }
        return t;
    }

    /**
     * Computes the <code>transform-origin</code> in user space; the reference box is the view box or, for <code>transform-box</code>
     * <code>fill-box</code>/<code>stroke-box</code>, the bounding box of the element.
     * <p>
     * <code>transform-origin</code> 을 사용자 공간에서 계산한다. 기준 상자는 viewBox 이며, <code>transform-box</code> 가
     * <code>fill-box</code>/<code>stroke-box</code> 이면 요소의 경계 상자이다.
     *
     * @param e  the element / 요소
     * @param st the style / 스타일
     * @return {x, y, reference width, reference height} / {x, y, 기준 너비, 기준 높이}
     */
    private double[] transformOrigin(SvgElement e, SvgStyle st) {
        double bx = 0;
        double by = 0;
        double bw = vpW;
        double bh = vpH;
        String box = st.own("transform-box");
        if (box != null && (box.equalsIgnoreCase("fill-box") || box.equalsIgnoreCase("stroke-box"))) {
            Rectangle2D bb = SvgBounds.bbox(this, e, st);
            if (bb != null) {
                bx = bb.getX();
                by = bb.getY();
                bw = bb.getWidth();
                bh = bb.getHeight();
            }
        }
        String o = st.own("transform-origin");
        double ox = 0;
        double oy = 0;
        if (o == null && e.parent == null) {
            // 가장 바깥 svg 는 CSS 상자이므로 변환 기준점의 기본값이 상자의 가운데이다.
            ox = bw / 2;
            oy = bh / 2;
        }
        if (o != null) {
            String[] t = o.trim().toLowerCase(Locale.ENGLISH).split("\\s+");
            double[] v = new double[2];
            // 각 값은 키워드, 길이, 백분율일 수 있다.
            for (int i = 0; i < 2 && i < t.length; i++) {
                double base = i == 0 ? bw : bh;
                String w = t[i];
                if ("left".equals(w) || "top".equals(w)) {
                    v[i] = 0;
                } else if ("right".equals(w) || "bottom".equals(w)) {
                    v[i] = base;
                } else if ("center".equals(w)) {
                    v[i] = base / 2;
                } else {
                    SvgLength l = SvgLength.parse(w);
                    v[i] = l == null ? 0 : l.toPx(st.fontSize, base, vpW, vpH);
                }
            }
            if (t.length == 1) {
                v[1] = bh / 2;
                if ("top".equals(t[0]) || "bottom".equals(t[0])) {
                    v[1] = "top".equals(t[0]) ? 0 : bh;
                    v[0] = bw / 2;
                }
            }
            ox = v[0];
            oy = v[1];
        }
        return new double[] { bx + ox, by + oy, bw, bh };
    }

    /**
     * The styles of elements computed from their place in the document (used for definitions such as gradients, markers and masks, which
     * inherit from where they are defined, not from where they are used).
     * <p>
     * 문서에서의 위치로 계산한 요소의 스타일(그라디언트, 마커, 마스크처럼 사용하는 곳이 아니라 정의된 곳에서 상속하는 정의 요소용).
     */
    private final java.util.Map<SvgElement, SvgStyle> definitionStyles = new java.util.IdentityHashMap<SvgElement, SvgStyle>();

    /**
     * Returns the style of an element as it follows from its ancestors in the document.
     * <p>
     * 문서의 조상에서 이어지는 요소의 스타일을 반환한다.
     *
     * @param el the element, or null for the base style / 요소. 기본 스타일이면 null
     * @return the style / 스타일
     */
    SvgStyle styleFor(SvgElement el) {
        if (el == null) {
            return new SvgStyle(doc.viewBox != null ? doc.viewBox[2] : doc.getNaturalWidth(), doc.viewBox != null ? doc.viewBox[3] : doc.getNaturalHeight());
        }
        SvgStyle s = definitionStyles.get(el);
        if (s == null) {
            s = new SvgStyle(styleFor(el.parent), el, vpW, vpH);
            definitionStyles.put(el, s);
        }
        return s;
    }

    /**
     * Redirects the drawing to an existing image with a given transform until {@link #endLayer} is called.
     * <p>
     * {@link #endLayer} 를 호출할 때까지 그리기를 주어진 변환을 가진 기존 이미지로 돌린다.
     *
     * @param img the target image (premultiplied ARGB, canvas size) / 대상 이미지(알파 미리 곱한 ARGB, 캔버스 크기)
     * @param ctm the transform from user space to the image / 사용자 공간에서 이미지로의 변환
     * @return the layer record / 레이어 기록
     */
    Layer redirect(BufferedImage img, AffineTransform ctm) {
        Layer l = new Layer();
        l.parentG = g;
        l.parentImg = curImg;
        l.ctm = new AffineTransform(ctm);
        l.img = img;
        l.g = createGraphics(img);
        l.g.setTransform(ctm);
        g = l.g;
        curImg = img;
        layerDepth++;
        return l;
    }

    /**
     * Starts a transparent layer that has the current transform; the drawing then goes to the layer.
     * <p>
     * 현재 변환을 가진 투명 레이어를 시작한다. 이후의 그리기는 레이어로 간다.
     *
     * @return the layer / 레이어
     */
    Layer pushLayer() {
        Layer l = new Layer();
        l.parentG = g;
        l.parentImg = curImg;
        l.ctm = g.getTransform();
        l.img = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB_PRE);
        l.g = createGraphics(l.img);
        l.g.setTransform(l.ctm);
        l.g.setClip(g.getClip());
        g = l.g;
        curImg = l.img;
        layerDepth++;
        return l;
    }

    /**
     * Ends a layer without drawing it: the previous graphics become current again and the layer graphics is released.
     * <p>
     * 레이어를 그리지 않고 끝낸다. 이전 그래픽스가 다시 현재가 되고 레이어 그래픽스는 해제된다.
     *
     * @param l the layer / 레이어
     */
    void endLayer(Layer l) {
        g = l.parentG;
        curImg = l.parentImg;
        l.g.dispose();
        layerDepth--;
    }

    /**
     * Draws a finished layer onto the graphics that was current when it began, then ends it.
     * <p>
     * 끝난 레이어를 시작할 때의 현재 그래픽스에 그린 뒤 레이어를 끝낸다.
     *
     * @param l     the layer / 레이어
     * @param alpha the opacity of the layer / 레이어의 투명도
     * @param blend the blend mode keyword or null / 혼합 모드 키워드 또는 null
     */
    void compositeLayer(Layer l, double alpha, String blend) {
        endLayer(l);
        if (blend != null) {
            SvgFilters.blendLayer(curImg, l.img, blend, alpha);
            return;
        }
        AffineTransform saved = g.getTransform();
        java.awt.Composite oldComp = g.getComposite();
        g.setTransform(new AffineTransform());
        g.setComposite(alpha >= 1 ? AlphaComposite.SrcOver : AlphaComposite.SrcOver.derive((float) alpha));
        g.drawImage(l.img, 0, 0, null);
        g.setComposite(oldComp);
        g.setTransform(saved);
    }

    /**
     * Draws an element through a transparent layer and applies the effects in the order of the specification: filter, clip, mask,
     * opacity.
     * <p>
     * 요소를 투명 레이어에 그리고 규격의 순서(필터, 클립, 마스크, 투명도)로 효과를 적용한다.
     *
     * @param e         the element / 요소
     * @param st        the style / 스타일
     * @param fx        the effects / 효과
     * @param outermost true for the root svg element / 루트 svg 요소이면 true
     */
    private void renderLayered(SvgElement e, SvgStyle st, Effects fx, boolean outermost) {
        Layer l = pushLayer();
        boolean done = false;
        try {
            renderContent(e, st, outermost);
            if (fx.filters != null) {
                SvgFilters.apply(this, l.img, fx.filters, e, st);
            }
            if (fx.clip != null) {
                SvgMasks.applyClipPath(this, l.img, fx.clip, e, st, l.ctm);
            }
            if (fx.mask != null) {
                SvgMasks.applyMask(this, l.img, fx.mask, e, st, l.ctm);
            }
            done = true;
        } finally {
            if (done) {
                compositeLayer(l, fx.opacity, fx.blend);
            } else {
                endLayer(l);
            }
        }
    }

    /**
     * Draws the content of an element (not its transform or effects).
     * <p>
     * 요소의 내용을 그린다(변환과 효과는 제외).
     *
     * @param e         the element / 요소
     * @param st        the style / 스타일
     * @param outermost true for the root svg element / 루트 svg 요소이면 true
     */
    void renderContent(SvgElement e, SvgStyle st, boolean outermost) {
        String n = e.name;
        if ("g".equals(n) || "a".equals(n)) {
            renderChildren(e, st);
        } else if ("switch".equals(n)) {
            renderSwitch(e, st);
        } else if ("svg".equals(n)) {
            if (outermost) {
                renderChildren(e, st);
            } else {
                renderNestedSvg(e, st, Double.NaN, Double.NaN);
            }
        } else if ("use".equals(n)) {
            renderUse(e, st);
        } else if ("image".equals(n)) {
            renderImage(e, st);
        } else if ("text".equals(n)) {
            SvgText.render(this, e, st);
        } else {
            Shape shape = null;
            SvgPath path = null;
            if ("path".equals(n)) {
                path = pathOf(e, st);
                shape = path.shape;
            } else {
                shape = basicShape(e, st);
                if ("line".equals(n) || "polyline".equals(n) || "polygon".equals(n)) {
                    path = SvgPath.parse(pointsToPathData(e, st));
                }
            }
            if (shape != null) {
                paintGeometry(e, st, shape, path);
            }
        }
    }

    /**
     * Draws the child elements of a container in document order.
     * <p>
     * 컨테이너의 자식 요소를 문서 순서로 그린다.
     *
     * @param e  the container / 컨테이너
     * @param st the style of the container / 컨테이너의 스타일
     */
    void renderChildren(SvgElement e, SvgStyle st) {
        // 자식을 순서대로 그린다(뒤의 자식이 위에 그려진다).
        for (int i = 0; i < e.children.size(); i++) {
            renderElement(e.children.get(i), st);
        }
    }

    /**
     * Draws the first child of a <code>switch</code> element whose conditions pass.
     * <p>
     * <code>switch</code> 요소의 자식 중 조건을 통과하는 첫 번째를 그린다.
     *
     * @param e  the switch element / switch 요소
     * @param st the style of the element / 요소의 스타일
     */
    private void renderSwitch(SvgElement e, SvgStyle st) {
        // 그릴 수 있는 요소 중 조건을 통과하는 첫 자식만 그리고 멈춘다.
        for (int i = 0; i < e.children.size(); i++) {
            SvgElement c = e.children.get(i);
            if (c.isSvg() && isGraphicalName(c.name) && conditionsPass(c)) {
                renderElement(c, st);
                return;
            }
        }
    }

    /**
     * Draws a <code>use</code> element: the referenced element of the same document is drawn at an offset with the style of the
     * <code>use</code> element as its parent style.
     * <p>
     * <code>use</code> 요소를 그린다. 같은 문서에서 참조한 요소를 이동시켜, <code>use</code> 요소의 스타일을 부모 스타일로 삼아 그린다.
     *
     * @param use the use element / use 요소
     * @param st  its style / 그 스타일
     */
    private void renderUse(SvgElement use, SvgStyle st) {
        SvgElement target = lookup(use.href());
        if (target == null || active.contains(target) || active.size() > MAX_REFERENCE_DEPTH || isAncestorOrSelf(target, use)) {
            return;
        }
        double x = lengthAttr(use, "x", st, 0, 0);
        double y = lengthAttr(use, "y", st, 0, 1);
        SvgLength wl = geometryLength(use, "width");
        SvgLength hl = geometryLength(use, "height");
        double w = wl == null ? Double.NaN : st.px(wl, 0);
        double h = hl == null ? Double.NaN : st.px(hl, 1);
        AffineTransform saved = g.getTransform();
        active.add(target);
        try {
            g.translate(x, y);
            if (target.is("symbol")) {
                tick();
                SvgStyle ss = new SvgStyle(st, target, vpW, vpH);
                if (ss.displayed()) {
                    renderSymbol(target, ss, w, h);
                }
            } else if (target.is("svg")) {
                tick();
                SvgStyle ss = new SvgStyle(st, target, vpW, vpH);
                if (ss.displayed()) {
                    AffineTransform t2 = g.getTransform();
                    renderStyledNested(target, ss, w, h);
                    g.setTransform(t2);
                }
            } else {
                renderElement(target, st);
            }
        } finally {
            active.remove(target);
            g.setTransform(saved);
        }
    }

    /**
     * Draws a nested <code>svg</code> element that is the target of a <code>use</code>, with an optional size from the use element.
     * <p>
     * <code>use</code> 의 대상인 중첩 <code>svg</code> 요소를 그린다. use 요소가 크기를 줄 수도 있다.
     *
     * @param target the svg element / svg 요소
     * @param st     its style / 그 스타일
     * @param w      width override or NaN / 너비 재지정. 없으면 NaN
     * @param h      height override or NaN / 높이 재지정. 없으면 NaN
     */
    private void renderStyledNested(SvgElement target, SvgStyle st, double w, double h) {
        AffineTransform saved = g.getTransform();
        double savedAlpha = alphaScale;
        try {
            AffineTransform t = transformOf(target, st);
            if (t == null) {
                return;
            }
            g.transform(t);
            Effects fx = effectsOf(target, st);
            if (!fx.needsLayer() || layerDepth >= MAX_LAYER_DEPTH) {
                renderNestedSvg(target, st, w, h);
            } else {
                Layer l = pushLayer();
                boolean done = false;
                try {
                    renderNestedSvg(target, st, w, h);
                    if (fx.filters != null) {
                        SvgFilters.apply(this, l.img, fx.filters, target, st);
                    }
                    if (fx.clip != null) {
                        SvgMasks.applyClipPath(this, l.img, fx.clip, target, st, l.ctm);
                    }
                    if (fx.mask != null) {
                        SvgMasks.applyMask(this, l.img, fx.mask, target, st, l.ctm);
                    }
                    done = true;
                } finally {
                    if (done) {
                        compositeLayer(l, fx.opacity, fx.blend);
                    } else {
                        endLayer(l);
                    }
                }
            }
        } finally {
            alphaScale = savedAlpha;
            g.setTransform(saved);
        }
    }

    /**
     * Tells whether an element is the same as or an ancestor of another.
     * <p>
     * 요소가 다른 요소와 같거나 그 조상인지 알려 준다.
     *
     * @param a the possible ancestor / 조상일 수 있는 요소
     * @param b the element to start from / 시작 요소
     * @return true if a is b or an ancestor of b / a 가 b 이거나 b 의 조상이면 true
     */
    private static boolean isAncestorOrSelf(SvgElement a, SvgElement b) {
        // b 에서 루트까지 올라가며 a 를 찾는다.
        for (SvgElement p = b; p != null; p = p.parent) {
            if (p == a) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the length of a geometry property: the CSS declaration if there is one, else the attribute.
     * <p>
     * 기하 속성의 길이를 반환한다: CSS 선언이 있으면 그것, 없으면 속성.
     *
     * @param e    the element / 요소
     * @param name the property or attribute name / 속성 이름
     * @return the length or null / 길이 또는 null
     */
    static SvgLength geometryLength(SvgElement e, String name) {
        String v = e.declared == null ? null : e.declared.get(name);
        if (v == null) {
            v = e.attrs.get(name);
        }
        if (v == null) {
            return null;
        }
        return SvgLength.parse(v);
    }

    /**
     * Returns a length attribute in pixels, or a default if it is missing or invalid.
     * <p>
     * 길이 속성을 픽셀로 반환한다. 없거나 올바르지 않으면 기본값.
     *
     * @param e    the element / 요소
     * @param name the attribute name / 속성 이름
     * @param st   the style for units / 단위를 위한 스타일
     * @param def  the default / 기본값
     * @param axis 0 for x, 1 for y, 2 for the diagonal / 0 이면 x, 1 이면 y, 2 이면 대각선
     * @return pixels / 픽셀
     */
    static double lengthAttr(SvgElement e, String name, SvgStyle st, double def, int axis) {
        SvgLength l = geometryLength(e, name);
        return l == null ? def : st.px(l, axis);
    }

    /**
     * Draws a nested <code>svg</code> element: a new viewport at its x, y, width and height with its own view box.
     * <p>
     * 중첩 <code>svg</code> 요소를 그린다: x, y, width, height 의 새 뷰포트와 자체 viewBox 를 가진다.
     *
     * @param e  the svg element / svg 요소
     * @param st its style / 그 스타일
     * @param ow width override (from use) or NaN / (use 의) 너비 재지정. 없으면 NaN
     * @param oh height override or NaN / 높이 재지정. 없으면 NaN
     */
    void renderNestedSvg(SvgElement e, SvgStyle st, double ow, double oh) {
        double x = lengthAttr(e, "x", st, 0, 0);
        double y = lengthAttr(e, "y", st, 0, 1);
        SvgLength wl = geometryLength(e, "width");
        SvgLength hl = geometryLength(e, "height");
        double w = !Double.isNaN(ow) ? ow : (wl == null ? vpW : st.px(wl, 0));
        double h = !Double.isNaN(oh) ? oh : (hl == null ? vpH : st.px(hl, 1));
        renderViewport(e, st, x, y, w, h);
    }

    /**
     * Draws a <code>symbol</code> instantiated by <code>use</code>: its size comes from the use element (default 100%).
     * <p>
     * <code>use</code> 가 인스턴스화한 <code>symbol</code> 을 그린다. 크기는 use 요소에서 오며 기본값은 100% 이다.
     *
     * @param sym the symbol / symbol
     * @param st  its style (child of the use style) / 그 스타일(use 스타일의 자식)
     * @param ow  width from use or NaN / use 의 너비. 없으면 NaN
     * @param oh  height from use or NaN / use 의 높이. 없으면 NaN
     */
    private void renderSymbol(SvgElement sym, SvgStyle st, double ow, double oh) {
        SvgLength wl = geometryLength(sym, "width");
        SvgLength hl = geometryLength(sym, "height");
        double w = !Double.isNaN(ow) ? ow : (wl == null ? vpW : st.px(wl, 0));
        double h = !Double.isNaN(oh) ? oh : (hl == null ? vpH : st.px(hl, 1));
        double x = lengthAttr(sym, "x", st, 0, 0);
        double y = lengthAttr(sym, "y", st, 0, 1);
        renderViewport(sym, st, x, y, w, h);
    }

    /**
     * Establishes a viewport (nested svg or symbol), clips to it unless <code>overflow</code> is visible, and draws the children.
     * <p>
     * 뷰포트(중첩 svg 나 symbol)를 만들고 <code>overflow</code> 가 visible 이 아니면 그에 맞게 자르며 자식을 그린다.
     *
     * @param e  the svg or symbol element / svg 또는 symbol 요소
     * @param st its style / 그 스타일
     * @param x  viewport x / 뷰포트 x
     * @param y  viewport y / 뷰포트 y
     * @param w  viewport width / 뷰포트 너비
     * @param h  viewport height / 뷰포트 높이
     */
    private void renderViewport(SvgElement e, SvgStyle st, double x, double y, double w, double h) {
        if (!(w > 0) || !(h > 0)) {
            return;
        }
        double[] vb = SvgImage.parseViewBox(e.attr("viewBox"));
        AffineTransform saved = g.getTransform();
        double svW = vpW;
        double svH = vpH;
        String overflow = st.own("overflow");
        boolean clip = overflow == null || !(overflow.equalsIgnoreCase("visible") || overflow.equalsIgnoreCase("auto"));
        Layer layer = null;
        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);
        boolean ok = false;
        try {
            if (clip && !coversCanvas(rect)) {
                layer = pushLayer();
            }
            g.translate(x, y);
            if (vb != null) {
                g.transform(viewBoxTransform(vb[0], vb[1], vb[2], vb[3], e.attr("preserveAspectRatio"), w, h));
                vpW = vb[2];
                vpH = vb[3];
            } else {
                vpW = w;
                vpH = h;
            }
            renderChildren(e, st);
            ok = true;
        } finally {
            vpW = svW;
            vpH = svH;
            g.setTransform(saved);
            if (layer != null) {
                if (ok) {
                    SvgMasks.clipToShape(this, layer.img, rect, layer.ctm);
                    compositeLayer(layer, 1, null);
                } else {
                    endLayer(layer);
                }
            }
        }
    }

    /**
     * Tells whether a rectangle (in the current user space) covers the whole canvas, so that clipping to it changes nothing.
     * <p>
     * (현재 사용자 공간의) 사각형이 캔버스 전체를 덮어 그것으로 자르는 것이 아무것도 바꾸지 않는지 알려 준다.
     *
     * @param r the rectangle / 사각형
     * @return true if it covers the canvas / 캔버스를 덮으면 true
     */
    boolean coversCanvas(Rectangle2D r) {
        AffineTransform t = g.getTransform();
        if (t.getShearX() != 0 || t.getShearY() != 0) {
            return false;
        }
        Rectangle2D dev = t.createTransformedShape(r).getBounds2D();
        return dev.getMinX() <= 0 && dev.getMinY() <= 0 && dev.getMaxX() >= canvasW && dev.getMaxY() >= canvasH;
    }

    /**
     * Builds the Java2D shape of a basic shape element (<code>rect</code>, <code>circle</code>, <code>ellipse</code>, <code>line</code>,
     * <code>polyline</code>, <code>polygon</code>), or null if the element is not drawn (zero or invalid size).
     * <p>
     * 기본 도형 요소(<code>rect</code>, <code>circle</code>, <code>ellipse</code>, <code>line</code>, <code>polyline</code>, <code>polygon</code>)의
     * Java2D 도형을 만든다. 요소를 그리지 않는 경우(크기가 0 이거나 올바르지 않음)에는 null.
     *
     * @param e  the element / 요소
     * @param st its style / 스타일
     * @return the shape or null / 도형 또는 null
     */
    Shape basicShape(SvgElement e, SvgStyle st) {
        String n = e.name;
        if ("rect".equals(n)) {
            double x = lengthAttr(e, "x", st, 0, 0);
            double y = lengthAttr(e, "y", st, 0, 1);
            SvgLength wl = geometryLength(e, "width");
            SvgLength hl = geometryLength(e, "height");
            if (wl == null || hl == null) {
                return null;
            }
            double w = st.px(wl, 0);
            double h = st.px(hl, 1);
            if (!(w > 0) || !(h > 0)) {
                return null;
            }
            SvgLength rxl = geometryLength(e, "rx");
            SvgLength ryl = geometryLength(e, "ry");
            double rx = rxl == null || rxl.value < 0 ? Double.NaN : st.px(rxl, 0);
            double ry = ryl == null || ryl.value < 0 ? Double.NaN : st.px(ryl, 1);
            if (Double.isNaN(rx) && !Double.isNaN(ry)) {
                rx = ry;
            }
            if (Double.isNaN(ry) && !Double.isNaN(rx)) {
                ry = rx;
            }
            if (Double.isNaN(rx) || Double.isNaN(ry) || rx <= 0 || ry <= 0) {
                return new Rectangle2D.Double(x, y, w, h);
            }
            rx = Math.min(rx, w / 2);
            ry = Math.min(ry, h / 2);
            return roundRect(x, y, w, h, rx, ry);
        }
        if ("circle".equals(n)) {
            double cx = lengthAttr(e, "cx", st, 0, 0);
            double cy = lengthAttr(e, "cy", st, 0, 1);
            SvgLength rl = geometryLength(e, "r");
            double r = rl == null ? 0 : st.px(rl, 2);
            if (!(r > 0)) {
                return null;
            }
            return ellipsePath(cx, cy, r, r);
        }
        if ("ellipse".equals(n)) {
            double cx = lengthAttr(e, "cx", st, 0, 0);
            double cy = lengthAttr(e, "cy", st, 0, 1);
            SvgLength rxl = geometryLength(e, "rx");
            SvgLength ryl = geometryLength(e, "ry");
            double rx = rxl == null ? Double.NaN : st.px(rxl, 0);
            double ry = ryl == null ? Double.NaN : st.px(ryl, 1);
            if (Double.isNaN(rx)) {
                rx = ry;
            }
            if (Double.isNaN(ry)) {
                ry = rx;
            }
            if (Double.isNaN(rx) || !(rx > 0) || !(ry > 0)) {
                return null;
            }
            return ellipsePath(cx, cy, rx, ry);
        }
        if ("line".equals(n)) {
            double x1 = lengthAttr(e, "x1", st, 0, 0);
            double y1 = lengthAttr(e, "y1", st, 0, 1);
            double x2 = lengthAttr(e, "x2", st, 0, 0);
            double y2 = lengthAttr(e, "y2", st, 0, 1);
            return new Line2D.Double(x1, y1, x2, y2);
        }
        if ("polyline".equals(n) || "polygon".equals(n)) {
            double[] pts = pointsOf(e);
            if (pts.length < 4) {
                return null;
            }
            Path2D.Double p = new Path2D.Double();
            p.moveTo(pts[0], pts[1]);
            // 점을 차례로 이어 선분을 만든다.
            for (int i = 2; i + 1 < pts.length; i += 2) {
                p.lineTo(pts[i], pts[i + 1]);
            }
            if ("polygon".equals(n)) {
                p.closePath();
            }
            return p;
        }
        return null;
    }

    /**
     * Builds an ellipse as four cubic curves.
     * <p>
     * 타원을 삼차 곡선 네 개로 만든다.
     *
     * @param cx center x / 중심 x
     * @param cy center y / 중심 y
     * @param rx x radius / x 반지름
     * @param ry y radius / y 반지름
     * @return the path / 경로
     */
    static Path2D.Double ellipsePath(double cx, double cy, double rx, double ry) {
        final double k = 0.5522847498307936;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx + rx, cy);
        p.curveTo(cx + rx, cy + k * ry, cx + k * rx, cy + ry, cx, cy + ry);
        p.curveTo(cx - k * rx, cy + ry, cx - rx, cy + k * ry, cx - rx, cy);
        p.curveTo(cx - rx, cy - k * ry, cx - k * rx, cy - ry, cx, cy - ry);
        p.curveTo(cx + k * rx, cy - ry, cx + rx, cy - k * ry, cx + rx, cy);
        p.closePath();
        return p;
    }

    /**
     * Builds a rectangle with elliptical corners.
     * <p>
     * 모서리가 타원인 사각형을 만든다.
     *
     * @param x  left / 왼쪽
     * @param y  top / 위
     * @param w  width / 너비
     * @param h  height / 높이
     * @param rx corner x radius / 모서리 x 반지름
     * @param ry corner y radius / 모서리 y 반지름
     * @return the path / 경로
     */
    private static Path2D.Double roundRect(double x, double y, double w, double h, double rx, double ry) {
        final double k = 0.5522847498307936;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x + rx, y);
        p.lineTo(x + w - rx, y);
        p.curveTo(x + w - rx + k * rx, y, x + w, y + ry - k * ry, x + w, y + ry);
        p.lineTo(x + w, y + h - ry);
        p.curveTo(x + w, y + h - ry + k * ry, x + w - rx + k * rx, y + h, x + w - rx, y + h);
        p.lineTo(x + rx, y + h);
        p.curveTo(x + rx - k * rx, y + h, x, y + h - ry + k * ry, x, y + h - ry);
        p.lineTo(x, y + ry);
        p.curveTo(x, y + ry - k * ry, x + rx - k * rx, y, x + rx, y);
        p.closePath();
        return p;
    }

    /**
     * Reads the <code>points</code> attribute of a polyline or polygon; an invalid token gives an empty list (nothing is drawn, as in
     * browsers) and an odd count drops the last number.
     * <p>
     * polyline/polygon 의 <code>points</code> 속성을 읽는다. 올바르지 않은 토큰이 있으면 빈 목록(그리지 않음)이고 홀수 개이면 마지막 수를 버린다.
     *
     * @param e the element / 요소
     * @return the coordinates, x and y alternating / 좌표. x 와 y 가 번갈아 나온다
     */
    private static double[] pointsOf(SvgElement e) {
        String s = e.attrs.get("points");
        if (s == null) {
            return new double[0];
        }
        SvgScanner sc = new SvgScanner(s);
        List<Double> list = new ArrayList<Double>();
        sc.skipWsAndCommas();
        // 수를 하나씩 읽는다. 올바르지 않은 토큰이 있으면 브라우저처럼 도형을 그리지 않는다.
        while (!sc.atEnd()) {
            if (!sc.number()) {
                return new double[0];
            }
            list.add(Double.valueOf(sc.value));
            sc.skipWsAndCommas();
        }
        int n = list.size() - (list.size() % 2);
        double[] r = new double[n];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < n; i++) {
            r[i] = list.get(i).doubleValue();
        }
        return r;
    }

    /**
     * Converts the geometry of a line, polyline or polygon into path data (for markers).
     * <p>
     * 선분, 꺾은선, 다각형의 기하를 경로 데이터로 바꾼다(마커용).
     *
     * @param e  the element / 요소
     * @param st its style / 스타일
     * @return path data / 경로 데이터
     */
    private String pointsToPathData(SvgElement e, SvgStyle st) {
        StringBuilder sb = new StringBuilder();
        if ("line".equals(e.name)) {
            sb.append("M").append(lengthAttr(e, "x1", st, 0, 0)).append(' ').append(lengthAttr(e, "y1", st, 0, 1));
            sb.append("L").append(lengthAttr(e, "x2", st, 0, 0)).append(' ').append(lengthAttr(e, "y2", st, 0, 1));
            return sb.toString();
        }
        double[] pts = pointsOf(e);
        // 점마다 M 또는 L 명령을 붙인다.
        for (int i = 0; i + 1 < pts.length; i += 2) {
            sb.append(i == 0 ? "M" : "L").append(pts[i]).append(' ').append(pts[i + 1]);
        }
        if ("polygon".equals(e.name) && pts.length >= 4) {
            sb.append('Z');
        }
        return sb.toString();
    }

    /**
     * Returns the path of a <code>path</code> element: the <code>d</code> attribute, or a CSS <code>d: path("...")</code> declaration.
     * <p>
     * <code>path</code> 요소의 경로를 반환한다: <code>d</code> 속성 또는 CSS <code>d: path("...")</code> 선언.
     *
     * @param e  the element / 요소
     * @param st its style / 스타일
     * @return the path (possibly empty) / 경로(비어 있을 수 있음)
     */
    SvgPath pathOf(SvgElement e, SvgStyle st) {
        String d = e.attrs.get("d");
        String css = e.declared == null ? null : e.declared.get("d");
        if (css != null) {
            String t = css.trim();
            if (t.toLowerCase(Locale.ENGLISH).startsWith("path(") && t.endsWith(")")) {
                String inner = t.substring(5, t.length() - 1).trim();
                if (inner.length() >= 2 && (inner.charAt(0) == '"' || inner.charAt(0) == '\'')) {
                    d = inner.substring(1, inner.length() - 1);
                }
            } else if ("none".equalsIgnoreCase(t)) {
                d = null;
            }
        }
        return SvgPath.parse(d);
    }

    /**
     * Builds the stroke of a style, or null if nothing is stroked (width zero).
     * <p>
     * 스타일의 선(stroke)을 만든다. 칠할 것이 없으면(너비 0) null.
     *
     * @param st the style / 스타일
     * @return the stroke or null / 선 또는 null
     */
    BasicStroke strokeOf(SvgStyle st) {
        SvgLength wl = st.length("stroke-width");
        double w = wl == null ? 1 : st.px(wl, 2);
        if (!(w > 0) || Double.isInfinite(w)) {
            return null;
        }
        String capK = st.keyword("stroke-linecap");
        int cap = BasicStroke.CAP_BUTT;
        if ("round".equals(capK)) {
            cap = BasicStroke.CAP_ROUND;
        } else if ("square".equals(capK)) {
            cap = BasicStroke.CAP_SQUARE;
        }
        String joinK = st.keyword("stroke-linejoin");
        int join = BasicStroke.JOIN_MITER;
        if ("round".equals(joinK)) {
            join = BasicStroke.JOIN_ROUND;
        } else if ("bevel".equals(joinK)) {
            join = BasicStroke.JOIN_BEVEL;
        }
        double ml = st.number("stroke-miterlimit", 4);
        if (ml < 1) {
            ml = 4;
        }
        float[] dash = st.dashArray();
        float phase = 0;
        if (dash != null) {
            SvgLength off = st.length("stroke-dashoffset");
            double o = off == null ? 0 : st.px(off, 2);
            double total = 0;
            // 점선 한 주기의 길이를 구해 시작 위치를 한 주기 안으로 맞춘다.
            for (int i = 0; i < dash.length; i++) {
                total += dash[i];
            }
            o = ((o % total) + total) % total;
            phase = (float) o;
            return new BasicStroke((float) w, cap, join, (float) ml, dash, phase);
        }
        return new BasicStroke((float) w, cap, join, (float) ml);
    }

    /**
     * Paints the fill, stroke and markers of a geometry element in the order of <code>paint-order</code>.
     * <p>
     * 기하 요소의 채우기, 선, 마커를 <code>paint-order</code> 순서로 칠한다.
     *
     * @param e     the element / 요소
     * @param st    its style / 스타일
     * @param shape the geometry in user space / 사용자 공간의 기하
     * @param path  the path with segments for markers, or null / 마커를 위한 구간을 가진 경로. 없으면 null
     */
    void paintGeometry(SvgElement e, SvgStyle st, Shape shape, SvgPath path) {
        Rectangle2D b = SvgBounds.shapeBounds(shape);
        if (b == null) {
            return;
        }
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        String sr = st.keyword("shape-rendering");
        if ("crispedges".equals(sr) || "optimizespeed".equals(sr)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        try {
            String po = st.keyword("paint-order");
            String[] order = paintOrder(po);
            // 정해진 순서(채우기, 선, 마커)로 칠한다.
            for (int i = 0; i < order.length; i++) {
                if (!st.visible() && !"markers".equals(order[i])) {
                    continue;
                }
                if ("fill".equals(order[i])) {
                    fillGeometry(shape, st, b);
                } else if ("stroke".equals(order[i])) {
                    strokeGeometry(shape, st, b);
                } else if (path != null) {
                    SvgMarkers.draw(this, e, st, path);
                }
            }
        } finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    /**
     * Resolves the <code>paint-order</code> keyword list to the three painting steps.
     * <p>
     * <code>paint-order</code> 키워드 목록을 세 칠하기 단계의 순서로 바꾼다.
     *
     * @param po the lower-case property value or null / 소문자 속성 값. 없으면 null
     * @return the order of "fill", "stroke" and "markers" / "fill", "stroke", "markers" 의 순서
     */
    private static String[] paintOrder(String po) {
        if (po == null || "normal".equals(po)) {
            return new String[] { "fill", "stroke", "markers" };
        }
        String[] t = po.trim().split("\\s+");
        List<String> r = new ArrayList<String>();
        // 지정된 키워드를 먼저 넣고 나머지는 기본 순서로 뒤에 붙인다.
        for (int i = 0; i < t.length; i++) {
            if (("fill".equals(t[i]) || "stroke".equals(t[i]) || "markers".equals(t[i])) && !r.contains(t[i])) {
                r.add(t[i]);
            }
        }
        String[] def = { "fill", "stroke", "markers" };
        // 기본 순서에서 빠진 단계를 뒤에 붙인다.
        for (int i = 0; i < def.length; i++) {
            if (!r.contains(def[i])) {
                r.add(def[i]);
            }
        }
        return r.toArray(new String[3]);
    }

    /**
     * Paints the fill of a geometry.
     * <p>
     * 기하의 채우기를 칠한다.
     *
     * @param shape the geometry / 기하
     * @param st    the style / 스타일
     * @param bbox  the bounding box of the geometry / 기하의 경계 상자
     */
    void fillGeometry(Shape shape, SvgStyle st, Rectangle2D bbox) {
        SvgPaint fill = st.paint("fill");
        if (fill.kind == SvgPaint.NONE) {
            return;
        }
        Shape s = shape;
        if (shape instanceof Path2D) {
            boolean evenOdd = "evenodd".equals(st.keyword("fill-rule"));
            Path2D.Double copy = new Path2D.Double(shape);
            copy.setWindingRule(evenOdd ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
            s = copy;
        }
        paintShape(s, fill, st.opacity("fill-opacity", 1), st, bbox);
    }

    /**
     * Paints the stroke of a geometry.
     * <p>
     * 기하의 선을 칠한다.
     *
     * @param shape the geometry / 기하
     * @param st    the style / 스타일
     * @param bbox  the bounding box of the geometry / 기하의 경계 상자
     */
    void strokeGeometry(Shape shape, SvgStyle st, Rectangle2D bbox) {
        SvgPaint stroke = st.paint("stroke");
        if (stroke.kind == SvgPaint.NONE) {
            return;
        }
        BasicStroke bs = strokeOf(st);
        if (bs == null) {
            return;
        }
        Shape outline;
        String ve = st.keyword("vector-effect");
        if (ve != null && ve.indexOf("non-scaling-stroke") >= 0) {
            // 배율에 영향받지 않는 선: 장치 좌표에서 선을 만든 뒤 사용자 좌표로 되돌린다.
            AffineTransform ctm = g.getTransform();
            try {
                AffineTransform inv = ctm.createInverse();
                Shape dev = ctm.createTransformedShape(shape);
                outline = inv.createTransformedShape(bs.createStrokedShape(dev));
            } catch (NoninvertibleTransformException ex) {
                return;
            }
        } else {
            outline = bs.createStrokedShape(shape);
        }
        paintShape(outline, stroke, st.opacity("stroke-opacity", 1), st, bbox);
    }

    /**
     * Fills a shape with a paint (colour, gradient, pattern or context paint).
     * <p>
     * 도형을 페인트(색, 그라디언트, 패턴, 컨텍스트 페인트)로 채운다.
     *
     * @param shape   the shape in user space / 사용자 공간의 도형
     * @param paint   the paint / 페인트
     * @param opacity the fill or stroke opacity / 채우기나 선의 투명도
     * @param st      the style of the element (for units and colours) / 요소의 스타일(단위와 색용)
     * @param bbox    the bounding box of the element for objectBoundingBox units / objectBoundingBox 단위를 위한 요소의 경계 상자
     */
    void paintShape(Shape shape, SvgPaint paint, double opacity, SvgStyle st, Rectangle2D bbox) {
        double op = opacity * alphaScale;
        if (op <= 0) {
            return;
        }
        SvgPaint p = paint;
        if (p.kind == SvgPaint.CONTEXT_FILL) {
            p = contextFill == null ? SvgPaint.NONE_PAINT : contextFill;
        } else if (p.kind == SvgPaint.CONTEXT_STROKE) {
            p = contextStroke == null ? SvgPaint.NONE_PAINT : contextStroke;
        }
        if (p.kind == SvgPaint.NONE) {
            return;
        }
        if (p.kind == SvgPaint.COLOR) {
            int a = (int) Math.round((p.argb >>> 24) * op);
            if (a <= 0) {
                return;
            }
            g.setColor(new Color((p.argb & 0xFFFFFF) | (Math.min(255, a) << 24), true));
            g.fill(shape);
            return;
        }
        if (p.kind == SvgPaint.URL) {
            SvgElement server = lookup(p.ref);
            if (server == null || !(server.is("linearGradient") || server.is("radialGradient") || server.is("pattern"))) {
                if (p.fallback != null) {
                    paintShape(shape, p.fallback, opacity, st, bbox);
                }
                return;
            }
            if (server.is("pattern")) {
                SvgPaintServers.fillPattern(this, server, shape, op, st, bbox);
                return;
            }
            Paint gp = SvgPaintServers.gradient(this, server, st, bbox, p.fallback);
            if (gp == null) {
                return;
            }
            java.awt.Composite old = g.getComposite();
            if (op < 1) {
                g.setComposite(AlphaComposite.SrcOver.derive((float) op));
            }
            g.setPaint(gp);
            g.fill(shape);
            g.setComposite(old);
        }
    }

    /**
     * Draws an <code>image</code> element: only <code>data:</code> URIs are decoded (external addresses are not loaded); the image is
     * fitted into the x, y, width, height rectangle by <code>preserveAspectRatio</code>.
     * <p>
     * <code>image</code> 요소를 그린다. <code>data:</code> URI 만 디코딩하고(외부 주소는 읽지 않음) 이미지를 x, y, width, height 사각형에
     * <code>preserveAspectRatio</code> 에 따라 맞춘다.
     *
     * @param e  the element / 요소
     * @param st its style / 스타일
     */
    private void renderImage(SvgElement e, SvgStyle st) {
        if (!st.visible()) {
            return;
        }
        String href = e.href();
        if (href == null) {
            return;
        }
        byte[] data = dataUri(href.trim());
        if (data == null) {
            return;
        }
        DecodedImage img;
        try {
            img = ImageDecoders.decode(data, dataUriMime(href.trim()), limits());
        } catch (MediaException ex) {
            if (ex.getKind() == MediaException.Kind.CANCELLED) {
                throw new Abort(ex);
            }
            return;
        }
        double iw = img.getWidth();
        double ih = img.getHeight();
        double x = lengthAttr(e, "x", st, 0, 0);
        double y = lengthAttr(e, "y", st, 0, 1);
        SvgLength wl = geometryLength(e, "width");
        SvgLength hl = geometryLength(e, "height");
        double w = wl == null ? Double.NaN : st.px(wl, 0);
        double h = hl == null ? Double.NaN : st.px(hl, 1);
        // width/height 가 없으면(auto) 이미지 크기나 비율로 정한다.
        if (Double.isNaN(w) && Double.isNaN(h)) {
            w = iw;
            h = ih;
        } else if (Double.isNaN(w)) {
            w = h * iw / ih;
        } else if (Double.isNaN(h)) {
            h = w * ih / iw;
        }
        if (!(w > 0) || !(h > 0)) {
            return;
        }
        DecodedFrame f = img.getFrames().get(0);
        BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] px = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        System.arraycopy(f.getArgb(), 0, px, 0, px.length);
        AffineTransform fit = viewBoxTransform(0, 0, iw, ih, e.attr("preserveAspectRatio"), w, h);
        AffineTransform saved = g.getTransform();
        java.awt.Shape oldClip = g.getClip();
        Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        try {
            g.translate(x, y);
            String ir = st.keyword("image-rendering");
            if ("pixelated".equals(ir) || "crisp-edges".equals(ir) || "optimizespeed".equals(ir)) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }
            String[] par = parseAspect(e.attr("preserveAspectRatio"));
            if ("slice".equals(par[1]) || coversPartially(fit, iw, ih, w, h)) {
                g.clip(new Rectangle2D.Double(0, 0, w, h));
            }
            AffineTransform t = new AffineTransform(g.getTransform());
            t.concatenate(fit);
            double alpha = alphaScale;
            java.awt.Composite oldComp = g.getComposite();
            if (alpha < 1) {
                g.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
            }
            g.setTransform(t);
            g.drawImage(bi, 0, 0, null);
            g.setComposite(oldComp);
        } finally {
            g.setTransform(saved);
            g.setClip(oldClip);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        }
    }

    /**
     * Tells whether a fitted image extends beyond the viewport rectangle (so it needs a clip).
     * <p>
     * 맞춘 이미지가 뷰포트 사각형 밖으로 나가는지(그래서 자르기가 필요한지) 알려 준다.
     *
     * @param fit the fit transform / 맞춤 변환
     * @param iw  image width / 이미지 너비
     * @param ih  image height / 이미지 높이
     * @param w   viewport width / 뷰포트 너비
     * @param h   viewport height / 뷰포트 높이
     * @return true if it extends beyond / 벗어나면 true
     */
    private static boolean coversPartially(AffineTransform fit, double iw, double ih, double w, double h) {
        Rectangle2D r = fit.createTransformedShape(new Rectangle2D.Double(0, 0, iw, ih)).getBounds2D();
        return r.getMinX() < -1e-6 || r.getMinY() < -1e-6 || r.getMaxX() > w + 1e-6 || r.getMaxY() > h + 1e-6;
    }

    /**
     * Decodes the data of a <code>data:</code> URI (base64 or percent-encoded); any other address returns null.
     * <p>
     * <code>data:</code> URI 의 데이터를 디코딩한다(base64 또는 퍼센트 인코딩). 다른 주소는 null 을 반환한다.
     *
     * @param uri the address / 주소
     * @return the bytes or null / 바이트 또는 null
     */
    static byte[] dataUri(String uri) {
        if (!uri.regionMatches(true, 0, "data:", 0, 5)) {
            return null;
        }
        int comma = uri.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String meta = uri.substring(5, comma).toLowerCase(Locale.ENGLISH);
        String body = uri.substring(comma + 1);
        if (meta.endsWith(";base64")) {
            return base64(body);
        }
        // 퍼센트 인코딩을 바이트로 되돌린다.
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '%' && i + 2 < body.length() + 0 && Character.digit(body.charAt(i + 1), 16) >= 0 && Character.digit(body.charAt(i + 2), 16) >= 0) {
                out.write(Character.digit(body.charAt(i + 1), 16) * 16 + Character.digit(body.charAt(i + 2), 16));
                i += 2;
            } else if (c < 128) {
                out.write(c);
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes("UTF-8");
                } catch (java.io.UnsupportedEncodingException ex) {
                    return null;
                }
                out.write(b, 0, b.length);
            }
        }
        return out.toByteArray();
    }

    /**
     * Returns the MIME type declared in a <code>data:</code> URI, or null.
     * <p>
     * <code>data:</code> URI 에 선언된 MIME 타입을 반환하며 없으면 null 이다.
     *
     * @param uri the address / 주소
     * @return the MIME type or null / MIME 타입 또는 null
     */
    private static String dataUriMime(String uri) {
        int comma = uri.indexOf(',');
        if (comma < 5) {
            return null;
        }
        String meta = uri.substring(5, comma);
        int semi = meta.indexOf(';');
        String mime = semi < 0 ? meta : meta.substring(0, semi);
        return mime.length() == 0 ? null : mime;
    }

    /**
     * Decodes base64 text (white space ignored, padding optional) without any Java 7 API.
     * <p>
     * base64 텍스트를 Java 7 API 없이 디코딩한다(공백은 무시, 패딩은 선택).
     *
     * @param s the text / 텍스트
     * @return the bytes or null if the text is not base64 / 바이트. base64 가 아니면 null
     */
    static byte[] base64(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int acc = 0;
        int bits = 0;
        // 문자마다 6비트씩 모아 8비트가 차면 바이트를 내보낸다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+' || c == '-') {
                v = 62;
            } else if (c == '/' || c == '_') {
                v = 63;
            } else if (c == '=') {
                break;
            } else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                continue;
            } else if (c == '%') {
                // 퍼센트 인코딩된 base64 문자(%2B 등)는 지원하지 않는다.
                return null;
            } else {
                return null;
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((acc >> bits) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    /**
     * Converts a Java2D path iterator of a shape into a flattened list of points (for bounds and markers).
     * <p>
     * 도형의 Java2D 경로 반복자를 평탄화한 점 목록으로 바꾼다(경계와 마커용).
     *
     * @param s the shape / 도형
     * @return a flat path / 평탄화된 경로
     */
    static GeneralPath flatten(Shape s) {
        GeneralPath out = new GeneralPath();
        PathIterator it = s.getPathIterator(null, 0.05);
        out.append(it, false);
        return out;
    }

    /**
     * Returns the canvas image (for filters that need the backdrop).
     * <p>
     * 캔버스 이미지를 반환한다(배경이 필요한 필터용).
     *
     * @return the canvas / 캔버스
     */
    BufferedImage canvas() {
        return canvas;
    }
}
