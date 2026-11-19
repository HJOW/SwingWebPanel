package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * An optional extension of a {@link ReplacedResolver} for the elements that cannot show their content: it gives the text that an
 * <code>img</code> element without a usable image stands for (HTML5 REC 2014-10-28 section 10.4.2, https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#images).
 * An image that is broken and has no dimensions of its own is laid out as a non-replaced inline element whose content is that text,
 * instead of a replaced box of size zero that shows nothing; an image that has dimensions keeps its replaced box, which the painter
 * fills with the same text.
 * <p>
 * 내용을 보여 줄 수 없는 요소를 위한 {@link ReplacedResolver} 의 선택적 확장: 쓸 수 있는 그림이 없는 <code>img</code> 요소가 대신하는 텍스트를 준다(HTML5 REC 2014-10-28
 * §10.4.2). 깨졌고 자기 크기가 없는 이미지는 아무것도 보이지 않는 크기 0 치환 상자 대신 그 텍스트를 내용으로 하는 비치환 인라인 요소로 배치하고, 크기가 있는 이미지는
 * 치환 상자를 유지하며 그리는 쪽이 같은 텍스트로 채운다.
 */
public interface ReplacedFallback {

    /**
     * Returns the text that a broken <code>img</code> element stands for.
     * <p>
     * 깨진 <code>img</code> 요소가 대신하는 텍스트를 반환한다.
     *
     * @param element the <code>img</code> element / <code>img</code> 요소
     * @return the text, or null if the image is loaded, still loading, or has nothing to show / 텍스트. 이미지가 로딩을 마쳤거나 로딩 중이거나 보여 줄 것이 없으면 null
     */
    String getFallbackText(Element element);
}
