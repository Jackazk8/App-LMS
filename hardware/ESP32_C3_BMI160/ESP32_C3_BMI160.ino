/* ESP32-C3 Super Mini + BMI160, BLE BIN20 v1, dos unidades independientes.
   Arduino-ESP32 3.x, biblioteca BLE incluida en el core (Bluedroid).
   SDA=GPIO4; SCL=GPIO5; BMI160 3V3/CS -> 3V3; GND/SA0 -> GND.
   Cambia MANO_IZQUIERDA a true para la segunda placa.
   Alimentacion inicial: USB-C. No requiere libreria externa para BMI160.
   El servicio UUID identifica BIN20 v1; no es UART/SPP ni texto por BLE.
*/
#include <Arduino.h>
#include <Wire.h>
#include <math.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <atomic>
#if !defined(CONFIG_IDF_TARGET_ESP32C3) && !defined(CONFIG_IDF_TARGET_ESP32)
#error "Firmware previsto para ESP32-C3 o ESP32 clasico con BLE."
#endif
constexpr bool MANO_IZQUIERDA = false;
constexpr int SDA_PIN = 4;
constexpr int SCL_PIN = 5;
constexpr uint32_t SAMPLE_PERIOD_MS = 20; // 50 Hz nominales
const char *DEVICE_NAME = MANO_IZQUIERDA ? "LSM_C3_Izq" : "LSM_C3_Der";
const char *SERVICE_UUID = "920b0001-4e8d-4f7f-9e51-50b7c41a1600";
const char *DATA_UUID = "920b0002-4e8d-4f7f-9e51-50b7c41a1600";
BLEServer *bleServer = nullptr;
BLECharacteristic *dataCharacteristic = nullptr;
std::atomic<bool> connected{false};
std::atomic<bool> restartAdvertising{false};
class SensorCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *) override { connected.store(true); }
  void onDisconnect(BLEServer *) override {
    connected.store(false);
    restartAdvertising.store(true);
  }
};
void beginBle() {
  BLEDevice::init(DEVICE_NAME);
  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new SensorCallbacks());
  BLEService *service = bleServer->createService(SERVICE_UUID);
  dataCharacteristic = service->createCharacteristic(DATA_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  dataCharacteristic->addDescriptor(new BLE2902());
  service->start();
  BLEAdvertising *advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->start();
}
void putU32(uint8_t *p, uint32_t value) {
  for (unsigned i=0; i<4; ++i) p[i]=(uint8_t)(value >> (8*i));
}
uint32_t sequenceNumber = 0;
uint32_t nextSampleAt = 0;
uint32_t lastPrintAt = 0;

uint8_t sensor = 0;
bool listo = false;

bool leerEn(uint8_t direccion, uint8_t registro, uint8_t *datos, size_t cantidad) {
  Wire.beginTransmission(direccion);
  Wire.write(registro);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom(direccion, cantidad, true) != cantidad) {
    while (Wire.available()) Wire.read();
    return false;
  }
  for (size_t i=0; i<cantidad; i++) datos[i] = Wire.read();
  return true;
}

bool escribir(uint8_t registro, uint8_t valor) {
  Wire.beginTransmission(sensor);
  Wire.write(registro); Wire.write(valor);
  bool ok = Wire.endTransmission() == 0;
  delay(2);  // Dar tiempo a escrituras incluso en suspend.
  return ok;
}

