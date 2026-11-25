package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * Everything the interpreter needs to know about a function, class constructor, method or arrow function, computed by
 * the parser: parameters, body, kind flags, strictness and the scope of the body.
 * <p>
 * 인터프리터가 함수, 클래스 생성자, 메서드, 화살표 함수에 대해 알아야 하는 모든 정보. 파서가 계산한다: 매개변수, 본문, 종류 플래그,
 * 엄격 여부, 본문 스코프.
 */
public final class FunctionInfo {

    /**
     * The function name or the empty string for anonymous functions.
     * <p>
     * 함수 이름. 익명 함수이면 빈 문자열.
     */
    public String name = "";

    /**
     * The parameters: IDENT, DEFAULT, REST or pattern nodes.
     * <p>
     * 매개변수: IDENT, DEFAULT, REST, 패턴 노드.
     */
    public Node[] params = new Node[0];

    /**
     * The body: a BLOCK node, or an expression node when {@link #exprBody} is set.
     * <p>
     * 본문: BLOCK 노드. {@link #exprBody} 이면 표현식 노드.
     */
    public Node body;

    /**
     * True for the concise body of an arrow function (<code>x =&gt; x + 1</code>).
     * <p>
     * 화살표 함수의 간결한 본문(<code>x =&gt; x + 1</code>)이면 true.
     */
    public boolean exprBody;

    /**
     * True for arrow functions (lexical this, arguments, super, new.target).
     * <p>
     * 화살표 함수이면 true(this, arguments, super, new.target 이 렉시컬).
     */
    public boolean arrow;

    /**
     * True for generator functions and methods.
     * <p>
     * 제너레이터 함수와 메서드이면 true.
     */
    public boolean generator;

    /**
     * True for async functions, async generators, async methods and async arrows.
     * <p>
     * async 함수, async 제너레이터, async 메서드와 async 화살표이면 true.
     */
    public boolean async;

    /**
     * True for concise methods, getters, setters and class methods (they have a home object and no prototype).
     * <p>
     * 간결한 메서드, getter, setter, 클래스 메서드이면 true(홈 객체가 있고 prototype 이 없다).
     */
    public boolean method;

    /**
     * True for the constructor of a class.
     * <p>
     * 클래스의 생성자이면 true.
     */
    public boolean classConstructor;

    /**
     * True for the constructor of a class with an extends clause.
     * <p>
     * extends 절이 있는 클래스의 생성자이면 true.
     */
    public boolean derived;

    /**
     * True if the code is strict mode code.
     * <p>
     * 엄격 모드 코드이면 true.
     */
    public boolean strict;

    /**
     * True if every parameter is a plain identifier (no defaults, rest or patterns).
     * <p>
     * 모든 매개변수가 단순 식별자(기본값, 나머지, 패턴 없음)이면 true.
     */
    public boolean simpleParams = true;

    /**
     * True if parameter defaults or patterns contain expressions (the body then gets its own var environment).
     * <p>
     * 매개변수 기본값이나 패턴에 표현식이 있으면 true(그러면 본문이 별도의 var 환경을 가진다).
     */
    public boolean hasParamExpressions;

    /**
     * The number of parameters before the first default or rest (the function's length property).
     * <p>
     * 첫 기본값이나 나머지 앞의 매개변수 수(함수의 length 프로퍼티).
     */
    public int length;

    /**
     * The bound names of all parameters in order.
     * <p>
     * 모든 매개변수의 바인딩 이름(순서대로).
     */
    public String[] paramNames = ScopeInfo.NO_NAMES;

    /**
     * True if the body (or an arrow function inside it) refers to <code>arguments</code>.
     * <p>
     * 본문(또는 그 안의 화살표 함수)이 <code>arguments</code> 를 참조하면 true.
     */
    public boolean usesArguments;

    /**
     * True if the code contains a direct call to eval.
     * <p>
     * 코드에 eval 직접 호출이 있으면 true.
     */
    public boolean hasDirectEval;

    /**
     * The scope of the function body (var names, hoisted functions, lexical declarations).
     * <p>
     * 함수 본문의 스코프(var 이름, 끌어올릴 함수, 렉시컬 선언).
     */
    public ScopeInfo scope = new ScopeInfo();

    /**
     * The source text of the whole script or module this function belongs to.
     * <p>
     * 이 함수가 속한 스크립트 또는 모듈의 전체 소스 텍스트.
     */
    public String source;

    /**
     * The name of the source (file name or URL).
     * <p>
     * 소스 이름(파일 이름이나 URL).
     */
    public String sourceName;

    /**
     * Offset of the first character of the function in {@link #source}.
     * <p>
     * {@link #source} 에서 함수 첫 글자의 오프셋.
     */
    public int start;

    /**
     * Offset after the last character of the function in {@link #source}.
     * <p>
     * {@link #source} 에서 함수 마지막 글자 다음의 오프셋.
     */
    public int end;

    /**
     * Returns the source text of the function as shown by Function.prototype.toString.
     * <p>
     * Function.prototype.toString 이 보여 주는 함수의 소스 텍스트를 반환한다.
     *
     * @return the source text / 소스 텍스트
     */
    public String sourceText() {
        if (source == null || end <= start || end > source.length()) {
            return "function " + name + "() { [native code] }";
        }
        return source.substring(start, end);
    }
}
