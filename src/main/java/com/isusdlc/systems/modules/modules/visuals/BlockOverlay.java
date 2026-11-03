package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.render.Draw3DUtility;
import com.isusdlc.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.client.render.VertexFormat.DrawMode;

@ModuleInfo(
   name = "Block Overlay",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.blockoverlay"
)
public class BlockOverlay extends BaseModule {
   private final BooleanSetting syncWithTheme = new BooleanSetting(this, "modules.settings.blockoverlay.sync_with_theme").enabled(true);
   private final ColorSetting color = new ColorSetting(this, "modules.settings.blockoverlay.color", () -> this.syncWithTheme.isEnabled())
      .color(new ColorRGBA(151.0F, 71.0F, 255.0F, 255.0F))
      .alpha(true);
   private final Animation alphaAnim = new Animation(200L, 0.0F, Easing.CUBIC_OUT);

   private final EventListener<Render3DEvent> onRender3D = event -> {
      boolean targetValid = mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(Type.BLOCK) && mc.world != null;
      alphaAnim.update(targetValid ? 1.0F : 0.0F);
      float alpha = alphaAnim.getValue();
      if (alpha < 0.01F) return;

      if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != Type.BLOCK || mc.world == null) return;
      BlockPos pos = hit.getBlockPos();
      MatrixStack matrices = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
      Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);
      ColorRGBA overlayColor = this.syncWithTheme.isEnabled() ? Colors.getAccentColor() : this.color.getColor();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Draw3DUtility.renderFilledBox(
         matrices,
         quadsBuffer,
         box.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
         overlayColor.withAlpha(50.0F * alpha)
      );
      RenderUtility.buildBuffer(quadsBuffer);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      Draw3DUtility.renderOutlinedBox(
         matrices,
         linesBuffer,
         box.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
         overlayColor.withAlpha(150.0F * alpha)
      );
      RenderUtility.buildBuffer(linesBuffer);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   };
}
