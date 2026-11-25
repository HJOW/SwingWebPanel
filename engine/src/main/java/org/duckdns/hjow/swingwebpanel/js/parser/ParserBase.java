package org.duckdns.hjow.swingwebpanel.js.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;

/**
 * Infrastructure shared by the parser classes: the token cursor with one token of lookahead, error reporting, the function
 * context (strictness, generator, labels), the scope tracker that detects redeclaration errors and computes
 * {@link ScopeInfo}, and the reserved word rules. The grammar itself lives in {@link ExpressionParser} and {@link Parser}.
 * <p>
 * 파서 클래스들이 공유하는 기반: 한 토큰 미리 보기를 가진 토큰 커서, 오류 보고, 함수 문맥(엄격 여부, 제너레이터, 레이블),
 * 재선언 오류를 찾고 {@link ScopeInfo} 를 계산하는 스코프 추적기, 예약어 규칙. 문법 자체는 {@link ExpressionParser} 와
 * {@link Parser} 에 있다.
 */
abstract class ParserBase {

    /**
     * Scope kind: function body (also used for the top level of scripts and modules).
     * <p>
     * 스코프 종류: 함수 본문(스크립트와 모듈의 최상위에도 사용).
     */
    static final int SCOPE_FUNCTION = 0;

    /**
     * Scope kind: block, for-loop head, switch body or catch clause.
     * <p>
     * 스코프 종류: 블록, for 머리, switch 본문, catch 절.
     */
    static final int SCOPE_BLOCK = 1;

    /**
     * Binding kind for declarations: var.
     * <p>
     * 선언 바인딩 종류: var.
     */
    static final int BIND_VAR = 0;

    /**
     * Binding kind: let.
     * <p>
     * 바인딩 종류: let.
     */
    static final int BIND_LET = 1;

    /**
     * Binding kind: const.
     * <p>
     * 바인딩 종류: const.
     */
    static final int BIND_CONST = 2;

    /**
     * Binding kind: function parameter.
     * <p>
     * 바인딩 종류: 함수 매개변수.
     */
    static final int BIND_PARAM = 3;

    /**
     * Binding kind: catch parameter.
     * <p>
     * 바인딩 종류: catch 매개변수.
     */
    static final int BIND_CATCH = 4;

    /**
     * Binding kind: no declaration (arrow parameter lists parsed speculatively and similar).
     * <p>
     * 바인딩 종류: 선언 없음(추측 파싱한 화살표 매개변수 등).
     */
    static final int BIND_NONE = 5;

    /**
     * A label of a labelled statement.
     * <p>
     * 레이블 문장의 레이블.
     */
    static final class LabelRec {
        /**
         * The label name.
         * <p>
         * 레이블 이름.
         */
        final String name;
        /**
         * True if the labelled statement is an iteration statement (continue may target it).
         * <p>
         * 레이블이 붙은 문장이 반복문이면 true(continue 의 대상이 될 수 있다).
         */
        boolean loop;

        /**
         * Creates a label record.
         * <p>
         * 레이블 기록을 만든다.
         *
         * @param name the label / 레이블
         */
        LabelRec(String name) {
            this.name = name;
        }
    }

    /**
     * A candidate for Annex B.3.3 function-in-block hoisting.
     * <p>
     * 부속서 B.3.3 의 블록 함수 끌어올림 후보.
     */
    static final class Candidate {
        /**
         * The function name.
         * <p>
         * 함수 이름.
         */
        final String name;
        /**
         * The FUNCTION_DECL node.
         * <p>
         * FUNCTION_DECL 노드.
         */
        final Node decl;
        /**
         * The scopes between the block and the function scope (the block itself is excluded).
         * <p>
         * 블록과 함수 스코프 사이의 스코프들(블록 자신은 제외).
         */
        final List<Scope> chain;

        /**
         * Creates a candidate.
         * <p>
         * 후보를 만든다.
         *
         * @param name  function name / 함수 이름
         * @param decl  declaration node / 선언 노드
         * @param chain enclosing scopes / 바깥 스코프들
         */
        Candidate(String name, Node decl, List<Scope> chain) {
            this.name = name;
            this.decl = decl;
            this.chain = chain;
        }
    }

