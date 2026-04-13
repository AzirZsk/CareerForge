package com.careerforge.company.handler;

import com.careerforge.company.dto.CompanyVO;
import com.careerforge.company.entity.Company;
import com.careerforge.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 公司 Handler
 * 处理公司相关的业务逻辑
 *
 * @author Azir
 */
@Component
@RequiredArgsConstructor
public class CompanyHandler {

    private final CompanyService companyService;

    /**
     * 根据公司名称获取公司信息
     *
     * @param name 公司名称
     * @return 公司 VO
     */
    public CompanyVO getCompanyByName(String name) {
        return companyService.findByName(name)
                .map(companyService::toVO)
                .orElse(null);
    }

    /**
     * 检查公司是否存在且调研未过期
     *
     * @param name 公司名称
     * @return true 如果存在且未过期
     */
    public boolean hasValidResearch(String name) {
        Company company = companyService.findByName(name).orElse(null);
        return !companyService.isResearchExpired(company);
    }

    /**
     * 保存公司调研结果
     *
     * @param name     公司名称
     * @param research 调研结果（JSON格式）
     * @return 公司实体
     */
    public Company saveResearch(String name, String research) {
        return companyService.createOrUpdateCompany(name, research);
    }

}
