package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * Read-only helpers that tell the renderer and the layout what a form control shows: which kind of control an element is, the label of a
 * button, the text of a text field (the placeholder when it is empty), the labels of the options of a <code>select</code> element. They only
 * read the live state of the controls, so they must be called on the document thread (or on a thread that owns the document).
 * <p>
 * 렌더러와 레이아웃에 폼 컨트롤이 무엇을 보여 주는지 알려 주는 읽기 전용 도우미: 요소가 어떤 종류의 컨트롤인지, 버튼의 라벨, 텍스트 필드의 글(비어 있으면
 * placeholder), <code>select</code> 요소의 option 라벨. 컨트롤의 살아 있는 상태만 읽으므로 문서 스레드(또는 문서를 소유한 스레드)에서 불러야 한다.
 */
public final class ControlDisplay {

    /** The kind of a <code>textarea</code> element. / <code>textarea</code> 요소의 종류. */
    public static final String TEXTAREA = "textarea";

    /** The kind of a <code>select</code> element. / <code>select</code> 요소의 종류. */
    public static final String SELECT = "select";

    /** The kind of a <code>progress</code> element. / <code>progress</code> 요소의 종류. */
    public static final String PROGRESS = "progress";

    /** The kind of a <code>meter</code> element. / <code>meter</code> 요소의 종류. */
    public static final String METER = "meter";

    /** The text that a password field shows for every character (a bullet). / 암호 필드가 글자마다 보여 주는 글(검은 점). */
    private static final char BULLET = (char) 0x2022;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ControlDisplay() {
    }

    /**
     * Returns the kind of control of an element: the (normalized) <code>type</code> of an <code>input</code> element
     * (<code>text</code>, <code>checkbox</code>, <code>submit</code> ...), or {@link #TEXTAREA}, {@link #SELECT}, {@link #PROGRESS},
     * {@link #METER}. Other elements, including <code>button</code>, give null.
     * <p>
     * 요소의 컨트롤 종류를 반환한다: <code>input</code> 요소의 (정규화된) <code>type</code>(<code>text</code>, <code>checkbox</code>,
     * <code>submit</code> 등), 또는 {@link #TEXTAREA}, {@link #SELECT}, {@link #PROGRESS}, {@link #METER}. <code>button</code> 을 비롯한 다른 요소는 null.
     *
     * @param e the element, may be null / 요소. null 가능
     * @return the kind or null / 종류 또는 null
     */
    public static String kind(Element e) {
        if (e == null || !Namespaces.HTML.equals(e.getNamespaceURI())) {
            return null;
        }
        if (e instanceof HtmlInputElement) {
            return ((HtmlInputElement) e).getType();
        }
        if (e instanceof HtmlTextAreaElement) {
            return TEXTAREA;
        }
        if (e instanceof HtmlSelectElement) {
            return SELECT;
        }
        if (e instanceof HtmlProgressElement) {
            return PROGRESS;
        }
        return e instanceof HtmlMeterElement ? METER : null;
    }

    /**
     * Tells whether an input type is shown as a one-line text field (the text types, the number and date types and the others that the user
     * edits as text).
     * <p>
     * input 종류가 한 줄 텍스트 필드로 보이는지 알려 준다(텍스트 종류, 숫자와 날짜 종류, 글로 편집하는 나머지).
     *
     * @param kind the kind from {@link #kind(Element)} / {@link #kind(Element)} 의 종류
     * @return true for a text field / 텍스트 필드이면 true
     */
    public static boolean isTextField(String kind) {
        return "text".equals(kind) || "search".equals(kind) || "tel".equals(kind) || "url".equals(kind) || "email".equals(kind)
                || "password".equals(kind) || "number".equals(kind) || "date".equals(kind) || "month".equals(kind) || "week".equals(kind)
                || "time".equals(kind) || "datetime-local".equals(kind);
    }

    /**
     * Tells whether an input type is shown as a push button (<code>button</code>, <code>submit</code>, <code>reset</code>).
     * <p>
     * input 종류가 누름 단추로 보이는지 알려 준다(<code>button</code>, <code>submit</code>, <code>reset</code>).
     *
     * @param kind the kind / 종류
     * @return true for a push button / 누름 단추이면 true
     */
    public static boolean isPushButton(String kind) {
        return "button".equals(kind) || "submit".equals(kind) || "reset".equals(kind);
    }

    /**
     * Tells whether an element is a list box (a <code>select</code> element with the <code>multiple</code> attribute or a display size above 1).
     * <p>
     * 요소가 리스트 박스(<code>multiple</code> 속성이 있거나 표시 크기가 1 을 넘는 <code>select</code> 요소)인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a list box / 리스트 박스이면 true
     */
    public static boolean isListBox(Element e) {
        return e instanceof HtmlSelectElement && (((HtmlSelectElement) e).isMultiple() || ((HtmlSelectElement) e).getDisplaySize() > 1);
    }

