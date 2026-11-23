package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The members of <code>Document</code> that are computed from the tree (<code>body</code>, <code>head</code>, <code>title</code>, the
 * <code>forms</code>, <code>images</code>, <code>links</code> and <code>scripts</code> collections ...). Members that depend on the page
 * host (<code>cookie</code>, <code>location</code>, <code>readyState</code>, <code>write</code> ...) are added by the host package.
 * <p>
 * 트리에서 계산하는 <code>Document</code> 의 멤버(<code>body</code>, <code>head</code>, <code>title</code>, <code>forms</code>, <code>images</code>,
 * <code>links</code>, <code>scripts</code> 컬렉션 등). 페이지 호스트에 의존하는 멤버(<code>cookie</code>, <code>location</code>,
 * <code>readyState</code>, <code>write</code> 등)는 호스트 패키지가 더한다.
 */
public final class DocumentExtras {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DocumentExtras() {
    }

    /**
     * Returns the document element's first child element with one of two local names, or null.
     * <p>
     * 문서 요소의 자식 요소 중 지역 이름이 두 이름 중 하나인 첫 요소를 반환한다. 없으면 null.
     *
     * @param doc  the document / 문서
     * @param a    the first name / 첫 이름
     * @param b    the second name (may be null) / 둘째 이름(null 가능)
     * @return the element or null / 요소 또는 null
     */
    private static Element child(Document doc, String a, String b) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            return null;
        }
        // 문서 요소의 자식 요소를 앞에서부터 보며 이름이 맞는 첫 요소를 찾는다.
        for (Element c = root.getFirstElementChild(); c != null; c = c.getNextElementSibling()) {
            String n = c.getLocalName();
            if (n.equals(a) || (b != null && n.equals(b))) {
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the <code>document.title</code> value: the text of the first <code>title</code> element with white space collapsed.
     * <p>
     * <code>document.title</code> 값을 반환한다: 첫 <code>title</code> 요소의 텍스트이며 공백을 정리한다.
     *
     * @param doc the document / 문서
     * @return the title / 제목
     */
    public static String titleOf(Document doc) {
        Element t = doc.getElementsByTagName("title").item(0);
        if (t == null) {
            return "";
        }
        String text = t.getTextContent();
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        // 연속된 공백 문자를 하나로 줄이고 앞뒤 공백을 없앤다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                space = sb.length() > 0;
            } else {
                if (space) {
                    sb.append(' ');
                    space = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the property name / 프로퍼티 이름
     * @param get    the getter / getter
     * @param set    the setter or null / setter 또는 null
     */
    private static void accessor(DomBridge bridge, JsObject proto, String name, NativeFunction.Call get, NativeFunction.Call set) {
        JsRealm realm = bridge.getRealm();
        proto.defineAccessor(name, realm.newFunction("get " + name, 0, get), set == null ? null : realm.newFunction("set " + name, 1, set),
                true, true);
    }

    /**
     * Returns the document of the receiver or throws the standard TypeError.
     * <p>
     * 수신자의 문서를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the document / 문서
     */
    private static Document doc(JsValue self) {
        Document d = (Document) DomBridge.targetOf(self, Document.class);
        if (d == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return d;
    }

    /**
     * Installs the tree-derived members on the <code>Document</code> prototype.
     * <p>
     * 트리에서 계산하는 멤버를 <code>Document</code> 프로토타입에 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        JsObject proto = bridge.getInterface("Document").getPrototype();
        accessor(bridge, proto, "body", new NativeFunction.Call() {
            /**
             * Returns the body or frameset element.
             * <p>
             * body 또는 frameset 요소를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(child(doc(self), "body", "frameset"));
            }
        }, new NativeFunction.Call() {
            /**
             * Replaces the body element.
             * <p>
             * body 요소를 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Document d = doc(self);
                Element nb = (Element) DomBridge.targetOf(Js.arg(args, 0), Element.class);
                if (nb == null || !(nb.getLocalName().equals("body") || nb.getLocalName().equals("frameset"))) {
                    throw JsException.typeError("The new body must be a body or frameset element.");
                }
                Element old = child(d, "body", "frameset");
                try {
                    if (old == null) {
                        if (d.getDocumentElement() == null) {
                            throw bridge.domError(org.duckdns.hjow.swingwebpanel.dom.DomException.hierarchyRequest("No document element"));
                        }
                        d.getDocumentElement().appendChild(nb);
                    } else if (old != nb) {
                        old.getParentNode().replaceChild(nb, old);
                    }
                } catch (org.duckdns.hjow.swingwebpanel.dom.DomException e) {
                    throw bridge.domError(e);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(bridge, proto, "head", new NativeFunction.Call() {
            /**
             * Returns the head element.
             * <p>
             * head 요소를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(child(doc(self), "head", null));
            }
        }, null);
        accessor(bridge, proto, "title", new NativeFunction.Call() {
            /**
             * Returns the document title.
             * <p>
             * 문서 제목을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the title / 제목
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(titleOf(doc(self)));
            }
        }, new NativeFunction.Call() {
            /**
             * Sets the document title, creating the title element in the head when needed.
             * <p>
             * 문서 제목을 지정한다. 필요하면 head 에 title 요소를 만든다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Document d = doc(self);
                String text = Ops.toString(Js.arg(args, 0));
                Element t = d.getElementsByTagName("title").item(0);
                if (t == null) {
                    Element head = child(d, "head", null);
                    if (head == null) {
                        return Js.UNDEFINED;
                    }
                    t = d.createElement("title");
                    head.appendChild(t);
                }
                t.setTextContent(text);
                return Js.UNDEFINED;
            }
        });
        collection(bridge, proto, "forms", "form");
        collection(bridge, proto, "images", "img");
        accessor(bridge, proto, "links", new NativeFunction.Call() {
            /**
             * Returns the live collection of the hyperlinks: the <code>a</code> and <code>area</code> elements with an <code>href</code>.
             * <p>
             * 하이퍼링크의 라이브 컬렉션을 반환한다: <code>href</code> 가 있는 <code>a</code> 와 <code>area</code> 요소.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(ElementCollections.links(doc(self)));
            }
        }, null);
        collection(bridge, proto, "scripts", "script");
        collection(bridge, proto, "embeds", "embed");
        accessor(bridge, proto, "children", new NativeFunction.Call() {
            /**
             * Returns the child elements of the document.
             * <p>
             * 문서의 자식 요소를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(ElementCollections.children(doc(self)));
            }
        }, null);
        accessor(bridge, proto, "all", new NativeFunction.Call() {
            /**
             * Returns every element of the document (<code>document.all</code>).
             * <p>
             * 문서의 모든 요소를 반환한다(<code>document.all</code>).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(ElementCollections.byTagName(doc(self), "*"));
            }
        }, null);
        accessor(bridge, proto, "activeElement", new NativeFunction.Call() {
            /**
             * Returns the focused element, or the body if no element has the focus.
             * <p>
             * 포커스된 요소를 반환한다. 포커스를 가진 요소가 없으면 body.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element focused = FocusManager.getFocused(doc(self));
                return bridge.toJs(focused != null ? focused : child(doc(self), "body", "frameset"));
            }
        }, null);
        proto.defineBuiltin("hasFocus", bridge.getRealm().newFunction("hasFocus", 0, new NativeFunction.Call() {
            /**
             * Tells whether the document has the focus: its window (the component) has it.
             * <p>
             * 문서에 포커스가 있는지 알려 준다: 창(컴포넌트)에 포커스가 있다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return a boolean / 불리언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(doc(self).isWindowFocused());
            }
        }));
        // document.links 는 href 가 있는 a 와 area 이다(HTML5 2014 REC 3.1.4). anchors 는 name 이 있는 a, plugins 는 embeds 와 같은 목록이다.
        accessor(bridge, proto, "anchors", new NativeFunction.Call() {
            /**
             * Returns the live collection of the named anchors (<code>document.anchors</code>).
             * <p>
             * 이름 있는 앵커의 라이브 컬렉션을 반환한다(<code>document.anchors</code>).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(ElementCollections.anchors(doc(self)));
            }
        }, null);
        collection(bridge, proto, "plugins", "embed");
        proto.defineBuiltin("getElementsByName", bridge.getRealm().newFunction("getElementsByName", 1, new NativeFunction.Call() {
            /**
             * Returns the live list of the HTML elements with a <code>name</code> attribute value.
             * <p>
             * <code>name</code> 속성 값이 같은 HTML 요소의 라이브 목록을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the list / 목록
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'getElementsByName' on 'Document': 1 argument required, but only 0 present.");
                }
                return bridge.toJs(ElementCollections.byNameAttribute(doc(self), Ops.toString(args[0])));
            }
        }));
        accessor(bridge, proto, "lastModified", new NativeFunction.Call() {
            /**
             * Returns the time of the last modification as <code>MM/DD/YYYY hh:mm:ss</code> in the local time. The component does not keep the
             * <code>Last-Modified</code> header of the document, so this is the current time, which is the fallback of the specification.
             * <p>
             * 마지막 수정 시각을 지역 시간의 <code>MM/DD/YYYY hh:mm:ss</code> 로 반환한다. 컴포넌트는 문서의 <code>Last-Modified</code> 헤더를 보관하지 않으므로
             * 규격의 대체값인 현재 시각이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss", java.util.Locale.ENGLISH).format(new java.util.Date()));
            }
        }, null);
        accessor(bridge, proto, "designMode", new NativeFunction.Call() {
            /**
             * Returns <code>"on"</code> while the whole document is editable, else <code>"off"</code>.
             * <p>
             * 문서 전체가 편집 가능한 동안 <code>"on"</code>, 아니면 <code>"off"</code> 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return "on" or "off" / "on" 또는 "off"
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(doc(self).isDesignMode() ? "on" : "off");
            }
        }, new NativeFunction.Call() {
            /**
             * Switches design mode: a value that is <code>on</code> (any letter case) switches it on and puts the caret at the start of the
             * document; <code>off</code> switches it off; another value is ignored (HTML Standard).
             * <p>
             * 디자인 모드를 바꾼다: <code>on</code>(대소문자 무관)이면 켜고 캐럿을 문서의 시작에 두며, <code>off</code> 이면 끄고, 다른 값은 무시한다(HTML Standard).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String v = Ops.toString(Js.arg(args, 0)).toLowerCase(java.util.Locale.ENGLISH);
                if ("on".equals(v)) {
                    doc(self).setDesignMode(true);
                } else if ("off".equals(v)) {
                    doc(self).setDesignMode(false);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(bridge, proto, "dir", new NativeFunction.Call() {
            /**
             * Returns the <code>dir</code> of the root element when it is <code>ltr</code> or <code>rtl</code>, else the empty string.
             * <p>
             * 루트 요소의 <code>dir</code> 이 <code>ltr</code> 이나 <code>rtl</code> 이면 그 값을, 아니면 빈 문자열을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the direction / 방향
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element root = doc(self).getDocumentElement();
                String v = root == null || root.getAttribute("dir") == null ? "" : root.getAttribute("dir").trim().toLowerCase();
                return Js.str("ltr".equals(v) || "rtl".equals(v) ? v : "");
            }
        }, new NativeFunction.Call() {
            /**
             * Sets the <code>dir</code> attribute of the root element.
             * <p>
             * 루트 요소의 <code>dir</code> 속성을 지정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element root = doc(self).getDocumentElement();
                if (root != null) {
                    root.setAttribute("dir", Ops.toString(Js.arg(args, 0)));
                }
                return Js.UNDEFINED;
            }
        });
        // 편집 명령: 문서의 편집기(Editor)가 실행하고 질의에 답한다.
        proto.defineBuiltin("queryCommandSupported", bridge.getRealm().newFunction("queryCommandSupported", 1, new NativeFunction.Call() {
            /**
             * Tells whether the editing command exists.
             * <p>
             * 편집 명령이 있는지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the answer / 답
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(doc(self).getEditor().isCommandSupported(Ops.toString(Js.arg(args, 0))));
            }
        }));
        proto.defineBuiltin("queryCommandEnabled", bridge.getRealm().newFunction("queryCommandEnabled", 1, new NativeFunction.Call() {
            /**
             * Tells whether the editing command can run now.
             * <p>
             * 편집 명령이 지금 실행될 수 있는지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the answer / 답
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(doc(self).getEditor().isCommandEnabled(Ops.toString(Js.arg(args, 0))));
            }
        }));
        proto.defineBuiltin("queryCommandState", bridge.getRealm().newFunction("queryCommandState", 1, new NativeFunction.Call() {
            /**
             * Tells whether the editing command is in effect for the selection (a format that applies).
             * <p>
             * 편집 명령이 선택에 효력이 있는지(적용되는 서식) 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the answer / 답
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(doc(self).getEditor().queryCommandState(Ops.toString(Js.arg(args, 0))));
            }
        }));
        proto.defineBuiltin("queryCommandIndeterm", bridge.getRealm().newFunction("queryCommandIndeterm", 1, new NativeFunction.Call() {
            /**
             * Answers false: the editor has no commands with an indeterminate state.
             * <p>
             * false 로 답한다: 편집기에는 불확정 상태를 가진 명령이 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false / false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(false);
            }
        }));
        proto.defineBuiltin("queryCommandValue", bridge.getRealm().newFunction("queryCommandValue", 1, new NativeFunction.Call() {
            /**
             * Returns the value of the editing command for the selection.
             * <p>
             * 선택에 대한 편집 명령의 값을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(doc(self).getEditor().queryCommandValue(Ops.toString(Js.arg(args, 0))));
            }
        }));
        proto.defineBuiltin("execCommand", bridge.getRealm().newFunction("execCommand", 1, new NativeFunction.Call() {
            /**
             * Runs the editing command on the selection and tells whether it ran.
             * <p>
             * 선택에 편집 명령을 실행하고 실행했는지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the command name, the user-interface flag (ignored), the value / 인수: 명령 이름, UI 플래그(무시), 값
             * @return true if the command ran / 명령을 실행했으면 true
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'execCommand' on 'Document': 1 argument required, but only 0 present.");
                }
                String value = args.length > 2 && !args[2].isUndefined() ? Ops.toString(args[2]) : null;
                return Js.bool(doc(self).getEditor().execCommand(Ops.toString(args[0]), value));
            }
        }));
        JsObject elProto = bridge.getInterface("Element").getPrototype();
        accessor(bridge, elProto, "baseURI", new NativeFunction.Call() {
            /**
             * Returns the base URL of the element's document.
             * <p>
             * 요소가 속한 문서의 기준 URL 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Node n = (Node) DomBridge.targetOf(self, Node.class);
                if (n == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return Js.str(n.document().getBaseUri());
            }
        }, null);
    }

    /**
     * Defines a live collection accessor that lists the elements with a tag name.
     * <p>
     * 태그 이름이 같은 요소를 나열하는 살아 있는 컬렉션 접근자를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the property name / 프로퍼티 이름
     * @param tag    the tag name / 태그 이름
     */
    private static void collection(final DomBridge bridge, JsObject proto, String name, final String tag) {
        accessor(bridge, proto, name, new NativeFunction.Call() {
            /**
             * Returns the live collection (the same object every time).
             * <p>
             * 살아 있는 컬렉션을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(ElementCollections.byTagName(doc(self), tag));
            }
        }, null);
    }
}
