package org.duckdns.hjow.swingwebpanel.dom.xpath;

/**
 * Resolves the prefixes of the name tests of an XPath expression to namespace URIs (the <code>XPathNSResolver</code> of DOM XPath).
 * <p>
 * XPath 식의 이름 테스트에 쓰인 접두사를 네임스페이스 URI 로 해석한다(DOM XPath 의 <code>XPathNSResolver</code>).
 */
public interface NamespaceResolver {

    /**
     * Returns the namespace URI of a prefix.
     * <p>
     * 접두사의 네임스페이스 URI 를 반환한다.
     *
     * @param prefix the prefix / 접두사
     * @return the namespace URI, or null if the prefix is not bound / 네임스페이스 URI. 접두사가 묶여 있지 않으면 null
     */
    String lookupNamespaceURI(String prefix);
}
