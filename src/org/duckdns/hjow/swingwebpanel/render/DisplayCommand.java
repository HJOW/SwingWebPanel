package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Rectangle2D;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * One drawing command of a {@link DisplayList} (plan items P09-04, P09-05). A command knows the area it paints in document coordinates (so
 * partial repaints skip commands outside the clip) and the DOM node it belongs to (for diagnostics). Commands are immutable after the
 * builder has finished and can be painted by any number of threads one after another; group commands ({@link Type#CLIP_START},
 * {@link Type#OPACITY_START}) carry the index of their matching end command so the painter can skip a whole group at once. Only this package
 * creates commands.
 * <p>
 * {@link DisplayList} 의 그리기 명령 하나(계획 P09-04, P09-05). 명령은 자신이 그리는 영역을 문서 좌표로 알고(부분 다시 그리기가 클립 밖의 명령을 건너뛰도록)
 * 속한 DOM 노드를 안다(진단용). 만들기가 끝난 뒤 명령은 불변이며 여러 스레드가 차례로 그릴 수 있다. 그룹 명령({@link Type#CLIP_START},
 * {@link Type#OPACITY_START})은 짝이 되는 끝 명령의 위치를 가지므로 그리는 쪽이 그룹 전체를 한 번에 건너뛸 수 있다. 명령은 이 패키지만 만든다.
 */
public abstract class DisplayCommand {

    /**
     * The kind of a command.
     * <p>
     * 명령의 종류.
     */
    public enum Type {
        /** A filled rectangle (background color). 채운 사각형(배경색). */
        FILL_RECT,
        /** A tiled or single background image. 타일 또는 단일 배경 이미지. */
        BACKGROUND_IMAGE,
        /** The border of a box. 상자의 경계선. */
        BORDER,
        /** A text fragment with its decorations. 장식을 포함한 텍스트 조각. */
        TEXT,
        /** A decoded image (a frame of it). 디코딩된 이미지(의 프레임). */
        IMAGE,
        /** A <code>box-shadow</code> (outer or inset, blurred). <code>box-shadow</code> 그림자(바깥 또는 안쪽, 흐림 포함). */
        SHADOW,
        /** The replacement drawing of an image, audio or video that cannot be shown. 보여 줄 수 없는 이미지/오디오/비디오의 대체 그림. */
        PLACEHOLDER,
        /** A form control (text field, check box, button, select, progress ...). 폼 컨트롤(텍스트 필드, 체크박스, 단추, select, progress 등). */
        CONTROL,
        /** A nested browsing context (iframe, frame) painted inside its content box. 콘텐츠 상자 안에 그리는 중첩된 브라우징 문맥(iframe, frame). */
        FRAME,
        /** A list marker shape (disc, circle, square). 목록 표식 모양(disc, circle, square). */
        MARKER_SHAPE,
        /** The start of a clipped group. 잘린 그룹의 시작. */
        CLIP_START,
        /** The end of a clipped group. 잘린 그룹의 끝. */
        CLIP_END,
        /** The start of a group that is painted with an opacity. 불투명도를 적용해 그리는 그룹의 시작. */
        OPACITY_START,
        /** The end of an opacity group. 불투명도 그룹의 끝. */
        OPACITY_END,
        /** The start of the group of a fixed box: its coordinates are those at scroll offset 0 and are not scrolled. fixed 상자 그룹의 시작. 좌표는 스크롤 오프셋 0 의 것이며 스크롤되지 않는다. */
        FIXED_START,
        /** The end of a fixed group. fixed 그룹의 끝. */
        FIXED_END,
        /** The start of the group of a sticky box: it is shifted by an amount that depends on the scroll position (position:sticky). sticky 상자 그룹의 시작. 스크롤 위치에 따라 정해지는 만큼 옮겨진다(position:sticky). */
        STICKY_START,
        /** The end of a sticky group. sticky 그룹의 끝. */
        STICKY_END,
        /** The start of a group that is painted with a 2D transform (P10-06); its bounds are those of the transformed content. 2D 변환을 적용해 그리는 그룹의 시작(P10-06). 영역은 변환된 내용의 것이다. */
        TRANSFORM_START,
        /** The end of a transform group. 변환 그룹의 끝. */
        TRANSFORM_END,
        /** The start of a group that is painted into a buffer with the filter and the mask of its box (P10-08). 상자의 필터와 마스크를 입히는 버퍼에 그리는 그룹의 시작(P10-08). */
        EFFECT_START,
        /** The end of an effect group. 효과 그룹의 끝. */
        EFFECT_END,
        /** The emphasis marks (text-emphasis) of a text fragment. 텍스트 조각의 강조점(text-emphasis). */
        EMPHASIS,
        /** The resize handle of a box with <code>resize</code> (two diagonal grip lines in the corner of its padding box). <code>resize</code> 가 있는 상자의 크기 조절 핸들(padding box 모서리의 대각선 줄 둘). */
        RESIZER
    }

    /**
     * The kind.
     * <p>
     * 종류.
     */
    private final Type type;

    /**
     * Left edge of the painted area.
     * <p>
     * 그리는 영역의 왼쪽.
     */
    private final double x;

    /**
     * Top edge of the painted area.
     * <p>
     * 그리는 영역의 위쪽.
     */
    private final double y;

    /**
     * Width of the painted area.
     * <p>
     * 그리는 영역의 너비.
     */
    private final double width;

    /**
     * Height of the painted area.
     * <p>
     * 그리는 영역의 높이.
     */
    private final double height;

    /**
     * The DOM node the command belongs to or null.
     * <p>
     * 명령이 속한 DOM 노드 또는 null.
     */
    private final Node node;

    /**
     * Index of the matching end command for group starts, otherwise -1.
     * <p>
     * 그룹 시작 명령의 짝이 되는 끝 명령의 위치. 아니면 -1.
     */
    int groupEnd = -1;

    /**
     * Creates a command.
     * <p>
     * 명령을 만든다.
     *
     * @param type   the kind / 종류
     * @param x      left of the painted area / 그리는 영역의 왼쪽
     * @param y      top / 위쪽
     * @param width  width / 너비
     * @param height height / 높이
     * @param node   the DOM node or null / DOM 노드 또는 null
     */
    DisplayCommand(Type type, double x, double y, double width, double height, Node node) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.node = node;
    }

    /**
     * Paints the command.
     * <p>
     * 명령을 그린다.
     *
     * @param ctx the paint context / 그리기 컨텍스트
     */
    abstract void paint(PaintContext ctx);

    /**
     * Returns the kind of the command.
     * <p>
     * 명령의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the area that the command paints, in document coordinates.
     * <p>
     * 명령이 그리는 영역을 문서 좌표로 반환한다.
     *
     * @return a new rectangle / 새 사각형
     */
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Returns the shape that a clip group start clips to: its bounds, or the rounded shape of a box with <code>border-radius</code>.
     * <p>
     * 클립 그룹 시작이 자르는 도형을 반환한다: 경계, 또는 <code>border-radius</code> 가 있는 상자의 둥근 도형.
     *
     * @return the shape in document coordinates / 문서 좌표의 도형
     */
    java.awt.Shape getClipShape() {
        return getBounds();
    }

    /**
     * Returns the DOM node the command belongs to.
     * <p>
     * 명령이 속한 DOM 노드를 반환한다.
     *
     * @return the node or null / 노드 또는 null
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns the index of the matching end command of a group start.
     * <p>
     * 그룹 시작 명령의 짝이 되는 끝 명령의 위치를 반환한다.
     *
     * @return the index, or -1 for other commands / 위치. 다른 명령이면 -1
     */
    public int getGroupEnd() {
        return groupEnd;
    }

    /**
     * Tells whether the painted area meets a rectangle (touching edges do not count).
     * <p>
     * 그리는 영역이 사각형과 만나는지 알려 준다(맞닿기만 하는 것은 제외).
     *
     * @param r the rectangle in document coordinates / 문서 좌표의 사각형
     * @return true if they overlap / 겹치면 true
     */
    boolean intersects(Rectangle2D r) {
        return x < r.getMaxX() && x + width > r.getMinX() && y < r.getMaxY() && y + height > r.getMinY();
    }

    /**
     * Returns a short description for diagnostics.
     * <p>
     * 진단용 짧은 설명을 반환한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return type + "[" + x + "," + y + " " + width + "x" + height + "]";
    }
}
