package org.duckdns.hjow.swingwebpanel.render;

/**
 * The geometry of the control bar of an <code>audio</code> element with <code>controls</code> (plan item P18-08): where the play button,
 * the time label, the seek bar, the mute button and the volume slider are in a bar of a given size, which one lies under a point, and what
 * position or volume a point on a slider means. The painter and the input handler use the same functions, so what the user sees is what the
 * mouse hits. The bar adapts to its width: a narrow bar drops the volume slider, then the time label and the mute button, then the seek
 * bar, and keeps the play button.
 * <p>
 * <code>controls</code> 가 있는 <code>audio</code> 요소의 컨트롤 막대의 기하(계획 P18-08): 주어진 크기의 막대에서 재생 버튼, 시간 표시, 탐색 막대,
 * 음소거 버튼, 음량 슬라이더가 어디에 있는지, 한 점 아래에 어느 것이 있는지, 슬라이더 위의 점이 어떤 위치나 음량을 뜻하는지. 그리기와 입력 처리가
 * 같은 함수를 쓰므로 사용자가 보는 것이 마우스가 맞히는 것이다. 막대는 너비에 맞춘다: 좁은 막대는 음량 슬라이더, 그다음 시간 표시와 음소거 버튼,
 * 그다음 탐색 막대를 빼고 재생 버튼은 남긴다.
 */
public final class MediaControls {

    /**
     * Part: nothing (the background of the bar).
     * <p>
     * 부분: 없음(막대의 배경).
     */
    public static final int PART_NONE = 0;

    /**
     * Part: the play and pause button.
     * <p>
     * 부분: 재생/일시정지 버튼.
     */
    public static final int PART_PLAY = 1;

    /**
     * Part: the time label.
     * <p>
     * 부분: 시간 표시.
     */
    public static final int PART_TIME = 2;

    /**
     * Part: the seek bar.
     * <p>
     * 부분: 탐색 막대.
     */
    public static final int PART_SEEK = 3;

    /**
     * Part: the mute button.
     * <p>
     * 부분: 음소거 버튼.
     */
    public static final int PART_MUTE = 4;

    /**
     * Part: the volume slider.
     * <p>
     * 부분: 음량 슬라이더.
     */
    public static final int PART_VOLUME = 5;

    /**
     * The space between the border of the bar and its parts and between the parts, in pixels.
     * <p>
     * 막대의 경계와 부분 사이, 부분들 사이의 간격(픽셀).
     */
    static final double GAP = 8;

    /**
     * The side of the play button, at most (it shrinks in a low bar).
     * <p>
     * 재생 버튼의 한 변의 최대 크기(낮은 막대에서는 줄어든다).
     */
    static final double BUTTON = 28;

    /**
     * The width of the time label.
     * <p>
     * 시간 표시의 너비.
     */
    static final double TIME_WIDTH = 82;

    /**
     * The side of the mute button.
     * <p>
     * 음소거 버튼의 한 변.
     */
    static final double MUTE_SIZE = 24;

    /**
     * The width of the volume slider.
     * <p>
     * 음량 슬라이더의 너비.
     */
    static final double VOLUME_WIDTH = 56;

    /**
     * The narrowest seek bar that is shown.
     * <p>
     * 보여 주는 가장 좁은 탐색 막대.
     */
    static final double MIN_SEEK_WIDTH = 24;

    /**
     * The bar width from which the time label is shown.
     * <p>
     * 시간 표시를 보여 주는 최소 막대 너비.
     */
    static final double TIME_FROM = 190;

    /**
     * The bar width from which the mute button is shown.
     * <p>
     * 음소거 버튼을 보여 주는 최소 막대 너비.
     */
    static final double MUTE_FROM = 150;

    /**
     * The bar width from which the volume slider is shown.
     * <p>
     * 음량 슬라이더를 보여 주는 최소 막대 너비.
     */
    static final double VOLUME_FROM = 270;

    /**
     * The rectangles of the parts, each as x, y, width, height relative to the top left corner of the bar, or null if the part is not shown.
     * <p>
     * 부분들의 사각형. 각각 막대의 왼쪽 위 모서리를 기준으로 한 x, y, 너비, 높이이며, 보이지 않는 부분은 null.
     */
    public static final class Layout {
        /**
         * The play button.
         * <p>
         * 재생 버튼.
         */
        public final double[] play;

