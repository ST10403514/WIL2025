// main.ino
// ESP32 (Arduino core) + MPU-6050 + NimBLE-Arduino (Nordic UART-style) gyro streamer with robust state machine
// Pins: SDA=GPIO21, SCL=GPIO22 (default Wire on ESP32). Change via Wire.begin(SDA, SCL) if needed.
// Libraries: Wire (built-in), NimBLE-Arduino, no external MPU library (tiny inline MPU6050 driver used).
// UUIDs: Nordic UART Service (NUS)
//  - Service:       6E400001-B5A3-F393-E0A9-E50E24DCCA9E
//  - RX Char (W):   6E400002-B5A3-F393-E0A9-E50E24DCCA9E
//  - TX Char (N):   6E400003-B5A3-F393-E0A9-E50E24DCCA9E

#include <Arduino.h>
#include <Wire.h>
#include <NimBLEDevice.h>

// ============================= Constants / Config =============================
static const char* DEVNAME_BOOT = "ESP32-GYRO-OFF";

static const uint8_t I2C_ADDR = 0x68;
static const uint32_t I2C_TIMEOUT_MS = 100;

static const uint16_t DEFAULT_RATE_HZ = 100;   // sampling (10–200)
static const uint16_t TX_HZ = 10;              // BLE transmit downsample
static const size_t   RING_SIZE = 256;         // buffered samples while disconnected

// MPU-6050 registers (subset)
static const uint8_t REG_PWR_MGMT_1   = 0x6B;
static const uint8_t REG_SMPLRT_DIV   = 0x19;
static const uint8_t REG_CONFIG       = 0x1A;
static const uint8_t REG_GYRO_CONFIG  = 0x1B;
static const uint8_t REG_ACCEL_CONFIG = 0x1C;
static const uint8_t REG_WHO_AM_I     = 0x75;
static const uint8_t REG_ACCEL_XOUT_H = 0x3B;

// Nordic UART UUIDs
static const char* UUID_NUS_SERVICE =       "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
static const char* UUID_NUS_CHAR_RX =       "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"; // Write
static const char* UUID_NUS_CHAR_TX =       "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"; // Notify

// ============================= State Machine =============================
enum SysState {
  BLE_DISCONNECTED,
  BLE_CONNECTED_IDLE,
  GYRO_OFF,
  GYRO_ON
};

static SysState state = BLE_DISCONNECTED;

// ============================= BLE Globals =============================
static NimBLEServer *g_server = nullptr;
static NimBLECharacteristic *g_tx = nullptr;
static NimBLECharacteristic *g_rx = nullptr;
static bool g_connected = false;

// ============================= Gyro driver + sampling =============================
static bool gyro_present = false;
static bool gyro_fail_reported = false;
static uint16_t rate_hz = DEFAULT_RATE_HZ; // sample rate
static uint32_t sample_period_ms = 1000 / DEFAULT_RATE_HZ;
static uint32_t next_sample_ms = 0;
static uint32_t next_tx_ms = 0;
static uint32_t tx_period_ms = 1000 / TX_HZ;
static uint32_t gyro_error_count = 0;

static bool calibrating = false;
static const uint16_t CAL_SAMPLES = 200;
static uint16_t cal_count = 0;
static double cal_sum_ax = 0, cal_sum_ay = 0, cal_sum_az = 0;
static double cal_sum_gx = 0, cal_sum_gy = 0, cal_sum_gz = 0;

static float bias_ax = 0, bias_ay = 0, bias_az = 0;
static float bias_gx = 0, bias_gy = 0, bias_gz = 0;

// Sample struct and ring buffer for disconnected buffering
struct Sample {
  uint32_t t;
  float ax, ay, az;
  float gx, gy, gz;
  float mm;
};
static Sample ring[RING_SIZE];
static size_t ring_head = 0;
static size_t ring_count = 0;

static void ringPush(const Sample &s) {
  ring[ring_head] = s;
  ring_head = (ring_head + 1) % RING_SIZE;
  if (ring_count < RING_SIZE) ring_count++;
}

