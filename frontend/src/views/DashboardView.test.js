import { mount } from '@vue/test-utils'
import DashboardView from '@/views/DashboardView.vue'

vi.mock('@/api', () => ({
  getActiveSessionCount: vi.fn(),
  getMonitoringStats: vi.fn(),
  getSystemStatus: vi.fn()
}))

import { getActiveSessionCount, getMonitoringStats, getSystemStatus } from '@/api'

function flushPromises() {
  return new Promise(resolve => {
    window.setTimeout(resolve, 0)
  })
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders monitoring summary after loading', async () => {
    getActiveSessionCount.mockResolvedValue({ activeSessions: 2 })
    getMonitoringStats.mockResolvedValue({
      summary: {
        activeSessions: 2,
        totalMessages: 9,
        totalToolCalls: 4,
        errorCount: 1,
        avgResponseTimeMs: 180,
        lastMessageAt: 1710000000000,
        lastErrorAt: 1710000005000
      },
      checkedAt: 1710000009000
    })
    getSystemStatus.mockResolvedValue({
      service: 'Customer Service Agent',
      status: 'UP',
      checkedAt: 1710000009000
    })

    const wrapper = mount(DashboardView)
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('活跃会话数')
    expect(wrapper.text()).toContain('工具调用数')
    expect(wrapper.text()).toContain('Customer Service Agent / UP')
    expect(wrapper.text()).toContain('9')
  })

  it('shows retry state when api call fails', async () => {
    getActiveSessionCount.mockResolvedValue({ activeSessions: 0 })
    getMonitoringStats.mockRejectedValue(new Error('network failed'))
    getSystemStatus.mockResolvedValue({
      service: 'Customer Service Agent',
      status: 'UP',
      checkedAt: 1710000009000
    })

    const wrapper = mount(DashboardView)
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('监控数据加载失败')
    expect(wrapper.text()).toContain('network failed')
  })

  it('supports legacy string monitoring responses', async () => {
    getActiveSessionCount.mockResolvedValue({ activeSessions: 4 })
    getMonitoringStats.mockResolvedValue('Total Messages: 15 | Total Tool Calls: 6')
    getSystemStatus.mockResolvedValue('Customer Service Agent is running. Total Messages: 15 | Total Tool Calls: 6')

    const wrapper = mount(DashboardView)
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('15')
    expect(wrapper.text()).toContain('6')
    expect(wrapper.text()).toContain('Customer Service Agent / UP')
  })
})
