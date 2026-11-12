-- =====================================================================
-- 企业内部问题登记小助手 · 数据库初始化脚本
-- MySQL 8.0+ / InnoDB / utf8mb4
--
-- 使用方式：手动执行一次，例如
--   mysql -uroot -p < src/main/resources/db/schema.sql
-- 不要配 spring.sql.init.mode=always，否则每次重启都会 DROP 表。
--
-- 设计约定：
--   1. 不建物理外键，以「索引 + 业务保证」维护关联；
--   2. 用户表逻辑删除，反馈主表逻辑删除，操作日志表只增不改；
--   3. 枚举统一存码值 + 列注释说明字典含义；
--   4. 字符集 utf8mb4，兼容 emoji 与中文标点。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `issue_helper`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE `issue_helper`;

-- ---------------------------------------------------------------------
-- 1. sys_user 用户表
--    单角色模型（role_code），不拆 role / user_role：
--    本系统权限点固定且少，多对多只会增加演示解释成本。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文(60 字符)',
    `real_name`   VARCHAR(50)  NOT NULL COMMENT '姓名(演示用)',
    `role_code`   VARCHAR(20)  NOT NULL COMMENT '角色:EMPLOYEE 员工 / SUPPORT 技术支持 / ADMIN 管理员(预留)',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态:0禁用 1启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role_code` (`role_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------------------------------------------------------------------
-- 2. feedback 反馈主表
--    关键约束：
--      uk_request_id        幂等最终防线：并发重复提交只有一条能插入成功
--      idx_status_type_time 覆盖看板"按状态+类型筛选并按时间倒序"
--      idx_submitter_status 覆盖员工端"我的反馈"
--      version              乐观锁，防两人同时改同一条
--    原文策略：content 存提交时原文，不做 trim 落库、不做摘要改写；
--              用于校验的规范化结果只存在于内存，不入库。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`
(
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `request_id`   VARCHAR(64)   NOT NULL COMMENT '提交编号(全局唯一,幂等键)',
    `submitter_id` BIGINT        NOT NULL COMMENT '提交人ID(sys_user.id)',
    `content`      TEXT          NOT NULL COMMENT '反馈原文,原样保存,不加工',
    `type_code`    VARCHAR(20)   NOT NULL COMMENT '类型:SYSTEM_FAULT/PERMISSION/FEATURE/CONSULT/UNDETERMINED',
    `type_source`  TINYINT       NOT NULL DEFAULT 1 COMMENT '类型来源:1提交者选择 2处理人复判',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '状态:0待处理 1处理中 2已完成',
    `handler_id`   BIGINT                 DEFAULT NULL COMMENT '当前处理人ID,未领取为空',
    `handler_note` VARCHAR(1000)          DEFAULT NULL COMMENT '内部处理备注,仅 SUPPORT/ADMIN 可见',
    `finish_time`  DATETIME               DEFAULT NULL COMMENT '完成时间,状态=2 时写入',
    `version`      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除:0正常 1已删除',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_id` (`request_id`) COMMENT '幂等键:相同编号不允许两条记录',
    KEY `idx_submitter_status` (`submitter_id`, `status`, `create_time`) COMMENT '员工端:我的反馈',
    KEY `idx_status_type_time` (`status`, `type_code`, `create_time`) COMMENT '支持端:看板筛选',
    KEY `idx_handler` (`handler_id`) COMMENT '支持端:我处理中的'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='反馈主表';

-- ---------------------------------------------------------------------
-- 3. feedback_operate_log 操作日志表（只增不改，无 update/delete 字段）
--    作用：让"谁在什么时候把状态从几改成几"可回溯。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `feedback_operate_log`;
CREATE TABLE `feedback_operate_log`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `feedback_id`   BIGINT      NOT NULL COMMENT '所属反馈ID',
    `operator_id`   BIGINT      NOT NULL COMMENT '操作人ID',
    `operator_role` VARCHAR(20) NOT NULL COMMENT '操作人角色快照',
    `operate_type`  TINYINT     NOT NULL COMMENT '操作事件:1提交 2开始处理 3完成 4类型复判 5备注更新',
    `from_status`   TINYINT              DEFAULT NULL COMMENT '变更前状态',
    `to_status`     TINYINT              DEFAULT NULL COMMENT '变更后状态',
    `from_type`     VARCHAR(20)          DEFAULT NULL COMMENT '变更前类型',
    `to_type`       VARCHAR(20)          DEFAULT NULL COMMENT '变更后类型',
    `remark`        VARCHAR(500)         DEFAULT NULL COMMENT '操作说明(内部,对员工不下发)',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_time` (`feedback_id`, `create_time`) COMMENT '按记录查时间线'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='反馈操作日志表';
