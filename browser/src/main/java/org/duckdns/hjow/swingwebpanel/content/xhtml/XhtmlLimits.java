package org.duckdns.hjow.swingwebpanel.content.xhtml;

/**
 * Upper bounds for parsing one XHTML document (plan item P17-05). They make every attack on the parser end with a bounded error
 * ({@link XmlParseError.Kind#LIMIT_EXCEEDED}) instead of an endless loop or an exhausted heap: an entity expansion bomb ("billion laughs"
 * or one huge entity used many times), a very deep nesting, millions of nodes or attributes, or a parse that simply takes too long.
 * The defaults suit real documents with a large margin; a host that needs other values changes them with the setters before the
 * parse. The values are copied when a parse starts, so changing an instance during a parse has no effect on it.
 * <p>
 * XHTML 문서 하나를 해석할 때의 상한(계획 P17-05). 파서에 대한 모든 공격이 무한 반복이나 힙 고갈이 아니라 한정된 오류
 * ({@link XmlParseError.Kind#LIMIT_EXCEEDED})로 끝나게 한다: 엔티티 확장 폭탄("billion laughs", 또는 큰 엔티티 하나를 여러 번 쓰는 경우), 아주 깊은 중첩,
 * 수백만 개의 노드나 속성, 단순히 너무 오래 걸리는 해석. 기본값은 실제 문서에 넉넉하며 다른 값이 필요한 호스트는 해석 전에 setter 로 바꾼다.
 * 해석을 시작할 때 값을 복사하므로 해석 도중에 인스턴스를 바꿔도 영향이 없다.
 */
public final class XhtmlLimits {

    /**
     * Default maximum size of the input text in characters (the same order as the 16 MiB memory limit of a response).
     * <p>
     * 입력 텍스트의 기본 최대 크기(문자 수). 응답 메모리 제한 16 MiB 와 같은 수준이다.
     */
    public static final int DEFAULT_MAX_CHARS = 16 * 1024 * 1024;

    /**
     * Default maximum nesting depth of elements.
     * <p>
     * 요소 중첩의 기본 최대 깊이.
     */
    public static final int DEFAULT_MAX_DEPTH = 512;

    /**
     * Default maximum number of nodes (elements, text, comments, ...) in the tree.
     * <p>
     * 트리의 기본 최대 노드 수(요소, 텍스트, 주석 등).
     */
    public static final int DEFAULT_MAX_NODES = 1000000;

    /**
     * Default maximum number of attributes of one element.
     * <p>
     * 요소 하나의 기본 최대 속성 수.
     */
    public static final int DEFAULT_MAX_ATTRIBUTES = 4096;

    /**
     * Default maximum total number of text and attribute value characters after entity expansion.
     * <p>
     * 엔티티를 펼친 뒤 텍스트와 속성 값 문자의 기본 최대 총수.
     */
    public static final long DEFAULT_MAX_TEXT_CHARS = 32L * 1024 * 1024;

    /**
     * Default maximum number of entity references that are expanded (every <code>&amp;nbsp;</code> counts once, nested references count too).
     * <p>
     * 펼치는 엔티티 참조의 기본 최대 수(<code>&amp;nbsp;</code> 하나가 한 번이며 중첩된 참조도 센다).
     */
    public static final int DEFAULT_MAX_ENTITY_EXPANSIONS = 500000;

    /**
     * Default maximum parse time in milliseconds.
     * <p>
     * 기본 최대 해석 시간(밀리초).
     */
    public static final long DEFAULT_MAX_MILLIS = 30000L;

    /**
     * Maximum size of the input in characters.
     * <p>
     * 입력의 최대 크기(문자 수).
     */
    private int maxChars = DEFAULT_MAX_CHARS;

    /**
     * Maximum nesting depth.
     * <p>
     * 최대 중첩 깊이.
     */
    private int maxDepth = DEFAULT_MAX_DEPTH;

    /**
     * Maximum number of nodes.
     * <p>
     * 최대 노드 수.
     */
    private int maxNodes = DEFAULT_MAX_NODES;

    /**
     * Maximum number of attributes per element.
     * <p>
     * 요소당 최대 속성 수.
     */
    private int maxAttributes = DEFAULT_MAX_ATTRIBUTES;

    /**
     * Maximum total number of expanded text characters.
     * <p>
     * 펼친 텍스트 문자의 최대 총수.
     */
    private long maxTextChars = DEFAULT_MAX_TEXT_CHARS;

    /**
     * Maximum parse time in milliseconds.
     * <p>
     * 최대 해석 시간(밀리초).
     */
    private long maxMillis = DEFAULT_MAX_MILLIS;

    /**
     * Maximum number of entity expansions.
     * <p>
     * 엔티티 확장의 최대 수.
     */
    private int maxEntityExpansions = DEFAULT_MAX_ENTITY_EXPANSIONS;

    /**
     * Creates the limits with the default values.
     * <p>
     * 기본값으로 상한을 만든다.
     */
    public XhtmlLimits() {
    }

