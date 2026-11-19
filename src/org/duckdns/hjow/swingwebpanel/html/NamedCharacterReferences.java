package org.duckdns.hjow.swingwebpanel.html;

import java.util.HashMap;
import java.util.Map;

/**
 * The table of named character references (<code>&amp;amp;</code>, <code>&amp;eacute;</code>, <code>&amp;NotEqualTilde;</code>, ...) of the HTML
 * Standard with its 2231 entries. The data comes from the generated {@link NamedCharacterData} (produced from the standard's
 * <code>entities.json</code> by <code>tools/entitygen</code>) and is expanded into a hash table on first use. Keys are the names as
 * the standard lists them: with a trailing semicolon, and for the 106 legacy references also without it (<code>amp</code> and
 * <code>amp;</code>).
 * <p>
 * HTML 표준의 이름 있는 문자 참조(<code>&amp;amp;</code>, <code>&amp;eacute;</code>, <code>&amp;NotEqualTilde;</code> 등) 2231 항목의 표. 데이터는 생성된
 * {@link NamedCharacterData}(<code>tools/entitygen</code> 이 표준의 <code>entities.json</code> 에서 만듦)에서 오며 처음 쓸 때 해시 표로 펼친다.
 * 키는 표준이 나열한 이름 그대로이다: 끝에 세미콜론이 있고, 106 개의 옛 참조는 세미콜론이 없는 형태도 있다(<code>amp</code> 와 <code>amp;</code>).
 */
public final class NamedCharacterReferences {

    /**
     * Length of the longest name without the ampersand, including the semicolon (<code>CounterClockwiseContourIntegral;</code>).
     * <p>
     * 앰퍼샌드를 뺀 가장 긴 이름의 길이(세미콜론 포함, <code>CounterClockwiseContourIntegral;</code>).
     */
    public static final int MAX_NAME_LENGTH = 32;

    /**
     * Name (with or without the semicolon) to replacement text, built lazily.
     * <p>
     * 이름(세미콜론 포함 또는 없음)에서 치환 문자열로의 표. 처음 쓸 때 만든다.
     */
    private static Map<String, String> table;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private NamedCharacterReferences() {
    }

    /**
     * Expands the generated data on first use.
     * <p>
     * 처음 쓸 때 생성된 데이터를 펼친다.
     *
     * @return the table / 표
     */
    private static synchronized Map<String, String> table() {
        if (table != null) {
            return table;
        }
        Map<String, String> m = new HashMap<String, String>(4096);
        // 조각마다 공백으로 구분된 "이름:코드포인트,코드포인트" 항목을 푼다.
        for (int c = 0; c < NamedCharacterData.CHUNKS.length; c++) {
            String[] entries = NamedCharacterData.CHUNKS[c].split(" ");
            // 항목마다 이름과 코드 포인트를 분리한다.
            for (int i = 0; i < entries.length; i++) {
                int colon = entries[i].indexOf(':');
                String[] cps = entries[i].substring(colon + 1).split(",");
                StringBuilder sb = new StringBuilder(2);
                // 코드 포인트마다 문자열에 덧붙인다.
                for (int k = 0; k < cps.length; k++) {
                    sb.appendCodePoint(Integer.parseInt(cps[k]));
                }
                m.put(entries[i].substring(0, colon), sb.toString());
            }
        }
        table = m;
        return m;
    }

    /**
     * Returns the replacement text of a reference name.
     * <p>
     * 참조 이름의 치환 문자열을 반환한다.
     *
     * @param name name without the leading ampersand, with the trailing semicolon if the reference has one, for example
     *             <code>amp;</code> or <code>amp</code> / 앞의 앰퍼샌드를 뺀 이름. 참조에 세미콜론이 있으면 끝에 붙인다(예: <code>amp;</code>, <code>amp</code>)
     * @return one or two characters (two UTF-16 units for one supplementary code point, or two code points), or null if the name is not
     *         in the table / 한두 글자(보충 코드 포인트 하나의 UTF-16 두 단위 또는 코드 포인트 둘). 표에 없으면 null
     */
    public static String lookup(String name) {
        return table().get(name);
    }

    /**
     * Returns the number of entries of the table (2231 for the standard's data).
     * <p>
     * 표의 항목 수를 반환한다(표준 데이터에서는 2231).
     *
     * @return entry count / 항목 수
     */
    public static int size() {
        return table().size();
    }
}
