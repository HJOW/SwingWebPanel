package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Text drawing for SVG (plan item P18-04) with Java2D fonts: <code>text</code> with nested <code>tspan</code>, <code>a</code> and
 * <code>textPath</code>, the position lists <code>x</code>, <code>y</code>, <code>dx</code>, <code>dy</code>, <code>rotate</code>,
 * <code>text-anchor</code>, <code>letter-spacing</code>, <code>word-spacing</code>, <code>text-decoration</code> (underline, overline,
 * line-through), <code>baseline-shift</code>, <code>dominant-baseline</code>, <code>font-family</code> (generic families and installed
 * families, with fallback to the next name of the list), <code>font-size</code>, <code>font-weight</code>, <code>font-style</code>, white space
 * handling (<code>xml:space</code>, <code>white-space</code>) and path text with <code>startOffset</code>. The glyph outlines are filled and
 * stroked like any other shape, so gradients, patterns and clip paths work on text. Not supported: vertical writing modes,
 * <code>textLength</code>, SVG fonts and web fonts (external fonts are never loaded).
 * <p>
 * Java2D 글꼴을 쓰는 SVG 문자 그리기(계획 P18-04): 중첩된 <code>tspan</code>, <code>a</code>, <code>textPath</code> 를 가진 <code>text</code>, 위치 목록
 * <code>x</code>, <code>y</code>, <code>dx</code>, <code>dy</code>, <code>rotate</code>, <code>text-anchor</code>, <code>letter-spacing</code>,
 * <code>word-spacing</code>, <code>text-decoration</code>(밑줄, 윗줄, 취소선), <code>baseline-shift</code>, <code>dominant-baseline</code>,
 * <code>font-family</code>(일반 계열과 설치된 계열, 목록의 다음 이름으로 대체), <code>font-size</code>, <code>font-weight</code>,
 * <code>font-style</code>, 공백 처리(<code>xml:space</code>, <code>white-space</code>), <code>startOffset</code> 이 있는 경로 문자. 글리프 윤곽은 다른
 * 도형처럼 채우고 선을 그리므로 문자에도 그라디언트, 패턴, 클립 경로가 동작한다. 지원하지 않는 것: 세로 쓰기, <code>textLength</code>, SVG 글꼴,
 * 웹 글꼴(외부 글꼴은 읽지 않는다).
 */
final class SvgText {

    /**
     * The font rendering context used for measuring and outlines: no transform, anti-aliased, fractional metrics.
     * <p>
     * 측정과 윤곽에 쓰는 글꼴 렌더링 문맥: 변환 없음, 안티앨리어싱, 소수 메트릭.
     */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * The lower-case names of the installed font families (loaded once, on first use).
     * <p>
     * 설치된 글꼴 계열의 소문자 이름(처음 쓸 때 한 번 읽는다).
     */
    private static volatile Map<String, String> installed;

    /**
     * One addressable character with its style and position adjustments.
     * <p>
     * 스타일과 위치 조정을 가진 주소 지정 가능한 문자 하나.
     */
    private static final class Ch {

        /**
         * The character (one or two UTF-16 units).
         * <p>
         * 문자(UTF-16 단위 하나 또는 둘).
         */
        String str;

        /**
         * The style of the element that contains the character.
         * <p>
         * 문자를 포함한 요소의 스타일.
         */
        SvgStyle style;

        /**
         * Absolute x or NaN.
         * <p>
         * 절대 x. 없으면 NaN.
         */
        double x = Double.NaN;

        /**
         * Absolute y or NaN.
         * <p>
         * 절대 y. 없으면 NaN.
         */
        double y = Double.NaN;

        /**
         * Relative x shift.
         * <p>
         * x 상대 이동.
         */
        double dx;

        /**
         * Relative y shift.
         * <p>
         * y 상대 이동.
         */
        double dy;

        /**
         * Rotation in degrees or NaN.
         * <p>
         * 회전(도). 없으면 NaN.
         */
        double rot = Double.NaN;

        /**
         * The path the character is placed on, or null.
         * <p>
         * 문자가 놓이는 경로. 없으면 null.
         */
        PathPlacement path;

        /**
         * The decoration flags: 1 underline, 2 overline, 4 line-through.
         * <p>
         * 장식 플래그: 1 밑줄, 2 윗줄, 4 취소선.
         */
        int deco;

        /**
         * The style of the element that declared the decoration.
         * <p>
         * 장식을 선언한 요소의 스타일.
         */
        SvgStyle decoStyle;
    }

    /**
     * A path that text follows, with its flattened length table.
     * <p>
     * 문자가 따라가는 경로와 평탄화한 길이 표.
     */
    private static final class PathPlacement {

        /**
         * The points of the flattened path (x, y pairs).
         * <p>
         * 평탄화한 경로의 점(x, y 쌍).
         */
        double[] pts;

        /**
         * The cumulative length at each point.
         * <p>
         * 각 점에서의 누적 길이.
         */
        double[] len;

        /**
         * The start offset along the path.
         * <p>
         * 경로를 따라간 시작 오프셋.
         */
        double offset;

