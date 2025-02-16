package com.byb.wallet.sol;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * sol 健康检测 controller
 *
 * @author Byb Team
 * @since 2024-11-21  11:50
 */
@RestController
@RequestMapping("/chain")
public class ChainHealthController {

    // 健康检测
    @RequestMapping("/health")
    public String health() {
        return "{\"timestamp\":" + System.currentTimeMillis() + ",\"status\":\"ok\"}";
    }
}
