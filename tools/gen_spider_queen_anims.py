"""Generates spider_queen.animation.json (GeckoLib/Bedrock animation format).

Generated rather than hand-authored because the 8 legs + 2 fangs are all sign-mirrored pairs -
one gait table here produces consistent left/right keyframes instead of 20 hand-typed bone
blocks that can silently drift. Animation names/bone names must keep matching
SpiderQueenEntity's RawAnimation constants and the geo.json bone names.

Sign conventions (verified against GeckoLib 5.4.5 source - BakedAnimationsAdapter maps rotation
keyframes (x,y,z) -> internal (-x,-y,+z) radians, identical to geometry bone rotations; position
keyframes -> internal (-x,+y,+z) model units; render applies base+anim rotation in Z*Y*X order):
- rot x > 0: pitches a bone front-down / rear-up ("nose down").
- rot y > 0: swings a RIGHT-side leg's tip FORWARD, a LEFT-side leg's tip backward.
- rot z > 0: swings a RIGHT-side leg's tip DOWN, a LEFT-side leg's tip UP; on the hanging
  fangs, tips toward +X (the entity's left).
- position y > 0 is up, z > 0 is backward (toward the abdomen).
Verify poses with tools/render_geckolib_preview.py (--anim <name> --times ...) - it
implements these exact conventions, so what it shows is what GeckoLib renders.

Run from the repo root: `python tools/gen_spider_queen_anims.py` writes
spider_queen.animation.json to the CURRENT working directory - copy it to
src/main/resources/assets/baum2/geckolib/animations/entity/spider_queen.animation.json.

Timing constraint: leap_windup's length (0.75s) must stay equal to
SpiderQueenEntity.LEAP_WINDUP_DURATION_TICKS (15 ticks) so the telegraph ends exactly at launch.
"""
import json

RIGHT_LEGS = ["right_front_leg", "right_middle_front_leg", "right_middle_hind_leg", "right_hind_leg"]
LEFT_LEGS = ["left_front_leg", "left_middle_front_leg", "left_middle_hind_leg", "left_hind_leg"]
ALL_LEGS = RIGHT_LEGS + LEFT_LEGS
BODY_BONES = ["body0", "body1", "head"]

# Alternating-tetrapod gait groups (real spider-like): diagonal sets swing together.
GROUP_A = ["right_front_leg", "left_middle_front_leg", "right_middle_hind_leg", "left_hind_leg"]

# Per-leg yaw swing amplitude (front legs reach, hind legs push)
YAW_AMP = {"front": 16, "middle_front": 11, "middle_hind": 10, "hind": 9}


def amp(leg):
    for key in ("middle_front", "middle_hind", "front", "hind"):
        if key in leg:
            return YAW_AMP[key]
    raise ValueError(leg)


def side(leg):
    return 1 if leg.startswith("right") else -1


def rot(**frames):
    return {"rotation": dict(frames)}


def merge(*channel_dicts):
    out = {}
    for d in channel_dicts:
        out.update(d)
    return out


def pos(**frames):
    return {"position": dict(frames)}


anims = {}

# ---------------------------------------------------------------------------
# idle: breathing bob, abdomen pulse, slow head scan, creepy fang chew, one impatient
# front-leg tap - a boss that visibly seethes while standing still.
# ---------------------------------------------------------------------------
idle_bones = {}
for b in BODY_BONES:
    idle_bones[b] = pos(**{"0": [0, 0, 0], "1.6": [0, 0.45, 0], "3.2": [0, 0, 0]})
idle_bones["body1"] = merge(
    pos(**{"0": [0, 0, 0], "1.6": [0, 0.6, 0], "3.2": [0, 0, 0]}),
    rot(**{"0": [0, 0, 0], "1.6": [2.5, 0, 0], "3.2": [0, 0, 0]}),
)
idle_bones["head"] = merge(
    idle_bones["head"],
    rot(**{"0": [1, 0, 0], "0.8": [0, 4, 0], "1.6": [-1.5, 0, 0], "2.4": [0, -4, 0], "3.2": [1, 0, 0]}),
)
idle_bones["left_fang"] = rot(**{"0": [0, 0, 0], "0.7": [-4, 0, 6], "1.05": [0, 0, -2], "1.4": [0, 0, 0],
                                 "2.3": [-3, 0, 4], "2.45": [0, 0, 0], "2.6": [-3, 0, 4], "2.8": [0, 0, 0]})
