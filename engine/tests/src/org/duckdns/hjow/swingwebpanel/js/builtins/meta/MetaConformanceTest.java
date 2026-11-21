package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The ES2015 conformance walk (task P14-10): it checks every entry of {@link ConformanceTable} against a realm that was created
 * by {@link JsRealm#create()}, that is, against the complete standard library as the other workers installed it. For each entry the
 * existence, kind (data, accessor, function, constructor), function <code>name</code> and <code>length</code>, the property
 * attributes, the identity links between aliases and the prototype chains are verified.
 * <p>
 * Entries that a worker has not delivered yet are listed in {@link #PENDING} together with the owner and the reason; they are
 * reported but do not fail the test. Everything else that deviates fails the test, so a built-in that a worker claims as finished
 * cannot silently differ from the specification. The list shrinks as the workers finish; entries that match nothing any more are
 * reported as stale.
 * <p>
 * ES2015 적합성 순회(작업 P14-10): {@link ConformanceTable} 의 모든 항목을 {@link JsRealm#create()} 로 만든 realm, 즉 다른 작업자들이
 * 설치한 완전한 표준 라이브러리에 대해 점검한다. 항목마다 존재 여부, 종류(데이터/접근자/함수/생성자), 함수의 <code>name</code>,
 * <code>length</code>, 프로퍼티 속성, 별칭 사이의 동일성, 프로토타입 체인을 확인한다.
 * <p>
 * 작업자가 아직 전달하지 않은 항목은 소유자와 이유를 붙여 {@link #PENDING} 에 적으며, 보고만 하고 시험은 실패시키지 않는다. 그 밖에 규격과 다른
 * 것은 시험을 실패시키므로, 작업자가 완료했다고 주장한 내장 객체가 조용히 규격과 달라질 수 없다. 작업자가 끝낼수록 목록이 줄어들며, 더 이상
 * 아무것도 일치시키지 않는 항목은 낡은 항목으로 보고한다.
 */
public class MetaConformanceTest {

    /**
     * Entries that are known not to be finished: <code>{ id pattern, owner, reason }</code>. A pattern matches an entry id that equals it
     * or starts with it followed by a dot. Keep this list as short as possible and delete a row as soon as its feature exists.
     * <p>
     * 아직 끝나지 않은 것으로 알려진 항목: <code>{ id 패턴, 소유자, 이유 }</code>. 패턴은 같은 id 이거나 패턴 뒤에 점이 이어지는 id 와 일치한다.
     * 목록은 최대한 짧게 유지하며 기능이 생기면 바로 그 행을 지운다.
     */
    private static final String[][] PENDING = {
    };

    /**
     * The realm under test.
     * <p>
     * 시험 대상 realm.
     */
    private JsRealm realm;

    /**
     * The global object of the realm.
     * <p>
     * realm 의 전역 객체.
     */
    private JsObject global;

    /**
     * The deviations found by the walk.
     * <p>
     * 순회에서 찾은 어긋난 점.
     */
    private List<Finding> findings;

    /**
     * The number of table items that were checked.
     * <p>
     * 점검한 표 항목의 수.
     */
    private int checked;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MetaConformanceTest() {
    }

    /**
     * Creates a fresh standard realm for each case.
     * <p>
     * 사례마다 새 표준 realm 을 만든다.
     */
    public void setUp() {
        realm = JsRealm.create();
        global = realm.getGlobalObject();
        findings = new ArrayList<Finding>();
        checked = 0;
    }

    /**
     * One deviation of the realm from the table.
     * <p>
     * realm 이 표와 어긋난 한 가지 점.
     */
    private static final class Finding {
        /**
         * The id of the entry (path and name).
         * <p>
         * 항목의 id(경로와 이름).
         */
        final String id;

        /**
         * True if the property does not exist at all, false if it exists but differs.
         * <p>
         * 프로퍼티가 아예 없으면 true, 있지만 다르면 false.
         */
        final boolean missing;

        /**
         * What differs.
         * <p>
         * 무엇이 다른지.
         */
        final String message;

        /**
         * Creates a finding.
         * <p>
         * 어긋난 점을 만든다.
         *
         * @param id      the entry id / 항목 id
         * @param missing true if missing / 없으면 true
         * @param message the description / 설명
         */
        Finding(String id, boolean missing, String message) {
            this.id = id;
            this.missing = missing;
            this.message = message;
        }

        /**
         * Returns the finding as one line.
         * <p>
         * 어긋난 점을 한 줄로 돌려준다.
         *
         * @return the text / 문자열
         */
        public String toString() {
            return id + (missing ? " [missing]" : " [wrong]") + ": " + message;
        }
    }

    /**
     * The exception that carries one deviation out of the verification helpers.
     * <p>
     * 점검 보조 메서드에서 어긋난 점 하나를 밖으로 전달하는 예외.
     */
    private static final class Problem extends RuntimeException {
        /**
         * Serialization version.
         * <p>
         * 직렬화 버전.
         */
        private static final long serialVersionUID = 1L;

        /**
         * True if the property does not exist.
         * <p>
         * 프로퍼티가 없으면 true.
         */
        final boolean missing;

        /**
         * Creates the exception.
         * <p>
         * 예외를 만든다.
         *
         * @param message the description / 설명
         * @param missing true if missing / 없으면 true
         */
        Problem(String message, boolean missing) {
            super(message);
            this.missing = missing;
        }

        /**
         * Creates a "present but different" problem.
         * <p>
         * "있지만 다름" 문제를 만든다.
         *
         * @param message the description / 설명
         * @return the problem / 문제
         */
        static Problem wrong(String message) {
            return new Problem(message, false);
        }

        /**
         * Creates a "does not exist" problem.
         * <p>
         * "없음" 문제를 만든다.
         *
         * @param message the description / 설명
         * @return the problem / 문제
         */
        static Problem absent(String message) {
            return new Problem(message, true);
        }
    }

    // ------------------------------------------------------------------ the cases

    /**
     * The table itself is well formed: known kinds, items present, ids unique, the allow-list rows complete.
     * <p>
     * 표 자체의 형식이 올바르다: 알려진 종류, 항목 존재, id 유일, 허용 목록 행이 완전함.
     */
    public void testTableIsWellFormed() {
        String known = " ctor ctor0 fn getter const tag val obj rfn lfn accessor robj link chain ";
        Set<String> ids = new HashSet<String>();
        List<String> lines = ConformanceTable.lines();
        // 표의 모든 줄을 읽어 종류와 항목의 형식, id 중복을 검사한다.
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int colon = line.indexOf(": ");
            Check.isTrue(colon > 0, "line has a path: " + line);
            String rest = line.substring(colon + 2);
            int sp = rest.indexOf(' ');
            Check.isTrue(sp > 0, "line has items: " + line);
            String kind = rest.substring(0, sp);
            Check.isTrue(known.indexOf(" " + kind + " ") >= 0, "known kind '" + kind + "' in: " + line);
            String[] parts = splitItems(kind, rest.substring(sp + 1));
            // 한 줄의 항목을 하나씩 검사한다.
            for (int j = 0; j < parts.length; j++) {
                Check.isTrue(parts[j].length() > 0, "no empty item in: " + line);
                boolean needsEquals = "link".equals(kind) || "chain".equals(kind) || "tag".equals(kind);
                Check.isTrue(!needsEquals || parts[j].indexOf('=') > 0, "item '" + parts[j] + "' needs '=' in: " + line);
                String id = itemId(line.substring(0, colon), kind, parts[j]);
                Check.isTrue(ids.add(id), "entry id is unique: " + id);
            }
        }
        // 허용 목록의 모든 행이 패턴, 소유자, 이유를 가지는지 확인한다.
        for (int i = 0; i < PENDING.length; i++) {
            Check.equal(3, PENDING[i].length, "allow-list row " + i + " has pattern, owner and reason");
            Check.isTrue(PENDING[i][0].length() > 0 && PENDING[i][1].length() > 0 && PENDING[i][2].length() > 0, "allow-list row " + i + " is filled");
        }
    }

    /**
     * A standard realm contains the meta library installed by this worker (so that the installer is loaded by the standard order).
     * <p>
     * 표준 realm 이 이 작업자의 meta 라이브러리를 포함한다(표준 순서가 설치기를 불러오는지 확인).
     */
    public void testStandardRealmInstallsMetaLibrary() {
        String[] names = { "Proxy", "Reflect", "Promise", "escape", "unescape" };
        // 이 작업자의 전역 이름이 모두 있는지 확인한다.
        for (int i = 0; i < names.length; i++) {
            Check.notNull(global.getOwnProperty(names[i]), "global " + names[i] + " exists in JsRealm.create()");
        }
    }

    /**
     * Walks the whole table over the standard realm. Deviations that are not listed in {@link #PENDING} fail the case; the listed ones
     * and the totals are printed.
     * <p>
     * 표 전체를 표준 realm 에 대해 훑는다. {@link #PENDING} 에 없는 어긋남은 사례를 실패시키며, 목록에 있는 것과 합계는 출력한다.
     */
    public void testWholeTableAgainstRealm() {
        List<String> lines = ConformanceTable.lines();
        // 표의 모든 줄을 realm 에 대해 점검한다.
        for (int i = 0; i < lines.size(); i++) {
            checkLine(lines.get(i));
        }
        int[] hits = new int[PENDING.length];
        List<Finding> unexpected = new ArrayList<Finding>();
        int pending = 0;
        int missing = 0;
        // 어긋난 점마다 허용 목록에 있는지 가려 낸다.
        for (int i = 0; i < findings.size(); i++) {
            Finding f = findings.get(i);
            if (f.missing) {
                missing++;
            }
            int row = pendingRow(f.id);
            if (row >= 0) {
                hits[row]++;
                pending++;
            } else {
                unexpected.add(f);
            }
        }
        System.out.println("ES2015 conformance table: " + checked + " entries checked, " + (checked - findings.size()) + " pass, "
                + missing + " missing, " + (findings.size() - missing) + " wrong; " + pending + " pending (allow-listed), "
                + unexpected.size() + " unexpected");
        // 허용 목록의 행마다 해당 항목 수와 이유를 출력하고 낡은 행을 표시한다.
        for (int i = 0; i < PENDING.length; i++) {
            if (hits[i] == 0) {
                System.out.println("  [stale allow-list row] " + PENDING[i][0] + " (" + PENDING[i][1] + "): no longer needed");
            } else {
                System.out.println("  [pending] " + PENDING[i][0] + " x" + hits[i] + " (" + PENDING[i][1] + "): " + PENDING[i][2]);
            }
        }
        if (!unexpected.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(unexpected.size()).append(" deviation(s) from the ES2015 table that are not on the allow-list:");
            // 앞쪽 일부만 실패 메시지에 싣는다.
            for (int i = 0; i < unexpected.size() && i < 60; i++) {
                sb.append("\n  ").append(unexpected.get(i));
            }
            if (unexpected.size() > 60) {
                sb.append("\n  ... and ").append(unexpected.size() - 60).append(" more");
            }
            Check.fail(sb.toString());
        }
    }

    /**
     * The walk itself detects deviations: after a missing function, a wrong function length, a wrongly enumerable property and a
     * wrong prototype were planted in a realm, exactly those four entries are reported.
     * <p>
     * 순회 자체가 어긋남을 찾아낸다: 없는 함수, 틀린 함수 length, 잘못 열거 가능한 프로퍼티, 틀린 프로토타입을 realm 에 심으면 정확히 그 네 항목이
     * 보고된다.
     */
    public void testWalkDetectsPlantedDeviations() {
        JsObject arrayProto = (JsObject) ((JsObject) global.get("Array")).get("prototype");
        Check.isTrue(arrayProto.delete("map"), "planted: Array.prototype.map deleted");
        JsObject math = (JsObject) global.get("Math");
        math.defineBuiltin("min", realm.newFunction("min", 5, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }));
        JsObject objectProto = realm.getObjectPrototype();
        objectProto.defineOwnProperty("hasOwnProperty", PropertyDescriptor.data(objectProto.get("hasOwnProperty"), true, true, true));
        JsObject reflect = (JsObject) global.get("Reflect");
        reflect.setPrototypeOf(null);
        List<String> lines = ConformanceTable.lines();
        // 표의 모든 줄을 심은 realm 에 대해 점검한다.
        for (int i = 0; i < lines.size(); i++) {
            checkLine(lines.get(i));
        }
        Set<String> ids = new HashSet<String>();
        // 보고된 어긋남의 id 를 모은다.
        for (int i = 0; i < findings.size(); i++) {
            ids.add(findings.get(i).id);
        }
        Check.equal(4, findings.size(), "exactly the planted deviations are reported: " + findings);
        Check.isTrue(ids.contains("Array.prototype.map"), "missing function reported");
        Check.isTrue(ids.contains("Math.min"), "wrong length reported");
        Check.isTrue(ids.contains("Object.prototype.hasOwnProperty"), "wrong attributes reported");
        Check.isTrue(ids.contains("chain:Reflect"), "wrong prototype reported");
    }

    /**
     * The global object exposes no Java or host-engine entry points (names of the Rhino/Nashorn style bridges and shell functions).
     * Other global names that are not part of the ES2015 table are printed for information.
     * <p>
     * 전역 객체가 Java 나 호스트 엔진의 진입점(Rhino/Nashorn 방식의 연결과 셸 함수 이름)을 노출하지 않는다. ES2015 표에 없는 다른 전역 이름은
     * 참고용으로 출력한다.
     */
    public void testNoJavaOrShellGlobals() {
        String[] forbidden = { "Packages", "java", "javax", "JavaImporter", "JavaAdapter", "importPackage", "importClass", "Java", "load",
                "loadWithNewGlobal", "exit", "quit", "readFile", "readLine", "System", "Runtime" };
        // 금지된 이름이 전역에 있는지 하나씩 확인한다.
        for (int i = 0; i < forbidden.length; i++) {
            Check.isNull(global.getOwnProperty(forbidden[i]), "global must not define " + forbidden[i]);
        }
        Set<String> table = new HashSet<String>();
        List<String> lines = ConformanceTable.lines();
        // 표의 전역 항목 이름을 모은다.
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("global: ") || line.startsWith("global: chain ")) {
                continue;
            }
            String rest = line.substring("global: ".length());
            String[] parts = rest.substring(rest.indexOf(' ') + 1).split(" ");
            // 항목에서 '=' 앞의 이름만 취한다.
            for (int j = 0; j < parts.length; j++) {
                int eq = parts[j].indexOf('=');
                table.add(eq >= 0 ? parts[j].substring(0, eq) : parts[j]);
            }
        }
        StringBuilder extra = new StringBuilder();
        List<Object> keys = global.ownKeys();
        // 전역의 문자열 키 중 표에 없는 것을 모은다.
        for (int i = 0; i < keys.size(); i++) {
            Object k = keys.get(i);
            if (k instanceof String && !table.contains(k)) {
                extra.append(extra.length() == 0 ? "" : ", ").append(k);
            }
        }
        System.out.println("global names outside the ES2015 table: " + extra);
    }

    /**
     * Lists the own properties of every object of the table that the table does not name (the extensions beyond ES2015, such as
     * later editions' methods, and anything unexpected). The list is printed for the conformance record; the case fails only if a
     * property is named like a host or Java bridge (<code>$</code> or a Java package style name).
     * <p>
     * 표에 나오는 모든 객체에 대해 표가 언급하지 않는 자체 프로퍼티(후속 판의 메서드 같은 ES2015 이후의 확장과 예상 밖의 것)를 나열한다. 목록은
     * 적합성 기록용으로 출력하며, 호스트나 Java 연결처럼 보이는 이름(<code>$</code> 나 Java 패키지 형태)이 있을 때만 실패한다.
     */
    public void testPropertiesBeyondTheTable() {
        Map<String, Set<String>> named = new LinkedHashMap<String, Set<String>>();
        List<String> lines = ConformanceTable.lines();
        // 표의 줄에서 객체 경로별로 이름이 나온 항목을 모은다.
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int colon = line.indexOf(": ");
            String path = line.substring(0, colon);
            String rest = line.substring(colon + 2);
            int sp = rest.indexOf(' ');
            String kind = rest.substring(0, sp);
            if ("chain".equals(kind)) {
                continue;
            }
            Set<String> set = named.get(path);
            if (set == null) {
                set = new HashSet<String>();
                named.put(path, set);
            }
            String[] parts = splitItems(kind, rest.substring(sp + 1));
            // 항목에서 '=' 앞의 이름을 취한다.
            for (int j = 0; j < parts.length; j++) {
                int eq = parts[j].indexOf('=');
                set.add(eq >= 0 ? parts[j].substring(0, eq) : parts[j]);
            }
        }
        List<String> suspicious = new ArrayList<String>();
        int total = 0;
        // 경로마다 객체의 자체 키 가운데 표에 없는 것을 찾는다.
        for (Map.Entry<String, Set<String>> entry : named.entrySet()) {
            JsValue h;
            try {
                h = resolve(entry.getKey());
            } catch (Problem p) {
                continue;
            }
            if (!(h instanceof JsObject)) {
                continue;
            }
            List<String> extras = new ArrayList<String>();
            List<Object> keys = ((JsObject) h).ownKeys();
            // 객체의 자체 키를 하나씩 표와 대조한다.
            for (int i = 0; i < keys.size(); i++) {
                Object k = keys.get(i);
                String n = k instanceof JsSymbol ? "@@" + ((JsSymbol) k).getDescription().replaceFirst("^Symbol\\.", "") : String.valueOf(k);
                if (entry.getValue().contains(n) || "length".equals(n) || "name".equals(n) || "prototype".equals(n) || "constructor".equals(n)) {
                    continue;
                }
                extras.add(n);
                if (n.indexOf('$') >= 0 || n.startsWith("java") || n.startsWith("Java")) {
                    suspicious.add(entry.getKey() + "." + n);
                }
            }
            if (!extras.isEmpty()) {
                total += extras.size();
                System.out.println("  beyond the table: " + entry.getKey() + " -> " + extras);
            }
        }
        System.out.println("own properties beyond the ES2015 table: " + total);
        Check.isTrue(suspicious.isEmpty(), "no host or Java bridge names: " + suspicious);
    }

    // ------------------------------------------------------------------ table walking

    /**
     * Splits the item part of a table line into items. A <code>tag</code> line has one item that may contain spaces.
     * <p>
     * 표의 줄에서 항목 부분을 항목들로 나눈다. <code>tag</code> 줄은 공백을 포함할 수 있는 항목 하나뿐이다.
     *
     * @param kind  the kind word / 종류 단어
     * @param items the item text / 항목 문자열
     * @return the items / 항목들
     */
    private static String[] splitItems(String kind, String items) {
        if ("tag".equals(kind)) {
            return new String[] { items };
        }
        return items.split(" ");
    }

    /**
     * Computes the entry id of an item: the path and the item name (the global root is left out).
     * <p>
     * 항목의 id 를 계산한다: 경로와 항목 이름(전역 루트는 생략).
     *
     * @param path the line path / 줄의 경로
     * @param kind the kind word / 종류 단어
     * @param item the item text / 항목 문자열
     * @return the id / id
     */
    private static String itemId(String path, String kind, String item) {
        int eq = item.indexOf('=');
        String name = eq >= 0 ? item.substring(0, eq) : item;
        if ("chain".equals(kind)) {
            return "chain:" + name;
        }
        return "global".equals(path) ? name : path + "." + name;
    }

    /**
     * Returns the allow-list row that covers an entry id, or -1.
     * <p>
     * 항목 id 를 포함하는 허용 목록의 행 번호를 돌려준다. 없으면 -1.
     *
     * @param id the entry id / 항목 id
     * @return the row index or -1 / 행 번호 또는 -1
     */
    private static int pendingRow(String id) {
        // 허용 목록의 행을 차례로 비교한다.
        for (int i = 0; i < PENDING.length; i++) {
            String p = PENDING[i][0];
            if (id.equals(p) || id.startsWith(p + ".")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks one table line.
     * <p>
     * 표의 한 줄을 점검한다.
     *
     * @param line the table line / 표의 줄
     */
    private void checkLine(String line) {
        int colon = line.indexOf(": ");
        String path = line.substring(0, colon);
        String rest = line.substring(colon + 2);
        int sp = rest.indexOf(' ');
        String kind = rest.substring(0, sp);
        String[] parts = splitItems(kind, rest.substring(sp + 1));
        JsObject holder = null;
        String holderProblem = null;
        // chain 줄은 경로를 쓰지 않으므로 보유 객체를 찾지 않는다.
        if (!"chain".equals(kind)) {
            try {
                JsValue h = resolve(path);
                if (h instanceof JsObject) {
                    holder = (JsObject) h;
                } else {
                    holderProblem = path + " is not an object";
                }
            } catch (Problem p) {
                holderProblem = p.getMessage();
            }
        }
        // 줄의 항목을 하나씩 점검한다.
        for (int i = 0; i < parts.length; i++) {
            String item = parts[i];
            String id = itemId(path, kind, item);
            checked++;
            if ("chain".equals(kind)) {
                try {
                    verifyChain(item);
                } catch (Problem p) {
                    findings.add(new Finding(id, p.missing, p.getMessage()));
                }
                continue;
            }
            if (holder == null) {
                findings.add(new Finding(id, true, "holder not available: " + holderProblem));
                continue;
            }
            int eq = item.indexOf('=');
            String name = eq >= 0 ? item.substring(0, eq) : item;
            String arg = eq >= 0 ? item.substring(eq + 1) : null;
            try {
                verify(holder, kind, name, arg);
            } catch (Problem p) {
                findings.add(new Finding(id, p.missing, p.getMessage()));
            }
        }
    }

    /**
     * Splits a path into its tokens: dot separated names, where <code>%Name.with.dots%</code> intrinsics are one token.
     * <p>
     * 경로를 토큰으로 나눈다: 점으로 구분한 이름이며 <code>%이름.점%</code> 형태의 내재 객체는 한 토큰이다.
     *
     * @param path the path / 경로
     * @return the tokens / 토큰
     */
    private static List<String> tokenize(String path) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        // 경로를 앞에서부터 읽으며 토큰을 하나씩 잘라 낸다.
        while (i < path.length()) {
            int end;
            if (path.charAt(i) == '%') {
                end = path.indexOf('%', i + 1) + 1;
            } else {
                end = path.indexOf('.', i);
                if (end < 0) {
                    end = path.length();
                }
            }
            out.add(path.substring(i, end));
            i = end;
            if (i < path.length() && path.charAt(i) == '.') {
                i++;
            }
        }
        return out;
    }

    /**
     * Resolves a path (global rooted, or starting with an intrinsic) to a value, following own data properties.
     * <p>
     * 경로(전역 기준 또는 내재 객체로 시작)를 값으로 해석한다. 자신의 데이터 프로퍼티를 따라간다.
     *
     * @param path the path / 경로
     * @return the value / 값
     */
    private JsValue resolve(String path) {
        List<String> tokens = tokenize(path);
        JsValue cur = global;
        int start = 0;
        String walked = "";
        if ("global".equals(tokens.get(0))) {
            start = 1;
            walked = "global";
        } else if (tokens.get(0).startsWith("%")) {
            JsObject in = realm.getIntrinsic(tokens.get(0));
            if (in == null) {
                throw Problem.absent("intrinsic " + tokens.get(0) + " is not registered");
            }
            cur = in;
            start = 1;
            walked = tokens.get(0);
        }
        // 토큰마다 현재 객체의 자체 프로퍼티를 따라 내려간다.
        for (int i = start; i < tokens.size(); i++) {
            if (!(cur instanceof JsObject)) {
                throw Problem.wrong(walked + " is not an object");
            }
            PropertyDescriptor d = ((JsObject) cur).getOwnProperty(keyOf(tokens.get(i)));
            walked = walked.length() == 0 ? tokens.get(i) : walked + "." + tokens.get(i);
            if (d == null) {
                throw Problem.absent(walked + " does not exist");
            }
            if (!d.isData()) {
                throw Problem.wrong(walked + " is an accessor property");
            }
            cur = d.value;
        }
        return cur;
    }

    /**
     * Converts an item name to a property key: names starting with <code>@@</code> are well-known symbols (read from the Symbol
     * constructor), others are strings.
     * <p>
     * 항목 이름을 프로퍼티 키로 바꾼다. <code>@@</code> 로 시작하면 잘 알려진 심볼(Symbol 생성자에서 읽음), 아니면 문자열이다.
     *
     * @param name the item name / 항목 이름
     * @return the key / 키
     */
    private Object keyOf(String name) {
        if (!name.startsWith("@@")) {
            return name;
        }
        JsValue symbolCtor = global.get("Symbol");
        if (!(symbolCtor instanceof JsObject)) {
            throw Problem.absent("global Symbol does not exist (needed for " + name + ")");
        }
        JsValue s = ((JsObject) symbolCtor).get(name.substring(2));
        if (!(s instanceof JsSymbol)) {
            throw Problem.absent("Symbol." + name.substring(2) + " does not exist");
        }
        return s;
    }

    /**
     * Returns the expected function name for an item name: symbol keys give <code>[Symbol.x]</code>.
     * <p>
     * 항목 이름에 대해 기대하는 함수 이름을 돌려준다. 심볼 키는 <code>[Symbol.x]</code> 가 된다.
     *
     * @param name the item name / 항목 이름
     * @return the function name / 함수 이름
     */
    private static String functionName(String name) {
        return name.startsWith("@@") ? "[Symbol." + name.substring(2) + "]" : name;
    }

    // ------------------------------------------------------------------ verification helpers

    /**
     * Verifies one item of a kind on a holder object; throws a {@link Problem} for the first deviation.
     * <p>
     * 보유 객체에서 한 종류의 항목 하나를 점검한다. 처음 어긋난 점을 {@link Problem} 으로 던진다.
     *
     * @param holder the object that must own the property / 프로퍼티를 가져야 하는 객체
     * @param kind   the kind word / 종류 단어
     * @param name   the property name / 프로퍼티 이름
     * @param arg    the text after '=' or null / '=' 뒤의 문자열 또는 null
     */
    private void verify(JsObject holder, String kind, String name, String arg) {
        PropertyDescriptor d = holder.getOwnProperty(keyOf(name));
        if (d == null) {
            throw Problem.absent("no own property " + name);
        }
        String fname = functionName(name);
        if ("ctor".equals(kind) || "ctor0".equals(kind)) {
            expectData(d, name, true, false, true);
            JsObject f = expectFunction(d.value, name, true);
            expectNameLength(f, name, fname, arg);
            PropertyDescriptor p = f.getOwnProperty("prototype");
            if ("ctor0".equals(kind)) {
                if (p != null) {
                    throw Problem.wrong(name + " must not have a prototype property");
                }
                return;
            }
            verifyConstructorPrototype(f, p, name);
        } else if ("fn".equals(kind)) {
            expectData(d, name, true, false, true);
            JsObject f = expectFunction(d.value, name, false);
            expectNameLength(f, name, fname, arg);
            if (f.getOwnProperty("prototype") != null) {
                throw Problem.wrong(name + " must not have a prototype property");
            }
        } else if ("rfn".equals(kind) || "lfn".equals(kind)) {
            expectData(d, name, false, false, "rfn".equals(kind));
            JsObject f = expectFunction(d.value, name, false);
            expectNameLength(f, name, fname, arg);
        } else if ("getter".equals(kind)) {
            expectAccessor(d, name, "get ".concat(fname), "get");
            if (d.set != null && d.set != Js.UNDEFINED) {
                throw Problem.wrong(name + " must not have a setter");
            }
        } else if ("accessor".equals(kind)) {
            expectAccessor(d, name, "get ".concat(fname), "get");
            expectAccessor(d, name, "set ".concat(fname), "set");
        } else if ("const".equals(kind)) {
            expectData(d, name, false, false, false);
        } else if ("val".equals(kind)) {
            expectData(d, name, true, false, true);
        } else if ("obj".equals(kind) || "robj".equals(kind)) {
            expectData(d, name, "obj".equals(kind), false, true);
            if (!(d.value instanceof JsObject) || ((JsObject) d.value).isCallable()) {
                throw Problem.wrong(name + " must be a non-callable object");
            }
        } else if ("tag".equals(kind)) {
            expectData(d, name, false, false, true);
            if (!(d.value instanceof JsString) || !arg.equals(((JsString) d.value).getValue())) {
                throw Problem.wrong(name + " is " + d.value + " but must be \"" + arg + "\"");
            }
        } else if ("link".equals(kind)) {
            expectData(d, name, true, false, true);
            JsValue target = resolve(arg);
            if (!Ops.sameValue(d.value, target)) {
                throw Problem.wrong(name + " is not the same function object as " + arg);
            }
        } else {
            throw new IllegalStateException("unknown kind " + kind);
        }
    }

    /**
     * Verifies the <code>prototype</code> property of a constructor and the <code>constructor</code> link back.
     * <p>
     * 생성자의 <code>prototype</code> 프로퍼티와 되돌아오는 <code>constructor</code> 연결을 점검한다.
     *
     * @param ctor  the constructor / 생성자
     * @param proto the descriptor of its prototype property or null / prototype 프로퍼티 서술자 또는 null
     * @param name  the display name / 표시용 이름
     */
    private void verifyConstructorPrototype(JsObject ctor, PropertyDescriptor proto, String name) {
        if (proto == null) {
            throw Problem.wrong(name + " has no prototype property");
        }
        expectData(proto, name + ".prototype", false, false, false);
        if (!(proto.value instanceof JsObject)) {
            throw Problem.wrong(name + ".prototype is not an object");
        }
        PropertyDescriptor back = ((JsObject) proto.value).getOwnProperty("constructor");
        if (back == null) {
            throw Problem.wrong(name + ".prototype has no constructor property");
        }
        expectData(back, name + ".prototype.constructor", true, false, true);
        if (!Ops.sameValue(back.value, ctor)) {
            throw Problem.wrong(name + ".prototype.constructor is not " + name);
        }
    }

    /**
     * Verifies a prototype chain item <code>a=b</code>: the prototype of the object a is the object b (or null).
     * <p>
     * 프로토타입 체인 항목 <code>a=b</code> 를 점검한다. 객체 a 의 프로토타입이 객체 b(또는 null)이다.
     *
     * @param item the item text / 항목 문자열
     */
    private void verifyChain(String item) {
        int eq = item.indexOf('=');
        String left = item.substring(0, eq);
        String right = item.substring(eq + 1);
        JsValue a = resolve(left);
        if (!(a instanceof JsObject)) {
            throw Problem.wrong(left + " is not an object");
        }
        JsObject proto = ((JsObject) a).getPrototypeOf();
        if ("null".equals(right)) {
            if (proto != null) {
                throw Problem.wrong("the prototype of " + left + " must be null");
            }
            return;
        }
        JsValue b = resolve(right);
        if (proto != b) {
            throw Problem.wrong("the prototype of " + left + " is not " + right);
        }
    }

    /**
     * Requires a data property with the given attributes.
     * <p>
     * 주어진 속성을 가진 데이터 프로퍼티를 요구한다.
     *
     * @param d            the descriptor / 서술자
     * @param label        the display name / 표시용 이름
     * @param writable     the expected writable flag / 기대하는 writable
     * @param enumerable   the expected enumerable flag / 기대하는 enumerable
     * @param configurable the expected configurable flag / 기대하는 configurable
     */
    private static void expectData(PropertyDescriptor d, String label, boolean writable, boolean enumerable, boolean configurable) {
        if (!d.isData()) {
            throw Problem.wrong(label + " must be a data property but is an accessor property");
        }
        if (d.isWritableTrue() != writable || d.isEnumerableTrue() != enumerable || d.isConfigurableTrue() != configurable) {
            throw Problem.wrong(label + " has attributes [" + flags(d.isWritableTrue(), d.isEnumerableTrue(), d.isConfigurableTrue())
                    + "] but must have [" + flags(writable, enumerable, configurable) + "] (writable, enumerable, configurable)");
        }
    }

    /**
     * Formats three attribute flags as <code>w e c</code> letters with dashes for false.
     * <p>
     * 속성 플래그 셋을 <code>w e c</code> 글자로 만든다. false 는 대시이다.
     *
     * @param w the writable flag / writable
     * @param e the enumerable flag / enumerable
     * @param c the configurable flag / configurable
     * @return the text / 문자열
     */
    private static String flags(boolean w, boolean e, boolean c) {
        return (w ? "w" : "-") + (e ? "e" : "-") + (c ? "c" : "-");
    }

    /**
     * Requires an accessor property half (getter or setter) that is a non-constructor function with the expected name and the
     * right length, and non-enumerable, configurable attributes.
     * <p>
     * 접근자 프로퍼티의 한쪽(getter 나 setter)이 기대하는 이름과 알맞은 length 를 가진 비생성자 함수이고 열거 불가, 설정 가능 속성인지 요구한다.
     *
     * @param d        the descriptor / 서술자
     * @param label    the display name / 표시용 이름
     * @param fullName the expected function name / 기대하는 함수 이름
     * @param half     "get" or "set" / "get" 이나 "set"
     */
    private static void expectAccessor(PropertyDescriptor d, String label, String fullName, String half) {
        if (!d.isAccessor()) {
            throw Problem.wrong(label + " must be an accessor property");
        }
        if (d.isEnumerableTrue() || !d.isConfigurableTrue()) {
            throw Problem.wrong(label + " must be non-enumerable and configurable");
        }
        JsValue fn = "get".equals(half) ? d.get : d.set;
        JsObject f = expectFunction(fn, label + " " + half + "ter", false);
        expectNameLength(f, label + " " + half + "ter", fullName, "get".equals(half) ? "0" : "1");
    }

    /**
     * Requires a callable value with the expected constructor-ness.
     * <p>
     * 호출 가능하며 생성자 여부가 기대와 같은 값을 요구한다.
     *
     * @param v           the value / 값
     * @param label       the display name / 표시용 이름
     * @param constructor true if it must be a constructor / 생성자여야 하면 true
     * @return the function object / 함수 객체
     */
    private static JsObject expectFunction(JsValue v, String label, boolean constructor) {
        if (!(v instanceof JsObject) || !((JsObject) v).isCallable()) {
            throw Problem.wrong(label + " must be a function but is " + v);
        }
        JsObject f = (JsObject) v;
        if (f.isConstructor() != constructor) {
            throw Problem.wrong(label + (constructor ? " must be a constructor" : " must not be a constructor"));
        }
        return f;
    }

    /**
     * Requires the own <code>name</code> and <code>length</code> properties of a built-in function (read-only, non-enumerable,
     * configurable) to have the expected values.
     * <p>
     * 내장 함수의 자체 <code>name</code>, <code>length</code> 프로퍼티(읽기 전용, 열거 불가, 설정 가능)가 기대한 값인지 요구한다.
     *
     * @param f            the function / 함수
     * @param label        the display name / 표시용 이름
     * @param expectedName the expected name / 기대하는 이름
     * @param length       the expected length text or null to skip / 기대하는 length 문자열. null 이면 점검하지 않음
     */
    private static void expectNameLength(JsObject f, String label, String expectedName, String length) {
        PropertyDescriptor nd = f.getOwnProperty("name");
        if (nd == null) {
            throw Problem.wrong(label + " has no own name property");
        }
        expectData(nd, label + ".name", false, false, true);
        if (!(nd.value instanceof JsString) || !expectedName.equals(((JsString) nd.value).getValue())) {
            throw Problem.wrong(label + ".name is " + nd.value + " but must be \"" + expectedName + "\"");
        }
        if (length == null) {
            return;
        }
        PropertyDescriptor ld = f.getOwnProperty("length");
        if (ld == null) {
            throw Problem.wrong(label + " has no own length property");
        }
        expectData(ld, label + ".length", false, false, true);
        double want = Double.parseDouble(length);
        if (!(ld.value instanceof JsNumber) || ((JsNumber) ld.value).getValue() != want) {
            throw Problem.wrong(label + ".length is " + ld.value + " but must be " + length);
        }
    }
}
