package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.dom.DomException;

/**
 * The two operations that edit a rule list of the CSSOM (CSSOM section 6.4.1, "insert a CSS rule" and "remove a CSS rule"), shared by style sheets and
 * group rules: the index is checked, the text is parsed as exactly one rule in the context of the sheet (its namespaces, its base URL, its loader of
 * imports), the rules that CSS allows only at the start of a sheet (<code>@import</code>, <code>@namespace</code>) are checked against their position,
 * and the rule list, the container links and the change notification are updated together.
 * <p>
 * CSSOM 의 규칙 목록을 편집하는 두 연산(CSSOM 6.4.1 절, "insert a CSS rule" 와 "remove a CSS rule")으로, 스타일 시트와 그룹 규칙이 공유한다: 위치를 검사하고, 텍스트를
 * 시트의 문맥(네임스페이스, 기준 URL, 가져오기 로더)에서 규칙 정확히 하나로 파싱하고, CSS 가 시트의 시작에서만 허용하는 규칙(<code>@import</code>,
 * <code>@namespace</code>)은 위치를 검사하며, 규칙 목록과 컨테이너 링크와 변경 통지를 함께 갱신한다.
 */
final class RuleEditor {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RuleEditor() {
    }

    /**
     * Tells whether a rule may precede <code>@import</code> and <code>@namespace</code> rules: an <code>@import</code>, an <code>@namespace</code> or a
     * <code>@layer</code> statement.
     * <p>
     * 규칙이 <code>@import</code>, <code>@namespace</code> 규칙보다 앞설 수 있는지 알려 준다: <code>@import</code>, <code>@namespace</code>, <code>@layer</code> 문장.
     *
     * @param r the rule / 규칙
     * @return true if it belongs to the prologue of a sheet / 시트의 서문에 속하면 true
     */
    private static boolean isPrologue(CssRule r) {
        return r instanceof ImportRule || r instanceof NamespaceRule || (r instanceof GroupRule && ((GroupRule) r).isLayerStatement());
    }

    /**
     * Inserts a rule given as text into a rule list.
     * <p>
     * 텍스트로 주어진 규칙을 규칙 목록에 넣는다.
     *
     * @param sheet  the sheet that contains the list, or null / 목록을 담은 시트. 없으면 null
     * @param parent the rule that owns the list, or null for the list of the sheet / 목록을 가진 규칙. 시트의 목록이면 null
     * @param list   the (mutable) rule list / (수정 가능한) 규칙 목록
     * @param text   the rule text / 규칙 텍스트
     * @param index  the position / 위치
     * @return the position / 위치
     * @throws DomException <code>IndexSizeError</code>, <code>SyntaxError</code>, <code>HierarchyRequestError</code> or <code>InvalidStateError</code> / <code>IndexSizeError</code>, <code>SyntaxError</code>, <code>HierarchyRequestError</code>, <code>InvalidStateError</code>
     */
    static int insert(StyleSheet sheet, CssRule parent, List<CssRule> list, String text, int index) {
        if (index < 0 || index > list.size()) {
            throw DomException.indexSize("The index " + index + " is larger than the number of rules (" + list.size() + ").");
        }
        CssRule rule = StyleSheetBuilder.forEditing(sheet, null).parseOne(text == null ? "" : text, parent == null);
        boolean prologueRule = rule instanceof ImportRule || rule instanceof NamespaceRule;
        if (rule instanceof ImportRule && sheet != null && sheet.isConstructed()) {
            throw DomException.syntax("@import rules are not allowed in a constructed style sheet.");
        }
        // 가져오기와 네임스페이스 규칙은 앞에 서문 규칙만 있어야 하고, 그 밖의 규칙은 서문 규칙 앞에 넣을 수 없다.
        if (prologueRule) {
            for (int i = 0; i < index; i++) {
                CssRule before = list.get(i);
                if (rule instanceof ImportRule ? !(before instanceof ImportRule || isPrologueLayer(before)) : !isPrologue(before)) {
                    throw DomException.hierarchyRequest("The rule cannot be inserted at this position.");
                }
            }
            // 네임스페이스 규칙은 목록에 서문 규칙 이외의 규칙이 하나라도 있으면 넣을 수 없다.
            for (int i = 0; rule instanceof NamespaceRule && i < list.size(); i++) {
                if (!isPrologue(list.get(i))) {
                    throw DomException.invalidState("@namespace rules cannot be inserted into a style sheet that has other rules.");
                }
            }
        } else if (!(rule instanceof GroupRule && ((GroupRule) rule).isLayerStatement())) {
            // 다른 규칙은 뒤에 서문 규칙(가져오기, 네임스페이스)이 있는 위치에 넣을 수 없다.
            for (int i = index; i < list.size(); i++) {
                if (list.get(i) instanceof ImportRule || list.get(i) instanceof NamespaceRule) {
                    throw DomException.hierarchyRequest("The rule cannot be inserted before an @import or @namespace rule.");
                }
            }
        }
        list.add(index, rule);
        rule.attach(sheet, parent);
        if (rule instanceof NamespaceRule && sheet != null) {
            // 새 접두사가 이후에 넣을 규칙의 선택자에서 쓰이도록 시트의 네임스페이스 선언에 알린다.
            sheet.getNamespaces().declare(((NamespaceRule) rule).getPrefix(), ((NamespaceRule) rule).getNamespaceUri());
        }
        if (sheet != null) {
            sheet.markModified();
            sheet.changed();
        }
        return index;
    }

    /**
     * Tells whether a rule is a <code>@layer</code> statement.
     * <p>
     * 규칙이 <code>@layer</code> 문장인지 알려 준다.
     *
     * @param r the rule / 규칙
     * @return true for a layer statement / 레이어 문장이면 true
     */
    private static boolean isPrologueLayer(CssRule r) {
        return r instanceof GroupRule && ((GroupRule) r).isLayerStatement();
    }

    /**
     * Deletes the rule at an index of a rule list.
     * <p>
     * 규칙 목록의 위치에 있는 규칙을 지운다.
     *
     * @param sheet the sheet that contains the list, or null / 목록을 담은 시트. 없으면 null
     * @param list  the (mutable) rule list / (수정 가능한) 규칙 목록
     * @param index the position / 위치
     * @throws DomException <code>IndexSizeError</code> or <code>InvalidStateError</code> / <code>IndexSizeError</code> 또는 <code>InvalidStateError</code>
     */
    static void delete(StyleSheet sheet, List<CssRule> list, int index) {
        if (index < 0 || index >= list.size()) {
            throw DomException.indexSize("The index " + index + " is not smaller than the number of rules (" + list.size() + ").");
        }
        CssRule rule = list.get(index);
        // 네임스페이스 규칙은 시트에 서문 규칙 이외의 규칙이 있으면 지울 수 없다.
        for (int i = 0; rule instanceof NamespaceRule && i < list.size(); i++) {
            if (!isPrologue(list.get(i))) {
                throw DomException.invalidState("@namespace rules cannot be deleted from a style sheet that has other rules.");
            }
        }
        list.remove(index);
        rule.detach();
        if (sheet != null) {
            sheet.markModified();
            sheet.changed();
        }
    }
}
