package com.isusdlc.utility.render.batching;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;

public abstract class Batching {
   protected static Batching active;

   /**
    * Скоуп авто-батчинга HUD: внутри него draw-методы CustomDrawContext
    * собирают однотипные вызовы в батчи, а нетипичные — сбрасывают накопленное
    * (строгий порядок слоёв сохраняется). Вне скоупа поведение прежнее.
    */
   public static boolean AUTO = false;

   public static void flushActive() {
      if (AUTO && active != null) {
         active.draw();
      }
   }
   protected BufferBuilder builder;

   public Batching(VertexFormat vertexFormat) {
      this.builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, vertexFormat);
      active = this;
   }

   protected void build() {
      BuiltBuffer builtBuffer = this.builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   public abstract void draw();

   @Generated
   public BufferBuilder getBuilder() {
      return this.builder;
   }

   @Generated
   public static Batching getActive() {
      return active;
   }
}
