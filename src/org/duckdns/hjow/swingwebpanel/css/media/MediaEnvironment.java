package org.duckdns.hjow.swingwebpanel.css.media;

/**
 * The properties of the output device that media queries test: the viewport size, the pixel density, the color depth, the input capabilities
 * (<code>hover</code>, <code>pointer</code>), the user preferences (<code>prefers-color-scheme</code>, <code>prefers-reduced-motion</code>, ...) and the
 * media type (<code>screen</code> or <code>print</code>). The defaults describe a desktop screen with a mouse. The object is mutable; after changing it
 * hand it to <code>StyleResolver.setMediaEnvironment</code> so that the styles are recomputed.
 * <p>
 * 미디어 질의가 검사하는 출력 장치의 속성: 뷰포트 크기, 픽셀 밀도, 색 깊이, 입력 능력(<code>hover</code>, <code>pointer</code>), 사용자 선호
 * (<code>prefers-color-scheme</code>, <code>prefers-reduced-motion</code> 등), 미디어 종류(<code>screen</code> 또는 <code>print</code>). 기본값은 마우스가 있는 데스크톱
 * 화면이다. 객체는 가변이며, 바꾼 뒤 <code>StyleResolver.setMediaEnvironment</code> 에 넘겨야 스타일이 다시 계산된다.
 */
public final class MediaEnvironment {

    /** Viewport width in CSS pixels. / 뷰포트 폭(CSS 픽셀). */
    private double width = 800;
    /** Viewport height in CSS pixels. / 뷰포트 높이(CSS 픽셀). */
    private double height = 600;
    /** Device width, or NaN to use the viewport width. / 장치 폭. NaN 이면 뷰포트 폭. */
    private double deviceWidth = Double.NaN;
    /** Device height, or NaN to use the viewport height. / 장치 높이. NaN 이면 뷰포트 높이. */
    private double deviceHeight = Double.NaN;
    /** Device pixels per CSS pixel. / CSS 픽셀당 장치 픽셀. */
    private double resolution = 1.0;
    /** Bits per color component. / 색 구성 요소당 비트. */
    private int colorBits = 8;
    /** Bits per pixel of a monochrome device, 0 for color devices. / 흑백 장치의 픽셀당 비트. 컬러 장치는 0. */
    private int monochromeBits;
    /** The media type: screen or print. / 미디어 종류: screen 또는 print. */
    private String mediaType = "screen";
    /** Color scheme preference. / 색 구성표 선호. */
    private String colorScheme = "light";
    /** Reduced motion preference. / 동작 줄임 선호. */
    private String reducedMotion = "no-preference";
    /** Contrast preference. / 대비 선호. */
    private String contrast = "no-preference";
    /** Hover capability of the primary input. / 주 입력 장치의 hover 능력. */
    private String hover = "hover";
    /** Pointer accuracy of the primary input. / 주 입력 장치의 포인터 정확도. */
    private String pointer = "fine";
    /** Whether JavaScript is enabled (the <code>scripting</code> feature). / 자바스크립트가 켜져 있는지(<code>scripting</code> 특성). */
    private boolean scripting = true;
    /** Display mode. / 표시 모드. */
    private String displayMode = "browser";

    /**
     * Creates the default environment: an 800 by 600 screen.
     * <p>
     * 기본 환경을 만든다: 800 x 600 화면.
     */
    public MediaEnvironment() {
    }

