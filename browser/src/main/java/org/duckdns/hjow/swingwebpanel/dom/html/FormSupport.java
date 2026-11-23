package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The per-document support of the HTML form controls. A document gets one on first use ({@link Document#getFormSupport()}). It holds
 * the {@link FormHost}, a state version that the live lists use to notice changes that are not DOM changes, and it observes the child list
 * changes of the document for the things that the HTML Standard defines as reactions to insertion and removal: the selectedness of the options of a
 * <code>select</code>, the radio button group of an inserted checked radio button and the form owner that the parser assigned. The observation
 * starts when the first control element of the document is created, so a document without forms pays nothing.
 * <p>
 * 문서별 HTML 폼 컨트롤 지원. 문서는 처음 쓸 때({@link Document#getFormSupport()}) 하나를 얻는다. {@link FormHost}, DOM 변경이 아닌 변경을 라이브 목록이
 * 알아채는 데 쓰는 상태 버전을 가지며, HTML 표준이 삽입과 제거에 대한 반응으로 정의한 것들을 위해 문서의 자식 목록 변경을 관찰한다: <code>select</code> 의
 * option 의 selectedness, 삽입된 체크된 라디오 버튼의 라디오 그룹, 파서가 지정한 폼 소유자. 관찰은 문서의 첫 컨트롤 요소가 만들어질 때 시작하므로 폼이 없는
 * 문서는 비용이 없다.
 */
public final class FormSupport implements DomMutationListener {

    /** The document. / 문서. */
    private final Document document;

    /** The host or null. / 호스트 또는 null. */
    private FormHost host;

    /** True once the tree observation was installed. / 트리 관찰을 설치했으면 true. */
    private boolean observing;

    /** Grows with every state change of a control. / 컨트롤의 상태가 바뀔 때마다 커진다. */
    private long stateVersion;

    /** The event that is being dispatched by {@link #dispatchPlain(Element, Event, boolean)} (so that its activation is not run twice), or null. / {@link #dispatchPlain(Element, Event, boolean)} 이 전달 중인 이벤트(활성화가 두 번 실행되지 않게 한다). 없으면 null. */
    private Event plainEvent;

    /**
     * Creates the support of a document; call {@link Document#getFormSupport()} instead.
     * <p>
     * 문서의 지원 객체를 만든다. 대신 {@link Document#getFormSupport()} 를 부른다.
     *
     * @param document the document / 문서
     */
    public FormSupport(Document document) {
        this.document = document;
    }

    /**
     * Returns the document.
     * <p>
     * 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the host.
     * <p>
     * 호스트를 반환한다.
     *
     * @return the host or null / 호스트 또는 null
     */
    public FormHost getHost() {
        return host;
    }

    /**
     * Installs the host.
     * <p>
     * 호스트를 설치한다.
     *
     * @param host the host or null / 호스트 또는 null
     */
    public void setHost(FormHost host) {
        this.host = host;
    }

    /**
     * Returns the state version: it grows whenever the state of a control changed. Live lists compare it together with the mutation
     * version of the document.
     * <p>
     * 상태 버전을 반환한다: 컨트롤의 상태가 바뀔 때마다 커진다. 라이브 목록은 문서의 변경 버전과 함께 이것을 비교한다.
     *
     * @return the version / 버전
     */
    public long getStateVersion() {
        return stateVersion;
    }

    /**
     * Called when a control element of the document was created: starts the tree observation if it is not running.
     * <p>
     * 문서의 컨트롤 요소가 만들어졌을 때 호출된다. 트리 관찰이 돌고 있지 않으면 시작한다.
     */
    void controlCreated() {
        if (!observing) {
            observing = true;
            document.addMutationListener(this);
        }
    }

    /**
     * Records that the state of an element changed and tells the host.
     * <p>
     * 요소의 상태가 바뀌었음을 기록하고 호스트에 알린다.
     *
     * @param element the element / 요소
     */
    void stateChanged(Element element) {
        stateVersion++;
        FormHost h = host;
        if (h != null) {
            try {
                h.stateChanged(element);
            } catch (RuntimeException e) {
                // 호스트의 오류가 DOM 연산을 깨뜨리지 않게 한다.
                System.err.println("FormHost.stateChanged failed: " + e);
            }
        }
    }

    /**
     * Reacts to a child list change (see the class description).
     * <p>
     * 자식 목록 변경에 반응한다(클래스 설명 참조).
     *
     * @param record    the change / 변경
     * @param connected whether the target is connected / 대상이 연결되어 있는지
     */
    public void mutated(MutationRecord record, boolean connected) {
        if (record.getType() != MutationRecord.CHILD_LIST) {
            return;
        }
        Node target = record.getTarget();
        List<Node> added = record.getAddedNodes();
        List<Node> removed = record.getRemovedNodes();
        HtmlSelectElement select = HtmlSelectElement.containerOwner(target);
        if (select != null) {
            select.optionsChanged(added);
        }
        // 추가된 하위 트리의 컨트롤에 삽입을 알린다.
        for (int i = 0; i < added.size(); i++) {
            walk(added.get(i), true);
        }
        // 제거된 하위 트리의 컨트롤에 제거를 알린다.
        for (int i = 0; i < removed.size(); i++) {
            walk(removed.get(i), false);
        }
    }

    /**
     * Calls the insertion or removal hook of every control element of a subtree.
     * <p>
     * 하위 트리의 모든 컨트롤 요소의 삽입 또는 제거 훅을 호출한다.
     *
     * @param root     subtree root / 하위 트리 루트
     * @param inserted true for an insertion, false for a removal / 삽입이면 true, 제거이면 false
     */
    private void walk(Node root, boolean inserted) {
        if (!(root instanceof Element)) {
            return;
        }
        Node n = root;
        // 하위 트리를 앞에서부터 순회하며 컨트롤 요소의 훅을 부른다.
        while (n != null) {
            if (n instanceof HtmlControl) {
                if (inserted) {
                    ((HtmlControl) n).insertedIntoTree();
                } else {
                    ((HtmlControl) n).removedFromTree();
                }
            }
            n = Node.nextInTree(n, root);
        }
    }

    /**
     * Dispatches an event at an element with the plain dispatch algorithm (no activation behavior): the call that {@link FormActivation} makes after it
     * ran the legacy-pre-activation behavior.
     * <p>
     * 이벤트를 요소에 일반 전달 알고리즘(활성화 동작 없음)으로 전달한다. {@link FormActivation} 이 legacy-pre-activation 동작을 실행한 뒤 하는 호출이다.
     *
     * @param target  the target / 대상
     * @param event   the event / 이벤트
     * @param trusted true for an event of the user agent / 사용자 에이전트의 이벤트이면 true
     * @return false if the event was cancelled / 이벤트가 취소되었으면 false
     */
    static boolean dispatchPlain(Element target, Event event, boolean trusted) {
        FormSupport s = target.document().getFormSupport();
        Event previous = s.plainEvent;
        s.plainEvent = event;
        try {
            return trusted ? target.dispatchTrustedEvent(event) : target.dispatchEvent(event);
        } finally {
            s.plainEvent = previous;
        }
    }

    /**
     * Tells whether an event is being dispatched by {@link #dispatchPlain(Element, Event, boolean)} (so the activation behavior must not run again).
     * <p>
     * 이벤트가 {@link #dispatchPlain(Element, Event, boolean)} 로 전달 중인지(활성화 동작을 다시 실행하지 말아야 하는지) 알려 준다.
     *
     * @param target the element that dispatches / 전달하는 요소
     * @param event  the event / 이벤트
     * @return true if it is a plain dispatch / 일반 전달이면 true
     */
    static boolean isPlain(Element target, Event event) {
        return target.document().getFormSupport().plainEvent == event;
    }
}
