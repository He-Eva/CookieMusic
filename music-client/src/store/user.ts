export default {
  state: {
    userId: localStorage.getItem("cm_userId") || "", // ID（持久化）
    username: localStorage.getItem("cm_username") || "", // 名字（持久化）
    userPic: localStorage.getItem("cm_userPic") || "", // 图片（持久化）
    isAdmin: localStorage.getItem("cm_isAdmin") === "true",
  },
  getters: {
    userId: (state) => state.userId,
    username: (state) => state.username,
    userPic: (state) => state.userPic,
    isAdmin: (state) => state.isAdmin,
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
    setIsAdmin: (state, isAdmin) => {
      const flag = Boolean(isAdmin);
      state.isAdmin = flag;
      localStorage.setItem("cm_isAdmin", String(flag));
    },
    clearUser: (state) => {
      state.userId = "";
      state.username = "";
      state.userPic = "";
      state.isAdmin = false;
      localStorage.removeItem("cm_userId");
      localStorage.removeItem("cm_username");
      localStorage.removeItem("cm_userPic");
      localStorage.removeItem("cm_isAdmin");
    },
  },
};
