package fi.peltodata.domain;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FarmfieldMapper {
    Map<String,Object> findFarmField(@Param("farmfieldId") long farmfieldId);
    List<Map<String,Object>> findAllFarmFields();
    List<Map<String,Object>> findAllFarmFieldsByUserId(@Param("userId") long userId);
    long update(final Farmfield farmfield);
    void insertFarmField(final Farmfield farmfield);
    void insertFarmFieldMapLayer(@Param("farmfieldId") long farmfieldId, @Param("maplayerId") long maplayerId);
    long delete(final long farmfieldId);
    List<Integer> findFarmLayers(@Param("farmfieldId") long farmfieldId);
}
