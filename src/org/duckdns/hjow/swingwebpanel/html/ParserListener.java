package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Observer of a running {@link HtmlParser}: it learns about diagnostics, the document mode, the resources that the document refers
 * to (scripts, style sheets, images, base URL, refresh) and elements that have just been created or closed. This is how the loading
 * model of the browser (P07-08) hooks into the parser without the parser knowing anything about networking or scripting.
 * <p>
 * All callbacks are made on the thread that runs the parser, between two tokens, so the document tree is consistent (pending text has
 * been flushed) and the callback may inspect or even modify the tree. Subclass {@link ParserListenerAdapter} to override only some of
 * the callbacks.
 * <p>
 * 실행 중인 {@link HtmlParser} 의 관찰자: 진단, 문서 모드, 문서가 참조하는 자원(스크립트, 스타일시트, 이미지, 기준 URL, 새로고침), 방금 만들어지거나
 * 닫힌 요소를 알려 준다. 이를 통해 브라우저의 로딩 모델(P07-08)이 파서가 네트워크나 스크립트를 몰라도 되도록 파서에 연결된다.
 * <p>
 * 모든 콜백은 파서를 실행하는 스레드에서 토큰과 토큰 사이에 호출되므로 문서 트리는 일관된 상태(대기 중인 텍스트가 반영됨)이며, 콜백이 트리를 살펴보거나
 * 수정할 수도 있다. 일부 콜백만 재정의하려면 {@link ParserListenerAdapter} 를 상속한다.
 */
public interface ParserListener {

    /**
     * Called for every parse error (a diagnostic; parsing continues).
     * <p>
     * 파싱 오류마다 호출된다(진단이며 파싱은 계속된다).
     *
     * @param error the error / 오류
     */
    void parseError(ParseError error);

    /**
     * Called once when the DOCTYPE (or its absence) fixes the document mode.
     * <p>
     * DOCTYPE(또는 DOCTYPE 이 없음)으로 문서 모드가 정해질 때 한 번 호출된다.
     *
     * @param mode {@link org.duckdns.hjow.swingwebpanel.dom.Document#NO_QUIRKS_MODE}, {@link org.duckdns.hjow.swingwebpanel.dom.Document#LIMITED_QUIRKS_MODE} or
     *             {@link org.duckdns.hjow.swingwebpanel.dom.Document#QUIRKS_MODE} / 문서 모드 상수
     */
    void documentModeDetermined(int mode);

    /**
     * Called right after an element has been created from a start tag and inserted into the tree (its attributes are already set,
     * its children are not parsed yet).
     * <p>
     * 시작 태그로 요소를 만들어 트리에 삽입한 직후 호출된다(속성은 이미 지정되었고 자식은 아직 파싱되지 않았다).
     *
     * @param element the element / 요소
     */
    void elementInserted(Element element);

    /**
     * Called when an element has been popped off the stack of open elements, that is, when its children have been parsed ("finish
     * parsing the children"). Elements that the parser closes implicitly are reported too.
     * <p>
     * 요소가 열린 요소 스택에서 제거될 때, 즉 자식 파싱이 끝났을 때 호출된다("finish parsing the children"). 파서가 암시적으로 닫는 요소도 보고된다.
     *
     * @param element the element / 요소
     */
    void elementClosed(Element element);

    /**
     * Called when the end tag of a <code>script</code> element (HTML or SVG) has been processed, at the point where the HTML Standard
     * says to "prepare the script element". The element's text content is complete. The listener may run the script (which may call
     * {@link ParserControl#insertAtInsertionPoint(String)} for <code>document.write</code>) or call {@link ParserControl#pause()}
     * while an external script is loading and {@link HtmlParser#resume()} later. Scripts of fragment parsing are not reported
     * (they are marked "already started" instead).
     * <p>
     * <code>script</code> 요소(HTML 또는 SVG)의 종료 태그를 처리했을 때, HTML 표준이 "prepare the script element" 를 하라고 한 지점에서 호출된다.
     * 요소의 텍스트 내용은 완성되어 있다. 리스너는 스크립트를 실행하거나(<code>document.write</code> 를 위해
     * {@link ParserControl#insertAtInsertionPoint(String)} 를 호출할 수 있다), 외부 스크립트를 불러오는 동안 {@link ParserControl#pause()} 를 호출하고
     * 나중에 {@link HtmlParser#resume()} 을 호출할 수 있다. 조각 파싱의 스크립트는 보고하지 않는다(대신 "already started" 로 표시한다).
     *
     * @param script  the script element / script 요소
     * @param control control of the parser / 파서 제어
     */
    void scriptReady(Element script, ParserControl control);

    /**
     * Called when a <code>style</code> element has been closed (its text is the style sheet source).
     * <p>
     * <code>style</code> 요소가 닫혔을 때 호출된다(텍스트가 스타일시트 원문이다).
     *
     * @param style the style element / style 요소
     */
    void styleReady(Element style);

    /**
     * Called when a <code>link</code> element whose <code>rel</code> contains <code>stylesheet</code> has been inserted.
     * <p>
     * <code>rel</code> 에 <code>stylesheet</code> 가 있는 <code>link</code> 요소가 삽입되었을 때 호출된다.
     *
     * @param link the link element / link 요소
     */
    void stylesheetFound(Element link);

    /**
     * Called when an <code>img</code> element has been inserted (the <code>image</code> start tag is turned into <code>img</code>).
     * <p>
     * <code>img</code> 요소가 삽입되었을 때 호출된다(<code>image</code> 시작 태그는 <code>img</code> 로 바뀐다).
     *
     * @param img the img element / img 요소
     */
    void imageFound(Element img);

    /**
     * Called when the first <code>base</code> element with an <code>href</code> attribute fixes the document base URL. The parser has
     * already stored the resolved value with {@link org.duckdns.hjow.swingwebpanel.dom.Document#setBaseUri(String)}.
     * <p>
     * <code>href</code> 속성이 있는 첫 <code>base</code> 요소가 문서 기준 URL 을 정할 때 호출된다. 파서는 이미 해석한 값을
     * {@link org.duckdns.hjow.swingwebpanel.dom.Document#setBaseUri(String)} 로 저장했다.
     *
     * @param href     the attribute value / 속성 값
     * @param resolved the absolute base URL that was stored / 저장된 절대 기준 URL
     */
    void baseHrefChanged(String href, String resolved);

    /**
     * Called when a <code>meta http-equiv="refresh"</code> element has been inserted.
     * <p>
     * <code>meta http-equiv="refresh"</code> 요소가 삽입되었을 때 호출된다.
     *
     * @param content the <code>content</code> attribute value / <code>content</code> 속성 값
     */
    void metaRefresh(String content);

    /**
     * Called when a <code>title</code> element has been closed.
     * <p>
     * <code>title</code> 요소가 닫혔을 때 호출된다.
     *
     * @param title the text of the first title element of the document / 문서의 첫 title 요소의 텍스트
     */
    void titleReady(String title);

    /**
     * Called when a <code>meta</code> element declares a character encoding (<code>charset</code> or
     * <code>http-equiv="content-type"</code>), whether or not it changes the encoding in use.
     * <p>
     * <code>meta</code> 요소가 문자 인코딩을 선언했을 때(<code>charset</code> 또는 <code>http-equiv="content-type"</code>), 사용 중인 인코딩이
     * 바뀌는지와 관계없이 호출된다.
     *
     * @param encodingName the WHATWG encoding name / WHATWG 인코딩 이름
     */
    void charsetDeclared(String encodingName);
}
