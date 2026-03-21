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
