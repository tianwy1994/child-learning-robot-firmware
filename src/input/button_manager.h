#ifndef BUTTON_MANAGER_H
#define BUTTON_MANAGER_H

#include <Arduino.h>
#include "../include/config.h"

/**
 * 按钮管理器 —— 处理 GPIO 0 按钮的消抖、短按和长按检测。
 *
 * 工作流程：
 *   1. 持续读取按钮引脚电平
 *   2. 消抖处理（默认 50ms）
 *   3. 检测短按（< 1s）和长按（> 3s）
 *   4. 通过回调通知上层逻辑
 */
class ButtonManager {
public:
    void begin();
    void update();

    // 按钮事件回调
    typedef void (*ButtonEventCallback)();
    void onShortPress(ButtonEventCallback callback);
    void onLongPress(ButtonEventCallback callback);
    void onPressed(ButtonEventCallback callback);     // 按下瞬间
    void onReleased(ButtonEventCallback callback);    // 松开瞬间

    bool isPressed();       // 当前是否按下
    unsigned long pressDuration();  // 当前按住时长（ms）

private:
    bool _lastRawState = HIGH;      // 上次原始电平
    bool _debouncedState = HIGH;    // 消抖后电平
    unsigned long _lastDebounceTime = 0;
    unsigned long _pressStartTime = 0;
    bool _longPressFired = false;   // 长按回调是否已触发

    ButtonEventCallback _shortPressCb = nullptr;
    ButtonEventCallback _longPressCb = nullptr;
    ButtonEventCallback _pressedCb = nullptr;
    ButtonEventCallback _releasedCb = nullptr;
};

#endif // BUTTON_MANAGER_H
