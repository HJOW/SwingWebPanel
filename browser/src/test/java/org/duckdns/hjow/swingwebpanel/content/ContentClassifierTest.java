package org.duckdns.hjow.swingwebpanel.content;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ContentClassifier} (plan items P17-01, P01-17, P01-18): the table of media types, the policy for responses (missing
 * type, attachment, XML that is XHTML), the sniffing of unlabeled bodies and the argument check of <code>setContent</code>.
 * <p>
 * {@link ContentClassifier} 의 시험(계획 P17-01, P01-17, P01-18): 미디어 타입 표, 응답 정책(없는 타입, 첨부, XHTML 인 XML), 라벨 없는 본문의 판별,
 * <code>setContent</code> 인수 검사.
 */
public class ContentClassifierTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContentClassifierTest() {
    }

    /**
     * Converts ASCII text to bytes.
     * <p>
     * ASCII 텍스트를 바이트로 바꾼다.
     *
     * @param s the text / 텍스트
     * @return the bytes / 바이트
     */
    private static byte[] b(String s) {
        byte[] out = new byte[s.length()];
        // 글자를 하나씩 바이트로 옮긴다(0~255 범위만 쓴다).
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) s.charAt(i);
        }
        return out;
    }

    /**
     * Checks the kind of a media type.
     * <p>
     * 미디어 타입의 종류를 확인한다.
     *
     * @param mime the media type / 미디어 타입
     * @param kind the expected kind / 기대하는 종류
     */
    private static void kind(String mime, ContentKind kind) {
        Check.equal(kind, ContentClassifier.kindOfMime(mime), "kind of " + mime);
    }

    /**
     * The table of media types.
     * <p>
     * 미디어 타입 표.
     */
    public void testKindOfMime() {
        kind("text/html", ContentKind.HTML);
        kind("TEXT/HTML", ContentKind.HTML);
        kind("application/xhtml+xml", ContentKind.XHTML);
        kind("text/markdown", ContentKind.MARKDOWN);
        kind("text/x-markdown", ContentKind.MARKDOWN);
        kind("text/plain", ContentKind.PLAIN_TEXT);
        String[] text = {"text/css", "text/javascript", "text/xml", "text/csv-schema", "text/richtext", "text/anything", "application/javascript", "application/x-javascript",
            "application/ecmascript", "application/json", "application/xml", "application/atom+xml", "application/rss+xml", "application/vnd.api+json",
            "application/soap+xml", "application/ld+json"};
        // 텍스트로 보이는 타입이다.
        for (int i = 0; i < text.length; i++) {
            kind(text[i], ContentKind.PLAIN_TEXT);
        }
        String[] unsupportedText = {"text/calendar", "text/vcard", "text/x-vcard", "text/csv", "text/tab-separated-values", "text/rtf", "text/tsv",
            "text/x-csv", "text/directory", "text/ldif", "text/vnd.sun.j2me.app-descriptor", "TEXT/CSV", "text/x-ms-contact"};
        // 텍스트이지만 Chromium 이 텍스트로 보이지 않는 타입은 다운로드이다.
        for (int i = 0; i < unsupportedText.length; i++) {
            kind(unsupportedText[i], ContentKind.DOWNLOAD);
        }
        kind("image/png", ContentKind.IMAGE);
        kind("image/svg+xml", ContentKind.IMAGE);
        kind("image/x-unknown", ContentKind.IMAGE);
        kind("audio/mpeg", ContentKind.AUDIO);
        kind("audio/x-wav", ContentKind.AUDIO);
        kind("application/ogg", ContentKind.AUDIO);
        kind("video/mp4", ContentKind.VIDEO);
        kind("video/webm", ContentKind.VIDEO);
        String[] download = {"application/octet-stream", "application/pdf", "application/zip", "application/x-gzip", "application/java-archive", "font/woff2",
            "multipart/form-data", "message/rfc822", "application/x-www-form-urlencoded", "application/wasm", "model/gltf+json", "x-world/x-vrml"};
        // 페이지가 보여 줄 수 없는 타입은 다운로드이다.
        for (int i = 0; i < download.length; i++) {
            kind(download[i], ContentKind.DOWNLOAD);
        }
        kind(null, ContentKind.UNKNOWN);
        kind("", ContentKind.UNKNOWN);
        kind("garbage", ContentKind.UNKNOWN);
        kind("/x", ContentKind.UNKNOWN);
        Check.isTrue(ContentKind.HTML.isTextDocument() && ContentKind.XHTML.isTextDocument() && ContentKind.PLAIN_TEXT.isTextDocument()
                && ContentKind.MARKDOWN.isTextDocument(), "the four text kinds");
        Check.isFalse(ContentKind.IMAGE.isTextDocument() || ContentKind.DOWNLOAD.isTextDocument(), "other kinds are no text documents");
        Check.isTrue(ContentKind.VIDEO.isDisplayable() && ContentKind.IMAGE.isDisplayable(), "displayable");
        Check.isFalse(ContentKind.DOWNLOAD.isDisplayable() || ContentKind.UNKNOWN.isDisplayable(), "not displayable");
    }

    /**
     * A decision for a header value.
     * <p>
     * 헤더 값에 대한 결정.
     *
     * @param header     the Content-Type header / Content-Type 헤더
     * @param attachment true for an attachment / 첨부이면 true
     * @param head       the first bytes / 첫 바이트
     * @param kind       the expected kind / 기대하는 종류
     * @param mime       the expected media type / 기대하는 미디어 타입
     * @return the decision / 결정
     */
    private static ContentDecision decide(String header, boolean attachment, byte[] head, ContentKind kind, String mime) {
        ContentDecision d = ContentClassifier.classifyResponse(header, attachment, head);
        Check.equal(kind, d.getKind(), "kind for [" + header + "] attachment=" + attachment + ": " + d);
        Check.equal(mime, d.getMime(), "mime for [" + header + "]: " + d);
        Check.isTrue(d.getKind() != ContentKind.UNKNOWN, "a decision is never unknown");
        return d;
    }

    /**
     * Responses with a usable type: parameters, case and charset are handled, text types are shown as text, the display type is the
     * canonical one.
     * <p>
     * 쓸 수 있는 타입이 있는 응답: 매개변수, 대소문자, charset 을 처리하고, 텍스트 타입은 텍스트로 보이며, 표시 타입은 정규 타입이다.
     */
    public void testResponsesWithType() {
        ContentDecision html = decide("Text/HTML; Charset=EUC-KR", false, b("<html>"), ContentKind.HTML, "text/html");
        Check.equal("EUC-KR", html.getCharset(), "charset label");
        Check.isFalse(html.isSniffed(), "not sniffed");
        Check.isFalse(html.isDownload(), "no download");
        Check.equal(ContentDecision.Reason.NONE, html.getReason(), "no reason");
        decide("application/xhtml+xml;charset=utf-8", false, null, ContentKind.XHTML, "application/xhtml+xml");
        decide("text/markdown; variant=GFM", false, null, ContentKind.MARKDOWN, "text/markdown");
        ContentDecision css = decide("text/css", false, null, ContentKind.PLAIN_TEXT, "text/css");
        Check.equal("text/plain", css.getDisplayMime(), "shown as plain text");
        Check.equal("text/html", html.getDisplayMime(), "display type of html");
        Check.equal("text/markdown", decide("text/x-markdown", false, null, ContentKind.MARKDOWN, "text/x-markdown").getDisplayMime(), "display type of x-markdown");
        decide("image/png", false, null, ContentKind.IMAGE, "image/png");
        ContentDecision video = decide("video/mp4", false, null, ContentKind.VIDEO, "video/mp4");
        Check.equal("video/mp4", video.getDisplayMime(), "media types keep their type");
        ContentDecision pdf = decide("application/pdf", false, null, ContentKind.DOWNLOAD, "application/pdf");
        Check.isTrue(pdf.isDownload(), "download");
        Check.equal(ContentDecision.Reason.NOT_DISPLAYABLE, pdf.getReason(), "reason");
        decide("text/csv", false, null, ContentKind.DOWNLOAD, "text/csv");
    }

    /**
     * An attachment is always a download, with a type and without.
     * <p>
     * 첨부는 타입이 있든 없든 항상 다운로드이다.
     */
    public void testAttachment() {
        ContentDecision d = decide("text/html", true, b("<html>"), ContentKind.DOWNLOAD, "text/html");
        Check.equal(ContentDecision.Reason.ATTACHMENT, d.getReason(), "reason");
        decide("image/png", true, null, ContentKind.DOWNLOAD, "image/png");
        ContentDecision none = decide(null, true, b("<html>"), ContentKind.DOWNLOAD, "text/html");
        Check.isTrue(none.isSniffed(), "the sniffed type is reported");
        Check.equal(ContentDecision.Reason.ATTACHMENT, none.getReason(), "reason without a type");
    }

    /**
     * A missing or malformed Content-Type is not an error: the body decides.
     * <p>
     * 없거나 잘못된 Content-Type 은 오류가 아니다: 본문이 결정한다.
     */
    public void testMissingType() {
        ContentDecision d = decide(null, false, b("  \r\n<!DOCTYPE HTML><html>"), ContentKind.HTML, "text/html");
        Check.isTrue(d.isSniffed(), "sniffed");
        decide("", false, b("hello world"), ContentKind.PLAIN_TEXT, "text/plain");
        decide("garbage", false, b("<?xml version=\"1.0\"?><r/>"), ContentKind.PLAIN_TEXT, "text/xml");
        decide("text/html, text/plain", false, b("%PDF-1.4"), ContentKind.DOWNLOAD, "application/pdf");
        decide(null, false, null, ContentKind.PLAIN_TEXT, "text/plain");
        decide(null, false, new byte[0], ContentKind.PLAIN_TEXT, "text/plain");
        decide(null, false, b("GIF89a\u0001\u0000"), ContentKind.IMAGE, "image/gif");
        decide(null, false, b("ID3\u0004\u0000"), ContentKind.AUDIO, "audio/mpeg");
        decide(null, false, b("\u0000\u0001\u0002\u0003 binary"), ContentKind.DOWNLOAD, "application/octet-stream");
        Check.equal("EUC-KR", decide("bogus; charset=EUC-KR", false, b("<html>"), ContentKind.HTML, "text/html").getCharset() == null ? "EUC-KR" : "x",
                "an invalid header yields no charset (and the test string of a mismatch)");
    }

    /**
     * The sniffing table.
     * <p>
     * 판별 표.
     */
    public void testSniff() {
        String[][] cases = {
            {"<!doctype html><title>x", "text/html"}, {"<HTML>", "text/html"}, {" \t\r\n\f<html lang=en>", "text/html"}, {"<head>", "text/html"},
            {"<script>alert(1)</script>", "text/html"}, {"<iframe src=x>", "text/html"}, {"<h1>x", "text/html"}, {"<div>", "text/html"}, {"<font>", "text/html"},
            {"<table>", "text/html"}, {"<a href=x>", "text/html"}, {"<style>", "text/html"}, {"<title>", "text/html"}, {"<b>", "text/html"}, {"<body>", "text/html"},
            {"<br>", "text/html"}, {"<p>", "text/html"}, {"<!-- c -->", "text/html"}, {"ï»¿<html>", "text/html"},
            {"<htmlx>", "text/plain"}, {"<abc>", "text/plain"}, {"<html", "text/plain"}, {"<xml>", "text/plain"}, {"text <html>", "text/plain"},
            {"<?xml version=\"1.0\"?>", "text/xml"}, {"ï»¿<?xml?>", "text/xml"}, {"%PDF-1.7", "application/pdf"}, {"%!PS-Adobe-3.0", "application/postscript"},
            {"þÿ\u0000a", "text/plain"}, {"ÿþa\u0000", "text/plain"}, {"ï»¿plain", "text/plain"},
            {"\u0000\u0000\u0001\u0000x", "image/x-icon"}, {"GIF87a", "image/gif"}, {"GIF89a", "image/gif"}, {"\u0089PNG\r\n\u001a\n", "image/png"},
            {"ÿØÿxx", "image/jpeg"}, {"BM6\u0000\u0000\u0000", "image/bmp"}, {"RIFF\u0000\u0000\u0000\u0000WEBPVP8 ", "image/webp"},
            {"RIFF\u0000\u0000\u0000\u0000WAVEfmt ", "audio/wave"}, {"RIFF\u0000\u0000\u0000\u0000AVI LIST", "video/avi"}, {"FORM\u0000\u0000\u0000\u0000AIFF", "audio/aiff"},
            {".snd\u0000", "audio/basic"}, {"ID3\u0003", "audio/mpeg"}, {"OggS\u0000", "application/ogg"}, {"fLaC\u0000", "audio/flac"}, {"MThd\u0000\u0000\u0000\u0006", "audio/midi"},
            {"\u001aEß£", "video/webm"}, {"\u0000\u0000\u0000\u0018ftypmp42", "video/mp4"}, {"\u0000\u0000\u0000\u0018ftypM4A ", "audio/mp4"},
            {"\u0000\u0000\u0000 ftypavif", "image/avif"}, {"\u001f\u008b\u0008\u0000", "application/x-gzip"}, {"PK\u0003\u0004abc", "application/zip"},
            {"Rar!\u001a\u0007\u0000", "application/x-rar-compressed"}, {"just text\nwith lines\tand tabs", "text/plain"}, {"é latin 1 text", "text/plain"},
            {"text with NUL \u0000 inside", "application/octet-stream"}, {"text with \u0001 control", "application/octet-stream"}, {"\u001b[0m escape", "text/plain"},
            {"", "text/plain"}
        };
        // 표의 입력마다 판별 결과를 확인한다.
        for (int i = 0; i < cases.length; i++) {
            Check.equal(cases[i][1], ContentClassifier.sniff(b(cases[i][0])), "sniff of [" + cases[i][0].replace("\u0000", "\\0") + "]");
        }
        Check.equal("text/plain", ContentClassifier.sniff(null), "null");
        // 판별은 처음 1445 바이트만 본다. 그 뒤의 제어 바이트는 영향이 없다.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ContentClassifier.SNIFF_LENGTH; i++) {
            sb.append('a');
        }
        sb.append('\u0000');
        Check.equal("text/plain", ContentClassifier.sniff(b(sb.toString())), "only the first bytes are examined");
    }

    /**
     * XML types whose root element is <code>html</code> in the XHTML namespace are XHTML; other XML stays text.
     * <p>
     * 루트 요소가 XHTML 네임스페이스의 <code>html</code> 인 XML 타입은 XHTML 이고, 다른 XML 은 텍스트로 남는다.
     */
    public void testXmlThatIsXhtml() {
        String ns = "http://www.w3.org/1999/xhtml";
        String xhtml = "<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
                + "<html xmlns=\"" + ns + "\"><body/></html>";
        decide("application/xml", false, b(xhtml), ContentKind.XHTML, "application/xml");
        decide("text/xml; charset=utf-8", false, b(xhtml), ContentKind.XHTML, "text/xml");
        decide("application/xml", false, b("<h:html xmlns:h=\"" + ns + "\"/>"), ContentKind.XHTML, "application/xml");
        ContentDecision x = decide(null, false, b(xhtml), ContentKind.XHTML, "text/xml");
        Check.isTrue(x.isSniffed(), "sniffed as text/xml and then recognized as XHTML");
        Check.equal("application/xhtml+xml", x.getDisplayMime(), "display type");
        decide("application/xml", false, b("<?xml version=\"1.0\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"/>"), ContentKind.PLAIN_TEXT, "application/xml");
        decide("application/xml", false, b("<html/>"), ContentKind.PLAIN_TEXT, "application/xml");
        decide("application/xml", false, null, ContentKind.PLAIN_TEXT, "application/xml");
        decide("application/rss+xml", false, b(xhtml), ContentKind.PLAIN_TEXT, "application/rss+xml");
        decide("text/plain", false, b(xhtml), ContentKind.PLAIN_TEXT, "text/plain");
    }

    /**
     * The argument of <code>setContent</code>: four types, parameters and case are accepted, everything else is an error and no
     * download.
     * <p>
     * <code>setContent</code> 의 인수: 네 가지 타입이며 매개변수와 대소문자는 받아들이고 그 밖의 것은 오류이고 다운로드가 아니다.
     */
    public void testSetContent() {
        Check.equal(ContentKind.HTML, ContentClassifier.classifySetContent("text/html"), "html");
        Check.equal(ContentKind.HTML, ContentClassifier.classifySetContent("TEXT/HTML; charset=utf-8"), "html with parameter and case");
        Check.equal(ContentKind.PLAIN_TEXT, ContentClassifier.classifySetContent(" text/plain "), "plain with spaces");
        Check.equal(ContentKind.MARKDOWN, ContentClassifier.classifySetContent("text/markdown;variant=GFM"), "markdown");
        Check.equal(ContentKind.XHTML, ContentClassifier.classifySetContent("Application/XHTML+XML"), "xhtml");
        Check.equal("text/html", ContentClassifier.requireSetContentMime("Text/HTML;charset=x"), "normalized");
        Check.equal("application/xhtml+xml", ContentClassifier.requireSetContentMime("application/xhtml+xml; charset=\"x y\""), "normalized with a quoted parameter");
        String[] bad = {null, "", "   ", ";", "garbage", "text/css", "text/x-markdown", "image/png", "application/octet-stream", "application/xml", "text/", "*/*",
            "text/html, text/plain", "audio/wav"};
        // 지원하지 않는 인수마다 IllegalArgumentException 이어야 하고 isSetContentMime 은 false 이다.
        for (int i = 0; i < bad.length; i++) {
            final String arg = bad[i];
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Classifies an unsupported argument.
                 * <p>
                 * 지원하지 않는 인수를 분류한다.
                 *
                 * @throws Exception always the expected one / 기대하는 예외
                 */
                public void run() throws Exception {
                    ContentClassifier.classifySetContent(arg);
                }
            }, "rejected: [" + arg + "]");
            Check.isFalse(ContentClassifier.isSetContentMime(arg), "isSetContentMime false for [" + arg + "]");
        }
        Check.isTrue(ContentClassifier.isSetContentMime("text/plain"), "isSetContentMime true");
    }
}
