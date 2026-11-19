package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;

/**
 * A conditional group rule: <code>@media</code>, <code>@supports</code> and <code>@layer</code> blocks, which contain other rules. Its rules apply when
 * the condition holds: a media group while its query list matches the environment, a supports group when its condition was true when the sheet was
 * parsed, a layer group always (cascade layers are parsed but their ordering is not applied). The rules of a group can be inserted and deleted
 * ({@link #insertRule}, {@link #deleteRule}) and the media list of a media group can be replaced, as the CSSOM allows.
 * <p>
 * 조건부 그룹 규칙: 다른 규칙을 담는 <code>@media</code>, <code>@supports</code>, <code>@layer</code> 블록. 조건이 성립할 때 그 안의 규칙이 적용된다: media 그룹은 질의 목록이
 * 환경과 일치하는 동안, supports 그룹은 스타일 시트를 파싱할 때 조건이 참이었으면, layer 그룹은 항상(캐스케이드 레이어는 파싱하지만 순서는 적용하지 않는다).
 * 그룹의 규칙은 CSSOM 이 허용하는 대로 넣고 지울 수 있고({@link #insertRule}, {@link #deleteRule}) media 그룹의 미디어 목록은 바꿀 수 있다.
 */
public final class GroupRule extends CssRule {

    /** The kind: {@link #MEDIA_RULE}, {@link #SUPPORTS_RULE} or {@link #LAYER_RULE}. / 종류: MEDIA_RULE, SUPPORTS_RULE, LAYER_RULE. */
    private final int type;

    /** The media query list (media groups) or null. / 미디어 질의 목록(media 그룹) 또는 null. */
    private MediaQueryList media;

    /** The condition text (supports) or the layer name. / 조건 문자열(supports) 또는 레이어 이름. */
    private final String condition;

    /** True if the supports condition holds. / supports 조건이 참이면 true. */
    private final boolean supported;

    /** The contained rules (changed through {@link #insertRule} and {@link #deleteRule}). / 포함된 규칙({@link #insertRule}, {@link #deleteRule} 로 바뀐다). */
    private final List<CssRule> rules;

    /** The read-only view of the rules that {@link #getRules()} hands out (it follows the changes). / {@link #getRules()} 가 내주는 규칙의 읽기 전용 뷰(변경을 따라간다). */
    private final List<CssRule> rulesView;

    /** True for a <code>@layer a, b;</code> statement (no block). <code>@layer a, b;</code> 문장(블록 없음)이면 true. */
    private boolean statement;

    /** The condition of a <code>@container</code> group, or null. / <code>@container</code> 그룹의 조건. 없으면 null. */
    private ContainerCondition container;

    /** The scope of a <code>@scope</code> group, or null. / <code>@scope</code> 그룹의 범위. 없으면 null. */
    private ScopeSpec scope;

    /** The media list object of the CSSOM, created when it is first asked for. / CSSOM 의 미디어 목록 객체. 처음 요청할 때 만든다. */
    private MediaList mediaList;

    /** The rule list object of the CSSOM, created when it is first asked for. / CSSOM 의 규칙 목록 객체. 처음 요청할 때 만든다. */
    private CssRuleList ruleList;

    /**
     * Creates a group rule.
     * <p>
     * 그룹 규칙을 만든다.
     *
     * @param type      type constant / 종류 상수
     * @param media     media list or null / 미디어 목록 또는 null
     * @param condition condition or name text / 조건 또는 이름 문자열
     * @param supported true if the supports condition holds / supports 조건이 참이면 true
     * @param rules     contained rules (copied) / 포함된 규칙(복사됨)
     */
    public GroupRule(int type, MediaQueryList media, String condition, boolean supported, List<CssRule> rules) {
        this.type = type;
        this.media = media;
        this.condition = condition;
        this.supported = supported;
        this.rules = new ArrayList<CssRule>(rules);
        this.rulesView = Collections.unmodifiableList(this.rules);
    }

