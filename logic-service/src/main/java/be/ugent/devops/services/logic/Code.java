package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.BaseMove;
import be.ugent.devops.commons.model.MoveFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class Code {
    private String code;
    private String validUntil;
    private String type;
    private boolean used;
    public Code(String code, String validuntil, String name){
        this.validUntil = validuntil;
        this.code = code;
        this.type = name;
        used = false;
    }
    public Code(){
        used = false;
    }

    public String getCode() {
        if(!used && LocalDateTime.from(getValidUntil()).isAfter(LocalDateTime.now())){
            return code;
        }
        return null;
    }
    /*
    public void setCode(String code) {
        this.code = code;
    }
     */

    /*
    public String getType() {
        return type;
    }
    */

    public boolean getUsed() {
        return used;
    }

    /*
    public void setType(String type) {
        this.type = type;
    }
    */

    public TemporalAccessor getValidUntil() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
        TemporalAccessor time = format.parse(validUntil);
        return time;
    }

    /*
    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
    */

    public BaseMove UseCode(){
        if(this.getCode() == null){
            return null;
        }
        used = true;
        return MoveFactory.redeemBonusCode(code);
    }
}
