## tinytomcat

一个用Java编写的Http服务器,有以下特点:

- 利用NIO的IO多路复用和Reactor模式,配合内部实现的线程池形成整体的并发模型
- 使用零拷贝技术高效的实现了二进制类型Http响应
- 利用状态机解析Http请求,实现了对Get,Post,Put,Delete请求的解析
- 支持Cookie和Session功能,支持解析:url query, multipart form等类型请求参数
- 使用定时器清理长时间不活动的Socket连接,封装了一个简单的Socket线程同步工具供内部使用
- 利用反射技术,支持将Http中的请求参数自动映射成Java对象
- 支持用户方便的定义Http请求的处理器,用户只需添加对应的注解,系统就可以自动注册该处理器

## 压力测试

采用jmeter工具进行压测,采用的平台是macos13.5,采用5000个并发请求,模拟20轮请求:执行结果如下所示

```shell
$ tail -n 1 jmeter.log

2023-08-06 19:26:53,021 INFO o.a.j.r.Summariser: summary = 400000 in 00:00:09 = 42978.4/s Avg:    27 Min:     0 Max:  1300 Err:    59 (0.01%)
```

> 因操作系统限制文件描述符不能同时打开太多,后续会在linux上进行测试,上述请求接口进行了具体的文件读写并响应真实的文件数据,而非一个空白响应

## 使用姿势

- 注册一个处理器
    ```java
    @RequestMetadata(path = "/dog", method = GET)
    public void dog(HttpRequest request, HttpResponse response) throws Exception {
        request.setSession("lastReqTime", new Date());
        response.writeJson(Json.marshal(request.getSession("lastReqTime")));
    }
    ```
  你只需要添加Http请求的注解,注意需要写明请求路径,请求方式,利用`request`和`response`中封装的方法可以完成Http响应.
- 获得请求参数

  有几种获取请求参数的方式

    - 通过request封装的方法获得:该方式可以获取路径参数(即url query)和Multipartform,以及urlencode类型的参数
        ```java
        String value = request.getRequestParam("key");
        ```
    - 直接获取请求对象:你需要在注册的处理器中添加更多的参数,目前支持多个对象自动注入,但是还不支持对象嵌套
        ```java
        @RequestMetadata(path = "/cat", method = POST)
        public void cat(HttpRequest request, HttpResponse response, Cat cat) throws Exception {
            //在这里根据cat进行一系列操作
        }
        ```
      其中Cat实体类你需要写入相应注解:
        ```java
        @RequestParameter
        public class Cat {
            @AutoCompleteEnable(id = "food")
            public String food;
            @AutoCompleteEnable(id = "master")
            public String master;

            @Override
            public String toString() {
                return "Cat{" +
                        "food='" + food + '\'' +
                        ", master='" + master + '\'' +
                        '}';
            }
        }
        ```
      接着,你可以构造如下的请求来直接获得cat参数:
      `http://localhost:8080/cat?food=a&master=b`
      > 该种参数自动的方式依赖于`AutoCompleteEnable`注解中的id属性
    - 对于复杂的请求参数,建议采用POST请求,并且在请求载荷中用json传递,该种方式可以完成复杂对象的自动装载,本项目依赖于Jackson来完成反序列化操作,进而自动装载请求参数
- 设置session

  HttpRequest类型中封装好了有关session的操作方法,你只需要直接调用如下方法即可:
    ```java
    @RequestMetadata(path = "/dog", method = GET)
    public void dog(HttpRequest request, HttpResponse response) throws Exception {
        request.setSession("lastReqTime", new Date());
        response.writeJson(Json.marshal(request.getSession("lastReqTime")));
    }
    //setSession:设置一个参数
    //getSession: 得到之前设置过得session对象
    //writeJson: HttpResponse内部封装的方法,用来返回json格式的http响应
    ```
- 得到请求Cookie

  对于Cookie你只需要参照http请求头的数据,得到由前端传递的Cookie数据,该项操作也封装成了一个方法:
    ```java
    @RequestMetadata(path = "/index", method = GET)
    public void index(HttpRequest request, HttpResponse response) throws Exception {
        String cookie = request.getRequestCookie();
        new DefaultHandler().doHandle(request, response);
    }
    ```

## 响应请求

- 目前`HttpResponse`内部封装了一些方法以供调用:

```java
    //直接返回json字符串
    public void writeJson(String response) throws Exception {
    }
    //字节流的形式返回json
    public void writeJson(byte[] response) throws Exception {
    }
    //返回html字符串
    public void writeHtml(String response) throws Exception {
    }
    //以字节流的形式返回html文本
    public void writeHtml(byte[] response) throws Exception {
    }
    //响应二进制信息
    public void writeBinary(byte[] response) throws Exception {
    }
    //零拷贝的方式高效的返回二进制数据
    //目前支持:html,css,js,png,jpg,octet-stream
    //需要使用者输入路径便可以通过文件类型发送正确的http响应报头响应数据
    public void writeBinary(String path) throws Exception {
    }

```

- 一个使用例子 `GET http://127.0.0.1:8080/index.html`

  上述静态文件处理器无需用户注册,因为默认的实现方式是:
  当一个http请求到达时,优先匹配用户注册的处理器,若无法匹配,则尝试匹配到静态文件处理器,若静态文件处理器依然无法处理该请求,则返回404的相应报头
    ```java
    public class StaticFilesHandler implements HttpRequestHandler {

        public Set<String> allowedFileTypes = new HashSet<>(Arrays.asList("html", "png", "jpeg", "webp", "js", "css", "jpg"));

        @Override
        public boolean hit(HttpRequest request) {
            String path = request.getRequestHeader().path;
            String[] split = path.split("\\.");
            String type = split[split.length - 1];
            return allowedFileTypes.contains(type) && request.getRequestHeader().getMethod().equals(GET);
        }

        @Override
        public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
            response.writeBinary(root + request.getRequestHeader().getPath());
        }
    }
    ```
  其中, `hit()`
  方法先判断请求的文件是否是合法类型,然后再续判断该请求是否为GET请求,若两个条件均符合则返回给该请求的文件内容,具体的实现可以参照`HttpResponse`
  源码