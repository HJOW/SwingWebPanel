package org.duckdns.hjow.swingwebpanel.css.props;

import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;

/**
 * A shorthand property: a name that sets several longhand properties from one value. The parser of a shorthand receives the component values of the
 * declaration (whitespace removed) and writes one specified value for every longhand it sets; longhands the written value does not mention are set to
 * <code>initial</code>. Subclasses live in {@link Shorthands}.
 * <p>
 * 단축 속성: 값 하나로 여러 개별 속성을 정하는 이름. 단축 속성의 파서는 선언의 컴포넌트 값(공백 제거)을 받아 정하는 모든 개별 속성에 지정 값 하나씩을 쓰며,
 * 쓰인 값이 언급하지 않는 개별 속성은 <code>initial</code> 로 정한다. 하위 클래스는 {@link Shorthands} 에 있다.
 */
public abstract class Shorthand {

    /** The shorthand name. / 단축 속성 이름. */
    private final String name;

    /** The ids of all longhands it sets. / 정하는 모든 개별 속성의 id. */
    private final int[] longhands;

    /**
     * Creates a shorthand.
     * <p>
     * 단축 속성을 만든다.
     *
     * @param name      the name / 이름
     * @param longhands the longhand ids / 개별 속성 id
     */
    protected Shorthand(String name, int[] longhands) {
        this.name = name;
        this.longhands = longhands.clone();
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 반환한다.
     *
     * @return lower-case name / 소문자 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ids of the longhands this shorthand sets.
     * <p>
     * 이 단축 속성이 정하는 개별 속성의 id 를 반환한다.
     *
     * @return a copy of the ids / id 의 복사본
     */
    public int[] getLonghands() {
        return longhands.clone();
    }

    /**
     * Parses the value and writes the longhand values.
     * <p>
     * 값을 파싱해 개별 속성 값을 쓴다.
     *
     * @param items the declaration value without whitespace tokens / 공백 토큰이 없는 선언 값
     * @param reg   the registry (for the longhand grammars) / 등록부(개별 속성 문법용)
     * @param env   the parse environment / 파싱 환경
     * @param out   receives longhand id and specified value / 개별 속성 id 와 지정 값을 받는다
     * @return true if the value is valid / 올바른 값이면 true
     */
    public abstract boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out);

    /**
     * Serializes the shorthand from the values of its longhands (CSSOM <code>getPropertyValue</code>).
     * <p>
     * 개별 속성의 값으로 단축 속성을 직렬화한다(CSSOM <code>getPropertyValue</code>).
     *
     * @param values longhand values by id (a missing id means the longhand is not set) / id 별 개별 속성 값(없으면 설정되지 않은 것)
     * @return the shorthand text, or null if it cannot be represented / 단축 속성 문자열. 표현할 수 없으면 null
     */
    public String serialize(Map<Integer, CssValue> values) {
        return null;
    }
}
