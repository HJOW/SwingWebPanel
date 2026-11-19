package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.ContainerCondition;
import org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.ImportRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PageRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PositionTryRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PropertyRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.ScopeSpec;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.selector.ComplexSelector;
import org.duckdns.hjow.swingwebpanel.css.selector.CompoundSelector;
import org.duckdns.hjow.swingwebpanel.css.selector.PseudoClasses;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SimpleSelector;

/**
 * The index of all the style rules that apply in a media environment, flattened from the style sheets in cascade order: groups and imports are
 * expanded (a group that does not apply is left out), and every complex selector of a rule becomes an entry stored in a bucket by the id, class or tag
 * name of its rightmost compound selector, so that only the rules that can match an element are tested. The index also records which kinds of selectors
 * occur (sibling combinators, structural pseudo-classes, <code>:has()</code>), because they decide how much of the tree must be recomputed after a change.
 * <p>
 * 미디어 환경에서 적용되는 모든 스타일 규칙의 색인. 스타일 시트를 캐스케이드 순서대로 펴서 만든다: 그룹과 가져오기는 펼치고(적용되지 않는 그룹은 뺀다), 규칙의 복합
 * 선택자마다 항목을 만들어 가장 오른쪽 복합 선택자의 id, 클래스, 태그 이름으로 버킷에 넣으므로 요소와 일치할 수 있는 규칙만 검사한다. 어떤 종류의 선택자(형제 결합자, 구조
 * 의사 클래스, <code>:has()</code>)가 있는지도 기록하는데, 변경 뒤에 트리의 얼마를 다시 계산할지가 이에 달려 있다.
 */
final class RuleIndex {

    /**
     * One selector of a style rule with the information the cascade needs.
     * <p>
     * 캐스케이드에 필요한 정보가 붙은 스타일 규칙의 선택자 하나.
     */
    static final class Entry {
        /** The complex selector. / 복합 선택자. */
        final ComplexSelector selector;
        /** Packed specificity. / 묶은 구체성. */
        final long specificity;
        /** Position of the rule in the cascade (source order over all sheets). / 캐스케이드에서의 규칙 위치(모든 시트에 걸친 소스 순서). */
        final int order;
        /** Origin of the sheet. / 시트의 출처. */
        final int origin;
        /** The declarations. / 선언. */
        final CssStyleDeclaration style;
        /** Pseudo-element the selector targets, or null. / 선택자가 가리키는 의사 요소. 없으면 null. */
        final String pseudo;
        /** The cascade layer of the rule as a path such as <code>a.b</code>, or null for an unlayered rule. 규칙의 캐스케이드 레이어(<code>a.b</code> 같은 경로). 레이어가 없으면 null. */
        final String layer;
        /** The <code>@container</code> conditions the rule is inside of (outermost first), or null. 규칙을 둘러싼 <code>@container</code> 조건(바깥쪽이 먼저). 없으면 null. */
        final ContainerCondition[] containers;
        /** The <code>@scope</code> scopes the rule is inside of (outermost first), or null. 규칙을 둘러싼 <code>@scope</code> 범위(바깥쪽이 먼저). 없으면 null. */
        final ScopeSpec[] scopes;
        /** True if the selector does not name <code>:scope</code>, so that it only matches inside the scoping root. 선택자가 <code>:scope</code> 를 쓰지 않아 범위 루트 안에서만 일치하면 true. */
        final boolean scopeRelative;
        /** True if the rule is inside <code>@starting-style</code>: it only takes part in the starting style, not in the after-change style. 규칙이 <code>@starting-style</code> 안에 있으면 true: 변경 후 스타일이 아니라 시작 스타일에만 참가한다. */
        boolean starting;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param selector the selector / 선택자
         * @param order    cascade order / 캐스케이드 순서
         * @param origin   origin constant / 출처 상수
         * @param style    declarations / 선언
         * @param layer    the layer path or null / 레이어 경로. 없으면 null
         * @param cq       the container conditions or null / 컨테이너 조건. 없으면 null
         * @param sq       the scopes or null / 범위. 없으면 null
         */
        Entry(ComplexSelector selector, int order, int origin, CssStyleDeclaration style, String layer, ContainerCondition[] cq, ScopeSpec[] sq) {
            this.layer = layer;
            this.containers = cq;
            this.scopes = sq;
            this.scopeRelative = sq != null && !mentionsScope(selector);
            this.selector = selector;
            this.specificity = selector.getSpecificity();
            this.order = order;
            this.origin = origin;
            this.style = style;
            this.pseudo = selector.getPseudoElement();
        }
    }

