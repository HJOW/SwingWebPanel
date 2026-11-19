package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.media.font.Woff2;

/**
 * The <code>@font-face</code> faces of a page (plan item P10-05, CSS Fonts Level 3 section 4 https://www.w3.org/TR/css-fonts-3/ edition
 * (edition of the feature table); WOFF 1.0 https://www.w3.org/TR/WOFF/): the faces are made from the rules, the sources of a face are tried in order, and a
 * face that is loaded is chosen for a request by the font matching of CSS Fonts 3 section 5.2 (the style first, then the weight in the
 * order that depends on the requested weight). <b>Formats:</b> TrueType (<code>0x00010000</code>, <code>true</code>), WOFF 1.0
 * (rebuilt to TrueType with {@link Woff}) and WOFF 2.0 (decompressed with the own Brotli decoder and rebuilt with {@link Woff2}, including the transformed
 * <code>glyf</code>, <code>loca</code> and <code>hmtx</code> tables; font collections are refused) are read with {@link Font#createFont(int, java.io.InputStream)}, which
 * is a public JRE API; OpenType with CFF outlines (<code>OTTO</code>) is attempted and, where the JRE cannot read it (JDK 6u45 throws
 * <code>FontFormatException</code>, checked by the tests of the project), the source counts as failed and the next one is tried;
 * Embedded OpenType, SVG fonts and Type 1 are not supported: a <code>format()</code> hint that names them (or any other
 * unknown format) skips the source, and a file with such a signature fails. <code>local()</code> sources use an installed font family.
 * <b>Loading:</b> the bytes come from a {@link Loader} (the common request layer of the page with the cache, the headers and the
 * same-origin rule); while they are pending the face is not used, so the text takes the next family of <code>font-family</code> and the
 * page is laid out again when the bytes arrive (the <code>swap</code> behavior of <code>font-display</code>; a font that arrives after the periods of <code>fallback</code> or <code>optional</code> is not used, see {@link #late}; while a pending face is in its block period the text is laid out with the fallback font but not painted, see {@link #blockedUntil}). The
 * <code>unicode-range</code> of a face applies to a whole text segment: the face is used only if all characters of the text are in the
 * ranges (a segment that is split between two faces with different ranges takes the next family). A synthetic bold or italic is made
 * by the JRE when the face lacks the requested one. At most {@link #MAX_FACES} faces are read per page; the file size is limited by
 * {@link Woff#MAX_SFNT_SIZE}. The JRE writes the fonts that it creates to temporary files that it removes when the font is collected
 * or the JVM ends. The class is thread-safe (the layout helper thread asks, the document thread sets the rules).
 * <p>
 * 페이지의 <code>@font-face</code> 글꼴 면(계획 P10-05, CSS Fonts Level 3 4 절 https://www.w3.org/TR/css-fonts-3/ (기능 표의 판본), WOFF 1.0 https://www.w3.org/TR/WOFF/):
 * 글꼴 면은 규칙에서 만들고, 면의 소스는 차례로 시도하며, 읽힌 면은 CSS Fonts 3 5.2 절의 글꼴 맞춤(스타일 먼저, 그다음 요청한 굵기에 따라 정해지는 순서의 굵기)으로 요청에
 * 골라진다. <b>형식:</b> TrueType(<code>0x00010000</code>, <code>true</code>), WOFF 1.0({@link Woff} 로 TrueType 으로 다시 만든다), WOFF 2.0(자체 Brotli 디코더로 풀고
 * {@link Woff2} 로 변환된 <code>glyf</code>, <code>loca</code>, <code>hmtx</code> 테이블까지 복원해 다시 만든다. 글꼴 컬렉션은 거부한다)은 JRE 공개 API
 * {@link Font#createFont(int, java.io.InputStream)} 로 읽는다. CFF 외곽선의 OpenType(<code>OTTO</code>)은 시도하며 JRE 가 읽지 못하면(JDK 6u45 는
 * <code>FontFormatException</code> 을 던진다. 프로젝트 시험이 확인한다) 소스는 실패로 세고 다음 소스를 시도한다. Embedded OpenType, SVG 글꼴, Type 1 은
 * 지원하지 않는다: 이들을 가리키는(또는 그 밖의 모르는 형식의) <code>format()</code> 힌트는 소스를 건너뛰고 그런 서명의 파일은 실패한다. <code>local()</code> 소스는 설치된 글꼴
 * 패밀리를 쓴다. <b>로딩:</b> 바이트는 {@link Loader}(캐시, 헤더, 같은 오리진 규칙이 있는 페이지의 공통 요청 계층)에서 오며, 아직 오지 않은 동안 그 면은 쓰이지 않아 텍스트는
 * <code>font-family</code> 의 다음 패밀리를 쓰고 바이트가 오면 페이지를 다시 배치한다(<code>font-display</code> 의 <code>swap</code> 동작. <code>fallback</code> 나 <code>optional</code> 의 기간이 지난 뒤 도착한 글꼴은 쓰지 않는다({@link #late} 참조). 대기 중인 면이 block 기간에 있으면 텍스트는 대체 글꼴로 배치하되 그리지 않는다({@link #blockedUntil} 참조)). 면의
 * <code>unicode-range</code> 는 텍스트 세그먼트 전체에 적용된다: 텍스트의 모든 글자가 범위 안일 때만 그 면을 쓴다(범위가 다른 두 면에 걸치는 세그먼트는 다음 패밀리를 쓴다).
 * 면에 요청한 굵기나 이탤릭이 없으면 JRE 가 합성 굵게/기울임을 만든다. 페이지마다 최대 {@link #MAX_FACES} 개의 면을 읽고 파일 크기는 {@link Woff#MAX_SFNT_SIZE} 로
 * 제한한다. JRE 는 만든 글꼴을 임시 파일에 쓰고 글꼴이 수거되거나 JVM 이 끝날 때 지운다. 클래스는 스레드 안전하다(레이아웃 보조 스레드가 묻고 문서 스레드가 규칙을 정한다).
 */
