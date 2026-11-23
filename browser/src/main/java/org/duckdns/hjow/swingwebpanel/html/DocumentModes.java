package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;

/**
 * Determination of the document mode (no-quirks, limited-quirks, quirks) from a DOCTYPE token, as the "initial" insertion mode of the
 * HTML Standard prescribes: the force-quirks flag, a name other than <code>html</code>, a list of public identifiers (the old HTML 2.0,
 * 3.2 and 4.0 era DTDs and some vendor DTDs) and the rules for the HTML 4.01 and XHTML 1.0 transitional and frameset DTDs, which depend on
 * the presence of a system identifier. All identifier comparisons ignore ASCII case. The mode decides, among other things, the layout
 * rules of old documents (P07-07).
 * <p>
 * DOCTYPE 토큰에서 문서 모드(no-quirks, limited-quirks, quirks)를 정한다. HTML 표준의 "initial" 삽입 모드가 정한 대로이다: force-quirks 플래그,
 * <code>html</code> 이 아닌 이름, 공개 식별자 목록(HTML 2.0, 3.2, 4.0 시대의 옛 DTD 와 일부 공급자 DTD), 시스템 식별자 유무에 따라 달라지는 HTML 4.01 과
 * XHTML 1.0 의 transitional/frameset DTD 규칙. 모든 식별자 비교는 ASCII 대소문자를 무시한다. 문서 모드는 옛 문서의 레이아웃 규칙 등을 결정한다(P07-07).
 */
public final class DocumentModes {

    /**
     * Public identifiers that put the document in quirks mode if the document's identifier equals one of them.
     * <p>
     * 문서의 식별자가 이 중 하나와 같으면 문서를 quirks 모드로 만드는 공개 식별자.
     */
    private static final String[] QUIRKS_PUBLIC_EXACT = {"-//w3o//dtd w3 html strict 3.0//en//", "-/w3c/dtd html 4.0 transitional/en", "html"};

    /**
     * Prefixes of public identifiers that put the document in quirks mode.
     * <p>
     * 문서를 quirks 모드로 만드는 공개 식별자의 접두사.
     */
    private static final String[] QUIRKS_PUBLIC_PREFIX = {"+//silmaril//dtd html pro v0r11 19970101//", "-//as//dtd html 3.0 aswedit + extensions//",
            "-//advasoft ltd//dtd html 3.0 aswedit + extensions//", "-//ietf//dtd html 2.0 level 1//", "-//ietf//dtd html 2.0 level 2//",
            "-//ietf//dtd html 2.0 strict level 1//", "-//ietf//dtd html 2.0 strict level 2//", "-//ietf//dtd html 2.0 strict//",
            "-//ietf//dtd html 2.0//", "-//ietf//dtd html 2.1e//", "-//ietf//dtd html 3.0//", "-//ietf//dtd html 3.2 final//",
            "-//ietf//dtd html 3.2//", "-//ietf//dtd html 3//", "-//ietf//dtd html level 0//", "-//ietf//dtd html level 1//",
            "-//ietf//dtd html level 2//", "-//ietf//dtd html level 3//", "-//ietf//dtd html strict level 0//",
            "-//ietf//dtd html strict level 1//", "-//ietf//dtd html strict level 2//", "-//ietf//dtd html strict level 3//",
            "-//ietf//dtd html strict//", "-//ietf//dtd html//", "-//metrius//dtd metrius presentational//",
            "-//microsoft//dtd internet explorer 2.0 html strict//", "-//microsoft//dtd internet explorer 2.0 html//",
            "-//microsoft//dtd internet explorer 2.0 tables//", "-//microsoft//dtd internet explorer 3.0 html strict//",
            "-//microsoft//dtd internet explorer 3.0 html//", "-//microsoft//dtd internet explorer 3.0 tables//",
            "-//netscape comm. corp.//dtd html//", "-//netscape comm. corp.//dtd strict html//", "-//o'reilly and associates//dtd html 2.0//",
            "-//o'reilly and associates//dtd html extended 1.0//", "-//o'reilly and associates//dtd html extended relaxed 1.0//",
            "-//sq//dtd html 2.0 hotmetal + extensions//", "-//softquad software//dtd hotmetal pro 6.0::19990601::extensions to html 4.0//",
            "-//softquad//dtd hotmetal pro 4.0::19971010::extensions to html 4.0//", "-//spyglass//dtd html 2.0 extended//",
            "-//sun microsystems corp.//dtd hotjava html//", "-//sun microsystems corp.//dtd hotjava strict html//",
            "-//w3c//dtd html 3 1995-03-24//", "-//w3c//dtd html 3.2 draft//", "-//w3c//dtd html 3.2 final//", "-//w3c//dtd html 3.2//",
            "-//w3c//dtd html 3.2s draft//", "-//w3c//dtd html 4.0 frameset//", "-//w3c//dtd html 4.0 transitional//",
            "-//w3c//dtd html experimental 19960712//", "-//w3c//dtd html experimental 970421//", "-//w3c//dtd w3 html//",
            "-//w3o//dtd w3 html 3.0//", "-//webtechs//dtd mozilla html 2.0//", "-//webtechs//dtd mozilla html//"};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DocumentModes() {
    }

