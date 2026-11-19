package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The selection of a document (W3C Selection API, <code>Selection</code>): none or one {@link Range} and a direction, so that the
 * <em>anchor</em> (where the selection started) and the <em>focus</em> (where it ends now) can be told apart from the range's start and end.
 * This is the model only: the screen (marking the selected text, mouse and keyboard selection, the clipboard) is connected by the component
 * (plan item P11-08). The range of the selection is live, like every {@link Range}; scripts can read it with {@link #getRangeAt(int)} and
 * change it directly. Only one range is supported, as in the browsers that follow the specification (<code>rangeCount</code> is 0 or 1).
 * <p>
 * 문서의 선택(W3C Selection API 의 <code>Selection</code>): 없거나 {@link Range} 하나와 방향으로 이루어지며, 선택이 시작한 곳인 <em>anchor</em> 와
 * 지금 끝나는 곳인 <em>focus</em> 를 범위의 시작과 끝과 구별한다. 모델만 담당한다: 화면(선택한 텍스트 표시, 마우스와 키보드 선택, 클립보드)은
 * 컴포넌트가 연결한다(계획 P11-08). 선택의 범위는 모든 {@link Range} 처럼 살아 있으며 스크립트가 {@link #getRangeAt(int)} 로 읽고 직접 바꿀 수 있다.
 * 규격을 따르는 브라우저처럼 범위는 하나만 지원한다(<code>rangeCount</code> 는 0 또는 1).
 */
public final class Selection {

    /**
     * The document that the selection belongs to.
     * <p>
     * 선택이 속한 문서.
     */
    private final Document document;

    /**
     * The range of the selection, or null if the selection is empty.
     * <p>
     * 선택의 범위. 선택이 비어 있으면 null.
     */
    private Range range;

    /**
     * True if the focus is before the anchor.
     * <p>
     * focus 가 anchor 보다 앞이면 true.
     */
    private boolean backwards;

    /**
     * The listener that is told about every change of the selection, or null.
     * <p>
     * 선택이 바뀔 때마다 알리는 리스너. 없으면 null.
     */
    private volatile SelectionListener listener;

    /**
     * Creates an empty selection for a document.
     * <p>
     * 문서의 빈 선택을 만든다.
     *
     * @param document the document, not null / 문서(null 불가)
     */
    Selection(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        this.document = document;
    }

    /**
     * Sets the listener that is told about every change of the selection (null removes it). The component that shows the page uses it to
     * draw the highlight of the selection and to fire <code>selectionchange</code>.
     * <p>
     * 선택이 바뀔 때마다 알리는 리스너를 지정한다(null 이면 제거). 페이지를 보여 주는 컴포넌트가 이것으로 선택 강조를 그리고
     * <code>selectionchange</code> 를 보낸다.
     *
     * @param listener the listener or null / 리스너. 없으면 null
     */
    public void setListener(SelectionListener listener) {
        this.listener = listener;
    }

    /**
     * Tells the listener that the selection changed.
     * <p>
     * 선택이 바뀌었음을 리스너에 알린다.
     */
    private void changed() {
        SelectionListener l = listener;
        // 리스너가 있을 때만 알린다.
        if (l != null) {
            l.selectionChanged(this);
        }
    }

    /**
     * Called by the range of the selection when one of its boundary points moved (a script changed the live range object, or the tree
     * changed under it).
     * <p>
     * 선택의 범위가 경계점이 움직였을 때 부른다(스크립트가 살아 있는 범위 객체를 바꿨거나 그 아래의 트리가 바뀌었다).
     *
     * @param moved the range that moved / 움직인 범위
     */
    void rangeChanged(Range moved) {
        if (moved == range) {
            changed();
        }
    }

    /**
     * Makes a range the range of the selection (the old range stops being tied to it) and tells the listener.
     * <p>
     * 범위를 선택의 범위로 삼고(옛 범위는 선택과의 연결이 끊긴다) 리스너에 알린다.
     *
     * @param newRange the range, not null / 범위(null 불가)
     * @param back     true if the focus is before the anchor / focus 가 anchor 보다 앞이면 true
     */
    private void use(Range newRange, boolean back) {
        if (range != null && range != newRange) {
            range.untie(this);
        }
        range = newRange;
        backwards = back;
        newRange.tie(this);
        changed();
    }

    /**
     * Empties the selection and tells the listener if it was not empty.
     * <p>
     * 선택을 비우고, 비어 있지 않았으면 리스너에 알린다.
     */
    private void drop() {
        if (range != null) {
            range.untie(this);
            range = null;
            changed();
        }
    }

    /**
     * Returns the document of the selection.
     * <p>
     * 선택의 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the node where the selection started.
     * <p>
     * 선택이 시작한 노드를 반환한다.
     *
     * @return the anchor node, or null for an empty selection / anchor 노드. 선택이 비어 있으면 null
     */
    public Node getAnchorNode() {
        return range == null ? null : (backwards ? range.getEndContainer() : range.getStartContainer());
    }

    /**
     * Returns the offset where the selection started.
     * <p>
     * 선택이 시작한 오프셋을 반환한다.
     *
     * @return the anchor offset, 0 for an empty selection / anchor 오프셋. 선택이 비어 있으면 0
     */
    public int getAnchorOffset() {
        return range == null ? 0 : (backwards ? range.getEndOffset() : range.getStartOffset());
    }

    /**
     * Returns the node where the selection ends now.
     * <p>
     * 선택이 지금 끝나는 노드를 반환한다.
     *
     * @return the focus node, or null for an empty selection / focus 노드. 선택이 비어 있으면 null
     */
    public Node getFocusNode() {
        return range == null ? null : (backwards ? range.getStartContainer() : range.getEndContainer());
    }

    /**
     * Returns the offset where the selection ends now.
     * <p>
     * 선택이 지금 끝나는 오프셋을 반환한다.
     *
     * @return the focus offset, 0 for an empty selection / focus 오프셋. 선택이 비어 있으면 0
     */
    public int getFocusOffset() {
        return range == null ? 0 : (backwards ? range.getStartOffset() : range.getEndOffset());
    }

    /**
     * Tells whether the selection is empty or collapsed to a point.
     * <p>
     * 선택이 비어 있거나 한 점으로 접혀 있는지 알려 준다.
     *
     * @return true if there is no range or it is collapsed / 범위가 없거나 접혀 있으면 true
     */
    public boolean isCollapsed() {
        return range == null || range.isCollapsed();
    }

    /**
     * Returns the number of ranges (0 or 1).
     * <p>
     * 범위 수(0 또는 1)를 반환한다.
     *
     * @return the number of ranges / 범위 수
     */
    public int getRangeCount() {
        return range == null ? 0 : 1;
    }

    /**
     * Returns the kind of the selection: <code>None</code>, <code>Caret</code> or <code>Range</code>.
     * <p>
     * 선택의 종류를 반환한다: <code>None</code>, <code>Caret</code>, <code>Range</code>.
     *
     * @return the type name / 종류 이름
     */
    public String getType() {
        if (range == null) {
            return "None";
        }
        return range.isCollapsed() ? "Caret" : "Range";
    }

    /**
     * Returns the direction of the selection: <code>none</code> for an empty or collapsed selection, otherwise <code>forward</code> or
     * <code>backward</code>.
     * <p>
     * 선택의 방향을 반환한다: 비어 있거나 접힌 선택은 <code>none</code>, 아니면 <code>forward</code> 또는 <code>backward</code>.
     *
     * @return the direction name / 방향 이름
     */
    public String getDirection() {
        if (range == null || range.isCollapsed()) {
            return "none";
        }
        return backwards ? "backward" : "forward";
    }

    /**
     * Returns a range of the selection (the live range object itself).
     * <p>
     * 선택의 범위를 반환한다(살아 있는 범위 객체 자체).
     *
     * @param index the index (only 0 exists) / 인덱스(0 만 있다)
     * @return the range / 범위
     * @throws DomException IndexSizeError if there is no such range / 그런 범위가 없으면 IndexSizeError
     */
    public Range getRangeAt(int index) {
        if (index != 0 || range == null) {
            throw DomException.indexSize("the selection has no range at index " + index);
        }
        return range;
    }

    /**
     * Makes a range the range of the selection, unless the selection has one already or the range is not in the document of the selection
     * (then nothing happens).
     * <p>
     * 범위를 선택의 범위로 삼는다. 선택에 이미 범위가 있거나 범위가 선택의 문서 안에 없으면 아무 일도 일어나지 않는다.
     *
     * @param newRange the range / 범위
     */
    public void addRange(Range newRange) {
        if (newRange == null) {
            throw new IllegalArgumentException("range must not be null");
        }
        if (newRange.getStartContainer().getRootNode() != document || range != null) {
            return;
        }
        use(newRange, false);
    }

    /**
     * Removes a range from the selection.
     * <p>
     * 선택에서 범위를 제거한다.
     *
     * @param old the range / 범위
     * @throws DomException NotFoundError if the range is not the range of the selection / 선택의 범위가 아니면 NotFoundError
     */
    public void removeRange(Range old) {
        if (old == null || old != range) {
            throw DomException.notFound("the range is not in the selection");
        }
        drop();
    }

    /**
     * Empties the selection.
     * <p>
     * 선택을 비운다.
     */
    public void removeAllRanges() {
        drop();
    }

    /**
     * Collapses the selection to a point (null empties it).
     * <p>
     * 선택을 한 점으로 접는다(null 이면 비운다).
     *
     * @param node   the node, or null / 노드. 없으면 null
     * @param offset the offset / 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    public void collapse(Node node, int offset) {
        if (node == null) {
            drop();
            return;
        }
        checkPoint(node, offset);
        if (node.getRootNode() != document) {
            return;
        }
        Range r = new Range(document);
        r.setStart(node, offset);
        r.setEnd(node, offset);
        use(r, false);
    }

    /**
     * Collapses the selection to its start.
     * <p>
     * 선택을 시작으로 접는다.
     *
     * @throws DomException InvalidStateError for an empty selection / 선택이 비어 있으면 InvalidStateError
     */
    public void collapseToStart() {
        requireRange();
        collapse(range.getStartContainer(), range.getStartOffset());
    }

    /**
     * Collapses the selection to its end.
     * <p>
     * 선택을 끝으로 접는다.
     *
     * @throws DomException InvalidStateError for an empty selection / 선택이 비어 있으면 InvalidStateError
     */
    public void collapseToEnd() {
        requireRange();
        collapse(range.getEndContainer(), range.getEndOffset());
    }

    /**
     * Moves the focus of the selection to a point; the anchor stays.
     * <p>
     * 선택의 focus 를 한 점으로 옮긴다. anchor 는 그대로이다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @throws DomException InvalidStateError for an empty selection, InvalidNodeTypeError, IndexSizeError / 선택이 비어 있으면 InvalidStateError,
     *                      InvalidNodeTypeError, IndexSizeError
     */
    public void extend(Node node, int offset) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        checkPoint(node, offset);
        if (node.getRootNode() != document) {
            return;
        }
        requireRange();
        Node anchorNode = getAnchorNode();
        int anchorOffset = getAnchorOffset();
        Range r = new Range(document);
        // 새 focus 가 범위와 다른 트리에 있으면 새 focus 한 점으로, 아니면 anchor 와 새 focus 사이로 범위를 다시 만든다.
        boolean before;
        if (node.getRootNode() != anchorNode.getRootNode()) {
            r.setStart(node, offset);
            r.setEnd(node, offset);
            before = false;
        } else if (Range.comparePoints(anchorNode, anchorOffset, node, offset) <= 0) {
            r.setStart(anchorNode, anchorOffset);
            r.setEnd(node, offset);
            before = false;
        } else {
            r.setStart(node, offset);
            r.setEnd(anchorNode, anchorOffset);
            before = true;
        }
        use(r, before);
    }

    /**
     * Selects from an anchor point to a focus point.
     * <p>
     * anchor 점부터 focus 점까지를 선택한다.
     *
     * @param anchorNode   the anchor node / anchor 노드
     * @param anchorOffset the anchor offset / anchor 오프셋
     * @param focusNode    the focus node / focus 노드
     * @param focusOffset  the focus offset / focus 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    public void setBaseAndExtent(Node anchorNode, int anchorOffset, Node focusNode, int focusOffset) {
        if (anchorNode == null || focusNode == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        checkPoint(anchorNode, anchorOffset);
        checkPoint(focusNode, focusOffset);
        if (anchorNode.getRootNode() != document || focusNode.getRootNode() != document) {
            return;
        }
        Range r = new Range(document);
        boolean back;
        // anchor 가 focus 보다 앞이면 정방향, 뒤이면 역방향 선택이다.
        if (Range.comparePoints(anchorNode, anchorOffset, focusNode, focusOffset) <= 0) {
            r.setStart(anchorNode, anchorOffset);
            r.setEnd(focusNode, focusOffset);
            back = false;
        } else {
            r.setStart(focusNode, focusOffset);
            r.setEnd(anchorNode, anchorOffset);
            back = true;
        }
        use(r, back);
    }

    /**
     * Selects everything inside a node.
     * <p>
     * 노드 안의 모든 것을 선택한다.
     *
     * @param node the node / 노드
     * @throws DomException InvalidNodeTypeError for a document type / 문서 유형이면 InvalidNodeTypeError
     */
    public void selectAllChildren(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "a document type has no contents");
        }
        if (node.getRootNode() != document) {
            return;
        }
        Range r = new Range(document);
        r.selectNodeContents(node);
        use(r, false);
    }

    /**
     * Removes the selected content from the document.
     * <p>
     * 선택한 내용을 문서에서 지운다.
     */
    public void deleteFromDocument() {
        if (range != null) {
            range.deleteContents();
        }
    }

    /**
     * Tells whether the selection contains a node. With partial containment any overlap counts; without it the node must lie completely
     * inside the range (a node whose start and end touch the boundary points of the range counts as inside).
     * <p>
     * 선택이 노드를 포함하는지 알려 준다. 부분 포함을 허용하면 조금이라도 겹치면 되고, 아니면 노드가 범위 안에 완전히 있어야 한다(노드의 시작과 끝이
     * 범위의 경계점에 닿아도 안에 있는 것으로 본다).
     *
     * @param node             the node / 노드
     * @param allowPartialContainment true to count a node that only overlaps / 겹치기만 하는 노드도 세려면 true
     * @return true if contained / 포함하면 true
     */
    public boolean containsNode(Node node, boolean allowPartialContainment) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (range == null || node.getRootNode() != document) {
            return false;
        }
        int length = Range.length(node);
        if (allowPartialContainment) {
            return Range.comparePoints(range.getStartContainer(), range.getStartOffset(), node, length) < 0
                    && Range.comparePoints(range.getEndContainer(), range.getEndOffset(), node, 0) > 0;
        }
        return Range.comparePoints(node, 0, range.getStartContainer(), range.getStartOffset()) >= 0
                && Range.comparePoints(node, length, range.getEndContainer(), range.getEndOffset()) <= 0;
    }

    /**
     * Returns the selected text.
     * <p>
     * 선택한 텍스트를 반환한다.
     *
     * @return the text, empty for an empty selection / 텍스트. 선택이 비어 있으면 빈 문자열
     */
    public String toString() {
        return range == null ? "" : range.toString();
    }

    /**
     * Throws InvalidStateError for an empty selection.
     * <p>
     * 선택이 비어 있으면 InvalidStateError 를 던진다.
     */
    private void requireRange() {
        if (range == null) {
            throw DomException.invalidState("the selection has no range");
        }
    }

    /**
     * Checks a point: a document type is not allowed, and the offset must be inside the node.
     * <p>
     * 점을 검사한다: 문서 유형은 안 되며 오프셋이 노드 안에 있어야 한다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @throws DomException InvalidNodeTypeError, IndexSizeError / InvalidNodeTypeError, IndexSizeError
     */
    private static void checkPoint(Node node, int offset) {
        if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            throw new DomException("InvalidNodeTypeError", DomException.INVALID_NODE_TYPE_ERR, "a document type cannot be a boundary point");
        }
        if (offset < 0 || offset > Range.length(node)) {
            throw DomException.indexSize("the offset " + offset + " is larger than the length of the node");
        }
    }
}
