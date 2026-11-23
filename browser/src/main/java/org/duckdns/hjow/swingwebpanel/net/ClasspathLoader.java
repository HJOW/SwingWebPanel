package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads <code>classpath://path</code> resources through <code>ClassLoader.getResourceAsStream</code>. The class path is the only
 * source: the address is never turned into a <code>file:</code> or <code>jar:</code> URL that is then opened, so a
 * class path address cannot be used to reach arbitrary files, and resources inside JAR files work exactly like files in
 * directories.
 * <ul>
 * <li><b>Path rules</b>: the text after <code>classpath://</code> is the resource path relative to the class path root. It is
 * percent-decoded exactly once (spaces as <code>%20</code>, UTF-8 file names), "." segments are skipped, ".." removes the previous
 * segment and a ".." that would climb above the root makes the resource <em>not found</em>; backslashes, NUL and other control
 * characters are refused. A path that is empty or ends with "/" is a directory and never loaded.</li>
 * <li><b>Result</b>: status 200 with the media type guessed from the file name ({@link MimeTypes}), or status 404 with
 * {@link FailureKind#NOT_FOUND} when the resource does not exist, is a directory or has an illegal path.</li>
 * </ul>
 * <p>
 * <code>classpath://경로</code> 리소스를 <code>ClassLoader.getResourceAsStream</code> 으로 읽는다. 클래스패스만이 출처이다. 주소를
 * <code>file:</code> 이나 <code>jar:</code> URL 로 바꾸어 여는 일은 없으므로 클래스패스 주소로 임의 파일에 닿을 수 없고,
 * JAR 안의 리소스도 디렉터리의 파일과 똑같이 읽힌다.
 * <ul>
 * <li><b>경로 규칙</b>: <code>classpath://</code> 뒤의 문자열이 클래스패스 루트 기준 리소스 경로이다. 퍼센트 인코딩은 정확히 한 번
 * 디코딩하고(공백은 <code>%20</code>, UTF-8 파일 이름), "." 세그먼트는 건너뛰며, ".." 는 앞 세그먼트를 지우고, 루트 위로
 * 올라가려는 ".." 가 있으면 리소스를 <em>찾을 수 없음</em>으로 한다. 역슬래시, NUL, 기타 제어 문자는 거부한다. 경로가 비었거나
 * "/" 로 끝나면 디렉터리이므로 읽지 않는다.</li>
 * <li><b>결과</b>: 파일 이름으로 추정한 미디어 타입({@link MimeTypes})과 상태 200, 또는 리소스가 없거나 디렉터리이거나 경로가
 * 잘못되었을 때 상태 404 와 {@link FailureKind#NOT_FOUND}.</li>
 * </ul>
 */
public final class ClasspathLoader {

    /**
     * Class loader used to find resources.
     * <p>
     * 리소스를 찾는 클래스 로더.
     */
    private final ClassLoader loader;
    /**
     * Size limits.
     * <p>
     * 크기 제한.
     */
    private final FetchLimits limits;

    /**
     * Creates a loader.
     * <p>
     * 로더를 만든다.
     *
     * @param loader class loader, null for the context class loader (then this class's loader) / 클래스 로더. null 이면 컨텍스트 클래스 로더(없으면 이 클래스의 로더)
     * @param limits limits, null for the defaults / 제한. null 이면 기본값
     */
    public ClasspathLoader(ClassLoader loader, FetchLimits limits) {
        ClassLoader l = loader;
        if (l == null) {
            l = Thread.currentThread().getContextClassLoader();
        }
        if (l == null) {
            l = ClasspathLoader.class.getClassLoader();
        }
        this.loader = l;
        this.limits = limits == null ? FetchLimits.DEFAULT : limits;
    }

    /**
     * Converts the path of a classpath URL to a clean resource name, or null when it is illegal or escapes the root.
     * <p>
     * 클래스패스 URL 의 경로를 깨끗한 리소스 이름으로 바꾼다. 잘못되었거나 루트를 벗어나면 null.
     *
     * @param url classpath URL / 클래스패스 URL
     * @return resource name such as <code>a/b.html</code>, or null / <code>a/b.html</code> 같은 리소스 이름, 또는 null
     */
    public static String resourceName(String url) {
        UrlUtil.Parts p;
        try {
            p = UrlUtil.parse(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!UrlUtil.SCHEME_CLASSPATH.equals(p.scheme)) {
            return null;
        }
        String decoded = UrlUtil.percentDecode(p.path.startsWith("/") ? p.path.substring(1) : p.path);
        // 역슬래시와 제어 문자(NUL 포함)는 경로로 쓸 수 없다.
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (c == '\\' || c < 0x20 || c == 0x7F) {
                return null;
            }
        }
        if (decoded.length() == 0 || decoded.endsWith("/")) {
            return null;
        }
        List<String> parts = new ArrayList<String>();
        String[] segs = decoded.split("/");
        // 세그먼트를 훑으며 '.' 은 건너뛰고 '..' 은 앞 세그먼트를 지우되 루트를 벗어나면 실패로 본다.
        for (int i = 0; i < segs.length; i++) {
            if (segs[i].length() == 0 || ".".equals(segs[i])) {
                continue;
            }
            if ("..".equals(segs[i])) {
                if (parts.isEmpty()) {
                    return null;
                }
                parts.remove(parts.size() - 1);
            } else {
                parts.add(segs[i]);
            }
        }
        // 첫 세그먼트의 ':' 는 file:/jar: 같은 스킴이나 드라이브 문자로 보이므로 URL 해석에 의한 우회를 막기 위해 거부한다.
        if (parts.isEmpty() || parts.get(0).indexOf(':') >= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 남은 세그먼트를 슬래시로 이어 리소스 이름을 만든다.
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /**
     * Loads a classpath resource. Never throws for a missing resource.
     * <p>
     * 클래스패스 리소스를 읽는다. 리소스가 없어도 예외를 던지지 않는다.
     *
     * @param req     the request / 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @return the response / 응답
     */
    public ResourceResponse load(ResourceRequest req, ResponseHandler handler) {
        ResourceResponse res = new ResourceResponse(req.getUrl());
        if (SchemePolicy.classify(req.getUrl()) != SchemePolicy.Kind.CLASSPATH) {
            res.setFailure(FailureKind.BLOCKED_SCHEME, "not a classpath url: " + req.getUrl());
            return res;
        }
        String url = UrlUtil.normalize(req.getUrl());
        res.setFinalUrl(url);
        String name = resourceName(url);
        if (name == null || isDirectory(name)) {
            return notFound(res, "classpath resource not found: " + req.getUrl());
        }
        if (req.getCancelToken().isCancelled()) {
            res.setFailure(FailureKind.CANCELLED, "request cancelled");
            return res;
        }
        InputStream in = loader.getResourceAsStream(name);
        if (in == null) {
            return notFound(res, "classpath resource not found: " + name);
        }
        String mime = MimeTypes.fromFileName(name);
        res.setStatus(200, "OK");
        res.addHeader("Content-Type", mime);
        res.setContentType(mime, null);
        OutputStream sink = null;
        try {
            sink = handler == null ? null : handler.onResponse(res);
            ByteArrayOutputStream mem = sink == null ? new ByteArrayOutputStream() : null;
            byte[] buf = new byte[8192];
            long count = 0;
            int n;
            ProgressListener pl = req.getProgressListener();
            // 리소스를 끝까지 읽으며 취소와 메모리 제한을 확인한다.
            while ((n = in.read(buf)) >= 0) {
                req.getCancelToken().throwIfCancelled();
                count += n;
                if (mem != null) {
                    if (count > limits.getMaxMemoryBytes()) {
                        res.setStatus(0, "");
                        res.setFailure(FailureKind.TOO_LARGE, "resource exceeds " + limits.getMaxMemoryBytes() + " bytes");
                        return res;
                    }
                    mem.write(buf, 0, n);
                } else {
                    sink.write(buf, 0, n);
                }
                if (pl != null) {
                    pl.onProgress(count, -1);
                }
            }
            res.setDeclaredLength(count);
            if (sink != null) {
                sink.flush();
                res.setStreamed(count);
            } else {
                res.setBody(mem.toByteArray());
            }
            if (pl != null) {
                pl.onProgress(count, count);
            }
            return res;
        } catch (CancelledException e) {
            res.setFailure(FailureKind.CANCELLED, "request cancelled");
            return res;
        } catch (IOException e) {
            res.setStatus(0, "");
            res.setFailure(FailureKind.IO, String.valueOf(e));
            return res;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // 닫기 실패는 무시한다.
            } catch (RuntimeException e) {
                // 일부 JRE 의 JAR 스트림은 닫을 때 NPE 를 내므로 무시한다.
            }
            if (sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    // 닫기 실패는 무시한다.
                }
            }
        }
    }

    /**
     * Records a "not found" result (status 404).
     * <p>
     * "찾을 수 없음" 결과를 기록한다(상태 404).
     *
     * @param res response / 응답
     * @param msg message / 메시지
     * @return the response / 응답
     */
    private static ResourceResponse notFound(ResourceResponse res, String msg) {
        res.setStatus(404, "Not Found");
        res.setFailure(FailureKind.NOT_FOUND, msg);
        return res;
    }

    /**
     * Tells whether the name denotes a directory of the class path (directories must never be listed or loaded).
     * <p>
     * 이름이 클래스패스의 디렉터리를 가리키는지 알려 준다(디렉터리는 목록을 보이거나 읽으면 안 된다).
     *
     * @param name resource name / 리소스 이름
     * @return true for a directory / 디렉터리이면 true
     */
    private boolean isDirectory(String name) {
        URL u = loader.getResource(name);
        if (u == null) {
            return false;
        }
        if ("file".equals(u.getProtocol())) {
            try {
                return new File(u.toURI()).isDirectory();
            } catch (URISyntaxException e) {
                return new File(u.getPath()).isDirectory();
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        // JAR 안에서는 디렉터리 항목 "이름/" 이 따로 있으면 그 이름은 디렉터리이다(일반 파일은 "이름/" 항목이 없다).
        if ("jar".equals(u.getProtocol())) {
            return loader.getResource(name + "/") != null;
        }
        return u.getPath().endsWith("/");
    }
}