    /** Entries by id of the subject. / 대상의 id 별 항목. */
    private final Map<String, List<Entry>> byId = new HashMap<String, List<Entry>>();

    /** Entries by class of the subject. / 대상의 클래스별 항목. */
    private final Map<String, List<Entry>> byClass = new HashMap<String, List<Entry>>();

    /** Entries by tag name of the subject. / 대상의 태그 이름별 항목. */
    private final Map<String, List<Entry>> byTag = new HashMap<String, List<Entry>>();

    /** Entries without id, class or tag. / id, 클래스, 태그가 없는 항목. */
    private final List<Entry> universal = new ArrayList<Entry>();

    /** The environment used to evaluate media conditions. / 미디어 조건을 평가한 환경. */
    private final MediaEnvironment env;

    /** True for quirks mode (ids and classes are keyed in lower case). / 호환 모드이면 true(id 와 클래스를 소문자 키로 쓴다). */
    private final boolean quirks;

    /** Next cascade order. / 다음 캐스케이드 순서. */
    private int nextOrder;

    /** The layer paths in the order of their first appearance. 레이어 경로(처음 나온 순서). */
    private final List<String> layerOrder = new ArrayList<String>();

    /** The ranks of the layers, computed when they are first asked for. 레이어의 순위. 처음 물을 때 계산한다. */
    private Map<String, Integer> layerRanks;

    /** Counter of anonymous layers. 이름 없는 레이어의 개수. */
    private int anonymous;

    /** The container conditions of the rules that are being added, outermost first, or null. 지금 추가하는 규칙을 둘러싼 컨테이너 조건(바깥쪽이 먼저). 없으면 null. */
    private ContainerCondition[] cq;

    /** The scopes of the rules that are being added, outermost first, or null. 지금 추가하는 규칙을 둘러싼 범위(바깥쪽이 먼저). 없으면 null. */
    private ScopeSpec[] sq;

    /** The registered custom properties (<code>@property</code>) by name, in the order of registration. 이름별 등록된 사용자 정의 속성(<code>@property</code>). 등록 순서. */
    private final Map<String, RegisteredProperty> registrations = new java.util.LinkedHashMap<String, RegisteredProperty>();

    /**
     * Returns the registered custom properties of the style sheets.
     * <p>
     * 스타일 시트의 등록된 사용자 정의 속성을 반환한다.
     *
     * @return the registrations by name / 이름별 등록
     */
    Map<String, RegisteredProperty> registrations() {
        return registrations;
    }

    /** The counter style definitions (<code>@counter-style</code>) by name; a later definition replaces an earlier one. 이름별 카운터 스타일 정의(<code>@counter-style</code>). 나중 정의가 앞의 것을 대체한다. */
    private final Map<String, CounterStyleRule> counterStyles = new java.util.LinkedHashMap<String, CounterStyleRule>();

    /**
     * Returns the counter style definitions of the style sheets.
     * <p>
     * 스타일 시트의 카운터 스타일 정의를 반환한다.
     *
     * @return the definitions by name / 이름별 정의
     */
    Map<String, CounterStyleRule> counterStyleRules() {
        return counterStyles;
    }

