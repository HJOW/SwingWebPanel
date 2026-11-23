package org.duckdns.hjow.swingwebpanel.layout;

import java.util.Set;

/**
 * The strictness of the line breaking rules of Chinese and Japanese text (<code>line-break: loose | normal | strict</code>, CSS Text Module Level 3 section
 * 5.3, https://www.w3.org/TR/css-text-3/#line-break-property). The break iterator of the JRE already forbids the breaks that every level forbids (before
 * closing brackets, closing punctuation, small kana and the prolonged sound mark; after opening brackets), which is what <code>strict</code> asks for;
 * this class adjusts the set of break opportunities of a text for the other levels, with the rules that the specification requires:
 * <ul>
 * <li><code>normal</code> and <code>auto</code>: breaks before the wave dash U+301C and the katakana-hiragana double hyphen U+30A0 are allowed;</li>
 * <li><code>strict</code>: those are forbidden too;</li>
 * <li><code>loose</code> also allows breaks before small kana and the prolonged sound mark (the class CJ of UAX #14), before iteration marks, between
 * inseparable characters (the leaders U+2025 and U+2026), before the hyphens U+2010 and U+2013 that follow an ideograph, before centered punctuation (the
 * middle dot and the full width colon, semicolon, exclamation and question marks), before suffixes such as the full width percent sign and the degree
 * sign and after prefixes such as the full width yen and dollar signs.</li>
 * </ul>
 * A break is added only after a character of the class ID (ideographs, kana, full width forms), never after an opening bracket.
 * <p>
 * 중국어와 일본어 텍스트의 줄바꿈 규칙 엄격도(<code>line-break: loose | normal | strict</code>, CSS Text Module Level 3 5.3 절). JRE 의 줄바꿈 반복자는 모든 단계가
 * 금지하는 줄바꿈(닫는 괄호, 닫는 문장 부호, 작은 가나, 장음 부호 앞, 여는 괄호 뒤)을 이미 금지하며 이것이 <code>strict</code> 가 요구하는 것이다. 이 클래스는 규격이
 * 요구하는 규칙으로 다른 단계를 위해 텍스트의 줄바꿈 기회를 조정한다.
 * <ul>
 * <li><code>normal</code>, <code>auto</code>: 물결 대시 U+301C 와 가타카나-히라가나 이중 하이픈 U+30A0 앞에서 줄바꿈을 허용한다.</li>
 * <li><code>strict</code>: 그것도 금지한다.</li>
 * <li><code>loose</code>: 작은 가나와 장음 부호(UAX #14 의 CJ 클래스) 앞, 반복 부호 앞, 나눌 수 없는 문자(U+2025, U+2026 리더) 사이, 한자 뒤의 하이픈 U+2010 과 U+2013 앞,
 * 가운뎃점 문장 부호(가운뎃점, 전각 콜론, 세미콜론, 느낌표, 물음표) 앞, 전각 퍼센트와 도 기호 같은 접미사 앞, 전각 엔과 달러 기호 같은 접두사 뒤에서도 허용한다.</li>
 * </ul>
 * 줄바꿈은 ID 클래스 문자(한자, 가나, 전각 형태) 뒤에만 더하며 여는 괄호 뒤에는 더하지 않는다.
 */
final class CjkLineBreak {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CjkLineBreak() {
    }

    /**
     * Adjusts the break opportunities of a text for the strictness of <code>line-break</code>.
     * <p>
     * 텍스트의 줄바꿈 기회를 <code>line-break</code> 의 엄격도에 맞게 조정한다.
     *
     * @param text   the text / 텍스트
     * @param breaks the break offsets (sorted set, an offset is the index of the character that starts the next line) that is changed / 바꿀 줄바꿈 위치(정렬 집합. 위치는 다음 줄을 시작하는 글자의 인덱스)
     * @param level  <code>auto</code>, <code>normal</code>, <code>strict</code> or <code>loose</code> / <code>auto</code>, <code>normal</code>, <code>strict</code>, <code>loose</code> 중 하나
     */
    static void adjust(String text, Set<Integer> breaks, String level) {
        boolean loose = "loose".equals(level);
        boolean strict = "strict".equals(level);
        int n = text.length();
        // 이웃한 두 글자 사이마다 단계에 따라 줄바꿈을 더하거나 뺀다.
        for (int i = 1; i < n; i++) {
            int cp = text.codePointAt(i);
            if (Character.isLowSurrogate(text.charAt(i))) {
                continue;
            }
            int prev = text.codePointBefore(i);
            boolean afterId = isId(prev) && !isOpening(prev);
            Integer pos = Integer.valueOf(i);
            if (cp == 0x301C || cp == 0x30A0) {
                // 물결 대시와 이중 하이픈: normal 과 loose 는 허용하고 strict 는 금지한다.
                if (strict) {
                    breaks.remove(pos);
                } else if (afterId) {
                    breaks.add(pos);
                }
                continue;
            }
            if (!loose || !afterId) {
                continue;
            }
            if (isSmallKanaOrProlonged(cp) || isIterationMark(cp) || isCentered(cp) || isSuffix(cp) || cp == 0x2010 || cp == 0x2013) {
                breaks.add(pos);
            } else if (isInseparable(cp) && isInseparable(prev)) {
                breaks.add(pos);
            }
        }
        boolean cjkText = false;
        // 접두사 규칙은 중국어나 일본어 텍스트(한자나 가나가 있는 텍스트)에만 적용한다.
        for (int i = 0; i < n && !cjkText; i++) {
            cjkText = isId(text.codePointAt(i));
        }
        // loose 는 접두사(전각 엔 기호 등) 뒤의 줄바꿈도 허용한다(다음 글자가 공백이 아닐 때).
        if (loose && cjkText) {
            for (int i = 1; i < n; i++) {
                int prev = text.codePointBefore(i);
                if (isPrefix(prev) && !Character.isWhitespace(text.codePointAt(i)) && !Character.isLowSurrogate(text.charAt(i))) {
                    breaks.add(Integer.valueOf(i));
                }
            }
        }
    }

