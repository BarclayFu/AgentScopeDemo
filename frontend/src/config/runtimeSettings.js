export const SETTINGS_STORAGE_KEY = 'agentscope.admin.settings'

export const DEFAULT_SETTINGS = Object.freeze({
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',
  defaultUserId: 'user001',
  defaultStreamInterval: 30
})

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function normalizeStreamInterval(value) {
  const parsed = Number.parseInt(value, 10)
  if (Number.isNaN(parsed)) {
    return DEFAULT_SETTINGS.defaultStreamInterval
  }
  return Math.min(100, Math.max(0, parsed))
}

export function normalizeSettings(settings = {}) {
  return {
    apiBaseUrl: typeof settings.apiBaseUrl === 'string'
      ? settings.apiBaseUrl.trim()
      : DEFAULT_SETTINGS.apiBaseUrl,
    defaultUserId: settings.defaultUserId || DEFAULT_SETTINGS.defaultUserId,
    defaultStreamInterval: normalizeStreamInterval(settings.defaultStreamInterval)
  }
}

export function loadStoredSettings() {
  if (!canUseStorage()) {
    return normalizeSettings(DEFAULT_SETTINGS)
  }

  const raw = window.localStorage.getItem(SETTINGS_STORAGE_KEY)
  if (!raw) {
    return normalizeSettings(DEFAULT_SETTINGS)
  }

  try {
    return normalizeSettings({
      ...DEFAULT_SETTINGS,
      ...JSON.parse(raw)
    })
  } catch (error) {
    console.warn('Failed to parse stored settings:', error)
    return normalizeSettings(DEFAULT_SETTINGS)
  }
}

export function saveStoredSettings(settings) {
  const normalized = normalizeSettings({
    ...loadStoredSettings(),
    ...settings
  })

  if (canUseStorage()) {
    window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(normalized))
  }

  return normalized
}

export function resetStoredSettings() {
  const defaults = normalizeSettings(DEFAULT_SETTINGS)
  if (canUseStorage()) {
    window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(defaults))
  }
  return defaults
}

function trimTrailingSlash(value) {
  return value.replace(/\/+$/, '')
}

export function resolveApiBaseUrl() {
  return trimTrailingSlash(loadStoredSettings().apiBaseUrl || '')
}

export function buildApiUrl(path) {
  if (/^https?:\/\//.test(path)) {
    return path
  }

  const baseUrl = resolveApiBaseUrl()
  if (!baseUrl) {
    return path
  }

  if (baseUrl.endsWith('/api') && path.startsWith('/api/')) {
    return `${baseUrl}${path.slice(4)}`
  }

  return `${baseUrl}${path}`
}
