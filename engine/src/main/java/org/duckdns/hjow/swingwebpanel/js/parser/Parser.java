package org.duckdns.hjow.swingwebpanel.js.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.Op;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;

/**
 * The JavaScript parser (ECMA-262 6th edition clauses 12 to 15) with the early errors of the specification:
 * statements, declarations with redeclaration checks, strict mode, labels, break/continue/return validity, automatic
 * semicolon insertion, the Script and Module goals (import/export), eval code and the Function constructor.
 * Syntax that is not part of ES2015 is reported as a SyntaxError with its position; nothing is silently ignored.
 * <p>
 * Use the static methods: {@link #parseScript(String, String)}, {@link #parseModule(String, String)},
 * {@link #parseEval(String, String, EvalOptions)} and {@link #parseFunctionConstructor(String, String, boolean, String)}.
 * Each returns a PROGRAM node (or a FUNCTION node) of {@link org.duckdns.hjow.swingwebpanel.js.ast.Node}.
 * <p>
 * ECMA-262 6판 12~15장의 JavaScript 파서이며 규격의 조기 오류를 구현한다: 문장, 재선언 검사가 있는 선언, 엄격 모드, 레이블,
 * break/continue/return 유효성, 자동 세미콜론 삽입, Script 와 Module 목표(import/export), eval 코드, Function 생성자.
 * ES2015 에 없는 문법은 위치가 있는 SyntaxError 로 보고하며 조용히 무시하지 않는다.
 * <p>
 * 정적 메서드를 사용한다: {@link #parseScript(String, String)}, {@link #parseModule(String, String)},
 * {@link #parseEval(String, String, EvalOptions)}, {@link #parseFunctionConstructor(String, String, boolean, String)}.
 * 각각 {@link org.duckdns.hjow.swingwebpanel.js.ast.Node} 의 PROGRAM 노드(또는 FUNCTION 노드)를 반환한다.
 */
public final class Parser extends ExpressionParser {

    /**
     * Context of the caller of eval that affects how eval code may be parsed.
     * <p>
     * eval 코드의 파싱 방식에 영향을 주는 eval 호출자의 문맥.
     */
    public static final class EvalOptions {
        /**
         * True if the calling code is strict (eval code is then strict too).
         * <p>
         * 호출 코드가 엄격하면 true(그러면 eval 코드도 엄격하다).
         */
        public boolean strict;
        /**
         * True if new.target is allowed (direct eval inside a non-arrow function).
         * <p>
         * new.target 을 허용하면 true(비 화살표 함수 안의 직접 eval).
         */
        public boolean allowNewTarget;
        /**
         * True if super.property is allowed (direct eval inside a method).
         * <p>
         * super.프로퍼티 를 허용하면 true(메서드 안의 직접 eval).
         */
        public boolean allowSuperProp;
        /**
         * True if super() is allowed (direct eval inside a derived constructor).
         * <p>
         * super() 를 허용하면 true(파생 생성자 안의 직접 eval).
         */
        public boolean allowSuperCall;
    }

    /**
     * The validator used for regular expression literals of newly created parsers.
     * <p>
     * 새로 만드는 파서의 정규식 리터럴 검증에 쓰는 검증기.
     */
    private static volatile RegExpValidator defaultRegExpValidator;

    /**
     * Information collected for modules.
     * <p>
     * 모듈에서 수집한 정보.
     */
    private ProgramInfo pinfo;

    /**
     * Names exported so far (duplicates are early errors).
     * <p>
     * 지금까지 내보낸 이름(중복은 조기 오류).
     */
    private final Set<String> exportedNames = new HashSet<String>();

    /**
     * Local names referenced by <code>export { x }</code>, checked at the end of the module.
     * <p>
     * <code>export { x }</code> 가 참조한 로컬 이름. 모듈 끝에서 검사한다.
     */
    private final List<Token> exportRefs = new ArrayList<Token>();

    /**
     * Creates a parser.
     * <p>
     * 파서를 만든다.
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name / 소스 이름
     * @param module     true for the Module goal / Module 목표이면 true
     */
    private Parser(String source, String sourceName, boolean module) {
        super(source, sourceName, module);
        this.regExpValidator = defaultRegExpValidator;
    }

    /**
     * Sets the validator that checks regular expression literal patterns for all parsers created afterwards.
     * <p>
     * 이후 만들어지는 모든 파서가 정규식 리터럴 패턴을 검사하는 데 쓸 검증기를 지정한다.
     *
     * @param validator the validator or null / 검증기 또는 null
     */
    public static void setDefaultRegExpValidator(RegExpValidator validator) {
        defaultRegExpValidator = validator;
    }

    // ------------------------------------------------------------------ entry points

    /**
     * Parses a Script.
     * <p>
     * Script 를 파싱한다.
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name for errors / 오류에 쓰는 소스 이름
     * @return the PROGRAM node / PROGRAM 노드
     */
    public static Node parseScript(String source, String sourceName) {
        return new Parser(source, sourceName, false).parseProgram(null);
    }

    /**
     * Parses a Module (always strict, import and export allowed).
     * <p>
     * Module 을 파싱한다(항상 엄격, import/export 허용).
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name for errors / 오류에 쓰는 소스 이름
     * @return the PROGRAM node / PROGRAM 노드
     */
    public static Node parseModule(String source, String sourceName) {
        return new Parser(source, sourceName, true).parseProgram(null);
    }

    /**
     * Parses eval code (a Script that inherits some context from the caller).
     * <p>
     * eval 코드를 파싱한다(호출자의 일부 문맥을 이어받는 Script).
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name / 소스 이름
     * @param options    the caller context / 호출자 문맥
     * @return the PROGRAM node / PROGRAM 노드
     */
    public static Node parseEval(String source, String sourceName, EvalOptions options) {
        return new Parser(source, sourceName, false).parseProgram(options == null ? new EvalOptions() : options);
    }

