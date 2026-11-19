package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;

/**
 * The web fonts of a page for the layout (plan item P10-05, CSS Fonts Level 3 <code>@font-face</code>): the font resolver asks it for
 * every family name of <code>font-family</code> before it looks for an installed font of that name. An implementation answers with the
 * font of a loaded <code>@font-face</code> of that family, or null if the family is not a web font family, the font is still loading
 * (the text then uses the next family, as with <code>font-display: swap</code>), failed, or does not cover the text.
 * <p>
 * 레이아웃을 위한 페이지의 웹 글꼴(계획 P10-05, CSS Fonts Level 3 <code>@font-face</code>): 글꼴 리졸버는 <code>font-family</code> 의 패밀리 이름마다 그 이름의 설치된 글꼴을
 * 찾기 전에 이것에 묻는다. 구현은 그 패밀리의 읽힌 <code>@font-face</code> 글꼴로 답하거나, 패밀리가 웹 글꼴 패밀리가 아니거나 글꼴을 아직 읽는 중이거나(그러면 텍스트는
 * <code>font-display: swap</code> 처럼 다음 패밀리를 쓴다) 실패했거나 텍스트를 덮지 못하면 null 로 답한다.
 */
public interface WebFontSource {

    /**
     * Finds the font for a family, weight and style.
     * <p>
     * 패밀리, 굵기, 스타일에 맞는 글꼴을 찾는다.
     *
     * @param family the family name of <code>font-family</code> / <code>font-family</code> 의 패밀리 이름
     * @param weight the requested weight, 100 to 900 / 요청한 굵기. 100~900
     * @param italic true for an italic or oblique style / 이탤릭이나 oblique 스타일이면 true
     * @param text   the text that the font has to show, may be null / 글꼴이 보여야 하는 텍스트. null 가능
     * @param size   the font size in pixels / 글꼴 크기(픽셀)
     * @return the font derived to the size (with a synthetic bold or italic if the face lacks the requested one), or null / 크기에 맞춘 글꼴(글꼴 면에 요청한 굵기나 이탤릭이 없으면 합성 굵게/기울임). 없으면 null
     */
    Font find(String family, int weight, boolean italic, String text, float size);

    /**
     * Finds the font for a family, weight and style, and says whether a bold or an italic that the face lacks may be synthesized
     * (<code>font-synthesis-weight</code> and <code>font-synthesis-style</code>, CSS Fonts Level 4): when it may not, the face is used as it is.
     * <p>
     * 패밀리, 굵기, 스타일에 맞는 글꼴을 찾고, 면에 없는 굵게나 기울임을 합성해도 되는지(CSS Fonts Level 4 <code>font-synthesis-weight</code>, <code>font-synthesis-style</code>)
     * 함께 받는다. 합성하면 안 되면 면을 있는 그대로 쓴다.
     *
     * @param family    the family name of <code>font-family</code> / <code>font-family</code> 의 패밀리 이름
     * @param weight    the requested weight, 100 to 900 / 요청한 굵기. 100~900
     * @param italic    true for an italic or oblique style / 이탤릭이나 oblique 스타일이면 true
     * @param text      the text that the font has to show, may be null / 글꼴이 보여야 하는 텍스트. null 가능
     * @param size      the font size in pixels / 글꼴 크기(픽셀)
     * @param synthBold true if a bold may be made up for a face that is not bold / 굵지 않은 면의 굵게를 만들어 내도 되면 true
     * @param synthItalic true if an italic may be made up for a face that is not italic / 이탤릭이 아닌 면의 기울임을 만들어 내도 되면 true
     * @return the font derived to the size, or null / 크기에 맞춘 글꼴. 없으면 null
     */
    Font find(String family, int weight, boolean italic, String text, float size, boolean synthBold, boolean synthItalic);

    /**
     * Finds a font like {@link #find(String, int, boolean, String, float, boolean, boolean)} with the glyph alternates that a style asks for
     * (<code>font-variant-alternates</code> and <code>font-feature-settings</code>, CSS Fonts 4 sections 6.8, 6.9 and 6.12).
     * <p>
     * 스타일이 요구하는 대체 글리프(<code>font-variant-alternates</code> 와 <code>font-feature-settings</code>, CSS Fonts 4 6.8, 6.9, 6.12 절)를 가진 글꼴을
     * {@link #find(String, int, boolean, String, float, boolean, boolean)} 처럼 찾는다.
     *
     * @param family      the family name / 패밀리 이름
     * @param weight      the requested weight / 요청한 굵기
     * @param italic      true for italic / 이탤릭이면 true
     * @param text        the text, may be null / 텍스트. null 가능
     * @param size        the size in px / 크기(px)
     * @param synthBold   true if a bold may be synthesized / 굵게를 합성해도 되면 true
     * @param synthItalic true if an italic may be synthesized / 기울임을 합성해도 되면 true
     * @param style       the computed style, may be null / 계산 스타일. null 가능
     * @return the font or null / 글꼴 또는 null
     */
    Font find(String family, int weight, boolean italic, String text, float size, boolean synthBold, boolean synthItalic, org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle style);

    /**
     * Tells until when a text that asks for a family must be invisible: the face that would show it is still loading and in the block period of its
     * <code>font-display</code> (CSS Fonts 4 section 4.8). The font resolver asks after {@link #find} returned null; a blocked text is laid out with the next
     * family that is available and is not painted.
     * <p>
     * 패밀리를 요청한 텍스트가 언제까지 보이지 않아야 하는지 알려 준다: 그 텍스트를 보일 면이 아직 읽는 중이고 <code>font-display</code> 의 block 기간에 있을 때이다(CSS Fonts 4
     * 4.8 절). 글꼴 리졸버는 {@link #find} 가 null 을 반환한 뒤에 묻는다. 막힌 텍스트는 쓸 수 있는 다음 패밀리로 배치하고 그리지 않는다.
     *
     * @param family the family name of <code>font-family</code> / <code>font-family</code> 의 패밀리 이름
     * @param weight the requested weight, 100 to 900 / 요청한 굵기. 100~900
     * @param italic true for an italic or oblique style / 이탤릭이나 oblique 스타일이면 true
     * @param text   the text that the font has to show, may be null / 글꼴이 보여야 하는 텍스트. null 가능
     * @return the end of the block period in milliseconds of the system clock, or 0 if the text is not blocked / block 기간의 끝(시스템 시계 ms). 막히지 않았으면 0
     */
    long blockedUntil(String family, int weight, boolean italic, String text);
}
