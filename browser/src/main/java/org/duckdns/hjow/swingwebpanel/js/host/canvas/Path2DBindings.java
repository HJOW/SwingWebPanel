package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.geom.AffineTransform;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
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
 * The script interface of <code>Path2D</code> (plan item P20-05): the constructor (empty, a copy of another path, or SVG path data) and the
 * path methods <code>addPath</code>, <code>closePath</code>, <code>moveTo</code>, <code>lineTo</code>, <code>bezierCurveTo</code>,
 * <code>quadraticCurveTo</code>, <code>arc</code>, <code>arcTo</code>, <code>ellipse</code>, <code>rect</code> and <code>roundRect</code>, which
 * follow the rules of the same methods of the context (non-finite arguments are ignored, a negative radius is an <code>IndexSizeError</code>)
 * but keep the points in user space. A <code>Path2D</code> is used with <code>fill</code>, <code>stroke</code>, <code>clip</code>,
 * <code>isPointInPath</code> and <code>isPointInStroke</code> of the context, which apply their matrix at that time.
 * <p>
 * <code>Path2D</code> 의 스크립트 인터페이스(계획 P20-05): 생성자(빈 경로, 다른 경로의 복사본, SVG 경로 데이터)와 경로 메서드 <code>addPath</code>,
 * <code>closePath</code>, <code>moveTo</code>, <code>lineTo</code>, <code>bezierCurveTo</code>, <code>quadraticCurveTo</code>, <code>arc</code>,
 * <code>arcTo</code>, <code>ellipse</code>, <code>rect</code>, <code>roundRect</code>. 이들은 컨텍스트의 같은 이름 메서드와 규칙이 같지만(유한하지 않은 인수는
 * 무시하고, 음수 반지름은 <code>IndexSizeError</code>) 점을 사용자 좌표로 보관한다. <code>Path2D</code> 는 컨텍스트의 <code>fill</code>,
 * <code>stroke</code>, <code>clip</code>, <code>isPointInPath</code>, <code>isPointInStroke</code> 와 함께 쓰며 그때 컨텍스트의 행렬이 적용된다.
 */
final class Path2DBindings {

    /**
     * The identity matrix that the methods add points with.
     * <p>
     * 메서드가 점을 더할 때 쓰는 단위 행렬.
     */
    private static final AffineTransform IDENTITY = new AffineTransform();

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
     * A member of <code>Path2D</code>.
     * <p>
     * <code>Path2D</code> 의 멤버.
     */
    private abstract class Member implements NativeFunction.Call {
        /**
         * Finds the path of the receiver, runs the member and converts engine exceptions.
         * <p>
         * 수신자의 경로를 찾아 멤버를 실행하고 엔진 예외를 변환한다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return undefined / undefined
         */
        public final JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            Path2DValue v = (Path2DValue) DomBridge.targetOf(self, Path2DValue.class);
            if (v == null) {
                throw JsException.typeError("Illegal invocation");
            }
            try {
                run(v.path(), args);
            } catch (DomException e) {
                throw bridge.domError(e);
            }
            return Js.UNDEFINED;
        }

