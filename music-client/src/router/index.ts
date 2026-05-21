/**
 * 前端路由配置 + 全局导航守卫
 * - 用户端页面挂在 YinContainer 下（带顶栏、播放条）
 * - 管理端 /admin 独立布局（AdminContainer）
 * - meta.requireAuth：需登录；meta.adminOnly：需管理员
 */
import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import store from "@/store";
const routes: Array<RouteRecordRaw> = [
  {
    path: "/:pathMatch(.*)*",
    redirect: "/404",
  },
  {
    path: "/404",
    component: () => import("@/views/error/404.vue"),
  },
  // ========== 用户端主布局（顶栏 + 底部播放器） ==========
  {
    path: "/",
    name: "yin-container",
    component: () => import("@/views/YinContainer.vue"),
    children: [
      // 首页：轮播、猜你喜欢推荐
      {
        path: "/",
        name: "home",
        component: () => import("@/views/Home.vue"),
      },
      // 登录 / 注册
      {
        path: "/sign-in",
        name: "sign-in",
        component: () => import("@/views/SignIn.vue"),
      },
      {
        path: "/sign-up",
        name: "sign-up",
        component: () => import("@/views/SignUp.vue"),
      },
      // 个人中心（需登录）
      {
        path: "/personal",
        name: "personal",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/personal/Personal.vue"),
      },
      // 歌单
      {
        path: "/song-sheet",
        name: "song-sheet",
        component: () => import("@/views/song-sheet/SongSheet.vue"),
      },
      {
        path: "/song-sheet-detail/:id",
        name: "song-sheet-detail",
        component: () => import("@/views/song-sheet/SongSheetDetail.vue"),
      },
      // 歌手
      {
        path: "/singer",
        name: "singer",
        component: () => import("@/views/singer/Singer.vue"),
      },
      {
        path: "/singer-detail/:id",
        name: "singer-detail",
        component: () => import("@/views/singer/SingerDetail.vue"),
      },
      // 歌词页（与底部播放条联动）
      {
        path: "/lyric/:id",
        name: "lyric",
        component: () => import("@/views/Lyric.vue"),
      },
      // 搜索
      {
        path: "/search",
        name: "search",
        component: () => import("@/views/search/Search.vue"),
      },
      // 社区笔记
      {
        path: "/community",
        name: "community",
        component: () => import("@/views/community/Community.vue"),
      },
      {
        path: "/social",
        name: "social",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/community/SocialCenter.vue"),
      },
      {
        path: "/community/publish",
        name: "community-publish",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/community/CommunityPublish.vue"),
      },
      {
        path: "/community/detail/:id",
        name: "community-detail",
        component: () => import("@/views/community/CommunityDetail.vue"),
      },
      {
        path: "/personal-data",
        name: "personal-data",
        component: () => import("@/views/setting/PersonalData.vue"),
      },
      // 用户设置（资料、改密、注销）
      {
        path: "/setting",
        name: "setting",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/setting/Setting.vue"),
        children: [
          {
            path: "/setting/PersonalData",
            name: "personalData",
            meta: {
              requireAuth: true,
            },
            component: () => import("@/views/setting/PersonalData.vue"),
          }
        ]
      },
    ],
  },
  // ========== 管理后台（独立侧栏，无 YinHeader） ==========
  {
    path: "/admin",
    component: () => import("@/views/admin/AdminContainer.vue"),
    meta: {
      requireAuth: true,
      adminOnly: true,
    },
    children: [
      {
        path: "",
        redirect: "/admin/dashboard",
      },
      {
        path: "dashboard",
        name: "admin-dashboard",
        component: () => import("@/views/admin/Dashboard.vue"),
      },
      {
        path: "post-audit",
        name: "admin-post-audit",
        component: () => import("@/views/admin/PostAudit.vue"),
      },
      {
        path: "user",
        name: "admin-user",
        component: () => import("@/views/admin/UserManage.vue"),
      },
      {
        path: "comment",
        name: "admin-comment",
        component: () => import("@/views/admin/CommentManage.vue"),
      },
      // 歌手增删改 + 头像上传
      {
        path: "singer",
        name: "admin-singer",
        component: () => import("@/views/admin/SingerManage.vue"),
      },
      {
        path: "song",
        name: "admin-song-center",
        component: () => import("@/views/admin/SongManage.vue"),
      },
      {
        path: "song-list",
        name: "admin-song-list",
        component: () => import("@/views/admin/SongListManage.vue"),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

// 全局守卫：未登录拦截；非管理员禁止进入 /admin
router.beforeEach((to, _from, next) => {
  const requireAuth = to.matched.some((record) => record.meta && (record.meta as any).requireAuth);
  const adminOnly = to.matched.some((record) => record.meta && (record.meta as any).adminOnly);
  const token = store.getters.token;
  const isAdmin = Boolean(store.getters.isAdmin) || localStorage.getItem("cm_isAdmin") === "true";

  if (requireAuth && !token) {
    next({ path: "/sign-in" });
  } else if (adminOnly && !isAdmin) {
    next({ path: "/" });
  } else {
    next();
  }
});

export default router;