    /**
     * Creates the <code>@layer a, b;</code> statement that names layers (and so fixes their order) without giving them rules.
     * <p>
     * 레이어에 규칙을 주지 않고 이름만 정해(그래서 순서를 정하는) <code>@layer a, b;</code> 문장을 만든다.
     *
     * @param names the comma separated layer names / 쉼표로 구분한 레이어 이름
     * @return the rule / 규칙
     */
    public static GroupRule layerStatement(String names) {
        GroupRule g = new GroupRule(LAYER_RULE, null, names, true, new ArrayList<CssRule>());
        g.statement = true;
        return g;
    }

    /**
     * Creates a <code>@container</code> group: its rules apply to the elements whose query container satisfies the condition (the cascade tests
     * the condition for every element, as it depends on the layout of the container).
     * <p>
     * <code>@container</code> 그룹을 만든다: 질의 컨테이너가 조건을 만족하는 요소에 그 안의 규칙이 적용된다(조건은 컨테이너의 레이아웃에 달려 있으므로 캐스케이드가 요소마다
     * 검사한다).
     *
     * @param condition the parsed prelude / 파싱한 전문
     * @param rules     the contained rules / 포함된 규칙
     * @return the rule / 규칙
     */
    public static GroupRule container(ContainerCondition condition, List<CssRule> rules) {
        GroupRule g = new GroupRule(CONTAINER_RULE, null, condition.toCssText(), true, rules);
        g.container = condition;
        return g;
    }

    /**
     * Creates a <code>@scope</code> group: its style rules only apply to the elements inside the scope (<code>:scope</code> in them is the scoping
     * root).
     * <p>
     * <code>@scope</code> 그룹을 만든다: 안의 스타일 규칙은 범위 안의 요소에만 적용된다(안의 <code>:scope</code> 는 범위 루트이다).
     *
     * @param spec  the scope / 범위
     * @param rules the contained rules / 포함된 규칙
     * @return the rule / 규칙
     */
    public static GroupRule scope(ScopeSpec spec, List<CssRule> rules) {
        GroupRule g = new GroupRule(SCOPE_RULE, null, spec.toCssText(), true, rules);
        g.scope = spec;
        return g;
    }

    /**
     * Creates a <code>@starting-style</code> group (CSS Transitions Level 2, section 3.3): its style rules do not apply to the after-change style
     * of an element; they only make the starting style from which a transition starts on an element that had no before-change style.
     * <p>
     * <code>@starting-style</code> 그룹을 만든다(CSS Transitions Level 2, 3.3 절): 안의 스타일 규칙은 요소의 변경 후 스타일에는 적용되지 않고, 변경 전 스타일이
     * 없던 요소에서 전환이 출발하는 시작 스타일을 만들 때만 쓰인다.
     *
     * @param rules the contained rules / 포함된 규칙
     * @return the rule / 규칙
     */
    public static GroupRule startingStyle(List<CssRule> rules) {
        return new GroupRule(STARTING_STYLE_RULE, null, "", true, rules);
    }

    /**
     * Returns the condition of a <code>@container</code> group.
     * <p>
     * <code>@container</code> 그룹의 조건을 반환한다.
     *
     * @return the condition or null / 조건 또는 null
     */
    public ContainerCondition getContainer() {
        return container;
    }

    /**
     * Returns the scope of a <code>@scope</code> group.
     * <p>
     * <code>@scope</code> 그룹의 범위를 반환한다.
     *
     * @return the scope or null / 범위 또는 null
     */
    public ScopeSpec getScope() {
        return scope;
    }

    /**
     * Returns the CSSOM <code>containerName</code> of a <code>@container</code> group.
     * <p>
     * <code>@container</code> 그룹의 CSSOM <code>containerName</code> 을 반환한다.
     *
     * @return the container name, the empty string if the query names none or this is not a container group / 컨테이너 이름. 이름이 없거나 container 그룹이 아니면 빈 문자열
     */
    public String getContainerName() {
        return container == null || container.getName() == null ? "" : container.getName();
    }

    /**
     * Returns the CSSOM <code>containerQuery</code> of a <code>@container</code> group: the condition without the container name.
     * <p>
     * <code>@container</code> 그룹의 CSSOM <code>containerQuery</code> 를 반환한다: 컨테이너 이름을 뺀 조건.
     *
     * @return the query text, the empty string if this is not a container group / 질의 문자열. container 그룹이 아니면 빈 문자열
     */
    public String getContainerQuery() {
        return container == null ? "" : container.getQueryText();
    }

