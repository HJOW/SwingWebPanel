package org.duckdns.hjow.swingwebpanel.layout;

/**
 * One counter style (CSS Counter Styles Level 3, <a href="https://www.w3.org/TR/css-counter-styles-3/">https://www.w3.org/TR/css-counter-styles-3/</a>):
 * the way a number is written, by a <i>system</i> (cyclic, fixed, symbolic, alphabetic, numeric, additive) that turns a value into symbols, the sign for
 * negative values, the range of the values it can write, the padding, and the <i>fallback</i> style that writes the values outside the range. The
 * methods only compute the representation of a value (section 3, "Counter Style Algorithms"); the lookup of the fallback style by name is the job of
 * {@link CounterStyles}.
 * <p>
 * 카운터 스타일 하나(CSS Counter Styles Level 3, <a href="https://www.w3.org/TR/css-counter-styles-3/">https://www.w3.org/TR/css-counter-styles-3/</a>): 숫자를
 * 쓰는 방법. 값을 기호로 바꾸는 <i>시스템</i>(cyclic, fixed, symbolic, alphabetic, numeric, additive), 음수 부호, 쓸 수 있는 값의 범위, 채우기, 범위 밖의 값을 쓰는
 * <i>대체</i> 스타일로 이루어진다. 메서드는 값의 표현만 계산한다(3 절 "Counter Style Algorithms"). 대체 스타일을 이름으로 찾는 일은 {@link CounterStyles} 가 한다.
 */
final class CounterStyle {

    /** System: the symbols repeat. / 시스템: 기호가 되풀이된다. */
    static final int CYCLIC = 0;
    /** System: a symbol for each value from a first value. / 시스템: 첫 값부터 값마다 기호 하나. */
    static final int FIXED = 1;
    /** System: the symbols repeat and each round repeats the symbol once more. / 시스템: 기호가 되풀이되고 한 바퀴마다 기호를 한 번 더 반복한다. */
    static final int SYMBOLIC = 2;
    /** System: bijective numeration (a, b, ..., z, aa, ab, ...). / 시스템: 일대일 진법(a, b, ..., z, aa, ab, ...). */
    static final int ALPHABETIC = 3;
    /** System: positional numeration. / 시스템: 위치 진법. */
    static final int NUMERIC = 4;
    /** System: a sum of weights. / 시스템: 가중치의 합. */
    static final int ADDITIVE = 5;

    /** The system. / 시스템. */
    int system;

    /** The symbols of the cyclic, fixed, symbolic, alphabetic and numeric systems. / cyclic, fixed, symbolic, alphabetic, numeric 시스템의 기호. */
    String[] symbols = new String[0];

    /** The weights of the additive system, in descending order. / additive 시스템의 가중치. 내림차순. */
    int[] weights = new int[0];

    /** The symbols of the additive system, one for each weight. / additive 시스템의 기호. 가중치마다 하나. */
    String[] additive = new String[0];

    /** The first value of the fixed system. / fixed 시스템의 첫 값. */
    int first = 1;

    /** The text before a negative value. / 음수 앞의 텍스트. */
    String negativePrefix = "-";

    /** The text after a negative value. / 음수 뒤의 텍스트. */
    String negativeSuffix = "";

    /** The text before the representation in a list marker. / 목록 표식에서 표현 앞의 텍스트. */
    String prefix = "";

    /** The text after the representation in a list marker. / 목록 표식에서 표현 뒤의 텍스트. */
    String suffix = ". ";

    /** The lowest value the style can write. / 쓸 수 있는 가장 낮은 값. */
    long lower = Long.MIN_VALUE;

    /** The highest value the style can write. / 쓸 수 있는 가장 높은 값. */
    long upper = Long.MAX_VALUE;

    /** The minimum length of the representation in symbols; 0 for none. / 표현의 최소 길이(기호 수). 없으면 0. */
    int padLength;

    /** The symbol that pads the representation. / 표현을 채우는 기호. */
    String padSymbol = "";

    /** The name of the style that writes the values this one cannot, or null for <code>decimal</code>. / 이 스타일이 쓸 수 없는 값을 쓰는 스타일의 이름. <code>decimal</code> 이면 null. */
    String fallback;

