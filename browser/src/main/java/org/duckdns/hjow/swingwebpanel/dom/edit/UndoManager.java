package org.duckdns.hjow.swingwebpanel.dom.edit;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Selection;

/**
 * The undo history of the editor: every editing operation is recorded as the list of DOM mutations that it caused (the
 * {@link MutationRecord}s of the document, which carry the old values), and undoing replays the inverse of those mutations in reverse
 * order, so that the DOM is exactly as it was and the nodes keep their identity. The selection before and after an operation is stored
 * with it. Undoing a step records the inverse mutations as a step of the redo list, which makes redo the same operation again.
 * Consecutive typing can be merged into one step with a key (the browsers undo a word of typing at a time, not a letter).
 * <p>
 * 편집기의 되돌리기 이력: 편집 작업마다 그것이 일으킨 DOM 변경 목록(옛 값을 가진 문서의 {@link MutationRecord})을 기록하고, 되돌릴 때는 그 변경의 역을 거꾸로
 * 다시 실행하므로 DOM 이 정확히 이전과 같고 노드의 동일성도 유지된다. 작업 전후의 선택도 함께 보관한다. 단계를 되돌리면 그 역변경이 다시 하기 목록의 한
 * 단계로 기록되어, 다시 하기는 같은 작업을 다시 한다. 이어지는 글자 입력은 키로 한 단계에 합칠 수 있다(브라우저는 글자 하나가 아니라 입력 덩어리를 되돌린다).
 */
public final class UndoManager implements DomMutationListener {

    /**
     * A recorded selection: the four numbers of <code>setBaseAndExtent</code>.
     * <p>
     * 기록된 선택: <code>setBaseAndExtent</code> 의 네 값.
     */
    private static final class Saved {
        /** The anchor node, or null if there was no selection. 앵커 노드. 선택이 없었으면 null. */
        final Node anchorNode;
        /** The anchor offset. 앵커 오프셋. */
        final int anchorOffset;
        /** The focus node. focus 노드. */
        final Node focusNode;
        /** The focus offset. focus 오프셋. */
        final int focusOffset;

        /**
         * Records the selection of a document.
         * <p>
         * 문서의 선택을 기록한다.
         *
         * @param s the selection / 선택
         */
        Saved(Selection s) {
            boolean has = s.getRangeCount() > 0;
            anchorNode = has ? s.getAnchorNode() : null;
            anchorOffset = has ? s.getAnchorOffset() : 0;
            focusNode = has ? s.getFocusNode() : null;
            focusOffset = has ? s.getFocusOffset() : 0;
        }

        /**
         * Puts the recorded selection back.
         * <p>
         * 기록한 선택을 되돌려 놓는다.
         *
         * @param s the selection of the document / 문서의 선택
         */
        void restore(Selection s) {
            if (anchorNode == null || focusNode == null) {
                s.removeAllRanges();
            } else {
                s.setBaseAndExtent(anchorNode, anchorOffset, focusNode, focusOffset);
            }
        }

        /**
         * Tells whether two recorded selections are the same.
         * <p>
         * 기록된 두 선택이 같은지 알려 준다.
         *
         * @param o the other / 다른 것
         * @return true if the same / 같으면 true
         */
        boolean same(Saved o) {
            return anchorNode == o.anchorNode && anchorOffset == o.anchorOffset && focusNode == o.focusNode && focusOffset == o.focusOffset;
        }
    }

    /**
     * One step of the history.
     * <p>
     * 이력의 한 단계.
     */
    private static final class Step {
        /** The mutations in the order they happened. 일어난 순서의 변경. */
        final List<MutationRecord> records = new ArrayList<MutationRecord>();
        /** The selection before the step. 단계 전의 선택. */
        Saved before;
        /** The selection after the step. 단계 뒤의 선택. */
        Saved after;
        /** The key that lets the next step merge into this one, or null. 다음 단계가 이 단계에 합쳐지게 하는 키. 없으면 null. */
        String mergeKey;
    }

    /**
     * The document.
     * <p>
     * 문서.
     */
    private final Document doc;

    /**
     * The steps that can be undone, the last one is the newest.
     * <p>
     * 되돌릴 수 있는 단계. 마지막이 가장 최근.
     */
    private final List<Step> undoSteps = new ArrayList<Step>();

