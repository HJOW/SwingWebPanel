package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Window;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * The Swing look and feel of the application (plan item P31-12): which ones the Java runtime offers, and switching to one of them while
 * the program runs. The look and feel belongs to the whole Java virtual machine, so a change reaches every window of the process. It
 * only changes the controls of the application; what a page shows is drawn by the browser component itself. Use it on the event dispatch
 * thread.
 * <p>
 * 응용 프로그램의 Swing 룩앤필(계획 P31-12): Java 런타임이 제공하는 것들, 그리고 프로그램이 실행 중일 때 그중 하나로 바꾸기. 룩앤필은 Java 가상 머신
 * 전체의 것이므로 변경은 프로세스의 모든 창에 미친다. 응용 프로그램의 컨트롤만 바뀌며, 페이지가 보이는 것은 브라우저 컴포넌트가 직접 그린다. 이벤트
 * 디스패치 스레드에서 쓴다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * LookAndFeels.apply(UIManager.getSystemLookAndFeelClassName());
 * LookAndFeels.apply("");      // back to the one that Java started with
 * </pre>
 */
public final class LookAndFeels {

    /** The class name of the look and feel that was in use before this class changed anything. / 이 클래스가 무엇을 바꾸기 전에 쓰던 룩앤필의 클래스 이름. */
    private static final String INITIAL = currentClassName();

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private LookAndFeels() {
    }

    /**
     * Returns the class name of the look and feel in use.
     * <p>
     * 지금 쓰는 룩앤필의 클래스 이름을 반환한다.
     *
     * @return the class name, or the empty string if there is none / 클래스 이름. 룩앤필이 없으면 빈 문자열
     */
    public static String currentClassName() {
        LookAndFeel current = UIManager.getLookAndFeel();
        return current == null ? "" : current.getClass().getName();
    }

    /**
     * Returns the class name of the look and feel that Java started the application with; the empty setting stands for it.
     * <p>
     * Java 가 응용 프로그램을 시작할 때 쓴 룩앤필의 클래스 이름을 반환한다. 빈 설정이 이것을 뜻한다.
     *
     * @return the class name / 클래스 이름
     */
    public static String initialClassName() {
        return INITIAL;
    }

    /**
     * Switches to a look and feel and redraws every window of the process with it. Asking for the one that is already in use does nothing.
     * A look and feel that cannot be used (its class is missing, it is not a look and feel, or the platform does not support it) leaves
     * the one in use.
     * <p>
     * 룩앤필을 바꾸고 프로세스의 모든 창을 그것으로 다시 그린다. 이미 쓰는 것을 요청하면 아무것도 하지 않는다. 쓸 수 없는 룩앤필(클래스가 없거나, 룩앤필이
     * 아니거나, 플랫폼이 지원하지 않음)이면 쓰던 것을 그대로 둔다.
     *
     * @param className the class name of the look and feel; null or blank for the one that Java started with / 룩앤필의 클래스 이름. null 이나
     *                  빈 글이면 Java 가 처음에 쓴 것
     * @return true if that look and feel is in use now / 지금 그 룩앤필을 쓰고 있으면 true
     */
    public static boolean apply(String className) {
        String wanted = className == null ? "" : className.trim();
        String target = wanted.length() == 0 ? INITIAL : wanted;
        if (target.equals(currentClassName())) {
            return true;
        }
        LookAndFeel before = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(target);
        } catch (Exception e) {
            // 클래스가 없거나 지원하지 않는 룩앤필이다. 바꾸다 만 상태가 남지 않게 되돌린다.
            restore(before);
            return false;
        } catch (LinkageError e) {
            // 클래스는 있지만 읽어 들일 수 없는 룩앤필이다.
            restore(before);
            return false;
        }
        Window[] windows = Window.getWindows();
        // 열려 있거나 숨겨 둔 모든 창의 컴포넌트에 새 룩앤필을 입힌다.
        for (int i = 0; i < windows.length; i++) {
            SwingUtilities.updateComponentTreeUI(windows[i]);
        }
        return true;
    }

    /**
     * Puts the previous look and feel back if a failed change replaced it.
     * <p>
     * 실패한 변경이 이전 룩앤필을 바꿔 놓았으면 되돌린다.
     *
     * @param before the look and feel before the change / 변경 전의 룩앤필
     */
    private static void restore(LookAndFeel before) {
        if (before == null || UIManager.getLookAndFeel() == before) {
            return;
        }
        try {
            UIManager.setLookAndFeel(before);
        } catch (javax.swing.UnsupportedLookAndFeelException e) {
            // 방금까지 쓰던 룩앤필이므로 일어나지 않는다. 일어나도 더 되돌릴 곳이 없다.
            return;
        }
    }
}
