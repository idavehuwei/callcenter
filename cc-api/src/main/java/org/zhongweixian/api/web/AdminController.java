package org.zhongweixian.api.web;

import org.cti.cc.po.AdminAccountInfo;
import org.cti.cc.po.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zhongweixian.api.service.AdminService;

/**
 * Created by caoliang on 2022/1/6
 * <p>
 * 超管操作
 */
@RestController
@RequestMapping("admin")
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取所有菜单
     *
     * @return
     */
    @GetMapping("getAllMenus")
    public CommonResponse getAllMenus(@ModelAttribute("adminAccountInfo") AdminAccountInfo adminAccountInfo) {
        return new CommonResponse(adminService.getAllMenus(adminAccountInfo.getBindCompanyId()));
    }

    /**
     * 登录获取菜单
     *
     * @param adminAccountInfo
     * @return
     */
    @GetMapping("getMenus")
    public CommonResponse getMenus(@ModelAttribute("adminAccountInfo") AdminAccountInfo adminAccountInfo) {
        return new CommonResponse(adminService.getMenus(adminAccountInfo.getBindCompanyId()));
    }


}