    /**
     * The steps that can be redone, the last one is the next to redo.
     * <p>
     * 다시 할 수 있는 단계. 마지막이 다음에 다시 할 것.
     */
    private final List<Step> redoSteps = new ArrayList<Step>();

    /**
     * The step that collects mutations now, or null.
     * <p>
     * 지금 변경을 모으는 단계. 없으면 null.
     */
    private Step current;

    /**
     * The most steps that are kept (the oldest are dropped).
     * <p>
     * 보관하는 최대 단계 수(가장 오래된 것부터 버린다).
     */
    public static final int LIMIT = 200;

    /**
     * Creates the history of a document.
     * <p>
     * 문서의 이력을 만든다.
     *
     * @param doc the document / 문서
     */
    public UndoManager(Document doc) {
        this.doc = doc;
    }

    /**
     * Collects a mutation while a step is open.
     * <p>
     * 단계가 열려 있는 동안 변경을 모은다.
     *
     * @param record    the mutation / 변경
     * @param connected whether the node is connected / 노드가 연결되어 있는지
     */
    public void mutated(MutationRecord record, boolean connected) {
        if (current != null) {
            current.records.add(record);
        }
    }

    /**
     * Opens a step: the mutations of the document are collected until {@link #end}. Steps do not nest (a second call while one is open is
     * ignored and its {@link #end} does nothing).
     * <p>
     * 단계를 연다: {@link #end} 까지 문서의 변경을 모은다. 단계는 중첩되지 않는다(열려 있는 동안의 두 번째 호출은 무시되고 그 {@link #end} 도 아무것도 하지 않는다).
     *
     * @return true if this call opened the step (the caller must call {@link #end}) / 이 호출이 단계를 열었으면 true(호출자가 {@link #end} 를 불러야 한다)
     */
    public boolean begin() {
        if (current != null) {
            return false;
        }
        current = new Step();
        current.before = new Saved(doc.getSelection());
        doc.addMutationListener(this);
        return true;
    }

    /**
     * Closes the step that {@link #begin} opened. A step without mutations is dropped. Otherwise it joins the history; with a merge key equal
     * to that of the previous step, and when the selection before it is the one that the previous step left, it is merged into the previous
     * step (typing is undone by the word, not by the letter).
     * <p>
     * {@link #begin} 이 연 단계를 닫는다. 변경이 없는 단계는 버린다. 아니면 이력에 들어가며, 병합 키가 이전 단계와 같고 그 전의 선택이 이전 단계가 남긴 선택이면
     * 이전 단계에 합쳐진다(글자가 아니라 낱말 단위로 되돌린다).
     *
     * @param mergeKey the key for merging with the previous step, or null / 이전 단계와 합치는 키. 없으면 null
     */
    public void end(String mergeKey) {
        Step step = current;
        if (step == null) {
            return;
        }
        current = null;
        doc.removeMutationListener(this);
        if (step.records.isEmpty()) {
            return;
        }
        step.after = new Saved(doc.getSelection());
        redoSteps.clear();
        Step last = undoSteps.isEmpty() ? null : undoSteps.get(undoSteps.size() - 1);
        // 같은 키의 연속 입력이고 선택이 이어지면 이전 단계에 합친다.
        if (mergeKey != null && last != null && mergeKey.equals(last.mergeKey) && last.after.same(step.before)) {
            last.records.addAll(step.records);
            last.after = step.after;
            return;
        }
        step.mergeKey = mergeKey;
        undoSteps.add(step);
        // 보관 한도를 넘으면 가장 오래된 단계를 버린다.
        while (undoSteps.size() > LIMIT) {
            undoSteps.remove(0);
        }
    }

    /**
     * Ends the merging: the next step starts a new step of its own (called when something other than typing happens, for example a click).
     * <p>
     * 병합을 끝낸다: 다음 단계는 자기만의 새 단계가 된다(클릭처럼 글자 입력이 아닌 일이 생겼을 때 부른다).
     */
    public void breakMerging() {
        if (!undoSteps.isEmpty()) {
            undoSteps.get(undoSteps.size() - 1).mergeKey = null;
        }
    }

