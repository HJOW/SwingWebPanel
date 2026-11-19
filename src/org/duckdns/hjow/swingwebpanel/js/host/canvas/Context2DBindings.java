package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.DomMatrix;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.MatrixBindings;

/**
 * The script interface of <code>CanvasRenderingContext2D</code> (plan item P20-05): the attributes and methods of the interface on its
 * prototype, <code>CanvasGradient</code> and <code>CanvasPattern</code> creation, <code>ImageData</code> access and text metrics. The members
 * are thin: they convert the arguments the way WebIDL does (<code>unrestricted double</code> keeps NaN and infinities, <code>double</code> throws
 * a <code>TypeError</code> for them, missing required arguments are a <code>TypeError</code>), hold the monitor of the surface and call the
 * {@link Context2D} engine, whose {@link DomException}s become JavaScript <code>DOMException</code>s. <code>filter</code> accepts <code>none</code> or a
 * list of CSS filter functions (see {@link org.duckdns.hjow.swingwebpanel.render.CanvasFilters}). The text attributes <code>letterSpacing</code> and
 * <code>wordSpacing</code> change the layout, <code>fontKerning = "normal"</code> asks for kerning, <code>fontStretch</code>,
 * <code>fontVariantCaps</code>, <code>textRendering</code> and <code>lang</code> are only kept as state. Not implemented, and not defined on the
 * prototype: hit regions and <code>scrollPathIntoView</code> (removed from the specification).
 * <p>
 * <code>CanvasRenderingContext2D</code> 의 스크립트 인터페이스(계획 P20-05): 프로토타입의 속성과 메서드, <code>CanvasGradient</code> 와
 * <code>CanvasPattern</code> 생성, <code>ImageData</code> 접근, 텍스트 측정. 멤버는 얇다: WebIDL 처럼 인수를 변환하고(<code>unrestricted double</code> 은
 * NaN 과 무한대를 유지하고 <code>double</code> 은 이들에 <code>TypeError</code> 를 던지며 필수 인수가 없으면 <code>TypeError</code>), 표면의 모니터를 잡고,
 * {@link Context2D} 엔진을 부르며, 엔진의 {@link DomException} 은 JavaScript <code>DOMException</code> 이 된다. <code>filter</code> 는 <code>none</code> 이나 CSS 필터 함수
 * 목록을 받는다({@link org.duckdns.hjow.swingwebpanel.render.CanvasFilters} 참고). 텍스트 속성 <code>letterSpacing</code> 과
 * <code>wordSpacing</code> 은 배치를 바꾸고, <code>fontKerning = "normal"</code> 은 커닝을 요청하며, <code>fontStretch</code>, <code>fontVariantCaps</code>,
 * <code>textRendering</code>, <code>lang</code> 은 상태로만 보관한다. 구현하지 않아 프로토타입에 정의하지 않은 것: 히트 영역과 <code>scrollPathIntoView</code>(규격에서 삭제됨).
 */
final class Context2DBindings {

    /**
     * The owner of the bindings.
     * <p>
     * 연결의 소유자.
     */
    private final CanvasBindings owner;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final CanvasHost host;

    /**
     * The DOM bridge.
     * <p>
     * DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm of the page.
     * <p>
     * 페이지의 realm 객체.
     */
    private final JsRealm realm;

    /**
     * A member that runs on a context with the monitor of its surface held.
     * <p>
     * 컨텍스트에서 그 표면의 모니터를 잡고 실행되는 멤버.
     */
    private abstract class Op implements NativeFunction.Call {
        /**
         * Finds the context of the receiver, runs the member under the monitor and converts engine exceptions.
         * <p>
         * 수신자의 컨텍스트를 찾아 모니터를 잡고 멤버를 실행하며 엔진 예외를 변환한다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the result / 결과
         */
        public final JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            Context2D c = context(self);
            synchronized (c.getSurface()) {
                try {
                    return run(c, args);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        }

        /**
         * Does the work of the member.
         * <p>
         * 멤버의 일을 한다.
         *
         * @param c    the context / 컨텍스트
         * @param args arguments / 인수
         * @return the result / 결과
         */
        abstract JsValue run(Context2D c, JsValue[] args);
    }

