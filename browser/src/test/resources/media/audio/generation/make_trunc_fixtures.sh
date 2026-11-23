#!/bin/sh
# Longer files for the tests of the lenient handling of cut-off and damaged data (plan item P25-05, decodeAudioData): the fixtures of the
# other tests are 0.1 to 0.4 seconds long, which is too short for an Ogg file to have more than a page or two, so a copy that is cut off
# has no complete page left. These files are 2 seconds long, the Ogg files with a page every 0.1 seconds, and are cut by the test itself. Synthetic signal:
# a stereo sweep with a quiet 1760 Hz tone on the right channel, so no copyrighted audio.
# Usage: sh make_trunc_fixtures.sh <ffmpeg> <audio fixture directory (tests/resources/media/audio)>
# Made with: ffmpeg N-125875-g5d4d3bdc61-20260731 (2026-11-22), libvorbis, libopus, libmp3lame, native aac and flac encoders.
FF="$1"
DIR="$2"
OUT="$DIR/trunc"
mkdir -p "$OUT"
SRC="aevalsrc=0.4*sin(2*PI*(220+300*t)*t)|0.3*sin(2*PI*330*t)+0.05*sin(2*PI*1760*t):s=44100:d=2"
"$FF" -v error -y -f lavfi -i "$SRC" -c:a libvorbis -q:a 3 -page_duration 100000 "$OUT/long_vorbis_44100_st.ogg"
"$FF" -v error -y -f lavfi -i "$SRC" -ar 48000 -c:a libopus -b:a 64k -page_duration 100000 "$OUT/long_opus_48000_st.opus"
"$FF" -v error -y -f lavfi -i "$SRC" -ar 8000 -sample_fmt s16 -c:a flac -compression_level 5 "$OUT/long_flac_8000_st.flac"
"$FF" -v error -y -f lavfi -i "$SRC" -ar 8000 -sample_fmt s16 -c:a flac -compression_level 5 -f ogg -page_duration 100000 "$OUT/long_oggflac_8000_st.oga"
"$FF" -v error -y -f lavfi -i "$SRC" -c:a libmp3lame -b:a 96k "$OUT/long_mp3_44100_st.mp3"
"$FF" -v error -y -f lavfi -i "$SRC" -c:a aac -b:a 96k -f adts "$OUT/long_aac_44100_st.aac"
