# hrtfgen - HRTF 자료 변환 도구

MIT KEMAR 더미 헤드의 HRTF(머리전달함수) 측정 자료를 SwingWebPanel 의 Web Audio `PannerNode`(HRTF 패닝)가 읽는 자원 파일로 바꾸는 개발 보조 도구입니다.
제품(SwingWebPanel)에 포함되지 않으며, 자원 파일을 다시 만들어야 할 때만 사용합니다. 생성한 자원 파일은 이미 저장소에 들어 있습니다
(`browser/src/main/resources/org/duckdns/hjow/swingwebpanel/media/webaudio/dsp/kemar-compact.hrtf`).

## 원본 자료와 저자 표기

HRTF Measurements of a KEMAR Dummy-Head Microphone, **Bill Gardner and Keith Martin**, MIT Media Lab Machine Listening Group, 1994.
https://sound.media.mit.edu/resources/KEMAR.html (`compact.zip`, 2026-11-22 조회)

이 자료의 저작권은 MIT Media Laboratory 에 있으며(Copyright 1994), "저자를 표기하면 연구와 상업 용도 모두 제한 없이 자유롭게 쓸 수 있다"는 조건으로 공개되어 있습니다.
SwingWebPanel 의 HRTF 패닝을 사용하는 응용 프로그램은 이 저자 표기를 유지하세요.

## 준비

JDK 6 이 필요합니다. 기본 경로는 `C:\Program Files\Java\jdk1.6.0_45` 이며, 다른 위치라면 환경 변수 `JAVA6_HOME` 을 지정하세요.
JDK 6 을 찾지 못하면 오류로 종료합니다.

```
tools\hrtfgen\build.bat      (Windows)
tools/hrtfgen/build.sh       (sh)
```

## 사용법

원본 `compact.zip` 은 `tools/webfetch` 로 내려받을 수 있습니다(HTTPS 이므로 새 JRE 로 실행: `WEBFETCH_JAVA_HOME`).

```
tools\hrtfgen\run.bat compact.zip kemar-compact.hrtf --retrieved 2026-11-22
```

| 종료 코드 | 의미 |
| --- | --- |
| 0 | 성공 |
| 1 | 사용법 오류 |
| 2 | 압축 파일을 읽지 못함, 형식이 기대와 다름, 출력 파일을 쓰지 못함 |

실행하면 측정 개수, 제거한 공통 지연, 변환 오차를 표준 오류에 출력합니다. 같은 입력이면 항상 같은 파일이 만들어집니다(약 40초).

## 하는 일

각 방향(고도 -40~90도, 방위각 0~180도, 368곳)의 왼쪽/오른쪽 귀 임펄스 응답(128탭, 44100 Hz)을 **최소 위상 커널**과 **지연**으로 나눠 한 파일에 담습니다.
엔진은 이 커널들을 방향 사이에서 보간하고 지연을 다시 적용해 소리를 입체화합니다. 자세한 방법과 파일 형식은 `AGENTS.md` 를 참고하세요.
