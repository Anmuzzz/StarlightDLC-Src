package com.isusdlc.utility.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Generated;
import net.minecraft.block.entity.BlockEntity;

public final class WorldUtility {
   public static List<BlockEntity> blockEntities = new CopyOnWriteArrayList<>();

   @Generated
   private WorldUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
