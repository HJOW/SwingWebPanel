package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;

/**
 * The script object of a <code>CSSStyleDeclaration</code>: either the <em>inline style</em> of an element (<code>element.style</code>,
 * readable and writable; every write parses the <code>style</code> attribute, changes one declaration and writes the attribute back, so the
 * style resolver and the serialization always agree) or a <em>computed style</em> snapshot (<code>getComputedStyle</code>, read-only).
 * CSS properties are reachable by their dashed names and by their camel-case names (<code>style.backgroundColor</code>,
 * <code>style['background-color']</code>, <code>style.cssFloat</code>); other names are ordinary properties.
 * <p>
 * <code>CSSStyleDeclaration</code> 의 스크립트 객체: 요소의 <em>인라인 스타일</em>(<code>element.style</code>, 읽기/쓰기. 쓸 때마다
 * <code>style</code> 속성을 해석해 선언 하나를 바꾸고 속성에 다시 써서 스타일 리졸버와 직렬화가 항상 일치한다) 또는 <em>계산 스타일</em> 스냅샷
 * (<code>getComputedStyle</code>, 읽기 전용). CSS 프로퍼티는 대시 이름과 카멜 케이스 이름(<code>style.backgroundColor</code>,
 * <code>style['background-color']</code>, <code>style.cssFloat</code>)으로 접근하며 그 밖의 이름은 일반 프로퍼티이다.
 */
public final class StyleObject extends HostObject {

    /**
     * The element whose <code>style</code> attribute is edited, or null for a computed style.
     * <p>
     * <code>style</code> 속성을 편집하는 요소. 계산 스타일이면 null.
     */
    private final Element element;

    /**
     * The computed style snapshot, or null for an inline style.
     * <p>
     * 계산 스타일 스냅샷. 인라인 스타일이면 null.
     */
    private final ComputedStyle computed;

    /**
     * The declaration block of a rule (the <code>style</code> of a <code>CSSStyleRule</code> or a <code>CSSKeyframeRule</code>), or null. The object
     * edits this block itself, which is the one that the style resolver reads, so a change takes effect at once.
     * <p>
     * 규칙의 선언 블록(<code>CSSStyleRule</code>, <code>CSSKeyframeRule</code> 의 <code>style</code>). 아니면 null. 객체는 스타일 리졸버가 읽는 바로 이 블록을
     * 직접 편집하므로 변경이 즉시 반영된다.
     */
    private final CssStyleDeclaration block;

    /** The property names of a computed style, listed when first asked for (the computed style is a snapshot). / 계산 스타일의 프로퍼티 이름. 처음 요청할 때 나열한다(계산 스타일은 스냅샷이다). */
    private List<String> computedNames;

    /**
     * Creates the object.
     * <p>
     * 객체를 만든다.
     *
     * @param bridge   the bridge / 브리지
     * @param proto    the CSSStyleDeclaration prototype / CSSStyleDeclaration 프로토타입
     * @param element  the element for an inline style, or null / 인라인 스타일이면 요소, 아니면 null
     * @param computed the computed style, or null / 계산 스타일 또는 null
     */
    public StyleObject(DomBridge bridge, JsObject proto, Element element, ComputedStyle computed) {
        super(bridge, proto, element != null ? element : computed);
        this.element = element;
        this.computed = computed;
        this.block = null;
    }

    /**
     * Creates the object of the declaration block of a rule (<code>rule.style</code>), which is read and written directly.
     * <p>
     * 규칙의 선언 블록(<code>rule.style</code>)의 객체를 만든다. 블록을 직접 읽고 쓴다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the CSSStyleDeclaration prototype / CSSStyleDeclaration 프로토타입
     * @param block  the declaration block of the rule / 규칙의 선언 블록
     */
    public StyleObject(DomBridge bridge, JsObject proto, CssStyleDeclaration block) {
        super(bridge, proto, block);
        this.element = null;
        this.computed = null;
        this.block = block;
    }

    /**
     * Returns the rule that owns the declaration block (the CSSOM <code>parentRule</code>).
     * <p>
     * 선언 블록을 가진 규칙을 반환한다(CSSOM <code>parentRule</code>).
     *
     * @return the rule, or null for the style of an element and for a computed style / 규칙. 요소의 스타일이나 계산 스타일이면 null
     */
    public CssRule getParentRule() {
        return block == null ? null : block.getParentRule();
    }

    /**
     * Tells whether this is a read-only computed style.
     * <p>
     * 읽기 전용 계산 스타일인지 알려 준다.
     *
     * @return true for computed styles / 계산 스타일이면 true
     */
    public boolean isComputed() {
        return computed != null;
    }

