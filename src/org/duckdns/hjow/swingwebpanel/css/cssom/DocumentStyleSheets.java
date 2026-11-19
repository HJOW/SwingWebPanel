package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.ProcessingInstruction;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Collects the author style sheets of a document in tree order: the <code>&lt;style&gt;</code> elements (their text, with the <code>media</code>,
 * <code>title</code> and <code>type</code> attributes) and the <code>&lt;link rel="stylesheet"&gt;</code> elements (loaded through a
 * {@link StyleSheetLoader}; alternate style sheets and links without a URL are skipped). <code>@import</code> rules inside the sheets are loaded with
 * the same loader. The collector is synchronous: the page loader calls it on a loading thread, not on the event dispatch thread.
 * <p>
 * 문서의 작성자 스타일 시트를 트리 순서로 모은다: <code>&lt;style&gt;</code> 요소(<code>media</code>, <code>title</code>, <code>type</code> 속성이 있는 텍스트)와
 * <code>&lt;link rel="stylesheet"&gt;</code> 요소({@link StyleSheetLoader} 로 읽음. 대체 스타일 시트와 URL 이 없는 link 는 건너뜀). 시트 안의 <code>@import</code> 규칙은 같은
 * 로더로 읽는다. 수집기는 동기식이며 페이지 로더가 이벤트 디스패치 스레드가 아니라 로딩 스레드에서 호출한다.
 */
public final class DocumentStyleSheets {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DocumentStyleSheets() {
    }

    /**
     * Collects the style sheets of a document.
     * <p>
     * 문서의 스타일 시트를 모은다.
     *
     * @param doc    the document (its base URI resolves relative links) / 문서(기준 URI 가 상대 링크를 해석한다)
     * @param loader loads linked and imported sheets, or null to skip them / 링크와 가져오기 시트를 읽는다. null 이면 건너뜀
     * @param diag   collector or null / 수집기 또는 null
     * @return the sheets in document order / 문서 순서의 시트
     */
    public static List<StyleSheet> collect(Document doc, StyleSheetLoader loader, CssDiagnostics diag) {
        return collect(doc, loader, diag, null);
    }

    /**
     * Collects the style sheets of a document and keeps the sheet objects of the previous collection while their source did not change: a
     * <code>&lt;style&gt;</code> element whose text is the same and a <code>&lt;link&gt;</code> element whose address is the same get the sheet they had,
     * so a script that holds a <code>CSSStyleSheet</code> (and the rules it inserted into it) keeps working when another style element is added or a
     * linked sheet arrives. A kept sheet still loads the <code>@import</code> rules that were pending the last time.
     * <p>
     * 문서의 스타일 시트를 모으되, 출처가 바뀌지 않는 동안에는 이전 수집의 시트 객체를 유지한다: 텍스트가 같은 <code>&lt;style&gt;</code> 요소와 주소가 같은
     * <code>&lt;link&gt;</code> 요소는 가졌던 시트를 그대로 받으므로, 스크립트가 <code>CSSStyleSheet</code>(와 그 안에 넣은 규칙)를 붙잡고 있어도 다른 style 요소가
     * 추가되거나 링크한 시트가 도착할 때 계속 동작한다. 유지한 시트도 지난번에 대기 중이던 <code>@import</code> 규칙을 읽는다.
     *
     * @param doc      the document / 문서
     * @param loader   loads linked and imported sheets, or null to skip them / 링크와 가져오기 시트를 읽는다. null 이면 건너뜀
     * @param diag     collector or null / 수집기 또는 null
     * @param previous the sheets of the previous collection by owner element, or null / 이전 수집의 시트(소유 요소별). 없으면 null
     * @return the sheets in document order / 문서 순서의 시트
     */
    public static List<StyleSheet> collect(Document doc, StyleSheetLoader loader, CssDiagnostics diag, Map<Element, StyleSheet> previous) {
        List<StyleSheet> out = new ArrayList<StyleSheet>();
        String base = doc.getBaseUri() != null && doc.getBaseUri().length() > 0 ? doc.getBaseUri() : doc.getUrl();
        // 호환 모드 문서의 스타일 시트는 호환 모드로 파싱한다(단위 없는 길이, # 없는 16진수 색).
        boolean quirks = doc.getCompatibilityMode() == Document.QUIRKS_MODE;
        // 문서의 요소를 트리 순서로 훑으며 style 과 link 요소를 찾는다.
        for (Node n = Node.nextInTree(doc, doc); n != null; n = Node.nextInTree(n, doc)) {
            if (n instanceof ProcessingInstruction && n.getParentNode() == doc && !doc.isHtmlDocument()
                    && "xml-stylesheet".equals(((ProcessingInstruction) n).getTarget())) {
                // XML 문서(XHTML)의 머리말에 있는 xml-stylesheet 처리 명령은 style 요소처럼 스타일 시트를 연결한다.
                StyleSheet s = fromInstruction((ProcessingInstruction) n, base, loader, diag, quirks);
                if (s != null) {
                    out.add(s);
                }
                continue;
            }
            if (!(n instanceof Element) || !Namespaces.HTML.equals(((Element) n).getNamespaceURI())) {
                continue;
            }
            Element e = (Element) n;
            String name = e.getLocalName();
            StyleSheet prev = previous == null ? null : previous.get(e);
            if (name.equals("style") && typeOk(e)) {
                StyleSheetLoader scoped = scope(loader, e);
                String text = e.getTextContent();
                StyleSheet s;
                if (prev != null && text.equals(prev.getSourceKey())) {
                    // 텍스트가 같으면 시트를 그대로 쓰되 대기 중이던 가져오기를 읽는다.
                    s = prev;
                    s.resolvePendingImports(scoped, diag);
                } else {
                    s = StyleSheet.parse(text, base, StyleSheet.ORIGIN_AUTHOR, scoped, diag, quirks);
                    s.setSourceKey(text);
                }
                decorate(s, e);
                out.add(s);
            } else if (name.equals("link") && isStylesheetLink(e) && typeOk(e)) {
                StyleSheetLoader scoped = scope(loader, e);
                String abs = linkAddress(e, base);
                StyleSheet s;
                if (prev != null && abs != null && abs.equals(prev.getSourceKey())) {
                    // 주소가 같고 이미 읽은 시트이면 그대로 쓰되 대기 중이던 가져오기를 읽는다.
                    s = prev;
                    s.resolvePendingImports(scoped, diag);
                } else {
                    s = loadLink(e, base, scoped, diag, quirks);
                    if (s != null) {
                        s.setSourceKey(abs);
                    }
                }
                if (s != null) {
                    decorate(s, e);
                    out.add(s);
                }
            }
        }
        return out;
    }

