package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Small helpers for the properties that paint a stacking context as one group: <code>filter</code> (CSS Filter Effects 1),
 * <code>mix-blend-mode</code> and <code>isolation</code> (CSS Compositing and Blending Level 1, https://www.w3.org/TR/compositing-1/) and the
 * mask properties (CSS Masking Level 1). Such a box is a stacking context; a box with a filter is also the containing block of its
 * absolutely positioned and fixed descendants (Filter Effects 1 section 5.1).
 * <p>
 * 쌓임 맥락을 한 그룹으로 그리게 하는 속성을 위한 작은 도우미: <code>filter</code>(CSS Filter Effects 1), <code>mix-blend-mode</code>, <code>isolation</code>
 * (CSS Compositing and Blending Level 1, https://www.w3.org/TR/compositing-1/), 마스크 속성(CSS Masking Level 1). 그런 상자는 쌓임 맥락이며, 필터가 있는 상자는
 * 자신의 절대 배치와 fixed 자손의 포함 블록이기도 하다(Filter Effects 1 5.1 절).
 */
public final class Effects {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Effects() {
    }

    /**
     * Returns the blend mode of a style: the keyword of <code>mix-blend-mode</code>.
     * <p>
     * 스타일의 혼합 모드를 반환한다: <code>mix-blend-mode</code> 의 키워드.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the keyword, <code>normal</code> when there is none / 키워드. 없으면 <code>normal</code>
     */
    public static String blendMode(ComputedStyle s) {
        if (s == null) {
            return "normal";
        }
        CssValue v = s.get(Prop.MIX_BLEND_MODE);
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : "normal";
    }

    /**
     * Tells whether a style blends its box with what is behind it (<code>mix-blend-mode</code> other than <code>normal</code>).
     * <p>
     * 스타일이 상자를 뒤에 있는 것과 혼합하는지(<code>normal</code> 이 아닌 <code>mix-blend-mode</code>) 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box blends / 혼합하면 true
     */
    public static boolean blends(ComputedStyle s) {
        return !"normal".equals(blendMode(s));
    }

    /**
     * Tells whether a style isolates its box (<code>isolation: isolate</code>).
     * <p>
     * 스타일이 상자를 격리하는지(<code>isolation: isolate</code>) 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if isolated / 격리하면 true
     */
    public static boolean isolates(ComputedStyle s) {
        return s != null && s.isKeyword(Prop.ISOLATION, "isolate");
    }

    /**
     * Tells whether a style has a mask (a <code>mask-image</code> other than <code>none</code>, or a usable <code>mask-border-source</code>).
     * <p>
     * 스타일에 마스크가 있는지(<code>none</code> 이 아닌 <code>mask-image</code>) 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box has a mask / 상자에 마스크가 있으면 true
     */
    public static boolean hasMask(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        // mask-border-source 가 none 이 아니면 마스크 테두리가 있다.
        CssValue border = s.get(Prop.MASK_BORDER_SOURCE);
        if (border != null && !border.isKeyword("none")) {
            return true;
        }
        CssValue v = s.get(Prop.MASK_IMAGE);
        if (v instanceof CssList) {
            // 쉼표 목록이면 none 이 아닌 레이어가 하나라도 있는지 본다.
            List<CssValue> items = ((CssList) v).getItems();
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isKeyword("none")) {
                    return true;
                }
            }
            return false;
        }
        return v != null && !v.isKeyword("none");
    }

    /**
     * Tells whether the effects of a style make its box a stacking context: a filter, a blend mode, <code>isolation: isolate</code> or a
     * mask.
     * <p>
     * 스타일의 효과가 상자를 쌓임 맥락으로 만드는지 알려 준다: 필터, 혼합 모드, <code>isolation: isolate</code>, 마스크.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box is a stacking context because of them / 이것들 때문에 쌓임 맥락이면 true
     */
    public static boolean makesStackingContext(ComputedStyle s) {
        return FilterSpec.has(s) || FilterSpec.hasBackdrop(s) || blends(s) || isolates(s) || hasMask(s);
    }
}
