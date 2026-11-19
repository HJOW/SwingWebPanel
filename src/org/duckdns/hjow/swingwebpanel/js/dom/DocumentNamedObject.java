package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;

/**
 * The wrapper of an HTML document with the named properties of old pages (HTML5 REC 2014-10-28 section 5.3, named access on the
 * <code>Document</code> object): <code>document.myform</code> is the <code>form</code> element with <code>name="myform"</code>,
 * <code>document.logo</code> the <code>img</code>, <code>embed</code> or <code>object</code> with that name, and so on (see
 * {@link ElementCollections#documentNames}). If exactly one element has the name the property is that element, otherwise a live
 * collection of them. Unlike the specification, which lets these names hide the built-in members of the document (the property that
 * is known as DOM clobbering), a name never hides a member that exists: the named properties come after the prototype chain, so a form
 * named <code>cookie</code> does not replace <code>document.cookie</code>. The names are indexed once for each state of the document.
 * <p>
 * 옛 페이지의 이름 있는 프로퍼티를 가진 HTML 문서의 래퍼(HTML5 REC 2014-10-28 §5.3, <code>Document</code> 객체의 이름 접근): <code>document.myform</code> 은
 * <code>name="myform"</code> 인 <code>form</code> 요소, <code>document.logo</code> 는 그 이름의 <code>img</code>, <code>embed</code>, <code>object</code> 등이다
 * ({@link ElementCollections#documentNames} 참조). 이름이 같은 요소가 하나이면 프로퍼티는 그 요소이고 아니면 그 요소들의 라이브 컬렉션이다. 이 이름이 문서의 내장 멤버를
 * 가리게 하는 규격(DOM clobbering 으로 알려진 성질)과 달리 이름은 있는 멤버를 가리지 않는다: 이름 있는 프로퍼티는 프로토타입 체인 뒤에 오므로 <code>cookie</code> 라는 폼이
 * <code>document.cookie</code> 를 바꾸지 않는다. 이름은 문서의 상태마다 한 번 색인한다.
 */
final class DocumentNamedObject extends HostObject {

    /**
     * The elements by name for the state of the document that the version names (in tree order).
     * <p>
     * 버전이 가리키는 문서 상태의 이름별 요소(트리 순서).
     */
    private Map<String, List<Element>> index;

    /**
     * The mutation version of the document that the index is valid for.
     * <p>
     * 색인이 유효한 문서의 변경 버전.
     */
    private long indexVersion = -1;

    /**
     * Creates the wrapper.
     * <p>
     * 래퍼를 만든다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param target the document / 문서
     */
    DocumentNamedObject(DomBridge bridge, JsObject proto, Document target) {
        super(bridge, proto, target);
    }

    /**
     * Returns the own property, or the named property if nothing in the prototype chain has the name.
     * <p>
     * 자체 프로퍼티를 반환하며 프로토타입 체인에 그 이름이 없으면 이름 있는 프로퍼티를 반환한다.
     *
     * @param key the property key / 프로퍼티 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property own = super.getOwnPropertySlot(key);
        if (own != null || !(key instanceof String)) {
            return own;
        }
        JsObject proto = getPrototypeOf();
        // 프로토타입 체인에 있는 이름(내장 멤버)은 가리지 않는다.
        if (proto != null && proto.hasProperty(key)) {
            return null;
        }
        Document doc = (Document) getTarget();
        List<Element> hits = lookup(doc, (String) key);
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        JsValue value = hits.size() == 1 ? getBridge().toJs(hits.get(0)) : getBridge().toJs(ElementCollections.documentNamed(doc, (String) key));
        return Property.data(value, false, false, true);
    }

    /**
     * Looks a name up in the index of the document, which is built again when the document has changed since it was built.
     * <p>
     * 문서의 색인에서 이름을 찾는다. 색인을 만든 뒤 문서가 바뀌었으면 색인을 다시 만든다.
     *
     * @param doc  the document / 문서
     * @param name the name / 이름
     * @return the elements with the name in tree order, or null / 그 이름을 가진 트리 순서의 요소. 없으면 null
     */
    private List<Element> lookup(Document doc, String name) {
        if (!doc.isHtmlDocument()) {
            return null;
        }
        long version = doc.getMutationVersion();
        if (index == null || indexVersion != version) {
            Map<String, List<Element>> built = new LinkedHashMap<String, List<Element>>();
            // 문서의 모든 요소를 트리 순서로 훑으며 이름 있는 프로퍼티가 되는 이름마다 요소를 모은다.
            for (Node n = Node.nextInTree(doc, doc); n != null; n = Node.nextInTree(n, doc)) {
                if (n instanceof Element) {
                    List<String> names = ElementCollections.documentNames((Element) n);
                    // 한 요소의 이름들을 색인에 더한다(같은 이름이 둘이면 한 번만).
                    for (int i = 0; i < names.size(); i++) {
                        List<Element> list = built.get(names.get(i));
                        if (list == null) {
                            list = new ArrayList<Element>(1);
                            built.put(names.get(i), list);
                        }
                        if (list.isEmpty() || list.get(list.size() - 1) != n) {
                            list.add((Element) n);
                        }
                    }
                }
            }
            index = built;
            indexVersion = version;
        }
        return index.get(name);
    }
}