    /**
     * Creates a copy of other limits.
     * <p>
     * 다른 상한의 복사본을 만든다.
     *
     * @param other the limits to copy / 복사할 상한
     */
    public XhtmlLimits(XhtmlLimits other) {
        this.maxChars = other.maxChars;
        this.maxDepth = other.maxDepth;
        this.maxNodes = other.maxNodes;
        this.maxAttributes = other.maxAttributes;
        this.maxTextChars = other.maxTextChars;
        this.maxMillis = other.maxMillis;
        this.maxEntityExpansions = other.maxEntityExpansions;
    }

    /**
     * Returns the maximum size of the input in characters.
     * <p>
     * 입력의 최대 크기(문자 수)를 반환한다.
     *
     * @return the limit / 상한
     */
    public int getMaxChars() {
        return maxChars;
    }

    /**
     * Sets the maximum size of the input in characters.
     * <p>
     * 입력의 최대 크기(문자 수)를 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxChars(int value) {
        this.maxChars = positive(value);
        return this;
    }

    /**
     * Returns the maximum nesting depth of elements.
     * <p>
     * 요소의 최대 중첩 깊이를 반환한다.
     *
     * @return the limit / 상한
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Sets the maximum nesting depth of elements.
     * <p>
     * 요소의 최대 중첩 깊이를 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxDepth(int value) {
        this.maxDepth = positive(value);
        return this;
    }

    /**
     * Returns the maximum number of nodes.
     * <p>
     * 최대 노드 수를 반환한다.
     *
     * @return the limit / 상한
     */
    public int getMaxNodes() {
        return maxNodes;
    }

    /**
     * Sets the maximum number of nodes.
     * <p>
     * 최대 노드 수를 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxNodes(int value) {
        this.maxNodes = positive(value);
        return this;
    }

    /**
     * Returns the maximum number of attributes of one element.
     * <p>
     * 요소 하나의 최대 속성 수를 반환한다.
     *
     * @return the limit / 상한
     */
    public int getMaxAttributes() {
        return maxAttributes;
    }

    /**
     * Sets the maximum number of attributes of one element.
     * <p>
     * 요소 하나의 최대 속성 수를 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxAttributes(int value) {
        this.maxAttributes = positive(value);
        return this;
    }

    /**
     * Returns the maximum total number of expanded text and attribute value characters.
     * <p>
     * 펼친 텍스트와 속성 값 문자의 최대 총수를 반환한다.
     *
     * @return the limit / 상한
     */
    public long getMaxTextChars() {
        return maxTextChars;
    }

    /**
     * Sets the maximum total number of expanded text and attribute value characters.
     * <p>
     * 펼친 텍스트와 속성 값 문자의 최대 총수를 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxTextChars(long value) {
        if (value < 1) {
            throw new IllegalArgumentException("limit must be positive: " + value);
        }
        this.maxTextChars = value;
        return this;
    }

    /**
     * Returns the maximum parse time in milliseconds.
     * <p>
     * 최대 해석 시간(밀리초)을 반환한다.
     *
     * @return the limit / 상한
     */
    public long getMaxMillis() {
        return maxMillis;
    }

    /**
     * Sets the maximum parse time in milliseconds.
     * <p>
     * 최대 해석 시간(밀리초)을 지정한다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxMillis(long value) {
        if (value < 1) {
            throw new IllegalArgumentException("limit must be positive: " + value);
        }
        this.maxMillis = value;
        return this;
    }

    /**
     * Returns the maximum number of entity expansions.
     * <p>
     * 엔티티 확장의 최대 수를 반환한다.
     *
     * @return the limit / 상한
     */
    public int getMaxEntityExpansions() {
        return maxEntityExpansions;
    }

    /**
     * Sets the maximum number of entity expansions. Every reference to a general entity counts, <code>&amp;nbsp;</code> as well as the
     * references inside the replacement text of an entity; this is what stops "billion laughs" style documents.
     * <p>
     * 엔티티 확장의 최대 수를 지정한다. 일반 엔티티 참조는 모두 센다. <code>&amp;nbsp;</code> 도, 엔티티의 치환 텍스트 안의 참조도 센다. 이것이
     * "billion laughs" 형태의 문서를 막는다.
     *
     * @param value the limit, at least 1 / 상한(1 이상)
     * @return this object / 이 객체
     * @throws IllegalArgumentException if the value is less than 1 / 값이 1 보다 작으면
     */
    public XhtmlLimits setMaxEntityExpansions(int value) {
        this.maxEntityExpansions = positive(value);
        return this;
    }

    /**
     * Checks that a limit is positive.
     * <p>
     * 상한이 양수인지 확인한다.
     *
     * @param value the value / 값
     * @return the same value / 같은 값
     * @throws IllegalArgumentException if it is less than 1 / 1 보다 작으면
     */
    private static int positive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("limit must be positive: " + value);
        }
        return value;
    }
}
