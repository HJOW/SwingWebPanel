# hrtfgen (AI용 문서)

## 목적

MIT KEMAR 더미 헤드의 HRTF 측정(Bill Gardner, Keith Martin, MIT Media Lab, 1994)의 **compact 자료**(`compact.zip`)를 Web Audio 엔진의
`PannerNode`(`panningModel = "HRTF"`)가 읽는 이진 자원 파일 하나로 바꾸는 개발 보조 도구이다. 제품 기능이 아니며 제품 JAR/테스트는 이 도구의 클래스에
의존하지 않는다. 제품 쪽 읽기 코드는 `browser/src/main/java/org/duckdns/hjow/swingwebpanel/media/webaudio/dsp/HrtfDatabase.java` 이고, 생성한 자원은
`browser/src/main/resources/org/duckdns/hjow/swingwebpanel/media/webaudio/dsp/kemar-compact.hrtf`(Maven이 브라우저 JAR에 복사한다)이다.
계획 항목: P25-04(PannerNode, HRTF 데이터).

## 원본 자료와 이용 조건

- 원본: `https://sound.media.mit.edu/resources/KEMAR/compact.zip` (소개 페이지 `https://sound.media.mit.edu/resources/KEMAR.html`).
- 조회: 2026-11-22, `tools/webfetch`(JRE 25)로 내려받음. 응답 `Last-Modified: Fri, 14 Jul 2000 13:39:50 GMT`, 231461 바이트, CRC-32 는 도구가 출력 파일의 출처 표기에 기록한다.
- 이용 조건(같은 페이지와 `https://sound.media.mit.edu/resources/KEMAR/README`): "This data is Copyright 1994 by the MIT Media Laboratory. It is provided free with
  no restrictions on use, provided the authors are cited when the data is used in any research or commercial application." 즉 **저자 표기를 조건으로 자유롭게 쓸 수 있다**.
  저자 표기는 출력 파일 안의 문장, `HrtfDatabase` Javadoc, 이 도구의 `README.md` 에 있다(저장소 루트 `README.md` 에도 넣는 것은 조정자 요청으로 남겼다).
- 자료의 형식(원본 FAQ Q3, 확인한 사실): 압축 파일 안에 `elev<고도>/H<고도>e<방위각>a.wav` 368개. 각 파일은 44100 Hz, 16비트, 스테레오(왼쪽 귀, 오른쪽 귀)의
  표준 RIFF WAV 이며 data 청크가 512바이트(128프레임)이다. 고도 -40~90도(10도 간격, 14개), 방위각 0~180도(오른쪽, 고도마다 간격이 다름, 파일 이름의 정수 도).
  왼쪽(-방위각)은 좌우 대칭(귀 바꾸기)으로 얻는다.

## 빌드와 실행

- 소스: `tools/hrtfgen/src/hrtfgen/HrtfGen.java` (Java 6 문법, 외부 라이브러리 없음).
- 빌드: `tools/hrtfgen/build.sh` 또는 `tools\hrtfgen\build.bat` (JDK 6 필요. `JAVA6_HOME` 으로 경로 변경, 기본 `C:\Program Files\Java\jdk1.6.0_45`. 못 찾으면 종료 코드 1)
- 실행: `tools/hrtfgen/run.sh <compact.zip> <출력.hrtf> [--retrieved YYYY-MM-DD]` 또는 `tools\hrtfgen\run.bat ...`. 네트워크를 쓰지 않으므로 JRE 6 으로 실행한다.
  - 예: `sh tools/hrtfgen/run.sh build/hrtf-src/compact.zip browser/src/main/resources/org/duckdns/hjow/swingwebpanel/media/webaudio/dsp/kemar-compact.hrtf --retrieved 2026-11-22`
  - 같은 입력이면 출력 바이트가 항상 같다(재현 가능). JDK 6u45 에서 약 40초.
