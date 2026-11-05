package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.modules.modules.combat.AuraDev.NeuroAuraSystem;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.game.MessageUtility;
import com.isusdlc.utility.rotations.MoveCorrection;
import com.isusdlc.utility.rotations.Rotation;
import com.isusdlc.utility.rotations.RotationMath;
import com.isusdlc.utility.time.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@ModuleInfo(
   name = "Kill Aura",
   category = ModuleCategory.COMBAT,
   desc = "auto attacking aura"
)
public class KillAura extends BaseModule {

   private final SliderSetting maxDistanceSetting = new SliderSetting(this, "Max Distance").min(1.0F).max(6.0F).step(0.1F).currentValue(3.0F);
   private final BooleanSetting armoredPlayers = new BooleanSetting(this, "Armored Players").enable();
   private final BooleanSetting unarmoredPlayers = new BooleanSetting(this, "Unarmored Players").enable();
   private final BooleanSetting invisiblePlayers = new BooleanSetting(this, "Invisible Players");
   private final BooleanSetting hostileMobs = new BooleanSetting(this, "Hostile Mobs");
   private final BooleanSetting animals = new BooleanSetting(this, "Animals");
   private final BooleanSetting villagers = new BooleanSetting(this, "Villagers");
   private final BooleanSetting golems = new BooleanSetting(this, "Golems");
   private final BooleanSetting phantoms = new BooleanSetting(this, "Phantoms");
   private final BooleanSetting armorStands = new BooleanSetting(this, "Armor Stands");
   private final BooleanSetting friends = new BooleanSetting(this, "Friends");
   private final BooleanSetting raytraceCheck = new BooleanSetting(this, "Raytrace check");
   private final BooleanSetting dynamicCooldown = new BooleanSetting(this, "Dynamic cooldown");
   private final BooleanSetting breakShield = new BooleanSetting(this, "Break shield");
   private final BooleanSetting unpressShield = new BooleanSetting(this, "Un press shield");
   private final BooleanSetting checkUse = new BooleanSetting(this, "Check use");
   private final ModeSetting criticalMode = new ModeSetting(this, "Critical mode");
   private final ModeSetting.Value critNone = new ModeSetting.Value(this.criticalMode, "None");
   private final ModeSetting.Value critOnly = new ModeSetting.Value(this.criticalMode, "Only critical").select();
   private final ModeSetting.Value critAdaptive = new ModeSetting.Value(this.criticalMode, "Adaptive");
    private final ModeSetting rotationMode = new ModeSetting(this, "Rotation mode");
    private final ModeSetting.Value grim = new ModeSetting.Value(this.rotationMode, "Grim");
    private final ModeSetting.Value matrix = new ModeSetting.Value(this.rotationMode, "Matrix");
    private final ModeSetting.Value funtimeSnap = new ModeSetting.Value(this.rotationMode, "FunTime Snap").select();
    private final ModeSetting.Value lonyJir = new ModeSetting.Value(this.rotationMode, "Lony Jir");
    private final ModeSetting.Value reallyworld = new ModeSetting.Value(this.rotationMode, "ReallyWorld");
    private final ModeSetting.Value neuro = new ModeSetting.Value(this.rotationMode, "Neuro");
    private final SliderSetting shakeSpeed = new SliderSetting(this, "Shake speed").min(0.0F).max(20.0F).step(0.1F).currentValue(5.0F);
    private final SliderSetting shakeIntensity = new SliderSetting(this, "Shake intensity").min(0.0F).max(10.0F).step(0.1F).currentValue(2.0F);
    private final ModeSetting moveCorrectionMode = new ModeSetting(this, "Move correction");
   private final ModeSetting.Value corrNone = new ModeSetting.Value(this.moveCorrectionMode, "None");
   private final ModeSetting.Value corrFree = new ModeSetting.Value(this.moveCorrectionMode, "Free").select();
   private final ModeSetting.Value corrDirect = new ModeSetting.Value(this.moveCorrectionMode, "Direct");
   private final ModeSetting sprintMode = new ModeSetting(this, "Sprint mode");
   private final ModeSetting.Value sprintNone = new ModeSetting.Value(this.sprintMode, "None");
   private final ModeSetting.Value sprintLegit = new ModeSetting.Value(this.sprintMode, "Legit").select();
   private final ModeSetting.Value sprintPacket = new ModeSetting.Value(this.sprintMode, "Packet");
   private final ModeSetting versionMode = new ModeSetting(this, "Version");
   private final ModeSetting.Value v1_8 = new ModeSetting.Value(this.versionMode, "1.8");
   private final ModeSetting.Value v1_9 = new ModeSetting.Value(this.versionMode, "1.9").select();
   private final BooleanSetting showAimPoint = new BooleanSetting(this, "Show aim point");
   private final BooleanSetting debugMode = new BooleanSetting(this, "Debug");
   private final BooleanSetting legitTarget = new BooleanSetting(this, "Legit target");
   private final SliderSetting legitSpeed = new SliderSetting(this, "Legit speed").min(0.5F).max(20.0F).step(0.5F).currentValue(6.0F);

