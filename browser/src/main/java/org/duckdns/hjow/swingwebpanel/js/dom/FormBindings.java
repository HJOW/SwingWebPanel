package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Connects the HTML form elements of {@link org.duckdns.hjow.swingwebpanel.dom.html} to JavaScript (plan items P11-05 to P11-07 and P15-02): the interfaces
 * <code>HTMLFormElement</code>, <code>HTMLInputElement</code>, <code>HTMLButtonElement</code>, <code>HTMLSelectElement</code>,
 * <code>HTMLOptionElement</code>, <code>HTMLOptGroupElement</code>, <code>HTMLTextAreaElement</code>, <code>HTMLLabelElement</code>,
 * <code>HTMLFieldSetElement</code>, <code>HTMLLegendElement</code>, <code>HTMLOutputElement</code>, <code>HTMLDataListElement</code>,
 * <code>HTMLProgressElement</code>, <code>HTMLMeterElement</code>, the collections <code>HTMLFormControlsCollection</code>,
 * <code>HTMLOptionsCollection</code> and <code>RadioNodeList</code>, <code>ValidityState</code>, <code>FileList</code> and the
 * <code>Option</code> constructor. The IDL attributes read and write the state of the controls (the value, the checkedness, the selectedness,
 * the validity), not the content attributes; the content attributes that the standard defines as plain reflection are reflected.
 * Only the members that are listed are visible to scripts; no Java class is exposed.
 * <p>
 * {@link org.duckdns.hjow.swingwebpanel.dom.html} 의 HTML 폼 요소를 JavaScript 에 연결한다(계획 P11-05~P11-07, P15-02): 인터페이스 <code>HTMLFormElement</code>,
 * <code>HTMLInputElement</code>, <code>HTMLButtonElement</code>, <code>HTMLSelectElement</code>, <code>HTMLOptionElement</code>,
 * <code>HTMLOptGroupElement</code>, <code>HTMLTextAreaElement</code>, <code>HTMLLabelElement</code>, <code>HTMLFieldSetElement</code>,
 * <code>HTMLLegendElement</code>, <code>HTMLOutputElement</code>, <code>HTMLDataListElement</code>, <code>HTMLProgressElement</code>,
 * <code>HTMLMeterElement</code>, 컬렉션 <code>HTMLFormControlsCollection</code>, <code>HTMLOptionsCollection</code>, <code>RadioNodeList</code>,
 * <code>ValidityState</code>, <code>FileList</code>, <code>Option</code> 생성자. IDL 속성은 콘텐츠 속성이 아니라 컨트롤의 상태(값, checkedness, selectedness,
 * validity)를 읽고 쓰며, 표준이 단순 반영으로 정의한 콘텐츠 속성은 그대로 반영한다. 나열한 멤버만 스크립트에 보이며 Java 클래스는 노출되지 않는다.
 */
public final class FormBindings {

    /**
     * Creates the script <code>File</code> object of a file that the user selected. The installer of the web APIs registers one in the realm under
     * {@link #FILE_FACTORY_KEY}.
     * <p>
     * 사용자가 선택한 파일의 스크립트 <code>File</code> 객체를 만든다. 웹 API 설치기가 realm 에 {@link #FILE_FACTORY_KEY} 로 하나를 등록한다.
     */
    public interface FileFactory {
        /**
         * Creates the <code>File</code> object of an entry.
         * <p>
         * 항목의 <code>File</code> 객체를 만든다.
         *
         * @param entry the file / 파일
         * @return the script object / 스크립트 객체
         */
        JsObject newFile(FileEntry entry);
    }

