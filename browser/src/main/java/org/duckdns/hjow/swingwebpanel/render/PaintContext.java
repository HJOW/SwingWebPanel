package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * The state shared by the commands of one paint pass: the target graphics (already transformed to document coordinates), the visible part of
 * the document used to skip commands and the time that selects the frame of animated images.
 * <p>
 * 한 번의 그리기에서 명령들이 공유하는 상태: 대상 그래픽스(이미 문서 좌표로 변환됨), 명령을 건너뛰는 데 쓰는 문서의 보이는 영역, 애니메이션 이미지의
 * 프레임을 고르는 시각.
 */
final class PaintContext {

    /**
     * The graphics to paint on; its user space is the document space.
     * <p>
     * 그릴 대상 그래픽스. 사용자 공간이 문서 공간이다.
     */
    final Graphics2D g;

    /**
     * The visible part of the document in document coordinates.
     * <p>
     * 문서 좌표로 나타낸 문서의 보이는 영역.
     */
    final Rectangle2D clipDoc;

    /**
     * The animation time in milliseconds.
     * <p>
     * 애니메이션 시각(밀리초).
     */
    final long timeMillis;

    /**
     * The horizontal scroll offset that a fixed group has to undo (0 inside a fixed group).
     * <p>
     * fixed 그룹이 되돌려야 하는 가로 스크롤 오프셋(fixed 그룹 안에서는 0).
     */
    final int scrollX;

    /**
     * The vertical scroll offset that a fixed group has to undo (0 inside a fixed group).
     * <p>
     * fixed 그룹이 되돌려야 하는 세로 스크롤 오프셋(fixed 그룹 안에서는 0).
     */
    final int scrollY;

    /**
     * The image that the graphics paint on, if it is known: a pixel of the device space of {@link #g} is the pixel of this image at the same position. It is what
     * <code>backdrop-filter</code> reads and rewrites; null when the graphics paint on something that cannot be read (then there is no backdrop).
     * <p>
     * 그래픽스가 그리는 이미지(알 수 있을 때): {@link #g} 의 기기 공간 화소는 같은 위치의 이 이미지 화소이다. <code>backdrop-filter</code> 가 읽고 다시 쓰는 것이다.
     * 읽을 수 없는 곳에 그리면 null(그러면 배경이 없다).
     */
    final BufferedImage surface;

    /**
     * Creates a context.
     * <p>
     * 컨텍스트를 만든다.
     *
     * @param g          the graphics / 그래픽스
     * @param clipDoc    visible part in document coordinates / 문서 좌표의 보이는 영역
     * @param timeMillis animation time / 애니메이션 시각
     */
    PaintContext(Graphics2D g, Rectangle2D clipDoc, long timeMillis) {
        this(g, clipDoc, timeMillis, 0, 0);
    }

    /**
     * Creates a context that knows the scroll offset of the page.
     * <p>
     * 페이지의 스크롤 오프셋을 아는 컨텍스트를 만든다.
     *
     * @param g          the graphics / 그래픽스
     * @param clipDoc    visible part in document coordinates / 문서 좌표로 보이는 영역
     * @param timeMillis animation time / 애니메이션 시각
     * @param scrollX    horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY    vertical scroll offset / 세로 스크롤 오프셋
     */
    PaintContext(Graphics2D g, Rectangle2D clipDoc, long timeMillis, int scrollX, int scrollY) {
        this(g, clipDoc, timeMillis, scrollX, scrollY, null);
    }

    /**
     * Creates a context that also knows the image that is painted on.
     * <p>
     * 그리는 대상 이미지도 아는 컨텍스트를 만든다.
     *
     * @param g          the graphics / 그래픽스
     * @param clipDoc    visible part in document coordinates / 문서 좌표로 보이는 영역
     * @param timeMillis animation time / 애니메이션 시각
     * @param scrollX    horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY    vertical scroll offset / 세로 스크롤 오프셋
     * @param surface    the image that the graphics paint on, or null / 그래픽스가 그리는 이미지. 없으면 null
     */
    PaintContext(Graphics2D g, Rectangle2D clipDoc, long timeMillis, int scrollX, int scrollY, BufferedImage surface) {
        this(g, clipDoc, timeMillis, scrollX, scrollY, surface, null);
    }

