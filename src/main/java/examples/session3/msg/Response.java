package examples.session3.msg;

import java.io.Serializable;

public class Response implements Serializable {
    public int x;
    public Response(int x) {
        this.x = x;
    }
}