        /**
         * The total length.
         * <p>
         * 전체 길이.
         */
        double total;
    }

    /**
     * One run of text that is laid out and drawn as a unit.
     * <p>
     * 한 덩어리로 배치되고 그려지는 문자 구간 하나.
     */
    private static final class Run {

        /**
         * The characters of the run.
         * <p>
         * 구간의 문자.
         */
        String text;

        /**
         * The style of the run.
         * <p>
         * 구간의 스타일.
         */
        SvgStyle style;

        /**
         * The font of the run.
         * <p>
         * 구간의 글꼴.
         */
        Font font;

        /**
         * The left end of the baseline.
         * <p>
         * 기준선의 왼쪽 끝.
         */
        double x;

        /**
         * The y position of the baseline.
         * <p>
         * 기준선의 y 위치.
         */
        double y;

        /**
         * The advance width including letter spacing.
         * <p>
         * 글자 간격을 포함한 진행 너비.
         */
        double advance;

        /**
         * The index of the first character in the character list.
         * <p>
         * 문자 목록에서 첫 문자의 위치.
         */
        int first;

        /**
         * The rotation in degrees (0 if none).
         * <p>
         * 회전(도). 없으면 0.
         */
        double rot;

        /**
         * True if this run starts a new text chunk.
         * <p>
         * 이 구간이 새 문자 덩어리를 시작하면 true.
         */
        boolean chunkStart;

        /**
         * The placement on a path or null.
         * <p>
         * 경로 위의 배치. 없으면 null.
         */
        PathPlacement path;

        /**
         * The final glyph outline in user space.
         * <p>
         * 사용자 공간에서의 최종 글리프 윤곽.
         */
        Shape outline;

        /**
         * The decoration rectangles in user space.
         * <p>
         * 사용자 공간의 장식 사각형.
         */
        List<Shape> decorations = new ArrayList<Shape>();

        /**
         * The style of the element that declared each decoration (parallel to {@link #decorations}).
         * <p>
         * 각 장식을 선언한 요소의 스타일({@link #decorations} 와 나란히 있다).
         */
        List<SvgStyle> decoStyles = new ArrayList<SvgStyle>();

        /**
         * The box of the run for bounding boxes.
         * <p>
         * 경계 상자에 쓰는 구간의 상자.
         */
        Rectangle2D cell;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgText() {
    }

    /**
     * Draws a <code>text</code> element.
     * <p>
     * <code>text</code> 요소를 그린다.
     *
     * @param r  the renderer / 렌더러
     * @param e  the text element / text 요소
     * @param st its style / 그 스타일
     */
    static void render(SvgRenderer r, SvgElement e, SvgStyle st) {
        List<Run> runs = layout(r, e, st);
        if (runs.isEmpty()) {
            return;
        }
        Rectangle2D bb = unionBounds(runs);
        // 구간마다 채우기와 선과 장식을 칠한다.
        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            if (run.outline == null || !run.style.visible()) {
                continue;
            }
            String po = run.style.keyword("paint-order");
            boolean strokeFirst = po != null && po.startsWith("stroke");
            if (strokeFirst) {
                strokeRun(r, run, bb);
            }
            SvgPaint fill = run.style.paint("fill");
            if (fill.kind != SvgPaint.NONE) {
                r.paintShape(run.outline, fill, run.style.opacity("fill-opacity", 1), run.style, bb);
            }
            if (!strokeFirst) {
                strokeRun(r, run, bb);
            }
            // 장식 선은 장식을 선언한 요소의 채우기로 칠한다.
            for (int k = 0; k < run.decorations.size(); k++) {
                SvgStyle ds = run.decoStyles.get(k);
                SvgPaint dp = ds.paint("fill");
                r.paintShape(run.decorations.get(k), dp, ds.opacity("fill-opacity", 1), ds, bb);
            }
        }
    }

    /**
     * Strokes the outline of a run.
     * <p>
     * 구간의 윤곽에 선을 그린다.
     *
     * @param r   the renderer / 렌더러
     * @param run the run / 구간
     * @param bb  the box of the whole text / 문자 전체의 상자
     */
    private static void strokeRun(SvgRenderer r, Run run, Rectangle2D bb) {
        SvgPaint stroke = run.style.paint("stroke");
        if (stroke.kind == SvgPaint.NONE) {
            return;
        }
        BasicStroke bs = r.strokeOf(run.style);
        if (bs == null) {
            return;
        }
        r.paintShape(bs.createStrokedShape(run.outline), stroke, run.style.opacity("stroke-opacity", 1), run.style, bb);
    }

