package org.duckdns.hjow.swingwebpanel.css.values;

/**
 * The reference sizes needed to turn relative lengths into pixels: the font size of the element (for <code>em</code>), the font size of the root
 * element (<code>rem</code>), the line height (<code>lh</code>), the viewport size (<code>vw</code>, <code>vh</code>, ...) and approximations of the
 * x-height and zero-advance of the font (<code>ex</code>, <code>ch</code>). Percentages are not resolved here; they depend on the layout.
 * <p>
 * 상대 길이를 픽셀로 바꾸는 데 필요한 기준 크기: 요소의 글꼴 크기(<code>em</code>), 루트 요소의 글꼴 크기(<code>rem</code>), 줄 높이(<code>lh</code>),
 * 뷰포트 크기(<code>vw</code>, <code>vh</code> 등), 글꼴의 x 높이와 0 의 폭 근사값(<code>ex</code>, <code>ch</code>). 백분율은 여기서 계산하지 않으며 레이아웃에 달려 있다.
 */
public final class LengthContext {

    /** Default font size in pixels. / 기본 글꼴 크기(픽셀). */
    public static final double DEFAULT_FONT_SIZE = 16.0;

    /** Font size of the element. / 요소의 글꼴 크기. */
    private final double fontSize;

    /** Font size of the root element. / 루트 요소의 글꼴 크기. */
    private final double rootFontSize;

    /** Line height in pixels. / 줄 높이(픽셀). */
    private final double lineHeight;

    /** Viewport width in pixels. / 뷰포트 폭(픽셀). */
    private final double viewportWidth;

    /** Viewport height in pixels. / 뷰포트 높이(픽셀). */
    private final double viewportHeight;

    /**
     * Creates a context.
     * <p>
     * 컨텍스트를 만든다.
     *
     * @param fontSize       element font size / 요소 글꼴 크기
     * @param rootFontSize   root font size / 루트 글꼴 크기
     * @param lineHeight     line height / 줄 높이
     * @param viewportWidth  viewport width / 뷰포트 폭
     * @param viewportHeight viewport height / 뷰포트 높이
     */
    public LengthContext(double fontSize, double rootFontSize, double lineHeight, double viewportWidth, double viewportHeight) {
        this(fontSize, rootFontSize, lineHeight, viewportWidth, viewportHeight, false, false);
    }

    /** True if the user agent prefers a dark color scheme (<code>prefers-color-scheme: dark</code>). 사용자 에이전트가 어두운 색 구성표를 선호하면 true(<code>prefers-color-scheme: dark</code>). */
    private final boolean prefersDark;

    /** True if the element uses the dark color scheme (known once its <code>color-scheme</code> is computed). 요소가 어두운 색 구성표를 쓰면 true(<code>color-scheme</code> 을 계산한 뒤에 알려진다). */
    private final boolean dark;

    /** Content box width of the nearest query container for <code>cqw</code>, or NaN if there is none (the small viewport width then). <code>cqw</code> 를 위한 가장 가까운 질의 컨테이너의 content box 너비. 없으면 NaN(그러면 작은 뷰포트 너비). */
    private double containerWidth = Double.NaN;

    /** Content box height of the nearest query container for <code>cqh</code>, or NaN if there is none. <code>cqh</code> 를 위한 가장 가까운 질의 컨테이너의 content box 높이. 없으면 NaN. */
    private double containerHeight = Double.NaN;

    /**
     * Returns a copy that knows the size of the query container of the element (CSS Conditional Level 5, 4.4 "Container Relative Lengths"): a size that
     * is NaN has no container, and <code>cqw</code>/<code>cqh</code> then fall back to the viewport.
     * <p>
     * 요소의 질의 컨테이너 크기를 아는 복사본을 반환한다(CSS Conditional Level 5, 4.4 "Container Relative Lengths"): NaN 인 크기는 컨테이너가 없는 것이며 그러면
     * <code>cqw</code>/<code>cqh</code> 는 뷰포트로 대체된다.
     *
     * @param width  the content box width of the nearest container with inline size containment, or NaN / 인라인 크기 격리가 있는 가장 가까운 컨테이너의 content box 너비. 없으면 NaN
     * @param height the content box height of the nearest container with size containment, or NaN / 크기 격리가 있는 가장 가까운 컨테이너의 content box 높이. 없으면 NaN
     * @return the copy / 복사본
     */
    public LengthContext withContainer(double width, double height) {
        LengthContext c = new LengthContext(fontSize, rootFontSize, lineHeight, viewportWidth, viewportHeight, prefersDark, dark);
        c.containerWidth = width;
        c.containerHeight = height;
        return c;
    }

