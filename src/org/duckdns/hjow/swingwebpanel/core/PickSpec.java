package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;

/**
 * Everything that the drop-down list of a <code>select</code> element needs: the element, the geometry and style of the control as it was drawn,
 * the rows (labels, which can be chosen, which are group labels) and the listener that is told what the user chose. The form host makes it on
 * the document thread and the overlay manager reads it on the EDT; it never changes.
 * <p>
 * <code>select</code> 요소의 드롭다운 목록이 필요로 하는 모든 것: 요소, 그려진 컨트롤의 위치와 스타일, 행(라벨, 고를 수 있는 행, 그룹 라벨), 사용자가 고른 것을
 * 받는 리스너. 폼 호스트가 문서 스레드에서 만들고 오버레이 관리자가 EDT 에서 읽으며, 바뀌지 않는다.
 */
final class PickSpec {

    /**
     * Receives the choice of the user. All methods are called on the EDT.
     * <p>
     * 사용자의 선택을 받는다. 모든 메서드는 EDT 에서 호출된다.
     */
    interface Listener {

        /**
         * The user chose a row.
         * <p>
         * 사용자가 행을 골랐다.
         *
         * @param row the index of the row / 행의 인덱스
         */
        void picked(int row);

        /**
         * The user closed the list without a choice.
         * <p>
         * 사용자가 선택 없이 목록을 닫았다.
         */
        void cancelled();

        /**
         * Tells whether the page of the control is still the page that the component shows.
         * <p>
         * 컨트롤의 페이지가 아직 컴포넌트가 보여 주는 페이지인지 알려 준다.
         *
         * @return true if current / 현재 페이지이면 true
         */
        boolean isCurrent();
    }

    /** The select element. select 요소. */
    final Element element;

    /** The drawn control, for the geometry and the font. 그려진 컨트롤(위치와 글꼴). */
    final ControlSnapshot control;

    /** The text of every row. 모든 행의 글. */
    final String[] labels;

    /** True for the rows that can be chosen. 고를 수 있는 행이면 true. */
    final boolean[] enabled;

    /** True for the rows that are labels of option groups. optgroup 라벨인 행이면 true. */
    final boolean[] group;

    /** The indentation of every row (1 inside an option group). 모든 행의 들여쓰기(optgroup 안은 1). */
    final int[] indent;

    /** The index of the selected row, or -1. 선택된 행의 인덱스. 없으면 -1. */
    final int selected;

    /** The receiver of the choice. 선택을 받는 쪽. */
    final Listener listener;

    /**
     * Creates the specification.
     * <p>
     * 명세를 만든다.
     *
     * @param element  the select element / select 요소
     * @param control  the drawn control / 그려진 컨트롤
     * @param labels   the row texts / 행의 글
     * @param enabled  the rows that can be chosen / 고를 수 있는 행
     * @param group    the rows that are group labels / 그룹 라벨인 행
     * @param indent   the indentation of the rows / 행의 들여쓰기
     * @param selected the selected row or -1 / 선택된 행. 없으면 -1
     * @param listener the receiver of the choice / 선택을 받는 쪽
     */
    PickSpec(Element element, ControlSnapshot control, String[] labels, boolean[] enabled, boolean[] group, int[] indent, int selected,
            Listener listener) {
        this.element = element;
        this.control = control;
        this.labels = labels.clone();
        this.enabled = enabled.clone();
        this.group = group.clone();
        this.indent = indent.clone();
        this.selected = selected;
        this.listener = listener;
    }
}