    /**
     * Parses the source for the Function constructor (CreateDynamicFunction, ECMA-262 6th edition 19.2.1.1.1). The parameter
     * text and the body text are checked separately so that neither can break out of its part, then the complete text
     * <code>function anonymous(params\n) {\nbody\n}</code> is parsed.
     * <p>
     * Function 생성자용 소스를 파싱한다(CreateDynamicFunction, ECMA-262 6판 19.2.1.1.1). 매개변수 텍스트와 본문 텍스트를 따로 검사해
     * 어느 쪽도 자기 부분을 벗어날 수 없게 한 뒤, 전체 텍스트 <code>function anonymous(params\n) {\nbody\n}</code> 를 파싱한다.
     *
     * @param params     the parameter list text / 매개변수 목록 텍스트
     * @param body       the body text / 본문 텍스트
     * @param generator  true for GeneratorFunction / GeneratorFunction 이면 true
     * @param sourceName the source name / 소스 이름
     * @return the FUNCTION node (its FunctionInfo source text is the full function text) / FUNCTION 노드(FunctionInfo 의 소스 텍스트가 전체 함수 텍스트)
     */
    public static Node parseFunctionConstructor(String params, String body, boolean generator, String sourceName) {
        String star = generator ? "*" : "";
        String paramCheck = "function" + star + " anonymous(" + params + "\n) {}";
        Node pc = parseScript(paramCheck, sourceName);
        if (pc.list.length != 1 || pc.list[0].type != NodeType.FUNCTION_DECL || pc.list[0].b.fn.end != paramCheck.length()
                || pc.list[0].b.fn.body.list.length != 0) {
            throw JsException.syntaxError("Invalid function parameters", sourceName, 1, 1);
        }
        String bodyCheck = "function" + star + " anonymous(\n) {\n" + body + "\n}";
        Node bc = parseScript(bodyCheck, sourceName);
        if (bc.list.length != 1 || bc.list[0].type != NodeType.FUNCTION_DECL || bc.list[0].b.fn.end != bodyCheck.length()) {
            throw JsException.syntaxError("Invalid function body", sourceName, 1, 1);
        }
        String text = "function" + star + " anonymous(" + params + "\n) {\n" + body + "\n}";
        Node prog = parseScript("(" + text + ")", sourceName);
        Node fn = prog.list[0].a;
        fn.fn.start = 1;
        fn.fn.end = 1 + text.length();
        fn.fn.name = "anonymous";
        return fn;
    }

    // ------------------------------------------------------------------ program

    /**
     * Parses the whole source as a program.
     * <p>
     * 소스 전체를 프로그램으로 파싱한다.
     *
     * @param eval eval options or null for ordinary scripts and modules / eval 옵션. 일반 스크립트와 모듈이면 null
     * @return the PROGRAM node / PROGRAM 노드
     */
    private Node parseProgram(EvalOptions eval) {
        fc = new FuncCtx();
        fc.strict = moduleGoal || (eval != null && eval.strict);
        if (eval != null) {
            fc.allowNewTarget = eval.allowNewTarget;
            fc.allowSuperProp = eval.allowSuperProp;
            fc.allowSuperCall = eval.allowSuperCall;
        }
        Scope top = new Scope(null, SCOPE_FUNCTION);
        top.moduleTop = moduleGoal;
        scope = top;
        fc.funcScope = top;
        pinfo = new ProgramInfo();
        pinfo.module = moduleGoal;
        pinfo.source = source;
        pinfo.sourceName = sourceName;
        next();
        Token start = tok;
        List<Node> stmts = parseBodyStatements(null, false);
        resolvePrivateNames();
        pinfo.strict = fc.strict;
        resolveCandidates(top);
        if (moduleGoal) {
            checkModuleExports(top);
        }
        Node prog = mk(NodeType.PROGRAM, start);
        prog.list = stmts.toArray(new Node[stmts.size()]);
        prog.scope = toInfo(top);
        prog.scope.hasDirectEval = fc.hasEval;
        prog.value = pinfo;
        prog.end = source.length();
        return prog;
    }

    /**
     * Checks that every name exported with <code>export { x }</code> is declared in the module.
     * <p>
     * <code>export { x }</code> 로 내보낸 모든 이름이 모듈에 선언되어 있는지 검사한다.
     *
     * @param top the module scope / 모듈 스코프
     */
    private void checkModuleExports(Scope top) {
        // 내보낸 로컬 이름이 선언되어 있는지 하나씩 확인한다.
        for (int i = 0; i < exportRefs.size(); i++) {
            Token t = exportRefs.get(i);
            if (!top.lex.containsKey(t.value) && !top.vars.contains(t.value)) {
                throw error("Export '" + t.value + "' is not defined in module", t);
            }
        }
    }

    /**
     * Parses a sequence of statements with a directive prologue, until the end of input or a closing brace.
     * <p>
     * 지시문 서문이 있는 문장 나열을 입력의 끝이나 닫는 중괄호까지 파싱한다.
     *
     * @param info  the function description when parsing a function body, otherwise null / 함수 본문이면 함수 설명, 아니면 null
     * @param brace true to stop at a closing brace, false to stop at the end of input / 닫는 중괄호에서 멈추려면 true, 입력 끝에서 멈추려면 false
     * @return the statements / 문장들
     */
    private List<Node> parseBodyStatements(FunctionInfo info, boolean brace) {
        List<Node> stmts = new ArrayList<Node>();
        boolean prologue = true;
        Token octal = null;
        // 닫는 중괄호 또는 입력 끝까지 문장을 읽는다. 앞쪽의 문자열 문장은 지시문이다.
        while (brace ? !isP("}") : tok.type != TokenType.EOF) {
            if (tok.type == TokenType.EOF) {
                throw unexpected(tok);
            }
            Token st = tok;
            Node s = parseStatementListItem();
            if (prologue) {
                if (s.type == NodeType.EXPR_STMT && s.a.type == NodeType.LITERAL && s.a.value instanceof JsString
                        && !s.a.paren && st.type == TokenType.STRING) {
                    if (st.legacyOctal && octal == null) {
                        octal = st;
                    }
                    if (st.raw.equals("'use strict'") || st.raw.equals("\"use strict\"")) {
                        if (info != null && !info.simpleParams) {
                            throw error("Illegal 'use strict' directive in function with non-simple parameter list", st);
                        }
                        fc.strict = true;
                        if (octal != null) {
                            throw error("Octal escape sequences are not allowed in strict mode.", octal);
                        }
                    }
                } else {
                    prologue = false;
                }
            }
            stmts.add(s);
        }
        return stmts;
    }

