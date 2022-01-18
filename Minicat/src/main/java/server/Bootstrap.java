package server;

import classloader.MyClassLoader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Minicat的主类
 */
public class Bootstrap {

    /**
     * 定义socket监听的端口号
     */
    private int port = 8080;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    /**
     * Minicat启动需要初始化展开的一些操作
     */
    public void start() throws Exception {

        // 加载解析相关的配置，web.xml
        loadServlet();

        // 解析server.xml，读取webapps路径
        // 遍历webapps，每个应用通过单独设置类加载器（防止默认的类加载机制导致不同应用的相同class问题）
        // 读取web.xml的servlet配置，生成servlet保存到servletMap
        // 保存的key需要加上应用的前缀
        loadWebapps();

        System.out.println("全部加载完成，servletMap:"+servletMap);

        // 定义一个线程池
        int corePoolSize = 10;
        int maximumPoolSize = 50;
        long keepAliveTime = 100L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(50);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();


        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );


        /*
            完成Minicat 1.0版本
            需求：浏览器请求http://localhost:8080,返回一个固定的字符串到页面"Hello Minicat!"
         */
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("=====>>>Minicat start on port：" + port);

        /*while(true) {
            Socket socket = serverSocket.accept();
            // 有了socket，接收到请求，获取输出流
            OutputStream outputStream = socket.getOutputStream();
            String data = "Hello Minicat!";
            String responseText = HttpProtocolUtil.getHttpHeader200(data.getBytes().length) + data;
            outputStream.write(responseText.getBytes());
            socket.close();
        }*/


        /**
         * 完成Minicat 2.0版本
         * 需求：封装Request和Response对象，返回html静态资源文件
         */
        /*while(true) {
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();

            // 封装Request对象和Response对象
            Request request = new Request(inputStream);
            Response response = new Response(socket.getOutputStream());

            response.outputHtml(request.getUrl());
            socket.close();

        }*/


        /**
         * 完成Minicat 3.0版本
         * 需求：可以请求动态资源（Servlet）
         */
        /*while(true) {
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();

            // 封装Request对象和Response对象
            Request request = new Request(inputStream);
            Response response = new Response(socket.getOutputStream());

            // 静态资源处理
            if(servletMap.get(request.getUrl()) == null) {
                response.outputHtml(request.getUrl());
            }else{
                // 动态资源servlet请求
                HttpServlet httpServlet = servletMap.get(request.getUrl());
                httpServlet.service(request,response);
            }

            socket.close();

        }
*/

        /*
            多线程改造（不使用线程池）
         */
        /*while(true) {
            Socket socket = serverSocket.accept();
            RequestProcessor requestProcessor = new RequestProcessor(socket,servletMap);
            requestProcessor.start();
        }*/
        // System.out.println("=========>>>>>>使用线程池进行多线程改造");
        /*
            多线程改造（使用线程池）
         */
        /*
        while(true) {

            Socket socket = serverSocket.accept();
            RequestProcessor requestProcessor = new RequestProcessor(socket,servletMap);
            //requestProcessor.start();
            threadPoolExecutor.execute(requestProcessor);
        }
        */

