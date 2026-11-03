package com.isusdlc.mixin.minecraft.network;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.event.impl.network.SendPacketEvent;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class ClientConnectionMixin implements IMinecraft {
   @Unique
   private static volatile boolean stackOverflowFix;

   @Inject(
      method = {"handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      if (elegant.getInstance().getEventManager() == null) {
         return;
      }
      ReceivePacketEvent event = new ReceivePacketEvent(packet);
      elegant.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void triggerSendPacketEvent(Packet<?> packet, CallbackInfo ci) {
      SendPacketEvent event = new SendPacketEvent(packet);
      if (elegant.getInstance().getEventManager() == null) {
         return;
      }
      if (!stackOverflowFix) {
         elegant.getInstance().getEventManager().triggerEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }

         Packet<?> newPacket = event.getPacket();
         if (newPacket != packet) {
            ci.cancel();
            stackOverflowFix = true;
            try {
               if (mc.getNetworkHandler() != null) {
                  mc.getNetworkHandler().sendPacket(newPacket);
               }
            } finally {
               stackOverflowFix = false;
            }
         }
      }
   }
}
