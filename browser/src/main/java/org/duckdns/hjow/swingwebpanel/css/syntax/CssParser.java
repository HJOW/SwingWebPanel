package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;

/**
 * The parser of CSS Syntax Module Level 3 (section 5, "Parsing"): it turns text into component values and the component values into rules
 * and declarations, with the error recovery of the specification. A broken declaration is skipped up to the next semicolon, a rule without a
 * block is dropped, an unterminated block is closed at the end of input, and none of this affects the neighbouring declarations and rules.
 * <p>
 * Entry points: {@link #parseStylesheet}, {@link #parseRuleList} (the contents of <code>@media</code>), {@link #parseDeclarationList} (a
 * <code>style</code> attribute), {@link #parseDeclaration}, {@link #parseComponentValues}, {@link #splitCommas}.
 * <p>
 * CSS Syntax Module Level 3 의 파서(5절 "Parsing"): 텍스트를 컴포넌트 값으로, 컴포넌트 값을 규칙과 선언으로 바꾸며 규격의 오류 복구를 따른다.
 * 잘못된 선언은 다음 세미콜론까지 건너뛰고, 블록이 없는 규칙은 버리며, 끝나지 않은 블록은 입력 끝에서 닫는다. 이런 처리는 이웃 선언과 규칙에 영향을 주지 않는다.
 * <p>
 * 진입점: {@link #parseStylesheet}, {@link #parseRuleList}(<code>@media</code> 의 내용), {@link #parseDeclarationList}(<code>style</code> 속성),
 * {@link #parseDeclaration}, {@link #parseComponentValues}, {@link #splitCommas}.
 */
public final class CssParser {

    /**
     * Maximum nesting depth of blocks and functions. Hostile input that nests deeper is treated as if the text ended there, so that no recursive
     * consumer (calc parsing, <code>var()</code> substitution, serialization) can overflow the Java stack.
     * <p>
     * 블록과 함수의 최대 중첩 깊이. 이보다 깊이 중첩한 악의적인 입력은 거기서 텍스트가 끝난 것으로 다뤄, 재귀하는 소비자(calc 파싱, <code>var()</code> 치환, 직렬화)가
     * Java 스택을 넘치게 하지 못한다.
     */
    public static final int MAX_NESTING = 256;

    /** The list being parsed. / 파싱 중인 목록. */
    private final List<ComponentValue> in;

    /** Current index. / 현재 위치. */
    private int pos;

    /** Diagnostics collector or null. / 진단 수집기 또는 null. */
    private final CssDiagnostics diag;

    /**
     * Creates a parser over component values.
     * <p>
     * 컴포넌트 값 위의 파서를 만든다.
     *
     * @param in   component values / 컴포넌트 값
     * @param diag collector or null / 수집기 또는 null
     */
    private CssParser(List<ComponentValue> in, CssDiagnostics diag) {
        this.in = in;
        this.diag = diag;
    }

    /**
     * Frame of the iterative component value builder.
     * <p>
     * 반복형 컴포넌트 값 구성기의 프레임.
     */
    private static final class Frame {
        /** Opening token type, or 0 for the function frame. / 여는 토큰 종류. */
        final int openType;
        /** Function name, or null for blocks. / 함수 이름. 블록이면 null. */
        final String name;
        /** Closing token type. / 닫는 토큰 종류. */
        final int endType;
        /** Items collected so far. / 지금까지 모은 항목. */
        final List<ComponentValue> items = new ArrayList<ComponentValue>();

        /**
         * Creates a frame.
         * <p>
         * 프레임을 만든다.
         *
         * @param openType opening type / 여는 종류
         * @param name     function name or null / 함수 이름 또는 null
         * @param endType  closing type / 닫는 종류
         */
        Frame(int openType, String name, int endType) {
            this.openType = openType;
            this.name = name;
            this.endType = endType;
        }

        /**
         * Builds the finished component value of this frame.
         * <p>
         * 이 프레임의 완성된 컴포넌트 값을 만든다.
         *
         * @return block or function / 블록 또는 함수
         */
        ComponentValue build() {
            if (name != null) {
                return new CssFunction(name, items);
            }
            return new CssBlock(openType, items);
        }
    }

