package org.duckdns.hjow.swingwebpanel.storage;

/**
 * One item of a web storage area (localStorage or sessionStorage) as seen by the host application: the origin it belongs
 * to, its key and its value. Instances are immutable, so the collections returned by
 * <code>getLocalStorage()</code> and <code>getSessionStorage()</code> are read-only copies that never change the real storage.
 * <p>
 * 호스트 애플리케이션이 보는 웹 저장소(localStorage 또는 sessionStorage) 항목 하나: 속한 오리진, 키, 값. 불변 객체이므로
 * <code>getLocalStorage()</code> 와 <code>getSessionStorage()</code> 가 반환하는 컬렉션은 실제 저장소를 바꾸지 않는 읽기 전용 복사본이다.
 */
public final class StorageEntry {

    /**
     * Serialized origin such as <code>http://example.com</code>.
     * <p>
     * <code>http://example.com</code> 같은 직렬화된 오리진.
     */
    private final String origin;

    /**
     * Item key.
     * <p>
     * 항목 키.
     */
    private final String key;

    /**
     * Item value.
     * <p>
     * 항목 값.
     */
    private final String value;

    /**
     * Creates an entry.
     * <p>
     * 항목을 만든다.
     *
     * @param origin serialized origin, not null / 직렬화된 오리진(null 불가)
     * @param key    key, not null / 키(null 불가)
     * @param value  value, not null / 값(null 불가)
     * @throws IllegalArgumentException if an argument is null / 인수가 null 일 때
     */
    public StorageEntry(String origin, String key, String value) {
        if (origin == null || key == null || value == null) {
            throw new IllegalArgumentException("origin, key and value must not be null");
        }
        this.origin = origin;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the origin.
     * <p>
     * 오리진을 반환한다.
     *
     * @return origin / 오리진
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Returns the key.
     * <p>
     * 키를 반환한다.
     *
     * @return key / 키
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value.
     * <p>
     * 값을 반환한다.
     *
     * @return value / 값
     */
    public String getValue() {
        return value;
    }

    /**
     * Tests equality of origin, key and value.
     * <p>
     * 오리진, 키, 값이 모두 같은지 확인한다.
     *
     * @param o other object / 비교 대상
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StorageEntry)) {
            return false;
        }
        StorageEntry e = (StorageEntry) o;
        return origin.equals(e.origin) && key.equals(e.key) && value.equals(e.value);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     * <p>
     * {@link #equals(Object)} 와 일치하는 해시 코드를 반환한다.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return (origin.hashCode() * 31 + key.hashCode()) * 31 + value.hashCode();
    }

    /**
     * Returns a readable text for logs.
     * <p>
     * 로그용 읽기 쉬운 문자열을 반환한다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return origin + " " + key + "=" + value;
    }
}
