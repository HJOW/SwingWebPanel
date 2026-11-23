package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A DOM event (DOM Standard section 2). It carries the event type, the flags that control propagation and cancellation, and, during
 * dispatch, the target, current target and phase. Subclasses add event specific data (mouse position, key, ...). An event can be
 * dispatched with {@link EventTarget#dispatchEvent(Event)}; after the dispatch, {@link #isDefaultPrevented()} tells the caller whether the
 * default action must be skipped.
 * <p>
 * DOM 이벤트(DOM 표준 2절). 이벤트 종류, 전파와 취소를 제어하는 플래그, 그리고 전달 중의 대상, 현재 대상, 단계를 가진다. 하위 클래스가 이벤트별
 * 데이터(마우스 위치, 키 등)를 더한다. 이벤트는 {@link EventTarget#dispatchEvent(Event)} 로 전달하며, 전달 후 {@link #isDefaultPrevented()} 가
 * 호출자에게 기본 동작을 건너뛰어야 하는지 알려 준다.
 */
public class Event {

    /**
     * Phase value: the event is not being dispatched.
     * <p>
     * 단계 값: 이벤트가 전달 중이 아님.
     */
    public static final short NONE = 0;

    /**
     * Phase value: capturing, from the root to the target's parent.
     * <p>
     * 단계 값: 캡처. 루트에서 대상의 부모까지.
     */
    public static final short CAPTURING_PHASE = 1;

    /**
     * Phase value: at the target.
     * <p>
     * 단계 값: 대상에서.
     */
    public static final short AT_TARGET = 2;

    /**
     * Phase value: bubbling, from the target's parent to the root.
     * <p>
     * 단계 값: 버블. 대상의 부모에서 루트까지.
     */
    public static final short BUBBLING_PHASE = 3;

    /**
     * Event type such as <code>click</code>.
     * <p>
     * <code>click</code> 같은 이벤트 종류.
     */
    private String type;

    /**
     * Whether the event bubbles.
     * <p>
     * 이벤트가 버블하는지 여부.
     */
    private boolean bubbles;

    /**
     * Whether the event can be cancelled.
     * <p>
     * 이벤트를 취소할 수 있는지 여부.
     */
    private boolean cancelable;

    /**
     * Whether the event crosses shadow boundaries (kept for completeness).
     * <p>
     * 이벤트가 섀도 경계를 넘는지 여부(완전성을 위해 보관).
     */
    private boolean composed;

    /**
     * True if the user agent created the event (as opposed to script).
     * <p>
     * 스크립트가 아니라 사용자 에이전트가 만든 이벤트이면 true.
     */
    private boolean trusted;

    /**
     * Creation time in milliseconds.
     * <p>
     * 생성 시각(밀리초).
     */
    private final long timeStamp = System.currentTimeMillis();

    /**
     * Original target.
     * <p>
     * 원래 대상.
     */
    EventTarget target;

    /**
     * Target whose listeners are being run.
     * <p>
     * 리스너를 실행 중인 대상.
     */
    EventTarget currentTarget;

    /**
     * Current phase.
     * <p>
     * 현재 단계.
     */
    short eventPhase = NONE;

    /**
     * The stop propagation flag.
     * <p>
     * 전파 중단 플래그.
     */
    boolean stopPropagation;

    /**
     * The stop immediate propagation flag.
     * <p>
     * 즉시 전파 중단 플래그.
     */
    boolean stopImmediate;

    /**
     * The canceled flag.
     * <p>
     * 취소됨 플래그.
     */
    boolean canceled;

    /**
     * True while a listener marked passive runs (preventDefault is then ignored).
     * <p>
     * passive 로 표시된 리스너가 실행 중이면 true(그때 preventDefault 는 무시된다).
     */
    boolean inPassiveListener;

    /**
     * True while the event is being dispatched.
     * <p>
     * 이벤트가 전달 중이면 true.
     */
    boolean dispatching;

    /**
     * True once the event was dispatched at least once and not re-initialized.
     * <p>
     * 한 번 이상 전달되어 다시 초기화되지 않았으면 true.
     */
    boolean dispatched;

    /**
     * The event path computed for the current dispatch (target first).
     * <p>
     * 현재 전달에 대해 계산한 이벤트 경로(대상이 첫 번째).
     */
    List<EventTarget> path = Collections.emptyList();

    /**
     * Creates an event of a type that does not bubble and cannot be cancelled.
     * <p>
     * 버블하지 않고 취소할 수 없는 종류의 이벤트를 만든다.
     *
     * @param type event type, not null / 이벤트 종류(null 불가)
     */
    public Event(String type) {
        this(type, false, false);
    }

    /**
     * Creates an event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type       event type, not null / 이벤트 종류(null 불가)
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public Event(String type, boolean bubbles, boolean cancelable) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = type;
        this.bubbles = bubbles;
        this.cancelable = cancelable;
    }

    /**
     * Re-initializes the event (the legacy <code>initEvent</code>); ignored while the event is dispatched.
     * <p>
     * 이벤트를 다시 초기화한다(옛 <code>initEvent</code>). 전달 중에는 무시한다.
     *
     * @param newType       event type / 이벤트 종류
     * @param newBubbles    whether it bubbles / 버블 여부
     * @param newCancelable whether it can be cancelled / 취소 가능 여부
     */
    public void initEvent(String newType, boolean newBubbles, boolean newCancelable) {
        if (dispatching) {
            return;
        }
        this.type = newType;
        this.bubbles = newBubbles;
        this.cancelable = newCancelable;
        this.stopPropagation = false;
        this.stopImmediate = false;
        this.canceled = false;
        this.dispatched = false;
        this.target = null;
    }

    /**
     * Returns the event type.
     * <p>
     * 이벤트 종류를 반환한다.
     *
     * @return type / 종류
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the target the event was dispatched to.
     * <p>
     * 이벤트를 전달한 대상을 반환한다.
     *
     * @return target or null before dispatch / 대상. 전달 전에는 null
     */
    public EventTarget getTarget() {
        return target;
    }

    /**
     * Returns the target whose listener is running now.
     * <p>
     * 지금 리스너가 실행 중인 대상을 반환한다.
     *
     * @return current target or null / 현재 대상 또는 null
     */
    public EventTarget getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Returns the current phase ({@link #NONE}, {@link #CAPTURING_PHASE}, {@link #AT_TARGET}, {@link #BUBBLING_PHASE}).
     * <p>
     * 현재 단계({@link #NONE}, {@link #CAPTURING_PHASE}, {@link #AT_TARGET}, {@link #BUBBLING_PHASE})를 반환한다.
     *
     * @return phase / 단계
     */
    public short getEventPhase() {
        return eventPhase;
    }

    /**
     * Tells whether the event bubbles.
     * <p>
     * 이벤트가 버블하는지 알려 준다.
     *
     * @return true if it bubbles / 버블하면 true
     */
    public boolean isBubbles() {
        return bubbles;
    }

    /**
     * Tells whether the event can be cancelled.
     * <p>
     * 이벤트를 취소할 수 있는지 알려 준다.
     *
     * @return true if cancelable / 취소 가능하면 true
     */
    public boolean isCancelable() {
        return cancelable;
    }

    /**
     * Tells whether the event is composed.
     * <p>
     * 이벤트가 composed 인지 알려 준다.
     *
     * @return true if composed / composed 이면 true
     */
    public boolean isComposed() {
        return composed;
    }

    /**
     * Sets the composed flag (before dispatch).
     * <p>
     * composed 플래그를 지정한다(전달 전).
     *
     * @param value new value / 새 값
     */
    public void setComposed(boolean value) {
        this.composed = value;
    }

    /**
     * Tells whether the event was created by the user agent.
     * <p>
     * 사용자 에이전트가 만든 이벤트인지 알려 준다.
     *
     * @return true if trusted / 신뢰된 이벤트이면 true
     */
    public boolean isTrusted() {
        return trusted;
    }

    /**
     * Marks the event as created by the user agent (used by the library for real user input and load events).
     * <p>
     * 사용자 에이전트가 만든 이벤트로 표시한다(라이브러리가 실제 사용자 입력과 load 이벤트에 쓴다).
     *
     * @param value new value / 새 값
     */
    public void setTrusted(boolean value) {
        this.trusted = value;
    }

    /**
     * Returns the creation time in milliseconds since the epoch.
     * <p>
     * 에포크 이후 밀리초 단위의 생성 시각을 반환한다.
     *
     * @return time stamp / 시각
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Stops the propagation to further targets; the remaining listeners of the current target still run.
     * <p>
     * 이후 대상으로의 전파를 멈춘다. 현재 대상의 나머지 리스너는 계속 실행된다.
     */
    public void stopPropagation() {
        stopPropagation = true;
    }

    /**
     * Stops the propagation and also skips the remaining listeners of the current target.
     * <p>
     * 전파를 멈추고 현재 대상의 나머지 리스너도 건너뛴다.
     */
    public void stopImmediatePropagation() {
        stopPropagation = true;
        stopImmediate = true;
    }

    /**
     * Cancels the event if it is cancelable and the running listener is not passive.
     * <p>
     * 이벤트가 취소 가능하고 실행 중인 리스너가 passive 가 아니면 취소한다.
     */
    public void preventDefault() {
        if (cancelable && !inPassiveListener) {
            canceled = true;
        }
    }

    /**
     * Tells whether the event was cancelled.
     * <p>
     * 이벤트가 취소되었는지 알려 준다.
     *
     * @return true if the default action must be skipped / 기본 동작을 건너뛰어야 하면 true
     */
    public boolean isDefaultPrevented() {
        return canceled;
    }

    /**
     * Returns the targets of the current dispatch, the target first and the outermost ancestor last; empty when not dispatching.
     * <p>
     * 현재 전달의 대상 목록을 반환한다. 대상이 맨 앞이고 가장 바깥 조상이 맨 뒤이며, 전달 중이 아니면 비어 있다.
     *
     * @return path copy / 경로 복사본
     */
    public List<EventTarget> composedPath() {
        return new ArrayList<EventTarget>(path);
    }
}
