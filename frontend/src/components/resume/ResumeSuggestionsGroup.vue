<!--=====================================================
  简历建议分组卡片
  按简历分组展示优化建议，点击可跳转到简历详情页
  @author Azir
=====================================================-->
<template>
  <div class="suggestion-group-card" @click="goToResume">
    <!-- 简历标题行 -->
    <div class="group-header">
      <div class="resume-info">
        <svg class="resume-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
        </svg>
        <span class="resume-name">{{ group.resumeName }}</span>
        <span class="target-position" v-if="group.targetPosition">{{ group.targetPosition }}</span>
      </div>
      <div class="suggestion-badge">
        {{ group.suggestionCount }} 条建议
        <svg class="arrow-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </div>
    </div>
    <!-- 建议列表 -->
    <div class="suggestion-items" v-if="group.suggestions.length > 0">
      <div
        v-for="suggestion in group.suggestions"
        :key="suggestion.id"
        class="suggestion-item"
      >
        <span class="priority-dot" :class="suggestion.type"></span>
        <span class="suggestion-title">{{ suggestion.title }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ResumeSuggestionsGroup } from '@/types'

const props = defineProps<{
  group: ResumeSuggestionsGroup
}>()

const router = useRouter()

function goToResume(): void {
  router.push(`/resume/${props.group.resumeId}`)
}
</script>

<style lang="scss" scoped>
.suggestion-group-card {
  padding: $spacing-lg;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(212, 168, 83, 0.2);
    transform: translateY(-2px);
  }
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.resume-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.resume-icon {
  color: $color-accent;
}

.resume-name {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.target-position {
  font-size: $text-xs;
  color: $color-text-tertiary;
  &::before {
    content: '·';
    margin-right: $spacing-xs;
  }
}

.suggestion-badge {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  color: $color-accent;
}

.arrow-icon {
  opacity: 0;
  transition: opacity $transition-fast;
  .suggestion-group-card:hover & {
    opacity: 1;
  }
}

.suggestion-items {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-secondary;
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  &.critical { background: $color-error; }
  &.improvement { background: $color-warning; }
  &.enhancement { background: $color-info; }
}

.suggestion-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
