package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormEntryList;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.FormBindings;

/**
 * Installs <code>FormData</code> (XMLHttpRequest Standard, section 4) and the <code>multipart/form-data</code> encoder that
 * <code>fetch()</code> and <code>XMLHttpRequest</code> use for a form data body. An entry list holds (name, value) pairs whose value is a
 * string or a <code>File</code>. <code>new FormData(form)</code> collects the entry list of an <code>HTMLFormElement</code> with the algorithm of the form
 * layer (<code>FormEntryList</code>): it reads the live state of the controls (what the user typed, the checkedness, the selected options, the files that the
 * user chose); a file input without a selection contributes an empty file, and a page can never read a local file that the user did not choose.
 * Elements that are not <code>HtmlFormElement</code> objects fall back to the content attributes. Iteration returns snapshot iterators.
 * <p>
 * <code>FormData</code> 를 설치한다(XMLHttpRequest 표준 4절). 그리고 <code>fetch()</code> 와 <code>XMLHttpRequest</code> 가 폼 데이터 본문에 쓰는
 * <code>multipart/form-data</code> 인코더도 둔다. 항목 목록은 값이 문자열 또는 <code>File</code> 인 (이름, 값) 쌍이다. <code>new FormData(form)</code> 은
 * <code>HTMLFormElement</code> 의 항목 목록을 폼 계층의 알고리즘(<code>FormEntryList</code>)으로 모은다. 컨트롤의 실제 상태(사용자가 입력한 것, checkedness,
 * 선택한 option, 사용자가 고른 파일)를 읽으며, 선택이 없는 파일 입력은 빈 파일을 낸다. 사용자가 고르지 않은 로컬 파일을 페이지가 읽을 수는 없다.
 * <code>HtmlFormElement</code> 가 아닌 요소는 콘텐츠 속성을 읽는 방식으로 대신한다. 반복은 스냅샷 반복자를 반환한다.
 */
final class FormDataApi {

    /**
     * Creates the script <code>File</code> objects of the files that the user selected in a file upload control (the realm gives it to the form
     * bindings under {@link FormBindings#FILE_FACTORY_KEY}).
     * <p>
     * 파일 업로드 컨트롤에서 사용자가 선택한 파일의 스크립트 <code>File</code> 객체를 만든다(realm 이 폼 바인딩에 {@link FormBindings#FILE_FACTORY_KEY} 로 준다).
     */
    private static final class SelectedFiles extends JsObject implements FormBindings.FileFactory {
        /** The page APIs. / 페이지 API. */
        private final WebApis api;

        /**
         * Creates the factory.
         * <p>
         * 팩터리를 만든다.
         *
         * @param api the page APIs / 페이지 API
         */
        SelectedFiles(WebApis api) {
            super(api.realm().getObjectPrototype());
            this.api = api;
        }

        /**
         * Creates the <code>File</code> of a selected file.
         * <p>
         * 선택한 파일의 <code>File</code> 을 만든다.
         *
         * @param entry the file / 파일
         * @return the object / 객체
         */
        public JsObject newFile(FileEntry entry) {
            return api.blobs.newFile(entry.getContent(), entry.getType(), entry.getName(), (double) entry.getLastModified());
        }
    }

    /**
     * The script object of a <code>FormData</code>.
     * <p>
     * <code>FormData</code> 의 스크립트 객체.
     */
    static final class FormDataObject extends JsObject {
        /** The entries: {name, value} where value is a String or a {@link BlobApi.FileObject}. / 항목: {이름, 값}. 값은 String 또는 {@link BlobApi.FileObject}. */
        final List<Object[]> entries = new ArrayList<Object[]>();

        /**
         * Creates an empty object.
         * <p>
         * 빈 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        FormDataObject(JsObject proto) {
            super(proto);
        }
    }

    /** The characters of a multipart boundary. / multipart 경계에 쓰는 문자. */
    private static final String BOUNDARY_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype. / 프로토타입. */
    private JsObject proto;

