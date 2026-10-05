package arc.util;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utility class for making HTTP requests.
 * 发起 HTTP 请求的工具类。
 */
public class Http{
    protected static ExecutorService exec = Threads.unboundedExecutor("HTTP", 1);

    /** @return a new HttpRequest that must be configured & submitted. 返回一个需要配置并提交的新 HttpRequest。 */
    public static HttpRequest request(HttpMethod method, String url){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(method).url(url);
    }

    /** @return a new GET HttpRequest that must be configured & submitted. 返回一个需要配置并提交的新 GET HttpRequest。 */
    public static HttpRequest get(String url){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(HttpMethod.GET).url(url);
    }

    /**
     * Creates and submits a HTTP GET request.
     * 创建并提交 HTTP GET 请求。
     */
    public static void get(String url, ConsT<HttpResponse, Exception> callback){
        get(url).submit(callback);
    }

    /**
     * Creates and submits a HTTP GET request, with an error handler.
     * 创建并提交 HTTP GET 请求,并带有错误处理器。
     */
    public static void get(String url, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
        get(url).error(error).submit(callback);
    }

    /** @return a new POST HttpRequest that must be configured & submitted. 返回一个需要配置并提交的新 POST HttpRequest。 */
    public static HttpRequest post(String url){
        return post(url, (String)null);
    }

    /**
     * Creates and submits a HTTP POST request.
     * 创建并提交 HTTP POST 请求。
     */
    public static void post(String url, ConsT<HttpResponse, Exception> callback){
        post(url).submit(callback);
    }

    /** @return a new POST HttpRequest that must be configured & submitted. 返回一个需要配置并提交的新 POST HttpRequest。 */
    public static HttpRequest post(String url, String content){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(HttpMethod.POST).url(url).content(content);
    }

    public static class HttpResponse{
        private final HttpURLConnection connection;
        private HttpStatus status;

        protected HttpResponse(HttpURLConnection connection) throws IOException{
            this.connection = connection;
            this.status = HttpStatus.byCode(connection.getResponseCode());
        }

        /** @return the length of received content in bytes as a long. May throw an exception (?) 已接收内容的长度(以字节为单位的 long)。可能抛出异常(?) */
        public long getContentLength(){
            return connection.getContentLength();
        }

        /**
         * Returns the data of the HTTP response as a byte[].
         * <p>
         * <b>Note</b>: This method may only be called once per response.
         * </p>
         * <p>
         * 以 byte[] 形式返回 HTTP 响应的数据。
         * <p>
         * <b>注意</b>:每个响应只能调用一次该方法。
         * </p>
         * @return the result as a byte[] or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with {@link HttpRequest#timeout(int)} 结果 byte[],若超时或操作被取消/异常终止则为 null。超时在创建 HTTP 请求时通过 {@link HttpRequest#timeout(int)} 指定
         */
        public byte[] getResult(){
            InputStream input = getResultAsStream();

            // If the response does not contain any content, input will be null.
            // 如果响应不包含任何内容,input 将为 null。
            if(input == null){
                return Streams.emptyBytes;
            }

            try{
                return Streams.copyBytes(input, connection.getContentLength());
            }catch(IOException e){
                return Streams.emptyBytes;
            }finally{
                Streams.close(input);
            }
        }

        /**
         * Returns the data of the HTTP response as a {@link String}.
         * <p>
         * <b>Note</b>: This method may only be called once per response.
         * </p>
         * <p>
         * 以 {@link String} 形式返回 HTTP 响应的数据。
         * <p>
         * <b>注意</b>:每个响应只能调用一次该方法。
         * </p>
         * @return the result as a string or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with {@link HttpRequest#timeout(int)} 结果字符串,若超时或操作被取消/异常终止则为 null。超时在创建 HTTP 请求时通过 {@link HttpRequest#timeout(int)} 指定
         */
        public String getResultAsString(){
            InputStream input = getResultAsStream();

            // If the response does not contain any content, input will be null.
            // 如果响应不包含任何内容,input 将为 null。
            if(input == null){
                return "";
            }

            try{
                return Streams.copyString(input, connection.getContentLength());
            }catch(IOException e){
                return "";
            }finally{
                Streams.close(input);
            }
        }

