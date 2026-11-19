package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of values: space separated (the components of one value such as <code>1px solid red</code>) or comma separated (alternatives such as
 * a font family list or several backgrounds). Property grammars produce nested lists that mirror the structure of the written value.
 * <p>
 * 값의 목록: 공백으로 구분(<code>1px solid red</code> 같은 한 값의 구성 요소) 또는 쉼표로 구분(글꼴 목록, 여러 배경 같은 대안들). 속성 문법은 쓰인 값의
 * 구조를 그대로 반영한 중첩 목록을 만든다.
 */
public final class CssList extends CssValue {

    /** The items. / 항목들. */
    private final List<CssValue> items;

    /** True for comma separated lists. / 쉼표 구분 목록이면 true. */
    private final boolean comma;

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param items items (copied) / 항목(복사됨)
     * @param comma true for a comma separated list / 쉼표 구분 목록이면 true
     */
    public CssList(List<CssValue> items, boolean comma) {
        this.items = Collections.unmodifiableList(new ArrayList<CssValue>(items));
        this.comma = comma;
    }

    /**
     * Wraps a single value in a one-item list unless it already is a list of the requested kind.
     * <p>
     * 단일 값을 한 항목짜리 목록으로 감싼다. 요청한 종류의 목록이면 그대로 둔다.
     *
     * @param v     value / 값
     * @param comma true for a comma list / 쉼표 목록이면 true
     * @return the list / 목록
     */
    public static CssList asList(CssValue v, boolean comma) {
        if (v instanceof CssList && ((CssList) v).comma == comma) {
            return (CssList) v;
        }
        List<CssValue> l = new ArrayList<CssValue>();
        l.add(v);
        return new CssList(l, comma);
    }

    /**
     * Returns the items.
     * <p>
     * 항목을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<CssValue> getItems() {
        return items;
    }

    /**
     * Returns the number of items.
     * <p>
     * 항목 수를 반환한다.
     *
     * @return size / 개수
     */
    public int size() {
        return items.size();
    }

    /**
     * Returns an item.
     * <p>
     * 항목을 반환한다.
     *
     * @param i index / 위치
     * @return the item / 항목
     */
    public CssValue get(int i) {
        return items.get(i);
    }

    /**
     * Tells whether the list is comma separated.
     * <p>
     * 쉼표로 구분한 목록인지 알려 준다.
     *
     * @return true for comma lists / 쉼표 목록이면 true
     */
    public boolean isComma() {
        return comma;
    }

    /**
     * Serializes the items with spaces or with comma and space.
     * <p>
     * 항목을 공백 또는 쉼표와 공백으로 이어 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        // 항목을 구분자로 이어 붙인다.
        for (int i = 0; i < items.size(); i++) {
            // 쉼표 구분 목록이거나 항목이 쉼표 연산자이면 앞에 공백을 두지 않는다.
            if (i > 0) {
                sb.append(comma ? ", " : items.get(i) == CssOperator.COMMA ? "" : " ");
            }
            sb.append(items.get(i).toCssText());
        }
        return sb.toString();
    }

    /**
     * Compares kind and items.
     * <p>
     * 종류와 항목을 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssList && ((CssList) o).comma == comma && ((CssList) o).items.equals(items);
    }

    /**
     * Hash of the items.
     * <p>
     * 항목의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return items.hashCode() + (comma ? 1 : 0);
    }
}
