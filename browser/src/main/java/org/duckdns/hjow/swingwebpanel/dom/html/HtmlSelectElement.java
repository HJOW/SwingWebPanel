package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>select</code> element (HTML Standard 4.10.7): a choice of one option (or of several with <code>multiple</code>). The options and
 * their selectedness are the state of the control; this class implements the "selectedness setting algorithm" (a single-choice select with a
 * display size of 1 always has one selected option unless all are disabled, and never more than one), the <code>value</code>,
 * <code>selectedIndex</code>, <code>options</code> and <code>selectedOptions</code> APIs, the reset algorithm and the
 * constraint validation of a required select. The rules are applied lazily: a change of the options only marks them pending and the next
 * read applies them, so that building a select with thousands of options does not cost quadratic time.
 * <p>
 * <code>select</code> 요소(HTML 표준 4.10.7): option 하나(<code>multiple</code> 이면 여러 개)의 선택. option 과 그 selectedness 가 컨트롤의 상태이며, 이 클래스는
 * "selectedness setting algorithm"(표시 크기 1 인 단일 선택 select 는 모두 비활성이 아닌 한 항상 하나가 선택되고 둘 이상은 선택되지 않는다),
 * <code>value</code>, <code>selectedIndex</code>, <code>options</code>, <code>selectedOptions</code> API, 초기화 알고리즘, 필수 select 의 제약 검증을 구현한다.
 * 규칙은 게으르게 적용한다: option 이 바뀌면 보류 표시만 하고 다음 읽기에서 적용하므로 수천 개의 option 으로 select 를 만들어도 제곱 시간이 들지 않는다.
 */
public final class HtmlSelectElement extends HtmlListedElement {

    /** True if the selectedness rules must run before the state is read. / 상태를 읽기 전에 selectedness 규칙을 실행해야 하면 true. */
    private boolean pending;

    /** The cached options collection. / 캐시한 options 컬렉션. */
    private HtmlOptionsCollection options;

    /** The cached selected options collection. / 캐시한 selectedOptions 컬렉션. */
    private FormElementList selected;

    /**
     * Creates a select.
     * <p>
     * select 를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlSelectElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * A select is labelable.
     * <p>
     * select 는 라벨을 붙일 수 있다.
     *
     * @return true / true
     */
    public boolean isLabelable() {
        return true;
    }

    /**
     * A select takes part in form submission.
     * <p>
     * select 는 폼 제출에 참여한다.
     *
     * @return true / true
     */
    boolean isSubmittable() {
        return true;
    }

    /**
     * A select is resettable.
     * <p>
     * select 는 초기화할 수 있다.
     *
     * @return true / true
     */
    boolean isResettable() {
        return true;
    }

    /**
     * A select takes part in the default focus order.
     * <p>
     * select 는 기본 포커스 순서에 참여한다.
     *
     * @return 0 / 0
     */
    int defaultTabIndex() {
        return 0;
    }

    /**
     * Finds the select that owns the options inside a node: the node itself if it is a select, or its parent if it is an
     * <code>optgroup</code> that is a child of a select.
     * <p>
     * 노드 안의 option 을 가진 select 를 찾는다: 노드가 select 이면 그것, <code>optgroup</code> 이고 그 부모가 select 이면 부모.
     *
     * @param container a node whose children changed / 자식이 바뀐 노드
     * @return the select or null / select 또는 null
     */
    static HtmlSelectElement containerOwner(Node container) {
        if (container instanceof HtmlSelectElement) {
            return (HtmlSelectElement) container;
        }
        if (container instanceof HtmlOptGroupElement && container.getParentNode() instanceof HtmlSelectElement) {
            return (HtmlSelectElement) container.getParentNode();
        }
        return null;
    }

