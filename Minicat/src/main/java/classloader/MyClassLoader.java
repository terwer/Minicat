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

    public MyClassLoader(String absAppPath) {
        this.absAppPath = absAppPath;
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
        String classFullPath = "file://" + absAppPath + "/" + name.replaceAll("\\.","/") + ".class";
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
