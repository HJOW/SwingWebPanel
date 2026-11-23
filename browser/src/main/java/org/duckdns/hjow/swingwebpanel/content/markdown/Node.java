package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * A node of the Markdown syntax tree. The tree is a doubly linked list of children per parent (so inline processing can move and remove
 * nodes cheaply) and every traversal in this package is iterative, so deeply nested input cannot overflow the Java stack. The fields are
 * package-private on purpose: the block parser, the inline parser and the renderer are the only users and they treat the node as a
 * plain record.
 * <p>
 * Markdown 구문 트리의 노드. 트리는 부모마다 이중 연결 리스트로 자식을 가지므로(인라인 처리가 노드를 싸게 옮기고 지울 수 있다) 이 패키지의 모든
 * 순회는 반복형이며 깊게 중첩된 입력도 Java 스택을 넘치게 하지 못한다. 필드는 일부러 패키지 비공개이다: 블록 파서, 인라인 파서, 렌더러만 쓰며
 * 노드를 단순한 레코드로 다룬다.
 */
final class Node {

    /** Kind of the node. 노드 종류. */
    final NodeType type;

    /** Parent node or null for the root. 부모 노드. 루트면 null. */
    Node parent;

    /** First child or null. 첫 자식 또는 null. */
    Node firstChild;

    /** Last child or null. 마지막 자식 또는 null. */
    Node lastChild;

    /** Previous sibling or null. 이전 형제 또는 null. */
    Node prev;

    /** Next sibling or null. 다음 형제 또는 null. */
    Node next;

    /** Literal text of text, code, code block, HTML and similar nodes. 텍스트, 코드, 코드 블록, HTML 등의 글자 내용. */
    String literal;

    /** Link or image destination (already URL-normalized). 링크/이미지 목적지(이미 URL 정규화됨). */
    String destination;

    /** Link or image title; empty if none. 링크/이미지 제목. 없으면 빈 문자열. */
    String title;

    /** Info string of a fenced code block. 울타리 코드 블록의 정보 문자열. */
    String info;

    /** Heading level 1 to 6. 제목 수준 1~6. */
    int level;

    /** Raw content collected line by line while a block is open (paragraph, heading, code, HTML, table cell). 블록이 열려 있는 동안 줄 단위로 모으는 원시 내용. */
    StringBuilder content;

    /** True while the block can still receive lines. 블록이 아직 줄을 받을 수 있으면 true. */
    boolean open = true;

    /** True if the last line of this block was blank (used for tight/loose lists). 이 블록의 마지막 줄이 빈 줄이었으면 true(촘촘한/느슨한 목록 판정용). */
    boolean lastLineBlank;

    /** True once {@link #lastLineBlank} was inspected through the list. 목록을 따라 lastLineBlank 를 이미 살폈으면 true. */
    boolean lastLineChecked;

    /** Nesting depth of container blocks (the document is 0). 컨테이너 블록 중첩 깊이(문서는 0). */
    int depth;

    /** Line number at which the block started (1-based). 블록이 시작한 줄 번호(1부터). */
    int startLine;

    /** List data of a list or list item; null for other nodes. 목록/목록 항목의 목록 정보. 다른 노드는 null. */
    ListData list;

    /** True if the code block is fenced. 코드 블록이 울타리형이면 true. */
    boolean fenced;

    /** Fence character of a fenced code block. 울타리 코드 블록의 울타리 문자. */
    char fenceChar;

    /** Fence length of a fenced code block. 울타리 코드 블록의 울타리 길이. */
    int fenceLength;

    /** Indentation of the opening fence. 여는 울타리의 들여쓰기. */
    int fenceOffset;

    /** HTML block start condition 1 to 7. HTML 블록 시작 조건 1~7. */
    int htmlBlockType;

    /** Column alignments of a table: 0 none, 1 left, 2 center, 3 right. 표의 열 정렬: 0 없음, 1 왼쪽, 2 가운데, 3 오른쪽. */
    int[] aligns;

    /** True for the header row of a table (on a row) or a header cell. 표의 머리 행(행) 또는 머리 셀이면 true. */
    boolean header;

