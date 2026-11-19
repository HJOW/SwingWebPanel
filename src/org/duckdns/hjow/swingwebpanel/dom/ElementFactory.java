package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Creates the element objects of a document. The DOM package creates plain {@link Element} objects; the HTML layer installs a factory
 * that creates the specialized element classes (anchor, image, form control ...) for HTML-namespace elements (plan items HEI-01 to HEI-09).
 * <p>
 * 문서의 요소 객체를 만든다. DOM 패키지는 일반 {@link Element} 를 만들고, HTML 계층이 HTML 네임스페이스 요소에 대해 특화된 요소 클래스
 * (앵커, 이미지, 폼 컨트롤 등)를 만드는 팩터리를 설치한다(계획 HEI-01~HEI-09).
 */
public interface ElementFactory {

    /**
     * Creates an element.
     * <p>
     * 요소를 만든다.
     *
     * @param document     owner document / 소유 문서
     * @param namespaceURI namespace or null / 네임스페이스 또는 null
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name, already normalized (lower case for HTML elements of HTML documents) / 로컬 이름(이미 정규화됨)
     * @return the new element, never null / 새 요소(null 아님)
     */
    Element createElement(Document document, String namespaceURI, String prefix, String localName);
}