    /**
     * Tells whether a code point is of the line breaking class ID for the purpose of these rules: an ideograph, a kana, a CJK symbol or a full width form.
     * <p>
     * 코드 포인트가 이 규칙에서 줄바꿈 클래스 ID 인지 알려 준다: 한자, 가나, CJK 기호, 전각 형태.
     *
     * @param cp the code point / 코드 포인트
     * @return true for ID / ID 이면 true
     */
    static boolean isId(int cp) {
        return (cp >= 0x2E80 && cp <= 0x303F) || (cp >= 0x3040 && cp <= 0x30FF) || (cp >= 0x3100 && cp <= 0x312F) || (cp >= 0x31F0 && cp <= 0x31FF)
                || (cp >= 0x3400 && cp <= 0x4DBF) || (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFF00 && cp <= 0xFFEF)
                || (cp >= 0x20000 && cp <= 0x2FFFF);
    }

    /**
     * Tells whether a code point is an opening bracket or quotation mark (no break may follow it).
     * <p>
     * 코드 포인트가 여는 괄호나 따옴표인지 알려 준다(그 뒤에서는 줄을 바꿀 수 없다).
     *
     * @param cp the code point / 코드 포인트
     * @return true for an opening bracket / 여는 괄호이면 true
     */
    static boolean isOpening(int cp) {
        return cp == 0x3008 || cp == 0x300A || cp == 0x300C || cp == 0x300E || cp == 0x3010 || cp == 0x3014 || cp == 0x3016 || cp == 0x3018 || cp == 0x301A
                || cp == 0x301D || cp == 0xFF08 || cp == 0xFF3B || cp == 0xFF5B || cp == 0xFF5F || cp == 0xFF62 || cp == 0x2018 || cp == 0x201C;
    }

    /**
     * Tells whether a code point is a small kana or the prolonged sound mark (the class CJ).
     * <p>
     * 코드 포인트가 작은 가나나 장음 부호(CJ 클래스)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for CJ / CJ 이면 true
     */
    static boolean isSmallKanaOrProlonged(int cp) {
        switch (cp) {
            case 0x3041: case 0x3043: case 0x3045: case 0x3047: case 0x3049: case 0x3063: case 0x3083: case 0x3085: case 0x3087: case 0x308E:
            case 0x3095: case 0x3096: case 0x30A1: case 0x30A3: case 0x30A5: case 0x30A7: case 0x30A9: case 0x30C3: case 0x30E3: case 0x30E5:
            case 0x30E7: case 0x30EE: case 0x30F5: case 0x30F6: case 0x30FC: case 0xFF67: case 0xFF68: case 0xFF69: case 0xFF6A: case 0xFF6B:
            case 0xFF6C: case 0xFF6D: case 0xFF6E: case 0xFF6F: case 0xFF70:
                return true;
            default:
                return cp >= 0x31F0 && cp <= 0x31FF;
        }
    }

    /**
     * Tells whether a code point is an iteration mark.
     * <p>
     * 코드 포인트가 반복 부호인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for an iteration mark / 반복 부호이면 true
     */
    static boolean isIterationMark(int cp) {
        return cp == 0x3005 || cp == 0x303B || cp == 0x309D || cp == 0x309E || cp == 0x30FD || cp == 0x30FE;
    }

    /**
     * Tells whether a code point is one of the inseparable characters (the leaders).
     * <p>
     * 코드 포인트가 나눌 수 없는 문자(리더) 중 하나인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for IN / IN 이면 true
     */
    static boolean isInseparable(int cp) {
        return cp == 0x2024 || cp == 0x2025 || cp == 0x2026 || cp == 0xFE19;
    }

    /**
     * Tells whether a code point is one of the centered punctuation marks before which <code>loose</code> allows a break.
     * <p>
     * 코드 포인트가 <code>loose</code> 가 그 앞의 줄바꿈을 허용하는 가운뎃점 문장 부호 중 하나인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for such a mark / 그런 부호이면 true
     */
    static boolean isCentered(int cp) {
        return cp == 0x30FB || cp == 0xFF1A || cp == 0xFF1B || cp == 0xFF65 || cp == 0x203C || cp == 0x2047 || cp == 0x2048 || cp == 0x2049 || cp == 0xFF01
                || cp == 0xFF1F;
    }

    /**
     * Tells whether a code point is a wide postfix (the class PO with a wide, full width or ambiguous East Asian width).
     * <p>
     * 코드 포인트가 넓은 접미사(동아시아 너비가 wide, full width, ambiguous 인 PO 클래스)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for such a postfix / 그런 접미사이면 true
     */
    static boolean isSuffix(int cp) {
        return cp == 0xB0 || cp == 0xA2 || cp == 0x2030 || cp == 0x2032 || cp == 0x2033 || cp == 0x2103 || cp == 0xFF05 || cp == 0xFFE0;
    }

    /**
     * Tells whether a code point is a wide prefix (the class PR with a wide, full width or ambiguous East Asian width).
     * <p>
     * 코드 포인트가 넓은 접두사(동아시아 너비가 wide, full width, ambiguous 인 PR 클래스)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for such a prefix / 그런 접두사이면 true
     */
    static boolean isPrefix(int cp) {
        return cp == 0xA3 || cp == 0xA5 || cp == 0xB1 || cp == 0x2116 || cp == 0xFF04 || cp == 0xFF0B || cp == 0xFFE1 || cp == 0xFFE5 || cp == 0xFFE6;
    }
}