    /**
     * Creates the bindings.
     * <p>
     * 연결을 만든다.
     *
     * @param owner the owner / 소유자
     * @param host  the page / 페이지
     */
    Context2DBindings(CanvasBindings owner, CanvasHost host) {
        this.owner = owner;
        this.host = host;
        this.bridge = host.bridge();
        this.realm = host.realm();
    }

    /**
     * Returns the context of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 컨텍스트를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the context / 컨텍스트
     */
    private static Context2D context(JsValue self) {
        Context2D c = (Context2D) DomBridge.targetOf(self, Context2D.class);
        if (c == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return c;
    }

    /**
     * Throws the TypeError of missing required arguments.
     * <p>
     * 필수 인수가 없을 때의 TypeError 를 던진다.
     *
     * @param args     the arguments / 인수
     * @param required the number required / 필요한 수
     * @param member   the member name / 멤버 이름
     */
    private static void need(JsValue[] args, int required, String member) {
        if (args.length < required) {
            throw JsException.typeError("Failed to execute '" + member + "' on 'CanvasRenderingContext2D': " + required
                    + " argument" + (required == 1 ? "" : "s") + " required, but only " + args.length + " present.");
        }
    }

    /**
     * Converts an argument to an <code>unrestricted double</code>.
     * <p>
     * 인수를 <code>unrestricted double</code> 로 변환한다.
     *
     * @param args arguments / 인수
     * @param i    the position / 위치
     * @return the number / 숫자
     */
    private static double num(JsValue[] args, int i) {
        return Ops.toNumber(Js.arg(args, i));
    }

    /**
     * Converts an argument to a <code>double</code>: NaN and infinities are a TypeError.
     * <p>
     * 인수를 <code>double</code> 로 변환한다. NaN 과 무한대는 TypeError 이다.
     *
     * @param args   arguments / 인수
     * @param i      the position / 위치
     * @param member the member name / 멤버 이름
     * @return the number / 숫자
     */
    private static double finiteNum(JsValue[] args, int i, String member) {
        double d = Ops.toNumber(Js.arg(args, i));
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError("Failed to execute '" + member + "' on 'CanvasRenderingContext2D': The provided double value is non-finite.");
        }
        return d;
    }

    /**
     * Converts an argument to a string.
     * <p>
     * 인수를 문자열로 변환한다.
     *
     * @param args arguments / 인수
     * @param i    the position / 위치
     * @return the string / 문자열
     */
    private static String str(JsValue[] args, int i) {
        return Ops.toString(Js.arg(args, i));
    }

    /**
     * Reads the optional <code>maxWidth</code> argument of <code>fillText</code> and <code>strokeText</code>: NaN when it was left out.
     * A <code>maxWidth</code> that was given as NaN also reads as NaN, so the caller tells the two apart by looking at the arguments.
     * <p>
     * <code>fillText</code> 와 <code>strokeText</code> 의 선택 인수 <code>maxWidth</code> 를 읽는다. 생략했으면 NaN 이다. NaN 으로 준 <code>maxWidth</code> 도
     * NaN 으로 읽히므로 호출자는 인수를 보고 둘을 구별한다.
     *
     * @param args arguments / 인수
     * @return the number or NaN / 숫자 또는 NaN
     */
    private static double maxWidth(JsValue[] args) {
        return args.length > 3 && !args[3].isUndefined() ? num(args, 3) : Double.NaN;
    }

    /**
     * Reads a fill rule argument (<code>nonzero</code> or <code>evenodd</code>); anything else is a TypeError.
     * <p>
     * 채움 규칙 인수(<code>nonzero</code> 또는 <code>evenodd</code>)를 읽는다. 다른 값은 TypeError 이다.
     *
     * @param args   arguments / 인수
     * @param i      the position / 위치
     * @param member the member name / 멤버 이름
     * @return true for even-odd / 짝홀이면 true
     */
    private static boolean fillRule(JsValue[] args, int i, String member) {
        if (args.length <= i || args[i].isUndefined()) {
            return false;
        }
        String v = Ops.toString(args[i]);
        if ("evenodd".equals(v)) {
            return true;
        }
        if ("nonzero".equals(v)) {
            return false;
        }
        throw JsException.typeError("Failed to execute '" + member + "' on 'CanvasRenderingContext2D': The provided value '" + v
                + "' is not a valid enum value of type CanvasFillRule.");
    }

