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
        <el-form-item label="关联歌曲">
          <el-select
            v-model="form.refSongId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="可选：搜索并关联一首歌曲"
            :remote-method="searchSongs"
            :loading="songLoading"
            @change="onSongChange"
            style="width: 100%;"
          >
            <el-option v-for="s in songOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="写点什么吧…" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="post-upload"
            :action="uploadAction"
            list-type="picture-card"
            name="file"
            :limit="9"
            :show-file-list="false"
            :on-success="onUploadSuccess"
            :on-change="onUploadChange"
            :on-progress="onUploadProgress"
            :on-error="onUploadError"
            :on-exceed="onUploadExceed"
            :before-upload="beforeUpload"
            :with-credentials="true"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div v-if="imageItems.length" class="image-list">
            <div
              v-for="(item, idx) in imageItems"
              :key="item.uid"
              class="image-item"
              :class="{ dragging: dragIndex === idx }"
              draggable="true"
              @dragstart="onDragStart(idx)"
              @dragover.prevent
              @drop="onDrop(idx)"
              @dragend="onDragEnd"
            >
              <el-image class="image-preview" fit="cover" :src="displayImageUrl(item.url)"></el-image>
              <el-button class="delete-btn" type="danger" circle size="small" @click="removeImage(idx)">
                <el-icon><Delete /></el-icon>
              </el-button>
              <div class="image-meta">
                <span class="image-name">{{ item.name }}</span>
                <span v-if="idx === 0" class="cover-tag">封面</span>
              </div>
              <el-progress
                v-if="item.status === 'uploading'"
                :percentage="Math.round(item.percent)"
                :stroke-width="6"
                status="success"
              />
              <div v-else-if="item.status === 'error'" class="error-text">上传失败</div>
            </div>
          </div>
          <div class="upload-tip">支持 jpg/jpeg/png/gif/webp，单张不超过 5MB，最多 9 张</div>
          <div class="upload-tip">可拖拽调整顺序，第一张作为封面</div>
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
import { Plus, Delete } from "@element-plus/icons-vue";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { NavName } from "@/enums";
import type { UploadFile, UploadFiles, UploadProgressEvent } from "element-plus";

const store = useStore();
const router = useRouter();
const { proxy } = getCurrentInstance() as any;
const { changeIndex } = mixin();

const consumerId = computed(() => store.getters.userId);

const form = ref({
  title: "",
  topic: "",
  content: "",
  refSongId: undefined as number | undefined,
  refSongName: "",
});
type PostImageItem = {
  uid: string | number;
  name: string;
  url: string;
  percent: number;
  status: "uploading" | "done" | "error";
};

const imageItems = ref<PostImageItem[]>([]);
const dragIndex = ref<number | null>(null);
const uploadAction = HttpManager.postImageUploadUrl();
const submitting = ref(false);
const attachImageUrl = HttpManager.attachImageUrl;
const songOptions = ref<any[]>([]);
const songLoading = ref(false);

function displayImageUrl(url: string) {
  if (!url) return "";
  if (url.startsWith("blob:") || url.startsWith("http")) return url;
  return attachImageUrl(url);
}

function goBack() {
  router.push({ path: "/community" });
}

async function searchSongs(query: string) {
  songLoading.value = true;
  try {
    const q = (query || "").trim();
    const res = (await (q ? HttpManager.getSongOfSingerName(q) : HttpManager.getAllSong())) as any;
    const list = Array.isArray(res?.data) ? res.data : [];
    songOptions.value = list.slice(0, 30).map((x: any) => ({ id: Number(x.id), name: x.name }));
  } finally {
    songLoading.value = false;
  }
}

function onSongChange(v: number | undefined) {
  if (!v) {
    form.value.refSongName = "";
    return;
  }
  const hit = songOptions.value.find((x: any) => Number(x.id) === Number(v));
  form.value.refSongName = hit?.name || "";
}

