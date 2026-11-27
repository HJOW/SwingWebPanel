package org.duckdns.hjow.swingwebpanel.core;

/**
 * The default fonts of one component (API contract 5.27): the family for content whose author named no font, the families behind the
 * generic names <code>serif</code>, <code>sans-serif</code> and <code>monospace</code>, the size of <code>font-size: medium</code>, and
 * the size of the monospace elements. An instance is immutable; the component replaces it as a whole when the host changes a value, and
 * every page of the component (frames included) reads the same instance. Nothing here is static, so two components never share their
 * defaults.
 * <p>
 * 컴포넌트 하나의 기본 글꼴(API 계약 5.27): 작성자가 글꼴을 지정하지 않은 내용에 쓰는 패밀리, 일반 이름 <code>serif</code>, <code>sans-serif</code>,
 * <code>monospace</code> 뒤의 패밀리, <code>font-size: medium</code> 의 크기, 고정폭 요소의 크기. 인스턴스는 바뀌지 않는다. 호스트가 값을 바꾸면
 * 컴포넌트가 통째로 교체하고, 컴포넌트의 모든 페이지(프레임 포함)가 같은 인스턴스를 읽는다. 정적인 것이 없으므로 두 컴포넌트가 기본값을 공유하는
 * 일은 없다.
 */
public final class FontDefaults {

    /** The size of <code>medium</code> when the host set none, in CSS pixels. / 호스트가 정하지 않았을 때 <code>medium</code> 의 크기(CSS 픽셀). */
    public static final int BUILT_IN_SIZE = 16;

    /** The smallest size that the host can set. / 호스트가 정할 수 있는 가장 작은 크기. */
    public static final int MIN_SIZE = 6;

    /** The largest size that the host can set. / 호스트가 정할 수 있는 가장 큰 크기. */
    public static final int MAX_SIZE = 72;

    /** The defaults of a component whose host set nothing. / 호스트가 아무것도 정하지 않은 컴포넌트의 기본값. */
    public static final FontDefaults NONE = new FontDefaults("", "", "", "", BUILT_IN_SIZE, 0);

    /** The family for content without an author font, or the empty string. / 작성자 글꼴이 없는 내용에 쓰는 패밀리. 없으면 빈 문자열. */
    private final String standard;

    /** The family behind <code>serif</code>, or the empty string. / <code>serif</code> 뒤의 패밀리. 없으면 빈 문자열. */
    private final String serif;

    /** The family behind <code>sans-serif</code>, or the empty string. / <code>sans-serif</code> 뒤의 패밀리. 없으면 빈 문자열. */
    private final String sansSerif;

    /** The family behind <code>monospace</code>, or the empty string. / <code>monospace</code> 뒤의 패밀리. 없으면 빈 문자열. */
    private final String monospace;

    /** The size of <code>medium</code> in CSS pixels. / <code>medium</code> 의 크기(CSS 픽셀). */
    private final int size;

    /** The size of the monospace elements in CSS pixels, or 0 for the size of the surrounding text. / 고정폭 요소의 크기(CSS 픽셀). 주변 글의 크기를 쓰면 0. */
    private final int monospaceSize;

    /**
     * Creates the defaults.
     * <p>
     * 기본값을 만든다.
     *
     * @param standard      the family for content without an author font, or the empty string / 작성자 글꼴이 없는 내용의 패밀리 또는 빈 문자열
     * @param serif         the family behind <code>serif</code>, or the empty string / <code>serif</code> 뒤의 패밀리 또는 빈 문자열
     * @param sansSerif     the family behind <code>sans-serif</code>, or the empty string / <code>sans-serif</code> 뒤의 패밀리 또는 빈 문자열
     * @param monospace     the family behind <code>monospace</code>, or the empty string / <code>monospace</code> 뒤의 패밀리 또는 빈 문자열
     * @param size          the size of <code>medium</code> / <code>medium</code> 의 크기
     * @param monospaceSize the size of the monospace elements, or 0 / 고정폭 요소의 크기 또는 0
     */
    public FontDefaults(String standard, String serif, String sansSerif, String monospace, int size, int monospaceSize) {
        this.standard = standard == null ? "" : standard;
        this.serif = serif == null ? "" : serif;
        this.sansSerif = sansSerif == null ? "" : sansSerif;
        this.monospace = monospace == null ? "" : monospace;
        this.size = size;
        this.monospaceSize = monospaceSize;
    }

