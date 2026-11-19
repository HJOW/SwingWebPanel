package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.OrientedContent;
import org.duckdns.hjow.swingwebpanel.layout.ReplacedContent;
import org.duckdns.hjow.swingwebpanel.layout.ReplacedFallback;
import org.duckdns.hjow.swingwebpanel.layout.ReplacedResolver;
import org.duckdns.hjow.swingwebpanel.layout.ShapeImage;
import org.duckdns.hjow.swingwebpanel.layout.ShapeImageSource;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Gives the layout the natural size of loaded <code>img</code> elements, read from an {@link ImageSource}; images that are not loaded give
 * no content, so the layout falls back to the <code>width</code>/<code>height</code> attributes and its defaults (plan items P09-02,
 * P18-05).
 * <p>
 * 읽은 <code>img</code> 요소의 자연 크기를 {@link ImageSource} 에서 읽어 레이아웃에 알려 준다. 읽지 않은 이미지는 콘텐츠를 주지 않으므로 레이아웃이
 * <code>width</code>/<code>height</code> 속성과 기본값을 쓴다(계획 P09-02, P18-05).
 */
public final class ImageReplacedResolver implements ReplacedResolver, ReplacedFallback, ShapeImageSource, org.duckdns.hjow.swingwebpanel.layout.MarkerImageSource {

    /**
     * The image states.
     * <p>
     * 이미지 상태.
     */
    private final ImageSource images;

    /**
     * The media environment (viewport and resolution) that <code>srcset</code>, <code>sizes</code> and <code>picture</code> are chosen with.
     * <p>
     * <code>srcset</code>, <code>sizes</code>, <code>picture</code> 를 고를 때 쓰는 미디어 환경(뷰포트와 해상도).
     */
    private final MediaEnvironment environment;

    /**
     * Returns the natural size of the image of a list marker (<code>list-style-image</code>): any <code>&lt;image&gt;</code> that the renderer paints
     * (a URL, <code>image-set()</code>, a gradient, <code>cross-fade()</code>, <code>image()</code>); <code>element()</code> is not known before the layout and
     * an image that has not arrived is null, so the marker uses <code>list-style-type</code> until it arrives.
     * <p>
     * 목록 표식 이미지(<code>list-style-image</code>)의 고유 크기를 반환한다: 렌더러가 그리는 모든 <code>&lt;image&gt;</code>(URL, <code>image-set()</code>, 그라디언트,
     * <code>cross-fade()</code>, <code>image()</code>). <code>element()</code> 는 배치 전에 알 수 없고 도착하지 않은 이미지는 null 이므로 도착할 때까지 표식은
     * <code>list-style-type</code> 을 쓴다.
     *
     * @param image   the value / 값
     * @param element the list item / 목록 항목
     * @return {width, height} (NaN without a natural size) or null / {너비, 높이}(고유 크기가 없으면 NaN). 없으면 null
     */
    public double[] markerImageSize(org.duckdns.hjow.swingwebpanel.css.values.CssValue image, Element element) {
        if (image instanceof org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue && ((org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue) image).isNamed("element")) {
            return null;
        }
        BackgroundLayers layers = new BackgroundLayers(images, element.getOwnerDocument());
        GeneratedImages.Picture p = layers.anyPicture(image, element, null);
        return p == null ? null : new double[] {p.naturalWidth(), p.naturalHeight()};
    }

    /**
     * Creates the resolver with a default 800 x 600 viewport.
     * <p>
     * 기본 800 x 600 뷰포트로 리졸버를 만든다.
     *
     * @param images the image states, may be null / 이미지 상태. null 일 수 있다
     */
    public ImageReplacedResolver(ImageSource images) {
        this(images, 800, 600);
    }

    /**
     * Creates the resolver for a viewport.
     * <p>
     * 뷰포트에 맞는 리졸버를 만든다.
     *
     * @param images    the image states, may be null / 이미지 상태. null 일 수 있다
     * @param viewportW the viewport width in px / 뷰포트 너비(px)
     * @param viewportH the viewport height in px / 뷰포트 높이(px)
     */
    public ImageReplacedResolver(ImageSource images, double viewportW, double viewportH) {
        this.images = images;
        this.environment = new MediaEnvironment(viewportW, viewportH);
    }

