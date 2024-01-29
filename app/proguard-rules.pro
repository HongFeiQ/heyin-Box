#############################################
#
# 对于一些基本指令的添加
#
#############################################
-optimizationpasses 5                                       #指定代码压缩级别
-dontusemixedcaseclassnames                                 #混淆时不会产生形形色色的类名
-dontskipnonpubliclibraryclasses                            #指定不忽略非公共类库
-dontpreverify                                              #不预校验，(作用于Java平台，Android不需要，去掉可加快混淆)
-ignorewarnings                                             #屏蔽警告
-verbose                                                    #混淆时记录日志

-printconfiguration ./build/outputs/mapping/full-config.txt   # 输出所有规则叠加后的混淆规则
-printmapping proguardMapping.txt                          #生成原类名与混淆后类名的映射文件mapping.txt

# -dontoptimize                                             #不优化输入的类文件 关闭代码优化
# -dontshrink                                               #关闭压缩
# -dontobfuscate                                            #关闭混淆

#-dontusemixedcaseclassnames                                #混淆后类型都为小写
# -dontwarn com.squareup.okhttp.**                          #指定类不输出警告信息
# -dontskipnonpubliclibraryclasses                          #不跳过非公共的库的类
# -useuniqueclassmembernames                                #把混淆类中的方法名也混淆
# -allowaccessmodification                                  #优化时允许访问并修改有修饰符的类及类的成员
# -renamesourcefileattribute SourceFile                     #将源码中有意义的类名转换成SourceFile，用于混淆具体崩溃代码
# -keepattributes SourceFile,LineNumberTable                #保留行号
# -keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod #避免混淆注解、内部类、泛型、匿名类
# -optimizations !code/simplification/cast,!field/ ,!class/merging/  #指定混淆时采用的算法


#####################################################################################################
# 语法组成：
# [保持命令] [类] {
# [成员]
# }
# 保持命令：
#-keep                                                    #防止类和类成员被移除或被混淆；
#-keepnames                                               #防止类和类成员被混淆；
#-keepclassmembers                                        #防止类成员被移除或被混淆；
#-keepclassmembernames                                    #防止类成员被混淆；
#-keepclasseswithmembers                                  #防止拥有该成员的类和类成员被移除或被混淆；
#-keepclasseswithmembernames                             #防止拥有该成员的类和类成员被混淆；
# 类：
# 具体的类
# 访问修饰符 → public、private、protected
#通配符(*) → 匹配任意长度字符，但不包含包名分隔符(.)
#通配符(**) → 匹配任意长度字符，且包含包名分隔符(.)
#extends → 匹配实现了某个父类的子类
#implements → 匹配实现了某接口的类
#$ → 内部类
#成员：
#匹配所有构造器 → <init>
#匹配所有域 → <field>
#匹配所有方法 → <methods>
#访问修饰符 → public、private、protected
#除了 * 和 ** 通配符外，还支持 *** 通配符，匹配任意参数类型
#... → 匹配任意长度的任意类型参数，如void test(...)可以匹配不同参数个数的test方法

