package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>DOMMatrixReadOnly</code> and <code>DOMMatrix</code> (plan item P20-05-b, Geometry Interfaces Module Level 1 section 6): the
 * constructors (nothing, a sequence of 6 or 16 numbers, a CSS transform list), the sixteen elements and the aliases <code>a</code> to
 * <code>f</code>, <code>is2D</code> and <code>isIdentity</code>, the immutable and the mutable transformations, <code>inverse()</code>,
 * <code>transformPoint()</code> and <code>DOMPoint.matrixTransform()</code>, the dictionary arguments, the typed array conversions, the
 * stringifier and <code>toJSON()</code>. The values are compared as numbers with a tolerance where trigonometry is involved and as exact
 * strings where it is not.
 * <p>
 * <code>DOMMatrixReadOnly</code> 와 <code>DOMMatrix</code> 시험(계획 P20-05-b, Geometry Interfaces Module Level 1 6 절): 생성자(인수 없음, 숫자 6개나 16개 시퀀스,
 * CSS 변환 목록), 열여섯 요소와 별칭 <code>a</code> ~ <code>f</code>, <code>is2D</code> 와 <code>isIdentity</code>, 새 행렬을 돌려주는 변환과 자신을 바꾸는 변환,
 * <code>inverse()</code>, <code>transformPoint()</code> 와 <code>DOMPoint.matrixTransform()</code>, 사전 인수, 형식화 배열 변환, 문자열 변환과
 * <code>toJSON()</code>. 삼각함수가 끼는 값은 오차를 두고 숫자로, 그렇지 않은 값은 문자열로 정확히 비교한다.
 */
public class DomMatrixTest {

