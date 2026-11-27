package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The print preview of a browser window (plan item P31-10): it shows the pages of a document as they will be printed, one page at a
 * time on a gray desk, with buttons for the previous and the next page, a list of sizes and a button that prints. The pages are painted
 * by the document itself (the same object that a print job gets), so what is seen is what is printed; the preview never talks to a
 * printer, it only asks the window to print when the user presses the button.
 * <p>
 * 브라우저 창의 인쇄 미리 보기(계획 P31-10): 문서의 쪽을 인쇄될 모습대로, 회색 바탕 위에 한 번에 한 쪽씩 보이고, 이전·다음 쪽 단추, 크기 목록,
 * 인쇄 단추가 있다. 쪽은 문서 자신(인쇄 작업이 받는 것과 같은 객체)이 그리므로 보이는 것이 인쇄되는 것이다. 미리 보기는 프린터와 통신하지
 * 않으며, 사용자가 단추를 누르면 창에 인쇄를 요청할 뿐이다.
 */
final class PrintPreview extends JDialog {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The sizes of the list in percent; 0 fits the page into the window. / 목록의 크기(퍼센트). 0 은 쪽을 창에 맞춘다. */
    private static final int[] SIZES = { 0, 50, 75, 100, 150, 200 };

    /** The space around the paper in pixels. / 용지 둘레의 여백(픽셀). */
    private static final int DESK = 16;

    /** The document that is shown. / 보이는 문서. */
    private final transient Pageable document;

    /** What the print button does. / 인쇄 단추가 하는 일. */
    private final transient Runnable printAction;

    /** The page that is shown, starting at 0. / 보이는 쪽(0 부터). */
    private int page;

    /** The chosen size in percent, 0 for fitting. / 고른 크기(퍼센트). 맞추기이면 0. */
    private int size;

    /** Shows the number of the page and the number of pages. / 쪽 번호와 쪽 수를 보인다. */
    private final JLabel position = new JLabel();

    /** Goes to the previous page. / 이전 쪽으로 간다. */
    private final JButton previous = new JButton(Texts.of("Previous", "이전"));

    /** Goes to the next page. / 다음 쪽으로 간다. */
    private final JButton next = new JButton(Texts.of("Next", "다음"));

    /** The list of sizes. / 크기 목록. */
    private final JComboBox sizes;

    /** Paints the page. / 쪽을 그린다. */
    private final Sheet sheet = new Sheet();

    /** The scroll pane around the sheet. / 종이를 감싼 스크롤 창. */
    private final JScrollPane scroll = new JScrollPane(sheet);

    /**
     * The component that paints the current page on a desk.
     * <p>
     * 현재 쪽을 바탕 위에 그리는 컴포넌트.
     */
    private final class Sheet extends JComponent {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /** The image of the page that was painted last. / 마지막으로 그린 쪽의 이미지. */
        private transient BufferedImage cached;

        /** The page of the cached image. / 캐시한 이미지의 쪽. */
        private int cachedPage = -1;

        /** The pixels per point of the cached image. / 캐시한 이미지의 포인트당 픽셀 수. */
        private double cachedScale;

        /**
         * Creates the sheet.
         * <p>
         * 종이를 만든다.
         */
        Sheet() {
            setOpaque(true);
        }

        /**
         * Returns the size that the page needs at the chosen size.
         * <p>
         * 고른 크기에서 쪽이 필요로 하는 크기를 반환한다.
         *
         * @return the size of the paper plus the desk around it / 용지에 둘레의 바탕을 더한 크기
         */
        public Dimension getPreferredSize() {
            PageFormat f = document.getPageFormat(page);
            double s = scale();
            return new Dimension((int) Math.ceil(f.getWidth() * s) + 2 * DESK, (int) Math.ceil(f.getHeight() * s) + 2 * DESK);
        }