    /** The source of boundary characters. / 경계 문자의 출처. */
    private final Random random = new Random();

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    FormDataApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>FormData</code>.
     * <p>
     * <code>FormData</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        // 사용자가 고른 파일의 File 객체를 만드는 팩터리를 realm 에 둔다(input.files 가 쓴다).
        api.realm().setIntrinsic(FormBindings.FILE_FACTORY_KEY, new SelectedFiles(api));
        JsRealm realm = api.realm();
        Natives.ClassDef c = Natives.defineClass(realm, g, "FormData", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                FormDataObject f = new FormDataObject(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                JsValue form = Js.arg(args, 0);
                if (!form.isUndefined()) {
                    Object el = DomBridge.targetOf(form, Element.class);
                    if (!(el instanceof Element) || !"form".equals(((Element) el).getLocalName())) {
                        throw JsException.typeError("Failed to construct 'FormData': parameter 1 is not of type 'HTMLFormElement'.");
                    }
                    Element submitter = null;
                    JsValue sv = Js.arg(args, 1);
                    if (!sv.isNullOrUndefined()) {
                        Object s = DomBridge.targetOf(sv, Element.class);
                        if (!(s instanceof Element)) {
                            throw JsException.typeError("Failed to construct 'FormData': parameter 2 is not of type 'HTMLElement'.");
                        }
                        submitter = (Element) s;
                    }
                    collect((Element) el, submitter, f.entries);
                }
                return f;
            }
        });
        proto = c.prototype;
        Natives.method(realm, proto, "append", 2, this, "append");
        Natives.method(realm, proto, "delete", 1, this, "delete");
        Natives.method(realm, proto, "get", 1, this, "get");
        Natives.method(realm, proto, "getAll", 1, this, "getAll");
        Natives.method(realm, proto, "has", 1, this, "has");
        Natives.method(realm, proto, "set", 2, this, "set");
        Natives.method(realm, proto, "forEach", 1, this, "forEach");
        Natives.method(realm, proto, "keys", 0, this, "keys");
        Natives.method(realm, proto, "values", 0, this, "values");
        NativeFunction entries = Natives.function(realm, "entries", 0, this, "entries");
        proto.defineData("entries", entries, true, true, true);
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, entries);
    }

    /**
     * Creates an empty <code>FormData</code> object for a Java caller.
     * <p>
     * Java 호출자를 위해 빈 <code>FormData</code> 객체를 만든다.
     *
     * @return the object / 객체
     */
    FormDataObject create() {
        return new FormDataObject(proto);
    }

    /**
     * Collects the entry list of a form: with the algorithm of the form layer from the state of its controls, or from the content attributes of the
     * controls if the element is not an <code>HtmlFormElement</code>.
     * <p>
     * 폼의 항목 목록을 모은다: 폼 계층의 알고리즘으로 컨트롤의 상태에서, <code>HtmlFormElement</code> 가 아닌 요소이면 컨트롤의 콘텐츠 속성에서 모은다.
     *
     * @param form      the form element / 폼 요소
     * @param submitter the submit button that started the submission, or null / 전송을 시작한 제출 버튼. 없으면 null
     * @param out       receives the entries / 항목을 받는다
     */
    private void collect(Element form, Element submitter, List<Object[]> out) {
        if (form instanceof HtmlFormElement) {
            List<FormEntry> entries = FormEntryList.construct((HtmlFormElement) form, submitter, "UTF-8");
            // 폼 계층이 만든 항목을 이름과 값(문자열 또는 File)의 쌍으로 옮긴다.
            for (int i = 0; i < entries.size(); i++) {
                FormEntry e = entries.get(i);
                if (e.isFile()) {
                    FileEntry f = e.getFile();
                    out.add(new Object[] { e.getName(), api.blobs.newFile(f.getContent(), f.getType().length() == 0 ? "application/octet-stream" : f.getType(),
                            f.getName(), (double) f.getLastModified()) });
                } else {
                    out.add(new Object[] { e.getName(), e.getValue() });
                }
            }
            return;
        }
        Node n = Node.nextInTree(form, form);
        // 폼 안의 모든 후손을 문서 순서대로 방문하며 성공 컨트롤을 찾는다.
        while (n != null) {
            if (n instanceof Element) {
                control((Element) n, submitter, out);
            }
            n = Node.nextInTree(n, form);
        }
    }

    /**
     * Adds the entries of one control element.
     * <p>
     * 컨트롤 요소 하나의 항목을 추가한다.
     *
     * @param e         the element / 요소
     * @param submitter the submitter or null / 제출 버튼 또는 null
     * @param out       receives the entries / 항목을 받는다
     */
    private void control(Element e, Element submitter, List<Object[]> out) {
        String tag = e.getLocalName();
        String name = e.getAttribute("name");
        if (name == null || name.length() == 0 || e.hasAttribute("disabled")) {
            return;
        }
        if ("input".equals(tag)) {
            String type = e.getAttribute("type") == null ? "text" : e.getAttribute("type").toLowerCase();
            String value = e.getAttribute("value");
            if ("checkbox".equals(type) || "radio".equals(type)) {
                if (e.hasAttribute("checked")) {
                    out.add(new Object[] { name, value == null ? "on" : value });
                }
            } else if ("file".equals(type)) {
                // 페이지는 로컬 파일을 읽을 수 없으므로 선택한 파일이 없는 것과 같은 빈 파일을 낸다.
                out.add(new Object[] { name, api.blobs.newFile(new byte[0], "application/octet-stream", "", System.currentTimeMillis()) });
            } else if ("submit".equals(type) || "image".equals(type)) {
                if (e == submitter) {
                    out.add(new Object[] { name, value == null ? "" : value });
                }
            } else if (!"button".equals(type) && !"reset".equals(type)) {
                out.add(new Object[] { name, value == null ? "" : value });
            }
        } else if ("textarea".equals(tag)) {
            out.add(new Object[] { name, e.getTextContent() });
        } else if ("select".equals(tag)) {
            selectEntries(e, name, out);
        } else if ("button".equals(tag)) {
            String type = e.getAttribute("type") == null ? "submit" : e.getAttribute("type").toLowerCase();
            if ("submit".equals(type) && e == submitter) {
                out.add(new Object[] { name, e.getAttribute("value") == null ? "" : e.getAttribute("value") });
            }
        }
    }

    /**
     * Adds the selected options of a <code>select</code> element.
     * <p>
     * <code>select</code> 요소의 선택된 옵션을 추가한다.
     *
     * @param select the element / 요소
     * @param name   the control name / 컨트롤 이름
     * @param out    receives the entries / 항목을 받는다
     */
    private void selectEntries(Element select, String name, List<Object[]> out) {
        boolean multiple = select.hasAttribute("multiple");
        Element first = null;
        boolean any = false;
        Node n = Node.nextInTree(select, select);
        // select 안의 option 을 문서 순서대로 보며 selected 인 것을 모은다.
        while (n != null) {
            if (n instanceof Element && "option".equals(((Element) n).getLocalName()) && !((Element) n).hasAttribute("disabled")) {
                Element o = (Element) n;
                if (first == null) {
                    first = o;
                }
                if (o.hasAttribute("selected") && (multiple || !any)) {
                    out.add(new Object[] { name, optionValue(o) });
                    any = true;
                }
            }
            n = Node.nextInTree(n, select);
        }
        // 단일 선택에서 선택된 옵션이 없으면 첫 번째 옵션이 선택된 것으로 본다.
        if (!any && !multiple && first != null) {
            out.add(new Object[] { name, optionValue(first) });
        }
    }

    /**
     * Returns the value of an option: its <code>value</code> attribute or its text.
     * <p>
     * 옵션의 값을 반환한다: <code>value</code> 속성 또는 텍스트.
     *
     * @param o the option / 옵션
     * @return the value / 값
     */
    private static String optionValue(Element o) {
        String v = o.getAttribute("value");
        return v != null ? v : o.getTextContent().trim();
    }

    /**
     * Converts the value arguments of <code>append</code> and <code>set</code> to an entry value.
     * <p>
     * <code>append</code> 와 <code>set</code> 의 값 인수를 항목 값으로 바꾼다.
     *
     * @param method the method name for messages / 메시지용 메서드 이름
     * @param args   the arguments (name, value[, filename]) / 인수(이름, 값[, 파일 이름])
     * @return a String or a file object / String 또는 파일 객체
     */
    private Object entryValue(String method, JsValue[] args) {
        JsValue v = args[1];
        if (v instanceof BlobApi.BlobObject) {
            BlobApi.BlobObject b = (BlobApi.BlobObject) v;
            if (args.length > 2 && !args[2].isUndefined()) {
                return api.blobs.newFile(b.data, b.type, Natives.usv(args[2]).replace('/', ':'), System.currentTimeMillis());
            }
            if (b instanceof BlobApi.FileObject) {
                return b;
            }
            return api.blobs.newFile(b.data, b.type, "blob", System.currentTimeMillis());
        }
        if (args.length > 2) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'FormData': parameter 2 is not of type 'Blob'.");
        }
        return Natives.usv(v);
    }

    /**
     * Converts an entry value to a script value.
     * <p>
     * 항목 값을 스크립트 값으로 바꾼다.
     *
     * @param v a String or a file object / String 또는 파일 객체
     * @return the script value / 스크립트 값
     */
    private static JsValue scriptValue(Object v) {
        return v instanceof String ? Js.str((String) v) : (JsValue) v;
    }

    /**
     * Implements <code>formData.append(name, value, filename)</code>.
     * <p>
     * <code>formData.append(name, value, filename)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue append(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 2, "append", "FormData");
        f.entries.add(new Object[] { Natives.usv(args[0]), entryValue("append", args) });
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>formData.delete(name)</code>.
     * <p>
     * <code>formData.delete(name)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue delete(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 1, "delete", "FormData");
        String name = Natives.usv(args[0]);
        // 이름이 같은 항목을 모두 지운다.
        for (int i = f.entries.size() - 1; i >= 0; i--) {
            if (f.entries.get(i)[0].equals(name)) {
                f.entries.remove(i);
            }
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>formData.get(name)</code>.
     * <p>
     * <code>formData.get(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the first value or null / 첫 값 또는 null
     */
    JsValue get(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 1, "get", "FormData");
        String name = Natives.usv(args[0]);
        // 이름이 같은 첫 항목의 값을 찾는다.
        for (int i = 0; i < f.entries.size(); i++) {
            if (f.entries.get(i)[0].equals(name)) {
                return scriptValue(f.entries.get(i)[1]);
            }
        }
        return Js.NULL;
    }

    /**
     * Implements <code>formData.getAll(name)</code>.
     * <p>
     * <code>formData.getAll(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an array of values / 값의 배열
     */
    JsValue getAll(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 1, "getAll", "FormData");
        String name = Natives.usv(args[0]);
        List<JsValue> out = new ArrayList<JsValue>();
        // 이름이 같은 항목의 값을 순서대로 모은다.
        for (int i = 0; i < f.entries.size(); i++) {
            if (f.entries.get(i)[0].equals(name)) {
                out.add(scriptValue(f.entries.get(i)[1]));
            }
        }
        return api.realm().newArray(out);
    }

    /**
     * Implements <code>formData.has(name)</code>.
     * <p>
     * <code>formData.has(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return whether an entry exists / 항목이 있는지
     */
    JsValue has(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 1, "has", "FormData");
        String name = Natives.usv(args[0]);
        // 이름이 같은 항목이 하나라도 있는지 찾는다.
        for (int i = 0; i < f.entries.size(); i++) {
            if (f.entries.get(i)[0].equals(name)) {
                return Js.TRUE;
            }
        }
        return Js.FALSE;
    }

    /**
     * Implements <code>formData.set(name, value, filename)</code>.
     * <p>
     * <code>formData.set(name, value, filename)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue set(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 2, "set", "FormData");
        String name = Natives.usv(args[0]);
        Object value = entryValue("set", args);
        int at = -1;
        // 이름이 같은 첫 항목은 새 항목으로 바꾸고 나머지는 지운다.
        for (int i = 0; i < f.entries.size(); i++) {
            if (f.entries.get(i)[0].equals(name)) {
                if (at < 0) {
                    at = i;
                    f.entries.set(i, new Object[] { name, value });
                } else {
                    f.entries.remove(i);
                    i--;
                }
            }
        }
        if (at < 0) {
            f.entries.add(new Object[] { name, value });
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>formData.forEach(callback, thisArg)</code>.
     * <p>
     * <code>formData.forEach(callback, thisArg)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue forEach(JsValue self, JsValue[] args) {
        FormDataObject f = Natives.self(self, FormDataObject.class);
        Natives.requireArgs(args, 1, "forEach", "FormData");
        if (!args[0].isCallable()) {
            throw JsException.typeError("Failed to execute 'forEach' on 'FormData': The callback provided as parameter 1 is not a function.");
        }
        JsValue thisArg = Js.arg(args, 1);
        // 호출 중 목록이 바뀔 수 있으므로 현재 크기를 매번 확인하며 순회한다.
        for (int i = 0; i < f.entries.size(); i++) {
            Object[] e = f.entries.get(i);
            Ops.call(args[0], thisArg, new JsValue[] { scriptValue(e[1]), Js.str((String) e[0]), f });
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>formData.keys()</code>.
     * <p>
     * <code>formData.keys()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue keys(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, FormDataObject.class), 0);
    }

    /**
     * Implements <code>formData.values()</code>.
     * <p>
     * <code>formData.values()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue values(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, FormDataObject.class), 1);
    }

    /**
     * Implements <code>formData.entries()</code> and <code>formData[Symbol.iterator]()</code>.
     * <p>
     * <code>formData.entries()</code> 와 <code>formData[Symbol.iterator]()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue entries(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, FormDataObject.class), 2);
    }

    /**
     * Creates an iterator over a snapshot of names, values or pairs.
     * <p>
     * 이름, 값, 쌍의 스냅샷에 대한 반복자를 만든다.
     *
     * @param f    the object / 객체
     * @param kind 0 names, 1 values, 2 pairs / 0 이름, 1 값, 2 쌍
     * @return the iterator / 반복자
     */
    private JsValue iterate(FormDataObject f, int kind) {
        List<JsValue> out = new ArrayList<JsValue>();
        // 현재 항목의 스냅샷으로 배열을 만들어 그 반복자를 돌려준다.
        for (int i = 0; i < f.entries.size(); i++) {
            Object[] e = f.entries.get(i);
            if (kind == 0) {
                out.add(Js.str((String) e[0]));
            } else if (kind == 1) {
                out.add(scriptValue(e[1]));
            } else {
                out.add(Natives.pair(api.realm(), Js.str((String) e[0]), scriptValue(e[1])));
            }
        }
        return Natives.iterator(api.realm(), out);
    }

    /**
     * Creates a random multipart boundary.
     * <p>
     * 무작위 multipart 경계를 만든다.
     *
     * @return the boundary text / 경계 문자열
     */
    String newBoundary() {
        StringBuilder b = new StringBuilder("----SwingWebPanelFormBoundary");
        // 영문자와 숫자 16개를 무작위로 덧붙인다.
        for (int i = 0; i < 16; i++) {
            b.append(BOUNDARY_CHARS.charAt(random.nextInt(BOUNDARY_CHARS.length())));
        }
        return b.toString();
    }

    /**
     * Encodes an entry list as <code>multipart/form-data</code>: every part has a <code>Content-Disposition</code> header with the escaped
     * name (and file name), file parts a <code>Content-Type</code>, string values use CRLF line endings.
     * <p>
     * 항목 목록을 <code>multipart/form-data</code> 로 인코딩한다: 모든 부분은 이스케이프한 이름(과 파일 이름)을 가진 <code>Content-Disposition</code> 헤더를
     * 갖고 파일 부분은 <code>Content-Type</code> 도 가지며, 문자열 값은 CRLF 줄 끝을 쓴다.
     *
     * @param entries  the entries / 항목
     * @param boundary the boundary text / 경계 문자열
     * @return the encoded body / 인코딩한 본문
     */
    static byte[] multipart(List<Object[]> entries, String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 항목마다 경계 줄, 헤더, 빈 줄, 값, CRLF 순서로 쓴다.
        for (int i = 0; i < entries.size(); i++) {
            Object[] e = entries.get(i);
            StringBuilder head = new StringBuilder();
            head.append("--").append(boundary).append("\r\n");
            head.append("Content-Disposition: form-data; name=\"").append(escape(crlf((String) e[0]))).append('"');
            byte[] value;
            if (e[1] instanceof BlobApi.FileObject) {
                BlobApi.FileObject file = (BlobApi.FileObject) e[1];
                head.append("; filename=\"").append(escape(file.name)).append("\"\r\n");
                head.append("Content-Type: ").append(file.type.length() == 0 ? "application/octet-stream" : file.type).append("\r\n\r\n");
                value = file.data;
            } else {
                head.append("\r\n\r\n");
                value = Encodings.utf8(crlf((String) e[1]));
            }
            byte[] h = Encodings.utf8(head.toString());
            out.write(h, 0, h.length);
            out.write(value, 0, value.length);
            out.write('\r');
            out.write('\n');
        }
        byte[] end = Encodings.utf8("--" + boundary + "--\r\n");
        out.write(end, 0, end.length);
        return out.toByteArray();
    }

    /**
     * Normalizes every line ending of a text to CRLF.
     * <p>
     * 텍스트의 모든 줄 끝을 CRLF 로 바꾼다.
     *
     * @param s the text / 텍스트
     * @return the normalized text / 정규화한 텍스트
     */
    private static String crlf(String s) {
        if (s.indexOf('\n') < 0 && s.indexOf('\r') < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length() + 8);
        // CR 만 있거나 LF 만 있는 줄 끝도 CRLF 로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append("\r\n");
            } else if (c == '\n') {
                out.append("\r\n");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Escapes a name or file name for the <code>Content-Disposition</code> header: LF, CR and the double quote are percent-encoded.
     * <p>
     * 이름이나 파일 이름을 <code>Content-Disposition</code> 헤더용으로 이스케이프한다: LF, CR, 큰따옴표를 퍼센트 인코딩한다.
     *
     * @param s the text / 텍스트
     * @return the escaped text / 이스케이프한 텍스트
     */
    private static String escape(String s) {
        return s.replace("\n", "%0A").replace("\r", "%0D").replace("\"", "%22");
    }
}
