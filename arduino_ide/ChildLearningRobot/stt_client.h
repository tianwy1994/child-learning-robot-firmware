#ifndef STT_CLIENT_H
#define STT_CLIENT_H

#include <Arduino.h>
#include "config.h"

class HttpClient;

class SttClient {
public:
    String recognize(HttpClient& httpClient, const uint8_t* audioData, size_t audioSize);
};

#endif // STT_CLIENT_H
