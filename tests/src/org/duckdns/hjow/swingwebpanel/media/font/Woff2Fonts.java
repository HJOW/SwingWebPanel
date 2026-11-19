package org.duckdns.hjow.swingwebpanel.media.font;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test support: the TrueType fonts of the JRE and their conversion to WOFF2 at test time. The fonts that the JRE ships in <code>lib/fonts</code> (Lucida Sans, Lucida Bright
 * and Lucida Typewriter of Bigelow &amp; Holmes) may be used with Java only, so no file made from them is kept in the repository: a test that needs a real font reads the
 * font file of the JRE that runs it, makes the WOFF2 file here with the test encoder {@link Woff2Builder} (the table data is stored in Brotli blocks that are not compressed)
 * and throws it away. A test whose JRE has no such file is skipped with that reason.
 * <p>
 * 시험 지원: JRE 의 TrueType 글꼴과 시험 때의 WOFF2 변환. JRE 가 <code>lib/fonts</code> 에 담은 글꼴(Bigelow &amp; Holmes 의 Lucida Sans, Lucida Bright, Lucida Typewriter)은 Java 에서만 쓰도록
 * 허락된 것이어서 그것으로 만든 파일을 저장소에 두지 않는다: 실제 글꼴이 필요한 시험은 그것을 실행하는 JRE 의 글꼴 파일을 읽어 여기서 시험용 인코더 {@link Woff2Builder} 로
 * WOFF2 파일을 만들고(테이블 데이터는 압축하지 않은 Brotli 블록에 저장한다) 버린다. JRE 에 그런 파일이 없으면 시험은 그 이유로 건너뛴다.
 */
public final class Woff2Fonts {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Woff2Fonts() {
    }

    /**
     * Reads a TrueType font file of the JRE (<code>lib/fonts</code> below <code>java.home</code>).
     * <p>
     * JRE 의 TrueType 글꼴 파일(<code>java.home</code> 아래 <code>lib/fonts</code>)을 읽는다.
     *
     * @param name the file name, for example <code>LucidaBrightDemiBold.ttf</code> / 파일 이름. 예: <code>LucidaBrightDemiBold.ttf</code>
     * @return the bytes, or null if the JRE has no such file / 바이트. JRE 에 그런 파일이 없으면 null
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없으면
     */
    public static byte[] jreFont(String name) throws IOException {
        File f = new File(new File(System.getProperty("java.home"), "lib/fonts"), name);
        if (!f.isFile()) {
            return null;
        }
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Converts a TrueType font to a WOFF2 file with the test encoder.
     * <p>
     * 시험용 인코더로 TrueType 글꼴을 WOFF2 파일로 바꾼다.
     *
     * @param ttf       the font / 글꼴
     * @param transform true for the transformed <code>glyf</code>, <code>loca</code> (and <code>hmtx</code> where possible), false for the null transforms / 변환한
     *                  <code>glyf</code>, <code>loca</code>(가능하면 <code>hmtx</code>)이면 true, 널 변환이면 false
     * @return the WOFF2 file / WOFF2 파일
     * @throws IOException if the font cannot be converted / 글꼴을 바꿀 수 없으면
     */
    public static byte[] woff2(byte[] ttf, boolean transform) throws IOException {
        return Woff2Builder.fromSfnt(ttf, transform).build().file;
    }

    /**
     * Converts a TrueType font to a WOFF2 file that also has an extended metadata block and a private data block (their content is not interpreted).
     * <p>
     * TrueType 글꼴을 확장 메타데이터 블록과 비공개 데이터 블록이 붙은 WOFF2 파일로 바꾼다(내용은 해석하지 않는다).
     *
     * @param ttf the font / 글꼴
     * @return the WOFF2 file with the transformed tables / 변환한 테이블이 든 WOFF2 파일
     * @throws IOException if the font cannot be converted / 글꼴을 바꿀 수 없으면
     */
    public static byte[] woff2WithMetadata(byte[] ttf) throws IOException {
        Woff2Builder b = Woff2Builder.fromSfnt(ttf, true);
        Woff2Builder.Result r = b.build();
        byte[] meta = Woff2Builder.stored("<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata version=\"1.0\"><uniqueid id=\"swingwebpanel.woff2.test\"/></metadata>".getBytes("UTF-8"));
        byte[] priv = "private data of the test".getBytes("UTF-8");
        return Woff2Builder.assemble(b.flavor, r.numTables, r.dir, Woff2Builder.stored(r.stream), meta, priv);
    }
}
