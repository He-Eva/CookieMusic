<template>
  <div class="upload">
    <el-upload
      drag
      :action="uploadUrl()"
      :show-file-list="false"
      :on-success="handleAvatarSuccess"
      :on-error="handleAvatarError"
      :before-upload="beforeAvatarUpload"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">将文件拖到此处或点击上传</div>
      <template #tip>
        <p class="el-upload__tip">只能上传 {{ uploadTypes.join("、") }} 文件, 且不超过10M</p>
      </template>
    </el-upload>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, getCurrentInstance } from "vue";
import { useStore } from "vuex";
import { UploadFilled } from "@element-plus/icons-vue";
import { HttpManager } from "@/api";

export default defineComponent({
  components: {
    UploadFilled,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();

    const uploadTypes = ref(["jpg", "jpeg", "png", "gif"]);
    const userId = computed(() => store.getters.userId);

    function uploadUrl() {
      if (!userId.value) return "";
      return HttpManager.uploadUrl(userId.value);
    }

    function beforeAvatarUpload(file) {
      const ltCode = 2;
      const sizeInMB = file.size / 1024 / 1024;
      const isExistFileType = uploadTypes.value.includes(file.type.replace(/image\//, ""));
      const isValidSize = sizeInMB > 0 && sizeInMB <= ltCode;

      if (!userId.value) {
        (proxy as any).$message.error("当前账号不支持头像上传");
        return false;
      }

      if (!isValidSize) {
        (proxy as any).$message.error(`图片大小范围是 0~${ltCode}MB!`);
      }
      if (!isExistFileType) {
        (proxy as any).$message.error(`图片只支持 ${uploadTypes.value.join("、")} 格式!`);
      }

      return isValidSize && isExistFileType;
    }

    function handleAvatarSuccess(response, file) {
      (proxy as any).$message({
        message: response.message,
        type: response.type,
      });

      if (response.success) proxy.$store.commit("setUserPic", response.data);
    }
    function handleAvatarError() {
      (proxy as any).$message.error("上传失败，请检查图片格式/大小或后端服务");
    }

    return {
      uploadTypes,
      uploadUrl,
      beforeAvatarUpload,
      handleAvatarSuccess,
      handleAvatarError,
    };
  },
});
</script>

<style scoped>
.upload {
  width: 100%;
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
