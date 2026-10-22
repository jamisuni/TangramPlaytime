#!/usr/bin/env python3
"""
Make the game's five sound effects (WO-007, seam S-52).

Writes these five files, and no other, under app/src/main/res/raw/:

    sfx_pick_up.wav  sfx_turn.wav  sfx_lock.wav  sfx_home.wav  sfx_solve.wav

Each is a 16-bit mono PCM WAV at 22 050 Hz. The recipes are the ones of
decisions.md 2026-10-21 "The five sounds and the tick.", taken from DI-1's
`sfx` and `tone()` (Spec/prototype/tangram-prototype.html, a behaviour
reference only):

    pick-up  sine, 520 -> 700 Hz, 70 ms
    turn     square, 760 Hz, 40 ms
    lock     triangle 1320 Hz (50 ms) + sine 1760 Hz (60 ms), 60 ms
    return   sine, 300 -> 180 Hz, 160 ms, soft and low
    solve    four rising triangle notes (523, 659, 784, 1047 Hz), each
             240 ms, 110 ms apart: 570 ms

As in DI-1's tone(), each tone starts at its gain and falls exponentially
to 0.0001 at its end, and a slide moves the frequency exponentially. The
square and triangle waves are band-limited (odd harmonics below the Nyquist
frequency, scaled to peak 1), as Web Audio's built-in waves are. Each tone
fades in and out over EDGE_MS, so no file starts or ends with a click.

Levels: the tones keep DI-1's gains, except the return's (see RETURN_GAIN),
and one factor for all five files puts the loudest peak at LOUDEST_PEAK.

Deterministic: Python standard library only, no randomness, no time stamp
in the files. A second run writes the same bytes (architecture.md G-06:
change a recipe here and run the tool again).

Before it writes anything the tool checks REQ-033 rules 1-2: each file
lasts at most 1 s, no sample clips, and the return is the softest of the
five (peak and RMS) and the lowest (its highest frequency is below every
other sound's lowest). A failed check writes no file and exits 1.

Run from anywhere:  python tools/make_sounds.py
"""

import math
import os
import struct
import sys
import wave

RATE = 22050            # samples per second (S-52)
FULL_SCALE = 32767      # the largest 16-bit sample
MAX_MS = 1000           # REQ-033 rule 1: each effect lasts at most 1 s
FLOOR = 0.0001          # DI-1's exponential gain ramp ends here
EDGE_MS = 1             # each tone fades in and out over this long
LOUDEST_PEAK = 0.5      # the loudest file peaks here (-6 dBFS), leaving headroom when sounds overlap
RETURN_GAIN = 0.015     # DI-1 plays the return at 0.05; below the turn's 0.025 it is the softest of the five
PERIOD_POINTS = 4096    # points per period when a band-limited wave is scaled to peak 1

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "raw")
OUT_DIR_SHOWN = "app/src/main/res/raw"
RETURN_FILE = "sfx_home.wav"


def samples(ms):
    """Milliseconds to a whole number of samples, rounded half up, in integer arithmetic."""
    return (ms * RATE + 500) // 1000


class Tone:
    """One DI-1 tone(): a wave shape at a gain, from f0 Hz (sliding to f1 Hz), from start_ms for dur_ms."""

    def __init__(self, shape, f0, dur_ms, gain, f1=None, start_ms=0):
        self.shape = shape
        self.f0 = f0
        self.f1 = f1
        self.dur_ms = dur_ms
        self.gain = gain
        self.start_ms = start_ms

    def lowest(self):
        return min(self.f0, self.f1 if self.f1 is not None else self.f0)

    def highest(self):
        return max(self.f0, self.f1 if self.f1 is not None else self.f0)


# (file, sound, length in ms, tones): decisions.md 2026-10-21 "The five sounds and the tick."
SOUNDS = (
    ("sfx_pick_up.wav", "pick-up", 70, (
        Tone("sine", 520, 70, 0.06, f1=700),
    )),
    ("sfx_turn.wav", "turn", 40, (
        Tone("square", 760, 40, 0.025),
    )),
    ("sfx_lock.wav", "lock", 60, (
        Tone("triangle", 1320, 50, 0.08),
        Tone("sine", 1760, 60, 0.04),
    )),
    ("sfx_home.wav", "return", 160, (
        Tone("sine", 300, 160, RETURN_GAIN, f1=180),
    )),
    ("sfx_solve.wav", "solve", 570, tuple(
        Tone("triangle", f, 240, 0.07, start_ms=110 * i)
        for i, f in enumerate((523, 659, 784, 1047))
    )),
)


