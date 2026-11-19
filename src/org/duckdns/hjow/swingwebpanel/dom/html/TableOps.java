package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The table object model of the HTML specification (HTML5 2014 REC section 4.9, interfaces <code>HTMLTableElement</code>,
 * <code>HTMLTableSectionElement</code>, <code>HTMLTableRowElement</code> and <code>HTMLTableCellElement</code>): the parts of a table
 * (<code>caption</code>, <code>tHead</code>, <code>tFoot</code>, <code>tBodies</code>), the live collections of rows and cells, and the
 * operations that create, insert and delete them with the index rules of the specification (an index of -1 means the end, an index outside
 * the range is an <code>IndexSizeError</code>). The functions work on the DOM tree directly, so the result is the same for the parser, the
 * scripts and the layout; the script bindings are in <code>js.dom.TableBindings</code>. The live row list is in tree order (thead rows,
 * body rows and foot rows as they stand in the markup), which equals the order of the specification for the usual order thead, tbody,
 * tfoot.
 * <p>
 * HTML 규격의 표 객체 모델(HTML5 2014 REC 4.9절, 인터페이스 <code>HTMLTableElement</code>, <code>HTMLTableSectionElement</code>,
 * <code>HTMLTableRowElement</code>, <code>HTMLTableCellElement</code>): 표의 부분(<code>caption</code>, <code>tHead</code>, <code>tFoot</code>,
 * <code>tBodies</code>), 행과 셀의 라이브 컬렉션, 규격의 인덱스 규칙(-1 은 끝, 범위 밖은 <code>IndexSizeError</code>)으로 그것들을 만들고 넣고 지우는
 * 연산. 함수는 DOM 트리를 직접 다루므로 파서, 스크립트, 배치에서 결과가 같으며 스크립트 연결은 <code>js.dom.TableBindings</code> 에 있다. 라이브 행
 * 목록은 트리 순서(마크업에 놓인 thead 행, 본문 행, tfoot 행)이며, 흔한 순서 thead, tbody, tfoot 에서는 규격의 순서와 같다.
 */
public final class TableOps {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private TableOps() {
    }

    /**
     * Tells whether a node is an HTML element with a local name.
     * <p>
     * 노드가 어떤 로컬 이름의 HTML 요소인지 알려 준다.
     *
     * @param n    the node / 노드
     * @param name the local name / 로컬 이름
     * @return true if it is such an element / 그런 요소이면 true
     */
    public static boolean is(Node n, String name) {
        return n instanceof Element && Namespaces.HTML.equals(((Element) n).getNamespaceURI()) && name.equals(((Element) n).getLocalName());
    }

