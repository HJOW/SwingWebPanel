package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Tells the layout engine what external content a replaced element has (the loaded image, the media placeholder, the form control ...).
 * Elements that the engine treats as replaced (<code>img video audio canvas iframe embed object input select textarea svg</code>) fall back
 * to their <code>width</code>/<code>height</code> attributes and to built-in default sizes when the resolver returns null.
 * <p>
 * 치환 요소가 가진 외부 콘텐츠(읽은 이미지, 미디어 표시, 폼 컨트롤 등)를 레이아웃 엔진에 알려 준다. 엔진이 치환 요소로 다루는 요소(<code>img video
 * audio canvas iframe embed object input select textarea svg</code>)는 리졸버가 null 을 돌려주면 <code>width</code>/<code>height</code> 속성과
 * 내장 기본 크기를 쓴다.
 */
public interface ReplacedResolver {

    /**
     * Returns the content of a replaced element.
     * <p>
     * 치환 요소의 콘텐츠를 반환한다.
     *
     * @param element the element / 요소
     * @return the content, or null if nothing is known about it / 콘텐츠. 알려진 것이 없으면 null
     */
    ReplacedContent getReplacedContent(Element element);
}