   private static final int[] CLICK_INTERVALS = {
      12, 13, 11, 12, 12, 12, 12, 12, 12, 12, 12, 13, 12, 12, 12, 12, 12, 10,
      12, 13, 13, 12, 12, 11, 13, 11, 12, 12, 12, 11, 12, 11, 13, 13, 13, 11, 12, 11, 12, 12, 11, 12, 14, 13,
      12, 12, 11, 12, 11, 12, 13, 12, 12, 11, 13, 12, 12, 11, 13, 12, 12, 13, 12, 12, 12, 12, 11, 13, 11, 11,
      13, 13, 12, 11, 13, 11, 12, 11, 13
   };
   private int clickIndex;
   private boolean alternateTickDelay;
   private long lastClickTime;
   private final Random random = new Random();
    public LivingEntity target;
    private Vec3d currentAimPoint;
    private boolean targetBlocked;
    private boolean wasSprinting;
    private long critWaitStart;
    private boolean shieldDisengaged;
    private boolean stopSprintPacketSent;

    public final NeuroAuraSystem neuroSystem = new NeuroAuraSystem();
    public final Timer attackTimer = new Timer();

    private float lastSnapYaw;
    private float lastSnapPitch;
    private float shakeTime;
    private float lonyJirAcceleration;
    private boolean lonyJirIsBack;

    private float reallyworldLastYaw;
    private float reallyworldLastPitch;
    private boolean reallyworldHasLast;
    private final Random reallyworldRandom = new Random();
    private static final float REALLYWORLD_MAX_YAW_SPEED = 90.0F;
    private static final float REALLYWORLD_MAX_PITCH_SPEED = 60.0F;
    private static final float REALLYWORLD_NOISE_AMPLITUDE = 1.2F;

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
       if (mc.world == null || mc.player == null) return;


