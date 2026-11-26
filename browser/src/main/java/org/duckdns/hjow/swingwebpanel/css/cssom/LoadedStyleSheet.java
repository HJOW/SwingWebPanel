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
    /** Whether the loader established same-origin access independently of the textual URL. / 로더가 URL 문자열과 별도로 동일 오리진 접근을 확인했는지 여부. */
    private final boolean originClean;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param text the text / 텍스트
     * @param url  the final URL / 최종 URL
     */
    public LoadedStyleSheet(String text, String url) {
        this(text, url, false);
    }

    /**
     * Creates a result with explicit same-origin access information.
     * <p>
     * 명시적인 동일 오리진 접근 정보로 결과를 만든다.
     *
     * @param text decoded CSS / 디코딩한 CSS
     * @param url final URL / 최종 URL
     * @param originClean true if origin access has been independently established / 오리진 접근을 별도로 확인했으면 true
     */
    public LoadedStyleSheet(String text, String url, boolean originClean) {
        this.text = text;
        this.url = url;
        this.originClean = originClean;
    }

    /** Returns the loader's explicit origin access decision. / 로더가 명시적으로 허용한 오리진 접근 여부를 반환한다. */
    boolean isOriginClean() {
        return originClean;
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
