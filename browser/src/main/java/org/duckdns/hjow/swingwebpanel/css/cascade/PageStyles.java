package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.PageRule;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * The cascade of the page context (CSS Paged Media Level 3, <a href="https://www.w3.org/TR/css-page-3/">https://www.w3.org/TR/css-page-3/</a>,
 * W3C Working Draft 14 September 2023, section 4.4 "Cascading in the page context" and section 7 "Page Size"): it takes the <code>@page</code> rules
 * of the style sheets and works out the {@link PageBox} of one page, that is the size of the page box (<code>size</code>), its margins
 * (<code>margin</code>; a percentage is relative to the page size), the marks and the bleed. The engine does not print, so nothing here is laid out:
 * the class gives the computed descriptors that a printing host (or a script through the CSSOM) can read.
 * <p>
 * Usage: <code>PageStyles.resolve(resolver.getPageRules(), null, 1, false, true)</code> returns the box of the first (right) page without a name.
 * <p>
 * 쪽 문맥의 계단식(CSS Paged Media Level 3, <a href="https://www.w3.org/TR/css-page-3/">https://www.w3.org/TR/css-page-3/</a>, W3C 작업 초안
 * 2023-09-14, 4.4 절 "쪽 문맥의 계단식"과 7 절 "쪽 크기"): 스타일 시트의 <code>@page</code> 규칙으로 한 쪽의 {@link PageBox}, 즉 쪽 상자의 크기
 * (<code>size</code>), 여백(<code>margin</code>. 백분율은 쪽 크기 기준), 표시와 재단 여유를 구한다. 엔진은 인쇄하지 않으므로 아무것도 배치하지 않는다. 인쇄하는 호스트나
 * (CSSOM 을 통한) 스크립트가 읽을 계산된 서술자를 준다.
 * <p>
 * 사용법: <code>PageStyles.resolve(resolver.getPageRules(), null, 1, false, true)</code> 는 이름 없는 첫(오른쪽) 쪽의 상자를 반환한다.
 */
public final class PageStyles {

    /** Pixels per inch of the CSS reference pixel. / CSS 기준 픽셀의 인치당 픽셀 수. */
    private static final double PX_PER_IN = 96.0;

    /** Pixels per millimeter. / 밀리미터당 픽셀 수. */
    private static final double PX_PER_MM = PX_PER_IN / 25.4;

    /** The bleed that <code>bleed: auto</code> computes to when the crop marks are on: 6pt. / 자르기 표시가 있을 때 <code>bleed: auto</code> 가 되는 값: 6pt. */
    private static final double AUTO_BLEED_PX = 6.0 * PX_PER_IN / 72.0;

    /** The page names and sizes {name, width in mm, height in mm}; the width is the shorter side. / 쪽 크기 이름과 크기 {이름, 너비 mm, 높이 mm}. 너비가 짧은 변. */
    private static final Object[][] SIZES = {
        {"a5", Double.valueOf(148), Double.valueOf(210)}, {"a4", Double.valueOf(210), Double.valueOf(297)},
        {"a3", Double.valueOf(297), Double.valueOf(420)}, {"b5", Double.valueOf(176), Double.valueOf(250)},
        {"b4", Double.valueOf(250), Double.valueOf(353)}, {"jis-b5", Double.valueOf(182), Double.valueOf(257)},
        {"jis-b4", Double.valueOf(257), Double.valueOf(364)}, {"letter", Double.valueOf(8.5 * 25.4), Double.valueOf(11 * 25.4)},
        {"legal", Double.valueOf(8.5 * 25.4), Double.valueOf(14 * 25.4)}, {"ledger", Double.valueOf(11 * 25.4), Double.valueOf(17 * 25.4)},
    };

    /** The font size for the units <code>em</code> and <code>ex</code> in the page context (no root element is consulted). / 쪽 문맥의 <code>em</code>, <code>ex</code> 기준 글꼴 크기. */
    private static final double PAGE_FONT_SIZE = 16.0;

