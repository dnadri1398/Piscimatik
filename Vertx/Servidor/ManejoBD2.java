package proyecto.Piscina;

import java.lang.reflect.Type;
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
import proyecto.Clases.Sensor;
import proyecto.Clases.luces_estado;

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
			listen(8081, result -> {
				if (result.succeeded()) {
					System.out.println("Servidor database desplegado");
				}else {
					System.out.println("Error de despliegue");
				}
			});
		router.route().handler(BodyHandler.create());
		
		//sin revisar
				
				
		
		//cuando se agregen en las rutas el iddepuradora agregas a las búsquedas en BD
		
		router.put("/sensores/agregar").handler(this::handleAgrega2);
		router.put("/sensores/:nombre/agregar").handler(this::handleAgrega1);
		router.put("/luces/ONOF").handler(this::handleLucesONOF);
		router.put("/dep/ONOF").handler(this::handleDepONOF);
		
		
		router.get("/sensores/sensclima/:idSen").handler(this::handleClima);
		router.get("/sensores/:nombre/:idSen").handler(this::handleSpecificSens);		
		router.get("/:idDep/sensores/all").handler(this::handleAllSensors);
		router.get("/:idDep/sensores/:sensor").handler(this::handleSensors);
		router.get("/:idDep/luces").handler(this::handleLuces);
		router.get("/:idDep/estados").handler(this::handleDepEncendido);
		
		
		
		
		
		
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
		JsonObject body = routing.getBodyAsJson();
		long fecha = obtenerFecha();
		if(body.containsKey("idSen")) {
			int idSen = body.getInteger("idSen");
			String inserta = "INSERT INTO " + sensor + " (idSen, fecha) VALUES (" + idSen + ", " + fecha + ")";
			if(body.containsKey("valor")) {
				int valor = body.getInteger("valor");
				inserta = "INSERT INTO " + sensor + " (idSen,valor, fecha) VALUES (" + idSen + ", " + valor
						+ ", " + fecha + ")";
			}
			String inserta2 = inserta;//la función query necesita una variable final
				
			mySQLClient.getConnection(connection -> {
				if(connection.succeeded()) {
					connection.result().query(inserta2, result->{
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
		}else {
			routing.response().setStatusCode(400).end();
		}
	}


	private void handleAgrega2(RoutingContext routing) {
		JsonObject body = routing.getBodyAsJson();
		
		if(body.containsKey("idDep") && body.containsKey("nombre")) {
			int idDep = body.getInteger("idDep");
			String nombre = body.getString("nombre");
			String inserta = "INSERT INTO sensores (idDepuradora, nombre) VALUES (" + idDep + ", " + nombre + ")";
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
		}else {
			routing.response().setStatusCode(400).end();
		}
	}
	private void handleLucesONOF(RoutingContext routingContext) {
		JsonObject body = routingContext.getBodyAsJson();
		if(body.containsKey("idDepuradora") && body.containsKey("encendido")) {
			int idDepuradora = body.getInteger("idDepuradora");
			int encendido = body.getInteger("encendido");
			long fecha = obtenerFecha();
			String query = "INSERT INTO luces (idDepuradora, encendido, fecha) VALUES (" + idDepuradora + ", " + encendido +
					", " + fecha + ")";
			
			mySQLClient.getConnection(connection->{
				if(connection.succeeded()) {
					connection.result().query(query, result ->{
						if (result.succeeded()) {
							routingContext.response().end("Se ha modificado correctamente.");
						}else {
							System.out.println(result.cause().getMessage());
							routingContext.response().setStatusCode(400).end();
						}
						connection.result().close();
					});
				}else {
					System.out.println(connection.cause().getMessage());
					routingContext.response().setStatusCode(400).end();
				}
			});
		}
	}
	private void handleLuces(RoutingContext routingConext) {
		String paramStr = "SELECT * FROM luces WHERE idDepuradora = " + routingConext.pathParam("idDep");
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query(paramStr , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String json = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<luces_estado>>(){}.getType();
						List<luces_estado> luces = gsonResult.fromJson(json, tipoLista);
												
						routingConext.response().end(luces.toString());
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
	private void handleDepONOF(RoutingContext routingContext) {
		JsonObject body = routingContext.getBodyAsJson();
		if(body.containsKey("idDepuradora") && body.containsKey("encendido")) {
			int idDepuradora = body.getInteger("idDepuradora");
			int encendido = body.getInteger("encendido");
			long fecha = obtenerFecha();
			String query = "INSERT INTO estadosdep (idDepuradora, encendido, fecha) VALUES (" + idDepuradora + ", " + encendido +
					", " + fecha + ")";
			
			mySQLClient.getConnection(connection->{
				if(connection.succeeded()) {
					connection.result().query(query, result ->{
						if (result.succeeded()) {
							routingContext.response().end("Se ha modificado correctamente.");
						}else {
							System.out.println(result.cause().getMessage());
							routingContext.response().setStatusCode(400).end();
						}
						connection.result().close();
					});
				}else {
					System.out.println(connection.cause().getMessage());
					routingContext.response().setStatusCode(400).end();
				}
			});
		}
	}
	private void handleDepEncendido(RoutingContext routingConext) {
		String paramStr = "SELECT * FROM estadosdep WHERE idDepuradora = " + routingConext.pathParam("idDep");
		mySQLClient.getConnection(connection -> {
			if (connection.succeeded()) {
				connection.result().query(paramStr , result -> {
					if (result.succeeded()) {
						Gson gsonResult = new Gson();
						JsonObject jsonResult = result.result().toJson();
						String json = jsonResult.getJsonArray("rows").encode();
						
						Type tipoLista = new TypeToken<List<luces_estado>>(){}.getType();
						List<luces_estado> luces = gsonResult.fromJson(json, tipoLista);
												
						routingConext.response().end(luces.toString());
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
	
	private Long obtenerFecha() {
		return System.currentTimeMillis();
	}
	
	
}
