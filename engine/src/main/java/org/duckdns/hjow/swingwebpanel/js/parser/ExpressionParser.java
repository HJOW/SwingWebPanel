package org.duckdns.hjow.swingwebpanel.js.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.NumberText;
import org.duckdns.hjow.swingwebpanel.js.ast.FunctionInfo;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.Op;
import org.duckdns.hjow.swingwebpanel.js.ast.TemplateParts;

/**
 * The expression part of the parser (ECMA-262 6th edition clauses 12, 13.3.3, 14): operator precedence, assignment with the
 * cover grammar for destructuring, arrow functions (speculative parse of the parameter list), template literals, object and array
 * literals, functions, methods and classes, binding patterns and the early errors that belong to them. Statements and function
 * bodies are parsed by {@link Parser}.
 * <p>
 * Accepted as a harmless extension of ES2015: a trailing comma in argument lists and parameter lists (ES2017).
 * <p>
 * 파서의 표현식 부분(ECMA-262 6판 12, 13.3.3, 14장): 연산자 우선순위, 구조 분해를 위한 커버 문법이 있는 대입, 화살표 함수(매개변수 목록을
 * 추측 파싱), 템플릿 리터럴, 객체/배열 리터럴, 함수, 메서드, 클래스, 바인딩 패턴과 그에 속한 조기 오류. 문장과 함수 본문은
 * {@link Parser} 가 파싱한다.
 * <p>
 * ES2015 의 무해한 확장으로 받아들이는 것: 인수 목록과 매개변수 목록의 마지막 쉼표(ES2017).
 */
abstract class ExpressionParser extends ParserBase {

    /** Private declaration kinds. / private 선언 종류. */
    private static final int PRIVATE_FIELD = 1;
    /** Private method kind. / private 메서드 종류. */
    private static final int PRIVATE_METHOD = 2;
    /** Private getter kind. / private getter 종류. */
    private static final int PRIVATE_GETTER = 4;
    /** Private setter kind. / private setter 종류. */
    private static final int PRIVATE_SETTER = 8;

    /**
     * Private-name environment for one class.
     * <p>
     * 클래스 하나의 private 이름 환경.
     */
    private static final class PrivateEnvironment {
        /** Enclosing class environment. / 바깥 클래스 환경. */
        final PrivateEnvironment parent;
        /** Names declared by this class. / 이 클래스가 선언한 이름. */
        final Map<String, PrivateDeclaration> names = new HashMap<String, PrivateDeclaration>();

        /**
         * Creates a class environment.
         * <p>
         * 클래스 환경을 만든다.
         *
         * @param parent enclosing class environment / 바깥 환경
         */
        PrivateEnvironment(PrivateEnvironment parent) {
            this.parent = parent;
        }
    }

    /**
     * One declared private slot.
     * <p>
     * 하나의 private 슬롯 선언.
     */
    private static final class PrivateDeclaration {
        /** True for a static private element. / static private 요소이면 true. */
        final boolean staticElement;
        /** Declaration kind bits. / 선언 종류 비트. */
        int kinds;

        /**
         * Creates a private declaration.
         * <p>
         * private 선언을 만든다.
         */
        PrivateDeclaration(boolean staticElement, int kinds) {
            this.staticElement = staticElement;
            this.kinds = kinds;
        }
    }

    /**
     * A private-name use resolved after all class elements are parsed.
     * <p>
     * 모든 클래스 요소를 파싱한 뒤 해석하는 private 이름 사용.
     */
    private static final class PrivateUse {
        /** Name without the hash. / 해시를 뺀 이름. */
        final String name;
        /** Source token for an error. / 오류 위치 토큰. */
        final Token token;
        /** Innermost visible class environment. / 가장 안쪽의 보이는 클래스 환경. */
        final PrivateEnvironment environment;

        /**
         * Creates a use record.
         * <p>
         * 사용 기록을 만든다.
         */
        PrivateUse(String name, Token token, PrivateEnvironment environment) {
            this.name = name;
            this.token = token;
            this.environment = environment;
        }
    }

    /** Current class private environments, innermost last. / 현재 클래스 private 환경(가장 안쪽이 마지막). */
    private final List<PrivateEnvironment> privateEnvironments = new ArrayList<PrivateEnvironment>();
    /** Private-name uses awaiting whole-program resolution. / 전체 프로그램 해석을 기다리는 private 이름 사용. */
    private final List<PrivateUse> privateUses = new ArrayList<PrivateUse>();

    /**
     * True if the last call of {@link #parsePropertyName()} read a computed key.
     * <p>
     * 마지막 {@link #parsePropertyName()} 호출이 계산된 키를 읽었으면 true.
     */
    protected boolean keyComputed;

    /**
     * Creates the parser state.
     * <p>
     * 파서 상태를 만든다.
     *
     * @param source     the source text / 소스 텍스트
     * @param sourceName the source name / 소스 이름
     * @param module     true for the Module goal / Module 목표이면 true
     */
    ExpressionParser(String source, String sourceName, boolean module) {
        super(source, sourceName, module);
    }

    /**
     * Enters a class private-name environment.
     * <p>
     * 클래스 private 이름 환경에 들어간다.
     */
    private void pushPrivateEnvironment() {
        privateEnvironments.add(new PrivateEnvironment(
                privateEnvironments.isEmpty() ? null : privateEnvironments.get(privateEnvironments.size() - 1)));
    }

    /**
     * Leaves the innermost class private-name environment.
     * <p>
     * 가장 안쪽 클래스 private 이름 환경에서 나온다.
     */
    private void popPrivateEnvironment() {
        privateEnvironments.remove(privateEnvironments.size() - 1);
    }

    /**
     * Records a private-name use in the current lexical class chain.
     * <p>
     * 현재 렉시컬 클래스 체인에서 private 이름 사용을 기록한다.
     *
     * @param token private-name token / private 이름 토큰
     */
    private void recordPrivateUse(Token token) {
        PrivateEnvironment env = privateEnvironments.isEmpty() ? null : privateEnvironments.get(privateEnvironments.size() - 1);
        privateUses.add(new PrivateUse(token.value, token, env));
    }

    /**
     * Declares a private class element and rejects duplicate private slots.
     * <p>
     * private 클래스 요소를 선언하고 중복 슬롯을 거부한다.
     *
     * @param token         private-name token / private 이름 토큰
     * @param staticElement true for a static element / static 요소이면 true
     * @param kind          declaration kind bit / 선언 종류 비트
     */
    private void declarePrivate(Token token, boolean staticElement, int kind) {
        if (privateEnvironments.isEmpty()) {
            throw error("Private name outside a class body", token);
        }
        PrivateEnvironment env = privateEnvironments.get(privateEnvironments.size() - 1);
        PrivateDeclaration old = env.names.get(token.value);
        if (old == null) {
            env.names.put(token.value, new PrivateDeclaration(staticElement, kind));
            return;
        }
        if (old.staticElement != staticElement) {
            throw error("Private name has a conflicting static declaration", token);
        }
        boolean accessorPair = (old.kinds == PRIVATE_GETTER && kind == PRIVATE_SETTER)
                || (old.kinds == PRIVATE_SETTER && kind == PRIVATE_GETTER);
        if (!accessorPair) {
            throw error("Private name has already been declared", token);
        }
        old.kinds |= kind;
    }

    /**
     * Resolves all deferred private-name uses against enclosing class environments.
     * <p>
     * 모든 지연된 private 이름 사용을 바깥 클래스 환경에서 해석한다.
     */
    protected final void resolvePrivateNames() {
        // 모든 사용을 가장 안쪽부터 찾아가며 선언된 클래스 슬롯이 있는지 검사한다.
        for (int i = 0; i < privateUses.size(); i++) {
            PrivateUse use = privateUses.get(i);
            boolean found = false;
            PrivateEnvironment env = use.environment;
            // 바깥 private 환경을 따라 올라가며 같은 이름을 찾는다.
            while (env != null) {
                if (env.names.containsKey(use.name)) {
                    found = true;
                    break;
                }
                env = env.parent;
            }
            if (!found) {
                throw error("Private name '#" + use.name + "' is not declared in an enclosing class", use.token);
            }
        }
    }

    /**
     * Parses a function body block <code>{ ... }</code> (with directive prologue) into the function description and checks
     * the strict mode rules that depend on the body.
     * <p>
     * 함수 본문 블록 <code>{ ... }</code> (지시문 서문 포함)을 파싱해 함수 설명에 넣고, 본문에 따라 달라지는 엄격 모드 규칙을 검사한다.
     *
     * @param info the function description / 함수 설명
     * @param ctx  the function context / 함수 문맥
     */
    abstract void parseFunctionBody(FunctionInfo info, FuncCtx ctx);

    /**
     * Parses one statement list item for class static blocks and ordinary parser bodies.
     * <p>
     * 클래스 static 블록과 일반 파서 본문용 문장 목록 항목 하나를 파싱한다.
     *
     * @return parsed statement or declaration / 파싱한 문장 또는 선언
     */
    abstract Node parseStatementListItem();

    // ------------------------------------------------------------------ expressions

    /**
     * Parses an Expression (comma operator).
     * <p>
     * Expression(쉼표 연산자)을 파싱한다.
     *
     * @param noIn true to forbid the in operator (for-loop initializers) / in 연산자를 금지하려면 true(for 초기화)
     * @return the node / 노드
     */
    protected Node parseExpression(boolean noIn) {
        Token start = tok;
        Node first = parseAssignment(noIn);
        if (!isP(",")) {
            return first;
        }
        List<Node> list = new ArrayList<Node>();
        list.add(first);
        // 쉼표로 이어진 식을 모두 모은다.
        while (eatP(",")) {
            list.add(parseAssignment(noIn));
        }
        Node seq = mk(NodeType.SEQUENCE, start);
        seq.list = list.toArray(new Node[list.size()]);
        return done(seq);
    }

    /**
     * Parses an AssignmentExpression.
     * <p>
     * AssignmentExpression 을 파싱한다.
     *
     * @param noIn true to forbid the in operator / in 연산자를 금지하려면 true
     * @return the node / 노드
     */
    protected Node parseAssignment(boolean noIn) {
        return parseAssignmentCover(noIn, false);
    }

