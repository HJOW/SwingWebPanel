package org.duckdns.hjow.swingwebpanel.render;

/**
 * The display command of a form control: it carries the snapshot of the control and draws it with {@link ControlPainter} when it is
 * painted. The background, the border and the outline of the control are separate commands.
 * <p>
 * 폼 컨트롤의 디스플레이 명령: 컨트롤의 스냅샷을 가지고 있다가 그려질 때 {@link ControlPainter} 로 그린다. 컨트롤의 배경, 경계선, 아웃라인은 별도 명령이다.
 */
final class ControlCommand extends DisplayCommand {

    /** The snapshot of the control. 컨트롤의 스냅샷. */
    private final ControlSnapshot snapshot;

    /**
     * Creates the command for a control.
     * <p>
     * 컨트롤의 명령을 만든다.
     *
     * @param snapshot the snapshot of the control / 컨트롤의 스냅샷
     */
    ControlCommand(ControlSnapshot snapshot) {
        super(Type.CONTROL, snapshot.x, snapshot.y, snapshot.w, snapshot.h, snapshot.element);
        this.snapshot = snapshot;
    }

    /**
     * Returns the snapshot of the control.
     * <p>
     * 컨트롤의 스냅샷을 반환한다.
     *
     * @return the snapshot / 스냅샷
     */
    ControlSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Draws the control.
     * <p>
     * 컨트롤을 그린다.
     *
     * @param ctx the paint context / 그리기 컨텍스트
     */
    void paint(PaintContext ctx) {
        ControlPainter.paint(ctx.g, snapshot);
    }
}
