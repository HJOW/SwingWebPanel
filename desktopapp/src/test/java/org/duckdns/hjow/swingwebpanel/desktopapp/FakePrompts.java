package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Answers the questions of the application without a dialog, for tests: the test sets the answers beforehand and reads afterwards what
 * was asked.
 * <p>
 * 시험을 위해 응용 프로그램의 질문에 대화 상자 없이 답한다: 시험이 미리 답을 정해 두고, 나중에 무엇을 물었는지 읽는다.
 */
public final class FakePrompts implements BrowserApp.Prompts {

    /** The answer to yes-or-no questions. / 예/아니요 질문의 답. */
    public boolean answer;

    /** The file that is chosen, or null to cancel. / 고르는 파일. 취소하면 null. */
    public File file;

    /** The text that is typed, or null to cancel. / 입력하는 글. 취소하면 null. */
    public String text;

    /** The number of yes-or-no questions that were asked. / 물어본 예/아니요 질문의 수. */
    public int asked;

    /** The name that was suggested for the last file question. / 마지막 파일 질문에서 제안된 이름. */
    public String suggested;

    /** The messages and questions in the order they were shown. / 보인 순서대로의 메시지와 질문. */
    public final List<String> messages = new ArrayList<String>();

    /**
     * Creates prompts that refuse and cancel everything.
     * <p>
     * 모든 것을 거절하고 취소하는 질문 수단을 만든다.
     */
    public FakePrompts() {
    }

    /**
     * Answers a yes-or-no question with the prepared answer.
     * <p>
     * 예/아니요 질문에 준비한 답으로 답한다.
     *
     * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
     * @param message the question / 질문
     * @return the prepared answer / 준비한 답
     */
    public boolean confirm(Component parent, String message) {
        asked++;
        messages.add(message);
        return answer;
    }

    /**
     * Chooses the prepared file.
     * <p>
     * 준비한 파일을 고른다.
     *
     * @param parent        the component that the question belongs to / 질문이 속한 컴포넌트
     * @param save          true to choose a file to write / 쓸 파일을 고르면 true
     * @param suggestedName the suggested file name or null / 제안된 파일 이름 또는 null
     * @return the prepared file / 준비한 파일
     */
    public File chooseFile(Component parent, boolean save, String suggestedName) {
        suggested = suggestedName;
        return file;
    }

    /**
     * Chooses the prepared file as the directory.
     * <p>
     * 준비한 파일을 디렉터리로 고른다.
     *
     * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
     * @param current the directory to start in or null / 시작할 디렉터리 또는 null
     * @return the prepared file / 준비한 파일
     */
    public File chooseDirectory(Component parent, File current) {
        return file;
    }

    /**
     * Remembers the message.
     * <p>
     * 메시지를 기억한다.
     *
     * @param parent  the component that the message belongs to / 메시지가 속한 컴포넌트
     * @param message the message / 메시지
     */
    public void inform(Component parent, String message) {
        messages.add(message);
    }

    /**
     * Types the prepared text.
     * <p>
     * 준비한 글을 입력한다.
     *
     * @param parent  the component that the question belongs to / 질문이 속한 컴포넌트
     * @param message the question / 질문
     * @param initial the text that is filled in or null / 미리 채운 글 또는 null
     * @return the prepared text / 준비한 글
     */
    public String ask(Component parent, String message, String initial) {
        messages.add(message);
        return text;
    }
}