    /**
     * Copies the container sizes of this context into a copy that was made with other values.
     * <p>
     * 이 컨텍스트의 컨테이너 크기를 다른 값으로 만든 복사본에 옮긴다.
     *
     * @param c the copy / 복사본
     * @return the copy / 복사본
     */
    private LengthContext carry(LengthContext c) {
        c.containerWidth = containerWidth;
        c.containerHeight = containerHeight;
        return c;
    }

    /**
     * Returns the content box width of the nearest query container.
     * <p>
     * 가장 가까운 질의 컨테이너의 content box 너비를 반환한다.
     *
     * @return the width, or NaN if there is none / 너비. 없으면 NaN
     */
    public double getContainerWidth() {
        return containerWidth;
    }

    /**
     * Returns the content box height of the nearest query container.
     * <p>
     * 가장 가까운 질의 컨테이너의 content box 높이를 반환한다.
     *
     * @return the height, or NaN if there is none / 높이. 없으면 NaN
     */
    public double getContainerHeight() {
        return containerHeight;
    }

    /**
     * Creates a context with the color scheme.
     * <p>
     * 색 구성표를 가진 컨텍스트를 만든다.
     *
     * @param fontSize       element font size / 요소 글꼴 크기
     * @param rootFontSize   root font size / 루트 글꼴 크기
     * @param lineHeight     line height / 줄 높이
     * @param viewportWidth  viewport width / 뷰포트 폭
     * @param viewportHeight viewport height / 뷰포트 높이
     * @param prefersDark    true if the user prefers a dark scheme / 사용자가 어두운 구성표를 선호하면 true
     * @param dark           true if the element uses the dark scheme / 요소가 어두운 구성표를 쓰면 true
     */
    private LengthContext(double fontSize, double rootFontSize, double lineHeight, double viewportWidth, double viewportHeight, boolean prefersDark,
            boolean dark) {
        this.fontSize = fontSize;
        this.rootFontSize = rootFontSize;
        this.lineHeight = lineHeight;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.prefersDark = prefersDark;
        this.dark = dark;
    }

    /**
     * Returns a copy that records whether the user agent prefers a dark color scheme.
     * <p>
     * 사용자 에이전트가 어두운 색 구성표를 선호하는지 기록한 복사본을 반환한다.
     *
     * @param prefers true for a dark preference / 어두운 구성표를 선호하면 true
     * @return the copy / 복사본
     */
    public LengthContext withPrefersDark(boolean prefers) {
        return carry(new LengthContext(fontSize, rootFontSize, lineHeight, viewportWidth, viewportHeight, prefers, dark));
    }

    /**
     * Returns a copy for an element that uses the dark color scheme (or not).
     * <p>
     * 어두운 색 구성표를 쓰는(또는 쓰지 않는) 요소를 위한 복사본을 반환한다.
     *
     * @param useDark true for the dark scheme / 어두운 구성표이면 true
     * @return the copy / 복사본
     */
    public LengthContext withDark(boolean useDark) {
        return carry(new LengthContext(fontSize, rootFontSize, lineHeight, viewportWidth, viewportHeight, prefersDark, useDark));
    }

    /**
     * Tells whether the user agent prefers a dark color scheme.
     * <p>
     * 사용자 에이전트가 어두운 색 구성표를 선호하는지 알려 준다.
     *
     * @return true for a dark preference / 어두운 구성표를 선호하면 true
     */
    public boolean isPrefersDark() {
        return prefersDark;
    }

    /**
     * Tells whether the element uses the dark color scheme.
     * <p>
     * 요소가 어두운 색 구성표를 쓰는지 알려 준다.
     *
     * @return true for the dark scheme / 어두운 구성표이면 true
     */
    public boolean isDark() {
        return dark;
    }

    /**
     * Creates the default context: 16px fonts, a 1.2 line height and an 800 by 600 viewport.
     * <p>
     * 기본 컨텍스트를 만든다: 16px 글꼴, 줄 높이 1.2, 800 x 600 뷰포트.
     *
     * @return the default context / 기본 컨텍스트
     */
    public static LengthContext defaults() {
        return new LengthContext(DEFAULT_FONT_SIZE, DEFAULT_FONT_SIZE, DEFAULT_FONT_SIZE * 1.2, 800, 600);
    }

    /**
     * Returns a copy with another element font size (and a line height scaled with it).
     * <p>
     * 요소 글꼴 크기(와 그에 비례한 줄 높이)만 다른 복사본을 반환한다.
     *
     * @param newFontSize font size in pixels / 글꼴 크기(픽셀)
     * @return the copy / 복사본
     */
    public LengthContext withFontSize(double newFontSize) {
        return carry(new LengthContext(newFontSize, rootFontSize, newFontSize * 1.2, viewportWidth, viewportHeight, prefersDark, dark));
    }

