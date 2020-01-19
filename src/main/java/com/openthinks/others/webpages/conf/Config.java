package com.openthinks.others.webpages.conf;

import java.lang.reflect.Field;
import java.util.Properties;

public class Config extends Properties {
  private static final long serialVersionUID = -4074773711094826397L;

  public static enum ConfigType {
    PROPERTIES, XML;
  }

  public String template(ConfigType type) throws IllegalArgumentException, IllegalAccessException {
    Field[] fields = getClass().getDeclaredFields();
    for (Field field : fields) {
      ConfigDesc desc = field.getAnnotation(ConfigDesc.class);
      if (desc == null)
        continue;
      Object propName = field.get(this);
      String propDesc = desc.value();
      if (propName != null)
        this.setProperty(propName.toString(),
            type == ConfigType.PROPERTIES ? "#" + propDesc : "<!--" + propDesc + "-->");
    }
    return "";
  }
}
