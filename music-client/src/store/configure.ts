export default {
  state: {
    token: localStorage.getItem("cm_token") === "1", // 用户是否登录（持久化）
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
      const v = !!token;
      state.token = v;
      localStorage.setItem("cm_token", v ? "1" : "0");
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
