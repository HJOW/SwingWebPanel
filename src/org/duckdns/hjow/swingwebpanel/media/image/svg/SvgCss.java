package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The CSS part of SVG documents (plan item P18-04): a style sheet parser (rules, declarations, <code>!important</code>, <code>@media</code>
 * with a small media query evaluator, <code>@import</code> and other at-rules skipped), a selector engine (type, universal, id, class,
 * attribute selectors, the combinators descendant, child, next sibling and later sibling, and the structural pseudo-classes) with
 * specificity, and the cascade that merges presentation attributes, style sheets and the <code>style</code> attribute into the declared
 * properties of every element. The pointer and state pseudo-classes (<code>:hover</code>, <code>:focus</code> and so on) never match
 * because a still image has no state. Nothing is fetched: <code>@import</code> and <code>url()</code> values are not loaded here.
 * <p>
 * SVG 문서의 CSS 부분(계획 P18-04): 스타일시트 파서(규칙, 선언, <code>!important</code>, 작은 미디어 질의 평가기를 가진 <code>@media</code>,
 * 건너뛰는 <code>@import</code> 및 기타 at-규칙), 선택자 엔진(타입, 전체, id, 클래스, 속성 선택자, 후손/자식/다음 형제/이후 형제 결합자, 구조 의사
 * 클래스)과 구체성, 표현 속성과 스타일시트와 <code>style</code> 속성을 합쳐 요소마다 선언된 속성을 만드는 연쇄. 정지 이미지에는 상태가 없으므로
 * 포인터/상태 의사 클래스(<code>:hover</code>, <code>:focus</code> 등)는 절대 맞지 않는다. 아무것도 가져오지 않으며 <code>@import</code> 와
 * <code>url()</code> 값은 여기서 읽지 않는다.
 */
final class SvgCss {

    /**
     * The presentation attributes that SVG maps to CSS properties.
     * <p>
     * SVG 가 CSS 속성에 대응시키는 표현 속성.
     */
    private static final Set<String> PRESENTATION = new HashSet<String>(Arrays.asList("alignment-baseline", "baseline-shift", "clip-path",
            "clip-rule", "color", "color-interpolation", "color-interpolation-filters", "direction", "display", "dominant-baseline", "fill",
            "fill-opacity", "fill-rule", "filter", "flood-color", "flood-opacity", "font-family", "font-size", "font-size-adjust",
            "font-stretch", "font-style", "font-variant", "font-weight", "image-rendering", "letter-spacing", "lighting-color", "marker-end",
            "marker-mid", "marker-start", "mask", "opacity", "overflow", "paint-order", "pointer-events", "shape-rendering", "stop-color",
            "stop-opacity", "stroke", "stroke-dasharray", "stroke-dashoffset", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit",
            "stroke-opacity", "stroke-width", "text-anchor", "text-decoration", "text-rendering", "unicode-bidi", "vector-effect",
            "visibility", "word-spacing", "writing-mode", "mix-blend-mode", "isolation", "white-space", "transform-origin", "transform-box",
            "mask-type", "clip", "enable-background"));

    /**
     * One declaration of a style rule or of a style attribute.
     * <p>
     * 스타일 규칙이나 style 속성의 선언 하나.
     */
    static final class Decl {

        /**
         * The property name in lower case (custom properties keep their case).
         * <p>
         * 소문자 속성 이름(사용자 정의 속성은 대소문자를 유지).
         */
        final String name;

        /**
         * The value text without <code>!important</code>.
         * <p>
         * <code>!important</code> 를 뺀 값 텍스트.
         */
        final String value;

        /**
         * True if the declaration is important.
         * <p>
         * 중요한 선언이면 true.
         */
        final boolean important;

        /**
         * Creates a declaration.
         * <p>
         * 선언을 만든다.
         *
         * @param name      name / 이름
         * @param value     value / 값
         * @param important importance / 중요도
         */
        Decl(String name, String value, boolean important) {
            this.name = name;
            this.value = value;
            this.important = important;
        }
    }

    /**
     * One compound selector (no combinators): the simple selectors that apply to one element.
     * <p>
     * 복합 선택자 하나(결합자 없음): 한 요소에 적용되는 단순 선택자들.
     */
    private static final class Compound {

        /**
         * The element type or null for any element.
         * <p>
         * 요소 타입. 모든 요소이면 null.
         */
        String type;

        /**
         * The required id or null.
         * <p>
         * 필요한 id. 없으면 null.
         */
        String id;

        /**
         * The required classes.
         * <p>
         * 필요한 클래스.
         */
        final List<String> classes = new ArrayList<String>();

        /**
         * The attribute selectors as {name, operator, value} triples.
         * <p>
         * 속성 선택자({이름, 연산자, 값} 세 쌍).
         */
        final List<String[]> attrs = new ArrayList<String[]>();

        /**
         * The pseudo-classes as {name, argument} pairs.
         * <p>
         * 의사 클래스({이름, 인수} 쌍).
         */
        final List<String[]> pseudos = new ArrayList<String[]>();

        /**
         * The selector lists inside <code>:not()</code>, <code>:is()</code> and <code>:where()</code>, parallel to the matching pseudos.
         * <p>
         * <code>:not()</code>, <code>:is()</code>, <code>:where()</code> 안의 선택자 목록. 해당 의사 클래스와 나란히 있다.
         */
        final List<Selector[]> nested = new ArrayList<Selector[]>();
    }

    /**
     * A complex selector: compounds joined by combinators.
     * <p>
     * 복잡 선택자: 결합자로 이어진 복합 선택자들.
     */
    static final class Selector {

        /**
         * The compound selectors from left to right.
         * <p>
         * 왼쪽에서 오른쪽 순서의 복합 선택자.
         */
        private final List<Compound> parts = new ArrayList<Compound>();

        /**
         * The combinator before each part except the first: a space, <code>&gt;</code>, <code>+</code> or <code>~</code>.
         * <p>
         * 첫 번째를 뺀 각 부분 앞의 결합자: 스페이스, <code>&gt;</code>, <code>+</code>, <code>~</code>.
         */
        private final List<Character> combs = new ArrayList<Character>();

        /**
         * The specificity as <code>(ids &lt;&lt; 16) | (classes &lt;&lt; 8) | types</code>.
         * <p>
         * 구체성: <code>(id 수 &lt;&lt; 16) | (클래스 수 &lt;&lt; 8) | 타입 수</code>.
         */
        int specificity;
    }

    /**
     * One style rule: a selector with the declarations of its block.
     * <p>
     * 스타일 규칙 하나: 선택자와 그 블록의 선언.
     */
    static final class Rule {

        /**
         * The selector.
         * <p>
         * 선택자.
         */
        final Selector selector;

        /**
         * The declarations.
         * <p>
         * 선언.
         */
        final List<Decl> decls;

        /**
         * The source order.
         * <p>
         * 소스 순서.
         */
        final int order;

