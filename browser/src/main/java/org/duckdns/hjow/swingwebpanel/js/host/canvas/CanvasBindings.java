package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.MatrixBindings;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.render.CanvasSizes;
import org.duckdns.hjow.swingwebpanel.render.CanvasSource;

/**
 * The canvas of one page (plan items P20-05, P00-08): it installs the script interface of <code>HTMLCanvasElement</code>
 * (<code>width</code>, <code>height</code>, <code>getContext</code>, <code>toDataURL</code>, <code>toBlob</code>), <code>CanvasRenderingContext2D</code>,
 * <code>CanvasGradient</code>, <code>CanvasPattern</code>, <code>TextMetrics</code> and <code>ImageData</code>, owns the {@link CanvasSurface} of every
 * canvas element that a script touched, and answers the renderer as a {@link CanvasSource}. <code>getContext("2d")</code> returns the one
 * context of the canvas; every other context type (<code>webgl</code>, <code>webgl2</code>, <code>experimental-webgl</code>, <code>webgpu</code>,
 * <code>bitmaprenderer</code>, unknown names) returns null, no WebGL or WebGPU interface object is defined and <code>navigator.gpu</code> does not
 * exist, because 3D acceleration is not supported by this project. Setting the <code>width</code> or <code>height</code> attribute, even to the
 * same value, replaces the bitmap with a transparent one and resets the context. Reading pixels back from a canvas that drew cross-origin
 * images without permission (<code>getImageData</code>, <code>toDataURL</code>, <code>toBlob</code>) throws <code>SecurityError</code>.
 * <p>
 * 한 페이지의 캔버스(계획 P20-05, P00-08): <code>HTMLCanvasElement</code>(<code>width</code>, <code>height</code>, <code>getContext</code>,
 * <code>toDataURL</code>, <code>toBlob</code>), <code>CanvasRenderingContext2D</code>, <code>CanvasGradient</code>, <code>CanvasPattern</code>,
 * <code>TextMetrics</code>, <code>ImageData</code> 의 스크립트 인터페이스를 설치하고, 스크립트가 건드린 모든 canvas 요소의 {@link CanvasSurface} 를
 * 소유하며, {@link CanvasSource} 로서 렌더러에 답한다. <code>getContext("2d")</code> 는 캔버스의 하나뿐인 컨텍스트를 반환하고, 다른 모든 컨텍스트 형식
 * (<code>webgl</code>, <code>webgl2</code>, <code>experimental-webgl</code>, <code>webgpu</code>, <code>bitmaprenderer</code>, 모르는 이름)은 null 을
 * 반환한다. 이 프로젝트는 3D 가속을 지원하지 않으므로 WebGL 이나 WebGPU 인터페이스 객체도 <code>navigator.gpu</code> 도 정의하지 않는다.
 * <code>width</code> 나 <code>height</code> 속성을 (같은 값이라도) 지정하면 비트맵이 투명한 새것으로 바뀌고 컨텍스트가 초기화된다. 허락 없이 다른 오리진의
 * 이미지를 그린 캔버스에서 화소를 읽어 오면(<code>getImageData</code>, <code>toDataURL</code>, <code>toBlob</code>) <code>SecurityError</code> 를 던진다.
 */
public final class CanvasBindings implements CanvasSource {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final CanvasHost host;

    /**
     * The DOM bridge, known when the interfaces are installed.
     * <p>
     * DOM 브리지. 인터페이스를 설치할 때 알게 된다.
     */
    private DomBridge bridge;

    /**
     * The realm, known when the interfaces are installed.
     * <p>
     * realm. 인터페이스를 설치할 때 알게 된다.
     */
    private JsRealm realm;

    /**
     * The surfaces by canvas element; a surface never refers to its element strongly, so an unused canvas can be collected.
     * <p>
     * 캔버스 요소별 표면. 표면은 요소를 강하게 참조하지 않으므로 쓰이지 않는 캔버스는 수거될 수 있다.
     */
    private final Map<Element, CanvasSurface> surfaces = new WeakHashMap<Element, CanvasSurface>();

    /**
     * The canvas elements that became the placeholder of an <code>OffscreenCanvas</code> (<code>transferControlToOffscreen()</code>): they cannot
     * have a rendering context and their bitmap is not resized by the size attributes any more. Elements are held weakly.
     * <p>
     * <code>OffscreenCanvas</code> 의 자리표시가 된 canvas 요소(<code>transferControlToOffscreen()</code>). 렌더링 컨텍스트를 가질 수 없고 크기 속성으로
     * 비트맵이 다시 만들어지지도 않는다. 요소는 약하게 잡는다.
     */
    private final Map<Element, Boolean> placeholders = new WeakHashMap<Element, Boolean>();

    /**
     * The surfaces of the offscreen canvases of the page, so that disposing the page releases their bitmaps. Surfaces are held weakly.
     * <p>
     * 페이지의 offscreen 캔버스의 표면. 페이지를 폐기하면 그 비트맵을 해제하기 위한 것이다. 표면은 약하게 잡는다.
     */
    private final Map<CanvasSurface, Boolean> offscreens = new WeakHashMap<CanvasSurface, Boolean>();

    /**
     * The script interface of <code>OffscreenCanvas</code>, <code>ImageBitmap</code> and <code>createImageBitmap()</code>, or null before the
     * interfaces are installed.
     * <p>
     * <code>OffscreenCanvas</code>, <code>ImageBitmap</code>, <code>createImageBitmap()</code> 의 스크립트 인터페이스. 인터페이스를 설치하기 전에는 null.
     */
    private OffscreenBindings offscreen;

    /**
     * The listener on the document that resets bitmaps when the size attributes are set.
     * <p>
     * 크기 속성이 지정될 때 비트맵을 초기화하는 문서의 리스너.
     */
    private DomMutationListener mutationListener;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private boolean disposed;

    /**
     * Receives the change notifications of a surface and passes them to the page without holding the element.
     * <p>
     * 표면의 변경 알림을 받아 요소를 붙잡지 않고 페이지에 전달한다.
     */
    private static final class ElementListener implements CanvasSurface.Listener {
        /**
         * The element, weakly.
         * <p>
         * 요소(약한 참조).
         */
        private final java.lang.ref.WeakReference<Element> element;

        /**
         * The page.
         * <p>
         * 페이지.
         */
        private final CanvasHost host;

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         *
         * @param element the element / 요소
         * @param host    the page / 페이지
         */
        ElementListener(Element element, CanvasHost host) {
            this.element = new java.lang.ref.WeakReference<Element>(element);
            this.host = host;
        }

