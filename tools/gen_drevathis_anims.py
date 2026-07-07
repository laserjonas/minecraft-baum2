"""Generates drevathis.animation.json (GeckoLib/Bedrock animation format).

Same pipeline as tools/gen_zombie_colossus_anims.py. Animation names/bone names must keep
matching DrevathisEntity's RawAnimation constants and drevathis.geo.json's bone names (body,
head, right_arm/right_forearm/blade, left_arm/left_forearm, right_leg, left_leg, tail,
tail_tip - head/forearms/blade/tail_tip are CHILD bones, keyframes are local, layered on the
parent's).

Sign conventions (verified GeckoLib mapping, see the spider/colossus scripts):
- rot x > 0 pitches a bone front-down; on a hanging limb it swings the tip BACKWARD, so
  forward swings are negative x.
- rot z > 0 swings a hanging limb's tip toward +X = the entity's LEFT; "outward" is negative z
  for right limbs, positive z for left limbs.
- rot y on the backward-extending tail swings its tip sideways.
- position y > 0 is up, z > 0 is backward.

Blade keyframe note: the blade bone's BASE rotation in the geometry is [24, 0, 14] (carried
tip-up-outward at rest). Animation keyframes below are LOCAL OFFSETS layered on that base.
GOTCHA (found in preview iteration): "canceling" the base does NOT point the blade along a
raised arm - the blade is authored tip-UP from the fist, so with the arm rotated near 180 the
canceled blade dangles down behind the back. To keep the blade tip pointing world-up under a
parent arm rotation A, the blade's local x must be ~(-A), minus the 24 base (see BLADE_SKY).

Server-timing contracts (keep these in lockstep with DrevathisEntity):
- throw_wave: 0.6s one-shot; the DARK WAVE PROJECTILE spawns 6 ticks (0.30s) after the
  trigger, exactly on the palm-thrust frame.
- curse_ground: 1.6s one-shot; CurseGroundGoal activates the cursed zone at tick 20 (1.00s),
  exactly on the downward slash frame.
- stampede_run: 0.5s loop, state-driven while the STAMPEDE_ACTIVE TrackedData is true.
- end_channel: 5.0s (= the goal's 100-tick channel), hold_on_last_frame; the terror burst
  fires at the very end of the channel as the arms collapse inward.

Run from the repo root, preview each pose with e.g.
  python tools/render_geckolib_preview.py --model drevathis --anim curse_ground --times 0.5,1.0,1.3
then copy to src/main/resources/assets/baum2/geckolib/animations/entity/drevathis.animation.json
"""
import json

# baseline posture: a slight predatory forward lean, elbows loosely bent, tail alive
LEAN = 4.0          # body forward lean (deg)
HEAD_COUNTER = -3.0  # head counter-pitch so the ember eyes stay on the player
ELBOW = -10.0       # resting forearm bend


def rot(**frames):
    return {"rotation": dict(frames)}


def pos(**frames):
    return {"position": dict(frames)}


def merge(*dicts):
    out = {}
    for d in dicts:
        out.update(d)
    return out


anims = {}

