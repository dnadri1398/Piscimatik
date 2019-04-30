package proyecto.Piscina;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Hello world!
 *
 */
public class Principal extends AbstractVerticle
{
	public void start(Future<Void> startFuture) {
		vertx.deployVerticle(new ManejoBD2());
		
	}
}
