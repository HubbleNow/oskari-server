package fi.peltodata.domain;

import org.apache.ibatis.annotations.*;

import java.util.List;

public interface FarmfieldExecutionMapper {
    @Select("select exec.id, state, execution_started_at, field_id, output_type, output_filename, field_file_id from peltodata_field_execution exec" +
            " inner join peltodata_field pf on pf.id = exec.field_id where pf.user_id = #{id}")
    @Results({
            @Result(property = "executionStartedAt", column = "execution_started_at"),
            @Result(property = "outputType", column = "output_type"),
            @Result(property = "farmfieldId", column = "field_id"),
            @Result(property = "farmfieldFileId", column = "field_file_id"),
            @Result(property = "outputFilename", column = "output_filename")
    })
    List<FarmfieldExecution> findAllFarmFieldExecutionsForUser(Long userId);

    @Select("select id, state, execution_started_at, field_id, output_type, output_filename, field_file_id from peltodata_field_execution")
    @Results({
            @Result(property = "executionStartedAt", column = "execution_started_at"),
            @Result(property = "outputType", column = "output_type"),
            @Result(property = "farmfieldId", column = "field_id"),
            @Result(property = "farmfieldFileId", column = "field_file_id"),
            @Result(property = "outputFilename", column = "output_filename")
    })
    List<FarmfieldExecution> findAllFarmfieldExecutions();
    @Select("select id, state, execution_started_at, field_id, output_type, output_filename, field_file_id from peltodata_field_execution where id = #{id}")
    @Results({
            @Result(property = "executionStartedAt", column = "execution_started_at"),
            @Result(property = "outputType", column = "output_type"),
            @Result(property = "farmfieldId", column = "field_id"),
            @Result(property = "farmfieldFileId", column = "field_file_id"),
            @Result(property = "outputFilename", column = "output_filename")
    })
    FarmfieldExecution findFarmfieldById(long id);

    @Insert("INSERT INTO peltodata_field_execution (state, field_id, output_type, output_filename, field_file_id) " +
            "VALUES (#{state}, #{farmfieldId}, #{outputType}, #{outputFilename}, #{farmfieldFileId})")
    @Options(useGeneratedKeys=true)
    void insertFarmfieldExecution(FarmfieldExecution execution);

    @Update("UPDATE peltodata_field_execution SET " +
            "state=#{state}, " +
            "output_type=#{outputType}, " +
            "field_id=#{farmfieldId}, " +
            "execution_started_at=#{executionStartedAt}, " +
            "output_filename=#{outputFilename}, " +
            "field_file_id=#{farmfieldFileId} " +
            "WHERE id=#{id}")
    void updateFarmfieldExecution(FarmfieldExecution execution);

    @Delete("delete from peltodata_field_execution where id = #{id}")
    void deleteFarmfieldExecution(long id);
}