    /**
     * Returns the CSSOM <code>start</code> of a <code>@scope</code> group: the text of the start selector list.
     * <p>
     * <code>@scope</code> 그룹의 CSSOM <code>start</code> 를 반환한다: 시작 선택자 목록의 문자열.
     *
     * @return the selector text, or null if the scope has no start selector / 선택자 문자열. 시작 선택자가 없으면 null
     */
    public String getScopeStart() {
        return scope == null ? null : scope.getStartText();
    }

    /**
     * Returns the CSSOM <code>end</code> of a <code>@scope</code> group: the text of the end selector list.
     * <p>
     * <code>@scope</code> 그룹의 CSSOM <code>end</code> 를 반환한다: 끝 선택자 목록의 문자열.
     *
     * @return the selector text, or null if the scope has no end selector / 선택자 문자열. 끝 선택자가 없으면 null
     */
    public String getScopeEnd() {
        return scope == null ? null : scope.getEndText();
    }

    /**
     * Tells whether this is a <code>@layer</code> statement that only names layers.
     * <p>
     * 레이어 이름만 정하는 <code>@layer</code> 문장인지 알려 준다.
     *
     * @return true for a statement / 문장이면 true
     */
    public boolean isLayerStatement() {
        return statement;
    }

    /**
     * Returns the contained rules. The list is a read-only view that follows later insertions and deletions.
     * <p>
     * 포함된 규칙을 반환한다. 목록은 이후의 삽입과 삭제를 따라가는 읽기 전용 뷰이다.
     *
     * @return unmodifiable view / 수정할 수 없는 뷰
     */
    public List<CssRule> getRules() {
        return rulesView;
    }

    /**
     * Returns the rule list object of the CSSOM, <code>cssRules</code> (the same object every time).
     * <p>
     * CSSOM 의 규칙 목록 객체 <code>cssRules</code> 를 반환한다(매번 같은 객체).
     *
     * @return the rule list / 규칙 목록
     */
    public CssRuleList getCssRules() {
        if (ruleList == null) {
            ruleList = new CssRuleList(rulesView);
        }
        return ruleList;
    }

    /**
     * Returns the media query list of a media group.
     * <p>
     * media 그룹의 미디어 질의 목록을 반환한다.
     *
     * @return the list or null / 목록 또는 null
     */
    public MediaQueryList getMedia() {
        return media;
    }

    /**
     * Replaces the media query list of a media group (the style resolver is told).
     * <p>
     * media 그룹의 미디어 질의 목록을 바꾼다(스타일 리졸버에 알린다).
     *
     * @param m the list / 목록
     */
    public void setMedia(MediaQueryList m) {
        this.media = m;
        changed();
    }

    /**
     * Returns the media list object of the CSSOM of a media group (<code>media</code>), the same object every time.
     * <p>
     * media 그룹의 CSSOM 미디어 목록 객체(<code>media</code>)를 반환한다. 매번 같은 객체.
     *
     * @return the media list, or null for a group that is not an <code>@media</code> rule / 미디어 목록. <code>@media</code> 규칙이 아닌 그룹이면 null
     */
    public MediaList getMediaList() {
        if (media == null) {
            return null;
        }
        if (mediaList == null) {
            mediaList = new MediaList(this);
        }
        return mediaList;
    }

    /**
     * Returns the condition text of a supports group or the name of a layer.
     * <p>
     * supports 그룹의 조건 문자열이나 레이어의 이름을 반환한다.
     *
     * @return text / 문자열
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Returns the CSSOM <code>conditionText</code>: the media queries of a media group or the condition of a supports group.
     * <p>
     * CSSOM <code>conditionText</code> 를 반환한다: media 그룹의 미디어 질의 또는 supports 그룹의 조건.
     *
     * @return text / 문자열
     */
    public String getConditionText() {
        return type == MEDIA_RULE ? getMediaList().getMediaText() : condition;
    }

    /**
     * Returns the name of a <code>@layer</code> block (the CSSOM <code>name</code>); the empty string for an anonymous layer.
     * <p>
     * <code>@layer</code> 블록의 이름을 반환한다(CSSOM <code>name</code>). 이름 없는 레이어이면 빈 문자열.
     *
     * @return the name / 이름
     */
    public String getName() {
        return condition.trim();
    }

