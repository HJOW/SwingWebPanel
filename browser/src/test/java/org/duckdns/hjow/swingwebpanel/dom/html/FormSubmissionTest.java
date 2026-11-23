package org.duckdns.hjow.swingwebpanel.dom.html;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the form submission (plan items P11-06 and P11-07, HTML Standard 4.10.22): constructing the entry list, the three encodings with
 * their bytes (Korean text, spaces, special characters, line breaks, a character that the encoding cannot represent), the multipart boundary
 * structure, the method, action, target and enctype of the submission with the overrides of the submitter, the <code>submit</code> and
 * <code>reset</code> events and the cases that must not navigate.
 * <p>
 * 폼 제출 시험(계획 P11-06, P11-07, HTML 표준 4.10.22): 항목 목록 만들기, 세 가지 인코딩과 그 바이트(한글, 공백, 특수 문자, 줄 바꿈, 인코딩이 표현할 수 없는 문자),
 * multipart 경계 구조, submitter 가 재정의하는 method/action/target/enctype, <code>submit</code>, <code>reset</code> 이벤트, 이동하면 안 되는 경우.
 */
public class FormSubmissionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormSubmissionTest() {
    }

    /**
     * Returns the entries as <code>name=value</code> strings joined with a semicolon.
     * <p>
     * 항목을 <code>이름=값</code> 문자열로 만들어 세미콜론으로 이어서 반환한다.
     *
     * @param entries the entries / 항목
     * @return the text / 문자열
     */
    private static String show(List<FormEntry> entries) {
        StringBuilder b = new StringBuilder();
        // 항목을 하나씩 이름=값 형태로 이어 붙인다.
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                b.append(';');
            }
            FormEntry e = entries.get(i);
            b.append(e.getName()).append('=').append(e.isFile() ? "<file:" + e.getFile().getName() + ">" : e.getValue());
        }
        return b.toString();
    }

    /**
     * Submits a form through <code>requestSubmit</code> and returns the single new submission that the host received.
     * <p>
     * <code>requestSubmit</code> 로 폼을 제출하고 호스트가 새로 받은 하나뿐인 제출 정보를 반환한다.
     *
     * @param d        the document / 문서
     * @param formId   the id of the form / 폼의 id
     * @param submitId the id of a submit button, or null / 제출 버튼의 id. 없으면 null
     * @return the submission / 제출 정보
     */
    private static FormSubmission submit(Document d, String formId, String submitId) {
        FormTestSupport.RecordingHost host = (FormTestSupport.RecordingHost) d.getFormHost();
        if (host == null) {
            host = FormTestSupport.host(d);
        }
        int before = host.submissions.size();
        FormTestSupport.form(d, formId).requestSubmit(submitId == null ? null : d.getElementById(submitId));
        Check.equal(before + 1, host.submissions.size(), "the form " + formId + " was submitted");
        return host.submissions.get(before);
    }

    /**
     * Decodes UTF-8 bytes.
     * <p>
     * UTF-8 바이트를 디코딩한다.
     *
     * @param b the bytes / 바이트
     * @return the text / 문자열
     */
    private static String fromUtf8(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * The entry list: tree order, checked controls only, no disabled, no unnamed, only the submitter among buttons, select options, textarea
     * values with CRLF.
     * <p>
     * 항목 목록: 트리 순서, 체크된 컨트롤만, 비활성/이름 없는 것 제외, 버튼은 submitter 만, select 의 option, CRLF 의 textarea 값.
     */
    public void testEntryList() {
        Document d = FormTestSupport.page("<form id=f><input name=t value='v1'><input type=checkbox name=c1 value=a checked><input type=checkbox name=c2 value=b>"
                + "<input type=checkbox name=c3 checked><input type=radio name=r value=x><input type=radio name=r value=y checked>"
                + "<input name=dis value=no disabled><input value=unnamed><input name='' value=emptyname>"
                + "<select name=s><option value=1>one</option><option value=2 selected>two</option></select>"
                + "<select name=m multiple><option value=1 selected>one</option><option value=2 disabled selected>two</option><option selected>three</option></select>"
                + "<select><option selected>nameless</option></select>"
                + "<textarea name=ta>\nline1\nline2</textarea>"
                + "<input type=hidden name=h value=hid><input id=go type=submit name=go value=Go><input type=submit name=other value=Other>"
                + "<button id=b1 name=b1 value=B1>b</button><input id=rst type=reset name=rst value=R><input type=button name=btn value=Btn>"
                + "<fieldset disabled><input name=infs value=no></fieldset><datalist><input name=indl value=no></datalist>"
                + "<input type=search name=se value=found><input type=number name=n value=5><input type=color name=col><input type=range name=rg>"
                + "</form><input form=f name=outside value=yes>");
        HtmlFormElement f = FormTestSupport.form(d, "f");
        String common1 = "t=v1;c1=a;c3=on;r=y;s=2;m=1;m=three;ta=line1\r\nline2;h=hid;";
        String common2 = "se=found;n=5;col=#000000;rg=50;outside=yes";
        Check.equal(common1 + common2, show(FormEntryList.construct(f, null, "UTF-8")), "entries without a submitter");
        Check.equal(common1 + "go=Go;" + common2, show(FormEntryList.construct(f, d.getElementById("go"), "UTF-8")), "with the first submit button as the submitter");
        Check.equal(common1 + "b1=B1;" + common2, show(FormEntryList.construct(f, d.getElementById("b1"), "UTF-8")), "with the button element as the submitter");
        Check.equal(common1 + common2, show(FormEntryList.construct(f, d.getElementById("rst"), "UTF-8")), "a reset button never contributes");
    }

    /**
     * The urlencoded body: Korean text as UTF-8, a space as <code>+</code>, reserved characters percent-encoded, line breaks as CRLF.
     * <p>
     * urlencoded 본문: 한글은 UTF-8, 공백은 <code>+</code>, 예약 문자는 퍼센트 인코딩, 줄 바꿈은 CRLF.
     */
    public void testUrlEncodedBody() {
        Document d = FormTestSupport.page("<form id=f method=post action='/save'><input name='q' value='한글 text &amp; more'><input name='a b' value='x=y&amp;z'>"
                + "<textarea name=ta>l1\nl2</textarea><input name=s value='*-._~!()'></form>");
        FormSubmission s = submit(d, "f", null);
        Check.equal("POST", s.getMethod(), "method");
        Check.equal("http://example.test/save", s.getUrl(), "action is resolved against the document");
        Check.equal("application/x-www-form-urlencoded", s.getContentType(), "content type");
        Check.equal("q=%ED%95%9C%EA%B8%80+text+%26+more&a+b=x%3Dy%26z&ta=l1%0D%0Al2&s=*-._%7E%21%28%29", FormTestSupport.latin1(s.getBody()), "body");
        Check.equal("UTF-8", s.getEncoding(), "encoding");
        Check.equal("", s.getTarget(), "no target");
        Check.equal(4, s.getEntries().size(), "the entries are kept");
    }

    /**
     * The GET submission: the query of the action is replaced by the serialized entries, the fragment is kept; an ftp action takes no query.
     * <p>
     * GET 제출: action 의 쿼리는 직렬화한 항목으로 바뀌고 프래그먼트는 유지된다. ftp action 은 쿼리를 받지 않는다.
     */
    public void testGetSubmission() {
        Document d = FormTestSupport.page("<form id=f action='/search?old=1#frag'><input name=q value='a b'><input name=k value='한'></form>"
                + "<form id=g method=get action='ftp://host/dir/'><input name=q value=z></form>"
                + "<form id=h action=''><input name=q value=z></form><form id=i method=POST action='data:text/html,x'><input name=q value=z></form>"
                + "<form id=j method=post action='javascript:void(0)'><input name=q value=z></form>");
        FormSubmission s = submit(d, "f", null);
        Check.equal("GET", s.getMethod(), "method");
        Check.equal("http://example.test/search?q=a+b&k=%ED%95%9C#frag", s.getUrl(), "the query was replaced and the fragment kept");
        Check.isNull(s.getBody(), "no body");
        Check.isNull(s.getContentType(), "no content type");
        Check.equal("ftp://host/dir/", submit(d, "g", null).getUrl(), "an ftp action takes no query");
        Check.equal("http://example.test/dir/page.html?q=z", submit(d, "h", null).getUrl(), "an empty action stands for the document URL");
        FormSubmission data = submit(d, "i", null);
        Check.equal("GET", data.getMethod(), "a data: URL is always fetched with GET");
        Check.equal("data:text/html,x", data.getUrl(), "and takes no entries");
        FormSubmission js = submit(d, "j", null);
        Check.equal("GET", js.getMethod(), "javascript: too");
        Check.equal("post", js.getOriginalMethod(), "the method state of the form is kept for the host");
    }

    /**
     * The multipart body: boundary structure, escaped names, file parts with a content type, text parts without one, an empty file part for a file
     * control without a selection.
     * <p>
     * multipart 본문: 경계 구조, 이스케이프한 이름, 콘텐츠 타입이 있는 파일 부분, 콘텐츠 타입이 없는 텍스트 부분, 선택이 없는 파일 컨트롤의 빈 파일 부분.
     */
    public void testMultipartBody() {
        Document d = FormTestSupport.page("<form id=f method=post enctype='multipart/form-data' action=/up><input name=t value='한 글'><input name='we&quot;ird' value='x'>"
                + "<input id=up type=file name=up><input type=file name=none><textarea name=ta>a\nb</textarea></form>");
        FormTestSupport.input(d, "up").setSelectedFiles(Arrays.asList(new FileEntry("C:\\dir\\a.txt", "Text/Plain", 0, FormTestSupport.bytes("HELLO", "US-ASCII"))));
        FormSubmission s = submit(d, "f", null);
        String ct = s.getContentType();
        Check.isTrue(ct.startsWith("multipart/form-data; boundary="), "content type: " + ct);
        String b = ct.substring("multipart/form-data; boundary=".length());
        Check.isTrue(b.length() >= 20, "the boundary is long enough: " + b);
        String korean = FormTestSupport.latin1(FormTestSupport.bytes("한 글", "UTF-8"));
        String expected = "--" + b + "\r\nContent-Disposition: form-data; name=\"t\"\r\n\r\n" + korean + "\r\n"
                + "--" + b + "\r\nContent-Disposition: form-data; name=\"we%22ird\"\r\n\r\nx\r\n"
                + "--" + b + "\r\nContent-Disposition: form-data; name=\"up\"; filename=\"a.txt\"\r\nContent-Type: text/plain\r\n\r\nHELLO\r\n"
                + "--" + b + "\r\nContent-Disposition: form-data; name=\"none\"; filename=\"\"\r\nContent-Type: application/octet-stream\r\n\r\n\r\n"
                + "--" + b + "\r\nContent-Disposition: form-data; name=\"ta\"\r\n\r\na\r\nb\r\n"
                + "--" + b + "--\r\n";
        Check.equal(expected, FormTestSupport.latin1(s.getBody()), "multipart body");
        Check.equal("a.txt", FormTestSupport.input(d, "up").getFiles().item(0).getName(), "a file name has no directory part");
        Check.equal("C:\\fakepath\\a.txt", FormTestSupport.input(d, "up").getValue(), "value is the fake path of the first file");
        Check.equal("text/plain", FormTestSupport.input(d, "up").getFiles().item(0).getType(), "the type is lower-cased");
    }

    /**
     * The text/plain body: <code>name=value</code> lines that end with CRLF, nothing is escaped.
     * <p>
     * text/plain 본문: CRLF 로 끝나는 <code>이름=값</code> 줄. 아무것도 이스케이프하지 않는다.
     */
    public void testTextPlainBody() {
        Document d = FormTestSupport.page("<form id=f method=post enctype='text/plain' action=/p><input name=a value='x y'><input name=b value='한'><textarea name=ta>1\n2</textarea>"
                + "<input type=file name=up></form>");
        FormSubmission s = submit(d, "f", null);
        Check.equal("text/plain", s.getContentType(), "content type");
        Check.equal("a=x y\r\nb=한\r\nta=1\r\n2\r\nup=\r\n", fromUtf8(s.getBody()), "text/plain keeps the text and ends every line with CRLF; a file is its name");
        Check.equal("POST", s.getMethod(), "method");
    }

    /**
     * The encoding of the submission: the first usable label of <code>accept-charset</code>, otherwise the document; characters that the encoding cannot
     * represent become numeric character references; <code>_charset_</code> gets the encoding name.
     * <p>
     * 제출의 인코딩: <code>accept-charset</code> 의 첫 쓸 수 있는 라벨, 없으면 문서. 인코딩이 표현할 수 없는 문자는 숫자 문자 참조가 된다. <code>_charset_</code> 은 인코딩 이름을 얻는다.
     */
    public void testEncodingSelection() {
        Document d = FormTestSupport.page("<form id=f method=post action=/a><input name=q value='é☃한'><input type=hidden name=_charset_></form>"
                + "<form id=l1 method=post action=/a accept-charset='iso-8859-1'><input name=q value='é☃한'><input type=hidden name=_CHARSET_></form>"
                + "<form id=l2 method=post action=/a accept-charset='bogus utf-8'><input name=q value='한'></form>"
                + "<form id=l3 method=post action=/a accept-charset='bogus'><input name=q value='한'></form>"
                + "<form id=l4 method=post action=/a accept-charset='utf-16'><input name=q value='한'></form>"
                + "<form id=l5 method=post action=/a accept-charset='euc-kr'><input name=q value='한글'><input type=hidden name=_charset_></form>");
        FormSubmission utf8 = submit(d, "f", null);
        Check.equal("q=%C3%A9%E2%98%83%ED%95%9C&_charset_=UTF-8", FormTestSupport.latin1(utf8.getBody()), "UTF-8 with the _charset_ field");
        FormSubmission latin = submit(d, "l1", null);
        Check.equal("windows-1252", latin.getEncoding(), "iso-8859-1 is a label of windows-1252");
        Check.equal("q=%E9%26%239731%3B%26%2354620%3B&_CHARSET_=windows-1252", FormTestSupport.latin1(latin.getBody()),
                "characters that the encoding cannot represent become decimal references that are escaped like text");
        Check.equal("q=%ED%95%9C", FormTestSupport.latin1(submit(d, "l2", null).getBody()), "the first known label wins");
        Check.equal("UTF-8", submit(d, "l3", null).getEncoding(), "no known label: UTF-8");
        Check.equal("UTF-8", submit(d, "l4", null).getEncoding(), "UTF-16 is replaced by UTF-8");
        if (Charset.isSupported("EUC-KR")) {
            FormSubmission kr = submit(d, "l5", null);
            Check.equal("EUC-KR", kr.getEncoding(), "euc-kr");
            Check.equal("q=%C7%D1%B1%DB&_charset_=EUC-KR", FormTestSupport.latin1(kr.getBody()), "Korean text as EUC-KR bytes");
        } else {
            Check.skip("this JRE has no EUC-KR charset");
        }
    }

    /**
     * Line breaks of names and values (for example a hidden input whose attribute contains a line break) become CRLF in the entry list.
     * <p>
     * 이름과 값의 줄 바꿈(예: 속성에 줄 바꿈이 있는 hidden input)은 항목 목록에서 CRLF 가 된다.
     */
    public void testLineBreaksAreNormalized() {
        Document d = FormTestSupport.page("<form id=f method=post action=/a><input type=hidden name='n\rx' value='a\nb\r\nc\rd'></form>");
        FormSubmission s = submit(d, "f", null);
        Check.equal("n\r\nx=a\r\nb\r\nc\r\nd", show(s.getEntries()), "entry list");
        Check.equal("n%0D%0Ax=a%0D%0Ab%0D%0Ac%0D%0Ad", FormTestSupport.latin1(s.getBody()), "body");
    }

    /**
     * The <code>dirname</code> entry follows the control and holds the directionality.
     * <p>
     * <code>dirname</code> 항목은 컨트롤 뒤에 오고 방향성을 담는다.
     */
    public void testDirname() {
        Document d = FormTestSupport.page("<form id=f method=post action=/a><input name=q dirname=q.dir value=a><textarea name=t dirname=t.dir dir=rtl>x</textarea>"
                + "<input name=h dirname=h.dir dir=auto value='\u05e9\u05dc\u05d5\u05dd'><div dir=rtl><input name=g dirname=g.dir value=y></div><input name=e dirname='' value=z></form>");
        Check.equal("q=a;q.dir=ltr;t=x;t.dir=rtl;h=\u05e9\u05dc\u05d5\u05dd;h.dir=rtl;g=y;g.dir=rtl;e=z", show(FormEntryList.construct(FormTestSupport.form(d, "f"), null, "UTF-8")),
                "dirname entries");
    }

    /**
     * An image button submits the coordinates of the click.
     * <p>
     * 이미지 버튼은 클릭 좌표를 제출한다.
     */
    public void testImageButton() {
        Document d = FormTestSupport.page("<form id=f action=/a><input name=q value=1><input id=im type=image name=pic src=x.png><input id=im2 type=image src=y.png></form>");
        HtmlInputElement im = FormTestSupport.input(d, "im");
        im.setSelectedCoordinate(10, 20);
        Check.equal("q=1;pic.x=10;pic.y=20", show(FormEntryList.construct(FormTestSupport.form(d, "f"), im, "UTF-8")), "named image button");
        FormTestSupport.input(d, "im2").setSelectedCoordinate(3, 4);
        Check.equal("q=1;x=3;y=4", show(FormEntryList.construct(FormTestSupport.form(d, "f"), FormTestSupport.input(d, "im2"), "UTF-8")), "image button without a name");
        Check.equal("q=1", show(FormEntryList.construct(FormTestSupport.form(d, "f"), null, "UTF-8")), "an image button that is not the submitter adds nothing");
        Check.equal("http://example.test/a?q=1&pic.x=10&pic.y=20", submit(d, "f", "im").getUrl(), "submitted through the image button");
    }

    /**
     * The submitter overrides action, method, enctype, target and validation of the form.
     * <p>
     * submitter 가 폼의 action, method, enctype, target, 검증을 재정의한다.
     */
    public void testSubmitterOverrides() {
        Document d = FormTestSupport.page("<form id=f method=get action=/a enctype='application/x-www-form-urlencoded' target=_self><input name=x value=1>"
                + "<button id=b1 name=n value=v formaction='/b?z=1' formmethod=POST formenctype='text/plain' formtarget=_blank>go</button><button id=b2 name=m value=w>plain</button>"
                + "<input id=b3 type=submit value=Send formmethod=post formenctype=MULTIPART/FORM-DATA></form>");
        FormSubmission s1 = submit(d, "f", "b1");
        Check.equal("POST", s1.getMethod(), "formmethod");
        Check.equal("http://example.test/b?z=1", s1.getUrl(), "formaction");
        Check.equal("text/plain", s1.getContentType(), "formenctype");
        Check.equal("_blank", s1.getTarget(), "formtarget");
        Check.equal("x=1\r\nn=v\r\n", fromUtf8(s1.getBody()), "body with the submitter");
        FormSubmission s2 = submit(d, "f", "b2");
        Check.equal("GET", s2.getMethod(), "the form's own method");
        Check.equal("http://example.test/a?x=1&m=w", s2.getUrl(), "the form's own action");
        Check.equal("_self", s2.getTarget(), "the form's target");
        FormSubmission s3 = submit(d, "f", "b3");
        Check.isTrue(s3.getContentType().startsWith("multipart/form-data; boundary="), "enctype keywords are case-insensitive");
        Check.equal("POST", s3.getMethod(), "formmethod of an input");
        Check.equal("http://example.test/a", s3.getUrl(), "a POST keeps the action URL");
    }

    /**
     * Cases that never navigate: a form that is not in a document, <code>method=dialog</code>, and invalid arguments of <code>requestSubmit</code>.
     * <p>
     * 이동하지 않는 경우: 문서에 없는 폼, <code>method=dialog</code>, <code>requestSubmit</code> 의 잘못된 인수.
     */
    public void testNoNavigation() {
        final Document d = FormTestSupport.page("<form id=f action=/a><input name=x value=1><button id=b>b</button><input type=button id=nb></form><form id=g method=dialog><input name=y></form>"
                + "<form id=h><button id=hb></button></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        FormTestSupport.form(d, "g").requestSubmit(null);
        FormTestSupport.form(d, "g").submit();
        Check.equal(0, host.submissions.size(), "method=dialog does not navigate");
        Element detached = d.createElement("form");
        detached.setAttribute("action", "/x");
        ((HtmlFormElement) detached).submit();
        ((HtmlFormElement) detached).requestSubmit(null);
        Check.equal(0, host.submissions.size(), "a form that is not connected cannot navigate");
        final HtmlFormElement f = FormTestSupport.form(d, "f");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Passes a button that is not a submit button.
             * <p>
             * 제출 버튼이 아닌 버튼을 넘긴다.
             */
            public void run() {
                f.requestSubmit(d.getElementById("nb"));
            }
        }, "not a submit button");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Passes a submit button of another form.
             * <p>
             * 다른 폼의 제출 버튼을 넘긴다.
             */
            public void run() {
                f.requestSubmit(d.getElementById("hb"));
            }
        }, "a submit button of another form");
    }

    /**
     * The <code>submit</code> event: it is cancelable, carries the submitter and stops the navigation when cancelled; <code>submit()</code> does not fire it.
     * <p>
     * <code>submit</code> 이벤트: 취소 가능하고 submitter 를 담으며 취소하면 이동이 멈춘다. <code>submit()</code> 은 이 이벤트를 발생시키지 않는다.
     */
    public void testSubmitEvent() {
        Document d = FormTestSupport.page("<form id=f action=/a><input name=x value=1><button id=b>b</button></form>");
        FormTestSupport.RecordingHost host = FormTestSupport.host(d);
        final List<Object> seen = new ArrayList<Object>();
        final boolean[] cancel = {true};
        d.getElementById("f").addEventListener("submit", new EventListener() {
            /**
             * Records the submitter and cancels the event while requested.
             * <p>
             * submitter 를 기록하고 요청되는 동안 이벤트를 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                seen.add(e instanceof SubmitEvent ? ((SubmitEvent) e).getSubmitter() : "not a SubmitEvent");
                seen.add(Boolean.valueOf(e.isBubbles()) + "/" + e.isCancelable() + "/" + e.isTrusted());
                if (cancel[0]) {
                    e.preventDefault();
                }
            }
        }, false);
        FormTestSupport.form(d, "f").requestSubmit(d.getElementById("b"));
        Check.equal(0, host.submissions.size(), "a cancelled submit event stops the submission");
        Check.same(d.getElementById("b"), seen.get(0), "the submitter");
        Check.equal("true/true/true", seen.get(1), "the event bubbles, is cancelable and trusted");
        cancel[0] = false;
        FormTestSupport.form(d, "f").requestSubmit(null);
        Check.equal(1, host.submissions.size(), "not cancelled");
        Check.isNull(seen.get(2), "no submitter");
        FormTestSupport.form(d, "f").submit();
        Check.equal(2, host.submissions.size(), "submit()");
        Check.equal(4, seen.size(), "and it fired no event");
    }

    /**
     * The <code>reset</code> event is cancelable; the reset algorithm restores the values, checkedness and selection of every resettable control.
     * <p>
     * <code>reset</code> 이벤트는 취소 가능하다. 초기화 알고리즘은 초기화할 수 있는 모든 컨트롤의 값, checkedness, 선택을 되살린다.
     */
    public void testReset() {
        Document d = FormTestSupport.page("<form id=f><input id=t value=a><input id=c type=checkbox checked><textarea id=ta>x</textarea>"
                + "<select id=s><option>1</option><option selected>2</option></select><input id=h type=hidden value=hh><input id=file type=file></form>");
        HtmlInputElement t = FormTestSupport.input(d, "t");
        HtmlInputElement c = FormTestSupport.input(d, "c");
        HtmlTextAreaElement ta = FormTestSupport.textarea(d, "ta");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        t.setValue("changed");
        c.setChecked(false);
        ta.setValue("changed");
        s.setSelectedIndex(0);
        FormTestSupport.input(d, "file").setSelectedFiles(Arrays.asList(new FileEntry("a", "", 0, new byte[0])));
        final boolean[] cancel = {true};
        final List<String> log = new ArrayList<String>();
        d.getElementById("f").addEventListener("reset", new EventListener() {
            /**
             * Logs the event and cancels it while requested.
             * <p>
             * 이벤트를 기록하고 요청되는 동안 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                log.add(e.getType() + "/" + e.isBubbles() + "/" + e.isCancelable());
                if (cancel[0]) {
                    e.preventDefault();
                }
            }
        }, false);
        FormTestSupport.form(d, "f").reset();
        Check.equal("changed", t.getValue(), "a cancelled reset event keeps the values");
        Check.equal(Arrays.asList("reset/true/true"), log, "the event");
        cancel[0] = false;
        FormTestSupport.form(d, "f").reset();
        Check.equal("a", t.getValue(), "text restored");
        Check.isTrue(c.isChecked(), "checkbox restored");
        Check.equal("x", ta.getValue(), "textarea restored");
        Check.equal(1, s.getSelectedIndex(), "select restored");
        Check.equal(0, FormTestSupport.input(d, "file").getFiles().getLength(), "the file selection is emptied");
        Check.equal("hh", FormTestSupport.input(d, "h").getValue(), "hidden");
    }

    /**
     * Enumerated attributes of the form: <code>method</code> and <code>enctype</code> are case-insensitive and fall back to their defaults; the
     * <code>action</code> IDL attribute is resolved.
     * <p>
     * 폼의 열거형 속성: <code>method</code> 와 <code>enctype</code> 은 대소문자를 구분하지 않으며 올바르지 않으면 기본값이다. <code>action</code> IDL 속성은 해석된 URL 이다.
     */
    public void testFormAttributes() {
        Document d = FormTestSupport.page("<form id=a method=POST enctype='TEXT/PLAIN' action='x/y?z'></form><form id=b method=bogus enctype=bogus></form><form id=c method=DIALOG novalidate></form>");
        Check.equal("post", FormTestSupport.form(d, "a").getMethod(), "post");
        Check.equal("text/plain", FormTestSupport.form(d, "a").getEnctype(), "enctype");
        Check.equal("http://example.test/dir/x/y?z", FormTestSupport.form(d, "a").getAction(), "action is resolved");
        Check.equal("get", FormTestSupport.form(d, "b").getMethod(), "invalid method");
        Check.equal("application/x-www-form-urlencoded", FormTestSupport.form(d, "b").getEnctype(), "invalid enctype");
        Check.equal("http://example.test/dir/page.html", FormTestSupport.form(d, "b").getAction(), "no action: the document URL");
        Check.equal("dialog", FormTestSupport.form(d, "c").getMethod(), "dialog");
        Check.isTrue(FormTestSupport.form(d, "c").isNoValidate(), "novalidate");
    }
}