    /** The <code>@font-feature-values</code> rules in document order. 문서 순서의 <code>@font-feature-values</code> 규칙. */
    private final List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> featureValues = new ArrayList<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule>();

    /**
     * Returns the <code>@font-feature-values</code> rules of the style sheets in document order.
     * <p>
     * 스타일 시트의 <code>@font-feature-values</code> 규칙을 문서 순서로 반환한다.
     *
     * @return the rules / 규칙
     */
    List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> fontFeatureValues() {
        return featureValues;
    }

    /** The position options (<code>@position-try</code>) by name; a later rule of the same name replaces an earlier one. 이름별 위치 대안(<code>@position-try</code>). 같은 이름의 나중 규칙이 앞의 것을 대체한다. */
    private final Map<String, PositionTryRule> positionTries = new java.util.LinkedHashMap<String, PositionTryRule>();

    /**
     * Returns the position options of the style sheets.
     * <p>
     * 스타일 시트의 위치 대안을 반환한다.
     *
     * @return the rules by name / 이름별 규칙
     */
    Map<String, PositionTryRule> positionTryRules() {
        return positionTries;
    }

    /** The page rules (<code>@page</code>) of the style sheets in the order of the sources. 스타일 시트의 <code>@page</code> 규칙(소스 순서). */
    private final List<PageRule> pages = new ArrayList<PageRule>();

    /**
     * Returns the page rules of the style sheets.
     * <p>
     * 스타일 시트의 <code>@page</code> 규칙을 반환한다.
     *
     * @return the rules in the order of the sources / 소스 순서의 규칙
     */
    List<PageRule> pageRules() {
        return pages;
    }

    /** True if a rule inside <code>@container</code> exists, so that styles depend on the layout. <code>@container</code> 안의 규칙이 있어 스타일이 레이아웃에 달려 있으면 true. */
    boolean usesContainers;

    /** True if a rule inside <code>@scope</code> exists. <code>@scope</code> 안의 규칙이 있으면 true. */
    boolean usesScopes;

    /** True if a rule inside <code>@starting-style</code> exists. <code>@starting-style</code> 안의 규칙이 있으면 true. */
    boolean hasStarting;

    /** True while the rules inside a <code>@starting-style</code> group are added. <code>@starting-style</code> 그룹 안의 규칙을 추가하는 동안 true. */
    private boolean inStarting;

    /** True if a sibling combinator occurs. / 형제 결합자가 있으면 true. */
    boolean usesSiblings;

    /** True if a structural pseudo-class occurs. / 구조 의사 클래스가 있으면 true. */
    boolean usesStructural;

    /** True if <code>:has()</code> occurs. / <code>:has()</code> 가 있으면 true. */
    boolean usesHas;

    /**
     * Creates an index.
     * <p>
     * 색인을 만든다.
     *
     * @param env    the media environment / 미디어 환경
     * @param quirks true for quirks mode / 호환 모드이면 true
     */
    RuleIndex(MediaEnvironment env, boolean quirks) {
        this.env = env;
        this.quirks = quirks;
    }

    /**
     * Adds a style sheet (and, recursively, its imports and groups) if it is enabled and its media match.
     * <p>
     * 스타일 시트를 추가한다. 켜져 있고 미디어가 맞으면 가져온 시트와 그룹도 재귀로 추가한다.
     *
     * @param sheet the sheet / 시트
     */
    void addSheet(StyleSheet sheet) {
        addSheet(sheet, null);
    }

    /**
     * Adds a style sheet that is put into a cascade layer.
     * <p>
     * 캐스케이드 레이어에 넣을 스타일 시트를 추가한다.
     *
     * @param sheet the sheet / 시트
     * @param layer the layer path or null / 레이어 경로. 없으면 null
     */
    private void addSheet(StyleSheet sheet, String layer) {
        if (sheet.isDisabled() || !sheet.getMedia().matches(env)) {
            return;
        }
        addRules(sheet.getRules(), sheet.getOrigin(), layer);
    }

