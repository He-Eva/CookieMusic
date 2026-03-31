<template>
  <div class="detail">
    <el-card class="card" v-loading="loading">
      <template #header>
        <div class="header">
          <div class="title">{{ post?.title || "未命名" }}</div>
          <div class="actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!post && !loading" description="帖子已提交，等待审核中" />

      <template v-else>
      <div class="meta">
        <span v-if="post?.topic">#{{ post.topic }}</span>
        <span class="time">{{ formatTime(post?.createTime) }}</span>
      </div>

      <div class="author-row" v-if="post">
        <div class="author">
          <el-image class="avatar" fit="cover" :src="attachImageUrl(post.authorAvatar)" />
          <span>作者：{{ post.authorName || `用户 ${post.consumerId}` }}</span>
        </div>
        <div class="author-actions" v-if="showFollowButton">
          <el-button :type="isFollowing ? 'success' : 'primary'" :loading="followSubmitting" @click="toggleFollow">
            {{ isFollowing ? "已关注" : "关注" }}
          </el-button>
        </div>
      </div>

      <div v-if="post?.refSongId" class="rel-song" @click="goSong(post.refSongId)">
        关联歌曲：{{ post.refSongName || `歌曲 ${post.refSongId}` }}
      </div>

      <div class="content">{{ post?.content }}</div>
      <div v-if="detailImages.length" class="post-images">
        <el-image v-for="(img, idx) in detailImages" :key="idx" class="post-image" fit="cover" :src="attachImageUrl(img)" />
      </div>

      <div class="stats">
        <el-button :type="liked ? 'primary' : 'default'" @click="toggleLike">
          {{ liked ? "已点赞" : "点赞" }}（{{ post?.likeCount || 0 }}）
        </el-button>
        <span class="comment-count">评论 {{ post?.commentCount || 0 }}</span>
      </div>
      </template>
    </el-card>

    <el-card class="card comment-card">
      <template #header>
        <div class="header">
          <div class="title">评论</div>
        </div>
      </template>

      <div class="comment-box">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="写下你的评论…" />
        <div class="comment-actions">
          <el-button type="primary" :disabled="!commentContent.trim()" :loading="commentSubmitting" @click="submitComment">
            发表评论
          </el-button>
        </div>
      </div>

      <el-empty v-if="!commentLoading && !comments.length" description="暂无评论" />

      <el-skeleton v-else-if="commentLoading" :rows="4" animated />

      <div v-else class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-meta">
            <div class="comment-user">
              <el-image class="avatar" fit="cover" :src="attachImageUrl(c.userAvatar)" />
              <span>{{ c.userName || `用户 ${c.consumerId}` }}</span>
            </div>
            <span class="time">{{ formatTime(c.createTime) }}</span>
          </div>
          <div class="comment-content">{{ c.content }}</div>
        </div>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :current-page="commentPageNum"
          :page-size="commentPageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="commentTotal"
          @current-change="onCommentPageChange"
          @size-change="onCommentSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { NavName } from "@/enums";

const route = useRoute();
const router = useRouter();
const store = useStore();
const { proxy } = getCurrentInstance() as any;
const { changeIndex, checkStatus } = mixin();
const attachImageUrl = HttpManager.attachImageUrl;

const consumerId = computed(() => Number(store.getters.userId || 0));

const postId = computed(() => Number(route.params.id));

const loading = ref(false);
const post = ref<any>(null);

const liked = ref(false);

const isFollowing = ref(false);
const followSubmitting = ref(false);

const commentContent = ref("");
const commentSubmitting = ref(false);

const commentLoading = ref(false);
const comments = ref<any[]>([]);
const commentTotal = ref(0);
const commentPageNum = ref(1);
const commentPageSize = ref(10);

const detailImages = computed(() => {
  const raw = post.value?.images;
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter(Boolean);
  try {
    const arr = JSON.parse(String(raw));
    if (Array.isArray(arr)) return arr.filter(Boolean);
  } catch (e) {
    // fall back for legacy comma-separated format
  }
  return String(raw)
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean);
});

function formatTime(t: any) {
  if (!t) return "";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function goBack() {
  router.push({ path: "/community" });
}

function goSong(id: number) {
  router.push({ path: `/lyric/${id}` });
}

async function fetchDetail() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) return;
  loading.value = true;
  try {
    const res = (await HttpManager.getPostDetail(postId.value)) as any;
    if (res?.success) {
      post.value = res.data;
      await refreshFollowStatus();
      await refreshLikeStatus();
    } else {
      post.value = null;
      // Newly published posts may still be pending review; avoid noisy toast here.
    }
  } finally {
    loading.value = false;
  }
}

