package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.storage.SessionStorage;

/**
 * What the pages of one component share about workers (plan item P20-09-b): the broadcast hub of every origin (the
 * <code>BroadcastChannel</code> objects of the same origin in all pages of the component and in their workers hear each other, as the HTML
 * Standard "Broadcasting to other browsing contexts" says) and the shared workers (a <code>SharedWorker</code> that names the same script URL
 * and the same name from the same origin gets the one shared worker, whichever page asks). The registry is kept in a slot of the
 * <code>SessionStorage</code> of the component, so it lives and dies with the component: no static variable holds it, and nothing of it is
 * visible to another component. All methods may be called from any thread.
 * <p>
 * 컴포넌트의 페이지들이 worker 에 관해 공유하는 것(계획 P20-09-b): 오리진별 브로드캐스트 허브(컴포넌트의 모든 페이지와 그 worker 에 있는 같은 오리진의
 * <code>BroadcastChannel</code> 객체는 HTML 표준 "Broadcasting to other browsing contexts" 대로 서로 듣는다)와 공유 worker(같은 오리진에서 같은 스크립트 URL과
 * 같은 이름을 쓰는 <code>SharedWorker</code> 는 어느 페이지가 요청해도 하나의 공유 worker 를 얻는다). 등록부는 컴포넌트의 <code>SessionStorage</code> 슬롯에
 * 있으므로 컴포넌트와 운명을 같이 하며, 정적 변수가 붙잡지 않고, 다른 컴포넌트에는 보이지 않는다. 모든 메서드는 어느 스레드에서나 부를 수 있다.
 */
final class WorkerRegistry {

    /**
     * The name of the slot of the component that holds the registry.
     * <p>
     * 등록부를 가진 컴포넌트 슬롯의 이름.
     */
    private static final String SLOT = "org.duckdns.hjow.swingwebpanel.js.worker.WorkerRegistry";

    /**
     * The largest number of shared workers that live at the same time in one component.
     * <p>
     * 컴포넌트 하나에서 동시에 살아 있는 공유 worker 의 최대 수.
     */
    static final int MAX_SHARED_WORKERS = 16;

    /**
     * The broadcast hubs by serialized origin; guarded by itself.
     * <p>
     * 직렬화된 오리진별 브로드캐스트 허브. 자기 자신으로 보호한다.
     */
    private final Map<String, BroadcastHub> hubs = new HashMap<String, BroadcastHub>();

    /**
     * The shared workers that live; guarded by {@link #hubs}.
     * <p>
     * 살아 있는 공유 worker. {@link #hubs} 로 보호한다.
     */
    private final List<SharedEntry> shared = new ArrayList<SharedEntry>();

    /**
     * Creates an empty registry.
     * <p>
     * 빈 등록부를 만든다.
     */
    private WorkerRegistry() {
    }

    /**
     * Returns the registry of the component of an environment; an environment without a component storage gets a registry of its own.
     * <p>
     * 환경의 컴포넌트의 등록부를 반환한다. 컴포넌트 저장소가 없는 환경은 자기만의 등록부를 얻는다.
     *
     * @param env the environment of a page / 페이지의 환경
     * @return the registry / 등록부
     */
    static WorkerRegistry of(HostEnvironment env) {
        SessionStorage storage = env.getSessionStorage();
        if (storage == null) {
            return new WorkerRegistry();
        }
        return (WorkerRegistry) storage.getSlot(SLOT, new SessionStorage.SlotFactory() {
            /**
             * Creates the registry of a component.
             * <p>
             * 컴포넌트의 등록부를 만든다.
             *
             * @return the registry / 등록부
             */
            public Object create() {
                return new WorkerRegistry();
            }
        });
    }

    /**
     * Returns the broadcast hub of an origin; a document with an opaque origin gets a hub that nobody else can reach.
     * <p>
     * 오리진의 브로드캐스트 허브를 반환한다. 불투명 오리진의 문서는 아무도 닿을 수 없는 허브를 얻는다.
     *
     * @param origin the origin of the document / 문서의 오리진
     * @return the hub / 허브
     */
    BroadcastHub hubFor(Origin origin) {
        if (origin.isOpaque()) {
            return new BroadcastHub();
        }
        String key = origin.serialize();
        synchronized (hubs) {
            BroadcastHub h = hubs.get(key);
            if (h == null) {
                h = new BroadcastHub();
                hubs.put(key, h);
            }
            return h;
        }
    }

    /**
     * Finds the live shared worker with a constructor key, or null.
     * <p>
     * 생성자 키가 같은 살아 있는 공유 worker 를 찾는다. 없으면 null.
     *
     * @param origin the origin of the constructor / 생성자의 오리진
     * @param url    the script URL / 스크립트 URL
     * @param name   the name / 이름
     * @return the entry or null / 항목 또는 null
     */
    SharedEntry findShared(String origin, String url, String name) {
        synchronized (hubs) {
            // 닫힌 공유 worker 는 목록에서 정리하고, 키가 같은 살아 있는 것을 찾는다.
            for (int i = shared.size() - 1; i >= 0; i--) {
                SharedEntry e = shared.get(i);
                if (!e.isAlive()) {
                    shared.remove(i);
                } else if (e.matches(origin, url, name)) {
                    return e;
                }
            }
            return null;
        }
    }

    /**
     * Adds a new shared worker.
     * <p>
     * 새 공유 worker 를 더한다.
     *
     * @param entry the entry / 항목
     * @return false if there are too many shared workers / 공유 worker 가 너무 많으면 false
     */
    boolean addShared(SharedEntry entry) {
        synchronized (hubs) {
            // 끝난 공유 worker 는 세지 않도록 먼저 목록에서 정리한다.
            for (int i = shared.size() - 1; i >= 0; i--) {
                if (!shared.get(i).isAlive()) {
                    shared.remove(i);
                }
            }
            if (shared.size() >= MAX_SHARED_WORKERS) {
                return false;
            }
            shared.add(entry);
            return true;
        }
    }

    /**
     * Tells that a page ended: it stops owning shared workers, and a shared worker without an owner is terminated and forgotten.
     * <p>
     * 페이지가 끝났음을 알린다: 페이지는 공유 worker 의 소유자에서 빠지고, 소유자가 없어진 공유 worker 는 종료되고 잊혀진다.
     *
     * @param owner the page that ended / 끝난 페이지
     */
    void release(WorkerApi owner) {
        List<SharedEntry> dead = new ArrayList<SharedEntry>();
        synchronized (hubs) {
            // 모든 공유 worker 에서 이 페이지를 소유자에서 빼고, 소유자가 없어진 것을 모은다.
            for (int i = shared.size() - 1; i >= 0; i--) {
                SharedEntry e = shared.get(i);
                e.removeOwner(owner);
                if (!e.hasOwners() || !e.isAlive()) {
                    dead.add(shared.remove(i));
                }
            }
        }
        // 잠금 밖에서 종료한다.
        for (int i = 0; i < dead.size(); i++) {
            dead.get(i).terminate();
        }
    }

    /**
     * Returns the number of shared workers that live.
     * <p>
     * 살아 있는 공유 worker 의 수를 반환한다.
     *
     * @return the count / 개수
     */
    int sharedCount() {
        int n = 0;
        synchronized (hubs) {
            // 목록에서 살아 있는 것을 센다.
            for (int i = 0; i < shared.size(); i++) {
                if (shared.get(i).isAlive()) {
                    n++;
                }
            }
        }
        return n;
    }
}
