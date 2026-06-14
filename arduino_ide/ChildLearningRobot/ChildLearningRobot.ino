/**
 * 儿童学习陪伴机器人 v2.0 — 主程序
 *
 * 功能概述：
 *   1. BLE 配网（APP 发送 token）
 *   2. WiFi 连接 + 自动重连
 *   3. 每日打卡（开机自动）
 *   4. 专注模式（按钮触发，30秒轮询状态+提醒）
 *   5. 语音聊天（长按录音 → STT → AI 对话 → TTS 播放）
 *   6. RGB LED 状态指示
 *
 * 按钮交互：
 *   - 短按（<1秒）：开始/结束专注
 *   - 长按（>3秒）：开始语音录音，松开后发送
 *
 * 状态机：
 *   BOOT → IDLE ⟷ FOCUSING
 *              ↕       ↕
 *          LISTENING → PROCESSING → SPEAKING
 */

#include "config.h"
#include "auth_manager.h"
#include "ble_server.h"
#include "wifi_manager.h"
#include "http_client.h"
#include "stt_client.h"
#include "display_manager.h"
#include "audio_player.h"
#include "mic_manager.h"
#include "button_manager.h"
#include "chat_manager.h"
#include "focus_manager.h"
#include "game_manager.h"

// ============================================================
// 全局对象
// ============================================================
AuthManager authManager;
BleServer bleServer;
WifiManager wifiManager;
HttpClient httpClient;
SttClient sttClient;
DisplayManager displayManager;
AudioPlayer audioPlayer;
MicManager micManager;
ButtonManager buttonManager;
ChatManager chatManager;
FocusManager focusManager;
GameManager gameManager;

// ============================================================
// 状态变量
// ============================================================
DeviceState currentState = STATE_BOOT;
DeviceState prevState = STATE_IDLE;
bool greetingPlayed = false;
bool checkedIn = false;
bool wifiWasConnected = false;

// ============================================================
// 回调函数
// ============================================================
void onTokenReceived(const String& token) {
    Serial.println("[Main] 收到新 token");
    authManager.setToken(token);
    httpClient.setToken(token);
    displayManager.showAuthenticated();
}

void onAuthStateChanged(AuthState newState) {
    switch (newState) {
        case AUTH_LOCKED:        displayManager.showLocked(); break;
        case AUTH_AUTHENTICATED: displayManager.showAuthenticated(); httpClient.setToken(authManager.getToken()); break;
        case AUTH_EXPIRED:       displayManager.showExpired(); break;
    }
}

void onWiFiConnected(bool connected) {
    if (connected) {
        displayManager.showWiFiConnected();
        Serial.printf("[Main] WiFi 已连接，IP: %s\n", wifiManager.getLocalIP().c_str());
        if (authManager.isAuthenticated() && !greetingPlayed) {
            greetingPlayed = true;
            audioPlayer.playPreset(httpClient, "greeting");
        }
    } else {
        displayManager.showWiFiConnecting();
    }
}

void onUnauthorized() {
    Serial.println("[Main] token 无效，清除 token");
    authManager.clearToken();
    displayManager.showLocked();
}

// ============================================================
// 状态切换
// ============================================================
void setState(DeviceState newState) {
    if (currentState != newState) {
        prevState = currentState;
        currentState = newState;
        Serial.printf("[State] %d → %d\n", prevState, newState);
        switch (newState) {
            case STATE_BOOT:         displayManager.showStarting(); break;
            case STATE_IDLE:         authManager.isAuthenticated() ? displayManager.showAuthenticated() : displayManager.showLocked(); break;
            case STATE_FOCUSING:     displayManager.showDeviceState(STATE_FOCUSING); break;
            case STATE_FOCUS_BREAK:  displayManager.showDeviceState(STATE_FOCUS_BREAK); break;
            case STATE_LISTENING:    displayManager.showDeviceState(STATE_LISTENING); break;
            case STATE_PROCESSING:   displayManager.showDeviceState(STATE_PROCESSING); break;
            case STATE_SPEAKING:     displayManager.showDeviceState(STATE_SPEAKING); break;
            case STATE_ERROR:        displayManager.showDeviceState(STATE_ERROR); break;
        }
    }
}

// ============================================================
// 按钮回调
// ============================================================
void onButtonShortPress() {
    Serial.printf("[Button] 短按，当前状态: %d\n", currentState);
    switch (currentState) {
        case STATE_IDLE:
            if (wifiManager.isConnected() && authManager.isAuthenticated()) {
                setState(STATE_FOCUSING);
                if (focusManager.startFocus(httpClient)) {
                    audioPlayer.playText(httpClient, "专注模式已开始，加油学习吧！");
                } else {
                    audioPlayer.playText(httpClient, "专注模式启动失败，请检查网络。");
                    setState(STATE_IDLE);
                }
            } else {
                audioPlayer.playPreset(httpClient, "greeting");
            }
            break;
        case STATE_FOCUSING:
            if (focusManager.endFocus(httpClient)) {
                audioPlayer.playText(httpClient, "专注结束，辛苦了！休息一下吧。");
            }
            setState(STATE_IDLE);
            break;
        case STATE_FOCUS_BREAK:
            audioPlayer.playText(httpClient, "休息结束，继续加油！");
            setState(STATE_FOCUSING);
            break;
        default: break;
    }
}

void onButtonLongPress() {
    Serial.printf("[Button] 长按，当前状态: %d\n", currentState);
    if (currentState != STATE_LISTENING && currentState != STATE_PROCESSING && currentState != STATE_SPEAKING) {
        if (!wifiManager.isConnected() || !authManager.isAuthenticated()) {
            audioPlayer.playPreset(httpClient, "greeting");
            return;
        }
        if (audioPlayer.isPlaying()) audioPlayer.stop();
        if (micManager.startRecording()) {
            setState(STATE_LISTENING);
        } else {
            Serial.println("[Main] 录音启动失败");
        }
    }
}

