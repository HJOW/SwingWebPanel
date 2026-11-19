package org.duckdns.hjow.swingwebpanel.html;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Parser metadata: template contents owned by each element, weak form owners and script flags ("parser document", "already
 * started", "force async"). The side tables contain no strong reference back to an element or its document.
 * <p>
 * Access template contents only on the document thread, like other DOM operations. The stored data only has a meaning for elements created by the parser or by
 * {@link HtmlMarkupSupport}.
 * <p>
 * HTML 파서의 부가 정보: 각 요소가 직접 소유하는 template 내용, 약한 폼 소유자와 스크립트 플래그. 보조 표에는 요소나 문서로 돌아가는 강한 참조가 없다.
 * <p>
 * template 내용은 다른 DOM 연산처럼 문서 스레드에서만 접근한다. 저장된 데이터는 파서나 {@link HtmlMarkupSupport} 가 만든 요소에서만 의미가 있다.
 */
public final class ParserNodeData {

    /**
     * Script flags of one <code>script</code> element.
     * <p>
     * <code>script</code> 요소 하나의 스크립트 플래그.
     */
    public static final class ScriptFlags {
        /**
         * True if the element was inserted by the parser ("parser document" is not null).
         * <p>
         * 요소가 파서에 의해 삽입되었으면(parser document 가 null 이 아니면) true.
         */
        private volatile boolean parserInserted;

        /**
         * True if the script has run or must never run ("already started").
         * <p>
         * 스크립트가 이미 실행되었거나 절대 실행되면 안 되면("already started") true.
         */
        private volatile boolean alreadyStarted;

        /**
         * True if the script behaves like an async one although it has no <code>async</code> attribute ("force async"). It is true for
         * a script that was created by a script (<code>document.createElement</code>), so that such a script does not block anything, and
         * false for a script that the parser created. Setting the <code>async</code> property of the script element clears it.
         * <p>
         * <code>async</code> 속성이 없어도 async 처럼 동작하면("force async") true. 스크립트가 만든 script(<code>document.createElement</code>)는
         * 기본이 true 여서 무엇도 막지 않고, 파서가 만든 script 는 false 이다. script 요소의 <code>async</code> 프로퍼티를 지정하면 지워진다.
         */
        private volatile boolean forceAsync = true;

        /**
         * Creates the flags with all values false.
         * <p>
         * 모든 값이 false 인 플래그를 만든다.
         */
        ScriptFlags() {
        }

        /**
         * Tells whether the parser inserted the element.
         * <p>
         * 파서가 요소를 삽입했는지 알려 준다.
         *
         * @return true if inserted by the parser / 파서가 삽입했으면 true
         */
        public boolean isParserInserted() {
            return parserInserted;
        }

        /**
         * Sets the parser-inserted flag.
         * <p>
         * parser-inserted 플래그를 지정한다.
         *
         * @param value new value / 새 값
         */
        public void setParserInserted(boolean value) {
            parserInserted = value;
        }

        /**
         * Tells whether the script has already started (so it must not run again).
         * <p>
         * 스크립트가 이미 시작되었는지(다시 실행하면 안 되는지) 알려 준다.
         *
         * @return the "already started" flag / "already started" 플래그
         */
        public boolean isAlreadyStarted() {
            return alreadyStarted;
        }

        /**
         * Sets the "already started" flag.
         * <p>
         * "already started" 플래그를 지정한다.
         *
         * @param value new value / 새 값
         */
        public void setAlreadyStarted(boolean value) {
            alreadyStarted = value;
        }

        /**
         * Tells whether "force async" is set.
         * <p>
         * "force async" 가 지정되었는지 알려 준다.
         *
         * @return the flag / 플래그
         */
        public boolean isForceAsync() {
            return forceAsync;
        }

        /**
         * Sets the "force async" flag.
         * <p>
         * "force async" 플래그를 지정한다.
         *
         * @param value new value / 새 값
         */
        public void setForceAsync(boolean value) {
            forceAsync = value;
        }
    }

    /**
     * Form-associated element to its form owner (weakly held).
     * <p>
     * 폼 연관 요소에서 폼 소유자로의 표(약하게 보관).
     */
    private static final Map<Element, WeakReference<Element>> FORM_OWNERS =
            Collections.synchronizedMap(new WeakHashMap<Element, WeakReference<Element>>());

    /**
     * Script element to its flags.
     * <p>
     * script 요소에서 플래그로의 표.
     */
    private static final Map<Element, ScriptFlags> SCRIPT_FLAGS = Collections.synchronizedMap(new WeakHashMap<Element, ScriptFlags>());

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ParserNodeData() {
    }

    /**
     * Returns the template contents of a <code>template</code> element.
     * <p>
     * <code>template</code> 요소의 template contents 를 반환한다.
     *
     * @param template element / 요소
     * @return the contents fragment, or null if the element has none / 내용 조각. 없으면 null
     */
    public static DocumentFragment getTemplateContents(Element template) {
        return template.getTemplateContents();
    }

    /**
     * Stores the template contents of a <code>template</code> element.
     * <p>
     * <code>template</code> 요소의 template contents 를 저장한다.
     *
     * @param template element / 요소
     * @param contents contents fragment / 내용 조각
     */
    public static void setTemplateContents(Element template, DocumentFragment contents) {
        template.setTemplateContents(contents);
    }

    /**
     * Returns the template contents of an element that is an HTML <code>template</code>, creating them if needed.
     * <p>
     * HTML <code>template</code> 인 요소의 template contents 를 반환한다. 필요하면 만든다.
     *
     * @param template an HTML template element / HTML template 요소
     * @return the contents / 내용
     */
    public static DocumentFragment ensureTemplateContents(Element template) {
        synchronized (template) {
            DocumentFragment f = template.getTemplateContents();
            if (f == null) {
                f = template.getOwnerDocument().createDocumentFragment();
                template.setTemplateContents(f);
            }
            return f;
        }
    }

    /**
     * Returns the form that the parser associated an element with.
     * <p>
     * 파서가 요소를 연결한 폼을 반환한다.
     *
     * @param element form-associated element / 폼 연관 요소
     * @return the form owner, or null / 폼 소유자 또는 null
     */
    public static Element getFormOwner(Element element) {
        WeakReference<Element> ref = FORM_OWNERS.get(element);
        return ref == null ? null : ref.get();
    }

    /**
     * Records the form owner of an element.
     * <p>
     * 요소의 폼 소유자를 기록한다.
     *
     * @param element form-associated element / 폼 연관 요소
     * @param form    form owner / 폼 소유자
     */
    public static void setFormOwner(Element element, Element form) {
        FORM_OWNERS.put(element, new WeakReference<Element>(form));
    }

    /**
     * Returns the script flags of a <code>script</code> element, creating them (all false) if needed.
     * <p>
     * <code>script</code> 요소의 스크립트 플래그를 반환한다. 필요하면 (모두 false 로) 만든다.
     *
     * @param script script element / script 요소
     * @return the flags / 플래그
     */
    public static ScriptFlags scriptFlags(Element script) {
        synchronized (SCRIPT_FLAGS) {
            ScriptFlags f = SCRIPT_FLAGS.get(script);
            if (f == null) {
                f = new ScriptFlags();
                SCRIPT_FLAGS.put(script, f);
            }
            return f;
        }
    }
}
