package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * The script members of the elements whose state is more than a reflected attribute (HTML5 2014 REC chapters 4.8, 4.11 and 4.12 and the
 * HTML Standard where the 2014 text has no counterpart):
 * <ul>
 * <li><code>HTMLMapElement.areas</code>: the live collection of the <code>area</code> descendants (the same object every time);</li>
 * <li><code>HTMLDialogElement</code>: <code>show()</code>, <code>showModal()</code>, <code>close(returnValue)</code> and
 * <code>returnValue</code>; <code>close()</code> queues a task that fires the <code>close</code> event (it does not bubble). A modal dialog enters the
 * top layer of the document (it matches <code>:modal</code> and is painted above everything over its <code>::backdrop</code>); the rest of the document
 * is not made inert;</li>
 * <li><code>HTMLTrackElement</code>: its members (<code>kind</code>, <code>readyState</code>, <code>track</code>) are installed by {@link TextTrackBindings} together with the text tracks;</li>
 * <li><code>HTMLObjectElement</code>: <code>form</code> (the form owner), <code>contentDocument</code> and <code>contentWindow</code> (always
 * <code>null</code>: no plug-in or nested document is run), <code>getSVGDocument()</code> (<code>null</code>), and the constraint validation
 * members of an element that is barred from it: <code>willValidate</code> is false, <code>validationMessage</code> is empty,
 * <code>checkValidity()</code> and <code>reportValidity()</code> are true, <code>setCustomValidity()</code> changes nothing.</li>
 * </ul>
 * The members are installed on the prototypes of the element interfaces that {@link org.duckdns.hjow.swingwebpanel.js.dom.DomBridge} has defined. Use:
 * <code>new ElementStates(page).install();</code> once per page, after the interfaces exist.
 * <p>
 * 속성 반영보다 큰 상태를 가진 요소의 스크립트 멤버(HTML5 2014 REC 4.8, 4.11, 4.12장과 2014 문서에 대응하는 내용이 없는 부분은 HTML Standard):
 * <ul>
 * <li><code>HTMLMapElement.areas</code>: <code>area</code> 후손의 라이브 컬렉션(매번 같은 객체);</li>
 * <li><code>HTMLDialogElement</code>: <code>show()</code>, <code>showModal()</code>, <code>close(returnValue)</code>, <code>returnValue</code>.
 * <code>close()</code> 는 <code>close</code> 이벤트(버블링하지 않음)를 발생시키는 작업을 예약한다. 모달 대화상자는 문서의 최상위 계층에 들어간다(<code>:modal</code> 과
 * 일치하고 <code>::backdrop</code> 위에서 모든 것 위에 그려진다). 문서의 나머지를 비활성으로 만들지는 않는다;</li>
 * <li><code>HTMLTrackElement</code>: 멤버(<code>kind</code>, <code>readyState</code>, <code>track</code>)는 텍스트 트랙과 함께 {@link TextTrackBindings} 가 설치한다;</li>
 * <li><code>HTMLObjectElement</code>: <code>form</code>(폼 소유자), <code>contentDocument</code> 와 <code>contentWindow</code>(항상 <code>null</code>:
 * 플러그인이나 중첩 문서를 실행하지 않는다), <code>getSVGDocument()</code>(<code>null</code>), 그리고 제약 검증 대상이 아닌 요소의 멤버:
 * <code>willValidate</code> 는 false, <code>validationMessage</code> 는 빈 문자열, <code>checkValidity()</code>/<code>reportValidity()</code> 는
 * true, <code>setCustomValidity()</code> 는 아무것도 바꾸지 않는다.</li>
 * </ul>
 * 멤버는 {@link org.duckdns.hjow.swingwebpanel.js.dom.DomBridge} 가 정의한 요소 인터페이스의 프로토타입에 설치한다. 사용: 인터페이스가 만들어진 뒤 페이지마다
 * 한 번 <code>new ElementStates(page).install();</code>.
 */
final class ElementStates {

    /**
     * The page whose elements get the members.
     * <p>
     * 멤버를 받는 요소가 속한 페이지.
     */
    private final PageScripts page;

