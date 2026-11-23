package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The attribute-derived defaults of the HTML pseudo-classes: link, form control state (disabled, checked, required, read-write, placeholder
 * shown, validity), language and direction, <code>:open</code>, <code>:defined</code>. They describe the element as the markup left it. Live
 * state that does not appear in attributes (typed text, a clicked checkbox) comes from an {@link ElementState} and wins over these defaults.
 * <p>
 * HTML 의사 클래스의 속성 기반 기본 판정: 링크, 폼 컨트롤 상태(disabled, checked, required, read-write, placeholder-shown, 유효성), 언어와 방향,
 * <code>:open</code>, <code>:defined</code>. 마크업이 남긴 그대로의 요소를 설명한다. 속성에 나타나지 않는 실시간 상태(입력한 글, 클릭한 체크박스)는
 * {@link ElementState} 가 제공하며 이 기본값보다 우선한다.
 */
final class HtmlStates {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HtmlStates() {
    }

    /**
     * Tells whether an element is in the HTML namespace.
     * <p>
     * 요소가 HTML 네임스페이스에 있는지 알려 준다.
     *
     * @param e element / 요소
     * @return true for HTML elements / HTML 요소이면 true
     */
    static boolean isHtml(Element e) {
        return Namespaces.HTML.equals(e.getNamespaceURI());
    }

    /**
     * Returns the lower-cased value of the <code>type</code> attribute, or the default of <code>input</code>.
     * <p>
     * <code>type</code> 속성의 소문자 값을 반환한다. 없으면 <code>input</code> 의 기본값.
     *
     * @param e element / 요소
     * @return type / 종류
     */
    private static String inputType(Element e) {
        String t = e.getAttribute("type");
        return t == null ? "text" : Namespaces.asciiLower(t.trim());
    }

    /**
     * Tells whether an input type holds editable text (or a similar free value).
     * <p>
     * input 종류가 편집 가능한 글(또는 비슷한 자유 값)을 담는지 알려 준다.
     *
     * @param type lower-case type / 소문자 종류
     * @return true for text-like types / 글 형태이면 true
     */
    private static boolean isTextLike(String type) {
        return type.equals("text") || type.equals("search") || type.equals("url") || type.equals("tel") || type.equals("email")
                || type.equals("password") || type.equals("number") || type.equals("date") || type.equals("datetime-local")
                || type.equals("month") || type.equals("week") || type.equals("time") || type.length() == 0 || !isKnownType(type);
    }

