package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * The script interface of <code>OffscreenCanvas</code>, <code>ImageBitmap</code> and the global function <code>createImageBitmap()</code> (plan item
 * P20-05-b; HTML Standard 4.12.5.3 "The OffscreenCanvas interface" and 8.11.2 "The ImageBitmap interface", retrieved 2026-11-17). They exist in a page
 * and in a worker alike. <code>new OffscreenCanvas(width, height)</code> makes a bitmap that is not in any document; <code>getContext("2d")</code> gives
 * its <code>OffscreenCanvasRenderingContext2D</code> (the same engine and members as <code>CanvasRenderingContext2D</code>; every other context type is
 * null, because WebGL and WebGPU are not supported); <code>width</code> and <code>height</code> replace the bitmap by a transparent one (and reset the
 * context); <code>convertToBlob()</code> returns a promise of an encoded <code>Blob</code>; <code>transferToImageBitmap()</code> hands the picture over as an
 * <code>ImageBitmap</code> and leaves a blank bitmap. <code>createImageBitmap(source[, sx, sy, sw, sh][, options])</code> copies, crops, scales and flips an
 * image, canvas, offscreen canvas, bitmap, <code>ImageData</code> or <code>Blob</code> into an immutable <code>ImageBitmap</code>; a <code>video</code> element
 * is not a usable source because video is not supported. The promise is settled within the call (its reactions run at the next microtask checkpoint),
 * not in a later task; decoding the bytes of a blob runs on the script thread.
 * <p>
 * <code>OffscreenCanvas</code>, <code>ImageBitmap</code>, 전역 함수 <code>createImageBitmap()</code> 의 스크립트 인터페이스(계획 P20-05-b, HTML 표준 4.12.5.3
 * "The OffscreenCanvas interface"와 8.11.2 "The ImageBitmap interface", 조회 2026-11-17). 페이지와 worker 에 모두 있다. <code>new OffscreenCanvas(width, height)</code>
 * 는 어떤 문서에도 속하지 않는 비트맵을 만든다. <code>getContext("2d")</code> 는 그 <code>OffscreenCanvasRenderingContext2D</code> 를 주며(<code>CanvasRenderingContext2D</code>
 * 와 같은 엔진과 멤버), 다른 모든 컨텍스트 형식은 null 이다(WebGL 과 WebGPU 를 지원하지 않는다). <code>width</code>, <code>height</code> 는 비트맵을 투명한 것으로
 * 바꾸고 컨텍스트를 초기화한다. <code>convertToBlob()</code> 은 인코딩된 <code>Blob</code> 의 프라미스를 반환한다. <code>transferToImageBitmap()</code> 은 그림을
 * <code>ImageBitmap</code> 으로 넘기고 빈 비트맵을 남긴다. <code>createImageBitmap(source[, sx, sy, sw, sh][, options])</code> 은 이미지, 캔버스, offscreen 캔버스,
 * 비트맵, <code>ImageData</code>, <code>Blob</code> 을 복사해 자르고 늘이고 뒤집어 바뀌지 않는 <code>ImageBitmap</code> 으로 만든다. <code>video</code> 요소는 비디오를
 * 지원하지 않으므로 쓸 수 있는 원본이 아니다. 프라미스는 나중의 작업이 아니라 호출 안에서 확정되며(반응은 다음 마이크로태스크 검사점에 실행된다), 블롭 바이트의
 * 디코딩은 스크립트 스레드에서 한다.
 */
final class OffscreenBindings {

    /**
     * The values of the enumeration <code>OffscreenRenderingContextId</code>.
     * <p>
     * 열거형 <code>OffscreenRenderingContextId</code> 의 값.
     */
    private static final String[] CONTEXT_IDS = { "2d", "bitmaprenderer", "webgl", "webgl2", "webgpu" };

    /**
     * The owner of the bindings.
     * <p>
     * 연결의 소유자.
     */
    private final CanvasBindings owner;

    /**
     * The script context that owns the objects.
     * <p>
     * 객체를 소유한 스크립트 문맥.
     */
    private final CanvasHost host;

    /**
     * The DOM bridge.
     * <p>
     * DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm.
     * <p>
     * realm 객체.
     */
    private final JsRealm realm;

