package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.db.HibernateHelper;
import ru.georgeee.bachelor.yarn.dict.Dict;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Dict_Cache")
@Getter @ToString
public class DictCacheEntry extends BaseEntity {
    @Basic
    @Setter
    private String dictKey;
    @Basic
    @Setter
    private String word;
    @Transient
    private List<Dict.Translation> translations;

    public void setTranslations(List<Dict.Translation> translations) {
        this.translations = translations instanceof Serializable ? translations : new ArrayList<>(translations);
    }

    @Access(AccessType.PROPERTY)
    @Column(name = "data")
    public byte[] getData() {
        return HibernateHelper.serialize(translations);
    }

    public void setData(byte[] data) {
        translations = HibernateHelper.deserialize(data);
    }
}