        /**
         * Tells the page that the canvas changed.
         * <p>
         * 캔버스가 바뀌었음을 페이지에 알린다.
         */
        public void surfaceChanged() {
            Element e = element.get();
            if (e != null && !host.isDisposed()) {
                host.canvasChanged(e);
            }
        }
    }

    /**
     * Creates the canvas bindings of a page; the realm and the bridge of the page may not exist yet, they are read by {@link #install}.
     * <p>
     * 페이지의 캔버스 연결을 만든다. 페이지의 realm 과 브리지는 아직 없을 수 있으며 {@link #install} 이 읽는다.
     *
     * @param host the page / 페이지
     */
    public CanvasBindings(CanvasHost host) {
        this.host = host;
    }

    /**
     * Tells whether an element is an HTML <code>canvas</code> element.
     * <p>
     * 요소가 HTML <code>canvas</code> 요소인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a canvas / canvas 이면 true
     */
    private static boolean isCanvas(Element e) {
        return e != null && "canvas".equals(e.getLocalName() == null ? null : e.getLocalName().toLowerCase(Locale.ENGLISH))
                && (e.getNamespaceURI() == null || Namespaces.HTML.equals(e.getNamespaceURI()));
    }

    /**
     * Returns the canvas element of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 캔버스 요소를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element canvas(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (!isCanvas(e)) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Returns the surface of a canvas element, creating it with the size of the attributes at the first call.
     * <p>
     * 캔버스 요소의 표면을 반환한다. 처음 부를 때 속성의 크기로 만든다.
     *
     * @param e the canvas element / 캔버스 요소
     * @return the surface / 표면
     */
    CanvasSurface surfaceOf(Element e) {
        synchronized (surfaces) {
            CanvasSurface s = surfaces.get(e);
            if (s == null) {
                s = new CanvasSurface(CanvasSizes.width(e), CanvasSizes.height(e), new ElementListener(e, host));
                s.setOwner(e);
                if (disposed) {
                    s.dispose();
                }
                surfaces.put(e, s);
            }
            return s;
        }
    }

    /**
     * Returns the snapshot that the renderer paints.
     * <p>
     * 렌더러가 그리는 스냅샷을 반환한다.
     *
     * @param canvas the <code>canvas</code> element / <code>canvas</code> 요소
     * @return the snapshot or null / 스냅샷 또는 null
     */
    public DecodedImage getCanvasImage(Element canvas) {
        CanvasSurface s;
        synchronized (surfaces) {
            s = surfaces.get(canvas);
        }
        return s == null ? null : s.snapshot();
    }

    /**
     * Releases every bitmap and context; the bindings draw nothing afterwards.
     * <p>
     * 모든 비트맵과 컨텍스트를 해제한다. 그 뒤 연결은 아무것도 그리지 않는다.
     */
    public void dispose() {
        java.util.List<CanvasSurface> all;
        synchronized (surfaces) {
            disposed = true;
            all = new java.util.ArrayList<CanvasSurface>(surfaces.values());
            surfaces.clear();
            all.addAll(offscreens.keySet());
            offscreens.clear();
            placeholders.clear();
        }
        // 표면을 하나씩 해제한다.
        for (int i = 0; i < all.size(); i++) {
            all.get(i).dispose();
        }
        if (mutationListener != null) {
            host.document().removeMutationListener(mutationListener);
            mutationListener = null;
        }
    }

    /**
     * Remembers the surface of an offscreen canvas so that disposing the page releases its bitmap; a surface that is registered after the
     * disposal is released at once.
     * <p>
     * offscreen 캔버스의 표면을 기억해 페이지를 폐기할 때 비트맵을 해제하게 한다. 폐기 뒤에 등록되는 표면은 곧바로 해제한다.
     *
     * @param s the surface / 표면
     */
    void track(CanvasSurface s) {
        boolean already;
        synchronized (surfaces) {
            already = disposed;
            if (!already) {
                offscreens.put(s, Boolean.TRUE);
            }
        }
        // 이미 폐기된 페이지의 표면은 바로 해제한다.
        if (already) {
            s.dispose();
        }
    }

    /**
     * Tells whether a canvas element is the placeholder of an <code>OffscreenCanvas</code>.
     * <p>
     * canvas 요소가 <code>OffscreenCanvas</code> 의 자리표시인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a placeholder / 자리표시이면 true
     */
    boolean isPlaceholder(Element e) {
        synchronized (surfaces) {
            return placeholders.containsKey(e);
        }
    }