        /**
         * Paints the desk and the page.
         * <p>
         * 바탕과 쪽을 그린다.
         *
         * @param g the graphics / 그래픽스
         */
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(0x808890));
            g.fillRect(0, 0, getWidth(), getHeight());
            BufferedImage image = image();
            int x = Math.max(DESK, (getWidth() - image.getWidth()) / 2);
            g.setColor(new Color(0x505860));
            g.fillRect(x + 3, DESK + 3, image.getWidth(), image.getHeight());
            g.drawImage(image, x, DESK, null);
        }

        /**
         * Returns the image of the current page at the current size, painting it if needed.
         * <p>
         * 현재 크기의 현재 쪽 이미지를 반환한다. 필요하면 그린다.
         *
         * @return the image / 이미지
         */
        BufferedImage image() {
            double s = scale();
            if (cached != null && cachedPage == page && Math.abs(cachedScale - s) < 1e-9) {
                return cached;
            }
            cached = PrintPreview.render(document, page, s);
            cachedPage = page;
            cachedScale = s;
            return cached;
        }
    }

    /**
     * Creates the preview of a document; it is not shown yet.
     * <p>
     * 문서의 미리 보기를 만든다. 아직 보이지 않는다.
     *
     * @param owner       the browser window / 브라우저 창
     * @param document    the document to show / 보일 문서
     * @param title       what names the document / 문서를 부르는 글
     * @param printAction what the print button does, or null for a preview without it / 인쇄 단추가 하는 일. 단추가 없으면 null
     */
    PrintPreview(Window owner, Pageable document, String title, Runnable printAction) {
        super(owner, Texts.of("Print Preview - ", "인쇄 미리 보기 - ") + (title == null ? "" : title), ModalityType.MODELESS);
        this.document = document;
        this.printAction = printAction;
        setName("print-preview");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        String[] names = new String[SIZES.length];
        // 크기마다 목록의 글을 만든다.
        for (int i = 0; i < SIZES.length; i++) {
            names[i] = SIZES[i] == 0 ? Texts.of("Fit page", "쪽 맞춤") : SIZES[i] + "%";
        }
        sizes = new JComboBox(names);
        sizes.setName("preview-size");
        sizes.addActionListener(new ActionListener() {
            /**
             * Shows the page at the chosen size.
             * <p>
             * 고른 크기로 쪽을 보인다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                size = SIZES[Math.max(0, sizes.getSelectedIndex())];
                refresh();
            }
        });
        previous.setName("preview-previous");
        previous.addActionListener(new ActionListener() {
            /**
             * Goes to the previous page.
             * <p>
             * 이전 쪽으로 간다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                showPage(page - 1);
            }
        });
        next.setName("preview-next");
        next.addActionListener(new ActionListener() {
            /**
             * Goes to the next page.
             * <p>
             * 다음 쪽으로 간다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                showPage(page + 1);
            }
        });
        JButton print = new JButton(Texts.of("Print...", "인쇄..."));
        print.setName("preview-print");
        print.setVisible(printAction != null);
        print.addActionListener(new ActionListener() {
            /**
             * Asks the window to print the document.
             * <p>
             * 문서를 인쇄하도록 창에 요청한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                print();
            }
        });
        JButton close = new JButton(Texts.of("Close", "닫기"));
        close.setName("preview-close");
        close.addActionListener(new ActionListener() {
            /**
             * Closes the preview.
             * <p>
             * 미리 보기를 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        position.setName("preview-position");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.add(previous);
        bar.add(position);
        bar.add(next);
        bar.add(sizes);
        bar.add(print);
        bar.add(close);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(bar, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        setSize(720, 820);
        setLocationRelativeTo(owner);
        refresh();
    }

    /**
     * Returns the pixels per point at which the page is shown: the chosen percentage of the real size on a screen of 96 pixels per
     * inch, or the size at which the whole page fits the window.
     * <p>
     * 쪽을 보이는 포인트당 픽셀 수를 반환한다: 인치당 96 픽셀인 화면에서의 실제 크기에 고른 퍼센트를 곱한 것, 또는 쪽 전체가 창에 들어가는 크기.
     *
     * @return the scale / 배율
     */
    private double scale() {
        if (size > 0) {
            return size / 100.0 * 96.0 / 72.0;
        }
        PageFormat f = document.getPageFormat(page);
        Dimension view = scroll.getViewport().getExtentSize();
        // 창이 아직 배치되지 않았으면 창의 크기에서 어림한다.
        double w = view.width > 2 * DESK ? view.width : getWidth() - 40;
        double h = view.height > 2 * DESK ? view.height : getHeight() - 110;
        double fit = Math.min((w - 2 * DESK - 4) / f.getWidth(), (h - 2 * DESK - 4) / f.getHeight());
        return Math.max(0.1, Math.min(4, fit));
    }

    /**
     * Paints a page of a document into an image, on white paper.
     * <p>
     * 문서의 한 쪽을 흰 종이 위의 이미지에 그린다.
     *
     * @param document the document / 문서
     * @param index    the page, starting at 0 / 쪽(0 부터)
     * @param scale    pixels per point / 포인트당 픽셀 수
     * @return the image of the whole paper / 용지 전체의 이미지
     */
    static BufferedImage render(Pageable document, int index, double scale) {
        PageFormat f = document.getPageFormat(index);
        BufferedImage image = new BufferedImage(Math.max(1, (int) Math.ceil(f.getWidth() * scale)), Math.max(1, (int) Math.ceil(f.getHeight() * scale)),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.scale(scale, scale);
            document.getPrintable(index).print(g, f, index);
        } catch (PrinterException e) {
            // 그릴 수 없는 쪽은 빈 종이로 보인다.
            g.setColor(Color.WHITE);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Shows another page; a page that does not exist is ignored.
     * <p>
     * 다른 쪽을 보인다. 없는 쪽은 무시한다.
     *
     * @param index the page, starting at 0 / 쪽(0 부터)
     */
    void showPage(int index) {
        if (index >= 0 && index < document.getNumberOfPages()) {
            page = index;
            refresh();
        }
    }

    /**
     * Shows the current page again: the label, the buttons and the sheet.
     * <p>
     * 현재 쪽을 다시 보인다: 레이블, 단추, 종이.
     */
    private void refresh() {
        int count = document.getNumberOfPages();
        position.setText((page + 1) + " / " + count);
        previous.setEnabled(page > 0);
        next.setEnabled(page < count - 1);
        sheet.revalidate();
        sheet.repaint();
    }

    /**
     * Asks the window to print the document, as the print button does.
     * <p>
     * 인쇄 단추처럼, 문서를 인쇄하도록 창에 요청한다.
     */
    void print() {
        if (printAction != null) {
            printAction.run();
        }
    }

    /**
     * Returns the page that is shown; for tests.
     * <p>
     * 보이는 쪽을 반환한다. 시험용이다.
     *
     * @return the page, starting at 0 / 쪽(0 부터)
     */
    int getPage() {
        return page;
    }

    /**
     * Returns the text that shows the page number; for tests.
     * <p>
     * 쪽 번호를 보이는 글을 반환한다. 시험용이다.
     *
     * @return for example <code>2 / 5</code> / 예: <code>2 / 5</code>
     */
    String getPositionText() {
        return position.getText();
    }

    /**
     * Returns the image of the page that is shown, as the preview paints it; for tests.
     * <p>
     * 미리 보기가 그리는 그대로의, 보이는 쪽의 이미지를 반환한다. 시험용이다.
     *
     * @return the image / 이미지
     */
    BufferedImage getPageImage() {
        return sheet.image();
    }

    /**
     * Returns the list of sizes; for tests.
     * <p>
     * 크기 목록을 반환한다. 시험용이다.
     *
     * @return the list / 목록
     */
    JComboBox getSizeBox() {
        return sizes;
    }

    /**
     * Returns the document that is shown; for tests.
     * <p>
     * 보이는 문서를 반환한다. 시험용이다.
     *
     * @return the document / 문서
     */
    Pageable getDocument() {
        return document;
    }
}