static bool ringPop(Sample &out) {
  if (ring_count == 0) return false;
  size_t idx = (ring_head + RING_SIZE - ring_count) % RING_SIZE;
  out = ring[idx];
  ring_count--;
  return true;
}

// ============================= MPU6050 Driver (minimal) =============================
static bool i2cWrite8(uint8_t reg, uint8_t val) {
  Wire.beginTransmission(I2C_ADDR);
  Wire.write(reg);
  Wire.write(val);
  return (Wire.endTransmission() == 0);
}

static bool i2cRead8(uint8_t reg, uint8_t &val) {
  Wire.beginTransmission(I2C_ADDR);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom((int)I2C_ADDR, 1) != 1) return false;
  val = Wire.read();
  return true;
}

static bool i2cReadBlock(uint8_t reg, uint8_t *buf, size_t len) {
  Wire.beginTransmission(I2C_ADDR);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) return false;
  size_t r = Wire.requestFrom((int)I2C_ADDR, (int)len);
  if (r != len) return false;
  for (size_t i = 0; i < len; ++i) buf[i] = Wire.read();
  return true;
}

static bool mpuWake() {
  uint8_t v;
  if (!i2cRead8(REG_PWR_MGMT_1, v)) return false;
  v &= ~(1 << 6); // sleep=0
  v &= ~0x7;
  v |= 0x01;      // clock PLL
  return i2cWrite8(REG_PWR_MGMT_1, v);
}

static bool mpuSleep() {
  uint8_t v;
  if (!i2cRead8(REG_PWR_MGMT_1, v)) return false;
  v |= (1 << 6); // sleep=1
  return i2cWrite8(REG_PWR_MGMT_1, v);
}

static bool mpuConfigRate(uint16_t hz) {
  if (!i2cWrite8(REG_CONFIG, 3)) return false; // DLPF=42 Hz
  uint16_t clamp_hz = hz;
  if (clamp_hz < 10) clamp_hz = 10;
  if (clamp_hz > 200) clamp_hz = 200;
  uint8_t div = (uint8_t)((1000 / clamp_hz) - 1);
  if (div > 255) div = 255;
  if (!i2cWrite8(REG_SMPLRT_DIV, div)) return false;
  if (!i2cWrite8(REG_GYRO_CONFIG, (3 << 3))) return false;  // ±2000 dps
  if (!i2cWrite8(REG_ACCEL_CONFIG, (2 << 3))) return false; // ±8g
  rate_hz = clamp_hz;
  sample_period_ms = 1000 / rate_hz;
  tx_period_ms = 1000 / TX_HZ;
  return true;
}

static bool mpuReadAccelGyro(float &ax, float &ay, float &az, float &gx, float &gy, float &gz) {
  uint8_t raw[14];
  if (!i2cReadBlock(REG_ACCEL_XOUT_H, raw, sizeof(raw))) return false;
  int16_t rax = (int16_t)((raw[0] << 8) | raw[1]);
  int16_t ray = (int16_t)((raw[2] << 8) | raw[3]);
  int16_t raz = (int16_t)((raw[4] << 8) | raw[5]);
  int16_t rgx = (int16_t)((raw[8] << 8) | raw[9]);
  int16_t rgy = (int16_t)((raw[10] << 8) | raw[11]);
  int16_t rgz = (int16_t)((raw[12] << 8) | raw[13]);
  ax = (float)rax / 4096.0f; ay = (float)ray / 4096.0f; az = (float)raz / 4096.0f;
  gx = (float)rgx / 16.4f;   gy = (float)rgy / 16.4f;   gz = (float)rgz / 16.4f;
  ax -= bias_ax; ay -= bias_ay; az -= bias_az;
  gx -= bias_gx; gy -= bias_gy; gz -= bias_gz;
  return true;
}

// ============================= Utilities =============================
static void txJSON(const char *line) {
  if (!g_connected || !g_tx) return;
  g_tx->setValue((uint8_t*)line, strlen(line));
  g_tx->notify();
}

