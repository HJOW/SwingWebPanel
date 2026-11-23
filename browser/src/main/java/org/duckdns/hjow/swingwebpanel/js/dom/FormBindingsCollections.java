package org.duckdns.hjow.swingwebpanel.js.dom;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FileList;
import org.duckdns.hjow.swingwebpanel.dom.html.FormElementList;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionsCollection;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.dom.html.RadioNodeList;
import org.duckdns.hjow.swingwebpanel.dom.html.ValidityState;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The collection objects of the form bindings (see {@link FormBindings}): <code>HTMLFormControlsCollection</code> (also used for
 * <code>labels</code>, <code>selectedOptions</code>, <code>fieldset.elements</code> and <code>datalist.options</code>),
 * <code>HTMLOptionsCollection</code>, <code>RadioNodeList</code> and <code>FileList</code>, with their classification and index and name access, and
 * the indexed and named access of <code>HTMLFormElement</code> and <code>HTMLSelectElement</code> themselves.
 * <p>
 * 폼 바인딩의 컬렉션 객체({@link FormBindings} 참조): <code>HTMLFormControlsCollection</code>(<code>labels</code>, <code>selectedOptions</code>,
 * <code>fieldset.elements</code>, <code>datalist.options</code> 에도 쓴다), <code>HTMLOptionsCollection</code>, <code>RadioNodeList</code>,
 * <code>FileList</code> 와 그 분류, 위치와 이름 접근. <code>HTMLFormElement</code>, <code>HTMLSelectElement</code> 자신의 위치와 이름 접근도 다룬다.
 */
final class FormBindingsCollections {

    /**
     * The script objects of the selected files that were created (so that the same file is the same object while a script holds it). The values are weak: a script
     * object refers to its file entry and, through its realm, to the whole page, so a strong value would keep the entry (the weak key) and the page alive for as long
     * as the process lives. When nobody holds the script object any more a later access simply creates a new one, which no script can tell apart.
     * <p>
     * 만든 선택 파일의 스크립트 객체(스크립트가 쥐고 있는 동안 같은 파일이 같은 객체가 되도록). 값은 약한 참조이다. 스크립트 객체는 파일 항목을 참조하고 그의 realm
     * 을 통해 페이지 전체를 참조하므로 값을 강한 참조로 두면 (약한 키인) 항목과 페이지가 프로세스가 끝날 때까지 살아남는다. 아무도 스크립트 객체를 쥐고 있지
     * 않으면 나중의 접근이 새로 만들며, 스크립트는 그 차이를 알 수 없다.
     */
    private static final WeakHashMap<FileEntry, WeakReference<JsObject>> FILES = new WeakHashMap<FileEntry, WeakReference<JsObject>>();

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormBindingsCollections() {
    }

    /**
     * Chooses the interface of the collection objects of the form layer.
     * <p>
     * 폼 계층의 컬렉션 객체의 인터페이스를 고른다.
     *
     * @param o the Java object / Java 객체
     * @return the interface name or null / 인터페이스 이름 또는 null
     */
    static String classify(Object o) {
        if (o instanceof RadioNodeList) {
            return "RadioNodeList";
        } else if (o instanceof HtmlOptionsCollection) {
            return "HTMLOptionsCollection";
        } else if (o instanceof FormElementList) {
            return "HTMLFormControlsCollection";
        } else if (o instanceof ValidityState) {
            return "ValidityState";
        } else if (o instanceof FileList) {
            return "FileList";
        }
        return null;
    }

    /**
     * Returns the script <code>File</code> of an entry, creating it with the factory the first time.
     * <p>
     * 항목의 스크립트 <code>File</code> 을 반환하며, 처음에는 팩터리로 만든다.
     *
     * @param bridge  the bridge (unused, kept for symmetry) / 브리지(쓰지 않음)
     * @param factory the factory / 팩터리
     * @param entry   the file / 파일
     * @return the script object / 스크립트 객체
     */
    static JsValue fileOf(DomBridge bridge, FormBindings.FileFactory factory, FileEntry entry) {
        synchronized (FILES) {
            WeakReference<JsObject> ref = FILES.get(entry);
            JsObject f = ref == null ? null : ref.get();
            if (f == null) {
                f = factory.newFile(entry);
                FILES.put(entry, new WeakReference<JsObject>(f));
            }
            return f;
        }
    }

    /**
     * Installs the collection interfaces and <code>FileList</code>.
     * <p>
     * 컬렉션 인터페이스와 <code>FileList</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void install(final DomBridge bridge) {
        bridge.defineInterface("HTMLFormControlsCollection", "HTMLCollection", FormElementList.class, new String[] { "a:length", "m:item(int)",
            "m:namedItem(String)" });
        bridge.setIndexed("HTMLFormControlsCollection", new ListSource());
        DomBridge.InterfaceInfo options = bridge.defineInterface("HTMLOptionsCollection", "HTMLFormControlsCollection", HtmlOptionsCollection.class,
            new String[] { "a:length=", "a:selectedIndex=", "m:item(int)", "m:namedItem(String)", "m:remove(int)" });
        bridge.setIndexed("HTMLOptionsCollection", new ListSource());
        FormBindingsMembers.installOptionsAdd(bridge, options);
        bridge.defineInterface("RadioNodeList", "NodeList", RadioNodeList.class, new String[] { "a:length", "m:item(int)", "a:value=" });
        bridge.setIndexed("RadioNodeList", new ListSource());
        DomBridge.InterfaceInfo files = bridge.defineInterface("FileList", null, FileList.class, new String[] { "a:length" });
        bridge.setIndexed("FileList", new IndexedObject.Source() {
            /**
             * Returns the number of files.
             * <p>
             * 파일 수를 반환한다.
             *
             * @param t the list / 목록
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((FileList) t).getLength();
            }

            /**
             * Returns the <code>File</code> object at an index.
             * <p>
             * 위치의 <code>File</code> 객체를 반환한다.
             *
             * @param t the list / 목록
             * @param i the index / 위치
             * @return the object / 객체
             */
            public Object item(Object t, int i) {
                return FormBindings.fileObject(bridge, ((FileList) t).item(i));
            }

