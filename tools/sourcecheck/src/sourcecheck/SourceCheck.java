package sourcecheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Source tree checker for the SwingWebPanel project (plan items P02-07, P02-08, P02-09, P22-08). It verifies file encodings
 * (UTF-8 for java/md/sh, MS949 for bat), forbidden APIs and non-Java-6 syntax, native/binary artifacts, English+Korean Javadoc on
 * every declaration, and Korean comments on loops. Run <code>java -cp build/classes sourcecheck.SourceCheck [options] [paths]</code>;
 * the default paths are src, tests, examples and tools. Exit code: 0 = clean, 1 = violations, 2 = usage or I/O error.
 * <p>
 * SwingWebPanel 프로젝트의 소스 트리 검사기(계획 P02-07, P02-08, P02-09, P22-08). 파일 인코딩(java/md/sh 는 UTF-8, bat 는 MS949),
 * 금지 API 와 Java 6 이후 구문, 네이티브/바이너리 산출물, 모든 선언의 영어+한국어 Javadoc, 반복문의 한국어 주석을 검사한다.
 * <code>java -cp build/classes sourcecheck.SourceCheck [옵션] [경로]</code> 로 실행하며 기본 경로는 src, tests, examples, tools 이다.
 * 종료 코드: 0 = 위반 없음, 1 = 위반 있음, 2 = 사용법 또는 입출력 오류.
 */
public final class SourceCheck {

    /**
     * Exit code: no violation.
     * <p>
     * 종료 코드: 위반 없음.
     */
    public static final int EXIT_CLEAN = 0;

    /**
     * Exit code: violations found.
     * <p>
     * 종료 코드: 위반 발견.
     */
    public static final int EXIT_VIOLATIONS = 1;

    /**
     * Exit code: usage or I/O error.
     * <p>
     * 종료 코드: 사용법 또는 입출력 오류.
     */
    public static final int EXIT_ERROR = 2;

    /**
     * Directory names that are never scanned.
     * <p>
     * 검사하지 않는 디렉터리 이름.
     */
    private static final Set<String> SKIP_DIRS = new HashSet<String>(Arrays.asList(
            "build", "bin", ".git", ".settings", "node_modules", "target", "out"));

    /**
     * Extensions of native or binary artifacts that must not be committed.
     * <p>
     * 커밋하면 안 되는 네이티브/바이너리 산출물 확장자.
     */
    private static final Set<String> ARTIFACT_EXT = new HashSet<String>(Arrays.asList(
            "dll", "so", "dylib", "jnilib", "c", "cc", "cpp", "cxx", "h", "hpp", "hh", "m", "mm", "jar", "class", "exe", "node", "o", "a", "lib"));

    /**
     * Private constructor: use {@link #main(String[])} or {@link #run(String[], PrintStream, PrintStream)}.
     * <p>
     * 비공개 생성자: {@link #main(String[])} 또는 {@link #run(String[], PrintStream, PrintStream)} 을 사용한다.
     */
    private SourceCheck() {
    }

    /**
     * Program entry point.
     * <p>
     * 프로그램 진입점.
     *
     * @param args command line arguments / 명령행 인수
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Runs the checker.
     * <p>
     * 검사기를 실행한다.
     *
     * @param args command line arguments / 명령행 인수
     * @param out  report stream / 보고 스트림
     * @param err  error stream / 오류 스트림
     * @return exit code / 종료 코드
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        File root = new File(".");
        Set<String> only = new HashSet<String>(Arrays.asList("encoding", "forbidden", "javadoc", "loops"));
        int max = 200;
        boolean selftest = false;
        List<String> paths = new ArrayList<String>();
        // 명령행 인수를 해석한다.
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--root".equals(a) && i + 1 < args.length) {
                root = new File(args[++i]);
            } else if ("--only".equals(a) && i + 1 < args.length) {
                only = new HashSet<String>(Arrays.asList(args[++i].split(",")));
            } else if ("--max".equals(a) && i + 1 < args.length) {
                max = Integer.parseInt(args[++i]);
            } else if ("--selftest".equals(a)) {
                selftest = true;
            } else if (a.startsWith("--")) {
                err.println("sourcecheck: unknown option " + a);
                err.println("usage: sourcecheck.SourceCheck [--root DIR] [--only encoding,forbidden,javadoc,loops] [--max N] [--selftest] [PATH...]");
                return EXIT_ERROR;
            } else {
                paths.add(a);
            }
        }
        if (selftest) {
            return SelfTest.run(out, err);
        }
        if (paths.isEmpty()) {
            paths.addAll(Arrays.asList("src", "engine", "tests", "examples", "tools", "scripts", "docs", "AGENTS.md", "build.sh", "build.bat", "test.sh", "test.bat"));
        }
        List<File> files = new ArrayList<File>();
        // 지정한 경로(파일 또는 디렉터리)마다 검사 대상 파일을 모은다.
        for (int i = 0; i < paths.size(); i++) {
            File f = new File(root, paths.get(i));
            if (f.exists()) {
                collect(f, files);
            }
        }
        Collections.sort(files);

        List<Violation> violations = new ArrayList<Violation>();
        try {
            // 파일마다 확장자에 맞는 검사를 수행한다.
            for (int i = 0; i < files.size(); i++) {
                checkFile(root, files.get(i), only, violations);
            }
        } catch (IOException e) {
            err.println("sourcecheck: I/O error: " + e);
            return EXIT_ERROR;
        }
        // 위반을 출력하되 --max 로 출력 줄 수를 제한한다.
        for (int i = 0; i < violations.size() && i < max; i++) {
            out.println(violations.get(i));
        }
        if (violations.size() > max) {
            out.println("... " + (violations.size() - max) + " more");
        }
        out.println(violations.size() + " violation(s) in " + files.size() + " file(s) checked");
        return violations.isEmpty() ? EXIT_CLEAN : EXIT_VIOLATIONS;
    }

    /**
     * Collects files below a path, skipping build output directories.
     * <p>
     * 경로 아래의 파일을 모으며 빌드 출력 디렉터리는 건너뛴다.
     *
     * @param f      file or directory / 파일 또는 디렉터리
     * @param result list receiving files / 파일을 받을 목록
     */
    private static void collect(File f, List<File> result) {
        if (f.isDirectory()) {
            if (SKIP_DIRS.contains(f.getName())) {
                return;
            }
            File[] children = f.listFiles();
            if (children != null) {
                // 하위 항목을 재귀적으로 모은다.
                for (int i = 0; i < children.length; i++) {
                    collect(children[i], result);
                }
            }
        } else {
            result.add(f);
        }
    }

