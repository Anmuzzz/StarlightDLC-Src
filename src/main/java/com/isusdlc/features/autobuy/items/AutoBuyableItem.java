package com.isusdlc.features.autobuy.items;

import com.isusdlc.features.autobuy.settings.AutoBuyItemSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public interface AutoBuyableItem {
   String getDisplayName();

   String getSearchName();

   ItemStack createItemStack();

   int getPrice();

   boolean isEnabled();

   void setEnabled(boolean enabled);

   AutoBuyItemSettings getSettings();

   default Item getItem() {
      return createItemStack().getItem();
   }

   default boolean needsAdditionalCheck() {
      ItemStack stack = createItemStack();
      NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData != null) {
         NbtCompound nbt = customData.copyNbt();
         if (nbt != null) {
            if (nbt.getBoolean("HolyWorldSphere") || nbt.getBoolean("HolyWorldSphereShard")
               || nbt.getBoolean("HolyWorldTalik") || nbt.getBoolean("HolyWorldExpBottle")
               || nbt.getBoolean("HolyWorldBackpack") || nbt.getBoolean("HolyWorldPyrotechnic")
               || nbt.getBoolean("HolyWorldKringe") || nbt.getBoolean("HolyWorldRune")
               || nbt.getBoolean("HolyWorldKringeEffect") || nbt.getBoolean("HolyWorldPotion")
               || nbt.getBoolean("HolyWorldStandardPotion") || nbt.getBoolean("HolyWorldMultiEffectPotion")
               || nbt.getBoolean("SpookyTimeSphere") || nbt.getBoolean("SpookyTimeTalik")
               || nbt.getBoolean("SpookyTimePotion") || nbt.getBoolean("SpookyTimeSpecial"))
               return true;
            if (nbt.contains("spookyItemType", 8)) {
               String type = nbt.getString("spookyItemType");
               if (type != null && !type.isEmpty()) return true;
            }
            if (nbt.contains("AttributeModifiers", 9)) {
               NbtList list = nbt.getList("AttributeModifiers", 10);
               if (!list.isEmpty()) return true;
            }
            if (nbt.contains("RequiredEnchantments", 9)) {
               NbtList list = nbt.getList("RequiredEnchantments", 10);
               if (!list.isEmpty()) return true;
            }
         }
      }
      ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
      if (ench != null && !ench.isEmpty()) return true;
      LoreComponent lore = stack.get(DataComponentTypes.LORE);
      if (lore != null && !lore.lines().isEmpty()) return true;
      PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
      if (potion != null && potion.getEffects() != null && potion.getEffects().iterator().hasNext()) return true;
      return false;
   }
}