    /**
     * Parses an AssignmentExpression, optionally keeping a pending cover-grammar error for the caller (array and object
     * literal elements may still become destructuring patterns).
     * <p>
     * AssignmentExpression 을 파싱한다. 필요하면 처리되지 않은 커버 문법 오류를 호출자에게 남긴다(배열/객체 리터럴의 요소는
     * 나중에 구조 분해 패턴이 될 수 있다).
     *
     * @param noIn       true to forbid in / in 을 금지하려면 true
     * @param allowCover true if the caller may still convert the result to a pattern / 호출자가 결과를 패턴으로 바꿀 수 있으면 true
     * @return the node / 노드
     */
    protected Node parseAssignmentCover(boolean noIn, boolean allowCover) {
        if (canAwait() && isN("await")) {
            return parseAwait();
        }
        if (fc.generator && isN("yield")) {
            return parseYield(noIn);
        }
        if (isN("async") && !peek().newlineBefore) {
            Token n = peek();
            if (n.type == TokenType.NAME && !isReservedWord(n.value) && peek(2).isPunct("=>") && !peek(2).newlineBefore) {
                return parseAsyncIdentifierArrow(noIn);
            }
            if (n.isPunct("(")) {
                Node arrow = tryParseAsyncArrow(noIn);
                if (arrow != null) {
                    return arrow;
                }
            }
        }
        if (tok.type == TokenType.NAME && !isReservedWord(tok.value)) {
            Token n = peek();
            if (n.isPunct("=>") && !n.newlineBefore) {
                return parseIdentifierArrow(noIn);
            }
        }
        if (isP("(")) {
            Node arrow = tryParseArrow(noIn);
            if (arrow != null) {
                return arrow;
            }
        }
        Token savedCover = coverError;
        String savedMsg = coverMessage;
        coverError = null;
        Token start = tok;
        Node left = parseConditional(noIn);
        if (left.type == NodeType.PRIVATE_NAME) {
            throw error("Private name must be the left operand of 'in'", left);
        }
        int op = assignmentOp(tok);
        if (op >= 0) {
            Token opTok = tok;
            Node target;
            if (op == 0 && !left.paren && (left.type == NodeType.ARRAY || left.type == NodeType.OBJECT)) {
                target = toAssignmentTarget(left);
                coverError = null;
            } else {
                checkSimpleTarget(left, "Invalid left-hand side in assignment");
                target = left;
                if (coverError != null) {
                    throw error(coverMessage, coverError);
                }
            }
            next();
            Node right = parseAssignment(noIn);
            Node n = mk(NodeType.ASSIGN, start);
            n.op = op;
            n.a = target;
            n.b = right;
            coverError = savedCover;
            coverMessage = savedMsg;
            return done(n);
        }
        if (coverError != null) {
            if (!allowCover) {
                throw error(coverMessage, coverError);
            }
            if (savedCover != null) {
                coverError = savedCover;
                coverMessage = savedMsg;
            }
        } else {
            coverError = savedCover;
            coverMessage = savedMsg;
        }
        return left;
    }

    /**
     * Returns true where await is a keyword: an async function or the top level of a module.
     * <p>
     * await 가 키워드인 곳(async 함수 또는 모듈 최상위)이면 true.
     */
    private boolean canAwait() {
        return fc.async && !fc.classFieldInitializer && !fc.classStaticBlock
                || (moduleGoal && !fc.inFunction && !fc.classFieldInitializer && !fc.classStaticBlock);
    }

    /**
     * Parses an AwaitExpression.
     * <p>
     * AwaitExpression 을 파싱한다.
     *
     * @return the AWAIT node / AWAIT 노드
     */
    private Node parseAwait() {
        if (fc.inParams) {
            throw error("'await' is not allowed in a formal parameter list");
        }
        Token t = tok;
        next();
        Node n = mk(NodeType.AWAIT, t);
        n.hasYield = true;
        n.a = parseUnary();
        n.end = n.a.end;
        return n;
    }

    /**
     * Returns the operator code of an assignment operator token: 0 for plain =, an {@link Op} code for compound operators,
     * or -1 if the token is not an assignment operator.
     * <p>
     * 대입 연산자 토큰의 연산자 코드를 반환한다: 단순 = 은 0, 복합 연산자는 {@link Op} 코드, 대입 연산자가 아니면 -1.
     *
     * @param t the token / 토큰
     * @return the code / 코드
     */
    private int assignmentOp(Token t) {
        if (t.type != TokenType.PUNCT) {
            return -1;
        }
        String v = t.value;
        if (v.length() < 2 && !v.equals("=")) {
            return -1;
        }
        if (v.equals("=")) {
            return 0;
        }
        if (v.equals("+=")) {
            return Op.ADD;
        }
        if (v.equals("-=")) {
            return Op.SUB;
        }
        if (v.equals("*=")) {
            return Op.MUL;
        }
        if (v.equals("**=")) {
            return Op.EXP;
        }
        if (v.equals("&&=")) {
            return Op.LOGAND;
        }
        if (v.equals("||=")) {
            return Op.LOGOR;
        }
        if (v.equals("??=")) {
            return Op.COALESCE;
        }
        if (v.equals("/=")) {
            return Op.DIV;
        }
        if (v.equals("%=")) {
            return Op.MOD;
        }
        if (v.equals("<<=")) {
            return Op.SHL;
        }
        if (v.equals(">>=")) {
            return Op.SHR;
        }
        if (v.equals(">>>=")) {
            return Op.USHR;
        }
        if (v.equals("&=")) {
            return Op.BITAND;
        }
        if (v.equals("|=")) {
            return Op.BITOR;
        }
        if (v.equals("^=")) {
            return Op.BITXOR;
        }
        return -1;
    }

    /**
     * Checks that a node can be the target of a compound assignment or update: an identifier (not eval or arguments in strict
     * code) or a member access.
     * <p>
     * 노드가 복합 대입이나 증감의 대상이 될 수 있는지 검사한다: 식별자(엄격 코드에서는 eval, arguments 제외) 또는 멤버 접근.
     *
     * @param n   the node / 노드
     * @param msg the error message / 오류 메시지
     */
    protected final void checkSimpleTarget(Node n, String msg) {
        if (n.type == NodeType.IDENT) {
            if (fc.strict && (n.name.equals("eval") || n.name.equals("arguments"))) {
                throw error("Unexpected eval or arguments in strict mode", n);
            }
            return;
        }
        if (n.type == NodeType.MEMBER) {
            if (n.optional) {
                throw error(msg, n);
            }
            return;
        }
        throw error(msg, n);
    }

    /**
     * Parses a yield expression inside a generator.
     * <p>
     * 제너레이터 안의 yield 표현식을 파싱한다.
     *
     * @param noIn true to forbid in / in 을 금지하려면 true
     * @return the YIELD node / YIELD 노드
     */
    private Node parseYield(boolean noIn) {
        if (fc.inParams) {
            throw error("Yield expression not allowed in formal parameter");
        }
        Token t = tok;
        next();
        Node n = mk(NodeType.YIELD, t);
        n.hasYield = true;
        if (!tok.newlineBefore) {
            if (isP("*")) {
                next();
                n.flag = true;
                n.a = parseAssignment(noIn);
            } else if (!(isP(")") || isP("]") || isP("}") || isP(",") || isP(";") || isP(":") || tok.type == TokenType.EOF
                    || (isN("in") && noIn))) {
                n.a = parseAssignment(noIn);
            }
        }
        return done(n);
    }

    /**
     * Parses a ConditionalExpression.
     * <p>
     * ConditionalExpression 을 파싱한다.
     *
     * @param noIn true to forbid in / in 을 금지하려면 true
     * @return the node / 노드
     */
    private Node parseConditional(boolean noIn) {
        Token start = tok;
        Node test = parseBinary(0, noIn);
        if (!isP("?")) {
            return test;
        }
        next();
        Node a = parseAssignment(false);
        expectP(":");
        Node b = parseAssignment(noIn);
        Node n = mk(NodeType.CONDITIONAL, start);
        n.a = test;
        n.b = a;
        n.c = b;
        return done(n);
    }

    /**
     * Returns the binary operator code of a token or 0 if it is not a binary operator.
     * <p>
     * 토큰의 이항 연산자 코드를 반환한다. 이항 연산자가 아니면 0.
     *
     * @param t    the token / 토큰
     * @param noIn true if the in operator is forbidden / in 연산자가 금지되었으면 true
     * @return the code / 코드
     */
    private int binaryOp(Token t, boolean noIn) {
        if (t.type == TokenType.NAME) {
            if (t.escaped) {
                return 0;
            }
            if (t.value.equals("instanceof")) {
                return Op.INSTANCEOF;
            }
            if (t.value.equals("in") && !noIn) {
                return Op.IN;
            }
            return 0;
        }
        if (t.type != TokenType.PUNCT) {
            return 0;
        }
        String v = t.value;
        switch (v.length()) {
        case 1:
            switch (v.charAt(0)) {
            case '+':
                return Op.ADD;
            case '-':
                return Op.SUB;
            case '*':
                return Op.MUL;
            case '/':
                return Op.DIV;
            case '%':
                return Op.MOD;
            case '<':
                return Op.LT;
            case '>':
                return Op.GT;
            case '&':
                return Op.BITAND;
            case '|':
                return Op.BITOR;
            case '^':
                return Op.BITXOR;
            default:
                return 0;
            }
        case 2:
            if (v.equals("==")) {
                return Op.EQ;
            }
            if (v.equals("!=")) {
                return Op.NE;
            }
            if (v.equals("<=")) {
                return Op.LE;
            }
            if (v.equals(">=")) {
                return Op.GE;
            }
            if (v.equals("<<")) {
                return Op.SHL;
            }
            if (v.equals(">>")) {
                return Op.SHR;
            }
            if (v.equals("&&")) {
                return Op.LOGAND;
            }
            if (v.equals("||")) {
                return Op.LOGOR;
            }
            if (v.equals("??")) {
                return Op.COALESCE;
            }
            return 0;
        case 3:
            if (v.equals("===")) {
                return Op.SEQ;
            }
            if (v.equals("!==")) {
                return Op.SNE;
            }
            if (v.equals(">>>")) {
                return Op.USHR;
            }
            return 0;
        default:
            return 0;
        }
    }

    /**
     * Returns the precedence of a binary operator (higher binds tighter).
     * <p>
     * 이항 연산자의 우선순위를 반환한다(클수록 강하게 묶는다).
     *
     * @param op the operator code / 연산자 코드
     * @return the precedence / 우선순위
     */
    private static int precedence(int op) {
        switch (op) {
        case Op.COALESCE:
        case Op.LOGOR:
            return 1;
        case Op.LOGAND:
            return 2;
        case Op.BITOR:
            return 3;
        case Op.BITXOR:
            return 4;
        case Op.BITAND:
            return 5;
        case Op.EQ:
        case Op.NE:
        case Op.SEQ:
        case Op.SNE:
            return 6;
        case Op.LT:
        case Op.GT:
        case Op.LE:
        case Op.GE:
        case Op.INSTANCEOF:
        case Op.IN:
            return 7;
        case Op.SHL:
        case Op.SHR:
        case Op.USHR:
            return 8;
        case Op.ADD:
        case Op.SUB:
            return 9;
        case Op.EXP:
            return 11;
        default:
            return 10;
        }
    }

