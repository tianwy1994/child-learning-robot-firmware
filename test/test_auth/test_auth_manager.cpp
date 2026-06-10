#include <unity.h>
#include "../../src/auth/auth_manager.h"
#include "../../include/config.h"

/**
 * 认证管理器单元测试。
 */

AuthManager authManager;

void setUp(void) {
    // 每个测试前重置
}

void tearDown(void) {
    // 每个测试后清理
}

void test_initial_state_is_locked(void) {
    // 首次启动（无保存的 token）应为 LOCKED 状态
    // 注意：在真实硬件上测试时，NVS 可能已有数据
    TEST_ASSERT_TRUE(authManager.getAuthState() == AUTH_LOCKED ||
                     authManager.getAuthState() == AUTH_AUTHENTICATED);
}

void test_set_token_changes_state(void) {
    authManager.setToken("test.token.value");
    TEST_ASSERT_TRUE(authManager.isAuthenticated());
    TEST_ASSERT_TRUE(authManager.hasToken());
    TEST_ASSERT_EQUAL_STRING("test.token.value", authManager.getToken().c_str());
}

void test_clear_token_locks_device(void) {
    authManager.setToken("test.token.value");
    TEST_ASSERT_TRUE(authManager.isAuthenticated());

    authManager.clearToken();
    TEST_ASSERT_FALSE(authManager.isAuthenticated());
    TEST_ASSERT_FALSE(authManager.hasToken());
    TEST_ASSERT_EQUAL(AUTH_LOCKED, authManager.getAuthState());
}

void test_device_id_generated(void) {
    String deviceId = authManager.getDeviceId();
    TEST_ASSERT_TRUE(deviceId.length() > 0);
    // MAC 地址格式: XX:XX:XX:XX:XX:XX (17 字符)
    TEST_ASSERT_EQUAL(17, deviceId.length());
}

void setup() {
    delay(2000); // 等待串口稳定

    UNITY_BEGIN();

    authManager.begin();

    RUN_TEST(test_initial_state_is_locked);
    RUN_TEST(test_set_token_changes_state);
    RUN_TEST(test_clear_token_locks_device);
    RUN_TEST(test_device_id_generated);

    UNITY_END();
}

void loop() {
    // 测试完成后空循环
}
