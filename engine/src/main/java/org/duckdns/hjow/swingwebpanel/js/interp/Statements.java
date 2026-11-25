package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.AsyncIteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.NodeType;
import org.duckdns.hjow.swingwebpanel.js.ast.Op;
import org.duckdns.hjow.swingwebpanel.js.ast.ScopeInfo;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.EnumerateSupport.ForInIterator;

/**
 * Executes statements (ECMA-262 6th edition clauses 13 and 14) and produces explicit {@link Completion} records: normal, break,
 * continue and return completions flow through blocks, loops, labels, switch and try/finally exactly as specified; throw
 * completions travel as {@link JsException}.
 * <p>
 * Like expressions, statements that contain a yield keep a {@link NodeState} so that a generator can resume inside them:
 * statement lists remember the index, loops remember the phase (test, body, update), and try statements remember whether the
 * try block, the catch block or the finally block was running together with the pending completion.
 * <p>
 * 문장을 실행하고(ECMA-262 6판 13, 14장) 명시적인 {@link Completion} 레코드를 만든다: normal, break, continue, return 완료가 블록,
 * 반복문, 레이블, switch, try/finally 를 규격대로 흐르며 throw 완료는 {@link JsException} 으로 전달된다.
 * <p>
 * 표현식과 마찬가지로 yield 를 포함한 문장은 {@link NodeState} 를 유지하여 제너레이터가 그 안에서 재개될 수 있다: 문장 목록은
 * 인덱스를, 반복문은 단계(조건, 본문, 갱신)를, try 문은 try/catch/finally 중 어디를 실행 중이었는지와 보류 중인 완료를 기억한다.
 */
final class Statements {

    /** Phase evaluating a for-in source after its legacy initializer. / 레거시 초기화식 이후 for-in의 원본을 평가하는 단계. */
    private static final int FOR_IN_AFTER_INITIALIZER = 3;

    /** Phase evaluating a legacy call target for a fetched loop value. / 꺼낸 반복 값의 레거시 호출 대상을 평가하는 단계. */
    private static final int FOR_CALL_TARGET = 4;

    /**
     * The interpreter.
     * <p>
     * 인터프리터.
     */
    private final Interpreter in;

    /**
     * The realm that owns this component.
     * <p>
     * 이 구성 요소를 소유한 realm.
     */
    private final JsRealm realm;

    /**
     * Creates the executor.
     * <p>
     * 실행기를 만든다.
     *
     * @param in the interpreter / 인터프리터
     */
    Statements(Interpreter in) {
        this.in = in;
        this.realm = in.realm;
    }

    // ------------------------------------------------------------------ dispatch

    /**
     * Executes a statement.
     * <p>
     * 문장을 실행한다.
     *
     * @param n   the node / 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @return the completion / 완료
     */
    Completion exec(Node n, Environment env, Ctx ctx) {
        return exec(n, env, ctx, null);
    }

    /**
     * Executes a statement that may be labelled by the given labels (needed by continue).
     * <p>
     * 주어진 레이블이 붙은 문장을 실행한다(continue 에 필요).
     *
     * @param n      the node / 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param labels the labels directly attached to the statement or null / 문장에 직접 붙은 레이블 또는 null
     * @return the completion / 완료
     */
    Completion exec(Node n, Environment env, Ctx ctx, List<String> labels) {
        in.limits.tick();
        if (n.hasYield) {
            GenState gen = ctx.gen;
            NodeState st = gen.enter(n);
            try {
                return execNode(n, env, ctx, st, labels);
            } finally {
                gen.leave(n);
            }
        }
        return execNode(n, env, ctx, null, labels);
    }

    /**
     * Executes a top-level statement list (script or module body) without generator state.
     * <p>
     * 제너레이터 상태 없이 최상위 문장 목록(스크립트나 모듈 본문)을 실행한다.
     *
     * @param list the statements / 문장들
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @return the completion / 완료
     */
    Completion execList(Node[] list, Environment env, Ctx ctx) {
        return runList(list, env, ctx, null);
    }

    /**
     * Evaluates a child expression, or replays its memoized value after a generator resumed.
     * <p>
     * 자식 표현식을 평가한다. 제너레이터가 재개되었으면 저장된 값을 재사용한다.
     *
     * @param st    the node state or null / 노드 상태 또는 null
     * @param child the expression / 표현식
     * @param env   the environment / 환경
     * @param ctx   the context / 문맥
     * @return the value / 값
     */
    private JsValue memo(NodeState st, Node child, Environment env, Ctx ctx) {
        if (st == null) {
            return in.ex.eval(child, env, ctx);
        }
        if (st.cursor < st.count) {
            return (JsValue) st.vals[st.cursor++];
        }
        JsValue v = in.ex.eval(child, env, ctx);
        st.add(v);
        st.cursor++;
        return v;
    }