    /**
     * Parses the current inline style.
     * <p>
     * 현재 인라인 스타일을 해석한다.
     *
     * @return the declaration block / 선언 블록
     */
    private CssStyleDeclaration declaration() {
        if (block != null) {
            return block;
        }
        String text = element.getAttribute("style");
        // 호환 모드 문서의 style 은 호환 모드로 파싱한다(단위 없는 길이와 # 없는 16진수 색을 읽고 쓴다, CSS Values 4 부록 C).
        boolean quirks = element.document().getCompatibilityMode() == org.duckdns.hjow.swingwebpanel.dom.Document.QUIRKS_MODE;
        return CssStyleDeclaration.parse(text == null ? "" : text, element.document().getBaseUri(), new CssDiagnostics(), quirks);
    }

    /**
     * Writes a declaration block back to the <code>style</code> attribute (the block of a rule is edited in place, so there is nothing to write).
     * <p>
     * 선언 블록을 <code>style</code> 속성에 다시 쓴다(규칙의 블록은 제자리에서 편집하므로 쓸 것이 없다).
     *
     * @param d the block / 블록
     */
    private void commit(CssStyleDeclaration d) {
        if (block == null) {
            element.setAttribute("style", d.getCssText());
        }
    }

    /**
     * Refuses a change of a computed style: its declarations are read-only (CSSOM, the "readonly flag" of a resolved style declaration), so a
     * change throws a <code>NoModificationAllowedError</code>.
     * <p>
     * 계산 스타일의 변경을 거부한다: 계산 스타일의 선언은 읽기 전용이므로(CSSOM, 해석된 스타일 선언의 "readonly flag") 변경하면
     * <code>NoModificationAllowedError</code> 를 던진다.
     *
     * @param name the property that the script tried to change / 스크립트가 바꾸려 한 프로퍼티
     */
    private void checkWritable(String name) {
        if (computed != null) {
            DomException e = new DomException("NoModificationAllowedError", DomException.NO_MODIFICATION_ALLOWED_ERR, "Failed to set the '" + name
                    + "' property on 'CSSStyleDeclaration': These styles are computed, and therefore the '" + name + "' property is read-only.");
            throw getBridge() == null ? e : getBridge().domError(e);
        }
    }

    /**
     * Returns the value of a property as the standard serializes it.
     * <p>
     * 표준이 직렬화하는 대로 프로퍼티의 값을 반환한다.
     *
     * @param name the dashed property name / 대시 프로퍼티 이름
     * @return the value or "" / 값 또는 ""
     */
    public String getPropertyValue(String name) {
        return computed != null ? computed.getPropertyValue(name) : declaration().getPropertyValue(name);
    }

    /**
     * Returns the priority (<code>important</code> or "") of an inline declaration.
     * <p>
     * 인라인 선언의 우선순위(<code>important</code> 또는 "")를 반환한다.
     *
     * @param name the dashed property name / 대시 프로퍼티 이름
     * @return the priority / 우선순위
     */
    public String getPropertyPriority(String name) {
        return computed != null ? "" : declaration().getPropertyPriority(name);
    }

    /**
     * Sets or removes a property of the inline style (an empty value removes it); ignored for computed styles.
     * <p>
     * 인라인 스타일의 프로퍼티를 지정하거나 제거한다(빈 값은 제거). 계산 스타일에서는 무시한다.
     *
     * @param name     the dashed name / 대시 이름
     * @param value    the value / 값
     * @param priority the priority / 우선순위
     */
    public void setProperty(String name, String value, String priority) {
        checkWritable(name);
        CssStyleDeclaration d = declaration();
        if (value.length() == 0) {
            d.removeProperty(name);
        } else {
            d.setProperty(name, value, priority);
        }
        commit(d);
    }

    /**
     * Removes a property of the inline style and returns its old value.
     * <p>
     * 인라인 스타일의 프로퍼티를 제거하고 이전 값을 반환한다.
     *
     * @param name the dashed name / 대시 이름
     * @return the old value / 이전 값
     */
    public String removeProperty(String name) {
        checkWritable(name);
        CssStyleDeclaration d = declaration();
        String old = d.removeProperty(name);
        commit(d);
        return old == null ? "" : old;
    }

    /**
     * Returns the serialized declarations (<code>cssText</code>).
     * <p>
     * 직렬화한 선언(<code>cssText</code>)을 반환한다.
     *
     * @return the text / 텍스트
     */
    public String getCssText() {
        return computed != null ? "" : declaration().getCssText();
    }

    /**
     * Replaces all declarations (<code>cssText = ...</code>).
     * <p>
     * 모든 선언을 바꾼다(<code>cssText = ...</code>).
     *
     * @param text the new text / 새 텍스트
     */
    public void setCssText(String text) {
        checkWritable("cssText");
        if (block != null) {
            block.setCssText(text);
        } else if (computed == null) {
            element.setAttribute("style", text);
        }
    }

    /**
     * Returns the number of declarations (inline style only).
     * <p>
     * 선언 수를 반환한다(인라인 스타일만).
     *
     * @return the count / 개수
     */
    public int getLength() {
        return computed != null ? computedNames().size() : declaration().getLength();
    }

