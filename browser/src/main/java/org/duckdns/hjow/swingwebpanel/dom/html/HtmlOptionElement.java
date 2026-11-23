package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Text;

/**
 * The <code>option</code> element (HTML Standard 4.10.10): one choice of a <code>select</code> or one suggestion of a
 * <code>datalist</code>. Besides its attributes it has the state "selectedness" (whether it is selected) and "dirtiness" (whether the user or a
 * script changed the selectedness, which stops the <code>selected</code> attribute from changing it). The selection rules between the options
 * of a single-choice <code>select</code> belong to {@link HtmlSelectElement}.
 * <p>
 * <code>option</code> 요소(HTML 표준 4.10.10): <code>select</code> 의 선택지 하나 또는 <code>datalist</code> 의 제안 하나. 속성 외에 "selectedness"(선택되었는지)와
 * "dirtiness"(사용자나 스크립트가 selectedness 를 바꿨는지. 바꿨으면 <code>selected</code> 속성이 더는 바꾸지 못한다) 상태를 가진다. 단일 선택
 * <code>select</code> 의 option 사이의 선택 규칙은 {@link HtmlSelectElement} 가 맡는다.
 */
public final class HtmlOptionElement extends HtmlControl {

    /** The selectedness of the option (whether it is selected). / option 의 selectedness(선택되어 있는지). */
    private boolean selectedness;

    /** The dirtiness: set once the selectedness was changed by script or the user. / dirtiness: 스크립트나 사용자가 selectedness 를 바꾸면 켜진다. */
    private boolean dirtiness;

    /**
     * Creates an option.
     * <p>
     * option 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlOptionElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Copies the selectedness and the dirtiness to a clone.
     * <p>
     * selectedness 와 dirtiness 를 복제본에 복사한다.
     *
     * @param copy the clone / 복제본
     */
    protected void copyStateTo(Element copy) {
        super.copyStateTo(copy);
        HtmlOptionElement o = (HtmlOptionElement) copy;
        o.selectedness = selectedness;
        o.dirtiness = dirtiness;
    }

    /**
     * Reacts to the <code>selected</code> attribute (it sets the selectedness unless the option is dirty) and to the attributes that
     * decide which option a single-choice select picks.
     * <p>
     * <code>selected</code> 속성(dirty 가 아닐 때 selectedness 를 바꾼다)과 단일 선택 select 가 고르는 option 을 정하는 속성에 반응한다.
     *
     * @param local     attribute local name / 속성 로컬 이름
     * @param namespace attribute namespace / 속성 네임스페이스
     * @param oldValue  previous value or null / 이전 값 또는 null
     * @param newValue  new value or null / 새 값 또는 null
     */
    protected void attributeChanged(String local, String namespace, String oldValue, String newValue) {
        if (namespace != null) {
            return;
        }
        if ("selected".equals(local) && !dirtiness) {
            applySelectedness(newValue != null);
        } else if ("disabled".equals(local)) {
            HtmlSelectElement s = getSelect();
            if (s != null) {
                s.selectednessMayChange();
            }
        }
    }

    /**
     * Sets the selectedness and lets the select apply its rules.
     * <p>
     * selectedness 를 지정하고 select 가 규칙을 적용하게 한다.
     *
     * @param value the new selectedness / 새 selectedness
     */
    void applySelectedness(boolean value) {
        selectedness = value;
        HtmlSelectElement s = getSelect();
        if (s != null) {
            s.optionSelectednessSet(this);
        }
        stateChanged();
    }

    /**
     * Returns the select that owns this option: the parent <code>select</code>, or the <code>select</code> that is the parent of the parent
     * <code>optgroup</code>.
     * <p>
     * 이 option 을 가진 select 를 반환한다: 부모 <code>select</code>, 또는 부모 <code>optgroup</code> 의 부모인 <code>select</code>.
     *
     * @return the select or null / select 또는 null
     */
    HtmlSelectElement getSelect() {
        Node p = getParentNode();
        if (p instanceof HtmlSelectElement) {
            return (HtmlSelectElement) p;
        }
        if (p instanceof HtmlOptGroupElement && p.getParentNode() instanceof HtmlSelectElement) {
            return (HtmlSelectElement) p.getParentNode();
        }
        return null;
    }

