package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Shape;

import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgReference;

/**
 * The start command of an effect group (plan item P10-08, CSS Filter Effects 1 and CSS Masking 1): everything up to the matching end
 * command is painted into a buffer of its own, the filter functions and then the mask are applied to the buffer, and the buffer is drawn
 * onto the page. The bounds of the command are the bounds of the content including the area that the filter functions may reach (blurs
 * and shadows make the painted area larger than the box); {@link #getExtent} tells how far, so that the painter can paint the content
 * outside of the visible area that is blurred into it.
 * <p>
 * 효과 그룹의 시작 명령(계획 P10-08, CSS Filter Effects 1, CSS Masking 1): 짝이 되는 끝 명령까지의 모든 것을 자신의 버퍼에 그리고, 버퍼에 필터 함수와 마스크를 적용한 뒤 버퍼를
 * 페이지에 그린다. 명령의 영역은 필터 함수가 닿을 수 있는 영역을 포함한 내용의 영역이다(흐림과 그림자는 그려지는 영역을 상자보다 넓힌다). {@link #getExtent} 는 얼마나
 * 먼지 알려 주어, 그리는 쪽이 보이는 영역 밖에 있어도 그 안으로 번지는 내용을 그릴 수 있게 한다.
 */
final class EffectGroup extends DisplayCommand {

    /** The filter functions, or null. 필터 함수. 없으면 null. */
    private final FilterSpec filter;

    /** The mask, or null. 마스크. 없으면 null. */
    private final MaskLayers mask;

    /** How far the effects reach beyond the content: {left, top, right, bottom}. 효과가 내용 밖으로 닿는 거리. */
    private final double[] extent;

    /** The border box of the element (the user space of the SVG references), or null. 요소의 border box(SVG 참조의 사용자 공간). 없으면 null. */
    private final java.awt.geom.Rectangle2D box;

    /** The definitions of the clip path reference, or null. 클립 경로 참조의 정의. 없으면 null. */
    private final SvgReference clipRef;

    /** The id of the referenced <code>clipPath</code> element, or null. 참조한 <code>clipPath</code> 요소의 id. 없으면 null. */
    private final String clipId;

    /** The filter functions of <code>backdrop-filter</code>, or null. <code>backdrop-filter</code> 의 필터 함수. 없으면 null. */
    private final FilterSpec backdrop;

    /** The border box of the element (with the rounded corners) in document coordinates, the area that the backdrop filter changes. 요소의 border box(둥근 모서리 포함, 문서 좌표). 배경 필터가 바꾸는 영역. */
    private final Shape backdropShape;

    /**
     * Creates the command.
     * <p>
     * 명령을 만든다.
     *
     * @param x      left of the area / 영역의 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 너비
     * @param h      height / 높이
     * @param filter the filter functions or null / 필터 함수 또는 null
     * @param mask   the mask or null / 마스크 또는 null
     * @param node   the DOM node or null / DOM 노드 또는 null
     */
    EffectGroup(double x, double y, double w, double h, FilterSpec filter, MaskLayers mask, Node node) {
        this(x, y, w, h, filter, mask, null, null, node);
    }

    /**
     * Creates the command of a box that also filters its backdrop.
     * <p>
     * 자신의 배경도 필터링하는 상자의 명령을 만든다.
     *
     * @param x             left of the area / 영역의 왼쪽
     * @param y             top / 위쪽
     * @param w             width / 너비
     * @param h             height / 높이
     * @param filter        the filter functions or null / 필터 함수 또는 null
     * @param mask          the mask or null / 마스크 또는 null
     * @param backdrop      the filter functions of <code>backdrop-filter</code> or null / <code>backdrop-filter</code> 의 필터 함수 또는 null
     * @param backdropShape the border box that the backdrop filter changes, or null / 배경 필터가 바꾸는 border box. 없으면 null
     * @param node          the DOM node or null / DOM 노드 또는 null
     */
    EffectGroup(double x, double y, double w, double h, FilterSpec filter, MaskLayers mask, FilterSpec backdrop, Shape backdropShape, Node node) {
        this(x, y, w, h, filter, mask, backdrop, backdropShape, null, null, null, node);
    }

