package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The console log of a tab (plan item P31-09): entries with their levels and lines, the filter by level, the limit, clearing and the
 * listeners.
 * <p>
 * 탭의 콘솔 로그(계획 P31-09): 수준과 줄이 있는 항목, 수준으로 거르기, 한도, 지우기, 리스너.
 */
public class ConsoleLogTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ConsoleLogTest() {
    }

    /**
     * Entries keep their level, message and place; the line of an entry shows all three; the ranks order the levels.
     * <p>
     * 항목은 수준, 메시지, 위치를 가진다. 항목의 줄은 셋을 모두 보인다. 순위가 수준의 순서를 정한다.
     */
    public void testEntriesAndLevels() {
        ConsoleLog log = new ConsoleLog();
        log.add("LOG", "hello", null);
        log.add("WARN", "careful", "http://h/a.js:3");
        log.add("ERROR", "broken", "");
        log.add("DEBUG", "detail", null);
        log.add("INFO", "note", null);
        log.add(null, null, null);
        List<ConsoleLog.Entry> all = log.getEntries();
        Check.equal(6, all.size(), "every message is kept");
        Check.equal("[log] hello", all.get(0).toString(), "a line without a place");
        Check.equal("[warn] careful  (http://h/a.js:3)", all.get(1).toString(), "a line with a place");
        Check.equal("error", all.get(2).getLevel(), "the level name is in lower case");
        Check.equal(ConsoleLog.ERROR, all.get(2).getRank(), "errors rank highest");
        Check.equal(ConsoleLog.WARN, all.get(1).getRank(), "then warnings");
        Check.equal(ConsoleLog.LOG, all.get(4).getRank(), "info counts as ordinary output");
        Check.equal(ConsoleLog.DEBUG, all.get(3).getRank(), "debugging output ranks lowest");
        Check.equal("[log] ", all.get(5).toString(), "missing values become the ordinary level and an empty message");
        Check.equal(5, log.getEntries(ConsoleLog.LOG).size(), "the filter leaves out debugging output");
        Check.equal(2, log.getEntries(ConsoleLog.WARN).size(), "warnings and errors");
        Check.equal(1, log.getEntries(ConsoleLog.ERROR).size(), "errors only");
        all.clear();
        Check.equal(6, log.getEntries().size(), "the returned list is a copy");
    }

    /**
     * The log keeps only the newest messages and counts what it dropped; clearing empties it; listeners hear both.
     * <p>
     * 로그는 가장 최근 메시지만 보관하고 버린 수를 센다. 지우면 비워진다. 리스너가 둘 다 듣는다.
     */
    public void testLimitClearAndListeners() {
        ConsoleLog log = new ConsoleLog();
        final List<String> heard = new ArrayList<String>();
        ConsoleLog.Listener listener = new ConsoleLog.Listener() {
            /** Remembers the new entry. / 새 항목을 기억한다. */
            public void entryAdded(ConsoleLog source, ConsoleLog.Entry entry) {
                heard.add(entry.getMessage());
            }

            /** Remembers the clearing. / 지워졌음을 기억한다. */
            public void logCleared(ConsoleLog source) {
                heard.add("(cleared)");
            }
        };
        log.addListener(listener);
        log.addListener(listener);
        // 한도보다 다섯 개 많이 더한다.
        for (int i = 0; i < ConsoleLog.LIMIT + 5; i++) {
            log.add("log", "m" + i, null);
        }
        Check.equal(ConsoleLog.LIMIT, log.getEntries().size(), "only the newest messages are kept");
        Check.equal(5, log.getDroppedCount(), "the dropped ones are counted");
        Check.equal("m5", log.getEntries().get(0).getMessage(), "the oldest are dropped first");
        Check.equal(ConsoleLog.LIMIT + 5, heard.size(), "a listener that is registered twice hears each message once");
        log.clear();
        Check.equal(0, log.getEntries().size(), "clearing empties the log");
        Check.equal(0, log.getDroppedCount(), "and the count of dropped messages");
        Check.equal("(cleared)", heard.get(heard.size() - 1), "the listener hears the clearing");
        log.removeListener(listener);
        log.add("log", "late", null);
        Check.equal("(cleared)", heard.get(heard.size() - 1), "a removed listener hears nothing more");
    }
}
