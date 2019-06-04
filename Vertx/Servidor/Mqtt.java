package proyecto.Piscina;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

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
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;

public class Mqtt extends AbstractVerticle {

	private static Multimap<String, MqttEndpoint> clientTopics;
	private static String IP = "192.168.43.217";
	public String mensajeRecibido = "";
	public boolean listo = false;
	Runnable task3;
	public static String[] listaTopics;
	public static int tam = 0;

	public void start(Future<Void> startFuture) {
		clientTopics = HashMultimap.create();
		// Configuramos el servidor MQTT
		MqttServer mqttServer = MqttServer.create(vertx);
		init(mqttServer);
		
		
		Runnable task = () -> {
			vertx.eventBus().consumer("BUS", message->{
				JsonObject json = new JsonObject(message.body().toString());
				if(json.containsKey("idDep") && json.containsKey("ONOFF")) {
					System.out.println("BOOLEANO: " + json.getBoolean("Dep_luces"));
					cliente(json.getString("idDep"), json.getString("ONOFF"), json.getBoolean("Dep_luces"));
					mensajeRecibido = (json.getBoolean("Dep_luces"))? "Depuradora": "Luces";
					
					if(json.getString("ONOFF").equals("off")) mensajeRecibido += " apagada";					
					else if(json.getString("ONOFF").equals("on")) mensajeRecibido += " encendida";
					
					else mensajeRecibido = "Accion de depuradora desconocida";
				
					message.reply(mensajeRecibido);
			
				};
			});
		};
		Thread thread1 = new Thread(task);
		thread1.start();
		
	}


