#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <Arduino.h>
#include <NimBLEDevice.h>
#include "config.h"

/**
 * BLE 服务端 —— 接收移动端 APP 发送的设备 token。
 *
 * 工作流程：
 *   1. 启动 BLE 广播，等待 APP 连接
 *   2. APP 连接后写入 token 到特征值
 *   3. 收到 token 后通过回调通知 AuthManager
 */
class BleServer {
public:
    void begin(const String& deviceId);
    void stop();
    bool isAdvertising();

    // Token 接收回调
    typedef void (*TokenReceivedCallback)(const String& token);
    void onTokenReceived(TokenReceivedCallback callback);

private:
    NimBLEServer* _server = nullptr;
    NimBLEService* _service = nullptr;
    NimBLECharacteristic* _tokenChar = nullptr;
    bool _advertising = false;
    TokenReceivedCallback _tokenCallback = nullptr;

    // 回调类
    class ServerCallbacks : public NimBLEServerCallbacks {
    public:
        ServerCallbacks(BleServer* parent) : _parent(parent) {}
        void onConnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo) override;
        void onDisconnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo, int reason) override;
    private:
        BleServer* _parent;
    };

    class TokenCallbacks : public NimBLECharacteristicCallbacks {
    public:
        TokenCallbacks(BleServer* parent) : _parent(parent) {}
        void onWrite(NimBLECharacteristic* pCharacteristic, NimBLEConnInfo& connInfo) override;
    private:
        BleServer* _parent;
    };
};

#endif // BLE_SERVER_H
