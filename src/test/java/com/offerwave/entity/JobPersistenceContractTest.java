package com.offerwave.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JobPersistenceContractTest {

    @Test
    void uniqueHashMustBeWritableByMybatisPlus() throws Exception {
        TableField tableField = Job.class.getDeclaredField("uniqueHash").getAnnotation(TableField.class);

        assertNotEquals(FieldStrategy.NEVER, tableField.insertStrategy());
        assertNotEquals(FieldStrategy.NEVER, tableField.updateStrategy());
    }
}