    /**
     * Returns how many canvases have a surface (for tests).
     * <p>
     * 표면을 가진 캔버스의 수를 반환한다(시험용).
     *
     * @return the number / 개수
     */
    public int surfaceCount() {
        synchronized (surfaces) {
            return surfaces.size();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 도우미: 색, 글꼴, 이미지 소스
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads the font size and color of the canvas element into the context, when the text refers to them (<code>em</code>, a percentage,
     * <code>larger</code>, <code>smaller</code>, <code>currentcolor</code>).
     * <p>
     * 텍스트가 캔버스 요소의 글꼴 크기나 색을 참조하면(<code>em</code>, 백분율, <code>larger</code>, <code>smaller</code>, <code>currentcolor</code>) 그 값을
     * 컨텍스트에 읽어 넣는다.
     *
     * @param c    the context / 컨텍스트
     * @param text the text that is about to be parsed / 곧 해석할 텍스트
     */
    void refreshElementStyle(Context2D c, String text) {
        String t = text.toLowerCase(Locale.ENGLISH);
        if (t.indexOf("em") < 0 && t.indexOf('%') < 0 && t.indexOf("larger") < 0 && t.indexOf("smaller") < 0 && t.indexOf("currentcolor") < 0) {
            return;
        }
        Object owner = c.getSurface().getOwner();
        StyleResolver resolver = host.styleResolver();
        // 요소와 스타일 리졸버가 있으면 계산된 글꼴 크기와 색을 읽는다.
        if (owner instanceof Element && resolver != null) {
            ComputedStyle cs = resolver.getComputedStyle((Element) owner);
            c.setElementStyle(cs.fontSizePx(), cs.color(Prop.COLOR));
        }
    }

    /**
     * Draws a focus ring if an element has the focus and is a descendant of the canvas element (the steps of <code>drawFocusIfNeeded()</code>):
     * nothing is drawn for an element that is not focused, for the canvas element itself, for an element outside the canvas, and for an
     * <code>OffscreenCanvas</code>, which has no fallback content.
     * <p>
     * 요소가 포커스를 가졌고 캔버스 요소의 자손이면 포커스 링을 그린다(<code>drawFocusIfNeeded()</code> 의 단계): 포커스가 없는 요소, 캔버스 요소 자신, 캔버스 밖의
     * 요소, 대체 콘텐츠가 없는 <code>OffscreenCanvas</code> 에는 아무것도 그리지 않는다.
     *
     * @param c       the context / 컨텍스트
     * @param path    the path or null for the current default path / 경로. 현재 기본 경로이면 null
     * @param element the element that may have the focus / 포커스를 가졌을 수 있는 요소
     */
    void drawFocusIfNeeded(Context2D c, Path2DValue path, Element element) {
        Object canvas = c.getSurface().getOwner();
        if (!(canvas instanceof Element) || FocusManager.getFocused(host.document()) != element) {
            return;
        }
        // 요소의 조상을 따라 올라가며 캔버스 요소를 찾는다.
        for (Node n = element.getParentNode(); n != null; n = n.getParentNode()) {
            if (n == canvas) {
                c.drawFocusRing(path);
                return;
            }
        }
    }

    /**
     * Parses a color for a style attribute, resolving <code>currentcolor</code>.
     * <p>
     * 스타일 속성을 위해 색을 해석한다. <code>currentcolor</code> 도 풀어 준다.
     *
     * @param c    the context / 컨텍스트
     * @param text the color text / 색 문자열
     * @return the ARGB color, or null if the text is not a color (the assignment is then ignored) / ARGB 색. 색이 아니면 null(그러면 대입이 무시된다)
     */
    Integer parseColor(Context2D c, String text) {
        refreshElementStyle(c, text);
        return CanvasColors.parse(text, c.getCurrentColor());
    }

    /**
     * Converts a script value to an image source for <code>drawImage()</code> and <code>createPattern()</code>.
     * <p>
     * 스크립트 값을 <code>drawImage()</code> 와 <code>createPattern()</code> 의 이미지 소스로 변환한다.
     *
     * @param target the context that draws / 그리는 컨텍스트
     * @param v      the value / 값
     * @return the pixels, or null if there is nothing to draw (an image that is not loaded yet, a video) / 화소. 그릴 것이 없으면(아직 읽지 않은 이미지, 비디오) null
     * @throws DomException InvalidStateError for a broken image or an empty canvas / 깨진 이미지나 빈 캔버스이면 InvalidStateError
     */
    CanvasImage imageSource(Context2D target, JsValue v) {
        // ImageBitmap 과 OffscreenCanvas 는 요소가 아닌 값 객체이다.
        ImageBitmapValue bitmap = (ImageBitmapValue) DomBridge.targetOf(v, ImageBitmapValue.class);
        if (bitmap != null) {
            CanvasImage pixels = bitmap.image();
            if (pixels == null) {
                throw DomException.invalidState("The image source is detached.");
            }
            return pixels;
        }
        OffscreenCanvasValue offscreenCanvas = (OffscreenCanvasValue) DomBridge.targetOf(v, OffscreenCanvasValue.class);
        if (offscreenCanvas != null) {
            CanvasSurface s = offscreenCanvas.surface();
            if (s == null) {
                throw DomException.invalidState("The image source is detached.");
            }
            if (s.getWidth() == 0 || s.getHeight() == 0) {
                throw DomException.invalidState("The image argument is an OffscreenCanvas with a width or height of 0.");
            }
            return s.image(s == target.getSurface());
        }
        Element e = (Element) DomBridge.targetOf(v, Element.class);
        String tag = e == null || e.getLocalName() == null ? "" : e.getLocalName().toLowerCase(Locale.ENGLISH);
        if ("img".equals(tag)) {
            return host.imagePixels(e);
        }
        if ("canvas".equals(tag)) {
            CanvasSurface s = surfaceOf(e);
            if (s.getWidth() == 0 || s.getHeight() == 0) {
                throw DomException.invalidState("The image argument is a canvas element with a width or height of 0.");
            }
            return s.image(s == target.getSurface());
        }
        if ("video".equals(tag)) {
            // 비디오는 지원하지 않으므로 그릴 프레임이 없다. 규격은 HAVE_NOTHING 인 비디오를 그리지 않고 반환하라고 한다.
            return null;
        }
        throw JsException.typeError("The provided value is not of type '(CSSImageValue or HTMLImageElement or SVGImageElement or "
                + "HTMLVideoElement or HTMLCanvasElement or ImageBitmap or OffscreenCanvas or VideoFrame)'.");
    }

    /**
     * Converts a script value to the source picture of <code>createImageBitmap()</code> (<code>ImageBitmapSource</code>): an <code>img</code>
     * element, a <code>canvas</code> element, an <code>OffscreenCanvas</code>, an <code>ImageBitmap</code>, an <code>ImageData</code> or a
     * <code>Blob</code> whose bytes are decoded as an image. The pixels are a copy that the caller may keep. A <code>video</code> element is a
     * source that is not usable here (video is not supported by this project).
     * <p>
     * 스크립트 값을 <code>createImageBitmap()</code> 의 원본 그림(<code>ImageBitmapSource</code>)으로 변환한다: <code>img</code> 요소, <code>canvas</code> 요소,
     * <code>OffscreenCanvas</code>, <code>ImageBitmap</code>, <code>ImageData</code>, 바이트를 이미지로 디코딩하는 <code>Blob</code>. 화소는 호출자가 가져도 되는
     * 복사본이다. <code>video</code> 요소는 여기서 쓸 수 없는 원본이다(이 프로젝트는 비디오를 지원하지 않는다).
     *
     * @param v the value / 값
     * @return the pixels / 화소
     * @throws DomException InvalidStateError if the source is not usable (a broken or unloaded image, an empty canvas, a closed bitmap, a blob that
     *                      is no image, a video) / 원본을 쓸 수 없으면(깨졌거나 아직 없는 이미지, 빈 캔버스, 닫힌 비트맵, 이미지가 아닌 블롭, 비디오) InvalidStateError
     * @throws JsException  TypeError if the value is none of the source types / 값이 원본 형식 중 어느 것도 아니면 TypeError
     */
    CanvasImage bitmapSource(JsValue v) {
        ImageBitmapValue bitmap = (ImageBitmapValue) DomBridge.targetOf(v, ImageBitmapValue.class);
        if (bitmap != null) {
            CanvasImage pixels = bitmap.image();
            if (pixels == null) {
                throw DomException.invalidState("The ImageBitmap has been detached.");
            }
            return pixels;
        }
        OffscreenCanvasValue offscreenCanvas = (OffscreenCanvasValue) DomBridge.targetOf(v, OffscreenCanvasValue.class);
        if (offscreenCanvas != null) {
            return surfacePixels(offscreenCanvas.surface());
        }
        ImageDataValue data = (ImageDataValue) DomBridge.targetOf(v, ImageDataValue.class);
        if (data != null) {
            byte[] b = data.bytes();
            int[] argb = new int[data.getWidth() * data.getHeight()];
            // RGBA 바이트를 ARGB 화소로 바꾼다(ImageData 가 나중에 바뀌어도 비트맵은 바뀌지 않도록 복사한다).
            for (int i = 0; i < argb.length; i++) {
                argb[i] = ((b[i * 4 + 3] & 0xFF) << 24) | ((b[i * 4] & 0xFF) << 16) | ((b[i * 4 + 1] & 0xFF) << 8) | (b[i * 4 + 2] & 0xFF);
            }
            return new CanvasImage(data.getWidth(), data.getHeight(), argb, true);
        }
        byte[] blob = org.duckdns.hjow.swingwebpanel.js.host.web.BlobBytes.bytes(v);
        if (blob != null) {
            return decodeBlob(blob, org.duckdns.hjow.swingwebpanel.js.host.web.BlobBytes.type(v));
        }
        Element e = (Element) DomBridge.targetOf(v, Element.class);
        String tag = e == null || e.getLocalName() == null ? "" : e.getLocalName().toLowerCase(Locale.ENGLISH);
        if ("img".equals(tag)) {
            CanvasImage pixels = host.imagePixels(e);
            if (pixels == null) {
                throw DomException.invalidState("The HTMLImageElement provided is not completely available.");
            }
            return pixels;
        }
        if ("canvas".equals(tag)) {
            return surfacePixels(surfaceOf(e));
        }
        if ("video".equals(tag)) {
            throw DomException.invalidState("Video is not supported, so the HTMLVideoElement has no frame to copy.");
        }
        throw JsException.typeError("The provided value is not of type '(HTMLImageElement or SVGImageElement or HTMLVideoElement or HTMLCanvasElement "
                + "or ImageBitmap or OffscreenCanvas or VideoFrame or Blob or ImageData)'.");
    }

    /**
     * The limits for decoding the bytes of a <code>Blob</code> in <code>createImageBitmap()</code> (the same as for the images of the page).
     * <p>
     * <code>createImageBitmap()</code> 이 <code>Blob</code> 의 바이트를 디코딩할 때의 한도(페이지의 이미지와 같다).
     */
    private static final org.duckdns.hjow.swingwebpanel.media.DecodeLimits BLOB_LIMITS = new org.duckdns.hjow.swingwebpanel.media.DecodeLimits(8L * 1024 * 1024, 8L * 1024 * 1024, 256,
            16L * 1024 * 1024);

    /**
     * Decodes the bytes of a <code>Blob</code> as an image and returns its first frame (an animation gives its default picture).
     * <p>
     * <code>Blob</code> 의 바이트를 이미지로 디코딩해 첫 프레임을 반환한다(애니메이션은 기본 그림을 준다).
     *
     * @param bytes the bytes / 바이트
     * @param type  the MIME type of the blob / 블롭의 MIME 타입
     * @return the pixels / 화소
     * @throws DomException InvalidStateError if the bytes are not an image that the decoders know / 바이트가 디코더가 아는 이미지가 아니면 InvalidStateError
     */
    private static CanvasImage decodeBlob(byte[] bytes, String type) {
        try {
            DecodedImage di = org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders.decode(bytes, type, BLOB_LIMITS);
            return new CanvasImage(di.getWidth(), di.getHeight(), di.getFrames().get(0).getArgb().clone(), true);
        } catch (org.duckdns.hjow.swingwebpanel.media.MediaException e) {
            throw DomException.invalidState("The source image could not be decoded.");
        }
    }

    /**
     * Returns a copy of the pixels of a canvas surface for <code>createImageBitmap()</code>, with the origin-clean flag of the surface.
     * <p>
     * <code>createImageBitmap()</code> 을 위해 캔버스 표면 화소의 복사본을 표면의 origin-clean 표시와 함께 반환한다.
     *
     * @param s the surface, or null for a detached offscreen canvas / 표면. 떼어 낸 offscreen 캔버스이면 null
     * @return the pixels / 화소
     * @throws DomException InvalidStateError if the surface is detached or has no pixels / 표면을 떼어 냈거나 화소가 없으면 InvalidStateError
     */
    private static CanvasImage surfacePixels(CanvasSurface s) {
        if (s == null) {
            throw DomException.invalidState("The canvas has been detached.");
        }
        if (s.getWidth() == 0 || s.getHeight() == 0) {
            throw DomException.invalidState("The canvas has a width or height of 0.");
        }
        CanvasImage pixels = s.image(true);
        if (pixels == null) {
            throw DomException.invalidState("The canvas is too large to be copied.");
        }
        return pixels;
    }

    /**
     * Reads the radii argument of <code>roundRect()</code> into the eight numbers that the engine takes.
     * <p>
     * <code>roundRect()</code> 의 반지름 인수를 엔진이 받는 숫자 여덟 개로 읽는다.
     *
     * @param v the argument: undefined, a number, an object with x and y, or a list of one to four of them / 인수: undefined, 숫자, x 와 y 를 가진 객체, 또는 그 하나~넷의 목록
     * @return eight numbers, or null if the argument is non-finite (nothing is drawn) / 숫자 여덟 개. 유한하지 않으면 null(아무것도 그리지 않음)
     */
    static double[] radii(JsValue v) {
        JsValue[] list;
        if (v.isUndefined()) {
            list = new JsValue[] { Js.num(0) };
        } else if (v.isObject() && Ops.isArray(v)) {
            list = Ops.createListFromArrayLike(v);
        } else {
            list = new JsValue[] { v };
        }
        if (list.length < 1 || list.length > 4) {
            throw JsException.rangeError("Failed to execute 'roundRect': " + list.length + " radii provided. Between one and four radii are necessary.");
        }
        double[][] corner = new double[list.length][2];
        // 반지름 항목마다 숫자 하나이거나 x, y 를 가진 객체이다.
        for (int i = 0; i < list.length; i++) {
            if (list[i].isObject()) {
                JsObject o = (JsObject) list[i];
                JsValue x = o.get("x");
                JsValue y = o.get("y");
                corner[i][0] = x.isUndefined() ? 0 : Ops.toNumber(x);
                corner[i][1] = y.isUndefined() ? 0 : Ops.toNumber(y);
            } else {
                corner[i][0] = Ops.toNumber(list[i]);
                corner[i][1] = corner[i][0];
            }
            if (Double.isNaN(corner[i][0]) || Double.isInfinite(corner[i][0]) || Double.isNaN(corner[i][1]) || Double.isInfinite(corner[i][1])) {
                return null;
            }
            if (corner[i][0] < 0 || corner[i][1] < 0) {
                throw JsException.rangeError("Failed to execute 'roundRect': Radius value " + (corner[i][0] < 0 ? corner[i][0] : corner[i][1]) + " is negative.");
            }
        }
        // 항목 수에 따라 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래 모서리에 나눠 준다.
        int[][] pick = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 1, 0, 1 }, { 0, 1, 2, 1 }, { 0, 1, 2, 3 } };
        double[] out = new double[8];
        // 네 모서리의 x, y 반지름을 차례로 채운다.
        for (int k = 0; k < 4; k++) {
            double[] c = corner[pick[list.length][k]];
            out[k * 2] = c[0];
            out[k * 2 + 1] = c[1];
        }
        return out;
    }

