package com.isusdlc.display.screens.autobuy;

import com.isusdlc.features.autobuy.AutoBuyModule;
import com.isusdlc.features.autobuy.CommandSender;
import com.isusdlc.features.autobuy.items.AutoBuyableItem;
import com.isusdlc.features.autobuy.originalitems.ItemRegistry;
import com.isusdlc.features.autobuy.settings.AutoBuyItemSettings;
import com.isusdlc.features.autobuy.settings.AutoBuySettingsManager;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.Map.Entry;

public class AutoBuyScreen extends Screen implements IMinecraft {
   public static AutoBuyScreen INSTANCE = new AutoBuyScreen();
   private final AutoBuyModule autoBuy = AutoBuyModule.getInstance();
   private float animProgress = 1.0F;
   private float settingsAnimProgress = 0.0F;
   private boolean settingsAnimDir = false;
   private float scroll = 0.0F;
   private float smoothedScroll = 0.0F;
   private AutoBuyableItem settingsItem = null;
   private AutoBuyableItem settingsAnimatedItem = null;
   private AutoBuyableItem selectedItem = null;
   private String priceInput = "";
   private boolean isEditingPrice = false;
   private boolean isTextSelected = false;
   private int cursorPosition = 0;
   private final HashMap<AutoBuyableItem, Integer> customPrices = new HashMap<>();
   private final Map<String, Set<String>> parserEnabledItemsByServer = new HashMap<>();
   private final Map<String, Map<String, Float>> durabilitySettingsByServer = new HashMap<>();
   private final Map<AutoBuyableItem, Boolean> parserSettings = new HashMap<>();
   private final Map<AutoBuyableItem, Float> durabilitySettings = new HashMap<>();
   private final Map<AutoBuyableItem, Boolean> sliderDragging = new HashMap<>();
   private long buttonClickTime = 0L;
   private float x, y;
   private final float panelWidth = 284.5F;
   private final float panelHeight = 290.0F;
   private String searchText = "";
   private boolean showSearch = false;
   private String searchInput = "";

   public AutoBuyScreen() {
      super(Text.of("AutoBuyScreen"));
   }

   public void openGui() {
      cleanupDuplicatePrices();
      animProgress = 0.0F;
      mc.setScreen(this);
   }

   private void cleanupDuplicatePrices() {
      HashMap<String, AutoBuyableItem> nameMap = new HashMap<>();
      HashMap<String, Integer> priceMap = new HashMap<>();
      for (Entry<AutoBuyableItem, Integer> e : customPrices.entrySet()) {
         AutoBuyableItem item = e.getKey();
         if (item != null && item.getDisplayName() != null) {
            nameMap.put(item.getDisplayName(), item);
            priceMap.put(item.getDisplayName(), e.getValue());
         }
      }
      customPrices.clear();
      for (Entry<String, AutoBuyableItem> e : nameMap.entrySet()) {
         Integer price = priceMap.get(e.getKey());
         if (price != null && price > 0) customPrices.put(e.getValue(), price);
      }
   }

   public Map<AutoBuyableItem, Integer> getCustomPrices() { return customPrices; }

   public List<AutoBuyableItem> getItemsForCurrentProfile() {
      AutoBuyModule ab = autoBuy;
      if (ab == null) return ItemRegistry.getFunTimeItems();
      switch (ab.getServerMode()) {
         case SPOOKYTIME: return ItemRegistry.getSpookyTime();
         case HOLYWORLD: return ItemRegistry.getHolyWorld();
         default: return ItemRegistry.getFunTimeItems();
      }
   }

   private AutoBuyModule.AutoBuyServerMode getCurrentServerMode() {
      return autoBuy != null ? autoBuy.getServerMode() : AutoBuyModule.AutoBuyServerMode.FUNTIME;
   }

   public boolean isParserEnabled(AutoBuyableItem item) {
      if (item == null) return false;
      Boolean b = parserSettings.get(item);
      if (b != null) return b;
      String name = item.getDisplayName();
      if (name == null) return false;
      String mode = getCurrentServerMode().name();
      Set<String> set = parserEnabledItemsByServer.getOrDefault(mode, new HashSet<>());
      return set.contains(name.toLowerCase());
   }

   public void toggleParserEnabled(AutoBuyableItem item) {
      if (item == null) return;
      String name = item.getDisplayName();
      if (name == null) return;
      String mode = getCurrentServerMode().name();
      Set<String> set = parserEnabledItemsByServer.computeIfAbsent(mode, k -> new HashSet<>());
      String lower = name.toLowerCase();
      if (set.contains(lower)) set.remove(lower);
      else set.add(lower);
      parserSettings.put(item, set.contains(lower));
   }