    /**
     * The <code>areas</code> collection of every map that asked for it (so the same object is returned every time).
     * <p>
     * <code>areas</code> 를 요청한 map 마다의 컬렉션(매번 같은 객체를 돌려주기 위함).
     */
    private final Map<Element, JsValue> areas = new WeakHashMap<Element, JsValue>();

    /**
     * The <code>returnValue</code> of the dialogs that have one.
     * <p>
     * <code>returnValue</code> 를 가진 대화상자의 값.
     */
    private final Map<Element, String> returnValues = new WeakHashMap<Element, String>();

    /**
     * The dialogs that were opened with <code>showModal()</code> and are still open.
     * <p>
     * <code>showModal()</code> 로 열었고 아직 열려 있는 대화상자.
     */
    private final Set<Element> modals = Collections.newSetFromMap(new WeakHashMap<Element, Boolean>());

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    ElementStates(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the element of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 요소를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element element(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter / 읽기
     * @param set   the setter or null / 쓰기. 없으면 null
     */
    private void accessor(JsObject proto, String name, NativeFunction.Call get, NativeFunction.Call set) {
        JsRealm r = page.getRealm();
        proto.defineAccessor(name, r.newFunction("get " + name, 0, get), set == null ? null : r.newFunction("set " + name, 1, set), true, true);
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param arity the declared number of arguments / 선언한 인수 개수
     * @param body  the work / 하는 일
     */
    private void method(JsObject proto, String name, int arity, NativeFunction.Call body) {
        proto.defineBuiltin(name, page.getRealm().newFunction(name, arity, body));
    }

    /**
     * Turns a DOM exception of Java code into the exception of the script.
     * <p>
     * Java 코드의 DOM 예외를 스크립트 예외로 바꾼다.
     *
     * @param ex the exception / 예외
     * @return the script exception / 스크립트 예외
     */
    private JsException error(DomException ex) {
        return page.getBridge().domError(ex);
    }

    /**
     * Installs all the members.
     * <p>
     * 모든 멤버를 설치한다.
     */
    void install() {
        installMap();
        installDialog();
        installObject();
        installGlobalAttributes();
        // rel 속성의 토큰 목록 relList 는 링크 요소 셋이 같다(iframe 의 sandbox 는 프레임 작업이 맡는다).
        DomBridge bridge = page.getBridge();
        tokenList(bridge.getInterface("HTMLAnchorElement").getPrototype(), "relList", "rel");
        tokenList(bridge.getInterface("HTMLAreaElement").getPrototype(), "relList", "rel");
        tokenList(bridge.getInterface("HTMLLinkElement").getPrototype(), "relList", "rel");
        tokenList(bridge.getInterface("HTMLFormElement").getPrototype(), "relList", "rel");
        tokenList(bridge.getInterface("HTMLLinkElement").getPrototype(), "sizes", "sizes");
        // embed 와 iframe 의 getSVGDocument 는 object 와 같이 중첩 문서를 실행하지 않으므로 null 을 반환한다.
        nullMethod(bridge.getInterface("HTMLEmbedElement").getPrototype(), "getSVGDocument");
        nullMethod(bridge.getInterface("HTMLIFrameElement").getPrototype(), "getSVGDocument");
    }

    /**
     * Reacts to a change of the document: when the <code>open</code> attribute of a connected <code>details</code> element is added or removed,
     * a task is queued that fires the <code>toggle</code> event at the element (it does not bubble and cannot be cancelled; HTML Standard
     * 4.11.1). A change of the value of the attribute fires nothing.
     * <p>
     * 문서의 변경에 반응한다: 문서에 연결된 <code>details</code> 요소의 <code>open</code> 속성이 더해지거나 지워지면 요소에 <code>toggle</code> 이벤트(버블링하지
     * 않고 취소할 수 없음, HTML 표준 4.11.1)를 발생시키는 작업을 예약한다. 속성 값만 바뀐 경우에는 아무것도 발생시키지 않는다.
     *
     * @param record    the change / 변경
     * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
     */
    void mutated(MutationRecord record, boolean connected) {
        if (!connected || record.getType() != MutationRecord.ATTRIBUTES || !"open".equals(record.getAttributeName())
                || !(record.getTarget() instanceof Element)) {
            return;
        }
        final Element details = (Element) record.getTarget();
        boolean was = record.getOldValue() != null;
        boolean now = details.hasAttribute("open");
        if (was == now || !"details".equals(details.getLocalName()) || !org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML.equals(details.getNamespaceURI())) {
            return;
        }
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null) {
            return;
        }
        s.post(new Runnable() {
            /**
             * Fires the event under the page lock.
             * <p>
             * 페이지 잠금 아래에서 이벤트를 발생시킨다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Fires the event.
                     * <p>
                     * 이벤트를 발생시킨다.
                     */
                    public void run() {
                        page.dispatchEvent(details, new Event("toggle", false, false));
                    }
                });
            }
        }, 0);
    }

    /**
     * Defines a method without parameters that always returns <code>null</code> (after checking the receiver).
     * <p>
     * 항상 <code>null</code> 을 반환하는 매개변수 없는 메서드를 정의한다(수신자를 확인한 뒤).
     *
     * @param proto the prototype / 프로토타입
     * @param name  the method name / 메서드 이름
     */
    private void nullMethod(JsObject proto, String name) {
        method(proto, name, 0, new NativeFunction.Call() {
            /**
             * Returns null.
             * <p>
             * null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return null / null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.NULL;
            }
        });
    }

    /**
     * Installs the <code>areas</code> property of the map element (<code>HTMLMapElement.areas</code>).
     * <p>
     * map 요소의 <code>areas</code> 프로퍼티(<code>HTMLMapElement.areas</code>)를 설치한다.
     */
    private void installMap() {
        final DomBridge bridge = page.getBridge();
        accessor(bridge.getInterface("HTMLMapElement").getPrototype(), "areas", new NativeFunction.Call() {
            /**
             * Returns the live collection of the <code>area</code> descendants.
             * <p>
             * <code>area</code> 후손의 라이브 컬렉션을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element map = element(self);
                JsValue c = areas.get(map);
                if (c == null) {
                    c = bridge.toJs(map.getElementsByTagName("area"));
                    areas.put(map, c);
                }
                return c;
            }
        }, null);
    }

    /**
     * Returns the <code>returnValue</code> of a dialog.
     * <p>
     * 대화상자의 <code>returnValue</code> 를 반환한다.
     *
     * @param dialog the dialog / 대화상자
     * @return the value, empty at the start / 값. 처음에는 빈 문자열
     */
    private String returnValueOf(Element dialog) {
        String v = returnValues.get(dialog);
        return v == null ? "" : v;
    }

    /**
     * Queues the task that fires the <code>close</code> event of a dialog (it does not bubble and cannot be cancelled).
     * <p>
     * 대화상자의 <code>close</code> 이벤트(버블링하지 않고 취소할 수 없다)를 발생시키는 작업을 예약한다.
     *
     * @param dialog the dialog / 대화상자
     */
    private void queueClose(final Element dialog) {
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null) {
            return;
        }
        s.post(new Runnable() {
            /**
             * Fires the event under the page lock.
             * <p>
             * 페이지 잠금 아래에서 이벤트를 발생시킨다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Fires the event.
                     * <p>
                     * 이벤트를 발생시킨다.
                     */
                    public void run() {
                        page.dispatchEvent(dialog, new Event("close", false, false));
                    }
                });
            }
        }, 0);
    }

    /**
     * Installs the members of <code>HTMLDialogElement</code>.
     * <p>
     * <code>HTMLDialogElement</code> 의 멤버를 설치한다.
     */
    private void installDialog() {
        JsObject proto = page.getBridge().getInterface("HTMLDialogElement").getPrototype();
        accessor(proto, "returnValue", new NativeFunction.Call() {
            /**
             * Reads the return value.
             * <p>
             * 반환값을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(returnValueOf(element(self)));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes the return value.
             * <p>
             * 반환값을 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                returnValues.put(element(self), Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        method(proto, "show", 0, new NativeFunction.Call() {
            /**
             * Opens the dialog without making it modal.
             * <p>
             * 대화상자를 모달 없이 연다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element d = element(self);
                // 이미 열려 있으면 아무것도 하지 않는다.
                if (!d.hasAttribute("open")) {
                    d.setAttribute("open", "");
                }
                return Js.UNDEFINED;
            }
        });
        method(proto, "showModal", 0, new NativeFunction.Call() {
            /**
             * Opens the dialog as a modal dialog in the top layer of the document (over its ::backdrop).
             * <p>
             * 대화상자를 문서의 최상위 계층에서(::backdrop 위에) 모달로 연다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element d = element(self);
                // 이미 열려 있거나 문서에 연결되어 있지 않으면 InvalidStateError.
                if (d.hasAttribute("open")) {
                    throw error(DomException.invalidState("The element already has an 'open' attribute, and therefore cannot be opened modally."));
                }
                if (!d.isConnected()) {
                    throw error(DomException.invalidState("The element is not in a Document."));
                }
                // 모달 대화상자는 최상위 계층에 들어가 :modal 이 되고 ::backdrop 위에 그려진다(CSS Positioned Layout 4 3 절). 속성보다 먼저 넣어 다시 계산하는 스타일이 보게 한다.
                d.getOwnerDocument().addToTopLayer(d);
                d.setAttribute("open", "");
                modals.add(d);
                return Js.UNDEFINED;
            }
        });
        method(proto, "close", 0, new NativeFunction.Call() {
            /**
             * Closes the dialog, sets <code>returnValue</code> if an argument is given and queues the <code>close</code> event.
             * <p>
             * 대화상자를 닫고, 인수가 있으면 <code>returnValue</code> 를 정하고, <code>close</code> 이벤트를 예약한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element d = element(self);
                // 열려 있지 않으면 아무것도 하지 않는다.
                if (!d.hasAttribute("open")) {
                    return Js.UNDEFINED;
                }
                // 최상위 계층에서 먼저 빼서 open 속성의 변경으로 다시 계산하는 스타일이 :modal 이 아님을 보게 한다.
                d.getOwnerDocument().removeFromTopLayer(d);
                d.removeAttribute("open");
                modals.remove(d);
                if (args.length > 0 && !args[0].isUndefined()) {
                    returnValues.put(d, Ops.toString(args[0]));
                }
                queueClose(d);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Returns the form owner of an <code>object</code> element: the form of the <code>form</code> attribute, or else the nearest
     * <code>form</code> ancestor.
     * <p>
     * <code>object</code> 요소의 폼 소유자를 반환한다: <code>form</code> 속성이 가리키는 폼, 없으면 가장 가까운 <code>form</code> 조상.
     *
     * @param object the element / 요소
     * @return the form or null / 폼 또는 null
     */
    private static Element formOwnerOf(Element object) {
        String id = object.getAttribute("form");
        if (id != null) {
            // form 속성이 있으면 같은 문서의 그 id 를 가진 form 요소만 소유자가 된다.
            Element e = object.isConnected() ? object.document().getElementById(id) : null;
            return e != null && "form".equals(e.getLocalName()) ? e : null;
        }
        // 속성이 없으면 가장 가까운 form 조상을 찾는다.
        for (Node n = object.getParentNode(); n != null; n = n.getParentNode()) {
            if (n instanceof Element && "form".equals(((Element) n).getLocalName())) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Installs the members of <code>HTMLObjectElement</code>.
     * <p>
     * <code>HTMLObjectElement</code> 의 멤버를 설치한다.
     */
    private void installObject() {
        final DomBridge bridge = page.getBridge();
        JsObject proto = bridge.getInterface("HTMLObjectElement").getPrototype();
        accessor(proto, "form", new NativeFunction.Call() {
            /**
             * Returns the form owner or null.
             * <p>
             * 폼 소유자 또는 null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the form or null / 폼 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element f = formOwnerOf(element(self));
                return f == null ? Js.NULL : bridge.toJs(f);
            }
        }, null);
        nullAccessor(proto, "contentDocument");
        nullAccessor(proto, "contentWindow");
        method(proto, "getSVGDocument", 0, new NativeFunction.Call() {
            /**
             * Returns null: no nested document is run.
             * <p>
             * null 을 반환한다: 중첩 문서를 실행하지 않는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return null / null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.NULL;
            }
        });
        accessor(proto, "willValidate", new NativeFunction.Call() {
            /**
             * Returns false: an <code>object</code> element is barred from constraint validation.
             * <p>
             * false 를 반환한다: <code>object</code> 요소는 제약 검증 대상이 아니다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false / false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.bool(false);
            }
        }, null);
        accessor(proto, "validationMessage", new NativeFunction.Call() {
            /**
             * Returns the empty string.
             * <p>
             * 빈 문자열을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return an empty string / 빈 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.str("");
            }
        }, null);
        NativeFunction.Call valid = new NativeFunction.Call() {
            /**
             * Returns true: there is nothing to validate.
             * <p>
             * true 를 반환한다: 검증할 것이 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return true / true
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.bool(true);
            }
        };
        method(proto, "checkValidity", 0, valid);
        method(proto, "reportValidity", 0, valid);
        method(proto, "setCustomValidity", 1, new NativeFunction.Call() {
            /**
             * Does nothing (the element is barred from constraint validation).
             * <p>
             * 아무것도 하지 않는다(제약 검증 대상이 아니다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Returns the value of an enumerated global attribute of an element or of the nearest ancestor that has a known value (the "inherit"
     * state of <code>translate</code> and <code>spellcheck</code>).
     * <p>
     * 요소의 열거형 전역 속성 값을, 없으면 알려진 값을 가진 가장 가까운 조상의 값을 반환한다(<code>translate</code>, <code>spellcheck</code> 의
     * 상속 상태).
     *
     * @param e           the element / 요소
     * @param attr        the attribute / 속성
     * @param yes         the value (lower case) that means true / 참을 뜻하는 값(소문자)
     * @param no          the value (lower case) that means false / 거짓을 뜻하는 값(소문자)
     * @param emptyYes    true if the empty string means true / 빈 문자열이 참이면 true
     * @param rootDefault the result when no element decides / 결정하는 요소가 없을 때의 결과
     * @return the state / 상태
     */
    private static boolean inherited(Element e, String attr, String yes, String no, boolean emptyYes, boolean rootDefault) {
        // 요소에서 위로 올라가며 값이 정해진 첫 요소를 찾는다.
        for (Node n = e; n instanceof Element; n = n.getParentNode()) {
            String v = ((Element) n).getAttribute(attr);
            if (v == null) {
                continue;
            }
            String lower = v.trim().toLowerCase();
            if (yes.equals(lower) || (emptyYes && lower.length() == 0)) {
                return true;
            }
            if (no.equals(lower)) {
                return false;
            }
        }
        return rootDefault;
    }

    /**
     * Returns the state of <code>contenteditable</code> of an element: <code>true</code>, <code>false</code>, <code>plaintext-only</code> or
     * <code>inherit</code> (the attribute is missing or has a wrong value).
     * <p>
     * 요소의 <code>contenteditable</code> 상태를 반환한다: <code>true</code>, <code>false</code>, <code>plaintext-only</code>,
     * <code>inherit</code>(속성이 없거나 값이 잘못됨).
     *
     * @param e the element / 요소
     * @return the state / 상태
     */
    private static String editableState(Element e) {
        String v = e.getAttribute("contenteditable");
        if (v == null) {
            return "inherit";
        }
        String lower = v.trim().toLowerCase();
        if (lower.length() == 0 || "true".equals(lower)) {
            return "true";
        }
        return "false".equals(lower) || "plaintext-only".equals(lower) ? lower : "inherit";
    }

    /**
     * Defines a boolean accessor of <code>HTMLElement</code> that is stored as an enumerated attribute.
     * <p>
     * 열거형 속성으로 저장하는 <code>HTMLElement</code> 의 불리언 접근자를 정의한다.
     *
     * @param he       the prototype / 프로토타입
     * @param name     the property and attribute name / 프로퍼티 겸 속성 이름
     * @param yes      the attribute value for true / 참의 속성 값
     * @param no       the attribute value for false / 거짓의 속성 값
     * @param emptyYes true if the empty string means true / 빈 문자열이 참이면 true
     */
    private void enumerated(JsObject he, final String name, final String yes, final String no, final boolean emptyYes) {
        accessor(he, name, new NativeFunction.Call() {
            /**
             * Reads the state (inherited from the ancestors, true at the root).
             * <p>
             * 상태를 읽는다(조상에서 상속하며 루트에서는 true).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(inherited(element(self), name, yes, no, emptyYes, true));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes the attribute with the value of the state.
             * <p>
             * 상태의 값으로 속성을 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setAttribute(name, Ops.toBoolean(Js.arg(args, 0)) ? yes : no);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs the members of <code>HTMLElement</code> whose value is more than a reflected attribute (<code>draggable</code>,
     * <code>spellcheck</code>, <code>translate</code>, <code>contentEditable</code>, <code>isContentEditable</code>, <code>outerText</code>,
     * <code>accessKeyLabel</code>) and the old alias <code>webkitMatchesSelector</code>. Editing and dragging themselves are P20-06; these
     * members only keep and report the state.
     * <p>
     * 값이 속성 반영 이상인 <code>HTMLElement</code> 멤버(<code>draggable</code>, <code>spellcheck</code>, <code>translate</code>,
     * <code>contentEditable</code>, <code>isContentEditable</code>, <code>outerText</code>, <code>accessKeyLabel</code>)와 옛 별칭
     * <code>webkitMatchesSelector</code> 를 설치한다. 편집과 끌기 자체는 P20-06 이며 이 멤버들은 상태를 보관하고 알려 주기만 한다.
     */
    private void installGlobalAttributes() {
        final DomBridge bridge = page.getBridge();
        JsObject he = bridge.getInterface("HTMLElement").getPrototype();
        accessor(he, "draggable", new NativeFunction.Call() {
            /**
             * Reads the draggable state: <code>true</code>/<code>false</code> of the attribute, else images and links with an
             * <code>href</code> are draggable.
             * <p>
             * 끌 수 있는지 읽는다: 속성의 <code>true</code>/<code>false</code>, 없으면 이미지와 <code>href</code> 가 있는 링크는 끌 수 있다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                String v = e.getAttribute("draggable");
                String lower = v == null ? "" : v.trim().toLowerCase();
                if ("true".equals(lower) || "false".equals(lower)) {
                    return Js.bool("true".equals(lower));
                }
                String tag = e.getLocalName();
                return Js.bool("img".equals(tag) || ("a".equals(tag) && e.hasAttribute("href")));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes the attribute as <code>true</code> or <code>false</code>.
             * <p>
             * 속성을 <code>true</code> 또는 <code>false</code> 로 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setAttribute("draggable", Ops.toBoolean(Js.arg(args, 0)) ? "true" : "false");
                return Js.UNDEFINED;
            }
        });
        enumerated(he, "spellcheck", "true", "false", true);
        enumerated(he, "translate", "yes", "no", true);
        accessor(he, "contentEditable", new NativeFunction.Call() {
            /**
             * Reads the state of the <code>contenteditable</code> attribute.
             * <p>
             * <code>contenteditable</code> 속성의 상태를 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return <code>true</code>, <code>false</code>, <code>plaintext-only</code> or <code>inherit</code> / 상태 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(editableState(element(self)));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes the state: <code>inherit</code> removes the attribute and a value that is not a state is a SyntaxError.
             * <p>
             * 상태를 쓴다. <code>inherit</code> 는 속성을 지우고 상태가 아닌 값은 SyntaxError 이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                String v = Ops.toString(Js.arg(args, 0)).toLowerCase();
                if ("inherit".equals(v)) {
                    e.removeAttribute("contenteditable");
                } else if ("true".equals(v) || "false".equals(v) || "plaintext-only".equals(v)) {
                    e.setAttribute("contenteditable", v);
                } else {
                    throw error(DomException.syntax("The value provided ('" + v + "') is not one of 'true', 'false', 'plaintext-only', or 'inherit'."));
                }
                return Js.UNDEFINED;
            }
        });
        accessor(he, "isContentEditable", new NativeFunction.Call() {
            /**
             * Tells whether the element is editable: the nearest ancestor (or itself) that decides.
             * <p>
             * 요소를 편집할 수 있는지 알려 준다: 결정하는 가장 가까운 조상(또는 자신)에 따른다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                // 편집 호스트 안이거나 호스트 자신이면 편집 가능하다(디자인 모드의 문서 전체 포함).
                return Js.bool(org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts.isEditable(element(self)));
            }
        }, null);
        accessor(he, "accessKeyLabel", new NativeFunction.Call() {
            /**
             * Returns the label of the access key: the keyboard shortcuts of the page are not assigned, so it is empty.
             * <p>
             * 접근 키의 레이블을 반환한다: 페이지의 키보드 단축키를 할당하지 않으므로 빈 문자열이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return an empty string / 빈 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.str("");
            }
        }, null);
        accessor(he, "outerText", new NativeFunction.Call() {
            /**
             * Reads the text like <code>innerText</code>.
             * <p>
             * <code>innerText</code> 처럼 텍스트를 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 텍스트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return ((JsObject) self).get("innerText");
            }
        }, new NativeFunction.Call() {
            /**
             * Replaces the element with a text node of the value (a NoModificationAllowedError if it has no parent).
             * <p>
             * 요소를 값의 텍스트 노드로 교체한다(부모가 없으면 NoModificationAllowedError).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                Node parent = e.getParentNode();
                if (parent == null) {
                    throw error(new DomException("NoModificationAllowedError", DomException.NO_MODIFICATION_ALLOWED_ERR, "The element has no parent."));
                }
                String text = Ops.toString(Js.arg(args, 0));
                try {
                    parent.replaceChild(e.document().createTextNode(text), e);
                } catch (DomException ex) {
                    throw error(ex);
                }
                return Js.UNDEFINED;
            }
        });
        // 옛 이름 webkitMatchesSelector 는 matches 와 같은 함수이다.
        JsObject el = bridge.getInterface("Element").getPrototype();
        JsValue matches = el.get("matches");
        if (matches.isObject()) {
            el.defineBuiltin("webkitMatchesSelector", matches);
        }
    }

    /**
     * Defines a token list property (a <code>DOMTokenList</code> of an attribute such as <code>relList</code> of <code>rel</code>): reading
     * returns the live list, always the same object; writing a string sets the attribute (<code>[PutForwards=value]</code>).
     * <p>
     * 토큰 목록 프로퍼티(<code>rel</code> 의 <code>relList</code> 같은 속성의 <code>DOMTokenList</code>)를 정의한다: 읽으면 라이브 목록(항상 같은
     * 객체)을 반환하고, 문자열을 쓰면 속성을 지정한다(<code>[PutForwards=value]</code>).
     *
     * @param proto     the prototype / 프로토타입
     * @param name      the property name / 프로퍼티 이름
     * @param attribute the attribute name / 속성 이름
     */
    private void tokenList(JsObject proto, String name, final String attribute) {
        final DomBridge bridge = page.getBridge();
        accessor(proto, name, new NativeFunction.Call() {
            /**
             * Returns the live token list.
             * <p>
             * 라이브 토큰 목록을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the list / 목록
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(element(self).getTokenList(attribute));
            }
        }, new NativeFunction.Call() {
            /**
             * Sets the attribute to the string.
             * <p>
             * 속성을 문자열로 지정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setAttribute(attribute, Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Defines a read-only accessor that always reads <code>null</code>.
     * <p>
     * 항상 <code>null</code> 을 읽는 읽기 전용 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     */
    private void nullAccessor(JsObject proto, String name) {
        accessor(proto, name, new NativeFunction.Call() {
            /**
             * Returns null.
             * <p>
             * null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return null / null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                return Js.NULL;
            }
        }, null);
    }
}
