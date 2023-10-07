# Box

=== Source Code - Editing the app default settings ===
/src/main/java/com/github/tvbox/osc/base/App.java

    private void initParams() {

        putDefault(HawkConfig.HOME_REC, 2);       // Home Rec 0=豆瓣, 1=推荐, 2=历史
        putDefault(HawkConfig.PLAY_TYPE, 1);      // Player   0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码");// IJK Render 软解码, 硬解码
        putDefault(HawkConfig.HOME_SHOW_SOURCE, true);  // true=Show, false=Not show
        putDefault(HawkConfig.HOME_NUM, 2);       // History Number
        putDefault(HawkConfig.DOH_URL, 2);        // DNS
        putDefault(HawkConfig.SEARCH_VIEW, 2);    // Text or Picture

    }

更新日志：
31、
1、感谢爱佬提供新quickjs引擎
2、感谢watson提供建议
3、适配升级到爱佬的新quickjs引擎
4、修复已知bug
5、优化清理code
30、修复由于xstream升级后导致xml源解析失败的问题，报错：ForbiddenClassException
29、media3 update online
28、clean code
27、
1、update code
2、Delete app/schemas/com.github.tvbox.osc.data.AppDataBase directory
3、Delete app/release directory
26、
fix 多仓不能remote push api_url已成功
25、
1、App启动时清理荐片下载缓存2、fix 多仓不能remote push api_url(未成功）
24、
App启动时清理荐片下载缓存
23、
update about_dialog add websocket动态实时日志使用说明
22、
websocket动态实时日志使用说明：IP:PORT/log
21、
增加websocket动态实时日志调试成功截图
20、
fix websocket动态实时日志
19、
remove unused resources
18、
fix render problem bug
17、
1、美化图标
2、禁止首页显示推荐
16、
1、update to HE影
2、增加混淆dictoO0.txt
3、配置构建缓存org.gradle.caching = true
4、并行构建org.gradle.parallel = true
15、
Update VOD focus size x1.2
Update VOD controller UI
Fix crash during search subtitle on enter
14、
1、update dialog_about
2、update readme
3、remove unused resoucres
4、reformat code
5、optimize  imports
6、fix bug

13、
增加截图
12、
1、支持多仓
2、支持自动循环播放（最后一集跳到第一集）
11、
支持websocket实时动态日志调试
10、
升级 exoplayer from 2.19.1 to media3 1.1.1
9、
update EXO from 2.19.0 to 2.19.1
8、
支持荐片
7、
update build.gradle code
6、
clean code
5、
增加支持py源代码
4、
update README.md
3、
居于https://github.com/takagen99/Box8月28日版本创建

2、
Initial commit
1、
第一次建仓