    /**
     * Returns a copy with another line height.
     * <p>
     * 줄 높이만 다른 복사본을 반환한다.
     *
     * @param newLineHeight line height in pixels / 줄 높이(픽셀)
     * @return the copy / 복사본
     */
    public LengthContext withLineHeight(double newLineHeight) {
        return carry(new LengthContext(fontSize, rootFontSize, newLineHeight, viewportWidth, viewportHeight, prefersDark, dark));
    }

    /**
     * Returns the element font size.
     * <p>
     * 요소 글꼴 크기를 반환한다.
     *
     * @return pixels / 픽셀
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * Returns the root font size.
     * <p>
     * 루트 글꼴 크기를 반환한다.
     *
     * @return pixels / 픽셀
     */
    public double getRootFontSize() {
        return rootFontSize;
    }

    /**
     * Returns the viewport width.
     * <p>
     * 뷰포트 폭을 반환한다.
     *
     * @return pixels / 픽셀
     */
    public double getViewportWidth() {
        return viewportWidth;
    }

    /**
     * Returns the viewport height.
     * <p>
     * 뷰포트 높이를 반환한다.
     *
     * @return pixels / 픽셀
     */
    public double getViewportHeight() {
        return viewportHeight;
    }

    /**
     * Returns a copy with another root font size.
     * <p>
     * 루트 글꼴 크기만 다른 복사본을 반환한다.
     *
     * @param newRoot root font size in pixels / 루트 글꼴 크기(픽셀)
     * @return the copy / 복사본
     */
    public LengthContext withRootFontSize(double newRoot) {
        return carry(new LengthContext(fontSize, newRoot, lineHeight, viewportWidth, viewportHeight, prefersDark, dark));
    }

    /**
     * Converts a length to pixels.
     * <p>
     * 길이를 픽셀로 바꾼다.
     *
     * @param value number / 숫자
     * @param unit  lower-case unit name / 소문자 단위 이름
     * @return pixels (NaN for an unknown unit) / 픽셀(알 수 없는 단위이면 NaN)
     */
    public double toPx(double value, String unit) {
        int kind = CssUnits.kindOf(unit);
        if (kind == CssUnits.ABSOLUTE_LENGTH) {
            return CssUnits.absoluteToPx(value, unit);
        }
        if (kind == CssUnits.FONT_RELATIVE_LENGTH) {
            if (unit.equals("em")) {
                return value * fontSize;
            }
            if (unit.equals("rem")) {
                return value * rootFontSize;
            }
            if (unit.equals("ex") || unit.equals("ch") || unit.equals("cap")) {
                return value * fontSize * 0.5;
            }
            if (unit.equals("lh")) {
                return value * lineHeight;
            }
            if (unit.equals("rlh")) {
                return value * rootFontSize * 1.2;
            }
            if (unit.equals("ic")) {
                return value * fontSize;
            }
            return value * rootFontSize * 0.5;
        }
        if (kind == CssUnits.CONTAINER_LENGTH) {
            // 컨테이너 상대 길이는 축마다 가장 가까운 컨테이너의 크기를 쓰고 컨테이너가 없으면 뷰포트 크기를 쓴다(1 단위 = 크기의 1 %).
            double w = (Double.isNaN(containerWidth) ? viewportWidth : containerWidth) / 100.0;
            double h = (Double.isNaN(containerHeight) ? viewportHeight : containerHeight) / 100.0;
            if (unit.equals("cqw") || unit.equals("cqi")) {
                return value * w;
            }
            if (unit.equals("cqh") || unit.equals("cqb")) {
                return value * h;
            }
            return value * (unit.equals("cqmin") ? Math.min(w, h) : Math.max(w, h));
        }
        if (kind == CssUnits.VIEWPORT_LENGTH) {
            char first = unit.charAt(0);
            // 작은/큰/동적 뷰포트 변형(sv*, lv*, dv*)은 접두 글자를 뗀 기본 단위로 계산한다.
            String base = first == 's' || first == 'l' || first == 'd' ? unit.substring(1) : unit;
            double w = viewportWidth / 100.0;
            double h = viewportHeight / 100.0;
            if (base.equals("vw") || base.equals("vi")) {
                return value * w;
            }
            if (base.equals("vh") || base.equals("vb")) {
                return value * h;
            }
            if (base.equals("vmin")) {
                return value * Math.min(w, h);
            }
            return value * Math.max(w, h);
        }
        return Double.NaN;
    }
}