        /**
         * Returns the data of the HTTP response as an {@link InputStream}. <b><br>
         * Warning:</b> Do not store a reference to this InputStream. The underlying HTTP connection will be closed after that
         * callback finishes executing. Reading from the InputStream after it's connection has been closed will lead to exception.
         * <p>
         * 以 {@link InputStream} 形式返回 HTTP 响应的数据。<b><br>
         * 警告:</b>不要保存对该 InputStream 的引用。底层 HTTP 连接会在该回调执行完毕后关闭,连接关闭后再从 InputStream 读取将导致异常。
         * @return An {@link InputStream} with the {@link HttpResponse} data. 包含 {@link HttpResponse} 数据的 {@link InputStream}。
         */
        public InputStream getResultAsStream(){
            try{
                return connection.getInputStream();
            }catch(IOException e){
                return connection.getErrorStream();
            }
        }

        /** @return the {@link HttpStatus} containing the statusCode of the HTTP response. 包含 HTTP 响应 statusCode 的 {@link HttpStatus}。 */
        public HttpStatus getStatus(){
            return status;
        }

        /** @return the value of the header with the given name as a {@link String}, or null if the header is not set. 给定名称的请求头的值({@link String}),若未设置该请求头则为 null。 */
        public String getHeader(String name){
            return connection.getHeaderField(name);
        }

        /**
         * Returns a Map of the headers. The keys are Strings that represent the header name. Each values is a List of Strings that
         * represent the corresponding header values.
         * <p>
         * 返回请求头的 Map。键为表示请求头名称的字符串,每个值是表示对应请求头值的字符串列表。
         */
        public ObjectMap<String, Ar<String>> getHeaders(){
            //convert between the struct types
            // 在结构类型之间转换
            ObjectMap<String, Ar<String>> out = new ObjectMap<>();
            Map<String, List<String>> fields = connection.getHeaderFields();
            for(String key : fields.keySet()){
                if(key != null){
                    out.put(key, Ar.with(fields.get(key).toArray(new String[0])));
                }
            }
            return out;
        }

    }

    public static class HttpRequest{
        public HttpMethod method = HttpMethod.GET;
        /**
         * The URL to send this request to.
         * 发送此请求的目标 URL。
         */
        public String url;
        public ObjectMap<String, String> headers = new ObjectMap<>();
        /**The time to wait for the HTTP request to be processed, use 0 to block until it is done. The timeout is used for both
         * <p>
         * 等待 HTTP 请求被处理的时间,设为 0 表示阻塞直到完成。该超时既用作建立 TCP 连接的超时,也用作等待接收到第一个字节数据的超时。
         * the timeout when establishing TCP connection, and the timeout until the first byte of data is received.*/
        public int timeout = 8000;

        /**The content to be used in the HTTP request: A string encoded in the corresponding Content-Encoding set in the headers, with the data to send with the
         * HTTP request. For example, in case of HTTP GET, the content is used as the query string of the GET while on a
         * <p>
         * HTTP 请求中使用的内容:按请求头中设置的相应 Content-Encoding 编码的字符串,包含随 HTTP 请求发送的数据。例如 HTTP GET 时,内容用作 GET 的查询字符串;HTTP POST 时,用于发送 POST 数据。
         * HTTP POST it is used to send the POST data.*/
        public String content;

        /**
         * The content as a stream to be used for a POST for example, to transmit custom data.
         * 以流形式提供的内容,例如用于 POST 以传输自定义数据。
         */
        public InputStream contentStream;

        /**Sets whether 301 and 302 redirects are followed. By default true. Can't be changed in the web backend because this uses
         * <p>
         * 设置是否遵循 301 和 302 重定向。默认为 true。web 后端中无法更改,因为它使用的 XmlHttpRequest 总是重定向。
         * XmlHttpRequests which always redirect.*/
        public boolean followRedirects = true;
        /**
         * Whether a cross-origin request will include credentials. Default: false
         * 跨域请求是否包含凭据。默认:false
         */
        public boolean includeCredentials = false;
        /**
         * Handler for 4xx + 5xx errors, as well as exceptions thrown during connection.
         * 用于 4xx + 5xx 错误以及连接期间抛出的异常的处理器。
         */
        public Cons<Throwable> errorHandler = Log::err;

