package org.nicolie.towersforpgm.translations;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import net.kyori.adventure.translation.GlobalTranslator;

public final class TranslationLoader {

  private static final Pattern FILE_PATTERN =
      Pattern.compile("(.+)_([a-z]{2})_([A-Z]{2})\\.properties");

  public static void register() {

    PluginTranslator translator = new PluginTranslator();

    List<String> files;
    try {
      files = ResourceScanner.list("i18n");
    } catch (Exception e) {
      System.err.println("Failed to scan i18n resources");
      e.printStackTrace();
      return;
    }

    for (String file : files) {

      if (!file.endsWith(".properties")) continue;

      Properties props = load(file);

      if (props == null) continue;

      if (file.startsWith("i18n/templates/")) {

        for (String key : props.stringPropertyNames()) {

          translator.addTemplate(key, props.getProperty(key));
        }

        continue;
      }

      if (file.startsWith("i18n/translations/")) {
        String name = file.substring(file.lastIndexOf("/") + 1);
        Matcher m = FILE_PATTERN.matcher(name);
        if (!m.matches()) continue;
        Locale locale = Locale.forLanguageTag(m.group(2) + "-" + m.group(3));
        for (String key : props.stringPropertyNames()) {

          translator.addTranslation(locale, key, props.getProperty(key));
        }
      }
    }
    GlobalTranslator.translator().addSource(translator);
  }

  private static Properties load(String path) {
    Properties props = new Properties();
    try (InputStream in = TranslationLoader.class.getClassLoader().getResourceAsStream(path)) {

      props.load(new InputStreamReader(in, StandardCharsets.UTF_8));

    } catch (Exception e) {
      e.printStackTrace();
    }
    return props;
  }
}
