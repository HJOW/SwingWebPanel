package netstormparse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.parser.Parser;

/**
 * Parses the external Netstorm bundle and module tree with the SwingWebPanel parser.
 * It does not execute JavaScript or copy game assets into this repository.
 * <p>
 * 외부 Netstorm 번들과 모듈 트리를 SwingWebPanel 파서로 읽는다.
 * JavaScript 를 실행하거나 게임 자산을 이 저장소로 복사하지 않는다.
 */
public final class NetstormParse {

    /**
     * Root directory of the selected Netstorm web tree.
     * <p>
     * 선택한 Netstorm 웹 트리의 루트 디렉터리.
     */
    private final File root;

    /**
     * All selected module files sorted by path.
     * <p>
     * 경로 순으로 정렬한 모든 선택 모듈 파일.
     */
    private final List<File> moduleFiles = new ArrayList<File>();

    /**
     * Parsed modules indexed by canonical path.
     * <p>
     * 정규 경로로 찾는 파싱한 모듈.
     */
    private final Map<String, File> modules = new HashMap<String, File>();

    /**
     * Number of static module requests checked.
     * <p>
     * 검사한 정적 모듈 요청 수.
     */
    private int moduleRequests;

    /**
     * Number of syntax errors.
     * <p>
     * 구문 오류 수.
     */
    private int syntaxErrors;

    /**
     * Number of missing relative modules.
     * <p>
     * 없는 상대 모듈 수.
     */
    private int missingModules;

    /**
     * Private constructor for one validation run.
     * <p>
     * 한 검증 실행용 비공개 생성자.
     *
     * @param root external web root / 외부 웹 루트
     */
    private NetstormParse(File root) {
        this.root = root;
    }

    /**
     * Runs the external parse check.
     * <p>
     * 외부 파싱 검사를 실행한다.
     *
     * @param args one web-root argument / 웹 루트 인수 하나
     * @throws Exception on usage or I/O failure / 사용법 또는 입출력 실패
     */
    public static void main(String[] args) throws Exception {
        boolean profile = args.length == 2 && "--bundle-profile".equals(args[1]);
        if (args.length != 1 && !profile) {
            System.err.println("Usage: netstormparse <netstorm www directory> [--bundle-profile]");
            System.exit(2);
        }
        File root = new File(args[0]).getCanonicalFile();
        if (!root.isDirectory()) {
            System.err.println("Not a directory: " + root);
            System.exit(2);
        }
        NetstormParse check = new NetstormParse(root);
        int exit = profile ? check.profileBundle() : check.run();
        System.exit(exit);
    }

    /**
     * Measures one bundle parse with the current JVM heap settings.
     * <p>
     * 현재 JVM 힙 설정으로 번들 파싱 한 번을 계측한다.
     *
     * @return success or syntax failure code / 성공 또는 구문 실패 코드
     * @throws IOException if the bundle cannot be read / 번들을 읽을 수 없으면
     */
    private int profileBundle() throws IOException {
        File bundle = new File(root, "dist/netstorm.bundle.js");
        String source = readUtf8(bundle);
        Runtime vm = Runtime.getRuntime();
        long heapBefore = vm.totalMemory() - vm.freeMemory();
        long start = System.nanoTime();
        Node tree = Parser.parseScript(source, bundle.getPath());
        long elapsed = System.nanoTime() - start;
        long heapAfter = vm.totalMemory() - vm.freeMemory();
        System.out.println("parse java=" + System.getProperty("java.version") + " chars=" + source.length()
                + " time_ms=" + elapsed / 1000000.0 + " max_heap_bytes=" + vm.maxMemory()
                + " heap_before_bytes=" + heapBefore + " heap_after_bytes=" + heapAfter + " parsed=" + (tree != null));
        return tree == null ? 1 : 0;
    }