    /**
     * Returns the natural size of a loaded image.
     * <p>
     * 읽은 이미지의 자연 크기를 반환한다.
     *
     * @param element the element / 요소
     * @return the content, or null if it is not a loaded image / 콘텐츠. 읽은 이미지가 아니면 null
     */
    public ReplacedContent getReplacedContent(Element element) {
        // 캔버스의 자연 크기는 width, height 속성이며 각각 기본값 300, 150 을 쓴다(이미지와 무관하다).
        if ("canvas".equals(element.getLocalName())) {
            final double cw = CanvasSizes.width(element);
            final double ch = CanvasSizes.height(element);
            return new ReplacedContent() {
                /**
                 * Returns the width of the bitmap of the canvas.
                 * <p>
                 * 캔버스 비트맵의 너비를 반환한다.
                 *
                 * @return width in px / 너비(px)
                 */
                public double getNaturalWidth() {
                    return cw;
                }

                /**
                 * Returns the height of the bitmap of the canvas.
                 * <p>
                 * 캔버스 비트맵의 높이를 반환한다.
                 *
                 * @return height in px / 높이(px)
                 */
                public double getNaturalHeight() {
                    return ch;
                }
            };
        }
        if (images == null || !"img".equals(element.getLocalName())) {
            return null;
        }
        ImageSelection.Choice choice = ImageSelection.select(element, environment);
        if (choice == null) {
            return null;
        }
        String url = absoluteUrl(element, choice.getUrl());
        ImageResource r = ImageLookup.of(images, element, url);
        if (r == null || r.getState() != ImageResource.State.LOADED || r.getImage() == null) {
            return null;
        }
        final DecodedImage di = r.getImage();
        final double density = choice.getDensity();
        // 방향이 적용된 이미지는 image-orientation: none 일 때 저장된 크기를 줄 수 있도록 OrientedContent 로 돌려준다.
        if (di.getAppliedOrientation() != 1) {
            return new OrientedContent() {
                /**
                 * Returns the width of the image (after the orientation).
                 * <p>
                 * 이미지의 너비를 반환한다(방향 적용 뒤).
                 *
                 * @return width in px / 너비(px)
                 */
                public double getNaturalWidth() {
                    return di.getWidth() / density;
                }

                /**
                 * Returns the height of the image (after the orientation).
                 * <p>
                 * 이미지의 높이를 반환한다(방향 적용 뒤).
                 *
                 * @return height in px / 높이(px)
                 */
                public double getNaturalHeight() {
                    return di.getHeight() / density;
                }

                /**
                 * Returns the content of the picture as stored in the file.
                 * <p>
                 * 파일에 저장된 그대로의 그림의 콘텐츠를 반환한다.
                 *
                 * @return the content with the stored size / 저장된 크기의 콘텐츠
                 */
                public ReplacedContent withoutOrientation() {
                    final DecodedImage stored = di.unoriented();
                    return new ReplacedContent() {
                        /**
                         * Returns the width of the stored picture.
                         * <p>
                         * 저장된 그림의 너비를 반환한다.
                         *
                         * @return width in px / 너비(px)
                         */
                        public double getNaturalWidth() {
                            return stored.getWidth() / density;
                        }

                        /**
                         * Returns the height of the stored picture.
                         * <p>
                         * 저장된 그림의 높이를 반환한다.
                         *
                         * @return height in px / 높이(px)
                         */
                        public double getNaturalHeight() {
                            return stored.getHeight() / density;
                        }
                    };
                }
            };
        }
        return new ReplacedContent() {
            /**
             * Returns the width of the image.
             * <p>
             * 이미지의 너비를 반환한다.
             *
             * @return width in px / 너비(px)
             */
            public double getNaturalWidth() {
                return di.getWidth() / density;
            }

            /**
             * Returns the height of the image.
             * <p>
             * 이미지의 높이를 반환한다.
             *
             * @return height in px / 높이(px)
             */
            public double getNaturalHeight() {
                return di.getHeight() / density;
            }
        };
    }

    /**
     * Returns the text that a broken <code>img</code> stands for (HTML5 2014 section 10.4.2): without a source the <code>alt</code> text,
     * for an image that failed to load the unsupported URL notice of the project (plan item P18-06) and the <code>alt</code> text in front
     * of it, if the element has one. A loaded or still loading image has none.
     * <p>
     * 깨진 <code>img</code> 가 대신하는 텍스트를 반환한다(HTML5 2014 §10.4.2): 소스가 없으면 <code>alt</code> 텍스트이고, 읽지 못한 이미지는 프로젝트의 미지원 URL 안내
     * (계획 P18-06)와 앞에 놓는 <code>alt</code> 텍스트(요소에 있으면)이다. 읽었거나 읽는 중인 이미지는 없다.
     *
     * @param element the <code>img</code> element / <code>img</code> 요소
     * @return the text, or null / 텍스트. 없으면 null
     */
    public String getFallbackText(Element element) {
        if (images == null || !"img".equals(element.getLocalName())) {
            return null;
        }
        String alt = element.getAttribute("alt") == null ? "" : element.getAttribute("alt");
        ImageSelection.Choice choice = ImageSelection.select(element, environment);
        if (choice == null || choice.getUrl() == null || choice.getUrl().trim().length() == 0) {
            // 소스가 없는 이미지는 alt 텍스트만 대신한다.
            return alt.length() == 0 ? null : alt;
        }
        String url = absoluteUrl(element, choice.getUrl());
        ImageResource r = ImageLookup.of(images, element, url);
        // 읽지 못한 이미지만 깨진 이미지이다(읽는 중이거나 아직 요청하지 않았으면 곧 나타날 수 있다).
        if (r == null || r.getState() != ImageResource.State.FAILED) {
            return null;
        }
        String notice = "[" + DisplayListBuilder.UNSUPPORTED_URL + url + "]";
        return alt.length() == 0 ? notice : alt + " " + notice;
    }

