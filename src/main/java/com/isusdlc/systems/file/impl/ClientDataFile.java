package com.isusdlc.systems.file.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.isusdlc.elegant;
import com.isusdlc.mixin.minecraft.client.IMinecraftClient;
import com.isusdlc.systems.config.ConfigFile;
import com.isusdlc.systems.file.ClientFile;
import com.isusdlc.systems.file.FileManager;
import com.isusdlc.systems.file.api.FileInfo;
import com.isusdlc.systems.modules.constructions.swinganim.SwingManager;
import com.isusdlc.systems.modules.constructions.swinganim.SwingPhase;
import com.isusdlc.systems.modules.constructions.swinganim.presets.SwingPreset;
import com.isusdlc.systems.modules.constructions.swinganim.presets.SwingPresetFile;
import com.isusdlc.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import com.isusdlc.systems.setting.Setting;
import com.isusdlc.systems.theme.Theme;
import com.isusdlc.ui.components.ColorPicker;
import com.isusdlc.ui.hud.HudElement;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

@FileInfo(
   name = "client"
)
public class ClientDataFile extends ClientFile implements IMinecraft {
   private String lastConfigName = null;

   public String getLastConfigName() {
      return this.lastConfigName;
   }

   @Override
   public void write() {
      JsonObject json = new JsonObject();
      json.addProperty("username", mc.getSession().getUsername());
      json.addProperty("theme", elegant.getInstance().getThemeManager().getCurrentTheme().name());
      json.addProperty("swing", elegant.getInstance().getSwingManager().getCurrent());
      json.add("hudElements", this.getHudElementsJsonArray());
      json.add("friends", this.getFriendsJsonArray());
      json.add("colorPickerPresets", this.getColorPickerPresetsJsonArray());
      ConfigFile currentConfig = elegant.getInstance().getConfigManager().getCurrent();
      if (currentConfig != null) {
         json.addProperty("lastConfig", currentConfig.getFileName());
      }

      try (FileWriter writer = new FileWriter(this.file)) {
         writer.write(FileManager.GSON.toJson(json));
      } catch (Exception var81) {
         var81.printStackTrace();
      }
   }

