package com.isusdlc.systems.modules.modules.combat.AuraDev;

import com.isusdlc.elegant;
import com.isusdlc.systems.modules.modules.combat.KillAura;
import com.isusdlc.utility.math.MathUtility;
import com.isusdlc.utility.math.PerlinNoise;
import com.isusdlc.utility.rotations.Rotation;
import com.isusdlc.utility.rotations.RotationMath;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.isusdlc.utility.interfaces.IMinecraft.mc;

public class NeuroAuraSystem {

    @Getter
    private final List<NeuroPattern> recordedPatterns = new CopyOnWriteArrayList<>();
    @Getter @Setter
    private boolean isRecording = false;
    @Getter @Setter
    private boolean isUsingNeuro = false;
    @Getter @Setter
    private boolean showStats = true;
    @Getter @Setter
    private String currentPatternName = null;

    @Getter
    private String lastDebugMessage = "§7Готов";
    private long lastRecordTime = 0;
    private final String patternsDirectory = "neuro_patterns";
    @Getter
    private int recordedThisSession = 0;
    private long sessionStartTime = 0;
    private PerlinNoise noise = new PerlinNoise();
    private long startTime = System.currentTimeMillis();
    private float smoothFactor = 0.3f;
    private Vec3d lastTargetPos = null;
    private Rotation lastRotation = null;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private float noiseFactor = 1.0F;
    private int currentPointIndex = 0;
    private final List<Vec3d> targetPoints = new ArrayList<>();
    private static final long MIN_RECORD_INTERVAL = 50;
    private int tickCounter = 0;

    public NeuroAuraSystem() {
        createPatternsDirectory();
        startTime = System.currentTimeMillis();
    }

