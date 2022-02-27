# FUN EXPRESSION
`FUN`是一种基于java的表达式语言。我创造它就跟该表达式语言的名字一样，为了**玩**

## 基本语法

### 一. 变量

为了方便,该语言的类型会自动推断(都是使用java的Object类型)。定义一个变量可以直接用`def`关键字，例如：

```shell
def a; //直接定义

def i=10;//定义int 当长度超过32位时则会变成long
def bool=true; //定义Boolean
def str="abc";//定义字符串
def str='abc';//或者使用单引号
def d=12.3;//定义一个double

def obj1=new('java.util.ArrayList');//创建一个ArrayList对象
def obj2=new('java.util.ArrayList',8);//创建一个初始容量为8的ArrayList对象
//new()函数是一个可变参数的函数,第一个参数为对象的全类名,后面的参数为构造函数的参数


def arr=[];//定义一个列表
def arr=[1,2,3,4,5,6];//定义一个列表并赋值
def map={};//定义一个map
def map={'k1':1,2:2};//定义一个map，并赋值
```

>fun里面的列表都是使用JAVA里面的ArrayList实现的，Map都是使用JAVA里面的HashMap实现的

所以上面的arr(map)就可以直接当作ArrayList(HashMap)来使用,例如:

```shell
arr.add(32);
map.put(12,12);
```

普通变量的访问可以使用`#+变量名`或`@+变量名`或`直接变量名`，例如:

```shell
i;
#i;
@i;
//空值台都会输出[1,2,3,4,5,6]
arr
#arr
@arr
//空值台都会输出[1,2,3,4,5,6]
```

> 关于三者的区别，会在下面的     函数中的变量访问     中说明

静态变量与静态方法的访问可以使用`T(fullClassName).varOrMethod`种形式访问，例如:

```shell
def min=T(java.lang.Integer).MIN_VALUE;
def int=T(java.lang.Integer).parseInt("12");
```

列表和Map的访问可以直接通过`[]`来访问，或者使用ArrayList或HashMap相应的方法(例如arr.get(1)或arr.set(2,3)):

```shell
arr[0]==arr.get(0); //访问值
map['k1']==map.get('k1');
arr[0]=1223; //赋值
map['k1']=32;
```

### 二.流程控制

#### 1. 循环

正序循环使用`for(var:iterable)`种形式,var表示每个迭代个体，逆序循环使用`for(var@iterable)`种形式,var表示迭代个体。例如

```shell
fun> for(i:3){print(i)};  //正序循环3次，下面数字为输出结果
0
1
2
fun> for(i@3){print(i)};  //逆序循环3次，下面数字为输出结果
2
1
0
fun> def arr=[43,12,66,12,44,2];
fun> for(i:arr){print(i);}; //顺序遍历列表
43
12
66
12
44
2
```

要更加细致的控制循环可以使用FUN提供的系统函数，可以使用`help`指令查看。例如:

```shell
for(i:range(5,10)){print(i)}; //循环6次，i输出为5，5，6，7，8，9，10
for(i:stepRange(5,10,2)){print(i)}; //循环3次，i输出为5,7,9

//使用condition函数表示条件循环
//condition函数需要接收一个`@block{}`形式的代码块。`@block{}`会在下面有相关解释
for(i:condition(@block{true})){print(i)}; //死循环 这时i为循环次数
for(i:condition(@block{i<5})){print(i)}; //当i小于5时循环
```

#### 2. 条件

条件判断和java一样使用`if`

```java
if(left>=right){return;};

if#left>=right){
    return;
}else if(left==right){
	continue;
}else{
	print('haha');
};// 这里需要加上分号(';')
```

> 无论是循环还是条件控制，最后结束时都需要加上分号(;)，因为FUN是使用分号作为语句分割的,不加分号程序会报错

### 三.函数

由于是一门表达式语言所以没有使用专门的关键字来创建函数，而是使用FUN系统提供的一个函数`func()`来创建函数(没错是使用函数来创建函数)。

#### 1. func函数

该函数是一个可变参数的函数。第一个参数为函数名称，第二到倒数第二个参数为你要定义的函数的参数名称，最后一个参数为函数体,使用`@block{}`来放置

> @block{}:表示一个代码块,代码被包含在大括号中。
>
> 当使用@block{}关键字包含的代码作为参数传递给函数时，函数会接受到一个TokenSteam类的对象。
>
> TokenSteam对象表示一段代码，可以随时被调用执行，可以理解为java里面使用Lambda表达式作为方法参数时方法接收的对象。
>
> TokenSteam的详细信息可以参考下面的原理分析。

定义函数:

```shell
//定义一个函数用来求两个数中的最大的数
func('max','a','b',@block{
	//在block里面可以直接访问a和b,其原理是在运行时func函数会给TokenStream里面Token对应的Context设置一个a和b,详细见下面的原理分析.
	if(a>b){
		return a;
	}else{
		return b;
	};
});


//当返回的数需要进行相关操作时,需要用大括号括起来.
func('maxPlus1','a','b',@block{
	if(a>b){
		return {a+1};
	}else{
		return {b+1};
	};
});
```