    /**
     * Tokenizes text and groups the tokens into component values (blocks and functions are nested). The nesting is handled with an explicit
     * stack, so even extremely deep input cannot overflow the Java stack; blocks left open at the end of input are closed implicitly.
     * <p>
     * 텍스트를 토큰화하고 토큰을 컴포넌트 값(블록과 함수는 중첩)으로 묶는다. 중첩은 명시적 스택으로 처리하므로 아주 깊은 입력도 Java 스택을 넘치게 하지
     * 않으며, 입력 끝에서 열려 있는 블록은 암묵적으로 닫는다.
     *
     * @param text CSS text / CSS 문자열
     * @return component values / 컴포넌트 값
     */
    public static List<ComponentValue> parseComponentValues(String text) {
        return group(new CssTokenizer(text));
    }

    /**
     * Groups the tokens of a tokenizer into component values.
     * <p>
     * 토큰화기의 토큰을 컴포넌트 값으로 묶는다.
     *
     * @param t tokenizer / 토큰화기
     * @return component values / 컴포넌트 값
     */
    static List<ComponentValue> group(CssTokenizer t) {
        List<ComponentValue> root = new ArrayList<ComponentValue>();
        List<Frame> stack = new ArrayList<Frame>();
        // 입력 끝 토큰이 나올 때까지 토큰을 읽어 블록 구조로 묶는다.
        while (true) {
            CssToken tok = t.next();
            int type = tok.getType();
            List<ComponentValue> target = stack.isEmpty() ? root : stack.get(stack.size() - 1).items;
            boolean opening = type == CssToken.LEFT_BRACE || type == CssToken.LEFT_PAREN || type == CssToken.LEFT_BRACKET || type == CssToken.FUNCTION;
            if (type == CssToken.EOF || (opening && stack.size() >= MAX_NESTING)) {
                // 입력이 끝났거나 중첩이 한도를 넘으면(악의적인 입력) 거기서 입력이 끝난 것으로 보고 열려 있는 블록을 안쪽부터 닫아 부모에 붙인다.
                while (!stack.isEmpty()) {
                    Frame f = stack.remove(stack.size() - 1);
                    List<ComponentValue> parent = stack.isEmpty() ? root : stack.get(stack.size() - 1).items;
                    parent.add(f.build());
                }
                return root;
            }
            if (!stack.isEmpty() && type == stack.get(stack.size() - 1).endType) {
                Frame f = stack.remove(stack.size() - 1);
                List<ComponentValue> parent = stack.isEmpty() ? root : stack.get(stack.size() - 1).items;
                parent.add(f.build());
                continue;
            }
            switch (type) {
            case CssToken.LEFT_BRACE:
                stack.add(new Frame(type, null, CssToken.RIGHT_BRACE));
                break;
            case CssToken.LEFT_PAREN:
                stack.add(new Frame(type, null, CssToken.RIGHT_PAREN));
                break;
            case CssToken.LEFT_BRACKET:
                stack.add(new Frame(type, null, CssToken.RIGHT_BRACKET));
                break;
            case CssToken.FUNCTION:
                stack.add(new Frame(0, tok.getValue(), CssToken.RIGHT_PAREN));
                break;
            default:
                target.add(tok);
                break;
            }
        }
    }

    /**
     * Tells whether a component value is a whitespace token.
     * <p>
     * 컴포넌트 값이 공백 토큰인지 알려 준다.
     *
     * @param v component value / 컴포넌트 값
     * @return true for whitespace / 공백이면 true
     */
    public static boolean isWhitespace(ComponentValue v) {
        return v instanceof CssToken && ((CssToken) v).is(CssToken.WHITESPACE);
    }