    /**
     * Parses a function body block (called by the expression parser).
     * <p>
     * 함수 본문 블록을 파싱한다(표현식 파서가 호출).
     *
     * @param info the function description / 함수 설명
     * @param ctx  the function context / 함수 문맥
     */
    void parseFunctionBody(FunctionInfo info, FuncCtx ctx) {
        Token open = tok;
        expectP("{");
        List<Node> stmts = parseBodyStatements(info, true);
        expectP("}");
        Node block = mk(NodeType.BLOCK, open);
        block.list = stmts.toArray(new Node[stmts.size()]);
        info.body = done(block);
    }

    // ------------------------------------------------------------------ statements

    /**
     * Parses a StatementListItem: a declaration or a statement.
     * <p>
     * StatementListItem 을 파싱한다: 선언 또는 문장.
     *
     * @return the node / 노드
     */
    protected Node parseStatementListItem() {
        if (tok.type == TokenType.NAME && !tok.escaped) {
            String v = tok.value;
            if (v.equals("function")) {
                return parseFunctionDeclaration();
            }
            if (v.equals("async") && !peek().newlineBefore && peek().isName("function")) {
                return parseFunctionDeclaration(false, true);
            }
            if (v.equals("class")) {
                return parseClassDeclaration();
            }
            if (v.equals("const")) {
                return parseVarStatement(Op.CONST);
            }
            if (v.equals("let") && isLetDeclaration()) {
                return parseVarStatement(Op.LET);
            }
            if (v.equals("import") && moduleGoal && !peek().isPunct("(") && !peek().isPunct(".")) {
                return parseImport();
            }
            if (v.equals("export") && moduleGoal) {
                return parseExport();
            }
        }
        return parseStatement();
    }

    /**
     * Decides whether a <code>let</code> token starts a declaration (it is an identifier in sloppy mode otherwise).
     * <p>
     * <code>let</code> 토큰이 선언의 시작인지 판단한다(그렇지 않으면 느슨한 모드의 식별자).
     *
     * @return true for declarations / 선언이면 true
     */
    private boolean isLetDeclaration() {
        Token n = peek();
        if (n.isPunct("[") || n.isPunct("{")) {
            return true;
        }
        if (n.type == TokenType.NAME) {
            return !(n.isName("in") || n.isName("instanceof")) || fc.strict;
        }
        return fc.strict;
    }

    /**
     * Parses a Statement.
     * <p>
     * Statement 를 파싱한다.
     *
     * @return the node / 노드
     */
    private Node parseStatement() {
        Token t = tok;
        if (t.type == TokenType.PUNCT) {
            if (t.value.equals("{")) {
                return parseBlock();
            }
            if (t.value.equals(";")) {
                next();
                return done(mk(NodeType.EMPTY, t));
            }
        } else if (t.type == TokenType.NAME && !t.escaped) {
            String v = t.value;
            if (v.equals("var")) {
                return parseVarStatement(Op.VAR);
            }
            if (v.equals("if")) {
                return parseIf();
            }
            if (v.equals("for")) {
                return parseFor(null);
            }
            if (v.equals("while")) {
                return parseWhile(null);
            }
            if (v.equals("do")) {
                return parseDoWhile(null);
            }
            if (v.equals("return")) {
                return parseReturn();
            }
            if (v.equals("break") || v.equals("continue")) {
                return parseBreakContinue(v.equals("break"));
            }
            if (v.equals("throw")) {
                return parseThrow();
            }
            if (v.equals("try")) {
                return parseTry();
            }
            if (v.equals("switch")) {
                return parseSwitch();
            }
            if (v.equals("with")) {
                return parseWith();
            }
            if (v.equals("debugger")) {
                next();
                consumeSemicolon();
                return done(mk(NodeType.DEBUGGER, t));
            }
            if (v.equals("function")) {
                throw error(fc.strict ? "In strict mode code, functions can only be declared at top level or inside a block."
                        : "In non-strict mode code, functions can only be declared at top level, inside a block, or as the body of an if statement.");
            }
            if (v.equals("class")) {
                throw unexpected(tok);
            }
            if (v.equals("let") && peek().isPunct("[")) {
                throw error("Lexical declaration cannot appear in a single-statement context");
            }
            if ((v.equals("import") && !peek().isPunct("(") && !peek().isPunct(".")) || v.equals("export")) {
                throw error(moduleGoal ? "Unexpected import/export position (only at the top level of a module)"
                        : "Cannot use import/export statement outside a module");
            }
        }
        if (t.type == TokenType.NAME && !isReservedWord(t.value) && peek().isPunct(":")) {
            return parseLabeled(new ArrayList<LabelRec>());
        }
        Node e = parseExpression(false);
        consumeSemicolon();
        Node n = mk(NodeType.EXPR_STMT, t);
        n.a = e;
        return done(n);
    }

