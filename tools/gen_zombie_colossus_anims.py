"""Generates zombie_colossus.animation.json (GeckoLib/Bedrock animation format).

Generated (like tools/gen_spider_queen_anims.py) so mirrored limb pairs come from one table.
Animation names/bone names must keep matching ZombieColossusEntity's RawAnimation constants and
zombie_colossus.geo.json's bone names (body, head, right_arm/right_forearm/club,
left_arm/left_forearm, right_leg, left_leg - head/forearms/club are CHILD bones, so their
keyframes are local, layered on top of the parent's).

Sign conventions (same verified GeckoLib mapping as the spider script - see its docstring):
- rot x > 0 pitches a bone front-down; on a hanging limb (arm/leg) it swings the tip BACKWARD,
  so forward swings are negative x.
- rot z > 0 swings a hanging limb's tip toward +X = the entity's LEFT; "outward" is negative z
  for the right limbs, positive z for the left.
- position y > 0 is up, z > 0 is backward.

Club keyframe note: the club bone's BASE rotation in the geometry is [52, 0, -32]
(carried forward-down-outward at rest, per user feedback). Absolute club poses below are
authored as offsets against that base - if the base pitch changes, every absolute club rx
keyframe (smash/rage/leap/earthquake) must shift by the same delta.

Server-timing contracts (keep these in lockstep with ZombieColossusEntity):
- smash: the goal deals damage SMASH_DAMAGE_DELAY_TICKS (6) after triggering - the club lands
  at 0.30s here.
- rage: strikes fire at ticks 8/13/17 after the trigger - club impacts at 0.40/0.65/0.85s.
- leap_windup: LeapAttackGoal.WINDUP_TICKS = 10 -> 0.5s length.
- leap_flight: flight table is 14 ticks -> 0.7s, hold_on_last_frame.
- earthquake: EarthquakeGoal slams at tick 15 -> impact at 0.75s here.

Run from the repo root, preview each pose with e.g.
  python tools/render_geckolib_preview.py --model zombie_colossus --anim earthquake --times 0.6,0.75,1.0
then copy to src/main/resources/assets/baum2/geckolib/animations/entity/zombie_colossus.animation.json
"""
import json