    /**
     * The key of the realm intrinsic that holds the {@link FileFactory} (the object must be a {@link JsObject} that implements it).
     * <p>
     * {@link FileFactory} 를 담는 realm 내장 객체의 키(그 객체는 {@link FileFactory} 를 구현한 {@link JsObject} 여야 한다).
     */
    public static final String FILE_FACTORY_KEY = "%FormFileFactory%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormBindings() {
    }

    /**
     * Installs the form interfaces into a bridge (after the common HTML element interfaces).
     * <p>
     * 폼 인터페이스를 브리지에 설치한다(흔한 HTML 요소 인터페이스를 설치한 뒤).
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of the form collection objects.
             * <p>
             * 폼 컬렉션 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return FormBindingsCollections.classify(o);
            }
        });
        FormBindingsCollections.install(bridge);
        installValidity(bridge);
        installControls(bridge);
        installOptionConstructor(bridge);
    }

    /**
     * Returns the <code>File</code> object of a file entry (the same object for the same entry).
     * <p>
     * 파일 항목의 <code>File</code> 객체를 반환한다(같은 항목이면 같은 객체).
     *
     * @param bridge the bridge / 브리지
     * @param entry  the file / 파일
     * @return the script object / 스크립트 객체
     */
    static JsValue fileObject(DomBridge bridge, FileEntry entry) {
        JsObject f = bridge.getRealm().getIntrinsic(FILE_FACTORY_KEY);
        if (!(f instanceof FileFactory)) {
            throw JsException.typeError("File objects are not available in this page.");
        }
        return FormBindingsCollections.fileOf(bridge, (FileFactory) f, entry);
    }

    /**
     * Installs the <code>ValidityState</code> interface.
     * <p>
     * <code>ValidityState</code> 인터페이스를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installValidity(DomBridge bridge) {
        bridge.defineInterface("ValidityState", null, org.duckdns.hjow.swingwebpanel.dom.html.ValidityState.class, new String[] { "a:valueMissing", "a:typeMismatch",
            "a:patternMismatch", "a:tooLong", "a:tooShort", "a:rangeUnderflow", "a:rangeOverflow", "a:stepMismatch", "a:badInput", "a:customError",
            "a:valid" });
    }

    /**
     * The members that every listed control shares: the validation API.
     * <p>
     * 모든 목록 컨트롤이 공유하는 멤버: 검증 API.
     */
    private static final String[] VALIDATION = { "a:willValidate", "a:validity", "a:validationMessage", "m:checkValidity()", "m:reportValidity()",
        "m:setCustomValidity(String)" };

    /**
     * Joins specification arrays.
     * <p>
     * 명세 배열을 이어 붙인다.
     *
     * @param parts the arrays / 배열들
     * @return the joined array / 이어 붙인 배열
     */
    private static String[] join(String[]... parts) {
        int n = 0;
        // 전체 길이를 먼저 더한다.
        for (int i = 0; i < parts.length; i++) {
            n += parts[i].length;
        }
        String[] out = new String[n];
        int at = 0;
        // 배열을 차례로 복사한다.
        for (int i = 0; i < parts.length; i++) {
            System.arraycopy(parts[i], 0, out, at, parts[i].length);
            at += parts[i].length;
        }
        return out;
    }

    /**
     * Installs the interfaces of the form elements and the members that need more than a specification string.
     * <p>
     * 폼 요소의 인터페이스와 명세 문자열만으로 부족한 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installControls(DomBridge bridge) {
        bridge.defineInterface("SubmitEvent", "Event", org.duckdns.hjow.swingwebpanel.dom.html.SubmitEvent.class, new String[] { "a:submitter" });
        bridge.defineInterface("HTMLFormElement", "HTMLElement", HtmlFormElement.class, new String[] { "r:acceptCharset:accept-charset:string",
            "r:name::string", "r:target::string", "r:autocomplete::string", "r:noValidate:novalidate:bool", "r:rel::string", "a:action=", "a:method=", "a:enctype=",
            "a:encoding=@Enctype", "a:elements", "a:length", "m:submit()", "m:reset()", "m:checkValidity()", "m:reportValidity()" });
        FormBindingsMembers.installForm(bridge, bridge.getInterface("HTMLFormElement"));
        FormBindingsCollections.installFormIndexed(bridge);
        bridge.defineInterface("HTMLLabelElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlLabelElement.class, new String[] {
            "r:htmlFor:for:string", "a:control", "a:form" });
        DomBridge.InterfaceInfo input = bridge.defineInterface("HTMLInputElement", "HTMLElement", HtmlInputElement.class, join(new String[] {
            "r:accept::string", "r:alt::string", "r:autocomplete::string", "r:autofocus::bool", "r:dirName:dirname:string", "r:disabled::bool",
            "r:formAction:formaction:url", "r:formEnctype:formenctype:string", "r:formMethod:formmethod:string",
            "r:formNoValidate:formnovalidate:bool", "r:formTarget:formtarget:string", "r:height::uint", "r:max::string", "r:min::string",
            "r:multiple::bool", "r:name::string", "r:pattern::string", "r:placeholder::string", "r:readOnly:readonly:bool", "r:required::bool",
            "r:src::url", "r:step::string", "r:width::uint", "r:align::string", "r:useMap:usemap:string", "r:capture::string", "a:type=", "a:value=", "a:defaultValue=", "a:checked=", "a:defaultChecked=",
            "a:indeterminate=", "a:valueAsNumber=", "a:maxLength=", "a:minLength=", "a:size=", "a:form", "a:labels", "a:list",
            "a:selectionDirection=", "m:select()", "m:setSelectionRange(int,int,String*)" }, VALIDATION));
        FormBindingsMembers.installInput(bridge, input);
        bridge.defineInterface("HTMLButtonElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlButtonElement.class, join(new String[] {
            "r:autofocus::bool", "r:disabled::bool", "r:formAction:formaction:url", "r:formEnctype:formenctype:string",
            "r:formMethod:formmethod:string", "r:formNoValidate:formnovalidate:bool", "r:formTarget:formtarget:string", "r:name::string", "a:type=",
            "a:value=", "a:form", "a:labels" }, VALIDATION));
        DomBridge.InterfaceInfo area = bridge.defineInterface("HTMLTextAreaElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlTextAreaElement.class,
            join(new String[] { "r:autocomplete::string", "r:autofocus::bool", "r:dirName:dirname:string", "r:disabled::bool", "r:name::string",
                "r:placeholder::string", "r:readOnly:readonly:bool", "r:required::bool", "r:wrap::string", "a:rows=", "a:cols=", "a:maxLength=",
                "a:minLength=", "a:type", "a:defaultValue=", "a:value=", "a:textLength", "a:selectionStart=", "a:selectionEnd=",
                "a:selectionDirection=", "a:form", "a:labels", "m:select()", "m:setSelectionRange(int,int,String*)" }, VALIDATION));
        FormBindingsMembers.installTextArea(bridge, area);
        DomBridge.InterfaceInfo select = bridge.defineInterface("HTMLSelectElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement.class,
            join(new String[] { "r:autocomplete::string", "r:autofocus::bool", "r:disabled::bool", "r:multiple::bool", "r:name::string",
                "r:required::bool", "r:size::uint", "a:type", "a:options", "a:selectedOptions", "a:length=", "a:selectedIndex=", "a:value=",
                "a:form", "a:labels", "m:item(int)", "m:namedItem(String)" }, VALIDATION));
        FormBindingsMembers.installSelect(bridge, select);
        FormBindingsCollections.installSelectIndexed(bridge);
        bridge.defineInterface("HTMLOptGroupElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptGroupElement.class, new String[] {
            "r:disabled::bool", "r:label::string" });
        bridge.defineInterface("HTMLOptionElement", "HTMLElement", HtmlOptionElement.class, new String[] { "r:disabled::bool", "a:form", "a:text=",
            "a:index", "a:selected=", "a:defaultSelected=", "a:value=", "a:label=" });
        bridge.defineInterface("HTMLFieldSetElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlFieldSetElement.class, join(new String[] {
            "r:disabled::bool", "r:name::string", "a:type", "a:elements", "a:form" }, VALIDATION));
        bridge.defineInterface("HTMLLegendElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlLegendElement.class, new String[] { "r:align::string", "a:form" });
        bridge.defineInterface("HTMLOutputElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlOutputElement.class, join(new String[] {
            "r:name::string", "a:htmlFor=", "a:type", "a:defaultValue=", "a:value=", "a:form", "a:labels" }, VALIDATION));
        bridge.defineInterface("HTMLDataListElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlDataListElement.class, new String[] { "a:options" });
        bridge.defineInterface("HTMLProgressElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlProgressElement.class, new String[] { "a:value=",
            "a:max=", "a:position", "a:labels" });
        bridge.defineInterface("HTMLMeterElement", "HTMLElement", org.duckdns.hjow.swingwebpanel.dom.html.HtmlMeterElement.class, new String[] { "a:value=",
            "a:min=", "a:max=", "a:low=", "a:high=", "a:optimum=", "a:labels" });
    }

    /**
     * Installs the <code>Option</code> constructor: <code>new Option(text, value, defaultSelected, selected)</code> creates an
     * <code>option</code> element of the document of the page.
     * <p>
     * <code>Option</code> 생성자를 설치한다: <code>new Option(text, value, defaultSelected, selected)</code> 는 페이지 문서의 <code>option</code> 요소를 만든다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installOptionConstructor(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        final JsObject proto = bridge.getInterface("HTMLOptionElement").getPrototype();
        NativeFunction ctor = new NativeFunction(realm, "Option", 0, new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'Option': Please use the 'new' operator, this DOM object constructor cannot be called "
                        + "as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates the option element.
             * <p>
             * option 요소를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments (text, value, defaultSelected, selected) / 인수(text, value, defaultSelected, selected)
             * @param newTarget the new target / new 대상
             * @return the element / 요소
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Document doc = (Document) DomBridge.targetOf(r.getGlobalObject().get("document"), Document.class);
                if (doc == null) {
                    throw JsException.typeError("Failed to construct 'Option': there is no document.");
                }
                Element e = doc.createElement("option");
                // text 가 비어 있지 않으면 텍스트 노드를 자식으로 넣는다.
                if (args.length > 0 && !args[0].isUndefined()) {
                    String text = Ops.toString(args[0]);
                    if (text.length() > 0) {
                        Text t = doc.createTextNode(text);
                        e.appendChild(t);
                    }
                }
                // value 가 있으면 value 속성을 지정한다.
                if (args.length > 1 && !args[1].isUndefined()) {
                    e.setAttribute("value", Ops.toString(args[1]));
                }
                if (args.length > 2 && Ops.toBoolean(args[2])) {
                    e.setAttribute("selected", "");
                }
                if (args.length > 3 && Ops.toBoolean(args[3])) {
                    ((HtmlOptionElement) e).setSelected(true);
                }
                return (JsObject) bridge.toJs(e);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        realm.getGlobalObject().defineBuiltin("Option", ctor);
    }
}
