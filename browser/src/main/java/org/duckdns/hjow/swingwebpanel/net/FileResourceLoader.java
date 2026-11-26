package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Reads an ordinary local file with the component's cancellation and response limits.
 * <p>
 * 컴포넌트의 취소와 응답 제한을 적용하여 로컬의 일반 파일을 읽는다.
 */
final class FileResourceLoader {
    /** Limits on an in-memory response. / 메모리 응답의 한도. */
    private final FetchLimits limits;

    /**
     * Creates the local reader.
     * <p>
     * 로컬 읽기 객체를 만든다.
     *
     * @param limits response limits / 응답 한도
     */
    FileResourceLoader(FetchLimits limits) {
        this.limits = limits;
    }

    /**
     * Builds a local absolute path while rejecting decoded network and device path forms.
     * <p>
     * 디코딩한 네트워크·장치 경로를 거부하고 로컬 절대 경로를 만든다.
     *
     * @param url normalized file URL / 정규화한 파일 URL
     * @return local path or null / 로컬 경로 또는 null
     */
    private static File localFile(String url) {
        UrlUtil.Parts parts = UrlUtil.parse(url);
        String path = UrlUtil.percentDecode(parts.path);
        if (!path.startsWith("/") || path.startsWith("//")) {
            return null;
        }
        // 슬래시로 구분한 일반 경로만 허용하고 Windows 장치 경로와 대체 데이터 스트림 문자를 거부한다.
        boolean windows = File.separatorChar == '\\';
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < 0x20 || c == 0x7f || c == '\\' || windows && c == ':' && i != 2) {
                return null;
            }
        }
        if (windows && path.length() > 3 && path.charAt(2) == ':' && path.charAt(3) == '/') {
            path = path.substring(1);
        }
        File file = new File(path);
        return file.isAbsolute() ? file : null;
    }

    /**
     * Produces the headers and body of a local GET or HEAD request.
     * <p>
     * 로컬 GET 또는 HEAD 요청의 헤더와 본문을 만든다.
     *
     * @param req resource request / 리소스 요청
     * @param handler optional streaming consumer / 선택 스트리밍 소비자
     * @return local response / 로컬 응답
     */
    ResourceResponse load(ResourceRequest req, ResponseHandler handler) {
        String original = req.getUrl();
        String url = UrlUtil.normalize(original);
        if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
            return ResourceResponse.failure(original, FailureKind.BLOCKED_SCHEME, "file URLs support GET and HEAD");
        }
        FileInputStream input = null;
        OutputStream sink = null;
        CancelToken token = req.getCancelToken();
        try {
            token.throwIfCancelled();
            File file = localFile(url);
            if (file == null) {
                return ResourceResponse.failure(original, FailureKind.BLOCKED_SCHEME, "not a local file path");
            }
            if (file.isDirectory() || !file.isFile()) {
                return ResourceResponse.failure(original, FailureKind.NOT_FOUND, "local file not found");
            }
            input = new FileInputStream(file);
            long size = input.getChannel().size();
            String mime = MimeTypes.fromFileName(file.getName());
            ResourceResponse response = new ResourceResponse(original);
            response.setFinalUrl(url);
            response.setStatus(200, "OK");
            response.setContentType(mime, null);
            response.addHeader("Content-Type", mime);
            response.setDeclaredLength(size);
            response.addHeader("Content-Length", Long.toString(size));
            token.throwIfCancelled();
            sink = handler == null ? null : handler.onResponse(response);
            if ("HEAD".equals(req.getMethod())) {
                return response;
            }
            long memoryLimit = limits.getMaxMemoryBytes();
            if (sink == null && size > memoryLimit) {
                return ResourceResponse.failure(original, FailureKind.TOO_LARGE, "file exceeds the memory limit");
            }
            ByteArrayOutputStream memory = sink == null ? new ByteArrayOutputStream((int) Math.min(size, 8192)) : null;
            OutputStream output = sink == null ? memory : sink;
            byte[] buffer = new byte[8192];
            long count = 0;
            int read;
            ProgressListener progress = req.getProgressListener();
            // 파일을 청크로 전송하며 읽는 동안 커진 파일에도 메모리 한도를 적용한다.
            while ((read = input.read(buffer)) != -1) {
                token.throwIfCancelled();
                count += read;
                if (memory != null && count > memoryLimit) {
                    return ResourceResponse.failure(original, FailureKind.TOO_LARGE, "file exceeds the memory limit");
                }
                output.write(buffer, 0, read);
                if (progress != null) {
                    progress.onProgress(count, size);
                }
            }
            token.throwIfCancelled();
            if (sink != null) {
                sink.flush();
                response.setStreamed(count);
            } else {
                response.setBody(memory.toByteArray());
            }
            return response;
        } catch (IOException e) {
            return ResourceResponse.failure(original, token.isCancelled() ? FailureKind.CANCELLED : FailureKind.IO, e.getMessage());
        } catch (SecurityException e) {
            return ResourceResponse.failure(original, FailureKind.IO, "local file access was denied");
        } finally {
            close(input);
            close(sink);
        }
    }

    /**
     * Closes a stream, leaving the response to report the transfer result.
     * <p>
     * 스트림을 닫고 전송 결과는 응답에 맡긴다.
     *
     * @param stream the owned stream, if opened / 열었으면 소유하는 스트림
     */
    private static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // 읽기·쓰기 오류는 응답을 만들 때 처리한다.
            }
        }
    }
}
