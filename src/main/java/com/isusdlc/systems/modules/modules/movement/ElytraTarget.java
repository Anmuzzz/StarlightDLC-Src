package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.network.SendPacketEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.modules.modules.combat.KillAura;
import com.isusdlc.systems.setting.settings.BindSetting;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.rotations.MoveCorrection;
import com.isusdlc.utility.rotations.Rotation;
import com.isusdlc.utility.rotations.RotationPriority;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
   name = "Elytra Target",
   category = ModuleCategory.MOVEMENT,
   desc = "Target players to elytra!"
)
public class ElytraTarget extends BaseModule {

    public final ModeSetting mode = new ModeSetting(this, "Mode");
    public final ModeSetting.Value normal = new ModeSetting.Value(this.mode, "Обычный").select();
    public final ModeSetting.Value advanced = new ModeSetting.Value(this.mode, "Продвинутый");

    private final SliderSetting sila = new SliderSetting(this, "Отлёт", () -> !this.mode.is(this.advanced))
       .min(5.0F).max(40.0F).step(1.0F).currentValue(8.0F);
    private final SliderSetting silaTime = new SliderSetting(this, "Время отлёта", () -> !this.mode.is(this.advanced))
       .min(200.0F).max(1000.0F).step(10.0F).currentValue(400.0F);

    private final BooleanSetting perelet = new BooleanSetting(this, "Перелёт").enable();
    private final SliderSetting predict = new SliderSetting(this, "Предикт", () -> !this.perelet.isEnabled())
       .min(0.1F).max(6.0F).step(0.1F).currentValue(2.6F);

