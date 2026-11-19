package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Reads the <code>CanvasRenderingContext2DSettings</code> dictionary that <code>getContext("2d", settings)</code> takes (HTML Standard 4.12.5.1.2
 * "The canvas settings"): <code>alpha</code> (false makes the canvas opaque black at the start), <code>colorSpace</code> (<code>srgb</code> or
 * <code>display-p3</code>: both are accepted, an unknown name is a <code>TypeError</code>, and the bitmap stays sRGB), <code>desynchronized</code>
 * and <code>willReadFrequently</code> (hints that are only reported back by <code>getContextAttributes()</code>). Only the first call that creates
 * the context reads the settings. An argument that is not an object is the same as no settings.
 * <p>
 * <code>getContext("2d", settings)</code> 이 받는 <code>CanvasRenderingContext2DSettings</code> 사전을 읽는다(HTML 표준 4.12.5.1.2 "The canvas settings"):
 * <code>alpha</code>(false 이면 캔버스가 불투명한 검정으로 시작), <code>colorSpace</code>(<code>srgb</code> 또는 <code>display-p3</code>. 둘 다 받아들이고 모르는
 * 이름은 <code>TypeError</code> 이며 비트맵은 sRGB 로 남는다), <code>desynchronized</code>, <code>willReadFrequently</code>(<code>getContextAttributes()</code>
 * 가 되돌려 줄 뿐인 힌트). 컨텍스트를 만드는 첫 호출만 설정을 읽는다. 객체가 아닌 인수는 설정이 없는 것과 같다.
 */
final class ContextSettings {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ContextSettings() {
    }

    /**
     * Applies the settings to the surface whose context was just created.
     * <p>
     * 방금 컨텍스트를 만든 표면에 설정을 적용한다.
     *
     * @param s       the surface / 표면
     * @param options the second argument of <code>getContext</code> / <code>getContext</code> 의 두 번째 인수
     * @throws JsException TypeError if <code>colorSpace</code> is not an allowed name / <code>colorSpace</code> 가 허용된 이름이 아니면 TypeError
     */
    static void apply(CanvasSurface s, JsValue options) {
        if (options == null || !options.isObject()) {
            return;
        }
        JsObject o = (JsObject) options;
        JsValue space = o.get("colorSpace");
        // colorSpace 는 열거형이므로 모르는 이름은 TypeError 이다.
        if (!space.isUndefined()) {
            String name = Ops.toString(space);
            if (!"srgb".equals(name) && !"display-p3".equals(name)) {
                throw JsException.typeError("Failed to execute 'getContext': The provided value '" + name
                        + "' is not a valid enum value of type PredefinedColorSpace.");
            }
        }
        JsValue alpha = o.get("alpha");
        if (!alpha.isUndefined() && !Ops.toBoolean(alpha)) {
            s.setOpaque(true);
        }
        JsValue desync = o.get("desynchronized");
        JsValue willRead = o.get("willReadFrequently");
        s.setHints(!willRead.isUndefined() && Ops.toBoolean(willRead), !desync.isUndefined() && Ops.toBoolean(desync));
    }
}
