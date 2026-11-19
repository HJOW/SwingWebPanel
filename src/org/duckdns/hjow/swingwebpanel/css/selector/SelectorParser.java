package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The parser of Selectors Level 3/4 selector text. It works on the component values produced by the CSS parser, resolves namespace prefixes
 * against a {@link NamespaceMap}, and reports anything it does not understand with a {@link SelectorSyntaxException}: a rule with an invalid
 * selector is dropped as a whole, and <code>querySelector</code> raises a <code>SyntaxError</code>. Inside <code>:is()</code> and
 * <code>:where()</code> invalid entries are ignored (forgiving lists).
 * <p>
 * Supported: type, universal, id, class, attribute (all operators and the <code>i</code>/<code>s</code> flags), the combinators
 * <code>' '</code> <code>&gt;</code> <code>+</code> <code>~</code>, every pseudo-class of {@link PseudoClasses}, the pseudo-elements listed there.
 * <p>
 * Selectors Level 3/4 선택자 텍스트의 파서. CSS 파서가 만든 컴포넌트 값 위에서 동작하며 {@link NamespaceMap} 으로 네임스페이스 접두사를 해석하고,
 * 이해하지 못하는 것은 {@link SelectorSyntaxException} 으로 알린다. 올바르지 않은 선택자를 가진 규칙은 통째로 버려지고 <code>querySelector</code> 는
 * <code>SyntaxError</code> 를 낸다. <code>:is()</code> 와 <code>:where()</code> 안에서는 잘못된 항목을 무시한다(관대한 목록).
 * <p>
 * 지원: 타입, 전체, id, 클래스, 속성(모든 연산자와 <code>i</code>/<code>s</code> 플래그), 결합자 <code>' '</code> <code>&gt;</code> <code>+</code> <code>~</code>,
 * {@link PseudoClasses} 의 모든 의사 클래스와 거기 나열한 의사 요소.
 */
public final class SelectorParser {

    /** The component values being parsed. / 파싱 중인 컴포넌트 값. */
    private final List<ComponentValue> in;

    /** Current index. / 현재 위치. */
    private int pos;

    /** Namespace declarations or null. / 네임스페이스 선언 또는 null. */
    private final NamespaceMap namespaces;

    /** Maximum nesting depth of selector arguments (<code>:is(:not(...))</code>), which keeps hostile input from overflowing the stack. / 선택자 인수의 최대 중첩 깊이(<code>:is(:not(...))</code>). 악의적인 입력이 스택을 넘치게 하지 못하게 한다. */
    public static final int MAX_NESTING = 24;

    /** Nesting depth of this parser. / 이 파서의 중첩 깊이. */
    private final int depth;

    /**
     * Creates a parser over component values.
     * <p>
     * 컴포넌트 값 위의 파서를 만든다.
     *
     * @param in         component values / 컴포넌트 값
     * @param namespaces namespace declarations or null / 네임스페이스 선언 또는 null
     * @param depth      nesting depth / 중첩 깊이
     */
    private SelectorParser(List<ComponentValue> in, NamespaceMap namespaces, int depth) {
        this.in = in;
        this.namespaces = namespaces;
        this.depth = depth;
    }

    /**
     * Parses selector text (a comma separated list).
     * <p>
     * 선택자 텍스트(쉼표로 구분한 목록)를 파싱한다.
     *
     * @param text       selector text / 선택자 문자열
     * @param namespaces namespace declarations or null (prefixes are then invalid) / 네임스페이스 선언 또는 null(접두사는 무효)
     * @return the list, never empty / 목록(비어 있지 않음)
     * @throws SelectorSyntaxException if the text is not a valid selector list / 올바른 선택자 목록이 아니면
     */
    public static SelectorList parse(String text, NamespaceMap namespaces) throws SelectorSyntaxException {
        return parse(CssParser.parseComponentValues(text), namespaces);
    }

