# 文件近传

文件近传（CloseSend）是一个手机端局域网文件服务小工具。Android 版本是原生 Android/Java；iOS 版本位于 `ios/`，使用 SwiftUI 和 Network.framework 实现。它会在手机上启动一个局域网 HTTP 文件服务，电脑和手机连接同一个 Wi-Fi 后，电脑可以用浏览器打开手机端显示的地址，浏览目录、下载文件、上传文件。

## 主要功能

- 手机端显示可访问链接，例如 `http://手机IP:8080`
- 可自定义访问端口，默认 `8080`
- 可设置访问密码
- PC 浏览器端浏览手机文件目录
- 支持单文件下载
- 支持多选文件并批量下载为 zip
- 支持从 PC 上传文件到手机当前目录
- 上传、下载时显示进度条和操作结果
- iOS 普通文件上传使用流式写入，支持超过 100 MB 的大文件
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
- iOS 版本默认共享 App 的 Documents 目录，支持通过“文件”App/Finder 文件共享/浏览器上传导入文件
- iOS 版本支持读取系统相册照片/视频，并支持把电脑上传的照片/视频保存到系统相册
- iOS 版本可在 App 内播放上传到 Documents/Music 文件夹的 MP3/M4A/WAV/AAC/FLAC 等音频文件

## 使用方式

1. 安装并打开 App。
2. 按提示授予文件访问权限。
3. 按需设置访问密码和端口号。
4. 确保手机和电脑在同一个局域网。
5. 在电脑浏览器打开手机端显示的链接。
6. 登录后即可上传、下载或批量下载文件。

### iOS 使用说明

1. 用 Xcode 安装并打开 iOS App。
2. 点 App 内相册说明下方的“继续”，选择允许访问全部照片，或至少选择需要下载的照片/视频。
3. 确保 iPhone 和电脑在同一个局域网。
4. 在电脑浏览器打开 App 显示的地址。
5. 进入 `Photos Library` 下载 iPhone 相册里的照片和视频。
6. 在 `Photos Library` 页面点 `Upload to Photos`，可把电脑上的照片/视频保存到 iPhone 系统相册。
7. PDF、文档、普通文件请在 `Documents / PDF files` 或其它 Documents 子目录中上传和下载。
8. MP3 等音频文件可上传到 `Music` 文件夹，回到 iOS App 的“已上传音乐”区域点“刷新”后播放。

> iOS 不允许普通第三方 App 把任意本地 MP3 导入 Apple 自带“音乐”App 曲库。本 App 的音乐播放功能是在 CloseSend 内播放 `Documents/Music` 中的音频文件；如果需要进入系统“音乐”App 曲库，请通过 Finder/Music 同步或 Apple Music/iCloud 音乐资料库等系统支持的方式处理。

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

### Android

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

### iOS

iOS 工程位于：

```text
ios/PhoneFileBridge.xcodeproj
```

可用 Xcode 打开后选择 `PhoneFileBridge` Scheme 构建运行。命令行模拟器 Debug 构建示例：

最低系统版本为 iOS 15.0，可安装到 iPad mini 4 的 iOS 15.8.x。

```bash
xcodebuild -project ios/PhoneFileBridge.xcodeproj \
  -scheme PhoneFileBridge \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath ios/DerivedData \
  CODE_SIGNING_ALLOWED=NO \
  build
```

iOS 系统不允许普通 App 访问整台手机文件系统，因此 iOS 版本的文件区根目录是 App 沙盒内的 Documents 目录。`Info.plist` 已开启 `UIFileSharingEnabled` 和 `LSSupportsOpeningDocumentsInPlace`，便于从 Finder 或“文件”App 管理这些文件。系统相册的照片/视频通过 PhotoKit 单独授权访问。

## 版本更新历史

### 2026-06-10

- iOS 版本完成 App Store 审核流程，按审核反馈优化相册权限请求入口。
- iOS 本地版本将相册权限请求前的按钮文案由“允许访问相册”调整为中性的“继续”，避免在系统权限弹窗前引导用户授权。
- iOS 本地版本在相册权限已拒绝或受限时改为提供“打开系统设置”入口，不再重复触发授权请求。
- iOS 本地版本同步更新浏览器端无相册权限提示，避免继续引用旧的授权按钮文案。
- README 同步更新 iOS 使用说明；iOS 本地代码仍不推送到 GitHub。

### 2026-06-05

- 项目中文名更新为“文件近传”，App 名称更新为 `CloseSend`。
- Android 启动器名称、网页标题、APK 输出文件名和 Gradle 根项目名同步更新为 `CloseSend`。
- README 补充 iOS 版本使用说明、构建说明和 iOS 系统限制说明。
- iOS 本地版本新增系统相册浏览/下载/上传能力，并支持把电脑上传的照片和视频保存到系统相册。
- iOS 本地版本新增 Documents 沙盒文件区管理，支持 `Download`、`Documents`、`Photos`、`Camera`、`Movies`、`Music` 等快捷目录。
- iOS 本地版本普通文件上传改为流式写入，支持超过 100 MB 的大文件。
- iOS 本地版本新增 `Documents/Music` 音频列表和 App 内播放能力。
- iOS 本地版本优化网页文件列表：顶部固定 `Photos Library` / `Documents` 入口，当前区域和当前文件夹高亮；相册与沙盒文件列表均分栏显示文件大小、时间等信息。
- `.gitignore` 补充 Xcode DerivedData 和用户态工程状态文件忽略规则。

#### iOS 本地变更同步清单

以下 iOS 本地代码不推送到 GitHub；记录在此用于后续同步 Android 版本能力：

- 应用命名：iOS 主界面中文标题改为“文件近传”，导航标题、网页标题、Bundle 显示名和构建产物名改为 `CloseSend`。
- 顶部导航：网页端固定显示 `Photos Library` 和 `Documents` 两个入口；当前区域差异化高亮；沙盒快捷目录中当前选中的 `Download`、`Documents`、`Photos`、`Camera`、`Movies`、`Music` 会高亮。
- 沙盒文件列表：沙盒目录列表分栏显示 `文件名 / 大小 / 更新时间 / 操作`；文件夹大小列显示 `Folder`，普通文件显示格式化后的文件大小。
- 相册文件列表：系统相册列表分栏显示 `文件名 / 大小 / 类型与时间 / 操作`；文件大小从 PhotoKit 资源信息读取，无法读取时显示 `Size unknown`。
- 大文件上传：普通文件上传和上传到相册均改为流式写入，避免超过 100 MB 时因整包缓冲触发网络错误。
- 相册能力：新增 `Photos Library` 页面，支持列出系统照片/视频、下载单个资源、选择多个资源顺序下载，以及把电脑上传的照片/视频保存到系统相册。
- 沙盒能力：iOS 文件区根目录为 App Documents，启动时创建 `Download`、`Documents`、`Photos`、`Camera`、`Movies`、`Music` 快捷目录。
- 音乐能力：上传到 `Documents/Music` 的 MP3/M4A/WAV/AAC/FLAC 等音频可在 App 内刷新列表并播放、暂停、停止；不导入 Apple“音乐”App 曲库。
- 页面交互：上传、下载、打包下载、保存到相册均显示进度条和结果状态；相册多选下载按资源顺序逐个下载。
- iOS 限制说明：README 明确记录 iOS 不能让普通 App 访问整台文件系统，也不能把任意本地 MP3 导入 Apple“音乐”App 曲库。

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
