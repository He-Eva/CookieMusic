<template>
  <div class="personal">
    <div class="personal-info">
      <div class="personal-img" @click="handleAvatarClick">
        <el-image fit="cover" :src="attachImageUrl(userPic)"/>
      </div>
      <div class="personal-msg">
        <div class="username">{{ personalInfo.username }}</div>
        <div class="introduction">{{ personalInfo.introduction }}</div>
      </div>
      <el-button class="edit-info" round :icon="Edit" @click="goPage()">修改个人信息</el-button>
    </div>
    <div class="personal-body">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="收藏歌曲" name="collect">
          <el-empty v-if="!collectSongList.length" description="暂无收藏歌曲"></el-empty>
          <song-list v-else :songList="collectSongList" :show="true" @changeData="changeData"></song-list>
        </el-tab-pane>
        <el-tab-pane label="收藏歌单" name="collect_sheet">
          <el-empty v-if="!collectSongSheetList.length" description="暂无收藏歌单"></el-empty>
          <play-list v-else title="" path="song-sheet-detail" :playList="collectSongSheetList"></play-list>
        </el-tab-pane>
        <el-tab-pane label="历史播放记录" name="history">
          <el-empty v-if="!historyRows.length" description="暂无播放记录"></el-empty>
          <div v-else class="history">
            <el-table highlight-current-row :data="historyRows" @row-click="handleHistoryRowClick">
              <el-table-column prop="songName" label="歌曲" />
              <el-table-column prop="singerName" label="歌手" />
              <el-table-column prop="introduction" label="专辑" />
              <el-table-column prop="playTimeText" label="播放时间" width="180" />
              <el-table-column prop="playSecondsText" label="时长" width="100" />
            </el-table>
            <div class="history-pagination">
              <el-pagination
                background
                layout="prev, pager, next, total"
                :page-size="historyPageSize"
                :total="historyTotal"
                :current-page="historyPageNum"
                @current-change="handleHistoryPageChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <el-dialog v-model="dialogTableVisible" title="修改头像">
      <upload></upload>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { defineComponent, nextTick, ref, computed, watch, reactive, getCurrentInstance } from "vue";
import { useStore } from "vuex";
import { Edit } from "@element-plus/icons-vue";
import SongList from "@/components/SongList.vue";
import PlayList from "@/components/PlayList.vue";
import Upload from "../setting/Upload.vue";
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { RouterName } from "@/enums";