    /**
     * Parses a selector list given as component values (for example the prelude of a style rule).
     * <p>
     * 컴포넌트 값으로 주어진 선택자 목록(예: 스타일 규칙의 전문)을 파싱한다.
     *
     * @param values     component values / 컴포넌트 값
     * @param namespaces namespace declarations or null / 네임스페이스 선언 또는 null
     * @return the list / 목록
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    public static SelectorList parse(List<ComponentValue> values, NamespaceMap namespaces) throws SelectorSyntaxException {
        return parseList(values, namespaces, false, false, 0);
    }

    /**
     * Parses a comma separated list of complex selectors.
     * <p>
     * 쉼표로 구분한 복합 선택자 목록을 파싱한다.
     *
     * @param values     component values / 컴포넌트 값
     * @param namespaces namespace declarations or null / 네임스페이스 선언 또는 null
     * @param forgiving  true to ignore invalid entries and accept an empty list / 잘못된 항목을 무시하고 빈 목록을 허용하려면 true
     * @param relative   true for relative selectors (<code>:has()</code>) / 상대 선택자(<code>:has()</code>)이면 true
     * @return the list / 목록
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private static SelectorList parseList(List<ComponentValue> values, NamespaceMap namespaces, boolean forgiving, boolean relative, int depth)
            throws SelectorSyntaxException {
        if (depth > MAX_NESTING) {
            throw new SelectorSyntaxException("selector arguments are nested too deeply");
        }
        List<List<ComponentValue>> parts = CssParser.splitCommas(values);
        List<ComplexSelector> out = new ArrayList<ComplexSelector>();
        // 쉼표로 나눈 조각마다 복합 선택자 하나를 파싱한다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> part = parts.get(i);
            try {
                if (part.isEmpty()) {
                    throw new SelectorSyntaxException("empty selector");
                }
                SelectorParser p = new SelectorParser(part, namespaces, depth);
                out.add(p.parseComplex(relative));
            } catch (SelectorSyntaxException e) {
                if (!forgiving) {
                    throw e;
                }
            }
        }
        if (out.isEmpty() && !forgiving) {
            throw new SelectorSyntaxException("empty selector list");
        }
        return new SelectorList(out);
    }

    /**
     * Skips whitespace.
     * <p>
     * 공백을 건너뛴다.
     *
     * @return true if any whitespace was skipped / 공백을 건너뛰었으면 true
     */
    private boolean skipWs() {
        boolean any = false;
        // 연속된 공백 토큰을 건너뛴다.
        while (pos < in.size() && CssParser.isWhitespace(in.get(pos))) {
            pos++;
            any = true;
        }
        return any;
    }

    /**
     * Returns the combinator at the current position without consuming it.
     * <p>
     * 현재 위치의 결합자를 소비하지 않고 반환한다.
     *
     * @return combinator character, or 0 if the next token is not a combinator / 결합자 문자. 다음 토큰이 결합자가 아니면 0
     */
    private char peekCombinator() {
        if (pos < in.size() && in.get(pos) instanceof CssToken) {
            CssToken t = (CssToken) in.get(pos);
            if (t.isDelim('>')) {
                return ComplexSelector.CHILD;
            }
            if (t.isDelim('+')) {
                return ComplexSelector.NEXT_SIBLING;
            }
            if (t.isDelim('~')) {
                return ComplexSelector.LATER_SIBLING;
            }
        }
        return 0;
    }

    /**
     * Parses one complex selector from the whole input.
     * <p>
     * 입력 전체에서 복합 선택자 하나를 파싱한다.
     *
     * @param relative true for a relative selector / 상대 선택자이면 true
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private ComplexSelector parseComplex(boolean relative) throws SelectorSyntaxException {
        skipWs();
        char leading = 0;
        if (relative) {
            leading = peekCombinator();
            if (leading != 0) {
                pos++;
                skipWs();
            } else {
                leading = ComplexSelector.DESCENDANT;
            }
        }
        List<CompoundSelector> compounds = new ArrayList<CompoundSelector>();
        List<Character> combs = new ArrayList<Character>();
        // 복합 선택자와 결합자를 번갈아 읽는다.
        while (true) {
            CompoundSelector c = parseCompound();
            compounds.add(c);
            boolean ws = skipWs();
            if (pos >= in.size()) {
                break;
            }
            char comb = peekCombinator();
            if (comb != 0) {
                pos++;
                skipWs();
            } else if (ws) {
                comb = ComplexSelector.DESCENDANT;
            } else {
                throw new SelectorSyntaxException("unexpected token " + in.get(pos).toCssText());
            }
            if (c.getPseudoElement() != null) {
                throw new SelectorSyntaxException("a pseudo-element must be the last part of a selector");
            }
            combs.add(Character.valueOf(comb));
        }
        char[] arr = new char[combs.size()];
        // 결합자 목록을 배열로 옮긴다.
        for (int i = 0; i < arr.length; i++) {
            arr[i] = combs.get(i).charValue();
        }
        return new ComplexSelector(compounds, arr, leading);
    }

    /**
     * Returns the current component value or null at the end.
     * <p>
     * 현재 컴포넌트 값을 반환한다. 끝이면 null.
     *
     * @return the value or null / 값 또는 null
     */
    private ComponentValue peek() {
        return pos < in.size() ? in.get(pos) : null;
    }