public final class WebFontSet implements WebFontSource {

    /** The largest number of faces that are read per page. 페이지마다 읽는 면의 최대 수. */
    public static final int MAX_FACES = 32;

    /** The block period of <code>font-display: fallback</code> and <code>optional</code> in milliseconds (CSS Fonts 4 section 4.8: "extremely small", at most 100 ms). <code>font-display: fallback</code> 와 <code>optional</code> 의 block 기간(ms). CSS Fonts 4 4.8 절: 아주 짧음, 최대 100ms. */
    public static final long SHORT_BLOCK_MS = 100;

    /** The block period of <code>font-display: block</code> and <code>auto</code> in milliseconds (CSS Fonts 4 section 4.8: "short", 3 s recommended; <code>auto</code> is chosen by the user agent and is <code>block</code> here, as in the major browsers). <code>font-display: block</code> 과 <code>auto</code> 의 block 기간(ms). CSS Fonts 4 4.8 절: 짧음, 3초 권장. <code>auto</code> 는 사용자 에이전트가 정하며 여기서는 주요 브라우저처럼 <code>block</code> 이다. */
    public static final long LONG_BLOCK_MS = 3000;

    /** The swap period of <code>font-display: fallback</code> in milliseconds (CSS Fonts 4 section 4.8: "short", at most 3 s). <code>font-display: fallback</code> 의 swap 기간(ms). CSS Fonts 4 4.8 절: 짧음, 최대 3초. */
    public static final long FALLBACK_SWAP_MS = 3000;

    /**
     * The source of the bytes of a font file: the request layer of the page.
     * <p>
     * 글꼴 파일의 바이트의 출처: 페이지의 요청 계층.
     */
    public interface Loader {
        /**
         * Returns the bytes of the font at a URL.
         * <p>
         * URL 의 글꼴 바이트를 반환한다.
         *
         * @param absoluteUrl the absolute URL / 절대 URL
         * @return the bytes, or null while the request is still running / 바이트. 요청이 아직 진행 중이면 null
         * @throws IOException if the request failed or is not allowed / 요청이 실패했거나 허용되지 않으면
         */
        byte[] load(String absoluteUrl) throws IOException;
    }

    /**
     * A {@link Loader} that also tells when the request of a font started and when its bytes arrived, so that <code>font-display</code> can tell whether the
     * font arrived in time (CSS Fonts 4 section 4.8). The times are measured where the request runs (not where the layout asks), so that a late layout
     * does not make a font late.
     * <p>
     * 글꼴 요청이 언제 시작되었고 바이트가 언제 도착했는지도 알려 주는 {@link Loader}. <code>font-display</code> 가 글꼴이 제때 도착했는지 가릴 수 있게 한다(CSS Fonts 4 4.8 절).
     * 시간은 레이아웃이 묻는 곳이 아니라 요청이 실행되는 곳에서 재므로 레이아웃이 늦어도 글꼴이 늦은 것이 되지 않는다.
     */
    public interface TimedLoader extends Loader {
        /**
         * Returns the times of the request of a font.
         * <p>
         * 글꼴 요청의 시각을 반환한다.
         *
         * @param absoluteUrl the absolute URL / 절대 URL
         * @return {start, arrival} in milliseconds of the system clock (arrival is 0 until the bytes are there), or null if they are not known / 시스템 시계 ms 의 {시작, 도착}
         *         (도착은 바이트가 올 때까지 0). 모르면 null
         */
        long[] times(String absoluteUrl);
    }

    /** The state of a face. 면의 상태. */
    private static final int PENDING = 0;
    /** The face is loaded. 면을 읽었다. */
    private static final int LOADED = 1;
    /** Every source of the face failed. 면의 모든 소스가 실패했다. */
    private static final int FAILED = 2;

    /**
     * One face made from a <code>@font-face</code> rule.
     * <p>
     * <code>@font-face</code> 규칙 하나로 만든 면.
     */
    private static final class Face {
        /** The rule. 규칙. */
        FontFaceRule rule;
        /** The family in lower case. 소문자 패밀리. */
        String family;
        /** The lowest weight that the face covers. 면이 덮는 가장 가벼운 굵기. */
        int weightLo = 400;
        /** The highest weight that the face covers. 면이 덮는 가장 무거운 굵기. */
        int weightHi = 400;
        /** True for an italic or oblique face. 이탤릭이나 oblique 면이면 true. */
        boolean italic;
        /** The unicode ranges {low, high} pairs, null for all. 유니코드 범위 {하한, 상한} 쌍. 전부이면 null. */
        int[][] ranges;
        /** The state. 상태. */
        int state = PENDING;
        /** The index of the source that is being tried. 시도 중인 소스의 위치. */
        int current;
        /** The font when loaded. 읽었을 때의 글꼴. */
        Font font;
        /** The TrueType bytes of the loaded font (for the glyph alternates), or null. 읽은 글꼴의 TrueType 바이트(대체 글리프용). 없으면 null. */
        byte[] sfnt;
        /** The fonts with glyph alternates by the description of their features. 기능 기술별 대체 글리프 글꼴. */
        Map<String, Font> variants;
        /** The reason of a failure, for diagnostics. 실패 이유(진단용). */
        String failure;
        /** The value of the <code>font-display</code> descriptor in lower case (<code>auto</code> if not given). <code>font-display</code> 서술자의 값(소문자. 없으면 <code>auto</code>). */
        String display = "auto";
        /** True if the rule gives <code>font-display</code> (otherwise the <code>@font-feature-values</code> of the family may). 규칙이 <code>font-display</code> 를 주면 true(아니면 패밀리의 <code>@font-feature-values</code> 가 줄 수 있다). */
        boolean displayGiven;
    }

