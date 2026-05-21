<!-- 【页面】管理-歌手 CRUD + 头像 /admin/singer -->
<template>
  <div class="admin-page singer-manage">
    <div class="header">
      <h2>歌手管理</h2>
      <div class="tools">
        <el-input
          v-model="keyword"
          placeholder="按歌手名筛选"
          clearable
          class="tool-input"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button type="success" @click="openCreate">添加歌手</el-button>
      </div>
    </div>

    <el-table :data="pageItems" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column label="头像" width="120" align="center">
        <template #default="scope">
          <el-image class="avatar" fit="cover" :src="HttpManager.attachImageUrl(scope.row.pic)" />
          <el-upload
            class="avatar-upload"
            :action="HttpManager.singerAvatarUploadUrl(scope.row.id)"
            name="file"
            :show-file-list="false"
            :with-credentials="true"
            :before-upload="beforeImgUpload"
            :on-success="onAvatarSuccess"
          >
            <el-button size="small" link type="primary">更新头像</el-button>
          </el-upload>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="歌手名" min-width="120" show-overflow-tooltip />
      <el-table-column label="性别" width="80">
        <template #default="scope">{{ sexText(scope.row.sex) }}</template>
      </el-table-column>
      <el-table-column label="出生" width="120">
        <template #default="scope">{{ formatBirth(scope.row.birth) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地区" width="100" show-overflow-tooltip />
      <el-table-column prop="introduction" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-popconfirm title="确认删除该歌手？关联歌曲可能受影响" @confirm="removeSinger(scope.row.id)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[10, 15, 30, 50]"
        :total="filteredList.length"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="createVisible" title="添加歌手" width="520px" @closed="resetCreate">
      <el-form :model="createForm" label-width="88px">
        <el-form-item label="歌手名" required>
          <el-input v-model="createForm.name" placeholder="请输入歌手名" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="createForm.sex">
            <el-radio :label="0">女</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">组合</el-radio>
            <el-radio :label="3">不明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故乡">
          <el-input v-model="createForm.location" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="createForm.birth" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="createForm.introduction" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑歌手" width="520px">
      <el-form :model="editForm" label-width="88px">
        <el-form-item label="歌手名" required>
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="editForm.sex">
            <el-radio :label="0">女</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">组合</el-radio>
            <el-radio :label="3">不明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故乡">
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="editForm.birth" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.introduction" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import { HttpManager } from "@/api";
import { getBirth } from "@/utils";

const { proxy } = getCurrentInstance() as any;

const loading = ref(false);
const allList = ref<any[]>([]);
const keyword = ref("");
const pageNum = ref(1);
const pageSize = ref(15);

const createVisible = ref(false);
const createSaving = ref(false);
const createForm = reactive({
  name: "",
  sex: 1 as number,
  birth: new Date() as Date,
  location: "",
  introduction: "",
});

const editVisible = ref(false);
const editSaving = ref(false);
const editForm = reactive({
  id: 0,
  name: "",
  sex: 1 as number,
  birth: new Date() as Date,
  location: "",
  introduction: "",
});

const uploadTypes = ["jpg", "jpeg", "png", "gif"];

function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败";
}

function sexText(v: any) {
  const s = Number(v);
  if (s === 0) return "女";
  if (s === 1) return "男";
  if (s === 2) return "组合";
  if (s === 3) return "不明";
  return "-";
}

function formatBirth(v: any) {
  if (!v) return "-";
  return getBirth(v) || String(v).slice(0, 10);
}

const filteredList = computed(() => {
  const kw = keyword.value.trim();
  let list = [...allList.value];
  if (kw) {
    list = list.filter((s) => String(s.name || "").includes(kw));
  }
  return list.sort((a, b) => Number(b.id) - Number(a.id));
});

const pageItems = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value;
  return filteredList.value.slice(start, start + pageSize.value);
});

function beforeImgUpload(file: File) {
  const isLt2M = file.size / 1024 / 1024 < 2;
  const ext = file.type.replace(/image\//, "");
  const okType = uploadTypes.includes(ext);
  if (!okType) {
    proxy?.$message?.({ message: `仅支持 ${uploadTypes.join("、")}`, type: "warning" });
  }
  if (!isLt2M) {
    proxy?.$message?.({ message: "图片不能超过 2MB", type: "warning" });
  }
  return okType && isLt2M;
}

function onAvatarSuccess(res: any) {
  const body = res?.data ?? res;
  proxy?.$message?.({ message: body?.message || "上传完成", type: body?.type || "success" });
  if (body?.success !== false) loadSingers();
}

async function loadSingers() {
  loading.value = true;
  try {
    const res = (await HttpManager.getAllSinger()) as any;
    allList.value = Array.isArray(res?.data) ? res.data : [];
  } catch (err: any) {
    allList.value = [];
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  pageNum.value = 1;
}

function onPageChange(p: number) {
  pageNum.value = p;
}

function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
}

function openCreate() {
  createVisible.value = true;
}

function resetCreate() {
  createForm.name = "";
  createForm.sex = 1;
  createForm.birth = new Date();
  createForm.location = "";
  createForm.introduction = "";
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    proxy?.$message?.({ message: "请填写歌手名", type: "warning" });
    return;
  }
  createSaving.value = true;
  try {
    const res = (await HttpManager.addSinger({
      name: createForm.name.trim(),
      sex: createForm.sex,
      birth: getBirth(createForm.birth),
      location: createForm.location,
      introduction: createForm.introduction,
    })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) {
      createVisible.value = false;
      await loadSingers();
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    createSaving.value = false;
  }
}

function openEdit(row: any) {
  editForm.id = row.id;
  editForm.name = row.name || "";
  editForm.sex = Number(row.sex ?? 1);
  editForm.location = row.location || "";
  editForm.introduction = row.introduction || "";
  editForm.birth = row.birth ? new Date(row.birth) : new Date();
  editVisible.value = true;
}

async function submitEdit() {
  if (!editForm.name.trim()) {
    proxy?.$message?.({ message: "请填写歌手名", type: "warning" });
    return;
  }
  editSaving.value = true;
  try {
    const res = (await HttpManager.updateSinger({
      id: editForm.id,
      name: editForm.name.trim(),
      sex: editForm.sex,
      birth: getBirth(editForm.birth),
      location: editForm.location,
      introduction: editForm.introduction,
    })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) {
      editVisible.value = false;
      await loadSingers();
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    editSaving.value = false;
  }
}

async function removeSinger(id: number) {
  try {
    const res = (await HttpManager.deleteSinger(id)) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) await loadSingers();
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

onMounted(() => {
  loadSingers();
});
</script>

<style scoped lang="scss">
.singer-manage {
  padding: 0;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}
.tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tool-input {
  width: 200px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  display: block;
  margin: 0 auto 4px;
}
.avatar-upload {
  display: flex;
  justify-content: center;
}
</style>
