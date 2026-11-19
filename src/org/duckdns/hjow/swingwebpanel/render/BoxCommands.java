package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The display commands of the decoration of boxes that has rounded corners, shadows and gradients (plan item P10-04, CSS Backgrounds
 * and Borders Level 3): {@link ShapeFill} (a filled rounded rectangle), {@link RoundedBorder} (the border ring of a box with
 * rounded corners), {@link Shadow} (<code>box-shadow</code>, painted from an image that the builder computed), {@link RoundedClip}
 * (the clip of <code>overflow</code> along the rounded padding edge) and {@link Layer} (the tiles of a background image or gradient
 * inside a clip shape). They are created by {@link DisplayListBuilder} only. Shapes are painted with anti-aliasing.
 * <p>
 * 둥근 모서리, 그림자, 그라디언트가 있는 상자 장식의 디스플레이 명령(계획 P10-04, CSS Backgrounds and Borders Level 3): {@link ShapeFill}(채운 둥근 사각형),
 * {@link RoundedBorder}(둥근 모서리 상자의 border 고리), {@link Shadow}(<code>box-shadow</code>, 빌더가 계산한 이미지로 그림), {@link RoundedClip}(둥근 padding 가장자리를
 * 따라 자르는 <code>overflow</code> 클립), {@link Layer}(클립 도형 안의 배경 이미지나 그라디언트 타일). {@link DisplayListBuilder} 만 만든다. 도형은 안티앨리어싱으로 그린다.
 */
final class BoxCommands {

    /**
     * Private constructor: static holder class.
     * <p>
     * 비공개 생성자: 정적 묶음 클래스이다.
     */
    private BoxCommands() {
    }

    /**
     * Turns anti-aliasing on and returns the previous setting.
     * <p>
     * 안티앨리어싱을 켜고 이전 설정을 반환한다.
     *
     * @param g the graphics / 그래픽스
     * @return the previous hint value / 이전 힌트 값
     */
    static Object aaOn(Graphics2D g) {
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return old;
    }

