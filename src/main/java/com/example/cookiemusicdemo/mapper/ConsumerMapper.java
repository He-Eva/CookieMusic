package com.example.cookiemusicdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cookiemusicdemo.model.domain.Consumer;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsumerMapper extends BaseMapper<Consumer> {

    List<Consumer> selectAdminUserPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("keyword") String keyword,
            @Param("status") Integer status
    );

    long countAdminUsers(
            @Param("keyword") String keyword,
            @Param("status") Integer status
    );
}
