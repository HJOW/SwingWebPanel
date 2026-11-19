package org.duckdns.hjow.swingwebpanel.render;

import java.util.Collections;
import java.util.List;

/**
 * The immutable list of drawing commands of one page in paint order (plan items P09-04, P09-05). It is built once per layout on the document
 * thread by {@link DisplayListBuilder} and then only read, so the event dispatch thread can paint it while the document thread builds the
 * next one. Group commands (clip, opacity) are nested properly: every start command names the index of its end command.
 * <p>
 * 한 페이지의 그리기 명령을 그리기 순서로 담은 불변 리스트(계획 P09-04, P09-05). 레이아웃마다 문서 스레드에서 {@link DisplayListBuilder} 가 한 번
 * 만들고 이후에는 읽기만 하므로, 문서 스레드가 다음 것을 만드는 동안 이벤트 디스패치 스레드가 이것을 그릴 수 있다. 그룹 명령(클립, 불투명도)은
 * 올바르게 중첩되며 모든 시작 명령은 끝 명령의 위치를 안다.
 */
public final class DisplayList {

    /**
     * The commands in paint order.
     * <p>
     * 그리기 순서의 명령.
     */
    private final DisplayCommand[] commands;

    /**
     * Width of the area the commands cover (document width).
     * <p>
     * 명령이 덮는 영역의 너비(문서 너비).
     */
    private final double width;

    /**
     * Height of the area the commands cover (document height).
     * <p>
     * 명령이 덮는 영역의 높이(문서 높이).
     */
    private final double height;

    /** True if a command filters its backdrop (<code>backdrop-filter</code>), so that the page has to be painted on an image that can be read. <code>backdrop-filter</code> 로 배경을 필터링하는 명령이 있으면 true. 그러면 읽을 수 있는 이미지에 페이지를 그려야 한다. */
    private final boolean backdrops;

    /** True if a command is painted at a place that depends on the scroll position (a fixed or sticky group). 그려지는 자리가 스크롤 위치에 달린 명령(fixed, sticky 그룹)이 있으면 true. */
    private final boolean scrollDependent;

    /** Width of the viewport that the layout used (the scrollport of sticky boxes). 레이아웃이 쓴 뷰포트의 너비(sticky 상자의 스크롤포트). */
    private final double viewportWidth;

    /** Height of the viewport that the layout used. 레이아웃이 쓴 뷰포트의 높이. */
    private final double viewportHeight;

    /**
     * Creates a list.
     * <p>
     * 리스트를 만든다.
     *
     * @param list   the commands / 명령
     * @param width  document width / 문서 너비
     * @param height document height / 문서 높이
     */
    DisplayList(List<DisplayCommand> list, double width, double height) {
        this(list, width, height, width, height);
    }

    /**
     * Creates a list that knows the viewport of its layout.
     * <p>
     * 레이아웃의 뷰포트를 아는 리스트를 만든다.
     *
     * @param list           the commands / 명령
     * @param width          document width / 문서 너비
     * @param height         document height / 문서 높이
     * @param viewportWidth  width of the viewport that the layout used / 레이아웃이 쓴 뷰포트의 너비
     * @param viewportHeight height of the viewport / 뷰포트의 높이
     */
    DisplayList(List<DisplayCommand> list, double width, double height, double viewportWidth, double viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.commands = list.toArray(new DisplayCommand[list.size()]);
        boolean any = false;
        boolean scrolling = false;
        // 배경 필터가 있는 효과 그룹이 하나라도 있는지, 스크롤에 따라 그려지는 자리가 달라지는 그룹(fixed, sticky)이 있는지 본다.
        for (int i = 0; i < commands.length; i++) {
            if (commands[i] instanceof EffectGroup && ((EffectGroup) commands[i]).getBackdrop() != null) {
                any = true;
            }
            DisplayCommand.Type type = commands[i].getType();
            if (type == DisplayCommand.Type.FIXED_START || type == DisplayCommand.Type.STICKY_START) {
                scrolling = true;
            }
        }
        this.backdrops = any;
        this.scrollDependent = scrolling;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the number of commands.
     * <p>
     * 명령의 수를 반환한다.
     *
     * @return the count / 개수
     */
    public int size() {
        return commands.length;
    }

    /**
     * Returns a command.
     * <p>
     * 명령 하나를 반환한다.
     *
     * @param index the index / 위치
     * @return the command / 명령
     */
    public DisplayCommand get(int index) {
        return commands[index];
    }

    /**
     * Returns all commands as an unmodifiable list.
     * <p>
     * 모든 명령을 수정할 수 없는 리스트로 반환한다.
     *
     * @return the commands / 명령
     */
    public List<DisplayCommand> getCommands() {
        return Collections.unmodifiableList(java.util.Arrays.asList(commands));
    }

    /**
     * Returns the width of the document covered by the list.
     * <p>
     * 리스트가 덮는 문서의 너비를 반환한다.
     *
     * @return width in px / 너비(px)
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height of the document covered by the list.
     * <p>
     * 리스트가 덮는 문서의 높이를 반환한다.
     *
     * @return height in px / 높이(px)
     */
    public double getHeight() {
        return height;
    }

    /**
     * Tells whether the page has content that is painted at a place that depends on the scroll position (<code>position:fixed</code> and
     * <code>position:sticky</code> boxes): a host that scrolls by copying pixels has to paint the whole viewport again after it scrolled.
     * <p>
     * 스크롤 위치에 따라 그려지는 자리가 달라지는 내용(<code>position:fixed</code> 와 <code>position:sticky</code> 상자)이 있는지 알려 준다: 픽셀을 복사해 스크롤하는
     * 호스트는 스크롤한 뒤 뷰포트 전체를 다시 그려야 한다.
     *
     * @return true if the painting depends on the scroll position / 그리기가 스크롤 위치에 달려 있으면 true
     */
    public boolean isScrollDependent() {
        return scrollDependent;
    }

    /**
     * Returns the width of the viewport that the layout used: the scrollport that <code>position:sticky</code> boxes stick to.
     * <p>
     * 레이아웃이 쓴 뷰포트의 너비를 반환한다: <code>position:sticky</code> 상자가 붙는 스크롤포트.
     *
     * @return width in px / 너비(px)
     */
    public double getViewportWidth() {
        return viewportWidth;
    }

    /**
     * Returns the height of the viewport that the layout used.
     * <p>
     * 레이아웃이 쓴 뷰포트의 높이를 반환한다.
     *
     * @return height in px / 높이(px)
     */
    public double getViewportHeight() {
        return viewportHeight;
    }

    /**
     * Tells whether a command filters its backdrop (<code>backdrop-filter</code>).
     * <p>
     * 명령 중에 배경을 필터링하는 것(<code>backdrop-filter</code>)이 있는지 알려 준다.
     *
     * @return true if the list has one / 있으면 true
     */
    boolean hasBackdrops() {
        return backdrops;
    }

    /**
     * Returns the internal command array for the painter (not copied).
     * <p>
     * 그리는 쪽을 위해 내부 명령 배열을 반환한다(복사하지 않음).
     *
     * @return the commands / 명령
     */
    DisplayCommand[] array() {
        return commands;
    }
}