    /**
     * Returns the raw selectedness without asking the select to apply pending rules.
     * <p>
     * select 가 보류 중인 규칙을 적용하도록 묻지 않고 원래 selectedness 를 반환한다.
     *
     * @return the stored selectedness / 저장된 selectedness
     */
    boolean rawSelected() {
        return selectedness;
    }

    /**
     * Sets the raw selectedness without any rule (used by the select).
     * <p>
     * 규칙 없이 원래 selectedness 를 지정한다(select 가 쓴다).
     *
     * @param value the selectedness / selectedness
     */
    void setRawSelected(boolean value) {
        selectedness = value;
    }

    /**
     * Sets the dirtiness (used by the select).
     * <p>
     * dirtiness 를 지정한다(select 가 쓴다).
     *
     * @param value the dirtiness / dirtiness
     */
    void setDirty(boolean value) {
        dirtiness = value;
    }

    /**
     * Returns the <code>selected</code> IDL attribute: the selectedness.
     * <p>
     * <code>selected</code> IDL 속성을 반환한다: selectedness.
     *
     * @return true if selected / 선택되었으면 true
     */
    public boolean isSelected() {
        HtmlSelectElement s = getSelect();
        if (s != null) {
            s.ensureConsistent();
        }
        return selectedness;
    }

    /**
     * Sets <code>selected</code>: the selectedness changes, the option becomes dirty and the select applies its rules ("ask for a reset").
     * <p>
     * <code>selected</code> 를 지정한다: selectedness 가 바뀌고 option 이 dirty 가 되며 select 가 규칙을 적용한다("ask for a reset").
     *
     * @param value the new selectedness / 새 selectedness
     */
    public void setSelected(boolean value) {
        dirtiness = true;
        applySelectedness(value);
    }

    /**
     * Returns the <code>defaultSelected</code> IDL attribute: whether the <code>selected</code> attribute is present.
     * <p>
     * <code>defaultSelected</code> IDL 속성을 반환한다: <code>selected</code> 속성이 있는지.
     *
     * @return true if the attribute exists / 속성이 있으면 true
     */
    public boolean isDefaultSelected() {
        return hasAttribute("selected");
    }

    /**
     * Sets or removes the <code>selected</code> attribute.
     * <p>
     * <code>selected</code> 속성을 지정하거나 제거한다.
     *
     * @param value true to add the attribute / 속성을 추가하려면 true
     */
    public void setDefaultSelected(boolean value) {
        if (value) {
            setAttribute("selected", "");
        } else {
            removeAttribute("selected");
        }
    }

    /**
     * Tells whether the option is disabled: its own <code>disabled</code> attribute or a disabled parent <code>optgroup</code>.
     * <p>
     * option 이 비활성인지 알려 준다: 자신의 <code>disabled</code> 속성 또는 비활성인 부모 <code>optgroup</code>.
     *
     * @return true if disabled / 비활성이면 true
     */
    public boolean isDisabled() {
        if (hasAttribute("disabled")) {
            return true;
        }
        Node p = getParentNode();
        return p instanceof HtmlOptGroupElement && ((HtmlOptGroupElement) p).isDisabled();
    }

    /**
     * Returns the <code>text</code> IDL attribute: the text of the option without the text of <code>script</code> descendants, with ASCII
     * white space collapsed and stripped.
     * <p>
     * <code>text</code> IDL 속성을 반환한다: <code>script</code> 후손의 글을 뺀 option 의 글을 ASCII 공백을 줄이고 앞뒤를 제거해 반환한다.
     *
     * @return the text / 글
     */
    public String getText() {
        StringBuilder b = new StringBuilder();
        collectText(this, b);
        return collapse(b.toString());
    }

