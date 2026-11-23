package org.duckdns.hjow.swingwebpanel.css.cssom;

/**
 * The result of loading a style sheet: its decoded text and the URL it was finally loaded from (the base for the relative urls inside it).
 * <p>
 * 스타일 시트를 읽은 결과: 디코딩한 텍스트와 최종적으로 읽은 URL(그 안의 상대 url 의 기준).
 */
public final class LoadedStyleSheet {

    /** The decoded text. / 디코딩한 텍스트. */
    private final String text;

    /** The final URL. / 최종 URL. */
    private final String url;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param text the text / 텍스트
     * @param url  the final URL / 최종 URL
     */
    public LoadedStyleSheet(String text, String url) {
        this.text = text;
        this.url = url;
    }

    /**
     * Returns the text.
     * <p>
     * 텍스트를 반환한다.
     *
     * @return the text / 텍스트
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the final URL.
     * <p>
     * 최종 URL 을 반환한다.
     *
     * @return the URL / URL
     */
    public String getUrl() {
        return url;
    }
}
