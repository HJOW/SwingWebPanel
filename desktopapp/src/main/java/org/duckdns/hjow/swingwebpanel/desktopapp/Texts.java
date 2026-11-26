package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.util.Locale;

/**
 * The language of the user interface texts: every text of the application is written in English and in Korean where it is used, and this
 * class picks one. Korean is used when the default locale of the JVM is Korean, English otherwise; the system property
 * <code>swingwebpanel.desktopapp.lang</code> (<code>ko</code> or <code>en</code>) overrides the locale.
 * <p>
 * 사용자 인터페이스 글의 언어: 응용 프로그램의 모든 글은 쓰는 곳에 영어와 한국어로 함께 적고, 이 클래스가 하나를 고른다. JVM 의 기본 로캘이
 * 한국어이면 한국어를, 아니면 영어를 쓴다. 시스템 속성 <code>swingwebpanel.desktopapp.lang</code>(<code>ko</code> 또는 <code>en</code>)이 로캘보다
 * 우선한다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * button.setToolTipText(Texts.of("Back", "뒤로"));
 * </pre>
 */
public final class Texts {

    /** The system property that selects the language. / 언어를 고르는 시스템 속성. */
    public static final String LANGUAGE_PROPERTY = "swingwebpanel.desktopapp.lang";

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private Texts() {
    }

    /**
     * Tells whether the user interface is shown in Korean.
     * <p>
     * 사용자 인터페이스를 한국어로 보이는지 알려 준다.
     *
     * @return true for Korean, false for English / 한국어이면 true, 영어이면 false
     */
    public static boolean isKorean() {
        String forced = System.getProperty(LANGUAGE_PROPERTY);
        // 속성으로 지정했으면 그 값을, 아니면 기본 로캘을 따른다.
        if (forced != null && forced.length() > 0) {
            return "ko".equalsIgnoreCase(forced);
        }
        return "ko".equals(Locale.getDefault().getLanguage());
    }

    /**
     * Picks the text for the language of the user interface.
     * <p>
     * 사용자 인터페이스의 언어에 맞는 글을 고른다.
     *
     * @param english the English text / 영어 글
     * @param korean  the Korean text / 한국어 글
     * @return one of the two / 둘 중 하나
     */
    public static String of(String english, String korean) {
        return isKorean() ? korean : english;
    }
}