    /**
     * Declarations of one scope while parsing.
     * <p>
     * 파싱 중 한 스코프의 선언들.
     */
    static final class Scope {
        /**
         * The enclosing scope.
         * <p>
         * 바깥 스코프.
         */
        final Scope parent;
        /**
         * The scope kind.
         * <p>
         * 스코프 종류.
         */
        final int kind;
        /**
         * Lexically declared names with their const flag.
         * <p>
         * 렉서컬 선언 이름과 const 여부.
         */
        final Map<String, Boolean> lex = new LinkedHashMap<String, Boolean>();
        /**
         * Names of var declarations made in or passing through this scope.
         * <p>
         * 이 스코프 안에서 또는 이 스코프를 거쳐 선언된 var 이름들.
         */
        final Set<String> vars = new HashSet<String>();
        /**
         * Hoisted names in order (function-level scopes).
         * <p>
         * 끌어올려진 이름들의 순서(함수 수준 스코프).
         */
        final Set<String> varOrder = new LinkedHashSet<String>();
        /**
         * Names of block-level function declarations (they are initialized from the function list, not from lexNames).
         * <p>
         * 블록 수준 함수 선언의 이름들(lexNames 가 아니라 함수 목록에서 초기화된다).
         */
        final Set<String> funcNames = new HashSet<String>();
        /**
         * Function declarations to instantiate on scope entry.
         * <p>
         * 스코프 진입 시 인스턴스화할 함수 선언들.
         */
        final List<Node> functions = new ArrayList<Node>();
        /**
         * Parameter names (function scopes).
         * <p>
         * 매개변수 이름(함수 스코프).
         */
        final Set<String> params = new HashSet<String>();
        /**
         * Name of a simple catch parameter, which a var declaration may redeclare (Annex B.3.5).
         * <p>
         * var 선언이 다시 선언할 수 있는 단순 catch 매개변수 이름(부속서 B.3.5).
         */
        String catchSimple;
        /**
         * True for module top-level scopes where function declarations are lexical.
         * <p>
         * 함수 선언이 렉시컬인 모듈 최상위 스코프이면 true.
         */
        boolean moduleTop;
        /**
         * Annex B hoisting candidates (function-level scopes).
         * <p>
         * 부속서 B 끌어올림 후보(함수 수준 스코프).
         */
        final List<Candidate> candidates = new ArrayList<Candidate>();
        /**
         * True if a direct eval call occurs in this scope.
         * <p>
         * 이 스코프에서 eval 직접 호출이 있으면 true.
         */
        boolean hasEval;

        /**
         * Creates a scope.
         * <p>
         * 스코프를 만든다.
         *
         * @param parent the enclosing scope / 바깥 스코프
         * @param kind   the kind / 종류
         */
        Scope(Scope parent, int kind) {
            this.parent = parent;
            this.kind = kind;
        }
    }

    /**
     * The context of the function (or script) being parsed.
     * <p>
     * 파싱 중인 함수(또는 스크립트)의 문맥.
     */
    static final class FuncCtx {
        /**
         * The enclosing function context.
         * <p>
         * 바깥 함수 문맥.
         */
        FuncCtx parent;
        /**
         * Strict mode flag.
         * <p>
         * 엄격 모드 여부.
         */
        boolean strict;
        /**
         * True inside generators (yield is an operator).
         * <p>
         * 제너레이터 안이면 true(yield 는 연산자).
         */
        boolean generator;
        /**
         * True inside async functions and async generators (await is an operator).
         * <p>
         * async 함수와 async 제너레이터 안이면 true(await 는 연산자).
         */
        boolean async;
        /**
         * True for arrow functions.
         * <p>
         * 화살표 함수이면 true.
         */
        boolean arrow;
        /**
         * True inside any function (return is allowed).
         * <p>
         * 함수 안이면 true(return 허용).
         */
        boolean inFunction;
        /**
         * True where new.target is allowed.
         * <p>
         * new.target 을 쓸 수 있으면 true.
         */
        boolean allowNewTarget;
        /**
         * True where super.property is allowed.
         * <p>
         * super.프로퍼티 를 쓸 수 있으면 true.
         */
        boolean allowSuperProp;
        /**
         * True where super() is allowed (derived constructors).
         * <p>
         * super() 를 쓸 수 있으면 true(파생 생성자).
         */
        boolean allowSuperCall;
        /**
         * True while the parameter list is being parsed (yield expressions are errors there).
         * <p>
         * 매개변수 목록을 파싱하는 동안 true(여기서 yield 식은 오류).
         */
        boolean inParams;
        /**
         * Set when the code refers to arguments.
         * <p>
         * 코드가 arguments 를 참조하면 설정된다.
         */
        boolean usesArguments;
        /**
         * Set when the code contains a direct eval call.
         * <p>
         * 코드에 eval 직접 호출이 있으면 설정된다.
         */
        boolean hasEval;
        /**
         * The labels in scope.
         * <p>
         * 유효한 레이블들.
         */
        List<LabelRec> labels = new ArrayList<LabelRec>();
        /**
         * Nesting depth of iteration statements (break and continue).
         * <p>
         * 반복문 중첩 깊이(break, continue 용).
         */
        int loopDepth;
        /**
         * Nesting depth of switch statements (break).
         * <p>
         * switch 중첩 깊이(break 용).
         */
        int switchDepth;
        /**
         * The function scope.
         * <p>
         * 함수 스코프.
         */
        Scope funcScope;
    }

