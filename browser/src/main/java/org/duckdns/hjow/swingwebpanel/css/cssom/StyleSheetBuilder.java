package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.selector.NamespaceMap;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxRule;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.dom.DomException;

/**
 * Builds a {@link StyleSheet} from the rules of the syntax layer: it drops what is invalid exactly as a browser does (a rule with an invalid selector,
 * an <code>@import</code> after other rules, an unknown at-rule), and interprets the at-rules the engine knows: <code>@import</code> (loaded through
 * the {@link StyleSheetLoader} with cycle, depth and count limits), <code>@namespace</code>, <code>@media</code>, <code>@supports</code>,
 * <code>@layer</code>, <code>@font-face</code>, <code>@keyframes</code>. <code>@charset</code> is ignored and the other at-rules are kept as
 * {@link UnknownRule}. Everything that is dropped or not applied is reported to the diagnostics.
 * <p>
 * 구문 계층의 규칙으로 {@link StyleSheet} 를 만든다: 브라우저처럼 올바르지 않은 것을 버리고(올바르지 않은 선택자를 가진 규칙, 다른 규칙 뒤의 <code>@import</code>, 알 수 없는
 * at-규칙), 엔진이 아는 at-규칙을 해석한다: <code>@import</code>({@link StyleSheetLoader} 로 읽으며 순환, 깊이, 개수 제한이 있음), <code>@namespace</code>,
 * <code>@media</code>, <code>@supports</code>, <code>@layer</code>, <code>@font-face</code>, <code>@keyframes</code>. <code>@charset</code> 은 무시하고 다른 at-규칙은
 * {@link UnknownRule} 로 보관한다. 버리거나 적용하지 않은 모든 것은 진단에 보고한다.
 */
final class StyleSheetBuilder {

    /** The URL of the sheet being built. / 만드는 시트의 URL. */
    private final String url;

    /** The origin. / 출처. */
    private final int origin;

    /** Loads imports. / 가져오기를 읽는다. */
    private final StyleSheetLoader loader;

    /** Diagnostics or null. / 진단 또는 null. */
    private final CssDiagnostics diag;

    /** The URLs of the sheets being imported (cycle detection). / 가져오는 중인 시트의 URL(순환 검출). */
    private final List<String> importStack;

    /** One-element counter of imports done so far in the whole tree. / 전체 트리에서 지금까지 가져온 수를 세는 한 칸짜리 카운터. */
    private final int[] importCount;

    /** The namespace declarations. / 네임스페이스 선언. */
    private final NamespaceMap namespaces;

    /** The parse environment (base URL of the sheet). / 파싱 환경(시트의 기준 URL). */
    private final ParseEnv env;

    /** True if the sheet is parsed in quirks mode (also passed to imported sheets). / 시트를 호환 모드로 파싱하면 true(가져온 시트에도 넘긴다). */
    private final boolean quirks;

    /** The number of <code>@scope</code> rules that enclose the rules being built (a top-level <code>&amp;</code> inside is <code>:where(:scope)</code>). 지금 만드는 규칙을 둘러싼 <code>@scope</code> 규칙의 수(그 안의 최상위 <code>&amp;</code> 는 <code>:where(:scope)</code> 이다). */
    private int scopeDepth;

    /** True while an <code>@import</code> is still allowed. / <code>@import</code> 가 아직 허용되는 동안 true. */
    private boolean importsAllowed = true;

    /** True while an <code>@namespace</code> is still allowed. / <code>@namespace</code> 가 아직 허용되는 동안 true. */
    private boolean namespacesAllowed = true;

    /** True if the builder parses one rule for the CSSOM <code>insertRule</code> (a namespace is declared only after the position was accepted). / CSSOM <code>insertRule</code> 를 위해 규칙 하나를 파싱하면 true(네임스페이스는 위치가 받아들여진 뒤에 선언한다). */
    private boolean editing;

    /**
     * Creates a builder.
     * <p>
     * 구성기를 만든다.
     *
     * @param url         sheet URL or null / 시트 URL 또는 null
     * @param origin      origin constant / 출처 상수
     * @param loader      the loader or null / 로더 또는 null
     * @param diag        diagnostics or null / 진단 또는 null
     * @param importStack the URLs being imported / 가져오는 중인 URL
     * @param importCount the shared import counter / 공유하는 가져오기 카운터
     * @param quirks      true to parse the declarations in quirks mode / 선언을 호환 모드로 파싱하면 true
     */
    StyleSheetBuilder(String url, int origin, StyleSheetLoader loader, CssDiagnostics diag, List<String> importStack, int[] importCount,
            boolean quirks) {
        this(url, origin, loader, diag, importStack, importCount, new NamespaceMap(), quirks);
    }

    /**
     * Creates a builder that declares the namespaces into an existing map.
     * <p>
     * 기존 맵에 네임스페이스를 선언하는 구성기를 만든다.
     *
     * @param url         sheet URL or null / 시트 URL 또는 null
     * @param origin      origin constant / 출처 상수
     * @param loader      the loader or null / 로더 또는 null
     * @param diag        diagnostics or null / 진단 또는 null
     * @param importStack the URLs being imported / 가져오는 중인 URL
     * @param importCount the shared import counter / 공유하는 가져오기 카운터
     * @param namespaces  the namespace declarations / 네임스페이스 선언
     * @param quirks      true to parse in quirks mode / 호환 모드로 파싱하면 true
     */
    StyleSheetBuilder(String url, int origin, StyleSheetLoader loader, CssDiagnostics diag, List<String> importStack, int[] importCount,
            NamespaceMap namespaces, boolean quirks) {
        this.url = url;
        this.origin = origin;
        this.loader = loader;
        this.diag = diag;
        this.importStack = importStack;
        this.importCount = importCount;
        this.quirks = quirks;
        this.namespaces = namespaces;
        this.env = new ParseEnv(PropertyRegistry.getInstance(), url, quirks);
        if (url != null) {
            importStack.add(url);
        }
    }

    /**
     * Creates the builder that parses rules into an existing sheet (CSSOM <code>insertRule</code>, and the loading of imports that arrived late): it
     * knows the namespaces, the base URL, the origin and the loader of the sheet, and its import stack holds the URLs of the sheet and of the sheets
     * that imported it, so that an import cycle is still found.
     * <p>
     * 기존 시트에 규칙을 파싱해 넣는 구성기를 만든다(CSSOM <code>insertRule</code> 와 늦게 도착한 가져오기의 로딩): 시트의 네임스페이스, 기준 URL, 출처, 로더를 알고,
     * 가져오기 스택에는 시트와 그것을 가져온 시트들의 URL 이 들어 있어 가져오기 순환을 여전히 찾아낸다.
     *
     * @param sheet the sheet, or null for a rule that is in no sheet / 시트. 시트에 들어 있지 않은 규칙이면 null
     * @param diag  diagnostics or null / 진단 또는 null
     * @return the builder / 구성기
     */
    static StyleSheetBuilder forEditing(StyleSheet sheet, CssDiagnostics diag) {
        List<String> stack = new ArrayList<String>();
        int loaded = 0;
        StyleSheet root = sheet;
        // 가져온 시트 사슬을 올라가며 URL 을 모으고 루트 시트를 찾는다.
        for (StyleSheet s = sheet; s != null; s = s.getParentStyleSheet()) {
            if (s.getUrl() != null) {
                stack.add(0, s.getUrl());
            }
            root = s;
        }
        if (root != null) {
            loaded = countLoadedImports(root);
        }
        StyleSheetBuilder b = new StyleSheetBuilder(sheet == null ? StyleSheet.ORIGIN_AUTHOR : sheet.getOrigin(), sheet == null ? null : sheet.getLoader(),
                diag, stack, new int[] {loaded}, sheet == null ? new NamespaceMap() : sheet.getNamespaces(), sheet == null ? null : sheet.getUrl(), sheet != null && sheet.isQuirks());
        b.editing = true;
        return b;
    }

