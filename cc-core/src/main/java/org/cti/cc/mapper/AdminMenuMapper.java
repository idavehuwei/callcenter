package org.cti.cc.mapper;

import org.apache.ibatis.annotations.Param;
import org.cti.cc.entity.AdminMenu;

import java.util.List;
import java.util.Map;

public interface AdminMenuMapper {
    int deleteByPrimaryKey(Long id);

    int insert(AdminMenu record);

    int insertSelective(AdminMenu record);

    AdminMenu selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AdminMenu record);

    int updateByPrimaryKey(AdminMenu record);


    /**
     * 查询所有菜单
     *
     * @return
     */
    List<AdminMenu> selectAllMenus();

    /**
     * @param parentId
     * @return
     */
    List<AdminMenu> selectAllChildMenus(String parentId);

    /**
     * 用户菜单
     *
     * @param params
     * @return
     */
    List<AdminMenu> selectUserMenus(Map<String, Object> params);
}