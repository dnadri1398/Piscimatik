package proyecto.Piscina;

import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Hello world!
 *
 */
public class Principal extends AbstractVerticle
{
	public void start(Future<Void> startFuture) {
		vertx.deployVerticle(new ManejoBD());
		vertx.deployVerticle(new Mqtt());
		vertx.deployVerticle(new ClienteMqtt());
	
	}
	
	
}
