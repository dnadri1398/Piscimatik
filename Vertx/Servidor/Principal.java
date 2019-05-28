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
		//vertx.deployVerticle(new Servidor());
		vertx.deployVerticle(new ManejoBD());
		vertx.deployVerticle(new Mqtt());
		//vertx.deployVerticle(new ClienteMqtt());
		//2019-04-06 13:34:36.787
		//1554550476787
	}
}
