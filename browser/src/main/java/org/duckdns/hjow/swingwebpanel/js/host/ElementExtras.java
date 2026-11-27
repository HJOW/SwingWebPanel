package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DatasetObject;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.StyleObject;

/**
 * Members of the HTML element interfaces that need the page environment (plan item P15-03): <code>style</code> and
 * <code>CSSStyleDeclaration</code>, <code>dataset</code>, <code>innerText</code>, <code>click()</code>, <code>focus()</code>,
 * <code>getBoundingClientRect()</code> and the <code>offset*</code>/<code>client*</code> sizes (answered by the layout hook of the
 * environment, zero until layout exists), <code>HTMLTemplateElement.content</code>, the canvas interfaces (installed by the
 * {@link org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasBindings} of the page; WebGL and WebGPU are never available), image state and
 * <code>getComputedStyle</code>.
 * <p>
 * 페이지 환경이 필요한 HTML 요소 인터페이스의 멤버(계획 P15-03): <code>style</code> 과 <code>CSSStyleDeclaration</code>, <code>dataset</code>,
 * <code>innerText</code>, <code>click()</code>, <code>focus()</code>, <code>getBoundingClientRect()</code>, <code>offset*</code>/<code>client*</code>
 * 크기(환경의 레이아웃 훅이 답하며 레이아웃이 생기기 전에는 0), <code>HTMLTemplateElement.content</code>, 캔버스 인터페이스(페이지의
 * {@link org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasBindings} 가 설치한다. WebGL 과 WebGPU 는 절대 제공하지 않는다), 이미지 상태, <code>getComputedStyle</code>.
 */
final class ElementExtras {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The inline style objects of the elements (the same object every time).
     * <p>
     * 요소의 인라인 스타일 객체(매번 같은 객체).
     */
    private final Map<Element, StyleObject> styles = new WeakHashMap<Element, StyleObject>();

    /**
     * The dataset objects of the elements.
     * <p>
     * 요소의 dataset 객체.
     */
    private final Map<Element, DatasetObject> datasets = new WeakHashMap<Element, DatasetObject>();

    /**
     * The prototype of the style declaration objects.
     * <p>
     * 스타일 선언 객체의 프로토타입.
     */
    private JsObject styleProto;

