#include "ble_server.h"

void BleServer::begin(const String& deviceId) {
    Serial.println("[BLE] 初始化 BLE 服务...");

    // 初始化 NimBLE
    NimBLEDevice::init(BLE_DEVICE_NAME);
    NimBLEDevice::setPower(9);

    // 创建服务器
    _server = NimBLEDevice::createServer();
    _server->setCallbacks(new ServerCallbacks(this));

    // 创建服务
    _service = _server->createService(BLE_SERVICE_UUID);

    // 创建 token 写入特征值
    _tokenChar = _service->createCharacteristic(
        BLE_TOKEN_CHAR_UUID,
        NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR
    );
    _tokenChar->setCallbacks(new TokenCallbacks(this));

    // 启动服务
    _service->start();

    // 配置广播
    NimBLEAdvertising* advertising = NimBLEDevice::getAdvertising();
    advertising->addServiceUUID(BLE_SERVICE_UUID);
    advertising->enableScanResponse(true);
    advertising->setPreferredParams(0x06, 0x12);

    // 开始广播
    NimBLEDevice::startAdvertising();
    _advertising = true;

    Serial.println("[BLE] BLE 服务已启动，等待连接...");
    Serial.printf("[BLE] 设备名称: %s\n", BLE_DEVICE_NAME);
    Serial.printf("[BLE] 服务 UUID: %s\n", BLE_SERVICE_UUID);
}

void BleServer::stop() {
    NimBLEDevice::stopAdvertising();
    _advertising = false;
    Serial.println("[BLE] BLE 广播已停止");
}

bool BleServer::isAdvertising() {
    return _advertising;
}

void BleServer::onTokenReceived(TokenReceivedCallback callback) {
    _tokenCallback = callback;
}

// ServerCallbacks 实现
void BleServer::ServerCallbacks::onConnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo) {
    Serial.println("[BLE] 设备已连接");
    _parent->_advertising = false;
}

void BleServer::ServerCallbacks::onDisconnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo, int reason) {
    Serial.println("[BLE] 设备已断开，重新开始广播");
    NimBLEDevice::startAdvertising();
    _parent->_advertising = true;
}

// TokenCallbacks 实现
void BleServer::TokenCallbacks::onWrite(NimBLECharacteristic* pCharacteristic, NimBLEConnInfo& connInfo) {
    String value = pCharacteristic->getValue();
    Serial.printf("[BLE] 收到 token，长度: %d\n", value.length());

    if (value.length() > 0) {
        Serial.printf("[BLE] token 前20字符: %s...\n", value.substring(0, min(20, (int)value.length())).c_str());

        if (_parent->_tokenCallback) {
            _parent->_tokenCallback(value);
        }
    }
}
