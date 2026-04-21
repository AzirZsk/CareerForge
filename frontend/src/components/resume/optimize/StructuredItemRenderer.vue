<!--=====================================================
  CareerForge 结构化数组项渲染器
  解析 JSON 字符串并按字段类型定制渲染
  @author Azir
=====================================================-->
<template>
  <!-- Skills 定制渲染 -->
  <div
    v-if="field === 'skills' && item.parsed"
    class="si si-skill"
  >
    <div class="si-header">
      <span
        v-if="item.parsed.level"
        class="si-level"
        :class="getLevelClass(item.parsed.level)"
      >{{ item.parsed.level }}</span>
      <span class="si-name">{{ item.parsed.name }}</span>
      <span
        v-if="item.parsed.category"
        class="si-tag"
      >{{ item.parsed.category }}</span>
    </div>
    <div
      v-if="item.parsed.description"
      class="si-desc"
    >
      {{ item.parsed.description }}
    </div>
  </div>

  <!-- Certificates 定制渲染 -->
  <div
    v-else-if="field === 'certificates' && item.parsed"
    class="si si-cert"
  >
    <div class="si-header">
      <span class="si-name">{{ item.parsed.name }}</span>
      <span
        v-if="item.parsed.date"
        class="si-tag"
      >{{ item.parsed.date }}</span>
    </div>
    <div
      v-if="item.parsed.issuer"
      class="si-meta"
    >
      {{ item.parsed.issuer }}
    </div>
  </div>

  <!-- 通用 JSON 对象渲染 -->
  <div
    v-else-if="item.parsed"
    class="si si-generic"
  >
    <div class="si-header">
      <span
        v-if="item.parsed.name"
        class="si-name"
      >{{ item.parsed.name }}</span>
      <span
        v-if="item.parsed.period || item.parsed.date"
        class="si-tag"
      >{{ item.parsed.period || item.parsed.date }}</span>
    </div>
    <div
      v-if="item.parsed.description"
      class="si-desc"
    >
      {{ item.parsed.description }}
    </div>
    <div
      v-if="item.parsed.highlights?.length"
      class="si-highlights"
    >
      <div
        v-for="(h, hIdx) in item.parsed.highlights"
        :key="hIdx"
        class="si-highlight"
      >
        · {{ h }}
      </div>
    </div>
    <div
      v-if="item.parsed.issuer"
      class="si-meta"
    >
      {{ item.parsed.issuer }}
    </div>
  </div>

  <!-- 纯文本兜底 -->
  <span v-else>{{ item.raw }}</span>
</template>

<script setup lang="ts">
defineProps<{
  item: { raw: string; parsed: Record<string, any> | null }
  field: string
}>()

function getLevelClass(level: string): string {
  const map: Record<string, string> = {
    '\u7CBE\u901A': 'master',
    '\u719F\u7EC3': 'proficient',
    '\u719F\u6089': 'familiar',
    '\u4E86\u89E3': 'basic'
  }
  return map[level] || 'basic'
}
</script>

<style lang="scss" scoped>
.si {
  line-height: 1.6;
}

.si-header {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.si-name {
  font-weight: $weight-medium;
  flex: 1;
  min-width: 0;
}

.si-tag {
  font-size: 10px;
  color: $color-text-tertiary;
  opacity: 0.8;
  flex-shrink: 0;
}

.si-level {
  display: inline-block;
  padding: 0 5px;
  border-radius: $radius-sm;
  font-size: 10px;
  font-weight: $weight-medium;
  flex-shrink: 0;

  &.master {
    background: rgba(212, 168, 83, 0.2);
    color: $color-accent;
  }
  &.proficient {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.familiar {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
  &.basic {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-tertiary;
  }
}

.si-desc {
  color: $color-text-secondary;
  padding-left: 0;
  margin-top: 1px;
}

.si-meta {
  color: $color-text-tertiary;
  font-size: 10px;
}

.si-highlights {
  margin-top: 1px;
}

.si-highlight {
  color: $color-text-secondary;
  padding-left: $spacing-sm;
}
</style>