# baseline posture, applied in idle/walk so the brute always reads hunched and elbows-bent,
# and the transition into any one-shot starts from a consistent silhouette
HUNCH = 5.0        # body forward lean (deg)
HEAD_COUNTER = -4.0  # head counter-pitch so the eyes stay on the player
ELBOW = -8.0       # slight forearm bend


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
# idle (3.6s loop): heavy breathing, slow head scan, club shoulder-bounce
# ---------------------------------------------------------------------------
anims["animation.zombie_colossus.idle"] = {"loop": True, "animation_length": 3.6, "bones": {
    "body": merge(
        pos(**{"0": [0, 0, 0], "1.8": [0, 0.4, 0], "3.6": [0, 0, 0]}),
        rot(**{"0": [HUNCH, 0, 0], "1.8": [HUNCH + 1.5, 0, 0], "3.6": [HUNCH, 0, 0]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.9": [HEAD_COUNTER, 6, 0],
                   "1.8": [HEAD_COUNTER - 1, 0, 0], "2.7": [HEAD_COUNTER, -6, 2],
                   "3.6": [HEAD_COUNTER, 0, 0]}),
    "right_arm": rot(**{"0": [0, 0, -1], "1.8": [-2, 0, -3], "3.6": [0, 0, -1]}),
    "left_arm": rot(**{"0": [0, 0, 1], "1.8": [-2, 0, 3], "3.6": [0, 0, 1]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "1.8": [ELBOW - 3, 0, 0], "3.6": [ELBOW, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "1.8": [ELBOW - 3, 0, 0], "3.6": [ELBOW, 0, 0]}),
    "club": rot(**{"0": [0, 0, 0], "1.8": [2, 0, 2], "3.6": [0, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# walk (1.0s loop, two heavy steps): stomping gait, counter-swinging arms, impact dips
# ---------------------------------------------------------------------------
LEG_SWING = 26
walk_bones = {
    "right_leg": rot(**{"0": [-LEG_SWING, 0, 0], "0.5": [LEG_SWING, 0, 0], "1.0": [-LEG_SWING, 0, 0]}),
    "left_leg": rot(**{"0": [LEG_SWING, 0, 0], "0.5": [-LEG_SWING, 0, 0], "1.0": [LEG_SWING, 0, 0]}),
    "body": merge(
        # dip right after each footfall - the "weight" of the stomp
        pos(**{"0": [0, 0.3, 0], "0.12": [0, -0.5, 0], "0.3": [0, 0.3, 0], "0.5": [0, 0.3, 0],
               "0.62": [0, -0.5, 0], "0.8": [0, 0.3, 0], "1.0": [0, 0.3, 0]}),
        rot(**{"0": [HUNCH + 1, 0, 2.5], "0.5": [HUNCH + 1, 0, -2.5], "1.0": [HUNCH + 1, 0, 2.5]}),
    ),
    "head": rot(**{"0": [HEAD_COUNTER, 0, -1.5], "0.5": [HEAD_COUNTER, 0, 1.5],
                   "1.0": [HEAD_COUNTER, 0, -1.5]}),
    # club arm swings less (he's carrying), off-arm swings free
    "right_arm": rot(**{"0": [10, 0, -2], "0.5": [-10, 0, -2], "1.0": [10, 0, -2]}),
    "left_arm": rot(**{"0": [-16, 0, 2], "0.5": [16, 0, 2], "1.0": [-16, 0, 2]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.25": [ELBOW - 5, 0, 0], "0.5": [ELBOW, 0, 0],
                            "0.75": [ELBOW - 5, 0, 0], "1.0": [ELBOW, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW - 4, 0, 0], "0.5": [ELBOW - 10, 0, 0], "1.0": [ELBOW - 4, 0, 0]}),
    "club": rot(**{"0": [0, 0, 0], "0.25": [-4, 0, 0], "0.5": [0, 0, 0], "0.75": [4, 0, 0],
                   "1.0": [0, 0, 0]}),
}
anims["animation.zombie_colossus.walk"] = {"loop": True, "animation_length": 1.0, "bones": walk_bones}

# ---------------------------------------------------------------------------
# smash (one-shot, 0.8s): raise club overhead, slam down at 0.30s (= damage tick), recover
# ---------------------------------------------------------------------------
anims["animation.zombie_colossus.smash"] = {"loop": False, "animation_length": 0.8, "bones": {
    "body": rot(**{"0": [HUNCH, 0, 0], "0.2": [-6, -8, 0], "0.3": [16, 6, 0],
                   "0.45": [16, 6, 0], "0.8": [HUNCH, 0, 0]}),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.2": [-10, 0, 0], "0.3": [6, 0, 0],
                   "0.45": [6, 0, 0], "0.8": [HEAD_COUNTER, 0, 0]}),
    "right_arm": rot(**{"0": [0, 0, -1], "0.2": [-148, 0, -8], "0.3": [38, 0, -4],
                        "0.45": [38, 0, -4], "0.8": [0, 0, -1]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.2": [-24, 0, 0], "0.3": [-4, 0, 0],
                            "0.45": [-4, 0, 0], "0.8": [ELBOW, 0, 0]}),
    # club aligns with the arm for the swing (cancels its resting 52/-32 base rotation)
    "club": rot(**{"0": [0, 0, 0], "0.2": [-56, 0, 28], "0.3": [-52, 0, 30],
                   "0.45": [-52, 0, 30], "0.8": [0, 0, 0]}),
    "left_arm": rot(**{"0": [0, 0, 1], "0.2": [24, 0, 14], "0.3": [-18, 0, 8],
                       "0.45": [-18, 0, 8], "0.8": [0, 0, 1]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.2": [ELBOW, 0, 0], "0.8": [ELBOW, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# rage (one-shot, 1.4s): two-handed overhead wind-up, then THREE strikes landing exactly at
# the server's 0.40 / 0.65 / 0.85s damage ticks, big recover
# ---------------------------------------------------------------------------
UP, DOWN = -152, 34
anims["animation.zombie_colossus.rage"] = {"loop": False, "animation_length": 1.4, "bones": {
    "body": rot(**{"0": [HUNCH, 0, 0], "0.35": [-10, 0, 0], "0.4": [18, 0, 0],
                   "0.55": [-4, -6, 0], "0.65": [16, 8, 0], "0.75": [-6, 4, 0],
                   "0.85": [20, -4, 0], "1.05": [20, -4, 0], "1.4": [HUNCH, 0, 0]}),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.35": [-12, 0, 0], "0.4": [8, 0, 0],
                   "0.85": [8, 0, 0], "1.4": [HEAD_COUNTER, 0, 0]}),
    "right_arm": rot(**{"0": [0, 0, -1], "0.35": [UP, 0, -6], "0.4": [DOWN, 0, -3],
                        "0.55": [UP * 0.65, 0, -6], "0.65": [DOWN - 6, 0, -3],
                        "0.75": [UP * 0.75, 0, -6], "0.85": [DOWN + 6, 0, -3],
                        "1.05": [DOWN + 6, 0, -3], "1.4": [0, 0, -1]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-26, 0, 0], "0.4": [-4, 0, 0],
                            "0.85": [-4, 0, 0], "1.4": [ELBOW, 0, 0]}),
    "club": rot(**{"0": [0, 0, 0], "0.35": [-56, 0, 30], "0.4": [-52, 0, 30],
                   "1.05": [-52, 0, 30], "1.4": [0, 0, 0]}),
    # off-hand joins the grip for a two-handed read
    "left_arm": rot(**{"0": [0, 0, 1], "0.35": [UP + 10, -14, 10], "0.4": [DOWN - 8, -10, 6],
                       "0.55": [UP * 0.6, -14, 10], "0.65": [DOWN - 10, -10, 6],
                       "0.75": [UP * 0.7, -14, 10], "0.85": [DOWN, -10, 6],
                       "1.05": [DOWN, -10, 6], "1.4": [0, 0, 1]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-20, 0, 0], "0.4": [-6, 0, 0],
                           "0.85": [-6, 0, 0], "1.4": [ELBOW, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# leap_windup (0.5s, state-driven while LEAP_WINDUP_TICKS > 0): deep crouch, arms wound back
# ---------------------------------------------------------------------------
anims["animation.zombie_colossus.leap_windup"] = {"loop": False, "animation_length": 0.5, "bones": {
    "body": merge(pos(**{"0": [0, 0, 0], "0.35": [0, -3, 0], "0.5": [0, -3.2, 0]}),
                  rot(**{"0": [HUNCH, 0, 0], "0.35": [16, 0, 0], "0.5": [18, 0, 0]})),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.35": [-14, 0, 0], "0.5": [-16, 0, 0]}),
    "right_leg": merge(pos(**{"0": [0, 0, 0], "0.35": [0, -1.2, 0], "0.5": [0, -1.2, 0]}),
                       rot(**{"0": [0, 0, 0], "0.35": [-14, 0, 0], "0.5": [-14, 0, 0]})),
    "left_leg": merge(pos(**{"0": [0, 0, 0], "0.35": [0, -1.2, 0], "0.5": [0, -1.2, 0]}),
                      rot(**{"0": [0, 0, 0], "0.35": [-14, 0, 0], "0.5": [-14, 0, 0]})),
    "right_arm": rot(**{"0": [0, 0, -1], "0.35": [38, 0, -12], "0.5": [42, 0, -12]}),
    "left_arm": rot(**{"0": [0, 0, 1], "0.35": [38, 0, 12], "0.5": [42, 0, 12]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-30, 0, 0], "0.5": [-30, 0, 0]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.35": [-30, 0, 0], "0.5": [-30, 0, 0]}),
    "club": rot(**{"0": [0, 0, 0], "0.35": [-68, 0, 14], "0.5": [-68, 0, 14]}),
}}

# ---------------------------------------------------------------------------
# leap_flight (0.7s = 14 flight ticks, hold_on_last_frame): explode upward, then club raised
# overhead ready to smash on landing
# ---------------------------------------------------------------------------
anims["animation.zombie_colossus.leap_flight"] = {"loop": "hold_on_last_frame", "animation_length": 0.7,
                                                  "bones": {
    "body": rot(**{"0": [-10, 0, 0], "0.4": [-4, 0, 0], "0.7": [-4, 0, 0]}),
    "head": rot(**{"0": [-8, 0, 0], "0.7": [-8, 0, 0]}),
    "right_leg": rot(**{"0": [28, 0, -4], "0.4": [20, 0, -4], "0.7": [20, 0, -4]}),
    "left_leg": rot(**{"0": [14, 0, 4], "0.4": [26, 0, 4], "0.7": [26, 0, 4]}),
    "right_arm": rot(**{"0": [-70, 0, -18], "0.35": [-150, 0, -10], "0.7": [-150, 0, -10]}),
    "right_forearm": rot(**{"0": [-20, 0, 0], "0.35": [-26, 0, 0], "0.7": [-26, 0, 0]}),
    "club": rot(**{"0": [-72, 0, 16], "0.35": [-54, 0, 30], "0.7": [-54, 0, 30]}),
    "left_arm": rot(**{"0": [-70, 0, 18], "0.35": [-40, 0, 24], "0.7": [-40, 0, 24]}),
    "left_forearm": rot(**{"0": [-20, 0, 0], "0.7": [-20, 0, 0]}),
}}

# ---------------------------------------------------------------------------
# earthquake (one-shot, 2.2s): rise to full height with the club sky-high, slam the ground at
# 0.75s (= the server's damage/shockwave tick), shuddering impact hold, slow heavy recover
# ---------------------------------------------------------------------------
eq_body_pos = {"0": [0, 0, 0], "0.6": [0, 1.0, 0], "0.75": [0, -2.6, 0],
               "0.85": [0, -2.45, 0], "0.95": [0, -2.6, 0], "1.05": [0, -2.45, 0],
               "1.15": [0, -2.6, 0], "1.5": [0, -2.0, 0], "2.2": [0, 0, 0]}
anims["animation.zombie_colossus.earthquake"] = {"loop": False, "animation_length": 2.2, "bones": {
    "body": merge(pos(**eq_body_pos),
                  rot(**{"0": [HUNCH, 0, 0], "0.6": [-14, 0, 0], "0.75": [24, 0, 0],
                         "1.15": [24, 0, 0], "1.5": [18, 0, 0], "2.2": [HUNCH, 0, 0]})),
    "head": rot(**{"0": [HEAD_COUNTER, 0, 0], "0.6": [-18, 0, 0], "0.75": [10, 0, 0],
                   "1.15": [10, 0, 0], "2.2": [HEAD_COUNTER, 0, 0]}),
    "right_arm": rot(**{"0": [0, 0, -1], "0.6": [-158, 0, -10], "0.75": [52, 0, -6],
                        "0.85": [50, 0, -6], "0.95": [52, 0, -6], "1.15": [52, 0, -6],
                        "1.5": [40, 0, -6], "2.2": [0, 0, -1]}),
    "right_forearm": rot(**{"0": [ELBOW, 0, 0], "0.6": [-18, 0, 0], "0.75": [6, 0, 0],
                            "1.15": [6, 0, 0], "2.2": [ELBOW, 0, 0]}),
    "club": rot(**{"0": [0, 0, 0], "0.6": [-54, 0, 32], "0.75": [-50, 0, 32],
                   "1.15": [-50, 0, 32], "2.2": [0, 0, 0]}),
    "left_arm": rot(**{"0": [0, 0, 1], "0.6": [-150, -12, 12], "0.75": [48, -8, 8],
                       "1.15": [48, -8, 8], "1.5": [36, -8, 8], "2.2": [0, 0, 1]}),
    "left_forearm": rot(**{"0": [ELBOW, 0, 0], "0.6": [-18, 0, 0], "0.75": [4, 0, 0],
                           "1.15": [4, 0, 0], "2.2": [ELBOW, 0, 0]}),
    "right_leg": rot(**{"0": [0, 0, 0], "0.75": [-10, 0, 0], "1.15": [-10, 0, 0],
                        "2.2": [0, 0, 0]}),
    "left_leg": rot(**{"0": [0, 0, 0], "0.75": [-10, 0, 0], "1.15": [-10, 0, 0],
                       "2.2": [0, 0, 0]}),
}}

doc = {"format_version": "1.8.0", "animations": anims}
with open("zombie_colossus.animation.json", "w", encoding="utf-8") as f:
    json.dump(doc, f, indent=2)
print("wrote zombie_colossus.animation.json:", ", ".join(a.split(".")[-1] for a in anims))
