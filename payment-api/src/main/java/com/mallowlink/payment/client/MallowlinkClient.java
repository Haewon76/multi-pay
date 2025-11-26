package com.mallowlink.payment.client;

import com.mallowlink.payment.config.MallowlinkConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "mallowlinkClient",
        url = "${external.service.tiger.url}",
        configuration = MallowlinkConfig.class
)
public interface MallowlinkClient {

    @PostMapping("/transaction/devOffice/agencies")
    String agencies(@RequestBody String request);

}
