package org.duckdns.hjow.swingwebpanel.core;

/**
 * Text helpers for the small generated pages of the core package. Anything that comes from outside (URLs, messages) is escaped
 * before it is placed into generated HTML, so an address can never become markup.
 * <p>
 * core 패키지가 만드는 작은 생성 페이지용 문자열 도우미. 바깥에서 온 값(URL, 메시지)은 생성 HTML 에 넣기 전에 이스케이프하므로
 * 주소가 마크업이 될 수 없다.
 */
final class HtmlText {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HtmlText() {
    }

    /**
     * Escapes the characters that are special in HTML text and attribute values.
     * <p>
     * HTML 본문과 속성값에서 특별한 문자를 이스케이프한다.
     *
     * @param s text, null is treated as empty / 문자열(null 은 빈 문자열로 취급)
     * @return escaped text / 이스케이프한 문자열
     */
    static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        // 글자마다 HTML 특수 문자를 문자 참조로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&#39;");
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }
}
