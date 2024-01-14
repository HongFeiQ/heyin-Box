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

debug build
////////////////////////////////////////////////////////////////
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

def static buildTime() {
return new Date().format("yyyyMMdd_HHmm", TimeZone.getTimeZone("GMT+08:00"))
}

android {
compileSdk 33

    defaultConfig {
        applicationId 'com.github.tvbox.osc.hlhwan'
        minSdkVersion 21
        targetSdkVersion 31
        versionCode 1
        versionName "1.0.".concat(buildTime())
        multiDexEnabled true
        //设置room的Schema的位置
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
            }
        }
    }

    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
    }

    buildTypes {
        debug {
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            minifyEnabled false

            ndk {
                abiFilters 'armeabi-v7a'
            }
        }
        release {
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            minifyEnabled true

            ndk {
                abiFilters 'armeabi-v7a'
            }
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    lintOptions {
        checkReleaseBuilds false
        abortOnError false
    }
    dexOptions {
        javaMaxHeapSize "4g"
        additionalParameters += '--multi-dex'
        additionalParameters += '--set-max-idx-number=48000'
        additionalParameters += '--minimal-main-dex'
    }
    buildFeatures {
        dataBinding = true
    }

// splits {
// abi {
// enable true
// reset()
//
// // Specifies a list of ABIs that Gradle should create APKs for.
// include "x86", "x86_64", "armeabi-v7a", "arm64-v8a", "armeabi", "mips", "mips64"
// universalApk true //generate an additional APK that contains all the ABIs
// }
// }

}

///////////////////////////////////////////////////////////////////
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

static def getVersionName() {
def name = "2023.10.13"
return name
}

def getVersionCode() {// 获取版本号
def versionFile = file('version.properties')// 读取第一步新建的文件
if (versionFile.canRead()) {// 判断文件读取异常
Properties versionProps = new Properties()
versionProps.load(new FileInputStream(versionFile))
def versionCode = versionProps['VERSION_CODE'].toInteger()// 读取文件里面的版本号
def runTasks = gradle.startParameter.taskNames
if (':app:assembleRelease' in runTasks || ':app:packageRelease' in runTasks
|| ':app:assembleProfessionalRelease' in runTasks
|| ':app:assembleNormalRelease' in runTasks) {
// 版本号自增之后再写入文件（此处是关键，版本号自增+1）
versionProps['VERSION_CODE'] = (++versionCode).toString()
versionProps.store(versionFile.newWriter(), null)
}
return versionCode // 返回自增之后的版本号
}
// else {
// throw new GradleException("Could not find version.properties!")
// }
}

def appName = "HE影"
def versionCodeNew = getVersionCode()
def versionNameNew = getVersionName()
android {
compileSdkVersion rootProject.ext.compileSdkVersion

    defaultConfig {
        applicationId 'com.hlhwan.heyin.drpy2'
        minSdkVersion rootProject.ext.minSdkVersion
        targetSdkVersion rootProject.ext.targetSdkVersion
        versionCode versionCodeNew
        versionName versionNameNew
        multiDexEnabled true

        //设置room的Schema的位置
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
            }
        }

        ndk {
            //abiFilters 'arm64-v8a'
            abiFilters "armeabi-v7a"
        }

    }


    packagingOptions {
        exclude 'META-INF/androidx.localbroadcastmanager_localbroadcastmanager.version'
        exclude 'META-INF/androidx.customview_customview.version'
        exclude 'META-INF/androidx.legacy_legacy-support-core-ui.version'
        exclude 'META-INF/androidx.legacy_legacy-support-core-utils.version'
        exclude 'META-INF/androidx.slidingpanelayout_slidingpanelayout.version'
        exclude 'META-INF/androidx.*.version'
        exclude 'META-INF/proguard/androidx-annotations.pro'
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/rxjava.properties'
        exclude 'META-INF/beans.xml'
    }

    signingConfigs {

        release {
            storePassword "000624"
            keyAlias "bunny"
            keyPassword "426000"
            storeFile file('../tvbox.jks')
            v1SigningEnabled true
            v2SigningEnabled true
        }
    }

    buildTypes {

        debug {
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            minifyEnabled false

        }
        release {
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            minifyEnabled true   // 是否代码混淆
            multiDexEnabled true // 防止方法数量超过65536导致错误
            shrinkResources true // 删除无用资源
            zipAlignEnabled true

        }
    }

    applicationVariants.all { variant ->
        variant.outputs.all {
            def type = variant.buildType.name
            def fileName = appName + "_V" + versionNameNew + "_C" + versionCodeNew +
                    "_" + type + ".apk"
            outputFileName = fileName
        }
    }

    lintOptions {
        checkReleaseBuilds false
        abortOnError false
    }

    buildFeatures {
        dataBinding = true
    }
    namespace 'com.github.tvbox.osc'

}
////////////////////////////////////////////////////////////////////////////////////

更新日志：
33、
几个月来的BIG update&clean code
32、
1、修复多处bug
2、update混淆规则
3、update AndroidManifest.xml
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
5、optimize imports
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
