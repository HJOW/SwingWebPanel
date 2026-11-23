package org.duckdns.hjow.swingwebpanel.storage;

/**
 * Receives the changes of a storage so that other documents of the same origin can fire <code>storage</code> events (plan item
 * P16-08). The change made by a document is not reported to that document itself (the <code>source</code> argument identifies it).
 * The method is called on the thread that changed the storage: an implementation must hand over to its document thread.
 * <p>
 * 같은 오리진의 다른 문서가 <code>storage</code> 이벤트를 일으킬 수 있도록 저장소의 변경을 받는다(계획 P16-08). 문서가 만든 변경은 그 문서
 * 자신에게 보고되지 않는다(<code>source</code> 인수가 그 문서를 가리킨다). 저장소를 바꾼 스레드에서 호출되므로 구현은 자신의 문서 스레드로 넘겨야 한다.
 */
public interface StorageListener {

    /**
     * Returns the origin whose changes this listener wants (serialized origin such as <code>http://example.com</code>).
     * <p>
     * 이 리스너가 받으려는 변경의 오리진을 반환한다(<code>http://example.com</code> 같은 직렬화된 오리진).
     *
     * @return origin / 오리진
     */
    String getOrigin();

    /**
     * Called after an item was added, changed or removed, or after the area was cleared (key null).
     * <p>
     * 항목이 추가, 변경, 제거된 뒤, 또는 영역이 비워진 뒤(key 가 null) 호출된다.
     *
     * @param key      changed key, null for clear / 바뀐 키. clear 이면 null
     * @param oldValue previous value or null / 이전 값 또는 null
     * @param newValue new value or null / 새 값 또는 null
     * @param source   the object that made the change (documents compare it to themselves), may be null / 변경을 만든 객체(문서가 자기 자신과 비교). null 가능
     */
    void storageChanged(String key, String oldValue, String newValue, Object source);
}
