package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.props.Shorthand;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;

/**
 * A declaration block: the declarations of a style rule, of a <code>style</code> attribute or of a keyframe, parsed into longhand values. Shorthands are
 * expanded when they are stored; the normal and the <code>!important</code> declarations are kept apart because the cascade treats them as different
 * layers (within a block a later declaration replaces an earlier one of the same importance only). Custom properties are kept as component values.
 * The object also offers the CSSOM view of a declaration block (<code>getPropertyValue</code>, <code>setProperty</code>, <code>removeProperty</code>,
 * <code>cssText</code>) for the <code>style</code> object of an element.
 * <p>
 * 선언 블록: 스타일 규칙, <code>style</code> 속성, 키프레임의 선언을 개별 속성 값으로 파싱한 것. 단축 속성은 저장할 때 전개한다. 일반 선언과 <code>!important</code> 선언은
 * 캐스케이드가 서로 다른 층으로 다루므로 따로 보관한다(한 블록 안에서는 중요도가 같은 앞 선언만 뒤 선언으로 대체된다). 사용자 정의 속성은 컴포넌트 값으로 보관한다.
 * 요소의 <code>style</code> 객체를 위한 CSSOM 관점(<code>getPropertyValue</code>, <code>setProperty</code>, <code>removeProperty</code>, <code>cssText</code>)도 제공한다.
 */
public final class CssStyleDeclaration {

    /** Normal longhand declarations by id, in source order. / 소스 순서의 일반 개별 속성 선언(id 별). */
    private final Map<Integer, CssValue> normal = new LinkedHashMap<Integer, CssValue>();

    /** Important longhand declarations by id. / 중요한 개별 속성 선언(id 별). */
    private final Map<Integer, CssValue> important = new LinkedHashMap<Integer, CssValue>();

    /** Normal custom property declarations. / 일반 사용자 정의 속성 선언. */
    private final Map<String, List<ComponentValue>> customNormal = new LinkedHashMap<String, List<ComponentValue>>();

    /** Important custom property declarations. / 중요한 사용자 정의 속성 선언. */
    private final Map<String, List<ComponentValue>> customImportant = new LinkedHashMap<String, List<ComponentValue>>();

    /** Listener called after every change, or null. / 변경할 때마다 호출되는 리스너. 없으면 null. */
    private Runnable onChange;

    /** The base URL that relative urls written through {@link #setProperty} and {@link #setCssText} resolve against, or null. / {@link #setProperty}, {@link #setCssText} 로 쓰는 상대 url 을 해석하는 기준 URL. 없으면 null. */
    private String baseUrl;

    /** The rule whose block this is (the CSSOM <code>parentRule</code>), or null. / 이 블록을 가진 규칙(CSSOM <code>parentRule</code>). 없으면 null. */
    private CssRule parentRule;

    /** True if text written through {@link #setProperty} and {@link #setCssText} is parsed in quirks mode (the mode of the owner of the block). / {@link #setProperty}, {@link #setCssText} 로 쓰는 문자열을 호환 모드로 파싱하면 true(블록 소유자의 모드). */
    private boolean quirks;

    /** The longhand ids that this block accepts (the descriptors of an <code>@position-try</code> rule), or null if it accepts every property. / 이 블록이 받는 개별 속성 id(<code>@position-try</code> 규칙의 서술자). 모든 속성을 받으면 null. */
    private java.util.Set<Integer> allowed;

    /**
     * Creates an empty declaration block.
     * <p>
     * 빈 선언 블록을 만든다.
     */
    public CssStyleDeclaration() {
    }

    /**
     * Parses declaration text (the content of a <code>style</code> attribute) into a new block.
     * <p>
     * 선언 문자열(<code>style</code> 속성의 내용)을 파싱해 새 블록을 만든다.
     *
     * @param text declarations / 선언
     * @param base base URL for relative urls or null / 상대 url 의 기준 URL 또는 null
     * @param diag collector or null / 수집기 또는 null
     * @return the block / 블록
     */
    public static CssStyleDeclaration parse(String text, String base, CssDiagnostics diag) {
        return parse(text, base, diag, false);
    }