    /**
     * The helper script: <code>near(a, b)</code> compares two matrices or two lists of numbers with a small tolerance.
     * <p>
     * 도우미 스크립트: <code>near(a, b)</code> 는 행렬 둘이나 숫자 목록 둘을 작은 오차로 비교한다.
     */
    private static final String HELPERS = "function arr(m) { return [m.m11, m.m12, m.m13, m.m14, m.m21, m.m22, m.m23, m.m24, m.m31, m.m32, m.m33, m.m34,"
            + " m.m41, m.m42, m.m43, m.m44]; }"
            + "function near(x, y) { var p = x.length === undefined ? arr(x) : x; var q = y.length === undefined ? arr(y) : y;"
            + " if (p.length !== q.length) return false; for (var i = 0; i < p.length; i++) { if (!(Math.abs(p[i] - q[i]) < 1e-9)) return false; } return true; }"
            + "function err(f) { try { f(); return 'none'; } catch (e) { return e.name; } }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DomMatrixTest() {
    }

    /**
     * Creates a page with the helper script.
     * <p>
     * 도우미 스크립트가 있는 페이지를 만든다.
     *
     * @return the fixture / 시험 도구
     */
    private static PageFixture page() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        f.scripts.evaluate(HELPERS, "helpers");
        return f;
    }

    /**
     * Evaluates a script and returns the string of the last expression.
     * <p>
     * 스크립트를 평가하여 마지막 식의 문자열을 반환한다.
     *
     * @param f    the page / 페이지
     * @param code the script / 스크립트
     * @return the result / 결과
     */
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * The constructors: no argument is the 2D identity, a list of 6 numbers is a 2D matrix and a list of 16 a 3D matrix (in column-major order),
     * any other length is a TypeError, and the interfaces and the legacy names exist.
     * <p>
     * 생성자: 인수가 없으면 2D 단위 행렬, 숫자 6개 목록은 2D 행렬, 16개 목록은 3D 행렬(열 우선 순서)이고, 다른 길이는 TypeError 이며, 인터페이스와 옛 이름이
     * 존재한다.
     */
    public void testConstructors() {
        PageFixture f = page();
        Check.equal("matrix(1, 0, 0, 1, 0, 0)|true|true", js(f, "var m = new DOMMatrix(); [String(m), m.is2D, m.isIdentity].join('|')"), "the identity");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)|1,2,3,4,5,6|true|false",
                js(f, "var m = new DOMMatrix([1, 2, 3, 4, 5, 6]); [String(m), [m.a, m.b, m.c, m.d, m.e, m.f], m.is2D, m.isIdentity].join('|')"),
                "six numbers are a, b, c, d, e, f");
        Check.equal("1,2,5,6,0,0|false", js(f, "var m = new DOMMatrix([1, 2, 0, 0, 5, 6, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]); [[m.m11, m.m12, m.m21, m.m22, m.m41, m.m42] , m.is2D].join('|')"),
                "sixteen numbers are a 3D matrix in column-major order");
        Check.equal("7,8,9", js(f, "var m = new DOMMatrix([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 7, 8, 9, 1]); [m.m41, m.m42, m.m43].join()"),
                "the last four numbers are the translation");
        Check.equal("TypeError", js(f, "err(function() { new DOMMatrix([1, 2, 3]); })"), "three numbers");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrix(); })"), "the constructor needs new");
        Check.equal("true,true,true,true,true,true", js(f, "[new DOMMatrix() instanceof DOMMatrixReadOnly, new DOMMatrixReadOnly() instanceof DOMMatrixReadOnly,"
                + " !(new DOMMatrixReadOnly() instanceof DOMMatrix), SVGMatrix === DOMMatrix, WebKitCSSMatrix === DOMMatrix,"
                + " Object.prototype.toString.call(new DOMMatrix()) === '[object DOMMatrix]'].join()"), "the interfaces and the legacy names");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(new DOMMatrix(new Float32Array([1, 2, 3, 4, 5, 6])))"), "any iterable of numbers");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrixReadOnly())"), "the read-only matrix is stringified the same way");
    }

    /**
     * The string form: a CSS transform list is multiplied from left to right, absolute units and angle units are converted, and a list that uses
     * only 2D functions is 2D. Invalid syntax and relative units are a <code>SyntaxError</code>.
     * <p>
     * 문자열 형태: CSS 변환 목록은 왼쪽에서 오른쪽으로 곱하고, 절대 단위와 각도 단위는 변환하며, 2D 함수만 쓰면 2D 이다. 잘못된 문법과 상대 단위는
     * <code>SyntaxError</code> 이다.
     */
    public void testStringParsing() {
        PageFixture f = page();
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix(''))"), "the empty string is the identity");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix('none'))"), "none is the identity");
        Check.equal("matrix(2, 0, 0, 2, 10, 20)", js(f, "String(new DOMMatrix('translate(10px, 20px) scale(2)'))"), "scale is applied to a point first");
        Check.equal("matrix(2, 0, 0, 2, 20, 40)", js(f, "String(new DOMMatrix('scale(2) translate(10px, 20px)'))"), "the translation is scaled");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(new DOMMatrix(' matrix( 1 , 2 , 3 , 4 , 5 , 6 ) '))"), "white space");
        Check.equal("true", js(f, "var m = new DOMMatrix('rotate(90deg)'); near(m, [0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1])"), "a quarter turn");
        Check.equal("true", js(f, "near(new DOMMatrix('rotate(0.25turn)'), new DOMMatrix('rotate(100grad)')) && near(new DOMMatrix('rotate(90deg)'),"
                + " new DOMMatrix('rotate(' + (Math.PI / 2) + 'rad)'))"), "turn, grad and rad");
        Check.equal("96,96,37.795276,48", js(f, "var m = new DOMMatrix('translate(1in, 1in)'); var n = new DOMMatrix('translate(1cm, 3pc)');"
                + " [m.e, m.f, Math.round(n.e * 1e6) / 1e6, n.f].join()"), "absolute length units");
        Check.equal("true", js(f, "var m = new DOMMatrix('skew(45deg, 0) skewX(0) scaleX(2) scaleY(3)'); near([m.a, m.b, m.c, m.d], [2, 0, 3, 3])"), "skew and the axis scales");
        Check.equal("true", js(f, "near(new DOMMatrix('skewY(45deg)'), new DOMMatrix([1, 1, 0, 1, 0, 0]))"), "skewY");
        Check.equal("true,true", js(f, "var m = new DOMMatrix('translateX(5px) translateY(7px)'); [m.is2D, m.e === 5 && m.f === 7].join()"), "2D functions");
        Check.equal("false,1,2,3", js(f, "var m = new DOMMatrix('translate3d(1px, 2px, 3px)'); [m.is2D, m.m41, m.m42, m.m43].join()"), "a 3D function makes the matrix 3D");
        Check.equal("false", js(f, "new DOMMatrix('translateZ(0)').is2D + ''"), "even a translateZ(0) is a 3D function");
        Check.equal("false", js(f, "new DOMMatrix('rotateZ(10deg)').is2D + ''"), "so is rotateZ");
        Check.equal("true", js(f, "var m = new DOMMatrix('rotateX(90deg)'); near(m, [1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1])"), "rotateX");
        Check.equal("true", js(f, "var m = new DOMMatrix('rotateY(90deg)'); near(m, [0, 0, -1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1])"), "rotateY");
        Check.equal("true", js(f, "near(new DOMMatrix('rotate3d(0, 0, 1, 30deg)'), new DOMMatrix().rotate(30))"), "rotate3d around z");
        Check.equal("-0.01,false", js(f, "var m = new DOMMatrix('perspective(100px)'); [m.m34, m.is2D].join()"), "perspective");
        Check.equal("true", js(f, "near(new DOMMatrix('matrix3d(1,0,0,0, 0,1,0,0, 0,0,1,0, 7,8,9,1)'), [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 7, 8, 9, 1])"), "matrix3d");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('foo'); })"), "an unknown word");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('translate(1em, 0)'); })"), "a relative unit needs a context");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('translate(10%)'); })"), "a percentage too");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('rotate(10)'); })"), "an angle needs a unit unless it is 0");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('scale(1, 2, 3)'); })"), "too many arguments");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('scale(1) foo(2)'); })"), "an unknown function");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('inherit'); })"), "a keyword other than none");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('translate(1px 2px)'); })"), "commas are required");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix('translate(calc(1px + 2px))'); })"), "calc is not supported");
        Check.equal("SyntaxError", js(f, "err(function() { new DOMMatrix(5); })"), "a number is converted to a string");
        Check.equal("matrix(1, 0, 0, 1, 3, 0)", js(f, "String(new DOMMatrix('translate(3px)'))"), "one translate argument");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix('rotate(0)'))"), "a plain 0 is an angle");
    }

    /**
     * The elements and their aliases: a to f are m11, m12, m21, m22, m41, m42; the read-only matrix has no setters; on a <code>DOMMatrix</code>
     * the setters change the element and a 3D element that is no longer the identity value turns <code>is2D</code> false for good.
     * <p>
     * 요소와 별칭: a ~ f 는 m11, m12, m21, m22, m41, m42 이다. 읽기 전용 행렬에는 설정자가 없고, <code>DOMMatrix</code> 에서는 설정자가 요소를 바꾸며 단위 행렬의 값이
     * 아닌 3D 요소는 <code>is2D</code> 를 영원히 false 로 만든다.
     */
    public void testAttributes() {
        PageFixture f = page();
        Check.equal("1,2,3,4,5,6", js(f, "var m = new DOMMatrix([1, 2, 3, 4, 5, 6]); [m.m11, m.m12, m.m21, m.m22, m.m41, m.m42].join()"), "the aliases");
        Check.equal("9,8,7,6,5,4|true", js(f, "var m = new DOMMatrix(); m.a = 9; m.b = 8; m.c = 7; m.d = 6; m.e = 5; m.f = 4;"
                + " [[m.m11, m.m12, m.m21, m.m22, m.m41, m.m42].join(), m.is2D].join('|')"), "the setters of the aliases");
        Check.equal("true,true,true", js(f, "var d = Object.getOwnPropertyDescriptor(DOMMatrixReadOnly.prototype, 'm11');"
                + " var w = Object.getOwnPropertyDescriptor(DOMMatrix.prototype, 'm11'); var a = Object.getOwnPropertyDescriptor(DOMMatrixReadOnly.prototype, 'is2D');"
                + " [d.set === undefined && typeof d.get === 'function', typeof w.set === 'function', a.set === undefined].join()"),
                "m11 is read-only on DOMMatrixReadOnly and read-write on DOMMatrix, is2D is always read-only");
        Check.equal("1", js(f, "var m = new DOMMatrixReadOnly([1, 2, 3, 4, 5, 6]); m.a = 100; m.a + ''"), "assigning to a read-only attribute does nothing in sloppy mode");
        Check.equal("true,true,true,false,false", js(f, "var m = new DOMMatrix(); m.m33 = 1; m.m44 = 1; m.m13 = 0; var r = [m.is2D]; m.m23 = -0; r.push(m.is2D); m.m11 = 5; r.push(m.is2D);"
                + " m.m13 = 1; r.push(m.is2D); m.m13 = 0; r.push(m.is2D); r.join()"),
                "setting a 3D element to its identity value keeps the matrix 2D, anything else makes it 3D for good");
        Check.equal("false", js(f, "var m = new DOMMatrix(); m.m33 = 2; m.is2D + ''"), "m33 other than 1");
        Check.equal("false", js(f, "var m = new DOMMatrix(); m.m44 = 0; m.is2D + ''"), "m44 other than 1");
        Check.equal("false,true", js(f, "var m = new DOMMatrix(); m.m43 = 1; var r = [m.isIdentity]; m.m43 = 0; r.push(m.isIdentity); r.join()"), "isIdentity looks at all sixteen");
        Check.equal("true", js(f, "new DOMMatrix([1, 0, -0, 1, -0, 0]).isIdentity + ''"), "minus zero counts as zero");
        Check.equal("NaN,true", js(f, "var m = new DOMMatrix(); m.a = 'x'; [m.a, m.is2D].join()"), "values are converted to numbers (NaN is allowed)");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrixReadOnly.prototype.translate.call({}); })"), "methods check their receiver");
        Check.equal("TypeError", js(f, "err(function() { Object.getOwnPropertyDescriptor(DOMMatrix.prototype, 'a').get.call({}); })"), "attributes too");
    }

    /**
     * The immutable transformations return a new <code>DOMMatrix</code> and leave the receiver alone. Each is checked against the numbers of
     * the CSS functions.
     * <p>
     * 새 행렬을 돌려주는 변환은 새 <code>DOMMatrix</code> 를 반환하고 수신자를 바꾸지 않는다. 각각 CSS 함수의 숫자와 대조한다.
     */
    public void testImmutableTransformations() {
        PageFixture f = page();
        Check.equal("matrix(1, 2, 3, 4, 5, 6)|matrix(1, 2, 3, 4, 9, 12)|true,false,true", js(f, "var m = new DOMMatrixReadOnly([1, 2, 3, 4, 5, 6]); var t = m.translate(1, 1);"
                + " [String(m), String(t), [t instanceof DOMMatrix, t === m, t.is2D].join()].join('|')"),
                "translate returns a new writable matrix: e = a*1 + c*1 + 5, f = b*1 + d*1 + 6");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix().translate())"), "no arguments translate by 0");
        Check.equal("false,3", js(f, "var t = new DOMMatrix().translate(1, 2, 3); [t.is2D, t.m43].join()"), "a z translation makes it 3D");
        Check.equal("matrix(2, 0, 0, 2, 0, 0)", js(f, "String(new DOMMatrix().scale(2))"), "scale with one argument is uniform");
        Check.equal("matrix(2, 0, 0, 3, 0, 0)", js(f, "String(new DOMMatrix().scale(2, 3))"), "scale with two arguments");
        Check.equal("matrix(2, 0, 0, 3, 0, 0)", js(f, "String(new DOMMatrix().scaleNonUniform(2, 3))"), "scaleNonUniform");
        Check.equal("matrix(2, 0, 0, 2, -5, -7)", js(f, "String(new DOMMatrix().scale(2, 2, 1, 5, 7, 0))"), "scale around the origin (5, 7): e = 5 - 2*5");
        Check.equal("false,2,3", js(f, "var m = new DOMMatrix().scale(2, 2, 3); [m.is2D, m.m11, m.m33].join()"), "scale with z other than 1");
        Check.equal("matrix3d(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1)", js(f, "String(new DOMMatrix().scale3d(2))"), "scale3d");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix().scale3d(1))"), "scale3d by 1 stays 2D");
        Check.equal("true", js(f, "var c = Math.cos(Math.PI / 6), s = Math.sin(Math.PI / 6); near(new DOMMatrix().rotate(30), new DOMMatrix([c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]))"), "rotate with one argument is around z");
        Check.equal("true,true", js(f, "var m = new DOMMatrix().rotate(30); [m.is2D, near(m, new DOMMatrix().rotate(0, 0, 30))].join()"), "rotate(0, 0, z) is the same and 2D");
        Check.equal("true", js(f, "near(new DOMMatrix().rotate(90, 0, 0), new DOMMatrix('rotateX(90deg)')) && near(new DOMMatrix().rotate(0, 90, 0), new DOMMatrix('rotateY(90deg)'))"),
                "rotate(x, y, z) with two or more arguments rotates around x, y and z");
        Check.equal("false", js(f, "new DOMMatrix().rotate(10, 0, 0).is2D + ''"), "a rotation around x is 3D");
        Check.equal("true", js(f, "near(new DOMMatrix().rotate(10, 20, 30), new DOMMatrix().rotate(0, 0, 30).multiply(new DOMMatrix().rotate(0, 20, 0)).multiply(new DOMMatrix().rotate(10, 0, 0)))"),
                "rotation order is z, then y, then x (each post-multiplied)");
        Check.equal("true", js(f, "near(new DOMMatrix().rotateFromVector(1, 1), new DOMMatrix().rotate(45)) && near(new DOMMatrix().rotateFromVector(0, 1), new DOMMatrix().rotate(90))"), "rotateFromVector");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix().rotateFromVector(0, 0))"), "the zero vector rotates by 0");
        Check.equal("true,true", js(f, "var a = new DOMMatrix().rotateAxisAngle(0, 0, 1, 90); [a.is2D, near(a, new DOMMatrix().rotate(90))].join()"), "rotateAxisAngle around z stays 2D");
        Check.equal("false,true", js(f, "var a = new DOMMatrix().rotateAxisAngle(1, 0, 0, 90); [a.is2D, near(a, new DOMMatrix('rotateX(90deg)'))].join()"), "around x");
        Check.equal("true", js(f, "var a = new DOMMatrix().rotateAxisAngle(1, 1, 1, 120); near(a, [0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1])"),
                "a third of a turn around the diagonal permutes the axes");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(new DOMMatrix().rotateAxisAngle(0, 0, 0, 90))"), "the zero axis does nothing");
        Check.equal("true,true", js(f, "var s = new DOMMatrix().skewX(45); [Math.abs(s.c - 1) < 1e-12, s.a === 1 && s.b === 0 && s.d === 1].join()"), "skewX");
        Check.equal("true,true", js(f, "var s = new DOMMatrix().skewY(45); [Math.abs(s.b - 1) < 1e-12, s.a === 1 && s.c === 0 && s.d === 1].join()"), "skewY");
        Check.equal("matrix(-1, 0, 0, 1, 0, 0)|matrix(1, 0, 0, -1, 0, 0)", js(f, "[String(new DOMMatrix().flipX()), String(new DOMMatrix().flipY())].join('|')"), "flipX and flipY");
        Check.equal("matrix(-2, 0, 0, 3, 5, 6)", js(f, "String(new DOMMatrix([2, 0, 0, 3, 5, 6]).flipX())"), "flipX flips the x axis of the point before the matrix: a = -2");
        Check.equal("matrix(3, 4, 1, 2, 5, 6)|matrix(3, 4, 1, 2, 5, 6)|matrix(2, 1, 4, 3, 6, 5)", js(f, "var a = new DOMMatrix([1, 2, 3, 4, 5, 6]); var b = new DOMMatrix([0, 1, 1, 0, 0, 0]);"
                + " [String(a.multiply(b)), String(a.multiply({a: 0, b: 1, c: 1, d: 0})), String(b.multiply(a))].join('|')"),
                "multiply post-multiplies (A x B is not B x A) and takes a dictionary or a matrix");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(new DOMMatrix([1, 2, 3, 4, 5, 6]).multiply())"), "multiply() with nothing is the identity");
        Check.equal("false", js(f, "new DOMMatrix().multiply(new DOMMatrix().translate(0, 0, 1)).is2D + ''"), "a 3D factor makes the product 3D");
    }

    /**
     * The mutable transformations change the receiver and return it, so they chain; the example of the specification gives the same strings.
     * <p>
     * 자신을 바꾸는 변환은 수신자를 바꾸고 반환하므로 이어 쓸 수 있다. 규격의 예제와 같은 문자열이 나온다.
     */
    public void testMutableTransformations() {
        PageFixture f = page();
        Check.equal("matrix(2, 0, 0, 2, 40, 40)", js(f, "var m = new DOMMatrix(); m.scaleSelf(2); m.translateSelf(20, 20); String(m)"), "the example of the specification");
        Check.equal("matrix3d(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1)", js(f, "var m = new DOMMatrix(); m.scale3dSelf(2); String(m)"), "the 3D example");
        Check.equal("true", js(f, "var m = new DOMMatrix(); var r = m.translateSelf(20, 20).scaleSelf(2).translateSelf(-20, -20); r === m && String(m) === 'matrix(2, 0, 0, 2, -20, -20)'"),
                "chaining returns the same object");
        Check.equal("true", js(f, "var m = new DOMMatrix([1, 2, 3, 4, 5, 6]); var r = [m.multiplySelf({a: 2}), m.preMultiplySelf({a: 2})]; r[0] === m && r[1] === m"),
                "multiplySelf and preMultiplySelf return the receiver");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)|matrix(2, 4, 6, 8, 5, 6)|matrix(2, 4, 6, 8, 10, 12)", js(f, "var a = new DOMMatrix([1, 2, 3, 4, 5, 6]);"
                + " var b = DOMMatrix.fromMatrix(a).multiplySelf({a: 2, d: 2}); var c = DOMMatrix.fromMatrix(a).preMultiplySelf({a: 2, d: 2});"
                + " [String(a), String(b), String(c)].join('|')"),
                "multiplySelf post-multiplies and preMultiplySelf pre-multiplies");
        Check.equal("true", js(f, "var m = new DOMMatrix(); m.rotateSelf(0, 0, 90); near(m, new DOMMatrix([0, 1, -1, 0, 0, 0]))"), "rotateSelf");
        Check.equal("true", js(f, "var m = new DOMMatrix(); m.rotateSelf(90); near(m, new DOMMatrix([0, 1, -1, 0, 0, 0]))"), "rotateSelf with one argument");
        Check.equal("true", js(f, "var m = new DOMMatrix(); m.rotateFromVectorSelf(1, 1).rotateAxisAngleSelf(0, 0, 1, 45); near(m, new DOMMatrix().rotate(90))"), "the rotation methods compose");
        Check.equal("true", js(f, "var m = new DOMMatrix(); m.skewXSelf(45).skewYSelf(45); near([m.a, m.b, m.c, m.d], [2, 1, 1, 1])"), "skew composes: skewX then skewY post-multiplied");
        Check.equal("false,3", js(f, "var m = new DOMMatrix(); m.translateSelf(0, 0, 3); [m.is2D, m.m43].join()"), "translateSelf with z");
        Check.equal("false", js(f, "var m = new DOMMatrix(); m.scaleSelf(1, 1, 2); m.is2D + ''"), "scaleSelf with z");
        Check.equal("false", js(f, "var m = new DOMMatrix(); m.rotateSelf(1, 0, 0); m.is2D + ''"), "rotateSelf around x");
        Check.equal("false,0", js(f, "var m = new DOMMatrix().translate(0, 0, 3); m.translateSelf(0, 0, -3); [m.is2D, m.m43].join()"), "a matrix that became 3D never becomes 2D by arithmetic");
        Check.equal("matrix(3, 0, 0, 3, 7, 8)|true", js(f, "var m = new DOMMatrix().translate(0, 0, 1); m.setMatrixValue('translate(7px, 8px) scale(3)'); [String(m), m.is2D].join('|')"),
                "setMatrixValue is the only way to become 2D again");
        Check.equal("SyntaxError|TypeError", js(f, "[err(function() { new DOMMatrix().setMatrixValue('nonsense'); }), err(function() { new DOMMatrix().setMatrixValue(); })].join('|')"),
                "setMatrixValue checks its argument");
        Check.equal("undefined,function", js(f, "[typeof DOMMatrixReadOnly.prototype.translateSelf, typeof DOMMatrix.prototype.translateSelf].join()"),
                "the mutable methods are not on the read-only interface");
    }

    /**
     * <code>inverse()</code> and <code>invertSelf()</code>: a 2D matrix is inverted as an affine transformation, a 3D matrix as a 4x4 matrix, and a
     * matrix without an inverse becomes sixteen NaN and is not 2D any more; such a matrix cannot be stringified.
     * <p>
     * <code>inverse()</code> 와 <code>invertSelf()</code>: 2D 행렬은 아핀 변환으로, 3D 행렬은 4x4 행렬로 뒤집고, 역행렬이 없는 행렬은 NaN 열여섯 개가 되어 2D 가 아니게 되며
     * 그런 행렬은 문자열로 바꿀 수 없다.
     */
    public void testInverse() {
        PageFixture f = page();
        Check.equal("matrix(0.5, 0, 0, 0.5, -5, -10)", js(f, "String(new DOMMatrix([2, 0, 0, 2, 10, 20]).inverse())"), "scale and translation");
        Check.equal("true,true", js(f, "var m = new DOMMatrix([1, 2, 3, 4, 5, 6]); var p = m.multiply(m.inverse()); [near(p, new DOMMatrix()), p.is2D].join()"), "m x m^-1 is the identity and stays 2D");
        Check.equal("true,false", js(f, "var m = new DOMMatrix().translate(1, 2, 3).rotate(10, 20, 30).scale(2, 3, 4); var p = m.multiply(m.inverse()); [near(p, new DOMMatrix()), m.inverse().is2D].join()"),
                "a general 3D matrix");
        Check.equal("true", js(f, "var m = new DOMMatrix('perspective(50px) rotateX(20deg) translate3d(5px, 6px, 7px)'); near(m.multiply(m.inverse()), new DOMMatrix())"), "with perspective");
        Check.equal("NaN,NaN,false", js(f, "var m = new DOMMatrix([1, 2, 2, 4, 0, 0]).inverse(); [m.a, m.m44, m.is2D].join()"), "a singular 2D matrix");
        Check.equal("NaN,false", js(f, "var m = new DOMMatrix([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]).inverse(); [m.m22, m.is2D].join()"), "a singular 3D matrix");
        Check.equal("InvalidStateError|InvalidStateError", js(f, "var m = new DOMMatrix([1, 2, 2, 4, 0, 0]).inverse(); [err(function() { String(m); }), err(function() { m + ''; })].join('|')"),
                "a matrix with NaN cannot be stringified");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "var m = new DOMMatrix(); String(m.invertSelf())"), "the identity");
        Check.equal("true", js(f, "var m = new DOMMatrix([2, 0, 0, 4, 0, 0]); m.invertSelf() === m && m.a === 0.5 && m.d === 0.25"), "invertSelf returns the receiver");
        Check.equal("InvalidStateError", js(f, "err(function() { String(new DOMMatrix([Infinity, 0, 0, 1, 0, 0])); })"), "infinity cannot be stringified either");
    }

    /**
     * Points: <code>transformPoint()</code> multiplies the column vector (x, y, z, w) by the matrix, a 2D matrix with a 2D point uses only the
     * 2D part, and <code>DOMPoint.matrixTransform()</code> does the same with the matrix as the argument.
     * <p>
     * 점: <code>transformPoint()</code> 는 열 벡터 (x, y, z, w) 에 행렬을 곱하고, 2D 행렬과 2D 점은 2D 부분만 쓰며, <code>DOMPoint.matrixTransform()</code> 은 행렬을
     * 인수로 받아 같은 일을 한다.
     */
    public void testPoints() {
        PageFixture f = page();
        Check.equal("20,18,0,1", js(f, "var p = new DOMPoint(5, 4); var q = p.matrixTransform(new DOMMatrix([2, 0, 0, 2, 10, 10])); [q.x, q.y, q.z, q.w].join()"),
                "the example of the specification");
        Check.equal("5,4", js(f, "var p = new DOMPoint(5, 4); p.matrixTransform(new DOMMatrix([2, 0, 0, 2, 10, 10])); [p.x, p.y].join()"), "the point is not modified");
        Check.equal("true,true", js(f, "var q = new DOMMatrix([2, 0, 0, 2, 10, 10]).transformPoint({x: 5, y: 4}); [q instanceof DOMPoint, q.x === 20 && q.y === 18 && q.z === 0 && q.w === 1].join()"),
                "transformPoint takes a dictionary and returns a DOMPoint");
        Check.equal("0,0,0,1", js(f, "var q = new DOMMatrix().transformPoint(); [q.x, q.y, q.z, q.w].join()"), "the default point is the origin with w 1");
        Check.equal("1,2,3,1", js(f, "var q = new DOMMatrix().translate(1, 2, 3).transformPoint(); [q.x, q.y, q.z, q.w].join()"), "a 3D translation");
        Check.equal("0,0,-1", js(f, "var q = new DOMMatrix('rotateY(90deg)').transformPoint({x: 1, y: 0, z: 0}); [Math.round(q.x), Math.round(q.y), Math.round(q.z)].join()"), "rotateY turns x into -z");
        Check.equal("7,13,0,0.5", js(f, "var q = new DOMMatrix([1, 0, 0, 1, 10, 20]).transformPoint({x: 2, y: 3, w: 0.5}); [q.x, q.y, q.z, q.w].join()"),
                "a point with w other than 1 is multiplied as a full vector (the translation counts w times)");
        Check.equal("Infinity,0,1", js(f, "var q = new DOMMatrix([1, 0, 0, 1, 0, 0]).transformPoint({x: Infinity, y: 0}); [q.x, q.z, q.w].join()"),
                "an infinite x does not turn z and w into NaN (a 2D matrix with a 2D point uses only a to f)");
        Check.equal("TypeError", js(f, "err(function() { new DOMMatrix().transformPoint(5); })"), "the point must be an object");
        Check.equal("TypeError", js(f, "err(function() { DOMPointReadOnly.prototype.matrixTransform.call({}); })"), "matrixTransform checks its receiver");
        Check.equal("true", js(f, "var q = new DOMPointReadOnly(1, 2, 3, 4).matrixTransform(); [q instanceof DOMPoint, q.x, q.y, q.z, q.w].join() === 'true,1,2,3,4'"), "matrixTransform with nothing is the identity");
    }

    /**
     * The dictionary arguments (<code>DOMMatrixInit</code>): <code>a</code> and <code>m11</code> must agree, <code>is2D: true</code> excludes 3D
     * members, 3D members make the matrix 3D, and <code>fromMatrix()</code> / the typed array factories create matrices.
     * <p>
     * 사전 인수(<code>DOMMatrixInit</code>): <code>a</code> 와 <code>m11</code> 은 일치해야 하고, <code>is2D: true</code> 는 3D 멤버와 함께 쓸 수 없으며, 3D 멤버는 행렬을 3D 로
     * 만들고, <code>fromMatrix()</code> 와 형식화 배열 팩토리가 행렬을 만든다.
     */
    public void testDictionaries() {
        PageFixture f = page();
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(DOMMatrix.fromMatrix())"), "fromMatrix() is the identity");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(DOMMatrix.fromMatrix({a: 1, b: 2, c: 3, d: 4, e: 5, f: 6}))"), "the short names");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(DOMMatrix.fromMatrix({m11: 1, m12: 2, m21: 3, m22: 4, m41: 5, m42: 6}))"), "the long names");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "String(DOMMatrix.fromMatrix({a: 1, m11: 1, b: 2, m12: 2, c: 3, d: 4, e: 5, f: 6}))"), "both names with the same value");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrix.fromMatrix({a: 1, m11: 2}); })"), "conflicting names");
        Check.equal("none", js(f, "err(function() { DOMMatrix.fromMatrix({e: 0, m41: -0}); })"), "0 and -0 are the same value (SameValueZero)");
        Check.equal("none", js(f, "err(function() { DOMMatrix.fromMatrix({f: NaN, m42: NaN}); })"), "NaN equals NaN");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrix.fromMatrix({is2D: true, m13: 1}); })"), "is2D true with a 3D member");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrix.fromMatrix({is2D: true, m33: 2}); })"), "m33 is 1 in the identity");
        Check.equal("none", js(f, "err(function() { DOMMatrix.fromMatrix({is2D: true, m13: -0, m33: 1}); })"), "identity values of 3D members are fine");
        Check.equal("false,1,2", js(f, "var m = DOMMatrix.fromMatrix({m13: 1, m11: 2}); [m.is2D, m.m13, m.m11].join()"), "a 3D member makes the matrix 3D");
        Check.equal("false", js(f, "DOMMatrix.fromMatrix({is2D: false}).is2D + ''"), "is2D false on a dictionary without 3D members");
        Check.equal("true", js(f, "DOMMatrix.fromMatrix({m13: 0}).is2D + ''"), "a 3D member at its identity value stays 2D");
        Check.equal("TypeError", js(f, "err(function() { DOMMatrix.fromMatrix(5); })"), "a number is not a dictionary");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "String(DOMMatrix.fromMatrix(null))"), "null is the empty dictionary");
        Check.equal("matrix(2, 4, 6, 8, 10, 12)", js(f, "String(DOMMatrix.fromMatrix(new DOMMatrix([2, 4, 6, 8, 10, 12])))"), "another matrix is a dictionary");
        Check.equal("false,true", js(f, "var r = DOMMatrixReadOnly.fromMatrix({a: 3}); [r instanceof DOMMatrix, r instanceof DOMMatrixReadOnly].join()"), "the read-only factory");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)|matrix(1, 2, 3, 4, 5, 6)", js(f, "[String(DOMMatrix.fromFloat32Array(new Float32Array([1, 2, 3, 4, 5, 6]))), String(DOMMatrix.fromFloat64Array(new Float64Array([1, 2, 3, 4, 5, 6])))].join('|')"),
                "typed arrays with six elements");
        Check.equal("false,15", js(f, "var m = DOMMatrix.fromFloat64Array(new Float64Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 15])); [m.is2D, m.m44].join()"), "typed arrays with sixteen elements");
        Check.equal("TypeError|TypeError|TypeError", js(f, "[err(function() { DOMMatrix.fromFloat32Array(new Float32Array(5)); }), err(function() { DOMMatrix.fromFloat32Array(new Float64Array(6)); }),"
                + " err(function() { DOMMatrix.fromFloat64Array([1, 2, 3, 4, 5, 6]); })].join('|')"), "wrong length, wrong type, not a typed array");
        Check.equal("16,Float32Array,1,2,0,0,3,4,0,0,0,0,1,0,5,6,0,1", js(f, "var a = new DOMMatrix([1, 2, 3, 4, 5, 6]).toFloat32Array(); [a.length, a.constructor.name, Array.prototype.join.call(a)].join()"),
                "toFloat32Array lists m11 to m44 in column-major order");
        Check.equal("16,Float64Array,true", js(f, "var m = new DOMMatrix().translate(0.1, 0.2, 0.3); var a = m.toFloat64Array(); [a.length, a.constructor.name, a[12] === 0.1 && a[13] === 0.2 && a[14] === 0.3].join()"),
                "toFloat64Array keeps doubles");
        Check.equal("0.10000000149011612", js(f, "String(new DOMMatrix().translate(0.1, 0, 0).toFloat32Array()[12])"), "toFloat32Array rounds to float");
    }

    /**
     * <code>toJSON()</code> returns a plain object with the 2D aliases, the sixteen elements, <code>is2D</code> and <code>isIdentity</code>;
     * the stringifier writes <code>matrix()</code> or <code>matrix3d()</code>.
     * <p>
     * <code>toJSON()</code> 은 2D 별칭, 열여섯 요소, <code>is2D</code>, <code>isIdentity</code> 를 가진 일반 객체를 반환하고, 문자열 변환은 <code>matrix()</code> 나
     * <code>matrix3d()</code> 를 쓴다.
     */
    public void testSerialization() {
        PageFixture f = page();
        Check.equal("a,b,c,d,e,f,m11,m12,m13,m14,m21,m22,m23,m24,m31,m32,m33,m34,m41,m42,m43,m44,is2D,isIdentity", js(f, "Object.keys(new DOMMatrix().toJSON()).join()"), "the keys of toJSON");
        Check.equal("{\"a\":1,\"b\":2,\"c\":3,\"d\":4,\"e\":5,\"f\":6,\"m11\":1,\"m12\":2,\"m13\":0,\"m14\":0,\"m21\":3,\"m22\":4,\"m23\":0,\"m24\":0,\"m31\":0,\"m32\":0,\"m33\":1,\"m34\":0,\"m41\":5,\"m42\":6,\"m43\":0,\"m44\":1,\"is2D\":true,\"isIdentity\":false}",
                js(f, "JSON.stringify(new DOMMatrix([1, 2, 3, 4, 5, 6]))"), "JSON.stringify uses toJSON");
        Check.equal("matrix3d(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 2, 3, 1)", js(f, "String(new DOMMatrix().translate(1, 2, 3))"), "a 3D matrix is written as matrix3d");
        Check.equal("matrix(0.5, 0, 0, 0.5, 1.5, -2.25)", js(f, "String(new DOMMatrix([0.5, 0, 0, 0.5, 1.5, -2.25]))"), "numbers use the JavaScript number format");
        Check.equal("matrix(1e-7, 0, 0, 1, 1e+21, 0)", js(f, "String(new DOMMatrix([1e-7, 0, 0, 1, 1e21, 0]))"), "also the exponent form");
        Check.equal("string,matrix(1, 0, 0, 1, 0, 0)", js(f, "var m = new DOMMatrix(); [typeof m.toString(), m + ''].join()"), "the stringifier");
    }
}
