const storage = {}

const localStorageMock = {
  getItem(key) {
    return Object.prototype.hasOwnProperty.call(storage, key) ? storage[key] : null
  },
  setItem(key, value) {
    storage[key] = String(value)
  },
  removeItem(key) {
    delete storage[key]
  },
  clear() {
    Object.keys(storage).forEach(key => delete storage[key])
  }
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true
})
