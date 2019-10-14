# MyHqkFix
# 免费让你的APP拥有在线修复Bug的能力

#如何使用：

 《1》引入框架
 
    1、maven导入：
    
      <dependency>
        <groupId>com.hqk.fix</groupId>
        <artifactId>myhqkfix</artifactId>
        <version>1.0.0</version>
        <type>pom</type>
      </dependency>
      
    2、gradle导入：
    
      implementation 'com.hqk.fix:myhqkfix:1.0.0'
      
  《2》初始化框架
 
    1、在你自定义的Application里面 重写 attachBaseContext 方法，在里面添加
     
      @Override
      protected void attachBaseContext(Context base) {
          MyHqkFixUtil.getInstance().loadDex(base);
          super.attachBaseContext(base);
      }
      
  《3》加载修复包
  
    1、建议在启动页时，通过后台返回的接口读取最新修复包的下载地址。将这个修复包下载到本地。最后将下载好的修复包通过以下代码调用
    
      MyHqkFixUtil.getInstance().fix(MyApplication.gContext, fileName, dexDir)      
      
      参数讲解：
        1、上下文，建议用application的context
        2、修复包的名称，【名称一定按照打包的dex的名称来填写】 后面会讲解修复包名称的注意事项
        3、修复包的绝对目录，不传的话默认系统缓存路径

      
