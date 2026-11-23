package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The drop cap of <code>initial-letter</code> (CSS Inline Layout Module Level 3, <a href="https://www.w3.org/TR/css-inline-3/#initial-letter-styling">https://www.w3.org/TR/css-inline-3/#initial-letter-styling</a>):
 * <code>::first-letter { initial-letter: 3 }</code> makes the first letter as large as <code>size</code> lines of the paragraph, and the text of the first
 * <code>sink</code> lines flows beside it. This engine makes the letter a left float, which the layout already has for a drop cap, and gives it the font size, the line height
 * and the top margin that the specification asks of the initial letter box: the cap height of the letter is <code>(size - 1) * line height + the cap height of the
 * paragraph</code>, the box is <code>sink</code> lines high, and the top of its capital letters is on the top of the capital letters of the first line.
 * <p>
 * <code>initial-letter</code> 의 드롭 캡(CSS Inline Layout Module Level 3): <code>::first-letter { initial-letter: 3 }</code> 는 첫 글자를 문단의 <code>size</code> 줄 크기로 키우고 첫
 * <code>sink</code> 줄의 텍스트가 그 옆으로 흐르게 한다. 이 엔진은 글자를 왼쪽 float 로 만들고(드롭 캡을 위한 float 는 이미 있다) 규격이 처음 글자 상자에 요구하는 글꼴 크기, 줄 높이, 위쪽 margin 을
 * 준다: 글자의 대문자 높이는 <code>(size - 1) * 줄 높이 + 문단의 대문자 높이</code>, 상자는 <code>sink</code> 줄 높이이며, 대문자의 위쪽이 첫 줄 대문자의 위쪽에 맞는다.
 */
final class InitialLetter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private InitialLetter() {
    }

    /**
     * Returns the style that makes the first letter an initial letter, or the style itself if <code>initial-letter</code> is <code>normal</code> (or the letter is not
     * a candidate: a float or an absolutely positioned one has its own place).
     * <p>
     * 첫 글자를 처음 글자로 만드는 스타일을 반환한다. <code>initial-letter</code> 가 <code>normal</code> 이면(또는 글자가 float 나 절대 배치로 이미 자기 자리를 가졌으면) 스타일 그대로.
     *
     * @param fl     the style of <code>::first-letter</code> / <code>::first-letter</code> 의 스타일
     * @param parent the style of the paragraph (the element that the pseudo-element belongs to) / 문단(의사 요소가 속한 요소)의 스타일
     * @return the style / 스타일
     */
    static ComputedStyle adjust(ComputedStyle fl, ComputedStyle parent) {
        CssValue v = fl.get(Prop.INITIAL_LETTER);
        if (v == null || v.isKeyword("normal") || fl.get(Prop.FLOAT) != null && !fl.get(Prop.FLOAT).isKeyword("none")) {
            return fl;
        }
        double[] sizeSink = sizeAndSink(v);
        if (sizeSink == null) {
            return fl;
        }
        double size = sizeSink[0];
        double sink = sizeSink[1];
        Font pf = FontResolver.resolve(parent, "H");
        double[] pm = FontResolver.metrics(pf);
        double parentLine = parent.lineHeightPx();
        // 줄 높이가 normal 이면 글꼴의 ascent + descent + leading 이다.
        if (Double.isNaN(parentLine) || parentLine < 0) {
            parentLine = pm[0] + pm[1] + pm[2];
        }
        double parentCap = capHeight(pf);
        double capTarget = (size - 1) * parentLine + parentCap;
        Font lf = FontResolver.resolve(fl, "H");
        double ratio = capHeight(lf) / lf.getSize2D();
        if (ratio <= 0 || capTarget <= 0) {
            return fl;
        }
        double letterSize = capTarget / ratio;
        Font sized = lf.deriveFont((float) letterSize);
        double[] lm = FontResolver.metrics(sized);
        double boxHeight = sink * parentLine;
        // 상자 안의 대문자 위쪽 위치(반 줄간격 + ascent − 대문자 높이)와 첫 줄의 대문자 위쪽 위치를 맞추도록 위쪽 margin 을 정한다(음수가 될 수 있다).
        double letterTop = (boxHeight - (lm[0] + lm[1])) / 2 + lm[0] - capHeight(sized);
        double parentTop = (parentLine - (pm[0] + pm[1])) / 2 + pm[0] - parentCap;
        ComputedStyle out = fl.with(Prop.FLOAT, CssKeyword.of("left"));
        out = out.with(Prop.DISPLAY, CssKeyword.of("block"));
        out = out.with(Prop.FONT_SIZE, CssLength.px(letterSize));
        out = out.with(Prop.LINE_HEIGHT, CssLength.px(boxHeight));
        out = out.with(Prop.MARGIN_TOP, CssLength.px(parentTop - letterTop));
        // 위쪽 margin 만큼 아래쪽 margin 을 반대로 주어 float 가 차지하는 높이가 정확히 sink 줄이 되게 한다.
        out = out.with(Prop.MARGIN_BOTTOM, CssLength.px(letterTop - parentTop));
        return out;
    }

    /**
     * Reads the value of <code>initial-letter</code>: the size in lines and the sink in lines (<code>drop</code> or none is the whole number below the size,
     * <code>raise</code> is 1, an integer is itself).
     * <p>
     * <code>initial-letter</code> 의 값을 읽는다: 줄 수로 나타낸 크기와 가라앉는 줄 수(<code>drop</code> 이나 생략은 크기 아래의 정수, <code>raise</code> 는 1, 정수는 그 값).
     *
     * @param v the value / 값
     * @return {size, sink} or null if the value cannot be used / {크기, 가라앉는 줄 수}. 쓸 수 없으면 null
     */
    private static double[] sizeAndSink(CssValue v) {
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        if (items.isEmpty() || !(items.get(0) instanceof CssNumber)) {
            return null;
        }
        double size = ((CssNumber) items.get(0)).getValue();
        if (size < 1) {
            return null;
        }
        double sink = Math.max(1, Math.floor(size));
        // 둘째 값이 정수이면 그 값, raise 이면 1, drop 이면 크기 아래의 정수이다.
        if (items.size() > 1) {
            CssValue second = items.get(1);
            if (second instanceof CssNumber) {
                sink = Math.max(1, ((CssNumber) second).getValue());
            } else if (second.isKeyword("raise")) {
                sink = 1;
            }
        }
        return new double[] {size, sink};
    }

    /**
     * Measures the height of the capital letters of a font: the height of the glyph of <code>H</code>.
     * <p>
     * 글꼴의 대문자 높이를 잰다: <code>H</code> 글리프의 높이.
     *
     * @param f the font / 글꼴
     * @return the height in pixels / 높이(픽셀)
     */
    private static double capHeight(Font f) {
        return f.createGlyphVector(FontResolver.FRC, "H").getVisualBounds().getHeight();
    }
}
