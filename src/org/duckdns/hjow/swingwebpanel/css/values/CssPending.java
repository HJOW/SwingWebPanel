package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;

/**
 * A pending-substitution value: a declaration whose value contains <code>var()</code> cannot be parsed when the style sheet is read, because the
 * custom properties it refers to are only known per element. The declaration is kept as component values together with the name of the property
 * it was written for, and substituted and parsed at computed-value time (see <code>css.cascade.StyleResolver</code>).
 * <p>
 * 치환 대기 값: <code>var()</code> 가 든 선언은 참조하는 사용자 정의 속성을 요소마다 알 수 있으므로 스타일 시트를 읽을 때 파싱할 수 없다. 선언은 컴포넌트
 * 값과 쓰인 속성 이름으로 보관했다가 계산 값을 구할 때 치환하고 파싱한다(<code>css.cascade.StyleResolver</code> 참조).
 */
public final class CssPending extends CssValue {

    /** The property name as written (a shorthand or a longhand). / 쓰인 속성 이름(단축 속성 또는 개별 속성). */
    private final String property;

    /** The value as component values. / 컴포넌트 값으로 된 값. */
    private final List<ComponentValue> tokens;

    /** The base URL for relative url() values, or null. / 상대 url() 값의 기준 URL. 없으면 null. */
    private final String baseUrl;

    /**
     * Creates a pending value.
     * <p>
     * 치환 대기 값을 만든다.
     *
     * @param property the property name as written / 쓰인 속성 이름
     * @param tokens   the value component values (copied) / 값의 컴포넌트 값(복사됨)
     * @param baseUrl  the base URL of the style sheet or null / 스타일 시트의 기준 URL 또는 null
     */
    public CssPending(String property, List<ComponentValue> tokens, String baseUrl) {
        this.property = property;
        this.tokens = Collections.unmodifiableList(new ArrayList<ComponentValue>(tokens));
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the base URL used to resolve relative urls after substitution.
     * <p>
     * 치환 뒤 상대 url 을 해석하는 기준 URL 을 반환한다.
     *
     * @return the base URL or null / 기준 URL 또는 null
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the property name as written.
     * <p>
     * 쓰인 속성 이름을 반환한다.
     *
     * @return property name / 속성 이름
     */
    public String getProperty() {
        return property;
    }

    /**
     * Returns the value component values.
     * <p>
     * 값의 컴포넌트 값을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComponentValue> getTokens() {
        return tokens;
    }

    /**
     * Serializes the written value.
     * <p>
     * 쓰인 값을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssBlock.join(tokens).trim();
    }
}
