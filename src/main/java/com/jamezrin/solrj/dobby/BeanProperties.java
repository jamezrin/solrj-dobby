package com.jamezrin.solrj.dobby;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for discovering JavaBean getter methods via {@link Introspector}.
 *
 * <p>Replaces manual "get" + capitalize / "is" + capitalize logic with the standard {@code
 * java.beans} API, which correctly handles all JavaBean naming conventions.
 */
public final class BeanProperties {

  private BeanProperties() {}

  /**
   * Returns a map of property-name → read (getter) method for the given class.
   *
   * <p>Uses {@link Introspector#getBeanInfo(Class, Class)} with stop class {@code Object.class} to
   * exclude the {@code getClass()} pseudo-property.
   *
   * @param clazz the class to introspect
   * @return unmodifiable map of property names to their getter methods
   */
  public static Map<String, Method> getterMap(Class<?> clazz) {
    try {
      BeanInfo info = Introspector.getBeanInfo(clazz, Object.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

      Map<String, Method> map = new LinkedHashMap<>(descriptors.length);
      for (PropertyDescriptor pd : descriptors) {
        Method getter = pd.getReadMethod();
        if (getter != null) {
          getter.setAccessible(true);
          map.put(pd.getName(), getter);
        }
      }
      return map;
    } catch (IntrospectionException e) {
      throw new DobbyException("Failed to introspect " + clazz.getName(), e);
    }
  }

  /**
   * Derives the JavaBean property name from a setter method.
   *
   * <p>Uses {@link Introspector#decapitalize} for correct handling of edge cases (e.g., "setURL" →
   * "URL", not "uRL").
   *
   * @param setter the setter method
   * @return the property name
   */
  public static String propertyNameFromSetter(Method setter) {
    String name = setter.getName();
    if (name.startsWith("set") && name.length() > 3) {
      return Introspector.decapitalize(name.substring(3));
    }
    return name;
  }
}
