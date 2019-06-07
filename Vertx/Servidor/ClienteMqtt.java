ppackage proyecto.Piscina;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.messages.MqttPublishMessage;

public class ClienteMqtt extends AbstractVerticle{
	
	private static Multimap<String, MqttEndpoint> clientTopics;
	private static String IP = "192.168.43.217";
	//private static String IP = "192.168.1.128";
	private static String topicOut = "broker";

	
	

	public void start(Future<Void> startFuture) {
		clientTopics = HashMultimap.create();
		
		MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));

		mqttClient.connect(1883, IP, s -> {


			mqttClient.subscribe(topicOut, MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal " + topicOut);

					mqttClient.publishHandler(new Handler<MqttPublishMessage>() {
						@Override
						public void handle(MqttPublishMessage arg0) {
						
							JsonObject message = new JsonObject(arg0.payload());
							System.out.println("-----" + message.getString("clientId"));
							System.out.println("-----" + mqttClient.clientId());
							if (!message.getString("clientId").equals(mqttClient.clientId()))
								System.out.println("Mensaje recibido por el cliente: " + arg0.payload().toString());
						}
					});
				}
			});

			/*
			 * Este timer envía mensajes desde el cliente al servidor cada 3 segundos.
			 */
			new Timer().scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					Date horaActual = new Date(System.currentTimeMillis());
					Calendar activar = Calendar.getInstance();
					activar.setTime(horaActual);
		
					
					System.out.println(horaActual);
					//enviamos al broker si activamos o no la hora de vertido de productos
					if (mqttClient.isConnected()) {
						
						if(activar.get(Calendar.HOUR_OF_DAY) == 1) {
						mqttClient.publish(topicOut,
								Buffer.buffer(new JsonObject().put("horarioEcharProductos", "on")
										.encode()),
								MqttQoS.AT_LEAST_ONCE, false, false);
						}else if(activar.get(Calendar.HOUR_OF_DAY) == 3)
							mqttClient.publish(topicOut,
									Buffer.buffer(new JsonObject().put("horarioEcharProductos", "off")
											.encode()),
									MqttQoS.AT_LEAST_ONCE, false, false);
					}
					}
			}, 1000, 3000000);
		});
		
	}
	
}

