import { mount } from '@vue/test-utils'
import KnowledgeView from '@/views/KnowledgeView.vue'

vi.mock('@/api', () => ({
  getKnowledgeEntries: vi.fn(),
  getKnowledgeStatus: vi.fn(),
  createKnowledgeEntry: vi.fn(),
  deleteKnowledgeEntry: vi.fn(),
  rebuildKnowledgeBase: vi.fn()
}))

import {
  createKnowledgeEntry,
  getKnowledgeEntries,
  getKnowledgeStatus
} from '@/api'

function flushPromises() {
  return new Promise(resolve => {
    window.setTimeout(resolve, 0)
  })
}

describe('KnowledgeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders knowledge entries and status', async () => {
    getKnowledgeEntries.mockResolvedValue({
      entries: [
        {
          entryId: 'kb-1',
          title: '退换货政策',
          contentPreview: '7天无理由退货',
          source: 'seed',
          type: 'text',
          createdAt: 1710000000000,
          updatedAt: 1710000005000
        }
      ],
      total: 1,
      checkedAt: 1710000010000
    })
    getKnowledgeStatus.mockResolvedValue({
      initialized: true,
      totalEntries: 1,
      lastUpdatedAt: 1710000005000,
      lastRebuildAt: 1710000010000,
      lastOperationMessage: '知识库已刷新',
      checkedAt: 1710000010000
    })

    const wrapper = mount(KnowledgeView)
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('退换货政策')
    expect(wrapper.text()).toContain('知识库状态')
    expect(wrapper.text()).toContain('知识库已刷新')
  })

  it('creates a knowledge entry from the form', async () => {
    getKnowledgeEntries.mockResolvedValue({
      entries: [],
      total: 0,
      checkedAt: 1710000010000
    })
    getKnowledgeStatus.mockResolvedValue({
      initialized: true,
      totalEntries: 0,
      lastUpdatedAt: null,
      lastRebuildAt: null,
      lastOperationMessage: '',
      checkedAt: 1710000010000
    })
    createKnowledgeEntry.mockResolvedValue({
      message: '知识条目已创建',
      entryId: 'kb-2',
      checkedAt: 1710000020000
    })

    const wrapper = mount(KnowledgeView)
    await flushPromises()
    await flushPromises()

    const inputs = wrapper.findAll('input.text-input')
    await inputs[0].setValue('发票说明')
    await wrapper.find('textarea.text-area').setValue('支持开具电子发票')
    const buttons = wrapper.findAll('button.primary-btn')
    await buttons[1].trigger('click')
    await flushPromises()

    expect(createKnowledgeEntry).toHaveBeenCalled()
    expect(wrapper.text()).toContain('知识条目已创建')
  })
})