    /**
     * Runs a statement list, tracking the completion value (UpdateEmpty).
     * <p>
     * 문장 목록을 실행하며 완료 값을 추적한다(UpdateEmpty).
     *
     * @param list the statements / 문장들
     * @param env  the environment / 환경
     * @param ctx  the context / 문맥
     * @param st   the node state or null / 노드 상태 또는 null
     * @return the completion / 완료
     */
    Completion runList(Node[] list, Environment env, Ctx ctx, NodeState st) {
        int start = 0;
        JsValue last = null;
        if (st != null) {
            start = st.idx;
            last = (JsValue) st.o2;
        }
        // 문장을 순서대로 실행한다. 중단되면 인덱스와 마지막 값이 상태에 남는다.
        for (int i = start; i < list.length; i++) {
            if (st != null) {
                st.idx = i;
                st.o2 = last;
            }
            Completion c = exec(list[i], env, ctx);
            if (c.value != null) {
                last = c.value;
            }
            if (st != null) {
                st.o2 = last;
            }
            if (c.type != Completion.NORMAL) {
                return Completion.updateEmpty(c, last);
            }
        }
        return Completion.normal(last);
    }

    // ------------------------------------------------------------------ main switch

    /**
     * Executes one statement node.
     * <p>
     * 문장 노드 하나를 실행한다.
     *
     * @param n      the node / 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @param labels the labels attached to this statement or null / 이 문장에 붙은 레이블 또는 null
     * @return the completion / 완료
     */
    private Completion execNode(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        switch (n.type) {
        case NodeType.EXPR_STMT:
            return Completion.normal(in.ex.eval(n.a, env, ctx));
        case NodeType.VAR_DECL:
            return execVarDecl(n, env, ctx, st);
        case NodeType.BLOCK:
            return execBlock(n, env, ctx, st);
        case NodeType.EMPTY:
        case NodeType.DEBUGGER:
            return Completion.EMPTY;
        case NodeType.IF: {
            JsValue t = memo(st, n.a, env, ctx);
            Node branch = Ops.toBoolean(t) ? n.b : n.c;
            Completion c = branch == null ? Completion.EMPTY : exec(branch, env, ctx);
            return Completion.updateEmpty(c, Js.UNDEFINED);
        }
        case NodeType.WHILE:
            return execWhile(n, env, ctx, st, labels);
        case NodeType.DO_WHILE:
            return execDoWhile(n, env, ctx, st, labels);
        case NodeType.FOR:
            return execFor(n, env, ctx, st, labels);
        case NodeType.FOR_IN:
        case NodeType.FOR_OF:
            // 반복자 정리가 필요하므로 이 반복문 안의 호출은 꼬리 호출이 아니다.
            ctx.noTail++;
            try {
                return n.type == NodeType.FOR_OF && n.flag
                        ? execForAwait(n, env, ctx, st, labels)
                        : execForInOf(n, env, ctx, st, labels);
            } finally {
                ctx.noTail--;
            }
        case NodeType.RETURN:
            if (n.a == null) {
                return new Completion(Completion.RETURN, Js.UNDEFINED, null);
            }
            // 엄격 모드 함수의 꼬리 위치 return 은 호출을 바로 실행하지 않고 호출자의 반복문(트램펄린)에 맡긴다.
            if (ctx.tailAllowed && ctx.noTail == 0) {
                return new Completion(Completion.RETURN, in.ex.evalTail(n.a, env, ctx), null);
            }
            return new Completion(Completion.RETURN, in.ex.eval(n.a, env, ctx), null);
        case NodeType.BREAK:
            return new Completion(Completion.BREAK, null, n.name);
        case NodeType.CONTINUE:
            return new Completion(Completion.CONTINUE, null, n.name);
        case NodeType.THROW: {
            JsException thrown = new JsException(in.ex.eval(n.a, env, ctx));
            // throw 문 자체의 위치를 던져진 예외의 위치로 기억한다.
            thrown.recordThrowSite(ctx.sourceName, n.line, n.column);
            throw thrown;
        }
        case NodeType.TRY:
            // try 안의 호출은 catch/finally 가 필요하므로 꼬리 호출이 아니다.
            ctx.noTail++;
            try {
                return execTry(n, env, ctx, st);
            } finally {
                ctx.noTail--;
            }
        case NodeType.SWITCH:
            return execSwitch(n, env, ctx, st);
        case NodeType.LABELED:
            return execLabeled(n, env, ctx, labels);
        case NodeType.WITH:
            return execWith(n, env, ctx, st);
        case NodeType.FUNCTION_DECL:
            return execFunctionDecl(n, env, ctx);
        case NodeType.CLASS_DECL: {
            JsValue cls = in.cls.evaluate(n.b, env, ctx, "*default*".equals(n.a.name) ? "default" : null);
            in.pat.bindName(n.a.name, cls, env, ctx, Patterns.INIT);
            return Completion.EMPTY;
        }
        case NodeType.IMPORT:
            return Completion.EMPTY;
        case NodeType.EXPORT:
            return execExport(n, env, ctx);
        default:
            throw JsException.syntaxError("Unsupported statement (node type " + n.type + ")", null, n.line, n.column);
        }
    }

    // ------------------------------------------------------------------ declarations and blocks