   public List<String> getParserEnabledItemNames() {
      String mode = getCurrentServerMode().name();
      Set<String> set = parserEnabledItemsByServer.getOrDefault(mode, new HashSet<>());
      Set<String> dedup = new LinkedHashSet<>();
      List<String> result = new ArrayList<>();
      for (AutoBuyableItem item : getItemsForCurrentProfile()) {
         if (item != null && item.getDisplayName() != null) {
            String lower = item.getDisplayName().toLowerCase();
            if (set.contains(lower) && dedup.add(lower)) result.add(item.getDisplayName());
         }
      }
      return result;
   }

   public void setParserEnabledItemNames(Collection<String> names) {
      String mode = getCurrentServerMode().name();
      Set<String> set = parserEnabledItemsByServer.computeIfAbsent(mode, k -> new HashSet<>());
      set.clear();
      if (names != null) {
         for (String n : names) {
            if (n != null) {
               String t = n.trim().toLowerCase();
               if (!t.isEmpty()) set.add(t);
            }
         }
      }
   }

   public Map<String, Set<String>> getParserEnabledItemsByServer() { return parserEnabledItemsByServer; }

   public void setParserEnabledItemsByServer(Map<String, Set<String>> map) {
      if (map != null) {
         for (Entry<String, Set<String>> e : map.entrySet()) {
            parserEnabledItemsByServer.put(e.getKey(), e.getValue());
         }
      }
      syncParserSettingsWithServerData();
   }

   public void replaceParserEnabledItemsByServer(Map<String, Set<String>> map) {
      parserEnabledItemsByServer.clear();
      if (map != null) parserEnabledItemsByServer.putAll(map);
      syncParserSettingsWithServerData();
   }

   public void syncParserSettingsWithServerData() {
      String mode = getCurrentServerMode().name();
      Set<String> set = parserEnabledItemsByServer.getOrDefault(mode, new HashSet<>());
      for (Entry<AutoBuyableItem, Boolean> e : parserSettings.entrySet()) {
         AutoBuyableItem item = e.getKey();
         if (item != null && item.getDisplayName() != null) {
            e.setValue(set.contains(item.getDisplayName().toLowerCase()));
         }
      }
   }

   public Map<String, Map<String, Float>> getDurabilitySettingsByServer() { return new HashMap<>(durabilitySettingsByServer); }

   public void setDurabilitySettingsByServer(Map<String, Map<String, Float>> map) {
      durabilitySettingsByServer.clear();
      if (map != null) durabilitySettingsByServer.putAll(map);
   }

   public void setDurabilitySetting(String server, String itemName, float value) {
      Map<String, Float> map = durabilitySettingsByServer.computeIfAbsent(server, k -> new HashMap<>());
      map.put(itemName.toLowerCase(), value);
   }

   public void setCustomPrice(AutoBuyableItem item, int price, boolean sync) {
      if (item == null) return;
      String name = item.getDisplayName();
      if (name != null) customPrices.entrySet().removeIf(e -> {
         AutoBuyableItem k = e.getKey();
         return k != null && k.getDisplayName() != null && k.getDisplayName().equals(name);
      });
      if (price > 0) {
         customPrices.put(item, price);
         item.getSettings().setBuyBelow(price);
      } else {
         customPrices.remove(item);
         item.getSettings().setBuyBelow(0);
      }
      if (sync && autoBuy != null && autoBuy.isBuyerMode()) {
         try {
            autoBuy.getNetworkManager().sendToAllClients("price_sync:" + item.getDisplayName() + "|" + price);
         } catch (Exception ignored) {}
      }
   }

   public void applySyncedPrice(String name, int price) {
      if (name == null || name.isEmpty()) return;
      for (AutoBuyableItem item : ItemRegistry.getAllItems()) {
         if (item != null && item.getDisplayName() != null && item.getDisplayName().equalsIgnoreCase(name)) {
            customPrices.entrySet().removeIf(e -> {
               AutoBuyableItem k = e.getKey();
               return k != null && k.getDisplayName() != null && k.getDisplayName().equalsIgnoreCase(name);
            });
            setCustomPrice(item, price, false);
            break;
         }
      }
   }

   public boolean checkDurability(ItemStack stack, AutoBuyableItem item) {
      if (!stack.isDamageable()) return true;
      float dur = getDurabilitySetting(item);
      if (dur <= 0) return true;
      int maxDmg = stack.getMaxDamage();
      if (maxDmg <= 0) return true;
      int dmg = stack.getDamage();
      int remaining = maxDmg - dmg;
      float pct = (float) remaining / maxDmg * 100.0F;
      return pct >= dur;
   }

   public float getDurabilitySetting(AutoBuyableItem item) {
      Float f = durabilitySettings.get(item);
      if (f != null && f > 0) return f;
      String mode = getCurrentServerMode().name();
      String name = item.getDisplayName();
      if (name != null) {
         Map<String, Float> map = durabilitySettingsByServer.get(mode);
         if (map != null) {
            Float v = map.get(name.toLowerCase());
            if (v != null && v > 0) return v;
         }
      }
      return -1;
   }

