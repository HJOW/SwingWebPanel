package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;

/**
 * The result of {@link XhtmlParser}: the DOM document, the decoded text (the "received source" that <code>getContent()</code> keeps), the
 * encoding decision and, if the document is not well-formed or exceeds a limit, the structured error. When there is an error the
 * document holds what had been built before the error (a prefix of the tree); callers that follow the XML rule "no error recovery" show
 * an error page from {@link #getError()} and do not use that partial tree.
 * <p>
 * {@link XhtmlParser} 의 결과: DOM 문서, 디코딩한 텍스트(<code>getContent()</code> 가 보관하는 "수신 원문"), 인코딩 판정, 그리고 문서가 well-formed 가
 * 아니거나 상한을 넘으면 구조화된 오류. 오류가 있으면 문서에는 오류 전에 만든 것(트리의 앞부분)이 들어 있다. "오류 복구 없음"이라는 XML 규칙을 따르는
 * 호출자는 {@link #getError()} 로 오류 페이지를 보여 주고 그 부분 트리는 쓰지 않는다.
 */
public final class XhtmlResult {

    /**
     * The document.
     * <p>
     * 문서.
     */
    private final Document document;

    /**
     * The decoded text.
     * <p>
     * 디코딩한 텍스트.
     */
    private final XmlText text;

    /**
     * The error or null.
     * <p>
     * 오류 또는 null.
     */
    private final XmlParseError error;

    /**
     * The system identifiers that the document asked for and that were refused (external DTD, external entities).
     * <p>
     * 문서가 요청했지만 거부된 시스템 식별자(외부 DTD, 외부 엔티티).
     */
    private final List<String> refusedReferences;

    /**
     * The XHTML root flag.
     * <p>
     * XHTML 루트 여부.
     */
    private final boolean xhtmlRoot;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param document          the document / 문서
     * @param text              the decoded text / 디코딩한 텍스트
     * @param error             the error or null / 오류 또는 null
     * @param refusedReferences refused external references / 거부한 외부 참조
     * @param xhtmlRoot         true if the root element is XHTML <code>html</code> / 루트 요소가 XHTML <code>html</code> 이면 true
     */
    XhtmlResult(Document document, XmlText text, XmlParseError error, List<String> refusedReferences, boolean xhtmlRoot) {
        this.document = document;
        this.text = text;
        this.error = error;
        this.refusedReferences = Collections.unmodifiableList(refusedReferences);
        this.xhtmlRoot = xhtmlRoot;
    }

    /**
     * Returns the document (partial if {@link #getError()} is not null).
     * <p>
     * 문서를 반환한다({@link #getError()} 가 null 이 아니면 일부만 있다).
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the decoded text of the document, unchanged (line ends and the XML declaration included).
     * <p>
     * 디코딩한 문서 텍스트를 바꾸지 않고 반환한다(줄 끝과 XML 선언 포함).
     *
     * @return the received source / 수신 원문
     */
    public String getSource() {
        return text.getText();
    }

    /**
     * Returns the decoding result with the encoding decision.
     * <p>
     * 인코딩 판정이 담긴 디코딩 결과를 반환한다.
     *
     * @return the decoding result / 디코딩 결과
     */
    public XmlText getText() {
        return text;
    }

    /**
     * Returns the error, or null if the document is well-formed and within the limits.
     * <p>
     * 문서가 well-formed 이고 상한 안이면 null, 아니면 오류를 반환한다.
     *
     * @return the error or null / 오류 또는 null
     */
    public XmlParseError getError() {
        return error;
    }

    /**
     * Tells whether the document is well-formed and within the limits.
     * <p>
     * 문서가 well-formed 이고 상한 안인지 알려 준다.
     *
     * @return true if there is no error / 오류가 없으면 true
     */
    public boolean isWellFormed() {
        return error == null;
    }

    /**
     * Returns the system identifiers of external DTDs and external entities that the document asked for. None of them was loaded;
     * this list exists so that a host can show a diagnostic.
     * <p>
     * 문서가 요청한 외부 DTD 와 외부 엔티티의 시스템 식별자를 반환한다. 어느 것도 읽지 않았다. 호스트가 진단을 보여 줄 수 있도록 있는 목록이다.
     *
     * @return the refused identifiers / 거부한 식별자
     */
    public List<String> getRefusedReferences() {
        return refusedReferences;
    }

    /**
     * Tells whether the root element is <code>html</code> in the XHTML namespace (a document that is well-formed XML but has another
     * root is shown as a generic XML document).
     * <p>
     * 루트 요소가 XHTML 네임스페이스의 <code>html</code> 인지 알려 준다(well-formed XML 이지만 루트가 다른 문서는 일반 XML 문서로 보인다).
     *
     * @return true for an XHTML root / XHTML 루트이면 true
     */
    public boolean isXhtmlRoot() {
        return xhtmlRoot;
    }
}
