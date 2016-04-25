package ru.georgeee.bachelor.yarn.common;

import java.util.Collections;
import java.util.Map;

public class IdValueMapping<T> {
    private final Map<T, Integer> idByCode;
    private final Map<Integer, T> codeById;

    public IdValueMapping(Map<T, Integer> idByCode, Map<Integer, T> codeById) {
        this.idByCode = Collections.unmodifiableMap(idByCode);
        this.codeById = Collections.unmodifiableMap(codeById);
    }

    public T getById(Integer id) {
        return codeById.get(id);
    }

    public Integer getByCode(T val) {
        return idByCode.get(val);
    }
}