            /**
             * A file list has no named access.
             * <p>
             * 파일 목록에는 이름 접근이 없다.
             *
             * @param t the list / 목록
             * @param n the name / 이름
             * @return null / null
             */
            public Object named(Object t, String n) {
                return null;
            }
        });
        final JsRealm realm = bridge.getRealm();
        files.getPrototype().defineData("item", realm.newFunction("item", 1, new NativeFunction.Call() {
            /**
             * Returns the file at an index, or null.
             * <p>
             * 위치의 파일을 반환한다. 없으면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the file or null / 파일 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                FileList l = (FileList) DomBridge.targetOf(self, FileList.class);
                if (l == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'item' on 'FileList': 1 argument required, but only 0 present.");
                }
                FileEntry e = l.item(Ops.toInt32(args[0]));
                return e == null ? Js.NULL : FormBindings.fileObject(bridge, e);
            }
        }), true, true, true);
    }

    /**
     * Installs the indexed and named access of <code>form[i]</code> and <code>form[name]</code>.
     * <p>
     * <code>form[i]</code>, <code>form[name]</code> 의 위치와 이름 접근을 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void installFormIndexed(DomBridge bridge) {
        bridge.setIndexed("HTMLFormElement", new IndexedObject.Source() {
            /**
             * Returns the number of controls.
             * <p>
             * 컨트롤 수를 반환한다.
             *
             * @param t the form / 폼
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((HtmlFormElement) t).getLength();
            }

            /**
             * Returns the control at an index of <code>elements</code>.
             * <p>
             * <code>elements</code> 의 위치의 컨트롤을 반환한다.
             *
             * @param t the form / 폼
             * @param i the index / 위치
             * @return the control / 컨트롤
             */
            public Object item(Object t, int i) {
                return ((HtmlFormElement) t).getElements().item(i);
            }

            /**
             * Returns the control or <code>img</code> element with a name, or a list of them.
             * <p>
             * 이름이 같은 컨트롤이나 <code>img</code> 요소, 또는 그 목록을 반환한다.
             *
             * @param t the form / 폼
             * @param n the name / 이름
             * @return the object or null / 객체 또는 null
             */
            public Object named(Object t, String n) {
                return ((HtmlFormElement) t).getNamedProperty(n);
            }
        });
    }

    /**
     * Installs the indexed access of <code>select[i]</code> and the named access of its options.
     * <p>
     * <code>select[i]</code> 의 위치 접근과 option 의 이름 접근을 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    static void installSelectIndexed(DomBridge bridge) {
        bridge.setIndexed("HTMLSelectElement", new IndexedObject.Source() {
            /**
             * Returns the number of options.
             * <p>
             * option 수를 반환한다.
             *
             * @param t the select / select
             * @return the length / 길이
             */
            public int length(Object t) {
                return ((HtmlSelectElement) t).getLength();
            }

            /**
             * Returns the option at an index.
             * <p>
             * 위치의 option 을 반환한다.
             *
             * @param t the select / select
             * @param i the index / 위치
             * @return the option / option
             */
            public Object item(Object t, int i) {
                return ((HtmlSelectElement) t).item(i);
            }

            /**
             * Returns the option with an id or name.
             * <p>
             * id 나 name 이 같은 option 을 반환한다.
             *
             * @param t the select / select
             * @param n the name / 이름
             * @return the option or null / option 또는 null
             */
            public Object named(Object t, String n) {
                Element e = ((HtmlSelectElement) t).namedItem(n);
                return e;
            }
        });
    }

    /**
     * The index and name access of the {@link FormElementList} based collections.
     * <p>
     * {@link FormElementList} 기반 컬렉션의 위치와 이름 접근.
     */
    private static final class ListSource implements IndexedObject.Source {

        /**
         * Creates the source.
         * <p>
         * 소스를 만든다.
         */
        ListSource() {
        }

        /**
         * Returns the number of elements.
         * <p>
         * 요소 수를 반환한다.
         *
         * @param t the collection / 컬렉션
         * @return the length / 길이
         */
        public int length(Object t) {
            return ((FormElementList) t).getLength();
        }

        /**
         * Returns the element at an index.
         * <p>
         * 위치의 요소를 반환한다.
         *
         * @param t the collection / 컬렉션
         * @param i the index / 위치
         * @return the element / 요소
         */
        public Object item(Object t, int i) {
            return ((FormElementList) t).item(i);
        }

        /**
         * Returns the element (or a list of them) with an id or name.
         * <p>
         * id 나 name 이 같은 요소(또는 그 목록)를 반환한다.
         *
         * @param t the collection / 컬렉션
         * @param n the name / 이름
         * @return the object or null / 객체 또는 null
         */
        public Object named(Object t, String n) {
            return ((FormElementList) t).namedItem(n);
        }
    }
}