    /**
     * Parses a labelled statement, collecting directly nested labels so that continue can target loops.
     * <p>
     * 레이블 문장을 파싱한다. 연속된 레이블을 모아 continue 가 반복문을 가리킬 수 있게 한다.
     *
     * @param chain the labels of directly enclosing labelled statements / 바로 바깥 레이블 문장들의 레이블
     * @return the LABELED node / LABELED 노드
     */
    private Node parseLabeled(List<LabelRec> chain) {
        Token t = tok;
        checkIdentifier(t, false);
        for (int i = 0; i < fc.labels.size(); i++) { // 열려 있는 레이블과 이름을 비교한다.
            if (fc.labels.get(i).name.equals(t.value)) {
                throw error("Label '" + t.value + "' has already been declared", t);
            }
        }
        next();
        expectP(":");
        LabelRec rec = new LabelRec(t.value);
        fc.labels.add(rec);
        chain.add(rec);
        Node n = mk(NodeType.LABELED, t);
        n.name = t.value;
        Node body;
        if (tok.type == TokenType.NAME && !tok.escaped && (tok.value.equals("for") || tok.value.equals("while") || tok.value.equals("do"))) {
            // 반복문에 붙은 레이블은 모두 continue 의 대상이 될 수 있다.
            for (int i = 0; i < chain.size(); i++) {
                chain.get(i).loop = true;
            }
            body = parseStatement();
        } else if (tok.type == TokenType.NAME && !isReservedWord(tok.value) && peek().isPunct(":")) {
            body = parseLabeled(chain);
        } else if (isN("function")) {
            if (fc.strict) {
                throw error("In strict mode code, functions can only be declared at top level or inside a block.");
            }
            body = parseFunctionDeclaration();
        } else {
            body = parseStatement();
        }
        fc.labels.remove(fc.labels.size() - 1);
        n.a = body;
        return done(n);
    }

    /**
     * Parses a block statement with its own scope.
     * <p>
     * 자신의 스코프를 가진 블록 문장을 파싱한다.
     *
     * @return the BLOCK node / BLOCK 노드
     */
    private Node parseBlock() {
        Token open = tok;
        expectP("{");
        pushScope(SCOPE_BLOCK);
        List<Node> stmts = new ArrayList<Node>();
        // 닫는 중괄호까지 문장을 읽는다.
        while (!isP("}")) {
            if (tok.type == TokenType.EOF) {
                throw unexpected(tok);
            }
            stmts.add(parseStatementListItem());
        }
        expectP("}");
        Node n = mk(NodeType.BLOCK, open);
        n.list = stmts.toArray(new Node[stmts.size()]);
        n.scope = blockScopeInfo(popScope());
        return done(n);
    }

    /**
     * Returns the scope description of a block, or null if the block declares nothing (no environment is then needed).
     * <p>
     * 블록의 스코프 설명을 반환한다. 선언이 없으면 null(환경이 필요 없다).
     *
     * @param s the closed scope / 닫은 스코프
     * @return the description or null / 설명 또는 null
     */
    private ScopeInfo blockScopeInfo(Scope s) {
        ScopeInfo si = toInfo(s);
        if (si.lexNames.length == 0 && si.functions.length == 0) {
            return null;
        }
        return si;
    }

    /**
     * Parses a var, let or const statement.
     * <p>
     * var, let, const 문장을 파싱한다.
     *
     * @param kind the declaration kind (an {@link Op} code) / 선언 종류({@link Op} 코드)
     * @return the VAR_DECL node / VAR_DECL 노드
     */
    private Node parseVarStatement(int kind) {
        Node d = parseVarDeclarations(kind, false, false);
        consumeSemicolon();
        d.end = prev.end;
        return d;
    }

    /**
     * Parses the declarator list of a variable declaration.
     * <p>
     * 변수 선언의 선언자 목록을 파싱한다.
     *
     * @param kind      the declaration kind / 선언 종류
     * @param noIn      true to forbid in in initializers (for heads) / 초기값에서 in 을 금지하려면 true(for 머리)
     * @param inForHead true if parsed in a for head where in/of may follow / in/of 가 뒤따를 수 있는 for 머리이면 true
     * @return the VAR_DECL node / VAR_DECL 노드
     */
    private Node parseVarDeclarations(int kind, boolean noIn, boolean inForHead) {
        Token start = tok;
        next();
        int bind = kind == Op.VAR ? BIND_VAR : (kind == Op.LET ? BIND_LET : BIND_CONST);
        List<Node> decls = new ArrayList<Node>();
        // 쉼표로 이어진 선언자를 모두 읽는다.
        do {
            Token dt = tok;
            Node target = parseBindingTarget(bind);
            Node init = null;
            if (isP("=")) {
                next();
                init = parseAssignment(noIn);
            } else if (!(inForHead && (isN("in") || isN("of")))) {
                if (kind == Op.CONST) {
                    throw error("Missing initializer in const declaration");
                }
                if (target.type != NodeType.IDENT) {
                    throw error("Missing initializer in destructuring declaration");
                }
            }
            Node decl = mk(NodeType.DECLARATOR, dt);
            decl.a = target;
            decl.b = init;
            decls.add(done(decl));
        } while (eatP(","));
        Node n = mk(NodeType.VAR_DECL, start);
        n.op = kind;
        n.list = decls.toArray(new Node[decls.size()]);
        return done(n);
    }

    /**
     * Parses an if statement (Annex B allows a function declaration as a body in sloppy mode).
     * <p>
     * if 문을 파싱한다(부속서 B 는 느슨한 모드에서 함수 선언을 본문으로 허용한다).
     *
     * @return the IF node / IF 노드
     */
    private Node parseIf() {
        Token t = tok;
        next();
        expectP("(");
        Node test = parseExpression(false);
        expectP(")");
        Node n = mk(NodeType.IF, t);
        n.a = test;
        n.b = parseIfBody();
        if (isN("else")) {
            next();
            n.c = parseIfBody();
        }
        return done(n);
    }

    /**
     * Parses the body of an if or else branch.
     * <p>
     * if 나 else 가지의 본문을 파싱한다.
     *
     * @return the statement / 문장
     */
    private Node parseIfBody() {
        if (isN("function") && !fc.strict) {
            Token t = tok;
            pushScope(SCOPE_BLOCK);
            Node decl = parseFunctionDeclaration();
            Node blk = mk(NodeType.BLOCK, t);
            blk.list = new Node[] { decl };
            blk.scope = blockScopeInfo(popScope());
            return done(blk);
        }
        return parseStatement();
    }

    /**
     * Parses a loop body and tracks the loop depth for break and continue.
     * <p>
     * 반복문 본문을 파싱하고 break/continue 를 위해 반복 깊이를 추적한다.
     *
     * @return the body statement / 본문 문장
     */
    private Node parseLoopBody() {
        fc.loopDepth++;
        Node body = parseStatement();
        fc.loopDepth--;
        return body;
    }

