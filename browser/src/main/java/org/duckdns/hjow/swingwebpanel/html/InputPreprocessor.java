package org.duckdns.hjow.swingwebpanel.html;

/**
 * The preprocessing that the HTML Standard applies to the input stream before tokenization ("preprocessing the input stream"):
 * every CR LF pair and every lone CR becomes a single LF. This is kept apart from decoding on purpose: {@link DecodedText#getText()} keeps
 * the received text (it is what <code>getContent()</code> returns) while the parser works on the normalized copy.
 * <p>
 * It can also scan the text for the input stream parse errors of the standard (control characters, noncharacters, lone surrogates).
 * <p>
 * 토큰화 전에 HTML 표준이 입력 스트림에 적용하는 전처리("preprocessing the input stream"): 모든 CR LF 쌍과 홀로 있는 CR 은 LF 하나가 된다.
 * 이것은 의도적으로 디코딩과 분리했다. {@link DecodedText#getText()} 는 받은 텍스트(<code>getContent()</code> 가 반환하는 값)를 그대로 두고
 * 파서는 정규화된 복사본으로 일한다.
 * <p>
 * 표준의 입력 스트림 파싱 오류(제어 문자, 비문자, 짝 없는 서로게이트)를 텍스트에서 찾아 보고할 수도 있다.
 */
public final class InputPreprocessor {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private InputPreprocessor() {
    }

    /**
     * Normalizes new lines: CR LF and lone CR become LF. The same string object is returned if it contains no CR.
     * <p>
     * 줄바꿈을 정규화한다: CR LF 와 홀로 있는 CR 은 LF 가 된다. CR 이 없으면 같은 문자열 객체를 반환한다.
     *
     * @param text received text / 받은 텍스트
     * @return normalized text / 정규화된 텍스트
     */
    public static String normalize(String text) {
        if (text.indexOf('\r') < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int n = text.length();
        // 문자를 훑으며 CR LF 는 LF 하나로, 홀로 있는 CR 은 LF 로 바꾼다.
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                sb.append('\n');
                if (i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Reports the input stream parse errors in already normalized text: <code>control-character-in-input-stream</code>,
     * <code>noncharacter-in-input-stream</code> and <code>surrogate-in-input-stream</code>.
     * <p>
     * 이미 정규화된 텍스트에서 입력 스트림 파싱 오류를 보고한다: <code>control-character-in-input-stream</code>,
     * <code>noncharacter-in-input-stream</code>, <code>surrogate-in-input-stream</code>.
     *
     * @param text normalized text / 정규화된 텍스트
     * @param log  error collector / 오류 수집기
     */
    public static void scanErrors(String text, ParseErrorLog log) {
        int n = text.length();
        // 모든 문자를 훑어 오류 후보만 걸러 낸다.
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c >= 0x20 && c < 0x7F) {
                continue;
            }
            if (c == '\n' || c == '\t' || c == '\f') {
                continue;
            }
            if (c >= 0xD800 && c <= 0xDBFF && i + 1 < n && text.charAt(i + 1) >= 0xDC00 && text.charAt(i + 1) <= 0xDFFF) {
                // 올바른 서로게이트 쌍이면 합친 코드 포인트가 비문자(예: U+1FFFE)인지 본다.
                int cp = Character.toCodePoint(c, text.charAt(i + 1));
                if ((cp & 0xFFFE) == 0xFFFE) {
                    log.add("noncharacter-in-input-stream", i);
                }
                i++;
                continue;
            }
            if (c >= 0xD800 && c <= 0xDFFF) {
                log.add("surrogate-in-input-stream", i);
            } else if (c == 0 || (c >= 0x01 && c <= 0x1F) || (c >= 0x7F && c <= 0x9F)) {
                // NUL 은 토큰화기가 따로 보고하므로 여기서는 보고하지 않는다.
                if (c != 0) {
                    log.add("control-character-in-input-stream", i);
                }
            } else if ((c >= 0xFDD0 && c <= 0xFDEF) || c == 0xFFFE || c == 0xFFFF) {
                log.add("noncharacter-in-input-stream", i);
            }
        }
    }
}
