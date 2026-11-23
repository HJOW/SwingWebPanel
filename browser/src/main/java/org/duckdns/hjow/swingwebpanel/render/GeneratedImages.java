package org.duckdns.hjow.swingwebpanel.render;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.Locale;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The images that the image functions of CSS Images Level 4 generate (https://www.w3.org/TR/css-images-4/, W3C Working Draft of 24 September 2025):
 * <code>cross-fade()</code> (section 2.6: a weighted average of images and colors in premultiplied sRGB, with the natural size that is the weighted average of
 * the natural sizes of its images), <code>image()</code> (section 2.5: an image with a fallback color, a solid-color image, a fragment <code>#xywh=</code> of an
 * image, and the direction tags <code>ltr</code>/<code>rtl</code> that flip the image in a box of the opposite direction) and <code>element()</code> (section 2.7:
 * the rendering of an element of the document, made by the display list builder through {@link ElementPictures}). Every value of the type
 * <code>&lt;image&gt;</code> becomes a {@link Picture}: something with an optional natural size that paints itself into any rectangle. A picture is resolved
 * when the display list is built, so an image that has not arrived yet makes the function an invalid image (nothing is painted; inside <code>cross-fade()</code>
 * a transparent image without natural size), and the page is painted again when it arrives.
 * <p>
 * Use: {@link Resolver#resolve} turns a computed value into a picture (or null for an invalid image); the background layers, the border image and the mask
 * layers paint it with {@link Picture#paint}.
 * <p>
 * CSS Images Level 4 의 이미지 함수가 만드는 이미지(https://www.w3.org/TR/css-images-4/, W3C Working Draft 2025-09-24): <code>cross-fade()</code>(2.6 절: 이미지와 색을
 * 프리멀티플라이드 sRGB 에서 가중 평균하며, 고유 크기는 이미지들의 고유 크기의 가중 평균), <code>image()</code>(2.5 절: 대체 색이 있는 이미지, 단색 이미지, 이미지의 조각
 * <code>#xywh=</code>, 반대 방향의 상자에서 이미지를 뒤집는 방향 꼬리표 <code>ltr</code>/<code>rtl</code>), <code>element()</code>(2.7 절: 문서의 요소를 그린 그림. 디스플레이
 * 리스트 빌더가 {@link ElementPictures} 를 통해 만든다). <code>&lt;image&gt;</code> 형식의 모든 값은 {@link Picture} 가 된다: 고유 크기가 있을 수도 있고 어떤 사각형에도 스스로를
 * 그리는 것. 그림은 디스플레이 리스트를 만들 때 정하므로 아직 도착하지 않은 이미지는 함수를 잘못된 이미지로 만들고(아무것도 그리지 않으며, <code>cross-fade()</code> 안에서는 고유 크기
 * 없는 투명 이미지), 이미지가 도착하면 페이지를 다시 그린다.
 * <p>
 * 사용법: {@link Resolver#resolve} 가 계산 값을 그림으로 바꾸고(잘못된 이미지이면 null), 배경 레이어와 border 이미지와 마스크 레이어가 {@link Picture#paint} 로 그린다.
 */
final class GeneratedImages {

    /**
     * The largest number of pixels of a raster that a picture makes for itself (a larger tile is made smaller and scaled up when it is painted).
     * <p>
     * 그림이 스스로 만드는 래스터의 최대 화소 수(더 큰 타일은 작게 만들어 그릴 때 늘린다).
     */
    static final int MAX_RASTER_PIXELS = 4 * 1024 * 1024;

    /**
     * Not instantiable; the class holds the picture types and the resolver.
     * <p>
     * 인스턴스를 만들지 않는다. 그림 형식과 해석기를 담는 클래스이다.
     */
    private GeneratedImages() {
    }

    /**
     * Tells whether a value is one of the functions that this class resolves (<code>cross-fade()</code>, <code>-webkit-cross-fade()</code>, <code>image()</code>,
     * <code>element()</code>).
     * <p>
     * 값이 이 클래스가 해석하는 함수(<code>cross-fade()</code>, <code>-webkit-cross-fade()</code>, <code>image()</code>, <code>element()</code>)인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for those functions / 그 함수이면 true
     */
    static boolean isGenerated(CssValue v) {
        if (!(v instanceof CssFunctionValue)) {
            return false;
        }
        String n = ((CssFunctionValue) v).getName().toLowerCase(Locale.ENGLISH);
        return "cross-fade".equals(n) || "-webkit-cross-fade".equals(n) || "image".equals(n) || "element".equals(n);
    }

    /**
     * The renderer of <code>element()</code>: the display list builder, which knows the boxes of the document and paints them into a picture.
     * <p>
     * <code>element()</code> 를 그리는 쪽: 문서의 상자를 알고 그것을 그림으로 그리는 디스플레이 리스트 빌더.
     */
    interface ElementPictures {

        /**
         * Returns the picture of the element with an id, or null if it is an invalid image (no such element, not rendered and no paint source, or a cycle).
         * <p>
         * id 를 가진 요소의 그림을 반환한다. 잘못된 이미지이면(그런 요소가 없거나, 그려지지 않으며 그림 원본도 아니거나, 순환이면) null.
         *
         * @param id       the id without the <code>#</code> / <code>#</code> 를 뺀 id
         * @param referrer the node whose style uses the function / 함수를 쓰는 스타일의 노드
         * @return the picture or null / 그림 또는 null
         */
        Picture elementPicture(String id, Node referrer);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 그림
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * An image that paints itself into any rectangle; it may have natural dimensions.
     * <p>
     * 어떤 사각형에도 스스로를 그리는 이미지. 고유 크기가 있을 수 있다.
     */
    abstract static class Picture {

        /**
         * Returns the natural width in CSS pixels, or NaN if the picture has none.
         * <p>
         * 고유 너비(CSS 픽셀)를 반환한다. 없으면 NaN.
         *
         * @return the width or NaN / 너비 또는 NaN
         */
        double naturalWidth() {
            return Double.NaN;
        }

        /**
         * Returns the natural height in CSS pixels, or NaN if the picture has none.
         * <p>
         * 고유 높이(CSS 픽셀)를 반환한다. 없으면 NaN.
         *
         * @return the height or NaN / 높이 또는 NaN
         */
        double naturalHeight() {
            return Double.NaN;
        }

        /**
         * Tells whether the picture has a natural width and height.
         * <p>
         * 그림에 고유 너비와 높이가 있는지 알려 준다.
         *
         * @return true if it has both / 둘 다 있으면 true
         */
        final boolean hasNaturalSize() {
            return !Double.isNaN(naturalWidth()) && !Double.isNaN(naturalHeight()) && naturalWidth() > 0 && naturalHeight() > 0;
        }

        /**
         * Paints the picture scaled into a rectangle.
         * <p>
         * 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics (its state is restored) / 그래픽스(상태는 되돌린다)
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        abstract void paint(Graphics2D g, double x, double y, double w, double h, long time);

        /**
         * Paints the picture into a new raster of a size with premultiplied alpha.
         * <p>
         * 그림을 주어진 크기의 새 래스터에 프리멀티플라이드 알파로 그린다.
         *
         * @param w    width in pixels / 화소 너비
         * @param h    height in pixels / 화소 높이
         * @param time animation time / 애니메이션 시각
         * @return the raster / 래스터
         */
        BufferedImage raster(int w, int h, long time) {
            BufferedImage bi = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = bi.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                paint(g, 0, 0, w, h, time);
            } finally {
                g.dispose();
            }
            return bi;
        }
    }

    /**
     * A decoded raster image, or a part of it (an <code>#xywh=</code> fragment), possibly flipped in the inline direction.
     * <p>
     * 디코딩한 래스터 이미지나 그 일부(<code>#xywh=</code> 조각). 인라인 방향으로 뒤집힐 수 있다.
     */
    static final class RasterPicture extends Picture {
        /** The image. 이미지. */
        private final DecodedImage image;
        /** Device pixels per CSS pixel (the one of the <code>image-set()</code> candidate, else 1). CSS 픽셀당 장치 화소(<code>image-set()</code> 후보의 것, 아니면 1). */
        private final double resolution;
        /** The part {x, y, w, h} in image pixels. 이미지 화소 단위 부분 {x, y, w, h}. */
        private final int[] crop;
        /** Whether the picture is mirrored horizontally. 가로로 뒤집는지 여부. */
        private final boolean flip;

        /**
         * Creates the picture.
         * <p>
         * 그림을 만든다.
         *
         * @param image      the image / 이미지
         * @param resolution its resolution / 해상도
         * @param crop       the part {x, y, w, h} or null for all / 부분 {x, y, w, h}. 전체이면 null
         * @param flip       whether to mirror it / 뒤집을지 여부
         */
        RasterPicture(DecodedImage image, double resolution, int[] crop, boolean flip) {
            this.image = image;
            this.resolution = resolution > 0 ? resolution : 1;
            this.crop = crop != null ? crop : new int[] { 0, 0, image.getWidth(), image.getHeight() };
            this.flip = flip;
        }

        /**
         * Returns the natural width of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 너비(CSS 픽셀)를 반환한다.
         *
         * @return the width or NaN / 너비 또는 NaN
         */
        double naturalWidth() {
            return crop[2] / resolution;
        }

        /**
         * Returns the natural height of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 높이(CSS 픽셀)를 반환한다.
         *
         * @return the height or NaN / 높이 또는 NaN
         */
        double naturalHeight() {
            return crop[3] / resolution;
        }

        /**
         * Paints this picture scaled into a rectangle.
         * <p>
         * 이 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics / 그래픽스
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        void paint(Graphics2D g, double x, double y, double w, double h, long time) {
            if (w <= 0 || h <= 0 || crop[2] <= 0 || crop[3] <= 0) {
                return;
            }
            BufferedImage bi = DisplayCommands.imageOf(DisplayCommands.frameAt(image, time), image.getWidth(), image.getHeight());
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int dx1 = (int) Math.round(x);
                int dy1 = (int) Math.round(y);
                int dx2 = (int) Math.round(x + w);
                int dy2 = (int) Math.round(y + h);
                // 뒤집는 그림은 대상의 왼쪽과 오른쪽을 바꿔 그린다.
                if (flip) {
                    int t = dx1;
                    dx1 = dx2;
                    dx2 = t;
                }
                g.drawImage(bi, dx1, dy1, dx2, dy2, crop[0], crop[1], crop[0] + crop[2], crop[1] + crop[3], null);
            } finally {
                if (oldHint != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
                }
            }
        }
    }

    /**
     * A gradient: no natural size, the gradient line is laid out for the painted rectangle.
     * <p>
     * 그라디언트: 고유 크기가 없고 그라디언트 선은 그리는 사각형에 맞춰 놓인다.
     */
    static final class GradientPicture extends Picture {
        /** The gradient. 그라디언트. */
        private final GradientSpec spec;

        /**
         * Creates the picture.
         * <p>
         * 그림을 만든다.
         *
         * @param spec the gradient / 그라디언트
         */
        GradientPicture(GradientSpec spec) {
            this.spec = spec;
        }

        /**
         * Paints this picture scaled into a rectangle.
         * <p>
         * 이 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics / 그래픽스
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        void paint(Graphics2D g, double x, double y, double w, double h, long time) {
            if (w <= 0 || h <= 0) {
                return;
            }
            AffineTransform saved = g.getTransform();
            Paint old = g.getPaint();
            try {
                g.translate(x, y);
                g.setPaint(spec.createPaint(w, h));
                g.fill(new Rectangle2D.Double(0, 0, w, h));
            } finally {
                g.setTransform(saved);
                g.setPaint(old);
            }
        }
    }

    /**
     * A solid-color image without natural dimensions (CSS Images 4 section 2.5.3).
     * <p>
     * 고유 크기가 없는 단색 이미지(CSS Images 4 2.5.3 절).
     */
    static final class SolidPicture extends Picture {
        /** The ARGB color of the image. 이미지의 ARGB 색. */
        private final int argb;

        /**
         * Creates the picture.
         * <p>
         * 그림을 만든다.
         *
         * @param argb the color / 색
         */
        SolidPicture(int argb) {
            this.argb = argb;
        }

        /**
         * Returns the color.
         * <p>
         * 색을 반환한다.
         *
         * @return ARGB / ARGB
         */
        int getColor() {
            return argb;
        }

        /**
         * Paints this picture scaled into a rectangle.
         * <p>
         * 이 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics / 그래픽스
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        void paint(Graphics2D g, double x, double y, double w, double h, long time) {
            if (w <= 0 || h <= 0 || (argb >>> 24) == 0) {
                return;
            }
            java.awt.Color old = g.getColor();
            try {
                g.setColor(new java.awt.Color(argb, true));
                g.fill(new Rectangle2D.Double(x, y, w, h));
            } finally {
                g.setColor(old);
            }
        }
    }

    /**
     * The picture of <code>cross-fade()</code> (CSS Images 4 sections 2.6.1 and 2.6.2): the inputs with their percentages normalized so that they and a
     * transparent leftover add up to 100%; the natural size is the weighted average of the natural sizes of the inputs that have one (colors have none), and
     * every pixel is the weighted average of the pixels of the inputs rescaled to the painted size, in premultiplied sRGB (both the color and the alpha
     * channels). The last raster is kept, so the repeated tiles of a background are made once.
     * <p>
     * <code>cross-fade()</code> 의 그림(CSS Images 4 2.6.1, 2.6.2 절): 입력과 그 백분율을 입력과 투명한 나머지의 합이 100% 가 되도록 정규화한다. 고유 크기는 고유 크기가 있는
     * 입력들(색은 없음)의 고유 크기를 가중 평균한 것이고, 화소마다 그리는 크기로 늘린 입력의 화소를 프리멀티플라이드 sRGB 에서 가중 평균한다(색과 알파 채널 모두). 마지막
     * 래스터를 보관하므로 배경의 반복 타일은 한 번만 만든다.
     */
    static final class CrossFadePicture extends Picture {
        /** The inputs; null for an invalid image (transparent black). 입력. 잘못된 이미지(투명한 검정)이면 null. */
        private final Picture[] inputs;
        /** The weights of the inputs, normalized (with the leftover they add up to 1). 입력의 정규화한 가중치(나머지와 합이 1). */
        private final double[] weights;
        /** The natural width or NaN. 고유 너비 또는 NaN. */
        private final double naturalW;
        /** The natural height or NaN. 고유 높이 또는 NaN. */
        private final double naturalH;
        /** The last raster made, or null. 마지막으로 만든 래스터. 없으면 null. */
        private BufferedImage cached;
        /** The size and time {w, h, time} of the cached raster. 보관한 래스터의 크기와 시각 {w, h, 시각}. */
        private long[] cachedKey;

        /**
         * Creates the picture from the inputs and their percentages (NaN for an omitted percentage), normalizing them as section 2.6 says: the omitted
         * percentages share what the given ones leave of 100% (at least 0), a sum above 100% is scaled down to 100%, and a sum below leaves a transparent
         * leftover.
         * <p>
         * 입력과 백분율(빠진 백분율은 NaN)로 그림을 만들며 2.6 절대로 정규화한다: 빠진 백분율은 주어진 것이 100% 에서 남긴 것(최소 0)을 나누어 가지고, 합이 100% 를 넘으면 100% 로
         * 줄이며, 모자라면 투명한 나머지가 생긴다.
         *
         * @param inputs   the inputs (null for invalid images) / 입력(잘못된 이미지는 null)
         * @param percents the percentages or NaN / 백분율 또는 NaN
         */
        CrossFadePicture(Picture[] inputs, double[] percents) {
            this.inputs = inputs;
            int n = inputs.length;
            double given = 0;
            int omitted = 0;
            // 주어진 백분율을 더하고 빠진 것의 수를 센다.
            for (int i = 0; i < n; i++) {
                if (Double.isNaN(percents[i])) {
                    omitted++;
                } else {
                    given += percents[i];
                }
            }
            double share = omitted == 0 ? 0 : Math.max(0, 100 - given) / omitted;
            double[] p = new double[n];
            double sum = 0;
            // 빠진 백분율을 채운다.
            for (int i = 0; i < n; i++) {
                p[i] = Double.isNaN(percents[i]) ? share : percents[i];
                sum += p[i];
            }
            double total = Math.max(100, sum);
            weights = new double[n];
            // 합이 100 을 넘으면 100 으로 줄이고, 모자라면 나머지는 투명한 입력이다.
            for (int i = 0; i < n; i++) {
                weights[i] = total > 0 ? p[i] / total : 0;
            }
            double leftover = sum >= 100 ? 0 : 100 - sum;
            double sw = 0;
            double sh = 0;
            double sp = 0;
            // 고유 크기가 있는 이미지 입력의 크기를 백분율로 가중 평균한다(색과 크기 없는 이미지는 빠진다).
            for (int i = 0; i < n; i++) {
                if (inputs[i] != null && !(inputs[i] instanceof SolidPicture) && inputs[i].hasNaturalSize()) {
                    sw += inputs[i].naturalWidth() * p[i];
                    sh += inputs[i].naturalHeight() * p[i];
                    sp += p[i];
                }
            }
            if (leftover >= 100 || sp <= 0) {
                naturalW = Double.NaN;
                naturalH = Double.NaN;
            } else {
                naturalW = sw / sp;
                naturalH = sh / sp;
            }
        }

        /**
         * Returns the normalized weight of an input (for tests and diagnostics).
         * <p>
         * 입력의 정규화한 가중치를 반환한다(시험과 진단용).
         *
         * @param i the index / 순번
         * @return the weight between 0 and 1 / 0~1 의 가중치
         */
        double weight(int i) {
            return weights[i];
        }

        /**
         * Returns the natural width of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 너비(CSS 픽셀)를 반환한다.
         *
         * @return the width or NaN / 너비 또는 NaN
         */
        double naturalWidth() {
            return naturalW;
        }

        /**
         * Returns the natural height of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 높이(CSS 픽셀)를 반환한다.
         *
         * @return the height or NaN / 높이 또는 NaN
         */
        double naturalHeight() {
            return naturalH;
        }

        /**
         * Paints this picture scaled into a rectangle.
         * <p>
         * 이 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics / 그래픽스
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        void paint(Graphics2D g, double x, double y, double w, double h, long time) {
            if (w <= 0 || h <= 0) {
                return;
            }
            int iw = Math.max(1, (int) Math.round(w));
            int ih = Math.max(1, (int) Math.round(h));
            // 너무 큰 타일은 화소 수 한도 안으로 줄여 만든 뒤 늘려 그린다.
            if ((long) iw * ih > MAX_RASTER_PIXELS) {
                double f = Math.sqrt((double) MAX_RASTER_PIXELS / ((double) iw * ih));
                iw = Math.max(1, (int) (iw * f));
                ih = Math.max(1, (int) (ih * f));
            }
            BufferedImage bi = raster(iw, ih, time);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            java.awt.Composite oldComposite = g.getComposite();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(bi, (int) Math.round(x), (int) Math.round(y), (int) Math.round(x + w) - (int) Math.round(x),
                        (int) Math.round(y + h) - (int) Math.round(y), null);
            } finally {
                g.setComposite(oldComposite);
                if (oldHint != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
                }
            }
        }

        /**
         * Makes the raster of the cross-fade at a size (the last one is reused): the weighted sum of the premultiplied pixels of the inputs.
         * <p>
         * 주어진 크기로 cross-fade 의 래스터를 만든다(마지막 것은 다시 쓴다): 입력의 프리멀티플라이드 화소의 가중 합.
         *
         * @param w    width in pixels / 화소 너비
         * @param h    height in pixels / 화소 높이
         * @param time animation time / 애니메이션 시각
         * @return the raster / 래스터
         */
        synchronized BufferedImage raster(int w, int h, long time) {
            if (cached != null && cachedKey[0] == w && cachedKey[1] == h && cachedKey[2] == time) {
                return cached;
            }
            int count = w * h;
            double[] acc = new double[count * 4];
            // 입력마다 그리는 크기의 프리멀티플라이드 래스터를 만들어 가중치를 곱해 더한다.
            for (int i = 0; i < inputs.length; i++) {
                double wt = weights[i];
                if (inputs[i] == null || wt <= 0) {
                    continue;
                }
                if (inputs[i] instanceof SolidPicture) {
                    int c = ((SolidPicture) inputs[i]).getColor();
                    double a = (c >>> 24) / 255.0;
                    double pa = a * 255 * wt;
                    double pr = ((c >> 16) & 255) * a * wt;
                    double pg = ((c >> 8) & 255) * a * wt;
                    double pb = (c & 255) * a * wt;
                    // 단색은 모든 화소에 같은 값을 더한다.
                    for (int k = 0; k < count; k++) {
                        acc[k * 4] += pa;
                        acc[k * 4 + 1] += pr;
                        acc[k * 4 + 2] += pg;
                        acc[k * 4 + 3] += pb;
                    }
                    continue;
                }
                BufferedImage in = inputs[i].raster(w, h, time);
                int[] px = ((DataBufferInt) in.getRaster().getDataBuffer()).getData();
                // 프리멀티플라이드 화소의 네 채널에 가중치를 곱해 더한다.
                for (int k = 0; k < count; k++) {
                    int c = px[k];
                    acc[k * 4] += (c >>> 24) * wt;
                    acc[k * 4 + 1] += ((c >> 16) & 255) * wt;
                    acc[k * 4 + 2] += ((c >> 8) & 255) * wt;
                    acc[k * 4 + 3] += (c & 255) * wt;
                }
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            int[] dst = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
            // 모은 값을 반올림해 프리멀티플라이드 화소로 묶는다(색 채널은 알파를 넘지 않게 한다).
            for (int k = 0; k < count; k++) {
                int a = clamp(acc[k * 4]);
                int r = Math.min(a, clamp(acc[k * 4 + 1]));
                int gg = Math.min(a, clamp(acc[k * 4 + 2]));
                int b = Math.min(a, clamp(acc[k * 4 + 3]));
                dst[k] = (a << 24) | (r << 16) | (gg << 8) | b;
            }
            cached = out;
            cachedKey = new long[] { w, h, time };
            return out;
        }

        /**
         * Rounds a channel value to 0 to 255.
         * <p>
         * 채널 값을 0~255 로 반올림한다.
         *
         * @param v the value / 값
         * @return the channel / 채널
         */
        private static int clamp(double v) {
            int i = (int) Math.round(v);
            return i < 0 ? 0 : (i > 255 ? 255 : i);
        }
    }

    /**
     * The picture of <code>element()</code> for a rendered element (CSS Images 4 section 2.7): the display commands that paint the element and its descendants
     * (without the transforms of the element and its ancestors), and the decorated bounding box, which is the natural size and whose edges are the edges of the
     * image. Anything outside the box is clipped.
     * <p>
     * 그려지는 요소의 <code>element()</code> 그림(CSS Images 4 2.7 절): 요소와 자손을 (요소와 조상의 변환 없이) 그리는 디스플레이 명령과 장식된 경계 상자. 경계 상자가 고유
     * 크기이며 그 가장자리가 이미지의 가장자리이다. 상자 밖은 잘린다.
     */
    static final class ElementPicture extends Picture {
        /** The commands, in document coordinates. 문서 좌표의 명령. */
        private final DisplayCommand[] commands;
        /** The decorated bounding box in document coordinates. 문서 좌표의 장식된 경계 상자. */
        private final Rectangle2D bounds;

        /**
         * Creates the picture.
         * <p>
         * 그림을 만든다.
         *
         * @param commands the commands / 명령
         * @param bounds   the decorated bounding box / 장식된 경계 상자
         */
        ElementPicture(List<DisplayCommand> commands, Rectangle2D bounds) {
            this.commands = commands.toArray(new DisplayCommand[commands.size()]);
            this.bounds = bounds;
        }

        /**
         * Returns the natural width of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 너비(CSS 픽셀)를 반환한다.
         *
         * @return the width or NaN / 너비 또는 NaN
         */
        double naturalWidth() {
            return bounds.getWidth();
        }

        /**
         * Returns the natural height of this picture in CSS pixels.
         * <p>
         * 이 그림의 고유 높이(CSS 픽셀)를 반환한다.
         *
         * @return the height or NaN / 높이 또는 NaN
         */
        double naturalHeight() {
            return bounds.getHeight();
        }

        /**
         * Paints this picture scaled into a rectangle.
         * <p>
         * 이 그림을 사각형에 맞춰 그린다.
         *
         * @param g    the graphics / 그래픽스
         * @param x    left / 왼쪽
         * @param y    top / 위쪽
         * @param w    width / 너비
         * @param h    height / 높이
         * @param time animation time in milliseconds / 애니메이션 시각(밀리초)
         */
        void paint(Graphics2D g, double x, double y, double w, double h, long time) {
            if (w <= 0 || h <= 0 || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                return;
            }
            Graphics2D t = (Graphics2D) g.create();
            try {
                t.clip(new Rectangle2D.Double(x, y, w, h));
                t.translate(x, y);
                t.scale(w / bounds.getWidth(), h / bounds.getHeight());
                t.translate(-bounds.getX(), -bounds.getY());
                PagePainter.paintCommands(new PaintContext(t, bounds, time), commands);
            } finally {
                t.dispose();
            }
        }
    }

    /**
     * A display command that paints a picture into a rectangle (the image of a list marker).
     * <p>
     * 그림을 사각형에 그리는 디스플레이 명령(목록 표식의 이미지).
     */
    static final class Draw extends DisplayCommand {
        /** The picture. 그림. */
        private final Picture picture;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param picture the picture / 그림
         * @param x       left / 왼쪽
         * @param y       top / 위쪽
         * @param w       width / 너비
         * @param h       height / 높이
         * @param node    the DOM node / DOM 노드
         */
        Draw(Picture picture, double x, double y, double w, double h, Node node) {
            super(Type.BACKGROUND_IMAGE, x, y, w, h, node);
            this.picture = picture;
        }

        /**
         * Paints the picture into the bounds of the command.
         * <p>
         * 그림을 명령의 경계에 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Rectangle2D b = getBounds();
            picture.paint(ctx.g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), ctx.timeMillis);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 해석기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Turns <code>&lt;image&gt;</code> values into pictures for the boxes of one display list.
     * <p>
     * 디스플레이 리스트 하나의 상자들을 위해 <code>&lt;image&gt;</code> 값을 그림으로 바꾼다.
     */
    static final class Resolver {
        /** The image states, or null. 이미지 상태. 없으면 null. */
        private final ImageSource images;
        /** The background layer builder (for <code>image-set()</code> and URLs). 배경 레이어 빌더(<code>image-set()</code> 과 URL 용). */
        private final BackgroundLayers layers;
        /** The renderer of <code>element()</code>, or null. <code>element()</code> 를 그리는 쪽. 없으면 null. */
        private ElementPictures elements;

        /**
         * Creates the resolver.
         * <p>
         * 해석기를 만든다.
         *
         * @param images the image states or null / 이미지 상태 또는 null
         * @param layers the layer builder / 레이어 빌더
         */
        Resolver(ImageSource images, BackgroundLayers layers) {
            this.images = images;
            this.layers = layers;
        }

        /**
         * Installs the renderer of <code>element()</code>.
         * <p>
         * <code>element()</code> 를 그리는 쪽을 설치한다.
         *
         * @param elements the renderer or null / 그리는 쪽 또는 null
         */
        void setElementPictures(ElementPictures elements) {
            this.elements = elements;
        }

        /**
         * Resolves an image value of a box into a picture.
         * <p>
         * 상자의 이미지 값을 그림으로 바꾼다.
         *
         * @param v    the value / 값
         * @param node the node of the box (the referrer of <code>element()</code>) / 상자의 노드(<code>element()</code> 의 참조자)
         * @param s    the style of the box (its direction, <code>currentcolor</code> and <code>image-orientation</code>) / 상자의 스타일(방향, <code>currentcolor</code>,
         *             <code>image-orientation</code>)
         * @return the picture, or null for an invalid or loading image / 그림. 잘못되었거나 불러오는 중인 이미지이면 null
         */
        Picture resolve(CssValue v, Node node, ComputedStyle s) {
            return resolve(v, node, s, 0);
        }

        /**
         * Resolves an image value, counting the nesting of functions (a deep nesting is an invalid image).
         * <p>
         * 함수의 중첩을 세며 이미지 값을 해석한다(너무 깊은 중첩은 잘못된 이미지).
         *
         * @param v     the value / 값
         * @param node  the node / 노드
         * @param s     the style / 스타일
         * @param depth the nesting depth / 중첩 깊이
         * @return the picture or null / 그림 또는 null
         */
        private Picture resolve(CssValue v, Node node, ComputedStyle s, int depth) {
            if (v == null || depth > 16) {
                return null;
            }
            int currentColor = s == null ? 0xFF000000 : s.color(Prop.COLOR);
            if (v instanceof CssColor) {
                return new SolidPicture(((CssColor) v).getArgb());
            }
            if (v.isKeyword("currentcolor")) {
                return new SolidPicture(currentColor);
            }
            double[] res = { 1 };
            CssValue img = layers.pickImage(v, res);
            if (img instanceof CssUrl) {
                if (BackgroundLayers.fragmentReference(img) != null) {
                    return null;
                }
                DecodedImage di = loaded((CssUrl) img);
                if (di == null) {
                    return null;
                }
                boolean none = s != null && s.isKeyword(Prop.IMAGE_ORIENTATION, "none");
                return new RasterPicture(none ? di.unoriented() : di, res[0], null, false);
            }
            if (!(img instanceof CssFunctionValue)) {
                return null;
            }
            CssFunctionValue f = (CssFunctionValue) img;
            if (f.isNamed("cross-fade")) {
                return crossFade(f, node, s, depth);
            }
            if (f.isNamed("-webkit-cross-fade")) {
                return legacyCrossFade(f, node, s, depth);
            }
            if (f.isNamed("image")) {
                return image(f, s, currentColor);
            }
            if (f.isNamed("element")) {
                String id = f.getArguments().isEmpty() ? null : f.getArguments().get(0).toCssText();
                if (id == null || !id.startsWith("#") || elements == null) {
                    return null;
                }
                return elements.elementPicture(id.substring(1), node);
            }
            GradientSpec g = GradientSpec.parse(f, currentColor);
            return g == null ? null : new GradientPicture(g);
        }

        /**
         * Returns the decoded image of a URL if it has arrived (and starts fetching it otherwise).
         * <p>
         * URL 의 이미지가 도착했으면 디코딩한 이미지를 반환한다(아니면 가져오기를 시작한다).
         *
         * @param u the URL value / URL 값
         * @return the image or null / 이미지 또는 null
         */
        private DecodedImage loaded(CssUrl u) {
            return loaded(layers.absolute(u));
        }

        /**
         * Returns the decoded image of an absolute URL if it has arrived.
         * <p>
         * 절대 URL 의 이미지가 도착했으면 디코딩한 이미지를 반환한다.
         *
         * @param url the URL / URL
         * @return the image or null / 이미지 또는 null
         */
        private DecodedImage loaded(String url) {
            ImageResource r = images == null || url == null ? null : images.getImage(url);
            return r == null || r.getState() != ImageResource.State.LOADED ? null : r.getImage();
        }

        /**
         * Resolves <code>cross-fade()</code>: every argument is an image or a color with an optional percentage.
         * <p>
         * <code>cross-fade()</code> 를 해석한다: 인수마다 이미지나 색과 선택적인 백분율이다.
         *
         * @param f     the function / 함수
         * @param node  the node / 노드
         * @param s     the style / 스타일
         * @param depth the nesting depth / 중첩 깊이
         * @return the picture or null / 그림 또는 null
         */
        private Picture crossFade(CssFunctionValue f, Node node, ComputedStyle s, int depth) {
            List<CssValue> args = f.getArguments();
            if (args.isEmpty()) {
                return null;
            }
            Picture[] inputs = new Picture[args.size()];
            double[] percents = new double[args.size()];
            // 인수마다 백분율과 이미지(색)를 나눠 읽는다.
            for (int i = 0; i < args.size(); i++) {
                CssValue a = args.get(i);
                List<CssValue> parts = a instanceof CssList && !((CssList) a).isComma() ? ((CssList) a).getItems() : java.util.Collections.singletonList(a);
                percents[i] = Double.NaN;
                CssValue picture = null;
                // 성분 중 백분율이 아닌 것이 이미지나 색이다.
                for (int k = 0; k < parts.size(); k++) {
                    if (parts.get(k) instanceof CssPercentage) {
                        percents[i] = ((CssPercentage) parts.get(k)).getValue();
                    } else {
                        picture = parts.get(k);
                    }
                }
                // 잘못되었거나 불러오는 중인 입력은 고유 크기 없는 투명 이미지로 본다(CSS Images 3 의 잘못된 이미지).
                inputs[i] = resolve(picture, node, s, depth + 1);
            }
            return new CrossFadePicture(inputs, percents);
        }

        /**
         * Resolves the legacy <code>-webkit-cross-fade(A, B, p)</code> as <code>cross-fade(A (1-p), B p)</code>.
         * <p>
         * 옛 형식 <code>-webkit-cross-fade(A, B, p)</code> 를 <code>cross-fade(A (1-p), B p)</code> 로 해석한다.
         *
         * @param f     the function / 함수
         * @param node  the node / 노드
         * @param s     the style / 스타일
         * @param depth the nesting depth / 중첩 깊이
         * @return the picture or null / 그림 또는 null
         */
        private Picture legacyCrossFade(CssFunctionValue f, Node node, ComputedStyle s, int depth) {
            List<CssValue> args = f.getArguments();
            if (args.size() != 3) {
                return null;
            }
            double p;
            if (args.get(2) instanceof CssPercentage) {
                p = ((CssPercentage) args.get(2)).getValue() / 100.0;
            } else if (args.get(2) instanceof CssNumber) {
                p = ((CssNumber) args.get(2)).getValue();
            } else {
                return null;
            }
            Picture a = resolve(args.get(0), node, s, depth + 1);
            Picture b = resolve(args.get(1), node, s, depth + 1);
            return new CrossFadePicture(new Picture[] { a, b }, new double[] { (1 - p) * 100, p * 100 });
        }

        /**
         * Resolves <code>image()</code> (CSS Images 4 section 2.5): the image of the URL (always oriented by its metadata, a fragment <code>#xywh=</code> cuts a
         * part out, an unknown fragment makes an invalid image), flipped horizontally if a direction tag differs from the direction of the box; if the URL is
         * invalid or loading, the color (a solid-color image without natural size) if one is given; only a color is a solid-color image.
         * <p>
         * <code>image()</code> 를 해석한다(CSS Images 4 2.5 절): URL 의 이미지(메타데이터의 방향을 항상 적용하고, 조각 <code>#xywh=</code> 는 일부를 잘라 내며, 모르는 조각은 잘못된
         * 이미지), 방향 꼬리표가 상자의 방향과 다르면 가로로 뒤집는다. URL 이 잘못되었거나 불러오는 중이면 색이 있을 때 그 색(고유 크기 없는 단색 이미지). 색만 있으면 단색 이미지.
         *
         * @param f            the function / 함수
         * @param s            the style / 스타일
         * @param currentColor the color of <code>currentcolor</code> / <code>currentcolor</code> 의 색
         * @return the picture or null / 그림 또는 null
         */
        private Picture image(CssFunctionValue f, ComputedStyle s, int currentColor) {
            List<CssValue> args = f.getArguments();
            if (args.isEmpty()) {
                return null;
            }
            List<CssValue> head = args.get(0) instanceof CssList ? ((CssList) args.get(0)).getItems() : java.util.Collections.singletonList(args.get(0));
            String tag = null;
            CssUrl src = null;
            CssValue color = args.size() > 1 ? args.get(1) : null;
            // 첫 인수에서 방향 꼬리표와 URL(또는 색)을 찾는다.
            for (int i = 0; i < head.size(); i++) {
                CssValue h = head.get(i);
                if (h.isKeyword("ltr") || h.isKeyword("rtl")) {
                    tag = ((CssKeyword) h).getName();
                } else if (h instanceof CssUrl) {
                    src = (CssUrl) h;
                } else {
                    color = h;
                }
            }
            if (src != null) {
                Picture p = fragmentOf(src, tag != null && s != null && !tag.equals(s.keyword(Prop.DIRECTION)));
                if (p != null) {
                    return p;
                }
            }
            if (color instanceof CssColor) {
                return new SolidPicture(((CssColor) color).getArgb());
            }
            if (color != null && color.isKeyword("currentcolor")) {
                return new SolidPicture(currentColor);
            }
            return null;
        }

        /**
         * Returns the image of the URL of <code>image()</code>, cut to its media fragment <code>#xywh=[pixel:|percent:]x,y,w,h</code> (Media Fragments URI 1.0
         * spatial dimension); null if the image is invalid, loading, or the fragment is not understood or does not meet the image.
         * <p>
         * <code>image()</code> 의 URL 이미지를 미디어 조각 <code>#xywh=[pixel:|percent:]x,y,w,h</code>(Media Fragments URI 1.0 의 공간 차원)로 잘라 반환한다. 이미지가
         * 잘못되었거나 불러오는 중이거나 조각을 알 수 없거나 이미지와 만나지 않으면 null.
         *
         * @param src  the URL / URL
         * @param flip whether to mirror the image / 이미지를 뒤집을지 여부
         * @return the picture or null / 그림 또는 null
         */
        private Picture fragmentOf(CssUrl src, boolean flip) {
            String url = layers.absolute(src);
            if (url == null) {
                return null;
            }
            int hash = url.indexOf('#');
            String fragment = hash < 0 ? null : url.substring(hash + 1);
            DecodedImage di = loaded(hash < 0 ? url : url.substring(0, hash));
            if (di == null) {
                return null;
            }
            int[] crop = null;
            if (fragment != null && fragment.length() > 0) {
                crop = xywh(fragment, di.getWidth(), di.getHeight());
                if (crop == null) {
                    return null;
                }
            }
            return new RasterPicture(di, 1, crop, flip);
        }
    }

    /**
     * Parses a spatial media fragment <code>xywh=[pixel:|percent:]x,y,w,h</code> and clips it to the image (Media Fragments URI 1.0 section 4.2.2).
     * <p>
     * 공간 미디어 조각 <code>xywh=[pixel:|percent:]x,y,w,h</code> 를 파싱해 이미지 안으로 자른다(Media Fragments URI 1.0 4.2.2 절).
     *
     * @param fragment the fragment without the <code>#</code> / <code>#</code> 를 뺀 조각
     * @param iw       the width of the image / 이미지 너비
     * @param ih       the height of the image / 이미지 높이
     * @return {x, y, w, h} in image pixels, or null if the fragment is not such a fragment or the part is empty / 이미지 화소 단위 {x, y, w, h}. 그런 조각이 아니거나 부분이 비면 null
     */
    static int[] xywh(String fragment, int iw, int ih) {
        if (!fragment.startsWith("xywh=")) {
            return null;
        }
        String rest = fragment.substring(5);
        boolean percent = false;
        if (rest.startsWith("pixel:")) {
            rest = rest.substring(6);
        } else if (rest.startsWith("percent:")) {
            rest = rest.substring(8);
            percent = true;
        }
        String[] parts = rest.split(",", -1);
        if (parts.length != 4) {
            return null;
        }
        double[] v = new double[4];
        // 네 수는 음이 아닌 정수이다(percent 는 소수도 받는다).
        for (int i = 0; i < 4; i++) {
            String p = parts[i];
            if (p.length() == 0 || !p.matches(percent ? "[0-9]+(\\.[0-9]+)?" : "[0-9]+")) {
                return null;
            }
            v[i] = Double.parseDouble(p);
        }
        if (percent) {
            v[0] = v[0] * iw / 100.0;
            v[2] = v[2] * iw / 100.0;
            v[1] = v[1] * ih / 100.0;
            v[3] = v[3] * ih / 100.0;
        }
        if (v[2] <= 0 || v[3] <= 0) {
            return null;
        }
        int x0 = (int) Math.round(Math.max(0, v[0]));
        int y0 = (int) Math.round(Math.max(0, v[1]));
        int x1 = (int) Math.round(Math.min(iw, v[0] + v[2]));
        int y1 = (int) Math.round(Math.min(ih, v[1] + v[3]));
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        return new int[] { x0, y0, x1 - x0, y1 - y0 };
    }
}
