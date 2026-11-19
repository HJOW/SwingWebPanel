package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fixed list of rectangles (<code>DOMRectList</code>, what <code>getClientRects()</code> returns): the length and the rectangles by
 * index. The list does not change after it was created.
 * <p>
 * 사각형의 고정된 목록(<code>DOMRectList</code>, <code>getClientRects()</code> 가 돌려주는 것): 길이와 위치로 읽는 사각형. 목록은 만든 뒤 바뀌지 않는다.
 */
public final class DomRectList {

    /**
     * The rectangles.
     * <p>
     * 사각형들.
     */
    private final List<DomRect> items;

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param rects the rectangles, copied / 사각형(복사한다)
     */
    public DomRectList(List<DomRect> rects) {
        this.items = Collections.unmodifiableList(new ArrayList<DomRect>(rects));
    }

    /**
     * Returns the number of rectangles.
     * <p>
     * 사각형 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return items.size();
    }

    /**
     * Returns a rectangle.
     * <p>
     * 사각형 하나를 반환한다.
     *
     * @param index the index / 위치
     * @return the rectangle, or null if the index is out of range / 사각형. 범위 밖이면 null
     */
    public DomRect item(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }
}