	/**
	 * Método encargado de inicializar el servidor y ajustar todos los manejadores
	 * 
	 * @param mqttServer
	 */
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			/*
			 * Si se ejecuta este código es que un cliente se ha suscrito al servidor MQTT
			 * para algún topic.
			 */
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesión: " + endpoint.isCleanSession() + "]");
			/*
			 * Indicamos al cliente que se ha contectado al servidor MQTT y que no tenía
			 * sesión previamente creada (parámetro false)
			 */
			endpoint.accept(false);

			/*
			 * Handler para gestionar las suscripciones a un determinado topic. Aquí
			 * registraremos el cliente para poder reenviar todos los mensajes que se
			 * publicen en el topic al que se ha suscrito.
			 */
			handleSubscription(endpoint);

			/*
			 * Handler para gestionar las desuscripciones de un determinado topic. Haremos
			 * lo contrario que el punto anterior para eliminar al cliente de la lista de
			 * clientes registrados en el topic. De este modo, no seguirá recibiendo
			 * mensajes en este topic.
			 */
			handleUnsubscription(endpoint);

			/*
			 * Este handler será llamado cuando se publique un mensaje por parte del cliente
			 * en algún topic creado en el servidor MQTT. En esta función obtendremos todos
			 * los clientes suscritos a este topic y reenviaremos el mensaje a cada uno de
			 * ellos. Esta es la tarea principal del broken MQTT. En este caso hemos
			 * implementado un broker muy muy sencillo. Para gestionar QoS, asegurar la
			 * entrega, guardar los mensajes en una BBDD para después entregarlos, guardar
			 * los clientes en caso de caída del servidor, etc. debemos recurrir a un código
			 * más elaborado o usar una solución existente como por ejemplo Mosquitto.
			 */
			publishHandler(endpoint);

			/*
			 * Handler encargado de gestionar las desconexiones de los clientes al servidor.
			 * En este caso eliminaremos al cliente de todos los topics a los que estuviera
			 * suscrito.
			 */
			handleClientDisconnect(endpoint);
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server está a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}

	/**
	 * Método encargado de gestionar las suscripciones de los clientes a los
	 * diferentes topics. En este método se registrará el cliente asociado al topic
	 * al que se suscribe
	 * 
	 * @param endpoint
	 */
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			// Los niveles de QoS permiten saber el tipo de entrega que se realizará:
			// - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los clientes, pero no
			// que se haga una única vez (pueden llegar duplicados)
			// - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes un única
			// vez (mecanismo más costoso)
			// - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente, por lo que no
			// es necesario ACK por parte de éste
			List<MqttQoS> grantedQosLevels = new ArrayList<>();			
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripción al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());

				// Añadimos al cliente en la lista de clientes suscritos al topic
				clientTopics.put(s.topicName(), endpoint);
			}

			/*
			 * Enviamos el ACK al cliente de que se ha suscrito al topic con los niveles de
			 * QoS indicados
			 */
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	/**
	 * Método encargado de eliminar la suscripción de un cliente a un topic. En este
	 * método se eliminará al cliente de la lista de clientes suscritos a ese topic.
	 * 
	 * @param endpoint
	 */
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				// Eliminos al cliente de la lista de clientes suscritos al topic
				clientTopics.remove(t, endpoint);
				System.out.println("Eliminada la suscripción del topic " + t);
			}
			// Informamos al cliente que la desuscripción se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}

	/**
	 * Manejador encargado de notificar y procesar la desconexión de los clientes.
	 * 
	 * @param endpoint
	 */
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			// Eliminamos al cliente de todos los topics a los que estaba suscritos
			Stream.of(clientTopics.keySet()).filter(e -> clientTopics.containsEntry(e, endpoint))
					.forEach(s -> clientTopics.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}

	/**
	 * Manejador encargado de interceptar los envíos de mensajes de los diferentes
	 * clientes. Este método deberá procesar el mensaje, identificar los clientes
	 * suscritos al topic donde se publica dicho mensaje y enviar el mensaje a cada
	 * uno de esos clientes.
	 * 
	 * @param endpoint
	 */
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			/*
			 * Suscribimos un handler cuando se solicite una publicación de un mensaje en un
			 * topic
			 */
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			/*
			 * Suscribimos un handler cuando haya finalizado la publicación del mensaje en
			 * el topic
			 */
			endpoint.publishComplete(messageId);
		});
	}

	/**
	 * Método de utilidad para la gestión de los mensajes salientes.
	 * 
	 * @param message
	 * @param endpoint
	 */
	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		/*
		 * System.out.println("Mensaje publicado por el cliente " +
		 * endpoint.clientIdentifier() + " en el topic " + message.topicName());
		 * System.out.println("    Contenido del mensaje: " +
		 * message.payload().toString());
		 */

		/*
		 * Obtenemos todos los clientes suscritos a ese topic (exceptuando el cliente
		 * que envía el mensaje) para así poder reenviar el mensaje a cada uno de ellos.
		 * Es aquí donde nuestro código realiza las funciones de un broken MQTT
		 */
		// System.out.println("Origen: " + endpoint.clientIdentifier());
		List<MqttEndpoint> clientsToRemove = new ArrayList<>();
		for (MqttEndpoint client : clientTopics.get(message.topicName())) {
			// System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				try {
					client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(),
							message.isRetain()).publishReleaseHandler(idHandler -> {
								client.publishComplete(idHandler);
							});
				} catch (Exception e) {
					if (e.getMessage().toLowerCase().equals("connection not accepted yet")) {
						clientsToRemove.add(client);
					} else {
						System.out.println("Error, no se pudo enviar mensaje. " + e.getMessage());
					}
				}
		}
		
		if (!clientsToRemove.isEmpty()) {
			clientsToRemove.forEach(clientToRemove -> handleClientDisconnect(clientToRemove));
			clientsToRemove.clear();
		}

		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			/*
			 * Se podría hacer algo con el mensaje como, por ejemplo, almacenar un registro
			 * en la base de datos
			 */
			}
			// Envía el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			/*
			 * Envía el ACK al cliente de que el mensaje ha sido publicado y cierra el canal
			 * para este mensaje. Así se evita que los mensajes se publiquen por duplicado
			 * (QoS)
			 */
			endpoint.publishRelease(message.messageId());
		}
	}
	private static void handleMessageTODOS(MqttPublishMessage message, MqttEndpoint endpoint) {
		/*
		 * System.out.println("Mensaje publicado por el cliente " +
		 * endpoint.clientIdentifier() + " en el topic " + message.topicName());
		 * System.out.println("    Contenido del mensaje: " +
		 * message.payload().toString());
		 */

		/*
		 * Obtenemos todos los clientes suscritos a ese topic (exceptuando el cliente
		 * que envía el mensaje) para así poder reenviar el mensaje a cada uno de ellos.
		 * Es aquí donde nuestro código realiza las funciones de un broken MQTT
		 */
		// System.out.println("Origen: " + endpoint.clientIdentifier());
		
		
		List<MqttEndpoint> clientsToRemove = new ArrayList<>();
		for (MqttEndpoint client : clientTopics.get(message.topicName())) {
			// System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				try {
					client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(),
							message.isRetain()).publishReleaseHandler(idHandler -> {
								client.publishComplete(idHandler);
							});
				} catch (Exception e) {
					if (e.getMessage().toLowerCase().equals("connection not accepted yet")) {
						clientsToRemove.add(client);
					} else {
						System.out.println("Error, no se pudo enviar mensaje. " + e.getMessage());
					}
				}
		}
		
		if (!clientsToRemove.isEmpty()) {
			clientsToRemove.forEach(clientToRemove -> handleClientDisconnect(clientToRemove));
			clientsToRemove.clear();
		}

		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();//------------------------------------------------
			switch (topicName) {
			/*
			 * Se podría hacer algo con el mensaje como, por ejemplo, almacenar un registro
			 * en la base de datos
			 */
			}
			// Envía el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			/*
			 * Envía el ACK al cliente de que el mensaje ha sido publicado y cierra el canal
			 * para este mensaje. Así se evita que los mensajes se publiquen por duplicado
			 * (QoS)
			 */
			endpoint.publishRelease(message.messageId());
		}
	}

	
	
	
	
	
	
	
	
	public void cliente(String idDep, String encendido, boolean Dep_Luces ) {
		String topicOut = "/depuradoraIn/" + idDep;
		MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
		mqttClient.connect(1883, IP, s -> {

			mqttClient.subscribe(topicOut, MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					
					System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal " + topicOut);

					mqttClient.publishHandler(new Handler<MqttPublishMessage>() {
						@Override
						public void handle(MqttPublishMessage arg0) {
							
							//mensajeRecibido = arg0.payload().toString();							
							System.out.println(mensajeRecibido);
							System.out.println("-----" + mqttClient.clientId());
							//listo = true;
							}
					});
				}
			});
			JsonObject json = new JsonObject();
			json.put(Dep_Luces? "encendidoDep":"encendidoLuces", encendido);
			mqttClient.publish(topicOut,
					Buffer.buffer(json.encode()),
					MqttQoS.AT_LEAST_ONCE, false, false);
			
				
		});
	}

}