    /**
     * Returns the representation of a value with the sign and the padding, or null if the style cannot write it: the value is outside the range, or
     * the system has no representation of it (the symbolic and alphabetic systems have none for values below 1, the fixed system none outside its
     * symbols, the additive system none for a value that its weights cannot sum to), and the caller then uses the fallback.
     * <p>
     * 값의 표현을 부호와 채우기와 함께 반환한다. 스타일이 쓸 수 없으면 null(호출한 쪽이 대체 스타일을 쓴다).
     *
     * @param value the counter value / 카운터 값
     * @return the representation or null / 표현. 없으면 null
     */
    String represent(long value) {
        if (value < lower || value > upper) {
            return null;
        }
        boolean negative = value < 0 && system != CYCLIC && system != FIXED;
        String core = basic(negative ? -value : value);
        if (core == null) {
            return null;
        }
        // 부호와 채우기를 더한다: 채우기는 부호까지 포함한 길이를 기준으로 앞에 붙인다.
        int len = core.codePointCount(0, core.length());
        if (negative) {
            len += negativePrefix.codePointCount(0, negativePrefix.length()) + negativeSuffix.codePointCount(0, negativeSuffix.length());
        }
        StringBuilder pad = new StringBuilder();
        // 최소 길이에 모자란 만큼 채움 기호를 앞에 붙인다.
        for (int i = len; i < padLength && padSymbol.length() > 0; i++) {
            pad.append(padSymbol);
        }
        return negative ? negativePrefix + pad + core + negativeSuffix : pad + core;
    }

    /**
     * Returns the symbols of a non-negative value by the system, or null if the system has no representation of it.
     * <p>
     * 음이 아닌 값의 기호를 시스템에 따라 반환한다. 시스템에 그 표현이 없으면 null.
     *
     * @param v the value (negative only for the cyclic and fixed systems) / 값(cyclic 과 fixed 시스템에서만 음수 가능)
     * @return the text or null / 텍스트. 없으면 null
     */
    private String basic(long v) {
        int n = symbols.length;
        switch (system) {
        case CYCLIC:
            return n == 0 ? null : symbols[(int) (((v - 1) % n + n) % n)];
        case FIXED:
            long i = v - first;
            return i >= 0 && i < n ? symbols[(int) i] : null;
        case SYMBOLIC:
            if (v < 1 || n == 0) {
                return null;
            }
            StringBuilder s = new StringBuilder();
            long times = (v - 1) / n + 1;
            // 한 바퀴를 돌 때마다 기호를 한 번 더 반복한다.
            for (long k = 0; k < times && s.length() < 4096; k++) {
                s.append(symbols[(int) ((v - 1) % n)]);
            }
            return s.toString();
        case ALPHABETIC:
            if (v < 1 || n < 2) {
                return null;
            }
            StringBuilder a = new StringBuilder();
            long x = v;
            // 일대일 진법의 자리수를 낮은 자리부터 뽑아 앞에 붙인다.
            while (x > 0) {
                x--;
                a.insert(0, symbols[(int) (x % n)]);
                x /= n;
            }
            return a.toString();
        case NUMERIC:
            if (n < 2) {
                return null;
            }
            if (v == 0) {
                return symbols[0];
            }
            StringBuilder d = new StringBuilder();
            long y = v;
            // 위치 진법의 자리수를 낮은 자리부터 뽑아 앞에 붙인다.
            while (y > 0) {
                d.insert(0, symbols[(int) (y % n)]);
                y /= n;
            }
            return d.toString();
        case ADDITIVE:
            return additive(v);
        default:
            return null;
        }
    }

    /**
     * Writes a value as a sum of the weights of the additive system, greedily from the largest weight.
     * <p>
     * 값을 additive 시스템의 가중치의 합으로 큰 가중치부터 탐욕적으로 쓴다.
     *
     * @param v the value / 값
     * @return the text, or null if the weights cannot make the value / 텍스트. 가중치로 만들 수 없으면 null
     */
    private String additive(long v) {
        if (v == 0) {
            // 가중치 0 인 기호가 있으면 0 을 그 기호로 쓴다.
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] == 0) {
                    return additive[i];
                }
            }
            return null;
        }
        StringBuilder sb = new StringBuilder();
        long rest = v;
        // 큰 가중치부터 가능한 만큼 반복해 쓴다.
        for (int i = 0; i < weights.length && rest > 0; i++) {
            if (weights[i] <= 0) {
                continue;
            }
            long times = rest / weights[i];
            // 가중치를 나눈 몫만큼 기호를 되풀이해 쓴다.
            for (long k = 0; k < times && sb.length() < 4096; k++) {
                sb.append(additive[i]);
            }
            rest -= times * weights[i];
        }
        return rest == 0 ? sb.toString() : null;
    }
}
