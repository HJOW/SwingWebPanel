package org.duckdns.hjow.swingwebpanel.core;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Inspection of the static state of the product code (plan item P21-05): a static field is a place where an object can outlive the page and
 * the component that created it, so every static field that can hold changing state — a thread pool, a thread, a timer, a thread local, a
 * reference, a cache, a registry, a mutable singleton, a field that is not final — must be in the reviewed list below with the reason why it
 * does not hold on to a component, a page or a script realm. A new static field of that kind makes this test fail until somebody has looked at
 * it and written down why it is fine; a reviewed entry whose field is gone also fails, so the list cannot rot. Constant tables (final
 * arrays, sets and maps with an upper case name that are only filled once) are not listed.
 * <p>
 * 제품 코드의 정적 상태 점검(계획 P21-05): 정적 필드는 객체가 그것을 만든 페이지와 컴포넌트보다 오래 살 수 있는 곳이므로, 바뀌는 상태를 가질 수 있는 정적
 * 필드(스레드 풀, 스레드, 타이머, 스레드 로컬, 참조, 캐시, 등록부, 바뀌는 싱글턴, final 이 아닌 필드)는 모두 아래의 검토 목록에 컴포넌트, 페이지, 스크립트
 * realm 을 붙잡지 않는 이유와 함께 있어야 한다. 그런 새 정적 필드가 생기면 누군가 살펴보고 괜찮은 이유를 적을 때까지 이 시험이 실패하고, 필드가 없어진
 * 검토 항목도 실패하므로 목록이 낡을 수 없다. 상수 표(한 번만 채우는 대문자 이름의 final 배열, 집합, 맵)는 목록에 없다.
 */
public class StaticStateAuditTest {

    /**
     * The name parts that mark a static field as runtime state even if it is final and written in capitals.
     * <p>
     * final 이고 대문자로 쓰였어도 실행 중 상태로 보는 이름 조각.
     */
    private static final Pattern STATE_NAME = Pattern.compile(".*(CACHE|POOL|REGISTRY|CLOCK|WORKER|HOOKS|OWNERS|FLAGS|LIMITS|IMAGES|FILES|FONTS|METRICS|ZONES|NESTING|JOINING"
            + "|DECODERS|CODECS|INSTALLED).*");

    /**
     * The reviewed static fields (class#field) and why each is fine.
     * <p>
     * 검토한 정적 필드(class#field)와 괜찮은 이유.
     */
    private static final Map<String, String> REVIEWED = new LinkedHashMap<String, String>();

