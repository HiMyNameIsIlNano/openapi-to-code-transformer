package hi.mynameis.ilnano;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
* For OpenRewrite to give me type-attributed access to @ApiAnnotation, the annotation class should be on the parse classpath when JavaProcessor parses the emitted text.
* Either
Ship MyApiOperation and Type as real classes in your plugin (or a tiny companion artifact) and include them on the JavaParser classpath. This gives you full type attribution — the recipe can match @org.myplugin.annotations.MyApiOperation precisely rather than by simple name.
Or accept name-only matching if you'd rather not ship the class — workable, but less bulletproof against collisions.
I'd ship the real annotation. It's a handful of lines, it makes matching rock-solid, and since it's SOURCE-retained it adds zero runtime weight to generated projects.
* */
@Retention(RetentionPolicy.SOURCE)   // never survives to bytecode
@Target(ElementType.METHOD)
public @interface ApiAnnotation {

    OperationTypeEnum type();

    String path();

    String[] produces() default {"application/json"};

}