        /**
         * Does the work of the member.
         * <p>
         * 멤버의 일을 한다.
         *
         * @param p    the path / 경로
         * @param args arguments / 인수
         */
        abstract void run(PathBuilder p, JsValue[] args);
    }

    /**
     * Creates the bindings.
     * <p>
     * 연결을 만든다.
     *
     * @param host the page / 페이지
     */
    Path2DBindings(CanvasHost host) {
        this.bridge = host.bridge();
        this.realm = host.realm();
    }

    /**
     * Converts an argument to a number (NaN and infinities stay).
     * <p>
     * 인수를 숫자로 변환한다(NaN 과 무한대는 그대로 둔다).
     *
     * @param args arguments / 인수
     * @param i    the position / 위치
     * @return the number / 숫자
     */
    private static double num(JsValue[] args, int i) {
        return Ops.toNumber(Js.arg(args, i));
    }

    /**
     * Throws the TypeError of missing required arguments.
     * <p>
     * 필수 인수가 없을 때의 TypeError 를 던진다.
     *
     * @param args     arguments / 인수
     * @param required the number required / 필요한 수
     * @param member   the member name / 멤버 이름
     */
    private static void need(JsValue[] args, int required, String member) {
        if (args.length < required) {
            throw JsException.typeError("Failed to execute '" + member + "' on 'Path2D': " + required + " argument" + (required == 1 ? "" : "s")
                    + " required, but only " + args.length + " present.");
        }
    }

    /**
     * Tells whether all numbers are finite.
     * <p>
     * 모든 숫자가 유한한지 알려 준다.
     *
     * @param v the numbers / 숫자
     * @return true if finite / 유한하면 true
     */
    private static boolean finite(double... v) {
        return Context2D.finite(v);
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param m      the behaviour / 동작
     */
    private void method(JsObject proto, String name, int length, Member m) {
        proto.defineData(name, realm.newFunction(name, length, m), true, true, true);
    }

    /**
     * Reads the optional matrix argument of <code>addPath</code>: an object with the members <code>a</code> to <code>f</code> (or
     * <code>m11</code>, <code>m12</code>, <code>m21</code>, <code>m22</code>, <code>m41</code>, <code>m42</code>); missing members are those of the
     * identity matrix.
     * <p>
     * <code>addPath</code> 의 선택 인수인 행렬을 읽는다: <code>a</code> ~ <code>f</code>(또는 <code>m11</code>, <code>m12</code>, <code>m21</code>,
     * <code>m22</code>, <code>m41</code>, <code>m42</code>) 멤버를 가진 객체. 없는 멤버는 단위 행렬의 값이다.
     *
     * @param v the argument / 인수
     * @return the six numbers, or null if one is not finite / 숫자 여섯 개. 하나라도 유한하지 않으면 null
     */
    private static double[] matrix(JsValue v) {
        if (!(v.isUndefined() || v.isNull() || v.isObject())) {
            throw JsException.typeError("Failed to execute 'addPath' on 'Path2D': parameter 2 is not of type 'DOMMatrix2DInit'.");
        }
        // DOMMatrix2DInit 사전(또는 DOMMatrix)을 규격대로 읽는다: a 와 m11 이 다르게 주어지면 TypeError.
        double[] m = MatrixBindings.fromInit2D(v).to2DArray();
        return finite(m) ? m : null;
    }

    /**
     * Installs the interface, its members and its constructor.
     * <p>
     * 인터페이스, 멤버, 생성자를 설치한다.
     */
    void install() {
        bridge.defineInterface("Path2D", null, Path2DValue.class, new String[0]);
        JsObject proto = bridge.getInterface("Path2D").getPrototype();
        method(proto, "addPath", 1, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 1, "addPath");
                Path2DValue other = (Path2DValue) DomBridge.targetOf(a[0], Path2DValue.class);
                if (other == null) {
                    throw JsException.typeError("Failed to execute 'addPath' on 'Path2D': parameter 1 is not of type 'Path2D'.");
                }
                double[] m = matrix(Js.arg(a, 1));
                if (m != null && other.path() != p) {
                    p.append(other.path(), new AffineTransform(m));
                } else if (m != null) {
                    // 자기 자신을 더할 때는 복사본을 더한다.
                    p.append(new Path2DValue(other).path(), new AffineTransform(m));
                }
            }
        });
        method(proto, "closePath", 0, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                p.closePath();
            }
        });
        method(proto, "moveTo", 2, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 2, "moveTo");
                if (finite(num(a, 0), num(a, 1))) {
                    p.moveTo(IDENTITY, num(a, 0), num(a, 1));
                }
            }
        });
        method(proto, "lineTo", 2, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 2, "lineTo");
                if (finite(num(a, 0), num(a, 1))) {
                    p.lineTo(IDENTITY, num(a, 0), num(a, 1));
                }
            }
        });
        method(proto, "quadraticCurveTo", 4, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 4, "quadraticCurveTo");
                if (finite(num(a, 0), num(a, 1), num(a, 2), num(a, 3))) {
                    p.quadTo(IDENTITY, num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                }
            }
        });
        method(proto, "bezierCurveTo", 6, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 6, "bezierCurveTo");
                if (finite(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5))) {
                    p.cubicTo(IDENTITY, num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5));
                }
            }
        });
        method(proto, "arcTo", 5, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 5, "arcTo");
                if (finite(num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4))) {
                    if (num(a, 4) < 0) {
                        throw DomException.indexSize("The radius provided (" + num(a, 4) + ") is negative.");
                    }
                    p.arcTo(IDENTITY, num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4));
                }
            }
        });
        method(proto, "rect", 4, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 4, "rect");
                if (finite(num(a, 0), num(a, 1), num(a, 2), num(a, 3))) {
                    p.rect(IDENTITY, num(a, 0), num(a, 1), num(a, 2), num(a, 3));
                }
            }
        });
        method(proto, "roundRect", 4, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 4, "roundRect");
                double x = num(a, 0);
                double y = num(a, 1);
                double w = num(a, 2);
                double h = num(a, 3);
                double[] r = CanvasBindings.radii(Js.arg(a, 4));
                if (r != null && finite(x, y, w, h)) {
                    p.roundRect(IDENTITY, x, y, w, h, r);
                }
            }
        });
        method(proto, "arc", 5, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 5, "arc");
                ellipse(p, num(a, 0), num(a, 1), num(a, 2), num(a, 2), 0, num(a, 3), num(a, 4), Ops.toBoolean(Js.arg(a, 5)));
            }
        });
        method(proto, "ellipse", 7, new Member() {
            void run(PathBuilder p, JsValue[] a) {
                need(a, 7, "ellipse");
                ellipse(p, num(a, 0), num(a, 1), num(a, 2), num(a, 3), num(a, 4), num(a, 5), num(a, 6), Ops.toBoolean(Js.arg(a, 7)));
            }
        });
        installConstructor();
    }

    /**
     * Adds an elliptical arc after the checks of the specification.
     * <p>
     * 규격의 검사를 거쳐 타원호를 더한다.
     *
     * @param p     the path / 경로
     * @param x     center x / 중심 x
     * @param y     center y / 중심 y
     * @param rx    x radius / x 반지름
     * @param ry    y radius / y 반지름
     * @param rot   rotation / 회전
     * @param start start angle / 시작 각도
     * @param end   end angle / 끝 각도
     * @param ccw   counter-clockwise / 반시계 방향
     */
    private static void ellipse(PathBuilder p, double x, double y, double rx, double ry, double rot, double start, double end, boolean ccw) {
        if (!finite(x, y, rx, ry, rot, start, end)) {
            return;
        }
        if (rx < 0 || ry < 0) {
            throw DomException.indexSize("The radius provided (" + (rx < 0 ? rx : ry) + ") is negative.");
        }
        p.ellipse(IDENTITY, x, y, rx, ry, rot, start, end, ccw);
    }

    /**
     * Installs the constructor of <code>Path2D</code>.
     * <p>
     * <code>Path2D</code> 의 생성자를 설치한다.
     */
    private void installConstructor() {
        NativeFunction ctor = new NativeFunction(realm, "Path2D", 0, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'Path2D': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an empty path, a copy of a path, or the path of SVG path data.
             * <p>
             * 빈 경로, 경로의 복사본, SVG 경로 데이터의 경로를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the object / 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Path2DValue v;
                JsValue first = Js.arg(args, 0);
                Path2DValue other = (Path2DValue) DomBridge.targetOf(first, Path2DValue.class);
                if (first.isUndefined()) {
                    v = new Path2DValue();
                } else if (other != null) {
                    v = new Path2DValue(other);
                } else {
                    v = new Path2DValue(Ops.toString(first));
                }
                return (JsObject) bridge.toJs(v);
            }
        });
        bridge.setConstructor("Path2D", ctor);
    }
}
