package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.DomPoint;
import org.duckdns.hjow.swingwebpanel.dom.DomPointReadOnly;
import org.duckdns.hjow.swingwebpanel.dom.DomRect;
import org.duckdns.hjow.swingwebpanel.dom.DomRectList;
import org.duckdns.hjow.swingwebpanel.dom.DomRectReadOnly;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The geometry interfaces that the layout queries return (plan item P20-03, Geometry Interfaces Module): <code>DOMRectReadOnly</code>,
 * <code>DOMRect</code> (<code>x</code>, <code>y</code>, <code>width</code>, <code>height</code>, the edges <code>top</code>, <code>right</code>,
 * <code>bottom</code>, <code>left</code>, <code>toJSON()</code>, the static <code>fromRect()</code>, a constructor that takes the four
 * numbers), <code>DOMRectList</code> (what <code>getClientRects()</code> returns: <code>length</code>, <code>item()</code>, indexed access and
 * iteration), and <code>DOMPointReadOnly</code>/<code>DOMPoint</code> (<code>x</code>, <code>y</code>, <code>z</code>, <code>w</code>,
 * <code>toJSON()</code>, <code>fromPoint()</code>). Their instances are real instances of these interfaces, so
 * <code>rect instanceof DOMRect</code> is true. Matrix transforms (<code>DOMMatrix</code>, <code>DOMQuad</code>) are not part of it.
 * <p>
 * 레이아웃 조회가 돌려주는 기하 인터페이스(계획 P20-03, Geometry Interfaces Module): <code>DOMRectReadOnly</code>, <code>DOMRect</code>(<code>x</code>,
 * <code>y</code>, <code>width</code>, <code>height</code>, 가장자리 <code>top</code>, <code>right</code>, <code>bottom</code>, <code>left</code>,
 * <code>toJSON()</code>, 정적 <code>fromRect()</code>, 숫자 네 개를 받는 생성자), <code>DOMRectList</code>(<code>getClientRects()</code> 의 반환값:
 * <code>length</code>, <code>item()</code>, 위치 접근, 반복), <code>DOMPointReadOnly</code>/<code>DOMPoint</code>(<code>x</code>, <code>y</code>,
 * <code>z</code>, <code>w</code>, <code>toJSON()</code>, <code>fromPoint()</code>). 인스턴스는 이 인터페이스의 진짜 인스턴스이므로
 * <code>rect instanceof DOMRect</code> 가 참이다. 행렬 변환(<code>DOMMatrix</code>, <code>DOMQuad</code>)은 포함하지 않는다.
 */
public final class GeometryBindings {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private GeometryBindings() {
    }

    /**
     * Installs the interfaces and the classifier that maps the Java classes to them.
     * <p>
     * 인터페이스와 Java 클래스를 그것에 대응시키는 분류기를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the geometry interface of a Java object.
             * <p>
             * Java 객체의 기하 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof DomRect) {
                    return "DOMRect";
                } else if (o instanceof DomRectReadOnly) {
                    return "DOMRectReadOnly";
                } else if (o instanceof DomRectList) {
                    return "DOMRectList";
                } else if (o instanceof DomPoint) {
                    return "DOMPoint";
                } else if (o instanceof DomPointReadOnly) {
                    return "DOMPointReadOnly";
                }
                return null;
            }
        });
        installRects(bridge);
        installPoints(bridge);
    }

    /**
     * Installs the rectangle interfaces and the list.
     * <p>
     * 사각형 인터페이스와 목록을 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installRects(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo readOnly = bridge.defineInterface("DOMRectReadOnly", null, DomRectReadOnly.class,
                new String[] { "a:x", "a:y", "a:width", "a:height", "a:top", "a:right", "a:bottom", "a:left" });
        final DomBridge.InterfaceInfo writable = bridge.defineInterface("DOMRect", "DOMRectReadOnly", DomRect.class,
                new String[] { "a:x=", "a:y=", "a:width=", "a:height=" });
        installRectConstructor(bridge, readOnly, false);
        installRectConstructor(bridge, writable, true);
        readOnly.getPrototype().defineData("toJSON", realm.newFunction("toJSON", 0, new NativeFunction.Call() {
            /**
             * Returns a plain object with the eight numbers of the rectangle.
             * <p>
             * 사각형의 숫자 여덟 개를 가진 일반 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomRectReadOnly rect = (DomRectReadOnly) DomBridge.targetOf(self, DomRectReadOnly.class);
                if (rect == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                JsObject o = r.newObject();
                o.createDataProperty("x", Js.num(rect.getX()));
                o.createDataProperty("y", Js.num(rect.getY()));
                o.createDataProperty("width", Js.num(rect.getWidth()));
                o.createDataProperty("height", Js.num(rect.getHeight()));
                o.createDataProperty("top", Js.num(rect.getTop()));
                o.createDataProperty("right", Js.num(rect.getRight()));
                o.createDataProperty("bottom", Js.num(rect.getBottom()));
                o.createDataProperty("left", Js.num(rect.getLeft()));
                return o;
            }
        }), true, true, true);
        final DomBridge.InterfaceInfo list = bridge.defineInterface("DOMRectList", null, DomRectList.class,
                new String[] { "a:length", "m:item(int)" });
        bridge.setIndexed("DOMRectList", new IndexedObject.Source() {
            /**
             * Returns the number of rectangles.
             * <p>
             * 사각형 수를 반환한다.
             *
             * @param target the list / 목록
             * @return the length / 길이
             */
            public int length(Object target) {
                return ((DomRectList) target).getLength();
            }