    /**
     * Returns the alpha mask of an image for a <code>shape-outside</code> (CSS Shapes 1 section 4). The image is fetched in the CORS mode
     * <code>anonymous</code> when the source supports it, so that a cross-origin image without permission is not available (the shape must be
     * CORS-same-origin); the first frame of an animated image is used.
     * <p>
     * <code>shape-outside</code> 를 위한 이미지의 알파 마스크를 반환한다(CSS Shapes 1 4 절). 소스가 지원하면 CORS 모드 <code>anonymous</code> 로 가져오므로 허락 없는 다른 오리진의
     * 이미지는 쓸 수 없다(모양은 CORS 같은 오리진이어야 한다). 애니메이션 이미지는 첫 프레임을 쓴다.
     *
     * @param absoluteUrl the absolute address of the image / 이미지의 절대 주소
     * @return the mask, or null if the image is not available / 마스크. 이미지를 쓸 수 없으면 null
     */
    public ShapeImage getShapeImage(String absoluteUrl) {
        if (images == null || absoluteUrl == null) {
            return null;
        }
        ImageResource r = images instanceof CorsImageSource ? ((CorsImageSource) images).getImage(absoluteUrl, "anonymous") : images.getImage(absoluteUrl);
        if (r == null || r.getState() != ImageResource.State.LOADED || r.getImage() == null || r.getImage().getFrames().isEmpty()) {
            return null;
        }
        DecodedImage d = r.getImage();
        int[] argb = d.getFrames().get(0).getArgb();
        int w = d.getWidth();
        int h = d.getHeight();
        if (w < 1 || h < 1 || argb == null || argb.length < (long) w * h) {
            return null;
        }
        byte[] alpha = new byte[w * h];
        // 픽셀마다 알파 채널(최상위 바이트)만 꺼낸다.
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = (byte) (argb[i] >>> 24);
        }
        return new ShapeImage(w, h, alpha);
    }

    /**
     * Returns the alpha mask of a gradient painted into a box (CSS Shapes 1 section 4: a gradient is an image too).
     * <p>
     * 상자에 그린 그라디언트의 알파 마스크를 반환한다(CSS Shapes 1 4 절: 그라디언트도 이미지이다).
     *
     * @param gradient     the gradient function / 그라디언트 함수
     * @param currentColor the color that <code>currentcolor</code> means / <code>currentcolor</code> 가 뜻하는 색
     * @param width        the width of the box in pixels / 상자의 너비(픽셀)
     * @param height       the height of the box in pixels / 상자의 높이(픽셀)
     * @return the mask (at most 512 by 512 samples), or null if the function is not a gradient / 마스크(최대 512x512 표본). 그라디언트가 아니면 null
     */
    public ShapeImage getShapeGradient(CssFunctionValue gradient, int currentColor, int width, int height) {
        GradientSpec spec = GradientSpec.parse(gradient, currentColor);
        // 그라디언트는 표본 해상도(축마다 최대 512)로만 그리면 모양에 충분하다.
        int w = Math.max(1, Math.min(512, width));
        int h = Math.max(1, Math.min(512, height));
        if (spec == null) {
            return null;
        }
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        try {
            g.setPaint(spec.createPaint(w, h));
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        byte[] alpha = new byte[w * h];
        // 그린 그라디언트의 알파 채널만 꺼낸다.
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = (byte) (argb[i] >>> 24);
        }
        return new ShapeImage(w, h, alpha);
    }

    /**
     * Resolves the source of an image against the base URL of its document.
     * <p>
     * 이미지의 소스를 문서의 기준 URL 로 해석한다.
     *
     * @param element the element / 요소
     * @param src     the source attribute value / 소스 속성 값
     * @return the absolute URL, or the trimmed value if it cannot be resolved / 절대 URL. 해석할 수 없으면 다듬은 값
     */
    private static String absoluteUrl(Element element, String src) {
        String url = src.trim();
        Document doc = element.getOwnerDocument();
        String base = doc == null ? null : doc.getBaseUri();
        if (base != null && base.length() > 0 && !"about:blank".equals(base) && !"about:text".equals(base)) {
            try {
                url = UrlUtil.resolve(base, url);
            } catch (IllegalArgumentException e) {
                url = src.trim();
            }
        }
        return url;
    }
}
