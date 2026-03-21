<template>
  <el-form ref="passwordForm" label-width="90px" :model="form" :rules="rules">
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input type="password" v-model="form.oldPassword" />
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword">
      <el-input type="password" v-model="form.newPassword" />
    </el-form-item>
    <el-form-item label="确认新密码" prop="confirmPassword">
      <el-input type="password" v-model="form.confirmPassword" />
    </el-form-item>
    <el-form-item>
      <el-button @click="clearData">重置</el-button>
      <el-button type="primary" @click="confirm">确认修改</el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, reactive } from "vue";
import { HttpManager } from "@/api";
import { validatePassword } from "@/enums";

export default defineComponent({
  setup() {
    const { proxy } = getCurrentInstance() as any;

    const form = reactive({
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    });

    const validateCheck = (_rule: any, value: any, callback: any) => {
      if (value === "") {
        callback(new Error("密码不能为空"));
      } else if (value !== form.newPassword) {
        callback(new Error("两次输入的新密码不一致"));
      } else {
        callback();
      }
    };

    const rules = reactive({
      oldPassword: [{ validator: validatePassword, trigger: "blur", min: 3 }],
      newPassword: [{ validator: validatePassword, trigger: "blur", min: 3 }],
      confirmPassword: [{ validator: validateCheck, trigger: "blur", min: 3 }],
    });

    function clearData() {
      form.oldPassword = "";
      form.newPassword = "";
      form.confirmPassword = "";
    }

    async function confirm() {
      let canRun = true;
      (proxy.$refs["passwordForm"] as any).validate((valid: boolean) => {
        if (!valid) canRun = false;
      });
      if (!canRun) return;

      const result = (await HttpManager.adminUpdatePassword({
        oldPassword: form.oldPassword,
        password: form.newPassword,
      })) as ResponseBody;
      proxy.$message({
        message: result.message,
        type: result.type,
      });
      if (result.success) {
        clearData();
      }
    }

    return {
      form,
      rules,
      clearData,
      confirm,
    };
  },
});
</script>

<style scoped></style>