    /**
     * Creates an environment with a viewport size.
     * <p>
     * 뷰포트 크기를 지정해 환경을 만든다.
     *
     * @param width  viewport width / 뷰포트 폭
     * @param height viewport height / 뷰포트 높이
     */
    public MediaEnvironment(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns an independent copy.
     * <p>
     * 독립된 복사본을 반환한다.
     *
     * @return the copy / 복사본
     */
    public MediaEnvironment copy() {
        MediaEnvironment e = new MediaEnvironment(width, height);
        e.deviceWidth = deviceWidth;
        e.deviceHeight = deviceHeight;
        e.resolution = resolution;
        e.colorBits = colorBits;
        e.monochromeBits = monochromeBits;
        e.mediaType = mediaType;
        e.colorScheme = colorScheme;
        e.reducedMotion = reducedMotion;
        e.contrast = contrast;
        e.hover = hover;
        e.pointer = pointer;
        e.scripting = scripting;
        e.displayMode = displayMode;
        return e;
    }

    /**
     * Returns the viewport width.
     * <p>
     * 뷰포트 폭을 반환한다.
     *
     * @return CSS pixels / CSS 픽셀
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the viewport height.
     * <p>
     * 뷰포트 높이를 반환한다.
     *
     * @return CSS pixels / CSS 픽셀
     */
    public double getHeight() {
        return height;
    }

    /**
     * Sets the viewport size.
     * <p>
     * 뷰포트 크기를 설정한다.
     *
     * @param w width in CSS pixels / 폭(CSS 픽셀)
     * @param h height in CSS pixels / 높이(CSS 픽셀)
     */
    public void setViewport(double w, double h) {
        this.width = w;
        this.height = h;
    }

    /**
     * Returns the device width.
     * <p>
     * 장치 폭을 반환한다.
     *
     * @return CSS pixels / CSS 픽셀
     */
    public double getDeviceWidth() {
        return Double.isNaN(deviceWidth) ? width : deviceWidth;
    }

    /**
     * Returns the device height.
     * <p>
     * 장치 높이를 반환한다.
     *
     * @return CSS pixels / CSS 픽셀
     */
    public double getDeviceHeight() {
        return Double.isNaN(deviceHeight) ? height : deviceHeight;
    }

    /**
     * Sets the device size (the screen, as opposed to the viewport).
     * <p>
     * 장치 크기(뷰포트가 아니라 화면)를 설정한다.
     *
     * @param w width / 폭
     * @param h height / 높이
     */
    public void setDeviceSize(double w, double h) {
        this.deviceWidth = w;
        this.deviceHeight = h;
    }

    /**
     * Returns the resolution in device pixels per CSS pixel.
     * <p>
     * 해상도를 CSS 픽셀당 장치 픽셀로 반환한다.
     *
     * @return dppx / dppx
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * Sets the resolution.
     * <p>
     * 해상도를 설정한다.
     *
     * @param dppx device pixels per CSS pixel / CSS 픽셀당 장치 픽셀
     */
    public void setResolution(double dppx) {
        this.resolution = dppx;
    }

    /**
     * Returns the bits per color component.
     * <p>
     * 색 구성 요소당 비트를 반환한다.
     *
     * @return bits / 비트
     */
    public int getColorBits() {
        return colorBits;
    }

    /**
     * Sets the bits per color component (0 for a monochrome device).
     * <p>
     * 색 구성 요소당 비트를 설정한다(흑백 장치는 0).
     *
     * @param bits bits / 비트
     */
    public void setColorBits(int bits) {
        this.colorBits = bits;
    }

    /**
     * Returns the monochrome bits per pixel.
     * <p>
     * 흑백 픽셀당 비트를 반환한다.
     *
     * @return bits, 0 for color devices / 비트. 컬러 장치는 0
     */
    public int getMonochromeBits() {
        return monochromeBits;
    }

    /**
     * Sets the monochrome bits per pixel.
     * <p>
     * 흑백 픽셀당 비트를 설정한다.
     *
     * @param bits bits / 비트
     */
    public void setMonochromeBits(int bits) {
        this.monochromeBits = bits;
    }

    /**
     * Returns the media type.
     * <p>
     * 미디어 종류를 반환한다.
     *
     * @return screen or print / screen 또는 print
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Sets the media type.
     * <p>
     * 미디어 종류를 설정한다.
     *
     * @param type screen or print / screen 또는 print
     */
    public void setMediaType(String type) {
        this.mediaType = type;
    }

    /**
     * Returns the preferred color scheme.
     * <p>
     * 선호하는 색 구성표를 반환한다.
     *
     * @return light or dark / light 또는 dark
     */
    public String getColorScheme() {
        return colorScheme;
    }

    /**
     * Sets the preferred color scheme.
     * <p>
     * 선호하는 색 구성표를 설정한다.
     *
     * @param scheme light or dark / light 또는 dark
     */
    public void setColorScheme(String scheme) {
        this.colorScheme = scheme;
    }

    /**
     * Returns the reduced motion preference.
     * <p>
     * 동작 줄임 선호를 반환한다.
     *
     * @return no-preference or reduce / no-preference 또는 reduce
     */
    public String getReducedMotion() {
        return reducedMotion;
    }

    /**
     * Sets the reduced motion preference.
     * <p>
     * 동작 줄임 선호를 설정한다.
     *
     * @param v no-preference or reduce / no-preference 또는 reduce
     */
    public void setReducedMotion(String v) {
        this.reducedMotion = v;
    }

    /**
     * Returns the contrast preference.
     * <p>
     * 대비 선호를 반환한다.
     *
     * @return no-preference, more, less or custom / no-preference, more, less, custom 중 하나
     */
    public String getContrast() {
        return contrast;
    }

    /**
     * Returns the hover capability.
     * <p>
     * hover 능력을 반환한다.
     *
     * @return hover or none / hover 또는 none
     */
    public String getHover() {
        return hover;
    }

    /**
     * Returns the pointer accuracy.
     * <p>
     * 포인터 정확도를 반환한다.
     *
     * @return fine, coarse or none / fine, coarse, none 중 하나
     */
    public String getPointer() {
        return pointer;
    }

    /**
     * Sets the primary input capabilities.
     * <p>
     * 주 입력 장치의 능력을 설정한다.
     *
     * @param hover   hover or none / hover 또는 none
     * @param pointer fine, coarse or none / fine, coarse, none 중 하나
     */
    public void setInput(String hover, String pointer) {
        this.hover = hover;
        this.pointer = pointer;
    }

    /**
     * Tells whether scripting is enabled.
     * <p>
     * 스크립트가 켜져 있는지 알려 준다.
     *
     * @return true if enabled / 켜져 있으면 true
     */
    public boolean isScripting() {
        return scripting;
    }

    /**
     * Sets whether scripting is enabled.
     * <p>
     * 스크립트가 켜져 있는지 설정한다.
     *
     * @param enabled true if enabled / 켜져 있으면 true
     */
    public void setScripting(boolean enabled) {
        this.scripting = enabled;
    }

    /**
     * Returns the display mode.
     * <p>
     * 표시 모드를 반환한다.
     *
     * @return browser, fullscreen, standalone ... / browser, fullscreen, standalone 등
     */
    public String getDisplayMode() {
        return displayMode;
    }

    /**
     * Tells whether another environment has the same values (so media-dependent styles need no recomputation).
     * <p>
     * 다른 환경과 값이 같은지 알려 준다(그러면 미디어에 의존하는 스타일을 다시 계산하지 않아도 된다).
     *
     * @param o the other environment / 다른 환경
     * @return true if equal / 같으면 true
     */
    public boolean sameAs(MediaEnvironment o) {
        return o != null && width == o.width && height == o.height && getDeviceWidth() == o.getDeviceWidth() && getDeviceHeight() == o.getDeviceHeight()
                && resolution == o.resolution && colorBits == o.colorBits && monochromeBits == o.monochromeBits && mediaType.equals(o.mediaType)
                && colorScheme.equals(o.colorScheme) && reducedMotion.equals(o.reducedMotion) && contrast.equals(o.contrast)
                && hover.equals(o.hover) && pointer.equals(o.pointer) && scripting == o.scripting && displayMode.equals(o.displayMode);
    }
}