def harmonics(shape, f_max):
    """(n, amplitude) pairs of the band-limited wave: odd n with n * f_max below Nyquist, peak 1."""
    if shape == "sine":
        return ((1, 1.0),)
    parts = []
    n = 1
    while n * f_max < RATE / 2:
        if shape == "square":
            parts.append((n, 4.0 / (math.pi * n)))
        elif shape == "triangle":
            sign = 1.0 if (n // 2) % 2 == 0 else -1.0
            parts.append((n, sign * 8.0 / (math.pi * math.pi * n * n)))
        else:
            raise ValueError("unknown wave shape: " + shape)
        n += 2
    peak = max(
        abs(sum(a * math.sin(k * 2.0 * math.pi * i / PERIOD_POINTS) for k, a in parts))
        for i in range(PERIOD_POINTS)
    )
    return tuple((k, a / peak) for k, a in parts)


def render(tone):
    """The tone's samples, as floats at full scale 1.0."""
    n = samples(tone.dur_ms)
    length = n / RATE
    parts = harmonics(tone.shape, tone.highest())
    edge = samples(EDGE_MS)
    decay = math.log(FLOOR / tone.gain)
    if tone.f1 is not None:
        ratio = tone.f1 / tone.f0
        log_ratio = math.log(ratio)
    out = []
    for j in range(n):
        t = j / RATE
        if tone.f1 is None:
            cycles = tone.f0 * t
        else:
            cycles = tone.f0 * length * (ratio ** (t / length) - 1.0) / log_ratio
        phase = 2.0 * math.pi * cycles
        value = sum(a * math.sin(k * phase) for k, a in parts)
        level = tone.gain * math.exp(decay * t / length)
        if j < edge:
            level *= j / edge
        if n - 1 - j < edge:
            level *= (n - 1 - j) / edge
        out.append(value * level)
    return out


def mix(name, length_ms, tones):
    """The file's samples: its tones added at their start times."""
    n = samples(length_ms)
    buf = [0.0] * n
    end = 0
    for tone in tones:
        start = samples(tone.start_ms)
        part = render(tone)
        if start + len(part) > n:
            fail("%s: a tone ends after the file's %d ms" % (name, length_ms))
        for j, v in enumerate(part):
            buf[start + j] += v
        end = max(end, start + len(part))
    if end != n:
        fail("%s: the tones end at sample %d, not at the file's %d ms (%d)" % (name, end, length_ms, n))
    return buf


def rms(values):
    return math.sqrt(sum(v * v for v in values) / len(values))


def dbfs(value):
    return 20.0 * math.log10(value / FULL_SCALE) if value > 0 else float("-inf")


def fail(message):
    sys.stderr.write("make_sounds: " + message + "; no file written\n")
    sys.exit(1)


def build():
    """Every file's 16-bit samples, checked against REQ-033 rules 1-2: [(file, sound, length_ms, tones, pcm)]."""
    raw = [(name, sound, length_ms, tones, mix(name, length_ms, tones))
           for name, sound, length_ms, tones in SOUNDS]
    loudest = max(max(abs(v) for v in buf) for _, _, _, _, buf in raw)
    scale = LOUDEST_PEAK * FULL_SCALE / loudest
    made = [(name, sound, length_ms, tones, [int(math.floor(v * scale + 0.5)) for v in buf])
            for name, sound, length_ms, tones, buf in raw]

    for name, _, length_ms, _, pcm in made:
        if len(pcm) > samples(MAX_MS):
            fail("%s lasts %d ms; REQ-033 rule 1 allows at most %d ms" % (name, length_ms, MAX_MS))
        if max(abs(v) for v in pcm) >= FULL_SCALE:
            fail("%s clips" % name)

    ret = [m for m in made if m[0] == RETURN_FILE][0]
    others = [m for m in made if m[0] != RETURN_FILE]
    ret_peak = max(abs(v) for v in ret[4])
    ret_rms = rms(ret[4])
    ret_high = max(t.highest() for t in ret[3])
    for name, _, _, tones, pcm in others:
        if ret_peak >= max(abs(v) for v in pcm):
            fail("the return's peak is not below %s's (REQ-033 rule 2: soft)" % name)
        if ret_rms >= rms(pcm):
            fail("the return's RMS is not below %s's (REQ-033 rule 2: soft)" % name)
        if ret_high >= min(t.lowest() for t in tones):
            fail("the return is not lower than %s (REQ-033 rule 2: low)" % name)
    return made


def write(name, pcm):
    with wave.open(os.path.join(OUT_DIR, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(struct.pack("<%dh" % len(pcm), *pcm))


def main():
    made = build()
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, sound, _, _, pcm in made:
        write(name, pcm)
        size = os.path.getsize(os.path.join(OUT_DIR, name))
        print("%-16s %-8s %6d samples %7.2f ms  peak %6.1f dBFS  rms %6.1f dBFS  %6d bytes" % (
            name, sound, len(pcm), 1000.0 * len(pcm) / RATE,
            dbfs(max(abs(v) for v in pcm)), dbfs(rms(pcm)), size))
    print("%d sounds written to %s" % (len(made), OUT_DIR_SHOWN))


if __name__ == "__main__":
    main()