    /**
     * Tells whether a component value is a token of a type.
     * <p>
     * 컴포넌트 값이 특정 종류의 토큰인지 알려 준다.
     *
     * @param v    component value / 컴포넌트 값
     * @param type token type / 토큰 종류
     * @return true if it is such a token / 그런 토큰이면 true
     */
    static boolean isToken(ComponentValue v, int type) {
        return v instanceof CssToken && ((CssToken) v).is(type);
    }

    /**
     * Returns a copy of the list without leading and trailing whitespace.
     * <p>
     * 앞뒤 공백을 제거한 목록의 복사본을 반환한다.
     *
     * @param list component values / 컴포넌트 값
     * @return trimmed copy / 다듬은 복사본
     */
    public static List<ComponentValue> trim(List<ComponentValue> list) {
        int a = 0;
        int b = list.size();
        // 앞쪽 공백을 건너뛴다.
        while (a < b && isWhitespace(list.get(a))) {
            a++;
        }
        // 뒤쪽 공백을 건너뛴다.
        while (b > a && isWhitespace(list.get(b - 1))) {
            b--;
        }
        return new ArrayList<ComponentValue>(list.subList(a, b));
    }

    /**
     * Splits a list at its top-level commas; each part is trimmed.
     * <p>
     * 목록을 최상위 쉼표에서 나눈다. 각 조각은 앞뒤 공백을 제거한다.
     *
     * @param list component values / 컴포넌트 값
     * @return the parts (a list without commas gives one part) / 조각들(쉼표가 없으면 조각 하나)
     */
    public static List<List<ComponentValue>> splitCommas(List<ComponentValue> list) {
        List<List<ComponentValue>> out = new ArrayList<List<ComponentValue>>();
        List<ComponentValue> cur = new ArrayList<ComponentValue>();
        // 쉼표를 만날 때마다 지금까지 모은 값을 한 조각으로 확정한다.
        for (int i = 0; i < list.size(); i++) {
            ComponentValue v = list.get(i);
            if (isToken(v, CssToken.COMMA)) {
                out.add(trim(cur));
                cur = new ArrayList<ComponentValue>();
            } else {
                cur.add(v);
            }
        }
        out.add(trim(cur));
        return out;
    }

    /**
     * Parses a whole style sheet text into its top-level rules. The tokens CDO and CDC between rules are ignored.
     * <p>
     * 스타일 시트 텍스트 전체를 최상위 규칙들로 파싱한다. 규칙 사이의 CDO, CDC 토큰은 무시한다.
     *
     * @param text CSS text / CSS 문자열
     * @param diag collector or null / 수집기 또는 null
     * @return rules in source order / 소스 순서의 규칙
     */
    public static List<SyntaxRule> parseStylesheet(String text, CssDiagnostics diag) {
        return parseRuleList(parseComponentValues(text), true, diag);
    }

    /**
     * Parses a list of component values into rules (the contents of a grouping at-rule such as <code>@media</code>, or a whole style sheet).
     * <p>
     * 컴포넌트 값 목록을 규칙들로 파싱한다(<code>@media</code> 같은 그룹 at-규칙의 내용이나 스타일 시트 전체).
     *
     * @param list     component values / 컴포넌트 값
     * @param topLevel true to ignore CDO and CDC (top level of a style sheet) / 최상위(CDO, CDC 무시)이면 true
     * @param diag     collector or null / 수집기 또는 null
     * @return rules in source order / 소스 순서의 규칙
     */
    public static List<SyntaxRule> parseRuleList(List<ComponentValue> list, boolean topLevel, CssDiagnostics diag) {
        CssParser p = new CssParser(list, diag);
        List<SyntaxRule> rules = new ArrayList<SyntaxRule>();
        // 목록 끝까지 규칙을 하나씩 소비한다.
        while (p.pos < list.size()) {
            ComponentValue v = list.get(p.pos);
            if (isWhitespace(v)) {
                p.pos++;
            } else if (topLevel && (isToken(v, CssToken.CDO) || isToken(v, CssToken.CDC))) {
                p.pos++;
            } else if (isToken(v, CssToken.AT_KEYWORD)) {
                rules.add(p.consumeAtRule());
            } else {
                SyntaxRule r = p.consumeQualifiedRule();
                if (r != null) {
                    rules.add(r);
                }
            }
        }
        return rules;
    }

