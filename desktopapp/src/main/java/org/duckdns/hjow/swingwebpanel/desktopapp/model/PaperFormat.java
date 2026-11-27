package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.Locale;

/**
 * The paper of the page setup as a setting (plan item P31-10): turns a <code>java.awt.print.PageFormat</code> into the text that the
 * settings file keeps and back. The text is <code>width height left top right bottom orientation</code> with lengths in points
 * (1/72 inch) of the page as it is read (a landscape page is wider than high) and the orientation <code>portrait</code>,
 * <code>landscape</code> or <code>reverse-landscape</code>. Text that cannot be understood gives the default paper: A4, or Letter where
 * that is the usual paper, in portrait orientation with margins of half an inch. The class does not talk to a printer.
 * <p>
 * 설정으로서의 페이지 설정 용지(계획 P31-10): <code>java.awt.print.PageFormat</code> 을 설정 파일이 보관하는 글로, 그리고 그 반대로 바꾼다. 글은
 * <code>너비 높이 왼쪽 위 오른쪽 아래 방향</code> 이며, 길이는 읽는 방향 그대로의 쪽(가로 방향 쪽은 높이보다 넓다)의 포인트(1/72 인치)이고 방향은
 * <code>portrait</code>, <code>landscape</code>, <code>reverse-landscape</code> 중 하나이다. 이해할 수 없는 글은 기본 용지를 준다: A4(Letter 가
 * 보통인 곳에서는 Letter), 세로 방향, 여백 반 인치. 이 클래스는 프린터와 통신하지 않는다.
 */
public final class PaperFormat {

    /** The margin of the default paper in points: half an inch. / 기본 용지의 여백(포인트): 반 인치. */
    private static final double DEFAULT_MARGIN = 36;

    /** The smallest side of a paper that is accepted, in points. / 받아들이는 용지의 가장 짧은 변(포인트). */
    private static final double MIN_SIDE = 72;

    /** The largest side of a paper that is accepted, in points. / 받아들이는 용지의 가장 긴 변(포인트). */
    private static final double MAX_SIDE = 14400;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private PaperFormat() {
    }

    /**
     * Returns the default paper for a locale: Letter in the United States, Canada and Mexico, A4 elsewhere; portrait, margins of half
     * an inch.
     * <p>
     * 로캘의 기본 용지를 반환한다: 미국, 캐나다, 멕시코에서는 Letter, 그 밖에는 A4. 세로 방향, 여백 반 인치.
     *
     * @param locale the locale, or null for the default locale / 로캘. 기본 로캘이면 null
     * @return a new format / 새 형식
     */
    public static PageFormat defaultFormat(Locale locale) {
        String country = (locale == null ? Locale.getDefault() : locale).getCountry();
        boolean letter = "US".equals(country) || "CA".equals(country) || "MX".equals(country);
        double w = letter ? 612 : 595.28;
        double h = letter ? 792 : 841.89;
        return make(w, h, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, PageFormat.PORTRAIT);
    }

    /**
     * Makes a format from the size and the margins of the page as it is read.
     * <p>
     * 읽는 방향 그대로의 쪽의 크기와 여백으로 형식을 만든다.
     *
     * @param width       the width of the page in points / 쪽의 너비(포인트)
     * @param height      the height of the page in points / 쪽의 높이(포인트)
     * @param left        the left margin / 왼쪽 여백
     * @param top         the top margin / 위쪽 여백
     * @param right       the right margin / 오른쪽 여백
     * @param bottom      the bottom margin / 아래쪽 여백
     * @param orientation <code>PageFormat.PORTRAIT</code>, <code>LANDSCAPE</code> or <code>REVERSE_LANDSCAPE</code> /
     *                    <code>PageFormat.PORTRAIT</code>, <code>LANDSCAPE</code>, <code>REVERSE_LANDSCAPE</code> 중 하나
     * @return the format / 형식
     */
    public static PageFormat make(double width, double height, double left, double top, double right, double bottom, int orientation) {
        double areaWidth = Math.max(1, width - left - right);
        double areaHeight = Math.max(1, height - top - bottom);
        Paper p = new Paper();
        PageFormat f = new PageFormat();
        // 용지는 항상 세로로 놓인 것으로 적고, 방향이 그것을 돌린다. 쪽 영역을 용지의 좌표로 옮겨 적는다.
        if (orientation == PageFormat.LANDSCAPE) {
            p.setSize(height, width);
            p.setImageableArea(top, right, areaHeight, areaWidth);
        } else if (orientation == PageFormat.REVERSE_LANDSCAPE) {
            p.setSize(height, width);
            p.setImageableArea(bottom, left, areaHeight, areaWidth);
        } else {
            p.setSize(width, height);
            p.setImageableArea(left, top, areaWidth, areaHeight);
        }
        f.setOrientation(orientation);
        f.setPaper(p);
        return f;
    }

