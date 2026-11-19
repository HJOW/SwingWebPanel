package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The contents of a declaration block: the declarations and the at-rules found inside it (for example the margin at-rules of
 * <code>@page</code>). Qualified rules inside a declaration block are not part of the supported grammar and are dropped by the parser.
 * <p>
 * 선언 블록의 내용: 그 안의 선언들과 at-규칙들(예: <code>@page</code> 의 여백 at-규칙). 선언 블록 안의 한정 규칙은 지원하는 문법이 아니므로
 * 파서가 버린다.
 */
public final class DeclarationBlock {

    /** Declarations in source order. / 소스 순서의 선언. */
    private final List<SyntaxDeclaration> declarations;

    /** At-rules in source order. / 소스 순서의 at-규칙. */
    private final List<SyntaxRule> rules;

    /**
     * Creates the contents.
     * <p>
     * 내용을 만든다.
     *
     * @param declarations declarations / 선언
     * @param rules        at-rules / at-규칙
     */
    DeclarationBlock(List<SyntaxDeclaration> declarations, List<SyntaxRule> rules) {
        this.declarations = Collections.unmodifiableList(new ArrayList<SyntaxDeclaration>(declarations));
        this.rules = Collections.unmodifiableList(new ArrayList<SyntaxRule>(rules));
    }

    /**
     * Returns the declarations.
     * <p>
     * 선언을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<SyntaxDeclaration> getDeclarations() {
        return declarations;
    }

    /**
     * Returns the at-rules.
     * <p>
     * at-규칙을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<SyntaxRule> getRules() {
        return rules;
    }
}
