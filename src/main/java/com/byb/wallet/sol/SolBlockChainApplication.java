package com.byb.wallet.sol;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sol Block Chain Application
 *
 * @author Byb Team
 * @since 2024-05-29  15:14
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.byb.wallet")
@MapperScan(basePackages = {"com.byb.wallet.infrastructure.common.**.mapper"})
public class SolBlockChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolBlockChainApplication.class, args);
        log.info(">>>>>>>>> SolBlockChainApplication start success! >>>>>>>>>>");
    }
}