    /**
     * Creates an <code>ImageData</code> of transparent black pixels.
     * <p>
     * 투명한 검정 화소의 <code>ImageData</code> 를 만든다.
     *
     * @param w width in pixels / 너비(화소)
     * @param h height in pixels / 높이(화소)
     * @return the object / 객체
     */
    JsValue newImageData(int w, int h) {
        if ((long) w * h > CanvasLimits.MAX_IMAGE_DATA_PIXELS) {
            throw JsException.rangeError("The requested image size exceeds the supported range.");
        }
        return newImageData(w, h, new byte[w * h * 4]);
    }

    /**
     * Creates an <code>ImageData</code> over bytes (the bytes are shared with its <code>data</code> array).
     * <p>
     * 바이트 위의 <code>ImageData</code> 를 만든다(바이트는 <code>data</code> 배열과 공유한다).
     *
     * @param w     width in pixels / 너비(화소)
     * @param h     height in pixels / 높이(화소)
     * @param bytes the RGBA bytes / RGBA 바이트
     * @return the object / 객체
     */
    JsValue newImageData(int w, int h, byte[] bytes) {
        ImageDataValue v = new ImageDataValue(w, h, bytes);
        v.setDataObject(TypedArrays.newUint8ClampedArray(realm, TypedArrays.newArrayBuffer(realm, bytes), 0, bytes.length));
        return bridge.toJs(v);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 설치
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Installs the interfaces, the members of the canvas element and the document listener.
     * <p>
     * 인터페이스, 캔버스 요소의 멤버, 문서 리스너를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    public void install(JsObject g) {
        installShared(g);
        installCanvasElement();
        mutationListener = new DomMutationListener() {
            /**
             * Resets the bitmap of a canvas whose size attribute was set.
             * <p>
             * 크기 속성이 지정된 캔버스의 비트맵을 초기화한다.
             *
             * @param record    the change / 변경
             * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
             */
            public void mutated(MutationRecord record, boolean connected) {
                if (record.getType() != MutationRecord.ATTRIBUTES || !(record.getTarget() instanceof Element)) {
                    return;
                }
                String name = record.getAttributeName();
                Element e = (Element) record.getTarget();
                if (("width".equals(name) || "height".equals(name)) && isCanvas(e)) {
                    CanvasSurface s;
                    synchronized (surfaces) {
                        s = placeholders.containsKey(e) ? null : surfaces.get(e);
                    }
                    if (s != null) {
                        s.resize(CanvasSizes.width(e), CanvasSizes.height(e));
                    }
                }
            }
        };
        host.document().addMutationListener(mutationListener);
    }

    /**
     * Installs what every script context that has canvases needs, a page and a worker alike: the interfaces <code>CanvasRenderingContext2D</code>,
     * <code>OffscreenCanvasRenderingContext2D</code>, <code>CanvasGradient</code>, <code>CanvasPattern</code>, <code>TextMetrics</code>,
     * <code>ImageData</code>, <code>Path2D</code>, <code>OffscreenCanvas</code> and <code>ImageBitmap</code> and the global function
     * <code>createImageBitmap()</code>. The <code>canvas</code> element is not part of it (a worker has no elements).
     * <p>
     * 캔버스를 가진 모든 스크립트 문맥(페이지와 worker)이 필요로 하는 것을 설치한다: 인터페이스 <code>CanvasRenderingContext2D</code>,
     * <code>OffscreenCanvasRenderingContext2D</code>, <code>CanvasGradient</code>, <code>CanvasPattern</code>, <code>TextMetrics</code>, <code>ImageData</code>,
     * <code>Path2D</code>, <code>OffscreenCanvas</code>, <code>ImageBitmap</code> 과 전역 함수 <code>createImageBitmap()</code>. <code>canvas</code> 요소는 여기에 속하지
     * 않는다(worker 에는 요소가 없다).
     *
     * @param g the global object / 전역 객체
     */
    public void installShared(JsObject g) {
        bridge = host.bridge();
        realm = host.realm();
        bridge.defineInterface("CanvasRenderingContext2D", null, Context2D.class, new String[0]);
        bridge.defineInterface("OffscreenCanvasRenderingContext2D", null, Context2D.class, new String[0]);
        bridge.defineInterface("CanvasGradient", null, CanvasGradient.class, new String[0]);
        bridge.defineInterface("CanvasPattern", null, CanvasPattern.class, new String[0]);
        bridge.defineInterface("TextMetrics", null, TextMetricsValue.class, new String[] { "a:width", "a:actualBoundingBoxLeft",
            "a:actualBoundingBoxRight", "a:fontBoundingBoxAscent", "a:fontBoundingBoxDescent", "a:actualBoundingBoxAscent",
            "a:actualBoundingBoxDescent", "a:emHeightAscent", "a:emHeightDescent", "a:hangingBaseline", "a:alphabeticBaseline",
            "a:ideographicBaseline" });
        bridge.defineInterface("ImageData", null, ImageDataValue.class, new String[] { "a:width", "a:height", "a:colorSpace" });
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Names the interface of the canvas value objects.
             * <p>
             * 캔버스 값 객체의 인터페이스 이름을 정한다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름. 모르면 null
             */
            public String classify(Object target) {
                if (target instanceof Context2D) {
                    // offscreen 캔버스의 컨텍스트는 OffscreenCanvasRenderingContext2D 이다.
                    return ((Context2D) target).getSurface().getOwner() instanceof OffscreenCanvasValue ? "OffscreenCanvasRenderingContext2D"
                            : "CanvasRenderingContext2D";
                } else if (target instanceof OffscreenCanvasValue) {
                    return "OffscreenCanvas";
                } else if (target instanceof ImageBitmapValue) {
                    return "ImageBitmap";
                } else if (target instanceof CanvasGradient) {
                    return "CanvasGradient";
                } else if (target instanceof CanvasPattern) {
                    return "CanvasPattern";
                } else if (target instanceof TextMetricsValue) {
                    return "TextMetrics";
                } else if (target instanceof Path2DValue) {
                    return "Path2D";
                }
                return target instanceof ImageDataValue ? "ImageData" : null;
            }
        });
        new Path2DBindings(host).install();
        new Context2DBindings(this, host).install(bridge.getInterface("CanvasRenderingContext2D").getPrototype(), true);
        new Context2DBindings(this, host).install(bridge.getInterface("OffscreenCanvasRenderingContext2D").getPrototype(), false);
        installGradient();
        installPattern();
        installImageData();
        offscreen = new OffscreenBindings(this, host);
        offscreen.install(g);
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param call   the behaviour / 동작
     */
    private void method(JsObject proto, String name, int length, NativeFunction.Call call) {
        proto.defineData(name, realm.newFunction(name, length, call), true, true, true);
    }

