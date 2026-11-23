package org.duckdns.hjow.swingwebpanel.render;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;

/**
 * Chooses the source of an <code>img</code> element (plan item P18-05): the <code>src</code> attribute, the <code>srcset</code> and
 * <code>sizes</code> attributes, and the <code>source</code> siblings in front of it when its parent is a <code>picture</code> element. The
 * algorithm is "update the source set" and "select an image source from a source set" of the HTML Standard (the image-selection parts that
 * the W3C HTML5 2014 recommendation lacks, from HTML 5.1 / the WHATWG HTML Living Standard): a <code>source</code> is skipped when it has no
 * <code>srcset</code>, when its <code>type</code> is not an image type that this component decodes, or when its <code>media</code> query does
 * not match; the first usable one provides the source set. The candidates carry a density (<code>x</code>) or a width (<code>w</code>)
 * descriptor; widths are turned into densities with the source size that <code>sizes</code> gives (default <code>100vw</code>). The chosen
 * candidate is the smallest density that is not lower than the device pixel ratio of the environment, or else the largest density. The class
 * has no state; the renderer, the layout resolver and the script image loader call it with the same environment so that all of them agree on
 * the same <code>currentSrc</code>.
 * <p>
 * <code>img</code> 요소의 소스를 고른다(계획 P18-05): <code>src</code> 속성, <code>srcset</code> 과 <code>sizes</code> 속성, 그리고 부모가
 * <code>picture</code> 이면 앞에 있는 <code>source</code> 형제들. 알고리즘은 HTML 표준의 "update the source set" 과 "select an image source
 * from a source set" 이다(W3C HTML5 2014 권고안에 없는 이미지 선택 부분은 HTML 5.1/WHATWG HTML Living Standard 를 따른다). <code>source</code> 는
 * <code>srcset</code> 이 없거나, <code>type</code> 이 이 컴포넌트가 디코딩하지 않는 이미지 형식이거나, <code>media</code> 질의가 맞지 않으면 건너뛰고
 * 처음 쓸 수 있는 것이 소스 집합을 제공한다. 후보는 밀도(<code>x</code>) 또는 너비(<code>w</code>) 기술자를 갖고, 너비는 <code>sizes</code> 가 주는
 * 소스 크기(기본 <code>100vw</code>)로 밀도로 바꾼다. 고르는 후보는 환경의 장치 픽셀 비율 이상인 가장 작은 밀도, 없으면 가장 큰 밀도이다. 이 클래스는
 * 상태가 없다. 렌더러, 레이아웃 리졸버, 스크립트의 이미지 로더가 같은 환경으로 부르므로 모두 같은 <code>currentSrc</code> 에 합의한다.
 */
public final class ImageSelection {

    /**
     * The result of a selection: the chosen URL as written in the document and its density.
     * <p>
     * 선택의 결과: 문서에 적힌 그대로의 선택된 URL 과 그 밀도.
     */
    public static final class Choice {
        /** The chosen URL as written (not yet resolved). / 적힌 그대로의 선택된 URL(아직 해석 전). */
        private final String url;
        /** The density of the chosen candidate (1 for a plain src). / 선택된 후보의 밀도(일반 src 는 1). */
        private final double density;

        /**
         * Creates a choice.
         * <p>
         * 선택 결과를 만든다.
         *
         * @param url     the chosen URL / 선택된 URL
         * @param density its density / 그 밀도
         */
        Choice(String url, double density) {
            this.url = url;
            this.density = density;
        }

        /**
         * Returns the chosen URL as written in the document.
         * <p>
         * 문서에 적힌 그대로의 선택된 URL 을 반환한다.
         *
         * @return the URL / URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * Returns the density of the chosen candidate; the natural size of the image is its pixel size divided by it.
         * <p>
         * 선택된 후보의 밀도를 반환한다. 이미지의 자연 크기는 픽셀 크기를 이 값으로 나눈 것이다.
         *
         * @return the density (greater than 0) / 밀도(0 초과)
         */
        public double getDensity() {
            return density;
        }
    }