    /**
     * Performs bundle, inline module, source module and static-link validation.
     * <p>
     * 번들, 인라인 모듈, 원본 모듈, 정적 연결 검증을 수행한다.
     *
     * @return process exit code / 프로세스 종료 코드
     * @throws IOException if a file cannot be read / 파일을 읽지 못하면
     */
    private int run() throws IOException {
        File bundle = new File(root, "dist/netstorm.bundle.js");
        File debug = new File(root, "debug.html");
        String bundleText = readUtf8(bundle);
        System.out.println("bundle: " + bundle.getCanonicalPath() + " (" + bundleText.length() + " UTF-16 code units)");
        parse(false, bundleText, bundle.getPath());

        String html = readUtf8(debug);
        List<String> inlineModules = extractModuleScripts(html);
        System.out.println("debug-inline-modules: " + inlineModules.size());
        // debug.html 의 모든 inline module 조각을 각각 Module 목표로 파싱한다.
        for (int i = 0; i < inlineModules.size(); i++) {
            parse(true, inlineModules.get(i), debug.getPath() + "#module-" + (i + 1));
        }

        collectModules(root);
        Collections.sort(moduleFiles, new FilePathOrder());
        System.out.println("source-modules: " + moduleFiles.size());
        // 모든 원본 모듈을 먼저 파싱한 뒤 요청 파일의 존재 여부를 검사한다.
        for (int i = 0; i < moduleFiles.size(); i++) {
            File module = moduleFiles.get(i);
            modules.put(module.getCanonicalPath(), module);
            String text = readUtf8(module);
            parse(true, text, relative(module));
        }
        // 정적 import 요청을 각 파일의 ProgramInfo 에서 직접 검사한다.
        for (int i = 0; i < moduleFiles.size(); i++) {
            checkRequests(moduleFiles.get(i));
        }
        System.out.println("static-imports: " + moduleRequests);
        System.out.println("syntax-errors: " + syntaxErrors + ", missing-modules: " + missingModules);
        return syntaxErrors == 0 && missingModules == 0 ? 0 : 1;
    }

    /**
     * Parses one source and prints a positioned syntax error when it fails.
     * <p>
     * 한 소스를 파싱하고 실패하면 위치가 있는 구문 오류를 출력한다.
     *
     * @param module true for a Module goal / Module 목표이면 true
     * @param text source text / 소스 텍스트
     * @param name diagnostic source name / 오류용 소스 이름
     * @return parsed ProgramInfo or null / 파싱한 ProgramInfo 또는 null
     */
    private ProgramInfo parse(boolean module, String text, String name) {
        try {
            Node node = module ? Parser.parseModule(text, name) : Parser.parseScript(text, name);
            return (ProgramInfo) node.value;
        } catch (JsException e) {
            syntaxErrors++;
            System.err.println(e.getMessage());
            return null;
        } catch (RuntimeException e) {
            syntaxErrors++;
            System.err.println(name + ": parser failure: " + e);
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Re-parses one module and verifies each relative static module request.
     * <p>
     * 모듈을 다시 파싱해 각 상대 정적 모듈 요청을 검증한다.
     *
     * @param file module file / 모듈 파일
     * @throws IOException if the source cannot be read / 소스를 읽지 못하면
     */
    private void checkRequests(File file) throws IOException {
        ProgramInfo info = parse(true, readUtf8(file), relative(file));
        if (info == null) {
            return;
        }
        // ProgramInfo 의 최초 요청 순서를 따라 상대 경로를 정규화한다.
        for (int i = 0; i < info.requestedModules.size(); i++) {
            String request = info.requestedModules.get(i);
            if (!request.startsWith("./") && !request.startsWith("../")) {
                continue;
            }
            moduleRequests++;
            File target = resolve(file, request);
            String key = target.getCanonicalPath();
            if (!target.isFile()) {
                missingModules++;
                System.err.println(relative(file) + ": missing module " + request + " -> " + target.getCanonicalPath());
            } else if (!key.startsWith(root.getCanonicalPath() + File.separator)) {
                missingModules++;
                System.err.println(relative(file) + ": module escapes root: " + request);
            } else if (!modules.containsKey(key)) {
                missingModules++;
                System.err.println(relative(file) + ": module is outside collected source set: " + request);
            }
        }
    }

    /**
     * Collects JavaScript files outside the dist directory.
     * <p>
     * dist 디렉터리 밖의 JavaScript 파일을 수집한다.
     *
     * @param dir directory to visit / 방문할 디렉터리
     */
    private void collectModules(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        // 자식 이름을 순회하되 dist 아래 번들과 생성물은 모듈 원본에서 제외한다.
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                if (!child.getName().equals("dist")) {
                    collectModules(child);
                }
            } else if (child.getName().toLowerCase(Locale.ENGLISH).endsWith(".js")) {
                moduleFiles.add(child);
            }
        }
    }