    /**
     * Collects the text nodes below an element, skipping script elements.
     * <p>
     * 요소 아래의 텍스트 노드를 모은다. script 요소는 건너뛴다.
     *
     * @param parent the element / 요소
     * @param out    receives the text / 글을 받는다
     */
    private static void collectText(Node parent, StringBuilder out) {
        // 자식을 차례로 보며 텍스트는 모으고 script 가 아닌 요소는 안으로 들어간다.
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Text) {
                out.append(((Text) c).getData());
            } else if (c instanceof Element && !"script".equals(((Element) c).getLocalName())) {
                collectText(c, out);
            }
        }
    }

    /**
     * Strips and collapses ASCII white space.
     * <p>
     * ASCII 공백의 앞뒤를 제거하고 연속한 공백을 하나로 줄인다.
     *
     * @param s the text / 글
     * @return the collapsed text / 줄인 글
     */
    static String collapse(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean space = false;
        // 공백이 아닌 문자 사이의 공백 연속을 공백 하나로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (HtmlText.isSpace(c)) {
                space = b.length() > 0;
            } else {
                if (space) {
                    b.append(' ');
                    space = false;
                }
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Sets the text content.
     * <p>
     * 텍스트 내용을 지정한다.
     *
     * @param value the new text / 새 글
     */
    public void setText(String value) {
        setTextContent(value);
    }

    /**
     * Returns the <code>value</code> IDL attribute: the <code>value</code> attribute, or the text if there is none.
     * <p>
     * <code>value</code> IDL 속성을 반환한다: <code>value</code> 속성, 없으면 글.
     *
     * @return the value / 값
     */
    public String getValue() {
        String v = getAttribute("value");
        return v != null ? v : getText();
    }

    /**
     * Sets the <code>value</code> attribute.
     * <p>
     * <code>value</code> 속성을 지정한다.
     *
     * @param value the new value / 새 값
     */
    public void setValue(String value) {
        setAttribute("value", value);
    }

    /**
     * Returns the <code>label</code> IDL attribute: the <code>label</code> attribute, or the text if there is none.
     * <p>
     * <code>label</code> IDL 속성을 반환한다: <code>label</code> 속성, 없으면 글.
     *
     * @return the label / 라벨
     */
    public String getLabel() {
        String v = getAttribute("label");
        return v != null ? v : getText();
    }

    /**
     * Sets the <code>label</code> attribute.
     * <p>
     * <code>label</code> 속성을 지정한다.
     *
     * @param value the new label / 새 라벨
     */
    public void setLabel(String value) {
        setAttribute("label", value);
    }

    /**
     * Returns the <code>index</code> IDL attribute: the position in the options list of its select (or of its datalist), otherwise 0.
     * <p>
     * <code>index</code> IDL 속성을 반환한다: 소속 select(또는 datalist)의 option 목록에서의 위치. 없으면 0.
     *
     * @return the index / 위치
     */
    public int getIndex() {
        HtmlSelectElement s = getSelect();
        List<HtmlOptionElement> options;
        if (s != null) {
            options = s.optionList();
        } else {
            Node p = getParentNode();
            if (!(p instanceof HtmlDataListElement)) {
                return 0;
            }
            // datalist 의 option 후손 중 이 option 의 위치를 찾는다.
            int i = 0;
            List<Element> l = ((HtmlDataListElement) p).getOptions().toList();
            // 목록을 앞에서부터 보며 자신이 나오는 위치를 찾는다.
            for (; i < l.size(); i++) {
                if (l.get(i) == this) {
                    return i;
                }
            }
            return 0;
        }
        int at = options.indexOf(this);
        return at < 0 ? 0 : at;
    }

    /**
     * Returns the form owner of the select that contains the option.
     * <p>
     * option 을 가진 select 의 폼 소유자를 반환한다.
     *
     * @return the form or null / 폼 또는 null
     */
    public HtmlFormElement getForm() {
        HtmlSelectElement s = getSelect();
        return s == null ? null : s.getForm();
    }
}
