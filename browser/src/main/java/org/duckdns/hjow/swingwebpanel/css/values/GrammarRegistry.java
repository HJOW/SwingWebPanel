package org.duckdns.hjow.swingwebpanel.css.values;

/**
 * The lookup of named value definitions (<code>&lt;line-width&gt;</code>, <code>&lt;position&gt;</code>, <code>&lt;family-name&gt;</code> ...) used by
 * a {@link ValueGrammar} to resolve the references that are not built-in types.
 * <p>
 * {@link ValueGrammar} 가 기본 형식이 아닌 참조를 해석할 때 쓰는 이름 있는 값 정의(<code>&lt;line-width&gt;</code>, <code>&lt;position&gt;</code>,
 * <code>&lt;family-name&gt;</code> 등)의 조회.
 */
public interface GrammarRegistry {

    /**
     * Returns the grammar of a named definition.
     * <p>
     * 이름 있는 정의의 문법을 반환한다.
     *
     * @param name definition name without the angle brackets / 꺾쇠 없는 정의 이름
     * @return the grammar, or null if the name is unknown / 문법. 이름을 모르면 null
     */
    ValueGrammar get(String name);
}
