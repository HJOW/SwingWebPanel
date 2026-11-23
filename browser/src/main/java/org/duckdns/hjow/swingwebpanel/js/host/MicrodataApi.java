package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Microdata;
import org.duckdns.hjow.swingwebpanel.dom.PropertiesCollection;
import org.duckdns.hjow.swingwebpanel.dom.PropertyNodeList;
import org.duckdns.hjow.swingwebpanel.dom.StaticNodeList;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.IndexedObject;

/**
 * The script API of microdata (W3C Working Group Note "HTML Microdata", 29 October 2013, section 6; the model is {@link Microdata}):
 * <code>document.getItems([types])</code>, and on every HTML element <code>itemScope</code>, <code>itemType</code>, <code>itemId</code>,
 * <code>itemProp</code>, <code>itemRef</code>, <code>properties</code> (an <code>HTMLPropertiesCollection</code> with <code>names</code> and
 * <code>namedItem()</code>, whose named items are <code>PropertyNodeList</code> objects with <code>getValues()</code>) and
 * <code>itemValue</code>. Differences from the Note: <code>getItems()</code> returns a static <code>NodeList</code> (the Note asks for a live
 * one), and <code>properties.names</code> is a new array at every read (the Note asks for the same live array).
 * <p>
 * 마이크로데이터의 스크립트 API(W3C 작업 그룹 노트 "HTML Microdata", 2013-10-29, 6절. 모델은 {@link Microdata}): <code>document.getItems([types])</code>,
 * 그리고 모든 HTML 요소의 <code>itemScope</code>, <code>itemType</code>, <code>itemId</code>, <code>itemProp</code>, <code>itemRef</code>,
 * <code>properties</code>(<code>names</code> 와 <code>namedItem()</code> 이 있는 <code>HTMLPropertiesCollection</code>. 이름으로 얻는 것은
 * <code>getValues()</code> 가 있는 <code>PropertyNodeList</code>), <code>itemValue</code>. 노트와 다른 점: <code>getItems()</code> 는 정적
 * <code>NodeList</code> 를 반환하고(노트는 라이브를 요구), <code>properties.names</code> 는 읽을 때마다 새 배열이다(노트는 같은 라이브 배열을 요구).
 */
final class MicrodataApi {

    /**
     * The page whose realm gets the members.
     * <p>
     * 멤버를 받는 realm 이 속한 페이지.
     */
    private final PageScripts page;

