package org.commonwl.view.util;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@TypeDefs({@TypeDef(name = "json", typeClass = JsonBinaryType.class)})
@MappedSuperclass
public class BaseEntity {}
