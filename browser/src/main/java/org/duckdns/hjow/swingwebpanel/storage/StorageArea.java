package org.duckdns.hjow.swingwebpanel.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory storage area of one origin: an insertion-ordered map of string keys to string values with a size limit
 * (see {@link StorageLimits}). It is the data structure behind sessionStorage and behind each origin of localStorage. All methods are
 * synchronized. A change that would exceed the limit throws {@link StorageQuotaException} before anything is modified.
 * <p>
 * 한 오리진의 메모리 저장 영역: 문자열 키를 문자열 값에 대응시키는 삽입 순서 맵이며 크기 제한({@link StorageLimits} 참조)이 있다. sessionStorage 와
 * localStorage 의 각 오리진 뒤에 있는 자료 구조이다. 모든 메서드는 동기화되어 있다. 제한을 넘게 되는 변경은 아무것도 바꾸기 전에
 * {@link StorageQuotaException} 을 던진다.
 */
public final class StorageArea implements WebStorage {

    /**
     * The items in insertion order.
     * <p>
     * 삽입 순서의 항목.
     */
    private final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

    /**
     * Current size: code units of all keys and values.
     * <p>
     * 현재 크기: 모든 키와 값의 코드 단위 수.
     */
    private int chars;

    /**
     * Size limit in code units.
     * <p>
     * 코드 단위 수로 된 크기 제한.
     */
    private final int maxChars;

    /**
     * Creates an area with the standard per-origin limit.
     * <p>
     * 표준 오리진별 제한을 가진 영역을 만든다.
     */
    public StorageArea() {
        this(StorageLimits.MAX_ORIGIN_CHARS);
    }

    /**
     * Creates an area with a custom size limit (used by tests and by the process-wide manager).
     * <p>
     * 사용자 지정 크기 제한을 가진 영역을 만든다(시험과 프로세스 공용 관리자가 쓴다).
     *
     * @param maxChars limit in code units / 코드 단위 수 제한
     */
    public StorageArea(int maxChars) {
        this.maxChars = maxChars;
    }

    /**
     * Returns the number of items.
     * <p>
     * 항목의 수를 반환한다.
     *
     * @return length / 길이
     */
    public synchronized int getLength() {
        return map.size();
    }

    /**
     * Returns the key at an index (insertion order).
     * <p>
     * 위치의 키를 반환한다(삽입 순서).
     *
     * @param index zero based index / 0 기반 위치
     * @return key or null / 키 또는 null
     */
    public synchronized String key(int index) {
        if (index < 0 || index >= map.size()) {
            return null;
        }
        int i = 0;
        // 삽입 순서로 index 번째 키까지 센다.
        for (String k : map.keySet()) {
            if (i++ == index) {
                return k;
            }
        }
        return null;
    }

    /**
     * Returns the value of an item.
     * <p>
     * 항목의 값을 반환한다.
     *
     * @param key key / 키
     * @return value or null / 값 또는 null
     */
    public synchronized String getItem(String key) {
        return map.get(key);
    }

    /**
     * Stores an item.
     * <p>
     * 항목을 저장한다.
     *
     * @param key   key / 키
     * @param value value / 값
     * @throws StorageQuotaException if a limit would be exceeded / 제한을 넘게 되면
     */
    public void setItem(String key, String value) {
        put(key, value);
    }

    /**
     * Stores an item and returns the previous value (null if the key was new).
     * <p>
     * 항목을 저장하고 이전 값을 반환한다(새 키이면 null).
     *
     * @param key   key, not null / 키(null 불가)
     * @param value value, not null / 값(null 불가)
     * @return previous value or null / 이전 값 또는 null
     * @throws StorageQuotaException if a limit would be exceeded; nothing changes then / 제한을 넘게 되면. 이때 아무것도 바뀌지 않는다
     */
    public synchronized String put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key and value must not be null");
        }
        String old = map.get(key);
        long newSize = (long) chars + value.length() + (old == null ? key.length() : -old.length());
        if (newSize > maxChars) {
            throw new StorageQuotaException("the storage quota of " + maxChars + " characters would be exceeded");
        }
        if (old == null && map.size() >= StorageLimits.MAX_ITEMS_PER_ORIGIN) {
            throw new StorageQuotaException("the storage cannot hold more than " + StorageLimits.MAX_ITEMS_PER_ORIGIN + " items");
        }
        chars = (int) newSize;
        map.put(key, value);
        return old;
    }

    /**
     * Removes an item.
     * <p>
     * 항목을 제거한다.
     *
     * @param key key / 키
     */
    public void removeItem(String key) {
        remove(key);
    }

    /**
     * Removes an item and returns its previous value.
     * <p>
     * 항목을 제거하고 이전 값을 반환한다.
     *
     * @param key key / 키
     * @return previous value or null if it did not exist / 이전 값. 없었으면 null
     */
    public synchronized String remove(String key) {
        String old = map.remove(key);
        if (old != null) {
            chars -= key.length() + old.length();
        }
        return old;
    }

    /**
     * Removes all items.
     * <p>
     * 모든 항목을 제거한다.
     */
    public synchronized void clear() {
        map.clear();
        chars = 0;
    }

    /**
     * Returns the size in code units.
     * <p>
     * 코드 단위 수로 된 크기를 반환한다.
     *
     * @return size / 크기
     */
    public synchronized int getSize() {
        return chars;
    }

    /**
     * Returns a read-only snapshot of the items as entries of an origin, in insertion order.
     * <p>
     * 항목의 읽기 전용 스냅샷을 오리진의 항목으로 삽입 순서대로 반환한다.
     *
     * @param origin serialized origin / 직렬화된 오리진
     * @return entries / 항목
     */
    public synchronized List<StorageEntry> entries(String origin) {
        List<StorageEntry> list = new ArrayList<StorageEntry>(map.size());
        // 항목을 삽입 순서로 불변 객체에 복사한다.
        for (Map.Entry<String, String> e : map.entrySet()) {
            list.add(new StorageEntry(origin, e.getKey(), e.getValue()));
        }
        return list;
    }
}
