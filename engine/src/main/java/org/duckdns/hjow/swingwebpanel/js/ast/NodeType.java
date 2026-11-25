package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * The node kinds of the syntax tree. Each constant documents how the generic fields of {@link Node} are used:
 * <code>a b c d</code> (child nodes), <code>list</code> (child list), <code>name</code>, <code>value</code>, <code>op</code>,
 * <code>flag</code>, <code>fn</code> (function description) and <code>scope</code> (scope description).
 * Operators and property kinds are the integer constants of {@link Op}.
 * <p>
 * 구문 트리의 노드 종류. 각 상수는 {@link Node} 의 범용 필드를 어떻게 쓰는지 설명한다:
 * <code>a b c d</code>(자식 노드), <code>list</code>(자식 목록), <code>name</code>, <code>value</code>, <code>op</code>,
 * <code>flag</code>, <code>fn</code>(함수 설명), <code>scope</code>(스코프 설명).
 * 연산자와 프로퍼티 종류는 {@link Op} 의 정수 상수이다.
 */
public final class NodeType {

    /** Private constructor: constants holder. / 비공개 생성자: 상수 보관용 클래스. */
    private NodeType() {
    }

    // ---------------------------------------------------------------- expressions

    /** Literal number, string, boolean or null; value holds the JsValue. / 숫자, 문자열, 불리언, null 리터럴. value 에 JsValue. */
    public static final int LITERAL = 1;
    /** Identifier reference or binding name; name is the identifier. / 식별자 참조 또는 바인딩 이름. name 이 식별자. */
    public static final int IDENT = 2;
    /** The this keyword. / this 키워드. */
    public static final int THIS = 3;
    /** The super keyword, only as callee of CALL or object of MEMBER. / super 키워드. CALL 의 callee 나 MEMBER 의 object 로만 쓴다. */
    public static final int SUPER = 4;
    /** The new.target meta property. / new.target 메타 프로퍼티. */
    public static final int NEW_TARGET = 5;
    /** Template literal; list = substitution expressions, value = TemplateParts. / 템플릿 리터럴. list 는 치환식, value 는 TemplateParts. */
    public static final int TEMPLATE = 6;
    /** Tagged template; a = tag expression, b = TEMPLATE node. / 태그 템플릿. a 는 태그 식, b 는 TEMPLATE 노드. */
    public static final int TAGGED_TEMPLATE = 7;
    /** Regular expression literal; name = pattern, value = flags string. / 정규식 리터럴. name 은 패턴, value 는 플래그 문자열. */
    public static final int REGEXP = 8;
    /** Array literal; list = elements (null for a hole, SPREAD for spread). / 배열 리터럴. list 는 요소(빈 칸은 null, 전개는 SPREAD). */
    public static final int ARRAY = 9;
    /** Object literal; list = PROPERTY nodes. / 객체 리터럴. list 는 PROPERTY 노드들. */
    public static final int OBJECT = 10;
    /** Object literal property; a = key, b = value or FUNCTION, op = Op.PROP_*, flag = computed key. / 객체 리터럴 프로퍼티. a 키, b 값 또는 FUNCTION, op 는 Op.PROP_*, flag 는 계산된 키. */
    public static final int PROPERTY = 11;
    /** Function expression or declaration body description; fn = FunctionInfo. / 함수 표현식. fn 이 FunctionInfo. */
    public static final int FUNCTION = 12;
    /** Class expression; name = class name or null, a = heritage, b = constructor FUNCTION or null, list = CLASS_MEMBER, value = source text. / 클래스 표현식. name 은 클래스 이름, a 는 상속식, b 는 생성자 FUNCTION, list 는 CLASS_MEMBER, value 는 소스 텍스트. */
    public static final int CLASS = 13;
    /** Class member; a = key, b = FUNCTION, op = Op.PROP_METHOD/GET/SET, flag = static, value = Boolean.TRUE if computed. / 클래스 멤버. a 키, b FUNCTION, op 종류, flag 는 static, value 가 TRUE 이면 계산된 키. */
    public static final int CLASS_MEMBER = 14;
    /** Unary operator; op = Op.NEG/POS/NOT/BITNOT/TYPEOF/VOID/DELETE, a = operand. / 단항 연산자. op 연산자, a 피연산자. */
    public static final int UNARY = 15;
    /** ++ or --; op = Op.INC/DEC, flag = prefix, a = target. / ++ 또는 --. op 연산자, flag 는 전위, a 는 대상. */
    public static final int UPDATE = 16;
    /** Binary operator; op = Op.*, a = left, b = right. / 이항 연산자. op 연산자, a 왼쪽, b 오른쪽. */
    public static final int BINARY = 17;
    /** Short-circuit operator; op = Op.LOGAND/LOGOR, a = left, b = right. / 단락 평가 연산자. op 연산자, a 왼쪽, b 오른쪽. */
    public static final int LOGICAL = 18;
    /** Assignment; op = 0 for = or the Op of a compound operator, a = target (expression or pattern), b = value. / 대입. op 는 = 이면 0, 복합이면 연산자. a 대상, b 값. */
    public static final int ASSIGN = 19;
    /** Conditional expression a ? b : c. / 조건 표현식 a ? b : c. */
    public static final int CONDITIONAL = 20;
    /** Call; a = callee, list = arguments (SPREAD allowed). / 호출. a 는 callee, list 는 인수(SPREAD 허용). */
    public static final int CALL = 21;
    /** new expression; a = callee, list = arguments. / new 표현식. a 는 callee, list 는 인수. */
    public static final int NEW = 22;
    /** Member access; a = object, name = property name when not computed, b = key expression when flag (computed) is set. / 멤버 접근. a 객체, 계산되지 않으면 name, flag 이면 b 가 키 식. */
    public static final int MEMBER = 23;
    /** Comma expression; list = expressions. / 쉼표 표현식. list 는 식들. */
    public static final int SEQUENCE = 24;
    /** Spread element or argument; a = expression. / 전개 요소나 인수. a 는 식. */
    public static final int SPREAD = 25;
    /** yield expression; a = operand or null, flag = delegating yield*. / yield 표현식. a 는 피연산자 또는 null, flag 는 yield*. */
    public static final int YIELD = 26;
    /** Optional chain; a = the complete optional member/call expression. / optional chain. a 는 전체 선택적 멤버/호출 식. */
    public static final int OPTIONAL_CHAIN = 27;
    /** Await expression; a = UnaryExpression operand. / await 식. a 는 단항 피연산자. */
    public static final int AWAIT = 28;

