#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ESP8266HTTPClient.h>
#include <ArduinoJson.h>
#include <Ticker.h>

//PARAMETROS A MODIFICAR

const char* ssid = ;
const char* pass =;
const char* broker =;
const char* http_server = ;
const char* http_server_port = "8081";
#define DEBUG true


const char* outTopic = "/depuradoraOut/001";
const char* comun = "broker";
const char* inTopic = "/depuradoraIn/001";
int idDepuradora = 1;

WiFiClient espClient;
PubSubClient client(espClient);


long currentTime, lastTime;
long tiempoActual, tiempoAnterior, tiempoVertido;
int count = 0;
char messages[50];
Ticker ticker;
bool interrupcion = false;
int encendidoDepuradora = LOW;
int luzExterior = 800;//carga este valor inicial para que "sea de dia"
boolean echarPHUp = false;// el sistema indica que hay que echar producto
boolean permisoEcharProductos = true;//el usuario permite o no verter productos
boolean horarioEcharProductos = false;//determina si es hora de echar productos
boolean servoAbiertoPH = false; //indica si se está vertiendo aumentador ph
boolean depTrasVertido= false;
int caudalPHUp = 1; //como ejemplo usamos 1 litro por segundo
float capacidadPiscina = 10*4*1.5;
float cantidadAumentador = capacidadPiscina * 0.02;// son 0.02 por cada m3 para
//aumentar 1 decima de PH


//pines
#define lectura  A0
#define luces D4
#define depuradora 2
#define sensorEcho D5
#define latchPin D1  // Pin conectado al Pin 12 del 74HC595 (Latch)
#define dataPin D2  // Pin conectado al Pin 14 del 74HC595 (Data)
#define clockPin D3

//IDs dentro de la base de datos de los sensores de éste espPiscina
#define idTemperatura 2
#define idph 1


const byte numeros[9] = {
	0b00000000,//por si acaso colocaremos todo a cero antes de hacer otras lecturas
	0b10000000,
	0b01000000,
	0b00100000,
	0b00010000,
	0b00001000,
	0b00000100,
	0b00000010,
	0b00000001
};
//TAREA: leer los sensores cada hora mediante el uso de Ticker
float leeTemperatura(int valorCrudo){
	float R1 = 10000;
	float logR2, R2, T, Tc;
	float c1 = 1.009249522e-03, c2 = 2.378405444e-04, c3 = 2.019202697e-07;

	R2 = R1 * (906.0 / (float)valorCrudo - 1.0);// se ha calibrado a 906 tendria que ser 1023.0 ha causa del diodo, resta precision
	logR2 = log(R2);
	T = (1.0 / (c1 + c2*logR2 + c3*logR2*logR2*logR2));
	Tc = T - 273.15;

	return Tc;
}
float calcularDistancia()
{
	float sonido = 34300.0;
	// La función pulseIn obtiene el tiempo que tarda en cambiar entre estados, en este caso a HIGH
	unsigned long tiempo = pulseIn(sensorEcho, HIGH);
	// Obtenemos la distancia en cm, hay que convertir el tiempo en segudos ya que está en microsegundos
	// por eso se multiplica por 0.000001
	float distancia = tiempo * 0.000001 * sonido / 2.0;
	delay(500);

	return distancia;
}

float calcularPH(){
	float valorPH=analogRead(lectura);

	float pHVol=(float)valorPH*5.0/1024/6;
	float phValue = -5.70 * pHVol + 21.34 + 7;
	return phValue;
}

int leeLuz(){
	int valorEncendido = 200;
	int encendido;
	int valorLuz = analogRead(lectura);
	if(valorLuz < valorEncendido){
		encendido = 1;
	}
	else if(valorLuz >= valorEncendido){
		encendido = 0;
	}else encendido = 0;
	return encendido;
}


void setupWifi(){
	delay(100);
	Serial.print("\nconectando a la red: ");
	Serial.println(ssid);

	WiFi.begin(ssid, pass);
	while(WiFi.status() != WL_CONNECTED){
		delay(100);
		Serial.print("-");
	}
	Serial.print("conectado, IP registrada: ");
	Serial.println(WiFi.localIP());
}

