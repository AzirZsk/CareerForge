<template>
  <div class="datetime-picker" ref="pickerRef">
    <div class="picker-input" :class="{ focused: isOpen, disabled }" @click="togglePicker">
      <span class="picker-icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
          <line x1="16" y1="2" x2="16" y2="6"></line>
          <line x1="8" y1="2" x2="8" y2="6"></line>
          <line x1="3" y1="10" x2="21" y2="10"></line>
        </svg>
      </span>
      <span class="picker-value" :class="{ placeholder: !displayValue }">
        {{ displayValue || placeholder }}
      </span>
      <span v-if="modelValue && clearable" class="clear-btn" @click.stop="clearValue">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </span>
    </div>

    <Teleport to="body">
      <div v-if="isOpen" class="picker-dropdown" :style="dropdownStyle">
        <div class="dropdown-header">
          <button type="button" class="nav-btn" @click="prevMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          <span class="current-month">{{ yearMonthLabel }}</span>
          <button type="button" class="nav-btn" @click="nextMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
        </div>

        <div class="calendar-grid">
          <div class="weekday" v-for="day in weekdays" :key="day">{{ day }}</div>
          <button
            v-for="date in calendarDays"
            :key="date.key"
            type="button"
            class="day-cell"
            :class="{
              'other-month': date.otherMonth,
              'today': date.isToday,
              'selected': date.isSelected
            }"
            @click="selectDate(date)"
          >
            {{ date.day }}
          </button>
        </div>

        <div class="time-picker">
          <div class="time-label">时间</div>
          <div class="time-inputs">
            <select v-model="selectedHour" class="time-select">
              <option v-for="h in hours" :key="h" :value="h">{{ h.padStart(2, '0') }}</option>
            </select>
            <span class="time-separator">:</span>
            <select v-model="selectedMinute" class="time-select">
              <option v-for="m in minutes" :key="m" :value="m">{{ m.padStart(2, '0') }}</option>
            </select>
          </div>
        </div>

        <div class="dropdown-footer">
          <button type="button" class="btn-text" @click="setNow">此刻</button>
          <button type="button" class="btn-primary" @click="confirmSelection">确定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
}>(), {
  placeholder: '选择日期时间',
  disabled: false,
  clearable: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const pickerRef = ref<HTMLElement | null>(null)
const isOpen = ref(false)
const dropdownStyle = ref<Record<string, string>>({})

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const hours = Array.from({ length: 24 }, (_, i) => String(i))
const minutes = Array.from({ length: 60 }, (_, i) => String(i))

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth())
const selectedHour = ref('09')
const selectedMinute = ref('00')
const tempSelectedDate = ref<Date | null>(null)

const yearMonthLabel = computed(() => {
  return `${currentYear.value}年${currentMonth.value + 1}月`
})

const displayValue = computed(() => {
  if (!props.modelValue) return ''
  const date = new Date(props.modelValue)
  if (isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
})

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const selectedDate = tempSelectedDate.value || (props.modelValue ? new Date(props.modelValue) : null)

  const days: Array<{
    key: string
    day: number
    date: Date
    otherMonth: boolean
    isToday: boolean
    isSelected: boolean
  }> = []

  const prevMonth = new Date(year, month, 0)
  const prevMonthDays = prevMonth.getDate()

  for (let i = startWeekday - 1; i >= 0; i--) {
    const day = prevMonthDays - i
    const date = new Date(year, month - 1, day)
    days.push({
      key: `prev-${day}`,
      day,
      date,
      otherMonth: true,
      isToday: false,
      isSelected: false
    })
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const isToday = date.getTime() === today.getTime()
    const isSelected = selectedDate &&
      date.getFullYear() === selectedDate.getFullYear() &&
      date.getMonth() === selectedDate.getMonth() &&
      date.getDate() === selectedDate.getDate()

    days.push({
      key: `curr-${day}`,
      day,
      date,
      otherMonth: false,
      isToday,
      isSelected: !!isSelected
    })
  }

  const remaining = 42 - days.length
  for (let day = 1; day <= remaining; day++) {
    const date = new Date(year, month + 1, day)
    days.push({
      key: `next-${day}`,
      day,
      date,
      otherMonth: true,
      isToday: false,
      isSelected: false
    })
  }

  return days
})

function togglePicker() {
  if (props.disabled) return
  isOpen.value = !isOpen.value

  if (isOpen.value) {
    updateDropdownPosition()
    if (props.modelValue) {
      const date = new Date(props.modelValue)
      if (!isNaN(date.getTime())) {
        currentYear.value = date.getFullYear()
        currentMonth.value = date.getMonth()
        selectedHour.value = String(date.getHours())
        selectedMinute.value = String(date.getMinutes())
        tempSelectedDate.value = date
      }
    }
  }
}

