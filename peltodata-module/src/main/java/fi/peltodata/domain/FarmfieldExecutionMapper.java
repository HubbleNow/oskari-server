package fi.peltodata.domain;

import java.util.List;
import java.util.Map;

public interface FarmfieldExecutionMapper {
    List<Map<String,Object>> findAllFarmFieldExecutionsForUser();
}
