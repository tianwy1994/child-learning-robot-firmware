#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>
#include "../include/config.h"

/**
 * WiFi 连接管理器。
 */
class WifiManager {
public:
    void begin();
    void update();
    bool isConnected();
    String getLocalIP();
    int getRSSI();

    // 连接状态回调
    typedef void (*ConnectionCallback)(bool connected);
    void onConnectionChange(ConnectionCallback callback);

private:
    bool _connected = false;
    bool _connecting = false;
    unsigned long _lastRetry = 0;
    int _retryCount = 0;
    ConnectionCallback _connCallback = nullptr;

    void connect();
    void onConnected();
    void onDisconnected();
};

#endif // WIFI_MANAGER_H