function beforeUpload(file: File) {
  if (imageItems.value.length >= 9) {
    proxy.$message({ message: "最多上传 9 张图片", type: "warning" });
    return false;
  }
  const okType = ["image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"].includes(file.type);
  const okSize = file.size / 1024 / 1024 < 5;
  if (!okType) {
    proxy.$message({ message: "仅支持 jpg/jpeg/png/gif/webp", type: "warning" });
    return false;
  }
  if (!okSize) {
    proxy.$message({ message: "图片不能超过 5MB", type: "warning" });
    return false;
  }
  return true;
}

function onUploadChange(uploadFile: UploadFile, _uploadFiles: UploadFiles) {
  const exists = imageItems.value.find((x) => String(x.uid) === String(uploadFile.uid));
  if (exists) return;
  const preview = uploadFile.url || (uploadFile.raw ? URL.createObjectURL(uploadFile.raw) : "");
  imageItems.value.push({
    uid: uploadFile.uid,
    name: uploadFile.name || "图片",
    url: preview,
    percent: 0,
    status: "uploading",
  });
}

function onUploadProgress(evt: UploadProgressEvent, uploadFile: UploadFile) {
  const item = imageItems.value.find((x) => String(x.uid) === String(uploadFile.uid));
  if (!item) return;
  item.percent = evt.percent || 0;
  item.status = "uploading";
}

function onUploadSuccess(res: any, uploadFile: UploadFile) {
  const item = imageItems.value.find((x) => String(x.uid) === String(uploadFile.uid));
  if (!item) return;
  if (res?.success && res?.data) {
    item.url = res.data;
    item.percent = 100;
    item.status = "done";
  } else {
    item.status = "error";
    proxy.$message({ message: res?.message || "上传失败", type: "error" });
  }
}

function onUploadError(_err: any, uploadFile: UploadFile) {
  const item = imageItems.value.find((x) => String(x.uid) === String(uploadFile.uid));
  if (!item) return;
  item.status = "error";
}

function onUploadExceed() {
  proxy.$message({ message: "最多上传 9 张图片", type: "warning" });
}

function removeImage(idx: number) {
  imageItems.value.splice(idx, 1);
}

function onDragStart(idx: number) {
  dragIndex.value = idx;
}

function onDrop(idx: number) {
  if (dragIndex.value === null || dragIndex.value === idx) return;
  const moved = imageItems.value.splice(dragIndex.value, 1)[0];
  imageItems.value.splice(idx, 0, moved);
  dragIndex.value = null;
}

function onDragEnd() {
  dragIndex.value = null;
}

async function submit() {
  if (!form.value.content.trim()) {
    proxy.$message({ message: "内容不能为空", type: "warning" });
    return;
  }
  if (imageItems.value.some((x) => x.status === "uploading")) {
    proxy.$message({ message: "图片仍在上传中，请稍候", type: "warning" });
    return;
  }
  const imageUrls = imageItems.value.filter((x) => x.status === "done").map((x) => x.url);
  submitting.value = true;
  try {
    const res = (await HttpManager.addPost({
      consumerId: Number(consumerId.value),
      title: form.value.title,
      content: form.value.content,
      topic: form.value.topic,
      coverUrl: imageUrls[0] || "",
      images: imageUrls.length ? JSON.stringify(imageUrls) : "",
      refSongId: form.value.refSongId,
      refSongName: form.value.refSongName,
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
.upload-tip {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.image-list {
  width: 100%;
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.image-item {
  position: relative;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.image-item.dragging {
  opacity: 0.6;
}

.image-item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.image-preview {
  width: 100%;
  height: 120px;
  border-radius: 6px;
}

.delete-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0.9;
}

.image-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.image-name {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cover-tag {
  display: inline-block;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  padding: 2px 6px;
  border-radius: 999px;
  white-space: nowrap;
  line-height: 1.2;
}

.error-text {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}
</style>

