package org.duckdns.hjow.swingwebpanel.dom;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The registry of the live {@link Range}s and {@link NodeIterator}s of one document: the objects that must follow the changes of the tree
 * (DOM Standard, "live ranges" and the "NodeIterator pre-removing steps"). The document tells the registry about every change of a child
 * list and of character data; the registry passes it on to each object that is still alive. The objects are held weakly, so a range or an
 * iterator that nobody uses any more is collected with no cleanup call.
 * <p>
 * 한 문서의 살아 있는 {@link Range} 와 {@link NodeIterator} 의 등록부: 트리의 변경을 따라가야 하는 객체들(DOM 표준 "live ranges" 와 "NodeIterator
 * pre-removing steps"). 문서가 자식 목록과 문자 데이터의 모든 변경을 등록부에 알리면 등록부는 아직 살아 있는 각 객체에 전달한다. 객체는 약하게
 * 붙잡으므로 아무도 쓰지 않는 범위나 반복자는 정리 호출 없이 수거된다.
 */
final class LiveRanges {

    /**
     * The live ranges.
     * <p>
     * 살아 있는 범위.
     */
    private final List<WeakReference<Range>> ranges = new ArrayList<WeakReference<Range>>();

    /**
     * The live iterators.
     * <p>
     * 살아 있는 반복자.
     */
    private final List<WeakReference<NodeIterator>> iterators = new ArrayList<WeakReference<NodeIterator>>();

    /**
     * Registers a range.
     * <p>
     * 범위를 등록한다.
     *
     * @param range the range / 범위
     */
    void add(Range range) {
        prune();
        ranges.add(new WeakReference<Range>(range));
    }

    /**
     * Registers an iterator.
     * <p>
     * 반복자를 등록한다.
     *
     * @param iterator the iterator / 반복자
     */
    void add(NodeIterator iterator) {
        prune();
        iterators.add(new WeakReference<NodeIterator>(iterator));
    }

    /**
     * Drops the entries whose object was collected.
     * <p>
     * 객체가 수거된 항목을 지운다.
     */
    private void prune() {
        // 수거된 약한 참조를 두 목록에서 지운다. 항목이 많을 때만 해서 등록 비용을 일정하게 유지한다.
        if (ranges.size() > 32 && (ranges.size() & 31) == 0) {
            for (int i = ranges.size() - 1; i >= 0; i--) {
                if (ranges.get(i).get() == null) {
                    ranges.remove(i);
                }
            }
        }
        if (iterators.size() > 32 && (iterators.size() & 31) == 0) {
            // 반복자 목록에서 수거된 항목을 뒤에서부터 지운다.
            for (int i = iterators.size() - 1; i >= 0; i--) {
                if (iterators.get(i).get() == null) {
                    iterators.remove(i);
                }
            }
        }
    }

    /**
     * Tells whether nothing is registered (the document then skips the work).
     * <p>
     * 등록된 것이 없는지 알려 준다(그러면 문서는 일을 건너뛴다).
     *
     * @return true if empty / 비어 있으면 true
     */
    boolean isEmpty() {
        return ranges.isEmpty() && iterators.isEmpty();
    }

    /**
     * Applies a change of the child list of a parent to all registered ranges and iterators.
     * <p>
     * 부모의 자식 목록 변경을 등록된 모든 범위와 반복자에 적용한다.
     *
     * @param parent  the parent / 부모
     * @param added   the inserted nodes / 삽입된 노드
     * @param removed the removed nodes / 제거된 노드
     * @param prev    the sibling before the changed place, or null / 바뀐 자리 앞의 형제. 없으면 null
     * @param next    the sibling after the changed place, or null / 바뀐 자리 뒤의 형제. 없으면 null
     */
    void childListChanged(Node parent, List<Node> added, List<Node> removed, Node prev, Node next) {
        int index = prev == null ? 0 : Range.indexOf(prev) + 1;
        // 살아 있는 범위마다 경계점을 조정한다.
        for (int i = 0; i < ranges.size(); i++) {
            Range r = ranges.get(i).get();
            if (r != null) {
                r.childListChanged(parent, removed, index, added.size());
            }
        }
        if (!removed.isEmpty()) {
            // 살아 있는 반복자마다 기준 노드를 조정한다.
            for (int i = 0; i < iterators.size(); i++) {
                NodeIterator it = iterators.get(i).get();
                if (it != null) {
                    it.nodesRemoved(parent, removed, prev, next);
                }
            }
        }
    }

    /**
     * Applies a replacement of character data to all registered ranges.
     * <p>
     * 문자 데이터의 교체를 등록된 모든 범위에 적용한다.
     *
     * @param node       the node / 노드
     * @param offset     where the replacement started / 교체가 시작된 위치
     * @param count      the number of replaced characters / 교체된 문자 수
     * @param dataLength the number of inserted characters / 삽입된 문자 수
     */
    void dataReplaced(Node node, int offset, int count, int dataLength) {
        // 살아 있는 범위마다 경계점을 조정한다.
        for (int i = 0; i < ranges.size(); i++) {
            Range r = ranges.get(i).get();
            if (r != null) {
                r.dataReplaced(node, offset, count, dataLength);
            }
        }
    }

    /**
     * Applies the split of a text node to all registered ranges.
     * <p>
     * 텍스트 노드의 분할을 등록된 모든 범위에 적용한다.
     *
     * @param node    the original node / 원래 노드
     * @param created the new node / 새 노드
     * @param offset  the split offset / 쪼갠 위치
     */
    void textSplit(Node node, Node created, int offset) {
        // 살아 있는 범위마다 경계점을 조정한다.
        for (int i = 0; i < ranges.size(); i++) {
            Range r = ranges.get(i).get();
            if (r != null) {
                r.textSplit(node, created, offset);
            }
        }
    }

    /**
     * Passes the merge of a text node into the one before it (<code>normalize</code>) to the live ranges.
     * <p>
     * 텍스트 노드를 앞의 텍스트 노드에 합치는 일(<code>normalize</code>)을 살아 있는 범위에 전달한다.
     *
     * @param target  the text node that receives the data / 데이터를 받는 텍스트 노드
     * @param current the merged text node / 합쳐지는 텍스트 노드
     * @param length  the length of the target before the data of the current node / 현재 노드의 데이터 앞의 대상 길이
     * @param index   the index of the current node in its parent / 현재 노드의 부모 안의 위치
     */
    void textMerged(Node target, Node current, int length, int index) {
        // 살아 있는 범위마다 경계점을 조정한다.
        for (int i = 0; i < ranges.size(); i++) {
            Range r = ranges.get(i).get();
            if (r != null) {
                r.textMerged(target, current, length, index);
            }
        }
    }
}
