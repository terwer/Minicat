# Minicat

简易版的Tomcat，主要实现了静态资源和动态Servlet的功能

# 回顾Minicat1.0到3.0

## Minicat 1.0版本
需求：浏览器请求http://localhost:8080,返回一个固定的字符串到页面"Hello Minicat!"

## Minicat 2.0版本
需求：封装Request和Response对象，返回html静态资源文件

## Minicat 3.0版本
需求：可以请求动态资源（Servlet）

# Minicat V4.0要求

1、Minicat V4.0需要在已有Minicat基础上进一步扩展，模拟出webapps部署效果 

2、磁盘上放置一个webapps目录，webapps中可以有多个项目，例如demo1、demo2、demo3... 

3、每个项目中含有servlet，可以根据请求url定位对应servlet进一步处理。

# 4.0实现效果

wenapps目录 测试url

http://localhost:8080/lagou/demo1

http://localhost:8080/lagou/demo2

http://localhost:8080/lagou/demo3