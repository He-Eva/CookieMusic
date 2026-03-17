<template>
  <div class="publish">
    <el-card class="card">
      <template #header>
        <div class="header">
          <span>发布笔记</span>
          <el-button @click="goBack()">返回</el-button>
        </div>
      </template>

      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="可不填" />
        </el-form-item>
        <el-form-item label="话题">
          <el-input v-model="form.topic" maxlength="50" show-word-limit placeholder="例如：日常 / 推荐 / 听歌笔记" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="写点什么吧…" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, ref } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { NavName } from "@/enums";

const store = useStore();
const router = useRouter();
const { proxy } = getCurrentInstance() as any;
const { changeIndex } = mixin();

const consumerId = computed(() => store.getters.userId);

const form = ref({
  title: "",
  topic: "",
  content: "",
});
const submitting = ref(false);

function goBack() {
  router.push({ path: "/community" });
}

async function submit() {
  if (!form.value.content.trim()) {
    proxy.$message({ message: "内容不能为空", type: "warning" });
    return;
  }
  submitting.value = true;
  try {
    const res = (await HttpManager.addPost({
      consumerId: Number(consumerId.value),
      title: form.value.title,
      content: form.value.content,
      topic: form.value.topic,
    })) as any;
    proxy.$message({ message: res.message, type: res.type });
    if (res.success) {
      router.push({ path: `/community/detail/${res.data}` });
    }
  } finally {
    submitting.value = false;
  }
}

changeIndex(NavName.Community);
</script>

<style scoped lang="scss">
.publish {
  width: 90%;
  margin: 0 auto;
  padding-top: 20px;
}
.card {
  max-width: 780px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

