import axios from 'axios'
import router from '../router'

const BASE_URL = process.env.NODE_HOST

axios.defaults.timeout = 5000 // 超时时间设置
axios.defaults.withCredentials = true // true允许跨域
axios.defaults.baseURL = BASE_URL //从本地环境中动态获取本机地址
// Content-Type POST响应头  表单数据：键值对形式/字符集：UTF-8
axios.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'

// 响应拦截器
axios.interceptors.response.use(
  response => {
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    // 否则的话抛出错误
    if (response.status === 200) {
      return Promise.resolve(response)
    } else {
      return Promise.reject(response)
    }
  },
  // 服务器状态码不是2开头的的情况
  error => {
    if (error.response.status) {
      switch (error.response.status) {
        // 401: 未登录
        case 401:
          router.replace({
            path: "/",
            query: {
              // redirect: router.currentRoute.fullPath
            },
          });
          break;
        case 403:
          // console.log('管理员权限已修改请重新登录')
          // 跳转登录页面，并将要浏览的页面fullPath传过去，登录成功后跳转需要访问的页面
          setTimeout(() => {
            router.replace({
              path: "/",
              query: {
                // redirect: router.currentRoute.fullPath
              },
            });
          }, 1000);
          break;

        // 404请求不存在
        case 404:
          // console.log('请求页面飞到火星去了')
          break;
      }
      return Promise.reject(error.response);
    }
  }
)

export function getBaseURL() {
  return BASE_URL;
}

/**
 * 封装get方法
 * @param url
 * @param data
 * @returns {Promise}
 */
//传入url,参数（可选）
export function get(url, params?: object) {
  //返回一个新的promise对象
  return new Promise((resolve, reject) => {
    //使用axios库发送get请求
    axios.get(url, params).then(
        //请求成功时，调用resolve并将响应的数据部分作为参数传递
      response => resolve(response.data),
      //请求失败时，调用reject并将错误信息作为参数传递
      error => reject(error)
    )
  });
}

/**
 * 封装post请求
 * @param url
 * @param data
 * @returns {Promise}
 */
//传入url,data请求体（要发送的数据，默认为一个空对象）
export function post(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.post(url, data).then(
      response => resolve(response.data),
      error => reject(error)
    );
  });
}

/**
 * 封装delete请求
 * @param url
 * @param data
 * @returns {Promise}
 */
export function deletes(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.delete(url, data).then(
      response => resolve(response.data),
      error => reject(error)
    );
  });
}

/**
 * 封装put请求
 * @param url
 * @param data
 * @returns {Promise}
 */
export function put(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.put(url, data).then(
      response => resolve(response.data),
      error => reject(error)
    );
  });
}