        /**
         * The time label, or null.
         * <p>
         * 시간 표시. 없으면 null.
         */
        public final double[] time;

        /**
         * The track of the seek bar, or null.
         * <p>
         * 탐색 막대의 트랙. 없으면 null.
         */
        public final double[] seek;

        /**
         * The mute button, or null.
         * <p>
         * 음소거 버튼. 없으면 null.
         */
        public final double[] mute;

        /**
         * The track of the volume slider, or null.
         * <p>
         * 음량 슬라이더의 트랙. 없으면 null.
         */
        public final double[] volume;

        /**
         * Creates the layout.
         * <p>
         * 배치를 만든다.
         *
         * @param play   the play button / 재생 버튼
         * @param time   the time label or null / 시간 표시 또는 null
         * @param seek   the seek track or null / 탐색 트랙 또는 null
         * @param mute   the mute button or null / 음소거 버튼 또는 null
         * @param volume the volume track or null / 음량 트랙 또는 null
         */
        Layout(double[] play, double[] time, double[] seek, double[] mute, double[] volume) {
            this.play = play;
            this.time = time;
            this.seek = seek;
            this.mute = mute;
            this.volume = volume;
        }
    }

    /**
     * Not instantiated.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private MediaControls() {
    }

    /**
     * Computes the layout of a bar.
     * <p>
     * 막대의 배치를 계산한다.
     *
     * @param w the width of the bar in pixels / 막대의 너비(픽셀)
     * @param h the height of the bar in pixels / 막대의 높이(픽셀)
     * @return the rectangles of the parts / 부분들의 사각형
     */
    public static Layout layout(double w, double h) {
        double size = Math.max(8, Math.min(BUTTON, h - 8));
        double[] play = new double[] { GAP, (h - size) / 2, size, size };
        double left = GAP + size;
        double[] time = null;
        // 너비가 충분하면 시간 표시를 재생 버튼 옆에 둔다.
        if (w >= TIME_FROM) {
            time = new double[] { left + 6, (h - 16) / 2, TIME_WIDTH, 16 };
            left = left + 6 + TIME_WIDTH;
        }
        double right = w - GAP;
        double[] volume = null;
        // 너비가 충분하면 음량 슬라이더를 오른쪽 끝에 둔다.
        if (w >= VOLUME_FROM) {
            volume = new double[] { right - VOLUME_WIDTH, (h - 4) / 2, VOLUME_WIDTH, 4 };
            right = right - VOLUME_WIDTH - 6;
        }
        double[] mute = null;
        // 너비가 충분하면 음소거 버튼을 음량 슬라이더 왼쪽에 둔다.
        if (w >= MUTE_FROM) {
            double s = Math.min(MUTE_SIZE, Math.max(8, h - 8));
            mute = new double[] { right - s, (h - s) / 2, s, s };
            right = right - s;
        }
        double[] seek = null;
        double from = left + GAP;
        double to = right - GAP;
        // 남은 폭이 충분할 때만 탐색 막대를 둔다.
        if (to - from >= MIN_SEEK_WIDTH) {
            seek = new double[] { from, (h - 4) / 2, to - from, 4 };
        }
        return new Layout(play, time, seek, mute, volume);
    }

