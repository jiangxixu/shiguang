# 拾光 · 个人知识收藏库

把散落在不同平台的收藏放到一起，按标题、标签和笔记找回它们。

**[打开网站](https://jiangxixu.github.io/shiguang/) · [下载 Android APK](https://github.com/jiangxixu/shiguang/releases/latest/download/shiguang-android.apk) · [版本发布](https://github.com/jiangxixu/shiguang/releases)**

## 网页版与 Android App

网站通过 GitHub Pages 发布，电脑和手机浏览器均可打开。Android 是可直接安装的独立 APK，内置网站资源，支持离线整理收藏，适用于 Android 8.0 及以上。安装包在 GitHub Releases 发布，未上架应用商店。

**当前没有账号登录和云端自动同步。** 网页、不同浏览器、Android App 的收藏各自保存在本设备，可通过 JSON 备份导出与导入迁移。公开仓库只包含源码与示例，真实收藏不会上传到 GitHub。浏览器清除站点数据、清除 App 数据或卸载 App 会删除本机收藏，操作前请导出备份。静态 GitHub Pages 不具备服务器账号数据库；如需自动同步，要另接后端。

## 主要功能

- 标题 / 标签 / 笔记 / 作者搜索，多个搜索词用空格分隔、同时匹配。
- 平台、标签、阅读状态和内容类型筛选；网格与列表视图。
- 特别关注、待阅读、标为已读、随机回看、笔记和标签编辑。
- JSON、CSV、HTML 浏览器书签导入；导入前检查勾选；URL 自动去重。
- Android 系统分享：在其他 App 选择分享链接到“拾光”，检查标题和笔记后保存。
- Android 文件选择器导入、系统文件窗口导出备份，不申请全部文件或相册访问权限。

首次进入显示醒目标注的示例库；点击“进入我的空白知识库”，或添加 / 导入真实内容即可退出示例。

## 如何带入收藏

电脑上打开网站的“导入与采集”，下载并解压采集扩展。在 Chrome 的 chrome://extensions 或 Edge 的 edge://extensions 开启开发者模式，选择“加载已解压的扩展程序”。

1. 在原平台登录，打开收藏 / 书签 / 稍后观看页面。
2. 滚动加载需要的条目，点击扩展“采集当前页面”。
3. 检查勾选链接，导出 JSON。
4. 回到拾光导入文件，重复链接会跳过，并保留已有笔记和状态。

扩展只有 activeTab 和 scripting 权限，只读取你主动点击时页面 DOM 中已加载的链接，不读取密码或 Cookie。动态 / 虚拟列表以及网页结构变化可能导致漏采，可分批采集或手动补充。未使用你的真实账号验证所有平台。浏览器采集扩展用于桌面 Chrome / Edge，Android App 可接收分享链接与导入备份文件。

## 来源与兼容性

提供小红书、B 站、知乎、LINUX DO、抖音、YouTube、GitHub、自定义“炉子”和其他网站的分类。平台的收藏夹、帖子书签、稍后观看、喜欢、Stars 可作为候选收集来源，由你自行选择并检查后导入；这些分类不代表已连接账号。

“炉子”的具体应用尚未确认，作为自定义来源；添加或导入时可手动指定。YouTube 稍后观看不支持常规播放列表列表 API 读取，见[官方说明](https://developers.google.com/youtube/v3/docs/playlists/list)。

支持带 items 数组的 JSON、HTML 书签和 CSV。CSV 表头使用 title,url,tags,notes 或 标题,链接,标签,笔记；也支持 YouTube Takeout 的 Video Id 表头。其他平台专有导出格式不保证兼容。搜索覆盖已保存的元数据和笔记，不抓取网页正文或下载视频。

## 开发与发布

本地运行：安装 Node.js，执行 npm start，打开 http://127.0.0.1:4178 。也可以双击 start.bat。无前端第三方依赖。

测试：npm test。导入、URL 去重、搜索、危险 URL 拒绝，以及 B 站跨子域 / 知乎专栏 / YouTube 页面链接采集均有自动测试。

推送 main 后，GitHub Actions 自动检查并发布 dist/ 到 GitHub Pages。Android 构建使用 JDK 17、Gradle 8.13、AGP 8.11.1 和 Android SDK 35，内置同一份 dist/。构建后验证 APK 签名，并发布 APK 与 SHA-256 校验值到 Releases。

Android 发布前需配置仓库 Actions Secrets：ANDROID_KEYSTORE_BASE64、ANDROID_KEYSTORE_PASSWORD。固定 PKCS12 签名密钥不进入源码；后续更新必须使用同一签名。发布新版本时同时增加 android/app/build.gradle 中的 versionCode 和 versionName；已存在的发布标签不会被自动覆盖。

可以在 Actions 页面手动执行网站或 Android 工作流。Android 成功构建不代表已经在你的实体手机上测试，安装后请先用少量收藏验证导入、分享与备份。
