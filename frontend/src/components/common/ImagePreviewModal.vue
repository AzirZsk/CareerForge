<!--=====================================================
  图片全屏预览弹窗组件
  支持缩放、拖拽、ESC关闭
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible"
        class="image-preview-overlay"
        @click="close"
      >
        <!-- 图片容器 -->
        <div
          class="image-container"
          @click.stop
        >
          <img
            :src="src"
            :style="imageStyle"
            class="preview-image"
            draggable="false"
            @wheel.prevent="handleWheel"
            @mousedown="startDrag"
          >
        </div>

        <!-- 关闭按钮 -->
        <button
          class="close-btn"
          title="关闭 (Esc)"
          @click="close"
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line
              x1="18"
              y1="6"
              x2="6"
              y2="18"
            />
            <line
              x1="6"
              y1="6"
              x2="18"
              y2="18"
            />
          </svg>
        </button>

        <!-- 操作按钮 -->
        <div
          class="action-bar"
          @click.stop
        >
          <button
            class="action-btn"
            title="缩小"
            @click="zoomOut"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle
                cx="11"
                cy="11"
                r="8"
              />
              <line
                x1="21"
                y1="21"
                x2="16.65"
                y2="16.65"
              />
              <line
                x1="8"
                y1="11"
                x2="14"
                y2="11"
              />
            </svg>
          </button>
          <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
          <button
            class="action-btn"
            title="放大"
            @click="zoomIn"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle
                cx="11"
                cy="11"
                r="8"
              />
              <line
                x1="21"
                y1="21"
                x2="16.65"
                y2="16.65"
              />
              <line
                x1="11"
                y1="8"
                x2="11"
                y2="14"
              />
              <line
                x1="8"
                y1="11"
                x2="14"
                y2="11"
              />
            </svg>
          </button>
          <div class="action-divider" />
          <button
            class="action-btn"
            title="向左旋转"
            @click="rotateLeft"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M2.5 2v6h6M2.66 15.57a10 10 0 1 0 .57-8.38" />
            </svg>
          </button>
          <button
            class="action-btn"
            title="向右旋转"
            @click="rotateRight"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38" />
            </svg>
          </button>
          <div class="action-divider" />
          <button
            class="action-btn"
            title="重置"
            @click="resetZoom"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
              <path d="M3 3v5h5" />
            </svg>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  visible: boolean
  src: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'close': []
}>()

// 缩放状态
const scale = ref(1)
const minScale = 0.5
const maxScale = 5
const scaleStep = 0.2

// 旋转状态
const rotation = ref(0)

// 拖拽状态
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const position = ref({ x: 0, y: 0 })

// 计算图片样式
const imageStyle = computed(() => ({
  transform: `translate(${position.value.x}px, ${position.value.y}px) scale(${scale.value}) rotate(${rotation.value}deg)`,
  transition: isDragging.value ? 'none' : 'transform 0.15s ease'
}))

// 缩放操作
function zoomIn() {
  scale.value = Math.min(scale.value + scaleStep, maxScale)
}

function zoomOut() {
  scale.value = Math.max(scale.value - scaleStep, minScale)
}

function resetZoom() {
  scale.value = 1
  position.value = { x: 0, y: 0 }
  rotation.value = 0
}

// 旋转操作
function rotateLeft() {
  rotation.value -= 90
}

function rotateRight() {
  rotation.value += 90
}

// 滚轮缩放
function handleWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -scaleStep : scaleStep
  scale.value = Math.max(minScale, Math.min(scale.value + delta, maxScale))
}

// 拖拽操作
function startDrag(e: MouseEvent) {
  isDragging.value = true
  dragStart.value = {
    x: e.clientX - position.value.x,
    y: e.clientY - position.value.y
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value) return
  position.value = {
    x: e.clientX - dragStart.value.x,
    y: e.clientY - dragStart.value.y
  }
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 关闭操作
function close() {
  emit('update:visible', false)
  emit('close')
}

// ESC 关闭
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) {
    close()
  }
}

// 监听 visible 变化，重置状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    resetZoom()
  }
})

// 键盘事件
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})
</script>

<style lang="scss" scoped>
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(8px);
  z-index: $z-modal-overlay;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}

.image-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.preview-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  cursor: grab;
  user-select: none;

  &:active {
    cursor: grabbing;
  }
}

.close-btn {
  position: absolute;
  top: $spacing-lg;
  right: $spacing-lg;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  color: $color-text-primary;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: scale(1.1);
  }
}

.action-bar {
  position: absolute;
  bottom: $spacing-lg;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba(0, 0, 0, 0.6);
  border-radius: $radius-full;
  backdrop-filter: blur(8px);
}

.action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  color: $color-text-primary;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  &:active {
    transform: scale(0.95);
  }
}

.zoom-level {
  font-size: $text-sm;
  color: $color-text-secondary;
  padding: 0 $spacing-xs;
  min-width: 45px;
  text-align: center;
}

.action-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 $spacing-xs;
}

// 淡入淡出动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;

  .preview-image {
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;

  .preview-image {
    transform: scale(0.9);
    opacity: 0;
  }
}
</style>