   private String normalizeDisplayName(String s) {
      if (s == null) return null;
      return s.replace("ᴀ", "a").replace("ʙ", "b").replace("ᴄ", "c")
         .replace("ᴅ", "d").replace("ᴇ", "e").replace("ꜰ", "f")
         .replace("ɢ", "g").replace("ʜ", "h").replace("ɪ", "i")
         .replace("ᴊ", "j").replace("ᴋ", "k").replace("ʟ", "l")
         .replace("ᴍ", "m").replace("ɴ", "n").replace("ᴏ", "o")
         .replace("ᴘ", "p").replace("ʀ", "r").replace("ꜱ", "s")
         .replace("ᴛ", "t").replace("ᴜ", "u").replace("ᴠ", "v")
         .replace("ᴡ", "w").replace("ʏ", "y").replace("ᴢ", "z")
         .replace("ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ", "armortality")
         .replace("ɪɴꜰɪɴɪᴛʏ", "infinity")
         .replace("ᴇɴᴅʟᴇꜱꜱ", "endless")
         .replace("ꜰᴏʀᴛᴜɴᴇ", "fortune")
         .replace("ᴇꜰꜰɪᴄɪᴇɴᴄʏ", "efficiency");
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (context == null || mc.getWindow() == null) return;
      MatrixStack matrices = context.getMatrices();
      if (animProgress < 0.01F) {
         context.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), applyOpacity(0xFF000000, (int)(100 * animProgress)));
      }
      animProgress = Math.min(1.0F, animProgress + 0.05F);
      context.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), applyOpacity(0xFF000000, (int)(100 * animProgress)));

      matrices.push();
      float cx = mc.getWindow().getScaledWidth() / 2.0F;
      float cy = mc.getWindow().getScaledHeight() / 2.0F;
      matrices.translate(cx, cy, 0);
      matrices.scale(animProgress, animProgress, 1.0F);
      matrices.translate(-cx, -cy, 0);

      x = (mc.getWindow().getScaledWidth() - panelWidth) / 2.0F;
      y = (mc.getWindow().getScaledHeight() - panelHeight) / 2.0F;

      renderMainPanel(context, matrices, mouseX, mouseY, delta);
      renderSettingsPanel(context, matrices, mouseX, mouseY, delta);

      matrices.pop();
   }

   private void renderMainPanel(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float delta) {
      int bgColor = 0xFF1a1a2e;
      drawRoundedRect(context, x, y, panelWidth, panelHeight, 6, bgColor);
      renderCategory(context, matrices, mouseX, mouseY);
      renderItemsList(context, matrices, mouseX, mouseY, delta);
      renderPriceInput(context, matrices, mouseX, mouseY);
      renderSearchButton(context, matrices, mouseX, mouseY);
      renderScrollbar(context, matrices);
   }

   private void renderCategory(DrawContext context, MatrixStack matrices, int mouseX, int mouseY) {
      float catW = panelWidth - 10;
      float catX = x + 5;
      float catY = y + 4;
      matrices.push();
      matrices.translate(0, 0, 200);
      drawRoundedRect(context, catX, catY, catW, 22, 4, 0xFF4a4a6a);
      TextRenderer font = textRenderer;
      context.drawText(font, "AutoBuy [" + getCurrentServerMode().getDisplayName() + "]", (int)(x + 23), (int)(y + 12.5F), 0xFFFFFFFF, true);
      matrices.pop();
   }

   private void renderItemsList(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float delta) {
      float listY = y + 31;
      float listH;
      if (selectedItem != null) {
         listH = (y + panelHeight - 8 - 20) - 8 - 14 + 2 + 4.5F - 8 - listY;
      } else {
         listH = panelHeight - 85;
      }

      float itemSize = 21;
      float gap = 6;
      float contentH = calculateContentHeight();
      float maxScroll = Math.max(0, contentH - gap - listH);
      scroll = MathHelper.clamp(scroll, -maxScroll, 0);
      smoothedScroll += (scroll - smoothedScroll) * 0.15F;

      List<AutoBuyableItem> items = getItemsForCurrentProfile();
      if (items == null || items.isEmpty()) return;

      int cols = 10;
      float startX = x + 8;
      float drawX = startX;
      float drawY = listY + smoothedScroll;

      for (int i = 0; i < items.size(); i++) {
         AutoBuyableItem item = items.get(i);
         if (item == null) continue;
         boolean inView = drawY + itemSize >= listY && drawY <= listY + listH;
         if (inView) {
            renderItemCard(context, matrices, item, i, drawX, drawY, itemSize, mouseX, mouseY);
         }
         drawX += itemSize + gap;
         if ((i + 1) % cols == 0) {
            drawX = startX;
            drawY += itemSize + gap;
         }
      }
   }

   private void renderItemCard(DrawContext context, MatrixStack matrices, AutoBuyableItem item, int index, float rx, float ry, float size, int mouseX, int mouseY) {
      boolean hovered = mouseX >= rx && mouseX <= rx + size && mouseY >= ry && mouseY <= ry + size;
      boolean isSelected = this.selectedItem == item;
      boolean hasCustomPrice = customPrices.containsKey(item);
      int customPriceVal = hasCustomPrice ? customPrices.get(item) : 0;
      String displayName = item.getDisplayName();
      boolean foundInCustom = false;
      int foundPrice = 0;
      if (displayName != null) {
         for (Entry<AutoBuyableItem, Integer> e : customPrices.entrySet()) {
            if (e.getKey() != null && displayName.equals(e.getKey().getDisplayName())) {
               foundInCustom = true;
               foundPrice = e.getValue();
               break;
            }
         }
      }

      boolean hasPrice = (hasCustomPrice && customPriceVal > 0) || (foundInCustom && foundPrice > 0);
      int price = hasCustomPrice ? customPriceVal : (foundInCustom ? foundPrice : item.getPrice());
      boolean priceValid = price > 0;
      boolean parserOn = isParserEnabled(item);

      int borderColor = 0;
      float borderThickness = 0;
      if (parserOn) {
         borderColor = 0xFF57575a;
         borderThickness = 2;
      }

      int cardBg;
      if (hasPrice && priceValid) {
         cardBg = 0xFF4a6a8a;
      } else if (hovered || isSelected) {
         cardBg = 0xFF3a3a4e;
      } else {
         cardBg = 0xFF2a2a3e;
      }

      float bt = borderThickness + 0.01F;
      if (borderThickness > 0) {
         drawRoundedRect(context, rx - bt, ry - bt, size + bt * 2, size + bt * 2, 6, borderColor);
      }
      drawRoundedRect(context, rx, ry, size, size, 4, cardBg);

      try {
         ItemStack stack = item.createItemStack();
         if (stack != null) {
            matrices.push();
            matrices.translate(0, 0, -100);
            context.drawItemWithoutEntity(stack, (int)(rx + (size - 16) / 2), (int)(ry + (size - 16) / 2));
            matrices.pop();
         }
      } catch (Exception ignored) {}
   }

   private void renderPriceInput(DrawContext context, MatrixStack matrices, int mouseX, int mouseY) {
      if (selectedItem == null) return;
      float inputH = 20;
      float inputW = panelWidth - 8 - 70 - 8 - 8;
      float inputY = y + panelHeight - 8 - inputH;
      float inputX = x + 8;
      float btnX = inputX + inputW + 8;

      // Selected item info above input
      String itemName = selectedItem.getDisplayName();
      String normName = normalizeDisplayName(itemName);
      if (normName == null) normName = "Unknown";
      float nameY = inputY - 8 - 14 + 2;
      ItemStack stack = selectedItem.createItemStack();
      if (stack != null) {
         matrices.push();
         matrices.translate(x + 8, nameY + 4.5F, 0);
         matrices.scale(0.7F, 0.7F, 1);
         context.drawItemWithoutEntity(stack, 0, 0);
         matrices.pop();
      }
      context.drawText(textRenderer, normName, (int)(x + 8 + 11.2F + 2.5F), (int)(nameY + 9), 0xFFFFFFFF, true);

      // Price input field
      drawRoundedRect(context, inputX, inputY, inputW, inputH, 6, 0xFF2a2a3e);
      drawRoundedBorder(context, inputX, inputY, inputW, inputH, 6, 2, 0xFF2f2f32);

      String priceText;
      if (isEditingPrice) {
         priceText = priceInput.isEmpty() ? "" : formatPriceWithDots(priceInput);
      } else {
         int p = customPrices.getOrDefault(selectedItem, selectedItem.getPrice());
         priceText = p > 0 ? formatPrice(p) : "Введите цену...";
      }
      int textColor = isEditingPrice ? 0xFFFFFFFF : 0xFF89898c;
      context.drawText(textRenderer, priceText, (int)(inputX + 5), (int)(inputY + inputH / 2 - 1.5F), textColor, true);

      // Cursor blink
      if (isEditingPrice && System.currentTimeMillis() % 1000 < 500) {
         String before = priceInput.substring(0, Math.min(cursorPosition, priceInput.length()));
         String beforeFormatted = formatPriceWithDots(before);
         float cursorX = inputX + 5 + textRenderer.getWidth(beforeFormatted);
         context.fill((int)cursorX, (int)(inputY + inputH / 2 - 3.5F), (int)(cursorX + 0.5F), (int)(inputY + inputH / 2 + 3.5F), 0xFFFFFFFF);
      }
   }

   private void renderSearchButton(DrawContext context, MatrixStack matrices, int mouseX, int mouseY) {
      if (selectedItem == null) return;
      float btnW = 70;
      float btnH = 20;
      float inputW = panelWidth - 8 - btnW - 8 - 8;
      float btnY = y + panelHeight - 8 - btnH;
      float btnX = x + 8 + inputW + 8;

      boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
      int btnColor = hovered ? 0xFF6a6a8a : 0xFF4a4a6a;
      drawRoundedRect(context, btnX, btnY, btnW, btnH, 4, btnColor);
      context.drawText(textRenderer, "Найти на /ah", (int)(btnX + (btnW - textRenderer.getWidth("Найти на /ah")) / 2), (int)(btnY + (btnH - 7) / 2), 0xFFFFFFFF, true);
   }

   private float calculateContentHeight() {
      List<AutoBuyableItem> items = getItemsForCurrentProfile();
      if (items == null || items.isEmpty()) return 0;
      float itemSize = 21;
      float gap = 6;
      int cols = 10;
      int rows = (int) Math.ceil((double) items.size() / cols);
      return rows * (itemSize + gap);
   }

   private void renderScrollbar(DrawContext context, MatrixStack matrices) {
      List<AutoBuyableItem> items = getItemsForCurrentProfile();
      if (items == null || items.isEmpty()) return;
      float listY = y + 31;
      float listH;
      if (selectedItem != null) {
         listH = (y + panelHeight - 8 - 20) - 8 - 14 + 2 + 4.5F - 8 - listY;
      } else {
         listH = panelHeight - 85;
      }

      float barX = x + panelWidth - 8 - 2;
      float contentH = calculateContentHeight();
      if (contentH <= listH) return;

      context.fill((int)(barX + 2.5F), (int)listY, (int)(barX + 4.5F), (int)(listY + listH), 0xFF1b1b1e);
      float maxScroll = contentH - listH;
      float scrollRatio = Math.abs(smoothedScroll) / maxScroll;
      float barH = Math.max(15, listH / contentH * listH);
      float barY = listY + scrollRatio * (listH - barH);
      context.fill((int)(barX + 2.5F), (int)barY, (int)(barX + 4.5F), (int)(barY + barH), 0xFF6a6a8a);
   }

   private void renderSettingsPanel(DrawContext context, MatrixStack matrices, int mouseX, int mouseY, float delta) {
      float progress = Math.min(1, Math.max(0, settingsAnimProgress));
      if (settingsItem == null && progress <= 0) return;

      AutoBuyableItem item = settingsItem != null ? settingsItem : settingsAnimatedItem;
      if (item == null) return;

      float spW = panelWidth / 2.2F;
      ItemStack stack = item.createItemStack();
      boolean hasDurability = stack != null && stack.isDamageable();
      float spH = hasDurability ? panelHeight / 3.8F : panelHeight / 5.5F;
      float gap = 10;
      float spX = x + panelWidth + gap;
      float spY = y;

      matrices.push();
      float spCX = spX + spW / 2;
      float spCY = spY + spH / 2;
      matrices.translate(spCX, spCY, 0);
      matrices.scale(progress, progress, 1);
      matrices.translate(-spCX, -spCY, 0);

      drawRoundedRect(context, spX, spY, spW, spH, 6, 0xFF1a1a2e);

      // Header
      int alpha = (int)(255 * progress);
      drawRoundedRect(context, spX + 5, spY + 4, spW - 10, 22, 4, applyOpacity(0xFF4a4a6a, alpha));
      context.drawText(textRenderer, "Настройки", (int)(spX + 10), (int)(spY + 12), applyOpacity(0xFFFFFFFF, alpha), true);

      // Parser checkbox
      float checkY = spY + 4 + 22 + 8;
      boolean parserOn = isParserEnabled(item);
      context.drawText(textRenderer, "Парсить цену", (int)(spX + 10), (int)(checkY + 5), applyOpacity(0xFFFFFFFF, alpha), true);

      float checkBoxX = spX + spW - 10 - 12.5F;
      float checkBoxY = checkY + 3;
      int checkColor = parserOn ? 0xFF6a8aff : 0xFF3a3a4e;
      drawRoundedRect(context, checkBoxX, checkBoxY, 12.5F, 7, 3, checkColor);
      if (parserOn) {
         context.fill((int)(checkBoxX + 5.5F), (int)(checkBoxY + 1), (int)(checkBoxX + 11), (int)(checkBoxY + 6), 0xFFFFFFFF);
      }

      // Durability slider
      if (hasDurability && stack != null) {
         float sliderY = checkY + 15 + 10;
         Float durVal = durabilitySettings.get(item);
         if (durVal == null || durVal < 0) durVal = getDurabilitySetting(item);
         if (durVal < 0) durVal = 0F;
         context.drawText(textRenderer, "Мин. прочность: " + (int)(float)durVal + "%", (int)(spX + 10), (int)(sliderY + 5), applyOpacity(0xFFFFFFFF, alpha), true);

         float sliderX = spX + 10;
         float sliderW = spW - 20;
         float sliderTrackY = sliderY + 15;
         context.fill((int)sliderX, (int)sliderTrackY, (int)(sliderX + sliderW), (int)(sliderTrackY + 4), 0xFF3a3a4e);
         float fillW = sliderW * (durVal / 100F);
         context.fill((int)sliderX, (int)sliderTrackY, (int)(sliderX + fillW), (int)(sliderTrackY + 4), 0xFF6a8aff);
         // Drag handle
         float handleX = sliderX + fillW - 2;
         context.fill((int)handleX, (int)(sliderTrackY - 2), (int)(handleX + 4), (int)(sliderTrackY + 6), 0xFFFFFFFF);
      }

      matrices.pop();

      if (settingsAnimDir && progress >= 1) settingsAnimDir = false;
      if (!settingsAnimDir && settingsItem == null && progress > 0) settingsAnimProgress = Math.max(0, progress - 0.05F);
      else if (settingsAnimDir) settingsAnimProgress = Math.min(1, progress + 0.05F);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         float inputH = 20;
         float inputW = panelWidth - 8 - 70 - 8 - 8;
         float btnW = 70;
         float inputY = y + panelHeight - 8 - inputH;
         float inputX = x + 8;
         float btnX = inputX + inputW + 8;

         // Search button click
         if (selectedItem != null && isHovered(mouseX, mouseY, btnX, inputY, btnW, inputH)) {
            String searchName = selectedItem.getSearchName();
            String query = searchName.replace("[★] ", "").replace("[★]", "").trim();
            query = convertLevelFormat(query);
            String cmd = "/ah search " + query;
            if (mc.player != null && mc.player.networkHandler != null) {
               CommandSender.sendCommand(mc.player, cmd);
            }
            return true;
         }

         // Price input click
         if (selectedItem != null && isHovered(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
            isEditingPrice = true;
            isTextSelected = false;
            int p = customPrices.getOrDefault(selectedItem, selectedItem.getPrice());
            priceInput = p > 0 ? String.valueOf(p) : "";
            cursorPosition = priceInput.length();
            return true;
         }

         // Item grid click
         float listY = y + 31;
         List<AutoBuyableItem> items = getItemsForCurrentProfile();
         float itemSize = 21;
         float gap = 6;
         int cols = 10;
         float startX = x + 8;
         float drawX = startX;
         float drawY = listY + smoothedScroll;

         for (int i = 0; i < items.size(); i++) {
            AutoBuyableItem item = items.get(i);
            if (item == null) continue;
            if (isHovered(mouseX, mouseY, drawX, drawY, itemSize, itemSize)) {
               if (settingsItem == item) {
                  settingsAnimatedItem = item;
                  settingsItem = null;
                  settingsAnimDir = false;
                  settingsAnimProgress = 1.0F;
               } else {
                  settingsAnimatedItem = item;
                  settingsItem = item;
                  selectedItem = item;
                  isEditingPrice = false;
                  settingsAnimDir = true;
                  settingsAnimProgress = 0;
               }
               return true;
            }
            drawX += itemSize + gap;
            if ((i + 1) % cols == 0) {
               drawX = startX;
               drawY += itemSize + gap;
            }
         }
      }

      if (button == 1) {
         // Right click - toggle parser
         if (settingsItem != null) {
            float spW = panelWidth / 2.2F;
            ItemStack stack = settingsItem.createItemStack();
            boolean hasDurability = stack != null && stack.isDamageable();
            float spH = hasDurability ? panelHeight / 3.8F : panelHeight / 5.5F;
            float spX = x + panelWidth + 10;
            float spY = y;
            if (isHovered(mouseX, mouseY, spX, spY, spW, spH)) {
               return super.mouseClicked(mouseX, mouseY, button);
            }
         }

         float listY = y + 31;
         List<AutoBuyableItem> items = getItemsForCurrentProfile();
         float itemSize = 21;
         float gap = 6;
         int cols = 10;
         float startX = x + 8;
         float drawX = startX;
         float drawY = listY + smoothedScroll;
         for (int i = 0; i < items.size(); i++) {
            AutoBuyableItem item = items.get(i);
            if (item == null) continue;
            if (isHovered(mouseX, mouseY, drawX, drawY, itemSize, itemSize)) {
               toggleParserEnabled(item);
               return true;
            }
            drawX += itemSize + gap;
            if ((i + 1) % cols == 0) {
               drawX = startX;
               drawY += itemSize + gap;
            }
         }
      }

      // Settings panel interactions
      if (settingsItem != null && button == 0) {
         float spW = panelWidth / 2.2F;
         ItemStack stack = settingsItem.createItemStack();
         boolean hasDurability = stack != null && stack.isDamageable();
         float spH = hasDurability ? panelHeight / 3.8F : panelHeight / 5.5F;
         float spX = x + panelWidth + 10;
         float spY = y;
         if (isHovered(mouseX, mouseY, spX, spY, spW, spH)) {
            // Parser checkbox click
            float checkY = spY + 4 + 22 + 8;
            float checkBoxX = spX + spW - 10 - 12.5F;
            float checkBoxY = checkY + 3;
            if (isHovered(mouseX, mouseY, spX + 9, checkY, checkBoxX + 12.5F - (spX + 9), 15)) {
               boolean was = isParserEnabled(settingsItem);
               parserSettings.put(settingsItem, !was);
               String name = settingsItem.getDisplayName();
               if (name != null) {
                  String mode = getCurrentServerMode().name();
                  Set<String> set = parserEnabledItemsByServer.computeIfAbsent(mode, k -> new HashSet<>());
                  if (!was) set.add(name.toLowerCase());
                  else set.remove(name.toLowerCase());
               }
               return true;
            }

            // Durability slider click
            if (hasDurability && stack != null) {
               float sliderY = checkY + 15 + 10;
               float sliderTrackY = sliderY + 15;
               float sliderX = spX + 10;
               float sliderW = spW - 20;
               if (isHovered(mouseX, mouseY, sliderX, sliderTrackY, sliderW, 4)) {
                  sliderDragging.put(settingsItem, true);
                  updateSliderValue(settingsItem, (int)mouseX, sliderX, sliderW);
                  return true;
               }
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (settingsItem != null && button == 0 && sliderDragging.getOrDefault(settingsItem, false)) {
         ItemStack stack = settingsItem.createItemStack();
         if (stack != null && stack.isDamageable()) {
            float spX = x + panelWidth + 10;
            float sliderX = spX + 10;
            float sliderW = panelWidth / 2.2F - 20;
            updateSliderValue(settingsItem, (int)mouseX, sliderX, sliderW);
            return true;
         }
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (settingsItem != null && button == 0) {
         sliderDragging.put(settingsItem, false);
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void updateSliderValue(AutoBuyableItem item, int mouseX, float sliderX, float sliderW) {
      float relX = MathHelper.clamp(mouseX - sliderX + 0.5F, 0, sliderW);
      float val = relX / sliderW * 100;
      val = Math.round(val);
      val = MathHelper.clamp(val, 0, 100);
      durabilitySettings.put(item, val);
      String mode = getCurrentServerMode().name();
      String name = item.getDisplayName();
      if (name != null) setDurabilitySetting(mode, name, val);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (isHovered(mouseX, mouseY, x + 1.5F, y + 1, panelWidth - 3, panelHeight - 23)) {
         scroll = (float)(scroll + verticalAmount * 20);
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean charTyped(char chr, int mods) {
      if (isEditingPrice && Character.isDigit(chr)) {
         if (isTextSelected) {
            priceInput = String.valueOf(chr);
            cursorPosition = 1;
            isTextSelected = false;
         } else {
            priceInput = priceInput.substring(0, cursorPosition) + chr + priceInput.substring(cursorPosition);
            cursorPosition++;
         }
         return true;
      }
      return super.charTyped(chr, mods);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (isEditingPrice) {
         if (Screen.hasControlDown()) {
            switch (keyCode) {
               case 65: // Ctrl+A
                  if (!priceInput.isEmpty()) {
                     isTextSelected = true;
                     GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), priceInput);
                  }
                  return true;
               case 67: // Ctrl+C
                  if (!priceInput.isEmpty()) {
                     GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), priceInput);
                  }
                  return true;
               case 86: // Ctrl+V
                  String clip = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                  if (clip != null) {
                     StringBuilder digits = new StringBuilder();
                     for (char c : clip.toCharArray()) {
                        if (Character.isDigit(c)) digits.append(c);
                     }
                     if (digits.length() > 0) {
                        if (isTextSelected) {
                           priceInput = digits.toString();
                           cursorPosition = priceInput.length();
                        } else {
                           priceInput = priceInput.substring(0, cursorPosition) + digits + priceInput.substring(cursorPosition);
                           cursorPosition += digits.length();
                        }
                     }
                  }
                  isTextSelected = false;
                  return true;
            }
         }

         if (keyCode == 259) { // Backspace
            if (isTextSelected) {
               priceInput = "";
               cursorPosition = 0;
               isTextSelected = false;
            } else if (!priceInput.isEmpty() && cursorPosition > 0) {
               priceInput = priceInput.substring(0, cursorPosition - 1) + priceInput.substring(cursorPosition);
               cursorPosition--;
            }
            return true;
         }

         if (keyCode == 261) { // Delete
            if (!priceInput.isEmpty() && cursorPosition < priceInput.length()) {
               priceInput = priceInput.substring(0, cursorPosition) + priceInput.substring(cursorPosition + 1);
            }
            return true;
         }

         if (keyCode == 263) { // Left
            if (cursorPosition > 0) cursorPosition--;
            isTextSelected = false;
            return true;
         }

         if (keyCode == 262) { // Right
            if (cursorPosition < priceInput.length()) cursorPosition++;
            isTextSelected = false;
            return true;
         }

         if (keyCode == 265) { // Up
            if (selectedItem != null) {
               try {
                  int val = priceInput.isEmpty() ? 0 : Integer.parseInt(priceInput);
                  val = Math.max(0, val + 1);
                  priceInput = String.valueOf(val);
                  cursorPosition = priceInput.length();
               } catch (NumberFormatException ignored) {
                  priceInput = "1";
                  cursorPosition = 1;
               }
            }
            return true;
         }

         if (keyCode == 264) { // Down
            if (selectedItem != null) {
               try {
                  int val = priceInput.isEmpty() ? 0 : Integer.parseInt(priceInput);
                  val = Math.max(0, val - 1);
                  priceInput = val == 0 ? "" : String.valueOf(val);
                  cursorPosition = priceInput.length();
               } catch (NumberFormatException ignored) {
                  priceInput = "";
                  cursorPosition = 0;
               }
            }
            return true;
         }

         if (keyCode == 257 && selectedItem != null) { // Enter
            try {
               if (!priceInput.isEmpty()) {
                  int val = Integer.parseInt(priceInput);
                  setCustomPrice(selectedItem, val, true);
               } else {
                  setCustomPrice(selectedItem, 0, true);
               }
            } catch (NumberFormatException ignored) {}
            isEditingPrice = false;
            priceInput = "";
            cursorPosition = 0;
            return true;
         }

         if (keyCode == 256) { // Esc
            isEditingPrice = false;
            priceInput = "";
            cursorPosition = 0;
            return true;
         }
      }

      if (keyCode == 256) {
         close();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean shouldPause() { return false; }

   private String formatPrice(int price) {
      StringBuilder sb = new StringBuilder();
      String s = String.valueOf(price);
      int count = 0;
      for (int i = s.length() - 1; i >= 0; i--) {
         if (count > 0 && count % 3 == 0) sb.insert(0, '.');
         sb.insert(0, s.charAt(i));
         count++;
      }
      return sb.toString();
   }

   private String formatPriceWithDots(String input) {
      if (input == null || input.isEmpty()) return "";
      StringBuilder sb = new StringBuilder();
      int count = 0;
      for (int i = input.length() - 1; i >= 0; i--) {
         char c = input.charAt(i);
         if (Character.isDigit(c)) {
            if (count > 0 && count % 3 == 0) sb.insert(0, '.');
            sb.insert(0, c);
            count++;
         }
      }
      return sb.toString();
   }

   private String convertLevelFormat(String input) {
      if (input == null || input.isEmpty()) return input;
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("(.+?)\\s*\\[(\\d+)\\s*[уУ]р\\.?\\]");
      java.util.regex.Matcher m = p.matcher(input);
      if (m.find()) {
         String name = m.group(1).trim();
         String lvl = m.group(2);
         name = name.replace(" опыта", "").replace(" Опыта", "").trim();
         return name + " с уровнем " + lvl;
      }
      return input;
   }

   private boolean isHovered(double mx, double my, float rx, float ry, float rw, float rh) {
      return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
   }

   private int applyOpacity(int color, int alpha) {
      alpha = MathHelper.clamp(alpha, 0, 255);
      return (color & 0x00FFFFFF) | (alpha << 24);
   }

   private void drawRoundedRect(DrawContext context, float x, float y, float w, float h, int radius, int color) {
      float r = Math.min(radius, Math.min(w / 2, h / 2));
      context.fill((int)(x + r), (int)y, (int)(x + w - r), (int)(y + h), color);
      context.fill((int)x, (int)(y + r), (int)(x + w), (int)(y + h - r), color);
      if (r > 0) {
         context.fill((int)(x + r), (int)(y + r), (int)(x + w - r), (int)(y + h - r), color);
      }
   }

   private void drawRoundedBorder(DrawContext context, float x, float y, float w, float h, int radius, int thickness, int color) {
      float r = Math.min(radius, Math.min(w / 2, h / 2));
      for (int t = 0; t < thickness; t++) {
         float bx = x - t;
         float by = y - t;
         float bw = w + t * 2;
         float bh = h + t * 2;
         context.fill((int)(bx + r), (int)by, (int)(bx + bw - r), (int)(by + bh), color);
         context.fill((int)bx, (int)(by + r), (int)(bx + bw), (int)(by + bh - r), color);
      }
   }
}
