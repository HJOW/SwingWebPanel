package org.duckdns.hjow.swingwebpanel.storage;

/**
 * One storage area as web pages see it (the <code>Storage</code> interface): the items of one origin in localStorage or sessionStorage.
 * The JavaScript layer exposes exactly these operations (plus the named property access that maps to getItem/setItem/removeItem).
 * Keys and values are strings; the order of <code>key(i)</code> is the insertion order and stable while the storage is unchanged.
 * <p>
 * 웹 페이지가 보는 저장 영역 하나(<code>Storage</code> 인터페이스): localStorage 또는 sessionStorage 에서 한 오리진의 항목들. JavaScript 계층은 정확히
 * 이 연산들(과 getItem/setItem/removeItem 에 대응하는 이름 있는 프로퍼티 접근)을 노출한다. 키와 값은 문자열이며 <code>key(i)</code> 의 순서는
 * 삽입 순서이고 저장소가 바뀌지 않는 동안 안정적이다.
 */
public interface WebStorage {

    /**
     * Returns the number of items.
     * <p>
     * 항목의 수를 반환한다.
     *
     * @return length / 길이
     */
    int getLength();

    /**
     * Returns the name of the item at an index.
     * <p>
     * 위치의 항목 이름을 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return the key or null if out of range / 키. 범위 밖이면 null
     */
    String key(int index);

    /**
     * Returns the value of an item.
     * <p>
     * 항목의 값을 반환한다.
     *
     * @param key key / 키
     * @return the value or null if absent / 값. 없으면 null
     */
    String getItem(String key);

    /**
     * Stores an item (adds or replaces).
     * <p>
     * 항목을 저장한다(추가 또는 교체).
     *
     * @param key   key, not null / 키(null 불가)
     * @param value value, not null / 값(null 불가)
     * @throws StorageQuotaException if a limit would be exceeded; nothing changes then / 제한을 넘으면. 이때 아무것도 바뀌지 않는다
     */
    void setItem(String key, String value);

    /**
     * Removes an item; nothing happens if it does not exist.
     * <p>
     * 항목을 제거한다. 없으면 아무 일도 없다.
     *
     * @param key key / 키
     */
    void removeItem(String key);

    /**
     * Removes all items of this area.
     * <p>
     * 이 영역의 모든 항목을 제거한다.
     */
    void clear();
}
