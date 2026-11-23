package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.render.CanvasFilters;

/**
 * The drawing engine behind <code>CanvasRenderingContext2D</code>: the state stack, the transformation matrix, the current path, filling,
 * stroking (with dashes, caps and joins), clipping with antialiased masks, rectangles, text, images, pixel data, shadows with Gaussian blur and
 * all 26 compositing operators. It is pure Java2D on a <code>BufferedImage</code> of the {@link CanvasSurface}; nothing uses the GPU. The
 * methods take Java numbers and objects; the script bindings do the WebIDL conversions and the type checks. Where the specification says "if
 * any argument is infinite or NaN, return" the methods return without doing anything, and invalid values of attributes are ignored. Methods
 * that must throw (<code>IndexSizeError</code>, <code>SecurityError</code>, <code>InvalidStateError</code>) throw {@link DomException}. The
 * methods are not synchronized; the caller holds the monitor of the surface.
 * <p>
 * Drawing model: a shape (or the glyph outline of a text, or an image) is painted into a transparent layer, the layer is multiplied by the clip
 * mask and by the global alpha, and then composited with the operator onto the bitmap, over the whole clipped canvas for the operators that
 * change pixels where the source is transparent. Plain <code>source-over</code> drawing without a shadow and without a non-rectangular clip
 * skips the layer and paints straight onto the bitmap.
 * <p>
 * <code>CanvasRenderingContext2D</code> 뒤의 그리기 엔진: 상태 스택, 변환 행렬, 현재 경로, 채우기, 선 긋기(점선, 끝 모양, 꺾임 포함), 안티앨리어싱된
 * 마스크로 클리핑, 사각형, 텍스트, 이미지, 화소 데이터, 가우시안 번짐 그림자, 합성 연산자 26 개 전부. {@link CanvasSurface} 의
 * <code>BufferedImage</code> 위의 순수 Java2D 이며 GPU 를 쓰지 않는다. 메서드는 Java 숫자와 객체를 받으며 WebIDL 변환과 형식 검사는 스크립트 연결이
 * 한다. 규격이 "인수가 무한대나 NaN 이면 반환" 이라고 한 곳에서는 아무것도 하지 않고 반환하며, 속성의 잘못된 값은 무시한다. 던져야 하는
 * 경우(<code>IndexSizeError</code>, <code>SecurityError</code>, <code>InvalidStateError</code>)는 {@link DomException} 을 던진다. 메서드는 동기화하지
 * 않으며 호출자가 표면의 모니터를 잡는다.
 * <p>
 * 그리기 모델: 도형(또는 텍스트의 글리프 윤곽, 이미지)을 투명한 레이어에 칠하고, 레이어에 클립 마스크와 전역 알파를 곱한 뒤, 연산자로 비트맵에
 * 합성한다. 원본이 투명한 곳의 화소도 바꾸는 연산자는 클리핑된 캔버스 전체에 합성한다. 그림자와 사각형이 아닌 클립이 없는 평범한
 * <code>source-over</code> 그리기는 레이어를 건너뛰고 비트맵에 바로 칠한다.
 */
public final class Context2D {

    /**
     * The deepest <code>save()</code> nesting that is remembered; deeper calls are ignored.
     * <p>
     * 기억하는 <code>save()</code> 중첩의 최대 깊이. 더 깊은 호출은 무시한다.
     */
    private static final int MAX_STATES = 10000;

    /**
     * The most pixels of a layer that the engine allocates for one drawing call (a layer never exceeds the bitmap).
     * <p>
     * 그리기 호출 하나를 위해 엔진이 할당하는 레이어의 최대 화소 수(레이어는 비트맵보다 커지지 않는다).
     */
    private static final long MAX_LAYER_PIXELS = CanvasLimits.MAX_PIXELS;

    /**
     * The most cells of the buffers of a shadow (float alpha, blur copy and color pixels, about 16 bytes per cell); a shadow that needs
     * more is not drawn.
     * <p>
     * 그림자 버퍼(float 알파, 번짐 복사본, 색 화소. 칸당 약 16바이트)의 최대 칸 수. 더 필요한 그림자는 그리지 않는다.
     */
    private static final long MAX_SHADOW_CELLS = 4L * 1024 * 1024;

    /**
     * The most multiply-add operations that the blur of one shadow may need (cells times the width of the kernel, twice); a shadow that
     * needs more is not drawn, so that a script cannot keep the document thread busy for long with one call.
     * <p>
     * 그림자 하나의 번짐이 필요로 할 수 있는 곱셈-덧셈 연산의 최대 수(칸 수 곱하기 커널 너비, 두 번). 더 필요한 그림자는 그리지 않으며, 그래서 스크립트가
     * 호출 하나로 문서 스레드를 오래 붙잡지 못한다.
     */
    private static final long MAX_BLUR_WORK = 400L * 1000 * 1000;

    /**
     * The surface that this context draws on.
     * <p>
     * 이 컨텍스트가 그리는 표면.
     */
    private final CanvasSurface surface;

    /**
     * The current drawing state.
     * <p>
     * 현재 그리기 상태.
     */
    private DrawState state = new DrawState();

    /**
     * The saved states.
     * <p>
     * 저장된 상태.
     */
    private final List<DrawState> stack = new ArrayList<DrawState>();

    /**
     * The current default path.
     * <p>
     * 현재 기본 경로.
     */
    private final PathBuilder path = new PathBuilder();

    /**
     * The font size that relative font sizes refer to (the font size of the canvas element).
     * <p>
     * 상대 글꼴 크기가 기준으로 삼는 글꼴 크기(캔버스 요소의 글꼴 크기).
     */
    private double baseFontSize = 16;

    /**
     * The color that <code>currentcolor</code> means (the color of the canvas element).
     * <p>
     * <code>currentcolor</code> 가 뜻하는 색(캔버스 요소의 색).
     */
    private int currentColor = 0xFF000000;

    /**
     * Pixel data read from the bitmap: its size and the RGBA bytes.
     * <p>
     * 비트맵에서 읽은 화소 데이터: 크기와 RGBA 바이트.
     */
    public static final class RawImage {
        /**
         * The width in pixels.
         * <p>
         * 너비(화소).
         */
        public final int width;

        /**
         * The height in pixels.
         * <p>
         * 높이(화소).
         */
        public final int height;

        /**
         * The bytes, four per pixel in the order red, green, blue, alpha, row by row.
         * <p>
         * 화소당 네 바이트(빨강, 초록, 파랑, 알파) 행 순서의 바이트.
         */
        public final byte[] data;

        /**
         * Creates the value.
         * <p>
         * 값을 만든다.
         *
         * @param width  width / 너비
         * @param height height / 높이
         * @param data   the bytes / 바이트
         */
        public RawImage(int width, int height, byte[] data) {
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }

    /**
     * Something that paints onto a graphics whose coordinates are the bitmap coordinates.
     * <p>
     * 좌표가 비트맵 좌표인 그래픽스에 칠하는 것.
     */
    private interface Painter {
        /**
         * Paints.
         * <p>
         * 칠한다.
         *
         * @param g the graphics / 그래픽스
         */
        void paint(Graphics2D g);
    }

    /**
     * Creates the context of a surface.
     * <p>
     * 표면의 컨텍스트를 만든다.
     *
     * @param surface the surface / 표면
     */
    Context2D(CanvasSurface surface) {
        this.surface = surface;
    }

    /**
     * Returns the surface.
     * <p>
     * 표면을 반환한다.
     *
     * @return the surface / 표면
     */
    public CanvasSurface getSurface() {
        return surface;
    }

    /**
     * Tells the context the font size and color of the canvas element, which relative sizes and <code>currentcolor</code> refer to.
     * <p>
     * 컨텍스트에 상대 크기와 <code>currentcolor</code> 가 기준으로 삼는 캔버스 요소의 글꼴 크기와 색을 알려 준다.
     *
     * @param fontSizePx the font size in px / 글꼴 크기(px)
     * @param argb       the color / 색
     */
    public void setElementStyle(double fontSizePx, int argb) {
        baseFontSize = fontSizePx > 0 ? fontSizePx : 16;
        currentColor = argb;
    }

    /**
     * Returns the color that <code>currentcolor</code> means.
     * <p>
     * <code>currentcolor</code> 가 뜻하는 색을 반환한다.
     *
     * @return the ARGB color / ARGB 색
     */
    public int getCurrentColor() {
        return currentColor;
    }

    /**
     * Returns the font size that relative sizes refer to.
     * <p>
     * 상대 크기가 기준으로 삼는 글꼴 크기를 반환한다.
     *
     * @return the size in px / 크기(px)
     */
    public double getBaseFontSize() {
        return baseFontSize;
    }

    /**
     * Resets the state to the defaults (the bitmap was replaced): state stack, matrix, clip, styles, path.
     * <p>
     * 상태를 기본값으로 되돌린다(비트맵이 교체됨): 상태 스택, 행렬, 클립, 스타일, 경로.
     */
    void resetState() {
        state = new DrawState();
        stack.clear();
        path.reset();
    }

