package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * The Location host object, including the view exposed to another origin.
 * <p>
 * 다른 오리진이 보는 표현을 포함하는 Location 호스트 객체.
 */
final class LocationObject extends JsObject {

    /**
     * Creates a location in the caller's realm.
     * <p>
     * 호출자의 렐름에 Location을 만든다.
     *
     * @param realm the owning realm / 소유 렐름
     */
    LocationObject(JsRealm realm) {
        super(realm.getObjectPrototype());
    }

    /**
     * Rejects a private element addition per HTML HostEnsureCanAddPrivateElement.
     * <p>
     * HTML HostEnsureCanAddPrivateElement에 따라 private 요소 추가를 거부한다.
     */
    @Override
    public void ensureCanAddPrivateElement() {
        throw JsException.typeError("Cannot add a private element to Location");
    }
}