    /**
     * Creates a context that also knows the viewport.
     * <p>
     * 뷰포트도 아는 컨텍스트를 만든다.
     *
     * @param g          the graphics / 그래픽스
     * @param clipDoc    visible part in document coordinates / 문서 좌표로 보이는 영역
     * @param timeMillis animation time / 애니메이션 시각
     * @param scrollX    horizontal offset of the viewport that a fixed group undoes / fixed 그룹이 되돌리는 뷰포트의 가로 오프셋
     * @param scrollY    vertical offset of the viewport / 뷰포트의 세로 오프셋
     * @param surface    the image that the graphics paint on, or null / 그래픽스가 그리는 이미지. 없으면 null
     * @param viewport   the viewport in the current user space, or null if it is not known (sticky boxes are then not shifted) / 지금 사용자 공간으로 나타낸 뷰포트. 모르면 null(그러면 sticky 상자는 옮기지 않는다)
     */
    PaintContext(Graphics2D g, Rectangle2D clipDoc, long timeMillis, int scrollX, int scrollY, BufferedImage surface, Rectangle2D viewport) {
        this.surface = surface;
        this.g = g;
        this.clipDoc = clipDoc;
        this.timeMillis = timeMillis;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.viewport = viewport;
    }

    /**
     * The viewport (the scrollport of the page) in the current user space, or null if it is not known: <code>position:sticky</code> boxes
     * are shifted against it (see {@link org.duckdns.hjow.swingwebpanel.layout.StickyConstraint}).
     * <p>
     * 지금 사용자 공간으로 나타낸 뷰포트(페이지의 스크롤포트). 모르면 null: <code>position:sticky</code> 상자는 이것에 대해 옮겨진다
     * ({@link org.duckdns.hjow.swingwebpanel.layout.StickyConstraint} 참조).
     */
    final Rectangle2D viewport;

    /**
     * Creates a context for the same pass on other graphics (used for opacity groups that are painted off-screen).
     * <p>
     * 같은 그리기에서 다른 그래픽스에 대한 컨텍스트를 만든다(화면 밖에 그리는 불투명도 그룹에 쓴다).
     *
     * @param other the graphics to use / 쓸 그래픽스
     * @return the new context / 새 컨텍스트
     */
    PaintContext with(Graphics2D other) {
        return new PaintContext(other, clipDoc, timeMillis, scrollX, scrollY, null, viewport);
    }

    /**
     * Creates a context for an off-screen buffer that covers another visible area (effect groups): the same time, scroll offset and
     * viewport, other graphics, visible area and image.
     * <p>
     * 다른 보이는 영역을 덮는 화면 밖 버퍼를 위한 컨텍스트를 만든다(효과 그룹): 같은 시각, 스크롤 오프셋, 뷰포트에 다른 그래픽스, 보이는 영역, 이미지.
     *
     * @param other   the graphics to use / 쓸 그래픽스
     * @param visible the visible area in document coordinates / 문서 좌표의 보이는 영역
     * @param surface the image that they paint on / 그것이 그리는 이미지
     * @return the new context / 새 컨텍스트
     */
    PaintContext with(Graphics2D other, Rectangle2D visible, BufferedImage surface) {
        return new PaintContext(other, visible, timeMillis, scrollX, scrollY, surface, viewport);
    }