idle_bones["right_fang"] = rot(**{"0": [0, 0, 0], "0.7": [-4, 0, -6], "1.05": [0, 0, 2], "1.4": [0, 0, 0],
                                  "2.3": [-3, 0, -4], "2.45": [0, 0, 0], "2.6": [-3, 0, -4], "2.8": [0, 0, 0]})
# single impatient tap of the right front leg
idle_bones["right_front_leg"] = rot(**{"0": [0, 0, 0], "1.9": [0, 0, 0], "2.02": [0, 6, -9],
                                       "2.14": [0, 2, -1], "2.26": [0, 0, 0]})
anims["animation.spider_queen.idle"] = {"loop": True, "animation_length": 3.2, "bones": idle_bones}

# ---------------------------------------------------------------------------
# walk: alternating-tetrapod gait, 0.6s cycle - swing legs lift + reach while stance legs push.
# ---------------------------------------------------------------------------
walk_bones = {}
for leg in ALL_LEGS:
    a = amp(leg) * side(leg)
    in_a = leg in GROUP_A
    # phase F: group A swings forward 0->0.3 (airborne), pushes back 0.3->0.6 (grounded)
    y0, y1 = (-a, a) if in_a else (a, -a)
    yaw = {"0": [0, y0, 0], "0.3": [0, y1, 0], "0.6": [0, y0, 0]}
    lift_t = "0.15" if in_a else "0.45"
    lift = -7 * side(leg)  # tip up during the swing phase
    frames = dict(yaw)
    # the lift keyframe sits mid-swing; restate the yaw midpoint (0, by linearity) there so
    # inserting the keyframe doesn't distort the yaw channel
    frames[lift_t] = [0, 0, lift]
    walk_bones[leg] = {"rotation": frames}
for b, phase in (("body0", 0), ("body1", 1), ("head", 0)):
    bob = {"0": [0, 0.18, 0], "0.15": [0, -0.14, 0], "0.3": [0, 0.18, 0],
           "0.45": [0, -0.14, 0], "0.6": [0, 0.18, 0]}
    walk_bones[b] = {"position": bob}
walk_bones["body1"] = merge(walk_bones["body1"],
                            rot(**{"0": [0, 0, 1.5], "0.3": [0, 0, -1.5], "0.6": [0, 0, 1.5]}))
walk_bones["left_fang"] = rot(**{"0": [-2, 0, 0], "0.3": [2, 0, 0], "0.6": [-2, 0, 0]})
walk_bones["right_fang"] = rot(**{"0": [2, 0, 0], "0.3": [-2, 0, 0], "0.6": [2, 0, 0]})
anims["animation.spider_queen.walk"] = {"loop": True, "animation_length": 0.6, "bones": walk_bones}

# ---------------------------------------------------------------------------
# leap_windup (0.75s = 15 ticks, matches LEAP_WINDUP_DURATION_TICKS): crouch + coil + threat
# pose - body sinks, abdomen cocks up, face tilts onto the target, front legs raise high and
# spread, fangs flare, then a final recoil right before launch.
# ---------------------------------------------------------------------------
CROUCH = -2.2
wind_bones = {
    "body0": merge(pos(**{"0": [0, 0, 0], "0.35": [0, CROUCH, 0], "0.62": [0, CROUCH, 0], "0.75": [0, CROUCH, 0.9]}),
                   rot(**{"0": [0, 0, 0], "0.35": [4, 0, 0], "0.75": [6, 0, 0]})),
    "body1": merge(pos(**{"0": [0, 0, 0], "0.35": [0, CROUCH, 0], "0.62": [0, CROUCH, 0], "0.75": [0, CROUCH, 0.9]}),
                   rot(**{"0": [0, 0, 0], "0.35": [12, 0, 0], "0.75": [14, 0, 0]})),
    "head": merge(pos(**{"0": [0, 0, 0], "0.35": [0, CROUCH + 0.4, 0], "0.5": [0, CROUCH + 0.25, 0],
                         "0.62": [0, CROUCH + 0.4, 0], "0.75": [0, CROUCH + 0.4, 0.9]}),
                  rot(**{"0": [0, 0, 0], "0.35": [-10, 0, 0], "0.75": [-14, 0, 0]})),
    "left_fang": rot(**{"0": [0, 0, 0], "0.35": [-12, 0, 18], "0.62": [-12, 0, 18], "0.75": [-16, 0, 24]}),
    "right_fang": rot(**{"0": [0, 0, 0], "0.35": [-12, 0, -18], "0.62": [-12, 0, -18], "0.75": [-16, 0, -24]}),
}
# tremor: tiny vertical jitter while holding the crouch
for b in ("body0", "body1"):
    p = wind_bones[b]["position"]
    p["0.42"] = [0, CROUCH + 0.12, 0]
    p["0.5"] = [0, CROUCH, 0]
    p["0.58"] = [0, CROUCH + 0.12, 0]
