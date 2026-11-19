package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * One shared worker of a component (HTML Standard "Shared workers and the SharedWorker interface"): the worker itself ({@link WorkerScope}
 * with the interface <code>SharedWorkerGlobalScope</code>), the key that finds it again (the origin of the constructor, the script URL and
 * the name), the pages that own it (it ends when the last owner ended, or when it closes itself) and the <code>SharedWorker</code> objects
 * that are connected to it, which it tells about a script that could not be loaded. It is also the {@link WorkerParent} of the worker: the
 * console output and the errors of a shared worker go to the console of an owner, because a shared worker has no <code>Worker</code> object
 * to receive <code>error</code> events (the specification gives the user agent the console for that).
 * <p>
 * 컴포넌트의 공유 worker 하나(HTML 표준 "Shared workers and the SharedWorker interface"): worker 자신(<code>SharedWorkerGlobalScope</code> 인터페이스를 가진
 * {@link WorkerScope}), 다시 찾는 키(생성자의 오리진, 스크립트 URL, 이름), 이것을 소유한 페이지(마지막 소유자가 끝나거나 스스로 닫으면 끝난다), 연결된
 * <code>SharedWorker</code> 객체(스크립트를 가져오지 못했을 때 알려 준다)이다. worker 의 {@link WorkerParent} 이기도 하다: 공유 worker 에는 <code>error</code>
 * 이벤트를 받을 <code>Worker</code> 객체가 없으므로 콘솔 출력과 오류는 소유자의 콘솔로 간다(규격은 그것을 사용자 에이전트의 콘솔에 맡긴다).
 */
final class SharedEntry implements WorkerParent {

    /**
     * The origin of the page that created the shared worker (serialized).
     * <p>
     * 공유 worker 를 만든 페이지의 오리진(직렬화).
     */
    private final String origin;

    /**
     * The absolute URL of the script.
     * <p>
     * 스크립트의 절대 URL.
     */
    private final String url;

    /**
     * The name of the shared worker.
     * <p>
     * 공유 worker 의 이름.
     */
    private final String name;

    /**
     * The worker; set once right after creation.
     * <p>
     * worker. 만든 직후 한 번 지정한다.
     */
    private volatile WorkerScope scope;

    /**
     * The pages that own the worker; guarded by itself.
     * <p>
     * worker 를 소유한 페이지. 자기 자신으로 보호한다.
     */
    private final List<WorkerApi> owners = new ArrayList<WorkerApi>();

    /**
     * The <code>SharedWorker</code> objects that are connected; guarded by {@link #owners}.
     * <p>
     * 연결된 <code>SharedWorker</code> 객체. {@link #owners} 로 보호한다.
     */
    private final List<SharedHandle> handles = new ArrayList<SharedHandle>();

    /**
     * Creates the entry of a shared worker that has no worker yet.
     * <p>
     * 아직 worker 가 없는 공유 worker 의 항목을 만든다.
     *
     * @param origin the origin of the constructor / 생성자의 오리진
     * @param url    the script URL / 스크립트 URL
     * @param name   the name / 이름
     */
    SharedEntry(String origin, String url, String name) {
        this.origin = origin;
        this.url = url;
        this.name = name;
    }

    /**
     * Sets the worker of the entry (once, before it starts).
     * <p>
     * 항목의 worker 를 지정한다(시작하기 전에 한 번).
     *
     * @param s the worker / worker
     */
    void attach(WorkerScope s) {
        this.scope = s;
    }

    /**
     * Returns the worker.
     * <p>
     * worker 를 반환한다.
     *
     * @return the worker / worker
     */
    WorkerScope getScope() {
        return scope;
    }

    /**
     * Tells whether the key of the constructor is the key of this shared worker.
     * <p>
     * 생성자의 키가 이 공유 worker 의 키인지 알려 준다.
     *
     * @param o the origin / 오리진
     * @param u the script URL / 스크립트 URL
     * @param n the name / 이름
     * @return true if they are equal / 같으면 true
     */
    boolean matches(String o, String u, String n) {
        return origin.equals(o) && url.equals(u) && name.equals(n);
    }

