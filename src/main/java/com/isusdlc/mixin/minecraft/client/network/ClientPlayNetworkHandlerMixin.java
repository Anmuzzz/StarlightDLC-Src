package com.isusdlc.mixin.minecraft.client.network;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.impl.game.PickupEvent;
import com.isusdlc.systems.event.impl.game.TotemLossEvent;
import com.isusdlc.systems.event.impl.game.WorldChangeEvent;
import com.isusdlc.utility.game.WorldUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.rotations.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.ClientConnection;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler implements IMinecraft {
   private final com.isusdlc.utility.time.Timer sweepTimer = new com.isusdlc.utility.time.Timer();
   @Unique
   private Rotation oldRotation = Rotation.ZERO;

   protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = {"onItemPickupAnimation(Lnet/minecraft/network/packet/s2c/play/ItemPickupAnimationS2CPacket;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;",
         ordinal = 0
      )}
   )
   private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
      Entity itemEntity = this.client.world.getEntityById(packet.getEntityId());
      Entity entity = this.client.world.getEntityById(packet.getCollectorEntityId());
      if (itemEntity instanceof ItemEntity && entity == this.client.player) {
         elegant.getInstance().getEventManager().triggerEvent(new PickupEvent(((ItemEntity)itemEntity).getStack(), packet.getStackAmount()));
      }
   }

   @Inject(
      method = {"onBlockEntityUpdate(Lnet/minecraft/network/packet/s2c/play/BlockEntityUpdateS2CPacket;)V"},
      at = {@At("TAIL")}
   )
   private void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
      if (mc.world != null) {
         // выгруженные чанки помечают свои BlockEntity как removed — выметаем их раз в 5с,
         // иначе список растёт всю сессию (утечка содержимого сундуков)
         if (sweepTimer.finished(5000L)) {
            sweepTimer.reset();
            WorldUtility.blockEntities.removeIf(BlockEntity::isRemoved);
         }

         BlockPos pos = packet.getPos();
         BlockEntity blockEntity = mc.world.getBlockEntity(pos);
         if (blockEntity != null && !blockEntity.isRemoved() && !WorldUtility.blockEntities.contains(blockEntity)) {
            WorldUtility.blockEntities.add(blockEntity);
         }
      }
   }

   @Inject(
      method = {"onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V"},
      at = {@At("TAIL")}
   )
   private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
      if (mc.world != null) {
         WorldChunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());
         chunk.getBlockEntities().values().forEach(be -> {
            if (!WorldUtility.blockEntities.contains(be)) {
               WorldUtility.blockEntities.add(be);
            }
         });
      }
   }

   @Inject(
      method = {"onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V"},
      at = {@At("TAIL")}
   )
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      WorldUtility.blockEntities.clear();
      elegant.getInstance().getEventManager().triggerEvent(new WorldChangeEvent());
   }

   @Inject(
      method = {"onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V"},
      at = {@At("HEAD")}
   )
   public void savePlayerRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      if (mc.player != null) {
         this.oldRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   @Inject(
      method = {"onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V"},
      at = {@At("RETURN")}
   )
   public void modifyPlayerRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      if (mc.player != null) {
         new Rotation(packet.change().yaw(), packet.change().pitch());
      }
   }

   @Inject(
      method = {"onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V"},
      at = {@At("HEAD")}
   )
   public void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
      if (mc.world != null) {
         if (packet.getStatus() == 35 && packet.getEntity(mc.world) instanceof PlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            ItemStack totem = null;
            if (mainHand.getItem() == Items.TOTEM_OF_UNDYING) {
               totem = mainHand;
            } else if (offHand.getItem() == Items.TOTEM_OF_UNDYING) {
               totem = offHand;
            }

            if (totem != null) {
               boolean wasEnchanted = totem.hasEnchantments();
               elegant.getInstance().getEventManager().triggerEvent(new TotemLossEvent(player, totem, wasEnchanted));
            }
         }
      }
   }
}
