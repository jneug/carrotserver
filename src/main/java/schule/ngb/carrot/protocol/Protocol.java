package schule.ngb.carrot.protocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
public @interface Protocol {

	String name() default "";

	int port() default 0;

	String config() default "";

	Class<? extends ProtocolHandlerFactory> factory() default GenericProtocolHandlerFactory.class;

}
