package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the control bar of the <code>audio</code> element (plan item P18-08; FEATURE_MATRIX AUD-10): the layout at different widths
 * (a narrow bar drops the volume slider, then the time label and the mute button, then the seek bar), the part under a point, what a
 * point on a slider means (also outside of the bar while dragging), the time text, and the painted pixels in the states of the bar (paused,
 * playing, muted, a longer position, a loading element). The layout is the one that the painter and the mouse handling both use, so these
 * checks tie the picture to the hit areas.
 * <p>
 * <code>audio</code> 요소의 컨트롤 막대 시험(계획 P18-08; FEATURE_MATRIX AUD-10): 너비에 따른 배치(좁은 막대는 음량 슬라이더, 그다음 시간 표시와 음소거
 * 버튼, 그다음 탐색 막대를 뺀다), 한 점 아래의 부분, 슬라이더 위의 점이 뜻하는 것(끌 때는 막대 밖에서도), 시간 문자열, 막대의 상태(일시정지, 재생 중,
 * 음소거, 더 긴 위치, 로드 중인 요소)에서 그려진 픽셀. 배치는 그리기와 마우스 처리가 함께 쓰는 것이므로 이 확인은 그림을 클릭 영역에 묶는다.
 */
public class MediaControlsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaControlsTest() {
    }

    /**
     * Paints a bar into an image and returns a hash of its pixels.
     * <p>
     * 막대를 이미지에 그리고 그 픽셀의 해시를 반환한다.
     *
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param state the state / 상태
     * @return the hash / 해시
     */
    private static long paint(int w, int h, MediaControlState state) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        MediaControlsPainter.paint(g, 0, 0, w, h, state);
        g.dispose();
        long hash = 1125899906842597L;
        // 픽셀을 하나씩 섞어 해시를 만든다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                hash = 31 * hash + img.getRGB(x, y);
            }
        }
        return hash;
    }

    /**
     * The default bar of 300 x 54 pixels shows all the parts, in this order from the left, without overlapping.
     * <p>
     * 300 x 54 픽셀의 기본 막대는 모든 부분을 왼쪽부터 이 순서로, 겹치지 않게 보여 준다.
     */
    public void testDefaultBarLayout() {
        MediaControls.Layout l = MediaControls.layout(300, 54);
        Check.notNull(l.time, "the time label");
        Check.notNull(l.seek, "the seek bar");
        Check.notNull(l.mute, "the mute button");
        Check.notNull(l.volume, "the volume slider");
        Check.isTrue(l.play[0] + l.play[2] <= l.time[0], "the play button is left of the time label");
        Check.isTrue(l.time[0] + l.time[2] <= l.seek[0], "the time label is left of the seek bar");
        Check.isTrue(l.seek[0] + l.seek[2] <= l.mute[0], "the seek bar is left of the mute button");
        Check.isTrue(l.mute[0] + l.mute[2] <= l.volume[0], "the mute button is left of the volume slider");
        Check.isTrue(l.volume[0] + l.volume[2] <= 300, "everything is inside the bar");
        // 모든 부분이 막대의 높이 안에 있는지 확인한다.
        double[][] parts = { l.play, l.time, l.seek, l.mute, l.volume };
        for (double[] p : parts) {
            Check.isTrue(p[1] >= 0 && p[1] + p[3] <= 54, "inside the height of the bar");
        }
    }

    /**
     * A narrow bar drops the volume slider first, then the time label and the mute button, then the seek bar, and always keeps the play
     * button.
     * <p>
     * 좁은 막대는 음량 슬라이더를 먼저 빼고, 그다음 시간 표시와 음소거 버튼, 그다음 탐색 막대를 빼며, 재생 버튼은 항상 남긴다.
     */
    public void testNarrowBarsDropParts() {
        MediaControls.Layout wide = MediaControls.layout(240, 54);
        Check.isNull(wide.volume, "no volume slider at 240");
        Check.notNull(wide.mute, "the mute button stays at 240");
        Check.notNull(wide.time, "the time label stays at 240");
        MediaControls.Layout medium = MediaControls.layout(160, 54);
        Check.isNull(medium.time, "no time label at 160");
        Check.notNull(medium.mute, "the mute button stays at 160");
        MediaControls.Layout narrow = MediaControls.layout(100, 54);
        Check.isNull(narrow.mute, "no mute button at 100");
        Check.notNull(narrow.seek, "a short seek bar at 100");
        MediaControls.Layout tiny = MediaControls.layout(50, 54);
        Check.isNull(tiny.seek, "no seek bar at 50");
        Check.notNull(tiny.play, "the play button is always there");
        Check.equal(MediaControls.PART_PLAY, MediaControls.partAt(50, 54, 20, 27), "the play button of a tiny bar");
        Check.equal(MediaControls.PART_NONE, MediaControls.partAt(50, 54, 45, 27), "nothing else in a tiny bar");
    }

    /**
     * The part under a point: the buttons, the time label, the sliders with their whole height, and nothing outside of the bar.
     * <p>
     * 한 점 아래의 부분: 버튼, 시간 표시, 전체 높이의 슬라이더, 막대 밖은 없음.
     */
    public void testPartAt() {
        MediaControls.Layout l = MediaControls.layout(300, 54);
        Check.equal(MediaControls.PART_PLAY, MediaControls.partAt(300, 54, l.play[0] + l.play[2] / 2, l.play[1] + l.play[3] / 2), "play");
        Check.equal(MediaControls.PART_TIME, MediaControls.partAt(300, 54, l.time[0] + 4, l.time[1] + 4), "time");
        Check.equal(MediaControls.PART_SEEK, MediaControls.partAt(300, 54, l.seek[0] + l.seek[2] / 2, l.seek[1] + 2), "seek on the track");
        Check.equal(MediaControls.PART_SEEK, MediaControls.partAt(300, 54, l.seek[0] + l.seek[2] / 2, 3), "seek at the top of the bar");
        Check.equal(MediaControls.PART_SEEK, MediaControls.partAt(300, 54, l.seek[0] + l.seek[2] / 2, 50), "seek at the bottom of the bar");
        Check.equal(MediaControls.PART_MUTE, MediaControls.partAt(300, 54, l.mute[0] + l.mute[2] / 2, l.mute[1] + l.mute[3] / 2), "mute");
        Check.equal(MediaControls.PART_VOLUME, MediaControls.partAt(300, 54, l.volume[0] + l.volume[2] / 2, 27), "volume");
        Check.equal(MediaControls.PART_NONE, MediaControls.partAt(300, 54, -1, 20), "left of the bar");
        Check.equal(MediaControls.PART_NONE, MediaControls.partAt(300, 54, 20, 54), "below the bar");
        Check.equal(MediaControls.PART_NONE, MediaControls.partAt(300, 54, 299.5, 1), "the corner is the background");
    }

    /**
     * A point on a slider is a fraction from 0 to 1 that is limited at both ends, also for a point outside of the bar (while dragging); a
     * part that is not shown has no fraction.
     * <p>
     * 슬라이더 위의 점은 0 에서 1 사이의 비율이며 양 끝에서 제한된다. 막대 밖의 점(끌 때)도 그렇다. 보이지 않는 부분에는 비율이 없다.
     */
    public void testFractions() {
        MediaControls.Layout l = MediaControls.layout(300, 54);
        double mid = l.seek[0] + l.seek[2] / 2;
        Check.near(0.5, MediaControls.fractionAt(300, 54, MediaControls.PART_SEEK, mid), 1e-9, "the middle of the seek bar");
        Check.near(0.0, MediaControls.fractionAt(300, 54, MediaControls.PART_SEEK, -500), 1e-9, "far left");
        Check.near(1.0, MediaControls.fractionAt(300, 54, MediaControls.PART_SEEK, 5000), 1e-9, "far right");
        Check.near(0.25, MediaControls.fractionAt(300, 54, MediaControls.PART_VOLUME, l.volume[0] + l.volume[2] / 4), 1e-9, "a quarter of the volume");
        Check.near(0.0, MediaControls.fractionAt(50, 54, MediaControls.PART_SEEK, 30), 1e-9, "a seek bar that is not shown");
        Check.near(0.0, MediaControls.fractionAt(300, 54, MediaControls.PART_PLAY, 30), 1e-9, "a button has no fraction");
    }

    /**
     * The time label: <code>m:ss</code> and <code>h:mm:ss</code>, the position with the length when it is known, only the position for an
     * unknown length, and 0:00 for values that are not times.
     * <p>
     * 시간 표시: <code>m:ss</code> 와 <code>h:mm:ss</code>, 길이를 알면 위치와 길이, 길이를 모르면 위치만, 시간이 아닌 값은 0:00.
     */
    public void testTimeText() {
        Check.equal("0:00", MediaControls.formatTime(0), "zero");
        Check.equal("0:09", MediaControls.formatTime(9.99), "seconds are cut, not rounded");
        Check.equal("1:05", MediaControls.formatTime(65), "a minute");
        Check.equal("59:59", MediaControls.formatTime(3599.5), "just below an hour");
        Check.equal("1:01:01", MediaControls.formatTime(3661), "an hour");
        Check.equal("0:00", MediaControls.formatTime(Double.NaN), "NaN");
        Check.equal("0:00", MediaControls.formatTime(-4), "negative");
        Check.equal("0:00", MediaControls.formatTime(Double.POSITIVE_INFINITY), "infinity");
        Check.equal("1:05 / 3:20", MediaControls.timeText(new MediaControlState(true, false, false, 65, 200, 1, false)), "position and length");
        Check.equal("0:12", MediaControls.timeText(new MediaControlState(true, false, false, 12, Double.POSITIVE_INFINITY, 1, false)), "an unknown length");
        Check.equal("0:00 / 0:00", MediaControls.timeText(MediaControlState.IDLE), "before the metadata");
    }

    /**
     * The picture follows the state: pausing, muting, the position and loading each change the painted pixels, an equal state paints the same
     * pixels, and a very small bar and a high bar paint without an error.
     * <p>
     * 그림은 상태를 따른다: 일시정지, 음소거, 위치, 로드 중은 각각 그려진 픽셀을 바꾸고, 같은 상태는 같은 픽셀을 그리며, 아주 작은 막대와 높은 막대도 오류
     * 없이 그려진다.
     */
    public void testPaintFollowsTheState() {
        MediaControlState paused = new MediaControlState(true, true, false, 10, 100, 1, false);
        long pausedHash = paint(300, 54, paused);
        Check.equal(pausedHash, paint(300, 54, new MediaControlState(true, true, false, 10, 100, 1, false)), "an equal state paints equal pixels");
        Check.isTrue(pausedHash != paint(300, 54, new MediaControlState(true, false, false, 10, 100, 1, false)), "playing shows the pause symbol");
        Check.isTrue(pausedHash != paint(300, 54, new MediaControlState(true, true, false, 10, 100, 1, true)), "muted shows the slash and an empty volume");
        Check.isTrue(pausedHash != paint(300, 54, new MediaControlState(true, true, false, 60, 100, 1, false)), "a later position moves the handle");
        Check.isTrue(pausedHash != paint(300, 54, new MediaControlState(true, true, false, 10, 100, 0.3, false)), "another volume moves the handle");
        Check.isTrue(pausedHash != paint(300, 54, MediaControlState.IDLE), "a loading element shows a dimmed seek bar");
        Check.isTrue(paint(300, 54, new MediaControlState(true, true, true, 100, 100, 1, false)) != paint(300, 54, new MediaControlState(true, false, false, 100, 100, 1, false)),
                "an ended element shows the play symbol again");
        paint(20, 10, paused);
        paint(1000, 200, paused);
    }
}