void reconnect(){
	while(!client.connected()){
		Serial.print("\nconnecting to ");
		Serial.println(broker);
		//if(client.connect("espPiscina", brokerUser, brokerPass)){
		if(client.connect("espPiscina")){
			Serial.print("\nconnected to");
			Serial.println(broker);
			client.subscribe(comun);
			client.subscribe(inTopic);
			}else{
			Serial.print("\nretrying to connect");
			delay(5000);
		}
	}
}


//-------------------------------PUTS SOBRE EL SERVIDOR---------------------
void PutDepuradoraOnOff(int OnOff){
	HTTPClient http;
	String url = "http://";
	url += http_server;
	url += ":";
	url += http_server_port;
	url += "/dep/";
	url += "ONOFF";
	String message = "Enviando peticion PUT al servidor REST. ";
	message += url;
	Serial.println(message);
	http.begin(url);

	const size_t bufferSize = JSON_OBJECT_SIZE(1) + 370;
	DynamicJsonDocument root(bufferSize);
	root["idDepuradora"] = idDepuradora;
	root["encendido"] = OnOff;
	String json_string;
	serializeJson(root, json_string);

	int httpCode = http.PUT(json_string);

	if (httpCode > 0)
	{

		String payload = http.getString();
		Serial.println("payload put: " + payload);
	}

	Serial.printf("\nRespuesta servidor REST PUT %d\n", httpCode);
	http.end();
}


void PutLucesOnOff(int OnOff){
	HTTPClient http;
	String url = "http://";
	url += http_server;
	url += ":";
	url += http_server_port;
	url += "/luces/";
	url += "ONOFF";
	String message = "Enviando peticion PUT al servidor REST. ";
	message += url;
	Serial.println(message);
	http.begin(url);

	const size_t bufferSize = JSON_OBJECT_SIZE(1) + 370;
	DynamicJsonDocument root(bufferSize);
	root["idDepuradora"] = idDepuradora;
	root["encendido"] = OnOff;
	String json_string;
	serializeJson(root, json_string);

	int httpCode = http.PUT(json_string);

	if (httpCode > 0)
	{

		String payload = http.getString();
		Serial.println("payload put: " + payload);
	}

	Serial.printf("\nRespuesta servidor REST PUT %d\n", httpCode);
	http.end();
}

void PutTemperatura(int temperatura){
	//  /sensores/:nombre/agregar
	HTTPClient http;
	String url = "http://";
	url += http_server;
	url += ":";
	url += http_server_port;
	url += "/sensores/senstemp/agregar";
	String message = "Enviando peticion PUT al servidor REST. ";
	message += url;
	Serial.println(message);
	http.begin(url);

	const size_t bufferSize = JSON_OBJECT_SIZE(1) + 370;
	DynamicJsonDocument root(bufferSize);
	root["idSen"] = idTemperatura;//id del sensor dentro de la BD
	root["valor"] = temperatura;//valor a enviar
	String json_string;
	serializeJson(root, json_string);

	int httpCode = http.PUT(json_string);

	if (httpCode > 0)
	{

		String payload = http.getString();
		Serial.println("payload put: " + payload);
	}

	Serial.printf("\nRespuesta servidor REST PUT %d\n", httpCode);
	http.end();
}
void PutPH(int ph){
	//  /sensores/:nombre/agregar
	HTTPClient http;
	String url = "http://";
	url += http_server;
	url += ":";
	url += http_server_port;
	url += "/sensores/sensph/agregar";
	String message = "Enviando peticion PUT al servidor REST. ";
	message += url;
	Serial.println(message);
	http.begin(url);

	const size_t bufferSize = JSON_OBJECT_SIZE(1) + 370;
	DynamicJsonDocument root(bufferSize);
	root["idSen"] = idph;//id del sensor dentro de la BD
	root["valor"] = ph;//valor a enviar
	String json_string;
	serializeJson(root, json_string);

	int httpCode = http.PUT(json_string);

	if (httpCode > 0)
	{

		String payload = http.getString();
		Serial.println("payload put: " + payload);
	}

	Serial.printf("\nRespuesta servidor REST PUT %d\n", httpCode);
	http.end();
}


