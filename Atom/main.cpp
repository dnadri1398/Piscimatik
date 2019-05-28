/*
se ha dejado a lo largo del programa algunos comentarios que comienzan por
TAREA:
usar el buscador para ver qué hay que realizar en el código
*/
#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ESP8266HTTPClient.h>
#include <ArduinoJson.h>
#include <Ticker.h>

//PARAMETROS A MODIFICAR
const char* ssid = ;
const char* pass = ;
const char* broker = ;
const char* http_server = ;
const char* http_server_port = ;
#define DEBUG true

const char* brokerUser = ;
const char* brokerPass = ;
const char* outTopic = ;
const char* inTopic = ;
const char* depuradora = ;//solicitamos al broker el id de la depuradora mediante una consulta, hacer mas tarde
int idDepuradora = 1;

WiFiClient espClient;
PubSubClient client(espClient);


long currentTime, lastTime;
int count = 0;
char messages[50];
Ticker ticker;

//pines
#define lectura  A0
#define depuradora 2

float leeTemperatura(int valorCrudo){
  float R1 = 10000;
  float logR2, R2, T, Tc;
  float c1 = 1.009249522e-03, c2 = 2.378405444e-04, c3 = 2.019202697e-07;

  R2 = R1 * (1023.0 / (float)valorCrudo - 1.0);
  logR2 = log(R2);
  T = (1.0 / (c1 + c2*logR2 + c3*logR2*logR2*logR2));
  Tc = T - 273.15;

  return Tc;
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
      client.subscribe(inTopic);
    }else{
      Serial.print("\nretrying to connect");
      delay(5000);
    }
  }
}



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
      PutDepuradoraOnOff(OnOff);
    }
    else if(strcmp(action, "off") == 0){
      digitalWrite(depuradora, HIGH);
      OnOff = 0;
      Serial.println("Depuradora apagada");
      PutDepuradoraOnOff(OnOff);
    }
    else{
      Serial.println("Accion de depuradora desconocida");
    }
  }else if(!doc["encendidoLuces"].isNull()){
    const char* action = doc["encendidoLuces"];
    Serial.printf("Luces encendidas: %s\n", action);
    int OnOff = 0;

    if(strcmp(action, "on") == 0){
      digitalWrite(depuradora, LOW);
      OnOff = 1;
      Serial.println("Luces encendida");
      PutLucesOnOff(OnOff);
    }
    else if(strcmp(action, "off") == 0){
      digitalWrite(depuradora, HIGH);
      OnOff = 0;
      Serial.println("Luces apagada");
      PutLucesOnOff(OnOff);
    }
    else{
      Serial.println("Accion de Luces desconocida");
    }
  }else if(!doc["leerTemperatura"].isNull()){
    const char* action = doc["leerTemperatura"];
    Serial.printf("leer temperatura: %s\n", action);

    if(strcmp(action, "on") == 0){
      int valor = 0;
      float temperatura = 0.0;
      char tempChar[5];

      valor = analogRead(lectura);
      Serial.print("valor leido: ");
      Serial.println(valor);
      temperatura = leeTemperatura(valor);
      Serial.println(temperatura);

      snprintf(tempChar, 75, "temperatura:%f", temperatura);
      client.publish(outTopic, tempChar);
      //TAREA: poner put de la lectura para subirlo a BBDD
    }
    else if(strcmp(action, "off") == 0){
      Serial.println("operacion de lectura de Temp no realizada");
    }
    else{
      Serial.println("Accion de lectura de Temp desconocida");
    }
  }
    /*TAREA:
    hacer acciones de lectura de:
    -temperatura
    -temperatura exterior
    -luz
    -ph
    -cloro
    -prox ph aumentador
    -prox ph reductor
    -prox cloro
    */

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

void setup() {
  // put your setup code here, to run once:
  pinMode(depuradora, OUTPUT);
  Serial.begin(115200);
  setupWifi();
  client.setServer(broker, 1883);
  client.setCallback(callback);
}

void loop() {
  /*digitalWrite(2, HIGH);
  delay(200);
  digitalWrite(2, LOW);
  delay(200);
  */
  if(!client.connected()){
    reconnect();
  }
  client.loop();

  if(DEBUG){
    prueba();
  }

}
