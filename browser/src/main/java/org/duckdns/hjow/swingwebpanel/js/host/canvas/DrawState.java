package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;

/**
 * The drawing state of a 2D context, the part that <code>save()</code> and <code>restore()</code> push and pop: the transformation matrix, the
 * clipping region, the styles, the line, shadow, compositing, text and image smoothing attributes. The clipping region ({@link #clip}) and its
 * mask are never modified once stored, so a copy shares them. The current path is not part of the state.
 * <p>
 * 2D 컨텍스트의 그리기 상태, 즉 <code>save()</code> 와 <code>restore()</code> 가 넣고 꺼내는 부분: 변환 행렬, 클리핑 영역, 스타일, 선/그림자/합성/텍스트/
 * 이미지 부드럽게 하기 속성. 클리핑 영역({@link #clip})과 그 마스크는 한 번 저장하면 바뀌지 않으므로 복사본이 공유한다. 현재 경로는 상태에 속하지 않는다.
 */
final class DrawState {

    /**
     * The current transformation matrix (user space to bitmap).
     * <p>
     * 현재 변환 행렬(사용자 좌표에서 비트맵으로).
     */
    AffineTransform ctm = new AffineTransform();

    /**
     * The clipping region in device space, or null for no clipping.
     * <p>
     * 장치 좌표의 클리핑 영역. 클리핑이 없으면 null.
     */
    Area clip;

    /**
     * The clipping region as an integer rectangle when it is one (drawing can then use the cheap rectangular clip), else null.
     * <p>
     * 클리핑 영역이 정수 사각형이면 그 사각형(그러면 값싼 사각형 클립을 쓸 수 있다). 아니면 null.
     */
    Rectangle clipRect;

    /**
     * The antialiased coverage of the clipping region over the whole bitmap (0..255 per pixel), computed when first needed, or null.
     * <p>
     * 비트맵 전체에 대한 클리핑 영역의 안티앨리어싱된 덮는 비율(화소당 0..255). 처음 필요할 때 계산한다. 없으면 null.
     */
    byte[] clipMask;

    /**
     * The fill style: an <code>Integer</code> ARGB color, a {@link CanvasGradient} or a {@link CanvasPattern}.
     * <p>
     * 채우기 스타일: <code>Integer</code> ARGB 색, {@link CanvasGradient}, {@link CanvasPattern} 중 하나.
     */
    Object fillStyle = Integer.valueOf(0xFF000000);

    /**
     * The stroke style, of the same kinds as {@link #fillStyle}.
     * <p>
     * 선 스타일. {@link #fillStyle} 과 같은 종류.
     */
    Object strokeStyle = Integer.valueOf(0xFF000000);

    /**
     * The line width in user space units.
     * <p>
     * 선 너비(사용자 좌표 단위).
     */
    double lineWidth = 1;

    /**
     * The line cap as a <code>BasicStroke</code> constant (0 butt, 1 round, 2 square).
     * <p>
     * <code>BasicStroke</code> 상수인 선 끝 모양(0 butt, 1 round, 2 square).
     */
    int lineCap = 0;

    /**
     * The line join as a <code>BasicStroke</code> constant (0 miter, 1 round, 2 bevel).
     * <p>
     * <code>BasicStroke</code> 상수인 선 꺾임 모양(0 miter, 1 round, 2 bevel).
     */
    int lineJoin = 0;

    /**
     * The miter limit.
     * <p>
     * 마이터 한계.
     */
    double miterLimit = 10;

    /**
     * The line dash pattern (always of even length), or null for a solid line.
     * <p>
     * 선 점선 패턴(길이는 항상 짝수). 실선이면 null.
     */
    double[] lineDash;

    /**
     * The line dash offset.
     * <p>
     * 선 점선 오프셋.
     */
    double lineDashOffset;

    /**
     * The global alpha 0..1.
     * <p>
     * 전역 알파 0..1.
     */
    double globalAlpha = 1;

    /**
     * The compositing operator number, see {@link Composites}.
     * <p>
     * 합성 연산자 번호. {@link Composites} 참고.
     */
    int composite = Composites.SOURCE_OVER;

    /**
     * The shadow color (ARGB), transparent black by default.
     * <p>
     * 그림자 색(ARGB). 기본은 투명한 검정.
     */
    int shadowColor;

    /**
     * The shadow blur.
     * <p>
     * 그림자 번짐.
     */
    double shadowBlur;

    /**
     * The shadow offset in x, in device pixels.
     * <p>
     * 그림자 x 오프셋(장치 화소).
     */
    double shadowOffsetX;

    /**
     * The shadow offset in y, in device pixels.
     * <p>
     * 그림자 y 오프셋(장치 화소).
     */
    double shadowOffsetY;

    /**
     * The font.
     * <p>
     * 글꼴.
     */
    CanvasFont font = CanvasFont.parse(CanvasFont.DEFAULT, 16);

    /**
     * The text alignment: start, end, left, right or center.
     * <p>
     * 텍스트 정렬: start, end, left, right, center.
     */
    String textAlign = "start";

    /**
     * The text baseline: top, hanging, middle, alphabetic, ideographic or bottom.
     * <p>
     * 텍스트 기준선: top, hanging, middle, alphabetic, ideographic, bottom.
     */
    String textBaseline = "alphabetic";