    /**
     * Creates the context for the inside of a sticky group: the user space is moved by the shift, so the visible area, the viewport and the
     * offset that a fixed group has to undo are moved back by it.
     * <p>
     * sticky 그룹 안쪽의 컨텍스트를 만든다: 사용자 공간이 이동량만큼 옮겨지므로 보이는 영역, 뷰포트, fixed 그룹이 되돌려야 하는 오프셋을 그만큼 되돌린다.
     *
     * @param dx horizontal shift of the group / 그룹의 가로 이동량
     * @param dy vertical shift of the group / 그룹의 세로 이동량
     * @return the new context / 새 컨텍스트
     */
    PaintContext shifted(double dx, double dy) {
        Rectangle2D vis = new Rectangle2D.Double(clipDoc.getX() - dx, clipDoc.getY() - dy, clipDoc.getWidth(), clipDoc.getHeight());
        Rectangle2D view = viewport == null ? null : new Rectangle2D.Double(viewport.getX() - dx, viewport.getY() - dy, viewport.getWidth(), viewport.getHeight());
        return new PaintContext(g, vis, timeMillis, (int) Math.round(scrollX - dx), (int) Math.round(scrollY - dy), surface, view);
    }

    /**
     * Creates a context for the same pass on other graphics that paint on a known image (an off-screen group buffer).
     * <p>
     * 같은 그리기에서 알려진 이미지에 그리는 다른 그래픽스의 컨텍스트를 만든다(화면 밖 그룹 버퍼).
     *
     * @param other   the graphics to use / 쓸 그래픽스
     * @param surface the image that they paint on / 그것이 그리는 이미지
     * @return the new context / 새 컨텍스트
     */
    PaintContext with(Graphics2D other, BufferedImage surface) {
        return new PaintContext(other, clipDoc, timeMillis, scrollX, scrollY, surface, viewport);
    }

    /**
     * Creates the context for the inside of a fixed group: the user space is moved by the scroll offset, so the visible area in that
     * space is moved back by it, and nested fixed groups need no further correction.
     * <p>
     * fixed 그룹 안쪽의 컨텍스트를 만든다: 사용자 공간이 스크롤 오프셋만큼 옮겨지므로 그 공간에서 보이는 영역은 그만큼 되돌려지며, 중첩된 fixed 그룹은 더
     * 보정할 필요가 없다.
     *
     * @return the new context / 새 컨텍스트
     */
    /**
     * Returns a context for the inside of a transformed group: the same graphics and time with the visible area given in the local
     * (untransformed) coordinates of the group.
     * <p>
     * 변환된 그룹의 안쪽을 위한 컨텍스트를 반환한다: 같은 그래픽스와 시각, 보이는 영역은 그룹의 지역(변환 전) 좌표로 준 것.
     *
     * @param localClip the visible area in the local coordinates / 지역 좌표의 보이는 영역
     * @param inverse   the inverse of the transform of the group (maps the viewport to the local coordinates) / 그룹 변환의 역(뷰포트를 지역 좌표로 옮긴다)
     * @return the context / 컨텍스트
     */
    PaintContext withClip(Rectangle2D localClip, java.awt.geom.AffineTransform inverse) {
        Rectangle2D view = viewport == null ? null : inverse.createTransformedShape(viewport).getBounds2D();
        return new PaintContext(g, localClip, timeMillis, scrollX, scrollY, surface, view);
    }

    /**
     * Returns the context for the inside of a fixed group.
     * <p>
     * fixed 그룹의 안쪽을 위한 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    PaintContext fixedInside() {
        Rectangle2D shifted = new Rectangle2D.Double(clipDoc.getX() - scrollX, clipDoc.getY() - scrollY, clipDoc.getWidth(), clipDoc.getHeight());
        // fixed 그룹 안에서 뷰포트는 원점에 있다.
        Rectangle2D view = viewport == null ? null : new Rectangle2D.Double(0, 0, viewport.getWidth(), viewport.getHeight());
        return new PaintContext(g, shifted, timeMillis, 0, 0, surface, view);
    }
}
