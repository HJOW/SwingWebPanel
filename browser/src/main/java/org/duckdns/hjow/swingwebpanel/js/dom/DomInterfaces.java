package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.CDATASection;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.ClassList;
import org.duckdns.hjow.swingwebpanel.dom.Comment;
import org.duckdns.hjow.swingwebpanel.dom.CompositionEvent;
import org.duckdns.hjow.swingwebpanel.dom.CustomEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.DocumentType;
import org.duckdns.hjow.swingwebpanel.dom.DomImplementation;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.FocusEvent;
import org.duckdns.hjow.swingwebpanel.dom.HashChangeEvent;
import org.duckdns.hjow.swingwebpanel.dom.InputEvent;
import org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.NamedAttributeMap;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.NodeList;
import org.duckdns.hjow.swingwebpanel.dom.PopStateEvent;
import org.duckdns.hjow.swingwebpanel.dom.ProcessingInstruction;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.dom.UiEvent;
import org.duckdns.hjow.swingwebpanel.dom.WheelEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The table of the DOM interfaces that scripts see (DOM Standard and the common HTML element interfaces) and the code that installs them
 * into a {@link DomBridge}. Most interfaces are described by member specification strings (see
 * {@link DomBridge#defineInterface(String, String, Class, String[])}); the few members that need real logic (event listener registration,
 * event and exception constructors, iteration helpers) are written out here.
 * <p>
 * 스크립트가 보는 DOM 인터페이스(DOM Standard 와 흔한 HTML 요소 인터페이스)의 표와 이를 {@link DomBridge} 에 설치하는 코드. 대부분의 인터페이스는 멤버
 * 명세 문자열로 기술하며({@link DomBridge#defineInterface(String, String, Class, String[])} 참고) 실제 논리가 필요한 몇몇 멤버(이벤트 리스너 등록,
 * 이벤트와 예외 생성자, 반복 도우미)는 여기에 직접 쓴다.
 */
public final class DomInterfaces {

    /**
     * The HTML namespace.
     * <p>
     * HTML 네임스페이스.
     */
    private static final String HTML_NS = "http://www.w3.org/1999/xhtml";

    /**
     * The SVG namespace.
     * <p>
     * SVG 네임스페이스.
     */
    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    /**
     * Interface names of the HTML elements by local name; every other known HTML name uses HTMLElement and unknown names HTMLUnknownElement.
     * <p>
     * 지역 이름별 HTML 요소의 인터페이스 이름. 그 밖의 알려진 HTML 이름은 HTMLElement, 모르는 이름은 HTMLUnknownElement 를 쓴다.
     */
    private static final Map<String, String> TAG_INTERFACES = new HashMap<String, String>();

    /**
     * HTML element names that have no specific interface.
     * <p>
     * 전용 인터페이스가 없는 HTML 요소 이름.
     */
    private static final String PLAIN_HTML = " abbr address article aside b bdi bdo cite code dd dfn dt em figcaption figure footer header hgroup i kbd main mark nav "
            + "noscript rp rt ruby s samp section small strong sub summary sup u var wbr center nobr tt big strike acronym ";

    static {
        String[][] pairs = { { "a", "HTMLAnchorElement" }, { "img", "HTMLImageElement" }, { "input", "HTMLInputElement" },
            { "textarea", "HTMLTextAreaElement" }, { "select", "HTMLSelectElement" }, { "option", "HTMLOptionElement" },
            { "button", "HTMLButtonElement" }, { "form", "HTMLFormElement" }, { "label", "HTMLLabelElement" },
            { "script", "HTMLScriptElement" }, { "style", "HTMLStyleElement" }, { "link", "HTMLLinkElement" },
            { "meta", "HTMLMetaElement" }, { "title", "HTMLTitleElement" }, { "body", "HTMLBodyElement" },
            { "head", "HTMLHeadElement" }, { "html", "HTMLHtmlElement" }, { "div", "HTMLDivElement" }, { "span", "HTMLSpanElement" },
            { "p", "HTMLParagraphElement" }, { "br", "HTMLBRElement" }, { "hr", "HTMLHRElement" }, { "ul", "HTMLUListElement" },
            { "ol", "HTMLOListElement" }, { "li", "HTMLLIElement" }, { "canvas", "HTMLCanvasElement" },
            { "template", "HTMLTemplateElement" }, { "h1", "HTMLHeadingElement" }, { "h2", "HTMLHeadingElement" },
            { "h3", "HTMLHeadingElement" }, { "h4", "HTMLHeadingElement" }, { "h5", "HTMLHeadingElement" },
            { "h6", "HTMLHeadingElement" }, { "pre", "HTMLPreElement" }, { "table", "HTMLTableElement" },
            { "tr", "HTMLTableRowElement" }, { "td", "HTMLTableCellElement" }, { "th", "HTMLTableCellElement" },
            { "iframe", "HTMLIFrameElement" }, { "frame", "HTMLFrameElement" }, { "frameset", "HTMLFrameSetElement" },
            { "audio", "HTMLAudioElement" }, { "video", "HTMLVideoElement" },
            { "source", "HTMLSourceElement" }, { "base", "HTMLBaseElement" }, { "blockquote", "HTMLQuoteElement" },
            { "q", "HTMLQuoteElement" }, { "optgroup", "HTMLOptGroupElement" }, { "fieldset", "HTMLFieldSetElement" },
            { "legend", "HTMLLegendElement" }, { "output", "HTMLOutputElement" }, { "datalist", "HTMLDataListElement" },
            { "progress", "HTMLProgressElement" }, { "meter", "HTMLMeterElement" } };
        // 태그 이름과 인터페이스 이름 쌍을 표에 넣는다.
        for (int i = 0; i < pairs.length; i++) {
            TAG_INTERFACES.put(pairs[i][0], pairs[i][1]);
        }
        // 반영 속성만 있는 나머지 HTML5/과거 요소의 태그도 등록한다.
        HtmlMoreInterfaces.registerTags(TAG_INTERFACES);
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DomInterfaces() {
    }

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
     * Members of the ParentNode mixin (Element, Document, DocumentFragment).
     * <p>
     * ParentNode 믹스인(Element, Document, DocumentFragment)의 멤버.
     */
    private static final String[] PARENT_NODE = { "a:firstElementChild", "a:lastElementChild", "a:childElementCount", "m:append(Object...)",
        "m:prepend(Object...)", "m:replaceChildren(Object...)" };

    /**
     * Members of the ChildNode mixin (Element, CharacterData, DocumentType).
     * <p>
     * ChildNode 믹스인(Element, CharacterData, DocumentType)의 멤버.
     */
    private static final String[] CHILD_NODE = { "a:previousElementSibling", "a:nextElementSibling", "m:before(Object...)",
        "m:after(Object...)", "m:replaceWith(Object...)", "m:remove()" };

    /**
     * Installs all DOM interfaces and the interface-specific helpers into a bridge.
     * <p>
     * 모든 DOM 인터페이스와 인터페이스별 도우미를 브리지에 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a DOM object.
             * <p>
             * DOM 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return classifyDom(o);
            }
        });
        installDomException(bridge);
        installEvents(bridge, true);
        installNodes(bridge);
        installCollections(bridge);
        installHtml(bridge);
        installEventTarget(bridge);
        EventHandlers eh = bridge.getEventHandlers();
        eh.define(bridge.getInterface("HTMLElement").getPrototype(), Element.class, EventHandlers.GLOBAL_EVENTS);
        eh.define(bridge.getInterface("Document").getPrototype(), Document.class, EventHandlers.GLOBAL_EVENTS);
        eh.define(bridge.getInterface("HTMLBodyElement").getPrototype(), Element.class, EventHandlers.WINDOW_EVENTS);
        DocumentExtras.install(bridge);
        HistoryBindings.install(bridge);
        ClipboardBindings.install(bridge);
        AnimationEventBindings.install(bridge);
        RangeBindings.install(bridge);
        GeometryBindings.install(bridge);
        MatrixBindings.install(bridge);
        EditBindings.install(bridge);
    }

    /**
     * Installs only what a script context without a document needs (a worker, plan item P20-09-b): <code>DOMException</code>,
     * <code>Event</code>, <code>CustomEvent</code>, <code>ErrorEvent</code> and <code>EventTarget</code> with its three methods. No node,
     * collection, document, UI event or storage interface is defined, so none of them is reachable from such a context.
     * <p>
     * 문서가 없는 스크립트 문맥(worker, 계획 P20-09-b)에 필요한 것만 설치한다: <code>DOMException</code>, <code>Event</code>,
     * <code>CustomEvent</code>, <code>ErrorEvent</code>, 메서드 셋을 가진 <code>EventTarget</code>. 노드, 컬렉션, 문서, UI 이벤트, 저장소
     * 인터페이스는 정의하지 않으므로 그런 문맥에서는 어느 것에도 닿을 수 없다.
     *
     * @param bridge the bridge of the context / 문맥의 브리지
     */
    public static void installEventCore(final DomBridge bridge) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of an event object.
             * <p>
             * 이벤트 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return classifyDom(o);
            }
        });
        installDomException(bridge);
        installEvents(bridge, false);
        bridge.defineInterface("EventTarget", null, EventTarget.class, new String[0]);
        installEventTarget(bridge);
    }

    /**
     * Chooses the interface name of a DOM object.
     * <p>
     * DOM 객체의 인터페이스 이름을 고른다.
     *
     * @param o the Java object / Java 객체
     * @return the interface name or null / 인터페이스 이름 또는 null
     */
    static String classifyDom(Object o) {
        if (o instanceof Element) {
            return elementInterface((Element) o);
        } else if (o instanceof CDATASection) {
            return "CDATASection";
        } else if (o instanceof Text) {
            return "Text";
        } else if (o instanceof Comment) {
            return "Comment";
        } else if (o instanceof ProcessingInstruction) {
            return "ProcessingInstruction";
        } else if (o instanceof DocumentType) {
            return "DocumentType";
        } else if (o instanceof DocumentFragment) {
            return "DocumentFragment";
        } else if (o instanceof Document) {
            return ((Document) o).isHtmlDocument() ? "HTMLDocument" : "Document";
        } else if (o instanceof Attr) {
            return "Attr";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.storage.StorageEvent) {
            return "StorageEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.storage.WebStorage) {
            return "Storage";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.DragEvent) {
            return "DragEvent";
        } else if (o instanceof WheelEvent) {
            return "WheelEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.PointerEvent) {
            return "PointerEvent";
        } else if (o instanceof MouseEvent) {
            return "MouseEvent";
        } else if (o instanceof KeyboardEvent) {
            return "KeyboardEvent";
        } else if (o instanceof FocusEvent) {
            return "FocusEvent";
        } else if (o instanceof InputEvent) {
            return "InputEvent";
        } else if (o instanceof CompositionEvent) {
            return "CompositionEvent";
        } else if (o instanceof UiEvent) {
            return "UIEvent";
        } else if (o instanceof CustomEvent) {
            return "CustomEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.MessageEvent) {
            return "MessageEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.MediaQueryListEvent) {
            return "MediaQueryListEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.TrackEvent) {
            return "TrackEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.CloseEvent) {
            return "CloseEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.PageTransitionEvent) {
            return "PageTransitionEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.BeforeUnloadEvent) {
            return "BeforeUnloadEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.ClipboardEvent) {
            return "ClipboardEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.ClipboardData) {
            return "DataTransfer";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.DataTransferItemList) {
            return "DataTransferItemList";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.DataTransferItem) {
            return "DataTransferItem";
        } else if (o instanceof PopStateEvent) {
            return "PopStateEvent";
        } else if (o instanceof HashChangeEvent) {
            return "HashChangeEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.html.SubmitEvent) {
            return "SubmitEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.css.anim.CssTransitionEvent) {
            return "TransitionEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.css.anim.CssKeyframesEvent) {
            return "AnimationEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.css.anim.AnimationPlaybackEvent) {
            // Web Animations 의 finish/cancel/remove 이벤트(인터페이스는 js.host.anim 이 정의한다).
            return "AnimationPlaybackEvent";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.ErrorEvent) {
            return "ErrorEvent";
        } else if (o instanceof Event) {
            return "Event";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.PropertyNodeList) {
            return "PropertyNodeList";
        } else if (o instanceof NodeList) {
            return "NodeList";
        } else if (o instanceof org.duckdns.hjow.swingwebpanel.dom.PropertiesCollection) {
            return "HTMLPropertiesCollection";
        } else if (o instanceof ElementCollection) {
            return "HTMLCollection";
        } else if (o instanceof NamedAttributeMap) {
            return "NamedNodeMap";
        } else if (o instanceof ClassList) {
            return "DOMTokenList";
        } else if (o instanceof DomImplementation) {
            return "DOMImplementation";
        }
        return null;
    }

    /**
     * Chooses the interface of an element from its namespace and name.
     * <p>
     * 요소의 네임스페이스와 이름으로 요소의 인터페이스를 고른다.
     *
     * @param e the element / 요소
     * @return the interface name / 인터페이스 이름
     */
    private static String elementInterface(Element e) {
        String ns = e.getNamespaceURI();
        if (HTML_NS.equals(ns)) {
            String name = e.getLocalName();
            String specific = TAG_INTERFACES.get(name);
            if (specific != null) {
                return specific;
            }
            if (PLAIN_HTML.indexOf(" " + name + " ") >= 0 || name.indexOf('-') >= 0) {
                return "HTMLElement";
            }
            return "HTMLUnknownElement";
        }
        return SVG_NS.equals(ns) ? "SVGElement" : "Element";
    }

    /**
     * Installs the DOMException interface.
     * <p>
     * DOMException 인터페이스를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installDomException(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        DomBridge.InterfaceInfo info = bridge.defineInterface("DOMException", null, Object.class, new String[0]);
        info.getPrototype().setPrototypeOf(realm.getErrorPrototype(org.duckdns.hjow.swingwebpanel.js.ErrorKind.ERROR));
        realm.setIntrinsic(DomExceptionObjects.PROTOTYPE_INTRINSIC, info.getPrototype());
        String[] names = DomExceptionObjects.legacyNames();
        // 예전 오류 코드를 INDEX_SIZE_ERR 같은 상수 이름으로 만들어 설치한다.
        for (int i = 1; i < names.length; i++) {
            String n = names[i];
            if (n.endsWith("Error")) {
                n = n.substring(0, n.length() - 5);
            }
            StringBuilder c = new StringBuilder();
            // 대문자 앞에 밑줄을 넣어 상수 이름을 만든다.
            for (int k = 0; k < n.length(); k++) {
                char ch = n.charAt(k);
                if (k > 0 && Character.isUpperCase(ch)) {
                    c.append('_');
                }
                c.append(Character.toUpperCase(ch));
            }
            c.append("_ERR");
            JsValue v = Js.num(i);
            info.getPrototype().defineData(c.toString(), v, false, true, false);
            info.getConstructor().defineData(c.toString(), v, false, true, false);
        }
        NativeFunction ctor = new NativeFunction(realm, realm.getFunctionPrototype(), "DOMException", 0, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'DOMException': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a DOMException from a message and a name.
             * <p>
             * 메시지와 이름으로 DOMException 을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the object / 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                String msg = args.length > 0 && !args[0].isUndefined() ? Ops.toString(args[0]) : "";
                String name = args.length > 1 && !args[1].isUndefined() ? Ops.toString(args[1]) : "Error";
                return DomExceptionObjects.create(bridge, name, msg);
            }
        });
        for (int i = 1; i < names.length; i++) {
            // 상수는 setConstructor 가 prototype 을 다시 연결하기 전에 이미 새 생성자에도 필요하므로 복사한다.
            JsValue v = info.getConstructor().get(constName(names[i]));
            if (!v.isUndefined()) {
                ctor.defineData(constName(names[i]), v, false, true, false);
            }
        }
        bridge.setConstructor("DOMException", ctor);
    }

    /**
     * Returns the constant name of a legacy error name.
     * <p>
     * 예전 오류 이름의 상수 이름을 반환한다.
     *
     * @param errorName the error name / 오류 이름
     * @return the constant name / 상수 이름
     */
    private static String constName(String errorName) {
        String n = errorName.endsWith("Error") ? errorName.substring(0, errorName.length() - 5) : errorName;
        StringBuilder c = new StringBuilder();
        // 대문자 앞에 밑줄을 넣어 상수 이름을 만든다.
        for (int k = 0; k < n.length(); k++) {
            char ch = n.charAt(k);
            if (k > 0 && Character.isUpperCase(ch)) {
                c.append('_');
            }
            c.append(Character.toUpperCase(ch));
        }
        return c.append("_ERR").toString();
    }

    /**
     * Reads a boolean member of an options object.
     * <p>
     * 옵션 객체의 불리언 멤버를 읽는다.
     *
     * @param options the options value / 옵션 값
     * @param name    the member name / 멤버 이름
     * @return the flag / 플래그
     */
    private static boolean flag(JsValue options, String name) {
        return options.isObject() && Ops.toBoolean(((JsObject) options).get(name));
    }

    /**
     * Installs Event and CustomEvent.
     * <p>
     * Event 와 CustomEvent 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installEvents(final DomBridge bridge, boolean ui) {
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo ev = bridge.defineInterface("Event", null, Event.class, new String[] { "c:NONE=0",
            "c:CAPTURING_PHASE=1", "c:AT_TARGET=2", "c:BUBBLING_PHASE=3", "a:type", "a:target", "a:currentTarget", "a:eventPhase",
            "a:bubbles", "a:cancelable", "a:defaultPrevented@DefaultPrevented", "a:composed", "a:isTrusted@Trusted", "a:timeStamp",
            "m:stopPropagation()", "m:stopImmediatePropagation()", "m:preventDefault()", "m:composedPath()",
            "m:initEvent(String,boolean*,boolean*)" });
        ev.getPrototype().defineAccessor("srcElement", realm.newFunction("get srcElement", 0, new NativeFunction.Call() {
            /**
             * Returns the target (legacy alias).
             * <p>
             * 대상을 반환한다(예전 별칭).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the target / 대상
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Event e = (Event) DomBridge.targetOf(self, Event.class);
                return e == null || e.getTarget() == null ? Js.NULL : bridge.toJs(e.getTarget());
            }
        }), null, true, true);
        ev.getPrototype().defineAccessor("returnValue", realm.newFunction("get returnValue", 0, new NativeFunction.Call() {
            /**
             * Returns the opposite of defaultPrevented (legacy).
             * <p>
             * defaultPrevented 의 반대를 반환한다(예전 속성).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the flag / 플래그
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Event e = (Event) DomBridge.targetOf(self, Event.class);
                return Js.bool(e == null || !e.isDefaultPrevented());
            }
        }), realm.newFunction("set returnValue", 1, new NativeFunction.Call() {
            /**
             * Setting false cancels the event (legacy).
             * <p>
             * false 로 지정하면 이벤트를 취소한다(예전 속성).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Event e = (Event) DomBridge.targetOf(self, Event.class);
                if (e != null && !Ops.toBoolean(Js.arg(args, 0))) {
                    e.preventDefault();
                }
                return Js.UNDEFINED;
            }
        }), true, true);
        NativeFunction evCtor = new NativeFunction(realm, realm.getFunctionPrototype(), "Event", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'Event': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an event from a type and an options object.
             * <p>
             * 종류와 옵션 객체로 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'Event': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                Event e = new Event(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"));
                e.setComposed(flag(opts, "composed"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, ev.getPrototype()));
                return w;
            }
        });
        bridge.setConstructor("Event", evCtor);
        final DomBridge.InterfaceInfo ce = bridge.defineInterface("CustomEvent", "Event", CustomEvent.class,
                new String[] { "a:detail", "m:initCustomEvent(String,boolean*,boolean*,Object*)" });
        NativeFunction ceCtor = new NativeFunction(realm, evCtor, "CustomEvent", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'CustomEvent': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a custom event with its detail value.
             * <p>
             * 상세 값을 가진 사용자 정의 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'CustomEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                JsValue detail = opts.isObject() ? ((JsObject) opts).get("detail") : Js.NULL;
                CustomEvent e = new CustomEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"),
                        detail.isUndefined() ? Js.NULL : detail);
                e.setComposed(flag(opts, "composed"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, ce.getPrototype()));
                return w;
            }
        });
        bridge.setConstructor("CustomEvent", ceCtor);
        installErrorEvent(bridge, evCtor);
        // 문서가 없는 문맥(worker)에는 마우스·키보드 같은 UI 이벤트 인터페이스를 만들지 않는다.
        if (ui) {
            installUiEvents(bridge, evCtor);
        }
    }

    /**
     * Installs <code>ErrorEvent</code> with its constructor (the init dictionary members <code>message</code>, <code>filename</code>,
     * <code>lineno</code>, <code>colno</code>, <code>error</code>, <code>bubbles</code> and <code>cancelable</code>).
     * <p>
     * <code>ErrorEvent</code> 를 생성자와 함께 설치한다(초기화 사전의 <code>message</code>, <code>filename</code>, <code>lineno</code>,
     * <code>colno</code>, <code>error</code>, <code>bubbles</code>, <code>cancelable</code> 멤버).
     *
     * @param bridge the bridge / 브리지
     * @param evCtor the constructor of Event / Event 의 생성자
     */
    private static void installErrorEvent(final DomBridge bridge, NativeFunction evCtor) {
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo info = bridge.defineInterface("ErrorEvent", "Event", org.duckdns.hjow.swingwebpanel.dom.ErrorEvent.class,
                new String[] { "a:message", "a:filename", "a:lineno", "a:colno", "a:error" });
        bridge.setConstructor("ErrorEvent", new NativeFunction(realm, evCtor, "ErrorEvent", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'ErrorEvent': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an error event from a type and an init dictionary.
             * <p>
             * 종류와 초기화 사전으로 오류 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'ErrorEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                org.duckdns.hjow.swingwebpanel.dom.ErrorEvent e = new org.duckdns.hjow.swingwebpanel.dom.ErrorEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"));
                JsValue err = opts.isObject() ? ((JsObject) opts).get("error") : Js.UNDEFINED;
                e.setContent(text(opts, "message", ""), text(opts, "filename", ""), (int) number(opts, "lineno"), (int) number(opts, "colno"),
                        err.isUndefined() ? null : err);
                e.setComposed(flag(opts, "composed"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Makes the Java object of a UI event from the constructor arguments.
     * <p>
     * 생성자 인수로 UI 이벤트의 Java 객체를 만든다.
     */
    private interface UiMaker {
        /**
         * Creates the event and fills the fields that are specific to its interface.
         * <p>
         * 이벤트를 만들고 그 인터페이스에 고유한 필드를 채운다.
         *
         * @param type       the event type / 이벤트 종류
         * @param bubbles    the bubbles flag / bubbles 표시
         * @param cancelable the cancelable flag / cancelable 표시
         * @param options    the options object or a non-object / 옵션 객체 또는 객체가 아닌 값
         * @return the event / 이벤트
         */
        UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue options);
    }

    /**
     * Reads a number member of an options object (0 if it is missing).
     * <p>
     * 옵션 객체의 숫자 멤버를 읽는다(없으면 0).
     *
     * @param options the options / 옵션
     * @param name    the member / 멤버
     * @return the number / 숫자
     */
    private static double number(JsValue options, String name) {
        if (!options.isObject()) {
            return 0;
        }
        JsValue v = ((JsObject) options).get(name);
        if (v.isUndefined()) {
            return 0;
        }
        double d = Ops.toNumber(v);
        return Double.isNaN(d) ? 0 : d;
    }

    /**
     * Reads a number member of an options object, with a default for a member that is missing (a dictionary default that is not 0).
     * <p>
     * 옵션 객체의 숫자 멤버를 읽는다. 멤버가 없으면 기본값을 쓴다(0 이 아닌 사전 기본값).
     *
     * @param options  the options / 옵션
     * @param name     the member / 멤버
     * @param fallback the value of a missing member / 없는 멤버의 값
     * @return the number / 숫자
     */
    private static double numberOr(JsValue options, String name, double fallback) {
        if (!options.isObject() || ((JsObject) options).get(name).isUndefined()) {
            return fallback;
        }
        return number(options, name);
    }

    /**
     * Reads a sequence of pointer events from a member of an options object (the lists of coalesced and predicted events); a member that is
     * missing is the empty list and an item that is not a <code>PointerEvent</code> is a <code>TypeError</code>.
     * <p>
     * 옵션 객체의 멤버에서 포인터 이벤트의 시퀀스를 읽는다(합쳐진 이벤트와 예측 이벤트 목록). 없는 멤버는 빈 목록이고 <code>PointerEvent</code> 가 아닌 항목은
     * <code>TypeError</code> 이다.
     *
     * @param options the options / 옵션
     * @param name    the member / 멤버
     * @return the events / 이벤트
     */
    private static java.util.List<org.duckdns.hjow.swingwebpanel.dom.PointerEvent> pointerEvents(JsValue options, String name) {
        java.util.List<org.duckdns.hjow.swingwebpanel.dom.PointerEvent> list = new java.util.ArrayList<org.duckdns.hjow.swingwebpanel.dom.PointerEvent>();
        if (!options.isObject() || ((JsObject) options).get(name).isNullOrUndefined()) {
            return list;
        }
        JsValue[] items = Ops.createListFromArrayLike(((JsObject) options).get(name));
        // 항목이 모두 PointerEvent 인지 확인하며 목록에 넣는다.
        for (int i = 0; i < items.length; i++) {
            Object t = DomBridge.targetOf(items[i], org.duckdns.hjow.swingwebpanel.dom.PointerEvent.class);
            if (t == null) {
                throw JsException.typeError("Failed to construct 'PointerEvent': Failed to read the '" + name + "' property from 'PointerEventInit': "
                        + "Failed to convert a value to 'PointerEvent'.");
            }
            list.add((org.duckdns.hjow.swingwebpanel.dom.PointerEvent) t);
        }
        return list;
    }

    /**
     * Reads a string member of an options object.
     * <p>
     * 옵션 객체의 문자열 멤버를 읽는다.
     *
     * @param options  the options / 옵션
     * @param name     the member / 멤버
     * @param fallback the value if it is missing / 없을 때의 값
     * @return the text / 문자열
     */
    private static String text(JsValue options, String name, String fallback) {
        if (!options.isObject()) {
            return fallback;
        }
        JsValue v = ((JsObject) options).get(name);
        return v.isUndefined() ? fallback : Ops.toString(v);
    }

    /**
     * Reads the related target of an options object.
     * <p>
     * 옵션 객체의 상대 대상을 읽는다.
     *
     * @param options the options / 옵션
     * @return the target or null / 대상. 없으면 null
     */
    private static EventTarget related(JsValue options) {
        if (!options.isObject()) {
            return null;
        }
        JsValue v = ((JsObject) options).get("relatedTarget");
        return v.isObject() ? (EventTarget) DomBridge.targetOf(v, EventTarget.class) : null;
    }

    /**
     * Defines one UI event interface with its constructor.
     * <p>
     * UI 이벤트 인터페이스 하나를 생성자와 함께 정의한다.
     *
     * @param bridge     the bridge / 브리지
     * @param name       the interface name / 인터페이스 이름
     * @param parent     the parent interface name / 부모 인터페이스 이름
     * @param parentCtor the constructor of the parent / 부모의 생성자
     * @param cls        the Java class / Java 클래스
     * @param specs      the member descriptions / 멤버 기술
     * @param maker      creates the Java event / Java 이벤트를 만든다
     * @return the constructor / 생성자
     */
    private static NativeFunction defineUiInterface(final DomBridge bridge, final String name, String parent, NativeFunction parentCtor,
            Class<?> cls, String[] specs, final UiMaker maker) {
        final JsRealm realm = bridge.getRealm();
        final DomBridge.InterfaceInfo info = bridge.defineInterface(name, parent, cls, specs);
        NativeFunction ctor = new NativeFunction(realm, parentCtor, name, 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates the event from a type and an options object.
             * <p>
             * 종류와 옵션 객체로 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct '" + name + "': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                UiEvent e = maker.make(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"), opts);
                e.setComposed(flag(opts, "composed"));
                e.setDetail((int) number(opts, "detail"));
                e.setModifiers(flag(opts, "ctrlKey"), flag(opts, "shiftKey"), flag(opts, "altKey"), flag(opts, "metaKey"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        });
        bridge.setConstructor(name, ctor);
        return ctor;
    }

    /**
     * Installs the UI event interfaces: UIEvent, MouseEvent, WheelEvent, KeyboardEvent, FocusEvent, InputEvent and CompositionEvent.
     * <p>
     * UI 이벤트 인터페이스를 설치한다: UIEvent, MouseEvent, WheelEvent, KeyboardEvent, FocusEvent, InputEvent, CompositionEvent.
     *
     * @param bridge  the bridge / 브리지
     * @param evCtor  the constructor of Event / Event 의 생성자
     */
    private static void installUiEvents(DomBridge bridge, NativeFunction evCtor) {
        NativeFunction ui = defineUiInterface(bridge, "UIEvent", "Event", evCtor, UiEvent.class, new String[] { "a:detail" }, new UiMaker() {
            /**
             * Creates a plain UI event.
             * <p>
             * 기본 UI 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param options    the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue options) {
                return new UiEvent(type, bubbles, cancelable);
            }
        });
        String[] modifiers = { "a:ctrlKey", "a:shiftKey", "a:altKey", "a:metaKey", "m:getModifierState(String)" };
        NativeFunction mouse = defineUiInterface(bridge, "MouseEvent", "UIEvent", ui, MouseEvent.class, join(modifiers, new String[] { "a:screenX",
            "a:screenY", "a:clientX", "a:clientY", "a:pageX", "a:pageY", "a:offsetX", "a:offsetY", "a:x@ClientX", "a:y@ClientY", "a:button",
            "a:buttons", "a:relatedTarget" }), new UiMaker() {
            /**
             * Creates a mouse event with the coordinates and buttons of the options.
             * <p>
             * 옵션의 좌표와 버튼으로 마우스 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                MouseEvent e = new MouseEvent(type, bubbles, cancelable);
                fillMouse(e, o);
                return e;
            }
        });
        defineUiInterface(bridge, "WheelEvent", "MouseEvent", mouse, WheelEvent.class, new String[] { "c:DOM_DELTA_PIXEL=0", "c:DOM_DELTA_LINE=1",
            "c:DOM_DELTA_PAGE=2", "a:deltaX", "a:deltaY", "a:deltaZ", "a:deltaMode" }, new UiMaker() {
            /**
             * Creates a wheel event with the amounts of the options.
             * <p>
             * 옵션의 양으로 휠 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                WheelEvent e = new WheelEvent(type, bubbles, cancelable);
                fillMouse(e, o);
                e.setDeltas(number(o, "deltaX"), number(o, "deltaY"), number(o, "deltaZ"), (int) number(o, "deltaMode"));
                return e;
            }
        });
        defineUiInterface(bridge, "DragEvent", "MouseEvent", mouse, org.duckdns.hjow.swingwebpanel.dom.DragEvent.class, new String[] { "a:dataTransfer" }, new UiMaker() {
            /**
             * Creates a drag event with the coordinates of the options and their <code>dataTransfer</code> (none if absent).
             * <p>
             * 옵션의 좌표와 <code>dataTransfer</code>(없으면 없음)로 끌기 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                org.duckdns.hjow.swingwebpanel.dom.DragEvent e = new org.duckdns.hjow.swingwebpanel.dom.DragEvent(type, bubbles, cancelable);
                fillMouse(e, o);
                e.setDataTransfer(EditBindings.readTransfer(o));
                return e;
            }
        });
        defineUiInterface(bridge, "PointerEvent", "MouseEvent", mouse, org.duckdns.hjow.swingwebpanel.dom.PointerEvent.class, new String[] { "a:pointerId", "a:width", "a:height",
            "a:pressure", "a:tangentialPressure", "a:tiltX", "a:tiltY", "a:twist", "a:altitudeAngle", "a:azimuthAngle", "a:pointerType",
            "a:isPrimary@Primary", "m:getCoalescedEvents()", "m:getPredictedEvents()" }, new UiMaker() {
            /**
             * Creates a pointer event from a PointerEventInit dictionary (the defaults of the specification for the members that are missing:
             * pointer 0, no type, not primary, a 1 by 1 contact, pressure 0, altitude PI / 2).
             * <p>
             * PointerEventInit 사전으로 포인터 이벤트를 만든다(없는 멤버는 규격의 기본값: 포인터 0, 종류 없음, 주 포인터 아님, 1 x 1 접촉, 압력 0, 고도각 PI / 2).
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                org.duckdns.hjow.swingwebpanel.dom.PointerEvent e = new org.duckdns.hjow.swingwebpanel.dom.PointerEvent(type, bubbles, cancelable);
                fillMouse(e, o);
                e.setPointer((int) number(o, "pointerId"), text(o, "pointerType", ""), flag(o, "isPrimary"), numberOr(o, "width", 1), numberOr(o, "height", 1),
                        number(o, "pressure"), number(o, "tangentialPressure"), (int) number(o, "tiltX"), (int) number(o, "tiltY"), (int) number(o, "twist"),
                        numberOr(o, "altitudeAngle", Math.PI / 2), number(o, "azimuthAngle"));
                e.setEventLists(pointerEvents(o, "coalescedEvents"), pointerEvents(o, "predictedEvents"));
                return e;
            }
        });
        defineUiInterface(bridge, "KeyboardEvent", "UIEvent", ui, KeyboardEvent.class, join(modifiers, new String[] { "c:DOM_KEY_LOCATION_STANDARD=0",
            "c:DOM_KEY_LOCATION_LEFT=1", "c:DOM_KEY_LOCATION_RIGHT=2", "c:DOM_KEY_LOCATION_NUMPAD=3", "a:key", "a:code", "a:location",
            "a:repeat", "a:isComposing@Composing", "a:keyCode", "a:charCode", "a:which" }), new UiMaker() {
            /**
             * Creates a keyboard event with the key of the options.
             * <p>
             * 옵션의 키로 키보드 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                KeyboardEvent e = new KeyboardEvent(type, bubbles, cancelable);
                e.setKey(text(o, "key", ""), text(o, "code", ""), (int) number(o, "location"), (int) number(o, "keyCode"), (int) number(o, "charCode"));
                e.setStates(flag(o, "repeat"), flag(o, "isComposing"));
                return e;
            }
        });
        defineUiInterface(bridge, "FocusEvent", "UIEvent", ui, FocusEvent.class, new String[] { "a:relatedTarget" }, new UiMaker() {
            /**
             * Creates a focus event with the related target of the options.
             * <p>
             * 옵션의 상대 대상으로 포커스 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                FocusEvent e = new FocusEvent(type, bubbles, cancelable);
                e.setRelatedTarget(related(o));
                return e;
            }
        });
        defineUiInterface(bridge, "InputEvent", "UIEvent", ui, InputEvent.class, new String[] { "a:data", "a:isComposing@Composing", "a:inputType", "a:dataTransfer", "m:getTargetRanges()" },
                new UiMaker() {
            /**
             * Creates an input event with the data of the options.
             * <p>
             * 옵션의 데이터로 입력 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                InputEvent e = new InputEvent(type, bubbles, cancelable);
                e.setInput(o.isObject() && !((JsObject) o).get("data").isUndefined() && !((JsObject) o).get("data").isNull() ? text(o, "data", null) : null,
                        text(o, "inputType", ""), flag(o, "isComposing"));
                e.setDataTransfer(EditBindings.readTransfer(o));
                e.setTargetRanges(EditBindings.readTargetRanges(o));
                return e;
            }
        });
        defineUiInterface(bridge, "CompositionEvent", "UIEvent", ui, CompositionEvent.class, new String[] { "a:data" }, new UiMaker() {
            /**
             * Creates a composition event with the data of the options.
             * <p>
             * 옵션의 데이터로 조합 이벤트를 만든다.
             *
             * @param type       the type / 종류
             * @param bubbles    bubbles / bubbles
             * @param cancelable cancelable / cancelable
             * @param o          the options / 옵션
             * @return the event / 이벤트
             */
            public UiEvent make(String type, boolean bubbles, boolean cancelable, JsValue o) {
                CompositionEvent e = new CompositionEvent(type, bubbles, cancelable);
                e.setData(text(o, "data", ""));
                return e;
            }
        });
    }

    /**
     * Fills the mouse fields of an event from an options object (page and offset coordinates equal the client coordinates because a
     * constructed event has no scroll position or target box).
     * <p>
     * 옵션 객체로 이벤트의 마우스 필드를 채운다(생성한 이벤트에는 스크롤 위치와 대상 상자가 없으므로 page 와 offset 좌표는 client 좌표와 같다).
     *
     * @param e the event / 이벤트
     * @param o the options / 옵션
     */
    private static void fillMouse(MouseEvent e, JsValue o) {
        double cx = number(o, "clientX");
        double cy = number(o, "clientY");
        e.setCoordinates(number(o, "screenX"), number(o, "screenY"), cx, cy, cx, cy, cx, cy);
        e.setButtons((int) number(o, "button"), (int) number(o, "buttons"));
        e.setRelatedTarget(related(o));
    }

    /**
     * Installs the node interfaces (Node, Element, Document, Text ...).
     * <p>
     * 노드 인터페이스(Node, Element, Document, Text 등)를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installNodes(DomBridge bridge) {
        bridge.defineInterface("EventTarget", null, EventTarget.class, new String[0]);
        bridge.defineInterface("Node", "EventTarget", Node.class, new String[] { "c:ELEMENT_NODE=1", "c:ATTRIBUTE_NODE=2",
            "c:TEXT_NODE=3", "c:CDATA_SECTION_NODE=4", "c:ENTITY_REFERENCE_NODE=5", "c:ENTITY_NODE=6", "c:PROCESSING_INSTRUCTION_NODE=7",
            "c:COMMENT_NODE=8", "c:DOCUMENT_NODE=9", "c:DOCUMENT_TYPE_NODE=10", "c:DOCUMENT_FRAGMENT_NODE=11", "c:NOTATION_NODE=12",
            "c:DOCUMENT_POSITION_DISCONNECTED=1", "c:DOCUMENT_POSITION_PRECEDING=2", "c:DOCUMENT_POSITION_FOLLOWING=4",
            "c:DOCUMENT_POSITION_CONTAINS=8", "c:DOCUMENT_POSITION_CONTAINED_BY=16",
            "c:DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC=32", "a:nodeType", "a:nodeName", "a:isConnected@Connected", "a:ownerDocument",
            "a:parentNode", "a:parentElement", "a:childNodes", "a:firstChild", "a:lastChild", "a:previousSibling", "a:nextSibling",
            "a:nodeValue=?", "a:textContent=?", "m:hasChildNodes()", "m:getRootNode()", "m:normalize()", "m:cloneNode(boolean*)",
            "m:isEqualNode(Node?)", "m:isSameNode(Node?)", "m:compareDocumentPosition(Node)", "m:contains(Node?)",
            "m:insertBefore(Node,Node?)", "m:appendChild(Node)", "m:replaceChild(Node,Node)", "m:removeChild(Node)" });
        bridge.defineInterface("Document", "Node", Document.class, join(PARENT_NODE, new String[] { "a:URL@Url", "a:documentURI@Url",
            "a:baseURI@BaseUri", "a:characterSet", "a:charset@CharacterSet", "a:inputEncoding@CharacterSet", "a:contentType",
            "a:compatMode", "a:doctype", "a:documentElement", "a:implementation", "m:createElement(String)",
            "m:createElementNS(String?,String)", "m:createTextNode(String)", "m:createComment(String)",
            "m:createDocumentFragment()", "m:createAttribute(String)", "m:createAttributeNS(String?,String)", "m:createCDATASection(String)",
            "m:createProcessingInstruction(String,String)",
            "m:createEvent(String)", "m:importNode(Node,boolean*)",
            "m:adoptNode(Node)", "m:getElementById(String)", "m:getElementsByTagName(String)", "m:getElementsByTagNameNS(String?,String)",
            "m:getElementsByClassName(String)", "m:querySelector(String)", "m:querySelectorAll(String)" }));
        bridge.defineInterface("HTMLDocument", "Document", Document.class, new String[0]);
        // HTML 문서는 옛 페이지처럼 document.myform, document.logo 같은 이름 있는 프로퍼티를 가진다(HTML5 2014 §5.3).
        bridge.setFactory("HTMLDocument", new DomBridge.Factory() {
            /**
             * Creates the wrapper of an HTML document with its named properties.
             * <p>
             * 이름 있는 프로퍼티를 가진 HTML 문서의 래퍼를 만든다.
             *
             * @param b      the bridge / 브리지
             * @param proto  the prototype / 프로토타입
             * @param target the document / 문서
             * @return the wrapper / 래퍼
             */
            public HostObject create(DomBridge b, JsObject proto, Object target) {
                return new DocumentNamedObject(b, proto, (Document) target);
            }
        });
        bridge.defineInterface("DocumentFragment", "Node", DocumentFragment.class, join(PARENT_NODE,
                new String[] { "m:getElementById(String)", "m:querySelector(String)", "m:querySelectorAll(String)" }));
        bridge.defineInterface("DocumentType", "Node", DocumentType.class, join(CHILD_NODE,
                new String[] { "a:name", "a:publicId", "a:systemId" }));
        bridge.defineInterface("Element", "Node", Element.class, join(PARENT_NODE, CHILD_NODE, new String[] { "a:namespaceURI",
            "a:prefix", "a:localName", "a:tagName", "a:id=", "a:className=", "a:classList", "a:attributes", "a:children",
            "a:innerHTML=?@InnerHtml", "a:outerHTML=@OuterHtml", "m:hasAttributes()", "m:getAttributeNames()", "m:getAttribute(String)",
            "m:getAttributeNS(String?,String)", "m:setAttribute(String,String)", "m:setAttributeNS(String?,String,String)",
            "m:removeAttribute(String)", "m:removeAttributeNS(String?,String)", "m:hasAttribute(String)",
            "m:hasAttributeNS(String?,String)", "m:toggleAttribute(String,Boolean*)", "m:getAttributeNode(String)",
            "m:getAttributeNodeNS(String?,String)", "m:setAttributeNode(Attr)", "m:setAttributeNodeNS(Attr)@setAttributeNode",
            "m:removeAttributeNode(Attr)",
            "m:getElementsByTagName(String)", "m:getElementsByTagNameNS(String?,String)", "m:getElementsByClassName(String)",
            "m:querySelector(String)", "m:querySelectorAll(String)", "m:matches(String)", "m:closest(String)",
            "m:insertAdjacentHTML(String,String)@insertAdjacentHtml", "m:insertAdjacentElement(String,Element)",
            "m:insertAdjacentText(String,String)",
            "r:role::nstring", "r:ariaAtomic:aria-atomic:nstring", "r:ariaAutoComplete:aria-autocomplete:nstring", "r:ariaBusy:aria-busy:nstring",
            "r:ariaChecked:aria-checked:nstring", "r:ariaColCount:aria-colcount:nstring", "r:ariaColIndex:aria-colindex:nstring",
            "r:ariaColSpan:aria-colspan:nstring", "r:ariaCurrent:aria-current:nstring", "r:ariaDescription:aria-description:nstring",
            "r:ariaDisabled:aria-disabled:nstring", "r:ariaExpanded:aria-expanded:nstring", "r:ariaHasPopup:aria-haspopup:nstring",
            "r:ariaHidden:aria-hidden:nstring", "r:ariaInvalid:aria-invalid:nstring", "r:ariaKeyShortcuts:aria-keyshortcuts:nstring",
            "r:ariaLabel:aria-label:nstring", "r:ariaLevel:aria-level:nstring", "r:ariaLive:aria-live:nstring", "r:ariaModal:aria-modal:nstring",
            "r:ariaMultiLine:aria-multiline:nstring", "r:ariaMultiSelectable:aria-multiselectable:nstring",
            "r:ariaOrientation:aria-orientation:nstring", "r:ariaPlaceholder:aria-placeholder:nstring", "r:ariaPosInSet:aria-posinset:nstring",
            "r:ariaPressed:aria-pressed:nstring", "r:ariaReadOnly:aria-readonly:nstring", "r:ariaRelevant:aria-relevant:nstring",
            "r:ariaRequired:aria-required:nstring", "r:ariaRoleDescription:aria-roledescription:nstring", "r:ariaRowCount:aria-rowcount:nstring",
            "r:ariaRowIndex:aria-rowindex:nstring", "r:ariaRowSpan:aria-rowspan:nstring", "r:ariaSelected:aria-selected:nstring",
            "r:ariaSetSize:aria-setsize:nstring", "r:ariaSort:aria-sort:nstring", "r:ariaValueMax:aria-valuemax:nstring",
            "r:ariaValueMin:aria-valuemin:nstring", "r:ariaValueNow:aria-valuenow:nstring", "r:ariaValueText:aria-valuetext:nstring" }));
        bridge.defineInterface("SVGElement", "Element", Element.class, new String[0]);
        bridge.defineInterface("Attr", null, Attr.class, new String[] { "a:name", "a:localName", "a:namespaceURI", "a:prefix",
            "a:value=", "a:ownerElement" });
        bridge.defineInterface("CharacterData", "Node", CharacterData.class, join(CHILD_NODE, new String[] { "a:data=", "a:length",
            "m:substringData(int,int)", "m:appendData(String)", "m:insertData(int,String)", "m:deleteData(int,int)",
            "m:replaceData(int,int,String)" }));
        bridge.defineInterface("Text", "CharacterData", Text.class, new String[] { "a:wholeText", "m:splitText(int)" });
        bridge.defineInterface("CDATASection", "Text", CDATASection.class, new String[0]);
        bridge.defineInterface("Comment", "CharacterData", Comment.class, new String[0]);
        bridge.defineInterface("ProcessingInstruction", "CharacterData", ProcessingInstruction.class, new String[] { "a:target" });
        bridge.defineInterface("DOMImplementation", null, DomImplementation.class, new String[] {
            "m:createDocumentType(String,String,String)", "m:hasFeature(String*,String*)" });
    }

    /**
     * Installs the list-like interfaces and their iteration helpers.
     * <p>
     * 목록 형태 인터페이스와 반복 도우미를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installCollections(DomBridge bridge) {
        JsRealm realm = bridge.getRealm();
        DomBridge.InterfaceInfo nl = bridge.defineInterface("NodeList", null, NodeList.class, new String[] { "a:length", "m:item(int)" });
        bridge.setIndexed("NodeList", new IndexedObject.Source() {
            /**
             * Returns the length of the node list.
             * <p>
             * 노드 목록의 길이를 반환한다.
             *
             * @param t the list / 목록
             * @return length / 길이
             */
            public int length(Object t) {
                return ((NodeList) t).getLength();
            }

            /**
             * Returns a node.
             * <p>
             * 노드를 반환한다.
             *
             * @param t the list / 목록
             * @param i the index / 위치
             * @return the node / 노드
             */
            public Object item(Object t, int i) {
                return ((NodeList) t).item(i);
            }

            /**
             * Node lists have no named access.
             * <p>
             * 노드 목록에는 이름 접근이 없다.
             *
             * @param t the list / 목록
             * @param n the name / 이름
             * @return null / null
             */
            public Object named(Object t, String n) {
                return null;
            }
        });
        DomBridge.InterfaceInfo hc = bridge.defineInterface("HTMLCollection", null, ElementCollection.class, new String[] { "a:length",
            "m:item(int)", "m:namedItem(String)" });
        bridge.setIndexed("HTMLCollection", new IndexedObject.Source() {
            /**
             * Returns the length of the collection.
             * <p>
             * 컬렉션의 길이를 반환한다.
             *
             * @param t the collection / 컬렉션
             * @return length / 길이
             */
            public int length(Object t) {
                return ((ElementCollection) t).getLength();
            }

            /**
             * Returns an element.
             * <p>
             * 요소를 반환한다.
             *
             * @param t the collection / 컬렉션
             * @param i the index / 위치
             * @return the element / 요소
             */
            public Object item(Object t, int i) {
                return ((ElementCollection) t).item(i);
            }

            /**
             * Returns the element with an id or name.
             * <p>
             * id 나 name 이 같은 요소를 반환한다.
             *
             * @param t the collection / 컬렉션
             * @param n the name / 이름
             * @return the element or null / 요소 또는 null
             */
            public Object named(Object t, String n) {
                return ((ElementCollection) t).namedItem(n);
            }
        });
        DomBridge.InterfaceInfo nm = bridge.defineInterface("NamedNodeMap", null, NamedAttributeMap.class, new String[] { "a:length",
            "m:item(int)", "m:getNamedItem(String)", "m:getNamedItemNS(String?,String)", "m:setNamedItem(Attr)",
            "m:removeNamedItem(String)" });
        bridge.setIndexed("NamedNodeMap", new IndexedObject.Source() {
            /**
             * Returns the number of attributes.
             * <p>
             * 속성 수를 반환한다.
             *
             * @param t the map / 맵
             * @return length / 길이
             */
            public int length(Object t) {
                return ((NamedAttributeMap) t).getLength();
            }

            /**
             * Returns an attribute.
             * <p>
             * 속성을 반환한다.
             *
             * @param t the map / 맵
             * @param i the index / 위치
             * @return the attribute / 속성
             */
            public Object item(Object t, int i) {
                return ((NamedAttributeMap) t).item(i);
            }

            /**
             * Returns the attribute with a name.
             * <p>
             * 이름이 같은 속성을 반환한다.
             *
             * @param t the map / 맵
             * @param n the name / 이름
             * @return the attribute or null / 속성 또는 null
             */
            public Object named(Object t, String n) {
                return ((NamedAttributeMap) t).getNamedItem(n);
            }
        });
        DomBridge.InterfaceInfo tl = bridge.defineInterface("DOMTokenList", null, ClassList.class, new String[] { "a:length", "a:value=",
            "m:item(int)", "m:contains(String)", "m:add(String...)", "m:remove(String...)", "m:toggle(String,Boolean*)",
            "m:replace(String,String)" });
        bridge.setIndexed("DOMTokenList", new IndexedObject.Source() {
            /**
             * Returns the number of tokens.
             * <p>
             * 토큰 수를 반환한다.
             *
             * @param t the list / 목록
             * @return length / 길이
             */
            public int length(Object t) {
                return ((ClassList) t).getLength();
            }

            /**
             * Returns a token.
             * <p>
             * 토큰을 반환한다.
             *
             * @param t the list / 목록
             * @param i the index / 위치
             * @return the token / 토큰
             */
            public Object item(Object t, int i) {
                return ((ClassList) t).item(i);
            }

            /**
             * Token lists have no named access.
             * <p>
             * 토큰 목록에는 이름 접근이 없다.
             *
             * @param t the list / 목록
             * @param n the name / 이름
             * @return null / null
             */
            public Object named(Object t, String n) {
                return null;
            }
        });
        tl.getPrototype().defineBuiltin("toString", realm.newFunction("toString", 0, new NativeFunction.Call() {
            /**
             * Returns the token list as a string.
             * <p>
             * 토큰 목록을 문자열로 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the string / 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ClassList c = (ClassList) DomBridge.targetOf(self, ClassList.class);
                if (c == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return Js.str(c.getValue());
            }
        }));
        JsValue values = realm.getIntrinsic("%ArrayProto_values%");
        JsValue forEach = realm.getArrayPrototype().get("forEach");
        DomBridge.InterfaceInfo[] iterable = { nl, hc, tl, nm };
        // 목록 형태 인터페이스에 배열의 values/forEach 를 연결해 for-of 와 forEach 가 동작하게 한다.
        for (int i = 0; i < iterable.length; i++) {
            JsObject p = iterable[i].getPrototype();
            if (values != null && !values.isUndefined()) {
                p.defineBuiltin(WellKnownSymbols.ITERATOR, values);
            }
            if (iterable[i] != hc && iterable[i] != nm) {
                p.defineBuiltin("forEach", forEach);
            }
        }
    }

    /**
     * Installs the common HTML element interfaces with their reflected content attributes.
     * <p>
     * 반영되는 콘텐츠 속성을 가진 흔한 HTML 요소 인터페이스를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installHtml(DomBridge bridge) {
        bridge.defineInterface("HTMLElement", "Element", Element.class, new String[] { "r:title::string", "r:lang::string",
            "r:dir::string", "r:hidden::bool", "r:tabIndex:tabindex:int-1", "r:accessKey::string", "r:inputMode:inputmode:string",
            "r:enterKeyHint:enterkeyhint:string", "r:autocapitalize::string", "r:nonce::string", "r:inert::bool" });
        bridge.defineInterface("HTMLUnknownElement", "HTMLElement", Element.class, new String[0]);
        // 아래의 일부 반영 속성은 HTML5 2014 REC §11.2(Non-conforming features)의 폐기된 IDL 속성이다.
        bridge.defineInterface("HTMLHtmlElement", "HTMLElement", Element.class, new String[] { "r:version::string" });
        bridge.defineInterface("HTMLHeadElement", "HTMLElement", Element.class, new String[] { "r:profile::string" });
        bridge.defineInterface("HTMLBodyElement", "HTMLElement", Element.class, new String[] { "r:text::string", "r:link::string",
            "r:vLink:vlink:string", "r:aLink:alink:string", "r:bgColor:bgcolor:string", "r:background::string" });
        bridge.defineInterface("HTMLDivElement", "HTMLElement", Element.class, new String[] { "r:align::string" });
        bridge.defineInterface("HTMLSpanElement", "HTMLElement", Element.class, new String[0]);
        bridge.defineInterface("HTMLParagraphElement", "HTMLElement", Element.class, new String[] { "r:align::string" });
        bridge.defineInterface("HTMLHeadingElement", "HTMLElement", Element.class, new String[] { "r:align::string" });
        bridge.defineInterface("HTMLBRElement", "HTMLElement", Element.class, new String[] { "r:clear::string" });
        bridge.defineInterface("HTMLHRElement", "HTMLElement", Element.class, new String[] { "r:align::string", "r:color::string",
            "r:noShade:noshade:bool", "r:size::string", "r:width::string" });
        bridge.defineInterface("HTMLPreElement", "HTMLElement", Element.class, new String[] { "r:width::int" });
        bridge.defineInterface("HTMLQuoteElement", "HTMLElement", Element.class, new String[] { "r:cite::url" });
        bridge.defineInterface("HTMLUListElement", "HTMLElement", Element.class, new String[] { "r:compact::bool", "r:type::string" });
        bridge.defineInterface("HTMLOListElement", "HTMLElement", Element.class, new String[] { "r:reversed::bool", "r:start::int",
            "r:type::string", "r:compact::bool" });
        bridge.defineInterface("HTMLLIElement", "HTMLElement", Element.class, new String[] { "r:value::int", "r:type::string" });
        bridge.defineInterface("HTMLTitleElement", "HTMLElement", Element.class, new String[0]);
        bridge.defineInterface("HTMLBaseElement", "HTMLElement", Element.class, new String[] { "r:href::url", "r:target::string" });
        bridge.defineInterface("HTMLMetaElement", "HTMLElement", Element.class, new String[] { "r:name::string", "r:content::string",
            "r:httpEquiv:http-equiv:string", "r:charset::string", "r:scheme::string", "r:media::string" });
        bridge.defineInterface("HTMLLinkElement", "HTMLElement", Element.class, new String[] { "r:href::url", "r:rel::string",
            "r:type::string", "r:media::string", "r:hreflang::string", "r:crossOrigin:crossorigin:string", "r:disabled::bool",
            "r:referrerPolicy:referrerpolicy:string", "r:as::string", "r:integrity::string", "r:charset::string", "r:rev::string",
            "r:target::string", "r:imageSrcset:imagesrcset:string", "r:imageSizes:imagesizes:string" });
        bridge.defineInterface("HTMLStyleElement", "HTMLElement", Element.class, new String[] { "r:media::string", "r:type::string",
            "r:disabled::bool" });
        bridge.defineInterface("HTMLScriptElement", "HTMLElement", Element.class, new String[] { "r:src::url", "r:type::string",
            "r:charset::string", "r:async::bool", "r:defer::bool", "r:crossOrigin:crossorigin:string", "r:noModule:nomodule:bool",
            "r:event::string", "r:htmlFor:for:string", "r:integrity::string", "r:referrerPolicy:referrerpolicy:string" });
        bridge.defineInterface("HTMLAnchorElement", "HTMLElement", Element.class, new String[] { "r:href::url", "r:target::string",
            "r:rel::string", "r:download::string", "r:hreflang::string", "r:type::string", "r:ping::string",
            "r:referrerPolicy:referrerpolicy:string", "r:coords::string", "r:charset::string", "r:name::string", "r:rev::string",
            "r:shape::string" });
        bridge.defineInterface("HTMLImageElement", "HTMLElement", Element.class, new String[] { "r:src::url", "r:alt::string",
            "r:srcset::string", "r:sizes::string", "r:crossOrigin:crossorigin:string", "r:useMap:usemap:string", "r:isMap:ismap:bool",
            "r:width::uint", "r:height::uint", "r:name::string", "r:align::string", "r:loading::string", "r:decoding::string",
            "r:referrerPolicy:referrerpolicy:string", "r:lowsrc::url", "r:hspace::uint", "r:vspace::uint", "r:border::string",
            "r:longDesc:longdesc:url", "r:fetchPriority:fetchpriority:string" });
        // 폼 요소(form, label, input, textarea, select, option, button 등)의 인터페이스는 상태를 읽고 쓰므로 FormBindings 가 설치한다.
        FormBindings.install(bridge);
        // 반영 속성만 있는 나머지 요소 인터페이스(dl, time, area, object, 표의 부분, 과거 요소 등)를 정의한다.
        HtmlMoreInterfaces.install(bridge);
        bridge.defineInterface("HTMLCanvasElement", "HTMLElement", Element.class, new String[] { "r:width::uint", "r:height::uint" });
        bridge.defineInterface("HTMLTemplateElement", "HTMLElement", Element.class, new String[0]);
        bridge.defineInterface("HTMLTableElement", "HTMLElement", Element.class, new String[] { "r:border::string", "r:summary::string",
            "r:align::string", "r:bgColor:bgcolor:string", "r:cellPadding:cellpadding:string", "r:cellSpacing:cellspacing:string",
            "r:frame::string", "r:rules::string", "r:width::string" });
        bridge.defineInterface("HTMLTableRowElement", "HTMLElement", Element.class, new String[] { "r:align::string",
            "r:bgColor:bgcolor:string", "r:ch:char:string", "r:chOff:charoff:string", "r:vAlign:valign:string" });
        bridge.defineInterface("HTMLTableCellElement", "HTMLElement", Element.class, new String[] { "r:colSpan:colspan:uint1",
            "r:rowSpan:rowspan:uintd1", "r:headers::string", "r:abbr::string", "r:scope::string", "r:align::string", "r:axis::string",
            "r:bgColor:bgcolor:string", "r:ch:char:string", "r:chOff:charoff:string", "r:height::string", "r:noWrap:nowrap:bool",
            "r:vAlign:valign:string", "r:width::string" });
        // 표 객체 모델(caption, rows, insertRow, cells, cellIndex 등)을 연결한다. 표, 구역, 행, 셀 인터페이스가 모두 정의된 뒤여야 한다.
        TableBindings.install(bridge);
        // a 와 area 의 URL 유틸리티(hostname, pathname, search, hash 등)를 연결한다. 두 인터페이스가 모두 정의된 뒤여야 한다.
        HyperlinkBindings.install(bridge);
        // map 의 areas 와 images 컬렉션을 연결한다. HTMLMapElement 가 정의된 뒤여야 한다.
        MapBindings.install(bridge);
        // contentWindow 와 contentDocument 는 중첩 브라우징 문맥을 아는 FrameApi 가 이 인터페이스에 더한다. sandbox 는 문자열 반영이다(DOMTokenList 아님).
        bridge.defineInterface("HTMLIFrameElement", "HTMLElement", Element.class, new String[] { "r:src::url", "r:srcdoc::string",
            "r:name::string", "r:width::string", "r:height::string", "r:allowFullscreen:allowfullscreen:bool", "r:sandbox::string",
            "r:scrolling::string", "r:frameBorder:frameborder:string", "r:longDesc:longdesc:url", "r:marginHeight:marginheight:string",
            "r:marginWidth:marginwidth:string", "r:align::string", "r:allow::string", "r:loading::string",
            "r:referrerPolicy:referrerpolicy:string" });
        bridge.defineInterface("HTMLFrameElement", "HTMLElement", Element.class, new String[] { "r:name::string", "r:src::url",
            "r:scrolling::string", "r:frameBorder:frameborder:string", "r:longDesc:longdesc:url", "r:marginHeight:marginheight:string",
            "r:marginWidth:marginwidth:string", "r:noResize:noresize:bool" });
        bridge.defineInterface("HTMLFrameSetElement", "HTMLElement", Element.class, new String[] { "r:cols::string", "r:rows::string" });
        bridge.defineInterface("HTMLMediaElement", "HTMLElement", Element.class, new String[] { "c:NETWORK_EMPTY=0", "c:NETWORK_IDLE=1",
            "c:NETWORK_LOADING=2", "c:NETWORK_NO_SOURCE=3", "c:HAVE_NOTHING=0", "c:HAVE_METADATA=1", "c:HAVE_CURRENT_DATA=2",
            "c:HAVE_FUTURE_DATA=3", "c:HAVE_ENOUGH_DATA=4", "r:src::url", "r:autoplay::bool",
            "r:loop::bool", "r:defaultMuted:muted:bool", "r:controls::bool", "r:preload::string", "r:crossOrigin:crossorigin:string" });
        bridge.defineInterface("HTMLAudioElement", "HTMLMediaElement", Element.class, new String[0]);
        bridge.defineInterface("HTMLVideoElement", "HTMLMediaElement", Element.class, new String[] { "r:width::uint", "r:height::uint",
            "r:playsInline:playsinline:bool", "r:poster::url" });
        bridge.defineInterface("HTMLSourceElement", "HTMLElement", Element.class, new String[] { "r:src::url", "r:type::string",
            "r:srcset::string", "r:sizes::string", "r:media::string" });
    }

    /**
     * Installs <code>addEventListener</code>, <code>removeEventListener</code> and <code>dispatchEvent</code> on EventTarget.
     * <p>
     * EventTarget 에 <code>addEventListener</code>, <code>removeEventListener</code>, <code>dispatchEvent</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installEventTarget(final DomBridge bridge) {
        JsObject proto = bridge.getInterface("EventTarget").getPrototype();
        JsRealm realm = bridge.getRealm();
        proto.defineData("addEventListener", realm.newFunction("addEventListener", 2, new NativeFunction.Call() {
            /**
             * Registers a listener.
             * <p>
             * 리스너를 등록한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                EventTarget t = thisTarget(self);
                JsValue cb = Js.arg(args, 1);
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'addEventListener' on 'EventTarget': 2 arguments required, but only "
                            + args.length + " present.");
                }
                if (!cb.isObject()) {
                    return Js.UNDEFINED;
                }
                JsValue opts = Js.arg(args, 2);
                boolean capture = opts.isObject() ? flag(opts, "capture") : Ops.toBoolean(opts);
                t.addEventListener(Ops.toString(args[0]), new JsListener(bridge, (JsObject) cb), capture, flag(opts, "once"),
                        flag(opts, "passive"));
                return Js.UNDEFINED;
            }
        }), true, true, true);
        proto.defineData("removeEventListener", realm.newFunction("removeEventListener", 2, new NativeFunction.Call() {
            /**
             * Removes a listener.
             * <p>
             * 리스너를 제거한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                EventTarget t = thisTarget(self);
                JsValue cb = Js.arg(args, 1);
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'removeEventListener' on 'EventTarget': 2 arguments required, but only "
                            + args.length + " present.");
                }
                if (!cb.isObject()) {
                    return Js.UNDEFINED;
                }
                JsValue opts = Js.arg(args, 2);
                boolean capture = opts.isObject() ? flag(opts, "capture") : Ops.toBoolean(opts);
                t.removeEventListener(Ops.toString(args[0]), new JsListener(bridge, (JsObject) cb), capture);
                return Js.UNDEFINED;
            }
        }), true, true, true);
        proto.defineData("dispatchEvent", realm.newFunction("dispatchEvent", 1, new NativeFunction.Call() {
            /**
             * Dispatches an event synchronously.
             * <p>
             * 이벤트를 동기적으로 전달한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false if the event was cancelled / 이벤트가 취소되었으면 false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                EventTarget t = thisTarget(self);
                Event e = (Event) DomBridge.targetOf(Js.arg(args, 0), Event.class);
                if (e == null) {
                    throw JsException.typeError("Failed to execute 'dispatchEvent' on 'EventTarget': parameter 1 is not of type 'Event'.");
                }
                try {
                    return Js.bool(t.dispatchEvent(e));
                } catch (org.duckdns.hjow.swingwebpanel.dom.DomException ex) {
                    throw bridge.domError(ex);
                }
            }
        }), true, true, true);
    }

    /**
     * Returns the event target of this value or throws the standard TypeError.
     * <p>
     * this 값의 이벤트 대상을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the target / 대상
     */
    private static EventTarget thisTarget(JsValue self) {
        EventTarget t = (EventTarget) DomBridge.targetOf(self, EventTarget.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return t;
    }

    /**
     * Implements <code>addEventListener</code> for a given target (shared by the interface method and the window functions).
     * <p>
     * 주어진 대상에 대한 <code>addEventListener</code> 를 구현한다(인터페이스 메서드와 window 함수가 공유).
     *
     * @param bridge the bridge / 브리지
     * @param t      the target / 대상
     * @param args   the script arguments / 스크립트 인수
     */
    public static void addListener(DomBridge bridge, EventTarget t, JsValue[] args) {
        if (args.length < 2) {
            throw JsException.typeError("Failed to execute 'addEventListener' on 'EventTarget': 2 arguments required, but only " + args.length
                    + " present.");
        }
        JsValue cb = args[1];
        if (!cb.isObject()) {
            return;
        }
        JsValue opts = Js.arg(args, 2);
        boolean capture = opts.isObject() ? flag(opts, "capture") : Ops.toBoolean(opts);
        t.addEventListener(Ops.toString(args[0]), new JsListener(bridge, (JsObject) cb), capture, flag(opts, "once"), flag(opts, "passive"));
    }

    /**
     * Implements <code>removeEventListener</code> for a given target.
     * <p>
     * 주어진 대상에 대한 <code>removeEventListener</code> 를 구현한다.
     *
     * @param bridge the bridge / 브리지
     * @param t      the target / 대상
     * @param args   the script arguments / 스크립트 인수
     */
    public static void removeListener(DomBridge bridge, EventTarget t, JsValue[] args) {
        if (args.length < 2) {
            throw JsException.typeError("Failed to execute 'removeEventListener' on 'EventTarget': 2 arguments required, but only "
                    + args.length + " present.");
        }
        JsValue cb = args[1];
        if (!cb.isObject()) {
            return;
        }
        JsValue opts = Js.arg(args, 2);
        boolean capture = opts.isObject() ? flag(opts, "capture") : Ops.toBoolean(opts);
        t.removeEventListener(Ops.toString(args[0]), new JsListener(bridge, (JsObject) cb), capture);
    }

    /**
     * Implements <code>dispatchEvent</code> for a given target.
     * <p>
     * 주어진 대상에 대한 <code>dispatchEvent</code> 를 구현한다.
     *
     * @param bridge the bridge / 브리지
     * @param t      the target / 대상
     * @param args   the script arguments / 스크립트 인수
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    public static JsValue dispatch(DomBridge bridge, EventTarget t, JsValue[] args) {
        Event e = (Event) DomBridge.targetOf(Js.arg(args, 0), Event.class);
        if (e == null) {
            throw JsException.typeError("Failed to execute 'dispatchEvent' on 'EventTarget': parameter 1 is not of type 'Event'.");
        }
        try {
            return Js.bool(t.dispatchEvent(e));
        } catch (org.duckdns.hjow.swingwebpanel.dom.DomException ex) {
            throw bridge.domError(ex);
        }
    }

    /**
     * Installs the <code>Storage</code> and <code>StorageEvent</code> interfaces (plan items P16-01, P16-08). Storage objects get named
     * property access through {@link StorageObject}.
     * <p>
     * <code>Storage</code> 와 <code>StorageEvent</code> 인터페이스를 설치한다(계획 P16-01, P16-08). Storage 객체는 {@link StorageObject} 로 이름 있는
     * 프로퍼티 접근을 가진다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void installStorage(final DomBridge bridge) {
        if (bridge.getInterface("Storage") != null) {
            return;
        }
        bridge.defineInterface("Storage", null, org.duckdns.hjow.swingwebpanel.storage.WebStorage.class, new String[] { "a:length", "m:key(int)",
            "m:getItem(String)", "m:setItem(String,String)", "m:removeItem(String)", "m:clear()" });
        bridge.setFactory("Storage", new DomBridge.Factory() {
            /**
             * Creates the named-property wrapper of a storage.
             * <p>
             * 저장소의 이름 있는 프로퍼티 래퍼를 만든다.
             *
             * @param b      the bridge / 브리지
             * @param proto  the prototype / 프로토타입
             * @param target the storage / 저장소
             * @return the wrapper / 래퍼
             */
            public HostObject create(DomBridge b, JsObject proto, Object target) {
                return new StorageObject(b, proto, (org.duckdns.hjow.swingwebpanel.storage.WebStorage) target);
            }
        });
        bridge.defineInterface("StorageEvent", "Event", org.duckdns.hjow.swingwebpanel.storage.StorageEvent.class, new String[] { "a:key", "a:oldValue",
            "a:newValue", "a:url", "a:storageArea" });
    }
}