    /**
     * Returns the family for content without an author font.
     * <p>
     * 작성자 글꼴이 없는 내용에 쓰는 패밀리를 반환한다.
     *
     * @return the family, or the empty string / 패밀리. 없으면 빈 문자열
     */
    public String getStandard() {
        return standard;
    }

    /**
     * Returns the family behind <code>serif</code>.
     * <p>
     * <code>serif</code> 뒤의 패밀리를 반환한다.
     *
     * @return the family, or the empty string / 패밀리. 없으면 빈 문자열
     */
    public String getSerif() {
        return serif;
    }

    /**
     * Returns the family behind <code>sans-serif</code>.
     * <p>
     * <code>sans-serif</code> 뒤의 패밀리를 반환한다.
     *
     * @return the family, or the empty string / 패밀리. 없으면 빈 문자열
     */
    public String getSansSerif() {
        return sansSerif;
    }

    /**
     * Returns the family behind <code>monospace</code>.
     * <p>
     * <code>monospace</code> 뒤의 패밀리를 반환한다.
     *
     * @return the family, or the empty string / 패밀리. 없으면 빈 문자열
     */
    public String getMonospace() {
        return monospace;
    }

    /**
     * Returns the size of <code>medium</code>.
     * <p>
     * <code>medium</code> 의 크기를 반환한다.
     *
     * @return the size in CSS pixels / 크기(CSS 픽셀)
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the size of the monospace elements.
     * <p>
     * 고정폭 요소의 크기를 반환한다.
     *
     * @return the size in CSS pixels, or 0 for the size of the surrounding text / 크기(CSS 픽셀). 주변 글의 크기를 쓰면 0
     */
    public int getMonospaceSize() {
        return monospaceSize;
    }

    /**
     * Returns a copy with another family for one of the kinds <code>standard</code>, <code>serif</code>, <code>sans-serif</code> and
     * <code>monospace</code>.
     * <p>
     * <code>standard</code>, <code>serif</code>, <code>sans-serif</code>, <code>monospace</code> 가운데 한 종류의 패밀리만 다른 복사본을 반환한다.
     *
     * @param kind   the kind / 종류
     * @param family the family, or the empty string for none / 패밀리. 없으면 빈 문자열
     * @return the copy, or null for an unknown kind / 복사본. 모르는 종류이면 null
     */
    public FontDefaults withFamily(String kind, String family) {
        if ("standard".equals(kind)) {
            return new FontDefaults(family, serif, sansSerif, monospace, size, monospaceSize);
        }
        if ("serif".equals(kind)) {
            return new FontDefaults(standard, family, sansSerif, monospace, size, monospaceSize);
        }
        if ("sans-serif".equals(kind)) {
            return new FontDefaults(standard, serif, family, monospace, size, monospaceSize);
        }
        if ("monospace".equals(kind)) {
            return new FontDefaults(standard, serif, sansSerif, family, size, monospaceSize);
        }
        return null;
    }

    /**
     * Returns the family of one of the kinds.
     * <p>
     * 한 종류의 패밀리를 반환한다.
     *
     * @param kind <code>standard</code>, <code>serif</code>, <code>sans-serif</code> or <code>monospace</code> /
     *             <code>standard</code>, <code>serif</code>, <code>sans-serif</code>, <code>monospace</code> 중 하나
     * @return the family (the empty string for none), or null for an unknown kind / 패밀리(없으면 빈 문자열). 모르는 종류이면 null
     */
    public String getFamily(String kind) {
        if ("standard".equals(kind)) {
            return standard;
        }
        if ("serif".equals(kind)) {
            return serif;
        }
        if ("sans-serif".equals(kind)) {
            return sansSerif;
        }
        return "monospace".equals(kind) ? monospace : null;
    }