    /**
     * Parses binary operators with precedence climbing: only operators that bind tighter than minPrec are consumed.
     * <p>
     * 우선순위 상승법으로 이항 연산자를 파싱한다. minPrec 보다 강하게 묶는 연산자만 소비한다.
     *
     * @param minPrec the minimum (exclusive) precedence / 최소(미포함) 우선순위
     * @param noIn    true to forbid in / in 을 금지하려면 true
     * @return the node / 노드
     */
    private Node parseBinary(int minPrec, boolean noIn) {
        Token start = tok;
        Node left = parseUnary();
        // 오른쪽에 더 강하게 묶는 연산자가 이어지는 동안 반복한다.
        while (true) {
            int op = binaryOp(tok, noIn);
            if (op == 0) {
                break;
            }
            int prec = precedence(op);
            if (prec <= minPrec) {
                break;
            }
            if (mixesNullish(op, left, null)) {
                throw error("Nullish coalescing operator may not be mixed with && or || without parentheses", tok);
            }
            next();
            Node right = parseBinary(prec, noIn);
            if (mixesNullish(op, left, right)) {
                throw error("Nullish coalescing operator may not be mixed with && or || without parentheses", left);
            }
            Node n = mk(op == Op.LOGAND || op == Op.LOGOR || op == Op.COALESCE ? NodeType.LOGICAL : NodeType.BINARY, start);
            n.op = op;
            n.a = left;
            n.b = right;
            left = done(n);
        }
        return left;
    }

    /**
     * Rejects an unparenthesized nullish operator mixed with logical and/or.
     * <p>
     * 괄호 없이 nullish 연산자가 논리 AND/OR 와 섞였는지 검사한다.
     *
     * @param op    current operator / 현재 연산자
     * @param left  left expression / 왼쪽 식
     * @param right right expression or null / 오른쪽 식 또는 null
     * @return true for an early error / 조기 오류이면 true
     */
    private static boolean mixesNullish(int op, Node left, Node right) {
        if (op == Op.COALESCE) {
            return isLogicalAndOr(left) || isLogicalAndOr(right);
        }
        if (op == Op.LOGAND || op == Op.LOGOR) {
            return isCoalesce(left) || isCoalesce(right);
        }
        return false;
    }

    /**
     * Tests the top-level logical and/or kind without looking through parentheses.
     * <p>
     * 괄호 밖으로 들어가지 않고 최상위 논리 AND/OR 종류인지 검사한다.
     */
    private static boolean isLogicalAndOr(Node n) {
        return n != null && !n.paren && n.type == NodeType.LOGICAL
                && (n.op == Op.LOGAND || n.op == Op.LOGOR);
    }

    /**
     * Tests the top-level nullish kind without looking through parentheses.
     * <p>
     * 괄호 밖으로 들어가지 않고 최상위 nullish 종류인지 검사한다.
     */
    private static boolean isCoalesce(Node n) {
        return n != null && !n.paren && n.type == NodeType.LOGICAL && n.op == Op.COALESCE;
    }

    /**
     * Parses unary and update expressions.
     * <p>
     * 단항 및 증감 표현식을 파싱한다.
     *
     * @return the node / 노드
     */
    private Node parseUnary() {
        Token t = tok;
        if (canAwait() && isN("await")) {
            return parseAwait();
        }
        int op = 0;
        if (t.type == TokenType.PUNCT) {
            String v = t.value;
            if (v.equals("!")) {
                op = Op.NOT;
            } else if (v.equals("~")) {
                op = Op.BITNOT;
            } else if (v.equals("+")) {
                op = Op.POS;
            } else if (v.equals("-")) {
                op = Op.NEG;
            } else if (v.equals("++") || v.equals("--")) {
                next();
                Node operand = parseUnary();
                checkSimpleTarget(operand, "Invalid left-hand side expression in prefix operation");
                Node n = mk(NodeType.UPDATE, t);
                n.op = v.equals("++") ? Op.INC : Op.DEC;
                n.flag = true;
                n.a = operand;
                return done(n);
            }
        } else if (t.type == TokenType.NAME && !t.escaped) {
            if (t.value.equals("typeof")) {
                op = Op.TYPEOF;
            } else if (t.value.equals("void")) {
                op = Op.VOID;
            } else if (t.value.equals("delete")) {
                op = Op.DELETE;
            }
        }
        if (op != 0) {
            next();
            Node operand = parseUnary();
            if (operand.type == NodeType.BINARY && operand.op == Op.EXP && !operand.paren) {
                throw error("Unary expression may not be the left-hand side of exponentiation", t);
            }
            if (op == Op.DELETE && fc.strict && operand.type == NodeType.IDENT) {
                throw error("Delete of an unqualified identifier in strict mode.", operand);
            }
            if (op == Op.DELETE && operand.type == NodeType.MEMBER && operand.privateName) {
                throw error("Private fields cannot be deleted", operand);
            }
            Node n = mk(NodeType.UNARY, t);
            n.op = op;
            n.a = operand;
            return done(n);
        }
        Node e = parseLeftHandSide();
        if ((isP("++") || isP("--")) && !tok.newlineBefore) {
            checkSimpleTarget(e, "Invalid left-hand side expression in postfix operation");
            Node n = mk(NodeType.UPDATE, t);
            n.op = tok.value.equals("++") ? Op.INC : Op.DEC;
            n.a = e;
            next();
            e = done(n);
        }
        if (isP("**")) {
            next();
            Node n = mk(NodeType.BINARY, t);
            n.op = Op.EXP;
            n.a = e;
            n.b = parseUnary();
            return done(n);
        }
        return e;
    }

    /**
     * Parses a LeftHandSideExpression: new, call and member expressions.
     * <p>
     * LeftHandSideExpression 을 파싱한다: new, 호출, 멤버 표현식.
     *
     * @return the node / 노드
     */
    protected final Node parseLeftHandSide() {
        Token start = tok;
        Node e;
        if (isN("new")) {
            e = parseNew();
        } else if (isN("super")) {
            e = parseSuper();
        } else {
            e = parsePrimary();
        }
        return parseCallTail(e, start, true);
    }

    /**
     * Parses member accesses, calls and tagged templates after an expression.
     * <p>
     * 식 뒤의 멤버 접근, 호출, 태그 템플릿을 파싱한다.
     *
     * @param e         the expression so far / 지금까지의 식
     * @param start     the first token of the whole expression / 전체 식의 첫 토큰
     * @param allowCall true if calls are allowed (false for the callee of new) / 호출을 허용하면 true(new 의 callee 는 false)
     * @return the node / 노드
     */
    private Node parseCallTail(Node e, Token start, boolean allowCall) {
        boolean optional = false;
        // 점, 대괄호, 호출, 템플릿이 이어지는 동안 식을 감싸 나간다.
        while (true) {
            if (isP("?.")) {
                if (!allowCall) {
                    throw error("Invalid optional chain from new expression", tok);
                }
                optional = true;
                next();
                if (isP("(")) {
                    Node call = mk(NodeType.CALL, start);
                    call.a = e;
                    call.optional = true;
                    call.list = parseArguments();
                    e = done(call);
                } else if (isP("[")) {
                    next();
                    Node key = parseExpression(false);
                    expectP("]");
                    Node m = mk(NodeType.MEMBER, start);
                    m.a = e;
                    m.b = key;
                    m.flag = true;
                    m.optional = true;
                    e = done(m);
                } else {
                    if (tok.type != TokenType.NAME) {
                        throw unexpected(tok);
                    }
                    Node m = mk(NodeType.MEMBER, start);
                    m.a = e;
                    m.name = tok.value;
                    m.optional = true;
                    next();
                    e = done(m);
                }
            } else if (isP(".")) {
                next();
                if (tok.type != TokenType.NAME && tok.type != TokenType.PRIVATE_NAME) {
                    throw unexpected(tok);
                }
                Token property = tok;
                Node m = mk(NodeType.MEMBER, start);
                m.a = e;
                m.privateName = tok.type == TokenType.PRIVATE_NAME;
                m.name = m.privateName ? "#" + tok.value : tok.value;
                if (m.privateName) {
                    recordPrivateUse(property);
                }
                next();
                e = done(m);
            } else if (isP("[")) {
                next();
                Node key = parseExpression(false);
                expectP("]");
                Node m = mk(NodeType.MEMBER, start);
                m.a = e;
                m.b = key;
                m.flag = true;
                e = done(m);
            } else if (isP("(") && allowCall) {
                Node call = mk(NodeType.CALL, start);
                call.a = e;
                call.list = parseArguments();
                if (e.type == NodeType.IDENT && e.name.equals("eval")) {
                    markEval();
                }
                if (e.type == NodeType.SUPER && !fc.allowSuperCall) {
                    throw error("'super' keyword unexpected here", e);
                }
                e = done(call);
            } else if (tok.type == TokenType.TEMPLATE) {
                if (optional) {
                    throw error("Invalid tagged template on optional chain", tok);
                }
                Node tt = mk(NodeType.TAGGED_TEMPLATE, start);
                tt.a = e;
                tt.b = parseTemplate(true);
                e = done(tt);
            } else {
                if (!optional) {
                    return e;
                }
                Node chain = mk(NodeType.OPTIONAL_CHAIN, start);
                chain.a = e;
                return done(chain);
            }
        }
    }

    /**
     * Marks the enclosing scopes and functions as containing a direct eval call.
     * <p>
     * 바깥 스코프와 함수에 eval 직접 호출이 있다고 표시한다.
     */
    private void markEval() {
        // 모든 바깥 스코프와 함수 문맥에서 eval 이 지역 변수에 접근할 수 있다.
        for (Scope s = scope; s != null; s = s.parent) {
            s.hasEval = true;
        }
        for (FuncCtx c = fc; c != null; c = c.parent) { // 바깥 함수 문맥으로 올라가며 표시한다.
            c.hasEval = true;
        }
    }

    /**
     * Parses the argument list of a call or new expression.
     * <p>
     * 호출이나 new 표현식의 인수 목록을 파싱한다.
     *
     * @return the arguments / 인수들
     */
    private Node[] parseArguments() {
        expectP("(");
        List<Node> args = new ArrayList<Node>();
        // 닫는 괄호가 나올 때까지 인수를 읽는다.
        while (!isP(")")) {
            if (isP("...")) {
                Token s = tok;
                next();
                Node sp = mk(NodeType.SPREAD, s);
                sp.a = parseAssignment(false);
                args.add(done(sp));
            } else {
                args.add(parseAssignment(false));
            }
            if (!isP(")")) {
                expectP(",");
            }
        }
        expectP(")");
        return args.toArray(new Node[args.size()]);
    }

