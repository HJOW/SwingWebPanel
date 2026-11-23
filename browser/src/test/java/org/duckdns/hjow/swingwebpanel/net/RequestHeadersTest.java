package org.duckdns.hjow.swingwebpanel.net;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link RequestHeaders} (plan item P04-04 / API_CONTRACT 5.15): case-insensitive overwrite, User-Agent
 * exclusion, whole-call validation and copy semantics.
 * <p>
 * {@link RequestHeaders} 시험(계획 P04-04, API_CONTRACT 5.15): 대소문자 무관 덮어쓰기, User-Agent 제외, 호출 전체 검증, 복사본 의미.
 */
public class RequestHeadersTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RequestHeadersTest() {
    }

    /**
     * Builds a one-entry map.
     * <p>
     * 항목이 하나인 맵을 만든다.
     *
     * @param k key / 키
     * @param v value / 값
     * @return the map / 맵
     */
    private static Map<String, String> map(String k, String v) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(k, v);
        return m;
    }

    /**
     * A new value overwrites the old one by case-insensitive name and keeps other headers.
     * <p>
     * 새 값이 대소문자 구분 없는 이름으로 기존 값을 덮어쓰고 다른 헤더는 유지한다.
     */
    public void testOverwriteIsCaseInsensitiveAndKeepsOthers() {
        RequestHeaders h = new RequestHeaders();
        h.addAll(map("X-One", "1"));
        h.addAll(map("X-Two", "2"));
        h.addAll(map("x-ONE", "uno"));
        Check.equal("uno", h.get("X-One"), "overwritten");
        Check.equal("2", h.get("x-two"), "other header kept");
        Check.equal(2, h.size(), "no duplicate entry");
        Map<String, String> copy = h.toMap();
        Check.equal("[x-ONE, X-Two]", copy.keySet().toString(), "position kept and the latest spelling used");
    }

    /**
     * User-Agent in any letter case is ignored and never reported.
     * <p>
     * 어떤 대소문자의 User-Agent 도 무시하며 조회 결과에 나오지 않는다.
     */
    public void testUserAgentIsIgnored() {
        RequestHeaders h = new RequestHeaders();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("user-agent", "evil/1");
        m.put("USER-AGENT", "evil/2");
        m.put("User-Agent", "evil/3");
        m.put("X-Ok", "yes");
        h.addAll(m);
        Check.equal(1, h.size(), "only the ordinary header is stored");
        Check.isNull(h.get("User-Agent"), "not retrievable");
        Check.isFalse(h.toMap().containsKey("User-Agent"), "not in the copy");
    }

    /**
     * Invalid input rejects the whole call and nothing is applied.
     * <p>
     * 잘못된 입력은 호출 전체를 거부하고 아무것도 적용하지 않는다.
     */
    public void testInvalidInputIsRejectedAsAWhole() {
        final RequestHeaders h = new RequestHeaders();
        h.addAll(map("X-Keep", "k"));
        final Map<String, String> crlf = new LinkedHashMap<String, String>();
        crlf.put("X-Good", "g");
        crlf.put("X-Bad", "a\r\nInjected: 1");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a map with a CRLF value.
             * <p>
             * CRLF 값이 있는 맵을 추가한다.
             */
            public void run() {
                h.addAll(crlf);
            }
        }, "CR/LF in value");
        Check.isNull(h.get("X-Good"), "no partial application");
        Check.equal(1, h.size(), "unchanged after the rejection");
        final Map<Object, Object> raw = new HashMap<Object, Object>();
        raw.put("X-Num", Integer.valueOf(5));
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a map whose value is not a String.
             * <p>
             * 값이 String 이 아닌 맵을 추가한다.
             */
            public void run() {
                h.addAll(raw);
            }
        }, "non-String value");
        final Map<Object, Object> rawKey = new HashMap<Object, Object>();
        rawKey.put(Integer.valueOf(1), "v");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a map whose key is not a String.
             * <p>
             * 키가 String 이 아닌 맵을 추가한다.
             */
            public void run() {
                h.addAll(rawKey);
            }
        }, "non-String key");
        final Map<String, String> nullValue = new HashMap<String, String>();
        nullValue.put("X-Null", null);
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a map with a null value.
             * <p>
             * null 값이 있는 맵을 추가한다.
             */
            public void run() {
                h.addAll(nullValue);
            }
        }, "null value");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a null map.
             * <p>
             * null 맵을 추가한다.
             */
            public void run() {
                h.addAll(null);
            }
        }, "null map");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds an empty header name.
             * <p>
             * 빈 헤더 이름을 추가한다.
             */
            public void run() {
                h.addAll(map("", "v"));
            }
        }, "empty name");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds a header name with a colon.
             * <p>
             * 콜론이 있는 헤더 이름을 추가한다.
             */
            public void run() {
                h.addAll(map("Bad:Name", "v"));
            }
        }, "illegal character in name");
    }

    /**
     * The map returned by toMap() and copy() are independent of the original.
     * <p>
     * toMap() 과 copy() 가 돌려준 것은 원본과 독립적이다.
     */
    public void testCopiesAreIndependent() {
        RequestHeaders h = new RequestHeaders();
        h.addAll(map("X-A", "1"));
        Map<String, String> copy = h.toMap();
        copy.put("X-B", "2");
        RequestHeaders clone = h.copy();
        clone.addAll(map("X-A", "changed"));
        Check.equal(1, h.size(), "original unchanged by map edit");
        Check.equal("1", h.get("X-A"), "original unchanged by clone edit");
    }
}
