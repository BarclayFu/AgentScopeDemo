import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_SETTINGS,
  loadStoredSettings,
  normalizeSettings,
  resetStoredSettings,
  saveStoredSettings
} from '@/config/runtimeSettings'

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref(loadStoredSettings())

  const hasCustomApiBaseUrl = computed(() => Boolean(settings.value.apiBaseUrl))

  function updateDraft(partial) {
    settings.value = normalizeSettings({
      ...settings.value,
      ...partial
    })
  }

  function saveSettings(partial = {}) {
    settings.value = saveStoredSettings({
      ...settings.value,
      ...partial
    })
    return settings.value
  }

  function resetSettings() {
    settings.value = resetStoredSettings()
    return settings.value
  }

  function restoreFromStorage() {
    settings.value = loadStoredSettings()
    return settings.value
  }

  return {
    settings,
    defaults: DEFAULT_SETTINGS,
    hasCustomApiBaseUrl,
    updateDraft,
    saveSettings,
    resetSettings,
    restoreFromStorage
  }
})
