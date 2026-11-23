package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The drawing surface of one <code>canvas</code> element: its bitmap, its 2D context (created by the first <code>getContext("2d")</code>), the
 * origin-clean flag and the change tracking for the renderer. The script thread draws through the context; the renderer reads an immutable
 * snapshot with {@link #snapshot()}, a copy of the pixels that is made only when something changed since the last snapshot (so an unchanged
 * canvas costs nothing per frame and a changing one costs one copy per rendered frame). One change notification is sent per snapshot, however many
 * drawing calls happened in between. All methods are synchronized on the surface, and the script bindings hold the same monitor while a context
 * method runs.
 * <p>
 * <code>canvas</code> 요소 하나의 그리기 표면: 비트맵, 2D 컨텍스트(첫 <code>getContext("2d")</code> 가 만든다), origin-clean 표시, 렌더러를 위한 변경 추적.
 * 스크립트 스레드는 컨텍스트로 그리고, 렌더러는 {@link #snapshot()} 으로 불변 스냅샷을 읽는다. 스냅샷은 마지막 스냅샷 이후 무언가 바뀌었을 때만 화소를
 * 복사해 만들므로(바뀌지 않은 캔버스는 프레임마다 비용이 없고 바뀌는 캔버스는 그려진 프레임마다 복사 한 번이 든다) 그 사이에 그리기 호출이 몇 번이든
 * 변경 알림은 스냅샷마다 한 번만 보낸다. 모든 메서드는 표면에서 동기화하며, 스크립트 연결은 컨텍스트 메서드가 도는 동안 같은 모니터를 잡는다.
 */
public final class CanvasSurface {

    /**
     * The receiver of change notifications.
     * <p>
     * 변경 알림을 받는 쪽.
     */
    public interface Listener {
        /**
         * Called (possibly from the script thread, while the surface is locked, so it must only post work) when the picture changed after
         * the last snapshot.
         * <p>
         * 마지막 스냅샷 뒤 그림이 바뀌었을 때 불린다(스크립트 스레드에서 표면이 잠긴 채 불릴 수 있으므로 작업을 올리기만 해야 한다).
         */
        void surfaceChanged();
    }

    /**
     * The width of the bitmap that the canvas attributes ask for.
     * <p>
     * 캔버스 속성이 요구하는 비트맵 너비.
     */
    private int width;

    /**
     * The height of the bitmap that the canvas attributes ask for.
     * <p>
     * 캔버스 속성이 요구하는 비트맵 높이.
     */
    private int height;

    /**
     * The bitmap, never null (possibly empty).
     * <p>
     * 비트맵. null 이 아니다(비어 있을 수 있음).
     */
    private CanvasBitmap bitmap;

    /**
     * The 2D context, null until the script asked for it.
     * <p>
     * 2D 컨텍스트. 스크립트가 요청하기 전에는 null.
     */
    private Context2D context;

    /**
     * False once a pixel that scripts must not read was drawn.
     * <p>
     * 스크립트가 읽으면 안 되는 화소가 그려진 뒤에는 false.
     */
    private boolean originClean = true;

    /**
     * Incremented whenever the picture or the size changes.
     * <p>
     * 그림이나 크기가 바뀔 때마다 증가한다.
     */
    private long version;

    /**
     * The version that {@link #cached} was made from.
     * <p>
     * {@link #cached} 를 만든 버전.
     */
    private long cachedVersion = -1;

    /**
     * The last snapshot.
     * <p>
     * 마지막 스냅샷.
     */
    private DecodedImage cached;

    /**
     * True after a notification was sent and until the next snapshot.
     * <p>
     * 알림을 보낸 뒤 다음 스냅샷까지 true.
     */
    private boolean notified;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private boolean disposed;

    /**
     * The receiver of change notifications, or null.
     * <p>
     * 변경 알림을 받는 쪽. 없으면 null.
     */
    private Listener listener;

    /**
     * The owner (weakly held), or null.
     * <p>
     * 소유자(약하게 잡음). 없으면 null.
     */
    private java.lang.ref.WeakReference<Object> owner;

    /**
     * The owner that this surface keeps alive (an <code>OffscreenCanvas</code>: the context of such a canvas must be able to name it as long as
     * the context lives), or null.
     * <p>
     * 이 표면이 살려 두는 소유자(<code>OffscreenCanvas</code>: 그런 캔버스의 컨텍스트는 자신이 살아 있는 동안 캔버스를 알려 줄 수 있어야 한다). 없으면 null.
     */
    private Object strongOwner;

    /**
     * Creates a surface with a bitmap of a size.
     * <p>
     * 크기가 정해진 비트맵을 가진 표면을 만든다.
     *
     * @param width    width in pixels / 너비(화소)
     * @param height   height in pixels / 높이(화소)
     * @param listener the receiver of change notifications, or null / 변경 알림을 받는 쪽. 없으면 null
     */
    public CanvasSurface(int width, int height, Listener listener) {
        this.width = width;
        this.height = height;
        this.listener = listener;
        this.bitmap = new CanvasBitmap(width, height);
    }

    /**
     * Remembers the object that owns the surface (the <code>canvas</code> element) without keeping it alive.
     * <p>
     * 표면을 소유한 객체(<code>canvas</code> 요소)를 살려 두지 않고 기억한다.
     *
     * @param o the owner / 소유자
     */
    public synchronized void setOwner(Object o) {
        owner = new java.lang.ref.WeakReference<Object>(o);
    }

    /**
     * Returns the owner, or null if it was collected.
     * <p>
     * 소유자를 반환한다. 수거되었으면 null.
     *
     * @return the owner or null / 소유자 또는 null
     */
    public synchronized Object getOwner() {
        if (strongOwner != null) {
            return strongOwner;
        }
        return owner == null ? null : owner.get();
    }

    /**
     * Remembers the object that owns the surface and keeps it alive (for an <code>OffscreenCanvas</code>, which has no element that something
     * else holds).
     * <p>
     * 표면을 소유한 객체를 기억하고 살려 둔다(다른 것이 붙잡고 있는 요소가 없는 <code>OffscreenCanvas</code> 용).
     *
     * @param o the owner / 소유자
     */
    synchronized void setStrongOwner(Object o) {
        strongOwner = o;
    }

    /**
     * Replaces the picture by a blank one of the same size (transparent black, or opaque black for an opaque canvas) without touching the state
     * of the context; <code>OffscreenCanvas.transferToImageBitmap()</code> does it after it took the picture.
     * <p>
     * 컨텍스트의 상태를 건드리지 않고 그림을 같은 크기의 빈 것(투명한 검정. 불투명 캔버스는 불투명한 검정)으로 바꾼다. <code>OffscreenCanvas.transferToImageBitmap()</code>
     * 이 그림을 가져간 뒤 이렇게 한다.
     */
    synchronized void blank() {
        if (disposed || bitmap.isEmpty()) {
            return;
        }
        bitmap.clear();
        // 불투명한 캔버스는 불투명한 검정으로 다시 시작한다.
        if (opaque) {
            bitmap.fillOpaqueBlack();
        }
        markChanged();
    }

    /**
     * Returns the width that the canvas asks for.
     * <p>
     * 캔버스가 요구하는 너비를 반환한다.
     *
     * @return width in pixels / 너비(화소)
     */
    public synchronized int getWidth() {
        return width;
    }

    /**
     * Returns the height that the canvas asks for.
     * <p>
     * 캔버스가 요구하는 높이를 반환한다.
     *
     * @return height in pixels / 높이(화소)
     */
    public synchronized int getHeight() {
        return height;
    }

    /**
     * Returns the bitmap.
     * <p>
     * 비트맵을 반환한다.
     *
     * @return the bitmap / 비트맵
     */
    synchronized CanvasBitmap bitmap() {
        return bitmap;
    }

    /**
     * Returns the 2D context, creating it at the first call.
     * <p>
     * 2D 컨텍스트를 반환한다. 처음 부를 때 만든다.
     *
     * @return the context / 컨텍스트
     */
    public synchronized Context2D getContext() {
        if (context == null) {
            context = new Context2D(this);
        }
        return context;
    }

    /**
     * Tells whether the 2D context exists.
     * <p>
     * 2D 컨텍스트가 있는지 알려 준다.
     *
     * @return true after the first {@link #getContext()} / 첫 {@link #getContext()} 뒤에는 true
     */
    public synchronized boolean hasContext() {
        return context != null;
    }

    /**
     * Sets the bitmap size (the canvas attributes changed, or were set to the same value): the bitmap becomes transparent black at the new
     * size, the context state is reset to its defaults, and the canvas is origin-clean again.
     * <p>
     * 비트맵 크기를 정한다(캔버스 속성이 바뀌었거나 같은 값으로 지정됨): 비트맵은 새 크기의 투명한 검정이 되고, 컨텍스트 상태는 기본값으로 돌아가며,
     * 캔버스는 다시 origin-clean 이 된다.
     *
     * @param w new width in pixels / 새 너비(화소)
     * @param h new height in pixels / 새 높이(화소)
     */
    public synchronized void resize(int w, int h) {
        if (disposed) {
            return;
        }
        bitmap.dispose();
        width = w;
        height = h;
        bitmap = new CanvasBitmap(w, h);
        originClean = true;
        // 불투명 캔버스는 새 비트맵도 불투명한 검정으로 시작한다.
        if (opaque) {
            bitmap.fillOpaqueBlack();
        }
        if (context != null) {
            context.resetState();
        }
        markChanged();
    }

    /**
     * Tells whether scripts may read the pixels back (<code>getImageData</code>, <code>toDataURL</code>, <code>toBlob</code>).
     * <p>
     * 스크립트가 화소를 다시 읽어도 되는지(<code>getImageData</code>, <code>toDataURL</code>, <code>toBlob</code>) 알려 준다.
     *
     * @return true while the canvas is origin-clean / 캔버스가 origin-clean 인 동안 true
     */
    public synchronized boolean isOriginClean() {
        return originClean;
    }

    /**
     * Marks the canvas as tainted: it drew pixels of another origin, so reading back is no longer allowed. The flag is reset by
     * {@link #resize(int, int)} only.
     * <p>
     * 캔버스를 오염된 것으로 표시한다: 다른 오리진의 화소를 그렸으므로 더 이상 읽어 올 수 없다. 표시는 {@link #resize(int, int)} 만 지운다.
     */
    synchronized void taint() {
        originClean = false;
    }

    /**
     * Records that the picture changed and tells the listener once until the next snapshot.
     * <p>
     * 그림이 바뀌었음을 기록하고 다음 스냅샷까지 리스너에 한 번만 알린다.
     */
    synchronized void markChanged() {
        version++;
        if (!notified && !disposed) {
            notified = true;
            // 알림은 작업을 올리기만 하는 리스너가 처리한다.
            if (listener != null) {
                listener.surfaceChanged();
            }
        }
    }

    /**
     * Returns an immutable copy of the current picture for the renderer, or the previous copy when nothing changed.
     * <p>
     * 렌더러를 위한 현재 그림의 불변 복사본을 반환한다. 바뀐 것이 없으면 이전 복사본을 반환한다.
     *
     * @return the snapshot, or null if nothing was ever drawn (no context), the bitmap is empty or the surface was disposed / 스냅샷. 그린 적이 없거나(컨텍스트 없음) 비트맵이 비었거나 표면이 폐기되었으면 null
     */
    public synchronized DecodedImage snapshot() {
        notified = false;
        if (disposed || context == null || bitmap.isEmpty()) {
            return null;
        }
        flattenIfOpaque();
        if (cached == null || cachedVersion != version) {
            cached = bitmap.snapshot();
            cachedVersion = version;
        }
        return cached;
    }

    /**
     * Whether the context was created with <code>alpha: false</code>: the bitmap is then opaque and starts black.
     * <p>
     * 컨텍스트를 <code>alpha: false</code> 로 만들었는지 여부. 그러면 비트맵은 불투명하며 검정으로 시작한다.
     */
    private boolean opaque;

    /**
     * The version for which the pixels were last flattened onto black (-1: never).
     * <p>
     * 화소를 마지막으로 검정 위에 합친 버전(-1 은 한 번도 없음).
     */
    private long flatVersion = -1;

    /**
     * Makes the canvas opaque (the context attribute <code>alpha: false</code>): the bitmap becomes opaque black now, and whatever is drawn is
     * flattened onto black whenever the pixels are read, so scripts and the renderer never see a transparent pixel.
     * <p>
     * 캔버스를 불투명하게 한다(컨텍스트 속성 <code>alpha: false</code>): 비트맵은 지금 불투명한 검정이 되고, 그려진 것은 화소를 읽을 때마다 검정 위에 합쳐지므로
     * 스크립트와 렌더러는 투명한 화소를 보지 못한다.
     *
     * @param value true for an opaque canvas / 불투명한 캔버스이면 true
     */
    public synchronized void setOpaque(boolean value) {
        opaque = value;
        if (value && !bitmap.isEmpty()) {
            bitmap.fillOpaqueBlack();
            markChanged();
        }
    }

    /**
     * The context attribute <code>willReadFrequently</code> that the context was created with (a hint; it changes nothing here).
     * <p>
     * 컨텍스트를 만들 때 준 컨텍스트 속성 <code>willReadFrequently</code>(힌트이며 여기서는 아무것도 바꾸지 않는다).
     */
    private boolean willReadFrequently;

    /**
     * The context attribute <code>desynchronized</code> that the context was created with (a hint; it changes nothing here).
     * <p>
     * 컨텍스트를 만들 때 준 컨텍스트 속성 <code>desynchronized</code>(힌트이며 여기서는 아무것도 바꾸지 않는다).
     */
    private boolean desynchronized;

    /**
     * Remembers the two hint attributes of the context settings so that <code>getContextAttributes()</code> reports them back.
     * <p>
     * 컨텍스트 설정의 두 힌트 속성을 기억해 <code>getContextAttributes()</code> 가 그대로 돌려주게 한다.
     *
     * @param willRead      the value of <code>willReadFrequently</code> / <code>willReadFrequently</code> 값
     * @param desynchronize the value of <code>desynchronized</code> / <code>desynchronized</code> 값
     */
    synchronized void setHints(boolean willRead, boolean desynchronize) {
        willReadFrequently = willRead;
        desynchronized = desynchronize;
    }

    /**
     * Returns the <code>willReadFrequently</code> hint.
     * <p>
     * <code>willReadFrequently</code> 힌트를 반환한다.
     *
     * @return the hint / 힌트
     */
    synchronized boolean isWillReadFrequently() {
        return willReadFrequently;
    }

    /**
     * Returns the <code>desynchronized</code> hint.
     * <p>
     * <code>desynchronized</code> 힌트를 반환한다.
     *
     * @return the hint / 힌트
     */
    synchronized boolean isDesynchronized() {
        return desynchronized;
    }

    /**
     * Tells whether the canvas is opaque.
     * <p>
     * 캔버스가 불투명한지 알려 준다.
     *
     * @return true for <code>alpha: false</code> / <code>alpha: false</code> 이면 true
     */
    public synchronized boolean isOpaque() {
        return opaque;
    }

    /**
     * Flattens the pixels of an opaque canvas onto black if something was drawn since the last time; does nothing for other canvases.
     * <p>
     * 불투명한 캔버스에서 마지막 이후에 그린 것이 있으면 화소를 검정 위에 합친다. 다른 캔버스에서는 아무것도 하지 않는다.
     */
    synchronized void flattenIfOpaque() {
        if (opaque && flatVersion != version && !bitmap.isEmpty()) {
            bitmap.flattenOverBlack();
            flatVersion = version;
        }
    }

    /**
     * Returns the current pixels as an image source for <code>drawImage()</code>, <code>createPattern()</code> and the encoders.
     * <p>
     * 현재 화소를 <code>drawImage()</code>, <code>createPattern()</code>, 인코더를 위한 이미지 소스로 반환한다.
     *
     * @param copy true to copy the pixels (needed when the source is the canvas that is drawn on), false to share the live array / 화소를 복사하려면 true(그리는 대상 캔버스 자신이 원본일 때 필요), 살아 있는 배열을 공유하려면 false
     * @return the image, or null if the bitmap is empty / 이미지. 비트맵이 비었으면 null
     */
    public synchronized CanvasImage image(boolean copy) {
        if (bitmap.isEmpty()) {
            return null;
        }
        flattenIfOpaque();
        return new CanvasImage(bitmap.getWidth(), bitmap.getHeight(), copy ? bitmap.pixels().clone() : bitmap.pixels(), originClean);
    }

    /**
     * Releases the bitmap and the context; the surface draws nothing afterwards and the renderer sees no picture.
     * <p>
     * 비트맵과 컨텍스트를 해제한다. 그 뒤 표면은 아무것도 그리지 않고 렌더러는 그림을 보지 못한다.
     */
    public synchronized void dispose() {
        disposed = true;
        listener = null;
        cached = null;
        strongOwner = null;
        bitmap.dispose();
        bitmap = new CanvasBitmap(0, 0);
        context = null;
    }

    /**
     * Tells whether the surface was disposed.
     * <p>
     * 표면이 폐기되었는지 알려 준다.
     *
     * @return true after {@link #dispose()} / {@link #dispose()} 뒤에는 true
     */
    public synchronized boolean isDisposed() {
        return disposed;
    }
}
