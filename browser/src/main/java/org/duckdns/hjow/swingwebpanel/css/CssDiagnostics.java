package org.duckdns.hjow.swingwebpanel.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects the diagnostics of the CSS engine: dropped declarations, unknown properties, invalid values, unsupported features and so on.
 * A browser silently ignores such input; this object keeps a record of it so that developers (and the feature matrix) can see which part of a
 * style sheet was not applied. Passing <code>null</code> wherever a collector is accepted switches the recording off. Instances are thread-safe.
 * <p>
 * CSS 엔진의 진단을 모은다: 버려진 선언, 알 수 없는 속성, 잘못된 값, 지원하지 않는 기능 등. 브라우저는 이런 입력을 조용히 무시하지만 이 객체는 그 기록을
 * 남겨 개발자(와 기능 표)가 스타일 시트의 어느 부분이 적용되지 않았는지 볼 수 있게 한다. 수집기를 받는 곳에 <code>null</code> 을 넘기면 기록하지 않는다.
 * 인스턴스는 스레드 안전하다.
 */
public final class CssDiagnostics {

    /** Kind: a declaration or rule was dropped because it is invalid. / 종류: 잘못되어 선언이나 규칙을 버렸다. */
    public static final String INVALID = "invalid";

    /** Kind: the property or feature is known but not applied by this engine. / 종류: 알려져 있지만 이 엔진이 적용하지 않는 속성이나 기능. */
    public static final String UNSUPPORTED = "unsupported";

    /** Kind: the property name is unknown. / 종류: 알 수 없는 속성 이름. */
    public static final String UNKNOWN = "unknown";

    /** Kind: a recoverable oddity (for example a missing semicolon). / 종류: 복구 가능한 이상 상황. */
    public static final String RECOVERED = "recovered";

    /** Kind: the property is parsed and stored but has no effect on rendering. / 종류: 속성을 파싱해 저장하지만 렌더링에는 영향이 없다. */
    public static final String PARSED_ONLY = "parsed-only";

    /** Maximum number of retained entries (older entries are kept, new ones are counted only). / 보관하는 최대 항목 수(초과분은 개수만 센다). */
    public static final int MAX_ENTRIES = 2000;

    /** Recorded entries. / 기록한 항목. */
    private final List<Entry> entries = new ArrayList<Entry>();

    /** Number of entries that were not retained. / 보관하지 못한 항목 수. */
    private int overflow;

    /**
     * One recorded diagnostic.
     * <p>
     * 기록된 진단 하나.
     */
    public static final class Entry {
        /** Kind constant. / 종류 상수. */
        private final String kind;
        /** Subject (property name, selector, rule). / 대상(속성 이름, 선택자, 규칙). */
        private final String subject;
        /** Human readable message. / 사람이 읽는 메시지. */
        private final String message;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param kind    kind constant / 종류 상수
         * @param subject subject / 대상
         * @param message message / 메시지
         */
        Entry(String kind, String subject, String message) {
            this.kind = kind;
            this.subject = subject;
            this.message = message;
        }

        /**
         * Returns the kind.
         * <p>
         * 종류를 반환한다.
         *
         * @return kind constant / 종류 상수
         */
        public String getKind() {
            return kind;
        }

        /**
         * Returns the subject.
         * <p>
         * 대상을 반환한다.
         *
         * @return subject / 대상
         */
        public String getSubject() {
            return subject;
        }

        /**
         * Returns the message.
         * <p>
         * 메시지를 반환한다.
         *
         * @return message / 메시지
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns a one-line description.
         * <p>
         * 한 줄 설명을 반환한다.
         *
         * @return text / 문자열
         */
        public String toString() {
            return kind + ": " + subject + " - " + message;
        }
    }

    /**
     * Records a diagnostic. A null collector is handled by {@link #report}.
     * <p>
     * 진단을 기록한다. null 수집기는 {@link #report} 가 처리한다.
     *
     * @param kind    kind constant / 종류 상수
     * @param subject subject / 대상
     * @param message message / 메시지
     */
    public synchronized void add(String kind, String subject, String message) {
        if (entries.size() >= MAX_ENTRIES) {
            overflow++;
            return;
        }
        entries.add(new Entry(kind, subject, message));
    }

    /**
     * Records a diagnostic into a collector that may be null.
     * <p>
     * null 일 수 있는 수집기에 진단을 기록한다.
     *
     * @param d       collector or null / 수집기 또는 null
     * @param kind    kind constant / 종류 상수
     * @param subject subject / 대상
     * @param message message / 메시지
     */
    public static void report(CssDiagnostics d, String kind, String subject, String message) {
        if (d != null) {
            d.add(kind, subject, message);
        }
    }

    /**
     * Returns a snapshot of the recorded entries.
     * <p>
     * 기록된 항목의 복사본을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public synchronized List<Entry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    /**
     * Counts the entries of a kind.
     * <p>
     * 종류별 항목 수를 센다.
     *
     * @param kind kind constant, or null for all / 종류 상수. null 이면 전체
     * @return count (including entries that were not retained when kind is null) / 개수(kind 가 null 이면 보관하지 못한 항목 포함)
     */
    public synchronized int count(String kind) {
        if (kind == null) {
            return entries.size() + overflow;
        }
        int n = 0;
        // 요청한 종류와 같은 항목만 센다.
        for (int i = 0; i < entries.size(); i++) {
            if (kind.equals(entries.get(i).getKind())) {
                n++;
            }
        }
        return n;
    }

    /**
     * Tells whether an entry of the kind mentions the subject.
     * <p>
     * 해당 종류의 항목 중 대상이 일치하는 것이 있는지 알려 준다.
     *
     * @param kind    kind constant / 종류 상수
     * @param subject subject text / 대상 문자열
     * @return true if found / 있으면 true
     */
    public synchronized boolean has(String kind, String subject) {
        // 종류와 대상이 모두 같은 항목을 찾는다.
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (kind.equals(e.getKind()) && subject.equals(e.getSubject())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all entries.
     * <p>
     * 모든 항목을 지운다.
     */
    public synchronized void clear() {
        entries.clear();
        overflow = 0;
    }
}
