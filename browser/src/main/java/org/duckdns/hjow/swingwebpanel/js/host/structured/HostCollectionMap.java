package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An ordered Java map for cloned script keys, using identity for keys that were script objects.
 * Its callers publish a read-only view once construction is complete.
 * <p>
 * 복제한 스크립트 키를 순서대로 담고, 스크립트 객체에서 온 키는 정체성으로 비교하는 Java 맵.
 * 생성이 끝나면 호출자가 읽기 전용 뷰를 공개한다.
 */
final class HostCollectionMap extends AbstractMap<Object, Object> {

    /** Ordered entries indexed with their JavaScript equality rule. / JavaScript의 동등성 규칙으로 찾는 순서 있는 항목. */
    private final Map<Key, Object> values = new LinkedHashMap<Key, Object>();

    /** Starts an empty map. / 빈 맵을 시작한다. */
    HostCollectionMap() {
    }

    /**
     * Adds one serialized map key or set member.
     * <p>
     * 직렬화된 맵의 키 또는 집합의 원소 하나를 추가한다.
     *
     * @param key the converted key / 변환한 키
     * @param value the value, or null for set backing / 값. 집합의 저장소면 null
     * @param identity whether the key was a script object / 키가 스크립트 객체였는지 여부
     */
    void append(Object key, Object value, boolean identity) {
        values.put(new Key(key, identity), value);
    }

    /**
     * Checks a primitive key by value or a converted object key by identity.
     * <p>
     * 원시 키는 값, 변환한 객체 키는 정체성으로 찾는다.
     *
     * @param key key to look up / 찾을 키
     * @return whether the key is present / 키가 있으면 true
     */
    public boolean containsKey(Object key) {
        return values.containsKey(new Key(key, true)) || values.containsKey(new Key(key, false));
    }

    /**
     * Reads a value without conflating two distinct object keys with equal contents.
     * <p>
     * 내용이 같은 별개 객체 키를 구분하여 값을 조회한다.
     *
     * @param key the key to look up / 찾을 키
     * @return the stored value or null / 저장한 값 또는 null
     */
    public Object get(Object key) {
        Key objectKey = new Key(key, true);
        return values.containsKey(objectKey) ? values.get(objectKey) : values.get(new Key(key, false));
    }

    /**
     * Returns the immutable entries in script order.
     * <p>
     * 스크립트 순서로 된 수정 불가 항목을 반환한다.
     *
     * @return an ordered set view / 순서 있는 집합 뷰
     */
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new AbstractSet<Map.Entry<Object, Object>>() {
            public int size() {
                return values.size();
            }

            public Iterator<Map.Entry<Object, Object>> iterator() {
                final Iterator<Map.Entry<Key, Object>> source = values.entrySet().iterator();
                return new Iterator<Map.Entry<Object, Object>>() {
                    public boolean hasNext() {
                        return source.hasNext();
                    }

                    public Map.Entry<Object, Object> next() {
                        Map.Entry<Key, Object> entry = source.next();
                        return new SimpleImmutableEntry<Object, Object>(entry.getKey().value, entry.getValue());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Associates a Java key with its script classification.
     * <p>
     * Java 키에 스크립트에서의 분류를 연결한다.
     */
    private static final class Key {

        /** Exposed Java key. / 외부에 보이는 Java 키. */
        final Object value;

        /** True for object identities. / 객체 정체성이면 true. */
        private final boolean identity;

        /**
         * Creates a key with one equality rule.
         * <p>
         * 동등성 규칙 하나로 키를 만든다.
         *
         * @param value key value / 키의 값
         * @param identity whether to compare by reference / 참조로 비교하는지 여부
         */
        Key(Object value, boolean identity) {
            this.value = value;
            this.identity = identity;
        }

        /** Hashes the value or its identity. / 값이나 정체성의 해시를 구한다. */
        public int hashCode() {
            return identity ? System.identityHashCode(value) : value == null ? 0 : value.hashCode();
        }

        /**
         * Compares only keys in the same class of script value.
         * <p>
         * 스크립트 값의 분류가 같은 키끼리 비교한다.
         *
         * @param object other key / 다른 키
         * @return whether the keys match / 키가 맞으면 true
         */
        public boolean equals(Object object) {
            if (!(object instanceof Key)) {
                return false;
            }
            Key other = (Key) object;
            return identity == other.identity && (value == other.value || !identity && value != null && value.equals(other.value));
        }
    }
}
