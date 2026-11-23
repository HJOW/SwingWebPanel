package org.duckdns.hjow.swingwebpanel.media.vtt;

/**
 * A WebVTT cue (WebVTT 3.2/3.3 and the <code>VTTCue</code> interface of 9.1, with the text track cue of HTML 4.8.11.11.1): the identifier,
 * the times, the pause-on-exit flag, the cue settings and the raw cue text. The fields are public because the class is a plain record that the
 * file parser fills and the script interface checks; the defaults are those of the file parser and of the constructor of <code>VTTCue</code>.
 * The values that can be <code>auto</code> (<code>line</code>, <code>position</code>) are {@link #AUTO} (not-a-number, which a real value never
 * is) in that case.
 * <p>
 * WebVTT cue(WebVTT 3.2/3.3 과 9.1 의 <code>VTTCue</code> 인터페이스, HTML 4.8.11.11.1 의 텍스트 트랙 cue 포함): 식별자, 시각, pause-on-exit 표시,
 * cue 설정, 원문 cue 텍스트. 클래스는 파일 파서가 채우고 스크립트 인터페이스가 검사하는 단순한 기록이므로 필드는 public 이다. 기본값은 파일 파서와
 * <code>VTTCue</code> 생성자의 값이다. <code>auto</code> 가 될 수 있는 값(<code>line</code>, <code>position</code>)은 그때 {@link #AUTO}(실제 값이
 * 될 수 없는 NaN)이다.
 */
public final class VttCue {

    /**
     * The value of <code>line</code> and <code>position</code> that stands for the keyword <code>auto</code>.
     * <p>
     * 키워드 <code>auto</code> 를 나타내는 <code>line</code>, <code>position</code> 의 값.
     */
    public static final double AUTO = Double.NaN;

    /**
     * The identifier of the cue (empty if it has none).
     * <p>
     * cue 의 식별자(없으면 빈 문자열).
     */
    public String id = "";

    /**
     * The start time in seconds.
     * <p>
     * 시작 시각(초).
     */
    public double startTime;

    /**
     * The end time in seconds, positive infinity for an unbounded cue.
     * <p>
     * 끝 시각(초). 끝이 없는 cue 는 양의 무한대.
     */
    public double endTime;

    /**
     * The pause-on-exit flag: playback pauses when the end of the cue is reached.
     * <p>
     * pause-on-exit 표시: cue 의 끝에 닿으면 재생이 멈춘다.
     */
    public boolean pauseOnExit;

    /**
     * The region of the cue, or null.
     * <p>
     * cue 의 region. 없으면 null.
     */
    public VttRegion region;

    /**
     * The writing direction: the empty string for horizontal, <code>rl</code> (vertical growing left) or <code>lr</code> (vertical growing right).
     * <p>
     * 쓰기 방향: 가로는 빈 문자열, 아니면 <code>rl</code>(세로, 왼쪽으로 자람) 또는 <code>lr</code>(세로, 오른쪽으로 자람).
     */
    public String vertical = "";

    /**
     * The snap-to-lines flag: true if <code>line</code> is a line number, false if it is a percentage.
     * <p>
     * snap-to-lines 표시: <code>line</code> 이 줄 번호이면 true, 백분율이면 false.
     */
    public boolean snapToLines = true;

    /**
     * The line, or {@link #AUTO}.
     * <p>
     * 줄. 또는 {@link #AUTO}.
     */
    public double line = AUTO;

    /**
     * The line alignment: <code>start</code>, <code>center</code> or <code>end</code>.
     * <p>
     * 줄 정렬: <code>start</code>, <code>center</code>, <code>end</code>.
     */
    public String lineAlign = "start";

    /**
     * The position as a percentage, or {@link #AUTO}.
     * <p>
     * 백분율의 위치. 또는 {@link #AUTO}.
     */
    public double position = AUTO;

    /**
     * The position alignment: <code>line-left</code>, <code>center</code>, <code>line-right</code> or <code>auto</code>.
     * <p>
     * 위치 정렬: <code>line-left</code>, <code>center</code>, <code>line-right</code>, <code>auto</code>.
     */
    public String positionAlign = "auto";

    /**
     * The size as a percentage of the video, 0 to 100.
     * <p>
     * 비디오에 대한 백분율의 크기(0에서 100).
     */
    public double size = 100;

    /**
     * The text alignment: <code>start</code>, <code>center</code>, <code>end</code>, <code>left</code> or <code>right</code>.
     * <p>
     * 텍스트 정렬: <code>start</code>, <code>center</code>, <code>end</code>, <code>left</code>, <code>right</code>.
     */
    public String align = "center";

    /**
     * The raw cue text.
     * <p>
     * 원문 cue 텍스트.
     */
    public String text = "";

    /**
     * Creates a cue with the defaults of the file parser.
     * <p>
     * 파일 파서의 기본값으로 cue 를 만든다.
     */
    public VttCue() {
    }

    /**
     * Tells whether a value of <code>line</code> or <code>position</code> is the keyword <code>auto</code>.
     * <p>
     * <code>line</code>, <code>position</code> 의 값이 키워드 <code>auto</code> 인지 알려 준다.
     *
     * @param value the value / 값
     * @return true for auto / auto 이면 true
     */
    public static boolean isAuto(double value) {
        return Double.isNaN(value);
    }
}