        protected HttpRequest(){

        }

        protected HttpRequest(HttpMethod method){
            this.method = method;
        }

        public HttpRequest error(Cons<Throwable> failed){
            errorHandler = failed;
            return this;
        }

        public HttpRequest method(HttpMethod method){
            this.method = method;
            return this;
        }

        public HttpRequest url(String url){
            this.url = url;
            return this;
        }

        public HttpRequest timeout(int timeout){
            this.timeout = timeout;
            return this;
        }

        public HttpRequest redirects(boolean followRedirects){
            this.followRedirects = followRedirects;
            return this;
        }

        public HttpRequest credentials(boolean includeCredentials){
            this.includeCredentials = includeCredentials;
            return this;
        }

        public HttpRequest header(String name, String value){
            headers.put(name, value);
            return this;
        }

        public HttpRequest content(String content){
            this.content = content;
            return this;
        }

        public HttpRequest content(InputStream contentStream){
            this.contentStream = contentStream;
            return this;
        }

        /**
         * Submits this request asynchronously.
         * 异步提交此请求。
         */
        public void submit(ConsT<HttpResponse, Exception> success){
            Http.exec.submit(() -> block(success));
        }

        /**
         * Blocks until this request is done.
         * 阻塞直到此请求完成。
         */
        public void block(ConsT<HttpResponse, Exception> success){
            if(url == null){
                errorHandler.get(new ArcRuntimeException("can't process a HTTP request without URL set"));
                return;
            }

            try{
                URL url;

                if(method == HttpMethod.GET){
                    String queryString = "";
                    String value = content;
                    if(value != null && !"".equals(value)) queryString = "?" + value;
                    url = new URL(this.url + queryString);
                }else{
                    url = new URL(this.url);
                }

                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                //should be enabled to upload data.
                // 应启用此项才能上传数据。
                boolean doingOutPut = method == HttpMethod.POST || method == HttpMethod.PUT;
                connection.setDoOutput(doingOutPut);
                connection.setDoInput(true);
                connection.setRequestMethod(method.toString());
                HttpURLConnection.setFollowRedirects(followRedirects);

                //set headers
                // 设置请求头
                headers.each(connection::addRequestProperty);

                //timeouts
                // 超时设置
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);

                try{
                    // Set the content for POST and PUT (GET has the information embedded in the URL)
                    // 为 POST 和 PUT 设置内容(GET 的信息已嵌入 URL 中)
                    if(doingOutPut){
                        // we probably need to use the content as stream here instead of using it as a string.
                        // 这里可能需要将内容作为流使用,而不是作为字符串使用。
                        if(content != null){
                            try(OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), Strings.utf8)){
                                writer.write(content);
                            }
                        }else{
                            if(contentStream != null){
                                try(OutputStream os = connection.getOutputStream()){
                                    Streams.copy(contentStream, os);
                                }
                            }
                        }
                    }

                    connection.connect();