    /**
     * Parses declaration text into a new block, optionally in quirks mode (unitless lengths and hashless hex colors are accepted by the
     * properties that the Quirks Mode Standard names).
     * <p>
     * 선언 문자열을 새 블록으로 파싱한다. 호환 모드로 파싱할 수 있다(호환 모드 표준이 정한 속성에서 단위 없는 길이와 # 없는 16진수 색을 받는다).
     *
     * @param text   declarations / 선언
     * @param base   base URL for relative urls or null / 상대 url 의 기준 URL 또는 null
     * @param diag   collector or null / 수집기 또는 null
     * @param quirks true to parse in quirks mode / 호환 모드로 파싱하면 true
     * @return the block / 블록
     */
    public static CssStyleDeclaration parse(String text, String base, CssDiagnostics diag, boolean quirks) {
        CssStyleDeclaration d = new CssStyleDeclaration();
        d.quirks = quirks;
        d.addAll(CssParser.parseDeclarationList(text == null ? "" : text, diag), new ParseEnv(PropertyRegistry.getInstance(), base, quirks), diag);
        return d;
    }

    /**
     * Sets whether text that is written later through {@link #setProperty} and {@link #setCssText} (the <code>style</code> of an element or a rule,
     * changed by script) is parsed in quirks mode, as the document or style sheet that owns the block is (CSS Values 4 appendix C: the
     * quirky lengths and the quirky hex colors are accepted when CSS is parsed in quirks mode).
     * <p>
     * 나중에 {@link #setProperty}, {@link #setCssText} 로 쓰는 문자열(스크립트가 바꾸는 요소나 규칙의 <code>style</code>)을 블록을 가진 문서나 스타일 시트처럼 호환
     * 모드로 파싱할지 정한다(CSS Values 4 부록 C: 호환 모드로 CSS 를 파싱할 때 quirky length 와 quirky hex color 를 받는다).
     *
     * @param quirks true to parse in quirks mode / 호환 모드로 파싱하면 true
     */
    public void setQuirks(boolean quirks) {
        this.quirks = quirks;
    }

    /**
     * Tells whether text that is written through {@link #setProperty} and {@link #setCssText} is parsed in quirks mode.
     * <p>
     * {@link #setProperty}, {@link #setCssText} 로 쓰는 문자열을 호환 모드로 파싱하는지 알려 준다.
     *
     * @return true for quirks mode / 호환 모드이면 true
     */
    public boolean isQuirks() {
        return quirks;
    }

    /**
     * Restricts the block to some properties: a declaration that sets any other longhand is dropped as a whole (and reported). The block of an
     * <code>@position-try</code> rule takes the inset, margin, size and self-alignment properties, <code>position-anchor</code> and <code>position-area</code>
     * only (CSS Anchor Positioning 1 section 6.4). Custom properties are not accepted either, and <code>!important</code> makes a declaration invalid.
     * <p>
     * 블록이 받는 속성을 제한한다: 다른 개별 속성을 하나라도 정하는 선언은 통째로 버린다(보고한다). <code>@position-try</code> 규칙의
     * 블록은 오프셋, 마진, 크기, 자기 정렬 속성과 <code>position-anchor</code>, <code>position-area</code> 만 받는다(CSS Anchor Positioning 1 6.4 절). 사용자 정의 속성도 받지 않는다.
     *
     * @param ids the accepted longhand ids / 받는 개별 속성 id
     */
    void restrictTo(java.util.Set<Integer> ids) {
        this.allowed = ids;
    }

    /**
     * Registers a listener that is called after each change.
     * <p>
     * 변경마다 호출되는 리스너를 등록한다.
     *
     * @param r listener or null / 리스너 또는 null
     */
    public void setChangeListener(Runnable r) {
        this.onChange = r;
    }