       target = findTarget();
       this.targetBlocked = target != null && !raytraceToEntity(target);
        if (target == null) {
           currentAimPoint = null;
           releaseSprint();
           reallyworldHasLast = false;
           if (neuroSystem.isShowStats() && mc.player.age % 20 == 0) {
             mc.player.sendMessage(Text.literal(neuroSystem.getStatusString()), true);
          }
       } else {
          if (rotateToTarget(target)) {
             attackTarget(target);
          }
          if (neuroSystem.isRecording()) {
             neuroSystem.recordTick(target, mc.player.getYaw(), mc.player.getPitch());
          }
       }
    };

   @Override
   public void onDisable() {
      this.critWaitStart = 0L;
      releaseSprint();
      target = null;
      currentAimPoint = null;
      lastSnapYaw = 0;
      lastSnapPitch = 0;
      shakeTime = 0;
      lonyJirAcceleration = 0;
      lonyJirIsBack = false;
      reallyworldHasLast = false;
      super.onDisable();
   }

   private void handleSprintPre() {
      if (sprintPacket.isSelected() && mc.player != null && mc.player.isSprinting()) {
         mc.player.setSprinting(false);
         stopSprintPacketSent = true;
         wasSprinting = true;
      }
   }

   private void handleSprintPost() {
      if (sprintPacket.isSelected() && stopSprintPacketSent) {
         if (mc.player != null) mc.player.setSprinting(true);
         stopSprintPacketSent = false;
      }
   }

   private void releaseSprint() {
      if (wasSprinting) {
         if (mc.player != null) mc.player.setSprinting(true);
         wasSprinting = false;
      }
   }

   private LivingEntity findTarget() {
      if (mc.world == null || mc.player == null) return null;

      float maxDist = maxDistanceSetting.getCurrentValue();
      Vec3d eyePosEarly = mc.player.getEyePos();

      // гистерезис: держим текущую цель, пока она валидна и в радиусе ×1.1 (без фликера между равными)
      LivingEntity current = this.target;
      if (current != null
         && current.isAlive()
         && current.getHealth() > 0.0F
         && getDistanceSqTo(current, eyePosEarly) <= maxDist * maxDist * 1.21F
         && isValidTargetType(current)) {
         return current;
      }

      Box searchBox = mc.player.getBoundingBox().expand(maxDist);
      List<LivingEntity> entities = mc.world.getEntitiesByClass(LivingEntity.class, searchBox,
         e -> e != mc.player && e.isAlive() && e.getHealth() > 0.0F);
      if (entities.isEmpty()) return null;

      Vec3d eyePos = mc.player.getEyePos();
      List<LivingEntity> valid = new ArrayList<>();
      for (LivingEntity e : entities) {
         if (!(getDistanceSqTo(e, eyePos) > maxDist * maxDist) && isValidTargetType(e)) {
            valid.add(e);
         }
      }
      return valid.stream().min(Comparator.comparingDouble(e -> getDistanceSqTo(e, eyePos))).orElse(null);
   }

   private boolean isValidTargetType(LivingEntity entity) {
      if (entity instanceof PlayerEntity player) {
         if (elegant.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            return friends.isEnabled();
         }
         boolean hasArmor = hasAnyArmor(player);
         boolean isInvisible = player.isInvisible();
         if (hasArmor) return armoredPlayers.isEnabled();
         return isInvisible ? invisiblePlayers.isEnabled() : unarmoredPlayers.isEnabled();
      }
      if (entity instanceof HostileEntity) return hostileMobs.isEnabled();
      if (entity instanceof AnimalEntity) return animals.isEnabled();
      if (entity instanceof VillagerEntity) return villagers.isEnabled();
      if (entity instanceof GolemEntity) return golems.isEnabled();
      if (entity instanceof ArmorStandEntity) return armorStands.isEnabled();
      if (entity instanceof PhantomEntity) return phantoms.isEnabled();
      return false;
   }

   private boolean hasAnyArmor(PlayerEntity player) {
      for (ItemStack item : player.getArmorItems()) {
         if (!item.isEmpty()) return true;
      }
      return false;
   }

   private double getDistanceSqTo(LivingEntity entity, Vec3d eyePos) {
      Box box = entity.getBoundingBox();
      double nearestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
      double nearestY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
      double nearestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
      return eyePos.squaredDistanceTo(new Vec3d(nearestX, nearestY, nearestZ));
   }

   private boolean rotateToTarget(LivingEntity target) {
      Vec3d aimPoint = computeAimPoint(target);
      Vec3d delta = aimPoint.subtract(mc.player.getEyePos());
      currentAimPoint = aimPoint;

      float targetYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float targetPitch = (float) (-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));

      if (legitTarget.isEnabled()) {
         float speed = legitSpeed.getCurrentValue();
         float yawDelta = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
         float pitchDelta = MathHelper.wrapDegrees(targetPitch - mc.player.getPitch());
         float maxYawStep = speed * 2.0F;
         float maxPitchStep = speed * 1.2F;
         float newYaw = mc.player.getYaw() + MathHelper.clamp(yawDelta, -maxYawStep, maxYawStep);
         float newPitch = mc.player.getPitch() + MathHelper.clamp(pitchDelta, -maxPitchStep, maxPitchStep);
         newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);
         mc.player.setYaw(newYaw);
         mc.player.setPitch(newPitch);
         return true;
      }

      if (neuro.isSelected() && neuroSystem.isUsingNeuro()) {
         Rotation neuroRot = neuroSystem.getNeuroRotation(target);
         if (neuroRot != null) {
            Rotation currentRot = elegant.getInstance().getRotationHandler().getCurrentRotation();
            float yawDiff = RotationMath.getAngleDifference(currentRot.getYaw(), neuroRot.getYaw());
            float pitchDiff = Math.abs(currentRot.getPitch() - neuroRot.getPitch());
            float yawSpeed = Math.max((90.0F - Math.abs(yawDiff)) / 40.0F, 1.0F) * 25.0F;
            float pitchSpeed = Math.abs(pitchDiff) / 30.0F * 25.0F;

            MoveCorrection correction = corrNone.isSelected()
               ? MoveCorrection.NONE
               : (corrFree.isSelected() ? MoveCorrection.SILENT : MoveCorrection.DIRECT);
            elegant.getInstance().getRotationHandler().rotate(
               neuroRot, correction, yawSpeed, pitchSpeed, 180.0F
            );
            return true;
         }
      }

      if (reallyworld.isSelected()) {
         reallyworldRotation(MathHelper.wrapDegrees(targetYaw), MathHelper.wrapDegrees(targetPitch));
         return true;
      }

      Rotation targetRotation = new Rotation(MathHelper.wrapDegrees(targetYaw), MathHelper.wrapDegrees(targetPitch));
      Rotation finalRotation;
      float yawSpeed = 180.0F;
      float pitchSpeed = 180.0F;
      float returnSpeed = 180.0F;

      if (grim.isSelected()) {
         Rotation currentRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         finalRotation = grimLimit(currentRotation, targetRotation);
      } else if (matrix.isSelected()) {
         Rotation currentRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         finalRotation = matrixLimit(currentRotation, targetRotation);
      } else if (funtimeSnap.isSelected()) {
         finalRotation = funtimeSnapRotation(targetRotation);
         yawSpeed = 360.0F;
         pitchSpeed = 360.0F;
      } else if (lonyJir.isSelected()) {
         float deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastSnapYaw);
         float deltaPitch2 = mc.gameRenderer.getCamera().getPitch() - this.lastSnapPitch;
         if (mc.options.getPerspective() == net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT) {
            deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastSnapYaw);
            deltaPitch2 = -mc.gameRenderer.getCamera().getPitch() - this.lastSnapPitch;
         }
         boolean close = Math.abs(deltaYaw2) <= 3.0F && Math.abs(deltaPitch2) <= 3.0F;
         returnSpeed = close ? 360.0F : 0.0F;
         finalRotation = lonyJirRotation(targetRotation);
      } else {
         finalRotation = targetRotation;
      }

      MoveCorrection correction = corrNone.isSelected()
         ? MoveCorrection.NONE
         : (corrFree.isSelected() ? MoveCorrection.SILENT : MoveCorrection.DIRECT);
      elegant.getInstance().getRotationHandler().rotate(finalRotation, correction, yawSpeed, pitchSpeed, returnSpeed);
      return true;
   }

   private Rotation funtimeSnapRotation(Rotation target) {
      float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - this.lastSnapYaw);
      float deltaPitch = target.getPitch() - this.lastSnapPitch;

      float smoothYaw = this.lastSnapYaw + deltaYaw;
      float smoothPitch = this.lastSnapPitch + deltaPitch;

      double gcd = RotationMath.getGcd();
      smoothYaw -= (smoothYaw - this.lastSnapYaw) % (float) gcd;
      smoothPitch -= (smoothPitch - this.lastSnapPitch) % (float) gcd;

      this.shakeTime += this.shakeSpeed.getCurrentValue() * 0.05F;
      float intensity = this.shakeIntensity.getCurrentValue();
      float shakeYaw = (float) (Math.sin(this.shakeTime * 1.7) * intensity * 0.5);
      float shakePitch = (float) (Math.sin(this.shakeTime * 2.3 + 1.0) * intensity * 0.25);

      Rotation snapRot = new Rotation(smoothYaw + shakeYaw, smoothPitch + shakePitch);

      this.lastSnapYaw = smoothYaw;
      this.lastSnapPitch = smoothPitch;

      return snapRot;
   }

   private Rotation lonyJirRotation(Rotation target) {
      if (mc.player.isGliding()) {
         if (!this.lonyJirIsBack) {
            this.lonyJirAcceleration += 0.005F;
            if (this.lonyJirAcceleration >= 0.13F) {
               this.lonyJirIsBack = true;
            }
         } else {
            if (this.lonyJirAcceleration >= -0.02F) {
               this.lonyJirAcceleration -= 0.005F;
            }
            if (this.lonyJirAcceleration <= -0.02F) {
               this.lonyJirIsBack = false;
            }
         }
      } else if (this.targetBlocked) {
         this.lonyJirAcceleration += 0.0015F;
      } else if (this.lonyJirAcceleration > 0.0F) {
         this.lonyJirAcceleration -= 0.01F;
      }

      float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - this.lastSnapYaw);
      float deltaPitch = target.getPitch() - this.lastSnapPitch;

      float smooth = Math.max(this.lonyJirAcceleration, 0.0F);
      float newYaw = this.lastSnapYaw + deltaYaw * MathHelper.clamp(smooth, 0.0F, 1.0F);
      float newPitch = this.lastSnapPitch + deltaPitch * MathHelper.clamp(smooth / 2.0F, 0.0F, 1.0F);

      double gcd = RotationMath.getGcd();
      newYaw -= (newYaw - this.lastSnapYaw) % (float) gcd;
      newPitch -= (newPitch - this.lastSnapPitch) % (float) gcd;

      Rotation smoothRot = new Rotation(newYaw, newPitch);
      this.lastSnapYaw = smoothRot.getYaw();
      this.lastSnapPitch = smoothRot.getPitch();

      return smoothRot;
   }

   private Rotation grimLimit(Rotation current, Rotation target) {
      float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
      float pitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());
      float rotationDiff = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float straightYaw = Math.abs(yawDelta / rotationDiff) * 180.0F;
      float straightPitch = Math.abs(pitchDelta / rotationDiff) * 180.0F;
      return new Rotation(
         current.getYaw() + MathHelper.clamp(yawDelta, -straightYaw, straightYaw),
         current.getPitch() + MathHelper.clamp(pitchDelta, -straightPitch, straightPitch)
      );
   }

   private Rotation matrixLimit(Rotation current, Rotation target) {
      float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
      float pitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());
      float targetPitch = MathHelper.clamp(target.getPitch(), -80.0F, 80.0F);
      pitchDelta = MathHelper.wrapDegrees(targetPitch - current.getPitch());
      float noiseYaw = (random.nextFloat() - 0.5F) * 0.6F;
      float noisePitch = (random.nextFloat() - 0.5F) * 0.3F;
      float speedYaw = MathHelper.clamp(Math.abs(yawDelta), 0.5F, 6.0F) * 1.2F + random.nextFloat() * 0.5F;
      float speedPitch = MathHelper.clamp(Math.abs(pitchDelta), 0.3F, 3.0F) * 0.8F;
      return new Rotation(
         current.getYaw() + MathHelper.clamp(yawDelta, -speedYaw, speedYaw) + noiseYaw,
         current.getPitch() + MathHelper.clamp(pitchDelta, -speedPitch, speedPitch) + noisePitch
      );
   }

   private void reallyworldRotation(float targetYaw, float targetPitch) {
      targetYaw = MathHelper.wrapDegrees(targetYaw);
      targetPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);

      float currentYaw = reallyworldHasLast ? reallyworldLastYaw : mc.player.getYaw();
      float currentPitch = reallyworldHasLast ? reallyworldLastPitch : mc.player.getPitch();

      float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
      float pitchDelta = targetPitch - currentPitch;

      float stepYaw = MathHelper.clamp(yawDelta, -REALLYWORLD_MAX_YAW_SPEED, REALLYWORLD_MAX_YAW_SPEED);
      float stepPitch = MathHelper.clamp(pitchDelta, -REALLYWORLD_MAX_PITCH_SPEED, REALLYWORLD_MAX_PITCH_SPEED);

      float newYaw = currentYaw + stepYaw;
      float newPitch = currentPitch + stepPitch;

      double gcd = RotationMath.getGcd();
      newYaw -= (newYaw - currentYaw) % (float) gcd;
      newPitch -= (newPitch - currentPitch) % (float) gcd;

      if (Math.abs(yawDelta) > 2.0F || Math.abs(pitchDelta) > 1.0F) {
         newYaw += (reallyworldRandom.nextFloat() - 0.5F) * REALLYWORLD_NOISE_AMPLITUDE;
         newPitch += (reallyworldRandom.nextFloat() - 0.5F) * (REALLYWORLD_NOISE_AMPLITUDE * 0.5F);
         newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);
      }

      newYaw = MathHelper.wrapDegrees(newYaw);

      boolean yawChanged = Math.abs(MathHelper.wrapDegrees(newYaw - currentYaw)) > 1.0F;
      boolean pitchChanged = Math.abs(newPitch - currentPitch) > 1.0F;

      if (yawChanged || pitchChanged) {
         mc.player.setYaw(newYaw);
         mc.player.setPitch(newPitch);
         mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(newYaw, newPitch, mc.player.isOnGround(), true)
         );
         reallyworldLastYaw = newYaw;
         reallyworldLastPitch = newPitch;
         reallyworldHasLast = true;
      }
   }

   private boolean raytraceToEntity(LivingEntity entity) {
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d toTarget = entity.getBoundingBox().getCenter().subtract(eyePos);
      RaycastContext ctx = new RaycastContext(
         eyePos,
         eyePos.add(toTarget.normalize().multiply(maxDistanceSetting.getCurrentValue())),
         RaycastContext.ShapeType.COLLIDER,
         RaycastContext.FluidHandling.NONE,
         mc.player
      );
      return mc.world.raycast(ctx).getType() == HitResult.Type.MISS;
   }

   private Vec3d computeAimPoint(LivingEntity entity) {
      float maxDist = maxDistanceSetting.getCurrentValue();
      Box entityBox = entity.getBoundingBox().expand(-0.18);
      double stepY = entityBox.getLengthY() / 10.0;
      Vec3d eyePos = mc.player.getEyePos();
      List<Vec3d> candidates = new ArrayList<>();

      for (double y = entityBox.minY; y <= entityBox.maxY; y += stepY) {
         Vec3d point = new Vec3d(entityBox.getCenter().x, y, entityBox.getCenter().z);
         if (eyePos.squaredDistanceTo(point) < maxDist * maxDist) {
            candidates.add(point);
         }
      }

      if (candidates.isEmpty()) {
         return new Vec3d(entityBox.getCenter().x, MathHelper.clamp(eyePos.y, entityBox.minY, entityBox.maxY), entityBox.getCenter().z);
      }

      Vec3d currentRotationVec = mc.player.getRotationVecClient();
      float currentYaw = (float) Math.toDegrees(Math.atan2(currentRotationVec.z, currentRotationVec.x)) - 90.0F;
      float currentPitch = (float) (-Math.toDegrees(
         Math.atan2(currentRotationVec.y, Math.sqrt(currentRotationVec.x * currentRotationVec.x + currentRotationVec.z * currentRotationVec.z))));

      return candidates.stream().min(Comparator.comparingDouble(point -> {
         Vec3d d = point.subtract(eyePos);
         float py = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0F;
         float pp = (float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
         float dy = MathHelper.wrapDegrees(py - currentYaw);
         float dp = MathHelper.wrapDegrees(pp - currentPitch);
         return Math.hypot(dy, dp);
      })).orElse(entity.getEyePos());
   }

    private void attackTarget(LivingEntity target) {
       if (canAttack()) {
          if (neuroSystem.isRecording()) {
             neuroSystem.recordAttack(target, mc.player.getYaw(), mc.player.getPitch());
          }

          handleSprintPre();
          mc.interactionManager.attackEntity(mc.player, target);
          mc.player.resetLastAttackedTicks();
          mc.player.swingHand(Hand.MAIN_HAND);
          lastClickTime = System.currentTimeMillis();
          attackTimer.reset();
          handleSprintPost();
       }
    }

   private boolean canAttack() {
      if (isRaytraceBlocked()) return false;
      if (isCooldownBlocked()) return false;
      if (isPlayerUsingBlocked()) return false;
      if (critOnly.isSelected() && !hasMovementRestrictions()) {
         if (isPlayerInCriticalState()) {
            this.critWaitStart = 0L;
            return true;
         }

         // фолбэк: цель в зоне, но крита нет 200 мс — бьём без крита
         if (this.critWaitStart == 0L) {
            this.critWaitStart = System.currentTimeMillis();
         }

         return System.currentTimeMillis() - this.critWaitStart >= 200L;
      }
      if (critAdaptive.isSelected() && !hasMovementRestrictions()) {
         return mc.player.isOnGround() ? true : isPlayerInCriticalState();
      }
      return true;
   }

   private boolean isRaytraceBlocked() {
      if (!raytraceCheck.isEnabled()) return false;
      return this.targetBlocked;
   }

   private boolean isCooldownBlocked() {
      if (v1_8.isSelected()) {
         if (dynamicCooldown.isEnabled()) {
            int tickDelay = alternateTickDelay ? 1 : 2;
            alternateTickDelay = !alternateTickDelay;
            return lastClickPassed() < (tickDelay * 50L);
         }
         return false;
      }

      long delay = dynamicCooldown.isEnabled() ? calculateAverageCooldown() * 45L : 50L;
      float cooldownProgress = mc.player.getAttackCooldownProgress(0.5F);
      return lastClickPassed() < delay || cooldownProgress < 0.9F;
   }

   private long lastClickPassed() {
      return System.currentTimeMillis() - lastClickTime;
   }

   private int calculateAverageCooldown() {
      if (clickIndex >= CLICK_INTERVALS.length) clickIndex = 0;
      return CLICK_INTERVALS[clickIndex++];
   }

   private boolean isPlayerUsingBlocked() {
      if (!checkUse.isEnabled()) return false;
      if (mc.player.isUsingItem()) {
         if (mc.player.isBlocking() && unpressShield.isEnabled()) {
            if (!this.shieldDisengaged) {
               mc.interactionManager.stopUsingItem(mc.player);
               this.shieldDisengaged = true;
            }
            return false;
         }
         return true;
      }

      this.shieldDisengaged = false;
      return false;
   }

   private boolean hasMovementRestrictions() {
      return mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         || mc.player.isSubmergedInWater()
         || mc.player.isInLava()
         || mc.player.isClimbing()
         || mc.player.getAbilities().flying;
   }

   private boolean isPlayerInCriticalState() {
      double randomThreshold = 0.05 + random.nextDouble() * 0.05;
      return !mc.player.isOnGround()
         && mc.player.fallDistance > randomThreshold
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
   }
}
