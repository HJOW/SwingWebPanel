package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The factory object of independent documents (<code>document.implementation</code>): it creates document types, XML documents and
 * HTML documents.
 * <p>
 * 독립된 문서를 만드는 팩터리 객체(<code>document.implementation</code>): 문서 유형, XML 문서, HTML 문서를 만든다.
 */
public final class DomImplementation {

    /**
     * Creates the object (use {@link Document#getImplementation()}).
     * <p>
     * 객체를 만든다({@link Document#getImplementation()} 를 사용).
     */
    DomImplementation() {
    }

    /**
     * Creates a document type that is not yet owned by a document tree.
     * <p>
     * 아직 문서 트리가 소유하지 않은 문서 유형을 만든다.
     *
     * @param qualifiedName name / 이름
     * @param publicId      public identifier / 공개 식별자
     * @param systemId      system identifier / 시스템 식별자
     * @return the document type, owned by a new empty document / 새 빈 문서가 소유한 문서 유형
     * @throws DomException InvalidCharacterError for an invalid name / 이름이 올바르지 않으면 InvalidCharacterError
     */
    public DocumentType createDocumentType(String qualifiedName, String publicId, String systemId) {
        if (!Namespaces.isValidName(qualifiedName)) {
            throw DomException.invalidCharacter("invalid document type name: " + qualifiedName);
        }
        return new Document(true, "text/html").createDocumentType(qualifiedName, publicId, systemId);
    }

    /**
     * Creates an XML document, optionally with a document type and a root element (<code>createDocument</code>). The content type
     * follows the namespace of the root element: XHTML gives <code>application/xhtml+xml</code>, SVG gives <code>image/svg+xml</code>,
     * everything else <code>application/xml</code>.
     * <p>
     * 문서 유형과 루트 요소를 선택적으로 가진 XML 문서를 만든다(<code>createDocument</code>). 콘텐츠 타입은 루트 요소의 네임스페이스를 따른다:
     * XHTML 은 <code>application/xhtml+xml</code>, SVG 는 <code>image/svg+xml</code>, 그 밖에는 <code>application/xml</code>.
     *
     * @param namespace     namespace of the root element or null / 루트 요소의 네임스페이스 또는 null
     * @param qualifiedName qualified name of the root element, null or empty for none / 루트 요소의 이름. null 이나 빈 문자열이면 없음
     * @param doctype       document type or null / 문서 유형 또는 null
     * @return the document / 문서
     * @throws DomException InvalidCharacterError or NamespaceError / InvalidCharacterError 또는 NamespaceError
     */
    public Document createDocument(String namespace, String qualifiedName, DocumentType doctype) {
        String type = Namespaces.HTML.equals(namespace) ? "application/xhtml+xml"
                : Namespaces.SVG.equals(namespace) ? "image/svg+xml" : "application/xml";
        Document d = new Document(false, type);
        if (doctype != null) {
            d.appendChild(doctype);
        }
        if (qualifiedName != null && qualifiedName.length() > 0) {
            d.appendChild(d.createElementNS(namespace, qualifiedName));
        }
        return d;
    }

    /**
     * Creates an HTML document with the skeleton <code>&lt;!DOCTYPE html&gt;&lt;html&gt;&lt;head&gt;[&lt;title&gt;]&lt;/head&gt;&lt;body&gt;</code>
     * (<code>createHTMLDocument</code>).
     * <p>
     * <code>&lt;!DOCTYPE html&gt;&lt;html&gt;&lt;head&gt;[&lt;title&gt;]&lt;/head&gt;&lt;body&gt;</code> 뼈대를 가진 HTML 문서를 만든다
     * (<code>createHTMLDocument</code>).
     *
     * @param title title text, or null for a document without a title element / 제목 텍스트. title 요소가 없는 문서이면 null
     * @return the document / 문서
     */
    public Document createHtmlDocument(String title) {
        Document d = new Document(true, "text/html");
        d.appendChild(d.createDocumentType("html", "", ""));
        Element html = d.createElement("html");
        d.appendChild(html);
        Element head = d.createElement("head");
        html.appendChild(head);
        if (title != null) {
            Element t = d.createElement("title");
            t.appendChild(d.createTextNode(title));
            head.appendChild(t);
        }
        html.appendChild(d.createElement("body"));
        return d;
    }

    /**
     * Always returns true (the DOM Standard keeps this legacy method for compatibility).
     * <p>
     * 항상 true 를 반환한다(DOM 표준은 호환을 위해 이 옛 메서드를 유지한다).
     *
     * @param feature ignored / 무시한다
     * @param version ignored / 무시한다
     * @return true / true
     */
    public boolean hasFeature(String feature, String version) {
        return true;
    }
}
