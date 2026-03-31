package com.landit.company.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.company.dto.CompanyVO;
import com.landit.company.entity.Company;
import com.landit.company.mapper.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 公司服务类
 * 负责公司表的 CRUD 操作
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class CompanyService extends ServiceImpl<CompanyMapper, Company> {

    /**
     * 根据公司名称查询公司
     *
     * @param name 公司名称
     * @return 公司实体
     */
    public Optional<Company> findByName(String name) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Company::getName, name);
        return Optional.ofNullable(getOne(wrapper));
    }

    /**
     * 创建或更新公司调研信息
     *
     * @param name     公司名称
     * @param research 调研结果（JSON格式）
     * @return 公司实体
     */
    public Company createOrUpdateCompany(String name, String research) {
        Optional<Company> existingCompany = findByName(name);
        Company company;
        if (existingCompany.isPresent()) {
            company = existingCompany.get();
            company.setResearch(research);
            company.setResearchUpdatedAt(LocalDateTime.now());
            updateById(company);
        } else {
            company = new Company();
            company.setName(name);
            company.setResearch(research);
            company.setResearchUpdatedAt(LocalDateTime.now());
            save(company);
        }
        return company;
    }

    /**
     * 检查公司调研是否过期（超过30天）
     *
     * @param company 公司实体
     * @return true 如果过期或无调研信息
     */
    public boolean isResearchExpired(Company company) {
        if (company == null || company.getResearch() == null || company.getResearch().isEmpty()) {
            return true;
        }
        if (company.getResearchUpdatedAt() == null) {
            return true;
        }
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return company.getResearchUpdatedAt().isBefore(thirtyDaysAgo);
    }

    /**
     * 转换为 VO
     *
     * @param company 公司实体
     * @return CompanyVO
     */
    public CompanyVO toVO(Company company) {
        if (company == null) {
            return null;
        }
        return CompanyVO.builder()
                .id(String.valueOf(company.getId()))
                .name(company.getName())
                .research(company.getResearch())
                .researchUpdatedAt(company.getResearchUpdatedAt())
                .build();
    }

}
