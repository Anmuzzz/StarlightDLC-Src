package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

@ModuleInfo(
   name = "ItemRadius",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.itemradius"
)
public class ItemRadius extends BaseModule {
   private final BooleanSetting enderEye = new BooleanSetting(
         this, "modules.settings.itemradius.ender_eye", "modules.settings.itemradius.ender_eye.description"
      )
      .enable();
   private final ColorSetting enderEyeColor = new ColorSetting(this, "modules.settings.itemradius.ender_eye_color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
      .alpha(true);
   private final BooleanSetting sugar = new BooleanSetting(this, "modules.settings.itemradius.sugar", "modules.settings.itemradius.sugar.description").enable();
   private final ColorSetting sugarColor = new ColorSetting(this, "modules.settings.itemradius.sugar_color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
      .alpha(true);
   private final BooleanSetting netheriteScrap = new BooleanSetting(
         this, "modules.settings.itemradius.netherite_scrap", "modules.settings.itemradius.netherite_scrap.description"
      )
      .enable();
   private final ColorSetting netheriteScrapColor = new ColorSetting(this, "modules.settings.itemradius.netherite_scrap_color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
      .alpha(true);
   private final BooleanSetting driedKelp = new BooleanSetting(
         this, "modules.settings.itemradius.dried_kelp", "modules.settings.itemradius.dried_kelp.description"
      )
      .enable();
   private final ColorSetting driedKelpColor = new ColorSetting(this, "modules.settings.itemradius.dried_kelp_color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
      .alpha(true);
   private final BooleanSetting snowball = new BooleanSetting(this, "modules.settings.itemradius.snowball", "modules.settings.itemradius.snowball.description")
      .enable();
   private final ColorSetting snowballColor = new ColorSetting(this, "modules.settings.itemradius.snowball_color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
      .alpha(true);
   private final BooleanSetting proximityColor = new BooleanSetting(
         this, "modules.settings.itemradius.proximity_color", "modules.settings.itemradius.proximity_color.description"
      )
      .enable();
   private final ColorSetting proximityColorSetting = new ColorSetting(this, "modules.settings.itemradius.proximity_color_setting")
      .color(new ColorRGBA(0.0F, 255.0F, 0.0F, 255.0F))
      .alpha(true);
   private static final float ENDER_EYE_RADIUS = 14.0F;
   private static final float SUGAR_RADIUS = 14.0F;
   private static final float SNOWBALL_RADIUS = 12.0F;
   private static final float NETHERITE_SCRAP_RADIUS = 2.0F;
   private static final float TRAP_WIDTH = 5.0F;
   private static final float TRAP_HEIGHT = 4.0F;
   private static final float PLASTIC_WIDTH = 2.0F;
   private static final float PLASTIC_LENGTH = 5.0F;
   private static final float PLASTIC_HEIGHT = 4.0F;
   private static final float PLASTIC_FLOOR_SIZE = 5.0F;
   private static final float PLASTIC_FLOOR_HEIGHT = 2.0F;
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.player != null) {
         boolean hasEnderEye = mc.player.getMainHandStack().getItem() == Items.ENDER_EYE
            || mc.player.getOffHandStack().getItem() == Items.ENDER_EYE;
         boolean hasSugar = mc.player.getMainHandStack().getItem() == Items.SUGAR
            || mc.player.getOffHandStack().getItem() == Items.SUGAR;
         boolean hasNetheriteScrap = mc.player.getMainHandStack().getItem() == Items.NETHERITE_SCRAP
            || mc.player.getOffHandStack().getItem() == Items.NETHERITE_SCRAP;
         boolean hasDriedKelp = mc.player.getMainHandStack().getItem() == Items.DRIED_KELP
            || mc.player.getOffHandStack().getItem() == Items.DRIED_KELP;
         boolean hasSnowball = mc.player.getMainHandStack().getItem() == Items.SNOWBALL
            || mc.player.getOffHandStack().getItem() == Items.SNOWBALL;
         if (hasEnderEye || hasSugar || hasNetheriteScrap || hasDriedKelp || hasSnowball) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(8.0F);
            MatrixStack matrices = event.getMatrices();
            matrices.push();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Vec3d playerPos = mc.player.getPos();
            Vec3d centerPos = playerPos.add(0.0, -0.1, 0.0);
            if (this.enderEye.isEnabled() && hasEnderEye) {
               ColorRGBA color = this.enderEyeColor.getColor();
               int colorHex = color.getRGB();
               if (this.proximityColor.isEnabled() && this.isPlayerInRadius(centerPos, 14.0F)) {
                  colorHex = this.proximityColorSetting.getColor().getRGB();
               }

               this.drawCircle(matrices, centerPos, 14.0F, colorHex);
            }

            if (this.sugar.isEnabled() && hasSugar) {
               ColorRGBA color = this.sugarColor.getColor();
               int colorHex = color.getRGB();
               if (this.proximityColor.isEnabled() && this.isPlayerInRadius(centerPos, 14.0F)) {
                  colorHex = this.proximityColorSetting.getColor().getRGB();
               }

               this.drawCircle(matrices, centerPos, 14.0F, colorHex);
            }

            if (this.netheriteScrap.isEnabled() && hasNetheriteScrap) {
               ColorRGBA color = this.netheriteScrapColor.getColor();
               int colorHex = color.getRGB();
               Vec3d blockPos = this.getBlockAlignedPlayerPos();
               Vec3d trapPos = blockPos.add(0.0, 1.5, 0.0);
               if (this.proximityColor.isEnabled() && this.isPlayerInRadius(trapPos, 2.5F)) {
                  colorHex = this.proximityColorSetting.getColor().getRGB();
               }

               this.drawTrapCubeWithFill(matrices, trapPos, colorHex);
            }

            if (this.driedKelp.isEnabled() && hasDriedKelp) {
               ColorRGBA color = this.driedKelpColor.getColor();
               int colorHex = color.getRGB();
               Vec3d blockPos = this.getBlockAlignedPlayerPos();
               Vec3d plasticPos = blockPos.add(0.0, 1.5, 0.0);
               if (this.proximityColor.isEnabled() && this.isPlayerInRadius(plasticPos, Math.max(2.0F, 5.0F) / 2.0F)) {
                  colorHex = this.proximityColorSetting.getColor().getRGB();
               }

               this.drawPlastic(matrices, plasticPos, colorHex);
            }

            if (this.snowball.isEnabled() && hasSnowball) {
               ColorRGBA color = this.snowballColor.getColor();
               int colorHex = color.getRGB();
               Vec3d landingPos = this.getSnowballLandingPosition();
               if (this.proximityColor.isEnabled() && this.isPlayerInRadius(landingPos, 12.0F)) {
                  colorHex = this.proximityColorSetting.getColor().getRGB();
               }

               this.drawCircle(matrices, landingPos, 12.0F, colorHex);
            }

            matrices.pop();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
         }
      }
   };

   private boolean isPlayerInRadius(Vec3d center, float radius) {
      return mc.player != null && mc.world != null
         ? mc.world
            .getPlayers()
            .stream()
            .anyMatch(
               player -> {
                  if (player == mc.player) {
                     return false;
                  } else {
                     Vec3d playerPos = player.getPos();
                     double distance = Math.sqrt(
                        Math.pow(playerPos.x - center.x, 2.0)
                           + Math.pow(playerPos.y - center.y, 2.0)
                           + Math.pow(playerPos.z - center.z, 2.0)
                     );
                     return distance <= radius;
                  }
               }
            )
         : false;
   }

   private void drawCircle(MatrixStack matrices, Vec3d center, float radius, int color) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      double y = center.y;
      int segments = 64;

      for (int i = 0; i < segments; i++) {
         double angle1 = (Math.PI * 2) * i / segments;
         double angle2 = (Math.PI * 2) * (i + 1) / segments;
         double x1 = center.x + radius * Math.cos(angle1);
         double z1 = center.z + radius * Math.sin(angle1);
         double x2 = center.x + radius * Math.cos(angle2);
         double z2 = center.z + radius * Math.sin(angle2);
         buffer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y, (float)z1).color(color);
         buffer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y, (float)z2).color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private Vec3d getSnowballLandingPosition() {
      if (mc.player == null) {
         return Vec3d.ZERO;
      } else {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d lookVec = mc.player.getRotationVec(1.0F);
         float pitch = mc.player.getPitch(1.0F);
         double pitchRadians = Math.toRadians(pitch);
         double baseDistance = 20.0;
         double verticalMultiplier = 1.0 + Math.abs(Math.sin(pitchRadians)) * 1.0;
         double distance = baseDistance * verticalMultiplier;
         distance = Math.max(10.0, Math.min(50.0, distance));
         Vec3d landingPos = eyePos.add(lookVec.multiply(distance));
         double groundY = mc.player.getY();
         if (mc.world != null) {
            groundY = mc.world.getTopY(Type.MOTION_BLOCKING, (int)landingPos.x, (int)landingPos.z);
         }

         return new Vec3d(landingPos.x, groundY, landingPos.z);
      }
   }

   private Vec3d getBlockAlignedPlayerPos() {
      if (mc.player == null) {
         return Vec3d.ZERO;
      } else {
         double blockX = Math.floor(mc.player.getX()) + 0.5;
         double blockY = Math.floor(mc.player.getY()) + 0.5;
         double blockZ = Math.floor(mc.player.getZ()) + 0.5;
         return new Vec3d(blockX, blockY, blockZ);
      }
   }

   private Vec3d getInterpolatedPlayerPos(float partialTicks) {
      if (mc.player == null) {
         return Vec3d.ZERO;
      } else {
         double currentX = mc.player.getX();
         double currentY = mc.player.getY();
         double currentZ = mc.player.getZ();
         double prevX = mc.player.prevX;
         double prevY = mc.player.prevY;
         double prevZ = mc.player.prevZ;
         double interpolatedX = prevX + (currentX - prevX) * partialTicks;
         double interpolatedY = prevY + (currentY - prevY) * partialTicks;
         double interpolatedZ = prevZ + (currentZ - prevZ) * partialTicks;
         return new Vec3d(interpolatedX, interpolatedY, interpolatedZ);
      }
   }

   private void drawTrapCubeWithFill(MatrixStack matrices, Vec3d center, int color) {
      Tessellator tessellator = Tessellator.getInstance();
      float halfWidth = 2.5F;
      float halfHeight = 2.0F;
      float[][] vertices = new float[][]{
         {-halfWidth, -halfHeight, -halfWidth},
         {halfWidth, -halfHeight, -halfWidth},
         {halfWidth, halfHeight, -halfWidth},
         {-halfWidth, halfHeight, -halfWidth},
         {-halfWidth, -halfHeight, halfWidth},
         {halfWidth, -halfHeight, halfWidth},
         {halfWidth, halfHeight, halfWidth},
         {-halfWidth, halfHeight, halfWidth}
      };
      int[][] faces = new int[][]{{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2}};
      int fillColor = color & 16777215 | 1426063360;
      RenderSystem.disableCull();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      for (int[] face : faces) {
         for (int i = 0; i < face.length - 2; i++) {
            int v0 = face[0];
            int v1 = face[i + 1];
            int v2 = face[i + 2];
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v0][0]),
                  (float)(center.y + vertices[v0][1]),
                  (float)(center.z + vertices[v0][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v1][0]),
                  (float)(center.y + vertices[v1][1]),
                  (float)(center.z + vertices[v1][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v2][0]),
                  (float)(center.y + vertices[v2][1]),
                  (float)(center.z + vertices[v2][2])
               )
               .color(fillColor);
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableCull();
      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int[] edge : edges) {
         int v1 = edge[0];
         int v2 = edge[1];
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v1][0]),
               (float)(center.y + vertices[v1][1]),
               (float)(center.z + vertices[v1][2])
            )
            .color(color);
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v2][0]),
               (float)(center.y + vertices[v2][1]),
               (float)(center.z + vertices[v2][2])
            )
            .color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private void drawPlastic(MatrixStack matrices, Vec3d center, int color) {
      float pitch = mc.player.getPitch(1.0F);
      boolean isLookingUp = pitch < -45.0F;
      if (isLookingUp) {
         Vec3d headPos = mc.player.getEyePos();
         Vec3d floorPos = headPos.add(0.0, 0.0, 3.0);
         this.drawPlasticFloor(matrices, floorPos, color);
      } else {
         Vec3d panelPos = this.getPanelPositionFromCenter(center, 4.0);
         this.drawPlasticPanel(matrices, panelPos, color);
      }
   }

   private Vec3d getPanelPositionFromCenter(Vec3d center, double distance) {
      float yaw = mc.player.getYaw(1.0F);
      double lookX = -Math.sin(Math.toRadians(yaw));
      double lookZ = Math.cos(Math.toRadians(yaw));
      double panelX = Math.floor(center.x + lookX * distance) + 0.5;
      double panelZ = Math.floor(center.z + lookZ * distance) + 0.5;
      double panelY = Math.floor(center.y) + 0.5;
      return new Vec3d(panelX, panelY, panelZ);
   }

   private void drawPlasticPanel(MatrixStack matrices, Vec3d center, int color) {
      Tessellator tessellator = Tessellator.getInstance();
      float halfWidth = 1.0F;
      float halfLength = 2.5F;
      float halfHeight = 2.0F;
      RenderSystem.enableDepthTest();
      float[][] vertices = new float[][]{
         {-halfWidth, -halfHeight, -halfLength},
         {halfWidth, -halfHeight, -halfLength},
         {halfWidth, halfHeight, -halfLength},
         {-halfWidth, halfHeight, -halfLength},
         {-halfWidth, -halfHeight, halfLength},
         {halfWidth, -halfHeight, halfLength},
         {halfWidth, halfHeight, halfLength},
         {-halfWidth, halfHeight, halfLength}
      };
      float yaw = mc.player.getYaw(1.0F);
      matrices.push();
      matrices.translate(center.x, center.y, center.z);
      matrices.multiply(new Quaternionf(new AxisAngle4f((float)Math.toRadians(-yaw + 90.0F), 0.0F, 1.0F, 0.0F)));
      matrices.translate(-center.x, -center.y, -center.z);
      int[][] faces = new int[][]{{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2}};
      int fillColor = color & 16777215 | 1426063360;
      RenderSystem.disableCull();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      for (int[] face : faces) {
         for (int i = 0; i < face.length - 2; i++) {
            int v0 = face[0];
            int v1 = face[i + 1];
            int v2 = face[i + 2];
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v0][0]),
                  (float)(center.y + vertices[v0][1]),
                  (float)(center.z + vertices[v0][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v1][0]),
                  (float)(center.y + vertices[v1][1]),
                  (float)(center.z + vertices[v1][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v2][0]),
                  (float)(center.y + vertices[v2][1]),
                  (float)(center.z + vertices[v2][2])
               )
               .color(fillColor);
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableCull();
      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int[] edge : edges) {
         int v1 = edge[0];
         int v2 = edge[1];
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v1][0]),
               (float)(center.y + vertices[v1][1]),
               (float)(center.z + vertices[v1][2])
            )
            .color(color);
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v2][0]),
               (float)(center.y + vertices[v2][1]),
               (float)(center.z + vertices[v2][2])
            )
            .color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      matrices.pop();
   }

   private void drawPlasticFloor(MatrixStack matrices, Vec3d center, int color) {
      Tessellator tessellator = Tessellator.getInstance();
      RenderSystem.disableDepthTest();
      float halfSize = 2.5F;
      float halfHeight = 1.0F;
      float[][] vertices = new float[][]{
         {-halfSize, -halfHeight, -halfSize},
         {halfSize, -halfHeight, -halfSize},
         {halfSize, halfHeight, -halfSize},
         {-halfSize, halfHeight, -halfSize},
         {-halfSize, -halfHeight, halfSize},
         {halfSize, -halfHeight, halfSize},
         {halfSize, halfHeight, halfSize},
         {-halfSize, halfHeight, halfSize}
      };
      int[][] faces = new int[][]{{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2}};
      int fillColor = color & 16777215 | 1426063360;
      RenderSystem.disableCull();
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      for (int[] face : faces) {
         for (int i = 0; i < face.length - 2; i++) {
            int v0 = face[0];
            int v1 = face[i + 1];
            int v2 = face[i + 2];
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v0][0]),
                  (float)(center.y + vertices[v0][1]),
                  (float)(center.z + vertices[v0][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v1][0]),
                  (float)(center.y + vertices[v1][1]),
                  (float)(center.z + vertices[v1][2])
               )
               .color(fillColor);
            buffer.vertex(
                  matrices.peek().getPositionMatrix(),
                  (float)(center.x + vertices[v2][0]),
                  (float)(center.y + vertices[v2][1]),
                  (float)(center.z + vertices[v2][2])
               )
               .color(fillColor);
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableCull();
      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int[] edge : edges) {
         int v1 = edge[0];
         int v2 = edge[1];
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v1][0]),
               (float)(center.y + vertices[v1][1]),
               (float)(center.z + vertices[v1][2])
            )
            .color(color);
         buffer.vertex(
               matrices.peek().getPositionMatrix(),
               (float)(center.x + vertices[v2][0]),
               (float)(center.y + vertices[v2][1]),
               (float)(center.z + vertices[v2][2])
            )
            .color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public BooleanSetting getEnderEye() {
      return this.enderEye;
   }

   public ColorSetting getEnderEyeColor() {
      return this.enderEyeColor;
   }

   public BooleanSetting getSugar() {
      return this.sugar;
   }

   public ColorSetting getSugarColor() {
      return this.sugarColor;
   }

   public BooleanSetting getNetheriteScrap() {
      return this.netheriteScrap;
   }

   public ColorSetting getNetheriteScrapColor() {
      return this.netheriteScrapColor;
   }

   public BooleanSetting getDriedKelp() {
      return this.driedKelp;
   }

   public ColorSetting getDriedKelpColor() {
      return this.driedKelpColor;
   }

   public BooleanSetting getSnowball() {
      return this.snowball;
   }

   public ColorSetting getSnowballColor() {
      return this.snowballColor;
   }

   public BooleanSetting getProximityColor() {
      return this.proximityColor;
   }

   public ColorSetting getProximityColorSetting() {
      return this.proximityColorSetting;
   }

   public EventListener<Render3DEvent> getOnRender3D() {
      return this.onRender3D;
   }
}
