package com.careerforge.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.company.entity.Company;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公司 Mapper 接口
 *
 * @author Azir
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

}
