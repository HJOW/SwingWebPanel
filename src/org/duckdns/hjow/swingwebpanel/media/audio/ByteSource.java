package org.duckdns.hjow.swingwebpanel.media.audio;

/**
 * Random-access bytes of an encoded audio resource (plan item P18-08): the length is known and any position can be read, so a decoder can read
 * the headers, an index and then frame after frame without the whole resource in memory. A resource that was fetched into memory is an
 * {@link ArrayByteSource}; a long resource that was spooled to a temporary file is a {@link FileByteSource}, which keeps only a small window of
 * the file in memory. The decoders are meant to read through this interface so that both kinds decode to exactly the same bytes; until they are
 * moved over (see <code>docs/progress/media-play.md</code>, MEDIA-E), they still take a byte array.
 * <p>
 * Contract: {@link #get(long)} throws {@link IndexOutOfBoundsException} outside <code>0..length()-1</code> (exactly like an array, so a damaged
 * header that points past the end fails the same way as before); {@link #read(long, byte[], int, int)} copies as many bytes as exist and
 * returns their number. An input error of the underlying file (it was closed because the page went away, or it vanished) is a
 * {@link ByteSourceException}. A source may be used by one thread at a time; the file source is also safe for several threads. Closing
 * releases the file handle; a decoder never closes the source it was given (its owner does, see {@link MediaData}).
 * <p>
 * 인코딩된 오디오 리소스의 임의 접근 바이트(계획 P18-08): 길이를 알고 어느 위치든 읽을 수 있으므로 디코더는 리소스 전체를 메모리에 두지 않고 헤더와
 * 색인을 읽은 뒤 프레임을 하나씩 읽을 수 있다. 메모리로 가져온 리소스는 {@link ArrayByteSource} 이고, 임시 파일에 받아 둔 긴 리소스는 파일의 작은
 * 창만 메모리에 두는 {@link FileByteSource} 이다. 디코더는 두 종류가 정확히 같은 바이트로 디코딩되도록 이 인터페이스로 읽게 할
 * 계획이며, 옮기기 전까지는(<code>docs/progress/media-play.md</code> 의 MEDIA-E 참고) 아직 바이트 배열을 받는다.
 * <p>
 * 계약: {@link #get(long)} 은 <code>0..length()-1</code> 밖에서 {@link IndexOutOfBoundsException} 을 던진다(배열과 똑같아서 끝 너머를 가리키는 손상된
 * 헤더가 예전과 같은 방식으로 실패한다). {@link #read(long, byte[], int, int)} 는 있는 만큼 복사하고 그 수를 반환한다. 바탕 파일의 입력 오류(페이지가
 * 사라져 닫혔거나 파일이 없어짐)는 {@link ByteSourceException} 이다. 한 번에 한 스레드가 쓰면 되며 파일 원천은 여러 스레드에서도 안전하다. 닫으면
 * 파일 핸들을 놓는다. 디코더는 받은 원천을 닫지 않는다(소유자가 닫는다. {@link MediaData} 참고).
 */
public interface ByteSource {

    /**
     * Returns the number of bytes.
     * <p>
     * 바이트 수를 반환한다.
     *
     * @return the length / 길이
     */
    long length();

    /**
     * Returns the unsigned byte at a position.
     * <p>
     * 위치의 부호 없는 바이트를 반환한다.
     *
     * @param position the position, 0 to <code>length()-1</code> / 위치. 0 부터 <code>length()-1</code> 까지
     * @return the byte, 0 to 255 / 바이트(0~255)
     * @throws IndexOutOfBoundsException outside the source / 원천 밖이면
     * @throws ByteSourceException       on an input error / 입력 오류 시
     */
    int get(long position);

    /**
     * Copies bytes starting at a position into an array; fewer bytes than asked are copied only at the end of the source.
     * <p>
     * 위치부터 바이트를 배열에 복사한다. 요청보다 적게 복사하는 것은 원천의 끝에서뿐이다.
     *
     * @param position the first position, 0 or more / 첫 위치(0 이상)
     * @param buffer   the destination / 대상 배열
     * @param offset   the start in the destination / 대상의 시작 위치
     * @param length   the number of bytes wanted / 원하는 바이트 수
     * @return the bytes copied, 0 at or after the end / 복사한 바이트 수. 끝이거나 끝 뒤면 0
     * @throws IndexOutOfBoundsException for a negative position or a bad destination range / 음수 위치나 잘못된 대상 범위면
     * @throws ByteSourceException       on an input error / 입력 오류 시
     */
    int read(long position, byte[] buffer, int offset, int length);

    /**
     * Releases the underlying handle (idempotent); later reads fail with a {@link ByteSourceException}.
     * <p>
     * 바탕 핸들을 놓는다(여러 번 불러도 된다). 이후의 읽기는 {@link ByteSourceException} 으로 실패한다.
     */
    void close();
}
