<template>
  <audio
      controls="controls"
      preload="true"
      v-if="url"
      ref="audioRef"
      :src="attachImageUrl(url)"
      @canplay="startPlay"
      @ended="ended"
  ></audio>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch } from "vue";
import { useStore } from "vuex";
import { HttpManager } from "@/api";

export default defineComponent({
  setup() {
    const store = useStore();
    const audioRef = ref<HTMLAudioElement | null>(null);

    const url = computed(() => store.getters.url);
    const isPlay = computed({
      get: () => store.getters.isPlay,
      set: (value) => store.commit('setIsPlay', value)
    });

    // 监听播放状态的变化，并操作音频播放或暂停
    watch(isPlay, (newValue, oldValue) => {
      if (audioRef.value) {
        if (newValue) {
          audioRef.value.play();
        } else {
          audioRef.value.pause();
        }
      }
    });

    // 当音频可以播放时触发
    function startPlay() {
      if (audioRef.value) {
        audioRef.value.play();
        isPlay.value = true; // 更新播放状态
      }
    }

    // 当音频播放结束时触发
    function ended() {
      isPlay.value = false; // 更新播放状态
    }

    return {
      url,
      audioRef,
      startPlay,
      ended,
      attachImageUrl: HttpManager.attachImageUrl,
      isPlay // 提供给模板用于绑定或显示播放状态
    };
  },
});
</script>

<style>
audio {
  display: none;
}
</style>