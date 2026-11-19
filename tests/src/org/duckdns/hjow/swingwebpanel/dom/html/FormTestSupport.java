package org.duckdns.hjow.swingwebpanel.dom.html;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the form tests: parsing a page into a document, a recording {@link FormHost}, element lookup with the right class, a listener that
 * records events and text conversion of byte arrays.
 * <p>
 * 폼 시험의 도우미: 페이지를 문서로 파싱하기, 기록하는 {@link FormHost}, 올바른 클래스로 요소 찾기, 이벤트를 기록하는 리스너, 바이트 배열의 문자열 변환.
 */
final class FormTestSupport {

    /** The URL of the test documents. / 시험 문서의 URL. */
    static final String URL = "http://example.test/dir/page.html";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormTestSupport() {
    }

    /**
     * Parses markup into a document whose URL is {@link #URL}.
     * <p>
     * 마크업을 URL 이 {@link #URL} 인 문서로 파싱한다.
     *
     * @param markup the markup / 마크업
     * @return the document / 문서
     */
    static Document parse(String markup) {
        return HtmlParser.parseDocument(markup, URL, null, false);
    }

    /**
     * Parses a body fragment into a document (wrapped in html and body).
     * <p>
     * body 조각을 문서로 파싱한다(html 과 body 로 감싼다).
     *
     * @param body the body markup / body 마크업
     * @return the document / 문서
     */
    static Document page(String body) {
        return parse("<!DOCTYPE html><html><head><title>t</title></head><body>" + body + "</body></html>");
    }

    /**
     * Returns the input with an id.
     * <p>
     * id 가 같은 input 을 반환한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return the input / input
     */
    static HtmlInputElement input(Document d, String id) {
        Element e = d.getElementById(id);
        Check.isTrue(e instanceof HtmlInputElement, "#" + id + " is an input: " + e);
        return (HtmlInputElement) e;
    }

    /**
     * Returns the form with an id.
     * <p>
     * id 가 같은 form 을 반환한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return the form / form
     */
    static HtmlFormElement form(Document d, String id) {
        Element e = d.getElementById(id);
        Check.isTrue(e instanceof HtmlFormElement, "#" + id + " is a form: " + e);
        return (HtmlFormElement) e;
    }

    /**
     * Returns the select with an id.
     * <p>
     * id 가 같은 select 를 반환한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return the select / select
     */
    static HtmlSelectElement select(Document d, String id) {
        Element e = d.getElementById(id);
        Check.isTrue(e instanceof HtmlSelectElement, "#" + id + " is a select: " + e);
        return (HtmlSelectElement) e;
    }

    /**
     * Returns the textarea with an id.
     * <p>
     * id 가 같은 textarea 를 반환한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return the textarea / textarea
     */
    static HtmlTextAreaElement textarea(Document d, String id) {
        Element e = d.getElementById(id);
        Check.isTrue(e instanceof HtmlTextAreaElement, "#" + id + " is a textarea: " + e);
        return (HtmlTextAreaElement) e;
    }

    /**
     * Returns the option with an id.
     * <p>
     * id 가 같은 option 을 반환한다.
     *
     * @param d  the document / 문서
     * @param id the id / id
     * @return the option / option
     */
    static HtmlOptionElement option(Document d, String id) {
        Element e = d.getElementById(id);
        Check.isTrue(e instanceof HtmlOptionElement, "#" + id + " is an option: " + e);
        return (HtmlOptionElement) e;
    }

    /**
     * Converts bytes to a string one char per byte (ISO-8859-1), so that every byte value is visible in a comparison.
     * <p>
     * 바이트를 바이트당 문자 하나(ISO-8859-1)로 문자열로 바꿔, 비교할 때 모든 바이트 값이 보이게 한다.
     *
     * @param b the bytes / 바이트
     * @return the string / 문자열
     */
    static String latin1(byte[] b) {
        try {
            return new String(b, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Converts a string to bytes with a charset name.
     * <p>
     * 문자열을 charset 이름으로 바이트로 바꾼다.
     *
     * @param s       the string / 문자열
     * @param charset the charset name / charset 이름
     * @return the bytes / 바이트
     */
    static byte[] bytes(String s, String charset) {
        try {
            return s.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Creates a listener that appends <code>type@targetId</code> to a log.
     * <p>
     * <code>종류@대상id</code> 를 기록에 덧붙이는 리스너를 만든다.
     *
     * @param log the log / 기록
     * @return the listener / 리스너
     */
    static EventListener logger(final List<String> log) {
        return new EventListener() {
            /**
             * Logs the event.
             * <p>
             * 이벤트를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                Object t = e.getTarget();
                log.add(e.getType() + "@" + (t instanceof Element ? ((Element) t).getId() : String.valueOf(t)));
            }
        };
    }

    /**
     * A host that records what it is told.
     * <p>
     * 전달받은 것을 기록하는 호스트.
     */
    static final class RecordingHost extends FormHostAdapter {

        /** The submissions. / 제출 정보. */
        final List<FormSubmission> submissions = new ArrayList<FormSubmission>();

        /** The forms of the submissions. / 제출한 폼. */
        final List<Element> forms = new ArrayList<Element>();

        /** The reports of invalid controls: <code>id: message</code>. / 올바르지 않은 컨트롤 보고: <code>id: 메시지</code>. */
        final List<String> reports = new ArrayList<String>();

        /** The files that {@link #chooseFiles} hands out, or null for a cancelled chooser. / {@link #chooseFiles} 가 주는 파일. 취소이면 null. */
        FileEntry[] files;

        /** The number of calls of {@link #chooseFiles}. / {@link #chooseFiles} 호출 수. */
        int chooserCalls;

        /** The number of state change notifications. / 상태 변경 통지 수. */
        int stateChanges;

        /**
         * Creates the host.
         * <p>
         * 호스트를 만든다.
         */
        RecordingHost() {
        }

        /**
         * Records a submission.
         * <p>
         * 제출 정보를 기록한다.
         *
         * @param submission the submission / 제출 정보
         * @param form       the form / 폼
         */
        public void submit(FormSubmission submission, Element form) {
            submissions.add(submission);
            forms.add(form);
        }

        /**
         * Records a report.
         * <p>
         * 보고를 기록한다.
         *
         * @param control the control / 컨트롤
         * @param message the message / 메시지
         */
        public void reportInvalid(Element control, String message) {
            reports.add(control.getId() + ": " + message);
        }

        /**
         * Hands out the prepared files.
         * <p>
         * 준비된 파일을 준다.
         *
         * @param input    the input / input
         * @param multiple whether several files are accepted / 여러 파일을 받는지
         * @param accept   the accept attribute / accept 속성
         * @return the files or null / 파일 또는 null
         */
        public FileEntry[] chooseFiles(Element input, boolean multiple, String accept) {
            chooserCalls++;
            return files;
        }

        /**
         * Counts the notification.
         * <p>
         * 통지를 센다.
         *
         * @param element the element / 요소
         */
        public void stateChanged(Element element) {
            stateChanges++;
        }
    }

    /**
     * Installs a recording host in a document.
     * <p>
     * 기록하는 호스트를 문서에 설치한다.
     *
     * @param d the document / 문서
     * @return the host / 호스트
     */
    static RecordingHost host(Document d) {
        RecordingHost h = new RecordingHost();
        d.setFormHost(h);
        return h;
    }
}
