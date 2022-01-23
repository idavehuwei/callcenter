package org.zhongweixian.api.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.cti.cc.entity.CompanyDisplay;
import org.cti.cc.entity.CompanyPhone;
import org.cti.cc.entity.CompanyPhoneGroup;
import org.cti.cc.enums.ErrorCode;
import org.cti.cc.mapper.CompanyDisplayMapper;
import org.cti.cc.mapper.CompanyMapper;
import org.cti.cc.mapper.CompanyPhoneGroupMapper;
import org.cti.cc.mapper.CompanyPhoneMapper;
import org.cti.cc.mapper.base.BaseMapper;
import org.cti.cc.po.CompanyDisplayPo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zhongweixian.api.exception.BusinessException;
import org.zhongweixian.api.service.DisplayService;
import org.cti.cc.request.CompanyPhoneVo;
import org.cti.cc.request.DisplayGroupVo;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caoliang on 2021/5/26
 */
@Service
public class DisplayServiceImpl extends BaseServiceImpl<CompanyPhone> implements DisplayService {

    @Autowired
    private CompanyDisplayMapper companyDisplayMapper;

    @Autowired
    private CompanyPhoneMapper companyPhoneMapper;

    @Autowired
    private CompanyPhoneGroupMapper companyPhoneGroupMapper;

    @Autowired
    private CompanyMapper companyMapper;


    @Override
    BaseMapper<CompanyPhone> baseMapper() {
        return companyPhoneMapper;
    }

    @Override
    public int saveOrUpdatePhone(CompanyPhoneVo companyPhoneVo) {
        Map<String, Object> params = new HashMap<>();
        params.put("phone", companyPhoneVo.getPhone());
        List<CompanyPhone> companyPhones = companyPhoneMapper.selectListByMap(params);
        if (!CollectionUtils.isEmpty(companyPhones)) {
            //判断数据是否重复
            if (companyPhoneVo.getId() == null || !companyPhoneVo.getId().equals(companyPhones.get(0).getId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_EXCEPTION);
            }
        }
        if (companyPhoneVo.getId() == null) {
            //添加
            companyPhoneVo.setCts(Instant.now().getEpochSecond());
            CompanyPhone companyPhone = new CompanyPhone();
            BeanUtils.copyProperties(companyPhoneVo, companyPhone);
            return companyPhoneMapper.insertSelective(companyPhone);
        }
        //判断数据是否存在
        CompanyPhone exist = companyPhoneMapper.selectById(companyPhoneVo.getCompanyId(), companyPhoneVo.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        companyPhoneVo.setCts(null);
        companyPhoneVo.setUts(Instant.now().getEpochSecond());
        CompanyPhone companyPhone = new CompanyPhone();
        BeanUtils.copyProperties(companyPhoneVo, companyPhone);
        return companyPhoneMapper.updateByPrimaryKeySelective(companyPhone);
    }

    @Override
    public int deletePhone(Long companyId, Long id) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("companyId", companyId);
        List<CompanyPhone> companyPhones = companyPhoneMapper.selectListByMap(params);
        if (CollectionUtils.isEmpty(companyPhones)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        CompanyPhone companyPhone = companyPhones.get(0);

        //显号是否被号码池关联
        params.remove("id");
        params.put("phoneId", companyPhone.getId());
        List<CompanyDisplay> companyDisplays = companyDisplayMapper.selectListByMap(params);
        if (!CollectionUtils.isEmpty(companyDisplays)) {
            throw new BusinessException(ErrorCode.DATA_IS_USED);
        }
        companyPhone.setStatus(0);
        companyPhone.setPhone(companyPhone.getPhone() + randomDelete());
        companyPhone.setUts(Instant.now().getEpochSecond());
        return companyPhoneMapper.updateByPrimaryKeySelective(companyPhone);
    }

    @Override
    public PageInfo<CompanyDisplayPo> findDisplayByPage(Map<String, Object> params) {
        Integer pageNum = (Integer) params.get("pageNum");
        Integer pageSize = (Integer) params.get("pageSize");
        PageHelper.startPage(pageNum, pageSize);
        List<CompanyDisplayPo> list = companyDisplayMapper.selectPage(params);
        return new PageInfo<>(list);
    }

    @Override
    public CompanyDisplayPo getDisplay(Long companyId, Long id) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("id", id);
        List<CompanyDisplayPo> list = companyDisplayMapper.selectPage(params);
        if (CollectionUtils.isEmpty(list)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return list.get(0);
    }

    @Override
    public int saveOrUpdateDisplay(DisplayGroupVo displayGroupVo) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", displayGroupVo.getName());
        params.put("companyId", displayGroupVo.getCompanyId());
        if (displayGroupVo.getId() != null) {
            params.put("id", displayGroupVo.getId());
        }

        List<CompanyDisplay> displayList = companyDisplayMapper.selectListByMap(params);
        if (!CollectionUtils.isEmpty(displayList)) {
            //判断数据是否重复
            if (displayGroupVo.getId() == null ||
                    (displayGroupVo.getId() != null && !displayGroupVo.getId().equals(displayList.get(0).getId()))) {
                throw new BusinessException(ErrorCode.DUPLICATE_EXCEPTION);
            }
        }

        if (displayGroupVo.getId() == null) {
            //添加
            displayGroupVo.setCts(Instant.now().getEpochSecond());
            CompanyDisplay display = new CompanyDisplay();
            BeanUtils.copyProperties(displayGroupVo, display);
            companyDisplayMapper.insertSelective(display);

            int result = 0;
            //添加关联关系
            for (Long id : displayGroupVo.getPhoneIds()) {
                CompanyPhoneGroup phoneGroup = new CompanyPhoneGroup();
                phoneGroup.setCompanyId(displayGroupVo.getCompanyId());
                phoneGroup.setCts(Instant.now().getEpochSecond());
                phoneGroup.setDisplayId(display.getId());
                phoneGroup.setPhoneId(id);
                result += companyPhoneGroupMapper.insertSelective(phoneGroup);
            }
            return result;
        }
        //判断数据是否存在
        CompanyDisplay companyDisplay = companyDisplayMapper.selectById(displayGroupVo.getCompanyId(), displayGroupVo.getId());
        if (companyDisplay == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        displayGroupVo.setCts(null);
        displayGroupVo.setUts(Instant.now().getEpochSecond());
        companyDisplay = new CompanyDisplay();
        BeanUtils.copyProperties(displayGroupVo, companyDisplay);
        //添加关联关系,需要先删除关联关系
        companyPhoneGroupMapper.deleteByPrimaryKey(displayGroupVo.getId());
        for (Long id : displayGroupVo.getPhoneIds()) {
            CompanyPhoneGroup phoneGroup = new CompanyPhoneGroup();
            phoneGroup.setCompanyId(displayGroupVo.getCompanyId());
            phoneGroup.setCts(Instant.now().getEpochSecond());
            phoneGroup.setDisplayId(companyDisplay.getId());
            phoneGroup.setPhoneId(id);
            companyPhoneGroupMapper.insertSelective(phoneGroup);
        }
        return companyDisplayMapper.updateByPrimaryKeySelective(companyDisplay);
    }

    @Override
    public int deleteDisplay(Long companyId, Long id) {
        CompanyDisplay companyDisplay = companyDisplayMapper.selectById(companyId, id);
        if (companyDisplay == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        companyDisplay.setUts(Instant.now().getEpochSecond());
        companyDisplay.setName(companyDisplay.getName() + randomDelete());
        companyDisplay.setStatus(0);
        //删除关联表
        
        return companyDisplayMapper.updateByPrimaryKeySelective(companyDisplay);
    }
}
