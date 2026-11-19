package org.duckdns.hjow.swingwebpanel.storage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A list of {@link StorageListener}s with delivery filtered by origin and by source. In weak mode the list holds only weak references, so a
 * process-wide storage manager never keeps a component or document alive (plan items P16-02, P21-05); dead references are dropped when the
 * list is used.
 * <p>
 * 오리진과 변경 주체로 전달을 거르는 {@link StorageListener} 목록. weak 모드에서는 약한 참조만 가지므로 프로세스 공용 저장소 관리자가 컴포넌트나
 * 문서를 살려 두지 않는다(계획 P16-02, P21-05). 죽은 참조는 목록을 쓸 때 버린다.
 */
final class StorageListeners {

    /**
     * Whether references are weak.
     * <p>
     * 참조가 약한지 여부.
     */
    private final boolean weak;

    /**
     * Registered references (listeners or weak references to them).
     * <p>
     * 등록된 참조(리스너 또는 그에 대한 약한 참조).
     */
    private final List<Object> refs = new ArrayList<Object>();

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param weak true to hold weak references / 약한 참조를 가지려면 true
     */
    StorageListeners(boolean weak) {
        this.weak = weak;
    }

    /**
     * Registers a listener once.
     * <p>
     * 리스너를 한 번만 등록한다.
     *
     * @param l listener / 리스너
     */
    synchronized void add(StorageListener l) {
        // 이미 등록된 리스너이면 다시 등록하지 않는다.
        for (Object r : refs) {
            if (resolve(r) == l) {
                return;
            }
        }
        refs.add(weak ? new WeakReference<StorageListener>(l) : l);
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 제거한다.
     *
     * @param l listener / 리스너
     */
    synchronized void remove(StorageListener l) {
        Iterator<Object> it = refs.iterator();
        // 일치하는 참조와 이미 죽은 참조를 함께 정리한다.
        while (it.hasNext()) {
            StorageListener x = resolve(it.next());
            if (x == null || x == l) {
                it.remove();
            }
        }
    }

    /**
     * Returns the number of live listeners.
     * <p>
     * 살아 있는 리스너의 수를 반환한다.
     *
     * @return count / 개수
     */
    synchronized int size() {
        return snapshot().size();
    }

    /**
     * Resolves a stored reference.
     * <p>
     * 보관된 참조를 해석한다.
     *
     * @param r stored reference / 보관된 참조
     * @return the listener or null if it was collected / 리스너. 수거되었으면 null
     */
    @SuppressWarnings("unchecked")
    private StorageListener resolve(Object r) {
        return weak ? ((WeakReference<StorageListener>) r).get() : (StorageListener) r;
    }

    /**
     * Returns the live listeners and drops dead references.
     * <p>
     * 살아 있는 리스너를 반환하고 죽은 참조를 버린다.
     *
     * @return listeners / 리스너
     */
    private List<StorageListener> snapshot() {
        List<StorageListener> live = new ArrayList<StorageListener>(refs.size());
        Iterator<Object> it = refs.iterator();
        // 살아 있는 리스너만 모으고 죽은 참조는 목록에서 뺀다.
        while (it.hasNext()) {
            StorageListener l = resolve(it.next());
            if (l == null) {
                it.remove();
            } else {
                live.add(l);
            }
        }
        return live;
    }

    /**
     * Notifies the listeners of an origin, except the source of the change. A failing listener does not stop the others.
     * <p>
     * 오리진의 리스너에게 알린다(변경 주체는 제외). 실패한 리스너가 다른 리스너를 막지 않는다.
     *
     * @param origin   serialized origin that changed / 바뀐 직렬화된 오리진
     * @param key      key or null for clear / 키. clear 이면 null
     * @param oldValue previous value / 이전 값
     * @param newValue new value / 새 값
     * @param source   source of the change or null / 변경 주체 또는 null
     */
    void fire(String origin, String key, String oldValue, String newValue, Object source) {
        List<StorageListener> targets;
        synchronized (this) {
            targets = snapshot();
        }
        // 같은 오리진의 리스너 중 변경한 주체가 아닌 것에게 알린다.
        for (StorageListener l : targets) {
            if (l != source && origin.equals(l.getOrigin())) {
                try {
                    l.storageChanged(key, oldValue, newValue, source);
                } catch (RuntimeException e) {
                    Thread t = Thread.currentThread();
                    t.getUncaughtExceptionHandler().uncaughtException(t, e);
                }
            }
        }
    }
}