    /**
     * Parses a new expression or new.target.
     * <p>
     * new 표현식이나 new.target 을 파싱한다.
     *
     * @return the node / 노드
     */
    private Node parseNew() {
        Token t = tok;
        next();
        if (isP(".")) {
            next();
            if (!(tok.type == TokenType.NAME && tok.value.equals("target") && !tok.escaped)) {
                throw unexpected(tok);
            }
            if (!fc.allowNewTarget) {
                throw error("new.target expression is not allowed here", t);
            }
            next();
            return done(mk(NodeType.NEW_TARGET, t));
        }
        Token calleeStart = tok;
        Node callee;
        if (isN("new")) {
            callee = parseNew();
        } else if (isN("super")) {
            throw error("'super' keyword unexpected here");
        } else {
            callee = parsePrimary();
        }
        callee = parseCallTail(callee, calleeStart, false);
        Node n = mk(NodeType.NEW, t);
        n.a = callee;
        n.list = isP("(") ? parseArguments() : new Node[0];
        return done(n);
    }

    /**
     * Parses super.x, super[x] or super(...).
     * <p>
     * super.x, super[x], super(...) 를 파싱한다.
     *
     * @return the SUPER node (the caller continues with the member or call) / SUPER 노드(호출자가 멤버나 호출을 이어서 파싱)
     */
    private Node parseSuper() {
        Token t = tok;
        next();
        if (isP("(")) {
            if (!fc.allowSuperCall) {
                throw error("'super' keyword unexpected here", t);
            }
        } else if (isP(".") || isP("[")) {
            if (!fc.allowSuperProp) {
                throw error("'super' keyword unexpected here", t);
            }
        } else {
            throw error("'super' keyword unexpected here", t);
        }
        return done(mk(NodeType.SUPER, t));
    }

    /**
     * Parses a PrimaryExpression.
     * <p>
     * PrimaryExpression 을 파싱한다.
     *
     * @return the node / 노드
     */
    private Node parsePrimary() {
        Token t = tok;
        switch (t.type) {
        case NAME:
            return parsePrimaryName(t);
        case PRIVATE_NAME: {
            if (!peek().isName("in")) {
                throw error("Private name must be used in an 'in' expression", t);
            }
            recordPrivateUse(t);
            next();
            Node n = mk(NodeType.PRIVATE_NAME, t);
            n.name = "#" + t.value;
            n.privateName = true;
            return done(n);
        }
        case NUMBER: {
            if (fc.strict && t.legacyOctal) {
                throw error("Octal literals are not allowed in strict mode.", t);
            }
            next();
            Node n = mk(NodeType.LITERAL, t);
            n.value = JsNumber.of(t.number);
            return done(n);
        }
        case BIGINT: {
            next();
            Node n = mk(NodeType.LITERAL, t);
            n.value = t.bigInteger;
            return done(n);
        }
        case STRING: {
            if (fc.strict && t.legacyOctal) {
                throw error("Octal escape sequences are not allowed in strict mode.", t);
            }
            next();
            Node n = mk(NodeType.LITERAL, t);
            n.value = JsString.of(t.value);
            return done(n);
        }
        case TEMPLATE:
            return parseTemplate(false);
        case PUNCT:
            break;
        default:
            throw unexpected(t);
        }
        if (isP("(")) {
            next();
            Node e = parseExpression(false);
            expectP(")");
            e.paren = true;
            return e;
        }
        if (isP("[")) {
            return parseArrayLiteral();
        }
        if (isP("{")) {
            return parseObjectLiteral();
        }
        if (isP("/") || isP("/=")) {
            Token re = lexer.rescanRegExp(t);
            if (regExpValidator != null) {
                String err = regExpValidator.validate(re.value, re.flags);
                if (err != null) {
                    throw error(err, re);
                }
            }
            tok = re;
            next();
            Node n = mk(NodeType.REGEXP, re);
            n.name = re.value;
            n.value = re.flags;
            return done(n);
        }
        throw unexpected(t);
    }

    /**
     * Parses a primary expression that starts with a name: this, literals, function, class or an identifier reference.
     * <p>
     * 이름으로 시작하는 기본 표현식을 파싱한다: this, 리터럴, function, class, 식별자 참조.
     *
     * @param t the current NAME token / 현재 NAME 토큰
     * @return the node / 노드
     */
    private Node parsePrimaryName(Token t) {
        if (!t.escaped) {
            String v = t.value;
            if (v.equals("async") && !peek().newlineBefore && peek().isName("function")) {
                return parseFunctionExpression(true);
            }
            if (v.equals("import")) {
                Token next = peek();
                if (next.isPunct("(")) {
                    return parseImportCall();
                }
                if (next.isPunct(".")) {
                    if (!moduleGoal) {
                        throw error("import.meta is only valid in a module", t);
                    }
                    next();
                    expectP(".");
                    if (!isN("meta")) {
                        throw unexpected(tok);
                    }
                    next();
                    return done(mk(NodeType.IMPORT_META, t));
                }
            }
            if (v.equals("this")) {
                next();
                return done(mk(NodeType.THIS, t));
            }
            if (v.equals("null")) {
                next();
                Node n = mk(NodeType.LITERAL, t);
                n.value = Js.NULL;
                return done(n);
            }
            if (v.equals("true") || v.equals("false")) {
                next();
                Node n = mk(NodeType.LITERAL, t);
                n.value = JsBoolean.of(v.equals("true"));
                return done(n);
            }
            if (v.equals("function")) {
                return parseFunctionExpression();
            }
            if (v.equals("class")) {
                return parseClass(false, false);
            }
        }
        checkIdentifier(t, false);
        if (t.value.equals("arguments")) {
            if (fc.classFieldInitializer) {
                throw error("'arguments' is not allowed in a class field initializer", t);
            }
            markArguments();
        }
        next();
        Node n = mk(NodeType.IDENT, t);
        n.name = t.value;
        return done(n);
    }

    /**
     * Parses a dynamic import call.
     * <p>
     * 동적 import 호출을 파싱한다.
     *
     * @return the IMPORT_CALL node / IMPORT_CALL 노드
     */
    private Node parseImportCall() {
        Token start = tok;
        next();
        expectP("(");
        Node specifier = parseAssignment(false);
        if (isP(",")) {
            throw error("A dynamic import accepts one specifier in ECMAScript 2022", tok);
        }
        expectP(")");
        Node n = mk(NodeType.IMPORT_CALL, start);
        n.a = specifier;
        return done(n);
    }

    /**
     * Records that the code refers to arguments: the nearest non-arrow function uses it.
     * <p>
     * 코드가 arguments 를 참조함을 기록한다: 가장 가까운 비 화살표 함수가 사용하는 것이다.
     */
    protected final void markArguments() {
        FuncCtx c = fc;
        // 화살표 함수는 바깥 함수의 arguments 를 쓰므로 바깥으로 올라간다.
        while (c.arrow && c.parent != null) {
            c = c.parent;
        }
        c.usesArguments = true;
    }

    // ------------------------------------------------------------------ literals

    /**
     * Parses a template literal; untagged templates must not contain invalid escapes.
     * <p>
     * 템플릿 리터럴을 파싱한다. 태그가 없는 템플릿은 잘못된 이스케이프를 가질 수 없다.
     *
     * @param tagged true for the template of a tagged template / 태그 템플릿의 템플릿이면 true
     * @return the TEMPLATE node / TEMPLATE 노드
     */
    protected final Node parseTemplate(boolean tagged) {
        Token start = tok;
        List<String> cooked = new ArrayList<String>();
        List<String> raw = new ArrayList<String>();
        List<Node> exprs = new ArrayList<Node>();
        Token part = tok;
        // 템플릿 조각과 치환식을 번갈아 읽는다.
        while (true) {
            if (part.templateError != null && !tagged) {
                Token errTok = new Token();
                errTok.line = part.templateErrorLine;
                errTok.column = part.templateErrorColumn;
                throw error(part.templateError, errTok);
            }
            cooked.add(part.value);
            raw.add(part.raw);
            if (part.tail) {
                next();
                break;
            }
            next();
            exprs.add(parseExpression(false));
            if (!isP("}")) {
                throw unexpected(tok);
            }
            part = lexer.rescanTemplateContinuation(tok);
            tok = part;
        }
        Node n = mk(NodeType.TEMPLATE, start);
        n.list = exprs.toArray(new Node[exprs.size()]);
        n.value = new TemplateParts(cooked.toArray(new String[cooked.size()]), raw.toArray(new String[raw.size()]));
        return done(n);
    }

    /**
     * Parses an array literal.
     * <p>
     * 배열 리터럴을 파싱한다.
     *
     * @return the ARRAY node / ARRAY 노드
     */
    private Node parseArrayLiteral() {
        Token start = tok;
        next();
        List<Node> elems = new ArrayList<Node>();
        boolean trailingCommaAfterSpread = false;
        // 닫는 대괄호까지 요소, 빈 칸, 전개 요소를 읽는다.
        while (!isP("]")) {
            if (isP(",")) {
                next();
                elems.add(null);
                continue;
            }
            boolean spread = false;
            if (isP("...")) {
                Token s = tok;
                next();
                Node sp = mk(NodeType.SPREAD, s);
                sp.a = parseAssignmentCover(false, true);
                elems.add(done(sp));
                spread = true;
            } else {
                elems.add(parseAssignmentCover(false, true));
            }
            if (!isP("]")) {
                expectP(",");
                // 전개 요소 뒤의 쉼표는 구조 분해 패턴으로 바꿀 때 오류가 된다(나머지는 마지막이어야 함).
                if (spread && isP("]")) {
                    trailingCommaAfterSpread = true;
                }
            }
        }
        expectP("]");
        Node n = mk(NodeType.ARRAY, start);
        n.list = elems.toArray(new Node[elems.size()]);
        n.flag = trailingCommaAfterSpread;
        return done(n);
    }

    /**
     * Parses a property name: an identifier name, a string, a number or a computed key in brackets. The result is a
     * LITERAL string node (or the key expression when computed); {@link #keyComputed} tells which.
     * <p>
     * 프로퍼티 이름을 파싱한다: 식별자 이름, 문자열, 숫자, 대괄호의 계산된 키. 결과는 문자열 LITERAL 노드(계산된 키이면 키 식)이며
     * {@link #keyComputed} 로 구분한다.
     *
     * @return the key node / 키 노드
     */
    protected final Node parsePropertyName() {
        Token t = tok;
        keyComputed = false;
        if (t.type == TokenType.NAME) {
            next();
            Node n = mk(NodeType.LITERAL, t);
            n.value = JsString.of(t.value);
            n.name = t.value;
            return done(n);
        }
        if (t.type == TokenType.STRING) {
            if (fc.strict && t.legacyOctal) {
                throw error("Octal escape sequences are not allowed in strict mode.", t);
            }
            next();
            Node n = mk(NodeType.LITERAL, t);
            n.value = JsString.of(t.value);
            n.name = t.value;
            return done(n);
        }
        if (t.type == TokenType.NUMBER) {
            if (fc.strict && t.legacyOctal) {
                throw error("Octal literals are not allowed in strict mode.", t);
            }
            next();
            Node n = mk(NodeType.LITERAL, t);
            String text = NumberText.toString(t.number);
            n.value = JsString.of(text);
            n.name = text;
            return done(n);
        }
        if (isP("[")) {
            next();
            Node key = parseAssignment(false);
            expectP("]");
            keyComputed = true;
            return key;
        }
        throw unexpected(t);
    }

