/**
 * Markdown to HTML conversion for the <code>text/markdown</code> content type (plan items P17-03, P17-04): a self-written block and inline
 * parser that follows CommonMark 0.31.2 (<code>https://spec.commonmark.org/0.31.2/</code>) with the GitHub Flavored Markdown extensions
 * (tables, strikethrough, task lists, extended autolinks, tag filter) behind a switch. Raw HTML in the Markdown is escaped by default, and
 * links with dangerous schemes are neutralized. The entry point is {@link org.duckdns.hjow.swingwebpanel.content.markdown.MarkdownConverter}; the original
 * Markdown text is never changed by a conversion.
 * <p>
 * <code>text/markdown</code> 콘텐츠를 위한 Markdown → HTML 변환(계획 P17-03, P17-04): CommonMark 0.31.2
 * (<code>https://spec.commonmark.org/0.31.2/</code>)를 따르는 자체 블록/인라인 파서이며, GitHub Flavored Markdown 확장(표, 취소선, 작업 목록, 확장
 * 자동 링크, 태그 필터)은 스위치로 켠다. Markdown 안의 원시 HTML 은 기본적으로 이스케이프하고 위험한 스킴의 링크는 무력화한다. 진입점은
 * {@link org.duckdns.hjow.swingwebpanel.content.markdown.MarkdownConverter} 이며 변환은 원래 Markdown 텍스트를 바꾸지 않는다.
 */
package org.duckdns.hjow.swingwebpanel.content.markdown;