void callback(char* topic, byte* payload, unsigned int length){
	Serial.print("Mensaje recibido por canal: ");
	Serial.println(topic);
	for(int i = 0; i < length; i++){
		Serial.print((char) payload[i]);
	}
	Serial.println();

	DynamicJsonDocument doc(length);
	deserializeJson(doc, payload, length);

	//
	//verifica si se ha enviado una petición para encender la depuradora
	if(!doc["encendidoDep"].isNull()){
		const char* action = doc["encendidoDep"];
		Serial.printf("Depuradora encendida: %s\n", action);
		int OnOff = 0;

		if(strcmp(action, "on") == 0){
			digitalWrite(depuradora, LOW);
			OnOff = 1;
			Serial.println("Depuradora encendida");
			client.publish(inTopic, "Depuradora encendida");
			//PutDepuradoraOnOff(OnOff);
		}
		else if(strcmp(action, "off") == 0){
			digitalWrite(depuradora, HIGH);
			OnOff = 0;
			Serial.println("Depuradora apagada");
			client.publish(inTopic, "Depuradora apagada");
			//PutDepuradoraOnOff(OnOff);
		}
		else{
			Serial.println("Accion de depuradora desconocida");
			client.publish(inTopic, "Accion de depuradora desconocida");
		}
		}else if(!doc["encendidoLuces"].isNull()){
		const char* action = doc["encendidoLuces"];
		Serial.printf("Luces encendidas: %s\n", action);
		int OnOff = 0;

		if(strcmp(action, "on") == 0){
			digitalWrite(luces, LOW);
			OnOff = 1;
			client.publish(inTopic,"Luces encendidas");
			PutLucesOnOff(OnOff);
		}
		else if(strcmp(action, "off") == 0){
			digitalWrite(luces, HIGH);
			OnOff = 0;
			client.publish(inTopic,"Luces apagadas");
			PutLucesOnOff(OnOff);
		}
		else{
			client.publish(inTopic,"Luces apagadas");
		}
	}//---------------------------LECTURA MANUAL DE SENSORES----------------------
	else if(!doc["leerTemperatura"].isNull()){
		const char* action = doc["leerTemperatura"];
		Serial.printf("leer temperatura: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float temperatura = 0.0;
			char tempChar[5];
			digitalWrite(latchPin, LOW); //activa la señal al 74hc595
			shiftOut(dataPin, clockPin, LSBFIRST, numeros[1]);
			digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
			valor = analogRead(lectura);
			Serial.print("valor leido: ");//borrar
			Serial.println(valor);//borrar
			temperatura = leeTemperatura(valor);
			Serial.println(temperatura);//borrar

			snprintf(tempChar, 75, "temperatura:%f", temperatura);
			client.publish(outTopic, tempChar);
			PutTemperatura(valor);
		}
		else if(strcmp(action, "off") == 0){
			client.publish(outTopic, "operacion de lectura de Temp no realizada");
		}
		else{
			client.publish(outTopic, "Accion de lectura de Temp desconocida");
		}
	}//--------------
	else if(!doc["leerLuz"].isNull()){
		const char* action = doc["leerLuz"];
		Serial.printf("Leer cantidad de luz exterior: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float luz = 0.0;
			char luzChar[5];

			digitalWrite(latchPin, LOW); //activa la señal al 74hc595
			shiftOut(dataPin, clockPin, LSBFIRST, numeros[2]);
			digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer

			valor = analogRead(lectura);
			Serial.print("Valor leido: ");
			Serial.println(valor);
			//luz = leeLuz(valor);
			//Serial.println(luz);
			snprintf(luzChar, 75, "Luz:%d", valor);
			client.publish(outTopic, luzChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de Luz no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de Luz desconocida");
		}
	}//--------------
	else if(!doc["leerTemperaturaExterior"].isNull()){
		const char* action = doc["leerTemperaturaExterior"];
		Serial.printf("Leer temperatura exterior: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float temperaturaExterior = 0.0;
			char tempExteriorChar[5];

			valor = analogRead(lectura);
			Serial.print("Valor leido: ");
			Serial.println(valor);
			temperaturaExterior = leeTemperatura(valor);
			Serial.println(temperaturaExterior);
			snprintf(tempExteriorChar, 75, "Temperatura exterior:%f", temperaturaExterior);
			client.publish(outTopic, tempExteriorChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de Temp no realizada");
		}
		else{
			client.publish(outTopic,"Accion de lectura de Temp desconocida");
		}
	}//--------------
	else if(!doc["leerPH"].isNull()){
		const char* action = doc["leerPH"];
		Serial.printf("Leer nivel de PH: %s\n", action);

		if(strcmp(action, "on") == 0){
			float ph = 0.0;
			char phChar[5];

			digitalWrite(latchPin, LOW);
			shiftOut(dataPin, clockPin, LSBFIRST, numeros[3]);
			digitalWrite(latchPin, HIGH);
			ph = calcularPH();

			Serial.print("Valor leido: ");
			Serial.println(ph);
			//ph = leePH(valor);
			snprintf(phChar, 75, "PH:%f", ph);
			client.publish(outTopic,phChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de PH no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de PH desconocida");
		}
	} //--------------
	else if(!doc["leerCloro"].isNull()){
		const char* action = doc["leerCloro"];
		Serial.printf("Leer nivel de cloro: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float cloro = 0.0;
			char cloroChar[5];

			valor = analogRead(lectura);
			Serial.print("Valor leido: ");
			Serial.println(valor);
			//cloro = leeCloro(valor);
			Serial.println(cloro);
			snprintf(cloroChar, 75, "Cloro:%f", cloro);
			client.publish(outTopic, cloroChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de cloro no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de cloro desconocida");
		}

	} //--------------
	else if(!doc["leerPHUpDep"].isNull()){
		const char* action = doc["leerPHUpDep"];
		Serial.printf("Leer nivel de PHUp: %s\n", action);

		if(strcmp(action, "on") == 0){
			float phUp = 0.0;
			char phUpChar[5];

			digitalWrite(latchPin, LOW);
			shiftOut(dataPin, clockPin, LSBFIRST, numeros[4]);
			digitalWrite(latchPin, HIGH);
			phUp = calcularDistancia();

			Serial.print("Valor leido: ");
			//phUp = leePHUp(valor);
			Serial.println(phUp);
			snprintf(phUpChar, 75, "PHUp: %f", phUp);
			client.publish(outTopic,phUpChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de PHUp no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de PHUp desconocida");
		}

	} //--------------
	else if(!doc["leerPHDownDep"].isNull()){
		const char* action = doc["leerPHDownDep"];
		Serial.printf("Leer nivel de PHDown: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float phDown = 0.0;
			char phDownChar[5];

			valor = analogRead(lectura);
			Serial.print("Valor leido: ");
			Serial.println(valor);
			//phDown = leePHDown(valor);
			Serial.println(phDown);
			snprintf(phDownChar, 75, "PHDown:%f", phDown);
			client.publish(outTopic,phDownChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de PHDown no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de PHDown desconocida");
		}

	}//--------------
	else if(!doc["leerCloroDep"].isNull()){
		const char* action = doc["leerCloroDep"];
		Serial.printf("Leer nivel de cloroDep: %s\n", action);

		if(strcmp(action, "on") == 0){
			int valor = 0;
			float cloroDep = 0.0;
			char cloroDepChar[5];

			valor = analogRead(lectura);
			Serial.print("Valor leido: ");
			Serial.println(valor);
			//cloroDep = leecloroDep(valor);
			Serial.println(cloroDep);
			snprintf(cloroDepChar, 75, "CloroDep:%f", cloroDep);
			client.publish(outTopic,cloroDepChar);
			//TAREA: poner put de la lectura para subirlo a BBDD
			}else if(strcmp(action, "off") == 0){
			client.publish(outTopic,"Operacion de lectura de cloroDep no realizada");
			} else{
			client.publish(outTopic,"Accion de lectura de cloroDep desconocida");
		}

	}
	//------------------------
	else if(!doc["permisoEcharProductos"].isNull()){
		const char* action = doc["permisoEcharProductos"];
		Serial.printf("permiso para verter productos quimicos: %s\n", action);

		if(strcmp(action, "on") == 0){

			permisoEcharProductos = true;
			client.publish(outTopic,"activado");
			}else if(strcmp(action, "off") == 0){
			permisoEcharProductos = false;
			client.publish(outTopic,"desactivado");
			} else{
			client.publish(outTopic,"Accion desconocida");
		}

	}
	else if(!doc["horarioEcharProductos"].isNull()){
		const char* action = doc["horarioEcharProductos"];
		Serial.printf("horario para verter productos quimicos: %s\n", action);

		if(strcmp(action, "on") == 0){
			Serial.printf("SE HA ACTIVADO");
			//horarioEcharProductos = true;
			//client.publish(outTopic,"activado");
			}else if(strcmp(action, "off") == 0){
			//horarioEcharPRoductos = false;
			Serial.printf("SE HA DESACTIVADO");

			//client.publish(outTopic,"desactivado");
			} else{
			//client.publish(outTopic,"Accion desconocida");
		}

	}

}

void funcionEcharProductos(){
	//2 cl por cada metro cúbico de agua de la piscina, por cada décima que queramos bajar el pH

	if(echarPHUp && permisoEcharProductos && horarioEcharProductos){
		tiempoActual = millis();
		if(servoAbiertoPH){
			digitalWrite(depuradora, LOW);//enciende depuradora
			//TAREA: abrir servo
			tiempoAnterior = millis();

		}
		if(tiempoActual - tiempoAnterior >= tiempoVertido){
			//TAREA: cerrar Servo
			//se indica que la depuradora está operando tras verter productos
			depTrasVertido = true;
			echarPHUp = false;
		}

	}

}
//------------------------------TICKER--------------------------
void funcionInterrupcion(){
	interrupcion = true;
	//NO USAR AQUÍ LAS OPERACIONES PUT, DURANTE UNA INTERRUPCION NO TIENE CONEXION
	//A LA BASE DE DATOS
}


//TAREA: meter los put en cada lectura de sensor
void leeSensores(){
	digitalWrite(latchPin, LOW); //activa la señal al 74hc595
	shiftOut(dataPin, clockPin, LSBFIRST, numeros[1]);
	digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
	PutTemperatura(analogRead(lectura));

	delay(100);

	digitalWrite(latchPin, LOW); //activa la señal al 74hc595
	shiftOut(dataPin, clockPin, LSBFIRST, numeros[2]);
	digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
	luzExterior = analogRead(lectura);

	delay(100);

	digitalWrite(latchPin, LOW);
	shiftOut(dataPin, clockPin, LSBFIRST, numeros[3]);
	digitalWrite(latchPin, HIGH);
	float valorPH = calcularPH();
	if(valorPH < 7,2){
		echarPHUp = true;
		tiempoVertido = (7,5 - valorPH)*10 * cantidadAumentador * caudalPHUp;
	}
	//PUT ph

	delay(100);

	digitalWrite(latchPin, LOW);
	shiftOut(dataPin, clockPin, LSBFIRST, numeros[4]);
	digitalWrite(latchPin, HIGH);
	float valorDistancia = calcularDistancia();
	//PUT ph



	//PutPH(analogRead(lectura));
}


//ésta funcion solo sirve de prueba para verificar que envía mensajes al canal
void prueba() {
	char* topicPrueba = "prueba";
	currentTime = millis();
	if(currentTime - lastTime > 2000){
		count++;
		snprintf(messages, 75, "Count:%d", count);
		Serial.print("sending messages: ");
		Serial.println(messages);
		client.publish(topicPrueba, messages);
		lastTime = millis();
	}
}

int cont = 0;
int cont2 = 0;
void setup() {
	// put your setup code here, to run once:
	pinMode(depuradora, OUTPUT);
	pinMode(luces, OUTPUT);
	pinMode(latchPin, OUTPUT);
	pinMode(clockPin, OUTPUT);
	pinMode(dataPin, OUTPUT);
	pinMode(sensorEcho, INPUT);
	Serial.begin(115200);
	setupWifi();
	client.setServer(broker, 1883);
	client.setCallback(callback);
	ticker.attach(1000,funcionInterrupcion);
}

void loop() {
	if(!client.connected()){
		reconnect();
	}
	if(echarPHUp && permisoEcharProductos && horarioEcharProductos){
		funcionEcharProductos();
		}else{
		//TAREA: cerrar servo, puede ocurrir que se elimine el permiso mientras está abierto
	}
	client.loop();


	if(interrupcion){
		interrupcion = false;
		leeSensores();
		if(depTrasVertido){
			cont = cont +1;
			if(cont >=3){
				digitalWrite(depuradora, HIGH);//apagamos depuradora
				depTrasVertido = false;
				cont = 0;
			}
		}
		if(horarioEcharProductos){
			cont2 = cont2 +1;
			if(cont2 >= 3){
				horarioEcharProductos = false;
				Serial.println("se ha desactivado el horario");
				cont2 = 0;
			}
		}
	}

	if(DEBUG){
		prueba();
	}

}
