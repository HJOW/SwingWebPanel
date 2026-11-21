#!/bin/sh
# Float references of the wide decoding path (plan item P25-05): decodes a few lossy fixtures with ffmpeg to 32 bit float
# (the native decoders of ffmpeg; libopus for Opus) and keeps the first 1500 frames of every file, interleaved, in the native
# channel order of ffmpeg. Used by WideAccuracyTest: the float output of the product must be at least as close to these floats
# as the product's own 16 bit output is.
# Usage: sh make_wide_refs.sh <ffmpeg> <audio fixture directory (tests/resources/media/audio)>
# Made with: ffmpeg N-125875-g5d4d3bdc61-20260731 (2026-11-22).
FF="$1"
DIR="$2"
FRAMES=1500
mkdir -p "$DIR/wide"
ref() {
  name="$1"; file="$2"; channels="$3"; decoder="$4"
  bytes=$((FRAMES * channels * 4))
  if [ -n "$decoder" ]; then
    "$FF" -v error -y -c:a "$decoder" -i "$DIR/$file" -f f32le -acodec pcm_f32le - | head -c "$bytes" > "$DIR/wide/$name.f32"
  else
    "$FF" -v error -y -i "$DIR/$file" -f f32le -acodec pcm_f32le - | head -c "$bytes" > "$DIR/wide/$name.f32"
  fi
}
ref mp3_cbr128_44100_js mp3/mp3_cbr128_44100_js.mp3 2 ""
ref mp3_v2_32000_mono mp3/mp3_v2_32000_mono.mp3 1 ""
ref aac_adts_44100_st_128k aac/aac_adts_44100_st_128k.aac 2 ""
ref aac_m4a_48000_mono_64k aac/aac_m4a_48000_mono_64k.m4a 1 ""
ref aac_adts_16000_6ch aac/aac_adts_16000_6ch.aac 6 ""
ref aac_m4a_16000_8ch aac/aac_m4a_16000_8ch.m4a 8 ""
ref vorbis_q6_48000_webm webm/vorbis_q6_48000_webm.webm 2 ""
ref opus_audio_128k_48000_st opus/opus_audio_128k_48000_st.opus 2 libopus
ref opus_voip_40k_48000_mono opus/opus_voip_40k_48000_mono.opus 1 libopus
ref opus_audio_96k_48000_51 opus/opus_audio_96k_48000_51.opus 6 libopus
