package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.DomMatrix;
import org.duckdns.hjow.swingwebpanel.dom.DomMatrixParser;
import org.duckdns.hjow.swingwebpanel.dom.DomMatrixReadOnly;
import org.duckdns.hjow.swingwebpanel.dom.DomPoint;
import org.duckdns.hjow.swingwebpanel.dom.DomPointReadOnly;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;

/**
 * The matrix interfaces of the Geometry Interfaces Module Level 1 (section 6, https://www.w3.org/TR/geometry-1/, read 2026-11-17):
 * <code>DOMMatrixReadOnly</code> and <code>DOMMatrix</code> with the sixteen elements <code>m11</code> to <code>m44</code> and the aliases
 * <code>a</code> to <code>f</code>, <code>is2D</code>, <code>isIdentity</code>, the constructors (nothing, a CSS transform list string, a list of
 * 6 or 16 numbers), <code>fromMatrix()</code>, <code>fromFloat32Array()</code>, <code>fromFloat64Array()</code>, the immutable transformations
 * (<code>translate</code>, <code>scale</code>, <code>scaleNonUniform</code>, <code>scale3d</code>, <code>rotate</code>, <code>rotateFromVector</code>,
 * <code>rotateAxisAngle</code>, <code>skewX</code>, <code>skewY</code>, <code>multiply</code>, <code>flipX</code>, <code>flipY</code>,
 * <code>inverse</code>), <code>transformPoint()</code>, <code>toFloat32Array()</code>, <code>toFloat64Array()</code>, the stringifier and
 * <code>toJSON()</code>, and on <code>DOMMatrix</code> the mutable transformations (<code>multiplySelf</code>, <code>preMultiplySelf</code>,
 * <code>translateSelf</code> ... <code>invertSelf</code>) and <code>setMatrixValue()</code>. It also adds <code>matrixTransform()</code> to
 * <code>DOMPointReadOnly</code>, and the legacy names <code>SVGMatrix</code> and <code>WebKitCSSMatrix</code> for <code>DOMMatrix</code>.
 * The dictionary arguments (<code>DOMMatrixInit</code>) accept any object with the members, among them another matrix. There is no 3D
 * rendering: 3D values are stored, multiplied and inverted. The arithmetic is in {@link DomMatrix}, the string syntax in
 * {@link DomMatrixParser}.
 * <p>
 * Geometry Interfaces Module Level 1 의 행렬 인터페이스(6 절, https://www.w3.org/TR/geometry-1/, 조회 2026-11-17): 열여섯 요소 <code>m11</code> ~
 * <code>m44</code> 와 별칭 <code>a</code> ~ <code>f</code>, <code>is2D</code>, <code>isIdentity</code>, 생성자(인수 없음, CSS 변환 목록 문자열, 숫자 6개 또는 16개 목록),
 * <code>fromMatrix()</code>, <code>fromFloat32Array()</code>, <code>fromFloat64Array()</code>, 새 행렬을 돌려주는 변환(<code>translate</code>, <code>scale</code>,
 * <code>scaleNonUniform</code>, <code>scale3d</code>, <code>rotate</code>, <code>rotateFromVector</code>, <code>rotateAxisAngle</code>, <code>skewX</code>,
 * <code>skewY</code>, <code>multiply</code>, <code>flipX</code>, <code>flipY</code>, <code>inverse</code>), <code>transformPoint()</code>, <code>toFloat32Array()</code>,
 * <code>toFloat64Array()</code>, 문자열 변환, <code>toJSON()</code>, 그리고 <code>DOMMatrix</code> 의 자기 자신을 바꾸는 변환(<code>multiplySelf</code>,
 * <code>preMultiplySelf</code>, <code>translateSelf</code> ... <code>invertSelf</code>)과 <code>setMatrixValue()</code>. <code>DOMPointReadOnly</code> 에
 * <code>matrixTransform()</code> 을 더하고 <code>DOMMatrix</code> 의 옛 이름 <code>SVGMatrix</code>, <code>WebKitCSSMatrix</code> 도 정의한다. 사전 인수
 * (<code>DOMMatrixInit</code>)는 멤버를 가진 아무 객체(다른 행렬 포함)나 받는다. 3D 그리기는 없다: 3D 값은 저장, 곱셈, 역행렬만 한다. 계산은
 * {@link DomMatrix}, 문자열 문법은 {@link DomMatrixParser} 에 있다.
 */
public final class MatrixBindings {

    /**
     * The attribute names of the sixteen elements in column-major order.
     * <p>
     * 열여섯 요소의 속성 이름(열 우선 순서).
     */
    private static final String[] ELEMENTS = { "m11", "m12", "m13", "m14", "m21", "m22", "m23", "m24", "m31", "m32", "m33", "m34", "m41",
            "m42", "m43", "m44" };

    /**
     * The alias names a to f and the column-major index of the element they stand for.
     * <p>
     * 별칭 a ~ f 와 그것이 가리키는 요소의 열 우선 위치.
     */
    private static final String[] ALIASES = { "a", "b", "c", "d", "e", "f" };

