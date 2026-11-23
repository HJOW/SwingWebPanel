package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The background image layers of a box (plan item P10-04, CSS Backgrounds and Borders Level 3 section 3, CSS Images Level 3): for every
 * layer of <code>background-image</code> (the first layer is on top) it reads the layer values of <code>background-origin</code>,
 * <code>background-clip</code>, <code>background-size</code>, <code>background-position</code> (one to four values with the edge
 * offsets of <code>right 10px bottom 20px</code> and <code>calc()</code>) and <code>background-repeat</code> (<code>repeat</code>,
 * <code>no-repeat</code>, <code>space</code>, <code>round</code> per axis), computes the tile size, the tile position and the tile
 * spacing, and creates the display commands that paint the tiles of an image or a gradient clipped to the (possibly rounded) clip box.
 * The lists repeat if there are fewer values than layers (3.1). Layers whose image is not ready are skipped, so that they appear when the
 * image has been loaded and the page is painted again.
 * <p>
 * <code>image-set()</code> selects a candidate ({@link #pickImage}); the image functions <code>cross-fade()</code>, <code>image()</code> and <code>element()</code> of
 * CSS Images 4 are painted as {@link GeneratedImages.Picture pictures}. <code>background-attachment: fixed</code> is positioned in the viewport and
 * <code>local</code> in the scrollable area; <code>background-clip: text</code> is clipped to the glyphs by the caller.
 * <p>
 * 상자의 배경 이미지 레이어(계획 P10-04, CSS Backgrounds and Borders Level 3 3 절, CSS Images Level 3): <code>background-image</code> 의 레이어마다(첫 레이어가
 * 가장 위) <code>background-origin</code>, <code>background-clip</code>, <code>background-size</code>, <code>background-position</code>(<code>right 10px bottom 20px</code> 같은
 * 변 오프셋을 가진 1~4 개의 값과 <code>calc()</code>), <code>background-repeat</code>(축마다 <code>repeat</code>, <code>no-repeat</code>, <code>space</code>, <code>round</code>)의
 * 레이어 값을 읽고, 타일 크기, 타일 위치, 타일 간격을 계산하며, (둥글 수 있는) 클립 상자로 자른 이미지나 그라디언트의 타일을 그리는 디스플레이 명령을 만든다. 값이
 * 레이어보다 적으면 목록을 되풀이한다(3.1). 이미지가 준비되지 않은 레이어는 건너뛰므로 이미지를 읽은 뒤 다시 그리면 나타난다.
 * <p>
 * <code>image-set()</code> 은 후보 하나를 고르고({@link #pickImage}), CSS Images 4 의 이미지 함수 <code>cross-fade()</code>, <code>image()</code>, <code>element()</code> 는
 * {@link GeneratedImages.Picture 그림}으로 그린다. <code>background-attachment: fixed</code> 는 뷰포트에, <code>local</code> 은 스크롤 가능한 영역에 위치하며,
 * <code>background-clip: text</code> 는 호출자가 글리프로 자른다.
 */
final class BackgroundLayers {

    /**
     * The property ids that give the layers of the background: image, clip, origin, size, position and repeat.
     * <p>
     * 배경의 레이어를 주는 속성 id: 이미지, clip, origin, size, position, repeat.
     */
    static final int[] BACKGROUND = { Prop.BACKGROUND_IMAGE, Prop.BACKGROUND_CLIP, Prop.BACKGROUND_ORIGIN, Prop.BACKGROUND_SIZE, Prop.BACKGROUND_POSITION,
        Prop.BACKGROUND_REPEAT };

    /**
     * The property ids that give the layers of the mask (CSS Masking 1 section 6.1): image, clip, origin, size, position and repeat.
     * <p>
     * 마스크의 레이어를 주는 속성 id(CSS Masking 1 6.1 절): 이미지, clip, origin, size, position, repeat.
     */
    static final int[] MASK = { Prop.MASK_IMAGE, Prop.MASK_CLIP, Prop.MASK_ORIGIN, Prop.MASK_SIZE, Prop.MASK_POSITION, Prop.MASK_REPEAT };

    /** The image states, or null (then images that are not gradients are not painted). 이미지 상태. null 이면 그라디언트가 아닌 이미지는 그리지 않는다. */
    private final ImageSource images;
    /** The document (for the base URL), or null. 문서(기준 URL 용). 없으면 null. */
    private final Document doc;

    /**
     * Creates the layer builder.
     * <p>
     * 레이어 빌더를 만든다.
     *
     * @param images image states or null / 이미지 상태 또는 null
     * @param doc    the document or null / 문서 또는 null
     */
    BackgroundLayers(ImageSource images, Document doc) {
        this.images = images;
        this.doc = doc;
        this.generated = new GeneratedImages.Resolver(images, this);
    }

    /** The resolver of the image functions of CSS Images 4 (<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>). CSS Images 4 이미지 함수의 해석기. */
    private final GeneratedImages.Resolver generated;

    /**
     * Installs the renderer of <code>element()</code> (the display list builder).
     * <p>
     * <code>element()</code> 를 그리는 쪽(디스플레이 리스트 빌더)을 설치한다.
     *
     * @param elements the renderer or null / 그리는 쪽 또는 null
     */
    void setElementPictures(GeneratedImages.ElementPictures elements) {
        generated.setElementPictures(elements);
    }

    /**
     * Resolves a value that is one of the image functions of CSS Images 4 (<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>) into a
     * picture; any other value gives null.
     * <p>
     * CSS Images 4 의 이미지 함수(<code>cross-fade()</code>, <code>image()</code>, <code>element()</code>)인 값을 그림으로 바꾼다. 다른 값이면 null.
     *
     * @param v    the value / 값
     * @param node the node of the box / 상자의 노드
     * @param s    the style of the box / 상자의 스타일
     * @return the picture, or null for another value or an invalid image / 그림. 다른 값이거나 잘못된 이미지이면 null
     */
    GeneratedImages.Picture picture(CssValue v, Node node, ComputedStyle s) {
        return GeneratedImages.isGenerated(v) ? generated.resolve(v, node, s) : null;
    }

    /**
     * Resolves any <code>&lt;image&gt;</code> value into a picture: a URL, <code>image-set()</code>, a gradient or an image function of CSS Images 4.
     * <p>
     * 어떤 <code>&lt;image&gt;</code> 값이든 그림으로 바꾼다: URL, <code>image-set()</code>, 그라디언트, CSS Images 4 의 이미지 함수.
     *
     * @param v    the value / 값
     * @param node the node of the box / 상자의 노드
     * @param s    the style of the box or null / 상자의 스타일 또는 null
     * @return the picture, or null for an invalid or loading image / 그림. 잘못되었거나 불러오는 중인 이미지이면 null
     */
    GeneratedImages.Picture anyPicture(CssValue v, Node node, ComputedStyle s) {
        return generated.resolve(v, node, s);
    }

    /**
     * Splits a comma list value into its layers.
     * <p>
     * 쉼표 목록 값을 레이어로 나눈다.
     *
     * @param v the value / 값
     * @return the layers (one for a single value) / 레이어(단일 값이면 하나)
     */
    static List<CssValue> layers(CssValue v) {
        List<CssValue> l = new ArrayList<CssValue>();
        if (v instanceof CssList && ((CssList) v).isComma()) {
            l.addAll(((CssList) v).getItems());
        } else if (v != null) {
            l.add(v);
        }
        return l;
    }

    /**
     * Returns the value of a layer property for a layer (the list repeats).
     * <p>
     * 레이어의 레이어 속성 값을 반환한다(목록은 되풀이한다).
     *
     * @param s  the style / 스타일
     * @param id the property / 속성
     * @param i  the layer index / 레이어 번호
     * @return the value or null / 값 또는 null
     */
    static CssValue item(ComputedStyle s, int id, int i) {
        List<CssValue> l = layers(s.get(id));
        return l.isEmpty() ? null : l.get(i % l.size());
    }

    /**
     * Tells whether the style has a background layer that can be painted (an image URL or a gradient).
     * <p>
     * 스타일에 그릴 수 있는 배경 레이어(이미지 URL 이나 그라디언트)가 있는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true if there is such a layer / 그런 레이어가 있으면 true
     */
    static boolean hasLayers(ComputedStyle s) {
        List<CssValue> l = layers(s.get(Prop.BACKGROUND_IMAGE));
        // 레이어마다 URL 이나 그라디언트 함수인지 본다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) instanceof CssUrl || l.get(i) instanceof CssFunctionValue) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a length, a percentage or a calc expression against a base length.
     * <p>
     * 길이, 백분율, calc 식을 기준 길이에 대해 해석한다.
     *
     * @param v    the value / 값
     * @param base the length that 100% stands for / 100% 가 뜻하는 길이
     * @return pixels, NaN for <code>auto</code> and other values / 픽셀. <code>auto</code> 와 그 밖의 값이면 NaN
     */
    static double lengthOf(CssValue v, double base) {
        if (v instanceof CssLength) {
            return ((CssLength) v).getValue();
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() * base / 100.0;
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), base);
        }
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssNumber && ((org.duckdns.hjow.swingwebpanel.css.values.CssNumber) v).getValue() == 0) {
            return 0;
        }
        return Double.NaN;
    }

    /**
     * Resolves the tokens of a position (<code>background-position</code>, <code>at</code> of a gradient) against the free space: one
     * to four tokens with the keywords <code>left</code>, <code>right</code>, <code>top</code>, <code>bottom</code>,
     * <code>center</code>, lengths and percentages (CSS Backgrounds and Borders 3.6). A percentage is a fraction of the free space, a
     * length after <code>right</code> or <code>bottom</code> is the distance from that edge.
     * <p>
     * 위치의 토큰(<code>background-position</code>, 그라디언트의 <code>at</code>)을 여유 공간에 대해 해석한다: 키워드 <code>left</code>, <code>right</code>,
     * <code>top</code>, <code>bottom</code>, <code>center</code>, 길이, 백분율의 1~4 개 토큰(CSS Backgrounds and Borders 3.6). 백분율은 여유 공간의 비율이고
     * <code>right</code> 나 <code>bottom</code> 뒤의 길이는 그 가장자리에서의 거리이다.
     *
     * @param t     the tokens / 토큰
     * @param freeW free horizontal space / 가로 여유
     * @param freeH free vertical space / 세로 여유
     * @return {x, y} offsets / {x, y} 오프셋
     */
    static double[] resolvePosition(List<CssValue> t, double freeW, double freeH) {
        CssValue[] xs = null;
        CssValue[] ys = null;
        int n = t.size();
        if (n == 1) {
            CssValue a = t.get(0);
            if (a.isKeyword("top") || a.isKeyword("bottom")) {
                xs = new CssValue[] { CssKeyword.of("center"), null };
                ys = new CssValue[] { a, null };
            } else {
                xs = new CssValue[] { a, null };
                ys = new CssValue[] { CssKeyword.of("center"), null };
            }
        } else if (n == 2) {
            CssValue a = t.get(0);
            CssValue b = t.get(1);
            // 앞이 top/bottom 이거나 뒤가 left/right 이면 세로가 앞에 온 것이다.
            if (a.isKeyword("top") || a.isKeyword("bottom") || b.isKeyword("left") || b.isKeyword("right")) {
                xs = new CssValue[] { b, null };
                ys = new CssValue[] { a, null };
            } else {
                xs = new CssValue[] { a, null };
                ys = new CssValue[] { b, null };
            }
        } else if (n >= 3) {
            // 3~4 개: 키워드 하나 뒤에 오프셋이 올 수 있다. 키워드로 가로와 세로를 나눈다.
            int i = 0;
            while (i < n) {
                CssValue k = t.get(i);
                CssValue off = i + 1 < n && !(t.get(i + 1) instanceof CssKeyword) ? t.get(i + 1) : null;
                boolean horizontal = k.isKeyword("left") || k.isKeyword("right");
                boolean vertical = k.isKeyword("top") || k.isKeyword("bottom");
                if (horizontal || (k.isKeyword("center") && xs == null && !vertical)) {
                    xs = new CssValue[] { k, off };
                } else {
                    ys = new CssValue[] { k, off };
                }
                i += off == null ? 1 : 2;
            }
            if (xs == null) {
                xs = new CssValue[] { CssKeyword.of("center"), null };
            }
            if (ys == null) {
                ys = new CssValue[] { CssKeyword.of("center"), null };
            }
        }
        if (xs == null) {
            return new double[] { 0, 0 };
        }
        return new double[] { axis(xs, freeW, "left", "right"), axis(ys, freeH, "top", "bottom") };
    }

    /**
     * Resolves one axis of a position.
     * <p>
     * 위치의 한 축을 해석한다.
     *
     * @param t     {token, offset or null} / {토큰, 오프셋 또는 null}
     * @param free  the free space / 여유 공간
     * @param start the keyword of the start edge / 시작 가장자리 키워드
     * @param end   the keyword of the end edge / 끝 가장자리 키워드
     * @return the offset / 오프셋
     */
    private static double axis(CssValue[] t, double free, String start, String end) {
        CssValue k = t[0];
        double off = t[1] == null ? 0 : lengthOf(t[1], free);
        if (Double.isNaN(off)) {
            off = 0;
        }
        if (k.isKeyword(start)) {
            return off;
        }
        if (k.isKeyword(end)) {
            return free - off;
        }
        if (k.isKeyword("center")) {
            return free / 2;
        }
        double v = lengthOf(k, free);
        return Double.isNaN(v) ? 0 : v;
    }

    /**
     * Resolves <code>image-set()</code> to the image that it selects (CSS Images Level 4, <a href="https://www.w3.org/TR/css-images-4/#image-set-notation">image-set notation</a>):
     * every candidate is an image (a <code>url()</code> or a string) with a resolution (<code>x</code>, <code>dppx</code>, <code>dpi</code>, <code>dpcm</code>; 1x if it has none), and the
     * user agent takes the candidate of the smallest resolution that is not below the device pixel ratio of this engine (1), or the largest one if all are below it. A <code>type()</code>
     * hint is ignored (the decoders recognize the formats by their content). Any other value is returned as it is.
     * <p>
     * <code>image-set()</code> 을 그것이 고르는 이미지로 푼다(CSS Images Level 4 image-set 표기): 후보마다 이미지(<code>url()</code> 이나 문자열)와 해상도(<code>x</code>, <code>dppx</code>,
     * <code>dpi</code>, <code>dpcm</code>. 없으면 1x)가 있고, 사용자 에이전트는 이 엔진의 장치 화소 비율(1)보다 작지 않은 해상도 중 가장 작은 후보를 고르며 모두 그보다 작으면 가장 큰
     * 후보를 고른다. <code>type()</code> 힌트는 무시한다(디코더가 내용으로 형식을 알아본다). 다른 값은 그대로 반환한다.
     *
     * @param img the image value / 이미지 값
     * @return the selected <code>url()</code>, or the value itself / 고른 <code>url()</code>. 아니면 값 자신
     */
    CssValue pickImage(CssValue img) {
        return pickImage(img, null);
    }

    /**
     * Resolves <code>image-set()</code> like {@link #pickImage(CssValue)} and tells the resolution of the selected candidate: an image of a resolution of 2x has half the
     * natural size in CSS pixels.
     * <p>
     * {@link #pickImage(CssValue)} 처럼 <code>image-set()</code> 을 풀고 고른 후보의 해상도를 알려 준다: 해상도 2x 의 이미지는 CSS 픽셀로 고유 크기가 절반이다.
     *
     * @param img        the image value / 이미지 값
     * @param resolution receives the resolution of the selected candidate (1 if the value is not an <code>image-set()</code>) at index 0, may be null / 고른 후보의 해상도를 인덱스 0 에 받는다(<code>image-set()</code> 이 아니면 1). null 가능
     * @return the selected <code>url()</code>, or the value itself / 고른 <code>url()</code>. 아니면 값 자신
     */
    CssValue pickImage(CssValue img, double[] resolution) {
        if (resolution != null) {
            resolution[0] = 1;
        }
        if (!(img instanceof CssFunctionValue) || !(((CssFunctionValue) img).isNamed("image-set") || ((CssFunctionValue) img).isNamed("-webkit-image-set"))) {
            return img;
        }
        List<CssValue> args = ((CssFunctionValue) img).getArguments();
        CssUrl best = null;
        double bestRes = 0;
        // 인수는 쉼표로 나뉜 후보이고, 후보는 값 하나이거나 공백으로 이은 값의 목록이다.
        for (int i = 0; i < args.size(); i++) {
            List<CssValue> cur = args.get(i) instanceof CssList ? ((CssList) args.get(i)).getItems() : java.util.Collections.singletonList(args.get(i));
            CssUrl url = null;
            double res = 1;
            // 후보 안에서 이미지(url 이나 문자열)와 해상도를 찾는다.
            for (int k = 0; k < cur.size(); k++) {
                CssValue v = cur.get(k);
                if (v instanceof CssUrl && url == null) {
                    url = (CssUrl) v;
                } else if (v instanceof CssString && url == null) {
                    String ref = ((CssString) v).getValue();
                    url = new CssUrl(ref, resolve(ref));
                } else if (v instanceof CssDimension) {
                    CssDimension d = (CssDimension) v;
                    String unit = d.getUnit().toLowerCase(java.util.Locale.ENGLISH);
                    if ("x".equals(unit) || "dppx".equals(unit)) {
                        res = d.getValue();
                    } else if ("dpi".equals(unit)) {
                        res = d.getValue() / 96.0;
                    } else if ("dpcm".equals(unit)) {
                        res = d.getValue() * 2.54 / 96.0;
                    }
                }
            }
            if (url == null || res <= 0) {
                continue;
            }
            // 1 이상인 해상도 중 가장 작은 것을 고르고, 그런 것이 없으면 가장 큰 것을 고른다.
            boolean better = best == null || (bestRes < 1 && res > bestRes) || (res >= 1 && (bestRes < 1 || res < bestRes));
            if (better) {
                best = url;
                bestRes = res;
            }
        }
        if (best != null && resolution != null) {
            resolution[0] = bestRes;
        }
        return best != null ? best : img;
    }

    /**
     * Returns the id that an image value refers to inside the document, for <code>url(#id)</code>: such a value is a reference to an element (an SVG
     * <code>mask</code> element for <code>mask-image</code>), not an address of an image to fetch.
     * <p>
     * 이미지 값이 <code>url(#id)</code> 일 때 문서 안에서 가리키는 id 를 반환한다. 그런 값은 가져올 이미지의 주소가 아니라 요소(<code>mask-image</code> 에서는 SVG
     * <code>mask</code> 요소)의 참조이다.
     *
     * @param v the image value / 이미지 값
     * @return the id without the <code>#</code>, or null if the value is not a reference inside the document / <code>#</code> 를 뺀 id. 문서 안의 참조가 아니면 null
     */
    static String fragmentReference(CssValue v) {
        if (!(v instanceof CssUrl)) {
            return null;
        }
        String u = ((CssUrl) v).getUrl();
        return u != null && u.startsWith("#") && u.length() > 1 ? u.substring(1) : null;
    }

    /**
     * Returns the decoded image that an image value names, or null if the value is not a URL or the image has not arrived (<code>mask-border-source</code>).
     * <p>
     * 이미지 값이 가리키는 디코딩된 이미지를 반환한다. 값이 URL 이 아니거나 이미지가 아직 도착하지 않았으면 null(<code>mask-border-source</code>).
     *
     * @param img the value / 값
     * @return the image or null / 이미지. 없으면 null
     */
    DecodedImage decodedImage(CssValue source) {
        if (fragmentReference(source) != null) {
            return null;
        }
        CssValue img = pickImage(source);
        if (!(img instanceof CssUrl)) {
            return null;
        }
        CssUrl u = (CssUrl) img;
        String url = u.getResolvedUrl() != null && u.getResolvedUrl().length() > 0 ? u.getResolvedUrl() : resolve(u.getUrl());
        ImageResource r = images == null ? null : images.getImage(url);
        return r == null || r.getState() != ImageResource.State.LOADED ? null : r.getImage();
    }

    /**
     * Returns the gradient that an image value is, or null for any other value (<code>mask-border-source</code>).
     * <p>
     * 이미지 값이 그라디언트이면 그것을, 다른 값이면 null 을 반환한다(<code>mask-border-source</code>).
     *
     * @param img          the value / 값
     * @param currentColor the color for <code>currentcolor</code> / <code>currentcolor</code> 용 색
     * @return the gradient or null / 그라디언트. 없으면 null
     */
    GradientSpec gradient(CssValue img, int currentColor) {
        return img instanceof CssFunctionValue ? GradientSpec.parse((CssFunctionValue) img, currentColor) : null;
    }

    /**
     * Returns the absolute address of a URL value: the address resolved when the value was parsed, else the reference resolved against the base URL of the
     * document.
     * <p>
     * URL 값의 절대 주소를 반환한다: 값을 파싱할 때 해석한 주소, 없으면 문서의 기준 URL 로 해석한 참조.
     *
     * @param u the URL value / URL 값
     * @return the address / 주소
     */
    String absolute(CssUrl u) {
        return u.getResolvedUrl() != null && u.getResolvedUrl().length() > 0 ? u.getResolvedUrl() : resolve(u.getUrl());
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
     * Creates the commands that paint the background layers of a box, the bottom layer first.
     * <p>
     * 상자의 배경 레이어를 그리는 명령을 아래 레이어부터 만든다.
     *
     * @param out     receives the commands / 명령을 받는다
     * @param s       the style / 스타일
     * @param border  the rounded border edge of the box (the border box; for the canvas the canvas rectangle) / 상자의 둥근 border 가장자리(border box. 캔버스이면 캔버스 사각형)
     * @param borders the border widths {top, right, bottom, left} / border 너비 {위, 오른쪽, 아래, 왼쪽}
     * @param padding the paddings {top, right, bottom, left} / padding {위, 오른쪽, 아래, 왼쪽}
     * @param canvas  true for the background of the canvas (its box is the canvas, without borders) / 캔버스의 배경이면 true(상자는 캔버스이고 border 없음)
     * @param node    the DOM node / DOM 노드
     */
    void build(List<DisplayCommand> out, ComputedStyle s, RoundedBox border, double[] borders, double[] padding, boolean canvas, Node node) {
        build(out, s, border, borders, padding, canvas, node, BACKGROUND, null);
    }

    /**
     * Creates the commands that paint the layers of a layer property set (the background or the mask), the bottom layer first.
     * <p>
     * 레이어 속성 묶음(배경이나 마스크)의 레이어를 그리는 명령을 아래 레이어부터 만든다.
     *
     * @param out        receives the commands / 명령을 받는다
     * @param s          the style / 스타일
     * @param border     the rounded border edge of the box / 상자의 둥근 border 가장자리
     * @param borders    the border widths {top, right, bottom, left} / border 너비 {위, 오른쪽, 아래, 왼쪽}
     * @param padding    the paddings {top, right, bottom, left} / padding {위, 오른쪽, 아래, 왼쪽}
     * @param canvas     true for the background of the canvas / 캔버스의 배경이면 true
     * @param node       the DOM node / DOM 노드
     * @param props      the property ids of the layer set, {@link #BACKGROUND} or {@link #MASK} / 레이어 묶음의 속성 id. {@link #BACKGROUND} 나 {@link #MASK}
     * @param layerIndex receives the layer number of every command that was added to <code>out</code>, or null / <code>out</code> 에 더한 명령마다 레이어 번호를 받는다. 필요 없으면 null
     */
    void build(List<DisplayCommand> out, ComputedStyle s, RoundedBox border, double[] borders, double[] padding, boolean canvas, Node node, int[] props,
            List<Integer> layerIndex) {
        build(out, s, border, borders, padding, canvas, node, props, layerIndex, null);
    }

    /**
     * Creates the commands that paint the layers of a layer property set and knows the extent of the scrollable content of the box: a layer with
     * <code>background-attachment: local</code> is positioned in the whole scrollable area (the padding box grown to the content that overflows it), a layer with
     * <code>fixed</code> in the viewport at paint time, and a layer with <code>background-clip: text</code> is painted over the border box (the caller restricts it to the glyphs).
     * <p>
     * 레이어 속성 묶음의 레이어를 그리는 명령을 만들며 상자의 스크롤 가능한 콘텐츠 범위를 안다: <code>background-attachment: local</code> 인 레이어는 스크롤 가능한 전체
     * 영역(넘치는 콘텐츠까지 늘린 padding box)에, <code>fixed</code> 인 레이어는 그릴 때의 뷰포트에 위치하며, <code>background-clip: text</code> 인 레이어는 border box 에
     * 그린다(글리프로 제한하는 것은 호출자의 일이다).
     *
     * @param out          receives the commands / 명령을 받는다
     * @param s            the style / 스타일
     * @param border       the rounded border edge of the box / 상자의 둥근 border 가장자리
     * @param borders      the border widths {top, right, bottom, left} / border 너비 {위, 오른쪽, 아래, 왼쪽}
     * @param padding      the paddings {top, right, bottom, left} / padding {위, 오른쪽, 아래, 왼쪽}
     * @param canvas       true for the background of the canvas / 캔버스의 배경이면 true
     * @param node         the DOM node / DOM 노드
     * @param props        the property ids of the layer set / 레이어 묶음의 속성 id
     * @param layerIndex   receives the layer number of every command that was added to <code>out</code>, or null / <code>out</code> 에 더한 명령마다 레이어 번호를 받는다. 필요 없으면 null
     * @param scrollExtent {right, bottom} of the content of the box in document coordinates (for <code>local</code>), or null if the box does not scroll / 상자 콘텐츠의 문서 좌표 {오른쪽, 아래쪽}(<code>local</code> 용). 스크롤하지 않는 상자이면 null
     */
    void build(List<DisplayCommand> out, ComputedStyle s, RoundedBox border, double[] borders, double[] padding, boolean canvas, Node node, int[] props,
            List<Integer> layerIndex, double[] scrollExtent) {
        List<CssValue> imgs = layers(s.get(props[0]));
        int currentColor = s.color(Prop.COLOR);
        // 아래 레이어부터 위 레이어 순서로 그린다.
        for (int i = imgs.size() - 1; i >= 0; i--) {
            double[] imgRes = {1};
            CssValue img = pickImage(imgs.get(i), imgRes);
            DecodedImage di = null;
            GradientSpec grad = null;
            GeneratedImages.Picture pic = null;
            if (GeneratedImages.isGenerated(img)) {
                // cross-fade(), image(), element() 는 그림으로 해석한다. 잘못되었거나 불러오는 중인 이미지는 그리지 않는다.
                pic = generated.resolve(img, node, s);
                if (pic == null) {
                    continue;
                }
            } else if (img instanceof CssUrl) {
                CssUrl u = (CssUrl) img;
                // 마스크 이미지의 url(#id) 는 SVG mask 요소의 참조이므로 이미지로 가져오지 않는다(MaskLayers 가 요소로 처리한다).
                if (props == MASK && fragmentReference(u) != null) {
                    continue;
                }
                String url = u.getResolvedUrl() != null && u.getResolvedUrl().length() > 0 ? u.getResolvedUrl() : resolve(u.getUrl());
                ImageResource r = images == null ? null : images.getImage(url);
                if (r == null || r.getState() != ImageResource.State.LOADED || r.getImage() == null) {
                    continue;
                }
                // image-orientation: none 이면 EXIF 등의 방향을 적용하지 않은 저장된 그대로의 이미지를 쓴다(CSS Images 3 5.1절).
                di = s.isKeyword(Prop.IMAGE_ORIENTATION, "none") ? r.getImage().unoriented() : r.getImage();
            } else if (img instanceof CssFunctionValue) {
                grad = GradientSpec.parse((CssFunctionValue) img, currentColor);
                if (grad == null) {
                    continue;
                }
            } else {
                continue;
            }
            String clipKind = kw(item(s, props[1], i), "border-box");
            if ("text".equals(clipKind)) {
                // 글자로 자르는 배경은 border box 에 그리고 호출자가 글리프로 제한한다(캔버스의 배경과 마스크에는 text 가 없다).
                if (canvas || props == MASK) {
                    continue;
                }
                clipKind = "border-box";
            }
            RoundedBox clipBox = "no-clip".equals(clipKind) ? new RoundedBox(-1e6, -1e6, 2e6, 2e6, new double[8]) : edge(border, borders, padding, clipKind, canvas);
            RoundedBox origin = edge(border, borders, padding, kw(item(s, props[2], i), props == MASK ? "border-box" : "padding-box"), canvas);
            String attachment = props == BACKGROUND ? kw(item(s, Prop.BACKGROUND_ATTACHMENT, i), "scroll") : "scroll";
            // local 은 콘텐츠와 함께 스크롤하는 배경이므로 위치 기준이 넘치는 콘텐츠까지 늘린 padding box 이다(스크롤하지 않는 상자는 scroll 과 같다).
            if ("local".equals(attachment) && scrollExtent != null) {
                RoundedBox pad = edge(border, borders, padding, "padding-box", canvas);
                origin = new RoundedBox(pad.x, pad.y, Math.max(pad.w, scrollExtent[0] - pad.x), Math.max(pad.h, scrollExtent[1] - pad.y), new double[8]);
            }
            if (origin.w <= 0 || origin.h <= 0 || clipBox.w <= 0 || clipBox.h <= 0) {
                continue;
            }
            CssValue sizeValue = item(s, props[3], i);
            CssValue positionValue = item(s, props[4], i);
            CssValue repeatValue = item(s, props[5], i);
            Shape clipShape = clipBox.toShape();
            // fixed 는 뷰포트에 위치하며 뷰포트는 그릴 때에야 알므로 기하를 그릴 때 계산하는 명령을 만든다(뷰포트를 모르면 위치 기준 상자를 쓴다).
            if ("fixed".equals(attachment)) {
                out.add(new BoxCommands.FixedLayer(clipShape, di, grad, sizeValue, positionValue, repeatValue, imgRes[0], new double[] { origin.x, origin.y, origin.w, origin.h }, node)
                        .withPicture(pic));
                if (layerIndex != null) {
                    layerIndex.add(Integer.valueOf(i));
                }
                continue;
            }
            double[] g = pic != null ? tiles(sizeValue, pic.naturalWidth(), pic.naturalHeight(), repeatValue, positionValue, origin.x, origin.y, origin.w, origin.h)
                    : tiles(sizeValue, di, imgRes[0], repeatValue, positionValue, origin.x, origin.y, origin.w, origin.h);
            if (g == null) {
                continue;
            }
            out.add(new BoxCommands.Layer(clipShape, di, grad, g[0], g[1], g[2], g[3], g[4], g[5], g[6] != 0, g[7] != 0, node).withPicture(pic));
            if (layerIndex != null) {
                layerIndex.add(Integer.valueOf(i));
            }
        }
    }

    /**
     * Computes the tiles of one layer in a positioning area: the size of a tile (<code>background-size</code>, <code>round</code> adjusts it to a whole number of tiles), the
     * position of the first tile (<code>background-position</code>), the distance between the tiles (<code>space</code> spreads the free space between them) and whether the layer
     * repeats on each axis (CSS Backgrounds and Borders 3.4 to 3.6).
     * <p>
     * 위치 기준 영역 안에서 레이어 하나의 타일을 계산한다: 타일 크기(<code>background-size</code>, <code>round</code> 는 타일 수를 정수로 맞춰 조정), 첫 타일의 위치
     * (<code>background-position</code>), 타일 사이 거리(<code>space</code> 는 남는 공간을 타일 사이에 나눔), 축마다 반복 여부(CSS Backgrounds and Borders 3.4~3.6).
     *
     * @param sizeValue     the <code>background-size</code> value of the layer / 레이어의 <code>background-size</code> 값
     * @param di            the image or null for a gradient / 이미지. 그라디언트이면 null
     * @param resolution    the resolution of the image / 이미지의 해상도
     * @param repeatValue   the <code>background-repeat</code> value / <code>background-repeat</code> 값
     * @param positionValue the <code>background-position</code> value / <code>background-position</code> 값
     * @param ox            left of the positioning area / 위치 기준 영역의 왼쪽
     * @param oy            top of the positioning area / 위쪽
     * @param ow            width of the positioning area / 너비
     * @param oh            height of the positioning area / 높이
     * @return {startX, startY, tileW, tileH, stepX, stepY, repeatX (1 or 0), repeatY}, or null if nothing is painted / {startX, startY, 타일 너비, 타일 높이, stepX, stepY, 가로 반복(1 또는 0), 세로 반복}. 그릴 것이 없으면 null
     */
    static double[] tiles(CssValue sizeValue, DecodedImage di, double resolution, CssValue repeatValue, CssValue positionValue, double ox, double oy, double ow,
            double oh) {
        double nw = di == null ? Double.NaN : di.getWidth() / resolution;
        double nh = di == null ? Double.NaN : di.getHeight() / resolution;
        return tiles(sizeValue, nw, nh, repeatValue, positionValue, ox, oy, ow, oh);
    }

    /**
     * Computes the tiles of one layer like {@link #tiles(CssValue, DecodedImage, double, CssValue, CssValue, double, double, double, double)} for an image of
     * known natural dimensions (NaN for an image that has none, like a gradient or a solid-color image).
     * <p>
     * 고유 크기를 아는 이미지(그라디언트나 단색 이미지처럼 없으면 NaN)에 대해 레이어 하나의 타일을
     * {@link #tiles(CssValue, DecodedImage, double, CssValue, CssValue, double, double, double, double)} 처럼 계산한다.
     *
     * @param sizeValue     the <code>background-size</code> value of the layer / 레이어의 <code>background-size</code> 값
     * @param naturalW      the natural width in CSS pixels or NaN / 고유 너비(CSS 픽셀) 또는 NaN
     * @param naturalH      the natural height in CSS pixels or NaN / 고유 높이 또는 NaN
     * @param repeatValue   the <code>background-repeat</code> value / <code>background-repeat</code> 값
     * @param positionValue the <code>background-position</code> value / <code>background-position</code> 값
     * @param ox            left of the positioning area / 위치 기준 영역의 왼쪽
     * @param oy            top of the positioning area / 위쪽
     * @param ow            width of the positioning area / 너비
     * @param oh            height of the positioning area / 높이
     * @return the tiles as for the other method, or null / 다른 메서드와 같은 타일. 없으면 null
     */
    static double[] tiles(CssValue sizeValue, double naturalW, double naturalH, CssValue repeatValue, CssValue positionValue, double ox, double oy, double ow,
            double oh) {
        if (ow <= 0 || oh <= 0) {
            return null;
        }
        double[] size = tileSize(sizeValue, naturalW, naturalH, ow, oh);
        String[] rep = repeat(repeatValue);
        double tw = size[0];
        double th = size[1];
        // round 는 타일 수를 정수로 맞춰 크기를 조정한다(3.5).
        if ("round".equals(rep[0]) && tw > 0) {
            tw = ow / Math.max(1, Math.round(ow / tw));
        }
        if ("round".equals(rep[1]) && th > 0) {
            th = oh / Math.max(1, Math.round(oh / th));
        }
        if (tw < 0.5 || th < 0.5) {
            return null;
        }
        List<CssValue> pos = positionTokens(positionValue);
        double[] p = resolvePosition(pos, ow - tw, oh - th);
        double stepX = tw;
        double stepY = th;
        double startX = ox + p[0];
        double startY = oy + p[1];
        // space: 타일을 정수 개 놓고 남는 공간을 타일 사이에 나눈다(3.5). 한 개도 못 놓으면 한 번만 놓는다.
        if ("space".equals(rep[0])) {
            int cnt = (int) Math.floor(ow / tw);
            if (cnt >= 2) {
                stepX = tw + (ow - cnt * tw) / (cnt - 1);
                startX = ox;
            } else {
                rep[0] = "no-repeat";
            }
        }
        if ("space".equals(rep[1])) {
            int cnt = (int) Math.floor(oh / th);
            if (cnt >= 2) {
                stepY = th + (oh - cnt * th) / (cnt - 1);
                startY = oy;
            } else {
                rep[1] = "no-repeat";
            }
        }
        boolean repX = !"no-repeat".equals(rep[0]);
        boolean repY = !"no-repeat".equals(rep[1]);
        return new double[] { startX, startY, tw, th, stepX, stepY, repX ? 1 : 0, repY ? 1 : 0 };
    }

    /**
     * Returns the keyword of the <code>background-clip</code> of a layer (<code>border-box</code> if there is none).
     * <p>
     * 레이어의 <code>background-clip</code> 키워드를 반환한다(없으면 <code>border-box</code>).
     *
     * @param s the style / 스타일
     * @param i the layer number / 레이어 번호
     * @return the keyword / 키워드
     */
    static String clipOf(ComputedStyle s, int i) {
        return kw(item(s, Prop.BACKGROUND_CLIP, i), "border-box");
    }

    /**
     * Tells whether a layer is positioned with <code>background-attachment: local</code> (a scrolling box moves its background with its content).
     * <p>
     * 레이어가 <code>background-attachment: local</code> 로 위치하는지 알려 준다(스크롤하는 상자는 배경을 내용과 함께 움직인다).
     *
     * @param s the style / 스타일
     * @return true if some layer is local / 어느 레이어든 local 이면 true
     */
    static boolean usesLocal(ComputedStyle s) {
        List<CssValue> l = layers(s.get(Prop.BACKGROUND_ATTACHMENT));
        // 레이어마다 local 인지 본다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) instanceof CssKeyword && ((CssKeyword) l.get(i)).isKeyword("local")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the keyword of a value, or a default.
     * <p>
     * 값의 키워드를 반환하거나 기본값을 반환한다.
     *
     * @param v   the value / 값
     * @param def the default / 기본값
     * @return the keyword / 키워드
     */
    private static String kw(CssValue v, String def) {
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : def;
    }

    /**
     * Returns the rectangle of a box edge (border, padding or content) of the box; the canvas has a single edge.
     * <p>
     * 상자의 한 가장자리(border, padding, content)의 사각형을 반환한다. 캔버스는 가장자리가 하나뿐이다.
     *
     * @param border  the border edge / border 가장자리
     * @param borders the border widths / border 너비
     * @param padding the paddings / padding
     * @param kind    <code>border-box</code>, <code>padding-box</code> or <code>content-box</code> / 가장자리 종류
     * @param canvas  true for the canvas / 캔버스이면 true
     * @return the rounded rectangle of the edge / 가장자리의 둥근 사각형
     */
    private static RoundedBox edge(RoundedBox border, double[] borders, double[] padding, String kind, boolean canvas) {
        // 마스크의 기하 상자 이름: fill-box 는 content-box, stroke-box 와 view-box 와 margin-box 는 border-box 로 본다(CSS Masking 1 6.2 절; margin-box 는 근사).
        if ("fill-box".equals(kind)) {
            kind = "content-box";
        } else if ("margin-box".equals(kind) || "stroke-box".equals(kind) || "view-box".equals(kind)) {
            kind = "border-box";
        }
        if (canvas || "border-box".equals(kind)) {
            return border;
        }
        RoundedBox pb = border.inset(borders[0], borders[1], borders[2], borders[3]);
        if ("content-box".equals(kind)) {
            return pb.inset(padding[0], padding[1], padding[2], padding[3]);
        }
        return pb;
    }

    /**
     * Returns the tokens of a position value.
     * <p>
     * 위치 값의 토큰을 반환한다.
     *
     * @param v the value / 값
     * @return the tokens / 토큰
     */
    private static List<CssValue> positionTokens(CssValue v) {
        List<CssValue> t = new ArrayList<CssValue>();
        if (v instanceof CssList && !((CssList) v).isComma()) {
            // 공백 목록은 토큰으로 풀어 낸다.
            for (int i = 0; i < ((CssList) v).size(); i++) {
                t.add(((CssList) v).get(i));
            }
        } else if (v != null) {
            t.add(v);
        }
        if (t.isEmpty()) {
            t.add(CssKeyword.of("left"));
            t.add(CssKeyword.of("top"));
        }
        return t;
    }

    /**
     * Returns the repeat style of a layer for both axes.
     * <p>
     * 레이어의 반복 방식을 두 축에 대해 반환한다.
     *
     * @param v the value / 값
     * @return {horizontal, vertical}, each of repeat, no-repeat, space, round / {가로, 세로}. 각각 repeat, no-repeat, space, round
     */
    private static String[] repeat(CssValue v) {
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            if ("repeat-x".equals(k)) {
                return new String[] { "repeat", "no-repeat" };
            }
            if ("repeat-y".equals(k)) {
                return new String[] { "no-repeat", "repeat" };
            }
            return new String[] { k, k };
        }
        if (v instanceof CssList && ((CssList) v).size() >= 2) {
            return new String[] { kw(((CssList) v).get(0), "repeat"), kw(((CssList) v).get(1), "repeat") };
        }
        return new String[] { "repeat", "repeat" };
    }

    /**
     * Computes the size of one tile (<code>background-size</code> with the default sizing of CSS Images 5.3): <code>cover</code>
     * and <code>contain</code> scale an image with a natural ratio, an image without a natural size (a gradient) is as large as the
     * positioning area, <code>auto</code> keeps the ratio of an image.
     * <p>
     * 타일 하나의 크기를 계산한다(CSS Images 5.3 의 기본 크기 규칙과 <code>background-size</code>): <code>cover</code>, <code>contain</code> 은 고유 비율의 이미지를 확대/축소하고,
     * 고유 크기가 없는 이미지(그라디언트)는 위치 기준 영역만큼 크며, <code>auto</code> 는 이미지의 비율을 유지한다.
     *
     * @param v     the size value of the layer / 레이어의 크기 값
     * @param iw    the natural width of the image in CSS pixels, NaN if it has none (a gradient) / 이미지의 고유 너비(CSS 픽셀). 없으면(그라디언트) NaN
     * @param ih    the natural height, NaN if it has none / 고유 높이. 없으면 NaN
     * @param areaW width of the positioning area / 위치 기준 영역의 너비
     * @param areaH height of the positioning area / 높이
     * @return {width, height} / {너비, 높이}
     */
    private static double[] tileSize(CssValue v, double iw, double ih, double areaW, double areaH) {
        boolean none = Double.isNaN(iw) || Double.isNaN(ih);
        if (v instanceof CssKeyword && (((CssKeyword) v).isKeyword("cover") || ((CssKeyword) v).isKeyword("contain"))) {
            if (none || iw <= 0 || ih <= 0) {
                return new double[] { areaW, areaH };
            }
            double sx = areaW / iw;
            double sy = areaH / ih;
            double sc = ((CssKeyword) v).isKeyword("cover") ? Math.max(sx, sy) : Math.min(sx, sy);
            return new double[] { iw * sc, ih * sc };
        }
        double w = Double.NaN;
        double h = Double.NaN;
        if (v instanceof CssList && !((CssList) v).isComma() && ((CssList) v).size() >= 1) {
            w = lengthOf(((CssList) v).get(0), areaW);
            h = ((CssList) v).size() > 1 ? lengthOf(((CssList) v).get(1), areaH) : Double.NaN;
        } else if (v != null) {
            w = lengthOf(v, areaW);
        }
        if (none) {
            return new double[] { Double.isNaN(w) ? areaW : w, Double.isNaN(h) ? areaH : h };
        }
        if (Double.isNaN(w) && Double.isNaN(h)) {
            return new double[] { iw, ih };
        }
        if (Double.isNaN(w)) {
            w = ih > 0 ? iw * h / ih : 0;
        } else if (Double.isNaN(h)) {
            h = iw > 0 ? ih * w / iw : 0;
        }
        return new double[] { w, h };
    }
}