    /**
     * Sets the base URL that relative urls written through {@link #setProperty} and {@link #setCssText} resolve against (the URL of the style sheet
     * of a rule).
     * <p>
     * {@link #setProperty}, {@link #setCssText} 로 쓰는 상대 url 을 해석하는 기준 URL 을 설정한다(규칙이 속한 스타일 시트의 URL).
     *
     * @param base the base URL or null / 기준 URL 또는 null
     */
    public void setBaseUrl(String base) {
        this.baseUrl = base;
    }

    /**
     * Returns the rule that owns this block (the CSSOM <code>parentRule</code>).
     * <p>
     * 이 블록을 가진 규칙을 반환한다(CSSOM <code>parentRule</code>).
     *
     * @return the rule, or null for the block of an element or a computed style / 규칙. 요소나 계산 스타일의 블록이면 null
     */
    public CssRule getParentRule() {
        return parentRule;
    }

    /**
     * Records the rule that owns this block.
     * <p>
     * 이 블록을 가진 규칙을 기록한다.
     *
     * @param rule the rule / 규칙
     */
    void setParentRule(CssRule rule) {
        this.parentRule = rule;
    }

    /**
     * Notifies the change listener.
     * <p>
     * 변경 리스너에 알린다.
     */
    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }

    /**
     * Stores parsed declarations (the specified values, after shorthand expansion).
     * <p>
     * 파싱한 선언(단축 속성 전개 뒤의 지정 값)을 저장한다.
     *
     * @param decls the declarations / 선언
     * @param env   parse environment / 파싱 환경
     * @param diag  collector or null / 수집기 또는 null
     */
    public void addAll(List<SyntaxDeclaration> decls, ParseEnv env, CssDiagnostics diag) {
        // 선언을 소스 순서대로 하나씩 저장한다.
        for (int i = 0; i < decls.size(); i++) {
            add(decls.get(i), env, diag);
        }
    }

    /**
     * Stores one declaration; invalid, unknown and unsupported declarations are dropped and reported.
     * <p>
     * 선언 하나를 저장한다. 올바르지 않거나 알 수 없거나 지원하지 않는 선언은 버리고 보고한다.
     *
     * @param d    the declaration / 선언
     * @param env  parse environment / 파싱 환경
     * @param diag collector or null / 수집기 또는 null
     * @return true if the declaration was stored / 저장했으면 true
     */
    public boolean add(SyntaxDeclaration d, ParseEnv env, CssDiagnostics diag) {
        return add(d, env, diag, false);
    }

    /**
     * Stores one declaration, either as the parser does (a later declaration of a property replaces the earlier one of the same importance and moves
     * to the end) or as the CSSOM <code>setProperty</code> does (<code>inPlace</code>: an existing declaration of the property is updated where it is,
     * whatever its importance was, so a change of the priority replaces the old declaration).
     * <p>
     * 선언 하나를 저장한다. 파서처럼(속성의 뒤 선언이 같은 중요도의 앞 선언을 대체하고 끝으로 옮긴다) 하거나, CSSOM <code>setProperty</code> 처럼
     * (<code>inPlace</code>: 속성의 기존 선언을 중요도와 무관하게 그 자리에서 갱신하므로 우선순위를 바꾸면 이전 선언이 대체된다) 한다.
     *
     * @param d       the declaration / 선언
     * @param env     parse environment / 파싱 환경
     * @param diag    collector or null / 수집기 또는 null
     * @param inPlace true for the CSSOM way / CSSOM 방식이면 true
     * @return true if the declaration was stored / 저장했으면 true
     */
    private boolean add(SyntaxDeclaration d, ParseEnv env, CssDiagnostics diag, boolean inPlace) {
        if (allowed != null && (d.isCustomProperty() || d.isImportant())) {
            // 제한된 블록은 사용자 정의 속성을 받지 않고 !important 는 그 속성을 무효로 만든다(@position-try, 6.4 절).
            if (diag != null) {
                diag.add(CssDiagnostics.INVALID, d.getName(), d.isImportant() ? "!important is invalid in this rule" : "custom properties are not accepted in this rule");
            }
            return false;
        }
        if (d.isCustomProperty()) {
            // CSSOM 방식이면 다른 중요도의 같은 사용자 정의 프로퍼티 선언을 지운다.
            if (inPlace) {
                (d.isImportant() ? customNormal : customImportant).remove(d.getName());
            }
            (d.isImportant() ? customImportant : customNormal).put(d.getName(), new ArrayList<ComponentValue>(d.getValue()));
            return true;
        }
        PropertyRegistry reg = PropertyRegistry.getInstance();
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parseDeclaration(d.getName(), d.getValue(), env, out);
        if (r != PropertyRegistry.OK) {
            if (diag != null) {
                diag.add(r == PropertyRegistry.UNKNOWN ? CssDiagnostics.UNKNOWN : r == PropertyRegistry.UNSUPPORTED ? CssDiagnostics.UNSUPPORTED
                        : CssDiagnostics.INVALID, d.getName(), r == PropertyRegistry.INVALID ? "invalid value: " + d.getValueText() : "declaration ignored");
            }
            return false;
        }
        // 제한된 블록이면 받지 않는 개별 속성이 하나라도 있는 선언은 통째로 버린다.
        if (allowed != null && !allowed.containsAll(out.keySet())) {
            if (diag != null) {
                diag.add(CssDiagnostics.INVALID, d.getName(), "this property is not accepted in this rule");
            }
            return false;
        }
        Map<Integer, CssValue> target = d.isImportant() ? important : normal;
        Map<Integer, CssValue> other = d.isImportant() ? normal : important;
        // 전개된 개별 속성 값을 저장한다(파서는 같은 중요도의 앞 선언을 대체하며 순서를 뒤로 옮기고, CSSOM 은 그 자리에서 갱신하며 다른 중요도의 선언을 지운다).
        for (Map.Entry<Integer, CssValue> e : out.entrySet()) {
            if (inPlace) {
                other.remove(e.getKey());
            } else {
                target.remove(e.getKey());
            }
            target.put(e.getKey(), e.getValue());
        }
        if (diag != null && out.size() == 1 && reg.byId(out.keySet().iterator().next().intValue()).isParsedOnly()) {
            diag.add(CssDiagnostics.PARSED_ONLY, d.getName(), "parsed but not applied by the renderer");
        }
        return true;
    }

    /**
     * Returns the normal longhand declarations.
     * <p>
     * 일반 개별 속성 선언을 반환한다.
     *
     * @return map by property id in source order (do not modify) / 소스 순서의 속성 id 별 맵(수정하지 말 것)
     */
    public Map<Integer, CssValue> normal() {
        return normal;
    }

    /**
     * Returns the important longhand declarations.
     * <p>
     * 중요한 개별 속성 선언을 반환한다.
     *
     * @return map by property id (do not modify) / 속성 id 별 맵(수정하지 말 것)
     */
    public Map<Integer, CssValue> important() {
        return important;
    }

    /**
     * Returns the normal custom property declarations.
     * <p>
     * 일반 사용자 정의 속성 선언을 반환한다.
     *
     * @return map by name (do not modify) / 이름별 맵(수정하지 말 것)
     */
    public Map<String, List<ComponentValue>> customNormal() {
        return customNormal;
    }

    /**
     * Returns the important custom property declarations.
     * <p>
     * 중요한 사용자 정의 속성 선언을 반환한다.
     *
     * @return map by name (do not modify) / 이름별 맵(수정하지 말 것)
     */
    public Map<String, List<ComponentValue>> customImportant() {
        return customImportant;
    }

    /**
     * Tells whether the block has no declaration.
     * <p>
     * 블록에 선언이 없는지 알려 준다.
     *
     * @return true if empty / 비어 있으면 true
     */
    public boolean isEmpty() {
        return normal.isEmpty() && important.isEmpty() && customNormal.isEmpty() && customImportant.isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // CSSOM 관점
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the number of properties (longhands and custom properties) in the block.
     * <p>
     * 블록에 든 속성(개별 속성과 사용자 정의 속성)의 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getLength() {
        return names().size();
    }

    /**
     * Returns the name of the property at an index (CSSOM <code>item()</code>).
     * <p>
     * 위치의 속성 이름을 반환한다(CSSOM <code>item()</code>).
     *
     * @param i index / 위치
     * @return the name, or the empty string if out of range / 이름. 범위를 벗어나면 빈 문자열
     */
    public String item(int i) {
        List<String> n = names();
        return i >= 0 && i < n.size() ? n.get(i) : "";
    }

    /**
     * Lists the names of the properties in the block: longhands in the order of their last declaration, then custom properties.
     * <p>
     * 블록의 속성 이름을 나열한다: 마지막 선언 순서의 개별 속성, 그 뒤에 사용자 정의 속성.
     *
     * @return the names / 이름
     */
    private List<String> names() {
        List<String> out = new ArrayList<String>();
        PropertyRegistry reg = PropertyRegistry.getInstance();
        // 일반과 중요 선언의 개별 속성 이름을 중복 없이 모은다.
        for (Integer id : normal.keySet()) {
            out.add(reg.byId(id.intValue()).getName());
        }
        // 항목 의 항목을 하나씩 처리한다.
        for (Integer id : important.keySet()) {
            String n = reg.byId(id.intValue()).getName();
            if (!out.contains(n)) {
                out.add(n);
            }
        }
        // 항목 의 항목을 하나씩 처리한다.
        for (String n : customNormal.keySet()) {
            out.add(n);
        }
        // 항목 의 항목을 하나씩 처리한다.
        for (String n : customImportant.keySet()) {
            if (!out.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * Returns the value of a property as text (CSSOM <code>getPropertyValue</code>); a shorthand is built from its longhands.
     * <p>
     * 속성의 값을 문자열로 반환한다(CSSOM <code>getPropertyValue</code>). 단축 속성은 개별 속성으로 만든다.
     *
     * @param name the property name / 속성 이름
     * @return the value, or the empty string if it is not set / 값. 설정되지 않았으면 빈 문자열
     */
    public String getPropertyValue(String name) {
        if (PropertyRegistry.isCustomProperty(name)) {
            List<ComponentValue> t = customImportant.containsKey(name) ? customImportant.get(name) : customNormal.get(name);
            return t == null ? "" : CssBlock.join(t).trim();
        }
        PropertyRegistry reg = PropertyRegistry.getInstance();
        int[] ids = reg.longhandsOf(name);
        if (ids == null) {
            return "";
        }
        Map<Integer, CssValue> vals = new LinkedHashMap<Integer, CssValue>();
        // 개별 속성이 모두 설정되어 있어야 단축 속성 값을 만들 수 있다.
        for (int i = 0; i < ids.length; i++) {
            CssValue v = important.containsKey(Integer.valueOf(ids[i])) ? important.get(Integer.valueOf(ids[i])) : normal.get(Integer.valueOf(ids[i]));
            if (v == null) {
                return "";
            }
            vals.put(Integer.valueOf(ids[i]), v);
        }
        if (ids.length == 1) {
            return vals.get(Integer.valueOf(ids[0])).toCssText();
        }
        Shorthand s = reg.getShorthand(name);
        String t = s == null ? null : s.serialize(vals);
        return t == null ? "" : t;
    }

    /**
     * Returns <code>important</code> if the property was declared important, else the empty string.
     * <p>
     * 속성이 important 로 선언되었으면 <code>important</code>, 아니면 빈 문자열을 반환한다.
     *
     * @param name the property name / 속성 이름
     * @return the priority / 우선순위
     */
    public String getPropertyPriority(String name) {
        if (PropertyRegistry.isCustomProperty(name)) {
            return customImportant.containsKey(name) ? "important" : "";
        }
        int[] ids = PropertyRegistry.getInstance().longhandsOf(name);
        // 모든 개별 속성이 important 이면 important 이다.
        for (int i = 0; ids != null && i < ids.length; i++) {
            if (!important.containsKey(Integer.valueOf(ids[i]))) {
                return "";
            }
        }
        return ids == null ? "" : "important";
    }

    /**
     * Sets a property from text (CSSOM <code>setProperty</code>). An invalid value leaves the block unchanged.
     * <p>
     * 문자열로 속성을 설정한다(CSSOM <code>setProperty</code>). 올바르지 않은 값은 블록을 바꾸지 않는다.
     *
     * @param name     the property name / 속성 이름
     * @param value    the value text; an empty value removes the property / 값 문자열. 빈 값은 속성을 지운다
     * @param priority empty or <code>important</code> / 빈 문자열 또는 <code>important</code>
     * @return true if the block changed / 블록이 바뀌었으면 true
     */
    public boolean setProperty(String name, String value, String priority) {
        if (value == null || value.trim().length() == 0) {
            return removeProperty(name).length() > 0;
        }
        boolean imp = "important".equalsIgnoreCase(priority);
        if (priority != null && priority.length() > 0 && !imp) {
            return false;
        }
        List<ComponentValue> tokens = CssParser.trim(CssParser.parseComponentValues(value));
        boolean ok = add(new SyntaxDeclaration(name, tokens, imp), new ParseEnv(PropertyRegistry.getInstance(), baseUrl, quirks), null, true);
        if (ok) {
            changed();
        }
        return ok;
    }

    /**
     * Removes a property or all longhands of a shorthand (CSSOM <code>removeProperty</code>).
     * <p>
     * 속성이나 단축 속성의 모든 개별 속성을 지운다(CSSOM <code>removeProperty</code>).
     *
     * @param name the property name / 속성 이름
     * @return the previous value text (empty if it was not set) / 이전 값 문자열(설정되지 않았으면 빈 문자열)
     */
    public String removeProperty(String name) {
        String old = getPropertyValue(name);
        boolean removed = false;
        if (PropertyRegistry.isCustomProperty(name)) {
            removed = customNormal.remove(name) != null;
            removed |= customImportant.remove(name) != null;
        } else {
            int[] ids = PropertyRegistry.getInstance().longhandsOf(name);
            // 개별 속성을 하나씩 지운다.
            for (int i = 0; ids != null && i < ids.length; i++) {
                removed |= normal.remove(Integer.valueOf(ids[i])) != null;
                removed |= important.remove(Integer.valueOf(ids[i])) != null;
            }
        }
        if (removed) {
            changed();
        }
        return old;
    }

    /**
     * Replaces the whole block with parsed declaration text (CSSOM <code>cssText</code> setter).
     * <p>
     * 블록 전체를 파싱한 선언 문자열로 바꾼다(CSSOM <code>cssText</code> 설정).
     *
     * @param text declarations / 선언
     */
    public void setCssText(String text) {
        normal.clear();
        important.clear();
        customNormal.clear();
        customImportant.clear();
        addAll(CssParser.parseDeclarationList(text == null ? "" : text, null), new ParseEnv(PropertyRegistry.getInstance(), baseUrl, quirks), null);
        changed();
    }

    /**
     * Serializes the block (CSSOM <code>cssText</code>): <code>name: value;</code> for each property, with shorthands collapsed when all their
     * longhands are present with the same priority and the shorthand knows how to serialize them.
     * <p>
     * 블록을 직렬화한다(CSSOM <code>cssText</code>): 속성마다 <code>name: value;</code>. 단축 속성의 모든 개별 속성이 같은 우선순위로 있고 단축 속성이 직렬화할 수 있으면 합친다.
     *
     * @return the text / 문자열
     */
    public String getCssText() {
        StringBuilder sb = new StringBuilder();
        PropertyRegistry reg = PropertyRegistry.getInstance();
        java.util.Set<String> done = new java.util.HashSet<String>();
        List<String> names = names();
        // 속성을 순서대로 쓰되 이미 단축 속성으로 쓴 개별 속성은 건너뛴다.
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (done.contains(n)) {
                continue;
            }
            String shorthand = collapse(reg, n, names, done);
            if (shorthand != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(shorthand);
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            String pri = getPropertyPriority(n);
            Property property = reg.byName(n);
            CssValue value = property == null ? null : (pri.length() > 0 ? important : normal).get(Integer.valueOf(property.getId()));
            String name = n;
            // 다시 파싱할 때 var() 결과의 문법이 달라지는 별칭은 쓰인 이름을 보존한다.
            if (value instanceof CssPending) {
                String original = ((CssPending) value).getProperty();
                if (n.equals(reg.canonicalName(original))) {
                    name = original;
                }
            }
            sb.append(name).append(": ").append(value == null ? getPropertyValue(n) : value.toCssText())
                    .append(pri.length() > 0 ? " !important" : "").append(';');
        }
        return sb.toString();
    }

    /**
     * Tries to write a longhand and its siblings as one shorthand declaration.
     * <p>
     * 개별 속성과 그 형제들을 단축 속성 선언 하나로 쓰려고 시도한다.
     *
     * @param reg   registry / 등록부
     * @param n     the longhand name / 개별 속성 이름
     * @param names all names present / 있는 모든 이름
     * @param done  receives the longhands that were written / 쓴 개별 속성을 받는다
     * @return the declaration text (with a leading space when needed) or null / 선언 문자열(필요하면 앞에 공백) 또는 null
     */
    private String collapse(PropertyRegistry reg, String n, List<String> names, java.util.Set<String> done) {
        String[] candidates = {"margin", "padding", "border-width", "border-style", "border-color", "overflow", "gap"};
        // 이 속성을 포함하는 후보 단축 속성을 찾아 모든 개별 속성이 같은 우선순위로 있으면 합친다.
        for (int c = 0; c < candidates.length; c++) {
            int[] ids = reg.longhandsOf(candidates[c]);
            boolean contains = false;
            boolean all = true;
            String pri = null;
            // ids 의 항목을 하나씩 처리한다.
            for (int i = 0; i < ids.length; i++) {
                String ln = reg.byId(ids[i]).getName();
                contains |= ln.equals(n);
                String p = getPropertyPriority(ln);
                all &= names.contains(ln) && (pri == null || pri.equals(p));
                pri = p;
            }
            if (contains && all) {
                String v = getPropertyValue(candidates[c]);
                if (v.length() > 0) {
                    // ids 의 항목을 하나씩 처리한다.
                    for (int i = 0; i < ids.length; i++) {
                        done.add(reg.byId(ids[i]).getName());
                    }
                    return candidates[c] + ": " + v + (pri.length() > 0 ? " !important" : "") + ";";
                }
            }
        }
        return null;
    }

    /**
     * Returns the longhand value stored for an id, important declarations first.
     * <p>
     * id 에 저장된 개별 속성 값을 반환한다. 중요한 선언이 우선이다.
     *
     * @param id the property id / 속성 id
     * @return the value or null / 값 또는 null
     */
    public CssValue get(int id) {
        CssValue v = important.get(Integer.valueOf(id));
        return v != null ? v : normal.get(Integer.valueOf(id));
    }

    /**
     * Returns a debugging text.
     * <p>
     * 디버깅용 문자열을 반환한다.
     *
     * @return the CSS text / CSS 문자열
     */
    public String toString() {
        return getCssText();
    }
}
