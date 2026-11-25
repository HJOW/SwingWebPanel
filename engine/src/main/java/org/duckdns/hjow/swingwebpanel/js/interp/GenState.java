package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.IdentityHashMap;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;

/**
 * The resumption state of one generator activation: the saved {@link NodeState}s of all nodes that are currently suspended,
 * the request that resumed the generator (next, throw or return with a value) and the value being yielded.
 * <p>
 * 제너레이터 활성 하나의 재개 상태: 현재 중단된 모든 노드의 {@link NodeState}, 제너레이터를 재개한 요청(값이 있는 next, throw, return),
 * 내보내는(yield) 값.
 */
final class GenState {

    /**
     * Resume request: next(value).
     * <p>
     * 재개 요청: next(value).
     */
    static final int NEXT = 0;

    /**
     * Resume request: throw(value).
     * <p>
     * 재개 요청: throw(value).
     */
    static final int THROW = 1;

    /**
     * Resume request: return(value).
     * <p>
     * 재개 요청: return(value).
     */
    static final int RETURN = 2;

    /**
     * Saved states of suspended nodes, keyed by node identity.
     * <p>
     * 중단된 노드의 저장 상태. 노드 동일성으로 찾는다.
     */
    final IdentityHashMap<Node, NodeState> saved = new IdentityHashMap<Node, NodeState>();

    /**
     * True while the Java stack unwinds after a yield: nodes then keep their state instead of discarding it.
     * <p>
     * yield 뒤에 Java 스택이 풀리는 동안 true. 이때 노드는 상태를 버리지 않고 유지한다.
     */
    boolean suspending;

    /**
     * The request that resumed the generator.
     * <p>
     * 제너레이터를 재개한 요청.
     */
    int resumeMode;

    /**
     * The value of the resume request.
     * <p>
     * 재개 요청의 값.
     */
    JsValue resumeValue;

    /**
     * The value handed out by the last yield.
     * <p>
     * 마지막 yield 가 내보낸 값.
     */
    JsValue yielded;

    /**
     * True if {@link #yielded} already is an iterator result object (yield* passes the inner result through unchanged).
     * <p>
     * {@link #yielded} 가 이미 이터레이터 결과 객체이면 true(yield* 는 안쪽 결과를 그대로 전달한다).
     */
    boolean yieldedRaw;

    /**
     * Value awaited by a suspended async function.
     * <p>
     * 중단된 async 함수가 기다리는 값.
     */
    JsValue awaited;

    /**
     * Returns the state of a node, creating it on first entry, and restarts its memo cursor.
     * <p>
     * 노드의 상태를 반환한다. 처음 들어가면 만들며, 저장 칸 커서를 처음으로 되돌린다.
     *
     * @param n the node / 노드
     * @return the state / 상태
     */
    NodeState enter(Node n) {
        NodeState st = saved.get(n);
        if (st == null) {
            st = new NodeState();
            saved.put(n, st);
        }
        st.cursor = 0;
        return st;
    }

    /**
     * Discards the state of a node unless the generator is being suspended.
     * <p>
     * 제너레이터가 중단되는 중이 아니면 노드의 상태를 버린다.
     *
     * @param n the node / 노드
     */
    void leave(Node n) {
        if (!suspending) {
            saved.remove(n);
        }
    }
}