    /**
     * The prototype of the dataset objects.
     * <p>
     * dataset 객체의 프로토타입.
     */
    private JsObject datasetProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    ElementExtras(PageScripts page) {
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
     * Replaces an element's children with a rendered text fragment (HTML §3.2.7).
     * <p>
     * 요소의 자식을 렌더링 텍스트 조각으로 교체한다(HTML §3.2.7).
     *
     * @param target element receiving the new children / 새 자식을 받는 요소
     * @param value value converted to a legacy DOMString / 레거시 DOMString으로 변환할 값
     */
    private static void setInnerText(Element target, JsValue value) {
        String text = value == Js.NULL ? "" : Ops.toString(value);
        Document owner = target.getOwnerDocument();
        DocumentFragment fragment = owner.createDocumentFragment();
        int start = 0;
        // CRLF, CR, LF를 각각 하나의 HTML br로 바꾸고 사이 문자열은 그대로 텍스트로 넣는다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\r' && c != '\n') {
                continue;
            }
            if (start < i) {
                fragment.appendChild(owner.createTextNode(text.substring(start, i)));
            }
            fragment.appendChild(owner.createElementNS(Namespaces.HTML, "br"));
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                i++;
            }
            start = i + 1;
        }
        if (start < text.length()) {
            fragment.appendChild(owner.createTextNode(text.substring(start)));
        }
        target.replaceChildren(fragment);
    }

    /**
     * Returns the style object of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 스타일 객체를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the style object / 스타일 객체
     */
    private static StyleObject style(JsValue self) {
        if (self instanceof StyleObject) {
            return (StyleObject) self;
        }
        throw JsException.typeError("Illegal invocation");
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter / getter
     * @param set   the setter or null / setter 또는 null
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
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the length / 길이
     * @param call   the behaviour / 동작
     */
    private void method(JsObject proto, String name, int length, NativeFunction.Call call) {
        proto.defineData(name, page.getRealm().newFunction(name, length, call), true, true, true);
    }

    /**
     * Builds a <code>DOMRect</code>.
     * <p>
     * <code>DOMRect</code> 객체를 만든다.
     *
     * @param b the bounds x, y, width, height / 경계 x, y, 폭, 높이
     * @return the object / 객체
     */
    private JsValue rect(double[] b) {
        return page.getBridge().wrap(new org.duckdns.hjow.swingwebpanel.dom.DomRect(b[0], b[1], b[2], b[3]));
    }

    /**
     * Tells whether an element is the scrolling element of its document: the root element in standards mode, the <code>body</code> element
     * in quirks mode (CSSOM View, "scrollTop" and "scrollingElement").
     * <p>
     * 요소가 문서의 스크롤 요소인지 알려 준다: 표준 모드에서는 루트 요소, 호환(quirks) 모드에서는 <code>body</code> 요소(CSSOM View 의
     * "scrollTop", "scrollingElement").
     *
     * @param e the element / 요소
     * @return true if it scrolls the viewport / 뷰포트를 스크롤하는 요소이면 true
     */
    static boolean isScrollingElement(Element e) {
        org.duckdns.hjow.swingwebpanel.dom.Document d = e.document();
        boolean quirks = "BackCompat".equals(d.getCompatMode());
        Element root = d.getDocumentElement();
        // 호환 모드에서는 root 의 첫 body 자식이, 표준 모드에서는 root 자신이 스크롤 요소이다.
        if (!quirks) {
            return e == root;
        }
        return e.getParentNode() == root && "body".equals(e.getLocalName());
    }

    /**
     * Returns the document position that <code>scrollIntoView</code> scrolls to on one axis.
     * <p>
     * <code>scrollIntoView</code> 가 한 축에서 스크롤할 문서 위치를 반환한다.
     *
     * @param align    <code>start</code>, <code>center</code>, <code>end</code> or <code>nearest</code> / 정렬 방식
     * @param offset   where the box begins in viewport pixels / 상자가 시작하는 뷰포트 픽셀 위치
     * @param size     the size of the box / 상자의 크기
     * @param viewport the size of the viewport on the axis / 그 축의 뷰포트 크기
     * @param scroll   the current scroll position on the axis / 그 축의 현재 스크롤 위치
     * @return the position to scroll to / 스크롤할 위치
     */
    static double scrollTarget(String align, double offset, double size, double viewport, double scroll) {
        if ("center".equals(align)) {
            return scroll + offset + size / 2 - viewport / 2;
        } else if ("end".equals(align)) {
            return scroll + offset + size - viewport;
        } else if ("nearest".equals(align)) {
            // 이미 보이면 그대로, 위(왼)쪽이 잘렸으면 시작에, 아래(오른)쪽이 잘렸으면 끝에 맞춘다. 뷰포트보다 큰 상자는 시작에 맞춘다.
            if (offset >= 0 && offset + size <= viewport) {
                return scroll;
            }
            if (offset < 0 || size > viewport) {
                return scroll + offset;
            }
            return scroll + offset + size - viewport;
        }
        return scroll + offset;
    }

    /**
     * Installs everything.
     * <p>
     * 모두 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        final HostEnvironment env = page.getEnvironment();
        DomBridge.InterfaceInfo csd = bridge.defineInterface("CSSStyleDeclaration", null, Object.class, new String[0]);
        styleProto = csd.getPrototype();
        installStyleMembers();
        datasetProto = InterfaceNames.define(page, g, "DOMStringMap");
        JsObject he = bridge.getInterface("HTMLElement").getPrototype();
        accessor(he, "style", new NativeFunction.Call() {
            /**
             * Returns the inline style object of the element.
             * <p>
             * 요소의 인라인 스타일 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the style object / 스타일 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                StyleObject s = styles.get(e);
                if (s == null) {
                    s = new StyleObject(bridge, styleProto, e, null);
                    styles.put(e, s);
                }
                return s;
            }
        }, new NativeFunction.Call() {
            /**
             * Assigning to <code>style</code> sets <code>cssText</code>.
             * <p>
             * <code>style</code> 에 대입하면 <code>cssText</code> 를 지정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setAttribute("style", Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        accessor(he, "dataset", new NativeFunction.Call() {
            /**
             * Returns the dataset object of the element.
             * <p>
             * 요소의 dataset 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the dataset / dataset
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                DatasetObject d = datasets.get(e);
                if (d == null) {
                    d = new DatasetObject(datasetProto, e);
                    datasets.put(e, d);
                }
                return d;
            }
        }, null);
        accessor(he, "innerText", new NativeFunction.Call() {
            /**
             * Returns the rendered text of the element, or its DOM text when there is no layout.
             * <p>
             * 요소의 렌더링 텍스트를 반환한다. 배치가 없으면 DOM 텍스트를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 텍스트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element target = element(self);
                String text = page.getEnvironment().getInnerText(target);
                return Js.str(text == null ? target.getTextContent() : text);
            }
        }, new NativeFunction.Call() {
            /**
             * Replaces the children with text and HTML line breaks.
             * <p>
             * 자식을 텍스트와 HTML 줄바꿈으로 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                setInnerText(element(self), Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        method(he, "click", 0, new NativeFunction.Call() {
            /**
             * Dispatches a synthetic click event and follows an uncancelled object download link.
             * <p>
             * 합성 click 이벤트를 전달하고 취소되지 않은 객체 다운로드 링크를 따라간다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                String tag = e.getLocalName();
                // click() 은 마우스 이벤트를 보내며, 비활성 컨트롤에는 아무것도 하지 않는다.
                if (e.hasAttribute("disabled") && ("button".equals(tag) || "input".equals(tag) || "select".equals(tag) || "textarea".equals(tag))) {
                    return Js.UNDEFINED;
                }
                if (e.dispatchEvent(new MouseEvent("click", true, true)) && "a".equals(tag) && e.hasAttribute("download")) {
                    page.getEnvironment().activateObjectDownload(e);
                }
                return Js.UNDEFINED;
            }
        });
        method(he, "focus", 0, new NativeFunction.Call() {
            /**
             * Gives the element the focus if it can have it (see {@link FocusManager}); the events are sent only while the window has the
             * focus, but the element is the focused element of the document either way.
             * <p>
             * 요소가 포커스를 가질 수 있으면 포커스를 준다({@link FocusManager} 참조). 이벤트는 창에 포커스가 있는 동안에만 보내지만 요소는 어느 경우에나
             * 문서의 포커스된 요소가 된다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                FocusManager.focus(e, e.getOwnerDocument().isWindowFocused());
                return Js.UNDEFINED;
            }
        });
        method(he, "blur", 0, new NativeFunction.Call() {
            /**
             * Removes the focus from the element if it has it.
             * <p>
             * 요소가 포커스를 가졌으면 포커스를 거둔다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                FocusManager.blur(e, e.getOwnerDocument().isWindowFocused());
                return Js.UNDEFINED;
            }
        });
        JsObject el = bridge.getInterface("Element").getPrototype();
        method(el, "getBoundingClientRect", 0, new NativeFunction.Call() {
            /**
             * Returns the rectangle of the element in viewport coordinates (from the layout hook).
             * <p>
             * 뷰포트 좌표의 요소 사각형을 반환한다(레이아웃 훅에서).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the rectangle / 사각형
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return rect(env.getElementBounds(element(self)));
            }
        });
        method(el, "getClientRects", 0, new NativeFunction.Call() {
            /**
             * Returns the rectangles of the element as an array.
             * <p>
             * 요소의 사각형을 배열로 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the rectangles / 사각형 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                double[][] boxes = env.getElementRects(element(self));
                // 호스트가 상자별 사각형을 알려 주면 그것으로, 아니면 합집합 사각형 하나로 목록을 만든다.
                java.util.List<org.duckdns.hjow.swingwebpanel.dom.DomRect> list = new java.util.ArrayList<org.duckdns.hjow.swingwebpanel.dom.DomRect>();
                if (boxes == null) {
                    double[] b = env.getElementBounds(element(self));
                    if (b[2] > 0 || b[3] > 0) {
                        list.add(new org.duckdns.hjow.swingwebpanel.dom.DomRect(b[0], b[1], b[2], b[3]));
                    }
                } else {
                    // 상자마다 사각형을 하나씩 더한다.
                    for (int k = 0; k < boxes.length; k++) {
                        list.add(new org.duckdns.hjow.swingwebpanel.dom.DomRect(boxes[k][0], boxes[k][1], boxes[k][2], boxes[k][3]));
                    }
                }
                return page.getBridge().wrap(new org.duckdns.hjow.swingwebpanel.dom.DomRectList(list));
            }
        });
        String[] sizeNames = { "offsetLeft", "offsetTop", "offsetWidth", "offsetHeight", "clientWidth", "clientHeight", "scrollWidth",
            "scrollHeight" };
        final int[] sizeIndex = { 0, 1, 2, 3, 2, 3, 2, 3 };
        // 크기 프로퍼티를 레이아웃 훅의 경계에서 읽는 접근자로 정의한다.
        for (int i = 0; i < sizeNames.length; i++) {
            final int idx = sizeIndex[i];
            accessor(he, sizeNames[i], new NativeFunction.Call() {
                /**
                 * Reads one number of the bounds from the layout hook.
                 * <p>
                 * 레이아웃 훅의 경계에서 숫자 하나를 읽는다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the number / 숫자
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.num(Math.round(env.getElementBounds(element(self))[idx]));
                }
            }, null);
        }
        accessor(el, "scrollTop", new NativeFunction.Call() {
            /**
             * Reads the vertical scroll position: the viewport position for the scrolling element of the document, 0 for the other elements
             * (they are clipped but not scrolled).
             * <p>
             * 세로 스크롤 위치를 읽는다: 문서의 스크롤 요소는 뷰포트 위치, 다른 요소는 0(잘리지만 스크롤되지 않는다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the position / 위치
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(isScrollingElement(element(self)) ? env.getScrollY() : 0);
            }
        }, new NativeFunction.Call() {
            /**
             * Scrolls the viewport for the scrolling element of the document, does nothing for the other elements.
             * <p>
             * 문서의 스크롤 요소이면 뷰포트를 스크롤하고 다른 요소이면 아무것도 하지 않는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (isScrollingElement(element(self))) {
                    double y = Ops.toNumber(Js.arg(args, 0));
                    env.scrollTo(env.getScrollX(), Double.isNaN(y) || Double.isInfinite(y) ? 0 : y);
                }
                return Js.UNDEFINED;
            }
        });
        accessor(el, "scrollLeft", new NativeFunction.Call() {
            /**
             * Reads the horizontal scroll position (see <code>scrollTop</code>).
             * <p>
             * 가로 스크롤 위치를 읽는다(<code>scrollTop</code> 참조).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the position / 위치
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(isScrollingElement(element(self)) ? env.getScrollX() : 0);
            }
        }, new NativeFunction.Call() {
            /**
             * Scrolls the viewport horizontally for the scrolling element of the document.
             * <p>
             * 문서의 스크롤 요소이면 뷰포트를 가로로 스크롤한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (isScrollingElement(element(self))) {
                    double x = Ops.toNumber(Js.arg(args, 0));
                    env.scrollTo(Double.isNaN(x) || Double.isInfinite(x) ? 0 : x, env.getScrollY());
                }
                return Js.UNDEFINED;
            }
        });
        NativeFunction.Call elementScroll = new NativeFunction.Call() {
            /**
             * <code>scroll(x, y)</code> and <code>scrollTo(x, y)</code> of an element: scrolls the viewport if the element is the scrolling
             * element of the document, otherwise nothing (elements are not scrolled).
             * <p>
             * 요소의 <code>scroll(x, y)</code>, <code>scrollTo(x, y)</code>: 요소가 문서의 스크롤 요소이면 뷰포트를 스크롤하고 아니면 아무것도 하지 않는다
             * (요소는 스크롤되지 않는다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (isScrollingElement(element(self))) {
                    JsObject w = page.getRealm().getGlobalObject();
                    ((JsObject) w.get("scrollTo")).call(w, args);
                }
                return Js.UNDEFINED;
            }
        };
        method(el, "scroll", 2, elementScroll);
        method(el, "scrollTo", 2, elementScroll);
        method(el, "scrollBy", 2, new NativeFunction.Call() {
            /**
             * <code>scrollBy(dx, dy)</code> of an element (see <code>scrollTo</code>).
             * <p>
             * 요소의 <code>scrollBy(dx, dy)</code>(<code>scrollTo</code> 참조).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (isScrollingElement(element(self))) {
                    JsObject w = page.getRealm().getGlobalObject();
                    ((JsObject) w.get("scrollBy")).call(w, args);
                }
                return Js.UNDEFINED;
            }
        });
        method(el, "scrollIntoView", 0, new NativeFunction.Call() {
            /**
             * Scrolls the viewport so that the element is visible: <code>scrollIntoView(true)</code> (the default) puts its top at the top of
             * the viewport, <code>false</code> puts its bottom at the bottom, and an options object chooses <code>block</code> and
             * <code>inline</code> as <code>start</code>, <code>center</code>, <code>end</code> or <code>nearest</code>. The viewport is the
             * only scrolling box, so the scrolling of ancestors is not done.
             * <p>
             * 요소가 보이도록 뷰포트를 스크롤한다: <code>scrollIntoView(true)</code>(기본)는 요소의 위쪽을 뷰포트 맨 위에, <code>false</code> 는 아래쪽을
             * 맨 아래에 두고, 옵션 객체는 <code>block</code> 과 <code>inline</code> 을 <code>start</code>, <code>center</code>, <code>end</code>,
             * <code>nearest</code> 로 고른다. 스크롤하는 상자는 뷰포트뿐이므로 조상의 스크롤은 하지 않는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                String block = "start";
                String inline = "nearest";
                JsValue a = Js.arg(args, 0);
                // 불리언이면 true=start, false=end, 객체이면 block/inline 멤버를 읽는다.
                if (a.isObject()) {
                    JsValue b = ((JsObject) a).get("block");
                    JsValue i = ((JsObject) a).get("inline");
                    block = b.isUndefined() ? "start" : Ops.toString(b);
                    inline = i.isUndefined() ? "nearest" : Ops.toString(i);
                } else if (!a.isUndefined()) {
                    block = Ops.toBoolean(a) ? "start" : "end";
                }
                double[] b = env.getElementBounds(e);
                double x = scrollTarget(inline, b[0], b[2], env.getViewportWidth(), env.getScrollX());
                double y = scrollTarget(block, b[1], b[3], env.getViewportHeight(), env.getScrollY());
                env.scrollTo(x, y);
                return Js.UNDEFINED;
            }
        });
        accessor(bridge.getInterface("HTMLTemplateElement").getPrototype(), "content", new NativeFunction.Call() {
            /**
             * Returns the template contents (a document fragment).
             * <p>
             * 템플릿 내용(문서 조각)을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the fragment / 조각
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DocumentFragment f = ParserNodeData.ensureTemplateContents(element(self));
                return bridge.toJs(f);
            }
        }, null);
        // 캔버스 요소와 2D 컨텍스트(getContext, toDataURL, ImageData 등)는 canvas 패키지가 설치한다. WebGL 과 WebGPU 는 null 이다.
        page.canvases().install(g);
        installImage(g);
        new MediaBindings(page, page.media()).install(g);
        new TextTrackBindings(page, page.media()).install(g);
        textAccessor(bridge.getInterface("HTMLAnchorElement").getPrototype());
        textAccessor(bridge.getInterface("HTMLScriptElement").getPrototype());
        scriptAsyncAccessor(bridge.getInterface("HTMLScriptElement").getPrototype());
        method(g, "getComputedStyle", 1, new NativeFunction.Call() {
            /**
             * Returns the computed style of an element as a read-only declaration.
             * <p>
             * 요소의 계산 스타일을 읽기 전용 선언으로 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the style declaration / 스타일 선언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = (Element) DomBridge.targetOf(Js.arg(args, 0), Element.class);
                if (e == null) {
                    throw JsException.typeError("Failed to execute 'getComputedStyle' on 'Window': parameter 1 is not of type 'Element'.");
                }
                String pseudo = pseudoElement(Js.arg(args, 1));
                // 올바른 의사 요소가 아니면 규격대로 빈 선언 객체를 돌려준다.
                if ("".equals(pseudo) && !Js.arg(args, 1).isNullOrUndefined()) {
                    return new StyleObject(bridge, styleProto, new org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration());
                }
                StyleResolver sr = env.getStyleResolver();
                // 다른 문서(프레임이나 다른 창의 문서)의 요소는 이 창의 스타일 엔진이 알지 못한다. 예외를 내지 않고 초기값을 돌려준다.
                // 그 요소의 계산 스타일은 그 문서의 창에서 getComputedStyle 을 불러야 얻는다.
                if (sr != null && e.getOwnerDocument() != page.getDocument()) {
                    sr = null;
                }
                ComputedStyle cs = sr == null ? ComputedStyle.initial()
                        : pseudo == null || pseudo.length() == 0 ? sr.getComputedStyle(e) : sr.getPseudoComputedStyle(e, pseudo);
                return new StyleObject(bridge, styleProto, null, cs);
            }
        });
    }

    /**
     * Converts the property name argument of a <code>CSSStyleDeclaration</code> method: the names of properties are case insensitive (lower case),
     * the names of custom properties (<code>--x</code>) are case sensitive.
     * <p>
     * <code>CSSStyleDeclaration</code> 메서드의 프로퍼티 이름 인수를 변환한다: 프로퍼티 이름은 대소문자를 구분하지 않고(소문자) 사용자 정의 프로퍼티(<code>--x</code>)
     * 이름은 구분한다.
     *
     * @param v the argument / 인수
     * @return the name to look up / 조회할 이름
     */
    private static String propertyName(JsValue v) {
        String n = Ops.toString(v);
        return n.startsWith("--") ? n : n.toLowerCase(java.util.Locale.ENGLISH);
    }

    /**
     * Converts a value or priority argument of <code>setProperty</code> (<code>[LegacyNullToEmptyString]</code>): null and a missing (undefined)
     * argument are the empty string.
     * <p>
     * <code>setProperty</code> 의 값이나 우선순위 인수를 변환한다(<code>[LegacyNullToEmptyString]</code>): null 과 없는(undefined) 인수는 빈 문자열이다.
     *
     * @param v the argument / 인수
     * @return the text / 텍스트
     */
    private static String valueText(JsValue v) {
        return v.isNullOrUndefined() ? "" : Ops.toString(v);
    }

    /**
     * Reads the second argument of <code>getComputedStyle</code> (CSSOM section 7.1): null if it is missing, null, undefined, empty or does not start
     * with a colon (the style of the element itself), the name of the pseudo-element without colons if it names one that this engine knows, and the
     * empty string if it starts with a colon but is not a pseudo-element selector (the result is an empty declaration then).
     * <p>
     * <code>getComputedStyle</code> 의 두 번째 인수를 읽는다(CSSOM 7.1 절): 없거나 null, undefined, 빈 문자열이거나 콜론으로 시작하지 않으면 null(요소 자신의 스타일),
     * 이 엔진이 아는 의사 요소를 가리키면 콜론 없는 의사 요소 이름, 콜론으로 시작하지만 의사 요소 선택자가 아니면 빈 문자열(그러면 빈 선언을 돌려준다).
     *
     * @param v the argument / 인수
     * @return null, the pseudo-element name or "" / null, 의사 요소 이름 또는 ""
     */
    static String pseudoElement(JsValue v) {
        if (v.isNullOrUndefined()) {
            return null;
        }
        String s = Ops.toString(v).trim().toLowerCase(java.util.Locale.ENGLISH);
        if (!s.startsWith(":")) {
            return null;
        }
        String name = s.startsWith("::") ? s.substring(2) : s.substring(1);
        boolean legacy = name.equals("before") || name.equals("after") || name.equals("first-line") || name.equals("first-letter");
        // 한 콜론 형태는 옛 네 가지만 허용한다.
        if (!s.startsWith("::") && !legacy) {
            return "";
        }
        String[] known = { "before", "after", "first-line", "first-letter", "marker", "selection", "placeholder", "backdrop", "file-selector-button" };
        // 알려진 의사 요소 이름과 비교한다.
        for (int i = 0; i < known.length; i++) {
            if (known[i].equals(name)) {
                return name;
            }
        }
        return "";
    }

    /**
     * Defines the <code>text</code> accessor (the text content) on an interface prototype.
     * <p>
     * 인터페이스 프로토타입에 <code>text</code> 접근자(텍스트 콘텐츠)를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void textAccessor(JsObject proto) {
        accessor(proto, "text", new NativeFunction.Call() {
            /**
             * Returns the text content.
             * <p>
             * 텍스트 콘텐츠를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 텍스트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(element(self).getTextContent());
            }
        }, new NativeFunction.Call() {
            /**
             * Replaces the text content.
             * <p>
             * 텍스트 콘텐츠를 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setTextContent(Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Defines the <code>async</code> accessor of the script element: it is true while the <code>async</code> attribute exists or the script
     * is "force async" (a script that a script created); assigning a value clears "force async" and sets or removes the attribute, which is
     * how a page asks for in-order execution with <code>script.async = false</code>.
     * <p>
     * script 요소의 <code>async</code> 접근자를 정의한다. <code>async</code> 속성이 있거나 스크립트가 "force async"(스크립트가 만든 스크립트)이면
     * true 이다. 값을 지정하면 "force async" 를 지우고 속성을 만들거나 지우며, 페이지가 <code>script.async = false</code> 로 순서 있는 실행을
     * 요청하는 방법이다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void scriptAsyncAccessor(JsObject proto) {
        accessor(proto, "async", new NativeFunction.Call() {
            /**
             * Returns whether the script is asynchronous.
             * <p>
             * 스크립트가 비동기인지 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return a boolean / 불리언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                return Js.bool(e.hasAttribute("async") || ParserNodeData.scriptFlags(e).isForceAsync());
            }
        }, new NativeFunction.Call() {
            /**
             * Sets or clears the asynchronous behavior.
             * <p>
             * 비동기 동작을 지정하거나 지운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                ParserNodeData.scriptFlags(e).setForceAsync(false);
                if (Ops.toBoolean(Js.arg(args, 0))) {
                    e.setAttribute("async", "");
                } else {
                    e.removeAttribute("async");
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * The script that makes the promise of <code>HTMLImageElement.decode()</code>; it is compiled at the first call, inside a page task.
     * <p>
     * <code>HTMLImageElement.decode()</code> 의 프라미스를 만드는 스크립트. 첫 호출 때 페이지 작업 안에서 컴파일한다.
     */
    private static final String DECODE_SCRIPT = "(function (img) { return new Promise(function (resolve, reject) {"
            + " function fail() { reject(new DOMException('The source image cannot be decoded.', 'EncodingError')); }"
            + " if (img.complete) { if (img.naturalWidth > 0) { resolve(); } else { fail(); } return; }"
            + " function off() { img.removeEventListener('load', onload); img.removeEventListener('error', onerror); }"
            + " function onload() { off(); resolve(); } function onerror() { off(); fail(); }"
            + " img.addEventListener('load', onload); img.addEventListener('error', onerror); }); })";

    /**
     * The compiled decode helper, or null before the first call.
     * <p>
     * 컴파일된 decode 도우미. 첫 호출 전에는 null.
     */
    private JsValue decodeHelper;

    /**
     * Installs the members of <code>HTMLImageElement</code> that depend on the image loading of the page: <code>complete</code>,
     * <code>naturalWidth</code>, <code>naturalHeight</code>, <code>currentSrc</code>, <code>decode()</code>, and the <code>Image</code>
     * constructor.
     * <p>
     * 페이지의 이미지 로딩에 따라 달라지는 <code>HTMLImageElement</code> 의 멤버를 설치한다: <code>complete</code>, <code>naturalWidth</code>,
     * <code>naturalHeight</code>, <code>currentSrc</code>, <code>decode()</code>, 그리고 <code>Image</code> 생성자.
     *
     * @param g the global object / 전역 객체
     */
    private void installImage(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        final JsObject img = bridge.getInterface("HTMLImageElement").getPrototype();
        accessor(img, "complete", new NativeFunction.Call() {
            /**
             * An image without a source is complete; others are complete when their load ended.
             * <p>
             * 소스가 없는 이미지는 완료이고, 나머지는 로드가 끝나면 완료이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the flag / 플래그
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(page.images().isComplete(element(self)));
            }
        }, null);
        accessor(img, "naturalWidth", new NativeFunction.Call() {
            /**
             * The natural width of the loaded image in CSS px, 0 if it is not available.
             * <p>
             * 로드한 이미지의 자연 너비(CSS px). 이용할 수 없으면 0.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width / 너비
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(page.images().naturalWidth(element(self)));
            }
        }, null);
        accessor(img, "naturalHeight", new NativeFunction.Call() {
            /**
             * The natural height of the loaded image in CSS px, 0 if it is not available.
             * <p>
             * 로드한 이미지의 자연 높이(CSS px). 이용할 수 없으면 0.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the height / 높이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(page.images().naturalHeight(element(self)));
            }
        }, null);
        accessor(img, "currentSrc", new NativeFunction.Call() {
            /**
             * The absolute URL of the source that was chosen from <code>src</code>, <code>srcset</code> and <code>picture</code>.
             * <p>
             * <code>src</code>, <code>srcset</code>, <code>picture</code> 에서 고른 소스의 절대 URL.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(page.images().currentSrc(element(self)));
            }
        }, null);
        method(img, "decode", 0, new NativeFunction.Call() {
            /**
             * Returns a promise that settles when the image is available (fulfilled) or failed (rejected with an EncodingError).
             * <p>
             * 이미지를 쓸 수 있게 되면 이행되고 실패하면 EncodingError 로 거부되는 프라미스를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                // 도우미 함수는 첫 호출 때 한 번만 컴파일한다.
                if (decodeHelper == null) {
                    decodeHelper = r.evaluateScript(DECODE_SCRIPT, "image-decode");
                }
                return Ops.call(decodeHelper, Js.UNDEFINED, new JsValue[] { self });
            }
        });
        final JsObject proto = img;
        NativeFunction ctor = new NativeFunction(realm, "Image", 0, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'Image': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an <code>img</code> element with the optional width and height.
             * <p>
             * 선택적인 너비와 높이를 가진 <code>img</code> 요소를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the element / 요소
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Element e = page.getDocument().createElement("img");
                // 인수로 너비와 높이가 오면 속성으로 반영한다.
                if (args.length > 0 && args[0] != Js.UNDEFINED) {
                    e.setAttribute("width", String.valueOf((long) Ops.toNumber(args[0])));
                }
                if (args.length > 1 && args[1] != Js.UNDEFINED) {
                    e.setAttribute("height", String.valueOf((long) Ops.toNumber(args[1])));
                }
                return (JsObject) bridge.toJs(e);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        g.defineBuiltin("Image", ctor);
    }

    /**
     * Installs the members of the <code>CSSStyleDeclaration</code> prototype.
     * <p>
     * <code>CSSStyleDeclaration</code> 프로토타입의 멤버를 설치한다.
     */
    private void installStyleMembers() {
        accessor(styleProto, "cssText", new NativeFunction.Call() {
            /**
             * Returns the serialized declarations.
             * <p>
             * 직렬화한 선언을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 텍스트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(style(self).getCssText());
            }
        }, new NativeFunction.Call() {
            /**
             * Replaces the declarations.
             * <p>
             * 선언을 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                style(self).setCssText(Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        accessor(styleProto, "length", new NativeFunction.Call() {
            /**
             * Returns the number of declarations.
             * <p>
             * 선언 수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the count / 개수
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(style(self).getLength());
            }
        }, null);
        method(styleProto, "item", 1, new NativeFunction.Call() {
            /**
             * Returns the property name at an index.
             * <p>
             * 위치의 프로퍼티 이름을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the name / 이름
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(style(self).item((int) Ops.toInteger(Js.arg(args, 0))));
            }
        });
        method(styleProto, "getPropertyValue", 1, new NativeFunction.Call() {
            /**
             * Returns the value of a property.
             * <p>
             * 프로퍼티의 값을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(style(self).getPropertyValue(propertyName(Js.arg(args, 0))));
            }
        });
        method(styleProto, "getPropertyPriority", 1, new NativeFunction.Call() {
            /**
             * Returns the priority of a property.
             * <p>
             * 프로퍼티의 우선순위를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return important or "" / important 또는 ""
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(style(self).getPropertyPriority(propertyName(Js.arg(args, 0))));
            }
        });
        method(styleProto, "setProperty", 2, new NativeFunction.Call() {
            /**
             * Sets a property (an empty value removes it).
             * <p>
             * 프로퍼티를 지정한다(빈 값은 제거).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'setProperty' on 'CSSStyleDeclaration': 2 arguments required, but only " + args.length
                            + " present.");
                }
                style(self).setProperty(propertyName(Js.arg(args, 0)), valueText(Js.arg(args, 1)), valueText(Js.arg(args, 2)));
                return Js.UNDEFINED;
            }
        });
        method(styleProto, "removeProperty", 1, new NativeFunction.Call() {
            /**
             * Removes a property and returns its old value.
             * <p>
             * 프로퍼티를 제거하고 이전 값을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the old value / 이전 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(style(self).removeProperty(propertyName(Js.arg(args, 0))));
            }
        });
    }
}
