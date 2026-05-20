package com.example.cookiemusicdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.cookiemusicdemo.common.R;
import com.example.cookiemusicdemo.controller.MinioUploadController;
import com.example.cookiemusicdemo.mapper.ConsumerMapper;
import com.example.cookiemusicdemo.model.domain.Consumer;
import com.example.cookiemusicdemo.model.request.ConsumerRequest;
import com.example.cookiemusicdemo.service.ConsumerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.example.cookiemusicdemo.constant.ConsumerRegexConstants;

import static com.example.cookiemusicdemo.constant.Constants.SALT;

/**
 * 普通用户（consumer 表）业务：注册、登录、资料与密码、头像；管理员侧用户列表与禁用。
 */
@Service
public class ConsumerServiceImpl extends ServiceImpl<ConsumerMapper, Consumer>
        implements ConsumerService {

    @Autowired
    private ConsumerMapper consumerMapper;

    // 用户注册
    @Override
    public R addUser(ConsumerRequest registryRequest) {
        if (this.existUser(registryRequest.getUsername())) {
            return R.warning("用户名已注册");
        }
        if (ConsumerRegexConstants.STRICT_REGISTRY_ENABLED) {
            R regexCheck = validateRegistry(registryRequest);
            if (regexCheck != null) {
                return regexCheck;
            }
        }
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(registryRequest, consumer);
        // MD5 加盐加密后入库（盐值见 Constants.SALT）
        String password = DigestUtils.md5DigestAsHex((SALT + registryRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
        consumer.setPassword(password);
        // 手机号、邮箱为空时存 null
        if (StringUtils.isBlank(consumer.getPhoneNum())) {
            consumer.setPhoneNum(null);
        }
        if ("".equals(consumer.getEmail())) {
            consumer.setEmail(null);
        }
        consumer.setAvator("/user01/consumer/img/default.jpg");
        consumer.setStatus((byte) 1);
        try {
            if (consumerMapper.insert(consumer) > 0) {
                return R.success("注册成功");
            } else {
                return R.error("注册失败");
            }
        } catch (DuplicateKeyException e) {
            return R.fatal(e.getMessage());
        }
    }

    // 修改用户资料（不含密码）
    @Override
    public R updateUserMsg(ConsumerRequest updateRequest) {
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(updateRequest, consumer);
        if (consumerMapper.updateById(consumer) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    // 修改密码（需校验旧密码）
    @Override
    public R updatePassword(ConsumerRequest updatePasswordRequest) {

        if (!this.verityPasswd(updatePasswordRequest.getUsername(), updatePasswordRequest.getOldPassword())) {
            return R.error("密码输入错误");
        }

        Consumer consumer = new Consumer();
        consumer.setId(updatePasswordRequest.getId());
        // 新密码同样 MD5 加盐后更新
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + updatePasswordRequest.getPassword()).getBytes(StandardCharsets.UTF_8));
        consumer.setPassword(secretPassword);

        if (consumerMapper.updateById(consumer) > 0) {
            return R.success("密码修改成功");
        } else {
            return R.error("密码修改失败");
        }
    }

    // 上传用户头像（MinIO + 更新 avator 字段）
    @Override
    public R updateUserAvator(MultipartFile avatorFile, int id) {
        String fileName = avatorFile.getOriginalFilename();
        String imgPath = "/user01/consumer/img/" + fileName;
        Consumer consumer = new Consumer();
        consumer.setId(id);
        consumer.setAvator(imgPath);
        System.out.println("UpdateConsumerImg" + imgPath);
        String s = MinioUploadController.uploadAtorImgFile(avatorFile);
        if (s.equals("File uploaded successfully!") && consumerMapper.updateById(consumer) > 0) {
            return R.success("上传成功", imgPath);
        } else {
            return R.error("上传失败");
        }
    }

    // 判断用户名是否已存在
    @Override
    public boolean existUser(String username) {
        QueryWrapper<Consumer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return consumerMapper.selectCount(queryWrapper) > 0;
    }

    // 校验用户名与密码是否匹配（明文密码先 MD5 加盐再与库比对）
    @Override
    public boolean verityPasswd(String username, String password) {
        QueryWrapper<Consumer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes(StandardCharsets.UTF_8));

        queryWrapper.eq("password", secretPassword);
        return consumerMapper.selectCount(queryWrapper) > 0;
    }

    // 删除用户
    @Override
    public R deleteUser(Integer id) {
        if (consumerMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    // 查询全部用户
    @Override
    public R allUser() {
        return R.success(null, consumerMapper.selectList(null));
    }

    // 按用户 ID 查询
    @Override
    public R userOfId(Integer id) {
        QueryWrapper<Consumer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        return R.success(null, consumerMapper.selectList(queryWrapper));
    }

    // 用户登录：校验密码、禁用状态，成功后写入 Session
    @Override
    public R loginStatus(ConsumerRequest loginRequest, HttpSession session) {

        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        if (this.verityPasswd(username, password)) {
            Consumer consumer = new Consumer();
            consumer.setUsername(username);
            Consumer dbUser = consumerMapper.selectOne(new QueryWrapper<>(consumer));
            if (dbUser == null) {
                return R.error("用户名或密码错误");
            }
            // status=0 表示管理员已禁用
            if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
                return R.error("账号已被禁用，请联系管理员");
            }
            session.setAttribute("username", username);
            return R.success("登录成功", consumerMapper.selectList(new QueryWrapper<>(consumer)));
        } else {
            return R.error("用户名或密码错误");
        }
    }

    // 管理员：用户分页列表（关键字、状态筛选）
    @Override
    public R adminUserPage(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pn - 1) * ps;
        Integer queryStatus = (status != null && (status < 0 || status > 1)) ? null : status;

        Map<String, Object> data = new HashMap<>();
        data.put("items", consumerMapper.selectAdminUserPage(offset, ps, keyword, queryStatus));
        data.put("total", consumerMapper.countAdminUsers(keyword, queryStatus));
        data.put("pageNum", pn);
        data.put("pageSize", ps);
        return R.success("管理员用户列表", data);
    }

    // 管理员：禁用/解禁用户（status 0 禁用，1 正常）
    @Override
    public R adminUpdateUserStatus(Integer userId, Integer status) {
        if (userId == null || status == null || (status != 0 && status != 1)) {
            return R.error("参数错误");
        }
        Consumer db = consumerMapper.selectById(userId);
        if (db == null) {
            return R.error("用户不存在");
        }
        Consumer update = new Consumer();
        update.setId(userId);
        update.setStatus((byte) status.intValue());
        if (consumerMapper.updateById(update) > 0) {
            return R.success(status == 1 ? "已解禁" : "已禁用");
        }
        return R.error("操作失败");
    }

    /**
     * 严格注册模式：正则校验
     */
    private R validateRegistry(ConsumerRequest req) {
        if (req.getUsername() == null || !req.getUsername().matches(ConsumerRegexConstants.USERNAME)) {
            return R.error("用户名格式不正确（4-20位字母、数字或下划线）");
        }
        if (req.getPassword() == null || !req.getPassword().matches(ConsumerRegexConstants.PASSWORD)) {
            return R.error("密码格式不正确（8-32位且包含字母和数字）");
        }
        if (StringUtils.isNotBlank(req.getPhoneNum())
                && !req.getPhoneNum().matches(ConsumerRegexConstants.PHONE)) {
            return R.error("手机号格式不正确");
        }
        if (StringUtils.isNotBlank(req.getEmail())
                && !req.getEmail().matches(ConsumerRegexConstants.EMAIL)) {
            return R.error("邮箱格式不正确");
        }
        return null;
    }
}