    /**
     * Parses a while loop.
     * <p>
     * while 반복문을 파싱한다.
     *
     * @param unused reserved / 예약
     * @return the WHILE node / WHILE 노드
     */
    private Node parseWhile(Object unused) {
        Token t = tok;
        next();
        expectP("(");
        Node n = mk(NodeType.WHILE, t);
        n.a = parseExpression(false);
        expectP(")");
        n.d = parseLoopBody();
        return done(n);
    }

    /**
     * Parses a do-while loop.
     * <p>
     * do-while 반복문을 파싱한다.
     *
     * @param unused reserved / 예약
     * @return the DO_WHILE node / DO_WHILE 노드
     */
    private Node parseDoWhile(Object unused) {
        Token t = tok;
        next();
        Node n = mk(NodeType.DO_WHILE, t);
        n.d = parseLoopBody();
        expectN("while");
        expectP("(");
        n.a = parseExpression(false);
        expectP(")");
        eatP(";");
        return done(n);
    }

    /**
     * Parses for, for-in and for-of statements.
     * <p>
     * for, for-in, for-of 문을 파싱한다.
     *
     * @param unused reserved / 예약
     * @return the FOR, FOR_IN or FOR_OF node / FOR, FOR_IN, FOR_OF 노드
     */
    private Node parseFor(Object unused) {
        Token t = tok;
        next();
        boolean forAwait = false;
        if (isN("await") && (fc.async || (moduleGoal && !fc.inFunction))) {
            forAwait = true;
            next();
        }
        expectP("(");
        Scope loopScope = pushScope(SCOPE_BLOCK);
        Node init = null;
        Node left = null;
        boolean isDeclHead = false;
        if (!isP(";")) {
            boolean isVar = isN("var");
            boolean isConst = isN("const");
            boolean isLet = isN("let") && isLetDeclaration();
            if (isVar || isConst || isLet) {
                isDeclHead = true;
                int kind = isVar ? Op.VAR : (isConst ? Op.CONST : Op.LET);
                Node decl = parseVarDeclarations(kind, true, true);
                if ((isN("of") || isN("in")) && decl.list.length == 1) {
                    if (decl.list[0].b != null) {
                        throw error("for-in/of loop variable declaration may not have an initializer.");
                    }
                    left = decl;
                } else {
                    init = decl;
                }
            } else {
                Token savedCover = coverError;
                String savedMsg = coverMessage;
                coverError = null;
                Node e = parseAssignmentCover(true, true);
                if (isN("of") || isN("in")) {
                    if ((e.type == NodeType.ARRAY || e.type == NodeType.OBJECT) && !e.paren) {
                        left = toAssignmentTarget(e);
                    } else {
                        checkSimpleTarget(e, "Invalid left-hand side in for-loop");
                        left = e;
                    }
                    coverError = savedCover;
                    coverMessage = savedMsg;
                } else {
                    if (coverError != null) {
                        throw error(coverMessage, coverError);
                    }
                    coverError = savedCover;
                    coverMessage = savedMsg;
                    if (isP(",")) {
                        List<Node> list = new ArrayList<Node>();
                        list.add(e);
                        // 쉼표로 이어진 초기화 식을 모은다.
                        while (eatP(",")) {
                            list.add(parseAssignment(true));
                        }
                        Node seq = mk(NodeType.SEQUENCE, t);
                        seq.list = list.toArray(new Node[list.size()]);
                        e = done(seq);
                    }
                    init = e;
                }
            }
        }
        if (left != null) {
            boolean isOf = isN("of");
            if (forAwait && !isOf) {
                throw error("for-await requires an 'of' clause", tok);
            }
            next();
            Node n = mk(isOf ? NodeType.FOR_OF : NodeType.FOR_IN, t);
            n.flag = forAwait;
            n.hasYield = forAwait || n.hasYield;
            n.a = left;
            n.b = isOf ? parseAssignment(false) : parseExpression(false);
            expectP(")");
            n.d = parseLoopBody();
            n.scope = blockScopeInfo(popScope());
            return done(n);
        }
        if (forAwait) {
            throw error("for-await requires a binding and an 'of' clause", tok);
        }
        expectP(";");
        Node n = mk(NodeType.FOR, t);
        n.a = init;
        if (!isP(";")) {
            n.b = parseExpression(false);
        }
        expectP(";");
        if (!isP(")")) {
            n.c = parseExpression(false);
        }
        expectP(")");
        n.d = parseLoopBody();
        n.scope = blockScopeInfo(popScope());
        return done(n);
    }

    /**
     * Parses a return statement.
     * <p>
     * return 문을 파싱한다.
     *
     * @return the RETURN node / RETURN 노드
     */
    private Node parseReturn() {
        Token t = tok;
        if (!fc.inFunction) {
            throw error("Illegal return statement");
        }
        next();
        Node n = mk(NodeType.RETURN, t);
        if (!isP(";") && !isP("}") && tok.type != TokenType.EOF && !tok.newlineBefore) {
            n.a = parseExpression(false);
        }
        consumeSemicolon();
        return done(n);
    }

    /**
     * Parses a break or continue statement and validates its target.
     * <p>
     * break 또는 continue 문을 파싱하고 대상을 검증한다.
     *
     * @param isBreak true for break, false for continue / break 이면 true, continue 이면 false
     * @return the BREAK or CONTINUE node / BREAK 또는 CONTINUE 노드
     */
    private Node parseBreakContinue(boolean isBreak) {
        Token t = tok;
        next();
        Node n = mk(isBreak ? NodeType.BREAK : NodeType.CONTINUE, t);
        if (tok.type == TokenType.NAME && !tok.newlineBefore && !isReservedWord(tok.value)) {
            Token lt = tok;
            LabelRec found = null;
            // 레이블이 현재 열려 있는 레이블 중에 있는지 찾는다.
            for (int i = 0; i < fc.labels.size(); i++) {
                if (fc.labels.get(i).name.equals(lt.value)) {
                    found = fc.labels.get(i);
                }
            }
            if (found == null) {
                throw error("Undefined label '" + lt.value + "'", lt);
            }
            if (!isBreak && !found.loop) {
                throw error("Illegal continue statement: '" + lt.value + "' does not denote an iteration statement", lt);
            }
            n.name = lt.value;
            next();
        } else if (isBreak) {
            if (fc.loopDepth == 0 && fc.switchDepth == 0) {
                throw error("Illegal break statement", t);
            }
        } else if (fc.loopDepth == 0) {
            throw error("Illegal continue statement: no surrounding iteration statement", t);
        }
        consumeSemicolon();
        return done(n);
    }

