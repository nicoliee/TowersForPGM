package org.nicolie.towersforpgm.translations;

import java.text.MessageFormat;
import java.util.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;

public final class PluginTranslator implements Translator {
  private static final PluginTranslator INSTANCE = new PluginTranslator();

  private final Key name = Key.key("towerspgm", "translator");

  private final Map<Locale, Map<String, String>> translations = new HashMap<>();
  private final Map<String, String> templates = new HashMap<>();

  @Override
  public Key name() {
    return name;
  }

  @Override
  public MessageFormat translate(String key, Locale locale) {

    Map<String, String> map = translations.get(locale);

    if (map != null && map.containsKey(key)) {
      return new MessageFormat(map.get(key), locale);
    }

    for (Map.Entry<Locale, Map<String, String>> entry : translations.entrySet()) {
      if (entry.getKey().getLanguage().equals(locale.getLanguage())) {
        map = entry.getValue();
        if (map != null && map.containsKey(key)) {
          return new MessageFormat(map.get(key), locale);
        }
      }
    }

    String template = templates.get(key);

    if (template != null) {
      return new MessageFormat(template, locale);
    }

    return null;
  }

  public void addTemplate(String key, String value) {
    templates.put(key, value);
  }

  public void addTranslation(Locale locale, String key, String value) {

    translations.computeIfAbsent(locale, l -> new HashMap<>()).put(key, value);
  }

  public static PluginTranslator getInstance() {
    return INSTANCE;
  }

  public String translateToString(String key, Locale locale, Object... args) {
    MessageFormat format = translate(key, locale);
    return format != null ? format.format(args) : key; // fallback al key si no existe
  }
}
