/**
 * XHTML documents (plan item P17-05): {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser} reads <code>application/xhtml+xml</code> with the XML
 * parser of the Java platform (public JAXP/SAX API) and builds the project's own DOM. XML is strict (no error recovery), names are case
 * sensitive, external DTDs, entities and XInclude are never loaded, and every parse is bounded by {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlLimits}.
 * Errors are reported as {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XmlParseError} with line, column and source line.
 * <p>
 * XHTML 문서(계획 P17-05): {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser} 는 Java 플랫폼의 XML 파서(공개 JAXP/SAX API)로
 * <code>application/xhtml+xml</code> 을 읽어 프로젝트 자체 DOM 을 만든다. XML 은 엄격하며(오류 복구 없음), 이름은 대소문자를 구분하고, 외부 DTD, 엔티티,
 * XInclude 는 절대 읽지 않으며, 모든 해석은 {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlLimits} 로 한정된다. 오류는 줄, 열, 소스 줄이 있는
 * {@link org.duckdns.hjow.swingwebpanel.content.xhtml.XmlParseError} 로 보고한다.
 */
package org.duckdns.hjow.swingwebpanel.content.xhtml;
