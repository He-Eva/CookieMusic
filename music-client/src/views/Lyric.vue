<!-- 【页面】歌词 /lyric/:id  parseLyric + Vuex curTime 高亮；可切歌 -->
<template>
  <div class="song-container">
    <el-image class="song-pic" fit="contain" :src="attachImageUrl(songPic)" />
    <ul class="song-info">
      <li>歌手：{{ singerName }}</li>
      <li>歌曲：{{ songTitle }}</li>
    </ul>
  </div>
  <div class="container">
    <div class="lyric-container">
      <div class="song-lyric">
        <transition-group name="lyric-fade">
          <!--有歌词-->
          <ul :style="{ top: lrcTop }" class="has-lyric" v-if="lyricArr.length" key="has-lyric">
            <li v-for="(item, index) in lyricArr" :key="index">
              {{ item[1] }}
            </li>
          </ul>
          <!--没歌词-->
          <div v-else class="no-lyric" key="no-lyric">
            <span>暂无歌词</span>
          </div>
        </transition-group>
      </div>
      <comment :playId="songId" :type="0"></comment>
    </div>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import Comment from "@/components/Comment.vue";
import { parseLyric } from "@/utils";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";

export default defineComponent({
  components: {
    Comment,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const { playSongById } = mixin();

    const lrcTop = ref("80px"); // 歌词滑动
    const lyricArr = ref([]); // 当前歌曲的歌词
    const songId = computed(() => store.getters.songId); // 歌曲ID
    const lyric = computed(() => store.getters.lyric); // 歌词
    const currentPlayList = computed(() => store.getters.currentPlayList); // 存放的音乐
    const currentPlayIndex = computed(() => store.getters.currentPlayIndex); // 当前歌曲在歌曲列表的位置
    const curTime = computed(() => store.getters.curTime);
    const changeTime = computed(() => store.getters.changeTime);
    const songTitle = computed(() => store.getters.songTitle); // 歌名
    const singerName = computed(() => store.getters.singerName); // 歌手名
    const songPic = computed(() => store.getters.songPic); // 歌曲图片

    /** 从接口拉最新歌词（改库后播放列表/Vuex 里仍是旧 lyric，必须刷新） */
    async function loadLyricFromServer(id: string | number) {
      const sid = Number(id);
      if (!Number.isFinite(sid)) return;
      try {
        const res = (await HttpManager.getSongOfId(sid)) as ResponseBody;
        if (!res?.success || !Array.isArray(res.data) || !res.data[0]) return;
        const text = res.data[0].lyric ?? "";
        lyricArr.value = parseLyric(text);
        store.commit("setLyric", text);
        const list = [...(currentPlayList.value as any[])];
        const idx = currentPlayIndex.value as number;
        if (idx >= 0 && list[idx]) {
          list[idx] = { ...list[idx], lyric: text };
          store.commit("setCurrentPlayList", list);
        }
        updateLyricByTime(Number(curTime.value) || 0);
      } catch (e) {
        console.error("load lyric failed", e);
      }
    }

    watch(songId, (id) => {
      if (id) loadLyricFromServer(id);
    });

    // 路由 /lyric/:id 与播放器不一致时（如从社区点关联歌曲）拉歌并播放
    watch(
      () => route.params.id,
      async (rawId) => {
        const rid = Number(rawId);
        if (!Number.isFinite(rid) || rid <= 0) return;
        const cur = Number(store.getters.songId);
        if (cur === rid) {
          await loadLyricFromServer(rid);
          return;
        }
        const ok = await playSongById(rid, false);
        if (ok) await loadLyricFromServer(rid);
      },
      { immediate: true }
    );

    function updateLyricByTime(t: number) {
      if (!lyricArr.value.length) return;
      const els = document.querySelectorAll(".has-lyric li") as NodeListOf<HTMLElement>;
      if (!els || els.length === 0) return;
      let idx = -1;
      for (let i = 0; i < lyricArr.value.length; i++) {
        if (t >= lyricArr.value[i][0]) idx = i;
        else break;
      }
      if (idx < 0) return;
      for (let j = 0; j < els.length; j++) {
        els[j].style.color = "#000";
        els[j].style.fontSize = "19px";
      }
      lrcTop.value = -idx * 46 + 50 + "px";
      if (els[idx]) {
        els[idx].style.color = "#95d2f6";
        els[idx].style.fontSize = "23px";
      }
    }

    // 正常播放滚动
    watch(curTime, () => updateLyricByTime(Number(curTime.value) || 0));
    // 拖动松手 seek 时，立即滚动到对应歌词（不等下一次 timeupdate）
    watch(changeTime, () => updateLyricByTime(Number(changeTime.value) || 0));

    return {
      songPic,
      singerName,
      songTitle,
      lrcTop,
      lyricArr,
      songId,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";

.song-container {
  position: fixed;
  top: 120px;
  left: 50px;
  display: flex;
  flex-direction: column;

  .song-pic {
    height: 300px;
    width: 300px;
    border: 4px solid white;
    border-radius: 12px;
  }

  .song-info {
    width: 300px;
    li {
      width: 100%;
      line-height: 40px;
      font-size: 21px;
      padding-left: 10%;
    }
  }
}

.lyric-container {
  font-family: $font-family;
  .song-lyric {
    position: relative;
    min-height: 300px;
    padding: 30px 0;
    overflow: auto;
    border-radius: 12px;
    background-color: $color-light-grey;
    .has-lyric {
      position: absolute;
      transition: all 1s;
      li {
        width: 100%;
        height: 46px;
        text-align: center;
        font-size: 19px;
        line-height: 46px;
      }
    }
    .no-lyric {
      position: absolute;
      margin: 100px 0;

      span {
        font-size: 23px;
        text-align: center;
      }
    }
  }
}

.lyric-fade-enter,
.lyric-fade-leave-to {
  transform: translateX(30px);
  opacity: 0;
}

.lyric-fade-enter-active,
.lyric-fade-leave-active {
  transition: all 0.3s ease;
}

@media screen and (min-width: $sm) {
  .container {
    padding-top: 30px;
  }
  .lyric-container {
    margin: 0 150px 0px 400px;
  }
}

@media screen and (max-width: $sm) {
  .container {
    padding: 20px;
  }
  .song-container {
    display: none;
  }
}
</style>
