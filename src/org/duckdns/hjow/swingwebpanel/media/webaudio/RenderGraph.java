package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Decides the order in which the nodes of a context are processed (Web Audio API 2.6, "order the AudioNodes of the BaseAudioContext to be
 * processed"). A node is processed after every node that feeds one of its inputs or one of its parameters. A cycle in the graph is a
 * strongly connected component with more than one node (or a node connected to itself): a delay node in a cycle is split into a reader
 * step (it reads the delay line, so it comes first like a source) and a writer step (it writes the delay line, so it comes after its
 * inputs), which breaks the cycle; the nodes of a cycle that has no delay node are muted. The components are found with Tarjan's
 * algorithm without recursion, so a chain of thousands of nodes cannot overflow the stack.
 * <p>
 * 컨텍스트의 노드를 처리하는 순서를 정한다(Web Audio API 2.6, "order the AudioNodes of the BaseAudioContext to be processed"). 노드는 자기 입력이나
 * 파라미터에 공급하는 모든 노드 뒤에 처리된다. 그래프의 순환은 노드가 둘 이상인 강연결 요소(또는 자기 자신에 연결된 노드)이다: 순환 안의 지연 노드는
 * 읽기 단계(지연선을 읽으므로 원천처럼 먼저 온다)와 쓰기 단계(지연선에 쓰므로 입력 뒤에 온다)로 나뉘어 순환이 끊어지고, 지연 노드가 없는 순환의 노드는
 * 무음 처리된다. 요소는 재귀 없이 Tarjan 알고리즘으로 찾으므로 수천 개의 노드 사슬도 스택을 넘치게 하지 않는다.
 */
final class RenderGraph {

    /**
     * The rendering order: the steps and their phases.
     * <p>
     * 렌더링 순서: 단계와 그 단계의 종류.
     */
    static final class Plan {

        /**
         * The nodes in processing order; a split delay node appears twice.
         * <p>
         * 처리 순서의 노드. 나뉜 지연 노드는 두 번 나온다.
         */
        final AudioNode[] nodes;

        /**
         * The phase of every step, one of the phase constants of {@link AudioNode}.
         * <p>
         * 단계마다의 종류({@link AudioNode} 의 단계 상수 중 하나).
         */
        final int[] phases;

        /**
         * The number of nodes that are muted because they are part of a cycle without a delay.
         * <p>
         * 지연 없는 순환의 일부여서 무음 처리된 노드 수.
         */
        final int mutedCount;

        /**
         * Creates a plan.
         * <p>
         * 계획을 만든다.
         *
         * @param nodes      the nodes / 노드
         * @param phases     the phases / 단계 종류
         * @param mutedCount the number of muted nodes / 무음 처리된 노드 수
         */
        Plan(AudioNode[] nodes, int[] phases, int mutedCount) {
            this.nodes = nodes;
            this.phases = phases;
            this.mutedCount = mutedCount;
        }
    }

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private RenderGraph() {
    }

    /**
     * Orders the nodes. The <code>muted</code> flag of every node is set or cleared.
     * <p>
     * 노드의 순서를 정한다. 모든 노드의 <code>muted</code> 표시를 켜거나 끈다.
     *
     * @param candidates the nodes that are processed / 처리할 노드
     * @return the plan / 계획
     */
    @SuppressWarnings("unchecked")
    static Plan order(List<AudioNode> candidates) {
        int m = candidates.size();
        IdentityHashMap<AudioNode, Integer> index = new IdentityHashMap<AudioNode, Integer>();
        // 노드마다 번호를 매기고 무음 표시를 지운다.
        for (int i = 0; i < m; i++) {
            AudioNode n = candidates.get(i);
            index.put(n, Integer.valueOf(i));
            n.muted = false;
            if (n instanceof DelayNode) {
                ((DelayNode) n).setCycleBreaker(false);
            }
        }
        ArrayList<Integer>[] adj = new ArrayList[m];
        for (int i = 0; i < m; i++) {
            adj[i] = new ArrayList<Integer>();
        }
        // 공급하는 노드에서 공급받는 노드로 가는 변을 만든다(입력과 파라미터 모두).
        for (int j = 0; j < m; j++) {
            AudioNode n = candidates.get(j);
            for (int in = 0; in < n.inputs.length; in++) {
                ArrayList<AudioNodeOutput> srcs = n.inputs[in].sources;
                for (int k = 0; k < srcs.size(); k++) {
                    Integer s = index.get(srcs.get(k).node);
                    if (s != null) {
                        adj[s.intValue()].add(Integer.valueOf(j));
                    }
                }
            }
        }
        for (int j = 0; j < m; j++) {
            AudioNode n = candidates.get(j);
            addParamEdges(n, j, index, adj, null);
        }
        int[][] comps = tarjan(m, toArrays(adj));
        boolean[] cyclic = new boolean[m];
        boolean any = false;
        // 노드가 둘 이상이거나 자기 자신으로 가는 변이 있는 요소가 순환이다.
        for (int c = 0; c < comps.length; c++) {
            int[] comp = comps[c];
            boolean cycle = comp.length > 1 || adj[comp[0]].contains(Integer.valueOf(comp[0]));
            if (cycle) {
                any = true;
                for (int k = 0; k < comp.length; k++) {
                    cyclic[comp[k]] = true;
                }
            }
        }
        if (!any) {
            AudioNode[] nodes = new AudioNode[m];
            int[] phases = new int[m];
            // 요소가 완료된 순서의 반대가 처리 순서이다.
            for (int c = comps.length - 1, p = 0; c >= 0; c--, p++) {
                nodes[p] = candidates.get(comps[c][0]);
                phases[p] = AudioNode.PHASE_FULL;
            }
            return new Plan(nodes, phases, 0);
        }
        return orderWithCycles(candidates, index, cyclic);
    }

    /**
     * Adds the edges from the sources of the parameters of a node to the node (or to its reader vertex).
     * <p>
     * 노드의 파라미터에 공급하는 원천에서 노드(또는 읽기 정점)로 가는 변을 더한다.
     *
     * @param n        the node / 노드
     * @param target   the vertex that depends on the parameters / 파라미터에 의존하는 정점
     * @param index    the vertex index of every node / 노드마다의 정점 번호
     * @param adj      the adjacency lists / 인접 목록
     * @param readerOf the reader vertex of every split delay node (by node vertex), or null when nothing is split / 나뉜 지연 노드마다의 읽기 정점(노드 정점 기준).
     *                 아무것도 나뉘지 않았으면 null
     */
    private static void addParamEdges(AudioNode n, int target, IdentityHashMap<AudioNode, Integer> index, ArrayList<Integer>[] adj, int[] readerOf) {
        ArrayList<AudioParam> params = n.paramsForGraph();
        // 파라미터마다 그것을 구동하는 출력의 노드에서 변을 만든다.
        for (int p = 0; p < params.size(); p++) {
            ArrayList<AudioNodeOutput> srcs = params.get(p).sources;
            for (int k = 0; k < srcs.size(); k++) {
                Integer s = index.get(srcs.get(k).node);
                if (s != null) {
                    int from = s.intValue();
                    if (readerOf != null && readerOf[from] >= 0) {
                        from = readerOf[from];
                    }
                    adj[from].add(Integer.valueOf(target));
                }
            }
        }
    }

    /**
     * Orders a graph that has cycles: the delay nodes in cycles are split, the cycles that remain are muted.
     * <p>
     * 순환이 있는 그래프의 순서를 정한다: 순환 안의 지연 노드를 나누고 남는 순환은 무음 처리한다.
     *
     * @param candidates the nodes / 노드
     * @param index      the vertex index of every node / 노드마다의 정점 번호
     * @param cyclic     true for the nodes that are in a cycle / 순환 안에 있는 노드이면 true
     * @return the plan / 계획
     */
    @SuppressWarnings("unchecked")
    private static Plan orderWithCycles(List<AudioNode> candidates, IdentityHashMap<AudioNode, Integer> index, boolean[] cyclic) {
        int m = candidates.size();
        int[] readerOf = new int[m];
        Arrays.fill(readerOf, -1);
        int total = m;
        ArrayList<Integer> readerOwners = new ArrayList<Integer>();
        // 순환 안의 지연 노드마다 읽기 정점을 하나 더 만든다.
        for (int v = 0; v < m; v++) {
            if (cyclic[v] && candidates.get(v) instanceof DelayNode) {
                readerOf[v] = total++;
                readerOwners.add(Integer.valueOf(v));
            }
        }
        ArrayList<Integer>[] adj = new ArrayList[total];
        for (int i = 0; i < total; i++) {
            adj[i] = new ArrayList<Integer>();
        }
        // 오디오 입력 변: 나뉜 지연 노드의 출력은 읽기 정점에서 나오고, 입력은 쓰기 정점(원래 정점)으로 들어간다.
        for (int j = 0; j < m; j++) {
            AudioNode n = candidates.get(j);
            for (int in = 0; in < n.inputs.length; in++) {
                ArrayList<AudioNodeOutput> srcs = n.inputs[in].sources;
                for (int k = 0; k < srcs.size(); k++) {
                    Integer s = index.get(srcs.get(k).node);
                    if (s != null) {
                        int from = readerOf[s.intValue()] >= 0 ? readerOf[s.intValue()] : s.intValue();
                        adj[from].add(Integer.valueOf(j));
                    }
                }
            }
        }
        // 파라미터 변: 나뉜 지연 노드의 파라미터는 읽기 정점이 필요로 한다.
        for (int j = 0; j < m; j++) {
            int target = readerOf[j] >= 0 ? readerOf[j] : j;
            addParamEdges(candidates.get(j), target, index, adj, readerOf);
        }
        int[][] comps = tarjan(total, toArrays(adj));
        boolean[] mutedNode = new boolean[m];
        int muted = 0;
        // 나눈 뒤에도 남는 순환의 노드는 무음 처리한다(읽기 정점이면 그 지연 노드 전체).
        for (int c = 0; c < comps.length; c++) {
            int[] comp = comps[c];
            boolean cycle = comp.length > 1 || adj[comp[0]].contains(Integer.valueOf(comp[0]));
            if (!cycle) {
                continue;
            }
            for (int k = 0; k < comp.length; k++) {
                int node = comp[k] < m ? comp[k] : readerOwners.get(comp[k] - m).intValue();
                if (!mutedNode[node]) {
                    mutedNode[node] = true;
                    muted++;
                    candidates.get(node).muted = true;
                }
            }
        }
        ArrayList<AudioNode> nodes = new ArrayList<AudioNode>();
        ArrayList<Integer> phases = new ArrayList<Integer>();
        // 요소가 완료된 순서의 반대로, 무음이 아닌 정점만 단계로 만든다.
        for (int c = comps.length - 1; c >= 0; c--) {
            int v = comps[c][0];
            int nodeIndex = v < m ? v : readerOwners.get(v - m).intValue();
            // 순환의 정점은 이미 노드가 무음 처리되어 있으므로 함께 건너뛴다.
            if (mutedNode[nodeIndex]) {
                continue;
            }
            nodes.add(candidates.get(nodeIndex));
            if (v >= m) {
                phases.add(Integer.valueOf(AudioNode.PHASE_READ));
            } else if (readerOf[v] >= 0) {
                phases.add(Integer.valueOf(AudioNode.PHASE_WRITE));
            } else {
                phases.add(Integer.valueOf(AudioNode.PHASE_FULL));
            }
        }
        AudioNode[] nodeArray = nodes.toArray(new AudioNode[nodes.size()]);
        int[] phaseArray = new int[phases.size()];
        for (int i = 0; i < phaseArray.length; i++) {
            phaseArray[i] = phases.get(i).intValue();
        }
        // 지연 노드가 순환을 끊는지 표시한다(delayTime 의 하한과 활성 판단이 달라진다).
        for (int v = 0; v < m; v++) {
            AudioNode n = candidates.get(v);
            if (n instanceof DelayNode) {
                ((DelayNode) n).setCycleBreaker(readerOf[v] >= 0 && !mutedNode[v]);
            }
        }
        return new Plan(nodeArray, phaseArray, muted);
    }

    /**
     * Copies adjacency lists into arrays.
     * <p>
     * 인접 목록을 배열로 복사한다.
     *
     * @param adj the lists / 목록
     * @return the arrays / 배열
     */
    private static int[][] toArrays(ArrayList<Integer>[] adj) {
        int[][] out = new int[adj.length][];
        // 정점마다 목록을 배열로 바꾼다.
        for (int v = 0; v < adj.length; v++) {
            out[v] = new int[adj[v].size()];
            for (int k = 0; k < out[v].length; k++) {
                out[v][k] = adj[v].get(k).intValue();
            }
        }
        return out;
    }

    /**
     * Finds the strongly connected components with Tarjan's algorithm, without recursion. The components come in the order in which they
     * are completed, which is the reverse of a topological order of the graph of components.
     * <p>
     * Tarjan 알고리즘으로 강연결 요소를 재귀 없이 찾는다. 요소는 완료된 순서로 나오며, 이는 요소 그래프의 위상 순서의 반대이다.
     *
     * @param n   the number of vertices / 정점 수
     * @param adj the successors of every vertex / 정점마다의 후속 정점
     * @return the components / 요소
     */
    static int[][] tarjan(int n, int[][] adj) {
        int[] number = new int[n];
        int[] low = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(number, -1);
        int[] stack = new int[n];
        int top = 0;
        int[] callVertex = new int[n];
        int[] callNext = new int[n];
        int counter = 0;
        ArrayList<int[]> result = new ArrayList<int[]>();
        // 방문하지 않은 정점마다 깊이 우선 탐색을 시작한다. 호출 스택은 배열로 직접 관리한다.
        for (int s = 0; s < n; s++) {
            if (number[s] != -1) {
                continue;
            }
            int depth;
            callVertex[0] = s;
            callNext[0] = 0;
            number[s] = low[s] = counter++;
            stack[top++] = s;
            onStack[s] = true;
            depth = 1;
            while (depth > 0) {
                int v = callVertex[depth - 1];
                if (callNext[depth - 1] < adj[v].length) {
                    int w = adj[v][callNext[depth - 1]++];
                    if (number[w] == -1) {
                        number[w] = low[w] = counter++;
                        stack[top++] = w;
                        onStack[w] = true;
                        callVertex[depth] = w;
                        callNext[depth] = 0;
                        depth++;
                    } else if (onStack[w]) {
                        low[v] = Math.min(low[v], number[w]);
                    }
                } else {
                    // 모든 후속 정점을 살폈다: 뿌리이면 요소를 스택에서 꺼낸다.
                    if (low[v] == number[v]) {
                        int size = 0;
                        int k = top;
                        while (stack[--k] != v) {
                            size++;
                        }
                        size++;
                        int[] comp = new int[size];
                        // 스택에서 v 까지 꺼내 요소로 만든다.
                        for (int i = 0; i < size; i++) {
                            int w = stack[--top];
                            onStack[w] = false;
                            comp[i] = w;
                        }
                        result.add(comp);
                    }
                    depth--;
                    if (depth > 0) {
                        int parent = callVertex[depth - 1];
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }
        }
        return result.toArray(new int[result.size()][]);
    }
}