export default defineComponent({
  components: {
    SongList,
    PlayList,
    Upload,
  },
  setup() {
    const { proxy } = getCurrentInstance() as any;
    const store = useStore();

    const { routerManager } = mixin();

    const activeTab = ref("collect");
    const dialogTableVisible = ref(false);
    const collectSongList = ref([]); // 收藏的歌曲
    const collectSongSheetList = ref([]); // 收藏的歌单
    const personalInfo = reactive({
      username: "",
      userSex: "",
      birth: "",
      location: "",
      introduction: "",
    });
    const userId = computed(() => store.getters.userId);
    const userPic = computed(() => store.getters.userPic);
    const isAdmin = computed(() => {
      return Boolean(store.getters.isAdmin) || localStorage.getItem("cm_isAdmin") === "true";
    });
    watch(userPic, () => {
      dialogTableVisible.value = false;
    });

    function handleAvatarClick() {
      if (isAdmin.value) {
        proxy?.$message?.({ message: "管理员账号暂不支持头像上传", type: "warning" });
        return;
      }
      dialogTableVisible.value = true;
    }

    function goPage() {
      routerManager(RouterName.Setting, { path: RouterName.Setting });
    }
    async function getUserInfo(id) {
      try {
        const result = (await HttpManager.getUserOfId(id)) as ResponseBody;
        const row = result?.data?.[0];
        if (!row) return;
        personalInfo.username = row.username;
        personalInfo.userSex = row.sex;
        personalInfo.birth = row.birth;
        personalInfo.introduction = row.introduction;
        personalInfo.location = row.location;
      } catch (e: any) {
        proxy?.$message?.({ message: e?.data?.message || "获取用户信息失败", type: "error" });
      }
    }
    // 获取收藏的歌曲
    async function getCollection(userId) {
      try {
        collectSongList.value = [];
        collectSongSheetList.value = [];
        const result = (await HttpManager.getCollectionOfUser(userId)) as ResponseBody;
        const collectIDList = result?.data || [];

        // type: 0=歌曲, 1=歌单
        const songIds = Array.from(
          new Set<number>(
            collectIDList
              .filter((x: any) => Number(x?.type) === 0 && x?.songId)
              .map((x: any) => Number(x.songId))
              .filter((x: any) => Number.isFinite(x))
          )
        );
        const songListIds = Array.from(
          new Set<number>(
            collectIDList
              .filter((x: any) => Number(x?.type) === 1 && x?.songListId)
              .map((x: any) => Number(x.songListId))
              .filter((x: any) => Number.isFinite(x))
          )
        );

        const [songs, songSheets] = await Promise.all([
          Promise.all(
            songIds.map(async (sid) => {
              const songRes = (await HttpManager.getSongOfId(sid)) as ResponseBody;
              return songRes?.data?.[0];
            })
          ),
          Promise.all(
            songListIds.map(async (lid) => {
              const listRes = (await HttpManager.getSongListOfId(lid)) as ResponseBody;
              return listRes?.data?.[0];
            })
          ),
        ]);

        collectSongList.value = songs.filter(Boolean);
        collectSongSheetList.value = songSheets.filter(Boolean);
      } catch (e: any) {
        proxy?.$message?.({ message: e?.data?.message || "获取收藏失败", type: "error" });
      }
    }

    function changeData() {
      getCollection(userId.value);
    }

    // ============= 历史播放记录 =============
    const historyPageNum = ref(1);
    const historyPageSize = ref(10);
    const historyTotal = ref(0);
    const historyRows = ref<any[]>([]);
    const songCache = new Map<number, any>();

    function formatPlayTime(v: any) {
      if (!v) return "";
      try {
        const d = new Date(v);
        if (Number.isNaN(d.getTime())) return String(v);
        const pad = (n: number) => (n < 10 ? `0${n}` : String(n));
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
      } catch {
        return String(v);
      }
    }

    function formatSeconds(sec: any) {
      const s = Number(sec);
      if (!Number.isFinite(s) || s <= 0) return "-";
      const mm = Math.floor(s / 60);
      const ss = Math.floor(s % 60);
      return `${mm}:${ss < 10 ? "0" : ""}${ss}`;
    }

    async function getSongByIdCached(id: number) {
      if (songCache.has(id)) return songCache.get(id);
      const songRes = (await HttpManager.getSongOfId(id)) as ResponseBody;
      const song = songRes?.data?.[0];
      if (song) songCache.set(id, song);
      return song;
    }

    async function loadPlayHistory() {
      if (!userId.value) return;
      try {
        const res = (await HttpManager.getPlayRecordByUser({
          consumerId: Number(userId.value),
          pageNum: historyPageNum.value,
          pageSize: historyPageSize.value,
        })) as any;
        if (!res?.success) return;
        const data = res?.data || {};
        const items = Array.isArray(data.items) ? data.items : [];
        historyTotal.value = Number(data.total || 0);

        const songIds: number[] = Array.from(
          new Set<number>(items.map((x: any) => Number(x.songId)).filter((x: any) => Number.isFinite(x)))
        );
        await Promise.all(songIds.map((sid: number) => getSongByIdCached(sid)));

        historyRows.value = items.map((it: any) => {
          const sid = Number(it.songId);
          const song = songCache.get(sid);
          return {
            ...song,
            songId: sid,
            playTime: it.playTime,
            playSeconds: it.playSeconds,
            playTimeText: formatPlayTime(it.playTime),
            playSecondsText: formatSeconds(it.playSeconds),
          };
        }).filter((x: any) => x && x.id);
      } catch (e: any) {
        proxy?.$message?.({ message: e?.data?.message || "获取播放历史失败", type: "error" });
      }
    }

    function handleHistoryPageChange(p: number) {
      historyPageNum.value = p;
      loadPlayHistory();
    }

    function handleHistoryRowClick(row: any) {
      // 复用 SongList 的播放逻辑（通过 mixin.playMusic）
      // 这里为了不改动太多代码，直接调用全局播放：用 SongList 的行数据结构即可
      try {
        const { playMusic } = mixin();
        playMusic({
          id: row.id,
          url: row.url,
          pic: row.pic,
          index: 0,
          name: row.name,
          lyric: row.lyric,
          currentSongList: historyRows.value.map((x: any) => x),
        });
      } catch {
        // ignore
      }
    }

    nextTick(() => {
      if (userId.value) {
        getUserInfo(userId.value);
        getCollection(userId.value);
        loadPlayHistory();
      }
    });

    watch(userId, (v) => {
      if (!v) return;
      historyPageNum.value = 1;
      getUserInfo(v);
      getCollection(v);
      loadPlayHistory();
    });

    return {
      Edit,
      userPic,
      dialogTableVisible,
      activeTab,
      collectSongList,
      collectSongSheetList,
      personalInfo,
      attachImageUrl: HttpManager.attachImageUrl,
      goPage,
      handleAvatarClick,
      changeData,
      historyRows,
      historyPageNum,
      historyPageSize,
      historyTotal,
      handleHistoryPageChange,
      handleHistoryRowClick,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";

.personal {
  padding-top: $header-height + 150px;

  &::before {
    content: "";
    background-color: $color-blue-shallow;
    position: absolute;
    top: 0;
    width: 100%;
    height: $header-height + 150px;
  }
}

.personal-info {
  position: relative;
  margin-bottom: 60px;
  .personal-img {
    height: 200px;
    width: 200px;
    border-radius: 50%;
    border: 5px solid $color-white;
    position: absolute;
    top: -180px;
    left: 50px;
    cursor: pointer;
    overflow: hidden;
    .el-image {
      width: 100%;
      height: 100%;
    }
    :deep(img) {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  .personal-msg {
    margin-left: 300px;
    position: absolute;
    top: -120px;

    .username {
      font-size: 50px;
      font-weight: 600;
    }

    .introduction {
      font-size: 20px;
      font-weight: 500;
    }
  }
  .edit-info {
    position: absolute;
    right: 10vw;
    margin-top: -120px;
  }
}

@media screen and (min-width: $sm) {
  .personal-body {
    padding: 0px 100px;
  }
}

@media screen and (max-width: $sm) {
  .edit-info {
    display: none;
  }
}

.history-pagination {
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
}
</style>
