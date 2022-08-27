
def a=12;
def b=22;
def c=12;


def myFun(a,b){
   print(a+b);
   print(#a+#b);
   print(@a+@b);
   print(@c+c);
   print(#c+c);
   a=1;
   b=2;
   c=3;
   print(a);
   print(b);
   print(c);
   print(@a);
   print(@b);
   print(@c);
   print(ctx);
   print(T(com.alibaba.fastjson.JSON).toJSONString(#ctx));
   print(@ctx);
};

myFun(100,200);