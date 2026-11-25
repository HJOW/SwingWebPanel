package org.duckdns.hjow.swingwebpanel.net;

/**
 * Why a resource is requested. The purpose selects the default <code>Accept</code> header and the cross-origin rules of
 * {@link OriginPolicy}: cross-origin images, audio, style sheets and scripts may be used for display or execution
 * ("no-cors") but their bodies stay hidden from scripts, while fonts and XHR/fetch need explicit CORS permission.
 * <p>
 * 리소스를 요청하는 목적. 목적에 따라 기본 <code>Accept</code> 헤더와 {@link OriginPolicy} 의 교차 오리진 규칙이 정해진다.
 * 교차 오리진 이미지, 오디오, 스타일시트, 스크립트는 표시나 실행에는 쓸 수 있지만("no-cors") 본문은 스크립트에 숨기고,
 * 글꼴과 XHR/fetch 는 명시적인 CORS 허용이 필요하다.
 */
public enum RequestPurpose {
    /**
     * Top-level document navigation.
     * <p>
     * 최상위 문서 이동.
     */
    DOCUMENT,
    /**
     * A nested browsing context (iframe, frame).
     * <p>
     * 중첩된 브라우징 컨텍스트(iframe, frame).
     */
    FRAME,
    /**
     * A script file.
     * <p>
     * 스크립트 파일.
     */
    SCRIPT,
    /**
     * A module script or a module that a module imports: the fetch uses CORS mode across origins (the response must grant access) and
     * the response must carry the MIME type for the module being requested.
     * <p>
     * 모듈 스크립트 또는 모듈이 import 하는 모듈. 교차 오리진 가져오기는 CORS 모드이며 응답은 요청한 모듈의 MIME 타입이어야 한다.
     */
    MODULE,
    /** Same-origin entry module of a worker. / 동일 오리진에서 가져오는 worker 진입 모듈. */
    WORKER_MODULE,
    /**
     * A style sheet.
     * <p>
     * 스타일시트.
     */
    STYLESHEET,
    /**
     * An image.
     * <p>
     * 이미지.
     */
    IMAGE,
    /**
     * An image with a <code>crossorigin</code> attribute: the fetch uses CORS mode across origins (the request carries <code>Origin</code> and the
     * response must grant access), which is what lets a canvas read the pixels of a cross-origin image.
     * <p>
     * <code>crossorigin</code> 속성이 있는 이미지: 교차 오리진 가져오기는 CORS 모드이며(요청이 <code>Origin</code> 을 싣고 응답이 접근을 허용해야 한다) 캔버스가
     * 교차 오리진 이미지의 화소를 읽을 수 있게 하는 것이 이것이다.
     */
    IMAGE_CORS,
    /**
     * Audio media.
     * <p>
     * 오디오 미디어.
     */
    AUDIO,
    /**
     * A web font (requires CORS across origins).
     * <p>
     * 웹 글꼴(교차 오리진은 CORS 필요).
     */
    FONT,
    /**
     * XMLHttpRequest or fetch (CORS mode).
     * <p>
     * XMLHttpRequest 또는 fetch(CORS 모드).
     */
    XHR,
    /**
     * A file download.
     * <p>
     * 파일 다운로드.
     */
    DOWNLOAD,
    /**
     * Anything else.
     * <p>
     * 그 밖의 목적.
     */
    OTHER
}
