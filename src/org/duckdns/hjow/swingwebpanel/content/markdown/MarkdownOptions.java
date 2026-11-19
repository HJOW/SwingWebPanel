package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * Options of a Markdown conversion. The defaults are the safe ones: GitHub Flavored Markdown extensions on, raw HTML escaped (shown as
 * text), dangerous link schemes neutralized. An instance is mutable while it is configured; the converter copies the values it needs, so
 * changing an instance later does not affect a conversion in progress. Configure it on one thread and then share it read-only.
 * <p>
 * Markdown 변환 옵션. 기본값은 안전한 쪽이다: GitHub Flavored Markdown 확장 켜짐, 원시 HTML 은 이스케이프(텍스트로 표시), 위험한 링크 스킴은
 * 무력화. 설정하는 동안에는 인스턴스를 바꿀 수 있고 변환기는 필요한 값을 복사해 쓰므로 나중에 바꿔도 진행 중인 변환에는 영향이 없다.
 * 한 스레드에서 설정한 뒤 읽기 전용으로 공유한다.
 */
public final class MarkdownOptions {

    /** Default maximum input length in characters (8 Mi). 기본 최대 입력 길이(문자 수, 8 Mi). */
    public static final int DEFAULT_MAX_INPUT_LENGTH = 8 * 1024 * 1024;

    /** Default maximum nesting depth of container blocks. 컨테이너 블록의 기본 최대 중첩 깊이. */
    public static final int DEFAULT_MAX_BLOCK_DEPTH = 100;

    /** Default maximum nesting depth of inline elements (emphasis, links). 인라인 요소(강조, 링크)의 기본 최대 중첩 깊이. */
    public static final int DEFAULT_MAX_INLINE_DEPTH = 100;

    /** Whether raw HTML is passed through. 원시 HTML 을 그대로 내보낼지 여부. */
    private boolean allowRawHtml;

    /** Whether the GFM extensions are on. GFM 확장을 켤지 여부. */
    private boolean gfm = true;

    /** Whether dangerous link schemes are neutralized. 위험한 링크 스킴을 무력화할지 여부. */
    private boolean neutralizeUnsafeUrls = true;

    /** Maximum input length. 최대 입력 길이. */
    private int maxInputLength = DEFAULT_MAX_INPUT_LENGTH;

    /** Maximum container block depth. 컨테이너 블록 최대 깊이. */
    private int maxBlockDepth = DEFAULT_MAX_BLOCK_DEPTH;

    /** Maximum inline nesting depth. 인라인 최대 중첩 깊이. */
    private int maxInlineDepth = DEFAULT_MAX_INLINE_DEPTH;

    /**
     * Creates the default options.
     * <p>
     * 기본 옵션을 만든다.
     */
    public MarkdownOptions() {
    }

    /**
     * Tells whether raw HTML blocks and inline HTML are written to the output as they are (default false: they are escaped and shown as
     * text). When true and GFM is on, the GFM tag filter still escapes the leading <code>&lt;</code> of <code>title</code>,
     * <code>textarea</code>, <code>style</code>, <code>xmp</code>, <code>iframe</code>, <code>noembed</code>, <code>noframes</code>,
     * <code>script</code> and <code>plaintext</code> tags.
     * <p>
     * 원시 HTML 블록과 인라인 HTML 을 그대로 출력하는지 알려 준다(기본 false: 이스케이프하여 텍스트로 보여 준다). true 이고 GFM 이 켜져 있어도
     * GFM 태그 필터가 위의 태그들의 여는 <code>&lt;</code> 를 이스케이프한다.
     *
     * @return true if raw HTML is passed through / 그대로 내보내면 true
     */
    public boolean isAllowRawHtml() {
        return allowRawHtml;
    }

    /**
     * Sets whether raw HTML is passed through. A page that turns this on takes responsibility for the Markdown text it shows.
     * <p>
     * 원시 HTML 을 그대로 내보낼지 지정한다. 이를 켜는 페이지는 보여 주는 Markdown 텍스트에 책임을 진다.
     *
     * @param allowRawHtml true to pass raw HTML through / 그대로 내보내려면 true
     * @return this object / 이 객체
     */
    public MarkdownOptions setAllowRawHtml(boolean allowRawHtml) {
        this.allowRawHtml = allowRawHtml;
        return this;
    }

    /**
     * Tells whether the GitHub Flavored Markdown extensions (tables, strikethrough with two tildes, task lists, extended autolinks,
     * tag filter) are on. The default is true.
     * <p>
     * GitHub Flavored Markdown 확장(표, 물결표 두 개 취소선, 작업 목록, 확장 자동 링크, 태그 필터)이 켜져 있는지 알려 준다. 기본값은 true.
     *
     * @return true if the extensions are on / 켜져 있으면 true
     */
    public boolean isGfm() {
        return gfm;
    }