    /**
     * Executes a var, let or const statement.
     * <p>
     * var, let, const 문을 실행한다.
     *
     * @param n   the VAR_DECL node / VAR_DECL 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the empty completion / 빈 완료
     */
    private Completion execVarDecl(Node n, Environment env, Ctx ctx, NodeState st) {
        int start = st != null ? st.idx : 0;
        // 선언자를 차례로 처리한다. 초기값에서 중단되면 인덱스가 남는다.
        for (int i = start; i < n.list.length; i++) {
            if (st != null) {
                st.idx = i;
            }
            Node d = n.list[i];
            Node target = d.a;
            if (d.b == null) {
                if (n.op == Op.LET) {
                    in.pat.bind(target, Js.UNDEFINED, env, ctx, Patterns.INIT);
                }
                continue;
            }
            JsValue value;
            if (target.type == NodeType.IDENT && in.ex.isAnonymous(d.b)) {
                value = in.ex.evalNamed(d.b, target.name, env, ctx);
            } else {
                value = in.ex.eval(d.b, env, ctx);
            }
            in.pat.bind(target, value, env, ctx, n.op == Op.VAR ? Patterns.ASSIGN : Patterns.INIT);
        }
        return Completion.EMPTY;
    }

    /**
     * Executes a block with its own scope.
     * <p>
     * 자신의 스코프를 가진 블록을 실행한다.
     *
     * @param n   the BLOCK node / BLOCK 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the completion / 완료
     */
    private Completion execBlock(Node n, Environment env, Ctx ctx, NodeState st) {
        Environment benv = env;
        if (n.scope != null) {
            if (st != null && st.o1 != null) {
                benv = (Environment) st.o1;
            } else {
                benv = new Environment(env);
                in.declareLexicals(n.scope, benv);
                in.instantiateBlockFunctions(n.scope, benv);
                if (st != null) {
                    st.o1 = benv;
                }
            }
        }
        return runList(n.list, benv, ctx, st);
    }

    /**
     * Executes a function declaration statement: only the Annex B hoisting copy happens here (the function itself was created
     * when the scope was entered).
     * <p>
     * 함수 선언문을 실행한다: 여기서는 부속서 B 의 끌어올림 복사만 일어난다(함수 자체는 스코프에 들어갈 때 만들어졌다).
     *
     * @param n   the FUNCTION_DECL node / FUNCTION_DECL 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @return the empty completion / 빈 완료
     */
    private Completion execFunctionDecl(Node n, Environment env, Ctx ctx) {
        if (n.flag) {
            String name = n.a.name;
            Binding src = env.own(name);
            if (src != null && ctx.varEnv != env) {
                JsValue f = src.value;
                if (ctx.varEnv == in.globalEnv) {
                    realm.getGlobalObject().set(name, f);
                } else {
                    Binding dst = ctx.varEnv.own(name);
                    if (dst != null) {
                        dst.value = f;
                        dst.initialized = true;
                    }
                }
            }
        }
        return Completion.EMPTY;
    }

    /**
     * Executes an export statement (declarations and default expressions have runtime effects).
     * <p>
     * export 문을 실행한다(선언과 기본 식은 실행 시 효과가 있다).
     *
     * @param n   the EXPORT node / EXPORT 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @return the empty completion / 빈 완료
     */
    private Completion execExport(Node n, Environment env, Ctx ctx) {
        if (n.op == 0 && n.a != null) {
            exec(n.a, env, ctx);
        } else if (n.op == 1) {
            if (n.flag) {
                JsValue v = in.ex.isAnonymous(n.a) ? in.ex.evalNamed(n.a, "default", env, ctx) : in.ex.eval(n.a, env, ctx);
                in.pat.bindName("*default*", v, env, ctx, Patterns.INIT);
            } else {
                exec(n.a, env, ctx);
            }
        }
        return Completion.EMPTY;
    }

    // ------------------------------------------------------------------ loops

    /**
     * Decides what a loop does with the completion of its body.
     * <p>
     * 반복문이 본문의 완료를 어떻게 처리할지 정한다.
     *
     * @param c      the body completion / 본문 완료
     * @param labels the labels of the loop or null / 반복문의 레이블 또는 null
     * @return 0 to continue with the next iteration, 1 to leave the loop normally, 2 to propagate the completion / 0 이면 다음 반복, 1 이면 정상 종료, 2 이면 완료를 전파
     */
    private static int bodyAction(Completion c, List<String> labels) {
        if (c.type == Completion.NORMAL) {
            return 0;
        }
        if (c.type == Completion.BREAK && c.target == null) {
            return 1;
        }
        if (c.type == Completion.CONTINUE && (c.target == null || (labels != null && labels.contains(c.target)))) {
            return 0;
        }
        return 2;
    }

