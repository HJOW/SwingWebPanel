package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.awt.Component;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;

/**
 * A print system for tests: it never reaches a printer or shows a dialog. It answers the page setup with a fixed paper and only
 * remembers the documents that it is asked to print, so that a test can look at their pages.
 * <p>
 * 시험용 인쇄 시스템: 프린터에 닿지 않고 대화 상자도 보이지 않는다. 페이지 설정에는 정해 둔 용지로 답하고, 인쇄해 달라고 받은 문서는 기억만
 * 하므로 시험이 그 쪽들을 볼 수 있다.
 */
public final class FakePrinting implements BrowserApp.Printing {

    /** The answer of the page setup; null means that the user cancelled. / 페이지 설정의 답. null 은 사용자가 취소했다는 뜻이다. */
    public PageFormat paper;

    /** The answer of the print dialog: true prints. / 인쇄 대화 상자의 답: true 이면 인쇄한다. */
    public boolean accept = true;

    /** The documents that were handed over for printing. / 인쇄하라고 넘겨받은 문서. */
    public final List<Pageable> printed = new ArrayList<Pageable>();

    /** The names of the print jobs. / 인쇄 작업의 이름. */
    public final List<String> jobNames = new ArrayList<String>();

    /** The paper that the last page setup started from. / 마지막 페이지 설정이 시작한 용지. */
    public PageFormat shown;

    /**
     * Creates the fake print system.
     * <p>
     * 가짜 인쇄 시스템을 만든다.
     */
    public FakePrinting() {
    }

    /**
     * Answers with the fixed paper.
     * <p>
     * 정해 둔 용지로 답한다.
     *
     * @param parent  ignored / 쓰지 않는다
     * @param current the paper now / 지금의 용지
     * @return the answer / 답
     */
    public PageFormat pageSetup(Component parent, PageFormat current) {
        shown = current;
        return paper;
    }

    /**
     * Remembers the document instead of printing it.
     * <p>
     * 문서를 인쇄하는 대신 기억한다.
     *
     * @param parent   ignored / 쓰지 않는다
     * @param document the pages / 쪽들
     * @param jobName  the name of the job / 작업의 이름
     * @return the fixed answer of the print dialog / 인쇄 대화 상자의 정해 둔 답
     */
    public boolean print(Component parent, Pageable document, String jobName) {
        if (accept) {
            printed.add(document);
            jobNames.add(jobName);
        }
        return accept;
    }
}
