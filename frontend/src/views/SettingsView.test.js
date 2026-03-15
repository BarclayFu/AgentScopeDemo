import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import SettingsView from '@/views/SettingsView.vue'

describe('SettingsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
  })

  it('saves settings to local storage', async () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [createPinia()]
      }
    })

    const input = wrapper.find('input.setting-input')
    await input.setValue('http://localhost:8080')

    const select = wrapper.find('select.setting-select')
    await select.setValue('user003')

    const saveButton = wrapper.find('button.primary-btn')
    await saveButton.trigger('click')

    expect(window.localStorage.getItem('agentscope.admin.settings')).toContain('user003')
    expect(wrapper.text()).toContain('设置已保存')
  })
})