static void txJSONf(const char *fmt, ...) {
  if (!g_connected || !g_tx) return;
  static char buf[192];
  va_list ap; va_start(ap, fmt);
  vsnprintf(buf, sizeof(buf), fmt, ap);
  va_end(ap);
  g_tx->setValue((uint8_t*)buf, strlen(buf));
  g_tx->notify();
}

static void statusJSON(const char* key, const char* val) {
  static char buf[128];
  snprintf(buf, sizeof(buf), "{\"%s\":\"%s\"}", key, val);
  Serial.println(buf);
  txJSON(buf);
}

static void ackJSONRate(uint16_t hz) {
  static char buf[64];
  snprintf(buf, sizeof(buf), "{\"rate\":%u}", hz);
  Serial.println(buf);
  txJSON(buf);
}

static void pongJSON() {
  static const char* s = "{\"pong\":true}";
  Serial.println(s);
  txJSON(s);
}

// ============================= BLE Callbacks =============================
class ServerCB : public NimBLEServerCallbacks {
  void onConnect(NimBLEServer* pServer) override {
    g_connected = true;
    state = BLE_CONNECTED_IDLE;
  }
  void onDisconnect(NimBLEServer* pServer) override {
    g_connected = false;
    state = BLE_DISCONNECTED;
    NimBLEDevice::startAdvertising();
  }
};

class RxCB : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic* ch) override {
    std::string rx = ch->getValue();
    if (rx.empty()) return;
    String s = String(rx.c_str()); s.trim(); s.toUpperCase();
    if (s == "PING") { pongJSON(); return; }
    if (s == "ON") {
      if (!gyro_present) { statusJSON("error","gyro_not_found"); return; }
      if (!mpuWake() || !mpuConfigRate(rate_hz)) { statusJSON("error","gyro_fail"); return; }
      next_sample_ms = millis(); next_tx_ms = millis();
      state = GYRO_ON; statusJSON("state","GYRO_ON"); return;
    }
    if (s == "OFF") {
      if (gyro_present) mpuSleep();
      state = GYRO_OFF; statusJSON("state","GYRO_OFF"); return;
    }
    if (s == "CAL") {
      if (!gyro_present) { statusJSON("error","gyro_not_found"); return; }
      calibrating = true; cal_count=0;
      cal_sum_ax=cal_sum_ay=cal_sum_az=0; cal_sum_gx=cal_sum_gy=cal_sum_gz=0;
      statusJSON("cal","started"); return;
    }
    if (s.startsWith("RATE")) {
      int sp = s.indexOf(' ');
      if (sp > 0 && sp+1 < (int)s.length()) {
        int hz = s.substring(sp+1).toInt();
        if (hz < 10) hz = 10; if (hz > 200) hz = 200;
        if (!gyro_present) { rate_hz = hz; sample_period_ms=1000/rate_hz; ackJSONRate(rate_hz); }
        else { if (mpuConfigRate(hz)) ackJSONRate(rate_hz); else statusJSON("error","rate_set_fail"); }
      } else { ackJSONRate(rate_hz); }
      return;
    }
    statusJSON("error","bad_cmd");
  }
};

// ============================= Setup & Loop =============================
static void bleInit() {
  NimBLEDevice::init(DEVNAME_BOOT);
  NimBLEDevice::setPower(ESP_PWR_LVL_P7);
  NimBLEDevice::setSecurityAuth(false,false,true);
  g_server = NimBLEDevice::createServer();
  g_server->setCallbacks(new ServerCB());
  NimBLEService* svc = g_server->createService(UUID_NUS_SERVICE);
  g_tx = svc->createCharacteristic(UUID_NUS_CHAR_TX, NIMBLE_PROPERTY::NOTIFY);
  g_rx = svc->createCharacteristic(UUID_NUS_CHAR_RX, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR);
  g_rx->setCallbacks(new RxCB());
  svc->start();
  NimBLEAdvertising* adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(UUID_NUS_SERVICE);
  adv->setScanResponse(true);
  adv->start();
  Serial.println("{\"boot\":\"GYRO:OFF\"}");
}

