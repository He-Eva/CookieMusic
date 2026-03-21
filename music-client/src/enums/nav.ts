import { RouterName } from "./router-name";

export const enum NavName {
  Home = "首页",
  SongSheet = "歌单",
  Singer = "歌手",
  Community = "笔记社区",
  Social = "社交中心",
  AdminAudit = "审核工作台",
  Personal = "个人主页",
  Setting = "设置",
  SignIn = "登录",
  SignUp = "注册",
  SignOut = "退出",
}

// 左侧导航栏
export const HEADERNAVLIST = [
  {
    name: NavName.Home,
    path: RouterName.Home,
  },
  {
    name: NavName.SongSheet,
    path: RouterName.SongSheet,
  },
  {
    name: NavName.Singer,
    path: RouterName.Singer,
  },
  {
    name: NavName.Community,
    path: RouterName.Community,
  },
  {
    name: NavName.Social,
    path: RouterName.Social,
  },
];

// 管理员模式导航栏（仅审核相关）
export const ADMIN_HEADERNAVLIST = [
  {
    name: NavName.AdminAudit,
    path: RouterName.AdminDashboard,
  },
];

// 右侧导航栏
export const SIGNLIST = [
  {
    name: NavName.SignIn,
    path: RouterName.SignIn,
  },
  {
    name: NavName.SignUp,
    path: RouterName.SignUp,
  },
];

// 用户下拉菜单项
export const MENULIST = [
  {
    name: NavName.Personal,
    path: RouterName.Personal,
  },
  {
    name: NavName.Setting,
    path: RouterName.Setting,
  },
  {
    name: NavName.SignOut,
    path: RouterName.SignOut,
  },
];
