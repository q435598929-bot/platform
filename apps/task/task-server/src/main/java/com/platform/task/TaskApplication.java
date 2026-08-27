package com.platform.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 任务应用入口
 *
 * 各任务作为独立脚本运行，直接执行对应类的 main() 方法即可：
 *
 *   controller/dji/
 *     V2MerchantBasicdataModifyTask    - 商户基本信息修改
 *     V2MerchantBusiOpenTask           - 商户业务开通
 *     V2MerchantBusiConfigTask         - 微信商户配置
 *     V2SupplementaryPictureTask       - 图片上传
 *     V2MerchantBasicdataStatusQueryTask - 申请状态查询（读取 Excel apply_no 回写 apply_result）
 *
 *   controller/util/
 *     ExcelRecordService  - Excel 读写工具（线程安全）
 *     LoggerUtil          - 日志工具
 */
@SpringBootApplication(scanBasePackages = "com.platform.task")
@EntityScan("com.platform.task.platform.domain")
@EnableJpaRepositories("com.platform.task.platform.repository")
@EnableScheduling
public class TaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }
}
