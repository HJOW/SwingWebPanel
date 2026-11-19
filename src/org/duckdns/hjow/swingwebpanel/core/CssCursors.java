package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Maps the CSS <code>cursor</code> property (CSS UI Level 3, <a href="https://www.w3.org/TR/css-ui-3/#cursor">https://www.w3.org/TR/css-ui-3/#cursor</a>) to the Swing cursor of the
 * component. The keyword at the end of the property value (the fallback after any <code>url()</code> images, which this engine does not use as cursor images) decides:
 * <code>auto</code> keeps the behavior of the component (the hand over a link, the text cursor over selectable text, else the default), the other keywords map to the predefined
 * cursors of Java ({@link Cursor}); keywords that Java 6 has no cursor for (<code>help</code>, <code>not-allowed</code>, <code>zoom-in</code> and others) give the default arrow,
 * and <code>none</code> gives a fully transparent cursor.
 * <p>
 * Usage: <code>component.setCursor(CssCursors.cursorFor(CssCursors.nameOf(style), overLink, overText))</code>.
 * <p>
 * CSS <code>cursor</code> 속성(CSS UI Level 3)을 컴포넌트의 Swing 커서로 바꾼다. 속성 값 끝의 키워드(<code>url()</code> 이미지 뒤의 대체값. 이 엔진은 커서 이미지를 쓰지 않는다)가
 * 정한다: <code>auto</code> 는 컴포넌트의 기존 동작(링크 위에서는 손, 선택할 수 있는 텍스트 위에서는 텍스트 커서, 그 밖에는 기본)을 유지하고, 다른 키워드는 Java 의 미리 정의된 커서
 * ({@link Cursor})로 바뀌며, Java 6 에 커서가 없는 키워드(<code>help</code>, <code>not-allowed</code>, <code>zoom-in</code> 등)는 기본 화살표이고, <code>none</code> 은 완전히 투명한 커서이다.
 * <p>
 * 사용법: <code>component.setCursor(CssCursors.cursorFor(CssCursors.nameOf(style), overLink, overText))</code>.
 */
final class CssCursors {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CssCursors() {
    }

    /**
     * Returns the keyword of the <code>cursor</code> property of a style: the last item of a list (after the images), <code>auto</code> if there is no style or value.
     * <p>
     * 스타일의 <code>cursor</code> 속성의 키워드를 반환한다: 목록이면 마지막 항목(이미지 뒤). 스타일이나 값이 없으면 <code>auto</code>.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the keyword in lower case / 소문자 키워드
     */
    static String nameOf(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.CURSOR);
        if (v instanceof CssList && ((CssList) v).size() > 0) {
            v = ((CssList) v).get(((CssList) v).size() - 1);
        }
        return v instanceof CssKeyword ? ((CssKeyword) v).getName().toLowerCase(java.util.Locale.ENGLISH) : "auto";
    }

    /**
     * Returns the cursor for a keyword of the <code>cursor</code> property.
     * <p>
     * <code>cursor</code> 속성의 키워드에 맞는 커서를 반환한다.
     *
     * @param name the keyword from {@link #nameOf} / {@link #nameOf} 의 키워드
     * @param link true while the pointer is over a link (the <code>auto</code> cursor is the hand) / 포인터가 링크 위에 있으면 true(<code>auto</code> 커서는 손)
     * @param text true while the pointer is over selectable text (the <code>auto</code> cursor is the text cursor) / 포인터가 선택할 수 있는 텍스트 위에 있으면 true(<code>auto</code> 커서는 텍스트 커서)
     * @return the cursor / 커서
     */
    static Cursor cursorFor(String name, boolean link, boolean text) {
        // 키워드마다 Java 의 미리 정의된 커서로 대응시킨다.
        if (name == null || "auto".equals(name)) {
            return Cursor.getPredefinedCursor(link ? Cursor.HAND_CURSOR : (text ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
        }
        if ("pointer".equals(name) || "grab".equals(name) || "grabbing".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
        if ("text".equals(name) || "vertical-text".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        }
        if ("wait".equals(name) || "progress".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        }
        if ("crosshair".equals(name) || "cell".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        if ("move".equals(name) || "all-scroll".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
        if ("e-resize".equals(name) || "ew-resize".equals(name) || "col-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        if ("w-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        }
        if ("n-resize".equals(name) || "ns-resize".equals(name) || "row-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        }
        if ("s-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        }
        if ("ne-resize".equals(name) || "nesw-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        }
        if ("nw-resize".equals(name) || "nwse-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        }
        if ("se-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
        }
        if ("sw-resize".equals(name)) {
            return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        }
        if ("none".equals(name)) {
            Cursor c = transparentCursor();
            return c != null ? c : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        // default, help, context-menu, alias, copy, no-drop, not-allowed, zoom-in, zoom-out: Java 6 에 같은 커서가 없어 기본 화살표를 쓴다.
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    /**
     * Returns the fully transparent cursor of <code>cursor: none</code>, or null if the toolkit cannot make a custom cursor (a headless environment).
     * <p>
     * <code>cursor: none</code> 의 완전히 투명한 커서를 반환한다. 툴킷이 사용자 정의 커서를 만들 수 없으면(헤드리스 환경) null.
     *
     * @return the cursor or null / 커서. 없으면 null
     */
    private static Cursor transparentCursor() {
        try {
            return Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "swingwebpanel-none");
        } catch (RuntimeException e) {
            // 헤드리스 환경 등에서 만들 수 없으면 기본 커서를 쓴다.
            return null;
        }
    }
}
