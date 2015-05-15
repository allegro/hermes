package pl.allegro.tech.hermes.common.time;

import java.util.Date;

public class SystemClock implements Clock {

    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Date getDate() {
        return new Date(getTime());
    }
    
}