    /**
     * Returns a copy with other sizes.
     * <p>
     * 크기만 다른 복사본을 반환한다.
     *
     * @param newSize          the size of <code>medium</code> / <code>medium</code> 의 크기
     * @param newMonospaceSize the size of the monospace elements, or 0 / 고정폭 요소의 크기 또는 0
     * @return the copy / 복사본
     */
    public FontDefaults withSizes(int newSize, int newMonospaceSize) {
        return new FontDefaults(standard, serif, sansSerif, monospace, newSize, newMonospaceSize);
    }

    /**
     * Returns the user style sheet that carries the defaults which are expressed as rules: the standard family as the font of the root
     * element, and the size of the monospace elements relative to the default size (so that it scales inside headings, and does not
     * shrink a second time when monospace elements are nested). Author rules win over these rules.
     * <p>
     * 규칙으로 표현하는 기본값을 담은 사용자 스타일 시트를 반환한다: 표준 패밀리는 루트 요소의 글꼴로, 고정폭 요소의 크기는 기본 크기에 대한
     * 비율로 적는다(그래서 제목 안에서는 함께 커지고, 고정폭 요소가 중첩되어도 한 번 더 작아지지 않는다). 작성자 규칙이 이 규칙을 이긴다.
     *
     * @return the CSS text, empty if no rule is needed / CSS 텍스트. 규칙이 필요 없으면 비어 있다
     */
    public String userStyleSheet() {
        StringBuilder css = new StringBuilder();
        if (standard.length() > 0) {
            css.append(":root { font-family: ").append(quote(standard)).append(", serif }\n");
        }
        if (monospaceSize > 0 && monospaceSize != size) {
            String list = "code, kbd, samp, tt, pre, listing, xmp, plaintext";
            String nested = ":is(" + list + ") :is(" + list + ")";
            css.append(list).append(" { font-size: ").append(ratio()).append("em }\n");
            css.append(nested).append(" { font-size: 1em }\n");
        }
        return css.toString();
    }

    /**
     * Returns the size of the monospace elements as a fraction of the default size.
     * <p>
     * 고정폭 요소의 크기를 기본 크기에 대한 비율로 반환한다.
     *
     * @return the fraction with four digits / 소수 넷째 자리까지의 비율
     */
    private String ratio() {
        long scaled = Math.round(monospaceSize * 10000.0 / size);
        String digits = String.valueOf(scaled % 10000);
        // 소수부를 네 자리로 맞춘다.
        while (digits.length() < 4) {
            digits = "0" + digits;
        }
        return (scaled / 10000) + "." + digits;
    }

    /**
     * Writes a family name as a CSS string.
     * <p>
     * 패밀리 이름을 CSS 문자열로 적는다.
     *
     * @param family the family name / 패밀리 이름
     * @return the quoted name / 따옴표로 묶은 이름
     */
    private static String quote(String family) {
        StringBuilder out = new StringBuilder("\"");
        // 따옴표와 역슬래시를 이스케이프하고 제어 문자는 뺀다.
        for (int i = 0; i < family.length(); i++) {
            char c = family.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c >= ' ') {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    /**
     * Describes the defaults in one line, for property change events and for logs.
     * <p>
     * 기본값을 한 줄로 적는다. 속성 변경 이벤트와 로그에 쓴다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return "standard=" + standard + "; serif=" + serif + "; sans-serif=" + sansSerif + "; monospace=" + monospace + "; size=" + size
                + "; monospace-size=" + monospaceSize;
    }

    /**
     * Compares the values.
     * <p>
     * 값을 비교한다.
     *
     * @param o another object / 다른 객체
     * @return true if it holds the same defaults / 같은 기본값이면 true
     */
    public boolean equals(Object o) {
        if (!(o instanceof FontDefaults)) {
            return false;
        }
        FontDefaults d = (FontDefaults) o;
        return standard.equals(d.standard) && serif.equals(d.serif) && sansSerif.equals(d.sansSerif) && monospace.equals(d.monospace)
                && size == d.size && monospaceSize == d.monospaceSize;
    }

    /**
     * Returns a hash of the values.
     * <p>
     * 값의 해시를 반환한다.
     *
     * @return the hash / 해시
     */
    public int hashCode() {
        return standard.hashCode() * 31 + serif.hashCode() * 17 + sansSerif.hashCode() * 13 + monospace.hashCode() * 7 + size * 3 + monospaceSize;
    }
}
