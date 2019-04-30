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
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import proyecto.Clases.Sensores;
import proyecto.Clases.SensClima;
import proyecto.Clases.Sensor;

public class ManejoBD2 extends AbstractVerticle{

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
		
		//cuando se agregen en las rutas el iddepuradora agregas a las búsquedas en BD
		router.get("/:idDep/sensores/all").handler(this::handleAllSensors);
		router.get("/:idDep/sensores/:sensor").handler(this::handleSensors);
		router.get("/sensores/sensclima/:idSen").handler(this::handleClima);
		router.get("/sensores/:nombre/:idSen").handler(this::handleSpecificSens);
		router.get("/sensores/:nombre/:idSen/agregar").handler(this::handleAgrega1);	
		router.get("/sensores/:nombre/:idSen/agregar/:valor").handler(this::handleAgrega2);
		
		//sin revisar
		
		
		
		
		
		
	}
	
	
	private void handleAllSensors(RoutingContext routingConext) {
		String paramStr = "SELECT * FROM sensores WHERE idDepuradora = " + routingConext.pathParam("idDep");
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


	private void handleSensors(RoutingContext routingConext) {
		String paramStr = "SELECT * FROM sensores WHERE nombre = '" + routingConext.pathParam("sensor")
				+ "' and idDepuradora = " + routingConext.pathParam("idDep");
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
	
	private void handleClima(RoutingContext routing) {
		String consulta = "SELECT * FROM sensclima WHERE idSen = " + routing.pathParam("idSen");
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query(consulta , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String sensores = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<SensClima>>(){}.getType();
						List<SensClima> sensoresph = gsonResult.fromJson(sensores, tipoLista);
												
						routing.response().end(sensoresph.toString());
					}else {
						System.out.println(result.cause().getMessage());
						routing.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}else {
				connection.result().close();
				System.out.println(connection.cause().getMessage());
				routing.response().setStatusCode(400).end();
			}
		});
	}


	private void handleSpecificSens(RoutingContext routing) {
		String consulta = "SELECT * FROM " + routing.pathParam("nombre") 
			+ " WHERE idSen = " + routing.pathParam("idSen");
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query(consulta , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String sensores = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<Sensor>>(){}.getType();
						List<Sensor> sensoresph = gsonResult.fromJson(sensores, tipoLista);
												
						routing.response().end(sensoresph.toString());
					}else {
						System.out.println(result.cause().getMessage());
						routing.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}else {
				connection.result().close();
				System.out.println(connection.cause().getMessage());
				routing.response().setStatusCode(400).end();
			}
		});
	}
	private void handleAgrega1(RoutingContext routing) {
		String sensor = routing.pathParam("nombre");
		int idSen = Integer.parseInt(routing.pathParam("idSen"));
		long fecha = obtenerFecha();
		String inserta = "INSERT INTO " + sensor + " (idSen, fecha) VALUES (" + idSen + ", " + fecha + ")";
		mySQLClient.getConnection(connection -> {
			if(connection.succeeded()) {
				connection.result().query(inserta, result->{
					if(result.succeeded()) {
						routing.response().end("Se ha insertado con éxito");
					}
					else {
						System.out.println(result.cause().getMessage());
						routing.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}
		});	
	}


	private void handleAgrega2(RoutingContext routing) {
		String sensor = routing.pathParam("nombre");
		int idSen = Integer.parseInt(routing.pathParam("idSen"));
		int valor = Integer.parseInt(routing.pathParam("valor"));
		long fecha = obtenerFecha();
		String inserta = "INSERT INTO " + sensor + " (idSen, valor, fecha) VALUES (" + idSen 
				+ ", " + valor + ", " +fecha + ")";
		mySQLClient.getConnection(connection -> {
			if(connection.succeeded()) {
				connection.result().query(inserta, result->{
					if(result.succeeded()) {
						routing.response().end("Se ha insertado con éxito");
					}
					else {
						System.out.println(result.cause().getMessage());
						routing.response().setStatusCode(400).end();
					}
					connection.result().close();
				});
			}
		});	
	}
	
	private Long obtenerFecha() {
		return System.currentTimeMillis();
	}
	
	
}