function updateDropdownPosition() {
  if (!pickerRef.value) return
  const rect = pickerRef.value.getBoundingClientRect()
  const dropdownWidth = 280
  const dropdownHeight = 340

  // 计算水平位置
  let left = rect.left
  const spaceRight = window.innerWidth - rect.left
  if (spaceRight < dropdownWidth + 16) {
    // 右边空间不够，往左弹出
    left = Math.max(8, window.innerWidth - dropdownWidth - 16)
  }

  // 计算垂直位置
  const spaceBelow = window.innerHeight - rect.bottom
  const spaceAbove = rect.top

  if (spaceBelow < dropdownHeight && spaceAbove > dropdownHeight) {
    // 下方空间不够，往上弹出
    dropdownStyle.value = {
      position: 'fixed',
      top: `${rect.top - dropdownHeight - 8}px`,
      left: `${left}px`,
      zIndex: '1100'
    }
  } else {
    // 往下弹出
    dropdownStyle.value = {
      position: 'fixed',
      top: `${rect.bottom + 8}px`,
      left: `${left}px`,
      zIndex: '1100'
    }
  }
}

function prevMonth() {
  if (currentMonth.value === 0) {
    currentYear.value--
    currentMonth.value = 11
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentYear.value++
    currentMonth.value = 0
  } else {
    currentMonth.value++
  }
}

function selectDate(date: { date: Date; otherMonth: boolean }) {
  tempSelectedDate.value = date.date
  if (date.otherMonth) {
    currentYear.value = date.date.getFullYear()
    currentMonth.value = date.date.getMonth()
  }
}

function setNow() {
  const now = new Date()
  tempSelectedDate.value = now
  currentYear.value = now.getFullYear()
  currentMonth.value = now.getMonth()
  selectedHour.value = String(now.getHours())
  selectedMinute.value = String(now.getMinutes())
  confirmSelection()
}

function confirmSelection() {
  if (!tempSelectedDate.value) {
    const today = new Date()
    tempSelectedDate.value = today
  }

  const date = new Date(tempSelectedDate.value)
  date.setHours(parseInt(selectedHour.value), parseInt(selectedMinute.value), 0, 0)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  emit('update:modelValue', `${year}-${month}-${day}T${hour}:${minute}`)
  isOpen.value = false
}

function clearValue() {
  emit('update:modelValue', '')
  tempSelectedDate.value = null
}

function handleClickOutside(e: MouseEvent) {
  if (!pickerRef.value) return
  const target = e.target as HTMLElement
  if (!pickerRef.value.contains(target) && !target.closest('.picker-dropdown')) {
    isOpen.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    const date = new Date(val)
    if (!isNaN(date.getTime())) {
      selectedHour.value = String(date.getHours())
      selectedMinute.value = String(date.getMinutes())
    }
  }
}, { immediate: true })

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.datetime-picker {
  position: relative;
  width: 100%;
}

.picker-input {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover:not(.disabled) {
    border-color: rgba($color-accent, 0.3);
  }

  &.focused {
    border-color: $color-accent;
  }

  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.picker-icon {
  display: flex;
  align-items: center;
  color: $color-text-tertiary;
}

.picker-value {
  flex: 1;
  font-size: 0.875rem;
  color: $color-text-primary;

  &.placeholder {
    color: $color-text-tertiary;
  }
}

.clear-btn {
  display: flex;
  align-items: center;
  padding: 2px;
  color: $color-text-tertiary;
  border-radius: 50%;
  transition: all 0.2s;

  &:hover {
    color: $color-text-primary;
    background: rgba($color-text-primary, 0.1);
  }
}

.picker-dropdown {
  width: 280px;
  background: $color-bg-secondary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-bottom: 1px solid $color-bg-tertiary;
}

.current-month {
  font-size: 0.9375rem;
  font-weight: 600;
  color: $color-text-primary;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($color-accent, 0.1);
    color: $color-accent;
  }
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  padding: $spacing-xs;
}

.weekday {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  font-size: 0.7rem;
  font-weight: 500;
  color: $color-text-tertiary;
}

.day-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  font-size: 0.8125rem;
  color: $color-text-primary;
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(.other-month) {
    background: rgba($color-accent, 0.1);
  }

  &.other-month {
    color: $color-text-muted;
  }

  &.today {
    font-weight: 600;
    color: $color-accent;
  }

  &.selected {
    background: $color-accent;
    color: $color-bg-primary;

    &:hover {
      background: $color-accent-light;
    }
  }
}

.time-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-sm $spacing-md;
  border-top: 1px solid $color-bg-tertiary;
}

.time-label {
  font-size: 0.875rem;
  color: $color-text-secondary;
}

.time-inputs {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.time-select {
  width: 60px;
  padding: $spacing-xs $spacing-sm;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-sm;
  color: $color-text-primary;
  font-size: 0.875rem;
  text-align: center;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.time-separator {
  font-size: 0.875rem;
  font-weight: 600;
  color: $color-text-secondary;
}

.dropdown-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  border-top: 1px solid $color-bg-tertiary;
}

.btn-text {
  background: transparent;
  border: none;
  color: $color-text-secondary;
  font-size: 0.875rem;
  cursor: pointer;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  transition: all 0.2s;

  &:hover {
    color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

.btn-primary {
  background: $color-accent;
  border: none;
  color: $color-bg-primary;
  font-size: 0.875rem;
  font-weight: 500;
  padding: $spacing-xs $spacing-md;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: $color-accent-light;
  }
}
</style>
