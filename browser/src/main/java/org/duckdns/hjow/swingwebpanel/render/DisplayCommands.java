package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The concrete display commands (plan items P09-04, P09-05): rectangle fill, background image, border, text with decorations, image,
 * placeholder, marker shape and the group commands for clipping and opacity. They are created by {@link DisplayListBuilder} only.
 * <p>
 * 구체적인 디스플레이 명령(계획 P09-04, P09-05): 사각형 채우기, 배경 이미지, 경계선, 장식이 있는 텍스트, 이미지, 대체 그림, 표식 모양, 클리핑과
 * 불투명도 그룹 명령. {@link DisplayListBuilder} 만 만든다.
 */
final class DisplayCommands {

    /**
     * Placeholder kind: an image that cannot be shown.
     * <p>
     * 대체 그림 종류: 보여 줄 수 없는 이미지.
     */
    static final int KIND_BROKEN = 0;

    /**
     * Placeholder kind: the unsupported video notice.
     * <p>
     * 대체 그림 종류: 지원하지 않는 비디오 안내.
     */
    static final int KIND_VIDEO = 1;

    /**
     * Placeholder kind: a minimal audio control bar.
     * <p>
     * 대체 그림 종류: 최소한의 오디오 컨트롤 막대.
     */
    static final int KIND_AUDIO = 2;

    /**
     * Marker shape: filled disc.
     * <p>
     * 표식 모양: 채운 원.
     */
    static final int SHAPE_DISC = 0;

    /**
     * Marker shape: hollow circle.
     * <p>
     * 표식 모양: 속이 빈 원.
     */
    static final int SHAPE_CIRCLE = 1;

    /**
     * Marker shape: filled square.
     * <p>
     * 표식 모양: 채운 사각형.
     */
    static final int SHAPE_SQUARE = 2;

    /**
     * Cache of the images created for decoded frames (weak keys, so frames can be collected).
     * <p>
     * 디코딩된 프레임에 대해 만든 이미지의 캐시(약한 키이므로 프레임을 수거할 수 있다).
     */
    private static final Map<DecodedFrame, BufferedImage> FRAME_IMAGES = Collections
            .synchronizedMap(new WeakHashMap<DecodedFrame, BufferedImage>());

    /**
     * Private constructor: holder of nested classes.
     * <p>
     * 비공개 생성자: 중첩 클래스의 보관소이다.
     */
    private DisplayCommands() {
    }

