package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the drawing of form controls (plan items P11-05 and P09-05): check boxes, radio buttons, the labels of buttons, the text of text
 * fields and text areas, drop-downs, list boxes, progress bars, meters and sliders are drawn from the live state of the elements, and the
 * snapshot that the Swing editors use gives the state and the geometry of the control.
 * <p>
 * 폼 컨트롤 그리기 시험(계획 P11-05, P09-05): 체크박스, 라디오 단추, 단추의 라벨, 텍스트 필드와 텍스트 영역의 글, 드롭다운, 리스트 박스, 진행 막대, 미터, 슬라이더가
 * 요소의 살아 있는 상태로 그려지고, Swing 편집기가 쓰는 스냅샷이 컨트롤의 상태와 위치를 알려 준다.
 */
public class ControlPaintTest {

    /** The accent color of checked controls. 체크된 컨트롤의 강조색. */
    private static final int ACCENT = 0x0075FF;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ControlPaintTest() {
    }

    /**
     * Renders a page and returns it.
     * <p>
     * 페이지를 렌더링해 반환한다.
     *
     * @param body the body HTML / body HTML
     * @return the rendered page / 렌더링된 페이지
     */
    private static RenderedPage page(String body) {
        return RenderTestSupport.page(body, 400, 300, null);
    }

