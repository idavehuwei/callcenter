package org.zhongweixian.api.service;

import org.cti.cc.entity.AdminUser;
import org.cti.cc.po.MenusPo;
import org.cti.cc.request.AdminLogin;

import java.util.List;

/**
 * Created by caoliang on 2022/1/6
 */
public interface AdminService extends BaseService<AdminUser> {

    /**
     * 账号登录
     *
     * @param adminLogin
     * @return
     */
    String login(AdminLogin adminLogin);

    /**
     * 获取菜单
     *
     * @return
     */
    List<MenusPo> getAllMenus(Long uid);

    /**
     * 获取菜单
     *
     * @param uid
     * @return
     */
    List<MenusPo> getMenus(Long uid);

    /**
     * 授权菜单
     *
     * @param ids
     * @return
     */
    int addMenus(List<Long> ids);

    /**
     * 取消授权
     *
     * @param ids
     * @return
     */
    int cancelMenus(List<Long> ids);


}