    /**
     * The <code>properties</code> collection of every element that asked for it (so the same object is returned every time).
     * <p>
     * <code>properties</code> 를 요청한 요소마다의 컬렉션(매번 같은 객체를 돌려주기 위함).
     */
    private final Map<Element, PropertiesCollection> collections = new WeakHashMap<Element, PropertiesCollection>();

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    MicrodataApi(PageScripts page) {
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
            throw org.duckdns.hjow.swingwebpanel.js.JsException.typeError("Illegal invocation");
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
     * Installs the interfaces and the members.
     * <p>
     * 인터페이스와 멤버를 설치한다.
     */
    void install() {
        final DomBridge bridge = page.getBridge();
        bridge.defineInterface("HTMLPropertiesCollection", "HTMLCollection", PropertiesCollection.class, new String[] { "a:names",
            "m:namedItem(String)@getNamed" });
        bridge.setIndexed("HTMLPropertiesCollection", new IndexedObject.Source() {
            /**
             * Returns the number of properties.
             * <p>
             * 프로퍼티 수를 반환한다.
             *
             * @param t the collection / 컬렉션
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((PropertiesCollection) t).getLength();
            }

            /**
             * Returns the property element at an index.
             * <p>
             * 위치의 프로퍼티 요소를 반환한다.
             *
             * @param t the collection / 컬렉션
             * @param i the index / 위치
             * @return the element / 요소
             */
            public Object item(Object t, int i) {
                return ((PropertiesCollection) t).item(i);
            }

            /**
             * Returns the list of the elements of a property name.
             * <p>
             * 프로퍼티 이름의 요소 목록을 반환한다.
             *
             * @param t the collection / 컬렉션
             * @param n the name / 이름
             * @return the list or null / 목록 또는 null
             */
            public Object named(Object t, String n) {
                return ((PropertiesCollection) t).getNamed(n);
            }
        });
        bridge.defineInterface("PropertyNodeList", "NodeList", PropertyNodeList.class, new String[] { "m:getValues()" });
        bridge.setIndexed("PropertyNodeList", new IndexedObject.Source() {
            /**
             * Returns the number of elements.
             * <p>
             * 요소 수를 반환한다.
             *
             * @param t the list / 목록
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((PropertyNodeList) t).getLength();
            }

            /**
             * Returns the element at an index.
             * <p>
             * 위치의 요소를 반환한다.
             *
             * @param t the list / 목록
             * @param i the index / 위치
             * @return the element / 요소
             */
            public Object item(Object t, int i) {
                return ((PropertyNodeList) t).item(i);
            }

            /**
             * A property node list has no named access.
             * <p>
             * 프로퍼티 노드 목록에는 이름 접근이 없다.
             *
             * @param t the list / 목록
             * @param n the name / 이름
             * @return null / null
             */
            public Object named(Object t, String n) {
                return null;
            }
        });
        installDocument();
        installElement();
    }

    /**
     * Installs the <code>getItems</code> method of the document (<code>document.getItems()</code>).
     * <p>
     * <code>document.getItems()</code> 를 설치한다.
     */
    private void installDocument() {
        final DomBridge bridge = page.getBridge();
        JsObject d = bridge.getInterface("Document").getPrototype();
        d.defineBuiltin("getItems", page.getRealm().newFunction("getItems", 0, new NativeFunction.Call() {
            /**
             * Returns the top-level items that have all the given types (a space separated list; all items when omitted).
             * <p>
             * 주어진 타입을 모두 가진 최상위 아이템을 반환한다(공백으로 구분한 목록. 생략하면 모든 아이템).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (types) / 인수(타입)
             * @return a NodeList of the elements / 요소의 NodeList
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue t = Js.arg(args, 0);
                List<String> types = t.isUndefined() ? new ArrayList<String>() : Microdata.tokens(Ops.toString(t));
                return bridge.toJs(new StaticNodeList(Microdata.topLevelItems(page.getDocument(), types)));
            }
        }));
    }

    /**
     * Installs the members of the HTML elements.
     * <p>
     * HTML 요소의 멤버를 설치한다.
     */
    private void installElement() {
        final DomBridge bridge = page.getBridge();
        JsObject he = bridge.getInterface("HTMLElement").getPrototype();
        accessor(he, "itemScope", new NativeFunction.Call() {
            /**
             * Reflects the <code>itemscope</code> attribute.
             * <p>
             * <code>itemscope</code> 속성을 반영한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return a boolean / 불리언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(element(self).hasAttribute("itemscope"));
            }
        }, new NativeFunction.Call() {
            /**
             * Sets or removes the <code>itemscope</code> attribute.
             * <p>
             * <code>itemscope</code> 속성을 지정하거나 지운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                if (Ops.toBoolean(Js.arg(args, 0))) {
                    e.setAttribute("itemscope", "");
                } else {
                    e.removeAttribute("itemscope");
                }
                return Js.UNDEFINED;
            }
        });
        accessor(he, "itemId", new NativeFunction.Call() {
            /**
             * Reflects the <code>itemid</code> attribute (empty if missing).
             * <p>
             * <code>itemid</code> 속성을 반영한다(없으면 빈 문자열).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String v = element(self).getAttribute("itemid");
                return Js.str(v == null ? "" : v);
            }
        }, new NativeFunction.Call() {
            /**
             * Sets the <code>itemid</code> attribute.
             * <p>
             * <code>itemid</code> 속성을 지정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self).setAttribute("itemid", Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        tokenList(he, "itemType", "itemtype");
        tokenList(he, "itemProp", "itemprop");
        tokenList(he, "itemRef", "itemref");
        accessor(he, "properties", new NativeFunction.Call() {
            /**
             * Returns the collection of the properties of the item (empty if the element is not an item).
             * <p>
             * 아이템의 프로퍼티 컬렉션을 반환한다(요소가 아이템이 아니면 비어 있다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = element(self);
                PropertiesCollection c = collections.get(e);
                if (c == null) {
                    c = new PropertiesCollection(e);
                    collections.put(e, c);
                }
                return bridge.toJs(c);
            }
        }, null);
        accessor(he, "itemValue", new NativeFunction.Call() {
            /**
             * Reads the value of the property element: null, the element itself, or a string (see {@link Microdata#itemValue(Element)}).
             * <p>
             * 프로퍼티 요소의 값을 읽는다: null, 요소 자신, 또는 문자열({@link Microdata#itemValue(Element)} 참조).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(Microdata.itemValue(element(self)));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes the value of the property element.
             * <p>
             * 프로퍼티 요소의 값을 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    Microdata.setItemValue(element(self), Ops.toString(Js.arg(args, 0)));
                } catch (DomException ex) {
                    throw bridge.domError(ex);
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Defines a token list property (<code>[PutForwards=value]</code>): reading gives the live list, writing a string sets the attribute.
     * <p>
     * 토큰 목록 프로퍼티를 정의한다(<code>[PutForwards=value]</code>): 읽으면 라이브 목록, 문자열을 쓰면 속성을 지정한다.
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
}