    /**
     * The computed descriptors of one page: the page box size and the page margins in CSS pixels, the marks, the bleed and the orientation
     * of the output.
     * <p>
     * 한 쪽의 계산된 서술자: 쪽 상자의 크기와 쪽 여백(CSS 픽셀), 표시, 재단 여유와 출력 방향.
     */
    public static final class PageBox {

        /** The page box width in pixels. / 쪽 상자 너비(픽셀). */
        private final double width;

        /** The page box height in pixels. / 쪽 상자 높이(픽셀). */
        private final double height;

        /** The margins top, right, bottom, left in pixels. / 위, 오른쪽, 아래, 왼쪽 여백(픽셀). */
        private final double[] margin;

        /** The computed <code>marks</code>: <code>none</code>, <code>crop</code>, <code>cross</code> or <code>crop cross</code>. / 계산된 <code>marks</code>. */
        private final String marks;

        /** The bleed in pixels. / 재단 여유(픽셀). */
        private final double bleed;

        /** The computed <code>page-orientation</code>. / 계산된 <code>page-orientation</code>. */
        private final String orientation;

        /** True if <code>size</code> is not <code>auto</code> (the author fixed the size). / <code>size</code> 가 <code>auto</code> 가 아니면(저자가 크기를 정했으면) true. */
        private final boolean fixedSize;

        /** For every side whether an <code>@page</code> rule set its margin. / 변마다 <code>@page</code> 규칙이 여백을 정했는지. */
        private final boolean[] marginSet;

        /**
         * Creates a page box.
         * <p>
         * 쪽 상자를 만든다.
         *
         * @param width       the width in pixels / 너비(픽셀)
         * @param height      the height in pixels / 높이(픽셀)
         * @param margin      top, right, bottom, left margins in pixels / 위, 오른쪽, 아래, 왼쪽 여백(픽셀)
         * @param marks       the computed marks / 계산된 marks
         * @param bleed       the bleed in pixels / 재단 여유(픽셀)
         * @param orientation the computed page orientation / 계산된 쪽 방향
         * @param fixedSize   true if the size was not auto / size 가 auto 가 아니었으면 true
         */
        PageBox(double width, double height, double[] margin, String marks, double bleed, String orientation, boolean fixedSize) {
            this(width, height, margin, marks, bleed, orientation, fixedSize, new boolean[4]);
        }

        /**
         * Creates the descriptors of a page, with the knowledge which margins were set.
         * <p>
         * 어느 여백이 정해졌는지를 포함하여 쪽의 서술자를 만든다.
         *
         * @param width       page box width in px / 쪽 상자 너비(px)
         * @param height      page box height in px / 쪽 상자 높이(px)
         * @param margin      top, right, bottom, left margin in px / 위, 오른쪽, 아래, 왼쪽 여백(px)
         * @param marks       the marks / 표시
         * @param bleed       the bleed in px / 재단 여유(px)
         * @param orientation the page orientation / 쪽 방향
         * @param fixedSize   true if a rule set the size / 규칙이 크기를 정했으면 true
         * @param marginSet   for top, right, bottom, left whether a rule set the margin / 위, 오른쪽, 아래, 왼쪽 여백을 규칙이 정했는지
         */
        PageBox(double width, double height, double[] margin, String marks, double bleed, String orientation, boolean fixedSize, boolean[] marginSet) {
            this.marginSet = marginSet;
            this.width = width;
            this.height = height;
            this.margin = margin;
            this.marks = marks;
            this.bleed = bleed;
            this.orientation = orientation;
            this.fixedSize = fixedSize;
        }

        /**
         * Returns the width of the page box.
         * <p>
         * 쪽 상자의 너비를 반환한다.
         *
         * @return the width in pixels / 너비(픽셀)
         */
        public double getWidth() {
            return width;
        }

