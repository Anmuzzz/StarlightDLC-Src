package com.isusdlc.systems.modules.modules.visuals.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;

import java.util.*;

public final class CosmeticBbAnimation {
    private final String name;
    private final float length;
    private final boolean loop;
    private final Map<String, BoneTrack> tracksByBoneUuid = new HashMap<>();

    public CosmeticBbAnimation(String name, float length, boolean loop) {
        this.name = name;
        this.length = Math.max(length, 1f / 20f);
        this.loop = loop;
    }

    public String getName() {
        return name;
    }

    public float getLength() {
        return length;
    }

    public boolean isLoop() {
        return loop;
    }

    public Map<String, BoneTrack> getTracks() {
        return tracksByBoneUuid;
    }

    public static Map<String, CosmeticBbAnimation> parseAll(JsonObject bbmodel) {
        Map<String, CosmeticBbAnimation> map = new LinkedHashMap<>();
        if (!bbmodel.has("animations")) return map;

        for (JsonElement el : bbmodel.getAsJsonArray("animations")) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String name = obj.has("name") ? obj.get("name").getAsString() : "anim";
            if (name.startsWith("--") || name.isBlank()) continue;

            float length = obj.has("length") ? obj.get("length").getAsFloat() : 1f;
            // в bbmodel "loop" — строковая перечисление: "loop", "once" или "hold"
            boolean loop = true;
            if (obj.has("loop")) {
                JsonElement loopEl = obj.get("loop");
                loop = loopEl.isJsonPrimitive() && loopEl.getAsJsonPrimitive().isString()
                    ? "loop".equalsIgnoreCase(loopEl.getAsString())
                    : loopEl.getAsBoolean();
            }
            CosmeticBbAnimation anim = new CosmeticBbAnimation(name, length, loop);

            if (obj.has("animators")) {
                JsonObject animators = obj.getAsJsonObject("animators");
                for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject boneAnim = entry.getValue().getAsJsonObject();
                    String boneUuid = entry.getKey();
                    if (boneAnim.has("uuid")) {
                        boneUuid = boneAnim.get("uuid").getAsString();
                    }
                    BoneTrack track = BoneTrack.parse(boneAnim);
                    if (!track.isEmpty()) {
                        anim.tracksByBoneUuid.put(boneUuid, track);
                    }
                }
            }

            if (anim.tracksByBoneUuid.isEmpty()) continue;
            map.put(name.toLowerCase(), anim);
        }
        return map;
    }

    public static void apply(CosmeticModel model, CosmeticBbAnimation animation, float time) {
        java.util.Set<String> filter = model.getAnimationBoneFilter();
        for (Map.Entry<String, BoneTrack> entry : animation.tracksByBoneUuid.entrySet()) {
            if (filter != null && !filter.contains(entry.getKey())) continue;
            CosmeticBone bone = model.getBoneByUuid(entry.getKey());
            if (bone == null) continue;

            BoneTrack track = entry.getValue();
            Vector3f rot = track.sampleRotation(time, animation.length, animation.loop);
            Vector3f pos = track.samplePosition(time, animation.length, animation.loop);

            if (rot != null) {
                bone.setAnimRotation(new Vector3f(
                        rot.x - bone.getDefaultRotation().x,
                        rot.y - bone.getDefaultRotation().y,
                        rot.z - bone.getDefaultRotation().z
                ));
            }
            if (pos != null) {
                bone.setAnimPosition(pos);
            }
        }
    }

    public static final class BoneTrack {
        private final List<Keyframe> rotations = new ArrayList<>();
        private final List<Keyframe> positions = new ArrayList<>();

        static BoneTrack parse(JsonObject boneAnim) {
            BoneTrack track = new BoneTrack();
            if (!boneAnim.has("keyframes")) return track;

            for (JsonElement el : boneAnim.getAsJsonArray("keyframes")) {
                if (!el.isJsonObject()) continue;
                JsonObject kf = el.getAsJsonObject();
                String channel = kf.has("channel") ? kf.get("channel").getAsString() : "";
                float t = kf.has("time") ? kf.get("time").getAsFloat() : 0f;
                Vector3f value = readPoint(kf);
                String interp = kf.has("interpolation") ? kf.get("interpolation").getAsString() : "linear";
                Keyframe keyframe = new Keyframe(t, value, interp);

                switch (channel) {
                    case "rotation" -> track.rotations.add(keyframe);
                    case "position" -> track.positions.add(keyframe);
                    default -> {
                    }
                }
            }

            track.rotations.sort(Comparator.comparing(k -> k.time));
            track.positions.sort(Comparator.comparing(k -> k.time));
            return track;
        }

        boolean isEmpty() {
            return rotations.isEmpty() && positions.isEmpty();
        }

        Vector3f sampleRotation(float time, float length, boolean loop) {
            return sample(rotations, time, length, loop);
        }

        Vector3f samplePosition(float time, float length, boolean loop) {
            return sample(positions, time, length, loop);
        }

        private static Vector3f sample(List<Keyframe> frames, float time, float length, boolean loop) {
            if (frames.isEmpty()) return null;
            if (frames.size() == 1) return new Vector3f(frames.get(0).value);

            float t = loop ? wrapTime(time, length) : Math.min(time, length);
            Keyframe prev = frames.get(0);
            Keyframe next = frames.get(frames.size() - 1);

            for (int i = 0; i < frames.size() - 1; i++) {
                Keyframe a = frames.get(i);
                Keyframe b = frames.get(i + 1);
                if (t >= a.time && t <= b.time) {
                    prev = a;
                    next = b;
                    break;
                }
            }
            if (t >= frames.get(frames.size() - 1).time) {
                prev = frames.get(frames.size() - 1);
                next = prev;
            }

            if (prev == next || Math.abs(next.time - prev.time) < 0.0001f) {
                return new Vector3f(prev.value);
            }

            float alpha = (t - prev.time) / (next.time - prev.time);
            if ("step".equals(prev.interpolation)) {
                return new Vector3f(prev.value);
            }
            return lerp(prev.value, next.value, MathHelperClamp(alpha));
        }

        private static float wrapTime(float time, float length) {
            if (length <= 0f) return time;
            float wrapped = time % length;
            return wrapped < 0f ? wrapped + length : wrapped;
        }

        private static float MathHelperClamp(float v) {
            return Math.max(0f, Math.min(1f, v));
        }

        private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
            return new Vector3f(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t,
                    a.z + (b.z - a.z) * t
            );
        }

        private static Vector3f readPoint(JsonObject kf) {
            if (!kf.has("data_points")) return new Vector3f();
            JsonArray points = kf.getAsJsonArray("data_points");
            if (points.isEmpty()) return new Vector3f();
            JsonObject point = points.get(0).getAsJsonObject();
            return new Vector3f(
                    parseNum(point, "x"),
                    parseNum(point, "y"),
                    parseNum(point, "z")
            );
        }

        private static float parseNum(JsonObject obj, String key) {
            if (!obj.has(key)) return 0f;
            JsonElement el = obj.get(key);
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsFloat();
            }
            try {
                return Float.parseFloat(el.getAsString().trim());
            } catch (Exception ignored) {
                return 0f;
            }
        }
    }

    private record Keyframe(float time, Vector3f value, String interpolation) {
    }
}
