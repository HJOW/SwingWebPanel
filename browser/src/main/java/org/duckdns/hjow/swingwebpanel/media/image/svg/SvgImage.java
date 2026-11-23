package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * A parsed SVG document that can be drawn at any size (plan item P18-04; FEATURE_MATRIX IMG-07). {@link #parse} reads the XML with the
 * project's own parser, runs the CSS cascade and computes the natural size; {@link #render} draws the document with Java2D into a box of a
 * requested size (so the image stays sharp when it is shown larger than its natural size), and {@link #toDecodedImage} rasterizes it at the
 * natural size for the common image pipeline. The document is immutable after parsing and can be drawn from several threads, each draw
 * uses its own state. Nothing external is loaded: only <code>data:</code> URIs are followed for images, <code>use</code> and paint servers
 * refer to elements of the same document, scripts are never run. The natural size follows the rules of browsers for an SVG used as an
 * image: absolute <code>width</code>/<code>height</code> lengths, otherwise derived from the <code>viewBox</code> ratio, otherwise
 * 300x150 (contained), rounded to whole pixels.
 * <p>
 * 임의의 크기로 그릴 수 있는 해석된 SVG 문서(계획 P18-04; FEATURE_MATRIX IMG-07). {@link #parse} 는 자체 파서로 XML 을 읽고 CSS 연쇄를 실행하며
 * 자연 크기를 계산한다. {@link #render} 는 요청한 크기의 상자에 Java2D 로 문서를 그리므로(자연 크기보다 크게 보여도 선명함) 공통 이미지 처리 경로에는
 * {@link #toDecodedImage} 가 자연 크기로 래스터화한다. 해석한 뒤에는 문서가 바뀌지 않으며 여러 스레드가 그릴 수 있고 그릴 때마다 자기 상태를 쓴다.
 * 외부 자원은 읽지 않는다: 이미지는 <code>data:</code> URI 만 따라가고, <code>use</code> 와 페인트 서버는 같은 문서의 요소만 참조하며, 스크립트는
 * 실행하지 않는다. 자연 크기는 이미지로 쓰인 SVG 에 대한 브라우저의 규칙을 따른다: 절대 길이의 <code>width</code>/<code>height</code>, 없으면
 * <code>viewBox</code> 비율로 유도, 그것도 없으면 300x150(비율 유지), 결과는 정수 픽셀로 반올림한다.
 */
public final class SvgImage {

    /**
     * Default width of an SVG without any size information, in pixels.
     * <p>
     * 크기 정보가 전혀 없는 SVG 의 기본 너비(픽셀).
     */
    static final double DEFAULT_WIDTH = 300;

    /**
     * Default height of an SVG without any size information, in pixels.
     * <p>
     * 크기 정보가 전혀 없는 SVG 의 기본 높이(픽셀).
     */
    static final double DEFAULT_HEIGHT = 150;

    /**
     * Maximum nesting of SVG images inside <code>image</code> elements (data URIs inside data URIs).
     * <p>
     * <code>image</code> 요소 안에 SVG 이미지가 중첩되는 최대 깊이(data URI 안의 data URI).
     */
    static final int MAX_IMAGE_NESTING = 3;

    /**
     * The nesting depth of SVG rendering in the current thread (images inside images).
     * <p>
     * 현재 스레드에서 SVG 그리기의 중첩 깊이(이미지 안의 이미지).
     */
    static final ThreadLocal<Integer> NESTING = new ThreadLocal<Integer>();

    /**
     * The root element.
     * <p>
     * 루트 요소.
     */
    final SvgElement root;

    /**
     * The elements by id (the first element wins for duplicate ids).
     * <p>
     * id 별 요소(id 가 중복이면 첫 요소가 쓰인다).
     */
    final Map<String, SvgElement> ids = new HashMap<String, SvgElement>();

    /**
     * The limits that apply to drawing.
     * <p>
     * 그리기에 적용되는 제한.
     */
    final DecodeLimits limits;

    /**
     * The natural width in CSS pixels (not rounded).
     * <p>
     * CSS 픽셀 단위의 자연 너비(반올림 전).
     */
    private final double naturalWidth;

    /**
     * The natural height in CSS pixels (not rounded).
     * <p>
     * CSS 픽셀 단위의 자연 높이(반올림 전).
     */
    private final double naturalHeight;

    /**
     * The view box of the root element as {x, y, width, height}, or null.
     * <p>
     * 루트 요소의 viewBox({x, y, 너비, 높이}). 없으면 null.
     */
    final double[] viewBox;

    /**
     * Tells whether the size comes from width and height lengths or the viewBox (false for the 300x150 default).
     * <p>
     * 크기가 width/height 길이나 viewBox 에서 왔는지(300x150 기본값이면 false) 알려 준다.
     */
    private final boolean sizeKnown;

    /**
     * Creates a document from a parsed tree.
     * <p>
     * 해석된 트리로 문서를 만든다.
     *
     * @param root   the root element / 루트 요소
     * @param limits the limits / 제한
     * @throws MediaException CORRUPT if the root is not an SVG element or has no usable size / 루트가 SVG 요소가 아니거나 쓸 수 있는 크기가 없을 때 CORRUPT
     */
    private SvgImage(SvgElement root, DecodeLimits limits) throws MediaException {
        this.root = root;
        this.limits = limits;
        if (!root.is("svg")) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the root element is not an svg element in the SVG namespace");
        }
        index(root);
        SvgStyle base = new SvgStyle(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        SvgLength w = sizeAttribute(root.attr("width"));
        SvgLength h = sizeAttribute(root.attr("height"));
        double[] vb = parseViewBox(root.attr("viewBox"));
        this.viewBox = vb;
        double wpx = w == null ? Double.NaN : w.toPx(base.fontSize, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        double hpx = h == null ? Double.NaN : h.toPx(base.fontSize, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        double ratio = vb != null ? vb[2] / vb[3] : (!Double.isNaN(wpx) && !Double.isNaN(hpx) && hpx > 0 ? wpx / hpx : Double.NaN);
        boolean known = !Double.isNaN(wpx) || !Double.isNaN(hpx) || vb != null;
        // 크기 결정: 두 길이가 모두 있으면 그대로, 하나만 있으면 비율로, 없으면 기본 상자 안에 비율을 맞춘다.
        if (Double.isNaN(wpx) && Double.isNaN(hpx)) {
            if (Double.isNaN(ratio)) {
                wpx = DEFAULT_WIDTH;
                hpx = DEFAULT_HEIGHT;
            } else if (ratio >= DEFAULT_WIDTH / DEFAULT_HEIGHT) {
                wpx = DEFAULT_WIDTH;
                hpx = DEFAULT_WIDTH / ratio;
            } else {
                hpx = DEFAULT_HEIGHT;
                wpx = DEFAULT_HEIGHT * ratio;
            }
        } else if (Double.isNaN(wpx)) {
            wpx = Double.isNaN(ratio) ? DEFAULT_WIDTH : hpx * ratio;
        } else if (Double.isNaN(hpx)) {
            hpx = Double.isNaN(ratio) ? DEFAULT_HEIGHT : wpx / ratio;
        }
        this.naturalWidth = wpx;
        this.naturalHeight = hpx;
        this.sizeKnown = known;
        SvgCss.cascade(root, new SvgCss.MediaEnv(wpx, hpx));
    }

    /**
     * Reads an absolute size length of the root element; percentages and invalid values count as missing.
     * <p>
     * 루트 요소의 절대 크기 길이를 읽는다. 백분율과 올바르지 않은 값은 없는 것으로 본다.
     *
     * @param text the attribute value / 속성 값
     * @return the length or null / 길이 또는 null
     */
    private static SvgLength sizeAttribute(String text) {
        if (text == null) {
            return null;
        }
        SvgLength l = SvgLength.parse(text);
        if (l == null || l.isPercent() || l.value < 0) {
            return null;
        }
        return l;
    }

    /**
     * Parses a <code>viewBox</code> value.
     * <p>
     * <code>viewBox</code> 값을 해석한다.
     *
     * @param text the attribute value / 속성 값
     * @return {x, y, width, height} or null if missing, invalid, or empty / {x, y, 너비, 높이}. 없거나 올바르지 않거나 비어 있으면 null
     */
    static double[] parseViewBox(String text) {
        double[] v = SvgScanner.numberList(text);
        if (v == null || v.length != 4 || v[2] <= 0 || v[3] <= 0) {
            return null;
        }
        return v;
    }

    /**
     * Records the ids of an element and its descendants.
     * <p>
     * 요소와 자손의 id 를 기록한다.
     *
     * @param e the element / 요소
     */
    private void index(SvgElement e) {
        String id = e.attrs.get("id");
        if (id != null && !ids.containsKey(id) && e.isSvg()) {
            ids.put(id, e);
        }
        // 자식 요소를 문서 순서로 훑는다.
        for (int i = 0; i < e.children.size(); i++) {
            index(e.children.get(i));
        }
    }

    /**
     * Parses SVG data.
     * <p>
     * SVG 데이터를 해석한다.
     *
     * @param data   the document bytes (UTF-8, UTF-16 or declared encoding; gzip compressed data is handled by {@link SvgDecoder}) / 문서 바이트(UTF-8, UTF-16, 선언된 인코딩. gzip 압축은 {@link SvgDecoder} 가 처리)
     * @param limits the limits, or null for the defaults / 제한. null 이면 기본값
     * @return the document / 문서
     * @throws MediaException CORRUPT for malformed XML or a document that is not SVG or has zero size, TOO_LARGE over a limit / 형식이 틀린 XML 이나 SVG 가 아니거나 크기가 0 이면 CORRUPT, 제한 초과는 TOO_LARGE
     */
    public static SvgImage parse(byte[] data, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        SvgElement root = SvgXmlParser.parse(data);
        SvgImage img = new SvgImage(root, lim);
        lim.checkImageSize(img.getWidth(), img.getHeight());
        return img;
    }

    /**
     * Parses an SVG document that is only a collection of definitions for the references of CSS (<code>filter: url(#id)</code> and the like): the size of the
     * document does not matter (a hidden <code>svg</code> of zero size is the usual place for definitions), so it is not checked and nothing is drawn.
     * <p>
     * CSS 참조(<code>filter: url(#id)</code> 등)를 위한 정의 모음일 뿐인 SVG 문서를 해석한다: 문서의 크기는 상관없으므로(정의는 보통 크기가 0 인 숨겨진
     * <code>svg</code> 에 둔다) 검사하지 않으며 아무것도 그리지 않는다.
     *
     * @param data   the document bytes / 문서 바이트
     * @param limits the limits, or null for the defaults / 제한. null 이면 기본값
     * @return the document / 문서
     * @throws MediaException CORRUPT for malformed XML or a document that is not SVG / 형식이 틀린 XML 이나 SVG 가 아니면 CORRUPT
     */
    static SvgImage parseDefinitions(byte[] data, DecodeLimits limits) throws MediaException {
        return new SvgImage(SvgXmlParser.parse(data), limits == null ? DecodeLimits.DEFAULT : limits);
    }

    /**
     * Returns the natural width in whole pixels (the rounded value of the CSS width).
     * <p>
     * 자연 너비를 정수 픽셀로 반환한다(CSS 너비를 반올림한 값).
     *
     * @return width in pixels / 픽셀 너비
     */
    public int getWidth() {
        return (int) Math.floor(naturalWidth + 0.5);
    }

    /**
     * Returns the natural height in whole pixels.
     * <p>
     * 자연 높이를 정수 픽셀로 반환한다.
     *
     * @return height in pixels / 픽셀 높이
     */
    public int getHeight() {
        return (int) Math.floor(naturalHeight + 0.5);
    }

    /**
     * Returns the natural width in CSS pixels without rounding.
     * <p>
     * 자연 너비를 반올림 없이 CSS 픽셀로 반환한다.
     *
     * @return width / 너비
     */
    public double getNaturalWidth() {
        return naturalWidth;
    }

    /**
     * Returns the natural height in CSS pixels without rounding.
     * <p>
     * 자연 높이를 반올림 없이 CSS 픽셀로 반환한다.
     *
     * @return height / 높이
     */
    public double getNaturalHeight() {
        return naturalHeight;
    }

    /**
     * Tells whether the document states its size (width/height lengths or a viewBox); false means the 300x150 default.
     * <p>
     * 문서가 크기를 밝히는지(width/height 길이나 viewBox) 알려 준다. false 이면 300x150 기본값이다.
     *
     * @return true if the size is known / 크기를 알면 true
     */
    public boolean hasIntrinsicSize() {
        return sizeKnown;
    }

    /**
     * Draws the document into the box (0, 0, width, height) of a graphics context. The view box and <code>preserveAspectRatio</code> map the
     * document to the box. The drawing is done on an internal image and then copied, so the transform of the target only moves and scales
     * that copy; call with a target at the final scale for the best quality.
     * <p>
     * 그래픽스 문맥의 상자 (0, 0, 너비, 높이)에 문서를 그린다. viewBox 와 <code>preserveAspectRatio</code> 가 문서를 상자에 대응시킨다. 그리기는 내부
     * 이미지에서 한 뒤 복사하므로 대상의 변환은 그 복사본만 옮기고 키운다. 가장 좋은 품질을 위해 최종 배율의 대상으로 호출한다.
     *
     * @param target the graphics context / 그래픽스 문맥
     * @param width  box width in pixels (at least 1) / 상자 너비(픽셀, 1 이상)
     * @param height box height in pixels (at least 1) / 상자 높이(픽셀, 1 이상)
     * @throws MediaException TOO_LARGE if the document is too complex or the box too large, CANCELLED if the thread is interrupted / 문서가 너무 복잡하거나 상자가 너무 크면 TOO_LARGE, 스레드가 인터럽트되면 CANCELLED
     */
    public void render(Graphics2D target, int width, int height) throws MediaException {
        BufferedImage canvas = renderImage(width, height);
        target.drawImage(canvas, 0, 0, null);
    }

    /**
     * Draws the document into a new image of the given size (premultiplied ARGB).
     * <p>
     * 지정한 크기의 새 이미지(알파 미리 곱한 ARGB)에 문서를 그린다.
     *
     * @param width  width in pixels / 너비(픽셀)
     * @param height height in pixels / 높이(픽셀)
     * @return the image / 이미지
     * @throws MediaException TOO_LARGE or CANCELLED / TOO_LARGE 또는 CANCELLED
     */
    public BufferedImage renderImage(int width, int height) throws MediaException {
        if (width < 1 || height < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid render size " + width + "x" + height);
        }
        limits.checkImageSize(width, height);
        // Java2D 를 처음 쓰면 AWT 가 대기하다 인터럽트 상태를 지울 수 있으므로 그리기 전에 먼저 확인한다.
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "decoding was cancelled");
        }
        Integer depth = NESTING.get();
        int d = depth == null ? 0 : depth.intValue();
        if (d >= MAX_IMAGE_NESTING) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "SVG images are nested too deeply");
        }
        NESTING.set(Integer.valueOf(d + 1));
        try {
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            new SvgRenderer(this, canvas).render();
            return canvas;
        } finally {
            if (d == 0) {
                NESTING.remove();
            } else {
                NESTING.set(Integer.valueOf(d));
            }
        }
    }

    /**
     * Draws the document at a size and returns non-premultiplied ARGB pixels (<code>0xAARRGGBB</code>), row by row.
     * <p>
     * 지정한 크기로 문서를 그리고 알파를 미리 곱하지 않은 ARGB 픽셀(<code>0xAARRGGBB</code>)을 행 순서로 반환한다.
     *
     * @param width  width in pixels / 너비(픽셀)
     * @param height height in pixels / 높이(픽셀)
     * @return pixels / 픽셀
     * @throws MediaException TOO_LARGE or CANCELLED / TOO_LARGE 또는 CANCELLED
     */
    public int[] renderArgb(int width, int height) throws MediaException {
        BufferedImage canvas = renderImage(width, height);
        int[] pre = canvas.getRaster().getDataBuffer() instanceof java.awt.image.DataBufferInt
                ? ((java.awt.image.DataBufferInt) canvas.getRaster().getDataBuffer()).getData() : canvas.getRGB(0, 0, width, height, null, 0, width);
        int[] out = new int[width * height];
        // 알파를 미리 곱한 픽셀을 곱하지 않은 값으로 되돌린다.
        for (int i = 0; i < out.length; i++) {
            out[i] = unpremultiply(pre[i]);
        }
        return out;
    }

    /**
     * Converts a premultiplied ARGB pixel to a non-premultiplied one.
     * <p>
     * 알파를 미리 곱한 ARGB 픽셀을 곱하지 않은 픽셀로 바꾼다.
     *
     * @param p premultiplied pixel / 알파를 미리 곱한 픽셀
     * @return non-premultiplied pixel / 알파를 곱하지 않은 픽셀
     */
    static int unpremultiply(int p) {
        int a = p >>> 24;
        if (a == 255 || a == 0) {
            return a == 0 ? 0 : p;
        }
        int r = Math.min(255, ((((p >> 16) & 0xFF) * 255) + a / 2) / a);
        int g = Math.min(255, ((((p >> 8) & 0xFF) * 255) + a / 2) / a);
        int b = Math.min(255, (((p & 0xFF) * 255) + a / 2) / a);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Rasterizes the document at its natural size as a still image for the common image pipeline.
     * <p>
     * 공통 이미지 처리 경로를 위해 문서를 자연 크기의 정지 이미지로 래스터화한다.
     *
     * @return the image / 이미지
     * @throws MediaException CORRUPT for a zero size, TOO_LARGE or CANCELLED / 크기가 0 이면 CORRUPT, TOO_LARGE 또는 CANCELLED
     */
    public DecodedImage toDecodedImage() throws MediaException {
        int w = getWidth();
        int h = getHeight();
        limits.checkImageSize(w, h);
        return new DecodedImage(w, h, renderArgb(w, h));
    }

    /**
     * Returns a lower-case keyword attribute of the root element (for diagnostics and tests).
     * <p>
     * 루트 요소의 소문자 키워드 속성을 반환한다(진단과 시험용).
     *
     * @param name the attribute name / 속성 이름
     * @return the value or null / 값 또는 null
     */
    String rootKeyword(String name) {
        String v = root.attr(name);
        return v == null ? null : v.trim().toLowerCase(Locale.ENGLISH);
    }
}
