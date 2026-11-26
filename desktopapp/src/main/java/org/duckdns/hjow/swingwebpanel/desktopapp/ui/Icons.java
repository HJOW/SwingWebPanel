package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import javax.swing.Icon;

/**
 * The icons of the toolbar buttons, drawn with Java2D so that the application needs no image files and no particular font: back, forward,
 * reload, stop, home and the small cross that closes a tab. A disabled component gets a grey icon.
 * <p>
 * 도구 모음 버튼의 아이콘. Java2D 로 그리므로 응용 프로그램에 이미지 파일이나 특정 글꼴이 필요 없다: 뒤로, 앞으로, 새로고침, 멈춤, 홈, 그리고 탭을
 * 닫는 작은 가위표. 비활성 컴포넌트에는 회색 아이콘을 그린다.
 */
final class Icons {

    /** Arrow to the left. / 왼쪽 화살표. */
    static final int BACK = 0;

    /** Arrow to the right. / 오른쪽 화살표. */
    static final int FORWARD = 1;

    /** Circular arrow of the reload button. / 새로고침 버튼의 둥근 화살표. */
    static final int RELOAD = 2;

    /** Cross of the stop button. / 멈춤 버튼의 가위표. */
    static final int STOP = 3;

    /** House of the home button. / 홈 버튼의 집 모양. */
    static final int HOME = 4;

    /** Small cross of a tab. / 탭의 작은 가위표. */
    static final int CLOSE = 5;

    /** Plus sign of the new tab button. / 새 탭 버튼의 더하기 표시. */
    static final int PLUS = 6;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private Icons() {
    }

    /**
     * Returns the icon of a kind in the size of a toolbar button.
     * <p>
     * 종류에 맞는 아이콘을 도구 모음 버튼 크기로 반환한다.
     *
     * @param kind one of the constants of this class / 이 클래스의 상수 중 하나
     * @return the icon / 아이콘
     */
    static Icon of(int kind) {
        return new Drawn(kind, kind == CLOSE || kind == PLUS ? 10 : 16);
    }

    /**
     * An icon that draws one of the shapes.
     * <p>
     * 모양 하나를 그리는 아이콘.
     */
    private static final class Drawn implements Icon {

        /** The kind of the shape. / 모양의 종류. */
        private final int kind;

        /** The width and height in pixels. / 너비와 높이(픽셀). */
        private final int size;

        /**
         * Creates the icon.
         * <p>
         * 아이콘을 만든다.
         *
         * @param kind the kind of the shape / 모양의 종류
         * @param size the width and height in pixels / 너비와 높이(픽셀)
         */
        Drawn(int kind, int size) {
            this.kind = kind;
            this.size = size;
        }

        /**
         * Returns the width.
         * <p>
         * 너비를 반환한다.
         *
         * @return the width in pixels / 너비(픽셀)
         */
        public int getIconWidth() {
            return size;
        }

        /**
         * Returns the height.
         * <p>
         * 높이를 반환한다.
         *
         * @return the height in pixels / 높이(픽셀)
         */
        public int getIconHeight() {
            return size;
        }

        /**
         * Draws the shape, dark for an enabled component and grey for a disabled one.
         * <p>
         * 모양을 그린다. 활성 컴포넌트에는 어둡게, 비활성 컴포넌트에는 회색으로 그린다.
         *
         * @param c the component that shows the icon / 아이콘을 보이는 컴포넌트
         * @param g the graphics / 그래픽스
         * @param x the left edge / 왼쪽 가장자리
         * @param y the top edge / 위쪽 가장자리
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.setColor(c == null || c.isEnabled() ? new Color(0x33, 0x33, 0x33) : new Color(0xB0, 0xB0, 0xB0));
                float s = size;
                g2.setStroke(new BasicStroke(s / 8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // 종류마다 다른 모양을 그린다.
                if (kind == BACK || kind == FORWARD) {
                    GeneralPath p = new GeneralPath();
                    float tip = kind == BACK ? s * 0.2f : s * 0.8f;
                    float tail = kind == BACK ? s * 0.8f : s * 0.2f;
                    float wing = s * 0.5f;
                    p.moveTo(tail, s * 0.5f);
                    p.lineTo(tip, s * 0.5f);
                    p.moveTo(wing, s * 0.2f);
                    p.lineTo(tip, s * 0.5f);
                    p.lineTo(wing, s * 0.8f);
                    g2.draw(p);
                } else if (kind == RELOAD) {
                    g2.draw(new Arc2D.Float(s * 0.2f, s * 0.2f, s * 0.6f, s * 0.6f, 60, 280, Arc2D.OPEN));
                    GeneralPath head = new GeneralPath();
                    head.moveTo(s * 0.62f, s * 0.08f);
                    head.lineTo(s * 0.68f, s * 0.30f);
                    head.lineTo(s * 0.88f, s * 0.20f);
                    g2.draw(head);
                } else if (kind == STOP || kind == CLOSE) {
                    float a = kind == STOP ? s * 0.25f : s * 0.2f;
                    g2.draw(new Line2D.Float(a, a, s - a, s - a));
                    g2.draw(new Line2D.Float(s - a, a, a, s - a));
                } else if (kind == PLUS) {
                    g2.draw(new Line2D.Float(s * 0.5f, s * 0.15f, s * 0.5f, s * 0.85f));
                    g2.draw(new Line2D.Float(s * 0.15f, s * 0.5f, s * 0.85f, s * 0.5f));
                } else {
                    GeneralPath house = new GeneralPath();
                    house.moveTo(s * 0.15f, s * 0.5f);
                    house.lineTo(s * 0.5f, s * 0.18f);
                    house.lineTo(s * 0.85f, s * 0.5f);
                    house.moveTo(s * 0.27f, s * 0.45f);
                    house.lineTo(s * 0.27f, s * 0.82f);
                    house.lineTo(s * 0.73f, s * 0.82f);
                    house.lineTo(s * 0.73f, s * 0.45f);
                    g2.draw(house);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
