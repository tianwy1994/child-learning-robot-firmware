#include "http_client.h"
#include <WiFi.h>

void HttpClient::setToken(const String& token) {
    _token = token;
}

String HttpClient::getToken() {
    return _token;
}

String HttpClient::get(const String& path, int& statusCode) {
    return sendRequest("GET", buildUrl(path), "", statusCode);
}

String HttpClient::post(const String& path, const String& jsonBody, int& statusCode) {
    return sendRequest("POST", buildUrl(path), jsonBody, statusCode);
}

int HttpClient::getBinary(const String& path, uint8_t* buffer, size_t maxSize) {
    return postBinary(path, "", buffer, maxSize);
}

int HttpClient::postBinary(const String& path, const String& jsonBody, uint8_t* buffer, size_t maxSize) {
    if (!WiFi.isConnected()) {
        Serial.println("[HTTP] WiFi 未连接，无法下载");
        return -1;
    }

    String url = buildUrl(path);
    HTTPClient http;
    http.begin(url);
    http.setTimeout(HTTP_TIMEOUT_MS);

    // 设置 Accept 为音频格式
    http.addHeader("Accept", "audio/pcm");
    if (!jsonBody.isEmpty()) {
        http.addHeader("Content-Type", "application/json");
    }

    // 自动携带 token
    if (!_token.isEmpty()) {
        http.addHeader("Authorization", "Bearer " + _token);
    }

    // 发送请求
    int statusCode;
    if (jsonBody.isEmpty()) {
        statusCode = http.GET();
    } else {
        statusCode = http.POST(jsonBody);
    }

    int bytesRead = 0;
    if (statusCode == 200) {
        // 流式读取二进制数据
        WiFiClient* stream = http.getStreamPtr();
        size_t totalBytes = http.getSize();

        if (totalBytes > 0 && totalBytes <= maxSize) {
            // 已知大小，一次性读取
            bytesRead = stream->readBytes(buffer, totalBytes);
        } else if (totalBytes > maxSize) {
            Serial.printf("[HTTP] 音频数据过大: %d > %d\n", totalBytes, maxSize);
            bytesRead = -1;
        } else {
            // 未知大小（chunked），分块读取
            size_t offset = 0;
            unsigned long timeout = millis();
            while (stream->available() && offset < maxSize) {
                int chunk = stream->readBytes(buffer + offset, min((size_t)256, maxSize - offset));
                offset += chunk;
                if (chunk > 0) timeout = millis();
                if (millis() - timeout > HTTP_TIMEOUT_MS) break;
            }
            bytesRead = offset;
        }

        Serial.printf("[HTTP] 二进制下载完成: %s, %d bytes\n", path.c_str(), bytesRead);
    } else if (statusCode == 401) {
        handleUnauthorized();
        Serial.printf("[HTTP] 二进制下载失败: 401, URL: %s\n", url.c_str());
    } else {
        Serial.printf("[HTTP] 二进制下载失败: %d, URL: %s\n", statusCode, url.c_str());
        bytesRead = -1;
    }

    http.end();
    return bytesRead;
}

bool HttpClient::isAuthenticated() {
    return !_token.isEmpty();
}

void HttpClient::handleUnauthorized() {
    Serial.println("[HTTP] 收到 401 响应，token 无效");
    if (_unauthCallback) {
        _unauthCallback();
    }
}

void HttpClient::onUnauthorized(UnauthorizedCallback callback) {
    _unauthCallback = callback;
}

String HttpClient::buildUrl(const String& path) {
    return String(SERVER_BASE_URL) + path;
}

String HttpClient::sendRequest(const String& method, const String& url, const String& body, int& statusCode) {
    if (!WiFi.isConnected()) {
        statusCode = -1;
        return "{\"error\":\"WiFi 未连接\"}";
    }

    HTTPClient http;
    http.begin(url);
    http.setTimeout(HTTP_TIMEOUT_MS);

    // 设置 Content-Type
    http.addHeader("Content-Type", "application/json");

    // 自动携带 token
    if (!_token.isEmpty()) {
        http.addHeader("Authorization", "Bearer " + _token);
    }

    // 发送请求
    if (method == "GET") {
        statusCode = http.GET();
    } else if (method == "POST") {
        statusCode = http.POST(body);
    } else {
        statusCode = http.sendRequest(method.c_str(), body);
    }

    String response = "";
    if (statusCode > 0) {
        response = http.getString();

        // 处理 401 响应
        if (statusCode == 401) {
            handleUnauthorized();
        }
    } else {
        response = "{\"error\":\"请求失败: " + http.errorToString(statusCode) + "\"}";
        Serial.printf("[HTTP] 请求失败: %s, URL: %s\n", http.errorToString(statusCode).c_str(), url.c_str());
    }

    http.end();
    return response;
}
