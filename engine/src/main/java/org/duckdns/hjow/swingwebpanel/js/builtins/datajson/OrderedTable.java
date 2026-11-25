package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * The insertion-ordered key/value storage behind Map and Set. Keys are compared with SameValueZero (NaN equals NaN, +0 equals -0 and the key
 * is stored as +0). Iteration works through {@link Cursor} objects that behave exactly like the index walk over the specification's entry
 * list: entries added during an iteration are visited, entries removed before they are reached are skipped, and clearing the table
 * lets a running cursor continue with entries that are added afterwards. Removed entries stay in the list as holes until compaction, which
 * renumbers all live cursors (kept in a weak list, so abandoned iterators cost nothing).
 * <p>
 * Map 과 Set 뒤의 삽입 순서 키/값 저장소. 키는 SameValueZero 로 비교한다(NaN 은 NaN 과 같고 +0 은 -0 과 같으며 키는 +0 으로 저장된다).
 * 순회는 규격의 항목 목록에 대한 인덱스 순회와 똑같이 동작하는 {@link Cursor} 로 한다: 순회 중 추가된 항목은 방문하고, 도달하기 전에 삭제된 항목은
 * 건너뛰며, 테이블을 비워도 진행 중인 커서는 이후에 추가되는 항목으로 계속된다. 삭제된 항목은 압축 때까지 목록에 빈칸으로 남고, 압축은 살아 있는 모든
 * 커서의 위치를 다시 매긴다(커서는 약한 목록에 있어 버려진 이터레이터는 비용이 없다).
 */
final class OrderedTable {

    /**
     * Holes allowed before a compaction is considered.
     * <p>
     * 압축을 고려하기 전에 허용하는 빈칸 수.
     */
    private static final int COMPACT_MIN_HOLES = 32;

    /**
     * One entry of the table.
     * <p>
     * 테이블의 항목 하나.
     */
    static final class Entry {
        /**
         * The key (never -0).
         * <p>
         * 키(-0 이 아님).
         */
        final JsValue key;

        /**
         * The value (the key itself for sets).
         * <p>
         * 값(집합이면 키 자체).
         */
        JsValue value;

