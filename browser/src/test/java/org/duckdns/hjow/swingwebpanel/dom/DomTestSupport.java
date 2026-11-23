package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the DOM tests: tree description for compact assertions, exception checks and a recording listener.
 * <p>
 * DOM 시험들이 공유하는 도우미: 간단히 비교하기 위한 트리 설명, 예외 검사, 기록용 리스너.
 */
public final class DomTestSupport {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DomTestSupport() {
    }

    /**
     * Describes a node and its subtree: elements as <code>name(children)</code> with id, text as quoted text, comments as
     * <code>#c</code>.
     * <p>
     * 노드와 하위 트리를 설명한다: 요소는 <code>이름(자식)</code> 과 id, 텍스트는 따옴표 문자열, 주석은 <code>#c</code>.
     *
     * @param n node / 노드
     * @return description / 설명
     */
    public static String describe(Node n) {
        StringBuilder sb = new StringBuilder();
        append(sb, n);
        return sb.toString();
    }

    /**
     * Appends the description of a node.
     * <p>
     * 노드의 설명을 덧붙인다.
     *
     * @param sb output / 출력
     * @param n  node / 노드
     */
    private static void append(StringBuilder sb, Node n) {
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element e = (Element) n;
            sb.append(e.getLocalName());
            if (e.getId().length() > 0) {
                sb.append('#').append(e.getId());
            }
            break;
        case Node.TEXT_NODE:
            sb.append('"').append(((CharacterData) n).getData()).append('"');
            return;
        case Node.COMMENT_NODE:
            sb.append("#c");
            return;
        case Node.DOCUMENT_TYPE_NODE:
            sb.append("!doctype");
            return;
        case Node.DOCUMENT_FRAGMENT_NODE:
            sb.append("#frag");
            break;
        case Node.DOCUMENT_NODE:
            sb.append("#doc");
            break;
        default:
            sb.append(n.getNodeName());
            return;
        }
        if (n.firstChild != null) {
            sb.append('(');
            // 자식을 쉼표로 구분해 차례로 설명한다.
            for (Node c = n.firstChild; c != null; c = c.nextSibling) {
                if (c != n.firstChild) {
                    sb.append(',');
                }
                append(sb, c);
            }
            sb.append(')');
        }
    }

    /**
     * Checks that a block throws a {@link DomException} with a given name.
     * <p>
     * 코드 조각이 주어진 이름의 {@link DomException} 을 던지는지 확인한다.
     *
     * @param name    expected error name / 기대하는 오류 이름
     * @param block   code to run / 실행할 코드
     * @param message failure description / 실패 설명
     */
    public static void throwsDom(String name, Block block, String message) {
        Throwable t = Check.throwsException(DomException.class, block, message);
        Check.equal(name, ((DomException) t).getName(), message + " (error name)");
    }

    /**
     * A mutation listener that records a short text per record and the connected flag.
     * <p>
     * 기록마다 짧은 문자열과 연결 플래그를 남기는 변경 리스너.
     */
    public static final class RecordingMutations implements DomMutationListener {
        /**
         * Recorded descriptions.
         * <p>
         * 기록된 설명.
         */
        public final List<String> events = new ArrayList<String>();

        /**
         * Records one change.
         * <p>
         * 변경 하나를 기록한다.
         *
         * @param r         record / 기록
         * @param connected connected flag / 연결 플래그
         */
        public void mutated(MutationRecord r, boolean connected) {
            StringBuilder sb = new StringBuilder(r.getTypeName()).append(' ').append(describe(r.getTarget()).split("\\(")[0]);
            if (r.getType() == MutationRecord.CHILD_LIST) {
                sb.append(" +").append(r.getAddedNodes().size()).append(" -").append(r.getRemovedNodes().size());
            } else if (r.getType() == MutationRecord.ATTRIBUTES) {
                sb.append(' ').append(r.getAttributeName()).append(" old=").append(r.getOldValue());
            } else {
                sb.append(" old=").append(r.getOldValue());
            }
            sb.append(connected ? " [connected]" : " [detached]");
            events.add(sb.toString());
        }
    }
}
