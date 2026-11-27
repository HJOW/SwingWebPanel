package org.duckdns.hjow.swingwebpanel.net;

import java.lang.reflect.Method;
import javax.net.ssl.SSLSocketFactory;

/**
 * Resolves the optional TLS socket factory through the caller's class path.
 * / 호출자의 클래스 경로에서 선택 TLS 소켓 팩터리를 찾는다.
 */
public final class OptionalTlsModule {

    /** Module socket factory type. / 모듈 소켓 팩터리 타입. */
    private static final String FACTORY = "org.duckdns.hjow.swingwebpanel.tls13.Tls13SocketFactory";

    /** Module failure classifier type. / 모듈 실패 판정 타입. */
    private static final String FAILURES = "org.duckdns.hjow.swingwebpanel.tls13.Tls13Failures";

    /** Utility class. / 인스턴스를 만들지 않는다. */
    private OptionalTlsModule() {
    }

    /**
     * Tests whether the factory class and its entry point can be loaded. / 팩터리 클래스와 진입점을 불러올 수 있는지 확인한다.
     *
     * @return availability without initializing the factory / 팩터리를 초기화하지 않고 확인한 가용 여부
     */
    public static boolean isAvailable() {
        return find(false) != null;
    }

    /**
     * Gets the initialized socket factory if possible. / 가능하면 초기화된 소켓 팩터리를 얻는다.
     *
     * @return factory or null in an unavailable or unusable installation / 팩터리. 모듈이 없거나 쓸 수 없으면 null
     */
    public static SSLSocketFactory load() {
        return (SSLSocketFactory) find(true);
    }

    /**
     * Probes or initializes the factory with the context loader, then the library loader.
     * / 컨텍스트 로더와 라이브러리 로더 순서로 팩터리를 확인하거나 초기화한다.
     *
     * @param initialize whether to get the factory instance / 팩터리 인스턴스를 얻을지 여부
     * @return the entry point or factory, or null / 진입점 또는 팩터리. 찾을 수 없으면 null
     */
    private static Object find(boolean initialize) {
        ClassLoader library;
        try {
            library = OptionalTlsModule.class.getClassLoader();
        } catch (SecurityException e) {
            library = null;
        }
        ClassLoader context;
        try {
            context = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            context = null;
        }
        // 컨텍스트 로더가 제공하는 모듈을 먼저 쓰고 실패하면 라이브러리 쪽을 확인한다.
        if (context != null && context != library) {
            Object result = find(context, initialize);
            if (result != null) {
                return result;
            }
        }
        return find(library, initialize);
    }

    /**
     * Looks up the public JSSE entry point in one loader. / 한 로더에서 공개 JSSE 진입점을 찾는다.
     *
     * @param loader class loader / 클래스 로더
     * @param initialize whether to invoke the factory accessor / 팩터리 접근자를 부를지 여부
     * @return entry point or initialized factory, or null on failure / 진입점 또는 초기화된 팩터리. 실패 시 null
     */
    private static Object find(ClassLoader loader, boolean initialize) {
        try {
            Class<?> type = Class.forName(FACTORY, false, loader).asSubclass(SSLSocketFactory.class);
            Method accessor = type.getMethod("getInstance");
            return initialize ? SSLSocketFactory.class.cast(accessor.invoke(null)) : accessor;
        } catch (Exception e) {
            return null;
        } catch (LinkageError e) {
            return null;
        }
    }

    /**
     * Asks the factory's own module whether the connection failure allows a retry.
     * / 연결 실패를 재시도해도 되는지 팩터리를 제공한 모듈에 묻는다.
     *
     * @param factory the actual factory configured on the failing connection / 실패한 연결에 실제 설치한 팩터리
     * @param failure connection failure / 연결 실패
     * @return true only for a recognized negotiation failure / 협상 실패로 판정된 경우에만 true
     */
    static boolean isNegotiationFailure(SSLSocketFactory factory, Throwable failure) {
        if (factory == null || !FACTORY.equals(factory.getClass().getName())) {
            return false;
        }
        try {
            Class<?> type = Class.forName(FAILURES, true, factory.getClass().getClassLoader());
            return Boolean.TRUE.equals(type.getMethod("isNegotiationFailure", Throwable.class).invoke(null, failure));
        } catch (Exception e) {
            return false;
        } catch (LinkageError e) {
            return false;
        }
    }
}
