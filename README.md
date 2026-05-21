# 手机文件桥

手机文件桥是一个原生 Android 小工具。它会在手机上启动一个局域网 HTTP 文件服务，电脑和手机连接同一个 Wi-Fi 后，电脑可以用浏览器打开手机端显示的地址，浏览手机公共目录、下载文件、上传文件。

## 主要功能

- 手机端显示可访问链接，例如 `http://手机IP:8080`
- 可自定义访问端口，默认 `8080`
- 可设置访问密码
- PC 浏览器端浏览手机文件目录
- 支持单文件下载
- 支持多选文件并批量下载为 zip
- 支持从 PC 上传文件到手机当前目录
- 上传、下载时显示进度条和操作结果
- 顶部显示目录树，方便确认当前位置
- 提供常用公共目录快捷入口：
  - Download
  - Documents
  - Photos
  - Camera
  - Movies
  - Music
- 默认打开公共 `Download` 目录，便于其它应用读写和查看
- 兼容较老 Android 版本，最低支持 Android 4.4

## 使用方式

1. 安装并打开 App。
2. 按提示授予文件访问权限。
3. 按需设置访问密码和端口号。
4. 确保手机和电脑在同一个局域网。
5. 在电脑浏览器打开手机端显示的链接。
6. 登录后即可上传、下载或批量下载文件。

## 权限说明

应用需要以下权限来提供局域网文件传输能力：

- 网络权限：用于启动手机端 HTTP 服务。
- Wi-Fi/网络状态权限：用于辅助获取局域网地址。
- 存储读取/写入权限：用于读取和写入公共文件目录。
- Android 11 及以上可能需要“管理所有文件”权限，否则系统会限制可访问目录。

## 默认目录

默认打开目录为手机公共 Download 目录，通常是：

```text
/storage/emulated/0/Download
```

网页顶部提供常用公共目录快捷按钮，可以跳转到照片、相机、文档、音乐、视频等目录。

## 构建

本项目是原生 Android/Java 工程。调试包构建命令：

```powershell
$env:JAVA_HOME='D:\android-studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat assembleDebug
```

构建成功后 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 作者信息

- 作者：Pyrrhus
- 邮箱：zhangxuefeng@batonsoft.com

## 推送到 GitHub

首次推送示例，远端仓库地址请替换成你自己的：

```powershell
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

如果你已经创建过远端，只需要检查远端地址后手动执行最后的推送命令即可：

```powershell
git remote -v
git push -u origin main
```

## 注意事项

- 文件服务只在 App 运行期间开启。
- 建议只在可信局域网内使用。
- 建议设置访问密码。
- 若端口被占用，可在手机端修改端口后保存并重启服务。
