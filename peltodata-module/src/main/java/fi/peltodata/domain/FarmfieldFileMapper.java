package fi.peltodata.domain;

import org.apache.ibatis.annotations.*;

import java.util.List;

public interface FarmfieldFileMapper {
    @Select("select id, original_file_name, full_path, file_date, field_id, type from peltodata_field_file")
    @Results({
            @Result(property = "fileDate", column = "file_date"),
            @Result(property = "originalFilename", column = "original_file_name"),
            @Result(property = "fullPath", column = "full_path"),
            @Result(property = "farmfieldId", column = "field_id")
    })
    List<FarmfieldFile> findAllFarmfieldFiles();

    @Select("select id, original_file_name, full_path, file_date, field_id, type from peltodata_field_file where id = #{id}")
    @Results({
            @Result(property = "fileDate", column = "file_date"),
            @Result(property = "originalFilename", column = "original_file_name"),
            @Result(property = "fullPath", column = "full_path"),
            @Result(property = "farmfieldId", column = "field_id")
    })
    FarmfieldFile findFarmfieldFileById(long id);

    @Select("select id, original_file_name, full_path, file_date, field_id, type from peltodata_field_file where field_id = #{farmfieldId}")
    @Results({
            @Result(property = "fileDate", column = "file_date"),
            @Result(property = "originalFilename", column = "original_file_name"),
            @Result(property = "fullPath", column = "full_path"),
            @Result(property = "farmfieldId", column = "field_id")
    })
    List<FarmfieldFile> findFarmfieldFileForFarm(long farmfieldId);

    @Insert("INSERT INTO peltodata_field_file (original_file_name, full_path, file_date, field_id, type) " +
            "VALUES (#{originalFilename}, #{fullPath}, #{fileDate}, #{farmfieldId}, #{type})")
    @Options(useGeneratedKeys=true)
    void insertFarmfieldFile(FarmfieldFile file);

    @Update("UPDATE peltodata_field_file SET " +
            "original_file_name=#{originalFilename}, " +
            "full_path=#{fullPath}, " +
            "file_date=#{fileDate}, " +
            "field_id=#{farmfieldId}, " +
            "type=#{type} " +
            "WHERE id=#{id}")
    void udpateFarmfieldFile(FarmfieldFile file);

    @Delete("delete from peltodata_field_file where id = #{id}")
    void deleteFarmfieldFile(long id);
}