    static {
        // 프로세스 전체가 공유하는 스레드와 시계: 컴포넌트나 페이지를 강하게 붙잡지 않는다.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.core.DocumentScheduler#CLOCK", "shared daemon clock, tasks hold their page weakly (DocumentSchedulerTest), idle threads end");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.core.PageCleanup#WORKER", "shared daemon release worker, a task only holds the page that is being released, idle threads end");
        // 약한 키를 쓰는 맵: 값이 키를 되짚어 붙잡지 않는다.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.html.ParserNodeData#FORM_OWNERS", "WeakHashMap with weak values (element to its form owner)");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.html.ParserNodeData#SCRIPT_FLAGS", "WeakHashMap, the values are plain flags without node references");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.builtins.datajson.RegExpBuiltins#CANCEL_HOOKS", "WeakHashMap, the hook refers to the limits object only, never to the realm key");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.builtins.datajson.RegExpBuiltins#STEP_LIMITS", "WeakHashMap, the values are Long");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DateBuiltins#ZONES", "WeakHashMap, the values are TimeZone objects without realm references");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.dom.FormBindingsCollections#FILES", "WeakHashMap with weak values (the script wrapper refers to its entry and its realm)");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.render.DisplayCommands#FRAME_IMAGES", "weak keys: decoded frame to its BufferedImage, the image does not refer to the frame");
        // 크기가 제한된 캐시와 문자열/값만 담는 공유 객체.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.dom.html.PatternMatcher#CACHE", "LRU of 64 compiled patterns keyed by the pattern text, no node or realm references");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.FontResolver#WEB", "ThreadLocal of the web fonts of the page that is laid out on the layout helper thread; set and removed in the finally of LayoutEngine.layout, so no page is kept after a layout (P10-05)");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.FontResolver#FONTS", "LRU of Font objects keyed by their description");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.FontResolver#METRICS", "LRU of font metrics keyed by their description");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.FontResolver#families", "set of installed font family names, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.FontResolver#metricsGraphics", "scratch Graphics2D on a 1x1 image for measuring, no component reference");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasFont#installed", "set of installed font family names, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.image.svg.SvgText#installed", "map of installed font names, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.image.avif.Av1WedgeMasks#table", "constant AV1 wedge masks (plain bytes without references), built once under the class lock");
        // 스레드 로컬: 호출이 끝나면 비우는 재진입 방지용 자료.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.ArrayMethods#JOINING", "ThreadLocal of arrays being joined (cycle detection), cleared when join returns");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.host.PageScripts#ENTRY", "ThreadLocal of the page whose task runs outermost on the thread (the entry of the sandbox rules for navigation); set when a task starts and removed in its finally, so nothing stays after the task (P20-02)");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.image.svg.SvgImage#NESTING", "ThreadLocal nesting depth counter, restored when the decode returns");
        // 코덱 등록부: 클래스 이름과 해독기 객체(상태 없음)만 있다.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders#CODECS", "registry of stateless decoder factories, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders#DECODERS", "registry of stateless decoder factories, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders#DECODERS", "registry of stateless decoders, filled once");
        // 한 번 채우는 불변 표와 클래스 이름 목록.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.JsNumber#CACHE", "array of immutable JsNumber values for small integers, filled once");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.date.DateParser#ZONES", "constant table of time zone abbreviations (name and offset)");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders#OPTIONAL_DECODERS", "class names of optional decoders, constant");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders#OPTIONAL_DECODERS", "class names of optional decoders, constant");
        // 지연 초기화하는 공유 표와 싱글턴: 페이지와 컴포넌트를 모른다.
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser#factory", "lazily created SAXParserFactory (secure features set), no page references");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet#htmlOnly", "parsed user agent style sheet, immutable rules shared by all pages");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet#quirks", "parsed user agent style sheet, immutable rules shared by all pages");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet#standards", "parsed user agent style sheet, immutable rules shared by all pages");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry#instance", "singleton table of property definitions");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.html.EncodingLabels#javaTable", "lazily built table of encoding names");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.html.EncodingLabels#labelTable", "lazily built table of encoding labels");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences#table", "lazily built table of character references");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.js.parser.Parser#defaultRegExpValidator", "stateless validator");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.layout.VerticalText#upright", "lazily built table of code point classes");
        REVIEWED.put("org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager#instance", "process wide localStorage manager: holds stored data and weak listeners only, never a component (P16)");
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StaticStateAuditTest() {
    }

    /**
     * Lists the names of all product classes in the directory or the JAR that holds <code>SwingWebPanel</code>.
     * <p>
     * <code>SwingWebPanel</code> 이 들어 있는 디렉터리나 JAR 의 모든 제품 클래스 이름을 나열한다.
     *
     * @return the class names / 클래스 이름
     * @throws Exception on failure / 실패 시
     */
    private static List<String> productClasses() throws Exception {
        URL location = SwingWebPanel.class.getProtectionDomain().getCodeSource().getLocation();
        File base = new File(URLDecoder.decode(location.getPath(), "UTF-8"));
        List<String> names = new ArrayList<String>();
        if (base.isDirectory()) {
            walk(base, base, names);
        } else {
            JarFile jar = new JarFile(base);
            try {
                Enumeration<JarEntry> entries = jar.entries();
                // JAR 의 항목 중 swingwebpanel 패키지의 클래스 파일만 모은다.
                while (entries.hasMoreElements()) {
                    String n = entries.nextElement().getName();
                    if (n.endsWith(".class") && n.startsWith("org/duckdns/hjow/swingwebpanel/")) {
                        names.add(n.substring(0, n.length() - 6).replace('/', '.'));
                    }
                }
            } finally {
                jar.close();
            }
        }
        return names;
    }

    /**
     * Collects the class names below a directory.
     * <p>
     * 디렉터리 아래의 클래스 이름을 모은다.
     *
     * @param root the root directory of the classes / 클래스의 루트 디렉터리
     * @param dir  the directory to read / 읽을 디렉터리
     * @param out  receives the names / 이름을 받는다
     * @throws IOException on failure / 실패 시
     */
    private static void walk(File root, File dir, List<String> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 클래스 파일은 이름으로 바꾸어 모은다.
        for (File f : files) {
            if (f.isDirectory()) {
                walk(root, f, out);
            } else if (f.getName().endsWith(".class")) {
                String rel = f.getPath().substring(root.getPath().length() + 1).replace(File.separatorChar, '.');
                out.add(rel.substring(0, rel.length() - 6));
            }
        }
    }

    /**
     * Tells whether a static field can hold changing state that outlives a page.
     * <p>
     * 정적 필드가 페이지보다 오래 사는 바뀌는 상태를 가질 수 있는지 알려 준다.
     *
     * @param f the field / 필드
     * @return true if it needs a review / 검토가 필요하면 true
     */
    private static boolean needsReview(Field f) {
        Class<?> t = f.getType();
        if (!Modifier.isStatic(f.getModifiers()) || t.isPrimitive() || f.isSynthetic() || f.getName().equals("$VALUES")) {
            return false;
        }
        boolean live = Executor.class.isAssignableFrom(t) || Thread.class.isAssignableFrom(t) || java.util.Timer.class.isAssignableFrom(t)
                || javax.swing.Timer.class.isAssignableFrom(t) || ThreadLocal.class.isAssignableFrom(t) || Reference.class.isAssignableFrom(t)
                || AtomicReference.class.isAssignableFrom(t) || java.util.WeakHashMap.class.isAssignableFrom(t);
        if (live) {
            return true;
        }
        if (!Modifier.isFinal(f.getModifiers())) {
            return t != String.class;
        }
        boolean holder = Map.class.isAssignableFrom(t) || Collection.class.isAssignableFrom(t) || t == Object.class
                || (t.isArray() && !t.getComponentType().isPrimitive());
        return holder && STATE_NAME.matcher(f.getName().toUpperCase()).matches();
    }

    /**
     * Every static field that can hold changing state is reviewed, and every review still refers to an existing field.
     * <p>
     * 바뀌는 상태를 가질 수 있는 모든 정적 필드가 검토되어 있고, 모든 검토 항목이 아직 있는 필드를 가리킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryStaticStateHolderIsReviewed() throws Exception {
        TreeSet<String> found = new TreeSet<String>();
        int classes = 0;
        // 모든 제품 클래스를 읽어 정적 필드를 검사한다(클래스 초기화는 하지 않는다).
        for (String name : productClasses()) {
            if (name.startsWith("org.duckdns.hjow.swingwebpanel.testing.")) {
                continue;
            }
            Class<?> c;
            try {
                c = Class.forName(name, false, SwingWebPanel.class.getClassLoader());
            } catch (Throwable e) {
                continue;
            }
            classes++;
            // 클래스의 정적 필드마다 검토가 필요한지 확인한다.
            for (Field f : c.getDeclaredFields()) {
                if (needsReview(f)) {
                    found.add(name + "#" + f.getName());
                }
            }
        }
        Check.isTrue(classes > 500, "the product classes were found: " + classes);
        List<String> unreviewed = new ArrayList<String>();
        // 찾은 필드 중 검토 목록에 없는 것을 모은다.
        for (String s : found) {
            if (!REVIEWED.containsKey(s)) {
                unreviewed.add(s);
            }
        }
        List<String> stale = new ArrayList<String>();
        // 검토 목록에 있으나 더는 없는(또는 검토 대상이 아닌) 필드를 모은다.
        for (String s : REVIEWED.keySet()) {
            if (!found.contains(s)) {
                stale.add(s);
            }
        }
        Check.isTrue(unreviewed.isEmpty(), "static fields that hold changing state and are not reviewed (add them to REVIEWED with the reason after looking at them): "
                + unreviewed);
        Check.isTrue(stale.isEmpty(), "reviewed fields that do not exist or do not need a review any more: " + stale);
    }
}
