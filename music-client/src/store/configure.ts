/**
 * Vuex 模块：全站 UI 与登录标记（非播放业务）
 * - token：是否已登录（同步 localStorage）
 * - searchWord：顶栏搜索后写入，搜索页读取
 * - showAside：右侧「当前播放列表」显隐
 * - activeNavName：顶栏导航高亮项
 */
export default {
  state: {
    token: localStorage.getItem("token") === "true",
    showAside: false, // 是否显示侧边栏
    searchWord: "", // 搜索关键词
    activeNavName: "", // 导航栏名称
  },
  getters: {
    token: (state) => state.token,
    activeNavName: (state) => state.activeNavName,
    showAside: (state) => state.showAside,
    searchWord: (state) => state.searchWord,
  },
  mutations: {
    setToken: (state, token) => {
      state.token = token;
      localStorage.setItem("token", String(token));
    },
    setActiveNavName: (state, activeNavName) => {
      state.activeNavName = activeNavName;
    },
    setShowAside: (state, showAside) => {
      state.showAside = showAside;
    },
    setSearchWord: (state, searchWord) => {
      state.searchWord = searchWord;
    },
  },
};