    private void createPatternsDirectory() {
        try {
            Path path = Paths.get(patternsDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recordTick(LivingEntity target, float currentYaw, float currentPitch) {
        if (!isRecording) return;
        if (target == null || mc.player == null) return;

        tickCounter++;
        if (tickCounter % 2 != 0) return;

        long now = System.currentTimeMillis();
        if (now - lastRecordTime < MIN_RECORD_INTERVAL) return;

        Rotation perfectRotation = RotationMath.getRotationTo(target.getEyePos());
        float yawDiff = MathHelper.wrapDegrees(currentYaw - perfectRotation.getYaw());
        float pitchDiff = currentPitch - perfectRotation.getPitch();

        double distance = mc.player.getEyePos().distanceTo(target.getEyePos());
        boolean isCritical = mc.player.fallDistance > 0 && !mc.player.isOnGround();
        double targetSpeed = target.getVelocity().horizontalLength();

        String targetType = (target instanceof PlayerEntity) ? "player" : "mob";

        NeuroPattern pattern = new NeuroPattern(
                yawDiff,
                pitchDiff,
                distance,
                isCritical,
                targetSpeed,
                targetType
        );

        recordedPatterns.add(pattern);
        lastRecordTime = now;
        recordedThisSession++;
        while (recordedPatterns.size() > 3000) {
            recordedPatterns.remove(0);
        }
        if (recordedThisSession % 20 == 0) {
            lastDebugMessage = "§aЗаписано: §f" + recordedPatterns.size() + " паттернов";
        }
    }

    public void recordAttack(LivingEntity target, float currentYaw, float currentPitch) {
        recordTick(target, currentYaw, currentPitch);
    }

    public Rotation getNeuroRotation(LivingEntity target) {
        if (!isUsingNeuro) {
            lastDebugMessage = "§cNeuro выкл";
            return null;
        }
        if (recordedPatterns.isEmpty()) {
            lastDebugMessage = "§cНет паттернов";
            return null;
        }
        if (target == null || mc.player == null) return null;

        Rotation perfectRotation = RotationMath.getRotationTo(target.getEyePos());
        double currentDistance = mc.player.getEyePos().distanceTo(target.getEyePos());
        double targetSpeed = target.getVelocity().horizontalLength();

        List<NeuroPattern> similarPatterns = new ArrayList<>();
        for (NeuroPattern p : recordedPatterns) {
            double distDiff = Math.abs(p.getDistance() - currentDistance);
            double speedDiff = Math.abs(p.getTargetSpeed() - targetSpeed);
            if (distDiff < 2.0 && speedDiff < 0.7) {
                similarPatterns.add(p);
            }
        }
        if (similarPatterns.isEmpty()) {
            int start = Math.max(0, recordedPatterns.size() - 800);
            similarPatterns = recordedPatterns.subList(start, recordedPatterns.size());
        }

        float avgYawDiff = 0;
        float avgPitchDiff = 0;
        float totalWeight = 0;
        long currentTime = System.currentTimeMillis();
        for (NeuroPattern p : similarPatterns) {
            float ageWeight = (float) Math.exp(-(currentTime - p.getTimestamp()) / 180000.0);
            float distWeight = 1.0f - (float) Math.min(1.0, Math.abs(p.getDistance() - currentDistance) / 5.0);
            float weight = ageWeight * distWeight;
            avgYawDiff += p.getYaw() * weight;
            avgPitchDiff += p.getPitch() * weight;
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            avgYawDiff /= totalWeight;
            avgPitchDiff /= totalWeight;
        }

        float baseYaw = perfectRotation.getYaw() + avgYawDiff;
        float basePitch = perfectRotation.getPitch() + avgPitchDiff;

        if (mc.player != null && mc.player.age % 500 == 0) {
            this.noise = new PerlinNoise();
            this.noiseFactor = 1.0F;
        }

        Vec3d nearY = RotationMath.getNearestPoint(target);
        float targetY = (float) MathHelper.clamp(
                MathUtility.interpolate(mc.player.getY(), target.getEyeY(), 0.5),
                target.getBoundingBox().minY,
                target.getBoundingBox().maxY
        );
        Rotation funTargetRot = RotationMath.getRotationTo(new Vec3d(nearY.x, targetY, nearY.z));

        KillAura aura = elegant.getInstance().getModuleManager().getModule(KillAura.class);
        boolean idle = aura != null && aura.attackTimer.finished(300L);

        float finalYaw = baseYaw;
        float finalPitch = basePitch;

        if (idle) {
            finalYaw += 5.0F;
            finalPitch -= 10.0F;
        }

        long timeElapsed = System.currentTimeMillis() - startTime;
        float yawNoise = (float) this.noise.noise(timeElapsed * 5.0E-4);
        float pitchNoise = (float) this.noise.noise(timeElapsed * 5.0E-4, 10.0);

        if (lastYaw != 0 && lastPitch != 0) {
            float yawDiff = RotationMath.getAngleDifference(finalYaw, lastYaw);
            float pitchDiff = Math.abs(finalPitch - lastPitch);
            if (Math.abs(yawDiff) > 30) {
                finalYaw = lastYaw + yawDiff * 0.3f;
            }
            if (pitchDiff > 20) {
                finalPitch = lastPitch + (finalPitch - lastPitch) * 0.3f;
            }
            finalYaw = lastYaw + (finalYaw - lastYaw) * smoothFactor;
            finalPitch = lastPitch + (finalPitch - lastPitch) * smoothFactor;
        }

        float yawDiff = RotationMath.getAngleDifference(finalYaw, funTargetRot.getYaw());
        float pitchDiff = Math.abs(finalPitch - funTargetRot.getPitch());
        float totalDiff = Math.abs(yawDiff) + Math.abs(pitchDiff);
        if (totalDiff < 10.0F) {
            this.noiseFactor = Math.max(0.0F, this.noiseFactor - 0.05F);
        }

        finalYaw += yawNoise * 5.0F * this.noiseFactor;
        finalPitch += pitchNoise * 5.0F * this.noiseFactor;
        finalPitch = MathHelper.clamp(finalPitch, -90.0F, 90.0F);
        finalYaw = MathHelper.wrapDegrees(finalYaw);

        if (lastTargetPos == null || !lastTargetPos.equals(target.getPos())) {
            updateTargetPoints(target);
            lastTargetPos = target.getPos();
        }

        if (!targetPoints.isEmpty()) {
            currentPointIndex = (currentPointIndex + 1) % targetPoints.size();
            Vec3d targetPoint = targetPoints.get(currentPointIndex);
            Rotation pointRotation = RotationMath.getRotationTo(targetPoint);
            float mixFactor = 0.01f;
            if (idle) {
                mixFactor = 0.0f;
            }
            finalYaw = finalYaw * (1 - mixFactor) + pointRotation.getYaw() * mixFactor;
            finalPitch = finalPitch * (1 - mixFactor) + pointRotation.getPitch() * mixFactor;
            finalYaw = MathHelper.wrapDegrees(finalYaw);
            finalPitch = MathHelper.clamp(finalPitch, -90.0F, 90.0F);
        }

        lastDebugMessage = String.format("§aНейро+Fun: Y%.1f° P%.1f° | Шум: %.2f",
                avgYawDiff, avgPitchDiff, this.noiseFactor);

        this.lastYaw = finalYaw;
        this.lastPitch = finalPitch;

        return new Rotation(finalYaw, finalPitch);
    }

    private void updateTargetPoints(LivingEntity target) {
        targetPoints.clear();
        Box box = target.getBoundingBox();
        Vec3d center = target.getEyePos();
        targetPoints.add(center);
        targetPoints.add(new Vec3d(center.x, box.minY + 0.1, center.z));
        targetPoints.add(new Vec3d(center.x, box.maxY - 0.1, center.z));
        targetPoints.add(new Vec3d(box.minX + 0.2, center.y, center.z));
        targetPoints.add(new Vec3d(box.maxX - 0.2, center.y, center.z));
        targetPoints.add(new Vec3d(center.x, center.y, box.minZ + 0.2));
        targetPoints.add(new Vec3d(center.x, center.y, box.maxZ - 0.2));
        for (int i = 0; i < 3; i++) {
            double x = MathUtility.random(box.minX, box.maxX);
            double y = MathUtility.random(box.minY, box.maxY);
            double z = MathUtility.random(box.minZ, box.maxZ);
            targetPoints.add(new Vec3d(x, y, z));
        }
        Collections.shuffle(targetPoints);
    }

    public void savePatterns(String profileName) {
        if (recordedPatterns.isEmpty()) {
            lastDebugMessage = "§cНет паттернов";
            return;
        }

        try {
            String filename = patternsDirectory + "/" + profileName + ".neuro";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(new ArrayList<>(recordedPatterns));
            }
            currentPatternName = profileName;
            lastDebugMessage = "§aСохранено " + recordedPatterns.size() + " паттернов";
        } catch (IOException e) {
            lastDebugMessage = "§cОшибка сохранения";
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadPatterns(String profileName) {
        String filename = patternsDirectory + "/" + profileName + ".neuro";
        File file = new File(filename);
        if (!file.exists()) {
            lastDebugMessage = "§eНет сохранений: " + profileName;
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            List<NeuroPattern> loaded = (List<NeuroPattern>) ois.readObject();
            recordedPatterns.clear();
            recordedPatterns.addAll(loaded);
            currentPatternName = profileName;
            lastDebugMessage = "§aЗагружено " + recordedPatterns.size() + " паттернов";
        } catch (IOException | ClassNotFoundException e) {
            lastDebugMessage = "§cОшибка загрузки";
            e.printStackTrace();
        }
    }

    public int getPatternCount() {
        return recordedPatterns.size();
    }

    public void startRecording() {
        isRecording = true;
        recordedThisSession = 0;
        sessionStartTime = System.currentTimeMillis();
        lastDebugMessage = "§aЗапись начата";
    }

    public void stopRecording() {
        isRecording = false;
        lastDebugMessage = "§eЗапись остановлена";
    }

    public void clearPatterns() {
        recordedPatterns.clear();
        recordedThisSession = 0;
        currentPatternName = null;
        lastDebugMessage = "§eПаттерны очищены";
    }

    public String getStatusString() {
        String status = "§8[§bNeuro§8] §fПаттернов: §e" + recordedPatterns.size();
        if (isRecording) {
            status += " §a[ЗАПИСЬ";
            if (recordedThisSession > 0) {
                status += " +" + recordedThisSession;
            }
            status += "]";
        }
        if (isUsingNeuro) {
            status += " §b[АКТИВЕН";
            if (currentPatternName != null) {
                status += " §7(" + currentPatternName + ")";
            }
            status += "]";
        }
        return status;
    }

    public List<String> getPatternNames() {
        List<String> patterns = new ArrayList<>();
        File dir = new File(patternsDirectory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".neuro"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    patterns.add(name.substring(0, name.length() - 6));
                }
            }
        }
        return patterns;
    }
}
