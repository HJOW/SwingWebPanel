package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;

/**
 * The script side of the image map object model (HTML5 2014 REC section 4.7.11): <code>HTMLMapElement.areas</code>, the live collection
 * of the <code>area</code> elements that are descendants of the map, and <code>HTMLMapElement.images</code>, the live collection of the
 * <code>img</code> and <code>object</code> elements of the document that use the map through their <code>usemap</code> attribute. The
 * <code>name</code> reflection is defined with the interface.
 * <p>
 * 이미지 맵 객체 모델(HTML5 2014 REC 4.7.11절)의 스크립트 쪽: 맵의 후손 <code>area</code> 요소의 라이브 컬렉션인 <code>HTMLMapElement.areas</code> 와 <code>usemap</code>
 * 속성으로 맵을 쓰는 문서의 <code>img</code>, <code>object</code> 요소의 라이브 컬렉션인 <code>HTMLMapElement.images</code>. <code>name</code> 반영은 인터페이스를
 * 정의할 때 함께 정의한다.
 */
final class MapBindings {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private MapBindings() {
    }

    /**
     * Defines <code>areas</code> and <code>images</code> on the prototype of <code>HTMLMapElement</code>; the interface has to be defined.
     * <p>
     * <code>HTMLMapElement</code> 의 프로토타입에 <code>areas</code> 와 <code>images</code> 를 정의한다. 인터페이스가 먼저 정의되어 있어야 한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void install(final DomBridge bridge) {
        JsObject proto = bridge.getInterface("HTMLMapElement").getPrototype();
        defineCollection(bridge, proto, "areas", true);
        defineCollection(bridge, proto, "images", false);
    }

    /**
     * Defines one read-only accessor that returns a live collection of the map.
     * <p>
     * 맵의 라이브 컬렉션을 돌려주는 읽기 전용 접근자 하나를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param name   the property name / 프로퍼티 이름
     * @param areas  true for the areas of the map, false for the images that use it / 맵의 area 이면 true, 맵을 쓰는 이미지이면 false
     */
    private static void defineCollection(final DomBridge bridge, JsObject proto, String name, final boolean areas) {
        JsRealm realm = bridge.getRealm();
        JsObject get = realm.newFunction("get " + name, 0, new NativeFunction.Call() {
            /**
             * Returns the collection of the map.
             * <p>
             * 맵의 컬렉션을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element map = (Element) DomBridge.targetOf(self, Element.class);
                if (map == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return bridge.toJs(areas ? ElementCollections.mapAreas(map) : ElementCollections.mapImages(map));
            }
        });
        proto.defineAccessor(name, get, null, true, true);
    }
}
