package org.duckdns.hjow.swingwebpanel.render;

import java.io.UnsupportedEncodingException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.XmlSerializer;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgImage;

/**
 * Draws an inline <code>&lt;svg&gt;</code> element (an element of the SVG namespace in an HTML or XHTML document) with the SVG renderer of
 * the image decoders: the subtree is serialized as an XML document and rasterized at the size of the box. What the SVG image decoder supports
 * is drawn (shapes, paths, text, gradients, patterns, clipping, masks, filters, <code>use</code>, <code>style</code> elements and attributes
 * inside the subtree), and the same limits apply: nothing outside the subtree is read, no external resource is loaded and no script runs.
 * Limits of this approach: the style sheets of the page do not apply to the elements of the subtree (only presentation attributes, the
 * <code>style</code> attribute and <code>style</code> elements inside the SVG), the inner elements are not hit tested separately (the click
 * target is the <code>svg</code> element), and animation is not run (the first state is drawn).
 * <p>
 * 인라인 <code>&lt;svg&gt;</code> 요소(HTML 이나 XHTML 문서의 SVG 네임스페이스 요소)를 이미지 디코더의 SVG 렌더러로 그린다: 하위 트리를 XML 문서로
 * 직렬화하여 상자 크기로 래스터화한다. SVG 이미지 디코더가 지원하는 것(도형, 경로, 문자, 그라디언트, 패턴, 클리핑, 마스크, 필터, <code>use</code>, 하위 트리 안의
 * <code>style</code> 요소와 속성)을 그리고 같은 제한이 적용된다: 하위 트리 밖은 읽지 않고, 외부 리소스를 읽지 않으며, 스크립트를 실행하지 않는다. 이 방식의
 * 한계: 페이지의 스타일시트는 하위 트리의 요소에 적용되지 않고(표현 속성, <code>style</code> 속성, SVG 안의 <code>style</code> 요소만), 안쪽 요소는 따로 히트
 * 테스트하지 않으며(클릭 대상은 <code>svg</code> 요소), 애니메이션은 실행하지 않는다(첫 상태를 그린다).
 */
final class InlineSvg {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private InlineSvg() {
    }

    /**
     * Tells whether an element is an inline SVG root: an <code>svg</code> element of the SVG namespace.
     * <p>
     * 요소가 인라인 SVG 루트인지 알려 준다: SVG 네임스페이스의 <code>svg</code> 요소.
     *
     * @param e the element / 요소
     * @return true for an inline SVG root / 인라인 SVG 루트이면 true
     */
    static boolean isRoot(Element e) {
        return "svg".equals(e.getLocalName()) && Namespaces.SVG.equals(e.getNamespaceURI());
    }

    /**
     * Rasterizes an inline SVG element at the given size.
     * <p>
     * 인라인 SVG 요소를 주어진 크기로 래스터화한다.
     *
     * @param svg    the root element / 루트 요소
     * @param width  the width in pixels (at least 1) / 너비(픽셀, 1 이상)
     * @param height the height in pixels (at least 1) / 높이(픽셀, 1 이상)
     * @return the image, or null if the subtree cannot be drawn (not well formed, too large) / 이미지. 하위 트리를 그릴 수 없으면(올바르지 않거나 너무 큼) null
     */
    static DecodedImage rasterize(Element svg, int width, int height) {
        try {
            String xml = XmlSerializer.serialize(svg);
            SvgImage image = SvgImage.parse(xml.getBytes("UTF-8"), DecodeLimits.DEFAULT);
            return new DecodedImage(width, height, image.renderArgb(width, height));
        } catch (MediaException e) {
            // 그릴 수 없는 SVG 는 아무것도 그리지 않는다(상자 크기와 배경은 그대로이다).
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (RuntimeException e) {
            // 렌더러의 예기치 않은 오류가 페이지 그리기를 막지 않게 한다.
            return null;
        }
    }
}
