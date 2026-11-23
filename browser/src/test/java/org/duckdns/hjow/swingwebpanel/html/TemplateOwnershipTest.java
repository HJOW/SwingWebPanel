package org.duckdns.hjow.swingwebpanel.html;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Regression tests for template ownership, cloning, importing and adoption (P07-06-a). Contents are not ordinary children,
 * and no static parser table may retain a discarded document through its template fragments.
 * <p>
 * template 소유권·복제·가져오기·이동 회귀 시험. 내용은 일반 자식이 아니며 정적 파서 표가 내용 조각을 통해 버린 문서를 붙잡으면 안 된다.
 */
public class TemplateOwnershipTest {
    /** Creates the test suite. / 시험 스위트를 만든다. */
    public TemplateOwnershipTest() {
    }

    /**
     * Creates a connected template with attributes and nested template contents.
     * <p>속성과 중첩 template 내용이 있는 연결된 template을 만든다.
     * @return the outer template / 바깥 template
     */
    private static Element template() {
        Document doc = HtmlParser.parseDocument("<!doctype html><body><template id=t><p title='one'>outer</p>"
                + "<template><span data-value='two'>inner</span></template></template>", null, null, true);
        return doc.getElementById("t");
    }

    /**
     * A shallow clone has its attributes but an empty, independently allocated contents fragment.
     * <p>얕은 복제는 속성을 가지지만 내용 조각은 비어 있고 독립적으로 만들어진다.
     */
    public void testShallowClone() {
        Element original = template();
        Element copy = (Element) original.cloneNode(false);
        Check.equal("t", copy.getId(), "attributes copied");
        Check.equal(0, copy.getChildNodes().getLength(), "contents are not children");
        DocumentFragment contents = ParserNodeData.ensureTemplateContents(copy);
        Check.isNull(contents.getFirstChild(), "shallow contents empty");
        Check.isTrue(contents != ParserNodeData.getTemplateContents(original), "independent fragment");
        Check.isTrue(contents.getOwnerDocument() == copy.getOwnerDocument(), "fragment owner");
    }

    /**
     * Deep cloning copies nested template contents and modifying the copy leaves the original untouched.
     * <p>깊은 복제는 중첩 template 내용도 복사하며 복사본을 바꾸어도 원본에는 영향이 없다.
     */
    public void testDeepClone() {
        Element original = template();
        Element copy = (Element) original.cloneNode(true);
        Check.equal(original.getInnerHtml(), copy.getInnerHtml(), "nested serialization preserved");
        DocumentFragment originalContents = ParserNodeData.getTemplateContents(original);
        DocumentFragment copiedContents = ParserNodeData.getTemplateContents(copy);
        Check.isTrue(originalContents != copiedContents, "outer fragment independent");
        Element nested = (Element) copiedContents.getLastChild();
        ParserNodeData.getTemplateContents(nested).getFirstChild().setTextContent("changed");
        Check.isTrue(copy.getInnerHtml().indexOf("changed") >= 0, "copy changed");
        Check.isTrue(original.getInnerHtml().indexOf("inner") >= 0, "original unchanged");
        Check.equal(0, copy.getChildNodes().getLength(), "contents stay outside child tree");
    }

    /**
     * importNode gives every copied fragment, descendant and attribute the destination owner document.
     * <p>importNode는 복사한 모든 조각·후손·속성의 소유 문서를 대상 문서로 정한다.
     */
    public void testImport() {
        Element original = template();
        Document target = Document.createHtmlDocument();
        target.setMarkupSupport(new HtmlMarkupSupport());
        Element imported = (Element) target.importNode(original, true);
        Check.equal(original.getInnerHtml(), imported.getInnerHtml(), "import preserves contents");
        assertOwner(imported, target);
        Check.isTrue(original.getOwnerDocument() != target, "original retains owner");
        Element shallow = (Element) target.importNode(original, false);
        Check.isNull(ParserNodeData.ensureTemplateContents(shallow).getFirstChild(), "shallow import empty");
    }

    /**
     * Adopting an ancestor also adopts the fragments of every nested template without connecting their contents.
     * <p>조상을 이동하면 모든 중첩 template 조각도 이동하며 내용이 일반 트리에 연결되지는 않는다.
     */
    public void testAdoptAncestor() {
        Element original = template();
        Element body = original.getParentElement();
        DocumentFragment contents = ParserNodeData.getTemplateContents(original);
        Document target = Document.createHtmlDocument();
        target.adoptNode(body);
        assertOwner(body, target);
        Check.isTrue(contents == ParserNodeData.getTemplateContents(original), "adoption keeps identity");
        Check.isNull(contents.getParentNode(), "contents remain detached");
        Check.isTrue(!contents.getFirstChild().isConnected(), "contents remain inert");
    }

    /**
     * Checks owners recursively, including template fragments that ordinary child iteration does not reach.
     * <p>일반 자식 순회로 도달하지 않는 template 조각까지 포함하여 소유자를 재귀 확인한다.
     * @param node node to inspect / 검사할 노드
     * @param owner expected owner / 기대 소유자
     */
    private static void assertOwner(Node node, Document owner) {
        Check.isTrue(node.getOwnerDocument() == owner, "owner of " + node.getNodeName());
        if (node instanceof Element) {
            Element element = (Element) node;
            // 속성도 이동된 문서를 참조해야 한다.
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Check.isTrue(element.getAttributes().item(i).getOwnerDocument() == owner, "attribute owner");
            }
            DocumentFragment contents = ParserNodeData.getTemplateContents(element);
            if (contents != null) {
                assertOwner(contents, owner);
            }
        }
        // 일반 후손과 그 안의 중첩 template도 모두 확인한다.
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            assertOwner(child, owner);
        }
    }

    /**
     * Creates the weak reference in a separate frame to avoid retaining a parser or document in the polling frame.
     * <p>대기 메서드의 스택이 파서나 문서를 붙잡지 않도록 별도 호출에서 약한 참조를 만든다.
     * @param queue receives the cleared reference / 해제된 참조를 받을 큐
     * @return weak document reference / 문서의 약한 참조
     */
    private static WeakReference<Document> discarded(ReferenceQueue<Document> queue) {
        return new WeakReference<Document>(template().getOwnerDocument(), queue);
    }

    /**
     * Parser metadata does not root documents containing templates. GC timing is not a product guarantee; this bounded
     * observation complements the removal of the static contents map.
     * <p>파서 부가 정보가 template 문서를 붙잡지 않는다. GC 시점의 제품 보장이 아닌 제한 시간 내 관측이며 전역 표 제거의 구조 검증을 보완한다.
     * @throws Exception when interrupted / 인터럽트 시
     */
    public void testDiscardedDocumentIsCollectable() throws Exception {
        ReferenceQueue<Document> queue = new ReferenceQueue<Document>();
        WeakReference<Document> reference = discarded(queue);
        long deadline = System.currentTimeMillis() + 30000;
        // 수거 시점은 비결정적이므로 최대 30초 동안 GC를 요청하며 큐를 확인한다.
        while (reference.get() != null && System.currentTimeMillis() < deadline) {
            System.gc();
            Thread.sleep(25);
        }
        Check.isNull(reference.get(), "template document not rooted by metadata");
        Check.isTrue(queue.remove(1000) == reference, "collection reported by reference queue");
    }
}