    /**
     * The lexer.
     * <p>
     * 렉서.
     */
    protected final Lexer lexer;

    /**
     * The source text.
     * <p>
     * 소스 텍스트.
     */
    protected final String source;

    /**
     * The source name.
     * <p>
     * 소스 이름.
     */
    protected final String sourceName;

    /**
     * True for the Module goal.
     * <p>
     * Module 목표이면 true.
     */
    protected final boolean moduleGoal;

    /**
     * The current (not yet consumed) token.
     * <p>
     * 현재(아직 소비하지 않은) 토큰.
     */
    protected Token tok;

    /**
     * The last consumed token.
     * <p>
     * 마지막으로 소비한 토큰.
     */
    protected Token prev;

    /**
     * The current function context.
     * <p>
     * 현재 함수 문맥.
     */
    protected FuncCtx fc;

    /**
     * The current scope.
     * <p>
     * 현재 스코프.
     */
    protected Scope scope;

    /**
     * The first pending cover-grammar error token (a shorthand default like {a = 1} or a duplicate __proto__), or null.
     * <p>
     * 처리되지 않은 첫 커버 문법 오류 토큰({a = 1} 같은 축약 기본값이나 중복 __proto__). 없으면 null.
     */
    protected Token coverError;

    /**
     * The message of the pending cover-grammar error.
     * <p>
     * 처리되지 않은 커버 문법 오류의 메시지.
     */
    protected String coverMessage;

    /**
     * The validator for regular expression literals (may be null).
     * <p>
     * 정규식 리터럴 검증기(null 일 수 있다).
     */
    protected RegExpValidator regExpValidator;

    /**
     * Creates the base state for parsing a source text.
     * <p>
     * 소스 텍스트 파싱을 위한 기본 상태를 만든다.
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name / 소스 이름
     * @param module     true for the Module goal / Module 목표이면 true
     */
    ParserBase(String source, String sourceName, boolean module) {
        this.source = source;
        this.sourceName = sourceName;
        this.moduleGoal = module;
        this.lexer = new Lexer(source, sourceName);
        this.lexer.setModule(module);
    }

    // ------------------------------------------------------------------ tokens

    /**
     * Consumes the current token and reads the next one.
     * <p>
     * 현재 토큰을 소비하고 다음 토큰을 읽는다.
     */
    protected void next() {
        prev = tok;
        tok = lexer.next();
    }

    /**
     * Returns the token after the current one without consuming anything.
     * <p>
     * 아무것도 소비하지 않고 현재 토큰 다음 토큰을 반환한다.
     *
     * @return the following token / 다음 토큰
     */
    protected Token peek() {
        return peek(1);
    }

    /**
     * Returns a token a selected distance ahead without consuming anything.
     * <p>
     * 지정한 거리 앞의 토큰을 소비하지 않고 반환한다.
     *
     * @param distance number of tokens after the current token (at least 1) / 현재 토큰 뒤 거리(1 이상)
     * @return the lookahead token / 미리 본 토큰
     */
    protected Token peek(int distance) {
        Lexer.State s = lexer.saveState();
        Token t = tok;
        // 현재 토큰 뒤에서 요청한 거리만큼 토큰을 읽는다.
        for (int i = 0; i < distance; i++) {
            t = lexer.next();
        }
        lexer.restoreState(s);
        return t;
    }

    /**
     * Tests whether the current token is the given punctuator.
     * <p>
     * 현재 토큰이 주어진 구두점인지 검사한다.
     *
     * @param p the punctuator / 구두점
     * @return true if it matches / 같으면 true
     */
    protected final boolean isP(String p) {
        return tok.type == TokenType.PUNCT && tok.value.equals(p);
    }

