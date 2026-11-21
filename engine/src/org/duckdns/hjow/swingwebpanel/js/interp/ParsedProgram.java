package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.ParsedScript;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;

/**
 * The parse tree of a script together with its name: the implementation of {@link ParsedScript}.
 * <p>
 * 스크립트의 구문 트리와 이름: {@link ParsedScript} 의 구현.
 */
final class ParsedProgram implements ParsedScript {

    /**
     * The PROGRAM node.
     * <p>
     * PROGRAM 노드.
     */
    final Node program;

    /**
     * The source name used in errors.
     * <p>
     * 오류에 쓰는 소스 이름.
     */
    final String name;

    /**
     * Creates the object.
     * <p>
     * 객체를 만든다.
     *
     * @param program the PROGRAM node / PROGRAM 노드
     * @param name    the source name, null is stored as the empty string / 소스 이름. null 이면 빈 문자열로 저장한다
     */
    ParsedProgram(Node program, String name) {
        this.program = program;
        this.name = name == null ? "" : name;
    }

    /**
     * Returns the source name.
     * <p>
     * 소스 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getSourceName() {
        return name;
    }
}
