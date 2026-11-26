package org.duckdns.hjow.swingwebpanel.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises live classpath Fetch bodies through the actual component without storing large test files.
 * <p>
 * 큰 시험 파일을 저장하지 않고 실제 컴포넌트의 클래스패스 Fetch 본문을 검증한다.
 */
public final class ClasspathFetchStreamTest {
    /** Size above the memory limit for consuming a full body. / 본문 일괄 소비의 메모리 한도를 넘는 크기. */
    private static final int SIZE = 17 * 1024 * 1024 + 3;

    /** Creates the suite. / 시험 묶음을 만든다. */
    public ClasspathFetchStreamTest() {
    }

    /**
     * Reads a large classpath body in chunks and checks the aggregate limit and cancellation.
     * <p>
     * 큰 클래스패스 본문을 청크로 읽고 일괄 소비 한도와 취소를 확인한다.
     *
     * @throws Exception if the real page cannot be exercised / 실제 페이지를 검증하지 못하면
     */
    public void testLargeClasspathBodyIsStreamed() throws Exception {
        final String html = "<!doctype html><script>(async()=>{"
                + "let r=await fetch('asset.bin'), reader=r.body.getReader(), size=0, ends=0;"
                + "for(;;){const a=await reader.read();if(a.done)break;size+=a.value.byteLength;ends=a.value[a.value.length-1];}"
                + "let bulk;try{await (await fetch('asset.bin')).arrayBuffer();}catch(e){bulk=e.name;}"
                + "const cancel=(await fetch('asset.bin')).body.getReader();let first=await cancel.read();"
                + "await cancel.cancel();opener.postMessage([size,ends,bulk,!first.done,(await cancel.read()).done].join('|'),'*');"
                + "})().catch(e=>opener.postMessage(e.name+':'+e.message,'*'));</script>";
        final byte[] page = html.getBytes("UTF-8");
        final Inbox inbox = new Inbox();
        final SwingWebPanel[] component = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component with a synthetic classpath stream. / 생성한 클래스패스 스트림으로 컴포넌트를 만든다. */
            public void run() {
                Thread edt = Thread.currentThread();
                ClassLoader saved = edt.getContextClassLoader();
                try {
                    edt.setContextClassLoader(new ClassLoader(saved) {
                        /** Produces the test page and a deterministic stream. / 시험 페이지와 결정적인 스트림을 만든다. */
                        public InputStream getResourceAsStream(String name) {
                            if ("live-fetch-fixture/index.html".equals(name)) {
                                return new ByteArrayInputStream(page);
                            }
                            if (!"live-fetch-fixture/asset.bin".equals(name)) {
                                return super.getResourceAsStream(name);
                            }
                            return new InputStream() {
                                /** Remaining fixture bytes in this request. / 이 요청의 남은 시험 바이트. */
                                private int left = SIZE;

                                /** Returns a single fixture byte. / 시험 바이트 하나를 반환한다. */
                                public int read() {
                                    if (left == 0) {
                                        return -1;
                                    }
                                    left--;
                                    return 42;
                                }

                                /** Supplies a chunk of fixture bytes. / 시험 바이트 청크를 제공한다. */
                                public int read(byte[] bytes, int offset, int length) {
                                    int count = Math.min(left, length);
                                    if (count == 0) {
                                        return length == 0 ? 0 : -1;
                                    }
                                    Arrays.fill(bytes, offset, offset + count, (byte) 42);
                                    left -= count;
                                    return count;
                                }
                            };
                        }
                    });
                    component[0] = new SwingWebPanel();
                    component[0].addWebMessageListener(inbox);
                } finally {
                    edt.setContextClassLoader(saved);
                }
            }
        });
        try {
            component[0].setPage("classpath://live-fetch-fixture/index.html");
            Check.equal(SIZE + "|42|RangeError|true|true", inbox.next().getData(), "the stream handles a body beyond the aggregate cap");
        } finally {
            component[0].dispose();
        }
    }
}
