package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The navigation history of one component: a list of confirmed page snapshots and the current position.
 * Rules (API contract 5.6): a new navigation after going back deletes the forward entries; consecutive identical
 * URLs are separate entries; back/forward/refresh never add entries. All methods are synchronized, so the history can be
 * read from any thread.
 * <p>
 * 컴포넌트 하나의 이동 이력: 확정된 페이지 스냅샷 목록과 현재 위치. 규칙(API 계약 5.6): 뒤로 간 뒤 새 이동을 하면 앞쪽 항목을 삭제하고,
 * 연속된 같은 URL 도 별도 항목이며, 뒤로/앞으로/새로고침은 항목을 추가하지 않는다. 모든 메서드가 동기화되어 어느 스레드에서도 읽을 수 있다.
 */
final class History {

    /**
     * Entries, oldest first.
     * <p>
     * 항목 목록. 오래된 것이 앞.
     */
    private final List<PageSnapshot> entries = new ArrayList<PageSnapshot>();

    /**
     * Index of the current entry.
     * <p>
     * 현재 항목의 위치.
     */
    private int index;

    /**
     * Creates a history that holds one entry.
     * <p>
     * 항목 하나를 가진 이력을 만든다.
     *
     * @param initial first entry (normally the blank page) / 첫 항목(보통 빈 페이지)
     */
    History(PageSnapshot initial) {
        entries.add(initial);
        index = 0;
    }

    /**
     * Returns the number of entries.
     * <p>
     * 항목 수를 반환한다.
     *
     * @return entry count / 항목 수
     */
    synchronized int size() {
        return entries.size();
    }

    /**
     * Returns the current position (0 based).
     * <p>
     * 현재 위치(0 기반)를 반환한다.
     *
     * @return position / 위치
     */
    synchronized int index() {
        return index;
    }

    /**
     * Returns the entry at a position.
     * <p>
     * 위치의 항목을 반환한다.
     *
     * @param i position / 위치
     * @return the entry / 항목
     */
    synchronized PageSnapshot get(int i) {
        return entries.get(i);
    }

    /**
     * Adds a new current entry after the current position, deleting forward entries.
     * <p>
     * 현재 위치 뒤에 새 현재 항목을 추가하며 앞쪽(forward) 항목은 삭제한다.
     *
     * @param snapshot the new page / 새 페이지
     * @return the entries that were deleted (their live objects are already gone) / 삭제된 항목(살아 있는 객체는 이미 없음)
     */
    synchronized List<PageSnapshot> push(PageSnapshot snapshot) {
        List<PageSnapshot> removed = new ArrayList<PageSnapshot>(entries.subList(index + 1, entries.size()));
        // 현재 위치보다 뒤의 항목을 지운다.
        while (entries.size() > index + 1) {
            entries.remove(entries.size() - 1);
        }
        entries.add(snapshot);
        index = entries.size() - 1;
        return removed;
    }

    /**
     * Replaces the current entry (refresh, or the page that was loaded for a history move).
     * <p>
     * 현재 항목을 교체한다(새로고침, 또는 이력 이동으로 읽은 페이지).
     *
     * @param snapshot new content of the current entry / 현재 항목의 새 내용
     */
    synchronized void replaceCurrent(PageSnapshot snapshot) {
        entries.set(index, snapshot);
    }

    /**
     * Replaces the entry at a position.
     * <p>
     * 위치의 항목을 교체한다.
     *
     * @param i        position / 위치
     * @param snapshot new content / 새 내용
     */
    synchronized void set(int i, PageSnapshot snapshot) {
        entries.set(i, snapshot);
    }

    /**
     * Moves the current position without changing any entry.
     * <p>
     * 항목을 바꾸지 않고 현재 위치만 옮긴다.
     *
     * @param newIndex new position / 새 위치
     */
    synchronized void moveTo(int newIndex) {
        index = newIndex;
    }

    /**
     * Returns a copy of all entry URLs.
     * <p>
     * 모든 항목 URL 의 복사본을 반환한다.
     *
     * @return URLs, oldest first / URL 목록(오래된 것이 앞)
     */
    synchronized Vector<String> urls() {
        Vector<String> result = new Vector<String>(entries.size());
        // 항목마다 URL 만 복사한다.
        for (int i = 0; i < entries.size(); i++) {
            result.add(entries.get(i).getUrl());
        }
        return result;
    }

    /**
     * Resets the history to one entry and returns the old entries so the caller can release them.
     * <p>
     * 이력을 항목 하나로 초기화하고 호출자가 해제할 수 있게 이전 항목을 돌려준다.
     *
     * @param initial the only entry afterwards / 초기화 후의 유일한 항목
     * @return the old entries / 이전 항목
     */
    synchronized List<PageSnapshot> reset(PageSnapshot initial) {
        List<PageSnapshot> old = new ArrayList<PageSnapshot>(entries);
        entries.clear();
        entries.add(initial);
        index = 0;
        return old;
    }
}
