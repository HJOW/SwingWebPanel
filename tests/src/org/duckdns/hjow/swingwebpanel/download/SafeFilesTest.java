package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the file operations of the download manager: partial files and the replacement of an existing file, which on JRE 6 for
 * Windows cannot be a plain rename (plan item P11-09).
 * <p>
 * 다운로드 관리자의 파일 작업 시험: 부분 파일과 기존 파일의 교체. JRE 6 의 Windows 에서는 교체가 단순한 이름 바꾸기일 수 없다(계획 P11-09).
 */
public class SafeFilesTest {

    /** The directory of the current case. 현재 사례의 디렉터리. */
    private File dir;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SafeFilesTest() {
    }

    /**
     * Creates a private directory for each case.
     * <p>
     * 사례마다 전용 디렉터리를 만든다.
     *
     * @throws Exception if it cannot be created / 만들 수 없으면
     */
    public void setUp() throws Exception {
        dir = File.createTempFile("swp-safe-", ".d");
        dir.delete();
        dir.mkdirs();
    }

    /**
     * Deletes the directory of the case.
     * <p>
     * 사례의 디렉터리를 지운다.
     */
    public void tearDown() {
        File[] children = dir.listFiles();
        if (children != null) {
            // 디렉터리 안의 파일을 하나씩 지운다.
            for (int i = 0; i < children.length; i++) {
                children[i].delete();
            }
        }
        dir.delete();
    }

    /**
     * Writes a text file.
     * <p>
     * 텍스트 파일을 쓴다.
     *
     * @param f    the file / 파일
     * @param text the content / 내용
     * @throws IOException on failure / 실패 시
     */
    static void write(File f, String text) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Reads a text file.
     * <p>
     * 텍스트 파일을 읽는다.
     *
     * @param f the file / 파일
     * @return the content / 내용
     * @throws IOException on failure / 실패 시
     */
    static String read(File f) throws IOException {
        InputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Partial files are new, empty, uniquely named files next to the target; the directory is created if needed.
     * <p>
     * 부분 파일은 대상 옆의 새롭고 비어 있으며 이름이 고유한 파일이고 디렉터리가 없으면 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPartFiles() throws Exception {
        Set<String> names = new HashSet<String>();
        // 같은 이름으로 여러 번 만들어도 이름이 겹치지 않는다.
        for (int i = 0; i < 50; i++) {
            File f = SafeFiles.newPartFile(dir, "movie.mkv");
            Check.isTrue(f.exists() && f.length() == 0, "created empty");
            Check.isTrue(f.getName().startsWith("movie.mkv.part-"), "name pattern: " + f.getName());
            Check.isTrue(names.add(f.getName()), "unique: " + f.getName());
        }
        File sub = new File(dir, "a/b");
        File f = SafeFiles.newPartFile(sub, "x");
        Check.isTrue(sub.isDirectory() && f.exists(), "the directory was created");
        f.delete();
        new File(dir, "a/b").delete();
        new File(dir, "a").delete();
        File notADir = new File(dir, "plain");
        write(notADir, "x");
        try {
            SafeFiles.newPartFile(new File(notADir, "inside"), "x");
            Check.fail("a regular file cannot be a directory");
        } catch (IOException expected) {
            Check.isTrue(true, "failure is reported");
        }
    }

    /**
     * Moving to a free name and over an existing file works and leaves no helper file behind.
     * <p>
     * 비어 있는 이름으로 옮기기와 기존 파일 위로 옮기기가 동작하며 보조 파일을 남기지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMoveAndReplace() throws Exception {
        File src = new File(dir, "src.part");
        File dst = new File(dir, "dst.txt");
        write(src, "new");
        SafeFiles.moveReplacing(src, dst);
        Check.isFalse(src.exists(), "source is gone");
        Check.equal("new", read(dst), "moved to a free name");
        write(src, "newer");
        SafeFiles.moveReplacing(src, dst);
        Check.isFalse(src.exists(), "source is gone again");
        Check.equal("newer", read(dst), "replaced the existing file");
        String[] left = dir.list();
        Check.equal(1, left.length, "no helper files are left: " + java.util.Arrays.asList(left));
    }

    /**
     * When the replacement fails the existing file is still there and the source is not lost.
     * <p>
     * 교체가 실패해도 기존 파일은 남아 있고 원본도 잃지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailedReplaceKeepsBothFiles() throws Exception {
        File dst = new File(dir, "keep.txt");
        write(dst, "old");
        File missing = new File(dir, "missing.part");
        try {
            SafeFiles.moveReplacing(missing, dst);
            Check.fail("a missing source cannot be moved");
        } catch (IOException expected) {
            Check.isTrue(true, "failure is reported");
        }
        Check.equal("old", read(dst), "the existing file is restored");
        File src = new File(dir, "src.part");
        write(src, "x");
        File directory = new File(dir, "adir");
        directory.mkdir();
        try {
            SafeFiles.moveReplacing(src, directory);
            Check.fail("a directory cannot be replaced");
        } catch (IOException expected) {
            Check.isTrue(true, "failure is reported");
        }
        Check.equal("x", read(src), "the source is still there");
        directory.delete();
        File inNewDir = new File(dir, "new/deeper/file.txt");
        SafeFiles.moveReplacing(src, inNewDir);
        Check.equal("x", read(inNewDir), "missing directories are created");
        inNewDir.delete();
        inNewDir.getParentFile().delete();
        inNewDir.getParentFile().getParentFile().delete();
    }

    /**
     * Copying keeps the content (used when a rename between volumes is impossible).
     * <p>
     * 복사는 내용을 유지한다(볼륨 사이의 이름 바꾸기가 불가능할 때 쓴다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCopy() throws Exception {
        File a = new File(dir, "a");
        File b = new File(dir, "b");
        write(a, "content 한글");
        SafeFiles.copyFile(a, b);
        Check.equal("content 한글", read(b), "copied");
        Check.isTrue(a.exists(), "the source stays");
    }
}
