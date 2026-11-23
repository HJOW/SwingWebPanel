package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The orientation of the characters of vertical text (plan item P10-03-e, CSS Writing Modes Level 3, W3C REC 2019-12-10, sections 5.1
 * and 9.1, and the <code>digits</code> value of Level 4): for every character of a text it decides whether the character is typeset
 * <i>sideways</i> (rotated a quarter turn clockwise like horizontal text), <i>upright</i> (its normal orientation, one cell of one em
 * along the line per typographic character unit) or <i>combined</i> (<code>text-combine-upright</code>: several characters composed
 * horizontally into the space of one upright character).
 * <p>
 * <code>text-orientation: mixed</code> (the initial value) typesets a character upright if its <code>Vertical_Orientation</code> property
 * of UAX #50 is <code>U</code>, <code>Tu</code> or <code>Tr</code> and sideways if it is <code>R</code> (section 5.1.2); the table is
 * generated from the Unicode data file by <code>tools/vogen</code> ({@link VerticalOrientationData}). <code>upright</code> typesets every
 * character upright, <code>sideways</code> none. White space (the space, the tab, the line feed, the form feed, the carriage return and
 * the no-break space) is always sideways, a combining mark follows the character before it (a typographic character unit, a
 * <i>cluster</i>, is one base character with its combining marks). <code>text-combine-upright: all</code> combines all characters of
 * a text run, <code>digits n</code> every maximal run of 1 to n ASCII digits (n is 2 to 4, 2 if omitted); a text run is the text of
 * one text node, so a sequence that is interrupted by a box boundary is not rejoined (documented limit). Further documented limits: the
 * vertical glyph forms of upright punctuation and brackets (the OpenType <code>vert</code> feature, the <code>Tu</code> and
 * <code>Tr</code> characters of UAX #50) are not applied, such characters are set with their normal glyphs in their cell; the
 * cell of an upright character is one em, not the vertical advance of the font; the baselines of different fonts in one line are
 * aligned at the alphabetic baseline of the logical line.
 * <p>
 * 세로 텍스트 글자의 방향(계획 P10-03-e, CSS Writing Modes Level 3, W3C REC 2019-12-10, 5.1 절과 9.1 절, 그리고 Level 4 의 <code>digits</code> 값):
 * 텍스트의 글자마다 <i>눕힘</i>(가로 텍스트처럼 시계 방향으로 사분원 회전), <i>똑바로</i>(본래 방향, 타이포그래피 문자 단위마다 줄을 따라 1em 한 칸)
 * 또는 <i>조합</i>(<code>text-combine-upright</code>: 여러 글자를 가로로 짜서 똑바로 선 한 글자의 공간에 넣음)을 정한다.
 * <p>
 * <code>text-orientation: mixed</code>(초기값)는 UAX #50 의 <code>Vertical_Orientation</code> 속성이 <code>U</code>, <code>Tu</code>, <code>Tr</code> 인 글자를 똑바로,
 * <code>R</code> 인 글자를 눕혀서 쓴다(5.1.2 절). 표는 <code>tools/vogen</code> 이 유니코드 데이터 파일에서 생성한다({@link VerticalOrientationData}).
 * <code>upright</code> 는 모든 글자를 똑바로, <code>sideways</code> 는 하나도 똑바로 세우지 않는다. 공백(스페이스, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴, 줄바꿈 없는 공백)은
 * 언제나 눕히고 결합 부호는 앞 글자를 따른다(타이포그래피 문자 단위, <i>클러스터</i>는 기본 글자 하나와 그 결합 부호이다). <code>text-combine-upright: all</code> 은 텍스트 구간의
 * 모든 글자를, <code>digits n</code> 은 1~n 자리의 ASCII 숫자가 이어진 최대 구간을 조합한다(n 은 2~4, 생략하면 2). 텍스트 구간은 텍스트 노드 하나의 텍스트이므로 상자
 * 경계로 끊긴 연속은 다시 잇지 않는다(문서화된 한계). 그 밖의 문서화된 한계: 똑바로 선 문장 부호와 괄호의 세로 자형(OpenType <code>vert</code> 기능, UAX #50 의
 * <code>Tu</code>, <code>Tr</code> 글자)은 적용하지 않고 그런 글자도 자기 칸에 평범한 글리프로 놓는다. 똑바로 선 글자의 칸은 글꼴의 세로 advance 가 아니라 1em 이다.
 * 한 줄 안에서 서로 다른 글꼴의 기준선은 논리적 줄의 알파벳 기준선에 맞춘다.
 */
final class VerticalText {

    /** Run kind: the character is typeset sideways (rotated). 구간 종류: 글자를 눕혀서(회전해서) 쓴다. */
    static final byte SIDE = 0;
    /** Run kind: the character is typeset upright. 구간 종류: 글자를 똑바로 쓴다. */
    static final byte UP = 1;
    /** Run kind: the character belongs to a horizontally composed group (<code>text-combine-upright</code>). 구간 종류: 가로로 조합한 묶음에 속한다. */
    static final byte COMBO = 2;

    /** Orientation mode: all text sideways. 방향 모드: 모든 텍스트를 눕힘. */
    static final int ORIENT_SIDEWAYS = 0;
    /** Orientation mode: mixed (UAX #50). 방향 모드: 혼합(UAX #50). */
    static final int ORIENT_MIXED = 1;
    /** Orientation mode: all text upright. 방향 모드: 모든 텍스트를 똑바로. */
    static final int ORIENT_UPRIGHT = 2;

    /** Combine mode: no combination. 조합 모드: 조합 없음. */
    static final int COMBINE_NONE = 0;
    /** Combine mode: <code>all</code>. 조합 모드: <code>all</code>. */
    static final int COMBINE_ALL = -1;

    /** The sorted upright ranges {from, to, from, to, ...}, parsed on first use. 정렬된 똑바로 범위 {시작, 끝, ...}. 처음 쓸 때 해석한다. */
    private static int[] upright;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private VerticalText() {
    }

    /**
     * Returns the sorted upright ranges of the generated table.
     * <p>
     * 생성된 표의 정렬된 똑바로 범위를 반환한다.
     *
     * @return {from, to, from, to, ...} / {시작, 끝, 시작, 끝, ...}
     */
    private static synchronized int[] ranges() {
        if (upright == null) {
            int count = 0;
            // 조각마다 쌍의 수를 센다.
            for (int i = 0; i < VerticalOrientationData.CHUNKS.length; i++) {
                String c = VerticalOrientationData.CHUNKS[i];
                // 문자열의 하이픈(쌍의 구분자)을 센다.
                for (int k = 0; k < c.length(); k++) {
                    if (c.charAt(k) == '-') {
                        count++;
                    }
                }
            }
            int[] r = new int[count * 2];
            int n = 0;
            // 조각의 쌍을 16 진수로 읽는다.
            for (int i = 0; i < VerticalOrientationData.CHUNKS.length; i++) {
                String[] pairs = VerticalOrientationData.CHUNKS[i].split(" ");
                // 쌍마다 시작과 끝을 읽는다.
                for (int k = 0; k < pairs.length; k++) {
                    int dash = pairs[k].indexOf('-');
                    if (dash < 0) {
                        continue;
                    }
                    r[n++] = Integer.parseInt(pairs[k].substring(0, dash), 16);
                    r[n++] = Integer.parseInt(pairs[k].substring(dash + 1), 16);
                }
            }
            upright = r;
        }
        return upright;
    }

    /**
     * Tells whether the <code>Vertical_Orientation</code> of a code point (UAX #50) is <code>U</code>, <code>Tu</code> or <code>Tr</code>, which
     * <code>text-orientation: mixed</code> typesets upright.
     * <p>
     * 코드 포인트의 <code>Vertical_Orientation</code>(UAX #50)이 <code>U</code>, <code>Tu</code>, <code>Tr</code> 인지 알려 준다. <code>text-orientation: mixed</code> 는 이런
     * 글자를 똑바로 세운다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for upright characters / 똑바로 서는 글자이면 true
     */
    static boolean isUpright(int cp) {
        if (cp < 0xA7) {
            return false;
        }
        int[] r = ranges();
        int lo = 0;
        int hi = r.length / 2 - 1;
        // 범위를 이분 탐색한다.
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (cp < r[mid * 2]) {
                hi = mid - 1;
            } else if (cp > r[mid * 2 + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a code point is white space that is always typeset sideways.
     * <p>
     * 코드 포인트가 언제나 눕혀 쓰는 공백인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for the white space / 공백이면 true
     */
    static boolean isSpace(int cp) {
        return cp == ' ' || cp == '\t' || cp == '\n' || cp == '\f' || cp == '\r' || cp == 0xA0;
    }

    /**
     * Tells whether a code point is a combining mark (it belongs to the cluster of the character before it).
     * <p>
     * 코드 포인트가 결합 부호인지(앞 글자의 클러스터에 속하는지) 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for combining marks / 결합 부호이면 true
     */
    static boolean isMark(int cp) {
        int t = Character.getType(cp);
        return t == Character.NON_SPACING_MARK || t == Character.ENCLOSING_MARK || t == Character.COMBINING_SPACING_MARK || cp == 0x200D;
    }

    /**
     * Returns the orientation mode of a text: the writing mode keyword <code>sideways-rl</code>/<code>sideways-lr</code> forces sideways
     * text, otherwise <code>text-orientation</code> decides.
     * <p>
     * 텍스트의 방향 모드를 반환한다: 쓰기 모드 키워드 <code>sideways-rl</code>/<code>sideways-lr</code> 은 눕힌 텍스트를 강제하고 그 밖에는
     * <code>text-orientation</code> 이 정한다.
     *
     * @param s    the style of the text / 텍스트의 스타일
     * @param mode the <code>writing-mode</code> keyword of the block container / 블록 컨테이너의 <code>writing-mode</code> 키워드
     * @return one of the <code>ORIENT_*</code> constants / <code>ORIENT_*</code> 상수 중 하나
     */
    static int orientationOf(ComputedStyle s, String mode) {
        if (mode != null && mode.startsWith("sideways")) {
            return ORIENT_SIDEWAYS;
        }
        String o = Css.kw(s, Prop.TEXT_ORIENTATION);
        if ("upright".equals(o)) {
            return ORIENT_UPRIGHT;
        }
        if ("sideways".equals(o)) {
            return ORIENT_SIDEWAYS;
        }
        return ORIENT_MIXED;
    }

    /**
     * Returns the combine mode of a style: {@link #COMBINE_NONE}, {@link #COMBINE_ALL} or the largest number of digits (2 to 4) for
     * <code>digits</code>.
     * <p>
     * 스타일의 조합 모드를 반환한다: {@link #COMBINE_NONE}, {@link #COMBINE_ALL}, <code>digits</code> 이면 가장 긴 숫자 수(2~4).
     *
     * @param s the style / 스타일
     * @return the combine mode / 조합 모드
     */
    static int combineOf(ComputedStyle s) {
        CssValue v = s.get(Prop.TEXT_COMBINE_UPRIGHT);
        if (v == null) {
            return COMBINE_NONE;
        }
        if (v.isKeyword("all")) {
            return COMBINE_ALL;
        }
        if (v.isKeyword("digits")) {
            return 2;
        }
        if (v instanceof CssList) {
            CssList l = (CssList) v;
            // `digits n` 은 키워드 뒤에 정수가 오는 목록이다.
            if (l.size() > 0 && l.get(0).isKeyword("digits")) {
                if (l.size() > 1 && l.get(1) instanceof CssNumber) {
                    int n = (int) ((CssNumber) l.get(1)).getValue();
                    return Math.max(2, Math.min(4, n));
                }
                return 2;
            }
        }
        return COMBINE_NONE;
    }

    /**
     * Classifies the characters of a text: for every UTF-16 unit the run kind ({@link #SIDE}, {@link #UP} or {@link #COMBO}).
     * <p>
     * 텍스트의 글자를 분류한다: UTF-16 단위마다 구간 종류({@link #SIDE}, {@link #UP}, {@link #COMBO}).
     *
     * @param t       the text / 텍스트
     * @param orient  the orientation mode, one of the <code>ORIENT_*</code> constants / 방향 모드, <code>ORIENT_*</code> 상수 중 하나
     * @param combine the combine mode of {@link #combineOf} / {@link #combineOf} 의 조합 모드
     * @return the kinds, or null if the whole text is sideways / 종류. 텍스트 전체가 눕혀지면 null
     */
    static byte[] classify(String t, int orient, int combine) {
        int n = t.length();
        byte[] k = null;
        if (orient != ORIENT_SIDEWAYS) {
            int i = 0;
            byte prev = SIDE;
            // 코드 포인트마다 방향을 정한다. 결합 부호는 앞 글자를 따른다.
            while (i < n) {
                int cp = t.codePointAt(i);
                int len = Character.charCount(cp);
                byte kind;
                if (i > 0 && isMark(cp)) {
                    kind = prev;
                } else if (isSpace(cp)) {
                    kind = SIDE;
                } else if (orient == ORIENT_UPRIGHT) {
                    kind = UP;
                } else {
                    kind = isUpright(cp) ? UP : SIDE;
                }
                if (kind != SIDE) {
                    if (k == null) {
                        k = new byte[n];
                    }
                    // 서로게이트 쌍의 두 단위 모두에 같은 종류를 기록한다.
                    for (int j = 0; j < len; j++) {
                        k[i + j] = kind;
                    }
                }
                prev = kind;
                i += len;
            }
        }
        if (combine == COMBINE_ALL && n > 0) {
            k = new byte[n];
            // `all` 은 텍스트 구간 전체를 하나의 조합 묶음으로 만든다.
            for (int i = 0; i < n; i++) {
                k[i] = COMBO;
            }
        } else if (combine > 0) {
            int i = 0;
            // `digits n` 은 ASCII 숫자가 이어진 최대 구간 중 n 자리 이하인 것을 조합한다.
            while (i < n) {
                if (t.charAt(i) < '0' || t.charAt(i) > '9') {
                    i++;
                    continue;
                }
                int j = i;
                // 숫자가 이어지는 동안 구간 끝을 늘린다.
                while (j < n && t.charAt(j) >= '0' && t.charAt(j) <= '9') {
                    j++;
                }
                if (j - i <= combine) {
                    if (k == null) {
                        k = new byte[n];
                    }
                    // 구간의 모든 숫자를 조합 묶음으로 표시한다.
                    for (int q = i; q < j; q++) {
                        k[q] = COMBO;
                    }
                }
                i = j;
            }
        }
        return k;
    }

    /**
     * Returns the number of typographic character units (clusters) in a range of a text.
     * <p>
     * 텍스트 범위의 타이포그래피 문자 단위(클러스터) 수를 반환한다.
     *
     * @param t    the text / 텍스트
     * @param from start offset / 시작 위치
     * @param to   end offset (exclusive) / 끝 위치(제외)
     * @return the number of clusters / 클러스터 수
     */
    static int clusterCount(String t, int from, int to) {
        int count = 0;
        int i = from;
        // 코드 포인트를 훑으며 결합 부호가 아닌 것(클러스터의 시작)을 센다.
        while (i < to) {
            int cp = t.codePointAt(i);
            if (i == from || !isMark(cp)) {
                count++;
            }
            i += Character.charCount(cp);
        }
        return count;
    }

    /**
     * Returns the start offsets of the clusters of a text followed by the text length (so that cluster <i>k</i> is the range from element
     * <i>k</i> to element <i>k+1</i>).
     * <p>
     * 텍스트의 클러스터 시작 위치와 마지막에 텍스트 길이를 반환한다(클러스터 <i>k</i> 는 원소 <i>k</i> 부터 <i>k+1</i> 까지의 범위이다).
     *
     * @param t the text / 텍스트
     * @return the offsets / 위치
     */
    static int[] clusterOffsets(String t) {
        int[] tmp = new int[t.length() + 1];
        int count = 0;
        int i = 0;
        // 코드 포인트를 훑으며 클러스터의 시작 위치를 모은다.
        while (i < t.length()) {
            int cp = t.codePointAt(i);
            if (i == 0 || !isMark(cp)) {
                tmp[count++] = i;
            }
            i += Character.charCount(cp);
        }
        tmp[count++] = t.length();
        int[] r = new int[count];
        System.arraycopy(tmp, 0, r, 0, count);
        return r;
    }
}