    /**
     * Adds a list of rules.
     * <p>
     * 규칙 목록을 추가한다.
     *
     * @param rules  the rules / 규칙
     * @param origin the origin of the sheet / 시트의 출처
     */
    private void addRules(List<CssRule> rules, int origin, String layer) {
        // 규칙 종류에 따라 스타일 규칙은 항목으로, 그룹은 조건이 성립하면 재귀로, 가져오기는 가져온 시트를 추가한다.
        for (int i = 0; i < rules.size(); i++) {
            CssRule r = rules.get(i);
            if (r instanceof StyleRule) {
                addStyleRule((StyleRule) r, origin, layer);
            } else if (r instanceof CounterStyleRule) {
                // @counter-style 은 문서 전체의 카운터 스타일을 정의하며 같은 이름은 나중 정의가 이긴다.
                counterStyles.put(((CounterStyleRule) r).getName(), (CounterStyleRule) r);
            } else if (r instanceof org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule) {
                // @font-feature-values 는 패밀리별 기능 값의 이름을 정의하며 문서 순서로 모은다(같은 이름은 나중 정의가 이긴다).
                featureValues.add((org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule) r);
            } else if (r instanceof PositionTryRule) {
                // @position-try 는 @keyframes 처럼 같은 이름이면 나중 규칙이 이긴다.
                positionTries.put(((PositionTryRule) r).getName(), (PositionTryRule) r);
            } else if (r instanceof PageRule) {
                // @page 는 쪽 상자의 서술자이며 요소의 스타일에는 영향이 없다. 쪽 스타일 계산(PageStyles)을 위해 소스 순서로 모은다.
                pages.add((PageRule) r);
            } else if (r instanceof PropertyRule) {
                // @property 는 문서 전체에 사용자 정의 속성을 등록하며 나중 등록이 앞의 것을 대체한다. 초기값이 맥락에 의존하면 등록하지 않는다.
                RegisteredProperty rp = RegisteredProperty.of((PropertyRule) r);
                if (rp != null) {
                    registrations.put(rp.name, rp);
                }
            } else if (r instanceof GroupRule && ((GroupRule) r).getType() == CssRule.LAYER_RULE) {
                addLayer((GroupRule) r, origin, layer);
            } else if (r instanceof GroupRule && ((GroupRule) r).getType() == CssRule.CONTAINER_RULE) {
                // @container 안의 규칙은 항목마다 조건을 달고 들어가며, 조건은 요소의 질의 컨테이너가 정해진 뒤에 평가한다.
                ContainerCondition[] saved = cq;
                cq = append(saved, ((GroupRule) r).getContainer());
                usesContainers = true;
                addRules(((GroupRule) r).getRules(), origin, layer);
                cq = saved;
            } else if (r instanceof GroupRule && ((GroupRule) r).getType() == CssRule.STARTING_STYLE_RULE) {
                // @starting-style 안의 규칙은 항목에 표시를 달고 들어가며, 변경 후 스타일의 계산에서는 건너뛴다.
                boolean saved = inStarting;
                inStarting = true;
                hasStarting = true;
                addRules(((GroupRule) r).getRules(), origin, layer);
                inStarting = saved;
            } else if (r instanceof GroupRule && ((GroupRule) r).getType() == CssRule.SCOPE_RULE) {
                // @scope 안의 규칙은 항목마다 범위를 달고 들어간다.
                ScopeSpec[] saved = sq;
                sq = append(saved, ((GroupRule) r).getScope());
                usesScopes = true;
                addRules(((GroupRule) r).getRules(), origin, layer);
                sq = saved;
            } else if (r instanceof GroupRule && ((GroupRule) r).applies(env)) {
                addRules(((GroupRule) r).getRules(), origin, layer);
            } else if (r instanceof ImportRule && ((ImportRule) r).getStyleSheet() != null && ((ImportRule) r).getMedia().matches(env)) {
                ImportRule imp = (ImportRule) r;
                String inner = layer;
                if (imp.getLayer() != null) {
                    // layer(name) 로 가져온 시트는 그 이름의 레이어(이름이 없으면 익명 레이어)에 들어간다.
                    inner = join(layer, imp.getLayer().length() == 0 ? "#anonymous" + (anonymous++) : imp.getLayer());
                    registerLayer(inner);
                }
                addSheet(imp.getStyleSheet(), inner);
            }
        }
    }

