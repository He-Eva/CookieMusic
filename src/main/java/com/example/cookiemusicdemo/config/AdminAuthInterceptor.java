package com.example.cookiemusicdemo.config;

import com.alibaba.fastjson.JSON;
import com.example.cookiemusicdemo.common.R;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }

        // admin login endpoint is public
        if ("/admin/login/status".equals(uri)) {
            return true;
        }

        // protect /admin/** and content-management write APIs
        boolean isAdminPath = uri.startsWith("/admin/");
        boolean isSongWritePath = uri.startsWith("/song/")
                && !("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method));
        boolean isSongListProtected = isSongListNeedAdmin(uri, method);
        boolean isListSongProtected = isListSongNeedAdmin(uri, method);

        if (!isAdminPath && !isSongWritePath && !isSongListProtected && !isListSongProtected) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object adminName = (session == null) ? null : session.getAttribute("name");
        if (adminName != null) {
            return true;
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(R.error("管理员未登录或登录已过期")));
        return false;
    }

    /** 歌单：仅列表/详情/按标题或风格查询可匿名 GET，其余需管理员 */
    private boolean isSongListNeedAdmin(String uri, String method) {
        if (!uri.startsWith("/songList")) {
            return false;
        }
        boolean isGet = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (isGet) {
            if ("/songList/delete".equals(uri)) {
                return true;
            }
            if ("/songList".equals(uri)
                    || "/songList/detail".equals(uri)
                    || uri.startsWith("/songList/likeTitle/detail")
                    || uri.startsWith("/songList/style/detail")) {
                return false;
            }
            return true;
        }
        return true;
    }

    /** 歌单内歌曲：仅按歌单查列表可匿名 GET，其余需管理员 */
    private boolean isListSongNeedAdmin(String uri, String method) {
        if (!uri.startsWith("/listSong")) {
            return false;
        }
        boolean isGet = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (isGet) {
            if (uri.startsWith("/listSong/detail")) {
                return false;
            }
            return true;
        }
        return true;
    }
}