    // ---------------------------------------------------------------- patterns

    /** Array destructuring pattern; list = element patterns (null = elision, REST last). / 배열 구조 분해 패턴. list 는 요소 패턴(null 은 건너뜀, REST 는 마지막). */
    public static final int ARRAY_PATTERN = 30;
    /** Object destructuring pattern; list = PATTERN_PROP nodes (last may be none). / 객체 구조 분해 패턴. list 는 PATTERN_PROP 들. */
    public static final int OBJECT_PATTERN = 31;
    /** Property of an object pattern; a = key, b = target pattern (maybe DEFAULT), flag = computed key. / 객체 패턴 프로퍼티. a 키, b 대상 패턴(DEFAULT 가능), flag 계산된 키. */
    public static final int PATTERN_PROP = 32;
    /** Rest element in a pattern or parameter list; a = target. / 패턴이나 매개변수 목록의 나머지 요소. a 는 대상. */
    public static final int REST = 33;
    /** Target with default value; a = target pattern, b = default expression. / 기본값이 있는 대상. a 대상 패턴, b 기본값 식. */
    public static final int DEFAULT = 34;

    // ---------------------------------------------------------------- statements

    /** Whole script or module; list = statements, scope = top-level scope, value = ProgramInfo. / 스크립트 또는 모듈 전체. list 는 문장, scope 는 최상위 스코프, value 는 ProgramInfo. */
    public static final int PROGRAM = 40;
    /** var, let or const declaration; op = Op.VAR/LET/CONST, list = DECLARATOR nodes. / var, let, const 선언. op 는 종류, list 는 DECLARATOR. */
    public static final int VAR_DECL = 41;
    /** One declarator; a = target (IDENT or pattern), b = initializer or null. / 선언자 하나. a 대상(IDENT 또는 패턴), b 초기값 또는 null. */
    public static final int DECLARATOR = 42;
    /** Expression statement; a = expression. / 표현식 문장. a 는 식. */
    public static final int EXPR_STMT = 43;
    /** Block; list = statements, scope = block scope or null. / 블록. list 는 문장, scope 는 블록 스코프 또는 null. */
    public static final int BLOCK = 44;
    /** Empty statement. / 빈 문장. */
    public static final int EMPTY = 45;
    /** if statement; a = test, b = then, c = else or null. / if 문. a 조건, b then, c else 또는 null. */
    public static final int IF = 46;
    /** for(;;) loop; a = init or null, b = test or null, c = update or null, d = body, scope = loop scope for let. / for(;;) 반복. a 초기화, b 조건, c 갱신, d 본문, scope 는 let 용 스코프. */
    public static final int FOR = 47;
    /** for-in loop; a = left (VAR_DECL or target), b = object expression, d = body, scope = loop scope. / for-in 반복. a 좌변, b 객체식, d 본문, scope 반복 스코프. */
    public static final int FOR_IN = 48;
    /** for-of loop; a = left (VAR_DECL or target), b = iterable expression, d = body, scope = loop scope. / for-of 반복. a 좌변, b 반복 가능식, d 본문, scope 반복 스코프. */
    public static final int FOR_OF = 49;
    /** while loop; a = test, d = body. / while 반복. a 조건, d 본문. */
    public static final int WHILE = 50;
    /** do-while loop; d = body, a = test. / do-while 반복. d 본문, a 조건. */
    public static final int DO_WHILE = 51;
    /** return statement; a = value or null. / return 문. a 는 값 또는 null. */
    public static final int RETURN = 52;
    /** break statement; name = label or null. / break 문. name 은 레이블 또는 null. */
    public static final int BREAK = 53;
    /** continue statement; name = label or null. / continue 문. name 은 레이블 또는 null. */
    public static final int CONTINUE = 54;
    /** throw statement; a = value. / throw 문. a 는 값. */
    public static final int THROW = 55;
    /** try statement; a = block, b = catch parameter pattern or null, c = catch block or null, d = finally block or null, scope = catch scope. / try 문. a 블록, b catch 매개변수, c catch 블록, d finally 블록, scope catch 스코프. */
    public static final int TRY = 56;
    /** switch statement; a = discriminant, list = CASE nodes, scope = case block scope. / switch 문. a 판별식, list 는 CASE, scope 는 case 블록 스코프. */
    public static final int SWITCH = 57;
    /** switch case; a = test or null for default, list = statements. / switch 의 case. a 는 조건(default 이면 null), list 는 문장. */
    public static final int CASE = 58;
    /** Labelled statement; name = label, a = statement. / 레이블 문장. name 은 레이블, a 는 문장. */
    public static final int LABELED = 59;
    /** with statement; a = object, b = body. / with 문. a 객체, b 본문. */
    public static final int WITH = 60;
    /** The debugger statement. / debugger 문장. */
    public static final int DEBUGGER = 61;
    /** Function declaration; a = IDENT of the name, b = FUNCTION node, flag = Annex B hoisting to a var binding. / 함수 선언. a 는 이름 IDENT, b 는 FUNCTION, flag 는 var 로 끌어올림(부속서 B). */
    public static final int FUNCTION_DECL = 62;
    /** Class declaration; a = IDENT of the name, b = CLASS node. / 클래스 선언. a 는 이름 IDENT, b 는 CLASS 노드. */
    public static final int CLASS_DECL = 63;
    /** import declaration; name = module specifier; the bindings are described by ProgramInfo. / import 선언. name 은 모듈 지정자. 바인딩은 ProgramInfo 가 설명. */
    public static final int IMPORT = 64;
    /** export declaration (named, default or star); a = declaration or expression or null, name = module specifier or null. / export 선언(이름, 기본, 별표). a 는 선언 또는 식, name 은 모듈 지정자. */
    public static final int EXPORT = 65;
}
