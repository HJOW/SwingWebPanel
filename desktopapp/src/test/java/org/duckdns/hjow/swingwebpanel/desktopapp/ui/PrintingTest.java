package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.PaperFormat;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.PrintDocument;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Printing in a browser window (plan item P31-10): the page setup, the print command, the print preview, the settings for backgrounds
 * and for headers and footers, and <code>window.print()</code> of a page. The print system of the window is a fake one, so nothing
 * reaches a printer; the pages that would be printed are painted into images.
 * <p>
 * 브라우저 창의 인쇄(계획 P31-10): 페이지 설정, 인쇄 명령, 인쇄 미리 보기, 배경과 머리글·바닥글의 설정, 페이지의 <code>window.print()</code>. 창의
 * 인쇄 시스템은 가짜이므로 프린터에 닿는 것은 없다. 인쇄되었을 쪽은 이미지에 그린다.
 */
public class PrintingTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PrintingTest() {
    }

    /**
     * Waits until the fake print system has received a number of documents.
     * <p>
     * 가짜 인쇄 시스템이 문서를 정해진 수만큼 받을 때까지 기다린다.
     *
     * @param count the number of documents / 문서 수
     * @throws Exception on failure / 실패 시
     */
    private void awaitPrinted(final int count) throws Exception {
        await(new TestPages.Condition() {
            /** The documents arrived. / 문서가 도착했다. */
            public boolean holds() {
                return printing.printed.size() >= count;
            }
        }, "the print system receives document " + count + " (it has " + printing.printed.size() + ")");
    }

    /**
     * Counts the pixels of an image that are (nearly) pure red.
     * <p>
     * 이미지에서 (거의) 순수한 빨강인 화소를 센다.
     *
     * @param image the image / 이미지
     * @return the number of pixels / 화소 수
     */
    private static int red(BufferedImage image) {
        int n = 0;
        // 화소마다 빨강만 밝은지 본다.
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = image.getRGB(x, y);
                if (((c >> 16) & 0xFF) > 200 && ((c >> 8) & 0xFF) < 60 && (c & 0xFF) < 60) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * The page setup keeps the chosen paper in the settings and leaves it alone when the user cancels; the next printout uses it.
     * <p>
     * 페이지 설정은 고른 용지를 설정에 보관하고, 사용자가 취소하면 그대로 둔다. 다음 인쇄물이 그 용지를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageSetup() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("setup.html", "Setup", "<p>page setup</p>");
        onEdt(new Runnable() {
            /** Uses the page setup. / 페이지 설정을 쓴다. */
            public void run() {
                window.navigate(a);
                Check.equal("", app.getSettings().get(Settings.PRINT_PAPER), "no paper is kept at first");
                Check.equal(PageFormat.PORTRAIT, app.getPaper().getOrientation(), "the default paper is a portrait page");
                printing.paper = null;
                Check.isTrue(window.perform(Commands.PAGE_SETUP), "the page setup command is known");
                Check.notNull(printing.shown, "the dialog starts from the paper now");
                Check.equal("", app.getSettings().get(Settings.PRINT_PAPER), "cancelling keeps the paper");
                printing.paper = PaperFormat.make(842, 595, 50, 40, 50, 40, PageFormat.LANDSCAPE);
                window.perform(Commands.PAGE_SETUP);
                Check.equal("842 595 50 40 50 40 landscape", app.getSettings().get(Settings.PRINT_PAPER), "the chosen paper is a setting");
                Check.equal(PageFormat.LANDSCAPE, app.getPaper().getOrientation(), "the application prints on it from now on");
                Check.contains(window.getActiveTab().getStatusText(), "297 x 210 mm", "the status bar tells the paper");
            }
        });
        awaitTab(0, "Setup");
        onEdt(new Runnable() {
            /** Prints on the chosen paper. / 고른 용지에 인쇄한다. */
            public void run() {
                window.perform(Commands.PRINT);
            }
        });
        awaitPrinted(1);
        onEdt(new Runnable() {
            /** Looks at the paper of the document. / 문서의 용지를 본다. */
            public void run() {
                PageFormat f = printing.printed.get(0).getPageFormat(0);
                Check.near(842, f.getWidth(), 0.01, "the document is laid out for the paper of the page setup");
                Check.near(50, f.getImageableX(), 0.01, "with its margins");
            }
        });
    }

    /**
     * The print command hands the pages of the active tab to the print system, with headers and backgrounds as the settings say, and
     * <code>window.print()</code> of a page does the same.
     * <p>
     * 인쇄 명령은 활성 탭의 쪽들을 설정이 정한 대로 머리글과 배경을 넣어 인쇄 시스템에 넘기고, 페이지의 <code>window.print()</code> 도 같은 일을
     * 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPrintCommandAndScriptPrint() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("print.html", "Printed page", "<div style='height:120px;background:rgb(255,0,0)'></div>"
                + "<div style='height:2400px'>tall</div><p>end</p>"
                + "<script>window.addEventListener('message', function () { window.print(); });</" + "script>");
        onEdt(new Runnable() {
            /** Opens the page. / 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitTab(0, "Printed page");
        onEdt(new Runnable() {
            /** Prints with the settings as they are at first. / 처음 설정 그대로 인쇄한다. */
            public void run() {
                Check.isTrue(window.perform(Commands.PRINT), "the print command is known");
            }
        });
        awaitPrinted(1);
        onEdt(new Runnable() {
            /** Looks at the first document and changes the settings. / 첫 문서를 보고 설정을 바꾼다. */
            public void run() {
                Pageable doc = printing.printed.get(0);
                Check.isTrue(doc instanceof PrintDocument, "the browser prints its own document");
                Check.equal(3, doc.getNumberOfPages(), "a page of about 2600px needs three A4 pages");
                Check.equal("Printed page", printing.jobNames.get(0), "the print job has the title of the page");
                Check.isTrue(((PrintDocument) doc).hasHeaders(), "headers are on at first");
                Check.equal("Printed page", ((PrintDocument) doc).getMarginTexts(0)[0], "the header shows the title");
                Check.contains(((PrintDocument) doc).getMarginTexts(0)[2], "print.html", "the footer shows the address");
                Check.equal(0, red(PrintPreview.render(doc, 0, 1)), "backgrounds are left out at first");
                Check.contains(window.getActiveTab().getStatusText(), "3 page(s)", "the status bar tells the number of pages");
                app.getSettings().set(Settings.PRINT_BACKGROUNDS, "true");
                app.getSettings().set(Settings.PRINT_HEADERS, "false");
                window.perform(Commands.PRINT);
            }
        });
        awaitPrinted(2);
        onEdt(new Runnable() {
            /** Looks at the second document and lets the dialog be cancelled. / 둘째 문서를 보고 대화 상자를 취소하게 한다. */
            public void run() {
                Pageable doc = printing.printed.get(1);
                Check.isFalse(((PrintDocument) doc).hasHeaders(), "the setting turns headers off");
                Check.isTrue(red(PrintPreview.render(doc, 0, 1)) > 5000, "the setting prints backgrounds: " + red(PrintPreview.render(doc, 0, 1)));
                Check.equal(0, red(PrintPreview.render(doc, 1, 1)), "the second page shows another part of the page");
                printing.accept = false;
                window.perform(Commands.PRINT);
            }
        });
        Thread.sleep(600);
        onEdt(new Runnable() {
            /** Nothing was printed; lets the page ask. / 인쇄된 것이 없다. 페이지가 요청하게 한다. */
            public void run() {
                Check.equal(2, printing.printed.size(), "a cancelled print dialog prints nothing");
                printing.accept = true;
                window.getPanel().postMessage("print");
            }
        });
        awaitPrinted(3);
        onEdt(new Runnable() {
            /** The request of the page printed the same page. / 페이지의 요청이 같은 페이지를 인쇄했다. */
            public void run() {
                Check.equal(3, printing.printed.get(2).getNumberOfPages(), "window.print() prints the page as the command does");
                Check.equal("Printed page", printing.jobNames.get(2), "with the same job name");
            }
        });
    }

    /**
     * The print preview shows the pages of the active tab one at a time, steps through them, changes its size, and prints the document
     * that it shows.
     * <p>
     * 인쇄 미리 보기는 활성 탭의 쪽을 한 번에 한 쪽씩 보이고, 쪽을 넘기며, 크기를 바꾸고, 보이는 문서를 인쇄한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPrintPreview() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("preview.html", "Previewed", "<div style='height:100px;border:3px solid black'>first</div>"
                + "<div style='break-before:page;height:100px;border:3px solid black'>second</div>");
        onEdt(new Runnable() {
            /** Opens the page and asks for the preview. / 페이지를 열고 미리 보기를 요청한다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitTab(0, "Previewed");
        onEdt(new Runnable() {
            /** Asks for the preview. / 미리 보기를 요청한다. */
            public void run() {
                Check.isTrue(window.perform(Commands.PRINT_PREVIEW), "the preview command is known");
            }
        });
        await(new TestPages.Condition() {
            /** The preview exists. / 미리 보기가 있다. */
            public boolean holds() {
                return window.getLastPreview() != null;
            }
        }, "the preview is created when the pages are ready");
        onEdt(new Runnable() {
            /** Uses the preview. / 미리 보기를 쓴다. */
            public void run() {
                PrintPreview p = window.getLastPreview();
                Check.equal(0, printing.printed.size(), "a preview prints nothing by itself");
                Check.equal("1 / 2", p.getPositionText(), "the forced break makes two pages");
                Check.contains(p.getTitle(), "Previewed", "the title names the page");
                BufferedImage first = p.getPageImage();
                Check.isTrue(first.getWidth() > 100 && first.getHeight() > first.getWidth(), "the image has the shape of the paper: " + first.getWidth()
                        + "x" + first.getHeight());
                p.showPage(1);
                Check.equal("2 / 2", p.getPositionText(), "the next page is shown");
                p.showPage(2);
                Check.equal(1, p.getPage(), "there is no page behind the last one");
                p.showPage(0);
                p.getSizeBox().setSelectedIndex(3);
                BufferedImage full = p.getPageImage();
                PageFormat f = p.getDocument().getPageFormat(0);
                Check.equal((int) Math.ceil(f.getWidth() * 96 / 72), full.getWidth(), "100% shows the paper at its real size on a 96 dpi screen");
                p.getSizeBox().setSelectedIndex(1);
                Check.equal((int) Math.ceil(f.getWidth() * 96 / 72 / 2), p.getPageImage().getWidth(), "50% shows it at half the size");
                p.print();
                Check.equal(1, printing.printed.size(), "the print button prints");
                Check.same(p.getDocument(), printing.printed.get(0), "exactly the document that the preview shows");
                p.dispose();
            }
        });
    }
}
