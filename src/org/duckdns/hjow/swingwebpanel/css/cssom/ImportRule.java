package org.duckdns.hjow.swingwebpanel.css.cssom;

import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * An <code>@import</code> rule: the URL, the media queries and the style sheet that was loaded for it (null if the sheet could not be loaded, was
 * part of an import cycle or exceeded the import depth limit, or has not arrived yet). The imported sheet applies in the position of the rule, before the
 * rules that follow. The imported sheet knows this rule and the sheet that contains it (the CSSOM <code>ownerRule</code> and
 * <code>parentStyleSheet</code>).
 * <p>
 * <code>@import</code> 규칙: URL, 미디어 질의, 그에 대해 읽은 스타일 시트(읽지 못했거나 가져오기 순환이거나 가져오기 깊이 제한을 넘었거나 아직 도착하지 않았으면 null). 가져온 시트는
 * 규칙의 위치에서 뒤따르는 규칙보다 먼저 적용된다. 가져온 시트는 이 규칙과 이 규칙을 담은 시트를 안다(CSSOM <code>ownerRule</code>, <code>parentStyleSheet</code>).
 */
public final class ImportRule extends CssRule {

    /** The URL as written. / 쓰인 그대로의 URL. */
    private final String href;

    /** The absolute URL or null. / 절대 URL 또는 null. */
    private final String resolvedUrl;

    /** The media queries of the import. / 가져오기의 미디어 질의. */
    private MediaQueryList media;

    /** The imported sheet or null. / 가져온 시트 또는 null. */
    private StyleSheet sheet;

    /** The layer of <code>layer(name)</code>, "" for an anonymous layer, or null. <code>layer(name)</code> 의 레이어. 익명 레이어이면 "". 없으면 null. */
    private String layer;

    /** The media list object of the CSSOM, created when it is first asked for. / CSSOM 의 미디어 목록 객체. 처음 요청할 때 만든다. */
    private MediaList mediaList;

    /**
     * Creates an import rule.
     * <p>
     * 가져오기 규칙을 만든다.
     *
     * @param href        the URL as written / 쓰인 그대로의 URL
     * @param resolvedUrl the absolute URL or null / 절대 URL 또는 null
     * @param media       media queries / 미디어 질의
     * @param sheet       imported sheet or null / 가져온 시트 또는 null
     */
    public ImportRule(String href, String resolvedUrl, MediaQueryList media, StyleSheet sheet) {
        this.href = href;
        this.resolvedUrl = resolvedUrl;
        this.media = media;
        this.sheet = sheet;
    }

    /**
     * Returns the URL as written.
     * <p>
     * 쓰인 그대로의 URL 을 반환한다.
     *
     * @return the URL / URL
     */
    public String getHref() {
        return href;
    }

    /**
     * Returns the absolute URL.
     * <p>
     * 절대 URL 을 반환한다.
     *
     * @return the URL or null / URL 또는 null
     */
    public String getResolvedUrl() {
        return resolvedUrl;
    }

    /**
     * Returns the URL that the CSSOM <code>href</code> reports: the absolute URL if the address could be resolved, otherwise the URL as written.
     * <p>
     * CSSOM <code>href</code> 가 알려 주는 URL 을 반환한다: 주소를 해석할 수 있으면 절대 URL, 아니면 쓰인 그대로의 URL.
     *
     * @return the URL / URL
     */
    public String getResolvedHref() {
        return resolvedUrl != null ? resolvedUrl : href;
    }

    /**
     * Returns the media queries.
     * <p>
     * 미디어 질의를 반환한다.
     *
     * @return the list / 목록
     */
    public MediaQueryList getMedia() {
        return media;
    }

    /**
     * Replaces the media queries of the import (and of the imported sheet) and tells the sheet.
     * <p>
     * 가져오기(와 가져온 시트)의 미디어 질의를 바꾸고 시트에 알린다.
     *
     * @param m the list / 목록
     */
    public void setMedia(MediaQueryList m) {
        this.media = m;
        if (sheet != null) {
            sheet.setMedia(m);
        }
        changed();
    }

    /**
     * Returns the media list object of the CSSOM (<code>media</code>), the same object every time.
     * <p>
     * CSSOM 미디어 목록 객체(<code>media</code>)를 반환한다. 매번 같은 객체.
     *
     * @return the media list / 미디어 목록
     */
    public MediaList getMediaList() {
        if (mediaList == null) {
            mediaList = new MediaList(this);
        }
        return mediaList;
    }

    /**
     * Returns the imported sheet.
     * <p>
     * 가져온 시트를 반환한다.
     *
     * @return the sheet or null / 시트 또는 null
     */
    public StyleSheet getStyleSheet() {
        return sheet;
    }

    /**
     * Gives the rule the sheet that was loaded for it later (the resource had not arrived when the sheet was parsed).
     * <p>
     * 나중에 읽은 시트를 규칙에 준다(시트를 파싱할 때는 리소스가 아직 도착하지 않았었다).
     *
     * @param s the sheet / 시트
     */
    void setStyleSheet(StyleSheet s) {
        this.sheet = s;
        if (s != null) {
            s.setImporter(getParentStyleSheet(), this);
        }
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #IMPORT_RULE} / IMPORT_RULE
     */
    public int getType() {
        return IMPORT_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        String m = media.toCssText();
        return "@import url(" + CssEscape.string(href) + ")" + (m.equals("all") ? "" : " " + m) + ";";
    }

    /**
     * Sets the layer that the imported sheet is put into (<code>@import url(a.css) layer(name)</code>).
     * <p>
     * 가져온 시트를 넣을 레이어를 정한다(<code>@import url(a.css) layer(name)</code>).
     *
     * @param name the layer name, "" for the anonymous layer of <code>layer</code> without a name, or null for no layer / 레이어 이름. 이름 없는 <code>layer</code> 의 익명 레이어이면 "". 레이어가 없으면 null
     */
    public void setLayer(String name) {
        this.layer = name;
    }

    /**
     * Returns the layer that the imported sheet is put into.
     * <p>
     * 가져온 시트를 넣는 레이어를 반환한다.
     *
     * @return the layer name, "" for an anonymous layer, or null for no layer / 레이어 이름. 익명 레이어이면 "". 없으면 null
     */
    public String getLayer() {
        return layer;
    }

    /**
     * Returns the CSSOM <code>layerName</code>: null if the import is not in a layer, the empty string for an anonymous layer.
     * <p>
     * CSSOM <code>layerName</code> 을 반환한다: 레이어에 넣지 않으면 null, 이름 없는 레이어이면 빈 문자열.
     *
     * @return the layer name or null / 레이어 이름 또는 null
     */
    public String getLayerName() {
        return layer;
    }

    /**
     * Puts the rule and the sheet that it imported into a sheet.
     * <p>
     * 규칙과 그것이 가져온 시트를 시트에 넣는다.
     *
     * @param parentSheet the sheet that contains the rule / 규칙을 담은 시트
     * @param parent      the containing rule or null / 담은 규칙 또는 null
     */
    void attach(StyleSheet parentSheet, CssRule parent) {
        super.attach(parentSheet, parent);
        if (sheet != null) {
            sheet.setImporter(parentSheet, this);
        }
    }

    /**
     * Takes the rule out of its sheet: the imported sheet forgets its importer.
     * <p>
     * 규칙을 시트에서 뺀다: 가져온 시트는 자신을 가져온 곳을 잊는다.
     */
    void detach() {
        super.detach();
        if (sheet != null) {
            sheet.setImporter(null, null);
        }
    }
}
