package org.duckdns.hjow.swingwebpanel.render;

import java.io.UnsupportedEncodingException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.XmlSerializer;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgReference;

/**
 * Finds the SVG elements that a box refers to with <code>filter: url(#id)</code>, <code>clip-path: url(#id)</code> and <code>mask-image: url(#id)</code>
 * in the document of the box (CSS Filter Effects 1 section 5, CSS Masking 1 sections 5 and 7). The element must be of the kind that the property names
 * (<code>filter</code>, <code>clipPath</code> or <code>mask</code>) and in the SVG namespace; the definitions are the outermost <code>svg</code> element
 * that contains it, which is serialized and parsed once for every <code>svg</code> element and kept (an inline <code>svg</code> that only holds
 * definitions is usually hidden or empty, so it is read from the DOM and not from the layout). A reference to another document is not followed. One
 * instance serves the document of one display list build and is used on one thread.
 * <p>
 * 상자가 문서 안에서 <code>filter: url(#id)</code>, <code>clip-path: url(#id)</code>, <code>mask-image: url(#id)</code> 로 가리키는 SVG 요소를 찾는다(CSS Filter Effects 1 5 절,
 * CSS Masking 1 5, 7 절). 요소는 속성이 말하는 종류(<code>filter</code>, <code>clipPath</code>, <code>mask</code>)이고 SVG 네임스페이스에 있어야 한다. 정의는 그것을 담은 가장
 * 바깥 <code>svg</code> 요소이며 <code>svg</code> 요소마다 한 번 직렬화해 해석하고 보관한다(정의만 든 인라인 <code>svg</code> 는 보통 숨겨지거나 비어 있으므로 레이아웃이
 * 아니라 DOM 에서 읽는다). 다른 문서의 참조는 따르지 않는다. 인스턴스 하나는 디스플레이 리스트를 만드는 문서 하나를 위한 것이며 한 스레드에서 쓴다.
 */
final class SvgReferences implements FilterSpec.Resolver {

    /** The document that the references are looked up in. 참조를 찾는 문서. */
    private final Document doc;

    /** The definitions by the outermost <code>svg</code> element; a null value means that the element could not be read. 가장 바깥 <code>svg</code> 요소별 정의. null 은 읽을 수 없는 요소. */
    private final Map<Element, SvgReference> cache = new IdentityHashMap<Element, SvgReference>();

    /**
     * Creates the finder.
     * <p>
     * 찾는 쪽을 만든다.
     *
     * @param doc the document of the boxes / 상자의 문서
     */
    SvgReferences(Document doc) {
        this.doc = doc;
    }

    /**
     * Returns the definitions with a <code>filter</code> element of an id.
     * <p>
     * id 가 있는 <code>filter</code> 요소가 든 정의를 반환한다.
     *
     * @param id the id / id
     * @return the definitions or null / 정의. 없으면 null
     */
    public SvgReference filter(String id) {
        return find(id, "filter");
    }

    /**
     * Returns the definitions with a <code>clipPath</code> element of an id.
     * <p>
     * id 가 있는 <code>clipPath</code> 요소가 든 정의를 반환한다.
     *
     * @param id the id / id
     * @return the definitions or null / 정의. 없으면 null
     */
    SvgReference clipPath(String id) {
        return find(id, "clipPath");
    }

    /**
     * Returns the definitions with a <code>mask</code> element of an id.
     * <p>
     * id 가 있는 <code>mask</code> 요소가 든 정의를 반환한다.
     *
     * @param id the id / id
     * @return the definitions or null / 정의. 없으면 null
     */
    SvgReference mask(String id) {
        return find(id, "mask");
    }

    /**
     * Finds the definitions that contain an element of a kind with an id.
     * <p>
     * id 가 있는 어떤 종류의 요소가 든 정의를 찾는다.
     *
     * @param id   the id / id
     * @param kind the local name of the element / 요소의 지역 이름
     * @return the definitions or null / 정의. 없으면 null
     */
    private SvgReference find(String id, String kind) {
        Element e = doc == null || id == null ? null : doc.getElementById(id);
        if (e == null || !Namespaces.SVG.equals(e.getNamespaceURI()) || !kind.equals(e.getLocalName())) {
            return null;
        }
        Element root = e;
        // SVG 네임스페이스 조상을 따라 가장 바깥 svg 요소까지 올라간다.
        for (Node p = root.getParentNode(); p instanceof Element && Namespaces.SVG.equals(((Element) p).getNamespaceURI()); p = p.getParentNode()) {
            root = (Element) p;
        }
        if (!"svg".equals(root.getLocalName())) {
            return null;
        }
        SvgReference ref;
        // 같은 svg 요소는 한 번만 직렬화해 해석한다(읽을 수 없는 요소도 기억한다).
        if (cache.containsKey(root)) {
            ref = cache.get(root);
        } else {
            ref = parse(root);
            cache.put(root, ref);
        }
        return ref != null && ref.has(id, kind) ? ref : null;
    }

    /**
     * Serializes and parses an <code>svg</code> element.
     * <p>
     * <code>svg</code> 요소를 직렬화해 해석한다.
     *
     * @param svg the element / 요소
     * @return the definitions, or null if the element cannot be read / 정의. 읽을 수 없으면 null
     */
    private static SvgReference parse(Element svg) {
        try {
            return SvgReference.parse(XmlSerializer.serialize(svg).getBytes("UTF-8"));
        } catch (MediaException e) {
            // 올바르지 않거나 너무 큰 svg 는 참조를 풀 수 없는 것으로 본다.
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (RuntimeException e) {
            // 직렬화나 해석의 예기치 않은 오류가 페이지 그리기를 막지 않게 한다.
            return null;
        }
    }
}
