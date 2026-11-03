package com.isusdlc.systems.modules.modules.visuals.cosmetic;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import static com.isusdlc.systems.modules.modules.visuals.cosmetic.IisusClientInfo.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
public class CosmeticModel {
    private final String id;
    private final String name;
    private final String description;
    private final String accentColor;
    private final String folderName;
    private CosmeticType type;
    private final File externalDirectory;
    @Setter private String source = "Built-in";

    private final List<CosmeticBone> rootBones = new ArrayList<>();
    private final List<CosmeticBone> renderRoots = new ArrayList<>();
    private boolean customLegsPresent;
    private boolean bowReplacementAccessory;
    private boolean swordReplacementAccessory;
    private boolean hatOnlyAccessory;
    private boolean wingsOnlyAccessory;
    private Set<String> visibleBoneUuids;
    @Setter private Set<String> animationBoneFilter;
    private final Map<String, CosmeticBone> boneMap = new HashMap<>();
    private final Map<String, CosmeticBone> boneMapByUuid = new HashMap<>();
    private final Map<String, CosmeticBbAnimation> bbAnimations = new LinkedHashMap<>();
    private final List<CosmeticTextureSlot> textureSlots = new ArrayList<>();
    private int textureWidth = 64;
    private int textureHeight = 64;
    private Identifier textureId;
    private boolean textureLoaded = false;
    private CosmeticTextureSlot fallbackSlot;