    /**
     * Returns the component value at an offset or null.
     * <p>
     * 오프셋 위치의 컴포넌트 값을 반환한다. 없으면 null.
     *
     * @param off offset from the current position / 현재 위치로부터의 오프셋
     * @return the value or null / 값 또는 null
     */
    private ComponentValue peekAt(int off) {
        int i = pos + off;
        return i < in.size() ? in.get(i) : null;
    }

    /**
     * Tells whether a component value is a delimiter token.
     * <p>
     * 컴포넌트 값이 구분 문자 토큰인지 알려 준다.
     *
     * @param v component value or null / 컴포넌트 값 또는 null
     * @param c delimiter character / 구분 문자
     * @return true if it is that delimiter / 그 구분 문자이면 true
     */
    private static boolean isDelim(ComponentValue v, char c) {
        return v instanceof CssToken && ((CssToken) v).isDelim(c);
    }

    /**
     * Tells whether a component value is a token of a type.
     * <p>
     * 컴포넌트 값이 특정 종류의 토큰인지 알려 준다.
     *
     * @param v    component value or null / 컴포넌트 값 또는 null
     * @param type token type / 토큰 종류
     * @return true if it is such a token / 그런 토큰이면 true
     */
    private static boolean isTok(ComponentValue v, int type) {
        return v instanceof CssToken && ((CssToken) v).is(type);
    }