    /**
     * One candidate of a <code>srcset</code> attribute.
     * <p>
     * <code>srcset</code> 속성의 후보 하나.
     */
    private static final class Candidate {
        /** The URL as written. / 적힌 URL. */
        final String url;
        /** The density from an x descriptor, or 1 when there is no descriptor; 0 when the width is used. / x 기술자의 밀도(없으면 1), 너비를 쓰면 0. */
        final double density;
        /** The width from a w descriptor, or 0. / w 기술자의 너비, 없으면 0. */
        final double width;

        /**
         * Creates a candidate.
         * <p>
         * 후보를 만든다.
         *
         * @param url     the URL / URL
         * @param density the density or 0 / 밀도 또는 0
         * @param width   the width or 0 / 너비 또는 0
         */
        Candidate(String url, double density, double width) {
            this.url = url;
            this.density = density;
            this.width = width;
        }
    }

    /**
     * The font size that relative lengths in <code>sizes</code> are computed with, in px.
     * <p>
     * <code>sizes</code> 의 상대 길이를 계산하는 글꼴 크기(px).
     */
    private static final double BASE_FONT_PX = 16;

    /**
     * Not instantiable; the class only offers static methods.
     * <p>
     * 인스턴스를 만들 수 없다. 정적 메서드만 제공한다.
     */
    private ImageSelection() {
    }