    private final BooleanSetting leaveHP = new BooleanSetting(this, "Улетать", () -> !this.mode.is(this.advanced)).enable();
    private final BooleanSetting leaveLowHP = new BooleanSetting(this, "Улетать при мало здоровья", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));
    private final BooleanSetting leaveUsingItem = new BooleanSetting(this, "Улетать при исп. предмета", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));
    private final BooleanSetting leaveShieldPush = new BooleanSetting(this, "Улетать при отжиме щита", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));

    private final BooleanSetting resolver = new BooleanSetting(this, "Resolver уклонение", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting resolverStrength = new SliderSetting(this, "Сила резольвера", () -> !(this.resolver.isEnabled() && this.mode.is(this.advanced)))
       .min(4.0F).max(30.0F).step(1.0F).currentValue(12.0F);

    public final BooleanSetting prefer = new BooleanSetting(this, "Менять направление", () -> !this.mode.is(this.advanced)).enable();
    public final BindSetting preferBind = new BindSetting(this, "Prefer bind");

    private final BooleanSetting preferNorth = new BooleanSetting(this, "Pref Север", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferSouth = new BooleanSetting(this, "Pref Юг", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferWest = new BooleanSetting(this, "Pref Запад", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferEast = new BooleanSetting(this, "Pref Восток", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferUp = new BooleanSetting(this, "Pref Верх", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferDown = new BooleanSetting(this, "Pref Вниз", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced)));

    private final BooleanSetting avoidWalls = new BooleanSetting(this, "Избегать стены", () -> !this.mode.is(this.advanced)).enable();
    private final BooleanSetting firework = new BooleanSetting(this, "Отправка фейерверков", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting fireworkTime = new SliderSetting(this, "Время отправки", () -> !(this.firework.isEnabled() && this.mode.is(this.advanced)))
       .min(100.0F).max(2000.0F).step(10.0F).currentValue(800.0F);

    private final BooleanSetting fakelags = new BooleanSetting(this, "Fake Lags", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting fakelagsDistance = new SliderSetting(this, "Дистанция флагера", () -> !(this.fakelags.isEnabled() && this.mode.is(this.advanced)))
       .min(5.0F).max(20.0F).step(0.5F).currentValue(10.0F);
    private final SliderSetting fakelagsMaxDistance = new SliderSetting(this, "Макс дистанция флагера", () -> !(this.fakelags.isEnabled() && this.mode.is(this.advanced)))
       .min(15.0F).max(50.0F).step(1.0F).currentValue(30.0F);

    private final CopyOnWriteArrayList<Packet<?>> packetBuffer = new CopyOnWriteArrayList<>();
    private boolean isAccumulatingPackets = false;
    private boolean wasInFarRange = false;
    private LivingEntity currentTarget = null;
    private Vec3d defensivePos = null;
    private long defensiveTimer = 0L;

    private final BooleanSetting visual = new BooleanSetting(this, "Рендерить позицию");

    public boolean defensiveActive, lastDefensive;

    private int rotationPhase = 0;
    private int directionIndex = 0;
    private long lastHitTime = 0;
    private LivingEntity lastTarget = null;
    private long targetLastAttackTime = 0;

    private final Map<String, Vec3d> namedDirections = Map.of(
            "Север", new Vec3d(0, 0, -1),
            "Юг", new Vec3d(0, 0, 1),
            "Запад", new Vec3d(-1, 0, 0),
            "Восток", new Vec3d(1, 0, 0),
            "Верх", new Vec3d(0, 1, 0),
            "Вниз", new Vec3d(0, -1, 0)
    );

    private List<Vec3d> airDirections = new ArrayList<>();
    private long fireworkTimer = 0L;
    private int x, y, z;

    private long lastPacketSend = 0L;
    private long lastFireworkCheck = 0L;

    private final EventListener<SendPacketEvent> onPacket = event -> {
        if (mc.player == null || mc.world == null) return;
        if (!this.mode.is(this.advanced) || !mc.player.isGliding()) return;
        if (!fakelags.isEnabled()) return;

        if (isAccumulatingPackets && !event.isCancelled() && !(event.getPacket() instanceof KeepAliveC2SPacket)) {
            packetBuffer.add(event.getPacket());
            event.cancel();
        }
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (!this.mode.is(this.advanced) || !mc.player.isGliding()) return;

        updateFakeLagsState();

        if (firework.isEnabled()) {
            long now = System.currentTimeMillis();
            if (now - lastFireworkCheck > fireworkTime.getCurrentValue()) {
                inventorySwap(new ItemStack(Items.FIREWORK_ROCKET));
                lastFireworkCheck = now;
            }
        }

        defensiveActive = shouldLeave();
        lastDefensive = defensiveActive;

        if (resolver.isEnabled() && rotationPhase != 0) {
            float evadePitch = (float) (Math.sin(System.currentTimeMillis() / 100.0) * resolverStrength.getCurrentValue());
            float evadeYaw = (float) (Math.cos(System.nanoTime() / 100.0) * resolverStrength.getCurrentValue());
            float cy = getCurrentYaw();
            float cp = getCurrentPitch();
            rotate(cy + evadeYaw, MathHelper.clamp(cp + evadePitch, -89.9f, 89.9f));
        }

        if (rotationPhase != 0 && System.currentTimeMillis() - lastHitTime > silaTime.getCurrentValue()) {
            rotationPhase = 0;
        }
    };

    private final EventListener<Render3DEvent> onRender = event -> {
        if (!this.mode.is(this.advanced)) return;

        if (defensivePos != null && fakelags.isEnabled() && isAccumulatingPackets) {
        }
    };

    @Override
    public void onEnable() {
        rebuildAirDirections();
        packetBuffer.clear();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
    }

    private void rebuildAirDirections() {
        airDirections.clear();
        boolean isFlying = mc.player != null && mc.player.isGliding();

        addIfEnabled(preferNorth, isFlying, "Север");
        addIfEnabled(preferSouth, isFlying, "Юг");
        addIfEnabled(preferWest, isFlying, "Запад");
        addIfEnabled(preferEast, isFlying, "Восток");
        addIfEnabled(preferUp, isFlying, "Верх");
        addIfEnabled(preferDown, isFlying, "Вниз");

        if (airDirections.isEmpty()) {
            for (Map.Entry<String, Vec3d> entry : namedDirections.entrySet()) {
                if (entry.getKey().equals("Вниз") && !isFlying) continue;
                airDirections.add(entry.getValue());
            }
        }
        if (directionIndex >= airDirections.size()) directionIndex = 0;
    }

    private void addIfEnabled(BooleanSetting setting, boolean isFlying, String name) {
        if (setting.isEnabled()) {
            if (name.equals("Вниз") && !isFlying) return;
            Vec3d dir = namedDirections.get(name);
            if (dir != null) airDirections.add(dir);
        }
    }

    private void updateFakeLagsState() {
        if (!fakelags.isEnabled()) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            return;
        }

        boolean isFlying = mc.player.isGliding();
        KillAura killAura = elegant.getInstance().getModuleManager().getModuleSafe(KillAura.class);
        LivingEntity target = killAura != null ? killAura.target : null;
        boolean hasTarget = target != null;

        if (target != null && !target.equals(currentTarget)) {
            currentTarget = target;
            if (isAccumulatingPackets) {
                sendBufferedPackets();
            }
        }

        if (!isFlying || !hasTarget || perelet.isEnabled() || defensiveActive) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }

        double distanceToTarget = mc.player.getPos().distanceTo(target.getPos());
        double triggerDistance = fakelagsDistance.getCurrentValue();
        double maxDistance = fakelagsMaxDistance.getCurrentValue();

        if (distanceToTarget > maxDistance) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }
        if (distanceToTarget > triggerDistance && distanceToTarget <= maxDistance) {
            if (!isAccumulatingPackets) {
                isAccumulatingPackets = true;
                defensivePos = mc.player.getPos();
                defensiveTimer = System.currentTimeMillis();
            }
            wasInFarRange = true;
        } else if (distanceToTarget <= triggerDistance && wasInFarRange) {
            sendBufferedPackets();
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        } else if (distanceToTarget <= triggerDistance) {
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        }

        if (isAccumulatingPackets && System.currentTimeMillis() - defensiveTimer > 1500) {
            sendBufferedPackets();
            isAccumulatingPackets = true;
            defensiveTimer = System.currentTimeMillis();
        }
    }

    private void sendBufferedPackets() {
        if (packetBuffer.isEmpty()) return;
        for (Packet<?> packet : packetBuffer) {
            mc.getNetworkHandler().sendPacket(packet);
        }
        packetBuffer.clear();
    }

    @Override
    public void onDisable() {
        sendBufferedPackets();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
        rotationPhase = 0;
    }

    private boolean shouldLeave() {
        boolean lowHP = mc.player.getHealth() <= 4.0f && leaveLowHP.isEnabled();
        boolean usingItem = mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD) && leaveUsingItem.isEnabled();
        return leaveHP.isEnabled() && (lowHP || usingItem);
    }

    private void inventorySwap(ItemStack item) {
        // фейерверк в оффхенде — используем сразу
        if (mc.player.getOffHandStack().getItem() == item.getItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
            return;
        }

        // иначе ищем в хотбаре: выбор слота + использование (буст пока в полёте)
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item.getItem()) {
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    public String getMode() {
        return mode.getValue().getName();
    }

    private void rotate(float yaw, float pitch) {
        Rotation rot = new Rotation(yaw, pitch);
        elegant.getInstance().getRotationHandler().rotate(rot, MoveCorrection.DIRECT, 180f, 180f, 180f, RotationPriority.OVERRIDE);
    }

    private float getCurrentYaw() {
        return elegant.getInstance().getRotationHandler().getCurrentRotation().getYaw();
    }

    private float getCurrentPitch() {
        return elegant.getInstance().getRotationHandler().getCurrentRotation().getPitch();
    }
}