void onButtonReleased() {
    Serial.printf("[Button] 松开，当前状态: %d\n", currentState);
    if (currentState == STATE_LISTENING) {
        size_t recordedSize = 0;
        micManager.stopRecording(recordedSize);
        if (recordedSize > AUDIO_SAMPLE_RATE * 2) {  // 至少 1 秒
            setState(STATE_PROCESSING);
        } else {
            Serial.println("[Main] 录音太短，忽略");
            setState(focusManager.isFocusing() ? STATE_FOCUSING : STATE_IDLE);
        }
    }
}

// ============================================================
// 语音处理
// ============================================================
void processVoiceInput(const uint8_t* audioData, size_t audioSize) {
    // Step 1: 语音识别
    Serial.println("[Voice] 步骤1: 语音识别...");
    String recognizedText = sttClient.recognize(httpClient, audioData, audioSize);
    if (recognizedText.isEmpty()) {
        audioPlayer.playText(httpClient, "抱歉，我没有听清楚，请再说一次。");
        setState(focusManager.isFocusing() ? STATE_FOCUSING : STATE_IDLE);
        return;
    }
    Serial.printf("[Voice] 识别结果: %s\n", recognizedText.c_str());

    // Step 2: AI 对话
    Serial.println("[Voice] 步骤2: AI 对话...");
    String aiReply = chatManager.sendMessage(httpClient, recognizedText, ROLE_COMPANION);
    if (aiReply.isEmpty()) {
        audioPlayer.playText(httpClient, "哎呀，我走神了，请再说一次。");
        setState(focusManager.isFocusing() ? STATE_FOCUSING : STATE_IDLE);
        return;
    }
    Serial.printf("[Voice] AI 回复: %s\n", aiReply.c_str());

    // Step 3: TTS 播放
    Serial.println("[Voice] 步骤3: TTS 播放...");
    setState(STATE_SPEAKING);
    audioPlayer.playText(httpClient, aiReply);
}

// ============================================================
// setup()
// ============================================================
void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n================================");
    Serial.println("  儿童学习陪伴机器人 v2.0");
    Serial.println("================================");

    displayManager.begin();
    displayManager.showStarting();

    buttonManager.begin();
    buttonManager.onShortPress(onButtonShortPress);
    buttonManager.onLongPress(onButtonLongPress);
    buttonManager.onReleased(onButtonReleased);

    authManager.begin();
    authManager.onStateChange(onAuthStateChanged);

    Serial.printf("[Main] 设备 ID: %s\n", authManager.getDeviceId().c_str());
    Serial.printf("[Main] 认证状态: %s\n", authManager.isAuthenticated() ? "已认证" : "未认证");

    bleServer.begin(authManager.getDeviceId());
    bleServer.onTokenReceived(onTokenReceived);

    wifiManager.begin();
    wifiManager.onConnectionChange(onWiFiConnected);

    httpClient.onUnauthorized(onUnauthorized);
    if (authManager.isAuthenticated()) httpClient.setToken(authManager.getToken());

    audioPlayer.begin();
    micManager.begin();

    if (authManager.isAuthenticated()) displayManager.showAuthenticated();
    else displayManager.showLocked();

    setState(STATE_IDLE);
    Serial.println("[Main] 初始化完成");
    Serial.println("================================\n");
}

// ============================================================
// loop()
// ============================================================
void loop() {
    buttonManager.update();
    authManager.update();
    wifiManager.update();
    displayManager.update();
    audioPlayer.update();

    // WiFi 刚连接时：每日打卡
    if (wifiManager.isConnected() && !wifiWasConnected) {
        wifiWasConnected = true;
        if (authManager.isAuthenticated() && !checkedIn) {
            checkedIn = gameManager.dailyCheckin(httpClient, audioPlayer);
        }
    }
    if (!wifiManager.isConnected()) wifiWasConnected = false;

    // === 状态机 ===
    switch (currentState) {
        case STATE_IDLE:
            if (focusManager.isFocusing()) {
                focusManager.pollStatus(httpClient, audioPlayer);
                if (!focusManager.isFocusing()) setState(STATE_IDLE);
            }
            break;

        case STATE_FOCUSING:
            if (focusManager.isFocusing()) {
                focusManager.pollStatus(httpClient, audioPlayer);
                if (focusManager.isInBreak()) setState(STATE_FOCUS_BREAK);
            } else {
                setState(STATE_IDLE);
            }
            break;

        case STATE_FOCUS_BREAK:
            focusManager.pollStatus(httpClient, audioPlayer);
            if (!focusManager.isInBreak() && focusManager.isFocusing()) setState(STATE_FOCUSING);
            else if (!focusManager.isFocusing()) setState(STATE_IDLE);
            break;

        case STATE_LISTENING:
            micManager.update();
            if (micManager.isBufferFull()) {
                Serial.println("[Main] 录音缓冲区已满，自动停止");
                onButtonReleased();
            }
            break;

        case STATE_PROCESSING: {
            const uint8_t* audioData = micManager.getRecordedData();
            size_t recordedSize = micManager.getRecordedSize();
            if (audioData && recordedSize > 0) {
                processVoiceInput(audioData, recordedSize);
            } else {
                setState(focusManager.isFocusing() ? STATE_FOCUSING : STATE_IDLE);
            }
            break;
        }

        case STATE_SPEAKING:
            if (!audioPlayer.isPlaying()) {
                Serial.println("[Main] 语音播放完成");
                setState(focusManager.isFocusing() ? STATE_FOCUSING : STATE_IDLE);
            }
            break;

        default: break;
    }

    delay(10);
}
