package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.LinkedHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpCompiler;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpFlags;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpLimitException;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpProgram;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpSyntaxException;

/**
 * Matches the value of a control against its <code>pattern</code> attribute with the project's own JavaScript regular expression engine
 * (not <code>java.util.regex</code>): the pattern is compiled as <code>^(?:pattern)$</code> with the unicode flag, as the HTML Standard says
 * (the standard now says the <code>v</code> flag; the engine implements the <code>u</code> flag of ECMAScript 2015). An invalid pattern is
 * ignored: the constraint is then never violated. Compiled programs are cached.
 * <p>
 * 컨트롤의 값을 <code>pattern</code> 속성에 대해 프로젝트의 자체 JavaScript 정규식 엔진(<code>java.util.regex</code> 아님)으로 맞춰 본다: 패턴은 HTML 표준대로
 * unicode 플래그를 주고 <code>^(?:pattern)$</code> 로 컴파일한다(표준은 지금은 <code>v</code> 플래그를 말하지만 엔진은 ECMAScript 2015 의 <code>u</code> 플래그를
 * 구현한다). 올바르지 않은 패턴은 무시한다: 그러면 이 제약은 위반되지 않는다. 컴파일한 프로그램은 캐시한다.
 */
final class PatternMatcher {

    /** The maximum number of cached programs. / 캐시할 프로그램의 최대 수. */
    private static final int CACHE_SIZE = 64;

    /** The cache: pattern text to program (a missing program is cached as the marker {@link #INVALID}). / 캐시: 패턴 문자열에서 프로그램으로(없는 프로그램은 표지 {@link #INVALID} 로 캐시). */
    private static final Map<String, RegExpProgram> CACHE = new LinkedHashMap<String, RegExpProgram>() {
        /** The serial version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /**
         * Removes the eldest entry when the cache is full.
         * <p>
         * 캐시가 가득 차면 가장 오래된 항목을 제거한다.
         *
         * @param eldest the eldest entry / 가장 오래된 항목
         * @return true to remove it / 제거하려면 true
         */
        protected boolean removeEldestEntry(Map.Entry<String, RegExpProgram> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** The marker of a pattern that does not compile. / 컴파일되지 않는 패턴의 표지. */
    private static final RegExpProgram INVALID;

    static {
        RegExpProgram p = null;
        try {
            p = RegExpCompiler.compile("x", 0);
        } catch (RegExpSyntaxException e) {
            // 상수 패턴이므로 일어나지 않는다.
            throw new IllegalStateException(e);
        }
        INVALID = p;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PatternMatcher() {
    }

    /**
     * Tells whether a value matches a pattern as a whole.
     * <p>
     * 값이 패턴 전체와 일치하는지 알려 준다.
     *
     * @param pattern the pattern attribute / pattern 속성
     * @param value   the value / 값
     * @return TRUE if it matches, FALSE if not, null if the pattern is not valid (or took too long) / 일치하면 TRUE, 아니면 FALSE, 패턴이 올바르지 않으면(또는 너무 오래 걸리면) null
     */
    static Boolean matches(String pattern, String value) {
        RegExpProgram program;
        synchronized (CACHE) {
            program = CACHE.get(pattern);
            if (program == null) {
                try {
                    program = RegExpCompiler.compile("^(?:" + pattern + ")$", RegExpFlags.UNICODE);
                } catch (RegExpSyntaxException e) {
                    program = INVALID;
                } catch (RuntimeException e) {
                    program = INVALID;
                }
                CACHE.put(pattern, program);
            }
        }
        if (program == INVALID) {
            return null;
        }
        try {
            return program.exec(value, 0) != null ? Boolean.TRUE : Boolean.FALSE;
        } catch (RegExpLimitException e) {
            return null;
        }
    }
}
