package org.duckdns.hjow.swingwebpanel.core;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTargets;
import org.duckdns.hjow.swingwebpanel.core.frames.SandboxFlags;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;

/**
 * Chooses the browsing context that a link, a form or a script navigates (the rules for choosing a browsing context of HTML5 REC 2014-10-28
 * 5.1 with the sandboxing rules of 4.7.2) and starts that navigation through the same navigation policy as the navigation of the top-level
 * document. The keywords <code>_self</code>, <code>_parent</code> and <code>_top</code> select the current, the parent and the top-level
 * context; a plain name selects the frame of that name anywhere in the frame tree of the page. A name without a frame and
 * <code>_blank</code> would need a new window, which a component does not have: the current context is reused (the HTML Standard allows that
 * configuration), unless the sandbox of the document forbids popups, in which case nothing happens and the status text tells so.
 * <p>
 * 링크, 폼, 스크립트가 이동시키는 브라우징 문맥을 고르고(HTML5 REC 2014-10-28 5.1 의 브라우징 문맥 선택 규칙과 4.7.2 의 샌드박스 규칙) 그 이동을 최상위
 * 문서의 이동과 같은 이동 정책으로 시작한다. 키워드 <code>_self</code>, <code>_parent</code>, <code>_top</code> 은 현재, 부모, 최상위 문맥을 고르고,
 * 일반 이름은 페이지의 프레임 트리 어디서나 그 이름의 프레임을 고른다. 프레임이 없는 이름과 <code>_blank</code> 는 새 창이 필요하지만 컴포넌트에는 창이
 * 없다. 그래서 현재 문맥을 재사용한다(HTML 표준이 허용하는 설정). 문서의 샌드박스가 팝업을 금지하면 아무 일도 하지 않고 상태 문구로 알린다.
 */
final class FrameNavigator {

    /**
     * The choice of a navigation: the top-level document, a frame, or nothing.
     * <p>
     * 이동의 선택: 최상위 문서, 프레임, 또는 아무것도 아님.
     */
    static final class Choice {
        /** True if the top-level document is navigated. / 최상위 문서를 이동시키면 true. */
        final boolean top;
        /** The frame that is navigated, or null. / 이동시킬 프레임. 없으면 null. */
        final FrameContext frame;
        /** Why nothing is navigated, or null. / 아무것도 이동시키지 않는 이유. 없으면 null. */
        final String blocked;
        /** True if the target needs a new browsing context and the caller does not reuse the current one. / 대상이 새 브라우징 문맥을 필요로 하고 호출자가 현재 문맥을 재사용하지 않으면 true. */
        final boolean newContext;

        /**
         * Creates a choice.
         * <p>
         * 선택을 만든다.
         *
         * @param top     true for the top-level document / 최상위 문서이면 true
         * @param frame   the frame or null / 프레임 또는 null
         * @param blocked the reason or null / 이유 또는 null
         */
        Choice(boolean top, FrameContext frame, String blocked) {
            this(top, frame, blocked, false);
        }

