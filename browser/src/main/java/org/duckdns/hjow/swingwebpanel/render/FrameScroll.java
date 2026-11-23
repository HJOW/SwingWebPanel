package org.duckdns.hjow.swingwebpanel.render;

/**
 * The arithmetic of the scrolling of a frame: how far a frame may scroll, where the thumb of an overlay scrollbar is and how a position
 * of the thumb maps back to a scroll offset. The painter and the input handling use the same functions, so a thumb that is drawn can be
 * grabbed where it is drawn.
 * <p>
 * 프레임 스크롤의 계산: 프레임이 얼마나 스크롤할 수 있는지, 오버레이 스크롤바의 썸이 어디에 있는지, 썸의 위치가 스크롤 오프셋으로 어떻게 되돌아가는지.
 * 그리는 쪽과 입력 처리가 같은 함수를 쓰므로 그려진 썸을 그려진 곳에서 잡을 수 있다.
 */
public final class FrameScroll {

    /**
     * The thickness of an overlay scrollbar in CSS pixels. It does not take layout space.
     * <p>
     * 오버레이 스크롤바의 두께(CSS 픽셀). 레이아웃 공간을 차지하지 않는다.
     */
    public static final int BAR = 10;

    /**
     * The smallest length of a thumb in CSS pixels, so that it can always be grabbed.
     * <p>
     * 썸의 최소 길이(CSS 픽셀). 항상 잡을 수 있게 한다.
     */
    public static final int MIN_THUMB = 16;

    /**
     * The height of one wheel line in CSS pixels: one wheel notch scrolls a frame by this many pixels per line.
     * <p>
     * 휠 한 줄의 높이(CSS 픽셀): 휠 한 칸은 프레임을 줄당 이만큼 스크롤한다.
     */
    public static final int LINE = 16;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FrameScroll() {
    }

    /**
     * Returns the largest scroll offset: the part of the content that does not fit into the view.
     * <p>
     * 가장 큰 스크롤 오프셋을 반환한다: 내용 중 뷰에 들어가지 않는 부분.
     *
     * @param content the length of the content / 내용의 길이
     * @param view    the length of the view / 뷰의 길이
     * @return the maximum offset, at least 0 / 최대 오프셋(0 이상)
     */
    public static int max(int content, int view) {
        return Math.max(0, content - view);
    }

    /**
     * Limits a scroll offset to the range that the content allows.
     * <p>
     * 스크롤 오프셋을 내용이 허용하는 범위로 제한한다.
     *
     * @param offset  the wanted offset / 원하는 오프셋
     * @param content the length of the content / 내용의 길이
     * @param view    the length of the view / 뷰의 길이
     * @return the offset inside [0, max] / [0, 최대] 안의 오프셋
     */
    public static int clamp(int offset, int content, int view) {
        return Math.max(0, Math.min(offset, max(content, view)));
    }

    /**
     * Computes the thumb of a scrollbar: its start and its length along the track, proportional to the visible part of the content.
     * <p>
     * 스크롤바의 썸을 계산한다: 트랙을 따른 시작과 길이이며 내용 중 보이는 부분에 비례한다.
     *
     * @param track   the length of the track / 트랙의 길이
     * @param content the length of the content / 내용의 길이
     * @param offset  the scroll offset / 스크롤 오프셋
     * @return {start, length} / {시작, 길이}
     */
    public static double[] thumb(double track, double content, double offset) {
        if (content <= 0 || track <= 0) {
            return new double[] { 0, Math.max(0, track) };
        }
        double length = Math.max(MIN_THUMB, Math.min(track, track * track / content));
        double span = content - track;
        double start = span <= 0 ? 0 : (track - length) * Math.max(0, Math.min(1, offset / span));
        return new double[] { start, length };
    }

    /**
     * Maps the position of the start of a thumb back to a scroll offset (the inverse of {@link #thumb}).
     * <p>
     * 썸 시작 위치를 스크롤 오프셋으로 되돌린다({@link #thumb} 의 역).
     *
     * @param track      the length of the track / 트랙의 길이
     * @param content    the length of the content / 내용의 길이
     * @param thumbStart the wanted start of the thumb / 원하는 썸 시작
     * @return the scroll offset / 스크롤 오프셋
     */
    public static int offsetForThumb(double track, double content, double thumbStart) {
        double length = Math.max(MIN_THUMB, Math.min(track, track * track / Math.max(1, content)));
        double free = track - length;
        if (free <= 0) {
            return 0;
        }
        double span = content - track;
        double fraction = Math.max(0, Math.min(1, thumbStart / free));
        return (int) Math.round(span * fraction);
    }
}
