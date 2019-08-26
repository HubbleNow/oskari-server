package fi.peltodata.service;

import fi.peltodata.domain.Farmfield;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FarmfieldMapper {
    List<Map<String,Object>> findFarmField(long farmfieldId);
    List<Map<String,Object>> findAllFarmFields();
    long update(final Farmfield farmfield);
    void insertFarmField(final Farmfield farmfield);
    void insertFarmFieldMapLayer(@Param("farmfieldId") long farmfieldId, @Param("maplayerId") long maplayerId);
    long delete(final long farmfieldId);

}
