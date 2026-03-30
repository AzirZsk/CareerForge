<template>
  <div class="transcript-viewer" ref="containerRef">
    <div class="transcript-header">
      <h3>文字记录</h3>
      <span class="segment-count">{{ transcript.length }} 条记录</span>
    </div>
    <div class="transcript-list">
      <div
        v-for="(entry, index) in transcript"
        :key="index"
        :class="['transcript-item', `role-${entry.role}`, { active: isActiveSegment(entry.segmentIndex) }]"
        @click="handleClick(entry.segmentIndex)"
      >
        <div class="item-header">
          <span class="role-badge" :class="entry.role">{{ getRoleName(entry.role) }}</span>
          <span class="timestamp">{{ formatTimestamp(entry.timestamp) }}</span>
        </div>
        <div class="item-content">{{ entry.content }}</div>
      </div>
      <div v-if="transcript.length === 0" class="empty-state">
        暂无文字记录
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { TranscriptEntry } from '@/types/interview-voice'
import { getRoleName, formatTimestamp } from '@/utils/recording-helpers'

// Props
interface Props {
  transcript: TranscriptEntry[]
  currentTime: number
  currentSegmentIndex: number
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  (e: 'jumpToSegment', segmentIndex: number): void
}>()

// Refs
const containerRef = ref<HTMLElement | null>(null)

// 判断是否是当前活动片段
function isActiveSegment(segmentIndex: number): boolean {
  return segmentIndex === props.currentSegmentIndex
}

// 点击跳转
function handleClick(segmentIndex: number) {
  emit('jumpToSegment', segmentIndex)
}

// 自动滚动到当前片段
function scrollToActiveItem() {
  if (!containerRef.value) return
  const activeItem = containerRef.value.querySelector('.transcript-item.active')
  if (activeItem) {
    activeItem.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 监听当前片段变化，自动滚动
watch(() => props.currentSegmentIndex, () => {
  nextTick(() => {
    scrollToActiveItem()
  })
})
</script>

<style lang="scss" scoped>
.transcript-viewer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  overflow: hidden;
}

.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $color-bg-tertiary;

  h3 {
    font-size: 1rem;
    font-weight: 600;
    color: $color-text-primary;
    margin: 0;
  }

  .segment-count {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }
}

.transcript-list {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md;
}

.transcript-item {
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;

  &:hover {
    background: $color-bg-tertiary;
  }

  &.role-interviewer {
    border-left-color: $color-accent;
    background: rgba($color-accent, 0.05);

    &:hover {
      background: rgba($color-accent, 0.1);
    }
  }

  &.role-candidate {
    border-left-color: $color-info;
    background: rgba($color-info, 0.05);

    &:hover {
      background: rgba($color-info, 0.1);
    }
  }

  &.role-assistant {
    border-left-color: $color-success;
    background: rgba($color-success, 0.05);

    &:hover {
      background: rgba($color-success, 0.1);
    }
  }

  &.active {
    box-shadow: 0 0 0 2px $color-accent;

    &.role-interviewer {
      background: rgba($color-accent, 0.15);
    }

    &.role-candidate {
      background: rgba($color-info, 0.15);
    }

    &.role-assistant {
      background: rgba($color-success, 0.15);
    }
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.role-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: $radius-sm;

  &.interviewer {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.candidate {
    background: rgba($color-info, 0.2);
    color: $color-info;
  }

  &.assistant {
    background: rgba($color-success, 0.2);
    color: $color-success;
  }
}

.timestamp {
  font-size: 0.75rem;
  color: $color-text-tertiary;
}

.item-content {
  font-size: 0.9375rem;
  color: $color-text-secondary;
  line-height: 1.6;
  word-break: break-word;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: $color-text-tertiary;
  font-size: 0.875rem;
}
</style>
