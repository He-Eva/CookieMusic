<template>
  <yin-login-logo></yin-login-logo>
  <div class="sign">
    <div class="sign-head">
      <span>帐号登录</span>
    </div>
    <el-radio-group v-model="loginType" style="margin-bottom: 12px">
      <el-radio-button label="user">用户登录</el-radio-button>
      <el-radio-button label="admin">管理员登录</el-radio-button>
    </el-radio-group>
    <el-form ref="signInForm" status-icon :model="registerForm" :rules="SignInRules">
      <el-form-item prop="username">
        <el-input placeholder="用户名" v-model="registerForm.username"></el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input type="password" placeholder="密码" v-model="registerForm.password" @keyup.enter="handleLoginIn"></el-input>
      </el-form-item>
      <el-form-item class="sign-btn">
        <el-button @click="handleSignUp">注册</el-button>
        <el-button type="primary" @click="handleLoginIn">登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts">
import { defineComponent, reactive, getCurrentInstance, ref } from "vue";
import mixin from "@/mixins/mixin";
import YinLoginLogo from "@/components/layouts/YinLoginLogo.vue";
import { HttpManager } from "@/api";
import { NavName, RouterName, SignInRules } from "@/enums";

export default defineComponent({
  components: {
    YinLoginLogo,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const { routerManager, changeIndex } = mixin();

    // 登录用户名密码
    const registerForm = reactive({
      username: "",
      password: "",
    });
    const loginType = ref("user");
    async function handleLoginIn() {
      let canRun = true;
      (proxy.$refs["signInForm"] as any).validate((valid) => {
        if (!valid) return (canRun = false);
      });
      if (!canRun) return;


      try {
        const result = (await (loginType.value === "admin"
          ? HttpManager.adminSignIn({
              username: registerForm.username,
              password: registerForm.password,
            })
          : HttpManager.signIn({
              username: registerForm.username,
              password: registerForm.password,
            }))) as ResponseBody;
        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });

        if (result.success) {
          proxy.$store.commit("setToken", true);
          if (loginType.value === "admin") {
            proxy.$store.commit("setIsAdmin", true);
            proxy.$store.commit("setUserId", "");
            proxy.$store.commit("setUsername", registerForm.username);
            proxy.$store.commit("setUserPic", "/user01/consumer/img/default.jpg");
            changeIndex(NavName.AdminAudit);
            routerManager(RouterName.AdminDashboard, { path: RouterName.AdminDashboard });
          } else {
            proxy.$store.commit("setIsAdmin", false);
            proxy.$store.commit("setUserId", result.data[0].id);
            proxy.$store.commit("setUsername", result.data[0].username);
            proxy.$store.commit("setUserPic", result.data[0].avator);
            changeIndex(NavName.Home);
            routerManager(RouterName.Home, { path: RouterName.Home });
          }
        }
      } catch (error) {
        console.error(error);
      }
    }

    function handleSignUp() {
      routerManager(RouterName.SignUp, { path: RouterName.SignUp });
    }

    return {
      registerForm,
      loginType,
      SignInRules,
      handleLoginIn,
      handleSignUp,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/sign.scss";
</style>
