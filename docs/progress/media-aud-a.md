# MEDIA-AUD-A 진행 기록 (WAV/AIFF/AU, Ogg, FLAC, Vorbis)

> **갱신(MEDIA-D, 2026-11-18)**: 이 문서가 "시험용 인코더로만 검증"이라고 적은 한계는 `media-matrix.md` 11절에서 실제 인코더(ffmpeg 의 libmp3lame, 기본 AAC, libvorbis, libopus, flac)가 만든 파일과 ffmpeg 의 참조 PCM 으로 다시 확인했다. 그 과정에서 찾아 고친 결함과 남은 한계는 11.4~11.6절을 따른다.

담당 경로: `src/org/duckdns/hjow/swingwebpanel/media/audio/**`(mp3/aac/opus/webm 하위 패키지와 mp4 컨테이너 제외), `tests/src/org/duckdns/hjow/swingwebpanel/media/audio/{testkit,pcm,registry,ogg,flac,vorbis}`, 이 문서.
시험 실행: `scripts/pkgtest.sh` 는 소스 패키지를 재귀로 찾아 AUD-B 의 미완성 패키지까지 컴파일하고, 패키지 루트의 `AudioDecoders` 를 단독으로 지정할 수 없다.
그래서 AUD-A 는 같은 방식의 임시 스크립트(소스: `media/*.java`, `media/audio/*.java`, `media/audio/{pcm,ogg,flac,vorbis}/**`)로 시험했다.
AUD-B 가 끝난 뒤에는 `scripts/pkgtest.sh maa "media" "media/audio"` 로 전체를 한 번에 확인할 수 있다.

## 현재 작업 번호: P18-07 (PCM 계열 완료, Ogg/FLAC/Vorbis 진행)

### 1. AudioDecoders 와 PCM 계열 (WAV/AIFF/AU)

- 변경 파일: `media/audio/{AudioDecoders,BlockPcmSource,Bytes,ChannelMixer,PcmConverter}.java`, `media/audio/pcm/{SampleFormat,RawPcmSource,AdpcmSource,WavDecoder,AiffDecoder,AuDecoder}.java`,
  `tests/.../media/audio/testkit/AudioKit.java`, `tests/.../media/audio/pcm/{WavDecoderTest,AiffAuDecoderTest,AdpcmTest,SampleConversionTest}.java`, `tests/.../media/audio/registry/{AudioDecodersTest,BlockPcmSourceTest}.java`
- 검증 (JDK 6u45, 위 임시 스크립트): 47 passed / 0 failed(시험 실행기 자체 시험 7건 포함). sourcecheck 0 위반(내 경로).
- 공개 API (AUD-B 가 재사용 가능): `AudioDecoders.open(byte[], mimeHint, limits)`, `register(AudioDecoder)`, `getSupportedMimeTypes()`, `isSupportedMime`, `canPlayType`, `normalizeMime`;
  `BlockPcmSource`(블록 단위 디코딩 기반 클래스: `fill(byte[])`, `seekTo(long)`, `canSeek()`, `release()`), `ChannelMixer`(다운믹스), `PcmConverter`(G.711, `clip16`, `narrow`), `Bytes`.
- AUD-B 가 알아야 할 것: 선택적 디코더는 리플렉션으로 `org.duckdns.hjow.swingwebpanel.media.audio.{mp3.Mp3Decoder,aac.AacDecoder,opus.OpusDecoder,webm.WebmDecoder}`(공개 무인자 생성자)를 찾는다.
  `canPlayType` 의 코덱 판정은 디코더의 `getFormatName()` 에 의존한다: `mp3`, `aac`, `opus`, `webm`(그 밖의 이름이면 해당 코덱은 빈 문자열을 반환).

#### 정책(문서화한 결정)