    /**
     * The prototype of <code>OffscreenCanvas</code> objects.
     * <p>
     * <code>OffscreenCanvas</code> 객체의 프로토타입.
     */
    private JsObject canvasProto;

    /**
     * The options of one <code>createImageBitmap()</code> call.
     * <p>
     * <code>createImageBitmap()</code> 호출 하나의 옵션.
     */
    private static final class BitmapOptions {
        /**
         * True for <code>imageOrientation: "flipY"</code>.
         * <p>
         * <code>imageOrientation: "flipY"</code> 이면 true.
         */
        boolean flipY;

        /**
         * The requested width, or -1.
         * <p>
         * 요청한 너비. 없으면 -1.
         */
        long resizeWidth = -1;

        /**
         * The requested height, or -1.
         * <p>
         * 요청한 높이. 없으면 -1.
         */
        long resizeHeight = -1;

        /**
         * The interpolation, a constant of {@link BitmapOps}.
         * <p>
         * 보간. {@link BitmapOps} 의 상수.
         */
        int quality = BitmapOps.LOW;
    }

    /**
     * Creates the bindings.
     * <p>
     * 연결을 만든다.
     *
     * @param owner the owner / 소유자
     * @param host  the script context / 스크립트 문맥
     */
    OffscreenBindings(CanvasBindings owner, CanvasHost host) {
        this.owner = owner;
        this.host = host;
        this.bridge = host.bridge();
        this.realm = host.realm();
    }