    /**
     * Parses an object literal.
     * <p>
     * 객체 리터럴을 파싱한다.
     *
     * @return the OBJECT node / OBJECT 노드
     */
    private Node parseObjectLiteral() {
        Token start = tok;
        next();
        List<Node> props = new ArrayList<Node>();
        boolean sawProto = false;
        // 닫는 중괄호까지 프로퍼티를 읽는다.
        while (!isP("}")) {
            Node p = parseObjectProperty();
            if (p.type == NodeType.PROPERTY && p.op == Op.PROP_INIT && !p.flag && p.a.type == NodeType.LITERAL
                    && "__proto__".equals(p.a.name)) {
                if (sawProto && coverError == null) {
                    coverError = tok;
                    coverMessage = "Duplicate __proto__ fields are not allowed in object literals";
                }
                sawProto = true;
            }
            props.add(p);
            if (!isP("}")) {
                expectP(",");
            }
        }
        expectP("}");
        Node n = mk(NodeType.OBJECT, start);
        n.list = props.toArray(new Node[props.size()]);
        return done(n);
    }

    /**
     * Returns true if the token after get/set starts a property name, which makes it an accessor.
     * <p>
     * get/set 다음 토큰이 프로퍼티 이름의 시작이면 true(그러면 접근자이다).
     *
     * @param n the token after get or set / get 또는 set 다음 토큰
     * @return true if it starts a property name / 프로퍼티 이름의 시작이면 true
     */
    protected final boolean startsPropertyName(Token n) {
        return n.type == TokenType.NAME || n.type == TokenType.STRING || n.type == TokenType.NUMBER
                || n.type == TokenType.PRIVATE_NAME || n.isPunct("[");
    }

    /**
     * Parses one property of an object literal.
     * <p>
     * 객체 리터럴의 프로퍼티 하나를 파싱한다.
     *
     * @return the PROPERTY node / PROPERTY 노드
     */
    private Node parseObjectProperty() {
        Token start = tok;
        if (isP("...")) {
            next();
            Node spread = mk(NodeType.SPREAD, start);
            spread.a = parseAssignment(false);
            return done(spread);
        }
        Node prop = mk(NodeType.PROPERTY, start);
        boolean async = false;
        if (isN("async") && !peek().newlineBefore && (peek().isPunct("*") || startsPropertyName(peek()))) {
            async = true;
            next();
        }
        if (isP("*")) {
            next();
            prop.a = parsePropertyName();
            prop.flag = keyComputed;
            prop.op = Op.PROP_METHOD;
            prop.b = parseMethodFunction(start, prop, true, false, false, false, false, async);
            return done(prop);
        }
        if (tok.type == TokenType.NAME && !tok.escaped && (tok.value.equals("get") || tok.value.equals("set")) && startsPropertyName(peek())) {
            boolean getter = tok.value.equals("get");
            next();
            prop.a = parsePropertyName();
            prop.flag = keyComputed;
            prop.op = getter ? Op.PROP_GET : Op.PROP_SET;
            prop.b = parseMethodFunction(start, prop, false, getter, !getter, false, false, false);
            return done(prop);
        }
        Token keyTok = tok;
        prop.a = parsePropertyName();
        prop.flag = keyComputed;
        if (isP("(")) {
            prop.op = Op.PROP_METHOD;
            prop.b = parseMethodFunction(start, prop, false, false, false, false, false, async);
            return done(prop);
        }
        if (isP(":")) {
            next();
            prop.op = Op.PROP_INIT;
            prop.b = parseAssignmentCover(false, true);
            return done(prop);
        }
        // 축약 프로퍼티: 키가 올바른 식별자 참조여야 한다.
        if (keyTok.type != TokenType.NAME || prop.flag) {
            throw unexpected(tok);
        }
        checkIdentifier(keyTok, false);
        if (keyTok.value.equals("arguments")) {
            markArguments();
        }
        Node id = mk(NodeType.IDENT, keyTok);
        id.name = keyTok.value;
        id.end = keyTok.end;
        if (isP("=")) {
            if (coverError == null) {
                coverError = tok;
                coverMessage = "Invalid shorthand property initializer";
            }
            next();
            Node def = mk(NodeType.DEFAULT, keyTok);
            def.a = id;
            def.b = parseAssignment(false);
            prop.op = Op.PROP_COVER_INIT;
            prop.b = done(def);
        } else {
            prop.op = Op.PROP_SHORTHAND;
            prop.b = id;
        }
        return done(prop);
    }

    // ------------------------------------------------------------------ patterns

    /**
     * Converts an array or object literal (or a simple target) into an assignment pattern for destructuring assignment
     * and for-in/of heads.
     * <p>
     * 배열이나 객체 리터럴(또는 단순 대상)을 구조 분해 대입과 for-in/of 머리용 대입 패턴으로 변환한다.
     *
     * @param n the expression / 식
     * @return the pattern / 패턴
     */
    protected final Node toAssignmentTarget(Node n) {
        switch (n.type) {
        case NodeType.IDENT:
            if (fc.strict && (n.name.equals("eval") || n.name.equals("arguments"))) {
                throw error("Unexpected eval or arguments in strict mode", n);
            }
            return n;
        case NodeType.MEMBER:
            return n;
        case NodeType.ARRAY: {
            if (n.paren) {
                throw error("Invalid destructuring assignment target", n);
            }
            if (n.flag) {
                throw error("Rest element must be last element", n);
            }
            Node p = new Node(NodeType.ARRAY_PATTERN, n.line, n.column);
            p.start = n.start;
            p.end = n.end;
            p.list = new Node[n.list.length];
            // 각 요소를 대상 패턴으로 바꾼다. 나머지(...)는 마지막이어야 한다.
            for (int i = 0; i < n.list.length; i++) {
                Node e = n.list[i];
                if (e == null) {
                    continue;
                }
                if (e.type == NodeType.SPREAD) {
                    if (i != n.list.length - 1) {
                        throw error("Rest element must be last element", e);
                    }
                    Node r = new Node(NodeType.REST, e.line, e.column);
                    r.a = toAssignmentTargetOrDefault(e.a);
                    if (r.a.type == NodeType.DEFAULT) {
                        throw error("Rest element may not have a default initializer", e);
                    }
                    p.list[i] = r;
                } else {
                    p.list[i] = toAssignmentTargetOrDefault(e);
                }
            }
            return p;
        }
        case NodeType.OBJECT: {
            if (n.paren) {
                throw error("Invalid destructuring assignment target", n);
            }
            Node p = new Node(NodeType.OBJECT_PATTERN, n.line, n.column);
            p.start = n.start;
            p.end = n.end;
            p.list = new Node[n.list.length];
            // 객체 리터럴의 프로퍼티를 패턴 프로퍼티로 바꾼다.
            for (int i = 0; i < n.list.length; i++) {
                Node q = n.list[i];
                if (q.type == NodeType.SPREAD) {
                    if (i != n.list.length - 1) {
                        throw error("Rest element must be last element", q);
                    }
                    Node rest = new Node(NodeType.REST, q.line, q.column);
                    rest.start = q.start;
                    rest.end = q.end;
                    rest.a = toAssignmentTarget(q.a);
                    p.list[i] = rest;
                    continue;
                }
                Node pp = new Node(NodeType.PATTERN_PROP, q.line, q.column);
                pp.a = q.a;
                pp.flag = q.flag;
                switch (q.op) {
                case Op.PROP_INIT:
                    pp.b = toAssignmentTargetOrDefault(q.b);
                    break;
                case Op.PROP_SHORTHAND:
                    pp.b = toAssignmentTarget(q.b);
                    break;
                case Op.PROP_COVER_INIT:
                    toAssignmentTarget(q.b.a);
                    pp.b = q.b;
                    break;
                default:
                    throw error("Invalid destructuring assignment target", q);
                }
                p.list[i] = pp;
            }
            return p;
        }
        default:
            throw error("Invalid destructuring assignment target", n);
        }
    }

    /**
     * Converts a pattern element that may carry a default value (<code>x = 1</code>).
     * <p>
     * 기본값(<code>x = 1</code>)을 가질 수 있는 패턴 요소를 변환한다.
     *
     * @param n the expression / 식
     * @return the pattern element / 패턴 요소
     */
    private Node toAssignmentTargetOrDefault(Node n) {
        if (n.type == NodeType.ASSIGN && n.op == 0 && !n.paren) {
            Node d = new Node(NodeType.DEFAULT, n.line, n.column);
            d.a = n.a;
            d.b = n.b;
            d.start = n.start;
            d.end = n.end;
            d.hasYield = n.hasYield;
            return d;
        }
        if (n.type == NodeType.DEFAULT) {
            return n;
        }
        return toAssignmentTarget(n);
    }

    /**
     * Declares a name bound by a binding pattern.
     * <p>
     * 바인딩 패턴이 묶는 이름을 선언한다.
     *
     * @param name the name / 이름
     * @param kind the BIND_ kind / BIND_ 종류
     * @param t    the token for errors / 오류 위치 토큰
     */
    protected final void declareBinding(String name, int kind, Token t) {
        switch (kind) {
        case BIND_VAR:
            declareVar(name, t);
            break;
        case BIND_LET:
        case BIND_CONST:
            if (name.equals("let")) {
                throw error("let is disallowed as a lexically bound name", t);
            }
            declareLexical(name, kind == BIND_CONST, t);
            break;
        case BIND_PARAM:
            scope.params.add(name);
            break;
        case BIND_CATCH:
            if (scope.lex.containsKey(name)) {
                throw redeclared(name, t);
            }
            scope.lex.put(name, Boolean.FALSE);
            break;
        default:
            break;
        }
    }

    /**
     * Parses a binding identifier or a binding pattern.
     * <p>
     * 바인딩 식별자나 바인딩 패턴을 파싱한다.
     *
     * @param kind the BIND_ kind that declares the names / 이름을 선언하는 BIND_ 종류
     * @return the IDENT or pattern node / IDENT 또는 패턴 노드
     */
    protected final Node parseBindingTarget(int kind) {
        if (isP("[")) {
            return parseArrayBindingPattern(kind);
        }
        if (isP("{")) {
            return parseObjectBindingPattern(kind);
        }
        if (tok.type != TokenType.NAME) {
            throw unexpected(tok);
        }
        Token t = tok;
        checkBindingName(t);
        next();
        declareBinding(t.value, kind, t);
        Node n = mk(NodeType.IDENT, t);
        n.name = t.value;
        return done(n);
    }

