#ifndef STT_CLIENT_H
#define STT_CLIENT_H

#include <Arduino.h>
#include "../include/config.h"

class HttpClient;

/**
 * 语音识别客户端 —— 将录音音频发送到服务端进行语音转文字。
 *
 * 流程：
 *   1. 录音结束后，将 PCM 音频数据发送到服务端
 *   2. 服务端调用 STT 引擎（百度/腾讯等）识别语音
 *   3. 返回识别结果文本
 *
 * 服务端接口：POST /api/hardware/stt/recognize
 *   请求：binary PCM audio (16kHz 16-bit mono)
 *   响应：{ "code": 200, "data": { "text": "识别的文本" } }
 */
class SttClient {
public:
    /**
     * 发送音频数据进行语音识别。
     *
     * @param httpClient  HTTP 客户端（需已设置 token）
     * @param audioData   PCM 音频数据
     * @param audioSize   数据大小（字节）
     * @return 识别出的文本，失败返回空字符串
     */
    String recognize(HttpClient& httpClient, const uint8_t* audioData, size_t audioSize);
};

#endif // STT_CLIENT_H
