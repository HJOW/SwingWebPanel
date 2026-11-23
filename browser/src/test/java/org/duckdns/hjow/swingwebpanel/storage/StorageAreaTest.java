package org.duckdns.hjow.swingwebpanel.storage;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link StorageArea} and {@link StorageEntry} (plan items P16-01, P16-07; contract case C-STORAGE).
 * <p>
 * {@link StorageArea} 와 {@link StorageEntry} 시험(계획 P16-01, P16-07, 계약 사례 C-STORAGE).
 */
public class StorageAreaTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StorageAreaTest() {
    }

    /**
     * P16-01: items keep their insertion order, key(i) is stable, replacing keeps the position, missing items give null.
     * <p>
     * P16-01: 항목은 삽입 순서를 유지하고 key(i)는 안정적이며 교체해도 위치가 유지되고 없는 항목은 null 이다.
     */
    public void testOrderAndAccess() {
        StorageArea a = new StorageArea();
        Check.equal(0, a.getLength(), "empty");
        Check.isNull(a.getItem("x"), "missing item");
        Check.isNull(a.key(0), "key of an empty storage");
        Check.isNull(a.put("b", "1"), "new key");
        a.setItem("a", "2");
        a.setItem("c", "3");
        Check.equal("1", a.put("b", "9"), "put returns the previous value");
        Check.equal(3, a.getLength(), "length");
        Check.equal("b", a.key(0), "first key");
        Check.equal("a", a.key(1), "second key");
        Check.equal("c", a.key(2), "third key");
        Check.isNull(a.key(3), "key out of range");
        Check.isNull(a.key(-1), "negative index");
        Check.equal("9", a.getItem("b"), "value after replace");
        Check.equal("9", a.remove("b"), "remove returns the old value");
        Check.isNull(a.remove("b"), "removing again");
        a.removeItem("zzz");
        Check.equal("a", a.key(0), "order after removal");
        a.clear();
        Check.equal(0, a.getLength(), "after clear");
        Check.equal(0, a.getSize(), "size after clear");
    }

    /**
     * P16-01: keys and values are strings; null is rejected; the empty string is a valid key and value.
     * <p>
     * P16-01: 키와 값은 문자열이며 null 은 거부되고 빈 문자열은 올바른 키와 값이다.
     */
    public void testNullAndEmptyStrings() {
        final StorageArea a = new StorageArea();
        a.setItem("", "");
        Check.equal("", a.getItem(""), "empty key and value");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Stores a null value.
             * <p>
             * null 값을 저장한다.
             */
            public void run() {
                a.setItem("k", null);
            }
        }, "null value");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Stores a null key.
             * <p>
             * null 키를 저장한다.
             */
            public void run() {
                a.setItem(null, "v");
            }
        }, "null key");
        Check.equal(1, a.getLength(), "failed calls store nothing");
    }

    /**
     * P16-07: the quota counts keys plus values, is exact, and leaves the storage unchanged when exceeded.
     * <p>
     * P16-07: 쿼터는 키와 값의 합을 세고 정확하며 초과하면 저장소를 바꾸지 않는다.
     */
    public void testQuota() {
        final StorageArea a = new StorageArea(10);
        a.setItem("ab", "123456");
        Check.equal(8, a.getSize(), "size of key plus value");
        a.setItem("c", "d");
        Check.equal(10, a.getSize(), "exactly at the limit is allowed");
        Check.throwsException(StorageQuotaException.class, new Block() {
            /**
             * Adds one more character.
             * <p>
             * 한 글자를 더 넣는다.
             */
            public void run() {
                a.setItem("e", "");
            }
        }, "a new key over the limit");
        Check.throwsException(StorageQuotaException.class, new Block() {
            /**
             * Grows an existing value over the limit.
             * <p>
             * 기존 값을 키워 제한을 넘긴다.
             */
            public void run() {
                a.setItem("c", "dd");
            }
        }, "a longer value over the limit");
        Check.equal("d", a.getItem("c"), "the old value survives a failed replace");
        Check.equal(2, a.getLength(), "nothing was added");
        a.setItem("ab", "1");
        Check.equal(5, a.getSize(), "a shorter value frees space");
        a.setItem("zz", "123");
        Check.equal(10, a.getSize(), "space that was freed can be reused up to the limit");
        a.remove("zz");
        a.remove("c");
        a.remove("ab");
        Check.equal(0, a.getSize(), "size returns to zero");
        StorageArea big = new StorageArea();
        Check.equal(StorageLimits.MAX_ORIGIN_CHARS, 2621440, "the standard limit is 5 MiB of characters");
        big.setItem("k", repeat('x', StorageLimits.MAX_ORIGIN_CHARS - 1));
        Check.equal(StorageLimits.MAX_ORIGIN_CHARS, big.getSize(), "the standard limit is reachable exactly");
    }

    /**
     * Builds a string of a character repeated n times.
     * <p>
     * 문자 하나를 n 번 반복한 문자열을 만든다.
     *
     * @param c character / 문자
     * @param n count / 횟수
     * @return string / 문자열
     */
    private static String repeat(char c, int n) {
        char[] buf = new char[n];
        java.util.Arrays.fill(buf, c);
        return new String(buf);
    }

    /**
     * P16-01: entries are immutable copies that do not follow later changes.
     * <p>
     * P16-01: 항목은 이후의 변경을 따르지 않는 불변 복사본이다.
     */
    public void testEntriesAreCopies() {
        StorageArea a = new StorageArea();
        a.setItem("k1", "v1");
        a.setItem("k2", "v2");
        List<StorageEntry> list = a.entries("http://x.test");
        a.setItem("k1", "changed");
        a.clear();
        Check.equal(2, list.size(), "the snapshot keeps its size");
        Check.equal("v1", list.get(0).getValue(), "and its values");
        Check.equal("http://x.test", list.get(0).getOrigin(), "origin");
        Check.equal("k2", list.get(1).getKey(), "key");
        Check.equal(new StorageEntry("o", "k", "v"), new StorageEntry("o", "k", "v"), "entries have value equality");
        Check.equal(new StorageEntry("o", "k", "v").hashCode(), new StorageEntry("o", "k", "v").hashCode(), "and a matching hash code");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Creates an entry with a null part.
             * <p>
             * 일부가 null 인 항목을 만든다.
             */
            public void run() {
                new StorageEntry("o", null, "v");
            }
        }, "null part");
    }
}
