package org.nicolie.towersforpgm.translations;

import java.net.*;
import java.util.*;
import java.util.jar.*;

public class ResourceScanner {

  public static List<String> list(String path) throws Exception {

    List<String> files = new ArrayList<>();

    URL url = ResourceScanner.class.getClassLoader().getResource(path);

    if (url == null) return files;

    if (url.getProtocol().equals("jar")) {

      String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));

      try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {

        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {

          JarEntry entry = entries.nextElement();

          if (entry.getName().startsWith(path)) {

            files.add(entry.getName());
          }
        }
      }
    }

    return files;
  }
}
