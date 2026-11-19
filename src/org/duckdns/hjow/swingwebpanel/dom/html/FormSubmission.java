package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a form submission: what the host must request. For <code>GET</code> the entry list is already merged into the query of
 * {@link #getUrl()} (a fragment of the action is preserved) and there is no body; for <code>POST</code> the body and its content type
 * (including the boundary of <code>multipart/form-data</code>) are ready. Instances are immutable.
 * <p>
 * 폼 제출의 결과: 호스트가 요청해야 하는 것. <code>GET</code> 은 항목 목록이 이미 {@link #getUrl()} 의 쿼리에 합쳐져 있고(action 의 프래그먼트는 유지) 본문이 없다.
 * <code>POST</code> 는 본문과 콘텐츠 타입(<code>multipart/form-data</code> 의 경계 포함)이 준비되어 있다. 인스턴스는 불변이다.
 */
public final class FormSubmission {

    /** The method name of a GET navigation. / GET 이동의 메서드 이름. */
    public static final String GET = "GET";

    /** The method name of a POST navigation. / POST 이동의 메서드 이름. */
    public static final String POST = "POST";

    /** GET or POST. / GET 또는 POST. */
    private final String method;

    /** The method as the author wrote it: get, post or dialog. / 작성자가 쓴 메서드: get, post, dialog. */
    private final String originalMethod;

    /** The final URL. / 최종 URL. */
    private final String url;

    /** The content type of the body, null without a body. / 본문의 콘텐츠 타입. 본문이 없으면 null. */
    private final String contentType;

    /** The body bytes, null without a body. / 본문 바이트. 본문이 없으면 null. */
    private final byte[] body;

    /** The browsing context name (the target), empty if none. / 브라우징 컨텍스트 이름(target). 없으면 빈 문자열. */
    private final String target;

    /** The name of the encoding used for the entries. / 항목에 쓴 인코딩 이름. */
    private final String encoding;

    /** The entries that were encoded. / 인코딩한 항목. */
    private final List<FormEntry> entries;

    /**
     * Creates a submission.
     * <p>
     * 제출 정보를 만든다.
     *
     * @param method         GET or POST / GET 또는 POST
     * @param originalMethod get, post or dialog / get, post, dialog
     * @param url            the final URL / 최종 URL
     * @param contentType    the content type of the body or null / 본문의 콘텐츠 타입 또는 null
     * @param body           the body or null / 본문 또는 null
     * @param target         the target / target
     * @param encoding       the encoding name / 인코딩 이름
     * @param entries        the entries / 항목
     */
    FormSubmission(String method, String originalMethod, String url, String contentType, byte[] body, String target, String encoding,
            List<FormEntry> entries) {
        this.method = method;
        this.originalMethod = originalMethod;
        this.url = url;
        this.contentType = contentType;
        this.body = body == null ? null : body.clone();
        this.target = target == null ? "" : target;
        this.encoding = encoding;
        this.entries = Collections.unmodifiableList(new ArrayList<FormEntry>(entries));
    }

    /**
     * Returns the method of the navigation.
     * <p>
     * 이동의 메서드를 반환한다.
     *
     * @return {@link #GET} or {@link #POST} / {@link #GET} 또는 {@link #POST}
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the method as written in the markup.
     * <p>
     * 마크업에 쓴 메서드를 반환한다.
     *
     * @return get, post or dialog / get, post, dialog
     */
    public String getOriginalMethod() {
        return originalMethod;
    }

    /**
     * Returns the URL to request.
     * <p>
     * 요청할 URL 을 반환한다.
     *
     * @return the URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the content type of the body.
     * <p>
     * 본문의 콘텐츠 타입을 반환한다.
     *
     * @return the content type, null for GET / 콘텐츠 타입. GET 이면 null
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns a copy of the body.
     * <p>
     * 본문의 복사본을 반환한다.
     *
     * @return the body, null for GET / 본문. GET 이면 null
     */
    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    /**
     * Returns the target browsing context name.
     * <p>
     * 대상 브라우징 컨텍스트 이름을 반환한다.
     *
     * @return the target, empty if none / target. 없으면 빈 문자열
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returns the name of the encoding of the entries.
     * <p>
     * 항목의 인코딩 이름을 반환한다.
     *
     * @return the encoding name / 인코딩 이름
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns the entry list that was submitted.
     * <p>
     * 제출한 항목 목록을 반환한다.
     *
     * @return the unmodifiable entries / 수정할 수 없는 항목
     */
    public List<FormEntry> getEntries() {
        return entries;
    }
}
