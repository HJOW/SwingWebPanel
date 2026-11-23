import json, sys
tables = json.load(open(sys.argv[1]))
out_path = sys.argv[2]
q = tables['Quantizer_Matrix']
flat = q['flat']
assert q['shape'] == [15, 2, 3344] and len(flat) == 15 * 2 * 3344 and max(flat) < 256 and min(flat) >= 0
hexs = ''.join('%02x' % v for v in flat)
CH = 25000
chunks = [hexs[i:i + CH] for i in range(0, len(hexs), CH)]
o = []
o.append('package org.duckdns.hjow.swingwebpanel.media.image.avif;\n')
o.append('''/**
 * The quantizer matrices of the AV1 specification (table <code>Quantizer_Matrix</code> of section 7.12.3, 15 levels x 2 plane types x 3344
 * weights). The table is generated from the specification by <code>tests/resources/media/avif/generation/gen_qm.py</code>; each weight is
 * stored as two hexadecimal digits so that the class stays below the limits of the Java class file format. The table is decoded when the
 * class is first used, that is, only for frames that use quantizer matrices.
 * <p>
 * AV1 규격의 양자화 행렬(7.12.3절의 <code>Quantizer_Matrix</code> 표, 15 수준 x 평면 종류 2 x 가중치 3344개). 이 표는
 * <code>tests/resources/media/avif/generation/gen_qm.py</code> 가 규격에서 생성하며, 클래스가 Java 클래스 파일 형식의 한계를 넘지 않도록 가중치마다
 * 16진수 두 자리로 저장한다. 표는 클래스를 처음 쓸 때, 즉 양자화 행렬을 쓰는 프레임에서만 풀어 놓는다.
 */''')
o.append('final class AvifQmTable {\n')
o.append('''    /**
     * The number of weights of one level and plane type.
     * <p>
     * 수준 하나와 평면 종류 하나의 가중치 수.
     */
    private static final int QM_TOTAL_SIZE = 3344;
''')
o.append('''    /**
     * The hexadecimal text of the table in pieces (level, plane type, position in row-major order).
     * <p>
     * 표의 16진수 텍스트 조각(수준, 평면 종류, 위치 순서의 행 우선).
     */
    private static final String[] HEX = {''')
for i, c in enumerate(chunks):
    o.append('        "%s"%s' % (c, ',' if i < len(chunks) - 1 else ''))
o.append('    };\n')
o.append('''    /**
     * The decoded weights (one byte each, to be read as unsigned values).
     * <p>
     * 풀어 놓은 가중치(각각 1바이트, 부호 없는 값으로 읽는다).
     */
    private static final byte[] WEIGHTS = decode();

    /**
     * Private constructor: static data holder.
     * <p>
     * 비공개 생성자: 정적 자료 보관 클래스이다.
     */
    private AvifQmTable() {
    }

    /**
     * Decodes the hexadecimal text.
     * <p>
     * 16진수 텍스트를 푼다.
     *
     * @return the weights / 가중치
     */
    private static byte[] decode() {
        byte[] out = new byte[15 * 2 * QM_TOTAL_SIZE];
        int w = 0;
        int half = 0;
        boolean high = true;
        // 조각마다 16진수 두 자리를 한 바이트로 합쳐 채운다.
        for (int k = 0; k < HEX.length; k++) {
            String s = HEX[k];
            // 조각의 16진수 문자를 하나씩 읽는다.
            for (int i = 0; i < s.length(); i++) {
                int d = Character.digit(s.charAt(i), 16);
                if (high) {
                    half = d << 4;
                } else {
                    out[w++] = (byte) (half | d);
                }
                high = !high;
            }
        }
        return out;
    }

    /**
     * Returns one weight of the quantizer matrix.
     * <p>
     * 양자화 행렬의 가중치 하나를 반환한다.
     *
     * @param level  the matrix level 0..14 / 행렬 수준 0~14
     * @param chroma 0 for luma, 1 for chroma / 휘도면 0, 색차면 1
     * @param index  the position <code>Qm_Offset[txSz] + i * tw + j</code> / 위치 <code>Qm_Offset[txSz] + i * tw + j</code>
     * @return the weight / 가중치
     */
    static int weight(int level, int chroma, int index) {
        return WEIGHTS[(level * 2 + chroma) * QM_TOTAL_SIZE + index] & 0xFF;
    }
}''')
open(out_path, 'w', encoding='utf-8', newline='\n').write('\n'.join(o) + '\n')
print('written', out_path, len(chunks), 'chunks')
