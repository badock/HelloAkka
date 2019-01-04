package distributed.simple.msg;

import java.io.Serializable;

public class Greeting implements Serializable {
    public final String message;

    public Greeting(String message) {
        this.message = message;
    }
}