    /**
     * Resets the context as <code>reset()</code> does: the bitmap becomes transparent and the state returns to its defaults.
     * <p>
     * <code>reset()</code> 처럼 컨텍스트를 되돌린다: 비트맵은 투명해지고 상태는 기본값으로 돌아간다.
     */
    public void reset() {
        resetState();
        CanvasBitmap bm = surface.bitmap();
        if (!bm.isEmpty()) {
            bm.clear();
            surface.markChanged();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 상태 스택
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Pushes a copy of the drawing state.
     * <p>
     * 그리기 상태의 복사본을 쌓는다.
     */
    public void save() {
        if (stack.size() < MAX_STATES) {
            stack.add(state.copy());
        }
    }

    /**
     * Pops the drawing state; nothing happens when the stack is empty.
     * <p>
     * 그리기 상태를 꺼낸다. 스택이 비어 있으면 아무 일도 없다.
     */
    public void restore() {
        if (!stack.isEmpty()) {
            state = stack.remove(stack.size() - 1);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 변환
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether all numbers are finite.
     * <p>
     * 모든 숫자가 유한한지 알려 준다.
     *
     * @param v the numbers / 숫자
     * @return true if none is NaN or infinite / NaN 이나 무한대가 없으면 true
     */
    static boolean finite(double... v) {
        // 숫자를 하나씩 검사한다.
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i]) || Double.isInfinite(v[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Scales the user space.
     * <p>
     * 사용자 좌표계를 늘이거나 줄인다.
     *
     * @param x horizontal factor / 가로 배율
     * @param y vertical factor / 세로 배율
     */
    public void scale(double x, double y) {
        if (finite(x, y)) {
            state.ctm.scale(x, y);
        }
    }

    /**
     * Rotates the user space.
     * <p>
     * 사용자 좌표계를 회전한다.
     *
     * @param angle the angle in radians, clockwise / 각도(라디안, 시계 방향)
     */
    public void rotate(double angle) {
        if (finite(angle)) {
            state.ctm.rotate(angle);
        }
    }

    /**
     * Moves the user space.
     * <p>
     * 사용자 좌표계를 옮긴다.
     *
     * @param x horizontal distance / 가로 거리
     * @param y vertical distance / 세로 거리
     */
    public void translate(double x, double y) {
        if (finite(x, y)) {
            state.ctm.translate(x, y);
        }
    }

    /**
     * Multiplies the matrix by another one.
     * <p>
     * 행렬에 다른 행렬을 곱한다.
     *
     * @param a m11 / m11
     * @param b m12 / m12
     * @param c m21 / m21
     * @param d m22 / m22
     * @param e dx / dx
     * @param f dy / dy
     */
    public void transform(double a, double b, double c, double d, double e, double f) {
        if (finite(a, b, c, d, e, f)) {
            state.ctm.concatenate(new AffineTransform(a, b, c, d, e, f));
        }
    }

    /**
     * Replaces the matrix.
     * <p>
     * 행렬을 교체한다.
     *
     * @param a m11 / m11
     * @param b m12 / m12
     * @param c m21 / m21
     * @param d m22 / m22
     * @param e dx / dx
     * @param f dy / dy
     */
    public void setTransform(double a, double b, double c, double d, double e, double f) {
        if (finite(a, b, c, d, e, f)) {
            state.ctm.setTransform(a, b, c, d, e, f);
        }
    }

    /**
     * Resets the matrix to the identity.
     * <p>
     * 행렬을 단위 행렬로 되돌린다.
     */
    public void resetTransform() {
        state.ctm.setToIdentity();
    }

    /**
     * Returns the matrix as a, b, c, d, e, f.
     * <p>
     * 행렬을 a, b, c, d, e, f 로 반환한다.
     *
     * @return six numbers / 숫자 여섯 개
     */
    public double[] getTransform() {
        double[] m = new double[6];
        state.ctm.getMatrix(m);
        return m;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 속성
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the global alpha.
     * <p>
     * 전역 알파를 반환한다.
     *
     * @return the alpha / 알파
     */
    public double getGlobalAlpha() {
        return state.globalAlpha;
    }

    /**
     * Sets the global alpha; values outside 0..1 and NaN are ignored.
     * <p>
     * 전역 알파를 정한다. 0..1 밖의 값과 NaN 은 무시한다.
     *
     * @param v the alpha / 알파
     */
    public void setGlobalAlpha(double v) {
        if (v >= 0 && v <= 1) {
            state.globalAlpha = v;
        }
    }

    /**
     * Returns the compositing operator name.
     * <p>
     * 합성 연산자 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getGlobalCompositeOperation() {
        return Composites.name(state.composite);
    }

    /**
     * Sets the compositing operator; an unknown name is ignored.
     * <p>
     * 합성 연산자를 정한다. 모르는 이름은 무시한다.
     *
     * @param name the operator name / 연산자 이름
     */
    public void setGlobalCompositeOperation(String name) {
        int op = Composites.parse(name);
        if (op >= 0) {
            state.composite = op;
        }
    }

    /**
     * Returns the line width.
     * <p>
     * 선 너비를 반환한다.
     *
     * @return the width / 너비
     */
    public double getLineWidth() {
        return state.lineWidth;
    }

    /**
     * Sets the line width; zero, negative and non-finite values are ignored.
     * <p>
     * 선 너비를 정한다. 0, 음수, 유한하지 않은 값은 무시한다.
     *
     * @param v the width / 너비
     */
    public void setLineWidth(double v) {
        if (v > 0 && finite(v)) {
            state.lineWidth = v;
        }
    }

    /**
     * Returns the line cap name.
     * <p>
     * 선 끝 모양 이름을 반환한다.
     *
     * @return butt, round or square / butt, round, square
     */
    public String getLineCap() {
        return state.lineCap == 1 ? "round" : (state.lineCap == 2 ? "square" : "butt");
    }

    /**
     * Sets the line cap; an unknown name is ignored.
     * <p>
     * 선 끝 모양을 정한다. 모르는 이름은 무시한다.
     *
     * @param v butt, round or square / butt, round, square
     */
    public void setLineCap(String v) {
        if ("butt".equals(v)) {
            state.lineCap = BasicStroke.CAP_BUTT;
        } else if ("round".equals(v)) {
            state.lineCap = BasicStroke.CAP_ROUND;
        } else if ("square".equals(v)) {
            state.lineCap = BasicStroke.CAP_SQUARE;
        }
    }

    /**
     * Returns the line join name.
     * <p>
     * 선 꺾임 모양 이름을 반환한다.
     *
     * @return round, bevel or miter / round, bevel, miter
     */
    public String getLineJoin() {
        return state.lineJoin == 1 ? "round" : (state.lineJoin == 2 ? "bevel" : "miter");
    }

    /**
     * Sets the line join; an unknown name is ignored.
     * <p>
     * 선 꺾임 모양을 정한다. 모르는 이름은 무시한다.
     *
     * @param v round, bevel or miter / round, bevel, miter
     */
    public void setLineJoin(String v) {
        if ("miter".equals(v)) {
            state.lineJoin = BasicStroke.JOIN_MITER;
        } else if ("round".equals(v)) {
            state.lineJoin = BasicStroke.JOIN_ROUND;
        } else if ("bevel".equals(v)) {
            state.lineJoin = BasicStroke.JOIN_BEVEL;
        }
    }

    /**
     * Returns the miter limit.
     * <p>
     * 마이터 한계를 반환한다.
     *
     * @return the limit / 한계
     */
    public double getMiterLimit() {
        return state.miterLimit;
    }

    /**
     * Sets the miter limit; zero, negative and non-finite values are ignored.
     * <p>
     * 마이터 한계를 정한다. 0, 음수, 유한하지 않은 값은 무시한다.
     *
     * @param v the limit / 한계
     */
    public void setMiterLimit(double v) {
        if (v > 0 && finite(v)) {
            state.miterLimit = v;
        }
    }

    /**
     * Sets the line dash pattern; negative or non-finite entries make the call do nothing, an odd number of entries is repeated once.
     * <p>
     * 점선 패턴을 정한다. 음수나 유한하지 않은 항목이 있으면 아무것도 하지 않고, 항목이 홀수 개이면 한 번 더 반복한다.
     *
     * @param segments the dash lengths / 점선 길이
     */
    public void setLineDash(double[] segments) {
        // 항목이 유효한지 확인한다.
        for (int i = 0; i < segments.length; i++) {
            if (!(segments[i] >= 0) || !finite(segments[i])) {
                return;
            }
        }
        if (segments.length == 0) {
            state.lineDash = null;
            return;
        }
        double[] d = new double[segments.length % 2 == 0 ? segments.length : segments.length * 2];
        // 홀수 개이면 같은 항목을 한 번 더 이어 붙인다.
        for (int i = 0; i < d.length; i++) {
            d[i] = segments[i % segments.length];
        }
        state.lineDash = d;
    }

    /**
     * Returns a copy of the line dash pattern (empty for a solid line).
     * <p>
     * 점선 패턴의 복사본을 반환한다(실선이면 빈 배열).
     *
     * @return the dash lengths / 점선 길이
     */
    public double[] getLineDash() {
        return state.lineDash == null ? new double[0] : state.lineDash.clone();
    }

    /**
     * Returns the dash offset.
     * <p>
     * 점선 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getLineDashOffset() {
        return state.lineDashOffset;
    }

    /**
     * Sets the dash offset; a non-finite value is ignored.
     * <p>
     * 점선 오프셋을 정한다. 유한하지 않은 값은 무시한다.
     *
     * @param v the offset / 오프셋
     */
    public void setLineDashOffset(double v) {
        if (finite(v)) {
            state.lineDashOffset = v;
        }
    }

    /**
     * Returns the shadow color.
     * <p>
     * 그림자 색을 반환한다.
     *
     * @return the ARGB color / ARGB 색
     */
    public int getShadowColor() {
        return state.shadowColor;
    }

    /**
     * Sets the shadow color.
     * <p>
     * 그림자 색을 정한다.
     *
     * @param argb the color / 색
     */
    public void setShadowColor(int argb) {
        state.shadowColor = argb;
    }

    /**
     * Returns the shadow blur.
     * <p>
     * 그림자 번짐을 반환한다.
     *
     * @return the blur / 번짐
     */
    public double getShadowBlur() {
        return state.shadowBlur;
    }

    /**
     * Sets the shadow blur; negative and non-finite values are ignored.
     * <p>
     * 그림자 번짐을 정한다. 음수와 유한하지 않은 값은 무시한다.
     *
     * @param v the blur / 번짐
     */
    public void setShadowBlur(double v) {
        if (v >= 0 && finite(v)) {
            state.shadowBlur = v;
        }
    }

    /**
     * Returns the horizontal shadow offset.
     * <p>
     * 그림자의 가로 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getShadowOffsetX() {
        return state.shadowOffsetX;
    }

    /**
     * Sets the horizontal shadow offset; a non-finite value is ignored.
     * <p>
     * 그림자의 가로 오프셋을 정한다. 유한하지 않은 값은 무시한다.
     *
     * @param v the offset / 오프셋
     */
    public void setShadowOffsetX(double v) {
        if (finite(v)) {
            state.shadowOffsetX = v;
        }
    }

    /**
     * Returns the vertical shadow offset.
     * <p>
     * 그림자의 세로 오프셋을 반환한다.
     *
     * @return the offset / 오프셋
     */
    public double getShadowOffsetY() {
        return state.shadowOffsetY;
    }

    /**
     * Sets the vertical shadow offset; a non-finite value is ignored.
     * <p>
     * 그림자의 세로 오프셋을 정한다. 유한하지 않은 값은 무시한다.
     *
     * @param v the offset / 오프셋
     */
    public void setShadowOffsetY(double v) {
        if (finite(v)) {
            state.shadowOffsetY = v;
        }
    }

    /**
     * Returns the fill style: an <code>Integer</code> ARGB color, a gradient or a pattern.
     * <p>
     * 채우기 스타일을 반환한다: <code>Integer</code> ARGB 색, 그라디언트, 패턴.
     *
     * @return the style / 스타일
     */
    public Object getFillStyle() {
        return state.fillStyle;
    }

    /**
     * Sets the fill style.
     * <p>
     * 채우기 스타일을 정한다.
     *
     * @param style an <code>Integer</code> color, a {@link CanvasGradient} or a {@link CanvasPattern} / <code>Integer</code> 색, {@link CanvasGradient}, {@link CanvasPattern}
     */
    public void setFillStyle(Object style) {
        state.fillStyle = style;
    }

    /**
     * Returns the stroke style.
     * <p>
     * 선 스타일을 반환한다.
     *
     * @return the style / 스타일
     */
    public Object getStrokeStyle() {
        return state.strokeStyle;
    }

    /**
     * Sets the stroke style.
     * <p>
     * 선 스타일을 정한다.
     *
     * @param style an <code>Integer</code> color, a {@link CanvasGradient} or a {@link CanvasPattern} / <code>Integer</code> 색, {@link CanvasGradient}, {@link CanvasPattern}
     */
    public void setStrokeStyle(Object style) {
        state.strokeStyle = style;
    }

    /**
     * Returns the current font.
     * <p>
     * 현재 글꼴을 반환한다.
     *
     * @return the font / 글꼴
     */
    public CanvasFont getFont() {
        return state.font;
    }

    /**
     * Sets the font.
     * <p>
     * 글꼴을 정한다.
     *
     * @param f the font / 글꼴
     */
    public void setFont(CanvasFont f) {
        state.font = f;
    }

    /**
     * Returns the text alignment.
     * <p>
     * 텍스트 정렬을 반환한다.
     *
     * @return the alignment / 정렬
     */
    public String getTextAlign() {
        return state.textAlign;
    }

    /**
     * Sets the text alignment; an unknown value is ignored.
     * <p>
     * 텍스트 정렬을 정한다. 모르는 값은 무시한다.
     *
     * @param v start, end, left, right or center / start, end, left, right, center
     */
    public void setTextAlign(String v) {
        if ("start".equals(v) || "end".equals(v) || "left".equals(v) || "right".equals(v) || "center".equals(v)) {
            state.textAlign = v;
        }
    }

    /**
     * Returns the text baseline.
     * <p>
     * 텍스트 기준선을 반환한다.
     *
     * @return the baseline / 기준선
     */
    public String getTextBaseline() {
        return state.textBaseline;
    }

    /**
     * Sets the text baseline; an unknown value is ignored.
     * <p>
     * 텍스트 기준선을 정한다. 모르는 값은 무시한다.
     *
     * @param v top, hanging, middle, alphabetic, ideographic or bottom / top, hanging, middle, alphabetic, ideographic, bottom
     */
    public void setTextBaseline(String v) {
        if ("top".equals(v) || "hanging".equals(v) || "middle".equals(v) || "alphabetic".equals(v) || "ideographic".equals(v)
                || "bottom".equals(v)) {
            state.textBaseline = v;
        }
    }

    /**
     * Returns the text direction.
     * <p>
     * 텍스트 방향을 반환한다.
     *
     * @return ltr, rtl or inherit / ltr, rtl, inherit
     */
    public String getDirection() {
        return state.direction;
    }

    /**
     * Sets the text direction; an unknown value is ignored.
     * <p>
     * 텍스트 방향을 정한다. 모르는 값은 무시한다.
     *
     * @param v ltr, rtl or inherit / ltr, rtl, inherit
     */
    public void setDirection(String v) {
        if ("ltr".equals(v) || "rtl".equals(v) || "inherit".equals(v)) {
            state.direction = v;
        }
    }

    /**
     * Returns the serialized <code>letterSpacing</code>.
     * <p>
     * 직렬화된 <code>letterSpacing</code> 을 반환한다.
     *
     * @return for example <code>0px</code> / 예를 들어 <code>0px</code>
     */
    public String getLetterSpacing() {
        return state.letterSpacing.serialize();
    }

    /**
     * Sets <code>letterSpacing</code>: the spacing that is added after every character when text is drawn or measured. A value that is not a CSS
     * length is ignored.
     * <p>
     * <code>letterSpacing</code> 을 정한다: 글자를 그리거나 잴 때 모든 글자 뒤에 더하는 간격. CSS 길이가 아닌 값은 무시한다.
     *
     * @param v a length such as <code>2px</code>, <code>0.1em</code> / <code>2px</code>, <code>0.1em</code> 같은 길이
     */
    public void setLetterSpacing(String v) {
        TextSpacing t = TextSpacing.parse(v);
        if (t != null) {
            state.letterSpacing = t;
        }
    }

    /**
     * Returns the serialized <code>wordSpacing</code>.
     * <p>
     * 직렬화된 <code>wordSpacing</code> 을 반환한다.
     *
     * @return for example <code>0px</code> / 예를 들어 <code>0px</code>
     */
    public String getWordSpacing() {
        return state.wordSpacing.serialize();
    }

    /**
     * Sets <code>wordSpacing</code>: the spacing that is added after every space and no-break space when text is drawn or measured. A value that
     * is not a CSS length is ignored.
     * <p>
     * <code>wordSpacing</code> 을 정한다: 글자를 그리거나 잴 때 모든 스페이스와 줄바꿈 없는 스페이스 뒤에 더하는 간격. CSS 길이가 아닌 값은 무시한다.
     *
     * @param v a length such as <code>10px</code> / <code>10px</code> 같은 길이
     */
    public void setWordSpacing(String v) {
        TextSpacing t = TextSpacing.parse(v);
        if (t != null) {
            state.wordSpacing = t;
        }
    }

    /**
     * Returns the <code>fontKerning</code> keyword.
     * <p>
     * <code>fontKerning</code> 키워드를 반환한다.
     *
     * @return auto, normal or none / auto, normal, none
     */
    public String getFontKerning() {
        return state.fontKerning;
    }

    /**
     * Sets <code>fontKerning</code>; an unknown keyword is ignored.
     * <p>
     * <code>fontKerning</code> 을 정한다. 모르는 키워드는 무시한다.
     *
     * @param v auto, normal or none / auto, normal, none
     */
    public void setFontKerning(String v) {
        if ("auto".equals(v) || "normal".equals(v) || "none".equals(v)) {
            state.fontKerning = v;
        }
    }

    /**
     * Returns the <code>fontStretch</code> keyword.
     * <p>
     * <code>fontStretch</code> 키워드를 반환한다.
     *
     * @return one of the nine CSS keywords / 아홉 개의 CSS 키워드 중 하나
     */
    public String getFontStretch() {
        return state.fontStretch;
    }

    /**
     * Sets <code>fontStretch</code>; an unknown keyword is ignored.
     * <p>
     * <code>fontStretch</code> 를 정한다. 모르는 키워드는 무시한다.
     *
     * @param v ultra-condensed ... ultra-expanded / ultra-condensed ... ultra-expanded
     */
    public void setFontStretch(String v) {
        if (oneOf(v, FONT_STRETCH)) {
            state.fontStretch = v;
        }
    }

    /**
     * Returns the <code>fontVariantCaps</code> keyword.
     * <p>
     * <code>fontVariantCaps</code> 키워드를 반환한다.
     *
     * @return one of the seven keywords / 일곱 키워드 중 하나
     */
    public String getFontVariantCaps() {
        return state.fontVariantCaps;
    }

    /**
     * Sets <code>fontVariantCaps</code>; an unknown keyword is ignored.
     * <p>
     * <code>fontVariantCaps</code> 를 정한다. 모르는 키워드는 무시한다.
     *
     * @param v normal, small-caps, all-small-caps, petite-caps, all-petite-caps, unicase or titling-caps / 일곱 키워드 중 하나
     */
    public void setFontVariantCaps(String v) {
        if (oneOf(v, FONT_VARIANT_CAPS)) {
            state.fontVariantCaps = v;
        }
    }

    /**
     * Returns the <code>textRendering</code> keyword.
     * <p>
     * <code>textRendering</code> 키워드를 반환한다.
     *
     * @return auto, optimizeSpeed, optimizeLegibility or geometricPrecision / auto, optimizeSpeed, optimizeLegibility, geometricPrecision
     */
    public String getTextRendering() {
        return state.textRendering;
    }

    /**
     * Sets <code>textRendering</code>; an unknown keyword is ignored (the keywords are case sensitive).
     * <p>
     * <code>textRendering</code> 을 정한다. 모르는 키워드는 무시한다(키워드는 대소문자를 구분한다).
     *
     * @param v auto, optimizeSpeed, optimizeLegibility or geometricPrecision / auto, optimizeSpeed, optimizeLegibility, geometricPrecision
     */
    public void setTextRendering(String v) {
        if (oneOf(v, TEXT_RENDERING)) {
            state.textRendering = v;
        }
    }

    /**
     * Returns the <code>lang</code> value.
     * <p>
     * <code>lang</code> 값을 반환한다.
     *
     * @return a language tag, the empty string or inherit / 언어 태그, 빈 문자열, inherit
     */
    public String getLang() {
        return state.lang;
    }

    /**
     * Sets <code>lang</code>: the value is taken as it is (the specification stores any given string).
     * <p>
     * <code>lang</code> 을 정한다: 값을 그대로 쓴다(규격은 주어진 문자열을 그대로 보관한다).
     *
     * @param v the language / 언어
     */
    public void setLang(String v) {
        state.lang = v;
    }

    /**
     * Returns the <code>filter</code> string as it was set.
     * <p>
     * 설정한 그대로의 <code>filter</code> 문자열을 반환한다.
     *
     * @return the string, <code>none</code> by default / 문자열. 기본은 <code>none</code>
     */
    public String getFilter() {
        return state.filter;
    }

    /**
     * Sets <code>filter</code>: <code>none</code> or a <code>&lt;filter-value-list&gt;</code>; anything else (also the empty string and CSS-wide
     * keywords) is ignored and the old value stays. Relative lengths refer to the font size of the canvas element at this moment. Every drawing
     * operation except <code>clearRect()</code> and <code>putImageData()</code> is filtered: the image that is drawn (with the global alpha applied) is the
     * input of the filter, shadows and compositing use its output.
     * <p>
     * <code>filter</code> 를 정한다: <code>none</code> 이거나 <code>&lt;filter-value-list&gt;</code>. 그 밖의 값(빈 문자열과 CSS 전역 키워드 포함)은 무시하고 옛 값을 유지한다.
     * 상대 길이는 이 시점의 캔버스 요소의 글꼴 크기를 기준으로 삼는다. <code>clearRect()</code> 와 <code>putImageData()</code> 를 뺀 모든 그리기가 필터를 거친다: 그려진
     * 이미지(전역 알파를 곱한 것)가 필터의 입력이고 그림자와 합성은 그 출력을 쓴다.
     *
     * @param v the filter / 필터
     */
    public void setFilter(String v) {
        if ("none".equals(v)) {
            state.filter = "none";
            state.filterSpec = null;
            return;
        }
        CanvasFilters.Parsed p = CanvasFilters.parse(v, baseFontSize, currentColor);
        if (p != null) {
            state.filter = v;
            state.filterSpec = p.getSpec();
        }
    }

    /**
     * The values of <code>fontStretch</code>.
     * <p>
     * <code>fontStretch</code> 의 값.
     */
    private static final String[] FONT_STRETCH = { "ultra-condensed", "extra-condensed", "condensed", "semi-condensed", "normal", "semi-expanded",
        "expanded", "extra-expanded", "ultra-expanded" };

    /**
     * The values of <code>fontVariantCaps</code>.
     * <p>
     * <code>fontVariantCaps</code> 의 값.
     */
    private static final String[] FONT_VARIANT_CAPS = { "normal", "small-caps", "all-small-caps", "petite-caps", "all-petite-caps", "unicase",
        "titling-caps" };

    /**
     * The values of <code>textRendering</code>.
     * <p>
     * <code>textRendering</code> 의 값.
     */
    private static final String[] TEXT_RENDERING = { "auto", "optimizeSpeed", "optimizeLegibility", "geometricPrecision" };

    /**
     * Tells whether a string is one of the keywords.
     * <p>
     * 문자열이 키워드 중 하나인지 알려 준다.
     *
     * @param v        the string / 문자열
     * @param keywords the keywords / 키워드들
     * @return true if it is one of them / 하나이면 true
     */
    private static boolean oneOf(String v, String[] keywords) {
        // 키워드를 하나씩 비교한다.
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].equals(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether images are smoothed.
     * <p>
     * 이미지를 부드럽게 하는지 알려 준다.
     *
     * @return true if smoothed / 부드럽게 하면 true
     */
    public boolean isImageSmoothingEnabled() {
        return state.smoothing;
    }

    /**
     * Sets whether images are smoothed.
     * <p>
     * 이미지를 부드럽게 할지 정한다.
     *
     * @param v true to smooth / 부드럽게 하려면 true
     */
    public void setImageSmoothingEnabled(boolean v) {
        state.smoothing = v;
    }

    /**
     * Returns the smoothing quality.
     * <p>
     * 부드럽게 하기 품질을 반환한다.
     *
     * @return low, medium or high / low, medium, high
     */
    public String getImageSmoothingQuality() {
        return state.smoothingQuality;
    }

    /**
     * Sets the smoothing quality; an unknown value is ignored.
     * <p>
     * 부드럽게 하기 품질을 정한다. 모르는 값은 무시한다.
     *
     * @param v low, medium or high / low, medium, high
     */
    public void setImageSmoothingQuality(String v) {
        if ("low".equals(v) || "medium".equals(v) || "high".equals(v)) {
            state.smoothingQuality = v;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 경로
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Starts a new path.
     * <p>
     * 새 경로를 시작한다.
     */
    public void beginPath() {
        path.reset();
    }

    /**
     * Closes the current subpath.
     * <p>
     * 현재 하위 경로를 닫는다.
     */
    public void closePath() {
        path.closePath();
    }

    /**
     * Starts a subpath.
     * <p>
     * 하위 경로를 시작한다.
     *
     * @param x x / x
     * @param y y / y
     */
    public void moveTo(double x, double y) {
        if (finite(x, y)) {
            path.moveTo(state.ctm, x, y);
        }
    }

    /**
     * Adds a line.
     * <p>
     * 직선을 더한다.
     *
     * @param x x / x
     * @param y y / y
     */
    public void lineTo(double x, double y) {
        if (finite(x, y)) {
            path.lineTo(state.ctm, x, y);
        }
    }

    /**
     * Adds a quadratic curve.
     * <p>
     * 2차 곡선을 더한다.
     *
     * @param cpx control x / 제어점 x
     * @param cpy control y / 제어점 y
     * @param x   end x / 끝 x
     * @param y   end y / 끝 y
     */
    public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
        if (finite(cpx, cpy, x, y)) {
            path.quadTo(state.ctm, cpx, cpy, x, y);
        }
    }

    /**
     * Adds a cubic curve.
     * <p>
     * 3차 곡선을 더한다.
     *
     * @param cp1x first control x / 첫 제어점 x
     * @param cp1y first control y / 첫 제어점 y
     * @param cp2x second control x / 둘째 제어점 x
     * @param cp2y second control y / 둘째 제어점 y
     * @param x    end x / 끝 x
     * @param y    end y / 끝 y
     */
    public void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        if (finite(cp1x, cp1y, cp2x, cp2y, x, y)) {
            path.cubicTo(state.ctm, cp1x, cp1y, cp2x, cp2y, x, y);
        }
    }

    /**
     * Adds a rounded corner between two lines.
     * <p>
     * 두 직선 사이에 둥근 모서리를 더한다.
     *
     * @param x1     first point x / 첫 점 x
     * @param y1     first point y / 첫 점 y
     * @param x2     second point x / 둘째 점 x
     * @param y2     second point y / 둘째 점 y
     * @param radius the radius / 반지름
     * @throws DomException IndexSizeError for a negative radius / 반지름이 음수이면 IndexSizeError
     */
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        if (!finite(x1, y1, x2, y2, radius)) {
            return;
        }
        if (radius < 0) {
            throw DomException.indexSize("The radius provided (" + radius + ") is negative.");
        }
        path.arcTo(state.ctm, x1, y1, x2, y2, radius);
    }

    /**
     * Adds a rectangle subpath.
     * <p>
     * 사각형 하위 경로를 더한다.
     *
     * @param x x / x
     * @param y y / y
     * @param w width / 너비
     * @param h height / 높이
     */
    public void rect(double x, double y, double w, double h) {
        if (finite(x, y, w, h)) {
            path.rect(state.ctm, x, y, w, h);
        }
    }

    /**
     * Adds a circular arc.
     * <p>
     * 원호를 더한다.
     *
     * @param x     center x / 중심 x
     * @param y     center y / 중심 y
     * @param r     radius / 반지름
     * @param start start angle in radians / 시작 각도(라디안)
     * @param end   end angle in radians / 끝 각도(라디안)
     * @param ccw   counter-clockwise / 반시계 방향
     * @throws DomException IndexSizeError for a negative radius / 반지름이 음수이면 IndexSizeError
     */
    public void arc(double x, double y, double r, double start, double end, boolean ccw) {
        ellipse(x, y, r, r, 0, start, end, ccw);
    }

    /**
     * Adds an elliptical arc.
     * <p>
     * 타원호를 더한다.
     *
     * @param x     center x / 중심 x
     * @param y     center y / 중심 y
     * @param rx    x radius / x 반지름
     * @param ry    y radius / y 반지름
     * @param rot   rotation in radians / 회전(라디안)
     * @param start start angle in radians / 시작 각도(라디안)
     * @param end   end angle in radians / 끝 각도(라디안)
     * @param ccw   counter-clockwise / 반시계 방향
     * @throws DomException IndexSizeError for a negative radius / 반지름이 음수이면 IndexSizeError
     */
    public void ellipse(double x, double y, double rx, double ry, double rot, double start, double end, boolean ccw) {
        if (!finite(x, y, rx, ry, rot, start, end)) {
            return;
        }
        if (rx < 0 || ry < 0) {
            throw DomException.indexSize("The radius provided (" + (rx < 0 ? rx : ry) + ") is negative.");
        }
        path.ellipse(state.ctm, x, y, rx, ry, rot, start, end, ccw);
    }

    /**
     * Adds a rounded rectangle.
     * <p>
     * 둥근 사각형을 더한다.
     *
     * @param x     x / x
     * @param y     y / y
     * @param w     width (may be negative) / 너비(음수 가능)
     * @param h     height (may be negative) / 높이(음수 가능)
     * @param radii eight numbers: the x and y radius of the top left, top right, bottom right and bottom left corner / 숫자 여덟 개: 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래 모서리의 x, y 반지름
     */
    public void roundRect(double x, double y, double w, double h, double[] radii) {
        if (!finite(x, y, w, h) || !finite(radii)) {
            return;
        }
        path.roundRect(state.ctm, x, y, w, h, radii);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 그리기 파이프라인
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the Java2D paint of a style for the current matrix, tainting the canvas when a pattern of foreign pixels is used.
     * <p>
     * 스타일의 현재 행렬에 대한 Java2D 페인트를 반환한다. 외부 화소의 패턴을 쓰면 캔버스를 오염시킨다.
     *
     * @param style the style / 스타일
     * @return the paint, or null if nothing can be painted (a gradient or pattern with a matrix that cannot be inverted) / 페인트. 칠할 수 없으면(뒤집을 수 없는 행렬의 그라디언트나 패턴) null
     */
    private Paint paintOf(Object style) {
        if (style instanceof Integer) {
            return new Color(((Integer) style).intValue(), true);
        }
        if (style instanceof CanvasGradient) {
            return ((CanvasGradient) style).toPaint(state.ctm);
        }
        if (style instanceof CanvasPattern) {
            CanvasPattern p = (CanvasPattern) style;
            // 외부 화소를 가진 패턴으로 칠하면 캔버스가 오염된다.
            if (!p.isOriginClean()) {
                surface.taint();
            }
            return p.toPaint(state.ctm, state.smoothing);
        }
        return null;
    }

    /**
     * Gives a graphics the interpolation of the image smoothing attributes.
     * <p>
     * 그래픽스에 이미지 부드럽게 하기 속성의 보간을 준다.
     *
     * @param g the graphics / 그래픽스
     */
    private void applySmoothing(Graphics2D g) {
        Object v;
        if (!state.smoothing) {
            v = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        } else if ("high".equals(state.smoothingQuality)) {
            v = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        } else {
            v = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, v);
    }

    /**
     * Paints something and composites it onto the bitmap as the drawing model describes.
     * <p>
     * 그리기 모델에 따라 무언가를 칠하고 비트맵에 합성한다.
     *
     * @param bounds  the area that the painter touches in bitmap coordinates / 칠하는 쪽이 건드리는 비트맵 좌표의 영역
     * @param painter the painter / 칠하는 쪽
     * @param op      the compositing operator / 합성 연산자
     * @param globalAlpha the global alpha / 전역 알파
     * @param shadows true if the shadow attributes and the filter apply / 그림자 속성과 필터를 적용하면 true
     */
    private void render(Rectangle2D bounds, Painter painter, int op, double globalAlpha, boolean shadows) {
        CanvasBitmap bm = surface.bitmap();
        if (bm.isEmpty()) {
            return;
        }
        double alpha = globalAlpha;
        DrawState s = state;
        boolean shadow = shadows && s.hasShadow();
        boolean maskClip = s.clip != null && s.clipRect == null;
        FilterSpec filter = shadows ? s.filterSpec : null;
        // 그림자도 필터도 마스크 클립도 없는 source-over 는 비트맵에 바로 칠한다.
        if (op == Composites.SOURCE_OVER && !shadow && !maskClip && filter == null) {
            Graphics2D g = bm.graphics();
            g.setTransform(new AffineTransform());
            g.setClip(s.clipRect);
            g.setComposite(alpha >= 1 ? AlphaComposite.SrcOver : AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
            applySmoothing(g);
            try {
                painter.paint(g);
            } finally {
                g.setClip(null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            surface.markChanged();
            return;
        }
        int w = bm.getWidth();
        int h = bm.getHeight();
        Rectangle full = new Rectangle(0, 0, w, h);
        Rectangle grown = grow(bounds);
        // 필터가 있으면 흐림과 그림자가 번지는 거리만큼 레이어를 넓힌다(그린 것이 없으면 넓히지 않는다).
        if (filter != null && (grown.width > 0 || grown.height > 0)) {
            int[] m = CanvasFilters.margin(filter);
            grown = new Rectangle(grown.x - m[0], grown.y - m[1], grown.width + m[0] + m[2], grown.height + m[1] + m[3]);
        }
        Rectangle src = grown.intersection(full);
        boolean whole = Composites.affectsWholeCanvas(op);
        Rectangle clipBox = full;
        // 클립이 있으면 그 경계 안으로 합성 범위를 줄인다.
        if (s.clipRect != null) {
            clipBox = s.clipRect.intersection(full);
        } else if (s.clip != null) {
            clipBox = s.clip.getBounds().intersection(full);
        }
        if (clipBox.isEmpty() || (src.isEmpty() && !whole)) {
            surface.markChanged();
            return;
        }
        byte[] mask = maskClip ? clipMask(bm) : null;
        int[] layer = null;
        // 원본이 닿는 영역이 있으면 투명한 레이어에 칠한다.
        if (!src.isEmpty() && (long) src.width * src.height <= MAX_LAYER_PIXELS) {
            BufferedImage li = new BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = li.createGraphics();
            try {
                CanvasBitmap.configure(lg);
                applySmoothing(lg);
                lg.translate(-src.x, -src.y);
                painter.paint(lg);
            } finally {
                lg.dispose();
            }
            layer = ((DataBufferInt) li.getRaster().getDataBuffer()).getData();
        }
        // 그리기 모델: 전역 알파를 곱한 이미지 A 를 필터에 넣어 이미지 B 를 얻고, 그림자와 합성은 B 에서 한다(알파는 이미 곱했으므로 1 로 둔다).
        if (filter != null && layer != null) {
            if (alpha < 1) {
                // 레이어의 모든 화소에 전역 알파를 곱한다.
                for (int i = 0; i < layer.length; i++) {
                    if ((layer[i] >>> 24) != 0) {
                        layer[i] = CanvasColors.scaleAlpha(layer[i], alpha);
                    }
                }
                alpha = 1;
            }
            CanvasFilters.apply(layer, src.width, src.height, filter);
        }
        if (shadow && layer != null) {
            drawShadow(bm, layer, src, op, alpha, mask, clipBox, whole);
        }
        Rectangle region = whole ? clipBox : src.intersection(clipBox);
        compose(bm, layer, src, region, op, alpha, mask, whole);
        surface.markChanged();
    }

    /**
     * Grows a rectangle outwards to whole pixels with a margin for antialiasing.
     * <p>
     * 사각형을 안티앨리어싱 여백과 함께 바깥쪽의 정수 화소로 넓힌다.
     *
     * @param b the bounds / 경계
     * @return the integer rectangle / 정수 사각형
     */
    private static Rectangle grow(Rectangle2D b) {
        if (b.isEmpty() && b.getWidth() == 0 && b.getHeight() == 0) {
            return new Rectangle(0, 0, 0, 0);
        }
        double x0 = Math.max(-1e7, Math.floor(b.getMinX()) - 1);
        double y0 = Math.max(-1e7, Math.floor(b.getMinY()) - 1);
        double x1 = Math.min(1e7, Math.ceil(b.getMaxX()) + 1);
        double y1 = Math.min(1e7, Math.ceil(b.getMaxY()) + 1);
        return new Rectangle((int) x0, (int) y0, (int) Math.max(0, x1 - x0), (int) Math.max(0, y1 - y0));
    }

    /**
     * Returns the coverage mask of the clip over the whole bitmap, making it when needed.
     * <p>
     * 비트맵 전체에 대한 클립의 덮는 비율 마스크를 반환한다. 필요하면 만든다.
     *
     * @param bm the bitmap / 비트맵
     * @return one byte per pixel / 화소당 바이트 하나
     */
    private byte[] clipMask(CanvasBitmap bm) {
        DrawState s = state;
        if (s.clipMask == null) {
            BufferedImage mi = new BufferedImage(bm.getWidth(), bm.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D mg = mi.createGraphics();
            try {
                CanvasBitmap.configure(mg);
                mg.setColor(Color.WHITE);
                mg.fill(s.clip);
            } finally {
                mg.dispose();
            }
            int[] p = ((DataBufferInt) mi.getRaster().getDataBuffer()).getData();
            byte[] m = new byte[p.length];
            // 알파 채널이 곧 덮는 비율이다.
            for (int i = 0; i < p.length; i++) {
                m[i] = (byte) (p[i] >>> 24);
            }
            s.clipMask = m;
        }
        return s.clipMask;
    }

    /**
     * Composites a layer onto the bitmap pixel by pixel.
     * <p>
     * 레이어를 화소별로 비트맵에 합성한다.
     *
     * @param bm     the bitmap / 비트맵
     * @param layer  the layer pixels (or null for a fully transparent source) / 레이어 화소(완전히 투명한 원본이면 null)
     * @param lr     the area of the layer in bitmap coordinates / 레이어의 비트맵 좌표 영역
     * @param region the area to composite / 합성할 영역
     * @param op     the operator / 연산자
     * @param alpha  the global alpha / 전역 알파
     * @param mask   the clip coverage or null / 클립 비율 또는 null
     * @param whole  true if transparent source pixels matter / 투명한 원본 화소도 의미가 있으면 true
     */
    private static void compose(CanvasBitmap bm, int[] layer, Rectangle lr, Rectangle region, int op, double alpha, byte[] mask, boolean whole) {
        int[] px = bm.pixels();
        int w = bm.getWidth();
        // 영역의 화소를 하나씩 합성한다.
        for (int y = region.y; y < region.y + region.height; y++) {
            for (int x = region.x; x < region.x + region.width; x++) {
                int s = 0;
                if (layer != null && x >= lr.x && y >= lr.y && x < lr.x + lr.width && y < lr.y + lr.height) {
                    s = layer[(y - lr.y) * lr.width + (x - lr.x)];
                }
                if (alpha < 1 && (s >>> 24) != 0) {
                    s = CanvasColors.scaleAlpha(s, alpha);
                }
                if ((s >>> 24) == 0 && !whole) {
                    continue;
                }
                int i = y * w + x;
                int cov = mask == null ? 255 : (mask[i] & 0xFF);
                if (cov == 0) {
                    continue;
                }
                int d = px[i];
                int r = Composites.apply(op, s, d);
                px[i] = cov == 255 ? r : Composites.mix(d, r, cov);
            }
        }
    }

    /**
     * Draws the shadow of a layer: its alpha, offset, blurred and colored with the shadow color.
     * <p>
     * 레이어의 그림자를 그린다: 알파를 오프셋하고 번지게 하여 그림자 색으로 칠한다.
     *
     * @param bm      the bitmap / 비트맵
     * @param layer   the layer / 레이어
     * @param src     the area of the layer / 레이어의 영역
     * @param op      the operator / 연산자
     * @param alpha   the global alpha / 전역 알파
     * @param mask    the clip coverage or null / 클립 비율 또는 null
     * @param clipBox the bounds of the clip / 클립의 경계
     * @param whole   true if transparent source pixels matter / 투명한 원본 화소도 의미가 있으면 true
     */
    private void drawShadow(CanvasBitmap bm, int[] layer, Rectangle src, int op, double alpha, byte[] mask, Rectangle clipBox, boolean whole) {
        DrawState s = state;
        double blur = Math.min(s.shadowBlur, CanvasLimits.MAX_SHADOW_BLUR);
        int ext = ShadowBlur.extent(blur);
        int ox = (int) Math.round(s.shadowOffsetX);
        int oy = (int) Math.round(s.shadowOffsetY);
        int sw = src.width + 2 * ext;
        int sh = src.height + 2 * ext;
        // 번짐 버퍼가 너무 크거나 번짐 계산량이 너무 많으면 이 그림자는 그리지 않는다.
        if ((long) sw * sh > MAX_SHADOW_CELLS || (long) sw * sh * (2L * ext + 1) * 2 > MAX_BLUR_WORK) {
            return;
        }
        float[] a = new float[sw * sh];
        // 원본 알파를 번짐 버퍼의 가운데에 옮긴다.
        for (int y = 0; y < src.height; y++) {
            for (int x = 0; x < src.width; x++) {
                a[(y + ext) * sw + (x + ext)] = layer[y * src.width + x] >>> 24;
            }
        }
        ShadowBlur.blur(a, sw, sh, blur);
        double shadowAlpha = (s.shadowColor >>> 24) / 255.0;
        int rgb = s.shadowColor & 0xFFFFFF;
        int[] pix = new int[sw * sh];
        // 번진 알파에 그림자 색의 알파를 곱해 그림자 화소를 만든다.
        for (int i = 0; i < pix.length; i++) {
            int v = (int) Math.round(a[i] * shadowAlpha);
            pix[i] = (Math.min(255, v) << 24) | rgb;
        }
        Rectangle shadowRect = new Rectangle(src.x - ext + ox, src.y - ext + oy, sw, sh);
        Rectangle full = new Rectangle(0, 0, bm.getWidth(), bm.getHeight());
        Rectangle region = whole ? clipBox : shadowRect.intersection(clipBox).intersection(full);
        compose(bm, pix, shadowRect, region, op, alpha, mask, whole);
    }

    /**
     * Fills a device-space shape with a style.
     * <p>
     * 장치 좌표의 도형을 스타일로 채운다.
     *
     * @param shape the shape / 도형
     * @param style the style / 스타일
     */
    private void paintShape(final Shape shape, Object style) {
        final Paint paint = paintOf(style);
        if (paint == null || shape == null) {
            return;
        }
        render(shape.getBounds2D(), new Painter() {
            /**
             * Fills the shape.
             * <p>
             * 도형을 채운다.
             *
             * @param g the graphics / 그래픽스
             */
            public void paint(Graphics2D g) {
                g.setPaint(paint);
                g.fill(shape);
            }
        }, state.composite, state.globalAlpha, true);
    }

    /**
     * Builds the Java2D stroke of the current line attributes.
     * <p>
     * 현재 선 속성의 Java2D 선을 만든다.
     *
     * @return the stroke / 선
     */
    private BasicStroke makeStroke() {
        DrawState s = state;
        float[] dash = null;
        float phase = 0;
        // 점선이 있고 길이의 합이 0 보다 크면 점선 배열과 시작 위치를 만든다.
        if (s.lineDash != null) {
            double total = 0;
            // 점선 길이를 모두 더한다.
            for (int i = 0; i < s.lineDash.length; i++) {
                total += s.lineDash[i];
            }
            if (total > 0) {
                dash = new float[s.lineDash.length];
                // 점선 길이를 float 배열로 옮긴다.
                for (int i = 0; i < dash.length; i++) {
                    dash[i] = (float) s.lineDash[i];
                }
                double p = s.lineDashOffset % total;
                phase = (float) (p < 0 ? p + total : p);
            }
        }
        return new BasicStroke((float) s.lineWidth, s.lineCap, s.lineJoin, (float) Math.max(1.0, s.miterLimit), dash, phase);
    }

    /**
     * Computes the stroked outline of a device-space path under the current matrix and line attributes.
     * <p>
     * 현재 행렬과 선 속성으로 장치 좌표 경로의 선 윤곽을 계산한다.
     *
     * @param devicePath the path in device space / 장치 좌표의 경로
     * @return the outline in device space, or null if the matrix cannot be inverted / 장치 좌표의 윤곽. 행렬을 뒤집을 수 없으면 null
     */
    private Shape outlineOf(Shape devicePath) {
        AffineTransform inverse;
        try {
            inverse = state.ctm.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        Shape user = inverse.createTransformedShape(devicePath);
        Shape stroked = makeStroke().createStrokedShape(user);
        return state.ctm.createTransformedShape(stroked);
    }

    /**
     * Fills the current path.
     * <p>
     * 현재 경로를 채운다.
     *
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     */
    public void fill(boolean evenOdd) {
        if (path.isEmpty()) {
            return;
        }
        paintShape(path.copy(evenOdd), state.fillStyle);
    }

    /**
     * Strokes the current path.
     * <p>
     * 현재 경로를 선으로 긋는다.
     */
    public void stroke() {
        if (path.isEmpty()) {
            return;
        }
        paintShape(outlineOf(path.copy(false)), state.strokeStyle);
    }

    /**
     * Draws a focus ring around a path (<code>drawFocusIfNeeded()</code> calls this once it has decided that the element is focused): a one pixel
     * dark ring with a one pixel white halo on both sides, so it shows on any background, the way the focus rings of the page look like
     * <code>outline: auto</code>. As the specification asks, the ring is subject to the transformation matrix and the clipping region but not to
     * the shadow, the global alpha, the compositing operator, the filter or any of the styles.
     * <p>
     * 경로 둘레에 포커스 링을 그린다(<code>drawFocusIfNeeded()</code> 가 요소가 포커스를 가졌다고 판단하면 이것을 부른다): 1 화소 두께의 어두운 링과 양쪽의 1 화소 흰
     * 후광으로, 어떤 배경에서도 보이며 페이지의 포커스 링(<code>outline: auto</code>)처럼 보인다. 규격이 요구하는 대로 링에는 변환 행렬과 클리핑 영역이 적용되고 그림자,
     * 전역 알파, 합성 연산자, 필터, 어떤 스타일도 적용되지 않는다.
     *
     * @param p the path, or null for the current default path / 경로. 현재 기본 경로이면 null
     */
    public void drawFocusRing(Path2DValue p) {
        Shape shape;
        // 현재 기본 경로는 이미 장치 좌표이고, Path2D 는 현재 변환 행렬로 옮긴다.
        if (p == null) {
            if (path.isEmpty()) {
                return;
            }
            shape = path.copy(false);
        } else {
            if (p.path().isEmpty()) {
                return;
            }
            shape = state.ctm.createTransformedShape(p.path().copy(false));
        }
        final Shape halo = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f).createStrokedShape(shape);
        final Shape ring = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f).createStrokedShape(shape);
        render(halo.getBounds2D(), new Painter() {
            /**
             * Fills the halo and then the ring.
             * <p>
             * 후광을 채운 뒤 링을 채운다.
             *
             * @param g the graphics / 그래픽스
             */
            public void paint(Graphics2D g) {
                g.setPaint(Color.WHITE);
                g.fill(halo);
                g.setPaint(FOCUS_RING);
                g.fill(ring);
            }
        }, Composites.SOURCE_OVER, 1, false);
    }

    /**
     * The color of the focus ring, a dark gray.
     * <p>
     * 포커스 링의 색. 어두운 회색.
     */
    private static final Color FOCUS_RING = new Color(16, 16, 16);

    /**
     * Intersects the clipping region with the current path.
     * <p>
     * 클리핑 영역을 현재 경로와 교차시킨다.
     *
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     */
    public void clip(boolean evenOdd) {
        clipShape(path.copy(evenOdd));
    }

    /**
     * Intersects the clipping region with a path of a <code>Path2D</code> (its points are in user space and the current matrix applies).
     * <p>
     * 클리핑 영역을 <code>Path2D</code> 의 경로와 교차시킨다(점은 사용자 좌표이며 현재 행렬이 적용된다).
     *
     * @param p       the path / 경로
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     */
    public void clipPath(Path2DValue p, boolean evenOdd) {
        clipShape(state.ctm.createTransformedShape(p.path().copy(evenOdd)));
    }

    /**
     * Fills a <code>Path2D</code> with the fill style.
     * <p>
     * <code>Path2D</code> 를 채우기 스타일로 채운다.
     *
     * @param p       the path / 경로
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     */
    public void fillPath(Path2DValue p, boolean evenOdd) {
        if (!p.path().isEmpty()) {
            paintShape(state.ctm.createTransformedShape(p.path().copy(evenOdd)), state.fillStyle);
        }
    }

    /**
     * Strokes a <code>Path2D</code> with the stroke style.
     * <p>
     * <code>Path2D</code> 를 선 스타일로 긋는다.
     *
     * @param p the path / 경로
     */
    public void strokePath(Path2DValue p) {
        if (!p.path().isEmpty()) {
            paintShape(outlineOf(state.ctm.createTransformedShape(p.path().copy(false))), state.strokeStyle);
        }
    }

    /**
     * Tells whether a point lies inside a <code>Path2D</code> (the current matrix applies to the path, not to the point).
     * <p>
     * 점이 <code>Path2D</code> 안에 있는지 알려 준다(현재 행렬은 점이 아니라 경로에 적용한다).
     *
     * @param p       the path / 경로
     * @param x       x in canvas coordinates / 캔버스 좌표의 x
     * @param y       y in canvas coordinates / 캔버스 좌표의 y
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     * @return true if inside / 안이면 true
     */
    public boolean isPointInPath(Path2DValue p, double x, double y, boolean evenOdd) {
        return finite(x, y) && !p.path().isEmpty() && state.ctm.createTransformedShape(p.path().copy(evenOdd)).contains(x, y);
    }

    /**
     * Tells whether a point lies on the stroke of a <code>Path2D</code>.
     * <p>
     * 점이 <code>Path2D</code> 의 선 위에 있는지 알려 준다.
     *
     * @param p the path / 경로
     * @param x x in canvas coordinates / 캔버스 좌표의 x
     * @param y y in canvas coordinates / 캔버스 좌표의 y
     * @return true if on the stroke / 선 위이면 true
     */
    public boolean isPointInStroke(Path2DValue p, double x, double y) {
        if (!finite(x, y) || p.path().isEmpty()) {
            return false;
        }
        Shape o = outlineOf(state.ctm.createTransformedShape(p.path().copy(false)));
        return o != null && o.contains(x, y);
    }

    /**
     * Intersects the clipping region with a shape in device space.
     * <p>
     * 클리핑 영역을 장치 좌표의 도형과 교차시킨다.
     *
     * @param shape the shape / 도형
     */
    private void clipShape(Shape shape) {
        Area a = new Area(shape);
        DrawState s = state;
        if (s.clip != null) {
            a.intersect(s.clip);
        }
        s.clip = a;
        s.clipMask = null;
        s.clipRect = null;
        // 클립이 정수 좌표의 사각형이면 값싼 사각형 클립으로 쓴다.
        if (a.isEmpty()) {
            s.clipRect = new Rectangle(0, 0, 0, 0);
        } else if (a.isRectangular()) {
            Rectangle2D b = a.getBounds2D();
            if (b.getX() == Math.rint(b.getX()) && b.getY() == Math.rint(b.getY()) && b.getWidth() == Math.rint(b.getWidth())
                    && b.getHeight() == Math.rint(b.getHeight())) {
                s.clipRect = new Rectangle((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
            }
        }
    }

    /**
     * Tells whether a point of the bitmap lies inside the current path.
     * <p>
     * 비트맵의 한 점이 현재 경로 안에 있는지 알려 준다.
     *
     * @param x       x in canvas coordinates (not transformed) / 캔버스 좌표의 x(변환되지 않음)
     * @param y       y in canvas coordinates (not transformed) / 캔버스 좌표의 y(변환되지 않음)
     * @param evenOdd true for the even-odd rule / 짝홀 규칙이면 true
     * @return true if inside / 안이면 true
     */
    public boolean isPointInPath(double x, double y, boolean evenOdd) {
        return finite(x, y) && !path.isEmpty() && path.copy(evenOdd).contains(x, y);
    }

    /**
     * Tells whether a point of the bitmap lies on the stroke of the current path.
     * <p>
     * 비트맵의 한 점이 현재 경로의 선 위에 있는지 알려 준다.
     *
     * @param x x in canvas coordinates (not transformed) / 캔버스 좌표의 x(변환되지 않음)
     * @param y y in canvas coordinates (not transformed) / 캔버스 좌표의 y(변환되지 않음)
     * @return true if on the stroke / 선 위이면 true
     */
    public boolean isPointInStroke(double x, double y) {
        if (!finite(x, y) || path.isEmpty()) {
            return false;
        }
        Shape o = outlineOf(path.copy(false));
        return o != null && o.contains(x, y);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 사각형
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Makes the device-space polygon of a user-space rectangle.
     * <p>
     * 사용자 좌표 사각형의 장치 좌표 다각형을 만든다.
     *
     * @param x x / x
     * @param y y / y
     * @param w width / 너비
     * @param h height / 높이
     * @return the closed polygon / 닫힌 다각형
     */
    private Path2D.Double rectPath(double x, double y, double w, double h) {
        PathBuilder b = new PathBuilder();
        b.rect(state.ctm, x, y, w, h);
        return (Path2D.Double) b.copy(false);
    }

    /**
     * Fills a rectangle without touching the current path.
     * <p>
     * 현재 경로를 건드리지 않고 사각형을 채운다.
     *
     * @param x x / x
     * @param y y / y
     * @param w width / 너비
     * @param h height / 높이
     */
    public void fillRect(double x, double y, double w, double h) {
        if (!finite(x, y, w, h) || w == 0 || h == 0) {
            return;
        }
        paintShape(rectPath(x, y, w, h), state.fillStyle);
    }

    /**
     * Strokes a rectangle without touching the current path.
     * <p>
     * 현재 경로를 건드리지 않고 사각형을 선으로 긋는다.
     *
     * @param x x / x
     * @param y y / y
     * @param w width / 너비
     * @param h height / 높이
     */
    public void strokeRect(double x, double y, double w, double h) {
        if (!finite(x, y, w, h) || (w == 0 && h == 0)) {
            return;
        }
        paintShape(outlineOf(rectPath(x, y, w, h)), state.strokeStyle);
    }

    /**
     * Makes a rectangle transparent black (ignoring the global alpha, the shadow and the compositing operator).
     * <p>
     * 사각형을 투명한 검정으로 만든다(전역 알파, 그림자, 합성 연산자는 무시한다).
     *
     * @param x x / x
     * @param y y / y
     * @param w width / 너비
     * @param h height / 높이
     */
    public void clearRect(double x, double y, double w, double h) {
        if (!finite(x, y, w, h) || w == 0 || h == 0) {
            return;
        }
        final Path2D.Double shape = rectPath(x, y, w, h);
        CanvasBitmap bm = surface.bitmap();
        if (bm.isEmpty()) {
            return;
        }
        DrawState s = state;
        // 마스크 클립이 없으면 Clear 합성으로 바로 지우고, 있으면 destination-out 레이어로 지운다.
        if (s.clip == null || s.clipRect != null) {
            Graphics2D g = bm.graphics();
            g.setTransform(new AffineTransform());
            g.setClip(s.clipRect);
            g.setComposite(AlphaComposite.Clear);
            try {
                g.fill(shape);
            } finally {
                g.setClip(null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            surface.markChanged();
            return;
        }
        render(shape.getBounds2D(), new Painter() {
            /**
             * Fills the rectangle with opaque black, which destination-out turns into a hole.
             * <p>
             * 사각형을 불투명한 검정으로 채운다. destination-out 이 이것을 구멍으로 바꾼다.
             *
             * @param g the graphics / 그래픽스
             */
            public void paint(Graphics2D g) {
                g.setPaint(Color.BLACK);
                g.fill(shape);
            }
        }, Composites.DESTINATION_OUT, 1, false);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 텍스트
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Fills a text.
     * <p>
     * 텍스트를 채운다.
     *
     * @param text     the text / 텍스트
     * @param x        anchor x / 기준점 x
     * @param y        anchor y / 기준점 y
     * @param maxWidth the maximum width, NaN for none / 최대 너비. 없으면 NaN
     */
    public void fillText(String text, double x, double y, double maxWidth) {
        Shape outline = textOutline(text, x, y, maxWidth);
        if (outline != null) {
            paintShape(outline, state.fillStyle);
        }
    }

    /**
     * Strokes a text.
     * <p>
     * 텍스트를 선으로 긋는다.
     *
     * @param text     the text / 텍스트
     * @param x        anchor x / 기준점 x
     * @param y        anchor y / 기준점 y
     * @param maxWidth the maximum width, NaN for none / 최대 너비. 없으면 NaN
     */
    public void strokeText(String text, double x, double y, double maxWidth) {
        Shape outline = textOutline(text, x, y, maxWidth);
        if (outline != null) {
            paintShape(outlineOf(outline), state.strokeStyle);
        }
    }

    /**
     * Lays a text out and returns its outline in device space.
     * <p>
     * 텍스트를 배치하고 장치 좌표의 윤곽을 반환한다.
     *
     * @param text     the text / 텍스트
     * @param x        anchor x / 기준점 x
     * @param y        anchor y / 기준점 y
     * @param maxWidth the maximum width, NaN for none / 최대 너비. 없으면 NaN
     * @return the outline, or null if there is nothing to draw / 윤곽. 그릴 것이 없으면 null
     */
    private Shape textOutline(String text, double x, double y, double maxWidth) {
        if (!finite(x, y) || (maxWidth == maxWidth && maxWidth <= 0)) {
            return null;
        }
        CanvasText.Laid laid = CanvasText.layout(text, state, maxWidth);
        if (laid.outline == null) {
            return null;
        }
        AffineTransform at = new AffineTransform(state.ctm);
        at.translate(x, y);
        return at.createTransformedShape(laid.outline);
    }

    /**
     * Measures a text.
     * <p>
     * 텍스트를 측정한다.
     *
     * @param text the text / 텍스트
     * @return the metrics / 측정값
     */
    public TextMetricsValue measureText(String text) {
        return new TextMetricsValue(CanvasText.layout(text, state, Double.NaN).metrics);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 이미지
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Draws an image or another canvas. The source rectangle is clipped to the image (the destination is clipped by the same fraction), a
     * negative source or destination width or height only names the rectangle on the other side of the point (the specification processes the
     * image in its original direction, so it is not mirrored), and nothing is drawn for an empty rectangle. A source that is not origin-clean
     * taints the canvas.
     * <p>
     * 이미지나 다른 캔버스를 그린다. 원본 사각형은 이미지 안으로 자르고(대상도 같은 비율로 자른다), 원본이나 대상의 음수 너비와 높이는 점의 반대쪽 사각형을 가리킬
     * 뿐이며(규격은 이미지를 원래 방향으로 처리하라고 하므로 뒤집지 않는다), 빈 사각형이면 아무것도 그리지 않는다. origin-clean 이 아닌 원본은 캔버스를
     * 오염시킨다.
     *
     * @param img the source pixels / 원본 화소
     * @param sx  source x / 원본 x
     * @param sy  source y / 원본 y
     * @param sw  source width / 원본 너비
     * @param sh  source height / 원본 높이
     * @param dx  destination x / 대상 x
     * @param dy  destination y / 대상 y
     * @param dw  destination width / 대상 너비
     * @param dh  destination height / 대상 높이
     */
    public void drawImage(CanvasImage img, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh) {
        if (!finite(sx, sy, sw, sh, dx, dy, dw, dh) || img.getWidth() <= 0 || img.getHeight() <= 0) {
            return;
        }
        double x0 = sx;
        double y0 = sy;
        double w0 = sw;
        double h0 = sh;
        double x1 = dx;
        double y1 = dy;
        double w1 = dw;
        double h1 = dh;
        // 음수 너비와 높이는 시작점을 옮기고 부호를 없애 정규화한다(규격: 이미지는 원래 방향으로 처리하며 뒤집지 않는다).
        if (w0 < 0) {
            x0 += w0;
            w0 = -w0;
        }
        if (h0 < 0) {
            y0 += h0;
            h0 = -h0;
        }
        if (w1 < 0) {
            x1 += w1;
            w1 = -w1;
        }
        if (h1 < 0) {
            y1 += h1;
            h1 = -h1;
        }
        if (w0 == 0 || h0 == 0 || w1 == 0 || h1 == 0) {
            return;
        }
        // 원본 사각형에서 대상 사각형으로의 배율.
        double fx = w1 / w0;
        double fy = h1 / h0;
        // 원본 사각형을 이미지 안으로 자른다.
        double cx0 = Math.max(0, x0);
        double cy0 = Math.max(0, y0);
        double cx1 = Math.min(img.getWidth(), x0 + w0);
        double cy1 = Math.min(img.getHeight(), y0 + h0);
        if (cx0 >= cx1 || cy0 >= cy1) {
            return;
        }
        // 원본 사각형을 자른 비율대로 대상 사각형도 자른다.
        double ax = x1 + (cx0 - x0) * fx;
        double bx = x1 + (cx1 - x0) * fx;
        double ay = y1 + (cy0 - y0) * fy;
        double by = y1 + (cy1 - y0) * fy;
        if (!img.isOriginClean()) {
            surface.taint();
        }
        final BufferedImage bi = img.toBufferedImage();
        AffineTransform t = new AffineTransform(state.ctm);
        t.translate(x1, y1);
        t.scale(fx, fy);
        t.translate(-x0, -y0);
        final AffineTransform transform = t;
        final Shape dest = state.ctm.createTransformedShape(new Rectangle2D.Double(Math.min(ax, bx), Math.min(ay, by), Math.abs(bx - ax),
                Math.abs(by - ay)));
        render(dest.getBounds2D(), new Painter() {
            /**
             * Draws the image through the destination rectangle.
             * <p>
             * 대상 사각형을 통해 이미지를 그린다.
             *
             * @param g the graphics / 그래픽스
             */
            public void paint(Graphics2D g) {
                g.clip(dest);
                g.drawImage(bi, transform, null);
            }
        }, state.composite, state.globalAlpha, true);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 화소 데이터
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads pixels from the bitmap. Pixels outside the bitmap are transparent black.
     * <p>
     * 비트맵에서 화소를 읽는다. 비트맵 밖의 화소는 투명한 검정이다.
     *
     * @param sx x of the rectangle / 사각형의 x
     * @param sy y of the rectangle / 사각형의 y
     * @param sw width (may be negative) / 너비(음수 가능)
     * @param sh height (may be negative) / 높이(음수 가능)
     * @return the pixels / 화소
     * @throws DomException IndexSizeError if the width or height is zero, SecurityError if the canvas is tainted, InvalidStateError if the bitmap could not be allocated / 너비나 높이가 0 이면 IndexSizeError, 캔버스가 오염되었으면 SecurityError, 비트맵을 할당하지 못했으면 InvalidStateError
     */
    public RawImage getImageData(int sx, int sy, int sw, int sh) {
        if (sw == 0 || sh == 0) {
            throw DomException.indexSize("The source width or height is 0.");
        }
        if (!surface.isOriginClean()) {
            throw DomException.security("The canvas has been tainted by cross-origin data.");
        }
        surface.flattenIfOpaque();
        CanvasBitmap bm = surface.bitmap();
        if (bm.isTooLarge()) {
            throw DomException.invalidState("The canvas is too large to read.");
        }
        int x = sx;
        int y = sy;
        int w = sw;
        int h = sh;
        // 음수 너비와 높이는 시작점을 옮기고 부호를 없앤다.
        if (w < 0) {
            x += w;
            w = -w;
        }
        if (h < 0) {
            y += h;
            h = -h;
        }
        if ((long) w * h > CanvasLimits.MAX_IMAGE_DATA_PIXELS) {
            throw DomException.indexSize("The requested area is too large.");
        }
        byte[] data = new byte[w * h * 4];
        int[] px = bm.pixels();
        int bw = bm.getWidth();
        int bh = bm.getHeight();
        // 비트맵 안에 있는 화소만 RGBA 바이트로 옮긴다. 밖은 0 으로 남는다.
        for (int j = 0; j < h; j++) {
            int by = y + j;
            if (by < 0 || by >= bh) {
                continue;
            }
            // 행의 화소를 하나씩 옮긴다.
            for (int i = 0; i < w; i++) {
                int bx = x + i;
                if (bx < 0 || bx >= bw) {
                    continue;
                }
                int p = px[by * bw + bx];
                int o = (j * w + i) * 4;
                data[o] = (byte) (p >> 16);
                data[o + 1] = (byte) (p >> 8);
                data[o + 2] = (byte) p;
                data[o + 3] = (byte) (p >>> 24);
            }
        }
        return new RawImage(w, h, data);
    }

    /**
     * Writes pixels into the bitmap, replacing what is there: the matrix, the clip, the global alpha, the shadow and the compositing operator
     * do not apply.
     * <p>
     * 화소를 비트맵에 써서 그 자리를 교체한다: 행렬, 클립, 전역 알파, 그림자, 합성 연산자는 적용되지 않는다.
     *
     * @param data   the RGBA bytes of the image data / 이미지 데이터의 RGBA 바이트
     * @param iw     the width of the image data / 이미지 데이터의 너비
     * @param ih     the height of the image data / 이미지 데이터의 높이
     * @param dx     destination x / 대상 x
     * @param dy     destination y / 대상 y
     * @param dirtyX x of the dirty rectangle / 변경 사각형의 x
     * @param dirtyY y of the dirty rectangle / 변경 사각형의 y
     * @param dirtyW width of the dirty rectangle (may be negative) / 변경 사각형의 너비(음수 가능)
     * @param dirtyH height of the dirty rectangle (may be negative) / 변경 사각형의 높이(음수 가능)
     */
    public void putImageData(byte[] data, int iw, int ih, int dx, int dy, int dirtyX, int dirtyY, int dirtyW, int dirtyH) {
        CanvasBitmap bm = surface.bitmap();
        if (bm.isEmpty()) {
            return;
        }
        int x0 = dirtyX;
        int y0 = dirtyY;
        int w0 = dirtyW;
        int h0 = dirtyH;
        // 음수 너비와 높이는 시작점을 옮기고 부호를 없앤다.
        if (w0 < 0) {
            x0 += w0;
            w0 = -w0;
        }
        if (h0 < 0) {
            y0 += h0;
            h0 = -h0;
        }
        int xa = Math.max(0, x0);
        int ya = Math.max(0, y0);
        int xb = (int) Math.min((long) iw, (long) x0 + w0);
        int yb = (int) Math.min((long) ih, (long) y0 + h0);
        int[] px = bm.pixels();
        int bw = bm.getWidth();
        int bh = bm.getHeight();
        // 변경 사각형 안의 화소를 비트맵 범위 안에서 그대로 덮어쓴다.
        for (int j = ya; j < yb; j++) {
            int by = dy + j;
            if (by < 0 || by >= bh) {
                continue;
            }
            // 행의 화소를 하나씩 덮어쓴다.
            for (int i = xa; i < xb; i++) {
                int bx = dx + i;
                if (bx < 0 || bx >= bw) {
                    continue;
                }
                int o = (j * iw + i) * 4;
                px[by * bw + bx] = ((data[o + 3] & 0xFF) << 24) | ((data[o] & 0xFF) << 16) | ((data[o + 1] & 0xFF) << 8) | (data[o + 2] & 0xFF);
            }
        }
        surface.markChanged();
    }
}