    /**
     * Creates the command of a box with references to SVG elements: the filter may have <code>url(#id)</code> functions, and the box may be clipped
     * by an SVG <code>clipPath</code> element. Their user space is the border box, so the box is part of the command.
     * <p>
     * SVG 요소 참조가 있는 상자의 명령을 만든다: 필터에 <code>url(#id)</code> 함수가 있을 수 있고 SVG <code>clipPath</code> 요소로 상자를 자를 수 있다. 이들의 사용자
     * 공간은 border box 이므로 상자가 명령의 일부이다.
     *
     * @param x             left of the area / 영역의 왼쪽
     * @param y             top / 위쪽
     * @param w             width / 너비
     * @param h             height / 높이
     * @param filter        the filter functions (the references resolved) or null / 필터 함수(참조를 푼 것) 또는 null
     * @param mask          the mask or null / 마스크 또는 null
     * @param backdrop      the filter functions of <code>backdrop-filter</code> or null / <code>backdrop-filter</code> 의 필터 함수 또는 null
     * @param backdropShape the border box that the backdrop filter changes, or null / 배경 필터가 바꾸는 border box. 없으면 null
     * @param box           the border box of the element in document coordinates, or null if there are no references / 요소의 border box(문서 좌표). 참조가 없으면 null
     * @param clipRef       the definitions of the clip path reference, or null / 클립 경로 참조의 정의. 없으면 null
     * @param clipId        the id of the <code>clipPath</code> element, or null / <code>clipPath</code> 요소의 id. 없으면 null
     * @param node          the DOM node or null / DOM 노드 또는 null
     */
    EffectGroup(double x, double y, double w, double h, FilterSpec filter, MaskLayers mask, FilterSpec backdrop, Shape backdropShape, java.awt.geom.Rectangle2D box,
            SvgReference clipRef, String clipId, Node node) {
        super(Type.EFFECT_START, x, y, w, h, node);
        this.backdrop = backdrop;
        this.backdropShape = backdropShape;
        this.filter = filter;
        this.mask = mask;
        this.box = box;
        this.clipRef = clipRef;
        this.clipId = clipId;
        this.extent = filter == null ? new double[4] : filter.extent();
    }

    /**
     * Returns the border box of the element in document coordinates.
     * <p>
     * 요소의 border box 를 문서 좌표로 반환한다.
     *
     * @return the box, or null if the command has no references to SVG elements / 상자. SVG 요소 참조가 없으면 null
     */
    java.awt.geom.Rectangle2D getBox() {
        return box;
    }

    /**
     * Returns the definitions of the <code>clip-path: url(#id)</code> reference.
     * <p>
     * <code>clip-path: url(#id)</code> 참조의 정의를 반환한다.
     *
     * @return the definitions or null / 정의. 없으면 null
     */
    SvgReference getClipReference() {
        return clipRef;
    }

    /**
     * Returns the id of the referenced <code>clipPath</code> element.
     * <p>
     * 참조한 <code>clipPath</code> 요소의 id 를 반환한다.
     *
     * @return the id or null / id. 없으면 null
     */
    String getClipId() {
        return clipId;
    }

    /**
     * Returns the filter functions.
     * <p>
     * 필터 함수를 반환한다.
     *
     * @return the filter functions or null / 필터 함수. 없으면 null
     */
    FilterSpec getFilter() {
        return filter;
    }

    /**
     * Returns the filter functions of <code>backdrop-filter</code>.
     * <p>
     * <code>backdrop-filter</code> 의 필터 함수를 반환한다.
     *
     * @return the filter functions or null / 필터 함수. 없으면 null
     */
    FilterSpec getBackdrop() {
        return backdrop;
    }

    /**
     * Returns the border box that the backdrop filter changes.
     * <p>
     * 배경 필터가 바꾸는 border box 를 반환한다.
     *
     * @return the shape in document coordinates, or null / 문서 좌표의 모양. 없으면 null
     */
    Shape getBackdropShape() {
        return backdropShape;
    }

    /**
     * Returns the mask.
     * <p>
     * 마스크를 반환한다.
     *
     * @return the mask or null / 마스크. 없으면 null
     */
    MaskLayers getMask() {
        return mask;
    }

    /**
     * Returns how far the filter functions reach beyond the content.
     * <p>
     * 필터 함수가 내용 밖으로 닿는 거리를 반환한다.
     *
     * @return {left, top, right, bottom} in document px (a copy) / 문서 px 의 {왼쪽, 위쪽, 오른쪽, 아래쪽}(사본)
     */
    double[] getExtent() {
        return extent.clone();
    }

    /**
     * Paints nothing (the painter handles groups).
     * <p>
     * 아무것도 그리지 않는다(그룹은 그리는 쪽이 처리한다).
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        // 그룹 명령은 PagePainter 가 구조적으로 처리하므로 여기서 할 일이 없다.
    }
}
