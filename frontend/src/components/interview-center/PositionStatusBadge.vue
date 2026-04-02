<template>
  <span class="status-badge" :class="[status, { pulse: config.pulse }]">
    <span class="status-dot"></span>
    <span class="status-text">{{ config.label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PositionStatus } from '@/types/job-position'
import { getStatusConfig } from '@/constants/position-status'

const props = defineProps<{
  status: PositionStatus
}>()

const config = computed(() => getStatusConfig(props.status))
</script>

<style scoped lang="scss">
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: $radius-full;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s;

  // 状态颜色
  &.draft {
    color: #71717a;
    background: rgba(113, 113, 122, 0.15);
    border: 1px solid rgba(113, 113, 122, 0.3);

    .status-dot {
      background: #71717a;
    }
  }

  &.applied {
    color: #60a5fa;
    background: rgba(96, 165, 250, 0.15);
    border: 1px solid rgba(96, 165, 250, 0.3);

    .status-dot {
      background: #60a5fa;
    }
  }

  &.interviewing {
    color: #fbbf24;
    background: rgba(251, 191, 36, 0.15);
    border: 1px solid rgba(251, 191, 36, 0.3);

    .status-dot {
      background: #fbbf24;
    }

    // 脉动动画
    &.pulse .status-dot {
      animation: pulse 2s infinite;
    }
  }

  &.offered {
    color: #34d399;
    background: rgba(52, 211, 153, 0.15);
    border: 1px solid rgba(52, 211, 153, 0.3);

    .status-dot {
      background: #34d399;
    }
  }

  &.rejected {
    color: #f87171;
    background: rgba(248, 113, 113, 0.15);
    border: 1px solid rgba(248, 113, 113, 0.3);

    .status-dot {
      background: #f87171;
    }
  }

  &.withdrawn {
    color: #71717a;
    background: rgba(113, 113, 122, 0.15);
    border: 1px solid rgba(113, 113, 122, 0.3);

    .status-dot {
      background: #71717a;
    }
  }
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-text {
  line-height: 1;
}

// 脉动动画
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }

  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}
</style>
