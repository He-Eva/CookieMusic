const { defineConfig } = require('@vue/cli-service')

/** 后端 API 路径前缀，开发时由 devServer 代理到 Spring Boot */
const API_PREFIXES = [
  'admin',
  'user',
  'song',
  'songList',
  'listSong',
  'post',
  'comment',
  'collection',
  'rankList',
  'banner',
  'follow',
  'followings',
  'followers',
  'playRecord',
  'recommend',
  'singer',
  'lyric',
  'excle',
  'user01',
  'img',
  'userSupport',
  'download',
]

function buildProxy() {
  const target = process.env.VUE_APP_BACKEND || 'http://localhost:8888'
  const proxy = {}
  for (const prefix of API_PREFIXES) {
    proxy[`/${prefix}`] = {
      target,
      changeOrigin: true,
      ws: true,
    }
  }
  return proxy
}

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8080,
    proxy: buildProxy(),
    // Element Plus 表格在 Tab 切换时会触发 ResizeObserver，属无害警告；不弹出 dev overlay
    client: {
      overlay: {
        runtimeErrors: (error) => {
          const msg = error && error.message ? String(error.message) : ''
          if (/ResizeObserver/i.test(msg)) return false
          return true
        },
      },
    },
  },
  chainWebpack: (config) => {
    config.resolve.alias.set('@', __dirname + '/src')

    config.plugin('define').tap((definitions) => {
      // 开发：请求发到当前 devServer 同源，由 proxy 转发，JSESSIONID 才能带上
      // 生产：直连后端（可用环境变量 VUE_APP_API_BASE 覆盖）
      const isProd = process.env.NODE_ENV === 'production'
      const nodeHost = isProd
        ? process.env.VUE_APP_API_BASE || 'http://localhost:8888'
        : process.env.VUE_APP_DEV_HOST || 'http://localhost:8080'

      Object.assign(definitions[0]['process.env'], {
        NODE_HOST: JSON.stringify(nodeHost),
      })
      return definitions
    })
  },
})
