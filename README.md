# 拾光 · 个人知识收藏库

把散落在不同平台的收藏放到一起，按标题、标签和笔记找回它们。

**[打开网站](https://jiangxixu.github.io/shiguang/) · [下载 Android APK](https://github.com/jiangxixu/shiguang/releases/latest/download/shiguang-android.apk) · [版本发布](https://github.com/jiangxixu/shiguang/releases)**

## 网页版与 Android App

网站通过 GitHub Pages 发布，电脑和手机浏览器均可打开。Android 是可直接安装的独立 APK，内置网站资源，支持离线整理收藏，适用于 Android 8.0 及以上。安装包在 GitHub Releases 发布，未上架应用商店。

**支持邮箱登录与 Supabase 跨设备自动同步。** 在网站、Android App 和电脑扩展中用同一个邮箱登录一次，以后手机“分享 → 拾光”、电脑按住空格再按 S 打开收藏窗口，保存后进入私有知识库，查看时再打开网站。无需每次填写表单或导入文件。离线时先保存本机：网站打开后同步、App 打开或再次分享时重试、扩展在浏览器运行时每分钟重试。未登录仍可本地使用。

登录邮件默认是一次性登录链接：网页可直接点邮件按钮；App / 扩展长按或右键邮件按钮复制链接，粘贴进登录框。各设备分别申请一封新邮件，链接只能使用一次。Supabase 默认邮件服务只向项目组织成员邮箱发送，个人使用请用该成员邮箱；开放给其他人需要配置自有 SMTP。真实收藏存放在 Supabase，行级权限只允许所属账号访问；公开 GitHub 仓库仅包含代码、公开客户端 key 和示例，不包含真实收藏、登录令牌或管理密钥。

清除设备数据或卸载前请确认同步完成，或导出备份。

## 主要功能

- 标题 / 标签 / 笔记 / 作者搜索，多个搜索词用空格分隔、同时匹配。
- 平台、标签、阅读状态和内容类型筛选；网格与列表视图。
- 特别关注、待阅读、标为已读、随机回看、笔记和标签编辑。
- JSON、CSV、HTML 浏览器书签导入；导入前检查勾选；URL 自动去重。
- Android 系统分享：在其他 App 选择分享链接到“拾光”，自动保存并返回原 App，无需打开网站或填写表单。
- Android 文件选择器导入、系统文件窗口导出备份，不申请全部文件或相册访问权限。

首次进入显示醒目标注的示例库；点击“进入我的空白知识库”，或添加 / 导入真实内容即可退出示例。

## 日常快捷收藏

电脑：安装 / 更新「拾光 · 一键收藏」扩展后，刷新原网页，在视频或文章页面**按住空格，再按 S**，或在网页右键选择「收藏到拾光…」。弹出的独立收藏窗口可选填「分类 / 标签」和「为什么收藏它」；留空也可以保存。多个分类用逗号分隔，在网站中以标签显示并可筛选、搜索。选中的网页文字会作为摘录保存。点击「保存收藏」或 Ctrl + Enter 后先保存到本机扩展，再自动同步；Esc 取消不会新增收藏。

空格 + S 仅在普通 HTTP / HTTPS 网页内生效，在输入框、文本编辑区中不触发。普通空格滚动保留。扩展安装或更新后，已经打开的来源网页需要刷新一次。备用快捷键 Alt + Shift + S 由浏览器扩展命令提供，也可在 Chrome 的 chrome://extensions/shortcuts 中调整。浏览器设置页、扩展商店等受保护页面不能采集。Chrome 原生分享菜单没有接入此扩展，使用快捷键或网页右键入口。

网站里的收藏封面和标题直接在新标签页打开原内容；「编辑」修改标题、分类和收藏原因；「详情」查看完整笔记、阅读状态和删除入口。列表视图同样支持这些操作。重复链接保留已有笔记和状态。扩展排队数据只有在网站成功写入后才移除。

Android：更新到 1.3.1，在原 App 中选择“分享 → 更多 → 拾光”。出现“已收进拾光”后返回原 App，之后打开拾光即可搜索。如果原 App 不提供分享链接，只分享图片或未包含 HTTP/HTTPS 链接，会提示未保存。可稍后在拾光补充标签和笔记。

**登录后跨设备同步。** 网站继续部署于 GitHub Pages，私有数据由 Supabase 保存。扩展右键 →「选项」中登录，App 和网站右上角「登录同步」中登录。

## 如何带入旧收藏

电脑上打开网站的“导入与采集”，下载并解压采集扩展。在 Chrome 的 chrome://extensions 或 Edge 的 edge://extensions 开启开发者模式，选择“加载已解压的扩展程序”。

1. 在扩展图标上点右键 →「选项」，打开旧收藏批量采集工具。然后在原平台登录，打开收藏 / 书签 / 稍后观看页面。
2. 滚动加载需要的条目，点击扩展“采集当前页面”。
3. 检查勾选链接，导出 JSON。
4. 回到拾光导入文件，重复链接会跳过，并保留已有笔记和状态。

扩展使用 activeTab、scripting、storage、alarms、contextMenus 权限，仅对 jiangxixu.github.io 与拾光 Supabase 项目请求常驻访问；快捷键脚本在普通 HTTP / HTTPS 网页监听空格 + S，不读取密码或输入框内容；知识库归档脚本只匹配 /shiguang/ 路径。扩展只在你主动触发收藏时读取来源网页标题、链接和选中文字，收藏先保存到扩展存储，网站打开后自动归档；批量模式读取页面 DOM 中已加载的链接，不读取密码或 Cookie。动态 / 虚拟列表以及网页结构变化可能导致漏采，可分批采集或手动补充。未使用你的真实账号验证所有平台。浏览器采集扩展用于桌面 Chrome / Edge，Android App 可接收分享链接与导入备份文件。

## 来源与兼容性

提供小红书、B 站、知乎、LINUX DO、抖音、YouTube、GitHub、自定义“炉子”和其他网站的分类。平台的收藏夹、帖子书签、稍后观看、喜欢、Stars 可作为候选收集来源，由你自行选择并检查后导入；这些分类不代表已连接账号。

“炉子”的具体应用尚未确认，作为自定义来源；添加或导入时可手动指定。YouTube 稍后观看不支持常规播放列表列表 API 读取，见[官方说明](https://developers.google.com/youtube/v3/docs/playlists/list)。

支持带 items 数组的 JSON、HTML 书签和 CSV。CSV 表头使用 title,url,tags,notes 或 标题,链接,标签,笔记；也支持 YouTube Takeout 的 Video Id 表头。其他平台专有导出格式不保证兼容。搜索覆盖已保存的元数据和笔记，不抓取网页正文或下载视频。

## 开发与发布

本地运行：安装 Node.js，执行 npm start，打开 http://127.0.0.1:4178 。也可以双击 start.bat。Supabase 官方客户端已打包到 dist/supabase.js，无运行时 CDN 依赖。修改依赖后运行 npm ci 和 node scripts/bundle-cloud.cjs。

测试：npm test。导入、URL 去重、搜索、危险 URL 拒绝，以及 B 站跨子域 / 知乎专栏 / YouTube 页面链接采集均有自动测试。

推送 main 后，GitHub Actions 自动检查并发布 dist/ 到 GitHub Pages。Android 构建使用 JDK 17、Gradle 8.13、AGP 8.11.1 和 Android SDK 35，内置同一份 dist/。构建后验证 APK 签名，并发布 APK 与 SHA-256 校验值到 Releases。

Android 发布前需配置仓库 Actions Secrets：ANDROID_KEYSTORE_BASE64、ANDROID_KEYSTORE_PASSWORD。固定 PKCS12 签名密钥不进入源码；后续更新必须使用同一签名。发布新版本时同时增加 android/app/build.gradle 中的 versionCode 和 versionName；已存在的发布标签不会被自动覆盖。

可以在 Actions 页面手动执行网站或 Android 工作流。Android 成功构建不代表已经在你的实体手机上测试，安装后请先用少量收藏验证导入、分享与备份。

## 云端架构

数据库结构见 supabase/migrations/，library_entries 使用 (user_id,url) 主键、RLS 和删除标记。客户端本地记录待同步操作，成功确认后才移除；同步过程中出现的新编辑保留在队列。跨设备同一条笔记同时编辑采用最后一次成功写入；建议避免多端同时编辑同一条。重新收藏重复链接保留已有笔记，删除后再次收藏可恢复。客户端使用公开 publishable key；不要在 dist 或扩展中放 service_role / secret key。

项目部署时同时更新网站、App、扩展中的 cloud-config.js。Android 分享页只允许应用资源与指定 Supabase API，按来源绑定的消息仅传回保存结果。测试覆盖离线重试、上传中编辑、删除标记、重复链接和登录链接校验；数据库已在回滚事务中验证跨账号读写隔离。

## 本机文件整理

本机相关文件统一位于 F:\Apps\shiguang：

- collector/：浏览器扩展，Chrome 加载此目录。
- dist/：网站和 Android 内置网页。
- android/：Android 源码。
- tools/：本机发布工具、下载包和操作截图，仅本机保留。
- signing/：原签名备份，保留原有访问限制，仅本机保留。
- archives/：旧版压缩包，仅本机保留。

tools/、signing/、archives/ 已加入 .gitignore，不会随代码上传 GitHub。
