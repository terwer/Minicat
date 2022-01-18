package classloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 自定义类加载器，主要用于应用的servlet加载
 *
 * @name: MyClassLoader
 * @author: terwer
 * @date: 2022-01-18 10:19
 **/
public class MyClassLoader extends ClassLoader {

    private String absAppPath;
    private String servletClass;

    public MyClassLoader(String absAppPath, String servletClass) {
        this.absAppPath = absAppPath;
        this.servletClass = servletClass;
    }

    /**
     * 通过指定全路径记载class文件
     *
     * @param name class文件路径（包名+class文件名）
     * @return Class<?>
     * @throws ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // 这里要注意：如果不是servlet，需要使用双亲委派
        // 因为应用里面会使用Minicat里面定义的类
        // 而这些类没必要拷贝一份到应用里面
        // 只有servlet才去跳过双亲委派
        if (!name.equals(this.servletClass)) {
            System.out.println("非servlet，不使用自定义类加载器:" + name);
            Class<?> clazz = this.getClass().getClassLoader().loadClass(name);
            return clazz;
        }

        String filePath = absAppPath + "/" + name.replaceAll("\\.", "/") + ".class";
        String classFullPath = "file://" + filePath;
        System.out.println("MyClassLoader开始加载:" + classFullPath);

        byte[] classBytes = null;
        Path path = null;

        try {
            path = Paths.get(new URI(classFullPath));
            classBytes = Files.readAllBytes(path);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        Class<?> clazz = defineClass(name, classBytes, 0, classBytes.length);

        return clazz;
    }
}