    /**
     * Executes a while loop.
     * <p>
     * while 반복문을 실행한다.
     *
     * @param n      the WHILE node / WHILE 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @param labels the loop's labels / 반복문의 레이블
     * @return the completion / 완료
     */
    private Completion execWhile(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        int phase = st != null ? st.phase : 0;
        JsValue v = st != null && st.o2 != null ? (JsValue) st.o2 : Js.UNDEFINED;
        // 조건과 본문을 번갈아 실행한다. phase 는 중단 위치(0 조건, 1 본문)를 가리킨다.
        while (true) {
            in.limits.tick();
            if (phase == 0) {
                JsValue t = in.ex.eval(n.a, env, ctx);
                if (!Ops.toBoolean(t)) {
                    return Completion.normal(v);
                }
                phase = 1;
                if (st != null) {
                    st.phase = 1;
                }
            }
            Completion c = exec(n.d, env, ctx);
            phase = 0;
            if (st != null) {
                st.phase = 0;
            }
            if (c.value != null) {
                v = c.value;
                if (st != null) {
                    st.o2 = v;
                }
            }
            int act = bodyAction(c, labels);
            if (act == 1) {
                return Completion.normal(v);
            }
            if (act == 2) {
                return Completion.updateEmpty(c, v);
            }
        }
    }

    /**
     * Executes a do-while loop.
     * <p>
     * do-while 반복문을 실행한다.
     *
     * @param n      the DO_WHILE node / DO_WHILE 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @param labels the loop's labels / 반복문의 레이블
     * @return the completion / 완료
     */
    private Completion execDoWhile(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        int phase = st != null ? st.phase : 0;
        JsValue v = st != null && st.o2 != null ? (JsValue) st.o2 : Js.UNDEFINED;
        // 본문과 조건을 번갈아 실행한다. phase 는 중단 위치(0 본문, 1 조건)를 가리킨다.
        while (true) {
            in.limits.tick();
            if (phase == 0) {
                Completion c = exec(n.d, env, ctx);
                if (c.value != null) {
                    v = c.value;
                    if (st != null) {
                        st.o2 = v;
                    }
                }
                int act = bodyAction(c, labels);
                if (act == 1) {
                    return Completion.normal(v);
                }
                if (act == 2) {
                    return Completion.updateEmpty(c, v);
                }
                phase = 1;
                if (st != null) {
                    st.phase = 1;
                }
            }
            JsValue t = in.ex.eval(n.a, env, ctx);
            phase = 0;
            if (st != null) {
                st.phase = 0;
            }
            if (!Ops.toBoolean(t)) {
                return Completion.normal(v);
            }
        }
    }

    /**
     * Creates the next per-iteration environment of a for loop with let declarations (CreatePerIterationEnvironment).
     * <p>
     * let 선언이 있는 for 반복문의 다음 반복용 환경을 만든다(CreatePerIterationEnvironment).
     *
     * @param last  the environment of the finished iteration / 끝난 반복의 환경
     * @param scope the loop scope description / 반복문 스코프 설명
     * @return the new environment with copies of the loop variables / 반복 변수의 복사본을 가진 새 환경
     */
    private Environment copyEnvironment(Environment last, ScopeInfo scope) {
        Environment e = new Environment(last.outer);
        // 반복 변수의 현재 값을 새 환경으로 복사한다. 클로저는 반복마다 다른 변수를 본다.
        for (int i = 0; i < scope.lexNames.length; i++) {
            Binding old = last.own(scope.lexNames[i]);
            e.declare(scope.lexNames[i], old.mutable, old.initialized, old.value);
        }
        return e;
    }

    /**
     * Executes a for(;;) loop.
     * <p>
     * for(;;) 반복문을 실행한다.
     *
     * @param n      the FOR node / FOR 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @param labels the loop's labels / 반복문의 레이블
     * @return the completion / 완료
     */
    private Completion execFor(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        Environment loopEnv;
        int phase;
        JsValue v = Js.UNDEFINED;
        if (st != null && st.o1 != null) {
            loopEnv = (Environment) st.o1;
            phase = st.phase;
            if (st.o2 != null) {
                v = (JsValue) st.o2;
            }
        } else {
            loopEnv = env;
            if (n.scope != null) {
                loopEnv = new Environment(env);
                in.declareLexicals(n.scope, loopEnv);
            }
            if (st != null) {
                st.o1 = loopEnv;
            }
            phase = 0;
        }
        if (phase == 0) {
            if (n.a != null) {
                if (n.a.type == NodeType.VAR_DECL) {
                    exec(n.a, loopEnv, ctx);
                } else {
                    in.ex.eval(n.a, loopEnv, ctx);
                }
            }
            if (n.scope != null) {
                loopEnv = copyEnvironment(loopEnv, n.scope);
                if (st != null) {
                    st.o1 = loopEnv;
                }
            }
            phase = 1;
            if (st != null) {
                st.phase = 1;
            }
        }
        // 조건(1), 본문(2), 갱신(3) 단계를 돌며 반복한다.
        while (true) {
            in.limits.tick();
            if (phase == 1) {
                if (n.b != null && !Ops.toBoolean(in.ex.eval(n.b, loopEnv, ctx))) {
                    return Completion.normal(v);
                }
                phase = 2;
                if (st != null) {
                    st.phase = 2;
                }
            }
            if (phase == 2) {
                Completion c = exec(n.d, loopEnv, ctx);
                if (c.value != null) {
                    v = c.value;
                    if (st != null) {
                        st.o2 = v;
                    }
                }
                int act = bodyAction(c, labels);
                if (act == 1) {
                    return Completion.normal(v);
                }
                if (act == 2) {
                    return Completion.updateEmpty(c, v);
                }
                if (n.scope != null) {
                    loopEnv = copyEnvironment(loopEnv, n.scope);
                    if (st != null) {
                        st.o1 = loopEnv;
                    }
                }
                phase = 3;
                if (st != null) {
                    st.phase = 3;
                }
            }
            if (phase == 3) {
                if (n.c != null) {
                    in.ex.eval(n.c, loopEnv, ctx);
                }
                phase = 1;
                if (st != null) {
                    st.phase = 1;
                }
            }
        }
    }