    /**
     * The text direction: ltr, rtl or inherit.
     * <p>
     * 텍스트 방향: ltr, rtl, inherit.
     */
    String direction = "inherit";

    /**
     * The spacing added after every character (<code>letterSpacing</code>), <code>0px</code> by default.
     * <p>
     * 모든 글자 뒤에 더하는 간격(<code>letterSpacing</code>). 기본은 <code>0px</code>.
     */
    TextSpacing letterSpacing = TextSpacing.ZERO;

    /**
     * The spacing added after every word separator (<code>wordSpacing</code>), <code>0px</code> by default.
     * <p>
     * 모든 낱말 구분 문자 뒤에 더하는 간격(<code>wordSpacing</code>). 기본은 <code>0px</code>.
     */
    TextSpacing wordSpacing = TextSpacing.ZERO;

    /**
     * The <code>fontKerning</code> keyword: auto, normal or none.
     * <p>
     * <code>fontKerning</code> 키워드: auto, normal, none.
     */
    String fontKerning = "auto";

    /**
     * The <code>fontStretch</code> keyword, <code>normal</code> by default.
     * <p>
     * <code>fontStretch</code> 키워드. 기본은 <code>normal</code>.
     */
    String fontStretch = "normal";

    /**
     * The <code>fontVariantCaps</code> keyword, <code>normal</code> by default.
     * <p>
     * <code>fontVariantCaps</code> 키워드. 기본은 <code>normal</code>.
     */
    String fontVariantCaps = "normal";

    /**
     * The <code>textRendering</code> keyword, <code>auto</code> by default.
     * <p>
     * <code>textRendering</code> 키워드. 기본은 <code>auto</code>.
     */
    String textRendering = "auto";

    /**
     * The <code>lang</code> value (a language tag, the empty string or <code>inherit</code>).
     * <p>
     * <code>lang</code> 값(언어 태그, 빈 문자열, <code>inherit</code>).
     */
    String lang = "inherit";

    /**
     * The <code>filter</code> string as it was set (<code>none</code> by default).
     * <p>
     * 설정한 그대로의 <code>filter</code> 문자열(기본은 <code>none</code>).
     */
    String filter = "none";

    /**
     * The filter functions of {@link #filter}, or null when nothing is filtered (<code>none</code>, or a reference that filters nothing). The
     * object never changes, so a copy shares it.
     * <p>
     * {@link #filter} 의 필터 함수. 아무것도 거르지 않으면(<code>none</code>이나 아무것도 거르지 않는 참조) null. 객체는 바뀌지 않으므로 복사본이 공유한다.
     */
    FilterSpec filterSpec;

    /**
     * Whether images are smoothed when they are scaled.
     * <p>
     * 이미지를 늘이거나 줄일 때 부드럽게 하는지 여부.
     */
    boolean smoothing = true;

    /**
     * The smoothing quality: low, medium or high.
     * <p>
     * 부드럽게 하기 품질: low, medium, high.
     */
    String smoothingQuality = "low";

    /**
     * Creates a copy for <code>save()</code>; the clip is shared because it is immutable.
     * <p>
     * <code>save()</code> 를 위한 복사본을 만든다. 클립은 바뀌지 않으므로 공유한다.
     *
     * @return the copy / 복사본
     */
    DrawState copy() {
        DrawState s = new DrawState();
        s.ctm = new AffineTransform(ctm);
        s.clip = clip;
        s.clipRect = clipRect;
        s.clipMask = clipMask;
        s.fillStyle = fillStyle;
        s.strokeStyle = strokeStyle;
        s.lineWidth = lineWidth;
        s.lineCap = lineCap;
        s.lineJoin = lineJoin;
        s.miterLimit = miterLimit;
        s.lineDash = lineDash;
        s.lineDashOffset = lineDashOffset;
        s.globalAlpha = globalAlpha;
        s.composite = composite;
        s.shadowColor = shadowColor;
        s.shadowBlur = shadowBlur;
        s.shadowOffsetX = shadowOffsetX;
        s.shadowOffsetY = shadowOffsetY;
        s.font = font;
        s.textAlign = textAlign;
        s.textBaseline = textBaseline;
        s.direction = direction;
        s.letterSpacing = letterSpacing;
        s.wordSpacing = wordSpacing;
        s.fontKerning = fontKerning;
        s.fontStretch = fontStretch;
        s.fontVariantCaps = fontVariantCaps;
        s.textRendering = textRendering;
        s.lang = lang;
        s.filter = filter;
        s.filterSpec = filterSpec;
        s.smoothing = smoothing;
        s.smoothingQuality = smoothingQuality;
        return s;
    }

    /**
     * Tells whether a shadow is drawn: the shadow color is not fully transparent and the blur or an offset is not zero.
     * <p>
     * 그림자를 그리는지 알려 준다: 그림자 색이 완전히 투명하지 않고 번짐이나 오프셋 하나가 0 이 아니다.
     *
     * @return true if shadows are drawn / 그림자를 그리면 true
     */
    boolean hasShadow() {
        return (shadowColor >>> 24) != 0 && (shadowBlur != 0 || shadowOffsetX != 0 || shadowOffsetY != 0);
    }
}