    /**
     * Tells whether there is something to undo.
     * <p>
     * 되돌릴 것이 있는지 알려 준다.
     *
     * @return true if there is / 있으면 true
     */
    public boolean canUndo() {
        return !undoSteps.isEmpty();
    }

    /**
     * Tells whether there is something to redo.
     * <p>
     * 다시 할 것이 있는지 알려 준다.
     *
     * @return true if there is / 있으면 true
     */
    public boolean canRedo() {
        return !redoSteps.isEmpty();
    }

    /**
     * Forgets the whole history (the document was replaced or a script rewrote it).
     * <p>
     * 이력 전체를 잊는다(문서가 교체되었거나 스크립트가 다시 썼다).
     */
    public void clear() {
        undoSteps.clear();
        redoSteps.clear();
    }

    /**
     * Undoes the newest step and puts the selection that was before it back.
     * <p>
     * 가장 최근 단계를 되돌리고 그 전의 선택을 되돌려 놓는다.
     *
     * @return true if a step was undone / 되돌렸으면 true
     */
    public boolean undo() {
        return replay(undoSteps, redoSteps);
    }

    /**
     * Redoes the step that was undone last and puts the selection that was after it back.
     * <p>
     * 마지막으로 되돌린 단계를 다시 하고 그 뒤의 선택을 되돌려 놓는다.
     *
     * @return true if a step was redone / 다시 했으면 true
     */
    public boolean redo() {
        return replay(redoSteps, undoSteps);
    }

    /**
     * Applies the inverse of the newest step of a list and records the inverse mutations as a step of the other list.
     * <p>
     * 한 목록의 가장 최근 단계의 역을 적용하고 그 역변경을 다른 목록의 한 단계로 기록한다.
     *
     * @param from the list that the step is taken from / 단계를 꺼내는 목록
     * @param to   the list that receives the inverse step / 역 단계를 받는 목록
     * @return true if a step was replayed / 되돌렸으면 true
     */
    private boolean replay(List<Step> from, List<Step> to) {
        if (from.isEmpty() || current != null) {
            return false;
        }
        Step step = from.remove(from.size() - 1);
        Step inverse = new Step();
        inverse.before = step.after;
        inverse.after = step.before;
        current = inverse;
        doc.addMutationListener(this);
        try {
            // 기록된 변경을 거꾸로 하나씩 되돌린다.
            for (int i = step.records.size() - 1; i >= 0; i--) {
                invert(step.records.get(i));
            }
        } finally {
            current = null;
            doc.removeMutationListener(this);
        }
        // 되돌린 결과의 선택은 어느 방향이든 단계 앞의 선택이다(다시 하기의 단계는 되돌린 단계의 "뒤" 선택을 앞에 가지고 있다).
        step.before.restore(doc.getSelection());
        to.add(inverse);
        return true;
    }

    /**
     * Applies the inverse of one mutation.
     * <p>
     * 변경 하나의 역을 적용한다.
     *
     * @param r the mutation / 변경
     */
    private static void invert(MutationRecord r) {
        Node target = r.getTarget();
        switch (r.getType()) {
        case MutationRecord.CHARACTER_DATA:
            ((CharacterData) target).setData(r.getOldValue());
            break;
        case MutationRecord.ATTRIBUTES:
            // 옛 값이 없었으면 속성을 지우고, 있었으면 되돌려 쓴다.
            if (r.getOldValue() == null) {
                ((Element) target).removeAttribute(r.getAttributeName());
            } else {
                ((Element) target).setAttribute(r.getAttributeName(), r.getOldValue());
            }
            break;
        default:
            List<Node> added = r.getAddedNodes();
            // 더해진 노드를 먼저 지운다.
            for (int i = 0; i < added.size(); i++) {
                Node n = added.get(i);
                if (n.getParentNode() == target) {
                    target.removeChild(n);
                }
            }
            Node next = r.getNextSibling();
            List<Node> removed = r.getRemovedNodes();
            // 제거되었던 노드를 원래 자리(다음 형제 앞, 없으면 끝)에 되돌려 놓는다.
            for (int i = 0; i < removed.size(); i++) {
                target.insertBefore(removed.get(i), next != null && next.getParentNode() == target ? next : null);
            }
            break;
        }
    }
}