    /**
     * Restores the anti-aliasing setting.
     * <p>
     * 안티앨리어싱 설정을 되돌린다.
     *
     * @param g   the graphics / 그래픽스
     * @param old the previous hint value / 이전 힌트 값
     */
    static void aaRestore(Graphics2D g, Object old) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old == null ? RenderingHints.VALUE_ANTIALIAS_OFF : old);
    }

    /**
     * Fills a shape (the background color of a box with rounded corners).
     * <p>
     * 도형을 채운다(둥근 모서리 상자의 배경색).
     */
    static final class ShapeFill extends DisplayCommand {
        /** The shape. 도형. */
        private final Shape shape;
        /** The color as ARGB value. ARGB 값으로 나타낸 색상. */
        private final int argb;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param shape the shape in document coordinates / 문서 좌표의 도형
         * @param argb  the color / 색
         * @param node  DOM node or null / DOM 노드 또는 null
         */
        ShapeFill(Shape shape, int argb, Node node) {
            super(Type.FILL_RECT, shape.getBounds2D().getX(), shape.getBounds2D().getY(), shape.getBounds2D().getWidth(), shape.getBounds2D().getHeight(), node);
            this.shape = shape;
            this.argb = argb;
        }

        /**
         * Paints the shape.
         * <p>
         * 도형을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Object aa = aaOn(ctx.g);
            ctx.g.setColor(new Color(argb, true));
            ctx.g.fill(shape);
            aaRestore(ctx.g, aa);
        }
    }

    /**
     * Paints the border of a box with rounded corners: the ring between the outer and the inner rounded edge, every side separately in its
     * style and color (the sides are cut at the diagonals between the outer and the inner corner points). Styles: solid; inset and outset
     * (shaded sides); double, groove and ridge (two bands of the ring); dashed and dotted (a stroke along the middle of the ring).
     * <p>
     * 둥근 모서리 상자의 border 를 그린다: 바깥과 안쪽 둥근 가장자리 사이의 고리를 변마다 자기 스타일과 색으로 그린다(변은 바깥과 안쪽 모서리 점 사이의 대각선에서
     * 잘린다). 스타일: solid, inset과 outset(그늘진 변), double, groove, ridge(고리의 두 띠), dashed와 dotted(고리 가운데를 따라가는 선).
     */
    static final class RoundedBorder extends DisplayCommand {
        /** The outer edge. 바깥 가장자리. */
        private final RoundedBox outer;
        /** The side widths {top, right, bottom, left}. 변 너비 {위, 오른쪽, 아래, 왼쪽}. */
        private final double[] widths;
        /** The side styles. 변 스타일. */
        private final String[] styles;
        /** The side colors as ARGB. 변 색(ARGB). */
        private final int[] colors;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param outer  the rounded border edge / 둥근 border 가장자리
         * @param widths side widths in TOP, RIGHT, BOTTOM, LEFT order / TOP, RIGHT, BOTTOM, LEFT 순서의 변 너비
         * @param styles side styles / 변 스타일
         * @param colors side colors (ARGB) / 변 색(ARGB)
         * @param node   DOM node or null / DOM 노드 또는 null
         */
        RoundedBorder(RoundedBox outer, double[] widths, String[] styles, int[] colors, Node node) {
            super(Type.BORDER, outer.x, outer.y, outer.w, outer.h, node);
            this.outer = outer;
            this.widths = widths;
            this.styles = styles;
            this.colors = colors;
        }

        /**
         * Paints the border.
         * <p>
         * 경계선을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Object aa = aaOn(g);
            Shape oldClip = g.getClip();
            try {
                RoundedBox inner = outer.inset(widths[0], widths[1], widths[2], widths[3]);
                Area ring = new Area(outer.toShape());
                ring.subtract(new Area(inner.toShape()));
                boolean same = true;
                // 네 변이 같은 solid 스타일과 색이면 고리를 한 번에 채워 이음매가 보이지 않게 한다.
                for (int i = 0; i < 4; i++) {
                    if (!"solid".equals(styles[i]) || widths[i] <= 0 || colors[i] != colors[0]) {
                        same = false;
                    }
                }
                if (same) {
                    g.setColor(new Color(colors[0], true));
                    g.fill(ring);
                    return;
                }
                // 변마다 대각선으로 자른 부분에 자기 스타일로 그린다.
                for (int side = 0; side < 4; side++) {
                    if (widths[side] <= 0 || !BorderPainter.isVisibleStyle(styles[side]) || (colors[side] >>> 24) == 0) {
                        continue;
                    }
                    Area region = new Area(sidePolygon(side));
                    region.intersect(ring);
                    paintSide(g, side, region);
                }
            } finally {
                g.setClip(oldClip);
                aaRestore(g, aa);
            }
        }

        /**
         * Returns the trapezoid of a side between the outer and the inner corner points.
         * <p>
         * 바깥과 안쪽 모서리 점 사이의 변의 사다리꼴을 반환한다.
         *
         * @param side the side / 변
         * @return the polygon / 다각형
         */
        private Path2D sidePolygon(int side) {
            double x = outer.x;
            double y = outer.y;
            double w = outer.w;
            double h = outer.h;
            double wt = widths[0];
            double wr = widths[1];
            double wb = widths[2];
            double wl = widths[3];
            Path2D.Double p = new Path2D.Double();
            if (side == 0) {
                p.moveTo(x, y);
                p.lineTo(x + w, y);
                p.lineTo(x + w - wr, y + wt);
                p.lineTo(x + wl, y + wt);
            } else if (side == 1) {
                p.moveTo(x + w, y);
                p.lineTo(x + w, y + h);
                p.lineTo(x + w - wr, y + h - wb);
                p.lineTo(x + w - wr, y + wt);
            } else if (side == 2) {
                p.moveTo(x + w, y + h);
                p.lineTo(x, y + h);
                p.lineTo(x + wl, y + h - wb);
                p.lineTo(x + w - wr, y + h - wb);
            } else {
                p.moveTo(x, y + h);
                p.lineTo(x, y);
                p.lineTo(x + wl, y + wt);
                p.lineTo(x + wl, y + h - wb);
            }
            p.closePath();
            return p;
        }

        /**
         * Returns a darker color for the shaded sides.
         * <p>
         * 그늘진 변의 더 어두운 색을 반환한다.
         *
         * @param c the color / 색
         * @return the darker color / 더 어두운 색
         */
        private static Color shade(Color c) {
            return new Color(c.getRed() * 2 / 3, c.getGreen() * 2 / 3, c.getBlue() * 2 / 3, c.getAlpha());
        }

        /**
         * Paints one side in its style inside the region of the ring that belongs to it.
         * <p>
         * 고리 중 변에 속한 영역 안에 변 하나를 해당 스타일로 그린다.
         *
         * @param g      the graphics / 그래픽스
         * @param side   the side / 변
         * @param region the part of the ring of this side / 이 변에 속한 고리의 부분
         */
        private void paintSide(Graphics2D g, int side, Area region) {
            String style = styles[side];
            Color color = new Color(colors[side], true);
            double bw = widths[side];
            boolean topLeft = side == 0 || side == 3;
            if ("dashed".equals(style) || "dotted".equals(style)) {
                boolean dotted = "dotted".equals(style);
                RoundedBox mid = outer.inset(widths[0] / 2, widths[1] / 2, widths[2] / 2, widths[3] / 2);
                float[] dash = dotted ? new float[] { 0.01f, (float) (2 * bw) } : new float[] { (float) (3 * bw), (float) (2 * bw) };
                Shape prev = g.getClip();
                java.awt.Stroke prevStroke = g.getStroke();
                g.clip(region);
                g.setColor(color);
                g.setStroke(new BasicStroke((float) bw, dotted ? BasicStroke.CAP_ROUND : BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.draw(mid.toShape());
                g.setStroke(prevStroke);
                g.setClip(prev);
            } else if (("double".equals(style) && bw >= 3) || "groove".equals(style) || "ridge".equals(style)) {
                boolean dbl = "double".equals(style);
                double a = dbl ? 1.0 / 3 : 0.5;
                double b = dbl ? 2.0 / 3 : 0.5;
                RoundedBox outerBand = outer.inset(widths[0] * a, widths[1] * a, widths[2] * a, widths[3] * a);
                RoundedBox innerBand = outer.inset(widths[0] * b, widths[1] * b, widths[2] * b, widths[3] * b);
                Area first = new Area(outer.toShape());
                first.subtract(new Area(outerBand.toShape()));
                first.intersect(region);
                Area second = new Area(innerBand.toShape());
                second.subtract(new Area(outer.inset(widths[0], widths[1], widths[2], widths[3]).toShape()));
                second.intersect(region);
                Color c1 = color;
                Color c2 = color;
                if ("groove".equals(style)) {
                    c1 = topLeft ? shade(color) : color;
                    c2 = topLeft ? color : shade(color);
                } else if ("ridge".equals(style)) {
                    c1 = topLeft ? color : shade(color);
                    c2 = topLeft ? shade(color) : color;
                }
                g.setColor(c1);
                g.fill(first);
                g.setColor(c2);
                g.fill(second);
            } else {
                Color c = color;
                if ("inset".equals(style) && topLeft) {
                    c = shade(color);
                } else if ("outset".equals(style) && !topLeft) {
                    c = shade(color);
                }
                g.setColor(c);
                g.fill(region);
            }
        }
    }

    /**
     * Paints a <code>box-shadow</code> from an image that was computed when the display list was built (the blurred silhouette in the
     * shadow color), or, without blur, a plain shape. An outer shadow is clipped to the outside of the border box, an inset shadow to
     * the inside of the padding box.
     * <p>
     * <code>box-shadow</code> 를 디스플레이 리스트를 만들 때 계산한 이미지(그림자 색의 흐려진 실루엣)로 그리고, 흐림이 없으면 평범한 도형으로 그린다. 바깥 그림자는
     * border box 바깥으로, 안쪽 그림자는 padding box 안쪽으로 자른다.
     */
    static final class Shadow extends DisplayCommand {
        /** The shadow image (null for an unblurred shadow). 그림자 이미지(흐림이 없으면 null). */
        private final BufferedImage image;
        /** Left of the image. 이미지의 왼쪽. */
        private final int imageX;
        /** Top of the image. 이미지의 위쪽. */
        private final int imageY;
        /** The shape of an unblurred shadow. 흐리지 않은 그림자의 도형. */
        private final Shape shape;
        /** The color of an unblurred shadow as ARGB. 흐리지 않은 그림자의 색(ARGB). */
        private final int argb;
        /** The area that the shadow is clipped to. 그림자를 자르는 영역. */
        private final Shape clip;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param bounds the area that the command paints {x, y, width, height} / 명령이 그리는 영역
         * @param image  the blurred shadow or null / 흐려진 그림자 또는 null
         * @param imageX left of the image / 이미지의 왼쪽
         * @param imageY top of the image / 이미지의 위쪽
         * @param shape  the shape if there is no image / 이미지가 없을 때의 도형
         * @param argb   the color if there is no image / 이미지가 없을 때의 색
         * @param clip   the area that the shadow is clipped to / 그림자를 자르는 영역
         * @param node   DOM node or null / DOM 노드 또는 null
         */
        Shadow(Rectangle2D bounds, BufferedImage image, int imageX, int imageY, Shape shape, int argb, Shape clip, Node node) {
            super(Type.SHADOW, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), node);
            this.image = image;
            this.imageX = imageX;
            this.imageY = imageY;
            this.shape = shape;
            this.argb = argb;
            this.clip = clip;
        }

        /**
         * Paints the shadow.
         * <p>
         * 그림자를 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            Shape old = g.getClip();
            Object aa = aaOn(g);
            try {
                g.clip(clip);
                if (image != null) {
                    g.drawImage(image, imageX, imageY, null);
                } else {
                    g.setColor(new Color(argb, true));
                    g.fill(shape);
                }
            } finally {
                g.setClip(old);
                aaRestore(g, aa);
            }
        }
    }

    /**
     * The start of a clip group whose clip is a shape (the rounded padding edge of a box with <code>overflow</code>).
     * <p>
     * 클립이 도형인 클립 그룹의 시작(<code>overflow</code> 가 있는 상자의 둥근 padding 가장자리).
     */
    static final class RoundedClip extends DisplayCommand {
        /** The clip shape. 클립 도형. */
        private final Shape shape;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param shape the clip shape in document coordinates / 문서 좌표의 클립 도형
         * @param node  DOM node or null / DOM 노드 또는 null
         */
        RoundedClip(Shape shape, Node node) {
            super(Type.CLIP_START, shape.getBounds2D().getX(), shape.getBounds2D().getY(), shape.getBounds2D().getWidth(), shape.getBounds2D().getHeight(), node);
            this.shape = shape;
        }

        /**
         * Returns the shape that clips the group.
         * <p>
         * 그룹을 자르는 도형을 반환한다.
         *
         * @return the shape / 도형
         */
        Shape getClipShape() {
            return shape;
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

    /**
     * Paints the tiles of one background layer (an image or a gradient) inside a clip shape.
     * <p>
     * 배경 레이어 하나(이미지나 그라디언트)의 타일을 클립 도형 안에 그린다.
     */
    static final class Layer extends DisplayCommand {
        /** The clip shape (the background clip edge). 클립 도형(배경 클립 가장자리). */
        private final Shape clip;
        /** The image, or null for a gradient. 이미지. 그라디언트이면 null. */
        private final DecodedImage image;
        /** The gradient, or null for an image. 그라디언트. 이미지이면 null. */
        private final GradientSpec gradient;
        /** Left of the first tile. 첫 타일의 왼쪽. */
        private final double originX;
        /** Top of the first tile. 첫 타일의 위쪽. */
        private final double originY;
        /** Tile width. 타일 너비. */
        private final double tileW;
        /** Tile height. 타일 높이. */
        private final double tileH;
        /** Distance between the left edges of neighbouring tiles. 이웃한 타일의 왼쪽 사이 거리. */
        private final double stepX;
        /** Distance between the top edges of neighbouring tiles. 이웃한 타일의 위쪽 사이 거리. */
        private final double stepY;
        /** Whether the layer repeats horizontally. 가로로 반복하는지 여부. */
        private final boolean repeatX;
        /** Whether the layer repeats vertically. 세로로 반복하는지 여부. */
        private final boolean repeatY;
        /** The picture of an image function of CSS Images 4, or null. CSS Images 4 이미지 함수의 그림. 없으면 null. */
        private GeneratedImages.Picture picture;

        /**
         * Makes the layer paint a picture of an image function (<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>) in every tile.
         * <p>
         * 레이어가 타일마다 이미지 함수(<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>)의 그림을 그리게 한다.
         *
         * @param p the picture or null / 그림 또는 null
         * @return this command / 이 명령
         */
        Layer withPicture(GeneratedImages.Picture p) {
            this.picture = p;
            return this;
        }

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param clip     the clip shape / 클립 도형
         * @param image    the image or null / 이미지 또는 null
         * @param gradient the gradient or null / 그라디언트 또는 null
         * @param originX  left of the first tile / 첫 타일의 왼쪽
         * @param originY  top of the first tile / 첫 타일의 위쪽
         * @param tileW    tile width / 타일 너비
         * @param tileH    tile height / 타일 높이
         * @param stepX    horizontal distance between tiles / 타일 사이 가로 거리
         * @param stepY    vertical distance between tiles / 타일 사이 세로 거리
         * @param repeatX  repeat horizontally / 가로 반복
         * @param repeatY  repeat vertically / 세로 반복
         * @param node     DOM node or null / DOM 노드 또는 null
         */
        Layer(Shape clip, DecodedImage image, GradientSpec gradient, double originX, double originY, double tileW, double tileH, double stepX,
                double stepY, boolean repeatX, boolean repeatY, Node node) {
            super(Type.BACKGROUND_IMAGE, clip.getBounds2D().getX(), clip.getBounds2D().getY(), clip.getBounds2D().getWidth(),
                    clip.getBounds2D().getHeight(), node);
            this.clip = clip;
            this.image = image;
            this.gradient = gradient;
            this.originX = originX;
            this.originY = originY;
            this.tileW = tileW;
            this.tileH = tileH;
            this.stepX = stepX;
            this.stepY = stepY;
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
            if (tileW < 0.5 || tileH < 0.5 || stepX < 0.5 || stepY < 0.5) {
                return;
            }
            Graphics2D g = ctx.g;
            Shape old = g.getClip();
            try {
                g.clip(clip);
                Rectangle2D vis = getBounds().createIntersection(ctx.clipDoc);
                BufferedImage bi = image == null ? null : DisplayCommands.imageOf(DisplayCommands.frameAt(image, ctx.timeMillis), image.getWidth(), image.getHeight());
                Paint paint = gradient == null ? null : gradient.createPaint(tileW, tileH);
                double startX = originX;
                double startY = originY;
                if (repeatX) {
                    startX = originX - Math.ceil((originX - vis.getMinX()) / stepX) * stepX;
                }
                if (repeatY) {
                    startY = originY - Math.ceil((originY - vis.getMinY()) / stepY) * stepY;
                }
                int guard = 0;
                // 보이는 영역을 덮을 때까지 타일을 가로, 세로로 놓는다(한 번에 그리는 타일 수는 제한한다).
                for (double ty = startY; ty < vis.getMaxY() && guard < 20000; ty += stepY) {
                    for (double tx = startX; tx < vis.getMaxX() && guard < 20000; tx += stepX) {
                        if (picture != null) {
                            picture.paint(g, tx, ty, tileW, tileH, ctx.timeMillis);
                        } else {
                            paintTile(g, bi, paint, tx, ty);
                        }
                        guard++;
                        if (!repeatX) {
                            break;
                        }
                    }
                    if (!repeatY) {
                        break;
                    }
                }
            } finally {
                g.setClip(old);
            }
        }

        /**
         * Paints one tile.
         * <p>
         * 타일 하나를 그린다.
         *
         * @param g     the graphics / 그래픽스
         * @param bi    the image or null / 이미지 또는 null
         * @param paint the gradient paint or null / 그라디언트 페인트 또는 null
         * @param tx    left of the tile / 타일의 왼쪽
         * @param ty    top of the tile / 타일의 위쪽
         */
        private void paintTile(Graphics2D g, BufferedImage bi, Paint paint, double tx, double ty) {
            if (bi != null) {
                Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                boolean scaled = Math.round(tileW) != bi.getWidth() || Math.round(tileH) != bi.getHeight();
                if (scaled) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }
                g.drawImage(bi, (int) Math.round(tx), (int) Math.round(ty), (int) Math.round(tileW), (int) Math.round(tileH), null);
                if (scaled && oldHint != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
                }
                return;
            }
            AffineTransform saved = g.getTransform();
            Paint oldPaint = g.getPaint();
            try {
                g.translate(tx, ty);
                g.setPaint(paint);
                g.fill(new Rectangle2D.Double(0, 0, tileW, tileH));
            } finally {
                g.setTransform(saved);
                g.setPaint(oldPaint);
            }
        }
    }

    /**
     * A background layer with <code>background-attachment: fixed</code> (CSS Backgrounds and Borders Level 3 section 3.9): the layer is
     * painted inside the clip shape of its box, but its tiles are positioned and sized against the <b>viewport</b>, so the image stays at the
     * same place of the viewport while the page scrolls behind the box. The viewport is known only when the page is painted, so the tiles
     * are computed at paint time ({@link BackgroundLayers#tiles}); without a known viewport the positioning area of the box is used, as for
     * <code>scroll</code>.
     * <p>
     * <code>background-attachment: fixed</code> 인 배경 레이어(CSS Backgrounds and Borders Level 3 3.9 절): 레이어는 상자의 클립 도형 안에 그려지지만 타일은
     * <b>뷰포트</b>를 기준으로 위치와 크기가 정해지므로, 페이지가 상자 뒤에서 스크롤되어도 이미지는 뷰포트의 같은 자리에 머문다. 뷰포트는 페이지를 그릴 때에야 알 수 있으므로
     * 타일을 그릴 때 계산한다({@link BackgroundLayers#tiles}). 뷰포트를 모르면 <code>scroll</code> 처럼 상자의 위치 기준 영역을 쓴다.
     */
    static final class FixedLayer extends DisplayCommand {
        /** The clip shape. 클립 도형. */
        private final Shape clip;
        /** The image, or null for a gradient. 이미지. 그라디언트이면 null. */
        private final DecodedImage image;
        /** The gradient, or null for an image. 그라디언트. 이미지이면 null. */
        private final GradientSpec gradient;
        /** The <code>background-size</code> value of the layer. 레이어의 <code>background-size</code> 값. */
        private final org.duckdns.hjow.swingwebpanel.css.values.CssValue size;
        /** The <code>background-position</code> value of the layer. 레이어의 <code>background-position</code> 값. */
        private final org.duckdns.hjow.swingwebpanel.css.values.CssValue position;
        /** The <code>background-repeat</code> value of the layer. 레이어의 <code>background-repeat</code> 값. */
        private final org.duckdns.hjow.swingwebpanel.css.values.CssValue repeat;
        /** The resolution of the image. 이미지의 해상도. */
        private final double resolution;
        /** The positioning area of the box {x, y, w, h}, used when the viewport is not known. 뷰포트를 모를 때 쓰는 상자의 위치 기준 영역 {x, y, w, h}. */
        private final double[] fallback;
        /** The picture of an image function of CSS Images 4, or null. CSS Images 4 이미지 함수의 그림. 없으면 null. */
        private GeneratedImages.Picture picture;

        /**
         * Makes the layer paint a picture of an image function in every tile.
         * <p>
         * 레이어가 타일마다 이미지 함수의 그림을 그리게 한다.
         *
         * @param p the picture or null / 그림 또는 null
         * @return this command / 이 명령
         */
        FixedLayer withPicture(GeneratedImages.Picture p) {
            this.picture = p;
            return this;
        }

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param clip       the clip shape / 클립 도형
         * @param image      the image or null / 이미지 또는 null
         * @param gradient   the gradient or null / 그라디언트 또는 null
         * @param size       the size value / 크기 값
         * @param position   the position value / 위치 값
         * @param repeat     the repeat value / 반복 값
         * @param resolution the resolution of the image / 이미지의 해상도
         * @param fallback   the positioning area of the box {x, y, w, h} / 상자의 위치 기준 영역 {x, y, w, h}
         * @param node       the DOM node or null / DOM 노드 또는 null
         */
        FixedLayer(Shape clip, DecodedImage image, GradientSpec gradient, org.duckdns.hjow.swingwebpanel.css.values.CssValue size, org.duckdns.hjow.swingwebpanel.css.values.CssValue position,
                org.duckdns.hjow.swingwebpanel.css.values.CssValue repeat, double resolution, double[] fallback, Node node) {
            super(Type.BACKGROUND_IMAGE, clip.getBounds2D().getX(), clip.getBounds2D().getY(), clip.getBounds2D().getWidth(), clip.getBounds2D().getHeight(),
                    node);
            this.clip = clip;
            this.image = image;
            this.gradient = gradient;
            this.size = size;
            this.position = position;
            this.repeat = repeat;
            this.resolution = resolution;
            this.fallback = fallback;
        }

        /**
         * Computes the tiles against the viewport of the paint pass and paints them.
         * <p>
         * 그리기 단계의 뷰포트를 기준으로 타일을 계산해 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            double[] area = ctx.viewport != null ? new double[] { ctx.viewport.getX(), ctx.viewport.getY(), ctx.viewport.getWidth(), ctx.viewport.getHeight() } : fallback;
            double[] g = picture != null ? BackgroundLayers.tiles(size, picture.naturalWidth(), picture.naturalHeight(), repeat, position, area[0], area[1], area[2], area[3])
                    : BackgroundLayers.tiles(size, image, resolution, repeat, position, area[0], area[1], area[2], area[3]);
            if (g == null) {
                return;
            }
            new Layer(clip, image, gradient, g[0], g[1], g[2], g[3], g[4], g[5], g[6] != 0, g[7] != 0, getNode()).withPicture(picture).paint(ctx);
        }
    }
}