static void i2cInit() {
  Wire.setTimeOut(I2C_TIMEOUT_MS);
  Wire.begin();
  delay(10);
  uint8_t who=0; uint32_t t0=millis(); bool ok=false;
  while (millis()-t0 < I2C_TIMEOUT_MS) { if (i2cRead8(REG_WHO_AM_I, who)) { ok=true; break; } delay(2);} 
  if (!ok) { gyro_present=false; Serial.println("{\"info\":\"GYRO:NOT_FOUND\"}"); }
  else { gyro_present = (who == I2C_ADDR); if (!gyro_present) Serial.println("{\"info\":\"GYRO:NOT_FOUND\"}"); }
  if (gyro_present) { mpuWake(); mpuConfigRate(DEFAULT_RATE_HZ); mpuSleep(); }
}

void setup() {
  Serial.begin(115200); delay(50);
  Serial.println("READY GYRO:OFF");
  i2cInit();
  bleInit();
  state = BLE_DISCONNECTED;
  next_sample_ms = millis(); next_tx_ms = millis();
}

static void processCalibration(float ax, float ay, float az, float gx, float gy, float gz) {
  if (!calibrating) return;
  cal_sum_ax += ax; cal_sum_ay += ay; cal_sum_az += az;
  cal_sum_gx += gx; cal_sum_gy += gy; cal_sum_gz += gz;
  if (++cal_count >= CAL_SAMPLES) {
    bias_ax = (float)(cal_sum_ax / CAL_SAMPLES);
    bias_ay = (float)(cal_sum_ay / CAL_SAMPLES);
    bias_az = (float)(cal_sum_az / CAL_SAMPLES);
    bias_gx = (float)(cal_sum_gx / CAL_SAMPLES);
    bias_gy = (float)(cal_sum_gy / CAL_SAMPLES);
    bias_gz = (float)(cal_sum_gz / CAL_SAMPLES);
    calibrating = false;
    Serial.println("{\"cal\":\"ok\"}");
    txJSON("{\"cal\":\"ok\"}");
  }
}

static void streamOne(const Sample &s) {
  char buf[192];
  int n = snprintf(buf, sizeof(buf),
    "{\"t\":%lu,\"ax\":%.3f,\"ay\":%.3f,\"az\":%.3f,\"gx\":%.2f,\"gy\":%.2f,\"gz\":%.2f,\"mm\":%.3f}",
    (unsigned long)s.t, s.ax, s.ay, s.az, s.gx, s.gy, s.gz, s.mm);
  if (n > 0 && n < (int)sizeof(buf)) {
    if (g_connected) { g_tx->setValue((uint8_t*)buf, n); g_tx->notify(); }
  }
}

static void sampleTick() {
  if (state != GYRO_ON || !gyro_present) return;
  uint32_t now = millis();
  if (now < next_sample_ms) return;
  next_sample_ms += sample_period_ms;
  float ax,ay,az,gx,gy,gz;
  if (!mpuReadAccelGyro(ax,ay,az,gx,gy,gz)) {
    if (++gyro_error_count > 250) {
      if (!gyro_fail_reported) { statusJSON("error","gyro_fail"); gyro_fail_reported=true; }
      if (gyro_present) mpuSleep();
      state = GYRO_OFF;
    }
    return;
  }
  gyro_error_count = 0;
  float mm = sqrtf(gx*gx + gy*gy + gz*gz);
  Sample s{now, ax, ay, az, gx, gy, gz, mm};
  processCalibration(ax,ay,az,gx,gy,gz);
  ringPush(s);
  if (now >= next_tx_ms) {
    next_tx_ms += tx_period_ms;
    Sample latest; while (ring_count > 1) { Sample d; ringPop(d);} if (ringPop(latest)) streamOne(latest);
  }
}

static void tickState() {
  if (!g_connected) {
    if (state != BLE_DISCONNECTED) state = BLE_DISCONNECTED;
  } else {
    if (state == BLE_DISCONNECTED) state = BLE_CONNECTED_IDLE;
  }
}

void loop() {
  tickState();
  sampleTick();
  delay(0);
}