    /**
     * Switches the GFM extensions. With false the converter is a plain CommonMark 0.31.2 converter.
     * <p>
     * GFM 확장을 켜거나 끈다. false 이면 변환기는 순수한 CommonMark 0.31.2 변환기가 된다.
     *
     * @param gfm true to enable the extensions / 확장을 켜려면 true
     * @return this object / 이 객체
     */
    public MarkdownOptions setGfm(boolean gfm) {
        this.gfm = gfm;
        return this;
    }

    /**
     * Tells whether link and image destinations with the schemes <code>javascript:</code>, <code>vbscript:</code>, <code>data:</code> and
     * <code>file:</code> are neutralized (the link gets no <code>href</code>, the image an empty <code>src</code>). Default true.
     * <p>
     * <code>javascript:</code>, <code>vbscript:</code>, <code>data:</code>, <code>file:</code> 스킴의 링크/이미지 목적지를 무력화하는지(링크는
     * <code>href</code> 없음, 이미지는 빈 <code>src</code>) 알려 준다. 기본값 true.
     *
     * @return true if unsafe URLs are neutralized / 무력화하면 true
     */
    public boolean isNeutralizeUnsafeUrls() {
        return neutralizeUnsafeUrls;
    }

    /**
     * Sets whether dangerous URL schemes are neutralized.
     * <p>
     * 위험한 URL 스킴을 무력화할지 지정한다.
     *
     * @param neutralize true to neutralize / 무력화하려면 true
     * @return this object / 이 객체
     */
    public MarkdownOptions setNeutralizeUnsafeUrls(boolean neutralize) {
        this.neutralizeUnsafeUrls = neutralize;
        return this;
    }

    /**
     * Returns the maximum input length in characters; longer input makes the converter throw {@link MarkdownLimitException}.
     * <p>
     * 최대 입력 길이(문자 수)를 반환한다. 더 긴 입력은 변환기가 {@link MarkdownLimitException} 을 던지게 한다.
     *
     * @return limit / 제한
     */
    public int getMaxInputLength() {
        return maxInputLength;
    }

    /**
     * Sets the maximum input length.
     * <p>
     * 최대 입력 길이를 지정한다.
     *
     * @param maxInputLength characters, at least 1 / 문자 수(1 이상)
     * @return this object / 이 객체
     */
    public MarkdownOptions setMaxInputLength(int maxInputLength) {
        if (maxInputLength < 1) {
            throw new IllegalArgumentException("maxInputLength must be positive");
        }
        this.maxInputLength = maxInputLength;
        return this;
    }

    /**
     * Returns the maximum nesting depth of container blocks (block quotes, list items); markers beyond it are shown as text.
     * <p>
     * 컨테이너 블록(인용, 목록 항목)의 최대 중첩 깊이를 반환한다. 이를 넘는 표식은 텍스트로 보여 준다.
     *
     * @return depth / 깊이
     */
    public int getMaxBlockDepth() {
        return maxBlockDepth;
    }

    /**
     * Sets the maximum nesting depth of container blocks.
     * <p>
     * 컨테이너 블록의 최대 중첩 깊이를 지정한다.
     *
     * @param maxBlockDepth depth, at least 1 / 깊이(1 이상)
     * @return this object / 이 객체
     */
    public MarkdownOptions setMaxBlockDepth(int maxBlockDepth) {
        if (maxBlockDepth < 1) {
            throw new IllegalArgumentException("maxBlockDepth must be positive");
        }
        this.maxBlockDepth = maxBlockDepth;
        return this;
    }

    /**
     * Returns the maximum nesting depth of inline elements; emphasis or links that would be nested deeper stay literal text.
     * <p>
     * 인라인 요소의 최대 중첩 깊이를 반환한다. 이보다 깊게 중첩될 강조나 링크는 글자 그대로의 텍스트로 남는다.
     *
     * @return depth / 깊이
     */
    public int getMaxInlineDepth() {
        return maxInlineDepth;
    }

    /**
     * Sets the maximum nesting depth of inline elements.
     * <p>
     * 인라인 요소의 최대 중첩 깊이를 지정한다.
     *
     * @param maxInlineDepth depth, at least 1 / 깊이(1 이상)
     * @return this object / 이 객체
     */
    public MarkdownOptions setMaxInlineDepth(int maxInlineDepth) {
        if (maxInlineDepth < 1) {
            throw new IllegalArgumentException("maxInlineDepth must be positive");
        }
        this.maxInlineDepth = maxInlineDepth;
        return this;
    }

    /**
     * Returns a copy of these options.
     * <p>
     * 이 옵션의 복사본을 반환한다.
     *
     * @return copy / 복사본
     */
    public MarkdownOptions copy() {
        MarkdownOptions o = new MarkdownOptions();
        o.allowRawHtml = allowRawHtml;
        o.gfm = gfm;
        o.neutralizeUnsafeUrls = neutralizeUnsafeUrls;
        o.maxInputLength = maxInputLength;
        o.maxBlockDepth = maxBlockDepth;
        o.maxInlineDepth = maxInlineDepth;
        return o;
    }
}
