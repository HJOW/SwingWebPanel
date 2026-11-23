package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.Resizing;

/**
 * The user input side of the <code>resize</code> property (CSS Basic User Interface Module Level 4, https://www.w3.org/TR/css-ui-4/#resize): a
 * press of the main mouse button on the resize handle of a box ({@link Resizing}) starts a resize, dragging the pointer sets the inline
 * <code>width</code> and <code>height</code> of the element so that its border box follows the pointer (as far as the direction of the
 * <code>resize</code> value allows), and releasing the button ends it. The size is written into the <code>style</code> attribute of the element
 * like a script would do (the page is laid out again by the normal change path, and the author's <code>min-*</code> and <code>max-*</code>
 * still limit it); a <code>box-sizing: content-box</code> box gets the border box size minus its padding and border. A press on the handle is
 * a user interface action: the page gets no <code>mousedown</code> and no <code>mouseup</code> for it. The class keeps the state of one drag per
 * component; its methods run on the document queue, except {@link #release()} and {@link #isActive()}, which may be called on any thread.
 * <p>
 * <code>resize</code> 속성의 사용자 입력 쪽(CSS Basic User Interface Module Level 4, https://www.w3.org/TR/css-ui-4/#resize): 상자의 크기 조절 핸들({@link Resizing})을 마우스
 * 주 버튼으로 누르면 크기 조절이 시작되고, 포인터를 끌면 요소의 인라인 <code>width</code>, <code>height</code> 를 정해(<code>resize</code> 값의 방향이 허용하는 만큼) border box 가
 * 포인터를 따라가며, 버튼을 놓으면 끝난다. 크기는 스크립트가 하듯이 요소의 <code>style</code> 속성에 쓴다(페이지는 일반 변경 경로로 다시 배치되며 작성자의
 * <code>min-*</code>, <code>max-*</code> 는 여전히 제한한다). <code>box-sizing: content-box</code> 상자는 border box 크기에서 padding 과 border 를 뺀 값을 얻는다. 핸들 위의
 * 누름은 사용자 인터페이스 동작이므로 페이지는 그 때문에 <code>mousedown</code> 도 <code>mouseup</code> 도 받지 않는다. 컴포넌트마다 끌기 하나의 상태를 가지며, 메서드는
 * 문서 큐에서 실행된다. {@link #release()} 와 {@link #isActive()} 는 어느 스레드에서나 부를 수 있다.
 */
final class PageResize {

    /**
     * The state of a drag: the element, the directions, and the sizes and the pointer position at the press.
     * <p>
     * 끌기의 상태: 요소, 방향, 누를 때의 크기와 포인터 위치.
     */
    private static final class Drag {
        /** The element whose size changes. 크기가 바뀌는 요소. */
        final Element element;
        /** The directions, bits of {@link Resizing#HORIZONTAL} and {@link Resizing#VERTICAL}. 방향. */
        final int directions;
        /** The document x of the pointer at the press. 누를 때 포인터의 문서 x. */
        final double pressX;
        /** The document y of the pointer at the press. 누를 때 포인터의 문서 y. */
        final double pressY;
        /** The width of the border box at the press. 누를 때 border box 의 너비. */
        final double width;
        /** The height of the border box at the press. 누를 때 border box 의 높이. */
        final double height;
        /** The padding and border of the box along the width that the value of <code>width</code> does not include. <code>width</code> 값에 포함되지 않는 가로 padding 과 border. */
        final double insetX;
        /** The same along the height. 높이에 대한 같은 값. */
        final double insetY;
        /** The last written width, or NaN. 마지막으로 쓴 너비. 없으면 NaN. */
        double lastWidth = Double.NaN;
        /** The last written height, or NaN. 마지막으로 쓴 높이. 없으면 NaN. */
        double lastHeight = Double.NaN;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param element    the element / 요소
         * @param directions the directions / 방향
         * @param pressX     pointer x / 포인터 x
         * @param pressY     pointer y / 포인터 y
         * @param box        the box at the press / 누를 때의 상자
         */
        Drag(Element element, int directions, double pressX, double pressY, LayoutBox box) {
            this.element = element;
            this.directions = directions;
            this.pressX = pressX;
            this.pressY = pressY;
            this.width = box.getWidth();
            this.height = box.getHeight();
            this.insetX = Resizing.insets(box, true);
            this.insetY = Resizing.insets(box, false);
        }
    }

    /** The drag that is running, or null. 진행 중인 끌기. 없으면 null. */
    private volatile Drag drag;