    /**
     * Runs the checks that apply to one file.
     * <p>
     * 파일 하나에 적용되는 검사를 수행한다.
     *
     * @param root base directory for display paths / 표시 경로의 기준 디렉터리
     * @param f    file / 파일
     * @param only enabled check groups / 켜진 검사 묶음
     * @param out  list receiving violations / 위반을 받을 목록
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없을 때
     */
    private static void checkFile(File root, File f, Set<String> only, List<Violation> out) throws IOException {
        String path = displayPath(root, f);
        String name = f.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        if (ARTIFACT_EXT.contains(ext) && only.contains("forbidden")) {
            out.add(new Violation(path, 0, "native-artifact", "native/binary/C source file is not allowed in the repository"));
            return;
        }
        // 시험 입력 자료(tests/resources, engine/tests/resources)는 의도적으로 다른 인코딩일 수 있어 내용 검사를 하지 않는다.
        if (path.startsWith("tests/resources/") || path.startsWith("engine/tests/resources/")) {
            return;
        }
        if (!"java".equals(ext) && !"md".equals(ext) && !"sh".equals(ext) && !"bat".equals(ext)) {
            return;
        }
        byte[] data = readAll(f);
        if ("bat".equals(ext)) {
            if (only.contains("encoding")) {
                EncodingChecks.checkBat(path, data, out);
            }
            return;
        }
        String text = null;
        if (only.contains("encoding") || "java".equals(ext)) {
            List<Violation> enc = new ArrayList<Violation>();
            text = EncodingChecks.checkUtf8Text(path, data, ext, enc);
            if (only.contains("encoding")) {
                out.addAll(enc);
            }
        }
        if ("java".equals(ext) && text != null) {
            JavaScanner sc = new JavaScanner(text);
            if (only.contains("forbidden")) {
                ForbiddenChecks.check(sc, path, out);
            }
            DocChecks doc = new DocChecks(sc, path, out);
            if (only.contains("javadoc")) {
                doc.checkJavadoc();
            }
            if (only.contains("loops")) {
                doc.checkLoops();
            }
        }
    }

    /**
     * Builds the display path (relative to the root, forward slashes).
     * <p>
     * 표시 경로(기준 디렉터리 상대, 슬래시 구분)를 만든다.
     *
     * @param root base directory / 기준 디렉터리
     * @param f    file / 파일
     * @return display path / 표시 경로
     */
    static String displayPath(File root, File f) {
        String rp = root.getAbsoluteFile().toURI().normalize().getPath();
        String fp = f.getAbsoluteFile().toURI().normalize().getPath();
        if (fp.startsWith(rp)) {
            fp = fp.substring(rp.length());
        }
        return fp;
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f file / 파일
     * @return bytes / 바이트
     * @throws IOException on read failure / 읽기 실패 시
     */
    static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Writes bytes to a file (used by the self test).
     * <p>
     * 파일에 바이트를 쓴다(자체 시험에서 사용).
     *
     * @param f    file / 파일
     * @param data bytes / 바이트
     * @throws IOException on write failure / 쓰기 실패 시
     */
    static void writeAll(File f, byte[] data) throws IOException {
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        OutputStream os = new FileOutputStream(f);
        try {
            os.write(data);
        } finally {
            os.close();
        }
    }
}
