package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The display command of a nested browsing context: it paints the page of a frame inside the content box of the frame element, clipped to
 * the box and moved by the scroll offset of the frame, and then the overlay scrollbars. The page of the frame is itself an immutable display
 * list, so frames nest to any depth and are painted with the same painter as the top-level page.
 * <p>
 * 중첩된 브라우징 문맥의 디스플레이 명령: 프레임 요소의 콘텐츠 상자 안에 프레임의 페이지를 상자로 클립하고 프레임의 스크롤 오프셋만큼 옮겨 그린 뒤
 * 오버레이 스크롤바를 그린다. 프레임의 페이지는 그 자체가 불변 디스플레이 리스트이므로 프레임은 얼마든지 중첩되며 최상위 페이지와 같은 그리기 도구로 그려진다.
 */
final class FrameCommand extends DisplayCommand {

    /**
     * The picture of the frame.
     * <p>
     * 프레임의 그림.
     */
    private final FramePicture picture;

    /**
     * Creates the command for the content box of a frame element.
     * <p>
     * 프레임 요소의 콘텐츠 상자에 대한 명령을 만든다.
     *
     * @param x       left of the box / 상자의 왼쪽
     * @param y       top of the box / 상자의 위쪽
     * @param w       width of the box / 상자의 너비
     * @param h       height of the box / 상자의 높이
     * @param picture the picture of the frame / 프레임의 그림
     * @param node    the frame element / 프레임 요소
     */
    FrameCommand(double x, double y, double w, double h, FramePicture picture, Node node) {
        super(Type.FRAME, x, y, w, h, node);
        this.picture = picture;
    }

    /**
     * Returns the picture of the frame.
     * <p>
     * 프레임의 그림을 반환한다.
     *
     * @return the picture / 그림
     */
    FramePicture getPicture() {
        return picture;
    }

    /**
     * Paints the page of the frame and the scrollbars.
     * <p>
     * 프레임의 페이지와 스크롤바를 그린다.
     *
     * @param ctx the paint context / 그리기 컨텍스트
     */
    void paint(PaintContext ctx) {
        Rectangle2D box = getBounds();
        Graphics2D g = (Graphics2D) ctx.g.create();
        try {
            g.translate(box.getX(), box.getY());
            g.clip(new Rectangle2D.Double(0, 0, box.getWidth(), box.getHeight()));
            RenderedPage page = picture.getPage();
            // 문서가 아직 없는 프레임은 아무것도 그리지 않는다(투명).
            if (page != null) {
                Rectangle area = g.getClipBounds();
                if (area != null) {
                    PagePainter.paint(g, page.getDisplayList(), area, picture.getScrollX(), picture.getScrollY(), 1.0, ctx.timeMillis);
                }
            }
            paintScrollbars(g, box.getWidth(), box.getHeight(), page);
        } finally {
            g.dispose();
        }
    }

    /**
     * Paints the overlay scrollbars: a translucent track along the right and bottom edge and a thumb that shows the visible part.
     * <p>
     * 오버레이 스크롤바를 그린다: 오른쪽과 아래 가장자리를 따라 반투명 트랙과, 보이는 부분을 나타내는 썸.
     *
     * @param g    the graphics with the origin at the content box / 원점이 콘텐츠 상자인 그래픽스
     * @param w    the width of the box / 상자의 너비
     * @param h    the height of the box / 상자의 높이
     * @param page the page of the frame or null / 프레임의 페이지 또는 null
     */
    private void paintScrollbars(Graphics2D g, double w, double h, RenderedPage page) {
        if (page == null) {
            return;
        }
        int bar = FrameScroll.BAR;
        Color track = new Color(0, 0, 0, 24);
        Color thumb = new Color(0, 0, 0, 120);
        if (picture.hasVerticalBar()) {
            double length = h - (picture.hasHorizontalBar() ? bar : 0);
            double[] t = FrameScroll.thumb(length, page.getPreferredHeight(), picture.getScrollY());
            g.setColor(track);
            g.fill(new Rectangle2D.Double(w - bar, 0, bar, length));
            g.setColor(thumb);
            g.fill(new Rectangle2D.Double(w - bar + 2, t[0], bar - 4, t[1]));
        }
        if (picture.hasHorizontalBar()) {
            double length = w - (picture.hasVerticalBar() ? bar : 0);
            double[] t = FrameScroll.thumb(length, page.getPreferredWidth(), picture.getScrollX());
            g.setColor(track);
            g.fill(new Rectangle2D.Double(0, h - bar, length, bar));
            g.setColor(thumb);
            g.fill(new Rectangle2D.Double(t[0], h - bar + 2, t[1], bar - 4));
        }
    }
}