# ---------------------------------------------------------------------------
# idle (4.0s loop): slow breathing, restless tail, hunting head scan, blade micro-bob
# ---------------------------------------------------------------------------
anims["animation.drevathis.idle"] = {"loop": True, "animation_length": 4.0, "bones": {
    "body": merge(
        pos(**{"0": [0, 0, 0], "2.0": [0, 0.35, 0], "4.0": [0, 0, 0]}),
        rot(**{"0": [LEAN, 0, 0], "2.0": [LEAN + 1.2, 0, 0], "4.0": [LEAN, 0, 0]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "1.0": [HEAD_COUNTER - 1, 7, 0],
                   "2.0": [HEAD_COUNTER, 0, 0], "3.0": [HEAD_COUNTER - 1, -7, 1.5],
                   "4.0": [HEAD_COUNTER, 0, 0]}),
    "right_arm": rot(**{"0": [0, 0, -1.5], "2.0": [-2, 0, -3], "4.0": [0, 0, -1.5]}),
    "left_arm": rot(**{"0": [0, 0, 1.5], "2.0": [-2, 0, 3], "4.0": [0, 0, 1.5]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "2.0": [ELBOW - 3, 0, 0], "4.0": [ELBOW, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "2.0": [ELBOW - 4, 0, 0], "4.0": [ELBOW, 0, 0]}),
    "blade": rot(**{"0": [0, 0, 0], "2.0": [2, 0, 1.5], "4.0": [0, 0, 0]}),
    "tail": rot(**{"0": [0, 8, 0], "1.0": [2, 0, 0], "2.0": [0, -9, 0], "3.0": [-2, 0, 0],
                   "4.0": [0, 8, 0]}),
    "tail_tip": rot(**{"0": [0, 10, 0], "1.0": [0, 0, 0], "2.0": [0, -12, 0],
                       "3.0": [0, 0, 0], "4.0": [0, 10, 0]}),
}}

# ---------------------------------------------------------------------------
# walk (0.9s loop, two strides): prowling gait, counter-swinging arms (blade arm swings
# less), tail streaming with the motion
# ---------------------------------------------------------------------------
LEG_SWING = 25
anims["animation.drevathis.walk"] = {"loop": True, "animation_length": 0.9, "bones": {
    "right_leg": rot(**{"0": [-LEG_SWING, 0, 0], "0.45": [LEG_SWING, 0, 0],
                        "0.9": [-LEG_SWING, 0, 0]}),
    "left_leg": rot(**{"0": [LEG_SWING, 0, 0], "0.45": [-LEG_SWING, 0, 0],
                       "0.9": [LEG_SWING, 0, 0]}),
    "body": merge(
        pos(**{"0": [0, 0.2, 0], "0.12": [0, -0.3, 0], "0.3": [0, 0.2, 0],
               "0.45": [0, 0.2, 0], "0.57": [0, -0.3, 0], "0.75": [0, 0.2, 0],
               "0.9": [0, 0.2, 0]}),
        rot(**{"0": [LEAN + 3, 0, 2], "0.45": [LEAN + 3, 0, -2], "0.9": [LEAN + 3, 0, 2]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER - 2, 0, -1], "0.45": [HEAD_COUNTER - 2, 0, 1],
                   "0.9": [HEAD_COUNTER - 2, 0, -1]}),
    "right_arm": rot(**{"0": [9, 0, -2], "0.45": [-9, 0, -2], "0.9": [9, 0, -2]}),
    "left_arm": rot(**{"0": [-15, 0, 2], "0.45": [15, 0, 2], "0.9": [-15, 0, 2]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.45": [ELBOW - 4, 0, 0], "0.9": [ELBOW, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW - 3, 0, 0], "0.45": [ELBOW - 8, 0, 0],
                           "0.9": [ELBOW - 3, 0, 0]}),
    "blade": rot(**{"0": [0, 0, 0], "0.22": [-3, 0, 0], "0.45": [0, 0, 0],
                    "0.67": [3, 0, 0], "0.9": [0, 0, 0]}),
    "tail": rot(**{"0": [-4, 10, 0], "0.45": [-4, -10, 0], "0.9": [-4, 10, 0]}),
    "tail_tip": rot(**{"0": [0, 12, 0], "0.45": [0, -12, 0], "0.9": [0, 12, 0]}),
}}

# ---------------------------------------------------------------------------
# throw_wave (one-shot, 0.6s): coil back with the left hand drawn in, then a hard open-palm
# thrust at 0.30s - the server spawns the dark-wave projectile on that exact tick (6)
# ---------------------------------------------------------------------------
anims["animation.drevathis.throw_wave"] = {"loop": False, "animation_length": 0.6, "bones": {
    "body": rot(**{"0": [LEAN, 0, 0], "0.18": [LEAN - 2, 14, 0], "0.3": [LEAN + 6, -12, 0],
                   "0.42": [LEAN + 6, -12, 0], "0.6": [LEAN, 0, 0]}),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.18": [HEAD_COUNTER, -14, 0],
                   "0.3": [HEAD_COUNTER + 2, 12, 0], "0.42": [HEAD_COUNTER + 2, 12, 0],
                   "0.6": [HEAD_COUNTER, 0, 0]}),
    # left arm: draw back across the chest, then thrust straight forward, palm open
    "left_arm": rot(**{"0": [0, 0, 1.5], "0.18": [34, 0, 18], "0.3": [-86, 0, -6],
                       "0.42": [-86, 0, -6], "0.6": [0, 0, 1.5]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.18": [-44, 0, 0], "0.3": [-2, 0, 0],
                           "0.42": [-2, 0, 0], "0.6": [ELBOW, 0, 0]}),
    # blade arm sweeps back for balance
    "right_arm": rot(**{"0": [0, 0, -1.5], "0.18": [10, 0, -4], "0.3": [26, 0, -10],
                        "0.42": [26, 0, -10], "0.6": [0, 0, -1.5]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.3": [ELBOW - 4, 0, 0], "0.6": [ELBOW, 0, 0]}),
    "blade": rot(**{"0": [0, 0, 0], "0.3": [6, 0, 4], "0.6": [0, 0, 0]}),
    "tail": rot(**{"0": [0, 0, 0], "0.18": [0, 14, 0], "0.3": [0, -16, 0], "0.6": [0, 0, 0]}),
    "tail_tip": rot(**{"0": [0, 0, 0], "0.3": [0, -14, 0], "0.6": [0, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# curse_ground (one-shot, 1.6s): two-handed raise of the blade to the sky, a trembling
# channel, then a scything downward slash at 1.00s (tick 20) - the moment the ground curse
# erupts server-side - and a slow, looming recover
# ---------------------------------------------------------------------------
RAISE = -158.0
# Blade LOCAL x when the arm is raised: world-up needs local ~ +158, minus the 24 base = +134.
# On the slash the arm ends at -35 and the blade plunges to world ~ -169 (down-forward), local
# -134; interpolating +134 -> -134 sweeps the tip sky -> forward -> ground: the scythe arc.
BLADE_SKY = [134.0, 0.0, -14.0]
BLADE_PLUNGE = [-134.0, 0.0, -14.0]
anims["animation.drevathis.curse_ground"] = {"loop": False, "animation_length": 1.6, "bones": {
    "body": merge(
        pos(**{"0": [0, 0, 0], "0.35": [0, 0.8, 0], "1.0": [0, -1.6, 0],
               "1.25": [0, -1.6, 0], "1.6": [0, 0, 0]}),
        rot(**{"0": [LEAN, 0, 0], "0.35": [-8, 0, 0], "0.9": [-9, 0, 0],
               "1.0": [22, 0, 0], "1.25": [22, 0, 0], "1.6": [LEAN, 0, 0]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.35": [-22, 0, 0], "0.9": [-24, 0, 0],
                   "1.0": [10, 0, 0], "1.25": [10, 0, 0], "1.6": [HEAD_COUNTER, 0, 0]}),
    # right arm: raise straight up (tremble during the channel), slash down-forward
    "right_arm": rot(**{"0": [0, 0, -1.5], "0.35": [RAISE, 0, -8], "0.55": [RAISE + 4, 0, -8],
                        "0.75": [RAISE - 3, 0, -8], "0.9": [RAISE, 0, -8],
                        "1.0": [-35, 0, -5], "1.25": [-35, 0, -5], "1.6": [0, 0, -1.5]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-14, 0, 0], "0.9": [-14, 0, 0],
                            "1.0": [-2, 0, 0], "1.25": [-2, 0, 0], "1.6": [ELBOW, 0, 0]}),
    # blade points SKYWARD during the channel, then plunges down-forward on the slash
    "blade": rot(**{"0": [0, 0, 0], "0.35": BLADE_SKY, "0.9": BLADE_SKY,
                    "1.0": BLADE_PLUNGE, "1.25": BLADE_PLUNGE, "1.6": [0, 0, 0]}),
    # left hand joins the grip overhead, flies wide on the slash
    "left_arm": rot(**{"0": [0, 0, 1.5], "0.35": [RAISE + 8, -16, 10],
                       "0.9": [RAISE + 8, -16, 10], "1.0": [30, 0, 26],
                       "1.25": [30, 0, 26], "1.6": [0, 0, 1.5]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-18, 0, 0], "0.9": [-18, 0, 0],
                           "1.0": [-4, 0, 0], "1.6": [ELBOW, 0, 0]}),
    "tail": rot(**{"0": [0, 0, 0], "0.35": [-10, 0, 0], "0.9": [-12, 0, 0],
                   "1.0": [8, 0, 0], "1.6": [0, 0, 0]}),
    "tail_tip": rot(**{"0": [0, 0, 0], "0.35": [-8, 0, 0], "1.0": [10, 0, 0],
                       "1.6": [0, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# stampede_run (0.5s loop, state-driven while STAMPEDE_ACTIVE): horns-first charge - deep
# forward lean, head lowered, arms raked back, galloping leg swing, tail streaming straight
# ---------------------------------------------------------------------------
GALLOP = 38
anims["animation.drevathis.stampede_run"] = {"loop": True, "animation_length": 0.5, "bones": {
    "body": merge(
        pos(**{"0": [0, -0.5, 0], "0.12": [0, 0.5, 0], "0.25": [0, -0.5, 0],
               "0.37": [0, 0.5, 0], "0.5": [0, -0.5, 0]}),
        rot(**{"0": [26, 0, 0], "0.25": [24, 0, 0], "0.5": [26, 0, 0]}),
    ),
    # head down so the horns lead
    "head": rot(**{"0": [-14, 0, 0], "0.25": [-12, 0, 0], "0.5": [-14, 0, 0]}),
    "right_leg": rot(**{"0": [-GALLOP, 0, 0], "0.25": [GALLOP, 0, 0], "0.5": [-GALLOP, 0, 0]}),
    "left_leg": rot(**{"0": [GALLOP, 0, 0], "0.25": [-GALLOP, 0, 0], "0.5": [GALLOP, 0, 0]}),
    "right_arm": rot(**{"0": [30, 0, -8], "0.25": [36, 0, -8], "0.5": [30, 0, -8]}),
    "left_arm": rot(**{"0": [36, 0, 8], "0.25": [30, 0, 8], "0.5": [36, 0, 8]}),
    "right_forearm": rot(**{"0": [-6, 0, 0], "0.5": [-6, 0, 0]}),
    "left_forearm": rot(**{"0": [-6, 0, 0], "0.5": [-6, 0, 0]}),
    "blade": rot(**{"0": [8, 0, -4], "0.5": [8, 0, -4]}),
    "tail": rot(**{"0": [14, 4, 0], "0.25": [16, -4, 0], "0.5": [14, 4, 0]}),
    "tail_tip": rot(**{"0": [6, 6, 0], "0.25": [6, -6, 0], "0.5": [6, 6, 0]}),
}}

# ---------------------------------------------------------------------------
# end_channel (5.0s = the goal's full 100-tick channel, hold_on_last_frame): arms spread wide
# and tilted skyward, head thrown back, trembling throughout while comets fall; at 4.5s the
# arms snap inward-down for the terror burst that fires at the channel's end
# ---------------------------------------------------------------------------
# Arms flung wide and skyward: driven by z (sideways swing past horizontal, tips 35 deg above
# level) - an x-dominant pose ended up crossing the arms in front of the face (preview finding)
SPREAD_R = [0.0, 0.0, -125.0]
SPREAD_L = [0.0, 0.0, 125.0]
# Blade along the raised-wide arm line: a ~180 local x flip re-points the tip beyond the fist
# (156 = 180 - the 24 base pitch), keeping it sky-out instead of stabbing back down inward
BLADE_SPREAD = [156.0, 0.0, -14.0]
def _tremble(base, amp, t0, t1, step, axis=0):
    """Keyframes oscillating around base[axis] +/- amp between t0..t1."""
    frames = {}
    t = t0
    flip = 1
    while t <= t1 + 1e-9:
        v = list(base)
        v[axis] = base[axis] + amp * flip
        frames[f"{round(t, 2):g}"] = v
        flip = -flip
        t += step
    return frames


end_bones = {
    "body": merge(
        pos(**{"0": [0, 0, 0], "0.6": [0, 1.0, 0], "4.4": [0, 1.0, 0],
               "4.7": [0, -1.8, 0], "5.0": [0, -1.2, 0]}),
        rot(**{"0": [LEAN, 0, 0], "0.6": [-14, 0, 0], "4.4": [-14, 0, 0],
               "4.7": [24, 0, 0], "5.0": [12, 0, 0]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.6": [-34, 0, 0], "4.4": [-34, 0, 0],
                   "4.7": [14, 0, 0], "5.0": [4, 0, 0]}),
    "right_arm": rot(**merge({"0": [0, 0, -1.5]},
                             _tremble(SPREAD_R, 3.0, 0.6, 4.4, 0.4, axis=2),
                             {"4.7": [38, 0, -14], "5.0": [24, 0, -8]})),
    "left_arm": rot(**merge({"0": [0, 0, 1.5]},
                            _tremble(SPREAD_L, 3.0, 0.6, 4.4, 0.4, axis=2),
                            {"4.7": [38, 0, 14], "5.0": [24, 0, 8]})),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.6": [-10, 0, 0], "4.4": [-10, 0, 0],
                            "4.7": [-4, 0, 0], "5.0": [ELBOW, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.6": [-10, 0, 0], "4.4": [-10, 0, 0],
                           "4.7": [-4, 0, 0], "5.0": [ELBOW, 0, 0]}),
    "blade": rot(**{"0": [0, 0, 0], "0.6": BLADE_SPREAD, "4.4": BLADE_SPREAD,
                    "4.7": [4, 0, 0], "5.0": [0, 0, 0]}),
    "tail": rot(**merge({"0": [0, 0, 0]}, _tremble([-16.0, 0.0, 0.0], 8.0, 0.6, 4.4, 0.5, axis=1),
                        {"4.7": [10, 0, 0], "5.0": [4, 0, 0]})),
    "tail_tip": rot(**{"0": [0, 0, 0], "0.6": [16, 0, 0], "4.4": [16, 0, 0],
                       "5.0": [0, 0, 0]}),
}
anims["animation.drevathis.end_channel"] = {"loop": "hold_on_last_frame",
                                            "animation_length": 5.0, "bones": end_bones}

doc = {"format_version": "1.8.0", "animations": anims}
with open("drevathis.animation.json", "w", encoding="utf-8") as f:
    json.dump(doc, f, indent=2)
print("wrote drevathis.animation.json:", ", ".join(a.split(".")[-1] for a in anims))
