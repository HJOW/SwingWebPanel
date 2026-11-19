package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * The saved partial progress of one syntax tree node while a generator is suspended inside it. Only nodes that contain a
 * yield have such a state. When the generator is resumed the body is walked again from the top; every node on the path to the
 * suspended yield finds its state, skips the work that is already done (memoized child values, loop phase, current environment,
 * iterator ...) and re-enters the child that was suspended. This is how generators resume without Java threads.
 * <p>
 * 제너레이터가 노드 안에서 중단되었을 때 그 노드의 저장된 부분 진행 상황. yield 를 포함한 노드만 이 상태를 가진다. 제너레이터를
 * 재개하면 본문을 처음부터 다시 따라가며, 중단된 yield 까지의 경로에 있는 각 노드는 자신의 상태를 찾아 이미 끝낸 작업(저장된
 * 자식 값, 반복문 단계, 현재 환경, 이터레이터 등)을 건너뛰고 중단된 자식으로 다시 들어간다. 이것이 Java 스레드 없이 제너레이터를
 * 재개하는 방법이다.
 */
final class NodeState {

    /**
     * Memoized values of already evaluated children (in evaluation order).
     * <p>
     * 이미 평가한 자식의 저장된 값(평가 순서대로).
     */
    Object[] vals = new Object[4];

    /**
     * Number of memoized values.
     * <p>
     * 저장된 값의 개수.
     */
    int count;

    /**
     * Index of the next memo slot during the current walk (reset when the node is entered).
     * <p>
     * 이번 순회에서 다음에 쓸 저장 칸의 인덱스(노드에 들어갈 때 초기화).
     */
    int cursor;

    /**
     * A node specific phase number (loops, try statements, yield).
     * <p>
     * 노드별 단계 번호(반복문, try 문, yield).
     */
    int phase;

    /**
     * A node specific index (statement index, declarator index, case index).
     * <p>
     * 노드별 인덱스(문장 인덱스, 선언자 인덱스, case 인덱스).
     */
    int idx;

    /**
     * A second node specific index.
     * <p>
     * 노드별 두 번째 인덱스.
     */
    int idx2;

    /**
     * Node specific object slot 1 (environment, iterator, partially built object ...).
     * <p>
     * 노드별 객체 칸 1(환경, 이터레이터, 만들던 객체 등).
     */
    Object o1;

    /**
     * Node specific object slot 2.
     * <p>
     * 노드별 객체 칸 2.
     */
    Object o2;

    /**
     * Node specific object slot 3.
     * <p>
     * 노드별 객체 칸 3.
     */
    Object o3;

    /**
     * Appends a memoized value.
     * <p>
     * 저장 값을 추가한다.
     *
     * @param v the value / 값
     */
    void add(Object v) {
        if (count == vals.length) {
            Object[] n = new Object[count * 2];
            System.arraycopy(vals, 0, n, 0, count);
            vals = n;
        }
        vals[count++] = v;
    }
}
