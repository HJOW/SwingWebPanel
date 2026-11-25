package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The agent-cluster and origin identity used for sharing buffers across browser realms.
 * <p>
 * 브라우저 realm 사이에서 버퍼를 공유할 때 사용하는 에이전트 클러스터와 오리진 식별자.
 */
public final class SharedMemoryScope {

    /** Scopes of live realms. A scope contains no reference to a realm. / 살아 있는 realm의 범위. 범위는 realm을 참조하지 않는다. */
    private static final Map<JsRealm, SharedMemoryScope> SCOPES = new WeakHashMap<JsRealm, SharedMemoryScope>();

    /** Host agent-cluster identity. / 호스트 에이전트 클러스터 식별자. */
    private final Object cluster;

    /** Effective origin of the realm. / realm의 유효 오리진. */
    private final Origin origin;

    /**
     * Records the already assigned identity for one realm.
     * <p>
     * 한 realm에 이미 부여된 식별자를 기록한다.
     *
     * @param cluster host cluster token / 호스트 클러스터 토큰
     * @param origin effective origin / 유효 오리진
     */
    private SharedMemoryScope(Object cluster, Origin origin) {
        this.cluster = cluster;
        this.origin = origin;
    }

    /**
     * Binds a host-owned realm to a cluster and its effective origin at creation time.
     * Use a standalone token for the cluster so that queued messages retain no host object.
     * <p>
     * 호스트가 소유한 realm을 만들 때 클러스터와 유효 오리진을 지정한다.
     * 대기 중인 메시지가 호스트 객체를 유지하지 않도록 독립 토큰을 사용한다.
     *
     * @param realm browser realm / 브라우저 realm
     * @param cluster stable host cluster token / 고정된 호스트 클러스터 토큰
     * @param origin effective origin / 유효 오리진
     */
    public static void bind(JsRealm realm, Object cluster, Origin origin) {
        if (realm == null || cluster == null || origin == null) {
            throw new IllegalArgumentException("Realm, cluster and origin are required");
        }
        synchronized (SCOPES) {
            if (SCOPES.containsKey(realm)) {
                throw new IllegalStateException("The realm already has a shared memory scope");
            }
            SCOPES.put(realm, new SharedMemoryScope(cluster, origin));
        }
    }

    /**
     * Finds the scope of a realm, if the browser host has assigned one.
     * <p>
     * 브라우저 호스트가 범위를 지정했으면 realm의 범위를 찾는다.
     *
     * @param realm the realm / 찾을 realm
     * @return the scope or null / 범위 또는 null
     */
    static SharedMemoryScope of(JsRealm realm) {
        synchronized (SCOPES) {
            return SCOPES.get(realm);
        }
    }

    /**
     * Tests whether another realm may receive shared bytes from this scope.
     * <p>
     * 다른 realm이 이 범위의 공유 바이트를 받을 수 있는지 검사한다.
     *
     * @param receiver the receiving scope or null / 받는 쪽 범위 또는 null
     * @return true for the same realm or a matching cluster and nonopaque origin / 같은 realm 또는 같은 클러스터의 불투명하지 않은 오리진이면 true
     */
    boolean permits(SharedMemoryScope receiver) {
        return this == receiver || (receiver != null && cluster == receiver.cluster && !origin.isOpaque() && origin.equals(receiver.origin));
    }
}
