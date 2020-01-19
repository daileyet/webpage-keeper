/**
 * 
 */
package com.openthinks.others.webpages.conf;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
/**
 * @author dailey.dai@openthinks.com
 *
 */
public @interface ConfigDesc {
  String value() default "";
}
