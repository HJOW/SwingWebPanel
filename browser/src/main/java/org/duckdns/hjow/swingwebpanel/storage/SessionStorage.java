package org.duckdns.hjow.swingwebpanel.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The sessionStorage of one browser component: one {@link StorageArea} per origin, kept in memory as an ordinary field of the
 * component's session. Nothing static refers to it, so it disappears together with the component (plan items P16-02, P21-06). A change of
 * one document is reported to the other documents of the same component and origin (frames); it is never visible to other components.
 * <p>
 * 브라우저 컴포넌트 하나의 sessionStorage: 오리진마다 {@link StorageArea} 하나이며 컴포넌트 세션의 일반 필드로 메모리에 둔다. 정적 변수가 이것을
 * 가리키지 않으므로 컴포넌트와 함께 사라진다(계획 P16-02, P21-06). 한 문서의 변경은 같은 컴포넌트·같은 오리진의 다른 문서(프레임)에 보고되며
 * 다른 컴포넌트에는 보이지 않는다.
 */
public final class SessionStorage {

    /**
     * Areas by serialized origin.
     * <p>
     * 직렬화된 오리진별 영역.
     */
    private final Map<String, StorageArea> areas = new HashMap<String, StorageArea>();

    /**
     * Listeners of the documents of this component.
     * <p>
     * 이 컴포넌트의 문서들의 리스너.
     */
    private final StorageListeners listeners = new StorageListeners(false);

    /**
     * The objects that other parts of the product keep for this component (see {@link #getSlot(String, SlotFactory)}); never cleared by
     * {@link #clear()}.
     * <p>
     * 제품의 다른 부분이 이 컴포넌트를 위해 보관하는 객체({@link #getSlot(String, SlotFactory)} 참고). {@link #clear()} 로 지워지지 않는다.
     */
    private final Map<String, Object> slots = new HashMap<String, Object>();

    /**
     * Creates the object that a slot of the component holds.
     * <p>
     * 컴포넌트의 슬롯이 가지는 객체를 만든다.
     */
    public interface SlotFactory {
        /**
         * Creates the object, called once per component and slot name.
         * <p>
         * 객체를 만든다. 컴포넌트와 슬롯 이름마다 한 번 불린다.
         *
         * @return the new object / 새 객체
         */
        Object create();
    }

    /**
     * Creates an empty sessionStorage.
     * <p>
     * 빈 sessionStorage 를 만든다.
     */
    public SessionStorage() {
    }

    /**
     * Returns the object that another part of the product keeps for this component under a name, creating it with the factory the first
     * time. This is where state that belongs to the whole component, not to one page (for example the shared workers and the broadcast
     * channels of the pages of the component), lives: it is an ordinary field of this object, so it disappears with the component and no
     * static variable has to hold it. It is neither part of the storage areas nor cleared with them.
     * <p>
     * 제품의 다른 부분이 이 컴포넌트를 위해 이름으로 보관하는 객체를 반환하며 처음에는 팩토리로 만든다. 페이지 하나가 아니라 컴포넌트 전체에 속하는 상태(예:
     * 컴포넌트의 페이지들이 공유하는 공유 worker 와 브로드캐스트 채널)가 사는 곳이다. 이 객체의 일반 필드이므로 컴포넌트와 함께 사라지고 정적 변수가 붙잡을
     * 필요가 없다. 저장 영역의 일부가 아니며 영역과 함께 지워지지도 않는다.
     *
     * @param name    the slot name / 슬롯 이름
     * @param factory creates the object the first time / 처음에 객체를 만든다
     * @return the object of the slot / 슬롯의 객체
     */
    public synchronized Object getSlot(String name, SlotFactory factory) {
        Object o = slots.get(name);
        if (o == null) {
            o = factory.create();
            slots.put(name, o);
        }
        return o;
    }

    /**
     * Returns the storage view of an origin.
     * <p>
     * 오리진의 저장소 뷰를 반환한다.
     *
     * @param origin origin of the document / 문서의 오리진
     * @param source the document that will use the view (excluded from change notifications), may be null / 뷰를 쓸 문서(변경 통지에서 제외). null 가능
     * @return the view / 뷰
     * @throws DomException SecurityError for opaque origins (about:blank, about:text) / 불투명 오리진(about:blank, about:text)이면 SecurityError
     */
    public WebStorage forOrigin(Origin origin, Object source) {
        if (origin == null || origin.isOpaque()) {
            throw DomException.security("this document has no storage (opaque origin)");
        }
        return new View(origin.serialize(), source);
    }

