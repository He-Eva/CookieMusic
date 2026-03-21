import { getBaseURL, get, post, deletes } from "./request";

const HttpManager = {
  // 获取图片信息
  attachImageUrl: (url) => {
    const fallback = `${getBaseURL()}/user01/consumer/img/default.jpg`;
    return url ? `${getBaseURL()}/${url}` : fallback;
  }
  ,
  // =======================> 用户 API 完成
  // 登录
  signIn: ({username,password}) => post(`user/login/status`, {username,password}),
  // 管理员登录
  adminSignIn: ({ username, password }) => post(`admin/login/status`, { username, password }),
  // 管理员退出
  adminLogout: () => get(`admin/logout`),
  // 管理员改密
  adminUpdatePassword: ({ oldPassword, password }) => post(`admin/password/update`, { oldPassword, password }),
  // 管理员帖子审核列表
  adminPostList: ({ pageNum = 1, pageSize = 10, status }: { pageNum?: number; pageSize?: number; status?: number | string } = {}) => {
    const qs = [`pageNum=${pageNum}`, `pageSize=${pageSize}`];
    if (status !== undefined && status !== null && status !== "") qs.push(`status=${status}`);
    return get(`admin/post?${qs.join("&")}`);
  },
  // 管理员帖子详情
  adminPostDetail: (id) => get(`admin/post/detail?id=${id}`),
  // 管理员用户分页
  adminUserPage: ({
    pageNum = 1,
    pageSize = 10,
    keyword = "",
    status,
  }: {
    pageNum?: number;
    pageSize?: number;
    keyword?: string;
    status?: number | string;
  } = {}) => {
    const qs = [`pageNum=${pageNum}`, `pageSize=${pageSize}`];
    if (keyword) qs.push(`keyword=${encodeURIComponent(keyword)}`);
    if (status !== undefined && status !== null && status !== "") qs.push(`status=${status}`);
    return get(`admin/user?${qs.join("&")}`);
  },
  // 管理员禁用/解禁用户
  adminUpdateUserStatus: ({ userId, status }) => post(`admin/user/status?userId=${userId}&status=${status}`),
  // 管理员评论分页
  adminCommentPage: ({
    pageNum = 1,
    pageSize = 10,
    keyword = "",
  }: {
    pageNum?: number;
    pageSize?: number;
    keyword?: string;
  } = {}) => {
    const qs = [`pageNum=${pageNum}`, `pageSize=${pageSize}`];
    if (keyword) qs.push(`keyword=${encodeURIComponent(keyword)}`);
    return get(`admin/comment?${qs.join("&")}`);
  },
  // 管理员删除评论
  adminDeleteComment: (id) => post(`admin/comment/delete?id=${id}`),
  // 审核帖子（通过/驳回）
  adminAuditPost: ({ postId, status, reason }) => post(`admin/post/audit`, { postId, status, reason }),
  // 下架帖子
  adminOfflinePost: ({ postId }) => post(`admin/post/offline`, { postId }),
  // 注册
  SignUp: ({username,password,sex,phoneNum,email,birth,introduction,location}) => post(`user/add`, {username,password,sex,phoneNum,email,birth,introduction,location}),
  // 删除用户
  deleteUser: (id) => get(`user/delete?id=${id}`),
  // 更新用户信息
  updateUserMsg: ({id,username,sex,phoneNum,email,birth,introduction,location}) => post(`user/update`, {id,username,sex,phoneNum,email,birth,introduction,location}),
  updateUserPassword: ({id,username,oldPassword,password}) => post(`user/updatePassword`, {id,username,oldPassword,password}),
  // 返回指定ID的用户
  getUserOfId: (id) => get(`user/detail?id=${id}`),
  // 更新用户头像
  uploadUrl: (userId) => {
    const last_url = `${getBaseURL()}/user/avatar/update?id=${userId}`
    return last_url
  },

  // =======================> 歌单 API 完成
  // 获取全部歌单
  getSongList: () => get("songList"),
  // 返回指定ID的歌单
  getSongListOfId: (id) => get(`songList/detail?id=${id}`),
  // 获取歌单类型
  getSongListOfStyle: (style) => get(`songList/style/detail?style=${style}`),
  // 返回标题包含文字的歌单
  getSongListOfLikeTitle: (keywords) => get(`songList/likeTitle/detail?title=${keywords}`),
  // 返回歌单里指定歌单ID的歌曲
  getListSongOfSongId: (songListId) => get(`listSong/detail?songListId=${songListId}`),

  // =======================> 歌手 API  完成
  // 返回所有歌手
  getAllSinger: () => get("singer"),
  // 通过性别对歌手分类
  getSingerOfSex: (sex) => get(`singer/sex/detail?sex=${sex}`),

  // =======================> 收藏 API 完成
  // 返回的指定用户ID的收藏列表
  getCollectionOfUser: (userId) => get(`collection/detail?userId=${userId}`),
  // 添加收藏 type: 0=歌曲, 1=歌单（payload 字段按需传）
  setCollection: (payload) => post(`collection/add`, payload),

  // 取消收藏歌曲
  deleteCollection: (userId, songId) => deletes(`collection/delete?userId=${userId}&&songId=${songId}&&type=0`),
  // 取消收藏歌单
  deleteCollectionSongList: (userId, songListId) => deletes(`collection/delete?userId=${userId}&&songListId=${songListId}&&type=1`),

  // 是否收藏（歌曲/歌单均支持，payload 字段按需传）
  isCollection: (payload) => post(`collection/status`, payload),
  // 是否收藏歌单
  isCollectionSongList: ({ userId, songListId }) => post(`collection/status`, { userId, type: 1, songListId }),

  // =======================> 评分 API 完成
  // 提交评分
  setRank: ({songListId,consumerId,score}) => post(`rankList/add`, {songListId,consumerId,score}),
  // 获取指定歌单的评分
  getRankOfSongListId: (songListId) => get(`rankList?songListId=${songListId}`),
  // 获取指定用户的歌单评分
  getUserRank: (consumerId, songListId) => get(`/rankList/user?consumerId=${consumerId}&songListId=${songListId}`),

  // =======================> 评论 API 完成
  // 添加评论
  setComment: ({userId,content,songId,songListId,nowType}) => post(`comment/add`, {userId,content,songId,songListId,nowType}),
  // 删除评论
  deleteComment: (id) => get(`comment/delete?id=${id}`),
  // 点赞
  setSupport: ({id,up}) => post(`comment/like`, {id,up}),
  // 返回所有评论
  getAllComment: (type, id) => {
    let url = "";
    if (type === 1) {
      url = `comment/songList/detail?songListId=${id}`;
    } else if (type === 0) {
      url = `comment/song/detail?songId=${id}`;
    }
    return get(url);
  },

  // =======================> 歌曲 API
  // 返回全部歌曲
  getAllSong: () => get(`song`),
  // 返回指定歌曲ID的歌曲
  getSongOfId: (id) => get(`song/detail?id=${id}`),
  // 返回指定歌手ID的歌曲
  getSongOfSingerId: (id) => get(`song/singer/detail?singerId=${id}`),
  // 返回指定歌手名的歌曲
  getSongOfSingerName: (keywords) => get(`song/singerName/detail?name=${keywords}`),
  // 下载音乐
  downloadMusic: (url) => get(url, { responseType: "blob" }),

  //======================> 点赞api的优化 避免有些是重复的点赞！新增数据表了得

  testAlreadySupport:({commentId,userId}) => post(`userSupport/test`, {commentId,userId}),

  deleteUserSupport:({commentId,userId}) => post(`userSupport/delete`, {commentId,userId}),

  insertUserSupport:({commentId,userId}) => post(`userSupport/insert`, {commentId,userId}),

  //获取所有的海报
  getBannerList: () => get("banner/getAllBanner"),

  // =======================> 笔记社区 API
  // 发布笔记
  addPost: ({ consumerId, title, content, coverUrl, images, topic }) =>
    post(`post/add`, { consumerId, title, content, coverUrl, images, topic }),
  // 笔记列表（分页）
  getPostList: ({ pageNum = 1, pageSize = 10, order = "latest", topic = "", feed = "all", consumerId }) => {
    const params = [
      `pageNum=${pageNum}`,
      `pageSize=${pageSize}`,
      `order=${order}`,
      `feed=${feed}`,
    ];
    if (topic) params.push(`topic=${encodeURIComponent(topic)}`);
    if (feed === "following" && consumerId) params.push(`consumerId=${consumerId}`);
    return get(`post?${params.join("&")}`);
  },
  // 笔记详情
  getPostDetail: (id) => get(`post/detail?id=${id}`),
  // 我的笔记
  getUserPostList: ({ consumerId, pageNum = 1, pageSize = 10 }) =>
    get(`post/user?consumerId=${consumerId}&pageNum=${pageNum}&pageSize=${pageSize}`),
  // 我点赞的笔记
  getLikedPostList: ({ consumerId, pageNum = 1, pageSize = 10 }) =>
    get(`post/liked?consumerId=${consumerId}&pageNum=${pageNum}&pageSize=${pageSize}`),
  // 点赞 / 取消 / toggle
  likePost: ({ postId, consumerId, like }) => post(`post/like`, { postId, consumerId, like }),
  // 添加评论
  addPostComment: ({ postId, consumerId, content }) => post(`post/comment/add`, { postId, consumerId, content }),
  // 评论列表
  getPostCommentList: ({ postId, pageNum = 1, pageSize = 10 }) =>
    get(`post/comment?postId=${postId}&pageNum=${pageNum}&pageSize=${pageSize}`),

  // =======================> 关注 API
  followUser: ({ userId, followUserId }) => post(`follow`, { userId, followUserId }),
  unfollowUser: ({ userId, followUserId }) => post(`follow/delete`, { userId, followUserId }),
  followStatus: ({ userId, followUserId }) => post(`follow/status`, { userId, followUserId }),
  getFollowings: (userId) => get(`followings?userId=${userId}`),
  getFollowers: (userId) => get(`followers?userId=${userId}`),
  getFollowingUsers: (userId) => get(`followings/users?userId=${userId}`),
  getFollowerUsers: (userId) => get(`followers/users?userId=${userId}`),

  // =======================> 播放记录 API
  addPlayRecord: ({ consumerId, songId, playSeconds, source }) =>
    post(`playRecord/add`, { consumerId, songId, playSeconds, source }),
  getPlayRecordByUser: ({ consumerId, pageNum = 1, pageSize = 10 }) =>
    get(`playRecord/user?consumerId=${consumerId}&pageNum=${pageNum}&pageSize=${pageSize}`),

  // =======================> 推荐 API
  getRecommendSongs: ({ consumerId, limit = 10 }) =>
    get(`recommend/songs?consumerId=${consumerId}&limit=${limit}`),
};



export { HttpManager };