   @Override
   public void read() {
      try (FileReader reader = new FileReader(this.getFile())) {
         JsonObject object = (JsonObject)FileManager.GSON.fromJson(reader, JsonObject.class);
         if (object == null) {
            elegant.LOGGER.warn("Client data file is empty or corrupted, using defaults");
            return;
         }
         if (object.has("username")) {
            String username = object.get("username").getAsString();
            ((IMinecraftClient)mc).setSession(new Session(username, UUID.randomUUID(), "", Optional.empty(), Optional.empty(), AccountType.MOJANG));
         }

         if (object.has("swing")) {
            String swing = object.get("swing").getAsString();
            SwingManager swingManager = elegant.getInstance().getSwingManager();
            SwingPresetManager manager = elegant.getInstance().getSwingPresetManager();
            boolean foundBuiltIn = false;

            for (SwingPreset value : elegant.getInstance().getSwingManager().getPresets()) {
               if (value.getName().equals(swing)) {
                  swingManager.getBezier().start(value.getBezierStart()).end(value.getBezierEnd());
                  swingManager.getBack().enabled(value.isSwingBack());
                  swingManager.getSpeed().setCurrentValue(value.getSpeed());
                  SwingPhase start = swingManager.getStartPhase();
                  start.getAnchorX().setCurrentValue(value.getFrom().getAnchorX());
                  start.getAnchorY().setCurrentValue(value.getFrom().getAnchorY());
                  start.getAnchorZ().setCurrentValue(value.getFrom().getAnchorZ());
                  start.getMoveX().setCurrentValue(value.getFrom().getMoveX());
                  start.getMoveY().setCurrentValue(value.getFrom().getMoveY());
                  start.getMoveZ().setCurrentValue(value.getFrom().getMoveZ());
                  start.getRotateX().setCurrentValue(value.getFrom().getRotateX());
                  start.getRotateY().setCurrentValue(value.getFrom().getRotateY());
                  start.getRotateZ().setCurrentValue(value.getFrom().getRotateZ());
                  SwingPhase end = swingManager.getEndPhase();
                  end.getAnchorX().setCurrentValue(value.getTo().getAnchorX());
                  end.getAnchorY().setCurrentValue(value.getTo().getAnchorY());
                  end.getAnchorZ().setCurrentValue(value.getTo().getAnchorZ());
                  end.getMoveX().setCurrentValue(value.getTo().getMoveX());
                  end.getMoveY().setCurrentValue(value.getTo().getMoveY());
                  end.getMoveZ().setCurrentValue(value.getTo().getMoveZ());
                  end.getRotateX().setCurrentValue(value.getTo().getRotateX());
                  end.getRotateY().setCurrentValue(value.getTo().getRotateY());
                  end.getRotateZ().setCurrentValue(value.getTo().getRotateZ());
                  swingManager.setCurrent(swing);
                  foundBuiltIn = true;
                  break;
               }
            }

            if (!foundBuiltIn) {
               SwingPresetFile customPreset = manager.getPreset(swing);
               if (customPreset != null) {
                  manager.setCurrent(customPreset);
                  swingManager.setCurrent(swing);
                  customPreset.load();
               }
            }
         }

         if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();

            try {
               Theme theme = Theme.valueOf(themeName);
               elegant.getInstance().getThemeManager().setCurrentTheme(theme);
            } catch (IllegalArgumentException var15) {
               elegant.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
            }
         }

         if (object.has("friends")) {
            JsonArray friendsArray = object.getAsJsonArray("friends");
            elegant.getInstance().getFriendManager().clear();

            for (JsonElement friendElement : friendsArray) {
               elegant.getInstance().getFriendManager().add(friendElement.getAsString());
            }
         }

         if (object.has("colorPickerPresets")) {
            this.loadColorPickerPresets(object.getAsJsonArray("colorPickerPresets"));
         }

         if (object.has("hudElements")) {
            for (JsonElement elemObj : object.getAsJsonArray("hudElements")) {
               JsonObject elementObject = elemObj.getAsJsonObject();
               String name = elementObject.get("name").getAsString();
               float x = elementObject.get("x").getAsFloat();
               float y = elementObject.get("y").getAsFloat();
               boolean showing = elementObject.get("showing").getAsBoolean();
               HudElement element = elegant.getInstance().getHud().getElementByName(name);
               if (element != null) {
                  element.setX(x);
                  element.setY(y);
                  element.setShowing(showing);
                  if (elementObject.has("settings")) {
                     JsonObject settingsObject = elementObject.getAsJsonObject("settings");

                     for (Setting setting : element.getSettings()) {
                        if (settingsObject.has(setting.getName())) {
                           setting.load(settingsObject.get(setting.getName()));
                        }
                     }
                  }
               }
            }
         }

         if (object.has("lastConfig")) {
            this.lastConfigName = object.get("lastConfig").getAsString();
            elegant.LOGGER.info("Found lastConfig in client.myth: {}", this.lastConfigName);
         }
      } catch (Exception var17) {
         var17.printStackTrace();
      }
   }

   private JsonArray getHudElementsJsonArray() {
      JsonArray hudElementsArray = new JsonArray();

      for (HudElement element : elegant.getInstance().getHud().getElements()) {
         JsonObject elementObject = new JsonObject();
         elementObject.addProperty("name", element.getName());
         elementObject.addProperty("x", element.getX());
         elementObject.addProperty("y", element.getY());
         elementObject.addProperty("showing", element.isShowing());
         elementObject.add("settings", this.getSettingsJsonObject(element));
         hudElementsArray.add(elementObject);
      }

      return hudElementsArray;
   }

   private JsonObject getSettingsJsonObject(HudElement element) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : element.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   private JsonArray getFriendsJsonArray() {
      JsonArray friendsJsonArray = new JsonArray();

      for (String friendsName : elegant.getInstance().getFriendManager().listFriends()) {
         friendsJsonArray.add(friendsName);
      }

      return friendsJsonArray;
   }

   private JsonArray getColorPickerPresetsJsonArray() {
      JsonArray presetsArray = new JsonArray();

      for (ColorPicker.Preset preset : ColorPicker.COLOR_PRESETS) {
         if (preset.isShowing()) {
            JsonObject presetObject = new JsonObject();
            ColorRGBA color = preset.getColor();
            presetObject.addProperty("red", color.getRed());
            presetObject.addProperty("green", color.getGreen());
            presetObject.addProperty("blue", color.getBlue());
            presetObject.addProperty("alpha", color.getAlpha());
            presetsArray.add(presetObject);
         }
      }

      return presetsArray;
   }

   private void loadColorPickerPresets(JsonArray presetsArray) {
      List<ColorPicker.Preset> loadedPresets = new ArrayList<>();

      for (JsonElement presetElement : presetsArray) {
         JsonObject presetObject = presetElement.getAsJsonObject();
         float red = presetObject.get("red").getAsFloat();
         float green = presetObject.get("green").getAsFloat();
         float blue = presetObject.get("blue").getAsFloat();
         float alpha = presetObject.get("alpha").getAsFloat();
         ColorRGBA color = new ColorRGBA(red, green, blue, alpha);
         loadedPresets.add(new ColorPicker.Preset(color));
      }

      ColorPicker.setColorPresets(loadedPresets);
   }
}
