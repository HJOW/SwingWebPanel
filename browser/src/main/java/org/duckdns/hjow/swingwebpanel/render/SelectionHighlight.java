package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The drawn highlight of a selection (plan item P11-08): the area of the selected text and replaced elements of one rendered page, as one
 * shape in document coordinates. The document thread computes it with {@link TextSelections#highlight} when the selection or the layout
 * changed and hands it over; the event dispatch thread only paints it on top of the page, scaled by the zoom factor. It is immutable. The
 * area is filled in one operation, so rectangles that overlap by a fraction of a pixel are not blended twice. The default color is the
 * system blue as a translucent overlay (the text stays visible below it); the <code>::selection</code> pseudo-element is not applied yet.
 * <p>
 * 선택의 그려지는 강조(계획 P11-08): 한 렌더링된 페이지의 선택된 텍스트와 치환 요소의 영역을 문서 좌표의 도형 하나로 나타낸다. 선택이나 레이아웃이 바뀌면 문서
 * 스레드가 {@link TextSelections#highlight} 로 계산해 넘기고, 이벤트 디스패치 스레드는 그것을 배율에 맞춰 페이지 위에 그리기만 한다. 불변이다. 영역은 한 번에
 * 채우므로 조금씩 겹치는 사각형이 두 번 섞이지 않는다. 기본 색은 시스템 파란색의 반투명 덧칠이다(그 아래 글자가 보인다). <code>::selection</code> 의사 요소는
 * 아직 적용하지 않는다.
 */
public final class SelectionHighlight {

    /**
     * The default highlight color as ARGB: the system blue, translucent.
     * <p>
     * 기본 강조 색(ARGB): 시스템 파란색, 반투명.
     */
    public static final int DEFAULT_COLOR = 0x703390FF;

    /**
     * The rendered page that the shape was computed for (so that a highlight of an older page is never drawn over a newer one).
     * <p>
     * 도형을 계산한 렌더링된 페이지(옛 페이지의 강조가 새 페이지 위에 그려지지 않게 한다).
     */
    private final RenderedPage page;

    /**
     * The highlighted area in document coordinates.
     * <p>
     * 강조된 영역(문서 좌표).
     */
    private final Shape shape;

    /**
     * The bounds of the area in document coordinates.
     * <p>
     * 영역의 경계(문서 좌표).
     */
    private final Rectangle2D bounds;

    /**
     * The color of the highlight as an ARGB value.
     * <p>
     * ARGB 값으로 나타낸 강조의 색상.
     */
    private final int color;

    /**
     * Creates a highlight.
     * <p>
     * 강조를 만든다.
     *
     * @param page   the page, not null / 페이지(null 불가)
     * @param shape  the area in document coordinates, not null / 문서 좌표의 영역(null 불가)
     * @param color  the ARGB color / ARGB 색
     */
    SelectionHighlight(RenderedPage page, Shape shape, int color) {
        this.page = page;
        this.shape = shape;
        this.bounds = shape.getBounds2D();
        this.color = color;
    }

    /**
     * Makes the mark of the editing caret: a small opaque rectangle of one rendered page, drawn the same way as a highlight.
     * <p>
     * 편집 캐럿의 표시를 만든다: 한 렌더링된 페이지의 작은 불투명 사각형이며 강조와 같은 방식으로 그린다.
     *
     * @param page  the page, not null / 페이지(null 불가)
     * @param bar   the bar in document coordinates / 문서 좌표의 막대
     * @param color the ARGB color / ARGB 색
     * @return the mark / 표시
     */
    static SelectionHighlight caret(RenderedPage page, Rectangle2D bar, int color) {
        return new SelectionHighlight(page, bar, color);
    }

    /**
     * Returns the rendered page that this highlight belongs to.
     * <p>
     * 이 강조가 속한 렌더링된 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    public RenderedPage getPage() {
        return page;
    }

    /**
     * Returns the highlighted area (document coordinates).
     * <p>
     * 강조된 영역을 반환한다(문서 좌표).
     *
     * @return the shape (do not modify it) / 도형(바꾸지 말 것)
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Returns the bounds of the highlighted area (document coordinates).
     * <p>
     * 강조된 영역의 경계를 반환한다(문서 좌표).
     *
     * @return the bounds / 경계
     */
    public Rectangle2D getBounds() {
        return bounds;
    }

    /**
     * Returns the ARGB color of the highlight.
     * <p>
     * 강조의 ARGB 색을 반환한다.
     *
     * @return the color / 색
     */
    public int getColor() {
        return color;
    }

    /**
     * Tells whether nothing is highlighted.
     * <p>
     * 강조된 것이 없는지 알려 준다.
     *
     * @return true if the area is empty / 영역이 비어 있으면 true
     */
    public boolean isEmpty() {
        return bounds.isEmpty();
    }

    /**
     * Paints the highlight. The graphics must be in component coordinates (the page is painted with the same call: the document is scaled by
     * the zoom factor and the scroll position is the translation of the component); its state is put back when this returns.
     * <p>
     * 강조를 그린다. 그래픽스는 컴포넌트 좌표여야 한다(페이지도 같은 방식으로 그려진다: 문서를 배율로 확대하며 스크롤 위치는 컴포넌트의 이동이다).
     * 돌아올 때 그래픽스의 상태를 되돌린다.
     *
     * @param g     the graphics of the component / 컴포넌트의 그래픽스
     * @param scale the zoom factor, 1 for 100 percent / 확대 배율. 1 이면 100 %
     */
    public void paint(Graphics2D g, double scale) {
        AffineTransform saved = g.getTransform();
        Color oldColor = g.getColor();
        Object oldAa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            g.scale(scale, scale);
            g.setColor(new Color(color, true));
            // 영역을 한 번에 채워 겹치는 부분이 두 번 섞이지 않게 한다.
            g.fill(shape);
        } finally {
            g.setTransform(saved);
            g.setColor(oldColor);
            if (oldAa != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
            }
        }
    }
}
