package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * The declarations of one scope, computed by the parser (early errors for duplicates are reported there) and used by the
 * interpreter to create bindings on scope entry: hoisted <code>var</code> names, function declarations to initialize
 * (hoisting), and lexical declarations that start in the temporal dead zone.
 * <p>
 * 한 스코프의 선언 정보. 파서가 계산하며(중복에 대한 조기 오류는 파서가 보고) 인터프리터가 스코프 진입 시 바인딩을 만드는 데 쓴다:
 * 끌어올려지는 <code>var</code> 이름, 초기화할 함수 선언(호이스팅), 일시적 사각지대에서 시작하는 렉시컬 선언.
 */
public final class ScopeInfo {

    /**
     * An empty array of names.
     * <p>
     * 이름의 빈 배열.
     */
    public static final String[] NO_NAMES = new String[0];

    /**
     * Names declared with var (and top-level function names, and Annex B hoisted block functions), in order of appearance.
     * Only set for function, script and module scopes.
     * <p>
     * var 로 선언된 이름(최상위 함수 이름과 부속서 B 로 끌어올려진 블록 함수 포함). 나타난 순서. 함수, 스크립트, 모듈 스코프에서만 설정된다.
     */
    public String[] varNames = NO_NAMES;

    /**
     * Function declarations (FUNCTION_DECL nodes) to instantiate when the scope is entered.
     * <p>
     * 스코프에 들어갈 때 인스턴스화할 함수 선언(FUNCTION_DECL 노드).
     */
    public Node[] functions = new Node[0];

    /**
     * Names of let, const, class and import declarations in this scope (block-level function names are not included;
     * they are created from {@link #functions}).
     * <p>
     * 이 스코프의 let, const, class, import 선언 이름(블록 수준 함수 이름은 {@link #functions} 에서 만들어지므로 포함하지 않는다).
     */
    public String[] lexNames = NO_NAMES;

    /**
     * For each entry of {@link #lexNames}: true if it is immutable (const, class inner binding of declarations is mutable,
     * import bindings are immutable).
     * <p>
     * {@link #lexNames} 의 각 항목에 대해 불변(const, import 바인딩)이면 true. 클래스 선언의 바깥 바인딩은 가변이다.
     */
    public boolean[] lexConst = new boolean[0];

    /**
     * True if the scope contains a direct call to eval (the interpreter may then need full environments).
     * <p>
     * 스코프에 eval 직접 호출이 있으면 true(인터프리터가 전체 환경을 유지해야 할 수 있다).
     */
    public boolean hasDirectEval;
}
