//通关脚本定义java类
defClass('com.freedy.expression.stander.TestDefClass',@block{

    def a=10;
    def b='haha';
    def c=30.2;
    def d;
    def e;
    def f;
    def g=new('java.util.Date');

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