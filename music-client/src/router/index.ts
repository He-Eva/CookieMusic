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
  {
    path: "/",
    name: "yin-container",
    component: () => import("@/views/YinContainer.vue"),
    children: [
      {
        path: "/",
        name: "home",
        component: () => import("@/views/Home.vue"),
      },
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
      {
        path: "/personal",
        name: "personal",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/personal/Personal.vue"),
      },
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
      {
        path: "/lyric/:id",
        name: "lyric",
        component: () => import("@/views/Lyric.vue"),
      },
      {
        path: "/search",
        name: "search",
        component: () => import("@/views/search/Search.vue"),
      },
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

router.beforeEach((to, _from, next) => {
  const requireAuth = to.matched.some((record) => record.meta && (record.meta as any).requireAuth);
  const adminOnly = to.matched.some((record) => record.meta && (record.meta as any).adminOnly);
  const token = store.getters.token;
  const isAdmin = Boolean(store.getters.isAdmin) || localStorage.getItem("cm_isAdmin") === "true";

  // 管理员模式：优先使用后台路由
  const adminAllowedPrefix = ["/admin", "/setting", "/sign-in", "/404"];
  const isAdminAllowed = adminAllowedPrefix.some((prefix) => to.path.startsWith(prefix));

  if (requireAuth && !token) {
    next({ path: "/sign-in" });
  } else if (isAdmin && !isAdminAllowed) {
    next({ path: "/admin/post-audit" });
  } else if (adminOnly && !isAdmin) {
    next({ path: "/" });
  } else {
    next();
  }
});

export default router;
