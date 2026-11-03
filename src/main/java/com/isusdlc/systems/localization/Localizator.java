package com.isusdlc.systems.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Generated;
import com.isusdlc.elegant;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public final class Localizator {
   private static final Language DEFAULT_LANG = Language.RU_RU;
   private static Language currentLanguage = DEFAULT_LANG;
   private static final Map<String, String> translations = new HashMap<>();

   public static void loadTranslations() {
      String langFile = "/assets/" + elegant.MOD_ID + "/lang/" + currentLanguage.getCode() + ".lang";
      System.out.println("Loading translations from: " + langFile);

      try (InputStream inputStream = Localizator.class.getResourceAsStream(langFile);
         BufferedReader reader = inputStream == null ? null : new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
         if (inputStream == null) {
            throw new RuntimeException("Language file not found: " + langFile);
         } else {
            translations.clear();
            String fileName = currentLanguage.getCode() + ".lang";
            int lineNumber = 0;

            String line;
            while ((line = reader.readLine()) != null) {
               lineNumber++;
               line = removeComments(line).trim();
               if (!line.isEmpty()) {
                  int equalIndex = line.indexOf(61);
                  if (equalIndex == -1) {
                     elegant.LOGGER.warn("Warning: Invalid line format at line {} in {}: {}", new Object[]{lineNumber, fileName, line});
                  } else {
                     String key = line.substring(0, equalIndex).trim();
                     String value = line.substring(equalIndex + 1).trim();
                     if (key.isEmpty()) {
                        elegant.LOGGER.warn("Warning: Empty key at line {} in {}", lineNumber, fileName);
                     } else {
                        translations.put(key, value);
                     }
                  }
               }
            }

            System.out.println("Loaded " + translations.size() + " translations");
         }
      } catch (IOException var9) {
         throw new RuntimeException("Failed to load translations for language: " + currentLanguage.getCode(), var9);
      }
   }

   public static void setLanguage(@Nonnull Language lang) {
      currentLanguage = lang;
      loadTranslations();
   }

   public static String translate(String key, Object... args) {
      String format = translations.getOrDefault(key, key);
      if (args.length == 0) {
         return format;
      } else {
         try {
            return String.format(format, args);
         } catch (IllegalFormatException var4) {
            elegant.LOGGER.warn("Failed to format translation key '{}' with format '{}': {}", new Object[]{key, format, var4.getMessage()});
            return format;
         }
      }
   }

   public static String translateOrEmpty(String key) {
      return translations.getOrDefault(key, " ");
   }

   @VMProtect(
      type = VMProtectType.MUTATION
   )
   private static void parseLine(String line, int lineNumber, String fileName) {
      int equalIndex = line.indexOf(61);
      if (equalIndex == -1) {
         elegant.LOGGER.warn("Warning: Invalid line format at line {} in {}: {}", new Object[]{lineNumber, fileName, line});
      } else {
         String key = line.substring(0, equalIndex).trim();
         String value = line.substring(equalIndex + 1).trim();
         if (key.isEmpty()) {
            elegant.LOGGER.warn("Warning: Empty key at line {} in {}", lineNumber, fileName);
         } else {
            translations.put(key, value);
         }
      }
   }

   private static String removeComments(String line) {
      int commentIndex = line.indexOf("#");
      return commentIndex != -1 ? line.substring(0, commentIndex) : line;
   }

   @Generated
   private Localizator() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static Language getCurrentLanguage() {
      return currentLanguage;
   }
}