- 출력은 항상 16비트 부호 있는 리틀 엔디언 인터리브, 모노/스테레오. 채널이 3개 이상이면 `ChannelMixer` 로 스테레오로 섞는다: 앞 L/R 은 자기 쪽, 앞/뒤 중앙은 -3 dB 로 양쪽, 뒤/옆 L/R 은 -3 dB 로 자기 쪽, LFE 는 버림, 각 쪽의 가중치 합 중 큰 값으로 나누어 풀 스케일을 넘지 않게 한다. 기본 배치는 WAV/FLAC/Vorbis 의 일반 스피커 순서(3: LRC, 4: LRBlBr, 5: LRCBlBr, 6: LRCLfeBlBr, 7: LRCLfeBcSlSr, 8: LRCLfeBlBrSlSr), 9개 이상은 L/R 교대. WAV 확장 헤더의 스피커 마스크가 있고 설정 비트 수가 채널 수와 같으면 마스크를 따른다.
- 형식은 파일 시그니처로 정하고 MIME 타입은 힌트로만 쓴다(힌트의 타입을 선언한 디코더를 먼저 시도). 빈 데이터 CORRUPT, 알 수 없는 형식 UNSUPPORTED_FORMAT, `DecodeLimits.getMaxBytes()` 초과 TOO_LARGE.
- WAV: `data` 청크 크기가 0, 0xFFFFFFFF 이거나 파일보다 크면 파일 끝까지로 본다(스트리밍/잘린 파일). 프레임 중간에서 잘린 데이터는 마지막 불완전 프레임을 버리고 정상 종료한다. `fmt `/`data` 가 없으면 CORRUPT, 압축 태그(MP3 등)는 UNSUPPORTED_FORMAT.
- AIFF: COMM 의 프레임 수가 실제보다 많으면 있는 만큼만, 적으면 선언한 만큼만 재생(0 이면 전체). `ima4` 등 지원하지 않는 AIFF-C 압축은 UNSUPPORTED_FORMAT.
- AU: 크기 0xFFFFFFFF/초과는 파일 끝까지. ADPCM/DSP 인코딩은 UNSUPPORTED_FORMAT.
- 잘린/손상된 파일: PCM 계열은 정상 종료, ADPCM 블록 헤더 오류(스텝 인덱스>88, 예측기 번호 초과)는 읽기에서 CORRUPT. 오류가 나기 전까지 디코딩한 PCM 은 먼저 전달하고 그 다음 읽기부터 같은 오류를 계속 보고한다(`BlockPcmSource`). `seekToFrame` 은 오류 상태를 해제한다.
- 닫은 뒤의 `read`/`seekToFrame` 은 `MediaException(ACCESS_FAILED)`.
- 위치 이동은 항상 정확하다(PCM 은 즉시, ADPCM 은 블록 단위로 이동한 뒤 앞부분을 버림).

#### JDK 6u45 의 Java Sound 가 읽는 형식 (실제 실행 결과, `AudioSystem.getAudioInputStream`)

읽음: WAV PCM 8/16/24/32, WAV float 32/64, WAV A-law/μ-law, WAV 6채널 PCM, AU μ-law/PCM 8/16/24/32/A-law, AU 크기 미정(0xFFFFFFFF), AIFF PCM 8/16/24/32, AIFC `ulaw`.
못 읽음(UnsupportedAudioFileException): WAV IMA ADPCM, WAV MS ADPCM, WAV EXTENSIBLE, AU float32/float64, AIFC `sowt`/`twos`/`fl32`.
읽기는 성공하지만 내용이 비는 경우: WAV `data` 크기 0xFFFFFFFF(스트리밍 헤더)는 `read` 가 즉시 -1.
결론: 위 못 읽음/오작동 경우를 처리해야 하므로 PCM 계열은 Java Sound 의 파서를 쓰지 않고 자체 파서로 구현했다(Java Sound 는 사용하지 않음).

### 체크박스 후보

- 아직 없음(AUD-01/02/03 은 위 시험으로 검증했으나, 조정자는 실제 `AudioDecoders` 가 Ogg/FLAC 디코더까지 갖춘 뒤 AUD-06/07 과 함께 판단).
  PCM 계열(AUD-01, AUD-02, AUD-03)은 구현과 시험이 끝났으므로 FEATURE_MATRIX 의 "검증 완료" 후보이다. P18-07 은 AUD-04~09 가 모두 끝나야 체크할 수 있다.

### 미해결/남은 일

- Ogg 컨테이너(`media/audio/ogg`), FLAC(`flac`), Vorbis(`vorbis`)는 아직 구현 중이다. 현재 `FlacDecoder`/`VorbisDecoder` 는 아무것도 인식하지 않는 자리 표시 클래스이다.

### 2. Ogg 컨테이너 (`media/audio/ogg`) — AUD-B 의 Opus 디코더가 재사용한다

- 변경 파일: `media/audio/ogg/{OggPage,OggCrc,OggPacket,OggStream,OggPacketStream,OggReader}.java`, `tests/.../testkit/OggMux.java`(비트 단위 CRC 로 쓴 시험용 멀티플렉서), `tests/.../ogg/OggReaderTest.java`
- 검증: 시험 8건 추가(전체 55 passed / 0 failed), sourcecheck 0 위반.
- 공개 API (안정): `OggReader.scan(byte[])` → `getStreams()`(`OggStream` 목록), `findFirst("opus")`, `OggReader.sniffCodecs(byte[])`(파일 첫머리 BOS 페이지의 코덱 목록; 전체 색인 없이 `accepts()` 에 쓰기 좋음),
  `OggStream`(`getSerial/getCodec/getFirstPacket/getPageCount/getPageOffset/getPageGranule/getLastGranule/findPageAfterGranule/openPackets(startPage)`),
  `OggPacketStream.next()` → `OggPacket`(`getData/getGranule/isLast/getPageIndex`) 또는 null, `getDamagedPages()`.