        /**
         * Creates a rule.
         * <p>
         * 규칙을 만든다.
         *
         * @param selector selector / 선택자
         * @param decls    declarations / 선언
         * @param order    source order / 소스 순서
         */
        Rule(Selector selector, List<Decl> decls, int order) {
            this.selector = selector;
            this.decls = decls;
            this.order = order;
        }
    }

    /**
     * The environment for media queries: the size of the image viewport.
     * <p>
     * 미디어 질의의 환경: 이미지 뷰포트의 크기.
     */
    static final class MediaEnv {

        /**
         * Viewport width in pixels.
         * <p>
         * 뷰포트 너비(픽셀).
         */
        final double width;

        /**
         * Viewport height in pixels.
         * <p>
         * 뷰포트 높이(픽셀).
         */
        final double height;

        /**
         * Creates an environment.
         * <p>
         * 환경을 만든다.
         *
         * @param width  width / 너비
         * @param height height / 높이
         */
        MediaEnv(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgCss() {
    }

    /**
     * Removes CSS comments (outside strings).
     * <p>
     * CSS 주석을 제거한다(문자열 밖에서만).
     *
     * @param css the text / 텍스트
     * @return text without comments / 주석이 없는 텍스트
     */
    private static String stripComments(String css) {
        if (css.indexOf("/*") < 0) {
            return css;
        }
        StringBuilder sb = new StringBuilder(css.length());
        int i = 0;
        int n = css.length();
        // 문자열은 그대로 두고 '/* ... */' 만 건너뛴다.
        while (i < n) {
            char c = css.charAt(i);
            if (c == '"' || c == '\'') {
                int j = i + 1;
                // 닫는 따옴표를 만날 때까지 문자열 안을 건너뛴다.
                while (j < n && css.charAt(j) != c) {
                    if (css.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                j = Math.min(n, j + 1);
                sb.append(css, i, j);
                i = j;
            } else if (c == '/' && i + 1 < n && css.charAt(i + 1) == '*') {
                int e = css.indexOf("*/", i + 2);
                if (e < 0) {
                    break;
                }
                sb.append(' ');
                i = e + 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Finds the index of the matching closing brace for the brace at the given position, skipping strings, or the text length if missing.
     * <p>
     * 지정한 위치의 여는 중괄호에 짝이 되는 닫는 중괄호의 위치를 찾는다(문자열은 건너뜀). 없으면 텍스트 길이를 반환한다.
     *
     * @param s    the text / 텍스트
     * @param open the index of the opening brace / 여는 중괄호의 위치
     * @return the index of the closing brace / 닫는 중괄호의 위치
     */
    private static int matchBrace(String s, int open) {
        int depth = 0;
        int n = s.length();
        // 중괄호 깊이를 세며 짝을 찾는다.
        for (int i = open; i < n; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                i++;
                // 닫는 따옴표를 만날 때까지 문자열 안을 건너뛴다.
                while (i < n && s.charAt(i) != c) {
                    if (s.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return n;
    }

    /**
     * Parses a style sheet and appends its rules.
     * <p>
     * 스타일시트를 해석해 규칙을 덧붙인다.
     *
     * @param css the style sheet text / 스타일시트 텍스트
     * @param out receives the rules / 규칙을 받는다
     * @param env the media environment / 미디어 환경
     */
    static void parseStylesheet(String css, List<Rule> out, MediaEnv env) {
        parseRules(stripComments(css), out, env, 0);
    }

    /**
     * Parses rules (and at-rules) of a style sheet or of a media block.
     * <p>
     * 스타일시트나 미디어 블록의 규칙(과 at-규칙)을 해석한다.
     *
     * @param css   the text without comments / 주석이 없는 텍스트
     * @param out   receives the rules / 규칙을 받는다
     * @param env   the media environment / 미디어 환경
     * @param depth the nesting depth of at-rules / at-규칙의 중첩 깊이
     */
    private static void parseRules(String css, List<Rule> out, MediaEnv env, int depth) {
        int i = 0;
        int n = css.length();
        // 규칙을 하나씩 읽는다.
        while (i < n) {
            while (i < n && SvgScanner.isWs(css.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            if (css.charAt(i) == '@') {
                int j = i;
                // 이름이 끝나는 곳과 블록/세미콜론의 시작을 찾는다.
                while (j < n && css.charAt(j) != '{' && css.charAt(j) != ';') {
                    j++;
                }
                if (j >= n) {
                    break;
                }
                String head = css.substring(i + 1, j).trim();
                int sp = 0;
                // at-규칙 이름에 쓰이는 문자를 센다.
                while (sp < head.length() && (Character.isLetter(head.charAt(sp)) || head.charAt(sp) == '-')) {
                    sp++;
                }
                String atName = head.substring(0, sp).toLowerCase(Locale.ENGLISH);
                if (css.charAt(j) == ';') {
                    i = j + 1;
                    continue;
                }
                int close = matchBrace(css, j);
                String body = css.substring(j + 1, Math.min(close, n));
                if ("media".equals(atName) && depth < 8 && evalMedia(head.substring(sp).trim(), env)) {
                    parseRules(body, out, env, depth + 1);
                } else if ("supports".equals(atName) && depth < 8) {
                    parseRules(body, out, env, depth + 1);
                }
                i = close + 1;
                continue;
            }
            int brace = css.indexOf('{', i);
            if (brace < 0) {
                break;
            }
            int close = matchBrace(css, brace);
            String selText = css.substring(i, brace).trim();
            String body = css.substring(brace + 1, Math.min(close, n));
            i = close + 1;
            List<Selector> sels = parseSelectorList(selText);
            if (sels == null) {
                continue;
            }
            List<Decl> decls = parseDeclarations(body);
            // 선택자마다 같은 선언 목록을 가진 규칙을 만든다.
            for (int k = 0; k < sels.size(); k++) {
                out.add(new Rule(sels.get(k), decls, out.size()));
            }
        }
    }

    /**
     * Parses a block of declarations (the text of a style attribute or a rule body).
     * <p>
     * 선언 블록(style 속성이나 규칙 본문의 텍스트)을 해석한다.
     *
     * @param text the text / 텍스트
     * @return the declarations / 선언
     */
    static List<Decl> parseDeclarations(String text) {
        List<Decl> list = new ArrayList<Decl>();
        String t = stripComments(text);
        int i = 0;
        int n = t.length();
        // 최상위 세미콜론으로 선언을 나눈다(괄호와 문자열 안은 제외).
        while (i < n) {
            int depth = 0;
            int j = i;
            // 닫는 괄호 이전의 문자를 한 글자씩 살핀다.
            while (j < n) {
                char c = t.charAt(j);
                if (c == '"' || c == '\'') {
                    j++;
                    // 닫는 따옴표를 만날 때까지 문자열 안을 건너뛴다.
                    while (j < n && t.charAt(j) != c) {
                        if (t.charAt(j) == '\\') {
                            j++;
                        }
                        j++;
                    }
                } else if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (c == ';' && depth <= 0) {
                    break;
                }
                j++;
            }
            String d = t.substring(i, Math.min(j, n));
            i = j + 1;
            int colon = d.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = d.substring(0, colon).trim();
            String value = d.substring(colon + 1).trim();
            if (name.length() == 0 || value.length() == 0) {
                continue;
            }
            if (!name.startsWith("--")) {
                name = name.toLowerCase(Locale.ENGLISH);
            }
            boolean important = false;
            int bang = value.lastIndexOf('!');
            if (bang >= 0 && value.substring(bang + 1).trim().equalsIgnoreCase("important")) {
                important = true;
                value = value.substring(0, bang).trim();
            }
            if (value.length() > 0) {
                list.add(new Decl(name, value, important));
            }
        }
        return list;
    }

    /**
     * Evaluates a media query list: true if any query matches.
     * <p>
     * 미디어 질의 목록을 평가한다. 하나라도 맞으면 true.
     *
     * @param q   the text after <code>@media</code> / <code>@media</code> 뒤의 텍스트
     * @param env the media environment / 미디어 환경
     * @return true if the media block applies / 미디어 블록이 적용되면 true
     */
    static boolean evalMedia(String q, MediaEnv env) {
        String t = q.trim().toLowerCase(Locale.ENGLISH);
        if (t.length() == 0) {
            return true;
        }
        String[] list = t.split(",");
        // 질의 중 하나라도 참이면 블록을 적용한다.
        for (int i = 0; i < list.length; i++) {
            if (evalOneMedia(list[i].trim(), env)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluates one media query such as <code>screen and (min-width: 100px)</code>.
     * <p>
     * <code>screen and (min-width: 100px)</code> 같은 미디어 질의 하나를 평가한다.
     *
     * @param q   the query / 질의
     * @param env the media environment / 미디어 환경
     * @return the result / 결과
     */
    private static boolean evalOneMedia(String q, MediaEnv env) {
        String t = q;
        boolean negate = false;
        if (t.startsWith("not ")) {
            negate = true;
            t = t.substring(4).trim();
        } else if (t.startsWith("only ")) {
            t = t.substring(5).trim();
        }
        boolean result = true;
        int i = 0;
        // 타입 이름과 (feature) 조건을 'and' 로 이어진 순서대로 평가한다.
        while (i < t.length()) {
            while (i < t.length() && (t.charAt(i) == ' ')) {
                i++;
            }
            if (i >= t.length()) {
                break;
            }
            if (t.charAt(i) == '(') {
                int close = t.indexOf(')', i);
                if (close < 0) {
                    result = false;
                    break;
                }
                if (!evalFeature(t.substring(i + 1, close).trim(), env)) {
                    result = false;
                }
                i = close + 1;
            } else {
                int j = i;
                // 다음 공백이나 괄호 전까지가 한 단어이다.
                while (j < t.length() && t.charAt(j) != ' ' && t.charAt(j) != '(') {
                    j++;
                }
                String word = t.substring(i, j);
                if ("and".equals(word)) {
                    // 연결어이다.
                    i = j;
                    continue;
                }
                if (!"all".equals(word) && !"screen".equals(word)) {
                    result = false;
                }
                i = j;
            }
        }
        return negate != result;
    }

    /**
     * Evaluates one media feature condition such as <code>min-width: 100px</code>.
     * <p>
     * <code>min-width: 100px</code> 같은 미디어 기능 조건 하나를 평가한다.
     *
     * @param f   the text inside the parentheses / 괄호 안의 텍스트
     * @param env the media environment / 미디어 환경
     * @return the result / 결과
     */
    private static boolean evalFeature(String f, MediaEnv env) {
        int colon = f.indexOf(':');
        String name = colon < 0 ? f.trim() : f.substring(0, colon).trim();
        String val = colon < 0 ? "" : f.substring(colon + 1).trim();
        if ("prefers-color-scheme".equals(name)) {
            return "light".equals(val);
        }
        if ("prefers-reduced-motion".equals(name)) {
            return "no-preference".equals(val);
        }
        if ("color".equals(name)) {
            return true;
        }
        if ("orientation".equals(name)) {
            return env.width >= env.height ? "landscape".equals(val) : "portrait".equals(val);
        }
        if ("hover".equals(name) || "any-hover".equals(name)) {
            return "none".equals(val) ? false : "hover".equals(val);
        }
        if ("min-width".equals(name) || "max-width".equals(name) || "width".equals(name) || "min-height".equals(name)
                || "max-height".equals(name) || "height".equals(name)) {
            SvgLength l = SvgLength.parse(val);
            if (l == null) {
                return false;
            }
            double px = l.toPx(16, 0, env.width, env.height);
            double actual = name.endsWith("width") ? env.width : env.height;
            if (name.startsWith("min-")) {
                return actual >= px;
            }
            if (name.startsWith("max-")) {
                return actual <= px;
            }
            return actual == px;
        }
        if ("resolution".equals(name) || "min-resolution".equals(name) || "max-resolution".equals(name)
                || "-webkit-min-device-pixel-ratio".equals(name) || "-webkit-max-device-pixel-ratio".equals(name)) {
            double dppx;
            try {
                String num = val;
                if (num.endsWith("dppx") || num.endsWith("dpcm") || num.endsWith("dpi")) {
                    num = num.replaceAll("[a-z]+$", "");
                }
                dppx = Double.parseDouble(num);
            } catch (NumberFormatException e) {
                return false;
            }
            if (val.endsWith("dpi")) {
                dppx /= 96;
            } else if (val.endsWith("dpcm")) {
                dppx /= 96 / 2.54;
            }
            if (name.startsWith("min-") || name.startsWith("-webkit-min")) {
                return 1 >= dppx;
            }
            if (name.startsWith("max-") || name.startsWith("-webkit-max")) {
                return 1 <= dppx;
            }
            return dppx == 1;
        }
        return false;
    }

    /**
     * Parses a comma separated selector list.
     * <p>
     * 쉼표로 구분된 선택자 목록을 해석한다.
     *
     * @param text the list text / 목록 텍스트
     * @return the selectors, or null if any of them is invalid (the whole rule is dropped then) / 선택자. 하나라도 올바르지 않으면 null(규칙 전체를 버림)
     */
    static List<Selector> parseSelectorList(String text) {
        List<Selector> list = new ArrayList<Selector>();
        List<String> parts = splitTopLevel(text, ',');
        // 선택자를 하나씩 해석한다.
        for (int i = 0; i < parts.size(); i++) {
            Selector s = parseSelector(parts.get(i).trim());
            if (s == null) {
                return null;
            }
            list.add(s);
        }
        return list.isEmpty() ? null : list;
    }

    /**
     * Splits a text at a separator character outside parentheses, brackets and strings.
     * <p>
     * 괄호, 대괄호, 문자열 밖의 구분 문자에서 텍스트를 나눈다.
     *
     * @param text the text / 텍스트
     * @param sep  the separator / 구분 문자
     * @return the parts / 나눈 부분
     */
    private static List<String> splitTopLevel(String text, char sep) {
        List<String> parts = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        // 한 글자씩 보며 깊이가 0 인 구분 문자에서 자른다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i++;
                // 닫는 따옴표를 만날 때까지 문자열 안을 건너뛴다.
                while (i < text.length() && text.charAt(i) != c) {
                    if (text.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
            } else if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (c == sep && depth == 0) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    /**
     * Parses one complex selector.
     * <p>
     * 복잡 선택자 하나를 해석한다.
     *
     * @param text the selector text / 선택자 텍스트
     * @return the selector or null if invalid / 선택자. 올바르지 않으면 null
     */
    private static Selector parseSelector(String text) {
        if (text.length() == 0) {
            return null;
        }
        Selector sel = new Selector();
        int[] pos = { 0 };
        char pending = 0;
        int n = text.length();
        // 복합 선택자와 결합자를 번갈아 읽는다.
        while (pos[0] < n) {
            while (pos[0] < n && SvgScanner.isWs(text.charAt(pos[0]))) {
                pos[0]++;
            }
            if (pos[0] >= n) {
                break;
            }
            char c = text.charAt(pos[0]);
            if (c == '>' || c == '+' || c == '~') {
                if (pending != 0 || sel.parts.isEmpty()) {
                    return null;
                }
                pending = c;
                pos[0]++;
                continue;
            }
            Compound comp = parseCompound(text, pos);
            if (comp == null) {
                return null;
            }
            if (!sel.parts.isEmpty()) {
                sel.combs.add(Character.valueOf(pending != 0 ? pending : ' '));
            } else if (pending != 0) {
                return null;
            }
            sel.parts.add(comp);
            pending = 0;
        }
        if (pending != 0 || sel.parts.isEmpty()) {
            return null;
        }
        int a = 0;
        int b = 0;
        int c = 0;
        // 구체성은 복합 선택자마다 id, 클래스류, 타입 수를 더해 구한다.
        for (int i = 0; i < sel.parts.size(); i++) {
            int[] sp = specificity(sel.parts.get(i));
            a += sp[0];
            b += sp[1];
            c += sp[2];
        }
        sel.specificity = (Math.min(a, 255) << 16) | (Math.min(b, 255) << 8) | Math.min(c, 255);
        return sel;
    }

    /**
     * Computes the specificity parts of a compound selector.
     * <p>
     * 복합 선택자의 구체성 요소를 계산한다.
     *
     * @param c the compound / 복합 선택자
     * @return {ids, classes, types} / {id 수, 클래스류 수, 타입 수}
     */
    private static int[] specificity(Compound c) {
        int a = c.id != null ? 1 : 0;
        int b = c.classes.size() + c.attrs.size();
        int t = c.type != null ? 1 : 0;
        // 의사 클래스는 종류에 따라 더한다(:where 는 0, :not/:is 는 안의 가장 큰 값).
        for (int i = 0; i < c.pseudos.size(); i++) {
            String name = c.pseudos.get(i)[0];
            if ("where".equals(name)) {
                continue;
            }
            Selector[] inner = c.nested.get(i);
            if (inner != null) {
                int best = 0;
                int[] bestParts = { 0, 0, 0 };
                // 안쪽 선택자 중 구체성이 가장 큰 것을 고른다.
                for (int k = 0; k < inner.length; k++) {
                    if (inner[k].specificity >= best) {
                        best = inner[k].specificity;
                        bestParts = new int[] { (best >> 16) & 255, (best >> 8) & 255, best & 255 };
                    }
                }
                a += bestParts[0];
                b += bestParts[1];
                t += bestParts[2];
            } else {
                b++;
            }
        }
        return new int[] { a, b, t };
    }

    /**
     * Tells whether a character can be part of an identifier.
     * <p>
     * 문자가 식별자의 일부가 될 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for an identifier character / 식별자 문자이면 true
     */
    private static boolean isIdent(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c > 127;
    }

    /**
     * Reads an identifier at a position (backslash escapes are decoded).
     * <p>
     * 위치의 식별자를 읽는다(역슬래시 이스케이프는 해독한다).
     *
     * @param t   the text / 텍스트
     * @param pos the position, advanced / 위치. 앞으로 이동한다
     * @return the identifier (possibly empty) / 식별자(빈 문자열일 수 있음)
     */
    private static String ident(String t, int[] pos) {
        StringBuilder sb = new StringBuilder();
        int i = pos[0];
        // 식별자 문자가 이어지는 동안 읽는다.
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '\\' && i + 1 < t.length()) {
                int j = i + 1;
                int hex = 0;
                int digits = 0;
                // 이스케이프의 16진수 숫자를 최대 6개까지 읽는다.
                while (j < t.length() && digits < 6 && Character.digit(t.charAt(j), 16) >= 0) {
                    hex = hex * 16 + Character.digit(t.charAt(j), 16);
                    j++;
                    digits++;
                }
                if (digits > 0) {
                    sb.appendCodePoint(hex > 0 && hex <= 0x10FFFF ? hex : 0xFFFD);
                    if (j < t.length() && SvgScanner.isWs(t.charAt(j))) {
                        j++;
                    }
                } else {
                    sb.append(t.charAt(j));
                    j++;
                }
                i = j;
            } else if (isIdent(c)) {
                sb.append(c);
                i++;
            } else {
                break;
            }
        }
        pos[0] = i;
        return sb.toString();
    }

    /**
     * Parses a compound selector at a position.
     * <p>
     * 위치에서 복합 선택자를 해석한다.
     *
     * @param t   the selector text / 선택자 텍스트
     * @param pos the position, advanced / 위치. 앞으로 이동한다
     * @return the compound or null if invalid / 복합 선택자. 올바르지 않으면 null
     */
    private static Compound parseCompound(String t, int[] pos) {
        Compound c = new Compound();
        boolean any = false;
        int n = t.length();
        // 단순 선택자를 공백이나 결합자가 나올 때까지 읽는다.
        while (pos[0] < n) {
            char ch = t.charAt(pos[0]);
            if (SvgScanner.isWs(ch) || ch == '>' || ch == '+' || ch == '~' || ch == ',') {
                break;
            }
            if (ch == '*') {
                pos[0]++;
                any = true;
                if (pos[0] < n && t.charAt(pos[0]) == '|') {
                    pos[0]++;
                    String name = ident(t, pos);
                    if (name.length() > 0) {
                        c.type = name;
                    } else if (pos[0] < n && t.charAt(pos[0]) == '*') {
                        pos[0]++;
                    }
                }
            } else if (ch == '#') {
                pos[0]++;
                String id = ident(t, pos);
                if (id.length() == 0) {
                    return null;
                }
                c.id = id;
                any = true;
            } else if (ch == '.') {
                pos[0]++;
                String cls = ident(t, pos);
                if (cls.length() == 0) {
                    return null;
                }
                c.classes.add(cls);
                any = true;
            } else if (ch == '[') {
                int close = findClose(t, pos[0], ']');
                if (close < 0) {
                    return null;
                }
                String[] a = parseAttrSelector(t.substring(pos[0] + 1, close));
                if (a == null) {
                    return null;
                }
                c.attrs.add(a);
                pos[0] = close + 1;
                any = true;
            } else if (ch == ':') {
                if (pos[0] + 1 < n && t.charAt(pos[0] + 1) == ':') {
                    return null;
                }
                pos[0]++;
                String name = ident(t, pos).toLowerCase(Locale.ENGLISH);
                String arg = null;
                Selector[] inner = null;
                if (pos[0] < n && t.charAt(pos[0]) == '(') {
                    int close = findClose(t, pos[0], ')');
                    if (close < 0) {
                        return null;
                    }
                    arg = t.substring(pos[0] + 1, close).trim();
                    pos[0] = close + 1;
                    if ("not".equals(name) || "is".equals(name) || "where".equals(name) || "matches".equals(name)) {
                        List<Selector> l = parseSelectorList(arg);
                        if (l == null) {
                            return null;
                        }
                        inner = l.toArray(new Selector[l.size()]);
                    }
                }
                if (!knownPseudo(name, arg)) {
                    return null;
                }
                c.pseudos.add(new String[] { name, arg });
                c.nested.add(inner);
                any = true;
            } else if (isIdent(ch) || ch == '\\') {
                String name = ident(t, pos);
                if (pos[0] < n && t.charAt(pos[0]) == '|' && !(pos[0] + 1 < n && t.charAt(pos[0] + 1) == '=')) {
                    pos[0]++;
                    name = ident(t, pos);
                }
                if (name.length() == 0) {
                    return null;
                }
                c.type = name;
                any = true;
            } else {
                return null;
            }
        }
        return any ? c : null;
    }

    /**
     * Finds the closing bracket of a bracketed part, skipping strings.
     * <p>
     * 대괄호/괄호 부분의 닫는 문자를 찾는다(문자열은 건너뜀).
     *
     * @param t     the text / 텍스트
     * @param open  the index of the opening character / 여는 문자의 위치
     * @param close the closing character / 닫는 문자
     * @return the index of the closing character or -1 / 닫는 문자의 위치. 없으면 -1
     */
    private static int findClose(String t, int open, char close) {
        int depth = 0;
        char opener = t.charAt(open);
        // 같은 종류의 괄호 깊이를 세며 짝을 찾는다.
        for (int i = open; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"' || c == '\'') {
                i++;
                // 닫는 따옴표를 만날 때까지 문자열 안을 건너뛴다.
                while (i < t.length() && t.charAt(i) != c) {
                    if (t.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
            } else if (c == opener) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Parses the inside of an attribute selector.
     * <p>
     * 속성 선택자의 안쪽을 해석한다.
     *
     * @param s the text between the brackets / 대괄호 사이의 텍스트
     * @return {name, operator, value} or null if invalid / {이름, 연산자, 값}. 올바르지 않으면 null
     */
    private static String[] parseAttrSelector(String s) {
        String t = s.trim();
        int[] pos = { 0 };
        String name = ident(t, pos);
        if (pos[0] < t.length() && t.charAt(pos[0]) == '|' && !(pos[0] + 1 < t.length() && t.charAt(pos[0] + 1) == '=')) {
            pos[0]++;
            name = ident(t, pos);
        }
        if (name.length() == 0) {
            return null;
        }
        if (pos[0] < t.length() && t.charAt(pos[0]) == ':') {
            // xlink:href 같은 접두 이름을 이어 읽는다.
            pos[0]++;
            name = name + ":" + ident(t, pos);
        }
        // 공백을 건너뛴다.
        while (pos[0] < t.length() && SvgScanner.isWs(t.charAt(pos[0]))) {
            pos[0]++;
        }
        if (pos[0] >= t.length()) {
            return new String[] { name, "", "" };
        }
        String op;
        char c = t.charAt(pos[0]);
        if (c == '=') {
            op = "=";
            pos[0]++;
        } else if ("~|^$*".indexOf(c) >= 0 && pos[0] + 1 < t.length() && t.charAt(pos[0] + 1) == '=') {
            op = t.substring(pos[0], pos[0] + 2);
            pos[0] += 2;
        } else {
            return null;
        }
        String v = t.substring(pos[0]).trim();
        // 값 뒤의 대소문자 무시 표지(i, s)는 따로 떼어 낸다.
        boolean ci = false;
        if (v.length() >= 3 && (v.endsWith(" i") || v.endsWith(" I"))) {
            ci = true;
            v = v.substring(0, v.length() - 2).trim();
        }
        if (v.length() >= 2 && (v.charAt(0) == '"' || v.charAt(0) == '\'') && v.charAt(v.length() - 1) == v.charAt(0)) {
            v = v.substring(1, v.length() - 1);
        }
        return new String[] { name, ci ? op + "i" : op, v };
    }

    /**
     * Tells whether a pseudo-class name (with its argument) is understood.
     * <p>
     * 의사 클래스 이름(과 인수)을 이해하는지 알려 준다.
     *
     * @param name the name / 이름
     * @param arg  the argument or null / 인수 또는 null
     * @return true if known / 알려진 것이면 true
     */
    private static boolean knownPseudo(String name, String arg) {
        if ("root".equals(name) || "first-child".equals(name) || "last-child".equals(name) || "only-child".equals(name)
                || "first-of-type".equals(name) || "last-of-type".equals(name) || "only-of-type".equals(name) || "empty".equals(name)
                || "not".equals(name) || "is".equals(name) || "where".equals(name) || "matches".equals(name) || "link".equals(name)
                || "any-link".equals(name) || "hover".equals(name) || "active".equals(name) || "focus".equals(name)
                || "focus-visible".equals(name) || "focus-within".equals(name) || "visited".equals(name) || "target".equals(name)
                || "checked".equals(name) || "disabled".equals(name) || "enabled".equals(name)) {
            return true;
        }
        if ("nth-child".equals(name) || "nth-last-child".equals(name) || "nth-of-type".equals(name) || "nth-last-of-type".equals(name)) {
            return arg != null && parseNth(arg) != null;
        }
        return false;
    }

    /**
     * Parses an <code>an+b</code> expression.
     * <p>
     * <code>an+b</code> 식을 해석한다.
     *
     * @param arg the expression text / 식 텍스트
     * @return {a, b} or null / {a, b}. 올바르지 않으면 null
     */
    private static int[] parseNth(String arg) {
        String t = arg.trim().toLowerCase(Locale.ENGLISH).replace(" ", "");
        if ("odd".equals(t)) {
            return new int[] { 2, 1 };
        }
        if ("even".equals(t)) {
            return new int[] { 2, 0 };
        }
        try {
            int n = t.indexOf('n');
            if (n < 0) {
                return new int[] { 0, Integer.parseInt(t) };
            }
            String as = t.substring(0, n);
            int a;
            if (as.length() == 0 || "+".equals(as)) {
                a = 1;
            } else if ("-".equals(as)) {
                a = -1;
            } else {
                a = Integer.parseInt(as);
            }
            String bs = t.substring(n + 1);
            int b = bs.length() == 0 ? 0 : Integer.parseInt(bs.startsWith("+") ? bs.substring(1) : bs);
            return new int[] { a, b };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Tells whether an index (1 based) is matched by <code>an+b</code>.
     * <p>
     * 위치(1부터)가 <code>an+b</code> 와 맞는지 알려 준다.
     *
     * @param ab the pair {a, b} / {a, b} 쌍
     * @param i  the position / 위치
     * @return true if matched / 맞으면 true
     */
    private static boolean nthMatches(int[] ab, int i) {
        int a = ab[0];
        int b = ab[1];
        if (a == 0) {
            return i == b;
        }
        int d = i - b;
        return d % a == 0 && d / a >= 0;
    }

    /**
     * Tells whether a selector matches an element.
     * <p>
     * 선택자가 요소와 맞는지 알려 준다.
     *
     * @param s the selector / 선택자
     * @param e the element / 요소
     * @return true if it matches / 맞으면 true
     */
    static boolean matches(Selector s, SvgElement e) {
        return matchFrom(s, s.parts.size() - 1, e);
    }

    /**
     * Matches the selector parts up to an index against an element and its relatives (right to left).
     * <p>
     * 선택자의 지정한 위치까지의 부분을 요소와 그 친족에 대해 오른쪽에서 왼쪽으로 맞춘다.
     *
     * @param s   the selector / 선택자
     * @param idx the index of the last part to match / 맞출 마지막 부분의 위치
     * @param e   the element / 요소
     * @return true if matched / 맞으면 true
     */
    private static boolean matchFrom(Selector s, int idx, SvgElement e) {
        if (!matchCompound(s.parts.get(idx), e)) {
            return false;
        }
        if (idx == 0) {
            return true;
        }
        char comb = s.combs.get(idx - 1).charValue();
        if (comb == '>') {
            return e.parent != null && matchFrom(s, idx - 1, e.parent);
        }
        if (comb == ' ') {
            // 조상 중 하나라도 앞 부분과 맞으면 된다.
            for (SvgElement p = e.parent; p != null; p = p.parent) {
                if (matchFrom(s, idx - 1, p)) {
                    return true;
                }
            }
            return false;
        }
        if (e.parent == null) {
            return false;
        }
        List<SvgElement> sib = e.parent.children;
        int me = sib.indexOf(e);
        if (comb == '+') {
            return me > 0 && matchFrom(s, idx - 1, sib.get(me - 1));
        }
        // 이후 형제 결합자: 앞선 형제 중 하나라도 맞으면 된다.
        for (int k = me - 1; k >= 0; k--) {
            if (matchFrom(s, idx - 1, sib.get(k))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches one compound selector against an element.
     * <p>
     * 복합 선택자 하나를 요소와 맞춘다.
     *
     * @param c the compound / 복합 선택자
     * @param e the element / 요소
     * @return true if matched / 맞으면 true
     */
    private static boolean matchCompound(Compound c, SvgElement e) {
        if (!e.isSvg()) {
            return false;
        }
        if (c.type != null && !c.type.equals(e.name)) {
            return false;
        }
        if (c.id != null && !c.id.equals(e.attrs.get("id"))) {
            return false;
        }
        if (!c.classes.isEmpty()) {
            String cls = e.attrs.get("class");
            if (cls == null) {
                return false;
            }
            List<String> have = Arrays.asList(cls.trim().split("\\s+"));
            // 필요한 클래스가 모두 있어야 한다.
            for (int i = 0; i < c.classes.size(); i++) {
                if (!have.contains(c.classes.get(i))) {
                    return false;
                }
            }
        }
        // 속성 선택자를 하나씩 검사한다.
        for (int i = 0; i < c.attrs.size(); i++) {
            if (!matchAttr(c.attrs.get(i), e)) {
                return false;
            }
        }
        // 의사 클래스를 하나씩 검사한다.
        for (int i = 0; i < c.pseudos.size(); i++) {
            if (!matchPseudo(c.pseudos.get(i), c.nested.get(i), e)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches an attribute selector.
     * <p>
     * 속성 선택자를 맞춘다.
     *
     * @param a the {name, operator, value} triple / {이름, 연산자, 값} 세 쌍
     * @param e the element / 요소
     * @return true if matched / 맞으면 true
     */
    private static boolean matchAttr(String[] a, SvgElement e) {
        String v = e.attrs.get(a[0]);
        if (v == null) {
            return false;
        }
        String op = a[1];
        String want = a[2];
        boolean ci = op.endsWith("i") && op.length() > 1;
        if (ci) {
            op = op.substring(0, op.length() - 1);
            v = v.toLowerCase(Locale.ENGLISH);
            want = want.toLowerCase(Locale.ENGLISH);
        }
        if (op.length() == 0) {
            return true;
        }
        if ("=".equals(op)) {
            return v.equals(want);
        }
        if ("~=".equals(op)) {
            return want.length() > 0 && Arrays.asList(v.trim().split("\\s+")).contains(want);
        }
        if ("|=".equals(op)) {
            return v.equals(want) || v.startsWith(want + "-");
        }
        if ("^=".equals(op)) {
            return want.length() > 0 && v.startsWith(want);
        }
        if ("$=".equals(op)) {
            return want.length() > 0 && v.endsWith(want);
        }
        return want.length() > 0 && v.indexOf(want) >= 0;
    }

    /**
     * Matches a pseudo-class.
     * <p>
     * 의사 클래스를 맞춘다.
     *
     * @param ps    the {name, argument} pair / {이름, 인수} 쌍
     * @param inner the nested selectors of <code>:not</code>/<code>:is</code> or null / <code>:not</code>/<code>:is</code> 의 안쪽 선택자 또는 null
     * @param e     the element / 요소
     * @return true if matched / 맞으면 true
     */
    private static boolean matchPseudo(String[] ps, Selector[] inner, SvgElement e) {
        String name = ps[0];
        if ("root".equals(name)) {
            return e.parent == null;
        }
        if ("not".equals(name)) {
            // 안쪽 선택자 중 하나라도 맞으면 :not 은 실패한다.
            for (int i = 0; i < inner.length; i++) {
                if (matches(inner[i], e)) {
                    return false;
                }
            }
            return true;
        }
        if ("is".equals(name) || "where".equals(name) || "matches".equals(name)) {
            // 안쪽 선택자 중 하나라도 맞으면 된다.
            for (int i = 0; i < inner.length; i++) {
                if (matches(inner[i], e)) {
                    return true;
                }
            }
            return false;
        }
        if ("empty".equals(name)) {
            return e.content.isEmpty();
        }
        if ("link".equals(name) || "any-link".equals(name)) {
            return "a".equals(e.name) && e.href() != null;
        }
        if ("first-child".equals(name) || "last-child".equals(name) || "only-child".equals(name) || "nth-child".equals(name)
                || "nth-last-child".equals(name) || "first-of-type".equals(name) || "last-of-type".equals(name)
                || "only-of-type".equals(name) || "nth-of-type".equals(name) || "nth-last-of-type".equals(name)) {
            return matchStructural(name, ps[1], e);
        }
        // 상태 의사 클래스는 정지 이미지에서 절대 맞지 않는다.
        return false;
    }

    /**
     * Matches the structural pseudo-classes that depend on the position among siblings.
     * <p>
     * 형제 사이의 위치에 따르는 구조 의사 클래스를 맞춘다.
     *
     * @param name the pseudo-class name / 의사 클래스 이름
     * @param arg  the argument or null / 인수 또는 null
     * @param e    the element / 요소
     * @return true if matched / 맞으면 true
     */
    private static boolean matchStructural(String name, String arg, SvgElement e) {
        if (e.parent == null) {
            return false;
        }
        boolean ofType = name.indexOf("of-type") >= 0;
        boolean fromEnd = name.startsWith("last") || name.indexOf("last-") >= 0;
        int pos = 0;
        int total = 0;
        // 같은 부모의 자식 중 대상 요소의 순번과 전체 수를 센다.
        for (int i = 0; i < e.parent.children.size(); i++) {
            SvgElement s = e.parent.children.get(i);
            if (ofType && (!s.name.equals(e.name) || !s.namespace.equals(e.namespace))) {
                continue;
            }
            total++;
            if (s == e) {
                pos = total;
            }
        }
        int index = fromEnd ? total - pos + 1 : pos;
        if (name.startsWith("only-")) {
            return total == 1;
        }
        if (name.startsWith("first-") || name.startsWith("last-")) {
            return index == 1;
        }
        int[] ab = parseNth(arg);
        return ab != null && nthMatches(ab, index);
    }

    /**
     * The values of the properties that accept only a fixed set of keywords.
     * <p>
     * 정해진 키워드만 받는 속성의 값.
     */
    private static final Map<String, Set<String>> KEYWORDS = new HashMap<String, Set<String>>();

    static {
        KEYWORDS.put("fill-rule", set("nonzero", "evenodd"));
        KEYWORDS.put("clip-rule", set("nonzero", "evenodd"));
        KEYWORDS.put("stroke-linecap", set("butt", "round", "square"));
        KEYWORDS.put("stroke-linejoin", set("miter", "round", "bevel", "miter-clip", "arcs"));
        KEYWORDS.put("visibility", set("visible", "hidden", "collapse"));
        KEYWORDS.put("text-anchor", set("start", "middle", "end"));
        KEYWORDS.put("shape-rendering", set("auto", "optimizespeed", "crispedges", "geometricprecision"));
        KEYWORDS.put("overflow", set("visible", "hidden", "scroll", "auto", "clip"));
        KEYWORDS.put("font-style", set("normal", "italic", "oblique"));
        KEYWORDS.put("direction", set("ltr", "rtl"));
        KEYWORDS.put("mask-type", set("luminance", "alpha"));
    }

    /**
     * Builds a set of lower-case keywords.
     * <p>
     * 소문자 키워드 집합을 만든다.
     *
     * @param k the keywords / 키워드
     * @return the set / 집합
     */
    private static Set<String> set(String... k) {
        return new HashSet<String>(Arrays.asList(k));
    }

    /**
     * Tells whether a declared value is valid for its property, so that a value that CSS would drop at parse time is dropped here too. The
     * check covers the properties that the renderer interprets (paints, colours, opacities, lengths, numbers, keywords, references); other
     * properties and values that contain <code>var()</code> are accepted.
     * <p>
     * 선언된 값이 속성에 올바른지 알려 준다. CSS 가 해석 시점에 버릴 값을 여기서도 버리기 위해서이다. 렌더러가 해석하는 속성(페인트, 색, 투명도, 길이,
     * 수, 키워드, 참조)을 검사하며 다른 속성과 <code>var()</code> 를 포함한 값은 받아들인다.
     *
     * @param name  the property name / 속성 이름
     * @param value the value text / 값 텍스트
     * @return true if the value is acceptable / 받아들일 수 있으면 true
     */
    static boolean isValidValue(String name, String value) {
        String v = value.trim();
        if (v.length() == 0) {
            return false;
        }
        String l = v.toLowerCase(Locale.ENGLISH);
        if (name.startsWith("--") || l.indexOf("var(") >= 0 || "inherit".equals(l) || "initial".equals(l) || "unset".equals(l)
                || "revert".equals(l)) {
            return true;
        }
        if ("fill".equals(name) || "stroke".equals(name)) {
            return SvgPaint.parse(v) != null;
        }
        if ("color".equals(name) || "stop-color".equals(name) || "flood-color".equals(name) || "lighting-color".equals(name)) {
            return "currentcolor".equals(l) || SvgColors.parse(v) != null;
        }
        if ("opacity".equals(name) || "fill-opacity".equals(name) || "stroke-opacity".equals(name) || "stop-opacity".equals(name)
                || "flood-opacity".equals(name)) {
            SvgLength len = SvgLength.parse(v);
            return len != null && (len.unit.length() == 0 || len.isPercent());
        }
        if ("stroke-width".equals(name) || "stroke-dashoffset".equals(name)) {
            SvgLength len = SvgLength.parse(v);
            return len != null && ("stroke-dashoffset".equals(name) || len.value >= 0);
        }
        if ("stroke-miterlimit".equals(name)) {
            SvgScanner sc = new SvgScanner(v);
            return sc.number() && sc.value >= 1 && (sc.p == v.length());
        }
        if ("stroke-dasharray".equals(name)) {
            return "none".equals(l) || SvgLength.parseList(v) != null;
        }
        if ("font-size".equals(name)) {
            return SvgLength.parse(v) != null || l.matches("x*-?(small|large)|medium|larger|smaller|xxx-large");
        }
        if ("letter-spacing".equals(name) || "word-spacing".equals(name)) {
            return "normal".equals(l) || SvgLength.parse(v) != null;
        }
        Set<String> kw = KEYWORDS.get(name);
        if (kw != null) {
            return kw.contains(l);
        }
        return true;
    }

    /**
     * Returns true if a name is a presentation attribute.
     * <p>
     * 이름이 표현 속성이면 true 를 반환한다.
     *
     * @param name the attribute name / 속성 이름
     * @return true for a presentation attribute / 표현 속성이면 true
     */
    static boolean isPresentationAttribute(String name) {
        return PRESENTATION.contains(name);
    }

    /**
     * One entry of the cascade: a value with its priority.
     * <p>
     * 연쇄의 항목 하나: 우선순위를 가진 값.
     */
    private static final class Entry {

        /**
         * The property name.
         * <p>
         * 속성 이름.
         */
        final String name;

        /**
         * The value text of the entry.
         * <p>
         * 항목의 값 텍스트.
         */
        final String value;

        /**
         * The priority key: higher wins.
         * <p>
         * 우선순위 키. 클수록 이긴다.
         */
        final long key;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param name  name / 이름
         * @param value value / 값
         * @param key   priority key / 우선순위 키
         */
        Entry(String name, String value, long key) {
            this.name = name;
            this.value = value;
            this.key = key;
        }
    }

    /**
     * Runs the cascade for the whole tree: collects the style sheets of the document, evaluates them and stores the declared properties of
     * every element in {@link SvgElement#declared}.
     * <p>
     * 트리 전체에 연쇄를 실행한다. 문서의 스타일시트를 모아 평가하고 모든 요소의 선언된 속성을 {@link SvgElement#declared} 에 저장한다.
     *
     * @param root the root element / 루트 요소
     * @param env  the media environment / 미디어 환경
     */
    static void cascade(SvgElement root, MediaEnv env) {
        List<Rule> rules = new ArrayList<Rule>();
        collectStyles(root, rules, env);
        // 규칙 목록은 구체성과 소스 순서로 안정적으로 정렬해 두면 뒤의 규칙이 이긴다.
        Collections.sort(rules, new Comparator<Rule>() {
            public int compare(Rule a, Rule b) {
                if (a.selector.specificity != b.selector.specificity) {
                    return a.selector.specificity < b.selector.specificity ? -1 : 1;
                }
                return a.order < b.order ? -1 : (a.order == b.order ? 0 : 1);
            }
        });
        cascadeElement(root, rules);
    }

    /**
     * Collects the rules of all <code>style</code> elements in document order.
     * <p>
     * 모든 <code>style</code> 요소의 규칙을 문서 순서로 모은다.
     *
     * @param e     the element to search / 검색할 요소
     * @param rules receives the rules / 규칙을 받는다
     * @param env   the media environment / 미디어 환경
     */
    private static void collectStyles(SvgElement e, List<Rule> rules, MediaEnv env) {
        if (e.is("style")) {
            String type = e.attrs.get("type");
            String media = e.attrs.get("media");
            boolean typeOk = type == null || type.trim().length() == 0 || "text/css".equalsIgnoreCase(type.trim());
            if (typeOk && (media == null || evalMedia(media, env))) {
                parseStylesheet(e.textContent(), rules, env);
            }
        }
        // 자식 요소도 같은 방식으로 훑는다.
        for (int i = 0; i < e.children.size(); i++) {
            collectStyles(e.children.get(i), rules, env);
        }
    }

    /**
     * Computes the declared properties of an element and of its descendants.
     * <p>
     * 요소와 그 자손의 선언된 속성을 계산한다.
     *
     * @param e     the element / 요소
     * @param rules the sorted rules / 정렬된 규칙
     */
    private static void cascadeElement(SvgElement e, List<Rule> rules) {
        if (e.isSvg()) {
            List<Entry> entries = new ArrayList<Entry>();
            // 표현 속성은 가장 낮은 우선순위이다.
            for (Map.Entry<String, String> a : e.attrs.entrySet()) {
                if (isPresentationAttribute(a.getKey()) && isValidValue(a.getKey(), a.getValue())) {
                    entries.add(new Entry(a.getKey(), a.getValue().trim(), 0));
                }
            }
            // 선택자가 맞는 규칙의 선언을 구체성/순서 순으로 더한다.
            for (int i = 0; i < rules.size(); i++) {
                Rule r = rules.get(i);
                if (matches(r.selector, e)) {
                    addDecls(entries, r.decls, 1, 3, r.selector.specificity, r.order);
                }
            }
            String style = e.attrs.get("style");
            if (style != null) {
                addDecls(entries, parseDeclarations(style), 2, 4, 0, 0);
            }
            Map<String, String> declared = new HashMap<String, String>();
            Map<String, Long> best = new HashMap<String, Long>();
            // 속성마다 우선순위 키가 가장 큰 값을 고른다.
            for (int i = 0; i < entries.size(); i++) {
                Entry en = entries.get(i);
                Long old = best.get(en.name);
                if (old == null || en.key >= old.longValue()) {
                    best.put(en.name, Long.valueOf(en.key));
                    declared.put(en.name, en.value);
                }
            }
            e.declared = declared;
        } else {
            e.declared = new HashMap<String, String>();
        }
        // 자손에도 같은 방식으로 적용한다.
        for (int i = 0; i < e.children.size(); i++) {
            cascadeElement(e.children.get(i), rules);
        }
    }

    /**
     * Adds declarations to the cascade entries; the shorthand <code>marker</code> is expanded.
     * <p>
     * 선언을 연쇄 항목에 추가한다. 단축 속성 <code>marker</code> 는 펼친다.
     *
     * @param entries     receives the entries / 항목을 받는다
     * @param decls       the declarations / 선언
     * @param normalLevel the origin level of normal declarations (1 style sheet, 2 style attribute) / 일반 선언의 출처 수준(1 스타일시트, 2 style 속성)
     * @param importLevel the origin level of important declarations / 중요한 선언의 출처 수준
     * @param spec        the specificity of the rule / 규칙의 구체성
     * @param order       the source order of the rule / 규칙의 소스 순서
     */
    private static void addDecls(List<Entry> entries, List<Decl> decls, int normalLevel, int importLevel, long spec, long order) {
        // 선언을 순서대로 추가한다(같은 우선순위에서는 뒤의 것이 이기도록 순번을 늘린다).
        for (int i = 0; i < decls.size(); i++) {
            Decl d = decls.get(i);
            // 올바르지 않은 값의 선언은 버려져 이전의 올바른 선언이 남는다.
            if (!isValidValue(d.name, d.value)) {
                continue;
            }
            long key = ((long) (d.important ? importLevel : normalLevel) << 56) | (spec << 32) | ((order & 0xFFFFF) << 12) | (i & 0xFFF);
            if ("marker".equals(d.name)) {
                entries.add(new Entry("marker-start", d.value, key));
                entries.add(new Entry("marker-mid", d.value, key));
                entries.add(new Entry("marker-end", d.value, key));
            } else {
                entries.add(new Entry(d.name, d.value, key));
            }
        }
    }
}