    /**
     * Loads the style sheet that an <code>xml-stylesheet</code> processing instruction of an XML document links (Associating Style Sheets
     * with XML documents 1.0): the instruction needs an <code>href</code> and the type <code>text/css</code>; an alternate style sheet
     * (<code>alternate="yes"</code>) is not used; the <code>media</code> and <code>title</code> pseudo-attributes are those of the
     * <code>link</code> element.
     * <p>
     * XML 문서의 <code>xml-stylesheet</code> 처리 명령이 연결하는 스타일 시트를 읽는다(Associating Style Sheets with XML documents 1.0): 처리 명령에는 <code>href</code> 와 타입
     * <code>text/css</code> 가 필요하고, 대체 스타일 시트(<code>alternate="yes"</code>)는 쓰지 않으며, <code>media</code> 와 <code>title</code> 의사 속성은
     * <code>link</code> 요소의 것과 같다.
     *
     * @param pi     the processing instruction / 처리 명령
     * @param base   the document base URL / 문서 기준 URL
     * @param loader the loader or null / 로더 또는 null
     * @param diag   collector or null / 수집기 또는 null
     * @param quirks true to parse the sheet in quirks mode / 시트를 호환 모드로 파싱하면 true
     * @return the sheet, or null if the instruction does not link a usable sheet / 시트. 쓸 수 있는 시트를 연결하지 않으면 null
     */
    private static StyleSheet fromInstruction(ProcessingInstruction pi, String base, StyleSheetLoader loader, CssDiagnostics diag, boolean quirks) {
        Map<String, String> attrs = XmlStylesheetInstruction.parse(pi.getData());
        String href = attrs.get("href");
        String type = attrs.get("type");
        if (href == null || href.trim().length() == 0 || type == null || !"text/css".equalsIgnoreCase(type.trim())
                || "yes".equals(attrs.get("alternate"))) {
            return null;
        }
        String abs;
        try {
            abs = base == null ? href.trim() : UrlUtil.resolve(base, href.trim());
        } catch (RuntimeException ex) {
            CssDiagnostics.report(diag, CssDiagnostics.INVALID, "<?xml-stylesheet href=" + href + ">", "the URL could not be resolved");
            return null;
        }
        if (loader == null) {
            CssDiagnostics.report(diag, CssDiagnostics.UNSUPPORTED, "<?xml-stylesheet href=" + href + ">", "no style sheet loader, instruction skipped");
            return null;
        }
        try {
            LoadedStyleSheet l = loader.load(abs);
            String finalUrl = l.getUrl() != null ? l.getUrl() : abs;
            StyleSheet s = StyleSheet.parse(l.getText(), finalUrl, StyleSheet.ORIGIN_AUTHOR, loader, diag, quirks);
            s.setHref(finalUrl);
            s.setSourceKey(abs);
            s.applyOwnerAttributes(attrs.get("media"), attrs.get("title"), false);
            s.setOwner(pi);
            return s;
        } catch (IOException ex) {
            CssDiagnostics.report(diag, CssDiagnostics.INVALID, "<?xml-stylesheet href=" + href + ">", "could not be loaded: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Resolves the <code>href</code> of a link element against the base URL.
     * <p>
     * link 요소의 <code>href</code> 를 기준 URL 로 해석한다.
     *
     * @param e    the link element / link 요소
     * @param base the document base URL / 문서 기준 URL
     * @return the absolute URL, or null if there is no usable <code>href</code> / 절대 URL. 쓸 수 있는 <code>href</code> 가 없으면 null
     */
    private static String linkAddress(Element e, String base) {
        String href = e.getAttribute("href");
        if (href == null || href.trim().length() == 0) {
            return null;
        }
        try {
            return base == null ? href.trim() : UrlUtil.resolve(base, href.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Applies optional owner-specific loading conditions only to stylesheet elements. / 스타일시트 요소에만 선택적 소유자별 로딩 조건을 적용한다.
     * @param loader common loader / 공통 로더
     * @param owner stylesheet element / 시트 요소
     * @return scoped or original loader / 범위 로더 또는 원래 로더 */
    private static StyleSheetLoader scope(StyleSheetLoader loader, Element owner) {
        return loader instanceof ContextualStyleSheetLoader ? ((ContextualStyleSheetLoader) loader).forOwner(owner) : loader;
    }

    /**
     * Applies the media, title and disabled attributes of the owning element to a sheet.
     * <p>
     * 소유 요소의 media, title, disabled 속성을 시트에 적용한다.
     *
     * @param s the sheet / 시트
     * @param e the owning element / 소유 요소
     */
    private static void decorate(StyleSheet s, Element e) {
        s.applyOwnerAttributes(e.getAttribute("media"), e.getAttribute("title"), e.getAttribute("disabled") != null);
        s.setOwner(e);
    }

    /**
     * Tells whether the <code>type</code> attribute allows CSS (missing, empty or <code>text/css</code>).
     * <p>
     * <code>type</code> 속성이 CSS 를 허용하는지(없음, 빈 값, <code>text/css</code>) 알려 준다.
     *
     * @param e the element / 요소
     * @return true if it is CSS / CSS 이면 true
     */
    private static boolean typeOk(Element e) {
        String t = e.getAttribute("type");
        return t == null || t.trim().length() == 0 || t.trim().equalsIgnoreCase("text/css");
    }

    /**
     * Tells whether a link element is a persistent or preferred style sheet link (<code>rel</code> contains <code>stylesheet</code> but not
     * <code>alternate</code>).
     * <p>
     * link 요소가 고정 또는 선호 스타일 시트 링크인지(<code>rel</code> 에 <code>stylesheet</code> 가 있고 <code>alternate</code> 는 없음) 알려 준다.
     *
     * @param e the link element / link 요소
     * @return true for style sheet links / 스타일 시트 링크이면 true
     */
    private static boolean isStylesheetLink(Element e) {
        String rel = e.getAttribute("rel");
        if (rel == null) {
            return false;
        }
        String[] tokens = rel.toLowerCase().split("\\s+");
        boolean sheet = false;
        // rel 토큰을 훑어 stylesheet 와 alternate 가 있는지 본다.
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("stylesheet")) {
                sheet = true;
            }
            if (tokens[i].equals("alternate")) {
                return false;
            }
        }
        return sheet;
    }

    /**
     * Loads the sheet of a link element.
     * <p>
     * link 요소의 시트를 읽는다.
     *
     * @param e      the link element / link 요소
     * @param base   the document base URL / 문서 기준 URL
     * @param loader the loader or null / 로더 또는 null
     * @param diag   collector or null / 수집기 또는 null
     * @param quirks true to parse the sheet in quirks mode / 시트를 호환 모드로 파싱하면 true
     * @return the sheet or null if it cannot be loaded / 시트. 읽을 수 없으면 null
     */
    private static StyleSheet loadLink(Element e, String base, StyleSheetLoader loader, CssDiagnostics diag, boolean quirks) {
        String href = e.getAttribute("href");
        if (href == null || href.trim().length() == 0) {
            return null;
        }
        String abs;
        try {
            abs = base == null ? href.trim() : UrlUtil.resolve(base, href.trim());
        } catch (RuntimeException ex) {
            CssDiagnostics.report(diag, CssDiagnostics.INVALID, "<link href=" + href + ">", "the URL could not be resolved");
            return null;
        }
        if (loader == null) {
            CssDiagnostics.report(diag, CssDiagnostics.UNSUPPORTED, "<link href=" + href + ">", "no style sheet loader, link skipped");
            return null;
        }
        try {
            LoadedStyleSheet l = loader.load(abs);
            String finalUrl = l.getUrl() != null ? l.getUrl() : abs;
            StyleSheet s = StyleSheet.parse(l.getText(), finalUrl, StyleSheet.ORIGIN_AUTHOR, loader, diag, quirks);
            s.setHref(finalUrl);
            return s;
        } catch (IOException ex) {
            CssDiagnostics.report(diag, CssDiagnostics.INVALID, "<link href=" + href + ">", "could not be loaded: " + ex.getMessage());
            return null;
        }
    }
}
