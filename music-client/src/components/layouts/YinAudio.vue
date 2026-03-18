<template>
  <audio
    :src="attachImageUrl(songUrl)"
    controls="true"
    :ref="player"
    preload="metadata"
    @loadedmetadata="loadedmetadata"
    @durationchange="durationchange"
    @error="onError"
    @canplay="canplay"
    @timeupdate="timeupdate"
    @ended="ended"
  >
    <!--（1）属性：controls，preload（2）事件：canplay，timeupdate，ended（3）方法：play()，pause() -->
    <!--controls：向用户显示音频控件（播放/暂停/进度条/音量）-->
    <!--preload：属性规定是否在页面加载后载入音频-->
    <!--canplay：当音频/视频处于加载过程中时，会发生的事件-->
    <!--timeupdate：当目前的播放位置已更改时-->
    <!--ended：当目前的播放列表已结束时-->
  </audio>
</template>

<script lang="ts">
import { defineComponent, ref, getCurrentInstance, computed, watch } from "vue";
import { useStore } from "vuex";
import { HttpManager } from "@/api";

export default defineComponent({
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const divRef = ref<HTMLAudioElement>();
    const shownAutoplayWarn = ref(false);
    const player = (el) => {
      divRef.value = el;
    };

    const songUrl = computed(() => store.getters.songUrl); // 音乐链接
    const songId = computed(() => store.getters.songId); // 音乐ID
    const isPlay = computed(() => store.getters.isPlay); // 播放状态
    const volume = computed(() => store.getters.volume); // 音量
    const changeTime = computed(() => store.getters.changeTime); // 指定播放时刻
    const autoNext = computed(() => store.getters.autoNext); // 用于触发自动播放下一首
    const token = computed(() => store.getters.token);
    const userId = computed(() => store.getters.userId);
    const reportedSongId = ref<number | null>(null);
    // 监听播放还是暂停
    watch(isPlay, () => togglePlay());
    // 跳到指定时刻播放
    watch(changeTime, () => {
        if (!divRef.value) return;
        const t = Number(changeTime.value);
        if (!Number.isFinite(t) || t < 0) return;
        const d = Number(divRef.value.duration);
        // 音频未 ready（duration 还没出来）时不 seek，避免回到开头
        if (!Number.isFinite(d) || d <= 0) return;
        divRef.value.currentTime = Math.max(0, Math.min(d - 0.1, t));
      }
    );
    watch(volume, (value) => (divRef.value.volume = value));
    watch(songUrl, () => {
      // 切歌时强制重新加载元数据，避免某些音频 duration 一直为 0 导致无法拖动
      try {
        if (!divRef.value) return;
        proxy.$store.commit("setDuration", 0);
        proxy.$store.commit("setCurTime", 0);
        // 重新加载音频资源/元数据
        divRef.value.load();
      } catch {
        // ignore
      }
    });
    watch(songId, (v) => {
      // 新歌切换后允许再次上报“开始播放”
      const sid = Number(v);
      reportedSongId.value = Number.isFinite(sid) ? null : null;
    });

    function syncDuration() {
      if (!divRef.value) return;
      const d = Number(divRef.value.duration);
      if (Number.isFinite(d) && d > 0) {
        proxy.$store.commit("setDuration", d);
      }
    }

    function loadedmetadata() {
      syncDuration();
    }

    function durationchange() {
      syncDuration();
    }

    function onError() {
      // 某些音频资源无法解析元数据/不支持拖动，给出提示但不中断播放逻辑
      try {
        (proxy as any)?.$message?.({ message: "该音频可能不支持拖动（元数据获取失败）", type: "warning" });
      } catch {
        // ignore
      }
    }

    function reportPlayStartOnce() {
      try {
        if (!token.value || !userId.value || !songId.value) return;
        const sid = Number(songId.value);
        const uid = Number(userId.value);
        if (!Number.isFinite(sid) || !Number.isFinite(uid)) return;
        if (reportedSongId.value === sid) return;
        reportedSongId.value = sid;
        // 点击播放即记录：即使未完整播放也会有历史记录
        HttpManager.addPlayRecord({
          consumerId: uid,
          songId: sid,
          playSeconds: 1,
          source: 0,
        });
      } catch (e) {
        // ignore
      }
    }

    // 开始 / 暂停
    function togglePlay() {
      if (!divRef.value) return;
      if (isPlay.value) {
        const p = divRef.value.play();
        // Some browsers block autoplay until user interacts
        if (p && typeof (p as any).catch === "function") {
          (p as any).then(() => {
            reportPlayStartOnce();
          }).catch(() => {
            proxy.$store.commit("setIsPlay", false);
            if (!shownAutoplayWarn.value) {
              shownAutoplayWarn.value = true;
              (proxy as any)?.$message?.({
                message: "浏览器限制自动播放，请点击一次播放按钮",
                type: "warning",
              });
            }
          });
        }
      } else {
        divRef.value.pause();
      }
    }
    // 获取歌曲链接后准备播放
    function canplay() {
      //  记录音乐时长
      syncDuration();
      //  开始播放
      const p = divRef.value.play();
      if (p && typeof (p as any).catch === "function") {
        (p as any).then(() => {
          proxy.$store.commit("setIsPlay", true);
          reportPlayStartOnce();
        }).catch(() => {
          proxy.$store.commit("setIsPlay", false);
          if (!shownAutoplayWarn.value) {
            shownAutoplayWarn.value = true;
            (proxy as any)?.$message?.({
              message: "浏览器限制自动播放，请点击一次播放按钮",
              type: "warning",
            });
          }
        });
      } else {
        proxy.$store.commit("setIsPlay", true);
        reportPlayStartOnce();
      }
    }
    // 音乐播放时记录音乐的播放位置
    function timeupdate() {
      proxy.$store.commit("setCurTime", divRef.value.currentTime);
      // console.log("audio-curtime" + divRef.value.currentTime)
      
    }
    // 音乐播放结束时触发
    function ended() {
      proxy.$store.commit("setIsPlay", false);
      proxy.$store.commit("setCurTime", 0);
      proxy.$store.commit("setAutoNext", !autoNext.value);

      // 上报播放记录（避免频繁请求：仅在播放结束时写一次）
      try {
        if (token.value && userId.value && songId.value) {
          const playSeconds = Math.floor(divRef.value.currentTime || 0);
          if (playSeconds > 0) {
            HttpManager.addPlayRecord({
              consumerId: Number(userId.value),
              songId: Number(songId.value),
              playSeconds,
              source: 0,
            });
          }
        }
      } catch (e) {
        // ignore client-side record failures
      }
    }

    return {
      songUrl,
      player,
      loadedmetadata,
      durationchange,
      onError,
      canplay,
      timeupdate,
      ended,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style scoped>
audio {
  /* display: none; */
}
</style>