    /**
     * Returns the <code>Path2D</code> that is the first argument of <code>fill</code>, <code>stroke</code>, <code>clip</code>,
     * <code>isPointInPath</code> or <code>isPointInStroke</code>, or null when the first argument is something else that these methods accept
     * (a fill rule, a coordinate). Any other object is a TypeError.
     * <p>
     * <code>fill</code>, <code>stroke</code>, <code>clip</code>, <code>isPointInPath</code>, <code>isPointInStroke</code> 의 첫 인수인 <code>Path2D</code> 를
     * 반환한다. 첫 인수가 이 메서드들이 받는 다른 것(채움 규칙, 좌표)이면 null. 그 밖의 객체는 TypeError 이다.
     *
     * @param args   arguments / 인수
     * @param member the member name / 멤버 이름
     * @return the path or null / 경로 또는 null
     */
    private static Path2DValue pathArgument(JsValue[] args, String member) {
        if (args.length == 0 || !args[0].isObject()) {
            return null;
        }
        Path2DValue p = (Path2DValue) DomBridge.targetOf(args[0], Path2DValue.class);
        if (p == null && !(args[0] instanceof org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject)) {
            throw JsException.typeError("Failed to execute '" + member + "' on 'CanvasRenderingContext2D': parameter 1 is not of type 'Path2D'.");
        }
        return p;
    }

    /**
     * Defines a method that runs under the monitor of the surface.
     * <p>
     * 표면의 모니터를 잡고 실행되는 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param op     the behaviour / 동작
     */
    private void method(JsObject proto, String name, int length, Op op) {
        proto.defineData(name, realm.newFunction(name, length, op), true, true, true);
    }

    /**
     * Defines an accessor property whose getter and setter run under the monitor of the surface.
     * <p>
     * getter 와 setter 가 표면의 모니터를 잡고 실행되는 접근자 프로퍼티를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter / getter
     * @param set   the setter / setter
     */
    private void accessor(JsObject proto, String name, Op get, Op set) {
        proto.defineAccessor(name, realm.newFunction("get " + name, 0, get), realm.newFunction("set " + name, 1, set), true, true);
    }

    /**
     * Converts a style value to the engine's style object (an ARGB color, a gradient or a pattern), or null if it must be ignored.
     * <p>
     * 스타일 값을 엔진의 스타일 객체(ARGB 색, 그라디언트, 패턴)로 변환한다. 무시해야 하면 null.
     *
     * @param c the context / 컨텍스트
     * @param v the value / 값
     * @return the style or null / 스타일 또는 null
     */
    private Object style(Context2D c, JsValue v) {
        Object g = DomBridge.targetOf(v, CanvasGradient.class);
        if (g != null) {
            return g;
        }
        Object p = DomBridge.targetOf(v, CanvasPattern.class);
        if (p != null) {
            return p;
        }
        return owner.parseColor(c, Ops.toString(v));
    }

    /**
     * Converts a style of the engine to a script value.
     * <p>
     * 엔진의 스타일을 스크립트 값으로 변환한다.
     *
     * @param style the style / 스타일
     * @return the value / 값
     */
    private JsValue styleValue(Object style) {
        if (style instanceof Integer) {
            return Js.str(CanvasColors.serialize(((Integer) style).intValue()));
        }
        return bridge.toJs(style);
    }

    /**
     * Reads the six numbers of a transformation matrix from a number list or from an object with the members a to f.
     * <p>
     * 변환 행렬의 숫자 여섯 개를 숫자 목록이나 a ~ f 멤버를 가진 객체에서 읽는다.
     *
     * @param args   arguments / 인수
     * @param member the member name / 멤버 이름
     * @return six numbers / 숫자 여섯 개
     */
    private double[] matrix(JsValue[] args, String member) {
        // 객체(DOMMatrix2DInit 사전 또는 다른 행렬) 하나이면 a ~ f 와 m11 형태의 멤버를 규격대로 읽는다.
        if (args.length == 1 && args[0].isObject()) {
            return MatrixBindings.fromInit2D(args[0]).to2DArray();
        }
        need(args, 6, member);
        double[] m = new double[6];
        // 숫자 여섯 개를 읽는다.
        for (int i = 0; i < 6; i++) {
            m[i] = num(args, i);
        }
        return m;
    }