    /**
     * Tells which part of the bar lies under a point. The slider parts count the whole height of the bar around the track, so a click does
     * not have to hit the thin track exactly.
     * <p>
     * 막대의 어느 부분이 점 아래에 있는지 알려 준다. 슬라이더 부분은 트랙 둘레의 막대 전체 높이를 포함하므로 얇은 트랙을 정확히 맞히지 않아도 된다.
     *
     * @param w the width of the bar / 막대의 너비
     * @param h the height of the bar / 막대의 높이
     * @param x the x of the point relative to the bar / 막대를 기준으로 한 점의 x
     * @param y the y of the point relative to the bar / 막대를 기준으로 한 점의 y
     * @return one of the <code>PART_</code> constants / <code>PART_</code> 상수 중 하나
     */
    public static int partAt(double w, double h, double x, double y) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return PART_NONE;
        }
        Layout l = layout(w, h);
        // 위치가 슬라이더의 좌우 여유 안이면 슬라이더로 친다(높이는 막대 전체).
        if (l.seek != null && x >= l.seek[0] - 6 && x <= l.seek[0] + l.seek[2] + 6) {
            return PART_SEEK;
        }
        if (l.volume != null && x >= l.volume[0] - 6 && x <= l.volume[0] + l.volume[2] + 6) {
            return PART_VOLUME;
        }
        if (inside(l.play, x, y, GAP / 2)) {
            return PART_PLAY;
        }
        if (inside(l.mute, x, y, 2)) {
            return PART_MUTE;
        }
        if (l.time != null && inside(l.time, x, y, 0)) {
            return PART_TIME;
        }
        return PART_NONE;
    }

    /**
     * Tells whether a point is inside a rectangle that is enlarged by a margin.
     * <p>
     * 점이 여백만큼 키운 사각형 안에 있는지 알려 준다.
     *
     * @param r      the rectangle (x, y, width, height) or null / 사각형(x, y, 너비, 높이). null 가능
     * @param x      the x of the point / 점의 x
     * @param y      the y of the point / 점의 y
     * @param margin the margin on all sides / 모든 변의 여백
     * @return true if inside / 안에 있으면 true
     */
    private static boolean inside(double[] r, double x, double y, double margin) {
        return r != null && x >= r[0] - margin && x < r[0] + r[2] + margin && y >= r[1] - margin && y < r[1] + r[3] + margin;
    }

    /**
     * Returns the fraction, from 0 to 1, that a point on a slider stands for.
     * <p>
     * 슬라이더 위의 점이 뜻하는 0 에서 1 사이의 비율을 반환한다.
     *
     * @param w    the width of the bar / 막대의 너비
     * @param h    the height of the bar / 막대의 높이
     * @param part {@link #PART_SEEK} or {@link #PART_VOLUME} / {@link #PART_SEEK} 또는 {@link #PART_VOLUME}
     * @param x    the x of the point relative to the bar (it may lie outside while dragging) / 막대를 기준으로 한 점의 x(끌 때는 밖일 수 있다)
     * @return the fraction, 0 if the part is not shown / 비율. 부분이 보이지 않으면 0
     */
    public static double fractionAt(double w, double h, int part, double x) {
        Layout l = layout(w, h);
        double[] track = part == PART_SEEK ? l.seek : part == PART_VOLUME ? l.volume : null;
        if (track == null || track[2] <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, (x - track[0]) / track[2]));
    }

    /**
     * Formats a time as <code>m:ss</code>, or <code>h:mm:ss</code> from one hour. A time that is not finite or negative shows as 0:00.
     * <p>
     * 시간을 <code>m:ss</code>, 한 시간부터는 <code>h:mm:ss</code> 로 만든다. 유한하지 않거나 음수인 시간은 0:00 으로 보인다.
     *
     * @param seconds the time / 시간
     * @return the text / 문자열
     */
    public static String formatTime(double seconds) {
        long s = Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0 ? 0 : (long) Math.floor(seconds);
        long h = s / 3600;
        long m = (s / 60) % 60;
        long r = s % 60;
        // 한 시간 이상이면 시간 단위도 보여 준다.
        if (h > 0) {
            return h + ":" + (m < 10 ? "0" : "") + m + ":" + (r < 10 ? "0" : "") + r;
        }
        return m + ":" + (r < 10 ? "0" : "") + r;
    }

    /**
     * Returns the text of the time label: the position, and the length when it is known (<code>1:05 / 3:20</code>).
     * <p>
     * 시간 표시의 문자열을 반환한다: 위치, 그리고 길이를 알면 길이(<code>1:05 / 3:20</code>).
     *
     * @param state the state of the bar / 막대의 상태
     * @return the text / 문자열
     */
    public static String timeText(MediaControlState state) {
        double d = state.getDuration();
        String now = formatTime(state.getCurrentTime());
        // 길이를 알면 위치와 함께, 모르면 위치만 보여 준다.
        if (!Double.isNaN(d) && !Double.isInfinite(d)) {
            return now + " / " + formatTime(d);
        }
        return Double.isNaN(d) ? now + " / 0:00" : now;
    }
}
