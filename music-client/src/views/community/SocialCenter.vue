<template>
  <div class="social-center">
    <div class="header">
      <h2>社交中心</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="refresh">
      <el-tab-pane label="我的笔记" name="myPosts" />
      <el-tab-pane label="我点赞的" name="likedPosts" />
      <el-tab-pane :label="`关注 (${followingUsers.length})`" name="followings" />
      <el-tab-pane :label="`粉丝 (${followerUsers.length})`" name="followers" />
    </el-tabs>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else>
      <el-empty v-if="activeTab !== 'followings' && activeTab !== 'followers' && !posts.length" description="暂无数据" />
      <el-empty v-else-if="activeTab === 'followings' && !followingUsers.length" description="暂无关注" />
      <el-empty v-else-if="activeTab === 'followers' && !followerUsers.length" description="暂无粉丝" />

      <div v-else-if="activeTab === 'followings' || activeTab === 'followers'" class="user-grid">
        <el-card
          v-for="u in activeTab === 'followings' ? followingUsers : followerUsers"
          :key="u.id"
          class="user-card"
          shadow="hover"
        >
          <div class="user-row">
            <el-image class="avatar" fit="cover" :src="attachImageUrl(u.avator)" />
            <div class="name">{{ u.username || `用户 ${u.id}` }}</div>
          </div>
        </el-card>
      </div>

      <div v-else class="post-grid">
        <el-card v-for="item in posts" :key="item.id" class="post-card" shadow="hover" @click="goDetail(item.id)">
          <div class="title">{{ item.title || "未命名" }}</div>
          <div class="meta">
            <span>{{ item.authorName || `用户 ${item.consumerId}` }}</span>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="content">{{ item.content }}</div>
          <div class="stats">
            <span>点赞 {{ item.likeCount || 0 }}</span>
            <span>评论 {{ item.commentCount || 0 }}</span>
          </div>
        </el-card>
      </div>
    </template>

    <div class="pager" v-if="activeTab === 'myPosts' || activeTab === 'likedPosts'">
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

const { proxy } = getCurrentInstance() as any;
const store = useStore();
const router = useRouter();
const { changeIndex } = mixin();

const consumerId = computed(() => Number(store.getters.userId || 0));
const attachImageUrl = HttpManager.attachImageUrl;

const activeTab = ref("myPosts");
const loading = ref(false);
const posts = ref<any[]>([]);
const followingUsers = ref<any[]>([]);
const followerUsers = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

function formatTime(t: any) {
  if (!t) return "";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function goDetail(id: number) {
  router.push({ path: `${RouterName.CommunityDetail}/${id}` });
}

async function refresh() {
  if (!consumerId.value) return;
  loading.value = true;
  try {
    // Keep follow/follower counters in tab titles fresh regardless of active tab.
    const [followingRes, followerRes] = (await Promise.all([
      HttpManager.getFollowingUsers(consumerId.value),
      HttpManager.getFollowerUsers(consumerId.value),
    ])) as any[];
    followingUsers.value = followingRes?.data?.items || [];
    followerUsers.value = followerRes?.data?.items || [];

    if (activeTab.value === "myPosts") {
      const res = (await HttpManager.getUserPostList({
        consumerId: consumerId.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })) as any;
      posts.value = res?.data?.items || [];
      total.value = res?.data?.total || 0;
    } else if (activeTab.value === "likedPosts") {
      const res = (await HttpManager.getLikedPostList({
        consumerId: consumerId.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })) as any;
      posts.value = res?.data?.items || [];
      total.value = res?.data?.total || 0;
    } else if (activeTab.value === "followings") {
      // already loaded above
      posts.value = [];
      total.value = 0;
    } else if (activeTab.value === "followers") {
      // already loaded above
      posts.value = [];
      total.value = 0;
    }
  } catch (e: any) {
    proxy?.$message?.({ message: e?.data?.message || "加载失败", type: "error" });
  } finally {
    loading.value = false;
  }
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

onMounted(() => {
  changeIndex(NavName.Social);
  refresh();
});

watch(activeTab, () => {
  pageNum.value = 1;
  refresh();
});
</script>

<style scoped lang="scss">
.social-center {
  width: 90%;
  margin: 0 auto;
  padding-top: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.post-card {
  cursor: pointer;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.meta {
  margin-top: 8px;
  color: #888;
  font-size: 12px;
  display: flex;
  justify-content: space-between;
}
.content {
  margin-top: 10px;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.stats {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  color: #666;
  font-size: 12px;
}
.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}
.name {
  font-weight: 600;
}
.pager {
  margin: 18px 0 30px;
  display: flex;
  justify-content: center;
}
</style>