for leg in ALL_LEGS:
    s = side(leg)
    if "front_leg" in leg and "middle" not in leg:
        # raised threat pose, quivering, sweeping wider just before launch
        wind_bones[leg] = merge(
            rot(**{"0": [0, 0, 0], "0.35": [0, 18 * s, -70 * s], "0.45": [0, 18 * s, -66 * s],
                   "0.55": [0, 19 * s, -73 * s], "0.62": [0, 18 * s, -70 * s], "0.75": [0, 24 * s, -75 * s]}),
            pos(**{"0": [0, 0, 0], "0.35": [0, -1.0, 0], "0.75": [0, -1.0, 0]}),
        )
    else:
        # planted legs: follow the body down, flatten so the tips stay grounded, coil slightly
        coil = 8 if "hind" in leg and "middle" not in leg else 4
        wind_bones[leg] = merge(
            rot(**{"0": [0, 0, 0], "0.35": [0, coil * s, -13 * s], "0.75": [0, coil * s, -13 * s]}),
            pos(**{"0": [0, 0, 0], "0.35": [0, CROUCH, 0], "0.75": [0, CROUCH, 0]}),
        )
anims["animation.spider_queen.leap_windup"] = {"loop": False, "animation_length": 0.75, "bones": wind_bones}

# ---------------------------------------------------------------------------
# leap_flight: launch snap - nose up, all legs swept back and trailing; then the front two
# pairs whip forward mid-flight to grab. Holds the final frame until landing.
# ---------------------------------------------------------------------------
flight_bones = {
    "body0": rot(**{"0": [-10, 0, 0], "0.35": [-10, 0, 0]}),
    "body1": rot(**{"0": [-6, 0, 0], "0.35": [-6, 0, 0]}),
    "head": rot(**{"0": [-6, 0, 0], "0.35": [-6, 0, 0]}),
    "left_fang": rot(**{"0": [-14, 0, 22], "0.35": [-14, 0, 22]}),
    "right_fang": rot(**{"0": [-14, 0, -22], "0.35": [-14, 0, -22]}),
}
for leg in ALL_LEGS:
    s = side(leg)
    swept = [0, -20 * s, -36 * s]  # trailing: swept back, lifted toward the body plane
    if "front_leg" in leg and "middle" not in leg:
        flight_bones[leg] = rot(**{"0": swept, "0.18": swept, "0.3": [0, 32 * s, -48 * s],
                                   "0.35": [0, 32 * s, -48 * s]})
    elif "middle_front" in leg:
        flight_bones[leg] = rot(**{"0": swept, "0.18": swept, "0.3": [0, 14 * s, -38 * s],
                                   "0.35": [0, 14 * s, -38 * s]})
    else:
        flight_bones[leg] = rot(**{"0": swept, "0.35": swept})
anims["animation.spider_queen.leap_flight"] = {"loop": "hold_on_last_frame", "animation_length": 0.35,
                                               "bones": flight_bones}

doc = {"format_version": "1.8.0", "animations": anims}
with open("spider_queen.animation.json", "w", encoding="utf-8") as f:
    json.dump(doc, f, indent=2)
print("wrote spider_queen.animation.json:", ", ".join(a.split(".")[-1] for a in anims))
