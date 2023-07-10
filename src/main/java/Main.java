import com.wkf.tomcat.Reactor;

public class Main {
    public static void main(String[] args) throws Exception {
        new Reactor(8080).start();
    }
}