    /**
     * Tests whether the current token is the given unescaped keyword.
     * <p>
     * 현재 토큰이 주어진 이스케이프 없는 키워드인지 검사한다.
     *
     * @param kw the keyword / 키워드
     * @return true if it matches / 같으면 true
     */
    protected final boolean isN(String kw) {
        return tok.type == TokenType.NAME && !tok.escaped && tok.value.equals(kw);
    }

    /**
     * Consumes the punctuator if it is the current token.
     * <p>
     * 현재 토큰이 그 구두점이면 소비한다.
     *
     * @param p the punctuator / 구두점
     * @return true if consumed / 소비했으면 true
     */
    protected final boolean eatP(String p) {
        if (isP(p)) {
            next();
            return true;
        }
        return false;
    }

    /**
     * Consumes the punctuator or fails.
     * <p>
     * 그 구두점을 소비하거나 오류를 낸다.
     *
     * @param p the punctuator / 구두점
     */
    protected final void expectP(String p) {
        if (!isP(p)) {
            throw unexpected(tok);
        }
        next();
    }

    /**
     * Consumes the keyword or fails.
     * <p>
     * 그 키워드를 소비하거나 오류를 낸다.
     *
     * @param kw the keyword / 키워드
     */
    protected final void expectN(String kw) {
        if (!isN(kw)) {
            throw unexpected(tok);
        }
        next();
    }

    /**
     * Implements automatic semicolon insertion: a semicolon, a closing brace, the end of input or a preceding line terminator
     * ends the statement.
     * <p>
     * 자동 세미콜론 삽입을 구현한다: 세미콜론, 닫는 중괄호, 입력의 끝, 앞선 줄 끝이 있으면 문장이 끝난다.
     */
    protected final void consumeSemicolon() {
        if (isP(";")) {
            next();
            return;
        }
        if (isP("}") || tok.type == TokenType.EOF || tok.newlineBefore) {
            return;
        }
        throw unexpected(tok);
    }

    // ------------------------------------------------------------------ errors

    /**
     * Creates the "Unexpected ..." error for a token.
     * <p>
     * 토큰에 대한 "Unexpected ..." 오류를 만든다.
     *
     * @param t the token / 토큰
     * @return the exception to throw / 던질 예외
     */
    protected final JsException unexpected(Token t) {
        String msg;
        switch (t.type) {
        case EOF:
            msg = "Unexpected end of input";
            break;
        case STRING:
            msg = "Unexpected string";
            break;
        case NUMBER:
            msg = "Unexpected number";
            break;
        case TEMPLATE:
            msg = "Unexpected template string";
            break;
        case NAME:
            msg = isReservedWord(t.value) || (fc != null && fc.strict && isStrictReserved(t.value))
                    ? "Unexpected token '" + t.value + "'" : "Unexpected identifier '" + t.value + "'";
            break;
        default:
            msg = "Unexpected token '" + t.value + "'";
        }
        return JsException.syntaxError(msg, sourceName, t.line, t.column);
    }

    /**
     * Creates a SyntaxError at a token.
     * <p>
     * 토큰 위치에 SyntaxError 를 만든다.
     *
     * @param msg the message / 메시지
     * @param t   the token / 토큰
     * @return the exception to throw / 던질 예외
     */
    protected final JsException error(String msg, Token t) {
        return JsException.syntaxError(msg, sourceName, t.line, t.column);
    }

    /**
     * Creates a SyntaxError at a node.
     * <p>
     * 노드 위치에 SyntaxError 를 만든다.
     *
     * @param msg the message / 메시지
     * @param n   the node / 노드
     * @return the exception to throw / 던질 예외
     */
    protected final JsException error(String msg, Node n) {
        return JsException.syntaxError(msg, sourceName, n.line, n.column);
    }

    /**
     * Creates a SyntaxError at the current token.
     * <p>
     * 현재 토큰 위치에 SyntaxError 를 만든다.
     *
     * @param msg the message / 메시지
     * @return the exception to throw / 던질 예외
     */
    protected final JsException error(String msg) {
        return error(msg, tok);
    }

    // ------------------------------------------------------------------ reserved words