async function refreshLikeStatus() {
  if (!post.value) return;
  if (!consumerId.value) {
    liked.value = false;
    return;
  }
  const res = (await HttpManager.postLikeStatus({
    postId: postId.value,
    consumerId: consumerId.value,
  })) as any;
  if (res?.success) {
    liked.value = !!res.data;
  }
}

const showFollowButton = computed(() => {
  if (!post.value) return false;
  if (!consumerId.value) return false;
  // don't show follow button on self
  return consumerId.value !== Number(post.value.consumerId);
});

async function refreshFollowStatus() {
  if (!showFollowButton.value) return;
  const res = (await HttpManager.followStatus({
    userId: consumerId.value,
    followUserId: Number(post.value.consumerId),
  })) as any;
  if (res?.success) {
    isFollowing.value = !!res.data;
  }
}

async function toggleFollow() {
  if (!checkStatus(true)) return;
  if (!post.value) return;
  if (consumerId.value === Number(post.value.consumerId)) return;

  followSubmitting.value = true;
  try {
    const api = isFollowing.value ? HttpManager.unfollowUser : HttpManager.followUser;
    const res = (await api({
      userId: consumerId.value,
      followUserId: Number(post.value.consumerId),
    })) as any;
    proxy.$message({ message: res.message, type: res.type });
    if (res?.success) {
      isFollowing.value = !!res.data;
    }
  } finally {
    followSubmitting.value = false;
  }
}

async function fetchComments() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) return;
  commentLoading.value = true;
  try {
    const res = (await HttpManager.getPostCommentList({
      postId: postId.value,
      pageNum: commentPageNum.value,
      pageSize: commentPageSize.value,
    })) as any;
    if (res?.success) {
      comments.value = res.data?.items || [];
      commentTotal.value = res.data?.total || 0;
    } else {
      comments.value = [];
      commentTotal.value = 0;
    }
  } finally {
    commentLoading.value = false;
  }
}

async function toggleLike() {
  if (!checkStatus(true)) return;
  const res = (await HttpManager.likePost({
    postId: postId.value,
    consumerId: consumerId.value,
    like: null,
  })) as any;
  proxy.$message({ message: res.message, type: res.type });
  if (res?.success) {
    liked.value = !!res.data;
    await fetchDetail();
  }
}

async function submitComment() {
  if (!checkStatus(true)) return;
  if (!commentContent.value.trim()) return;
  commentSubmitting.value = true;
  try {
    const res = (await HttpManager.addPostComment({
      postId: postId.value,
      consumerId: consumerId.value,
      content: commentContent.value,
    })) as any;
    proxy.$message({ message: res.message, type: res.type });
    if (res?.success) {
      commentContent.value = "";
      commentPageNum.value = 1;
      await fetchDetail();
      await fetchComments();
    }
  } finally {
    commentSubmitting.value = false;
  }
}

function onCommentPageChange(p: number) {
  commentPageNum.value = p;
  fetchComments();
}

function onCommentSizeChange(ps: number) {
  commentPageSize.value = ps;
  commentPageNum.value = 1;
  fetchComments();
}

onMounted(async () => {
  changeIndex(NavName.Community);
  await fetchDetail();
  await fetchComments();
});

watch(
  () => route.params.id,
  async () => {
    if (!Number.isFinite(postId.value) || postId.value <= 0) return;
    commentPageNum.value = 1;
    await fetchDetail();
    await fetchComments();
  }
);
</script>

<style scoped lang="scss">
.detail {
  width: 90%;
  margin: 0 auto;
  padding-top: 20px;
  display: grid;
  gap: 14px;
}

.card {
  max-width: 860px;
  margin: 0 auto;
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title {
  font-weight: 700;
  font-size: 18px;
}

.meta {
  color: #888;
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.author-row {
  margin: 10px 0 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: #666;
  font-size: 13px;
}

.author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #333;
}

.rel-song {
  margin: 6px 0 10px;
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
}

.post-images {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.post-image {
  width: 100%;
  height: 140px;
  border-radius: 6px;
}

.stats {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-box {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: grid;
  gap: 12px;
}

.comment-item {
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  color: #888;
  font-size: 12px;
  margin-bottom: 6px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-content {
  white-space: pre-wrap;
  color: #333;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>

