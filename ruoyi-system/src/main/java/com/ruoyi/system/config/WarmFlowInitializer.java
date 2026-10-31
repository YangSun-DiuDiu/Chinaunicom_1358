package com.ruoyi.system.config;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * warm-flow 流程定义初始化
 *
 * <p>应用启动后检查「访客预约审批」流程定义（visitor_appointment）是否已发布：
 * 未发布则通过 importJson 导入流程定义并 publish。幂等：重复启动不会重复建定义。</p>
 *
 * <p>流程结构：start(发起,0) → approve(审批,BETWEEN=1) → end(结束,2)</p>
 * <ul>
 *   <li>approve 节点仅配置 PASS 跳过（通过 = skip 流转至 end）</li>
 *   <li>拒绝走 TaskService.terminationByInsId 终止实例（对齐 NetSight 集成模式，warm-flow 1.8.9 起禁止 REJECT 退回 START）</li>
 *   <li>审批人由发起时 FlowParams.nextHandler(approverId) 动态指定；超管/园区管理员代批通过 flow_user 认领</li>
 * </ul>
 *
 * @author ruoyi
 */
@Component
public class WarmFlowInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WarmFlowInitializer.class);

    /** 访客预约审批流程编码 */
    public static final String FLOW_CODE = "visitor_appointment";

    /** 流程定义 JSON（warm-flow importJson 格式，字段=定义/节点/跳转表驼峰） */
    private static final String FLOW_JSON = """
            {
              "flowCode": "visitor_appointment",
              "flowName": "访客预约审批",
              "category": "访客",
              "version": "1.0",
              "nodeList": [
                {
                  "nodeCode": "start",
                  "nodeName": "发起",
                  "nodeType": 0,
                  "coordinate": "100,200|100,200",
                  "skipList": [
                    {
                      "nowNodeCode": "start",
                      "nextNodeCode": "approve",
                      "skipName": "发起",
                      "skipType": "PASS"
                    }
                  ]
                },
                {
                  "nodeCode": "approve",
                  "nodeName": "审批",
                  "nodeType": 1,
                  "coordinate": "320,200|320,200",
                  "skipList": [
                    {
                      "nowNodeCode": "approve",
                      "nextNodeCode": "end",
                      "skipName": "通过",
                      "skipType": "PASS"
                    }
                  ]
                },
                {
                  "nodeCode": "end",
                  "nodeName": "结束",
                  "nodeType": 2,
                  "coordinate": "540,200|540,200"
                }
              ]
            }
            """;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Definition def = FlowEngine.defService().getPublishByFlowCode(FLOW_CODE);
            if (def != null) {
                log.info("warm-flow流程定义已发布: flowCode={}, defId={}", FLOW_CODE, def.getId());
                return;
            }
            Definition imported = FlowEngine.defService().importJson(FLOW_JSON);
            if (imported != null && imported.getId() != null) {
                FlowEngine.defService().publish(imported.getId());
                log.info("warm-flow流程定义导入并发布成功: flowCode={}, defId={}", FLOW_CODE, imported.getId());
            } else {
                log.error("warm-flow流程定义导入返回空，请检查 JSON 格式与 flow_* 表结构");
            }
        } catch (Exception e) {
            log.error("warm-flow流程定义初始化失败: flowCode={}", FLOW_CODE, e);
        }
    }
}