    /** Alignment of a table cell (same codes as {@link #aligns}). 표 셀의 정렬(aligns 와 같은 값). */
    int align;

    /** Task list state of a list item: 0 none, 1 unchecked, 2 checked. 목록 항목의 작업 상태: 0 없음, 1 미체크, 2 체크. */
    int task;

    /** Height of the inline subtree (0 for leaves), used to limit nesting. 인라인 하위 트리의 높이(잎은 0). 중첩 제한에 쓴다. */
    int height;

    /**
     * Data of a list or a list item.
     * <p>
     * 목록 또는 목록 항목의 정보.
     */
    static final class ListData {
        /** True for an ordered list. 순서 있는 목록이면 true. */
        boolean ordered;

        /** True if the list is tight. 촘촘한 목록이면 true. */
        boolean tight = true;

        /** Bullet character of a bullet list. 순서 없는 목록의 글머리 문자. */
        char bulletChar;

        /** Start number of an ordered list. 순서 있는 목록의 시작 번호. */
        int start;

        /** Delimiter of an ordered list: '.' or ')'. 순서 있는 목록의 구분 문자: '.' 또는 ')'. */
        char delimiter;

        /** Width of the marker plus the following spaces. 표식과 뒤따르는 공백의 너비. */
        int padding;

        /** Indentation of the marker. 표식의 들여쓰기. */
        int markerOffset;
    }

    /**
     * Creates a node.
     * <p>
     * 노드를 만든다.
     *
     * @param type kind of the node / 노드 종류
     */
    Node(NodeType type) {
        this.type = type;
    }

    /**
     * Creates a text node.
     * <p>
     * 텍스트 노드를 만든다.
     *
     * @param text the text / 텍스트
     * @return the node / 노드
     */
    static Node text(String text) {
        Node n = new Node(NodeType.TEXT);
        n.literal = text;
        return n;
    }

    /**
     * Appends a child at the end.
     * <p>
     * 자식을 끝에 붙인다.
     *
     * @param child the child, which must not have a parent / 자식(부모가 없어야 한다)
     */
    void appendChild(Node child) {
        child.unlink();
        child.parent = this;
        if (lastChild != null) {
            lastChild.next = child;
            child.prev = lastChild;
            lastChild = child;
        } else {
            firstChild = child;
            lastChild = child;
        }
    }

    /**
     * Inserts a sibling right after this node.
     * <p>
     * 이 노드 바로 뒤에 형제를 끼워 넣는다.
     *
     * @param sibling the node to insert / 끼워 넣을 노드
     */
    void insertAfter(Node sibling) {
        sibling.unlink();
        sibling.next = next;
        if (sibling.next != null) {
            sibling.next.prev = sibling;
        }
        sibling.prev = this;
        next = sibling;
        sibling.parent = parent;
        if (sibling.next == null && parent != null) {
            parent.lastChild = sibling;
        }
    }

    /**
     * Removes this node from its parent (its children stay attached to it).
     * <p>
     * 이 노드를 부모에서 떼어 낸다(자식은 이 노드에 붙어 있다).
     */
    void unlink() {
        if (prev != null) {
            prev.next = next;
        } else if (parent != null) {
            parent.firstChild = next;
        }
        if (next != null) {
            next.prev = prev;
        } else if (parent != null) {
            parent.lastChild = prev;
        }
        parent = null;
        next = null;
        prev = null;
    }

    /**
     * Tells whether the node can have children in the output (so the renderer reports enter and exit events).
     * <p>
     * 출력에서 자식을 가질 수 있는 노드인지 알려 준다(렌더러가 진입/종료 이벤트를 보고하는 노드).
     *
     * @return true for container nodes / 컨테이너 노드이면 true
     */
    boolean isContainer() {
        switch (type) {
            case DOCUMENT:
            case BLOCK_QUOTE:
            case LIST:
            case ITEM:
            case PARAGRAPH:
            case HEADING:
            case TABLE:
            case TABLE_ROW:
            case TABLE_CELL:
            case EMPH:
            case STRONG:
            case DEL:
            case LINK:
            case IMAGE:
                return true;
            default:
                return false;
        }
    }
}