    /**
     * Tests for the ES2015 reserved words that are always reserved (keywords, future reserved word enum, and the literals).
     * <p>
     * 항상 예약된 ES2015 예약어(키워드, 미래 예약어 enum, 리터럴)인지 검사한다.
     *
     * @param s the identifier name / 식별자 이름
     * @return true if reserved / 예약어이면 true
     */
    protected static boolean isReservedWord(String s) {
        switch (s.length()) {
        case 2:
            return s.equals("do") || s.equals("if") || s.equals("in");
        case 3:
            return s.equals("for") || s.equals("new") || s.equals("try") || s.equals("var");
        case 4:
            return s.equals("case") || s.equals("else") || s.equals("enum") || s.equals("null") || s.equals("this")
                    || s.equals("true") || s.equals("void") || s.equals("with");
        case 5:
            return s.equals("break") || s.equals("catch") || s.equals("class") || s.equals("const") || s.equals("false")
                    || s.equals("super") || s.equals("throw") || s.equals("while");
        case 6:
            return s.equals("delete") || s.equals("export") || s.equals("import") || s.equals("return") || s.equals("switch")
                    || s.equals("typeof");
        case 7:
            return s.equals("default") || s.equals("extends") || s.equals("finally");
        case 8:
            return s.equals("continue") || s.equals("debugger") || s.equals("function");
        case 10:
            return s.equals("instanceof");
        default:
            return false;
        }
    }

    /**
     * Tests for the words reserved only in strict mode code.
     * <p>
     * 엄격 모드 코드에서만 예약된 단어인지 검사한다.
     *
     * @param s the identifier name / 식별자 이름
     * @return true if reserved in strict mode / 엄격 모드에서 예약어이면 true
     */
    protected static boolean isStrictReserved(String s) {
        return s.equals("implements") || s.equals("interface") || s.equals("let") || s.equals("package") || s.equals("private")
                || s.equals("protected") || s.equals("public") || s.equals("static") || s.equals("yield");
    }

    /**
     * Validates an identifier used as a binding name (declaration, parameter, function name).
     * <p>
     * 바인딩 이름(선언, 매개변수, 함수 이름)으로 쓰인 식별자를 검증한다.
     *
     * @param t the NAME token / NAME 토큰
     */
    protected final void checkBindingName(Token t) {
        checkIdentifier(t, true);
    }

    /**
     * Validates an identifier used as a reference or binding.
     * <p>
     * 참조나 바인딩으로 쓰인 식별자를 검증한다.
     *
     * @param t       the NAME token / NAME 토큰
     * @param binding true for bindings (eval and arguments are then forbidden in strict code) / 바인딩이면 true(엄격 코드에서 eval, arguments 금지)
     */
    protected final void checkIdentifier(Token t, boolean binding) {
        String v = t.value;
        if (isReservedWord(v)) {
            throw error(t.escaped ? "Keyword must not contain escaped characters" : "Unexpected token '" + v + "'", t);
        }
        if (fc.strict) {
            if (isStrictReserved(v)) {
                throw error("Unexpected strict mode reserved word", t);
            }
            if (binding && (v.equals("eval") || v.equals("arguments"))) {
                throw error("Unexpected eval or arguments in strict mode", t);
            }
        } else if (v.equals("yield") && fc.generator) {
            throw error("Unexpected identifier 'yield' in a generator", t);
        }
        if ((moduleGoal || fc.async) && v.equals("await")) {
            throw error("Unexpected reserved word 'await' in this context", t);
        }
    }

    // ------------------------------------------------------------------ nodes

    /**
     * Creates a node positioned at a token.
     * <p>
     * 토큰 위치에 노드를 만든다.
     *
     * @param type the node type / 노드 종류
     * @param t    the first token / 첫 토큰
     * @return the node / 노드
     */
    protected final Node mk(int type, Token t) {
        Node n = new Node(type, t.line, t.column);
        n.start = t.start;
        return n;
    }

    /**
     * Completes a node: sets its end offset and propagates the yield flag from its children.
     * <p>
     * 노드를 마무리한다: 끝 오프셋을 정하고 자식의 yield 플래그를 전파한다.
     *
     * @param n the node / 노드
     * @return the same node / 같은 노드
     */
    protected final Node done(Node n) {
        n.end = prev != null ? prev.end : n.start;
        if (n.type != NodeType.FUNCTION) {
            boolean y = n.hasYield || (n.a != null && n.a.hasYield) || (n.b != null && n.b.hasYield)
                    || (n.c != null && n.c.hasYield) || (n.d != null && n.d.hasYield);
            if (!y && n.list != null) {
                // 자식 목록에 yield 가 있는지 확인한다.
                for (int i = 0; i < n.list.length; i++) {
                    if (n.list[i] != null && n.list[i].hasYield) {
                        y = true;
                        break;
                    }
                }
            }
            n.hasYield = y;
        }
        return n;
    }