- 정책: 색인할 때는 페이지 헤더만 읽고(가짜 `OggS` 가 진짜 페이지를 삼키지 않도록, 페이지 바로 뒤가 `OggS`/파일 끝이 아니면 CRC 로 확인) 페이지를 읽을 때 CRC-32 를 확인한다.
  CRC 가 틀린 페이지, 페이지 순서의 빈틈, 시작 페이지에서 이어 오는 패킷은 해당 패킷만 잃고 다음 패킷 경계부터 계속한다(`getDamagedPages()`). 잘린 마지막 페이지는 무시한다.
  다중화(grouped) 파일은 BOS 순서대로 스트림이 나열되고, 체인(chained) 파일은 끝난 스트림 뒤의 새 스트림(시리얼이 같아도)이 목록에 추가된다.
  재생기는 `findFirst(codec)` 로 체인의 첫 고리만 재생한다(체인 중간에 샘플링 주파수/채널이 바뀔 수 있어 이어 재생하지 않음). 패킷이 페이지에 끝나는 마지막 패킷이면 `getGranule()` 이 그 페이지의 그래뉼 위치이고 아니면 -1.

### 3. FLAC (`media/audio/flac`) — 네이티브 FLAC 과 Ogg FLAC

- 변경 파일: `media/audio/flac/{FlacDecoder,FlacStreamInfo,FlacBitReader,FlacFrameDecoder,FlacPcmSource,NativeFlacSource,OggFlacSource}.java`, `tests/.../testkit/FlacWriter.java`(FLAC 규격에서 직접 쓴 시험용 인코더: 상수/그대로/고정/LPC(자기상관+Levinson), Rice 파티션/이스케이프/5비트 매개변수, 버려진 비트, 4가지 스테레오 방식, 가변 블록, CRC-8/16, 탐색 표, Ogg FLAC 패킷), `tests/.../flac/FlacDecoderTest.java`(12건)
- 검증 (JDK 6u45): 전체 66 passed / 0 failed(FLAC 12건). sourcecheck 0 위반.
- 지원: 서브프레임 전 유형, 채널 배정 전부(독립 1~8채널, left/side, side/right, mid/side), 샘플 크기 4~32비트(16비트로 반올림, 디더 없음; 32비트의 33비트 side 채널 포함), 고정/가변 블록 크기(모든 블록 크기 코드와 8/16비트 명시 값),
  샘플링 주파수 코드(표준값, kHz, Hz, STREAMINFO), 프레임 CRC-8/CRC-16 검증, ID3v2 접두, ID3v1/패딩 꼬리표, STREAMINFO 의 전체 샘플 수 0(길이 미상), 탐색(SEEKTABLE 지점이 실제 프레임 시작인지 확인해 사용, 없으면 디코딩하며 만드는 프레임 색인; 항상 정확한 위치),
  Ogg FLAC(매핑 헤더/메타데이터 패킷, 오디오 패킷=프레임, 그래뉼 위치로 탐색).
- 정책: 프레임 헤더 CRC-8 이 틀려 헤더로 인정되지 않는 위치에서 뒤에 올바른 프레임이 있으면 "동기 상실" CORRUPT, 뒤에 올바른 프레임이 없으면(꼬리표/패딩) 정상 종료.
  CRC-16 불일치나 서브프레임 구조 오류는 CORRUPT(그 앞까지의 PCM 을 모두 전달한 뒤 다음 읽기에서 보고). 파일 끝에서 잘린 프레임은 정상 종료. Chromium(ffmpeg)은 CRC 오류를 건너뛰고 계속하지만 여기서는 엄격하게 처리한다(문서화한 결정).
  채널 3~8 은 FLAC 스피커 배치(= `ChannelMixer.forChannels` 기본 배치)로 스테레오 다운믹스.
- MIME: `audio/flac`, `audio/x-flac`(Ogg FLAC 은 컨테이너 MIME `audio/ogg` 로 오지만 시그니처로 인식).
- 한계: STREAMINFO 의 MD5 는 검증하지 않는다. 비표준 헤더(샘플 크기 코드 7 = 32비트)는 RFC 9639 에 따라 허용한다.