    private CosmeticModel(String id, String name, String description, String accentColor,
                          String folderName, CosmeticType type, File externalDirectory) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.accentColor = accentColor;
        this.folderName = folderName;
        this.type = type;
        this.externalDirectory = externalDirectory;
    }

    public CosmeticModel withType(CosmeticType newType) {
        this.type = newType;
        this.resetTexture();
        this.postProcess();
        return this;
    }

    public void resetTexture() {
        textureLoaded = false;
        textureId = null;
        for (CosmeticTextureSlot slot : textureSlots) {
            slot.reset();
        }
    }

    public CosmeticTextureSlot getTextureSlot(int index) {
        if (!textureSlots.isEmpty()) {
            if (index >= 0 && index < textureSlots.size()) {
                return textureSlots.get(index);
            }
            return textureSlots.getFirst();
        }
        if (textureId == null) return null;
        if (fallbackSlot == null) {
            fallbackSlot = new CosmeticTextureSlot(0, textureId, textureWidth, textureHeight, "fallback");
            if (textureLoaded) fallbackSlot.markLoaded();
        }
        return fallbackSlot;
    }

    public static CosmeticModel loadBuiltin(String resourcePath) {
        return loadBuiltin(resourcePath, null);
    }

    public static CosmeticModel loadBuiltin(String resourcePath, CosmeticType forcedType) {
        String path = normalizeResourcePath(resourcePath);
        return load(path, null, CosmeticModel.builtinCatalogId(path), forcedType);
    }

    private static String normalizeResourcePath(String path) {
        return path.replace('\\', '/').toLowerCase();
    }

    static String builtinCatalogId(String resourcePath) {
        return "builtin:" + normalizeResourcePath(resourcePath).replace('/', '_');
    }

    private static String resourceLeaf(String resourcePath) {
        int slash = resourcePath.lastIndexOf('/');
        return slash >= 0 ? resourcePath.substring(slash + 1) : resourcePath;
    }

    public static CosmeticModel loadExternal(File folder, String sourceTag) {
        if (folder == null || !folder.isDirectory()) return null;
        String key = sourceTag + ":" + folder.getName().toLowerCase();
        return load(folder.getName(), folder, key);
    }

    private static CosmeticModel load(String folderName, File externalDir, String catalogId) {
        return load(folderName, externalDir, catalogId, null);
    }

    private static CosmeticModel load(String folderName, File externalDir, String catalogId, CosmeticType forcedType) {
        try {
            String resourcePath = externalDir == null ? normalizeResourcePath(folderName) : folderName;
            JsonObject avatar;
            InputStream avatarStream = openAvatarStream(resourcePath, externalDir);
            if (avatarStream == null) return null;

            try (InputStreamReader reader = new InputStreamReader(avatarStream, StandardCharsets.UTF_8)) {
                avatar = JsonParser.parseReader(reader).getAsJsonObject();
            }

            String leaf = resourceLeaf(resourcePath);
            String name = avatar.has("name") ? avatar.get("name").getAsString() : leaf;
            String desc = avatar.has("description") ? avatar.get("description").getAsString() : "";
            String color = avatar.has("color") ? avatar.get("color").getAsString() : "#ffffff";
            CosmeticType type = forcedType != null ? forcedType : CosmeticType.fromJson(
                    avatar.has("category") ? avatar.get("category").getAsString()
                    : avatar.has("type") ? avatar.get("type").getAsString() : null
            );

            CosmeticModel model = new CosmeticModel(catalogId, name, desc, color, resourcePath, type, externalDir);
            if (sourceTagFromId(catalogId) != null) {
                model.setSource(sourceTagFromId(catalogId));
            }

            InputStream modelStream = openModelStream(resourcePath, externalDir);
            if (modelStream == null) {
                return model;
            }

            JsonObject bbmodel;
            try (InputStreamReader reader = new InputStreamReader(modelStream, StandardCharsets.UTF_8)) {
                bbmodel = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (bbmodel.has("resolution")) {
                JsonObject res = bbmodel.getAsJsonObject("resolution");
                model.textureWidth = res.get("width").getAsInt();
                model.textureHeight = res.get("height").getAsInt();
            }

            model.textureId = resolveTextureId(resourcePath, externalDir);
            model.loadTextureSlots(bbmodel);

            Map<String, JsonObject> elementMap = new HashMap<>();
            if (bbmodel.has("elements")) {
                for (JsonElement el : bbmodel.getAsJsonArray("elements")) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("uuid")) {
                        elementMap.put(obj.get("uuid").getAsString(), obj);
                    }
                }
            }

            Map<String, JsonObject> groupMap = buildGroupMap(bbmodel);

            if (bbmodel.has("outliner")) {
                for (JsonElement el : bbmodel.getAsJsonArray("outliner")) {
                    CosmeticBone bone = parseOutliner(el, elementMap, groupMap, model.textureWidth, model.textureHeight, resourcePath);
                    if (bone != null) {
                        model.rootBones.add(bone);
                        model.indexBone(bone);
                    }
                }
            }

            model.bbAnimations.putAll(CosmeticBbAnimation.parseAll(bbmodel));
            model.postProcess();

            return model;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sourceTagFromId(String catalogId) {
        int idx = catalogId.indexOf(':');
        if (idx <= 0) return null;
        return switch (catalogId.substring(0, idx)) {
            case "figura" -> "Figura";
            case "user" -> "Своя";
            case "builtin" -> null;
            default -> null;
        };
    }

    private static InputStream openAvatarStream(String resourcePath, File externalDir) throws IOException {
        if (externalDir != null) {
            File f = new File(externalDir, "avatar.json");
            if (f.exists()) return new FileInputStream(f);
            return null;
        }
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null || mc.getResourceManager() == null) return null;
        Identifier avatarId = Identifier.of(NAME,
                "cosmetic/" + resourcePath + "/avatar.json");
        return mc.getResourceManager().open(avatarId);
    }

    private static InputStream openModelStream(String resourcePath, File externalDir) throws IOException {
        if (externalDir != null) {
            File model = new File(externalDir, "model.bbmodel");
            if (!model.exists()) {
                model = new File(externalDir, resourceLeaf(resourcePath) + ".bbmodel");
            }
            if (model.exists()) return new FileInputStream(model);
            File[] bbmodels = externalDir.listFiles((dir, name) -> name.endsWith(".bbmodel"));
            if (bbmodels != null && bbmodels.length > 0) {
                return new FileInputStream(bbmodels[0]);
            }
            return null;
        }
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null || mc.getResourceManager() == null) return null;
        String namespace = NAME;
        String folderPrefix = "cosmetic/" + resourcePath + "/";
        String leaf = resourceLeaf(resourcePath);

        Map<Identifier, ?> found = mc.getResourceManager().findResources("cosmetic",
                id -> id.getPath().endsWith(".bbmodel") && id.getPath().startsWith(folderPrefix));
        if (!found.isEmpty()) {
            for (Identifier id : found.keySet()) {
                try {
                    return mc.getResourceManager().open(id);
                } catch (Exception ignored) {
                }
            }
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(folderPrefix + "model.bbmodel");
        candidates.add(folderPrefix + leaf + ".bbmodel");
        if (resourcePath.equals("pet/allay")) {
            candidates.add(folderPrefix + "alay42.bbmodel");
        }
        if (resourcePath.equals("pet/mothli")) {
            candidates.add(folderPrefix + "mothli.bbmodel");
        }
        for (String path : candidates) {
            try {
                return mc.getResourceManager().open(Identifier.of(namespace, path));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Identifier resolveTextureId(String resourcePath, File externalDir) {
        if (externalDir != null) {
            return Identifier.of(NAME,
                    "dynamic/cosmetic/" + resourcePath.replace('/', '_').replace(' ', '_'));
        }
        return Identifier.of(NAME,
                "cosmetic/" + resourcePath + "/outfits/texture.png");
    }

    private void loadTextureSlots(JsonObject bbmodel) {
        textureSlots.clear();
        if (!bbmodel.has("textures")) return;

        String modelKey = id.replace(':', '_');
        for (int i = 0; i < bbmodel.getAsJsonArray("textures").size(); i++) {
            if (!bbmodel.getAsJsonArray("textures").get(i).isJsonObject()) continue;
            JsonObject tex = bbmodel.getAsJsonArray("textures").get(i).getAsJsonObject();
            textureSlots.add(CosmeticTextureSlot.fromJson(i, modelKey, tex));
        }
    }

    public void ensureTextureSlotLoaded(CosmeticTextureSlot slot) {
        if (slot == null || slot.isLoaded()) return;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) return;
        slot.load(mc, externalDirectory, folderName);
    }

    public void loadTexture() {
        if (textureLoaded) return;
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null || mc.getTextureManager() == null) return;

            boolean anyLoaded = false;
            for (CosmeticTextureSlot slot : textureSlots) {
                slot.load(mc, externalDirectory, folderName);
                if (slot.isLoaded()) anyLoaded = true;
            }

            if (!anyLoaded) {
                if (textureId.getPath().startsWith("dynamic/cosmetic/")) {
                    loadExternalTexture(mc);
                } else if (mc.getResourceManager() != null) {
                    String leaf = resourceLeaf(folderName);
                    String[] texturePaths = {
                            "cosmetic/" + folderName + "/outfits/texture.png",
                            "cosmetic/" + folderName + "/avatar.png",
                            "cosmetic/" + folderName + "/texture.png",
                            "cosmetic/" + folderName + "/" + leaf + ".png"
                    };
                    for (String path : texturePaths) {
                        Identifier rawId = Identifier.of(NAME, path);
                        try (InputStream is = mc.getResourceManager().open(rawId)) {
                            registerTexture(mc, rawId, is);
                            anyLoaded = true;
                            break;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else if (!textureSlots.isEmpty() && textureSlots.get(0).isLoaded()) {
                textureId = textureSlots.get(0).getId();
                textureWidth = textureSlots.get(0).getWidth();
                textureHeight = textureSlots.get(0).getHeight();
            }

            textureLoaded = anyLoaded;
            if (!anyLoaded) {
                textureId = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void loadExternalTexture(net.minecraft.client.MinecraftClient mc) {
        File folder = externalDirectory;
        if (folder == null || !folder.isDirectory()) {
            File root = new File(CONFIG_PATH_COSMETICS);
            folder = findExternalFolder(root);
            if (folder == null) {
                folder = new File(new File(GAME_PATH, "figura/avatars"), folderName);
            }
        }
        if (folder == null || !folder.isDirectory()) return;

        String[] names = {"outfits/texture.png", "texture.png", folderName + ".png", "avatar.png"};
        for (String rel : names) {
            File tex = new File(folder, rel);
            if (!tex.exists()) continue;
            try (InputStream is = new FileInputStream(tex)) {
                registerTexture(mc, textureId, is);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private File findExternalFolder(File root) {
        if (!root.isDirectory()) return null;
        for (CosmeticType type : CosmeticType.values()) {
            File typed = new File(new File(root, type.getId().toLowerCase()), folderName);
            if (typed.isDirectory()) return typed;
        }
        File direct = new File(root, folderName);
        if (direct.isDirectory()) return direct;
        return null;
    }

    private void registerTexture(net.minecraft.client.MinecraftClient mc, Identifier rawId, InputStream is) throws IOException {
        net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(is);
        net.minecraft.client.texture.NativeImageBackedTexture tex =
                new net.minecraft.client.texture.NativeImageBackedTexture(img);
        mc.getTextureManager().registerTexture(rawId, tex);
        textureId = rawId;
        textureLoaded = true;
    }

    private void indexBone(CosmeticBone bone) {
        boneMap.put(bone.getName().toLowerCase(), bone);
        boneMapByUuid.put(bone.getUuid(), bone);
        for (CosmeticBone child : bone.getChildren()) indexBone(child);
    }

    public CosmeticBone getBone(String name) {
        return boneMap.get(name.toLowerCase());
    }

    public CosmeticBone getBoneByUuid(String uuid) {
        return boneMapByUuid.get(uuid);
    }

    public boolean hasBbAnimations() {
        return !bbAnimations.isEmpty();
    }

    public Collection<CosmeticBbAnimation> getBbAnimations() {
        return bbAnimations.values();
    }

    public CosmeticBbAnimation findBbAnimation(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null) continue;
            CosmeticBbAnimation anim = bbAnimations.get(candidate.toLowerCase());
            if (anim != null) return anim;
        }
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String lower = candidate.toLowerCase();
            for (Map.Entry<String, CosmeticBbAnimation> entry : bbAnimations.entrySet()) {
                if (entry.getKey().contains(lower) || lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public CosmeticBbAnimation getFirstBbAnimation() {
        for (String preferred : List.of(
                "idling", "idle", "idle_arms", "idleMonsBob", "defaultstate",
                "walking", "yap", "painting", "pose", "pose2"
        )) {
            CosmeticBbAnimation anim = findBbAnimation(preferred);
            if (anim != null) return anim;
        }
        if (bbAnimations.isEmpty()) return null;
        return bbAnimations.values().iterator().next();
    }

    public List<CosmeticBone> getRenderRoots() {
        return renderRoots.isEmpty() ? rootBones : renderRoots;
    }

    public boolean hasCustomLegs() {
        return customLegsPresent;
    }

    public boolean isBowReplacementAccessory() {
        return bowReplacementAccessory;
    }

    public boolean isSwordReplacementAccessory() {
        return swordReplacementAccessory;
    }

    public boolean isBodyOnlyAccessory() {
        return type == CosmeticType.ACCESSORY && !bowReplacementAccessory && !swordReplacementAccessory;
    }

    public boolean hasRestPoseAnimation() {
        return findBbAnimation(
                "idling", "idle", "idle_arms", "idleMonsBob", "defaultstate",
                "walking", "walkingback", "flywalk"
        ) != null;
    }

    public boolean shouldSkipBone(CosmeticBone bone) {
        if (visibleBoneUuids != null) {
            return !visibleBoneUuids.contains(bone.getUuid());
        }
        String lower = bone.getName().toLowerCase();
        String folder = folderName.toLowerCase();
        if (folder.contains("starhorm") && lower.contains("vanilla")) {
            return true;
        }
        if (customLegsPresent && lower.contains("vanilla") && (lower.contains("leg") || lower.contains("foot"))) {
            return true;
        }
        if (lower.contains("rightarm_fp") || lower.contains("leftarm_fp")) {
            return true;
        }
        if (hatOnlyAccessory) {
            return lower.equals("body") || lower.equals("leftarm") || lower.equals("rightarm")
                    || lower.equals("leftleg") || lower.equals("rightleg") || lower.equals("root");
        }
        return false;
    }

    public static CosmeticModel derivePet(CosmeticModel parent, String boneName, String petFolder, String petName) {
        if (parent == null) return null;
        CosmeticBone source = parent.findBoneDeep(boneName);
        if (source == null) return null;

        CosmeticModel pet = new CosmeticModel(
                "builtin:" + petFolder.replace('/', '_'),
                petName,
                "",
                parent.getAccentColor(),
                petFolder,
                CosmeticType.PET,
                null
        );
        pet.source = "Built-in";
        pet.textureWidth = parent.textureWidth;
        pet.textureHeight = parent.textureHeight;
        pet.textureId = parent.textureId;
        pet.textureSlots.addAll(parent.textureSlots);
        pet.textureLoaded = parent.textureLoaded;

        CosmeticBone root = source.deepClone();
        pet.rootBones.add(root);
        pet.renderRoots.add(root);
        pet.indexBone(root);

        Set<String> uuids = new HashSet<>();
        pet.collectUuids(root, uuids);
        pet.animationBoneFilter = uuids;

        for (Map.Entry<String, CosmeticBbAnimation> entry : parent.bbAnimations.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("mons") || key.contains("mini") || key.contains("idleMons".toLowerCase())) {
                pet.bbAnimations.put(entry.getKey(), entry.getValue());
            }
        }
        if (pet.bbAnimations.isEmpty()) {
            pet.bbAnimations.putAll(parent.bbAnimations);
        }
        pet.postProcess();
        return pet;
    }

    private void collectUuids(CosmeticBone bone, Set<String> uuids) {
        uuids.add(bone.getUuid());
        for (CosmeticBone child : bone.getChildren()) collectUuids(child, uuids);
    }

    private CosmeticBone findBoneDeep(String name) {
        String lower = name.toLowerCase();
        for (CosmeticBone root : rootBones) {
            CosmeticBone found = findBoneDeep(root, lower);
            if (found != null) return found;
        }
        return null;
    }

    private static CosmeticBone findBoneDeep(CosmeticBone bone, String lower) {
        if (bone.getName().toLowerCase().equals(lower) || bone.getName().toLowerCase().contains(lower)) {
            return bone;
        }
        for (CosmeticBone child : bone.getChildren()) {
            CosmeticBone found = findBoneDeep(child, lower);
            if (found != null) return found;
        }
        return null;
    }

    private void postProcess() {
        customLegsPresent = boneMap.keySet().stream().anyMatch(n ->
                n.contains("leftleg") || n.contains("rightleg") || n.contains("leftpaw") || n.contains("rightpaw"))
                && boneMap.keySet().stream().noneMatch(n -> n.equals("leftlegvanilla") || n.equals("rightlegvanilla")
                || (n.contains("vanilla") && n.contains("leg")));

        String folder = folderName.toLowerCase();
        bowReplacementAccessory = type == CosmeticType.ACCESSORY && folder.contains("bow");
        swordReplacementAccessory = false;
        hatOnlyAccessory = type == CosmeticType.ACCESSORY
                && (folder.contains("witch") || folder.contains("hat")) && !bowReplacementAccessory;
        wingsOnlyAccessory = type == CosmeticType.ACCESSORY && folder.contains("wing");
        visibleBoneUuids = null;
        if (wingsOnlyAccessory) {
            visibleBoneUuids = buildWingsBoneChainUuids();
        }

        renderRoots.clear();
        for (CosmeticBone root : rootBones) {
            String n = root.getName().toLowerCase();
            if (type == CosmeticType.MODEL) {
                if (n.equals("monst3r") || n.equals("skull") || n.endsWith("_fp")) continue;
            }
            if (type == CosmeticType.PET) {
                if (n.equals("arrow") || n.equals("skull")) continue;
            }
            renderRoots.add(root);
        }
        if (renderRoots.isEmpty()) {
            renderRoots.addAll(rootBones);
        }

        applyAccessorySubtreeFilter();
    }

    private void applyAccessorySubtreeFilter() {
        if (hatOnlyAccessory) {
            CosmeticBone head = findBoneDeep("head");
            CosmeticBone hatBone = findBoneDeep("bone");
            if (head != null && hatBone != null) {
                CosmeticBone mount = new CosmeticBone(
                        head.getName(), head.getUuid(),
                        new Vector3f(head.getPivot()), new Vector3f(head.getDefaultRotation()));
                mount.getChildren().add(hatBone.deepClone());
                renderRoots.clear();
                renderRoots.add(mount);
            }
            return;
        }
    }

    private Set<String> buildWingsBoneChainUuids() {
        Set<String> keep = new HashSet<>();
        CosmeticBone wings = findBoneDeep("wings");
        if (wings == null) return keep;
        collectBoneTreeUuids(wings, keep);
        for (CosmeticBone root : rootBones) {
            markBonePathToUuid(root, wings.getUuid(), keep);
        }
        return keep;
    }

    private static void collectBoneTreeUuids(CosmeticBone bone, Set<String> uuids) {
        uuids.add(bone.getUuid());
        for (CosmeticBone child : bone.getChildren()) {
            collectBoneTreeUuids(child, uuids);
        }
    }

    private static boolean markBonePathToUuid(CosmeticBone bone, String targetUuid, Set<String> keep) {
        keep.add(bone.getUuid());
        if (bone.getUuid().equals(targetUuid)) {
            return true;
        }
        for (CosmeticBone child : bone.getChildren()) {
            if (markBonePathToUuid(child, targetUuid, keep)) {
                return true;
            }
        }
        keep.remove(bone.getUuid());
        return false;
    }

    private static Map<String, JsonObject> buildGroupMap(JsonObject bbmodel) {
        Map<String, JsonObject> map = new HashMap<>();
        if (!bbmodel.has("groups")) return map;
        for (JsonElement el : bbmodel.getAsJsonArray("groups")) {
            if (!el.isJsonObject()) continue;
            JsonObject group = el.getAsJsonObject();
            if (group.has("uuid")) {
                map.put(group.get("uuid").getAsString(), group);
            }
        }
        return map;
    }

    private static Vector3f readVec3(JsonObject obj, String key, Vector3f fallback) {
        if (obj == null || !obj.has(key)) return new Vector3f(fallback);
        JsonArray arr = obj.getAsJsonArray(key);
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private static CosmeticBone parseOutliner(JsonElement element, Map<String, JsonObject> elementMap,
                                              Map<String, JsonObject> groupMap, int texW, int texH,
                                              String resourcePath) {
        if (element.isJsonPrimitive() || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();

        String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : UUID.randomUUID().toString();
        JsonObject group = groupMap.get(uuid);

        String name = "bone";
        if (obj.has("name")) {
            name = obj.get("name").getAsString();
        } else if (group != null && group.has("name")) {
            name = group.get("name").getAsString();
        }

        Vector3f pivot = obj.has("origin")
                ? readVec3(obj, "origin", new Vector3f())
                : readVec3(group, "origin", new Vector3f());

        Vector3f rotation = obj.has("rotation")
                ? readVec3(obj, "rotation", new Vector3f())
                : readVec3(group, "rotation", new Vector3f());

        CosmeticBone bone = new CosmeticBone(name, uuid, pivot, rotation);

        if (obj.has("children")) {
            for (JsonElement child : obj.getAsJsonArray("children")) {
                if (child.isJsonPrimitive()) {
                    String childUuid = child.getAsString();
                    JsonObject cubeObj = elementMap.get(childUuid);
                    if (cubeObj != null) {
                        CosmeticCube cube = parseCube(cubeObj, texW, texH, resourcePath);
                        if (cube != null) bone.getCubes().add(cube);
                    }
                } else {
                    CosmeticBone childBone = parseOutliner(child, elementMap, groupMap, texW, texH, resourcePath);
                    if (childBone != null) bone.getChildren().add(childBone);
                }
            }
        }

        return bone;
    }

    private static CosmeticCube parseCube(JsonObject obj, int texW, int texH, String resourcePath) {
        try {
            JsonArray fromArr = obj.getAsJsonArray("from");
            JsonArray toArr = obj.getAsJsonArray("to");
            Vector3f from = new Vector3f(fromArr.get(0).getAsFloat(), fromArr.get(1).getAsFloat(), fromArr.get(2).getAsFloat());
            Vector3f to = new Vector3f(toArr.get(0).getAsFloat(), toArr.get(1).getAsFloat(), toArr.get(2).getAsFloat());

            float dx = Math.abs(to.x - from.x);
            float dy = Math.abs(to.y - from.y);
            float dz = Math.abs(to.z - from.z);
            float minSize = Math.min(dx, Math.min(dy, dz));
            if (resourcePath != null && resourcePath.contains("howlpendragon") && minSize < 0.05f) {
                String ename = obj.has("name") ? obj.get("name").getAsString().toLowerCase() : "";
                if (ename.contains("shine")) {
                    return null;
                }
            }

            Vector3f rot = new Vector3f(0, 0, 0);
            if (obj.has("rotation")) {
                JsonArray r = obj.getAsJsonArray("rotation");
                rot.set(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat());
            }

            Vector3f origin = new Vector3f(0, 0, 0);
            if (obj.has("origin")) {
                JsonArray o = obj.getAsJsonArray("origin");
                origin.set(o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat());
            }

            float inflate = obj.has("inflate") ? obj.get("inflate").getAsFloat() : 0f;

            CosmeticCube.CubeFace[] faces = new CosmeticCube.CubeFace[6];
            String[] faceNames = {"north", "east", "south", "west", "up", "down"};

            if (obj.has("faces")) {
                JsonObject facesObj = obj.getAsJsonObject("faces");
                for (int i = 0; i < faceNames.length; i++) {
                    String fn = faceNames[i];
                    if (!facesObj.has(fn)) continue;
                    JsonObject face = facesObj.getAsJsonObject(fn);
                    if (!face.has("uv") || !face.get("uv").isJsonArray()) continue;
                    JsonArray uv = face.getAsJsonArray("uv");
                    if (uv.size() < 4) continue;
                    int tex = 0;
                    if (face.has("texture") && !face.get("texture").isJsonNull()) {
                        JsonElement texEl = face.get("texture");
                        if (texEl.isJsonPrimitive() && texEl.getAsJsonPrimitive().isNumber()) {
                            tex = texEl.getAsInt();
                        }
                    }
                    float u1 = uv.get(0).getAsFloat();
                    float v1 = uv.get(1).getAsFloat();
                    float u2 = uv.get(2).getAsFloat();
                    float v2 = uv.get(3).getAsFloat();
                    // реверс диапазона (u2 < u1 / v2 < v1) — это зеркалирование грани в Blockbench,
                    // не нормализуем: quadFace отображает u1/u2 на фиксированные углы квада
                    faces[i] = new CosmeticCube.CubeFace(u1, v1, u2, v2, tex);
                }
            }

            return new CosmeticCube(from, to, rot, origin, inflate, faces, texW, texH);
        } catch (Exception e) {
            return null;
        }
    }
}
