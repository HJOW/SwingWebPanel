package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Measures text for one layout run (plan item P09-03): the advance of a string in a font, plus letter spacing and word spacing. Results
 * are cached per font object for the duration of the run. Not thread-safe; one instance per layout.
 * <p>
 * 레이아웃 한 번 동안 텍스트를 측정한다(계획 P09-03): 글꼴에서 문자열의 advance 와 글자 간격, 단어 간격. 결과는 실행 동안 글꼴 객체별로
 * 캐시한다. 스레드 안전하지 않으며 레이아웃마다 하나를 쓴다.
 */
final class TextMeasure {

    /** Cache: font to (string to advance). 글꼴별 (문자열 → advance) 캐시. */
    private final Map<Font, Map<String, Double>> cache = new IdentityHashMap<Font, Map<String, Double>>();

    /**
     * Returns the advance of a string without any spacing.
     * <p>
     * 간격 없이 문자열의 advance 를 반환한다.
     *
     * @param f the font / 글꼴
     * @param s the string / 문자열
     * @return width in pixels / 픽셀 너비
     */
    double advance(Font f, String s) {
        if (s.length() == 0) {
            return 0;
        }
        Map<String, Double> m = cache.get(f);
        if (m == null) {
            m = new HashMap<String, Double>();
            cache.put(f, m);
        }
        Double w = m.get(s);
        if (w == null) {
            w = Double.valueOf(FontResolver.isComplex(s) ? FontResolver.simpleAdvance(f, s) : f.getStringBounds(s, FontResolver.FRC).getWidth());
            if (m.size() < 20000) {
                m.put(s, w);
            }
        }
        return w.doubleValue();
    }

    /**
     * Returns the width of a string including letter spacing (added after every code point) and word spacing (added after every space).
     * <p>
     * 글자 간격(코드 포인트마다 뒤에 더함)과 단어 간격(공백마다 뒤에 더함)을 포함한 문자열의 너비를 반환한다.
     *
     * @param f   the font / 글꼴
     * @param s   the string / 문자열
     * @param ls  letter spacing in pixels / 글자 간격(픽셀)
     * @param wsp word spacing in pixels / 단어 간격(픽셀)
     * @return width in pixels / 픽셀 너비
     */
    double width(Font f, String s, double ls, double wsp) {
        double w = advance(f, s);
        if (ls != 0) {
            w += ls * s.codePointCount(0, s.length());
        }
        if (wsp != 0) {
            int spaces = 0;
            // 단어 간격이 붙는 공백 문자를 센다.
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == ' ' || c == (char) 0xA0) {
                    spaces++;
                }
            }
            w += wsp * spaces;
        }
        return w;
    }
}
