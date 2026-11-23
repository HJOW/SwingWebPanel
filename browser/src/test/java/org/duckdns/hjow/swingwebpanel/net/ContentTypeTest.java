package org.duckdns.hjow.swingwebpanel.net;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ContentType}, {@link MimeTypes} and {@link CancelToken}.
 * <p>
 * {@link ContentType}, {@link MimeTypes}, {@link CancelToken} 시험.
 */
public class ContentTypeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContentTypeTest() {
    }

    /**
     * Parameters, case, quotes and whitespace are handled.
     * <p>
     * 매개변수, 대소문자, 따옴표, 공백을 처리한다.
     */
    public void testParse() {
        ContentType t = ContentType.parse("TEXT/HTML ; Charset=\"UTF-8\" ; x=y");
        Check.equal("text/html", t.getMime(), "mime lower-cased and trimmed");
        Check.equal("UTF-8", t.getCharset(), "quoted charset");
        Check.equal("euc-kr", ContentType.parse("text/plain;charset=euc-kr").getCharset(), "unquoted charset");
        Check.isNull(ContentType.parse("text/plain").getCharset(), "no charset");
        Check.equal("", ContentType.parse(null).getMime(), "null");
        Check.equal("", ContentType.parse("garbage").getMime(), "not a media type");
        Check.equal("", ContentType.parse("/html").getMime(), "missing type");
        Check.equal("text/x", ContentType.parse("text/x; a=\"b;c\"; charset=iso-8859-1").getMime(), "semicolon inside quotes");
        Check.equal("iso-8859-1", ContentType.parse("text/x; a=\"b;c\"; charset=iso-8859-1").getCharset(), "parameter after quoted semicolon");
    }

    /**
     * Media types are guessed from names.
     * <p>
     * 이름으로 미디어 타입을 추정한다.
     */
    public void testMimeTypes() {
        Check.equal("text/html", MimeTypes.fromFileName("/a/b/index.HTML"), "html upper case");
        Check.equal("image/png", MimeTypes.fromFileName("x.png?v=1#f"), "query and fragment ignored");
        Check.equal("audio/mpeg", MimeTypes.fromFileName("song.mp3"), "mp3");
        Check.equal(MimeTypes.OCTET_STREAM, MimeTypes.fromFileName("README"), "no extension");
        Check.equal(MimeTypes.OCTET_STREAM, MimeTypes.fromFileName("file."), "trailing dot");
        Check.equal(MimeTypes.OCTET_STREAM, MimeTypes.fromFileName("a.unknownext"), "unknown extension");
        Check.equal("text/markdown", MimeTypes.fromFileName("note.md"), "markdown");
    }

    /**
     * Cancelling runs hooks once; late hooks run immediately.
     * <p>
     * 취소하면 훅이 한 번 실행되고, 늦게 등록한 훅은 즉시 실행된다.
     */
    public void testCancelToken() {
        final int[] calls = new int[1];
        CancelToken t = new CancelToken();
        CancelToken.Hook h = new CancelToken.Hook() {
            /**
             * Counts calls.
             * <p>
             * 호출 수를 센다.
             */
            public void onCancel() {
                calls[0]++;
            }
        };
        t.addHook(h);
        Check.isFalse(t.isCancelled(), "not cancelled yet");
        t.cancel();
        t.cancel();
        Check.equal(1, calls[0], "hook runs once");
        Check.isTrue(t.isCancelled(), "cancelled");
        t.addHook(h);
        Check.equal(2, calls[0], "a late hook runs immediately");
        CancelToken u = new CancelToken();
        u.addHook(h);
        u.removeHook(h);
        u.cancel();
        Check.equal(2, calls[0], "a removed hook is not run");
    }
}
