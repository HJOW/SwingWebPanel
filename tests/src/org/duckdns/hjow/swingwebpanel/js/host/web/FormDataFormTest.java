package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>new FormData(form)</code> with the form layer (plan items P11-06 and P20-08): the entry list is built from the state of the controls (the
 * typed value, the checkedness, the selected options, the files that the user chose, the submitter), with the line breaks of the form data set, and the
 * files are real <code>File</code> objects.
 * <p>
 * 폼 계층과 함께 쓰는 <code>new FormData(form)</code> 시험(계획 P11-06, P20-08): 항목 목록은 컨트롤의 상태(입력한 값, checkedness, 선택한 option, 사용자가 고른 파일,
 * submitter)로 만들고 폼 데이터 집합의 줄 바꿈을 따르며, 파일은 실제 <code>File</code> 객체이다.
 */
public class FormDataFormTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormDataFormTest() {
    }

    /**
     * The test page.
     * <p>
     * 시험 페이지.
     */
    private static final String PAGE = "<html><body><form id=f><input id=t name=a value=1><input id=cb name=b type=checkbox checked><input name=c type=checkbox value=v>"
            + "<input id=r1 name=g type=radio value=x checked><input id=r2 name=g type=radio value=y><input name=h type=hidden>"
            + "<input type=submit id=sb name=s value=go><button id=bb name=bn value=B>b</button><input name=dis disabled value=x>"
            + "<textarea id=ta name=t>text\nmore</textarea><select id=sel name=sel><option value=o1>One</option><option value=o2 selected>Two</option></select>"
            + "<select id=ms name=ms multiple><option value=m1 selected>1</option><option value=m2>2</option><option value=m3 selected>3</option></select>"
            + "<input id=fi name=up type=file multiple><input name=nm></form></body></html>";

    /**
     * The entries follow the state of the controls, not the attributes.
     * <p>
     * 항목은 속성이 아니라 컨트롤의 상태를 따른다.
     */
    public void testEntriesFollowTheState() {
        WebFixture f = new WebFixture("http://example.test/p", PAGE);
        String all = "Array.from(new FormData(document.getElementById('f'))).map(function (e) { return e[0] + '=' + (typeof e[1] === 'string' ? e[1] : '<' + e[1].name + '>'); }).join(';')";
        Check.equal("a=1;b=on;g=x;h=;t=text\r\nmore;sel=o2;ms=m1;ms=m3;up=<>;nm=", f.js(all), "the initial state");
        f.js("var t = document.getElementById('t'); t.value = 'typed'; document.getElementById('cb').checked = false; document.getElementById('r2').checked = true;"
                + "document.getElementById('sel').selectedIndex = 0; document.getElementById('ta').value = 'a\\nb'; document.getElementById('ms').options[1].selected = true;");
        Check.equal("a=typed;g=y;h=;t=a\r\nb;sel=o1;ms=m1;ms=m2;ms=m3;up=<>;nm=", f.js(all), "the state after script changes");
    }

    /**
     * The submitter is in the entry list; a button that is not the submitter is not.
     * <p>
     * submitter 는 항목 목록에 있고, submitter 가 아닌 버튼은 없다.
     */
    public void testSubmitter() {
        WebFixture f = new WebFixture("http://example.test/p", PAGE);
        Check.equal("true", f.js("var fd = new FormData(document.getElementById('f'), document.getElementById('sb')); String(fd.get('s') === 'go' && !fd.has('bn'))"), "submit input");
        Check.equal("true", f.js("var fd2 = new FormData(document.getElementById('f'), document.getElementById('bb')); String(fd2.get('bn') === 'B' && !fd2.has('s'))"), "button element");
        Check.equal("false", f.js("String(new FormData(document.getElementById('f')).has('s'))"), "no submitter, no button");
    }

    /**
     * The files that the user chose are <code>File</code> objects with their names and sizes; <code>input.files</code> gives the same kind of object.
     * <p>
     * 사용자가 고른 파일은 이름과 크기를 가진 <code>File</code> 객체이다. <code>input.files</code> 도 같은 종류의 객체를 준다.
     */
    public void testFiles() {
        WebFixture f = new WebFixture("http://example.test/p", PAGE);
        HtmlInputElement in = (HtmlInputElement) f.scripts.getDocument().getElementById("fi");
        in.setSelectedFiles(Arrays.asList(new FileEntry("a.txt", "text/plain", 5, new byte[] {104, 105}), new FileEntry("b.bin", "", 6, new byte[] {1, 2, 3})));
        Check.equal("2|a.txt|2|text/plain|b.bin|3|application/octet-stream", f.js("var fd = new FormData(document.getElementById('f')); var up = fd.getAll('up');"
                + "[up.length, up[0].name, up[0].size, up[0].type, up[1].name, up[1].size, up[1].type].join('|')"), "FormData files");
        Check.equal("true|2|a.txt|true", f.js("var inp = document.getElementById('fi'); [inp.files instanceof FileList, inp.files.length, inp.files[0].name, inp.files[0] instanceof File].join('|')"),
                "input.files");
        Check.equal("true", f.js("String(inp.files[0] === inp.files[0] && inp.files === inp.files)"), "same objects");
        Check.equal("C:\\fakepath\\a.txt", f.js("inp.value"), "value of a file input");
    }
}