    /**
     * Adds a <code>@layer</code> rule: a statement names layers (in the current layer) in the order of the names, a block names one layer (or an
     * anonymous one) and adds its rules to it. The order of the layers is the order in which they are named for the first time (CSS Cascade
     * Level 5 section 6.4).
     * <p>
     * <code>@layer</code> 규칙을 추가한다: 문장은 현재 레이어 안에서 이름 순서대로 레이어의 이름을 정하고, 블록은 레이어 하나(또는 이름 없는 레이어)를 정해 그 안에 규칙을
     * 더한다. 레이어의 순서는 이름이 처음 나온 순서이다(CSS Cascade Level 5 6.4 절).
     *
     * @param rule   the rule / 규칙
     * @param origin the origin / 출처
     * @param parent the path of the layer that contains the rule, or null / 규칙을 담은 레이어의 경로. 없으면 null
     */
    private void addLayer(GroupRule rule, int origin, String parent) {
        if (rule.isLayerStatement()) {
            String[] names = rule.getCondition().split(",");
            // 이름마다 레이어를 만든다(이미 있으면 순서는 그대로).
            for (int i = 0; i < names.length; i++) {
                if (names[i].trim().length() > 0) {
                    registerLayer(join(parent, names[i].trim()));
                }
            }
            return;
        }
        String name = rule.getCondition().trim();
        String path = join(parent, name.length() == 0 ? "#anonymous" + (anonymous++) : name);
        registerLayer(path);
        addRules(rule.getRules(), origin, path);
    }

    /**
     * Returns a copy of a condition array with one more condition at the end.
     * <p>
     * 조건 배열에 조건 하나를 끝에 더한 복사본을 반환한다.
     *
     * @param a the array or null / 배열. 없으면 null
     * @param c the condition / 조건
     * @return the longer array / 더 긴 배열
     */
    private static ContainerCondition[] append(ContainerCondition[] a, ContainerCondition c) {
        ContainerCondition[] n = new ContainerCondition[a == null ? 1 : a.length + 1];
        if (a != null) {
            System.arraycopy(a, 0, n, 0, a.length);
        }
        n[n.length - 1] = c;
        return n;
    }

    /**
     * Returns a copy of a scope array with one more scope at the end.
     * <p>
     * 범위 배열에 범위 하나를 끝에 더한 복사본을 반환한다.
     *
     * @param a the array or null / 배열. 없으면 null
     * @param s the scope / 범위
     * @return the longer array / 더 긴 배열
     */
    private static ScopeSpec[] append(ScopeSpec[] a, ScopeSpec s) {
        ScopeSpec[] n = new ScopeSpec[a == null ? 1 : a.length + 1];
        if (a != null) {
            System.arraycopy(a, 0, n, 0, a.length);
        }
        n[n.length - 1] = s;
        return n;
    }