上面定义了两个函数，下面就可以直接来调了

```shell
def a=12;
def b=20;
print(max(a,b));
print(max(12,20));
print(maxPlus1(a,b));
print(maxPlus1(12,20));
```

#### 2.函数中的变量访问

变量的访问可以使用`#+变量名`或`@+变量名`或`直接变量名`

```shell
def a=20;
def c=20;

test(50,100);

//`直接访问`和`#+变量名`访问大部分场景下是一样的
func('test1','a','b',@block{
	a=0;  				//这时访问的a为函数参数 是一个局部变量,这时上面定义的a的值不会被修改
	#b=20;				//这时访问的b也为函数参数 是一个局部变量
});

//`@+变量名`会访问到全局变量而不是本地变量
func('test2','a','b',@block{
	@a=0;				//会将全局变量a改为0
	a=20;				//会将局部变量a改为20
	c=10;				//会将全局变量c改为0
	print(@a);			//输出0
	print(a);			//输出20
	print(c);			//输出10
});
```

当使用API而不是命令行时是可以设置root对象的，而`直接访问`首先会访问root对象的成员变量,如果没有相关的成员变量则会按照`#+变量名`的模式来访问。

在创建`EvaluationContext`是可以使用`Object setRoot(Object root);`方法来设置root的

> EvaluationContext 是运行时候的环境变量，所有定义的方法和变量都存在这个里面,

下面来演示该如何使用

```java
public class root{
    a=0;
}
```

```java
public class Test{
    public static void main(String[] args){
        StanderEvaluationContext context=new StanderEvaluationContext(new root);
        ExpressionPasser parser=new ExpressionPasser();
        Expression express=parser.parseExpression("""
            def a=20;
            def c=20;     
            test(30,30);                             
            func('test','a','b',@block{
                print(a);   //这输出0
                print(#a);  //这输出30
                print(@a);  //这输出20
            }
        """);
    }
}
```

#### 3.FUN内部提供函数

内部函数全部定义在`StanderEvaluationContext`中，如果自己创建了一个EvaluationContext又想要FUN内部提供函数时可以直接继承StanderEvaluationContext类

- **newInterface()**用于创建java的匿名内部类

例如有一个java接口

```java
public interface com.freedy.expression.com.freedy.expression.Test{
    void test1(int o1,int o2);
    void test1(Object o1);
}
```

下面使用`newInterface()`函数来创建匿名内部内

```shell
def a=newInterface('package.com.freedy.expression.com.freedy.expression.Test',
    'test1','o1','o2',@block{
        //your code
        print('i am test1'+o1+o2);
    },
    'test2','o2',@block{
        //your code
        print(o2);
	});
	
a.test1('1','2');
a.test2('ni hao');
```

> 详细用法可以调用help('newInterface');指令

- **lambda()**用于创建java的lambda表达式

例如我相对Arraylist进行排序

```shell
def arr=[32,5,12,7,6];
arr.sort(lambda('a','b',@block{
	return {a-b};
}));
print(arr);  //输出 5,6,7,12,32  
```

>详细用法可以调用help('newInterface');指令

- **code()**反编译Class

```java
def a=new('com.freedy.expression.token.BasicVarToken');
code(a);
code('com.freedy.expression.token.BasicVarToken'); 
//都会输出BasicVarToken的java代码
```

> 详细用法可以调用help('code');指令

- **defClass()**使用FUN脚本定义一个java类

```shell
//通过脚本定义java类  暂不支持接口与继承
defClass('com.freedy.expression.stander.TestDefClass',@block{

    def a=10;
    def b='haha';
    def c=30.2;
    def d;
    def e;
    def f;
    def g=new('java.util.Date');
	
	//方法名为construct的是构造函数
    func('construct','c','d',@block{
        print('i am born');
        @c=c;
        @d=d;
    });

    func('fun1','a','b',@block{
        print('i am fun1'+a+b);
        @a=a;
        @b=b;
    });

    func('fun2','a','b',@block{
        print('i am fun2'+a+@b);
    });

    func('fun3','a','b',@block{
        print('i am fun3'+@a+b);
    });
});

print(code('com.freedy.expression.stander.TestDefClass'));

def test=new ('com.freedy.expression.stander.TestDefClass',12.2,'4234');

test.fun1(1,'2'); 
```
> 详细用法可以调用help('code');指令

- **import()**简化导入

当使用`new()`函数时需要传入全限定类名。这样会使每次创建对象时都很麻烦，所以可以使用`import()`来导入包或者某个类，下次再调用`new()`时就可以直接简化类名

```shell
def arr1=new('java.util.ArrayList'); //没有使用import() 需要全类名

import('java.util.*');
def arr2=new('ArrayList');  //使用过import()后,每次new都只需要简化类名 
```