####################################################################################################
####################################################################################################
#常用自定义混淆规则范例：
#不混淆某个类的类名，及类中的内容
#-keep class cn.coderpig.myapp.example.Test { *;}
#不混淆指定包名下的类名，不包括子包下的类名
#-keep class cn.coderpig.myapp*
#不混淆指定包名下的类名，及类里的内容
#-keep class cn.coderpig.myapp* {*;}
# 不混淆指定包名下的类名，包括子包下的类名
#-keep class cn.coderpig.myapp**
# 不混淆某个类的子类
#-keep public class * extends cn.coderpig.myapp.base.BaseFragment
# 不混淆实现了某个接口的类
#-keep class * implements cn.coderpig.myapp.dao.DaoImp
# 不混淆类名中包含了"entity"的类，及类中内容
#-keep class **.*entity*.**  {*;}
# 不混淆内部类中的所有public内容
#-keep class cn.coderpig.myapp.widget.CustomView$OnClickInterface {
#           public *;
#  }
# 不混淆指定类的所有方法
#-keep cn.coderpig.myapp.example.Test {
#          public <methods>;
#   }
# 不混淆指定类的所有字段
#-keep cn.coderpig.myapp.example.Test {
#          public <fields>;
#   }
# 不混淆指定类的所有构造方法
#-keep cn.coderpig.myapp.example.Test {
#         public <init>;
#  }
# 不混淆指定参数作为形参的方法
#-keep cn.coderpig.myapp.example.Test {
#         public <methods>(java.lang.String);
#  }
#不混淆类的特定方法
#-keep cn.coderpig.myapp.example.Test {
#       public test(java.lang.String);
#  }
#不混淆native方法
#-keepclasseswithmembernames class * {
#             native <methods>;
#     }
#不混淆枚举类
#-keepclassmembersenum * {
#public static **[] values();
#    public static ** valueOf(java.lang.String);
#     }
#不混淆资源类
#-keepclassmembersclass **.R$ * {
#              public static <fields>;
#     }
#不混淆自定义控件
#-keep public class * entends android.view.View {
#              *** get*();
#              void set*(***);
#              public <init>;
#     }
#不混淆实现了Serializable接口的类成员，此处只是演示，也可以直接*;
#-keepclassmembers class * implements java.io.Serializable {
#              static final long serialVersionUID;
#              private static final java.io.ObjectStreamField[]serialPersistentFields;
#              private void writeObject(java.io.ObjectOutputStream);
#              private void readObject(java.io.ObjectInputStream);
#              java.lang.ObjectwriteReplace();
#              java.lang.ObjectreadResolve();
#     }
# 不混淆实现了parcelable接口的类成员
#-keep class * implements android.os.Parcelable {
#              public static final android.os.Parcelable$Creator *;
#     }
#                     注意事项：
#
# ①jni方法不可混淆，方法名需与native方法保持一致；
# ②反射用到的类不混淆，否则反射可能出问题；
# ③四大组件、Application子类、Framework层下的类、自定义的View默认不会被混淆，无需另外配置；
# ④WebView的JS调用接口方法不可混淆；
# ⑤注解相关的类不混淆；
# ⑥GSON、Fastjson等解析的Bean数据类不可混淆；
# ⑦枚举enum类中的values和valuesof这两个方法不可混淆(反射调用)；
# ⑧继承Parceable和Serializable等可序列化的类不可混淆；
# ⑨第三方库或SDK，请参考第三方提供的混淆规则，没提供的话，建议第三方包全部不混淆；

#####################################################################################################