        /**
         * Creates a choice.
         * <p>
         * 선택을 만든다.
         *
         * @param top        true for the top-level document / 최상위 문서이면 true
         * @param frame      the frame or null / 프레임 또는 null
         * @param blocked    the reason or null / 이유 또는 null
         * @param newContext true if a new context would be needed / 새 문맥이 필요하면 true
         */
        Choice(boolean top, FrameContext frame, String blocked, boolean newContext) {
            this.top = top;
            this.frame = frame;
            this.blocked = blocked;
            this.newContext = newContext;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FrameNavigator() {
    }

    /**
     * Chooses the browsing context for a target name.
     * <p>
     * 대상 이름으로 브라우징 문맥을 고른다.
     *
     * @param scope  the frame set of the top-level document (the frames that names are looked up in), or null if the page has none / 최상위 문서의 프레임 집합(이름을
     *               찾는 프레임들). 없으면 null
     * @param source the frame that contains the document that navigates, or null for the top-level document / 이동을 요청한 문서를 포함한 프레임. 최상위 문서이면 null
     * @param name   the target name, null or empty for the current context / 대상 이름. 현재 문맥이면 null 이나 빈 문자열
     * @return the choice / 선택
     */
    static Choice choose(FrameSet scope, FrameContext source, String name) {
        return choose(scope, source, name, true);
    }

    /**
     * Chooses the browsing context for a target name.
     * <p>
     * 대상 이름으로 브라우징 문맥을 고른다.
     *
     * @param scope  the frame set of the top-level document, or null / 최상위 문서의 프레임 집합. 없으면 null
     * @param source the frame that contains the document that navigates, or null for the top-level document / 이동을 요청한 문서를 포함한 프레임. 최상위 문서이면 null
     * @param name   the target name, null or empty for the current context / 대상 이름. 현재 문맥이면 null 이나 빈 문자열
     * @param reuse  true to reuse the current context for a name that needs a new one (links and forms), false to report that a new one
     *               would be needed (<code>window.open</code>) / 새 문맥이 필요한 이름에 현재 문맥을 재사용하면(링크, 폼) true, 새 문맥이 필요하다고 알리려면
     *               (<code>window.open</code>) false
     * @return the choice / 선택
     */
    static Choice choose(FrameSet scope, FrameContext source, String name, boolean reuse) {
        FrameContext chosen = null;
        boolean top = false;
        int kind = FrameTargets.classify(name);
        // 키워드나 이름에 따라 문맥을 고른다.
        if (kind == FrameTargets.SELF) {
            top = source == null;
            chosen = source;
        } else if (kind == FrameTargets.PARENT) {
            FrameContext parent = source == null ? null : source.owner.owner;
            top = parent == null;
            chosen = parent;
        } else if (kind == FrameTargets.TOP) {
            top = true;
        } else {
            FrameContext named = kind == FrameTargets.NAMED && scope != null ? find(scope, name) : null;
            if (named != null) {
                chosen = named;
            } else {
                // 새 문맥이 필요한 이름은 새 창이 없으므로 현재 문맥을 쓴다. 샌드박스가 팝업을 막았으면 아무것도 하지 않는다.
                if (source != null && !source.flags().allowsPopups()) {
                    return new Choice(false, null, "the sandbox of the frame does not allow it to open new browsing contexts");
                }
                if (!reuse) {
                    return new Choice(false, null, null, true);
                }
                top = source == null;
                chosen = source;
            }
        }
        // 샌드박스 문맥은 자기 자신과 자손만 이동시킬 수 있고, 최상위는 allow-top-navigation 이 있어야 한다.
        if (source != null && source.flags().isSandboxed()) {
            SandboxFlags flags = source.flags();
            boolean allowed = top ? flags.allowsTopNavigation() : chosen == source || isDescendant(chosen, source);
            if (!allowed) {
                return new Choice(false, null, top ? "the sandbox of the frame does not allow it to navigate the top-level page"
                        : "the sandbox of the frame only allows it to navigate itself and its own frames");
            }
        }
        return new Choice(top, top ? null : chosen, null);
    }

    /**
     * Finds the frame with a name in the frame tree, in document order and depth first.
     * <p>
     * 프레임 트리에서 이름이 같은 프레임을 문서 순서로 깊이 우선으로 찾는다.
     *
     * @param scope the frame set to start with / 시작할 프레임 집합
     * @param name  the name / 이름
     * @return the frame or null / 프레임 또는 null
     */
    static FrameContext find(FrameSet scope, String name) {
        List<FrameContext> list = scope.contexts();
        // 이 집합의 프레임을 차례로 보며 이름을 비교하고, 그 안의 프레임도 찾는다.
        for (int i = 0; i < list.size(); i++) {
            FrameContext c = list.get(i);
            if (name.equals(c.name())) {
                return c;
            }
            HtmlPageDocument page = c.page();
            FrameSet inner = page == null ? null : page.frames();
            FrameContext found = inner == null ? null : find(inner, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Tells whether a frame is a descendant of another frame.
     * <p>
     * 프레임이 다른 프레임의 자손인지 알려 준다.
     *
     * @param frame    the candidate / 후보
     * @param ancestor the possible ancestor / 조상일 수 있는 프레임
     * @return true if it is a descendant / 자손이면 true
     */
    static boolean isDescendant(FrameContext frame, FrameContext ancestor) {
        FrameContext c = frame == null ? null : frame.owner.owner;
        // 부모를 따라 올라가며 조상과 같은지 비교한다.
        while (c != null) {
            if (c == ancestor) {
                return true;
            }
            c = c.owner.owner;
        }
        return false;
    }

    /**
     * Returns the <code>target</code> of a link: its own attribute, or the one of the first <code>base</code> element of its document.
     * <p>
     * 링크의 <code>target</code> 을 반환한다: 자기 속성, 없으면 문서의 첫 <code>base</code> 요소의 것.
     *
     * @param link the link element / 링크 요소
     * @return the target name, empty if none / 대상 이름. 없으면 빈 문자열
     */
    static String targetOf(Element link) {
        String own = link.getAttribute("target");
        if (own != null && own.length() > 0) {
            return own;
        }
        List<Element> bases = link.getOwnerDocument() == null ? null : link.getOwnerDocument().getElementsByTagName("base").toList();
        // 문서의 base 요소 중 target 이 있는 첫 요소의 값을 쓴다.
        for (int i = 0; bases != null && i < bases.size(); i++) {
            String t = bases.get(i).getAttribute("target");
            if (t != null && t.length() > 0) {
                return t;
            }
        }
        return "";
    }

    /**
     * Follows a link: navigates the browsing context that its target selects, through the navigation policy.
     * <p>
     * 링크를 따라간다: 대상이 고르는 브라우징 문맥을 이동 정책을 거쳐 이동시킨다.
     *
     * @param session the component / 컴포넌트
     * @param top     the top-level document / 최상위 문서
     * @param source  the frame of the document that contains the link, or null / 링크를 포함한 문서의 프레임. 없으면 null
     * @param link    the link element / 링크 요소
     * @param href    the absolute address / 절대 주소
     */
    static void followLink(BrowserSession session, HtmlPageDocument top, FrameContext source, Element link, String href) {
        navigate(session, top, source, targetOf(link), href);
    }

    /**
     * Navigates a context that a target name selects, through the navigation policy of the component.
     * <p>
     * 대상 이름이 고르는 문맥을 컴포넌트의 이동 정책을 거쳐 이동시킨다.
     *
     * @param session the component / 컴포넌트
     * @param top     the top-level document / 최상위 문서
     * @param source  the frame of the document that navigates, or null / 이동을 요청한 문서의 프레임. 없으면 null
     * @param name    the target name / 대상 이름
     * @param href    the absolute address / 절대 주소
     */
    static void navigate(BrowserSession session, HtmlPageDocument top, FrameContext source, String name, String href) {
        String from = source == null ? session.getPage() : source.sourceUrl();
        if (!NavigationPolicy.allowed(from, href)) {
            return;
        }
        Choice choice = choose(top == null ? null : top.frames(), source, name);
        if (choice.blocked != null) {
            session.setStatusMessage("Navigation blocked: " + choice.blocked);
        } else if (choice.top) {
            session.navigateFromPage(href, false);
        } else if (choice.frame != null) {
            choice.frame.scriptNavigate(href, false);
        }
    }

    /**
     * Tells whether the script that is running may navigate a context: the sandbox of the browsing context of the script (the context of
     * the document that the outermost running task belongs to, the "entry" of the standard) must allow it. Without a running script (the
     * host navigates) everything is allowed.
     * <p>
     * 지금 실행 중인 스크립트가 문맥을 이동시킬 수 있는지 알려 준다: 스크립트의 브라우징 문맥(가장 바깥에서 실행 중인 작업이 속한 문서의 문맥, 표준의
     * "entry")의 샌드박스가 허용해야 한다. 실행 중인 스크립트가 없으면(호스트가 이동시킴) 모두 허용한다.
     *
     * @param target the context that would be navigated / 이동시킬 문맥
     * @return true if allowed / 허용이면 true
     */
    static boolean permitted(BrowsingContext target) {
        BrowsingContext entry = PageScripts.entryContext();
        return entry == null || entry == target || entry.mayNavigate(target);
    }

    /**
     * Opens a window for a script (<code>window.open</code>): the target selects an existing browsing context (see {@link #choose}), which
     * navigates to the address when there is one. There are no new windows, so a target that needs one gives no window.
     * <p>
     * 스크립트를 위해 창을 연다(<code>window.open</code>): 대상이 기존 브라우징 문맥을 고르고({@link #choose} 참조) 주소가 있으면 그 문맥이 이동한다.
     * 새 창은 없으므로 새 창이 필요한 대상은 창을 주지 않는다.
     *
     * @param session the component / 컴포넌트
     * @param scope   the frame set of the top-level document, or null / 최상위 문서의 프레임 집합. 없으면 null
     * @param source  the frame of the document that calls, or null for the top-level document / 호출한 문서의 프레임. 최상위 문서이면 null
     * @param top     the context of the top-level document / 최상위 문서의 문맥
     * @param name    the target name / 대상 이름
     * @param href    the absolute address, or an empty string / 절대 주소. 없으면 빈 문자열
     * @return the context that the target selected, or null / 대상이 고른 문맥. 없으면 null
     */
    static BrowsingContext open(BrowserSession session, FrameSet scope, FrameContext source, BrowsingContext top, String name, String href) {
        Choice choice = choose(scope, source, name, false);
        if (choice.blocked != null) {
            session.setStatusMessage("Window blocked: " + choice.blocked);
            return null;
        }
        if (choice.newContext) {
            return null;
        }
        BrowsingContext target = choice.top ? top : choice.frame;
        if (target == null) {
            return null;
        }
        // 주소가 있으면 이동 정책을 거쳐 선택한 문맥을 이동시킨다. 정책이 막은 주소는 창을 주지 않는다.
        if (href.length() > 0) {
            String from = source == null ? session.getPage() : source.sourceUrl();
            if (!NavigationPolicy.allowed(from, href)) {
                return null;
            }
            target.navigate(href, false);
        }
        return target;
    }

    /**
     * Creates the receiver of the forms of a document: the form is sent to the browsing context that its <code>target</code> selects, and
     * not at all when the sandbox of the document forbids forms.
     * <p>
     * 문서의 폼을 받는 쪽을 만든다: 폼은 <code>target</code> 이 고르는 브라우징 문맥으로 보내지며, 문서의 샌드박스가 폼을 금지하면 보내지 않는다.
     *
     * @param session      the component / 컴포넌트
     * @param presentation the presentation state of the document that contains the forms / 폼을 포함한 문서의 표현 상태
     * @param source       the frame of that document or null / 그 문서의 프레임. 없으면 null
     * @param toTop        the receiver of the component for forms that navigate the top-level document, or null for none / 최상위 문서를 이동시키는 폼을 위한
     *                     컴포넌트의 수신자. 없으면 null
     * @return the receiver / 수신자
     */
    static FormSubmitSink formSink(final BrowserSession session, final PagePresentation presentation, final FrameContext source,
            final FormSubmitSink toTop) {
        return new FormSubmitSink() {
            /**
             * Sends the form to the context that it targets.
             * <p>
             * 폼을 대상 문맥으로 보낸다.
             *
             * @param submission the submission / 제출 정보
             */
            public void submit(FormSubmission submission) {
                if (submission == null) {
                    return;
                }
                if (source != null && !source.flags().allowsForms()) {
                    session.setStatusMessage("Form blocked: the sandbox of the frame does not allow forms");
                    return;
                }
                FrameSet own = presentation.frameSet();
                FrameSet scope = own == null ? null : own.rootSet();
                Choice choice = choose(scope, source, submission.getTarget());
                if (choice.blocked != null) {
                    session.setStatusMessage("Form blocked: " + choice.blocked);
                } else if (choice.top || scope == null && source == null) {
                    if (toTop != null) {
                        toTop.submit(submission);
                    }
                } else if (choice.frame != null) {
                    choice.frame.submitForm(submission);
                }
            }
        };
    }
}