    /**
     * Tells whether the worker still runs (it was not terminated and did not close itself).
     * <p>
     * worker 가 아직 실행 중인지(종료되지 않았고 스스로 닫지 않았는지) 알려 준다.
     *
     * @return true while it runs / 실행 중이면 true
     */
    boolean isAlive() {
        WorkerScope s = scope;
        return s != null && s.isActive();
    }

    /**
     * Adds a connection: the page becomes an owner (once), and its <code>SharedWorker</code> object is remembered.
     * <p>
     * 연결을 더한다: 페이지는 소유자가 되고(한 번만) 그 페이지의 <code>SharedWorker</code> 객체를 기억한다.
     *
     * @param owner  the page / 페이지
     * @param handle its <code>SharedWorker</code> object / 그 페이지의 <code>SharedWorker</code> 객체
     */
    void addConnection(WorkerApi owner, SharedHandle handle) {
        synchronized (owners) {
            if (!owners.contains(owner)) {
                owners.add(owner);
            }
            handles.add(handle);
        }
    }

    /**
     * Removes a page from the owners together with its connections.
     * <p>
     * 페이지를 연결과 함께 소유자에서 뺀다.
     *
     * @param owner the page / 페이지
     */
    void removeOwner(WorkerApi owner) {
        synchronized (owners) {
            owners.remove(owner);
            // 이 페이지의 연결을 목록에서 뺀다.
            for (int i = handles.size() - 1; i >= 0; i--) {
                if (handles.get(i).getOwner() == owner) {
                    handles.remove(i);
                }
            }
        }
    }

    /**
     * Tells whether a page still owns the shared worker.
     * <p>
     * 페이지가 아직 공유 worker 를 소유하는지 알려 준다.
     *
     * @return true if there is an owner / 소유자가 있으면 true
     */
    boolean hasOwners() {
        synchronized (owners) {
            return !owners.isEmpty();
        }
    }

    /**
     * Terminates the worker (the last owner ended, or the component ends).
     * <p>
     * worker 를 종료한다(마지막 소유자가 끝났거나 컴포넌트가 끝난다).
     */
    void terminate() {
        WorkerScope s = scope;
        if (s != null) {
            s.terminate();
        }
    }

    /**
     * Returns a copy of the connected objects.
     * <p>
     * 연결된 객체의 복사본을 반환한다.
     *
     * @return the handles / 연결 객체
     */
    private List<SharedHandle> snapshot() {
        synchronized (owners) {
            return new ArrayList<SharedHandle>(handles);
        }
    }

    /**
     * A shared worker has no implicit port, so there is nothing to forward.
     * <p>
     * 공유 worker 에는 암묵 포트가 없으므로 전달할 것이 없다.
     *
     * @param data the serialized message / 직렬화된 메시지
     */
    public void messageFromWorker(ClonedValue data) {
        return;
    }

    /**
     * An uncaught error of the shared worker goes to the console of a page that owns it.
     * <p>
     * 공유 worker 의 잡히지 않은 오류는 소유한 페이지의 콘솔로 간다.
     *
     * @param message  the message / 메시지
     * @param filename the script URL / 스크립트 URL
     * @param line     the line / 줄
     * @param column   the column / 열
     */
    public void errorFromWorker(String message, String filename, int line, int column) {
        console("error", "Uncaught " + message + " (shared worker " + filename + ":" + line + ":" + column + ")");
    }

    /**
     * The script could not be loaded: every connected <code>SharedWorker</code> gets an <code>error</code> event.
     * <p>
     * 스크립트를 가져올 수 없다: 연결된 모든 <code>SharedWorker</code> 가 <code>error</code> 이벤트를 받는다.
     *
     * @param reason a short description / 짧은 설명
     */
    public void loadFailed(String reason) {
        List<SharedHandle> all = snapshot();
        // 연결된 객체마다 오류를 알린다.
        for (int i = 0; i < all.size(); i++) {
            all.get(i).loadFailed(reason);
        }
    }

    /**
     * Console output goes to the console of the first connected page that is still there.
     * <p>
     * 콘솔 출력은 아직 있는 첫 번째 연결된 페이지의 콘솔로 간다.
     *
     * @param level the level / 수준
     * @param text  the text / 문자열
     */
    public void console(String level, String text) {
        List<SharedHandle> all = snapshot();
        // 콘솔을 쓸 수 있는 첫 연결을 찾는다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).console(level, text)) {
                return;
            }
        }
    }
}