    /**
     * Installs everything on the prototype of <code>CanvasRenderingContext2D</code> or of <code>OffscreenCanvasRenderingContext2D</code>.
     * <p>
     * <code>CanvasRenderingContext2D</code> 나 <code>OffscreenCanvasRenderingContext2D</code> 의 프로토타입에 모든 것을 설치한다.
     *
     * @param proto the prototype / 프로토타입
     * @param userInterface true for <code>CanvasRenderingContext2D</code>, which also has <code>drawFocusIfNeeded()</code> (the offscreen
     *                      context has no user interface) / <code>drawFocusIfNeeded()</code> 도 있는 <code>CanvasRenderingContext2D</code> 이면 true(offscreen
     *                      컨텍스트에는 사용자 인터페이스가 없다)
     */
    void install(JsObject proto, boolean userInterface) {
        installFilter(proto);
        installState(proto);
        installStyles(proto);
        installPaths(proto);
        installDrawing(proto);
        installPixels(proto);
        if (userInterface) {
            installFocus(proto);
        }
    }

    /**
     * Installs the state, transformation and compositing members.
     * <p>
     * 상태, 변환, 합성 멤버를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installState(JsObject proto) {
        method(proto, "save", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.save();
                return Js.UNDEFINED;
            }
        });
        method(proto, "restore", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.restore();
                return Js.UNDEFINED;
            }
        });
        method(proto, "reset", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.reset();
                return Js.UNDEFINED;
            }
        });
        method(proto, "scale", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 2, "scale");
                c.scale(num(a, 0), num(a, 1));
                return Js.UNDEFINED;
            }
        });
        method(proto, "rotate", 1, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 1, "rotate");
                c.rotate(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        method(proto, "translate", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 2, "translate");
                c.translate(num(a, 0), num(a, 1));
                return Js.UNDEFINED;
            }
        });
        method(proto, "transform", 6, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 6, "transform");
                c.transform(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5));
                return Js.UNDEFINED;
            }
        });
        method(proto, "setTransform", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                double[] m = a.length == 0 ? new double[] { 1, 0, 0, 1, 0, 0 } : matrix(a, "setTransform");
                c.setTransform(m[0], m[1], m[2], m[3], m[4], m[5]);
                return Js.UNDEFINED;
            }
        });
        // getTransform 은 현재 변환 행렬의 복사본인 DOMMatrix 를 반환한다(이후 컨텍스트의 변환이 바뀌어도 그 행렬은 바뀌지 않는다).
        method(proto, "getTransform", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                double[] m = c.getTransform();
                return bridge.wrap(new DomMatrix(m[0], m[1], m[2], m[3], m[4], m[5]));
            }
        });
        method(proto, "resetTransform", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.resetTransform();
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "canvas", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Object element = c.getSurface().getOwner();
                return element == null ? Js.NULL : bridge.toJs(element);
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "globalAlpha", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getGlobalAlpha());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setGlobalAlpha(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "globalCompositeOperation", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getGlobalCompositeOperation());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setGlobalCompositeOperation(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "imageSmoothingEnabled", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.bool(c.isImageSmoothingEnabled());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setImageSmoothingEnabled(Ops.toBoolean(Js.arg(a, 0)));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "imageSmoothingQuality", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getImageSmoothingQuality());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setImageSmoothingQuality(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        method(proto, "getContextAttributes", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                JsObject o = realm.newObject();
                o.createDataProperty("alpha", Js.bool(!c.getSurface().isOpaque()));
                o.createDataProperty("colorSpace", Js.str("srgb"));
                o.createDataProperty("desynchronized", Js.bool(c.getSurface().isDesynchronized()));
                o.createDataProperty("willReadFrequently", Js.bool(c.getSurface().isWillReadFrequently()));
                return o;
            }
        });
        method(proto, "isContextLost", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.FALSE;
            }
        });
    }

    /**
     * Installs the style, shadow, line and text attributes.
     * <p>
     * 스타일, 그림자, 선, 텍스트 속성을 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installStyles(JsObject proto) {
        accessor(proto, "fillStyle", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return styleValue(c.getFillStyle());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Object s = style(c, Js.arg(a, 0));
                if (s != null) {
                    c.setFillStyle(s);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "strokeStyle", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return styleValue(c.getStrokeStyle());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Object s = style(c, Js.arg(a, 0));
                if (s != null) {
                    c.setStrokeStyle(s);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "shadowColor", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(CanvasColors.serialize(c.getShadowColor()));
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Object s = owner.parseColor(c, str(a, 0));
                if (s instanceof Integer) {
                    c.setShadowColor(((Integer) s).intValue());
                }
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "shadowBlur", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getShadowBlur());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setShadowBlur(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "shadowOffsetX", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getShadowOffsetX());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setShadowOffsetX(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "shadowOffsetY", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getShadowOffsetY());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setShadowOffsetY(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "lineWidth", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getLineWidth());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLineWidth(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "lineCap", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getLineCap());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLineCap(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "lineJoin", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getLineJoin());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLineJoin(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "miterLimit", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getMiterLimit());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setMiterLimit(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "lineDashOffset", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.num(c.getLineDashOffset());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLineDashOffset(num(a, 0));
                return Js.UNDEFINED;
            }
        });
        method(proto, "setLineDash", 1, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 1, "setLineDash");
                JsValue[] list = Ops.createListFromArrayLike(a[0]);
                double[] d = new double[list.length];
                // 목록의 항목을 숫자로 바꾼다.
                for (int i = 0; i < d.length; i++) {
                    d[i] = Ops.toNumber(list[i]);
                }
                c.setLineDash(d);
                return Js.UNDEFINED;
            }
        });
        method(proto, "getLineDash", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                double[] d = c.getLineDash();
                JsValue[] out = new JsValue[d.length];
                // 숫자를 하나씩 JavaScript 값으로 바꿔 배열을 만든다.
                for (int i = 0; i < d.length; i++) {
                    out[i] = Js.num(d[i]);
                }
                return realm.newArray(out);
            }
        });
        accessor(proto, "font", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getFont().serialize());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                String text = str(a, 0);
                owner.refreshElementStyle(c, text);
                CanvasFont f = CanvasFont.parse(text, c.getBaseFontSize());
                if (f != null) {
                    c.setFont(f);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "textAlign", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getTextAlign());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setTextAlign(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "textBaseline", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getTextBaseline());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setTextBaseline(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "direction", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getDirection());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setDirection(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        installTextExtras(proto);
    }

    /**
     * Installs the <code>filter</code> attribute (<code>none</code> by default; an invalid value is ignored).
     * <p>
     * <code>filter</code> 속성을 설치한다(기본은 <code>none</code>. 올바르지 않은 값은 무시한다).
     *
     * @param proto the prototype / 프로토타입
     */
    private void installFilter(JsObject proto) {
        accessor(proto, "filter", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getFilter());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                // 상대 길이와 currentcolor 가 캔버스 요소의 글꼴 크기와 색을 보도록 먼저 읽어 둔다.
                owner.refreshElementStyle(c, "em currentcolor");
                c.setFilter(str(a, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the text attributes that came after the first canvas text API: <code>letterSpacing</code>, <code>wordSpacing</code>,
     * <code>fontKerning</code>, <code>fontStretch</code>, <code>fontVariantCaps</code>, <code>textRendering</code> and <code>lang</code>.
     * <p>
     * 처음의 캔버스 텍스트 API 뒤에 생긴 텍스트 속성을 설치한다: <code>letterSpacing</code>, <code>wordSpacing</code>, <code>fontKerning</code>,
     * <code>fontStretch</code>, <code>fontVariantCaps</code>, <code>textRendering</code>, <code>lang</code>.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installTextExtras(JsObject proto) {
        accessor(proto, "letterSpacing", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getLetterSpacing());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLetterSpacing(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "wordSpacing", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getWordSpacing());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setWordSpacing(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "fontKerning", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getFontKerning());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setFontKerning(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "fontStretch", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getFontStretch());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setFontStretch(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "fontVariantCaps", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getFontVariantCaps());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setFontVariantCaps(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "textRendering", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getTextRendering());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setTextRendering(str(a, 0));
                return Js.UNDEFINED;
            }
        });
        accessor(proto, "lang", new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                return Js.str(c.getLang());
            }
        }, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.setLang(str(a, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the path members.
     * <p>
     * 경로 멤버를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installPaths(JsObject proto) {
        method(proto, "beginPath", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.beginPath();
                return Js.UNDEFINED;
            }
        });
        method(proto, "closePath", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                c.closePath();
                return Js.UNDEFINED;
            }
        });
        method(proto, "moveTo", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 2, "moveTo");
                c.moveTo(num(a, 0), num(a, 1));
                return Js.UNDEFINED;
            }
        });
        method(proto, "lineTo", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 2, "lineTo");
                c.lineTo(num(a, 0), num(a, 1));
                return Js.UNDEFINED;
            }
        });
        method(proto, "quadraticCurveTo", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "quadraticCurveTo");
                c.quadraticCurveTo(num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                return Js.UNDEFINED;
            }
        });
        method(proto, "bezierCurveTo", 6, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 6, "bezierCurveTo");
                c.bezierCurveTo(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5));
                return Js.UNDEFINED;
            }
        });
        method(proto, "arcTo", 5, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 5, "arcTo");
                c.arcTo(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4));
                return Js.UNDEFINED;
            }
        });
        method(proto, "rect", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "rect");
                c.rect(num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                return Js.UNDEFINED;
            }
        });
        method(proto, "roundRect", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "roundRect");
                double x = num(a, 0);
                double y = num(a, 1);
                double w = num(a, 2);
                double h = num(a, 3);
                double[] r = CanvasBindings.radii(Js.arg(a, 4));
                if (r != null) {
                    c.roundRect(x, y, w, h, r);
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "arc", 5, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 5, "arc");
                c.arc(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), Ops.toBoolean(Js.arg(a, 5)));
                return Js.UNDEFINED;
            }
        });
        method(proto, "ellipse", 7, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 7, "ellipse");
                c.ellipse(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5), num(a, 6), Ops.toBoolean(Js.arg(a, 7)));
                return Js.UNDEFINED;
            }
        });
        method(proto, "fill", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Path2DValue p = pathArgument(a, "fill");
                if (p != null) {
                    c.fillPath(p, fillRule(a, 1, "fill"));
                } else {
                    c.fill(fillRule(a, 0, "fill"));
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "stroke", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Path2DValue p = pathArgument(a, "stroke");
                if (p != null) {
                    c.strokePath(p);
                } else {
                    c.stroke();
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "clip", 0, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Path2DValue p = pathArgument(a, "clip");
                if (p != null) {
                    c.clipPath(p, fillRule(a, 1, "clip"));
                } else {
                    c.clip(fillRule(a, 0, "clip"));
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "isPointInPath", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Path2DValue p = pathArgument(a, "isPointInPath");
                if (p != null) {
                    need(a, 3, "isPointInPath");
                    return Js.bool(c.isPointInPath(p, num(a, 1), num(a, 2), fillRule(a, 3, "isPointInPath")));
                }
                need(a, 2, "isPointInPath");
                return Js.bool(c.isPointInPath(num(a, 0), num(a, 1), fillRule(a, 2, "isPointInPath")));
            }
        });
        method(proto, "isPointInStroke", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                Path2DValue p = pathArgument(a, "isPointInStroke");
                if (p != null) {
                    need(a, 3, "isPointInStroke");
                    return Js.bool(c.isPointInStroke(p, num(a, 1), num(a, 2)));
                }
                need(a, 2, "isPointInStroke");
                return Js.bool(c.isPointInStroke(num(a, 0), num(a, 1)));
            }
        });
    }

    /**
     * Installs the user interface member <code>drawFocusIfNeeded()</code> (only the context of a canvas element has it).
     * <p>
     * 사용자 인터페이스 멤버 <code>drawFocusIfNeeded()</code> 를 설치한다(캔버스 요소의 컨텍스트에만 있다).
     *
     * @param proto the prototype / 프로토타입
     */
    private void installFocus(JsObject proto) {
        // drawFocusIfNeeded(element) 와 drawFocusIfNeeded(path, element): 인수 수로 형태를 가른다.
        method(proto, "drawFocusIfNeeded", 1, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 1, "drawFocusIfNeeded");
                Path2DValue p = null;
                JsValue elementArgument = a[0];
                int position = 1;
                if (a.length > 1) {
                    p = (Path2DValue) DomBridge.targetOf(a[0], Path2DValue.class);
                    if (p == null) {
                        throw JsException.typeError("Failed to execute 'drawFocusIfNeeded' on 'CanvasRenderingContext2D': parameter 1 is not of type 'Path2D'.");
                    }
                    elementArgument = a[1];
                    position = 2;
                }
                Element element = (Element) DomBridge.targetOf(elementArgument, Element.class);
                if (element == null) {
                    throw JsException.typeError("Failed to execute 'drawFocusIfNeeded' on 'CanvasRenderingContext2D': parameter " + position
                            + " is not of type 'Element'.");
                }
                owner.drawFocusIfNeeded(c, p, element);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the rectangle, text, image and gradient members.
     * <p>
     * 사각형, 텍스트, 이미지, 그라디언트 멤버를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installDrawing(JsObject proto) {
        method(proto, "fillRect", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "fillRect");
                c.fillRect(num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                return Js.UNDEFINED;
            }
        });
        method(proto, "strokeRect", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "strokeRect");
                c.strokeRect(num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                return Js.UNDEFINED;
            }
        });
        method(proto, "clearRect", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "clearRect");
                c.clearRect(num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                return Js.UNDEFINED;
            }
        });
        method(proto, "fillText", 3, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 3, "fillText");
                double maxWidth = maxWidth(a);
                if (maxWidth == maxWidth || a.length <= 3 || a[3].isUndefined()) {
                    c.fillText(str(a, 0), num(a, 1), num(a, 2), maxWidth);
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "strokeText", 3, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 3, "strokeText");
                double maxWidth = maxWidth(a);
                if (maxWidth == maxWidth || a.length <= 3 || a[3].isUndefined()) {
                    c.strokeText(str(a, 0), num(a, 1), num(a, 2), maxWidth);
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "measureText", 1, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 1, "measureText");
                return bridge.toJs(c.measureText(str(a, 0)));
            }
        });
        method(proto, "drawImage", 3, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                if (a.length != 3 && a.length != 5 && a.length != 9) {
                    if (a.length < 3) {
                        need(a, 3, "drawImage");
                    }
                    throw JsException.typeError("Failed to execute 'drawImage' on 'CanvasRenderingContext2D': Valid arities are: [3, 5, 9], but "
                            + a.length + " arguments provided.");
                }
                CanvasImage img = owner.imageSource(c, a[0]);
                double[] n = new double[a.length - 1];
                // 첫 인수 뒤의 숫자 인수를 읽는다.
                for (int i = 0; i < n.length; i++) {
                    n[i] = num(a, i + 1);
                }
                if (img == null) {
                    return Js.UNDEFINED;
                }
                double iw = img.getWidth();
                double ih = img.getHeight();
                if (a.length == 3) {
                    c.drawImage(img, 0, 0, iw, ih, n[0], n[1], iw, ih);
                } else if (a.length == 5) {
                    c.drawImage(img, 0, 0, iw, ih, n[0], n[1], n[2], n[3]);
                } else {
                    c.drawImage(img, n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7]);
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "createLinearGradient", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "createLinearGradient");
                return bridge.toJs(CanvasGradient.linear(finiteNum(a, 0, "createLinearGradient"), finiteNum(a, 1, "createLinearGradient"),
                        finiteNum(a, 2, "createLinearGradient"), finiteNum(a, 3, "createLinearGradient")));
            }
        });
        method(proto, "createRadialGradient", 6, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 6, "createRadialGradient");
                double[] v = new double[6];
                // 여섯 숫자는 모두 유한해야 한다.
                for (int i = 0; i < 6; i++) {
                    v[i] = finiteNum(a, i, "createRadialGradient");
                }
                if (v[2] < 0 || v[5] < 0) {
                    throw DomException.indexSize("The radius provided (" + (v[2] < 0 ? v[2] : v[5]) + ") is negative.");
                }
                return bridge.toJs(CanvasGradient.radial(v[0], v[1], v[2], v[3], v[4], v[5]));
            }
        });
        method(proto, "createConicGradient", 3, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 3, "createConicGradient");
                return bridge.toJs(CanvasGradient.conic(finiteNum(a, 0, "createConicGradient"), finiteNum(a, 1, "createConicGradient"),
                        finiteNum(a, 2, "createConicGradient")));
            }
        });
        method(proto, "createPattern", 2, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 2, "createPattern");
                CanvasImage img = owner.imageSource(c, a[0]);
                String rep = a[1].isNull() ? "repeat" : Ops.toString(a[1]);
                boolean rx;
                boolean ry;
                if ("repeat".equals(rep) || rep.length() == 0) {
                    rx = true;
                    ry = true;
                } else if ("repeat-x".equals(rep)) {
                    rx = true;
                    ry = false;
                } else if ("repeat-y".equals(rep)) {
                    rx = false;
                    ry = true;
                } else if ("no-repeat".equals(rep)) {
                    rx = false;
                    ry = false;
                } else {
                    throw DomException.syntax("The provided type ('" + rep + "') is not one of 'repeat', 'no-repeat', 'repeat-x', or 'repeat-y'.");
                }
                if (img == null) {
                    return Js.NULL;
                }
                return bridge.toJs(new CanvasPattern(img.getWidth(), img.getHeight(), img.getPixels(), rx, ry, img.isOriginClean()));
            }
        });
    }

    /**
     * Installs the pixel data members.
     * <p>
     * 화소 데이터 멤버를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installPixels(JsObject proto) {
        method(proto, "createImageData", 1, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 1, "createImageData");
                if (a[0].isObject()) {
                    ImageDataValue src = (ImageDataValue) DomBridge.targetOf(a[0], ImageDataValue.class);
                    if (src == null) {
                        throw JsException.typeError("Failed to execute 'createImageData' on 'CanvasRenderingContext2D': parameter 1 is not of type 'ImageData'.");
                    }
                    return owner.newImageData(src.getWidth(), src.getHeight());
                }
                need(a, 2, "createImageData");
                int sw = Ops.toInt32(a[0]);
                int sh = Ops.toInt32(a[1]);
                if (sw == 0 || sh == 0) {
                    throw DomException.indexSize("The source width or height is 0.");
                }
                return owner.newImageData(Math.abs(sw), Math.abs(sh));
            }
        });
        method(proto, "getImageData", 4, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 4, "getImageData");
                Context2D.RawImage raw = c.getImageData(Ops.toInt32(a[0]), Ops.toInt32(a[1]), Ops.toInt32(a[2]), Ops.toInt32(a[3]));
                return owner.newImageData(raw.width, raw.height, raw.data);
            }
        });
        method(proto, "putImageData", 3, new Op() {
            JsValue run(Context2D c, JsValue[] a) {
                need(a, 3, "putImageData");
                ImageDataValue v = (ImageDataValue) DomBridge.targetOf(a[0], ImageDataValue.class);
                if (v == null) {
                    throw JsException.typeError("Failed to execute 'putImageData' on 'CanvasRenderingContext2D': parameter 1 is not of type 'ImageData'.");
                }
                if (a.length > 3 && a.length < 7) {
                    need(a, 7, "putImageData");
                }
                int dx = Ops.toInt32(a[1]);
                int dy = Ops.toInt32(a[2]);
                int ix = 0;
                int iy = 0;
                int iw = v.getWidth();
                int ih = v.getHeight();
                // 변경 사각형이 주어지면 그 값을 쓴다.
                if (a.length >= 7) {
                    ix = Ops.toInt32(a[3]);
                    iy = Ops.toInt32(a[4]);
                    iw = Ops.toInt32(a[5]);
                    ih = Ops.toInt32(a[6]);
                }
                c.putImageData(v.bytes(), v.getWidth(), v.getHeight(), dx, dy, ix, iy, iw, ih);
                return Js.UNDEFINED;
            }
        });
    }
}