    /**
     * Returns the first element child with a local name, or null.
     * <p>
     * 로컬 이름이 같은 첫 요소 자식을 반환한다. 없으면 null.
     *
     * @param parent the parent / 부모
     * @param name   the local name / 로컬 이름
     * @return the child or null / 자식 또는 null
     */
    private static Element firstChild(Node parent, String name) {
        // 자식을 앞에서부터 보며 이름이 같은 첫 요소를 찾는다.
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, name)) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Returns the last element child with a local name, or null.
     * <p>
     * 로컬 이름이 같은 마지막 요소 자식을 반환한다. 없으면 null.
     *
     * @param parent the parent / 부모
     * @param name   the local name / 로컬 이름
     * @return the child or null / 자식 또는 null
     */
    private static Element lastChild(Node parent, String name) {
        Element found = null;
        // 자식을 모두 보며 이름이 같은 마지막 요소를 기억한다.
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, name)) {
                found = (Element) n;
            }
        }
        return found;
    }

    /**
     * Returns the <code>caption</code> of a table, or null.
     * <p>
     * 표의 <code>caption</code> 을 반환한다. 없으면 null.
     *
     * @param table the table / 표
     * @return the caption or null / caption 또는 null
     */
    public static Element caption(Element table) {
        return firstChild(table, "caption");
    }

    /**
     * Returns the <code>thead</code> of a table, or null.
     * <p>
     * 표의 <code>thead</code> 를 반환한다. 없으면 null.
     *
     * @param table the table / 표
     * @return the thead or null / thead 또는 null
     */
    public static Element tHead(Element table) {
        return firstChild(table, "thead");
    }

    /**
     * Returns the <code>tfoot</code> of a table, or null.
     * <p>
     * 표의 <code>tfoot</code> 를 반환한다. 없으면 null.
     *
     * @param table the table / 표
     * @return the tfoot or null / tfoot 또는 null
     */
    public static Element tFoot(Element table) {
        return firstChild(table, "tfoot");
    }

    /**
     * Returns the live collection of the <code>tbody</code> children of a table.
     * <p>
     * 표의 <code>tbody</code> 자식의 라이브 컬렉션을 반환한다.
     *
     * @param table the table / 표
     * @return the collection / 컬렉션
     */
    public static ElementCollection tBodies(Element table) {
        return ElementCollections.childrenByTagName(table, "tbody");
    }

    /**
     * Returns the live collection of all rows of a table (the rows of the head, the bodies and the foot, and the rows that are children of
     * the table).
     * <p>
     * 표의 모든 행(머리, 본문, 바닥의 행과 표의 자식인 행)의 라이브 컬렉션을 반환한다.
     *
     * @param table the table / 표
     * @return the collection / 컬렉션
     */
    public static ElementCollection rows(Element table) {
        return ElementCollections.tableRows(table);
    }

    /**
     * Returns the live collection of the rows that are children of a table section.
     * <p>
     * 표 구역의 자식인 행의 라이브 컬렉션을 반환한다.
     *
     * @param section the section (<code>thead</code>, <code>tbody</code> or <code>tfoot</code>) / 구역
     * @return the collection / 컬렉션
     */
    public static ElementCollection sectionRows(Element section) {
        return ElementCollections.childrenByTagName(section, "tr");
    }

    /**
     * Returns the live collection of the cells (<code>td</code> and <code>th</code>) that are children of a row.
     * <p>
     * 행의 자식인 셀(<code>td</code> 와 <code>th</code>)의 라이브 컬렉션을 반환한다.
     *
     * @param row the row / 행
     * @return the collection / 컬렉션
     */
    public static ElementCollection cells(Element row) {
        return ElementCollections.childrenByTagName(row, "td", "th");
    }

    /**
     * Creates an element of the document of a node.
     * <p>
     * 노드가 속한 문서의 요소를 만든다.
     *
     * @param context a node of the document / 문서의 노드
     * @param name    the local name / 로컬 이름
     * @return the new element / 새 요소
     */
    private static Element create(Node context, String name) {
        Document d = context.document();
        return d.createElementNS(Namespaces.HTML, name);
    }

    /**
     * Returns the <code>caption</code> of a table and creates it as the first child if there is none.
     * <p>
     * 표의 <code>caption</code> 을 반환하며 없으면 첫 자식으로 만든다.
     *
     * @param table the table / 표
     * @return the caption / caption
     */
    public static Element createCaption(Element table) {
        Element c = caption(table);
        if (c == null) {
            c = create(table, "caption");
            table.insertBefore(c, table.getFirstChild());
        }
        return c;
    }

    /**
     * Removes the <code>caption</code> of a table.
     * <p>
     * 표의 <code>caption</code> 을 지운다.
     *
     * @param table the table / 표
     */
    public static void deleteCaption(Element table) {
        Element c = caption(table);
        if (c != null) {
            table.removeChild(c);
        }
    }

    /**
     * Returns the <code>thead</code> and creates it if there is none: it goes before the first child that is neither a <code>caption</code>
     * nor a <code>colgroup</code>.
     * <p>
     * <code>thead</code> 를 반환하며 없으면 만든다. <code>caption</code> 이나 <code>colgroup</code> 이 아닌 첫 자식 앞에 들어간다.
     *
     * @param table the table / 표
     * @return the thead / thead
     */
    public static Element createTHead(Element table) {
        Element h = tHead(table);
        if (h == null) {
            h = create(table, "thead");
            Node before = table.getFirstChild();
            // caption 과 colgroup 을 건너뛴 첫 자식 앞에 넣는다.
            while (before != null && (is(before, "caption") || is(before, "colgroup"))) {
                before = before.getNextSibling();
            }
            table.insertBefore(h, before);
        }
        return h;
    }

    /**
     * Removes the <code>thead</code> of a table.
     * <p>
     * 표의 <code>thead</code> 를 지운다.
     *
     * @param table the table / 표
     */
    public static void deleteTHead(Element table) {
        Element h = tHead(table);
        if (h != null) {
            table.removeChild(h);
        }
    }

    /**
     * Returns the <code>tfoot</code> and creates it as the last child if there is none.
     * <p>
     * <code>tfoot</code> 를 반환하며 없으면 마지막 자식으로 만든다.
     *
     * @param table the table / 표
     * @return the tfoot / tfoot
     */
    public static Element createTFoot(Element table) {
        Element f = tFoot(table);
        if (f == null) {
            f = create(table, "tfoot");
            table.appendChild(f);
        }
        return f;
    }

    /**
     * Removes the <code>tfoot</code> of a table.
     * <p>
     * 표의 <code>tfoot</code> 를 지운다.
     *
     * @param table the table / 표
     */
    public static void deleteTFoot(Element table) {
        Element f = tFoot(table);
        if (f != null) {
            table.removeChild(f);
        }
    }

    /**
     * Creates a <code>tbody</code> after the last <code>tbody</code> (or at the end if there is none) and returns it.
     * <p>
     * 마지막 <code>tbody</code> 뒤(없으면 끝)에 <code>tbody</code> 를 만들어 반환한다.
     *
     * @param table the table / 표
     * @return the new tbody / 새 tbody
     */
    public static Element createTBody(Element table) {
        Element body = create(table, "tbody");
        Element last = lastChild(table, "tbody");
        table.insertBefore(body, last == null ? null : last.getNextSibling());
        return body;
    }

    /**
     * Sets the <code>caption</code> of a table: the old one is removed and the new one becomes the first child.
     * <p>
     * 표의 <code>caption</code> 을 지정한다. 이전 것은 지우고 새 것이 첫 자식이 된다.
     *
     * @param table the table / 표
     * @param value the new caption or null / 새 caption 또는 null
     * @throws DomException <code>HierarchyRequestError</code> if the value is not a caption / 값이 caption 이 아니면
     */
    public static void setCaption(Element table, Element value) {
        if (value != null && !is(value, "caption")) {
            throw new DomException("HierarchyRequestError", DomException.HIERARCHY_REQUEST_ERR, "The new caption is not a caption element.");
        }
        deleteCaption(table);
        if (value != null) {
            table.insertBefore(value, table.getFirstChild());
        }
    }

    /**
     * Sets the <code>thead</code> of a table: the old one is removed and the new one goes to the place of a created head.
     * <p>
     * 표의 <code>thead</code> 를 지정한다. 이전 것은 지우고 새 것이 만들어진 머리의 자리에 들어간다.
     *
     * @param table the table / 표
     * @param value the new head or null / 새 머리 또는 null
     * @throws DomException <code>HierarchyRequestError</code> if the value is not a thead / 값이 thead 가 아니면
     */
    public static void setTHead(Element table, Element value) {
        if (value != null && !is(value, "thead")) {
            throw new DomException("HierarchyRequestError", DomException.HIERARCHY_REQUEST_ERR, "The new header is not a thead element.");
        }
        deleteTHead(table);
        if (value != null) {
            Node before = table.getFirstChild();
            // caption 과 colgroup 을 건너뛴 첫 자식 앞에 넣는다.
            while (before != null && (is(before, "caption") || is(before, "colgroup"))) {
                before = before.getNextSibling();
            }
            table.insertBefore(value, before);
        }
    }

    /**
     * Sets the <code>tfoot</code> of a table: the old one is removed and the new one becomes the last child.
     * <p>
     * 표의 <code>tfoot</code> 를 지정한다. 이전 것은 지우고 새 것이 마지막 자식이 된다.
     *
     * @param table the table / 표
     * @param value the new foot or null / 새 바닥 또는 null
     * @throws DomException <code>HierarchyRequestError</code> if the value is not a tfoot / 값이 tfoot 이 아니면
     */
    public static void setTFoot(Element table, Element value) {
        if (value != null && !is(value, "tfoot")) {
            throw new DomException("HierarchyRequestError", DomException.HIERARCHY_REQUEST_ERR, "The new footer is not a tfoot element.");
        }
        deleteTFoot(table);
        if (value != null) {
            table.appendChild(value);
        }
    }

    /**
     * Throws the error of an index that is out of range.
     * <p>
     * 범위를 벗어난 인덱스의 오류를 던진다.
     *
     * @param what what was indexed / 인덱스가 가리킨 것
     * @param index the index / 인덱스
     * @throws DomException always, <code>IndexSizeError</code> / 항상, <code>IndexSizeError</code>
     */
    private static void indexError(String what, int index) {
        throw new DomException("IndexSizeError", DomException.INDEX_SIZE_ERR, "The " + what + " index " + index + " is out of range.");
    }

    /**
     * Inserts a row into a table: with an index of -1 or the number of rows the row goes to the end (into the last body, a new body if the
     * table has none), else before the row with that index.
     * <p>
     * 표에 행을 넣는다. 인덱스가 -1 이거나 행 수이면 끝(마지막 본문 안, 표에 본문이 없으면 새 본문)에, 아니면 그 인덱스의 행 앞에 넣는다.
     *
     * @param table the table / 표
     * @param index the index, -1 for the end / 인덱스. -1 은 끝
     * @return the new row / 새 행
     * @throws DomException <code>IndexSizeError</code> if the index is less than -1 or more than the number of rows / 인덱스가 -1 보다 작거나 행 수보다 크면
     */
    public static Element insertRow(Element table, int index) {
        List<Element> rows = rows(table).toList();
        if (index < -1 || index > rows.size()) {
            indexError("row", index);
        }
        Element row = create(table, "tr");
        if (rows.isEmpty()) {
            Element body = lastChild(table, "tbody");
            if (body == null) {
                body = create(table, "tbody");
                table.appendChild(body);
            }
            body.appendChild(row);
        } else if (index == -1 || index == rows.size()) {
            Node parent = rows.get(rows.size() - 1).getParentNode();
            parent.appendChild(row);
        } else {
            Element target = rows.get(index);
            target.getParentNode().insertBefore(row, target);
        }
        return row;
    }

    /**
     * Deletes a row of a table: -1 is the last row.
     * <p>
     * 표의 행을 지운다. -1 은 마지막 행이다.
     *
     * @param table the table / 표
     * @param index the index / 인덱스
     * @throws DomException <code>IndexSizeError</code> if the index is out of range / 인덱스가 범위를 벗어나면
     */
    public static void deleteRow(Element table, int index) {
        List<Element> rows = rows(table).toList();
        if (index < -1 || index >= rows.size()) {
            indexError("row", index);
        }
        if (index == -1) {
            if (rows.isEmpty()) {
                return;
            }
            index = rows.size() - 1;
        }
        Element row = rows.get(index);
        row.getParentNode().removeChild(row);
    }

    /**
     * Inserts a row into a table section.
     * <p>
     * 표 구역에 행을 넣는다.
     *
     * @param section the section / 구역
     * @param index   the index, -1 for the end / 인덱스. -1 은 끝
     * @return the new row / 새 행
     * @throws DomException <code>IndexSizeError</code> if the index is out of range / 인덱스가 범위를 벗어나면
     */
    public static Element insertSectionRow(Element section, int index) {
        List<Element> rows = sectionRows(section).toList();
        if (index < -1 || index > rows.size()) {
            indexError("row", index);
        }
        Element row = create(section, "tr");
        section.insertBefore(row, index == -1 || index == rows.size() ? null : rows.get(index));
        return row;
    }

    /**
     * Deletes a row of a table section: -1 is the last row.
     * <p>
     * 표 구역의 행을 지운다. -1 은 마지막 행이다.
     *
     * @param section the section / 구역
     * @param index   the index / 인덱스
     * @throws DomException <code>IndexSizeError</code> if the index is out of range / 인덱스가 범위를 벗어나면
     */
    public static void deleteSectionRow(Element section, int index) {
        List<Element> rows = sectionRows(section).toList();
        if (index < -1 || index >= rows.size()) {
            indexError("row", index);
        }
        if (index == -1) {
            if (rows.isEmpty()) {
                return;
            }
            index = rows.size() - 1;
        }
        section.removeChild(rows.get(index));
    }

    /**
     * Inserts a cell (<code>td</code>) into a row.
     * <p>
     * 행에 셀(<code>td</code>)을 넣는다.
     *
     * @param row   the row / 행
     * @param index the index, -1 for the end / 인덱스. -1 은 끝
     * @return the new cell / 새 셀
     * @throws DomException <code>IndexSizeError</code> if the index is out of range / 인덱스가 범위를 벗어나면
     */
    public static Element insertCell(Element row, int index) {
        List<Element> cells = cells(row).toList();
        if (index < -1 || index > cells.size()) {
            indexError("cell", index);
        }
        Element cell = create(row, "td");
        row.insertBefore(cell, index == -1 || index == cells.size() ? null : cells.get(index));
        return cell;
    }

    /**
     * Deletes a cell of a row: -1 is the last cell.
     * <p>
     * 행의 셀을 지운다. -1 은 마지막 셀이다.
     *
     * @param row   the row / 행
     * @param index the index / 인덱스
     * @throws DomException <code>IndexSizeError</code> if the index is out of range / 인덱스가 범위를 벗어나면
     */
    public static void deleteCell(Element row, int index) {
        List<Element> cells = cells(row).toList();
        if (index < -1 || index >= cells.size()) {
            indexError("cell", index);
        }
        if (index == -1) {
            if (cells.isEmpty()) {
                return;
            }
            index = cells.size() - 1;
        }
        row.removeChild(cells.get(index));
    }

    /**
     * Returns the table that a row belongs to: its parent table, or the table that is the parent of its parent section.
     * <p>
     * 행이 속한 표를 반환한다: 부모 표, 또는 부모 구역의 부모인 표.
     *
     * @param row the row / 행
     * @return the table or null / 표 또는 null
     */
    private static Element tableOfRow(Element row) {
        Node p = row.getParentNode();
        if (is(p, "table")) {
            return (Element) p;
        }
        if ((is(p, "thead") || is(p, "tbody") || is(p, "tfoot")) && is(p.getParentNode(), "table")) {
            return (Element) p.getParentNode();
        }
        return null;
    }

    /**
     * Returns the position of a row in the rows of its table (<code>HTMLTableRowElement.rowIndex</code>), or -1 if it is in no table.
     * <p>
     * 행이 속한 표의 행 목록에서의 위치를 반환한다(<code>HTMLTableRowElement.rowIndex</code>). 표에 없으면 -1.
     *
     * @param row the row / 행
     * @return the index or -1 / 인덱스 또는 -1
     */
    public static int rowIndex(Element row) {
        Element table = tableOfRow(row);
        return table == null ? -1 : indexIn(rows(table).toList(), row);
    }

    /**
     * Returns the position of a row in the rows of its parent (<code>HTMLTableRowElement.sectionRowIndex</code>), or -1 if it has none of
     * the valid parents.
     * <p>
     * 행이 부모의 행 목록에서 차지하는 위치를 반환한다(<code>HTMLTableRowElement.sectionRowIndex</code>). 유효한 부모가 없으면 -1.
     *
     * @param row the row / 행
     * @return the index or -1 / 인덱스 또는 -1
     */
    public static int sectionRowIndex(Element row) {
        Element table = tableOfRow(row);
        if (table == null) {
            return -1;
        }
        Node p = row.getParentNode();
        return indexIn(is(p, "table") ? rows(table).toList() : sectionRows((Element) p).toList(), row);
    }

    /**
     * Returns the position of a cell in the cells of its row (<code>HTMLTableCellElement.cellIndex</code>), or -1 if the parent is no row.
     * <p>
     * 셀이 행의 셀 목록에서 차지하는 위치를 반환한다(<code>HTMLTableCellElement.cellIndex</code>). 부모가 행이 아니면 -1.
     *
     * @param cell the cell / 셀
     * @return the index or -1 / 인덱스 또는 -1
     */
    public static int cellIndex(Element cell) {
        Node p = cell.getParentNode();
        return is(p, "tr") ? indexIn(cells((Element) p).toList(), cell) : -1;
    }

    /**
     * Returns the position of an element in a list, or -1.
     * <p>
     * 목록에서 요소의 위치를 반환한다. 없으면 -1.
     *
     * @param list the list / 목록
     * @param e    the element / 요소
     * @return the index or -1 / 인덱스 또는 -1
     */
    private static int indexIn(List<Element> list, Element e) {
        // 목록을 앞에서부터 보며 같은 요소의 위치를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == e) {
                return i;
            }
        }
        return -1;
    }
}