        /**
         * True once the entry was removed.
         * <p>
         * 항목이 삭제되면 true.
         */
        boolean removed;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param key   the key / 키
         * @param value the value / 값
         */
        Entry(JsValue key, JsValue value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * A position in the entry list, advanced by {@link #next()}; compaction keeps it pointing at the same logical place.
     * <p>
     * 항목 목록 안의 위치. {@link #next()} 로 전진하며 압축이 일어나도 같은 논리적 위치를 가리킨다.
     */
    final class Cursor {
        /**
         * Index of the next entry to inspect.
         * <p>
         * 다음에 살펴볼 항목의 위치.
         */
        private int pos;

        /**
         * Returns the next live entry and advances, or null at the end.
         * <p>
         * 다음의 살아 있는 항목을 돌려주고 전진한다. 끝이면 null.
         *
         * @return the entry or null / 항목 또는 null
         */
        Entry next() {
            // 삭제된 칸은 건너뛰며 살아 있는 항목을 찾는다.
            while (pos < list.size()) {
                Entry e = list.get(pos++);
                if (!e.removed) {
                    return e;
                }
            }
            return null;
        }
    }

    /**
     * Finds entries by normalized key.
     * <p>
     * 정규화한 키로 항목을 찾는다.
     */
    private final HashMap<Object, Entry> index = new HashMap<Object, Entry>();

    /**
     * Entries in insertion order including holes.
     * <p>
     * 빈칸을 포함한 삽입 순서의 항목들.
     */
    private ArrayList<Entry> list = new ArrayList<Entry>();

    /**
     * Number of holes in the list.
     * <p>
     * 목록의 빈칸 수.
     */
    private int holes;

    /**
     * Live cursors (weak).
     * <p>
     * 살아 있는 커서(약한 참조).
     */
    private final ArrayList<WeakReference<Cursor>> cursors = new ArrayList<WeakReference<Cursor>>();

    /**
     * Normalizes a key for the hash lookup: numbers by value with +0 and one NaN, strings by text, everything else by identity.
     * <p>
     * 해시 조회용으로 키를 정규화한다: 숫자는 값(+0 과 NaN 하나), 문자열은 내용, 나머지는 동일성.
     *
     * @param k the key / 키
     * @return the lookup key / 조회 키
     */
    private static Object normalize(JsValue k) {
        if (k instanceof JsNumber) {
            double d = ((JsNumber) k).getValue();
            return Double.valueOf(d == 0 ? 0.0 : d);
        }
        if (k instanceof JsString) {
            return ((JsString) k).getValue();
        }
        return k;
    }

    /**
     * Returns the number of live entries.
     * <p>
     * 살아 있는 항목 수를 반환한다.
     *
     * @return the size / 크기
     */
    int size() {
        return index.size();
    }

    /**
     * Finds the entry of a key.
     * <p>
     * 키의 항목을 찾는다.
     *
     * @param key the key / 키
     * @return the entry or null / 항목 또는 null
     */
    Entry find(JsValue key) {
        return index.get(normalize(key));
    }

    /**
     * Adds an entry or replaces the value of an existing one (keeping its position).
     * <p>
     * 항목을 추가하거나 기존 항목의 값을 바꾼다(위치는 유지).
     *
     * @param key   the key / 키
     * @param value the value / 값
     */
    void put(JsValue key, JsValue value) {
        Object n = normalize(key);
        Entry e = index.get(n);
        if (e != null) {
            e.value = value;
            return;
        }
        e = new Entry(canonicalKey(key), value);
        index.put(n, e);
        list.add(e);
    }

    /**
     * Converts negative zero to the canonical collection key.
     * <p>
     * 음의 0을 정규 컬렉션 키로 바꾼다.
     *
     * @param key collection key / 컬렉션 키
     * @return key with zero canonicalized / 0을 정규화한 키
     */
    static JsValue canonicalKey(JsValue key) {
        return key instanceof JsNumber && ((JsNumber) key).getValue() == 0 ? Js.num(0) : key;
    }

    /**
     * Removes the entry of a key.
     * <p>
     * 키의 항목을 삭제한다.
     *
     * @param key the key / 키
     * @return true if an entry was removed / 항목이 삭제되었으면 true
     */
    boolean remove(JsValue key) {
        Entry e = index.remove(normalize(key));
        if (e == null) {
            return false;
        }
        e.removed = true;
        holes++;
        if (holes > COMPACT_MIN_HOLES && holes * 2 > list.size()) {
            compact();
        }
        return true;
    }

    /**
     * Removes all entries; running cursors continue with entries added later.
     * <p>
     * 모든 항목을 삭제한다. 진행 중인 커서는 이후 추가되는 항목으로 계속된다.
     */
    void clear() {
        // 목록의 모든 항목에 삭제 표시를 한 뒤 압축하면 모든 커서가 처음 위치로 돌아간다.
        for (int i = 0; i < list.size(); i++) {
            list.get(i).removed = true;
        }
        index.clear();
        holes = list.size();
        compact();
    }

    /**
     * Creates a cursor at the start of the table.
     * <p>
     * 테이블 처음에 커서를 만든다.
     *
     * @return the cursor / 커서
     */
    Cursor cursor() {
        Cursor c = new Cursor();
        cursors.add(new WeakReference<Cursor>(c));
        if (cursors.size() > 64 && (cursors.size() & 63) == 0) {
            pruneCursors();
        }
        return c;
    }

    /**
     * Forgets cursors that were garbage collected.
     * <p>
     * 가비지 수거된 커서를 잊는다.
     */
    private void pruneCursors() {
        Iterator<WeakReference<Cursor>> it = cursors.iterator();
        // 이미 사라진 약한 참조를 목록에서 지운다.
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove();
            }
        }
    }

    /**
     * Drops the holes from the list and renumbers the live cursors so that each still points before the same live entry.
     * <p>
     * 목록에서 빈칸을 없애고 살아 있는 커서의 위치를 다시 매겨 각 커서가 같은 살아 있는 항목 앞을 계속 가리키게 한다.
     */
    private void compact() {
        ArrayList<Entry> fresh = new ArrayList<Entry>(Math.max(index.size(), 8));
        // 위치별로 "그 앞에 살아 있는 항목이 몇 개인가" 를 세어 커서 위치 변환표를 만든다.
        int[] liveBefore = new int[list.size() + 1];
        for (int i = 0; i < list.size(); i++) {
            liveBefore[i] = fresh.size();
            Entry e = list.get(i);
            if (!e.removed) {
                fresh.add(e);
            }
        }
        liveBefore[list.size()] = fresh.size();
        List<WeakReference<Cursor>> alive = new ArrayList<WeakReference<Cursor>>();
        // 살아 있는 커서의 위치를 새 번호로 바꾸고 사라진 커서는 버린다.
        for (int i = 0; i < cursors.size(); i++) {
            Cursor c = cursors.get(i).get();
            if (c != null) {
                c.pos = liveBefore[Math.min(c.pos, list.size())];
                alive.add(cursors.get(i));
            }
        }
        cursors.clear();
        cursors.addAll(alive);
        list = fresh;
        holes = 0;
    }
}
