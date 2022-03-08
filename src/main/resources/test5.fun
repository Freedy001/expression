def var; //直接定义

def int=10;//定义int 当长度超过32位时则会变成long
def bool=true; //定义Boolean
def str1="abc";//定义字符串
def str2='abc';//或者使用单引号
def d=12.3;//定义一个double

def obj1=new('java.util.ArrayList');//创建一个ArrayList对象
def obj2=new('java.util.ArrayList',8);//创建一个初始容量为8的ArrayList对象
//new()函数是一个可变参数的函数,第一个参数为对象的全类名,后面的参数为构造函数的参数


def arrEmpty=[];//定义一个列表
def arr=[1,2,3,4,5,6];//定义一个列表并赋值
def mapEmpty={};//定义一个map
def map={'k1':1,2:2};//定义一个map，并赋值

arr.add(32);
map.put(12,12);


//暂时有bug
//arr[0]==arr.get(0); //访问值
//map['k1']==map.get('k1');
//arr[0]=1223; //赋值
//map['k1']=32;

for(i:3){print(i)};
for(i@3){print(i)};
for(i:arr){print(i);};
for(i:range(5,10)){print(i)};
for(i:stepRange(5,10,2)){print(i)};
for(i:condition(@block{false})){print(i)};

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

def a=12;
def b=20;
print(max(a,b));
print(max(12,20));
print(maxPlus1(a,b));
print(maxPlus1(12,20));



if(a>b){return {a+1};}else{	return {b+1};};