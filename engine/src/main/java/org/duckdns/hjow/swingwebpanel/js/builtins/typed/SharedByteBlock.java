package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.concurrent.atomic.AtomicLongArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;

/**
 * Shared byte storage whose aligned accesses use atomic 64-bit words.
 * Growth publishes new pages and keeps the storage of existing words.
 * <p>
 * 정렬된 접근에 원자적 64비트 워드를 사용하는 공유 바이트 저장소.
 * 크기를 늘릴 때 새 페이지를 공개하고 기존 워드의 저장소를 유지한다.
 */
final class SharedByteBlock {

    /** Number of bits in a page byte offset. / 페이지 내 바이트 위치의 비트 수. */
    private static final int PAGE_SHIFT = 12;

    /** Number of bytes in a page. / 한 페이지의 바이트 수. */
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;

    /** Number of 64-bit words per page. / 한 페이지의 64비트 워드 수. */
    private static final int WORDS_PER_PAGE = PAGE_SIZE / 8;

    /** Empty pages and zero visible length. / 빈 페이지와 접근 가능한 길이 0. */
    private static final Storage EMPTY = new Storage(new AtomicLongArray[0], 0);

    /**
     * One published state of the page directory and byte length.
     * <p>
     * 한 번에 공개되는 페이지 목록과 바이트 길이.
     */
    private static final class Storage {
        /** Word pages, shared with subsequent states. / 이후 상태와 공유하는 워드 페이지. */
        final AtomicLongArray[] pages;

        /** Number of visible bytes. / 접근할 수 있는 바이트 수. */
        final int length;

        /**
         * Stores a complete directory and its visible byte count.
         * <p>
         * 완성한 페이지 목록과 접근할 수 있는 바이트 수를 저장한다.
         *
         * @param pages page directory / 페이지 목록
         * @param length visible byte length / 접근 가능한 바이트 길이
         */
        Storage(AtomicLongArray[] pages, int length) {
            this.pages = pages;
            this.length = length;
        }
    }

    /** Current shared state. / 현재 공유 저장소 상태. */
    private volatile Storage storage;

    /**
     * Allocates zero-initialized shared storage.
     * <p>
     * 0으로 초기화한 공유 저장소를 할당한다.
     *
     * @param length initial byte length / 초기 바이트 길이
     */
    SharedByteBlock(int length) {
        storage = extend(EMPTY, length);
    }

    /**
     * Returns the published byte length.
     * <p>
     * 공개된 바이트 길이를 반환한다.
     *
     * @return current length / 현재 길이
     */
    int length() {
        return storage.length;
    }

    /**
     * Grows the byte range while retaining pages used by concurrent operations.
     * <p>
     * 동시에 실행 중인 연산이 쓰는 페이지를 유지하면서 바이트 범위를 넓힌다.
     *
     * @param length requested byte length / 요청 바이트 길이
     */
    synchronized void grow(int length) {
        Storage old = storage;
        if (length != old.length) {
            storage = extend(old, length);
        }
    }

    /**
     * Builds an expanded directory before making it visible to readers and writers.
     * <p>
     * 읽고 쓰는 쪽에 보이게 하기 전에 확장된 페이지 목록을 만든다.
     *
     * @param old previous storage / 이전 저장소
     * @param length requested length / 요청 길이
     * @return storage for the new length / 새 길이의 저장소
     */
    private static Storage extend(Storage old, int length) {
        if (length < old.length || length > ArrayBufferObject.MAX_BYTE_LENGTH) {
            throw JsException.rangeError("Invalid shared data block length");
        }
        int pageCount = length == 0 ? 0 : ((length - 1) >>> PAGE_SHIFT) + 1;
        try {
            AtomicLongArray[] pages = old.pages;
            if (pageCount != pages.length) {
                pages = new AtomicLongArray[pageCount];
                System.arraycopy(old.pages, 0, pages, 0, old.pages.length);
                // 기존 원자 워드는 그대로 두고 새 페이지의 워드를 0으로 할당한다.
                for (int i = old.pages.length; i < pageCount; i++) {
                    pages[i] = new AtomicLongArray(WORDS_PER_PAGE);
                }
            }
            return new Storage(pages, length);
        } catch (OutOfMemoryError e) {
            throw JsException.rangeError("Unable to allocate shared data block");
        }
    }

    /**
     * Reads a value of one to eight bytes. Values within one word are read atomically.
     * <p>
     * 한 바이트에서 여덟 바이트까지 읽는다. 한 워드 안의 값은 원자적으로 읽는다.
     *
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @param littleEndian whether to use little endian / 리틀 엔디언 사용 여부
     * @return bits of the value / 값의 비트
     */
    long load(int offset, int width, boolean littleEndian) {
        Storage current = checkedStorage(offset, width);
        long bits = 0;
        if ((offset & 7) + width <= 8) {
            bits = wordField(current, offset, width);
        } else {
            // 워드 경계를 넘는 비정렬 접근은 바이트를 순서대로 읽는다.
            for (int i = 0; i < width; i++) {
                bits |= wordField(current, offset + i, 1) << (8 * i);
            }
        }
        return littleEndian ? bits : reverse(bits, width);
    }

