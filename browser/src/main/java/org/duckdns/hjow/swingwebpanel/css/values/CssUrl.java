package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A <code>url()</code> value. The URL text is kept as written; the style sheet builder resolves it against the base URL of the style sheet
 * (the URL of the sheet, not of the document) into an absolute URL when one is known, so that background images and fonts load from the right place.
 * <p>
 * <code>url()</code> 값. URL 문자열은 쓰인 그대로 보관하며, 스타일 시트 구성기는 기준 URL(문서가 아니라 스타일 시트의 URL)이 알려져 있으면 그에 대해 절대 URL 로
 * 해석해 배경 이미지와 글꼴이 올바른 곳에서 읽히게 한다.
 */
public final class CssUrl extends CssValue {

    /** The URL as written. / 쓰인 그대로의 URL. */
    private final String url;

    /** The absolute URL, or null if it could not be resolved. / 절대 URL. 해석하지 못했으면 null. */
    private final String resolved;

    /**
     * Creates a url value.
     * <p>
     * url 값을 만든다.
     *
     * @param url      the URL as written / 쓰인 그대로의 URL
     * @param resolved the absolute URL or null / 절대 URL 또는 null
     */
    public CssUrl(String url, String resolved) {
        this.url = url;
        this.resolved = resolved;
    }

    /**
     * Returns the URL as written.
     * <p>
     * 쓰인 그대로의 URL 을 반환한다.
     *
     * @return URL text / URL 문자열
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the absolute URL, or the written URL if no base URL was known.
     * <p>
     * 절대 URL 을 반환한다. 기준 URL 을 몰랐으면 쓰인 그대로의 URL.
     *
     * @return URL text / URL 문자열
     */
    public String getResolvedUrl() {
        return resolved != null ? resolved : url;
    }

    /**
     * Returns a copy with another resolved URL.
     * <p>
     * 해석된 URL 만 다른 복사본을 반환한다.
     *
     * @param absolute absolute URL / 절대 URL
     * @return the copy / 복사본
     */
    public CssUrl withResolved(String absolute) {
        return new CssUrl(url, absolute);
    }

    /**
     * Serializes as <code>url("...")</code> using the URL as written.
     * <p>
     * 쓰인 그대로의 URL 로 <code>url("...")</code> 형태로 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return "url(" + CssEscape.string(url) + ")";
    }

    /**
     * Compares the written URL and the resolved URL.
     * <p>
     * 쓰인 URL 과 해석된 URL 을 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssUrl && ((CssUrl) o).url.equals(url) && getResolvedUrl().equals(((CssUrl) o).getResolvedUrl());
    }

    /**
     * Hash of the URL.
     * <p>
     * URL 의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return url.hashCode() + 5;
    }
}
