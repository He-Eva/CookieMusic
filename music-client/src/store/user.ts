export default {
  state: {
    userId: localStorage.getItem("cm_userId") || "", // ID（持久化）
    username: localStorage.getItem("cm_username") || "", // 名字（持久化）
    userPic: localStorage.getItem("cm_userPic") || "", // 图片（持久化）
  },
  getters: {
    userId: (state) => state.userId,
    username: (state) => state.username,
    userPic: (state) => state.userPic,
  },
  mutations: {
    setUserId: (state, userId) => {
      state.userId = userId;
      localStorage.setItem("cm_userId", String(userId ?? ""));
    },
    setUsername: (state, username) => {
      state.username = username;
      localStorage.setItem("cm_username", String(username ?? ""));
    },
    setUserPic: (state, userPic) => {
      state.userPic = userPic;
      localStorage.setItem("cm_userPic", String(userPic ?? ""));
    },
    clearUser: (state) => {
      state.userId = "";
      state.username = "";
      state.userPic = "";
      localStorage.removeItem("cm_userId");
      localStorage.removeItem("cm_username");
      localStorage.removeItem("cm_userPic");
    },
  },
};
