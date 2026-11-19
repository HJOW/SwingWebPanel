package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The element interfaces of HTML5 (W3C Recommendation 2014-10-28, chapters 4.3 to 4.9 and 4.11) and of the obsolete elements (HTML Standard
 * "Obsolete features") that have attributes to reflect but no behavior of their own: <code>HTMLDListElement</code>,
 * <code>HTMLDataElement</code>, <code>HTMLTimeElement</code>, <code>HTMLModElement</code> (<code>ins</code>, <code>del</code>),
 * <code>HTMLDetailsElement</code>, <code>HTMLDialogElement</code>, <code>HTMLMapElement</code>, <code>HTMLAreaElement</code>,
 * <code>HTMLParamElement</code>, <code>HTMLEmbedElement</code>, <code>HTMLObjectElement</code>, <code>HTMLTrackElement</code>,
 * <code>HTMLMenuElement</code>, <code>HTMLMenuItemElement</code>, <code>HTMLKeygenElement</code>, <code>HTMLPictureElement</code>, the table parts
 * <code>HTMLTableCaptionElement</code>, <code>HTMLTableColElement</code>, <code>HTMLTableSectionElement</code> and the legacy
 * <code>HTMLFontElement</code>, <code>HTMLBaseFontElement</code>, <code>HTMLDirectoryElement</code>, <code>HTMLAppletElement</code>,
 * <code>HTMLMarqueeElement</code>. Before this class these tags were <code>HTMLUnknownElement</code>. Every attribute listed is a reflecting
 * attribute: the property reads and writes the content attribute with the type conversion of the reflection rules (string, URL resolved
 * against the document, boolean, integer, unsigned integer). Methods and states that need more than that (<code>dialog.show()</code>, the table
 * methods <code>insertRow</code>, <code>createCaption</code> and so on, <code>HTMLMapElement.areas</code>, <code>HTMLTrackElement.readyState</code>)
 * are not here; the audit note <code>docs/progress/htmlifaces-audit.md</code> and the feature matrix list them as open.
 * <p>
 * HTML5(W3C 권고안 2014-10-28, 4.3~4.9장과 4.11장)의 요소 인터페이스와 반영할 속성만 있고 고유한 동작이 없는 폐기된 요소(HTML Standard "Obsolete
 * features")의 인터페이스: <code>HTMLDListElement</code>, <code>HTMLDataElement</code>, <code>HTMLTimeElement</code>, <code>HTMLModElement</code>
 * (<code>ins</code>, <code>del</code>), <code>HTMLDetailsElement</code>, <code>HTMLDialogElement</code>, <code>HTMLMapElement</code>,
 * <code>HTMLAreaElement</code>, <code>HTMLParamElement</code>, <code>HTMLEmbedElement</code>, <code>HTMLObjectElement</code>,
 * <code>HTMLTrackElement</code>, <code>HTMLMenuElement</code>, <code>HTMLMenuItemElement</code>, <code>HTMLKeygenElement</code>,
 * <code>HTMLPictureElement</code>, 표의 부분 <code>HTMLTableCaptionElement</code>, <code>HTMLTableColElement</code>,
 * <code>HTMLTableSectionElement</code>, 과거 요소 <code>HTMLFontElement</code>, <code>HTMLBaseFontElement</code>, <code>HTMLDirectoryElement</code>,
 * <code>HTMLAppletElement</code>, <code>HTMLMarqueeElement</code>. 이 클래스 이전에는 이 태그들이 <code>HTMLUnknownElement</code> 였다. 나열한 속성은
 * 모두 반영 속성이다: 프로퍼티가 콘텐츠 속성을 반영 규칙의 형 변환(문자열, 문서 기준으로 해석하는 URL, 불리언, 정수, 부호 없는 정수)으로 읽고 쓴다. 그 이상이
 * 필요한 메서드와 상태(<code>dialog.show()</code>, 표의 <code>insertRow</code>, <code>createCaption</code> 등(<code>HTMLMapElement.areas</code> 와 <code>images</code> 는 {@link MapBindings} 가 더한다),
 * <code>HTMLTrackElement.readyState</code>)는 여기에 없으며 감사 기록 <code>docs/progress/htmlifaces-audit.md</code> 와 기능 표에 미구현으로 적는다.
 */
final class HtmlMoreInterfaces {

    /**
     * The interfaces: the name of the interface, the tags that get it (separated by blanks) and the reflecting attributes in the format of
     * {@link DomBridge#defineInterface}.
     * <p>
     * 인터페이스 표: 인터페이스 이름, 그것을 받는 태그(공백으로 구분), {@link DomBridge#defineInterface} 형식의 반영 속성.
     */
    private static final String[][] TABLE = {
        { "HTMLDListElement", "dl", "r:compact::bool" },
        { "HTMLDataElement", "data", "r:value::string" },
        { "HTMLTimeElement", "time", "r:dateTime:datetime:string" },
        { "HTMLModElement", "ins del", "r:cite::url", "r:dateTime:datetime:string" },
        { "HTMLDetailsElement", "details", "r:open::bool", "r:name::string" },
        { "HTMLDialogElement", "dialog", "r:open::bool" },
        { "HTMLMapElement", "map", "r:name::string" },
        { "HTMLAreaElement", "area", "r:alt::string", "r:coords::string", "r:shape::string", "r:href::url", "r:target::string",
            "r:download::string", "r:rel::string", "r:hreflang::string", "r:type::string", "r:ping::string", "r:noHref:nohref:bool",
            "r:referrerPolicy:referrerpolicy:string" },
        { "HTMLParamElement", "param", "r:name::string", "r:value::string", "r:type::string", "r:valueType:valuetype:string" },
        { "HTMLEmbedElement", "embed", "r:src::url", "r:type::string", "r:width::string", "r:height::string", "r:align::string",
            "r:name::string" },
        { "HTMLObjectElement", "object", "r:data::url", "r:type::string", "r:typeMustMatch:typemustmatch:bool", "r:name::string",
            "r:useMap:usemap:string", "r:width::string", "r:height::string", "r:align::string", "r:archive::string", "r:border::string",
            "r:code::string", "r:codeBase:codebase:string", "r:codeType:codetype:string", "r:declare::bool", "r:hspace::uint",
            "r:standby::string", "r:vspace::uint" },
        { "HTMLTrackElement", "track", "c:NONE=0", "c:LOADING=1", "c:LOADED=2", "c:ERROR=3", "r:kind::string", "r:src::url", "r:srclang::string",
            "r:label::string", "r:default::bool" },
        { "HTMLMenuElement", "menu", "r:type::string", "r:label::string", "r:compact::bool" },
        { "HTMLMenuItemElement", "menuitem", "r:type::string", "r:label::string", "r:icon::url", "r:disabled::bool", "r:checked::bool",
            "r:radiogroup::string", "r:default::bool" },
        { "HTMLKeygenElement", "keygen", "r:autofocus::bool", "r:challenge::string", "r:disabled::bool", "r:keytype::string",
            "r:name::string" },
        { "HTMLPictureElement", "picture" },
        { "HTMLTableCaptionElement", "caption", "r:align::string" },
        { "HTMLTableColElement", "col colgroup", "r:span::uint1", "r:align::string", "r:ch:char:string", "r:chOff:charoff:string",
            "r:vAlign:valign:string", "r:width::string" },
        { "HTMLTableSectionElement", "thead tbody tfoot", "r:align::string", "r:ch:char:string", "r:chOff:charoff:string",
            "r:vAlign:valign:string" },
        { "HTMLFontElement", "font", "r:color::string", "r:face::string", "r:size::string" },
        { "HTMLBaseFontElement", "basefont", "r:color::string", "r:face::string", "r:size::int" },
        { "HTMLDirectoryElement", "dir", "r:compact::bool" },
        { "HTMLAppletElement", "applet", "r:align::string", "r:alt::string", "r:archive::string", "r:code::string", "r:codeBase:codebase:string",
            "r:height::string", "r:hspace::uint", "r:name::string", "r:object::string", "r:vspace::uint", "r:width::string" },
        { "HTMLMarqueeElement", "marquee", "r:behavior::string", "r:bgColor:bgcolor:string", "r:direction::string", "r:height::string",
            "r:hspace::uint", "r:loop::int", "r:scrollAmount:scrollamount:uint", "r:scrollDelay:scrolldelay:uint",
            "r:trueSpeed:truespeed:bool", "r:vspace::uint", "r:width::string" } };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private HtmlMoreInterfaces() {
    }

    /**
     * Adds the tags of these interfaces to the table that maps a tag to its interface.
     * <p>
     * 이 인터페이스들의 태그를 태그에서 인터페이스로 가는 표에 더한다.
     *
     * @param tagInterfaces the table (tag to interface name) / 표(태그에서 인터페이스 이름으로)
     */
    static void registerTags(Map<String, String> tagInterfaces) {
        // 표의 인터페이스마다 그 태그들을 등록한다.
        for (int i = 0; i < TABLE.length; i++) {
            String[] tags = TABLE[i][1].split(" ");
            // 인터페이스에 속한 태그를 하나씩 표에 넣는다.
            for (int t = 0; t < tags.length; t++) {
                tagInterfaces.put(tags[t], TABLE[i][0]);
            }
        }
    }

    /**
     * Defines the interfaces in a bridge.
     * <p>
     * 인터페이스를 브리지에 정의한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void install(DomBridge bridge) {
        // 표의 인터페이스를 HTMLElement 의 하위로 정의한다.
        for (int i = 0; i < TABLE.length; i++) {
            String[] specs = new String[TABLE[i].length - 2];
            System.arraycopy(TABLE[i], 2, specs, 0, specs.length);
            bridge.defineInterface(TABLE[i][0], "HTMLElement", Element.class, specs);
        }
    }
}
