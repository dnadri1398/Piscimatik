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
//TAREA: borrar del proyecto final el párametro movil
const int movil = 0;
const char* ssid = movil == 0?  "MiFibra-0F08":"asdf";
const char* pass = movil == 0? "ghMc67TK":"13571357";
const char* broker = movil == 0? "192.168.1.128":"192.168.43.217";
const char* http_server = movil == 0? "192.168.1.128":"192.168.43.217";
const char* http_server_port = "8081";
#define DEBUG true

const char* brokerUser = "saloje96@gmail.com";
const char* brokerPass = "de14b01b";
const char* outTopic = "/depuradoraOut/001";
const char* inTopic = "/depuradoraIn/001";
const char* depuradora = "jesus";//solicitamos al broker el id de la depuradora mediante una consulta, hacer mas tarde
int idDepuradora = 1;

WiFiClient espClient;
PubSubClient client(espClient);


long currentTime, lastTime;
int count = 0;
char messages[50];
Ticker ticker;
bool interrupcion = false;

//pines
#define lectura  A0
#define depuradora 2
#define latchPin D1  // Pin conectado al Pin 12 del 74HC595 (Latch)
#define dataPin D2  // Pin conectado al Pin 14 del 74HC595 (Data)
#define clockPin D3

//IDs dentro de la base de datos de los sensores de éste espPiscina
#define idTemperatura 2


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
      //TAREA: poner put de la lectura para subirlo a BBDD
    }
    else if(strcmp(action, "off") == 0){
      Serial.println("operacion de lectura de Temp no realizada");//borrar
      client.publish(outTopic, "operacion de lectura de Temp no realizada");
    }
    else{
      Serial.println("Accion de lectura de Temp desconocida");
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
      Serial.println("Operacion de lectura de Luz no realizada");
    } else{
      Serial.println("Accion de lectura de Luz desconocida");
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
      Serial.println("Operacion de lectura de Temp no realizada");
    }
    else{
      Serial.println("Accion de lectura de Temp desconocida");
    }
  }//--------------
  else if(!doc["leerPH"].isNull()){
      const char* action = doc["leerPH"];
      Serial.printf("Leer nivel de PH: %s\n", action);

      if(strcmp(action, "on") == 0){
        int valor = 0;
        float ph = 0.0;
        char phChar[5];

        valor = analogRead(lectura);
        Serial.print("Valor leido: ");
        Serial.println(valor);
        //ph = leePH(valor);
        Serial.println(ph);
        snprintf(phChar, 75, "PH:%f", ph);
        client.publish(outTopic,phChar);
        //TAREA: poner put de la lectura para subirlo a BBDD
      }else if(strcmp(action, "off") == 0){
        Serial.println("Operacion de lectura de PH no realizada");
      } else{
        Serial.println("Accion de lectura de PH desconocida");
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
        Serial.println("Operacion de lectura de cloro no realizada");
      } else{
        Serial.println("Accion de lectura de cloro desconocida");
        }

    } //--------------
    else if(!doc["leerPHUpDep"]){
      const char* action = doc["leerPHUpDep"];
      Serial.printf("Leer nivel de PHUp: %s\n", action);

      if(strcmp(action, "on") == 0){
        int valor = 0;
        float phUp = 0.0;
        char phUpChar[5];

        valor = analogRead(lectura);
        Serial.print("Valor leido: ");
        Serial.println(valor);
        //phUp = leePHUp(valor);
        Serial.println(phUp);
        snprintf(phUpChar, 75, "PHUp:%f", phUp);
        client.publish(outTopic,phUpChar);
        //TAREA: poner put de la lectura para subirlo a BBDD
      }else if(strcmp(action, "off") == 0){
        Serial.println("Operacion de lectura de PHUp no realizada");
      } else{
        Serial.println("Accion de lectura de PHUp desconocida");
      }

    } //--------------
    else if(!doc["leerPHDownDep"]){
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
        Serial.println("Operacion de lectura de PHDown no realizada");
      } else{
        Serial.println("Accion de lectura de PHDown desconocida");
      }

    }//--------------
    else if(!doc["leerCloroDep"]){
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
        Serial.println("Operacion de lectura de cloroDep no realizada");
      } else{
        Serial.println("Accion de lectura de cloroDep desconocida");
      }

    }

}


//------------------------------TICKER--------------------------
void funcionInterrupcion(){
  interrupcion = true;
  //NO USAR AQUÍ LAS OPERACIONES PUT, DURANTE UNA INTERRUPCION NO TIENE CONEXION
  //A LA BASE DE DATOS

  //----------leerTemperatura
  /*int valor = 0;

  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[1]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  valor = analogRead(lectura);
  Serial.println(valor);
  PutTemperatura(valor);*/
  //----------leer luz(no hay tabla en la BD)
//  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
//  shiftOut(dataPin, clockPin, LSBFIRST, numeros[2]);
//  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
//  putLuz(analogRead(lectura));
//  delay(100);
  //----------leer temperatura exterior
  /*digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[3]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putTemperaturaExterior(analogRead(lectura));
  delay(100);
  //----------leer ph
  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[4]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putPH(analogRead(lectura));
  delay(100);
  //----------leer cloro
  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[5]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putCloro(analogRead(lectura));
  delay(100);
  //----------leer ph up deposito
  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[6]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putPHUpDep(analogRead(lectura));
  delay(100);
  //----------leer ph down deposito
  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[7]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putPHDownDep(analogRead(lectura));
  delay(100);
  //----------leer cloro deposito
  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[8]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  putcloroDep(analogRead(lectura));*/
}
void leeSensores(){
  int valor = 0;

  digitalWrite(latchPin, LOW); //activa la señal al 74hc595
  shiftOut(dataPin, clockPin, LSBFIRST, numeros[1]);
  digitalWrite(latchPin, HIGH); //envía al 74hc595  lo guardado en el buffer
  valor = analogRead(lectura);
  Serial.println(valor);
  PutTemperatura(valor);
}


//TAREA: echar productos quimicos cuando sea de noche, esto se hace con interrupciones sensor de luz



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
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  Serial.begin(115200);
  setupWifi();
  client.setServer(broker, 1883);
  client.setCallback(callback);
  ticker.attach(10,funcionInterrupcion);
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
  if(interrupcion){
    leeSensores();
    interrupcion = false;
  }

  if(DEBUG){
    prueba();
  }

}
