package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The console messages of one page view (plan item P31-09): what the scripts of the pages of a tab wrote to the console and what the
 * browser reported about them, kept so that the console window can show messages that arrived before it was opened. Only the newest
 * {@link #LIMIT} messages are kept. It has no Swing parts; the tab fills it on the event dispatch thread and the console window reads it
 * there.
 * <p>
 * 페이지 보기 하나의 콘솔 메시지(계획 P31-09): 탭의 페이지 스크립트가 콘솔에 쓴 것과 브라우저가 그것에 대해 보고한 것을 보관하여, 콘솔 창이 열리기
 * 전에 도착한 메시지도 보일 수 있게 한다. 가장 최근의 {@link #LIMIT} 개만 보관한다. Swing 부품이 없다. 탭이 이벤트 디스패치 스레드에서 채우고
 * 콘솔 창이 거기서 읽는다.
 */
public final class ConsoleLog {

    /** The largest number of messages that are kept. / 보관하는 메시지의 최대 개수. */
    public static final int LIMIT = 1000;

    /** Rank of the level of debugging output. / 디버깅 출력 수준의 순위. */
    public static final int DEBUG = 0;

    /** Rank of the level of ordinary output. / 보통 출력 수준의 순위. */
    public static final int LOG = 1;

    /** Rank of the level of warnings. / 경고 수준의 순위. */
    public static final int WARN = 2;

    /** Rank of the level of errors. / 오류 수준의 순위. */
    public static final int ERROR = 3;

    /**
     * One console message.
     * <p>
     * 콘솔 메시지 하나.
     */
    public static final class Entry {

        /** The name of the level in lower case, for example <code>error</code>. / 소문자로 쓴 수준 이름. 예: <code>error</code>. */
        private final String level;

        /** The message. / 메시지. */
        private final String message;

        /** Where the message came from (script address and line), or empty. / 메시지가 나온 곳(스크립트 주소와 줄). 없으면 빈 문자열. */
        private final String where;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param level   the name of the level / 수준 이름
         * @param message the message / 메시지
         * @param where   where it came from, or null / 나온 곳 또는 null
         */
        public Entry(String level, String message, String where) {
            this.level = level == null ? "log" : level.toLowerCase(java.util.Locale.ENGLISH);
            this.message = message == null ? "" : message;
            this.where = where == null ? "" : where;
        }

        /**
         * Returns the name of the level in lower case.
         * <p>
         * 소문자로 쓴 수준 이름을 반환한다.
         *
         * @return the level name / 수준 이름
         */
        public String getLevel() {
            return level;
        }

        /**
         * Returns the rank of the level: {@link ConsoleLog#DEBUG}, {@link ConsoleLog#LOG}, {@link ConsoleLog#WARN} or
         * {@link ConsoleLog#ERROR}. Levels that are not warnings, errors or debugging output count as ordinary output.
         * <p>
         * 수준의 순위를 반환한다: {@link ConsoleLog#DEBUG}, {@link ConsoleLog#LOG}, {@link ConsoleLog#WARN}, {@link ConsoleLog#ERROR} 중
         * 하나. 경고, 오류, 디버깅 출력이 아닌 수준은 보통 출력으로 친다.
         *
         * @return the rank / 순위
         */
        public int getRank() {
            if ("error".equals(level)) {
                return ERROR;
            }
            if ("warn".equals(level) || "warning".equals(level)) {
                return WARN;
            }
            return "debug".equals(level) ? DEBUG : LOG;
        }

        /**
         * Returns the message.
         * <p>
         * 메시지를 반환한다.
         *
         * @return the message / 메시지
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns where the message came from.
         * <p>
         * 메시지가 나온 곳을 반환한다.
         *
         * @return the script address and line, or the empty string / 스크립트 주소와 줄. 없으면 빈 문자열
         */
        public String getWhere() {
            return where;
        }

        /**
         * Returns the entry as one line of the console window: the level in brackets, the message, and the place in parentheses.
         * <p>
         * 항목을 콘솔 창의 한 줄로 반환한다: 대괄호 안의 수준, 메시지, 괄호 안의 위치.
         *
         * @return the line / 줄
         */
        public String toString() {
            return "[" + level + "] " + message + (where.length() == 0 ? "" : "  (" + where + ")");
        }
    }

    /**
     * Receives the changes of a log.
     * <p>
     * 로그의 변경을 받는다.
     */
    public interface Listener {

        /**
         * Called after an entry was added.
         * <p>
         * 항목이 더해진 뒤에 호출된다.
         *
         * @param log   the log / 로그
         * @param entry the new entry / 새 항목
         */
        void entryAdded(ConsoleLog log, Entry entry);

        /**
         * Called after the log was emptied.
         * <p>
         * 로그가 비워진 뒤에 호출된다.
         *
         * @param log the log / 로그
         */
        void logCleared(ConsoleLog log);
    }

    /** The entries, the oldest first. / 항목. 오래된 것이 앞에 있다. */
    private final List<Entry> entries = new ArrayList<Entry>();

    /** The receivers of changes. / 변경을 받는 쪽. */
    private final List<Listener> listeners = new ArrayList<Listener>();

    /** How many old entries were dropped because the log was full. / 로그가 가득 차서 버린 옛 항목의 수. */
    private int dropped;

    /**
     * Creates an empty log.
     * <p>
     * 빈 로그를 만든다.
     */
    public ConsoleLog() {
    }

    /**
     * Adds a message; the oldest one is dropped if the log is full.
     * <p>
     * 메시지를 더한다. 로그가 가득 찼으면 가장 오래된 것을 버린다.
     *
     * @param level   the name of the level / 수준 이름
     * @param message the message / 메시지
     * @param where   where it came from, or null / 나온 곳 또는 null
     * @return the new entry / 새 항목
     */
    public Entry add(String level, String message, String where) {
        Entry e = new Entry(level, message, where);
        entries.add(e);
        // 한도를 넘으면 오래된 것부터 버린다.
        while (entries.size() > LIMIT) {
            entries.remove(0);
            dropped++;
        }
        Listener[] all = listeners.toArray(new Listener[listeners.size()]);
        // 리스너마다 새 항목을 알린다.
        for (int i = 0; i < all.length; i++) {
            all[i].entryAdded(this, e);
        }
        return e;
    }

    /**
     * Removes all messages.
     * <p>
     * 모든 메시지를 지운다.
     */
    public void clear() {
        entries.clear();
        dropped = 0;
        Listener[] all = listeners.toArray(new Listener[listeners.size()]);
        // 리스너마다 비워졌음을 알린다.
        for (int i = 0; i < all.length; i++) {
            all[i].logCleared(this);
        }
    }

    /**
     * Returns the messages, the oldest first.
     * <p>
     * 메시지를 오래된 것부터 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<Entry> getEntries() {
        return new ArrayList<Entry>(entries);
    }

    /**
     * Returns the messages whose level has at least a rank, the oldest first.
     * <p>
     * 수준이 주어진 순위 이상인 메시지를 오래된 것부터 반환한다.
     *
     * @param minimumRank {@link #DEBUG}, {@link #LOG}, {@link #WARN} or {@link #ERROR} / {@link #DEBUG}, {@link #LOG}, {@link #WARN},
     *                    {@link #ERROR} 중 하나
     * @return the messages / 메시지
     */
    public List<Entry> getEntries(int minimumRank) {
        List<Entry> out = new ArrayList<Entry>();
        // 항목마다 순위가 충분하면 고른다.
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getRank() >= minimumRank) {
                out.add(entries.get(i));
            }
        }
        return out;
    }

    /**
     * Returns how many old messages were dropped because the log was full.
     * <p>
     * 로그가 가득 차서 버린 옛 메시지의 수를 반환한다.
     *
     * @return the number / 개수
     */
    public int getDroppedCount() {
        return dropped;
    }

    /**
     * Registers a receiver of changes.
     * <p>
     * 변경을 받을 쪽을 등록한다.
     *
     * @param listener the receiver / 받는 쪽
     */
    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a receiver of changes.
     * <p>
     * 변경을 받는 쪽을 뺀다.
     *
     * @param listener the receiver / 받는 쪽
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