    /**
     * Creates the state holder.
     * <p>
     * 상태 보관자를 만든다.
     */
    PageResize() {
    }

    /**
     * Tells whether a drag is running.
     * <p>
     * 끌기가 진행 중인지 알려 준다.
     *
     * @return true during a resize / 크기 조절 중이면 true
     */
    boolean isActive() {
        return drag != null;
    }

    /**
     * Ends a drag (the button was released, or the page went away).
     * <p>
     * 끌기를 끝낸다(버튼을 놓았거나 페이지가 사라졌다).
     */
    void release() {
        drag = null;
    }

    /**
     * Starts a resize if the press is on a resize handle.
     * <p>
     * 누른 곳이 크기 조절 핸들이면 크기 조절을 시작한다.
     *
     * @param loc     what was hit / 맞은 것
     * @param pointer the press / 누름
     * @return true if the press belongs to a handle (so the page gets no mouse event and no text selection starts) / 누름이 핸들의 것이면(페이지가 마우스 이벤트를 받지 않고
     *         텍스트 선택도 시작하지 않으면) true
     */
    boolean press(PageInput.Located loc, PageInput.Pointer pointer) {
        LayoutBox hit = loc.hit == null ? null : loc.hit.getBox();
        LayoutBox box = Resizing.find(hit, loc.x, loc.y);
        Node node = box == null ? null : box.getNode();
        if (!(node instanceof Element)) {
            return false;
        }
        drag = new Drag((Element) node, Resizing.directions(box), pointer.docX, pointer.docY, box);
        return true;
    }

    /**
     * Applies the position of the pointer to the size of the element: writes <code>width</code> and <code>height</code> into its
     * <code>style</code> attribute.
     * <p>
     * 포인터 위치를 요소의 크기에 적용한다: <code>width</code> 와 <code>height</code> 를 <code>style</code> 속성에 쓴다.
     *
     * @param pointer the pointer / 포인터
     */
    void dragged(PageInput.Pointer pointer) {
        Drag d = drag;
        if (d == null) {
            return;
        }
        double w = Math.max(0, Math.round(d.width + (pointer.docX - d.pressX)));
        double h = Math.max(0, Math.round(d.height + (pointer.docY - d.pressY)));
        boolean horizontal = (d.directions & Resizing.HORIZONTAL) != 0;
        boolean vertical = (d.directions & Resizing.VERTICAL) != 0;
        // 방향이 허용하지 않는 축은 바꾸지 않고, 값이 마지막에 쓴 것과 같으면 다시 쓰지 않는다.
        boolean changeW = horizontal && w != d.lastWidth;
        boolean changeH = vertical && h != d.lastHeight;
        if (!changeW && !changeH) {
            return;
        }
        Document doc = d.element.document();
        String text = d.element.getAttribute("style");
        boolean quirks = doc.getCompatibilityMode() == Document.QUIRKS_MODE;
        CssStyleDeclaration style = CssStyleDeclaration.parse(text == null ? "" : text, doc.getBaseUri(), new CssDiagnostics(), quirks);
        if (changeW) {
            style.setProperty("width", length(Math.max(0, w - d.insetX)), "");
            d.lastWidth = w;
        }
        if (changeH) {
            style.setProperty("height", length(Math.max(0, h - d.insetY)), "");
            d.lastHeight = h;
        }
        d.element.setAttribute("style", style.getCssText());
    }

    /**
     * Formats a size in CSS pixels.
     * <p>
     * 크기를 CSS 픽셀로 포맷한다.
     *
     * @param px the size / 크기
     * @return the length text / 길이 텍스트
     */
    private static String length(double px) {
        return (long) px + "px";
    }

    /**
     * Returns the keyword of the cursor that the pointer shows over a resize handle.
     * <p>
     * 포인터가 크기 조절 핸들 위에 있을 때 보이는 커서의 키워드를 반환한다.
     *
     * @param loc what is under the pointer / 포인터 아래의 것
     * @return <code>se-resize</code>, <code>e-resize</code> or <code>s-resize</code>, or null if the pointer is not on a handle / 포인터가 핸들 위가 아니면 null
     */
    static String cursorAt(PageInput.Located loc) {
        LayoutBox hit = loc.hit == null ? null : loc.hit.getBox();
        LayoutBox box = Resizing.find(hit, loc.x, loc.y);
        if (box == null) {
            return null;
        }
        int dirs = Resizing.directions(box);
        if (dirs == Resizing.HORIZONTAL) {
            return "e-resize";
        }
        return dirs == Resizing.VERTICAL ? "s-resize" : "se-resize";
    }
}