- 종료 코드: 0 성공 / 1 사용법 오류 / 2 압축 파일을 읽지 못함·형식이 기대와 다름·출력 파일을 쓰지 못함.
- 출력(stderr): 측정 수, 제거한 공통 지연, 남은 최대 지연, 자른 최소 위상 커널의 크기 응답 최대 오차(dB), 자르기로 잘려 나간 최대 에너지 비율.
  2026-11-22 실행 결과: 368개, 공통 지연 6.37 샘플, 남은 최대 지연 29.99 샘플(0.68 ms), 크기 응답 오차 0.001 dB(100 Hz~16 kHz, 피크보다 40 dB 안쪽 빈), 잘려 나간 에너지 5e-8.
- 압축 파일은 디스크에 풀지 않고 메모리에서 읽는다(`ZipFile`, 항목 이름은 정규식으로 검사하고 맞지 않는 항목이 있으면 오류).

## 변환 방법

1. 각 WAV 의 왼쪽/오른쪽 귀 임펄스 응답(128탭)을 +-1 로 맞춘다.
2. 귀마다 **최소 위상 분해**(실수 켑스트럼, 16384점 FFT. 1024점이나 4096점은 깊은 노치에서 켑스트럼 겹침으로 오차가 커서(1024점 11.7 dB, 4096점 0.12 dB) 쓰지 않는다):
   `log|H|` 의 켑스트럼을 접어 최소 위상 스펙트럼을 만들고 128탭으로 자른다. 크기 응답은 원래 응답과 같다.
3. 귀마다 **지연**(샘플, 44100 Hz 기준): 최소 위상 스펙트럼을 `d` 샘플 지연한 것이 150 Hz ~ 1200 Hz 에서 측정한 스펙트럼에 복소수 최소 제곱 의미로 가장 가까운 `d`
   (`sum Re(H conj(H_min) e^(j w d))` 를 1/4 샘플 격자와 삼분 탐색으로 최대화). 처음에 쓴 초과 위상의 군지연 평균(0 ~ 1378 Hz, 또는 0 ~ 600 Hz)은 두 귀의 저주파 시간차가
   원본과 2~7 샘플 어긋나서(원본 응답을 1200 Hz 로 저역 통과해 상호 상관으로 잰 시차와 비교) 버렸다. 최소 제곱 맞춤은 368 방향 모두에서 0.93 샘플(21 us) 이내이다.
4. 모든 측정과 귀에 공통인 최소 지연(측정계의 전파 지연)을 뺀다. 남은 지연은 0 이상이다.
5. 아래 형식으로 쓴다.

## 출력 파일 형식 (`SWPHRTF1`, 빅 엔디언)

| 내용 | 형식 |
| --- | --- |
| 매직 | ASCII `SWPHRTF1` (8바이트) |
| 버전 | int, 1 |
| 샘플링 주파수 | int, 44100 |
| 탭 수 | int, 128 |
| 고도 수 | int |
| 고도마다 | short 고도(도), short 방위각 수 n, short x n 방위각(도, 오름차순) |
| 측정마다(고도 오름차순, 방위각 오름차순) | float 왼쪽 귀 지연, float 오른쪽 귀 지연(샘플), float x 128 왼쪽 최소 위상 커널, float x 128 오른쪽 커널 |
| 출처 표기 | int 길이, UTF-8 문장(저자 표기, 출처 URL, 조회 날짜, 원본 CRC-32) |

방위각은 오른쪽이 양수(0 정면, 90 오른쪽 옆, 180 뒤). 왼쪽 위치는 좌우를 바꿔 얻는다.

## 제약

- Java 6 문법/API 만 사용. 실제로 실행해 본 JRE: JDK 6u45 `1.6.0_45`(sh).
- 도구의 FFT 는 도구 안에 자체 구현이다(제품 `dsp.Fft` 에 의존하지 않는다).
- 원본 압축 파일은 저장소에 넣지 않는다(시험용 사본은 `browser/src/test/resources/kemar/compact.zip`, 시험이 원본 HRIR 과 비교하는 데 쓴다. 같은 이용 조건과 저자 표기).