    /**
     * Installs the method <code>addColorStop</code> on the prototype of <code>CanvasGradient</code>.
     * <p>
     * <code>CanvasGradient</code> 의 프로토타입에 <code>addColorStop</code> 메서드를 설치한다.
     */
    private void installGradient() {
        method(bridge.getInterface("CanvasGradient").getPrototype(), "addColorStop", 2, new NativeFunction.Call() {
            /**
             * Adds a color stop: an offset outside 0..1 is an IndexSizeError, a color that cannot be parsed is a SyntaxError.
             * <p>
             * 색 정지점을 더한다. 0..1 밖의 오프셋은 IndexSizeError, 해석할 수 없는 색은 SyntaxError 이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                CanvasGradient grad = (CanvasGradient) DomBridge.targetOf(self, CanvasGradient.class);
                if (grad == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'addColorStop' on 'CanvasGradient': 2 arguments required, but only " + args.length
                            + " present.");
                }
                double offset = Ops.toNumber(args[0]);
                if (Double.isNaN(offset) || Double.isInfinite(offset)) {
                    throw JsException.typeError("Failed to execute 'addColorStop' on 'CanvasGradient': The provided double value is non-finite.");
                }
                String text = Ops.toString(args[1]);
                if (offset < 0 || offset > 1) {
                    throw bridge.domError(DomException.indexSize("The provided value (" + offset + ") is outside the range (0.0, 1.0)."));
                }
                Integer color = CanvasColors.parse(text, 0xFF000000);
                if (color == null) {
                    throw bridge.domError(DomException.syntax("The value provided ('" + text + "') could not be parsed as a color."));
                }
                grad.addColorStop(offset, color.intValue());
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>CanvasPattern.prototype.setTransform()</code>: the argument is a <code>DOMMatrix2DInit</code> dictionary (another matrix
     * is fine; missing or <code>undefined</code> is the identity), and a TypeError for conflicting members.
     * <p>
     * <code>CanvasPattern.prototype.setTransform()</code> 을 설치한다. 인수는 <code>DOMMatrix2DInit</code> 사전이다(다른 행렬도 된다. 없거나
     * <code>undefined</code> 이면 단위 행렬). 충돌하는 멤버는 TypeError 이다.
     */
    private void installPattern() {
        method(bridge.getInterface("CanvasPattern").getPrototype(), "setTransform", 0, new NativeFunction.Call() {
            /**
             * Sets the transformation of the pattern.
             * <p>
             * 패턴의 변환을 정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                CanvasPattern pattern = (CanvasPattern) DomBridge.targetOf(self, CanvasPattern.class);
                if (pattern == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                double[] m = MatrixBindings.fromInit2D(Js.arg(args, 0)).to2DArray();
                pattern.setPatternTransform(m[0], m[1], m[2], m[3], m[4], m[5]);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the <code>data</code> attribute and the constructor of <code>ImageData</code>.
     * <p>
     * <code>ImageData</code> 의 <code>data</code> 속성과 생성자를 설치한다.
     */
    private void installImageData() {
        JsObject proto = bridge.getInterface("ImageData").getPrototype();
        proto.defineAccessor("data", realm.newFunction("get data", 0, new NativeFunction.Call() {
            /**
             * Returns the same <code>Uint8ClampedArray</code> every time.
             * <p>
             * 매번 같은 <code>Uint8ClampedArray</code> 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array / 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ImageDataValue v = (ImageDataValue) DomBridge.targetOf(self, ImageDataValue.class);
                if (v == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return v.dataObject();
            }
        }), null, true, true);
        NativeFunction ctor = new NativeFunction(realm, "ImageData", 2, new NativeFunction.Call() {
            /**
             * Calling the constructor without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 생성자를 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'ImageData': Please use the 'new' operator, this DOM object constructor cannot be called "
                        + "as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an <code>ImageData</code> of a size, or over a <code>Uint8ClampedArray</code>.
             * <p>
             * 크기가 정해진 <code>ImageData</code> 나 <code>Uint8ClampedArray</code> 위의 <code>ImageData</code> 를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the object / 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return (JsObject) constructImageData(args);
            }
        });
        bridge.setConstructor("ImageData", ctor);
    }

    /**
     * Implements <code>new ImageData(sw, sh)</code> and <code>new ImageData(data, sw[, sh])</code>.
     * <p>
     * <code>new ImageData(sw, sh)</code> 와 <code>new ImageData(data, sw[, sh])</code> 를 구현한다.
     *
     * @param args the arguments / 인수
     * @return the object / 객체
     */
    private JsValue constructImageData(JsValue[] args) {
        if (args.length < 2) {
            throw JsException.typeError("Failed to construct 'ImageData': 2 arguments required, but only " + args.length + " present.");
        }
        try {
            if (!args[0].isObject()) {
                long sw = Ops.toUint32(args[0]);
                long sh = Ops.toUint32(args[1]);
                if (sw == 0 || sh == 0) {
                    throw DomException.indexSize("The source width or height is zero.");
                }
                if (sw * sh > CanvasLimits.MAX_IMAGE_DATA_PIXELS) {
                    throw JsException.rangeError("The requested image size exceeds the supported range.");
                }
                return newImageData((int) sw, (int) sh);
            }
            if (!(args[0] instanceof TypedArrayObject) || ((TypedArrayObject) args[0]).getType() != ElementType.UINT8_CLAMPED) {
                throw JsException.typeError("Failed to construct 'ImageData': parameter 1 is not of type 'Uint8ClampedArray'.");
            }
            TypedArrayObject array = (TypedArrayObject) args[0];
            int length = array.getByteLength();
            if (length % 4 != 0) {
                throw DomException.invalidState("The input data length is not a multiple of 4.");
            }
            long sw = Ops.toUint32(args[1]);
            int pixels = length / 4;
            if (sw == 0) {
                throw DomException.indexSize("The source width is zero.");
            }
            if (pixels % sw != 0) {
                throw DomException.indexSize("The input data length is not a multiple of (4 * width).");
            }
            long height = pixels / sw;
            if (args.length > 2 && !args[2].isUndefined() && Ops.toUint32(args[2]) != height) {
                throw DomException.indexSize("The input data length is not equal to (4 * width * height).");
            }
            boolean whole = array.getByteOffset() == 0 && array.getBuffer().getByteLength() == length;
            byte[] bytes = whole ? array.getBuffer().getData() : java.util.Arrays.copyOfRange(array.getBuffer().getData(), array.getByteOffset(),
                    array.getByteOffset() + length);
            ImageDataValue v = new ImageDataValue((int) sw, (int) height, bytes);
            v.setDataObject(whole ? array : TypedArrays.newUint8ClampedArray(realm, TypedArrays.newArrayBuffer(realm, bytes), 0, length));
            return bridge.toJs(v);
        } catch (DomException e) {
            throw bridge.domError(e);
        }
    }

    /**
     * Installs the members of <code>HTMLCanvasElement</code>.
     * <p>
     * <code>HTMLCanvasElement</code> 의 멤버를 설치한다.
     */
    private void installCanvasElement() {
        JsObject proto = bridge.getInterface("HTMLCanvasElement").getPrototype();
        installSize(proto, "width", CanvasSizes.DEFAULT_WIDTH);
        installSize(proto, "height", CanvasSizes.DEFAULT_HEIGHT);
        method(proto, "getContext", 1, new NativeFunction.Call() {
            /**
             * Returns the 2D context, and null for every other context type (WebGL and WebGPU are not supported).
             * <p>
             * 2D 컨텍스트를 반환하고, 다른 모든 컨텍스트 형식(WebGL 과 WebGPU 는 지원하지 않음)에는 null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the context or null / 컨텍스트 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = canvas(self);
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'getContext' on 'HTMLCanvasElement': 1 argument required, but only 0 present.");
                }
                // transferControlToOffscreen() 로 OffscreenCanvas 에 넘긴 요소는 어떤 컨텍스트도 가질 수 없다.
                if (isPlaceholder(e)) {
                    throw bridge.domError(DomException.invalidState("Cannot get context from a canvas that has transferred its control to offscreen."));
                }
                // 2d 만 지원한다. webgl, webgl2, experimental-webgl, webgpu 등은 null 이다.
                if (!"2d".equals(Ops.toString(args[0]))) {
                    return Js.NULL;
                }
                CanvasSurface s = surfaceOf(e);
                boolean fresh = !s.hasContext();
                // 컨텍스트 속성(alpha 등)은 처음 컨텍스트를 만들 때만 읽는다. 오류가 나면 컨텍스트를 만들지 않는다.
                if (fresh) {
                    ContextSettings.apply(s, args.length > 1 ? args[1] : Js.UNDEFINED);
                }
                Context2D ctx = s.getContext();
                if (fresh) {
                    refreshElementStyle(ctx, "currentcolor 1em");
                }
                return bridge.toJs(ctx);
            }
        });
        method(proto, "toDataURL", 0, new NativeFunction.Call() {
            /**
             * Returns the picture as a <code>data:</code> URL (PNG by default, JPEG on request).
             * <p>
             * 그림을 <code>data:</code> URL 로 반환한다(기본 PNG, 요청하면 JPEG).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                CanvasEncoder.Encoded enc = encodeCanvas(canvas(self), args, 0, 1);
                return Js.str(enc == null ? "data:," : CanvasEncoder.dataUrl(enc));
            }
        });
        method(proto, "toBlob", 1, new NativeFunction.Call() {
            /**
             * Encodes the picture now and calls the callback with a <code>Blob</code> (or null) in a later task.
             * <p>
             * 그림을 지금 인코딩하고 나중의 작업에서 콜백을 <code>Blob</code>(또는 null)으로 부른다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = canvas(self);
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'toBlob' on 'HTMLCanvasElement': 1 argument required, but only 0 present.");
                }
                if (!(args[0] instanceof JsObject) || !((JsObject) args[0]).isCallable()) {
                    throw JsException.typeError("Failed to execute 'toBlob' on 'HTMLCanvasElement': The callback provided as parameter 1 is not a function.");
                }
                final JsValue callback = args[0];
                CanvasEncoder.Encoded enc = encodeCanvas(e, args, 1, 2);
                final JsValue blob = enc == null ? Js.NULL : makeBlob(enc);
                // 콜백은 다음 작업에서 부른다(setTimeout 0 과 같은 작업 큐).
                JsValue timer = realm.getGlobalObject().get("setTimeout");
                Ops.call(timer, Js.UNDEFINED, new JsValue[] { realm.newFunction("", 0, new NativeFunction.Call() {
                    /**
                     * Calls the callback of <code>toBlob</code>.
                     * <p>
                     * <code>toBlob</code> 의 콜백을 부른다.
                     *
                     * @param r2    the realm / realm
                     * @param self2 this value / this 값
                     * @param args2 arguments / 인수
                     * @return undefined / undefined
                     */
                    public JsValue call(JsRealm r2, JsValue self2, JsValue[] args2) {
                        Ops.call(callback, Js.UNDEFINED, new JsValue[] { blob });
                        return Js.UNDEFINED;
                    }
                }), Js.num(0) });
                return Js.UNDEFINED;
            }
        });
        method(proto, "transferControlToOffscreen", 0, new NativeFunction.Call() {
            /**
             * Returns an <code>OffscreenCanvas</code> that draws into this element (its placeholder): the element can no longer have a context.
             * <p>
             * 이 요소(자리표시)에 그리는 <code>OffscreenCanvas</code> 를 반환한다. 요소는 더 이상 컨텍스트를 가질 수 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the offscreen canvas / offscreen 캔버스
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return offscreen.transferControl(canvas(self));
            }
        });
    }

    /**
     * Makes a canvas element the placeholder of an offscreen canvas that shares its surface (<code>transferControlToOffscreen()</code>).
     * <p>
     * canvas 요소를 자기 표면을 공유하는 offscreen 캔버스의 자리표시로 만든다(<code>transferControlToOffscreen()</code>).
     *
     * @param e the canvas element / canvas 요소
     * @return the surface of the element / 요소의 표면
     * @throws DomException InvalidStateError if the element has a context or is a placeholder already / 요소에 컨텍스트가 있거나 이미 자리표시이면 InvalidStateError
     */
    CanvasSurface makePlaceholder(Element e) {
        CanvasSurface s = surfaceOf(e);
        synchronized (surfaces) {
            if (placeholders.containsKey(e) || s.hasContext()) {
                throw DomException.invalidState("Cannot transfer control from a canvas for more than one time, or from a canvas that has a rendering context.");
            }
            placeholders.put(e, Boolean.TRUE);
        }
        return s;
    }

    /**
     * Defines the <code>width</code> or <code>height</code> accessor: it reads the size of the attribute (with the default for an absent or
     * invalid value) and writes the attribute, which resets the bitmap through the document listener.
     * <p>
     * <code>width</code> 또는 <code>height</code> 접근자를 정의한다: 속성의 크기를 읽고(없거나 올바르지 않으면 기본값) 속성을 쓰며, 쓰면 문서 리스너가
     * 비트맵을 초기화한다.
     *
     * @param proto    the prototype / 프로토타입
     * @param name     <code>width</code> or <code>height</code> / <code>width</code> 또는 <code>height</code>
     * @param fallback the default size / 기본 크기
     */
    private void installSize(JsObject proto, final String name, final int fallback) {
        proto.defineAccessor(name, realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Returns the size in pixels.
             * <p>
             * 크기(화소)를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the size / 크기
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(CanvasSizes.parse(canvas(self).getAttribute(name), fallback));
            }
        }), realm.newFunction("set " + name, 1, new NativeFunction.Call() {
            /**
             * Sets the size attribute (a value above 2147483647 sets the default).
             * <p>
             * 크기 속성을 지정한다(2147483647 보다 크면 기본값을 지정한다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = canvas(self);
                long v = Ops.toUint32(Js.arg(args, 0));
                e.setAttribute(name, Long.toString(v > Integer.MAX_VALUE ? fallback : v));
                return Js.UNDEFINED;
            }
        }), true, true);
    }

    /**
     * Encodes the canvas for <code>toDataURL()</code> and <code>toBlob()</code> after the origin-clean check.
     * <p>
     * 출처 검사를 한 뒤 <code>toDataURL()</code> 과 <code>toBlob()</code> 을 위해 캔버스를 인코딩한다.
     *
     * @param e       the canvas / 캔버스
     * @param args    the arguments / 인수
     * @param typeAt  the position of the MIME type argument / MIME 타입 인수의 위치
     * @param qualAt  the position of the quality argument / 품질 인수의 위치
     * @return the file, or null if the canvas has no pixels / 파일. 캔버스에 화소가 없으면 null
     */
    private CanvasEncoder.Encoded encodeCanvas(Element e, JsValue[] args, int typeAt, int qualAt) {
        String type = args.length > typeAt && !args[typeAt].isUndefined() ? Ops.toString(args[typeAt]) : null;
        double quality = Double.NaN;
        // 품질은 숫자일 때만 쓴다.
        if (args.length > qualAt && args[qualAt].type() == org.duckdns.hjow.swingwebpanel.js.JsType.NUMBER) {
            quality = Ops.toNumber(args[qualAt]);
        }
        return encodeSurface(surfaceOf(e), type, quality);
    }

    /**
     * Encodes the picture of a surface after the origin-clean check.
     * <p>
     * 출처 검사를 한 뒤 표면의 그림을 인코딩한다.
     *
     * @param s       the surface / 표면
     * @param type    the requested MIME type, or null for PNG / 요청한 MIME 타입. PNG 이면 null
     * @param quality the quality 0..1, or NaN for the default / 품질 0..1. 기본값이면 NaN
     * @return the file, or null if the surface has no pixels / 파일. 표면에 화소가 없으면 null
     * @throws JsException SecurityError if the surface is tainted / 표면이 오염되었으면 SecurityError
     */
    CanvasEncoder.Encoded encodeSurface(CanvasSurface s, String type, double quality) {
        CanvasImage img;
        synchronized (s) {
            if (!s.isOriginClean()) {
                throw bridge.domError(DomException.security("Tainted canvases may not be exported."));
            }
            img = s.image(true);
        }
        if (img == null) {
            return null;
        }
        return CanvasEncoder.encode(img, type, quality);
    }

    /**
     * Makes a <code>Blob</code> with the global <code>Blob</code> constructor of the page.
     * <p>
     * 페이지의 전역 <code>Blob</code> 생성자로 <code>Blob</code> 을 만든다.
     *
     * @param enc the encoded file / 인코딩된 파일
     * @return the Blob object, or null if the page has no <code>Blob</code> / Blob 객체. 페이지에 <code>Blob</code> 이 없으면 null
     */
    JsValue makeBlob(CanvasEncoder.Encoded enc) {
        JsValue ctor = realm.getGlobalObject().get("Blob");
        if (!Ops.isConstructor(ctor)) {
            return Js.NULL;
        }
        JsObject options = realm.newObject();
        options.createDataProperty("type", Js.str(enc.type));
        JsValue part = TypedArrays.newUint8Array(realm, enc.bytes);
        return Ops.construct(ctor, new JsValue[] { realm.newArray(new JsValue[] { part }), options });
    }
}
