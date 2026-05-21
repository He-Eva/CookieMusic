<!-- 【布局】管理后台侧栏 + router-view；路由 /admin/* -->
<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <div class="admin-logo" @click="goDashboard">Online Music 管理后台</div>
      <el-menu :default-active="activePath" router class="admin-menu">
        <el-menu-item index="/admin/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/admin/post-audit">帖子审核</el-menu-item>
        <el-menu-item index="/admin/user">用户管理</el-menu-item>
        <el-menu-item index="/admin/comment">评论管理</el-menu-item>
        <el-menu-item index="/admin/singer">歌手管理</el-menu-item>
        <el-menu-item index="/admin/song">歌曲管理</el-menu-item>
        <el-menu-item index="/admin/song-list">歌单管理</el-menu-item>
      </el-menu>
    </aside>
    <section class="admin-main">
      <header class="admin-topbar">
        <div class="title">管理员工作台</div>
        <div class="actions">
          <el-button size="small" @click="goFront">返回前台</el-button>
          <el-button size="small" @click="goSetting">改密</el-button>
          <el-button size="small" type="danger" @click="logout">退出</el-button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { HttpManager } from "@/api";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance() as any;
const activePath = computed(() => route.path);

function goDashboard() {
  router.push("/admin/dashboard");
}

function goFront() {
  router.push("/");
}

function goSetting() {
  router.push("/setting");
}

async function logout() {
  try {
    await HttpManager.adminLogout();
  } finally {
    proxy?.$store?.commit("setToken", false);
    proxy?.$store?.commit("clearUser");
    router.push("/sign-in");
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f4f6fb;
  font-size: 17px;
}

.admin-side {
  width: 232px;
  background: #1f2937;
  color: #fff;
}
.admin-logo {
  height: 56px;
  line-height: 56px;
  font-size: 20px;
  text-align: center;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  cursor: pointer;
}
.admin-menu {
  border-right: none;
  --el-menu-bg-color: #1f2937;
  --el-menu-text-color: #c7d2fe;
  --el-menu-active-color: #fff;
}
.admin-menu :deep(.el-menu-item) {
  font-size: 17px;
  height: 48px;
  line-height: 48px;
}
.admin-main {
  flex: 1;
  min-width: 0;
}
.admin-topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.title {
  font-size: 21px;
  font-weight: 600;
  color: #303133;
}
.actions {
  display: flex;
  gap: 8px;
}
.actions :deep(.el-button) {
  font-size: 16px;
  padding: 8px 16px;
}
.admin-content {
  padding: 14px;
}

/* 子页面通用：表格、表单、分页、弹窗等整体加大约 2 档 */
.admin-content :deep(.el-table),
.admin-content :deep(.el-table th.el-table__cell),
.admin-content :deep(.el-table td.el-table__cell) {
  font-size: 17px;
}
.admin-content :deep(.el-table .cell) {
  line-height: 1.45;
}
.admin-content :deep(.el-button) {
  font-size: 16px;
}
.admin-content :deep(.el-input__wrapper),
.admin-content :deep(.el-select__wrapper),
.admin-content :deep(.el-textarea__inner) {
  font-size: 17px;
}
.admin-content :deep(.el-form-item__label) {
  font-size: 17px;
}
.admin-content :deep(.el-pagination),
.admin-content :deep(.el-pagination .el-pagination__total),
.admin-content :deep(.el-pagination__sizes .el-input__inner) {
  font-size: 16px;
}
.admin-content :deep(.el-dialog__title) {
  font-size: 19px;
}
.admin-content :deep(.el-dialog__body) {
  font-size: 17px;
}
.admin-content :deep(.el-tag) {
  font-size: 15px;
}
.admin-content :deep(.el-empty__description) {
  font-size: 16px;
}
.admin-content :deep(.el-card__body) {
  font-size: 17px;
}
</style>
