package com.joy.swiftcrawler.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joy.swiftcrawler.entity.SwiftCode;
import com.joy.swiftcrawler.mapper.SwiftCodeMapper;
import com.joy.swiftcrawler.service.SwiftCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@Service
public class SwiftCodeServiceImpl implements SwiftCodeService {

    private SwiftCodeMapper mapper;

    @Autowired
    public SwiftCodeServiceImpl(SwiftCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public void saveOrUpdate(SwiftCode swiftCode) {
        SwiftCode oldCode = mapper.selectOne(Wrappers.lambdaQuery(SwiftCode.class).eq(SwiftCode::getSwiftCode, swiftCode.getSwiftCode()));
        if (oldCode == null) {
            int rows = mapper.insert(swiftCode);
            if (rows == 1) {
                log.debug("insert swift code[{}].", swiftCode.getSwiftCode());
            } else {
                log.error("insert swift code[{}] error!", swiftCode.getSwiftCode());
            }
            return;
        }
        int rows = mapper.update(swiftCode, Wrappers.lambdaUpdate(SwiftCode.class).eq(SwiftCode::getSwiftCode, swiftCode.getSwiftCode()));
        if (rows == 1) {
            log.debug("update swift code [{}].", swiftCode.getSwiftCode());
        } else {
            log.error("update swift code [{}] error!", swiftCode.getSwiftCode());
        }
    }
}
