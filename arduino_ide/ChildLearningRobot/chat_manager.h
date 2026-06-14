#ifndef CHAT_MANAGER_H
#define CHAT_MANAGER_H

#include <Arduino.h>
#include "config.h"

class HttpClient;

class ChatManager {
public:
    String sendMessage(HttpClient& httpClient, const String& message, ChatRole role = ROLE_COMPANION);
    String getSessionId();
    void resetSession();

private:
    String _sessionId = "";
};

#endif // CHAT_MANAGER_H
