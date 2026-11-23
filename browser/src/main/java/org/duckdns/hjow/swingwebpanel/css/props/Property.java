package org.duckdns.hjow.swingwebpanel.css.props;

import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;

/**
 * The definition of one longhand property: id, name, inheritance, initial value, value grammar and whether it is only parsed. Instances are
 * created by {@link PropertyRegistry} from the rows of the property table and are immutable.
 * <p>
 * 개별 속성 하나의 정의: id, 이름, 상속 여부, 초기값, 값 문법, 파싱만 하는지 여부. {@link PropertyRegistry} 가 속성 표의 행에서 만들며 불변이다.
 */
public final class Property {

    /** The numeric id (see {@link Prop}). / 숫자 id({@link Prop} 참조). */
    private final int id;

    /** The lower-case name. / 소문자 이름. */
    private final String name;

    /** True if the property is inherited by default. / 기본으로 상속되는 속성이면 true. */
    private final boolean inherited;

    /** The initial value as a specified value. / 지정 값으로 표현한 초기값. */
    private final CssValue initial;

    /** The value definition. / 값 정의. */
    private final ValueGrammar grammar;

    /** True if the property has no effect on rendering. / 렌더링에 영향이 없는 속성이면 true. */
    private final boolean parsedOnly;

    /**
     * Creates a property definition.
     * <p>
     * 속성 정의를 만든다.
     *
     * @param id         numeric id / 숫자 id
     * @param name       lower-case name / 소문자 이름
     * @param inherited  inherited flag / 상속 플래그
     * @param initial    initial value / 초기값
     * @param grammar    value definition / 값 정의
     * @param parsedOnly parsed-only flag / 파싱만 플래그
     */
    Property(int id, String name, boolean inherited, CssValue initial, ValueGrammar grammar, boolean parsedOnly) {
        this.id = id;
        this.name = name;
        this.inherited = inherited;
        this.initial = initial;
        this.grammar = grammar;
        this.parsedOnly = parsedOnly;
    }

    /**
     * Returns the id.
     * <p>
     * id 를 반환한다.
     *
     * @return id from {@link Prop} / {@link Prop} 의 id
     */
    public int getId() {
        return id;
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
     * Tells whether the property is inherited.
     * <p>
     * 속성이 상속되는지 알려 준다.
     *
     * @return true if inherited / 상속되면 true
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * Returns the initial value.
     * <p>
     * 초기값을 반환한다.
     *
     * @return the specified initial value / 지정 값 형태의 초기값
     */
    public CssValue getInitial() {
        return initial;
    }

    /**
     * Returns the value grammar.
     * <p>
     * 값 문법을 반환한다.
     *
     * @return the grammar / 문법
     */
    public ValueGrammar getGrammar() {
        return grammar;
    }

    /**
     * Tells whether the property is parsed and stored but not applied by the renderer.
     * <p>
     * 속성을 파싱하고 저장하지만 렌더러가 적용하지 않는지 알려 준다.
     *
     * @return true for parsed-only properties / 파싱만 하는 속성이면 true
     */
    public boolean isParsedOnly() {
        return parsedOnly;
    }
}
