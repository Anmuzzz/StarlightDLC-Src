package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.render.HudRenderEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.interfaces.IScaledResolution;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "PVP AI",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.pvp_ai"
)
public class PVPAI extends BaseModule implements IMinecraft, IScaledResolution {
   private final Map<PlayerEntity, Float> playerWinChances = new HashMap<>();
   private float ourWinChance = 50.0F;
   private final BooleanSetting damageIndicator = new BooleanSetting(this, "modules.settings.pvp_ai.damage_indicator").enable();
   private final SliderSetting damageSize = new SliderSetting(this, "modules.settings.pvp_ai.damage_size", () -> !this.damageIndicator.isEnabled())
      .min(0.5F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(1.0F);
   private final ColorSetting damageColor = new ColorSetting(this, "modules.settings.pvp_ai.damage_color", () -> !this.damageIndicator.isEnabled())
      .color(new ColorRGBA(255.0F, 85.0F, 85.0F, 255.0F));
   private final List<PVPAI.DamageNumber> damageNumbers = new ArrayList<>();
   private final Map<LivingEntity, Float> entityHealthMap = new HashMap<>();
   private final EventListener<ClientPlayerTickEvent> onTickEvent = event -> {
      if (this.isEnabled() && this.damageIndicator.isEnabled()) {
         if (mc.player != null && mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof LivingEntity living && living != mc.player && !living.isRemoved()) {
                  float currentHealth = living.getHealth() + living.getAbsorptionAmount();
                  if (this.entityHealthMap.containsKey(living)) {
                     float previousHealth = this.entityHealthMap.get(living);
                     float damage = previousHealth - currentHealth;
                     if (damage > 0.01F) {
                        Vec3d pos = living.getPos().add(0.0, living.getHeight() + 0.3, 0.0);
                        this.damageNumbers.add(new PVPAI.DamageNumber(pos, damage));
                     }
                  }

                  this.entityHealthMap.put(living, currentHealth);
               }
            }

            this.entityHealthMap.entrySet().removeIf(entry -> entry.getKey().isRemoved());
         }
      }
   };
   private final EventListener<HudRenderEvent> on2DRender = event -> {
      if (mc.player != null && mc.world != null) {
         float ourPower = this.calculatePlayerPower(mc.player);
         float totalEnemyPower = 0.0F;
         int visibleEnemies = 0;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !player.isInvisible() && !player.isDead() && !(mc.player.distanceTo(player) > 50.0F)) {
               float enemyPower = this.calculatePlayerPower(player);
               this.playerWinChances.put(player, this.calculateWinChance(ourPower, enemyPower));
               totalEnemyPower += enemyPower;
               visibleEnemies++;
            }
         }

         // не копим PlayerEntity всех, кто когда-либо попадал в радиус
         this.playerWinChances.keySet().removeIf(p -> p.isRemoved() || !mc.world.getPlayers().contains(p));

         if (visibleEnemies > 0) {
            this.ourWinChance = this.calculateWinChance(ourPower, totalEnemyPower / visibleEnemies);
         } else {
            this.ourWinChance = 50.0F;
         }

         int screenWidth = (int)sr.getScaledWidth();
         int screenHeight = (int)sr.getScaledHeight();
         String ourText = "Ваш процент: " + (int)this.ourWinChance + "%";
         TextRenderer font = mc.textRenderer;
         int textWidth = font.getWidth(ourText);
         int textX = (screenWidth - textWidth) / 2;
         int textY = screenHeight - 70;
         event.getContext().drawText(font, ourText, textX, textY, 16777215, true);
      }
   };
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.player != null && mc.world != null) {
         MatrixStack matrices = event.getMatrices();
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         if (this.damageIndicator.isEnabled() && !this.damageNumbers.isEmpty()) {
            Iterator<PVPAI.DamageNumber> iterator = this.damageNumbers.iterator();

            while (iterator.hasNext()) {
               PVPAI.DamageNumber dmg = iterator.next();
               dmg.update();
               if (dmg.shouldRemove()) {
                  iterator.remove();
               } else {
                  Vec3d renderPos = dmg.getPosition();
                  double x = renderPos.x - cameraPos.x;
                  double y = renderPos.y - cameraPos.y;
                  double z = renderPos.z - cameraPos.z;
                  matrices.push();
                  matrices.translate(x, y, z);
                  matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
                  matrices.scale(-0.025F, -0.025F, 0.025F);
                  String damageText = this.formatDamage(dmg.damage);
                  float textWidth = mc.textRenderer.getWidth(damageText);
                  float alpha = dmg.getAlpha();
                  float scale = dmg.getScale() * this.damageSize.getCurrentValue();
                  matrices.scale(scale, scale, 1.0F);
                  ColorRGBA damageColorValue = this.damageColor.getColor();
                  int color = damageColorValue.withAlpha(alpha * 255.0F).getRGB();
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
                  mc.textRenderer
                     .draw(
                        damageText,
                        -textWidth / 2.0F,
                        0.0F,
                        color,
                        true,
                        matrices.peek().getPositionMatrix(),
                        immediate,
                        TextLayerType.SEE_THROUGH,
                        0,
                        15728880
                     );
                  immediate.draw();
                  RenderSystem.disableBlend();
                  matrices.pop();
               }
            }
         }

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !player.isInvisible() && !player.isDead() && this.playerWinChances.containsKey(player)) {
               double distance = mc.player.distanceTo(player);
               if (!(distance > 32.0)) {
                  float enemyWinChance = 100.0F - this.ourWinChance;
                  Vec3d playerPos = player.getPos();
                  double x = playerPos.x - cameraPos.x;
                  double y = playerPos.y + player.getHeight() + 0.6 - cameraPos.y;
                  double z = playerPos.z - cameraPos.z;
                  matrices.push();
                  matrices.translate(x, y, z);
                  matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
                  matrices.scale(0.025F, -0.025F, 0.025F);
                  String text = (int)enemyWinChance + "%";
                  TextRenderer font = mc.textRenderer;
                  int textWidth = font.getWidth(text);
                  int bgWidth = textWidth + 4;
                  int bgHeight = 10;
                  Matrix4f matrix = matrices.peek().getPositionMatrix();
                  BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                  buffer.vertex(matrix, -bgWidth / 2.0F - 1.0F, -1.0F, 0.0F).color(0, 0, 0, 64);
                  buffer.vertex(matrix, -bgWidth / 2.0F - 1.0F, bgHeight - 1, 0.0F).color(0, 0, 0, 64);
                  buffer.vertex(matrix, bgWidth / 2.0F + 1.0F, bgHeight - 1, 0.0F).color(0, 0, 0, 64);
                  buffer.vertex(matrix, bgWidth / 2.0F + 1.0F, -1.0F, 0.0F).color(0, 0, 0, 64);
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                  BufferRenderer.drawWithGlobalProgram(buffer.end());
                  int color = enemyWinChance > 50.0F ? 16733525 : 5635925;
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
                  mc.textRenderer
                     .draw(
                        text, -textWidth / 2.0F, 0.0F, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.NORMAL, 0, 15728880
                     );
                  immediate.draw();
                  RenderSystem.disableBlend();
                  matrices.pop();
               }
            }
         }
      }
   };

   private float calculatePlayerPower(PlayerEntity player) {
      float power = 0.0F;
      float healthPercent = player.getHealth() / player.getMaxHealth();
      power += healthPercent * 20.0F;
      power += player.getHungerManager().getFoodLevel() / 2.0F;
      float totalArmorValue = 0.0F;
      float armorDurabilityFactor = 0.0F;
      int armorCount = 0;

      for (ItemStack armor : player.getArmorItems()) {
         if (!armor.isEmpty()) {
            totalArmorValue += this.getArmorValue(armor);
            totalArmorValue += this.getEnchantmentPower(armor);
            if (armor.isDamageable()) {
               int maxDamage = armor.getMaxDamage();
               int damage = armor.getDamage();
               float durabilityPercent = 1.0F - (float)damage / maxDamage;
               armorDurabilityFactor += durabilityPercent;
               armorCount++;
            }
         }
      }

      if (armorCount > 0) {
         float avgDurability = armorDurabilityFactor / armorCount;
         power += totalArmorValue * avgDurability;
      } else {
         power += totalArmorValue;
      }

      ItemStack mainHand = player.getMainHandStack();
      if (!mainHand.isEmpty()) {
         power += this.getWeaponDamage(mainHand);
         power += this.getEnchantmentPower(mainHand);
      }

      if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
         int amplifier = player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
         power += amplifier * 5;
      }

      if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
         int amplifier = player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
         power += amplifier * 5;
      }

      if (player.hasStatusEffect(StatusEffects.SPEED)) {
         int amplifier = player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
         power += amplifier * 3;
      }

      if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
         int amplifier = player.getStatusEffect(StatusEffects.REGENERATION).getAmplifier() + 1;
         power += amplifier * 4;
      }

      if (player.hasStatusEffect(StatusEffects.WEAKNESS)) {
         int amplifier = player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1;
         power -= amplifier * 3;
      }

      if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
         int amplifier = player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1;
         power -= amplifier * 2;
      }

      return Math.max(1.0F, power);
   }

   private float getArmorValue(ItemStack armor) {
      String itemName = armor.getItem().toString().toLowerCase();
      if (itemName.contains("diamond")) {
         return 3.0F;
      } else if (itemName.contains("netherite")) {
         return 4.0F;
      } else if (itemName.contains("iron")) {
         return 2.0F;
      } else if (itemName.contains("gold")) {
         return 1.5F;
      } else if (itemName.contains("chain")) {
         return 1.5F;
      } else {
         return itemName.contains("leather") ? 1.0F : 0.5F;
      }
   }

   private float getWeaponDamage(ItemStack weapon) {
      String itemName = weapon.getItem().toString().toLowerCase();
      if (itemName.contains("netherite_sword")) {
         return 10.0F;
      } else if (itemName.contains("diamond_sword")) {
         return 9.0F;
      } else if (itemName.contains("iron_sword")) {
         return 7.0F;
      } else if (itemName.contains("gold_sword")) {
         return 5.0F;
      } else if (itemName.contains("stone_sword")) {
         return 6.0F;
      } else if (itemName.contains("wood_sword")) {
         return 5.0F;
      } else if (itemName.contains("netherite_axe")) {
         return 12.0F;
      } else if (itemName.contains("diamond_axe")) {
         return 11.0F;
      } else if (itemName.contains("iron_axe")) {
         return 9.0F;
      } else {
         return itemName.contains("trident") ? 10.0F : 2.0F;
      }
   }

   private float getEnchantmentPower(ItemStack item) {
      float power = 0.0F;
      ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)item.get(DataComponentTypes.ENCHANTMENTS);
      if (enchantments == null) {
         return 0.0F;
      } else {
         for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            int level = enchantments.getLevel(entry);
            if (entry.matchesKey(Enchantments.PROTECTION)) {
               power += level * 2;
            } else if (entry.matchesKey(Enchantments.SHARPNESS)) {
               power += level * 2.5F;
            } else if (entry.matchesKey(Enchantments.FIRE_ASPECT)) {
               power += level * 1.5F;
            } else if (entry.matchesKey(Enchantments.KNOCKBACK)) {
               power += level * 1.0F;
            } else if (entry.matchesKey(Enchantments.THORNS)) {
               power += level * 1.5F;
            } else if (entry.matchesKey(Enchantments.BLAST_PROTECTION)) {
               power += level * 1.5F;
            } else if (entry.matchesKey(Enchantments.PROJECTILE_PROTECTION)) {
               power += level * 1.5F;
            } else if (entry.matchesKey(Enchantments.FIRE_PROTECTION)) {
               power += level * 1.5F;
            } else if (entry.matchesKey(Enchantments.FEATHER_FALLING)) {
               power += level * 0.5F;
            } else if (entry.matchesKey(Enchantments.UNBREAKING)) {
               power += level * 0.5F;
            } else {
               power += level * 0.5F;
            }
         }

         return power;
      }
   }

   private float calculateWinChance(float ourPower, float enemyPower) {
      float total = ourPower + enemyPower;
      if (total == 0.0F) {
         return 50.0F;
      } else {
         float rawChance = ourPower / total * 100.0F;
         return MathHelper.clamp(rawChance, 5.0F, 95.0F);
      }
   }

   private String formatDamage(float damage) {
      int wholePart = (int)damage;
      int decimalPart = Math.round((damage - wholePart) * 10.0F);
      if (decimalPart == 10) {
         wholePart++;
         decimalPart = 0;
      }

      return wholePart + "." + decimalPart;
   }

   private static class DamageNumber {
      private final Vec3d startPos;
      private final float damage;
      private final Animation animation;
      private final long startTime;
      private static final long DURATION = 2000L;

      public DamageNumber(Vec3d pos, float damage) {
         this.startPos = pos;
         this.damage = damage;
         this.animation = new Animation(2000L, Easing.CUBIC_OUT);
         this.startTime = System.currentTimeMillis();
      }

      public void update() {
         long elapsed = System.currentTimeMillis() - this.startTime;
         float progress = Math.min(1.0F, (float)elapsed / 2000.0F);
         this.animation.setValue(progress);
      }

      public Vec3d getPosition() {
         float progress = this.animation.getValue();
         double yOffset = progress * 1.5;
         return this.startPos.add(0.0, yOffset, 0.0);
      }

      public float getAlpha() {
         float progress = this.animation.getValue();
         return progress > 0.7F ? 1.0F - (progress - 0.7F) / 0.3F : 1.0F;
      }

      public float getScale() {
         float progress = this.animation.getValue();
         return progress < 0.2F ? 0.8F + progress / 0.2F * 0.4F : 1.2F;
      }

      public boolean shouldRemove() {
         return System.currentTimeMillis() - this.startTime >= 2000L;
      }
   }
}