    /**
     * Parses a binding target with an optional default value.
     * <p>
     * 기본값이 있을 수 있는 바인딩 대상을 파싱한다.
     *
     * @param kind the BIND_ kind / BIND_ 종류
     * @return the node / 노드
     */
    protected final Node parseBindingElement(int kind) {
        Token start = tok;
        Node target = parseBindingTarget(kind);
        if (isP("=")) {
            next();
            Node d = mk(NodeType.DEFAULT, start);
            d.a = target;
            d.b = parseAssignment(false);
            return done(d);
        }
        return target;
    }

    /**
     * Parses an array binding pattern.
     * <p>
     * 배열 바인딩 패턴을 파싱한다.
     *
     * @param kind the BIND_ kind / BIND_ 종류
     * @return the ARRAY_PATTERN node / ARRAY_PATTERN 노드
     */
    private Node parseArrayBindingPattern(int kind) {
        Token start = tok;
        next();
        List<Node> elems = new ArrayList<Node>();
        // 닫는 대괄호까지 요소를 읽는다. 쉼표만 있으면 건너뜀(구멍)이다.
        while (!isP("]")) {
            if (isP(",")) {
                next();
                elems.add(null);
                continue;
            }
            if (isP("...")) {
                Token s = tok;
                next();
                Node r = mk(NodeType.REST, s);
                r.a = parseBindingTarget(kind);
                elems.add(done(r));
                if (!isP("]")) {
                    throw error("Rest element must be last element");
                }
                break;
            }
            elems.add(parseBindingElement(kind));
            if (!isP("]")) {
                expectP(",");
            }
        }
        expectP("]");
        Node n = mk(NodeType.ARRAY_PATTERN, start);
        n.list = elems.toArray(new Node[elems.size()]);
        return done(n);
    }

    /**
     * Parses an object binding pattern.
     * <p>
     * 객체 바인딩 패턴을 파싱한다.
     *
     * @param kind the BIND_ kind / BIND_ 종류
     * @return the OBJECT_PATTERN node / OBJECT_PATTERN 노드
     */
    private Node parseObjectBindingPattern(int kind) {
        Token start = tok;
        next();
        List<Node> props = new ArrayList<Node>();
        // 닫는 중괄호까지 프로퍼티를 읽는다.
        while (!isP("}")) {
            if (isP("...")) {
                Token restToken = tok;
                next();
                Node rest = mk(NodeType.REST, restToken);
                if (tok.type != TokenType.NAME) {
                    throw unexpected(tok);
                }
                rest.a = parseBindingTarget(kind);
                props.add(done(rest));
                if (!isP("}")) {
                    throw error("Rest property must be last in an object pattern", restToken);
                }
                break;
            }
            Token pt = tok;
            Node pp = mk(NodeType.PATTERN_PROP, pt);
            Token keyTok = tok;
            pp.a = parsePropertyName();
            pp.flag = keyComputed;
            if (isP(":")) {
                next();
                pp.b = parseBindingElement(kind);
            } else {
                if (keyTok.type != TokenType.NAME || pp.flag) {
                    throw unexpected(tok);
                }
                checkBindingName(keyTok);
                declareBinding(keyTok.value, kind, keyTok);
                Node id = mk(NodeType.IDENT, keyTok);
                id.name = keyTok.value;
                id.end = keyTok.end;
                if (isP("=")) {
                    next();
                    Node d = mk(NodeType.DEFAULT, keyTok);
                    d.a = id;
                    d.b = parseAssignment(false);
                    pp.b = done(d);
                } else {
                    pp.b = id;
                }
            }
            props.add(done(pp));
            if (!isP("}")) {
                expectP(",");
            }
        }
        expectP("}");
        Node n = mk(NodeType.OBJECT_PATTERN, start);
        n.list = props.toArray(new Node[props.size()]);
        return done(n);
    }

    // ------------------------------------------------------------------ functions

    /**
     * Creates the context of a new (non-arrow) function.
     * <p>
     * 새(비 화살표) 함수의 문맥을 만든다.
     *
     * @param generator true for generators / 제너레이터이면 true
     * @return the context / 문맥
     */
    private FuncCtx newFunctionCtx(boolean generator, boolean async) {
        FuncCtx c = new FuncCtx();
        c.parent = fc;
        c.strict = fc.strict;
        c.generator = generator;
        c.async = async;
        c.inFunction = true;
        c.allowNewTarget = true;
        return c;
    }

    /**
     * Parses a function expression.
     * <p>
     * 함수 표현식을 파싱한다.
     *
     * @return the FUNCTION node / FUNCTION 노드
     */
    protected final Node parseFunctionExpression() {
        return parseFunctionExpression(false);
    }

    /**
     * Parses a normal or async function expression.
     * <p>
     * 일반 또는 async 함수 표현식을 파싱한다.
     *
     * @param async true for an AsyncFunctionExpression / AsyncFunctionExpression 이면 true
     * @return the FUNCTION node / FUNCTION 노드
     */
    private Node parseFunctionExpression(boolean async) {
        Token start = tok;
        if (async) {
            next();
            if (!isN("function")) {
                throw unexpected(tok);
            }
        }
        next();
        boolean gen = eatP("*");
        FuncCtx ctx = newFunctionCtx(gen, async);
        FunctionInfo info = new FunctionInfo();
        info.generator = gen;
        info.async = async;
        Token nameTok = null;
        if (tok.type == TokenType.NAME) {
            nameTok = tok;
            // 함수 표현식의 이름은 함수 자신의 문맥(제너레이터 여부)에서 검사한다.
            FuncCtx saved = fc;
            fc = ctx;
            checkBindingName(nameTok);
            fc = saved;
            info.name = nameTok.value;
            next();
        }
        return parseFunctionTail(start, info, ctx, nameTok);
    }

    /**
     * Parses the parameters and body of a function whose header has been read, and builds the FUNCTION node.
     * <p>
     * 머리부를 읽은 함수의 매개변수와 본문을 파싱하고 FUNCTION 노드를 만든다.
     *
     * @param start   the first token of the function / 함수의 첫 토큰
     * @param info    the function description / 함수 설명
     * @param ctx     the function context / 함수 문맥
     * @param nameTok the name token or null / 이름 토큰 또는 null
     * @return the FUNCTION node / FUNCTION 노드
     */
    protected final Node parseFunctionTail(Token start, FunctionInfo info, FuncCtx ctx, Token nameTok) {
        FuncCtx savedFc = fc;
        Scope savedScope = scope;
        boolean wasStrict = ctx.strict;
        fc = ctx;
        Scope fs = new Scope(savedScope, SCOPE_FUNCTION);
        scope = fs;
        ctx.funcScope = fs;
        initInfo(info);
        info.start = start.start;
        // 접근자 종류는 이 함수에서만 쓰이도록 읽자마자 지운다(안쪽 함수가 영향받지 않게).
        int ak = accessorKind;
        accessorKind = 0;
        ctx.inParams = true;
        parseFormalParameters(info);
        ctx.inParams = false;
        checkAccessorParams(info, ak);
        parseFunctionBody(info, ctx);
        finishFunction(info, ctx, fs, nameTok, wasStrict);
        fc = savedFc;
        scope = savedScope;
        Node n = mk(NodeType.FUNCTION, start);
        n.fn = info;
        n.end = prev.end;
        info.end = prev.end;
        return n;
    }

    /**
     * Validates the accessor parameter counts (getters have none, setters exactly one without a rest element).
     * <p>
     * 접근자의 매개변수 개수를 검증한다(getter 는 없음, setter 는 정확히 하나이며 나머지 요소는 불가).
     *
     * @param info the function description / 함수 설명
     * @param kind 0 for none, 1 for getters, 2 for setters / 0 없음, 1 getter, 2 setter
     */
    private void checkAccessorParams(FunctionInfo info, int kind) {
        if (kind == 1 && info.params.length != 0) {
            throw error("Getter must not have any formal parameters.", prev);
        }
        if (kind == 2 && (info.params.length != 1 || info.params[0].type == NodeType.REST)) {
            throw error("Setter must have exactly one formal parameter.", prev);
        }
    }

    /**
     * Accessor kind of the function being parsed: 0 none, 1 getter, 2 setter (set by {@link #parseMethodFunction}).
     * <p>
     * 파싱 중인 함수의 접근자 종류: 0 없음, 1 getter, 2 setter({@link #parseMethodFunction} 이 설정).
     */
    private int accessorKind;

    /**
     * Parses a formal parameter list in parentheses.
     * <p>
     * 괄호로 둘러싼 형식 매개변수 목록을 파싱한다.
     *
     * @param info the function description to fill / 채울 함수 설명
     */
    protected final void parseFormalParameters(FunctionInfo info) {
        expectP("(");
        List<Node> params = new ArrayList<Node>();
        // 닫는 괄호까지 매개변수를 읽는다. 나머지 매개변수는 마지막이어야 한다.
        while (!isP(")")) {
            if (isP("...")) {
                Token s = tok;
                next();
                Node r = mk(NodeType.REST, s);
                r.a = parseBindingTarget(BIND_PARAM);
                params.add(done(r));
                if (!isP(")")) {
                    throw error("Rest parameter must be last formal parameter");
                }
                break;
            }
            params.add(parseBindingElement(BIND_PARAM));
            if (!isP(")")) {
                expectP(",");
            }
        }
        expectP(")");
        setParams(info, params);
    }