    /**
     * Returns a read-only copy of all items of all origins, sorted by origin and key.
     * <p>
     * 모든 오리진의 모든 항목의 읽기 전용 복사본을 오리진, 키 순으로 정렬해 반환한다.
     *
     * @return entries / 항목
     */
    public List<StorageEntry> entries() {
        List<StorageEntry> list = new ArrayList<StorageEntry>();
        synchronized (this) {
            // 오리진마다 항목을 모은다.
            for (Map.Entry<String, StorageArea> e : areas.entrySet()) {
                list.addAll(e.getValue().entries(e.getKey()));
            }
        }
        Collections.sort(list, new Comparator<StorageEntry>() {
            /**
             * Orders by origin, then by key.
             * <p>
             * 오리진, 키 순으로 정렬한다.
             *
             * @param a first / 첫 번째
             * @param b second / 두 번째
             * @return comparison / 비교 결과
             */
            public int compare(StorageEntry a, StorageEntry b) {
                int c = a.getOrigin().compareTo(b.getOrigin());
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        return list;
    }

    /**
     * Removes every item of every origin.
     * <p>
     * 모든 오리진의 모든 항목을 제거한다.
     */
    public synchronized void clear() {
        areas.clear();
    }

    /**
     * Copies every entry of another session storage into this one (the session storage of a window that a page opens starts as a copy
     * of the storage of its opener, WHATWG HTML 12.2.2).
     * <p>
     * 다른 sessionStorage 의 모든 항목을 여기로 복사한다(페이지가 연 창의 sessionStorage 는 여는 창의 저장소의 복사본으로 시작한다, WHATWG HTML
     * 12.2.2).
     *
     * @param other the storage to copy from / 복사해 올 저장소
     */
    public void copyFrom(SessionStorage other) {
        List<StorageEntry> entries = other.entries();
        synchronized (this) {
            // 항목마다 같은 오리진의 영역에 넣는다.
            for (int i = 0; i < entries.size(); i++) {
                StorageEntry e = entries.get(i);
                area(e.getOrigin()).setItem(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Registers a listener of a document of this component.
     * <p>
     * 이 컴포넌트의 문서의 리스너를 등록한다.
     *
     * @param l listener / 리스너
     */
    public void addListener(StorageListener l) {
        listeners.add(l);
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 제거한다.
     *
     * @param l listener / 리스너
     */
    public void removeListener(StorageListener l) {
        listeners.remove(l);
    }

    /**
     * Returns the area of an origin, creating it if needed.
     * <p>
     * 오리진의 영역을 반환하며 필요하면 만든다.
     *
     * @param origin serialized origin / 직렬화된 오리진
     * @return the area / 영역
     */
    private synchronized StorageArea area(String origin) {
        StorageArea a = areas.get(origin);
        if (a == null) {
            a = new StorageArea();
            areas.put(origin, a);
        }
        return a;
    }

    /**
     * The view of one origin that reports changes to the other documents.
     * <p>
     * 변경을 다른 문서에 보고하는 한 오리진의 뷰.
     */
    private final class View implements WebStorage {
        /**
         * Serialized origin.
         * <p>
         * 직렬화된 오리진.
         */
        private final String origin;

        /**
         * Document that uses the view.
         * <p>
         * 뷰를 쓰는 문서.
         */
        private final Object source;

        /**
         * Creates the view.
         * <p>
         * 뷰를 만든다.
         *
         * @param origin serialized origin / 직렬화된 오리진
         * @param source user of the view / 뷰의 사용자
         */
        View(String origin, Object source) {
            this.origin = origin;
            this.source = source;
        }

        /**
         * Returns the number of items.
         * <p>
         * 항목의 수를 반환한다.
         *
         * @return length / 길이
         */
        public int getLength() {
            return area(origin).getLength();
        }

        /**
         * Returns the key at an index.
         * <p>
         * 위치의 키를 반환한다.
         *
         * @param index index / 위치
         * @return key or null / 키 또는 null
         */
        public String key(int index) {
            return area(origin).key(index);
        }

        /**
         * Returns the value of an item.
         * <p>
         * 항목의 값을 반환한다.
         *
         * @param key key / 키
         * @return value or null / 값 또는 null
         */
        public String getItem(String key) {
            return area(origin).getItem(key);
        }

        /**
         * Stores an item and reports a real change.
         * <p>
         * 항목을 저장하고 실제 변경을 보고한다.
         *
         * @param key   key / 키
         * @param value value / 값
         */
        public void setItem(String key, String value) {
            String old = area(origin).put(key, value);
            if (old == null || !old.equals(value)) {
                listeners.fire(origin, key, old, value, source);
            }
        }

        /**
         * Removes an item and reports a real change.
         * <p>
         * 항목을 제거하고 실제 변경을 보고한다.
         *
         * @param key key / 키
         */
        public void removeItem(String key) {
            String old = area(origin).remove(key);
            if (old != null) {
                listeners.fire(origin, key, old, null, source);
            }
        }

        /**
         * Removes all items of the origin and reports it if there were any.
         * <p>
         * 오리진의 모든 항목을 제거하고 항목이 있었으면 보고한다.
         */
        public void clear() {
            StorageArea a = area(origin);
            boolean had = a.getLength() > 0;
            a.clear();
            if (had) {
                listeners.fire(origin, null, null, null, source);
            }
        }
    }
}
