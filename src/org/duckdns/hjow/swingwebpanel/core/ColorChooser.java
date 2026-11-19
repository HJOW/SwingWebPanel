package org.duckdns.hjow.swingwebpanel.core;

/**
 * Lets the user choose the color of a color control (<code>input type=color</code>). The default implementation of the component shows a Swing
 * color chooser dialog; a host or a test replaces it to choose a color without a dialog. It is called on the event dispatch thread.
 * <p>
 * 사용자가 색 컨트롤(<code>input type=color</code>)의 색을 고르게 한다. 컴포넌트의 기본 구현은 Swing 색 선택 대화상자를 보여 주며, 호스트나 시험은 대화상자 없이
 * 색을 고르도록 이것을 바꿀 수 있다. 이벤트 디스패치 스레드에서 호출된다.
 */
public interface ColorChooser {

    /**
     * Asks the user for a color.
     * <p>
     * 사용자에게 색을 묻는다.
     *
     * @param rgb the color that the control has now (0xRRGGBB) / 컨트롤의 현재 색(0xRRGGBB)
     * @return the chosen color (0xRRGGBB), or null if the user cancelled / 고른 색(0xRRGGBB). 사용자가 취소했으면 null
     */
    Integer choose(int rgb);
}