    /**
     * Returns the layer names of a <code>@layer a, b;</code> statement (the CSSOM <code>nameList</code>).
     * <p>
     * <code>@layer a, b;</code> 문장의 레이어 이름을 반환한다(CSSOM <code>nameList</code>).
     *
     * @return the names / 이름
     */
    public List<String> getNameList() {
        List<String> out = new ArrayList<String>();
        String[] names = condition.split(",");
        // 쉼표로 나눈 이름을 다듬어 모은다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].trim().length() > 0) {
                out.add(names[i].trim());
            }
        }
        return out;
    }

    /**
     * Tells whether the rules of the group apply in an environment.
     * <p>
     * 그룹의 규칙이 환경에서 적용되는지 알려 준다.
     *
     * @param env the media environment / 미디어 환경
     * @return true if the condition holds / 조건이 성립하면 true
     */
    public boolean applies(MediaEnvironment env) {
        if (type == MEDIA_RULE) {
            return media.matches(env);
        }
        return type != SUPPORTS_RULE || supported;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return the type constant / 종류 상수
     */
    public int getType() {
        return type;
    }

    /**
     * Inserts a rule given as text into the group (CSSOM <code>insertRule</code> of a grouping rule); <code>@import</code> and <code>@namespace</code>
     * rules cannot be nested.
     * <p>
     * 텍스트로 주어진 규칙을 그룹에 넣는다(그룹 규칙의 CSSOM <code>insertRule</code>). <code>@import</code> 와 <code>@namespace</code> 규칙은 중첩할 수 없다.
     *
     * @param text  one rule / 규칙 하나
     * @param index the position, from 0 to the number of rules / 위치. 0 부터 규칙 수까지
     * @return the position of the new rule / 새 규칙의 위치
     * @throws org.duckdns.hjow.swingwebpanel.dom.DomException as {@link StyleSheet#insertRule} / {@link StyleSheet#insertRule} 와 같다
     */
    public int insertRule(String text, int index) {
        return RuleEditor.insert(getParentStyleSheet(), this, rules, text, index);
    }

    /**
     * Deletes the rule at an index of the group (CSSOM <code>deleteRule</code>).
     * <p>
     * 그룹의 위치에 있는 규칙을 지운다(CSSOM <code>deleteRule</code>).
     *
     * @param index the position / 위치
     * @throws org.duckdns.hjow.swingwebpanel.dom.DomException <code>IndexSizeError</code> / <code>IndexSizeError</code>
     */
    public void deleteRule(int index) {
        RuleEditor.delete(getParentStyleSheet(), rules, index);
    }

    /**
     * Puts the group and the rules inside it into a sheet.
     * <p>
     * 그룹과 그 안의 규칙을 시트에 넣는다.
     *
     * @param sheet  the sheet / 시트
     * @param parent the containing rule or null / 담은 규칙 또는 null
     */
    void attach(StyleSheet sheet, CssRule parent) {
        super.attach(sheet, parent);
        // 안의 규칙마다 이 그룹을 부모로 하여 같은 시트에 붙인다.
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).attach(sheet, this);
        }
    }

    /**
     * Takes the group and the rules inside it out of their sheet.
     * <p>
     * 그룹과 그 안의 규칙을 시트에서 뺀다.
     */
    void detach() {
        super.detach();
        // 안의 규칙도 시트에서 뗀다.
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).detach();
        }
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        if (statement) {
            return "@layer " + condition + ";";
        }
        String head = type == MEDIA_RULE ? "@media " + media.toCssText() : type == SUPPORTS_RULE ? "@supports " + condition
                : type == CONTAINER_RULE ? "@container " + condition : type == SCOPE_RULE ? "@scope" + (condition.length() > 0 ? " " + condition : "")
                : type == STARTING_STYLE_RULE ? "@starting-style" : "@layer" + (condition.length() > 0 ? " " + condition : "");
        StringBuilder sb = new StringBuilder(head).append(" {");
        // 포함된 규칙을 차례로 이어 쓴다.
        for (int i = 0; i < rules.size(); i++) {
            sb.append("\n  ").append(rules.get(i).getCssText());
        }
        return sb.append("\n}").toString();
    }
}