    /**
     * The index of the element of each alias.
     * <p>
     * 각 별칭이 가리키는 요소의 위치.
     */
    private static final int[] ALIAS_INDEX = { 0, 1, 4, 5, 12, 13 };

    /**
     * The bridge.
     * <p>
     * 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm.
     * <p>
     * realm 객체.
     */
    private final JsRealm realm;

    /**
     * A method of the read-only interface: it runs on the matrix of the receiver.
     * <p>
     * 읽기 전용 인터페이스의 메서드: 수신자의 행렬에서 실행된다.
     */
    private abstract class Method implements NativeFunction.Call {
        /**
         * Finds the matrix of the receiver and runs the method.
         * <p>
         * 수신자의 행렬을 찾아 메서드를 실행한다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the result / 결과
         */
        public final JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            DomMatrixReadOnly m = (DomMatrixReadOnly) DomBridge.targetOf(self, DomMatrixReadOnly.class);
            if (m == null) {
                throw JsException.typeError("Illegal invocation");
            }
            return run(m, self, args);
        }

        /**
         * Does the work of the method.
         * <p>
         * 메서드의 일을 한다.
         *
         * @param m    the matrix / 행렬
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the result / 결과
         */
        abstract JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] args);
    }

    /**
     * A method of <code>DOMMatrix</code> that changes the matrix of the receiver and returns the receiver.
     * <p>
     * 수신자의 행렬을 바꾸고 수신자를 반환하는 <code>DOMMatrix</code> 의 메서드.
     */
    private abstract class Mutator implements NativeFunction.Call {
        /**
         * Finds the matrix of the receiver, changes it and returns the receiver.
         * <p>
         * 수신자의 행렬을 찾아 바꾸고 수신자를 반환한다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the receiver / 수신자
         */
        public final JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            DomMatrix m = (DomMatrix) DomBridge.targetOf(self, DomMatrix.class);
            if (m == null) {
                throw JsException.typeError("Illegal invocation");
            }
            run(m, args);
            return self;
        }

        /**
         * Changes the matrix.
         * <p>
         * 행렬을 바꾼다.
         *
         * @param m    the matrix / 행렬
         * @param args arguments / 인수
         */
        abstract void run(DomMatrix m, JsValue[] args);
    }

    /**
     * Creates the bindings of a realm.
     * <p>
     * realm 의 연결을 만든다.
     *
     * @param bridge the bridge / 브리지
     */
    private MatrixBindings(DomBridge bridge) {
        this.bridge = bridge;
        this.realm = bridge.getRealm();
    }

    /**
     * Installs the matrix interfaces and <code>DOMPointReadOnly.prototype.matrixTransform</code>.
     * <p>
     * 행렬 인터페이스와 <code>DOMPointReadOnly.prototype.matrixTransform</code> 을 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        final MatrixBindings b = new MatrixBindings(bridge);
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the matrix interface of a Java object.
             * <p>
             * Java 객체의 행렬 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof DomMatrix) {
                    return "DOMMatrix";
                } else if (o instanceof DomMatrixReadOnly) {
                    return "DOMMatrixReadOnly";
                }
                return null;
            }
        });
        b.install();
    }

    /**
     * Installs everything.
     * <p>
     * 모든 것을 설치한다.
     */
    private void install() {
        DomBridge.InterfaceInfo readOnly = bridge.defineInterface("DOMMatrixReadOnly", null, DomMatrixReadOnly.class, new String[0]);
        DomBridge.InterfaceInfo writable = bridge.defineInterface("DOMMatrix", "DOMMatrixReadOnly", DomMatrix.class, new String[0]);
        installConstructor(readOnly, false);
        NativeFunction ctor = installConstructor(writable, true);
        // 옛 이름 두 개는 DOMMatrix 와 같은 생성자 객체이다.
        realm.getGlobalObject().defineBuiltin("SVGMatrix", ctor);
        realm.getGlobalObject().defineBuiltin("WebKitCSSMatrix", ctor);
        installAttributes(readOnly.getPrototype(), writable.getPrototype());
        installReadOnlyMethods(readOnly.getPrototype());
        installMutators(writable.getPrototype());
        installPointTransform();
    }

    /**
     * Reads an argument as a number, or returns the default if it is missing or undefined.
     * <p>
     * 인수를 숫자로 읽는다. 없거나 undefined 이면 기본값을 반환한다.
     *
     * @param args arguments / 인수
     * @param i    the position / 위치
     * @param def  the default / 기본값
     * @return the number / 숫자
     */
    private static double num(JsValue[] args, int i, double def) {
        return i < args.length && !args[i].isUndefined() ? Ops.toNumber(args[i]) : def;
    }

    /**
     * Tells whether an optional argument is present (given and not undefined).
     * <p>
     * 선택 인수가 있는지(주어졌고 undefined 가 아닌지) 알려 준다.
     *
     * @param args arguments / 인수
     * @param i    the position / 위치
     * @return true if present / 있으면 true
     */
    private static boolean present(JsValue[] args, int i) {
        return i < args.length && !args[i].isUndefined();
    }

    /**
     * Reads one member of a dictionary: NaN-safe presence is returned through the holder.
     * <p>
     * 사전의 멤버 하나를 읽는다. 있는지 여부는 홀더로 돌려준다.
     *
     * @param dict    the dictionary object or null / 사전 객체 또는 null
     * @param name    the member name / 멤버 이름
     * @param present receives true at index 0 if the member is present / 멤버가 있으면 0 번에 true 를 받는다
     * @return the number (0 if absent) / 숫자(없으면 0)
     */
    private static double member(JsObject dict, String name, boolean[] present) {
        present[0] = false;
        if (dict == null) {
            return 0;
        }
        JsValue v = dict.get(name);
        if (v.isUndefined()) {
            return 0;
        }
        present[0] = true;
        return Ops.toNumber(v);
    }

    /**
     * Converts a dictionary argument to an object: undefined and null are the empty dictionary, other values that are not objects are a
     * TypeError.
     * <p>
     * 사전 인수를 객체로 바꾼다. undefined 와 null 은 빈 사전이고 객체가 아닌 다른 값은 TypeError 이다.
     *
     * @param v      the argument / 인수
     * @param target the interface for the message / 오류 메시지의 인터페이스
     * @return the object or null for the empty dictionary / 객체. 빈 사전이면 null
     */
    private static JsObject dictionary(JsValue v, String target) {
        if (v == null || v.isUndefined() || v.isNull()) {
            return null;
        }
        if (!v.isObject()) {
            throw JsException.typeError("The provided value is not of type '" + target + "'.");
        }
        return (JsObject) v;
    }

    /**
     * Creates a matrix from a <code>DOMMatrix2DInit</code> dictionary (an object with <code>a</code> to <code>f</code> or <code>m11</code>,
     * <code>m12</code>, <code>m21</code>, <code>m22</code>, <code>m41</code>, <code>m42</code>): "validate and fixup (2D)" of section 6.1. An
     * <code>a</code> and an <code>m11</code> (and the other pairs) that are both present must be the same value (SameValueZero), else it is a
     * TypeError. The members are read in the order of the specification (alphabetical). Another matrix is a valid argument. Used by the canvas
     * (<code>setTransform</code>, <code>addPath</code>, <code>CanvasPattern.setTransform</code>).
     * <p>
     * <code>DOMMatrix2DInit</code> 사전(<code>a</code> ~ <code>f</code> 또는 <code>m11</code>, <code>m12</code>, <code>m21</code>, <code>m22</code>, <code>m41</code>,
     * <code>m42</code> 를 가진 객체)으로 행렬을 만든다: 6.1 절의 "validate and fixup (2D)". <code>a</code> 와 <code>m11</code>(다른 쌍도) 이 둘 다 있으면 같은 값
     * (SameValueZero)이어야 하고 아니면 TypeError 이다. 멤버는 규격의 순서(사전순)로 읽는다. 다른 행렬도 올바른 인수이다. 캔버스(<code>setTransform</code>,
     * <code>addPath</code>, <code>CanvasPattern.setTransform</code>)가 쓴다.
     *
     * @param v the dictionary argument (undefined is the identity) / 사전 인수(undefined 는 단위 행렬)
     * @return the 2D matrix / 2D 행렬
     */
    public static DomMatrix fromInit2D(JsValue v) {
        JsObject d = dictionary(v, "DOMMatrix2DInit");
        double[] six = read2D(d);
        return new DomMatrix(six[0], six[1], six[2], six[3], six[4], six[5]);
    }

    /**
     * Reads and validates the six 2D members of a dictionary.
     * <p>
     * 사전의 2D 멤버 여섯 개를 읽고 검증한다.
     *
     * @param d the dictionary or null / 사전 또는 null
     * @return a, b, c, d, e, f / a, b, c, d, e, f
     */
    private static double[] read2D(JsObject d) {
        String[] alias = ALIASES;
        String[] full = { "m11", "m12", "m21", "m22", "m41", "m42" };
        double[] defaults = { 1, 0, 0, 1, 0, 0 };
        double[] shortValue = new double[6];
        boolean[] shortPresent = new boolean[6];
        double[] longValue = new double[6];
        boolean[] longPresent = new boolean[6];
        boolean[] flag = new boolean[1];
        // 사전순으로 a ~ f 를 먼저 읽는다.
        for (int i = 0; i < 6; i++) {
            shortValue[i] = member(d, alias[i], flag);
            shortPresent[i] = flag[0];
        }
        // 그 다음 m11 형태의 이름을 읽는다.
        for (int i = 0; i < 6; i++) {
            longValue[i] = member(d, full[i], flag);
            longPresent[i] = flag[0];
        }
        double[] result = new double[6];
        // 짧은 이름과 긴 이름이 둘 다 있으면 같아야 하고, 하나만 있으면 그것을, 없으면 기본값을 쓴다.
        for (int i = 0; i < 6; i++) {
            if (shortPresent[i] && longPresent[i] && !sameValueZero(shortValue[i], longValue[i])) {
                throw JsException.typeError("The '" + alias[i] + "' and '" + full[i] + "' members are both present with different values.");
            }
            result[i] = longPresent[i] ? longValue[i] : shortPresent[i] ? shortValue[i] : defaults[i];
        }
        return result;
    }

    /**
     * SameValueZero for two numbers (NaN equals NaN, 0 equals -0).
     * <p>
     * 숫자 둘의 SameValueZero(NaN 은 NaN 과 같고 0 은 -0 과 같다).
     *
     * @param a one number / 숫자 하나
     * @param b the other number / 다른 숫자
     * @return true if they are the same / 같으면 true
     */
    private static boolean sameValueZero(double a, double b) {
        return a == b || (Double.isNaN(a) && Double.isNaN(b));
    }

    /**
     * Creates a matrix from a <code>DOMMatrixInit</code> dictionary: "validate and fixup" of section 6.1 followed by "create from the
     * dictionary" of section 6.3. A 3D member that is not the identity value, or <code>is2D: false</code>, makes the matrix 3D; a 3D member
     * together with <code>is2D: true</code> is a TypeError.
     * <p>
     * <code>DOMMatrixInit</code> 사전으로 행렬을 만든다: 6.1 절의 "validate and fixup" 다음에 6.3 절의 "create from the dictionary". 단위 행렬의 값이 아닌 3D
     * 멤버나 <code>is2D: false</code> 는 행렬을 3D 로 만들고, 3D 멤버와 <code>is2D: true</code> 가 함께 있으면 TypeError 이다.
     *
     * @param v the dictionary argument (undefined is the identity) / 사전 인수(undefined 는 단위 행렬)
     * @return the matrix / 행렬
     */
    public static DomMatrix fromInit(JsValue v) {
        JsObject d = dictionary(v, "DOMMatrixInit");
        double[] six = read2D(d);
        boolean[] flag = new boolean[1];
        JsValue is2DValue = d == null ? Js.UNDEFINED : d.get("is2D");
        String[] names3D = { "m13", "m14", "m23", "m24", "m31", "m32", "m33", "m34", "m43", "m44" };
        // 3D 멤버의 단위 행렬 값(m33 과 m44 는 1).
        double[] identity3D = { 0, 0, 0, 0, 0, 0, 1, 0, 0, 1 };
        double[] values3D = new double[10];
        boolean not2D = false;
        // 3D 멤버를 사전순(m13, m14, m23, ...)으로 읽고 단위 행렬 값이 아닌 것이 있는지 본다.
        for (int i = 0; i < 10; i++) {
            double x = member(d, names3D[i], flag);
            values3D[i] = flag[0] ? x : identity3D[i];
            if (flag[0] && !(x == identity3D[i])) {
                not2D = true;
            }
        }
        boolean is2D;
        if (!is2DValue.isUndefined()) {
            is2D = Ops.toBoolean(is2DValue);
            if (is2D && not2D) {
                throw JsException.typeError("The is2D member is true but the dictionary has 3D members that are not the identity values.");
            }
        } else {
            is2D = !not2D;
        }
        if (is2D) {
            return new DomMatrix(six[0], six[1], six[2], six[3], six[4], six[5]);
        }
        double[] all = new double[16];
        all[0] = six[0];
        all[1] = six[1];
        all[2] = values3D[0];
        all[3] = values3D[1];
        all[4] = six[2];
        all[5] = six[3];
        all[6] = values3D[2];
        all[7] = values3D[3];
        all[8] = values3D[4];
        all[9] = values3D[5];
        all[10] = values3D[6];
        all[11] = values3D[7];
        all[12] = six[4];
        all[13] = six[5];
        all[14] = values3D[8];
        all[15] = values3D[9];
        return new DomMatrix(all);
    }

    /**
     * Creates a matrix from a list of 6 (2D) or 16 (3D) numbers.
     * <p>
     * 숫자 6개(2D) 또는 16개(3D) 목록으로 행렬을 만든다.
     *
     * @param values the numbers / 숫자들
     * @return the matrix / 행렬
     */
    private static DomMatrix fromNumbers(double[] values) {
        if (values.length == 6) {
            return new DomMatrix(values[0], values[1], values[2], values[3], values[4], values[5]);
        } else if (values.length == 16) {
            return new DomMatrix(values);
        }
        throw JsException.typeError("The sequence must contain 6 elements for a 2D matrix or 16 elements for a 3D matrix.");
    }

    /**
     * Parses a string into a matrix or throws the <code>SyntaxError</code> of the specification.
     * <p>
     * 문자열을 행렬로 해석하거나 규격의 <code>SyntaxError</code> 를 던진다.
     *
     * @param text the string / 문자열
     * @return the matrix / 행렬
     */
    private DomMatrix parseString(String text) {
        DomMatrix m = DomMatrixParser.parse(text);
        if (m == null) {
            throw bridge.domError(DomException.syntax("Failed to parse '" + text + "' as a transform list with absolute units."));
        }
        return m;
    }

    /**
     * Creates a matrix from the argument of the constructors: nothing, a string or a sequence of numbers.
     * <p>
     * 생성자의 인수(없음, 문자열, 숫자 시퀀스)로 행렬을 만든다.
     *
     * @param args the constructor arguments / 생성자 인수
     * @return the matrix / 행렬
     */
    private DomMatrix create(JsValue[] args) {
        if (!present(args, 0)) {
            return new DomMatrix();
        }
        JsValue init = args[0];
        // 이터러블 객체는 숫자 시퀀스로 읽는다.
        if (init.isObject() && !((JsObject) init).get(WellKnownSymbols.ITERATOR).isUndefined()) {
            List<JsValue> list = IteratorHelper.toList(realm, init);
            double[] numbers = new double[list.size()];
            // 요소를 하나씩 숫자로 바꾼다.
            for (int i = 0; i < numbers.length; i++) {
                numbers[i] = Ops.toNumber(list.get(i));
            }
            return fromNumbers(numbers);
        }
        return parseString(Ops.toString(init));
    }

    /**
     * Installs a constructor and its static members.
     * <p>
     * 생성자와 정적 멤버를 설치한다.
     *
     * @param info     the interface / 인터페이스
     * @param writable true for <code>DOMMatrix</code> / <code>DOMMatrix</code> 이면 true
     * @return the constructor / 생성자
     */
    private NativeFunction installConstructor(final DomBridge.InterfaceInfo info, final boolean writable) {
        final String name = writable ? "DOMMatrix" : "DOMMatrixReadOnly";
        JsObject parent = writable ? (JsObject) realm.getGlobalObject().get("DOMMatrixReadOnly") : null;
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
             * Creates a matrix from nothing, a string or a sequence of numbers.
             * <p>
             * 인수 없음, 문자열, 숫자 시퀀스로 행렬을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                DomMatrix m = create(args);
                return wrapWithPrototype(writable ? m : new DomMatrixReadOnly(m), newTarget, info);
            }
        });
        bridge.setConstructor(name, ctor);
        ctor.defineData("fromMatrix", realm.newFunction("fromMatrix", 0, new NativeFunction.Call() {
            /**
             * Creates a matrix from a dictionary.
             * <p>
             * 사전으로 행렬을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the matrix / 행렬
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomMatrix m = fromInit(Js.arg(args, 0));
                return bridge.wrap(writable ? m : new DomMatrixReadOnly(m));
            }
        }), true, true, true);
        ctor.defineData("fromFloat32Array", realm.newFunction("fromFloat32Array", 1, new NativeFunction.Call() {
            /**
             * Creates a matrix from a Float32Array of 6 or 16 elements.
             * <p>
             * 요소 6개 또는 16개의 Float32Array 로 행렬을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the matrix / 행렬
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomMatrix m = fromNumbers(floats(Js.arg(args, 0), ElementType.FLOAT32));
                return bridge.wrap(writable ? m : new DomMatrixReadOnly(m));
            }
        }), true, true, true);
        ctor.defineData("fromFloat64Array", realm.newFunction("fromFloat64Array", 1, new NativeFunction.Call() {
            /**
             * Creates a matrix from a Float64Array of 6 or 16 elements.
             * <p>
             * 요소 6개 또는 16개의 Float64Array 로 행렬을 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the matrix / 행렬
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomMatrix m = fromNumbers(floats(Js.arg(args, 0), ElementType.FLOAT64));
                return bridge.wrap(writable ? m : new DomMatrixReadOnly(m));
            }
        }), true, true, true);
        return ctor;
    }

    /**
     * Wraps a matrix with the prototype of the new target (so subclasses work).
     * <p>
     * 행렬을 new 대상의 프로토타입으로 감싼다(하위 클래스가 동작하도록).
     *
     * @param m         the matrix / 행렬
     * @param newTarget the new target / new 대상
     * @param info      the interface / 인터페이스
     * @return the wrapper / 래퍼
     */
    private JsObject wrapWithPrototype(DomMatrixReadOnly m, JsObject newTarget, DomBridge.InterfaceInfo info) {
        HostObject w = bridge.wrap(m);
        w.setPrototypeOf(Ops.getPrototypeFromConstructor(realm, newTarget, info.getPrototype()));
        return w;
    }

    /**
     * Reads the numbers of a typed array of a given type.
     * <p>
     * 지정한 형식의 형식화 배열의 숫자를 읽는다.
     *
     * @param v    the argument / 인수
     * @param type the required element type / 필요한 요소 형식
     * @return the numbers / 숫자들
     */
    private static double[] floats(JsValue v, ElementType type) {
        if (!(v instanceof TypedArrayObject) || ((TypedArrayObject) v).getType() != type) {
            throw JsException.typeError("The provided value is not of type '" + type.getConstructorName() + "'.");
        }
        TypedArrayObject a = (TypedArrayObject) v;
        double[] result = new double[a.getLength()];
        // 요소를 하나씩 읽는다.
        for (int i = 0; i < result.length; i++) {
            result[i] = a.getElement(i);
        }
        return result;
    }

    /**
     * Installs the element attributes: read-only on <code>DOMMatrixReadOnly</code>, with setters on <code>DOMMatrix</code>.
     * <p>
     * 요소 속성을 설치한다: <code>DOMMatrixReadOnly</code> 에는 읽기 전용, <code>DOMMatrix</code> 에는 설정자가 있게.
     *
     * @param readOnly the prototype of <code>DOMMatrixReadOnly</code> / <code>DOMMatrixReadOnly</code> 의 프로토타입
     * @param writable the prototype of <code>DOMMatrix</code> / <code>DOMMatrix</code> 의 프로토타입
     */
    private void installAttributes(JsObject readOnly, JsObject writable) {
        // 열여섯 요소와 여섯 별칭에 읽기 전용 접근자와 설정자가 있는 접근자를 만든다.
        for (int i = 0; i < 16; i++) {
            defineElement(readOnly, writable, ELEMENTS[i], i);
        }
        // 별칭 a ~ f 도 같은 방식으로 정의한다.
        for (int i = 0; i < 6; i++) {
            defineElement(readOnly, writable, ALIASES[i], ALIAS_INDEX[i]);
        }
        readOnly.defineAccessor("is2D", realm.newFunction("get is2D", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] args) {
                return Js.bool(m.is2D());
            }
        }), null, true, true);
        readOnly.defineAccessor("isIdentity", realm.newFunction("get isIdentity", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] args) {
                return Js.bool(m.isIdentity());
            }
        }), null, true, true);
    }

    /**
     * Defines the getter on the read-only prototype and the getter and setter on the writable one.
     * <p>
     * 읽기 전용 프로토타입에 getter 를, 쓰기 가능한 프로토타입에 getter 와 setter 를 정의한다.
     *
     * @param readOnly the read-only prototype / 읽기 전용 프로토타입
     * @param writable the writable prototype / 쓰기 가능한 프로토타입
     * @param name     the attribute name / 속성 이름
     * @param index    the element index / 요소 위치
     */
    private void defineElement(JsObject readOnly, JsObject writable, String name, final int index) {
        NativeFunction getter = realm.newFunction("get " + name, 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] args) {
                return Js.num(m.get(index));
            }
        });
        readOnly.defineAccessor(name, getter, null, true, true);
        NativeFunction setter = realm.newFunction("set " + name, 1, new Mutator() {
            void run(DomMatrix m, JsValue[] args) {
                m.set(index, Ops.toNumber(Js.arg(args, 0)));
            }
        });
        // 설정자는 새 값을 먼저 변환한 뒤 정한다(변환이 던지면 행렬은 그대로).
        writable.defineAccessor(name, getter, setter, true, true);
    }

    /**
     * Defines a method of the read-only interface.
     * <p>
     * 읽기 전용 인터페이스의 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param m      the behaviour / 동작
     */
    private void method(JsObject proto, String name, int length, NativeFunction.Call m) {
        proto.defineData(name, realm.newFunction(name, length, m), true, true, true);
    }

    /**
     * Wraps a result matrix.
     * <p>
     * 결과 행렬을 감싼다.
     *
     * @param m the matrix / 행렬
     * @return the wrapper / 래퍼
     */
    private JsValue result(DomMatrix m) {
        return bridge.wrap(m);
    }

    /**
     * Installs the methods of <code>DOMMatrixReadOnly</code>: each returns a new <code>DOMMatrix</code>.
     * <p>
     * <code>DOMMatrixReadOnly</code> 의 메서드를 설치한다. 각각 새 <code>DOMMatrix</code> 를 반환한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installReadOnlyMethods(JsObject proto) {
        method(proto, "translate", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).translateSelf(num(a, 0, 0), num(a, 1, 0), num(a, 2, 0)));
            }
        });
        method(proto, "scale", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                double sx = num(a, 0, 1);
                double sy = num(a, 1, sx);
                return result(new DomMatrix(m).scaleSelf(sx, sy, num(a, 2, 1), num(a, 3, 0), num(a, 4, 0), num(a, 5, 0)));
            }
        });
        method(proto, "scaleNonUniform", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).scaleSelf(num(a, 0, 1), num(a, 1, 1), 1, 0, 0, 0));
            }
        });
        method(proto, "scale3d", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).scale3dSelf(num(a, 0, 1), num(a, 1, 0), num(a, 2, 0), num(a, 3, 0)));
            }
        });
        method(proto, "rotate", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                double[] r = rotation(a);
                return result(new DomMatrix(m).rotateSelf(r[0], r[1], r[2]));
            }
        });
        method(proto, "rotateFromVector", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).rotateFromVectorSelf(num(a, 0, 0), num(a, 1, 0)));
            }
        });
        method(proto, "rotateAxisAngle", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).rotateAxisAngleSelf(num(a, 0, 0), num(a, 1, 0), num(a, 2, 0), num(a, 3, 0)));
            }
        });
        method(proto, "skewX", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).skewXSelf(num(a, 0, 0)));
            }
        });
        method(proto, "skewY", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).skewYSelf(num(a, 0, 0)));
            }
        });
        method(proto, "multiply", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).multiplySelf(fromInit(Js.arg(a, 0))));
            }
        });
        method(proto, "flipX", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).multiplySelf(new DomMatrixReadOnly(-1, 0, 0, 1, 0, 0)));
            }
        });
        method(proto, "flipY", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).multiplySelf(new DomMatrixReadOnly(1, 0, 0, -1, 0, 0)));
            }
        });
        method(proto, "inverse", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return result(new DomMatrix(m).invertSelf());
            }
        });
        method(proto, "transformPoint", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return bridge.wrap(m.transformPoint(pointInit(Js.arg(a, 0))));
            }
        });
        method(proto, "toFloat32Array", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return array(m, ElementType.FLOAT32, 4);
            }
        });
        method(proto, "toFloat64Array", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return array(m, ElementType.FLOAT64, 8);
            }
        });
        method(proto, "toJSON", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                JsObject o = realm.newObject();
                // a ~ f, m11 ~ m44, is2D, isIdentity 를 일반 객체의 데이터 프로퍼티로 만든다.
                for (int i = 0; i < 6; i++) {
                    o.createDataProperty(ALIASES[i], Js.num(m.get(ALIAS_INDEX[i])));
                }
                // 열여섯 요소를 같은 방식으로 더한다.
                for (int i = 0; i < 16; i++) {
                    o.createDataProperty(ELEMENTS[i], Js.num(m.get(i)));
                }
                o.createDataProperty("is2D", Js.bool(m.is2D()));
                o.createDataProperty("isIdentity", Js.bool(m.isIdentity()));
                return o;
            }
        });
        method(proto, "toString", 0, new Method() {
            JsValue run(DomMatrixReadOnly m, JsValue self, JsValue[] a) {
                return Js.str(stringify(m));
            }
        });
    }

    /**
     * Resolves the arguments of <code>rotate</code> and <code>rotateSelf</code>: with only one argument (or none) it is the rotation around
     * z.
     * <p>
     * <code>rotate</code> 와 <code>rotateSelf</code> 의 인수를 정한다: 인수가 하나뿐(또는 없음)이면 z 축 둘레의 회전이다.
     *
     * @param a arguments / 인수
     * @return rotX, rotY, rotZ in degrees / 도 단위 rotX, rotY, rotZ
     */
    private static double[] rotation(JsValue[] a) {
        double x = num(a, 0, 0);
        boolean hasY = present(a, 1);
        boolean hasZ = present(a, 2);
        double y = num(a, 1, 0);
        double z = num(a, 2, 0);
        // 둘째와 셋째 인수가 모두 없으면 첫째 인수가 z 축 회전이다.
        if (!hasY && !hasZ) {
            return new double[] { 0, 0, x };
        }
        return new double[] { x, y, z };
    }

    /**
     * Builds a Float32Array or Float64Array with the sixteen elements in column-major order.
     * <p>
     * 열여섯 요소를 열 우선 순서로 담은 Float32Array 나 Float64Array 를 만든다.
     *
     * @param m    the matrix / 행렬
     * @param type the element type / 요소 형식
     * @param size the bytes per element / 요소당 바이트
     * @return the typed array / 형식화 배열
     */
    private JsValue array(DomMatrixReadOnly m, ElementType type, int size) {
        TypedArrayObject a = TypedArrays.newTypedArray(realm, type, TypedArrays.newArrayBuffer(realm, new byte[16 * size]), 0, 16);
        // 열여섯 요소를 하나씩 쓴다.
        for (int i = 0; i < 16; i++) {
            a.setElement(i, m.get(i));
        }
        return a;
    }

    /**
     * The stringifier: <code>matrix(a, b, c, d, e, f)</code> for a 2D matrix, <code>matrix3d(...)</code> with sixteen numbers otherwise; a
     * matrix with a NaN or infinite element is an <code>InvalidStateError</code>.
     * <p>
     * 문자열 변환: 2D 행렬은 <code>matrix(a, b, c, d, e, f)</code>, 아니면 숫자 열여섯 개의 <code>matrix3d(...)</code>. NaN 이나 무한대 요소가 있으면
     * <code>InvalidStateError</code> 이다.
     *
     * @param m the matrix / 행렬
     * @return the string / 문자열
     */
    private String stringify(DomMatrixReadOnly m) {
        if (!m.isFinite()) {
            throw bridge.domError(DomException.invalidState("The matrix cannot be serialized because it has a non-finite value (NaN or Infinity)."));
        }
        StringBuilder sb = new StringBuilder();
        int[] order;
        if (m.is2D()) {
            sb.append("matrix(");
            order = new int[] { 0, 1, 4, 5, 12, 13 };
        } else {
            sb.append("matrix3d(");
            order = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        }
        // 요소를 JavaScript 숫자 문자열로 쉼표와 공백으로 이어 쓴다.
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Ops.toString(Js.num(m.get(order[i]))));
        }
        return sb.append(')').toString();
    }

    /**
     * Reads a <code>DOMPointInit</code> dictionary (x, y, z default 0, w default 1).
     * <p>
     * <code>DOMPointInit</code> 사전을 읽는다(x, y, z 기본 0, w 기본 1).
     *
     * @param v the argument / 인수
     * @return the point / 점
     */
    private static DomPointReadOnly pointInit(JsValue v) {
        JsObject d = dictionary(v, "DOMPointInit");
        boolean[] flag = new boolean[1];
        // 사전순(w, x, y, z)으로 읽는다.
        double w = member(d, "w", flag);
        if (!flag[0]) {
            w = 1;
        }
        double x = member(d, "x", flag);
        double y = member(d, "y", flag);
        double z = member(d, "z", flag);
        return new DomPointReadOnly(x, y, z, w);
    }

    /**
     * Installs the methods of <code>DOMMatrix</code> that change the matrix.
     * <p>
     * 행렬을 바꾸는 <code>DOMMatrix</code> 의 메서드를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installMutators(JsObject proto) {
        mutator(proto, "multiplySelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.multiplySelf(fromInit(Js.arg(a, 0)));
            }
        });
        mutator(proto, "preMultiplySelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.preMultiplySelf(fromInit(Js.arg(a, 0)));
            }
        });
        mutator(proto, "translateSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.translateSelf(num(a, 0, 0), num(a, 1, 0), num(a, 2, 0));
            }
        });
        mutator(proto, "scaleSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                double sx = num(a, 0, 1);
                double sy = num(a, 1, sx);
                m.scaleSelf(sx, sy, num(a, 2, 1), num(a, 3, 0), num(a, 4, 0), num(a, 5, 0));
            }
        });
        mutator(proto, "scale3dSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.scale3dSelf(num(a, 0, 1), num(a, 1, 0), num(a, 2, 0), num(a, 3, 0));
            }
        });
        mutator(proto, "rotateSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                double[] r = rotation(a);
                m.rotateSelf(r[0], r[1], r[2]);
            }
        });
        mutator(proto, "rotateFromVectorSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.rotateFromVectorSelf(num(a, 0, 0), num(a, 1, 0));
            }
        });
        mutator(proto, "rotateAxisAngleSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.rotateAxisAngleSelf(num(a, 0, 0), num(a, 1, 0), num(a, 2, 0), num(a, 3, 0));
            }
        });
        mutator(proto, "skewXSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.skewXSelf(num(a, 0, 0));
            }
        });
        mutator(proto, "skewYSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.skewYSelf(num(a, 0, 0));
            }
        });
        mutator(proto, "invertSelf", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                m.invertSelf();
            }
        });
        mutator(proto, "setMatrixValue", new Mutator() {
            void run(DomMatrix m, JsValue[] a) {
                if (a.length == 0) {
                    throw JsException.typeError("Failed to execute 'setMatrixValue' on 'DOMMatrix': 1 argument required, but only 0 present.");
                }
                DomMatrix parsed = parseString(Ops.toString(a[0]));
                m.setValues(parsed.toArray(), parsed.is2D());
            }
        });
    }

    /**
     * Defines a method that changes the matrix.
     * <p>
     * 행렬을 바꾸는 메서드를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param m     the behaviour / 동작
     */
    private void mutator(JsObject proto, String name, Mutator m) {
        proto.defineData(name, realm.newFunction(name, 0, m), true, true, true);
    }

    /**
     * Adds <code>matrixTransform()</code> to <code>DOMPointReadOnly</code>.
     * <p>
     * <code>DOMPointReadOnly</code> 에 <code>matrixTransform()</code> 을 더한다.
     */
    private void installPointTransform() {
        JsObject proto = bridge.getInterface("DOMPointReadOnly").getPrototype();
        proto.defineData("matrixTransform", realm.newFunction("matrixTransform", 0, new NativeFunction.Call() {
            /**
             * Transforms the point with a matrix given as a dictionary and returns a new <code>DOMPoint</code>.
             * <p>
             * 사전으로 준 행렬로 점을 변환하여 새 <code>DOMPoint</code> 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the new point / 새 점
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomPointReadOnly p = (DomPointReadOnly) DomBridge.targetOf(self, DomPointReadOnly.class);
                if (p == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                DomPoint moved = fromInit(Js.arg(args, 0)).transformPoint(p);
                return bridge.wrap(moved);
            }
        }), true, true, true);
    }
}