    /**
     * Returns the document mode for a DOCTYPE token.
     * <p>
     * DOCTYPE 토큰에 대한 문서 모드를 반환한다.
     *
     * @param name       DOCTYPE name (lower case) or null if missing / DOCTYPE 이름(소문자). 없으면 null
     * @param publicId   public identifier or null if missing / 공개 식별자. 없으면 null
     * @param systemId   system identifier or null if missing / 시스템 식별자. 없으면 null
     * @param forceQuirks the force-quirks flag of the token / 토큰의 force-quirks 플래그
     * @return {@link Document#QUIRKS_MODE}, {@link Document#LIMITED_QUIRKS_MODE} or {@link Document#NO_QUIRKS_MODE} / 문서 모드
     */
    public static int fromDoctype(String name, String publicId, String systemId, boolean forceQuirks) {
        if (forceQuirks || !"html".equals(name)) {
            return Document.QUIRKS_MODE;
        }
        String pub = publicId == null ? null : lower(publicId);
        String sys = systemId == null ? null : lower(systemId);
        if (pub != null) {
            // 공개 식별자가 정확히 일치하는 quirks 목록을 확인한다.
            for (int i = 0; i < QUIRKS_PUBLIC_EXACT.length; i++) {
                if (QUIRKS_PUBLIC_EXACT[i].equals(pub)) {
                    return Document.QUIRKS_MODE;
                }
            }
        }
        if ("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd".equals(sys)) {
            return Document.QUIRKS_MODE;
        }
        if (pub != null) {
            // 접두사가 일치하는 quirks 목록을 확인한다.
            for (int i = 0; i < QUIRKS_PUBLIC_PREFIX.length; i++) {
                if (pub.startsWith(QUIRKS_PUBLIC_PREFIX[i])) {
                    return Document.QUIRKS_MODE;
                }
            }
            boolean sysMissingOrEmpty = sys == null || sys.length() == 0;
            if (sysMissingOrEmpty && (pub.startsWith("-//w3c//dtd html 4.01 frameset//") || pub.startsWith("-//w3c//dtd html 4.01 transitional//"))) {
                return Document.QUIRKS_MODE;
            }
            if (pub.startsWith("-//w3c//dtd xhtml 1.0 frameset//") || pub.startsWith("-//w3c//dtd xhtml 1.0 transitional//")) {
                return Document.LIMITED_QUIRKS_MODE;
            }
            if (!sysMissingOrEmpty && (pub.startsWith("-//w3c//dtd html 4.01 frameset//") || pub.startsWith("-//w3c//dtd html 4.01 transitional//"))) {
                return Document.LIMITED_QUIRKS_MODE;
            }
        }
        return Document.NO_QUIRKS_MODE;
    }

    /**
     * Lower-cases a string for ASCII case-insensitive comparison, leaving other characters alone.
     * <p>
     * ASCII 대소문자를 무시하는 비교를 위해 문자열을 소문자로 바꾼다. 다른 문자는 그대로 둔다.
     *
     * @param s text / 문자열
     * @return lower-cased text / 소문자로 바꾼 문자열
     */
    static String lower(String s) {
        StringBuilder sb = null;
        // 대문자 ASCII 가 있을 때만 복사본을 만들어 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + 32));
            }
        }
        return sb == null ? s : sb.toString();
    }
}