    /**
     * Lists the properties of a computed style: every longhand property that has a value, followed by the custom properties.
     * <p>
     * 계산 스타일의 프로퍼티를 나열한다: 값이 있는 모든 개별 속성, 이어서 사용자 정의 프로퍼티.
     *
     * @return the names / 이름
     */
    private List<String> computedNames() {
        if (computedNames == null) {
            List<String> out = new ArrayList<String>();
            PropertyRegistry reg = PropertyRegistry.getInstance();
            // 등록된 개별 속성을 id 순서로 훑어 계산 값이 있는 것을 모은다.
            for (int id = 1; id <= Prop.COUNT; id++) {
                org.duckdns.hjow.swingwebpanel.css.props.Property p = reg.byId(id);
                if (p != null && computed.getPropertyValue(p.getName()).length() > 0) {
                    out.add(p.getName());
                }
            }
            out.addAll(computed.customPropertyNames());
            computedNames = out;
        }
        return computedNames;
    }

    /**
     * Returns the name of the declaration at an index.
     * <p>
     * 위치의 선언 이름을 반환한다.
     *
     * @param i the index / 위치
     * @return the name or "" / 이름 또는 ""
     */
    public String item(int i) {
        String s;
        if (computed != null) {
            List<String> names = computedNames();
            s = i >= 0 && i < names.size() ? names.get(i) : "";
        } else {
            s = declaration().item(i);
        }
        return s == null ? "" : s;
    }

    /**
     * Converts a script property name to a dashed CSS property name, or returns null if the name is not a CSS property.
     * <p>
     * 스크립트 프로퍼티 이름을 대시 CSS 프로퍼티 이름으로 바꾼다. CSS 프로퍼티가 아니면 null 을 반환한다.
     *
     * @param key the property key / 프로퍼티 키
     * @return the dashed name or null / 대시 이름 또는 null
     */
    public static String cssName(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        String k = (String) key;
        if (k.startsWith("--")) {
            return k;
        }
        String dashed;
        if ("cssFloat".equals(k)) {
            dashed = "float";
        } else {
            StringBuilder sb = new StringBuilder();
            // 대문자 앞에 대시를 넣고 소문자로 바꿔 카멜 케이스를 대시 이름으로 만든다.
            for (int i = 0; i < k.length(); i++) {
                char c = k.charAt(i);
                if (Character.isUpperCase(c)) {
                    sb.append('-').append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            dashed = sb.toString();
            if (dashed.startsWith("webkit-")) {
                dashed = "-" + dashed;
            }
        }
        PropertyRegistry reg = PropertyRegistry.getInstance();
        return reg.byName(dashed) != null || reg.getShorthand(dashed) != null ? dashed : null;
    }

    /**
     * Returns a CSS property as a data property when the key names one (and is not a member of the interface).
     * <p>
     * 키가 CSS 프로퍼티를 가리키고 인터페이스 멤버가 아니면 CSS 프로퍼티를 데이터 프로퍼티로 반환한다.
     *
     * @param key the key / 키
     * @return the property slot or null / 프로퍼티 칸 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property own = super.getOwnPropertySlot(key);
        if (own != null) {
            return own;
        }
        if (key instanceof String && org.duckdns.hjow.swingwebpanel.js.PropertyKeys.arrayIndex((String) key) >= 0) {
            int i = (int) org.duckdns.hjow.swingwebpanel.js.PropertyKeys.arrayIndex((String) key);
            return i < getLength() ? Property.data(Js.str(item(i)), false, true, true) : null;
        }
        JsObject proto = getPrototypeOf();
        String name = proto != null && proto.hasProperty(key) ? null : cssName(key);
        return name == null ? null : Property.data(Js.str(getPropertyValue(name)), computed == null, true, true);
    }

    /**
     * Assigns a CSS property of the inline style (<code>style.color = 'red'</code>).
     * <p>
     * 인라인 스타일의 CSS 프로퍼티를 지정한다(<code>style.color = 'red'</code>).
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / 수신자
     * @return true if handled / 처리했으면 true
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        JsObject proto = getPrototypeOf();
        String name = proto != null && proto.hasProperty(key) ? null : cssName(key);
        if (name != null && super.getOwnPropertySlot(key) == null) {
            setProperty(name, value.isNull() ? "" : Ops.toString(value), "");
            return true;
        }
        return super.set(key, value, receiver);
    }

    /**
     * Returns the declaration names (indices) followed by the ordinary own keys.
     * <p>
     * 선언 이름(위치)에 이어 일반 자체 키를 반환한다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> keys = new ArrayList<Object>();
        int n = getLength();
        // 선언 위치를 문자열 키로 나열한다.
        for (int i = 0; i < n; i++) {
            keys.add(org.duckdns.hjow.swingwebpanel.js.PropertyKeys.fromIndex(i));
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
