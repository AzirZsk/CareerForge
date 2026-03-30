<!--=====================================================
  技能模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block skills-block">
    <div
      v-for="(skill, idx) in skills"
      :key="idx"
      class="skill-item"
    >
      <div class="skill-header">
        <span class="skill-name">{{ skill.name }}</span>
        <span
          v-if="skill.level"
          class="skill-level"
        >{{ skill.level }}</span>
      </div>
      <p
        v-if="skill.description"
        class="skill-description"
      >
        {{ skill.description }}
      </p>
      <span
        v-if="skill.category"
        class="skill-category"
      >{{ skill.category }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Skill } from '@/types'

defineProps<{
  skills: Skill[]
}>()
</script>

<style lang="scss" scoped>
.skills-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.skill-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(255, 255, 255, 0.1);
  }
}

.skill-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
}

.skill-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.skill-level {
  padding: 2px $spacing-sm;
  font-size: $text-xs;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  border-radius: $radius-sm;
}

.skill-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-xs;
}

.skill-category {
  display: inline-block;
  padding: 2px $spacing-xs;
  font-size: $text-xs;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
}
</style>
