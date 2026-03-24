<!--=====================================================
  AI 助手图标组件
  统一的机器人图标，支持自定义尺寸
  @author Azir
=====================================================-->

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <!-- 机器人身体 -->
    <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
    <!-- 左眼 -->
    <circle cx="8" cy="14" r="1.5" fill="currentColor"/>
    <!-- 右眼 -->
    <circle cx="16" cy="14" r="1.5" fill="currentColor"/>
    <!-- 嘴巴 -->
    <path d="M9 18c.83.67 1.83 1 3 1s2.17-.33 3-1"/>
  </svg>
</template>

<script setup lang="ts">
interface Props {
  size?: number | string
}

withDefaults(defineProps<Props>(), {
  size: 24
})
</script>