    /**
     * Evaluates the one-time var initializer in an Annex B.3.5 for-in head.
     * <p>
     * 부속서 B.3.5 for-in의 var 초기화식을 한 번 평가한다.
     *
     * @param declaration single var declarator / var 선언자 하나
     * @param env current environment / 현재 환경
     * @param ctx current context / 현재 문맥
     * @param st suspension state, if any / 중단 상태, 또는 null
     */
    private void initializeForInVariable(Node declaration, Environment env, Ctx ctx, NodeState st) {
        String name = declaration.a.name;
        Reference target = st == null ? null : (Reference) st.o1;
        if (target == null) {
            // 초기화식에서 바인딩을 변경하거나 중단해도 먼저 결정한 참조에 쓰도록 보존한다.
            target = Reference.identifier(in.ex, env, name, ctx.strict);
            if (st != null) {
                st.o1 = target;
            }
        }
        Node initializer = declaration.b;
        JsValue value = in.ex.isAnonymous(initializer) ? in.ex.evalNamed(initializer, name, env, ctx)
                : in.ex.eval(initializer, env, ctx);
        target.putValue(value);
    }

    /**
     * Executes for-in and for-of loops (ForIn/OfHeadEvaluation and ForIn/OfBodyEvaluation).
     * <p>
     * for-in 과 for-of 반복문을 실행한다(ForIn/OfHeadEvaluation, ForIn/OfBodyEvaluation).
     *
     * @param n      the FOR_IN or FOR_OF node / FOR_IN 또는 FOR_OF 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param st     the node state or null / 노드 상태 또는 null
     * @param labels the loop's labels / 반복문의 레이블
     * @return the completion / 완료
     */
    private Completion execForInOf(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        boolean isOf = n.type == NodeType.FOR_OF;
        JsObject source;
        Environment iterEnv = env;
        int phase = st != null ? st.phase : 0;
        JsValue v = st != null && st.o3 != null ? (JsValue) st.o3 : Js.UNDEFINED;
        if (phase == 0 && !isOf && n.a.type == NodeType.VAR_DECL && n.a.list[0].b != null) {
            initializeForInVariable(n.a.list[0], env, ctx, st);
            // 우변에서 중단되어도 초기화식은 한 번만 실행한다.
            phase = FOR_IN_AFTER_INITIALIZER;
            if (st != null) {
                st.o1 = null;
                st.phase = phase;
            }
        }
        if (phase == 0 || phase == FOR_IN_AFTER_INITIALIZER) {
            Environment headEnv = env;
            if (n.scope != null) {
                headEnv = new Environment(env);
                in.declareLexicals(n.scope, headEnv);
            }
            JsValue rhs = in.ex.eval(n.b, headEnv, ctx);
            if (isOf) {
                source = IteratorHelper.getIterator(realm, rhs);
            } else {
                if (rhs.isNullOrUndefined()) {
                    return Completion.normal(Js.UNDEFINED);
                }
                source = new ForInIterator(realm, Ops.toObject(realm, rhs));
            }
            if (st != null) {
                st.o1 = source;
                st.phase = 1;
            }
            phase = 1;
        } else {
            source = (JsObject) st.o1;
            if (phase == 2 || phase == FOR_CALL_TARGET) {
                iterEnv = (Environment) st.o2;
            }
        }

        // 값을 하나 꺼내 바인딩하고(1) 본문을 실행하는(2) 과정을 반복한다.
        while (true) {
            in.limits.tick();
            if (phase == 1 || phase == FOR_CALL_TARGET) {
                JsValue item = Js.UNDEFINED;
                if (phase == 1) {
                    if (isOf) {
                        JsObject r = IteratorHelper.step(source);
                        if (r == null) {
                            return Completion.normal(v);
                        }
                        item = IteratorHelper.value(r);
                    } else {
                        String key = ((ForInIterator) source).nextKey();
                        if (key == null) {
                            return Completion.normal(v);
                        }
                        item = JsString.of(key);
                    }
                    iterEnv = env;
                    if (n.scope != null) {
                        iterEnv = new Environment(env);
                        in.declareLexicals(n.scope, iterEnv);
                    }
                    // 잘못된 호출 대상에서 중단하면 새 반복 값을 꺼내지 않고 그 호출을 재개한다.
                    if (st != null && n.a.type == NodeType.CALL) {
                        st.phase = FOR_CALL_TARGET;
                        st.o2 = iterEnv;
                    }
                }
                try {
                    bindLoopVariable(n.a, item, iterEnv, ctx);
                } catch (JsException e) {
                    if (isOf) {
                        IteratorHelper.closeQuietly(source);
                    }
                    throw e;
                } catch (GeneratorReturn g) {
                    if (isOf) {
                        IteratorHelper.close(source);
                    }
                    throw g;
                }
                phase = 2;
                if (st != null) {
                    st.phase = 2;
                    st.o2 = iterEnv;
                }
            }
            Completion c;
            try {
                c = exec(n.d, iterEnv, ctx);
            } catch (JsException e) {
                if (isOf) {
                    IteratorHelper.closeQuietly(source);
                }
                throw e;
            } catch (GeneratorReturn g) {
                if (isOf) {
                    IteratorHelper.closeQuietly(source);
                }
                throw g;
            }
            phase = 1;
            if (st != null) {
                st.phase = 1;
            }
            if (c.value != null) {
                v = c.value;
                if (st != null) {
                    st.o3 = v;
                }
            }
            int act = bodyAction(c, labels);
            if (act != 0) {
                if (isOf) {
                    IteratorHelper.close(source);
                }
                return act == 1 ? Completion.normal(v) : Completion.updateEmpty(c, v);
            }
        }
    }

