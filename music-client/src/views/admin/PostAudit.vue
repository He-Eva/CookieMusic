<template>
  <div class="post-audit">
    <div class="hero">
      <div class="hero-title">
        <h2>审核工作台</h2>
        <p>集中处理社区帖子审核，确保内容质量</p>
      </div>
      <div class="hero-tools">
        <el-select v-model="statusFilter" placeholder="状态筛选" class="status-select" @change="onFilterChange">
          <el-option label="全部状态" :value="''" />
          <el-option label="待审" :value="0" />
          <el-option label="通过" :value="1" />
          <el-option label="驳回" :value="2" />
          <el-option label="下架" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新列表</el-button>
      </div>
    </div>

    <div class="stats">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-title">当前页待审</div>
        <div class="stat-value">{{ pendingCount }}</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-title">当前页通过</div>
        <div class="stat-value success">{{ passedCount }}</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-title">当前页驳回</div>
        <div class="stat-value warning">{{ rejectedCount }}</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-title">当前页下架</div>
        <div class="stat-value danger">{{ offlineCount }}</div>
      </el-card>
    </div>

    <el-table :data="items" border stripe v-loading="loading" class="audit-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="authorName" label="作者" width="140" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="topic" label="话题" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center">
        <template #default="scope">
          <el-button size="small" @click="openDetail(scope.row.id)">详情</el-button>
          <el-button
            size="small"
            type="success"
            :disabled="Number(scope.row.status) === 1"
            @click="audit(scope.row.id, 1)"
          >
            通过
          </el-button>
          <el-button
            size="small"
            type="warning"
            :disabled="Number(scope.row.status) === 2"
            @click="audit(scope.row.id, 2)"
          >
            驳回
          </el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="Number(scope.row.status) === 3"
            @click="offline(scope.row.id)"
          >
            下架
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-drawer v-model="detailVisible" title="帖子审核详情" size="520px">
      <div v-if="detailLoading" class="loading-wrap">
        <el-skeleton :rows="8" animated />
      </div>
      <div v-else-if="detailItem" class="detail-wrap">
        <div class="detail-section">
          <div class="detail-label">标题</div>
          <div class="detail-value title">{{ detailItem.title || "-" }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">作者</div>
          <div class="detail-value">{{ detailItem.authorName || "-" }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">状态</div>
          <div class="detail-value">
            <el-tag :type="statusTag(detailItem.status)">{{ statusText(detailItem.status) }}</el-tag>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">发布时间</div>
          <div class="detail-value">{{ formatTime(detailItem.createTime) }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">话题</div>
          <div class="detail-value">{{ detailItem.topic || "-" }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">关联歌曲</div>
          <div class="detail-value">{{ detailItem.refSongName || (detailItem.refSongId ? `歌曲 ${detailItem.refSongId}` : "-") }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">正文</div>
          <div class="detail-content">{{ detailItem.content || "（无正文）" }}</div>
        </div>
        <div class="detail-section" v-if="detailItem.coverUrl">
          <div class="detail-label">封面</div>
          <el-image class="detail-image" fit="cover" :src="attachImageUrl(detailItem.coverUrl)" />
        </div>
        <div class="detail-section" v-if="detailImages.length">
          <div class="detail-label">配图</div>
          <div class="detail-images">
            <el-image
              v-for="(img, idx) in detailImages"
              :key="idx"
              class="detail-thumb"
              fit="cover"
              :src="attachImageUrl(img)"
            />
          </div>
        </div>
      </div>
      <div v-else class="detail-empty">暂无详情数据</div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, ref } from "vue";
import { HttpManager } from "@/api";

const { proxy } = getCurrentInstance() as any;
const loading = ref(false);
const items = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const statusFilter = ref<any>("");
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailItem = ref<any>(null);
const pendingCount = computed(() => items.value.filter((x) => Number(x.status) === 0).length);
const passedCount = computed(() => items.value.filter((x) => Number(x.status) === 1).length);
const rejectedCount = computed(() => items.value.filter((x) => Number(x.status) === 2).length);
const offlineCount = computed(() => items.value.filter((x) => Number(x.status) === 3).length);
const detailImages = computed(() => {
  const raw = detailItem.value?.images;
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter(Boolean);
  return String(raw)
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean);
});
function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败，请稍后重试";
}

function statusText(v: any) {
  const s = Number(v);
  if (s === 0) return "待审";
  if (s === 1) return "通过";
  if (s === 2) return "驳回";
  if (s === 3) return "下架";
  return "未知";
}
function statusTag(v: any) {
  const s = Number(v);
  if (s === 0) return "info";
  if (s === 1) return "success";
  if (s === 2) return "warning";
  if (s === 3) return "danger";
  return "";
}
function formatTime(t: any) {
  if (!t) return "";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

async function loadData() {
  loading.value = true;
  try {
    const res = (await HttpManager.adminPostList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: statusFilter.value === "" ? undefined : statusFilter.value,
    })) as any;
    items.value = res?.data?.items || [];
    total.value = res?.data?.total || 0;
  } catch (err: any) {
    items.value = [];
    total.value = 0;
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    loading.value = false;
  }
}

async function audit(postId: number, status: 1 | 2) {
  try {
    const res = (await HttpManager.adminAuditPost({ postId, status })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) loadData();
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

async function offline(postId: number) {
  try {
    const res = (await HttpManager.adminOfflinePost({ postId })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) loadData();
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

async function openDetail(postId: number) {
  detailVisible.value = true;
  detailLoading.value = true;
  detailItem.value = null;
  try {
    const res = (await HttpManager.adminPostDetail(postId)) as any;
    if (res?.success) {
      detailItem.value = res?.data || null;
    } else {
      proxy?.$message?.({ message: res?.message || "获取详情失败", type: "error" });
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    detailLoading.value = false;
  }
}

function onFilterChange() {
  pageNum.value = 1;
  loadData();
}
function onPageChange(p: number) {
  pageNum.value = p;
  loadData();
}
function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
  loadData();
}

onMounted(() => {
  loadData();
});

const attachImageUrl = HttpManager.attachImageUrl;
</script>

<style scoped lang="scss">
.post-audit {
  padding: 20px 24px;
  background: #f7f9fc;
  min-height: calc(100vh - 180px);
}
.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 14px;
  gap: 12px;
}
.hero-title h2 {
  margin: 0;
  font-size: 24px;
}
.hero-title p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}
.hero-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}
.status-select {
  width: 140px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
.stat-card {
  border-radius: 10px;
}
.stat-title {
  color: #909399;
  font-size: 12px;
}
.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.stat-value.success {
  color: #67c23a;
}
.stat-value.warning {
  color: #e6a23c;
}
.stat-value.danger {
  color: #f56c6c;
}
.audit-table {
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
.loading-wrap {
  padding: 8px 0;
}
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-section {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
}
.detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.detail-value {
  color: #303133;
}
.detail-value.title {
  font-size: 15px;
  font-weight: 600;
}
.detail-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
}
.detail-image {
  width: 100%;
  height: 180px;
  border-radius: 8px;
}
.detail-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.detail-thumb {
  width: 100%;
  height: 100px;
  border-radius: 6px;
}
.detail-empty {
  color: #909399;
  text-align: center;
  padding: 40px 0;
}
</style>

