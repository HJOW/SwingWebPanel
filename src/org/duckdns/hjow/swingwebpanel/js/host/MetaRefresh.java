package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The declarative refresh of a document: <code>&lt;meta http-equiv="refresh" content="5"&gt;</code> reloads the page after 5 seconds and
 * <code>content="5; url=/next"</code> (also <code>5, /next</code> and <code>5;/next</code>) goes to another address after 5 seconds (HTML5
 * 2014 REC section 4.2.5.3, the Refresh state of the pragma directive). Only the first refresh of a document is used, a time of 0 replaces
 * the entry of the history instead of adding one, and the move goes through the same navigation policy as <code>location.assign()</code>
 * (a blocked scheme is not requested). The timer belongs to the page: it does not run after the page was replaced, cleaned or disposed. It
 * needs the script runtime of the page, so it does not run when JavaScript is switched off (a page without scripting has no page runtime: known limitation). A <code>content</code> that does not start with digits, or with an address that cannot be resolved,
 * is ignored.
 * <p>
 * 문서의 선언적 새로고침: <code>&lt;meta http-equiv="refresh" content="5"&gt;</code> 는 5초 뒤 페이지를 다시 읽고, <code>content="5; url=/next"</code>(<code>5, /next</code> 와
 * <code>5;/next</code> 도)는 5초 뒤 다른 주소로 간다(HTML5 2014 REC 4.2.5.3절, pragma directive 의 Refresh 상태). 문서의 첫 새로고침만 쓰고, 시간이 0 이면 이력에
 * 항목을 더하지 않고 현재 항목을 교체하며, 이동은 <code>location.assign()</code> 과 같은 이동 정책을 거친다(차단된 스킴은 요청하지 않는다). 타이머는 페이지에 속하므로
 * 페이지가 교체, clean, 폐기된 뒤에는 실행되지 않는다. 페이지 런타임이 필요하므로 JavaScript 를 끄면 동작하지 않는다(스크립트를 끈 페이지에는 페이지 런타임이 없다: 알려진 한계). 숫자로 시작하지 않거나 해석할 수 없는 주소가 든 <code>content</code> 는 무시한다.
 */
final class MetaRefresh {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private MetaRefresh() {
    }

    /**
     * Looks for <code>meta http-equiv=refresh</code> elements in a subtree and starts the first one if the document has none yet.
     * <p>
     * 하위 트리에서 <code>meta http-equiv=refresh</code> 요소를 찾아 문서에 아직 없으면 첫 것을 시작한다.
     *
     * @param page the page / 페이지
     * @param root the root of the subtree (the document or an inserted node) / 하위 트리의 루트(문서나 삽입된 노드)
     */
    static void scan(PageScripts page, Node root) {
        if (page.isDisposed()) {
            return;
        }
        Node n = root;
        // 하위 트리를 트리 순서로 훑으며 meta refresh 를 찾는다.
        while (n != null) {
            if (n instanceof Element && isRefresh((Element) n) && start(page, (Element) n)) {
                return;
            }
            n = next(n, root);
        }
    }

    /**
     * Returns the next node of a depth first walk below a root.
     * <p>
     * 루트 아래 깊이 우선 순회의 다음 노드를 반환한다.
     *
     * @param n    the current node / 현재 노드
     * @param root the root / 루트
     * @return the next node or null / 다음 노드 또는 null
     */
    private static Node next(Node n, Node root) {
        if (n.getFirstChild() != null) {
            return n.getFirstChild();
        }
        // 형제가 없으면 위로 올라가며 다음 형제를 찾는다.
        for (Node p = n; p != null && p != root; p = p.getParentNode()) {
            if (p.getNextSibling() != null) {
                return p.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Tells whether an element is a <code>meta</code> element in the refresh state.
     * <p>
     * 요소가 refresh 상태의 <code>meta</code> 요소인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for meta http-equiv=refresh / meta http-equiv=refresh 이면 true
     */
    private static boolean isRefresh(Element e) {
        String he = e.getAttribute("http-equiv");
        return "meta".equals(e.getLocalName()) && Namespaces.HTML.equals(e.getNamespaceURI()) && he != null
                && "refresh".equals(he.trim().toLowerCase(java.util.Locale.ENGLISH)) && e.hasAttribute("content");
    }

    /**
     * Parses the <code>content</code> and schedules the move.
     * <p>
     * <code>content</code> 를 해석하고 이동을 예약한다.
     *
     * @param page the page / 페이지
     * @param meta the element / 요소
     * @return true if a refresh was scheduled / 새로고침을 예약했으면 true
     */
    private static boolean start(final PageScripts page, Element meta) {
        String c = meta.getAttribute("content").trim();
        int i = 0;
        // 앞쪽 숫자가 시간(초)이다. 숫자가 없으면 무시한다.
        while (i < c.length() && c.charAt(i) >= '0' && c.charAt(i) <= '9') {
            i++;
        }
        if (i == 0) {
            return false;
        }
        long seconds;
        try {
            seconds = Long.parseLong(c.substring(0, i));
        } catch (NumberFormatException e) {
            return false;
        }
        String rest = c.substring(i);
        // 소수 부분(".5")은 읽고 버린다.
        if (rest.startsWith(".")) {
            int k = 1;
            // 소수점 뒤의 숫자를 모두 건너뛴다.
            while (k < rest.length() && rest.charAt(k) >= '0' && rest.charAt(k) <= '9') {
                k++;
            }
            rest = rest.substring(k);
        }
        rest = rest.trim();
        // 시간 뒤의 구분자(; 또는 ,)를 건너뛴다.
        while (rest.length() > 0 && (rest.charAt(0) == ';' || rest.charAt(0) == ',')) {
            rest = rest.substring(1).trim();
        }
        if (rest.length() >= 3 && rest.substring(0, 3).equalsIgnoreCase("url")) {
            rest = rest.substring(3).trim();
            if (rest.startsWith("=")) {
                rest = rest.substring(1).trim();
            }
        }
        // 주소를 감싼 따옴표를 벗긴다.
        if (rest.length() >= 2 && (rest.charAt(0) == '"' || rest.charAt(0) == '\'') && rest.charAt(rest.length() - 1) == rest.charAt(0)) {
            rest = rest.substring(1, rest.length() - 1);
        } else if (rest.length() >= 1 && (rest.charAt(0) == '"' || rest.charAt(0) == '\'')) {
            rest = rest.substring(1);
        }
        final String target;
        if (rest.length() == 0) {
            target = null;
        } else {
            Document doc = meta.getOwnerDocument();
            try {
                target = UrlUtil.resolve(doc.getBaseUri(), rest);
            } catch (RuntimeException e) {
                return false;
            }
        }
        if (!page.claimDeclarativeRefresh()) {
            return false;
        }
        final boolean replace = seconds == 0;
        final HostEnvironment env = page.getEnvironment();
        Scheduler s = env.getScheduler();
        if (s == null) {
            return true;
        }
        s.post(new Runnable() {
            /**
             * Moves under the page lock if the page is still alive.
             * <p>
             * 페이지가 살아 있으면 페이지 잠금 아래에서 이동한다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Starts the navigation or the reload.
                     * <p>
                     * 이동이나 다시 읽기를 시작한다.
                     */
                    public void run() {
                        if (target == null) {
                            env.reload();
                        } else {
                            env.navigate(target, replace);
                        }
                    }
                });
            }
        }, seconds * 1000L);
        return true;
    }
}
