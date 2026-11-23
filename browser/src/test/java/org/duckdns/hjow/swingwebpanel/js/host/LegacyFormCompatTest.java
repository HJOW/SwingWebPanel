package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The named access of old pages to forms and form controls (plan item P07-07-c, LEG-14; HTML5 REC 2014-10-28 sections 4.10.3 and 5.3,
 * "named access on the Document object" and "named access on the Window object"): <code>document.forms['f']</code> and
 * <code>document.f</code>, the controls of a form by <code>name</code> and by <code>id</code> (<code>form.a</code>,
 * <code>form.elements['a']</code>), a control that has both a name and an id, radio buttons with one name, the <code>type</code> of
 * <code>input</code> elements with old, unknown and mixed-case values, and the <code>isindex</code> form.
 * <p>
 * 오래된 페이지가 폼과 폼 컨트롤에 이름으로 접근하는 방식(계획 P07-07-c, LEG-14, HTML5 REC 2014-10-28 §4.10.3, §5.3 "Document 객체의 이름 접근", "Window 객체의 이름 접근"):
 * <code>document.forms['f']</code> 와 <code>document.f</code>, 폼의 컨트롤을 <code>name</code> 과 <code>id</code> 로 찾기(<code>form.a</code>,
 * <code>form.elements['a']</code>), name 과 id 를 모두 가진 컨트롤, 이름이 같은 라디오 버튼, 옛 값, 알 수 없는 값, 대소문자가 섞인 값의 <code>input</code> <code>type</code>,
 * <code>isindex</code> 폼.
 */
public class LegacyFormCompatTest {

