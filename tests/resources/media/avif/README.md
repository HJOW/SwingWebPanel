# AVIF / AV1 시험 자료 / AVIF and AV1 test inputs

이 디렉터리의 파일은 `tests/src/org/duckdns/hjow/swingwebpanel/media/image/avif` 의 시험이 쓴다(계획 P18-03). 모두 작고(합계 약 190 KB), 외부에서 받아 온 파일이 아니라
`generation/make_fixtures.py` 가 합성 그림으로 만든 것이다. 라이선스 문제가 되는 사진은 쓰지 않았다.

## 구성

| 경로 | 내용 |
| --- | --- |
| `av1/*.obu` | 인코더가 만든 AV1 키 프레임(시간 단위 하나). 정지 화면이 쓸 수 있는 부호화 도구를 골고루 쓰도록 옵션을 달리했다. |
| `av1/index.txt` | 스트림마다 크기, 비트 깊이, 평면 수, 하위 표본화, **libdav1d 가 디코딩한 평면의 md5**, 헤더가 알리는 도구 목록. |
| `avif/*.avif` | Pillow(libavif 1.4.2)가 쓴 AVIF 파일: 4:2:0/4:2:2/4:4:4/4:0:0, 무손실(항등 행렬), 알파. |
| `avif/*.png` | 같은 AVIF 파일을 Pillow(libavif + dav1d)가 RGBA 로 디코딩한 결과. 색 변환의 기준이다. |
| `avif/index.txt` | AVIF 파일 목록과 설명. |
| `seq/*.obu`, `seq/index.txt` | 인터 프레임이 있는 AV1 시퀀스 39개(libaom, rav1e, SVT-AV1)와 프레임별 크기·md5(libdav1d). `Av1SequenceTest` 가 비트 단위로 비교한다. `generation/make_seq_fixtures.py` |
| `avis/*.avif`, `avis/index.txt` | 위 시퀀스를 ffmpeg AVIF 먹서로 감싼 애니메이션 AVIF 11개(알파 트랙, 편집 목록 반복, 기본 항목 없음, 크기 바뀌는 시퀀스). 프레임 수·크기·지속 시간·재생 횟수는 스크립트가 컨테이너에서 읽어 색인에 적는다. `generation/make_avis_fixtures.py` |
| `layers/*` | 공간 계층 두 개짜리 AV1 스트림과 `a1op`/`lsel` 속성을 단 AVIF 항목 파일, 계층 시퀀스. 인코더 출력의 키 프레임을 조립한 것이다(공간 계층을 만드는 인코더가 없다). `generation/make_layer_fixtures.py` |

## 기준 디코더와 비교 방식

- AV1 스트림은 libdav1d(ffmpeg 에 들어 있는 것)가 디코딩한 `rawvideo` 평면의 md5 와 **비트 단위로** 같아야 한다.
  8비트는 바이트, 10/12비트는 16비트 리틀 엔디언 값으로 평면(Y, U, V)과 행 순서대로 해시한다.
- AVIF 파일은 libavif 의 디코딩 결과(PNG)와 비교한다. YUV→RGB 변환과 색차 보간의 반올림 방식이 달라 완전히 같지는 않으므로 평균 차이와 큰 차이의
  비율에 한계를 둔다(시험 코드의 `testLibavifFilesMatchLibavif` 참조).
- 시퀀스(`seq/`)는 프레임마다 md5 를 적고(`WxH:md5` 목록), 모든 프레임이 libdav1d 의 디코딩과 비트 단위로 같아야 한다. 도구(인트라 블록 복사, 초해상도, 필름 그레인, 인터 예측의 각 도구)는
  `Av1ToolStats` 카운터로 실제 사용을 확인한다.
- 알려진 빈 곳: `enable_dual_filter=1` 을 쓰는 스트림은 사용 가능한 인코더가 만들지 못해 없다(`docs/progress/media-matrix.md` 10.5).

## 만든 환경 (2026-11-16)

- ffmpeg N-125875-g5d4d3bdc61-20260731 (libaom-av1, librav1e, libsvtav1 인코더, libdav1d 디코더), SVT-AV1 v4.1.0-279-gd3c4cb394.
- Python 3.13, Pillow 12.3.0 (libavif 1.4.2), numpy.
- 같은 인코더 버전에서는 같은 파일이 만들어지지만 다른 버전에서는 바이트가 다를 수 있다. 그래서 파일 자체를 저장소에 두며, 다시 만들면 `av1/index.txt` 의 md5 도
  함께 바뀐다.

다시 만들려면:

```
python tests/resources/media/avif/generation/make_fixtures.py <ffmpeg 실행 파일> <이 디렉터리의 절대 경로>
```

## 시험이 확인하는 부호화 도구

헤더가 알리는 도구는 `av1/index.txt` 의 마지막 열에 있다. 128x128 슈퍼블록, 화면 콘텐츠 도구(팔레트), 양자화 행렬, 분할, 델타 양자화와 델타 루프 필터,
타일, 축소된 변환 집합, CDEF, 루프 복원(위너/자기 유도/전환), 블록 경계 제거 필터, 비트 깊이 8/10/12, 색차 형식 4:0:0/4:2:0/4:2:2/4:4:4 를 포함한다
(`Av1StreamTest.testFixturesCoverTheTools`). 필터 인트라, 휘도로부터의 색차 예측, 모든 변환 종류와 크기는 헤더가 아니라 타일 데이터에서 쓰이며 스트림 전체의
md5 일치로 확인된다.