    // ------------------------------------------------------------------ scopes and declarations

    /**
     * Opens a scope.
     * <p>
     * 스코프를 연다.
     *
     * @param kind the scope kind / 스코프 종류
     * @return the new scope / 새 스코프
     */
    protected final Scope pushScope(int kind) {
        scope = new Scope(scope, kind);
        return scope;
    }

    /**
     * Closes the current scope and returns it.
     * <p>
     * 현재 스코프를 닫고 반환한다.
     *
     * @return the closed scope / 닫은 스코프
     */
    protected final Scope popScope() {
        Scope s = scope;
        scope = s.parent;
        return s;
    }

    /**
     * Creates the redeclaration error.
     * <p>
     * 재선언 오류를 만든다.
     *
     * @param name the name / 이름
     * @param t    the token / 토큰
     * @return the exception to throw / 던질 예외
     */
    protected final JsException redeclared(String name, Token t) {
        return error("Identifier '" + name + "' has already been declared", t);
    }

    /**
     * Declares a var name in the current scope chain up to the enclosing function scope.
     * <p>
     * var 이름을 현재 스코프에서 바깥 함수 스코프까지 선언한다.
     *
     * @param name the name / 이름
     * @param t    the token for errors / 오류 위치 토큰
     */
    protected final void declareVar(String name, Token t) {
        // 가장 가까운 함수 스코프까지 올라가며 렉시컬 선언과 충돌하는지 확인한다.
        for (Scope s = scope; s != null; s = s.parent) {
            if (s.lex.containsKey(name) && !name.equals(s.catchSimple)) {
                throw redeclared(name, t);
            }
            s.vars.add(name);
            if (s.kind == SCOPE_FUNCTION) {
                s.varOrder.add(name);
                break;
            }
        }
    }

    /**
     * Declares a let, const or class name in the current scope.
     * <p>
     * let, const, class 이름을 현재 스코프에 선언한다.
     *
     * @param name    the name / 이름
     * @param isConst true for const and imports / const 와 import 이면 true
     * @param t       the token for errors / 오류 위치 토큰
     */
    protected final void declareLexical(String name, boolean isConst, Token t) {
        if (scope.lex.containsKey(name) || scope.vars.contains(name) || (scope.kind == SCOPE_FUNCTION && scope.params.contains(name))) {
            throw redeclared(name, t);
        }
        scope.lex.put(name, Boolean.valueOf(isConst));
    }

    /**
     * Declares a function declaration name: var-like at the top level of functions and scripts, lexical in blocks and at the
     * top level of modules. Sloppy mode allows duplicate function declarations in a block (Annex B.3.3.4).
     * <p>
     * 함수 선언 이름을 선언한다: 함수와 스크립트의 최상위에서는 var 처럼, 블록과 모듈 최상위에서는 렉시컬로 선언한다.
     * 느슨한 모드에서는 블록 안의 중복 함수 선언을 허용한다(부속서 B.3.3.4).
     *
     * @param name the name / 이름
     * @param decl the FUNCTION_DECL node / FUNCTION_DECL 노드
     * @param t    the token for errors / 오류 위치 토큰
     */
    protected final void declareFunction(String name, Node decl, Token t) {
        if (scope.kind == SCOPE_FUNCTION && !scope.moduleTop) {
            declareVar(name, t);
            scope.functions.add(decl);
            return;
        }
        if (scope.lex.containsKey(name)) {
            if (!(scope.funcNames.contains(name) && !fc.strict)) {
                throw redeclared(name, t);
            }
        } else if (scope.vars.contains(name) || (scope.kind == SCOPE_FUNCTION && scope.params.contains(name))) {
            throw redeclared(name, t);
        } else {
            scope.lex.put(name, Boolean.FALSE);
        }
        scope.funcNames.add(name);
        scope.functions.add(decl);
        if (!fc.strict && scope.kind == SCOPE_BLOCK) {
            // 느슨한 모드의 블록 함수는 var 로 끌어올릴 후보가 된다(부속서 B.3.3).
            List<Scope> chain = new ArrayList<Scope>();
            Scope s = scope.parent;
            while (s != null) { // 바깥 스코프로 올라가며 함수 스코프를 찾는다.
                chain.add(s);
                if (s.kind == SCOPE_FUNCTION) {
                    break;
                }
                s = s.parent;
            }
            if (s != null) {
                s.candidates.add(new Candidate(name, decl, chain));
            }
        }
    }

