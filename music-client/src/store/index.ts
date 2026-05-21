/** Vuex 入口：合并 configure（全站 UI）、user（登录用户）、song（播放器） */
import { createStore } from "vuex";
import configure from "./configure";
import user from "./user";
import song from "./song";

export default createStore({
  modules: {
    configure,
    user,
    song,
  },
});