    /**
     * Returns the union of the boxes of the runs.
     * <p>
     * 구간 상자들의 합집합을 반환한다.
     *
     * @param runs the runs / 구간
     * @return the union or null / 합집합. 없으면 null
     */
    private static Rectangle2D unionBounds(List<Run> runs) {
        SvgBounds.Acc a = new SvgBounds.Acc();
        // 모든 구간의 상자를 합친다.
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).cell != null) {
                a.addRect(runs.get(i).cell, null);
            }
        }
        return a.result();
    }

    /**
     * Returns the box of a text element for object bounding box units.
     * <p>
     * 객체 경계 상자 단위를 위한 text 요소의 상자를 반환한다.
     *
     * @param r  the renderer / 렌더러
     * @param e  the text element / text 요소
     * @param st its style / 스타일
     * @return the box or null / 상자. 없으면 null
     */
    static Rectangle2D bounds(SvgRenderer r, SvgElement e, SvgStyle st) {
        List<Run> runs = layout(r, e, st);
        return runs.isEmpty() ? null : unionBounds(runs);
    }

    /**
     * Returns the union of the glyph outlines of a text element, used when the text is a child of a clip path.
     * <p>
     * 문자가 클립 경로의 자식일 때 쓰도록 text 요소의 글리프 윤곽 합집합을 반환한다.
     *
     * @param r  the renderer / 렌더러
     * @param e  the text element / text 요소
     * @param st its style / 스타일
     * @return the outline or null / 윤곽. 없으면 null
     */
    static Shape clipShape(SvgRenderer r, SvgElement e, SvgStyle st) {
        List<Run> runs = layout(r, e, st);
        if (runs.isEmpty()) {
            return null;
        }
        GeneralPath all = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        // 모든 구간의 윤곽과 장식을 하나의 경로로 합친다.
        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            if (run.outline != null) {
                all.append(run.outline, false);
            }
            // 장식 사각형을 하나씩 윤곽에 더한다.
            for (int k = 0; k < run.decorations.size(); k++) {
                all.append(run.decorations.get(k), false);
            }
        }
        return all;
    }

    /**
     * The state used while the characters of a text element are collected.
     * <p>
     * 문자 요소의 문자를 모으는 동안 쓰는 상태.
     */
    private static final class Collector {

        /**
         * The collected characters.
         * <p>
         * 모은 문자.
         */
        final List<Ch> chars = new ArrayList<Ch>();

        /**
         * True if the previous character was a (collapsed) space.
         * <p>
         * 직전 문자가 (축약된) 공백이었으면 true.
         */
        boolean lastSpace = true;

        /**
         * The path of the enclosing <code>textPath</code> or null.
         * <p>
         * 둘러싼 <code>textPath</code> 의 경로. 없으면 null.
         */
        PathPlacement path;

        /**
         * The inherited decoration flags.
         * <p>
         * 상속된 장식 플래그.
         */
        int deco;

        /**
         * The style that declared the inherited decoration.
         * <p>
         * 상속된 장식을 선언한 스타일.
         */
        SvgStyle decoStyle;

        /**
         * The index of the first character of the last collapsed space (for trimming the end).
         * <p>
         * 마지막 축약된 공백의 위치(끝을 다듬을 때 쓴다).
         */
        int trimFrom = -1;
    }

    /**
     * Collects the characters of a text content element and its descendants.
     * <p>
     * 문자 내용 요소와 그 자손의 문자를 모은다.
     *
     * @param r        the renderer / 렌더러
     * @param el       the element / 요소
     * @param st       its style / 스타일
     * @param c        the collector / 수집기
     * @param preserve true if white space is preserved here / 이곳에서 공백을 보존하면 true
     */
    private static void collect(SvgRenderer r, SvgElement el, SvgStyle st, Collector c, boolean preserve) {
        r.tick();
        int start = c.chars.size();
        String sp = el.attrs.get("xml:space");
        String ws = st.keyword("white-space");
        boolean keep = preserve;
        if ("preserve".equals(sp)) {
            keep = true;
        } else if ("default".equals(sp)) {
            keep = false;
        }
        if (ws != null && (ws.startsWith("pre"))) {
            keep = true;
        }
        int savedDeco = c.deco;
        SvgStyle savedDecoStyle = c.decoStyle;
        String td = st.own("text-decoration");
        if (td == null) {
            td = st.own("text-decoration-line");
        }
        if (td != null) {
            int flags = decoFlags(td);
            if (flags != 0) {
                c.deco |= flags;
                c.decoStyle = st;
            }
        }
        // 혼합 내용을 순서대로 처리한다: 문자열은 문자로, 자식 요소는 재귀로 모은다.
        for (int i = 0; i < el.content.size(); i++) {
            Object o = el.content.get(i);
            if (o instanceof String) {
                addText(c, (String) o, st, keep);
            } else {
                SvgElement child = (SvgElement) o;
                if (!child.isSvg() || !(child.name.equals("tspan") || child.name.equals("a") || child.name.equals("textPath")
                        || child.name.equals("altGlyph"))) {
                    continue;
                }
                SvgStyle cs = new SvgStyle(st, child, r.vpW, r.vpH);
                if (!cs.displayed() || !r.conditionsPass(child)) {
                    continue;
                }
                PathPlacement savedPath = c.path;
                if (child.name.equals("textPath")) {
                    PathPlacement pp = placement(r, child, cs);
                    if (pp == null) {
                        continue;
                    }
                    c.path = pp;
                }
                collect(r, child, cs, c, keep);
                c.path = savedPath;
            }
        }
        c.deco = savedDeco;
        c.decoStyle = savedDecoStyle;
        assignPositions(r, el, st, c.chars, start);
    }

    /**
     * Converts a <code>text-decoration</code> value to flags.
     * <p>
     * <code>text-decoration</code> 값을 플래그로 바꾼다.
     *
     * @param td the property value / 속성 값
     * @return 1 underline, 2 overline, 4 line-through, combined / 1 밑줄, 2 윗줄, 4 취소선을 합친 값
     */
    private static int decoFlags(String td) {
        String t = td.toLowerCase(Locale.ENGLISH);
        int f = 0;
        if (t.indexOf("underline") >= 0) {
            f |= 1;
        }
        if (t.indexOf("overline") >= 0) {
            f |= 2;
        }
        if (t.indexOf("line-through") >= 0) {
            f |= 4;
        }
        return f;
    }

    /**
     * Adds the characters of a text node, applying the white space rules.
     * <p>
     * 텍스트 노드의 문자를 공백 규칙을 적용해 추가한다.
     *
     * @param c        the collector / 수집기
     * @param s        the text / 텍스트
     * @param st       the style of the containing element / 포함한 요소의 스타일
     * @param preserve true to keep white space / 공백을 보존하려면 true
     */
    private static void addText(Collector c, String s, SvgStyle st, boolean preserve) {
        if (!(st.fontSize > 0)) {
            // 글꼴 크기가 0 이하이면 문자가 보이지 않는다.
            return;
        }
        int i = 0;
        // 코드 포인트마다 공백 규칙을 적용해 문자 목록에 더한다.
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            boolean space = cp == ' ' || cp == '\t' || cp == '\n' || cp == '\r';
            if (space && !preserve) {
                if (c.lastSpace) {
                    continue;
                }
                cp = ' ';
                c.lastSpace = true;
                c.trimFrom = c.chars.size();
            } else {
                if (space) {
                    cp = ' ';
                }
                c.lastSpace = space;
                c.trimFrom = -1;
            }
            Ch ch = new Ch();
            ch.str = new String(Character.toChars(cp));
            ch.style = st;
            ch.path = c.path;
            ch.deco = c.deco;
            ch.decoStyle = c.decoStyle;
            c.chars.add(ch);
        }
    }

    /**
     * Applies the position lists of an element (<code>x</code>, <code>y</code>, <code>dx</code>, <code>dy</code>, <code>rotate</code>) to
     * the characters that the element contains.
     * <p>
     * 요소의 위치 목록(<code>x</code>, <code>y</code>, <code>dx</code>, <code>dy</code>, <code>rotate</code>)을 요소가 포함한 문자에 적용한다.
     *
     * @param r     the renderer / 렌더러
     * @param el    the element / 요소
     * @param st    its style / 스타일
     * @param chars all collected characters / 지금까지 모은 모든 문자
     * @param start the index of the first character of the element / 요소의 첫 문자 위치
     */
    private static void assignPositions(SvgRenderer r, SvgElement el, SvgStyle st, List<Ch> chars, int start) {
        SvgLength[] xs = SvgLength.parseList(el.attrs.get("x"));
        SvgLength[] ys = SvgLength.parseList(el.attrs.get("y"));
        SvgLength[] dxs = SvgLength.parseList(el.attrs.get("dx"));
        SvgLength[] dys = SvgLength.parseList(el.attrs.get("dy"));
        double[] rot = SvgScanner.numberList(el.attrs.get("rotate"));
        // 목록의 k 번째 값을 요소의 k 번째 문자에 지정한다(문자가 모자라면 남는 값은 버린다).
        for (int k = 0; k < chars.size() - start; k++) {
            Ch ch = chars.get(start + k);
            if (xs != null && k < xs.length) {
                ch.x = st.px(xs[k], 0);
            }
            if (ys != null && k < ys.length) {
                ch.y = st.px(ys[k], 1);
            }
            if (dxs != null && k < dxs.length) {
                ch.dx += st.px(dxs[k], 0);
            }
            if (dys != null && k < dys.length) {
                ch.dy += st.px(dys[k], 1);
            }
            if (rot != null && rot.length > 0) {
                ch.rot = k < rot.length ? rot[k] : rot[rot.length - 1];
            }
        }
    }

    /**
     * Builds the path placement of a <code>textPath</code> element.
     * <p>
     * <code>textPath</code> 요소의 경로 배치를 만든다.
     *
     * @param r  the renderer / 렌더러
     * @param tp the textPath element / textPath 요소
     * @param st its style / 스타일
     * @return the placement or null if the reference is not a usable path / 배치. 참조가 쓸 수 있는 경로가 아니면 null
     */
    private static PathPlacement placement(SvgRenderer r, SvgElement tp, SvgStyle st) {
        SvgElement pe = r.lookup(tp.href());
        if (pe == null) {
            return null;
        }
        Shape shape;
        SvgStyle ps = r.styleFor(pe);
        if (pe.is("path")) {
            shape = r.pathOf(pe, ps).shape;
        } else {
            shape = r.basicShape(pe, ps);
        }
        if (shape == null) {
            return null;
        }
        AffineTransform t = r.transformOf(pe, ps);
        if (t != null && !t.isIdentity()) {
            shape = t.createTransformedShape(shape);
        }
        List<Double> pts = new ArrayList<Double>();
        PathIterator it = shape.getPathIterator(null, 0.1);
        double[] c = new double[6];
        double sx = 0;
        double sy = 0;
        // 평탄화한 경로의 점을 모은다(닫는 구간은 시작점으로 되돌아가는 선분이 된다).
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_CLOSE) {
                pts.add(Double.valueOf(sx));
                pts.add(Double.valueOf(sy));
            } else {
                if (type == PathIterator.SEG_MOVETO) {
                    sx = c[0];
                    sy = c[1];
                }
                pts.add(Double.valueOf(c[0]));
                pts.add(Double.valueOf(c[1]));
            }
            it.next();
        }
        if (pts.size() < 4) {
            return null;
        }
        PathPlacement pp = new PathPlacement();
        pp.pts = new double[pts.size()];
        pp.len = new double[pts.size() / 2];
        // 점 사이의 거리를 누적해 길이 표를 만든다.
        for (int i = 0; i < pp.pts.length; i++) {
            pp.pts[i] = pts.get(i).doubleValue();
        }
        // 점 사이의 거리를 누적해 길이 표를 채운다.
        for (int i = 1; i < pp.len.length; i++) {
            double dx = pp.pts[i * 2] - pp.pts[i * 2 - 2];
            double dy = pp.pts[i * 2 + 1] - pp.pts[i * 2 - 1];
            pp.len[i] = pp.len[i - 1] + Math.sqrt(dx * dx + dy * dy);
        }
        pp.total = pp.len[pp.len.length - 1];
        SvgLength so = SvgLength.parse(tp.attrs.get("startOffset"));
        if (so != null) {
            pp.offset = so.isPercent() ? so.value * pp.total / 100 : st.px(so, 0);
        }
        return pp;
    }

    /**
     * Looks up the position and direction on a placement path at a distance.
     * <p>
     * 경로 배치에서 거리에 해당하는 위치와 방향을 구한다.
     *
     * @param pp   the placement / 배치
     * @param dist the distance from the start / 시작에서의 거리
     * @return {x, y, angle in radians}, or null if outside the path / {x, y, 각도(라디안)}. 경로 밖이면 null
     */
    private static double[] pointAt(PathPlacement pp, double dist) {
        if (dist < 0 || dist > pp.total) {
            return null;
        }
        int n = pp.len.length;
        // 거리가 속한 선분을 찾는다.
        int i = 1;
        while (i < n - 1 && pp.len[i] < dist) {
            i++;
        }
        double seg = pp.len[i] - pp.len[i - 1];
        double t = seg == 0 ? 0 : (dist - pp.len[i - 1]) / seg;
        double x0 = pp.pts[i * 2 - 2];
        double y0 = pp.pts[i * 2 - 1];
        double x1 = pp.pts[i * 2];
        double y1 = pp.pts[i * 2 + 1];
        return new double[] { x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, Math.atan2(y1 - y0, x1 - x0) };
    }

    /**
     * Lays out a <code>text</code> element into runs with final outlines.
     * <p>
     * <code>text</code> 요소를 최종 윤곽을 가진 구간들로 배치한다.
     *
     * @param r  the renderer / 렌더러
     * @param e  the text element / text 요소
     * @param st its style / 스타일
     * @return the runs / 구간
     */
    private static List<Run> layout(SvgRenderer r, SvgElement e, SvgStyle st) {
        Collector c = new Collector();
        collect(r, e, st, c, false);
        // 축약된 끝 공백을 제거한다.
        if (c.trimFrom >= 0 && c.trimFrom == c.chars.size() - 1 && c.lastSpace) {
            c.chars.remove(c.chars.size() - 1);
        }
        List<Run> runs = new ArrayList<Run>();
        List<Ch> chars = c.chars;
        int n = chars.size();
        int i = 0;
        // 같은 스타일이고 위치 조정이 없는 연속 문자를 한 구간으로 묶는다.
        while (i < n) {
            Ch first = chars.get(i);
            int j = i + 1;
            boolean spaced = hasSpacing(first.style);
            if (!spaced && first.path == null && Double.isNaN(first.rot)) {
                // 합칠 수 있는 문자가 이어지는 동안 구간을 늘린다.
                while (j < n && mergeable(first, chars.get(j))) {
                    j++;
                }
            }
            Run run = new Run();
            StringBuilder sb = new StringBuilder();
            // 구간의 문자를 이어 붙인다.
            for (int k = i; k < j; k++) {
                sb.append(chars.get(k).str);
            }
            run.text = sb.toString();
            run.style = first.style;
            run.first = i;
            run.path = first.path;
            run.rot = Double.isNaN(first.rot) ? 0 : first.rot;
            run.font = fontFor(r, first.style);
            run.advance = advanceOf(run, spaced, first.style);
            runs.add(run);
            i = j;
        }
        place(r, runs, chars);
        // 배치된 구간마다 윤곽, 장식, 상자를 만든다.
        for (int k = 0; k < runs.size(); k++) {
            build(runs.get(k), chars);
        }
        return runs;
    }

    /**
     * Tells whether the letter or word spacing of a style is set.
     * <p>
     * 스타일에 글자/단어 간격이 설정되었는지 알려 준다.
     *
     * @param st the style / 스타일
     * @return true if set / 설정되었으면 true
     */
    private static boolean hasSpacing(SvgStyle st) {
        return spacing(st, "letter-spacing") != 0 || spacing(st, "word-spacing") != 0;
    }

    /**
     * Returns the letter or word spacing in pixels.
     * <p>
     * 글자/단어 간격을 픽셀로 반환한다.
     *
     * @param st   the style / 스타일
     * @param prop the property name / 속성 이름
     * @return the spacing / 간격
     */
    private static double spacing(SvgStyle st, String prop) {
        String v = st.get(prop);
        if (v == null || "normal".equalsIgnoreCase(v)) {
            return 0;
        }
        SvgLength l = SvgLength.parse(v);
        return l == null ? 0 : st.px(l, 0);
    }

    /**
     * Tells whether a character can join the run that starts with another one.
     * <p>
     * 문자가 다른 문자로 시작하는 구간에 합쳐질 수 있는지 알려 준다.
     *
     * @param a the first character of the run / 구간의 첫 문자
     * @param b the candidate / 후보 문자
     * @return true if it can be merged / 합칠 수 있으면 true
     */
    private static boolean mergeable(Ch a, Ch b) {
        return a.style == b.style && Double.isNaN(b.x) && Double.isNaN(b.y) && b.dx == 0 && b.dy == 0 && Double.isNaN(b.rot) && b.path == null
                && a.deco == b.deco && a.decoStyle == b.decoStyle;
    }

    /**
     * Measures the advance of a run, adding the letter and word spacing.
     * <p>
     * 구간의 진행 너비를 측정하며 글자/단어 간격을 더한다.
     *
     * @param run    the run / 구간
     * @param spaced true if spacing applies / 간격이 적용되면 true
     * @param st     the style / 스타일
     * @return the advance / 진행 너비
     */
    private static double advanceOf(Run run, boolean spaced, SvgStyle st) {
        if (run.text.length() == 0) {
            return 0;
        }
        double adv = new TextLayout(run.text, run.font, FRC).getAdvance();
        if (spaced) {
            double ls = spacing(st, "letter-spacing");
            double wsp = spacing(st, "word-spacing");
            int chars = run.text.codePointCount(0, run.text.length());
            adv += ls * chars;
            if (" ".equals(run.text)) {
                adv += wsp;
            }
        }
        return adv;
    }

    /**
     * Positions the runs: absolute positions, relative shifts, chunk anchoring and positions on paths.
     * <p>
     * 구간의 위치를 정한다: 절대 위치, 상대 이동, 덩어리 정렬, 경로 위의 위치.
     *
     * @param r     the renderer / 렌더러
     * @param runs  the runs / 구간
     * @param chars the characters / 문자
     */
    private static void place(SvgRenderer r, List<Run> runs, List<Ch> chars) {
        double cx = 0;
        double cy = 0;
        int chunkFirst = 0;
        // 구간마다 위치를 정하고 덩어리의 시작을 표시한다.
        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            Ch ch = chars.get(run.first);
            boolean newChunk = i == 0 || !Double.isNaN(ch.x) || !Double.isNaN(ch.y);
            if (!Double.isNaN(ch.x)) {
                cx = ch.x;
            }
            if (!Double.isNaN(ch.y)) {
                cy = ch.y;
            }
            cx += ch.dx;
            cy += ch.dy;
            run.chunkStart = newChunk;
            run.x = cx;
            run.y = cy + baselineShift(run);
            cx += run.advance;
            if (newChunk) {
                chunkFirst = i;
            }
            // 구간이 덩어리의 끝이면(다음 구간이 새 덩어리이거나 마지막) 정렬을 적용한다.
            boolean last = i == runs.size() - 1 || startsChunk(runs.get(i + 1), chars);
            if (last) {
                anchor(runs, chunkFirst, i);
            }
        }
    }

    /**
     * Tells whether a run starts a new text chunk (its first character has an absolute position).
     * <p>
     * 구간이 새 문자 덩어리를 시작하는지(첫 문자에 절대 위치가 있는지) 알려 준다.
     *
     * @param run   the run / 구간
     * @param chars the characters / 문자
     * @return true if a new chunk starts / 새 덩어리가 시작하면 true
     */
    private static boolean startsChunk(Run run, List<Ch> chars) {
        Ch ch = chars.get(run.first);
        return !Double.isNaN(ch.x) || !Double.isNaN(ch.y);
    }

    /**
     * Applies <code>text-anchor</code> to the runs of one chunk.
     * <p>
     * 한 덩어리의 구간에 <code>text-anchor</code> 를 적용한다.
     *
     * @param runs  the runs / 구간
     * @param from  the first run of the chunk / 덩어리의 첫 구간
     * @param to    the last run of the chunk / 덩어리의 마지막 구간
     */
    private static void anchor(List<Run> runs, int from, int to) {
        Run first = runs.get(from);
        String a = first.style.keyword("text-anchor");
        Run last = runs.get(to);
        double width;
        if (first.path != null) {
            width = 0;
            // 경로 위 문자는 모든 구간의 진행 너비를 더한다.
            for (int i = from; i <= to; i++) {
                width += runs.get(i).advance;
            }
        } else {
            width = last.x + last.advance - first.x;
        }
        double shift = 0;
        if ("middle".equals(a)) {
            shift = -width / 2;
        } else if ("end".equals(a)) {
            shift = -width;
        }
        // 덩어리의 모든 구간을 같은 만큼 옮긴다(경로 위 문자는 경로 거리에 반영된다).
        double dist = 0;
        for (int i = from; i <= to; i++) {
            Run run = runs.get(i);
            if (run.path != null) {
                run.x = run.path.offset + shift + dist;
                dist += run.advance;
            } else {
                run.x += shift;
            }
        }
    }

    /**
     * Computes the vertical shift of the baseline for <code>dominant-baseline</code>, <code>alignment-baseline</code> and
     * <code>baseline-shift</code>.
     * <p>
     * <code>dominant-baseline</code>, <code>alignment-baseline</code>, <code>baseline-shift</code> 에 따른 기준선의 세로 이동을 계산한다.
     *
     * @param run the run / 구간
     * @return the shift in pixels, positive downwards / 이동량(픽셀). 아래쪽이 양수
     */
    private static double baselineShift(Run run) {
        SvgStyle st = run.style;
        LineMetrics lm = run.font.getLineMetrics("x", FRC);
        double ascent = lm.getAscent();
        double descent = lm.getDescent();
        double shift = 0;
        String db = st.own("alignment-baseline");
        if (db == null || "auto".equalsIgnoreCase(db) || "baseline".equalsIgnoreCase(db)) {
            db = st.keyword("dominant-baseline");
        }
        if (db != null) {
            String d = db.toLowerCase(Locale.ENGLISH);
            if ("middle".equals(d)) {
                shift = run.font.createGlyphVector(FRC, "x").getVisualBounds().getHeight() / 2;
            } else if ("central".equals(d)) {
                shift = (ascent - descent) / 2;
            } else if ("hanging".equals(d)) {
                shift = ascent * 0.8;
            } else if ("text-before-edge".equals(d) || "text-top".equals(d)) {
                shift = ascent;
            } else if ("text-after-edge".equals(d) || "text-bottom".equals(d) || "ideographic".equals(d)) {
                shift = -descent;
            } else if ("mathematical".equals(d)) {
                shift = ascent * 0.5;
            }
        }
        String bs = st.own("baseline-shift");
        if (bs != null) {
            String b = bs.trim().toLowerCase(Locale.ENGLISH);
            if ("sub".equals(b)) {
                shift += st.fontSize / 5;
            } else if ("super".equals(b)) {
                shift -= st.fontSize / 3;
            } else {
                SvgLength l = SvgLength.parse(b);
                if (l != null) {
                    shift -= l.isPercent() ? l.value * st.fontSize / 100 : st.px(l, 1);
                }
            }
        }
        return shift;
    }

    /**
     * Builds the outline, decorations and box of a positioned run.
     * <p>
     * 배치된 구간의 윤곽, 장식, 상자를 만든다.
     *
     * @param run   the run / 구간
     * @param chars the characters / 문자
     */
    private static void build(Run run, List<Ch> chars) {
        Ch first = chars.get(run.first);
        SvgStyle st = run.style;
        LineMetrics lm = run.font.getLineMetrics(run.text.length() == 0 ? " " : run.text, FRC);
        double ls = spacing(st, "letter-spacing");
        double wsp = spacing(st, "word-spacing");
        Shape glyphs;
        if (run.text.length() == 0) {
            return;
        }
        if (ls != 0 || wsp != 0) {
            // 간격이 있으면 문자마다 따로 배치해 합친다.
            GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            double x = 0;
            int idx = 0;
            // 문자마다 따로 배치해 간격을 반영한다.
            while (idx < run.text.length()) {
                int cp = run.text.codePointAt(idx);
                String one = new String(Character.toChars(cp));
                TextLayout tl = new TextLayout(one, run.font, FRC);
                gp.append(tl.getOutline(AffineTransform.getTranslateInstance(x, 0)), false);
                x += tl.getAdvance() + ls + (cp == ' ' ? wsp : 0);
                idx += Character.charCount(cp);
            }
            glyphs = gp;
        } else {
            glyphs = new TextLayout(run.text, run.font, FRC).getOutline(null);
        }
        AffineTransform t = new AffineTransform();
        if (run.path != null) {
            double mid = run.x + run.advance / 2;
            double[] p = pointAt(run.path, mid);
            if (p == null) {
                run.outline = null;
                return;
            }
            t.translate(p[0], p[1]);
            t.rotate(p[2]);
            t.translate(-run.advance / 2, run.y);
        } else {
            t.translate(run.x, run.y);
            if (run.rot != 0) {
                t.rotate(Math.toRadians(run.rot));
            }
        }
        run.outline = t.createTransformedShape(glyphs);
        double asc = lm.getAscent();
        double desc = lm.getDescent();
        Rectangle2D cellLocal = new Rectangle2D.Double(0, -asc, run.advance, asc + desc);
        run.cell = t.createTransformedShape(cellLocal).getBounds2D();
        int deco = first.deco;
        if (deco != 0 && first.decoStyle != null) {
            double th = Math.max(1, lm.getUnderlineThickness());
            // 선택된 장식마다 기준선 기준의 사각형을 만든다.
            if ((deco & 1) != 0) {
                run.decorations.add(t.createTransformedShape(new Rectangle2D.Double(0, lm.getUnderlineOffset() - th / 2, run.advance, th)));
                run.decoStyles.add(first.decoStyle);
            }
            if ((deco & 2) != 0) {
                run.decorations.add(t.createTransformedShape(new Rectangle2D.Double(0, -asc, run.advance, th)));
                run.decoStyles.add(first.decoStyle);
            }
            if ((deco & 4) != 0) {
                double sth = Math.max(1, lm.getStrikethroughThickness());
                run.decorations.add(t.createTransformedShape(new Rectangle2D.Double(0, lm.getStrikethroughOffset() - sth / 2, run.advance, sth)));
                run.decoStyles.add(first.decoStyle);
            }
        }
    }

    /**
     * Resolves a Java font for a style: the first available family of <code>font-family</code>, the weight, the slant and the size.
     * <p>
     * 스타일에 맞는 Java 글꼴을 정한다: <code>font-family</code> 의 첫 번째 사용 가능한 계열, 굵기, 기울임, 크기.
     *
     * @param r  the renderer / 렌더러
     * @param st the style / 스타일
     * @return the font / 글꼴
     */
    private static Font fontFor(SvgRenderer r, SvgStyle st) {
        String fam = st.get("font-family");
        String name = "Serif";
        if (fam != null) {
            String[] list = fam.split(",");
            boolean found = false;
            // 목록의 이름을 차례로 보며 쓸 수 있는 첫 계열을 고른다.
            for (int i = 0; i < list.length && !found; i++) {
                String f = list[i].trim();
                if (f.length() >= 2 && (f.charAt(0) == '"' || f.charAt(0) == '\'')) {
                    f = f.substring(1, f.length() - 1).trim();
                }
                String mapped = mapFamily(f);
                if (mapped != null) {
                    name = mapped;
                    found = true;
                }
            }
            if (!found) {
                name = "SansSerif";
            }
        }
        int style = Font.PLAIN;
        if (st.fontWeight() >= 600) {
            style |= Font.BOLD;
        }
        if (st.italic()) {
            style |= Font.ITALIC;
        }
        double size = st.fontSize;
        if (!(size > 0)) {
            size = 1;
        }
        return new Font(name, style, 1).deriveFont((float) size);
    }

    /**
     * Maps a CSS family name to a Java family name, or returns null if it is not available.
     * <p>
     * CSS 계열 이름을 Java 계열 이름으로 바꾸며 사용할 수 없으면 null 을 반환한다.
     *
     * @param f the family name without quotes / 따옴표 없는 계열 이름
     * @return the Java family or null / Java 계열 또는 null
     */
    private static String mapFamily(String f) {
        String l = f.toLowerCase(Locale.ENGLISH);
        if ("serif".equals(l) || "cursive".equals(l) || "ui-serif".equals(l)) {
            return "Serif";
        }
        if ("sans-serif".equals(l) || "fantasy".equals(l) || "system-ui".equals(l) || "ui-sans-serif".equals(l) || "-apple-system".equals(l)
                || "blinkmacsystemfont".equals(l)) {
            return "SansSerif";
        }
        if ("monospace".equals(l) || "ui-monospace".equals(l)) {
            return "Monospaced";
        }
        Map<String, String> fams = installedFamilies();
        String hit = fams.get(l);
        if (hit != null) {
            return hit;
        }
        // 흔한 별칭은 가까운 논리 글꼴로 대신한다.
        if ("helvetica".equals(l) || "helvetica neue".equals(l)) {
            return fams.containsKey("arial") ? fams.get("arial") : "SansSerif";
        }
        if ("times".equals(l)) {
            return fams.containsKey("times new roman") ? fams.get("times new roman") : "Serif";
        }
        if ("courier".equals(l)) {
            return fams.containsKey("courier new") ? fams.get("courier new") : "Monospaced";
        }
        return null;
    }

    /**
     * Returns the installed font families by lower-case name.
     * <p>
     * 설치된 글꼴 계열을 소문자 이름으로 반환한다.
     *
     * @return map from lower-case name to name / 소문자 이름 → 이름
     */
    private static Map<String, String> installedFamilies() {
        Map<String, String> m = installed;
        if (m == null) {
            m = new HashMap<String, String>();
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH);
            // 설치된 계열 이름을 소문자 키로 등록한다.
            for (int i = 0; i < names.length; i++) {
                m.put(names[i].toLowerCase(Locale.ENGLISH), names[i]);
            }
            installed = m;
        }
        return m;
    }
}