    /**
     * Returns the image for a decoded frame; it shares the pixel array of the frame (no copy).
     * <p>
     * 디코딩된 프레임의 이미지를 반환한다. 프레임의 픽셀 배열을 공유한다(복사 없음).
     *
     * @param frame the frame / 프레임
     * @param w     width / 너비
     * @param h     height / 높이
     * @return the image / 이미지
     */
    static BufferedImage imageOf(DecodedFrame frame, int w, int h) {
        BufferedImage bi = FRAME_IMAGES.get(frame);
        if (bi == null) {
            int[] argb = frame.getArgb();
            DataBufferInt db = new DataBufferInt(argb, argb.length);
            WritableRaster r = Raster.createPackedRaster(db, w, h, w, new int[] { 0xff0000, 0xff00, 0xff, 0xff000000 }, null);
            ColorModel cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32, 0xff0000, 0xff00, 0xff, 0xff000000, false,
                    DataBuffer.TYPE_INT);
            bi = new BufferedImage(cm, r, false, null);
            FRAME_IMAGES.put(frame, bi);
        }
        return bi;
    }

    /**
     * Selects the frame of an image that is shown at a time.
     * <p>
     * 어떤 시각에 보이는 이미지의 프레임을 고른다.
     *
     * @param image the image / 이미지
     * @param time  animation time in milliseconds / 애니메이션 시각(밀리초)
     * @return the frame / 프레임
     */
    static DecodedFrame frameAt(DecodedImage image, long time) {
        List<DecodedFrame> frames = image.getFrames();
        if (frames.size() == 1) {
            return frames.get(0);
        }
        long total = 0;
        // 모든 프레임의 표시 시간을 더한다(너무 짧은 시간은 100ms 로 본다).
        for (DecodedFrame f : frames) {
            total += f.getDelayMillis() <= 10 ? 100 : f.getDelayMillis();
        }
        long t = Math.max(0, time);
        if (image.getLoopCount() != 0 && t >= total * image.getLoopCount()) {
            return frames.get(frames.size() - 1);
        }
        t = t % total;
        // 시각이 속하는 프레임을 찾는다.
        for (DecodedFrame f : frames) {
            long d = f.getDelayMillis() <= 10 ? 100 : f.getDelayMillis();
            if (t < d) {
                return f;
            }
            t -= d;
        }
        return frames.get(frames.size() - 1);
    }

    /**
     * Fills a rectangle (a background color).
     * <p>
     * 사각형을 채운다(배경색).
     */
    static final class FillRect extends DisplayCommand {
        /**
         * The fill color as ARGB value.
         * <p>
         * ARGB 값으로 나타낸 색상.
         */
        private final int argb;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param argb color / 색
         * @param node DOM node or null / DOM 노드 또는 null
         */
        FillRect(double x, double y, double w, double h, int argb, Node node) {
            super(Type.FILL_RECT, x, y, w, h, node);
            this.argb = argb;
        }

        /**
         * Returns the color.
         * <p>
         * 색을 반환한다.
         *
         * @return ARGB color / ARGB 색
         */
        int getArgb() {
            return argb;
        }

        /**
         * Paints the rectangle.
         * <p>
         * 사각형을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            ctx.g.setColor(new Color(argb, true));
            ctx.g.fill(getBounds());
        }
    }

    /**
     * Paints the resize handle of a box with the <code>resize</code> property (CSS Basic User Interface 4 section 6.1): two diagonal grip lines in
     * the corner of the padding box, a dark line with a light line next to it so that the handle can be seen on a light and on a dark
     * background. The handle looks the same for every direction (like in the browsers). The size of the box is changed by the input
     * handling, not by this command.
     * <p>
     * <code>resize</code> 속성이 있는 상자의 크기 조절 핸들을 그린다(CSS Basic User Interface 4 6.1 절): padding box 모서리의 대각선 줄 둘이며, 밝은 배경과 어두운
     * 배경에서 모두 보이도록 어두운 줄 옆에 밝은 줄을 둔다. 방향에 상관없이 같은 모양이다(브라우저와 같음). 상자의 크기는 입력 처리가 바꾸며 이 명령이 바꾸지 않는다.
     */
    static final class Resizer extends DisplayCommand {
        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x    left of the handle / 핸들의 왼쪽
         * @param y    top of the handle / 핸들의 위쪽
         * @param size the side of the handle / 핸들의 한 변
         * @param node DOM node or null / DOM 노드 또는 null
         */
        Resizer(double x, double y, double size, Node node) {
            super(Type.RESIZER, x, y, size, size, node);
        }

        /**
         * Paints the two grip lines (a dark line with a light line shifted by one pixel).
         * <p>
         * 줄 둘(한 픽셀 비킨 밝은 줄이 붙은 어두운 줄)을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Rectangle2D b = getBounds();
            Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            java.awt.Stroke old = g.getStroke();
            g.setStroke(new java.awt.BasicStroke(1f));
            double x1 = b.getMaxX() - 1;
            double y1 = b.getMaxY() - 1;
            // 대각선 둘을 모서리 가까운 것부터 그린다. 각 선은 어두운 줄과 한 픽셀 비킨 밝은 줄이다.
            for (int i = 0; i < 2; i++) {
                double d = i == 0 ? 4 : 9;
                double sx0 = x1 - 1;
                double sy0 = y1 - d - 2;
                double sx1 = x1 - d - 2;
                double sy1 = y1 - 1;
                g.setColor(new Color(0xFFFFFFFF, true));
                g.draw(new java.awt.geom.Line2D.Double(sx0 + 1, sy0 + 1, sx1 + 1, sy1 + 1));
                g.setColor(new Color(0xFF6E6E6E, true));
                g.draw(new java.awt.geom.Line2D.Double(sx0, sy0, sx1, sy1));
            }
            g.setStroke(old);
            if (aa == null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
            } else {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        }
    }

    /**
     * Paints the border of a box.
     * <p>
     * 상자의 경계선을 그린다.
     */
    static final class Border extends DisplayCommand {
        /**
         * Side widths (top, right, bottom, left).
         * <p>
         * 변 너비(위, 오른쪽, 아래, 왼쪽).
         */
        private final double[] widths;

        /**
         * Side styles.
         * <p>
         * 변 스타일.
         */
        private final String[] styles;

        /**
         * Side colors.
         * <p>
         * 변 색.
         */
        private final int[] colors;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x      left of the border box / 테두리 상자의 왼쪽
         * @param y      top / 위쪽
         * @param w      width / 너비
         * @param h      height / 높이
         * @param widths side widths / 변 너비
         * @param styles side styles / 변 스타일
         * @param colors side colors / 변 색
         * @param node   DOM node or null / DOM 노드 또는 null
         */
        Border(double x, double y, double w, double h, double[] widths, String[] styles, int[] colors, Node node) {
            super(Type.BORDER, x, y, w, h, node);
            this.widths = widths.clone();
            this.styles = styles.clone();
            this.colors = colors.clone();
        }

        /**
         * Paints the four sides.
         * <p>
         * 네 변을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Rectangle2D b = getBounds();
            BorderPainter.paint(ctx.g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), widths, styles, colors);
        }
    }

    /**
     * Paints a text fragment and its decorations.
     * <p>
     * 텍스트 조각과 그 장식을 그린다.
     */
    static final class Text extends DisplayCommand {
        /**
         * The text in logical order.
         * <p>
         * 논리 순서의 텍스트.
         */
        private final String text;
        /**
         * The font.
         * <p>
         * 글꼴.
         */
        private final Font font;
        /**
         * The y coordinate of the text baseline.
         * <p>
         * 텍스트 기준선의 y 좌표.
         */
        private final double baseline;
        /**
         * The color as ARGB value.
         * <p>
         * ARGB 값으로 나타낸 색상.
         */
        private final int color;
        /**
         * Letter spacing in px.
         * <p>
         * 글자 간격(px).
         */
        private final double letterSpacing;
        /**
         * Word spacing in px.
         * <p>
         * 단어 간격(px).
         */
        private final double wordSpacing;
        /**
         * Bidi level.
         * <p>
         * 양방향 수준.
         */
        private final int bidiLevel;
        /**
         * Decoration bits.
         * <p>
         * 장식 비트.
         */
        private final int decoration;
        /**
         * Decoration style.
         * <p>
         * 장식 스타일.
         */
        private final int decoStyle;
        /**
         * Decoration ARGB color.
         * <p>
         * 장식 ARGB 색.
         */
        private final int decoColor;
        /**
         * Decoration thickness or NaN.
         * <p>
         * 장식 두께. 자동이면 NaN.
         */
        private final double decoThickness;
        /**
         * Ascent of the font.
         * <p>
         * 글꼴의 ascent.
         */
        private final double ascent;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param text          text / 텍스트
         * @param font          font / 글꼴
         * @param x             left / 왼쪽
         * @param baseline      baseline y / 기준선 y
         * @param width         width / 너비
         * @param ascent        font ascent / 글꼴 ascent
         * @param descent       font descent / 글꼴 descent
         * @param color         ARGB color / ARGB 색
         * @param letterSpacing letter spacing / 글자 간격
         * @param wordSpacing   word spacing / 단어 간격
         * @param bidiLevel     bidi level / 양방향 수준
         * @param decoration    decoration bits / 장식 비트
         * @param decoStyle     decoration style / 장식 스타일
         * @param decoColor     decoration color / 장식 색
         * @param decoThickness decoration thickness or NaN / 장식 두께 또는 NaN
         * @param node          DOM node or null / DOM 노드 또는 null
         */
        Text(String text, Font font, double x, double baseline, double width, double ascent, double descent, int color, double letterSpacing,
                double wordSpacing, int bidiLevel, int decoration, int decoStyle, int decoColor, double decoThickness, Node node) {
            super(Type.TEXT, x, baseline - ascent - 2, width, ascent + descent + 6 + (decoration == 0 ? 0 : 3 * Math.max(1, font.getSize2D() / 8)), node);
            this.text = text;
            this.font = font;
            this.baseline = baseline;
            this.color = color;
            this.letterSpacing = letterSpacing;
            this.wordSpacing = wordSpacing;
            this.bidiLevel = bidiLevel;
            this.decoration = decoration;
            this.decoStyle = decoStyle;
            this.decoColor = decoColor;
            this.decoThickness = decoThickness;
            this.ascent = ascent;
        }

        /**
         * Returns the text.
         * <p>
         * 텍스트를 반환한다.
         *
         * @return the text / 텍스트
         */
        String getText() {
            return text;
        }

        /** The options of the underline (offset, position, skip-ink), or null for plain lines. 밑줄의 옵션(거리, 위치, skip-ink). 보통 선이면 null. */
        private TextRenderer.DecorationExtras extras;

        /**
         * Sets the options of the underline: <code>text-underline-offset</code>, <code>text-underline-position</code> and, for
         * <code>text-decoration-skip-ink</code>, whether the lines skip the glyphs.
         * <p>
         * 밑줄의 옵션을 정한다: <code>text-underline-offset</code>, <code>text-underline-position</code>, 그리고 <code>text-decoration-skip-ink</code> 에 따라 선이
         * 글리프를 건너뛰는지.
         *
         * @param underlineOffset the offset from the baseline in px, NaN for auto / 기준선에서의 거리(px). auto 이면 NaN
         * @param under           true for <code>text-underline-position: under</code> / <code>text-underline-position: under</code> 이면 true
         * @param descent         the descent of the font / 글꼴의 descent
         * @param skipInk         true if the lines skip the glyphs / 선이 글리프를 건너뛰면 true
         * @return this command / 이 명령
         */
        Text withUnderlineOptions(double underlineOffset, boolean under, double descent, boolean skipInk) {
            if (!Double.isNaN(underlineOffset) || under || skipInk) {
                this.extras = new TextRenderer.DecorationExtras(underlineOffset, under, descent, skipInk ? text : null, font, letterSpacing, wordSpacing, bidiLevel);
            }
            return this;
        }

        /**
         * Paints the text and its decoration lines.
         * <p>
         * 텍스트와 장식선을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Rectangle2D b = getBounds();
            g.setColor(new Color(color, true));
            TextRenderer.draw(g, text, font, b.getX(), baseline, letterSpacing, wordSpacing, bidiLevel);
            if (decoration != 0) {
                TextRenderer.drawDecorations(g, decoration, decoStyle, decoColor, decoThickness, b.getX(), baseline, b.getWidth(), ascent,
                        font.getSize2D(), extras);
            }
        }
    }

    /**
     * Paints text that is rotated by a quarter turn (sideways text in vertical writing modes): the text runs downwards (clockwise) or
     * upwards (counter-clockwise) from its origin on the baseline; decoration lines are drawn in the rotated frame.
     * <p>
     * 사분원만큼 회전한 텍스트(세로쓰기에서 눕힌 텍스트)를 그린다: 텍스트는 기준선 위의 원점에서 아래쪽(시계 방향) 또는 위쪽(시계 반대 방향)으로 흐르며 장식선은 회전한 좌표계에서
     * 그린다.
     */
    static final class RotatedText extends DisplayCommand {
        /** The text in logical order. 논리 순서의 텍스트. */
        private final String text;
        /** The font. 글꼴. */
        private final Font font;
        /** X of the origin on the baseline. 기준선 위 원점의 x. */
        private final double originX;
        /** Y of the origin (where the text starts). 원점(텍스트가 시작하는 곳)의 y. */
        private final double originY;
        /** True for a clockwise quarter turn, false for a counter-clockwise one. 시계 방향 사분원이면 true, 시계 반대 방향이면 false. */
        private final boolean clockwise;
        /** The advance of the text along its line. 줄 방향 텍스트 advance. */
        private final double advance;
        /** The ascent of the font. 글꼴의 ascent. */
        private final double ascent;
        /** The color as ARGB value. ARGB 값으로 나타낸 색상. */
        private final int color;
        /** Letter spacing in px. 글자 간격(px). */
        private final double letterSpacing;
        /** Word spacing in px. 단어 간격(px). */
        private final double wordSpacing;
        /** Bidi level. 양방향 수준. */
        private final int bidiLevel;
        /** Decoration bits. 장식 비트. */
        private final int decoration;
        /** Decoration style. 장식 스타일. */
        private final int decoStyle;
        /** Decoration ARGB color. 장식 ARGB 색. */
        private final int decoColor;
        /** Decoration thickness or NaN. 장식 두께. 자동이면 NaN. */
        private final double decoThickness;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param text          text / 텍스트
         * @param font          font / 글꼴
         * @param bounds        {x, y, width, height} of the box that holds the rotated text / 회전한 텍스트를 담은 상자의 {x, y, 너비, 높이}
         * @param originX       x of the origin on the baseline / 기준선 위 원점의 x
         * @param originY       y where the text starts / 텍스트가 시작하는 y
         * @param clockwise     true for a clockwise turn / 시계 방향이면 true
         * @param advance       advance of the text / 텍스트의 advance
         * @param ascent        font ascent / 글꼴 ascent
         * @param color         ARGB color / ARGB 색
         * @param letterSpacing letter spacing / 글자 간격
         * @param wordSpacing   word spacing / 단어 간격
         * @param bidiLevel     bidi level / 양방향 수준
         * @param decoration    decoration bits / 장식 비트
         * @param decoStyle     decoration style / 장식 스타일
         * @param decoColor     decoration color / 장식 색
         * @param decoThickness decoration thickness or NaN / 장식 두께 또는 NaN
         * @param node          DOM node or null / DOM 노드 또는 null
         */
        RotatedText(String text, Font font, double[] bounds, double originX, double originY, boolean clockwise, double advance, double ascent,
                int color, double letterSpacing, double wordSpacing, int bidiLevel, int decoration, int decoStyle, int decoColor,
                double decoThickness, Node node) {
            super(Type.TEXT, bounds[0] - 3, bounds[1] - 3, bounds[2] + 6, bounds[3] + 6, node);
            this.text = text;
            this.font = font;
            this.originX = originX;
            this.originY = originY;
            this.clockwise = clockwise;
            this.advance = advance;
            this.ascent = ascent;
            this.color = color;
            this.letterSpacing = letterSpacing;
            this.wordSpacing = wordSpacing;
            this.bidiLevel = bidiLevel;
            this.decoration = decoration;
            this.decoStyle = decoStyle;
            this.decoColor = decoColor;
            this.decoThickness = decoThickness;
        }

        /**
         * Paints the text and its decoration lines in a frame rotated around the origin.
         * <p>
         * 원점을 중심으로 회전한 좌표계에서 텍스트와 장식선을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            AffineTransform saved = g.getTransform();
            try {
                g.translate(originX, originY);
                g.rotate(clockwise ? Math.PI / 2 : -Math.PI / 2);
                g.setColor(new Color(color, true));
                TextRenderer.draw(g, text, font, 0, 0, letterSpacing, wordSpacing, bidiLevel);
                if (decoration != 0) {
                    TextRenderer.drawDecorations(g, decoration, decoStyle, decoColor, decoThickness, 0, 0, advance, ascent, font.getSize2D());
                }
            } finally {
                g.setTransform(saved);
            }
        }
    }

    /**
     * Paints a frame of a decoded image scaled to its box.
     * <p>
     * 디코딩된 이미지의 프레임을 상자 크기로 맞춰 그린다.
     */
    static final class Image extends DisplayCommand {
        /**
         * The decoded image.
         * <p>
         * 디코딩된 이미지.
         */
        private final DecodedImage image;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param w     width / 너비
         * @param h     height / 높이
         * @param image the image / 이미지
         * @param node  DOM node or null / DOM 노드 또는 null
         */
        Image(double x, double y, double w, double h, DecodedImage image, Node node) {
            super(Type.IMAGE, x, y, w, h, node);
            this.image = image;
        }

        /**
         * Returns the image.
         * <p>
         * 이미지를 반환한다.
         *
         * @return the image / 이미지
         */
        DecodedImage getImage() {
            return image;
        }

        /**
         * Paints the frame that belongs to the animation time.
         * <p>
         * 애니메이션 시각에 해당하는 프레임을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Rectangle2D b = getBounds();
            if (b.getWidth() <= 0 || b.getHeight() <= 0) {
                return;
            }
            BufferedImage bi = imageOf(frameAt(image, ctx.timeMillis), image.getWidth(), image.getHeight());
            Graphics2D g = ctx.g;
            Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            boolean scaled = b.getWidth() != image.getWidth() || b.getHeight() != image.getHeight();
            if (scaled) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            g.drawImage(bi, (int) Math.round(b.getX()), (int) Math.round(b.getY()), (int) Math.round(b.getWidth()), (int) Math.round(b.getHeight()), null);
            if (scaled && old != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
            }
        }
    }

    /**
     * Paints a background image (single or tiled) inside a box.
     * <p>
     * 상자 안에 배경 이미지(하나 또는 타일)를 그린다.
     */
    static final class BackgroundImage extends DisplayCommand {
        /**
         * The image.
         * <p>
         * 이미지.
         */
        private final DecodedImage image;
        /**
         * Left of the first tile.
         * <p>
         * 첫 타일의 왼쪽.
         */
        private final double originX;
        /**
         * Top of the first tile.
         * <p>
         * 첫 타일의 위쪽.
         */
        private final double originY;
        /**
         * Tile width.
         * <p>
         * 타일 너비.
         */
        private final double tileW;
        /**
         * Tile height.
         * <p>
         * 타일 높이.
         */
        private final double tileH;
        /**
         * Whether the image repeats horizontally.
         * <p>
         * 가로로 반복하는지 여부.
         */
        private final boolean repeatX;
        /**
         * Whether the image repeats vertically.
         * <p>
         * 세로로 반복하는지 여부.
         */
        private final boolean repeatY;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x       left of the painting area / 그리는 영역의 왼쪽
         * @param y       top / 위쪽
         * @param w       width / 너비
         * @param h       height / 높이
         * @param image   the image / 이미지
         * @param originX left of the first tile / 첫 타일의 왼쪽
         * @param originY top of the first tile / 첫 타일의 위쪽
         * @param tileW   tile width / 타일 너비
         * @param tileH   tile height / 타일 높이
         * @param repeatX repeat horizontally / 가로 반복
         * @param repeatY repeat vertically / 세로 반복
         * @param node    DOM node or null / DOM 노드 또는 null
         */
        BackgroundImage(double x, double y, double w, double h, DecodedImage image, double originX, double originY, double tileW, double tileH,
                boolean repeatX, boolean repeatY, Node node) {
            super(Type.BACKGROUND_IMAGE, x, y, w, h, node);
            this.image = image;
            this.originX = originX;
            this.originY = originY;
            this.tileW = tileW;
            this.tileH = tileH;
            this.repeatX = repeatX;
            this.repeatY = repeatY;
        }

        /**
         * Paints the tiles that meet the visible area.
         * <p>
         * 보이는 영역과 만나는 타일을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            if (tileW < 1 || tileH < 1) {
                return;
            }
            Graphics2D g = ctx.g;
            Shape old = g.getClip();
            g.clip(getBounds());
            BufferedImage bi = imageOf(frameAt(image, ctx.timeMillis), image.getWidth(), image.getHeight());
            Rectangle2D vis = getBounds().createIntersection(ctx.clipDoc);
            double startX = originX;
            double startY = originY;
            if (repeatX) {
                startX = originX - Math.ceil((originX - vis.getMinX()) / tileW) * tileW;
            }
            if (repeatY) {
                startY = originY - Math.ceil((originY - vis.getMinY()) / tileH) * tileH;
            }
            int guard = 0;
            // 보이는 영역을 덮을 때까지 타일을 가로, 세로로 놓는다(한 번에 그리는 타일 수는 제한한다).
            for (double ty = startY; ty < vis.getMaxY() && guard < 20000; ty += tileH) {
                for (double tx = startX; tx < vis.getMaxX() && guard < 20000; tx += tileW) {
                    g.drawImage(bi, (int) Math.round(tx), (int) Math.round(ty), (int) Math.round(tileW), (int) Math.round(tileH), null);
                    guard++;
                    if (!repeatX) {
                        break;
                    }
                }
                if (!repeatY) {
                    break;
                }
            }
            g.setClip(old);
        }
    }

    /**
     * Paints the replacement of an image, audio or video that cannot be shown: a frame with text lines, or an audio control bar.
     * <p>
     * 보여 줄 수 없는 이미지/오디오/비디오의 대체 그림을 그린다: 텍스트 줄이 있는 틀 또는 오디오 컨트롤 막대.
     */
    static final class Placeholder extends DisplayCommand {
        /**
         * The text lines.
         * <p>
         * 텍스트 줄.
         */
        private final String[] lines;
        /**
         * The kind (KIND_ constants).
         * <p>
         * 종류(KIND_ 상수).
         */
        private final int kind;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param w     width / 너비
         * @param h     height / 높이
         * @param lines text lines / 텍스트 줄
         * @param kind  kind / 종류
         * @param node  DOM node or null / DOM 노드 또는 null
         */
        Placeholder(double x, double y, double w, double h, String[] lines, int kind, Node node) {
            this(x, y, w, h, lines, kind, node, null);
        }

        /**
         * Creates the command of an audio control bar with the state that it shows.
         * <p>
         * 보여 줄 상태가 있는 오디오 컨트롤 막대의 명령을 만든다.
         *
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param w     width / 너비
         * @param h     height / 높이
         * @param lines text lines / 텍스트 줄
         * @param kind  kind / 종류
         * @param node  DOM node or null / DOM 노드 또는 null
         * @param state the state of the control bar, null for a bar that shows nothing yet / 컨트롤 막대의 상태. 아직 보여 줄 것이 없으면 null
         */
        Placeholder(double x, double y, double w, double h, String[] lines, int kind, Node node, MediaControlState state) {
            super(Type.PLACEHOLDER, x, y, w, h, node);
            this.lines = lines.clone();
            this.kind = kind;
            this.controls = state;
        }

        /**
         * The state that an audio control bar shows, or null.
         * <p>
         * 오디오 컨트롤 막대가 보여 주는 상태. 없으면 null.
         */
        private final MediaControlState controls;

        /**
         * Returns the state that the audio control bar shows.
         * <p>
         * 오디오 컨트롤 막대가 보여 주는 상태를 반환한다.
         *
         * @return the state, or null for a placeholder that is not a control bar or has none yet / 상태. 컨트롤 막대가 아니거나 아직 없으면 null
         */
        MediaControlState getControls() {
            return controls;
        }

        /**
         * Returns the text lines.
         * <p>
         * 텍스트 줄을 반환한다.
         *
         * @return a copy of the lines / 줄의 복사본
         */
        String[] getLines() {
            return lines.clone();
        }

        /**
         * Paints the frame and the text.
         * <p>
         * 틀과 텍스트를 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Rectangle2D b = getBounds();
            double x = b.getX();
            double y = b.getY();
            double w = b.getWidth();
            double h = b.getHeight();
            // 오디오 컨트롤 막대는 상태를 보여 주는 전용 그리기로 그린다.
            if (kind == KIND_AUDIO) {
                MediaControlsPainter.paint(g, x, y, w, h, controls == null ? MediaControlState.IDLE : controls);
                return;
            }
            g.setColor(kind == KIND_AUDIO ? new Color(0xF1F3F4) : new Color(0xF4F4F4));
            g.fill(b);
            g.setColor(new Color(0xA0A0A0));
            g.fill(new Rectangle2D.Double(x, y, w, 1));
            g.fill(new Rectangle2D.Double(x, y + h - 1, w, 1));
            g.fill(new Rectangle2D.Double(x, y, 1, h));
            g.fill(new Rectangle2D.Double(x + w - 1, y, 1, h));
            Font font = new Font("Dialog", Font.PLAIN, 12);
            Shape old = g.getClip();
            g.clip(new Rectangle2D.Double(x + 2, y + 2, Math.max(0, w - 4), Math.max(0, h - 4)));
            g.setColor(new Color(0x444444));
            if (kind == KIND_AUDIO) {
                Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Path2D.Double tri = new Path2D.Double();
                double cy = y + h / 2;
                tri.moveTo(x + 14, cy - 8);
                tri.lineTo(x + 14, cy + 8);
                tri.lineTo(x + 28, cy);
                tri.closePath();
                g.fill(tri);
                if (aa != null) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
                }
                TextRenderer.draw(g, "0:00 / 0:00", font, x + 40, cy + 5, 0, 0, 0);
            } else {
                double ly = y + 4 + 12;
                double maxWidth = Math.max(8, w - 8);
                java.awt.FontMetrics metrics = g.getFontMetrics(font);
                // 텍스트 줄을 위에서 아래로 그리되, 상자 폭을 넘는 줄은 나누어 여러 줄로 그린다.
                for (int i = 0; i < lines.length; i++) {
                    List<String> parts = wrapLine(lines[i], metrics, maxWidth);
                    // 나눈 조각을 한 줄씩 그린다.
                    for (int p = 0; p < parts.size(); p++) {
                        TextRenderer.draw(g, parts.get(p), font, x + 4, ly, 0, 0, 0);
                        ly += 16;
                    }
                }
            }
            g.setClip(old);
        }

        /**
         * Splits a text line into pieces that fit a width: at the last space that fits, or between characters if a word is longer than the width
         * (an address has no spaces), so that nothing is cut off at the edge of the box.
         * <p>
         * 글줄을 폭에 맞는 조각으로 나눈다: 들어가는 마지막 공백에서 나누고, 단어가 폭보다 길면(주소에는 공백이 없다) 글자 사이에서 나누어 상자 가장자리에서
         * 잘리는 내용이 없게 한다.
         *
         * @param text     the line / 글줄
         * @param metrics  the font metrics / 글꼴 측정값
         * @param maxWidth the largest width of a piece / 조각의 최대 폭
         * @return the pieces, at least one / 조각들(하나 이상)
         */
        static List<String> wrapLine(String text, java.awt.FontMetrics metrics, double maxWidth) {
            List<String> out = new java.util.ArrayList<String>();
            int start = 0;
            int n = text.length();
            // 남은 글이 폭에 들어올 때까지 앞에서부터 한 조각씩 잘라 낸다.
            while (start < n) {
                if (metrics.stringWidth(text.substring(start)) <= maxWidth) {
                    out.add(text.substring(start));
                    break;
                }
                int end = start + 1;
                int lastSpace = -1;
                // 폭을 넘기 직전까지 글자를 하나씩 늘려 가며 마지막 공백 위치를 기억한다.
                while (end < n && metrics.stringWidth(text.substring(start, end + 1)) <= maxWidth) {
                    end++;
                    if (text.charAt(end - 1) == ' ') {
                        lastSpace = end;
                    }
                }
                // 서로게이트 쌍의 가운데에서 끊지 않는다.
                if (end < n && end > start + 1 && Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
                int cut = lastSpace > start ? lastSpace : end;
                out.add(text.substring(start, cut));
                start = cut;
            }
            if (out.isEmpty()) {
                out.add("");
            }
            return out;
        }
    }

    /**
     * Paints a list marker as a shape (disc, circle, square).
     * <p>
     * 목록 표식을 모양(원, 빈 원, 사각형)으로 그린다.
     */
    static final class MarkerShape extends DisplayCommand {
        /**
         * The shape (SHAPE_ constants).
         * <p>
         * 모양(SHAPE_ 상수).
         */
        private final int shape;
        /**
         * The color as ARGB value.
         * <p>
         * ARGB 값으로 나타낸 색상.
         */
        private final int argb;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param x     left / 왼쪽
         * @param y     top / 위쪽
         * @param size  width and height / 너비와 높이
         * @param shape shape / 모양
         * @param argb  color / 색
         * @param node  DOM node or null / DOM 노드 또는 null
         */
        MarkerShape(double x, double y, double size, int shape, int argb, Node node) {
            super(Type.MARKER_SHAPE, x, y, size, size, node);
            this.shape = shape;
            this.argb = argb;
        }

        /**
         * Returns the shape.
         * <p>
         * 모양을 반환한다.
         *
         * @return shape constant / 모양 상수
         */
        int getShape() {
            return shape;
        }

        /**
         * Paints the shape.
         * <p>
         * 모양을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Rectangle2D b = getBounds();
            g.setColor(new Color(argb, true));
            if (shape == SHAPE_SQUARE) {
                g.fill(b);
                return;
            }
            Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (shape == SHAPE_DISC) {
                g.fill(new Ellipse2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
            } else {
                g.setStroke(new java.awt.BasicStroke(1f));
                g.draw(new Ellipse2D.Double(b.getX() + 0.5, b.getY() + 0.5, b.getWidth() - 1, b.getHeight() - 1));
            }
            if (aa != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        }
    }

    /**
     * A group command (clip start/end, opacity start/end): handled by the painter, painting nothing itself.
     * <p>
     * 그룹 명령(클립 시작/끝, 불투명도 시작/끝): 그리는 쪽이 처리하며 자신은 아무것도 그리지 않는다.
     */
    static final class Group extends DisplayCommand {
        /**
         * The opacity of an opacity group, 1 for the others.
         * <p>
         * 불투명도 그룹의 불투명도. 나머지는 1.
         */
        private final float alpha;

        /**
         * The blend mode constant ({@link BlendModes}) of an opacity group, {@link BlendModes#NORMAL} for the others.
         * <p>
         * 불투명도 그룹의 혼합 모드 상수({@link BlendModes}). 나머지는 {@link BlendModes#NORMAL}.
         */
        private final int blend;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param type   the group kind / 그룹 종류
         * @param x      left of the group area / 그룹 영역의 왼쪽
         * @param y      top / 위쪽
         * @param w      width / 너비
         * @param h      height / 높이
         * @param alpha  opacity / 불투명도
         * @param node   DOM node or null / DOM 노드 또는 null
         */
        Group(Type type, double x, double y, double w, double h, float alpha, Node node) {
            this(type, x, y, w, h, alpha, BlendModes.NORMAL, node);
        }

        /**
         * Creates a command of a group with a blend mode.
         * <p>
         * 혼합 모드가 있는 그룹의 명령을 만든다.
         *
         * @param type   the group kind / 그룹 종류
         * @param x      left of the group area / 그룹 영역의 왼쪽
         * @param y      top / 위쪽
         * @param w      width / 너비
         * @param h      height / 높이
         * @param alpha  opacity / 불투명도
         * @param blend  the blend mode constant / 혼합 모드 상수
         * @param node   DOM node or null / DOM 노드 또는 null
         */
        Group(Type type, double x, double y, double w, double h, float alpha, int blend, Node node) {
            super(type, x, y, w, h, node);
            this.alpha = alpha;
            this.blend = blend;
        }

        /**
         * Returns the blend mode of the group.
         * <p>
         * 그룹의 혼합 모드를 반환한다.
         *
         * @return the constant of {@link BlendModes} / {@link BlendModes} 의 상수
         */
        int getBlend() {
            return blend;
        }

        /**
         * Returns the opacity of the group.
         * <p>
         * 그룹의 불투명도를 반환한다.
         *
         * @return opacity between 0 and 1 / 0~1 의 불투명도
         */
        float getAlpha() {
            return alpha;
        }

        /**
         * Paints nothing (the painter handles groups).
         * <p>
         * 아무것도 그리지 않는다(그룹은 그리는 쪽이 처리한다).
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            // 그룹 명령은 PagePainter 가 구조적으로 처리하므로 여기서 할 일이 없다.
        }
    }
}
