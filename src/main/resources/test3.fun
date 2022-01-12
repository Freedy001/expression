import('java.lang.*');
import('java.io.*');

def content=new('FileInputStream','C:\Users\Freedy\Desktop\code\expression\src\main\resources\test4.fun');

new('String',content.readAllBytes(),env.get('file.encoding'));