    /**
     * Writes a value, preserving adjacent bytes in the same atomic word.
     * <p>
     * 같은 원자 워드의 이웃 바이트를 보존하면서 값을 쓴다.
     *
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @param bits value bits / 값의 비트
     * @param littleEndian whether to use little endian / 리틀 엔디언 사용 여부
     */
    void store(int offset, int width, long bits, boolean littleEndian) {
        Storage current = checkedStorage(offset, width);
        long value = littleEndian ? bits : reverse(bits, width);
        if ((offset & 7) + width <= 8) {
            updateWord(current, offset, width, 0, value, false);
        } else {
            // 워드 경계를 넘는 비정렬 접근은 바이트를 순서대로 쓴다.
            for (int i = 0; i < width; i++) {
                updateWord(current, offset + i, 1, 0, value >>> (8 * i), false);
            }
        }
    }

    /**
     * Replaces an aligned value if it equals the expected value.
     * <p>
     * 정렬된 값이 기대한 값과 같으면 교체한다.
     *
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @param expected expected bits / 기대 비트
     * @param value replacement bits / 교체 비트
     * @return previous value bits / 이전 값의 비트
     */
    long compareExchange(int offset, int width, long expected, long value) {
        Storage current = checkedStorage(offset, width);
        if ((offset & 7) + width > 8) {
            throw JsException.rangeError("Atomic access crosses a shared word");
        }
        return updateWord(current, offset, width, expected, value, true);
    }

    /**
     * Captures the current storage after checking the requested byte range.
     * <p>
     * 요청한 바이트 범위를 확인한 뒤 현재 저장소를 읽는다.
     *
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @return a stable page directory / 안정적인 페이지 목록
     */
    private Storage checkedStorage(int offset, int width) {
        Storage current = storage;
        if (width < 1 || width > 8 || offset < 0 || offset > current.length - width) {
            throw JsException.rangeError("Shared byte access is out of bounds");
        }
        return current;
    }

    /**
     * Reads a field wholly within one word.
     * <p>
     * 한 워드에 모두 포함된 필드를 읽는다.
     *
     * @param storage the captured storage / 읽어 둔 저장소
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @return unsigned field bits / 부호 없는 필드 비트
     */
    private static long wordField(Storage storage, int offset, int width) {
        AtomicLongArray page = storage.pages[offset >>> PAGE_SHIFT];
        long word = page.get((offset >>> 3) & (WORDS_PER_PAGE - 1));
        return (word >>> (8 * (offset & 7))) & mask(width);
    }

    /**
     * Updates all or part of a word with a compare-and-set loop.
     * <p>
     * compare-and-set 반복으로 워드의 전체 또는 일부를 갱신한다.
     *
     * @param storage the captured storage / 읽어 둔 저장소
     * @param offset first byte / 첫 바이트
     * @param width byte count / 바이트 수
     * @param expected expected field value / 기대 필드 값
     * @param value replacement field value / 교체 필드 값
     * @param compare true to require a field match / 필드가 일치해야 하면 true
     * @return the field before the update / 갱신 전 필드
     */
    private static long updateWord(Storage storage, int offset, int width, long expected, long value, boolean compare) {
        AtomicLongArray page = storage.pages[offset >>> PAGE_SHIFT];
        int index = (offset >>> 3) & (WORDS_PER_PAGE - 1);
        int shift = 8 * (offset & 7);
        long fieldMask = mask(width);
        long positionedMask = fieldMask << shift;
        long replacement = (value & fieldMask) << shift;
        long old;
        long field;
        // 다른 스레드가 같은 워드를 바꾸면 이웃 바이트를 다시 읽고 비교한다.
        do {
            old = page.get(index);
            field = (old >>> shift) & fieldMask;
            if (compare && field != (expected & fieldMask)) {
                return field;
            }
        } while (!page.compareAndSet(index, old, (old & ~positionedMask) | replacement));
        return field;
    }

    /**
     * Returns the mask of a field occupying the given byte count.
     * <p>
     * 주어진 바이트 수를 차지하는 필드의 마스크를 반환한다.
     *
     * @param width byte count / 바이트 수
     * @return the low-bit mask / 하위 비트 마스크
     */
    static long mask(int width) {
        return width == 8 ? -1L : (1L << (8 * width)) - 1;
    }

    /**
     * Reverses the order of the selected low bytes.
     * <p>
     * 선택한 하위 바이트의 순서를 뒤집는다.
     *
     * @param bits field bits / 필드 비트
     * @param width byte count / 바이트 수
     * @return bits of the differently ordered field / 순서를 바꾼 필드 비트
     */
    private static long reverse(long bits, int width) {
        return Long.reverseBytes(bits) >>> (8 * (8 - width));
    }
}