    /**
     * Extracts inline module script bodies from a narrow debug HTML page.
     * <p>
     * 좁은 범위의 debug HTML 페이지에서 inline module 스크립트 본문을 추출한다.
     *
     * @param html HTML source / HTML 소스
     * @return module script bodies / 모듈 스크립트 본문
     */
    private static List<String> extractModuleScripts(String html) {
        List<String> out = new ArrayList<String>();
        String lower = html.toLowerCase(Locale.ENGLISH);
        int at = 0;
        // type=module 태그를 찾으며 각 닫는 script 태그까지의 본문을 추출한다.
        while (true) {
            int script = lower.indexOf("<script", at);
            if (script < 0) {
                return out;
            }
            int openEnd = lower.indexOf('>', script);
            if (openEnd < 0) {
                return out;
            }
            String open = lower.substring(script, openEnd);
            int close = lower.indexOf("</script", openEnd);
            if (close < 0) {
                return out;
            }
            if (open.indexOf("type=\"module\"") >= 0 || open.indexOf("type='module'") >= 0) {
                out.add(html.substring(openEnd + 1, close));
            }
            at = close + 8;
        }
    }

    /**
     * Resolves a module request relative to the importing file.
     * <p>
     * 가져오는 파일 기준으로 모듈 요청을 해석한다.
     *
     * @param importer importing file / 가져오는 파일
     * @param request static module request / 정적 모듈 요청
     * @return unresolved target file / 아직 정규화하지 않은 대상 파일
     * @throws IOException if canonicalization fails / 정규화에 실패하면
     */
    private File resolve(File importer, String request) throws IOException {
        int query = request.indexOf('?');
        int fragment = request.indexOf('#');
        int cut = request.length();
        if (query >= 0 && query < cut) {
            cut = query;
        }
        if (fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String path = request.substring(0, cut).replace('/', File.separatorChar);
        return new File(importer.getParentFile(), path).getCanonicalFile();
    }

    /**
     * Returns a path relative to the root when possible.
     * <p>
     * 가능하면 루트 기준 상대 경로를 반환한다.
     *
     * @param file file / 파일
     * @return display path / 표시 경로
     * @throws IOException if canonicalization fails / 정규화 실패
     */
    private String relative(File file) throws IOException {
        String path = file.getCanonicalPath();
        String prefix = root.getCanonicalPath() + File.separator;
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    /**
     * Reads one file as strict UTF-8.
     * <p>
     * 파일 하나를 엄격한 UTF-8로 읽는다.
     *
     * @param file file / 파일
     * @return decoded text / 디코딩한 텍스트
     * @throws IOException if reading fails / 읽기 실패
     */
    private static String readUtf8(File file) throws IOException {
        if (!file.isFile()) {
            throw new IOException("Missing file: " + file);
        }
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            // 파일 끝까지 바이트를 읽어 UTF-8 디코딩에 전달한다.
            while ((n = in.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Orders files by canonical path.
     * <p>
     * 파일을 정규 경로로 정렬한다.
     */
    private static final class FilePathOrder implements java.util.Comparator<File> {
        /**
         * Compares two files.
         * <p>
         * 두 파일을 비교한다.
         */
        public int compare(File left, File right) {
            try {
                return left.getCanonicalPath().compareTo(right.getCanonicalPath());
            } catch (IOException e) {
                return left.getPath().compareTo(right.getPath());
            }
        }
    }
}

