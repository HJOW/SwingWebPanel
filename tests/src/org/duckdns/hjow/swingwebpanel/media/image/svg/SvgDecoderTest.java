package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the SVG decoder as a whole (plan item P18-04; FEATURE_MATRIX IMG-07): the registry, the natural size rules (checked against
 * the sizes that Chrome reports for the same documents), exact pixels of simple documents, errors, limits, hostile documents, cancellation,
 * the ban on external access and drawing at other sizes.
 * <p>
 * SVG 디코더 전체에 대한 시험(계획 P18-04; FEATURE_MATRIX IMG-07): 등록부, 자연 크기 규칙(같은 문서에 대해 Chrome 이 보고하는 크기와 대조), 단순한
 * 문서의 정확한 픽셀, 오류, 제한, 악의적인 문서, 취소, 외부 접근 금지, 다른 크기로 그리기.
 */
public class SvgDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgDecoderTest() {
    }

    /**
     * The registry finds the SVG decoder by content, ignores a wrong MIME hint and lists the MIME type.
     * <p>
     * 등록부가 내용으로 SVG 디코더를 찾고, 잘못된 MIME 힌트를 무시하며, MIME 타입을 목록에 올린다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testRegistry() throws Exception {
        byte[] data = SvgTestKit.utf8(SvgTestKit.doc(10, 10, "<rect width=\"10\" height=\"10\" fill=\"#00f\"/>"));
        DecodedImage a = ImageDecoders.decode(data, "image/svg+xml", null);
        Check.equal(10, a.getWidth(), "width");
        Check.equal(0xFF0000FFL, SvgTestKit.px(a, 5, 5) & 0xFFFFFFFFL, "pixel");
        DecodedImage b = ImageDecoders.decode(data, "image/png", null);
        Check.equal(0xFF0000FFL, SvgTestKit.px(b, 5, 5) & 0xFFFFFFFFL, "a wrong MIME hint does not decide the format");
        DecodedImage c = ImageDecoders.decode(data, null, null);
        Check.equal(10, c.getHeight(), "no hint");
        Check.isTrue(ImageDecoders.isSupportedMime("image/svg+xml"), "supported MIME");
        Check.isTrue(ImageDecoders.isSupportedMime("IMAGE/SVG+XML; charset=utf-8"), "MIME with parameters and letter case");
        Check.contains(ImageDecoders.getAcceptHeader(), "image/svg+xml", "Accept header lists SVG");
        Check.isTrue(ImageDecoders.find(data) instanceof SvgDecoder, "find by the first bytes");
        Check.equal("svg", new SvgDecoder().getFormatName(), "format name");
        Check.isFalse(c.isAnimated(), "a still image");
    }

    /**
     * The signature check accepts every usual way to start an SVG file and rejects other data.
     * <p>
     * 시그니처 검사는 SVG 파일이 시작하는 일반적인 방식을 모두 받아들이고 다른 데이터는 거부한다.
     */
    public void testAccepts() {
        SvgDecoder d = new SvgDecoder();
        Check.isTrue(d.accepts(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")), "plain");
        Check.isTrue(d.accepts(SvgTestKit.utf8("\n\t <svg/>")), "white space before");
        Check.isTrue(d.accepts(SvgTestKit.utf8("<?xml version=\"1.0\"?>\n<!-- c --><svg/>")), "xml declaration and comment");
        Check.isTrue(d.accepts(SvgTestKit.utf8("<?xml version=\"1.0\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"x.dtd\"><svg/>")), "doctype");
        Check.isTrue(d.accepts(SvgTestKit.utf8("<!DOCTYPE svg [ <!ENTITY a \"b\"> ]><svg/>")), "doctype with internal subset");
        Check.isTrue(d.accepts(SvgTestKit.utf8("<svg:svg xmlns:svg=\"http://www.w3.org/2000/svg\"/>")), "prefixed root");
        Check.isTrue(d.accepts(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<', 's', 'v', 'g', '>' }), "UTF-8 byte order mark");
        Check.isTrue(d.accepts(new byte[] { (byte) 0xFF, (byte) 0xFE, '<', 0, 's', 0, 'v', 0, 'g', 0, '>', 0 }), "UTF-16LE byte order mark");
        Check.isFalse(d.accepts(SvgTestKit.utf8("<html><svg/></html>")), "html with an svg inside");
        Check.isFalse(d.accepts(SvgTestKit.utf8("<svgx/>")), "another element name");
        Check.isFalse(d.accepts(SvgTestKit.utf8("<?xml version=\"1.0\"?><feed/>")), "another xml document");
        Check.isFalse(d.accepts(new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 }), "png");
        Check.isFalse(d.accepts(SvgTestKit.utf8("hello")), "text");
        Check.isFalse(d.accepts(new byte[0]), "empty");
        Check.isFalse(d.accepts(null), "null");
    }

    /**
     * Compressed SVG (svgz) is recognized and decoded, and a decompression bomb is refused.
     * <p>
     * 압축된 SVG(svgz)를 알아보고 디코딩하며 압축 폭탄은 거부한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testGzip() throws Exception {
        byte[] raw = SvgTestKit.utf8(SvgTestKit.doc(8, 8, "<rect width=\"8\" height=\"8\" fill=\"#0f0\"/>"));
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(bo);
        gz.write(raw);
        gz.close();
        byte[] z = bo.toByteArray();
        Check.isTrue(new SvgDecoder().accepts(z), "svgz is recognized");
        DecodedImage img = ImageDecoders.decode(z, "image/svg+xml", null);
        Check.equal(0xFF00FF00L, SvgTestKit.px(img, 4, 4) & 0xFFFFFFFFL, "decoded pixel");
        // 풀면 매우 커지는 압축 데이터를 만든다.
        ByteArrayOutputStream big = new ByteArrayOutputStream();
        GZIPOutputStream bgz = new GZIPOutputStream(big);
        bgz.write(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"5\" height=\"5\"><!--"));
        byte[] zeros = new byte[1 << 20];
        Arrays.fill(zeros, (byte) 'a');
        // 1 MiB 블록을 여러 번 써 제한(여기서는 2 MiB)을 넘긴다.
        for (int i = 0; i < 4; i++) {
            bgz.write(zeros);
        }
        bgz.write(SvgTestKit.utf8("--></svg>"));
        bgz.close();
        final byte[] bomb = big.toByteArray();
        final DecodeLimits small = new DecodeLimits(1000000, 2 * 1024 * 1024, 10, 1000000);
        Check.throwsException(MediaException.class, new Block() {
            public void run() throws Exception {
                new SvgDecoder().decode(bomb, small);
            }
        }, "decompression bomb");
    }

    /**
     * Natural size rules for the root element, compared with the sizes that Chrome reports for the same documents.
     * <p>
     * 루트 요소의 자연 크기 규칙을 같은 문서에 대해 Chrome 이 보고한 크기와 대조한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNaturalSize() throws Exception {
        String[][] cases = {
                { "width=\"10.5\" height=\"20.2\"", "11", "20" },
                { "viewBox=\"0 0 24 24\"", "150", "150" },
                { "viewBox=\"0 0 100 50\"", "300", "150" },
                { "", "300", "150" },
                { "width=\"50\" viewBox=\"0 0 100 50\"", "50", "25" },
                { "width=\"50%\" height=\"20\" viewBox=\"0 0 100 50\"", "40", "20" },
                { "width=\"2cm\" height=\"1in\"", "76", "96" },
                { "width=\"60\"", "60", "150" },
                { "height=\"60\"", "300", "60" },
                { "width=\"1in\" height=\"3cm\" viewBox=\"0 0 100 50\"", "96", "113" },
                { "width=\"100%\" height=\"100%\" viewBox=\"0 0 100 50\"", "300", "150" },
                { "width=\"80\" height=\"80\"", "80", "80" },
                { "viewBox=\"0 0 50 100\"", "75", "150" },
                { "width=\"auto\" height=\"12\"", "300", "12" } };
        // Chrome 이 보고한 표를 하나씩 확인한다.
        for (int i = 0; i < cases.length; i++) {
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" " + cases[i][0] + "><rect width=\"5\" height=\"5\"/></svg>";
            DecodedImage img = SvgTestKit.decode(svg);
            Check.equal(Integer.parseInt(cases[i][1]), img.getWidth(), "width of " + cases[i][0]);
            Check.equal(Integer.parseInt(cases[i][2]), img.getHeight(), "height of " + cases[i][0]);
        }
        SvgImage doc = SvgImage.parse(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10.5\" height=\"20.2\"/>"), null);
        Check.near(10.5, doc.getNaturalWidth(), 1e-9, "unrounded width");
        Check.isTrue(doc.hasIntrinsicSize(), "the size is stated");
        Check.isFalse(SvgImage.parse(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\"/>"), null).hasIntrinsicSize(), "default size");
    }

    /**
     * Documents that are not SVG or not well formed fail with CORRUPT.
     * <p>
     * SVG 가 아니거나 형식이 틀린 문서는 CORRUPT 로 실패한다.
     */
    public void testCorruptDocuments() {
        String ns = "xmlns=\"http://www.w3.org/2000/svg\"";
        String[] bad = { "<svg " + ns + ">", "<svg " + ns + "></g>", "<svg " + ns + " a=\"1\" a=\"2\"/>", "<svg " + ns + "/>text",
                "<svg " + ns + "/><svg " + ns + "/>", "", "   ", "<svg " + ns + "><g></svg>", "<svg " + ns + ">&nbsp;</svg>",
                "<svg " + ns + ">&#xZZ;</svg>", "<svg " + ns + " a=1/>", "<svg " + ns + " a=\"<\"/>", "<svg " + ns + "><!-- x </svg>",
                "<svg " + ns + "><![CDATA[x</svg>", "<x:svg " + ns + "/>", "<svg " + ns + " width=\"0\" height=\"10\"/>",
                "<svg " + ns + " width=\"10\" height=\"0\"/>", "<svg width=\"10\" height=\"10\"/>",
                "<svg xmlns=\"http://example.com/other\" width=\"10\" height=\"10\"/>", "<html xmlns=\"http://www.w3.org/1999/xhtml\"/>",
                "<svg " + ns + " width=\"10\" height=\"10\">&#0;</svg>", "<svg " + ns + " width=\"10\" height=\"10\">&#xD800;</svg>",
                "<svg " + ns + ">&undefined;</svg>", "<svg " + ns + " a=\"&undefined;\"/>" };
        // 잘못된 문서마다 CORRUPT 로 실패하는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            SvgTestKit.fails(MediaException.Kind.CORRUPT, bad[i], "bad document " + i + ": " + bad[i]);
        }
    }

    /**
     * Limits: too large images, nesting, element count and entity expansion fail with TOO_LARGE.
     * <p>
     * 제한: 너무 큰 이미지, 중첩, 요소 수, 엔티티 확장은 TOO_LARGE 로 실패한다.
     */
    public void testLimits() {
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100000\" height=\"100000\"/>", "pixel limit");
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1e9\" height=\"1e9\"/>", "huge size");
        StringBuilder deep = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"5\" height=\"5\">");
        // 중첩 한도(256)를 넘는 깊이로 g 를 겹친다.
        for (int i = 0; i < 300; i++) {
            deep.append("<g>");
        }
        // 조건이 참인 동안 반복해서 처리한다.
        for (int i = 0; i < 300; i++) {
            deep.append("</g>");
        }
        deep.append("</svg>");
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE, deep.toString(), "nesting depth");
        StringBuilder many = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"5\" height=\"5\">");
        // 요소 수 한도(400000)를 넘는 빈 요소를 이어 붙인다.
        for (int i = 0; i < SvgXmlParser.MAX_ELEMENTS + 5; i++) {
            many.append("<g/>");
        }
        many.append("</svg>");
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE, many.toString(), "element count");
        // 엔티티를 단계마다 열 배로 키우는 정의(billion laughs)를 만든다.
        StringBuilder bomb = new StringBuilder("<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY a0 \"0123456789\">");
        for (int i = 1; i < 10; i++) {
            bomb.append("<!ENTITY a").append(i).append(" \"");
            // 조건이 참인 동안 반복해서 처리한다.
            for (int k = 0; k < 10; k++) {
                bomb.append("&a").append(i - 1).append(";");
            }
            bomb.append("\">");
        }
        bomb.append("]><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"5\" height=\"5\"><text>&a9;</text></svg>");
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE, bomb.toString(), "entity expansion bomb");
        SvgTestKit.fails(MediaException.Kind.TOO_LARGE,
                "<!DOCTYPE svg [<!ENTITY a \"&a;\">]><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"5\" height=\"5\"><text>&a;</text></svg>",
                "self referencing entity");
    }

    /**
     * An exponential <code>use</code> tree and a very deep reference chain must end with TOO_LARGE or finish quickly, never hang.
     * <p>
     * 지수적으로 커지는 <code>use</code> 트리와 매우 깊은 참조 연쇄는 TOO_LARGE 로 끝나거나 빨리 끝나야 하며 멈춰서는 안 된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testUseBombIsBounded() throws Exception {
        StringBuilder sb = new StringBuilder("<defs><rect id=\"u0\" width=\"1\" height=\"1\"/>");
        // 단계마다 앞 단계를 두 번 사용하는 정의를 40 단계 쌓는다(2의 40제곱 번 방문).
        for (int i = 1; i <= 40; i++) {
            sb.append("<g id=\"u").append(i).append("\"><use href=\"#u").append(i - 1).append("\"/><use href=\"#u").append(i - 1).append("\" x=\"1\"/></g>");
        }
        sb.append("</defs><use href=\"#u40\"/>");
        long start = System.currentTimeMillis();
        try {
            SvgTestKit.decode(SvgTestKit.doc(20, 20, sb.toString()));
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.TOO_LARGE, e.getKind(), "the budget of element visits stops the document: " + e.getMessage());
        }
        Check.isTrue(System.currentTimeMillis() - start < 20000, "the document must be stopped quickly");
    }

    /**
     * A thread interrupt cancels decoding with CANCELLED and keeps the interrupt flag.
     * <p>
     * 스레드 인터럽트는 디코딩을 CANCELLED 로 취소하고 인터럽트 플래그를 유지한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testCancellation() throws Exception {
        StringBuilder sb = new StringBuilder();
        // 방문 횟수가 많아 취소 확인 지점을 지나는 요소를 많이 만든다.
        for (int i = 0; i < 3000; i++) {
            sb.append("<rect width=\"1\" height=\"1\"/>");
        }
        final String svg = SvgTestKit.doc(10, 10, sb.toString());
        Thread.currentThread().interrupt();
        try {
            SvgTestKit.fails(MediaException.Kind.CANCELLED, svg, "interrupted");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is kept");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Nothing outside the document is ever read: no connection is made for DTDs, entities, images, style sheets, uses and filters.
     * <p>
     * 문서 밖의 것을 읽지 않는다: DTD, 엔티티, 이미지, 스타일시트, use, 필터에 대해 연결을 만들지 않는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNoExternalAccess() throws Exception {
        final ServerSocket server = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
        final AtomicInteger connections = new AtomicInteger();
        server.setSoTimeout(100);
        final boolean[] stop = { false };
        Thread t = new Thread(new Runnable() {
            public void run() {
                // 연결이 하나라도 오면 세어 둔다.
                while (!stop[0]) {
                    try {
                        Socket s = server.accept();
                        connections.incrementAndGet();
                        s.close();
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        });
        t.start();
        try {
            String base = "http://127.0.0.1:" + server.getLocalPort();
            String svg = "<?xml version=\"1.0\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"" + base + "/svg11.dtd\" [<!ENTITY ext SYSTEM \""
                    + base + "/ent.xml\">]>" + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"50\" height=\"50\">"
                    + "<style>@import url(" + base + "/c.css); @font-face { font-family: X; src: url(" + base + "/f.woff) } rect { fill: red }</style>"
                    + "<defs><filter id=\"f\"><feImage href=\"" + base + "/i.png\"/></filter><pattern id=\"p\" width=\"5\" height=\"5\">"
                    + "<image href=\"" + base + "/p.png\" width=\"5\" height=\"5\"/></pattern></defs>" + "<text>&ext;</text>"
                    + "<image href=\"" + base + "/a.png\" width=\"10\" height=\"10\"/><image xlink:href=\"file:///c:/windows/win.ini\" width=\"10\" height=\"10\"/>"
                    + "<use href=\"" + base + "/s.svg#a\"/><use xlink:href=\"classpath://x.svg#a\"/>"
                    + "<rect x=\"20\" width=\"10\" height=\"10\" filter=\"url(#f)\"/><rect y=\"20\" width=\"10\" height=\"10\" fill=\"url(#p)\"/>"
                    + "<rect x=\"30\" y=\"30\" width=\"10\" height=\"10\" style=\"fill:url(" + base + "/x.svg#g) blue; clip-path: url(" + base + "/x.svg#c)\"/>"
                    + "<script>alert(1)</script><foreignObject width=\"10\" height=\"10\"><body xmlns=\"http://www.w3.org/1999/xhtml\"><img src=\"" + base
                    + "/img.png\"/></body></foreignObject></svg>";
            DecodedImage img = SvgTestKit.decode(svg);
            Check.equal(50, img.getWidth(), "the document still decodes");
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Check.equal(0, connections.get(), "no connection must be made");
        } finally {
            stop[0] = true;
            server.close();
            t.join(2000);
        }
    }

    /**
     * Exact pixels of simple shapes, strokes, opacity and layering.
     * <p>
     * 단순한 도형, 선, 투명도, 겹침의 정확한 픽셀.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testExactPixels() throws Exception {
        DecodedImage img = SvgTestKit.decode(SvgTestKit.doc(20, 20, "<rect x=\"2\" y=\"2\" width=\"10\" height=\"10\" fill=\"#ff0000\"/>"
                + "<rect x=\"10\" y=\"10\" width=\"8\" height=\"8\" fill=\"#0000ff\" fill-opacity=\".5\"/>"
                + "<rect x=\"0\" y=\"15\" width=\"20\" height=\"5\" fill=\"none\" stroke=\"#00ff00\" stroke-width=\"2\"/>"));
        Check.equal(0xFFFF0000L, SvgTestKit.px(img, 5, 5) & 0xFFFFFFFFL, "opaque red inside");
        Check.equal(0L, SvgTestKit.px(img, 1, 1) & 0xFFFFFFFFL, "transparent outside");
        Check.equal(0L, SvgTestKit.px(img, 12, 2) & 0xFFFFFFFFL, "transparent just right of the rectangle");
        SvgTestKit.near(0x800000FF, SvgTestKit.px(img, 15, 12), 1, "half transparent blue where nothing is below");
        int overlap = SvgTestKit.px(img, 11, 11);
        Check.equal(255L, (overlap >>> 24) & 0xFFL, "overlap is opaque");
        SvgTestKit.near(0xFF7F0080, overlap, 1, "red under 50% blue");
        SvgTestKit.near(0xFF00FF00, SvgTestKit.px(img, 5, 15), 0, "stroke of width 2 around y=15 covers rows 14 and 15");
        SvgTestKit.near(0xFF00FF00, SvgTestKit.px(img, 5, 14), 0, "row 14 is stroke");
        Check.equal(0L, SvgTestKit.px(img, 5, 13) & 0xFFFFFFFFL, "row 13 is outside the stroke");
        DecodedImage grp = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<g opacity=\".5\"><rect width=\"10\" height=\"10\" fill=\"#fff\"/><rect width=\"5\" height=\"10\" fill=\"#000\"/></g>"));
        SvgTestKit.near(0x80000000, SvgTestKit.px(grp, 2, 5), 1, "group opacity composites the group once: black 50%");
        SvgTestKit.near(0x80FFFFFF, SvgTestKit.px(grp, 8, 5), 1, "white 50%");
    }

    /**
     * Linear and radial gradients: end colours, middle colour, spread.
     * <p>
     * 선형/방사 그라디언트: 끝 색, 가운데 색, 펼침.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testGradientPixels() throws Exception {
        DecodedImage img = SvgTestKit.decode(SvgTestKit.doc(100, 20, "<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#ff0000\"/>"
                + "<stop offset=\"1\" stop-color=\"#0000ff\"/></linearGradient></defs><rect width=\"100\" height=\"20\" fill=\"url(#g)\"/>"));
        SvgTestKit.near(0xFFFE0001, SvgTestKit.px(img, 0, 10), 3, "start is red");
        SvgTestKit.near(0xFF7F0080, SvgTestKit.px(img, 50, 10), 4, "middle is purple");
        SvgTestKit.near(0xFF0100FE, SvgTestKit.px(img, 99, 10), 3, "end is blue");
        DecodedImage rad = SvgTestKit.decode(SvgTestKit.doc(40, 40, "<defs><radialGradient id=\"g\"><stop offset=\"0\" stop-color=\"#ffffff\"/>"
                + "<stop offset=\"1\" stop-color=\"#000000\"/></radialGradient></defs><rect width=\"40\" height=\"40\" fill=\"url(#g)\"/>"));
        SvgTestKit.near(0xFFFFFFFF, SvgTestKit.px(rad, 20, 20), 12, "centre is white");
        SvgTestKit.near(0xFF000000, SvgTestKit.px(rad, 0, 0), 2, "the corner is beyond the radius: black");
        DecodedImage hard = SvgTestKit.decode(SvgTestKit.doc(100, 10, "<defs><linearGradient id=\"g\"><stop offset=\".5\" stop-color=\"#f00\"/>"
                + "<stop offset=\".5\" stop-color=\"#00f\"/></linearGradient></defs><rect width=\"100\" height=\"10\" fill=\"url(#g)\"/>"));
        SvgTestKit.near(0xFFFF0000, SvgTestKit.px(hard, 10, 5), 1, "hard stop: left red");
        SvgTestKit.near(0xFF0000FF, SvgTestKit.px(hard, 90, 5), 1, "hard stop: right blue");
        DecodedImage single = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<defs><linearGradient id=\"g\"><stop offset=\".2\" stop-color=\"#123456\"/></linearGradient></defs>"
                + "<rect width=\"10\" height=\"10\" fill=\"url(#g)\"/>"));
        SvgTestKit.near(0xFF123456, SvgTestKit.px(single, 5, 5), 0, "a gradient with one stop is a solid colour");
        DecodedImage none = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<defs><linearGradient id=\"g\"/></defs><rect width=\"10\" height=\"10\" fill=\"url(#g) red\"/>"));
        Check.equal(0L, SvgTestKit.px(none, 5, 5) & 0xFFFFFFFFL, "a gradient without stops paints nothing (the fallback is for missing references)");
    }

    /**
     * Embedded data URI images are drawn; every other address is ignored.
     * <p>
     * 포함된 data URI 이미지는 그리고 다른 모든 주소는 무시한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testImages() throws Exception {
        // 빨강 1x1 PNG(무압축 deflate 블록)를 base64 로 쓴다.
        String red = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";
        DecodedImage img = SvgTestKit.decode(SvgTestKit.doc(20, 10, "<image x=\"0\" y=\"0\" width=\"10\" height=\"10\" href=\"data:image/png;base64," + red
                + "\" style=\"image-rendering:pixelated\"/><image x=\"10\" y=\"0\" width=\"10\" height=\"10\" href=\"http://127.0.0.1:9/a.png\"/>"));
        SvgTestKit.near(0xFFFF0000, SvgTestKit.px(img, 5, 5), 0, "data uri image scaled to its box");
        Check.equal(0L, SvgTestKit.px(img, 15, 5) & 0xFFFFFFFFL, "external address is not loaded");
        DecodedImage nested = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<image width=\"10\" height=\"10\" href=\"data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='4' height='4'%3E%3Crect width='4' height='4' fill='%2300f'/%3E%3C/svg%3E\"/>"));
        SvgTestKit.near(0xFF0000FF, SvgTestKit.px(nested, 5, 5), 1, "an SVG inside a data URI, percent encoded");
        DecodedImage broken = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<image width=\"10\" height=\"10\" href=\"data:image/png;base64,AAAA\"/><rect width=\"2\" height=\"2\" fill=\"#0f0\"/>"));
        Check.equal(0xFF00FF00L, SvgTestKit.px(broken, 0, 0) & 0xFFFFFFFFL, "a broken embedded image is skipped and drawing goes on");
    }

    /**
     * SVG nested through data URIs is limited in depth.
     * <p>
     * data URI 로 중첩된 SVG 는 깊이가 제한된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNestedImageDepth() throws Exception {
        String inner = "<svg xmlns='http://www.w3.org/2000/svg' width='4' height='4'><rect width='4' height='4' fill='#0a0'/></svg>";
        String cur = inner;
        // 같은 SVG 를 10 번 data URI 로 감싼 이미지 안에 다시 넣는다.
        for (int i = 0; i < 10; i++) {
            String b64 = java.net.URLEncoder.encode(cur, "UTF-8").replace("+", "%20");
            cur = "<svg xmlns='http://www.w3.org/2000/svg' width='4' height='4'><image width='4' height='4' href='data:image/svg+xml," + b64 + "'/></svg>";
        }
        DecodedImage img = SvgTestKit.decode(cur);
        Check.equal(4, img.getWidth(), "the outer document is decoded; the too deeply nested inner image is dropped");
    }

    /**
     * <code>systemLanguage</code> follows the default locale of the JVM.
     * <p>
     * <code>systemLanguage</code> 는 JVM 의 기본 로케일을 따른다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testSystemLanguage() throws Exception {
        Locale saved = Locale.getDefault();
        try {
            String svg = SvgTestKit.doc(30, 10, "<rect width=\"10\" height=\"10\" fill=\"#f00\" systemLanguage=\"ko\"/>"
                    + "<rect x=\"10\" width=\"10\" height=\"10\" fill=\"#0f0\" systemLanguage=\"en-US, de\"/><rect x=\"20\" width=\"10\" height=\"10\" fill=\"#00f\" systemLanguage=\"\"/>");
            Locale.setDefault(new Locale("ko", "KR"));
            DecodedImage ko = SvgTestKit.decode(svg);
            Check.equal(0xFFFF0000L, SvgTestKit.px(ko, 5, 5) & 0xFFFFFFFFL, "ko matches ko-KR");
            Check.equal(0L, SvgTestKit.px(ko, 15, 5) & 0xFFFFFFFFL, "en-US does not match ko-KR");
            Locale.setDefault(Locale.US);
            DecodedImage en = SvgTestKit.decode(svg);
            Check.equal(0L, SvgTestKit.px(en, 5, 5) & 0xFFFFFFFFL, "ko does not match en-US");
            Check.equal(0xFF00FF00L, SvgTestKit.px(en, 15, 5) & 0xFFFFFFFFL, "en-US matches");
            Check.equal(0L, SvgTestKit.px(en, 25, 5) & 0xFFFFFFFFL, "an empty list never matches");
            Locale.setDefault(Locale.ENGLISH);
            Check.equal(0xFF00FF00L, SvgTestKit.px(SvgTestKit.decode(svg), 15, 5) & 0xFFFFFFFFL, "the list item en-US matches the language en");
        } finally {
            Locale.setDefault(saved);
        }
    }

    /**
     * The document can be drawn at other sizes and stays sharp.
     * <p>
     * 문서를 다른 크기로 그릴 수 있고 선명하게 유지된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testRenderAtOtherSizes() throws Exception {
        SvgImage doc = SvgImage.parse(SvgTestKit.utf8(SvgTestKit.doc(10, 10, "<rect x=\"2\" y=\"2\" width=\"6\" height=\"6\" fill=\"#ff0000\"/>")), null);
        int[] big = doc.renderArgb(100, 100);
        Check.equal(0xFFFF0000L, big[50 * 100 + 50] & 0xFFFFFFFFL, "centre at 10x scale");
        Check.equal(0xFFFF0000L, big[20 * 100 + 20] & 0xFFFFFFFFL, "the corner pixel of the rectangle is exact at 10x");
        Check.equal(0L, big[19 * 100 + 50] & 0xFFFFFFFFL, "the edge stays sharp (no blur)");
        int[] wide = doc.renderArgb(40, 10);
        Check.equal(0xFFFF0000L, wide[5 * 40 + 20] & 0xFFFFFFFFL, "meet: the image is centred in a wide box");
        Check.equal(0L, wide[5 * 40 + 5] & 0xFFFFFFFFL, "the bands beside the image are empty");
        java.awt.image.BufferedImage target = new java.awt.image.BufferedImage(30, 30, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = target.createGraphics();
        doc.render(g, 30, 30);
        g.dispose();
        Check.equal(0xFFFF0000L, target.getRGB(15, 15) & 0xFFFFFFFFL, "render(Graphics2D, w, h)");
        final SvgImage d2 = doc;
        Check.throwsException(MediaException.class, new Block() {
            public void run() throws Exception {
                d2.renderArgb(0, 10);
            }
        }, "invalid size");
    }

    /**
     * The same document drawn from several threads at once gives identical results.
     * <p>
     * 같은 문서를 여러 스레드에서 동시에 그려도 결과가 같다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testConcurrentDrawing() throws Exception {
        final SvgImage doc = SvgImage.parse(SvgTestKit.utf8(SvgTestKit.doc(60, 60, "<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#f00\"/>"
                + "<stop offset=\"1\" stop-color=\"#00f\"/></linearGradient></defs><circle cx=\"30\" cy=\"30\" r=\"25\" fill=\"url(#g)\" opacity=\".7\" stroke=\"#000\" stroke-width=\"3\"/>")), null);
        final int[] expect = doc.renderArgb(60, 60);
        final List<String> errors = new ArrayList<String>();
        Thread[] ts = new Thread[4];
        // 네 스레드가 같은 문서를 반복해 그려 결과를 비교한다.
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        // 조건이 참인 동안 반복해서 처리한다.
                        for (int k = 0; k < 5; k++) {
                            if (!Arrays.equals(expect, doc.renderArgb(60, 60))) {
                                synchronized (errors) {
                                    errors.add("different result");
                                }
                            }
                        }
                    } catch (MediaException e) {
                        synchronized (errors) {
                            errors.add(e.toString());
                        }
                    }
                }
            });
            ts[i].start();
        }
        // 조건이 참인 동안 반복해서 처리한다.
        for (int i = 0; i < ts.length; i++) {
            ts[i].join();
        }
        Check.isTrue(errors.isEmpty(), "concurrent drawing: " + errors);
    }

    /**
     * Character encodings: UTF-8 with and without byte order mark, UTF-16, and a declared legacy encoding.
     * <p>
     * 문자 인코딩: BOM 이 있거나 없는 UTF-8, UTF-16, 선언된 옛 인코딩.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testEncodings() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><!-- \uD55C\uAE00 --><rect width=\"10\" height=\"10\" fill=\"#0f0\"/></svg>";
        DecodedImage a = new SvgDecoder().decode(svg.getBytes("UTF-16LE").length > 0 ? prepend(new byte[] { (byte) 0xFF, (byte) 0xFE }, svg.getBytes("UTF-16LE")) : null, null);
        Check.equal(0xFF00FF00L, SvgTestKit.px(a, 5, 5) & 0xFFFFFFFFL, "UTF-16LE with a byte order mark");
        DecodedImage b = new SvgDecoder().decode(prepend(new byte[] { (byte) 0xFE, (byte) 0xFF }, svg.getBytes("UTF-16BE")), null);
        Check.equal(0xFF00FF00L, SvgTestKit.px(b, 5, 5) & 0xFFFFFFFFL, "UTF-16BE with a byte order mark");
        DecodedImage c = new SvgDecoder().decode(prepend(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, svg.getBytes("UTF-8")), null);
        Check.equal(0xFF00FF00L, SvgTestKit.px(c, 5, 5) & 0xFFFFFFFFL, "UTF-8 with a byte order mark");
        String latin = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><!-- \u00E9 --><rect width=\"10\" height=\"10\" fill=\"#00f\"/></svg>";
        DecodedImage d = new SvgDecoder().decode(latin.getBytes("ISO-8859-1"), null);
        Check.equal(0xFF0000FFL, SvgTestKit.px(d, 5, 5) & 0xFFFFFFFFL, "declared ISO-8859-1");
        String unknown = "<?xml version=\"1.0\" encoding=\"no-such-charset\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect width=\"10\" height=\"10\" fill=\"#f0f\"/></svg>";
        DecodedImage e = new SvgDecoder().decode(unknown.getBytes("UTF-8"), null);
        Check.equal(0xFFFF00FFL, SvgTestKit.px(e, 5, 5) & 0xFFFFFFFFL, "an unknown encoding name falls back to UTF-8");
    }

    /**
     * Concatenates two byte arrays.
     * <p>
     * 두 바이트 배열을 이어 붙인다.
     *
     * @param a first / 앞
     * @param b second / 뒤
     * @return the concatenation / 이어 붙인 배열
     */
    private static byte[] prepend(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /**
     * Text produces ink where it should and nothing elsewhere; the clip path of text works.
     * <p>
     * 문자는 있어야 할 곳에 잉크를 만들고 다른 곳에는 만들지 않는다. 문자 클립 경로가 동작한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testTextInk() throws Exception {
        DecodedImage img = SvgTestKit.decode(SvgTestKit.doc(60, 30, "<text x=\"5\" y=\"24\" font-family=\"sans-serif\" font-size=\"24\" font-weight=\"bold\" fill=\"#000\">HI</text>"));
        int ink = 0;
        int outsideTop = 0;
        // 글자 영역과 그 위쪽 빈 영역의 불투명 픽셀 수를 센다.
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 60; x++) {
                int a = SvgTestKit.px(img, x, y) >>> 24;
                if (a > 128 && y >= 8 && y <= 24 && x >= 5 && x <= 45) {
                    ink++;
                }
                if (a > 0 && y < 3) {
                    outsideTop++;
                }
            }
        }
        Check.isTrue(ink > 80, "the bold text H I covers many pixels (" + ink + ")");
        Check.equal(0, outsideTop, "nothing above the ascent");
        DecodedImage invisible = SvgTestKit.decode(SvgTestKit.doc(60, 30, "<text x=\"5\" y=\"24\" font-size=\"24\" fill=\"#000\" display=\"none\">HI</text><text x=\"5\" y=\"24\" font-size=\"0\">HI</text>"));
        int sum = 0;
        // 조건이 참인 동안 반복해서 처리한다.
        for (int i = 0; i < 60 * 30; i++) {
            sum += invisible.getFrames().get(0).getArgb()[i] >>> 24;
        }
        Check.equal(0, sum, "display none and font-size 0 draw nothing");
    }

    /**
     * An unsupported filter primitive: the lighting primitives are skipped, so a filter that has only those hides the element.
     * <p>
     * 지원하지 않는 필터 기본 요소: 조명 기본 요소는 건너뛰므로 그것만 가진 필터는 요소를 숨긴다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testUnsupportedFilterPrimitive() throws Exception {
        DecodedImage img = SvgTestKit.decode(SvgTestKit.doc(10, 10, "<defs><filter id=\"f\"><feDiffuseLighting><feDistantLight/></feDiffuseLighting></filter></defs>"
                + "<rect width=\"10\" height=\"10\" fill=\"#f00\" filter=\"url(#f)\"/><rect x=\"8\" width=\"2\" height=\"2\" fill=\"#0f0\"/>"));
        Check.equal(0L, SvgTestKit.px(img, 2, 5) & 0xFFFFFFFFL, "the element with an unusable filter is not drawn");
        Check.equal(0xFF00FF00L, SvgTestKit.px(img, 9, 1) & 0xFFFFFFFFL, "the rest of the document is drawn");
    }
}