    /**
     * Parses a throw statement (no line terminator is allowed after throw).
     * <p>
     * throw 문을 파싱한다(throw 뒤에는 줄 끝이 올 수 없다).
     *
     * @return the THROW node / THROW 노드
     */
    private Node parseThrow() {
        Token t = tok;
        next();
        if (tok.newlineBefore) {
            throw error("Illegal newline after throw");
        }
        Node n = mk(NodeType.THROW, t);
        n.a = parseExpression(false);
        consumeSemicolon();
        return done(n);
    }

    /**
     * Parses a try statement.
     * <p>
     * try 문을 파싱한다.
     *
     * @return the TRY node / TRY 노드
     */
    private Node parseTry() {
        Token t = tok;
        next();
        Node n = mk(NodeType.TRY, t);
        n.a = parseBlock();
        if (isN("catch")) {
            next();
            Scope cs = pushScope(SCOPE_BLOCK);
            if (isP("(")) {
                next();
                n.b = parseBindingTarget(BIND_CATCH);
                if (n.b.type == NodeType.IDENT) {
                    cs.catchSimple = n.b.name;
                }
                expectP(")");
            }
            Token open = tok;
            expectP("{");
            List<Node> stmts = new ArrayList<Node>();
            // catch 블록은 매개변수와 같은 스코프를 공유한다.
            while (!isP("}")) {
                if (tok.type == TokenType.EOF) {
                    throw unexpected(tok);
                }
                stmts.add(parseStatementListItem());
            }
            expectP("}");
            Node blk = mk(NodeType.BLOCK, open);
            blk.list = stmts.toArray(new Node[stmts.size()]);
            n.c = done(blk);
            n.scope = toInfo(popScope());
        }
        if (isN("finally")) {
            next();
            n.d = parseBlock();
        }
        if (n.c == null && n.d == null) {
            throw error("Missing catch or finally after try");
        }
        return done(n);
    }

    /**
     * Parses a switch statement.
     * <p>
     * switch 문을 파싱한다.
     *
     * @return the SWITCH node / SWITCH 노드
     */
    private Node parseSwitch() {
        Token t = tok;
        next();
        expectP("(");
        Node n = mk(NodeType.SWITCH, t);
        n.a = parseExpression(false);
        expectP(")");
        expectP("{");
        pushScope(SCOPE_BLOCK);
        fc.switchDepth++;
        List<Node> cases = new ArrayList<Node>();
        boolean sawDefault = false;
        // 닫는 중괄호까지 case 와 default 절을 읽는다.
        while (!isP("}")) {
            Token ct = tok;
            Node c = mk(NodeType.CASE, ct);
            if (isN("case")) {
                next();
                c.a = parseExpression(false);
            } else if (isN("default")) {
                if (sawDefault) {
                    throw error("More than one default clause in switch statement");
                }
                sawDefault = true;
                next();
            } else {
                throw unexpected(tok);
            }
            expectP(":");
            List<Node> body = new ArrayList<Node>();
            while (!isP("}") && !isN("case") && !isN("default")) { // 다음 case/default 전까지의 문장을 읽는다.
                if (tok.type == TokenType.EOF) {
                    throw unexpected(tok);
                }
                body.add(parseStatementListItem());
            }
            c.list = body.toArray(new Node[body.size()]);
            cases.add(done(c));
        }
        expectP("}");
        fc.switchDepth--;
        n.list = cases.toArray(new Node[cases.size()]);
        n.scope = blockScopeInfo(popScope());
        return done(n);
    }

    /**
     * Parses a with statement (not allowed in strict mode).
     * <p>
     * with 문을 파싱한다(엄격 모드에서는 허용되지 않는다).
     *
     * @return the WITH node / WITH 노드
     */
    private Node parseWith() {
        Token t = tok;
        if (fc.strict) {
            throw error("Strict mode code may not include a with statement");
        }
        next();
        expectP("(");
        Node n = mk(NodeType.WITH, t);
        n.a = parseExpression(false);
        expectP(")");
        n.b = parseStatement();
        return done(n);
    }

    // ------------------------------------------------------------------ function and class declarations

    /**
     * Parses a function declaration (a name is required except for export default).
     * <p>
     * 함수 선언을 파싱한다(export default 가 아니면 이름이 필요하다).
     *
     * @return the FUNCTION_DECL node / FUNCTION_DECL 노드
     */
    private Node parseFunctionDeclaration() {
        return parseFunctionDeclaration(false, false);
    }

    /**
     * Parses a function declaration.
     * <p>
     * 함수 선언을 파싱한다.
     *
     * @param allowAnonymous true for <code>export default function () {}</code> / <code>export default function () {}</code> 이면 true
     * @return the FUNCTION_DECL node / FUNCTION_DECL 노드
     */
    private Node parseFunctionDeclaration(boolean allowAnonymous) {
        return parseFunctionDeclaration(allowAnonymous, false);
    }