    /**
     * Saved state of one for-await-of loop.
     * <p>
     * for-await-of 반복 하나의 저장 상태.
     */
    private static final class ForAwaitState {
        /** Base environment. / 기준 환경. */
        Environment base;
        /** Async iterator. / 비동기 이터레이터. */
        JsObject iterator;
        /** Current iteration environment. / 현재 반복 환경. */
        Environment iteration;
        /** Value used by the next async step. / 다음 비동기 단계가 쓸 값. */
        JsValue awaitedValue;
        /** True while an async step is suspended. / 비동기 단계가 중단됐으면 true. */
        boolean awaiting;
        /** Completion accumulated through the loop. / 반복에서 누적한 완료. */
        JsValue completion = Js.UNDEFINED;
        /** Pending completion after iterator close. / 이터레이터 close 뒤 보류 완료. */
        Completion pending;
        /** Pending abrupt JavaScript completion. / 보류 중인 비정상 JavaScript 완료. */
        RuntimeException pendingThrow;
    }

    /**
     * Executes a for-await-of statement by awaiting each iterator.next/return result.
     * <p>
     * 각 iterator.next/return 결과를 await 하며 for-await-of 문을 실행한다.
     */
    private Completion execForAwait(Node n, Environment env, Ctx ctx, NodeState st, List<String> labels) {
        ForAwaitState state;
        if (st.o1 instanceof ForAwaitState) {
            state = (ForAwaitState) st.o1;
        } else {
            Environment headEnv = env;
            if (n.scope != null) {
                headEnv = new Environment(env);
                in.declareLexicals(n.scope, headEnv);
            }
            state = new ForAwaitState();
            state.base = env;
            state.iterator = AsyncIteratorHelper.getAsyncIterator(realm, in.ex.eval(n.b, headEnv, ctx));
            st.o1 = state;
            st.phase = 1;
        }
        // 이터레이터 단계, 본문, close 단계를 await 중단과 함께 순서대로 진행한다.
        while (true) {
            in.limits.tick();
            if (st.phase == 1) {
                if (!state.awaiting) {
                    state.awaitedValue = AsyncIteratorHelper.next(state.iterator);
                }
                JsValue nextValue = awaitForLoop(state, ctx);
                if (!(nextValue instanceof JsObject)) {
                    throw JsException.typeError("Async iterator result " + JsException.describe(nextValue) + " is not an object");
                }
                JsObject result = (JsObject) nextValue;
                if (IteratorHelper.isDone(result)) {
                    return Completion.normal(state.completion);
                }
                state.iteration = env;
                if (n.scope != null) {
                    state.iteration = new Environment(env);
                    in.declareLexicals(n.scope, state.iteration);
                }
                try {
                    JsValue value = IteratorHelper.value(result);
                    if (n.a.type == NodeType.CALL) {
                        st.phase = FOR_CALL_TARGET;
                    } else {
                        bindLoopVariable(n.a, value, state.iteration, ctx);
                        st.phase = 2;
                    }
                } catch (RuntimeException e) {
                    return beginAsyncClose(state, n, st, ctx, labels, null, e);
                }
            }
            if (st.phase == FOR_CALL_TARGET) {
                try {
                    throw in.ex.callAssignmentError(n.a, state.iteration, ctx);
                } catch (JsException e) {
                    return beginAsyncClose(state, n, st, ctx, labels, null, e);
                } catch (GeneratorReturn e) {
                    return beginAsyncClose(state, n, st, ctx, labels, null, e);
                }
            }
            if (st.phase == 2) {
                Completion c;
                try {
                    c = exec(n.d, state.iteration, ctx);
                } catch (JsException e) {
                    return beginAsyncClose(state, n, st, ctx, labels, null, e);
                } catch (GeneratorReturn e) {
                    return beginAsyncClose(state, n, st, ctx, labels, null, e);
                }
                if (c.value != null) {
                    state.completion = c.value;
                }
                int act = bodyAction(c, labels);
                if (act != 0) {
                    Completion done = act == 1 ? Completion.normal(state.completion) : Completion.updateEmpty(c, state.completion);
                    return beginAsyncClose(state, n, st, ctx, labels, done, null);
                }
                st.phase = 1;
            }
            if (st.phase == 3) {
                JsValue closed;
                try {
                    closed = awaitForLoop(state, ctx);
                } catch (RuntimeException e) {
                    if (state.pendingThrow != null) {
                        throw state.pendingThrow;
                    }
                    throw e;
                }
                if (closed != null && !(closed instanceof JsObject)) {
                    throw JsException.typeError("Async iterator return() result is not an object");
                }
                if (state.pendingThrow != null) {
                    throw state.pendingThrow;
                }
                return state.pending;
            }
        }
    }

