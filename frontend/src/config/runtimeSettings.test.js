import {
  SETTINGS_STORAGE_KEY,
  buildApiUrl,
  loadStoredSettings,
  resetStoredSettings,
  saveStoredSettings
} from '@/config/runtimeSettings'

describe('runtimeSettings', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('saves and loads normalized settings', () => {
    saveStoredSettings({
      apiBaseUrl: ' http://localhost:8080/ ',
      defaultUserId: 'user002',
      defaultStreamInterval: 45
    })

    expect(loadStoredSettings()).toEqual({
      apiBaseUrl: 'http://localhost:8080/',
      defaultUserId: 'user002',
      defaultStreamInterval: 45
    })
  })

  it('builds compatible api url when base already contains api prefix', () => {
    window.localStorage.setItem(
      SETTINGS_STORAGE_KEY,
      JSON.stringify({
        apiBaseUrl: 'http://localhost:8080/api',
        defaultUserId: 'user001',
        defaultStreamInterval: 30
      })
    )

    expect(buildApiUrl('/api/chat/message')).toBe('http://localhost:8080/api/chat/message')
  })

  it('resets to defaults', () => {
    saveStoredSettings({
      apiBaseUrl: 'http://localhost:8080',
      defaultUserId: 'user003',
      defaultStreamInterval: 60
    })

    const defaults = resetStoredSettings()

    expect(defaults.defaultUserId).toBe('user001')
    expect(defaults.defaultStreamInterval).toBe(30)
  })
})