    /**
     * The page of the named access cases.
     * <p>
     * 이름 접근 사례의 페이지.
     */
    private static final String PAGE = "<!doctype html><body>"
            + "<form name=\"f1\" id=\"f1id\" action=\"/go\"><input name=\"a\" id=\"aid\" value=\"A\"><input id=\"onlyid\" value=\"I\"><input name=\"onlyname\" value=\"N\">"
            + "<input type=\"radio\" name=\"r\" value=\"1\"><input type=\"radio\" name=\"r\" value=\"2\" checked><select name=\"s\"><option value=\"o\">o</option></select>"
            + "<textarea name=\"t\">T</textarea><button name=\"b\" type=\"button\">B</button><fieldset name=\"fs\"></fieldset><object name=\"ob\"></object></form>"
            + "<form id=\"f2\"></form><form name=\"f3\"></form>"
            + "<img name=\"im\" id=\"imid\" src=\"x.png\"><img id=\"onlyimg\" src=\"y.png\"><embed name=\"em\"><object name=\"obj\"></object><a name=\"an\" id=\"anid\">a</a>"
            + "<div id=\"d\"></div><span name=\"sp\"></span>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyFormCompatTest() {
    }

    /**
     * Evaluates a script on the page of the named access cases.
     * <p>
     * 이름 접근 사례의 페이지에서 스크립트를 평가한다.
     *
     * @param f      the page / 페이지
     * @param script the script / 스크립트
     * @return the result as text / 문자열인 결과
     */
    private static String run(PageFixture f, String script) {
        return f.scripts.evaluate(script, "t");
    }

    /**
     * <code>document.forms</code> finds a form by <code>name</code> and by <code>id</code>, in tree order, and the forms without a name
     * or an id only by index.
     * <p>
     * <code>document.forms</code> 는 폼을 <code>name</code> 과 <code>id</code> 로 트리 순서에서 찾으며, name 도 id 도 없는 폼은 인덱스로만 찾는다.
     */
    public void testDocumentForms() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("true|true|true", run(f, "[document.forms['f1'] === document.forms[0], document.forms['f1id'] === document.forms[0], "
                + "document.forms.f1 === document.forms.namedItem('f1id')].join('|')"), "a form by name and by id");
        Check.equal("true|true", run(f, "[document.forms['f2'] === document.forms[1], document.forms['f3'] === document.forms[2]].join('|')"), "id only and name only");
        Check.equal("3|undefined", run(f, "document.forms.length + '|' + document.forms['nothing']"), "length and an unknown name");
        Check.equal("true|true|true", run(f, "[document.f1 === document.forms[0], document.f3 === document.forms[2], document.f2 === undefined].join('|')"),
                "document.f1: forms with a name are properties of the document, one with an id only is not");
    }

    /**
     * The named properties of the document: <code>img</code>, <code>embed</code>, <code>object</code> and <code>form</code> elements with
     * a <code>name</code>, and <code>img</code> elements with an <code>id</code> only if they have a name as well.
     * <p>
     * document 의 이름 있는 프로퍼티: <code>name</code> 이 있는 <code>img</code>, <code>embed</code>, <code>object</code>, <code>form</code> 요소. <code>img</code> 요소는
     * name 도 있을 때만 <code>id</code> 도 쓴다.
     */
    public void testDocumentNamedProperties() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("true|true", run(f, "[document.im === document.images[0], document.imid === document.images[0]].join('|')"),
                "an img with a name and an id: both names give it");
        Check.equal("undefined|undefined", run(f, "[String(document.onlyimg), String(document.an)].join('|')"), "an img with an id only, and an a element, are not properties");
        Check.equal("EMBED|OBJECT", run(f, "[document.em.tagName, document.obj.tagName].join('|')"), "embed and object with a name");
        Check.equal("undefined|undefined", run(f, "[String(document.sp), String(document.d)].join('|')"), "other elements are not properties");
        Check.equal("img|f1", run(f, "[typeof document.im === 'object' ? 'img' : 'x', document.f1.name].join('|')"), "types");
    }

    /**
     * The controls of a form by <code>name</code> and by <code>id</code>: the properties of the form, <code>form.elements</code>,
     * <code>namedItem</code>, a control with a name and an id, and radio buttons with one name.
     * <p>
     * 폼의 컨트롤을 <code>name</code> 과 <code>id</code> 로 찾기: 폼의 프로퍼티, <code>form.elements</code>, <code>namedItem</code>, name 과 id 가 모두 있는 컨트롤,
     * 이름이 같은 라디오 버튼.
     */
    public void testFormControlsByNameAndId() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("A|A|A|A", run(f, "var form = document.forms[0]; [form.a.value, form.aid.value, form.elements['a'].value, form.elements['aid'].value].join('|')"),
                "a control with a name and an id");
        Check.equal("I|N", run(f, "[form.onlyid.value, form.onlyname.value].join('|')"), "an id only, a name only");
        Check.equal("I|N", run(f, "[form.elements.onlyid.value, form.elements.namedItem('onlyname').value].join('|')"), "elements.namedItem");
        Check.equal("true|2", run(f, "[form.a === form.elements.a, form.r.value].join('|')"), "the same object, and the value of a radio group");
        Check.equal("2|RadioNodeList", run(f, "[form.r.length, Object.prototype.toString.call(form.r).slice(8, -1)].join('|')"), "radio buttons with one name make a list");
        Check.equal("o|T|B", run(f, "[form.s.value, form.t.value, form.b.textContent].join('|')"), "select, textarea and button by name");
        Check.equal("true|true|true", run(f, "[form.elements.length >= 8, form.fs === undefined || form.fs.tagName === 'FIELDSET', document.getElementById('aid').form === form].join('|')"),
                "the length of the controls, fieldset, and the form of a control");
        Check.equal("undefined", run(f, "String(form.ob)"), "an object element is not a listed control of the form");
        Check.equal("undefined|null", run(f, "[String(form.nothing), String(form.elements.namedItem('nothing'))].join('|')"), "unknown names");
    }

    /**
     * The named properties follow the document (live), a name that several elements have gives a collection, and a name never hides a
     * member that the document has (an old page with a form named <code>cookie</code> still has <code>document.cookie</code>).
     * <p>
     * 이름 있는 프로퍼티는 문서를 따르고(라이브), 여러 요소가 가진 이름은 컬렉션을 주며, 이름은 문서가 가진 멤버를 가리지 않는다(<code>cookie</code> 라는 폼이 있는 옛 페이지에도
     * <code>document.cookie</code> 가 있다).
     */
    public void testDocumentNamedAccessIsLiveAndNeverHidesMembers() {
        PageFixture f = new PageFixture().load("<!doctype html><body><form name=\"cookie\"></form><img name=\"getElementById\" src=\"x.png\">"
                + "<form name=\"dup\"></form><img name=\"dup\" src=\"y.png\"><form name=\"cookie\"></form>");
        Check.equal("function|string", run(f, "[typeof document.getElementById, typeof document.cookie].join('|')"), "built-in members are not hidden");
        Check.equal("2|FORM|IMG", run(f, "[document.dup.length, document.dup[0].tagName, document.dup[1].tagName].join('|')"), "a name of two elements gives a collection in tree order");
        Check.equal("true", run(f, "String(document.dup.item(0) === document.forms[1])"), "the collection holds the elements");
        Check.equal("true|undefined|true", run(f, "var form = document.createElement('form'); form.setAttribute('name', 'late'); document.body.appendChild(form);"
                + " var r1 = document.late === form; form.setAttribute('name', 'renamed'); var r2 = String(document.late); var r3 = document.renamed === form;"
                + " [r1, r2, r3].join('|')"), "a form that is added, then renamed");
        Check.equal("undefined", run(f, "document.body.removeChild(form); String(document.renamed)"), "a form that is removed");
        Check.equal("true", run(f, "var n = document.dup.length; document.body.appendChild(document.createElement('form')).name = 'dup'; String(document.dup.length === n + 1)"),
                "a collection of a name is live");
        Check.equal("undefined|undefined", run(f, "[String(document.nothing), String(document['no such name'])].join('|')"), "names that nothing has");
    }

    /**
     * Named access on the window: the id of every element and the name of <code>a</code>, <code>embed</code>, <code>form</code>,
     * <code>img</code> and <code>object</code> elements are global names, one element is the element and several are a live collection, a
     * name that the window already has is not hidden, a script that declares such a name gets its own variable, and an unknown name is still
     * a <code>ReferenceError</code>.
     * <p>
     * window 의 이름 접근: 모든 요소의 id 와 <code>a</code>, <code>embed</code>, <code>form</code>, <code>img</code>, <code>object</code> 요소의 name 이 전역 이름이다. 요소가
     * 하나이면 그 요소이고 여럿이면 라이브 컬렉션이다. window 에 이미 있는 이름은 가려지지 않고, 그런 이름을 선언하는 스크립트는 자신의 변수를 얻으며, 모르는 이름은 여전히
     * <code>ReferenceError</code> 이다.
     */
    public void testWindowNamedAccess() {
        PageFixture f = new PageFixture().load(PAGE + "<p id=\"dup2\">x</p><p id=\"dup2\">y</p><div id=\"location\"></div><div id=\"toString\"></div>");
        Check.equal("true|true|true", run(f, "[window.f1id === document.forms[0], window.f1 === document.forms[0], window.d === document.getElementById('d')].join('|')"),
                "an id of any element, and the name of a form");
        Check.equal("true|true|true", run(f, "[window.im === document.images[0], window.em.tagName === 'EMBED', window.an === document.getElementsByName('an')[0]].join('|')"),
                "the names of img, embed and a");
        Check.equal("undefined", run(f, "String(window.sp)"), "the name of a span is not a window property");
        Check.equal("true|true", run(f, "[d === document.getElementById('d'), typeof f1id === 'object'].join('|')"), "global names work as free variables");
        Check.equal("true|false", run(f, "['f1id' in window, 'nothing' in window].join('|')"), "in");
        Check.equal("2|P|P", run(f, "[dup2.length, dup2[0].tagName, dup2[1].tagName].join('|')"), "an id of two elements gives a collection");
        Check.equal("object|function", run(f, "[typeof window.location, typeof window.toString].join('|')"), "names of the window itself and of Object.prototype are not hidden");
        Check.equal("ReferenceError|undefined", run(f, "var e; try { unknownName; } catch (x) { e = x.name; } [e, typeof unknownName].join('|')"), "an unknown name");
        Check.equal("5|true", run(f, "var d = 5; [d, document.getElementById('d') !== null].join('|')"), "var d is a variable of its own, the element stays");
        Check.equal("true|undefined", run(f, "var added = document.createElement('div'); added.id = 'newid'; document.body.appendChild(added);"
                + " var r1 = window.newid === added; added.id = 'otherid'; [r1, String(window.newid)].join('|')"), "the names follow the document");
    }

    /**
     * The <code>type</code> of an <code>input</code> element is a state with a keyword that is compared without regard to case, and an
     * old or unknown value is the text state.
     * <p>
     * <code>input</code> 요소의 <code>type</code> 은 대소문자를 구분하지 않고 비교하는 키워드의 상태이며 옛 값이나 알 수 없는 값은 text 상태이다.
     */
    public void testInputTypeStates() {
        PageFixture f = new PageFixture().load("<!doctype html><body><form id=\"f\"><input id=\"i1\" type=\"TEXT\"><input id=\"i2\" type=\"Image\" src=\"x.png\">"
                + "<input id=\"i3\" type=\"foo\"><input id=\"i4\"><input id=\"i5\" type=\"\"><input id=\"i6\" type=\" checkbox\"><input id=\"i7\" type=\"CheckBox\">"
                + "<input id=\"i8\" type=\"datetime\"><input id=\"i9\" type=\"HIDDEN\" value=\"h\"></form>");
        Check.equal("text|image|text|text|text|text|checkbox|text|hidden", run(f, "var out = []; for (var n = 1; n <= 9; n++) { out.push(document.getElementById('i' + n).type); } out.join('|')"),
                "type states: the keyword is case-insensitive, white space is not removed, unknown values are text");
        Check.equal("TEXT|Image|foo||| checkbox", run(f, "[document.getElementById('i1').getAttribute('type'), document.getElementById('i2').getAttribute('type'),"
                + " document.getElementById('i3').getAttribute('type'), String(document.getElementById('i4').getAttribute('type')).replace('null', ''),"
                + " document.getElementById('i5').getAttribute('type'), document.getElementById('i6').getAttribute('type')].join('|')"), "the attribute keeps its value");
    }

    /**
     * The parser expands <code>isindex</code> into a form with a text field named <code>isindex</code>; the form is in the document
     * collections like any other form.
     * <p>
     * 파서는 <code>isindex</code> 를 <code>isindex</code> 라는 이름의 텍스트 필드가 있는 폼으로 확장한다. 그 폼은 다른 폼처럼 문서 컬렉션에 들어 있다.
     */
    public void testIsindexForm() {
        PageFixture f = new PageFixture().load("<!doctype html><body><isindex prompt=\"Search: \" action=\"/find\">");
        Check.equal("1|FORM|/find|isindex|text", run(f, "[document.forms.length, document.forms[0].tagName, document.forms[0].getAttribute('action'),"
                + " document.forms[0].elements[0].name, document.forms[0].elements[0].type].join('|')"), "the form and its field");
        Check.equal("Search: ", run(f, "document.forms[0].getElementsByTagName('label')[0].firstChild.data"), "the prompt");
    }
}