    /**
     * Writes a format as the text of the setting.
     * <p>
     * 형식을 설정의 글로 적는다.
     *
     * @param f the format / 형식
     * @return the text / 글
     */
    public static String format(PageFormat f) {
        double right = f.getWidth() - f.getImageableX() - f.getImageableWidth();
        double bottom = f.getHeight() - f.getImageableY() - f.getImageableHeight();
        String orientation = f.getOrientation() == PageFormat.LANDSCAPE ? "landscape"
                : f.getOrientation() == PageFormat.REVERSE_LANDSCAPE ? "reverse-landscape" : "portrait";
        return number(f.getWidth()) + " " + number(f.getHeight()) + " " + number(f.getImageableX()) + " " + number(f.getImageableY()) + " "
                + number(Math.max(0, right)) + " " + number(Math.max(0, bottom)) + " " + orientation;
    }

    /**
     * Writes a length with at most two decimals, without a locale.
     * <p>
     * 길이를 로캘과 무관하게 소수 둘째 자리까지 적는다.
     *
     * @param v the length / 길이
     * @return the text / 글
     */
    private static String number(double v) {
        double rounded = Math.round(v * 100) / 100.0;
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }

    /**
     * Reads the text of the setting.
     * <p>
     * 설정의 글을 읽는다.
     *
     * @param text   the text, may be null or empty / 글. null 이나 빈 글일 수 있다
     * @param locale the locale of the default paper, or null / 기본 용지의 로캘 또는 null
     * @return the format; the default paper if the text cannot be understood / 형식. 글을 이해할 수 없으면 기본 용지
     */
    public static PageFormat parse(String text, Locale locale) {
        String[] parts = text == null ? new String[0] : text.trim().split("\\s+");
        if (parts.length != 7) {
            return defaultFormat(locale);
        }
        double[] v = new double[6];
        try {
            // 앞의 여섯 값은 길이이다.
            for (int i = 0; i < 6; i++) {
                v[i] = Double.parseDouble(parts[i]);
                if (Double.isNaN(v[i]) || Double.isInfinite(v[i]) || v[i] < 0) {
                    return defaultFormat(locale);
                }
            }
        } catch (NumberFormatException e) {
            return defaultFormat(locale);
        }
        int orientation;
        if ("portrait".equals(parts[6])) {
            orientation = PageFormat.PORTRAIT;
        } else if ("landscape".equals(parts[6])) {
            orientation = PageFormat.LANDSCAPE;
        } else if ("reverse-landscape".equals(parts[6])) {
            orientation = PageFormat.REVERSE_LANDSCAPE;
        } else {
            return defaultFormat(locale);
        }
        // 말이 안 되는 크기나, 쪽을 다 차지하는 여백은 받지 않는다.
        if (v[0] < MIN_SIDE || v[1] < MIN_SIDE || v[0] > MAX_SIDE || v[1] > MAX_SIDE || v[2] + v[4] >= v[0] || v[3] + v[5] >= v[1]) {
            return defaultFormat(locale);
        }
        return make(v[0], v[1], v[2], v[3], v[4], v[5], orientation);
    }

    /**
     * Describes a format for the user: the size in millimeters and the orientation.
     * <p>
     * 형식을 사용자에게 설명한다: 밀리미터 단위의 크기와 방향.
     *
     * @param f the format / 형식
     * @return for example <code>210 x 297 mm</code> / 예: <code>210 x 297 mm</code>
     */
    public static String describe(PageFormat f) {
        return Math.round(f.getWidth() * 25.4 / 72) + " x " + Math.round(f.getHeight() * 25.4 / 72) + " mm";
    }
}
