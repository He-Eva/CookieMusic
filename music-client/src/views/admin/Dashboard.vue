<template>
  <div class="dashboard">
    <div class="head">
      <h2>数据看板</h2>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="errorText" :title="errorText" type="error" show-icon class="mb" @close="errorText = ''" />

    <el-row :gutter="14" class="row">
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card card-users">
          <div class="card-label">注册用户</div>
          <div class="card-value">{{ stats.userTotal }}</div>
          <div class="card-sub">禁用：{{ stats.userDisabled }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card card-post">
          <div class="card-label">社区帖子</div>
          <div class="card-value">{{ stats.postTotal }}</div>
          <div class="card-sub warn">待审：{{ stats.postPending }}</div>
          <el-button v-if="stats.postPending > 0" size="small" type="warning" class="mt" @click="goAudit">去审核</el-button>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card">
          <div class="card-label">帖子状态</div>
          <div class="card-sub2">通过 {{ stats.postApproved }} · 驳回 {{ stats.postRejected }} · 下架 {{ stats.postOffline }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card card-comment">
          <div class="card-label">全站评论</div>
          <div class="card-value">{{ stats.commentTotal }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="row">
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card">
          <div class="card-label">歌曲</div>
          <div class="card-value">{{ stats.songTotal }}</div>
          <div class="card-sub2">上架 {{ stats.songOnline }} · 下架 {{ stats.songOffline }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card">
          <div class="card-label">歌单</div>
          <div class="card-value">{{ stats.songListTotal }}</div>
          <div class="card-sub2">上架 {{ stats.songListOnline }} · 下架 {{ stats.songListOffline }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card">
          <div class="card-label">歌单曲目关联</div>
          <div class="card-value">{{ stats.listSongTotal }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="card card-play">
          <div class="card-label">播放记录条数</div>
          <div class="card-value">{{ stats.playRecordTotal }}</div>
          <div class="card-sub">（用户播放行为采样）</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { HttpManager } from "@/api";

const router = useRouter();
const loading = ref(false);
const errorText = ref("");

const stats = reactive({
  userTotal: 0,
  userDisabled: 0,
  postTotal: 0,
  postPending: 0,
  postApproved: 0,
  postRejected: 0,
  postOffline: 0,
  songTotal: 0,
  songOnline: 0,
  songOffline: 0,
  songListTotal: 0,
  songListOnline: 0,
  songListOffline: 0,
  commentTotal: 0,
  listSongTotal: 0,
  playRecordTotal: 0,
});

function applyData(d: any) {
  if (!d) return;
  Object.keys(stats).forEach((k) => {
    if (d[k] !== undefined && d[k] !== null) {
      (stats as any)[k] = Number(d[k]);
    }
  });
}

async function load() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = (await HttpManager.adminDashboardStats()) as any;
    if (res?.success) {
      applyData(res.data);
    } else {
      errorText.value = res?.message || "加载失败";
    }
  } catch (e: any) {
    errorText.value = e?.response?.data?.message || e?.message || "网络错误";
  } finally {
    loading.value = false;
  }
}

function goAudit() {
  router.push("/admin/post-audit");
}

onMounted(() => {
  load();
});
</script>

<style scoped lang="scss">
.dashboard {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.head h2 {
  margin: 0;
  font-size: 20px;
}
.mb {
  margin-bottom: 12px;
}
.row {
  margin-bottom: 4px;
}
.card {
  margin-bottom: 14px;
  border-radius: 10px;
}
.card-label {
  font-size: 13px;
  color: #909399;
}
.card-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.card-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.card-sub.warn {
  color: #e6a23c;
  font-weight: 600;
}
.card-sub2 {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.mt {
  margin-top: 8px;
}
.card-users .card-value {
  color: #409eff;
}
.card-post .card-value {
  color: #e6a23c;
}
.card-comment .card-value {
  color: #67c23a;
}
.card-play .card-value {
  color: #909399;
}
</style>