    /**
     * Parses one compound selector.
     * <p>
     * 복합 선택자 하나를 파싱한다.
     *
     * @return the compound selector / 복합 선택자
     * @throws SelectorSyntaxException if invalid or empty / 올바르지 않거나 비어 있으면
     */
    private CompoundSelector parseCompound() throws SelectorSyntaxException {
        List<SimpleSelector> simples = new ArrayList<SimpleSelector>();
        String pseudoElement = null;
        SimpleSelector type = tryParseType();
        if (type != null) {
            simples.add(type);
        }
        // 타입 선택자 뒤에 올 수 있는 단순 선택자를 더 이상 없을 때까지 읽는다.
        while (true) {
            ComponentValue v = peek();
            if (v == null) {
                break;
            }
            if (pseudoElement != null && !(v instanceof CssToken && ((CssToken) v).is(CssToken.WHITESPACE))
                    && !(v instanceof CssToken && peekCombinator() != 0)) {
                throw new SelectorSyntaxException("nothing may follow a pseudo-element in a compound selector");
            }
            if (isTok(v, CssToken.HASH)) {
                CssToken h = (CssToken) v;
                if (!h.isHashId()) {
                    throw new SelectorSyntaxException("invalid id selector #" + h.getValue());
                }
                simples.add(SimpleSelector.id(h.getValue()));
                pos++;
            } else if (isDelim(v, '.')) {
                ComponentValue n = peekAt(1);
                if (!isTok(n, CssToken.IDENT)) {
                    throw new SelectorSyntaxException("a class selector needs a name");
                }
                simples.add(SimpleSelector.cls(((CssToken) n).getValue()));
                pos += 2;
            } else if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_BRACKET) {
                simples.add(parseAttribute((CssBlock) v));
                pos++;
            } else if (isTok(v, CssToken.COLON)) {
                pseudoElement = parsePseudo(simples);
            } else {
                break;
            }
        }
        if (simples.isEmpty() && pseudoElement == null) {
            ComponentValue v = peek();
            throw new SelectorSyntaxException(v == null ? "a selector is missing" : "unexpected token " + v.toCssText());
        }
        return new CompoundSelector(simples, pseudoElement);
    }

    /**
     * Parses a type or universal selector with an optional namespace prefix, if one starts at the current position.
     * <p>
     * 현재 위치에서 네임스페이스 접두사가 붙을 수 있는 타입 또는 전체 선택자가 시작하면 파싱한다.
     *
     * @return the selector, or null if none starts here / 선택자. 여기서 시작하지 않으면 null
     * @throws SelectorSyntaxException if the namespace prefix is unknown / 네임스페이스 접두사를 모르면
     */
    private SimpleSelector tryParseType() throws SelectorSyntaxException {
        ComponentValue v0 = peek();
        ComponentValue v1 = peekAt(1);
        ComponentValue v2 = peekAt(2);
        String prefix = null;
        int consumed = 0;
        if (isDelim(v0, '|') && (isTok(v1, CssToken.IDENT) || isDelim(v1, '*'))) {
            prefix = "";
            consumed = 1;
        } else if ((isTok(v0, CssToken.IDENT) || isDelim(v0, '*')) && isDelim(v1, '|') && (isTok(v2, CssToken.IDENT) || isDelim(v2, '*'))) {
            prefix = isDelim(v0, '*') ? "*" : ((CssToken) v0).getValue();
            consumed = 2;
        }
        ComponentValue nameTok = consumed == 0 ? v0 : consumed == 1 ? v1 : v2;
        if (!(isTok(nameTok, CssToken.IDENT) || isDelim(nameTok, '*'))) {
            return null;
        }
        boolean nsAny;
        String ns = null;
        String nsText = "";
        if (prefix == null) {
            String def = namespaces == null ? null : namespaces.getDefaultNamespace();
            nsAny = def == null;
            ns = def;
        } else if (prefix.equals("*")) {
            nsAny = true;
            nsText = "*|";
        } else if (prefix.length() == 0) {
            nsAny = false;
            nsText = "|";
        } else {
            if (namespaces == null || !namespaces.has(prefix)) {
                throw new SelectorSyntaxException("unknown namespace prefix " + prefix);
            }
            nsAny = false;
            ns = namespaces.resolve(prefix);
            nsText = CssEscape.ident(prefix) + "|";
        }
        pos += consumed + 1;
        if (isDelim(nameTok, '*')) {
            return SimpleSelector.universal(nsAny, ns, nsText);
        }
        return SimpleSelector.type(((CssToken) nameTok).getValue(), nsAny, ns, nsText);
    }

    /**
     * Parses an attribute selector block.
     * <p>
     * 속성 선택자 블록을 파싱한다.
     *
     * @param block the bracket block / 대괄호 블록
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private SimpleSelector parseAttribute(CssBlock block) throws SelectorSyntaxException {
        List<ComponentValue> c = CssParser.trim(block.getContents());
        int i = 0;
        int n = c.size();
        String prefix = null;
        if (n >= 2 && isDelim(c.get(0), '|') && isTok(c.get(1), CssToken.IDENT)) {
            prefix = "";
            i = 1;
        } else if (n >= 3 && (isTok(c.get(0), CssToken.IDENT) || isDelim(c.get(0), '*')) && isDelim(c.get(1), '|') && isTok(c.get(2), CssToken.IDENT)) {
            prefix = isDelim(c.get(0), '*') ? "*" : ((CssToken) c.get(0)).getValue();
            i = 2;
        }
        if (i >= n || !isTok(c.get(i), CssToken.IDENT)) {
            throw new SelectorSyntaxException("an attribute selector needs an attribute name");
        }
        String local = ((CssToken) c.get(i)).getValue();
        i++;
        boolean nsAny = false;
        String ns = null;
        String nsText = "";
        if (prefix != null) {
            if (prefix.equals("*")) {
                nsAny = true;
                nsText = "*|";
            } else if (prefix.length() == 0) {
                nsText = "|";
            } else {
                if (namespaces == null || !namespaces.has(prefix)) {
                    throw new SelectorSyntaxException("unknown namespace prefix " + prefix);
                }
                ns = namespaces.resolve(prefix);
                nsText = CssEscape.ident(prefix) + "|";
            }
        }
        // 속성 이름 뒤의 공백을 건너뛴다.
        while (i < n && CssParser.isWhitespace(c.get(i))) {
            i++;
        }
        if (i == n) {
            return SimpleSelector.attribute(local, nsAny, ns, nsText, SimpleSelector.OP_EXISTS, null, SimpleSelector.CASE_DEFAULT);
        }
        int op;
        if (isDelim(c.get(i), '=')) {
            op = SimpleSelector.OP_EQUALS;
            i++;
        } else if (i + 1 < n && isDelim(c.get(i + 1), '=')) {
            op = operatorOf(c.get(i));
            i += 2;
        } else {
            throw new SelectorSyntaxException("invalid attribute operator");
        }
        // 연산자 뒤의 공백을 건너뛴다.
        while (i < n && CssParser.isWhitespace(c.get(i))) {
            i++;
        }
        if (i >= n || !(isTok(c.get(i), CssToken.IDENT) || isTok(c.get(i), CssToken.STRING))) {
            throw new SelectorSyntaxException("an attribute value must be an identifier or a string");
        }
        String value = ((CssToken) c.get(i)).getValue();
        i++;
        // 값 뒤의 공백을 건너뛴다.
        while (i < n && CssParser.isWhitespace(c.get(i))) {
            i++;
        }
        int mode = SimpleSelector.CASE_DEFAULT;
        if (i < n) {
            if (isTok(c.get(i), CssToken.IDENT) && ((CssToken) c.get(i)).isIdent("i")) {
                mode = SimpleSelector.CASE_INSENSITIVE;
            } else if (isTok(c.get(i), CssToken.IDENT) && ((CssToken) c.get(i)).isIdent("s")) {
                mode = SimpleSelector.CASE_SENSITIVE;
            } else {
                throw new SelectorSyntaxException("invalid attribute selector flag");
            }
            i++;
            // 플래그 뒤에는 공백만 올 수 있다.
            while (i < n && CssParser.isWhitespace(c.get(i))) {
                i++;
            }
            if (i < n) {
                throw new SelectorSyntaxException("unexpected token in an attribute selector");
            }
        }
        return SimpleSelector.attribute(local, nsAny, ns, nsText, op, value, mode);
    }

    /**
     * Maps the first character of a two-character attribute operator to its constant.
     * <p>
     * 두 글자 속성 연산자의 첫 글자를 상수로 바꾼다.
     *
     * @param v the delimiter before the equals sign / 등호 앞의 구분 문자
     * @return operator constant / 연산자 상수
     * @throws SelectorSyntaxException if it is not an operator character / 연산자 글자가 아니면
     */
    private static int operatorOf(ComponentValue v) throws SelectorSyntaxException {
        if (isDelim(v, '~')) {
            return SimpleSelector.OP_INCLUDES;
        }
        if (isDelim(v, '|')) {
            return SimpleSelector.OP_DASH;
        }
        if (isDelim(v, '^')) {
            return SimpleSelector.OP_PREFIX;
        }
        if (isDelim(v, '$')) {
            return SimpleSelector.OP_SUFFIX;
        }
        if (isDelim(v, '*')) {
            return SimpleSelector.OP_SUBSTRING;
        }
        throw new SelectorSyntaxException("invalid attribute operator");
    }

    /**
     * Parses a pseudo-class or pseudo-element at the current colon.
     * <p>
     * 현재 콜론에서 시작하는 의사 클래스나 의사 요소를 파싱한다.
     *
     * @param simples the list that receives a pseudo-class / 의사 클래스를 받을 목록
     * @return the pseudo-element name if a pseudo-element was parsed, else null / 의사 요소를 파싱했으면 그 이름. 아니면 null
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private String parsePseudo(List<SimpleSelector> simples) throws SelectorSyntaxException {
        pos++;
        boolean doubleColon = false;
        if (isTok(peek(), CssToken.COLON)) {
            doubleColon = true;
            pos++;
        }
        ComponentValue v = peek();
        if (isTok(v, CssToken.IDENT)) {
            String name = ((CssToken) v).getValue().toLowerCase();
            pos++;
            if (doubleColon) {
                if (!PseudoClasses.isPseudoElement(name)) {
                    throw new SelectorSyntaxException("unknown pseudo-element ::" + name);
                }
                return name;
            }
            if (PseudoClasses.isLegacyPseudoElement(name)) {
                return name;
            }
            int[] info = PseudoClasses.lookup(name);
            if (info == null) {
                throw new SelectorSyntaxException("unknown pseudo-class :" + name);
            }
            if (info[1] != PseudoClasses.ARG_NONE && info[1] != PseudoClasses.ARG_ANY) {
                throw new SelectorSyntaxException("the pseudo-class :" + name + " needs arguments");
            }
            simples.add(SimpleSelector.pseudo(name, info[0]));
            return null;
        }
        if (v instanceof CssFunction && !doubleColon) {
            CssFunction f = (CssFunction) v;
            pos++;
            simples.add(parseFunctionalPseudo(f));
            return null;
        }
        if (v instanceof CssFunction && ((CssFunction) v).isNamed("highlight")) {
            // ::highlight(<custom-highlight-name>) 는 식별자 하나를 받는다(CSS Custom Highlight API 1 4.1 절). 이름은 대소문자를 구분한다.
            List<ComponentValue> args = CssParser.trim(((CssFunction) v).getArguments());
            if (args.size() != 1 || !isTok(args.get(0), CssToken.IDENT)) {
                throw new SelectorSyntaxException("::highlight() needs one identifier");
            }
            pos++;
            return "highlight(" + ((CssToken) args.get(0)).getValue() + ")";
        }
        throw new SelectorSyntaxException("invalid pseudo selector");
    }

    /**
     * Parses a functional pseudo-class such as <code>:not(...)</code> or <code>:nth-child(...)</code>.
     * <p>
     * <code>:not(...)</code>, <code>:nth-child(...)</code> 같은 함수형 의사 클래스를 파싱한다.
     *
     * @param f the function / 함수
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private SimpleSelector parseFunctionalPseudo(CssFunction f) throws SelectorSyntaxException {
        String name = f.getName().toLowerCase();
        int[] info = PseudoClasses.lookup(name);
        if (info == null) {
            throw new SelectorSyntaxException("unknown pseudo-class :" + name + "()");
        }
        int id = info[0];
        List<ComponentValue> args = f.getArguments();
        switch (info[1]) {
        case PseudoClasses.ARG_NTH:
        case PseudoClasses.ARG_NTH_OF:
            return parseNth(name, id, args, info[1] == PseudoClasses.ARG_NTH_OF);
        case PseudoClasses.ARG_FORGIVING:
            return SimpleSelector.withList(name, id, checkNoPseudoElements(parseList(args, namespaces, true, false, depth + 1)));
        case PseudoClasses.ARG_LIST:
            return SimpleSelector.withList(name, id, checkNoPseudoElements(parseList(args, namespaces, false, false, depth + 1)));
        case PseudoClasses.ARG_RELATIVE:
            return SimpleSelector.withList(name, id, checkNoPseudoElements(parseList(args, namespaces, false, true, depth + 1)));
        case PseudoClasses.ARG_LANG:
            return parseLang(name, id, args);
        case PseudoClasses.ARG_DIR:
            return parseDir(name, id, args);
        case PseudoClasses.ARG_ANY:
            return SimpleSelector.pseudo(name, id);
        default:
            throw new SelectorSyntaxException("the pseudo-class :" + name + " takes no arguments");
        }
    }

    /**
     * Rejects pseudo-elements inside the argument of a logical pseudo-class.
     * <p>
     * 논리 의사 클래스의 인수 안에 있는 의사 요소를 거부한다.
     *
     * @param list the parsed list / 파싱한 목록
     * @return the same list / 같은 목록
     * @throws SelectorSyntaxException if a pseudo-element is found / 의사 요소가 있으면
     */
    private static SelectorList checkNoPseudoElements(SelectorList list) throws SelectorSyntaxException {
        // 목록의 모든 선택자에서 의사 요소가 없는지 확인한다.
        for (int i = 0; i < list.size(); i++) {
            if (list.getSelectors().get(i).getPseudoElement() != null) {
                throw new SelectorSyntaxException("a pseudo-element is not allowed here");
            }
        }
        return list;
    }

    /**
     * Parses the argument of an nth pseudo-class: <code>An+B</code> with an optional <code>of S</code>.
     * <p>
     * nth 의사 클래스의 인수를 파싱한다: <code>An+B</code> 와 선택적인 <code>of S</code>.
     *
     * @param name   pseudo-class name / 의사 클래스 이름
     * @param id     pseudo-class id / 의사 클래스 id
     * @param args   argument values / 인수 값
     * @param allowOf true if an <code>of S</code> clause is allowed / <code>of S</code> 절을 허용하면 true
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private SimpleSelector parseNth(String name, int id, List<ComponentValue> args, boolean allowOf) throws SelectorSyntaxException {
        int split = -1;
        // 최상위 "of" 식별자를 찾아 An+B 와 선택자 목록으로 나눈다.
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) instanceof CssToken && ((CssToken) args.get(i)).isIdent("of")) {
                split = i;
                break;
            }
        }
        List<ComponentValue> anbPart = split < 0 ? args : args.subList(0, split);
        AnPlusB anb = AnPlusB.parse(anbPart);
        if (anb == null) {
            throw new SelectorSyntaxException("invalid An+B expression");
        }
        SelectorList of = null;
        if (split >= 0) {
            if (!allowOf) {
                throw new SelectorSyntaxException("of S is not allowed in :" + name + "()");
            }
            of = checkNoPseudoElements(parseList(args.subList(split + 1, args.size()), namespaces, false, false, depth + 1));
        }
        return SimpleSelector.nth(name, id, anb, of);
    }

    /**
     * Parses the language ranges of <code>:lang()</code>.
     * <p>
     * <code>:lang()</code> 의 언어 범위를 파싱한다.
     *
     * @param name pseudo-class name / 의사 클래스 이름
     * @param id   pseudo-class id / 의사 클래스 id
     * @param args argument values / 인수 값
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private static SimpleSelector parseLang(String name, int id, List<ComponentValue> args) throws SelectorSyntaxException {
        List<List<ComponentValue>> parts = CssParser.splitCommas(args);
        List<String> words = new ArrayList<String>();
        // 쉼표로 나눈 각 조각이 식별자 또는 문자열 하나여야 한다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> p = parts.get(i);
            if (p.size() != 1 || !(isTok(p.get(0), CssToken.IDENT) || isTok(p.get(0), CssToken.STRING))) {
                throw new SelectorSyntaxException("invalid :lang() argument");
            }
            words.add(((CssToken) p.get(0)).getValue());
        }
        return SimpleSelector.withWords(name, id, words);
    }

    /**
     * Parses the direction keyword of <code>:dir()</code>.
     * <p>
     * <code>:dir()</code> 의 방향 키워드를 파싱한다.
     *
     * @param name pseudo-class name / 의사 클래스 이름
     * @param id   pseudo-class id / 의사 클래스 id
     * @param args argument values / 인수 값
     * @return the selector / 선택자
     * @throws SelectorSyntaxException if invalid / 올바르지 않으면
     */
    private static SimpleSelector parseDir(String name, int id, List<ComponentValue> args) throws SelectorSyntaxException {
        List<ComponentValue> t = CssParser.trim(args);
        if (t.size() == 1 && isTok(t.get(0), CssToken.IDENT)) {
            String w = ((CssToken) t.get(0)).getValue().toLowerCase();
            if (w.equals("ltr") || w.equals("rtl")) {
                List<String> words = new ArrayList<String>();
                words.add(w);
                return SimpleSelector.withWords(name, id, words);
            }
        }
        throw new SelectorSyntaxException("invalid :dir() argument");
    }
}
