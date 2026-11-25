package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live element and name data for the legacy HTMLAllCollection of one document.
 * <p>
 * 한 문서의 레거시 HTMLAllCollection에 쓰는 라이브 요소와 이름 데이터.
 */
public final class AllElements {

    /** The names of HTML elements that contribute their name attribute. / name 속성을 제공하는 HTML 요소 이름. */
    private static final String NAMED_TAGS = " a button embed form frame frameset iframe img input map meta object select textarea ";

    /** The document at the root of the collection. / 컬렉션의 루트인 문서. */
    private final Document document;

    /** The live elements in tree order. / 트리 순서의 라이브 요소 목록. */
    private final ElementCollection elements;

    /** The sole matching element per name, or a null value for repeated names. / 이름별 단일 일치 요소. 중복 이름의 값은 null. */
    private Map<String, Element> names;

    /** The document version of the name index. / 이름 색인을 만든 문서 버전. */
    private long namesVersion = -1L;

    /**
     * Creates the live collection for the given document.
     * <p>
     * 지정한 문서의 라이브 컬렉션을 만든다.
     *
     * @param document the document / 문서
     */
    AllElements(Document document) {
        this.document = document;
        elements = document.getElementsByTagName("*");
    }

    /**
     * Returns the current number of elements.
     * <p>
     * 현재 요소의 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return elements.getLength();
    }

    /**
     * Returns the element at an index.
     * <p>
     * 지정한 위치의 요소를 반환한다.
     *
     * @param index the index / 위치
     * @return the element, or null if out of range / 요소 또는 범위 밖이면 null
     */
    public Element item(int index) {
        return elements.item(index);
    }

    /**
     * Resolves an id or an eligible name. Multiple matches yield a live HTMLCollection.
     * <p>
     * id 또는 지원되는 이름을 조회한다. 일치 요소가 여러 개이면 라이브 HTMLCollection을 반환한다.
     *
     * @param name the id or name / id 또는 이름
     * @return an element, a live collection, or null / 요소, 라이브 컬렉션 또는 null
     */
    public Object namedItem(final String name) {
        Map<String, Element> index = currentNames();
        Element first = index.get(name);
        if (first != null || !index.containsKey(name)) {
            return first;
        }
        return new ElementCollection(document) {
            /**
             * Tests whether the element still contributes the requested id or name.
             * <p>
             * 요소가 조회한 id 또는 이름을 여전히 제공하는지 확인한다.
             *
             * @param element the candidate / 후보 요소
             * @return whether it matches / 일치하면 true
             */
            boolean matches(Element element) {
                return name.equals(element.getId()) || name.equals(eligibleName(element));
            }
        };
    }

    /**
     * Returns a snapshot of the supported names in tree order.
     * <p>
     * 지원하는 이름을 트리 순서로 복사해 반환한다.
     *
     * @return the names / 이름 목록
     */
    public List<String> supportedNames() {
        return new ArrayList<String>(currentNames().keySet());
    }

    /**
     * Reports whether the current tree contributes the given name.
     * <p>
     * 현재 트리에 지정한 이름이 있는지 확인한다.
     *
     * @param name the property name / 프로퍼티 이름
     * @return whether it is supported / 지원하면 true
     */
    public boolean supportsName(String name) {
        return currentNames().containsKey(name);
    }

    /**
     * Returns the cached id and name index for the current tree.
     * <p>
     * 현재 트리의 id와 이름 색인을 반환한다.
     *
     * @return the name index / 이름 색인
     */
    private Map<String, Element> currentNames() {
        long version = document.getMutationVersion();
        if (names == null || namesVersion != version) {
            Map<String, Element> index = new LinkedHashMap<String, Element>();
            // 트리 순서로 순회하면서 같은 요소의 id를 name보다 먼저 추가한다.
            for (Element element : elements.toList()) {
                String id = element.getId();
                String name = eligibleName(element);
                if (id.length() != 0) {
                    index.put(id, index.containsKey(id) ? null : element);
                }
                if (name != null && name.length() != 0 && !name.equals(id)) {
                    index.put(name, index.containsKey(name) ? null : element);
                }
            }
            names = index;
            namesVersion = version;
        }
        return names;
    }

    /**
     * Reads the name of an HTML element eligible for all-named lookup.
     * <p>
     * all 이름 검색에 참여하는 HTML 요소의 name 속성을 읽는다.
     *
     * @param element the element / 요소
     * @return its name or null / 이름 또는 null
     */
    private static String eligibleName(Element element) {
        return Namespaces.HTML.equals(element.getNamespaceURI()) && NAMED_TAGS.contains(" " + element.getLocalName() + " ")
                ? element.getAttribute("name") : null;
    }
}
