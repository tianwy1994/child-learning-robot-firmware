-- ============================================================
-- 趣味挑战题目数据表 (challenge_question)
-- 数据库: learning_robot
-- 用于存储魔法挑战乐园的所有题目，方便导出移库
-- ============================================================

CREATE DATABASE IF NOT EXISTS learning_robot
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE learning_robot;

DROP TABLE IF EXISTS challenge_question;

CREATE TABLE challenge_question (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(200)    NOT NULL                 COMMENT '题目标题',
    description     VARCHAR(500)    NOT NULL DEFAULT ''      COMMENT '题目描述',
    type            VARCHAR(30)     NOT NULL                 COMMENT '题目类型: click_select=点选, drag_to_slots=拖拽',
    difficulty      TINYINT         NOT NULL DEFAULT 1       COMMENT '难度: 1=简单, 2=中等, 3=困难',
    content         TEXT            NOT NULL                 COMMENT '题目内容/题干',
    answer          VARCHAR(500)    DEFAULT NULL             COMMENT '正确答案',
    explanation     VARCHAR(1000)   DEFAULT NULL             COMMENT '答案解析',
    exp_reward      INT             NOT NULL DEFAULT 10      COMMENT '经验值奖励',
    domain_key      VARCHAR(30)     NOT NULL                 COMMENT '领域标识: ENGLISH/MATH/SCIENCE/...',
    domain_name     VARCHAR(50)     NOT NULL                 COMMENT '领域中文名',
    domain_icon     VARCHAR(20)     NOT NULL DEFAULT ''      COMMENT '领域图标emoji',
    options         JSON            DEFAULT NULL             COMMENT '点选题选项列表(JSON数组)',
    page_ui_schema  JSON            DEFAULT NULL             COMMENT '拖拽题UI布局schema(JSON)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    INDEX idx_domain_key (domain_key),
    INDEX idx_type (type),
    INDEX idx_difficulty (difficulty),
    INDEX idx_domain_difficulty (domain_key, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='趣味挑战题目数据表';