int16_t firmado(const uint8_t *p) {
  int32_t v = (uint16_t)p[0] | ((uint16_t)p[1] << 8);
  return (int16_t)(v >= 32768 ? v-65536 : v);
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("\n--- ESP32-C3 + BMI160 BLE BIN20 v1 ---");
  if (!Wire.begin(SDA_PIN, SCL_PIN, 100000)) {
    Serial.println("No se pudo iniciar I2C."); return;
  }
  Wire.setTimeOut(100);
  Serial.println("Buscando dispositivos I2C...");
  unsigned encontrados = 0;
  for (uint8_t a=8; a<120; a++) {
    Wire.beginTransmission(a);
    if (Wire.endTransmission() == 0) {
      Serial.printf("Respuesta en 0x%02X\n", a); encontrados++;
    }
  }
  if (!encontrados) {
    Serial.println("SIN RESPUESTA: revisar alimentacion, GND, SDA/SCL, soldaduras y CS alto.");
    return;
  }
  // Responder en 0x68 no basta: comprobar el identificador antes de escribir.
  for (uint8_t a=0x68; a<=0x69; a++) {
    uint8_t id=0;
    if (leerEn(a, 0x00, &id, 1)) {
      Serial.printf("Direccion 0x%02X | CHIP_ID=0x%02X (BMI160 esperado: 0xD1)\n", a, id);
      if (id==0xD1) {sensor=a; break;}
    }
  }
  if (!sensor) {
    Serial.println("No se identifico un BMI160. No se enviaron comandos de configuracion.");
    return;
  }
  if (!escribir(0x7E, 0xB6)) {Serial.println("Fallo de reset I2C"); return;}
  delay(100);
  if (!escribir(0x7E, 0x11)) {Serial.println("Fallo al activar acelerometro"); return;}
  delay(100);
  if (!escribir(0x7E, 0x15)) {Serial.println("Fallo al activar giroscopio"); return;}
  delay(100);
  // 100 Hz, filtrado normal; acelerometro +/-2g, giroscopio +/-2000 grados/s.
  if (!escribir(0x40, 0x28) || !escribir(0x41, 0x03) ||
      !escribir(0x42, 0x28) || !escribir(0x43, 0x00)) {
    Serial.println("Fallo escribiendo configuracion"); return;
  }
  uint8_t estado=0, error=0, config[4];
  if (!leerEn(sensor,0x03,&estado,1) || !leerEn(sensor,0x02,&error,1) ||
      !leerEn(sensor,0x40,config,4)) {
    Serial.println("Fallo leyendo estado/configuracion"); return;
  }
  Serial.printf("PMU_STATUS=0x%02X | ERR_REG=0x%02X\n", estado, error);
  if ((estado & 0x3C)!=0x14 || error!=0 || config[0]!=0x28 || config[1]!=0x03 || config[2]!=0x28 || config[3]!=0x00) {
    Serial.println("Configuracion inesperada. Comparte estos valores antes de continuar."); return;
  }
  beginBle();
  Serial.printf("BLE: %s. Busca y conecta desde el panel de sensores de la app.\n", DEVICE_NAME);
  listo=true;
  Serial.println("BMI160 identificado y configurado. Dejalo quieto y luego inclina/gira la placa.");
  Serial.println("A: aceleracion en g (incluye gravedad). G: velocidad de giro en grados/s.");
}

void loop() {
  if (!listo) {delay(500); return;}
  if (restartAdvertising.exchange(false)) {
    delay(300);
    if (!connected.load()) BLEDevice::startAdvertising();
  }
  uint32_t now = millis();
  if ((int32_t)(now-nextSampleAt)<0) { delay(1); return; }
  nextSampleAt=now+SAMPLE_PERIOD_MS; // 50 muestras/s nominales; timestamp real en cada paquete.
  uint8_t datos[12];
  // 0x0C..0x11: giroscopio; 0x12..0x17: acelerometro, orden little-endian.
  if (!leerEn(sensor,0x0C,datos,sizeof(datos))) {
    Serial.println("ERROR DE LECTURA I2C: revisar contacto/cables.");
    delay(500); return;
  }
  float gx=firmado(datos)/16.4f, gy=firmado(datos+2)/16.4f, gz=firmado(datos+4)/16.4f;
  float ax=firmado(datos+6)/16384.0f, ay=firmado(datos+8)/16384.0f, az=firmado(datos+10)/16384.0f;
  float magnitud=sqrtf(ax*ax+ay*ay+az*az);
  uint32_t capturedAt=millis();
  // BIN20 v1: uint32 secuencia, uint32 ms, int16 ax ay az gx gy gz.
  // Little-endian, unidades crudas originales, sin perder precision.
  // 20 bytes caben incluso con MTU ATT predeterminado de 23.
  uint8_t packet[20];
  putU32(packet, sequenceNumber++);
  putU32(packet+4, capturedAt);
  memcpy(packet+8, datos+6, 6);  // acelerometro
  memcpy(packet+14, datos, 6);  // giroscopio
  if (connected.load()) {
    dataCharacteristic->setValue(packet, sizeof(packet));
    dataCharacteristic->notify();
  }
  if(capturedAt-lastPrintAt>=200) {
    lastPrintAt=capturedAt;
  Serial.printf("A[g] X:%+.3f Y:%+.3f Z:%+.3f | |A|:%.3f | G[grados/s] X:%+.2f Y:%+.2f Z:%+.2f\n",
                ax,ay,az,magnitud,gx,gy,gz);
  }
}
