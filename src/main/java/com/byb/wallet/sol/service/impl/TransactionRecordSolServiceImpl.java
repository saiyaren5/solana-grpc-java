package com.byb.wallet.sol.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.byb.wallet.infrastructure.common.block.sol.entity.TransactionRecordSol;
import com.byb.wallet.infrastructure.common.block.sol.mapper.TransactionRecordSolMapper;
import com.byb.wallet.sol.service.TransactionRecordSolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 索拉纳交易记录表 服务实现类
 * @author Byb Team
 * @since 2024-11-01 17:28:13
 */
@Slf4j
@Service
public class TransactionRecordSolServiceImpl extends ServiceImpl<TransactionRecordSolMapper, TransactionRecordSol> implements TransactionRecordSolService {

    @Override
    public boolean saveBatch(Collection<TransactionRecordSol> entityList) {
        baseMapper.insertBatchSomeColumn(new ArrayList<>(entityList));
        return true;
    }
}
