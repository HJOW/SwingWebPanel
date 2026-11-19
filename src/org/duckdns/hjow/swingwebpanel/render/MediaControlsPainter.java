package org.duckdns.hjow.swingwebpanel.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Paints the control bar of an <code>audio</code> element with Java2D (plan item P18-08): the background and border, the play or pause
 * symbol, the time label, the seek bar with its progress and handle, the speaker symbol of the mute button, and the volume slider. The
 * positions come from {@link MediaControls#layout(double, double)}, so the picture matches what the mouse hits. The painter changes the
 * Graphics2D state only temporarily.
 * <p>
 * <code>audio</code> 요소의 컨트롤 막대를 Java2D 로 그린다(계획 P18-08): 배경과 경계선, 재생 또는 일시정지 기호, 시간 표시, 진행 상황과 손잡이가 있는
 * 탐색 막대, 음소거 버튼의 스피커 기호, 음량 슬라이더. 위치는 {@link MediaControls#layout(double, double)} 에서 오므로 그림이 마우스가 맞히는 곳과
 * 일치한다. 그리기는 Graphics2D 의 상태를 일시적으로만 바꾼다.
 */
final class MediaControlsPainter {

    /**
     * The background of the bar.
     * <p>
     * 막대의 배경.
     */
    private static final Color BACKGROUND = new Color(0xF1F3F4);

    /**
     * The border of the bar.
     * <p>
     * 막대의 경계선.
     */
    private static final Color BORDER = new Color(0xA0A0A0);

    /**
     * The color of the symbols and the text.
     * <p>
     * 기호와 글자의 색.
     */
    private static final Color INK = new Color(0x3C4043);

    /**
     * The color of the unfilled part of a slider.
     * <p>
     * 슬라이더의 채워지지 않은 부분의 색.
     */
    private static final Color TRACK = new Color(0xC8CCD0);

    /**
     * The color of the filled part of a slider and of its handle.
     * <p>
     * 슬라이더의 채워진 부분과 손잡이의 색.
     */
    private static final Color FILL = new Color(0x1A73E8);

    /**
     * Not instantiated.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private MediaControlsPainter() {
    }

    /**
     * Paints the bar.
     * <p>
     * 막대를 그린다.
     *
     * @param g     the graphics / 그래픽
     * @param x     left of the bar / 막대의 왼쪽
     * @param y     top of the bar / 막대의 위쪽
     * @param w     width of the bar / 막대의 너비
     * @param h     height of the bar / 막대의 높이
     * @param state what the bar shows / 막대가 보여 줄 것
     */
    static void paint(Graphics2D g, double x, double y, double w, double h, MediaControlState state) {
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape oldClip = g.getClip();
        java.awt.Stroke oldStroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.clip(new Rectangle2D.Double(x, y, w, h));
        g.translate(x, y);
        // 바깥 틀: 둥근 배경과 경계선.
        RoundRectangle2D.Double frame = new RoundRectangle2D.Double(0.5, 0.5, Math.max(0, w - 1), Math.max(0, h - 1), 8, 8);
        g.setColor(BACKGROUND);
        g.fill(frame);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.draw(frame);
        MediaControls.Layout l = MediaControls.layout(w, h);
        paintPlay(g, l.play, state);
        // 부분마다 있는 것만 그린다.
        if (l.time != null) {
            g.setColor(INK);
            Font font = new Font("Dialog", Font.PLAIN, 12);
            TextRenderer.draw(g, MediaControls.timeText(state), font, l.time[0], l.time[1] + 12, 0, 0, 0);
        }
        if (l.seek != null) {
            double d = state.getDuration();
            double f = state.isLoaded() && d > 0 && !Double.isInfinite(d) ? Math.max(0, Math.min(1, state.getCurrentTime() / d)) : 0;
            paintSlider(g, l.seek, f, state.isLoaded());
        }
        if (l.mute != null) {
            paintSpeaker(g, l.mute, state.isMuted() || state.getVolume() <= 0);
        }
        if (l.volume != null) {
            paintSlider(g, l.volume, state.isMuted() ? 0 : state.getVolume(), true);
        }
        g.translate(-x, -y);
        g.setStroke(oldStroke);
        g.setClip(oldClip);
        if (aa != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
    }

    /**
     * Paints the play button: a triangle while paused, two bars while playing.
     * <p>
     * 재생 버튼을 그린다: 일시정지이면 삼각형, 재생 중이면 막대 둘.
     *
     * @param g     the graphics / 그래픽
     * @param r     the button (x, y, width, height) / 버튼(x, y, 너비, 높이)
     * @param state the state / 상태
     */
    private static void paintPlay(Graphics2D g, double[] r, MediaControlState state) {
        double cx = r[0] + r[2] / 2;
        double cy = r[1] + r[3] / 2;
        double s = r[2] * 0.36;
        g.setColor(INK);
        // 재생 중이면 일시정지 기호(막대 둘), 아니면 재생 기호(삼각형)를 그린다.
        if (!state.isPaused() && !state.isEnded()) {
            g.fill(new Rectangle2D.Double(cx - s * 0.9, cy - s, s * 0.7, s * 2));
            g.fill(new Rectangle2D.Double(cx + s * 0.2, cy - s, s * 0.7, s * 2));
        } else {
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(cx - s * 0.7, cy - s);
            tri.lineTo(cx - s * 0.7, cy + s);
            tri.lineTo(cx + s * 0.9, cy);
            tri.closePath();
            g.fill(tri);
        }
    }

    /**
     * Paints a slider: the track, the filled part up to a fraction, and the handle.
     * <p>
     * 슬라이더를 그린다: 트랙, 비율까지 채워진 부분, 손잡이.
     *
     * @param g       the graphics / 그래픽
     * @param r       the track (x, y, width, height) / 트랙(x, y, 너비, 높이)
     * @param fraction how much is filled, 0 to 1 / 채워진 정도(0에서 1)
     * @param enabled false to paint it dimmed, without a handle / false 이면 손잡이 없이 흐리게 그린다
     */
    private static void paintSlider(Graphics2D g, double[] r, double fraction, boolean enabled) {
        g.setColor(TRACK);
        g.fill(new RoundRectangle2D.Double(r[0], r[1], r[2], r[3], r[3], r[3]));
        if (!enabled) {
            return;
        }
        double filled = r[2] * fraction;
        g.setColor(FILL);
        g.fill(new RoundRectangle2D.Double(r[0], r[1], filled, r[3], r[3], r[3]));
        double hx = r[0] + filled;
        g.fill(new Ellipse2D.Double(hx - 5, r[1] + r[3] / 2 - 5, 10, 10));
    }

    /**
     * Paints the speaker symbol of the mute button, with a slash when the sound is off.
     * <p>
     * 음소거 버튼의 스피커 기호를 그린다. 소리가 꺼져 있으면 빗금이 있다.
     *
     * @param g    the graphics / 그래픽
     * @param r    the button (x, y, width, height) / 버튼(x, y, 너비, 높이)
     * @param off  true if the sound is off / 소리가 꺼져 있으면 true
     */
    private static void paintSpeaker(Graphics2D g, double[] r, boolean off) {
        double cx = r[0] + r[2] / 2;
        double cy = r[1] + r[3] / 2;
        double u = r[2] / 24.0;
        g.setColor(INK);
        Path2D.Double body = new Path2D.Double();
        body.moveTo(cx - 8 * u, cy - 3 * u);
        body.lineTo(cx - 4 * u, cy - 3 * u);
        body.lineTo(cx + 1 * u, cy - 7 * u);
        body.lineTo(cx + 1 * u, cy + 7 * u);
        body.lineTo(cx - 4 * u, cy + 3 * u);
        body.lineTo(cx - 8 * u, cy + 3 * u);
        body.closePath();
        g.fill(body);
        g.setStroke(new BasicStroke((float) Math.max(1.2, 1.6 * u)));
        // 소리가 켜져 있으면 음파 둘, 꺼져 있으면 빗금을 그린다.
        if (off) {
            g.draw(new Line2D.Double(cx + 4 * u, cy - 4 * u, cx + 10 * u, cy + 4 * u));
            g.draw(new Line2D.Double(cx + 4 * u, cy + 4 * u, cx + 10 * u, cy - 4 * u));
        } else {
            g.draw(new java.awt.geom.Arc2D.Double(cx - 3 * u, cy - 4 * u, 8 * u, 8 * u, -50, 100, java.awt.geom.Arc2D.OPEN));
            g.draw(new java.awt.geom.Arc2D.Double(cx - 3 * u, cy - 8 * u, 16 * u, 16 * u, -50, 100, java.awt.geom.Arc2D.OPEN));
        }
        g.setStroke(new BasicStroke(1f));
    }
}
