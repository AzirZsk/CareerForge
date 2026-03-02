<!--=====================================================
  LandIt 简历内容查看器组件
  展示简历的具体内容（用于对比视图）
  @author Azir
=====================================================-->

<template>
  <div class="resume-content-viewer">
    <div v-if="isEmpty" class="empty-state">
      <p>暂无简历内容</p>
    </div>

    <div v-else class="resume-content">
      <!-- 基本信息 -->
      <div v-if="resume.basicInfo" class="resume-section">
        <h3>基本信息</h3>
        <div class="info-grid">
          <div v-for="(value, key) in resume.basicInfo" :key="key" class="info-item">
            <span class="info-label">{{ getFieldLabel(String(key)) }}</span>
            <span class="info-value">{{ value || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 教育经历 -->
      <div v-if="resume.education?.length" class="resume-section">
        <h3>教育经历</h3>
        <div v-for="(edu, idx) in resume.education" :key="idx" class="experience-item">
          <div class="exp-header">
            <span class="exp-title">{{ edu.school }}</span>
            <span class="exp-period">{{ edu.period }}</span>
          </div>
          <div class="exp-details">
            {{ edu.degree }}<span v-if="edu.major"> · {{ edu.major }}</span>
          </div>
        </div>
      </div>

      <!-- 工作经历 -->
      <div v-if="resume.work?.length" class="resume-section">
        <h3>工作经历</h3>
        <div v-for="(job, idx) in resume.work" :key="idx" class="experience-item">
          <div class="exp-header">
            <span class="exp-title">{{ job.company }}</span>
            <span class="exp-period">{{ job.period }}</span>
          </div>
          <div class="exp-role">{{ job.position }}</div>
          <p v-if="job.description" class="exp-description">{{ job.description }}</p>
          <div v-if="job.achievements?.length" class="exp-achievements">
            <div v-for="(ach, achIdx) in job.achievements" :key="achIdx" class="achievement-item">
              {{ ach }}
            </div>
          </div>
        </div>
      </div>

      <!-- 项目经历 -->
      <div v-if="resume.projects?.length" class="resume-section">
        <h3>项目经历</h3>
        <div v-for="(proj, idx) in resume.projects" :key="idx" class="experience-item">
          <div class="exp-header">
            <span class="exp-title">{{ proj.name }}</span>
            <span class="exp-period">{{ proj.period }}</span>
          </div>
          <div class="exp-role">{{ proj.role }}</div>
          <p v-if="proj.description" class="exp-description">{{ proj.description }}</p>
          <div v-if="proj.achievements?.length" class="exp-achievements">
            <div v-for="(ach, achIdx) in proj.achievements" :key="achIdx" class="achievement-item">
              {{ ach }}
            </div>
          </div>
        </div>
      </div>

      <!-- 技能 -->
      <div v-if="resume.skills?.length" class="resume-section">
        <h3>专业技能</h3>
        <div class="skills-list">
          <span v-for="(skill, idx) in resume.skills" :key="idx" class="skill-tag">
            {{ skill }}
          </span>
        </div>
      </div>

      <!-- 证书 -->
      <div v-if="resume.certificates?.length" class="resume-section">
        <h3>证书荣誉</h3>
        <div v-for="(cert, idx) in resume.certificates" :key="idx" class="certificate-item">
          <span class="cert-name">{{ cert.name }}</span>
          <span v-if="cert.date" class="cert-date">{{ cert.date }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  resume: any
  side: 'before' | 'after'
  changes?: any[]
}

const props = defineProps<Props>()

const isEmpty = computed(() => !props.resume || Object.keys(props.resume).length === 0)

// 字段标签映射
const FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  gender: '性别',
  phone: '电话',
  email: '邮箱',
  targetPosition: '目标岗位',
  summary: '简介'
}

function getFieldLabel(key: string): string {
  return FIELD_LABELS[key] || key
}
</script>

<style lang="scss" scoped>
.resume-content-viewer {
  min-height: 300px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: $color-text-tertiary;
}

.resume-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.resume-section {
  h3 {
    font-size: $text-base;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.info-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.info-value {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.experience-item {
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.exp-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-details {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.exp-role {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
  white-space: pre-wrap;
}

.exp-achievements {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.achievement-item {
  font-size: $text-sm;
  color: $color-text-secondary;
  padding-left: $spacing-md;
  position: relative;

  &::before {
    content: '•';
    position: absolute;
    left: 0;
    color: $color-accent;
  }
}

.skills-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.skill-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.1);
  color: $color-accent;
  border-radius: $radius-sm;
  font-size: $text-sm;
}

.certificate-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.cert-name {
  font-size: $text-sm;
  color: $color-text-primary;
}

.cert-date {
  font-size: $text-xs;
  color: $color-text-tertiary;
}
</style>