    /**
     * Returns the label of a button-like <code>input</code>: its <code>value</code> attribute, or the default label of the type
     * (<code>Submit</code>, <code>Reset</code>), or the empty string. An image button shows its <code>alt</code> text.
     * <p>
     * 단추처럼 보이는 <code>input</code> 의 라벨을 반환한다: <code>value</code> 속성, 없으면 종류의 기본 라벨(<code>Submit</code>, <code>Reset</code>),
     * 그것도 없으면 빈 문자열. 이미지 단추는 <code>alt</code> 글을 보여 준다.
     *
     * @param input the input element / input 요소
     * @return the label / 라벨
     */
    public static String buttonLabel(Element input) {
        String kind = kind(input);
        String value = input.getAttribute("value");
        if ("image".equals(kind)) {
            String alt = input.getAttribute("alt");
            return alt != null && alt.length() > 0 ? alt : (value != null && value.length() > 0 ? value : "Submit");
        }
        if (value != null && value.length() > 0) {
            return value;
        }
        if ("submit".equals(kind)) {
            return "Submit";
        }
        return "reset".equals(kind) ? "Reset" : "";
    }

    /**
     * Returns the text that a text field or a textarea shows for its value: the value, with every character of a password replaced by a
     * bullet. The placeholder is not included; see {@link #placeholder(Element)}.
     * <p>
     * 텍스트 필드나 textarea 가 값으로 보여 주는 글을 반환한다: 값이며 암호는 글자마다 검은 점으로 바꾼다. placeholder 는 포함하지 않는다.
     * {@link #placeholder(Element)} 를 참조.
     *
     * @param e the control / 컨트롤
     * @return the text, empty if none / 글. 없으면 빈 문자열
     */
    public static String fieldText(Element e) {
        if (e instanceof HtmlTextAreaElement) {
            return ((HtmlTextAreaElement) e).getValue();
        }
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            String v = in.getValue();
            if ("password".equals(in.getType())) {
                StringBuilder b = new StringBuilder(v.length());
                // 암호의 코드 포인트마다 검은 점 하나를 만든다.
                for (int i = 0; i < v.length(); i = v.offsetByCodePoints(i, 1)) {
                    b.append(BULLET);
                }
                return b.toString();
            }
            return v;
        }
        return "";
    }

    /**
     * Returns the placeholder that a text field or textarea shows while it is empty, or the empty string if it shows none.
     * <p>
     * 텍스트 필드나 textarea 가 비어 있을 때 보여 주는 placeholder 를 반환한다. 없으면 빈 문자열.
     *
     * @param e the control / 컨트롤
     * @return the placeholder / placeholder
     */
    public static String placeholder(Element e) {
        boolean shown = e instanceof HtmlInputElement ? ((HtmlInputElement) e).isPlaceholderShown()
                : e instanceof HtmlTextAreaElement && ((HtmlTextAreaElement) e).isPlaceholderShown();
        String p = shown ? e.getAttribute("placeholder") : null;
        return p == null ? "" : p;
    }

    /**
     * Returns a sample of the text that the control may show, for choosing a font that can display it: the value, the placeholder, the label of a
     * button, the option labels of a select element.
     * <p>
     * 컨트롤이 보여 줄 수 있는 글의 표본을 반환한다(그 글을 표시할 수 있는 글꼴을 고르는 데 쓴다): 값, placeholder, 단추의 라벨, select 의 option 라벨.
     *
     * @param e the control / 컨트롤
     * @return the sample, may be empty / 표본. 비어 있을 수 있다
     */
    public static String sample(Element e) {
        String kind = kind(e);
        if (kind == null) {
            return "";
        }
        if (isPushButton(kind) || "image".equals(kind)) {
            return buttonLabel(e);
        }
        if (e instanceof HtmlSelectElement) {
            StringBuilder b = new StringBuilder();
            List<String> labels = optionLabels(e);
            // 표본이 너무 커지지 않도록 앞쪽 option 의 라벨만 모은다.
            for (int i = 0; i < labels.size() && b.length() < 400; i++) {
                b.append(labels.get(i));
            }
            return b.toString();
        }
        if (isTextField(kind) || TEXTAREA.equals(kind)) {
            String v = e instanceof HtmlTextAreaElement ? ((HtmlTextAreaElement) e).getValue() : ((HtmlInputElement) e).getValue();
            String p = e.getAttribute("placeholder");
            return v + (p == null ? "" : p);
        }
        return "";
    }

    /**
     * Returns the labels of the options of a select element in document order (the labels of the options inside option groups included; the
     * labels of the groups themselves are not in the list).
     * <p>
     * select 요소의 option 라벨을 문서 순서로 반환한다(optgroup 안의 option 도 포함하며 그룹 자체의 라벨은 목록에 없다).
     *
     * @param select the select element / select 요소
     * @return the labels / 라벨
     */
    public static List<String> optionLabels(Element select) {
        List<String> out = new ArrayList<String>();
        if (select instanceof HtmlSelectElement) {
            List<HtmlOptionElement> list = ((HtmlSelectElement) select).getOptionElements();
            // option 을 순서대로 보며 라벨을 모은다.
            for (int i = 0; i < list.size(); i++) {
                out.add(list.get(i).getLabel());
            }
        }
        return out;
    }

    /**
     * Returns the rows of a select element in the order in which a list shows them: an <code>optgroup</code> element (a label row) is followed
     * by its <code>option</code> children, and <code>option</code> children of the select element itself stand alone. The renderer and the host
     * use the same list, so that the row under the mouse is the same element in both.
     * <p>
     * select 요소의 행을 목록이 보여 주는 순서로 반환한다: <code>optgroup</code> 요소(라벨 행) 뒤에 그 <code>option</code> 자식이 오고, select 요소의 직접
     * <code>option</code> 자식은 따로 선다. 렌더러와 호스트가 같은 목록을 쓰므로 마우스 아래의 행이 양쪽에서 같은 요소이다.
     *
     * @param select the select element / select 요소
     * @return the <code>optgroup</code> and <code>option</code> elements / <code>optgroup</code> 과 <code>option</code> 요소
     */
    public static List<Element> rowElements(Element select) {
        List<Element> out = new ArrayList<Element>();
        // select 의 자식을 순서대로 보며 option 과 optgroup(과 그 안의 option)을 모은다.
        for (org.duckdns.hjow.swingwebpanel.dom.Node n = select.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof HtmlOptionElement) {
                out.add((Element) n);
            } else if (n instanceof HtmlOptGroupElement) {
                out.add((Element) n);
                // 그룹 안의 option 을 그룹 라벨 뒤에 모은다.
                for (org.duckdns.hjow.swingwebpanel.dom.Node g = n.getFirstChild(); g != null; g = g.getNextSibling()) {
                    if (g instanceof HtmlOptionElement) {
                        out.add((Element) g);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Tells whether a form control is disabled (by its own <code>disabled</code> attribute or by a disabled <code>fieldset</code>).
     * <p>
     * 폼 컨트롤이 비활성인지 알려 준다(자신의 <code>disabled</code> 속성이나 비활성 <code>fieldset</code> 때문에).
     *
     * @param e the element / 요소
     * @return true if disabled / 비활성이면 true
     */
    public static boolean isDisabled(Element e) {
        return e instanceof HtmlListedElement && ((HtmlListedElement) e).isDisabled();
    }

    /**
     * Tells whether the user cannot edit the text of a text field or textarea because of its <code>readonly</code> attribute.
     * <p>
     * 텍스트 필드나 textarea 가 <code>readonly</code> 속성 때문에 사용자가 글을 편집할 수 없는지 알려 준다.
     *
     * @param e the element / 요소
     * @return true if read-only / 읽기 전용이면 true
     */
    public static boolean isReadOnly(Element e) {
        String kind = kind(e);
        return (isTextField(kind) || TEXTAREA.equals(kind)) && e.hasAttribute("readonly");
    }

    /**
     * Returns the <code>maxlength</code> of a text field or textarea, or -1 if it has none.
     * <p>
     * 텍스트 필드나 textarea 의 <code>maxlength</code> 를 반환한다. 없으면 -1.
     *
     * @param e the element / 요소
     * @return the maximum length in UTF-16 code units, or -1 / 최대 길이(UTF-16 코드 단위). 없으면 -1
     */
    public static int maxLength(Element e) {
        if (e instanceof HtmlInputElement) {
            return ((HtmlInputElement) e).getMaxLength();
        }
        return e instanceof HtmlTextAreaElement ? ((HtmlTextAreaElement) e).getMaxLength() : -1;
    }

    /**
     * Returns the label of the selected option of a drop-down select element (the first selected option), or the empty string if none is
     * selected.
     * <p>
     * 드롭다운 select 요소에서 선택된 option(첫 번째 선택된 것)의 라벨을 반환한다. 선택된 것이 없으면 빈 문자열.
     *
     * @param select the select element / select 요소
     * @return the label / 라벨
     */
    public static String selectedLabel(Element select) {
        if (select instanceof HtmlSelectElement) {
            List<HtmlOptionElement> list = ((HtmlSelectElement) select).getOptionElements();
            // 선택된 첫 option 을 찾는다.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).isSelected()) {
                    return list.get(i).getLabel();
                }
            }
        }
        return "";
    }
}
