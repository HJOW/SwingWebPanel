package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import javax.net.ssl.HttpsURLConnection;

/**
 * The SSL extension point of the fetch layer. {@link HttpFetcher} calls {@link #configureSsl(HttpsURLConnection)} for
 * <b>every</b> https connection (main documents, redirects and sub-resources) before the request is sent. The public
 * component implements it with its <code>protected void configureSsl(HttpsURLConnection)</code> method so that hosts can
 * override the SSL handling by subclassing (custom trust, client certificates, protocol selection).
 * Implementations must only change the given connection; they must never alter JVM-wide SSL state
 * (<code>HttpsURLConnection.setDefault...</code>, <code>SSLContext.setDefault</code>, trust stores).
 * <p>
 * 가져오기 계층의 SSL 확장 지점. {@link HttpFetcher} 는 요청을 보내기 전에 <b>모든</b> https 연결(주 문서, 리디렉션, 하위
 * 리소스)에 대해 {@link #configureSsl(HttpsURLConnection)} 를 호출한다. 공개 컴포넌트는 자신의
 * <code>protected void configureSsl(HttpsURLConnection)</code> 메서드로 이 인터페이스를 구현하여 호스트가 하위 클래스에서
 * SSL 처리를 재정의(사용자 신뢰, 클라이언트 인증서, 프로토콜 선택)할 수 있게 한다. 구현은 전달된 연결만 바꿔야 하며
 * JVM 전역 SSL 상태(<code>HttpsURLConnection.setDefault...</code>, <code>SSLContext.setDefault</code>, 신뢰 저장소)는 절대
 * 바꾸면 안 된다.
 */
public interface SslConfigurer {

    /**
     * Configures one https connection before use. The default behaviour ({@link DefaultSslConfigurer}) keeps the JRE trust
     * store and host name verification and only enables the newest TLS protocol versions the running JRE supports.
     * <p>
     * https 연결 하나를 사용하기 전에 설정한다. 기본 동작({@link DefaultSslConfigurer})은 JRE 신뢰 저장소와 호스트 이름
     * 검증을 그대로 두고 실행 중인 JRE 가 지원하는 최신 TLS 프로토콜 버전만 켠다.
     *
     * @param connection the connection that is about to be used / 곧 사용할 연결
     * @throws IOException when the configuration fails; the request then fails with {@link FailureKind#TLS} / 설정이 실패하면
     *                     요청이 {@link FailureKind#TLS} 로 실패한다
     */
    void configureSsl(HttpsURLConnection connection) throws IOException;
}
