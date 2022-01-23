package org.zhongweixian.api.service.impl;

import org.cti.cc.entity.AdminUser;
import org.cti.cc.entity.AdminMenu;
import org.cti.cc.enums.ErrorCode;
import org.cti.cc.mapper.AdminUserMapper;
import org.cti.cc.mapper.AdminMenuMapper;
import org.cti.cc.mapper.base.BaseMapper;
import org.cti.cc.po.CompanyInfo;
import org.cti.cc.po.MenusPo;
import org.cti.cc.request.AdminLogin;
import org.cti.cc.util.AuthUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zhongweixian.api.exception.BusinessException;
import org.zhongweixian.api.service.AdminService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caoliang on 2022/1/7
 */
@Service
public class AdminServiceImpl extends BaseServiceImpl<AdminUser> implements AdminService {

    @Autowired
    private AdminMenuMapper adminMenuMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;


    @Override
    public String login(AdminLogin adminLogin) {
        AdminUser adminUser = adminUserMapper.adminLogin(adminLogin.getUsername());
        if (adminUser == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_ERROR);
        }
        if (adminUser.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        CompanyInfo companyInfo = companyMapper.selectById(adminUser.getCompanyId());


        String token = AuthUtil.createToken(adminUser.getUsername(), adminUser.getCompanyId(), companyInfo.getSecretKey());


        return null;
    }

    @Override
    public List<MenusPo> getAllMenus(Long uid) {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("menuLevel", 1);
        return allMenuIterator(params);
    }

    @Override
    public List<MenusPo> getMenus(Long uid) {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("menuLevel", 1);
        return userMenuIterator(params);
    }

    @Override
    public int addMenus(List<Long> ids) {
        return 0;
    }

    @Override
    public int cancelMenus(List<Long> ids) {
        return 0;
    }

    private List<MenusPo> userMenuIterator(Map<String, Object> params) {
        List<AdminMenu> adminMenus = adminMenuMapper.selectUserMenus(params);
        if (CollectionUtils.isEmpty(adminMenus)) {
            return null;
        }
        List<MenusPo> menusPoList = new ArrayList<>();
        for (AdminMenu child : adminMenus) {
            MenusPo childPo = new MenusPo();
            params.put("parentId", child.getMenuId());
            params.remove("menuLevel");
            BeanUtils.copyProperties(child, childPo);
            childPo.setChilds(userMenuIterator(params));
            menusPoList.add(childPo);
        }
        return menusPoList;
    }

    private List<MenusPo> allMenuIterator(Map<String, Object> params) {
        List<AdminMenu> adminMenus = adminMenuMapper.selectAllMenus(params);
        if (CollectionUtils.isEmpty(adminMenus)) {
            return null;
        }
        List<MenusPo> menusPoList = new ArrayList<>();
        for (AdminMenu child : adminMenus) {
            MenusPo childPo = new MenusPo();
            params.put("parentId", child.getMenuId());
            params.remove("menuLevel");
            BeanUtils.copyProperties(child, childPo);
            childPo.setUid(Long.parseLong(child.getStatus().toString()));
            childPo.setChilds(allMenuIterator(params));
            menusPoList.add(childPo);
        }
        return menusPoList;
    }

    @Override
    BaseMapper<AdminUser> baseMapper() {
        return adminUserMapper;
    }
}