    /**
     * Starts asynchronous iterator close after an abrupt loop body completion.
     * <p>
     * 반복 본문의 비정상 완료 뒤 비동기 이터레이터 close 를 시작한다.
     */
    private Completion beginAsyncClose(ForAwaitState state, Node loop, NodeState st, Ctx ctx, List<String> labels,
            Completion pending, RuntimeException abrupt) {
        state.pending = pending;
        state.pendingThrow = abrupt;
        JsValue close = AsyncIteratorHelper.close(state.iterator);
        if (close == null) {
            if (abrupt != null) {
                throw abrupt;
            }
            return pending;
        }
        state.awaitedValue = close;
        state.awaiting = false;
        st.phase = 3;
        return execForAwait(loop, state.base, ctx, st, labels);
    }

    /**
     * Suspends or resumes one Promise result within for-await.
     * <p>
     * for-await 안의 Promise 결과 하나를 중단하거나 재개한다.
     */
    private JsValue awaitForLoop(ForAwaitState state, Ctx ctx) {
        if (!state.awaiting) {
            state.awaiting = true;
            ctx.gen.suspending = true;
            ctx.gen.awaited = state.awaitedValue;
            throw AwaitSignal.INSTANCE;
        }
        state.awaiting = false;
        if (ctx.gen.resumeMode == GenState.THROW) {
            throw new JsException(ctx.gen.resumeValue);
        }
        return ctx.gen.resumeValue;
    }

    /**
     * Binds the value of the current iteration to the loop head: a declaration or an assignment target.
     * <p>
     * 현재 반복의 값을 반복문 머리에 바인딩한다: 선언 또는 대입 대상.
     *
     * @param left  the head: VAR_DECL or a target / 머리: VAR_DECL 또는 대상
     * @param value the value / 값
     * @param env   the iteration environment / 반복 환경
     * @param ctx   the context / 문맥
     */
    private void bindLoopVariable(Node left, JsValue value, Environment env, Ctx ctx) {
        if (left.type == NodeType.VAR_DECL) {
            Node target = left.list[0].a;
            in.pat.bind(target, value, env, ctx, left.op == Op.VAR ? Patterns.ASSIGN : Patterns.INIT);
        } else if (left.type == NodeType.CALL) {
            throw in.ex.callAssignmentError(left, env, ctx);
        } else {
            in.pat.bind(left, value, env, ctx, Patterns.ASSIGN);
        }
    }

    // ------------------------------------------------------------------ other statements

    /**
     * Executes a labelled statement: a break to the label ends it normally.
     * <p>
     * 레이블 문장을 실행한다: 그 레이블로의 break 는 정상 종료가 된다.
     *
     * @param n      the LABELED node / LABELED 노드
     * @param env    the environment / 환경
     * @param ctx    the context / 문맥
     * @param labels the outer labels or null / 바깥 레이블 또는 null
     * @return the completion / 완료
     */
    private Completion execLabeled(Node n, Environment env, Ctx ctx, List<String> labels) {
        List<String> ls = new ArrayList<String>();
        if (labels != null) {
            ls.addAll(labels);
        }
        ls.add(n.name);
        Completion c = exec(n.a, env, ctx, ls);
        if (c.type == Completion.BREAK && n.name.equals(c.target)) {
            return Completion.normal(c.value);
        }
        return c;
    }

    /**
     * Executes a with statement.
     * <p>
     * with 문을 실행한다.
     *
     * @param n   the WITH node / WITH 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the completion / 완료
     */
    private Completion execWith(Node n, Environment env, Ctx ctx, NodeState st) {
        Environment we;
        if (st != null && st.o1 != null) {
            we = (Environment) st.o1;
        } else {
            JsObject o = Ops.toObject(realm, memo(st, n.a, env, ctx));
            we = new Environment(env);
            we.objectRecord = o;
            we.withEnv = true;
            if (st != null) {
                st.o1 = we;
            }
        }
        Completion c = exec(n.b, we, ctx);
        return Completion.updateEmpty(c, Js.UNDEFINED);
    }