    /**
     * Creates a builder with an explicit base URL that is not pushed on the import stack (the stack already has the URLs).
     * <p>
     * 가져오기 스택에 넣지 않는 명시적인 기준 URL 을 가진 구성기를 만든다(스택에는 이미 URL 들이 들어 있다).
     *
     * @param origin      origin constant / 출처 상수
     * @param loader      the loader or null / 로더 또는 null
     * @param diag        diagnostics or null / 진단 또는 null
     * @param importStack the URLs being imported / 가져오는 중인 URL
     * @param importCount the shared import counter / 공유하는 가져오기 카운터
     * @param namespaces  the namespace declarations / 네임스페이스 선언
     * @param base        the base URL for relative urls or null / 상대 url 의 기준 URL 또는 null
     * @param quirks      true to parse in quirks mode / 호환 모드로 파싱하면 true
     */
    private StyleSheetBuilder(int origin, StyleSheetLoader loader, CssDiagnostics diag, List<String> importStack, int[] importCount,
            NamespaceMap namespaces, String base, boolean quirks) {
        this.url = base;
        this.origin = origin;
        this.loader = loader;
        this.diag = diag;
        this.importStack = importStack;
        this.importCount = importCount;
        this.namespaces = namespaces;
        this.quirks = quirks;
        this.env = new ParseEnv(PropertyRegistry.getInstance(), base, quirks);
    }