    /**
     * Reacts to the attributes that change the selection rules: <code>multiple</code> and <code>size</code>.
     * <p>
     * 선택 규칙을 바꾸는 속성 <code>multiple</code> 과 <code>size</code> 에 반응한다.
     *
     * @param local     attribute local name / 속성 로컬 이름
     * @param namespace attribute namespace / 속성 네임스페이스
     * @param oldValue  previous value or null / 이전 값 또는 null
     * @param newValue  new value or null / 새 값 또는 null
     */
    protected void attributeChanged(String local, String namespace, String oldValue, String newValue) {
        if (namespace != null) {
            return;
        }
        if ("multiple".equals(local) && newValue == null && oldValue != null) {
            // multiple 을 없애면 선택된 option 은 첫 번째 하나만 남긴다.
            boolean seen = false;
            List<HtmlOptionElement> list = optionList();
            // option 을 앞에서부터 보며 두 번째 이후 선택을 푼다.
            for (int i = 0; i < list.size(); i++) {
                HtmlOptionElement o = list.get(i);
                if (o.rawSelected()) {
                    if (seen) {
                        o.setRawSelected(false);
                    }
                    seen = true;
                }
            }
            pending = true;
            stateChanged();
        } else if ("multiple".equals(local) || "size".equals(local)) {
            pending = true;
            stateChanged();
        }
    }

