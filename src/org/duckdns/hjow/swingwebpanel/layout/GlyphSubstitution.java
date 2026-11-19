package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Glyph alternates of a web font without OpenType support in Java2D (plan item P10-08-h, CSS Fonts 4 sections 6.8 and 6.9): the JRE draws the glyph that the
 * <code>cmap</code> table gives for a character and cannot turn OpenType features on, so this class reads the <code>GSUB</code> table of the font itself,
 * applies the single substitutions (lookup type 1) and alternate substitutions (lookup type 3, the alternate number given by the feature value, 1 for
 * the first) of the requested features to every glyph that a character maps to, and writes a copy of the font whose <code>cmap</code> maps the characters
 * directly to the substituted glyphs. Loaded with {@link java.awt.Font#createFont}, that copy is measured and painted like any font, so a style set, a
 * stylistic alternate, a character variant, a swash, an ornament or an annotation of <code>font-variant-alternates</code> (and the same tags of
 * <code>font-feature-settings</code>) appears on the screen.
 * <p>
 * Limits: only features whose lookups are of the types 1, 3 and 7 (extension) are applied, and they act on single characters (no context, no ligature);
 * the features are found through the default language system of the <code>DFLT</code> or <code>latn</code> script, else in the feature list; a font of
 * the system has no bytes the JRE gives out, so this works only for web fonts (<code>@font-face</code>).
 * <p>
 * Java2D 의 OpenType 지원 없이 웹 글꼴의 대체 글리프를 쓰게 한다(계획 P10-08-h, CSS Fonts 4 6.8, 6.9 절): JRE 는 문자에 대해 <code>cmap</code> 표가 주는 글리프를 그리고
 * OpenType 기능을 켤 수 없으므로, 이 클래스가 글꼴의 <code>GSUB</code> 표를 직접 읽어 요청한 기능의 단일 치환(조회 종류 1)과 대체 치환(조회 종류 3, 대체 번호는 기능 값이며
 * 첫째가 1)을 문자가 가리키는 모든 글리프에 적용하고, <code>cmap</code> 이 문자를 치환된 글리프로 곧바로 가리키는 글꼴 사본을 쓴다. 그 사본을
 * {@link java.awt.Font#createFont} 로 읽으면 여느 글꼴처럼 재고 그리므로 <code>font-variant-alternates</code> 의 스타일 세트, 스타일 대체, 문자 변형, 스와시, 장식, 주석
 * (그리고 <code>font-feature-settings</code> 의 같은 태그)이 화면에 나타난다.
 * <p>
 * 한계: 조회가 종류 1, 3, 7(확장)인 기능만 적용하며 문자 하나씩에 작용한다(문맥과 합자 없음). 기능은 <code>DFLT</code> 나 <code>latn</code> 스크립트의 기본 언어 체계로 찾고,
 * 없으면 기능 목록에서 찾는다. 시스템 글꼴은 JRE 가 바이트를 내주지 않으므로 웹 글꼴(<code>@font-face</code>)에만 동작한다.
 */
final class GlyphSubstitution {

    /**
     * Not instantiable; the class only offers static methods.
     * <p>
     * 인스턴스를 만들지 않는다. 정적 메서드만 제공한다.
     */
    private GlyphSubstitution() {
    }

    /**
     * Returns a copy of a TrueType font whose characters map to the glyphs that the features select, or null if the features change no glyph (no
     * <code>GSUB</code>, no such feature, or a font that cannot be read).
     * <p>
     * 기능이 고르는 글리프로 문자를 가리키는 TrueType 글꼴의 사본을 반환한다. 기능이 바꾸는 글리프가 없으면(<code>GSUB</code> 가 없거나, 그런 기능이 없거나, 읽을 수 없는 글꼴) null.
     *
     * @param sfnt     the font file / 글꼴 파일
     * @param features the feature tags and their values (0 turns a feature off) / 기능 태그와 값(0 은 끔)
     * @return the new font file, or null / 새 글꼴 파일. 없으면 null
     */
    static byte[] apply(byte[] sfnt, Map<String, Integer> features) {
        try {
            int[] cmap = table(sfnt, "cmap");
            int[] gsub = table(sfnt, "GSUB");
            if (cmap == null || gsub == null) {
                return null;
            }
            TreeMap<Integer, Integer> map = readCmap(sfnt, cmap[0]);
            if (map.isEmpty()) {
                return null;
            }
            List<int[]> lookups = lookups(sfnt, gsub[0], features);
            if (lookups.isEmpty()) {
                return null;
            }
            boolean changed = false;
            TreeMap<Integer, Integer> out = new TreeMap<Integer, Integer>();
            // 문자마다 글리프에 조회를 차례로 적용한다.
            for (Map.Entry<Integer, Integer> e : map.entrySet()) {
                int g = e.getValue().intValue();
                int ng = g;
                // 기능의 조회를 조회 목록 순서로 적용한다.
                for (int i = 0; i < lookups.size(); i++) {
                    ng = substitute(sfnt, gsub[0], lookups.get(i), ng);
                }
                changed |= ng != g;
                out.put(e.getKey(), Integer.valueOf(ng));
            }
            return changed ? replaceTable(sfnt, "cmap", writeCmap(out)) : null;
        } catch (RuntimeException e) {
            // 손상된 표는 읽지 않은 것으로 본다(글꼴은 기능 없이 쓴다).
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 바이트 읽기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads an unsigned 16 bit number.
     * <p>
     * 부호 없는 16 비트 수를 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param off the offset / 위치
     * @return the number / 수
     */
    private static int u16(byte[] b, int off) {
        if (off < 0 || off + 2 > b.length) {
            throw new IllegalArgumentException("outside the font");
        }
        return ((b[off] & 255) << 8) | (b[off + 1] & 255);
    }

    /**
     * Reads a signed 16 bit number.
     * <p>
     * 부호 있는 16 비트 수를 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param off the offset / 위치
     * @return the number / 수
     */
    private static int s16(byte[] b, int off) {
        return (short) u16(b, off);
    }

    /**
     * Reads a 32 bit number.
     * <p>
     * 32 비트 수를 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param off the offset / 위치
     * @return the number / 수
     */
    private static int u32(byte[] b, int off) {
        return (u16(b, off) << 16) | u16(b, off + 2);
    }

    /**
     * Reads a tag of four characters.
     * <p>
     * 네 글자 태그를 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param off the offset / 위치
     * @return the tag / 태그
     */
    private static String tag(byte[] b, int off) {
        if (off + 4 > b.length) {
            throw new IllegalArgumentException("outside the font");
        }
        return new String(new char[] { (char) (b[off] & 255), (char) (b[off + 1] & 255), (char) (b[off + 2] & 255), (char) (b[off + 3] & 255) });
    }

    /**
     * Finds a table of the font.
     * <p>
     * 글꼴의 표를 찾는다.
     *
     * @param b   the font / 글꼴
     * @param tag the tag / 태그
     * @return {offset, length} or null / {위치, 길이} 또는 null
     */
    static int[] table(byte[] b, String tag) {
        int n = u16(b, 4);
        // 표 목록에서 태그를 찾는다.
        for (int i = 0; i < n; i++) {
            int r = 12 + 16 * i;
            if (tag(b, r).equals(tag)) {
                return new int[] { u32(b, r + 8), u32(b, r + 12) };
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // cmap
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads the Unicode mapping of the <code>cmap</code> table: the format 12 subtable of (3,10) or (0,4..6) if there is one, else the format 4 subtable of
     * (3,1) or (0,0..3).
     * <p>
     * <code>cmap</code> 표의 유니코드 대응을 읽는다: (3,10) 이나 (0,4~6) 의 형식 12 부표가 있으면 그것을, 없으면 (3,1) 이나 (0,0~3) 의 형식 4 부표를 읽는다.
     *
     * @param b    the font / 글꼴
     * @param base the offset of the table / 표의 위치
     * @return the characters and their glyphs / 문자와 글리프
     */
    private static TreeMap<Integer, Integer> readCmap(byte[] b, int base) {
        int n = u16(b, base + 2);
        int f12 = -1;
        int f4 = -1;
        // 인코딩 기록마다 유니코드 부표를 찾는다.
        for (int i = 0; i < n; i++) {
            int r = base + 4 + 8 * i;
            int platform = u16(b, r);
            int encoding = u16(b, r + 2);
            int sub = base + u32(b, r + 4);
            int format = u16(b, sub);
            boolean unicode = platform == 0 || (platform == 3 && (encoding == 1 || encoding == 10));
            if (unicode && format == 12 && f12 < 0) {
                f12 = sub;
            } else if (unicode && format == 4 && f4 < 0) {
                f4 = sub;
            }
        }
        TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
        if (f12 >= 0) {
            int groups = u32(b, f12 + 12);
            // 그룹마다 문자 범위를 연속된 글리프에 대응시킨다.
            for (int i = 0; i < groups && i < 1 << 20; i++) {
                int g = f12 + 16 + 12 * i;
                int start = u32(b, g);
                int end = u32(b, g + 4);
                int glyph = u32(b, g + 8);
                // 범위의 문자를 하나씩 넣는다(범위는 유니코드 안으로 한정한다).
                for (int c = start; c <= end && c <= 0x10FFFF && c >= 0; c++) {
                    map.put(Integer.valueOf(c), Integer.valueOf(glyph + (c - start)));
                }
            }
            return map;
        }
        if (f4 >= 0) {
            int segX2 = u16(b, f4 + 6);
            int ends = f4 + 14;
            int starts = ends + segX2 + 2;
            int deltas = starts + segX2;
            int ranges = deltas + segX2;
            // 구간마다 문자를 글리프로 바꾼다.
            for (int s = 0; s < segX2 / 2; s++) {
                int end = u16(b, ends + 2 * s);
                int start = u16(b, starts + 2 * s);
                int delta = s16(b, deltas + 2 * s);
                int ro = u16(b, ranges + 2 * s);
                // 구간의 문자를 하나씩 본다(0xFFFF 는 끝 표시).
                for (int c = start; c <= end && c < 0xFFFF; c++) {
                    int g;
                    if (ro == 0) {
                        g = (c + delta) & 0xFFFF;
                    } else {
                        int at = ranges + 2 * s + ro + 2 * (c - start);
                        g = u16(b, at);
                        g = g == 0 ? 0 : (g + delta) & 0xFFFF;
                    }
                    if (g != 0) {
                        map.put(Integer.valueOf(c), Integer.valueOf(g));
                    }
                }
            }
        }
        return map;
    }

    /**
     * Writes a <code>cmap</code> table with a format 4 subtable of the Basic Multilingual Plane (platform 3, encoding 1; left out if it would not fit) and a
     * format 12 subtable of all characters (platform 3, encoding 10).
     * <p>
     * 기본 다국어 평면의 형식 4 부표(플랫폼 3, 인코딩 1. 들어가지 않으면 뺌)와 모든 문자의 형식 12 부표(플랫폼 3, 인코딩 10)를 가진 <code>cmap</code> 표를 쓴다.
     *
     * @param map the characters and their glyphs / 문자와 글리프
     * @return the table / 표
     */
    private static byte[] writeCmap(TreeMap<Integer, Integer> map) {
        byte[] f4 = format4(map);
        // 형식 12: 연속된 문자가 연속된 글리프를 가리키는 그룹으로 나눈다.
        List<int[]> groups = new ArrayList<int[]>();
        int[] cur = null;
        // 문자를 차례로 보며 앞 그룹에 이어지면 늘리고 아니면 새 그룹을 연다.
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            int c = e.getKey().intValue();
            int g = e.getValue().intValue();
            if (cur != null && c == cur[1] + 1 && g == cur[2] + (c - cur[0])) {
                cur[1] = c;
            } else {
                cur = new int[] { c, c, g };
                groups.add(cur);
            }
        }
        int f12Len = 16 + 12 * groups.size();
        int records = f4 != null ? 2 : 1;
        int header = 4 + 8 * records;
        ByteWriter w = new ByteWriter(header + (f4 != null ? f4.length : 0) + f12Len);
        w.u16(0);
        w.u16(records);
        int at = header;
        if (f4 != null) {
            w.u16(3);
            w.u16(1);
            w.u32(at);
            at += f4.length;
        }
        w.u16(3);
        w.u16(10);
        w.u32(at);
        if (f4 != null) {
            w.bytes(f4);
        }
        w.u16(12);
        w.u16(0);
        w.u32(f12Len);
        w.u32(0);
        w.u32(groups.size());
        // 그룹을 차례로 적는다.
        for (int i = 0; i < groups.size(); i++) {
            w.u32(groups.get(i)[0]);
            w.u32(groups.get(i)[1]);
            w.u32(groups.get(i)[2]);
        }
        return w.out;
    }

    /**
     * Writes a format 4 subtable of the characters of the Basic Multilingual Plane: one segment per run of consecutive characters, with
     * <code>idDelta</code> when the glyphs are consecutive too and with the glyph array otherwise.
     * <p>
     * 기본 다국어 평면 문자의 형식 4 부표를 쓴다: 연속된 문자 덩어리마다 구간 하나이며, 글리프도 연속이면 <code>idDelta</code> 로, 아니면 글리프 배열로 적는다.
     *
     * @param map the characters and their glyphs / 문자와 글리프
     * @return the subtable, or null if it would be larger than 65535 bytes / 부표. 65535 바이트보다 크면 null
     */
    private static byte[] format4(TreeMap<Integer, Integer> map) {
        List<int[]> runs = new ArrayList<int[]>();
        int[] cur = null;
        // 0xFFFF 보다 작은 문자를 연속된 덩어리로 묶는다.
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            int c = e.getKey().intValue();
            if (c >= 0xFFFF) {
                break;
            }
            if (cur != null && c == cur[1] + 1) {
                cur[1] = c;
            } else {
                cur = new int[] { c, c };
                runs.add(cur);
            }
        }
        int seg = runs.size() + 1;
        List<Integer> glyphArray = new ArrayList<Integer>();
        int[] deltas = new int[seg];
        int[] offsets = new int[seg];
        int[] arrayStart = new int[seg];
        // 덩어리마다 글리프가 연속인지 보고 idDelta 나 글리프 배열을 정한다.
        for (int s = 0; s < runs.size(); s++) {
            int start = runs.get(s)[0];
            int end = runs.get(s)[1];
            int first = map.get(Integer.valueOf(start)).intValue();
            boolean consecutive = true;
            // 덩어리의 글리프가 하나씩 늘어나는지 확인한다.
            for (int c = start; c <= end && consecutive; c++) {
                consecutive = map.get(Integer.valueOf(c)).intValue() == first + (c - start);
            }
            if (consecutive) {
                deltas[s] = (first - start) & 0xFFFF;
                offsets[s] = 0;
            } else {
                deltas[s] = 0;
                arrayStart[s] = glyphArray.size();
                offsets[s] = -1;
                // 덩어리의 글리프를 배열에 넣는다.
                for (int c = start; c <= end; c++) {
                    glyphArray.add(map.get(Integer.valueOf(c)));
                }
            }
        }
        deltas[seg - 1] = 1;
        int length = 16 + 8 * seg + 2 * glyphArray.size();
        if (length > 0xFFFF || seg > 0x7FFF) {
            return null;
        }
        ByteWriter w = new ByteWriter(length);
        int pow = 1;
        int log = 0;
        // searchRange 와 entrySelector 를 구한다.
        while (pow * 2 <= seg) {
            pow *= 2;
            log++;
        }
        w.u16(4);
        w.u16(length);
        w.u16(0);
        w.u16(seg * 2);
        w.u16(pow * 2);
        w.u16(log);
        w.u16(seg * 2 - pow * 2);
        // 구간마다 끝 문자(endCode)를 적는다.
        for (int s = 0; s < runs.size(); s++) {
            w.u16(runs.get(s)[1]);
        }
        w.u16(0xFFFF);
        w.u16(0);
        // 구간마다 시작 문자(startCode)를 적는다.
        for (int s = 0; s < runs.size(); s++) {
            w.u16(runs.get(s)[0]);
        }
        w.u16(0xFFFF);
        // 구간마다 글리프 차이(idDelta)를 적는다.
        for (int s = 0; s < seg; s++) {
            w.u16(deltas[s]);
        }
        // idRangeOffset: 글리프 배열을 쓰는 구간은 자기 자리에서 배열의 시작까지 거리이다.
        for (int s = 0; s < seg; s++) {
            if (offsets[s] < 0) {
                w.u16(2 * (seg - s) + 2 * arrayStart[s]);
            } else {
                w.u16(0);
            }
        }
        // 글리프 배열(glyphIdArray)을 적는다.
        for (int i = 0; i < glyphArray.size(); i++) {
            w.u16(glyphArray.get(i).intValue());
        }
        return w.out;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // GSUB
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Finds the lookups of the requested features, in the order of the lookup list: every lookup is {lookup offset, type, value of the feature}.
     * <p>
     * 요청한 기능의 조회를 조회 목록 순서로 찾는다: 조회마다 {조회 위치, 종류, 기능의 값}.
     *
     * @param b        the font / 글꼴
     * @param gsub     the offset of the <code>GSUB</code> table / <code>GSUB</code> 표의 위치
     * @param features the features / 기능
     * @return the lookups / 조회
     */
    private static List<int[]> lookups(byte[] b, int gsub, Map<String, Integer> features) {
        int scriptList = gsub + u16(b, gsub + 4);
        int featureList = gsub + u16(b, gsub + 6);
        int lookupList = gsub + u16(b, gsub + 8);
        int featureCount = u16(b, featureList);
        List<Integer> allowed = defaultFeatures(b, scriptList);
        TreeMap<Integer, Integer> chosen = new TreeMap<Integer, Integer>();
        // 기능 기록마다 요청한 태그이고 기본 언어 체계에 있으면(없으면 아무 기록이나) 그 조회를 모은다.
        for (int i = 0; i < featureCount; i++) {
            int rec = featureList + 2 + 6 * i;
            String t = tag(b, rec);
            Integer value = features.get(t);
            if (value == null || value.intValue() <= 0 || (allowed != null && !allowed.contains(Integer.valueOf(i)))) {
                continue;
            }
            int feature = featureList + u16(b, rec + 4);
            int count = u16(b, feature + 2);
            // 기능이 가리키는 조회 번호를 모은다.
            for (int k = 0; k < count; k++) {
                chosen.put(Integer.valueOf(u16(b, feature + 4 + 2 * k)), value);
            }
        }
        List<int[]> out = new ArrayList<int[]>();
        int lookupCount = u16(b, lookupList);
        // 조회 목록 순서로 조회의 위치와 종류를 적는다.
        for (Map.Entry<Integer, Integer> e : chosen.entrySet()) {
            int index = e.getKey().intValue();
            if (index >= lookupCount) {
                continue;
            }
            int lookup = lookupList + u16(b, lookupList + 2 + 2 * index);
            out.add(new int[] { lookup, u16(b, lookup), e.getValue().intValue() });
        }
        return out;
    }

    /**
     * Returns the feature indexes of the default language system of the <code>DFLT</code> script, else of the <code>latn</code> script, or null if there
     * is neither (then every feature record may be used).
     * <p>
     * <code>DFLT</code> 스크립트, 없으면 <code>latn</code> 스크립트의 기본 언어 체계의 기능 번호를 반환한다. 둘 다 없으면 null(그러면 모든 기능 기록을 쓸 수 있다).
     *
     * @param b          the font / 글꼴
     * @param scriptList the offset of the script list / 스크립트 목록의 위치
     * @return the feature indexes or null / 기능 번호 또는 null
     */
    private static List<Integer> defaultFeatures(byte[] b, int scriptList) {
        int n = u16(b, scriptList);
        String[] prefer = { "DFLT", "latn" };
        // 선호하는 스크립트를 차례로 찾는다.
        for (int p = 0; p < prefer.length; p++) {
            // 스크립트 기록에서 태그를 찾는다.
            for (int i = 0; i < n; i++) {
                int rec = scriptList + 2 + 6 * i;
                if (!tag(b, rec).equals(prefer[p])) {
                    continue;
                }
                int script = scriptList + u16(b, rec + 4);
                int dflt = u16(b, script);
                if (dflt == 0) {
                    continue;
                }
                int langSys = script + dflt;
                List<Integer> out = new ArrayList<Integer>();
                int required = u16(b, langSys + 2);
                if (required != 0xFFFF) {
                    out.add(Integer.valueOf(required));
                }
                int count = u16(b, langSys + 4);
                // 언어 체계가 가리키는 기능 번호를 모은다.
                for (int k = 0; k < count; k++) {
                    out.add(Integer.valueOf(u16(b, langSys + 6 + 2 * k)));
                }
                return out;
            }
        }
        return null;
    }

    /**
     * Applies one lookup to a glyph: the first subtable whose coverage contains the glyph substitutes it (type 1: by a delta or a substitute array; type 3:
     * the alternate number <code>value</code>, 1 for the first; type 7: the extension subtable of type 1 or 3).
     * <p>
     * 조회 하나를 글리프에 적용한다: 덮는 범위에 글리프가 있는 첫 부표가 치환한다(종류 1: 차이나 치환 배열, 종류 3: 대체 번호 <code>value</code>, 첫째가 1, 종류 7: 종류 1 이나 3 의
     * 확장 부표).
     *
     * @param b      the font / 글꼴
     * @param gsub   the offset of the <code>GSUB</code> table / <code>GSUB</code> 표의 위치
     * @param lookup {offset, type, value} / {위치, 종류, 값}
     * @param glyph  the glyph / 글리프
     * @return the substituted glyph, or the glyph itself / 치환된 글리프. 없으면 글리프 자신
     */
    private static int substitute(byte[] b, int gsub, int[] lookup, int glyph) {
        int at = lookup[0];
        int type = lookup[1];
        int subtables = u16(b, at + 4);
        // 부표를 차례로 보며 글리프를 덮는 첫 부표를 적용한다.
        for (int i = 0; i < subtables; i++) {
            int sub = at + u16(b, at + 6 + 2 * i);
            int t = type;
            if (t == 7) {
                // 확장 부표는 실제 종류와 32 비트 위치를 담는다.
                t = u16(b, sub + 2);
                sub = sub + u32(b, sub + 4);
            }
            int format = u16(b, sub);
            int index = coverage(b, sub + u16(b, sub + 2), glyph);
            if (index < 0) {
                continue;
            }
            if (t == 1 && format == 1) {
                return (glyph + s16(b, sub + 4)) & 0xFFFF;
            }
            if (t == 1 && format == 2) {
                return index < u16(b, sub + 4) ? u16(b, sub + 6 + 2 * index) : glyph;
            }
            if (t == 3 && format == 1) {
                if (index >= u16(b, sub + 4)) {
                    return glyph;
                }
                int set = sub + u16(b, sub + 6 + 2 * index);
                int count = u16(b, set);
                int choice = lookup[2] - 1;
                return choice >= 0 && choice < count ? u16(b, set + 2 + 2 * choice) : glyph;
            }
            return glyph;
        }
        return glyph;
    }

    /**
     * Returns the coverage index of a glyph (format 1: a sorted glyph array; format 2: ranges with a start index), or -1.
     * <p>
     * 글리프의 덮는 범위 번호를 반환한다(형식 1: 정렬된 글리프 배열, 형식 2: 시작 번호가 있는 범위). 없으면 -1.
     *
     * @param b     the font / 글꼴
     * @param cov   the offset of the coverage table / 덮는 범위 표의 위치
     * @param glyph the glyph / 글리프
     * @return the index or -1 / 번호 또는 -1
     */
    private static int coverage(byte[] b, int cov, int glyph) {
        int format = u16(b, cov);
        int count = u16(b, cov + 2);
        if (format == 1) {
            // 글리프 배열에서 찾는다.
            for (int i = 0; i < count; i++) {
                if (u16(b, cov + 4 + 2 * i) == glyph) {
                    return i;
                }
            }
            return -1;
        }
        if (format == 2) {
            // 범위 기록에서 찾는다.
            for (int i = 0; i < count; i++) {
                int r = cov + 4 + 6 * i;
                int start = u16(b, r);
                int end = u16(b, r + 2);
                if (glyph >= start && glyph <= end) {
                    return u16(b, r + 4) + glyph - start;
                }
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 글꼴 다시 쓰기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Writes a copy of a font with one table replaced: the table directory, the four byte alignment, the table checksums and the checksum adjustment of the
     * <code>head</code> table are computed again.
     * <p>
     * 표 하나를 바꾼 글꼴 사본을 쓴다: 표 목록, 4 바이트 정렬, 표 체크섬, <code>head</code> 표의 체크섬 보정을 다시 계산한다.
     *
     * @param b     the font / 글꼴
     * @param tag   the tag of the table / 표의 태그
     * @param table the new content / 새 내용
     * @return the new font / 새 글꼴
     */
    static byte[] replaceTable(byte[] b, String tag, byte[] table) {
        int n = u16(b, 4);
        TreeSet<String> tags = new TreeSet<String>();
        TreeMap<String, byte[]> data = new TreeMap<String, byte[]>();
        // 표마다 내용을 꺼낸다(바꿀 표는 새 내용으로).
        for (int i = 0; i < n; i++) {
            int r = 12 + 16 * i;
            String t = tag(b, r);
            int off = u32(b, r + 8);
            int len = u32(b, r + 12);
            byte[] content;
            if (t.equals(tag)) {
                content = table;
            } else {
                if (off < 0 || len < 0 || off + len > b.length) {
                    throw new IllegalArgumentException("table outside the font");
                }
                content = new byte[len];
                System.arraycopy(b, off, content, 0, len);
            }
            tags.add(t);
            data.put(t, content);
        }
        int total = 12 + 16 * n;
        // 표마다 4 바이트로 정렬한 크기를 더한다.
        for (String t : tags) {
            total += (data.get(t).length + 3) & ~3;
        }
        ByteWriter w = new ByteWriter(total);
        w.u32(u32(b, 0));
        w.u16(n);
        int pow = 1;
        int log = 0;
        // searchRange 와 entrySelector 를 구한다.
        while (pow * 2 <= n) {
            pow *= 2;
            log++;
        }
        w.u16(pow * 16);
        w.u16(log);
        w.u16(n * 16 - pow * 16);
        int at = 12 + 16 * n;
        int headAt = -1;
        // 태그 순서로 표 목록을 적는다.
        for (String t : tags) {
            byte[] c = data.get(t);
            if (t.equals("head") && c.length >= 12) {
                // 체크섬 보정은 0 으로 두고 계산한다.
                c = c.clone();
                c[8] = 0;
                c[9] = 0;
                c[10] = 0;
                c[11] = 0;
                data.put(t, c);
                headAt = at;
            }
            w.bytes(t.getBytes(java.nio.charset.Charset.forName("ISO-8859-1")));
            w.u32(checksum(c, 0, c.length));
            w.u32(at);
            w.u32(c.length);
            at += (c.length + 3) & ~3;
        }
        // 표 내용을 4 바이트 정렬로 적는다.
        for (String t : tags) {
            byte[] c = data.get(t);
            w.bytes(c);
            w.pad();
        }
        if (headAt >= 0) {
            int adjust = (int) (0xB1B0AFBAL - (checksum(w.out, 0, w.out.length) & 0xFFFFFFFFL));
            w.out[headAt + 8] = (byte) (adjust >>> 24);
            w.out[headAt + 9] = (byte) (adjust >>> 16);
            w.out[headAt + 10] = (byte) (adjust >>> 8);
            w.out[headAt + 11] = (byte) adjust;
        }
        return w.out;
    }

    /**
     * Computes the checksum of a table (the sum of its 32 bit words, the last one padded with zeros).
     * <p>
     * 표의 체크섬(32 비트 낱말의 합, 마지막은 0 으로 채움)을 계산한다.
     *
     * @param b   the bytes / 바이트
     * @param off the start / 시작
     * @param len the length / 길이
     * @return the checksum / 체크섬
     */
    private static int checksum(byte[] b, int off, int len) {
        long sum = 0;
        // 4 바이트 낱말을 더한다.
        for (int i = 0; i < len; i += 4) {
            long word = 0;
            // 낱말의 네 바이트를 모은다(넘치면 0).
            for (int k = 0; k < 4; k++) {
                word = (word << 8) | (i + k < len ? b[off + i + k] & 255 : 0);
            }
            sum = (sum + word) & 0xFFFFFFFFL;
        }
        return (int) sum;
    }

    /**
     * A writer of big-endian numbers into a fixed array.
     * <p>
     * 고정 배열에 빅 엔디언 수를 쓰는 쪽.
     */
    private static final class ByteWriter {
        /** The output. 출력. */
        final byte[] out;
        /** The position. 위치. */
        int pos;

        /**
         * Creates a writer.
         * <p>
         * 쓰는 쪽을 만든다.
         *
         * @param size the size / 크기
         */
        ByteWriter(int size) {
            out = new byte[size];
        }

        /**
         * Writes 16 bits.
         * <p>
         * 16 비트를 쓴다.
         *
         * @param v the value / 값
         */
        void u16(int v) {
            out[pos++] = (byte) (v >>> 8);
            out[pos++] = (byte) v;
        }

        /**
         * Writes 32 bits.
         * <p>
         * 32 비트를 쓴다.
         *
         * @param v the value / 값
         */
        void u32(int v) {
            u16(v >>> 16);
            u16(v & 0xFFFF);
        }

        /**
         * Writes bytes.
         * <p>
         * 바이트를 쓴다.
         *
         * @param b the bytes / 바이트
         */
        void bytes(byte[] b) {
            System.arraycopy(b, 0, out, pos, b.length);
            pos += b.length;
        }

        /**
         * Pads with zeros to a multiple of four.
         * <p>
         * 4 의 배수가 되도록 0 을 채운다.
         */
        void pad() {
            // 4 의 배수가 될 때까지 0 을 쓴다.
            while ((pos & 3) != 0) {
                out[pos++] = 0;
            }
        }
    }
}
