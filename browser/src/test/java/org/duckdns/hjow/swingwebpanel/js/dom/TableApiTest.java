package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the table object model (plan item P20-01, HTML5 2014 REC section 4.9): the parts of a table, the live collections of rows and
 * cells, <code>insertRow</code>, <code>deleteRow</code>, <code>insertCell</code>, <code>deleteCell</code> with their index rules, the position
 * attributes (<code>rowIndex</code>, <code>sectionRowIndex</code>, <code>cellIndex</code>) and the errors.
 * <p>
 * 표 객체 모델 시험(계획 P20-01, HTML5 2014 REC 4.9절): 표의 부분, 행과 셀의 라이브 컬렉션, 인덱스 규칙이 있는 <code>insertRow</code>,
 * <code>deleteRow</code>, <code>insertCell</code>, <code>deleteCell</code>, 위치 속성(<code>rowIndex</code>, <code>sectionRowIndex</code>,
 * <code>cellIndex</code>), 오류.
 */
public class TableApiTest {

    /**
     * The page: a table with a caption, a head, two bodies, a foot and a row that is a child of the table.
     * <p>
     * 페이지: caption, 머리, 본문 둘, 바닥, 표의 자식인 행이 있는 표.
     */
    private static final String PAGE = "<!doctype html><body><table id=t><caption id=c>cap</caption><colgroup><col></colgroup>"
            + "<thead id=h><tr id=r0><th>h1<th>h2</tr></thead><tbody id=b1><tr id=r1><td>a<td>b</tr><tr id=r2><td>c<td>d</tr></tbody>"
            + "<tbody id=b2><tr id=r3><td>e</tr></tbody><tfoot id=f><tr id=r4><td>f</tr></tfoot></table><table id=empty></table></body>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TableApiTest() {
    }

    /**
     * The parts and the collections of a table.
     * <p>
     * 표의 부분과 컬렉션.
     */
    public void testPartsAndCollections() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("c", e.eval("document.getElementById('t').caption.id"), "caption");
        Check.equal("h", e.eval("document.getElementById('t').tHead.id"), "tHead");
        Check.equal("f", e.eval("document.getElementById('t').tFoot.id"), "tFoot");
        Check.equal("2", e.eval("document.getElementById('t').tBodies.length"), "tBodies");
        Check.equal("b2", e.eval("document.getElementById('t').tBodies[1].id"), "second body");
        Check.equal("5", e.eval("document.getElementById('t').rows.length"), "rows of head, bodies and foot");
        Check.equal("r0,r1,r2,r3,r4", e.eval("var s = []; var rows = document.getElementById('t').rows; for (var i = 0; i < rows.length; i++) s.push(rows[i].id); "
                + "s.join(',')"), "rows in order");
        Check.equal("2", e.eval("document.getElementById('b1').rows.length"), "rows of a section");
        Check.equal("2", e.eval("document.getElementById('r1').cells.length"), "cells");
        Check.equal("[object HTMLTableSectionElement]", e.eval("Object.prototype.toString.call(document.getElementById('b1').rows[0].parentNode)"),
                "the section interface");
        Check.equal("null", e.eval("String(document.getElementById('empty').caption)"), "no caption");
        Check.equal("0", e.eval("document.getElementById('empty').rows.length"), "no rows");
    }

    /**
     * The index attributes.
     * <p>
     * 인덱스 속성.
     */
    public void testPositions() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("0,0", e.eval("var r0 = document.getElementById('r0'); r0.rowIndex + ',' + r0.sectionRowIndex"), "first row");
        Check.equal("2,1", e.eval("var r2 = document.getElementById('r2'); r2.rowIndex + ',' + r2.sectionRowIndex"), "second row of the first body");
        Check.equal("3,0", e.eval("var r3 = document.getElementById('r3'); r3.rowIndex + ',' + r3.sectionRowIndex"), "row of the second body");
        Check.equal("1", e.eval("document.getElementById('r1').cells[1].cellIndex"), "cellIndex");
        Check.equal("-1,-1", e.eval("var x = document.createElement('tr'); x.rowIndex + ',' + x.sectionRowIndex"), "a row outside a table");
        Check.equal("-1", e.eval("document.createElement('td').cellIndex"), "a cell outside a row");
    }

    /**
     * <code>insertRow</code> and <code>deleteRow</code> of a table, with their index rules and errors.
     * <p>
     * 표의 <code>insertRow</code> 와 <code>deleteRow</code>, 인덱스 규칙과 오류.
     */
    public void testInsertAndDeleteRows() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("TR:b1", e.eval("var t = document.getElementById('t'); var n = t.insertRow(2); n.tagName + ':' + n.parentNode.id"),
                "inserted before the row with that index, in its section");
        Check.equal("6", e.eval("document.getElementById('t').rows.length"), "six rows");
        Check.equal("true", e.eval("var t2 = document.getElementById('t'); var last = t2.insertRow(); t2.rows[t2.rows.length - 1] === last "
                + "&& last.parentNode.id === 'f'"), "no argument appends to the parent of the last row");
        Check.equal("true", e.eval("var t3 = document.getElementById('t'); var first = t3.insertRow(0); t3.rows[0] === first && first.parentNode.id === 'h'"),
                "index 0 goes before the first row");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('t').insertRow(99); 'no error' } catch (x) { x.name }"), "too large");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('t').insertRow(-2); 'no error' } catch (x) { x.name }"), "less than -1");
        Check.equal("8", e.eval("document.getElementById('t').rows.length"), "no row was added by the failed calls");
        Check.equal("true", e.eval("var t4 = document.getElementById('t'); var row = t4.rows[1]; t4.deleteRow(1); row.parentNode === null "
                + "&& t4.rows.length === 7"), "deleteRow");
        Check.equal("6", e.eval("var t5 = document.getElementById('t'); t5.deleteRow(-1); String(t5.rows.length)"), "deleteRow(-1) removes the last row");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('t').deleteRow(99); 'no error' } catch (x) { x.name }"), "deleteRow too large");
        Check.equal("TypeError", e.eval("try { document.getElementById('t').deleteRow(); 'no error' } catch (x) { x.name }"), "deleteRow needs an argument");
        Check.equal("tbody:tr:1", e.eval("var em = document.getElementById('empty'); var r = em.insertRow(); "
                + "r.parentNode.tagName.toLowerCase() + ':' + r.tagName.toLowerCase() + ':' + em.rows.length"),
                "the first row of an empty table goes into a new tbody");
        Check.equal("0", e.eval("var em2 = document.getElementById('empty'); em2.deleteRow(-1); em2.rows.length"), "the last row is deleted");
        Check.equal("undefined", e.eval("String(document.getElementById('empty').deleteRow(-1))"), "deleteRow(-1) of a table without rows does nothing");
    }

    /**
     * The sections, rows and cells: <code>insertRow</code>, <code>insertCell</code>, <code>deleteCell</code>.
     * <p>
     * 구역, 행, 셀: <code>insertRow</code>, <code>insertCell</code>, <code>deleteCell</code>.
     */
    public void testSectionsRowsAndCells() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("true", e.eval("var b = document.getElementById('b1'); var n = b.insertRow(1); b.rows[1] === n && b.rows.length === 3"),
                "a section inserts a row at its index");
        Check.equal("true", e.eval("var b2 = document.getElementById('b1'); var l = b2.insertRow(); b2.rows[b2.rows.length - 1] === l"), "and at the end");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('b1').insertRow(50); 'no error' } catch (x) { x.name }"), "section index error");
        Check.equal("true", e.eval("var b3 = document.getElementById('b1'); var rw = b3.rows[1]; b3.deleteRow(1); rw.parentNode === null"), "section deleteRow");
        Check.equal("TD:1", e.eval("var r = document.getElementById('r1'); var c = r.insertCell(1); c.tagName + ':' + c.cellIndex"),
                "a cell is inserted at its index");
        Check.equal("3", e.eval("document.getElementById('r1').cells.length"), "three cells");
        Check.equal("true", e.eval("var r1 = document.getElementById('r1'); var end = r1.insertCell(); r1.cells[r1.cells.length - 1] === end"), "cell at the end");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('r1').insertCell(99); 'no error' } catch (x) { x.name }"), "cell index error");
        Check.equal("3", e.eval("var r2 = document.getElementById('r1'); r2.deleteCell(0); r2.cells.length"), "deleteCell");
        Check.equal("2", e.eval("var r3 = document.getElementById('r1'); r3.deleteCell(-1); r3.cells.length"), "deleteCell(-1)");
        Check.equal("IndexSizeError", e.eval("try { document.getElementById('r1').deleteCell(9); 'no error' } catch (x) { x.name }"), "deleteCell too large");
    }

    /**
     * Creating and deleting the parts, and the setters.
     * <p>
     * 부분의 생성과 삭제, 설정자.
     */
    public void testCreateDeleteAndSetParts() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("CAPTION:true", e.eval("var em = document.getElementById('empty'); var c = em.createCaption(); c.tagName + ':' + (em.firstChild === c)"),
                "createCaption makes the first child");
        Check.equal("true", e.eval("var em2 = document.getElementById('empty'); em2.createCaption() === em2.caption"), "a second call returns the same caption");
        Check.equal("null", e.eval("var em3 = document.getElementById('empty'); em3.deleteCaption(); String(em3.caption)"), "deleteCaption");
        Check.equal("THEAD,CAPTION,true", e.eval("var em4 = document.getElementById('empty'); em4.createCaption(); var h = em4.createTHead(); "
                + "[h.tagName, em4.firstChild.tagName, em4.children[1] === h].join(',')"), "createTHead goes after the caption");
        Check.equal("true", e.eval("var em5 = document.getElementById('empty'); em5.createTHead() === em5.tHead && em5.createTFoot() === em5.tFoot"), "head and foot");
        Check.equal("2", e.eval("var em6 = document.getElementById('empty'); em6.createTBody(); em6.createTBody(); em6.tBodies.length"), "createTBody");
        Check.equal("null,null", e.eval("var em7 = document.getElementById('empty'); em7.deleteTHead(); em7.deleteTFoot(); String(em7.tHead) + ',' + String(em7.tFoot)"),
                "deleteTHead and deleteTFoot");
        Check.equal("n", e.eval("var t = document.getElementById('t'); var cap = document.createElement('caption'); cap.id = 'n'; t.caption = cap; "
                + "t.caption.id"), "the caption setter replaces the old caption");
        Check.equal("1", e.eval("document.getElementById('t').getElementsByTagName('caption').length"), "only one caption is left");
        Check.equal("HierarchyRequestError", e.eval("try { document.getElementById('t').caption = document.createElement('div'); 'no error' } "
                + "catch (x) { x.name }"), "the setter refuses another element");
        Check.equal("h2", e.eval("var t2 = document.getElementById('t'); var nh = document.createElement('thead'); nh.id = 'h2'; t2.tHead = nh; t2.tHead.id"),
                "tHead setter");
        Check.equal("f2", e.eval("var t3 = document.getElementById('t'); var nf = document.createElement('tfoot'); nf.id = 'f2'; t3.tFoot = nf; t3.tFoot.id"),
                "tFoot setter");
    }

    /**
     * The collections are live and the parser's table elements and the created ones behave the same.
     * <p>
     * 컬렉션은 살아 있고 파서가 만든 표 요소와 만든 요소가 똑같이 동작한다.
     */
    public void testLiveCollectionsAndCreatedTables() {
        DomTestEnv e = new DomTestEnv("<!doctype html><body></body>");
        Check.equal("2,3", e.eval("var t = document.createElement('table'); var rows = t.rows; var a = t.insertRow(); a.insertCell(); a.insertCell(); "
                + "t.insertRow().insertCell(); var cells = a.cells; var n1 = cells.length; a.insertCell(); n1 + ',' + cells.length"),
                "the cells collection is live");
        Check.equal("2", e.eval("var t2 = document.createElement('table'); var rr = t2.rows; t2.insertRow(); t2.insertRow(); rr.length"), "the rows collection is live");
        Check.equal("TBODY", e.eval("var t3 = document.createElement('table'); t3.insertRow(); t3.firstChild.tagName"), "the new body is the child of the table");
    }
}
