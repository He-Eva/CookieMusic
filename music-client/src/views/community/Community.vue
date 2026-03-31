<template>
  <div class="community">
    <div class="toolbar">
      <div class="left">
        <el-radio-group v-model="feed" size="small" @change="onFeedChange">
          <el-radio-button label="all">全站</el-radio-button>
          <el-radio-button label="following">关注</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="order" size="small">
          <el-radio-button label="latest">最新</el-radio-button>
          <el-radio-button label="hot">最热</el-radio-button>
        </el-radio-group>
        <el-input v-model="topic" placeholder="话题（可选）" clearable class="topic-input" @keyup.enter="refresh()" />
        <el-button @click="refresh()">查询</el-button>
      </div>
      <div class="right">
        <el-button type="primary" @click="goPublish">发布笔记</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-empty v-else-if="!items.length" description="暂无笔记" />

    <div v-else class="list">
      <el-card v-for="item in items" :key="item.id" class="card" shadow="hover" @click="goDetail(item.id)">
        <div class="title-row">
          <div class="title">{{ item.title || "未命名" }}</div>
          <div class="meta">
            <span class="topic" v-if="item.topic">#{{ item.topic }}</span>
            <span class="time">{{ formatTime(item.createTime) }}</span>
          </div>
        </div>
        <div class="author-row">
          <el-image class="avatar" fit="cover" :src="attachImageUrl(item.authorAvatar)" />
          <span class="author-name">{{ item.authorName || `用户 ${item.consumerId}` }}</span>
        </div>
        <div v-if="item.refSongId" class="rel-song" @click.stop="goSong(item.refSongId)">
          关联歌曲：{{ item.refSongName || `歌曲 ${item.refSongId}` }}
        </div>
        <div class="content">{{ item.content }}</div>
        <el-image v-if="item.coverUrl" class="cover" fit="cover" :src="attachImageUrl(item.coverUrl)" />
        <div class="stats">
          <span>点赞 {{ item.likeCount || 0 }}</span>
          <span>评论 {{ item.commentCount || 0 }}</span>
        </div>
      </el-card>
    </div>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { NavName, RouterName } from "@/enums";

const store = useStore();
const router = useRouter();
const { proxy } = getCurrentInstance() as any;
const { changeIndex, checkStatus } = mixin();

const token = computed(() => store.getters.token);
const consumerId = computed(() => Number(store.getters.userId || 0));

const order = ref<"latest" | "hot">("latest");
const feed = ref<"all" | "following">("all");
const topic = ref("");

const loading = ref(false);
const items = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

function formatTime(t: any) {
  if (!t) return "";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

async function refresh() {
  loading.value = true;
  try {
    if (feed.value === "following" && !token.value) {
      feed.value = "all";
    }
    const res = (await HttpManager.getPostList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      order: order.value,
      topic: topic.value,
      feed: feed.value,
      consumerId: feed.value === "following" ? consumerId.value : undefined,
    })) as any;
    if (res?.success) {
      items.value = res.data?.items || [];
      total.value = res.data?.total || 0;
    } else {
      items.value = [];
      total.value = 0;
    }
  } catch (e: any) {
    items.value = [];
    total.value = 0;
    proxy?.$message?.({
      message: e?.data?.message || e?.statusText || "请求失败，请检查后端是否已重启/数据库是否包含 follow 表",
      type: "error",
    });
  } finally {
    loading.value = false;
  }
}

function onFeedChange(val: string) {
  if (val === "following" && !checkStatus(true)) {
    feed.value = "all";
    return;
  }
  pageNum.value = 1;
  refresh();
}

function onPageChange(p: number) {
  pageNum.value = p;
  refresh();
}

function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
  refresh();
}

function goDetail(id: number) {
  router.push({ path: `${RouterName.CommunityDetail}/${id}` });
}

function goSong(id: number) {
  router.push({ path: `${RouterName.Lyric}/${id}` });
}

function goPublish() {
  if (!checkStatus(true)) return;
  router.push({ path: RouterName.CommunityPublish });
}

onMounted(() => {
  changeIndex(NavName.Community);
  refresh();
});

watch(order, () => {
  pageNum.value = 1;
  refresh();
});

const attachImageUrl = HttpManager.attachImageUrl;
</script>

<style scoped lang="scss">
.community {
  width: 90%;
  margin: 0 auto;
  padding-top: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.topic-input {
  width: 220px;
}

.list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.card {
  cursor: pointer;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.title {
  font-weight: 700;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  display: flex;
  gap: 10px;
  color: #8a8a8a;
  font-size: 12px;
  white-space: nowrap;
}

.author-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #555;
  font-size: 13px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.author-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  margin-top: 10px;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rel-song {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
}

.cover {
  width: 100%;
  height: 160px;
  border-radius: 6px;
  margin-top: 10px;
}

.stats {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  color: #666;
  font-size: 12px;
}

.pager {
  margin: 18px 0 30px;
  display: flex;
  justify-content: center;
}
</style>

