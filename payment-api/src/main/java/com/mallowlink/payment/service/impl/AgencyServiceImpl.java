package com.mallowlink.payment.service.impl;

import com.mallowlink.payment.client.MallowlinkProperties;
import com.mallowlink.payment.service.AgencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements AgencyService {

    private final MallowlinkProperties mallowlinkProperties;

    @Override
    public void getAgencies() {
        log.info("getAgencies");



    }
}
