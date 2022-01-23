package org.cti.cc.mapper;

import org.cti.cc.entity.AdminMenu;
import org.cti.cc.mapper.base.BaseMapper;

import java.util.List;
import java.util.Map;

public interface AdminMenuMapper extends BaseMapper<AdminMenu> {


    /**
     * @param params
     * @return
     */
    List<AdminMenu> selectAllMenus(Map<String, Object> params);

    /**
     * 用户菜单
     *
     * @param params
     * @return
     */
    List<AdminMenu> selectUserMenus(Map<String, Object> params);
}