    /** The loader of the bytes. 바이트를 읽는 로더. */
    private final Loader loader;
    /** The faces in the order of the rules. 규칙 순서의 면. */
    private List<Face> faces = new ArrayList<Face>();
    /** The earliest end of a block period that {@link #blockedUntil} reported since the last {@link #takeBlockDeadline()}, 0 if none. 마지막 {@link #takeBlockDeadline()} 뒤 {@link #blockedUntil} 이 알린 가장 이른 block 기간의 끝. 없으면 0. */
    private long blockDeadline;
    /** The installed family names in lower case, made when the first <code>local()</code> source is tried. 설치된 패밀리 이름(소문자). 첫 <code>local()</code> 소스를 시도할 때 만든다. */
    private Set<String> installed;

    /**
     * Creates the set.
     * <p>
     * 집합을 만든다.
     *
     * @param loader the source of the font files / 글꼴 파일의 출처
     */
    public WebFontSet(Loader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("loader must not be null");
        }
        this.loader = loader;
    }

    /**
     * Sets the <code>@font-face</code> rules of the page. The faces of the rules that were there before (the same rule object) keep
     * their state, so a loaded font is not loaded again when the style sheets are scanned again.
     * <p>
     * 페이지의 <code>@font-face</code> 규칙을 정한다. 전에 있던 규칙(같은 규칙 객체)의 면은 상태를 유지하므로 스타일 시트를 다시 훑어도 읽힌 글꼴을 다시 읽지 않는다.
     *
     * @param rules the rules in the order of the style sheets / 스타일 시트 순서의 규칙
     */
    public synchronized void setRules(List<FontFaceRule> rules) {
        List<Face> next = new ArrayList<Face>(rules.size());
        // 규칙마다 이미 있는 면을 재사용하거나 새로 만든다.
        for (FontFaceRule r : rules) {
            Face f = null;
            // 같은 규칙 객체의 이전 면을 찾는다.
            for (Face old : faces) {
                if (old.rule == r) {
                    f = old;
                    break;
                }
            }
            next.add(f != null ? f : makeFace(r));
        }
        faces = next;
    }

    /**
     * Makes the face of a rule: the family, the weight range, the style and the unicode ranges.
     * <p>
     * 규칙의 면을 만든다: 패밀리, 굵기 범위, 스타일, 유니코드 범위.
     *
     * @param r the rule / 규칙
     * @return the face / 면
     */
    private static Face makeFace(FontFaceRule r) {
        Face f = new Face();
        f.rule = r;
        String fam = r.getFamily();
        f.family = fam == null ? "" : fam.toLowerCase(Locale.ENGLISH);
        String w = r.getDescriptor("font-weight");
        if (w != null) {
            String[] p = w.trim().toLowerCase(Locale.ENGLISH).split("\\s+");
            f.weightLo = weightOf(p[0]);
            f.weightHi = p.length > 1 ? weightOf(p[1]) : f.weightLo;
            if (f.weightHi < f.weightLo) {
                f.weightHi = f.weightLo;
            }
        }
        String st = r.getDescriptor("font-style");
        f.italic = st != null && (st.toLowerCase(Locale.ENGLISH).startsWith("italic") || st.toLowerCase(Locale.ENGLISH).startsWith("oblique"));
        f.ranges = parseRanges(r.getDescriptor("unicode-range"));
        String display = r.getDescriptor("font-display");
        if (display != null) {
            f.display = display.trim().toLowerCase(Locale.ENGLISH);
            f.displayGiven = true;
        }
        if (f.family.length() == 0 || r.getSources().isEmpty()) {
            f.state = FAILED;
            f.failure = "no family or no source";
        }
        return f;
    }

    /**
     * Reads a weight keyword or number.
     * <p>
     * 굵기 키워드나 숫자를 읽는다.
     *
     * @param s the text / 문자열
     * @return the weight / 굵기
     */
    private static int weightOf(String s) {
        if ("normal".equals(s)) {
            return 400;
        }
        if ("bold".equals(s)) {
            return 700;
        }
        try {
            return Math.max(1, Math.min(1000, (int) Math.round(Double.parseDouble(s))));
        } catch (NumberFormatException e) {
            return 400;
        }
    }

    /**
     * Reads a <code>unicode-range</code> descriptor: <code>U+26</code>, <code>U+0-7F</code>, <code>U+4??</code>, separated by commas.
     * An invalid descriptor means no range (all characters).
     * <p>
     * <code>unicode-range</code> 서술자를 읽는다: <code>U+26</code>, <code>U+0-7F</code>, <code>U+4??</code>, 쉼표로 구분. 올바르지 않으면 범위 없음(모든 글자)이다.
     *
     * @param text the descriptor, may be null / 서술자. null 가능
     * @return the {low, high} pairs, or null for all characters / {하한, 상한} 쌍. 모든 글자이면 null
     */
    static int[][] parseRanges(String text) {
        if (text == null || text.trim().length() == 0) {
            return null;
        }
        String[] parts = text.split(",");
        List<int[]> out = new ArrayList<int[]>();
        // 쉼표로 나눈 범위를 하나씩 읽는다.
        for (String raw : parts) {
            String p = raw.trim().toLowerCase(Locale.ENGLISH);
            if (!p.startsWith("u+")) {
                return null;
            }
            p = p.substring(2);
            try {
                if (p.indexOf('?') >= 0) {
                    out.add(new int[] { Integer.parseInt(p.replace('?', '0'), 16), Integer.parseInt(p.replace('?', 'f'), 16) });
                } else if (p.indexOf('-') > 0) {
                    String[] se = p.split("-");
                    out.add(new int[] { Integer.parseInt(se[0], 16), Integer.parseInt(se[1], 16) });
                } else {
                    int v = Integer.parseInt(p, 16);
                    out.add(new int[] { v, v });
                }
            } catch (NumberFormatException e) {
                return null;
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        return out.toArray(new int[out.size()][]);
    }

    /**
     * Tells whether the face covers all characters of a text (the text has no characters outside the <code>unicode-range</code>).
     * <p>
     * 면이 텍스트의 모든 글자를 덮는지(텍스트에 <code>unicode-range</code> 밖의 글자가 없는지) 알려 준다.
     *
     * @param f    the face / 면
     * @param text the text, may be null / 텍스트. null 가능
     * @return true if the face may be used for the text / 텍스트에 면을 쓸 수 있으면 true
     */
    private static boolean covers(Face f, String text) {
        if (f.ranges == null || text == null) {
            return true;
        }
        int i = 0;
        // 텍스트의 코드 포인트마다 어느 범위 안인지 확인한다.
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean in = false;
            // 범위 목록에서 코드 포인트가 들어가는 것을 찾는다.
            for (int k = 0; k < f.ranges.length && !in; k++) {
                in = cp >= f.ranges[k][0] && cp <= f.ranges[k][1];
            }
            if (!in) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the family name is the family of a face (a web font family): the family names that no rule declares are looked up
     * as installed fonts as usual.
     * <p>
     * 패밀리 이름이 면의 패밀리(웹 글꼴 패밀리)인지 알려 준다: 어떤 규칙도 선언하지 않은 패밀리 이름은 평소처럼 설치된 글꼴로 찾는다.
     *
     * @param family the family name / 패밀리 이름
     * @return true if a rule declares the family / 규칙이 그 패밀리를 선언하면 true
     */
    public synchronized boolean hasFamily(String family) {
        String l = family.toLowerCase(Locale.ENGLISH);
        // 면마다 패밀리 이름을 비교한다.
        for (Face f : faces) {
            if (f.family.equals(l)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a face of a family still waits for its bytes (the text uses the next family for the time being).
     * <p>
     * 패밀리의 면이 아직 바이트를 기다리는지(텍스트는 당분간 다음 패밀리를 쓴다) 알려 준다.
     *
     * @param family the family name / 패밀리 이름
     * @return true if a face of the family is pending / 그 패밀리의 면이 대기 중이면 true
     */
    public synchronized boolean isPending(String family) {
        String l = family.toLowerCase(Locale.ENGLISH);
        // 패밀리의 면 중 아직 읽는 중인 것이 있는지 본다(읽기를 시도하기도 한다).
        for (Face f : faces) {
            if (f.family.equals(l)) {
                ensureLoaded(f);
                if (f.state == PENDING) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the reasons why faces failed, for diagnostics ("family: reason").
     * <p>
     * 면이 실패한 이유를 진단용으로 반환한다("패밀리: 이유").
     *
     * @return the messages / 메시지
     */
    public synchronized List<String> failures() {
        List<String> out = new ArrayList<String>();
        // 실패한 면의 이유를 모은다.
        for (Face f : faces) {
            if (f.state == FAILED && f.failure != null) {
                out.add(f.family + ": " + f.failure);
            }
        }
        return out;
    }

    /**
     * Finds the font of a family for a weight, a style and a text.
     * <p>
     * 굵기, 스타일, 텍스트에 맞는 패밀리의 글꼴을 찾는다.
     *
     * @param family the family name / 패밀리 이름
     * @param weight the requested weight / 요청한 굵기
     * @param italic true for italic / 이탤릭이면 true
     * @param text   the text, may be null / 텍스트. null 가능
     * @param size   the size in pixels / 크기(픽셀)
     * @return the font or null / 글꼴 또는 null
     */
    public Font find(String family, int weight, boolean italic, String text, float size) {
        return find(family, weight, italic, text, size, true, true);
    }

    /**
     * Finds the font of a family for a weight, a style and a text; a bold or an italic that the face lacks is synthesized only if the caller says so
     * (<code>font-synthesis-weight</code> and <code>font-synthesis-style</code>).
     * <p>
     * 굵기, 스타일, 텍스트에 맞는 패밀리의 글꼴을 찾는다. 면에 없는 굵게나 기울임은 호출자가 허락할 때만 합성한다(<code>font-synthesis-weight</code>, <code>font-synthesis-style</code>).
     *
     * @param family      the family name / 패밀리 이름
     * @param weight      the requested weight / 요청한 굵기
     * @param italic      true for italic / 이탤릭이면 true
     * @param text        the text, may be null / 텍스트. null 가능
     * @param size        the size in pixels / 크기(픽셀)
     * @param synthBold   true if a bold may be synthesized / 굵게를 합성해도 되면 true
     * @param synthItalic true if an italic may be synthesized / 기울임을 합성해도 되면 true
     * @return the font or null / 글꼴 또는 null
     */
    public synchronized Font find(String family, int weight, boolean italic, String text, float size, boolean synthBold, boolean synthItalic) {
        return find(family, weight, italic, text, size, synthBold, synthItalic, null);
    }

    /** The <code>@font-feature-values</code> rules of the page in document order, or null. 페이지의 <code>@font-feature-values</code> 규칙(문서 순서). 없으면 null. */
    private List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> featureValues;

    /**
     * Returns the <code>font-display</code> value of a face: its own descriptor, else the <code>font-display</code> of the <code>@font-feature-values</code>
     * of its family (CSS Fonts 4 section 4.9.1), else <code>auto</code>.
     * <p>
     * 면의 <code>font-display</code> 값을 반환한다: 자신의 서술자, 없으면 그 패밀리의 <code>@font-feature-values</code> 의 <code>font-display</code>(CSS Fonts 4 4.9.1 절),
     * 그것도 없으면 <code>auto</code>.
     *
     * @param f the face / 면
     * @return the value in lower case / 소문자 값
     */
    private String displayOf(Face f) {
        if (f.displayGiven) {
            return f.display;
        }
        String d = org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule.fontDisplay(featureValues, f.family);
        return d != null ? d : f.display;
    }

    /**
     * Returns the <code>font-display</code> value that the faces of a family without the descriptor use (for tests and diagnostics).
     * <p>
     * 서술자가 없는 패밀리의 면이 쓰는 <code>font-display</code> 값을 반환한다(시험과 진단용).
     *
     * @param family the family / 패밀리
     * @return the value / 값
     */
    synchronized String defaultDisplay(String family) {
        String d = org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule.fontDisplay(featureValues, family);
        return d != null ? d : "auto";
    }

    /**
     * Sets the <code>@font-feature-values</code> rules of the page (CSS Fonts 4 section 6.9): the names of <code>font-variant-alternates</code> are looked up
     * in them for the family of a web font.
     * <p>
     * 페이지의 <code>@font-feature-values</code> 규칙을 정한다(CSS Fonts 4 6.9 절): 웹 글꼴의 패밀리에 대해 <code>font-variant-alternates</code> 의 이름을 여기서 찾는다.
     *
     * @param rules the rules in document order, or null / 문서 순서의 규칙. 없으면 null
     */
    public synchronized void setFeatureValues(List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> rules) {
        this.featureValues = rules == null ? null : new ArrayList<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule>(rules);
    }

    /**
     * Finds the loaded face of a family like {@link #find(String, int, boolean, String, float, boolean, boolean)} and gives it the glyph alternates that the
     * style asks for (<code>font-variant-alternates</code> with the <code>@font-feature-values</code> of the family, and the same tags of
     * <code>font-feature-settings</code>): the font is a copy whose characters map to the alternate glyphs of the <code>GSUB</code> table of the face
     * ({@link GlyphSubstitution}); a face without such glyphs is used as it is.
     * <p>
     * {@link #find(String, int, boolean, String, float, boolean, boolean)} 처럼 패밀리의 읽힌 면을 찾고 스타일이 요구하는 대체 글리프를 준다(그 패밀리의
     * <code>@font-feature-values</code> 로 푼 <code>font-variant-alternates</code> 와 <code>font-feature-settings</code> 의 같은 태그): 글꼴은 문자가 면의 <code>GSUB</code> 표의
     * 대체 글리프를 가리키는 사본이다({@link GlyphSubstitution}). 그런 글리프가 없는 면은 그대로 쓴다.
     *
     * @param family      the family name / 패밀리 이름
     * @param weight      the requested weight / 요청한 굵기
     * @param italic      true for italic / 이탤릭이면 true
     * @param text        the text, may be null / 텍스트. null 가능
     * @param size        the size in px / 크기(px)
     * @param synthBold   true if a bold may be synthesized / 굵게를 합성해도 되면 true
     * @param synthItalic true if an italic may be synthesized / 기울임을 합성해도 되면 true
     * @param style       the style, or null for no alternates / 스타일. 대체 글리프가 없으면 null
     * @return the font or null / 글꼴 또는 null
     */
    public synchronized Font find(String family, int weight, boolean italic, String text, float size, boolean synthBold, boolean synthItalic,
            org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle style) {
        String l = family.toLowerCase(Locale.ENGLISH);
        List<Face> cands = new ArrayList<Face>();
        // 패밀리가 같고 텍스트를 덮는 면을 모은다.
        for (Face f : faces) {
            if (f.family.equals(l) && f.state != FAILED && covers(f, text)) {
                cands.add(f);
            }
        }
        // 가장 알맞은 면부터 읽혔는지 보고 읽힌 첫 면을 쓴다(알맞은 면이 아직 대기 중이면 null: 다음 패밀리를 쓴다).
        while (!cands.isEmpty()) {
            Face best = pick(cands, weight, italic);
            ensureLoaded(best);
            if (best.state == LOADED) {
                int synth = 0;
                // 면에 없는 굵게/기울임은 font-synthesis-weight/style 이 허락할 때만 합성한다.
                if (weight >= 600 && best.weightHi < 600 && synthBold) {
                    synth |= Font.BOLD;
                }
                if (italic && !best.italic && synthItalic) {
                    synth |= Font.ITALIC;
                }
                Font f = withAlternates(best, family, style).deriveFont(synth, size);
                if (text == null || text.length() == 0 || f.canDisplayUpTo(text) < 0) {
                    return f;
                }
                return null;
            }
            if (best.state == PENDING) {
                return null;
            }
            cands.remove(best);
        }
        return null;
    }

    /**
     * Returns the font of a loaded face with the glyph alternates that a style asks for, made once per set of features.
     * <p>
     * 읽힌 면의 글꼴에 스타일이 요구하는 대체 글리프를 준 것을 반환한다. 기능의 묶음마다 한 번 만든다.
     *
     * @param face   the face / 면
     * @param family the family as asked / 요청한 패밀리
     * @param style  the style or null / 스타일 또는 null
     * @return the font (size 1) / 글꼴(크기 1)
     */
    private Font withAlternates(Face face, String family, org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle style) {
        if (style == null || face.sfnt == null) {
            return face.font;
        }
        Map<String, Integer> features = FontFeatures.glyphFeatures(style, featureValues, family);
        if (features.isEmpty()) {
            return face.font;
        }
        String key = features.toString();
        if (face.variants == null) {
            face.variants = new java.util.HashMap<String, Font>();
        }
        Font known = face.variants.get(key);
        if (known != null) {
            return known;
        }
        Font made = face.font;
        byte[] sfnt = GlyphSubstitution.apply(face.sfnt, features);
        if (sfnt != null) {
            try {
                made = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(sfnt));
            } catch (IOException e) {
                made = face.font;
            } catch (FontFormatException e) {
                made = face.font;
            }
        }
        face.variants.put(key, made);
        return made;
    }

    /**
     * Tells until when a text that asks for a family must be invisible because the face that would show it is still loading and in the block period of its
     * <code>font-display</code> (CSS Fonts 4 section 4.8: "If the font face is not loaded, any element attempting to use it must render an invisible fallback font
     * face"). The block period starts when the request of the font starts, as measured by a {@link TimedLoader}; with another loader there is no block period
     * (the text uses the next family at once, like <code>swap</code>). The answer is also kept for {@link #takeBlockDeadline()}, so that the page can be painted
     * again when the period ends. Call it after {@link #find} returned null for the same request.
     * <p>
     * 패밀리를 요청한 텍스트가 언제까지 보이지 않아야 하는지 알려 준다: 그 텍스트를 보일 면이 아직 읽는 중이고 <code>font-display</code> 의 block 기간에 있을 때이다(CSS Fonts 4 4.8 절:
     * "글꼴 면을 읽지 않았으면 그것을 쓰려는 요소는 보이지 않는 대체 글꼴 면으로 그려야 한다"). block 기간은 {@link TimedLoader} 가 잰 글꼴 요청의 시작부터이다. 다른 로더에서는 block
     * 기간이 없다(텍스트는 <code>swap</code> 처럼 곧바로 다음 패밀리를 쓴다). 답은 {@link #takeBlockDeadline()} 를 위해서도 보관하여 기간이 끝날 때 페이지를 다시 그릴 수 있게 한다.
     * 같은 요청에 대해 {@link #find} 가 null 을 반환한 뒤에 부른다.
     *
     * @param family the family name / 패밀리 이름
     * @param weight the requested weight / 요청한 굵기
     * @param italic true for italic / 이탤릭이면 true
     * @param text   the text, may be null / 텍스트. null 가능
     * @return the time (milliseconds of the system clock) at which the block period ends, or 0 if the text is not blocked / block 기간이 끝나는 시각(시스템 시계 ms).
     *         막히지 않았으면 0
     */
    public synchronized long blockedUntil(String family, int weight, boolean italic, String text) {
        if (!(loader instanceof TimedLoader)) {
            return 0;
        }
        String l = family.toLowerCase(Locale.ENGLISH);
        List<Face> cands = new ArrayList<Face>();
        // find 와 같은 후보(패밀리가 같고 실패하지 않았으며 텍스트를 덮는 면)를 모은다.
        for (Face f : faces) {
            if (f.family.equals(l) && f.state != FAILED && covers(f, text)) {
                cands.add(f);
            }
        }
        if (cands.isEmpty()) {
            return 0;
        }
        Face best = pick(cands, weight, italic);
        long period = blockPeriod(displayOf(best));
        if (best.state != PENDING || period <= 0) {
            return 0;
        }
        List<FontFaceRule.Source> sources = best.rule.getSources();
        String url = best.current < sources.size() ? sources.get(best.current).getResolvedUrl() : null;
        long[] times = url == null ? null : ((TimedLoader) loader).times(url);
        // 요청 시작 시각을 모르면 block 기간을 정할 수 없으므로 막지 않는다.
        if (times == null || times.length < 1 || times[0] <= 0) {
            return 0;
        }
        long end = times[0] + period;
        if (System.currentTimeMillis() >= end) {
            return 0;
        }
        // 가장 이른 끝을 보관한다: 페이지는 그때 다시 그린다.
        if (blockDeadline == 0 || end < blockDeadline) {
            blockDeadline = end;
        }
        return end;
    }

    /**
     * Returns the earliest end of a block period that {@link #blockedUntil} reported since the last call, and forgets it. The page asks after each layout and
     * lays the page out again at that time, when the text that was invisible becomes visible in the fallback font.
     * <p>
     * 마지막 호출 뒤 {@link #blockedUntil} 이 알린 가장 이른 block 기간의 끝을 반환하고 잊는다. 페이지는 배치마다 묻고 그 시각에 다시 배치하며, 그때 보이지 않던 텍스트가 대체 글꼴로
     * 보인다.
     *
     * @return the time in milliseconds of the system clock, or 0 / 시스템 시계 ms 의 시각. 없으면 0
     */
    public synchronized long takeBlockDeadline() {
        long d = blockDeadline;
        blockDeadline = 0;
        return d;
    }

    /**
     * Returns the block period of a <code>font-display</code> value (CSS Fonts 4 section 4.8): {@link #LONG_BLOCK_MS} for <code>block</code> and <code>auto</code>,
     * {@link #SHORT_BLOCK_MS} for <code>fallback</code> and <code>optional</code>, none for <code>swap</code>.
     * <p>
     * <code>font-display</code> 값의 block 기간을 반환한다(CSS Fonts 4 4.8 절): <code>block</code> 과 <code>auto</code> 는 {@link #LONG_BLOCK_MS}, <code>fallback</code> 과
     * <code>optional</code> 은 {@link #SHORT_BLOCK_MS}, <code>swap</code> 은 없음.
     *
     * @param display the value in lower case / 소문자 값
     * @return the period in milliseconds / 기간(ms)
     */
    static long blockPeriod(String display) {
        if ("swap".equals(display)) {
            return 0;
        }
        if ("fallback".equals(display) || "optional".equals(display)) {
            return SHORT_BLOCK_MS;
        }
        return LONG_BLOCK_MS;
    }

    /**
     * Picks the best face for a request (CSS Fonts 3 section 5.2): the style first, then the nearest weight in the preferred direction.
     * <p>
     * 요청에 가장 알맞은 면을 고른다(CSS Fonts 3 5.2 절): 스타일 먼저, 그다음 선호하는 방향의 가장 가까운 굵기.
     *
     * @param cands  the candidates, not empty / 후보. 비어 있지 않음
     * @param weight the requested weight / 요청한 굵기
     * @param italic true for italic / 이탤릭이면 true
     * @return the face / 면
     */
    private static Face pick(List<Face> cands, int weight, boolean italic) {
        boolean anySameStyle = false;
        // 요청한 스타일과 같은 면이 있는지 본다(없으면 다른 스타일의 면이 후보이다).
        for (Face f : cands) {
            if (f.italic == italic) {
                anySameStyle = true;
            }
        }
        Face best = null;
        long bestKey = Long.MAX_VALUE;
        // 후보마다 (순위, 거리) 키를 계산해 가장 작은 것을 고른다.
        for (Face f : cands) {
            if (anySameStyle && f.italic != italic) {
                continue;
            }
            int w = Math.max(f.weightLo, Math.min(f.weightHi, weight));
            int diff = w - weight;
            int rank;
            if (diff == 0) {
                rank = 0;
            } else if (weight < 400) {
                rank = diff < 0 ? 1 : 2;
            } else if (weight > 500) {
                rank = diff > 0 ? 1 : 2;
            } else if (diff > 0 && w <= 500) {
                rank = 1;
            } else if (diff < 0) {
                rank = 2;
            } else {
                rank = 3;
            }
            long key = rank * 100000L + Math.abs(diff);
            if (key < bestKey) {
                bestKey = key;
                best = f;
            }
        }
        return best;
    }

    /**
     * Tries the sources of a face in order until one is loaded, stays pending while the bytes of the source being tried are not there,
     * and fails when no source is left.
     * <p>
     * 면의 소스를 하나가 읽힐 때까지 차례로 시도한다. 시도 중인 소스의 바이트가 아직 없으면 대기 상태로 두고, 남은 소스가 없으면 실패한다.
     *
     * @param f the face / 면
     */
    private void ensureLoaded(Face f) {
        if (f.state != PENDING) {
            return;
        }
        int loadedFaces = 0;
        // 이미 읽은 면의 수가 한도에 이르면 더 읽지 않는다.
        for (Face o : faces) {
            if (o.state == LOADED) {
                loadedFaces++;
            }
        }
        if (loadedFaces >= MAX_FACES) {
            f.state = FAILED;
            f.failure = "too many web font faces";
            return;
        }
        List<FontFaceRule.Source> sources = f.rule.getSources();
        // 소스를 차례로 시도한다.
        while (f.current < sources.size()) {
            FontFaceRule.Source s = sources.get(f.current);
            if (s.getLocal() != null) {
                Font lf = localFont(s.getLocal());
                if (lf != null) {
                    f.font = lf;
                    f.state = LOADED;
                    return;
                }
                f.current++;
                continue;
            }
            String url = s.getResolvedUrl();
            if (url == null || !formatSupported(s.getFormat())) {
                f.current++;
                continue;
            }
            try {
                byte[] bytes = loader.load(url);
                if (bytes == null) {
                    return;
                }
                // font-display: 늦게 도착한 글꼴은 swap 기간이 유한한 값(fallback, optional)에서는 쓰지 않는다. 이 페이지에서는 대체 글꼴이 그대로 쓰인다.
                if (loader instanceof TimedLoader && late(displayOf(f), ((TimedLoader) loader).times(url))) {
                    f.state = FAILED;
                    f.failure = "font-display: " + displayOf(f) + ": the font arrived after its block and swap periods";
                    return;
                }
                byte[] sfnt = sfntOf(bytes);
                f.font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(sfnt));
                f.sfnt = sfnt;
                f.state = LOADED;
                return;
            } catch (IOException e) {
                f.failure = String.valueOf(e.getMessage());
            } catch (FontFormatException e) {
                f.failure = "font format: " + e.getMessage();
            } catch (RuntimeException e) {
                f.failure = String.valueOf(e);
            }
            f.current++;
        }
        f.state = FAILED;
        if (f.failure == null) {
            f.failure = "no usable source";
        }
    }

    /**
     * Tells whether a font arrived too late for its <code>font-display</code> (CSS Fonts 4 section 4.8): <code>block</code> and <code>auto</code> have a long
     * block period and an infinite swap period, <code>swap</code> no block period and an infinite swap period, <code>fallback</code> a short block period
     * ({@link #SHORT_BLOCK_MS}) and a short swap period ({@link #FALLBACK_SWAP_MS}), <code>optional</code> a short block period and no swap period. A font that
     * arrives after the block and the swap period is not used any more in this page (the fallback font stays). The invisible text of the block period is
     * decided by {@link #blockedUntil}.
     * <p>
     * 글꼴이 <code>font-display</code> 에 비해 너무 늦게 도착했는지 알려 준다(CSS Fonts 4 4.8 절): <code>block</code> 과 <code>auto</code> 는 긴 block 기간과 무한한 swap 기간,
     * <code>swap</code> 은 block 기간 없이 무한한 swap 기간, <code>fallback</code> 은 짧은 block 기간({@link #SHORT_BLOCK_MS})과 짧은 swap 기간({@link #FALLBACK_SWAP_MS}),
     * <code>optional</code> 은 짧은 block 기간과 swap 기간 없음이다. block 기간과 swap 기간이 지난 뒤 도착한 글꼴은 이 페이지에서 더 쓰지 않는다(대체 글꼴이 그대로 쓰인다).
     * block 기간의 보이지 않는 텍스트는 {@link #blockedUntil} 이 정한다.
     *
     * @param display the value of <code>font-display</code> in lower case / 소문자 <code>font-display</code> 값
     * @param times   {start, arrival} of the request in ms, or null / 요청의 {시작, 도착}(ms). 모르면 null
     * @return true if the font must not be used / 글꼴을 쓰면 안 되면 true
     */
    static boolean late(String display, long[] times) {
        if (times == null || times.length < 2 || times[1] <= 0) {
            return false;
        }
        long elapsed = times[1] - times[0];
        if ("optional".equals(display)) {
            return elapsed > SHORT_BLOCK_MS;
        }
        return "fallback".equals(display) && elapsed > SHORT_BLOCK_MS + FALLBACK_SWAP_MS;
    }

    /**
     * Tells whether a <code>format()</code> hint names a format that can be read (no hint means: try).
     * <p>
     * <code>format()</code> 힌트가 읽을 수 있는 형식을 가리키는지 알려 준다(힌트가 없으면 시도한다).
     *
     * @param format the hint, may be null / 힌트. null 가능
     * @return true if the source may be tried / 소스를 시도할 수 있으면 true
     */
    static boolean formatSupported(String format) {
        if (format == null || format.length() == 0) {
            return true;
        }
        String f = format.toLowerCase(Locale.ENGLISH);
        return "truetype".equals(f) || "opentype".equals(f) || "woff".equals(f) || "woff2".equals(f) || "truetype-variations".equals(f) || "opentype-variations".equals(f);
    }

    /**
     * Decodes the bytes of a font file into a font.
     * <p>
     * 글꼴 파일의 바이트를 글꼴로 디코딩한다.
     *
     * @param bytes the file / 파일
     * @return the font (size 1) / 글꼴(크기 1)
     * @throws IOException         if the format is not supported or the file is invalid / 형식을 지원하지 않거나 파일이 올바르지 않으면
     * @throws FontFormatException if the JRE cannot read the font / JRE 가 글꼴을 읽지 못하면
     */
    private static Font decode(byte[] bytes) throws IOException, FontFormatException {
        return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(sfntOf(bytes)));
    }

    /**
     * Turns the bytes of a font file into TrueType bytes (a WOFF or WOFF 2 file is unpacked).
     * <p>
     * 글꼴 파일의 바이트를 TrueType 바이트로 바꾼다(WOFF 나 WOFF 2 파일은 푼다).
     *
     * @param bytes the file / 파일
     * @return the TrueType bytes / TrueType 바이트
     * @throws IOException if the format is not supported or the file is invalid / 형식을 지원하지 않거나 파일이 올바르지 않으면
     */
    private static byte[] sfntOf(byte[] bytes) throws IOException {
        byte[] sfnt = bytes;
        if (Woff2.isWoff2(bytes)) {
            // WOFF 2 는 Brotli 로 압축한 테이블과 변환된 glyf/loca/hmtx 를 풀어 sfnt 로 다시 만든다.
            sfnt = Woff2.toSfnt(bytes);
        } else if (Woff.isWoff(bytes)) {
            sfnt = Woff.toSfnt(bytes);
        }
        if (!Woff.isSfnt(sfnt)) {
            throw new IOException("not a TrueType, OpenType or WOFF font");
        }
        if (sfnt.length > Woff.MAX_SFNT_SIZE) {
            throw new IOException("font file is too large");
        }
        return sfnt;
    }

    /**
     * Returns the installed font of a <code>local()</code> source, or null if no installed family has the name.
     * <p>
     * <code>local()</code> 소스의 설치된 글꼴을 반환한다. 그 이름의 설치된 패밀리가 없으면 null.
     *
     * @param name the name / 이름
     * @return the font (size 1) or null / 글꼴(크기 1) 또는 null
     */
    private Font localFont(String name) {
        if (installed == null) {
            installed = new HashSet<String>();
            try {
                String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                // 설치된 패밀리 이름을 소문자로 모은다.
                for (int i = 0; i < names.length; i++) {
                    installed.add(names[i].toLowerCase(Locale.ENGLISH));
                }
            } catch (RuntimeException e) {
                installed.clear();
            }
        }
        return installed.contains(name.toLowerCase(Locale.ENGLISH)) ? new Font(name, Font.PLAIN, 1) : null;
    }
}