    /**
     * Parses a normal or async function declaration.
     * <p>
     * 일반 또는 async 함수 선언을 파싱한다.
     *
     * @param allowAnonymous true for an export default anonymous function / 익명 export default 함수이면 true
     * @param async          true for an async declaration / async 선언이면 true
     * @return the FUNCTION_DECL node / FUNCTION_DECL 노드
     */
    private Node parseFunctionDeclaration(boolean allowAnonymous, boolean async) {
        Token start = tok;
        if (async) {
            next();
            if (!isN("function")) {
                throw unexpected(tok);
            }
        }
        next();
        boolean gen = eatP("*");
        FunctionInfo info = new FunctionInfo();
        info.generator = gen;
        info.async = async;
        Token nameTok = null;
        String bindingName;
        if (tok.type == TokenType.NAME && !isP("(")) {
            nameTok = tok;
            if (async && nameTok.value.equals("await")) {
                throw error("Unexpected reserved word 'await' in an async function name", nameTok);
            }
            checkBindingName(nameTok);
            info.name = nameTok.value;
            bindingName = nameTok.value;
            next();
        } else if (allowAnonymous) {
            info.name = "default";
            bindingName = "*default*";
        } else {
            throw unexpected(tok);
        }
        FuncCtx ctx = new FuncCtx();
        ctx.parent = fc;
        ctx.strict = fc.strict;
        ctx.generator = gen;
        ctx.async = async;
        ctx.inFunction = true;
        ctx.allowNewTarget = true;
        Node fn = parseFunctionTail(start, info, ctx, nameTok);
        Node decl = mk(NodeType.FUNCTION_DECL, start);
        Node id = mk(NodeType.IDENT, nameTok != null ? nameTok : start);
        id.name = bindingName;
        decl.a = id;
        decl.b = fn;
        decl.end = prev.end;
        Token errTok = nameTok != null ? nameTok : start;
        declareFunction(bindingName, decl, errTok);
        return decl;
    }

    /**
     * Parses a class declaration.
     * <p>
     * 클래스 선언을 파싱한다.
     *
     * @return the CLASS_DECL node / CLASS_DECL 노드
     */
    private Node parseClassDeclaration() {
        return parseClassDeclaration(false);
    }

    /**
     * Parses a class declaration.
     * <p>
     * 클래스 선언을 파싱한다.
     *
     * @param allowAnonymous true for <code>export default class {}</code> / <code>export default class {}</code> 이면 true
     * @return the CLASS_DECL node / CLASS_DECL 노드
     */
    private Node parseClassDeclaration(boolean allowAnonymous) {
        Token start = tok;
        Token nameTok = peek();
        Node cls = parseClass(true, allowAnonymous);
        Node decl = mk(NodeType.CLASS_DECL, start);
        Node id = mk(NodeType.IDENT, nameTok);
        id.name = cls.name != null ? cls.name : "*default*";
        decl.a = id;
        decl.b = cls;
        decl.end = prev.end;
        declareLexical(id.name, false, cls.name != null ? nameTok : start);
        return decl;
    }

    // ------------------------------------------------------------------ modules

    /**
     * Requires that the current token is a string literal and returns the module specifier.
     * <p>
     * 현재 토큰이 문자열 리터럴인지 확인하고 모듈 지정자를 반환한다.
     *
     * @return the specifier / 지정자
     */
    private String parseModuleSpecifier() {
        if (tok.type != TokenType.STRING) {
            throw unexpected(tok);
        }
        String s = tok.value;
        next();
        if (!pinfo.requestedModules.contains(s)) {
            pinfo.requestedModules.add(s);
        }
        return s;
    }

    /**
     * Parses an import declaration.
     * <p>
     * import 선언을 파싱한다.
     *
     * @return the IMPORT node / IMPORT 노드
     */
    private Node parseImport() {
        Token t = tok;
        if (scope.parent != null) {
            throw error("An import declaration can only be used at the top level of a module.");
        }
        next();
        Node n = mk(NodeType.IMPORT, t);
        if (tok.type == TokenType.STRING) {
            n.name = parseModuleSpecifier();
            consumeSemicolon();
            return done(n);
        }
        List<String[]> bindings = new ArrayList<String[]>();
        // 기본 가져오기, 네임스페이스 가져오기, 이름 목록 순서로 읽는다.
        if (tok.type == TokenType.NAME && !isP("{")) {
            Token d = tok;
            checkBindingName(d);
            next();
            bindings.add(new String[] { "default", d.value });
            declareLexical(d.value, true, d);
            if (isP(",")) {
                next();
                parseImportRest(bindings);
            }
        } else {
            parseImportRest(bindings);
        }
        if (!isN("from")) {
            throw unexpected(tok);
        }
        next();
        String spec = parseModuleSpecifier();
        for (int i = 0; i < bindings.size(); i++) { // 가져오기 바인딩을 항목으로 기록한다.
            pinfo.imports.add(new ProgramInfo.ImportEntry(spec, bindings.get(i)[0], bindings.get(i)[1]));
        }
        n.name = spec;
        consumeSemicolon();
        return done(n);
    }

    /**
     * Parses the namespace import or the named import list.
     * <p>
     * 네임스페이스 가져오기나 이름 목록 가져오기를 파싱한다.
     *
     * @param bindings receives pairs of imported name and local name / 가져오는 이름과 로컬 이름의 쌍을 받는다
     */
    private void parseImportRest(List<String[]> bindings) {
        if (isP("*")) {
            next();
            if (!isN("as")) {
                throw unexpected(tok);
            }
            next();
            Token l = tok;
            if (l.type != TokenType.NAME) {
                throw unexpected(tok);
            }
            checkBindingName(l);
            next();
            bindings.add(new String[] { "*", l.value });
            declareLexical(l.value, true, l);
            return;
        }
        expectP("{");
        // 닫는 중괄호까지 "이름" 또는 "이름 as 로컬" 항목을 읽는다.
        while (!isP("}")) {
            Token it = tok;
            if (it.type != TokenType.NAME) {
                throw unexpected(tok);
            }
            next();
            String local = it.value;
            Token localTok = it;
            if (isN("as")) {
                next();
                localTok = tok;
                if (localTok.type != TokenType.NAME) {
                    throw unexpected(tok);
                }
                local = localTok.value;
                next();
            }
            checkBindingName(localTok);
            bindings.add(new String[] { it.value, local });
            declareLexical(local, true, localTok);
            if (!isP("}")) {
                expectP(",");
            }
        }
        expectP("}");
    }

    /**
     * Records an exported name and rejects duplicates.
     * <p>
     * 내보내는 이름을 기록하고 중복을 거부한다.
     *
     * @param name the exported name / 내보내는 이름
     * @param t    the token for errors / 오류 위치 토큰
     */
    private void addExportName(String name, Token t) {
        if (!exportedNames.add(name)) {
            throw error("Duplicate export of '" + name + "'", t);
        }
    }