    /**
     * Selects the source of an image element for a viewport. The environment decides the media queries, the <code>vw</code> units of
     * <code>sizes</code> and the device pixel ratio.
     * <p>
     * 뷰포트에 대한 이미지 요소의 소스를 고른다. 환경이 미디어 질의, <code>sizes</code> 의 <code>vw</code> 단위, 장치 픽셀 비율을 정한다.
     *
     * @param img the <code>img</code> element / <code>img</code> 요소
     * @param env the media environment (viewport and resolution) / 미디어 환경(뷰포트와 해상도)
     * @return the choice, or null when the element names no image / 선택 결과. 요소가 이미지를 가리키지 않으면 null
     */
    public static Choice select(Element img, MediaEnvironment env) {
        String srcset = null;
        String sizes = null;
        Node parent = img.getParentNode();
        // picture 안이면 img 앞의 source 형제를 차례로 보고 처음 쓸 수 있는 것의 srcset/sizes 를 쓴다.
        if (parent instanceof Element && "picture".equals(localName((Element) parent))) {
            for (Node n = parent.getFirstChild(); n != null && n != img; n = n.getNextSibling()) {
                if (!(n instanceof Element) || !"source".equals(localName((Element) n))) {
                    continue;
                }
                Element source = (Element) n;
                String set = source.getAttribute("srcset");
                if (set == null || set.trim().length() == 0) {
                    continue;
                }
                String type = source.getAttribute("type");
                if (type != null && type.trim().length() > 0 && !ImageDecoders.isSupportedMime(type.trim())) {
                    continue;
                }
                String media = source.getAttribute("media");
                if (media != null && media.trim().length() > 0 && !MediaQueryList.parse(media).matches(env)) {
                    continue;
                }
                srcset = set;
                sizes = source.getAttribute("sizes");
                break;
            }
        }
        boolean fromSource = srcset != null;
        if (!fromSource) {
            srcset = img.getAttribute("srcset");
            sizes = img.getAttribute("sizes");
        }
        List<Candidate> candidates = srcset == null ? new ArrayList<Candidate>() : parseSrcset(srcset);
        String src = img.getAttribute("src");
        // img 자신의 소스 집합에서는 src 도 1x 후보가 된다(밀도 1 후보가 따로 없을 때).
        if (!fromSource && src != null && src.trim().length() > 0) {
            boolean hasOne = false;
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                // 밀도 1 후보가 이미 있거나 너비 기술자 후보가 있으면 src 는 후보에 더하지 않는다.
                if (c.width > 0 || c.density == 1) {
                    hasOne = true;
                }
            }
            if (!hasOne) {
                candidates.add(new Candidate(src.trim(), 1, 0));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        double sourceSize = hasWidthDescriptors(candidates) ? sourceSize(sizes, env) : 0;
        return choose(candidates, sourceSize, env.getResolution() <= 0 ? 1 : env.getResolution());
    }

    /**
     * Tells whether a candidate list uses width descriptors.
     * <p>
     * 후보 목록이 너비 기술자를 쓰는지 알려 준다.
     *
     * @param candidates the candidates / 후보들
     * @return true if one has a width / 하나라도 너비가 있으면 true
     */
    private static boolean hasWidthDescriptors(List<Candidate> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            // 후보 하나라도 w 기술자가 있으면 sizes 가 필요하다.
            if (candidates.get(i).width > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Picks the candidate for a device pixel ratio: the smallest density that is not below it, or else the largest density.
     * <p>
     * 장치 픽셀 비율에 맞는 후보를 고른다: 비율 이상인 가장 작은 밀도, 없으면 가장 큰 밀도.
     *
     * @param candidates the candidates / 후보들
     * @param sourceSize the source size for width descriptors, in px / 너비 기술자에 쓰는 소스 크기(px)
     * @param ratio      the device pixel ratio / 장치 픽셀 비율
     * @return the choice / 선택 결과
     */
    private static Choice choose(List<Candidate> candidates, double sourceSize, double ratio) {
        Candidate best = null;
        double bestDensity = 0;
        Candidate largest = null;
        double largestDensity = 0;
        // 후보를 모두 보며 밀도를 구하고 목표 비율 이상 중 가장 작은 것과 가장 큰 것을 갱신한다.
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            // 소스 크기를 알 수 없으면(뷰포트 0) 너비 후보도 1x 로 본다.
            double density = c.width > 0 ? (sourceSize > 0 ? c.width / sourceSize : 1) : c.density;
            // 밀도를 알 수 없는 후보(소스 크기가 0)는 건너뛴다.
            if (density <= 0) {
                continue;
            }
            if (largest == null || density > largestDensity) {
                largest = c;
                largestDensity = density;
            }
            // 비율 이상 중 가장 작은 밀도. 같은 밀도는 먼저 나온 후보를 유지한다.
            if (density >= ratio && (best == null || density < bestDensity)) {
                best = c;
                bestDensity = density;
            }
        }
        if (best != null) {
            return new Choice(best.url, bestDensity);
        }
        if (largest != null) {
            return new Choice(largest.url, largestDensity);
        }
        return null;
    }

    /**
     * Parses a <code>srcset</code> attribute into candidates: URLs separated by commas, each with at most one density or width descriptor.
     * A candidate with an invalid descriptor is dropped.
     * <p>
     * <code>srcset</code> 속성을 후보로 해석한다: 쉼표로 구분된 URL, 각각 밀도 또는 너비 기술자를 최대 하나 가진다. 잘못된 기술자의 후보는 버린다.
     *
     * @param text the attribute value / 속성 값
     * @return the candidates in order / 순서대로의 후보들
     */
    static List<Candidate> parseSrcset(String text) {
        List<Candidate> out = new ArrayList<Candidate>();
        int pos = 0;
        int len = text.length();
        // 후보마다: 공백과 쉼표를 건너뛰고, 공백 없는 URL 을 읽고, 쉼표(괄호 밖)까지의 기술자를 읽는다.
        while (pos < len) {
            while (pos < len && (isSpace(text.charAt(pos)) || text.charAt(pos) == ',')) {
                pos++;
            }
            if (pos >= len) {
                break;
            }
            int start = pos;
            // URL 은 공백이 나올 때까지의 글자이다.
            while (pos < len && !isSpace(text.charAt(pos))) {
                pos++;
            }
            String url = text.substring(start, pos);
            String descriptors = "";
            if (url.endsWith(",")) {
                // URL 뒤에 곧바로 쉼표가 오면 기술자가 없는 후보이다.
                int end = url.length();
                while (end > 0 && url.charAt(end - 1) == ',') {
                    end--;
                }
                url = url.substring(0, end);
            } else {
                int dstart = pos;
                int depth = 0;
                // 기술자는 괄호 밖의 쉼표가 나올 때까지 읽는다.
                while (pos < len) {
                    char c = text.charAt(pos);
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth = Math.max(0, depth - 1);
                    } else if (c == ',' && depth == 0) {
                        break;
                    }
                    pos++;
                }
                descriptors = text.substring(dstart, pos);
            }
            if (url.length() == 0) {
                continue;
            }
            Candidate c = candidate(url, descriptors);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Builds a candidate from its descriptors: nothing (1x), <code>Nx</code>, <code>Nw</code>, or <code>Nw Nh</code> (the height is ignored).
     * Returns null for an invalid combination.
     * <p>
     * 기술자로 후보를 만든다: 없음(1x), <code>Nx</code>, <code>Nw</code>, <code>Nw Nh</code>(높이는 무시). 잘못된 조합이면 null.
     *
     * @param url         the URL / URL
     * @param descriptors the descriptor text / 기술자 텍스트
     * @return the candidate or null / 후보 또는 null
     */
    private static Candidate candidate(String url, String descriptors) {
        double density = -1;
        double width = -1;
        boolean height = false;
        String[] tokens = descriptors.trim().length() == 0 ? new String[0] : descriptors.trim().split("\\s+");
        // 기술자 토큰을 하나씩 w/x/h 중 무엇인지 확인한다.
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            char unit = t.charAt(t.length() - 1);
            String number = t.substring(0, t.length() - 1);
            if (unit == 'w' && width < 0 && density < 0 && isInteger(number) && Double.parseDouble(number) > 0) {
                width = Double.parseDouble(number);
            } else if (unit == 'x' && density < 0 && width < 0 && !height && isFloat(number) && Double.parseDouble(number) >= 0) {
                density = Double.parseDouble(number);
            } else if (unit == 'h' && !height && density < 0 && isInteger(number) && Double.parseDouble(number) > 0) {
                height = true;
            } else {
                // 알 수 없거나 겹치는 기술자는 후보 전체를 버린다.
                return null;
            }
        }
        // h 기술자는 w 와 함께일 때만 유효하다.
        if (height && width < 0) {
            return null;
        }
        if (width > 0) {
            return new Candidate(url, 0, width);
        }
        if (density == 0) {
            return null;
        }
        return new Candidate(url, density < 0 ? 1 : density, 0);
    }

    /**
     * Computes the source size in px from a <code>sizes</code> attribute: the length of the first entry whose media condition matches, or
     * the last entry without a condition, or <code>100vw</code>.
     * <p>
     * <code>sizes</code> 속성에서 소스 크기(px)를 계산한다: 미디어 조건이 맞는 첫 항목의 길이, 없으면 조건 없는 마지막 항목, 그것도 없으면
     * <code>100vw</code>.
     *
     * @param sizes the attribute value or null / 속성 값 또는 null
     * @param env   the media environment / 미디어 환경
     * @return the size in px (greater than 0) / 크기(px, 0 초과)
     */
    static double sourceSize(String sizes, MediaEnvironment env) {
        double fallback = env.getWidth();
        if (sizes != null && sizes.trim().length() > 0) {
            List<String> entries = splitTopLevel(sizes);
            // 항목마다 "(조건) 길이" 형태에서 길이를 뒤에서 떼어 내고 조건이 맞으면 그 길이를 쓴다.
            for (int i = 0; i < entries.size(); i++) {
                String entry = entries.get(i).trim();
                if (entry.length() == 0) {
                    continue;
                }
                int cut = lengthStart(entry);
                String length = entry.substring(cut).trim();
                String condition = entry.substring(0, cut).trim();
                double px = evaluateLength(length, env);
                if (Double.isNaN(px) || px < 0) {
                    continue;
                }
                if (condition.length() == 0) {
                    // 조건 없는 항목은 목록의 마지막일 때 기본값이 된다.
                    if (i == entries.size() - 1) {
                        return px > 0 ? px : fallback;
                    }
                    continue;
                }
                if (MediaQueryList.parse(condition).matches(env)) {
                    return px > 0 ? px : fallback;
                }
            }
        }
        return fallback;
    }

    /**
     * Splits a list at the commas outside parentheses.
     * <p>
     * 괄호 밖의 쉼표에서 목록을 나눈다.
     *
     * @param text the list / 목록
     * @return the parts / 조각들
     */
    private static List<String> splitTopLevel(String text) {
        List<String> parts = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        // 글자를 보며 괄호 밖의 쉼표에서 항목을 나눈다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    /**
     * Finds where the length of a <code>sizes</code> entry starts: the last whitespace-separated token, or a trailing function such as
     * <code>calc(...)</code> as a whole.
     * <p>
     * <code>sizes</code> 항목에서 길이가 시작하는 위치를 찾는다: 공백으로 나뉜 마지막 토큰, 또는 끝의 <code>calc(...)</code> 같은 함수 전체.
     *
     * @param entry the entry / 항목
     * @return the index of the first char of the length / 길이의 첫 글자 위치
     */
    private static int lengthStart(String entry) {
        int i = entry.length() - 1;
        if (entry.charAt(i) == ')') {
            // 끝이 닫는 괄호이면 짝이 맞는 여는 괄호까지 거슬러 올라가 함수 이름까지 포함한다. 조건 전체가 괄호면 마지막 단어만 쓴다.
            int depth = 0;
            for (; i >= 0; i--) {
                char c = entry.charAt(i);
                if (c == ')') {
                    depth++;
                } else if (c == '(') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
            }
            int name = i;
            // 함수 이름을 이루는 글자(영문과 하이픈)를 거슬러 올라간다.
            while (name > 0 && (Character.isLetter(entry.charAt(name - 1)) || entry.charAt(name - 1) == '-')) {
                name--;
            }
            // 이름이 있는 함수(calc, min, max, clamp)만 길이이다.
            if (name < i) {
                return name;
            }
            return 0;
        }
        // 마지막 공백까지 거슬러 올라가 마지막 토큰의 시작을 찾는다.
        while (i >= 0 && !isSpace(entry.charAt(i))) {
            i--;
        }
        return i + 1;
    }

    /**
     * Evaluates a CSS length for <code>sizes</code>: a number with <code>px</code>, <code>em</code>, <code>rem</code>, <code>vw</code>,
     * <code>vh</code>, <code>vmin</code>, <code>vmax</code>, <code>cm</code>, <code>mm</code>, <code>in</code>, <code>pt</code>,
     * <code>pc</code>, <code>ch</code>, <code>ex</code>, or <code>calc()</code>/<code>min()</code>/<code>max()</code> of those.
     * <p>
     * <code>sizes</code> 용 CSS 길이를 계산한다: 숫자와 <code>px</code>, <code>em</code>, <code>rem</code>, <code>vw</code>, <code>vh</code>,
     * <code>vmin</code>, <code>vmax</code>, <code>cm</code>, <code>mm</code>, <code>in</code>, <code>pt</code>, <code>pc</code>,
     * <code>ch</code>, <code>ex</code>, 또는 이들의 <code>calc()</code>/<code>min()</code>/<code>max()</code>.
     *
     * @param text the length / 길이
     * @param env  the media environment / 미디어 환경
     * @return the value in px or NaN when invalid / px 값, 잘못되면 NaN
     */
    static double evaluateLength(String text, MediaEnvironment env) {
        LengthParser p = new LengthParser(text.trim(), env);
        double v = p.expression();
        p.skipSpace();
        if (p.pos != p.text.length()) {
            return Double.NaN;
        }
        // 단위 없는 숫자는 0 만 길이이다.
        if (text.trim().matches("[+-]?[0-9]*\\.?[0-9]+") && v != 0) {
            return Double.NaN;
        }
        return v;
    }

    /**
     * A recursive-descent evaluator for the lengths of <code>sizes</code>.
     * <p>
     * <code>sizes</code> 길이를 계산하는 재귀 하강 평가기.
     */
    private static final class LengthParser {
        /** The text. / 텍스트. */
        final String text;
        /** The environment. / 환경. */
        final MediaEnvironment env;
        /** The read position. / 읽는 위치. */
        int pos;

        /**
         * Creates a parser.
         * <p>
         * 파서를 만든다.
         *
         * @param text the text / 텍스트
         * @param env  the environment / 환경
         */
        LengthParser(String text, MediaEnvironment env) {
            this.text = text;
            this.env = env;
        }

        /**
         * Skips white space.
         * <p>
         * 공백을 건너뛴다.
         */
        void skipSpace() {
            // 공백을 모두 건너뛴다.
            while (pos < text.length() && isSpace(text.charAt(pos))) {
                pos++;
            }
        }

        /**
         * Reads a sum: terms joined by <code>+</code> and <code>-</code>.
         * <p>
         * 합을 읽는다: <code>+</code>, <code>-</code> 로 이은 항들.
         *
         * @return the value / 값
         */
        double expression() {
            double v = term();
            // 합과 차: 연산자 앞뒤에 공백이 있어야 하므로 공백을 건너뛴 뒤 연산자를 본다.
            while (!Double.isNaN(v)) {
                skipSpace();
                if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-') && pos + 1 < text.length()
                        && isSpace(text.charAt(pos + 1))) {
                    char op = text.charAt(pos++);
                    double r = term();
                    v = op == '+' ? v + r : v - r;
                } else {
                    break;
                }
            }
            return v;
        }

        /**
         * Reads a product: factors joined by <code>*</code> and <code>/</code>.
         * <p>
         * 곱을 읽는다: <code>*</code>, <code>/</code> 로 이은 인자들.
         *
         * @return the value / 값
         */
        double term() {
            double v = factor();
            // 곱과 나눗셈.
            while (!Double.isNaN(v)) {
                skipSpace();
                if (pos < text.length() && (text.charAt(pos) == '*' || text.charAt(pos) == '/')) {
                    char op = text.charAt(pos++);
                    double r = factor();
                    v = op == '*' ? v * r : (r == 0 ? Double.NaN : v / r);
                } else {
                    break;
                }
            }
            return v;
        }

        /**
         * Reads a factor: a number with a unit, a parenthesized sum, or a function.
         * <p>
         * 인자를 읽는다: 단위가 있는 숫자, 괄호로 묶은 합, 또는 함수.
         *
         * @return the value / 값
         */
        double factor() {
            skipSpace();
            if (pos >= text.length()) {
                return Double.NaN;
            }
            char c = text.charAt(pos);
            if (c == '(') {
                pos++;
                double v = expression();
                skipSpace();
                if (pos < text.length() && text.charAt(pos) == ')') {
                    pos++;
                    return v;
                }
                return Double.NaN;
            }
            if (Character.isLetter(c)) {
                return function();
            }
            return number();
        }

        /**
         * Reads <code>calc(...)</code>, <code>min(...)</code>, <code>max(...)</code> or <code>clamp(...)</code>.
         * <p>
         * <code>calc(...)</code>, <code>min(...)</code>, <code>max(...)</code>, <code>clamp(...)</code> 를 읽는다.
         *
         * @return the value / 값
         */
        double function() {
            int start = pos;
            // 함수 이름의 글자를 읽는다.
            while (pos < text.length() && (Character.isLetter(text.charAt(pos)) || text.charAt(pos) == '-')) {
                pos++;
            }
            String name = text.substring(start, pos).toLowerCase();
            if (pos >= text.length() || text.charAt(pos) != '(') {
                return Double.NaN;
            }
            pos++;
            List<Double> args = new ArrayList<Double>();
            // 쉼표로 나뉜 인수를 닫는 괄호까지 읽는다.
            while (true) {
                args.add(Double.valueOf(expression()));
                skipSpace();
                if (pos < text.length() && text.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                break;
            }
            skipSpace();
            if (pos >= text.length() || text.charAt(pos) != ')') {
                return Double.NaN;
            }
            pos++;
            // 인수 중 하나라도 잘못되면 함수 전체가 잘못이다.
            for (int i = 0; i < args.size(); i++) {
                if (Double.isNaN(args.get(i).doubleValue())) {
                    return Double.NaN;
                }
            }
            if ("calc".equals(name) && args.size() == 1) {
                return args.get(0).doubleValue();
            }
            if ("min".equals(name) && !args.isEmpty()) {
                double m = args.get(0).doubleValue();
                // min: 인수 중 가장 작은 값을 찾는다.
                for (int i = 1; i < args.size(); i++) {
                    m = Math.min(m, args.get(i).doubleValue());
                }
                return m;
            }
            if ("max".equals(name) && !args.isEmpty()) {
                double m = args.get(0).doubleValue();
                // max: 인수 중 가장 큰 값을 찾는다.
                for (int i = 1; i < args.size(); i++) {
                    m = Math.max(m, args.get(i).doubleValue());
                }
                return m;
            }
            if ("clamp".equals(name) && args.size() == 3) {
                return Math.max(args.get(0).doubleValue(), Math.min(args.get(1).doubleValue(), args.get(2).doubleValue()));
            }
            return Double.NaN;
        }

        /**
         * Reads a number with its unit and converts it to px; a bare 0 or a bare number inside a product counts as unitless.
         * <p>
         * 단위가 있는 숫자를 읽어 px 로 바꾼다. 맨 0 이나 곱 안의 단위 없는 숫자는 단위 없음으로 본다.
         *
         * @return the value in px / px 값
         */
        double number() {
            int start = pos;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            // 숫자(정수부와 소수점, 소수부)를 읽는다.
            while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
                pos++;
            }
            double v;
            try {
                v = Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
            int us = pos;
            // 단위의 글자를 읽는다.
            while (pos < text.length() && Character.isLetter(text.charAt(pos))) {
                pos++;
            }
            String unit = text.substring(us, pos).toLowerCase();
            if (unit.length() == 0) {
                // 단위 없는 숫자: 0 이거나 곱/나눗셈의 계수이므로 그대로 둔다.
                return v;
            }
            double vw = env.getWidth() / 100;
            double vh = env.getHeight() / 100;
            if ("px".equals(unit)) {
                return v;
            } else if ("em".equals(unit) || "rem".equals(unit)) {
                return v * BASE_FONT_PX;
            } else if ("ch".equals(unit) || "ex".equals(unit)) {
                return v * BASE_FONT_PX / 2;
            } else if ("vw".equals(unit)) {
                return v * vw;
            } else if ("vh".equals(unit)) {
                return v * vh;
            } else if ("vmin".equals(unit)) {
                return v * Math.min(vw, vh);
            } else if ("vmax".equals(unit)) {
                return v * Math.max(vw, vh);
            } else if ("cm".equals(unit)) {
                return v * 96 / 2.54;
            } else if ("mm".equals(unit)) {
                return v * 96 / 25.4;
            } else if ("in".equals(unit)) {
                return v * 96;
            } else if ("pt".equals(unit)) {
                return v * 96 / 72;
            } else if ("pc".equals(unit)) {
                return v * 16;
            }
            return Double.NaN;
        }
    }

    /**
     * Returns the lower-case local name of an element or the empty string.
     * <p>
     * 요소의 소문자 로컬 이름을 반환한다. 없으면 빈 문자열.
     *
     * @param e the element / 요소
     * @return the name / 이름
     */
    private static String localName(Element e) {
        return e.getLocalName() == null ? "" : e.getLocalName().toLowerCase();
    }

    /**
     * Tells whether a char is HTML white space.
     * <p>
     * 글자가 HTML 공백인지 알려 준다.
     *
     * @param c the char / 글자
     * @return true for space, tab, line feed, form feed, carriage return / 공백, 탭, 줄 바꿈, 폼 피드, 캐리지 리턴이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Tells whether text is a valid non-negative integer (digits only).
     * <p>
     * 텍스트가 올바른 음이 아닌 정수(숫자만)인지 알려 준다.
     *
     * @param s the text / 텍스트
     * @return true if valid / 올바르면 true
     */
    private static boolean isInteger(String s) {
        if (s.length() == 0 || s.length() > 9) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            // 숫자가 아닌 글자가 있으면 정수가 아니다.
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether text is a valid non-negative floating point number such as <code>1.5</code>, <code>.5</code> or <code>2e0</code>.
     * <p>
     * 텍스트가 <code>1.5</code>, <code>.5</code>, <code>2e0</code> 같은 올바른 음이 아닌 부동소수점 수인지 알려 준다.
     *
     * @param s the text / 텍스트
     * @return true if valid / 올바르면 true
     */
    private static boolean isFloat(String s) {
        if (s.length() == 0) {
            return false;
        }
        boolean digit = false;
        boolean dot = false;
        boolean exp = false;
        // 글자를 모두 보며 숫자가 아닌 것이 있으면 정수가 아니다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digit = true;
            } else if (c == '.' && !dot && !exp) {
                dot = true;
            } else if ((c == 'e' || c == 'E') && digit && !exp && i + 1 < s.length()) {
                exp = true;
                // 지수 부호는 e 바로 뒤에서만 허용한다.
                if (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-') {
                    i++;
                }
            } else {
                return false;
            }
        }
        return digit;
    }
}
