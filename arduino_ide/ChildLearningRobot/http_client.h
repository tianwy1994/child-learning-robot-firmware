#ifndef HTTP_CLIENT_H
#define HTTP_CLIENT_H

#include <Arduino.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "config.h"

class HttpClient {
public:
    void setToken(const String& token);
    String getToken();

    String get(const String& path, int& statusCode);
    String post(const String& path, const String& jsonBody, int& statusCode);

    int getBinary(const String& path, uint8_t* buffer, size_t maxSize);
    int postBinary(const String& path, const String& jsonBody, uint8_t* buffer, size_t maxSize);
    String postAudio(const String& path, const uint8_t* audioData, size_t audioSize, int& statusCode);

    bool isAuthenticated();
    void handleUnauthorized();

    typedef void (*UnauthorizedCallback)();
    void onUnauthorized(UnauthorizedCallback callback);

private:
    String _token;
    UnauthorizedCallback _unauthCallback = nullptr;

    String buildUrl(const String& path);
    String sendRequest(const String& method, const String& url, const String& body, int& statusCode);
};

#endif // HTTP_CLIENT_H
