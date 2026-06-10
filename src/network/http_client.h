#ifndef HTTP_CLIENT_H
#define HTTP_CLIENT_H

#include <Arduino.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "../include/config.h"

/**
 * HTTP 客户端封装 —— 自动携带设备 token。
 *
 * 所有请求自动附加 Authorization: Bearer <token> header。
 * 收到 401 响应时通知调用方 token 无效。
 */
class HttpClient {
public:
    void setToken(const String& token);
    String getToken();

    // HTTP 请求方法
    String get(const String& path, int& statusCode);
    String post(const String& path, const String& jsonBody, int& statusCode);

    // 二进制下载（音频等）
    int getBinary(const String& path, uint8_t* buffer, size_t maxSize);
    int postBinary(const String& path, const String& jsonBody, uint8_t* buffer, size_t maxSize);

    // 特殊方法
    bool isAuthenticated();
    void handleUnauthorized();

    // Token 失效回调
    typedef void (*UnauthorizedCallback)();
    void onUnauthorized(UnauthorizedCallback callback);

private:
    String _token;
    UnauthorizedCallback _unauthCallback = nullptr;

    String buildUrl(const String& path);
    String sendRequest(const String& method, const String& url, const String& body, int& statusCode);
};

#endif // HTTP_CLIENT_H
