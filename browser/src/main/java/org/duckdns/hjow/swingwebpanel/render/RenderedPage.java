package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;

/**
 * An immutable, fully rendered page (plan items P09-04 to P09-07): the {@link LayoutResult}, the {@link DisplayList} built from it and a
 * {@link HitTester}. The document thread builds it with {@link PageRenderer} and hands it over; from then on the event dispatch thread only
 * reads it: {@link #paint} in <code>paintComponent</code>, {@link #hitTest} for mouse events, {@link #getPreferredWidth()} and
 * {@link #getPreferredHeight()} for <code>getPreferredSize</code> (the scrollable area in unscaled document pixels). A new page object
 * replaces the old one after every relayout; the old one stays valid while a paint is still using it.
 * <p>
 * 완전히 렌더링된 불변 페이지(계획 P09-04~P09-07): {@link LayoutResult}, 그것으로 만든 {@link DisplayList}, {@link HitTester}. 문서 스레드가
 * {@link PageRenderer} 로 만들어 넘기며, 그 뒤 이벤트 디스패치 스레드는 읽기만 한다: <code>paintComponent</code> 에서 {@link #paint},
 * 마우스 이벤트에서 {@link #hitTest}, <code>getPreferredSize</code> 에서 {@link #getPreferredWidth()} 와 {@link #getPreferredHeight()}(배율을 적용하지 않은
 * 문서 픽셀의 스크롤 영역). 다시 배치할 때마다 새 페이지 객체가 옛것을 대체하며, 옛것은 그리기가 아직 쓰는 동안 유효하다.
 */
public final class RenderedPage {

    /**
     * The layout.
     * <p>
     * 레이아웃.
     */
    private final LayoutResult layout;

    /**
     * The display list.
     * <p>
     * 디스플레이 리스트.
     */
    private final DisplayList displayList;

    /**
     * The hit tester of the layout.
     * <p>
     * 레이아웃의 히트 테스터.
     */
    private final HitTester hitTester;

    /**
     * The snapshots of the form controls by element, made on first use.
     * <p>
     * 요소별 폼 컨트롤 스냅샷. 처음 쓸 때 만든다.
     */
    private java.util.Map<org.duckdns.hjow.swingwebpanel.dom.Element, ControlSnapshot> controls;

    /**
     * Creates a page.
     * <p>
     * 페이지를 만든다.
     *
     * @param layout      the layout / 레이아웃
     * @param displayList the display list / 디스플레이 리스트
     */
    RenderedPage(LayoutResult layout, DisplayList displayList) {
        this.layout = layout;
        this.displayList = displayList;
        this.hitTester = new HitTester(layout);
    }

    /**
     * Returns the snapshot of the form control of an element as it was drawn in this page, or null if the element is not a drawn control
     * (or has no box). The Swing editors of the text controls take their position, font and colors from it.
     * <p>
     * 요소의 폼 컨트롤이 이 페이지에서 그려진 대로의 스냅샷을 반환한다. 그려지는 컨트롤이 아니거나 상자가 없으면 null. 텍스트 컨트롤의 Swing 편집기가
     * 이것에서 위치, 글꼴, 색을 가져간다.
     *
     * @param element the element, may be null / 요소. null 가능
     * @return the snapshot or null / 스냅샷 또는 null
     */
    public synchronized ControlSnapshot getControl(org.duckdns.hjow.swingwebpanel.dom.Element element) {
        if (controls == null) {
            controls = new java.util.IdentityHashMap<org.duckdns.hjow.swingwebpanel.dom.Element, ControlSnapshot>();
            DisplayCommand[] all = displayList.array();
            // 디스플레이 리스트를 훑으며 컨트롤 명령의 스냅샷을 요소별로 모은다.
            for (int i = 0; i < all.length; i++) {
                if (all[i] instanceof ControlCommand) {
                    ControlSnapshot cs = ((ControlCommand) all[i]).getSnapshot();
                    controls.put(cs.getElement(), cs);
                }
            }
        }
        return controls.get(element);
    }

    /**
     * Returns the layout.
     * <p>
     * 레이아웃을 반환한다.
     *
     * @return the layout / 레이아웃
     */
    public LayoutResult getLayout() {
        return layout;
    }

    /**
     * Returns the display list.
     * <p>
     * 디스플레이 리스트를 반환한다.
     *
     * @return the display list / 디스플레이 리스트
     */
    public DisplayList getDisplayList() {
        return displayList;
    }

    /**
     * Returns the hit tester.
     * <p>
     * 히트 테스터를 반환한다.
     *
     * @return the hit tester / 히트 테스터
     */
    public HitTester getHitTester() {
        return hitTester;
    }