    /**
     * Returns the offscreen canvas of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 offscreen 캔버스를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the value / 값
     */
    private static OffscreenCanvasValue canvas(JsValue self) {
        OffscreenCanvasValue v = (OffscreenCanvasValue) DomBridge.targetOf(self, OffscreenCanvasValue.class);
        if (v == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return v;
    }

    /**
     * Converts a value as WebIDL <code>[EnforceRange] unsigned long long</code> does, limited to what an <code>int</code> bitmap size can hold: NaN,
     * the infinities and values outside the range are a <code>TypeError</code>, a fraction is cut off, and a value above the largest
     * <code>int</code> is the largest <code>int</code> (the bitmap is then too large and stays empty).
     * <p>
     * 값을 WebIDL <code>[EnforceRange] unsigned long long</code> 처럼 변환하되 <code>int</code> 비트맵 크기가 담을 수 있는 범위로 한정한다: NaN, 무한대, 범위 밖의 값은
     * <code>TypeError</code>, 소수부는 버리고, 가장 큰 <code>int</code> 보다 큰 값은 가장 큰 <code>int</code> 이다(그러면 비트맵이 너무 커서 비어 있게 된다).
     *
     * @param v      the value / 값
     * @param member the member for the message / 메시지에 쓸 멤버
     * @return the number / 숫자
     */
    private static int enforceSize(JsValue v, String member) {
        double d = Ops.toNumber(v);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError("Failed to " + member + ": Value is not of type 'unsigned long long'.");
        }
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        if (d < 0 || d > 9007199254740991.0) {
            throw JsException.typeError("Failed to " + member + ": Value is outside the 'unsigned long long' value range.");
        }
        return d > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) d;
    }

    /**
     * Installs the interfaces and the global function.
     * <p>
     * 인터페이스와 전역 함수를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        installCanvas();
        installBitmap();
        g.defineData("createImageBitmap", realm.newFunction("createImageBitmap", 1, new NativeFunction.Call() {
            /**
             * Starts <code>createImageBitmap()</code>; every failure, also a wrong argument, is a rejected promise.
             * <p>
             * <code>createImageBitmap()</code> 을 시작한다. 잘못된 인수를 포함한 모든 실패는 reject 된 프라미스이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    return createBitmap(args);
                } catch (JsException e) {
                    return CanvasPromises.rejected(realm, e.getValue(realm));
                } catch (DomException e) {
                    return CanvasPromises.rejected(realm, bridge.domError(e).getValue(realm));
                }
            }
        }), true, true, true);
    }

    /**
     * Installs the interface of offscreen canvases: the constructor, the size attributes, the context, the transfer to a bitmap and the export to
     * a blob.
     * <p>
     * offscreen 캔버스의 인터페이스를 설치한다: 생성자, 크기 속성, 컨텍스트, 비트맵으로의 전달, 블롭으로의 내보내기.
     */
    private void installCanvas() {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("OffscreenCanvas", "EventTarget", OffscreenCanvasValue.class, new String[0]);
        canvasProto = info.getPrototype();
        bridge.getEventHandlers().define(canvasProto, OffscreenCanvasValue.class, new String[] { "contextlost", "contextrestored" });
        NativeFunction ctor = new NativeFunction(realm, bridge.getInterface("EventTarget").getConstructor(), "OffscreenCanvas", 2,
                new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'OffscreenCanvas': Please use the 'new' operator, this DOM object constructor cannot be "
                        + "called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an offscreen canvas of a size.
             * <p>
             * 크기가 정해진 offscreen 캔버스를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the width and the height / 인수: 너비와 높이
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to construct 'OffscreenCanvas': 2 arguments required, but only " + args.length + " present.");
                }
                int w = enforceSize(args[0], "construct 'OffscreenCanvas'");
                int h = enforceSize(args[1], "construct 'OffscreenCanvas'");
                CanvasSurface s = new CanvasSurface(w, h, null);
                owner.track(s);
                HostObject wrapper = bridge.wrap(new OffscreenCanvasValue(s, null));
                wrapper.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, canvasProto));
                return wrapper;
            }
        });
        bridge.setConstructor("OffscreenCanvas", ctor);
        installSize("width", true);
        installSize("height", false);
        method(canvasProto, "getContext", 1, new NativeFunction.Call() {
            /**
             * Returns the 2D context, creating it at the first call; every other context type answers null.
             * <p>
             * 2D 컨텍스트를 반환하며 처음 부를 때 만든다. 다른 모든 컨텍스트 형식은 null 로 답한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the context type and the settings / 인수: 컨텍스트 형식과 설정
             * @return the context or null / 컨텍스트 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                OffscreenCanvasValue v = canvas(self);
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'getContext' on 'OffscreenCanvas': 1 argument required, but only 0 present.");
                }
                String id = Ops.toString(args[0]);
                boolean known = false;
                // 열거형에 없는 이름은 TypeError 이다.
                for (int i = 0; i < CONTEXT_IDS.length; i++) {
                    known |= CONTEXT_IDS[i].equals(id);
                }
                if (!known) {
                    throw JsException.typeError("Failed to execute 'getContext' on 'OffscreenCanvas': The provided value '" + id
                            + "' is not a valid enum value of type OffscreenRenderingContextId.");
                }
                CanvasSurface s = v.surface();
                if (s == null) {
                    throw bridge.domError(DomException.invalidState("Cannot get a context from an OffscreenCanvas that has been transferred."));
                }
                // 2d 만 지원한다. bitmaprenderer, webgl, webgl2, webgpu 는 null 이다.
                if (!"2d".equals(id)) {
                    return Js.NULL;
                }
                synchronized (s) {
                    boolean fresh = !s.hasContext();
                    // 컨텍스트 속성은 처음 컨텍스트를 만들 때만 읽는다. 오류가 나면 컨텍스트를 만들지 않는다.
                    if (fresh) {
                        ContextSettings.apply(s, args.length > 1 ? args[1] : Js.UNDEFINED);
                    }
                    Context2D ctx = s.getContext();
                    // 요소가 없으므로 상대 글꼴 크기는 기본 글꼴(10px)을 기준으로 삼는다.
                    if (fresh) {
                        ctx.setElementStyle(10, 0xFF000000);
                    }
                    return bridge.toJs(ctx);
                }
            }
        });
        method(canvasProto, "transferToImageBitmap", 0, new NativeFunction.Call() {
            /**
             * Hands the picture over as an <code>ImageBitmap</code> and leaves a blank bitmap of the same size.
             * <p>
             * 그림을 <code>ImageBitmap</code> 으로 넘기고 같은 크기의 빈 비트맵을 남긴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the bitmap / 비트맵
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                OffscreenCanvasValue v = canvas(self);
                CanvasSurface s = v.surface();
                if (s == null) {
                    throw bridge.domError(DomException.invalidState("Cannot transfer an ImageBitmap from a detached OffscreenCanvas."));
                }
                synchronized (s) {
                    // 컨텍스트가 없는 캔버스(context mode none)에서는 넘길 그림이 없다.
                    if (!s.hasContext()) {
                        throw bridge.domError(DomException.invalidState("Cannot transfer an ImageBitmap from an OffscreenCanvas without a context."));
                    }
                    CanvasImage pixels = s.image(true);
                    if (pixels == null) {
                        throw bridge.domError(DomException.invalidState("Cannot transfer an ImageBitmap from an OffscreenCanvas with no pixels."));
                    }
                    s.blank();
                    return bridge.toJs(new ImageBitmapValue(pixels));
                }
            }
        });
        method(canvasProto, "convertToBlob", 0, new NativeFunction.Call() {
            /**
             * Encodes the picture now and returns a promise of a <code>Blob</code>.
             * <p>
             * 그림을 지금 인코딩하고 <code>Blob</code> 의 프라미스를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the options <code>type</code> and <code>quality</code> / 인수: 옵션 <code>type</code>, <code>quality</code>
             * @return the promise / 프라미스
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    return convertToBlob(canvas(self), Js.arg(args, 0));
                } catch (JsException e) {
                    return CanvasPromises.rejected(realm, e.getValue(realm));
                }
            }
        });
    }

    /**
     * Encodes an offscreen canvas for <code>convertToBlob()</code>.
     * <p>
     * <code>convertToBlob()</code> 을 위해 offscreen 캔버스를 인코딩한다.
     *
     * @param v       the canvas / 캔버스
     * @param options the options argument / 옵션 인수
     * @return the promise / 프라미스
     */
    private JsValue convertToBlob(OffscreenCanvasValue v, JsValue options) {
        // 옵션은 사전이므로 객체, undefined, null 만 받는다.
        if (!options.isUndefined() && !options.isNull() && !options.isObject()) {
            throw JsException.typeError("Failed to execute 'convertToBlob' on 'OffscreenCanvas': The provided value is not of type 'ImageEncodeOptions'.");
        }
        String type = "image/png";
        double quality = Double.NaN;
        if (options.isObject()) {
            JsValue t = ((JsObject) options).get("type");
            JsValue q = ((JsObject) options).get("quality");
            if (!t.isUndefined()) {
                type = Ops.toString(t);
            }
            // 품질은 숫자일 때만 쓴다.
            if (!q.isUndefined()) {
                quality = Ops.toNumber(q);
            }
        }
        CanvasSurface s = v.surface();
        if (s == null) {
            return CanvasPromises.rejected(realm, bridge.domError(DomException.invalidState("The OffscreenCanvas has been transferred.")).getValue(realm));
        }
        try {
            CanvasEncoder.Encoded enc = owner.encodeSurface(s, type, quality);
            // 화소가 없으면 IndexSizeError 로 거부한다.
            if (enc == null) {
                return CanvasPromises.rejected(realm, bridge.domError(DomException.indexSize("The OffscreenCanvas has no pixels.")).getValue(realm));
            }
            JsValue blob = owner.makeBlob(enc);
            if (blob.isNull()) {
                return CanvasPromises.rejected(realm, bridge.domError(new DomException("EncodingError", (short) 0, "Blob is not available.")).getValue(realm));
            }
            return CanvasPromises.resolved(realm, blob);
        } catch (JsException e) {
            return CanvasPromises.rejected(realm, e.getValue(realm));
        }
    }

    /**
     * Installs the <code>width</code> or <code>height</code> accessor of <code>OffscreenCanvas</code>.
     * <p>
     * <code>OffscreenCanvas</code> 의 <code>width</code> 또는 <code>height</code> 접근자를 설치한다.
     *
     * @param name  <code>width</code> or <code>height</code> / <code>width</code> 또는 <code>height</code>
     * @param width true for the width / 너비이면 true
     */
    private void installSize(final String name, final boolean width) {
        canvasProto.defineAccessor(name, realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Returns the size of the bitmap.
             * <p>
             * 비트맵의 크기를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the size / 크기
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                OffscreenCanvasValue v = canvas(self);
                return Js.num(width ? v.getWidth() : v.getHeight());
            }
        }), realm.newFunction("set " + name, 1, new NativeFunction.Call() {
            /**
             * Replaces the bitmap by a transparent one of the new size and resets the context.
             * <p>
             * 비트맵을 새 크기의 투명한 것으로 바꾸고 컨텍스트를 초기화한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the size / 인수: 크기
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                OffscreenCanvasValue v = canvas(self);
                int size = enforceSize(Js.arg(args, 0), "set '" + name + "' on 'OffscreenCanvas'");
                CanvasSurface s = v.surface();
                if (s == null) {
                    throw bridge.domError(DomException.invalidState("Cannot resize an OffscreenCanvas that has been transferred."));
                }
                synchronized (s) {
                    s.resize(width ? size : s.getWidth(), width ? s.getHeight() : size);
                }
                return Js.UNDEFINED;
            }
        }), true, true);
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
     * Installs the interface of image bitmaps: the size attributes and the close method (a bitmap cannot be constructed by a script).
     * <p>
     * 이미지 비트맵의 인터페이스를 설치한다: 크기 속성과 close 메서드(비트맵은 스크립트가 생성할 수 없다).
     */
    private void installBitmap() {
        bridge.defineInterface("ImageBitmap", null, ImageBitmapValue.class, new String[] { "a:width", "a:height", "m:close()" });
    }

    /**
     * Makes an offscreen canvas for a canvas element (<code>transferControlToOffscreen()</code>).
     * <p>
     * canvas 요소를 위한 offscreen 캔버스를 만든다(<code>transferControlToOffscreen()</code>).
     *
     * @param e the canvas element / canvas 요소
     * @return the offscreen canvas / offscreen 캔버스
     */
    JsValue transferControl(Element e) {
        try {
            CanvasSurface s = owner.makePlaceholder(e);
            return bridge.toJs(new OffscreenCanvasValue(s, e));
        } catch (DomException ex) {
            throw bridge.domError(ex);
        }
    }

    /**
     * Reads the value of an enumeration member of a dictionary: <code>undefined</code> is the default, a value outside the list is a
     * <code>TypeError</code>.
     * <p>
     * 사전의 열거형 멤버 값을 읽는다: <code>undefined</code> 는 기본값이고 목록에 없는 값은 <code>TypeError</code> 이다.
     *
     * @param d       the dictionary / 사전
     * @param member  the member name / 멤버 이름
     * @param allowed the allowed values / 허용된 값
     * @param type    the enumeration name for the message / 메시지에 쓸 열거형 이름
     * @return the value, or null when the member is undefined / 값. 멤버가 undefined 이면 null
     */
    private static String enumMember(JsObject d, String member, String[] allowed, String type) {
        JsValue v = d.get(member);
        if (v.isUndefined()) {
            return null;
        }
        String s = Ops.toString(v);
        // 허용된 값이면 그대로 돌려준다.
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(s)) {
                return s;
            }
        }
        throw JsException.typeError("Failed to execute 'createImageBitmap': The provided value '" + s + "' is not a valid enum value of type " + type + ".");
    }

    /**
     * Converts a dictionary member as <code>[EnforceRange] unsigned long</code>.
     * <p>
     * 사전 멤버를 <code>[EnforceRange] unsigned long</code> 으로 변환한다.
     *
     * @param d      the dictionary / 사전
     * @param member the member name / 멤버 이름
     * @return the value, or -1 when the member is undefined / 값. 멤버가 undefined 이면 -1
     */
    private static long unsignedLong(JsObject d, String member) {
        JsValue v = d.get(member);
        if (v.isUndefined()) {
            return -1;
        }
        double n = Ops.toNumber(v);
        if (Double.isNaN(n) || Double.isInfinite(n)) {
            throw JsException.typeError("Failed to execute 'createImageBitmap': Value is not of type 'unsigned long'.");
        }
        n = n < 0 ? Math.ceil(n) : Math.floor(n);
        if (n < 0 || n > 4294967295.0) {
            throw JsException.typeError("Failed to execute 'createImageBitmap': Value is outside the 'unsigned long' value range.");
        }
        return (long) n;
    }

    /**
     * Reads the <code>ImageBitmapOptions</code> dictionary.
     * <p>
     * <code>ImageBitmapOptions</code> 사전을 읽는다.
     *
     * @param v the argument / 인수
     * @return the options / 옵션
     */
    private static BitmapOptions options(JsValue v) {
        BitmapOptions o = new BitmapOptions();
        if (v == null || v.isUndefined() || v.isNull()) {
            return o;
        }
        if (!v.isObject()) {
            throw JsException.typeError("Failed to execute 'createImageBitmap': The provided value is not of type 'ImageBitmapOptions'.");
        }
        JsObject d = (JsObject) v;
        // "none" 은 이전 이름이며 from-image 와 같다.
        String orientation = enumMember(d, "imageOrientation", new String[] { "from-image", "flipY", "none" }, "ImageOrientation");
        o.flipY = "flipY".equals(orientation);
        enumMember(d, "premultiplyAlpha", new String[] { "none", "premultiply", "default" }, "PremultiplyAlpha");
        enumMember(d, "colorSpaceConversion", new String[] { "none", "default" }, "ColorSpaceConversion");
        o.resizeWidth = unsignedLong(d, "resizeWidth");
        o.resizeHeight = unsignedLong(d, "resizeHeight");
        String quality = enumMember(d, "resizeQuality", new String[] { "pixelated", "low", "medium", "high" }, "ResizeQuality");
        if (quality != null) {
            o.quality = BitmapOps.quality(quality);
        }
        return o;
    }

    /**
     * Runs <code>createImageBitmap()</code>: the arity and the options are checked, the source is copied, cropped, scaled and flipped, and the
     * promise is fulfilled with the new <code>ImageBitmap</code>.
     * <p>
     * <code>createImageBitmap()</code> 을 실행한다: 인수 개수와 옵션을 검사하고, 원본을 복사해 자르고 늘이고 뒤집고, 새 <code>ImageBitmap</code> 으로 프라미스를
     * 이행한다.
     *
     * @param args the arguments / 인수
     * @return the promise / 프라미스
     */
    private JsValue createBitmap(JsValue[] args) {
        if (args.length < 1) {
            throw JsException.typeError("Failed to execute 'createImageBitmap': 1 argument required, but only 0 present.");
        }
        boolean crop = args.length >= 5;
        if (args.length == 3 || args.length == 4 || args.length > 6) {
            throw JsException.typeError("Failed to execute 'createImageBitmap': Valid arities are: [1, 2, 5, 6], but " + args.length + " arguments provided.");
        }
        int sx = 0;
        int sy = 0;
        int sw = 0;
        int sh = 0;
        // 자르기 인수가 있으면 정수 네 개로 읽는다.
        if (crop) {
            sx = Ops.toInt32(args[1]);
            sy = Ops.toInt32(args[2]);
            sw = Ops.toInt32(args[3]);
            sh = Ops.toInt32(args[4]);
        }
        BitmapOptions options = options(crop ? Js.arg(args, 5) : Js.arg(args, 1));
        if (crop && (sw == 0 || sh == 0)) {
            throw JsException.rangeError("Failed to execute 'createImageBitmap': The crop rect width or height is 0.");
        }
        if (options.resizeWidth == 0 || options.resizeHeight == 0) {
            throw bridge.domError(DomException.invalidState("Failed to execute 'createImageBitmap': The resize width or height is 0."));
        }
        CanvasImage src;
        try {
            src = owner.bitmapSource(args[0]);
        } catch (DomException e) {
            throw bridge.domError(e);
        }
        double rx = 0;
        double ry = 0;
        double rw = src.getWidth();
        double rh = src.getHeight();
        // 자르기 사각형은 음수 너비와 높이면 (sx, sy) 의 왼쪽과 위쪽에 놓인다.
        if (crop) {
            rx = Math.min((double) sx, (double) sx + sw);
            ry = Math.min((double) sy, (double) sy + sh);
            rw = Math.abs((double) sw);
            rh = Math.abs((double) sh);
        }
        long ow = BitmapOps.outputWidth(rw, rh, options.resizeWidth, options.resizeHeight);
        long oh = BitmapOps.outputHeight(rw, rh, options.resizeWidth, options.resizeHeight);
        if (ow < 1 || oh < 1 || !CanvasLimits.fits(ow, oh)) {
            throw bridge.domError(DomException.invalidState("Failed to execute 'createImageBitmap': The output size " + ow + "x" + oh
                    + " is empty or exceeds the supported range."));
        }
        int[] pixels = BitmapOps.render(src, rx, ry, rw, rh, (int) ow, (int) oh, options.flipY, options.quality);
        ImageBitmapValue bitmap = new ImageBitmapValue(new CanvasImage((int) ow, (int) oh, pixels, src.isOriginClean()));
        return CanvasPromises.resolved(realm, bridge.toJs(bitmap));
    }
}