    /**
     * Executes a try statement (ECMA-262 6th edition 13.15). The completion of the try or catch block is kept while the finally
     * block runs; an abrupt completion of finally replaces it. A generator's return request passes through the same
     * path so that finally blocks run.
     * <p>
     * try 문을 실행한다(ECMA-262 6판 13.15). finally 블록이 실행되는 동안 try 또는 catch 블록의 완료를 보관하며, finally 의 비정상
     * 완료는 이를 대체한다. 제너레이터의 return 요청도 같은 경로로 지나가므로 finally 블록이 실행된다.
     *
     * @param n   the TRY node / TRY 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the completion / 완료
     */
    private Completion execTry(Node n, Environment env, Ctx ctx, NodeState st) {
        int phase = st != null ? st.phase : 0;
        Completion outcome = null;
        RuntimeException pending = null;
        if (phase < 2) {
            try {
                if (phase == 0) {
                    try {
                        outcome = exec(n.a, env, ctx);
                    } catch (JsException e) {
                        if (n.c == null) {
                            throw e;
                        }
                        if (st != null) {
                            st.phase = 1;
                        }
                        outcome = runCatch(n, env, ctx, st, e);
                    }
                } else {
                    outcome = exec(n.c, (Environment) st.o1, ctx);
                }
            } catch (JsException e) {
                pending = e;
            } catch (GeneratorReturn g) {
                pending = g;
            }
        } else {
            outcome = (Completion) st.o2;
            pending = (RuntimeException) st.o3;
        }
        if (n.d != null) {
            if (phase < 2 && st != null) {
                st.phase = 2;
                st.o2 = outcome;
                st.o3 = pending;
            }
            Completion f = exec(n.d, env, ctx);
            if (f.type != Completion.NORMAL) {
                return f;
            }
        }
        if (pending != null) {
            throw pending;
        }
        return Completion.updateEmpty(outcome, Js.UNDEFINED);
    }

    /**
     * Runs a catch clause: creates its scope, binds the parameter and executes the block.
     * <p>
     * catch 절을 실행한다: 스코프를 만들고 매개변수를 바인딩하고 블록을 실행한다.
     *
     * @param n   the TRY node / TRY 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @param e   the caught exception / 잡은 예외
     * @return the completion of the catch block / catch 블록의 완료
     */
    private Completion runCatch(Node n, Environment env, Ctx ctx, NodeState st, JsException e) {
        Environment ce = new Environment(env);
        in.declareLexicals(n.scope, ce);
        in.instantiateBlockFunctions(n.scope, ce);
        if (st != null) {
            st.o1 = ce;
        }
        in.pat.bind(n.b, e.getValue(realm), ce, ctx, Patterns.INIT);
        return exec(n.c, ce, ctx);
    }

    /**
     * Executes a switch statement.
     * <p>
     * switch 문을 실행한다.
     *
     * @param n   the SWITCH node / SWITCH 노드
     * @param env the environment / 환경
     * @param ctx the context / 문맥
     * @param st  the node state or null / 노드 상태 또는 null
     * @return the completion / 완료
     */
    private Completion execSwitch(Node n, Environment env, Ctx ctx, NodeState st) {
        JsValue disc = memo(st, n.a, env, ctx);
        Environment senv = env;
        if (n.scope != null) {
            if (st != null && st.o1 != null) {
                senv = (Environment) st.o1;
            } else {
                senv = new Environment(env);
                in.declareLexicals(n.scope, senv);
                in.instantiateBlockFunctions(n.scope, senv);
                if (st != null) {
                    st.o1 = senv;
                }
            }
        }
        // case 조건을 순서대로 비교해 일치하는 절을 찾는다. 일치가 없으면 default 절이다.
        int matched = -1;
        int defaultIdx = -1;
        // 일치하는 case 를 순서대로 찾는다.
        for (int i = 0; i < n.list.length && matched < 0; i++) {
            Node c = n.list[i];
            if (c.a == null) {
                defaultIdx = i;
            } else if (Ops.strictEquals(disc, memo(st, c.a, senv, ctx))) {
                matched = i;
            }
        }
        if (matched < 0) {
            matched = defaultIdx;
        }
        if (matched < 0) {
            return Completion.normal(Js.UNDEFINED);
        }
        JsValue v = st != null && st.o2 != null ? (JsValue) st.o2 : Js.UNDEFINED;
        int ci = matched;
        int si = 0;
        if (st != null && st.phase == 1) {
            ci = st.idx;
            si = st.idx2;
        }
        // 일치한 절부터 끝까지(fall-through) 문장을 실행한다.
        for (; ci < n.list.length; ci++) {
            Node[] body = n.list[ci].list;
            // 선택된 case 부터 문장을 순서대로 실행한다(fall-through).
            for (; si < body.length; si++) {
                if (st != null) {
                    st.phase = 1;
                    st.idx = ci;
                    st.idx2 = si;
                    st.o2 = v;
                }
                Completion c = exec(body[si], senv, ctx);
                if (c.value != null) {
                    v = c.value;
                }
                if (c.type == Completion.BREAK && c.target == null) {
                    return Completion.normal(v);
                }
                if (c.type != Completion.NORMAL) {
                    return Completion.updateEmpty(c, v);
                }
            }
            si = 0;
        }
        return Completion.normal(v);
    }
}
