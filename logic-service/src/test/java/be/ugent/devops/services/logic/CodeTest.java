package be.ugent.devops.services.logic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeTest {
    @Test
    public void UseCode(){
        Code code = new Code("test","Wed, 31 Dec 2025 23:59:00 GMT", "testcode");
        Assertions.assertEquals(false, code.getUsed());
        code.UseCode();
        Assertions.assertEquals(true, code.getUsed());
    }

    @Test
    public void ExpiredCode(){
        Code code = new Code("test","Thu, 31 Dec 2020 23:59:00 GMT", "testcode");
        Assertions.assertEquals(false, code.getUsed());
        Assertions.assertEquals(null, code.UseCode());
        Assertions.assertEquals(false, code.getUsed());
    }
}