    /**
     * Tells whether an input type is one of the standard types (an unknown type behaves as text).
     * <p>
     * input 종류가 표준 종류인지 알려 준다(알 수 없는 종류는 text 처럼 동작한다).
     *
     * @param type lower-case type / 소문자 종류
     * @return true for standard types / 표준 종류이면 true
     */
    private static boolean isKnownType(String type) {
        String[] known = {"text", "search", "url", "tel", "email", "password", "number", "date", "datetime-local", "month", "week", "time",
            "hidden", "checkbox", "radio", "range", "color", "file", "button", "submit", "reset", "image"};
        // 표준 종류 목록에서 찾는다.
        for (int i = 0; i < known.length; i++) {
            if (known[i].equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether an element is an HTML element with one of the given lower-case names.
     * <p>
     * 요소가 주어진 소문자 이름 중 하나인 HTML 요소인지 알려 준다.
     *
     * @param e     element / 요소
     * @param names names / 이름들
     * @return true if it is / 그렇다면 true
     */
    static boolean isHtmlNamed(Element e, String... names) {
        if (!isHtml(e)) {
            return false;
        }
        String n = Namespaces.asciiLower(e.getLocalName());
        // 이름 목록에서 찾는다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements <code>:link</code> and <code>:any-link</code>.
     * <p>
     * <code>:link</code> 와 <code>:any-link</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true for hyperlinks / 하이퍼링크이면 true
     */
    static boolean isLink(Element e) {
        if (isHtmlNamed(e, "a", "area", "link")) {
            return e.getAttribute("href") != null;
        }
        if (Namespaces.SVG.equals(e.getNamespaceURI()) && e.getLocalName().equals("a")) {
            return e.getAttribute("href") != null || e.getAttributeNS(Namespaces.XLINK, "href") != null;
        }
        return false;
    }

    /**
     * Tells whether an element is a form control that can be disabled or enabled.
     * <p>
     * 요소가 disabled 나 enabled 가 될 수 있는 폼 컨트롤인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for such controls / 그런 컨트롤이면 true
     */
    private static boolean canBeDisabled(Element e) {
        return isHtmlNamed(e, "button", "input", "select", "textarea", "fieldset", "optgroup", "option");
    }

    /**
     * Implements <code>:disabled</code>.
     * <p>
     * <code>:disabled</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if disabled / disabled 이면 true
     */
    static boolean isDisabled(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.disabled(e);
        if (live != null) {
            return live.booleanValue();
        }
        if (!canBeDisabled(e)) {
            return false;
        }
        if (e.getAttribute("disabled") != null) {
            return true;
        }
        if (isHtmlNamed(e, "option")) {
            Element p = e.getParentElement();
            return p != null && isHtmlNamed(p, "optgroup") && p.getAttribute("disabled") != null;
        }
        if (isHtmlNamed(e, "optgroup")) {
            return false;
        }
        // 조상 fieldset 중 disabled 인 것이 있고 e 가 그 첫 legend 안이 아니면 disabled 이다.
        for (Element a = e.getParentElement(); a != null; a = a.getParentElement()) {
            if (isHtmlNamed(a, "fieldset") && a.getAttribute("disabled") != null) {
                Element legend = firstChildNamed(a, "legend");
                if (legend == null || !legend.contains(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the first element child with a name.
     * <p>
     * 이름이 맞는 첫 요소 자식을 반환한다.
     *
     * @param parent parent element / 부모 요소
     * @param name   lower-case name / 소문자 이름
     * @return the child or null / 자식 또는 null
     */
    private static Element firstChildNamed(Element parent, String name) {
        // 요소 자식을 앞에서부터 보며 이름이 맞는 첫 번째를 찾는다.
        for (Element c = parent.getFirstElementChild(); c != null; c = c.getNextElementSibling()) {
            if (isHtmlNamed(c, name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Implements <code>:enabled</code>.
     * <p>
     * <code>:enabled</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if it can be disabled but is not / 비활성이 될 수 있지만 아니면 true
     */
    static boolean isEnabled(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.enabled(e);
        if (live != null) {
            return live.booleanValue();
        }
        return canBeDisabled(e) && !isDisabled(e);
    }

    /**
     * Implements <code>:checked</code> from the attributes (<code>checked</code>, <code>selected</code>).
     * <p>
     * 속성(<code>checked</code>, <code>selected</code>)으로 <code>:checked</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if checked or selected / checked 나 selected 이면 true
     */
    static boolean isChecked(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.checked(e);
        if (live != null) {
            return live.booleanValue();
        }
        if (isHtmlNamed(e, "input")) {
            String t = inputType(e);
            return (t.equals("checkbox") || t.equals("radio")) && e.getAttribute("checked") != null;
        }
        return isHtmlNamed(e, "option") && e.getAttribute("selected") != null;
    }

    /**
     * Implements <code>:indeterminate</code> from the attributes.
     * <p>
     * 속성으로 <code>:indeterminate</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true for a progress bar without a value / 값이 없는 progress 이면 true
     */
    static boolean isIndeterminate(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.indeterminate(e);
        if (live != null) {
            return live.booleanValue();
        }
        return isHtmlNamed(e, "progress") && e.getAttribute("value") == null;
    }

    /**
     * Tells whether an element is a submit button.
     * <p>
     * 요소가 제출 버튼인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for submit buttons / 제출 버튼이면 true
     */
    private static boolean isSubmitButton(Element e) {
        if (isHtmlNamed(e, "button")) {
            String t = e.getAttribute("type");
            return t == null || Namespaces.asciiLower(t.trim()).equals("submit") || !(Namespaces.asciiLower(t.trim()).equals("reset")
                    || Namespaces.asciiLower(t.trim()).equals("button"));
        }
        if (isHtmlNamed(e, "input")) {
            String t = inputType(e);
            return t.equals("submit") || t.equals("image");
        }
        return false;
    }

    /**
     * Implements <code>:default</code>: a checked box or selected option, or the first submit button of a form.
     * <p>
     * <code>:default</code> 를 구현한다: 체크된 상자, 선택된 option, 폼의 첫 제출 버튼.
     *
     * @param e element / 요소
     * @return true for default controls / 기본 컨트롤이면 true
     */
    static boolean isDefault(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.isDefault(e);
        if (live != null) {
            return live.booleanValue();
        }
        if (isChecked(e)) {
            return true;
        }
        if (!isSubmitButton(e)) {
            return false;
        }
        Element form = e.getParentElement();
        // 가장 가까운 form 조상을 찾는다.
        while (form != null && !isHtmlNamed(form, "form")) {
            form = form.getParentElement();
        }
        if (form == null) {
            return false;
        }
        // 폼 안을 트리 순서로 훑어 첫 제출 버튼이 e 인지 본다.
        for (Node n = Node.nextInTree(form, form); n != null; n = Node.nextInTree(n, form)) {
            if (n instanceof Element && isSubmitButton((Element) n)) {
                return n == e;
            }
        }
        return false;
    }

    /**
     * Tells whether an element is a text-entry control for which <code>required</code> is meaningful.
     * <p>
     * 요소가 <code>required</code> 가 의미 있는 입력 컨트롤인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for such controls / 그런 컨트롤이면 true
     */
    private static boolean canBeRequired(Element e) {
        if (isHtmlNamed(e, "select", "textarea")) {
            return true;
        }
        if (isHtmlNamed(e, "input")) {
            String t = inputType(e);
            return !(t.equals("hidden") || t.equals("range") || t.equals("color") || t.equals("button") || t.equals("submit")
                    || t.equals("reset") || t.equals("image"));
        }
        return false;
    }

    /**
     * Implements <code>:required</code>.
     * <p>
     * <code>:required</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if required / required 이면 true
     */
    static boolean isRequired(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.required(e);
        if (live != null) {
            return live.booleanValue();
        }
        return canBeRequired(e) && e.getAttribute("required") != null;
    }

    /**
     * Implements <code>:optional</code>.
     * <p>
     * <code>:optional</code> 을 구현한다.
     *
     * @param e element / 요소
     * @return true if it may be required but is not / required 가 될 수 있지만 아니면 true
     */
    static boolean isOptional(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.optional(e);
        if (live != null) {
            return live.booleanValue();
        }
        return canBeRequired(e) && e.getAttribute("required") == null;
    }

    /**
     * Implements <code>:read-write</code>.
     * <p>
     * <code>:read-write</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true for editable elements / 편집할 수 있는 요소이면 true
     */
    static boolean isReadWrite(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.readWrite(e);
        if (live != null) {
            return live.booleanValue();
        }
        if (isHtmlNamed(e, "textarea")) {
            return e.getAttribute("readonly") == null && e.getAttribute("disabled") == null;
        }
        if (isHtmlNamed(e, "input")) {
            return isTextLike(inputType(e)) && e.getAttribute("readonly") == null && !isDisabled(e);
        }
        // contenteditable 은 가장 가까운 속성 값을 상속한다.
        for (Element a = e; a != null; a = a.getParentElement()) {
            String ce = a.getAttribute("contenteditable");
            if (ce != null) {
                String v = Namespaces.asciiLower(ce.trim());
                return v.length() == 0 || v.equals("true") || v.equals("plaintext-only");
            }
        }
        return false;
    }

    /**
     * Returns the current text of a textarea (its text content).
     * <p>
     * textarea 의 현재 글(텍스트 내용)을 반환한다.
     *
     * @param e element / 요소
     * @return text / 글
     */
    private static String textareaValue(Element e) {
        String s = e.getTextContent();
        return s == null ? "" : s;
    }

    /**
     * Implements <code>:placeholder-shown</code> from the attributes.
     * <p>
     * 속성으로 <code>:placeholder-shown</code> 을 구현한다.
     *
     * @param e element / 요소
     * @return true if a placeholder is displayed / placeholder 가 표시되면 true
     */
    static boolean isPlaceholderShown(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.placeholderShown(e);
        if (live != null) {
            return live.booleanValue();
        }
        String ph = e.getAttribute("placeholder");
        if (ph == null || ph.length() == 0) {
            return false;
        }
        if (isHtmlNamed(e, "textarea")) {
            return textareaValue(e).length() == 0;
        }
        if (isHtmlNamed(e, "input")) {
            String v = e.getAttribute("value");
            return isTextLike(inputType(e)) && (v == null || v.length() == 0);
        }
        return false;
    }

    /**
     * Tells whether a control fails the constraint check that the attributes alone can decide (a required control without a value).
     * <p>
     * 컨트롤이 속성만으로 판정할 수 있는 제약 검사에 실패하는지(값이 없는 required 컨트롤) 알려 준다.
     *
     * @param e control / 컨트롤
     * @return true if invalid / 유효하지 않으면 true
     */
    private static boolean controlInvalid(Element e) {
        if (!isRequired(e) || isDisabled(e)) {
            return false;
        }
        if (isHtmlNamed(e, "textarea")) {
            return textareaValue(e).length() == 0;
        }
        if (isHtmlNamed(e, "input")) {
            String t = inputType(e);
            if (t.equals("checkbox") || t.equals("radio")) {
                return e.getAttribute("checked") == null;
            }
            String v = e.getAttribute("value");
            return v == null || v.length() == 0;
        }
        return false;
    }

    /**
     * Tells whether an element takes part in constraint validation.
     * <p>
     * 요소가 제약 검증에 참여하는지 알려 준다.
     *
     * @param e element / 요소
     * @return true for validatable elements / 검증 대상이면 true
     */
    private static boolean isValidatable(Element e) {
        if (isHtmlNamed(e, "form", "fieldset", "select", "textarea")) {
            return true;
        }
        if (isHtmlNamed(e, "input")) {
            String t = inputType(e);
            return !(t.equals("hidden") || t.equals("button") || t.equals("reset") || t.equals("image"));
        }
        return false;
    }

    /**
     * Implements <code>:invalid</code> and (negated) <code>:valid</code> from the attributes.
     * <p>
     * 속성으로 <code>:invalid</code> 와 (부정하여) <code>:valid</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if the element is invalid / 요소가 유효하지 않으면 true
     */
    static boolean isInvalid(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.invalid(e);
        if (live != null) {
            return live.booleanValue();
        }
        if (!isValidatable(e)) {
            return false;
        }
        if (isHtmlNamed(e, "form", "fieldset")) {
            // 폼이나 fieldset 은 안의 컨트롤 중 유효하지 않은 것이 있으면 유효하지 않다.
            for (Node n = Node.nextInTree(e, e); n != null; n = Node.nextInTree(n, e)) {
                if (n instanceof Element && controlInvalid((Element) n)) {
                    return true;
                }
            }
            return false;
        }
        return controlInvalid(e);
    }

    /**
     * Implements <code>:valid</code>.
     * <p>
     * <code>:valid</code> 를 구현한다.
     *
     * @param e element / 요소
     * @return true if validatable and valid / 검증 대상이며 유효하면 true
     */
    static boolean isValid(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.valid(e);
        if (live != null) {
            return live.booleanValue();
        }
        return isValidatable(e) && !isInvalid(e);
    }

    /**
     * Tells whether an element matches <code>:in-range</code> by the live state of the form layer; elements that the form layer does not implement
     * never match.
     * <p>
     * 요소가 폼 계층의 실제 상태로 <code>:in-range</code> 에 일치하는지 알려 준다. 폼 계층이 구현하지 않는 요소는 일치하지 않는다.
     *
     * @param e element / 요소
     * @return true if in range / 범위 안이면 true
     */
    static boolean isInRange(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.inRange(e);
        return live != null && live.booleanValue();
    }

    /**
     * Tells whether an element matches <code>:out-of-range</code> by the live state of the form layer; elements that the form layer does not
     * implement never match.
     * <p>
     * 요소가 폼 계층의 실제 상태로 <code>:out-of-range</code> 에 일치하는지 알려 준다. 폼 계층이 구현하지 않는 요소는 일치하지 않는다.
     *
     * @param e element / 요소
     * @return true if out of range / 범위를 벗어났으면 true
     */
    static boolean isOutOfRange(Element e) {
        Boolean live = org.duckdns.hjow.swingwebpanel.dom.html.FormStates.outOfRange(e);
        return live != null && live.booleanValue();
    }

    /**
     * Implements <code>:open</code>.
     * <p>
     * <code>:open</code> 을 구현한다.
     *
     * @param e element / 요소
     * @return true for open details and dialogs / 열린 details 와 dialog 이면 true
     */
    static boolean isOpen(Element e) {
        return isHtmlNamed(e, "details", "dialog") && e.getAttribute("open") != null;
    }

    /**
     * Implements <code>:modal</code> (HTML Standard): an open <code>dialog</code> that is in the top layer of its document (opened with <code>showModal()</code>).
     * <p>
     * <code>:modal</code> 을 구현한다(HTML 표준): 문서의 최상위 계층에 있는 열린 <code>dialog</code>(<code>showModal()</code> 로 연 것).
     *
     * @param e element / 요소
     * @return true for modal dialogs / 모달 대화상자이면 true
     */
    static boolean isModal(Element e) {
        return isHtmlNamed(e, "dialog") && e.getAttribute("open") != null && e.getOwnerDocument() != null && e.getOwnerDocument().isInTopLayer(e);
    }

    /**
     * Implements <code>:defined</code>: custom element names (with a hyphen) are not defined because no custom element registry exists.
     * <p>
     * <code>:defined</code> 를 구현한다: 사용자 정의 요소 등록부가 없으므로 사용자 정의 요소 이름(하이픈 포함)은 정의되지 않은 것이다.
     *
     * @param e element / 요소
     * @return true for defined elements / 정의된 요소이면 true
     */
    static boolean isDefined(Element e) {
        return !isHtml(e) || e.getLocalName().indexOf('-') < 0;
    }

    /**
     * Implements <code>:target</code> from the fragment of the document URL.
     * <p>
     * 문서 URL 의 프래그먼트로 <code>:target</code> 을 구현한다.
     *
     * @param e element / 요소
     * @return true if the element is the fragment target / 프래그먼트 대상이면 true
     */
    static boolean isTarget(Element e) {
        Document d = e.getOwnerDocument();
        String url = d == null ? null : d.getUrl();
        int h = url == null ? -1 : url.indexOf('#');
        if (h < 0 || h == url.length() - 1) {
            return false;
        }
        String frag = url.substring(h + 1);
        return frag.equals(e.getId());
    }

    /**
     * Returns the language of an element: the nearest <code>lang</code> (or <code>xml:lang</code>) attribute of the element and its ancestors.
     * <p>
     * 요소의 언어를 반환한다: 요소와 조상의 가장 가까운 <code>lang</code>(또는 <code>xml:lang</code>) 속성.
     *
     * @param e element / 요소
     * @return the language tag (possibly empty), or null if none is declared / 언어 태그(빈 문자열일 수 있음). 선언이 없으면 null
     */
    static String language(Element e) {
        // 요소에서 위로 올라가며 lang 속성을 찾는다.
        for (Element a = e; a != null; a = a.getParentElement()) {
            String l = a.getAttributeNS(null, "lang");
            if (l == null) {
                l = a.getAttributeNS(Namespaces.XML, "lang");
            }
            if (l != null) {
                return l;
            }
        }
        return null;
    }

    /**
     * Implements <code>:lang()</code> with the extended filtering of RFC 4647.
     * <p>
     * RFC 4647 의 확장 필터링으로 <code>:lang()</code> 을 구현한다.
     *
     * @param e      element / 요소
     * @param ranges the language ranges of the selector / 선택자의 언어 범위
     * @return true if any range matches the language of the element / 범위 중 하나가 요소의 언어와 일치하면 true
     */
    static boolean matchesLang(Element e, List<String> ranges) {
        String lang = language(e);
        if (lang == null) {
            return false;
        }
        // 범위마다 확장 필터링을 적용해 하나라도 일치하면 true 이다.
        for (int i = 0; i < ranges.size(); i++) {
            if (extendedFilter(ranges.get(i), lang)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies the extended filtering algorithm of RFC 4647 section 3.3.2.
     * <p>
     * RFC 4647 3.3.2 절의 확장 필터링 알고리즘을 적용한다.
     *
     * @param range language range / 언어 범위
     * @param lang  language tag / 언어 태그
     * @return true if the range matches the tag / 범위가 태그와 일치하면 true
     */
    static boolean extendedFilter(String range, String lang) {
        if (range.length() == 0) {
            return lang.length() == 0;
        }
        if (lang.length() == 0) {
            return false;
        }
        String[] rs = split(range);
        String[] ls = split(lang);
        if (!rs[0].equals("*") && !rs[0].equalsIgnoreCase(ls[0])) {
            return false;
        }
        int li = 1;
        // 범위의 나머지 부분 태그를 언어 태그에서 순서대로 찾는다.
        for (int ri = 1; ri < rs.length; ri++) {
            if (rs[ri].equals("*")) {
                continue;
            }
            boolean found = false;
            // 언어 태그의 다음 부분 태그들을 훑으며 같은 것을 찾고 단일 문자 부분 태그에서는 멈춘다.
            while (li < ls.length) {
                String s = ls[li];
                if (s.equalsIgnoreCase(rs[ri])) {
                    li++;
                    found = true;
                    break;
                }
                if (s.length() == 1) {
                    return false;
                }
                li++;
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a language tag at hyphens.
     * <p>
     * 언어 태그를 하이픈에서 나눈다.
     *
     * @param s tag / 태그
     * @return subtags / 부분 태그
     */
    private static String[] split(String s) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        // 하이픈마다 부분 태그를 잘라 낸다.
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '-') {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return parts.toArray(new String[parts.size()]);
    }

    /**
     * Implements <code>:dir()</code>: the nearest <code>dir</code> attribute, <code>auto</code> being decided by the first strong character,
     * left-to-right by default.
     * <p>
     * <code>:dir()</code> 을 구현한다: 가장 가까운 <code>dir</code> 속성. <code>auto</code> 는 첫 강한 문자로 정하고 기본은 왼쪽에서 오른쪽이다.
     *
     * @param e element / 요소
     * @return "ltr" or "rtl" / "ltr" 또는 "rtl"
     */
    static String direction(Element e) {
        // 요소에서 위로 올라가며 dir 속성을 찾는다.
        for (Element a = e; a != null; a = a.getParentElement()) {
            String d = a.getAttribute("dir");
            if (d != null) {
                String v = Namespaces.asciiLower(d.trim());
                if (v.equals("ltr") || v.equals("rtl")) {
                    return v;
                }
                if (v.equals("auto")) {
                    return autoDirection(a);
                }
            }
        }
        return "ltr";
    }

    /**
     * Decides the direction of an <code>auto</code> element from the first strongly directional character of its text.
     * <p>
     * <code>auto</code> 요소의 방향을 글의 첫 강한 방향 문자로 정한다.
     *
     * @param e element / 요소
     * @return "ltr" or "rtl" / "ltr" 또는 "rtl"
     */
    private static String autoDirection(Element e) {
        String text = e.getTextContent();
        // 글을 앞에서부터 보며 방향성이 강한 첫 문자를 찾는다.
        for (int i = 0; text != null && i < text.length(); i++) {
            byte dir = Character.getDirectionality(text.charAt(i));
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return "rtl";
            }
            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return "ltr";
            }
        }
        return "ltr";
    }
}
