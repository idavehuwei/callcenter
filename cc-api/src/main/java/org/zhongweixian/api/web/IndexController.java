package org.zhongweixian.api.web;

import org.cti.cc.po.CommonResponse;
import org.cti.cc.request.AdminLogin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhongweixian.api.service.AdminService;

/**
 * Create by caoliang on 2020/12/15
 */

@RestController
@RequestMapping("index")
public class IndexController {
    private Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private AdminService adminService;

    @PostMapping("index")
    public CommonResponse index(@RequestBody String payload) {
        logger.info("{}", payload);
        return new CommonResponse();
    }

    @PostMapping("login")
    public CommonResponse login(@Validated @RequestBody AdminLogin adminLogin) {
        return new CommonResponse(adminService.login(adminLogin));
    }


}