    /**
     * Marks the selectedness rules as pending (an option became enabled or disabled).
     * <p>
     * selectedness 규칙을 보류 상태로 표시한다(option 이 활성/비활성이 되었다).
     */
    void selectednessMayChange() {
        pending = true;
        stateChanged();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // option 목록과 selectedness 규칙
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the list of options in tree order: the <code>option</code> children and the <code>option</code> children of the
     * <code>optgroup</code> children.
     * <p>
     * option 목록을 트리 순서로 반환한다: <code>option</code> 자식과 <code>optgroup</code> 자식의 <code>option</code> 자식.
     *
     * @return the options (a new list) / option 들(새 목록)
     */
    List<HtmlOptionElement> optionList() {
        List<HtmlOptionElement> out = new ArrayList<HtmlOptionElement>();
        // 자식을 차례로 보며 option 과 optgroup 안의 option 을 모은다.
        for (Node c = getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof HtmlOptionElement) {
                out.add((HtmlOptionElement) c);
            } else if (c instanceof HtmlOptGroupElement) {
                // optgroup 의 option 자식을 모은다.
                for (Node g = c.getFirstChild(); g != null; g = g.getNextSibling()) {
                    if (g instanceof HtmlOptionElement) {
                        out.add((HtmlOptionElement) g);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Returns the options in tree order after the selectedness setting algorithm has run (so that {@link HtmlOptionElement#isSelected()} is
     * consistent): for the renderer and the host that show or edit the list.
     * <p>
     * option 을 트리 순서로 반환한다. 선택 상태 설정 알고리즘을 먼저 실행하므로 {@link HtmlOptionElement#isSelected()} 가 일관된다. 목록을 보여 주거나
     * 편집하는 렌더러와 호스트가 쓴다.
     *
     * @return the options (a new list) / option 들(새 목록)
     */
    public List<HtmlOptionElement> getOptionElements() {
        ensureConsistent();
        return optionList();
    }

    /**
     * Tells whether the <code>multiple</code> attribute is present.
     * <p>
     * <code>multiple</code> 속성이 있는지 알려 준다.
     *
     * @return true for a multiple-choice select / 다중 선택 select 이면 true
     */
    public boolean isMultiple() {
        return hasAttribute("multiple");
    }

    /**
     * Returns the display size: the <code>size</code> attribute if it is a valid positive number, otherwise 4 for a multiple-choice select and 1
     * for a single-choice select.
     * <p>
     * 표시 크기를 반환한다: <code>size</code> 속성이 올바른 양수이면 그 값, 아니면 다중 선택 select 는 4, 단일 선택 select 는 1.
     *
     * @return the display size / 표시 크기
     */
    public int getDisplaySize() {
        int s = HtmlText.parseNonNegative(getAttribute("size"));
        return s > 0 ? s : (isMultiple() ? 4 : 1);
    }

    /**
     * Runs the selectedness setting algorithm if it is pending.
     * <p>
     * 보류 중이면 selectedness setting algorithm 을 실행한다.
     */
    void ensureConsistent() {
        if (pending) {
            pending = false;
            runSelectednessSetting();
        }
    }

    /**
     * The selectedness setting algorithm: in a single-choice select, only the last selected option stays selected, and when none is selected and
     * the display size is 1 the first option that is not disabled becomes selected.
     * <p>
     * selectedness setting algorithm: 단일 선택 select 에서는 마지막으로 선택된 option 만 선택된 채로 남고, 선택된 것이 없으며 표시 크기가 1 이면 비활성이 아닌 첫
     * option 이 선택된다.
     */
    private void runSelectednessSetting() {
        if (isMultiple()) {
            return;
        }
        HtmlOptionElement lastSelected = null;
        HtmlOptionElement firstEnabled = null;
        List<HtmlOptionElement> list = optionList();
        // option 을 앞에서부터 보며 선택된 것의 앞선 선택을 풀고 첫 번째 활성 option 을 기억한다.
        for (int i = 0; i < list.size(); i++) {
            HtmlOptionElement o = list.get(i);
            if (o.rawSelected()) {
                if (lastSelected != null) {
                    lastSelected.setRawSelected(false);
                }
                lastSelected = o;
            }
            if (firstEnabled == null && !o.isDisabled()) {
                firstEnabled = o;
            }
        }
        if (lastSelected == null && firstEnabled != null && getDisplaySize() == 1) {
            firstEnabled.setRawSelected(true);
        }
    }

    /**
     * Called when options were inserted or removed: if an inserted option is selected the other options are deselected (single choice), and the
     * rules become pending.
     * <p>
     * option 이 삽입되거나 제거되었을 때 호출된다: 삽입된 option 이 선택되어 있으면 다른 option 의 선택을 풀고(단일 선택) 규칙을 보류 상태로 둔다.
     *
     * @param added the nodes that were added to the select or to one of its optgroups / select 나 그 optgroup 에 추가된 노드
     */
    void optionsChanged(List<Node> added) {
        if (!isMultiple()) {
            HtmlOptionElement insertedSelected = null;
            // 추가된 노드 중 선택된 마지막 option 을 찾는다.
            for (int i = 0; i < added.size(); i++) {
                Node n = added.get(i);
                if (n instanceof HtmlOptionElement && ((HtmlOptionElement) n).rawSelected()) {
                    insertedSelected = (HtmlOptionElement) n;
                } else if (n instanceof HtmlOptGroupElement) {
                    // optgroup 안의 option 도 확인한다.
                    for (Node g = n.getFirstChild(); g != null; g = g.getNextSibling()) {
                        if (g instanceof HtmlOptionElement && ((HtmlOptionElement) g).rawSelected()) {
                            insertedSelected = (HtmlOptionElement) g;
                        }
                    }
                }
            }
            if (insertedSelected != null) {
                List<HtmlOptionElement> list = optionList();
                // 삽입된 선택 option 이 이기고 나머지는 선택을 푼다.
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) != insertedSelected) {
                        list.get(i).setRawSelected(false);
                    }
                }
            }
        }
        pending = true;
        stateChanged();
    }

    /**
     * Called when an option's selectedness was set: in a single-choice select the other options are deselected when the option is selected,
     * and the rules become pending.
     * <p>
     * option 의 selectedness 가 지정되었을 때 호출된다: 단일 선택 select 에서는 option 이 선택되면 다른 option 의 선택을 풀고 규칙을 보류 상태로 둔다.
     *
     * @param option the option / option
     */
    void optionSelectednessSet(HtmlOptionElement option) {
        if (!isMultiple() && option.rawSelected()) {
            List<HtmlOptionElement> list = optionList();
            // 선택된 option 이외의 option 의 선택을 푼다.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != option) {
                    list.get(i).setRawSelected(false);
                }
            }
        }
        pending = true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // IDL 속성과 메서드
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the <code>type</code> IDL attribute: <code>select-multiple</code> or <code>select-one</code>.
     * <p>
     * <code>type</code> IDL 속성을 반환한다: <code>select-multiple</code> 또는 <code>select-one</code>.
     *
     * @return the type / 종류
     */
    public String getType() {
        return isMultiple() ? "select-multiple" : "select-one";
    }

    /**
     * Returns the live <code>options</code> collection (the same object every time).
     * <p>
     * 라이브 <code>options</code> 컬렉션을 반환한다(매번 같은 객체).
     *
     * @return the collection / 컬렉션
     */
    public HtmlOptionsCollection getOptions() {
        if (options == null) {
            options = new HtmlOptionsCollection(this);
        }
        return options;
    }

    /**
     * Returns the live <code>selectedOptions</code> collection (the same object every time).
     * <p>
     * 라이브 <code>selectedOptions</code> 컬렉션을 반환한다(매번 같은 객체).
     *
     * @return the collection / 컬렉션
     */
    public FormElementList getSelectedOptions() {
        if (selected == null) {
            selected = new FormElementList(document()) {
                /**
                 * Collects the selected options.
                 * <p>
                 * 선택된 option 을 모은다.
                 *
                 * @return the options / option 들
                 */
                protected List<Element> compute() {
                    ensureConsistent();
                    List<Element> out = new ArrayList<Element>();
                    List<HtmlOptionElement> list = optionList();
                    // option 을 순서대로 보며 선택된 것만 모은다.
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).rawSelected()) {
                            out.add(list.get(i));
                        }
                    }
                    return out;
                }
            };
        }
        return selected;
    }

