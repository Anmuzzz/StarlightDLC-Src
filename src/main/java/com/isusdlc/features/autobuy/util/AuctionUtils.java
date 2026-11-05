package com.isusdlc.features.autobuy.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;

public class AuctionUtils {
   public static final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:[\\s,]\\d{3})*(?:\\.\\d{2})?)");
   public static final Pattern holyWorldPricePattern = Pattern.compile("(\\d+(?:[\\s,]\\d{3})*)¤");
   private static final String LORE_IMPERETRABLE_II = "Непробиваемый II";
   private static final String LORE_IMPERETRABLE_I = "Непробиваемый I";

   public static int getPrice(ItemStack var0) {
      ComponentMap var1 = var0.getComponents();
      String var2 = var0.getName().getString();
      if (var1 == null) {
         return -1;
      }

      String var3 = null;
      String var4 = var1.toString();
      var3 = StringUtils.substringBetween(var4, "literal{ $", "}[style={color=green}]");
      if (var3 == null || var3.isEmpty()) {
         LoreComponent var5 = (LoreComponent)var0.get(DataComponentTypes.LORE);
         if (var5 != null && !var5.lines().isEmpty()) {
            for (Text var7 : var5.lines()) {
               String var8 = var7.getString();
               Matcher var9 = funTimePricePattern.matcher(var8);
               String var10 = null;

               while (var9.find()) {
                  var10 = var9.group(1);
               }

               if (var10 == null) {
                  Matcher var11 = holyWorldPricePattern.matcher(var8);

                  while (var11.find()) {
                     var10 = var11.group(1);
                  }
               }

               if (var10 != null) {
                  var3 = var10;
                  break;
               }
            }
         }
      }

      if (var3 != null && !var3.isEmpty()) {
         try {
            var3 = var3.replaceAll("[\\s,]", "");
            return Integer.parseInt(var3);
         } catch (NumberFormatException var12) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static int getPriceFromNearbySlots(List<Slot> var0, int var1) {
      int[] var2 = new int[]{9, -9};

      for (int var6 : var2) {
         int var7 = var1 + var6;
         if (var7 >= 0 && var7 < var0.size()) {
            Slot var8 = (Slot)var0.get(var7);
            if (var8 != null) {
               ItemStack var9 = var8.getStack();
               if (!var9.isEmpty()) {
                  int price = getPrice(var9);
                  if (price > 0) {
                     return price;
                  }
               }
            }
         }
      }

      return -1;
   }

   private static String cleanString(String var0) {
      return var0 == null ? "" : var0.toLowerCase().trim().replaceAll("§.", "").replaceAll("[^a-zа-яё0-9\\s\\[\\]★]", "").replaceAll("\\s+", " ");
   }

   public static boolean isArmorItem(ItemStack var0) {
      return var0.getItem() == Items.NETHERITE_HELMET
         || var0.getItem() == Items.NETHERITE_CHESTPLATE
         || var0.getItem() == Items.NETHERITE_LEGGINGS
         || var0.getItem() == Items.NETHERITE_BOOTS
         || var0.getItem() == Items.DIAMOND_HELMET
         || var0.getItem() == Items.DIAMOND_CHESTPLATE
         || var0.getItem() == Items.DIAMOND_LEGGINGS
         || var0.getItem() == Items.DIAMOND_BOOTS
         || var0.getItem() == Items.IRON_HELMET
         || var0.getItem() == Items.IRON_CHESTPLATE
         || var0.getItem() == Items.IRON_LEGGINGS
         || var0.getItem() == Items.IRON_BOOTS
         || var0.getItem() == Items.GOLDEN_HELMET
         || var0.getItem() == Items.GOLDEN_CHESTPLATE
         || var0.getItem() == Items.GOLDEN_LEGGINGS
         || var0.getItem() == Items.GOLDEN_BOOTS
         || var0.getItem() == Items.CHAINMAIL_HELMET
         || var0.getItem() == Items.CHAINMAIL_CHESTPLATE
         || var0.getItem() == Items.CHAINMAIL_LEGGINGS
         || var0.getItem() == Items.CHAINMAIL_BOOTS
         || var0.getItem() == Items.LEATHER_HELMET
         || var0.getItem() == Items.LEATHER_CHESTPLATE
         || var0.getItem() == Items.LEATHER_LEGGINGS
         || var0.getItem() == Items.LEATHER_BOOTS
         || var0.getItem() == Items.TURTLE_HELMET;
   }

   public static boolean hasThornsEnchantment(ItemStack var0) {
      ItemEnchantmentsComponent var1 = (ItemEnchantmentsComponent)var0.get(DataComponentTypes.ENCHANTMENTS);
      if (var1 != null && !var1.isEmpty()) {
         for (RegistryEntry var3 : var1.getEnchantments()) {
            String var4 = var3.getIdAsString();
            if (var4 != null) {
               String var5 = var4.toLowerCase();
               if (var5.contains("thorns") || var5.contains("шип")) {
                  return true;
               }
            }
         }

         LoreComponent var6 = (LoreComponent)var0.get(DataComponentTypes.LORE);
         if (var6 != null) {
            for (Text var8 : var6.lines()) {
               String var9 = var8.getString().toLowerCase();
               if (var9.contains("thorns") || var9.contains("шип")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean compareItem(ItemStack var0, ItemStack var1) {
      String var2 = var0.getName().getString();
      String var3 = var1.getName().getString();
      String var4 = var0.getItem().toString();
      String var5 = var1.getItem().toString();
      NbtComponent var6 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      boolean var7 = false;
      boolean var8 = false;
      if (var6 != null) {
         NbtCompound var9 = var6.copyNbt();
         if (var9 != null) {
            var7 = var9.getBoolean("HolyWorldItem");
            var8 = var9.getBoolean("HolyWorldSphere");
         }
      }

      if (var8) {
         return var0.getItem() != Items.END_CRYSTAL && var0.getItem() != Items.PLAYER_HEAD ? false : compareHolyWorldSphere(var0, var1);
      }

      boolean var30 = false;
      boolean var10 = false;
      boolean var11 = false;
      boolean var12 = false;
      if (var6 != null) {
         NbtCompound var13 = var6.copyNbt();
         if (var13 != null && var13.getBoolean("SpookyTimeItem")) {
            var30 = true;
            var10 = var13.getBoolean("SpookyTimeSphere");
            var11 = var13.getBoolean("SpookyTimeTalik");
            var12 = var13.getBoolean("SpookyTimePotion");
         }
      }

      if (var12) {
         boolean var36 = (var0.getItem() == Items.SPLASH_POTION || var0.getItem() == Items.POTION)
            && (var1.getItem() == Items.SPLASH_POTION || var1.getItem() == Items.POTION);
         return !var36 ? false : compareSpookyTimePotion(var0, var1);
      }

      if (var10) {
         return var0.getItem() == Items.PLAYER_HEAD && var1.getItem() == Items.PLAYER_HEAD ? compareTalismanByAttributes(var0, var1) : false;
      }

      if (var0.getItem() != var1.getItem()) {
         return false;
      }

      if (var7) {
         NbtCompound var31 = var6.copyNbt();
         if (var31 != null) {
            if (var31.getBoolean("HolyWorldExpBottle")) {
               return compareHolyWorldExpBottle(var0, var1);
            }

            if (var31.getBoolean("HolyWorldBackpack")) {
               return compareHolyWorldBackpack(var0, var1);
            }

            if (var31.getBoolean("HolyWorldPyrotechnic")) {
               return compareHolyWorldPyrotechnic(var0, var1);
            }

            if (var31.getBoolean("HolyWorldKringe")) {
               return compareHolyWorldKringe(var0, var1);
            }

            if (var31.getBoolean("HolyWorldRune")) {
               return compareHolyWorldRune(var0, var1);
            }

            if (var31.getBoolean("HolyWorldSphereShard")) {
               return compareHolyWorldSphereShard(var0, var1);
            }

            if (var31.getBoolean("HolyWorldKringeEffect")) {
               return compareHolyWorldKringeEffect(var0, var1);
            }

            if (var31.getBoolean("HolyWorldPotion")) {
               return compareHolyWorldPotion(var0, var1);
            }

            if (var31.getBoolean("HolyWorldStandardPotion")) {
               return compareHolyWorldStandardPotion(var0, var1);
            }

            if (var31.getBoolean("HolyWorldMultiEffectPotion")) {
               return compareHolyWorldMultiEffectPotion(var0, var1);
            }

            if (var31.getBoolean("HolyWorldTalik")) {
               return compareHolyWorldTalik(var0, var1);
            }

            if (var31.contains("AttributeModifiers", 9)) {
               NbtList var41 = var31.getList("AttributeModifiers", 10);
               if (!var41.isEmpty()) {
                  return compareTalismanByAttributes(var0, var1);
               }
            }

            if (!holyWorldArmorVariantMatchesByNbt(var0, var1)) {
               return false;
            }

            return compareByEnchantments(var0, var1);
         }
      }

      if (var30 && !var12 && !var10) {
         if (var0.getItem() != var1.getItem()) {
            return false;
         }

         NbtCompound var35 = var6.copyNbt();
         if (var35 != null) {
            if (var35.getBoolean("SpookyTimeSpecial")) {
               return compareSpookyTimeSpecial(var0, var1);
            }

            if (var11) {
               if (var0.getItem() == Items.TOTEM_OF_UNDYING && var1.getItem() == Items.TOTEM_OF_UNDYING) {
                  NbtList var40 = var35.getList("AttributeModifiers", 10);
                  if (var40 != null && !var40.isEmpty()) {
                     return compareTalismanByAttributes(var0, var1);
                  }

                  NbtComponent var45 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
                  if (var45 != null) {
                     NbtCompound var48 = var45.copyNbt();
                     if (var48 != null && var48.contains("AttributeModifiers", 9)) {
                        NbtList var51 = var48.getList("AttributeModifiers", 10);
                        if (!var51.isEmpty()) {
                           return false;
                        }
                     }
                  }

                  return true;
               }

               return false;
            }
         }

         return compareByEnchantments(var0, var1);
      } else if (var0.getItem() == Items.TOTEM_OF_UNDYING && var1.getItem() == Items.TOTEM_OF_UNDYING) {
         boolean var33 = false;
         NbtComponent var39 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
         if (var39 != null) {
            NbtCompound var43 = var39.copyNbt();
            if (var43 != null && var43.contains("AttributeModifiers", 9)) {
               NbtList var46 = var43.getList("AttributeModifiers", 10);
               if (!var46.isEmpty()) {
                  var33 = true;
                  return compareTalismanByAttributes(var0, var1);
               }
            }
         }

         boolean var44 = false;
         NbtComponent var47 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var47 != null) {
            NbtCompound var50 = var47.copyNbt();
            if (var50 != null && var50.contains("AttributeModifiers", 9)) {
               NbtList var53 = var50.getList("AttributeModifiers", 10);
               if (!var53.isEmpty()) {
                  var44 = true;
               }
            }
         }

         return var33 || !var44;
      } else {
         NbtComponent var32 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
         if (var32 != null) {
            NbtCompound var14 = var32.copyNbt();
            if (var14 != null && var14.getBoolean("HolyWorldSphere")) {
               return compareHolyWorldSphere(var0, var1);
            }
         }

         if (var0.getItem() == Items.PLAYER_HEAD && var1.getItem() == Items.PLAYER_HEAD) {
            NbtComponent var38 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
            if (var38 != null) {
               NbtCompound var42 = var38.copyNbt();
               if (var42 != null && var42.contains("AttributeModifiers", 9)) {
                  return compareTalismanByAttributes(var0, var1);
               }
            }

            return false;
         } else {
            LoreComponent var37 = (LoreComponent)var0.get(DataComponentTypes.LORE);
            LoreComponent var15 = (LoreComponent)var1.get(DataComponentTypes.LORE);
            boolean var16 = var15 != null && !var15.lines().isEmpty();
            if (var16) {
               List<Text> var49 = var15.lines();
               if (var37 != null && !var37.lines().isEmpty()) {
                  List<String> var52 = var37.lines()
                     .stream()
                     .map(var0x -> cleanString(var0x.getString()))
                     .filter(var0x -> !var0x.isEmpty())
                     .collect(Collectors.toList());
                  String var54 = String.join(" ", var52);
                  boolean var55 = false;

                  for (String var58 : var52) {
                     if (var58.contains("оригинальный предмет") || var58.contains("★")) {
                        var55 = true;
                        break;
                     }
                  }

                  int var57 = 0;
                  int var59 = 0;

                  for (Text var61 : var49) {
                     String var62 = cleanString(var61.getString());
                     if (!var62.isEmpty()) {
                        boolean var63 = var62.contains("оригинальный предмет") || var62.contains("★");
                        if (var63) {
                           if (!var55) {
                              return false;
                           }

                           var57++;
                           var59++;
                        } else {
                           var59++;
                           boolean var64 = false;
                           Iterator var28 = var52.iterator();

                           while (true) {
                              if (var28.hasNext()) {
                                 String var29 = (String)var28.next();
                                 if (!var29.contains(var62) && !var62.contains(var29)) {
                                    continue;
                                 }

                                 var64 = true;
                              }

                              if (!var64 && var54.contains(var62)) {
                                 var64 = true;
                              }

                              if (var64) {
                                 var57++;
                              }
                              break;
                           }
                        }
                     }
                  }

                  return var57 >= var59;
               } else {
                  return false;
               }
            } else {
               if (var0.getItem() == Items.SPLASH_POTION && var1.getItem() == Items.SPLASH_POTION
                  || var0.getItem() == Items.POTION && var1.getItem() == Items.POTION) {
                  PotionContentsComponent var17 = (PotionContentsComponent)var0.get(DataComponentTypes.POTION_CONTENTS);
                  PotionContentsComponent var18 = (PotionContentsComponent)var1.get(DataComponentTypes.POTION_CONTENTS);
                  if (var18 != null) {
                     ArrayList<StatusEffectInstance> var19 = new ArrayList<>();
                     var18.getEffects().forEach(var19::add);
                     if (!var19.isEmpty()) {
                        if (var17 == null) {
                           return false;
                        }

                        ArrayList<StatusEffectInstance> var20 = new ArrayList<>();
                        var17.getEffects().forEach(var20::add);
                        if (var20.isEmpty()) {
                           return false;
                        }

                        ArrayList<String> var21 = new ArrayList<>();

                        for (StatusEffectInstance var23 : var19) {
                           boolean var24 = false;

                           for (StatusEffectInstance var26 : var20) {
                              if (var26.getEffectType().equals(var23.getEffectType()) && var26.getAmplifier() == var23.getAmplifier()) {
                                 if (var23.getDuration() <= 0) {
                                    var24 = true;
                                    var21.add(((StatusEffect)var23.getEffectType().value()).getName().getString() + "=" + var23.getAmplifier());
                                    break;
                                 }

                                 int var27 = Math.abs(var26.getDuration() - var23.getDuration());
                                 if (var27 <= 20) {
                                    var24 = true;
                                    var21.add(
                                       ((StatusEffect)var23.getEffectType().value()).getName().getString()
                                          + "="
                                          + var23.getAmplifier()
                                          + " (длительность: "
                                          + var26.getDuration()
                                          + " тиков)"
                                    );
                                    break;
                                 }
                              }
                           }

                           if (!var24) {
                              return false;
                           }
                        }

                        return true;
                     }
                  }
               }

               return compareByEnchantments(var0, var1);
            }
         }
      }
   }

   public static String getEnchantmentName(String var0) {
      switch (var0) {
         case "protection":
            return "защита";
         case "fire_protection":
            return "огнестойкость";
         case "blast_protection":
            return "взрывоустойчивость";
         case "projectile_protection":
            return "защита от снарядов";
         case "thorns":
            return "шипы";
         case "unbreaking":
            return "прочность";
         case "sharpness":
            return "острота";
         case "smite":
            return "небесная кара";
         case "bane_of_arthropods":
            return "гибель насекомых";
         case "knockback":
            return "отбрасывание";
         case "fire_aspect":
            return "заговор огня";
         case "looting":
            return "добыча";
         case "efficiency":
            return "эффективность";
         case "fortune":
            return "удача";
         case "silk_touch":
            return "шелковое касание";
         case "power":
            return "сила";
         case "punch":
            return "отдача";
         case "flame":
            return "пламя";
         case "infinity":
            return "бесконечность";
         case "mending":
            return "починка";
         case "frost_walker":
            return "ледяной путь";
         case "depth_strider":
            return "подводная ходьба";
         case "respiration":
            return "дыхание";
         case "aqua_affinity":
            return "подводная добыча";
         case "sweeping_edge":
            return "разящий клинок";
         default:
            return null;
      }
   }

   private static int getHolyWorldArmorVariant(ItemStack var0) {
      LoreComponent var1 = (LoreComponent)var0.get(DataComponentTypes.LORE);
      if (var1 != null && !var1.lines().isEmpty()) {
         for (Text var3 : var1.lines()) {
            String var4 = var3.getString();
            if (var4 != null) {
               if (var4.contains("Непробиваемый II")) {
                  return 2;
               }

               if (var4.contains("Непробиваемый I")) {
                  return 1;
               }
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static boolean holyWorldArmorVariantMatchesByNbt(ItemStack var0, ItemStack var1) {
      int var2 = getHolyWorldArmorVariant(var1);
      if (var2 == 0) {
         return true;
      }

      int var3 = getHolyWorldArmorVariant(var0);
      return var3 == 0 ? true : var3 == var2;
   }

   public static boolean compareByEnchantments(ItemStack var0, ItemStack var1) {
      ItemEnchantmentsComponent var2 = (ItemEnchantmentsComponent)var0.get(DataComponentTypes.ENCHANTMENTS);
      NbtComponent var3 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      NbtList var4 = null;
      if (var3 != null) {
         NbtCompound var5 = var3.copyNbt();
         if (var5 != null && var5.contains("RequiredEnchantments", 9)) {
            var4 = var5.getList("RequiredEnchantments", 10);
         }
      }

      ItemEnchantmentsComponent var15 = (ItemEnchantmentsComponent)var1.get(DataComponentTypes.ENCHANTMENTS);
      if (var15 != null && !var15.isEmpty() || var4 != null && !var4.isEmpty()) {
         if (var2 != null && !var2.isEmpty()) {
            ArrayList var6 = new ArrayList();
            if (var4 != null && !var4.isEmpty()) {
               for (int var16 = 0; var16 < var4.size(); var16++) {
                  NbtCompound var18 = var4.getCompound(var16);
                  String var20 = var18.getString("id");
                  short var22 = var18.getShort("lvl");
                  int var11 = 0;
                  boolean var12 = false;

                  for (RegistryEntry var14 : var2.getEnchantments()) {
                     if (var14.getIdAsString().equals(var20)) {
                        var11 = var2.getLevel(var14);
                        var12 = true;
                        break;
                     }
                  }

                  if (var22 == -1) {
                     if (!var12) {
                        return false;
                     }

                     var6.add(var20 + "=" + var11 + " (требуется любой уровень)");
                  } else {
                     if (!var12 || var11 < var22) {
                        return false;
                     }

                     var6.add(var20 + "=" + var11 + " (требуется " + var22 + ")");
                  }
               }
            } else if (var15 != null && !var15.isEmpty()) {
               for (RegistryEntry var8 : var15.getEnchantments()) {
                  int var9 = var15.getLevel(var8);
                  int var10 = var2.getLevel(var8);
                  if (var10 < var9) {
                     return false;
                  }

                  var6.add(var8.getIdAsString() + "=" + var10 + " (требуется " + var9 + ")");
               }
            }

            LoreComponent var17 = (LoreComponent)var0.get(DataComponentTypes.LORE);
            LoreComponent var19 = (LoreComponent)var1.get(DataComponentTypes.LORE);
            if (var19 != null && !var19.lines().isEmpty()) {
               if (var17 == null || var17.lines().isEmpty()) {
                  return false;
               }

               List<String> var21 = var17.lines().stream().map(var0x -> cleanString(var0x.getString())).filter(var0x -> !var0x.isEmpty()).toList();
               ArrayList<String> var23 = new ArrayList<>();

               for (Text var25 : var19.lines()) {
                  String var26 = cleanString(var25.getString());
                  if (!var26.isEmpty()) {
                     boolean var27 = var21.stream().anyMatch(var1x -> var1x.contains(var26) || var26.contains(var1x));
                     if (!var27) {
                        return false;
                     }

                     var23.add(var26);
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldSphere(ItemStack var0, ItemStack var1) {
      String var2 = var0.getName().getString();
      String var3 = var1.getName().getString();
      NbtComponent var4 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var4 == null) {
         return false;
      }

      NbtCompound var5 = var4.copyNbt();
      if (var5 == null) {
         return false;
      }

      String var6 = var5.getString("sphereName");
      String var7 = var5.getString("requiredEffects");
      if (var6 != null && !var6.isEmpty()) {
         NbtComponent var8 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var8 == null) {
            return false;
         }

         NbtCompound var9 = var8.copyNbt();
         if (var9 == null) {
            return false;
         }

         if (!var9.contains("sphereEffect", 10)) {
            if (var9.contains("sphereEffect", 8)) {
            }

            return false;
         } else {
            NbtCompound var10 = var9.getCompound("sphereEffect");
            String var11 = var10.getString("name");
            String var12 = var10.getString("effects");
            if (!var6.equals(var11)) {
               return false;
            }

            if (var7 != null && !var7.isEmpty()) {
               if (var12 != null && !var12.isEmpty()) {
                  String[] var22 = var7.split(",");

                  for (String var17 : var22) {
                     String[] var18 = var17.trim().split(":");
                     if (var18.length == 2) {
                        String var19 = var18[0].trim();
                        String var20 = var18[1].trim();
                        boolean var21 = var12.contains("\"nbtName\":\"" + var19 + "\"") && var12.contains("\"lvl\":" + var20);
                        if (!var21) {
                           return false;
                        }
                     }
                  }

                  return true;
               } else {
                  return false;
               }
            } else if (var12 != null && !var12.isEmpty()) {
               int var13 = 0;

               for (int var23 = 0; (var23 = var12.indexOf("\"nbtName\"", var23)) != -1; var23++) {
                  var13++;
               }

               return var13 <= 1;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean compareSphereEffectsToAttributes(String var0, NbtList var1) {
      if (var0 != null && !var0.isEmpty() && !var1.isEmpty()) {
         List<AuctionUtils.SphereEffect> var2 = parseSphereEffects(var0);
         if (var2.isEmpty()) {
            return false;
         }

         HashSet<AuctionUtils.AttributeInfo> var3 = new HashSet<>();

         for (int var4 = 0; var4 < var1.size(); var4++) {
            NbtCompound var5 = var1.getCompound(var4);
            String var6 = var5.getString("AttributeName");
            double var7 = var5.getDouble("Amount");
            int var9 = var5.getInt("Operation");
            String var10 = var5.getString("Slot");
            var3.add(new AuctionUtils.AttributeInfo(var6, var7, var9, var10));
         }

         for (AuctionUtils.AttributeInfo var14 : var3) {
            boolean var15 = false;

            for (AuctionUtils.SphereEffect var8 : var2) {
               String var17 = mapEffectToAttribute(var8.nbtName);
               if (var17 != null) {
                  double var18 = mapEffectLevelToAmount(var8.nbtName, var8.lvl);
                  int var12 = mapEffectToOperation(var8.nbtName);
                  if (var14.attributeName.equals(var17) && Math.abs(var14.amount - var18) < 0.01 && var14.operation == var12 && var14.slot.equals("offhand")) {
                     var15 = true;
                     break;
                  }
               }
            }

            if (!var15) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static List<AuctionUtils.SphereEffect> parseSphereEffects(String var0) {
      ArrayList<AuctionUtils.SphereEffect> var1 = new ArrayList<>();
      if (var0 != null && !var0.isEmpty()) {
         Pattern var2 = Pattern.compile("\\{\"lvl\":(\\d+),\"nbtName\":\"([^\"]+)\"\\}");
         Matcher var3 = var2.matcher(var0);

         while (var3.find()) {
            int var4 = Integer.parseInt(var3.group(1));
            String var5 = var3.group(2);
            var1.add(new AuctionUtils.SphereEffect(var5, var4));
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static String mapEffectToAttribute(String var0) {
      switch (var0) {
         case "hms-damage":
            return "minecraft:generic.attack_damage";
         case "hms-rush":
         case "hms-speed":
            return "minecraft:generic.movement_speed";
         case "hms-armor":
            return "minecraft:generic.armor";
         case "hms-health":
            return "minecraft:generic.max_health";
         default:
            return null;
      }
   }

   private static double mapEffectLevelToAmount(String var0, int var1) {
      switch (var0) {
         case "hms-damage":
            return var1;
         case "hms-rush":
            return 0.1 * var1;
         case "hms-speed":
            return 0.1 * var1;
         case "hms-armor":
            return var1;
         case "hms-health":
            return var1;
         default:
            return 0.0;
      }
   }

   private static int mapEffectToOperation(String var0) {
      switch (var0) {
         case "hms-rush":
         case "hms-speed":
            return 1;
         case "hms-damage":
         case "hms-armor":
         case "hms-health":
            return 0;
         default:
            return 0;
      }
   }

   private static String normalizeAttributeName(String var0) {
      if (var0 == null || var0.isEmpty()) {
         return var0;
      } else {
         return !var0.contains(":") ? "minecraft:" + var0 : var0;
      }
   }

   private static boolean attributeNamesMatch(String var0, String var1) {
      String var2 = normalizeAttributeName(var0);
      String var3 = normalizeAttributeName(var1);
      return var2.equals(var3) || var3.equals(var2) || var3.endsWith(var2) || var2.endsWith(var3.replace("minecraft:", ""));
   }

   private static boolean compareAttributesNbtToComponents(NbtList var0, AttributeModifiersComponent var1) {
      HashSet<AuctionUtils.AttributeInfo> var2 = new HashSet<>();

      for (int var3 = 0; var3 < var0.size(); var3++) {
         NbtCompound var4 = var0.getCompound(var3);
         String var5 = var4.getString("AttributeName");
         double var6 = var4.getDouble("Amount");
         int var8 = var4.getInt("Operation");
         String var9 = var4.getString("Slot");
         var2.add(new AuctionUtils.AttributeInfo(var5, var6, var8, var9));
      }

      int var15 = 0;

      for (Entry var18 : var1.modifiers()) {
         var15++;
      }

      if (var2.size() != var15) {
         return false;
      }

      for (AuctionUtils.AttributeInfo var19 : var2) {
         boolean var20 = false;

         for (Entry var21 : var1.modifiers()) {
            String var22 = var21.attribute().getIdAsString();
            double var10 = var21.modifier().value();
            int var12 = var21.modifier().operation().getId();
            String var13 = var21.slot().asString();
            boolean var14 = attributeNamesMatch(var19.attributeName, var22);
            if (var14 && Math.abs(var19.amount - var10) < 0.01 && var19.operation == var12 && var19.slot.equals(var13)) {
               var20 = true;
               break;
            }
         }

         if (!var20) {
            return false;
         }
      }

      return true;
   }

   public static boolean compareHolyWorldExpBottle(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldExpBottle")) {
         int var4 = var3.getInt("holy-exp-bottle-value");
         NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var5 != null) {
            NbtCompound var6 = var5.copyNbt();
            if (var6 == null) {
               ComponentMap var9 = var0.getComponents();
               if (var9 != null) {
                  String var11 = var9.toString();
                  if (var4 == 0) {
                     if (!var11.contains("holy-exp-bottle-value")) {
                        return true;
                     }

                     if (!var11.contains("holy-exp-bottle-value:315")
                        && !var11.contains("holy-exp-bottle-value:5345")
                        && !var11.contains("holy-exp-bottle-value:30971")) {
                        return true;
                     }
                  } else {
                     String var8 = "holy-exp-bottle-value:" + var4;
                     if (var11.contains(var8)) {
                        return true;
                     }
                  }
               }

               return false;
            } else if (var4 == 0) {
               if (!var6.contains("holy-exp-bottle-value")) {
                  return true;
               }

               int var10 = var6.getInt("holy-exp-bottle-value");
               return var10 != 315 && var10 != 5345 && var10 != 30971;
            } else if (var6.contains("holy-exp-bottle-value")) {
               int var7 = var6.getInt("holy-exp-bottle-value");
               return var7 == var4;
            } else {
               return false;
            }
         } else {
            return var4 == 0;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldBackpack(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldBackpack")) {
         String var4 = var3.getString("backpackType");
         if (var4 != null && !var4.isEmpty()) {
            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null && var6.contains("PublicBukkitValues", 10)) {
                  NbtCompound var7 = var6.getCompound("PublicBukkitValues");
                  if (var7.contains("litebackpacks:backpack")) {
                     String var8 = var7.getString("litebackpacks:backpack");
                     if (var4.equals(var8)) {
                        return true;
                     }
                  }
               }
            }

            ComponentMap var9 = var0.getComponents();
            if (var9 != null) {
               String var10 = var9.toString();
               if (var10.contains("litebackpacks:backpack") && var10.contains("\"" + var4 + "\"")) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldPyrotechnic(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldPyrotechnic")) {
         String var4 = var3.getString("pyrotechnicType");
         if (var4 != null && !var4.isEmpty()) {
            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null && var6.contains("pyrotechnic-item", 10)) {
                  NbtCompound var7 = var6.getCompound("pyrotechnic-item");
                  if (var7.contains("name")) {
                     String var8 = var7.getString("name");
                     if (var4.equals(var8)) {
                        return true;
                     }
                  }
               }
            }

            ComponentMap var9 = var0.getComponents();
            if (var9 != null) {
               String var10 = var9.toString();
               if (var10.contains("pyrotechnic-item") && var10.contains("\"" + var4 + "\"")) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldKringe(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldKringe")) {
         String var4 = var3.getString("kringeType");
         if (var4 != null && !var4.isEmpty()) {
            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null && var6.contains("kringeItems", 10)) {
                  NbtCompound var7 = var6.getCompound("kringeItems");
                  if (var7.contains("type")) {
                     String var8 = var7.getString("type");
                     if (var4.equals(var8)) {
                        return true;
                     }
                  }
               }
            }

            ComponentMap var9 = var0.getComponents();
            if (var9 != null) {
               String var10 = var9.toString();
               if (var10.contains("kringeItems") && var10.contains("\"" + var4 + "\"")) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldRune(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldRune")) {
         String var4 = var3.getString("runeId");
         if (var4 != null && !var4.isEmpty()) {
            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null && var6.contains("PublicBukkitValues", 10)) {
                  NbtCompound var7 = var6.getCompound("PublicBukkitValues");
                  if (var7.contains("literunes:rune-id")) {
                     String var8 = var7.getString("literunes:rune-id");
                     if (var4.equals(var8)) {
                        return true;
                     }
                  }
               }
            }

            ComponentMap var9 = var0.getComponents();
            if (var9 != null) {
               String var10 = var9.toString();
               if (var10.contains("literunes:rune-id") && var10.contains("\"" + var4 + "\"")) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldTalik(ItemStack var0, ItemStack var1) {
      return compareTalismanByAttributes(var0, var1);
   }

   public static boolean compareHolyWorldSphereShard(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldSphereShard")) {
         NbtComponent var4 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
         if (var4 != null) {
            NbtCompound var5 = var4.copyNbt();
            if (var5 != null) {
               if (var5.contains("PublicBukkitValues", 10)) {
                  NbtCompound var6 = var5.getCompound("PublicBukkitValues");
                  if (var6.contains("magicspheres:burned-sphere-shard")) {
                     byte var7 = var6.getByte("magicspheres:burned-sphere-shard");
                     if (var7 != 0) {
                        if (!var5.contains("sphereEffect")) {
                           return true;
                        }

                        int var8 = var5.getInt("sphereEffect");
                        if (var8 == 1) {
                           return true;
                        }
                     }
                  }
               }

               if (var5.contains("sphereEffect")) {
                  int var10 = var5.getInt("sphereEffect");
                  if (var10 == 1) {
                     return true;
                  }
               }
            }
         }

         ComponentMap var9 = var0.getComponents();
         if (var9 != null) {
            String var11 = var9.toString();
            if (var11.contains("magicspheres:burned-sphere-shard") || var11.contains("sphereEffect:1")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldKringeEffect(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldKringeEffect")) {
         String var4 = var3.getString("effectType");
         if (var4 != null && !var4.isEmpty()) {
            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null && var6.contains("kringeEffect", 10)) {
                  NbtCompound var7 = var6.getCompound("kringeEffect");
                  if (var7.contains("type")) {
                     String var8 = var7.getString("type");
                     if (var4.equals(var8)) {
                        return true;
                     }
                  }
               }
            }

            ComponentMap var9 = var0.getComponents();
            if (var9 != null) {
               String var10 = var9.toString();
               if (var10.contains("kringeEffect") && var10.contains("\"" + var4 + "\"")) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldPotion(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldPotion")) {
         String var4 = var3.getString("effectId");
         int var5 = var3.getInt("amplifier");
         NbtList var6 = var3.getList("allowedDurations", 10);
         ArrayList<Integer> var7 = new ArrayList<>();

         for (NbtElement var9 : var6) {
            if (var9 instanceof NbtCompound var10 && var10.contains("duration", 3)) {
               var7.add(var10.getInt("duration"));
            }
         }

         PotionContentsComponent var19 = (PotionContentsComponent)var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var19 == null) {
            return false;
         }

         Optional var20 = var19.potion();
         if (var20.isPresent()) {
            String var21 = ((RegistryEntry)var20.get()).getIdAsString();
            if (var21.equals("minecraft:strong_healing") && var4.equals("minecraft:instant_health") && var5 == 1) {
               return true;
            }
         }

         ArrayList<StatusEffectInstance> var22 = new ArrayList<>();
         var19.getEffects().forEach(var22::add);
         if (var22.isEmpty()) {
            return false;
         }

         for (StatusEffectInstance var12 : var22) {
            String var13 = var12.getEffectType().getIdAsString();
            int var14 = var12.getAmplifier();
            if (var13.equals(var4) && var14 == var5) {
               int var15 = var12.getDuration();
               if (var7.isEmpty()) {
                  return true;
               }

               for (Integer var17 : var7) {
                  int var18 = Math.abs(var15 - var17);
                  if (var18 <= 20) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldStandardPotion(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 == null) {
         return false;
      }

      if (!var3.getBoolean("HolyWorldStandardPotion")) {
         return false;
      }

      String var4 = var3.getString("potionType");
      if (var4 != null && !var4.isEmpty()) {
         PotionContentsComponent var5 = (PotionContentsComponent)var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var5 == null) {
            NbtComponent var11 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var11 != null) {
               NbtCompound var13 = var11.copyNbt();
               if (var13 != null && var13.contains("Potion", 8)) {
                  String var15 = var13.getString("Potion");
                  if (var15.equals(var4)) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            Optional var6 = var5.potion();
            if (var6.isEmpty()) {
               NbtComponent var12 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
               if (var12 != null) {
                  NbtCompound var14 = var12.copyNbt();
                  if (var14 != null && var14.contains("Potion", 8)) {
                     String var16 = var14.getString("Potion");
                     if (var16.equals(var4)) {
                        return true;
                     }
                  }
               }

               return false;
            } else {
               String var7 = ((RegistryEntry)var6.get()).getIdAsString();
               if (var7.equals(var4)) {
                  return true;
               }

               NbtComponent var8 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
               if (var8 != null) {
                  NbtCompound var9 = var8.copyNbt();
                  if (var9 != null && var9.contains("Potion", 8)) {
                     String var10 = var9.getString("Potion");
                     if (var10.equals(var4)) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean compareHolyWorldMultiEffectPotion(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("HolyWorldMultiEffectPotion")) {
         NbtList var4 = var3.getList("effects", 10);
         if (var4.isEmpty()) {
            return false;
         }

         PotionContentsComponent var5 = (PotionContentsComponent)var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var5 == null) {
            return false;
         }

         ArrayList<StatusEffectInstance> var6 = new ArrayList<>();
         var5.getEffects().forEach(var6::add);
         if (var6.isEmpty()) {
            return false;
         }

         int var7 = 0;

         for (NbtElement var9 : var4) {
            if (var9 instanceof NbtCompound var10) {
               String var11 = var10.getString("effectId");
               int var12 = var10.getInt("amplifier");
               int var13 = var10.getInt("duration");
               boolean var14 = false;

               for (StatusEffectInstance var16 : var6) {
                  String var17 = var16.getEffectType().getIdAsString();
                  int var18 = var16.getAmplifier();
                  int var19 = var16.getDuration();
                  if (var17.equals(var11) && var18 == var12) {
                     int var20 = Math.abs(var19 - var13);
                     if (var20 <= 20 || var13 == 0) {
                        var14 = true;
                        var7++;
                        break;
                     }
                  }
               }

               if (!var14) {
                  return false;
               }
            }
         }

         return var7 == var4.size();
      } else {
         return false;
      }
   }

   public static boolean compareSpookyTimeSpecial(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("SpookyTimeSpecial")) {
         String var4 = var3.getString("spookyItemType");
         if (var4 != null && !var4.isEmpty()) {
            if (var0.getItem() != var1.getItem()) {
               return false;
            }

            NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
            if (var5 != null) {
               NbtCompound var6 = var5.copyNbt();
               if (var6 != null) {
                  if (var6.contains("PublicBukkitValues", 10)) {
                     NbtCompound var7 = var6.getCompound("PublicBukkitValues");

                     for (String var9 : var7.getKeys()) {
                        if (var9.endsWith(var4)) {
                           return true;
                        }
                     }
                  }

                  if (var6.contains("spookyItemType", 8)) {
                     String var10 = var6.getString("spookyItemType");
                     if (var10.endsWith(var4)) {
                        return true;
                     }
                  }

                  ComponentMap var11 = var0.getComponents();
                  if (var11 != null) {
                     String var12 = var11.toString();
                     if (var12.contains(var4)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean compareSpookyTimePotion(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 != null && var3.getBoolean("SpookyTimePotion")) {
         NbtList var4 = var3.getList("effects", 10);
         if (var4.isEmpty()) {
            return false;
         }

         PotionContentsComponent var5 = (PotionContentsComponent)var0.get(DataComponentTypes.POTION_CONTENTS);
         if (var5 == null) {
            return false;
         }

         ArrayList<StatusEffectInstance> var6 = new ArrayList<>();
         var5.getEffects().forEach(var6::add);
         if (var6.isEmpty()) {
            return false;
         }

         int var7 = 0;

         for (NbtElement var9 : var4) {
            if (var9 instanceof NbtCompound var10) {
               String var11 = var10.getString("effectId");
               int var12 = var10.getInt("amplifier");
               int var13 = var10.getInt("duration");
               boolean var14 = false;

               for (StatusEffectInstance var16 : var6) {
                  String var17 = var16.getEffectType().getIdAsString();
                  int var18 = var16.getAmplifier();
                  int var19 = var16.getDuration();
                  if (var17.equals(var11) && var18 == var12) {
                     int var20 = Math.abs(var19 - var13);
                     if (var20 <= 20 || var13 == 0) {
                        var14 = true;
                        var7++;
                        break;
                     }
                  }
               }

               if (!var14) {
                  return false;
               }
            }
         }

         return var7 == var4.size();
      } else {
         return false;
      }
   }

   public static boolean compareTalismanByAttributes(ItemStack var0, ItemStack var1) {
      NbtComponent var2 = (NbtComponent)var1.get(DataComponentTypes.CUSTOM_DATA);
      if (var2 == null) {
         return false;
      }

      NbtCompound var3 = var2.copyNbt();
      if (var3 == null) {
         return false;
      }

      NbtList var4 = var3.getList("AttributeModifiers", 10);
      if (var4.isEmpty()) {
         return false;
      }

      NbtComponent var5 = (NbtComponent)var0.get(DataComponentTypes.CUSTOM_DATA);
      NbtList var6 = null;
      if (var5 != null) {
         NbtCompound var7 = var5.copyNbt();
         if (var7 != null) {
            var6 = var7.getList("AttributeModifiers", 10);
         }
      }

      AttributeModifiersComponent var24 = (AttributeModifiersComponent)var0.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if ((var6 == null || var6.isEmpty()) && var24 != null) {
         return compareAttributesNbtToComponents(var4, var24);
      }

      if (var6 != null && !var6.isEmpty()) {
         if (var6.size() < var4.size() && var5 != null) {
            NbtCompound var25 = var5.copyNbt();
            if (var25 != null && var25.contains("sphereEffect", 10)) {
               NbtCompound var27 = var25.getCompound("sphereEffect");
               if (var27.contains("effects", 8)) {
                  String var32 = var27.getString("effects");
                  return compareSphereEffectsToAttributes(var32, var4);
               }
            }
         }

         HashSet<AuctionUtils.AttributeInfo> var26 = new HashSet<>();

         for (int var28 = 0; var28 < var4.size(); var28++) {
            NbtCompound var30 = var4.getCompound(var28);
            String var11 = var30.getString("AttributeName");
            double var12 = var30.getDouble("Amount");
            int var14 = var30.getInt("Operation");
            String var15 = var30.getString("Slot");
            var26.add(new AuctionUtils.AttributeInfo(var11, var12, var14, var15));
         }

         ArrayList<String> var29 = new ArrayList<>();

         for (AuctionUtils.AttributeInfo var33 : var26) {
            boolean var34 = false;

            for (int var13 = 0; var13 < var6.size(); var13++) {
               NbtCompound var35 = var6.getCompound(var13);
               String var36 = var35.getString("AttributeName");
               double var16 = var35.getDouble("Amount");
               int var18 = var35.getInt("Operation");
               String var19 = var35.getString("Slot");
               boolean var20 = attributeNamesMatch(var33.attributeName, var36);
               boolean var21 = Math.abs(var33.amount - var16) < 0.01;
               boolean var22 = var33.operation == var18;
               boolean var23 = var33.slot.equals(var19);
               if (var20 && var22 && var23 && var21) {
                  var34 = true;
                  var29.add(var36 + "=" + var16 + " (op=" + var18 + ", slot=" + var19 + ")");
                  break;
               }
            }

            if (!var34) {
               return false;
            }
         }

         return true;
      } else {
         if (var5 != null) {
            NbtCompound var8 = var5.copyNbt();
            if (var8 != null && var8.contains("sphereEffect", 10)) {
               NbtCompound var9 = var8.getCompound("sphereEffect");
               if (var9.contains("effects", 8)) {
                  String var10 = var9.getString("effects");
                  return compareSphereEffectsToAttributes(var10, var4);
               }
            }
         }

         return false;
      }
   }

   private static class AttributeInfo {
      final String attributeName;
      final double amount;
      final int operation;
      final String slot;

      AttributeInfo(String var1, double var2, int var4, String var5) {
         this.attributeName = var1;
         this.amount = var2;
         this.operation = var4;
         this.slot = var5;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            AuctionUtils.AttributeInfo var2 = (AuctionUtils.AttributeInfo)var1;
            return Double.compare(var2.amount, this.amount) == 0
               && this.operation == var2.operation
               && this.attributeName.equals(var2.attributeName)
               && this.slot.equals(var2.slot);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.attributeName, this.amount, this.operation, this.slot);
      }
   }

   private static class SphereEffect {
      final String nbtName;
      final int lvl;

      SphereEffect(String var1, int var2) {
         this.nbtName = var1;
         this.lvl = var2;
      }
   }
}
