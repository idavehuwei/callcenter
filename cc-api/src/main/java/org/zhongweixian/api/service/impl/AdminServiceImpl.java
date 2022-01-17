package org.zhongweixian.api.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.cti.cc.entity.AdminMenu;
import org.cti.cc.mapper.AdminMenuMapper;
import org.cti.cc.po.MenusPo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zhongweixian.api.service.AdminService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caoliang on 2022/1/7
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMenuMapper adminMenuMapper;

    @Override
    public List<MenusPo> getAllMenus() {
        List<MenusPo> menusPoList = new ArrayList<>();
        List<AdminMenu> adminMenus = adminMenuMapper.selectAllMenus();
        if (CollectionUtils.isEmpty(adminMenus)) {
            return menusPoList;
        }
        for (AdminMenu adminMenu : adminMenus) {
            List<MenusPo> menus = menuIterator(adminMenu);
            if (!CollectionUtils.isEmpty(menus)) {
                menusPoList.addAll(menus);
            }
        }
        return menusPoList;
    }

    @Override
    public List<MenusPo> getMenus(Long uid) {
        List<MenusPo> menusPoList = new ArrayList<>();
        List<AdminMenu> adminMenus = adminMenuMapper.selectUserMenus(uid);
        if (CollectionUtils.isEmpty(adminMenus)) {
            return menusPoList;
        }
        for (AdminMenu adminMenu : adminMenus) {
            List<MenusPo> menus = menuIterator(adminMenu, uid);
            if (!CollectionUtils.isEmpty(menus)) {
                menusPoList.addAll(menus);
            }
        }
        return menusPoList;
    }

    @Override
    public int addMenus(List<Long> ids) {
        return 0;
    }

    @Override
    public int cancelMenus(List<Long> ids) {
        return 0;
    }


    /**
     * 递归获取菜单
     *
     * @param menu
     * @return
     */
    private List<MenusPo> menuIterator(AdminMenu menu) {
        List<AdminMenu> adminMenus = adminMenuMapper.selectAllChildMenus(menu.getMenuId());
        List<MenusPo> menusPoList = new ArrayList<>();
        MenusPo menusPo = new MenusPo();
        if (CollectionUtils.isEmpty(adminMenus)) {
            BeanUtils.copyProperties(menu, menusPo);
            menusPoList.add(menusPo);
            return menusPoList;
        }
        BeanUtils.copyProperties(menu, menusPo);
        for (AdminMenu child : adminMenus) {
            MenusPo childPo = new MenusPo();
            BeanUtils.copyProperties(child, childPo);
            menusPo.setChilds(menuIterator(child));
            menusPoList.add(menusPo);
        }
        return menusPoList;
    }
}
