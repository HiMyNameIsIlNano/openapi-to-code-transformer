package hi.mynameis.ilnano;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface ApiOperationMarker {

    String path() default "";

    String type() default "";

}