    /**
     * Stores parameters in the function description and derives simple/length/expression information.
     * <p>
     * 매개변수를 함수 설명에 저장하고 단순 여부, length, 표현식 포함 여부를 계산한다.
     *
     * @param info   the function description / 함수 설명
     * @param params the parameters / 매개변수들
     */
    protected final void setParams(FunctionInfo info, List<Node> params) {
        info.params = params.toArray(new Node[params.size()]);
        info.simpleParams = true;
        info.hasParamExpressions = false;
        boolean counting = true;
        int length = 0;
        // 모든 매개변수를 훑으며 단순 여부와 length 를 계산한다.
        for (int i = 0; i < info.params.length; i++) {
            Node p = info.params[i];
            if (p.type != NodeType.IDENT) {
                info.simpleParams = false;
            }
            if (p.type == NodeType.DEFAULT || p.type == NodeType.REST) {
                counting = false;
            }
            if (counting) {
                length++;
            }
            if (hasExpression(p)) {
                info.hasParamExpressions = true;
            }
        }
        info.length = length;
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < info.params.length; i++) { // 매개변수를 하나씩 검사한다.
            boundNames(info.params[i], names);
        }
        info.paramNames = names.toArray(new String[names.size()]);
    }

    /**
     * Tests whether a parameter pattern contains expressions that must be evaluated (defaults, computed keys).
     * <p>
     * 매개변수 패턴에 평가해야 하는 식(기본값, 계산된 키)이 있는지 검사한다.
     *
     * @param n the parameter / 매개변수
     * @return true if it has expressions / 식이 있으면 true
     */
    private boolean hasExpression(Node n) {
        if (n == null) {
            return false;
        }
        switch (n.type) {
        case NodeType.DEFAULT:
            return true;
        case NodeType.REST:
            return hasExpression(n.a);
        case NodeType.ARRAY_PATTERN:
            for (int i = 0; i < n.list.length; i++) { // 배열 패턴의 요소를 검사한다.
                if (hasExpression(n.list[i])) {
                    return true;
                }
            }
            return false;
        case NodeType.OBJECT_PATTERN:
            for (int i = 0; i < n.list.length; i++) { // 객체 패턴의 프로퍼티를 검사한다.
                if (n.list[i].flag || hasExpression(n.list[i].b)) {
                    return true;
                }
            }
            return false;
        default:
            return false;
        }
    }

    /**
     * Completes a function: strict mode checks that depend on the body, duplicate parameters, hoisting and scope description.
     * <p>
     * 함수를 마무리한다: 본문에 따라 달라지는 엄격 모드 검사, 중복 매개변수, 끌어올림, 스코프 설명.
     *
     * @param info      the function description / 함수 설명
     * @param ctx       the function context / 함수 문맥
     * @param fs        the function scope / 함수 스코프
     * @param nameTok   the function name token or null / 함수 이름 토큰 또는 null
     * @param wasStrict true if the context was already strict before the body / 본문 전에 이미 엄격했으면 true
     */
    protected final void finishFunction(FunctionInfo info, FuncCtx ctx, Scope fs, Token nameTok, boolean wasStrict) {
        info.strict = ctx.strict;
        info.usesArguments = ctx.usesArguments;
        info.hasDirectEval = ctx.hasEval;
        boolean noDup = ctx.strict || info.arrow || info.method || !info.simpleParams;
        if (noDup) {
            // 엄격 모드, 화살표, 메서드, 비단순 매개변수에서는 중복 이름이 오류이다.
            for (int i = 0; i < info.paramNames.length; i++) {
                for (int k = 0; k < i; k++) {
                    if (info.paramNames[i].equals(info.paramNames[k])) {
                        throw error("Duplicate parameter name not allowed in this context", prev);
                    }
                }
            }
        }
        if (ctx.strict && !wasStrict) {
            // 본문의 "use strict" 때문에 엄격해졌다면 이름과 매개변수를 다시 검사한다.
            if (nameTok != null) {
                recheckStrictName(nameTok.value, nameTok);
            }
            for (int i = 0; i < info.paramNames.length; i++) { // 매개변수 이름을 하나씩 검사한다.
                recheckStrictName(info.paramNames[i], prev);
            }
        }
        resolveCandidates(fs);
        info.scope = toInfo(fs);
        info.scope.hasDirectEval = ctx.hasEval;
    }

    /**
     * Checks a name against the strict mode binding rules.
     * <p>
     * 이름을 엄격 모드 바인딩 규칙에 대해 검사한다.
     *
     * @param name the name / 이름
     * @param t    the token for errors / 오류 위치 토큰
     */
    private void recheckStrictName(String name, Token t) {
        if (isStrictReserved(name) || name.equals("eval") || name.equals("arguments")) {
            throw error("Unexpected eval or arguments in strict mode", t);
        }
    }

    /**
     * Parses a method (object literal method, accessor, generator method or class method) from the opening parenthesis.
     * <p>
     * 메서드(객체 리터럴 메서드, 접근자, 제너레이터 메서드, 클래스 메서드)를 여는 괄호부터 파싱한다.
     *
     * @param start      the first token of the member / 멤버의 첫 토큰
     * @param member     the PROPERTY or CLASS_MEMBER node (its key a is already set) / PROPERTY 또는 CLASS_MEMBER 노드(키 a 는 이미 설정됨)
     * @param generator  true for generator methods / 제너레이터 메서드이면 true
     * @param getter     true for getters / getter 이면 true
     * @param setter     true for setters / setter 이면 true
     * @param classCtor  true for the class constructor / 클래스 생성자이면 true
     * @param derived    true if the class has an extends clause / 클래스에 extends 가 있으면 true
     * @return the FUNCTION node / FUNCTION 노드
     */
    protected final Node parseMethodFunction(Token start, Node member, boolean generator, boolean getter, boolean setter,
            boolean classCtor, boolean derived, boolean async) {
        FuncCtx ctx = newFunctionCtx(generator, async);
        ctx.allowSuperProp = true;
        ctx.allowSuperCall = classCtor && derived;
        FunctionInfo info = new FunctionInfo();
        info.generator = generator;
        info.async = async;
        info.method = true;
        info.classConstructor = classCtor;
        info.derived = classCtor && derived;
        if (member.a != null && member.a.type == NodeType.LITERAL && !member.flag) {
            info.name = member.a.name == null ? "" : member.a.name;
        } else if (member.a != null && member.a.type == NodeType.PRIVATE_NAME && !member.flag) {
            info.name = member.a.name;
        }
        int savedKind = accessorKind;
        accessorKind = getter ? 1 : (setter ? 2 : 0);
        Node f = parseFunctionTail(start, info, ctx, null);
        accessorKind = savedKind;
        return f;
    }

    // ------------------------------------------------------------------ arrow functions

    /**
     * Parses an arrow function whose only parameter is an identifier (<code>x =&gt; ...</code>).
     * <p>
     * 매개변수가 식별자 하나뿐인 화살표 함수(<code>x =&gt; ...</code>)를 파싱한다.
     *
     * @param noIn true to forbid in in a concise body / 간결한 본문에서 in 을 금지하려면 true
     * @return the FUNCTION node / FUNCTION 노드
     */
    private Node parseIdentifierArrow(boolean noIn) {
        return parseIdentifierArrow(noIn, false);
    }

    /**
     * Parses an identifier arrow or its async form.
     * <p>
     * 식별자 화살표나 그 async 형태를 파싱한다.
     *
     * @param noIn true to forbid in / in 을 금지하려면 true
     * @param async true for an async arrow / async 화살표이면 true
     * @return the FUNCTION node / FUNCTION 노드
     */
    private Node parseIdentifierArrow(boolean noIn, boolean async) {
        Token start = tok;
        FuncCtx savedFc = fc;
        Scope savedScope = scope;
        FuncCtx ctx = newArrowCtx();
        ctx.async = async;
        fc = ctx;
        Scope fs = new Scope(savedScope, SCOPE_FUNCTION);
        scope = fs;
        ctx.funcScope = fs;
        FunctionInfo info = new FunctionInfo();
        info.arrow = true;
        info.async = async;
        initInfo(info);
        info.start = start.start;
        Token paramToken = start;
        if (async) {
            next();
            paramToken = tok;
            if (paramToken.type != TokenType.NAME || isReservedWord(paramToken.value) || paramToken.newlineBefore) {
                throw unexpected(paramToken);
            }
            checkBindingName(paramToken);
        } else {
            checkBindingName(start);
        }
        List<Node> params = new ArrayList<Node>();
        Node id = mk(NodeType.IDENT, paramToken);
        id.name = paramToken.value;
        id.end = paramToken.end;
        params.add(id);
        fs.params.add(paramToken.value);
        next();
        setParams(info, params);
        return finishArrow(start, info, ctx, fs, savedFc, savedScope, noIn);
    }

    /**
     * Parses the async identifier-arrow entry form.
     * <p>
     * async 식별자 화살표 진입 형태를 파싱한다.
     */
    private Node parseAsyncIdentifierArrow(boolean noIn) {
        return parseIdentifierArrow(noIn, true);
    }

    /**
     * Creates the context of an arrow function: it shares this, arguments, super and new.target with the enclosing function.
     * <p>
     * 화살표 함수의 문맥을 만든다. this, arguments, super, new.target 을 바깥 함수와 공유한다.
     *
     * @return the context / 문맥
     */
    private FuncCtx newArrowCtx() {
        FuncCtx c = new FuncCtx();
        c.parent = fc;
        c.strict = fc.strict;
        c.arrow = true;
        c.inFunction = true;
        c.allowNewTarget = fc.allowNewTarget;
        c.allowSuperProp = fc.allowSuperProp;
        c.allowSuperCall = fc.allowSuperCall;
        c.classFieldInitializer = fc.classFieldInitializer;
        return c;
    }

    /**
     * Tries to parse a parenthesized arrow function; returns null (restoring the parser state) if the parentheses turn out
     * to be an ordinary parenthesized expression.
     * <p>
     * 괄호로 시작하는 화살표 함수를 파싱해 본다. 일반 괄호 식으로 밝혀지면 파서 상태를 되돌리고 null 을 반환한다.
     *
     * @param noIn true to forbid in in a concise body / 간결한 본문에서 in 을 금지하려면 true
     * @return the FUNCTION node or null / FUNCTION 노드 또는 null
     */
    private Node tryParseArrow(boolean noIn) {
        Token n = peek();
        boolean plausible = n.isPunct(")") || n.isPunct("...") || n.isPunct("[") || n.isPunct("{") || n.type == TokenType.NAME;
        if (!plausible) {
            return null;
        }
        Token start = tok;
        Lexer.State ls = lexer.saveState();
        Token savedTok = tok;
        Token savedPrev = prev;
        FuncCtx savedFc = fc;
        Scope savedScope = scope;
        Token savedCover = coverError;
        String savedMsg = coverMessage;
        FuncCtx ctx = newArrowCtx();
        fc = ctx;
        Scope fs = new Scope(savedScope, SCOPE_FUNCTION);
        scope = fs;
        ctx.funcScope = fs;
        FunctionInfo info = new FunctionInfo();
        info.arrow = true;
        initInfo(info);
        info.start = start.start;
        boolean committed = false;
        try {
            ctx.inParams = true;
            parseFormalParameters(info);
            ctx.inParams = false;
            if (isP("=>") && !tok.newlineBefore) {
                committed = true;
            }
        } catch (JsException e) {
            committed = false;
        }
        if (!committed) {
            lexer.restoreState(ls);
            tok = savedTok;
            prev = savedPrev;
            fc = savedFc;
            scope = savedScope;
            coverError = savedCover;
            coverMessage = savedMsg;
            return null;
        }
        return finishArrow(start, info, ctx, fs, savedFc, savedScope, noIn);
    }

    /**
     * Tries to parse an async parenthesized arrow function.
     * <p>
     * 괄호 매개변수가 있는 async 화살표 함수 파싱을 시도한다.
     *
     * @param noIn true to forbid in / in 을 금지하려면 true
     * @return the FUNCTION node or null / FUNCTION 노드 또는 null
     */
    private Node tryParseAsyncArrow(boolean noIn) {
        Token start = tok;
        Token afterAsync = peek();
        if (afterAsync.newlineBefore || !afterAsync.isPunct("(")) {
            return null;
        }
        Lexer.State ls = lexer.saveState();
        Token savedTok = tok;
        Token savedPrev = prev;
        FuncCtx savedFc = fc;
        Scope savedScope = scope;
        Token savedCover = coverError;
        String savedMsg = coverMessage;
        FuncCtx ctx = newArrowCtx();
        ctx.async = true;
        fc = ctx;
        Scope fs = new Scope(savedScope, SCOPE_FUNCTION);
        scope = fs;
        ctx.funcScope = fs;
        FunctionInfo info = new FunctionInfo();
        info.arrow = true;
        info.async = true;
        initInfo(info);
        info.start = start.start;
        boolean committed = false;
        try {
            next();
            ctx.inParams = true;
            parseFormalParameters(info);
            ctx.inParams = false;
            if (isP("=>") && !tok.newlineBefore) {
                committed = true;
            }
        } catch (JsException e) {
            committed = false;
        }
        if (!committed) {
            lexer.restoreState(ls);
            tok = savedTok;
            prev = savedPrev;
            fc = savedFc;
            scope = savedScope;
            coverError = savedCover;
            coverMessage = savedMsg;
            return null;
        }
        return finishArrow(start, info, ctx, fs, savedFc, savedScope, noIn);
    }

    /**
     * Parses the arrow and the body of an arrow function after its parameters.
     * <p>
     * 화살표 함수의 매개변수 뒤에서 화살표와 본문을 파싱한다.
     *
     * @param start      the first token / 첫 토큰
     * @param info       the function description / 함수 설명
     * @param ctx        the arrow context / 화살표 문맥
     * @param fs         the function scope / 함수 스코프
     * @param savedFc    the enclosing context to restore / 복원할 바깥 문맥
     * @param savedScope the enclosing scope to restore / 복원할 바깥 스코프
     * @param noIn       true to forbid in in a concise body / 간결한 본문에서 in 을 금지하려면 true
     * @return the FUNCTION node / FUNCTION 노드
     */
    private Node finishArrow(Token start, FunctionInfo info, FuncCtx ctx, Scope fs, FuncCtx savedFc, Scope savedScope, boolean noIn) {
        if (!isP("=>") || tok.newlineBefore) {
            throw unexpected(tok);
        }
        boolean wasStrict = ctx.strict;
        next();
        if (isP("{")) {
            parseFunctionBody(info, ctx);
        } else {
            info.exprBody = true;
            Node body = parseAssignment(noIn);
            info.body = body;
        }
        finishFunction(info, ctx, fs, null, wasStrict);
        fc = savedFc;
        scope = savedScope;
        if (ctx.usesArguments) {
            markArguments();
        }
        Node n = mk(NodeType.FUNCTION, start);
        n.fn = info;
        n.end = prev.end;
        info.end = prev.end;
        return n;
    }

    // ------------------------------------------------------------------ classes

    /**
     * Parses a class expression or declaration.
     * <p>
     * 클래스 표현식 또는 선언을 파싱한다.
     *
     * @param declaration    true for declarations (a name is required unless allowed) / 선언이면 true(허용되지 않으면 이름 필수)
     * @param allowAnonymous true if an anonymous class declaration is allowed (export default) / 익명 클래스 선언을 허용하면 true(export default)
     * @return the CLASS node / CLASS 노드
     */
    protected final Node parseClass(boolean declaration, boolean allowAnonymous) {
        Token start = tok;
        next();
        boolean savedStrict = fc.strict;
        fc.strict = true;
        Node cls = mk(NodeType.CLASS, start);
        if (tok.type == TokenType.NAME && !isN("extends")) {
            checkBindingName(tok);
            cls.name = tok.value;
            next();
        } else if (declaration && !allowAnonymous) {
            throw unexpected(tok);
        }
        if (isN("extends")) {
            next();
            cls.a = parseLeftHandSide();
        }
        expectP("{");
        pushPrivateEnvironment();
        List<Node> members = new ArrayList<Node>();
        // 닫는 중괄호까지 클래스 멤버를 읽는다.
        while (!isP("}")) {
            if (eatP(";")) {
                continue;
            }
            Node m = isN("static") && peek().isPunct("{")
                    ? parseClassStaticBlock()
                    : parseClassMember(cls.a != null);
            if (m.value == Boolean.FALSE) {
                if (cls.b != null) {
                    throw error("A class may only have one constructor", m);
                }
                cls.b = m.b;
            } else {
                members.add(m);
            }
        }
        expectP("}");
        popPrivateEnvironment();
        fc.strict = savedStrict;
        cls.list = members.toArray(new Node[members.size()]);
        cls.value = source;
        return done(cls);
    }

    /**
     * Parses one class member (a method, accessor or generator method, possibly static).
     * The value field of the result is {@link Boolean#FALSE} for the constructor and null otherwise.
     * <p>
     * 클래스 멤버 하나(메서드, 접근자, 제너레이터 메서드, static 가능)를 파싱한다. 결과의 value 필드는 생성자이면
     * {@link Boolean#FALSE}, 아니면 null 이다.
     *
     * @param derived true if the class has an extends clause / 클래스에 extends 가 있으면 true
     * @return the CLASS_MEMBER node / CLASS_MEMBER 노드
     */
    private Node parseClassMember(boolean derived) {
        Token start = tok;
        Node m = mk(NodeType.CLASS_MEMBER, start);
        boolean isStatic = false;
        if (isN("static") && !peek().isPunct("(") && !peek().isPunct("=") && !peek().isPunct(";")
                && !peek().isPunct("}")) {
            isStatic = true;
            next();
        }
        m.flag = isStatic;
        boolean async = false;
        if (isN("async") && !peek().newlineBefore && !peek().isPunct("(")
                && (peek().isPunct("*") || startsPropertyName(peek()))) {
            async = true;
            next();
        }
        boolean gen = false;
        boolean getter = false;
        boolean setter = false;
        if (isP("*")) {
            gen = true;
            next();
        } else if (tok.type == TokenType.NAME && !tok.escaped && (tok.value.equals("get") || tok.value.equals("set"))
                && startsPropertyName(peek())) {
            getter = tok.value.equals("get");
            setter = !getter;
            next();
        }
        Token keyToken = tok;
        m.a = parseClassPropertyName();
        boolean computed = keyComputed;
        m.privateName = keyToken.type == TokenType.PRIVATE_NAME;
        m.op = getter ? Op.PROP_GET : (setter ? Op.PROP_SET : Op.PROP_METHOD);
        boolean ctor = false;
        if (!computed && m.a.name != null) {
            if (!m.privateName && !isStatic && m.a.name.equals("constructor")) {
                if (getter || setter || gen || async) {
                    throw error("Class constructor may not be an accessor, generator or async method", m.a);
                }
                ctor = true;
            } else if (!m.privateName && isStatic && m.a.name.equals("prototype")) {
                throw error("Classes may not have a static property named 'prototype'", m.a);
            }
        }
        if (computed) {
            m.value = Boolean.TRUE;
        }
        int privateKind = getter ? PRIVATE_GETTER : (setter ? PRIVATE_SETTER : PRIVATE_METHOD);
        if (m.privateName) {
            declarePrivate(keyToken, isStatic, isP("(") ? privateKind : PRIVATE_FIELD);
        }
        if (isP("(")) {
            // 계산된 키는 FunctionInfo 이름을 정하지 않도록 member.flag 대신 value 로 표시한다.
            Node keyHolder = new Node(NodeType.PROPERTY, m.line, m.column);
            keyHolder.a = m.a;
            keyHolder.flag = computed;
            m.b = parseMethodFunction(start, keyHolder, gen, getter, setter, ctor, derived, async);
            if (ctor) {
                m.value = Boolean.FALSE;
            }
            return done(m);
        }
        if (gen || getter || setter || async) {
            throw error("Expected a method parameter list", tok);
        }
        if (ctor) {
            throw error("A class field cannot be named 'constructor'", m.a);
        }
        m.op = Op.PROP_FIELD;
        if (isP("=")) {
            next();
            m.b = parseClassFieldInitializer();
        }
        consumeSemicolon();
        return done(m);
    }

    /**
     * Parses a class property name, including a private name.
     * <p>
     * private 이름을 포함해 클래스 프로퍼티 이름을 파싱한다.
     *
     * @return the key node / 키 노드
     */
    private Node parseClassPropertyName() {
        keyComputed = false;
        if (tok.type != TokenType.PRIVATE_NAME) {
            return parsePropertyName();
        }
        Token t = tok;
        next();
        Node n = mk(NodeType.PRIVATE_NAME, t);
        n.name = "#" + t.value;
        n.privateName = true;
        return done(n);
    }

    /**
     * Parses a class field initializer in its isolated evaluation context.
     * <p>
     * 클래스 필드 초기화 식을 분리된 평가 문맥에서 파싱한다.
     *
     * @return initializer expression / 초기화 식
     */
    private Node parseClassFieldInitializer() {
        FuncCtx saved = fc;
        FuncCtx field = new FuncCtx();
        field.parent = saved;
        field.strict = true;
        field.classFieldInitializer = true;
        field.allowSuperProp = true;
        fc = field;
        Node value = parseAssignment(false);
        fc = saved;
        return value;
    }

    /**
     * Parses a class static initialization block.
     * <p>
     * 클래스 static 초기화 블록을 파싱한다.
     *
     * @return the CLASS_STATIC_BLOCK node / CLASS_STATIC_BLOCK 노드
     */
    private Node parseClassStaticBlock() {
        Token start = tok;
        next();
        Token open = tok;
        expectP("{");
        FuncCtx savedFc = fc;
        Scope savedScope = scope;
        FuncCtx blockContext = new FuncCtx();
        blockContext.parent = savedFc;
        blockContext.strict = true;
        blockContext.classStaticBlock = true;
        blockContext.allowSuperProp = true;
        fc = blockContext;
        Scope blockScope = new Scope(savedScope, SCOPE_FUNCTION);
        scope = blockScope;
        blockContext.funcScope = blockScope;
        List<Node> statements = new ArrayList<Node>();
        // 닫는 중괄호까지 static 블록 문장을 바깥 함수의 return 권한과 스코프를 물려받지 않고 읽는다.
        while (!isP("}")) {
            if (tok.type == TokenType.EOF) {
                throw unexpected(tok);
            }
            statements.add(parseStatementListItem());
        }
        expectP("}");
        fc = savedFc;
        scope = savedScope;
        Node block = mk(NodeType.CLASS_STATIC_BLOCK, start);
        block.start = open.start;
        block.list = statements.toArray(new Node[statements.size()]);
        block.scope = toInfo(blockScope);
        return done(block);
    }
}
