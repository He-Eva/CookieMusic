<template>
  <div class="setting">
    <h1>设置</h1>
    <el-tabs tab-position="left">
      <el-tab-pane v-if="!isAdmin" label="个人资料" class="content">
        <Personal-data></Personal-data>
      </el-tab-pane>
      <el-tab-pane v-if="!isAdmin" label="更改密码" class="content">
        <Password></Password>
      </el-tab-pane>
      <el-tab-pane v-if="isAdmin" label="管理员改密" class="content">
        <AdminPassword></AdminPassword>
      </el-tab-pane>
      <el-tab-pane label="账号和安全" class="content">
        <div class="content-account">
          <el-button type="danger" :icon="Delete" @click="cancelAccount">注销账号</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, computed, reactive } from "vue";
import { Delete } from "@element-plus/icons-vue";
import PersonalData from "./PersonalData.vue";
import Password from "./Password.vue";
import AdminPassword from "./AdminPassword.vue";
import { HttpManager } from "@/api";
import { useStore } from "vuex";
import mixin from "@/mixins/mixin";
import { RouterName } from "@/enums";

export default defineComponent({
  components: {
    PersonalData,
    Password,
    AdminPassword,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const { routerManager } = mixin();

    const userId = computed(() => store.getters.userId);
    const isAdmin = computed(() => {
      return Boolean(store.getters.isAdmin) || localStorage.getItem("cm_isAdmin") === "true";
    });
    async function cancelAccount() {
      if (!isAdmin.value) {
        const result = (await HttpManager.deleteUser(userId.value)) as ResponseBody;
        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });
      } else {
        const result = (await HttpManager.adminLogout()) as ResponseBody;
        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });
      }
      routerManager(RouterName.SignIn, { path: RouterName.SignIn });
      proxy.$store.commit("setToken", false);
      proxy.$store.commit("clearUser");
    }
    return {
      Delete,
      cancelAccount,
      isAdmin,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.setting {
  font-size: 17px;

  h1 {
    font-size: 28px;
    font-weight: 600;
    margin: 0 0 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid $color-grey;
  }

  /* 左侧 Tab */
  :deep(.el-tabs--left) {
    .el-tabs__header {
      width: 140px;
      margin-right: 24px;
    }
    .el-tabs__nav-wrap::after {
      display: none;
    }
    .el-tabs__item {
      font-size: 17px;
      height: 48px;
      line-height: 48px;
      padding: 0 16px;
      justify-content: flex-start;
    }
    .el-tabs__content {
      flex: 1;
      min-width: 0;
    }
  }

  :deep(.el-form-item__label) {
    font-size: 17px;
    line-height: 40px;
    white-space: nowrap;
    color: #606266;
  }

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner),
  :deep(.el-select__wrapper) {
    font-size: 17px;
    min-height: 40px;
  }

  :deep(.el-textarea__inner) {
    min-height: 88px;
    padding: 10px 12px;
    line-height: 1.5;
  }

  :deep(.el-button) {
    font-size: 17px;
    padding: 10px 22px;
  }

  :deep(.el-radio-group) {
    display: flex;
    align-items: center;
    min-height: 40px;
    gap: 8px;
  }

  :deep(.el-radio__label) {
    font-size: 17px;
  }

  :deep(.el-date-editor) {
    width: 100%;
  }

  :deep(.el-form-item__error) {
    font-size: 14px;
    padding-top: 4px;
  }

  /* 表单区域：左对齐、限宽居中，避免标签换行与全屏拉满 */
  :deep(.setting-form) {
    max-width: 560px;
    margin: 0 auto;
    text-align: left;
  }

  :deep(.setting-form .el-form-item:last-child .el-form-item__content) {
    display: flex;
    justify-content: flex-start;
    gap: 12px;
    padding-top: 8px;
  }
}

.content {
  padding: 8px 16px 24px;
  text-align: left;
}

.content-account {
  max-width: 560px;
  margin: 0 auto;
  padding-top: 8px;
}

@media screen and (min-width: $sm) {
  .setting {
    margin: 30px 10%;
    margin-top: 0;
    padding: 20px;
    min-height: 60vh;
  }
}

@media screen and (max-width: $sm) {
  .setting {
    padding: 20px;
  }
}
</style>