    /**
     * Consumes an at-rule at the current position: the prelude runs to a semicolon or to a curly block.
     * <p>
     * 현재 위치의 at-규칙을 소비한다. 전문은 세미콜론이나 중괄호 블록까지이다.
     *
     * @return the at-rule / at-규칙
     */
    private SyntaxRule consumeAtRule() {
        CssToken kw = (CssToken) in.get(pos++);
        List<ComponentValue> prelude = new ArrayList<ComponentValue>();
        // 세미콜론, 중괄호 블록, 목록 끝 중 하나를 만날 때까지 전문을 모은다.
        while (pos < in.size()) {
            ComponentValue v = in.get(pos);
            if (isToken(v, CssToken.SEMICOLON)) {
                pos++;
                return new SyntaxRule(true, kw.getValue(), trim(prelude), null, kw.getStart());
            }
            if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_BRACE) {
                pos++;
                return new SyntaxRule(true, kw.getValue(), trim(prelude), ((CssBlock) v).getContents(), kw.getStart());
            }
            prelude.add(v);
            pos++;
        }
        CssDiagnostics.report(diag, CssDiagnostics.RECOVERED, "@" + kw.getValue(), "at-rule ended by end of input");
        return new SyntaxRule(true, kw.getValue(), trim(prelude), null, kw.getStart());
    }

    /**
     * Consumes a qualified rule at the current position: the prelude runs to a curly block. Without a block the rule is dropped.
     * <p>
     * 현재 위치의 한정 규칙을 소비한다. 전문은 중괄호 블록까지이다. 블록이 없으면 규칙을 버린다.
     *
     * @return the rule, or null if it was dropped / 규칙. 버려졌으면 null
     */
    private SyntaxRule consumeQualifiedRule() {
        List<ComponentValue> prelude = new ArrayList<ComponentValue>();
        int start = in.get(pos) instanceof CssToken ? ((CssToken) in.get(pos)).getStart() : -1;
        // 중괄호 블록이나 목록 끝을 만날 때까지 전문을 모은다.
        while (pos < in.size()) {
            ComponentValue v = in.get(pos);
            if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_BRACE) {
                pos++;
                return new SyntaxRule(false, null, trim(prelude), ((CssBlock) v).getContents(), start);
            }
            prelude.add(v);
            pos++;
        }
        if (diag != null) {
            diag.add(CssDiagnostics.INVALID, describe(prelude), "rule without a block dropped");
        }
        return null;
    }

    /**
     * Describes component values for a diagnostic. Very deep nesting that would overflow the serializer is summarised.
     * <p>
     * 진단용으로 컴포넌트 값을 설명한다. 직렬화기가 스택을 넘칠 만큼 깊은 중첩은 요약한다.
     *
     * @param list component values / 컴포넌트 값
     * @return short description / 짧은 설명
     */
    static String describe(List<ComponentValue> list) {
        try {
            String s = CssBlock.join(list).trim();
            return s.length() > 120 ? s.substring(0, 120) + "..." : s;
        } catch (StackOverflowError e) {
            return "(too deeply nested)";
        }
    }

    /**
     * Parses the contents of a declaration block (or a <code>style</code> attribute text) into declarations.
     * <p>
     * 선언 블록의 내용(또는 <code>style</code> 속성 텍스트)을 선언들로 파싱한다.
     *
     * @param text CSS text / CSS 문자열
     * @param diag collector or null / 수집기 또는 null
     * @return declarations in source order / 소스 순서의 선언
     */
    public static List<SyntaxDeclaration> parseDeclarationList(String text, CssDiagnostics diag) {
        return parseDeclarationBlock(parseComponentValues(text), diag).getDeclarations();
    }

    /**
     * Parses component values into declarations and at-rules.
     * <p>
     * 컴포넌트 값을 선언과 at-규칙으로 파싱한다.
     *
     * @param list component values (block contents) / 컴포넌트 값(블록의 내용)
     * @param diag collector or null / 수집기 또는 null
     * @return declarations and at-rules / 선언과 at-규칙
     */
    public static DeclarationBlock parseDeclarationBlock(List<ComponentValue> list, CssDiagnostics diag) {
        CssParser p = new CssParser(list, diag);
        List<SyntaxDeclaration> decls = new ArrayList<SyntaxDeclaration>();
        List<SyntaxRule> rules = new ArrayList<SyntaxRule>();
        // 목록 끝까지 선언이나 at-규칙을 하나씩 소비한다.
        while (p.pos < list.size()) {
            ComponentValue v = list.get(p.pos);
            if (isWhitespace(v) || isToken(v, CssToken.SEMICOLON)) {
                p.pos++;
            } else if (isToken(v, CssToken.AT_KEYWORD)) {
                rules.add(p.consumeAtRule());
            } else {
                int end = p.pos;
                // 다음 세미콜론 앞까지가 선언 하나(또는 버릴 조각)이다.
                while (end < list.size() && !isToken(list.get(end), CssToken.SEMICOLON)) {
                    end++;
                }
                List<ComponentValue> seg = new ArrayList<ComponentValue>(list.subList(p.pos, end));
                SyntaxDeclaration d = isToken(v, CssToken.IDENT) ? declarationOf(seg) : null;
                if (d != null) {
                    decls.add(d);
                } else if (diag != null) {
                    diag.add(CssDiagnostics.INVALID, describe(seg), "declaration dropped");
                }
                p.pos = end;
            }
        }
        return new DeclarationBlock(decls, rules);
    }

    /**
     * Parses the contents of a style rule block that may nest rules (CSS Nesting Module Level 1, CSS Syntax Level 3 "consume a block's contents"):
     * the declarations, the nested qualified rules and the nested at-rules in source order. A segment that starts with an identifier is a declaration
     * unless it contains a top-level <code>{}</code> block next to other values (<code>a:hover { ... }</code> is a nested rule, <code>--x: { }</code>
     * is a custom property declaration); a segment that ends in a block is a nested qualified rule whose prelude is what precedes the block.
     * <p>
     * 규칙을 중첩할 수 있는 스타일 규칙 블록의 내용을 파싱한다(CSS Nesting Module Level 1, CSS Syntax Level 3 "consume a block's contents"): 선언, 중첩된 한정 규칙, 중첩된
     * at-규칙을 소스 순서대로. 식별자로 시작하는 조각은, 다른 값과 함께 최상위 <code>{}</code> 블록이 들어 있지 않으면 선언이다(<code>a:hover { ... }</code> 는 중첩 규칙이고
     * <code>--x: { }</code> 는 사용자 정의 속성 선언이다). 블록으로 끝나는 조각은 블록 앞이 전문인 중첩 한정 규칙이다.
     *
     * @param list component values (block contents) / 컴포넌트 값(블록의 내용)
     * @param diag collector or null / 수집기 또는 null
     * @return a list of {@link SyntaxDeclaration} and {@link SyntaxRule} objects in source order / 소스 순서의 {@link SyntaxDeclaration} 과 {@link SyntaxRule} 목록
     */
    public static List<Object> parseBlockContents(List<ComponentValue> list, CssDiagnostics diag) {
        CssParser p = new CssParser(list, diag);
        List<Object> out = new ArrayList<Object>();
        // 목록 끝까지 선언, 중첩 규칙, at-규칙을 하나씩 소비한다.
        while (p.pos < list.size()) {
            ComponentValue v = list.get(p.pos);
            if (isWhitespace(v) || isToken(v, CssToken.SEMICOLON)) {
                p.pos++;
                continue;
            }
            if (isToken(v, CssToken.AT_KEYWORD)) {
                out.add(p.consumeAtRule());
                continue;
            }
            int end = p.pos;
            int blockAt = -1;
            // 조각은 세미콜론 앞까지이거나 처음 나오는 최상위 중괄호 블록까지이다.
            while (end < list.size() && !isToken(list.get(end), CssToken.SEMICOLON)) {
                ComponentValue c = list.get(end);
                end++;
                if (c instanceof CssBlock && ((CssBlock) c).getOpenType() == CssToken.LEFT_BRACE) {
                    blockAt = end - 1;
                    break;
                }
            }
            List<ComponentValue> seg = new ArrayList<ComponentValue>(list.subList(p.pos, end));
            boolean custom = isToken(v, CssToken.IDENT) && ((CssToken) v).getValue().startsWith("--");
            if (blockAt >= 0 && !custom) {
                // 블록으로 끝나는 조각은 중첩 한정 규칙이다(전문은 블록 앞).
                List<ComponentValue> prelude = new ArrayList<ComponentValue>(list.subList(p.pos, blockAt));
                int start = v instanceof CssToken ? ((CssToken) v).getStart() : -1;
                out.add(new SyntaxRule(false, null, trim(prelude), ((CssBlock) list.get(blockAt)).getContents(), start));
            } else {
                SyntaxDeclaration d = isToken(v, CssToken.IDENT) ? declarationOf(seg) : null;
                if (d != null) {
                    out.add(d);
                } else if (diag != null) {
                    diag.add(CssDiagnostics.INVALID, describe(seg), "declaration dropped");
                }
            }
            p.pos = end;
        }
        return out;
    }

    /**
     * Parses one declaration text such as <code>color: red !important</code>.
     * <p>
     * <code>color: red !important</code> 같은 선언 하나를 파싱한다.
     *
     * @param text declaration text / 선언 문자열
     * @return the declaration, or null if the text is not a valid declaration / 선언. 올바른 선언이 아니면 null
     */
    public static SyntaxDeclaration parseDeclaration(String text) {
        List<ComponentValue> seg = trim(parseComponentValues(text));
        // 세미콜론이 있으면 선언 하나가 아니므로 거부한다.
        for (int i = 0; i < seg.size(); i++) {
            if (isToken(seg.get(i), CssToken.SEMICOLON)) {
                return null;
            }
        }
        return declarationOf(seg);
    }

    /**
     * Builds a declaration from the component values of one segment.
     * <p>
     * 한 조각의 컴포넌트 값으로 선언을 만든다.
     *
     * @param seg component values without the terminating semicolon / 끝 세미콜론이 없는 컴포넌트 값
     * @return the declaration, or null if invalid / 선언. 잘못되었으면 null
     */
    static SyntaxDeclaration declarationOf(List<ComponentValue> seg) {
        int i = 0;
        // 앞쪽 공백을 건너뛴다.
        while (i < seg.size() && isWhitespace(seg.get(i))) {
            i++;
        }
        if (i >= seg.size() || !isToken(seg.get(i), CssToken.IDENT)) {
            return null;
        }
        String name = ((CssToken) seg.get(i)).getValue();
        i++;
        // 이름과 콜론 사이의 공백을 건너뛴다.
        while (i < seg.size() && isWhitespace(seg.get(i))) {
            i++;
        }
        if (i >= seg.size() || !isToken(seg.get(i), CssToken.COLON)) {
            return null;
        }
        i++;
        List<ComponentValue> value = trim(new ArrayList<ComponentValue>(seg.subList(i, seg.size())));
        boolean important = false;
        int n = value.size();
        // 끝이 "!" 와 "important" 이면 important 로 표시하고 값에서 뺀다.
        if (n >= 2 && value.get(n - 1) instanceof CssToken && ((CssToken) value.get(n - 1)).isIdent("important")) {
            int k = n - 2;
            // important 앞의 공백을 건너뛰어 느낌표를 찾는다.
            while (k >= 0 && isWhitespace(value.get(k))) {
                k--;
            }
            if (k >= 0 && value.get(k) instanceof CssToken && ((CssToken) value.get(k)).isDelim('!')) {
                important = true;
                value = trim(new ArrayList<ComponentValue>(value.subList(0, k)));
            }
        }
        return new SyntaxDeclaration(name, value, important);
    }
}