> import()除了对new函数有效外还对T()访问静态变量/方法有效

- **require()**执行另外一个FUN脚本文件，需要传入路径参数

> 更多内部函数可以通过help指令查看

#### 4. 使用API自定义内部函数

可以使用EvaluationContext的registerFunction来像内部注册函数.

```java
public class Test{
    public static void main(String[] args){
        StanderEvaluationContext context=new StanderEvaluationContext(); //创建环境变量
        context.registerFunction("TEST",()->{							 //注册函数
            System.out.println("Test");
        });
        ExpressionPasser parser=new ExpressionPasser();						 //创建解析器
        Expression express=parser.parseExpression("TEST() //将会输出 Test");   //输入语句
        express.getValue(context);											 //执行语句
    }
}
```

### 四.其他特性

该表达式语言的设计非常简单所以并没有设计某些特性（相比于java）。例如没有设计异常处理的特性，没有实现多线程的特性，没有实现访问权限的特性等等。但是该表达式语言向外提供了可以调用Java语言的`自定义内部函数API`，可以使用这个特性来用Java代码实现部分功能。

例如我想实现异常处理可以添加如下函数:

```java
public class Test{
    public static void main(String[] args){
        //try_catch_finally同理
context.registerFunction("try_catch", (Consumer._3ParameterConsumer<Runnable, Class<? extends Throwable >, Consumer._1ParameterConsumer<Throwable>>) (_try, _exception, _catch) -> {
            try {
                _try.run();
            } catch (Throwable ex) {
                if (_exception.isInstance(ex)) {
                    _catch.accept(ex);
                }
            }
        });
}
```

然后就可以直接在表达式中使用

```shell
def exType=class("Exception");
try_catch(newInterface('com.freedy.expression.function.Runnable','run',@block{
    10/0;
}),exType,newInterface('com.freedy.expression.function.Consumer$_1ParameterConsumer','accept','ex',@block{
    print(ex);
}));
```

输出

```shell
com.freedy.expression.exception.ExpressionSyntaxException: 

:)BigInteger divide by zero at:
    10/0;
      ^
```

## 项目构建

构建版本**` JDK17`**

### 一. 使用命令行启动

```shell
git clone https://github.com/Freedy001/expression.git
```
```shell
cd expression
```
```shell
mvn package
```
```shell
cd target
```

```shell
java --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED  --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar expression-1.0.0.jar
# 可选参数 -DdisableJline=xxx 用于禁用代码提示功能，可能会解决一些不可预料的BUG
```

### 二. 使用IDEA启动

使用IDEA启动时需要加上JVM参数和编译参数

JVM参数(在`启动按钮->修改运行配置->修改选项->添加VM选项`中)

```shell
--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED
```

编译参数(在`设置->构建执行部署->编译器->java编译器`中)

```shell
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED
```

### 三. 项目集成FUN

项目导入

```xml
<dependency>
    <groupId>com.freedy</groupId>
    <artifactId>expression</artifactId>
    <version>1.0.1</version>
</dependency>
```

在你的项目使用如下API

```java
CommanderLine.startRemote(12,"abcdefghijklmnop","asdasfasasfasfasfas",ctx->{});
```

第一个参数为启动端口号，第二个参数为AES对称加密Key必须为16位，第三个参数为一个MD5的加密验证头可以随便填写，第四个参数为Netty的`channelActive`方法可以用于对连接的增强。

然后就可以再启动一个FUN利用`::connect`命令进行连接

```shell
fun> ::connect
address(ip:port):127.0.0.1:12
aes-key:abcdefghijklmnop
auth-key:asdasfasasfasfasfas
```

> 当你的项目集成了FUN之后表明了你的项目对外具有动态执行代码的能力。也可能被他人恶意使用，建议在集成时通过第四个参数增强API，例如加上IP限制等等。



## 项目解析

#### 一. 包与重要的类的介绍

`com.freedy.expression.exception`:异常包，其中`ExpressionSyntaxException`类是核心。FUN的所有语法异常包括运行时异常都是通过`ExpressionSyntaxException`进行包装。`ExpressionSyntaxException`可以将错误代码进行高亮输出以便快速检测错误。

`com.freedy.expression.function`:该包包含所有进行函数注册(可以参考上面 **使用API自定义内部函数**)的函数式接口。

`com.freedy.expression.stander`:所有FUN的内部函数功能都在这个包里面

`com.freedy.expression.token`:所有Token，token可以理解为语句的抽象化结果

`com.freedy.expression.tokenBuilder`:所有Token Builder,用于构建不同种类的Token

`Tokenizer`:将语句转化为Token集合

`TokenStream`:Token集合，与Context配合就可以被执行

`Expression`:TokenStream的执行器



## 说明

- 我创建该项目的本意是 可以在项目运行时不重启而动态运行一些内容（方便开发）。

- 由于本人时间和精力有限没有做过大量测试，所以有可能存在很多BUG。

  