                    try{
                        int code = connection.getResponseCode();

                        //4xx or 5xx error
                        // 4xx 或 5xx 错误
                        if(code >= 400){
                            HttpStatus status = HttpStatus.byCode(code);
                            errorHandler.get(new HttpStatusException("HTTP request failed with error: " + code + " (" + status + ", URL = " + url + ")", status, new HttpResponse(connection)));
                        }else{
                            success.get(new HttpResponse(connection));
                        }

                    }finally{
                        connection.disconnect();
                    }

                }catch(Throwable e){
                    connection.disconnect();
                    errorHandler.get(e);
                }
            }catch(Throwable e){
                errorHandler.get(e);
            }
        }
    }

    /**
     * Exception returned when a 4xx or 5xx error is encountered.
     * 遇到 4xx 或 5xx 错误时返回的异常。
     */
    public static class HttpStatusException extends RuntimeException{
        /**
         * The 4xx or 5xx error code.
         * 4xx 或 5xx 错误代码。
         */
        public HttpStatus status;
        /**
         * The response that was sent along with the status code.
         * 随状态码一起发送的响应。
         */
        public HttpResponse response;

        public HttpStatusException(String message, HttpStatus status, HttpResponse response){
            super(message);
            this.status = status;
            this.response = response;
        }
    }

    /**
     * Provides all HTTP methods to use when creating a {@link HttpRequest}.
     * 提供创建 {@link HttpRequest} 时使用的所有 HTTP 方法。
     */
    public enum HttpMethod{
        GET, POST, PUT, DELETE, HEAD, CONNECT, OPTIONS, TRACE
    }

    /**
     * Defines the status of an HTTP request.
     * 定义 HTTP 请求的状态。
     */
    public enum HttpStatus{
        UNKNOWN_STATUS(-1),

        //1xx - informational
        // 1xx - 信息响应
        CONTINUE(100),
        SWITCHING_PROTOCOLS(101),
        PROCESSING(102),
        EARLY_HINTS(103),

        //2xx - success
        // 2xx - 成功
        OK(200),
        CREATED(201),
        ACCEPTED(202),
        NON_AUTHORITATIVE_INFORMATION(203),
        NO_CONTENT(204),
        RESET_CONTENT(205),
        PARTIAL_CONTENT(206),
        MULTI_STATUS(207),
        ALREADY_REPORTED(208),
        IM_USED(226),

        //3xx - redirects
        // 3xx - 重定向
        MULTIPLE_CHOICES(300),
        MOVED_PERMANENTLY(301),
        MOVED_TEMPORARILY(302),
        SEE_OTHER(303),
        NOT_MODIFIED(304),
        USE_PROXY(305),
        SWITCH_PROXY(306),
        TEMPORARY_REDIRECT(307),
        PERMANENT_REDIRECT(308),

        //4xx - client error
        // 4xx - 客户端错误
        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        PAYMENT_REQUIRED(402),
        FORBIDDEN(403),
        NOT_FOUND(404),
        METHOD_NOT_ALLOWED(405),
        NOT_ACCEPTABLE(406),
        PROXY_AUTHENTICATION_REQUIRED(407),
        REQUEST_TIMEOUT(408),
        CONFLICT(409),
        GONE(410),
        LENGTH_REQUIRED(411),
        PRECONDITION_FAILED(412),
        REQUEST_TOO_LONG(413),
        REQUEST_URI_TOO_LONG(414),
        UNSUPPORTED_MEDIA_TYPE(415),
        REQUESTED_RANGE_NOT_SATISFIABLE(416),
        EXPECTATION_FAILED(417),
        IM_A_TEAPOT(418),
        INSUFFICIENT_SPACE_ON_RESOURCE(419),
        METHOD_FAILURE(420),
        MISDIRECTED_REQUEST(421),
        UNPROCESSABLE_ENTITY(422),
        LOCKED(423),
        FAILED_DEPENDENCY(424),
        TOO_EARLY(425),
        UPGRADE_REQUIRED(426),
        PRECONDITION_REQUIRED(428),
        TOO_MANY_REQUESTS(429),
        REQUEST_HEADER_FIELDS_TOO_LARGE(431),
        UNAVAILABLE_FOR_LEGAL_REASONS(451),

        //5xx - server error
        // 5xx - 服务器错误
        INTERNAL_SERVER_ERROR(500),
        NOT_IMPLEMENTED(501),
        BAD_GATEWAY(502),
        SERVICE_UNAVAILABLE(503),
        GATEWAY_TIMEOUT(504),
        HTTP_VERSION_NOT_SUPPORTED(505),
        VARIANT_ALSO_NEGOTIATES(506),
        INSUFFICIENT_STORAGE(507),
        LOOP_DETECTED(508),
        NOT_EXTENDED(510),
        NETWORK_AUTHENTICATION_REQUIRED(511);

        private static IntMap<HttpStatus> byCode;

        public final int code;

        HttpStatus(int code){
            this.code = code;
        }

        /**
         * Find an HTTP status enum by code.
         * 按代码查找 HTTP 状态枚举。
         */
        public static synchronized HttpStatus byCode(int code){
            if(byCode == null){
                byCode = new IntMap<>();
                for(HttpStatus status : HttpStatus.values()){
                    byCode.put(status.code, status);
                }
            }
            return byCode.get(code, UNKNOWN_STATUS);
        }
    }
}