### 4. Vorbis I (`media/audio/vorbis`) — Ogg Vorbis 전체 디코더

- 변경 파일: `media/audio/vorbis/{VorbisDecoder,VorbisSetup,VorbisCodebook,VorbisFloor,VorbisFloor0,VorbisFloor1,VorbisResidue,Imdct,VorbisStream,VorbisSource,VorbisBitReader}.java`,
  `tests/.../testkit/{VorbisWriter,VorbisBook,VorbisBits}.java`(Vorbis I 규격에서 직접 쓴 시험용 인코더), `tests/.../vorbis/{VorbisDecoderTest,VorbisFloorTest}.java`
- 검증 (JDK 6u45, 임시 스크립트): 전체 76 passed / 0 failed(Vorbis 시험 10건: IMDCT 정의식 비교, 코드북 5종, 설정 조합 모델 비교, 입력 복원 SNR, 잘린 끝, 탐색, 다중화/체인, 손상, 플로어 0 공식, 사용 안 함 표시). sourcecheck 0 위반.
  Vorbis 시험은 이차 시간 복잡도의 시험용 인코더 때문에 약 10초 걸린다.
- 지원: 식별/주석/설정 헤더, 코드북(ordered/sparse/일반, 조회 유형 0/1/2, 부호어 배정은 규격의 최저 미사용 부호어), 플로어 0(LSP)과 1(예측 부호화, 서브클래스), 잔차 0/1/2(여러 패스), 채널 결합, 하위 맵, 짧은/긴 블록과 창 모양(이웃 블록 크기에 따른 비대칭 창),
  FFT 기반 IMDCT(O(N log N), 블록 크기 64~8192), 임의 채널 수(3개 이상은 스테레오 다운믹스), 그래뉼 위치로 스트림 끝 자르기, 그래뉼 위치와 블록 크기로 정확한 탐색(이전 페이지에서 시작해 겹침을 준비한 뒤 프레임을 버림).
- 검증 방식(독립성): 시험용 인코더는 디코더 코드를 쓰지 않는다. 모델 PCM 은 직접(제곱 시간) MDCT/IMDCT 공식과 시간축 누적(중심 간격 = 이전 블록 1/4 + 이번 블록 1/4)으로 계산해 디코더의 이월/겹침 로직과 구조가 다르다. 부호어 배정은 구간 방식으로 구현했다.
  순방향 MDCT 스케일 4/N 은 TDAC 조건에서 유도했고 입력 복원 시험(SNR > 25 dB; 짧은/긴/섞인 블록)이 디코더 쪽 비정규화 IMDCT 가정과 일치함을 확인한다.
- 정책/한계: 시험 데이터는 모두 자체 인코더가 만든 것이다. 실제 libvorbis 로 만든 파일과의 상호 운용성은 이 환경에서 확인하지 못했다(외부 파일 다운로드 금지). 알려진 가정: floor1 의 dB 표는 `10^((i-255)*7/256)` 를 float 로 계산(참조 표와 float 반올림 수준의 차이).
  첫 오디오 페이지의 그래뉼 위치가 0 이 아닌 잘린 스트림은 위치를 0 부터 센다. Opus 는 다루지 않는다(AUD-B).
- MIME: `audio/ogg`, `application/ogg`, `audio/vorbis`.

### 체크박스 후보 (조정자용)

- FEATURE_MATRIX AUD-01(WAV), AUD-02(AIFF), AUD-03(AU), AUD-06(Ogg Vorbis), AUD-07(FLAC)은 구현과 시험이 끝났다("검증 완료" 후보). 단 AUD-06 은 실제 libvorbis 파일로 확인하지 못했다는 한계를 제약란에 적어야 한다.
- P18-07 은 AUD-04(MP3), AUD-05(AAC), AUD-08(Opus), AUD-09(WebM)까지 끝나야 체크할 수 있다. P18-01 의 오디오 부분(MIME 힌트 + 시그니처 판별, 실패 종류 구분)과 P18-02 의 오디오 부분(Java Sound 사용 여부 확인 후 자체 구현)은 AudioDecoders 로 완료.

### 조정자 요청

- `scripts/pkgtest.sh maa "media" "media/audio"` 가 AUD-B 의 코드까지 컴파일된 뒤에 전체를 한 번에 확인해 주세요(제 시험은 임시 스크립트로 AUD-A 패키지만 확인).
- AudioDecoders 의 `canPlayType` 이 코덱을 판정하려면 AUD-B 디코더의 `getFormatName()` 이 `mp3`, `aac`, `opus`, `webm` 이어야 합니다.