    /**
     * Tells whether a complex selector names <code>:scope</code> anywhere, also inside the arguments of functional pseudo-classes.
     * <p>
     * 복합 선택자가 <code>:scope</code> 를 어디서든(함수형 의사 클래스의 인수 안도) 쓰는지 알려 준다.
     *
     * @param c the selector / 선택자
     * @return true if <code>:scope</code> occurs / <code>:scope</code> 가 있으면 true
     */
    static boolean mentionsScope(ComplexSelector c) {
        // 복합 선택자마다 단순 선택자를 훑는다.
        for (int i = 0; i < c.getCompounds().size(); i++) {
            List<SimpleSelector> simples = c.getCompounds().get(i).getSimples();
            // 복합 선택자의 단순 선택자를 하나씩 본다.
            for (int k = 0; k < simples.size(); k++) {
                if (mentionsScope(simples.get(k))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether a simple selector is <code>:scope</code> or has an argument that names it.
     * <p>
     * 단순 선택자가 <code>:scope</code> 이거나 인수에 그것이 있는지 알려 준다.
     *
     * @param s the simple selector / 단순 선택자
     * @return true if so / 그렇다면 true
     */
    private static boolean mentionsScope(SimpleSelector s) {
        if (s.getKind() != SimpleSelector.PSEUDO_CLASS) {
            return false;
        }
        if (s.getPseudoId() == PseudoClasses.SCOPE) {
            return true;
        }
        SelectorList l = s.getList();
        // 인수 목록의 선택자도 같은 방식으로 훑는다.
        for (int i = 0; l != null && i < l.size(); i++) {
            if (mentionsScope(l.getSelectors().get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Joins a layer path and a name.
     * <p>
     * 레이어 경로와 이름을 잇는다.
     *
     * @param parent the path or null / 경로. 없으면 null
     * @param name   the name (it may be a dotted path itself) / 이름(점으로 이은 경로일 수도 있다)
     * @return the path / 경로
     */
    private static String join(String parent, String name) {
        return parent == null ? name : parent + "." + name;
    }

    /**
     * Records a layer path, and the layers above it, in the order of their first appearance.
     * <p>
     * 레이어 경로와 그 위의 레이어를 처음 나온 순서로 기록한다.
     *
     * @param path the path / 경로
     */
    private void registerLayer(String path) {
        int dot = path.lastIndexOf('.');
        if (dot > 0) {
            registerLayer(path.substring(0, dot));
        }
        if (!layerOrder.contains(path)) {
            layerOrder.add(path);
            layerRanks = null;
        }
    }

    /**
     * Returns the rank of a layer in the cascade: the layers are ordered by the first appearance of their names, the sub-layers of a layer
     * come before the rules of the layer itself, and an unlayered rule has the highest rank (for normal declarations; for important ones the
     * order is reversed by the caller).
     * <p>
     * 캐스케이드에서 레이어의 순위를 반환한다: 레이어는 이름이 처음 나온 순서이고, 레이어의 하위 레이어는 그 레이어 자신의 규칙보다 앞서며, 레이어가 없는 규칙의 순위가 가장
     * 높다(일반 선언 기준. 중요한 선언은 호출한 쪽이 순서를 뒤집는다).
     *
     * @param path the layer path or null / 레이어 경로. 없으면 null
     * @return the rank, <code>Integer.MAX_VALUE</code> for an unlayered rule / 순위. 레이어가 없으면 <code>Integer.MAX_VALUE</code>
     */
    int layerRank(String path) {
        if (path == null) {
            return Integer.MAX_VALUE;
        }
        if (layerRanks == null) {
            layerRanks = new HashMap<String, Integer>();
            int[] counter = {0};
            rankChildren(null, counter);
        }
        Integer r = layerRanks.get(path);
        return r == null ? Integer.MAX_VALUE - 1 : r.intValue();
    }

    /**
     * Gives ranks to the layers below a parent: the children in the order of appearance, each after its own sub-layers.
     * <p>
     * 부모 아래의 레이어에 순위를 준다: 자식은 나온 순서대로이고 각자 자신의 하위 레이어 뒤에 놓인다.
     *
     * @param parent  the parent path or null for the top level / 부모 경로. 최상위이면 null
     * @param counter the next rank (one element) / 다음 순위(원소 하나)
     */
    private void rankChildren(String parent, int[] counter) {
        // 처음 나온 순서대로 훑으며 직접 자식인 레이어에 하위 레이어, 자신의 순서로 순위를 준다.
        for (int i = 0; i < layerOrder.size(); i++) {
            String p = layerOrder.get(i);
            boolean child = parent == null ? p.indexOf('.') < 0 : p.startsWith(parent + ".") && p.indexOf('.', parent.length() + 1) < 0;
            if (child) {
                rankChildren(p, counter);
                layerRanks.put(p, Integer.valueOf(counter[0]++));
            }
        }
    }

    /**
     * Adds the entries of a style rule.
     * <p>
     * 스타일 규칙의 항목을 추가한다.
     *
     * @param rule   the rule / 규칙
     * @param origin the origin / 출처
     */
    private void addStyleRule(StyleRule rule, int origin, String layer) {
        int order = nextOrder++;
        // 컨테이너를 선언하는 규칙이 있으면 컨테이너의 크기를 레이아웃에서 재야 한다(cqw 같은 컨테이너 상대 길이가 그 크기를 쓴다).
        if (rule.getStyle().normal().containsKey(Integer.valueOf(Prop.CONTAINER_TYPE)) || rule.getStyle().important().containsKey(Integer.valueOf(Prop.CONTAINER_TYPE))) {
            usesContainers = true;
        }
        List<ComplexSelector> sels = rule.getSelectors().getSelectors();
        // 규칙의 복합 선택자마다 항목을 만들어 버킷에 넣는다.
        for (int i = 0; i < sels.size(); i++) {
            Entry e = new Entry(sels.get(i), order, origin, rule.getStyle(), layer, cq, sq);
            e.starting = inStarting;
            scan(sels.get(i));
            CompoundSelector subject = sels.get(i).getSubject();
            String id = null;
            String cls = null;
            String tag = null;
            List<SimpleSelector> simples = subject.getSimples();
            // 대상 복합 선택자에서 id, 클래스, 태그 이름 순으로 버킷 키를 고른다.
            for (int k = 0; k < simples.size(); k++) {
                SimpleSelector s = simples.get(k);
                if (s.getKind() == SimpleSelector.ID && id == null) {
                    id = key(s.getName());
                } else if (s.getKind() == SimpleSelector.CLASS && cls == null) {
                    cls = key(s.getName());
                } else if (s.getKind() == SimpleSelector.TYPE && tag == null) {
                    tag = s.getName().toLowerCase();
                }
            }
            if (id != null) {
                put(byId, id, e);
            } else if (cls != null) {
                put(byClass, cls, e);
            } else if (tag != null) {
                put(byTag, tag, e);
            } else {
                universal.add(e);
            }
        }
    }

    /**
     * Returns the bucket key of an id or class name.
     * <p>
     * id 나 클래스 이름의 버킷 키를 반환한다.
     *
     * @param name the name / 이름
     * @return the key (lower case in quirks mode) / 키(호환 모드에서는 소문자)
     */
    private String key(String name) {
        return quirks ? name.toLowerCase() : name;
    }

    /**
     * Adds an entry to a bucket map.
     * <p>
     * 버킷 맵에 항목을 추가한다.
     *
     * @param m the map / 맵
     * @param k the key / 키
     * @param e the entry / 항목
     */
    private static void put(Map<String, List<Entry>> m, String k, Entry e) {
        List<Entry> l = m.get(k);
        if (l == null) {
            l = new ArrayList<Entry>();
            m.put(k, l);
        }
        l.add(e);
    }

    /**
     * Records which kinds of selectors occur in a complex selector.
     * <p>
     * 복합 선택자에 어떤 종류의 선택자가 있는지 기록한다.
     *
     * @param c the selector / 선택자
     */
    private void scan(ComplexSelector c) {
        int n = c.getCompounds().size();
        // 결합자 중 형제 결합자가 있는지 본다.
        for (int i = 0; i + 1 < n; i++) {
            char comb = c.getCombinator(i);
            if (comb == ComplexSelector.NEXT_SIBLING || comb == ComplexSelector.LATER_SIBLING) {
                usesSiblings = true;
                usesStructural = true;
            }
        }
        // 복합 선택자마다 구조 의사 클래스와 :has 를 찾는다.
        for (int i = 0; i < n; i++) {
            List<SimpleSelector> simples = c.getCompounds().get(i).getSimples();
            // simples 의 항목을 하나씩 처리한다.
            for (int k = 0; k < simples.size(); k++) {
                scan(simples.get(k));
            }
        }
    }

    /**
     * Records the kind of a simple selector, descending into the arguments of logical pseudo-classes.
     * <p>
     * 단순 선택자의 종류를 기록하며 논리 의사 클래스의 인수 안으로 내려간다.
     *
     * @param s the simple selector / 단순 선택자
     */
    private void scan(SimpleSelector s) {
        if (s.getKind() != SimpleSelector.PSEUDO_CLASS) {
            return;
        }
        int id = s.getPseudoId();
        if (id == PseudoClasses.HAS) {
            usesHas = true;
            usesStructural = true;
            usesSiblings = true;
        }
        if (id == PseudoClasses.FIRST_CHILD || id == PseudoClasses.LAST_CHILD || id == PseudoClasses.ONLY_CHILD || id == PseudoClasses.FIRST_OF_TYPE
                || id == PseudoClasses.LAST_OF_TYPE || id == PseudoClasses.ONLY_OF_TYPE || id == PseudoClasses.NTH_CHILD
                || id == PseudoClasses.NTH_LAST_CHILD || id == PseudoClasses.NTH_OF_TYPE || id == PseudoClasses.NTH_LAST_OF_TYPE
                || id == PseudoClasses.EMPTY) {
            usesStructural = true;
        }
        SelectorList l = s.getList();
        // 인수 목록의 선택자도 같은 방식으로 훑는다.
        for (int i = 0; l != null && i < l.size(); i++) {
            scan(l.getSelectors().get(i));
        }
    }

    /**
     * Collects the entries that might match an element.
     * <p>
     * 요소와 일치할 수 있는 항목을 모은다.
     *
     * @param tagName  the lower-case local name of the element / 요소의 소문자 지역 이름
     * @param id       the id of the element / 요소의 id
     * @param classes  the class attribute value or null / class 속성 값 또는 null
     * @param out      receives the candidates / 후보를 받는다
     */
    void candidates(String tagName, String id, String classes, List<Entry> out) {
        if (id.length() > 0) {
            List<Entry> l = byId.get(key(id));
            if (l != null) {
                out.addAll(l);
            }
        }
        if (classes != null && classes.length() > 0 && !byClass.isEmpty()) {
            String[] tokens = classes.split("[ \\t\\n\\r\\f]+");
            // class 속성의 토큰마다 버킷을 확인한다.
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].length() > 0) {
                    List<Entry> l = byClass.get(key(tokens[i]));
                    if (l != null) {
                        out.addAll(l);
                    }
                }
            }
        }
        List<Entry> t = byTag.get(tagName);
        if (t != null) {
            out.addAll(t);
        }
        out.addAll(universal);
    }

    /**
     * Returns the number of entries.
     * <p>
     * 항목 수를 반환한다.
     *
     * @return count / 개수
     */
    int size() {
        int n = universal.size();
        // 모든 버킷의 항목 수를 더한다.
        for (List<Entry> l : byId.values()) {
            n += l.size();
        }
        // 항목 의 항목을 하나씩 처리한다.
        for (List<Entry> l : byClass.values()) {
            n += l.size();
        }
        // 항목 의 항목을 하나씩 처리한다.
        for (List<Entry> l : byTag.values()) {
            n += l.size();
        }
        return n;
    }
}