        /**
         * Returns the height of the page box.
         * <p>
         * 쪽 상자의 높이를 반환한다.
         *
         * @return the height in pixels / 높이(픽셀)
         */
        public double getHeight() {
            return height;
        }

        /**
         * Returns a margin of the page box.
         * <p>
         * 쪽 상자의 여백 하나를 반환한다.
         *
         * @param side 0 top, 1 right, 2 bottom, 3 left / 0 위, 1 오른쪽, 2 아래, 3 왼쪽
         * @return the margin in pixels / 여백(픽셀)
         */
        public double getMargin(int side) {
            return margin[side];
        }

        /**
         * Returns the width of the page area: the page box without the left and right margins (never negative).
         * <p>
         * 쪽 영역의 너비를 반환한다: 좌우 여백을 뺀 쪽 상자 너비(음수가 되지 않는다).
         *
         * @return the width in pixels / 너비(픽셀)
         */
        public double getAreaWidth() {
            return Math.max(0, width - margin[1] - margin[3]);
        }

        /**
         * Returns the height of the page area: the page box without the top and bottom margins (never negative).
         * <p>
         * 쪽 영역의 높이를 반환한다: 위아래 여백을 뺀 쪽 상자 높이(음수가 되지 않는다).
         *
         * @return the height in pixels / 높이(픽셀)
         */
        public double getAreaHeight() {
            return Math.max(0, height - margin[0] - margin[2]);
        }

        /**
         * Returns the computed <code>marks</code>.
         * <p>
         * 계산된 <code>marks</code> 를 반환한다.
         *
         * @return <code>none</code>, <code>crop</code>, <code>cross</code> or <code>crop cross</code> / marks 값
         */
        public String getMarks() {
            return marks;
        }

        /**
         * Returns the bleed: the extent of the bleed area outside the page box.
         * <p>
         * 재단 여유를 반환한다: 쪽 상자 밖 재단 여유 영역의 폭.
         *
         * @return the bleed in pixels / 재단 여유(픽셀)
         */
        public double getBleed() {
            return bleed;
        }

        /**
         * Returns the computed <code>page-orientation</code>.
         * <p>
         * 계산된 <code>page-orientation</code> 을 반환한다.
         *
         * @return <code>upright</code>, <code>rotate-left</code> or <code>rotate-right</code> / 방향 값
         */
        public String getOrientation() {
            return orientation;
        }

        /**
         * Tells whether an <code>@page</code> rule set the margin of a side; a margin that no rule set is 0 here and a printing host
         * uses the margin of its own paper for it.
         * <p>
         * <code>@page</code> 규칙이 한 변의 여백을 정했는지 알려 준다. 어떤 규칙도 정하지 않은 여백은 여기서 0 이며, 인쇄하는 호스트는 그 변에
         * 자기 용지의 여백을 쓴다.
         *
         * @param side 0 top, 1 right, 2 bottom, 3 left / 0 위, 1 오른쪽, 2 아래, 3 왼쪽
         * @return true if a rule set it / 규칙이 정했으면 true
         */
        public boolean isMarginSet(int side) {
            return marginSet[side];
        }

