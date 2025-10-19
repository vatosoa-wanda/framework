package sprint2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// L’annotation s’applique aux méthodes
@Target(ElementType.METHOD)
// Elle est accessible à l’exécution
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleUrl {
    String value(); // paramètre pour l’URL
}
