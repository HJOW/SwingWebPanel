package org.duckdns.hjow.swingwebpanel.core.frames;

/**
 * The fixed limits of nested browsing contexts (frames): how deep frames may be nested, how many frames one page may create and the default
 * size. Exceeding a limit never throws; the frame that would break it is not loaded and shows an error document instead (plan item P20-02).
 * The scrollbar sizes live in {@link org.duckdns.hjow.swingwebpanel.render.FrameScroll}.
 * <p>
 * 중첩된 브라우징 문맥(프레임)의 고정 한도: 프레임을 얼마나 깊이 중첩할 수 있는지, 한 페이지가 프레임을 몇 개까지 만들 수 있는지, 기본 크기. 한도를 넘어도
 * 예외를 던지지 않는다. 한도를 깨는 프레임은 로드하지 않고 오류 문서를 보여 준다(계획 P20-02). 스크롤바 크기는
 * {@link org.duckdns.hjow.swingwebpanel.render.FrameScroll} 에 있다.
 */
public final class FrameLimits {

    /**
     * The deepest nesting level that is loaded: the top-level document is level 0 and its frames are level 1. A frame at a deeper level is not
     * loaded; it shows an error document.
     * <p>
     * 로드하는 가장 깊은 중첩 단계: 최상위 문서가 0 단계이고 그 프레임이 1 단계이다. 더 깊은 단계의 프레임은 로드하지 않고 오류 문서를 보여 준다.
     */
    public static final int MAX_DEPTH = 8;

    /**
     * The maximum number of frames (all levels together) of one top-level page. Further frame elements are not given a browsing context.
     * <p>
     * 최상위 페이지 하나의 프레임 최대 개수(모든 단계 합계). 그 이상의 프레임 요소에는 브라우징 문맥을 주지 않는다.
     */
    public static final int MAX_FRAMES_PER_PAGE = 64;

    /**
     * The default width of an <code>iframe</code> in CSS pixels (as in the HTML Standard).
     * <p>
     * <code>iframe</code> 의 기본 너비(CSS 픽셀, HTML 표준과 같다).
     */
    public static final int DEFAULT_WIDTH = 300;

    /**
     * The default height of an <code>iframe</code> in CSS pixels (as in the HTML Standard).
     * <p>
     * <code>iframe</code> 의 기본 높이(CSS 픽셀, HTML 표준과 같다).
     */
    public static final int DEFAULT_HEIGHT = 150;

    /**
     * The maximum number of entries of the local history of one frame.
     * <p>
     * 프레임 하나의 지역 이력의 최대 항목 수.
     */
    public static final int MAX_HISTORY = 64;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있는 클래스이다.
     */
    private FrameLimits() {
    }
}