    /**
     * Returns the selected options as a list of options (a snapshot).
     * <p>
     * 선택된 option 을 option 목록(스냅샷)으로 반환한다.
     *
     * @return the selected options / 선택된 option
     */
    List<HtmlOptionElement> selectedOptionList() {
        ensureConsistent();
        List<HtmlOptionElement> out = new ArrayList<HtmlOptionElement>();
        List<HtmlOptionElement> list = optionList();
        // option 을 순서대로 보며 선택된 것만 모은다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).rawSelected()) {
                out.add(list.get(i));
            }
        }
        return out;
    }

    /**
     * Returns the <code>length</code> IDL attribute: the number of options.
     * <p>
     * <code>length</code> IDL 속성을 반환한다: option 의 수.
     *
     * @return the number of options / option 수
     */
    public int getLength() {
        return optionList().size();
    }

    /**
     * Sets <code>length</code>: a larger length appends empty <code>option</code> elements, a smaller one removes options from the end.
     * <p>
     * <code>length</code> 를 지정한다: 더 크면 빈 <code>option</code> 요소를 끝에 추가하고, 더 작으면 끝에서부터 option 을 제거한다.
     *
     * @param value the new length / 새 길이
     */
    public void setLength(int value) {
        List<HtmlOptionElement> list = optionList();
        int n = list.size();
        if (value > n) {
            // 모자란 만큼 빈 option 을 끝에 붙인다(상한을 두어 메모리 폭주를 막는다).
            int add = Math.min(value - n, 100000);
            for (int i = 0; i < add; i++) {
                appendChild(document().createElement("option"));
            }
        } else if (value < n) {
            // 끝에서부터 초과한 option 을 부모에서 제거한다.
            for (int i = n - 1; i >= value; i--) {
                list.get(i).remove();
            }
        }
    }

    /**
     * Returns the option at an index.
     * <p>
     * 위치의 option 을 반환한다.
     *
     * @param index the index / 위치
     * @return the option or null / option 또는 null
     */
    public Element item(int index) {
        List<HtmlOptionElement> list = optionList();
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    /**
     * Returns the first option whose id or <code>name</code> equals the name.
     * <p>
     * id 나 <code>name</code> 이 이름과 같은 첫 option 을 반환한다.
     *
     * @param name the name / 이름
     * @return the option or null / option 또는 null
     */
    public Element namedItem(String name) {
        Object o = getOptions().namedItem(name);
        return o instanceof Element ? (Element) o : null;
    }

    /**
     * Implements <code>add(element, before)</code>.
     * <p>
     * <code>add(element, before)</code> 를 구현한다.
     *
     * @param element an <code>option</code> or <code>optgroup</code> element / <code>option</code> 또는 <code>optgroup</code> 요소
     * @param before  an element of the options list that the new element goes before, or null / 새 요소가 앞에 놓일 option 목록의 요소. 없으면 null
     * @throws DomException HierarchyRequestError or NotFoundError / HierarchyRequestError 또는 NotFoundError
     */
    public void add(Element element, Element before) {
        if (element == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        if (element.contains(this)) {
            throw DomException.hierarchyRequest("the element cannot be added inside itself");
        }
        if (before != null && !contains(before)) {
            throw DomException.notFound("the reference element is not a descendant of the select");
        }
        if (element == before) {
            return;
        }
        Node parent = before != null ? before.getParentNode() : this;
        parent.insertBefore(element, before);
    }

    /**
     * Implements <code>add(element, index)</code>: the element goes before the option at the index, or at the end if there is none.
     * <p>
     * <code>add(element, index)</code> 를 구현한다: 요소를 위치의 option 앞에 넣고, 없으면 끝에 넣는다.
     *
     * @param element an <code>option</code> or <code>optgroup</code> element / <code>option</code> 또는 <code>optgroup</code> 요소
     * @param index   the index of the option to go before / 앞에 놓일 option 의 위치
     */
    public void addBeforeIndex(Element element, int index) {
        add(element, index < 0 ? null : item(index));
    }

    /**
     * Implements <code>remove(index)</code>: removes the option at the index (nothing happens if there is none).
     * <p>
     * <code>remove(index)</code> 를 구현한다: 위치의 option 을 제거한다(없으면 아무 일도 없다).
     *
     * @param index the index / 위치
     */
    public void removeOption(int index) {
        Element o = item(index);
        if (o != null) {
            o.remove();
        }
    }

    /**
     * Returns the <code>selectedIndex</code> IDL attribute: the index of the first selected option, or -1.
     * <p>
     * <code>selectedIndex</code> IDL 속성을 반환한다: 첫 선택 option 의 위치. 없으면 -1.
     *
     * @return the index / 위치
     */
    public int getSelectedIndex() {
        ensureConsistent();
        List<HtmlOptionElement> list = optionList();
        // option 을 앞에서부터 보며 처음 선택된 것의 위치를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).rawSelected()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets <code>selectedIndex</code>: only the option at the index is selected (all of them are deselected if the index is out of range).
     * <p>
     * <code>selectedIndex</code> 를 지정한다: 위치의 option 만 선택된다(범위를 벗어나면 모두 선택이 풀린다).
     *
     * @param index the index / 위치
     */
    public void setSelectedIndex(int index) {
        List<HtmlOptionElement> list = optionList();
        HtmlOptionElement match = null;
        // 모든 option 의 선택을 풀고 위치가 맞는 첫 option 을 기억한다.
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRawSelected(false);
            if (match == null && i == index) {
                match = list.get(i);
            }
        }
        if (match != null) {
            match.setRawSelected(true);
            match.setDirty(true);
        }
        // 선택을 직접 지정했으므로 보류 중인 규칙은 적용하지 않는다.
        pending = false;
        stateChanged();
    }

    /**
     * Returns the <code>value</code> IDL attribute: the value of the first selected option, or the empty string.
     * <p>
     * <code>value</code> IDL 속성을 반환한다: 첫 선택 option 의 값. 없으면 빈 문자열.
     *
     * @return the value / 값
     */
    public String getValue() {
        ensureConsistent();
        List<HtmlOptionElement> list = optionList();
        // option 을 앞에서부터 보며 처음 선택된 option 의 값을 반환한다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).rawSelected()) {
                return list.get(i).getValue();
            }
        }
        return "";
    }

    /**
     * Sets <code>value</code>: the first option with that value is selected and the others are deselected (none is selected if no option
     * has the value).
     * <p>
     * <code>value</code> 를 지정한다: 그 값을 가진 첫 option 이 선택되고 나머지는 선택이 풀린다(값이 같은 option 이 없으면 아무것도 선택되지 않는다).
     *
     * @param value the value / 값
     */
    public void setValue(String value) {
        List<HtmlOptionElement> list = optionList();
        HtmlOptionElement match = null;
        // 모든 option 의 선택을 풀고 값이 같은 첫 option 을 기억한다.
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRawSelected(false);
            if (match == null && list.get(i).getValue().equals(value)) {
                match = list.get(i);
            }
        }
        if (match != null) {
            match.setRawSelected(true);
            match.setDirty(true);
        }
        pending = false;
        stateChanged();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 초기화와 제약 검증
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * The reset algorithm: every option gets the selectedness of its <code>selected</code> attribute and loses its dirtiness, then the rules apply.
     * <p>
     * 초기화 알고리즘: 모든 option 이 <code>selected</code> 속성에 따른 selectedness 를 갖고 dirtiness 를 잃은 다음 규칙을 적용한다.
     */
    void resetControl() {
        List<HtmlOptionElement> list = optionList();
        // option 마다 selected 속성에 따라 selectedness 를 되돌린다.
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRawSelected(list.get(i).hasAttribute("selected"));
            list.get(i).setDirty(false);
        }
        pending = true;
        runSelectednessSettingNow();
        stateChanged();
    }

    /**
     * Applies the rules immediately.
     * <p>
     * 규칙을 즉시 적용한다.
     */
    private void runSelectednessSettingNow() {
        pending = false;
        runSelectednessSetting();
    }

    /**
     * Returns the placeholder label option: for a required select with a display size of 1, the first option if its value is empty and it is a
     * direct child of the select.
     * <p>
     * placeholder label option 을 반환한다: 표시 크기가 1 인 필수 select 에서, 값이 비어 있고 select 의 직접 자식인 첫 option.
     *
     * @return the option or null / option 또는 null
     */
    HtmlOptionElement placeholderLabelOption() {
        if (!hasAttribute("required") || getDisplaySize() != 1) {
            return null;
        }
        List<HtmlOptionElement> list = optionList();
        if (list.isEmpty()) {
            return null;
        }
        HtmlOptionElement first = list.get(0);
        return first.getParentNode() == this && first.getValue().length() == 0 ? first : null;
    }

    /**
     * Computes the violated constraints: <code>valueMissing</code> for a required select where no option is selected or only the placeholder label
     * option is.
     * <p>
     * 어긴 제약을 계산한다: 필수 select 에서 선택된 option 이 없거나 placeholder label option 만 선택되었으면 <code>valueMissing</code>.
     *
     * @return the flags / 플래그
     */
    int computeFlags() {
        if (!hasAttribute("required") || !isMutable()) {
            return 0;
        }
        List<HtmlOptionElement> sel = selectedOptionList();
        if (sel.isEmpty()) {
            return ValidityState.VALUE_MISSING;
        }
        HtmlOptionElement ph = placeholderLabelOption();
        return sel.size() == 1 && sel.get(0) == ph ? ValidityState.VALUE_MISSING : 0;
    }

    /**
     * Returns the message of a violated select constraint.
     * <p>
     * 어긴 select 제약의 메시지를 반환한다.
     *
     * @param f the flags / 플래그
     * @return the message / 메시지
     */
    String messageFor(int f) {
        if ((f & ValidityState.VALUE_MISSING) != 0) {
            return "Please select an item in the list.";
        }
        return super.messageFor(f);
    }
}
