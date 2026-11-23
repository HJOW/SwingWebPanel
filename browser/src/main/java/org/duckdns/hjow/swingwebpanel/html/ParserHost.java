package org.duckdns.hjow.swingwebpanel.html;

/**
 * Callback from the tree builder to the parser that owns it, for decisions that concern the whole parse and not the tree: a
 * <code>meta</code> element that declares a character encoding.
 * <p>
 * 트리 생성기가 자기를 소유한 파서에 보내는 콜백. 트리가 아니라 파싱 전체에 관한 결정(문자 인코딩을 선언하는 <code>meta</code> 요소)을 위한 것이다.
 */
interface ParserHost {

    /**
     * Called when a <code>meta</code> element declares an encoding. The host may decide to restart the parse with that encoding
     * ("change the encoding").
     * <p>
     * <code>meta</code> 요소가 인코딩을 선언했을 때 호출된다. 호스트는 그 인코딩으로 파싱을 다시 시작하기로 결정할 수 있다("change the encoding").
     *
     * @param canonicalName canonical encoding name / 표준 인코딩 이름
     * @return true if the parse must be abandoned and restarted / 파싱을 중단하고 다시 시작해야 하면 true
     */
    boolean metaCharset(String canonicalName);
}