    /**
     * Counts the imports that were loaded in a sheet and in the sheets it imported.
     * <p>
     * 시트와 그것이 가져온 시트에서 읽은 가져오기의 수를 센다.
     *
     * @param s the sheet / 시트
     * @return the number of loaded imports / 읽은 가져오기 수
     */
    private static int countLoadedImports(StyleSheet s) {
        int n = 0;
        List<CssRule> rules = s.getRules();
        // 최상위 가져오기 규칙마다 읽은 시트가 있으면 하나와 그 시트가 가져온 수를 더한다.
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i) instanceof ImportRule && ((ImportRule) rules.get(i)).getStyleSheet() != null) {
                n += 1 + countLoadedImports(((ImportRule) rules.get(i)).getStyleSheet());
            }
        }
        return n;
    }

    /**
     * Parses text as exactly one rule (the "parse a CSS rule" algorithm of the CSSOM), for <code>insertRule</code>.
     * <p>
     * 텍스트를 규칙 정확히 하나로 파싱한다(CSSOM 의 "parse a CSS rule" 알고리즘). <code>insertRule</code> 용이다.
     *
     * @param text     the text / 텍스트
     * @param topLevel true if the rule goes to the top level of a sheet / 규칙이 시트의 최상위에 들어가면 true
     * @return the rule / 규칙
     * @throws DomException <code>SyntaxError</code> if the text is not exactly one rule that this engine can represent, <code>HierarchyRequestError</code>
     *                      for an <code>@import</code> or <code>@namespace</code> that is not at the top level / 텍스트가 이 엔진이 나타낼 수 있는 규칙 정확히 하나가 아니면
     *                      <code>SyntaxError</code>, 최상위가 아닌 <code>@import</code>/<code>@namespace</code> 이면 <code>HierarchyRequestError</code>
     */
    CssRule parseOne(String text, boolean topLevel) {
        List<SyntaxRule> syn = CssParser.parseStylesheet(text, null);
        if (syn.size() != 1) {
            throw DomException.syntax("Failed to parse the rule '" + text + "'.");
        }
        SyntaxRule r = syn.get(0);
        if (r.isAtRule() && !topLevel && (r.getName().equals("import") || r.getName().equals("namespace"))) {
            throw DomException.hierarchyRequest("@" + r.getName() + " rules cannot be nested in another rule.");
        }
        // 위치 검사는 호출한 쪽이 하므로 가져오기와 네임스페이스는 여기서 허용 상태로 둔다.
        importsAllowed = true;
        namespacesAllowed = true;
        CssRule built;
        if (r.isAtRule()) {
            built = buildAtRule(r, topLevel, null);
        } else {
            // 중첩 규칙이 있는 스타일 규칙은 엔진이 여러 규칙으로 편다: insertRule 은 바깥 규칙만 넣는다(중첩 규칙은 넣지 않는 한계).
            List<CssRule> flat = buildStyleRules(r, null);
            built = flat.isEmpty() ? null : flat.get(0);
        }
        if (built == null) {
            throw DomException.syntax("Failed to parse the rule '" + text + "'.");
        }
        return built;
    }

    /**
     * Loads the imports of a sheet that could not be loaded when the sheet was parsed (see {@link StyleSheet#resolvePendingImports}).
     * <p>
     * 시트를 파싱할 때 읽지 못한 가져오기를 읽는다({@link StyleSheet#resolvePendingImports} 참고).
     *
     * @param sheet the sheet / 시트
     * @param diag  diagnostics or null / 진단 또는 null
     * @return true if an import was loaded now / 이번에 가져오기를 읽었으면 true
     */
    static boolean resolveImports(StyleSheet sheet, CssDiagnostics diag) {
        boolean any = false;
        List<CssRule> rules = sheet.getRules();
        // 최상위 가져오기 규칙을 훑는다: 시트가 없으면 다시 읽고, 있으면 그 시트의 가져오기를 이어서 확인한다.
        for (int i = 0; i < rules.size(); i++) {
            if (!(rules.get(i) instanceof ImportRule)) {
                continue;
            }
            ImportRule ir = (ImportRule) rules.get(i);
            StyleSheetLoader base = sheet.getLoader();
            StyleSheetLoader scoped = base instanceof ContextualStyleSheetLoader ? ((ContextualStyleSheetLoader) base).forMedia(ir.getMedia()) : base;
            if (ir.getStyleSheet() != null) {
                any |= ir.getStyleSheet().resolvePendingImports(scoped, diag);
            } else if (ir.getResolvedUrl() != null) {
                StyleSheet loaded = forEditing(sheet, diag).loadImport(ir.getHref(), ir.getResolvedUrl(), scoped);
                if (loaded != null) {
                    loaded.setMedia(ir.getMedia());
                    ir.setStyleSheet(loaded);
                    any = true;
                }
            }
        }
        if (any) {
            sheet.changed();
        }
        return any;
    }

    /**
     * Builds the sheet.
     * <p>
     * 시트를 만든다.
     *
     * @param text the CSS text / CSS 문자열
     * @return the sheet / 시트
     */
    StyleSheet build(String text) {
        List<SyntaxRule> syntax = CssParser.parseStylesheet(text, diag);
        List<CssRule> rules = buildRules(syntax, true);
        if (url != null) {
            importStack.remove(importStack.size() - 1);
        }
        return new StyleSheet(rules, origin, url, namespaces);
    }

    /**
     * Reports a diagnostic.
     * <p>
     * 진단을 보고한다.
     *
     * @param kind    kind constant / 종류 상수
     * @param subject subject / 대상
     * @param message message / 메시지
     */
    private void report(String kind, String subject, String message) {
        CssDiagnostics.report(diag, kind, subject, message);
    }

    /**
     * Builds the rules of a list.
     * <p>
     * 목록의 규칙을 만든다.
     *
     * @param list     the syntax rules / 구문 규칙
     * @param topLevel true for the top level of a sheet / 시트의 최상위이면 true
     * @return the rules / 규칙
     */
    private List<CssRule> buildRules(List<SyntaxRule> list, boolean topLevel) {
        List<CssRule> out = new ArrayList<CssRule>();
        // 규칙을 하나씩 해석해 올바른 것만 모은다.
        for (int i = 0; i < list.size(); i++) {
            SyntaxRule r = list.get(i);
            if (r.isAtRule()) {
                CssRule built = buildAtRule(r, topLevel, null);
                if (built != null) {
                    out.add(built);
                }
            } else {
                // 스타일 규칙은 중첩 규칙이 있으면 여러 규칙으로 펴진다.
                out.addAll(buildStyleRules(r, null));
            }
            boolean prologue = r.isAtRule() && (r.getName().equals("import") || r.getName().equals("charset") || r.getName().equals("layer")
                    || r.getName().equals("namespace"));
            // 서문(@charset, @import, @layer, @namespace) 이외의 규칙이 나오면 이후의 @import 와 @namespace 는 무효이다.
            if (topLevel && !prologue) {
                importsAllowed = false;
                namespacesAllowed = false;
            }
        }
        return out;
    }

    /**
     * The selector of the style rule that contains the rules being built: what the nesting selector <code>&amp;</code> stands for.
     * <p>
     * 지금 만드는 규칙들을 담은 스타일 규칙의 선택자: 중첩 선택자 <code>&amp;</code> 가 가리키는 것.
     */
    private static final class Parent {
        /** The resolved selector as component values. / 해석이 끝난 선택자의 컴포넌트 값. */
        final List<ComponentValue> prelude;
        /** The parsed selector list. / 파싱한 선택자 목록. */
        final SelectorList selectors;

        /**
         * Creates the parent.
         * <p>
         * 부모를 만든다.
         *
         * @param prelude   the resolved selector values / 해석이 끝난 선택자 값
         * @param selectors the parsed selectors / 파싱한 선택자
         */
        Parent(List<ComponentValue> prelude, SelectorList selectors) {
            this.prelude = prelude;
            this.selectors = selectors;
        }
    }

    /**
     * Builds a style rule and the rules nested in it (CSS Nesting Module Level 1): the rule itself with the declarations that come before the first
     * nested rule, then each nested rule with the selector resolved against this one (<code>&amp;</code> becomes <code>:is(parent)</code>, a nested
     * selector without <code>&amp;</code> becomes a descendant of the parent), and the declarations that follow a nested rule as a further rule with the
     * same selector so that the source order of the cascade is kept.
     * <p>
     * 스타일 규칙과 그 안에 중첩된 규칙을 만든다(CSS Nesting Module Level 1): 첫 중첩 규칙 앞의 선언을 가진 규칙 자신, 그리고 이 규칙에 대해 선택자를 해석한 각 중첩 규칙
     * (<code>&amp;</code> 는 <code>:is(부모)</code> 가 되고 <code>&amp;</code> 가 없는 중첩 선택자는 부모의 자손이 된다), 중첩 규칙 뒤에 오는 선언은 캐스케이드의 소스 순서를 지키도록
     * 같은 선택자를 가진 규칙으로 따로 만든다.
     *
     * @param r      the syntax rule / 구문 규칙
     * @param parent the style rule that contains this one, or null at the top level / 이 규칙을 담은 스타일 규칙. 최상위이면 null
     * @return the rules in source order; empty if the selector is invalid / 소스 순서의 규칙. 선택자가 올바르지 않으면 비어 있다
     */
    private List<CssRule> buildStyleRules(SyntaxRule r, Parent parent) {
        List<ComponentValue> prelude = r.getPrelude();
        // 중첩 규칙이거나 최상위에서 &가 쓰였으면 선택자를 부모 기준으로 바꾼다.
        if (parent != null || containsNesting(prelude)) {
            prelude = nestPrelude(prelude, parent == null ? null : parent.prelude);
        }
        SelectorList sel;
        try {
            sel = SelectorParser.parse(prelude, namespaces);
        } catch (SelectorSyntaxException e) {
            report(CssDiagnostics.INVALID, r.getPreludeText(), "rule dropped: " + e.getMessage());
            return new ArrayList<CssRule>();
        }
        return buildContents(r.getBlock(), new Parent(prelude, sel), true);
    }

    /**
     * Builds the rules of a block that holds declarations and nested rules: the declarations become style rules of the parent's selector, split at
     * every nested rule so that the cascade order of the source is kept.
     * <p>
     * 선언과 중첩 규칙이 든 블록의 규칙을 만든다: 선언은 부모 선택자의 스타일 규칙이 되며, 소스의 캐스케이드 순서를 지키도록 중첩 규칙마다 나뉜다.
     *
     * @param block       the contents of the block / 블록의 내용
     * @param me          the style rule the block belongs to / 블록이 속한 스타일 규칙
     * @param alwaysFirst true to always create the first rule even without declarations / 선언이 없어도 첫 규칙을 항상 만들려면 true
     * @return the rules in source order / 소스 순서의 규칙
     */
    private List<CssRule> buildContents(List<ComponentValue> block, Parent me, boolean alwaysFirst) {
        List<CssRule> out = new ArrayList<CssRule>();
        List<Object> items = CssParser.parseBlockContents(block, diag);
        CssStyleDeclaration cur = null;
        if (alwaysFirst) {
            cur = new CssStyleDeclaration();
            cur.setQuirks(quirks);
            out.add(new StyleRule(me.selectors, cur));
        }
        // 항목을 소스 순서대로 처리한다: 선언은 현재 규칙에 더하고, 중첩 규칙은 새 규칙을 만든다.
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if (o instanceof SyntaxDeclaration) {
                if (cur == null) {
                    cur = new CssStyleDeclaration();
                    cur.setQuirks(quirks);
                    out.add(new StyleRule(me.selectors, cur));
                }
                cur.add((SyntaxDeclaration) o, env, diag);
                continue;
            }
            SyntaxRule n = (SyntaxRule) o;
            cur = null;
            if (n.isAtRule()) {
                CssRule g = buildAtRule(n, false, me);
                if (g != null) {
                    out.add(g);
                }
            } else {
                out.addAll(buildStyleRules(n, me));
            }
        }
        return out;
    }

    /**
     * Tells whether component values contain the nesting selector <code>&amp;</code>, also inside functions.
     * <p>
     * 컴포넌트 값에 중첩 선택자 <code>&amp;</code> 가 들어 있는지 알려 준다. 함수 안도 본다.
     *
     * @param list the values / 값
     * @return true if <code>&amp;</code> occurs / <code>&amp;</code> 가 있으면 true
     */
    private static boolean containsNesting(List<ComponentValue> list) {
        // 값을 하나씩 보며 & 구분 문자나 함수 인수 안의 &를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            ComponentValue c = list.get(i);
            if (c instanceof CssToken && ((CssToken) c).isDelim('&')) {
                return true;
            }
            if (c instanceof CssFunction && containsNesting(((CssFunction) c).getArguments())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the selector of a nested rule against the selector of its parent: every comma separated selector that contains <code>&amp;</code> has
     * it replaced by <code>:is(parent)</code> (<code>:scope</code> at the top level of a sheet), every other one is prefixed with
     * <code>:is(parent)</code> and a descendant combinator (a leading combinator such as <code>&gt;</code> is kept).
     * <p>
     * 중첩 규칙의 선택자를 부모의 선택자 기준으로 해석한다: <code>&amp;</code> 가 든 쉼표 구분 선택자는 그것을 <code>:is(부모)</code>(시트 최상위에서는 <code>:scope</code>)로 바꾸고,
     * 그렇지 않은 선택자는 앞에 <code>:is(부모)</code> 와 자손 결합자를 붙인다(<code>&gt;</code> 같은 앞의 결합자는 유지).
     *
     * @param prelude the selector values as written / 쓰인 그대로의 선택자 값
     * @param parent  the resolved selector values of the parent, or null at the top level / 부모의 해석된 선택자 값. 최상위이면 null
     * @return the resolved selector values / 해석한 선택자 값
     */
    private List<ComponentValue> nestPrelude(List<ComponentValue> prelude, List<ComponentValue> parent) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(prelude);
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 쉼표로 나눈 선택자마다 &를 바꾸거나 부모를 앞에 붙인다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> part = CssParser.trim(parts.get(i));
            if (i > 0) {
                out.add(new CssToken(CssToken.COMMA, ",", 0));
            }
            if (containsNesting(part)) {
                out.addAll(substitute(part, parent));
            } else {
                out.addAll(selfReference(parent));
                out.add(new CssToken(CssToken.WHITESPACE, " ", 0));
                out.addAll(part);
            }
        }
        return out;
    }

    /**
     * Replaces the nesting selectors of component values by the parent selector.
     * <p>
     * 컴포넌트 값의 중첩 선택자를 부모 선택자로 바꾼다.
     *
     * @param list   the values / 값
     * @param parent the parent selector values or null / 부모 선택자 값. 없으면 null
     * @return the new values / 새 값
     */
    private List<ComponentValue> substitute(List<ComponentValue> list, List<ComponentValue> parent) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 값을 하나씩 보며 &는 부모로 바꾸고 함수 안은 재귀로 처리한다.
        for (int i = 0; i < list.size(); i++) {
            ComponentValue c = list.get(i);
            if (c instanceof CssToken && ((CssToken) c).isDelim('&')) {
                out.addAll(selfReference(parent));
            } else if (c instanceof CssFunction && containsNesting(((CssFunction) c).getArguments())) {
                out.add(new CssFunction(((CssFunction) c).getName(), substitute(((CssFunction) c).getArguments(), parent)));
            } else {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Returns the values that stand for the parent selector: <code>:is(parent)</code>, or <code>:scope</code> without a parent.
     * <p>
     * 부모 선택자를 나타내는 값을 반환한다: <code>:is(부모)</code>. 부모가 없으면 <code>:scope</code>.
     *
     * @param parent the parent selector values or null / 부모 선택자 값. 없으면 null
     * @return the values / 값
     */
    private List<ComponentValue> selfReference(List<ComponentValue> parent) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        out.add(new CssToken(CssToken.COLON, ":", 0));
        if (parent == null && scopeDepth > 0) {
            // @scope 안의 최상위 &는 :where(:scope) 처럼 구체성이 0 이다(CSS Nesting 3.3).
            List<ComponentValue> inner = new ArrayList<ComponentValue>();
            inner.add(new CssToken(CssToken.COLON, ":", 0));
            inner.add(new CssToken(CssToken.IDENT, "scope", 0));
            out.add(new CssFunction("where", inner));
        } else if (parent == null) {
            out.add(new CssToken(CssToken.IDENT, "scope", 0));
        } else {
            out.add(new CssFunction("is", parent));
        }
        return out;
    }

    /**
     * Builds the rules inside a grouping at-rule: a rule list normally, the declarations and nested rules of a block when the at-rule is nested
     * in a style rule (the declarations then apply to the elements of the enclosing selector).
     * <p>
     * 그룹 at-규칙 안의 규칙을 만든다: 보통은 규칙 목록이고, at-규칙이 스타일 규칙 안에 중첩되어 있으면 블록의 선언과 중첩 규칙이다(선언은 바깥 선택자의 요소에 적용된다).
     *
     * @param r      the at-rule with a block / 블록이 있는 at-규칙
     * @param parent the enclosing style rule or null / 바깥 스타일 규칙. 없으면 null
     * @return the rules / 규칙
     */
    private List<CssRule> children(SyntaxRule r, Parent parent) {
        if (parent == null) {
            return buildRules(CssParser.parseRuleList(r.getBlock(), false, diag), false);
        }
        return buildContents(r.getBlock(), parent, false);
    }

    /**
     * Builds an at-rule.
     * <p>
     * at-규칙을 만든다.
     *
     * @param r        the syntax rule / 구문 규칙
     * @param topLevel true at the top level of a sheet / 시트의 최상위이면 true
     * @param parent   the style rule that contains the at-rule (nesting), or null / 이 at-규칙을 담은 스타일 규칙(중첩). 없으면 null
     * @return the rule, or null if it is dropped or ignored / 규칙. 버리거나 무시하면 null
     */
    private CssRule buildAtRule(SyntaxRule r, boolean topLevel, Parent parent) {
        String n = r.getName();
        if (n.equals("charset")) {
            return null;
        }
        if (n.equals("import")) {
            return buildImport(r, topLevel);
        }
        if (n.equals("namespace")) {
            return buildNamespace(r, topLevel);
        }
        if (n.equals("media") && r.getBlock() != null) {
            MediaQueryList m = MediaQueryList.parse(r.getPrelude());
            return new GroupRule(CssRule.MEDIA_RULE, m, m.toCssText(), true, children(r, parent));
        }
        if (n.equals("supports") && r.getBlock() != null) {
            boolean ok = SupportsCondition.evaluate(r.getPrelude(), env.isQuirks());
            return new GroupRule(CssRule.SUPPORTS_RULE, null, r.getPreludeText(), ok, children(r, parent));
        }
        if (n.equals("layer")) {
            if (r.getBlock() == null) {
                // 블록이 없는 @layer a, b; 는 레이어의 이름과 순서만 정한다.
                return r.getPreludeText().trim().length() == 0 ? null : GroupRule.layerStatement(r.getPreludeText().trim());
            }
            return new GroupRule(CssRule.LAYER_RULE, null, r.getPreludeText(), true, children(r, parent));
        }
        if (n.equals("container") && r.getBlock() != null) {
            ContainerCondition cc = ContainerCondition.parse(r.getPrelude());
            if (!cc.isValid()) {
                report(CssDiagnostics.INVALID, "@container " + r.getPreludeText(), "invalid container condition: the rule never matches");
            }
            return GroupRule.container(cc, children(r, parent));
        }
        if (n.equals("scope") && r.getBlock() != null) {
            ScopeSpec spec;
            try {
                spec = ScopeSpec.parse(r.getPrelude(), namespaces);
            } catch (SelectorSyntaxException e) {
                report(CssDiagnostics.INVALID, "@scope " + r.getPreludeText(), "rule dropped: " + e.getMessage());
                return null;
            }
            // @scope 안의 규칙에서 최상위 &는 구체성 0 의 :where(:scope) 이다.
            scopeDepth++;
            try {
                return GroupRule.scope(spec, children(r, parent));
            } finally {
                scopeDepth--;
            }
        }
        if (n.equals("property") && r.getBlock() != null) {
            return buildProperty(r);
        }
        if (n.equals("counter-style") && r.getBlock() != null) {
            return buildCounterStyle(r);
        }
        if (n.equals("font-face") && r.getBlock() != null) {
            return buildFontFace(r);
        }
        if (n.equals("position-try") && r.getBlock() != null) {
            return buildPositionTry(r);
        }
        if ((n.equals("keyframes") || n.equals("-webkit-keyframes") || n.equals("-moz-keyframes")) && r.getBlock() != null) {
            return buildKeyframes(r);
        }
        if (n.equals("starting-style") && r.getBlock() != null) {
            // @starting-style 의 안쪽 규칙은 변경 후 스타일이 아니라 전환의 시작 스타일을 만든다.
            return GroupRule.startingStyle(children(r, parent));
        }
        if (n.equals("page") && r.getBlock() != null) {
            return buildPage(r);
        }
        if (n.equals("font-feature-values") && r.getBlock() != null) {
            return buildFontFeatureValues(r);
        }
        if (n.equals("viewport")) {
            report(CssDiagnostics.UNSUPPORTED, "@" + n, "at-rule parsed but not applied");
            return new UnknownRule(n, r.toString());
        }
        report(CssDiagnostics.UNKNOWN, "@" + n, "unknown at-rule skipped");
        return null;
    }

    /**
     * Builds an <code>@import</code> rule and loads the imported sheet.
     * <p>
     * <code>@import</code> 규칙을 만들고 가져온 시트를 읽는다.
     *
     * @param r        the syntax rule / 구문 규칙
     * @param topLevel true at the top level / 최상위이면 true
     * @return the rule or null if it is invalid / 규칙. 올바르지 않으면 null
     */
    private CssRule buildImport(SyntaxRule r, boolean topLevel) {
        if (!topLevel || !importsAllowed || r.getBlock() != null) {
            report(CssDiagnostics.INVALID, "@import", "an @import must come first in the style sheet");
            return null;
        }
        List<ComponentValue> it = new ArrayList<ComponentValue>();
        List<ComponentValue> p = r.getPrelude();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; i < p.size(); i++) {
            if (!CssParser.isWhitespace(p.get(i))) {
                it.add(p.get(i));
            }
        }
        String href = it.isEmpty() ? null : urlOf(it.get(0));
        if (href == null) {
            report(CssDiagnostics.INVALID, "@import", "no URL");
            return null;
        }
        boolean supports = true;
        String layerName = null;
        List<ComponentValue> media = new ArrayList<ComponentValue>();
        // URL 뒤의 layer() 와 supports() 는 처리하고 나머지는 미디어 질의로 본다.
        for (int i = 1; i < it.size(); i++) {
            ComponentValue c = it.get(i);
            if (c instanceof CssFunction && ((CssFunction) c).isNamed("supports")) {
                supports = SupportsCondition.evaluate(((CssFunction) c).getArguments(), env.isQuirks());
            } else if (c instanceof CssFunction && ((CssFunction) c).isNamed("layer")) {
                layerName = CssParser.trim(((CssFunction) c).getArguments()).size() == 0 ? "" : CssBlock.join(CssParser.trim(((CssFunction) c).getArguments())).trim();
            } else if (c instanceof CssToken && ((CssToken) c).isIdent("layer")) {
                layerName = "";
            } else {
                media.add(c);
            }
        }
        MediaQueryList ml = supports ? MediaQueryList.parse(media) : MediaQueryList.parse("not all");
        String resolved = env.resolve(href);
        StyleSheetLoader scoped = loader instanceof ContextualStyleSheetLoader
                ? ((ContextualStyleSheetLoader) loader).forMedia(ml) : loader;
        StyleSheet sheet = loadImport(href, resolved, scoped);
        if (sheet != null) {
            sheet.setMedia(ml);
        }
        ImportRule rule = new ImportRule(href, resolved, ml, sheet);
        // layer(name) 로 가져온 시트는 그 캐스케이드 레이어에 들어간다.
        rule.setLayer(layerName);
        return rule;
    }

    /**
     * Loads the sheet of an import, applying the cycle, depth and count limits.
     * <p>
     * 가져오기의 시트를 읽으며 순환, 깊이, 개수 제한을 적용한다.
     *
     * @param href     the URL as written / 쓰인 그대로의 URL
     * @param resolved the absolute URL or null / 절대 URL 또는 null
     * @param scoped loader inheriting the parent and import media conditions / 부모·import 미디어 조건을 상속한 로더
     * @return the sheet or null / 시트 또는 null
     */
    private StyleSheet loadImport(String href, String resolved, StyleSheetLoader scoped) {
        if (scoped == null || resolved == null) {
            report(CssDiagnostics.UNSUPPORTED, "@import " + href, resolved == null ? "the URL could not be resolved" : "no style sheet loader, import skipped");
            return null;
        }
        if (importStack.contains(resolved)) {
            report(CssDiagnostics.INVALID, "@import " + href, "import cycle");
            return null;
        }
        if (importStack.size() >= StyleSheet.MAX_IMPORT_DEPTH || importCount[0] >= StyleSheet.MAX_IMPORTS) {
            report(CssDiagnostics.INVALID, "@import " + href, "import depth or count limit exceeded");
            return null;
        }
        importCount[0]++;
        try {
            LoadedStyleSheet l = scoped.load(resolved);
            String finalUrl = l.getUrl() != null ? l.getUrl() : resolved;
            StyleSheet imported = new StyleSheetBuilder(finalUrl, origin, scoped, diag, importStack, importCount, quirks).build(l.getText());
            // 가져온 시트는 읽은 위치(href)와 그 안에 나중에 넣을 가져오기를 읽을 로더를 가진다.
            imported.setHref(finalUrl);
            imported.setLoader(scoped);
            return imported;
        } catch (IOException e) {
            report(CssDiagnostics.INVALID, "@import " + href, "could not be loaded: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads the URL of a url() token, a url("...") function or a string.
     * <p>
     * url() 토큰, url("...") 함수, 문자열에서 URL 을 읽는다.
     *
     * @param v the component value / 컴포넌트 값
     * @return the URL text or null / URL 문자열 또는 null
     */
    private static String urlOf(ComponentValue v) {
        if (v instanceof CssToken && (((CssToken) v).is(CssToken.URL) || ((CssToken) v).is(CssToken.STRING))) {
            return ((CssToken) v).getValue();
        }
        if (v instanceof CssFunction && ((CssFunction) v).isNamed("url")) {
            List<ComponentValue> a = CssParser.trim(((CssFunction) v).getArguments());
            if (a.size() == 1 && a.get(0) instanceof CssToken && ((CssToken) a.get(0)).is(CssToken.STRING)) {
                return ((CssToken) a.get(0)).getValue();
            }
        }
        return null;
    }

    /**
     * Builds an <code>@namespace</code> rule and registers the namespace for the selectors that follow.
     * <p>
     * <code>@namespace</code> 규칙을 만들고 뒤따르는 선택자를 위해 네임스페이스를 등록한다.
     *
     * @param r        the syntax rule / 구문 규칙
     * @param topLevel true at the top level / 최상위이면 true
     * @return the rule or null if it is invalid / 규칙. 올바르지 않으면 null
     */
    private CssRule buildNamespace(SyntaxRule r, boolean topLevel) {
        if (!topLevel || !namespacesAllowed || r.getBlock() != null) {
            report(CssDiagnostics.INVALID, "@namespace", "an @namespace must follow the @import rules and precede the other rules");
            return null;
        }
        List<ComponentValue> it = new ArrayList<ComponentValue>();
        List<ComponentValue> p = r.getPrelude();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; i < p.size(); i++) {
            if (!CssParser.isWhitespace(p.get(i))) {
                it.add(p.get(i));
            }
        }
        String prefix = "";
        int u = 0;
        if (it.size() == 2 && it.get(0) instanceof CssToken && ((CssToken) it.get(0)).is(CssToken.IDENT)) {
            prefix = ((CssToken) it.get(0)).getValue();
            u = 1;
        }
        String uri = it.size() == u + 1 ? urlOf(it.get(u)) : null;
        if (uri == null) {
            report(CssDiagnostics.INVALID, "@namespace", "invalid namespace rule");
            return null;
        }
        // insertRule 로 넣는 규칙은 위치가 받아들여진 뒤에 호출한 쪽이 선언한다.
        if (!editing) {
            namespaces.declare(prefix, uri);
        }
        importsAllowed = false;
        return new NamespaceRule(prefix, uri);
    }

    /**
     * Builds an <code>@property</code> rule: the prelude is the custom property name and the descriptors <code>syntax</code> (a string),
     * <code>inherits</code> (<code>true</code> or <code>false</code>) and <code>initial-value</code>; an invalid registration is dropped.
     * <p>
     * <code>@property</code> 규칙을 만든다: 전문은 사용자 정의 속성 이름이고 서술자는 <code>syntax</code>(문자열), <code>inherits</code>(<code>true</code> 또는
     * <code>false</code>), <code>initial-value</code> 이다. 올바르지 않은 등록은 버린다.
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null / 규칙 또는 null
     */
    private CssRule buildProperty(SyntaxRule r) {
        List<ComponentValue> p = CssParser.trim(r.getPrelude());
        if (p.size() != 1 || !(p.get(0) instanceof CssToken) || !((CssToken) p.get(0)).is(CssToken.IDENT)) {
            report(CssDiagnostics.INVALID, "@property", "the name of a registered custom property is required");
            return null;
        }
        String name = ((CssToken) p.get(0)).getValue();
        String syntax = null;
        String inherits = null;
        List<ComponentValue> initial = null;
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(r.getBlock(), diag).getDeclarations();
        // 서술자를 이름별로 모은다(나중 것이 앞의 것을 대체한다).
        for (int i = 0; i < decls.size(); i++) {
            String d = decls.get(i).getName().toLowerCase();
            List<ComponentValue> v = CssParser.trim(new ArrayList<ComponentValue>(decls.get(i).getValue()));
            if (d.equals("syntax") && v.size() == 1 && v.get(0) instanceof CssToken && ((CssToken) v.get(0)).is(CssToken.STRING)) {
                syntax = ((CssToken) v.get(0)).getValue();
            } else if (d.equals("inherits") && v.size() == 1 && v.get(0) instanceof CssToken && ((CssToken) v.get(0)).is(CssToken.IDENT)) {
                inherits = ((CssToken) v.get(0)).getValue();
            } else if (d.equals("initial-value")) {
                initial = v;
            }
        }
        PropertyRule rule = PropertyRule.create(name, syntax, inherits, initial);
        if (rule == null) {
            report(CssDiagnostics.INVALID, "@property " + name, "an invalid registration: syntax, inherits and (unless the syntax is *) initial-value are required");
        }
        return rule;
    }

    /**
     * Builds a <code>@page</code> rule (CSS Paged Media 3 section 4.3): the prelude is a page selector list, the block holds page descriptors and
     * the sixteen page-margin at-rules; a rule with an invalid selector list is dropped, and other at-rules inside the block are ignored.
     * <p>
     * <code>@page</code> 규칙을 만든다(CSS Paged Media 3 4.3 절): 전문은 쪽 선택자 목록이고 블록은 쪽 서술자와 열여섯 쪽 여백 at-규칙을 담는다. 선택자 목록이
     * 올바르지 않은 규칙은 버리고, 블록 안의 다른 at-규칙은 무시한다.
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null / 규칙 또는 null
     */
    private CssRule buildPage(SyntaxRule r) {
        CssStyleDeclaration style = new CssStyleDeclaration();
        List<MarginRule> margins = new ArrayList<MarginRule>();
        List<Object> items = CssParser.parseBlockContents(r.getBlock(), diag);
        // 블록의 항목을 소스 순서로 처리한다: 선언은 쪽 서술자, 여백 at-규칙은 여백 규칙이 되고 그 밖의 규칙은 무시한다.
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if (o instanceof SyntaxDeclaration) {
                style.add((SyntaxDeclaration) o, env, diag);
                continue;
            }
            SyntaxRule n = (SyntaxRule) o;
            String mname = n.getName() == null ? "" : n.getName().toLowerCase();
            if (n.isAtRule() && n.getBlock() != null && PageRule.isMarginName(mname)) {
                CssStyleDeclaration ms = new CssStyleDeclaration();
                List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(n.getBlock(), diag).getDeclarations();
                // 여백 규칙의 선언을 서술자로 더한다.
                for (int j = 0; j < decls.size(); j++) {
                    ms.add(decls.get(j), env, diag);
                }
                margins.add(new MarginRule(mname, ms));
            } else {
                report(CssDiagnostics.INVALID, n.isAtRule() ? "@" + n.getName() : n.getPreludeText(), "only declarations and page-margin rules are allowed in @page");
            }
        }
        PageRule rule = PageRule.create(r.getPrelude(), style, margins);
        if (rule == null) {
            report(CssDiagnostics.INVALID, "@page " + r.getPreludeText(), "rule dropped: invalid page selector");
        } else {
            report(CssDiagnostics.UNSUPPORTED, "@page", "page boxes are parsed but not laid out (no printing)");
        }
        return rule;
    }

    /**
     * The generic font families and the CSS-wide keywords that may not be an unquoted family name of <code>@font-feature-values</code>.
     * <p>
     * <code>@font-feature-values</code> 의 따옴표 없는 패밀리 이름이 될 수 없는 일반 글꼴 패밀리와 CSS 전역 키워드.
     */
    private static final String[] NOT_FAMILY_NAMES = { "serif", "sans-serif", "cursive", "fantasy", "monospace", "system-ui", "emoji", "math", "fangsong",
        "ui-serif", "ui-sans-serif", "ui-monospace", "ui-rounded", "inherit", "initial", "unset", "default", "revert", "revert-layer" };

    /**
     * Parses a list of <code>&lt;font-family-name&gt;</code>s (CSS Fonts 4 section 2.1): each is a string or a sequence of identifiers; a generic family or a
     * CSS-wide keyword as an unquoted name makes the list invalid.
     * <p>
     * <code>&lt;font-family-name&gt;</code> 목록을 파싱한다(CSS Fonts 4 2.1 절): 각각은 문자열이거나 식별자의 나열이다. 따옴표 없는 이름이 일반 패밀리나 CSS 전역 키워드이면
     * 목록이 올바르지 않다.
     *
     * @param values the component values / 컴포넌트 값
     * @return the names, or null if the list is not valid / 이름. 올바르지 않으면 null
     */
    static List<String> parseFamilyNames(List<ComponentValue> values) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(values);
        List<String> out = new ArrayList<String>();
        // 쉼표로 나눈 부분마다 문자열 하나이거나 식별자의 나열이어야 한다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> p = CssParser.trim(parts.get(i));
            if (p.isEmpty()) {
                return null;
            }
            if (p.size() == 1 && p.get(0) instanceof CssToken && ((CssToken) p.get(0)).is(CssToken.STRING)) {
                out.add(((CssToken) p.get(0)).getValue());
                continue;
            }
            StringBuilder name = new StringBuilder();
            int idents = 0;
            // 공백을 건너뛰며 식별자를 공백 하나로 잇는다.
            for (int k = 0; k < p.size(); k++) {
                ComponentValue c = p.get(k);
                if (CssParser.isWhitespace(c)) {
                    continue;
                }
                if (!(c instanceof CssToken) || !((CssToken) c).is(CssToken.IDENT)) {
                    return null;
                }
                String v = ((CssToken) c).getValue();
                // 하나뿐인 식별자가 일반 패밀리나 전역 키워드이면 패밀리 이름이 아니다.
                for (int j = 0; j < NOT_FAMILY_NAMES.length && idents == 0 && p.size() == 1; j++) {
                    if (NOT_FAMILY_NAMES[j].equalsIgnoreCase(v)) {
                        return null;
                    }
                }
                if (idents > 0) {
                    name.append(' ');
                }
                name.append(v);
                idents++;
            }
            out.add(name.toString());
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * Builds an <code>@font-feature-values</code> rule (CSS Fonts 4 section 6.9): the prelude is a list of named families (otherwise the whole rule is
     * invalid); the block holds feature value blocks (<code>@swash</code>, <code>@styleset</code> ...; an unknown at-rule is ignored) whose declarations map
     * names to non-negative integers (a declaration that does not fit the type is ignored, a later one replaces an earlier one), and the
     * <code>font-display</code> descriptor.
     * <p>
     * <code>@font-feature-values</code> 규칙을 만든다(CSS Fonts 4 6.9 절): 전문은 이름 있는 패밀리의 목록이며(아니면 규칙 전체가 올바르지 않다), 블록은 기능 값 블록
     * (<code>@swash</code>, <code>@styleset</code> 등. 모르는 at-규칙은 무시)과 <code>font-display</code> 서술자를 담는다. 기능 값 블록의 선언은 이름을 음이 아닌 정수에 대응시킨다
     * (종류에 맞지 않는 선언은 무시하고 나중 것이 앞의 것을 대체한다).
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null / 규칙 또는 null
     */
    private CssRule buildFontFeatureValues(SyntaxRule r) {
        List<String> families = parseFamilyNames(r.getPrelude());
        if (families == null) {
            report(CssDiagnostics.INVALID, "@font-feature-values " + r.getPreludeText(), "rule dropped: the prelude must be a list of named font families");
            return null;
        }
        List<Object> items = CssParser.parseBlockContents(r.getBlock(), diag);
        String display = null;
        List<Object[]> blocks = new ArrayList<Object[]>();
        // 블록의 항목을 소스 순서로 처리한다: font-display 서술자와 기능 값 블록.
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if (o instanceof SyntaxDeclaration) {
                SyntaxDeclaration d = (SyntaxDeclaration) o;
                List<ComponentValue> v = CssParser.trim(d.getValue());
                if ("font-display".equalsIgnoreCase(d.getName()) && v.size() == 1 && v.get(0) instanceof CssToken && ((CssToken) v.get(0)).is(CssToken.IDENT)) {
                    String k = ((CssToken) v.get(0)).getValue().toLowerCase(java.util.Locale.ENGLISH);
                    if (k.equals("auto") || k.equals("block") || k.equals("swap") || k.equals("fallback") || k.equals("optional")) {
                        display = k;
                    }
                }
                continue;
            }
            SyntaxRule n = (SyntaxRule) o;
            int type = n.isAtRule() && n.getBlock() != null ? FontFeatureValuesRule.typeIndex(n.getName() == null ? "" : n.getName()) : -1;
            if (type < 0) {
                report(CssDiagnostics.INVALID, n.isAtRule() ? "@" + n.getName() : n.getPreludeText(), "ignored inside @font-feature-values");
                continue;
            }
            blocks.add(new Object[] { Integer.valueOf(type), n });
        }
        FontFeatureValuesRule rule = new FontFeatureValuesRule(families, display);
        // 기능 값 블록마다 선언을 이름과 번호로 읽어 그 종류의 맵에 넣는다.
        for (int i = 0; i < blocks.size(); i++) {
            int type = ((Integer) blocks.get(i)[0]).intValue();
            SyntaxRule n = (SyntaxRule) blocks.get(i)[1];
            FontFeatureValuesRule.ValueMap map = rule.getMap(FontFeatureValuesRule.TYPES[type]);
            List<List<ComponentValue>> decls = splitTop(n.getBlock(), CssToken.SEMICOLON);
            // 선언마다 이름(대소문자를 지킨다), 콜론, 음이 아닌 정수의 나열인지 확인한다.
            for (int k = 0; k < decls.size(); k++) {
                List<ComponentValue> d = CssParser.trim(decls.get(k));
                if (d.isEmpty()) {
                    continue;
                }
                int colon = -1;
                // 첫 콜론을 찾는다.
                for (int j = 0; j < d.size() && colon < 0; j++) {
                    if (d.get(j) instanceof CssToken && ((CssToken) d.get(j)).is(CssToken.COLON)) {
                        colon = j;
                    }
                }
                List<ComponentValue> head = colon < 0 ? d : CssParser.trim(d.subList(0, colon));
                String name = head.size() == 1 && head.get(0) instanceof CssToken && ((CssToken) head.get(0)).is(CssToken.IDENT) ? ((CssToken) head.get(0)).getValue() : null;
                int[] indexes = colon < 0 ? null : integers(d.subList(colon + 1, d.size()));
                if (name == null || indexes == null || !map.define(name, indexes)) {
                    report(CssDiagnostics.INVALID, CssBlock.join(d).trim(), "invalid feature value in @" + FontFeatureValuesRule.TYPES[type]);
                }
            }
        }
        return rule;
    }

    /**
     * Splits component values at the top level at every token of a type (the contents of blocks and functions are not looked into).
     * <p>
     * 컴포넌트 값을 최상위에서 주어진 종류의 토큰마다 나눈다(블록과 함수 안은 보지 않는다).
     *
     * @param values the values / 값
     * @param type   the token type / 토큰 종류
     * @return the parts / 부분
     */
    private static List<List<ComponentValue>> splitTop(List<ComponentValue> values, int type) {
        List<List<ComponentValue>> out = new ArrayList<List<ComponentValue>>();
        List<ComponentValue> cur = new ArrayList<ComponentValue>();
        // 구분 토큰을 만날 때마다 지금까지 모은 부분을 끝낸다.
        for (int i = 0; i < values.size(); i++) {
            ComponentValue c = values.get(i);
            if (c instanceof CssToken && ((CssToken) c).is(type)) {
                out.add(cur);
                cur = new ArrayList<ComponentValue>();
            } else {
                cur.add(c);
            }
        }
        out.add(cur);
        return out;
    }

    /**
     * Reads a list of integers separated by white space.
     * <p>
     * 공백으로 나눈 정수 목록을 읽는다.
     *
     * @param values the component values / 컴포넌트 값
     * @return the integers, or null if something else occurs / 정수. 다른 것이 있으면 null
     */
    private static int[] integers(List<ComponentValue> values) {
        List<Integer> out = new ArrayList<Integer>();
        // 공백이 아닌 성분은 정수여야 한다.
        for (int i = 0; i < values.size(); i++) {
            ComponentValue c = values.get(i);
            if (CssParser.isWhitespace(c)) {
                continue;
            }
            if (!(c instanceof CssToken) || !((CssToken) c).is(CssToken.NUMBER) || !((CssToken) c).isInteger()) {
                return null;
            }
            double d = ((CssToken) c).getNumber();
            if (d > Integer.MAX_VALUE) {
                return null;
            }
            out.add(Integer.valueOf((int) d));
        }
        int[] a = new int[out.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i).intValue();
        }
        return a;
    }

    /**
     * Builds a <code>@counter-style</code> rule: the prelude is the counter style name (a custom identifier that an author may define) and the
     * block holds the descriptors; a definition that is not valid for its system is dropped later, by the layout that reads it.
     * <p>
     * <code>@counter-style</code> 규칙을 만든다: 전문은 카운터 스타일 이름(저자가 정의할 수 있는 사용자 정의 식별자)이고 블록은 서술자를 담는다. 시스템에 맞지 않는
     * 정의는 그것을 읽는 레이아웃이 나중에 버린다.
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null / 규칙 또는 null
     */
    private CssRule buildCounterStyle(SyntaxRule r) {
        List<ComponentValue> p = CssParser.trim(r.getPrelude());
        if (p.size() != 1 || !(p.get(0) instanceof CssToken) || !((CssToken) p.get(0)).is(CssToken.IDENT)
                || !CounterStyleRule.isDefinable(((CssToken) p.get(0)).getValue())) {
            report(CssDiagnostics.INVALID, "@counter-style " + r.getPreludeText(), "a counter style needs a name that can be defined");
            return null;
        }
        Map<String, List<ComponentValue>> d = new LinkedHashMap<String, List<ComponentValue>>();
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(r.getBlock(), diag).getDeclarations();
        // 서술자를 이름별로 모은다(나중 것이 앞의 것을 대체한다).
        for (int i = 0; i < decls.size(); i++) {
            d.put(decls.get(i).getName().toLowerCase(), new ArrayList<ComponentValue>(decls.get(i).getValue()));
        }
        return new CounterStyleRule(((CssToken) p.get(0)).getValue(), d);
    }

    /**
     * Builds an <code>@font-face</code> rule; a rule without a family or a source is dropped.
     * <p>
     * <code>@font-face</code> 규칙을 만든다. 글꼴 계열이나 출처가 없는 규칙은 버린다.
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null / 규칙 또는 null
     */
    private CssRule buildFontFace(SyntaxRule r) {
        Map<String, List<ComponentValue>> d = new LinkedHashMap<String, List<ComponentValue>>();
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(r.getBlock(), diag).getDeclarations();
        // 선언을 디스크립터로 모은다(나중 것이 앞의 것을 대체한다).
        for (int i = 0; i < decls.size(); i++) {
            d.put(decls.get(i).getName(), new ArrayList<ComponentValue>(decls.get(i).getValue()));
        }
        List<ComponentValue> src = d.get("src");
        if (src == null || !d.containsKey("font-family")) {
            report(CssDiagnostics.INVALID, "@font-face", "a font face needs font-family and src");
            return null;
        }
        List<FontFaceRule.Source> sources = FontFaceRule.parseSources(src, env);
        if (sources.isEmpty()) {
            report(CssDiagnostics.INVALID, "@font-face", "no usable source");
            return null;
        }
        return new FontFaceRule(d, sources);
    }

    /**
     * Builds a <code>@position-try</code> rule (CSS Anchor Positioning 1 section 6.4): the prelude is a dashed identifier, the block holds declarations; the ones that
     * are not about the position of the box and the ones with <code>!important</code> are dropped (the rule stays).
     * <p>
     * <code>@position-try</code> 규칙을 만든다(CSS Anchor Positioning 1 6.4 절): 전문은 대시 식별자이고 블록은 선언을 담는다. 상자의 위치에 관한 것이 아닌 선언과
     * <code>!important</code> 선언은 버린다(규칙은 남는다).
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null if the name is not a dashed identifier / 규칙. 이름이 대시 식별자가 아니면 null
     */
    private CssRule buildPositionTry(SyntaxRule r) {
        List<ComponentValue> p = CssParser.trim(r.getPrelude());
        if (p.size() != 1 || !(p.get(0) instanceof CssToken) || !((CssToken) p.get(0)).is(CssToken.IDENT) || !((CssToken) p.get(0)).getValue().startsWith("--")) {
            report(CssDiagnostics.INVALID, "@position-try " + r.getPreludeText(), "a position option needs a name that is a dashed identifier");
            return null;
        }
        CssStyleDeclaration style = new CssStyleDeclaration();
        style.restrictTo(PositionTryRule.acceptedLonghands());
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(r.getBlock(), diag).getDeclarations();
        ParseEnv tryEnv = new ParseEnv(PropertyRegistry.getInstance(), url);
        // 선언마다 받는 속성인지 검사하며 저장한다(받지 않는 속성과 !important 는 버려지고 보고된다).
        for (int i = 0; i < decls.size(); i++) {
            style.add(decls.get(i), tryEnv, diag);
        }
        return new PositionTryRule(((CssToken) p.get(0)).getValue(), style);
    }

    /**
     * Builds a <code>@keyframes</code> rule.
     * <p>
     * <code>@keyframes</code> 규칙을 만든다.
     *
     * @param r the syntax rule / 구문 규칙
     * @return the rule or null if the name is missing / 규칙. 이름이 없으면 null
     */
    private CssRule buildKeyframes(SyntaxRule r) {
        List<ComponentValue> p = CssParser.trim(r.getPrelude());
        if (p.size() != 1 || !(p.get(0) instanceof CssToken) || !(((CssToken) p.get(0)).is(CssToken.IDENT) || ((CssToken) p.get(0)).is(CssToken.STRING))) {
            report(CssDiagnostics.INVALID, "@keyframes", "a name is required");
            return null;
        }
        String name = ((CssToken) p.get(0)).getValue();
        List<KeyframesRule.Keyframe> frames = new ArrayList<KeyframesRule.Keyframe>();
        List<SyntaxRule> inner = CssParser.parseRuleList(r.getBlock(), false, diag);
        // 키프레임 규칙마다 오프셋 목록과 선언을 읽는다.
        for (int i = 0; i < inner.size(); i++) {
            KeyframesRule.Keyframe frame = buildKeyframe(inner.get(i), name, diag, url);
            if (frame != null) {
                frames.add(frame);
            }
        }
        return new KeyframesRule(name, frames);
    }

    /**
     * Builds one keyframe from its syntax rule (used for a whole <code>@keyframes</code> block and for the CSSOM <code>appendRule</code>).
     * <p>
     * 구문 규칙에서 키프레임 하나를 만든다(<code>@keyframes</code> 블록 전체와 CSSOM <code>appendRule</code> 이 쓴다).
     *
     * @param k    the syntax rule / 구문 규칙
     * @param name the animation name for diagnostics / 진단에 쓸 애니메이션 이름
     * @param diag collector or null / 수집기 또는 null
     * @param base the base URL for relative urls or null / 상대 url 의 기준 URL 또는 null
     * @return the keyframe, or null if its selector is invalid / 키프레임. 선택자가 올바르지 않으면 null
     */
    static KeyframesRule.Keyframe buildKeyframe(SyntaxRule k, String name, CssDiagnostics diag, String base) {
        List<Double> offsets = k.isAtRule() ? null : offsets(k.getPrelude());
        if (offsets == null) {
            CssDiagnostics.report(diag, CssDiagnostics.INVALID, "@keyframes " + name, "invalid keyframe selector '" + k.getPreludeText() + "'");
            return null;
        }
        ParseEnv keyframeEnv = new ParseEnv(PropertyRegistry.getInstance(), base);
        CssStyleDeclaration style = new CssStyleDeclaration();
        List<SyntaxDeclaration> decls = CssParser.parseDeclarationBlock(k.getBlock(), diag).getDeclarations();
        // 키프레임 안의 !important 선언은 규격대로 무시한다.
        for (int j = 0; j < decls.size(); j++) {
            if (decls.get(j).isImportant()) {
                CssDiagnostics.report(diag, CssDiagnostics.INVALID, decls.get(j).getName(), "!important is ignored in keyframes");
            } else {
                style.add(decls.get(j), keyframeEnv, diag);
            }
        }
        return new KeyframesRule.Keyframe(offsets, style);
    }

    /**
     * Parses a keyframe selector: <code>from</code>, <code>to</code> or percentages, comma separated.
     * <p>
     * 키프레임 선택자를 파싱한다: <code>from</code>, <code>to</code>, 백분율을 쉼표로 구분.
     *
     * @param prelude the prelude / 전문
     * @return the offsets from 0 to 1, or null if invalid / 0 에서 1 까지의 오프셋. 올바르지 않으면 null
     */
    static List<Double> offsets(List<ComponentValue> prelude) {
        List<Double> out = new ArrayList<Double>();
        List<List<ComponentValue>> parts = CssParser.splitCommas(prelude);
        // 쉼표로 나눈 조각이 각각 from, to 또는 0 에서 100 사이의 백분율이어야 한다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> s = parts.get(i);
            if (s.size() != 1 || !(s.get(0) instanceof CssToken)) {
                return null;
            }
            CssToken t = (CssToken) s.get(0);
            if (t.isIdent("from")) {
                out.add(Double.valueOf(0));
            } else if (t.isIdent("to")) {
                out.add(Double.valueOf(1));
            } else if (t.is(CssToken.PERCENTAGE) && t.getNumber() >= 0 && t.getNumber() <= 100) {
                out.add(Double.valueOf(t.getNumber() / 100.0));
            } else {
                return null;
            }
        }
        return out;
    }
}