    /**
     * Resolves the Annex B hoisting candidates of a function scope: a block function is also a var if that would not cause
     * an early error and the name is not a parameter.
     * <p>
     * 함수 스코프의 부속서 B 끌어올림 후보를 확정한다: 조기 오류가 생기지 않고 매개변수 이름이 아니면 블록 함수는 var 도 된다.
     *
     * @param s the function-level scope that is being closed / 닫는 함수 수준 스코프
     */
    protected final void resolveCandidates(Scope s) {
        // 후보마다 바깥 스코프에 같은 이름의 렉시컬 선언이 없는지 확인한다.
        for (int i = 0; i < s.candidates.size(); i++) {
            Candidate c = s.candidates.get(i);
            boolean ok = !s.params.contains(c.name);
            for (int k = 0; ok && k < c.chain.size(); k++) { // 바깥 스코프의 렉시컬 이름과 충돌하는지 확인한다.
                Scope e = c.chain.get(k);
                if (e.lex.containsKey(c.name) && !c.name.equals(e.catchSimple)) {
                    ok = false;
                }
            }
            if (ok) {
                c.decl.flag = true;
                s.vars.add(c.name);
                s.varOrder.add(c.name);
            }
        }
    }

    /**
     * Converts the parse-time scope into the {@link ScopeInfo} for the interpreter.
     * <p>
     * 파싱 중 스코프를 인터프리터용 {@link ScopeInfo} 로 변환한다.
     *
     * @param s the scope / 스코프
     * @return the description / 설명
     */
    protected final ScopeInfo toInfo(Scope s) {
        ScopeInfo info = new ScopeInfo();
        if (s.kind == SCOPE_FUNCTION) {
            info.varNames = s.varOrder.toArray(new String[s.varOrder.size()]);
        }
        List<String> names = new ArrayList<String>();
        List<Boolean> consts = new ArrayList<Boolean>();
        // 함수 선언 이름은 함수 목록에서 만들므로 lexNames 에서 제외한다.
        for (Map.Entry<String, Boolean> e : s.lex.entrySet()) {
            if (!s.funcNames.contains(e.getKey())) {
                names.add(e.getKey());
                consts.add(e.getValue());
            }
        }
        info.lexNames = names.toArray(new String[names.size()]);
        info.lexConst = new boolean[consts.size()];
        for (int i = 0; i < consts.size(); i++) { // const 여부를 배열로 옮긴다.
            info.lexConst[i] = consts.get(i).booleanValue();
        }
        info.functions = s.functions.toArray(new Node[s.functions.size()]);
        info.hasDirectEval = s.hasEval;
        return info;
    }

    /**
     * Fills the shared fields of a {@link FunctionInfo}.
     * <p>
     * {@link FunctionInfo} 의 공통 필드를 채운다.
     *
     * @param info the function description / 함수 설명
     */
    protected final void initInfo(FunctionInfo info) {
        info.source = source;
        info.sourceName = sourceName;
    }

    /**
     * Collects the names bound by a binding pattern or identifier node.
     * <p>
     * 바인딩 패턴이나 식별자 노드가 묶는 이름들을 모은다.
     *
     * @param n   the pattern / 패턴
     * @param out receives the names / 이름을 받는다
     */
    protected final void boundNames(Node n, List<String> out) {
        if (n == null) {
            return;
        }
        switch (n.type) {
        case NodeType.IDENT:
            out.add(n.name);
            break;
        case NodeType.DEFAULT:
        case NodeType.REST:
            boundNames(n.a, out);
            break;
        case NodeType.ARRAY_PATTERN:
            // 배열 패턴의 각 요소를 훑는다.
            for (int i = 0; i < n.list.length; i++) {
                boundNames(n.list[i], out);
            }
            break;
        case NodeType.OBJECT_PATTERN:
            // 객체 패턴의 각 프로퍼티 대상을 훑는다.
            for (int i = 0; i < n.list.length; i++) {
                boundNames(n.list[i].b, out);
            }
            break;
        default:
            break;
        }
    }
}
