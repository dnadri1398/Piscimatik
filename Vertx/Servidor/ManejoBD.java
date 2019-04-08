package proyecto.Piscina;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import proyecto.Clases.Sensores;
import proyecto.Clases.SensClima;

public class ManejoBD extends AbstractVerticle{

	private AsyncSQLClient mySQLClient;
	
	public void start(Future<Void> startFuture) {
		
		JsonObject config = new JsonObject()
				.put("host", "localhost")
				.put("username", "root")
				.put("password", "root")
				.put("database", "poolmatik")
				.put("port", 3306);
		mySQLClient = 
				MySQLClient.createShared(vertx, config);
		
		Router router = Router.router(vertx);
		vertx.createHttpServer().requestHandler(router).
			listen(8090, result -> {
				if (result.succeeded()) {
					System.out.println("Servidor database desplegado");
				}else {
					System.out.println("Error de despliegue");
				}
			});
		router.route().handler(BodyHandler.create());
		router.get("/sensores/sensclima").handler(this::handleClima);
		router.get("/sensores/:sensor").handler(this::handleAllSensors);
		router.get("/sensores/:sensor/:id").handler(this::handleSensor);
		
		
		
	}
	
	
	
	private void handleSensor(RoutingContext routing) {
		String sensor = routing.pathParam("sensor");
		String id = routing.pathParam("id");
		String consulta = "SELECT * FROM " + sensor + " WHERE id = " + id; 
		
		mySQLClient.getConnection(connection -> {
			if(connection.succeeded()) {
				connection.result().query(consulta, result -> {
					if(result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String sensores = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<Sensores>>(){}.getType();
						List<Sensores> sensoresph = gsonResult.fromJson(sensores, tipoLista);
												
						routing.response().end(sensoresph.toString());
					}
					else {
						System.out.println(result.cause().getMessage());
						routing.response().setStatusCode(400).end();
					}
				});
			}
			
			
		});
		
	}
	
	
	private void handleClima(RoutingContext routingC) {
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query("SELECT * FROM sensclima" , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String sensores = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<SensClima>>(){}.getType();
						List<SensClima> sensoresph = gsonResult.fromJson(sensores, tipoLista);
												
						routingC.response().end(sensoresph.toString());
					}else {
						System.out.println(result.cause().getMessage());
						routingC.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}else {
				connection.result().close();
				System.out.println(connection.cause().getMessage());
				routingC.response().setStatusCode(400).end();
			}
		});
	}
	
	private void handleAllSensors(RoutingContext routingConext) {
		String paramStr = "SELECT * FROM " + routingConext.pathParam("sensor");
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query(paramStr , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String sensores = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<Sensores>>(){}.getType();
						List<Sensores> sensoresph = gsonResult.fromJson(sensores, tipoLista);
												
						routingConext.response().end(sensoresph.toString());
					}else {
						System.out.println(result.cause().getMessage());
						routingConext.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}else {
				connection.result().close();
				System.out.println(connection.cause().getMessage());
				routingConext.response().setStatusCode(400).end();
			}
		});
	}
	
}
