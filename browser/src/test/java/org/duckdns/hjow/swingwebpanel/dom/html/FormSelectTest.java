package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>select</code>, <code>option</code> and <code>optgroup</code> elements (plan item P11-05, HTML Standard 4.10.7 to 4.10.10): the
 * selectedness setting algorithm, the dirtiness of options, <code>selectedIndex</code>, <code>value</code>, the live <code>options</code> and
 * <code>selectedOptions</code> collections, the text, value and label of an option, disabled options and the reset algorithm.
 * <p>
 * <code>select</code>, <code>option</code>, <code>optgroup</code> 요소의 시험(계획 P11-05, HTML 표준 4.10.7~4.10.10): selectedness setting algorithm, option 의 dirtiness,
 * <code>selectedIndex</code>, <code>value</code>, 라이브 <code>options</code> 와 <code>selectedOptions</code> 컬렉션, option 의 text/value/label, 비활성 option, 초기화 알고리즘.
 */
public class FormSelectTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormSelectTest() {
    }

    /**
     * The selectedness of a single-choice select with display size 1: the first option that is not disabled is selected when none is; the last option with
     * the <code>selected</code> attribute wins.
     * <p>
     * 표시 크기 1 인 단일 선택 select 의 selectedness: 선택된 것이 없으면 비활성이 아닌 첫 option 이 선택되고, <code>selected</code> 속성이 있는 마지막 option 이 이긴다.
     */
    public void testDefaultSelection() {
        Document d = FormTestSupport.page("<select id=a><option id=a1>one</option><option id=a2>two</option></select>"
                + "<select id=b><option id=b1 disabled>one</option><option id=b2>two</option></select>"
                + "<select id=c><option id=c1 selected>one</option><option id=c2 selected>two</option><option id=c3>three</option></select>"
                + "<select id=e></select><select id=f size=2><option>x</option></select><select id=g multiple><option>x</option></select>"
                + "<select id=h><option id=h1 disabled>x</option></select>");
        Check.equal(0, FormTestSupport.select(d, "a").getSelectedIndex(), "the first option of a single select is selected");
        Check.isTrue(FormTestSupport.option(d, "a1").isSelected(), "a1 is selected");
        Check.isFalse(FormTestSupport.option(d, "a2").isSelected(), "a2 is not");
        Check.equal(1, FormTestSupport.select(d, "b").getSelectedIndex(), "disabled options are skipped");
        Check.equal(1, FormTestSupport.select(d, "c").getSelectedIndex(), "the last option with selected wins");
        Check.isFalse(FormTestSupport.option(d, "c1").isSelected(), "c1 was deselected");
        Check.isTrue(FormTestSupport.option(d, "c1").isDefaultSelected(), "but its default selectedness (the attribute) stays");
        Check.equal(-1, FormTestSupport.select(d, "e").getSelectedIndex(), "no options");
        Check.equal(-1, FormTestSupport.select(d, "f").getSelectedIndex(), "display size 2: nothing is selected automatically");
        Check.equal(-1, FormTestSupport.select(d, "g").getSelectedIndex(), "multiple: nothing is selected automatically");
        Check.equal(-1, FormTestSupport.select(d, "h").getSelectedIndex(), "the only option is disabled");
        Check.equal("select-one", FormTestSupport.select(d, "a").getType(), "type");
        Check.equal("select-multiple", FormTestSupport.select(d, "g").getType(), "multiple type");
        Check.equal(1, FormTestSupport.select(d, "a").getDisplaySize(), "display size");
        Check.equal(2, FormTestSupport.select(d, "f").getDisplaySize(), "size attribute");
        Check.equal(4, FormTestSupport.select(d, "g").getDisplaySize(), "multiple default display size");
    }

    /**
     * <code>selectedIndex</code>, <code>value</code> and the option properties.
     * <p>
     * <code>selectedIndex</code>, <code>value</code> 와 option 의 속성.
     */
    public void testSelectedIndexAndValue() {
        Document d = FormTestSupport.page("<select id=s><option id=o1 value=a>Alpha</option><option id=o2 value=b>Beta</option><option id=o3>  Gamma   delta  </option></select>");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        Check.equal("a", s.getValue(), "value of the selected option");
        s.setSelectedIndex(1);
        Check.equal("b", s.getValue(), "selectedIndex changes the value");
        Check.isTrue(FormTestSupport.option(d, "o2").isSelected(), "o2 selected");
        Check.isFalse(FormTestSupport.option(d, "o1").isSelected(), "o1 deselected");
        s.setSelectedIndex(2);
        Check.equal("Gamma delta", s.getValue(), "an option without value attribute: its text, stripped and collapsed");
        s.setValue("a");
        Check.equal(0, s.getSelectedIndex(), "setting the value selects the first option with that value");
        s.setValue("nothing");
        Check.equal(-1, s.getSelectedIndex(), "no option has this value: nothing is selected");
        Check.equal("", s.getValue(), "empty value");
        s.setSelectedIndex(7);
        Check.equal(-1, s.getSelectedIndex(), "an index out of range selects nothing");
        s.setSelectedIndex(-1);
        Check.equal(-1, s.getSelectedIndex(), "-1 selects nothing and the selectedness algorithm does not revive the first option");
        Check.equal("Beta", FormTestSupport.option(d, "o2").getText(), "text");
        Check.equal("Beta", FormTestSupport.option(d, "o2").getLabel(), "label defaults to the text");
        FormTestSupport.option(d, "o2").setLabel("B");
        Check.equal("B", FormTestSupport.option(d, "o2").getLabel(), "label attribute");
        Check.equal(2, FormTestSupport.option(d, "o3").getIndex(), "index");
        Check.equal(3, s.getLength(), "length");
        Check.same(d.getElementById("o2"), s.item(1), "item");
        Check.same(d.getElementById("o2"), s.namedItem("o2"), "namedItem by id");
    }

    /**
     * The dirtiness of an option: after its <code>selected</code> IDL attribute was set, the <code>selected</code> content attribute does not change
     * its selectedness any more.
     * <p>
     * option 의 dirtiness: <code>selected</code> IDL 속성을 지정한 뒤에는 <code>selected</code> 콘텐츠 속성이 selectedness 를 바꾸지 못한다.
     */
    public void testOptionDirtiness() {
        Document d = FormTestSupport.page("<form id=f><select id=s multiple><option id=o1>a</option><option id=o2>b</option></select></form>");
        HtmlOptionElement o1 = FormTestSupport.option(d, "o1");
        HtmlOptionElement o2 = FormTestSupport.option(d, "o2");
        o1.setAttribute("selected", "");
        Check.isTrue(o1.isSelected(), "a clean option follows the attribute");
        o1.removeAttribute("selected");
        Check.isFalse(o1.isSelected(), "and follows its removal");
        o1.setSelected(true);
        o1.removeAttribute("selected");
        Check.isTrue(o1.isSelected(), "a dirty option ignores the attribute");
        o2.setSelected(true);
        Check.isTrue(o1.isSelected() && o2.isSelected(), "multiple: both are selected");
        FormTestSupport.form(d, "f").reset();
        Check.isFalse(o1.isSelected() || o2.isSelected(), "reset restores the selected attributes (none)");
        o2.setDefaultSelected(true);
        Check.isTrue(o2.isSelected(), "the attribute was added after the reset cleared the dirtiness");
        Check.isTrue(o2.isDefaultSelected(), "defaultSelected");
    }

    /**
     * Selecting an option of a single-choice select deselects the others; a multiple select keeps several; removing the multiple attribute keeps only one.
     * <p>
     * 단일 선택 select 에서 option 을 선택하면 나머지가 해제된다. 다중 선택 select 는 여러 개를 유지한다. multiple 속성을 없애면 하나만 남는다.
     */
    public void testSingleAndMultiple() {
        Document d = FormTestSupport.page("<select id=s><option id=o1>a</option><option id=o2>b</option><option id=o3>c</option></select>");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        FormTestSupport.option(d, "o3").setSelected(true);
        Check.equal(2, s.getSelectedIndex(), "o3 selected");
        Check.isFalse(FormTestSupport.option(d, "o1").isSelected(), "o1 was deselected");
        s.setAttribute("multiple", "");
        FormTestSupport.option(d, "o1").setSelected(true);
        Check.equal(2, s.getSelectedOptions().getLength(), "two options are selected in a multiple select");
        Check.same(d.getElementById("o1"), s.getSelectedOptions().item(0), "selectedOptions is in tree order");
        Check.equal(0, s.getSelectedIndex(), "selectedIndex is the first selected option");
        s.removeAttribute("multiple");
        Check.equal(1, s.getSelectedOptions().getLength(), "removing multiple keeps only the first selected option");
        Check.equal(0, s.getSelectedIndex(), "the first");
        s.setSelectedIndex(-1);
        s.setAttribute("multiple", "");
        s.removeAttribute("multiple");
        Check.equal(1, s.getSelectedOptions().getLength(), "back to single: the selectedness setting algorithm selects the first enabled option");
    }

    /**
     * Inserting and removing options runs the selectedness setting algorithm again; <code>options</code> is live; <code>add</code>, <code>remove</code>
     * and <code>length</code>.
     * <p>
     * option 을 삽입/제거하면 selectedness setting algorithm 이 다시 실행된다. <code>options</code> 는 라이브이다. <code>add</code>, <code>remove</code>, <code>length</code>.
     */
    public void testOptionsAreLive() {
        Document d = FormTestSupport.page("<select id=s><option id=o1>a</option><option id=o2>b</option></select>");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        HtmlOptionsCollection opts = s.getOptions();
        Check.equal(2, opts.getLength(), "two options");
        Check.same(opts, s.getOptions(), "the same collection every time");
        FormTestSupport.option(d, "o2").setSelected(true);
        Check.equal(1, s.getSelectedIndex(), "o2 selected");
        s.removeChild(d.getElementById("o2"));
        Check.equal(1, opts.getLength(), "the collection shrank");
        Check.equal(0, s.getSelectedIndex(), "removing the selected option selects the first enabled one again");
        Element n = d.createElement("option");
        n.setAttribute("selected", "");
        n.setAttribute("id", "n");
        s.add(n, d.getElementById("o1"));
        Check.same(n, opts.item(0), "add before an option");
        Check.equal(0, s.getSelectedIndex(), "the inserted selected option wins in a single select");
        Check.isFalse(FormTestSupport.option(d, "o1").isSelected(), "the old selection was deselected");
        s.addBeforeIndex(d.createElement("option"), 1);
        Check.equal(3, s.getLength(), "added at index 1");
        s.removeOption(0);
        Check.equal(2, s.getLength(), "removed the first");
        s.setLength(5);
        Check.equal(5, s.getLength(), "length grows with empty options");
        s.setLength(1);
        Check.equal(1, s.getLength(), "and shrinks");
        Element g = d.createElement("optgroup");
        Element og = d.createElement("option");
        g.appendChild(og);
        s.appendChild(g);
        Check.equal(2, s.getLength(), "options in an optgroup count");
        Check.same(og, opts.item(1), "in tree order");
    }

    /**
     * Disabled options and optgroups: a disabled option is skipped by the default selection and not submitted; an optgroup disables its options.
     * <p>
     * 비활성 option 과 optgroup: 비활성 option 은 기본 선택에서 건너뛰고 제출되지 않는다. optgroup 이 자식 option 을 비활성으로 만든다.
     */
    public void testDisabledOptions() {
        Document d = FormTestSupport.page("<select id=s><optgroup id=g disabled label=G><option id=o1>a</option></optgroup><option id=o2>b</option></select>");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        Check.isTrue(FormTestSupport.option(d, "o1").isDisabled(), "disabled by the optgroup");
        Check.isFalse(FormTestSupport.option(d, "o2").isDisabled(), "o2 is enabled");
        Check.equal(1, s.getSelectedIndex(), "the default selection skips the disabled option");
        d.getElementById("g").removeAttribute("disabled");
        Check.isFalse(FormTestSupport.option(d, "o1").isDisabled(), "enabled again");
        FormTestSupport.option(d, "o2").setAttribute("disabled", "");
        s.setSelectedIndex(-1);
        FormTestSupport.option(d, "o1").setSelected(true);
        Check.equal(0, s.getSelectedIndex(), "an option can be selected by script even when disabled");
    }

    /**
     * <code>select</code> reset: the selectedness comes from the <code>selected</code> attributes again.
     * <p>
     * <code>select</code> 초기화: selectedness 가 <code>selected</code> 속성에서 다시 정해진다.
     */
    public void testSelectReset() {
        Document d = FormTestSupport.page("<form id=f><select id=s><option>a</option><option selected>b</option><option>c</option></select>"
                + "<select id=m multiple><option selected>a</option><option>b</option><option selected>c</option></select></form>");
        HtmlSelectElement s = FormTestSupport.select(d, "s");
        HtmlSelectElement m = FormTestSupport.select(d, "m");
        Check.equal(1, s.getSelectedIndex(), "initial");
        s.setSelectedIndex(2);
        m.setSelectedIndex(1);
        Check.equal(1, m.getSelectedOptions().getLength(), "only b after the script change");
        FormTestSupport.form(d, "f").reset();
        Check.equal(1, s.getSelectedIndex(), "single select reset");
        Check.equal(2, m.getSelectedOptions().getLength(), "multiple select reset: a and c");
        Check.equal(0, m.getSelectedIndex(), "first selected");
    }

    /**
     * An option inside a select knows its form through the select; an option outside a select is not selected by default.
     * <p>
     * select 안의 option 은 select 를 통해 폼을 안다. select 밖의 option 은 기본으로 선택되지 않는다.
     */
    public void testOptionForm() {
        Document d = FormTestSupport.page("<form id=f><select><option id=o>a</option></select></form><datalist id=dl><option id=d1 value=x></option></datalist>");
        Check.same(d.getElementById("f"), FormTestSupport.option(d, "o").getForm(), "form through the select");
        Check.isNull(FormTestSupport.option(d, "d1").getForm(), "an option in a datalist has no form");
        Check.equal(1, ((HtmlDataListElement) d.getElementById("dl")).getOptions().getLength(), "datalist.options");
    }
}