    /**
     * Returns the width of the document in whole pixels (the content width, at least the viewport width).
     * <p>
     * 문서의 너비를 정수 픽셀로 반환한다(콘텐츠 너비, 최소 뷰포트 너비).
     *
     * @return width / 너비
     */
    public int getPreferredWidth() {
        return (int) Math.ceil(Math.max(layout.getContentWidth(), layout.getViewportWidth()));
    }

    /**
     * Returns the height of the document in whole pixels (the content height).
     * <p>
     * 문서의 높이를 정수 픽셀로 반환한다(콘텐츠 높이).
     *
     * @return height / 높이
     */
    public int getPreferredHeight() {
        return (int) Math.ceil(layout.getContentHeight());
    }

    /**
     * Paints the page at 100 percent.
     * <p>
     * 페이지를 100 % 로 그린다.
     *
     * @param g       the graphics of the component / 컴포넌트의 그래픽스
     * @param clip    the area to paint in component coordinates, null for the clip of the graphics / 컴포넌트 좌표로 그릴 영역. null 이면 그래픽스의 클립
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     */
    public void paint(Graphics2D g, Rectangle clip, int scrollX, int scrollY) {
        PagePainter.paint(g, displayList, clip, scrollX, scrollY, 1.0);
    }

    /**
     * Paints the page with a zoom factor and an animation time.
     * <p>
     * 페이지를 확대 배율과 애니메이션 시각으로 그린다.
     *
     * @param g          the graphics / 그래픽스
     * @param clip       the area to paint or null / 그릴 영역 또는 null
     * @param scrollX    horizontal scroll offset in document px / 문서 px 단위 가로 스크롤
     * @param scrollY    vertical scroll offset / 세로 스크롤
     * @param scale      the zoom factor / 확대 배율
     * @param timeMillis animation time in milliseconds / 애니메이션 시각(밀리초)
     */
    public void paint(Graphics2D g, Rectangle clip, int scrollX, int scrollY, double scale, long timeMillis) {
        PagePainter.paint(g, displayList, clip, scrollX, scrollY, scale, timeMillis);
    }

    /**
     * Paints the page of a host that does the scrolling itself (a <code>JScrollPane</code> moves the component, so the graphics are already
     * scrolled): <code>position:fixed</code> boxes stay at the viewport and <code>position:sticky</code> boxes stick to it, which needs the position
     * of the viewport that the host scrolled to.
     * <p>
     * 스크롤을 스스로 하는 호스트의 페이지를 그린다(<code>JScrollPane</code> 이 컴포넌트를 옮기므로 그래픽스는 이미 스크롤되어 있다): <code>position:fixed</code> 상자는
     * 뷰포트에 머물고 <code>position:sticky</code> 상자는 뷰포트에 붙으므로 호스트가 스크롤한 뷰포트의 위치가 필요하다.
     *
     * @param g          the graphics / 그래픽스
     * @param clip       the area to paint or null / 그릴 영역 또는 null
     * @param scrollX    horizontal offset applied here (0 if the graphics are scrolled already) / 여기서 적용하는 가로 오프셋(그래픽스가 이미 스크롤되었으면 0)
     * @param scrollY    vertical offset applied here / 여기서 적용하는 세로 오프셋
     * @param scale      the zoom factor / 확대 배율
     * @param timeMillis animation time in milliseconds / 애니메이션 시각(밀리초)
     * @param viewX      x of the viewport in document px / 문서 px 단위 뷰포트의 x
     * @param viewY      y of the viewport in document px / 문서 px 단위 뷰포트의 y
     */
    public void paint(Graphics2D g, Rectangle clip, int scrollX, int scrollY, double scale, long timeMillis, int viewX, int viewY) {
        PagePainter.paint(g, displayList, clip, scrollX, scrollY, scale, timeMillis, viewX, viewY);
    }

    /**
     * Finds what is at a point given in component coordinates.
     * <p>
     * 컴포넌트 좌표로 준 점에 무엇이 있는지 찾는다.
     *
     * @param screenX x in component coordinates / 컴포넌트 좌표의 x
     * @param screenY y in component coordinates / 컴포넌트 좌표의 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @param scale   the zoom factor / 확대 배율
     * @return the result / 결과
     */
    public HitResult hitTest(double screenX, double screenY, int scrollX, int scrollY, double scale) {
        return hitTester.hit(HitTester.toDocumentX(screenX, scrollX, scale), HitTester.toDocumentY(screenY, scrollY, scale), scrollX, scrollY);
    }

    /**
     * Finds what is at a point given in document coordinates.
     * <p>
     * 문서 좌표로 준 점에 무엇이 있는지 찾는다.
     *
     * @param docX x in document coordinates / 문서 좌표의 x
     * @param docY y in document coordinates / 문서 좌표의 y
     * @return the result / 결과
     */
    public HitResult hitTestDocument(double docX, double docY) {
        return hitTester.hit(docX, docY);
    }
}