            /**
             * Returns a rectangle.
             * <p>
             * 사각형 하나를 반환한다.
             *
             * @param target the list / 목록
             * @param index  the index / 위치
             * @return the rectangle / 사각형
             */
            public Object item(Object target, int index) {
                return ((DomRectList) target).item(index);
            }

            /**
             * The list has no named access.
             * <p>
             * 목록에는 이름 접근이 없다.
             *
             * @param target the list / 목록
             * @param name   the name / 이름
             * @return null / null
             */
            public Object named(Object target, String name) {
                return null;
            }
        });
        JsValue values = realm.getIntrinsic("%ArrayProto_values%");
        // 반복(for-of, 전개)이 되도록 배열의 values 와 forEach 를 연결한다.
        if (values != null && !values.isUndefined()) {
            list.getPrototype().defineBuiltin(WellKnownSymbols.ITERATOR, values);
        }
        list.getPrototype().defineBuiltin("forEach", realm.getArrayPrototype().get("forEach"));
    }

    /**
     * Installs the constructor and the static <code>fromRect</code> of a rectangle interface.
     * <p>
     * 사각형 인터페이스의 생성자와 정적 <code>fromRect</code> 를 설치한다.
     *
     * @param bridge   the bridge / 브리지
     * @param info     the interface / 인터페이스
     * @param writable true for <code>DOMRect</code>, false for <code>DOMRectReadOnly</code> / <code>DOMRect</code> 이면 true
     */
    private static void installRectConstructor(final DomBridge bridge, final DomBridge.InterfaceInfo info, final boolean writable) {
        final JsRealm realm = bridge.getRealm();
        final String name = writable ? "DOMRect" : "DOMRectReadOnly";
        JsObject parent = writable ? (JsObject) realm.getGlobalObject().get("DOMRectReadOnly") : null;
        NativeFunction ctor = new NativeFunction(realm, parent == null ? realm.getFunctionPrototype() : parent, name, 0,
                new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a rectangle from up to four numbers (x, y, width, height; a missing one is 0).
             * <p>
             * 숫자 최대 네 개(x, y, 너비, 높이. 빠진 것은 0)로 사각형을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                DomRectReadOnly rect = writable ? new DomRect(number(args, 0), number(args, 1), number(args, 2), number(args, 3))
                        : new DomRectReadOnly(number(args, 0), number(args, 1), number(args, 2), number(args, 3));
                HostObject w = bridge.wrap(rect);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        });
        bridge.setConstructor(name, ctor);
        ctor.defineData("fromRect", realm.newFunction("fromRect", 0, new NativeFunction.Call() {
            /**
             * Creates a rectangle from the <code>x</code>, <code>y</code>, <code>width</code> and <code>height</code> of an object.
             * <p>
             * 객체의 <code>x</code>, <code>y</code>, <code>width</code>, <code>height</code> 로 사각형을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the rectangle / 사각형
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                double x = member(o, "x");
                double y = member(o, "y");
                double w = member(o, "width");
                double h = member(o, "height");
                return bridge.wrap(writable ? new DomRect(x, y, w, h) : new DomRectReadOnly(x, y, w, h));
            }
        }), true, true, true);
    }

    /**
     * Installs the point interfaces.
     * <p>
     * 점 인터페이스를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installPoints(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo readOnly = bridge.defineInterface("DOMPointReadOnly", null, DomPointReadOnly.class,
                new String[] { "a:x", "a:y", "a:z", "a:w" });
        final DomBridge.InterfaceInfo writable = bridge.defineInterface("DOMPoint", "DOMPointReadOnly", DomPoint.class,
                new String[] { "a:x=", "a:y=", "a:z=", "a:w=" });
        installPointConstructor(bridge, readOnly, false);
        installPointConstructor(bridge, writable, true);
        readOnly.getPrototype().defineData("toJSON", realm.newFunction("toJSON", 0, new NativeFunction.Call() {
            /**
             * Returns a plain object with the four numbers of the point.
             * <p>
             * 점의 숫자 네 개를 가진 일반 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomPointReadOnly p = (DomPointReadOnly) DomBridge.targetOf(self, DomPointReadOnly.class);
                if (p == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                JsObject o = r.newObject();
                o.createDataProperty("x", Js.num(p.getX()));
                o.createDataProperty("y", Js.num(p.getY()));
                o.createDataProperty("z", Js.num(p.getZ()));
                o.createDataProperty("w", Js.num(p.getW()));
                return o;
            }
        }), true, true, true);
    }

    /**
     * Installs the constructor and the static <code>fromPoint</code> of a point interface.
     * <p>
     * 점 인터페이스의 생성자와 정적 <code>fromPoint</code> 를 설치한다.
     *
     * @param bridge   the bridge / 브리지
     * @param info     the interface / 인터페이스
     * @param writable true for <code>DOMPoint</code> / <code>DOMPoint</code> 이면 true
     */
    private static void installPointConstructor(final DomBridge bridge, final DomBridge.InterfaceInfo info, final boolean writable) {
        final JsRealm realm = bridge.getRealm();
        final String name = writable ? "DOMPoint" : "DOMPointReadOnly";
        JsObject parent = writable ? (JsObject) realm.getGlobalObject().get("DOMPointReadOnly") : null;
        NativeFunction ctor = new NativeFunction(realm, parent == null ? realm.getFunctionPrototype() : parent, name, 0,
                new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a point from up to four numbers (x, y, z default 0; w default 1).
             * <p>
             * 숫자 최대 네 개로 점을 만든다(x, y, z 기본 0, w 기본 1).
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                double w = args.length > 3 && !args[3].isUndefined() ? Ops.toNumber(args[3]) : 1;
                DomPointReadOnly p = writable ? new DomPoint(number(args, 0), number(args, 1), number(args, 2), w)
                        : new DomPointReadOnly(number(args, 0), number(args, 1), number(args, 2), w);
                HostObject wrapper = bridge.wrap(p);
                wrapper.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return wrapper;
            }
        });
        bridge.setConstructor(name, ctor);
        ctor.defineData("fromPoint", realm.newFunction("fromPoint", 0, new NativeFunction.Call() {
            /**
             * Creates a point from the <code>x</code>, <code>y</code>, <code>z</code> and <code>w</code> of an object.
             * <p>
             * 객체의 <code>x</code>, <code>y</code>, <code>z</code>, <code>w</code> 로 점을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the point / 점
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue o = Js.arg(args, 0);
                double wv = o.isObject() && !((JsObject) o).get("w").isUndefined() ? member(o, "w") : 1;
                double x = member(o, "x");
                double y = member(o, "y");
                double z = member(o, "z");
                return bridge.wrap(writable ? new DomPoint(x, y, z, wv) : new DomPointReadOnly(x, y, z, wv));
            }
        }), true, true, true);
    }

    /**
     * Reads one argument as a number (0 if it is missing or undefined).
     * <p>
     * 인수 하나를 숫자로 읽는다(없거나 undefined 이면 0).
     *
     * @param args  the arguments / 인수
     * @param index the position / 위치
     * @return the number / 숫자
     */
    private static double number(JsValue[] args, int index) {
        return index < args.length && !args[index].isUndefined() ? Ops.toNumber(args[index]) : 0;
    }

    /**
     * Reads a member of a dictionary object as a number (0 if it is missing or undefined).
     * <p>
     * 사전 객체의 멤버를 숫자로 읽는다(없거나 undefined 이면 0).
     *
     * @param dictionary the dictionary or any value / 사전 또는 아무 값
     * @param name       the member name / 멤버 이름
     * @return the number / 숫자
     */
    private static double member(JsValue dictionary, String name) {
        if (!dictionary.isObject()) {
            return 0;
        }
        JsValue v = ((JsObject) dictionary).get(name);
        return v.isUndefined() ? 0 : Ops.toNumber(v);
    }
}