-optimizations !code/simplification/cast/arithmetic,!field/*,!class/merging/*    #优化  #指定混淆时采用的算法
-keepattributes *Annotation*                                         #避免混淆注解、内部类、泛型、匿名类
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable                          # 保留代码行号，方便异常信息的追踪
-keepattributes Exceptions,InnerClasses,Deprecated,LocalVariable*Table,Synthetic,EnclosingMethod
-keepattributes EnclosingMethod
-renamesourcefileattribute SourceFile

#混淆字典
-obfuscationdictionary dictoO0.txt #自定义混淆字符
-classobfuscationdictionary dictoO0.txt
-packageobfuscationdictionary dictoO0.txt

# 重新包装所有重命名的包并放在给定的单一包中
#-flattenpackagehierarchy androidx.base

# 将包里的类混淆成n个再重新打包到一个统一的package中  会覆盖flattenpackagehierarchy选项
-repackageclasses  androidx.base
#   - 在文件中找到包含 -repackageclasses 和 -flattenpackagehierarchy 选项的行。
#   - 确保只使用其中一个选项。您可以注释掉或删除其中一个选项。

# 把混淆类中的方法名也混淆了
#-useuniqueclassmembernames
#实际上，"-useuniqueclassmembernames"不是ProGuard的选项，因此在配置文件中使用它会导致语法错误。
#如果您在ProGuard配置文件中使用了"-useuniqueclassmembernames"选项并遇到了报错，请将其从配置文件中删除。这样可以解决该问题。
#如果您需要对类成员进行唯一命名，ProGuard会在混淆过程中自动处理，无需手动指定选项。

#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################

# 保留我们使用的四大组件，自定义的Application等等这些类不被混淆
# 因为这些子类都有可能被外部调用
#noinspection ShrinkerUnresolvedReference
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# 保留support下的所有类及其内部类
-keep class android.support.** {*;}
# 保留继承的
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
#-keep public class * extends androidx.**

-keep class org.xmlpull.v1.** {*;}

# 保留R下面的资源
-keep class **.R$* {*;}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}
# Keep native methods
-keepclassmembers class * {
    native <methods>;
}
# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    #noinspection ShrinkerUnresolvedReference
    public void *(android.view.View);
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    #noinspection ShrinkerUnresolvedReference
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep public class * extends androidx.recyclerview.widget.RecyclerView$LayoutManager{
    *** get*();
    void set*(***);
    #noinspection ShrinkerUnresolvedReference
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class com.orhanobut.hawk.** { *; }

# 保留Parcelable序列化类不被混淆

-keep class * implements android.os.Parcelable {
#noinspection ShrinkerUnresolvedReference
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


#xwalk
-keep class org.xwalk.core.** { *; }
-keep class org.crosswalk.engine.** { *; }
-keep class org.chromium.** { *; }
-dontwarn android.view.**
-dontwarn android.media.**
-dontwarn org.chromium.**
#okhttp
-dontwarn okhttp3.**
-keep class okhttp3.**{*;}
#okio
-dontwarn okio.**
-keep class okio.**{*;}
#loadsir
-dontwarn com.kingja.loadsir.**
-keep class com.kingja.loadsir.** {*;}
#gson
# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }
# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }
# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
#noinspection ShrinkerUnresolvedReference
  @com.google.gson.annotations.SerializedName <fields>;
}
#xstream
-keep class com.thoughtworks.xstream.converters.extended.SubjectConverter { *; }
-keep class com.thoughtworks.xstream.converters.extended.ThrowableConverter { *; }
-keep class com.thoughtworks.xstream.converters.extended.StackTraceElementConverter { *; }
-keep class com.thoughtworks.xstream.converters.extended.CurrencyConverter { *; }
-keep class com.thoughtworks.xstream.converters.extended.RegexPatternConverter { *; }
-keep class com.thoughtworks.xstream.converters.extended.CharsetConverter { *; }
-keep class com.thoughtworks.xstream.** { *; }
#eventbus
-keepclassmembers class * {
#noinspection ShrinkerUnresolvedReference
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
#bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}
-keep class android.support.**{*;}

#dkplayer
-keep class com.dueeeke.videoplayer.** { *; }
-dontwarn com.dueeeke.videoplayer.**

# IjkPlayer
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**
-keep class xyz.doikki.** { *; }
-dontwarn xyz.doikki.**

# ExoPlayer
-keep class org.xmlpull.v1.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.exoplayer.**
#
-keep class androidx.media3.datasource.** { *; }
-dontwarn androidx.media3.datasource.**
-keep class androidx.media3.database.** { *; }
-dontwarn androidx.media3.database.**


-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep public class * implements java.io.Serializable {
    public *;
}

-keepclassmembers class * {
   public <init>(org.json.JSONObject);
}

# 对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
}

-keepclassmembers class * extends android.webkit.webViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.webViewClient {
    #noinspection ShrinkerUnresolvedReference
    public void *(android.webkit.webView, jav.lang.String);
}

# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# sardine webdav
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**

# filepicker
-keep class com.obsez.android.lib.filechooser.** { *; }
-dontwarn com.obsez.android.lib.filechooser.**

# jcifs (smb)
-keep class jcifs.** { *; }
-dontwarn jcifs.**

# 实体类
#-keep class com.github.tvbox.osc.bean.** { *; }
-keep class com.github.tvbox.osc.ui.fragment.homes.**{*;}
#CardView
-keep class com.github.tvbox.osc.ui.tv.widget.card.**{*;}
#ViewObj
-keep class com.github.tvbox.osc.ui.tv.widget.ViewObj{
    <methods>;
}

-keep class com.github.catvod.crawler.*{*;}

# magnet：解决模拟器推送 磁力链接 闪退
-keep class com.xunlei.downloadlib.** {*;}

# quickjs引擎
-keep class com.whl.quickjs.** {*;}

# 支持影视的ali相关的jar
-keep class com.google.gson.**{*;}
# 某些类会反射调用zxing导致生成阿里云二维码报错
-keep class com.google.zxing.** {*;}

# jsoup
-keep class org.jsoup.** {*;}




-keep public class com.undcover.freedom.pyramid.** { *; }
-dontwarn com.undcover.freedom.pyramid.**
-keep public class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# OKHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-dontwarn okhttp3.internal.platform.ConscryptPlatform     # OkHttp platform used only on JVM and when Conscrypt dependency is available.

# okhttp3
# The proguard configuration file for the following section is /home/cl/.gradle/caches/transforms-3/af3ecb4c3ae4accf6423845d738f047d/transformed/rules/lib/META-INF/proguard/okhttp3.pro
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*



# End of content from /home/cl/.gradle/caches/transforms-3/af3ecb4c3ae4accf6423845d738f047d/transformed/rules/lib/META-INF/proguard/okhttp3.pro

#一些其他可能用到的第三方规则

# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# appcompat库不做混淆
-keep class androidx.appcompat.**
#保留 AndroidManifest.xml 文件：防止删除 AndroidManifest.xml 文件中定义的组件
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

#保留序列化，例如 Serializable 接口
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#带有Context、View、AttributeSet类型参数的初始化方法
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}


#避免回调函数 onXXEvent 混淆，对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
    void *(**on*Changed);
}

#业务实体不做混淆，避免gson解析错误
-dontwarn com.grandstream.convergentconference.entity.**
-keep class com.grandstream.convergentconference.entity.** { *;}

#Rxjava、RxAndroid，官方ReadMe文档中说明无需特殊配置
-dontwarn java.util.concurrent.Flow*
#okhttp3、okio、retrofit，jar包中已包含相关proguard规则，无需配置
#其他一些配置

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.

-dontwarn com.ctc.wstx.stax.WstxInputFactory
-dontwarn com.ctc.wstx.stax.WstxOutputFactory
-dontwarn java.awt.Color
-dontwarn java.awt.Font
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.beans.PropertyEditor
-dontwarn javax.activation.ActivationDataFlavor
-dontwarn javax.swing.plaf.FontUIResource
-dontwarn javax.xml.bind.DatatypeConverter
-dontwarn net.sf.cglib.proxy.Callback
-dontwarn net.sf.cglib.proxy.CallbackFilter
-dontwarn net.sf.cglib.proxy.Enhancer
-dontwarn net.sf.cglib.proxy.Factory
-dontwarn net.sf.cglib.proxy.NoOp
-dontwarn net.sf.cglib.proxy.Proxy
-dontwarn nu.xom.Attribute
-dontwarn nu.xom.Builder
-dontwarn nu.xom.Document
-dontwarn nu.xom.Element
-dontwarn nu.xom.Elements
-dontwarn nu.xom.Node
-dontwarn nu.xom.ParentNode
-dontwarn nu.xom.ParsingException
-dontwarn nu.xom.Text
-dontwarn nu.xom.ValidityException
-dontwarn org.codehaus.jettison.AbstractXMLStreamWriter
-dontwarn org.codehaus.jettison.mapped.Configuration
-dontwarn org.codehaus.jettison.mapped.MappedNamespaceConvention
-dontwarn org.codehaus.jettison.mapped.MappedXMLInputFactory
-dontwarn org.codehaus.jettison.mapped.MappedXMLOutputFactory
-dontwarn org.dom4j.Attribute
-dontwarn org.dom4j.Branch
-dontwarn org.dom4j.Document
-dontwarn org.dom4j.DocumentException
-dontwarn org.dom4j.DocumentFactory
-dontwarn org.dom4j.Element
-dontwarn org.dom4j.io.OutputFormat
-dontwarn org.dom4j.io.SAXReader
-dontwarn org.dom4j.io.XMLWriter
-dontwarn org.dom4j.tree.DefaultElement
-dontwarn org.jdom.Attribute
-dontwarn org.jdom.Content
-dontwarn org.jdom.DefaultJDOMFactory
-dontwarn org.jdom.Document
-dontwarn org.jdom.Element
-dontwarn org.jdom.JDOMException
-dontwarn org.jdom.JDOMFactory
-dontwarn org.jdom.Text
-dontwarn org.jdom.input.SAXBuilder
-dontwarn org.jdom2.Attribute
-dontwarn org.jdom2.Content
-dontwarn org.jdom2.DefaultJDOMFactory
-dontwarn org.jdom2.Document
-dontwarn org.jdom2.Element
-dontwarn org.jdom2.JDOMException
-dontwarn org.jdom2.JDOMFactory
-dontwarn org.jdom2.Text
-dontwarn org.jdom2.input.SAXBuilder
-dontwarn org.joda.time.DateTime
-dontwarn org.joda.time.DateTimeZone
-dontwarn org.joda.time.format.DateTimeFormatter
-dontwarn org.joda.time.format.ISODateTimeFormat
-dontwarn org.kxml2.io.KXmlParser
-dontwarn org.xmlpull.mxp1.MXParser
