package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;

/**
 * The part of a running {@link HtmlParser} that a {@link ParserListener} may use while it handles a callback: inserting markup at the
 * insertion point (<code>document.write</code>) and pausing the parser (a pending parsing-blocking script).
 * <p>
 * {@link ParserListener} 가 콜백을 처리하는 동안 쓸 수 있는, 실행 중인 {@link HtmlParser} 의 부분: 삽입 지점에 마크업 삽입
 * (<code>document.write</code>)과 파서 일시 정지(파싱을 막는 스크립트 대기).
 */
public interface ParserControl {

    /**
     * Inserts text into the input stream just before the next input character, exactly as <code>document.write</code> does while
     * the parser is running. The text is tokenized after the callback returns. Ignored in fragment parsing.
     * <p>
     * 다음 입력 문자 바로 앞의 입력 스트림에 문자열을 삽입한다. 파서가 실행 중일 때 <code>document.write</code> 가 하는 것과 같다. 문자열은 콜백이
     * 반환된 뒤 토큰화된다. 조각 파싱에서는 무시된다.
     *
     * @param text markup text / 마크업 문자열
     */
    void insertAtInsertionPoint(String text);

    /**
     * Requests that {@link HtmlParser#run()} returns after the current token so that a blocking script can be loaded; continue with
     * {@link HtmlParser#resume()}.
     * <p>
     * 파싱을 막는 스크립트를 불러올 수 있도록 현재 토큰 뒤에 {@link HtmlParser#run()} 이 반환되게 요청한다. {@link HtmlParser#resume()} 으로 계속한다.
     */
    void pause();

    /**
     * Tells whether a fragment is being parsed (innerHTML) instead of a whole document.
     * <p>
     * 문서 전체가 아니라 조각(innerHTML)을 파싱 중인지 알려 준다.
     *
     * @return true for fragment parsing / 조각 파싱이면 true
     */
    boolean isFragment();

    /**
     * Returns the document that the parser creates nodes in.
     * <p>
     * 파서가 노드를 만드는 문서를 반환한다.
     *
     * @return document / 문서
     */
    Document getDocument();
}