        /**
         * 完成Minicat 4.0版本
         * 需求：实现webapps部署，并支持多项目部署
         */
        while (true) {
            Socket socket = serverSocket.accept();
            RequestProcessor requestProcessor = new RequestProcessor(socket, servletMap);
            threadPoolExecutor.execute(requestProcessor);
        }
    }

    /**
     * 加载解析server.xml，初始化webapps里面的servlet
     */
    private void loadWebapps() {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("server.xml");
        SAXReader saxReader = new SAXReader();

        try {
            Document document = saxReader.read(resourceAsStream);
            Element element = document.getRootElement();

            // 为了简单，暂不考虑多个host的情况，以单个host为例
            List<Element> hosts = element.selectNodes("//Host");
            // 未找到主机，不处理
            if (null == hosts || hosts.size() == 0) {
                return;
            }

            Element host = hosts.get(0);
            String appBase = host.attributeValue("appBase");

            // 找到appBase
            String classPath = this.getClass().getResource(".").getFile();
            String absoluteAppBase = Paths.get(classPath).getParent().getParent().getParent().toAbsolutePath().toString() +"/"+ appBase;
            File file = new File(absoluteAppBase);
            if (!file.exists()) {
                System.out.println("webapps目录不存在，将不会加载任何应用");
                return;
            }
            System.out.println("准备从下面的目录加载webapps:" + absoluteAppBase+"\n");

            File[] dirs = file.listFiles();

            for (File dir : dirs) {
                if (!dir.isDirectory()) {
                    continue;
                }

                // 遍历每个应用
                String appPath = "/" + dir.getName();

                List<Element> contextList = host.elements();
                for (Element context : contextList) {
                    String docBase = context.attributeValue("docBase");
                    if (!appPath.equals(docBase)) {
                        continue;
                    }

                    String appPrefix = context.attributeValue("path");
                    String absAppPath = absoluteAppBase + docBase;

                    System.out.println("开始加载应用:" + appPrefix + ",应用路径:" + absAppPath);
                    // 加载各个应用的servlet
                    loadWebappsServlet(appPrefix, absAppPath);
                }
            }
            System.out.println("加载webapps的servlet完成");
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    private Map<String, HttpServlet> servletMap = new HashMap<String, HttpServlet>();

    /**
     * 加载解析web.xml，初始化Servlet
     */
    private void loadServlet() {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("web.xml");
        SAXReader saxReader = new SAXReader();

        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();

            List<Element> selectNodes = rootElement.selectNodes("//servlet");
            for (int i = 0; i < selectNodes.size(); i++) {
                Element element = selectNodes.get(i);
                // <servlet-name>lagou</servlet-name>
                Element servletnameElement = (Element) element.selectSingleNode("servlet-name");
                String servletName = servletnameElement.getStringValue();
                // <servlet-class>server.LagouServlet</servlet-class>
                Element servletclassElement = (Element) element.selectSingleNode("servlet-class");
                String servletClass = servletclassElement.getStringValue();


                // 根据servlet-name的值找到url-pattern
                Element servletMapping = (Element) rootElement.selectSingleNode("/web-app/servlet-mapping[servlet-name='" + servletName + "']");
                // /lagou
                String urlPattern = servletMapping.selectSingleNode("url-pattern").getStringValue();
                servletMap.put(urlPattern, (HttpServlet) Class.forName(servletClass).newInstance());

            }

            System.out.println("加载servlet完成");
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void loadWebappsServlet(String appPrefix, String absAppPath) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = new FileInputStream(absAppPath + "/web.xml");

            SAXReader saxReader = new SAXReader();

            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();

            List<Element> selectNodes = rootElement.selectNodes("//servlet");
            for (int i = 0; i < selectNodes.size(); i++) {
                Element element = selectNodes.get(i);
                // <servlet-name>lagou</servlet-name>
                Element servletnameElement = (Element) element.selectSingleNode("servlet-name");
                String servletName = servletnameElement.getStringValue();
                // <servlet-class>server.LagouServlet</servlet-class>
                Element servletclassElement = (Element) element.selectSingleNode("servlet-class");
                String servletClass = servletclassElement.getStringValue();


                // 根据servlet-name的值找到url-pattern
                Element servletMapping = (Element) rootElement.selectSingleNode("/web-app/servlet-mapping[servlet-name='" + servletName + "']");
                // /lagou
                String urlPattern = servletMapping.selectSingleNode("url-pattern").getStringValue();

                // 使用自定义的类加载器加载
                MyClassLoader loader = new MyClassLoader(absAppPath);
                Class<?> aClass = loader.loadClass(servletClass);

                // 注意：下面的会从默认的类加载器加载
                //  Class<?> aClass = Class.forName(servletClass);

                servletMap.put(appPrefix + urlPattern, (HttpServlet) aClass.newInstance());

            }

            System.out.println("加载应用" + appPrefix + "的servlet完成\n");
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Minicat 的程序启动入口
     *
     * @param args
     */
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        try {
            // 启动Minicat
            bootstrap.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
