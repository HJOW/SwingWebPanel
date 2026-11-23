package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Empty implementation of {@link ParserListener} to subclass when only some callbacks are of interest.
 * <p>
 * 일부 콜백에만 관심이 있을 때 상속하는 {@link ParserListener} 의 빈 구현.
 */
public class ParserListenerAdapter implements ParserListener {

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param error the error / 오류
     */
    public void parseError(ParseError error) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param mode document mode / 문서 모드
     */
    public void documentModeDetermined(int mode) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param element the element / 요소
     */
    public void elementInserted(Element element) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param element the element / 요소
     */
    public void elementClosed(Element element) {
    }

    /**
     * Does nothing: the script is not executed.
     * <p>
     * 아무것도 하지 않는다: 스크립트는 실행되지 않는다.
     *
     * @param script  the script element / script 요소
     * @param control control of the parser / 파서 제어
     */
    public void scriptReady(Element script, ParserControl control) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param style the style element / style 요소
     */
    public void styleReady(Element style) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param link the link element / link 요소
     */
    public void stylesheetFound(Element link) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param img the img element / img 요소
     */
    public void imageFound(Element img) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param href     attribute value / 속성 값
     * @param resolved resolved URL / 해석된 URL
     */
    public void baseHrefChanged(String href, String resolved) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param content content attribute value / content 속성 값
     */
    public void metaRefresh(String content) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param title title text / 제목 텍스트
     */
    public void titleReady(String title) {
    }

    /**
     * Does nothing.
     * <p>
     * 아무것도 하지 않는다.
     *
     * @param encodingName encoding name / 인코딩 이름
     */
    public void charsetDeclared(String encodingName) {
    }
}