        /**
         * Tells whether the author fixed the size (<code>size</code> is not <code>auto</code>): then the page box has that size on whatever sheet
         * the page is printed.
         * <p>
         * 저자가 크기를 정했는지(<code>size</code> 가 <code>auto</code> 가 아닌지) 알려 준다. 그러면 쪽 상자는 어떤 용지에 인쇄하든 그 크기이다.
         *
         * @return true if the size is fixed / 크기가 고정이면 true
         */
        public boolean isFixedSize() {
            return fixedSize;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PageStyles() {
    }

    /**
     * Works out the page box of a page. The rules that match the page are applied from the least to the most specific (the order of the sources
     * breaks a tie); the declarations of one rule are applied in order, and an <code>!important</code> declaration of any rule wins over every
     * normal one. A property that no rule sets has the initial value; the initial margin is 0 (this engine has no default page margin, as there is no
     * unprintable area).
     * <p>
     * 쪽의 쪽 상자를 구한다. 쪽에 일치하는 규칙을 구체성이 낮은 것부터 높은 것까지(같으면 소스 순서) 적용하고, 한 규칙의 선언은 순서대로 적용하며, 어느 규칙의
     * <code>!important</code> 선언이든 모든 보통 선언을 이긴다. 어떤 규칙도 정하지 않은 속성은 초기값이다. 초기 여백은 0 이다(이 엔진에는 인쇄할 수 없는 영역이
     * 없으므로 기본 쪽 여백이 없다).
     *
     * @param rules    the <code>@page</code> rules in the order of the sources / 소스 순서의 <code>@page</code> 규칙
     * @param pageName the named page, or null/empty for the unnamed page / 쪽의 이름. 이름 없는 쪽이면 null 이나 빈 문자열
     * @param number   the page number counted from 1 / 1 부터 센 쪽 번호
     * @param blank    true for a blank page that a forced break made / 강제 나눔이 만든 빈 쪽이면 true
     * @param right    true for a right page, false for a left page / 오른쪽 쪽이면 true, 왼쪽 쪽이면 false
     * @return the page box / 쪽 상자
     */
    public static PageBox resolve(List<PageRule> rules, String pageName, int number, boolean blank, boolean right) {
        return resolve(rules, pageName, number, blank, right, 210 * PX_PER_MM, 297 * PX_PER_MM);
    }

    /**
     * Works out the page box of a page for a host that prints on a paper of its own: where no <code>@page</code> rule sets the size, the
     * page has the size of that paper (and <code>size: landscape</code> or <code>portrait</code> turns that paper), and percentage
     * margins are relative to it.
     * <p>
     * 자기 용지에 인쇄하는 호스트를 위해 한 쪽의 쪽 상자를 구한다: <code>@page</code> 규칙이 크기를 정하지 않으면 쪽은 그 용지의 크기이고
     * (<code>size: landscape</code> 나 <code>portrait</code> 는 그 용지를 돌린다) 백분율 여백은 그것을 기준으로 한다.
     *
     * @param rules      the <code>@page</code> rules in source order / 소스 순서의 <code>@page</code> 규칙
     * @param pageName   the name of the page or null / 쪽 이름 또는 null
     * @param number     the page number, starting at 1 / 쪽 번호(1 부터)
     * @param blank      true for a blank page / 빈 쪽이면 true
     * @param right      true for a right page / 오른쪽 쪽이면 true
     * @param autoWidth  the width of the paper of the host in px / 호스트 용지의 너비(px)
     * @param autoHeight the height of the paper of the host in px / 호스트 용지의 높이(px)
     * @return the page box / 쪽 상자
     */
    public static PageBox resolve(List<PageRule> rules, String pageName, int number, boolean blank, boolean right, double autoWidth,
            double autoHeight) {
        final List<PageRule> matching = new ArrayList<PageRule>();
        final List<Integer> specificity = new ArrayList<Integer>();
        // 쪽에 일치하는 규칙과 그 구체성을 모은다.
        for (int i = 0; i < rules.size(); i++) {
            int s = rules.get(i).specificity(pageName, number, blank, right);
            if (s >= 0) {
                matching.add(rules.get(i));
                specificity.add(Integer.valueOf(s));
            }
        }
        // 구체성이 낮은 것부터 정렬한다. 같으면 소스 순서를 유지하도록 안정 정렬(Collections.sort)을 쓴다.
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < matching.size(); i++) {
            order.add(Integer.valueOf(i));
        }
        Collections.sort(order, new Comparator<Integer>() {
            /**
             * Compares two matching rules by specificity.
             * <p>
             * 일치한 두 규칙을 구체성으로 비교한다.
             *
             * @param a the first index / 첫 인덱스
             * @param b the second index / 둘째 인덱스
             * @return the comparison / 비교 결과
             */
            public int compare(Integer a, Integer b) {
                return specificity.get(a.intValue()).compareTo(specificity.get(b.intValue()));
            }
        });
        CssValue[] values = new CssValue[Prop.COUNT + 1];
        // 보통 선언을 먼저, 중요 선언을 나중에 적용해 중요 선언이 이기게 한다.
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < order.size(); k++) {
                CssStyleDeclaration d = matching.get(order.get(k).intValue()).getStyle();
                Map<Integer, CssValue> m = pass == 0 ? d.normal() : d.important();
                // 선언된 개별 속성을 덮어쓴다(단축 속성은 이미 개별 속성으로 펼쳐져 있다).
                for (Map.Entry<Integer, CssValue> e : m.entrySet()) {
                    values[e.getKey().intValue()] = e.getValue();
                }
            }
        }
        return build(values, autoWidth, autoHeight);
    }

    /**
     * Turns the cascaded values into the page box.
     * <p>
     * 계단식으로 정해진 값으로 쪽 상자를 만든다.
     *
     * @param v          the values by property id (null means not set) / 속성 id 별 값(null 이면 정해지지 않음)
     * @param autoWidth  the page width while no rule sets the size, in px / 규칙이 크기를 정하지 않는 동안의 쪽 너비(px)
     * @param autoHeight the page height while no rule sets the size, in px / 규칙이 크기를 정하지 않는 동안의 쪽 높이(px)
     * @return the page box / 쪽 상자
     */
    private static PageBox build(CssValue[] v, double autoWidth, double autoHeight) {
        LengthContext lc = new LengthContext(PAGE_FONT_SIZE, PAGE_FONT_SIZE, PAGE_FONT_SIZE * 1.2, 0, 0);
        double[] size = { autoWidth, autoHeight };
        boolean fixed = sizeOf(v[Prop.SIZE], lc, size);
        double[] margin = new double[4];
        boolean[] set = new boolean[4];
        int[] ids = {Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT};
        // 위, 오른쪽, 아래, 왼쪽 여백을 계산한다. 백분율은 쪽 크기 기준이다(위아래는 높이, 좌우는 너비).
        for (int i = 0; i < 4; i++) {
            margin[i] = lengthOf(v[ids[i]], lc, i % 2 == 0 ? size[1] : size[0], 0);
            set[i] = v[ids[i]] != null && !v[ids[i]].isKeyword("auto");
        }
        String marks = marksOf(v[Prop.MARKS]);
        double bleed = lengthOf(v[Prop.BLEED], lc, 0, marks.indexOf("crop") >= 0 ? AUTO_BLEED_PX : 0);
        String orientation = v[Prop.PAGE_ORIENTATION] == null ? "upright" : v[Prop.PAGE_ORIENTATION].toCssText().toLowerCase();
        return new PageBox(size[0], size[1], margin, marks, bleed, orientation, fixed, set);
    }

    /**
     * Computes the page box size from the value of <code>size</code>: <code>auto</code> is the default sheet (A4, portrait), one length is a square,
     * two are the width and the height, a media name is its size, and <code>portrait</code>/<code>landscape</code> orient the size that the name
     * (or, without a name, the default sheet) gives.
     * <p>
     * <code>size</code> 의 값으로 쪽 상자 크기를 구한다: <code>auto</code> 는 기본 용지(A4 세로), 길이 하나는 정사각형, 둘은 너비와 높이, 용지 이름은 그 크기이며
     * <code>portrait</code>/<code>landscape</code> 는 이름(없으면 기본 용지)이 주는 크기의 방향을 정한다.
     *
     * @param value the value or null / 값. 없으면 null
     * @param lc    the length context / 길이 컨텍스트
     * @param out   receives width and height / 너비와 높이를 받는다
     * @return true if the size is fixed by the author / 저자가 크기를 정했으면 true
     */
    private static boolean sizeOf(CssValue value, LengthContext lc, double[] out) {
        if (value == null || value.isKeyword("auto")) {
            return false;
        }
        List<CssValue> items = new ArrayList<CssValue>();
        // 값이 목록이면 항목으로, 아니면 하나로 본다.
        if (value instanceof CssList) {
            items.addAll(((CssList) value).getItems());
        } else {
            items.add(value);
        }
        List<Double> lengths = new ArrayList<Double>();
        String orient = null;
        boolean named = false;
        // 항목을 종류별로 나눈다: 길이, 용지 이름, 방향.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it instanceof CssLength) {
                lengths.add(Double.valueOf(((CssLength) it).toPx(lc)));
                continue;
            }
            String kw = it.toCssText().toLowerCase();
            if (kw.equals("portrait") || kw.equals("landscape")) {
                orient = kw;
                continue;
            }
            // 용지 이름을 찾는다.
            for (int k = 0; k < SIZES.length; k++) {
                if (SIZES[k][0].equals(kw)) {
                    out[0] = ((Double) SIZES[k][1]).doubleValue() * PX_PER_MM;
                    out[1] = ((Double) SIZES[k][2]).doubleValue() * PX_PER_MM;
                    named = true;
                }
            }
        }
        if (!lengths.isEmpty()) {
            out[0] = lengths.get(0).doubleValue();
            out[1] = lengths.size() > 1 ? lengths.get(1).doubleValue() : out[0];
            return true;
        }
        // 방향은 용지 이름이 준 세로 크기를 가로로 돌리는 것이다: landscape 는 긴 변이 가로, portrait 는 짧은 변이 가로.
        if ("landscape".equals(orient) && out[0] < out[1]) {
            double t = out[0];
            out[0] = out[1];
            out[1] = t;
        } else if ("portrait".equals(orient) && out[0] > out[1]) {
            double t = out[0];
            out[0] = out[1];
            out[1] = t;
        }
        return named || orient != null;
    }

    /**
     * Converts a length-like value to pixels.
     * <p>
     * 길이 값을 픽셀로 바꾼다.
     *
     * @param v        the value or null / 값. 없으면 null
     * @param lc       the length context / 길이 컨텍스트
     * @param percent  the length that 100% means / 100% 가 뜻하는 길이
     * @param fallback the pixels of <code>auto</code> or an unset value / <code>auto</code> 이거나 정해지지 않은 값의 픽셀
     * @return the pixels / 픽셀
     */
    private static double lengthOf(CssValue v, LengthContext lc, double percent, double fallback) {
        if (v == null || v.isKeyword("auto")) {
            return fallback;
        }
        if (v instanceof CssLength) {
            return ((CssLength) v).toPx(lc);
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() * percent / 100.0;
        }
        if (v instanceof CssCalc) {
            CssValue s = ((CssCalc) v).simplify(lc);
            return s instanceof CssLength ? ((CssLength) s).toPx(lc) : fallback;
        }
        return fallback;
    }

    /**
     * Serializes the computed <code>marks</code> in the canonical order (<code>crop</code> before <code>cross</code>).
     * <p>
     * 계산된 <code>marks</code> 를 정규 순서(<code>crop</code>, <code>cross</code> 순)로 직렬화한다.
     *
     * @param v the value or null / 값. 없으면 null
     * @return the text / 문자열
     */
    private static String marksOf(CssValue v) {
        if (v == null) {
            return "none";
        }
        String t = " " + v.toCssText().toLowerCase() + " ";
        boolean crop = t.indexOf(" crop ") >= 0;
        boolean cross = t.indexOf(" cross ") >= 0;
        if (crop && cross) {
            return "crop cross";
        }
        return crop ? "crop" : (cross ? "cross" : "none");
    }
}