    /**
     * Parses an export declaration.
     * <p>
     * export 선언을 파싱한다.
     *
     * @return the EXPORT node / EXPORT 노드
     */
    private Node parseExport() {
        Token t = tok;
        if (scope.parent != null) {
            throw error("An export declaration can only be used at the top level of a module.");
        }
        next();
        Node n = mk(NodeType.EXPORT, t);
        if (isP("*")) {
            next();
            if (isN("as")) {
                next();
                if (tok.type != TokenType.NAME) {
                    throw unexpected(tok);
                }
                Token exported = tok;
                next();
                if (!isN("from")) {
                    throw unexpected(tok);
                }
                next();
                String spec = parseModuleSpecifier();
                addExportName(exported.value, exported);
                pinfo.indirectExports.add(new ProgramInfo.ExportEntry(exported.value, spec, "*", null));
                n.name = spec;
                n.op = 3;
                consumeSemicolon();
                return done(n);
            }
            if (!isN("from")) {
                throw unexpected(tok);
            }
            next();
            String spec = parseModuleSpecifier();
            pinfo.starExports.add(new ProgramInfo.ExportEntry(null, spec, "*", null));
            n.name = spec;
            n.op = 3;
            consumeSemicolon();
            return done(n);
        }
        if (isP("{")) {
            parseExportList(n);
            return done(n);
        }
        if (isN("default")) {
            next();
            n.op = 1;
            Token d = tok;
            addExportName("default", d);
            if (isN("function")) {
                n.a = parseFunctionDeclaration(true);
                pinfo.localExports.add(new ProgramInfo.ExportEntry("default", null, null, n.a.a.name));
            } else if (isN("async") && !peek().newlineBefore && peek().isName("function")) {
                n.a = parseFunctionDeclaration(true, true);
                pinfo.localExports.add(new ProgramInfo.ExportEntry("default", null, null, n.a.a.name));
            } else if (isN("class")) {
                n.a = parseClassDeclaration(true);
                pinfo.localExports.add(new ProgramInfo.ExportEntry("default", null, null, n.a.a.name));
            } else {
                n.a = parseAssignment(false);
                n.flag = true;
                consumeSemicolon();
                declareLexical("*default*", false, d);
                pinfo.localExports.add(new ProgramInfo.ExportEntry("default", null, null, "*default*"));
            }
            return done(n);
        }
        // export 뒤에 선언이 오는 형태: var, let, const, function, class
        n.op = 0;
        if (isN("var") || isN("const") || (isN("let") && isLetDeclaration())) {
            Node decl = parseVarStatement(isN("var") ? Op.VAR : (isN("const") ? Op.CONST : Op.LET));
            n.a = decl;
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < decl.list.length; i++) { // 선언자의 바인딩 이름을 모은다.
                boundNames(decl.list[i].a, names);
            }
            for (int i = 0; i < names.size(); i++) { // 이름마다 export 항목을 기록한다.
                addExportName(names.get(i), t);
                pinfo.localExports.add(new ProgramInfo.ExportEntry(names.get(i), null, null, names.get(i)));
            }
        } else if (isN("function")) {
            Token nt = peek();
            n.a = parseFunctionDeclaration(false);
            addExportName(n.a.a.name, nt);
            pinfo.localExports.add(new ProgramInfo.ExportEntry(n.a.a.name, null, null, n.a.a.name));
        } else if (isN("async") && !peek().newlineBefore && peek().isName("function")) {
            Token nt = peek(2);
            n.a = parseFunctionDeclaration(false, true);
            addExportName(n.a.a.name, nt);
            pinfo.localExports.add(new ProgramInfo.ExportEntry(n.a.a.name, null, null, n.a.a.name));
        } else if (isN("class")) {
            Token nt = peek();
            n.a = parseClassDeclaration(false);
            addExportName(n.a.a.name, nt);
            pinfo.localExports.add(new ProgramInfo.ExportEntry(n.a.a.name, null, null, n.a.a.name));
        } else {
            throw unexpected(tok);
        }
        return done(n);
    }

    /**
     * Parses <code>export { a, b as c } [from 'module']</code>.
     * <p>
     * <code>export { a, b as c } [from 'module']</code> 을 파싱한다.
     *
     * @param n the EXPORT node to fill / 채울 EXPORT 노드
     */
    private void parseExportList(Node n) {
        expectP("{");
        List<Token[]> items = new ArrayList<Token[]>();
        // 닫는 중괄호까지 "이름" 또는 "이름 as 내보낼이름" 항목을 읽는다.
        while (!isP("}")) {
            Token local = tok;
            if (local.type != TokenType.NAME) {
                throw unexpected(tok);
            }
            next();
            Token exported = local;
            if (isN("as")) {
                next();
                exported = tok;
                if (exported.type != TokenType.NAME) {
                    throw unexpected(tok);
                }
                next();
            }
            items.add(new Token[] { local, exported });
            if (!isP("}")) {
                expectP(",");
            }
        }
        expectP("}");
        if (isN("from")) {
            next();
            String spec = parseModuleSpecifier();
            n.name = spec;
            n.op = 3;
            for (int i = 0; i < items.size(); i++) { // 다시 내보내기 항목을 기록한다.
                addExportName(items.get(i)[1].value, items.get(i)[1]);
                pinfo.indirectExports.add(new ProgramInfo.ExportEntry(items.get(i)[1].value, spec, items.get(i)[0].value, null));
            }
        } else {
            n.op = 2;
            for (int i = 0; i < items.size(); i++) { // 로컬 export 항목을 기록한다.
                Token local = items.get(i)[0];
                if (isReservedWord(local.value)) {
                    throw error("Unexpected reserved word", local);
                }
                addExportName(items.get(i)[1].value, items.get(i)[1]);
                exportRefs.add(local);
                pinfo.localExports.add(new ProgramInfo.ExportEntry(items.get(i)[1].value, null, null, local.value));
            }
        }
        consumeSemicolon();
    }
}
