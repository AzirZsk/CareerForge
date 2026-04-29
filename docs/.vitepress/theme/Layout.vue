<script setup>
import DefaultTheme from 'vitepress/theme'
import { onMounted } from 'vue'
import { useRouter } from 'vitepress'
import mediumZoom from 'medium-zoom'

const { Layout } = DefaultTheme
const router = useRouter()

const initZoom = () => {
  mediumZoom('[data-zoomable]', {
    background: 'transparent',
  })
}

onMounted(initZoom)
router.onAfterRouteChanged = initZoom
</script>

<template>
  <Layout />
</template>

<style>
.medium-zoom-overlay {
  backdrop-filter: blur(5rem);
}

.medium-zoom-overlay,
.medium-zoom-image--opened {
  z-index: 999;
}
</style>
