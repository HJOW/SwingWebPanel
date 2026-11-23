package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttRegion;

/**
 * The script object of a WebVTT region (<code>VTTRegion</code>, WebVTT 9.2): the getters and setters read and write the {@link VttRegion}
 * that it stands for. A number that is not finite is a <code>TypeError</code> (the attributes are restricted doubles); a percentage outside
 * the range 0 to 100 is an <code>IndexSizeError</code>. The class is public because the DOM bridge reads its accessors by reflection.
 * <p>
 * WebVTT region 의 스크립트 객체(<code>VTTRegion</code>, WebVTT 9.2): getter 와 setter 가 그것이 나타내는 {@link VttRegion} 을 읽고 쓴다. 유한하지 않은
 * 수는 <code>TypeError</code>(속성은 제한된 double)이고 0~100 밖의 백분율은 <code>IndexSizeError</code> 이다. DOM 브리지가 접근자를 리플렉션으로 읽으므로
 * 클래스는 public 이다.
 */
public final class VttRegionValue {

    /**
     * The region that this object stands for.
     * <p>
     * 이 객체가 나타내는 region.
     */
    final VttRegion data;

    /**
     * Creates the object for a region.
     * <p>
     * region 의 객체를 만든다.
     *
     * @param data the region / region
     */
    VttRegionValue(VttRegion data) {
        this.data = data;
    }

    /**
     * Checks that a number is finite as the attributes of the interface require.
     * <p>
     * 인터페이스의 속성이 요구하듯 수가 유한한지 확인한다.
     *
     * @param v    the number / 수
     * @param name the attribute for the message / 메시지용 속성 이름
     * @return the number / 수
     */
    private static double finite(double v, String name) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw JsException.typeError("Failed to set the '" + name + "' property on 'VTTRegion': The provided double value is non-finite.");
        }
        return v;
    }

    /**
     * Checks that a number is a percentage.
     * <p>
     * 수가 백분율인지 확인한다.
     *
     * @param v    the number / 수
     * @param name the attribute for the message / 메시지용 속성 이름
     * @return the number / 수
     */
    private static double percent(double v, String name) {
        finite(v, name);
        if (v < 0 || v > 100) {
            throw DomException.indexSize("Failed to set the '" + name + "' property on 'VTTRegion': The value provided (" + v + ") is outside the range (0, 100).");
        }
        return v;
    }

    /**
     * Returns <code>id</code>.
     * <p>
     * <code>id</code> 를 반환한다.
     *
     * @return the identifier / 식별자
     */
    public String getId() {
        return data.id;
    }

    /**
     * Sets <code>id</code>.
     * <p>
     * <code>id</code> 를 정한다.
     *
     * @param v the identifier / 식별자
     */
    public void setId(String v) {
        data.id = v;
    }

    /**
     * Returns <code>width</code>.
     * <p>
     * <code>width</code> 를 반환한다.
     *
     * @return the width in percent / 폭(백분율)
     */
    public double getWidth() {
        return data.width;
    }

    /**
     * Sets <code>width</code>.
     * <p>
     * <code>width</code> 를 정한다.
     *
     * @param v the width in percent / 폭(백분율)
     */
    public void setWidth(double v) {
        data.width = percent(v, "width");
    }

    /**
     * Returns <code>lines</code>.
     * <p>
     * <code>lines</code> 를 반환한다.
     *
     * @return the number of lines / 줄 수
     */
    public double getLines() {
        return data.lines;
    }

    /**
     * Sets <code>lines</code> (an unsigned long: the value is taken modulo 2 to the 32nd power).
     * <p>
     * <code>lines</code> 를 정한다(unsigned long: 값은 2의 32제곱으로 나눈 나머지).
     *
     * @param v the number of lines / 줄 수
     */
    public void setLines(double v) {
        double d = Double.isNaN(v) || Double.isInfinite(v) ? 0 : (v < 0 ? Math.ceil(v) : Math.floor(v));
        double m = d % 4294967296.0;
        data.lines = (long) (m < 0 ? m + 4294967296.0 : m);
    }

    /**
     * Returns <code>regionAnchorX</code>.
     * <p>
     * <code>regionAnchorX</code> 를 반환한다.
     *
     * @return the anchor in percent / 고정점(백분율)
     */
    public double getRegionAnchorX() {
        return data.regionAnchorX;
    }

    /**
     * Sets <code>regionAnchorX</code>.
     * <p>
     * <code>regionAnchorX</code> 를 정한다.
     *
     * @param v the anchor in percent / 고정점(백분율)
     */
    public void setRegionAnchorX(double v) {
        data.regionAnchorX = percent(v, "regionAnchorX");
    }

    /**
     * Returns <code>regionAnchorY</code>.
     * <p>
     * <code>regionAnchorY</code> 를 반환한다.
     *
     * @return the anchor in percent / 고정점(백분율)
     */
    public double getRegionAnchorY() {
        return data.regionAnchorY;
    }

    /**
     * Sets <code>regionAnchorY</code>.
     * <p>
     * <code>regionAnchorY</code> 를 정한다.
     *
     * @param v the anchor in percent / 고정점(백분율)
     */
    public void setRegionAnchorY(double v) {
        data.regionAnchorY = percent(v, "regionAnchorY");
    }

    /**
     * Returns <code>viewportAnchorX</code>.
     * <p>
     * <code>viewportAnchorX</code> 를 반환한다.
     *
     * @return the anchor in percent / 고정점(백분율)
     */
    public double getViewportAnchorX() {
        return data.viewportAnchorX;
    }

    /**
     * Sets <code>viewportAnchorX</code>.
     * <p>
     * <code>viewportAnchorX</code> 를 정한다.
     *
     * @param v the anchor in percent / 고정점(백분율)
     */
    public void setViewportAnchorX(double v) {
        data.viewportAnchorX = percent(v, "viewportAnchorX");
    }

    /**
     * Returns <code>viewportAnchorY</code>.
     * <p>
     * <code>viewportAnchorY</code> 를 반환한다.
     *
     * @return the anchor in percent / 고정점(백분율)
     */
    public double getViewportAnchorY() {
        return data.viewportAnchorY;
    }

    /**
     * Sets <code>viewportAnchorY</code>.
     * <p>
     * <code>viewportAnchorY</code> 를 정한다.
     *
     * @param v the anchor in percent / 고정점(백분율)
     */
    public void setViewportAnchorY(double v) {
        data.viewportAnchorY = percent(v, "viewportAnchorY");
    }

    /**
     * Returns <code>scroll</code>.
     * <p>
     * <code>scroll</code> 을 반환한다.
     *
     * @return the empty string or <code>up</code> / 빈 문자열 또는 <code>up</code>
     */
    public String getScroll() {
        return data.scroll;
    }

    /**
     * Sets <code>scroll</code>: a value that is not one of the keywords is ignored (an enumeration attribute).
     * <p>
     * <code>scroll</code> 을 정한다: 키워드가 아닌 값은 무시한다(열거형 속성).
     *
     * @param v the value / 값
     */
    public void setScroll(String v) {
        if ("".equals(v) || "up".equals(v)) {
            data.scroll = v;
        }
    }
}
