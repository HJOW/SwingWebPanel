package org.duckdns.hjow.swingwebpanel.js.ast;

/**
 * A syntax tree node. One generic class represents every construct; the meaning of the child fields depends on the
 * {@link NodeType} constant in {@link #type} (see the documentation of each constant). Nodes are created by the parser and
 * read by the interpreter without changes after parsing.
 * <p>
 * 구문 트리 노드. 모든 구문을 하나의 범용 클래스로 나타내며, 자식 필드의 의미는 {@link #type} 의 {@link NodeType} 상수에 따라 달라진다
 * (각 상수의 설명 참조). 노드는 파서가 만들며, 인터프리터는 파싱이 끝난 노드를 변경하지 않고 읽는다.
 */
public final class Node {

    /**
     * The node kind, a {@link NodeType} constant.
     * <p>
     * 노드 종류. {@link NodeType} 상수.
     */
    public final int type;

    /**
     * 1-based source line of the first token.
     * <p>
     * 첫 토큰의 소스 줄 번호(1부터).
     */
    public int line;

    /**
     * 1-based source column of the first token.
     * <p>
     * 첫 토큰의 소스 열 번호(1부터).
     */
    public int column;

    /**
     * Offset of the first character in the source.
     * <p>
     * 소스에서 첫 글자의 오프셋.
     */
    public int start;

    /**
     * Offset after the last character in the source.
     * <p>
     * 소스에서 마지막 글자 다음의 오프셋.
     */
    public int end;

    /**
     * First child node (meaning depends on the type).
     * <p>
     * 첫 번째 자식 노드(의미는 종류에 따라 다르다).
     */
    public Node a;

    /**
     * Second child node.
     * <p>
     * 두 번째 자식 노드.
     */
    public Node b;

    /**
     * Third child node.
     * <p>
     * 세 번째 자식 노드.
     */
    public Node c;

    /**
     * Fourth child node.
     * <p>
     * 네 번째 자식 노드.
     */
    public Node d;

    /**
     * Child node list (statements, arguments, elements ...).
     * <p>
     * 자식 노드 목록(문장, 인수, 요소 등).
     */
    public Node[] list;

    /**
     * A name: identifier, property name, label, module specifier or regular expression pattern.
     * <p>
     * 이름: 식별자, 프로퍼티 이름, 레이블, 모듈 지정자, 정규식 패턴.
     */
    public String name;

    /**
     * A payload: literal value, template parts, regular expression flags, program information ...
     * <p>
     * 부가 정보: 리터럴 값, 템플릿 조각, 정규식 플래그, 프로그램 정보 등.
     */
    public Object value;

    /**
     * An operator or kind code, see {@link Op}.
     * <p>
     * 연산자 또는 종류 코드. {@link Op} 참조.
     */
    public int op;

    /**
     * A boolean attribute whose meaning depends on the type (computed, prefix, static, delegate ...).
     * <p>
     * 종류에 따라 의미가 달라지는 불리언 속성(계산된 키, 전위, static, 위임 등).
     */
    public boolean flag;

    /**
     * True if the expression was written in parentheses (matters for assignment targets and arrow functions).
     * <p>
     * 식이 괄호 안에 쓰였으면 true(대입 대상과 화살표 함수에서 의미가 있다).
     */
    public boolean paren;

    /**
     * True for an optional call or member access. The enclosing {@link NodeType#OPTIONAL_CHAIN} node defines the
     * short-circuit scope.
     * <p>
     * 선택적 호출이나 멤버 접근이면 true. 바깥 {@link NodeType#OPTIONAL_CHAIN} 노드가 단락 평가 범위를 정한다.
     */
    public boolean optional;

    /**
     * True for a private-name key or member access.
     * <p>
     * private 이름 키나 멤버 접근이면 true.
     */
    public boolean privateName;

    /**
     * True if this node contains a yield expression outside nested functions (used to resume generators).
     * <p>
     * 중첩 함수 밖에 yield 식을 포함하면 true(제너레이터 재개에 쓴다).
     */
    public boolean hasYield;

    /**
     * The function description for FUNCTION nodes.
     * <p>
     * FUNCTION 노드의 함수 설명.
     */
    public FunctionInfo fn;

    /**
     * The scope description for nodes that open a scope.
     * <p>
     * 스코프를 여는 노드의 스코프 설명.
     */
    public ScopeInfo scope;

    /**
     * Creates a node.
     * <p>
     * 노드를 만든다.
     *
     * @param type   the {@link NodeType} constant / {@link NodeType} 상수
     * @param line   source line / 소스 줄
     * @param column source column / 소스 열
     */
    public Node(int type, int line, int column) {
        this.type = type;
        this.line = line;
        this.column = column;
    }

    /**
     * Returns a short description for debugging.
     * <p>
     * 디버깅용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        return "Node[" + type + (name != null ? " " + name : "") + " @" + line + ":" + column + "]";
    }
}
