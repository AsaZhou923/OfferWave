package com.offerwave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offerwave.entity.MaterialPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MaterialPackageMapper extends BaseMapper<MaterialPackage> {

    @Update("UPDATE material_packages "
            + "SET view_count = COALESCE(view_count, 0) + 1 "
            + "WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE material_packages "
            + "SET download_count = COALESCE(download_count, 0) + 1 "
            + "WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);
}