    /**
     * Counts the pixels in an area that are not white (light text such as a placeholder is not counted by the dark-pixel count).
     * <p>
     * 영역에서 흰색이 아닌 픽셀 수를 센다(placeholder 같은 옅은 글은 어두운 픽셀 수로는 세어지지 않는다).
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(제외)
     * @param y1  bottom (exclusive) / 아래쪽(제외)
     * @return the count / 개수
     */
    private static int nonWhite(BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 흰색이 아닌 것을 센다.
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the content box of an element.
     * <p>
     * 요소의 content box 를 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the element id / 요소 id
     * @return the box / 상자
     */
    private static LayoutBox box(RenderedPage p, String id) {
        return RenderTestSupport.box(p, id);
    }

    /**
     * Returns the snapshot of an element.
     * <p>
     * 요소의 스냅샷을 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the element id / 요소 id
     * @return the snapshot / 스냅샷
     */
    private static ControlSnapshot snap(RenderedPage p, String id) {
        Element e = RenderTestSupport.lastDocument.getElementById(id);
        ControlSnapshot s = p.getControl(e);
        Check.notNull(s, "control #" + id + " has a snapshot");
        return s;
    }

    /**
     * Checks that a check box shows its checkedness, indeterminate state and disabled state.
     * <p>
     * 체크박스가 체크 상태, indeterminate 상태, 비활성 상태를 보여 주는지 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testCheckBox() throws Exception {
        RenderedPage p = page("<input id=a type=checkbox><input id=b type=checkbox checked><input id=c type=checkbox checked disabled>"
                + "<input id=d type=checkbox disabled>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        LayoutBox b = box(p, "b");
        int ax = (int) a.getContentX();
        int ay = (int) a.getContentY();
        int bx = (int) b.getContentX();
        int by = (int) b.getContentY();
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, ax + 3, ay + 10), "an unchecked box is white inside");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, ax + 6, ay), 0x767676, 40), "and has a gray edge");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, bx + 2, by + 11), ACCENT, 8), "a checked box is filled with the accent color");
        // 체크 표시(흰 선)가 상자 안에 그려졌는지 보기 위해 상자 안에서 흰색에 가까운 픽셀을 찾는다.
        boolean tick = false;
        // 상자 안쪽을 위에서 아래로 훑으며 흰색에 가까운 픽셀을 찾는다.
        for (int y = by + 3; y < by + 11 && !tick; y++) {
            for (int x = bx + 3; x < bx + 11; x++) {
                if (RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0xFFFFFF, 30)) {
                    tick = true;
                    break;
                }
            }
        }
        Check.isTrue(tick, "a checked box has a white tick");
        LayoutBox c = box(p, "c");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) c.getContentX() + 2, (int) c.getContentY() + 11), 0xA5A5A5, 12),
                "a disabled checked box is gray");
        LayoutBox d = box(p, "d");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) d.getContentX() + 6, (int) d.getContentY()), 0xBFBFBF, 40),
                "a disabled box has a light edge");
        Check.isTrue(snap(p, "b").isChecked(), "the snapshot knows that the box is checked");
        Check.isTrue(snap(p, "c").isDisabled(), "and that it is disabled");
    }

    /**
     * Checks that the drawing follows the state of the element after it changed (the display is made from the live state).
     * <p>
     * 요소의 상태가 바뀐 뒤 그림이 그 상태를 따르는지 확인한다(표시는 살아 있는 상태로 만든다).
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testDrawingFollowsTheState() throws Exception {
        RenderedPage p = page("<input id=a type=checkbox>");
        Check.isFalse(snap(p, "a").isChecked(), "unchecked at first");
        ((org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement) RenderTestSupport.lastDocument.getElementById("a")).setChecked(true);
        // 상태를 바꾼 뒤 다시 렌더링하면 새 상태가 그려진다.
        org.duckdns.hjow.swingwebpanel.css.CssEngine css = new org.duckdns.hjow.swingwebpanel.css.CssEngine(RenderTestSupport.lastDocument, null,
                new org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment(400, 300));
        css.loadStyleSheets();
        RenderedPage q = PageRenderer.render(RenderTestSupport.lastDocument, css, null, 400, 300);
        Check.isTrue(q.getControl(RenderTestSupport.lastDocument.getElementById("a")).isChecked(), "checked after the change");
    }

    /**
     * Checks radio buttons: a dot for the checked one.
     * <p>
     * 라디오 단추를 확인한다: 체크된 것에 점이 있다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testRadio() throws Exception {
        RenderedPage p = page("<input id=a type=radio name=r><input id=b type=radio name=r checked>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        LayoutBox b = box(p, "b");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, (int) (a.getContentX() + 6), (int) (a.getContentY() + 6)), "an unchecked radio button is empty");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) (b.getContentX() + 6), (int) (b.getContentY() + 6)), ACCENT, 10),
                "a checked radio button has a dot");
    }

    /**
     * Checks the label of a push button: centered in the box.
     * <p>
     * 누름 단추의 라벨을 확인한다: 상자 안 가운데에 있다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testButtonLabelIsCentered() throws Exception {
        RenderedPage p = page("<input id=a type=button value='OK' style='font:12px monospace;width:100px;height:30px;margin:0;border:0;padding:0;"
                + "background:#fff'>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        int x0 = (int) a.getContentX();
        int y0 = (int) a.getContentY();
        int left = -1;
        int right = -1;
        int top = -1;
        int bottom = -1;
        // 상자 안에서 글자 픽셀의 범위를 찾는다.
        for (int y = y0; y < y0 + 30; y++) {
            for (int x = x0; x < x0 + 100; x++) {
                if (RenderTestSupport.ink(img, x, y, x + 1, y + 1) > 0) {
                    left = left < 0 ? x : Math.min(left, x);
                    right = Math.max(right, x);
                    top = top < 0 ? y : Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        Check.isTrue(left >= 0, "the label is drawn");
        Check.near(50, (left + right) / 2.0 - x0, 3, "centered horizontally");
        Check.near(15, (top + bottom) / 2.0 - y0, 4, "centered vertically");
    }

    /**
     * Checks the text of a text field, its placeholder and a password.
     * <p>
     * 텍스트 필드의 글, placeholder, 암호를 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testTextFields() throws Exception {
        RenderedPage p = page("<input id=a value='hello' style='font:12px monospace'><input id=b style='font:12px monospace'>"
                + "<input id=c placeholder='type here' style='font:12px monospace'><input id=d type=password value='abcd'>"
                + "<input id=e value='x' disabled>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        Check.isTrue(RenderTestSupport.ink(img, (int) a.getContentX(), (int) a.getContentY(), (int) (a.getContentX() + a.getContentWidth()),
                (int) (a.getContentY() + a.getContentHeight())) > 10, "the value is drawn");
        LayoutBox b = box(p, "b");
        Check.equal(0, RenderTestSupport.ink(img, (int) b.getContentX(), (int) b.getContentY(), (int) (b.getContentX() + b.getContentWidth()),
                (int) (b.getContentY() + b.getContentHeight())), "an empty field draws no text");
        LayoutBox c = box(p, "c");
        int placeholderInk = nonWhite(img, (int) c.getContentX(), (int) c.getContentY(), (int) (c.getContentX() + c.getContentWidth()),
                (int) (c.getContentY() + c.getContentHeight()));
        Check.isTrue(placeholderInk > 10, "the placeholder is drawn while the field is empty");
        Check.equal("hello", snap(p, "a").getText(), "the snapshot has the value");
        Check.equal("", snap(p, "c").getText(), "and no value for an empty field");
        Check.equal("type here", snap(p, "c").getPlaceholder(), "and the placeholder");
        Check.equal("" + (char) 0x2022 + (char) 0x2022 + (char) 0x2022 + (char) 0x2022, snap(p, "d").getText(), "a password shows one bullet per character");
    }

    /**
     * Checks that a text area wraps its text at the width of its content.
     * <p>
     * 텍스트 영역이 content 너비에서 글을 줄 바꿈하는지 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testTextAreaWrapsLines() throws Exception {
        RenderedPage p = page("<textarea id=a cols=10 rows=4 style='font:10px monospace;overflow:hidden'>aaa bbb ccc ddd\nzz</textarea>");
        String[] lines = snap(p, "a").getLines();
        Check.isTrue(lines.length >= 3, "the text is wrapped into several lines: " + java.util.Arrays.asList(lines));
        Check.equal("zz", lines[lines.length - 1], "a line break starts a new line");
        double width = box(p, "a").getContentWidth();
        // 줄마다 너비가 content 너비를 넘지 않는지 본다.
        for (int i = 0; i < lines.length; i++) {
            Check.isTrue(TextRenderer.advance(snap(p, "a").getFont(), lines[i].trim()) <= width + 0.01, "line " + i + " fits: " + lines[i]);
        }
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        Check.isTrue(RenderTestSupport.ink(img, (int) a.getContentX(), (int) a.getContentY(), (int) (a.getContentX() + a.getContentWidth()),
                (int) (a.getContentY() + a.getContentHeight())) > 20, "the lines are drawn");
    }

    /**
     * Checks a drop-down: the selected label and the arrow.
     * <p>
     * 드롭다운을 확인한다: 선택된 라벨과 화살표.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testDropdown() throws Exception {
        RenderedPage p = page("<select id=a style='font:12px monospace'><option>one</option><option selected>two</option><option>three</option></select>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        Check.equal("two", snap(p, "a").getText(), "the selected option is shown");
        int x0 = (int) a.getContentX();
        int y0 = (int) a.getContentY();
        int w = (int) a.getContentWidth();
        int h = (int) a.getContentHeight();
        Check.isTrue(RenderTestSupport.ink(img, x0, y0, x0 + w - 18, y0 + h) > 10, "the label is drawn");
        Check.isTrue(RenderTestSupport.ink(img, x0 + w - 18, y0, x0 + w, y0 + h) > 5, "and the arrow");
        Check.equal(ControlSnapshot.Kind.DROPDOWN, snap(p, "a").getKind(), "the kind");
    }

    /**
     * Checks a list box: rows, the highlighted selection, option groups.
     * <p>
     * 리스트 박스를 확인한다: 행, 강조된 선택, optgroup.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testListBox() throws Exception {
        RenderedPage p = page("<select id=a size=3 style='font:12px monospace'><optgroup label='G'><option>one</option><option selected>two</option></optgroup>"
                + "<option disabled>three</option></select>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        ControlSnapshot s = snap(p, "a");
        Check.equal(ControlSnapshot.Kind.LIST_BOX, s.getKind(), "the kind");
        ControlSnapshot.Row[] rows = s.getRows();
        Check.equal(4, rows.length, "the group label and three options");
        Check.isTrue(rows[0].isGroup(), "the first row is the group label");
        Check.isTrue(rows[2].isSelected(), "the selected option is a selected row");
        Check.isTrue(rows[3].isDisabled(), "a disabled option");
        LayoutBox a = box(p, "a");
        int row = 2 - s.getTopRow();
        int x = (int) (a.getContentX() + a.getContentWidth()) - 20;
        int y = (int) (a.getContentY() + row * s.getLineHeight() + 1);
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0xC8C8C8, 6), "the selection of a list without the focus is gray");
    }

    /**
     * Checks a progress bar: the filled part follows the value.
     * <p>
     * 진행 막대를 확인한다: 채운 부분이 값을 따른다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testProgress() throws Exception {
        RenderedPage p = page("<progress id=a value=25 max=100 style='width:100px'></progress><progress id=b style='width:100px'></progress>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        int x0 = (int) a.getContentX();
        int y = (int) (a.getContentY() + a.getContentHeight() / 2);
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, x0 + 10, y), ACCENT, 8), "the filled part is the accent color");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, x0 + 60, y), 0xE9E9E9, 8), "the rest is the track");
        Check.near(0.25, snap(p, "a").getRatio(), 1e-9, "the ratio");
        Check.near(-1, snap(p, "b").getRatio(), 1e-9, "a progress bar without a value is indeterminate");
    }

    /**
     * Checks the color of a meter in its three regions.
     * <p>
     * 미터의 세 영역의 색을 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testMeterRegions() throws Exception {
        RenderedPage p = page("<meter id=a value=50 min=0 max=100 low=30 high=70 optimum=50 style='width:100px'></meter>"
                + "<meter id=b value=10 min=0 max=100 low=30 high=70 optimum=50 style='width:100px'></meter>"
                + "<meter id=c value=90 min=0 max=100 low=30 high=70 optimum=10 style='width:100px'></meter>");
        Check.equal(0, snap(p, "a").getRegion(), "inside the good range");
        Check.equal(1, snap(p, "b").getRegion(), "outside the good range, with the optimum in the middle");
        Check.equal(2, snap(p, "c").getRegion(), "far from an optimum in the low region");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) a.getContentX() + 10, (int) (a.getContentY() + a.getContentHeight() / 2)), 0x2FA84F, 8),
                "green");
        LayoutBox c = box(p, "c");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) c.getContentX() + 10, (int) (c.getContentY() + c.getContentHeight() / 2)), 0xDD4B39, 8),
                "red");
    }

    /**
     * Checks a slider: the thumb is at the position of the value.
     * <p>
     * 슬라이더를 확인한다: 손잡이가 값의 위치에 있다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testRange() throws Exception {
        RenderedPage p = page("<input id=a type=range value=100 style='width:100px'><input id=b type=range value=0 style='width:100px'>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox a = box(p, "a");
        int cy = (int) (a.getContentY() + a.getContentHeight() / 2);
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(img, (int) (a.getContentX() + a.getContentWidth()) - 6, cy), ACCENT, 8),
                "the thumb is at the right end");
        Check.near(1.0, snap(p, "a").getRatio(), 1e-9, "the ratio of 100");
        Check.near(0.0, snap(p, "b").getRatio(), 1e-9, "the ratio of 0");
    }

    /**
     * Checks that the snapshot of a control gives the geometry of the content box in document coordinates.
     * <p>
     * 컨트롤의 스냅샷이 문서 좌표의 content box 위치를 주는지 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testSnapshotGeometry() throws Exception {
        RenderedPage p = page("<div style='margin:30px 0 0 40px'><input id=a style='font:10px monospace;margin:0;border:1px solid;padding:2px' size=5></div>");
        ControlSnapshot s = snap(p, "a");
        LayoutBox b = box(p, "a");
        Check.near(b.getContentX(), s.getX(), 0.01, "x");
        Check.near(b.getContentY(), s.getY(), 0.01, "y");
        Check.near(b.getContentWidth(), s.getWidth(), 0.01, "width");
        Check.near(b.getContentHeight(), s.getHeight(), 0.01, "height");
        Check.near(43, s.getX(), 0.01, "40 + border 1 + padding 2");
        Check.notNull(s.getFont(), "the font");
        Check.equal(0xFF000000, s.getColor(), "the text color");
    }

    /**
     * Checks that the top border of a fieldset is broken where the legend is, and drawn everywhere else.
     * <p>
     * fieldset 의 위쪽 경계선이 legend 가 있는 곳에서 끊기고 다른 곳에는 그려지는지 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testFieldsetBorderIsBrokenAtTheLegend() throws Exception {
        RenderedPage p = page("<div style='height:20px'></div><fieldset id=f style='margin:0;padding:10px;border:2px solid #000'>"
                + "<legend id=l style='padding:0 6px;font:10px monospace'>Title</legend>x</fieldset>");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox f = box(p, "f");
        LayoutBox l = box(p, "l");
        int y = (int) f.getY() + 1;
        Check.equal(0x000000, RenderTestSupport.rgb(img, (int) f.getX() + 3, y), "the border is drawn left of the legend");
        Check.equal(0x000000, RenderTestSupport.rgb(img, (int) (l.getX() + l.getWidth()) + 5, y), "and right of it");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, (int) l.getX() + 2, y), "but not under the legend");
        Check.equal(0x000000, RenderTestSupport.rgb(img, (int) l.getX() + 2, (int) (f.getY() + f.getHeight()) - 1), "the bottom border is whole");
        Check.equal(0x000000, RenderTestSupport.rgb(img, (int) f.getX() + 1, (int) (f.getY() + f.getHeight() / 2)), "and so is the left border");
    }

    /**
     * Checks that an element that is not a drawn control has no snapshot.
     * <p>
     * 그려지는 컨트롤이 아닌 요소에는 스냅샷이 없는지 확인한다.
     *
     * @throws Exception if rendering fails / 렌더링이 실패하면
     */
    public void testNoSnapshotForOtherElements() throws Exception {
        RenderedPage p = page("<div id=a>x</div><input id=h type=hidden><button id=b>go</button>");
        Check.isNull(p.getControl(RenderTestSupport.lastDocument.getElementById("a")), "a div is no control");
        Check.isNull(p.getControl(RenderTestSupport.lastDocument.getElementById("h")), "a hidden input is not drawn");
        Check.isNull(p.getControl(RenderTestSupport.lastDocument.getElementById("b")), "a button element is drawn as a box with its children");
        BufferedImage img = RenderTestSupport.paint(p, 400, 300);
        LayoutBox b = box(p, "b");
        Check.isTrue(RenderTestSupport.ink(img, (int) b.getContentX(), (int) b.getContentY(), (int) (b.getContentX() + b.getContentWidth()),
                (int) (b.getContentY() + b.getContentHeight())) > 5, "the text of a button element is drawn");
    }
}
